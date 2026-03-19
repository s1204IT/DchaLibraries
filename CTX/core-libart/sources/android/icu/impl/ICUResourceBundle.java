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
import java.io.PrintStream;
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
    static final boolean $assertionsDisabled = false;
    public static final int ALIAS = 3;
    public static final int ARRAY16 = 9;
    private static final String DEFAULT_TAG = "default";
    private static final String FULL_LOCALE_NAMES_LIST = "fullLocaleNames.lst";
    private static final char HYPHEN = '-';
    private static final String ICUDATA = "ICUDATA";
    private static final String ICU_RESOURCE_INDEX = "res_index";
    protected static final String INSTALLED_LOCALES = "InstalledLocales";
    private static final String LOCALE = "LOCALE";
    public static final String NO_INHERITANCE_MARKER = "∅∅∅";
    public static final int RES_BOGUS = -1;
    private static final char RES_PATH_SEP_CHAR = '/';
    private static final String RES_PATH_SEP_STR = "/";
    public static final int STRING_V2 = 6;
    public static final int TABLE16 = 5;
    public static final int TABLE32 = 4;
    private ICUResourceBundle container;
    protected String key;
    WholeBundle wholeBundle;
    public static final ClassLoader ICU_DATA_CLASS_LOADER = ClassLoaderUtil.getClassLoader(ICUData.class);
    private static CacheBase<String, ICUResourceBundle, Loader> BUNDLE_CACHE = new SoftCache<String, ICUResourceBundle, Loader>() {
        @Override
        protected ICUResourceBundle createInstance(String str, Loader loader) {
            return loader.load();
        }
    };
    private static final boolean DEBUG = ICUDebug.enabled("localedata");
    private static CacheBase<String, AvailEntry, ClassLoader> GET_AVAILABLE_CACHE = new SoftCache<String, AvailEntry, ClassLoader>() {
        @Override
        protected AvailEntry createInstance(String str, ClassLoader classLoader) {
            return new AvailEntry(str, classLoader);
        }
    };

    public enum OpenType {
        LOCALE_DEFAULT_ROOT,
        LOCALE_ROOT,
        LOCALE_ONLY,
        DIRECT
    }

    protected static final class WholeBundle {
        String baseName;
        ClassLoader loader;
        String localeID;
        ICUResourceBundleReader reader;
        Set<String> topLevelKeys;
        ULocale ulocale;

        WholeBundle(String str, String str2, ClassLoader classLoader, ICUResourceBundleReader iCUResourceBundleReader) {
            this.baseName = str;
            this.localeID = str2;
            this.ulocale = new ULocale(str2);
            this.loader = classLoader;
            this.reader = iCUResourceBundleReader;
        }
    }

    private static abstract class Loader {
        abstract ICUResourceBundle load();

        private Loader() {
        }
    }

    public static final ULocale getFunctionalEquivalent(String str, ClassLoader classLoader, String str2, String str3, ULocale uLocale, boolean[] zArr, boolean z) {
        String str4;
        boolean z2;
        String str5;
        String string;
        String keywordValue = uLocale.getKeywordValue(str3);
        String baseName = uLocale.getBaseName();
        ULocale uLocale2 = new ULocale(baseName);
        int i = 0;
        if (keywordValue == null || keywordValue.length() == 0 || keywordValue.equals(DEFAULT_TAG)) {
            str4 = "";
            z2 = true;
        } else {
            str4 = keywordValue;
            z2 = false;
        }
        ICUResourceBundle parent = (ICUResourceBundle) UResourceBundle.getBundleInstance(str, uLocale2);
        if (zArr != null) {
            zArr[0] = false;
            ULocale[] uLocaleList = getAvailEntry(str, classLoader).getULocaleList();
            int i2 = 0;
            while (true) {
                if (i2 >= uLocaleList.length) {
                    break;
                }
                if (!uLocale2.equals(uLocaleList[i2])) {
                    i2++;
                } else {
                    zArr[0] = true;
                    break;
                }
            }
        }
        ULocale uLocale3 = null;
        String str6 = null;
        int i3 = 0;
        do {
            try {
                String string2 = ((ICUResourceBundle) parent.get(str2)).getString(DEFAULT_TAG);
                if (z2) {
                    z2 = false;
                    str4 = string2;
                }
                try {
                    uLocale3 = parent.getULocale();
                    str6 = string2;
                } catch (MissingResourceException e) {
                    str6 = string2;
                }
            } catch (MissingResourceException e2) {
            }
            if (uLocale3 == null) {
                parent = parent.getParent();
                i3++;
            }
            if (parent == null) {
                break;
            }
        } while (uLocale3 == null);
        ICUResourceBundle parent2 = (ICUResourceBundle) UResourceBundle.getBundleInstance(str, new ULocale(baseName));
        ULocale uLocale4 = null;
        int i4 = i3;
        int i5 = 0;
        do {
            try {
                ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) parent2.get(str2);
                iCUResourceBundle.get(str4);
                ULocale uLocale5 = iCUResourceBundle.getULocale();
                if (uLocale5 != null && i5 > i4) {
                    try {
                        string = iCUResourceBundle.getString(DEFAULT_TAG);
                        try {
                            parent2.getULocale();
                            i4 = i5;
                        } catch (MissingResourceException e3) {
                            str6 = string;
                            uLocale4 = uLocale5;
                        }
                    } catch (MissingResourceException e4) {
                    }
                } else {
                    string = str6;
                }
                str6 = string;
                uLocale4 = uLocale5;
            } catch (MissingResourceException e5) {
            }
            if (uLocale4 == null) {
                parent2 = parent2.getParent();
                i5++;
            }
            if (parent2 == null) {
                break;
            }
        } while (uLocale4 == null);
        if (uLocale4 != null || str6 == null || str6.equals(str4)) {
            i = i5;
            str5 = str6;
        } else {
            ICUResourceBundle parent3 = (ICUResourceBundle) UResourceBundle.getBundleInstance(str, new ULocale(baseName));
            str5 = str6;
            do {
                try {
                    ICUResourceBundle iCUResourceBundle2 = (ICUResourceBundle) parent3.get(str2);
                    ICUResourceBundle iCUResourceBundle3 = (ICUResourceBundle) iCUResourceBundle2.get(str6);
                    ULocale uLocale6 = parent3.getULocale();
                    try {
                        uLocale4 = !uLocale6.getBaseName().equals(iCUResourceBundle3.getULocale().getBaseName()) ? null : uLocale6;
                        if (uLocale4 != null && i > i4) {
                            String string3 = iCUResourceBundle2.getString(DEFAULT_TAG);
                            try {
                                parent3.getULocale();
                                str5 = string3;
                                i4 = i;
                            } catch (MissingResourceException e6) {
                                str5 = string3;
                            }
                        }
                    } catch (MissingResourceException e7) {
                        uLocale4 = uLocale6;
                    }
                } catch (MissingResourceException e8) {
                }
                if (uLocale4 == null) {
                    parent3 = parent3.getParent();
                    i++;
                }
                if (parent3 == null) {
                    break;
                }
            } while (uLocale4 == null);
            str4 = str6;
        }
        if (uLocale4 == null) {
            throw new MissingResourceException("Could not find locale containing requested or default keyword.", str, str3 + "=" + str4);
        }
        if (z && str5.equals(str4) && i <= i4) {
            return uLocale4;
        }
        return new ULocale(uLocale4.getBaseName() + "@" + str3 + "=" + str4);
    }

    public static final String[] getKeywordValues(String str, String str2) {
        HashSet hashSet = new HashSet();
        for (ULocale uLocale : getAvailEntry(str, ICU_DATA_CLASS_LOADER).getULocaleList()) {
            try {
                Enumeration<String> keys = ((ICUResourceBundle) UResourceBundle.getBundleInstance(str, uLocale).getObject(str2)).getKeys();
                while (keys.hasMoreElements()) {
                    String strNextElement = keys.nextElement();
                    if (!DEFAULT_TAG.equals(strNextElement) && !strNextElement.startsWith("private-")) {
                        hashSet.add(strNextElement);
                    }
                }
            } catch (Throwable th) {
            }
        }
        return (String[]) hashSet.toArray(new String[0]);
    }

    public ICUResourceBundle getWithFallback(String str) throws MissingResourceException {
        ICUResourceBundle iCUResourceBundleFindResourceWithFallback = findResourceWithFallback(str, this, null);
        if (iCUResourceBundleFindResourceWithFallback == null) {
            throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), str, getKey());
        }
        if (iCUResourceBundleFindResourceWithFallback.getType() == 0 && iCUResourceBundleFindResourceWithFallback.getString().equals(NO_INHERITANCE_MARKER)) {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", str, getKey());
        }
        return iCUResourceBundleFindResourceWithFallback;
    }

    public ICUResourceBundle at(int i) {
        return (ICUResourceBundle) handleGet(i, (HashMap<String, String>) null, this);
    }

    public ICUResourceBundle at(String str) {
        if (this instanceof ICUResourceBundleImpl.ResourceTable) {
            return (ICUResourceBundle) handleGet(str, (HashMap<String, String>) null, this);
        }
        return null;
    }

    @Override
    public ICUResourceBundle findTopLevel(int i) {
        return (ICUResourceBundle) super.findTopLevel(i);
    }

    @Override
    public ICUResourceBundle findTopLevel(String str) {
        return (ICUResourceBundle) super.findTopLevel(str);
    }

    public ICUResourceBundle findWithFallback(String str) {
        return findResourceWithFallback(str, this, null);
    }

    public String findStringWithFallback(String str) {
        return findStringWithFallback(str, this, null);
    }

    public String getStringWithFallback(String str) throws MissingResourceException {
        String strFindStringWithFallback = findStringWithFallback(str, this, null);
        if (strFindStringWithFallback == null) {
            throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), str, getKey());
        }
        if (strFindStringWithFallback.equals(NO_INHERITANCE_MARKER)) {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", str, getKey());
        }
        return strFindStringWithFallback;
    }

    public void getAllItemsWithFallbackNoFail(String str, UResource.Sink sink) {
        try {
            getAllItemsWithFallback(str, sink);
        } catch (MissingResourceException e) {
        }
    }

    public void getAllItemsWithFallback(String str, UResource.Sink sink) throws MissingResourceException {
        ICUResourceBundle iCUResourceBundleFindResourceWithFallback;
        int iCountPathKeys = countPathKeys(str);
        if (iCountPathKeys != 0) {
            int resDepth = getResDepth();
            String[] strArr = new String[resDepth + iCountPathKeys];
            getResPathKeys(str, iCountPathKeys, strArr, resDepth);
            iCUResourceBundleFindResourceWithFallback = findResourceWithFallback(strArr, resDepth, this, null);
            if (iCUResourceBundleFindResourceWithFallback == null) {
                throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), str, getKey());
            }
        } else {
            iCUResourceBundleFindResourceWithFallback = this;
        }
        iCUResourceBundleFindResourceWithFallback.getAllItemsWithFallback(new UResource.Key(), new ICUResourceBundleReader.ReaderValue(), sink);
    }

    private void getAllItemsWithFallback(UResource.Key key, ICUResourceBundleReader.ReaderValue readerValue, UResource.Sink sink) {
        ICUResourceBundleImpl iCUResourceBundleImpl = (ICUResourceBundleImpl) this;
        readerValue.reader = iCUResourceBundleImpl.wholeBundle.reader;
        readerValue.res = iCUResourceBundleImpl.getResource();
        key.setString(this.key != null ? this.key : "");
        sink.put(key, readerValue, this.parent == null);
        if (this.parent != null) {
            ICUResourceBundle iCUResourceBundleFindResourceWithFallback = (ICUResourceBundle) this.parent;
            int resDepth = getResDepth();
            if (resDepth != 0) {
                String[] strArr = new String[resDepth];
                getResPathKeys(strArr, resDepth);
                iCUResourceBundleFindResourceWithFallback = findResourceWithFallback(strArr, 0, iCUResourceBundleFindResourceWithFallback, null);
            }
            if (iCUResourceBundleFindResourceWithFallback != null) {
                iCUResourceBundleFindResourceWithFallback.getAllItemsWithFallback(key, readerValue, sink);
            }
        }
    }

    public static Set<String> getAvailableLocaleNameSet(String str, ClassLoader classLoader) {
        return getAvailEntry(str, classLoader).getLocaleNameSet();
    }

    public static Set<String> getFullLocaleNameSet() {
        return getFullLocaleNameSet(ICUData.ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    public static Set<String> getFullLocaleNameSet(String str, ClassLoader classLoader) {
        return getAvailEntry(str, classLoader).getFullLocaleNameSet();
    }

    public static Set<String> getAvailableLocaleNameSet() {
        return getAvailableLocaleNameSet(ICUData.ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    public static final ULocale[] getAvailableULocales(String str, ClassLoader classLoader) {
        return getAvailEntry(str, classLoader).getULocaleList();
    }

    public static final ULocale[] getAvailableULocales() {
        return getAvailableULocales(ICUData.ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    public static final Locale[] getAvailableLocales(String str, ClassLoader classLoader) {
        return getAvailEntry(str, classLoader).getLocaleList();
    }

    public static final Locale[] getAvailableLocales() {
        return getAvailEntry(ICUData.ICU_BASE_NAME, ICU_DATA_CLASS_LOADER).getLocaleList();
    }

    public static final Locale[] getLocaleList(ULocale[] uLocaleArr) {
        ArrayList arrayList = new ArrayList(uLocaleArr.length);
        HashSet hashSet = new HashSet();
        for (ULocale uLocale : uLocaleArr) {
            Locale locale = uLocale.toLocale();
            if (!hashSet.contains(locale)) {
                arrayList.add(locale);
                hashSet.add(locale);
            }
        }
        return (Locale[]) arrayList.toArray(new Locale[arrayList.size()]);
    }

    @Override
    public Locale getLocale() {
        return getULocale().toLocale();
    }

    private static final ULocale[] createULocaleList(String str, ClassLoader classLoader) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) ((ICUResourceBundle) UResourceBundle.instantiateBundle(str, ICU_RESOURCE_INDEX, classLoader, true)).get(INSTALLED_LOCALES);
        ULocale[] uLocaleArr = new ULocale[iCUResourceBundle.getSize()];
        UResourceBundleIterator iterator = iCUResourceBundle.getIterator();
        iterator.reset();
        int i = 0;
        while (iterator.hasNext()) {
            String key = iterator.next().getKey();
            if (key.equals("root")) {
                uLocaleArr[i] = ULocale.ROOT;
                i++;
            } else {
                uLocaleArr[i] = new ULocale(key);
                i++;
            }
        }
        return uLocaleArr;
    }

    private static final void addLocaleIDsFromIndexBundle(String str, ClassLoader classLoader, Set<String> set) {
        try {
            UResourceBundleIterator iterator = ((ICUResourceBundle) ((ICUResourceBundle) UResourceBundle.instantiateBundle(str, ICU_RESOURCE_INDEX, classLoader, true)).get(INSTALLED_LOCALES)).getIterator();
            iterator.reset();
            while (iterator.hasNext()) {
                set.add(iterator.next().getKey());
            }
        } catch (MissingResourceException e) {
            if (DEBUG) {
                System.out.println("couldn't find " + str + RES_PATH_SEP_CHAR + ICU_RESOURCE_INDEX + ".res");
                Thread.dumpStack();
            }
        }
    }

    private static final void addBundleBaseNamesFromClassLoader(final String str, final ClassLoader classLoader, final Set<String> set) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    Enumeration<URL> resources = classLoader.getResources(str);
                    if (resources == null) {
                        return null;
                    }
                    URLHandler.URLVisitor uRLVisitor = new URLHandler.URLVisitor() {
                        @Override
                        public void visit(String str2) {
                            if (str2.endsWith(".res")) {
                                set.add(str2.substring(0, str2.length() - 4));
                            }
                        }
                    };
                    while (resources.hasMoreElements()) {
                        URL urlNextElement = resources.nextElement();
                        URLHandler uRLHandler = URLHandler.get(urlNextElement);
                        if (uRLHandler == null) {
                            if (ICUResourceBundle.DEBUG) {
                                System.out.println("handler for " + urlNextElement + " is null");
                            }
                        } else {
                            uRLHandler.guide(uRLVisitor, false);
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

    private static void addLocaleIDsFromListFile(String str, ClassLoader classLoader, Set<String> set) {
        try {
            InputStream resourceAsStream = classLoader.getResourceAsStream(str + FULL_LOCALE_NAMES_LIST);
            if (resourceAsStream != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, "ASCII"));
                while (true) {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            if (line.length() != 0 && !line.startsWith("#")) {
                                set.add(line);
                            }
                        } else {
                            bufferedReader.close();
                            return;
                        }
                    } catch (Throwable th) {
                        bufferedReader.close();
                        throw th;
                    }
                }
            }
        } catch (IOException e) {
        }
    }

    private static Set<String> createFullLocaleNameSet(String str, ClassLoader classLoader) {
        String str2;
        String strSubstring;
        if (!str.endsWith(RES_PATH_SEP_STR)) {
            str2 = str + RES_PATH_SEP_STR;
        } else {
            str2 = str;
        }
        HashSet hashSet = new HashSet();
        if (!ICUConfig.get("android.icu.impl.ICUResourceBundle.skipRuntimeLocaleResourceScan", "false").equalsIgnoreCase("true")) {
            addBundleBaseNamesFromClassLoader(str2, classLoader, hashSet);
            if (str.startsWith(ICUData.ICU_BASE_NAME)) {
                if (str.length() == ICUData.ICU_BASE_NAME.length()) {
                    strSubstring = "";
                } else if (str.charAt(ICUData.ICU_BASE_NAME.length()) == '/') {
                    strSubstring = str.substring(ICUData.ICU_BASE_NAME.length() + 1);
                } else {
                    strSubstring = null;
                }
                if (strSubstring != null) {
                    ICUBinary.addBaseNamesInFileFolder(strSubstring, ".res", hashSet);
                }
            }
            hashSet.remove(ICU_RESOURCE_INDEX);
            Iterator it = hashSet.iterator();
            while (it.hasNext()) {
                String str3 = (String) it.next();
                if (str3.length() == 1 || str3.length() > 3) {
                    if (str3.indexOf(95) < 0) {
                        it.remove();
                    }
                }
            }
        }
        if (hashSet.isEmpty()) {
            if (DEBUG) {
                System.out.println("unable to enumerate data files in " + str);
            }
            addLocaleIDsFromListFile(str2, classLoader, hashSet);
        }
        if (hashSet.isEmpty()) {
            addLocaleIDsFromIndexBundle(str, classLoader, hashSet);
        }
        hashSet.remove("root");
        hashSet.add(ULocale.ROOT.toString());
        return Collections.unmodifiableSet(hashSet);
    }

    private static Set<String> createLocaleNameSet(String str, ClassLoader classLoader) {
        HashSet hashSet = new HashSet();
        addLocaleIDsFromIndexBundle(str, classLoader, hashSet);
        return Collections.unmodifiableSet(hashSet);
    }

    private static final class AvailEntry {
        private volatile Set<String> fullNameSet;
        private ClassLoader loader;
        private volatile Locale[] locales;
        private volatile Set<String> nameSet;
        private String prefix;
        private volatile ULocale[] ulocales;

        AvailEntry(String str, ClassLoader classLoader) {
            this.prefix = str;
            this.loader = classLoader;
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

    private static AvailEntry getAvailEntry(String str, ClassLoader classLoader) {
        return GET_AVAILABLE_CACHE.getInstance(str, classLoader);
    }

    private static final ICUResourceBundle findResourceWithFallback(String str, UResourceBundle uResourceBundle, UResourceBundle uResourceBundle2) {
        if (str.length() == 0) {
            return null;
        }
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) uResourceBundle;
        int resDepth = iCUResourceBundle.getResDepth();
        int iCountPathKeys = countPathKeys(str);
        String[] strArr = new String[resDepth + iCountPathKeys];
        getResPathKeys(str, iCountPathKeys, strArr, resDepth);
        return findResourceWithFallback(strArr, resDepth, iCUResourceBundle, uResourceBundle2);
    }

    private static final ICUResourceBundle findResourceWithFallback(String[] strArr, int i, ICUResourceBundle iCUResourceBundle, UResourceBundle uResourceBundle) {
        if (uResourceBundle == null) {
            uResourceBundle = iCUResourceBundle;
        }
        while (true) {
            int i2 = i + 1;
            ICUResourceBundle iCUResourceBundle2 = (ICUResourceBundle) iCUResourceBundle.handleGet(strArr[i], (HashMap<String, String>) null, uResourceBundle);
            if (iCUResourceBundle2 == null) {
                int i3 = i2 - 1;
                ICUResourceBundle parent = iCUResourceBundle.getParent();
                if (parent == null) {
                    return null;
                }
                int resDepth = iCUResourceBundle.getResDepth();
                if (i3 != resDepth) {
                    String[] strArr2 = new String[(strArr.length - i3) + resDepth];
                    System.arraycopy(strArr, i3, strArr2, resDepth, strArr.length - i3);
                    strArr = strArr2;
                }
                iCUResourceBundle.getResPathKeys(strArr, resDepth);
                iCUResourceBundle = parent;
                i = 0;
            } else if (i2 != strArr.length) {
                iCUResourceBundle = iCUResourceBundle2;
                i = i2;
            } else {
                return iCUResourceBundle2;
            }
        }
    }

    private static final String findStringWithFallback(String str, UResourceBundle uResourceBundle, UResourceBundle uResourceBundle2) {
        ICUResourceBundleReader.Container array;
        ICUResourceBundle aliasedResource;
        ICUResourceBundle parent;
        if (str.length() == 0 || !(uResourceBundle instanceof ICUResourceBundleImpl.ResourceContainer)) {
            return null;
        }
        if (uResourceBundle2 == null) {
            uResourceBundle2 = uResourceBundle;
        }
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) uResourceBundle;
        ICUResourceBundleReader iCUResourceBundleReader = iCUResourceBundle.wholeBundle.reader;
        int resDepth = iCUResourceBundle.getResDepth();
        int iCountPathKeys = countPathKeys(str);
        String[] strArr = new String[resDepth + iCountPathKeys];
        getResPathKeys(str, iCountPathKeys, strArr, resDepth);
        ICUResourceBundleReader iCUResourceBundleReader2 = iCUResourceBundleReader;
        int i = resDepth;
        String[] strArr2 = strArr;
        ICUResourceBundle iCUResourceBundle2 = iCUResourceBundle;
        int resource = -1;
        while (true) {
            if (resource == -1) {
                int type = iCUResourceBundle2.getType();
                if (type == 2 || type == 8) {
                    array = ((ICUResourceBundleImpl.ResourceContainer) iCUResourceBundle2).value;
                    int i2 = resDepth + 1;
                    String str2 = strArr2[resDepth];
                    resource = array.getResource(iCUResourceBundleReader2, str2);
                    if (resource == -1) {
                    }
                } else {
                    parent = iCUResourceBundle2.getParent();
                    if (parent == null) {
                    }
                }
            } else {
                int iRES_GET_TYPE = ICUResourceBundleReader.RES_GET_TYPE(resource);
                if (ICUResourceBundleReader.URES_IS_TABLE(iRES_GET_TYPE)) {
                    array = iCUResourceBundleReader2.getTable(resource);
                } else if (ICUResourceBundleReader.URES_IS_ARRAY(iRES_GET_TYPE)) {
                    array = iCUResourceBundleReader2.getArray(resource);
                } else {
                    resource = -1;
                    parent = iCUResourceBundle2.getParent();
                    if (parent == null) {
                        return null;
                    }
                    iCUResourceBundle2.getResPathKeys(strArr2, i);
                    iCUResourceBundleReader2 = parent.wholeBundle.reader;
                    i = 0;
                    iCUResourceBundle2 = parent;
                    resDepth = 0;
                }
                int i22 = resDepth + 1;
                String str22 = strArr2[resDepth];
                resource = array.getResource(iCUResourceBundleReader2, str22);
                if (resource == -1) {
                    if (ICUResourceBundleReader.RES_GET_TYPE(resource) == 3) {
                        iCUResourceBundle2.getResPathKeys(strArr2, i);
                        aliasedResource = getAliasedResource(iCUResourceBundle2, strArr2, i22, str22, resource, null, uResourceBundle2);
                    } else {
                        aliasedResource = null;
                    }
                    if (i22 == strArr2.length) {
                        if (aliasedResource != null) {
                            return aliasedResource.getString();
                        }
                        String string = iCUResourceBundleReader2.getString(resource);
                        if (string == null) {
                            throw new UResourceTypeMismatchException("");
                        }
                        return string;
                    }
                    if (aliasedResource != null) {
                        ICUResourceBundleReader iCUResourceBundleReader3 = aliasedResource.wholeBundle.reader;
                        int resDepth2 = aliasedResource.getResDepth();
                        if (i22 == resDepth2) {
                            iCUResourceBundleReader2 = iCUResourceBundleReader3;
                            i = resDepth2;
                            iCUResourceBundle2 = aliasedResource;
                            resDepth = i22;
                            resource = -1;
                        } else {
                            String[] strArr3 = new String[(strArr2.length - i22) + resDepth2];
                            System.arraycopy(strArr2, i22, strArr3, resDepth2, strArr2.length - i22);
                            iCUResourceBundleReader2 = iCUResourceBundleReader3;
                            i = resDepth2;
                            strArr2 = strArr3;
                            resource = -1;
                            iCUResourceBundle2 = aliasedResource;
                            resDepth = i;
                        }
                    } else {
                        resDepth = i22;
                    }
                } else {
                    parent = iCUResourceBundle2.getParent();
                    if (parent == null) {
                    }
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

    private void getResPathKeys(String[] strArr, int i) {
        ICUResourceBundle iCUResourceBundle = this;
        while (i > 0) {
            i--;
            strArr[i] = iCUResourceBundle.key;
            iCUResourceBundle = iCUResourceBundle.container;
        }
    }

    private static int countPathKeys(String str) {
        if (str.isEmpty()) {
            return 0;
        }
        int i = 1;
        for (int i2 = 0; i2 < str.length(); i2++) {
            if (str.charAt(i2) == '/') {
                i++;
            }
        }
        return i;
    }

    private static void getResPathKeys(String str, int i, String[] strArr, int i2) {
        if (i == 0) {
            return;
        }
        if (i == 1) {
            strArr[i2] = str;
            return;
        }
        int i3 = 0;
        while (true) {
            int iIndexOf = str.indexOf(47, i3);
            int i4 = i2 + 1;
            strArr[i2] = str.substring(i3, iIndexOf);
            if (i == 2) {
                strArr[i4] = str.substring(iIndexOf + 1);
                return;
            } else {
                i3 = iIndexOf + 1;
                i--;
                i2 = i4;
            }
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ICUResourceBundle) {
            ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) obj;
            if (getBaseName().equals(iCUResourceBundle.getBaseName()) && getLocaleID().equals(iCUResourceBundle.getLocaleID())) {
                return true;
            }
            return false;
        }
        return false;
    }

    public int hashCode() {
        return 42;
    }

    public static ICUResourceBundle getBundleInstance(String str, String str2, ClassLoader classLoader, boolean z) {
        return getBundleInstance(str, str2, classLoader, z ? OpenType.DIRECT : OpenType.LOCALE_DEFAULT_ROOT);
    }

    public static ICUResourceBundle getBundleInstance(String str, ULocale uLocale, OpenType openType) {
        if (uLocale == null) {
            uLocale = ULocale.getDefault();
        }
        return getBundleInstance(str, uLocale.getBaseName(), ICU_DATA_CLASS_LOADER, openType);
    }

    public static ICUResourceBundle getBundleInstance(String str, String str2, ClassLoader classLoader, OpenType openType) {
        ICUResourceBundle iCUResourceBundleInstantiateBundle;
        if (str == null) {
            str = ICUData.ICU_BASE_NAME;
        }
        String baseName = ULocale.getBaseName(str2);
        if (openType == OpenType.LOCALE_DEFAULT_ROOT) {
            iCUResourceBundleInstantiateBundle = instantiateBundle(str, baseName, ULocale.getDefault().getBaseName(), classLoader, openType);
        } else {
            iCUResourceBundleInstantiateBundle = instantiateBundle(str, baseName, null, classLoader, openType);
        }
        if (iCUResourceBundleInstantiateBundle == null) {
            throw new MissingResourceException("Could not find the bundle " + str + RES_PATH_SEP_STR + baseName + ".res", "", "");
        }
        return iCUResourceBundleInstantiateBundle;
    }

    private static boolean localeIDStartsWithLangSubtag(String str, String str2) {
        return str.startsWith(str2) && (str.length() == str2.length() || str.charAt(str2.length()) == '_');
    }

    private static ICUResourceBundle instantiateBundle(final String str, final String str2, final String str3, final ClassLoader classLoader, final OpenType openType) {
        String str4;
        final String fullName = ICUResourceBundleReader.getFullName(str, str2);
        char cOrdinal = (char) (48 + openType.ordinal());
        if (openType != OpenType.LOCALE_DEFAULT_ROOT) {
            str4 = fullName + '#' + cOrdinal;
        } else {
            str4 = fullName + '#' + cOrdinal + '#' + str3;
        }
        return BUNDLE_CACHE.getInstance(str4, new Loader() {
            {
                super();
            }

            @Override
            public ICUResourceBundle load() {
                String str5;
                ICUResourceBundle iCUResourceBundleCreateBundle;
                if (ICUResourceBundle.DEBUG) {
                    System.out.println("Creating " + fullName);
                }
                String str6 = str.indexOf(46) == -1 ? "root" : "";
                if (!str2.isEmpty()) {
                    str5 = str2;
                } else {
                    str5 = str6;
                }
                ICUResourceBundle iCUResourceBundleCreateBundle2 = ICUResourceBundle.createBundle(str, str5, classLoader);
                if (ICUResourceBundle.DEBUG) {
                    PrintStream printStream = System.out;
                    StringBuilder sb = new StringBuilder();
                    sb.append("The bundle created is: ");
                    sb.append(iCUResourceBundleCreateBundle2);
                    sb.append(" and openType=");
                    sb.append(openType);
                    sb.append(" and bundle.getNoFallback=");
                    sb.append(iCUResourceBundleCreateBundle2 != null && iCUResourceBundleCreateBundle2.getNoFallback());
                    printStream.println(sb.toString());
                }
                if (openType == OpenType.DIRECT || (iCUResourceBundleCreateBundle2 != null && iCUResourceBundleCreateBundle2.getNoFallback())) {
                    return iCUResourceBundleCreateBundle2;
                }
                if (iCUResourceBundleCreateBundle2 == null) {
                    int iLastIndexOf = str5.lastIndexOf(95);
                    if (iLastIndexOf != -1) {
                        iCUResourceBundleCreateBundle = ICUResourceBundle.instantiateBundle(str, str5.substring(0, iLastIndexOf), str3, classLoader, openType);
                    } else if (openType == OpenType.LOCALE_DEFAULT_ROOT && !ICUResourceBundle.localeIDStartsWithLangSubtag(str3, str5)) {
                        iCUResourceBundleCreateBundle = ICUResourceBundle.instantiateBundle(str, str3, str3, classLoader, openType);
                    } else if (openType != OpenType.LOCALE_ONLY && !str6.isEmpty()) {
                        iCUResourceBundleCreateBundle = ICUResourceBundle.createBundle(str, str6, classLoader);
                    } else {
                        return iCUResourceBundleCreateBundle2;
                    }
                    return iCUResourceBundleCreateBundle;
                }
                ICUResourceBundle iCUResourceBundleInstantiateBundle = null;
                String localeID = iCUResourceBundleCreateBundle2.getLocaleID();
                int iLastIndexOf2 = localeID.lastIndexOf(95);
                String strFindString = ((ICUResourceBundleImpl.ResourceTable) iCUResourceBundleCreateBundle2).findString("%%Parent");
                if (strFindString != null) {
                    iCUResourceBundleInstantiateBundle = ICUResourceBundle.instantiateBundle(str, strFindString, str3, classLoader, openType);
                } else if (iLastIndexOf2 != -1) {
                    iCUResourceBundleInstantiateBundle = ICUResourceBundle.instantiateBundle(str, localeID.substring(0, iLastIndexOf2), str3, classLoader, openType);
                } else if (!localeID.equals(str6)) {
                    iCUResourceBundleInstantiateBundle = ICUResourceBundle.instantiateBundle(str, str6, str3, classLoader, openType);
                }
                if (!iCUResourceBundleCreateBundle2.equals(iCUResourceBundleInstantiateBundle)) {
                    iCUResourceBundleCreateBundle2.setParent(iCUResourceBundleInstantiateBundle);
                    return iCUResourceBundleCreateBundle2;
                }
                return iCUResourceBundleCreateBundle2;
            }
        });
    }

    ICUResourceBundle get(String str, HashMap<String, String> map, UResourceBundle uResourceBundle) {
        ICUResourceBundle parent = (ICUResourceBundle) handleGet(str, map, uResourceBundle);
        if (parent == null) {
            parent = getParent();
            if (parent != null) {
                parent = parent.get(str, map, uResourceBundle);
            }
            if (parent == null) {
                throw new MissingResourceException("Can't find resource for bundle " + ICUResourceBundleReader.getFullName(getBaseName(), getLocaleID()) + ", key " + str, getClass().getName(), str);
            }
        }
        return parent;
    }

    public static ICUResourceBundle createBundle(String str, String str2, ClassLoader classLoader) {
        ICUResourceBundleReader reader = ICUResourceBundleReader.getReader(str, str2, classLoader);
        if (reader == null) {
            return null;
        }
        return getBundle(reader, str, str2, classLoader);
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

    public boolean isRoot() {
        return this.wholeBundle.localeID.isEmpty() || this.wholeBundle.localeID.equals("root");
    }

    @Override
    public ICUResourceBundle getParent() {
        return (ICUResourceBundle) this.parent;
    }

    @Override
    protected void setParent(ResourceBundle resourceBundle) {
        this.parent = resourceBundle;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    private boolean getNoFallback() {
        return this.wholeBundle.reader.getNoFallback();
    }

    private static ICUResourceBundle getBundle(ICUResourceBundleReader iCUResourceBundleReader, String str, String str2, ClassLoader classLoader) {
        int rootResource = iCUResourceBundleReader.getRootResource();
        if (ICUResourceBundleReader.URES_IS_TABLE(ICUResourceBundleReader.RES_GET_TYPE(rootResource))) {
            ICUResourceBundleImpl.ResourceTable resourceTable = new ICUResourceBundleImpl.ResourceTable(new WholeBundle(str, str2, classLoader, iCUResourceBundleReader), rootResource);
            String strFindString = resourceTable.findString("%%ALIAS");
            if (strFindString != null) {
                return (ICUResourceBundle) UResourceBundle.getBundleInstance(str, strFindString);
            }
            return resourceTable;
        }
        throw new IllegalStateException("Invalid format error");
    }

    protected ICUResourceBundle(WholeBundle wholeBundle) {
        this.wholeBundle = wholeBundle;
    }

    protected ICUResourceBundle(ICUResourceBundle iCUResourceBundle, String str) {
        this.key = str;
        this.wholeBundle = iCUResourceBundle.wholeBundle;
        this.container = iCUResourceBundle;
        this.parent = iCUResourceBundle.parent;
    }

    protected static ICUResourceBundle getAliasedResource(ICUResourceBundle iCUResourceBundle, String[] strArr, int i, String str, int i2, HashMap<String, String> map, UResourceBundle uResourceBundle) {
        HashMap<String, String> map2;
        String strSubstring;
        String strSubstring2;
        String strSubstring3;
        int iCountPathKeys;
        String[] strArr2;
        int iIndexOf;
        WholeBundle wholeBundle = iCUResourceBundle.wholeBundle;
        ClassLoader classLoader = wholeBundle.loader;
        String alias = wholeBundle.reader.getAlias(i2);
        if (map == null) {
            map2 = new HashMap<>();
        } else {
            map2 = map;
        }
        if (map2.get(alias) != null) {
            throw new IllegalArgumentException("Circular references in the resource bundles");
        }
        map2.put(alias, "");
        ICUResourceBundle iCUResourceBundleFindResourceWithFallback = null;
        if (alias.indexOf(47) == 0) {
            int iIndexOf2 = alias.indexOf(47, 1);
            int i3 = iIndexOf2 + 1;
            int iIndexOf3 = alias.indexOf(47, i3);
            strSubstring3 = alias.substring(1, iIndexOf2);
            if (iIndexOf3 < 0) {
                strSubstring = alias.substring(i3);
                strSubstring2 = null;
            } else {
                String strSubstring4 = alias.substring(i3, iIndexOf3);
                strSubstring2 = alias.substring(iIndexOf3 + 1, alias.length());
                strSubstring = strSubstring4;
            }
            if (strSubstring3.equals(ICUDATA)) {
                strSubstring3 = ICUData.ICU_BASE_NAME;
                classLoader = ICU_DATA_CLASS_LOADER;
            } else if (strSubstring3.indexOf(ICUDATA) > -1 && (iIndexOf = strSubstring3.indexOf(45)) > -1) {
                strSubstring3 = "android/icu/impl/data/icudt60b/" + strSubstring3.substring(iIndexOf + 1, strSubstring3.length());
                classLoader = ICU_DATA_CLASS_LOADER;
            }
        } else {
            int iIndexOf4 = alias.indexOf(47);
            if (iIndexOf4 != -1) {
                String strSubstring5 = alias.substring(0, iIndexOf4);
                strSubstring2 = alias.substring(iIndexOf4 + 1);
                strSubstring = strSubstring5;
            } else {
                strSubstring = alias;
                strSubstring2 = null;
            }
            strSubstring3 = wholeBundle.baseName;
        }
        if (strSubstring3.equals(LOCALE)) {
            String str2 = wholeBundle.baseName;
            String strSubstring6 = alias.substring(LOCALE.length() + 2, alias.length());
            ICUResourceBundle iCUResourceBundle2 = (ICUResourceBundle) uResourceBundle;
            while (iCUResourceBundle2.container != null) {
                iCUResourceBundle2 = iCUResourceBundle2.container;
            }
            iCUResourceBundleFindResourceWithFallback = findResourceWithFallback(strSubstring6, iCUResourceBundle2, null);
        } else {
            ICUResourceBundle bundleInstance = getBundleInstance(strSubstring3, strSubstring, classLoader, false);
            if (strSubstring2 != null) {
                iCountPathKeys = countPathKeys(strSubstring2);
                if (iCountPathKeys > 0) {
                    strArr2 = new String[iCountPathKeys];
                    getResPathKeys(strSubstring2, iCountPathKeys, strArr2, 0);
                } else {
                    strArr2 = strArr;
                }
            } else if (strArr == null) {
                int resDepth = iCUResourceBundle.getResDepth();
                int i4 = resDepth + 1;
                String[] strArr3 = new String[i4];
                iCUResourceBundle.getResPathKeys(strArr3, resDepth);
                strArr3[resDepth] = str;
                iCountPathKeys = i4;
                strArr2 = strArr3;
            } else {
                strArr2 = strArr;
                iCountPathKeys = i;
            }
            if (iCountPathKeys > 0) {
                iCUResourceBundleFindResourceWithFallback = bundleInstance;
                for (int i5 = 0; iCUResourceBundleFindResourceWithFallback != null && i5 < iCountPathKeys; i5++) {
                    iCUResourceBundleFindResourceWithFallback = iCUResourceBundleFindResourceWithFallback.get(strArr2[i5], map2, uResourceBundle);
                }
            }
        }
        if (iCUResourceBundleFindResourceWithFallback == null) {
            throw new MissingResourceException(wholeBundle.localeID, wholeBundle.baseName, str);
        }
        return iCUResourceBundleFindResourceWithFallback;
    }

    @Deprecated
    public final Set<String> getTopLevelKeySet() {
        return this.wholeBundle.topLevelKeys;
    }

    @Deprecated
    public final void setTopLevelKeySet(Set<String> set) {
        this.wholeBundle.topLevelKeys = set;
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
