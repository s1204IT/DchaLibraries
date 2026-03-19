package org.apache.xml.dtm.ref;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

class SecuritySupport {
    private static final Object securitySupport;

    SecuritySupport() {
    }

    static {
        Object securitySupport2;
        try {
            Class.forName("java.security.AccessController");
            securitySupport2 = new SecuritySupport12();
        } catch (Exception e) {
            securitySupport2 = new SecuritySupport();
        } catch (Throwable th) {
            securitySupport = new SecuritySupport();
            throw th;
        }
        securitySupport = securitySupport2;
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

    ClassLoader getParentClassLoader(ClassLoader classLoader) {
        return null;
    }

    String getSystemProperty(String str) {
        return System.getProperty(str);
    }

    FileInputStream getFileInputStream(File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    InputStream getResourceAsStream(ClassLoader classLoader, String str) {
        if (classLoader == null) {
            return ClassLoader.getSystemResourceAsStream(str);
        }
        return classLoader.getResourceAsStream(str);
    }

    boolean getFileExists(File file) {
        return file.exists();
    }

    long getLastModified(File file) {
        return file.lastModified();
    }
}
