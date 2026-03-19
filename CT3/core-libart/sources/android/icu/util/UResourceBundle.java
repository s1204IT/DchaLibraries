package android.icu.util;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUResourceBundleReader;
import android.icu.impl.ResourceBundleWrapper;
import android.icu.impl.SimpleCache;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public abstract class UResourceBundle extends ResourceBundle {
    public static final int ARRAY = 8;
    public static final int BINARY = 1;
    public static final int INT = 7;
    public static final int INT_VECTOR = 14;
    public static final int NONE = -1;
    private static final int ROOT_ICU = 1;
    private static final int ROOT_JAVA = 2;
    private static final int ROOT_MISSING = 0;
    public static final int STRING = 0;
    public static final int TABLE = 2;
    private static ICUCache<ResourceCacheKey, UResourceBundle> BUNDLE_CACHE = new SimpleCache();
    private static final ResourceCacheKey cacheKey = new ResourceCacheKey(null);
    private static SoftReference<ConcurrentHashMap<String, Integer>> ROOT_CACHE = new SoftReference<>(new ConcurrentHashMap());

    protected abstract String getBaseName();

    protected abstract String getLocaleID();

    protected abstract UResourceBundle getParent();

    public abstract ULocale getULocale();

    @Deprecated
    protected abstract void setLoadingStatus(int i);

    public static UResourceBundle getBundleInstance(String baseName, String localeName) {
        return getBundleInstance(baseName, localeName, ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root) {
        return getBundleInstance(baseName, localeName, root, false);
    }

    protected static UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root, boolean disableFallback) {
        return instantiateBundle(baseName, localeName, root, disableFallback);
    }

    public static UResourceBundle getBundleInstance(ULocale locale) {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return getBundleInstance("android/icu/impl/data/icudt56b", locale.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String baseName) {
        if (baseName == null) {
            baseName = "android/icu/impl/data/icudt56b";
        }
        ULocale uloc = ULocale.getDefault();
        return getBundleInstance(baseName, uloc.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String baseName, Locale locale) {
        if (baseName == null) {
            baseName = "android/icu/impl/data/icudt56b";
        }
        ULocale uloc = locale == null ? ULocale.getDefault() : ULocale.forLocale(locale);
        return getBundleInstance(baseName, uloc.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String baseName, ULocale locale) {
        if (baseName == null) {
            baseName = "android/icu/impl/data/icudt56b";
        }
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return getBundleInstance(baseName, locale.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String baseName, Locale locale, ClassLoader loader) {
        if (baseName == null) {
            baseName = "android/icu/impl/data/icudt56b";
        }
        ULocale uloc = locale == null ? ULocale.getDefault() : ULocale.forLocale(locale);
        return getBundleInstance(baseName, uloc.toString(), loader, false);
    }

    public static UResourceBundle getBundleInstance(String baseName, ULocale locale, ClassLoader loader) {
        if (baseName == null) {
            baseName = "android/icu/impl/data/icudt56b";
        }
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return getBundleInstance(baseName, locale.toString(), loader, false);
    }

    @Override
    public Locale getLocale() {
        return getULocale().toLocale();
    }

    @Deprecated
    public static void resetBundleCache() {
        BUNDLE_CACHE = new SimpleCache();
    }

    @Deprecated
    protected static UResourceBundle addToCache(String fullName, ULocale defaultLocale, UResourceBundle b) {
        synchronized (cacheKey) {
            cacheKey.setKeyValues(fullName, defaultLocale);
            UResourceBundle cachedBundle = BUNDLE_CACHE.get(cacheKey);
            if (cachedBundle != null) {
                return cachedBundle;
            }
            BUNDLE_CACHE.put((ResourceCacheKey) cacheKey.clone(), b);
            return b;
        }
    }

    @Deprecated
    protected static UResourceBundle loadFromCache(String fullName, ULocale defaultLocale) {
        UResourceBundle uResourceBundle;
        synchronized (cacheKey) {
            cacheKey.setKeyValues(fullName, defaultLocale);
            uResourceBundle = BUNDLE_CACHE.get(cacheKey);
        }
        return uResourceBundle;
    }

    private static final class ResourceCacheKey implements Cloneable {
        private ULocale defaultLocale;
        private int hashCodeCache;
        private String searchName;

        ResourceCacheKey(ResourceCacheKey resourceCacheKey) {
            this();
        }

        private ResourceCacheKey() {
        }

        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (this == other) {
                return true;
            }
            try {
                ResourceCacheKey otherEntry = (ResourceCacheKey) other;
                if (this.hashCodeCache != otherEntry.hashCodeCache || !this.searchName.equals(otherEntry.searchName)) {
                    return false;
                }
                if (this.defaultLocale == null) {
                    if (otherEntry.defaultLocale != null) {
                        return false;
                    }
                } else if (!this.defaultLocale.equals(otherEntry.defaultLocale)) {
                    return false;
                }
                return true;
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e2) {
                return false;
            }
        }

        public int hashCode() {
            return this.hashCodeCache;
        }

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new ICUCloneNotSupportedException(e);
            }
        }

        private synchronized void setKeyValues(String searchName, ULocale defaultLocale) {
            this.searchName = searchName;
            this.hashCodeCache = searchName.hashCode();
            this.defaultLocale = defaultLocale;
            if (defaultLocale != null) {
                this.hashCodeCache ^= defaultLocale.hashCode();
            }
        }
    }

    private static int getRootType(String baseName, ClassLoader root) throws Throwable {
        ConcurrentHashMap<String, Integer> m = ROOT_CACHE.get();
        if (m == null) {
            synchronized (UResourceBundle.class) {
                try {
                    m = ROOT_CACHE.get();
                    if (m == null) {
                        ConcurrentHashMap<String, Integer> m2 = new ConcurrentHashMap<>();
                        try {
                            ROOT_CACHE = new SoftReference<>(m2);
                            m = m2;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
        Integer rootType = m.get(baseName);
        if (rootType == null) {
            String rootLocale = baseName.indexOf(46) == -1 ? "root" : "";
            int rt = 0;
            try {
                ICUResourceBundle.getBundleInstance(baseName, rootLocale, root, true);
                rt = 1;
            } catch (MissingResourceException e) {
                try {
                    ResourceBundleWrapper.getBundleInstance(baseName, rootLocale, root, true);
                    rt = 2;
                } catch (MissingResourceException e2) {
                }
            }
            rootType = Integer.valueOf(rt);
            m.putIfAbsent(baseName, rootType);
        }
        return rootType.intValue();
    }

    private static void setRootType(String baseName, int rootType) throws Throwable {
        Integer rt = Integer.valueOf(rootType);
        ConcurrentHashMap<String, Integer> m = ROOT_CACHE.get();
        if (m == null) {
            synchronized (UResourceBundle.class) {
                try {
                    m = ROOT_CACHE.get();
                    if (m == null) {
                        ConcurrentHashMap<String, Integer> m2 = new ConcurrentHashMap<>();
                        try {
                            ROOT_CACHE = new SoftReference<>(m2);
                            m = m2;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
        m.put(baseName, rt);
    }

    protected static UResourceBundle instantiateBundle(String baseName, String localeName, ClassLoader root, boolean disableFallback) throws Throwable {
        int rootType = getRootType(baseName, root);
        ULocale defaultLocale = ULocale.getDefault();
        switch (rootType) {
            case 1:
                if (disableFallback) {
                    String fullName = ICUResourceBundleReader.getFullName(baseName, localeName);
                    UResourceBundle b = loadFromCache(fullName, defaultLocale);
                    if (b == null) {
                        UResourceBundle b2 = ICUResourceBundle.getBundleInstance(baseName, localeName, root, disableFallback);
                        return b2;
                    }
                    return b;
                }
                UResourceBundle b3 = ICUResourceBundle.getBundleInstance(baseName, localeName, root, disableFallback);
                return b3;
            case 2:
                return ResourceBundleWrapper.getBundleInstance(baseName, localeName, root, disableFallback);
            default:
                try {
                    UResourceBundle b4 = ICUResourceBundle.getBundleInstance(baseName, localeName, root, disableFallback);
                    setRootType(baseName, 1);
                    return b4;
                } catch (MissingResourceException e) {
                    UResourceBundle b5 = ResourceBundleWrapper.getBundleInstance(baseName, localeName, root, disableFallback);
                    setRootType(baseName, 2);
                    return b5;
                }
        }
    }

    public ByteBuffer getBinary() {
        throw new UResourceTypeMismatchException("");
    }

    public String getString() {
        throw new UResourceTypeMismatchException("");
    }

    public String[] getStringArray() {
        throw new UResourceTypeMismatchException("");
    }

    public byte[] getBinary(byte[] ba) {
        throw new UResourceTypeMismatchException("");
    }

    public int[] getIntVector() {
        throw new UResourceTypeMismatchException("");
    }

    public int getInt() {
        throw new UResourceTypeMismatchException("");
    }

    public int getUInt() {
        throw new UResourceTypeMismatchException("");
    }

    public UResourceBundle get(String aKey) {
        UResourceBundle obj = findTopLevel(aKey);
        if (obj == null) {
            String fullName = ICUResourceBundleReader.getFullName(getBaseName(), getLocaleID());
            throw new MissingResourceException("Can't find resource for bundle " + fullName + ", key " + aKey, getClass().getName(), aKey);
        }
        return obj;
    }

    @Deprecated
    protected UResourceBundle findTopLevel(String aKey) {
        for (UResourceBundle res = this; res != null; res = res.getParent()) {
            UResourceBundle obj = res.handleGet(aKey, (HashMap<String, String>) null, this);
            if (obj != null) {
                ((ICUResourceBundle) obj).setLoadingStatus(getLocaleID());
                return obj;
            }
        }
        return null;
    }

    public String getString(int index) {
        ICUResourceBundle temp = (ICUResourceBundle) get(index);
        if (temp.getType() == 0) {
            return temp.getString();
        }
        throw new UResourceTypeMismatchException("");
    }

    public UResourceBundle get(int index) {
        UResourceBundle obj = handleGet(index, (HashMap<String, String>) null, this);
        if (obj == null) {
            obj = (ICUResourceBundle) getParent();
            if (obj != null) {
                obj = obj.get(index);
            }
            if (obj == null) {
                throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getKey(), getClass().getName(), getKey());
            }
        }
        ((ICUResourceBundle) obj).setLoadingStatus(getLocaleID());
        return obj;
    }

    @Deprecated
    protected UResourceBundle findTopLevel(int index) {
        for (UResourceBundle res = this; res != null; res = res.getParent()) {
            UResourceBundle obj = res.handleGet(index, (HashMap<String, String>) null, this);
            if (obj != null) {
                ((ICUResourceBundle) obj).setLoadingStatus(getLocaleID());
                return obj;
            }
        }
        return null;
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(keySet());
    }

    @Override
    @Deprecated
    public Set<String> keySet() {
        TreeSet<String> newKeySet;
        Set<String> keys = null;
        ICUResourceBundle icurb = null;
        if (isTopLevelResource() && (this instanceof ICUResourceBundle)) {
            icurb = (ICUResourceBundle) this;
            keys = icurb.getTopLevelKeySet();
        }
        if (keys == null) {
            if (isTopLevelResource()) {
                if (this.parent == null) {
                    newKeySet = new TreeSet<>();
                } else if (this.parent instanceof UResourceBundle) {
                    newKeySet = new TreeSet<>(((UResourceBundle) this.parent).keySet());
                } else {
                    newKeySet = new TreeSet<>();
                    Enumeration<String> parentKeys = this.parent.getKeys();
                    while (parentKeys.hasMoreElements()) {
                        newKeySet.add(parentKeys.nextElement());
                    }
                }
                newKeySet.addAll(handleKeySet());
                keys = Collections.unmodifiableSet(newKeySet);
                if (icurb != null) {
                    icurb.setTopLevelKeySet(keys);
                }
            } else {
                return handleKeySet();
            }
        }
        return keys;
    }

    @Override
    @Deprecated
    protected Set<String> handleKeySet() {
        return Collections.emptySet();
    }

    public int getSize() {
        return 1;
    }

    public int getType() {
        return -1;
    }

    public VersionInfo getVersion() {
        return null;
    }

    public UResourceBundleIterator getIterator() {
        return new UResourceBundleIterator(this);
    }

    public String getKey() {
        return null;
    }

    protected UResourceBundle handleGet(String aKey, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        return null;
    }

    protected UResourceBundle handleGet(int index, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        return null;
    }

    protected String[] handleGetStringArray() {
        return null;
    }

    protected Enumeration<String> handleGetKeys() {
        return null;
    }

    @Override
    protected Object handleGetObject(String aKey) {
        return handleGetObjectImpl(aKey, this);
    }

    private Object handleGetObjectImpl(String aKey, UResourceBundle requested) {
        Object obj = resolveObject(aKey, requested);
        if (obj == null) {
            UResourceBundle parentBundle = getParent();
            if (parentBundle != null) {
                obj = parentBundle.handleGetObjectImpl(aKey, requested);
            }
            if (obj == null) {
                throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + aKey, getClass().getName(), aKey);
            }
        }
        return obj;
    }

    private Object resolveObject(String aKey, UResourceBundle requested) {
        if (getType() == 0) {
            return getString();
        }
        UResourceBundle obj = handleGet(aKey, (HashMap<String, String>) null, requested);
        if (obj != null) {
            if (obj.getType() == 0) {
                return obj.getString();
            }
            try {
                if (obj.getType() == 8) {
                    return obj.handleGetStringArray();
                }
            } catch (UResourceTypeMismatchException e) {
                return obj;
            }
        }
        return obj;
    }

    @Deprecated
    protected boolean isTopLevelResource() {
        return true;
    }
}
