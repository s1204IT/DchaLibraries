package java.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.util.logging.PlatformLogger;

public class CookieManager extends CookieHandler {
    private CookieStore cookieJar;
    private CookiePolicy policyCallback;

    public CookieManager() {
        this(null, null);
    }

    public CookieManager(CookieStore cookieStore, CookiePolicy cookiePolicy) {
        this.cookieJar = null;
        this.policyCallback = cookiePolicy == null ? CookiePolicy.ACCEPT_ORIGINAL_SERVER : cookiePolicy;
        if (cookieStore == null) {
            this.cookieJar = new InMemoryCookieStore();
        } else {
            this.cookieJar = cookieStore;
        }
    }

    public void setCookiePolicy(CookiePolicy cookiePolicy) {
        if (cookiePolicy != null) {
            this.policyCallback = cookiePolicy;
        }
    }

    public CookieStore getCookieStore() {
        return this.cookieJar;
    }

    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> map) throws IOException {
        if (uri == null || map == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        HashMap map2 = new HashMap();
        if (this.cookieJar == null) {
            return Collections.unmodifiableMap(map2);
        }
        boolean zEqualsIgnoreCase = "https".equalsIgnoreCase(uri.getScheme());
        ArrayList arrayList = new ArrayList();
        for (HttpCookie httpCookie : this.cookieJar.get(uri)) {
            if (pathMatches(uri, httpCookie) && (zEqualsIgnoreCase || !httpCookie.getSecure())) {
                String portlist = httpCookie.getPortlist();
                if (portlist != null && !portlist.isEmpty()) {
                    int port = uri.getPort();
                    if (port == -1) {
                        port = "https".equals(uri.getScheme()) ? 443 : 80;
                    }
                    if (isInPortList(portlist, port)) {
                        arrayList.add(httpCookie);
                    }
                } else {
                    arrayList.add(httpCookie);
                }
            }
        }
        if (arrayList.isEmpty()) {
            return Collections.emptyMap();
        }
        map2.put("Cookie", sortByPath(arrayList));
        return Collections.unmodifiableMap(map2);
    }

    @Override
    public void put(URI uri, Map<String, List<String>> map) throws IOException {
        List<HttpCookie> listEmptyList;
        if (uri == null || map == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        if (this.cookieJar == null) {
            return;
        }
        PlatformLogger logger = PlatformLogger.getLogger("java.net.CookieManager");
        for (String str : map.keySet()) {
            if (str != null && (str.equalsIgnoreCase("Set-Cookie2") || str.equalsIgnoreCase("Set-Cookie"))) {
                for (String str2 : map.get(str)) {
                    try {
                        listEmptyList = HttpCookie.parse(str2);
                    } catch (IllegalArgumentException e) {
                        listEmptyList = Collections.emptyList();
                        if (logger.isLoggable(PlatformLogger.Level.SEVERE)) {
                            logger.severe("Invalid cookie for " + ((Object) uri) + ": " + str2);
                        }
                    }
                    for (HttpCookie httpCookie : listEmptyList) {
                        try {
                            if (httpCookie.getPath() == null) {
                                String path = uri.getPath();
                                if (!path.endsWith("/")) {
                                    int iLastIndexOf = path.lastIndexOf("/");
                                    if (iLastIndexOf > 0) {
                                        path = path.substring(0, iLastIndexOf + 1);
                                    } else {
                                        path = "/";
                                    }
                                }
                                httpCookie.setPath(path);
                            } else if (!pathMatches(uri, httpCookie)) {
                            }
                            if (httpCookie.getDomain() == null) {
                                String host = uri.getHost();
                                if (host != null && !host.contains(".")) {
                                    host = host + ".local";
                                }
                                httpCookie.setDomain(host);
                            }
                            String portlist = httpCookie.getPortlist();
                            if (portlist != null) {
                                int port = uri.getPort();
                                if (port == -1) {
                                    port = "https".equals(uri.getScheme()) ? 443 : 80;
                                }
                                if (portlist.isEmpty()) {
                                    httpCookie.setPortlist("" + port);
                                    if (shouldAcceptInternal(uri, httpCookie)) {
                                        this.cookieJar.add(uri, httpCookie);
                                    }
                                } else if (isInPortList(portlist, port) && shouldAcceptInternal(uri, httpCookie)) {
                                    this.cookieJar.add(uri, httpCookie);
                                }
                            } else if (shouldAcceptInternal(uri, httpCookie)) {
                                this.cookieJar.add(uri, httpCookie);
                            }
                        } catch (IllegalArgumentException e2) {
                        }
                    }
                }
            }
        }
    }

    private boolean shouldAcceptInternal(URI uri, HttpCookie httpCookie) {
        try {
            return this.policyCallback.shouldAccept(uri, httpCookie);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isInPortList(String str, int i) {
        int iIndexOf = str.indexOf(",");
        while (iIndexOf > 0) {
            if (Integer.parseInt(str.substring(0, iIndexOf)) == i) {
                return true;
            }
            str = str.substring(iIndexOf + 1);
            iIndexOf = str.indexOf(",");
        }
        if (!str.isEmpty()) {
            try {
                if (Integer.parseInt(str) == i) {
                    return true;
                }
            } catch (NumberFormatException e) {
            }
        }
        return false;
    }

    private static boolean pathMatches(URI uri, HttpCookie httpCookie) {
        return normalizePath(uri.getPath()).startsWith(normalizePath(httpCookie.getPath()));
    }

    private static String normalizePath(String str) {
        if (str == null) {
            str = "";
        }
        if (!str.endsWith("/")) {
            return str + "/";
        }
        return str;
    }

    private List<String> sortByPath(List<HttpCookie> list) {
        Collections.sort(list, new CookiePathComparator());
        StringBuilder sb = new StringBuilder();
        int version = 1;
        for (HttpCookie httpCookie : list) {
            if (httpCookie.getVersion() < version) {
                version = httpCookie.getVersion();
            }
        }
        if (version == 1) {
            sb.append("$Version=\"1\"; ");
        }
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                sb.append("; ");
            }
            sb.append(list.get(i).toString());
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(sb.toString());
        return arrayList;
    }

    static class CookiePathComparator implements Comparator<HttpCookie> {
        CookiePathComparator() {
        }

        @Override
        public int compare(HttpCookie httpCookie, HttpCookie httpCookie2) {
            if (httpCookie == httpCookie2) {
                return 0;
            }
            if (httpCookie == null) {
                return -1;
            }
            if (httpCookie2 == null) {
                return 1;
            }
            if (httpCookie.getName().equals(httpCookie2.getName())) {
                String strNormalizePath = CookieManager.normalizePath(httpCookie.getPath());
                String strNormalizePath2 = CookieManager.normalizePath(httpCookie2.getPath());
                if (strNormalizePath.startsWith(strNormalizePath2)) {
                    return -1;
                }
                if (!strNormalizePath2.startsWith(strNormalizePath)) {
                    return 0;
                }
                return 1;
            }
            return 0;
        }
    }
}
