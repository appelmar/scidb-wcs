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
 * Simple data class for storing the spatial / temporal / vertical extent of SciDB arrays
 */
public class Extent {
    
    // easting, x, longitude
    public Double xmin,xmax;
    
    // northing, y, latitude
    public Double ymin,ymax;
    
    // ISO8601 strings
    public String tmin,tmax;
    
    
    public Double zmin,zmax;

    public Extent(Double xmin, Double xmax, Double ymin, Double ymax, String tmin, String tmax, Double zmin, Double zmax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.tmin = tmin;
        this.tmax = tmax;
        this.zmin = zmin;
        this.zmax = zmax;
    }
    

}
