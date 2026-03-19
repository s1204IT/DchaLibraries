package org.apache.commons.logging;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import org.apache.commons.logging.impl.Jdk14Logger;

@Deprecated
public abstract class LogFactory {
    public static final String DIAGNOSTICS_DEST_PROPERTY = "org.apache.commons.logging.diagnostics.dest";
    public static final String FACTORY_DEFAULT = "org.apache.commons.logging.impl.LogFactoryImpl";
    public static final String FACTORY_PROPERTIES = "commons-logging.properties";
    public static final String FACTORY_PROPERTY = "org.apache.commons.logging.LogFactory";
    public static final String HASHTABLE_IMPLEMENTATION_PROPERTY = "org.apache.commons.logging.LogFactory.HashtableImpl";
    public static final String PRIORITY_KEY = "priority";
    protected static final String SERVICE_ID = "META-INF/services/org.apache.commons.logging.LogFactory";
    public static final String TCCL_KEY = "use_tccl";
    private static final String WEAK_HASHTABLE_CLASSNAME = "org.apache.commons.logging.impl.WeakHashtable";
    private static String diagnosticPrefix;
    protected static Hashtable factories;
    private static PrintStream diagnosticsStream = null;
    protected static LogFactory nullClassLoaderFactory = null;
    private static ClassLoader thisClassLoader = getClassLoader(LogFactory.class);

    public abstract Object getAttribute(String str);

    public abstract String[] getAttributeNames();

    public abstract Log getInstance(Class cls) throws LogConfigurationException;

    public abstract Log getInstance(String str) throws LogConfigurationException;

    public abstract void release();

    public abstract void removeAttribute(String str);

    public abstract void setAttribute(String str, Object obj);

    static {
        factories = null;
        initDiagnostics();
        logClassLoaderEnvironment(LogFactory.class);
        factories = createFactoryStore();
        if (isDiagnosticsEnabled()) {
            logDiagnostic("BOOTSTRAP COMPLETED");
        }
    }

    protected LogFactory() {
    }

    private static final Hashtable createFactoryStore() {
        Hashtable hashtable;
        String property = System.getProperty(HASHTABLE_IMPLEMENTATION_PROPERTY);
        if (property == null) {
            property = WEAK_HASHTABLE_CLASSNAME;
        }
        try {
            hashtable = (Hashtable) Class.forName(property).newInstance();
        } catch (Throwable th) {
            if (!WEAK_HASHTABLE_CLASSNAME.equals(property)) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[ERROR] LogFactory: Load of custom hashtable failed");
                } else {
                    System.err.println("[ERROR] LogFactory: Load of custom hashtable failed");
                }
            }
            hashtable = null;
        }
        if (hashtable == null) {
            return new Hashtable();
        }
        return hashtable;
    }

    public static LogFactory getFactory() throws LogConfigurationException {
        ClassLoader classLoader;
        BufferedReader bufferedReader;
        String property;
        ClassLoader contextClassLoader = getContextClassLoader();
        if (contextClassLoader == null && isDiagnosticsEnabled()) {
            logDiagnostic("Context classloader is null.");
        }
        LogFactory cachedFactory = getCachedFactory(contextClassLoader);
        if (cachedFactory != null) {
            return cachedFactory;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[LOOKUP] LogFactory implementation requested for the first time for context classloader " + objectId(contextClassLoader));
            logHierarchy("[LOOKUP] ", contextClassLoader);
        }
        Properties configurationFile = getConfigurationFile(contextClassLoader, FACTORY_PROPERTIES);
        if (configurationFile != null && (property = configurationFile.getProperty(TCCL_KEY)) != null && !Boolean.valueOf(property).booleanValue()) {
            classLoader = thisClassLoader;
        } else {
            classLoader = contextClassLoader;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[LOOKUP] Looking for system property [org.apache.commons.logging.LogFactory] to define the LogFactory subclass to use...");
        }
        try {
            String property2 = System.getProperty(FACTORY_PROPERTY);
            if (property2 != null) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] Creating an instance of LogFactory class '" + property2 + "' as specified by system property " + FACTORY_PROPERTY);
                }
                cachedFactory = newFactory(property2, classLoader, contextClassLoader);
            } else if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] No system property [org.apache.commons.logging.LogFactory] defined.");
            }
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] A security exception occurred while trying to create an instance of the custom factory class: [" + e.getMessage().trim() + "]. Trying alternative implementations...");
            }
        } catch (RuntimeException e2) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] An exception occurred while trying to create an instance of the custom factory class: [" + e2.getMessage().trim() + "] as specified by a system property.");
            }
            throw e2;
        }
        if (cachedFactory == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] Looking for a resource file of name [META-INF/services/org.apache.commons.logging.LogFactory] to define the LogFactory subclass to use...");
            }
            try {
                InputStream resourceAsStream = getResourceAsStream(contextClassLoader, SERVICE_ID);
                if (resourceAsStream != null) {
                    try {
                        bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, "UTF-8"));
                    } catch (UnsupportedEncodingException e3) {
                        bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
                    }
                    String line = bufferedReader.readLine();
                    bufferedReader.close();
                    if (line != null && !"".equals(line)) {
                        if (isDiagnosticsEnabled()) {
                            logDiagnostic("[LOOKUP]  Creating an instance of LogFactory class " + line + " as specified by file '" + SERVICE_ID + "' which was present in the path of the context classloader.");
                        }
                        cachedFactory = newFactory(line, classLoader, contextClassLoader);
                    }
                } else if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] No resource file with name 'META-INF/services/org.apache.commons.logging.LogFactory' found.");
                }
            } catch (Exception e4) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] A security exception occurred while trying to create an instance of the custom factory class: [" + e4.getMessage().trim() + "]. Trying alternative implementations...");
                }
            }
        }
        if (cachedFactory == null) {
            if (configurationFile != null) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] Looking in properties file for entry with key 'org.apache.commons.logging.LogFactory' to define the LogFactory subclass to use...");
                }
                String property3 = configurationFile.getProperty(FACTORY_PROPERTY);
                if (property3 != null) {
                    if (isDiagnosticsEnabled()) {
                        logDiagnostic("[LOOKUP] Properties file specifies LogFactory subclass '" + property3 + "'");
                    }
                    cachedFactory = newFactory(property3, classLoader, contextClassLoader);
                } else if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] Properties file has no entry specifying LogFactory subclass.");
                }
            } else if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] No properties file available to determine LogFactory subclass from..");
            }
        }
        if (cachedFactory == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] Loading the default LogFactory implementation 'org.apache.commons.logging.impl.LogFactoryImpl' via the same classloader that loaded this LogFactory class (ie not looking in the context classloader).");
            }
            cachedFactory = newFactory(FACTORY_DEFAULT, thisClassLoader, contextClassLoader);
        }
        if (cachedFactory != null) {
            cacheFactory(contextClassLoader, cachedFactory);
            if (configurationFile != null) {
                Enumeration<?> enumerationPropertyNames = configurationFile.propertyNames();
                while (enumerationPropertyNames.hasMoreElements()) {
                    String str = (String) enumerationPropertyNames.nextElement();
                    cachedFactory.setAttribute(str, configurationFile.getProperty(str));
                }
            }
        }
        return cachedFactory;
    }

    public static Log getLog(Class cls) throws LogConfigurationException {
        return getLog(cls.getName());
    }

    public static Log getLog(String str) throws LogConfigurationException {
        return new Jdk14Logger(str);
    }

    public static void release(ClassLoader classLoader) {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Releasing factory for classloader " + objectId(classLoader));
        }
        synchronized (factories) {
            try {
                if (classLoader == null) {
                    if (nullClassLoaderFactory != null) {
                        nullClassLoaderFactory.release();
                        nullClassLoaderFactory = null;
                    }
                } else {
                    LogFactory logFactory = (LogFactory) factories.get(classLoader);
                    if (logFactory != null) {
                        logFactory.release();
                        factories.remove(classLoader);
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public static void releaseAll() {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Releasing factory for all classloaders.");
        }
        synchronized (factories) {
            Enumeration enumerationElements = factories.elements();
            while (enumerationElements.hasMoreElements()) {
                ((LogFactory) enumerationElements.nextElement()).release();
            }
            factories.clear();
            if (nullClassLoaderFactory != null) {
                nullClassLoaderFactory.release();
                nullClassLoaderFactory = null;
            }
        }
    }

    protected static ClassLoader getClassLoader(Class cls) {
        try {
            return cls.getClassLoader();
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Unable to get classloader for class '" + cls + "' due to security restrictions - " + e.getMessage());
            }
            throw e;
        }
    }

    protected static ClassLoader getContextClassLoader() throws LogConfigurationException {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return LogFactory.directGetContextClassLoader();
            }
        });
    }

    protected static ClassLoader directGetContextClassLoader() throws LogConfigurationException {
        ClassLoader classLoader;
        try {
            try {
                try {
                    classLoader = (ClassLoader) Thread.class.getMethod("getContextClassLoader", null).invoke(Thread.currentThread(), null);
                } catch (IllegalAccessException e) {
                    throw new LogConfigurationException("Unexpected IllegalAccessException", e);
                }
            } catch (InvocationTargetException e2) {
                if (!(e2.getTargetException() instanceof SecurityException)) {
                    throw new LogConfigurationException("Unexpected InvocationTargetException", e2.getTargetException());
                }
                classLoader = null;
            }
            return classLoader;
        } catch (NoSuchMethodException e3) {
            return getClassLoader(LogFactory.class);
        }
    }

    private static LogFactory getCachedFactory(ClassLoader classLoader) {
        if (classLoader == null) {
            return nullClassLoaderFactory;
        }
        return (LogFactory) factories.get(classLoader);
    }

    private static void cacheFactory(ClassLoader classLoader, LogFactory logFactory) {
        if (logFactory != null) {
            if (classLoader == null) {
                nullClassLoaderFactory = logFactory;
            } else {
                factories.put(classLoader, logFactory);
            }
        }
    }

    protected static LogFactory newFactory(final String str, final ClassLoader classLoader, ClassLoader classLoader2) throws LogConfigurationException {
        ?? DoPrivileged = AccessController.doPrivileged((PrivilegedAction<??>) new PrivilegedAction() {
            @Override
            public Object run() {
                return LogFactory.createFactory(str, classLoader);
            }
        });
        if (DoPrivileged instanceof LogConfigurationException) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("An error occurred while loading the factory class:" + DoPrivileged.getMessage());
                throw DoPrivileged;
            }
            throw DoPrivileged;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Created object " + objectId(DoPrivileged) + " to manage classloader " + objectId(classLoader2));
        }
        return (LogFactory) DoPrivileged;
    }

    protected static LogFactory newFactory(String str, ClassLoader classLoader) {
        return newFactory(str, classLoader, null);
    }

    protected static Object createFactory(String str, ClassLoader classLoader) {
        Class<?> cls;
        Class<?> clsLoadClass;
        NoClassDefFoundError e;
        ClassNotFoundException e2;
        String str2;
        Class<?> cls2;
        Class<?> cls3 = null;
        try {
            try {
                if (classLoader != null) {
                    try {
                        clsLoadClass = classLoader.loadClass(str);
                        try {
                            if (LogFactory.class.isAssignableFrom(clsLoadClass)) {
                                if (isDiagnosticsEnabled()) {
                                    logDiagnostic("Loaded class " + clsLoadClass.getName() + " from classloader " + objectId(classLoader));
                                }
                            } else if (isDiagnosticsEnabled()) {
                                logDiagnostic("Factory class " + clsLoadClass.getName() + " loaded from classloader " + objectId(clsLoadClass.getClassLoader()) + " does not extend '" + LogFactory.class.getName() + "' as loaded by this classloader.");
                                logHierarchy("[BAD CL TREE] ", classLoader);
                            }
                            return (LogFactory) clsLoadClass.newInstance();
                        } catch (ClassCastException e3) {
                            cls3 = clsLoadClass;
                            if (classLoader == thisClassLoader) {
                                boolean zImplementsLogFactory = implementsLogFactory(cls3);
                                String str3 = "The application has specified that a custom LogFactory implementation should be used but Class '" + str + "' cannot be converted to '" + LogFactory.class.getName() + "'. ";
                                if (zImplementsLogFactory) {
                                    str2 = str3 + "The conflict is caused by the presence of multiple LogFactory classes in incompatible classloaders. Background can be found in http://jakarta.apache.org/commons/logging/tech.html. If you have not explicitly specified a custom LogFactory then it is likely that the container has set one without your knowledge. In this case, consider using the commons-logging-adapters.jar file or specifying the standard LogFactory from the command line. ";
                                } else {
                                    str2 = str3 + "Please check the custom implementation. ";
                                }
                                String str4 = str2 + "Help can be found @http://jakarta.apache.org/commons/logging/troubleshooting.html.";
                                if (isDiagnosticsEnabled()) {
                                    logDiagnostic(str4);
                                }
                                throw new ClassCastException(str4);
                            }
                            if (isDiagnosticsEnabled()) {
                            }
                            cls2 = Class.forName(str);
                            return (LogFactory) cls2.newInstance();
                        } catch (ClassNotFoundException e4) {
                            e2 = e4;
                            if (classLoader == thisClassLoader) {
                                if (isDiagnosticsEnabled()) {
                                    logDiagnostic("Unable to locate any class called '" + str + "' via classloader " + objectId(classLoader));
                                }
                                throw e2;
                            }
                            if (isDiagnosticsEnabled()) {
                            }
                            cls2 = Class.forName(str);
                            return (LogFactory) cls2.newInstance();
                        } catch (NoClassDefFoundError e5) {
                            e = e5;
                            if (classLoader == thisClassLoader) {
                                if (isDiagnosticsEnabled()) {
                                    logDiagnostic("Class '" + str + "' cannot be loaded via classloader " + objectId(classLoader) + " - it depends on some other class that cannot be found.");
                                }
                                throw e;
                            }
                            if (isDiagnosticsEnabled()) {
                            }
                            cls2 = Class.forName(str);
                            return (LogFactory) cls2.newInstance();
                        }
                    } catch (ClassCastException e6) {
                    } catch (ClassNotFoundException e7) {
                        clsLoadClass = null;
                        e2 = e7;
                    } catch (NoClassDefFoundError e8) {
                        clsLoadClass = null;
                        e = e8;
                    }
                }
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("Unable to load factory class via classloader " + objectId(classLoader) + " - trying the classloader associated with this LogFactory.");
                }
                cls2 = Class.forName(str);
                try {
                    return (LogFactory) cls2.newInstance();
                } catch (Exception e9) {
                    cls = cls2;
                    e = e9;
                    if (isDiagnosticsEnabled()) {
                        logDiagnostic("Unable to create LogFactory instance.");
                    }
                    if (cls != null && !LogFactory.class.isAssignableFrom(cls)) {
                        return new LogConfigurationException("The chosen LogFactory implementation does not extend LogFactory. Please check your configuration.", e);
                    }
                    return new LogConfigurationException(e);
                }
            } catch (Exception e10) {
                e = e10;
            }
        } catch (Exception e11) {
            e = e11;
            cls = null;
        }
    }

    private static boolean implementsLogFactory(Class cls) {
        boolean z = false;
        if (cls != null) {
            try {
                ClassLoader classLoader = cls.getClassLoader();
                if (classLoader == null) {
                    logDiagnostic("[CUSTOM LOG FACTORY] was loaded by the boot classloader");
                } else {
                    logHierarchy("[CUSTOM LOG FACTORY] ", classLoader);
                    boolean zIsAssignableFrom = Class.forName(FACTORY_PROPERTY, false, classLoader).isAssignableFrom(cls);
                    try {
                        if (zIsAssignableFrom) {
                            logDiagnostic("[CUSTOM LOG FACTORY] " + cls.getName() + " implements LogFactory but was loaded by an incompatible classloader.");
                        } else {
                            logDiagnostic("[CUSTOM LOG FACTORY] " + cls.getName() + " does not implement LogFactory.");
                        }
                        z = zIsAssignableFrom;
                    } catch (ClassNotFoundException e) {
                        z = zIsAssignableFrom;
                        logDiagnostic("[CUSTOM LOG FACTORY] LogFactory class cannot be loaded by classloader which loaded the custom LogFactory implementation. Is the custom factory in the right classloader?");
                    } catch (LinkageError e2) {
                        e = e2;
                        z = zIsAssignableFrom;
                        logDiagnostic("[CUSTOM LOG FACTORY] LinkageError thrown whilst trying to determine whether the compatibility was caused by a classloader conflict: " + e.getMessage());
                    } catch (SecurityException e3) {
                        e = e3;
                        z = zIsAssignableFrom;
                        logDiagnostic("[CUSTOM LOG FACTORY] SecurityException thrown whilst trying to determine whether the compatibility was caused by a classloader conflict: " + e.getMessage());
                    }
                }
            } catch (ClassNotFoundException e4) {
            } catch (LinkageError e5) {
                e = e5;
            } catch (SecurityException e6) {
                e = e6;
            }
        }
        return z;
    }

    private static InputStream getResourceAsStream(final ClassLoader classLoader, final String str) {
        return (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                if (classLoader != null) {
                    return classLoader.getResourceAsStream(str);
                }
                return ClassLoader.getSystemResourceAsStream(str);
            }
        });
    }

    private static Enumeration getResources(final ClassLoader classLoader, final String str) {
        return (Enumeration) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    if (classLoader != null) {
                        return classLoader.getResources(str);
                    }
                    return ClassLoader.getSystemResources(str);
                } catch (IOException e) {
                    if (LogFactory.isDiagnosticsEnabled()) {
                        LogFactory.logDiagnostic("Exception while trying to find configuration file " + str + ":" + e.getMessage());
                    }
                    return null;
                } catch (NoSuchMethodError e2) {
                    return null;
                }
            }
        });
    }

    private static Properties getProperties(final URL url) {
        return (Properties) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    InputStream inputStreamOpenStream = url.openStream();
                    if (inputStreamOpenStream != null) {
                        Properties properties = new Properties();
                        properties.load(inputStreamOpenStream);
                        inputStreamOpenStream.close();
                        return properties;
                    }
                    return null;
                } catch (IOException e) {
                    if (LogFactory.isDiagnosticsEnabled()) {
                        LogFactory.logDiagnostic("Unable to read URL " + url);
                        return null;
                    }
                    return null;
                }
            }
        });
    }

    private static final Properties getConfigurationFile(ClassLoader classLoader, String str) {
        URL url;
        Enumeration resources;
        double d;
        double d2;
        Properties properties = null;
        try {
            resources = getResources(classLoader, str);
        } catch (SecurityException e) {
            url = null;
        }
        if (resources == null) {
            return null;
        }
        url = null;
        double d3 = 0.0d;
        while (resources.hasMoreElements()) {
            try {
                URL url2 = (URL) resources.nextElement();
                Properties properties2 = getProperties(url2);
                if (properties2 != null) {
                    if (properties == null) {
                        try {
                            String property = properties2.getProperty(PRIORITY_KEY);
                            if (property != null) {
                                d2 = Double.parseDouble(property);
                            } else {
                                d2 = 0.0d;
                            }
                            if (isDiagnosticsEnabled()) {
                                logDiagnostic("[LOOKUP] Properties file found at '" + url2 + "' with priority " + d2);
                            }
                            d3 = d2;
                            url = url2;
                            properties = properties2;
                        } catch (SecurityException e2) {
                            url = url2;
                            properties = properties2;
                            if (isDiagnosticsEnabled()) {
                                logDiagnostic("SecurityException thrown while trying to find/read config files.");
                            }
                            if (isDiagnosticsEnabled()) {
                            }
                            return properties;
                        }
                    } else {
                        String property2 = properties2.getProperty(PRIORITY_KEY);
                        if (property2 != null) {
                            d = Double.parseDouble(property2);
                        } else {
                            d = 0.0d;
                        }
                        if (d > d3) {
                            if (isDiagnosticsEnabled()) {
                                logDiagnostic("[LOOKUP] Properties file at '" + url2 + "' with priority " + d + " overrides file at '" + url + "' with priority " + d3);
                            }
                            url = url2;
                            properties = properties2;
                            d3 = d;
                        } else if (isDiagnosticsEnabled()) {
                            logDiagnostic("[LOOKUP] Properties file at '" + url2 + "' with priority " + d + " does not override file at '" + url + "' with priority " + d3);
                        }
                    }
                }
            } catch (SecurityException e3) {
            }
        }
        if (isDiagnosticsEnabled()) {
            if (properties == null) {
                logDiagnostic("[LOOKUP] No properties file of name '" + str + "' found.");
            } else {
                logDiagnostic("[LOOKUP] Properties file of name '" + str + "' found at '" + url + '\"');
            }
        }
        return properties;
    }

    private static void initDiagnostics() {
        String strObjectId;
        try {
            String property = System.getProperty(DIAGNOSTICS_DEST_PROPERTY);
            if (property == null) {
                return;
            }
            if (property.equals("STDOUT")) {
                diagnosticsStream = System.out;
            } else if (property.equals("STDERR")) {
                diagnosticsStream = System.err;
            } else {
                try {
                    diagnosticsStream = new PrintStream(new FileOutputStream(property, true));
                } catch (IOException e) {
                    return;
                }
            }
            try {
                ClassLoader classLoader = thisClassLoader;
                if (thisClassLoader == null) {
                    strObjectId = "BOOTLOADER";
                } else {
                    strObjectId = objectId(classLoader);
                }
            } catch (SecurityException e2) {
                strObjectId = "UNKNOWN";
            }
            diagnosticPrefix = "[LogFactory from " + strObjectId + "] ";
        } catch (SecurityException e3) {
        }
    }

    protected static boolean isDiagnosticsEnabled() {
        return diagnosticsStream != null;
    }

    private static final void logDiagnostic(String str) {
        if (diagnosticsStream != null) {
            diagnosticsStream.print(diagnosticPrefix);
            diagnosticsStream.println(str);
            diagnosticsStream.flush();
        }
    }

    protected static final void logRawDiagnostic(String str) {
        if (diagnosticsStream != null) {
            diagnosticsStream.println(str);
            diagnosticsStream.flush();
        }
    }

    private static void logClassLoaderEnvironment(Class cls) {
        if (!isDiagnosticsEnabled()) {
            return;
        }
        try {
            logDiagnostic("[ENV] Extension directories (java.ext.dir): " + System.getProperty("java.ext.dir"));
            logDiagnostic("[ENV] Application classpath (java.class.path): " + System.getProperty("java.class.path"));
        } catch (SecurityException e) {
            logDiagnostic("[ENV] Security setting prevent interrogation of system classpaths.");
        }
        String name = cls.getName();
        try {
            ClassLoader classLoader = getClassLoader(cls);
            logDiagnostic("[ENV] Class " + name + " was loaded via classloader " + objectId(classLoader));
            StringBuilder sb = new StringBuilder();
            sb.append("[ENV] Ancestry of classloader which loaded ");
            sb.append(name);
            sb.append(" is ");
            logHierarchy(sb.toString(), classLoader);
        } catch (SecurityException e2) {
            logDiagnostic("[ENV] Security forbids determining the classloader for " + name);
        }
    }

    private static void logHierarchy(String str, ClassLoader classLoader) {
        if (!isDiagnosticsEnabled()) {
            return;
        }
        if (classLoader != null) {
            logDiagnostic(str + objectId(classLoader) + " == '" + classLoader.toString() + "'");
        }
        try {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if (classLoader != null) {
                StringBuffer stringBuffer = new StringBuffer(str + "ClassLoader tree:");
                do {
                    stringBuffer.append(objectId(classLoader));
                    if (classLoader == systemClassLoader) {
                        stringBuffer.append(" (SYSTEM) ");
                    }
                    try {
                        classLoader = classLoader.getParent();
                        stringBuffer.append(" --> ");
                    } catch (SecurityException e) {
                        stringBuffer.append(" --> SECRET");
                    }
                } while (classLoader != null);
                stringBuffer.append("BOOT");
                logDiagnostic(stringBuffer.toString());
            }
        } catch (SecurityException e2) {
            logDiagnostic(str + "Security forbids determining the system classloader.");
        }
    }

    public static String objectId(Object obj) {
        if (obj == null) {
            return "null";
        }
        return obj.getClass().getName() + "@" + System.identityHashCode(obj);
    }
}
