package org.apache.xpath.functions;

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
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    return cl;
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
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    return cl;
                } catch (SecurityException e) {
                    return null;
                }
            }
        });
    }

    @Override
    ClassLoader getParentClassLoader(final ClassLoader cl) {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                ClassLoader parent = null;
                try {
                    parent = cl.getParent();
                } catch (SecurityException e) {
                }
                if (parent == cl) {
                    return null;
                }
                return parent;
            }
        });
    }

    @Override
    String getSystemProperty(final String propName) {
        return (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(propName);
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
    InputStream getResourceAsStream(final ClassLoader cl, final String name) {
        return (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                if (cl == null) {
                    InputStream ris = ClassLoader.getSystemResourceAsStream(name);
                    return ris;
                }
                InputStream ris2 = cl.getResourceAsStream(name);
                return ris2;
            }
        });
    }

    @Override
    boolean getFileExists(final File f) {
        return ((Boolean) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return new Boolean(f.exists());
            }
        })).booleanValue();
    }

    @Override
    long getLastModified(final File f) {
        return ((Long) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return new Long(f.lastModified());
            }
        })).longValue();
    }
}
