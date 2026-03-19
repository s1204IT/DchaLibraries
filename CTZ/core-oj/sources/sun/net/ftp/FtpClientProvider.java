package sun.net.ftp;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceConfigurationError;
import sun.net.ftp.impl.DefaultFtpClientProvider;

public abstract class FtpClientProvider {
    private static final Object lock = new Object();
    private static FtpClientProvider provider = null;

    public abstract FtpClient createFtpClient();

    protected FtpClientProvider() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("ftpClientProvider"));
        }
    }

    private static boolean loadProviderFromProperty() {
        String property = System.getProperty("sun.net.ftpClientProvider");
        if (property == null) {
            return false;
        }
        try {
            provider = (FtpClientProvider) Class.forName(property, true, null).newInstance();
            return true;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SecurityException e) {
            throw new ServiceConfigurationError(e.toString());
        }
    }

    private static boolean loadProviderAsService() {
        return false;
    }

    public static FtpClientProvider provider() {
        synchronized (lock) {
            if (provider != null) {
                return provider;
            }
            return (FtpClientProvider) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    if (!FtpClientProvider.loadProviderFromProperty() && !FtpClientProvider.loadProviderAsService()) {
                        FtpClientProvider unused = FtpClientProvider.provider = new DefaultFtpClientProvider();
                        return FtpClientProvider.provider;
                    }
                    return FtpClientProvider.provider;
                }
            });
        }
    }
}
