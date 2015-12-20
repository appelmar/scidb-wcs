/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidbwcs.wcs;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.n52.scidbwcs.util.Constants;


public class WCSGetCapabilitiesRequestTest {
    
    public WCSGetCapabilitiesRequestTest() {
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
     * Test of isVersionSupported method, of class WCSGetCapabilitiesRequest.
     */
    @org.junit.Test
    public void testIsVersionSupported() {
        WCSGetCapabilitiesRequest c = new WCSGetCapabilitiesRequest();
      
        c.version = Constants.WCS_VERSION;
        assertEquals(c.isVersionSupported(), true);
        c.version = "1";
        assertEquals(c.isVersionSupported(), false);

    }

    /**
     * Test of isValid method, of class WCSGetCapabilitiesRequest.
     */
    @org.junit.Test
    public void testIsValid() {
        WCSGetCapabilitiesRequest c = new WCSGetCapabilitiesRequest();
      
        c.request = "GetCapabilities";
        c.service= "WCS";
        c.version = Constants.WCS_VERSION;
        assertEquals(c.isValid(), true);
        c.request = "";
        assertEquals(c.isValid(), false);
    }

    /**
     * Test of fromXML method, of class WCSGetCapabilitiesRequest.
     */
    @org.junit.Test
    public void testFromXML() {
//        System.out.println("fromXML");
//        String xml = "";
//        WCSGetCapabilitiesRequest expResult = null;
//        WCSGetCapabilitiesRequest result = WCSGetCapabilitiesRequest.fromXML(xml);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of fromKVP method, of class WCSGetCapabilitiesRequest.
     */
    @org.junit.Test
    public void testFromKVP() {
        String test_GetCapabilities_KVP = "http://xyz.com/?REQUEST=GetCapabilities&SERVICE=WCS&VERSION=1.0.0";
        WCSGetCapabilitiesRequest c = WCSGetCapabilitiesRequest.fromKVP(test_GetCapabilities_KVP);
        assertEquals(c.request, "GetCapabilities");
        assertEquals(c.url, "http://xyz.com/");
        assertEquals(c.service, "WCS");
        assertEquals(c.version, "1.0.0");
        
    }
    
}
