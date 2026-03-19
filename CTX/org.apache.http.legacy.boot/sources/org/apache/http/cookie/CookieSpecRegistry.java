package org.apache.http.cookie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.http.params.HttpParams;

@Deprecated
public final class CookieSpecRegistry {
    private final Map<String, CookieSpecFactory> registeredSpecs = new LinkedHashMap();

    public synchronized void register(String str, CookieSpecFactory cookieSpecFactory) {
        if (str == null) {
            throw new IllegalArgumentException("Name may not be null");
        }
        if (cookieSpecFactory == null) {
            throw new IllegalArgumentException("Cookie spec factory may not be null");
        }
        this.registeredSpecs.put(str.toLowerCase(Locale.ENGLISH), cookieSpecFactory);
    }

    public synchronized void unregister(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Id may not be null");
        }
        this.registeredSpecs.remove(str.toLowerCase(Locale.ENGLISH));
    }

    public synchronized CookieSpec getCookieSpec(String str, HttpParams httpParams) throws IllegalStateException {
        CookieSpecFactory cookieSpecFactory;
        if (str == null) {
            throw new IllegalArgumentException("Name may not be null");
        }
        cookieSpecFactory = this.registeredSpecs.get(str.toLowerCase(Locale.ENGLISH));
        if (cookieSpecFactory != null) {
        } else {
            throw new IllegalStateException("Unsupported cookie spec: " + str);
        }
        return cookieSpecFactory.newInstance(httpParams);
    }

    public synchronized CookieSpec getCookieSpec(String str) throws IllegalStateException {
        return getCookieSpec(str, null);
    }

    public synchronized List<String> getSpecNames() {
        return new ArrayList(this.registeredSpecs.keySet());
    }

    public synchronized void setItems(Map<String, CookieSpecFactory> map) {
        if (map == null) {
            return;
        }
        this.registeredSpecs.clear();
        this.registeredSpecs.putAll(map);
    }
}
