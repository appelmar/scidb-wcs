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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.joda.time.DateTime;
import org.n52.scidbwcs.exception.WCSException;
import org.n52.scidbwcs.md.Array;
import org.n52.scidbwcs.md.ArrayManager;
import org.n52.scidbwcs.util.Constants;

/**
 * This class imeplements WCS GetCoverage requests
 */
public class WCSGetCoverageRequest extends AbstractRequest {

    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(WCSGetCoverageRequest.class);

    private static Long curID = new Long(0);

    private Long ID = null;

    public Long getRequestID() {
        if (ID == null) {
            assignID();
        }
        return ID;
    }

    private synchronized void assignID() {
        this.ID = curID++;
    }

    public String coverage;
    public String crs;
    public String response_crs;
    public double[] bbox; // TODO: Change to BBOX type
    public String[] time;

    // TODO: Add further key value parameters (range subsetting...)
    public int width, height, depth;
    public double resx, resy, resz;
    public String format;
    public String exceptions;
    public HashMap<String, String[]> parameter;
    public String interpolation;

    private Array A = null;

    public static WCSGetCapabilitiesRequest fromXML(String xml) {
        return null;
    }

    public static WCSGetCoverageRequest fromKVP(String kvp) throws WCSException {

        final WCSGetCoverageRequest req = new WCSGetCoverageRequest();

        int getparsidx = kvp.lastIndexOf("?");

        if (getparsidx >= 0) {
            req.url = kvp.substring(0, getparsidx);
            kvp = kvp.substring(getparsidx + 1);
        } else {
            req.url = kvp;
        }

        Map<String, String> kv = new HashMap<>();
        String[] args = kvp.split("&");
        for (String a : args) {
            int idx = a.indexOf("=");
            kv.put(a.substring(0, idx), a.substring(idx + 1));
        }

        req.request = kv.getOrDefault("REQUEST", "");
        kv.remove("REQUEST");
        req.service = kv.getOrDefault("SERVICE", "");
        kv.remove("SERVICE");
        req.version = kv.getOrDefault("VERSION", "");
        kv.remove("VERSION");
        req.coverage = kv.getOrDefault("COVERAGE", "");
        kv.remove("COVERAGE");
        req.width = Integer.parseInt(kv.getOrDefault("WIDTH", "-1"));
        kv.remove("WIDTH");
        req.height = Integer.parseInt(kv.getOrDefault("HEIGHT", "-1"));
        kv.remove("HEIGHT");
        req.depth = Integer.parseInt(kv.getOrDefault("DEPTH", "-1"));
        kv.remove("DEPTH");
        req.resx = Double.parseDouble(kv.getOrDefault("RESX", "0"));
        kv.remove("RESX");
        req.resy = Double.parseDouble(kv.getOrDefault("RESY", "0"));
        kv.remove("RESY");
        req.resz = Double.parseDouble(kv.getOrDefault("RESZ", "0"));
        kv.remove("RESZ");
        req.format = kv.getOrDefault("FORMAT", "");
        kv.remove("FORMAT");
        req.interpolation = kv.getOrDefault("INTERPOLATION", "");
        kv.remove("INTERPOLATION");

        String[] bbox = kv.getOrDefault("BBOX", "").split(",");
        kv.remove("BBOX");
        if (bbox.length == 4 || bbox.length == 6) {
            req.bbox = new double[bbox.length];
            for (int i = 0; i < bbox.length; ++i) {
                req.bbox[i] = Double.parseDouble(bbox[i]);
            }
        } else { // 
            // Time musst be given
            req.bbox = null;
        }

        // Time does not support tmin/tmax/tres style
        if (kv.containsKey("TIME")) {
            String[] time = kv.getOrDefault("TIME", null).split(",");
            kv.remove("TIME");
            if (!ArrayManager.instance().getArrayMD(req.coverage).isTemporal()) {
                throw new WCSException("Array '" + req.coverage + "' has no temporal reference but TIME WCS parameter is given.", WCSException.WCS_EXCEPTION_CODE.InvalidParameterValue);
            }
            if (time.length > 1) {
                throw new WCSException("Accepts only a single TIME value.", WCSException.WCS_EXCEPTION_CODE.InvalidParameterValue);
            } else if (time.length == 1) {

                if (time[0].split("/").length != 1) {
                    throw new WCSException("Time period start/end/res not supported.", WCSException.WCS_EXCEPTION_CODE.InvalidParameterValue);
                }
                try {
                    DateTime t = DateTime.parse(time[0]);
                } catch (Exception e) {
                    throw new WCSException("Invalid datetime format, ISO 8601 expected.", WCSException.WCS_EXCEPTION_CODE.InvalidParameterValue);
                }
                req.time = new String[time.length];
                for (int i = 0; i < time.length; ++i) {
                    req.time[i] = time[i];
                }
            } else {
                req.time = null;
            }
            kv.remove("TIME");
        } else {
            req.time = null;
        }

        req.crs = kv.getOrDefault("CRS", "");
        kv.remove("CRS");
        req.response_crs = kv.getOrDefault("RESPONSE_CRS", req.crs);
        kv.remove("RESPONSE_CRS");
        req.exceptions = kv.getOrDefault("EXCEPTIONS", "application / vnd.ogc.se_xml");
        kv.remove("EXCEPTIONS");

        // Add all other parameters
        req.parameter = new HashMap<>();
        kv.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(String t, String u) {
                // a/b/c is not supported
                req.parameter.put(t, u.split(","));
            }
        });

        return req;
    }

    

    @Override
    public boolean isValid() {
        boolean v = true;
        v &= (this.bbox != null || this.time != null);
        v &= isVersionSupported();
        boolean formatSupported = false;
        for (int i = 0; i < Constants.WCS_FORMATS.length; ++i) {
            formatSupported |= Constants.WCS_FORMATS[i].equals(this.format);
        }
        v &= formatSupported;
        v &= (this.resx > 0 && this.resy > 0) || (this.width > 0 && this.height > 0);
        v &= this.request.equalsIgnoreCase("GetCapabilities");
        v &= this.service.equalsIgnoreCase("WCS");
        v &= this.crs != null && this.crs.length() > 0;

        return v;
    }

    public String getMIME() {
        String mime = "";
        switch (this.format.toUpperCase()) {
            case "JPEG":
                mime = "image/jpeg";
                break;
            case "PNG":
                mime = "image/png";
                break;
            case "GIF":
                mime = "image/gif";
                break;
            case "BMP":
                mime = "image/bmp";
                break;
            case "GEOTIFF":
                mime = "image/tiff";
                break;
            case "NETCDF":
                mime = "application/x-netcdf";
                break;
            default:
                mime = "application/xml";
                break;
        }
        return mime;
    }

    @Override
    public void run(HttpServletResponse response) throws WCSException {
        try {
            String filename = GDALWrapper.runTranslate(this);
            assert (filename != null);
            response.setContentType(this.getMIME());

            ServletOutputStream oStream = response.getOutputStream();
            FileInputStream iStream = new FileInputStream(filename);
            FileChannel iChannel = iStream.getChannel();
            byte[] buf = new byte[64 * 1024];
            ByteBuffer byteBuffer = ByteBuffer.wrap(buf);

            try {
                int l = 0;
                while (l >= 0) {
                    oStream.write(buf, 0, l);
                    byteBuffer.clear();
                    l = iChannel.read(byteBuffer);
                }
            } finally {
                iStream.close();
            }
            oStream.flush();
            oStream.close();

        } catch (IOException ex) {
            log.error(ex);
            throw new WCSException("Error while writing GetCoverage output", WCSException.WCS_EXCEPTION_CODE.InternalServerError);
        }

    }

}
