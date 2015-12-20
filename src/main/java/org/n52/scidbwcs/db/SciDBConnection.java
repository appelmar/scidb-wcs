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

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.n52.scidbwcs.util.Config;
import org.scidb.jdbc.IStatementWrapper;

/**
 * Singleton implementation of SciDB database connection over JDBC
 */
public class SciDBConnection {

    private static final Logger log = LogManager.getLogger(SciDBConnection.class);
    private static SciDBConnection instance = null; // Singleton instance
    private Connection conn = null;
    private final String connString;

    // Private constructor that creates the singleton instance and builds the JDBC connection string
    private SciDBConnection() {
        try {
            Class.forName("org.scidb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.fatal("SciDB JDBC Driver is not in the CLASSPATH: " + e);
        }
        connString = "jdbc:scidb://" + Config.get().SCIDBWCS_DB_HOST + ":" + Config.get().SCIDBWCS_DB_PORT;
    }

    /**
     * Returns the singleton instance or creates a new connection if not yet available
     * @return a valid database connection instance 
     */
    public static SciDBConnection get() {
        if (instance == null) {
            instance = new SciDBConnection();
        }
        return instance;
    }

    /**
     * Checks whether the current connection is still alive and automatically reconnects if neccessary
     */
    private void createConnectionIfNeeded() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(connString);
            }
        } catch (SQLException e) {
            log.error("Cannot connect to database: " + e);
        }
    }

    /**
     * Executes a write query in AFL, i.e. does not return results
     * @param query AFL query string
     */
    public void queryWrite(String query) {
        queryWrite(query, true);
    }
    
    
    /**
     * Executes a write query in either AFL or AQL, i.e. does not return results
     * @param query SciDB query string
     * @param afl if true, query string is understood as AFL, otherwise as AQL
     */
    public void queryWrite(String query, boolean afl) {
        createConnectionIfNeeded();
        try {
            Statement st = conn.createStatement();
            if (afl) {
                IStatementWrapper stWrapper = st.unwrap(IStatementWrapper.class);
                stWrapper.setAfl(true);
            }
        } catch (SQLException e) {
            log.error("Error while trying to execute write query: " + e);
        }
    }

     /**
     * Executes a read query in AFL
     * @param query AFL query string
     * @return query result
     */
    public SciDBQueryResult queryRead(String query) {
        return queryRead(query, true);
    }

     /**
     * Executes a read query in AFL or AQL
     * @param query SciDB query string
     * @param afl if true, query string is understood as AFL, otherwise as AQL
     * @return query result
     */
    public SciDBQueryResult queryRead(String query, boolean afl) {
        createConnectionIfNeeded();
        SciDBQueryResult res = null;
        try {
            Statement st = conn.createStatement();
            if (afl) {
                IStatementWrapper stWrapper = st.unwrap(IStatementWrapper.class);
                stWrapper.setAfl(true);
            }
            res = new SciDBQueryResult(query, st.executeQuery(query));
        } catch (SQLException e) {
            log.error("Error while trying to execute read query: " + e);
        }
        return res;
    }

    

}
