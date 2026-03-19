package sun.net.spi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.SocksConsts;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import sun.net.NetProperties;
import sun.net.SocksProxy;

public class DefaultProxySelector extends ProxySelector {
    private static final String SOCKS_PROXY_VERSION = "socksProxyVersion";
    static final String[][] props = {new String[]{"http", "http.proxy", "proxy", "socksProxy"}, new String[]{"https", "https.proxy", "proxy", "socksProxy"}, new String[]{"ftp", "ftp.proxy", "ftpProxy", "proxy", "socksProxy"}, new String[]{"gopher", "gopherProxy", "socksProxy"}, new String[]{"socket", "socksProxy"}};
    private static boolean hasSystemProxies = false;

    static class NonProxyInfo {
        final String defaultVal;
        String hostsSource;
        Pattern pattern;
        final String property;
        static final String defStringVal = "localhost|127.*|[::1]|0.0.0.0|[::0]";
        static NonProxyInfo ftpNonProxyInfo = new NonProxyInfo("ftp.nonProxyHosts", null, null, defStringVal);
        static NonProxyInfo httpNonProxyInfo = new NonProxyInfo("http.nonProxyHosts", null, null, defStringVal);
        static NonProxyInfo socksNonProxyInfo = new NonProxyInfo("socksNonProxyHosts", null, null, defStringVal);
        static NonProxyInfo httpsNonProxyInfo = new NonProxyInfo("https.nonProxyHosts", null, null, defStringVal);

        NonProxyInfo(String str, String str2, Pattern pattern, String str3) {
            this.property = str;
            this.hostsSource = str2;
            this.pattern = pattern;
            this.defaultVal = str3;
        }
    }

    @Override
    public List<Proxy> select(URI uri) {
        String authority;
        if (uri == null) {
            throw new IllegalArgumentException("URI can't be null.");
        }
        final String scheme = uri.getScheme();
        String host = uri.getHost();
        if (host == null && (authority = uri.getAuthority()) != null) {
            int iIndexOf = authority.indexOf(64);
            if (iIndexOf >= 0) {
                authority = authority.substring(iIndexOf + 1);
            }
            int iLastIndexOf = authority.lastIndexOf(58);
            if (iLastIndexOf >= 0) {
                authority = authority.substring(0, iLastIndexOf);
            }
            host = authority;
        }
        if (scheme == null || host == null) {
            throw new IllegalArgumentException("protocol = " + scheme + " host = " + host);
        }
        ArrayList arrayList = new ArrayList(1);
        final NonProxyInfo nonProxyInfo = null;
        if ("http".equalsIgnoreCase(scheme)) {
            nonProxyInfo = NonProxyInfo.httpNonProxyInfo;
        } else if ("https".equalsIgnoreCase(scheme)) {
            nonProxyInfo = NonProxyInfo.httpsNonProxyInfo;
        } else if ("ftp".equalsIgnoreCase(scheme)) {
            nonProxyInfo = NonProxyInfo.ftpNonProxyInfo;
        } else if ("socket".equalsIgnoreCase(scheme)) {
            nonProxyInfo = NonProxyInfo.socksNonProxyInfo;
        }
        final String lowerCase = host.toLowerCase();
        arrayList.add((Proxy) AccessController.doPrivileged(new PrivilegedAction<Proxy>() {
            @Override
            public Proxy run() {
                for (int i = 0; i < DefaultProxySelector.props.length; i++) {
                    if (DefaultProxySelector.props[i][0].equalsIgnoreCase(scheme)) {
                        String str = null;
                        int i2 = 1;
                        while (i2 < DefaultProxySelector.props[i].length) {
                            str = NetProperties.get(DefaultProxySelector.props[i][i2] + "Host");
                            if (str != null && str.length() != 0) {
                                break;
                            }
                            i2++;
                        }
                        if (str == null || str.length() == 0) {
                            return Proxy.NO_PROXY;
                        }
                        if (nonProxyInfo != null) {
                            String str2 = NetProperties.get(nonProxyInfo.property);
                            synchronized (nonProxyInfo) {
                                try {
                                    if (str2 == null) {
                                        if (nonProxyInfo.defaultVal == null) {
                                            nonProxyInfo.hostsSource = null;
                                            nonProxyInfo.pattern = null;
                                        } else {
                                            str2 = nonProxyInfo.defaultVal;
                                        }
                                    } else if (str2.length() != 0) {
                                        str2 = str2 + "|localhost|127.*|[::1]|0.0.0.0|[::0]";
                                    }
                                    if (str2 != null && !str2.equals(nonProxyInfo.hostsSource)) {
                                        nonProxyInfo.pattern = DefaultProxySelector.toPattern(str2);
                                        nonProxyInfo.hostsSource = str2;
                                    }
                                    if (DefaultProxySelector.shouldNotUseProxyFor(nonProxyInfo.pattern, lowerCase)) {
                                        return Proxy.NO_PROXY;
                                    }
                                } finally {
                                }
                            }
                        }
                        int iIntValue = NetProperties.getInteger(DefaultProxySelector.props[i][i2] + "Port", 0).intValue();
                        if (iIntValue == 0 && i2 < DefaultProxySelector.props[i].length - 1) {
                            int iIntValue2 = iIntValue;
                            for (int i3 = 1; i3 < DefaultProxySelector.props[i].length - 1; i3++) {
                                if (i3 != i2 && iIntValue2 == 0) {
                                    iIntValue2 = NetProperties.getInteger(DefaultProxySelector.props[i][i3] + "Port", 0).intValue();
                                }
                            }
                            iIntValue = iIntValue2;
                        }
                        if (iIntValue == 0) {
                            iIntValue = i2 == DefaultProxySelector.props[i].length - 1 ? DefaultProxySelector.this.defaultPort("socket") : DefaultProxySelector.this.defaultPort(scheme);
                        }
                        InetSocketAddress inetSocketAddressCreateUnresolved = InetSocketAddress.createUnresolved(str, iIntValue);
                        if (i2 == DefaultProxySelector.props[i].length - 1) {
                            return SocksProxy.create(inetSocketAddressCreateUnresolved, NetProperties.getInteger(DefaultProxySelector.SOCKS_PROXY_VERSION, 5).intValue());
                        }
                        return new Proxy(Proxy.Type.HTTP, inetSocketAddressCreateUnresolved);
                    }
                }
                return Proxy.NO_PROXY;
            }
        }));
        return arrayList;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress socketAddress, IOException iOException) {
        if (uri == null || socketAddress == null || iOException == null) {
            throw new IllegalArgumentException("Arguments can't be null.");
        }
    }

    private int defaultPort(String str) {
        if ("http".equalsIgnoreCase(str)) {
            return 80;
        }
        if ("https".equalsIgnoreCase(str)) {
            return 443;
        }
        if ("ftp".equalsIgnoreCase(str)) {
            return 80;
        }
        if ("socket".equalsIgnoreCase(str)) {
            return SocksConsts.DEFAULT_PORT;
        }
        return "gopher".equalsIgnoreCase(str) ? 80 : -1;
    }

    static boolean shouldNotUseProxyFor(Pattern pattern, String str) {
        if (pattern == null || str.isEmpty()) {
            return false;
        }
        return pattern.matcher(str).matches();
    }

    static Pattern toPattern(String str) {
        StringJoiner stringJoiner = new StringJoiner("|");
        boolean z = true;
        for (String str2 : str.split("\\|")) {
            if (!str2.isEmpty()) {
                stringJoiner.add(disjunctToRegex(str2.toLowerCase()));
                z = false;
            }
        }
        if (z) {
            return null;
        }
        return Pattern.compile(stringJoiner.toString());
    }

    static String disjunctToRegex(String str) {
        if (str.startsWith("*")) {
            return ".*" + Pattern.quote(str.substring(1));
        }
        if (str.endsWith("*")) {
            return Pattern.quote(str.substring(0, str.length() - 1)) + ".*";
        }
        return Pattern.quote(str);
    }
}
