package mf.javax.xml.validation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

class SchemaFactoryFinder {
    private static final Class SERVICE_CLASS;
    private static final String SERVICE_ID;
    private static boolean debug;
    private final ClassLoader classLoader;
    private static SecuritySupport ss = new SecuritySupport();
    private static Properties cacheProps = new Properties();
    private static volatile boolean firstTime = true;

    static {
        debug = false;
        boolean z = true;
        try {
            if (ss.getSystemProperty("jaxp.debug") == null) {
                z = false;
            }
            debug = z;
        } catch (Exception e) {
            debug = false;
        }
        SERVICE_CLASS = SchemaFactory.class;
        SERVICE_ID = "META-INF/services/" + SERVICE_CLASS.getName();
    }

    private static void debugPrintln(String msg) {
        if (debug) {
            System.err.println("JAXP: " + msg);
        }
    }

    public SchemaFactoryFinder(ClassLoader loader) {
        this.classLoader = loader;
        if (debug) {
            debugDisplayClassLoader();
        }
    }

    private void debugDisplayClassLoader() {
        try {
            if (this.classLoader == ss.getContextClassLoader()) {
                debugPrintln("using thread context class loader (" + this.classLoader + ") for search");
                return;
            }
        } catch (Throwable th) {
        }
        if (this.classLoader == ClassLoader.getSystemClassLoader()) {
            debugPrintln("using system class loader (" + this.classLoader + ") for search");
            return;
        }
        debugPrintln("using class loader (" + this.classLoader + ") for search");
    }

    public SchemaFactory newFactory(String schemaLanguage) {
        if (schemaLanguage == null) {
            throw new NullPointerException();
        }
        SchemaFactory f = _newFactory(schemaLanguage);
        if (f != null) {
            debugPrintln("factory '" + f.getClass().getName() + "' was found for " + schemaLanguage);
        } else {
            debugPrintln("unable to find a factory for " + schemaLanguage);
        }
        return f;
    }

    private SchemaFactory _newFactory(String schemaLanguage) {
        SchemaFactory sf;
        SchemaFactory sf2;
        String propertyName = String.valueOf(SERVICE_CLASS.getName()) + ":" + schemaLanguage;
        try {
            debugPrintln("Looking up system property '" + propertyName + "'");
            String r = ss.getSystemProperty(propertyName);
            if (r != null) {
                debugPrintln("The value is '" + r + "'");
                SchemaFactory sf3 = createInstance(r, true);
                if (sf3 != null) {
                    return sf3;
                }
            } else {
                debugPrintln("The property is undefined.");
            }
        } catch (Throwable t) {
            if (debug) {
                debugPrintln("failed to look up system property '" + propertyName + "'");
                t.printStackTrace();
            }
        }
        String javah = ss.getSystemProperty("java.home");
        String configFile = String.valueOf(javah) + File.separator + "lib" + File.separator + "jaxp.properties";
        try {
            if (firstTime) {
                synchronized (cacheProps) {
                    if (firstTime) {
                        File f = new File(configFile);
                        firstTime = false;
                        if (ss.doesFileExist(f)) {
                            debugPrintln("Read properties file " + f);
                            cacheProps.load(ss.getFileInputStream(f));
                        }
                    }
                }
            }
            String factoryClassName = cacheProps.getProperty(propertyName);
            debugPrintln("found " + factoryClassName + " in $java.home/jaxp.properties");
            if (factoryClassName != null && (sf2 = createInstance(factoryClassName, true)) != null) {
                return sf2;
            }
        } catch (Exception ex) {
            if (debug) {
                ex.printStackTrace();
            }
        }
        Iterator sitr = createServiceFileIterator();
        while (sitr.hasNext()) {
            URL resource = (URL) sitr.next();
            debugPrintln("looking into " + resource);
            try {
                sf = loadFromService(schemaLanguage, resource.toExternalForm(), ss.getURLInputStream(resource));
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
        if (schemaLanguage.equals("http://www.w3.org/2001/XMLSchema")) {
            debugPrintln("attempting to use the platform default XML Schema validator");
            return createInstance("com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory", true);
        }
        debugPrintln("all things were tried, but none was found. bailing out.");
        return null;
    }

    private Class createClass(String className) {
        try {
            if (this.classLoader != null) {
                return this.classLoader.loadClass(className);
            }
            return Class.forName(className);
        } catch (Throwable t) {
            if (debug) {
                t.printStackTrace();
                return null;
            }
            return null;
        }
    }

    SchemaFactory createInstance(String className) {
        return createInstance(className, false);
    }

    SchemaFactory createInstance(String className, boolean useServicesMechanism) {
        SchemaFactory schemaFactory = null;
        debugPrintln("createInstance(" + className + ")");
        Class clazz = createClass(className);
        if (clazz == null) {
            debugPrintln("failed to getClass(" + className + ")");
            return null;
        }
        debugPrintln("loaded " + className + " from " + which(clazz));
        if (!useServicesMechanism) {
            try {
                schemaFactory = (SchemaFactory) newInstanceNoServiceLoader(clazz);
            } catch (ClassCastException classCastException) {
                debugPrintln("could not instantiate " + clazz.getName());
                if (debug) {
                    classCastException.printStackTrace();
                }
                return null;
            } catch (IllegalAccessException illegalAccessException) {
                debugPrintln("could not instantiate " + clazz.getName());
                if (debug) {
                    illegalAccessException.printStackTrace();
                }
                return null;
            } catch (InstantiationException instantiationException) {
                debugPrintln("could not instantiate " + clazz.getName());
                if (debug) {
                    instantiationException.printStackTrace();
                }
                return null;
            }
        }
        if (schemaFactory == null) {
            SchemaFactory schemaFactory2 = (SchemaFactory) clazz.newInstance();
            return schemaFactory2;
        }
        return schemaFactory;
    }

    private static Object newInstanceNoServiceLoader(Class<?> providerClass) {
        if (System.getSecurityManager() == null) {
            return null;
        }
        try {
            Method creationMethod = providerClass.getDeclaredMethod("newXMLSchemaFactoryNoServiceLoader", new Class[0]);
            return creationMethod.invoke(null, null);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e2) {
            return null;
        }
    }

    private static abstract class SingleIterator implements Iterator {
        private boolean seen;

        protected abstract Object value();

        private SingleIterator() {
            this.seen = false;
        }

        SingleIterator(SingleIterator singleIterator) {
            this();
        }

        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean hasNext() {
            return !this.seen;
        }

        @Override
        public final Object next() {
            if (this.seen) {
                throw new NoSuchElementException();
            }
            this.seen = true;
            return value();
        }
    }

    private SchemaFactory loadFromService(String schemaLanguage, String inputName, InputStream in) throws IOException {
        Class clazz;
        SchemaFactory schemaFactory = null;
        Class<?>[] clsArr = {"".getClass()};
        Object[] schemaLanguageObjectArray = {schemaLanguage};
        debugPrintln("Reading " + inputName);
        BufferedReader configFile = new BufferedReader(new InputStreamReader(in));
        while (true) {
            String line = configFile.readLine();
            String line2 = line;
            if (line != null) {
                int comment = line2.indexOf("#");
                switch (comment) {
                    case -1:
                        break;
                    case 0:
                        line2 = "";
                        break;
                    default:
                        line2 = line2.substring(0, comment);
                        break;
                }
                String line3 = line2.trim();
                if (line3.length() != 0 && (clazz = createClass(line3)) != null) {
                    try {
                        schemaFactory = (SchemaFactory) clazz.newInstance();
                        try {
                            Method isSchemaLanguageSupported = clazz.getMethod("isSchemaLanguageSupported", clsArr);
                            Boolean supported = (Boolean) isSchemaLanguageSupported.invoke(schemaFactory, schemaLanguageObjectArray);
                            if (supported.booleanValue()) {
                            }
                        } catch (IllegalAccessException e) {
                        } catch (NoSuchMethodException e2) {
                        } catch (InvocationTargetException e3) {
                        }
                        schemaFactory = null;
                    } catch (ClassCastException e4) {
                        schemaFactory = null;
                    } catch (IllegalAccessException e5) {
                        schemaFactory = null;
                    } catch (InstantiationException e6) {
                        schemaFactory = null;
                    }
                }
            }
        }
        configFile.close();
        return schemaFactory;
    }

    private Iterator createServiceFileIterator() {
        if (this.classLoader == null) {
            return new SingleIterator() {
                @Override
                protected Object value() {
                    ClassLoader classLoader = SchemaFactoryFinder.class.getClassLoader();
                    return SchemaFactoryFinder.ss.getResourceAsURL(classLoader, SchemaFactoryFinder.SERVICE_ID);
                }
            };
        }
        try {
            final Enumeration e = ss.getResources(this.classLoader, SERVICE_ID);
            if (!e.hasMoreElements()) {
                debugPrintln("no " + SERVICE_ID + " file was found");
            }
            return new Iterator() {
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean hasNext() {
                    return e.hasMoreElements();
                }

                @Override
                public Object next() {
                    return e.nextElement();
                }
            };
        } catch (IOException e2) {
            debugPrintln("failed to enumerate resources " + SERVICE_ID);
            if (debug) {
                e2.printStackTrace();
            }
            return new ArrayList().iterator();
        }
    }

    private static String which(Class clazz) {
        return which(clazz.getName(), clazz.getClassLoader());
    }

    private static String which(String classname, ClassLoader loader) {
        String classnameAsResource = String.valueOf(classname.replace('.', '/')) + ".class";
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        URL it = ss.getResourceAsURL(loader, classnameAsResource);
        if (it != null) {
            return it.toString();
        }
        return null;
    }
}
