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
package org.n52.scidbwcs.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.n52.scidbwcs.util.Config;

/**
 * This class uses SciDB's web service Shim to communicate with the database. It
 * is aimed at replacing JDBC usage in future releases. This class is a
 * singleton.
 */
public class ShimClient {

    private static final String SHIM_ENDPOINT_LOGIN = "/login";
    private static final String SHIM_ENDPOINT_NEWSESSION = "/new_session";
    private static final String SHIM_ENDPOINT_RELEASESESSION = "/release_session";
    private static final String SHIM_ENDPOINT_EXECUTEQUERY = "/execute_query";
    private static final String SHIM_ENDPOINT_READLINES = "/read_lines";
    private static final String SHIM_ENDPOINT_READBYTES = "/read_bytes";
    private static final String SHIM_ENDPOINT_UPLOADFILE = "/upload_file";
    private static final String SHIM_ENDPOINT_UPLOAD = "/upload";
    private static final String SHIM_ENDPOINT_CANCEL = "/cancel";

    private static final Logger log = LogManager.getLogger(ShimClient.class);

    /* General connection variables */
    private String host = "localhost";
    private String port = "8083";
    private String user = "scidb";
    private String pw = "scidb";
    private boolean ssl = true;
    private String url = null;
    private boolean auth = true;

    /* Status variables */
    private String curSessionID = null;
    private String curQueryID = null;
    private String curAuthToken = null;

    /* Singleton instance */
    private static ShimClient instance = null;

    /**
     * Get the class instance or create a new instance if necessary
     *
     * @return singleton instance
     */
    public static ShimClient get() {
        if (instance == null) {
            instance = new ShimClient();
        }
        return instance;
    }

    private ShimClient() {
        host = Config.get().SCIDBWCS_DB_HOST;
        port = Config.get().SCIDBWCS_DB_SHIMPORT;
        user = Config.get().SCIDBWCS_DB_USER;
        pw = Config.get().SCIDBWCS_DB_PW;
        ssl = Config.get().SCIDBWCS_DB_SSL;
        auth = ssl; // TODO: Check whether unauthenticated SSL and authenticated NON SSL is possible
        url = (ssl ? "https" : "http") + "://" + host + ":" + port;

        // Accept certificate of Shim if it runs on the same server
        if (host.equalsIgnoreCase("localhost") || host.equalsIgnoreCase("127.0.0.1")) {
            X509TrustManager x = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            HostnameVerifier v = new HostnameVerifier() {
                @Override
                public boolean verify(String string, SSLSession ssls) {
                    return true;
                }
            };

            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, new TrustManager[]{x}, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(v);

            } catch (KeyManagementException ex) {
                java.util.logging.Logger.getLogger(ShimClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException ex) {
                java.util.logging.Logger.getLogger(ShimClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static String parsToUrlString(Map<String, String> pars) {
        if (pars.isEmpty()) {
            return "";
        }
        String out = "?";
        try {
            String k0 = (String) pars.keySet().toArray()[0];
            out += k0 + "=" + URLEncoder.encode(pars.get(k0), "UTF-8");
            pars.remove(k0);

            for (Map.Entry<String, String> entry : pars.entrySet()) {
                out += "&" + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
            }
            return out;
        } catch (UnsupportedEncodingException ex) {
            return out;
        }

    }

    /**
     * use the static constructor to ignore HTTPS certificate checks if Shim
     * runs on localhost
     */
    private String performSimpleHTTPGet(URL u) {
        return performSimpleHTTPGet(u, false);
    }

    private String performSimpleHTTPGet(URL u, boolean keep_newline) {
        try {

            if (ssl) {
                HttpsURLConnection con = (HttpsURLConnection) u.openConnection();
                con.setReadTimeout(10000);
                con.setRequestMethod("GET");
                log.debug("Performing HTTPS GET: " + u);
                int responseCode = con.getResponseCode();

                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    log.error("HTTPS GET returned code " + responseCode);
                } else {
                    log.debug("HTTPS GET returned HTTP_OK");
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                StringBuffer response = new StringBuffer();

                while ((line = in.readLine()) != null) {
                    response.append(line);
                    if (keep_newline) {
                        response.append("\n");
                    }
                }
                in.close();
                con.disconnect();
                return response.toString();

            } else {
                HttpURLConnection con = (HttpURLConnection) u.openConnection();
                //con.setReadTimeout(10000);
                con.setRequestMethod("GET");
                log.debug("Performing HTTP GET: " + u);
                int responseCode = con.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    log.error("HTTP GET returned code " + responseCode);
                } else {
                    log.debug("HTTP GET returned HTTP_OK");
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    response.append(line);
                    if (keep_newline) {
                        response.append("\n");
                    }
                }
                in.close();
                con.disconnect();
                return response.toString();
            }

        } catch (IOException ex) {
            log.error("Error during HTTP GET request to Shim: " + ex);
        }
        return null;
    }

    private void login() {

        if (!auth) {
            return;
        }
        if (curAuthToken != null) {
            return; // Do not need to re-login if we alrerady have a token
        }

        // Build HTTP GET parameters
        HashMap<String, String> pars = new HashMap<>();
        pars.put("username", user);
        pars.put("password", pw);

        try {
            URL reqUrl = new URL(url + SHIM_ENDPOINT_LOGIN + parsToUrlString(pars));
            String response = performSimpleHTTPGet(reqUrl);

            curAuthToken = response;
        } catch (MalformedURLException ex) {
            log.error("Login to Shim failed: " + ex);
            curAuthToken = null;
        }
    }

    private void newSession() {
        if (auth && curAuthToken == null) {
            login();
        }

        // This method does not check whether there is already an active session. 
        // It always tries to create a new session
        // Build HTTP GET parameters
        HashMap<String, String> pars = new HashMap<>();
        if (auth) {
            assert curAuthToken != null;
            pars.put("auth", curAuthToken);
        }

        try {
            URL reqUrl = new URL(url + SHIM_ENDPOINT_NEWSESSION + parsToUrlString(pars));
            String response = performSimpleHTTPGet(reqUrl);

            // TODO: Test whether result is a number?
            curSessionID = response;

        } catch (MalformedURLException ex) {
            log.error("Shim new_session failed: " + ex);
            curSessionID = null;
        }
    }

    /**
     *
     * @param afl AFL query
     * @param outFormat Output format string, use null for write queries.
     */
    private void executeQuery(String afl, String outFormat) {
        if (curSessionID == null) {
            newSession();
        }
        assert curSessionID != null;

        HashMap<String, String> pars = new HashMap<>();
        pars.put("id", curSessionID);
        pars.put("query", afl);
        pars.put("release", "0");
        if (outFormat != null) {
            pars.put("save", outFormat);
        }
        pars.put("stream", "1");
        //pars.put("compression", "0");

        if (auth) {
            assert curAuthToken != null;
            pars.put("auth", curAuthToken);
        }

        try {
            URL reqUrl = new URL(url + SHIM_ENDPOINT_EXECUTEQUERY + parsToUrlString(pars));
            String response = performSimpleHTTPGet(reqUrl);

            // TODO: Test whether result is a number?
            curQueryID = response;

        } catch (MalformedURLException ex) {
            log.error("Shim execute_query failed: " + ex);
            curQueryID = null;
        }
    }

    private String readLines() {
        assert curSessionID != null;
        assert curQueryID != null;

        HashMap<String, String> pars = new HashMap<>();
        pars.put("id", curSessionID);
        pars.put("n", "0");
        if (auth) {
            assert curAuthToken != null;
            pars.put("auth", curAuthToken);
        }

        try {
            URL reqUrl = new URL(url + SHIM_ENDPOINT_READLINES + parsToUrlString(pars));
            String response = performSimpleHTTPGet(reqUrl, true);
            return response;

        } catch (MalformedURLException ex) {
            log.error("Shim read_lines failed: " + ex);
            curQueryID = null;
        }
        return null;
    }

    private void release() {

        HashMap<String, String> pars = new HashMap<>();
        pars.put("id", curSessionID);
        if (auth) {
            assert curAuthToken != null;
            pars.put("auth", curAuthToken);
        }

        try {
            URL reqUrl = new URL(url + SHIM_ENDPOINT_RELEASESESSION + parsToUrlString(pars));
            String response = performSimpleHTTPGet(reqUrl);
        } catch (MalformedURLException ex) {
            log.warn("Shim release_session failed: " + ex);
            curQueryID = null;
        } finally {
            curQueryID = null;
            curSessionID = null;
        }

    }

    /**
     * Runs a SciDB AFL red query and applies a callback function to each CSV
     * string row, i.e the string representation of one result array cell.
     *
     * @param afl SciDB AFL query
     * @param callback a callback function that takes one string cell
     * representation as input
     */
    public void queryReadCSV(String afl, IShimTextCellProcessor callback)   {

        newSession(); // automatically calls login if needed
        executeQuery(afl, "csv");
       
        String res = readLines();
        release();

        // Split result by new lines (CSV)
        String[] strcells = res.split("\\r?\\n");

        // Apply callback to each cell, ignore header
        for (int i = 1; i < strcells.length; ++i) {
            callback.process(strcells[i]);
        }

    }

}
