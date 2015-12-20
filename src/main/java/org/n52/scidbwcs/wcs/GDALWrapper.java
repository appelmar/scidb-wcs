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
package org.n52.scidbwcs.wcs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.joda.time.DateTime;
import org.n52.scidbwcs.exception.WCSException;
import org.n52.scidbwcs.md.Array;
import org.n52.scidbwcs.md.ArrayManager;
import org.n52.scidbwcs.util.Config;

/**
 * A wrapper class to call GDAL binaries
 */
public class GDALWrapper {

    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(GDALWrapper.class);

    private static ArrayList<String> buildTranslateCommand(WCSGetCoverageRequest req) throws WCSException {
        ArrayList<String> cmdlist = new ArrayList<>();
        String cmd = "";
        if (Config.get().SCIDBWCS_GDALPATH != null) {
            cmd += Config.get().SCIDBWCS_GDALPATH;
            if (!Config.get().SCIDBWCS_GDALPATH.endsWith("/")) {
                cmd += "/";
            }
        }
        cmd += "./gdal_translate";
        cmdlist.add(cmd);

        
        if (req.resx > 0 && req.resy > 0) {
            cmdlist.add("-tr");
            cmdlist.add(Double.toString(req.resx));
            cmdlist.add(Double.toString(req.resy));
        }
        
        
        if (req.width > 0 && req.height > 0) {
            cmdlist.add("-outsize");
            cmdlist.add(Integer.toString(req.width));
            cmdlist.add(Integer.toString(req.height));
        }
        
        
        if (req.crs.equalsIgnoreCase("IMAGE")) {
            assert req.bbox.length == 4 || req.bbox.length == 6;
            
            cmdlist.add("-srcwin");
            cmdlist.add(Long.toString((long) Math.round(req.bbox[0])));
            cmdlist.add(Long.toString((long) Math.round(req.bbox[1])));
            cmdlist.add(Long.toString((long) Math.round(req.bbox[2] - req.bbox[0])));
            cmdlist.add(Long.toString((long) Math.round(req.bbox[3] - req.bbox[1])));
            
            
        } else {
            assert req.bbox.length == 4 || req.bbox.length == 6;
            
             cmdlist.add("-projwin");
             cmdlist.add(Double.toString(req.bbox[0]));
             cmdlist.add(Double.toString(req.bbox[3]));
             cmdlist.add(Double.toString(req.bbox[2]));
             cmdlist.add(Double.toString(req.bbox[1]));
            
             cmdlist.add("-projwin_srs");
             cmdlist.add(req.crs);
        }

        
        
        if (req.time != null && req.time.length > 0) {
            // Convert datetime to index (could be done automatically by GDAl as well)
            Array A = ArrayManager.instance().getArrayMD(req.coverage);
            Long tidx = A.trs().indexAtDatetime(DateTime.parse(req.time[0]));
            
            if (tidx < A.getTDim().getTrueMin() || tidx > A.getTDim().getTrueMax()) {
                throw new WCSException("Requested time is out of the coverage's range.",WCSException.WCS_EXCEPTION_CODE.InvalidParameterValue);
            }
            // TODO: Check whether tidx is valid in temporal dimension
            
            cmdlist.add("-oo");
            cmdlist.add("t=" + tidx.toString());
        }
        
        
        
        
        switch (req.format.toUpperCase()) {
            case "JPEG":
            case "PNG":
            case "GIF":
            case "BMP":
                cmdlist.add("-of");
                cmdlist.add(req.format.toUpperCase());
                break;
            case "GEOTIFF":
                cmdlist.add("-of");
                cmdlist.add("GTiff");
                break;
            case "NETCDF":
                cmdlist.add("-of");
                cmdlist.add("netCDF");
                break;
        }
        
        
        
        switch (req.interpolation.toUpperCase()) {
            case "NEAREST":
                cmdlist.add("-r");
                cmdlist.add("nearest");
                break;
            case "BILINEAR":
                cmdlist.add("-r");
                cmdlist.add("bilinear");
                break;
            case "BICUBIC":
                cmdlist.add("-r");
                cmdlist.add("cubic");
                break;
        }
     
        
        // SciDB connection string
        String inDSStr = "SCIDB:array=" + req.coverage + " host=" + (Config.get().SCIDBWCS_DB_SSL ? "https" : "http") + "://" + Config.get().SCIDBWCS_DB_HOST + " port=" + Config.get().SCIDBWCS_DB_SHIMPORT + " user=" + Config.get().SCIDBWCS_DB_USER + " password=" + Config.get().SCIDBWCS_DB_PW;
        cmdlist.add(inDSStr);
       
        
        String outDSStr =  Config.get().SCIDBWCS_TEMPPATH + (Config.get().SCIDBWCS_TEMPPATH.endsWith("/") ? "" : "/") + req.getRequestID();
        
        switch (req.format.toUpperCase()) {
            case "JPEG":
                outDSStr += ".jpg";
                break;
            case "PNG":
                outDSStr += ".png";
                break;
            case "GIF":
                outDSStr += ".gif";
                break;
            case "BMP":
                outDSStr += ".bmp";
                break;
            case "GEOTIFF":
                outDSStr += ".tif";
                break;
            case "NETCDF":
                outDSStr += ".nc";
                break;
        }
        
        cmdlist.add(outDSStr);

        return cmdlist;
    }

    public static String runTranslate(WCSGetCoverageRequest req) throws WCSException {
        Runtime r = Runtime.getRuntime();

        ArrayList<String> cmds =  buildTranslateCommand(req);
        String [] cc = new String[cmds.size()];
        for (int i=0; i<cmds.size(); ++i) {
            cc[i] = cmds.get(i);
        }


        ///log.debug("Starting system command: '" + cmd + "'");
        //
        String result = null;
        
        Process p;
        try {
            
            p = r.exec(cc);
            
            boolean res = p.waitFor(Config.get().SCIDBWCS_GDALTIMOUT_SEC, TimeUnit.SECONDS);
            if (!res) {
                log.error("GDAL translate exceeded timeout of " + Config.get().SCIDBWCS_GDALTIMOUT_SEC + " seconds, aborting.");
                // timeout
            } else if (p.exitValue() != 0) {
                log.error("GDAL translate returned error (return value " + p.exitValue() + ")");
            } else {
                log.debug("GDAL translate finished. Output file written to " + cmds.get(cmds.size()-1));
                result = cmds.get(cmds.size()-1);
            }
            
            

        } catch (IOException ex) {
            log.error("Unable to start GDAL translate: " + ex);

        } catch (InterruptedException ex) {
            log.error("GDAL translate process has been interrupated: " + ex);
        }
        
        
        return result;
        

        //log.debug("Generated system command: '" + cmd + "'");
    }
}
