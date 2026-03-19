package org.apache.xalan.extensions;

import org.apache.xalan.templates.Constants;

class ObjectFactory {
    ObjectFactory() {
    }

    static ClassLoader findClassLoader() throws ConfigurationError {
        return Thread.currentThread().getContextClassLoader();
    }

    static Class findProviderClass(String str, ClassLoader classLoader, boolean z) throws ConfigurationError, ClassNotFoundException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            try {
                int iLastIndexOf = str.lastIndexOf(Constants.ATTRVAL_THIS);
                securityManager.checkPackageAccess(iLastIndexOf != -1 ? str.substring(0, iLastIndexOf) : str);
            } catch (SecurityException e) {
                throw e;
            }
        }
        if (classLoader == null) {
            return Class.forName(str);
        }
        try {
            return classLoader.loadClass(str);
        } catch (ClassNotFoundException e2) {
            if (z) {
                ClassLoader classLoader2 = ObjectFactory.class.getClassLoader();
                if (classLoader2 == null) {
                    return Class.forName(str);
                }
                if (classLoader != classLoader2) {
                    return classLoader2.loadClass(str);
                }
                throw e2;
            }
            throw e2;
        }
    }

    static class ConfigurationError extends Error {
        static final long serialVersionUID = 8564305128443551853L;
        private Exception exception;

        ConfigurationError(String str, Exception exc) {
            super(str);
            this.exception = exc;
        }

        Exception getException() {
            return this.exception;
        }
    }
}
