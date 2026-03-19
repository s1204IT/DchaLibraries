package org.apache.xalan.xslt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

class SecuritySupport12 extends SecuritySupport {
    SecuritySupport12() {
    }

    @Override
    ClassLoader getContextClassLoader() {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    return Thread.currentThread().getContextClassLoader();
                } catch (SecurityException e) {
                    return null;
                }
            }
        });
    }

    @Override
    ClassLoader getSystemClassLoader() {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    return ClassLoader.getSystemClassLoader();
                } catch (SecurityException e) {
                    return null;
                }
            }
        });
    }

    @Override
    ClassLoader getParentClassLoader(final ClassLoader classLoader) {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                ClassLoader parent;
                try {
                    parent = classLoader.getParent();
                } catch (SecurityException e) {
                    parent = null;
                }
                if (parent == classLoader) {
                    return null;
                }
                return parent;
            }
        });
    }

    @Override
    String getSystemProperty(final String str) {
        return (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(str);
            }
        });
    }

    @Override
    FileInputStream getFileInputStream(final File file) throws FileNotFoundException {
        try {
            return (FileInputStream) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                @Override
                public Object run() throws FileNotFoundException {
                    return new FileInputStream(file);
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((FileNotFoundException) e.getException());
        }
    }

    @Override
    InputStream getResourceAsStream(final ClassLoader classLoader, final String str) {
        return (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                if (classLoader == null) {
                    return ClassLoader.getSystemResourceAsStream(str);
                }
                return classLoader.getResourceAsStream(str);
            }
        });
    }

    @Override
    boolean getFileExists(final File file) {
        return ((Boolean) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return new Boolean(file.exists());
            }
        })).booleanValue();
    }

    @Override
    long getLastModified(final File file) {
        return ((Long) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return new Long(file.lastModified());
            }
        })).longValue();
    }
}
