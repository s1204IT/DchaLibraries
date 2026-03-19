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
        String val = System.getProperty("jaxp.debug");
        if (val != null && !"false".equals(val)) {
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
            String javah = System.getProperty("java.home");
            String configFile = javah + File.separator + "lib" + File.separator + "jaxp.properties";
            File f = new File(configFile);
            if (!f.exists()) {
                return;
            }
            if (SchemaFactoryFinder.debug) {
                SchemaFactoryFinder.debugPrintln("Read properties file " + f);
            }
            try {
                cacheProps.load(new FileInputStream(f));
            } catch (Exception ex) {
                if (!SchemaFactoryFinder.debug) {
                    return;
                }
                ex.printStackTrace();
            }
        }
    }

    private static void debugPrintln(String msg) {
        if (!debug) {
            return;
        }
        System.err.println("JAXP: " + msg);
    }

    public SchemaFactoryFinder(ClassLoader loader) {
        this.classLoader = loader;
        if (!debug) {
            return;
        }
        debugDisplayClassLoader();
    }

    private void debugDisplayClassLoader() {
        if (this.classLoader == Thread.currentThread().getContextClassLoader()) {
            debugPrintln("using thread context class loader (" + this.classLoader + ") for search");
        } else if (this.classLoader == ClassLoader.getSystemClassLoader()) {
            debugPrintln("using system class loader (" + this.classLoader + ") for search");
        } else {
            debugPrintln("using class loader (" + this.classLoader + ") for search");
        }
    }

    public SchemaFactory newFactory(String schemaLanguage) {
        if (schemaLanguage == null) {
            throw new NullPointerException("schemaLanguage == null");
        }
        SchemaFactory f = _newFactory(schemaLanguage);
        if (debug) {
            if (f != null) {
                debugPrintln("factory '" + f.getClass().getName() + "' was found for " + schemaLanguage);
            } else {
                debugPrintln("unable to find a factory for " + schemaLanguage);
            }
        }
        return f;
    }

    private SchemaFactory _newFactory(String schemaLanguage) {
        SchemaFactory sf;
        String propertyName = SERVICE_CLASS.getName() + ":" + schemaLanguage;
        try {
            if (debug) {
                debugPrintln("Looking up system property '" + propertyName + "'");
            }
            String r = System.getProperty(propertyName);
            if (r != null && r.length() > 0) {
                if (debug) {
                    debugPrintln("The value is '" + r + "'");
                }
                SchemaFactory sf2 = createInstance(r);
                if (sf2 != null) {
                    return sf2;
                }
            } else if (debug) {
                debugPrintln("The property is undefined.");
            }
        } catch (ThreadDeath td) {
            throw td;
        } catch (VirtualMachineError vme) {
            throw vme;
        } catch (Throwable t) {
            if (debug) {
                debugPrintln("failed to look up system property '" + propertyName + "'");
                t.printStackTrace();
            }
        }
        try {
            String factoryClassName = CacheHolder.cacheProps.getProperty(propertyName);
            if (debug) {
                debugPrintln("found " + factoryClassName + " in $java.home/jaxp.properties");
            }
            if (factoryClassName != null) {
                SchemaFactory sf3 = createInstance(factoryClassName);
                if (sf3 != null) {
                    return sf3;
                }
            }
        } catch (Exception ex) {
            if (debug) {
                ex.printStackTrace();
            }
        }
        for (URL resource : createServiceFileIterator()) {
            if (debug) {
                debugPrintln("looking into " + resource);
            }
            try {
                sf = loadFromServicesFile(schemaLanguage, resource.toExternalForm(), resource.openStream());
            } catch (IOException e) {
                if (debug) {
                    debugPrintln("failed to read " + resource);
                    e.printStackTrace();
                }
            }
            if (sf != null) {
                return sf;
            }
        }
        if (schemaLanguage.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI) || schemaLanguage.equals(W3C_XML_SCHEMA10_NS_URI)) {
            if (debug) {
                debugPrintln("attempting to use the platform default XML Schema 1.0 validator");
            }
            return createInstance("org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        }
        if (schemaLanguage.equals(W3C_XML_SCHEMA11_NS_URI)) {
            if (debug) {
                debugPrintln("attempting to use the platform default XML Schema 1.1 validator");
            }
            return createInstance("org.apache.xerces.jaxp.validation.XMLSchema11Factory");
        }
        if (debug) {
            debugPrintln("all things were tried, but none was found. bailing out.");
        }
        return null;
    }

    SchemaFactory createInstance(String className) {
        Class<?> cls;
        ?? NewInstance;
        try {
            if (debug) {
                debugPrintln("instantiating " + className);
            }
            if (this.classLoader != null) {
                cls = this.classLoader.loadClass(className);
            } else {
                cls = Class.forName(className);
            }
            if (debug) {
                debugPrintln("loaded it from " + which(cls));
            }
            NewInstance = cls.newInstance();
        } catch (ThreadDeath td) {
            throw td;
        } catch (VirtualMachineError vme) {
            throw vme;
        } catch (Throwable t) {
            debugPrintln("failed to instantiate " + className);
            if (debug) {
                t.printStackTrace();
            }
        }
        if (NewInstance instanceof SchemaFactory) {
            return NewInstance;
        }
        if (debug) {
            debugPrintln(className + " is not assignable to " + SERVICE_CLASS.getName());
        }
        return null;
    }

    private Iterable<URL> createServiceFileIterator() {
        if (this.classLoader == null) {
            ClassLoader classLoader = SchemaFactoryFinder.class.getClassLoader();
            return Collections.singleton(classLoader.getResource(SERVICE_ID));
        }
        try {
            Enumeration<URL> e = this.classLoader.getResources(SERVICE_ID);
            if (debug && !e.hasMoreElements()) {
                debugPrintln("no " + SERVICE_ID + " file was found");
            }
            return Collections.list(e);
        } catch (IOException e2) {
            if (debug) {
                debugPrintln("failed to enumerate resources " + SERVICE_ID);
                e2.printStackTrace();
            }
            return Collections.emptySet();
        }
    }

    private SchemaFactory loadFromServicesFile(String schemaLanguage, String resourceName, InputStream in) {
        BufferedReader rd;
        if (debug) {
            debugPrintln("Reading " + resourceName);
        }
        try {
            rd = new BufferedReader(new InputStreamReader(in, "UTF-8"), 80);
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(in), 80);
        }
        SchemaFactory resultFactory = null;
        while (true) {
            try {
                String factoryClassName = rd.readLine();
                if (factoryClassName == null) {
                    break;
                }
                int hashIndex = factoryClassName.indexOf(35);
                if (hashIndex != -1) {
                    factoryClassName = factoryClassName.substring(0, hashIndex);
                }
                String factoryClassName2 = factoryClassName.trim();
                if (factoryClassName2.length() != 0) {
                    try {
                        SchemaFactory foundFactory = createInstance(factoryClassName2);
                        if (foundFactory.isSchemaLanguageSupported(schemaLanguage)) {
                            resultFactory = foundFactory;
                            break;
                        }
                    } catch (Exception e2) {
                    }
                }
            } catch (IOException e3) {
            }
        }
        IoUtils.closeQuietly(rd);
        return resultFactory;
    }

    private static String which(Class clazz) {
        return which(clazz.getName(), clazz.getClassLoader());
    }

    private static String which(String classname, ClassLoader loader) {
        String classnameAsResource = classname.replace('.', '/') + ".class";
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        URL it = loader.getResource(classnameAsResource);
        if (it != null) {
            return it.toString();
        }
        return null;
    }
}
