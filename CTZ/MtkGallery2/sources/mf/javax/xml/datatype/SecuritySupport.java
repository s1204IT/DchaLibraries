package mf.javax.xml.datatype;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

class SecuritySupport {
    SecuritySupport() {
    }

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

    String getSystemProperty(final String propName) {
        return (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(propName);
            }
        });
    }

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

    InputStream getResourceAsStream(final ClassLoader cl, final String name) {
        return (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                if (cl == null) {
                    InputStream ris = Object.class.getResourceAsStream(name);
                    return ris;
                }
                InputStream ris2 = cl.getResourceAsStream(name);
                return ris2;
            }
        });
    }

    boolean doesFileExist(final File f) {
        return ((Boolean) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return new Boolean(f.exists());
            }
        })).booleanValue();
    }
}
