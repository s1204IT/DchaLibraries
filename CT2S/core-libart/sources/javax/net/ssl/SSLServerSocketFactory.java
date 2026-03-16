package javax.net.ssl;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import javax.net.ServerSocketFactory;
import org.apache.harmony.security.fortress.Services;

public abstract class SSLServerSocketFactory extends ServerSocketFactory {
    private static String defaultName;
    private static ServerSocketFactory defaultServerSocketFactory;
    private static int lastCacheVersion = -1;

    public abstract String[] getDefaultCipherSuites();

    public abstract String[] getSupportedCipherSuites();

    public static synchronized ServerSocketFactory getDefault() {
        SSLContext context;
        ServerSocketFactory serverSocketFactory;
        int newCacheVersion = Services.getCacheVersion();
        if (lastCacheVersion != newCacheVersion) {
            defaultServerSocketFactory = null;
            defaultName = null;
            lastCacheVersion = newCacheVersion;
        }
        if (defaultServerSocketFactory != null) {
            serverSocketFactory = defaultServerSocketFactory;
        } else {
            if (defaultName == null) {
                defaultName = Security.getProperty("ssl.ServerSocketFactory.provider");
                if (defaultName != null) {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (cl == null) {
                        cl = ClassLoader.getSystemClassLoader();
                    }
                    try {
                        Class<?> ssfc = Class.forName(defaultName, true, cl);
                        defaultServerSocketFactory = (ServerSocketFactory) ssfc.newInstance();
                    } catch (Exception e) {
                    }
                }
            }
            if (defaultServerSocketFactory == null) {
                try {
                    context = SSLContext.getDefault();
                } catch (NoSuchAlgorithmException e2) {
                    context = null;
                }
                if (context != null) {
                    defaultServerSocketFactory = context.getServerSocketFactory();
                }
            }
            if (defaultServerSocketFactory == null) {
                defaultServerSocketFactory = new DefaultSSLServerSocketFactory("No ServerSocketFactory installed");
            }
            serverSocketFactory = defaultServerSocketFactory;
        }
        return serverSocketFactory;
    }

    protected SSLServerSocketFactory() {
    }
}
