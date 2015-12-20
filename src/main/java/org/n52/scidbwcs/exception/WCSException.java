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
package org.n52.scidbwcs.exception;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import static java.net.HttpURLConnection.*;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.logging.log4j.LogManager;
import org.n52.scidbwcs.util.Constants;

/**
 * General class to handle Exceptions in WCS in order to serve OGC conform XML exception codes 
 */
public class WCSException extends Exception {
    
    
    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(WCSException.class);
 
    /**
     * Enum that holds standardized and custom error codes and messages
     */
    public enum WCS_EXCEPTION_CODE {
        InvalidFormat("Invalid output file format", HTTP_BAD_REQUEST),
        CoverageNotDefined("Undefined coverage", HTTP_BAD_REQUEST),
        CurrentUpdateSequence("Current update sequence", HTTP_BAD_REQUEST),
        InvalidUpdateSequence("Invalid update sequence", HTTP_BAD_REQUEST),
        MissingParameterValue("Missing parameter", HTTP_BAD_REQUEST),
        InvalidParameterValue("Invalid parameter value", HTTP_BAD_REQUEST),
        InternalServerError("Internal server error",HTTP_INTERNAL_ERROR);
        
        private WCS_EXCEPTION_CODE(String msg,int httpcode) {
            this.msg = msg;
            this.httpcode = httpcode;
        }
        /**
         * The exception's standard message
         */
        private final String msg; 
        
        /** 
         * HTTP return code that should be output to clients
         */
        private int httpcode;

        /**
         * Gets the exception message
         * @return message as a string
         */
        public String getMsg() {
            return msg;
        }

         /**
         * Gets the HTTP return code that should be output to clients
         * @return HTTP status code as integer
         */
        public int getHTTPCode() {
            return httpcode;
        }
        
        
    };
    
    private WCS_EXCEPTION_CODE code;
   
    public WCS_EXCEPTION_CODE getCode() {
        return code;
    }
    public WCSException(String msg, WCS_EXCEPTION_CODE code) {
        super(msg);
        this.code = code;
        log.debug("Generated WCS Exception '" + code.name() + "': " + msg);      
    }
    
    public WCSException(Throwable e, WCS_EXCEPTION_CODE code) {
        super(e);
        this.code = code;
        log.debug("Generated WCS Exception (from different exception) '" + code.name() + "': " + e.getMessage());      
    }
    
    
    
    public String getMIME() {
        return "application/vnd.ogc.se_xml";
    }
    
    /**
     * This function builds OGC standardized XML serialized exceptions
     * @return Exception in XML format as a string
     */
    public String toXML() {
        
        try {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            ByteArrayOutputStream buf = new ByteArrayOutputStream(1024); // 1 kB
            XMLStreamWriter writer = factory.createXMLStreamWriter(buf);
           
            writer.writeStartDocument();

            writer.writeStartElement("ServiceExceptionReport");
            writer.writeAttribute("version", Constants.WCS_VERSION);
            
            writer.writeStartElement("ServiceException");
            writer.writeAttribute("exceptionCode", code.name());
            // locator attribute is optional
            
            writer.writeCharacters(code.getMsg() + ": ");
            writer.writeCharacters(super.toString() + "\nStack trace: ");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            this.printStackTrace(pw);
            writer.writeCharacters(sw.toString());
            
            
            writer.writeEndElement(); // ServiceException
            
            writer.writeEndElement(); // ServiceExceptionReport
            writer.writeEndDocument();
            
            writer.close();

            return buf.toString("UTF-8");
            
        } catch (XMLStreamException | UnsupportedEncodingException ex) {
            log.error(ex);
        }
    
        // Make sure that there is ALWAYS a default XML exception output
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ServiceExceptionReport version=\"" + Constants.WCS_VERSION + "\" >   <ServiceException code=\"" + WCS_EXCEPTION_CODE.InternalServerError.name() +"\" >" + super.toString() + "</ServiceException></ServiceExceptionReport>";
        return xml;
        
    }
    
}
