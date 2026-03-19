package javax.xml.datatype;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Properties;
import libcore.io.IoUtils;

final class FactoryFinder {
    private static final String CLASS_NAME = "javax.xml.datatype.FactoryFinder";
    private static final int DEFAULT_LINE_LENGTH = 80;
    private static boolean debug;

    static {
        boolean z = false;
        debug = false;
        String val = System.getProperty("jaxp.debug");
        if (val != null && !"false".equals(val)) {
            z = true;
        }
        debug = z;
    }

    private static class CacheHolder {
        private static Properties cacheProps = new Properties();

        private CacheHolder() {
        }

        static {
            String javah = System.getProperty("java.home");
            String configFile = javah + File.separator + "lib" + File.separator + "jaxp.properties";
            File f = new File(configFile);
            if (!f.exists()) {
                return;
            }
            if (FactoryFinder.debug) {
                FactoryFinder.debugPrintln("Read properties file " + f);
            }
            try {
                cacheProps.load(new FileInputStream(f));
            } catch (Exception ex) {
                if (!FactoryFinder.debug) {
                    return;
                }
                ex.printStackTrace();
            }
        }
    }

    private FactoryFinder() {
    }

    private static void debugPrintln(String msg) {
        if (!debug) {
            return;
        }
        System.err.println("javax.xml.datatype.FactoryFinder:" + msg);
    }

    private static ClassLoader findClassLoader() throws ConfigurationError {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (debug) {
            debugPrintln("Using context class loader: " + classLoader);
        }
        if (classLoader == null) {
            classLoader = FactoryFinder.class.getClassLoader();
            if (debug) {
                debugPrintln("Using the class loader of FactoryFinder: " + classLoader);
            }
        }
        return classLoader;
    }

    static Object newInstance(String className, ClassLoader classLoader) throws ConfigurationError {
        Class<?> clsLoadClass;
        try {
            if (classLoader == null) {
                clsLoadClass = Class.forName(className);
            } else {
                clsLoadClass = classLoader.loadClass(className);
            }
            if (debug) {
                debugPrintln("Loaded " + className + " from " + which(clsLoadClass));
            }
            return clsLoadClass.newInstance();
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError("Provider " + className + " not found", x);
        } catch (Exception x2) {
            throw new ConfigurationError("Provider " + className + " could not be instantiated: " + x2, x2);
        }
    }

    static Object find(String factoryId, String fallbackClassName) throws ConfigurationError {
        ClassLoader classLoader = findClassLoader();
        String systemProp = System.getProperty(factoryId);
        if (systemProp != null && systemProp.length() > 0) {
            if (debug) {
                debugPrintln("found " + systemProp + " in the system property " + factoryId);
            }
            return newInstance(systemProp, classLoader);
        }
        try {
            String factoryClassName = CacheHolder.cacheProps.getProperty(factoryId);
            if (debug) {
                debugPrintln("found " + factoryClassName + " in $java.home/jaxp.properties");
            }
            if (factoryClassName != null) {
                return newInstance(factoryClassName, classLoader);
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
        if (debug) {
            debugPrintln("loaded from fallback value: " + fallbackClassName);
        }
        return newInstance(fallbackClassName, classLoader);
    }

    private static Object findJarServiceProvider(String factoryId) throws ConfigurationError {
        BufferedReader rd;
        String serviceId = "META-INF/services/" + factoryId;
        InputStream is = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            is = cl.getResourceAsStream(serviceId);
        }
        if (is == null) {
            cl = FactoryFinder.class.getClassLoader();
            is = cl.getResourceAsStream(serviceId);
        }
        if (is == null) {
            return null;
        }
        if (debug) {
            debugPrintln("found jar resource=" + serviceId + " using ClassLoader: " + cl);
        }
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"), 80);
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is), 80);
        }
        try {
            String factoryClassName = rd.readLine();
            if (factoryClassName == null || "".equals(factoryClassName)) {
                return null;
            }
            if (debug) {
                debugPrintln("found in resource, value=" + factoryClassName);
            }
            return newInstance(factoryClassName, cl);
        } catch (IOException e2) {
            return null;
        } finally {
            IoUtils.closeQuietly(rd);
        }
    }

    static class ConfigurationError extends Error {
        private static final long serialVersionUID = -3644413026244211347L;
        private Exception exception;

        ConfigurationError(String msg, Exception x) {
            super(msg);
            this.exception = x;
        }

        Exception getException() {
            return this.exception;
        }
    }

    private static String which(Class clazz) {
        URL it;
        try {
            String classnameAsResource = clazz.getName().replace('.', '/') + ".class";
            ClassLoader loader = clazz.getClassLoader();
            if (loader != null) {
                it = loader.getResource(classnameAsResource);
            } else {
                it = ClassLoader.getSystemResource(classnameAsResource);
            }
            if (it != null) {
                return it.toString();
            }
            return "unknown location";
        } catch (ThreadDeath td) {
            throw td;
        } catch (VirtualMachineError vme) {
            throw vme;
        } catch (Throwable t) {
            if (debug) {
                t.printStackTrace();
                return "unknown location";
            }
            return "unknown location";
        }
    }
}
