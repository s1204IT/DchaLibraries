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
        if (!isDiagnosticsEnabled()) {
            return;
        }
        logDiagnostic("Instance created.");
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    @Override
    public String[] getAttributeNames() {
        Vector names = new Vector();
        Enumeration keys = this.attributes.keys();
        while (keys.hasMoreElements()) {
            names.addElement((String) keys.nextElement());
        }
        String[] results = new String[names.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = (String) names.elementAt(i);
        }
        return results;
    }

    @Override
    public Log getInstance(Class clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }

    @Override
    public Log getInstance(String name) throws LogConfigurationException {
        Log instance = (Log) this.instances.get(name);
        if (instance == null) {
            Log instance2 = newInstance(name);
            this.instances.put(name, instance2);
            return instance2;
        }
        return instance;
    }

    @Override
    public void release() {
        logDiagnostic("Releasing all known loggers");
        this.instances.clear();
    }

    @Override
    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (this.logConstructor != null) {
            logDiagnostic("setAttribute: call too late; configuration already performed.");
        }
        if (value == null) {
            this.attributes.remove(name);
        } else {
            this.attributes.put(name, value);
        }
        if (!name.equals(LogFactory.TCCL_KEY)) {
            return;
        }
        this.useTCCL = Boolean.valueOf(value.toString()).booleanValue();
    }

    protected static ClassLoader getContextClassLoader() throws LogConfigurationException {
        return LogFactory.getContextClassLoader();
    }

    protected static boolean isDiagnosticsEnabled() {
        return LogFactory.isDiagnosticsEnabled();
    }

    protected static ClassLoader getClassLoader(Class clazz) {
        return LogFactory.getClassLoader(clazz);
    }

    private void initDiagnostics() {
        String classLoaderName;
        ClassLoader classLoader = getClassLoader(getClass());
        if (classLoader == null) {
            classLoaderName = "BOOTLOADER";
        } else {
            try {
                classLoaderName = objectId(classLoader);
            } catch (SecurityException e) {
                classLoaderName = "UNKNOWN";
            }
        }
        this.diagnosticPrefix = "[LogFactoryImpl@" + System.identityHashCode(this) + " from " + classLoaderName + "] ";
    }

    protected void logDiagnostic(String msg) {
        if (!isDiagnosticsEnabled()) {
            return;
        }
        logRawDiagnostic(this.diagnosticPrefix + msg);
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

    protected Log newInstance(String name) throws LogConfigurationException {
        Log instance;
        try {
            if (this.logConstructor == null) {
                instance = discoverLogImplementation(name);
            } else {
                Object[] params = {name};
                instance = (Log) this.logConstructor.newInstance(params);
            }
            if (this.logMethod != null) {
                Object[] params2 = {this};
                this.logMethod.invoke(instance, params2);
            }
            return instance;
        } catch (InvocationTargetException e) {
            Throwable c = e.getTargetException();
            if (c != null) {
                throw new LogConfigurationException(c);
            }
            throw new LogConfigurationException(e);
        } catch (LogConfigurationException lce) {
            throw lce;
        } catch (Throwable t) {
            throw new LogConfigurationException(t);
        }
    }

    private boolean isLogLibraryAvailable(String name, String classname) {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Checking for '" + name + "'.");
        }
        try {
            Log log = createLogFromClass(classname, getClass().getName(), false);
            if (log == null) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("Did not find '" + name + "'.");
                }
                return false;
            }
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Found '" + name + "'.");
                return true;
            }
            return true;
        } catch (LogConfigurationException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Logging system '" + name + "' is available but not useable.");
            }
            return false;
        }
    }

    private String getConfigurationValue(String property) {
        String value;
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[ENV] Trying to get configuration for item " + property);
        }
        Object valueObj = getAttribute(property);
        if (valueObj != null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[ENV] Found LogFactory attribute [" + valueObj + "] for " + property);
            }
            return valueObj.toString();
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[ENV] No LogFactory attribute found for " + property);
        }
        try {
            value = System.getProperty(property);
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[ENV] Security prevented reading system property " + property);
            }
        }
        if (value != null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[ENV] Found system property [" + value + "] for " + property);
            }
            return value;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[ENV] No system property found for property " + property);
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[ENV] No configuration defined for item " + property);
        }
        return null;
    }

    private boolean getBooleanConfiguration(String key, boolean dflt) {
        String val = getConfigurationValue(key);
        if (val == null) {
            return dflt;
        }
        return Boolean.valueOf(val).booleanValue();
    }

    private void initConfiguration() {
        this.allowFlawedContext = getBooleanConfiguration(ALLOW_FLAWED_CONTEXT_PROPERTY, true);
        this.allowFlawedDiscovery = getBooleanConfiguration(ALLOW_FLAWED_DISCOVERY_PROPERTY, true);
        this.allowFlawedHierarchy = getBooleanConfiguration(ALLOW_FLAWED_HIERARCHY_PROPERTY, true);
    }

    private Log discoverLogImplementation(String logCategory) throws LogConfigurationException {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Discovering a Log implementation...");
        }
        initConfiguration();
        Log result = null;
        String specifiedLogClassName = findUserSpecifiedLogClassName();
        if (specifiedLogClassName != null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Attempting to load user-specified log class '" + specifiedLogClassName + "'...");
            }
            Log result2 = createLogFromClass(specifiedLogClassName, logCategory, true);
            if (result2 == null) {
                StringBuffer messageBuffer = new StringBuffer("User-specified log class '");
                messageBuffer.append(specifiedLogClassName);
                messageBuffer.append("' cannot be found or is not useable.");
                if (specifiedLogClassName != null) {
                    informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_LOG4J_LOGGER);
                    informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_JDK14_LOGGER);
                    informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_LUMBERJACK_LOGGER);
                    informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_SIMPLE_LOGGER);
                }
                throw new LogConfigurationException(messageBuffer.toString());
            }
            return result2;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("No user-specified Log implementation; performing discovery using the standard supported logging implementations...");
        }
        for (int i = 0; i < classesToDiscover.length && result == null; i++) {
            result = createLogFromClass(classesToDiscover[i], logCategory, true);
        }
        if (result == null) {
            throw new LogConfigurationException("No suitable Log implementation");
        }
        return result;
    }

    private void informUponSimilarName(StringBuffer messageBuffer, String name, String candidate) {
        if (name.equals(candidate) || !name.regionMatches(true, 0, candidate, 0, PKG_LEN + 5)) {
            return;
        }
        messageBuffer.append(" Did you mean '");
        messageBuffer.append(candidate);
        messageBuffer.append("'?");
    }

    private String findUserSpecifiedLogClassName() {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Trying to get log class from attribute 'org.apache.commons.logging.Log'");
        }
        String specifiedClass = (String) getAttribute(LOG_PROPERTY);
        if (specifiedClass == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from attribute 'org.apache.commons.logging.log'");
            }
            specifiedClass = (String) getAttribute(LOG_PROPERTY_OLD);
        }
        if (specifiedClass == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from system property 'org.apache.commons.logging.Log'");
            }
            try {
                specifiedClass = System.getProperty(LOG_PROPERTY);
            } catch (SecurityException e) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("No access allowed to system property 'org.apache.commons.logging.Log' - " + e.getMessage());
                }
            }
        }
        if (specifiedClass == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from system property 'org.apache.commons.logging.log'");
            }
            try {
                specifiedClass = System.getProperty(LOG_PROPERTY_OLD);
            } catch (SecurityException e2) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("No access allowed to system property 'org.apache.commons.logging.log' - " + e2.getMessage());
                }
            }
        }
        if (specifiedClass != null) {
            return specifiedClass.trim();
        }
        return specifiedClass;
    }

    private Log createLogFromClass(String logAdapterClassName, String logCategory, boolean affectState) throws LogConfigurationException {
        Class<?> cls;
        Object o;
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Attempting to instantiate '" + logAdapterClassName + "'");
        }
        Object[] params = {logCategory};
        Log logAdapter = null;
        Constructor<?> constructor = null;
        Class<?> cls2 = null;
        ClassLoader currentCL = getBaseClassLoader();
        while (true) {
            logDiagnostic("Trying to load '" + logAdapterClassName + "' from classloader " + objectId(currentCL));
            try {
                if (isDiagnosticsEnabled()) {
                    String resourceName = logAdapterClassName.replace('.', '/') + ".class";
                    URL url = currentCL != null ? currentCL.getResource(resourceName) : ClassLoader.getSystemResource(resourceName + ".class");
                    if (url == null) {
                        logDiagnostic("Class '" + logAdapterClassName + "' [" + resourceName + "] cannot be found.");
                    } else {
                        logDiagnostic("Class '" + logAdapterClassName + "' was found at '" + url + "'");
                    }
                }
                try {
                    cls = Class.forName(logAdapterClassName, true, currentCL);
                } catch (ClassNotFoundException originalClassNotFoundException) {
                    String msg = "" + originalClassNotFoundException.getMessage();
                    logDiagnostic("The log adapter '" + logAdapterClassName + "' is not available via classloader " + objectId(currentCL) + ": " + msg.trim());
                    try {
                        cls = Class.forName(logAdapterClassName);
                    } catch (ClassNotFoundException secondaryClassNotFoundException) {
                        String msg2 = "" + secondaryClassNotFoundException.getMessage();
                        logDiagnostic("The log adapter '" + logAdapterClassName + "' is not available via the LogFactoryImpl class classloader: " + msg2.trim());
                    }
                }
                constructor = cls.getConstructor(this.logConstructorSignature);
                o = constructor.newInstance(params);
            } catch (ExceptionInInitializerError e) {
                String msg3 = "" + e.getMessage();
                logDiagnostic("The log adapter '" + logAdapterClassName + "' is unable to initialize itself when loaded via classloader " + objectId(currentCL) + ": " + msg3.trim());
            } catch (NoClassDefFoundError e2) {
                String msg4 = "" + e2.getMessage();
                logDiagnostic("The log adapter '" + logAdapterClassName + "' is missing dependencies when loaded via classloader " + objectId(currentCL) + ": " + msg4.trim());
            } catch (LogConfigurationException e3) {
                throw e3;
            } catch (Throwable t) {
                handleFlawedDiscovery(logAdapterClassName, currentCL, t);
            }
            if (!(o instanceof Log)) {
                handleFlawedHierarchy(currentCL, cls);
                if (currentCL == null) {
                    break;
                }
                currentCL = currentCL.getParent();
            } else {
                cls2 = cls;
                logAdapter = (Log) o;
                break;
            }
        }
        if (logAdapter != null && affectState) {
            this.logClassName = logAdapterClassName;
            this.logConstructor = constructor;
            try {
                this.logMethod = cls2.getMethod("setLogFactory", this.logMethodSignature);
                logDiagnostic("Found method setLogFactory(LogFactory) in '" + logAdapterClassName + "'");
            } catch (Throwable th) {
                this.logMethod = null;
                logDiagnostic("[INFO] '" + logAdapterClassName + "' from classloader " + objectId(currentCL) + " does not declare optional method setLogFactory(LogFactory)");
            }
            logDiagnostic("Log adapter '" + logAdapterClassName + "' from classloader " + objectId(cls2.getClassLoader()) + " has been selected for use.");
        }
        return logAdapter;
    }

    private ClassLoader getBaseClassLoader() throws LogConfigurationException {
        ClassLoader thisClassLoader = getClassLoader(LogFactoryImpl.class);
        if (!this.useTCCL) {
            return thisClassLoader;
        }
        ClassLoader contextClassLoader = getContextClassLoader();
        ClassLoader baseClassLoader = getLowestClassLoader(contextClassLoader, thisClassLoader);
        if (baseClassLoader == null) {
            if (this.allowFlawedContext) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[WARNING] the context classloader is not part of a parent-child relationship with the classloader that loaded LogFactoryImpl.");
                }
                return contextClassLoader;
            }
            throw new LogConfigurationException("Bad classloader hierarchy; LogFactoryImpl was loaded via a classloader that is not related to the current context classloader.");
        }
        if (baseClassLoader != contextClassLoader) {
            if (this.allowFlawedContext) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("Warning: the context classloader is an ancestor of the classloader that loaded LogFactoryImpl; it should be the same or a descendant. The application using commons-logging should ensure the context classloader is used correctly.");
                }
            } else {
                throw new LogConfigurationException("Bad classloader hierarchy; LogFactoryImpl was loaded via a classloader that is not related to the current context classloader.");
            }
        }
        return baseClassLoader;
    }

    private ClassLoader getLowestClassLoader(ClassLoader c1, ClassLoader c2) {
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }
        for (ClassLoader current = c1; current != null; current = current.getParent()) {
            if (current == c2) {
                return c1;
            }
        }
        for (ClassLoader current2 = c2; current2 != null; current2 = current2.getParent()) {
            if (current2 == c1) {
                return c2;
            }
        }
        return null;
    }

    private void handleFlawedDiscovery(String logAdapterClassName, ClassLoader classLoader, Throwable discoveryFlaw) {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Could not instantiate Log '" + logAdapterClassName + "' -- " + discoveryFlaw.getClass().getName() + ": " + discoveryFlaw.getLocalizedMessage());
        }
        if (!this.allowFlawedDiscovery) {
            throw new LogConfigurationException(discoveryFlaw);
        }
    }

    private void handleFlawedHierarchy(ClassLoader badClassLoader, Class badClass) throws LogConfigurationException {
        boolean implementsLog = false;
        String logInterfaceName = Log.class.getName();
        Class<?>[] interfaces = badClass.getInterfaces();
        int i = 0;
        while (true) {
            if (i >= interfaces.length) {
                break;
            }
            if (logInterfaceName.equals(interfaces[i].getName())) {
                implementsLog = true;
                break;
            }
            i++;
        }
        if (!implementsLog) {
            if (this.allowFlawedDiscovery) {
                if (isDiagnosticsEnabled()) {
                    StringBuffer msg = new StringBuffer();
                    msg.append("[WARNING] Log class '");
                    msg.append(badClass.getName());
                    msg.append("' does not implement the Log interface.");
                    logDiagnostic(msg.toString());
                    return;
                }
                return;
            }
            StringBuffer msg2 = new StringBuffer();
            msg2.append("Terminating logging for this context. ");
            msg2.append("Log class '");
            msg2.append(badClass.getName());
            msg2.append("' does not implement the Log interface.");
            if (isDiagnosticsEnabled()) {
                logDiagnostic(msg2.toString());
            }
            throw new LogConfigurationException(msg2.toString());
        }
        if (isDiagnosticsEnabled()) {
            try {
                ClassLoader logInterfaceClassLoader = getClassLoader(Log.class);
                logDiagnostic("Class '" + badClass.getName() + "' was found in classloader " + objectId(badClassLoader) + ". It is bound to a Log interface which is not the one loaded from classloader " + objectId(logInterfaceClassLoader));
            } catch (Throwable th) {
                logDiagnostic("Error while trying to output diagnostics about bad class '" + badClass + "'");
            }
        }
        if (this.allowFlawedHierarchy) {
            if (isDiagnosticsEnabled()) {
                StringBuffer msg3 = new StringBuffer();
                msg3.append("Warning: bad log hierarchy. ");
                msg3.append("You have more than one version of '");
                msg3.append(Log.class.getName());
                msg3.append("' visible.");
                logDiagnostic(msg3.toString());
                return;
            }
            return;
        }
        StringBuffer msg4 = new StringBuffer();
        msg4.append("Terminating logging for this context ");
        msg4.append("due to bad log hierarchy. ");
        msg4.append("You have more than one version of '");
        msg4.append(Log.class.getName());
        msg4.append("' visible.");
        if (isDiagnosticsEnabled()) {
            logDiagnostic(msg4.toString());
        }
        throw new LogConfigurationException(msg4.toString());
    }
}
