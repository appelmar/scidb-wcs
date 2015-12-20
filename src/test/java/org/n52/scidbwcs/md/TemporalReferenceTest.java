/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidbwcs.md;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Marius
 */
public class TemporalReferenceTest {
    
    public TemporalReferenceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of datetimeAtIndex method, of class TemporalReference.
     */
    @Test
    public void testDatetimeAtIndex() {
        
        TemporalReference t;
        
        t= new TemporalReference("t", "2001-01-01T00:00:00", "P1D");
        assertEquals(t.datetimeAtIndex(1L).isEqual(DateTime.parse("2001-01-02T00:00:00")), true);
        assertEquals(t.datetimeAtIndex(0L).isEqual(DateTime.parse("2001-01-01T00:00:00")), true);

        t = new TemporalReference("t", "2001-01-01T00:00:00", "P1M");
        assertEquals(t.datetimeAtIndex(1L).isEqual(DateTime.parse("2001-02-01T00:00:00")), true);
        
        t = new TemporalReference("t", "2001-01-01T00:00:00", "P1Y");
        assertEquals(t.datetimeAtIndex(1L).isEqual(DateTime.parse("2002-01-01T00:00:00")), true);
        
        t = new TemporalReference("t", "2001-01-01T00:00:00", "PT1h2m");
        assertEquals(t.datetimeAtIndex(2L).isEqual(DateTime.parse("2001-01-01T02:04:00")), true);
    }

    /**
     * Test of indexAtDatetime method, of class TemporalReference.
     */
    @Test
    public void testIndexAtDatetime() {
        TemporalReference t;
        
        t= new TemporalReference("t", "2001-01-01T00:00:00", "P1D");
        assertEquals(t.indexAtDatetime(DateTime.parse("2001-01-03T00:00:00")),2L);
        assertEquals(t.indexAtDatetime(DateTime.parse("2001-01-01T00:00:00")),0L);
        assertEquals(t.indexAtDatetime(DateTime.parse("2000-12-30T00:00:00")),-2L);
        
        
        t = new TemporalReference("t", "2001-01-01T00:00:00", "P1M");
        assertEquals(t.indexAtDatetime(DateTime.parse("2001-01-01T00:00:00")),0L);
        assertEquals(t.indexAtDatetime(DateTime.parse("2001-01-03T00:00:00")),0L);
        assertEquals(t.indexAtDatetime(DateTime.parse("2001-02-01T00:00:00")),1L);
        assertEquals(t.indexAtDatetime(DateTime.parse("2001-02-03T00:00:00")),1L);
        assertEquals(t.indexAtDatetime(DateTime.parse("2002-01-01T00:00:00")),12L);
        assertEquals(t.indexAtDatetime(DateTime.parse("2002-01-31T00:00:00")),12L);
        assertEquals(t.indexAtDatetime(DateTime.parse("2000-12-31T00:00:00")),-1L);
        
        t = new TemporalReference("t", "2001-01-01T00:00:00", "P1Y");
        
        
        
        t = new TemporalReference("t", "2001-01-01T00:00:00", "PT1h2m");
        assertEquals(t.indexAtDatetime(DateTime.parse("2001-01-01T02:04:01")),2L);
        
        
    }
    
}
