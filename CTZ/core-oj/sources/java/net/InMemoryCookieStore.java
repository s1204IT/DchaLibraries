package java.net;

import dalvik.system.VMRuntime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class InMemoryCookieStore implements CookieStore {
    private final boolean applyMCompatibility;
    private ReentrantLock lock;
    private Map<URI, List<HttpCookie>> uriIndex;

    public InMemoryCookieStore() {
        this(VMRuntime.getRuntime().getTargetSdkVersion());
    }

    public InMemoryCookieStore(int i) {
        this.uriIndex = null;
        this.lock = null;
        this.uriIndex = new HashMap();
        this.lock = new ReentrantLock(false);
        this.applyMCompatibility = i <= 23;
    }

    @Override
    public void add(URI uri, HttpCookie httpCookie) {
        if (httpCookie == null) {
            throw new NullPointerException("cookie is null");
        }
        this.lock.lock();
        try {
            addIndex(this.uriIndex, getEffectiveURI(uri), httpCookie);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri is null");
        }
        ArrayList arrayList = new ArrayList();
        this.lock.lock();
        try {
            getInternal1(arrayList, this.uriIndex, uri.getHost());
            getInternal2(arrayList, this.uriIndex, getEffectiveURI(uri));
            return arrayList;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public List<HttpCookie> getCookies() {
        ArrayList arrayList = new ArrayList();
        this.lock.lock();
        try {
            Iterator<List<HttpCookie>> it = this.uriIndex.values().iterator();
            while (it.hasNext()) {
                Iterator<HttpCookie> it2 = it.next().iterator();
                while (it2.hasNext()) {
                    HttpCookie next = it2.next();
                    if (next.hasExpired()) {
                        it2.remove();
                    } else if (!arrayList.contains(next)) {
                        arrayList.add(next);
                    }
                }
            }
            return Collections.unmodifiableList(arrayList);
        } finally {
            Collections.unmodifiableList(arrayList);
            this.lock.unlock();
        }
    }

    @Override
    public List<URI> getURIs() {
        ArrayList arrayList = new ArrayList();
        this.lock.lock();
        try {
            ArrayList arrayList2 = new ArrayList(this.uriIndex.keySet());
            arrayList2.remove((Object) null);
            return Collections.unmodifiableList(arrayList2);
        } finally {
            arrayList.addAll(this.uriIndex.keySet());
            this.lock.unlock();
        }
    }

    @Override
    public boolean remove(URI uri, HttpCookie httpCookie) {
        if (httpCookie == null) {
            throw new NullPointerException("cookie is null");
        }
        this.lock.lock();
        try {
            URI effectiveURI = getEffectiveURI(uri);
            if (this.uriIndex.get(effectiveURI) == null) {
                return false;
            }
            List<HttpCookie> list = this.uriIndex.get(effectiveURI);
            if (list != null) {
                return list.remove(httpCookie);
            }
            return false;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean removeAll() {
        this.lock.lock();
        try {
            boolean z = !this.uriIndex.isEmpty();
            this.uriIndex.clear();
            return z;
        } finally {
            this.lock.unlock();
        }
    }

    private boolean netscapeDomainMatches(String str, String str2) {
        if (str == null || str2 == null) {
            return false;
        }
        boolean zEqualsIgnoreCase = ".local".equalsIgnoreCase(str);
        int iIndexOf = str.indexOf(46);
        if (iIndexOf == 0) {
            iIndexOf = str.indexOf(46, 1);
        }
        if (!zEqualsIgnoreCase && (iIndexOf == -1 || iIndexOf == str.length() - 1)) {
            return false;
        }
        if (str2.indexOf(46) == -1 && zEqualsIgnoreCase) {
            return true;
        }
        int length = str2.length() - str.length();
        if (length == 0) {
            return str2.equalsIgnoreCase(str);
        }
        if (length > 0) {
            String strSubstring = str2.substring(length);
            if (this.applyMCompatibility && !str.startsWith(".")) {
                return false;
            }
            return strSubstring.equalsIgnoreCase(str);
        }
        if (length != -1 || str.charAt(0) != '.' || !str2.equalsIgnoreCase(str.substring(1))) {
            return false;
        }
        return true;
    }

    private void getInternal1(List<HttpCookie> list, Map<URI, List<HttpCookie>> map, String str) {
        ArrayList arrayList = new ArrayList();
        Iterator<Map.Entry<URI, List<HttpCookie>>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            List<HttpCookie> value = it.next().getValue();
            for (HttpCookie httpCookie : value) {
                String domain = httpCookie.getDomain();
                if ((httpCookie.getVersion() == 0 && netscapeDomainMatches(domain, str)) || (httpCookie.getVersion() == 1 && HttpCookie.domainMatches(domain, str))) {
                    if (!httpCookie.hasExpired()) {
                        if (!list.contains(httpCookie)) {
                            list.add(httpCookie);
                        }
                    } else {
                        arrayList.add(httpCookie);
                    }
                }
            }
            Iterator it2 = arrayList.iterator();
            while (it2.hasNext()) {
                value.remove((HttpCookie) it2.next());
            }
            arrayList.clear();
        }
    }

    private <T extends Comparable<T>> void getInternal2(List<HttpCookie> list, Map<T, List<HttpCookie>> map, T t) {
        for (T t2 : map.keySet()) {
            if (t2 == t || (t2 != null && t.compareTo(t2) == 0)) {
                List<HttpCookie> list2 = map.get(t2);
                if (list2 != null) {
                    Iterator<HttpCookie> it = list2.iterator();
                    while (it.hasNext()) {
                        HttpCookie next = it.next();
                        if (!next.hasExpired()) {
                            if (!list.contains(next)) {
                                list.add(next);
                            }
                        } else {
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    private <T> void addIndex(Map<T, List<HttpCookie>> map, T t, HttpCookie httpCookie) {
        List<HttpCookie> list = map.get(t);
        if (list != null) {
            list.remove(httpCookie);
            list.add(httpCookie);
        } else {
            ArrayList arrayList = new ArrayList();
            arrayList.add(httpCookie);
            map.put(t, arrayList);
        }
    }

    private URI getEffectiveURI(URI uri) {
        if (uri == null) {
            return null;
        }
        try {
            return new URI("http", uri.getHost(), null, null, null);
        } catch (URISyntaxException e) {
            return uri;
        }
    }
}
