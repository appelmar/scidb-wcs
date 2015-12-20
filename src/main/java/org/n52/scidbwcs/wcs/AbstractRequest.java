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

import javax.servlet.http.HttpServletResponse;
import org.n52.scidbwcs.exception.WCSException;

/**
 *
 * @author Marius
 */
public abstract class AbstractRequest {
    
    public String url;
    public String service;
    public String version;
    public String request;
    
    
    public boolean isVersionSupported() {
        return version.equalsIgnoreCase(org.n52.scidbwcs.util.Constants.WCS_VERSION);
    }
    
    public abstract boolean isValid();
    
     
    public abstract void run(HttpServletResponse response) throws WCSException;

    
}
