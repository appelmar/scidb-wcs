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

import java.text.DecimalFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class to handle affine transformations which are used to map array coordinates (integers) to world coordinates (spatially referenced) of 
 * raster data. 
 */
public class AffineTransform {

    private static final Logger log = LogManager.getLogger(AffineTransform.class);

    /** 
     * A class for 2D points
     */
    public static class double2 {

        public double x, y;

        public double2() {
            x = 0.0;
            y = 0.0;
        }

        public double2(double x, double y) {
            this.x = x;
            this.y = y;
        }
    };

    public double _x0, _y0, _a11, _a22, _a12, _a21;
    public AffineTransform _inv;

    /**
     * Default constructor creating an identity transformation
     */
    public AffineTransform() {
        this._x0 = 0.0;
        this._y0 = 0.0;
        this._a11 = 1.0;
        this._a12 = 0.0;
        this._a21 = 0.0;
        this._a22 = 1.0;
        this._inv = null;
    }

     /**
     * Creates an affine transform from a string representation
     * @param astr a string as from toString()
     */
    public AffineTransform(String astr) {

        this._x0 = 0.0;
        this._y0 = 0.0;
        this._a11 = 1.0;
        this._a12 = 0.0;
        this._a21 = 0.0;
        this._a22 = 1.0;
        this._inv = null;
        String[] parts = astr.split("[, ]");

        for (int i = 0; i < parts.length; ++i) {
            String[] kv = parts[i].split("[=:]");
            if (kv.length != 2) {
                log.warn("Cannot read affine transformation string: " + astr);
                break;
            } else if (kv[0].equals("x0")) {
                _x0 = Double.parseDouble(kv[1]);
            } else if (kv[0].equals("y0")) {
                _y0 = Double.parseDouble(kv[1]);
            } else if (kv[0].equals("a11")) {
                _a11 = Double.parseDouble(kv[1]);
            } else if (kv[0].equals("a22")) {
                _a22 = Double.parseDouble(kv[1]);
            } else if (kv[0].equals("a12")) {
                _a12 = Double.parseDouble(kv[1]);
            } else if (kv[0].equals("a21")) {
                _a21 = Double.parseDouble(kv[1]);
            } else {
                log.warn("Unknown affine transformation parameter " + kv[0] + " will be ignored");
            }
        }
    }

    
     /**
     * Creates an affine transform based on given parameters
     * @param x0 x offset
     * @param y0 y offset
     */
    public AffineTransform(double x0, double y0) {
        this._x0 = x0;
        this._y0 = y0;
        this._a11 = 1.0;
        this._a12 = 0.0;
        this._a21 = 0.0;
        this._a22 = 1.0;
        this._inv = null;

    }

    
     /**
     * Creates an affine transform based on given parameters
     * @param x0 x offset
     * @param y0 y offset
     * @param a11 transformation matrix element with (row,col) = (1,1)
     * @param a22 transformation matrix element with (row,col) = (2,2)
     */
    public AffineTransform(double x0, double y0, double a11, double a22) {
        this._x0 = x0;
        this._y0 = y0;
        this._a11 = a11;
        this._a12 = 0.0;
        this._a21 = 0.0;
        this._a22 = a22;
        this._inv = null;
    }

    /**
     * Creates an affine transform based on given parameters
     * @param x0 x offset
     * @param y0 y offset
     * @param a11 transformation matrix element with (row,col) = (1,1)
     * @param a22 transformation matrix element with (row,col) = (2,2)
     * @param a12 transformation matrix element with (row,col) = (1,2)
     * @param a21 transformation matrix element with (row,col) = (2,1)
     */
    public AffineTransform(double x0, double y0, double a11, double a22, double a12, double a21) {
        this._x0 = x0;
        this._y0 = y0;
        this._a11 = a11;
        this._a12 = a12;
        this._a21 = a21;
        this._a22 = a22;
        this._inv = null;
    }

    /**
     * Convert an affine transformation to a string
     * @return string representation of affine transformation parameters
     */
    @Override
    public String toString() {
        String out = new String();
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(16);
        out += "x0" + "=" + df.format(_x0) + " "
                + "y0" + "=" + df.format(_y0) + " "
                + "a11" + "=" + df.format(_a11) + " "
                + "a22" + "=" + df.format(_a22) + " "
                + "a12" + "=" + df.format(_a12) + " "
                + "a21" + "=" + df.format(_a21);
        return out;
    }

    /**
     * Checks whether a transformation is the identity
     * @return true if transformation equals id()
     */
    public boolean isIdentity() {
        return (_a11 == 1 && _a12 == 0 && _a21 == 0 && _a22 == 1 && _x0 == 0 && _y0 == 0);
    }

     /**
     * Applies the transformation to a given point
     * @param v Two-dimensional input point
     * @return Transformed point
     */
    public double2 f(double2 v) {
        double2 result = new double2();
        result.x = _x0 + _a11 * v.x + _a12 * v.y;
        result.y = _y0 + _a21 * v.x + _a22 * v.y;
        return result;
    }

     /**
     * Applies the transformation to a given point in place assuming that the output object has been allocated before.
     * @param v_in Two-dimensional input point
     * @param v_out Transformed point (needs to be allocated before)
     */
    public void f(double2 v_in, double2 v_out) {
        v_out.x = _x0 + _a11 * v_in.x + _a12 * v_in.y;
        v_out.y = _y0 + _a21 * v_in.x + _a22 * v_in.y;
    }

    /**
     * Applies the inverse transformation to a given point
     * @param v Two-dimensional input point
     * @return Transformed point
     */
    public double2 fInv(double2 v) {
        if (_inv == null) {
            double d = det();
            if (Double.compare(d, 0) == 0) {
                log.error("Affine transformation not invertible, det=0");

            }
            double d1 = 1 / d;
            double inv_a11 = d1 * _a22;
            double inv_a12 = d1 * (-_a12);
            double inv_a21 = d1 * (-_a21);
            double inv_a22 = d1 * _a11;
            double inv_x0 = -inv_a11 * _x0 + inv_a12 * _y0;
            double inv_y0 = inv_a21 * _x0 - inv_a22 * _y0;

            _inv = new AffineTransform(inv_x0, inv_y0, inv_a11, inv_a22, inv_a12, inv_a21);
            _inv._inv = this;
        }
        return _inv.f(v);
    }

    
    /**
     * Computes the determinant of a 2x2 matrix
     * @return determinant of a 2x2 matrix
     */
    public double det() {
        return _a11 * _a22 - _a12 * _a21;
    }

}
