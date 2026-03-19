package javax.net.ssl;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import javax.net.ServerSocketFactory;

public abstract class SSLServerSocketFactory extends ServerSocketFactory {
    private static SSLServerSocketFactory defaultServerSocketFactory;
    private static int lastVersion = -1;

    public abstract String[] getDefaultCipherSuites();

    public abstract String[] getSupportedCipherSuites();

    private static void log(String str) {
        if (SSLSocketFactory.DEBUG) {
            System.out.println(str);
        }
    }

    protected SSLServerSocketFactory() {
    }

    public static synchronized ServerSocketFactory getDefault() throws ClassNotFoundException {
        SSLContext sSLContext;
        if (defaultServerSocketFactory != null && lastVersion == Security.getVersion()) {
            return defaultServerSocketFactory;
        }
        lastVersion = Security.getVersion();
        SSLServerSocketFactory sSLServerSocketFactory = defaultServerSocketFactory;
        Class<?> cls = null;
        defaultServerSocketFactory = null;
        String securityProperty = SSLSocketFactory.getSecurityProperty("ssl.ServerSocketFactory.provider");
        if (securityProperty != null) {
            if (sSLServerSocketFactory != null && securityProperty.equals(sSLServerSocketFactory.getClass().getName())) {
                defaultServerSocketFactory = sSLServerSocketFactory;
                return defaultServerSocketFactory;
            }
            log("setting up default SSLServerSocketFactory");
            try {
                try {
                    cls = Class.forName(securityProperty);
                } catch (Exception e) {
                    log("SSLServerSocketFactory instantiation failed: " + ((Object) e));
                    try {
                        sSLContext = SSLContext.getDefault();
                        if (sSLContext == null) {
                        }
                        return defaultServerSocketFactory;
                    } catch (NoSuchAlgorithmException e2) {
                        return new DefaultSSLServerSocketFactory(e2);
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
            SSLServerSocketFactory sSLServerSocketFactory2 = (SSLServerSocketFactory) cls.newInstance();
            log("instantiated an instance of class " + securityProperty);
            defaultServerSocketFactory = sSLServerSocketFactory2;
            return sSLServerSocketFactory2;
        }
        sSLContext = SSLContext.getDefault();
        if (sSLContext == null) {
            defaultServerSocketFactory = sSLContext.getServerSocketFactory();
        } else {
            defaultServerSocketFactory = new DefaultSSLServerSocketFactory(new IllegalStateException("No factory found."));
        }
        return defaultServerSocketFactory;
    }
}
