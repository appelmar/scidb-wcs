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

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * A simple representation of geographic SciDB arrays. This class holds only array metadata but not acutal array data.
 */
public class Array {

    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(Array.class);
    private String name;
    private List<ArrayAttribute> attributes;
    private List<ArrayDimension> dimensions;

    private ArrayDimension xdim = null;
    private ArrayDimension ydim = null;
    private ArrayDimension tdim = null;
    private ArrayDimension vdim = null;

    private SpatialReference srs;

    public void setAttributes(List<ArrayAttribute> attributes) {
        this.attributes = attributes;
    }

    public void setDimensions(List<ArrayDimension> dimensions) {
        this.dimensions = dimensions;
    }

    public void setSrs(SpatialReference srs) {
        this.srs = srs;
    }

    public void setTrs(TemporalReference trs) {
        this.trs = trs;
    }

    public void setExtent(Extent extent) {
        this.extent = extent;
    }
    private TemporalReference trs;
    private Extent extent;
    private Extent extentWGS84;

    public Array() {
        name = "";
        attributes = new ArrayList<>();
        dimensions = new ArrayList<>();
        srs = null;
        trs = null;
    }

    public Array(String name) {
        this.name = name;
        attributes = new ArrayList<>();
        dimensions = new ArrayList<>();
        srs = null;
        trs = null;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the easting / longitude  array dimension
     * @return single array dimension dedicated to easting / longitude
     */
    public ArrayDimension getXDim() {
        if (srs() == null) {
            return null;
        }
        if (xdim == null) {
            for (int i = 0; i < dimensions.size(); ++i) {
                if (dimensions.get(i).name.equals(srs().xdim)) {
                    xdim = dimensions.get(i);
                    break;
                }
            }
        }
        return xdim;
    }

    /**
     * Gets the northing / latitude  array dimension
     * @return single array dimension dedicated to northing / latitude
     */
    public ArrayDimension getYDim() {
        if (srs() == null) {
            return null;
        }
        if (ydim == null) {
            for (int i = 0; i < dimensions.size(); ++i) {
                if (dimensions.get(i).name.equals(srs().ydim)) {
                    ydim = dimensions.get(i);
                    break;
                }
            }
        }
        return ydim;
    }

    /**
     * Gets the temporal array dimension
     * @return single array dimension dedicated to time
     */
    public ArrayDimension getTDim() {
        if (trs() == null) {
            return null;
        }
        if (tdim == null) {
            for (int i = 0; i < dimensions.size(); ++i) {
                if (dimensions.get(i).name.equals(trs().tdim)) {
                    tdim = dimensions.get(i);
                    break;
                }
            }
        }
        return tdim;
    }

    /**
     * Checks whether an array is spatial
     * @return true if array is spatially referenced
     */
    public boolean isSpatial() {
        return (srs != null);
    }

    /**
     * Checks whether an array is temporal
     * @return 
     */
    public boolean isTemporal() {
        return (trs != null);
    }

    /** 
     * Returns a list of array attributes
     * @return list of array attributes
     */
    public List<ArrayAttribute> Attributes() {
        return attributes;
    }

    /**
     * Returns a list of array dimensions
     * @return list of array dimensions
     */
    public List<ArrayDimension> Dimensions() {
        return dimensions;
    }

    
    /**
     * Gets the spatial reference of an array
     * @return spatial reference metadata
     */
    public SpatialReference srs() {
        return srs;
    }

    
    /**
     * Gets the spatial / temporal extent
     * @return Extent metadata
     */
    public Extent extent() {
        return extent;
    }

    /**
     * Gets the temporal reference of an array
     * @return temporal reference metadata
     */
    public TemporalReference trs() {
        return trs;
    }

    /**
     * Computes the spatial extent 
     * @return 
     */
    public Extent getSpatialExtentWGS84() {
        if (this.extentWGS84 != null) return this.extentWGS84;
        try {
            CoordinateReferenceSystem crs = null;
            try {
                crs = CRS.decode(srs().auth_name + ":" + srs().auth_id, true);
            } catch (Exception e) {
                // Try this if 1 does not succeed
                crs = CRS.parseWKT(srs().wkt);
            }

            CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
            MathTransform transform = CRS.findMathTransform(crs, targetCRS, true);

            double[] pts = {extent().xmin, extent().ymin, extent().xmin, extent().ymax, extent().xmax, extent().ymax, extent().xmax, extent().ymin};
            transform.transform(pts, 0, pts, 0, 4);
            double xmin = Double.POSITIVE_INFINITY;
            double xmax = Double.NEGATIVE_INFINITY;
            double ymin = Double.POSITIVE_INFINITY;
            double ymax = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < 4; ++i) {
                if (pts[2 * i] < xmin) {
                    xmin = pts[2 * i];
                }
                if (pts[2 * i] > xmax) {
                    xmax = pts[2 * i];
                }
                if (pts[2 * i + 1] < ymin) {
                    ymin = pts[2 * i + 1];
                }
                if (pts[2 * i + 1] > ymax) {
                    ymax = pts[2 * i + 1];
                }
            }
            if (CRS.getAxisOrder(crs) != CRS.getAxisOrder(targetCRS)) {
               this.extentWGS84 = new Extent(ymin, ymax, xmin, xmax, null, null, null, null);
             
            }
            else {
                this.extentWGS84 = new Extent(xmin, xmax, ymin, ymax, null, null, null, null);
            }
            return this.extentWGS84;

        } catch (Exception ex) {
            log.info("Cannot derive spatial extent in WGS84 for array " + name + ". Setting to (0,0,0,0): " + ex);
            return new Extent(0.0, 0.0, 0.0, 0.0, null, null, null, null);
        }

    }


    /**
     * Returns the array schema as a string
     * @return array schema 
     */
    @Override
    public String toString() {
        String out = this.name;
        out += "<";
        for (int i = 0; i < attributes.size() - 1; ++i) {
            out += attributes.get(i).name + ":" + attributes.get(i).typeId + " ";
            if (attributes.get(i).nullable) {
                out += "null ";
            }
            out += ",";
        }
        if (!attributes.isEmpty()) {
            out += attributes.get(attributes.size() - 1).name + ":" + attributes.get(attributes.size() - 1).typeId + " ";
            if (attributes.get(attributes.size() - 1).nullable) {
                out += "null ";
            }
        }
        out += ">";
        out += "[";
        for (int i = 0; i < dimensions.size() - 1; ++i) {
            out += dimensions.get(i).name + "=";
            out += (dimensions.get(i).min != null) ? dimensions.get(i).min : "?";
            out += ":";
            out += (dimensions.get(i).max != null) ? dimensions.get(i).max : "?";
            out += ",";
            out += (dimensions.get(i).chunkSize != null) ? dimensions.get(i).chunkSize : "?";
            out += ",";
            out += (dimensions.get(i).overlap != null) ? dimensions.get(i).overlap : "?";
            out += ",";
        }
        if (!dimensions.isEmpty()) {
            int i = dimensions.size() - 1;
            out += dimensions.get(i).name + "=";
            out += (dimensions.get(i).min != null) ? dimensions.get(i).min : "?";
            out += ":";
            out += (dimensions.get(i).max != null) ? dimensions.get(i).max : "?";
            out += ",";
            out += (dimensions.get(i).chunkSize != null) ? dimensions.get(i).chunkSize : "?";
            out += ",";
            out += (dimensions.get(i).overlap != null) ? dimensions.get(i).overlap : "?";
            out += "]";
        }

        return out;
    }

}
