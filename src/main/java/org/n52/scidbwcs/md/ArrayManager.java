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
package org.n52.scidbwcs.md;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.n52.scidbwcs.db.ISciDBCellProcessor;
import org.n52.scidbwcs.db.SciDBConnection;
import org.n52.scidbwcs.db.SciDBQueryResult;
import org.n52.scidbwcs.util.Config;

/**
 * The array manager is used to get array metadata from SciDB. A simple metadata
 * cache tries to minimize database queries, which are relatively slow from
 * JDBC. This is a singleton class.
 */
public class ArrayManager {

    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(ArrayManager.class);

    private static final Long REFRESH_AFTER_SEC = Config.get().SCIDBWCS_MDCACHE_REFRESH_AFTER_SEC;

    private static ArrayManager instance = null;

    private ArrayManager() {
        this.arrayCache = new HashMap<>();
        this.cacheTimes = new HashMap<>();
    }

    public static ArrayManager instance() {
        if (instance == null) {
            instance = new ArrayManager();
        }
        return instance;
    }

    private HashMap<String, Array> arrayCache; // Actual metadata cache with array names as keys
    private HashMap<String, Long> cacheTimes;  // Datetime of latest metadata queries for arrays

    private void cacheRemove(String array) {
        arrayCache.remove(array);
        cacheTimes.remove(array);
    }

    private boolean cacheHas(String array) {
        if (arrayCache.containsKey(array)) {
            if ((System.currentTimeMillis() - cacheTimes.getOrDefault(array, (long) 0)) <= REFRESH_AFTER_SEC) {
                return true;
            } else {
                cacheRemove(array);
                return false;
            }
        }
        return (false);
    }

    private Array cacheGet(String array) {
        if (arrayCache.containsKey(array)) {
            if ((System.currentTimeMillis() - cacheTimes.getOrDefault(array, (long) 0)) / 1000 <= REFRESH_AFTER_SEC) {
                return arrayCache.get(array);
            } else {
                cacheRemove(array);
                return null;
            }
        }
        return null;
    }

    public Array getArrayMD(String array) {
        ArrayList<String> s = new ArrayList<>();
        s.add(array);
        return getArrayMD(s).get(0);
    }

    public List<Array> getArrayMD() {
        return getArrayMD(new ArrayList<String>());
    }

    public List<Array> getArrayMD(List<String> arrays) {

        final List<Array> A = new ArrayList<>();

        SciDBQueryResult res;
        String afl = "";
        /* Currently, if all arrays are requested, this always causes a full reload from the database
        no matter whether all arrays are cached or not */

        if (arrays == null || arrays.isEmpty()) {
            afl = "eo_all()";
        } else {

            ArrayList<String> toLoad = new ArrayList<>();

            for (int i = 0; i < arrays.size(); ++i) {
                // Check whether array is already in cache
                Array a = cacheGet(arrays.get(i));
                if (a == null) {
                    toLoad.add(arrays.get(i));
                } else {
                    A.add(a);
                }
            }

            if (toLoad.isEmpty()) {
                return A;
            }

            afl = "eo_all(";
            for (int i = 0; i < toLoad.size() - 1; ++i) {
                afl += toLoad.get(i) + ",";
            }
            afl += arrays.get(toLoad.size() - 1) + ")";

        }
        log.debug("Performing AFL Query: " + afl);
        res = SciDBConnection.get().queryRead(afl);
        //System.out.println("RESULT SCHEMA: " + res.getResultSchema().toString());
        res.iterate(new ISciDBCellProcessor() {
            @Override
            public void process(ResultSet res1) throws SQLException {

                final String SEP = ";;;";

                try {
                    String name = res1.getString("name");
                    String dims_str = res1.getString("dimensions");
                    String attrs_str = res1.getString("attributes");
                    String srs_str = res1.getString("srs");
                    String trs_str = res1.getString("trs");
                    String extent_str = res1.getString("extent");

                    Array a = new Array(name);

                    // Parse dimension string
                    Pattern p = Pattern.compile("\\[(.*?)\\]");
                    Matcher m = p.matcher(dims_str);
                    while (m.find()) {
                        String[] pars = m.group(1).split(SEP);
                        // TODO: Assert that p has appropriate length
                        ArrayDimension d = new ArrayDimension(pars[0]);
                        d.min = Long.parseLong(pars[1]);
                        d.max = d.min + Long.parseLong(pars[2]) - 1;
                        d.chunkSize = Long.parseLong(pars[3]);
                        d.overlap = Long.parseLong(pars[4]);
                        d.curMin = Long.parseLong(pars[5]);
                        d.curMax = Long.parseLong(pars[6]);
                        a.Dimensions().add(d);
                    }

                    // Parse attribute string
                    p = Pattern.compile("\\<(.*?)\\>");
                    m = p.matcher(attrs_str);
                    while (m.find()) {
                        String[] pars = m.group(1).split(SEP);
                        // TODO: Assert that p has appropriate length
                        ArrayAttribute att = new ArrayAttribute();
                        att.name = pars[0];
                        att.typeId = pars[1];
                        if (pars.length > 2) {
                            att.nullable = Boolean.valueOf(pars[2]);
                        }
                        a.Attributes().add(att);
                    }

                    // Parse srs string (if not empty)
                    if (!srs_str.isEmpty()) {

                        String[] pars = srs_str.split(SEP);
                        // TODO: Assert that p has appropriate length
                        // xdim,ydim,atuhname,authid,A,wkt,proj4
                        a.setSrs(new SpatialReference(new AffineTransform(pars[4]), pars[0], pars[1], pars[2], Integer.parseInt(pars[3]), pars[6], pars[5]));
                    } else {
                        a.setSrs(null);
                    }

                    // Parse trs string (if not empty)
                    if (!trs_str.isEmpty()) {

                        String[] pars = trs_str.split(SEP);
                        // TODO: Assert that p has appropriate length

                        a.setTrs(new TemporalReference(pars[0], pars[1], pars[2]));
                    } else {
                        a.setTrs(null);
                    }

                    // Parse trs string (if not empty)
                    if (!extent_str.isEmpty()) {

                        String[] vals = extent_str.split(SEP, -2);
                        // TODO: Assert that vals has appropriate length
                        if (vals.length < 6) {
                            a.setExtent(null);
                        } else // TODO: Distinguish if not spatial or not temporal
                        {
                            if (a.isSpatial() && a.isTemporal()) {
                                a.setExtent(new Extent(Double.parseDouble(vals[0]), Double.parseDouble(vals[1]), Double.parseDouble(vals[2]), Double.parseDouble(vals[3]), vals[4], vals[5], Double.NaN, Double.NaN));
                            } else if (a.isSpatial()) {
                                a.setExtent(new Extent(Double.parseDouble(vals[0]), Double.parseDouble(vals[1]), Double.parseDouble(vals[2]), Double.parseDouble(vals[3]), "", "", Double.NaN, Double.NaN));
                            } else if (a.isTemporal()) {
                                a.setExtent(new Extent(Double.NaN, Double.NaN, Double.NaN, Double.NaN, vals[4], vals[5], Double.NaN, Double.NaN));
                            } else {
                                a.setExtent(null);
                            }
                        }

                    } else {
                        a.setExtent(null);
                    }

                    A.add(a);

                    arrayCache.put(a.getName(), a);
                    cacheTimes.put(a.getName(), System.currentTimeMillis());

                } catch (Exception e) {
                    log.debug("Cannot extract metadata: " + e);
                }
            }
        });

        return A;

    }

}
