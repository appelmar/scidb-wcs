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

/**
 * Metadata class for spatial references of SciDB arrays based.
 */
public class SpatialReference {
    public AffineTransform a;
    public String xdim,ydim;
    public String auth_name;
    public int auth_id;
    public String proj4;
    public String wkt;

    public SpatialReference(AffineTransform a, String xdim, String ydim, String auth_name, int auth_id, String proj4, String wkt) {
        this.a = a;
        this.xdim = xdim;
        this.ydim = ydim;
        this.auth_name = auth_name;
        this.auth_id = auth_id;
        this.proj4 = proj4;
        this.wkt = wkt;
    }
    
    
}
