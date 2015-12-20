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

import org.joda.time.*;

/**
 * Metadata class for temporal references of SciDB arrays based on joda.
 */
public class TemporalReference {

    public String tdim;

    public Period dt;
    public DateTime t0;

    public TemporalReference(String tdim, String t0_str, String dt_str) {
        this.tdim = tdim;
        this.t0 = DateTime.parse(t0_str);
        this.dt = Period.parse(dt_str);
    }
    
    
    public DateTime datetimeAtIndex(Long index) {
        return t0.plus(dt.multipliedBy(index.intValue())); // Dangerous for large arrays long->int
    }

    public long indexAtDatetime(DateTime t ) {
        Long dif = t.getMillis() - t0.getMillis();
        Long delta = t0.plus(dt).getMillis() - t0.getMillis();
        Long i = (long)Math.round((double)(dif) / (double)(delta));
        
        DateTime temp = t0.plus(dt.multipliedBy(i.intValue()));
        if (temp.isAfter(t)) {
            while (temp.isAfter(t)) {
                i = i - 1L;
                temp = t0.plus(dt.multipliedBy(i.intValue()));
            }
        }
        else if (temp.isBefore(t)) {
            while (temp.isBefore(t)) {
                i = i + 1L;
                temp = t0.plus(dt.multipliedBy(i.intValue()));
            }
        }
        if (t0.plus(dt.multipliedBy(i.intValue())).isAfter(t)) i = i -1L;
        return i;
    }
}
