package ferrisbuhler.orientdbbundle.impl;

import ferrisbuhler.orientdbbundle.OrientdbBundleException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience wrapper for the OSGi ConfigurationAdmin.
 * @author ferrisbuhler
 */
public class ConfigAdminHandler {
  private static final Logger logger = LoggerFactory.getLogger(ConfigAdminHandler.class);
  
  private Configuration config;
  protected Dictionary<String,Object> dictionary;
  
  protected ConfigAdminHandler() {}
  
  /**
   * convert configuration from Dictionary to Map and return this
   * @param configAdmin
   * @param cfgName
   * @throws OrientdbBundleException
   */
  public ConfigAdminHandler(ConfigurationAdmin configAdmin, String cfgName) throws OrientdbBundleException {

    if(configAdmin == null) {
      throw new OrientdbBundleException("ConfigAdminHandler - 'Felix Config Admin' not initialized!");
    }

    try {
      config = configAdmin.getConfiguration(cfgName);
      if(config == null) {
        throw new OrientdbBundleException("ConfigAdminHandler - Properties not present in '" + cfgName + "'!");
      }
      
      logger.info("ConfigAdminHandler - loading properties from '{}'.", config.getPid());
      
      dictionary = config.getProperties();
      
      if(!isValid()) {
        dictionary = new Hashtable<>();
        config = configAdmin.createFactoryConfiguration(cfgName);
        config.update(dictionary);
        logger.info("ConfigAdminHandler - Created new configuration '{}'!", cfgName);
      }
      
    } catch(IOException ex) {
      throw new OrientdbBundleException(ex);
    }
  }
  
  /**
   * return the initialized config dictionary or throw an exception
   * @return
   * @throws OrientdbBundleException 
   */
  private Dictionary getDictionary() throws OrientdbBundleException {
    if(dictionary != null) {
      return dictionary;
    } else {
      throw new OrientdbBundleException("Config dictionary has not been initialized!");
    }
  }
  
  /**
   * Indicates if the underlying 'Configuration' has been init correctly.
   * @return 
   */
  public final boolean isValid() {
    return dictionary != null && !dictionary.isEmpty();
  }
  
  /**
   * 
   * @param key
   * @return 
   * @throws ferrisbuhler.orientdbbundle.OrientdbBundleException 
   */
  public boolean hasParam(String key) throws OrientdbBundleException {
    return getDictionary().get(key) != null;
  }
  
  /**
   * Obtains the required parameter in the config dictionary. If the
   * parameter can't be found the method throws a exception.
   *
   * @param key the parameter name
   * @return the parameter value
   * @throws OrientdbBundleException
   */
  public String getParam(String key) throws OrientdbBundleException {
    String param = (String)getDictionary().get(key);
    
    if (param == null) {
      throw new OrientdbBundleException("getParam - Required Environment parameter \"" + key + "\" not found");
    }

    return param;
  }
  
  /**
   * Obtains the required parameter in the config dictionary. In case of failure
   * or if the value can't be found the default value is returned.
   *
   * @param key the parameter name
   * @param defaultValue a default value to return if the property is not available
   * @return the parameter value
   */
  public String getParam(String key, String defaultValue) {
    try {
      String value = (String)getDictionary().get(key);
      if (value != null) {
        return value;
      }
    } catch(OrientdbBundleException ex) {
      logger.warn("getParam({},{}) - error:", key, defaultValue, ex);
    }
    
    return defaultValue;
  }
  
  /**
   * Writes the config item to the configuration object.
   * @param key
   * @param value
   * @throws OrientdbBundleException 
   */
  public void setParam(String key, String value) throws OrientdbBundleException {
    getDictionary().put(key, value);
    try {
      config.update(getDictionary());
    } catch(IOException ex) {
      throw new OrientdbBundleException(ex);
    }
  }
}
