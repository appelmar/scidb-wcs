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
 * Metadata for SciDB array attributes
 */

public class ArrayAttribute {
    public String name;
    public String typeId;
    public String tag;
    public boolean nullable;

    public ArrayAttribute() {}
    
    
    public ArrayAttribute(String name, String typeId, String tag) {
        this.name = name;
        this.typeId = typeId;
        this.tag = tag; 
       // this.nullable = nullable;
    }
    
    public ArrayAttribute(String name, String typeId, boolean nullable) {
        this.name = name;
        this.typeId = typeId;
        this.nullable = nullable; 
        this.tag = "";
       // this.nullable = nullable;
    }

   
}
