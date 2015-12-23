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
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.logging.log4j.LogManager;
import org.n52.scidbwcs.exception.WCSException;
import org.n52.scidbwcs.md.Array;
import org.n52.scidbwcs.md.ArrayManager;
import org.n52.scidbwcs.md.Extent;
import org.n52.scidbwcs.util.Config;
import org.n52.scidbwcs.util.Constants;

/**
 * This class imeplements WCS GetCapabilities requests
 */
public class WCSGetCapabilitiesRequest extends AbstractRequest {
    
    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(WCSGetCapabilitiesRequest.class);


    public String section;
    public String updatesequence;
    
  
    
    
    @Override
    public boolean isValid() {
        return request.equalsIgnoreCase("GetCapabilities") && service.equalsIgnoreCase("WCS") && isVersionSupported();
        
    }
    
    public static WCSGetCapabilitiesRequest fromXML(String xml) {
        return null;
    }
    
    
    
    public static WCSGetCapabilitiesRequest fromKVP(String kvp) {
        WCSGetCapabilitiesRequest req = new WCSGetCapabilitiesRequest();
        
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
        req.section = kv.getOrDefault("SECTION", "");
        req.updatesequence = kv.getOrDefault("UPDATESEQUENCE", "");
        return req;
    }

    @Override
    public void run(HttpServletResponse response) throws WCSException {
        try {
            String WCSURI = "http://schemas.opengis.net/wcs/" + Constants.WCS_VERSION + "/getCoverage.xsd";
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            ByteArrayOutputStream buf = new ByteArrayOutputStream(1024 * 64); // 64 kB
            XMLStreamWriter writer = factory.createXMLStreamWriter(buf);
            writer.setPrefix("wcs", WCSURI);
            writer.writeStartDocument();

            writer.writeStartElement(WCSURI, "WCS_Capabilities");
            writer.writeAttribute("version", Constants.WCS_VERSION);
            writer.writeAttribute("xmlns:wcs","http://www.opengis.net/wcs");
            writer.writeAttribute("xmlns:xlink","http://www.opengis.net/wcs");
            writer.writeAttribute("xmlns:ogc","http://www.opengis.net/ogc" );
            writer.writeAttribute("xmlns:ows","http://www.opengis.net/ows/1.1" );
            writer.writeAttribute("xmlns:gml","http://www.opengis.net/gml" );
            writer.writeAttribute("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance" );
            writer.writeAttribute("xsi:schemaLocation", "http://www.opengis.net/wcs http://schemas.opengis.net/wcs/" + Constants.WCS_VERSION + "/wcsCapabilities.xsd");

            ///////////////////////////////////////////////////////////////////////////////////////////////////
            writer.writeStartElement(WCSURI, "Service");
            writer.writeStartElement(WCSURI, "name");
            writer.writeCharacters(Config.get().WCS_NAME);
            writer.writeEndElement();
            writer.writeStartElement(WCSURI, "description");
            writer.writeCharacters(Config.get().WCS_DESCRIPTION);
            writer.writeEndElement();
            writer.writeStartElement(WCSURI, "label");
            writer.writeCharacters(Config.get().WCS_LABEL);
            writer.writeEndElement();
            writer.writeStartElement(WCSURI, "fees");
            writer.writeCharacters(Config.get().WCS_FEES);
            writer.writeEndElement();

            if (Config.get().WCS_KEYWORDS != null && Config.get().WCS_KEYWORDS.length > 0) {
                writer.writeStartElement(WCSURI, "keywords");
                for (String s : Config.get().WCS_KEYWORDS) {
                    writer.writeStartElement(WCSURI, "keyword");
                    writer.writeCharacters(s);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            if (Config.get().WCS_ACCESSCONSTRAINTS != null) {
                writer.writeStartElement(WCSURI, "accessConstraints");
                writer.writeCharacters(Config.get().WCS_ACCESSCONSTRAINTS);
                writer.writeEndElement();
            }

            if (Config.get().WCS_MDLINK != null) {
                writer.writeStartElement(WCSURI, "metadataLink");
                writer.writeCharacters(Config.get().WCS_MDLINK);
                writer.writeEndElement();
            }

            if (Config.get().WCS_RESPONSIBLE != null) {
                writer.writeStartElement(WCSURI, "responsibleParty");
                if (Config.get().WCS_ISORGANIZATION) {
                    writer.writeStartElement(WCSURI, "organisationName");
                } else {
                    writer.writeStartElement(WCSURI, "individualName");
                }
                writer.writeCharacters(Config.get().WCS_RESPONSIBLE);
                writer.writeEndElement();
                writer.writeStartElement(WCSURI, "contactInfo");
                writer.writeStartElement(WCSURI, "phone");
                if (Config.get().WCS_CONTACT_PHONE != null) {
                    writer.writeStartElement(WCSURI, "voice");
                    writer.writeCharacters(Config.get().WCS_CONTACT_PHONE);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
                writer.writeStartElement(WCSURI, "address");
                if (Config.get().WCS_CONTACT_CITY != null) {
                    writer.writeStartElement(WCSURI, "city");
                    writer.writeCharacters(Config.get().WCS_CONTACT_CITY);
                    writer.writeEndElement();
                }
                if (Config.get().WCS_CONTACT_ZIP != null) {
                    writer.writeStartElement(WCSURI, "postalCode");
                    writer.writeCharacters(Config.get().WCS_CONTACT_ZIP);
                    writer.writeEndElement();
                }
                if (Config.get().WCS_CONTACT_COUNTRY != null) {
                    writer.writeStartElement(WCSURI, "country");
                    writer.writeCharacters(Config.get().WCS_CONTACT_COUNTRY);
                    writer.writeEndElement();
                }
                if (Config.get().WCS_CONTACT_MAIL != null) {
                    writer.writeStartElement(WCSURI, "electronicMailAddress");
                    writer.writeCharacters(Config.get().WCS_CONTACT_MAIL);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
                writer.writeEndElement();

                writer.writeEndElement();
            }

            writer.writeEndElement();

            ///////////////////////////////////////////////////////////////////////////////////////////////////
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            writer.writeStartElement(WCSURI, "Capability");

            writer.writeStartElement(WCSURI, "Request");
            writer.writeStartElement(WCSURI, "GetCapabilities");
            writer.writeStartElement(WCSURI, "DCPType");
            writer.writeStartElement(WCSURI, "HTTP");
            writer.writeStartElement(WCSURI, "Get");
            writer.writeStartElement(WCSURI, "OnlineResource");
            writer.writeAttribute("xlink:href", Config.get().WCS_PUBLIC_URL);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
//            writer.writeStartElement(WCSURI, "DCPType");
//            writer.writeStartElement(WCSURI, "HTTP");
//            writer.writeStartElement(WCSURI, "Post");
//            writer.writeStartElement(WCSURI, "OnlineResource");
//            writer.writeAttribute("xlink:href", Config.get().WCS_PUBLIC_URL);
//            writer.writeEndElement();
//            writer.writeEndElement();
//            writer.writeEndElement();
//            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeStartElement(WCSURI, "DescribeCoverage");
            writer.writeStartElement(WCSURI, "DCPType");
            writer.writeStartElement(WCSURI, "HTTP");
            writer.writeStartElement(WCSURI, "Get");
            writer.writeStartElement(WCSURI, "OnlineResource");
            writer.writeAttribute("xlink:href", Config.get().WCS_PUBLIC_URL);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
//            writer.writeStartElement(WCSURI, "DCPType");
//            writer.writeStartElement(WCSURI, "HTTP");
//            writer.writeStartElement(WCSURI, "Post");
//            writer.writeStartElement(WCSURI, "OnlineResource");
//            writer.writeAttribute("xlink:href", Config.get().WCS_PUBLIC_URL);
//            writer.writeEndElement();
//            writer.writeEndElement();
//            writer.writeEndElement();
//            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeStartElement(WCSURI, "GetCoverage");
            writer.writeStartElement(WCSURI, "DCPType");
            writer.writeStartElement(WCSURI, "HTTP");
            writer.writeStartElement(WCSURI, "Get");
            writer.writeStartElement(WCSURI, "OnlineResource");
            writer.writeAttribute("xlink:href", Config.get().WCS_PUBLIC_URL);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
//            writer.writeStartElement(WCSURI, "DCPType");
//            writer.writeStartElement(WCSURI, "HTTP");
//            writer.writeStartElement(WCSURI, "Post");
//            writer.writeStartElement(WCSURI, "OnlineResource");
//            writer.writeAttribute("xlink:href", Config.get().WCS_PUBLIC_URL);
//            writer.writeEndElement();
//            writer.writeEndElement();
//            writer.writeEndElement();
//            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeStartElement(WCSURI, "Exception");
            writer.writeStartElement(WCSURI, "Format");
            writer.writeCharacters("application/vnd.ogc.se_xml");
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            ///////////////////////////////////////////////////////////////////////////////////////////////////

            // Get all coverages
            //List<Array> arrayMD = Array.getArrayMDAll2();
            List<Array> arrayMD = ArrayManager.instance().getArrayMD_JDBC();

            
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            writer.writeStartElement(WCSURI, "ContentMetadata");
            for (Array A : arrayMD) {
                writer.writeStartElement(WCSURI, "CoverageOfferingBrief");

                writer.writeStartElement(WCSURI, "name");
                writer.writeCharacters(A.getName());
                writer.writeEndElement();
                writer.writeStartElement(WCSURI, "description");
                writer.writeCharacters(A.toString());
                writer.writeEndElement();
                writer.writeStartElement(WCSURI, "label");
                writer.writeCharacters(A.getName());
                writer.writeEndElement();
                // TODO: Add keywords here  

                try {

                    Extent extent84 = A.getSpatialExtentWGS84();

                    writer.writeStartElement(WCSURI, "lonLatEnvelope");
                    writer.writeAttribute("srsName", "urn:ogc:def:crs:OGC:1.3:CRS84");

                    writer.writeStartElement("gml:pos");
                    writer.writeCharacters(extent84.xmin + " " + extent84.xmax);
                    writer.writeEndElement();
                    writer.writeStartElement("gml:pos");
                    writer.writeCharacters(extent84.ymin + " " + extent84.ymax);
                    writer.writeEndElement();

                    writer.writeEndElement();
                } catch (Exception ex) {
                    //Logger.getLogger(SciDBWCSImpl.class.getName()).log(Level.SEVERE, null, ex);
                }

                writer.writeEndElement();
            }
            writer.writeEndElement();
            ///////////////////////////////////////////////////////////////////////////////////////////////////

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();

            response.setContentType("application/xml");
            response.getWriter().print(buf.toString());
            
        } catch (XMLStreamException | IOException  ex) {
            log.error(ex);
            throw new WCSException("Error while writing GetCapabilities response: " + ex, WCSException.WCS_EXCEPTION_CODE.InternalServerError);
        } 
    }
}
