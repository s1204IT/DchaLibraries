package javax.xml.validation;

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
import javax.xml.XMLConstants;
import libcore.io.IoUtils;

final class SchemaFactoryFinder {
    private static final int DEFAULT_LINE_LENGTH = 80;
    private static final Class SERVICE_CLASS;
    private static final String SERVICE_ID;
    private static final String W3C_XML_SCHEMA10_NS_URI = "http://www.w3.org/XML/XMLSchema/v1.0";
    private static final String W3C_XML_SCHEMA11_NS_URI = "http://www.w3.org/XML/XMLSchema/v1.1";
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
        SERVICE_CLASS = SchemaFactory.class;
        SERVICE_ID = "META-INF/services/" + SERVICE_CLASS.getName();
    }

    private static class CacheHolder {
        private static Properties cacheProps = new Properties();

        private CacheHolder() {
        }

        static {
            File file = new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jaxp.properties");
            if (file.exists()) {
                if (SchemaFactoryFinder.debug) {
                    SchemaFactoryFinder.debugPrintln("Read properties file " + file);
                }
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    try {
                        cacheProps.load(fileInputStream);
                        fileInputStream.close();
                    } finally {
                    }
                } catch (Exception e) {
                    if (SchemaFactoryFinder.debug) {
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

    public SchemaFactoryFinder(ClassLoader classLoader) {
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

    public SchemaFactory newFactory(String str) {
        if (str == null) {
            throw new NullPointerException("schemaLanguage == null");
        }
        SchemaFactory schemaFactory_newFactory = _newFactory(str);
        if (debug) {
            if (schemaFactory_newFactory != null) {
                debugPrintln("factory '" + schemaFactory_newFactory.getClass().getName() + "' was found for " + str);
            } else {
                debugPrintln("unable to find a factory for " + str);
            }
        }
        return schemaFactory_newFactory;
    }

    private SchemaFactory _newFactory(String str) {
        SchemaFactory schemaFactoryLoadFromServicesFile;
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
                SchemaFactory schemaFactoryCreateInstance = createInstance(property);
                if (schemaFactoryCreateInstance != null) {
                    return schemaFactoryCreateInstance;
                }
            } else if (debug) {
                debugPrintln("The property is undefined.");
            }
        } catch (ThreadDeath e) {
            throw e;
        } catch (VirtualMachineError e2) {
            throw e2;
        } catch (Throwable th) {
            if (debug) {
                debugPrintln("failed to look up system property '" + str2 + "'");
                th.printStackTrace();
            }
        }
        try {
            String property2 = CacheHolder.cacheProps.getProperty(str2);
            if (debug) {
                debugPrintln("found " + property2 + " in $java.home/jaxp.properties");
            }
            if (property2 != null) {
                SchemaFactory schemaFactoryCreateInstance2 = createInstance(property2);
                if (schemaFactoryCreateInstance2 != null) {
                    return schemaFactoryCreateInstance2;
                }
            }
        } catch (Exception e3) {
            if (debug) {
                e3.printStackTrace();
            }
        }
        for (URL url : createServiceFileIterator()) {
            if (debug) {
                debugPrintln("looking into " + url);
            }
            try {
                schemaFactoryLoadFromServicesFile = loadFromServicesFile(str, url.toExternalForm(), url.openStream());
            } catch (IOException e4) {
                if (debug) {
                    debugPrintln("failed to read " + url);
                    e4.printStackTrace();
                }
            }
            if (schemaFactoryLoadFromServicesFile != null) {
                return schemaFactoryLoadFromServicesFile;
            }
        }
        if (str.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI) || str.equals(W3C_XML_SCHEMA10_NS_URI)) {
            if (debug) {
                debugPrintln("attempting to use the platform default XML Schema 1.0 validator");
            }
            return createInstance("org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        }
        if (str.equals(W3C_XML_SCHEMA11_NS_URI)) {
            if (debug) {
                debugPrintln("attempting to use the platform default XML Schema 1.1 validator");
            }
            return createInstance("org.apache.xerces.jaxp.validation.XMLSchema11Factory");
        }
        if (debug) {
            debugPrintln("all things were tried, but none was found. bailing out.");
            return null;
        }
        return null;
    }

    SchemaFactory createInstance(String str) {
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
            if (objNewInstance instanceof SchemaFactory) {
                return (SchemaFactory) objNewInstance;
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
            debugPrintln("failed to instantiate " + str);
            if (debug) {
                th.printStackTrace();
                return null;
            }
            return null;
        }
    }

    private Iterable<URL> createServiceFileIterator() {
        if (this.classLoader == null) {
            return Collections.singleton(SchemaFactoryFinder.class.getClassLoader().getResource(SERVICE_ID));
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

    private SchemaFactory loadFromServicesFile(String str, String str2, InputStream inputStream) {
        BufferedReader bufferedReader;
        if (debug) {
            debugPrintln("Reading " + str2);
        }
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 80);
        } catch (UnsupportedEncodingException e) {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 80);
        }
        SchemaFactory schemaFactory = null;
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
                        SchemaFactory schemaFactoryCreateInstance = createInstance(strTrim);
                        if (schemaFactoryCreateInstance.isSchemaLanguageSupported(str)) {
                            schemaFactory = schemaFactoryCreateInstance;
                            break;
                        }
                    } catch (Exception e2) {
                    }
                }
            } catch (IOException e3) {
            }
        }
        IoUtils.closeQuietly(bufferedReader);
        return schemaFactory;
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
