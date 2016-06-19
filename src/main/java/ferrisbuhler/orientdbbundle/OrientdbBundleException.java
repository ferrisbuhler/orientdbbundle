package ferrisbuhler.orientdbbundle;

/**
 * Standard exception for this bundle
 * @author ferrisbuhler
 */
public class OrientdbBundleException extends Exception {
  
  public OrientdbBundleException(String string) {
    super(string);
  }

  public OrientdbBundleException(String string, Throwable thrwbl) {
    super(string, thrwbl);
  }

  public OrientdbBundleException(Throwable thrwbl) {
    super(thrwbl);
  }  
}
