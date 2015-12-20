/*
 * scidb-wcs - A Web Coverage Service implementation for SciDB
 *
 * Copyright (C) 2015 Marius Appel <marius.appel@uni-muenster.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.n52.scidbwcs.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.n52.scidbwcs.md.Array;
import org.n52.scidbwcs.md.ArrayAttribute;
import org.n52.scidbwcs.md.ArrayDimension;
import org.scidb.jdbc.IResultSetWrapper;

/**
 * Class for handling results of SciDB read queries
 */
public class SciDBQueryResult {

    private static final Logger log = LogManager.getLogger(SciDBConnection.class);

    private String query; // The original query
    private ResultSet res; // Corresponding JDBC ResultSet object

    /**
     * Creates a new object based on a JDBC ResultSet
     *
     * @param queryString the original SciDB query string
     * @param res Corresponding JDBC ResultSet object
     */
    public SciDBQueryResult(String queryString, ResultSet res) {
        this.res = res;
        this.query = queryString;
    }

    /**
     * Returns the SciDB schema of the result. Metadata entries include only the
     * array name, dimension names, and attribute names but neither spatial nor temporal
     * reference information is queried here.
     * @return Result schema metadata
     **/
    public Array getResultSchema() {
        Array arr = null;
        try {
            ResultSetMetaData meta = res.getMetaData();
            arr = new Array(meta.getTableName(0));

            IResultSetWrapper resWrapper = res.unwrap(IResultSetWrapper.class);
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if (resWrapper.isColumnAttribute(i)) 
                {
                    ArrayAttribute a = new ArrayAttribute(meta.getColumnName(i), meta.getColumnTypeName(i), meta.getColumnLabel(i));
                    arr.Attributes().add(a);
                } else if (resWrapper.isColumnDimension(i)) {
                    ArrayDimension d = new ArrayDimension(meta.getColumnName(i));

                    // Dimension details like chunk sizes, boundaries, etc. are not filled here!
                    arr.Dimensions().add(d);
                }
            }

        } catch (SQLException e) {
            log.error("Error while trying to fetch metadata of query output: " + e);
        }
        return arr;
    }

    /**
     * Returns the JDBC result object
     * @return JDBC query result
     */
    public ResultSet getResult() {
        return res;
    }

    /**
     * Cell-wise iterates over the result array of a SciDB query
     * @param f Callback function as ISciDBCellProcessor implementation (to be replaces with lambda)
     */
    public void iterate(ISciDBCellProcessor f) {
        try {
            while (!res.isAfterLast()) {
                f.process(res);
                res.next();
            }
        } catch (SQLException e) {
            log.error("Error while traversing query result: " + e);
        }
    }

}
