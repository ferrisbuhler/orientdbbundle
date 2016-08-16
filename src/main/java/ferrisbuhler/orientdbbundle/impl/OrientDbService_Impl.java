package ferrisbuhler.orientdbbundle.impl;

import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import ferrisbuhler.orientdbbundle.OrientDbService;
import ferrisbuhler.orientdbbundle.OrientdbBundleException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
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
public class OrientDbService_Impl implements OrientDbService, ConfigurationListener {
  private static final Logger logger = LoggerFactory.getLogger(OrientDbService_Impl.class);
  private static final String CONFIG_PID = "ferrisbuhler.orientdbbundle";
  private static final String DBURL_PARAM = "databaseUrl";  

  // default settings - may be overridden by configuration
  private static final String ORIENTDB_HOME_DEFAULT = "orient_db";
  private static final String ORIENTDB_CONFIG_FILE_PATH_DEFAULT = "etc/orientdb-server-config.xml";
  private static final String ORIENTDB_DATASTORE_DEFAULT = "my_datastore";
  private static final String ORIENTDB_USER_DEFAULT     = "admin";
  private static final String ORIENTDB_PASSWORD_DEFAULT = "admin";

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
    logger.info("activate: Starting OrientDb Service");

    /* loading config params from OSGi Config Admin
    ** (may origin from a config file etc/ferrisbuhler.orientdbbundle.cfg or from
    **  a feature bundle descriptor)
    */
    ConfigAdminHandler cfgAdminHdl = new ConfigAdminHandler(configAdmin, CONFIG_PID);
    String orientdb_home  = cfgAdminHdl.getParam("orientdb_home", ORIENTDB_HOME_DEFAULT);
    String cfgFile_path   = cfgAdminHdl.getParam("cfgFile_path", ORIENTDB_CONFIG_FILE_PATH_DEFAULT);
    String datastore_name = cfgAdminHdl.getParam("datastore_name", ORIENTDB_DATASTORE_DEFAULT);
    String datastore_user = cfgAdminHdl.getParam("datastore_name", ORIENTDB_USER_DEFAULT);
    String datastore_password = cfgAdminHdl.getParam("datastore_name", ORIENTDB_PASSWORD_DEFAULT);

    // setting the ORIENTDB_HOME environment variable
    Path orientDbHomePath = Paths.get(orientdb_home);
    System.setProperty("ORIENTDB_HOME", orientDbHomePath.toAbsolutePath().toString());
    logger.info("activate: OrientDbhome path = '{}'.", orientDbHomePath.toAbsolutePath());
        
    try {
      Path configFilePath = Paths.get(cfgFile_path);
      File orientDBCfgFile = configFilePath.toFile();

      if(orientDBCfgFile == null) {
        throw new OrientdbBundleException("activate - cannot create OrientDB config file: " + cfgFile_path);
      } else {
        if(orientDBCfgFile.exists() && orientDBCfgFile.isFile()) {
          logger.info("activate: Starting OrientDb with config file = '{}'.", configFilePath.toAbsolutePath());
        } else {
          orientDBCfgFile.createNewFile();
        }
      }

      /* ---------------------------------------------------------------------
       * init Orient DB server
       */
      // create and init server instance
      orientDbServer = OServerMain.create();      
      orientDbServer.startup(orientDBCfgFile);
      orientDbServer.activate();

      // data store location
      Path datastorePath = Paths.get(orientDbServer.getDatabaseDirectory(), datastore_name);    
      logger.debug("activate: datastore path: '{}'.", datastorePath);

      // data store URL
      datastoreUrl = "plocal:" + datastorePath;
      logger.info("activate: init datastore '{}'.", datastoreUrl);

      // init db factory
      dbGraphFactory = new OrientGraphFactory(datastoreUrl, datastore_user, datastore_password).setupPool(3, 15);

      // connect to data store as a test - create if not exists
      try (ODatabase dbo = dbGraphFactory.getDatabase(true, false)) {
        logger.info("activate: datastore '{}' is present and connective.", dbo.getURL());
        
        /* Writing the data store URL to a config file.
        ** This empowers other bundles to open the data store.
        */
        cfgAdminHdl.setParam(DBURL_PARAM, datastoreUrl);
      }
      
    } catch(Exception ex) {
      logger.error("activate - Error!", ex);
      deactivate();
      throw new OrientdbBundleException(ex);
    }
  }

  /**
   * OSGi service modified method
   * @throws ferrisbuhler.orientdbbundle.OrientdbBundleException
   */
  @Modified
  public void modified() throws OrientdbBundleException {
    deactivate();
    activate();
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
  public void configurationEvent(ConfigurationEvent event) {
    
    if(!CONFIG_PID.equals(event.getPid())) {
      return;
    }
    try {
      modified();
    } catch(OrientdbBundleException ex) {
      logger.warn("configurationEvent - reconfiguration terminated with error:", ex);
    }
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
