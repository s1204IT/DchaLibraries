package org.apache.commons.logging.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

@Deprecated
public class LogFactoryImpl extends LogFactory {
    public static final String ALLOW_FLAWED_CONTEXT_PROPERTY = "org.apache.commons.logging.Log.allowFlawedContext";
    public static final String ALLOW_FLAWED_DISCOVERY_PROPERTY = "org.apache.commons.logging.Log.allowFlawedDiscovery";
    public static final String ALLOW_FLAWED_HIERARCHY_PROPERTY = "org.apache.commons.logging.Log.allowFlawedHierarchy";
    public static final String LOG_PROPERTY = "org.apache.commons.logging.Log";
    protected static final String LOG_PROPERTY_OLD = "org.apache.commons.logging.log";
    private boolean allowFlawedContext;
    private boolean allowFlawedDiscovery;
    private boolean allowFlawedHierarchy;
    private String diagnosticPrefix;
    private String logClassName;
    private static final String PKG_IMPL = "org.apache.commons.logging.impl.";
    private static final int PKG_LEN = PKG_IMPL.length();
    private static final String LOGGING_IMPL_LOG4J_LOGGER = "org.apache.commons.logging.impl.Log4JLogger";
    private static final String LOGGING_IMPL_JDK14_LOGGER = "org.apache.commons.logging.impl.Jdk14Logger";
    private static final String LOGGING_IMPL_LUMBERJACK_LOGGER = "org.apache.commons.logging.impl.Jdk13LumberjackLogger";
    private static final String LOGGING_IMPL_SIMPLE_LOGGER = "org.apache.commons.logging.impl.SimpleLog";
    private static final String[] classesToDiscover = {LOGGING_IMPL_LOG4J_LOGGER, LOGGING_IMPL_JDK14_LOGGER, LOGGING_IMPL_LUMBERJACK_LOGGER, LOGGING_IMPL_SIMPLE_LOGGER};
    private boolean useTCCL = true;
    protected Hashtable attributes = new Hashtable();
    protected Hashtable instances = new Hashtable();
    protected Constructor logConstructor = null;
    protected Class[] logConstructorSignature = {String.class};
    protected Method logMethod = null;
    protected Class[] logMethodSignature = {LogFactory.class};

    public LogFactoryImpl() {
        initDiagnostics();
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Instance created.");
        }
    }

    @Override
    public Object getAttribute(String str) {
        return this.attributes.get(str);
    }

    @Override
    public String[] getAttributeNames() {
        Vector vector = new Vector();
        Enumeration enumerationKeys = this.attributes.keys();
        while (enumerationKeys.hasMoreElements()) {
            vector.addElement((String) enumerationKeys.nextElement());
        }
        String[] strArr = new String[vector.size()];
        for (int i = 0; i < strArr.length; i++) {
            strArr[i] = (String) vector.elementAt(i);
        }
        return strArr;
    }

    @Override
    public Log getInstance(Class cls) throws LogConfigurationException {
        return getInstance(cls.getName());
    }

    @Override
    public Log getInstance(String str) throws LogConfigurationException {
        Log log = (Log) this.instances.get(str);
        if (log == null) {
            Log logNewInstance = newInstance(str);
            this.instances.put(str, logNewInstance);
            return logNewInstance;
        }
        return log;
    }

    @Override
    public void release() {
        logDiagnostic("Releasing all known loggers");
        this.instances.clear();
    }

    @Override
    public void removeAttribute(String str) {
        this.attributes.remove(str);
    }

    @Override
    public void setAttribute(String str, Object obj) {
        if (this.logConstructor != null) {
            logDiagnostic("setAttribute: call too late; configuration already performed.");
        }
        if (obj == null) {
            this.attributes.remove(str);
        } else {
            this.attributes.put(str, obj);
        }
        if (str.equals(LogFactory.TCCL_KEY)) {
            this.useTCCL = Boolean.valueOf(obj.toString()).booleanValue();
        }
    }

    protected static ClassLoader getContextClassLoader() throws LogConfigurationException {
        return LogFactory.getContextClassLoader();
    }

    protected static boolean isDiagnosticsEnabled() {
        return LogFactory.isDiagnosticsEnabled();
    }

    protected static ClassLoader getClassLoader(Class cls) {
        return LogFactory.getClassLoader(cls);
    }

    private void initDiagnostics() {
        String strObjectId;
        ClassLoader classLoader = getClassLoader(getClass());
        if (classLoader == null) {
            strObjectId = "BOOTLOADER";
        } else {
            try {
                strObjectId = objectId(classLoader);
            } catch (SecurityException e) {
                strObjectId = "UNKNOWN";
            }
        }
        this.diagnosticPrefix = "[LogFactoryImpl@" + System.identityHashCode(this) + " from " + strObjectId + "] ";
    }

    protected void logDiagnostic(String str) {
        if (isDiagnosticsEnabled()) {
            logRawDiagnostic(this.diagnosticPrefix + str);
        }
    }

    protected String getLogClassName() {
        if (this.logClassName == null) {
            discoverLogImplementation(getClass().getName());
        }
        return this.logClassName;
    }

    protected Constructor getLogConstructor() throws LogConfigurationException {
        if (this.logConstructor == null) {
            discoverLogImplementation(getClass().getName());
        }
        return this.logConstructor;
    }

    protected boolean isJdk13LumberjackAvailable() {
        return isLogLibraryAvailable("Jdk13Lumberjack", LOGGING_IMPL_LUMBERJACK_LOGGER);
    }

    protected boolean isJdk14Available() {
        return isLogLibraryAvailable("Jdk14", LOGGING_IMPL_JDK14_LOGGER);
    }

    protected boolean isLog4JAvailable() {
        return isLogLibraryAvailable("Log4J", LOGGING_IMPL_LOG4J_LOGGER);
    }

    protected Log newInstance(String str) throws LogConfigurationException {
        Log logDiscoverLogImplementation;
        try {
            if (this.logConstructor == null) {
                logDiscoverLogImplementation = discoverLogImplementation(str);
            } else {
                logDiscoverLogImplementation = (Log) this.logConstructor.newInstance(str);
            }
            if (this.logMethod != null) {
                this.logMethod.invoke(logDiscoverLogImplementation, this);
            }
            return logDiscoverLogImplementation;
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException != null) {
                throw new LogConfigurationException(targetException);
            }
            throw new LogConfigurationException(e);
        } catch (LogConfigurationException e2) {
            throw e2;
        } catch (Throwable th) {
            throw new LogConfigurationException(th);
        }
    }

    private boolean isLogLibraryAvailable(String str, String str2) {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Checking for '" + str + "'.");
        }
        try {
            if (createLogFromClass(str2, getClass().getName(), false) == null) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("Did not find '" + str + "'.");
                }
                return false;
            }
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Found '" + str + "'.");
                return true;
            }
            return true;
        } catch (LogConfigurationException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Logging system '" + str + "' is available but not useable.");
            }
            return false;
        }
    }

    private String getConfigurationValue(String str) {
        String property;
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[ENV] Trying to get configuration for item " + str);
        }
        Object attribute = getAttribute(str);
        if (attribute != null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[ENV] Found LogFactory attribute [" + attribute + "] for " + str);
            }
            return attribute.toString();
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[ENV] No LogFactory attribute found for " + str);
        }
        try {
            property = System.getProperty(str);
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[ENV] Security prevented reading system property " + str);
            }
        }
        if (property != null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[ENV] Found system property [" + property + "] for " + str);
            }
            return property;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[ENV] No system property found for property " + str);
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[ENV] No configuration defined for item " + str);
            return null;
        }
        return null;
    }

    private boolean getBooleanConfiguration(String str, boolean z) {
        String configurationValue = getConfigurationValue(str);
        if (configurationValue == null) {
            return z;
        }
        return Boolean.valueOf(configurationValue).booleanValue();
    }

    private void initConfiguration() {
        this.allowFlawedContext = getBooleanConfiguration(ALLOW_FLAWED_CONTEXT_PROPERTY, true);
        this.allowFlawedDiscovery = getBooleanConfiguration(ALLOW_FLAWED_DISCOVERY_PROPERTY, true);
        this.allowFlawedHierarchy = getBooleanConfiguration(ALLOW_FLAWED_HIERARCHY_PROPERTY, true);
    }

    private Log discoverLogImplementation(String str) throws LogConfigurationException {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Discovering a Log implementation...");
        }
        initConfiguration();
        Log logCreateLogFromClass = null;
        String strFindUserSpecifiedLogClassName = findUserSpecifiedLogClassName();
        if (strFindUserSpecifiedLogClassName != null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Attempting to load user-specified log class '" + strFindUserSpecifiedLogClassName + "'...");
            }
            Log logCreateLogFromClass2 = createLogFromClass(strFindUserSpecifiedLogClassName, str, true);
            if (logCreateLogFromClass2 == null) {
                StringBuffer stringBuffer = new StringBuffer("User-specified log class '");
                stringBuffer.append(strFindUserSpecifiedLogClassName);
                stringBuffer.append("' cannot be found or is not useable.");
                if (strFindUserSpecifiedLogClassName != null) {
                    informUponSimilarName(stringBuffer, strFindUserSpecifiedLogClassName, LOGGING_IMPL_LOG4J_LOGGER);
                    informUponSimilarName(stringBuffer, strFindUserSpecifiedLogClassName, LOGGING_IMPL_JDK14_LOGGER);
                    informUponSimilarName(stringBuffer, strFindUserSpecifiedLogClassName, LOGGING_IMPL_LUMBERJACK_LOGGER);
                    informUponSimilarName(stringBuffer, strFindUserSpecifiedLogClassName, LOGGING_IMPL_SIMPLE_LOGGER);
                }
                throw new LogConfigurationException(stringBuffer.toString());
            }
            return logCreateLogFromClass2;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("No user-specified Log implementation; performing discovery using the standard supported logging implementations...");
        }
        for (int i = 0; i < classesToDiscover.length && logCreateLogFromClass == null; i++) {
            logCreateLogFromClass = createLogFromClass(classesToDiscover[i], str, true);
        }
        if (logCreateLogFromClass == null) {
            throw new LogConfigurationException("No suitable Log implementation");
        }
        return logCreateLogFromClass;
    }

    private void informUponSimilarName(StringBuffer stringBuffer, String str, String str2) {
        if (!str.equals(str2) && str.regionMatches(true, 0, str2, 0, PKG_LEN + 5)) {
            stringBuffer.append(" Did you mean '");
            stringBuffer.append(str2);
            stringBuffer.append("'?");
        }
    }

    private String findUserSpecifiedLogClassName() {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Trying to get log class from attribute 'org.apache.commons.logging.Log'");
        }
        String property = (String) getAttribute(LOG_PROPERTY);
        if (property == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from attribute 'org.apache.commons.logging.log'");
            }
            property = (String) getAttribute(LOG_PROPERTY_OLD);
        }
        if (property == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from system property 'org.apache.commons.logging.Log'");
            }
            try {
                property = System.getProperty(LOG_PROPERTY);
            } catch (SecurityException e) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("No access allowed to system property 'org.apache.commons.logging.Log' - " + e.getMessage());
                }
            }
        }
        if (property == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from system property 'org.apache.commons.logging.log'");
            }
            try {
                property = System.getProperty(LOG_PROPERTY_OLD);
            } catch (SecurityException e2) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("No access allowed to system property 'org.apache.commons.logging.log' - " + e2.getMessage());
                }
            }
        }
        if (property != null) {
            return property.trim();
        }
        return property;
    }

    private Log createLogFromClass(String str, String str2, boolean z) throws LogConfigurationException {
        Constructor<?> constructor;
        Log log;
        Class<?> cls;
        Object objNewInstance;
        URL systemResource;
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Attempting to instantiate '" + str + "'");
        }
        Object[] objArr = {str2};
        ClassLoader baseClassLoader = getBaseClassLoader();
        Constructor<?> constructor2 = null;
        Class<?> cls2 = null;
        while (true) {
            logDiagnostic("Trying to load '" + str + "' from classloader " + objectId(baseClassLoader));
            try {
                try {
                    if (isDiagnosticsEnabled()) {
                        String str3 = str.replace('.', '/') + ".class";
                        if (baseClassLoader != null) {
                            systemResource = baseClassLoader.getResource(str3);
                        } else {
                            systemResource = ClassLoader.getSystemResource(str3 + ".class");
                        }
                        if (systemResource == null) {
                            logDiagnostic("Class '" + str + "' [" + str3 + "] cannot be found.");
                        } else {
                            logDiagnostic("Class '" + str + "' was found at '" + systemResource + "'");
                        }
                    }
                    try {
                        cls = Class.forName(str, true, baseClassLoader);
                    } catch (ClassNotFoundException e) {
                        logDiagnostic("The log adapter '" + str + "' is not available via classloader " + objectId(baseClassLoader) + ": " + ("" + e.getMessage()).trim());
                        try {
                            cls = Class.forName(str);
                        } catch (ClassNotFoundException e2) {
                            logDiagnostic("The log adapter '" + str + "' is not available via the LogFactoryImpl class classloader: " + ("" + e2.getMessage()).trim());
                            break;
                        }
                    }
                    constructor = cls.getConstructor(this.logConstructorSignature);
                    try {
                        objNewInstance = constructor.newInstance(objArr);
                    } catch (ExceptionInInitializerError e3) {
                        e = e3;
                    } catch (NoClassDefFoundError e4) {
                        e = e4;
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (LogConfigurationException e5) {
                    throw e5;
                }
            } catch (ExceptionInInitializerError e6) {
                e = e6;
            } catch (NoClassDefFoundError e7) {
                e = e7;
            } catch (Throwable th2) {
                th = th2;
            }
            if (objNewInstance instanceof Log) {
                try {
                    log = (Log) objNewInstance;
                    cls2 = cls;
                    break;
                } catch (ExceptionInInitializerError e8) {
                    e = e8;
                    cls2 = cls;
                    constructor2 = constructor;
                    logDiagnostic("The log adapter '" + str + "' is unable to initialize itself when loaded via classloader " + objectId(baseClassLoader) + ": " + ("" + e.getMessage()).trim());
                    constructor = constructor2;
                    log = null;
                } catch (NoClassDefFoundError e9) {
                    e = e9;
                    cls2 = cls;
                    constructor2 = constructor;
                    logDiagnostic("The log adapter '" + str + "' is missing dependencies when loaded via classloader " + objectId(baseClassLoader) + ": " + ("" + e.getMessage()).trim());
                    constructor = constructor2;
                    log = null;
                } catch (Throwable th3) {
                    cls2 = cls;
                    th = th3;
                    constructor2 = constructor;
                    handleFlawedDiscovery(str, baseClassLoader, th);
                    if (baseClassLoader != null) {
                    }
                }
            } else {
                handleFlawedHierarchy(baseClassLoader, cls);
                constructor2 = constructor;
                if (baseClassLoader != null) {
                    break;
                }
                baseClassLoader = baseClassLoader.getParent();
            }
        }
        constructor = constructor2;
        log = null;
        if (log != null && z) {
            this.logClassName = str;
            this.logConstructor = constructor;
            try {
                this.logMethod = cls2.getMethod("setLogFactory", this.logMethodSignature);
                logDiagnostic("Found method setLogFactory(LogFactory) in '" + str + "'");
            } catch (Throwable th4) {
                this.logMethod = null;
                logDiagnostic("[INFO] '" + str + "' from classloader " + objectId(baseClassLoader) + " does not declare optional method setLogFactory(LogFactory)");
            }
            logDiagnostic("Log adapter '" + str + "' from classloader " + objectId(cls2.getClassLoader()) + " has been selected for use.");
        }
        return log;
    }

    private ClassLoader getBaseClassLoader() throws LogConfigurationException {
        ClassLoader classLoader = getClassLoader(LogFactoryImpl.class);
        if (!this.useTCCL) {
            return classLoader;
        }
        ClassLoader contextClassLoader = getContextClassLoader();
        ClassLoader lowestClassLoader = getLowestClassLoader(contextClassLoader, classLoader);
        if (lowestClassLoader == null) {
            if (this.allowFlawedContext) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[WARNING] the context classloader is not part of a parent-child relationship with the classloader that loaded LogFactoryImpl.");
                }
                return contextClassLoader;
            }
            throw new LogConfigurationException("Bad classloader hierarchy; LogFactoryImpl was loaded via a classloader that is not related to the current context classloader.");
        }
        if (lowestClassLoader != contextClassLoader) {
            if (this.allowFlawedContext) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("Warning: the context classloader is an ancestor of the classloader that loaded LogFactoryImpl; it should be the same or a descendant. The application using commons-logging should ensure the context classloader is used correctly.");
                }
            } else {
                throw new LogConfigurationException("Bad classloader hierarchy; LogFactoryImpl was loaded via a classloader that is not related to the current context classloader.");
            }
        }
        return lowestClassLoader;
    }

    private ClassLoader getLowestClassLoader(ClassLoader classLoader, ClassLoader classLoader2) {
        if (classLoader == null) {
            return classLoader2;
        }
        if (classLoader2 == null) {
            return classLoader;
        }
        for (ClassLoader parent = classLoader; parent != null; parent = parent.getParent()) {
            if (parent == classLoader2) {
                return classLoader;
            }
        }
        for (ClassLoader parent2 = classLoader2; parent2 != null; parent2 = parent2.getParent()) {
            if (parent2 == classLoader) {
                return classLoader2;
            }
        }
        return null;
    }

    private void handleFlawedDiscovery(String str, ClassLoader classLoader, Throwable th) {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Could not instantiate Log '" + str + "' -- " + th.getClass().getName() + ": " + th.getLocalizedMessage());
        }
        if (!this.allowFlawedDiscovery) {
            throw new LogConfigurationException(th);
        }
    }

    private void handleFlawedHierarchy(ClassLoader classLoader, Class cls) throws LogConfigurationException {
        String name = Log.class.getName();
        Class<?>[] interfaces = cls.getInterfaces();
        boolean z = false;
        int i = 0;
        while (true) {
            if (i >= interfaces.length) {
                break;
            }
            if (!name.equals(interfaces[i].getName())) {
                i++;
            } else {
                z = true;
                break;
            }
        }
        if (z) {
            if (isDiagnosticsEnabled()) {
                try {
                    logDiagnostic("Class '" + cls.getName() + "' was found in classloader " + objectId(classLoader) + ". It is bound to a Log interface which is not the one loaded from classloader " + objectId(getClassLoader(Log.class)));
                } catch (Throwable th) {
                    logDiagnostic("Error while trying to output diagnostics about bad class '" + cls + "'");
                }
            }
            if (!this.allowFlawedHierarchy) {
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("Terminating logging for this context ");
                stringBuffer.append("due to bad log hierarchy. ");
                stringBuffer.append("You have more than one version of '");
                stringBuffer.append(Log.class.getName());
                stringBuffer.append("' visible.");
                if (isDiagnosticsEnabled()) {
                    logDiagnostic(stringBuffer.toString());
                }
                throw new LogConfigurationException(stringBuffer.toString());
            }
            if (isDiagnosticsEnabled()) {
                StringBuffer stringBuffer2 = new StringBuffer();
                stringBuffer2.append("Warning: bad log hierarchy. ");
                stringBuffer2.append("You have more than one version of '");
                stringBuffer2.append(Log.class.getName());
                stringBuffer2.append("' visible.");
                logDiagnostic(stringBuffer2.toString());
                return;
            }
            return;
        }
        if (!this.allowFlawedDiscovery) {
            StringBuffer stringBuffer3 = new StringBuffer();
            stringBuffer3.append("Terminating logging for this context. ");
            stringBuffer3.append("Log class '");
            stringBuffer3.append(cls.getName());
            stringBuffer3.append("' does not implement the Log interface.");
            if (isDiagnosticsEnabled()) {
                logDiagnostic(stringBuffer3.toString());
            }
            throw new LogConfigurationException(stringBuffer3.toString());
        }
        if (isDiagnosticsEnabled()) {
            StringBuffer stringBuffer4 = new StringBuffer();
            stringBuffer4.append("[WARNING] Log class '");
            stringBuffer4.append(cls.getName());
            stringBuffer4.append("' does not implement the Log interface.");
            logDiagnostic(stringBuffer4.toString());
        }
    }
}
