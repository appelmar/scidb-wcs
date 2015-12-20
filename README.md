
# scidb-wcs
A prototypical WCS implementation for SciDB.

## Description
This Java web application implements a prototypical web coverage service (1.0.0) for the array database management system SciDB. To serve many different output file formats, the WCS interfaces the existing [GDAL driver for SciDB](https://github.com/mappl/scidb4gdal). The web application includes a single servlet at the endpoint

> http://YOURSERVER/scidb-wcs/ows 
 
which takes HTTP Get parameters as specified in the WCS standard. 

Key functionalities include:

  - GetCapabilities, DescribeCoverage, GetCoverage services according to OGC WCS 1.0.0 standard
  - GeoTIFF, JPEG, BMP, NetCDF output formats
  - Nearest neighbour, bilinear, and bicubic resampling
  - HTTP GET KVP protocol binding
  - Domain subsetting (rrimming or spatial range selection)
  - Range subsetting, i.e. selection of particular array attributes
  - Selection of temporal slices of spacetime arrays


## Requirements

This WCS connects to the SciDB coordinator instance over the network. It is not required, 
that it runs on the coordinator instance though this might be reasonable in practice. 
However, there are a few requirements:

- SciDB must use [scidb4geo](https://github.com/mappl/scidb4geo) to work with spatial reference
- SciDB must be accassible over both, native SciDB networking (usually port 1239) as well as SciDB's HTTP web service Shim. 
- GDAL including [scidb4gdal](https://github.com/mappl/scidb4gdal) must be installed on the server that runs the WCS



## Configuration

After deployment, you will find a file `WEB-INF/config.properties` which is used to configure the WCS. 
Selected cinfiguration parameters are described below. Further parameters e.g. for setting conctact details should be self-explanatory as you'll find them in the file. 

| Key | Description | Default |
| --- | ----------- | ------- |
| SCIDBWCS_DB_HOST | The host name of SciDB | localhost |
| SCIDBWCS_DB_PORT | Native SciDB port (not HTTP) | 1239 |
| SCIDBWCS_DB_SHIMPORT | Port used to connect to Shim over HTTP(S) | 8083 |
| SCIDBWCS_DB_USER | SciDB username | scidb |
| SCIDBWCS_DB_PW | SciDB password | scidb |
| SCIDBWCS_DB_SSL | Use SSL or not | true |
| SCIDBWCS_GDALPATH | Path to GDAL executables, null if executables are in PATH | /usr/local/bin/ |
| WCS_PUBLIC_URL  | The public URL how to reach the WCS over the web | http://localhost:8080/scidb-wcs/ows |
| ... | (see default file as an example) | ... |


## Build Instructions

The following steps build a WAR file using Maven:

1. Clone this project: `git clone https://github.com/mappl/scidb-wcs`

2. Download the SciDB JDBC driver and install it to your local Maven repository: `mvn install:install-file -Dfile=lib/scidb4j.jar -DgroupId=org.scidb -DartifactId=scidb4j -Dversion=0.1 -Dpackaging=jar`

3. Build the WAR file by running `mvn package` at the project root directory

