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
package org.n52.scidbwcs.servlets;

import java.io.File;
import java.io.IOException;
import static java.net.HttpURLConnection.HTTP_OK;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.n52.scidbwcs.util.Config;
import org.n52.scidbwcs.util.Constants;
import org.n52.scidbwcs.wcs.AbstractRequest;
import org.n52.scidbwcs.wcs.WCSDescribeCoverageRequest;
import org.n52.scidbwcs.wcs.WCSGetCapabilitiesRequest;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.n52.scidbwcs.exception.WCSException;
import org.n52.scidbwcs.wcs.WCSGetCoverageRequest;

/**
 * Servlet implementation which consumes WCS requests.
 */
public class ows extends HttpServlet {

    
    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(ows.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            Constants.SCIDBWCS_PROPERTIES_FILE = new File(this.getServletContext().getResource("WEB-INF/config.properties").toURI()).getAbsolutePath();
            Constants.LOG4J_PROPERTIES_FILE = new File(this.getServletContext().getResource("WEB-INF/log4j2.xml").toURI()).getAbsolutePath();

        } catch (MalformedURLException | URISyntaxException ex) {
            log.error(ex);
        } 

        try {
            // Clean and recreate directory for temporary coverages
            FileUtils.deleteDirectory(new File(Config.get().SCIDBWCS_TEMPPATH));
        } catch (IOException ex) {
             log.error(ex);
        }

        if (!(new File(Config.get().SCIDBWCS_TEMPPATH)).mkdirs()) {
            log.error("Cannot create temporary coverage directory, using '" + System.getProperty("java.io.tmpdir") + "' instead");
            Config.get().SCIDBWCS_TEMPPATH = System.getProperty("java.io.tmpdir");
        }
    }

   
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("HTTP GET: " + request.getQueryString());
        try {

            try {

                String req = request.getParameter("REQUEST").toLowerCase();
                AbstractRequest q;

                switch (req) {
                    case "getcapabilities": {
                        q = WCSGetCapabilitiesRequest.fromKVP(request.getQueryString());
                        if (!q.isValid()) {
                            throw new WCSException("Invalid request", WCSException.WCS_EXCEPTION_CODE.InternalServerError);
                        }
                        q.run(response);
                        break;
                    }

                    case "describecoverage": {
                        q = WCSDescribeCoverageRequest.fromKVP(request.getQueryString());
                        if (!q.isValid()) {
                            throw new WCSException("Invalid request", WCSException.WCS_EXCEPTION_CODE.InternalServerError);
                        }
                        q.run(response);
                        break;
                    }

                    case "getcoverage": {
                        q = (WCSGetCoverageRequest) WCSGetCoverageRequest.fromKVP(request.getQueryString());
                        if (!q.isValid()) {
                            throw new WCSException("Invalid request", WCSException.WCS_EXCEPTION_CODE.InternalServerError);
                        }
                        q.run(response);
                        break;
                    }

                    default:
                        throw new WCSException("Unsupported operation: " + req, WCSException.WCS_EXCEPTION_CODE.InvalidParameterValue);
                }

                response.setStatus(HTTP_OK);
            } // Make ALL exception a WCS exception
            catch (Exception ex) {
                if (!(ex instanceof WCSException)) {
                    throw new WCSException(ex, WCSException.WCS_EXCEPTION_CODE.InternalServerError);
                } else {
                    throw ex;
                }
            }

        } catch (WCSException ex) {
            response.setContentType(ex.getMIME());
            //response.setStatus(e.getCode().getHTTPCode());
            response.setStatus(HTTP_OK);
            response.getWriter().print(ex.toXML());  
        }

    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return Config.get().WCS_DESCRIPTION;
    }// </editor-fold>

}
