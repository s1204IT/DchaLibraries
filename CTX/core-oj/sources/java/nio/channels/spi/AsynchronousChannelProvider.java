package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import sun.nio.ch.DefaultAsynchronousChannelProvider;

public abstract class AsynchronousChannelProvider {
    public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(int i, ThreadFactory threadFactory) throws IOException;

    public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executorService, int i) throws IOException;

    public abstract AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException;

    public abstract AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException;

    private static Void checkPermission() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("asynchronousChannelProvider"));
            return null;
        }
        return null;
    }

    private AsynchronousChannelProvider(Void r1) {
    }

    protected AsynchronousChannelProvider() {
        this(checkPermission());
    }

    private static class ProviderHolder {
        static final AsynchronousChannelProvider provider = load();

        private ProviderHolder() {
        }

        private static AsynchronousChannelProvider load() {
            return (AsynchronousChannelProvider) AccessController.doPrivileged(new PrivilegedAction<AsynchronousChannelProvider>() {
                @Override
                public AsynchronousChannelProvider run() {
                    AsynchronousChannelProvider asynchronousChannelProviderLoadProviderFromProperty = ProviderHolder.loadProviderFromProperty();
                    if (asynchronousChannelProviderLoadProviderFromProperty == null) {
                        AsynchronousChannelProvider asynchronousChannelProviderLoadProviderAsService = ProviderHolder.loadProviderAsService();
                        if (asynchronousChannelProviderLoadProviderAsService != null) {
                            return asynchronousChannelProviderLoadProviderAsService;
                        }
                        return DefaultAsynchronousChannelProvider.create();
                    }
                    return asynchronousChannelProviderLoadProviderFromProperty;
                }
            });
        }

        private static AsynchronousChannelProvider loadProviderFromProperty() {
            String property = System.getProperty("java.nio.channels.spi.AsynchronousChannelProvider");
            if (property == null) {
                return null;
            }
            try {
                return (AsynchronousChannelProvider) Class.forName(property, true, ClassLoader.getSystemClassLoader()).newInstance();
            } catch (ClassNotFoundException e) {
                throw new ServiceConfigurationError(null, e);
            } catch (IllegalAccessException e2) {
                throw new ServiceConfigurationError(null, e2);
            } catch (InstantiationException e3) {
                throw new ServiceConfigurationError(null, e3);
            } catch (SecurityException e4) {
                throw new ServiceConfigurationError(null, e4);
            }
        }

        private static AsynchronousChannelProvider loadProviderAsService() {
            Iterator it = ServiceLoader.load(AsynchronousChannelProvider.class, ClassLoader.getSystemClassLoader()).iterator();
            while (it.hasNext()) {
                try {
                    return (AsynchronousChannelProvider) it.next();
                } catch (ServiceConfigurationError e) {
                    if (!(e.getCause() instanceof SecurityException)) {
                        throw e;
                    }
                }
            }
            return null;
        }
    }

    public static AsynchronousChannelProvider provider() {
        return ProviderHolder.provider;
    }
}
