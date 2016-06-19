package ferrisbuhler.orientdbbundle;

import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * This service initializes and controlls an Orient DB server.
 * The goal is to have an embedded data store running in an OSGi environment that
 * can be accessed just like a standalone OrientDB server. Especially the usage
 * of OrientDB Studio was aimed.<br/>
 * <br/>
 * For this purpose this bundle exports the url of the controlled data store<br/>
 * 1.) by writing the URL to an OSGi config file, and<br/>
 * 2.) provide the URL by the method <code>getDatastoreUrl</code>.<br/>
 * <br/>
 * The URL can be used by other bundles to open the data store.
 * 
 * @author ferrisbuhler
 */
public interface OrientDbService {

  /**
   * Get the central db factory
   * @return
   * @throws OrientdbBundleException
   */
  public OrientGraphFactory getGraphFactory() throws OrientdbBundleException;

  /**
   * get URL of the server's data store
   * @return
   * @throws OrientdbBundleException
   */
  public String getDatastoreUrl() throws OrientdbBundleException;
}
