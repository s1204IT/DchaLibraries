package javax.xml.xpath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import libcore.io.IoUtils;

final class XPathFactoryFinder {
    private static final int DEFAULT_LINE_LENGTH = 80;
    private static final Class SERVICE_CLASS;
    private static final String SERVICE_ID;
    private static boolean debug;
    private final ClassLoader classLoader;

    static {
        boolean z = false;
        debug = false;
        String property = System.getProperty("jaxp.debug");
        if (property != null && !"false".equals(property)) {
            z = true;
        }
        debug = z;
        SERVICE_CLASS = XPathFactory.class;
        SERVICE_ID = "META-INF/services/" + SERVICE_CLASS.getName();
    }

    private static class CacheHolder {
        private static Properties cacheProps = new Properties();

        private CacheHolder() {
        }

        static {
            File file = new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jaxp.properties");
            if (file.exists()) {
                if (XPathFactoryFinder.debug) {
                    XPathFactoryFinder.debugPrintln("Read properties file " + file);
                }
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    try {
                        cacheProps.load(fileInputStream);
                        fileInputStream.close();
                    } finally {
                    }
                } catch (Exception e) {
                    if (XPathFactoryFinder.debug) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void debugPrintln(String str) {
        if (debug) {
            System.err.println("JAXP: " + str);
        }
    }

    public XPathFactoryFinder(ClassLoader classLoader) {
        this.classLoader = classLoader;
        if (debug) {
            debugDisplayClassLoader();
        }
    }

    private void debugDisplayClassLoader() {
        if (this.classLoader == Thread.currentThread().getContextClassLoader()) {
            debugPrintln("using thread context class loader (" + this.classLoader + ") for search");
            return;
        }
        if (this.classLoader == ClassLoader.getSystemClassLoader()) {
            debugPrintln("using system class loader (" + this.classLoader + ") for search");
            return;
        }
        debugPrintln("using class loader (" + this.classLoader + ") for search");
    }

    public XPathFactory newFactory(String str) {
        if (str == null) {
            throw new NullPointerException("uri == null");
        }
        XPathFactory xPathFactory_newFactory = _newFactory(str);
        if (debug) {
            if (xPathFactory_newFactory != null) {
                debugPrintln("factory '" + xPathFactory_newFactory.getClass().getName() + "' was found for " + str);
            } else {
                debugPrintln("unable to find a factory for " + str);
            }
        }
        return xPathFactory_newFactory;
    }

    private XPathFactory _newFactory(String str) {
        XPathFactory xPathFactoryLoadFromServicesFile;
        String str2 = SERVICE_CLASS.getName() + ":" + str;
        try {
            if (debug) {
                debugPrintln("Looking up system property '" + str2 + "'");
            }
            String property = System.getProperty(str2);
            if (property != null && property.length() > 0) {
                if (debug) {
                    debugPrintln("The value is '" + property + "'");
                }
                XPathFactory xPathFactoryCreateInstance = createInstance(property);
                if (xPathFactoryCreateInstance != null) {
                    return xPathFactoryCreateInstance;
                }
            } else if (debug) {
                debugPrintln("The property is undefined.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String property2 = CacheHolder.cacheProps.getProperty(str2);
            if (debug) {
                debugPrintln("found " + property2 + " in $java.home/jaxp.properties");
            }
            if (property2 != null) {
                XPathFactory xPathFactoryCreateInstance2 = createInstance(property2);
                if (xPathFactoryCreateInstance2 != null) {
                    return xPathFactoryCreateInstance2;
                }
            }
        } catch (Exception e2) {
            if (debug) {
                e2.printStackTrace();
            }
        }
        for (URL url : createServiceFileIterator()) {
            if (debug) {
                debugPrintln("looking into " + url);
            }
            try {
                xPathFactoryLoadFromServicesFile = loadFromServicesFile(str, url.toExternalForm(), url.openStream());
            } catch (IOException e3) {
                if (debug) {
                    debugPrintln("failed to read " + url);
                    e3.printStackTrace();
                }
            }
            if (xPathFactoryLoadFromServicesFile != null) {
                return xPathFactoryLoadFromServicesFile;
            }
        }
        if (str.equals("http://java.sun.com/jaxp/xpath/dom")) {
            if (debug) {
                debugPrintln("attempting to use the platform default W3C DOM XPath lib");
            }
            return createInstance("org.apache.xpath.jaxp.XPathFactoryImpl");
        }
        if (debug) {
            debugPrintln("all things were tried, but none was found. bailing out.");
            return null;
        }
        return null;
    }

    XPathFactory createInstance(String str) {
        Class<?> cls;
        try {
            if (debug) {
                debugPrintln("instantiating " + str);
            }
            if (this.classLoader != null) {
                cls = this.classLoader.loadClass(str);
            } else {
                cls = Class.forName(str);
            }
            if (debug) {
                debugPrintln("loaded it from " + which(cls));
            }
            Object objNewInstance = cls.newInstance();
            if (objNewInstance instanceof XPathFactory) {
                return (XPathFactory) objNewInstance;
            }
            if (debug) {
                debugPrintln(str + " is not assignable to " + SERVICE_CLASS.getName());
                return null;
            }
            return null;
        } catch (ThreadDeath e) {
            throw e;
        } catch (VirtualMachineError e2) {
            throw e2;
        } catch (Throwable th) {
            if (debug) {
                debugPrintln("failed to instantiate " + str);
                th.printStackTrace();
                return null;
            }
            return null;
        }
    }

    private XPathFactory loadFromServicesFile(String str, String str2, InputStream inputStream) {
        BufferedReader bufferedReader;
        if (debug) {
            debugPrintln("Reading " + str2);
        }
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 80);
        } catch (UnsupportedEncodingException e) {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 80);
        }
        XPathFactory xPathFactory = null;
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                int iIndexOf = line.indexOf(35);
                if (iIndexOf != -1) {
                    line = line.substring(0, iIndexOf);
                }
                String strTrim = line.trim();
                if (strTrim.length() != 0) {
                    try {
                        XPathFactory xPathFactoryCreateInstance = createInstance(strTrim);
                        if (xPathFactoryCreateInstance.isObjectModelSupported(str)) {
                            xPathFactory = xPathFactoryCreateInstance;
                            break;
                        }
                    } catch (Exception e2) {
                    }
                }
            } catch (IOException e3) {
            }
        }
        IoUtils.closeQuietly(bufferedReader);
        return xPathFactory;
    }

    private Iterable<URL> createServiceFileIterator() {
        if (this.classLoader == null) {
            return Collections.singleton(XPathFactoryFinder.class.getClassLoader().getResource(SERVICE_ID));
        }
        try {
            Enumeration<URL> resources = this.classLoader.getResources(SERVICE_ID);
            if (debug && !resources.hasMoreElements()) {
                debugPrintln("no " + SERVICE_ID + " file was found");
            }
            return Collections.list(resources);
        } catch (IOException e) {
            if (debug) {
                debugPrintln("failed to enumerate resources " + SERVICE_ID);
                e.printStackTrace();
            }
            return Collections.emptySet();
        }
    }

    private static String which(Class cls) {
        return which(cls.getName(), cls.getClassLoader());
    }

    private static String which(String str, ClassLoader classLoader) {
        String str2 = str.replace('.', '/') + ".class";
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        URL resource = classLoader.getResource(str2);
        if (resource != null) {
            return resource.toString();
        }
        return null;
    }
}
