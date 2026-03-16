package org.apache.xml.serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

class SecuritySupport {
    private static final Object securitySupport;

    SecuritySupport() {
    }

    static {
        SecuritySupport ss = null;
        try {
            Class.forName("java.security.AccessController");
            SecuritySupport ss2 = new SecuritySupport12();
            securitySupport = ss2 == null ? new SecuritySupport() : ss2;
        } catch (Exception e) {
            if (0 == 0) {
                ss = new SecuritySupport();
            }
            securitySupport = ss;
        } catch (Throwable th) {
            if (0 == 0) {
                ss = new SecuritySupport();
            }
            securitySupport = ss;
            throw th;
        }
    }

    static SecuritySupport getInstance() {
        return (SecuritySupport) securitySupport;
    }

    ClassLoader getContextClassLoader() {
        return null;
    }

    ClassLoader getSystemClassLoader() {
        return null;
    }

    ClassLoader getParentClassLoader(ClassLoader cl) {
        return null;
    }

    String getSystemProperty(String propName) {
        return System.getProperty(propName);
    }

    FileInputStream getFileInputStream(File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    InputStream getResourceAsStream(ClassLoader cl, String name) {
        if (cl == null) {
            InputStream ris = ClassLoader.getSystemResourceAsStream(name);
            return ris;
        }
        InputStream ris2 = cl.getResourceAsStream(name);
        return ris2;
    }

    boolean getFileExists(File f) {
        return f.exists();
    }

    long getLastModified(File f) {
        return f.lastModified();
    }
}
