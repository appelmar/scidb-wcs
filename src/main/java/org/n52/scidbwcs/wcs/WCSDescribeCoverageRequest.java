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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.logging.log4j.LogManager;
import org.n52.scidbwcs.exception.WCSException;
import org.n52.scidbwcs.md.Array;
import org.n52.scidbwcs.md.ArrayManager;
import org.n52.scidbwcs.md.Extent;
import org.n52.scidbwcs.util.Constants;

/**
 * This class imeplements WCS DescribeCoverage requests
 */
public class WCSDescribeCoverageRequest extends AbstractRequest {
    
    
    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(AbstractRequest.class);
    
    public List<String> coverages;

    public WCSDescribeCoverageRequest(String url, String request, String service, String version, List<String> coverages) {
        this.url = url;
        this.request = request;
        this.service = service;
        this.version = version;
        this.coverages = coverages;
    }

    public WCSDescribeCoverageRequest() {}
    
    
    
    public static WCSDescribeCoverageRequest fromXML(String xml) {
        return null;
    }
    
    public static WCSDescribeCoverageRequest fromKVP(String kvp) {
         WCSDescribeCoverageRequest req = new WCSDescribeCoverageRequest();
        
        int getparsidx = kvp.lastIndexOf("?");
        
        if (getparsidx >= 0) {
            req.url = kvp.substring(0, getparsidx);
            kvp = kvp.substring(getparsidx +1);
        }
        else req.url = kvp;
        
        Map <String,String> kv = new HashMap<>();
        String[] args = kvp.split("&");
        for (String a : args) {
           int idx = a.indexOf("=");
           kv.put(a.substring(0, idx), a.substring(idx + 1));
        }
        
        req.request = kv.getOrDefault("REQUEST", "");
        req.service = kv.getOrDefault("SERVICE", "");
        req.version = kv.getOrDefault("VERSION", "");
        kv.getOrDefault("COVERAGE", "");
        
        req.coverages = new ArrayList<>();
        String [] covs = kv.getOrDefault("COVERAGE", "").split(",");
        for (String c : covs) req.coverages.add(c);

        return req;
    }
    
    
    
    
    
   
    /**
     *
     * @return
     */
    @Override
    public boolean isValid() {
        return request.equalsIgnoreCase("DescribeCoverage") && service.equalsIgnoreCase("WCS") && isVersionSupported();
        
    }

    
    @Override
    public void run(HttpServletResponse response) throws WCSException {
        try {
            List<Array> arrayMD = ArrayManager.instance().getArrayMD(this.coverages);
            
            String WCSURI = "http://schemas.opengis.net/wcs/" + Constants.WCS_VERSION + "/describeCoverage.xsd";
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            ByteArrayOutputStream buf = new ByteArrayOutputStream(1024 * 64); // 64 kB
            XMLStreamWriter writer = factory.createXMLStreamWriter(buf);
            writer.setPrefix("wcs", WCSURI);
            writer.writeStartDocument();

            writer.writeStartElement(WCSURI, "CoverageDescription");
            writer.writeAttribute("version", Constants.WCS_VERSION);
            writer.writeAttribute("xmlns:wcs","http://www.opengis.net/wcs");
            writer.writeAttribute("xmlns:xlink","http://www.opengis.net/wcs");
            writer.writeAttribute("xmlns:ogc","http://www.opengis.net/ogc" );
            writer.writeAttribute("xmlns:ows","http://www.opengis.net/ows/1.1" );
            writer.writeAttribute("xmlns:gml","http://www.opengis.net/gml" );
            writer.writeAttribute("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance" );
            writer.writeAttribute("xsi:schemaLocation", "http://www.opengis.net/wcs http://schemas.opengis.net/wcs/" + Constants.WCS_VERSION + "/describeCoverage.xsd");

            for (Array A : arrayMD) {
                writer.writeStartElement(WCSURI, "CoverageOffering");

                writer.writeStartElement(WCSURI, "name");
                writer.writeCharacters(A.getName());
                writer.writeEndElement(); // name
                writer.writeStartElement(WCSURI, "label");
                writer.writeCharacters(A.getName());
                writer.writeEndElement(); // label
                writer.writeStartElement(WCSURI, "description");
                writer.writeCharacters("Generated from SciDB array " + A.getName());
                writer.writeEndElement(); // description

                writer.writeStartElement(WCSURI, "lonLatEnvelope");
                Extent extent84 = A.getSpatialExtentWGS84();
                writer.writeAttribute("srsName", "urn:ogc:def:crs:OGC:1.3:CRS84");
                writer.writeStartElement("gml:pos");
                writer.writeCharacters(extent84.xmin + " " + extent84.xmax);
                writer.writeEndElement();
                writer.writeStartElement("gml:pos");
                writer.writeCharacters(extent84.ymin + " " + extent84.ymax);
                writer.writeEndElement();
                writer.writeEndElement(); // lonLatEnvelope

                writer.writeStartElement(WCSURI, "keywords");
                writer.writeStartElement(WCSURI, "keyword");
                writer.writeCharacters("WCS");
                writer.writeEndElement(); // keyword
                writer.writeStartElement(WCSURI, "keyword");
                writer.writeCharacters("SciDB");
                writer.writeEndElement(); // keyword
                writer.writeStartElement(WCSURI, "keyword");
                writer.writeCharacters(A.getName());
                writer.writeEndElement(); // keyword
                writer.writeEndElement(); // keywords

                writer.writeStartElement(WCSURI, "domainSet");

                if (A.isSpatial()) {

                    writer.writeStartElement(WCSURI, "spatialDomain");
                    writer.writeStartElement("gml:Envelope");
                    writer.writeAttribute("srsName", "EPSG:4326");
                    writer.writeStartElement("gml:pos");
                    writer.writeCharacters(extent84.xmin + " " + extent84.xmax);
                    writer.writeEndElement();
                    writer.writeStartElement("gml:pos");
                    writer.writeCharacters(extent84.ymin + " " + extent84.ymax);
                    writer.writeEndElement();
                    writer.writeEndElement(); // gml:Envelope
                    writer.writeStartElement("gml:RectifiedGrid");
                    writer.writeAttribute("dimension", "2");
                    writer.writeAttribute("srsName", "http://spatialreference.org/ref/" + (A.srs().auth_name).toLowerCase() + "/" + A.srs().auth_id + "/gml/");
                    writer.writeStartElement("gml:limits");
                    writer.writeStartElement("gml:GridEnvelope");
                    writer.writeStartElement("gml:low");
                    writer.writeCharacters(Math.max(A.getXDim().curMin, A.getXDim().min) + " " + Math.max(A.getYDim().curMin, A.getYDim().min));
                    writer.writeEndElement(); // gml:low
                    writer.writeStartElement("gml:high");
                    writer.writeCharacters(Math.min(A.getXDim().curMax, A.getXDim().max) + " " + Math.min(A.getYDim().curMax, A.getYDim().max));
                    writer.writeEndElement(); // gml:high
                    writer.writeEndElement(); // gml:GridEnvelope
                    writer.writeEndElement(); // gml:limits

                    writer.writeStartElement("gml:axisName");
                    writer.writeCharacters(A.getXDim().name);
                    writer.writeEndElement(); // gml:axisName
                    writer.writeStartElement("gml:axisName");
                    writer.writeCharacters(A.getYDim().name);
                    writer.writeEndElement(); // gml:axisName

                    writer.writeStartElement("gml:origin");
                    writer.writeStartElement("gml:pos");
                    writer.writeCharacters(A.srs().a._x0 + " " + A.srs().a._y0);
                    writer.writeEndElement(); // gml:pos
                    writer.writeEndElement(); // gml:origin

                    writer.writeStartElement("gml:offsetVector");
                    writer.writeCharacters(A.srs().a._a11 + " " + A.srs().a._a12);
                    writer.writeEndElement(); // gml:offsetVector

                    writer.writeStartElement("gml:offsetVector");
                    writer.writeCharacters(A.srs().a._a21 + " " + A.srs().a._a22);
                    writer.writeEndElement(); // gml:offsetVector

                    writer.writeEndElement(); // gml:RectifiedGrid
                    writer.writeEndElement(); // spatialDomain

                }

                if (A.isTemporal()) {
                    writer.writeStartElement(WCSURI, "temporalDomain");
                    
                    // This might lead to extremely large XML files for large arrays...
                    for (Long j=0L; j<A.getTDim().getTrueLength(); ++j) {
                        writer.writeStartElement("gml:timePosition");
                        writer.writeCharacters(A.trs().datetimeAtIndex(j).toString());
                        writer.writeEndElement(); // gml:timePosition
                    }
                  
                    
//                    writer.writeStartElement(WCSURI, "timePeriod");
//                    writer.writeStartElement(WCSURI, "beginPosition");
//                    writer.writeCharacters(A.trs().datetimeAtIndex(Math.max(A.getTDim().curMin, A.getTDim().min)).toString(ISODateTimeFormat.dateHourMinuteSecondFraction()));
//                    writer.writeEndElement(); // beginPosition
//                    writer.writeStartElement(WCSURI, "endPosition");
//                    writer.writeCharacters(A.trs().datetimeAtIndex( Math.min(A.getTDim().curMax, A.getTDim().max)).toString(ISODateTimeFormat.dateHourMinuteSecondFraction()));
//                    writer.writeEndElement(); // endPosition
//                    writer.writeStartElement(WCSURI, "timeResolution");
//                    writer.writeCharacters(A.trs().dt.toString());
//                    writer.writeEndElement(); // timeResolution
//                    writer.writeEndElement(); // timePeriod
                    
                    writer.writeEndElement(); // temporalDomain
                }

                writer.writeEndElement(); // domainSet

                writer.writeStartElement(WCSURI, "rangeSet");
                writer.writeStartElement(WCSURI, "RangeSet");
                writer.writeStartElement(WCSURI, "name");
                writer.writeCharacters(A.getName());
                writer.writeEndElement(); // name
                writer.writeStartElement(WCSURI, "label");
                writer.writeCharacters(A.getName());
                writer.writeEndElement(); // label

                writer.writeStartElement(WCSURI, "axisDescription");
                writer.writeStartElement(WCSURI, "AxisDescription");
                writer.writeStartElement(WCSURI, "name");
                writer.writeCharacters("Attribute");
                writer.writeEndElement(); // name
                writer.writeStartElement(WCSURI, "label");
                writer.writeCharacters("Attribute");
                writer.writeEndElement(); // label

                writer.writeStartElement(WCSURI, "values");
                writer.writeStartElement(WCSURI, "interval");
                writer.writeStartElement(WCSURI, "min");
                writer.writeCharacters("1");
                writer.writeEndElement(); // min
                writer.writeStartElement(WCSURI, "max");
                writer.writeCharacters(Integer.toString(A.Attributes().size()));
                writer.writeEndElement(); // min
                writer.writeEndElement(); // interval

                writer.writeEndElement(); // values

                writer.writeEndElement(); // AxisDescription
                writer.writeEndElement(); // axisDescription

                writer.writeEndElement(); // RangeSet

                writer.writeEndElement(); // rangeSet

                writer.writeStartElement(WCSURI, "supportedCRSs");
                writer.writeStartElement(WCSURI, "requestResponseCRSs");
                writer.writeCharacters(A.srs().auth_name + ":" + A.srs().auth_id);
                writer.writeEndElement(); // requestResponseCRSs
                writer.writeStartElement(WCSURI, "requestResponseCRSs");
                writer.writeCharacters("Image");
                writer.writeEndElement(); // requestResponseCRSs
                
//                writer.writeStartElement(WCSURI, "requestCRSs");
//                writer.writeCharacters(A.srs().auth_name + ":" + A.srs().auth_id);
//                writer.writeCharacters(" ");
//                writer.writeCharacters("Image");
//                writer.writeEndElement(); // requestCRSs
//                writer.writeStartElement(WCSURI, "responseCRSs");
//                writer.writeCharacters(A.srs().auth_name + ":" + A.srs().auth_id);
//                writer.writeCharacters(" Image");
//                writer.writeEndElement(); // responseCRSs
                writer.writeStartElement(WCSURI, "nativeCRSs");
                writer.writeCharacters(A.srs().auth_name + ":" + A.srs().auth_id);
               // writer.writeCharacters(" Image");
                writer.writeEndElement(); // nativeCRSs
                writer.writeEndElement(); // supportedCRSs

                writer.writeStartElement(WCSURI, "supportedFormats");
                for (String f : Constants.WCS_FORMATS) {
                    writer.writeStartElement(WCSURI, "formats");
                    writer.writeCharacters(f);
                    writer.writeEndElement(); // formats
                }
                writer.writeEndElement(); // supportedFormats

                writer.writeStartElement(WCSURI, "supportedInterpolations");
                writer.writeAttribute( "default",Constants.WCS_INTERPOLATIONS[0]);
                for (String f : Constants.WCS_INTERPOLATIONS) {
                    writer.writeStartElement(WCSURI, "interpolationMethod");
                    writer.writeCharacters(f);
                    writer.writeEndElement(); // interpolationMethod
                }
                writer.writeEndElement(); // supportedInterpolations

                writer.writeEndElement(); // CoverageOffering
            }

            writer.writeEndElement(); // CoverageDescription
            writer.writeEndDocument();

            writer.close();

             response.setContentType("application/xml");
             response.getWriter().print(buf.toString());

        } catch (IOException | XMLStreamException  ex) {
            log.error(ex);
            throw new WCSException("Error in Describe coverage: " + ex, WCSException.WCS_EXCEPTION_CODE.InternalServerError);
        } 
    }
    
   
    
    
}
