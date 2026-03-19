package mf.org.apache.xml.serialize;

import java.util.Properties;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

final class ObjectFactory {
    private static final boolean DEBUG = isDebugEnabled();
    private static Properties fXercesProperties = null;
    private static long fLastModified = -1;

    ObjectFactory() {
    }

    private static boolean isDebugEnabled() {
        try {
            String val = SecuritySupport.getSystemProperty("xerces.debug");
            if (val != null) {
                return !SchemaSymbols.ATTVAL_FALSE.equals(val);
            }
            return false;
        } catch (SecurityException e) {
            return false;
        }
    }

    private static void debugPrintln(String msg) {
        if (DEBUG) {
            System.err.println("XERCES: " + msg);
        }
    }

    static Object newInstance(String className, ClassLoader cl, boolean doFallback) throws ConfigurationError {
        try {
            Class providerClass = findProviderClass(className, cl, doFallback);
            Object instance = providerClass.newInstance();
            if (DEBUG) {
                debugPrintln("created new instance of " + providerClass + " using ClassLoader: " + cl);
            }
            return instance;
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError("Provider " + className + " not found", x);
        } catch (Exception x2) {
            throw new ConfigurationError("Provider " + className + " could not be instantiated: " + x2, x2);
        }
    }

    static Class findProviderClass(String className, ClassLoader cl, boolean doFallback) throws ConfigurationError, ClassNotFoundException {
        Class<?> clsLoadClass;
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            int lastDot = className.lastIndexOf(".");
            String packageName = className;
            if (lastDot != -1) {
                packageName = className.substring(0, lastDot);
            }
            security.checkPackageAccess(packageName);
        }
        if (cl == null) {
            return Class.forName(className);
        }
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException x) {
            if (doFallback) {
                ClassLoader current = ObjectFactory.class.getClassLoader();
                if (current == null) {
                    clsLoadClass = Class.forName(className);
                } else if (cl != current) {
                    clsLoadClass = current.loadClass(className);
                } else {
                    throw x;
                }
                return clsLoadClass;
            }
            throw x;
        }
    }

    static final class ConfigurationError extends Error {
        static final long serialVersionUID = 937647395548533254L;
        private Exception exception;

        ConfigurationError(String msg, Exception x) {
            super(msg);
            this.exception = x;
        }
    }
}
