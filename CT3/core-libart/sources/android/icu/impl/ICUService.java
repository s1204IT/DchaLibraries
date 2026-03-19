package android.icu.impl;

import android.icu.impl.ICURWLock;
import android.icu.util.ULocale;
import java.lang.ref.SoftReference;
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

public class ICUService extends ICUNotifier {
    private static final boolean DEBUG = ICUDebug.enabled("service");
    private SoftReference<Map<String, CacheEntry>> cacheref;
    private int defaultSize;
    private LocaleRef dnref;
    private final List<Factory> factories;
    private final ICURWLock factoryLock;
    private SoftReference<Map<String, Factory>> idref;
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

    public ICUService(String name) {
        this.factoryLock = new ICURWLock();
        this.factories = new ArrayList();
        this.defaultSize = 0;
        this.name = name;
    }

    public static class Key {
        private final String id;

        public Key(String id) {
            this.id = id;
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

        public boolean isFallbackOf(String idToCheck) {
            return canonicalID().equals(idToCheck);
        }
    }

    public static class SimpleFactory implements Factory {
        protected String id;
        protected Object instance;
        protected boolean visible;

        public SimpleFactory(Object instance, String id) {
            this(instance, id, true);
        }

        public SimpleFactory(Object instance, String id, boolean visible) {
            if (instance == null || id == null) {
                throw new IllegalArgumentException("Instance or id is null");
            }
            this.instance = instance;
            this.id = id;
            this.visible = visible;
        }

        @Override
        public Object create(Key key, ICUService service) {
            if (this.id.equals(key.currentID())) {
                return this.instance;
            }
            return null;
        }

        @Override
        public void updateVisibleIDs(Map<String, Factory> result) {
            if (this.visible) {
                result.put(this.id, this);
            } else {
                result.remove(this.id);
            }
        }

        @Override
        public String getDisplayName(String identifier, ULocale locale) {
            if (this.visible && this.id.equals(identifier)) {
                return identifier;
            }
            return null;
        }

        public String toString() {
            return super.toString() + ", id: " + this.id + ", visible: " + this.visible;
        }
    }

    public Object get(String descriptor) {
        return getKey(createKey(descriptor), null);
    }

    public Object get(String descriptor, String[] actualReturn) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor must not be null");
        }
        return getKey(createKey(descriptor), actualReturn);
    }

    public Object getKey(Key key) {
        return getKey(key, null);
    }

    public Object getKey(Key key, String[] actualReturn) {
        return getKey(key, actualReturn, null);
    }

    public Object getKey(Key key, String[] actualReturn, Factory factory) throws Throwable {
        int NDebug;
        int NDebug2;
        CacheEntry result;
        if (this.factories.size() == 0) {
            return handleDefault(key, actualReturn);
        }
        if (DEBUG) {
            System.out.println("Service: " + this.name + " key: " + key.canonicalID());
        }
        if (key != null) {
            try {
                this.factoryLock.acquireRead();
                Map<String, CacheEntry> cache = null;
                SoftReference<Map<String, CacheEntry>> cref = this.cacheref;
                if (cref != null) {
                    if (DEBUG) {
                        System.out.println("Service " + this.name + " ref exists");
                    }
                    Map<String, CacheEntry> cache2 = cref.get();
                    cache = cache2;
                }
                if (cache == null) {
                    if (DEBUG) {
                        System.out.println("Service " + this.name + " cache was empty");
                    }
                    cache = Collections.synchronizedMap(new HashMap());
                    cref = new SoftReference<>(cache);
                }
                ArrayList<String> cacheDescriptorList = null;
                boolean putInCache = false;
                int startIndex = 0;
                int limit = this.factories.size();
                boolean cacheResult = true;
                if (factory != null) {
                    int i = 0;
                    while (true) {
                        if (i >= limit) {
                            break;
                        }
                        if (factory != this.factories.get(i)) {
                            i++;
                        } else {
                            startIndex = i + 1;
                            break;
                        }
                    }
                    if (startIndex == 0) {
                        throw new IllegalStateException("Factory " + factory + "not registered with service: " + this);
                    }
                    cacheResult = false;
                    NDebug = 0;
                    loop1: while (true) {
                        String currentDescriptor = key.currentDescriptor();
                        if (DEBUG) {
                            NDebug2 = NDebug + 1;
                            System.out.println(this.name + "[" + NDebug + "] looking for: " + currentDescriptor);
                        } else {
                            NDebug2 = NDebug;
                        }
                        result = cache.get(currentDescriptor);
                        if (result != null) {
                            if (DEBUG) {
                                System.out.println(this.name + " found with descriptor: " + currentDescriptor);
                            }
                        } else {
                            if (DEBUG) {
                                System.out.println("did not find: " + currentDescriptor + " in cache");
                            }
                            putInCache = cacheResult;
                            int index = startIndex;
                            int index2 = index;
                            while (true) {
                                if (index2 >= limit) {
                                    break;
                                }
                                int index3 = index2 + 1;
                                Factory f = this.factories.get(index2);
                                if (DEBUG) {
                                    System.out.println("trying factory[" + (index3 - 1) + "] " + f.toString());
                                }
                                Object service = f.create(key, this);
                                if (service != null) {
                                    break loop1;
                                }
                                if (DEBUG) {
                                    System.out.println("factory did not support: " + currentDescriptor);
                                }
                                index2 = index3;
                            }
                        }
                        NDebug = NDebug2;
                    }
                    if (result != null) {
                        if (putInCache) {
                            if (DEBUG) {
                                System.out.println("caching '" + result.actualDescriptor + "'");
                            }
                            cache.put(result.actualDescriptor, result);
                            if (cacheDescriptorList != null) {
                                for (String desc : cacheDescriptorList) {
                                    if (DEBUG) {
                                        System.out.println(this.name + " adding descriptor: '" + desc + "' for actual: '" + result.actualDescriptor + "'");
                                    }
                                    cache.put(desc, result);
                                }
                            }
                            this.cacheref = cref;
                        }
                        if (actualReturn != null) {
                            if (result.actualDescriptor.indexOf("/") == 0) {
                                actualReturn[0] = result.actualDescriptor.substring(1);
                            } else {
                                actualReturn[0] = result.actualDescriptor;
                            }
                        }
                        if (DEBUG) {
                            System.out.println("found in service: " + this.name);
                        }
                        Object obj = result.service;
                        this.factoryLock.releaseRead();
                        return obj;
                    }
                    this.factoryLock.releaseRead();
                }
                NDebug = NDebug2;
            } catch (Throwable th) {
                th = th;
            }
        }
        if (DEBUG) {
            System.out.println("not found in service: " + this.name);
        }
        return handleDefault(key, actualReturn);
    }

    private static final class CacheEntry {
        final String actualDescriptor;
        final Object service;

        CacheEntry(String actualDescriptor, Object service) {
            this.actualDescriptor = actualDescriptor;
            this.service = service;
        }
    }

    protected Object handleDefault(Key key, String[] actualIDReturn) {
        return null;
    }

    public Set<String> getVisibleIDs() {
        return getVisibleIDs(null);
    }

    public Set<String> getVisibleIDs(String matchID) {
        Set<String> result = getVisibleIDMap().keySet();
        Key fallbackKey = createKey(matchID);
        if (fallbackKey != null) {
            Set<String> temp = new HashSet<>(result.size());
            for (String id : result) {
                if (fallbackKey.isFallbackOf(id)) {
                    temp.add(id);
                }
            }
            return temp;
        }
        return result;
    }

    private Map<String, Factory> getVisibleIDMap() throws Throwable {
        Map<String, Factory> idcache;
        Map<String, Factory> idcache2;
        Map<String, Factory> idcache3;
        SoftReference<Map<String, Factory>> ref = this.idref;
        if (ref != null) {
            Map<String, Factory> idcache4 = ref.get();
            idcache = idcache4;
            while (idcache == null) {
                synchronized (this) {
                    try {
                        if (ref == this.idref || this.idref == null) {
                            try {
                                this.factoryLock.acquireRead();
                                idcache2 = new HashMap<>();
                            } catch (Throwable th) {
                                th = th;
                            }
                            try {
                                ListIterator<Factory> lIter = this.factories.listIterator(this.factories.size());
                                while (lIter.hasPrevious()) {
                                    Factory f = lIter.previous();
                                    f.updateVisibleIDs(idcache2);
                                }
                                idcache3 = Collections.unmodifiableMap(idcache2);
                                this.idref = new SoftReference<>(idcache3);
                                try {
                                    this.factoryLock.releaseRead();
                                } catch (Throwable th2) {
                                    th = th2;
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                this.factoryLock.releaseRead();
                                throw th;
                            }
                        } else {
                            ref = this.idref;
                            idcache3 = ref.get();
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
            }
            return idcache;
        }
        idcache = idcache3;
    }

    public String getDisplayName(String id) {
        return getDisplayName(id, ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getDisplayName(String id, ULocale locale) throws Throwable {
        Map<String, Factory> m = getVisibleIDMap();
        Factory f = m.get(id);
        if (f != null) {
            return f.getDisplayName(id, locale);
        }
        Key key = createKey(id);
        while (key.fallback()) {
            Factory f2 = m.get(key.currentID());
            if (f2 != null) {
                return f2.getDisplayName(id, locale);
            }
        }
        return null;
    }

    public SortedMap<String, String> getDisplayNames() {
        ULocale locale = ULocale.getDefault(ULocale.Category.DISPLAY);
        return getDisplayNames(locale, null, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale locale) {
        return getDisplayNames(locale, null, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale locale, Comparator<Object> com2) {
        return getDisplayNames(locale, com2, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale locale, String matchID) {
        return getDisplayNames(locale, null, matchID);
    }

    public SortedMap<String, String> getDisplayNames(ULocale locale, Comparator<Object> com2, String matchID) throws Throwable {
        SortedMap<String, String> dncache;
        SortedMap<String, String> dncache2;
        LocaleRef ref = this.dnref;
        if (ref != null) {
            SortedMap<String, String> dncache3 = ref.get(locale, com2);
            dncache = dncache3;
            while (dncache == null) {
                synchronized (this) {
                    try {
                        if (ref == this.dnref || this.dnref == null) {
                            SortedMap<String, String> dncache4 = new TreeMap<>((Comparator<? super String>) com2);
                            try {
                                Map<String, Factory> m = getVisibleIDMap();
                                for (Map.Entry<String, Factory> e : m.entrySet()) {
                                    String id = e.getKey();
                                    Factory f = e.getValue();
                                    dncache4.put(f.getDisplayName(id, locale), id);
                                }
                                dncache2 = Collections.unmodifiableSortedMap(dncache4);
                                this.dnref = new LocaleRef(dncache2, locale, com2);
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } else {
                            ref = this.dnref;
                            dncache2 = ref.get(locale, com2);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }
            Key matchKey = createKey(matchID);
            if (matchKey == null) {
                return dncache;
            }
            SortedMap<String, String> result = new TreeMap<>((SortedMap<String, ? extends String>) dncache);
            Iterator<Map.Entry<String, String>> iter = result.entrySet().iterator();
            while (iter.hasNext()) {
                if (!matchKey.isFallbackOf(iter.next().getValue())) {
                    iter.remove();
                }
            }
            return result;
        }
        dncache = dncache2;
    }

    private static class LocaleRef {

        private Comparator<Object> f127com;
        private final ULocale locale;
        private SoftReference<SortedMap<String, String>> ref;

        LocaleRef(SortedMap<String, String> dnCache, ULocale locale, Comparator<Object> com2) {
            this.locale = locale;
            this.f127com = com2;
            this.ref = new SoftReference<>(dnCache);
        }

        SortedMap<String, String> get(ULocale loc, Comparator<Object> comp) {
            SortedMap<String, String> m = this.ref.get();
            if (m == null || !this.locale.equals(loc) || (this.f127com != comp && (this.f127com == null || !this.f127com.equals(comp)))) {
                return null;
            }
            return m;
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

    public Factory registerObject(Object obj, String id) {
        return registerObject(obj, id, true);
    }

    public Factory registerObject(Object obj, String id, boolean visible) {
        String canonicalID = createKey(id).canonicalID();
        return registerFactory(new SimpleFactory(obj, canonicalID, visible));
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
        boolean result = false;
        try {
            this.factoryLock.acquireWrite();
            if (this.factories.remove(factory)) {
                result = true;
                clearCaches();
            }
            if (result) {
                notifyChanged();
            }
            return result;
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

    public Key createKey(String id) {
        if (id == null) {
            return null;
        }
        return new Key(id);
    }

    protected void clearCaches() {
        this.cacheref = null;
        this.idref = null;
        this.dnref = null;
    }

    protected void clearServiceCache() {
        this.cacheref = null;
    }

    @Override
    protected boolean acceptsListener(EventListener l) {
        return l instanceof ServiceListener;
    }

    @Override
    protected void notifyListener(EventListener l) {
        ((ServiceListener) l).serviceChanged(this);
    }

    public String stats() {
        ICURWLock.Stats stats = this.factoryLock.resetStats();
        if (stats != null) {
            return stats.toString();
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
