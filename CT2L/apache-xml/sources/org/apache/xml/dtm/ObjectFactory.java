package org.apache.xml.dtm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import org.apache.xalan.templates.Constants;

class ObjectFactory {
    private static final boolean DEBUG = false;
    private static final String DEFAULT_PROPERTIES_FILENAME = "xalan.properties";
    private static final String SERVICES_PATH = "META-INF/services/";
    private static Properties fXalanProperties = null;
    private static long fLastModified = -1;

    ObjectFactory() {
    }

    static Object createObject(String factoryId, String fallbackClassName) throws ConfigurationError {
        return createObject(factoryId, null, fallbackClassName);
    }

    static Object createObject(String factoryId, String propertiesFilename, String fallbackClassName) throws ConfigurationError {
        Class factoryClass = lookUpFactoryClass(factoryId, propertiesFilename, fallbackClassName);
        if (factoryClass == null) {
            throw new ConfigurationError("Provider for " + factoryId + " cannot be found", null);
        }
        try {
            Object instance = factoryClass.newInstance();
            debugPrintln("created new instance of factory " + factoryId);
            return instance;
        } catch (Exception x) {
            throw new ConfigurationError("Provider for factory " + factoryId + " could not be instantiated: " + x, x);
        }
    }

    static Class lookUpFactoryClass(String factoryId) throws ConfigurationError {
        return lookUpFactoryClass(factoryId, null, null);
    }

    static Class lookUpFactoryClass(String factoryId, String propertiesFilename, String fallbackClassName) throws ConfigurationError {
        String factoryClassName = lookUpFactoryClassName(factoryId, propertiesFilename, fallbackClassName);
        ClassLoader cl = findClassLoader();
        if (factoryClassName == null) {
            factoryClassName = fallbackClassName;
        }
        try {
            Class providerClass = findProviderClass(factoryClassName, cl, true);
            debugPrintln("created new instance of " + providerClass + " using ClassLoader: " + cl);
            return providerClass;
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError("Provider " + factoryClassName + " not found", x);
        } catch (Exception x2) {
            throw new ConfigurationError("Provider " + factoryClassName + " could not be instantiated: " + x2, x2);
        }
    }

    static String lookUpFactoryClassName(String factoryId, String propertiesFilename, String fallbackClassName) {
        File propertiesFile;
        SecuritySupport ss = SecuritySupport.getInstance();
        try {
            String systemProp = ss.getSystemProperty(factoryId);
            if (systemProp != null) {
                debugPrintln("found system property, value=" + systemProp);
                return systemProp;
            }
        } catch (SecurityException e) {
        }
        String factoryClassName = null;
        if (propertiesFilename == null) {
            File propertiesFile2 = null;
            boolean propertiesFileExists = false;
            try {
                String javah = ss.getSystemProperty("java.home");
                propertiesFilename = javah + File.separator + "lib" + File.separator + DEFAULT_PROPERTIES_FILENAME;
                propertiesFile = new File(propertiesFilename);
            } catch (SecurityException e2) {
            }
            try {
                propertiesFileExists = ss.getFileExists(propertiesFile);
                propertiesFile2 = propertiesFile;
            } catch (SecurityException e3) {
                propertiesFile2 = propertiesFile;
                fLastModified = -1L;
                fXalanProperties = null;
            }
            synchronized (ObjectFactory.class) {
                boolean loadProperties = false;
                FileInputStream fis = null;
                try {
                    try {
                        if (fLastModified >= 0) {
                            if (propertiesFileExists) {
                                long j = fLastModified;
                                long lastModified = ss.getLastModified(propertiesFile2);
                                fLastModified = lastModified;
                                if (j < lastModified) {
                                    loadProperties = true;
                                } else if (!propertiesFileExists) {
                                    fLastModified = -1L;
                                    fXalanProperties = null;
                                }
                            }
                        } else if (propertiesFileExists) {
                            loadProperties = true;
                            fLastModified = ss.getLastModified(propertiesFile2);
                        }
                        if (loadProperties) {
                            fXalanProperties = new Properties();
                            fis = ss.getFileInputStream(propertiesFile2);
                            fXalanProperties.load(fis);
                        }
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e4) {
                            }
                        }
                    } catch (Exception e5) {
                        fXalanProperties = null;
                        fLastModified = -1L;
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e6) {
                            }
                        }
                    }
                } catch (Throwable th) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e7) {
                        }
                    }
                    throw th;
                }
            }
            if (fXalanProperties != null) {
                factoryClassName = fXalanProperties.getProperty(factoryId);
            }
        } else {
            FileInputStream fis2 = null;
            try {
                fis2 = ss.getFileInputStream(new File(propertiesFilename));
                Properties props = new Properties();
                props.load(fis2);
                factoryClassName = props.getProperty(factoryId);
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e8) {
                    }
                }
            } catch (Exception e9) {
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e10) {
                    }
                }
            } catch (Throwable th2) {
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e11) {
                    }
                }
                throw th2;
            }
        }
        if (factoryClassName == null) {
            String systemProp2 = findJarServiceProviderName(factoryId);
            return systemProp2;
        }
        debugPrintln("found in " + propertiesFilename + ", value=" + factoryClassName);
        String systemProp3 = factoryClassName;
        return systemProp3;
    }

    private static void debugPrintln(String msg) {
    }

    static ClassLoader findClassLoader() throws ConfigurationError {
        SecuritySupport ss = SecuritySupport.getInstance();
        ClassLoader context = ss.getContextClassLoader();
        ClassLoader system = ss.getSystemClassLoader();
        for (ClassLoader chain = system; context != chain; chain = ss.getParentClassLoader(chain)) {
            if (chain == null) {
                return context;
            }
        }
        ClassLoader current = ObjectFactory.class.getClassLoader();
        for (ClassLoader chain2 = system; current != chain2; chain2 = ss.getParentClassLoader(chain2)) {
            if (chain2 == null) {
                return current;
            }
        }
        return system;
    }

    static Object newInstance(String className, ClassLoader cl, boolean doFallback) throws ConfigurationError {
        try {
            Class providerClass = findProviderClass(className, cl, doFallback);
            Object instance = providerClass.newInstance();
            debugPrintln("created new instance of " + providerClass + " using ClassLoader: " + cl);
            return instance;
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError("Provider " + className + " not found", x);
        } catch (Exception x2) {
            throw new ConfigurationError("Provider " + className + " could not be instantiated: " + x2, x2);
        }
    }

    static Class findProviderClass(String className, ClassLoader cl, boolean doFallback) throws ClassNotFoundException, ConfigurationError {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            try {
                int lastDot = className.lastIndexOf(Constants.ATTRVAL_THIS);
                String packageName = className;
                if (lastDot != -1) {
                    packageName = className.substring(0, lastDot);
                }
                security.checkPackageAccess(packageName);
            } catch (SecurityException e) {
                throw e;
            }
        }
        if (cl == null) {
            return Class.forName(className);
        }
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException x) {
            if (doFallback) {
                ClassLoader current = ObjectFactory.class.getClassLoader();
                if (current == null) {
                    return Class.forName(className);
                }
                if (cl != current) {
                    return current.loadClass(className);
                }
                throw x;
            }
            throw x;
        }
    }

    private static String findJarServiceProviderName(String factoryId) {
        BufferedReader rd;
        ClassLoader current;
        SecuritySupport ss = SecuritySupport.getInstance();
        String serviceId = SERVICES_PATH + factoryId;
        ClassLoader cl = findClassLoader();
        InputStream is = ss.getResourceAsStream(cl, serviceId);
        if (is == null && cl != (current = ObjectFactory.class.getClassLoader())) {
            cl = current;
            is = ss.getResourceAsStream(cl, serviceId);
        }
        if (is == null) {
            return null;
        }
        debugPrintln("found jar resource=" + serviceId + " using ClassLoader: " + cl);
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is));
        }
        try {
            String factoryClassName = rd.readLine();
            try {
                rd.close();
            } catch (IOException e2) {
            }
            if (factoryClassName == null || "".equals(factoryClassName)) {
                return null;
            }
            debugPrintln("found in resource, value=" + factoryClassName);
            return factoryClassName;
        } catch (IOException e3) {
            try {
                rd.close();
            } catch (IOException e4) {
            }
            return null;
        } catch (Throwable th) {
            try {
                rd.close();
            } catch (IOException e5) {
            }
            throw th;
        }
    }

    static class ConfigurationError extends Error {
        static final long serialVersionUID = 5122054096615067992L;
        private Exception exception;

        ConfigurationError(String msg, Exception x) {
            super(msg);
            this.exception = x;
        }

        Exception getException() {
            return this.exception;
        }
    }
}
