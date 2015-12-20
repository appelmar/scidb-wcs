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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class for configuration of the WCS from file or dynamically in code. This is
 * a singleton class.
 */
public class Config {

    private static final Logger log = LogManager.getLogger(Config.class);
    private static Config instance = null; // Singleton instance

    private Config() {
    }

    /**
     * Returns a valid configuration instance. If not yet done it tries to read
     * settings from the configuration file. In case of any errors, default
     * values are taken.
     *
     * @return singleton instance
     */
    public static Config get() {
        if (instance == null) {
            try {
                // Try to load parameters from config file
                Gson gson = new Gson();

                String json = new String();
                List<String> lines = Files.readAllLines(new File(Constants.SCIDBWCS_PROPERTIES_FILE).toPath());
                for (int i = 0; i < lines.size(); ++i) {
                    json += lines.get(i);
                }

                instance = gson.fromJson(json, Config.class);
            } catch (Exception e) {
                // Use default parameters, no matter what exception type
                log.warn("Cannot read WCS configuration from file: " + e);
                instance = new Config();
            }
        }
        return instance;
    }

    /**
     * This methods writes a configuration file with current settings at the
     * specified location
     *
     * @param filename file to store current settings
     */
    public void store(String filename) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(this);
            PrintWriter out = new PrintWriter(filename);
            out.print(json);
            out.close();
        } catch (FileNotFoundException e) {
            log.error("Cannot store WCS configuration file: " + e);
        }
    }

    /**
     * This methods overwrites the configuration file with current settings
     */
    public void store() {
        store(Constants.SCIDBWCS_PROPERTIES_FILE);
    }

    /* The following variables represent actual configuration parameters.  Their interpretation shuld be self explanatory but some details are given in the documentation at github.
     */
    public String SCIDBWCS_DB_HOST = "localhost";
    public String SCIDBWCS_DB_PORT = "1239";
    public String SCIDBWCS_DB_USER = "scidb";
    public String SCIDBWCS_DB_PW = "scidb";
    public boolean SCIDBWCS_DB_SSL = true;
    public String SCIDBWCS_DB_SHIMPORT = "8083";
    public long SCIDBWCS_MDCACHE_REFRESH_AFTER_SEC = (long) 5 * 60;
    public String SCIDBWCS_GDALPATH = null; // If null, assumes that gdal_translate is in PATH
    public long SCIDBWCS_GDALTIMOUT_SEC = (long) 5 * 60; // 
    public String SCIDBWCS_TEMPPATH = ""; // relative path for temporary coverage files

    // WCS Server description for GetCapabilities
    public String WCS_DESCRIPTION = "This WCS prootype implementation for accessing SciDB arrays. Please notice that currently only two-dimensional coverages can be requested (i.e. either a spatial array or a temporal slice of a spatiotemporal array).";
    public String WCS_NAME = "SciDB-WCS";
    public String WCS_LABEL = "SciDB-WCS";
    public String WCS_MDLINK = null;
    public String[] WCS_KEYWORDS = {"SciDB", "WCS"};
    public String WCS_FEES = "NONE";
    public String WCS_ACCESSCONSTRAINTS = "NONE";
    public String WCS_PUBLIC_URL = "http://YOURSERVERHOST";
    public String WCS_RESPONSIBLE = null;
    public boolean WCS_ISORGANIZATION = false;

    public String WCS_CONTACT_PHONE = null;
    public String WCS_CONTACT_MAIL = null;
    public String WCS_CONTACT_COUNTRY = null;
    public String WCS_CONTACT_ZIP = null;
    public String WCS_CONTACT_STREET = null;
    public String WCS_CONTACT_CITY = null;
}
