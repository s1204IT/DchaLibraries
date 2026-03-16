package java.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CookieManager extends CookieHandler {
    private static final String VERSION_ONE_HEADER = "Set-cookie2";
    private static final String VERSION_ZERO_HEADER = "Set-cookie";
    private CookiePolicy policy;
    private CookieStore store;

    public CookieManager() {
        this(null, null);
    }

    public CookieManager(CookieStore store, CookiePolicy cookiePolicy) {
        this.store = store == null ? new CookieStoreImpl() : store;
        this.policy = cookiePolicy == null ? CookiePolicy.ACCEPT_ORIGINAL_SERVER : cookiePolicy;
    }

    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
        if (uri == null || requestHeaders == null) {
            throw new IllegalArgumentException();
        }
        List<HttpCookie> result = new ArrayList<>();
        for (HttpCookie cookie : this.store.get(uri)) {
            if (HttpCookie.pathMatches(cookie, uri) && HttpCookie.secureMatches(cookie, uri) && HttpCookie.portMatches(cookie, uri)) {
                result.add(cookie);
            }
        }
        return cookiesToHeaders(result);
    }

    private static Map<String, List<String>> cookiesToHeaders(List<HttpCookie> cookies) {
        if (cookies.isEmpty()) {
            return Collections.emptyMap();
        }
        StringBuilder result = new StringBuilder();
        int minVersion = 1;
        for (HttpCookie cookie : cookies) {
            minVersion = Math.min(minVersion, cookie.getVersion());
        }
        if (minVersion == 1) {
            result.append("$Version=\"1\"; ");
        }
        result.append(cookies.get(0).toString());
        for (int i = 1; i < cookies.size(); i++) {
            result.append("; ").append(cookies.get(i).toString());
        }
        return Collections.singletonMap("Cookie", Collections.singletonList(result.toString()));
    }

    @Override
    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        if (uri == null || responseHeaders == null) {
            throw new IllegalArgumentException();
        }
        List<HttpCookie> cookies = parseCookie(responseHeaders);
        for (HttpCookie cookie : cookies) {
            if (cookie.getDomain() == null) {
                cookie.setDomain(uri.getHost());
            }
            if (cookie.getPath() == null) {
                cookie.setPath(pathToCookiePath(uri.getPath()));
            } else if (!HttpCookie.pathMatches(cookie, uri)) {
            }
            if ("".equals(cookie.getPortlist())) {
                cookie.setPortlist(Integer.toString(uri.getEffectivePort()));
            } else if (cookie.getPortlist() == null || HttpCookie.portMatches(cookie, uri)) {
            }
            if (this.policy.shouldAccept(uri, cookie)) {
                this.store.add(uri, cookie);
            }
        }
    }

    static String pathToCookiePath(String path) {
        if (path == null) {
            return "/";
        }
        int lastSlash = path.lastIndexOf(47);
        return path.substring(0, lastSlash + 1);
    }

    private static List<HttpCookie> parseCookie(Map<String, List<String>> responseHeaders) {
        List<HttpCookie> cookies = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
            String key = entry.getKey();
            if (key != null && (key.equalsIgnoreCase(VERSION_ZERO_HEADER) || key.equalsIgnoreCase(VERSION_ONE_HEADER))) {
                for (String cookieStr : entry.getValue()) {
                    try {
                        for (HttpCookie cookie : HttpCookie.parse(cookieStr)) {
                            cookies.add(cookie);
                        }
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
        }
        return cookies;
    }

    public void setCookiePolicy(CookiePolicy cookiePolicy) {
        if (cookiePolicy != null) {
            this.policy = cookiePolicy;
        }
    }

    public CookieStore getCookieStore() {
        return this.store;
    }
}
