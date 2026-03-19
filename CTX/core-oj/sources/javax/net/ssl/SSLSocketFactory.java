package javax.net.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Locale;
import javax.net.SocketFactory;
import sun.security.action.GetPropertyAction;

public abstract class SSLSocketFactory extends SocketFactory {
    static final boolean DEBUG;
    private static SSLSocketFactory defaultSocketFactory;
    private static int lastVersion = -1;

    public abstract Socket createSocket(Socket socket, String str, int i, boolean z) throws IOException;

    public abstract String[] getDefaultCipherSuites();

    public abstract String[] getSupportedCipherSuites();

    static {
        String lowerCase = ((String) AccessController.doPrivileged(new GetPropertyAction("javax.net.debug", ""))).toLowerCase(Locale.ENGLISH);
        DEBUG = lowerCase.contains("all") || lowerCase.contains("ssl");
    }

    private static void log(String str) {
        if (DEBUG) {
            System.out.println(str);
        }
    }

    public static synchronized SocketFactory getDefault() throws ClassNotFoundException {
        SSLContext sSLContext;
        if (defaultSocketFactory != null && lastVersion == Security.getVersion()) {
            return defaultSocketFactory;
        }
        lastVersion = Security.getVersion();
        SSLSocketFactory sSLSocketFactory = defaultSocketFactory;
        Class<?> cls = null;
        defaultSocketFactory = null;
        String securityProperty = getSecurityProperty("ssl.SocketFactory.provider");
        if (securityProperty != null) {
            if (sSLSocketFactory != null && securityProperty.equals(sSLSocketFactory.getClass().getName())) {
                defaultSocketFactory = sSLSocketFactory;
                return defaultSocketFactory;
            }
            log("setting up default SSLSocketFactory");
            try {
                try {
                    cls = Class.forName(securityProperty);
                } catch (Exception e) {
                    log("SSLSocketFactory instantiation failed: " + e.toString());
                    try {
                        sSLContext = SSLContext.getDefault();
                        if (sSLContext == null) {
                        }
                        return defaultSocketFactory;
                    } catch (NoSuchAlgorithmException e2) {
                        return new DefaultSSLSocketFactory(e2);
                    }
                }
            } catch (ClassNotFoundException e3) {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                if (contextClassLoader == null) {
                    contextClassLoader = ClassLoader.getSystemClassLoader();
                }
                if (contextClassLoader != null) {
                    cls = Class.forName(securityProperty, true, contextClassLoader);
                }
            }
            log("class " + securityProperty + " is loaded");
            SSLSocketFactory sSLSocketFactory2 = (SSLSocketFactory) cls.newInstance();
            log("instantiated an instance of class " + securityProperty);
            defaultSocketFactory = sSLSocketFactory2;
            return sSLSocketFactory2;
        }
        sSLContext = SSLContext.getDefault();
        if (sSLContext == null) {
            defaultSocketFactory = sSLContext.getSocketFactory();
        } else {
            defaultSocketFactory = new DefaultSSLSocketFactory(new IllegalStateException("No factory found."));
        }
        return defaultSocketFactory;
    }

    static String getSecurityProperty(final String str) {
        return (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                String property = Security.getProperty(str);
                if (property != null) {
                    String strTrim = property.trim();
                    if (strTrim.length() == 0) {
                        return null;
                    }
                    return strTrim;
                }
                return property;
            }
        });
    }

    public Socket createSocket(Socket socket, InputStream inputStream, boolean z) throws IOException {
        throw new UnsupportedOperationException();
    }
}
