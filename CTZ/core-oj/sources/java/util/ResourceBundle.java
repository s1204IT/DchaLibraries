package java.util;

import dalvik.system.VMStack;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import sun.reflect.CallerSensitive;
import sun.util.locale.BaseLocale;
import sun.util.locale.LocaleObjectCache;

public abstract class ResourceBundle {
    static final boolean $assertionsDisabled = false;
    private static final int INITIAL_CACHE_SIZE = 32;
    private static final ResourceBundle NONEXISTENT_BUNDLE = new ResourceBundle() {
        @Override
        public Enumeration<String> getKeys() {
            return null;
        }

        @Override
        protected Object handleGetObject(String str) {
            return null;
        }

        public String toString() {
            return "NONEXISTENT_BUNDLE";
        }
    };
    private static final ConcurrentMap<CacheKey, BundleReference> cacheList = new ConcurrentHashMap(32);
    private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    private volatile CacheKey cacheKey;
    private volatile boolean expired;
    private volatile Set<String> keySet;
    private String name;
    protected ResourceBundle parent = null;
    private Locale locale = null;

    private interface CacheKeyReference {
        CacheKey getCacheKey();
    }

    public abstract Enumeration<String> getKeys();

    protected abstract Object handleGetObject(String str);

    public String getBaseBundleName() {
        return this.name;
    }

    public final String getString(String str) {
        return (String) getObject(str);
    }

    public final String[] getStringArray(String str) {
        return (String[]) getObject(str);
    }

    public final Object getObject(String str) {
        Object objHandleGetObject = handleGetObject(str);
        if (objHandleGetObject == null) {
            if (this.parent != null) {
                objHandleGetObject = this.parent.getObject(str);
            }
            if (objHandleGetObject == null) {
                throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + str, getClass().getName(), str);
            }
        }
        return objHandleGetObject;
    }

    public Locale getLocale() {
        return this.locale;
    }

    private static ClassLoader getLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        }
        return RBClassLoader.INSTANCE;
    }

    private static class RBClassLoader extends ClassLoader {
        private static final RBClassLoader INSTANCE = (RBClassLoader) AccessController.doPrivileged(new PrivilegedAction<RBClassLoader>() {
            @Override
            public RBClassLoader run() {
                return new RBClassLoader();
            }
        });
        private static final ClassLoader loader = ClassLoader.getSystemClassLoader();

        private RBClassLoader() {
        }

        @Override
        public Class<?> loadClass(String str) throws ClassNotFoundException {
            if (loader != null) {
                return loader.loadClass(str);
            }
            return Class.forName(str);
        }

        @Override
        public URL getResource(String str) {
            if (loader != null) {
                return loader.getResource(str);
            }
            return ClassLoader.getSystemResource(str);
        }

        @Override
        public InputStream getResourceAsStream(String str) {
            if (loader != null) {
                return loader.getResourceAsStream(str);
            }
            return ClassLoader.getSystemResourceAsStream(str);
        }
    }

    protected void setParent(ResourceBundle resourceBundle) {
        this.parent = resourceBundle;
    }

    private static class CacheKey implements Cloneable {
        private Throwable cause;
        private volatile long expirationTime;
        private String format;
        private int hashCodeCache;
        private volatile long loadTime;
        private LoaderReference loaderRef;
        private Locale locale;
        private String name;

        CacheKey(String str, Locale locale, ClassLoader classLoader) {
            this.name = str;
            this.locale = locale;
            if (classLoader == null) {
                this.loaderRef = null;
            } else {
                this.loaderRef = new LoaderReference(classLoader, ResourceBundle.referenceQueue, this);
            }
            calculateHashCode();
        }

        String getName() {
            return this.name;
        }

        CacheKey setName(String str) {
            if (!this.name.equals(str)) {
                this.name = str;
                calculateHashCode();
            }
            return this;
        }

        Locale getLocale() {
            return this.locale;
        }

        CacheKey setLocale(Locale locale) {
            if (!this.locale.equals(locale)) {
                this.locale = locale;
                calculateHashCode();
            }
            return this;
        }

        ClassLoader getLoader() {
            if (this.loaderRef != null) {
                return this.loaderRef.get();
            }
            return null;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            try {
                CacheKey cacheKey = (CacheKey) obj;
                if (this.hashCodeCache != cacheKey.hashCodeCache || !this.name.equals(cacheKey.name) || !this.locale.equals(cacheKey.locale)) {
                    return ResourceBundle.$assertionsDisabled;
                }
                if (this.loaderRef == null) {
                    if (cacheKey.loaderRef == null) {
                        return true;
                    }
                    return ResourceBundle.$assertionsDisabled;
                }
                ClassLoader classLoader = this.loaderRef.get();
                if (cacheKey.loaderRef != null && classLoader != null && classLoader == cacheKey.loaderRef.get()) {
                    return true;
                }
                return ResourceBundle.$assertionsDisabled;
            } catch (ClassCastException | NullPointerException e) {
                return ResourceBundle.$assertionsDisabled;
            }
        }

        public int hashCode() {
            return this.hashCodeCache;
        }

        private void calculateHashCode() {
            this.hashCodeCache = this.name.hashCode() << 3;
            this.hashCodeCache ^= this.locale.hashCode();
            ClassLoader loader = getLoader();
            if (loader != null) {
                this.hashCodeCache = loader.hashCode() ^ this.hashCodeCache;
            }
        }

        public Object clone() {
            try {
                CacheKey cacheKey = (CacheKey) super.clone();
                if (this.loaderRef != null) {
                    cacheKey.loaderRef = new LoaderReference(this.loaderRef.get(), ResourceBundle.referenceQueue, cacheKey);
                }
                cacheKey.cause = null;
                return cacheKey;
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e);
            }
        }

        String getFormat() {
            return this.format;
        }

        void setFormat(String str) {
            this.format = str;
        }

        private void setCause(Throwable th) {
            if (this.cause == null) {
                this.cause = th;
            } else if (this.cause instanceof ClassNotFoundException) {
                this.cause = th;
            }
        }

        private Throwable getCause() {
            return this.cause;
        }

        public String toString() {
            String string = this.locale.toString();
            if (string.length() == 0) {
                if (this.locale.getVariant().length() != 0) {
                    string = "__" + this.locale.getVariant();
                } else {
                    string = "\"\"";
                }
            }
            return "CacheKey[" + this.name + ", lc=" + string + ", ldr=" + ((Object) getLoader()) + "(format=" + this.format + ")]";
        }
    }

    private static class LoaderReference extends WeakReference<ClassLoader> implements CacheKeyReference {
        private CacheKey cacheKey;

        LoaderReference(ClassLoader classLoader, ReferenceQueue<Object> referenceQueue, CacheKey cacheKey) {
            super(classLoader, referenceQueue);
            this.cacheKey = cacheKey;
        }

        @Override
        public CacheKey getCacheKey() {
            return this.cacheKey;
        }
    }

    private static class BundleReference extends SoftReference<ResourceBundle> implements CacheKeyReference {
        private CacheKey cacheKey;

        BundleReference(ResourceBundle resourceBundle, ReferenceQueue<Object> referenceQueue, CacheKey cacheKey) {
            super(resourceBundle, referenceQueue);
            this.cacheKey = cacheKey;
        }

        @Override
        public CacheKey getCacheKey() {
            return this.cacheKey;
        }
    }

    @CallerSensitive
    public static final ResourceBundle getBundle(String str) {
        return getBundleImpl(str, Locale.getDefault(), getLoader(VMStack.getCallingClassLoader()), getDefaultControl(str));
    }

    @CallerSensitive
    public static final ResourceBundle getBundle(String str, Control control) {
        return getBundleImpl(str, Locale.getDefault(), getLoader(VMStack.getCallingClassLoader()), control);
    }

    @CallerSensitive
    public static final ResourceBundle getBundle(String str, Locale locale) {
        return getBundleImpl(str, locale, getLoader(VMStack.getCallingClassLoader()), getDefaultControl(str));
    }

    @CallerSensitive
    public static final ResourceBundle getBundle(String str, Locale locale, Control control) {
        return getBundleImpl(str, locale, getLoader(VMStack.getCallingClassLoader()), control);
    }

    public static ResourceBundle getBundle(String str, Locale locale, ClassLoader classLoader) {
        if (classLoader == null) {
            throw new NullPointerException();
        }
        return getBundleImpl(str, locale, classLoader, getDefaultControl(str));
    }

    public static ResourceBundle getBundle(String str, Locale locale, ClassLoader classLoader, Control control) {
        if (classLoader == null || control == null) {
            throw new NullPointerException();
        }
        return getBundleImpl(str, locale, classLoader, control);
    }

    private static Control getDefaultControl(String str) {
        return Control.INSTANCE;
    }

    private static ResourceBundle getBundleImpl(String str, Locale locale, ClassLoader classLoader, Control control) {
        ResourceBundle resourceBundleFindBundle;
        if (locale == null || control == null) {
            throw new NullPointerException();
        }
        CacheKey cacheKey = new CacheKey(str, locale, classLoader);
        BundleReference bundleReference = cacheList.get(cacheKey);
        if (bundleReference != null) {
            resourceBundleFindBundle = bundleReference.get();
        } else {
            resourceBundleFindBundle = null;
        }
        if (isValidBundle(resourceBundleFindBundle) && hasValidParentChain(resourceBundleFindBundle)) {
            return resourceBundleFindBundle;
        }
        boolean z = (control == Control.INSTANCE || (control instanceof SingleFormatControl)) ? true : $assertionsDisabled;
        List<String> formats = control.getFormats(str);
        if (!z && !checkList(formats)) {
            throw new IllegalArgumentException("Invalid Control: getFormats");
        }
        Locale fallbackLocale = locale;
        ResourceBundle resourceBundle = null;
        while (fallbackLocale != null) {
            List<Locale> candidateLocales = control.getCandidateLocales(str, fallbackLocale);
            if (z || checkList(candidateLocales)) {
                resourceBundleFindBundle = findBundle(cacheKey, candidateLocales, formats, 0, control, resourceBundle);
                if (isValidBundle(resourceBundleFindBundle)) {
                    boolean zEquals = Locale.ROOT.equals(resourceBundleFindBundle.locale);
                    if (!zEquals || resourceBundleFindBundle.locale.equals(locale) || (candidateLocales.size() == 1 && resourceBundleFindBundle.locale.equals(candidateLocales.get(0)))) {
                        break;
                    }
                    if (zEquals && resourceBundle == null) {
                        resourceBundle = resourceBundleFindBundle;
                    }
                }
                fallbackLocale = control.getFallbackLocale(str, fallbackLocale);
            } else {
                throw new IllegalArgumentException("Invalid Control: getCandidateLocales");
            }
        }
        if (resourceBundleFindBundle == null) {
            if (resourceBundle != null) {
                return resourceBundle;
            }
            throwMissingResourceException(str, locale, cacheKey.getCause());
            return resourceBundle;
        }
        return resourceBundleFindBundle;
    }

    private static boolean checkList(List<?> list) {
        boolean z = (list == null || list.isEmpty()) ? false : true;
        if (z) {
            int size = list.size();
            for (int i = 0; z && i < size; i++) {
                z = list.get(i) != null;
            }
        }
        return z;
    }

    private static ResourceBundle findBundle(CacheKey cacheKey, List<Locale> list, List<String> list2, int i, Control control, ResourceBundle resourceBundle) {
        ResourceBundle resourceBundleFindBundle;
        Locale locale = list.get(i);
        if (i != list.size() - 1) {
            resourceBundleFindBundle = findBundle(cacheKey, list, list2, i + 1, control, resourceBundle);
        } else {
            if (resourceBundle != null && Locale.ROOT.equals(locale)) {
                return resourceBundle;
            }
            resourceBundleFindBundle = null;
        }
        while (true) {
            Object objPoll = referenceQueue.poll();
            if (objPoll == null) {
                break;
            }
            cacheList.remove(((CacheKeyReference) objPoll).getCacheKey());
        }
        boolean z = $assertionsDisabled;
        cacheKey.setLocale(locale);
        ResourceBundle resourceBundleFindBundleInCache = findBundleInCache(cacheKey, control);
        if (isValidBundle(resourceBundleFindBundleInCache) && !(z = resourceBundleFindBundleInCache.expired)) {
            if (resourceBundleFindBundleInCache.parent == resourceBundleFindBundle) {
                return resourceBundleFindBundleInCache;
            }
            BundleReference bundleReference = cacheList.get(cacheKey);
            if (bundleReference != null && bundleReference.get() == resourceBundleFindBundleInCache) {
                cacheList.remove(cacheKey, bundleReference);
            }
        }
        if (resourceBundleFindBundleInCache != NONEXISTENT_BUNDLE) {
            CacheKey cacheKey2 = (CacheKey) cacheKey.clone();
            try {
                ResourceBundle resourceBundleLoadBundle = loadBundle(cacheKey, list2, control, z);
                if (resourceBundleLoadBundle != null) {
                    if (resourceBundleLoadBundle.parent == null) {
                        resourceBundleLoadBundle.setParent(resourceBundleFindBundle);
                    }
                    resourceBundleLoadBundle.locale = locale;
                    return putBundleInCache(cacheKey, resourceBundleLoadBundle, control);
                }
                putBundleInCache(cacheKey, NONEXISTENT_BUNDLE, control);
                if (cacheKey2.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                if (cacheKey2.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return resourceBundleFindBundle;
    }

    private static ResourceBundle loadBundle(CacheKey cacheKey, List<String> list, Control control, boolean z) throws IllegalAccessException, InstantiationException, IOException {
        Locale locale = cacheKey.getLocale();
        int size = list.size();
        ResourceBundle resourceBundleNewBundle = null;
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            }
            String str = list.get(i);
            try {
                resourceBundleNewBundle = control.newBundle(cacheKey.getName(), locale, str, cacheKey.getLoader(), z);
            } catch (Exception e) {
                cacheKey.setCause(e);
            } catch (LinkageError e2) {
                cacheKey.setCause(e2);
            }
            if (resourceBundleNewBundle == null) {
                i++;
            } else {
                cacheKey.setFormat(str);
                resourceBundleNewBundle.name = cacheKey.getName();
                resourceBundleNewBundle.locale = locale;
                resourceBundleNewBundle.expired = $assertionsDisabled;
                break;
            }
        }
        return resourceBundleNewBundle;
    }

    private static boolean isValidBundle(ResourceBundle resourceBundle) {
        if (resourceBundle == null || resourceBundle == NONEXISTENT_BUNDLE) {
            return $assertionsDisabled;
        }
        return true;
    }

    private static boolean hasValidParentChain(ResourceBundle resourceBundle) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        while (resourceBundle != null) {
            if (resourceBundle.expired) {
                return $assertionsDisabled;
            }
            CacheKey cacheKey = resourceBundle.cacheKey;
            if (cacheKey != null) {
                long j = cacheKey.expirationTime;
                if (j >= 0 && j <= jCurrentTimeMillis) {
                    return $assertionsDisabled;
                }
            }
            resourceBundle = resourceBundle.parent;
        }
        return true;
    }

    private static void throwMissingResourceException(String str, Locale locale, Throwable th) {
        if (th instanceof MissingResourceException) {
            th = null;
        }
        throw new MissingResourceException("Can't find bundle for base name " + str + ", locale " + ((Object) locale), str + BaseLocale.SEP + ((Object) locale), "", th);
    }

    private static ResourceBundle findBundleInCache(CacheKey cacheKey, Control control) {
        ResourceBundle resourceBundle;
        BundleReference bundleReference = cacheList.get(cacheKey);
        if (bundleReference == null || (resourceBundle = bundleReference.get()) == null) {
            return null;
        }
        ResourceBundle resourceBundle2 = resourceBundle.parent;
        if (resourceBundle2 != null && resourceBundle2.expired) {
            resourceBundle.expired = true;
            resourceBundle.cacheKey = null;
            cacheList.remove(cacheKey, bundleReference);
            return null;
        }
        CacheKey cacheKey2 = bundleReference.getCacheKey();
        long j = cacheKey2.expirationTime;
        if (!resourceBundle.expired && j >= 0 && j <= System.currentTimeMillis()) {
            if (resourceBundle != NONEXISTENT_BUNDLE) {
                synchronized (resourceBundle) {
                    long j2 = cacheKey2.expirationTime;
                    if (!resourceBundle.expired && j2 >= 0 && j2 <= System.currentTimeMillis()) {
                        try {
                            resourceBundle.expired = control.needsReload(cacheKey2.getName(), cacheKey2.getLocale(), cacheKey2.getFormat(), cacheKey2.getLoader(), resourceBundle, cacheKey2.loadTime);
                        } catch (Exception e) {
                            cacheKey.setCause(e);
                        }
                        if (resourceBundle.expired) {
                            resourceBundle.cacheKey = null;
                            cacheList.remove(cacheKey, bundleReference);
                        } else {
                            setExpirationTime(cacheKey2, control);
                        }
                    }
                }
            } else {
                cacheList.remove(cacheKey, bundleReference);
                return null;
            }
        }
        return resourceBundle;
    }

    private static ResourceBundle putBundleInCache(CacheKey cacheKey, ResourceBundle resourceBundle, Control control) {
        setExpirationTime(cacheKey, control);
        if (cacheKey.expirationTime != -1) {
            CacheKey cacheKey2 = (CacheKey) cacheKey.clone();
            BundleReference bundleReference = new BundleReference(resourceBundle, referenceQueue, cacheKey2);
            resourceBundle.cacheKey = cacheKey2;
            BundleReference bundleReferencePutIfAbsent = cacheList.putIfAbsent(cacheKey2, bundleReference);
            if (bundleReferencePutIfAbsent != null) {
                ResourceBundle resourceBundle2 = bundleReferencePutIfAbsent.get();
                if (resourceBundle2 != null && !resourceBundle2.expired) {
                    resourceBundle.cacheKey = null;
                    bundleReference.clear();
                    return resourceBundle2;
                }
                cacheList.put(cacheKey2, bundleReference);
                return resourceBundle;
            }
            return resourceBundle;
        }
        return resourceBundle;
    }

    private static void setExpirationTime(CacheKey cacheKey, Control control) {
        long timeToLive = control.getTimeToLive(cacheKey.getName(), cacheKey.getLocale());
        if (timeToLive >= 0) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            cacheKey.loadTime = jCurrentTimeMillis;
            cacheKey.expirationTime = jCurrentTimeMillis + timeToLive;
        } else {
            if (timeToLive >= -2) {
                cacheKey.expirationTime = timeToLive;
                return;
            }
            throw new IllegalArgumentException("Invalid Control: TTL=" + timeToLive);
        }
    }

    @CallerSensitive
    public static final void clearCache() {
        clearCache(getLoader(VMStack.getCallingClassLoader()));
    }

    public static final void clearCache(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new NullPointerException();
        }
        Set<CacheKey> setKeySet = cacheList.keySet();
        for (CacheKey cacheKey : setKeySet) {
            if (cacheKey.getLoader() == classLoader) {
                setKeySet.remove(cacheKey);
            }
        }
    }

    public boolean containsKey(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        for (ResourceBundle resourceBundle = this; resourceBundle != null; resourceBundle = resourceBundle.parent) {
            if (resourceBundle.handleKeySet().contains(str)) {
                return true;
            }
        }
        return $assertionsDisabled;
    }

    public Set<String> keySet() {
        HashSet hashSet = new HashSet();
        for (ResourceBundle resourceBundle = this; resourceBundle != null; resourceBundle = resourceBundle.parent) {
            hashSet.addAll(resourceBundle.handleKeySet());
        }
        return hashSet;
    }

    protected Set<String> handleKeySet() {
        if (this.keySet == null) {
            synchronized (this) {
                if (this.keySet == null) {
                    HashSet hashSet = new HashSet();
                    Enumeration<String> keys = getKeys();
                    while (keys.hasMoreElements()) {
                        String strNextElement = keys.nextElement();
                        if (handleGetObject(strNextElement) != null) {
                            hashSet.add(strNextElement);
                        }
                    }
                    this.keySet = hashSet;
                }
            }
        }
        return this.keySet;
    }

    public static class Control {
        public static final long TTL_DONT_CACHE = -1;
        public static final long TTL_NO_EXPIRATION_CONTROL = -2;
        public static final List<String> FORMAT_DEFAULT = Collections.unmodifiableList(Arrays.asList("java.class", "java.properties"));
        public static final List<String> FORMAT_CLASS = Collections.unmodifiableList(Arrays.asList("java.class"));
        public static final List<String> FORMAT_PROPERTIES = Collections.unmodifiableList(Arrays.asList("java.properties"));
        private static final Control INSTANCE = new Control();
        private static final CandidateListCache CANDIDATES_CACHE = new CandidateListCache();

        protected Control() {
        }

        public static final Control getControl(List<String> list) {
            if (!list.equals(FORMAT_PROPERTIES)) {
                if (!list.equals(FORMAT_CLASS)) {
                    if (list.equals(FORMAT_DEFAULT)) {
                        return INSTANCE;
                    }
                    throw new IllegalArgumentException();
                }
                return SingleFormatControl.CLASS_ONLY;
            }
            return SingleFormatControl.PROPERTIES_ONLY;
        }

        public static final Control getNoFallbackControl(List<String> list) {
            if (!list.equals(FORMAT_DEFAULT)) {
                if (!list.equals(FORMAT_PROPERTIES)) {
                    if (!list.equals(FORMAT_CLASS)) {
                        throw new IllegalArgumentException();
                    }
                    return NoFallbackControl.CLASS_ONLY_NO_FALLBACK;
                }
                return NoFallbackControl.PROPERTIES_ONLY_NO_FALLBACK;
            }
            return NoFallbackControl.NO_FALLBACK;
        }

        public List<String> getFormats(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            return FORMAT_DEFAULT;
        }

        public List<Locale> getCandidateLocales(String str, Locale locale) {
            if (str == null) {
                throw new NullPointerException();
            }
            return new ArrayList(CANDIDATES_CACHE.get(locale.getBaseLocale()));
        }

        private static class CandidateListCache extends LocaleObjectCache<BaseLocale, List<Locale>> {
            private CandidateListCache() {
            }

            @Override
            protected List<Locale> createObject(BaseLocale baseLocale) {
                String str;
                boolean z;
                boolean z2;
                String language = baseLocale.getLanguage();
                String script = baseLocale.getScript();
                String region = baseLocale.getRegion();
                String variant = baseLocale.getVariant();
                if (!language.equals("no")) {
                    str = variant;
                    z = false;
                    z2 = false;
                } else if (region.equals("NO") && variant.equals("NY")) {
                    str = "";
                    z = false;
                    z2 = true;
                } else {
                    str = variant;
                    z2 = false;
                    z = true;
                }
                if (language.equals("nb") || z) {
                    List<Locale> defaultList = getDefaultList("nb", script, region, str);
                    LinkedList linkedList = new LinkedList();
                    for (Locale locale : defaultList) {
                        linkedList.add(locale);
                        if (locale.getLanguage().length() == 0) {
                            break;
                        }
                        linkedList.add(Locale.getInstance("no", locale.getScript(), locale.getCountry(), locale.getVariant(), null));
                    }
                    return linkedList;
                }
                if (language.equals("nn") || z2) {
                    List<Locale> defaultList2 = getDefaultList("nn", script, region, str);
                    int size = defaultList2.size() - 1;
                    int i = size + 1;
                    defaultList2.add(size, Locale.getInstance("no", "NO", "NY"));
                    defaultList2.add(i, Locale.getInstance("no", "NO", ""));
                    defaultList2.add(i + 1, Locale.getInstance("no", "", ""));
                    return defaultList2;
                }
                if (language.equals("zh")) {
                    if (script.length() == 0 && region.length() > 0) {
                        int iHashCode = region.hashCode();
                        if (iHashCode == 2155) {
                            if (region.equals("CN")) {
                            }
                        } else if (iHashCode == 2307) {
                            if (region.equals("HK")) {
                            }
                        } else if (iHashCode == 2466) {
                            if (region.equals("MO")) {
                            }
                        } else if (iHashCode != 2644) {
                            if (iHashCode != 2691 || !region.equals("TW")) {
                            }
                            throw new UnsupportedOperationException("Method not decompiled: java.util.ResourceBundle.Control.CandidateListCache.createObject(sun.util.locale.BaseLocale):java.util.List");
                        }

                        private static List<Locale> getDefaultList(String str, String str2, String str3, String str4) {
                            LinkedList linkedList;
                            if (str4.length() > 0) {
                                linkedList = new LinkedList();
                                int length = str4.length();
                                while (length != -1) {
                                    linkedList.add(str4.substring(0, length));
                                    length = str4.lastIndexOf(95, length - 1);
                                }
                            } else {
                                linkedList = null;
                            }
                            LinkedList linkedList2 = new LinkedList();
                            if (linkedList != null) {
                                Iterator<E> it = linkedList.iterator();
                                while (it.hasNext()) {
                                    linkedList2.add(Locale.getInstance(str, str2, str3, (String) it.next(), null));
                                }
                            }
                            if (str3.length() > 0) {
                                linkedList2.add(Locale.getInstance(str, str2, str3, "", null));
                            }
                            if (str2.length() > 0) {
                                linkedList2.add(Locale.getInstance(str, str2, "", "", null));
                                if (linkedList != null) {
                                    Iterator<E> it2 = linkedList.iterator();
                                    while (it2.hasNext()) {
                                        linkedList2.add(Locale.getInstance(str, "", str3, (String) it2.next(), null));
                                    }
                                }
                                if (str3.length() > 0) {
                                    linkedList2.add(Locale.getInstance(str, "", str3, "", null));
                                }
                            }
                            if (str.length() > 0) {
                                linkedList2.add(Locale.getInstance(str, "", "", "", null));
                            }
                            linkedList2.add(Locale.ROOT);
                            return linkedList2;
                        }
                    }

                    public Locale getFallbackLocale(String str, Locale locale) {
                        if (str == null) {
                            throw new NullPointerException();
                        }
                        Locale locale2 = Locale.getDefault();
                        if (locale.equals(locale2)) {
                            return null;
                        }
                        return locale2;
                    }

                    public ResourceBundle newBundle(String str, Locale locale, String str2, final ClassLoader classLoader, final boolean z) throws IllegalAccessException, InstantiationException, IOException {
                        String bundleName = toBundleName(str, locale);
                        if (str2.equals("java.class")) {
                            try {
                                Class<?> clsLoadClass = classLoader.loadClass(bundleName);
                                if (ResourceBundle.class.isAssignableFrom(clsLoadClass)) {
                                    return (ResourceBundle) clsLoadClass.newInstance();
                                }
                                throw new ClassCastException(clsLoadClass.getName() + " cannot be cast to ResourceBundle");
                            } catch (ClassNotFoundException e) {
                                return null;
                            }
                        }
                        if (str2.equals("java.properties")) {
                            final String resourceName0 = toResourceName0(bundleName, "properties");
                            if (resourceName0 == null) {
                                return null;
                            }
                            try {
                                InputStream inputStream = (InputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                                    @Override
                                    public InputStream run() throws IOException {
                                        URLConnection uRLConnectionOpenConnection;
                                        if (z) {
                                            URL resource = classLoader.getResource(resourceName0);
                                            if (resource != null && (uRLConnectionOpenConnection = resource.openConnection()) != null) {
                                                uRLConnectionOpenConnection.setUseCaches(ResourceBundle.$assertionsDisabled);
                                                return uRLConnectionOpenConnection.getInputStream();
                                            }
                                            return null;
                                        }
                                        return classLoader.getResourceAsStream(resourceName0);
                                    }
                                });
                                if (inputStream == null) {
                                    return null;
                                }
                                try {
                                    return new PropertyResourceBundle(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                                } finally {
                                    inputStream.close();
                                }
                            } catch (PrivilegedActionException e2) {
                                throw ((IOException) e2.getException());
                            }
                        }
                        throw new IllegalArgumentException("unknown format: " + str2);
                    }

                    public long getTimeToLive(String str, Locale locale) {
                        if (str == null || locale == null) {
                            throw new NullPointerException();
                        }
                        return -2L;
                    }

                    public boolean needsReload(String str, Locale locale, String str2, ClassLoader classLoader, ResourceBundle resourceBundle, long j) {
                        URL resource;
                        if (resourceBundle == null) {
                            throw new NullPointerException();
                        }
                        if (str2.equals("java.class") || str2.equals("java.properties")) {
                            str2 = str2.substring(5);
                        }
                        try {
                            String resourceName0 = toResourceName0(toBundleName(str, locale), str2);
                            if (resourceName0 == null || (resource = classLoader.getResource(resourceName0)) == null) {
                                return ResourceBundle.$assertionsDisabled;
                            }
                            URLConnection uRLConnectionOpenConnection = resource.openConnection();
                            long lastModified = 0;
                            if (uRLConnectionOpenConnection != null) {
                                uRLConnectionOpenConnection.setUseCaches(ResourceBundle.$assertionsDisabled);
                                if (uRLConnectionOpenConnection instanceof JarURLConnection) {
                                    JarEntry jarEntry = ((JarURLConnection) uRLConnectionOpenConnection).getJarEntry();
                                    if (jarEntry != null) {
                                        long time = jarEntry.getTime();
                                        if (time != -1) {
                                            lastModified = time;
                                        }
                                    }
                                } else {
                                    lastModified = uRLConnectionOpenConnection.getLastModified();
                                }
                            }
                            if (lastModified >= j) {
                                return true;
                            }
                            return ResourceBundle.$assertionsDisabled;
                        } catch (NullPointerException e) {
                            throw e;
                        } catch (Exception e2) {
                            return ResourceBundle.$assertionsDisabled;
                        }
                    }

                    public String toBundleName(String str, Locale locale) {
                        if (locale == Locale.ROOT) {
                            return str;
                        }
                        String language = locale.getLanguage();
                        String script = locale.getScript();
                        String country = locale.getCountry();
                        String variant = locale.getVariant();
                        if (language == "" && country == "" && variant == "") {
                            return str;
                        }
                        StringBuilder sb = new StringBuilder(str);
                        sb.append('_');
                        if (script != "") {
                            if (variant != "") {
                                sb.append(language);
                                sb.append('_');
                                sb.append(script);
                                sb.append('_');
                                sb.append(country);
                                sb.append('_');
                                sb.append(variant);
                            } else if (country != "") {
                                sb.append(language);
                                sb.append('_');
                                sb.append(script);
                                sb.append('_');
                                sb.append(country);
                            } else {
                                sb.append(language);
                                sb.append('_');
                                sb.append(script);
                            }
                        } else if (variant != "") {
                            sb.append(language);
                            sb.append('_');
                            sb.append(country);
                            sb.append('_');
                            sb.append(variant);
                        } else if (country != "") {
                            sb.append(language);
                            sb.append('_');
                            sb.append(country);
                        } else {
                            sb.append(language);
                        }
                        return sb.toString();
                    }

                    public final String toResourceName(String str, String str2) {
                        StringBuilder sb = new StringBuilder(str.length() + 1 + str2.length());
                        sb.append(str.replace('.', '/'));
                        sb.append('.');
                        sb.append(str2);
                        return sb.toString();
                    }

                    private String toResourceName0(String str, String str2) {
                        if (str.contains("://")) {
                            return null;
                        }
                        return toResourceName(str, str2);
                    }
                }

                private static class SingleFormatControl extends Control {
                    private final List<String> formats;
                    private static final Control PROPERTIES_ONLY = new SingleFormatControl(FORMAT_PROPERTIES);
                    private static final Control CLASS_ONLY = new SingleFormatControl(FORMAT_CLASS);

                    protected SingleFormatControl(List<String> list) {
                        this.formats = list;
                    }

                    @Override
                    public List<String> getFormats(String str) {
                        if (str == null) {
                            throw new NullPointerException();
                        }
                        return this.formats;
                    }
                }

                private static final class NoFallbackControl extends SingleFormatControl {
                    private static final Control NO_FALLBACK = new NoFallbackControl(FORMAT_DEFAULT);
                    private static final Control PROPERTIES_ONLY_NO_FALLBACK = new NoFallbackControl(FORMAT_PROPERTIES);
                    private static final Control CLASS_ONLY_NO_FALLBACK = new NoFallbackControl(FORMAT_CLASS);

                    protected NoFallbackControl(List<String> list) {
                        super(list);
                    }

                    @Override
                    public Locale getFallbackLocale(String str, Locale locale) {
                        if (str == null || locale == null) {
                            throw new NullPointerException();
                        }
                        return null;
                    }
                }
            }
