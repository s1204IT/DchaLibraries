package android.net;

import android.content.Context;
import android.text.TextUtils;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Proxy {
    private static final String EXCL_REGEX = "[a-zA-Z0-9*]+(\\-[a-zA-Z0-9*]+)*(\\.[a-zA-Z0-9*]+(\\-[a-zA-Z0-9*]+)*)*";

    @Deprecated
    public static final String EXTRA_PROXY_INFO = "android.intent.extra.PROXY_INFO";
    private static final String NAME_IP_REGEX = "[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*";
    public static final String PROXY_CHANGE_ACTION = "android.intent.action.PROXY_CHANGE";
    public static final int PROXY_EXCLLIST_INVALID = 5;
    public static final int PROXY_HOSTNAME_EMPTY = 1;
    public static final int PROXY_HOSTNAME_INVALID = 2;
    public static final int PROXY_PORT_EMPTY = 3;
    public static final int PROXY_PORT_INVALID = 4;
    public static final int PROXY_VALID = 0;
    private static final String TAG = "Proxy";
    private static ConnectivityManager sConnectivityManager = null;
    private static final String HOSTNAME_REGEXP = "^$|^[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*$";
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEXP);
    private static final String EXCLLIST_REGEXP = "^$|^[a-zA-Z0-9*]+(\\-[a-zA-Z0-9*]+)*(\\.[a-zA-Z0-9*]+(\\-[a-zA-Z0-9*]+)*)*(,[a-zA-Z0-9*]+(\\-[a-zA-Z0-9*]+)*(\\.[a-zA-Z0-9*]+(\\-[a-zA-Z0-9*]+)*)*)*$";
    private static final Pattern EXCLLIST_PATTERN = Pattern.compile(EXCLLIST_REGEXP);
    private static final ProxySelector sDefaultProxySelector = ProxySelector.getDefault();

    public static final java.net.Proxy getProxy(Context context, String str) {
        if (str != null && !isLocalHost("")) {
            List<java.net.Proxy> listSelect = ProxySelector.getDefault().select(URI.create(str));
            if (listSelect.size() > 0) {
                return listSelect.get(0);
            }
        }
        return java.net.Proxy.NO_PROXY;
    }

    @Deprecated
    public static final String getHost(Context context) {
        java.net.Proxy proxy = getProxy(context, null);
        if (proxy == java.net.Proxy.NO_PROXY) {
            return null;
        }
        try {
            return ((InetSocketAddress) proxy.address()).getHostName();
        } catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    public static final int getPort(Context context) {
        java.net.Proxy proxy = getProxy(context, null);
        if (proxy == java.net.Proxy.NO_PROXY) {
            return -1;
        }
        try {
            return ((InetSocketAddress) proxy.address()).getPort();
        } catch (Exception e) {
            return -1;
        }
    }

    @Deprecated
    public static final String getDefaultHost() {
        String property = System.getProperty("http.proxyHost");
        if (TextUtils.isEmpty(property)) {
            return null;
        }
        return property;
    }

    @Deprecated
    public static final int getDefaultPort() {
        if (getDefaultHost() == null) {
            return -1;
        }
        try {
            return Integer.parseInt(System.getProperty("http.proxyPort"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static final boolean isLocalHost(String str) {
        if (str != null && str != null) {
            try {
                if (str.equalsIgnoreCase(ProxyInfo.LOCAL_HOST)) {
                    return true;
                }
                if (NetworkUtils.numericToInetAddress(str).isLoopbackAddress()) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
            }
        }
        return false;
    }

    public static int validate(String str, String str2, String str3) {
        Matcher matcher = HOSTNAME_PATTERN.matcher(str);
        Matcher matcher2 = EXCLLIST_PATTERN.matcher(str3);
        if (!matcher.matches()) {
            return 2;
        }
        if (!matcher2.matches()) {
            return 5;
        }
        if (str.length() > 0 && str2.length() == 0) {
            return 3;
        }
        if (str2.length() > 0) {
            if (str.length() == 0) {
                return 1;
            }
            try {
                int i = Integer.parseInt(str2);
                if (i <= 0 || i > 65535) {
                    return 4;
                }
                return 0;
            } catch (NumberFormatException e) {
                return 4;
            }
        }
        return 0;
    }

    public static final void setHttpProxySystemProperty(ProxyInfo proxyInfo) {
        Uri pacFileUrl;
        String string;
        String exclusionListAsString;
        Uri uri = Uri.EMPTY;
        String host = null;
        if (proxyInfo != null) {
            host = proxyInfo.getHost();
            string = Integer.toString(proxyInfo.getPort());
            exclusionListAsString = proxyInfo.getExclusionListAsString();
            pacFileUrl = proxyInfo.getPacFileUrl();
        } else {
            pacFileUrl = uri;
            string = null;
            exclusionListAsString = null;
        }
        setHttpProxySystemProperty(host, string, exclusionListAsString, pacFileUrl);
    }

    public static final void setHttpProxySystemProperty(String str, String str2, String str3, Uri uri) {
        if (str3 != null) {
            str3 = str3.replace(",", "|");
        }
        if (str != null) {
            System.setProperty("http.proxyHost", str);
            System.setProperty("https.proxyHost", str);
        } else {
            System.clearProperty("http.proxyHost");
            System.clearProperty("https.proxyHost");
        }
        if (str2 != null) {
            System.setProperty("http.proxyPort", str2);
            System.setProperty("https.proxyPort", str2);
        } else {
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyPort");
        }
        if (str3 != null) {
            System.setProperty("http.nonProxyHosts", str3);
            System.setProperty("https.nonProxyHosts", str3);
        } else {
            System.clearProperty("http.nonProxyHosts");
            System.clearProperty("https.nonProxyHosts");
        }
        if (!Uri.EMPTY.equals(uri)) {
            ProxySelector.setDefault(new PacProxySelector());
        } else {
            ProxySelector.setDefault(sDefaultProxySelector);
        }
    }
}
