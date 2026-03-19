package java.nio.channels.spi;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import sun.nio.ch.DefaultSelectorProvider;

public abstract class SelectorProvider {
    private static final Object lock = new Object();
    private static SelectorProvider provider = null;

    public abstract DatagramChannel openDatagramChannel() throws IOException;

    public abstract DatagramChannel openDatagramChannel(ProtocolFamily protocolFamily) throws IOException;

    public abstract Pipe openPipe() throws IOException;

    public abstract AbstractSelector openSelector() throws IOException;

    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;

    public abstract SocketChannel openSocketChannel() throws IOException;

    protected SelectorProvider() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("selectorProvider"));
        }
    }

    private static boolean loadProviderFromProperty() {
        String property = System.getProperty("java.nio.channels.spi.SelectorProvider");
        if (property == null) {
            return false;
        }
        try {
            provider = (SelectorProvider) Class.forName(property, true, ClassLoader.getSystemClassLoader()).newInstance();
            return true;
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

    private static boolean loadProviderAsService() {
        Iterator it = ServiceLoader.load(SelectorProvider.class, ClassLoader.getSystemClassLoader()).iterator();
        while (it.hasNext()) {
            try {
                provider = (SelectorProvider) it.next();
                return true;
            } catch (ServiceConfigurationError e) {
                if (!(e.getCause() instanceof SecurityException)) {
                    throw e;
                }
            }
        }
        return false;
    }

    public static SelectorProvider provider() {
        synchronized (lock) {
            if (provider != null) {
                return provider;
            }
            return (SelectorProvider) AccessController.doPrivileged(new PrivilegedAction<SelectorProvider>() {
                @Override
                public SelectorProvider run() {
                    if (!SelectorProvider.loadProviderFromProperty() && !SelectorProvider.loadProviderAsService()) {
                        SelectorProvider unused = SelectorProvider.provider = DefaultSelectorProvider.create();
                        return SelectorProvider.provider;
                    }
                    return SelectorProvider.provider;
                }
            });
        }
    }

    public Channel inheritedChannel() throws IOException {
        return null;
    }
}
