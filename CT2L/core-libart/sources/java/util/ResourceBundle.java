package java.util;

import dalvik.system.VMStack;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import libcore.io.IoUtils;

public abstract class ResourceBundle {
    private static final String EMPTY_STRING = "";
    private static final String UNDER_SCORE = "_";
    private long lastLoadTime = 0;
    private Locale locale;
    protected ResourceBundle parent;
    private static final ResourceBundle MISSING = new MissingBundle();
    private static final ResourceBundle MISSINGBASE = new MissingBundle();
    private static final WeakHashMap<Object, Hashtable<String, ResourceBundle>> cache = new WeakHashMap<>();
    private static Locale cacheLocale = Locale.getDefault();

    public abstract Enumeration<String> getKeys();

    protected abstract Object handleGetObject(String str);

    static class MissingBundle extends ResourceBundle {
        MissingBundle() {
        }

        @Override
        public Enumeration<String> getKeys() {
            return null;
        }

        @Override
        public Object handleGetObject(String name) {
            return null;
        }
    }

    public static ResourceBundle getBundle(String bundleName) throws MissingResourceException {
        ClassLoader classLoader = VMStack.getCallingClassLoader();
        if (classLoader == null) {
            classLoader = getLoader();
        }
        return getBundle(bundleName, Locale.getDefault(), classLoader);
    }

    public static ResourceBundle getBundle(String bundleName, Locale locale) {
        ClassLoader classLoader = VMStack.getCallingClassLoader();
        if (classLoader == null) {
            classLoader = getLoader();
        }
        return getBundle(bundleName, locale, classLoader);
    }

    public static ResourceBundle getBundle(String bundleName, Locale locale, ClassLoader loader) throws Throwable {
        if (loader == null) {
            throw new NullPointerException("loader == null");
        }
        if (bundleName == null) {
            throw new NullPointerException("bundleName == null");
        }
        Locale defaultLocale = Locale.getDefault();
        if (!cacheLocale.equals(defaultLocale)) {
            cache.clear();
            cacheLocale = defaultLocale;
        }
        ResourceBundle bundle = null;
        if (!locale.equals(defaultLocale)) {
            bundle = handleGetBundle(false, bundleName, locale, loader);
        }
        if (bundle == null && (bundle = handleGetBundle(true, bundleName, defaultLocale, loader)) == null) {
            throw missingResourceException(bundleName + '_' + locale, "");
        }
        return bundle;
    }

    private static MissingResourceException missingResourceException(String className, String key) {
        String detail = "Can't find resource for bundle '" + className + "', key '" + key + "'";
        throw new MissingResourceException(detail, className, key);
    }

    public static ResourceBundle getBundle(String baseName, Control control) {
        return getBundle(baseName, Locale.getDefault(), getLoader(), control);
    }

    public static ResourceBundle getBundle(String baseName, Locale targetLocale, Control control) {
        return getBundle(baseName, targetLocale, getLoader(), control);
    }

    private static ClassLoader getLoader() {
        ClassLoader cl = ResourceBundle.class.getClassLoader();
        if (cl == null) {
            return ClassLoader.getSystemClassLoader();
        }
        return cl;
    }

    public static ResourceBundle getBundle(String baseName, Locale targetLocale, ClassLoader loader, Control control) {
        boolean expired = false;
        String bundleName = control.toBundleName(baseName, targetLocale);
        String cacheKey = loader != null ? loader : "null";
        Hashtable<String, ResourceBundle> loaderCache = getLoaderCache(cacheKey);
        ResourceBundle result = loaderCache.get(bundleName);
        if (result != null) {
            long time = control.getTimeToLive(baseName, targetLocale);
            if (time == 0 || time == -2 || result.lastLoadTime + time < System.currentTimeMillis()) {
                if (MISSING == result) {
                    throw new MissingResourceException(null, bundleName + '_' + targetLocale, "");
                }
                return result;
            }
            expired = true;
        }
        ResourceBundle ret = processGetBundle(baseName, targetLocale, loader, control, expired, result);
        if (ret != null) {
            loaderCache.put(bundleName, ret);
            ret.lastLoadTime = System.currentTimeMillis();
            return ret;
        }
        loaderCache.put(bundleName, MISSING);
        throw new MissingResourceException(null, bundleName + '_' + targetLocale, "");
    }

    private static ResourceBundle processGetBundle(String baseName, Locale targetLocale, ClassLoader loader, Control control, boolean expired, ResourceBundle result) {
        List<Locale> locales = control.getCandidateLocales(baseName, targetLocale);
        if (locales == null) {
            throw new IllegalArgumentException();
        }
        List<String> formats = control.getFormats(baseName);
        if (Control.FORMAT_CLASS == formats || Control.FORMAT_PROPERTIES == formats || Control.FORMAT_DEFAULT == formats) {
            throw new IllegalArgumentException();
        }
        ResourceBundle ret = null;
        ResourceBundle currentBundle = null;
        ResourceBundle bundle = null;
        for (Locale locale : locales) {
            for (String format : formats) {
                if (expired) {
                    try {
                        bundle = control.newBundle(baseName, locale, format, loader, control.needsReload(baseName, locale, format, loader, result, System.currentTimeMillis()));
                    } catch (IOException e) {
                    } catch (IllegalAccessException e2) {
                    } catch (InstantiationException e3) {
                    }
                } else {
                    try {
                        bundle = control.newBundle(baseName, locale, format, loader, false);
                    } catch (IllegalArgumentException e4) {
                    }
                }
                if (bundle != null) {
                    if (currentBundle != null) {
                        currentBundle.setParent(bundle);
                        currentBundle = bundle;
                    } else if (ret == null) {
                        ret = bundle;
                        currentBundle = ret;
                    }
                }
                if (bundle != null) {
                    break;
                }
            }
        }
        if (ret != null) {
            if (!Locale.ROOT.equals(ret.getLocale())) {
                return ret;
            }
            if (locales.size() == 1 && locales.contains(Locale.ROOT)) {
                return ret;
            }
        }
        Locale nextLocale = control.getFallbackLocale(baseName, targetLocale);
        if (nextLocale != null) {
            ResourceBundle ret2 = processGetBundle(baseName, nextLocale, loader, control, expired, result);
            return ret2;
        }
        return ret;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public final Object getObject(String key) {
        ResourceBundle last;
        ResourceBundle theParent = this;
        do {
            Object result = theParent.handleGetObject(key);
            if (result != null) {
                return result;
            }
            last = theParent;
            theParent = theParent.parent;
        } while (theParent != null);
        throw missingResourceException(last.getClass().getName(), key);
    }

    public final String getString(String key) {
        return (String) getObject(key);
    }

    public final String[] getStringArray(String key) {
        return (String[]) getObject(key);
    }

    private static ResourceBundle handleGetBundle(boolean loadBase, String base, Locale locale, ClassLoader loader) throws Throwable {
        ResourceBundle bundle;
        ResourceBundle bundle2;
        ResourceBundle bundle3;
        ResourceBundle parent;
        Locale newLocale;
        String localeName = locale.toString();
        String bundleName = localeName.isEmpty() ? base : base + UNDER_SCORE + localeName;
        String cacheKey = loader != null ? loader : "null";
        Hashtable<String, ResourceBundle> loaderCache = getLoaderCache(cacheKey);
        ResourceBundle cached = loaderCache.get(bundleName);
        if (cached != null) {
            if (cached == MISSINGBASE) {
                return null;
            }
            if (cached != MISSING) {
                return cached;
            }
            if (!loadBase || (newLocale = strip(locale)) == null) {
                return null;
            }
            return handleGetBundle(loadBase, base, newLocale, loader);
        }
        ResourceBundle bundle4 = null;
        try {
            Class<?> bundleClass = Class.forName(bundleName, true, loader);
            if (ResourceBundle.class.isAssignableFrom(bundleClass)) {
                bundle4 = (ResourceBundle) bundleClass.newInstance();
            }
            bundle = bundle4;
        } catch (Exception e) {
            bundle = null;
        } catch (LinkageError e2) {
            bundle = null;
        }
        if (bundle != null) {
            bundle.setLocale(locale);
            bundle2 = bundle;
        } else {
            String fileName = bundleName.replace('.', '/') + ".properties";
            InputStream stream = loader != null ? loader.getResourceAsStream(fileName) : ClassLoader.getSystemResourceAsStream(fileName);
            if (stream != null) {
                try {
                    bundle2 = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    try {
                        bundle2.setLocale(locale);
                        IoUtils.closeQuietly(stream);
                    } catch (IOException e3) {
                        IoUtils.closeQuietly(stream);
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(stream);
                        throw th;
                    }
                } catch (IOException e4) {
                    bundle2 = bundle;
                } catch (Throwable th2) {
                    th = th2;
                }
            } else {
                bundle2 = bundle;
            }
        }
        Locale strippedLocale = strip(locale);
        if (bundle2 != null) {
            if (strippedLocale != null && (parent = handleGetBundle(loadBase, base, strippedLocale, loader)) != null) {
                bundle2.setParent(parent);
            }
            loaderCache.put(bundleName, bundle2);
            return bundle2;
        }
        if (strippedLocale != null && ((loadBase || !strippedLocale.toString().isEmpty()) && (bundle3 = handleGetBundle(loadBase, base, strippedLocale, loader)) != null)) {
            loaderCache.put(bundleName, bundle3);
            return bundle3;
        }
        loaderCache.put(bundleName, loadBase ? MISSINGBASE : MISSING);
        return null;
    }

    private static Hashtable<String, ResourceBundle> getLoaderCache(Object cacheKey) {
        Hashtable<String, ResourceBundle> loaderCache;
        synchronized (cache) {
            loaderCache = cache.get(cacheKey);
            if (loaderCache == null) {
                loaderCache = new Hashtable<>();
                cache.put(cacheKey, loaderCache);
            }
        }
        return loaderCache;
    }

    protected void setParent(ResourceBundle bundle) {
        this.parent = bundle;
    }

    private static Locale strip(Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        if (!variant.isEmpty()) {
            variant = "";
        } else if (!country.isEmpty()) {
            country = "";
        } else if (!language.isEmpty()) {
            language = "";
        } else {
            return null;
        }
        return new Locale(language, country, variant);
    }

    private void setLocale(Locale locale) {
        this.locale = locale;
    }

    public static void clearCache() {
        cache.remove(ClassLoader.getSystemClassLoader());
    }

    public static void clearCache(ClassLoader loader) {
        if (loader == null) {
            throw new NullPointerException("loader == null");
        }
        cache.remove(loader);
    }

    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        return keySet().contains(key);
    }

    public Set<String> keySet() {
        Set<String> ret = new HashSet<>();
        Enumeration<String> keys = getKeys();
        while (keys.hasMoreElements()) {
            ret.add(keys.nextElement());
        }
        return ret;
    }

    protected Set<String> handleKeySet() {
        Set<String> set = keySet();
        Set<String> ret = new HashSet<>();
        for (String key : set) {
            if (handleGetObject(key) != null) {
                ret.add(key);
            }
        }
        return ret;
    }

    private static class NoFallbackControl extends Control {
        static final Control NOFALLBACK_FORMAT_PROPERTIES_CONTROL = new NoFallbackControl(JAVAPROPERTIES);
        static final Control NOFALLBACK_FORMAT_CLASS_CONTROL = new NoFallbackControl(JAVACLASS);
        static final Control NOFALLBACK_FORMAT_DEFAULT_CONTROL = new NoFallbackControl(listDefault);

        public NoFallbackControl(String format) {
            listClass = new ArrayList();
            listClass.add(format);
            this.format = Collections.unmodifiableList(listClass);
        }

        public NoFallbackControl(List<String> list) {
            this.format = list;
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException("baseName == null");
            }
            if (locale == null) {
                throw new NullPointerException("locale == null");
            }
            return null;
        }
    }

    private static class SimpleControl extends Control {
        public SimpleControl(String format) {
            listClass = new ArrayList();
            listClass.add(format);
            this.format = Collections.unmodifiableList(listClass);
        }
    }

    public static class Control {
        public static final List<String> FORMAT_CLASS;
        private static final Control FORMAT_CLASS_CONTROL;
        public static final List<String> FORMAT_DEFAULT;
        private static final Control FORMAT_DEFAULT_CONTROL;
        public static final List<String> FORMAT_PROPERTIES;
        private static final Control FORMAT_PROPERTIES_CONTROL;
        public static final long TTL_DONT_CACHE = -1;
        public static final long TTL_NO_EXPIRATION_CONTROL = -2;
        List<String> format;
        static List<String> listDefault = new ArrayList();
        static List<String> listClass = new ArrayList();
        static List<String> listProperties = new ArrayList();
        static String JAVACLASS = "java.class";
        static String JAVAPROPERTIES = "java.properties";

        static {
            listDefault.add(JAVACLASS);
            listDefault.add(JAVAPROPERTIES);
            listClass.add(JAVACLASS);
            listProperties.add(JAVAPROPERTIES);
            FORMAT_DEFAULT = Collections.unmodifiableList(listDefault);
            FORMAT_CLASS = Collections.unmodifiableList(listClass);
            FORMAT_PROPERTIES = Collections.unmodifiableList(listProperties);
            FORMAT_PROPERTIES_CONTROL = new SimpleControl(JAVAPROPERTIES);
            FORMAT_CLASS_CONTROL = new SimpleControl(JAVACLASS);
            FORMAT_DEFAULT_CONTROL = new Control();
        }

        protected Control() {
            listClass = new ArrayList();
            listClass.add(JAVACLASS);
            listClass.add(JAVAPROPERTIES);
            this.format = Collections.unmodifiableList(listClass);
        }

        public static Control getControl(List<String> formats) {
            switch (formats.size()) {
                case 1:
                    if (formats.contains(JAVACLASS)) {
                        return FORMAT_CLASS_CONTROL;
                    }
                    if (formats.contains(JAVAPROPERTIES)) {
                        return FORMAT_PROPERTIES_CONTROL;
                    }
                    break;
                case 2:
                    if (formats.equals(FORMAT_DEFAULT)) {
                        return FORMAT_DEFAULT_CONTROL;
                    }
                    break;
            }
            throw new IllegalArgumentException();
        }

        public static Control getNoFallbackControl(List<String> formats) {
            switch (formats.size()) {
                case 1:
                    if (formats.contains(JAVACLASS)) {
                        return NoFallbackControl.NOFALLBACK_FORMAT_CLASS_CONTROL;
                    }
                    if (formats.contains(JAVAPROPERTIES)) {
                        return NoFallbackControl.NOFALLBACK_FORMAT_PROPERTIES_CONTROL;
                    }
                    break;
                case 2:
                    if (formats.equals(FORMAT_DEFAULT)) {
                        return NoFallbackControl.NOFALLBACK_FORMAT_DEFAULT_CONTROL;
                    }
                    break;
            }
            throw new IllegalArgumentException();
        }

        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException("baseName == null");
            }
            if (locale == null) {
                throw new NullPointerException("locale == null");
            }
            List<Locale> retList = new ArrayList<>();
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            if (!"".equals(variant)) {
                retList.add(new Locale(language, country, variant));
            }
            if (!"".equals(country)) {
                retList.add(new Locale(language, country));
            }
            if (!"".equals(language)) {
                retList.add(new Locale(language));
            }
            retList.add(Locale.ROOT);
            return retList;
        }

        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException("baseName == null");
            }
            return this.format;
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException("baseName == null");
            }
            if (locale == null) {
                throw new NullPointerException("locale == null");
            }
            if (Locale.getDefault() != locale) {
                return Locale.getDefault();
            }
            return null;
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            if (format == null) {
                throw new NullPointerException("format == null");
            }
            if (loader == null) {
                throw new NullPointerException("loader == null");
            }
            String bundleName = toBundleName(baseName, locale);
            if (format.equals(JAVACLASS)) {
                Class<?> cls = null;
                try {
                    cls = loader.loadClass(bundleName);
                } catch (Exception e) {
                } catch (NoClassDefFoundError e2) {
                }
                if (cls == null) {
                    return null;
                }
                try {
                    ResourceBundle bundle = (ResourceBundle) cls.newInstance();
                    bundle.setLocale(locale);
                    return bundle;
                } catch (NullPointerException e3) {
                    return null;
                }
            }
            if (format.equals(JAVAPROPERTIES)) {
                InputStream streams = null;
                String resourceName = toResourceName(bundleName, "properties");
                if (reload) {
                    URL url = null;
                    try {
                        url = loader.getResource(resourceName);
                    } catch (NullPointerException e4) {
                    }
                    if (url != null) {
                        URLConnection con = url.openConnection();
                        con.setUseCaches(false);
                        streams = con.getInputStream();
                    }
                } else {
                    try {
                        streams = loader.getResourceAsStream(resourceName);
                    } catch (NullPointerException e5) {
                    }
                }
                if (streams != null) {
                    try {
                        ResourceBundle ret = new PropertyResourceBundle(new InputStreamReader(streams));
                        ret.setLocale(locale);
                        streams.close();
                        return ret;
                    } catch (IOException e6) {
                        return null;
                    }
                }
                return null;
            }
            throw new IllegalArgumentException();
        }

        public long getTimeToLive(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException("baseName == null");
            }
            if (locale == null) {
                throw new NullPointerException("locale == null");
            }
            return -2L;
        }

        public boolean needsReload(String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {
            if (bundle == null) {
                throw new NullPointerException("bundle == null");
            }
            String bundleName = toBundleName(baseName, locale);
            String suffix = format;
            if (format.equals(JAVACLASS)) {
                suffix = "class";
            }
            if (format.equals(JAVAPROPERTIES)) {
                suffix = "properties";
            }
            String urlname = toResourceName(bundleName, suffix);
            URL url = loader.getResource(urlname);
            if (url != null) {
                String fileName = url.getFile();
                long lastModified = new File(fileName).lastModified();
                if (lastModified > loadTime) {
                    return true;
                }
            }
            return false;
        }

        public String toBundleName(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException("baseName == null");
            }
            StringBuilder ret = new StringBuilder();
            StringBuilder prefix = new StringBuilder();
            ret.append(baseName);
            if (!locale.getLanguage().equals("")) {
                ret.append(ResourceBundle.UNDER_SCORE);
                ret.append(locale.getLanguage());
            } else {
                prefix.append(ResourceBundle.UNDER_SCORE);
            }
            if (!locale.getCountry().equals("")) {
                ret.append((CharSequence) prefix);
                ret.append(ResourceBundle.UNDER_SCORE);
                ret.append(locale.getCountry());
                prefix = new StringBuilder();
            } else {
                prefix.append(ResourceBundle.UNDER_SCORE);
            }
            if (!locale.getVariant().equals("")) {
                ret.append((CharSequence) prefix);
                ret.append(ResourceBundle.UNDER_SCORE);
                ret.append(locale.getVariant());
            }
            return ret.toString();
        }

        public final String toResourceName(String bundleName, String suffix) {
            if (suffix == null) {
                throw new NullPointerException("suffix == null");
            }
            return bundleName.replace('.', '/') + '.' + suffix;
        }
    }
}
