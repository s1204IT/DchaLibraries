package mf.org.apache.xerces.impl.dv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

final class ObjectFactory {
    private static final int DEFAULT_LINE_LENGTH = 80;
    private static final String DEFAULT_PROPERTIES_FILENAME = "xerces.properties";
    private static final boolean DEBUG = isDebugEnabled();
    private static Properties fXercesProperties = null;
    private static long fLastModified = -1;

    ObjectFactory() {
    }

    static Object createObject(String factoryId, String fallbackClassName) throws ConfigurationError {
        return createObject(factoryId, null, fallbackClassName);
    }

    static Object createObject(String factoryId, String propertiesFilename, String fallbackClassName) throws ConfigurationError {
        String propertiesFilename2;
        String propertiesFilename3 = propertiesFilename;
        if (DEBUG) {
            debugPrintln("debug is on");
        }
        ClassLoader cl = findClassLoader();
        try {
            String systemProp = SecuritySupport.getSystemProperty(factoryId);
            if (systemProp != null && systemProp.length() > 0) {
                if (DEBUG) {
                    debugPrintln("found system property, value=" + systemProp);
                }
                return newInstance(systemProp, cl, true);
            }
        } catch (SecurityException e) {
        }
        String factoryClassName = null;
        if (propertiesFilename3 == null) {
            File propertiesFile = null;
            boolean propertiesFileExists = false;
            try {
                String javah = SecuritySupport.getSystemProperty("java.home");
                propertiesFilename3 = String.valueOf(javah) + File.separator + "lib" + File.separator + DEFAULT_PROPERTIES_FILENAME;
                propertiesFile = new File(propertiesFilename3);
                propertiesFileExists = SecuritySupport.getFileExists(propertiesFile);
            } catch (SecurityException e2) {
                fLastModified = -1L;
                fXercesProperties = null;
            }
            propertiesFilename2 = propertiesFilename3;
            synchronized (ObjectFactory.class) {
                boolean loadProperties = false;
                FileInputStream fis = null;
                try {
                    try {
                        if (fLastModified >= 0) {
                            if (propertiesFileExists) {
                                long j = fLastModified;
                                long lastModified = SecuritySupport.getLastModified(propertiesFile);
                                fLastModified = lastModified;
                                if (j < lastModified) {
                                    loadProperties = true;
                                } else if (!propertiesFileExists) {
                                    fLastModified = -1L;
                                    fXercesProperties = null;
                                }
                            }
                        } else if (propertiesFileExists) {
                            loadProperties = true;
                            fLastModified = SecuritySupport.getLastModified(propertiesFile);
                        }
                        if (loadProperties) {
                            fXercesProperties = new Properties();
                            fis = SecuritySupport.getFileInputStream(propertiesFile);
                            fXercesProperties.load(fis);
                        }
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e3) {
                            }
                        }
                    } catch (Exception e4) {
                        fXercesProperties = null;
                        fLastModified = -1L;
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e5) {
                            }
                        }
                    }
                } finally {
                }
            }
            if (fXercesProperties != null) {
                factoryClassName = fXercesProperties.getProperty(factoryId);
            }
        } else {
            FileInputStream fis2 = null;
            try {
                fis2 = SecuritySupport.getFileInputStream(new File(propertiesFilename3));
                Properties props = new Properties();
                props.load(fis2);
                factoryClassName = props.getProperty(factoryId);
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e6) {
                    }
                }
            } catch (Exception e7) {
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e8) {
                    }
                }
            } catch (Throwable th) {
                if (fis2 == null) {
                    throw th;
                }
                try {
                    fis2.close();
                    throw th;
                } catch (IOException e9) {
                    throw th;
                }
            }
            propertiesFilename2 = propertiesFilename3;
        }
        if (factoryClassName != null) {
            if (DEBUG) {
                debugPrintln("found in " + propertiesFilename2 + ", value=" + factoryClassName);
            }
            return newInstance(factoryClassName, cl, true);
        }
        Object provider = findJarServiceProvider(factoryId);
        if (provider != null) {
            return provider;
        }
        if (fallbackClassName == null) {
            throw new ConfigurationError("Provider for " + factoryId + " cannot be found", null);
        }
        if (DEBUG) {
            debugPrintln("using fallback, value=" + fallbackClassName);
        }
        return newInstance(fallbackClassName, cl, true);
    }

    private static boolean isDebugEnabled() {
        try {
            String val = SecuritySupport.getSystemProperty("xerces.debug");
            if (val != null) {
                return !SchemaSymbols.ATTVAL_FALSE.equals(val);
            }
            return false;
        } catch (SecurityException e) {
            return false;
        }
    }

    private static void debugPrintln(String msg) {
        if (DEBUG) {
            System.err.println("XERCES: " + msg);
        }
    }

    static ClassLoader findClassLoader() throws ConfigurationError {
        ClassLoader context = SecuritySupport.getContextClassLoader();
        ClassLoader system = SecuritySupport.getSystemClassLoader();
        for (ClassLoader chain = system; context != chain; chain = SecuritySupport.getParentClassLoader(chain)) {
            if (chain == null) {
                return context;
            }
        }
        ClassLoader current = ObjectFactory.class.getClassLoader();
        for (ClassLoader chain2 = system; current != chain2; chain2 = SecuritySupport.getParentClassLoader(chain2)) {
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
            if (DEBUG) {
                debugPrintln("created new instance of " + providerClass + " using ClassLoader: " + cl);
            }
            return instance;
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError("Provider " + className + " not found", x);
        } catch (Exception x2) {
            throw new ConfigurationError("Provider " + className + " could not be instantiated: " + x2, x2);
        }
    }

    static Class findProviderClass(String className, ClassLoader cl, boolean doFallback) throws ConfigurationError, ClassNotFoundException {
        Class<?> clsLoadClass;
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            int lastDot = className.lastIndexOf(".");
            String packageName = className;
            if (lastDot != -1) {
                packageName = className.substring(0, lastDot);
            }
            security.checkPackageAccess(packageName);
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
                    clsLoadClass = Class.forName(className);
                } else if (cl != current) {
                    clsLoadClass = current.loadClass(className);
                } else {
                    throw x;
                }
                return clsLoadClass;
            }
            throw x;
        }
    }

    private static Object findJarServiceProvider(String factoryId) throws ConfigurationError {
        BufferedReader rd;
        ClassLoader current;
        String serviceId = "META-INF/services/" + factoryId;
        ClassLoader cl = findClassLoader();
        InputStream is = SecuritySupport.getResourceAsStream(cl, serviceId);
        if (is == null && cl != (current = ObjectFactory.class.getClassLoader())) {
            cl = current;
            is = SecuritySupport.getResourceAsStream(cl, serviceId);
        }
        if (is == null) {
            return null;
        }
        if (DEBUG) {
            debugPrintln("found jar resource=" + serviceId + " using ClassLoader: " + cl);
        }
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"), DEFAULT_LINE_LENGTH);
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is), DEFAULT_LINE_LENGTH);
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
            if (DEBUG) {
                debugPrintln("found in resource, value=" + factoryClassName);
            }
            return newInstance(factoryClassName, cl, false);
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

    static final class ConfigurationError extends Error {
        static final long serialVersionUID = 8521878292694272124L;
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
