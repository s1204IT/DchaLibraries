package mf.javax.xml.stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

class FactoryFinder {
    private static boolean debug;
    static Properties cacheProps = new Properties();
    static volatile boolean firstTime = true;
    static SecuritySupport ss = new SecuritySupport();

    FactoryFinder() {
    }

    static {
        debug = false;
        boolean z = true;
        try {
            String val = ss.getSystemProperty("jaxp.debug");
            if (val == null || SchemaSymbols.ATTVAL_FALSE.equals(val)) {
                z = false;
            }
            debug = z;
        } catch (SecurityException e) {
            debug = false;
        }
    }

    private static void dPrint(String msg) {
        if (debug) {
            System.err.println("JAXP: " + msg);
        }
    }

    private static Class getProviderClass(String className, ClassLoader cl, boolean doFallback) throws ClassNotFoundException {
        try {
            if (cl == null) {
                ClassLoader cl2 = ss.getContextClassLoader();
                if (cl2 == null) {
                    throw new ClassNotFoundException();
                }
                return cl2.loadClass(className);
            }
            return cl.loadClass(className);
        } catch (ClassNotFoundException e1) {
            if (doFallback) {
                return Class.forName(className, true, FactoryFinder.class.getClassLoader());
            }
            throw e1;
        }
    }

    static Object newInstance(String className, ClassLoader cl, boolean doFallback) throws ConfigurationError {
        try {
            Class providerClass = getProviderClass(className, cl, doFallback);
            Object instance = providerClass.newInstance();
            if (debug) {
                dPrint("created new instance of " + providerClass + " using ClassLoader: " + cl);
            }
            return instance;
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError("Provider " + className + " not found", x);
        } catch (Exception x2) {
            throw new ConfigurationError("Provider " + className + " could not be instantiated: " + x2, x2);
        }
    }

    static Object find(String factoryId, String fallbackClassName) throws ConfigurationError {
        return find(factoryId, null, fallbackClassName);
    }

    static Object find(String factoryId, ClassLoader cl, String fallbackClassName) throws ConfigurationError {
        dPrint("find factoryId =" + factoryId);
        try {
            String systemProp = ss.getSystemProperty(factoryId);
            if (systemProp != null) {
                dPrint("found system property, value=" + systemProp);
                return newInstance(systemProp, null, true);
            }
        } catch (SecurityException se) {
            if (debug) {
                se.printStackTrace();
            }
        }
        String configFile = null;
        try {
            if (firstTime) {
                synchronized (cacheProps) {
                    if (firstTime) {
                        configFile = String.valueOf(ss.getSystemProperty("java.home")) + File.separator + "lib" + File.separator + "stax.properties";
                        File f = new File(configFile);
                        firstTime = false;
                        if (ss.doesFileExist(f)) {
                            dPrint("Read properties file " + f);
                            cacheProps.load(ss.getFileInputStream(f));
                        } else {
                            configFile = String.valueOf(ss.getSystemProperty("java.home")) + File.separator + "lib" + File.separator + "jaxp.properties";
                            File f2 = new File(configFile);
                            if (ss.doesFileExist(f2)) {
                                dPrint("Read properties file " + f2);
                                cacheProps.load(ss.getFileInputStream(f2));
                            }
                        }
                    }
                }
            }
            String factoryClassName = cacheProps.getProperty(factoryId);
            if (factoryClassName != null) {
                dPrint("found in " + configFile + " value=" + factoryClassName);
                return newInstance(factoryClassName, null, true);
            }
        } catch (Exception ex) {
            if (debug) {
                ex.printStackTrace();
            }
        }
        Object provider = findJarServiceProvider(factoryId);
        if (provider != null) {
            return provider;
        }
        if (fallbackClassName == null) {
            throw new ConfigurationError("Provider for " + factoryId + " cannot be found", null);
        }
        dPrint("loaded from fallback value: " + fallbackClassName);
        return newInstance(fallbackClassName, cl, true);
    }

    private static Object findJarServiceProvider(String factoryId) throws ConfigurationError {
        InputStream is;
        BufferedReader rd;
        String serviceId = "META-INF/services/" + factoryId;
        ClassLoader cl = ss.getContextClassLoader();
        if (cl != null) {
            is = ss.getResourceAsStream(cl, serviceId);
            if (is == null) {
                cl = FactoryFinder.class.getClassLoader();
                is = ss.getResourceAsStream(cl, serviceId);
            }
        } else {
            cl = FactoryFinder.class.getClassLoader();
            is = ss.getResourceAsStream(cl, serviceId);
        }
        if (is == null) {
            return null;
        }
        if (debug) {
            dPrint("found jar resource=" + serviceId + " using ClassLoader: " + cl);
        }
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is));
        }
        try {
            String factoryClassName = rd.readLine();
            rd.close();
            if (factoryClassName == null || "".equals(factoryClassName)) {
                return null;
            }
            dPrint("found in resource, value=" + factoryClassName);
            return newInstance(factoryClassName, cl, false);
        } catch (IOException e2) {
            return null;
        }
    }

    static class ConfigurationError extends Error {
        private Exception exception;

        ConfigurationError(String msg, Exception x) {
            super(msg);
            this.exception = x;
        }

        Exception getException() {
            return this.exception;
        }

        @Override
        public Throwable getCause() {
            return this.exception;
        }
    }
}
