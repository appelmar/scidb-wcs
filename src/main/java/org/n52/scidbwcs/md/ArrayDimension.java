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
 * Metadata for SciDB dimensions
 */
public class ArrayDimension {
    
    private static final Long INT64MAX = 4611686018427387903L;
  
    public String name;
    public Long min;
    public Long max;
    public Long curMin;
    public Long curMax;
    public Long chunkSize;
    public Long overlap;
    
    
    public Long getTrueMin() {
        if(Math.abs(curMin) == INT64MAX) {
            return min; 
        }
        return curMin;
    }
    
    public Long getTrueMax() {
        if(Math.abs(curMax) == INT64MAX) {
            return max; 
        }
        return curMax;
    }
    
    public Long getTrueLength() {
        
        return getTrueMax()-getTrueMin() +1;
    }
    
    public ArrayDimension(String name) {
        this.name = name;
        min = null;
        max = null;
        curMin = null;
        curMax = null;
        chunkSize = null;
        overlap = null;
    }

    public ArrayDimension(String name, Long min, Long max, Long curMin, Long curMax, Long chunkSize, Long overlap) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.curMin = curMin;
        this.curMax = curMax;
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }
    
    
    
}
