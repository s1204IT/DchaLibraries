package mf.org.apache.xml.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import mf.org.apache.xml.resolver.helpers.BootstrapResolver;
import mf.org.apache.xml.resolver.helpers.Debug;

public class CatalogManager {
    private BootstrapResolver bResolver = new BootstrapResolver();
    private String catalogClassName;
    private String catalogFiles;
    public Debug debug;
    private String defaultCatalogFiles;
    private boolean defaultOasisXMLCatalogPI;
    private boolean defaultPreferPublic;
    private boolean defaultRelativeCatalogs;
    private boolean defaultUseStaticCatalog;
    private int defaultVerbosity;
    private boolean fromPropertiesFile;
    private boolean ignoreMissingProperties;
    private Boolean oasisXMLCatalogPI;
    private Boolean preferPublic;
    private String propertyFile;
    private URL propertyFileURI;
    private Boolean relativeCatalogs;
    private ResourceBundle resources;
    private Boolean useStaticCatalog;
    private Integer verbosity;
    private static String pFiles = "xml.catalog.files";
    private static String pVerbosity = "xml.catalog.verbosity";
    private static String pPrefer = "xml.catalog.prefer";
    private static String pStatic = "xml.catalog.staticCatalog";
    private static String pAllowPI = "xml.catalog.allowPI";
    private static String pClassname = "xml.catalog.className";
    private static String pIgnoreMissing = "xml.catalog.ignoreMissing";
    private static CatalogManager staticManager = new CatalogManager();
    private static Catalog staticCatalog = null;

    public CatalogManager() {
        this.ignoreMissingProperties = (System.getProperty(pIgnoreMissing) == null && System.getProperty(pFiles) == null) ? false : true;
        this.propertyFile = "CatalogManager.properties";
        this.propertyFileURI = null;
        this.defaultCatalogFiles = "./xcatalog";
        this.catalogFiles = null;
        this.fromPropertiesFile = false;
        this.defaultVerbosity = 1;
        this.verbosity = null;
        this.defaultPreferPublic = true;
        this.preferPublic = null;
        this.defaultUseStaticCatalog = true;
        this.useStaticCatalog = null;
        this.defaultOasisXMLCatalogPI = true;
        this.oasisXMLCatalogPI = null;
        this.defaultRelativeCatalogs = true;
        this.relativeCatalogs = null;
        this.catalogClassName = null;
        this.debug = null;
        this.debug = new Debug();
    }

    public BootstrapResolver getBootstrapResolver() {
        return this.bResolver;
    }

    private synchronized void readProperties() {
        InputStream in;
        try {
            this.propertyFileURI = CatalogManager.class.getResource("/" + this.propertyFile);
            in = CatalogManager.class.getResourceAsStream("/" + this.propertyFile);
        } catch (IOException e) {
            if (!this.ignoreMissingProperties) {
                System.err.println("Failure trying to read " + this.propertyFile);
            }
        } catch (MissingResourceException e2) {
            if (!this.ignoreMissingProperties) {
                System.err.println("Cannot read " + this.propertyFile);
            }
        }
        if (in == null) {
            if (!this.ignoreMissingProperties) {
                System.err.println("Cannot find " + this.propertyFile);
                this.ignoreMissingProperties = true;
            }
            return;
        }
        this.resources = new PropertyResourceBundle(in);
        if (this.verbosity == null) {
            try {
                String verbStr = this.resources.getString("verbosity");
                int verb = Integer.parseInt(verbStr.trim());
                this.debug.setDebug(verb);
                this.verbosity = new Integer(verb);
            } catch (Exception e3) {
            }
        }
    }

    public static CatalogManager getStaticManager() {
        return staticManager;
    }

    public void setIgnoreMissingProperties(boolean ignore) {
        this.ignoreMissingProperties = ignore;
    }

    public void setVerbosity(int verbosity) {
        this.verbosity = new Integer(verbosity);
        this.debug.setDebug(verbosity);
    }

    public void setRelativeCatalogs(boolean relative) {
        this.relativeCatalogs = new Boolean(relative);
    }

    public void setCatalogFiles(String fileList) {
        this.catalogFiles = fileList;
        this.fromPropertiesFile = false;
    }

    private boolean queryPreferPublic() {
        String prefer = System.getProperty(pPrefer);
        if (prefer == null) {
            if (this.resources == null) {
                readProperties();
            }
            if (this.resources == null) {
                return this.defaultPreferPublic;
            }
            try {
                prefer = this.resources.getString("prefer");
            } catch (MissingResourceException e) {
                return this.defaultPreferPublic;
            }
        }
        if (prefer == null) {
            return this.defaultPreferPublic;
        }
        return prefer.equalsIgnoreCase("public");
    }

    public boolean getPreferPublic() {
        if (this.preferPublic == null) {
            this.preferPublic = new Boolean(queryPreferPublic());
        }
        return this.preferPublic.booleanValue();
    }

    public void setPreferPublic(boolean preferPublic) {
        this.preferPublic = new Boolean(preferPublic);
    }

    public void setUseStaticCatalog(boolean useStatic) {
        this.useStaticCatalog = new Boolean(useStatic);
    }

    public void setAllowOasisXMLCatalogPI(boolean allowPI) {
        this.oasisXMLCatalogPI = new Boolean(allowPI);
    }

    public void setCatalogClassName(String className) {
        this.catalogClassName = className;
    }
}
