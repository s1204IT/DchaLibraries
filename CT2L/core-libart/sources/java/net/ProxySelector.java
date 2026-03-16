package java.net;

import java.io.IOException;
import java.util.List;

public abstract class ProxySelector {
    private static ProxySelector defaultSelector = new ProxySelectorImpl();

    public abstract void connectFailed(URI uri, SocketAddress socketAddress, IOException iOException);

    public abstract List<Proxy> select(URI uri);

    public static ProxySelector getDefault() {
        return defaultSelector;
    }

    public static void setDefault(ProxySelector selector) {
        defaultSelector = selector;
    }
}
