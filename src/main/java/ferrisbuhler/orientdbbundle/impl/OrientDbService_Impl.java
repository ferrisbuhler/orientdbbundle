package ferrisbuhler.orientdbbundle.impl;

import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import ferrisbuhler.orientdbbundle.OrientDbService;
import ferrisbuhler.orientdbbundle.OrientdbBundleException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class initializes and controlls an Orient DB server.
 * @author ferrisbuhler
 */
@Component(
        name="OrientDbServer",
        immediate=true
)
@Service
public class OrientDbService_Impl implements OrientDbService {
  private static final Logger logger = LoggerFactory.getLogger(OrientDbService_Impl.class);
  private static final String CONFIG_PID = "my.dataStore.OrientDB";
  private static final String DBURL_PARAM = "databaseUrl";  

  private static final String ORIENTDB_HOME = "orient_db";
  private static final String ORIENTDB_CONFIG_FILE = "orientdb-server-config.xml";
  private static final String ORIENTDB_DATASTORE = "my_datastore";

  private static final String ORIENTDB_USER     = "admin";
  private static final String ORIENTDB_PASSWORD = "admin";

  private String datastoreUrl = null;
  
  @Reference
  private ConfigurationAdmin configAdmin;  

  private OServer orientDbServer = null;
  private OrientGraphFactory  dbGraphFactory = null;

  /**
   * Starts Orient DB server.
   * This method does some fixed wired settings (server home path, pool size,
   * etc.). Change them right here or extend the <code>ConfigurationAdmin</code>
   * usage in order to load them from a config file.
   * @throws OrientdbBundleException
   */
  @Activate
  public void activate() throws OrientdbBundleException {
    
    /* expecting the server's home directory %ORIENTDB_HOME% to reside in the
    ** current working directory.
    ** Commonly this is the base directory of OSGi application servers.
    */
    Path dbHomePath = Paths.get(ORIENTDB_HOME);
    Path configFilePath = dbHomePath.resolve(ORIENTDB_CONFIG_FILE);

    logger.info("activate: Starting OrientDb Service - config file = '{}'.", configFilePath);
    System.setProperty("ORIENTDB_HOME", dbHomePath.toAbsolutePath().toString());
    
    try {
      /* ---------------------------------------------------------------------
       * init Orient DB server
       */
      // create and init server instance
      orientDbServer = OServerMain.create();
      orientDbServer.startup(configFilePath.toFile());
      orientDbServer.activate();

      // data store location
      Path datastorePath = Paths.get(orientDbServer.getDatabaseDirectory(), ORIENTDB_DATASTORE);    
      logger.debug("activate: datastore path: '{}'.", datastorePath);

      // data store URL
      datastoreUrl = "plocal:" + datastorePath;
      logger.info("activate: init datastore '{}'.", datastoreUrl);

       // init db factory
      dbGraphFactory = new OrientGraphFactory(datastoreUrl, ORIENTDB_USER, ORIENTDB_PASSWORD).setupPool(3, 15);

      // connect to data store as a test - create if not exists
      try (ODatabase dbo = dbGraphFactory.getDatabase(true, false)) {
        logger.info("activate: datastore '{}' is present and connective.", dbo.getURL());
        
        /* Writing the data store URL to a config file.
        ** This empowers other bundles to open the data store.
        */
        new ConfigAdminHandler(configAdmin, CONFIG_PID).setParam(DBURL_PARAM, datastoreUrl);
      }
      
    } catch(Exception ex) {
      logger.error("activate - Error!", ex);
      deactivate();
      throw new OrientdbBundleException(ex);
    }
  }

  /**
   * stops Orient DB server
   */
  @Deactivate
  protected void deactivate() {
    logger.debug("stop");

    if(orientDbServer != null) {
      orientDbServer.shutdown();
      orientDbServer = null;
    }

    datastoreUrl = null;
  }
  
  @Override
  public OrientGraphFactory getGraphFactory() throws OrientdbBundleException {
    if(dbGraphFactory == null) {
      throw new OrientdbBundleException("Orient database factory has not been initialized!");
    }

    return dbGraphFactory;
  }

  @Override
  public String getDatastoreUrl() throws OrientdbBundleException {
    if(datastoreUrl == null) {
      throw new OrientdbBundleException("Orient DB server has not been initialized!");
    }

    return datastoreUrl;
  }
}
