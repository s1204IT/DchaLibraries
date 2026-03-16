package java.net;

import java.io.IOException;
import java.net.Proxy;
import java.util.Collections;
import java.util.List;

final class ProxySelectorImpl extends ProxySelector {
    ProxySelectorImpl() {
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (uri == null || sa == null || ioe == null) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public List<Proxy> select(URI uri) {
        return Collections.singletonList(selectOneProxy(uri));
    }

    private Proxy selectOneProxy(URI uri) {
        Proxy proxy;
        if (uri == null) {
            throw new IllegalArgumentException("uri == null");
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("scheme == null");
        }
        int port = -1;
        Proxy proxy2 = null;
        String nonProxyHostsKey = null;
        boolean httpProxyOkay = true;
        if ("http".equalsIgnoreCase(scheme)) {
            port = 80;
            nonProxyHostsKey = "http.nonProxyHosts";
            proxy2 = lookupProxy("http.proxyHost", "http.proxyPort", Proxy.Type.HTTP, 80);
        } else if ("https".equalsIgnoreCase(scheme)) {
            port = 443;
            nonProxyHostsKey = "https.nonProxyHosts";
            proxy2 = lookupProxy("https.proxyHost", "https.proxyPort", Proxy.Type.HTTP, 443);
        } else if ("ftp".equalsIgnoreCase(scheme)) {
            port = 80;
            nonProxyHostsKey = "ftp.nonProxyHosts";
            proxy2 = lookupProxy("ftp.proxyHost", "ftp.proxyPort", Proxy.Type.HTTP, 80);
        } else if ("socket".equalsIgnoreCase(scheme)) {
            httpProxyOkay = false;
        } else {
            return Proxy.NO_PROXY;
        }
        if (nonProxyHostsKey != null && isNonProxyHost(uri.getHost(), System.getProperty(nonProxyHostsKey))) {
            return Proxy.NO_PROXY;
        }
        if (proxy2 != null) {
            return proxy2;
        }
        if (httpProxyOkay && (proxy = lookupProxy("proxyHost", "proxyPort", Proxy.Type.HTTP, port)) != null) {
            return proxy;
        }
        Proxy proxy3 = lookupProxy("socksProxyHost", "socksProxyPort", Proxy.Type.SOCKS, 1080);
        return proxy3 != null ? proxy3 : Proxy.NO_PROXY;
    }

    private Proxy lookupProxy(String hostKey, String portKey, Proxy.Type type, int defaultPort) {
        String host = System.getProperty(hostKey);
        if (host == null || host.isEmpty()) {
            return null;
        }
        int port = getSystemPropertyInt(portKey, defaultPort);
        return new Proxy(type, InetSocketAddress.createUnresolved(host, port));
    }

    private int getSystemPropertyInt(String key, int defaultValue) {
        String string = System.getProperty(key);
        if (string != null) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean isNonProxyHost(String host, String nonProxyHosts) {
        if (host == null || nonProxyHosts == null) {
            return false;
        }
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < nonProxyHosts.length(); i++) {
            char c = nonProxyHosts.charAt(i);
            switch (c) {
                case '*':
                    patternBuilder.append(".*");
                    break;
                case '.':
                    patternBuilder.append("\\.");
                    break;
                default:
                    patternBuilder.append(c);
                    break;
            }
        }
        String pattern = patternBuilder.toString();
        return host.matches(pattern);
    }
}
