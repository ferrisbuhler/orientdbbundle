# orientdbbundle

This is a wrapper of the marvelous [OrientDB](http://orientdb.com/orientdb/)
database coming as an OSGi bundle.
(Inspired by http://orientdb.com/docs/last/Embedded-Server.html)

Well, yes, OrientDB JARs already come as OSGi bundles, partly, but not all of
them, do they? This package collects all parts and takes care for comprehensive
configuration with the standard OrientDB config file.

This bundle provides a complete embedded OrientDB server for an OSGi environment
(like [Apache Karaf](http://karaf.apache.org/)) that can be used by other OSGi
bundles but is also accessable from external clients. Especially (with some
extra setup) it also provides the web client 'OrientDB Studio'.

Admittedly this project brings only little benefit compared to the standard
approach of installing a standalone OrientDB server alongside your application
server.
But if you want to (or must) run a data store completely embedded in the OSGi
container then this approach might be of interest.

Function
========
The POM collects all neccessary Maven bundles for an OrientDB server. Some of
those entail a vast amount of dependencies. It took some trial-and-error to find
out which ones can be excluded from import (maybe you know a better way to deal
with this?).

The bundle provides a service that starts and stops the OrientDB server process
and exports the URL of its data store. By this way other bundles can open the data
store by themself and use it for whatever they are disposed.
Of course the OrientDB packages are exported to be available for other OSGi
bundles in the container.

Setup
=====
Data Store Location
-------------------
The present implementation expects an "OrientDB Home" directory `orient_db`
directly under the current working directory of the application server.
Create this folder there and copy/create the following ressources:

```
orient_db
 |- orientdb-server-config.xml (file)
 |- databases                  (folder)
```
You can find a template/example folder on the top level of this project.

By convention the OrientDB server looks for data stores in the folder
`<ORIENTDB_HOME>/databases`.

Please consult the OrientDB documentation for further assistence with the
`orientdb-server-config.xml` config file. (http://orientdb.com/docs/last/DB-Server.html)

OrientDB Studio
---------------
The example config file "orientdb-server-config.xml" sets the path for the
"OrientDB Studio" web application:
```
<entry value="orient_db/orientdb_studio/www" name="orientdb.www.path"/>
```
Change this entry to meet your needs.

The OrientDB Studio itself is not a part of this project.
* Download OrientDB Studio from https://search.maven.org/search?q=g:com.orientechnologies%20AND%20a:orientdb-studio
* Unpack the ZIP and copy the contents to the above configured folder.

(Note: the "www" folder is inside the ZIP).

___NOTICE___:
OrientDB Studio has a bug that occurs when beeing run from an embedded server.
Apply the following patch to `orientdb_studio/www/scripts/scripts.js` in order to
workaround it:
```
  var API = (function () {
     var m = window.location.pathname.match(/(.*\/)studio\/index.html/);
-    return m && m[1] ? m[1] : '/api/';
+    return m && m[1] ? m[1] : '/';
 })();
```
(taken from https://groups.google.com/forum/#!topic/orient-database/v6QkpKHOFdg)

Usage
=====
Obtaining the Data Store URL
----------------------------
Rather than harcoding the data store URL in each client bundle it is an obvious
approach to export the data store URL from the server. This happens in two
different ways:

1. OSGi Service
The main class of this bundle is provided as an OSGi service `OrientDbServer`.
This service's method `getDatastoreUrl()` returns the data store URL as soon as
the server has been started.

2. Configuration file
The service uses OSGi ConfigurationAdmin to write the data store URL to a config
file. Currently the PID is hardcoded `my.dataStore.OrientDB`.
Client bundle may use the same approach to obtain the data store URL.

This approach is more complicated than the first one, but is doesn't depend on
the presence of presence or activity of the OrientDB-Bundle. As long as the data
store exists a client can use it independently of a server process running or not.

Known Bugs
==========
GraphDB support for OrientDB Studio
-----------------------------------
Rather a "lack of knowledge" than a bug:

The POM needs to declare some `Embedded-Dependency` entries for some OrientDB modules.
However there are some problems with the `orientdb-graphdb` module:

Logon to OrientDB studio is not possible any more as soon as the service
dependency is activated. When the service dependency is commented out studio
works fine except of GraphDB operations.

I didn't spend much examination to that behaviour nor I filed a bug as it didn't
disturb me too much (using plain key/value). Maybe some fellow knows advice.

Versions
========
* This module is based on [OrientDB](http://orientdb.com/orientdb/) 2.2.37.
* It hasn't been tested yet against an OSGI environment (use earlier version for a tested release).
