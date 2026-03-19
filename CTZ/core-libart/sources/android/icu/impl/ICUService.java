package android.icu.impl;

import android.icu.impl.ICURWLock;
import android.icu.util.ULocale;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ICUService extends ICUNotifier {
    private static final boolean DEBUG = ICUDebug.enabled("service");
    private Map<String, CacheEntry> cache;
    private int defaultSize;
    private LocaleRef dnref;
    private final List<Factory> factories;
    private final ICURWLock factoryLock;
    private Map<String, Factory> idcache;
    protected final String name;

    public interface Factory {
        Object create(Key key, ICUService iCUService);

        String getDisplayName(String str, ULocale uLocale);

        void updateVisibleIDs(Map<String, Factory> map);
    }

    public interface ServiceListener extends EventListener {
        void serviceChanged(ICUService iCUService);
    }

    public ICUService() {
        this.factoryLock = new ICURWLock();
        this.factories = new ArrayList();
        this.defaultSize = 0;
        this.name = "";
    }

    public ICUService(String str) {
        this.factoryLock = new ICURWLock();
        this.factories = new ArrayList();
        this.defaultSize = 0;
        this.name = str;
    }

    public static class Key {
        private final String id;

        public Key(String str) {
            this.id = str;
        }

        public final String id() {
            return this.id;
        }

        public String canonicalID() {
            return this.id;
        }

        public String currentID() {
            return canonicalID();
        }

        public String currentDescriptor() {
            return "/" + currentID();
        }

        public boolean fallback() {
            return false;
        }

        public boolean isFallbackOf(String str) {
            return canonicalID().equals(str);
        }
    }

    public static class SimpleFactory implements Factory {
        protected String id;
        protected Object instance;
        protected boolean visible;

        public SimpleFactory(Object obj, String str) {
            this(obj, str, true);
        }

        public SimpleFactory(Object obj, String str, boolean z) {
            if (obj == null || str == null) {
                throw new IllegalArgumentException("Instance or id is null");
            }
            this.instance = obj;
            this.id = str;
            this.visible = z;
        }

        @Override
        public Object create(Key key, ICUService iCUService) {
            if (this.id.equals(key.currentID())) {
                return this.instance;
            }
            return null;
        }

        @Override
        public void updateVisibleIDs(Map<String, Factory> map) {
            if (this.visible) {
                map.put(this.id, this);
            } else {
                map.remove(this.id);
            }
        }

        @Override
        public String getDisplayName(String str, ULocale uLocale) {
            if (this.visible && this.id.equals(str)) {
                return str;
            }
            return null;
        }

        public String toString() {
            return super.toString() + ", id: " + this.id + ", visible: " + this.visible;
        }
    }

    public Object get(String str) {
        return getKey(createKey(str), null);
    }

    public Object get(String str, String[] strArr) {
        if (str == null) {
            throw new NullPointerException("descriptor must not be null");
        }
        return getKey(createKey(str), strArr);
    }

    public Object getKey(Key key) {
        return getKey(key, null);
    }

    public Object getKey(Key key, String[] strArr) {
        return getKey(key, strArr, null);
    }

    public Object getKey(Key key, String[] strArr, Factory factory) {
        ArrayList<String> arrayList;
        boolean z;
        int i;
        int i2;
        CacheEntry cacheEntry;
        if (this.factories.size() == 0) {
            return handleDefault(key, strArr);
        }
        if (DEBUG) {
            System.out.println("Service: " + this.name + " key: " + key.canonicalID());
        }
        if (key != null) {
            try {
                this.factoryLock.acquireRead();
                Map<String, CacheEntry> concurrentHashMap = this.cache;
                if (concurrentHashMap == null) {
                    if (DEBUG) {
                        System.out.println("Service " + this.name + " cache was empty");
                    }
                    concurrentHashMap = new ConcurrentHashMap<>();
                }
                int size = this.factories.size();
                if (factory != null) {
                    int i3 = 0;
                    while (true) {
                        if (i3 < size) {
                            if (factory != this.factories.get(i3)) {
                                i3++;
                            } else {
                                i2 = i3 + 1;
                                break;
                            }
                        } else {
                            i2 = 0;
                            break;
                        }
                    }
                    if (i2 == 0) {
                        throw new IllegalStateException("Factory " + factory + "not registered with service: " + this);
                    }
                    arrayList = null;
                    i = 0;
                    z = false;
                } else {
                    arrayList = null;
                    z = true;
                    i = 0;
                    i2 = 0;
                }
                boolean z2 = false;
                while (true) {
                    String strCurrentDescriptor = key.currentDescriptor();
                    if (DEBUG) {
                        System.out.println(this.name + "[" + i + "] looking for: " + strCurrentDescriptor);
                        i++;
                    }
                    cacheEntry = concurrentHashMap.get(strCurrentDescriptor);
                    if (cacheEntry != null) {
                        if (DEBUG) {
                            System.out.println(this.name + " found with descriptor: " + strCurrentDescriptor);
                        }
                        z = z2;
                    } else {
                        if (DEBUG) {
                            System.out.println("did not find: " + strCurrentDescriptor + " in cache");
                        }
                        int i4 = i2;
                        while (true) {
                            if (i4 >= size) {
                                break;
                            }
                            int i5 = i4 + 1;
                            Factory factory2 = this.factories.get(i4);
                            if (DEBUG) {
                                PrintStream printStream = System.out;
                                StringBuilder sb = new StringBuilder();
                                sb.append("trying factory[");
                                sb.append(i5 - 1);
                                sb.append("] ");
                                sb.append(factory2.toString());
                                printStream.println(sb.toString());
                            }
                            Object objCreate = factory2.create(key, this);
                            if (objCreate != null) {
                                cacheEntry = new CacheEntry(strCurrentDescriptor, objCreate);
                                if (DEBUG) {
                                    System.out.println(this.name + " factory supported: " + strCurrentDescriptor + ", caching");
                                }
                            } else {
                                if (DEBUG) {
                                    System.out.println("factory did not support: " + strCurrentDescriptor);
                                }
                                i4 = i5;
                            }
                        }
                    }
                    z2 = z;
                }
                if (cacheEntry != null) {
                    if (z) {
                        if (DEBUG) {
                            System.out.println("caching '" + cacheEntry.actualDescriptor + "'");
                        }
                        concurrentHashMap.put(cacheEntry.actualDescriptor, cacheEntry);
                        if (arrayList != null) {
                            for (String str : arrayList) {
                                if (DEBUG) {
                                    System.out.println(this.name + " adding descriptor: '" + str + "' for actual: '" + cacheEntry.actualDescriptor + "'");
                                }
                                concurrentHashMap.put(str, cacheEntry);
                            }
                        }
                        this.cache = concurrentHashMap;
                    }
                    if (strArr != null) {
                        if (cacheEntry.actualDescriptor.indexOf("/") == 0) {
                            strArr[0] = cacheEntry.actualDescriptor.substring(1);
                        } else {
                            strArr[0] = cacheEntry.actualDescriptor;
                        }
                    }
                    if (DEBUG) {
                        System.out.println("found in service: " + this.name);
                    }
                    return cacheEntry.service;
                }
            } finally {
                this.factoryLock.releaseRead();
            }
        }
        if (DEBUG) {
            System.out.println("not found in service: " + this.name);
        }
        return handleDefault(key, strArr);
    }

    private static final class CacheEntry {
        final String actualDescriptor;
        final Object service;

        CacheEntry(String str, Object obj) {
            this.actualDescriptor = str;
            this.service = obj;
        }
    }

    protected Object handleDefault(Key key, String[] strArr) {
        return null;
    }

    public Set<String> getVisibleIDs() {
        return getVisibleIDs(null);
    }

    public Set<String> getVisibleIDs(String str) {
        Set<String> setKeySet = getVisibleIDMap().keySet();
        Key keyCreateKey = createKey(str);
        if (keyCreateKey == null) {
            return setKeySet;
        }
        HashSet hashSet = new HashSet(setKeySet.size());
        for (String str2 : setKeySet) {
            if (keyCreateKey.isFallbackOf(str2)) {
                hashSet.add(str2);
            }
        }
        return hashSet;
    }

    private Map<String, Factory> getVisibleIDMap() {
        synchronized (this) {
            if (this.idcache == null) {
                try {
                    this.factoryLock.acquireRead();
                    HashMap map = new HashMap();
                    ListIterator<Factory> listIterator = this.factories.listIterator(this.factories.size());
                    while (listIterator.hasPrevious()) {
                        listIterator.previous().updateVisibleIDs(map);
                    }
                    this.idcache = Collections.unmodifiableMap(map);
                    this.factoryLock.releaseRead();
                } catch (Throwable th) {
                    this.factoryLock.releaseRead();
                    throw th;
                }
            }
        }
        return this.idcache;
    }

    public String getDisplayName(String str) {
        return getDisplayName(str, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getDisplayName(String str, ULocale uLocale) {
        Map<String, Factory> visibleIDMap = getVisibleIDMap();
        Factory factory = visibleIDMap.get(str);
        if (factory != null) {
            return factory.getDisplayName(str, uLocale);
        }
        Key keyCreateKey = createKey(str);
        while (keyCreateKey.fallback()) {
            Factory factory2 = visibleIDMap.get(keyCreateKey.currentID());
            if (factory2 != null) {
                return factory2.getDisplayName(str, uLocale);
            }
        }
        return null;
    }

    public SortedMap<String, String> getDisplayNames() {
        return getDisplayNames(ULocale.getDefault(ULocale.Category.DISPLAY), null, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale uLocale) {
        return getDisplayNames(uLocale, null, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale uLocale, Comparator<Object> comparator) {
        return getDisplayNames(uLocale, comparator, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale uLocale, String str) {
        return getDisplayNames(uLocale, null, str);
    }

    public SortedMap<String, String> getDisplayNames(ULocale uLocale, Comparator<Object> comparator, String str) {
        SortedMap<String, String> sortedMapUnmodifiableSortedMap;
        LocaleRef localeRef = this.dnref;
        if (localeRef != null) {
            sortedMapUnmodifiableSortedMap = localeRef.get(uLocale, comparator);
        } else {
            sortedMapUnmodifiableSortedMap = null;
        }
        while (sortedMapUnmodifiableSortedMap == null) {
            synchronized (this) {
                if (localeRef == this.dnref || this.dnref == null) {
                    TreeMap treeMap = new TreeMap(comparator);
                    for (Map.Entry<String, Factory> entry : getVisibleIDMap().entrySet()) {
                        String key = entry.getKey();
                        treeMap.put(entry.getValue().getDisplayName(key, uLocale), key);
                    }
                    sortedMapUnmodifiableSortedMap = Collections.unmodifiableSortedMap(treeMap);
                    this.dnref = new LocaleRef(sortedMapUnmodifiableSortedMap, uLocale, comparator);
                } else {
                    localeRef = this.dnref;
                    sortedMapUnmodifiableSortedMap = localeRef.get(uLocale, comparator);
                }
            }
        }
        Key keyCreateKey = createKey(str);
        if (keyCreateKey == null) {
            return sortedMapUnmodifiableSortedMap;
        }
        TreeMap treeMap2 = new TreeMap((SortedMap) sortedMapUnmodifiableSortedMap);
        Iterator it = treeMap2.entrySet().iterator();
        while (it.hasNext()) {
            if (!keyCreateKey.isFallbackOf((String) ((Map.Entry) it.next()).getValue())) {
                it.remove();
            }
        }
        return treeMap2;
    }

    private static class LocaleRef {
        private Comparator<Object> com;
        private SortedMap<String, String> dnCache;
        private final ULocale locale;

        LocaleRef(SortedMap<String, String> sortedMap, ULocale uLocale, Comparator<Object> comparator) {
            this.locale = uLocale;
            this.com = comparator;
            this.dnCache = sortedMap;
        }

        SortedMap<String, String> get(ULocale uLocale, Comparator<Object> comparator) {
            SortedMap<String, String> sortedMap = this.dnCache;
            if (sortedMap != null && this.locale.equals(uLocale)) {
                if (this.com == comparator || (this.com != null && this.com.equals(comparator))) {
                    return sortedMap;
                }
                return null;
            }
            return null;
        }
    }

    public final List<Factory> factories() {
        try {
            this.factoryLock.acquireRead();
            return new ArrayList(this.factories);
        } finally {
            this.factoryLock.releaseRead();
        }
    }

    public Factory registerObject(Object obj, String str) {
        return registerObject(obj, str, true);
    }

    public Factory registerObject(Object obj, String str, boolean z) {
        return registerFactory(new SimpleFactory(obj, createKey(str).canonicalID(), z));
    }

    public final Factory registerFactory(Factory factory) {
        if (factory == null) {
            throw new NullPointerException();
        }
        try {
            this.factoryLock.acquireWrite();
            this.factories.add(0, factory);
            clearCaches();
            this.factoryLock.releaseWrite();
            notifyChanged();
            return factory;
        } catch (Throwable th) {
            this.factoryLock.releaseWrite();
            throw th;
        }
    }

    public final boolean unregisterFactory(Factory factory) {
        if (factory == null) {
            throw new NullPointerException();
        }
        boolean z = false;
        try {
            this.factoryLock.acquireWrite();
            if (this.factories.remove(factory)) {
                z = true;
                clearCaches();
            }
            if (z) {
                notifyChanged();
            }
            return z;
        } finally {
            this.factoryLock.releaseWrite();
        }
    }

    public final void reset() {
        try {
            this.factoryLock.acquireWrite();
            reInitializeFactories();
            clearCaches();
            this.factoryLock.releaseWrite();
            notifyChanged();
        } catch (Throwable th) {
            this.factoryLock.releaseWrite();
            throw th;
        }
    }

    protected void reInitializeFactories() {
        this.factories.clear();
    }

    public boolean isDefault() {
        return this.factories.size() == this.defaultSize;
    }

    protected void markDefault() {
        this.defaultSize = this.factories.size();
    }

    public Key createKey(String str) {
        if (str == null) {
            return null;
        }
        return new Key(str);
    }

    protected void clearCaches() {
        this.cache = null;
        this.idcache = null;
        this.dnref = null;
    }

    protected void clearServiceCache() {
        this.cache = null;
    }

    @Override
    protected boolean acceptsListener(EventListener eventListener) {
        return eventListener instanceof ServiceListener;
    }

    @Override
    protected void notifyListener(EventListener eventListener) {
        ((ServiceListener) eventListener).serviceChanged(this);
    }

    public String stats() {
        ICURWLock.Stats statsResetStats = this.factoryLock.resetStats();
        if (statsResetStats != null) {
            return statsResetStats.toString();
        }
        return "no stats";
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return super.toString() + "{" + this.name + "}";
    }
}
