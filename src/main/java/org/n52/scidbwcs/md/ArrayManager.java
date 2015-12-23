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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.n52.scidbwcs.db.ISciDBCellProcessor;
import org.n52.scidbwcs.db.IShimTextCellProcessor;
import org.n52.scidbwcs.db.SciDBConnection;
import org.n52.scidbwcs.db.SciDBQueryResult;
import org.n52.scidbwcs.db.ShimClient;
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

    public Array getArrayMD_JDBC(String array) {
        ArrayList<String> s = new ArrayList<>();
        s.add(array);
        return getArrayMD_JDBC(s).get(0);
    }

    public List<Array> getArrayMD_JDBC() {
        return getArrayMD_JDBC(new ArrayList<String>());
    }

    public List<Array> getArrayMD_JDBC(List<String> arrays) {

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
                    log.debug("Processing MD of array '" + name + "'");
                    String dims_str = res1.getString("dimensions");
                    log.debug("Got dimension string '" + dims_str + "'");
                    String attrs_str = res1.getString("attributes");
                    log.debug("Got attribute string '" + attrs_str + "'");
                    String srs_str = res1.getString("srs");
                    log.debug("Got SRS string '" + srs_str + "'");
                    String trs_str = res1.getString("trs");
                    log.debug("Got TRS string '" + trs_str + "'");
                    String extent_str = res1.getString("extent");
                    log.debug("Got extent string '" + extent_str + "'");

                    Array a = new Array(name);

                    // Parse dimension string
                    Pattern p = Pattern.compile("\\[(.*?)\\]");
                    Matcher m = p.matcher(dims_str);
                    while (m.find()) {
                        String[] pars;
                        try {
                            pars = m.group(1).split(SEP);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse dimension string '" + m.group(1) + "' for array '" + name + "', will be ignored...");
                            return;
                        }
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
                        String[] pars;
                        try {
                            pars = m.group(1).split(SEP);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse attribute string '" + m.group(1) + "' for array '" + name + "', will be ignored...");
                            return;
                        }

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

                        String[] pars;
                        try {
                            pars = srs_str.split(SEP);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse SRS string '" + srs_str + "' for array '" + name + "', will be ignored...");
                            return;
                        }

                        // xdim,ydim,atuhname,authid,A,wkt,proj4
                        a.setSrs(new SpatialReference(new AffineTransform(pars[4]), pars[0], pars[1], pars[2], Integer.parseInt(pars[3]), pars[6], pars[5]));
                    } else {
                        a.setSrs(null);
                    }

                    // Parse trs string (if not empty)
                    if (!trs_str.isEmpty()) {

                        String[] pars;
                        try {
                            pars = trs_str.split(SEP);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse TRS string '" + trs_str + "' for array '" + name + "', will be ignored...");
                            return;
                        }

                        a.setTrs(new TemporalReference(pars[0], pars[1], pars[2]));
                    } else {
                        a.setTrs(null);
                    }

                    // Parse trs string (if not empty)
                    if (!extent_str.isEmpty()) {

                        String[] vals;
                        try {
                            vals = extent_str.split(SEP, -2);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse extent string '" + extent_str + "' for array '" + name + "', will be ignored...");
                            return;
                        }

                        if (vals.length < 6) {
                            a.setExtent(null);
                        } else // TODO: Distinguish if not spatial or not temporal
                         if (a.isSpatial() && a.isTemporal()) {
                                a.setExtent(new Extent(Double.parseDouble(vals[0]), Double.parseDouble(vals[1]), Double.parseDouble(vals[2]), Double.parseDouble(vals[3]), vals[4], vals[5], Double.NaN, Double.NaN));
                            } else if (a.isSpatial()) {
                                a.setExtent(new Extent(Double.parseDouble(vals[0]), Double.parseDouble(vals[1]), Double.parseDouble(vals[2]), Double.parseDouble(vals[3]), "", "", Double.NaN, Double.NaN));
                            } else if (a.isTemporal()) {
                                a.setExtent(new Extent(Double.NaN, Double.NaN, Double.NaN, Double.NaN, vals[4], vals[5], Double.NaN, Double.NaN));
                            } else {
                                a.setExtent(null);
                            }

                    } else {
                        a.setExtent(null);
                    }

                    A.add(a);

                    arrayCache.put(a.getName(), a);
                    cacheTimes.put(a.getName(), System.currentTimeMillis());

                } catch (Exception e) { // Simply ignore current array if any(!) exceptions are thrown

                    // see http://stackoverflow.com/questions/1149703/how-can-i-convert-a-stack-trace-to-a-string
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    sw.toString(); // stack trace as a string
                    log.warn("Cannot extract metadata: " + e);
                    log.debug(sw.toString());

                }
            }
        });

        return A;

    }

    
    
    
    
    
    
    
    
    public Array getArrayMD_shim(String array) {
        ArrayList<String> s = new ArrayList<>();
        s.add(array);
        return getArrayMD_shim(s).get(0);
    }

    public List<Array> getArrayMD_shim() {
        return getArrayMD_shim(new ArrayList<String>());
    }
    
    
    
    
    
    
    
    public List<Array> getArrayMD_shim(List<String> arrays) {

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

        afl="list()";
        ShimClient.get().queryReadCSV(afl, new IShimTextCellProcessor() {
            @Override
            public void process(String cell) {

                // Split by attributes according to ',' pattern (as all result attributes of eo_all are strings)
                String[] attrstrings = cell.split("','");

                if (attrstrings.length < 6) {
                    log.warn("Cannot parse CSV output cell '", cell, "'. Corresponding cell will be ignored...");
                    return;
                }
                
                
                
                final String SEP = ";;;";

                try {
                    String name = attrstrings[0].replace("'", ""); // Add first character '
                    log.debug("Processing MD of array '" + name + "'");
                    String dims_str =  attrstrings[1];
                    log.debug("Got dimension string '" + dims_str + "'");
                    String attrs_str =  attrstrings[2];
                    log.debug("Got attribute string '" + attrs_str + "'");
                    String srs_str = attrstrings[3];
                    log.debug("Got SRS string '" + srs_str + "'");
                    String trs_str =  attrstrings[4];
                    log.debug("Got TRS string '" + trs_str + "'");
                    String extent_str =  attrstrings[5].replace("'", ""); // Add last character '
                    log.debug("Got extent string '" + extent_str + "'"); 

                    Array a = new Array(name);

                    // Parse dimension string
                    Pattern p = Pattern.compile("\\[(.*?)\\]");
                    Matcher m = p.matcher(dims_str);
                    while (m.find()) {
                        String[] pars;
                        try {
                            pars = m.group(1).split(SEP);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse dimension string '" + m.group(1) + "' for array '" + name + "', will be ignored...");
                            return;
                        }
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
                        String[] pars;
                        try {
                            pars = m.group(1).split(SEP);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse attribute string '" + m.group(1) + "' for array '" + name + "', will be ignored...");
                            return;
                        }

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

                        String[] pars;
                        try {
                            pars = srs_str.split(SEP);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse SRS string '" + srs_str + "' for array '" + name + "', will be ignored...");
                            return;
                        }

                        // xdim,ydim,atuhname,authid,A,wkt,proj4
                        a.setSrs(new SpatialReference(new AffineTransform(pars[4]), pars[0], pars[1], pars[2], Integer.parseInt(pars[3]), pars[6], pars[5]));
                    } else {
                        a.setSrs(null);
                    }

                    // Parse trs string (if not empty)
                    if (!trs_str.isEmpty()) {

                        String[] pars;
                        try {
                            pars = trs_str.split(SEP);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse TRS string '" + trs_str + "' for array '" + name + "', will be ignored...");
                            return;
                        }

                        a.setTrs(new TemporalReference(pars[0], pars[1], pars[2]));
                    } else {
                        a.setTrs(null);
                    }

                    // Parse trs string (if not empty)
                    if (!extent_str.isEmpty()) {

                        String[] vals;
                        try {
                            vals = extent_str.split(SEP, -2);
                        } catch (NegativeArraySizeException ex) {
                            log.warn("Cannot parse extent string '" + extent_str + "' for array '" + name + "', will be ignored...");
                            return;
                        }

                        if (vals.length < 6) {
                            a.setExtent(null);
                        } else // TODO: Distinguish if not spatial or not temporal
                         if (a.isSpatial() && a.isTemporal()) {
                                a.setExtent(new Extent(Double.parseDouble(vals[0]), Double.parseDouble(vals[1]), Double.parseDouble(vals[2]), Double.parseDouble(vals[3]), vals[4], vals[5], Double.NaN, Double.NaN));
                            } else if (a.isSpatial()) {
                                a.setExtent(new Extent(Double.parseDouble(vals[0]), Double.parseDouble(vals[1]), Double.parseDouble(vals[2]), Double.parseDouble(vals[3]), "", "", Double.NaN, Double.NaN));
                            } else if (a.isTemporal()) {
                                a.setExtent(new Extent(Double.NaN, Double.NaN, Double.NaN, Double.NaN, vals[4], vals[5], Double.NaN, Double.NaN));
                            } else {
                                a.setExtent(null);
                            }

                    } else {
                        a.setExtent(null);
                    }

                    A.add(a);

                    arrayCache.put(a.getName(), a);
                    cacheTimes.put(a.getName(), System.currentTimeMillis());

                } catch (Exception e) { // Simply ignore current array if any(!) exceptions are thrown

                    // see http://stackoverflow.com/questions/1149703/how-can-i-convert-a-stack-trace-to-a-string
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    sw.toString(); // stack trace as a string
                    log.warn("Cannot extract metadata: " + e);
                    log.debug(sw.toString());

                }
                
              
            }
        });


        return A;

    }

}
