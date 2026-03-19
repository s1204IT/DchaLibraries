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
        String property = System.getProperty("jaxp.debug");
        if (property != null && !"false".equals(property)) {
            z = true;
        }
        debug = z;
    }

    private static class CacheHolder {
        private static Properties cacheProps = new Properties();

        private CacheHolder() {
        }

        static {
            File file = new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jaxp.properties");
            if (file.exists()) {
                if (FactoryFinder.debug) {
                    FactoryFinder.debugPrintln("Read properties file " + file);
                }
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    try {
                        cacheProps.load(fileInputStream);
                        fileInputStream.close();
                    } finally {
                    }
                } catch (Exception e) {
                    if (FactoryFinder.debug) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private FactoryFinder() {
    }

    private static void debugPrintln(String str) {
        if (debug) {
            System.err.println("javax.xml.datatype.FactoryFinder:" + str);
        }
    }

    private static ClassLoader findClassLoader() throws ConfigurationError {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (debug) {
            debugPrintln("Using context class loader: " + contextClassLoader);
        }
        if (contextClassLoader == null) {
            contextClassLoader = FactoryFinder.class.getClassLoader();
            if (debug) {
                debugPrintln("Using the class loader of FactoryFinder: " + contextClassLoader);
            }
        }
        return contextClassLoader;
    }

    static Object newInstance(String str, ClassLoader classLoader) throws ConfigurationError {
        Class<?> clsLoadClass;
        try {
            if (classLoader == null) {
                clsLoadClass = Class.forName(str);
            } else {
                clsLoadClass = classLoader.loadClass(str);
            }
            if (debug) {
                debugPrintln("Loaded " + str + " from " + which(clsLoadClass));
            }
            return clsLoadClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new ConfigurationError("Provider " + str + " not found", e);
        } catch (Exception e2) {
            throw new ConfigurationError("Provider " + str + " could not be instantiated: " + e2, e2);
        }
    }

    static Object find(String str, String str2) throws ConfigurationError {
        ClassLoader classLoaderFindClassLoader = findClassLoader();
        String property = System.getProperty(str);
        if (property == null || property.length() <= 0) {
            try {
                String property2 = CacheHolder.cacheProps.getProperty(str);
                if (debug) {
                    debugPrintln("found " + property2 + " in $java.home/jaxp.properties");
                }
                if (property2 != null) {
                    return newInstance(property2, classLoaderFindClassLoader);
                }
            } catch (Exception e) {
                if (debug) {
                    e.printStackTrace();
                }
            }
            Object objFindJarServiceProvider = findJarServiceProvider(str);
            if (objFindJarServiceProvider != null) {
                return objFindJarServiceProvider;
            }
            if (str2 == null) {
                throw new ConfigurationError("Provider for " + str + " cannot be found", null);
            }
            if (debug) {
                debugPrintln("loaded from fallback value: " + str2);
            }
            return newInstance(str2, classLoaderFindClassLoader);
        }
        if (debug) {
            debugPrintln("found " + property + " in the system property " + str);
        }
        return newInstance(property, classLoaderFindClassLoader);
    }

    private static Object findJarServiceProvider(String str) throws ConfigurationError {
        InputStream resourceAsStream;
        BufferedReader bufferedReader;
        String str2 = "META-INF/services/" + str;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            resourceAsStream = contextClassLoader.getResourceAsStream(str2);
        } else {
            resourceAsStream = null;
        }
        if (resourceAsStream == null) {
            contextClassLoader = FactoryFinder.class.getClassLoader();
            resourceAsStream = contextClassLoader.getResourceAsStream(str2);
        }
        if (resourceAsStream == null) {
            return null;
        }
        if (debug) {
            debugPrintln("found jar resource=" + str2 + " using ClassLoader: " + contextClassLoader);
        }
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, "UTF-8"), 80);
        } catch (UnsupportedEncodingException e) {
            bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream), 80);
        }
        try {
            String line = bufferedReader.readLine();
            if (line == null || "".equals(line)) {
                return null;
            }
            if (debug) {
                debugPrintln("found in resource, value=" + line);
            }
            return newInstance(line, contextClassLoader);
        } catch (IOException e2) {
            return null;
        } finally {
            IoUtils.closeQuietly(bufferedReader);
        }
    }

    static class ConfigurationError extends Error {
        private static final long serialVersionUID = -3644413026244211347L;
        private Exception exception;

        ConfigurationError(String str, Exception exc) {
            super(str);
            this.exception = exc;
        }

        Exception getException() {
            return this.exception;
        }
    }

    private static String which(Class cls) {
        URL systemResource;
        try {
            String str = cls.getName().replace('.', '/') + ".class";
            ClassLoader classLoader = cls.getClassLoader();
            if (classLoader != null) {
                systemResource = classLoader.getResource(str);
            } else {
                systemResource = ClassLoader.getSystemResource(str);
            }
            if (systemResource != null) {
                return systemResource.toString();
            }
            return "unknown location";
        } catch (ThreadDeath e) {
            throw e;
        } catch (VirtualMachineError e2) {
            throw e2;
        } catch (Throwable th) {
            if (debug) {
                th.printStackTrace();
                return "unknown location";
            }
            return "unknown location";
        }
    }
}
