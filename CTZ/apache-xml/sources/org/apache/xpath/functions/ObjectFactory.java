package org.apache.xpath.functions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import org.apache.xalan.templates.Constants;

class ObjectFactory {
    private static final boolean DEBUG = false;
    private static final String DEFAULT_PROPERTIES_FILENAME = "xalan.properties";
    private static final String SERVICES_PATH = "META-INF/services/";
    private static Properties fXalanProperties = null;
    private static long fLastModified = -1;

    ObjectFactory() {
    }

    static Object createObject(String str, String str2) throws ConfigurationError {
        return createObject(str, null, str2);
    }

    static Object createObject(String str, String str2, String str3) throws Throwable {
        Class clsLookUpFactoryClass = lookUpFactoryClass(str, str2, str3);
        if (clsLookUpFactoryClass == null) {
            throw new ConfigurationError("Provider for " + str + " cannot be found", null);
        }
        try {
            Object objNewInstance = clsLookUpFactoryClass.newInstance();
            debugPrintln("created new instance of factory " + str);
            return objNewInstance;
        } catch (Exception e) {
            throw new ConfigurationError("Provider for factory " + str + " could not be instantiated: " + e, e);
        }
    }

    static Class lookUpFactoryClass(String str) throws ConfigurationError {
        return lookUpFactoryClass(str, null, null);
    }

    static Class lookUpFactoryClass(String str, String str2, String str3) throws Throwable {
        String strLookUpFactoryClassName = lookUpFactoryClassName(str, str2, str3);
        ClassLoader classLoaderFindClassLoader = findClassLoader();
        if (strLookUpFactoryClassName == null) {
            strLookUpFactoryClassName = str3;
        }
        try {
            Class clsFindProviderClass = findProviderClass(strLookUpFactoryClassName, classLoaderFindClassLoader, true);
            debugPrintln("created new instance of " + clsFindProviderClass + " using ClassLoader: " + classLoaderFindClassLoader);
            return clsFindProviderClass;
        } catch (ClassNotFoundException e) {
            throw new ConfigurationError("Provider " + strLookUpFactoryClassName + " not found", e);
        } catch (Exception e2) {
            throw new ConfigurationError("Provider " + strLookUpFactoryClassName + " could not be instantiated: " + e2, e2);
        }
    }

    static String lookUpFactoryClassName(String str, String str2, String str3) throws Throwable {
        Throwable th;
        FileInputStream fileInputStream;
        File file;
        String str4;
        File file2;
        boolean fileExists;
        Throwable th2;
        FileInputStream fileInputStream2;
        boolean z;
        String str5 = str2;
        SecuritySupport securitySupport = SecuritySupport.getInstance();
        try {
            String systemProperty = securitySupport.getSystemProperty(str);
            if (systemProperty != null) {
                debugPrintln("found system property, value=" + systemProperty);
                return systemProperty;
            }
        } catch (SecurityException e) {
        }
        String property = null;
        FileInputStream fileInputStream3 = null;
        property = null;
        property = null;
        if (str5 == null) {
            try {
                str4 = securitySupport.getSystemProperty("java.home") + File.separator + "lib" + File.separator + DEFAULT_PROPERTIES_FILENAME;
                try {
                    file2 = new File(str4);
                } catch (SecurityException e2) {
                    file = null;
                }
            } catch (SecurityException e3) {
                file = null;
            }
            try {
                fileExists = securitySupport.getFileExists(file2);
            } catch (SecurityException e4) {
                file = file2;
                str5 = str4;
                fLastModified = -1L;
                fXalanProperties = null;
                str4 = str5;
                file2 = file;
                fileExists = false;
            }
            synchronized (ObjectFactory.class) {
                try {
                    z = true;
                } catch (Exception e5) {
                    fileInputStream2 = null;
                } catch (Throwable th3) {
                    th2 = th3;
                    if (fileInputStream3 != null) {
                    }
                }
                if (fLastModified >= 0) {
                    if (fileExists) {
                        long j = fLastModified;
                        long lastModified = securitySupport.getLastModified(file2);
                        fLastModified = lastModified;
                        if (j < lastModified) {
                            if (z) {
                                fXalanProperties = new Properties();
                                fileInputStream2 = securitySupport.getFileInputStream(file2);
                                try {
                                    try {
                                        fXalanProperties.load(fileInputStream2);
                                    } catch (Exception e6) {
                                        fXalanProperties = null;
                                        fLastModified = -1L;
                                        if (fileInputStream2 != null) {
                                        }
                                    }
                                } catch (Throwable th4) {
                                    th2 = th4;
                                    fileInputStream3 = fileInputStream2;
                                    if (fileInputStream3 != null) {
                                        throw th2;
                                    }
                                    try {
                                        fileInputStream3.close();
                                        throw th2;
                                    } catch (IOException e7) {
                                        throw th2;
                                    }
                                }
                            } else {
                                fileInputStream2 = null;
                            }
                            if (fileInputStream2 != null) {
                                try {
                                    fileInputStream2.close();
                                } catch (IOException e8) {
                                }
                            }
                        }
                    }
                    if (!fileExists) {
                        fLastModified = -1L;
                        fXalanProperties = null;
                    }
                    z = false;
                    if (z) {
                    }
                    if (fileInputStream2 != null) {
                    }
                } else {
                    if (fileExists) {
                        fLastModified = securitySupport.getLastModified(file2);
                    } else {
                        z = false;
                    }
                    if (z) {
                    }
                    if (fileInputStream2 != null) {
                    }
                }
            }
            property = fXalanProperties != null ? fXalanProperties.getProperty(str) : null;
            str5 = str4;
        } else {
            try {
                fileInputStream = securitySupport.getFileInputStream(new File(str5));
                try {
                    Properties properties = new Properties();
                    properties.load(fileInputStream);
                    String property2 = properties.getProperty(str);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e9) {
                        }
                    }
                    property = property2;
                } catch (Exception e10) {
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e11) {
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                    if (fileInputStream == null) {
                        throw th;
                    }
                    try {
                        fileInputStream.close();
                        throw th;
                    } catch (IOException e12) {
                        throw th;
                    }
                }
            } catch (Exception e13) {
                fileInputStream = null;
            } catch (Throwable th6) {
                th = th6;
                fileInputStream = null;
            }
        }
        if (property == null) {
            return findJarServiceProviderName(str);
        }
        debugPrintln("found in " + str5 + ", value=" + property);
        return property;
    }

    private static void debugPrintln(String str) {
    }

    static ClassLoader findClassLoader() throws ConfigurationError {
        SecuritySupport securitySupport = SecuritySupport.getInstance();
        ClassLoader contextClassLoader = securitySupport.getContextClassLoader();
        ClassLoader systemClassLoader = securitySupport.getSystemClassLoader();
        for (ClassLoader parentClassLoader = systemClassLoader; contextClassLoader != parentClassLoader; parentClassLoader = securitySupport.getParentClassLoader(parentClassLoader)) {
            if (parentClassLoader == null) {
                return contextClassLoader;
            }
        }
        ClassLoader classLoader = ObjectFactory.class.getClassLoader();
        for (ClassLoader parentClassLoader2 = systemClassLoader; classLoader != parentClassLoader2; parentClassLoader2 = securitySupport.getParentClassLoader(parentClassLoader2)) {
            if (parentClassLoader2 == null) {
                return classLoader;
            }
        }
        return systemClassLoader;
    }

    static Object newInstance(String str, ClassLoader classLoader, boolean z) throws ConfigurationError {
        try {
            Class clsFindProviderClass = findProviderClass(str, classLoader, z);
            Object objNewInstance = clsFindProviderClass.newInstance();
            debugPrintln("created new instance of " + clsFindProviderClass + " using ClassLoader: " + classLoader);
            return objNewInstance;
        } catch (ClassNotFoundException e) {
            throw new ConfigurationError("Provider " + str + " not found", e);
        } catch (Exception e2) {
            throw new ConfigurationError("Provider " + str + " could not be instantiated: " + e2, e2);
        }
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

    private static String findJarServiceProviderName(String str) {
        BufferedReader bufferedReader;
        ClassLoader classLoader;
        SecuritySupport securitySupport = SecuritySupport.getInstance();
        String str2 = SERVICES_PATH + str;
        ClassLoader classLoaderFindClassLoader = findClassLoader();
        InputStream resourceAsStream = securitySupport.getResourceAsStream(classLoaderFindClassLoader, str2);
        if (resourceAsStream == null && classLoaderFindClassLoader != (classLoader = ObjectFactory.class.getClassLoader())) {
            resourceAsStream = securitySupport.getResourceAsStream(classLoader, str2);
            classLoaderFindClassLoader = classLoader;
        }
        if (resourceAsStream == null) {
            return null;
        }
        debugPrintln("found jar resource=" + str2 + " using ClassLoader: " + classLoaderFindClassLoader);
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
        }
        try {
            String line = bufferedReader.readLine();
            try {
                bufferedReader.close();
            } catch (IOException e2) {
            }
            if (line == null || "".equals(line)) {
                return null;
            }
            debugPrintln("found in resource, value=" + line);
            return line;
        } catch (IOException e3) {
            try {
                bufferedReader.close();
            } catch (IOException e4) {
            }
            return null;
        } catch (Throwable th) {
            try {
                bufferedReader.close();
            } catch (IOException e5) {
            }
            throw th;
        }
    }

    static class ConfigurationError extends Error {
        static final long serialVersionUID = -5782303800588797207L;
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
