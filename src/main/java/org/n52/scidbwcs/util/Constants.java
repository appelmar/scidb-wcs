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
package org.n52.scidbwcs.util;

/**
 * Collection of global constants
 */
public class Constants {

    /**
     * Relative path of the JSON configuration file
     */
    public static String SCIDBWCS_PROPERTIES_FILE = "config.properties";

    /**
     * Relative path of the log4j3 configuration file
     */
    public static String LOG4J_PROPERTIES_FILE = "src/log4j.properties";

    /**
     * WCS version of this implementation
     */
    public final static String WCS_VERSION = "1.0.0";

    /**
     * File formats supported by this WCS
     */
    public final static String[] WCS_FORMATS = {"GeoTIFF", "JPEG", "PNG", "BMP", "NetCDF"}; // TODO: Add more from GDAL?

    /**
     * Interpolations supported by this WCS
     */
    public final static String[] WCS_INTERPOLATIONS = {"nearest", "bilinear", "bicubic"};
}
