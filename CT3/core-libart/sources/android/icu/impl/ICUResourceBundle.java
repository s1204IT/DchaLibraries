package android.icu.impl;

import android.icu.impl.ICUResourceBundleImpl;
import android.icu.impl.ICUResourceBundleReader;
import android.icu.impl.URLHandler;
import android.icu.impl.UResource;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import android.icu.util.UResourceTypeMismatchException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class ICUResourceBundle extends UResourceBundle {

    static final boolean f4assertionsDisabled;
    public static final int ALIAS = 3;
    public static final int ARRAY16 = 9;
    private static final boolean DEBUG;
    private static final String DEFAULT_TAG = "default";
    public static final int FROM_DEFAULT = 3;
    public static final int FROM_FALLBACK = 1;
    public static final int FROM_LOCALE = 4;
    public static final int FROM_ROOT = 2;
    private static final String FULL_LOCALE_NAMES_LIST = "fullLocaleNames.lst";
    private static CacheBase<String, AvailEntry, ClassLoader> GET_AVAILABLE_CACHE = null;
    private static final char HYPHEN = '-';
    private static final String ICUDATA = "ICUDATA";

    @Deprecated
    public static final String ICU_BASE_NAME = "android/icu/impl/data/icudt56b";

    @Deprecated
    public static final String ICU_BRKITR_BASE_NAME = "android/icu/impl/data/icudt56b/brkitr";

    @Deprecated
    public static final String ICU_BUNDLE = "data/icudt56b";

    @Deprecated
    public static final String ICU_COLLATION_BASE_NAME = "android/icu/impl/data/icudt56b/coll";

    @Deprecated
    public static final String ICU_CURR_BASE_NAME = "android/icu/impl/data/icudt56b/curr";
    public static final ClassLoader ICU_DATA_CLASS_LOADER;

    @Deprecated
    protected static final String ICU_DATA_PATH = "android/icu/impl/";

    @Deprecated
    public static final String ICU_LANG_BASE_NAME = "android/icu/impl/data/icudt56b/lang";

    @Deprecated
    public static final String ICU_RBNF_BASE_NAME = "android/icu/impl/data/icudt56b/rbnf";

    @Deprecated
    public static final String ICU_REGION_BASE_NAME = "android/icu/impl/data/icudt56b/region";
    private static final String ICU_RESOURCE_INDEX = "res_index";

    @Deprecated
    public static final String ICU_TRANSLIT_BASE_NAME = "android/icu/impl/data/icudt56b/translit";

    @Deprecated
    public static final String ICU_ZONE_BASE_NAME = "android/icu/impl/data/icudt56b/zone";
    protected static final String INSTALLED_LOCALES = "InstalledLocales";
    private static final String LOCALE = "LOCALE";
    private static final String NO_INHERITANCE_MARKER = "∅∅∅";
    public static final int RES_BOGUS = -1;
    private static final char RES_PATH_SEP_CHAR = '/';
    private static final String RES_PATH_SEP_STR = "/";
    public static final int STRING_V2 = 6;
    public static final int TABLE16 = 5;
    public static final int TABLE32 = 4;
    private ICUResourceBundle container;
    protected String key;
    private int loadingStatus = -1;
    WholeBundle wholeBundle;

    static {
        f4assertionsDisabled = !ICUResourceBundle.class.desiredAssertionStatus();
        ICU_DATA_CLASS_LOADER = ClassLoaderUtil.getClassLoader(ICUData.class);
        DEBUG = ICUDebug.enabled("localedata");
        GET_AVAILABLE_CACHE = new SoftCache<String, AvailEntry, ClassLoader>() {
            @Override
            protected AvailEntry createInstance(String key, ClassLoader loader) {
                return new AvailEntry(key, loader);
            }
        };
    }

    @Override
    public void setLoadingStatus(int newStatus) {
        this.loadingStatus = newStatus;
    }

    public int getLoadingStatus() {
        return this.loadingStatus;
    }

    public void setLoadingStatus(String requestedLocale) {
        String locale = getLocaleID();
        if (locale.equals("root")) {
            setLoadingStatus(2);
        } else if (locale.equals(requestedLocale)) {
            setLoadingStatus(4);
        } else {
            setLoadingStatus(1);
        }
    }

    protected static final class WholeBundle {
        String baseName;
        ClassLoader loader;
        String localeID;
        ICUResourceBundleReader reader;
        Set<String> topLevelKeys;
        ULocale ulocale;

        WholeBundle(String baseName, String localeID, ClassLoader loader, ICUResourceBundleReader reader) {
            this.baseName = baseName;
            this.localeID = localeID;
            this.ulocale = new ULocale(localeID);
            this.loader = loader;
            this.reader = reader;
        }
    }

    public static final ULocale getFunctionalEquivalent(String baseName, ClassLoader loader, String resName, String keyword, ULocale locID, boolean[] isAvailable, boolean omitDefault) {
        String kwVal = locID.getKeywordValue(keyword);
        String baseLoc = locID.getBaseName();
        String defStr = null;
        ULocale parent = new ULocale(baseLoc);
        ULocale defLoc = null;
        boolean lookForDefault = false;
        ULocale fullBase = null;
        int defDepth = 0;
        int resDepth = 0;
        if (kwVal == null || kwVal.length() == 0 || kwVal.equals(DEFAULT_TAG)) {
            kwVal = "";
            lookForDefault = true;
        }
        ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, parent);
        if (isAvailable != null) {
            isAvailable[0] = false;
            ULocale[] availableULocales = getAvailEntry(baseName, loader).getULocaleList();
            int i = 0;
            while (true) {
                if (i < availableULocales.length) {
                    if (!parent.equals(availableULocales[i])) {
                        i++;
                    } else {
                        isAvailable[0] = true;
                        break;
                    }
                }
            }
        }
        do {
            try {
                defStr = ((ICUResourceBundle) r.get(resName)).getString(DEFAULT_TAG);
                if (lookForDefault) {
                    kwVal = defStr;
                    lookForDefault = false;
                }
                defLoc = r.getULocale();
            } catch (MissingResourceException e) {
            }
            if (defLoc == null) {
                r = (ICUResourceBundle) r.getParent();
                defDepth++;
            }
            if (r == null) {
                break;
            }
        } while (defLoc == null);
        ICUResourceBundle r2 = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, new ULocale(baseLoc));
        do {
            try {
                ICUResourceBundle irb = (ICUResourceBundle) r2.get(resName);
                irb.get(kwVal);
                fullBase = irb.getULocale();
                if (fullBase != null && resDepth > defDepth) {
                    defStr = irb.getString(DEFAULT_TAG);
                    r2.getULocale();
                    defDepth = resDepth;
                }
            } catch (MissingResourceException e2) {
            }
            if (fullBase == null) {
                r2 = (ICUResourceBundle) r2.getParent();
                resDepth++;
            }
            if (r2 == null) {
                break;
            }
        } while (fullBase == null);
        if (fullBase == null && defStr != null && !defStr.equals(kwVal)) {
            kwVal = defStr;
            ICUResourceBundle r3 = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, new ULocale(baseLoc));
            resDepth = 0;
            do {
                try {
                    ICUResourceBundle irb2 = (ICUResourceBundle) r3.get(resName);
                    UResourceBundle urb = irb2.get(kwVal);
                    fullBase = r3.getULocale();
                    if (!fullBase.toString().equals(urb.getLocale().toString())) {
                        fullBase = null;
                    }
                    if (fullBase != null && resDepth > defDepth) {
                        defStr = irb2.getString(DEFAULT_TAG);
                        r3.getULocale();
                        defDepth = resDepth;
                    }
                } catch (MissingResourceException e3) {
                }
                if (fullBase == null) {
                    r3 = (ICUResourceBundle) r3.getParent();
                    resDepth++;
                }
                if (r3 == null) {
                    break;
                }
            } while (fullBase == null);
        }
        if (fullBase == null) {
            throw new MissingResourceException("Could not find locale containing requested or default keyword.", baseName, keyword + "=" + kwVal);
        }
        if (omitDefault && defStr.equals(kwVal) && resDepth <= defDepth) {
            return fullBase;
        }
        return new ULocale(fullBase.toString() + "@" + keyword + "=" + kwVal);
    }

    public static final String[] getKeywordValues(String baseName, String keyword) {
        Set<String> keywords = new HashSet<>();
        ULocale[] locales = getAvailEntry(baseName, ICU_DATA_CLASS_LOADER).getULocaleList();
        for (ULocale uLocale : locales) {
            try {
                UResourceBundle b = UResourceBundle.getBundleInstance(baseName, uLocale);
                ICUResourceBundle irb = (ICUResourceBundle) b.getObject(keyword);
                Enumeration<String> e = irb.getKeys();
                while (e.hasMoreElements()) {
                    String s = e.nextElement();
                    if (!DEFAULT_TAG.equals(s) && !s.startsWith("private-")) {
                        keywords.add(s);
                    }
                }
            } catch (Throwable th) {
            }
        }
        return (String[]) keywords.toArray(new String[0]);
    }

    public ICUResourceBundle getWithFallback(String path) throws MissingResourceException {
        ICUResourceBundle result = findResourceWithFallback(path, this, null);
        if (result == null) {
            throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), path, getKey());
        }
        if (result.getType() == 0 && result.getString().equals(NO_INHERITANCE_MARKER)) {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", path, getKey());
        }
        return result;
    }

    public ICUResourceBundle at(int index) {
        return (ICUResourceBundle) handleGet(index, (HashMap<String, String>) null, this);
    }

    public ICUResourceBundle at(String key) {
        if (this instanceof ICUResourceBundleImpl.ResourceTable) {
            return (ICUResourceBundle) handleGet(key, (HashMap<String, String>) null, this);
        }
        return null;
    }

    @Override
    public ICUResourceBundle findTopLevel(int index) {
        return (ICUResourceBundle) super.findTopLevel(index);
    }

    @Override
    public ICUResourceBundle findTopLevel(String aKey) {
        return (ICUResourceBundle) super.findTopLevel(aKey);
    }

    public ICUResourceBundle findWithFallback(String path) {
        return findResourceWithFallback(path, this, null);
    }

    public String findStringWithFallback(String path) {
        return findStringWithFallback(path, this, null);
    }

    public String getStringWithFallback(String path) throws MissingResourceException {
        String result = findStringWithFallback(path, this, null);
        if (result == null) {
            throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), path, getKey());
        }
        if (result.equals(NO_INHERITANCE_MARKER)) {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", path, getKey());
        }
        return result;
    }

    public void getAllArrayItemsWithFallback(String path, UResource.ArraySink sink) throws MissingResourceException {
        getAllContainerItemsWithFallback(path, sink, null);
    }

    public void getAllTableItemsWithFallback(String path, UResource.TableSink sink) throws MissingResourceException {
        getAllContainerItemsWithFallback(path, null, sink);
    }

    private void getAllContainerItemsWithFallback(String path, UResource.ArraySink arraySink, UResource.TableSink tableSink) throws MissingResourceException {
        ICUResourceBundle rb;
        int numPathKeys = countPathKeys(path);
        if (numPathKeys == 0) {
            rb = this;
        } else {
            int depth = getResDepth();
            String[] pathKeys = new String[depth + numPathKeys];
            getResPathKeys(path, numPathKeys, pathKeys, depth);
            rb = findResourceWithFallback(pathKeys, depth, this, null);
            if (rb == null) {
                throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), path, getKey());
            }
        }
        int expectedType = arraySink != null ? 8 : 2;
        if (rb.getType() != expectedType) {
            throw new UResourceTypeMismatchException("");
        }
        UResource.Key key = new UResource.Key();
        ICUResourceBundleReader.ReaderValue readerValue = new ICUResourceBundleReader.ReaderValue();
        rb.getAllContainerItemsWithFallback(key, readerValue, arraySink, tableSink);
    }

    private void getAllContainerItemsWithFallback(UResource.Key key, ICUResourceBundleReader.ReaderValue readerValue, UResource.ArraySink arraySink, UResource.TableSink tableSink) {
        ICUResourceBundle rb;
        int expectedType = arraySink != null ? 8 : 2;
        if (getType() == expectedType) {
            if (arraySink != null) {
                ((ICUResourceBundleImpl.ResourceArray) this).getAllItems(key, readerValue, arraySink);
            } else {
                ((ICUResourceBundleImpl.ResourceTable) this).getAllItems(key, readerValue, tableSink);
            }
        }
        if (this.parent == null) {
            return;
        }
        ICUResourceBundle parentBundle = (ICUResourceBundle) this.parent;
        int depth = getResDepth();
        if (depth == 0) {
            rb = parentBundle;
        } else {
            String[] pathKeys = new String[depth];
            getResPathKeys(pathKeys, depth);
            rb = findResourceWithFallback(pathKeys, 0, parentBundle, null);
        }
        if (rb == null || rb.getType() != expectedType) {
            return;
        }
        rb.getAllContainerItemsWithFallback(key, readerValue, arraySink, tableSink);
    }

    public static Set<String> getAvailableLocaleNameSet(String bundlePrefix, ClassLoader loader) {
        return getAvailEntry(bundlePrefix, loader).getLocaleNameSet();
    }

    public static Set<String> getFullLocaleNameSet() {
        return getFullLocaleNameSet("android/icu/impl/data/icudt56b", ICU_DATA_CLASS_LOADER);
    }

    public static Set<String> getFullLocaleNameSet(String bundlePrefix, ClassLoader loader) {
        return getAvailEntry(bundlePrefix, loader).getFullLocaleNameSet();
    }

    public static Set<String> getAvailableLocaleNameSet() {
        return getAvailableLocaleNameSet("android/icu/impl/data/icudt56b", ICU_DATA_CLASS_LOADER);
    }

    public static final ULocale[] getAvailableULocales(String baseName, ClassLoader loader) {
        return getAvailEntry(baseName, loader).getULocaleList();
    }

    public static final ULocale[] getAvailableULocales() {
        return getAvailableULocales("android/icu/impl/data/icudt56b", ICU_DATA_CLASS_LOADER);
    }

    public static final Locale[] getAvailableLocales(String baseName, ClassLoader loader) {
        return getAvailEntry(baseName, loader).getLocaleList();
    }

    public static final Locale[] getAvailableLocales() {
        return getAvailEntry("android/icu/impl/data/icudt56b", ICU_DATA_CLASS_LOADER).getLocaleList();
    }

    public static final Locale[] getLocaleList(ULocale[] ulocales) {
        ArrayList<Locale> list = new ArrayList<>(ulocales.length);
        HashSet<Locale> uniqueSet = new HashSet<>();
        for (ULocale uLocale : ulocales) {
            Locale loc = uLocale.toLocale();
            if (!uniqueSet.contains(loc)) {
                list.add(loc);
                uniqueSet.add(loc);
            }
        }
        return (Locale[]) list.toArray(new Locale[list.size()]);
    }

    @Override
    public Locale getLocale() {
        return getULocale().toLocale();
    }

    private static final ULocale[] createULocaleList(String baseName, ClassLoader root) {
        ICUResourceBundle bundle = (ICUResourceBundle) ((ICUResourceBundle) UResourceBundle.instantiateBundle(baseName, ICU_RESOURCE_INDEX, root, true)).get(INSTALLED_LOCALES);
        int length = bundle.getSize();
        int i = 0;
        ULocale[] locales = new ULocale[length];
        UResourceBundleIterator iter = bundle.getIterator();
        iter.reset();
        while (iter.hasNext()) {
            String locstr = iter.next().getKey();
            if (locstr.equals("root")) {
                locales[i] = ULocale.ROOT;
                i++;
            } else {
                locales[i] = new ULocale(locstr);
                i++;
            }
        }
        return locales;
    }

    private static final void addLocaleIDsFromIndexBundle(String baseName, ClassLoader root, Set<String> locales) {
        try {
            ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.instantiateBundle(baseName, ICU_RESOURCE_INDEX, root, true);
            UResourceBundleIterator iter = ((ICUResourceBundle) bundle.get(INSTALLED_LOCALES)).getIterator();
            iter.reset();
            while (iter.hasNext()) {
                String locstr = iter.next().getKey();
                locales.add(locstr);
            }
        } catch (MissingResourceException e) {
            if (DEBUG) {
                System.out.println("couldn't find " + baseName + RES_PATH_SEP_CHAR + ICU_RESOURCE_INDEX + ".res");
                Thread.dumpStack();
            }
        }
    }

    private static final void addBundleBaseNamesFromClassLoader(final String bn, final ClassLoader root, final Set<String> names) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    Enumeration<URL> urls = root.getResources(bn);
                    if (urls == null) {
                        return null;
                    }
                    final Set set = names;
                    URLHandler.URLVisitor v = new URLHandler.URLVisitor() {
                        @Override
                        public void visit(String s) {
                            if (!s.endsWith(".res")) {
                                return;
                            }
                            String locstr = s.substring(0, s.length() - 4);
                            set.add(locstr);
                        }
                    };
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        URLHandler handler = URLHandler.get(url);
                        if (handler != null) {
                            handler.guide(v, false);
                        } else if (ICUResourceBundle.DEBUG) {
                            System.out.println("handler for " + url + " is null");
                        }
                    }
                } catch (IOException e) {
                    if (ICUResourceBundle.DEBUG) {
                        System.out.println("ouch: " + e.getMessage());
                    }
                }
                return null;
            }
        });
    }

    private static void addLocaleIDsFromListFile(String bn, ClassLoader root, Set<String> locales) {
        try {
            InputStream s = root.getResourceAsStream(bn + FULL_LOCALE_NAMES_LIST);
            if (s == null) {
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(s, "ASCII"));
            while (true) {
                String line = br.readLine();
                if (line != null) {
                    if (line.length() != 0 && !line.startsWith("#")) {
                        locales.add(line);
                    }
                } else {
                    br.close();
                    return;
                }
            }
        } catch (IOException e) {
        }
    }

    private static Set<String> createFullLocaleNameSet(String baseName, ClassLoader loader) {
        String folder;
        String bn = baseName.endsWith(RES_PATH_SEP_STR) ? baseName : baseName + RES_PATH_SEP_STR;
        Set<String> set = new HashSet<>();
        String skipScan = ICUConfig.get("android.icu.impl.ICUResourceBundle.skipRuntimeLocaleResourceScan", "false");
        if (!skipScan.equalsIgnoreCase("true")) {
            addBundleBaseNamesFromClassLoader(bn, loader, set);
            if (baseName.startsWith("android/icu/impl/data/icudt56b")) {
                if (baseName.length() == "android/icu/impl/data/icudt56b".length()) {
                    folder = "";
                } else if (baseName.charAt("android/icu/impl/data/icudt56b".length()) == '/') {
                    folder = baseName.substring("android/icu/impl/data/icudt56b".length() + 1);
                } else {
                    folder = null;
                }
                if (folder != null) {
                    ICUBinary.addBaseNamesInFileFolder(folder, ".res", set);
                }
            }
            set.remove(ICU_RESOURCE_INDEX);
            Iterator<String> iter = set.iterator();
            while (iter.hasNext()) {
                String name = iter.next();
                if (name.length() == 1 || name.length() > 3) {
                    if (name.indexOf(95) < 0) {
                        iter.remove();
                    }
                }
            }
        }
        if (set.isEmpty()) {
            if (DEBUG) {
                System.out.println("unable to enumerate data files in " + baseName);
            }
            addLocaleIDsFromListFile(bn, loader, set);
        }
        if (set.isEmpty()) {
            addLocaleIDsFromIndexBundle(baseName, loader, set);
        }
        set.remove("root");
        set.add(ULocale.ROOT.toString());
        return Collections.unmodifiableSet(set);
    }

    private static Set<String> createLocaleNameSet(String baseName, ClassLoader loader) {
        HashSet<String> set = new HashSet<>();
        addLocaleIDsFromIndexBundle(baseName, loader, set);
        return Collections.unmodifiableSet(set);
    }

    private static final class AvailEntry {
        private volatile Set<String> fullNameSet;
        private ClassLoader loader;
        private volatile Locale[] locales;
        private volatile Set<String> nameSet;
        private String prefix;
        private volatile ULocale[] ulocales;

        AvailEntry(String prefix, ClassLoader loader) {
            this.prefix = prefix;
            this.loader = loader;
        }

        ULocale[] getULocaleList() {
            if (this.ulocales == null) {
                synchronized (this) {
                    if (this.ulocales == null) {
                        this.ulocales = ICUResourceBundle.createULocaleList(this.prefix, this.loader);
                    }
                }
            }
            return this.ulocales;
        }

        Locale[] getLocaleList() {
            if (this.locales == null) {
                getULocaleList();
                synchronized (this) {
                    if (this.locales == null) {
                        this.locales = ICUResourceBundle.getLocaleList(this.ulocales);
                    }
                }
            }
            return this.locales;
        }

        Set<String> getLocaleNameSet() {
            if (this.nameSet == null) {
                synchronized (this) {
                    if (this.nameSet == null) {
                        this.nameSet = ICUResourceBundle.createLocaleNameSet(this.prefix, this.loader);
                    }
                }
            }
            return this.nameSet;
        }

        Set<String> getFullLocaleNameSet() {
            if (this.fullNameSet == null) {
                synchronized (this) {
                    if (this.fullNameSet == null) {
                        this.fullNameSet = ICUResourceBundle.createFullLocaleNameSet(this.prefix, this.loader);
                    }
                }
            }
            return this.fullNameSet;
        }
    }

    private static AvailEntry getAvailEntry(String key, ClassLoader loader) {
        return GET_AVAILABLE_CACHE.getInstance(key, loader);
    }

    private static final ICUResourceBundle findResourceWithFallback(String path, UResourceBundle actualBundle, UResourceBundle requested) {
        if (path.length() == 0) {
            return null;
        }
        ICUResourceBundle base = (ICUResourceBundle) actualBundle;
        int depth = base.getResDepth();
        int numPathKeys = countPathKeys(path);
        if (!f4assertionsDisabled) {
            if (!(numPathKeys > 0)) {
                throw new AssertionError();
            }
        }
        String[] keys = new String[depth + numPathKeys];
        getResPathKeys(path, numPathKeys, keys, depth);
        return findResourceWithFallback(keys, depth, base, requested);
    }

    private static final ICUResourceBundle findResourceWithFallback(String[] keys, int depth, ICUResourceBundle base, UResourceBundle requested) {
        if (requested == null) {
            requested = base;
        }
        while (true) {
            int depth2 = depth + 1;
            String subKey = keys[depth];
            ICUResourceBundle sub = (ICUResourceBundle) base.handleGet(subKey, (HashMap<String, String>) null, requested);
            if (sub == null) {
                int depth3 = depth2 - 1;
                ICUResourceBundle nextBase = (ICUResourceBundle) base.getParent();
                if (nextBase == null) {
                    return null;
                }
                int baseDepth = base.getResDepth();
                if (depth3 != baseDepth) {
                    String[] newKeys = new String[(keys.length - depth3) + baseDepth];
                    System.arraycopy(keys, depth3, newKeys, baseDepth, keys.length - depth3);
                    keys = newKeys;
                }
                base.getResPathKeys(keys, baseDepth);
                base = nextBase;
                depth = 0;
            } else {
                if (depth2 == keys.length) {
                    sub.setLoadingStatus(((ICUResourceBundle) requested).getLocaleID());
                    return sub;
                }
                base = sub;
                depth = depth2;
            }
        }
    }

    private static final String findStringWithFallback(String path, UResourceBundle actualBundle, UResourceBundle requested) {
        ICUResourceBundleReader.Container readerContainer;
        ICUResourceBundle aliasedResource;
        ICUResourceBundle nextBase;
        if (path.length() == 0 || !(actualBundle instanceof ICUResourceBundleImpl.ResourceContainer)) {
            return null;
        }
        if (requested == null) {
            requested = actualBundle;
        }
        ICUResourceBundle base = (ICUResourceBundle) actualBundle;
        ICUResourceBundleReader reader = base.wholeBundle.reader;
        int res = -1;
        int baseDepth = base.getResDepth();
        int depth = baseDepth;
        int numPathKeys = countPathKeys(path);
        if (!f4assertionsDisabled) {
            if (!(numPathKeys > 0)) {
                throw new AssertionError();
            }
        }
        String[] keys = new String[baseDepth + numPathKeys];
        getResPathKeys(path, numPathKeys, keys, baseDepth);
        while (true) {
            int depth2 = depth;
            if (res == -1) {
                int type = base.getType();
                if (type == 2 || type == 8) {
                    readerContainer = ((ICUResourceBundleImpl.ResourceContainer) base).value;
                    depth = depth2 + 1;
                    String subKey = keys[depth2];
                    res = readerContainer.getResource(reader, subKey);
                    if (res != -1) {
                        int i = depth - 1;
                        nextBase = (ICUResourceBundle) base.getParent();
                        if (nextBase == null) {
                            return null;
                        }
                        base.getResPathKeys(keys, baseDepth);
                        base = nextBase;
                        reader = nextBase.wholeBundle.reader;
                        baseDepth = 0;
                        depth = 0;
                    } else {
                        if (ICUResourceBundleReader.RES_GET_TYPE(res) == 3) {
                            base.getResPathKeys(keys, baseDepth);
                            aliasedResource = getAliasedResource(base, keys, depth, subKey, res, null, requested);
                        } else {
                            aliasedResource = null;
                        }
                        if (depth == keys.length) {
                            if (aliasedResource != null) {
                                return aliasedResource.getString();
                            }
                            String s = reader.getString(res);
                            if (s == null) {
                                throw new UResourceTypeMismatchException("");
                            }
                            return s;
                        }
                        if (aliasedResource != null) {
                            base = aliasedResource;
                            reader = aliasedResource.wholeBundle.reader;
                            res = -1;
                            baseDepth = base.getResDepth();
                            if (depth != baseDepth) {
                                String[] newKeys = new String[(keys.length - depth) + baseDepth];
                                System.arraycopy(keys, depth, newKeys, baseDepth, keys.length - depth);
                                keys = newKeys;
                                depth = baseDepth;
                            }
                        }
                    }
                } else {
                    nextBase = (ICUResourceBundle) base.getParent();
                    if (nextBase == null) {
                    }
                }
            } else {
                int type2 = ICUResourceBundleReader.RES_GET_TYPE(res);
                if (ICUResourceBundleReader.URES_IS_TABLE(type2)) {
                    readerContainer = reader.getTable(res);
                } else if (ICUResourceBundleReader.URES_IS_ARRAY(type2)) {
                    readerContainer = reader.getArray(res);
                } else {
                    res = -1;
                    nextBase = (ICUResourceBundle) base.getParent();
                    if (nextBase == null) {
                    }
                }
                depth = depth2 + 1;
                String subKey2 = keys[depth2];
                res = readerContainer.getResource(reader, subKey2);
                if (res != -1) {
                }
            }
        }
    }

    private int getResDepth() {
        if (this.container == null) {
            return 0;
        }
        return this.container.getResDepth() + 1;
    }

    private void getResPathKeys(String[] keys, int depth) {
        ICUResourceBundle b = this;
        while (depth > 0) {
            depth--;
            keys[depth] = b.key;
            b = b.container;
            if (!f4assertionsDisabled) {
                if (!((depth == 0) == (b.container == null))) {
                    throw new AssertionError();
                }
            }
        }
    }

    private static int countPathKeys(String path) {
        if (path.length() == 0) {
            return 0;
        }
        int num = 1;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                num++;
            }
        }
        return num;
    }

    private static void getResPathKeys(String path, int num, String[] keys, int start) {
        if (num == 0) {
            return;
        }
        if (num == 1) {
            keys[start] = path;
            return;
        }
        int i = 0;
        while (true) {
            int j = path.indexOf(47, i);
            if (!f4assertionsDisabled) {
                if (!(j >= i)) {
                    throw new AssertionError();
                }
            }
            int start2 = start + 1;
            keys[start] = path.substring(i, j);
            if (num == 2) {
                if (!f4assertionsDisabled) {
                    if (!(path.indexOf(47, j + 1) < 0)) {
                        throw new AssertionError();
                    }
                }
                keys[start2] = path.substring(j + 1);
                return;
            }
            i = j + 1;
            num--;
            start = start2;
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ICUResourceBundle) {
            ICUResourceBundle o = (ICUResourceBundle) other;
            return getBaseName().equals(o.getBaseName()) && getLocaleID().equals(o.getLocaleID());
        }
        return false;
    }

    public int hashCode() {
        if (f4assertionsDisabled) {
            return 42;
        }
        throw new AssertionError("hashCode not designed");
    }

    public enum OpenType {
        LOCALE_DEFAULT_ROOT,
        LOCALE_ROOT,
        DIRECT;

        public static OpenType[] valuesCustom() {
            return values();
        }
    }

    public static UResourceBundle getBundleInstance(String baseName, String localeID, ClassLoader root, boolean disableFallback) {
        UResourceBundle b = instantiateBundle(baseName, localeID, root, disableFallback ? OpenType.DIRECT : OpenType.LOCALE_DEFAULT_ROOT);
        if (b == null) {
            throw new MissingResourceException("Could not find the bundle " + baseName + RES_PATH_SEP_STR + localeID + ".res", "", "");
        }
        return b;
    }

    protected static UResourceBundle instantiateBundle(String baseName, String localeID, ClassLoader root, boolean disableFallback) {
        return instantiateBundle(baseName, localeID, root, disableFallback ? OpenType.DIRECT : OpenType.LOCALE_DEFAULT_ROOT);
    }

    public static UResourceBundle getBundleInstance(String baseName, ULocale locale, OpenType openType) {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return getBundleInstance(baseName, locale.toString(), ICU_DATA_CLASS_LOADER, openType);
    }

    public static UResourceBundle getBundleInstance(String baseName, String localeID, ClassLoader root, OpenType openType) {
        if (baseName == null) {
            baseName = "android/icu/impl/data/icudt56b";
        }
        UResourceBundle b = instantiateBundle(baseName, localeID, root, openType);
        if (b == null) {
            throw new MissingResourceException("Could not find the bundle " + baseName + RES_PATH_SEP_STR + localeID + ".res", "", "");
        }
        return b;
    }

    private static synchronized UResourceBundle instantiateBundle(String baseName, String localeID, ClassLoader root, OpenType openType) {
        ULocale defaultLocale = ULocale.getDefault();
        String localeName = localeID;
        if (localeID.indexOf(64) >= 0) {
            localeName = ULocale.getBaseName(localeID);
        }
        String fullName = ICUResourceBundleReader.getFullName(baseName, localeName);
        ICUResourceBundle b = (ICUResourceBundle) loadFromCache(fullName, defaultLocale);
        String rootLocale = baseName.indexOf(46) == -1 ? "root" : "";
        String defaultID = defaultLocale.getBaseName();
        if (localeName.equals("")) {
            localeName = rootLocale;
        }
        if (DEBUG) {
            System.out.println("Creating " + fullName + " currently b is " + b);
        }
        if (b == null) {
            b = createBundle(baseName, localeName, root);
            if (DEBUG) {
                System.out.println("The bundle created is: " + b + " and openType=" + openType + " and bundle.getNoFallback=" + (b != null ? b.getNoFallback() : false));
            }
            if (openType == OpenType.DIRECT || (b != null && b.getNoFallback())) {
                return addToCache(fullName, defaultLocale, b);
            }
            if (b == null) {
                int i = localeName.lastIndexOf(95);
                if (i != -1) {
                    String temp = localeName.substring(0, i);
                    b = (ICUResourceBundle) instantiateBundle(baseName, temp, root, openType);
                    if (b != null && b.getULocale().getName().equals(temp)) {
                        b.setLoadingStatus(1);
                    }
                } else if (openType == OpenType.LOCALE_DEFAULT_ROOT && !defaultLocale.getLanguage().equals(localeName)) {
                    b = (ICUResourceBundle) instantiateBundle(baseName, defaultID, root, openType);
                    if (b != null) {
                        b.setLoadingStatus(3);
                    }
                } else if (rootLocale.length() != 0 && (b = createBundle(baseName, rootLocale, root)) != null) {
                    b.setLoadingStatus(2);
                }
            } else {
                UResourceBundle parent = null;
                String localeName2 = b.getLocaleID();
                int i2 = localeName2.lastIndexOf(95);
                b = (ICUResourceBundle) addToCache(fullName, defaultLocale, b);
                String parentLocaleName = ((ICUResourceBundleImpl.ResourceTable) b).findString("%%Parent");
                if (parentLocaleName != null) {
                    parent = instantiateBundle(baseName, parentLocaleName, root, openType);
                } else if (i2 != -1) {
                    parent = instantiateBundle(baseName, localeName2.substring(0, i2), root, openType);
                } else if (!localeName2.equals(rootLocale)) {
                    parent = instantiateBundle(baseName, rootLocale, root, true);
                }
                if (!b.equals(parent)) {
                    b.setParent(parent);
                }
            }
        }
        return b;
    }

    UResourceBundle get(String aKey, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        ICUResourceBundle obj = (ICUResourceBundle) handleGet(aKey, aliasesVisited, requested);
        if (obj == null) {
            obj = (ICUResourceBundle) getParent();
            if (obj != null) {
                obj = (ICUResourceBundle) obj.get(aKey, aliasesVisited, requested);
            }
            if (obj == null) {
                String fullName = ICUResourceBundleReader.getFullName(getBaseName(), getLocaleID());
                throw new MissingResourceException("Can't find resource for bundle " + fullName + ", key " + aKey, getClass().getName(), aKey);
            }
        }
        obj.setLoadingStatus(((ICUResourceBundle) requested).getLocaleID());
        return obj;
    }

    public static ICUResourceBundle createBundle(String baseName, String localeID, ClassLoader root) {
        ICUResourceBundleReader reader = ICUResourceBundleReader.getReader(baseName, localeID, root);
        if (reader == null) {
            return null;
        }
        return getBundle(reader, baseName, localeID, root);
    }

    @Override
    protected String getLocaleID() {
        return this.wholeBundle.localeID;
    }

    @Override
    protected String getBaseName() {
        return this.wholeBundle.baseName;
    }

    @Override
    public ULocale getULocale() {
        return this.wholeBundle.ulocale;
    }

    @Override
    public UResourceBundle getParent() {
        return (UResourceBundle) this.parent;
    }

    @Override
    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    private boolean getNoFallback() {
        return this.wholeBundle.reader.getNoFallback();
    }

    private static ICUResourceBundle getBundle(ICUResourceBundleReader reader, String baseName, String localeID, ClassLoader loader) {
        int rootRes = reader.getRootResource();
        if (ICUResourceBundleReader.URES_IS_TABLE(ICUResourceBundleReader.RES_GET_TYPE(rootRes))) {
            WholeBundle wb = new WholeBundle(baseName, localeID, loader, reader);
            ICUResourceBundleImpl.ResourceTable rootTable = new ICUResourceBundleImpl.ResourceTable(wb, rootRes);
            String aliasString = rootTable.findString("%%ALIAS");
            if (aliasString != null) {
                return (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, aliasString);
            }
            return rootTable;
        }
        throw new IllegalStateException("Invalid format error");
    }

    protected ICUResourceBundle(WholeBundle wholeBundle) {
        this.wholeBundle = wholeBundle;
    }

    protected ICUResourceBundle(ICUResourceBundle container, String key) {
        this.key = key;
        this.wholeBundle = container.wholeBundle;
        this.container = (ICUResourceBundleImpl.ResourceContainer) container;
        this.parent = container.parent;
    }

    protected static ICUResourceBundle getAliasedResource(ICUResourceBundle base, String[] keys, int depth, String key, int _resource, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        String locale;
        String bundleName;
        ICUResourceBundle bundle;
        int numKeys;
        int idx;
        WholeBundle wholeBundle = base.wholeBundle;
        ClassLoader loaderToUse = wholeBundle.loader;
        String keyPath = null;
        String rpath = wholeBundle.reader.getAlias(_resource);
        if (aliasesVisited == null) {
            aliasesVisited = new HashMap<>();
        }
        if (aliasesVisited.get(rpath) != null) {
            throw new IllegalArgumentException("Circular references in the resource bundles");
        }
        aliasesVisited.put(rpath, "");
        if (rpath.indexOf(47) == 0) {
            int i = rpath.indexOf(47, 1);
            int j = rpath.indexOf(47, i + 1);
            bundleName = rpath.substring(1, i);
            if (j < 0) {
                locale = rpath.substring(i + 1);
            } else {
                locale = rpath.substring(i + 1, j);
                keyPath = rpath.substring(j + 1, rpath.length());
            }
            if (bundleName.equals(ICUDATA)) {
                bundleName = "android/icu/impl/data/icudt56b";
                loaderToUse = ICU_DATA_CLASS_LOADER;
            } else if (bundleName.indexOf(ICUDATA) > -1 && (idx = bundleName.indexOf(45)) > -1) {
                bundleName = "android/icu/impl/data/icudt56b/" + bundleName.substring(idx + 1, bundleName.length());
                loaderToUse = ICU_DATA_CLASS_LOADER;
            }
        } else {
            int i2 = rpath.indexOf(47);
            if (i2 != -1) {
                locale = rpath.substring(0, i2);
                keyPath = rpath.substring(i2 + 1);
            } else {
                locale = rpath;
            }
            bundleName = wholeBundle.baseName;
        }
        ICUResourceBundle sub = null;
        if (bundleName.equals(LOCALE)) {
            String bundleName2 = wholeBundle.baseName;
            String keyPath2 = rpath.substring(LOCALE.length() + 2, rpath.length());
            ICUResourceBundle bundle2 = (ICUResourceBundle) requested;
            while (bundle2.container != null) {
                bundle2 = bundle2.container;
            }
            sub = findResourceWithFallback(keyPath2, bundle2, null);
        } else {
            if (locale == null) {
                bundle = (ICUResourceBundle) getBundleInstance(bundleName, "", loaderToUse, false);
            } else {
                bundle = (ICUResourceBundle) getBundleInstance(bundleName, locale, loaderToUse, false);
            }
            if (keyPath != null) {
                numKeys = countPathKeys(keyPath);
                if (numKeys > 0) {
                    keys = new String[numKeys];
                    getResPathKeys(keyPath, numKeys, keys, 0);
                }
            } else if (keys != null) {
                numKeys = depth;
            } else {
                int depth2 = base.getResDepth();
                numKeys = depth2 + 1;
                keys = new String[numKeys];
                base.getResPathKeys(keys, depth2);
                keys[depth2] = key;
            }
            if (numKeys > 0) {
                sub = bundle;
                for (int i3 = 0; sub != null && i3 < numKeys; i3++) {
                    sub = (ICUResourceBundle) sub.get(keys[i3], aliasesVisited, requested);
                }
            }
        }
        if (sub == null) {
            throw new MissingResourceException(wholeBundle.localeID, wholeBundle.baseName, key);
        }
        return sub;
    }

    public final Set<String> getTopLevelKeySet() {
        return this.wholeBundle.topLevelKeys;
    }

    public final void setTopLevelKeySet(Set<String> keySet) {
        this.wholeBundle.topLevelKeys = keySet;
    }

    @Override
    protected Enumeration<String> handleGetKeys() {
        return Collections.enumeration(handleKeySet());
    }

    @Override
    protected boolean isTopLevelResource() {
        return this.container == null;
    }
}
