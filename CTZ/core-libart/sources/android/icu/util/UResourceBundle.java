package android.icu.util;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUResourceBundleReader;
import android.icu.impl.ResourceBundleWrapper;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
    private static Map<String, RootType> ROOT_CACHE = new ConcurrentHashMap();
    public static final int STRING = 0;
    public static final int TABLE = 2;

    private enum RootType {
        MISSING,
        ICU,
        JAVA
    }

    protected abstract String getBaseName();

    protected abstract String getLocaleID();

    protected abstract UResourceBundle getParent();

    public abstract ULocale getULocale();

    public static UResourceBundle getBundleInstance(String str, String str2) {
        return getBundleInstance(str, str2, ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String str, String str2, ClassLoader classLoader) {
        return getBundleInstance(str, str2, classLoader, false);
    }

    protected static UResourceBundle getBundleInstance(String str, String str2, ClassLoader classLoader, boolean z) {
        return instantiateBundle(str, str2, classLoader, z);
    }

    public static UResourceBundle getBundleInstance(ULocale uLocale) {
        if (uLocale == null) {
            uLocale = ULocale.getDefault();
        }
        return getBundleInstance(ICUData.ICU_BASE_NAME, uLocale.getBaseName(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String str) {
        if (str == null) {
            str = ICUData.ICU_BASE_NAME;
        }
        return getBundleInstance(str, ULocale.getDefault().getBaseName(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String str, Locale locale) {
        if (str == null) {
            str = ICUData.ICU_BASE_NAME;
        }
        return getBundleInstance(str, (locale == null ? ULocale.getDefault() : ULocale.forLocale(locale)).getBaseName(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String str, ULocale uLocale) {
        if (str == null) {
            str = ICUData.ICU_BASE_NAME;
        }
        if (uLocale == null) {
            uLocale = ULocale.getDefault();
        }
        return getBundleInstance(str, uLocale.getBaseName(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }

    public static UResourceBundle getBundleInstance(String str, Locale locale, ClassLoader classLoader) {
        if (str == null) {
            str = ICUData.ICU_BASE_NAME;
        }
        return getBundleInstance(str, (locale == null ? ULocale.getDefault() : ULocale.forLocale(locale)).getBaseName(), classLoader, false);
    }

    public static UResourceBundle getBundleInstance(String str, ULocale uLocale, ClassLoader classLoader) {
        if (str == null) {
            str = ICUData.ICU_BASE_NAME;
        }
        if (uLocale == null) {
            uLocale = ULocale.getDefault();
        }
        return getBundleInstance(str, uLocale.getBaseName(), classLoader, false);
    }

    @Override
    public Locale getLocale() {
        return getULocale().toLocale();
    }

    private static RootType getRootType(String str, ClassLoader classLoader) {
        RootType rootType;
        RootType rootType2 = ROOT_CACHE.get(str);
        if (rootType2 == null) {
            String str2 = str.indexOf(46) == -1 ? "root" : "";
            try {
                ICUResourceBundle.getBundleInstance(str, str2, classLoader, true);
                rootType2 = RootType.ICU;
            } catch (MissingResourceException e) {
                try {
                    ResourceBundleWrapper.getBundleInstance(str, str2, classLoader, true);
                    rootType = RootType.JAVA;
                } catch (MissingResourceException e2) {
                    rootType = RootType.MISSING;
                }
                rootType2 = rootType;
            }
            ROOT_CACHE.put(str, rootType2);
        }
        return rootType2;
    }

    private static void setRootType(String str, RootType rootType) {
        ROOT_CACHE.put(str, rootType);
    }

    protected static UResourceBundle instantiateBundle(String str, String str2, ClassLoader classLoader, boolean z) {
        switch (getRootType(str, classLoader)) {
            case ICU:
                return ICUResourceBundle.getBundleInstance(str, str2, classLoader, z);
            case JAVA:
                return ResourceBundleWrapper.getBundleInstance(str, str2, classLoader, z);
            default:
                try {
                    ICUResourceBundle bundleInstance = ICUResourceBundle.getBundleInstance(str, str2, classLoader, z);
                    setRootType(str, RootType.ICU);
                    return bundleInstance;
                } catch (MissingResourceException e) {
                    ResourceBundleWrapper bundleInstance2 = ResourceBundleWrapper.getBundleInstance(str, str2, classLoader, z);
                    setRootType(str, RootType.JAVA);
                    return bundleInstance2;
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

    public byte[] getBinary(byte[] bArr) {
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

    public UResourceBundle get(String str) {
        UResourceBundle uResourceBundleFindTopLevel = findTopLevel(str);
        if (uResourceBundleFindTopLevel == null) {
            throw new MissingResourceException("Can't find resource for bundle " + ICUResourceBundleReader.getFullName(getBaseName(), getLocaleID()) + ", key " + str, getClass().getName(), str);
        }
        return uResourceBundleFindTopLevel;
    }

    @Deprecated
    protected UResourceBundle findTopLevel(String str) {
        for (UResourceBundle parent = this; parent != null; parent = parent.getParent()) {
            UResourceBundle uResourceBundleHandleGet = parent.handleGet(str, (HashMap<String, String>) null, this);
            if (uResourceBundleHandleGet != null) {
                return uResourceBundleHandleGet;
            }
        }
        return null;
    }

    public String getString(int i) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) get(i);
        if (iCUResourceBundle.getType() == 0) {
            return iCUResourceBundle.getString();
        }
        throw new UResourceTypeMismatchException("");
    }

    public UResourceBundle get(int i) {
        UResourceBundle uResourceBundleHandleGet = handleGet(i, (HashMap<String, String>) null, this);
        if (uResourceBundleHandleGet == null) {
            uResourceBundleHandleGet = getParent();
            if (uResourceBundleHandleGet != null) {
                uResourceBundleHandleGet = uResourceBundleHandleGet.get(i);
            }
            if (uResourceBundleHandleGet == null) {
                throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getKey(), getClass().getName(), getKey());
            }
        }
        return uResourceBundleHandleGet;
    }

    @Deprecated
    protected UResourceBundle findTopLevel(int i) {
        for (UResourceBundle parent = this; parent != null; parent = parent.getParent()) {
            UResourceBundle uResourceBundleHandleGet = parent.handleGet(i, (HashMap<String, String>) null, this);
            if (uResourceBundleHandleGet != null) {
                return uResourceBundleHandleGet;
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
        Set<String> setUnmodifiableSet;
        TreeSet treeSet;
        ICUResourceBundle iCUResourceBundle = null;
        if (isTopLevelResource() && (this instanceof ICUResourceBundle)) {
            iCUResourceBundle = (ICUResourceBundle) this;
            setUnmodifiableSet = iCUResourceBundle.getTopLevelKeySet();
        } else {
            setUnmodifiableSet = null;
        }
        if (setUnmodifiableSet == null) {
            if (isTopLevelResource()) {
                if (this.parent == null) {
                    treeSet = new TreeSet();
                } else if (this.parent instanceof UResourceBundle) {
                    treeSet = new TreeSet(((UResourceBundle) this.parent).keySet());
                } else {
                    treeSet = new TreeSet();
                    Enumeration<String> keys = this.parent.getKeys();
                    while (keys.hasMoreElements()) {
                        treeSet.add(keys.nextElement());
                    }
                }
                treeSet.addAll(handleKeySet());
                setUnmodifiableSet = Collections.unmodifiableSet(treeSet);
                if (iCUResourceBundle != null) {
                    iCUResourceBundle.setTopLevelKeySet(setUnmodifiableSet);
                }
            } else {
                return handleKeySet();
            }
        }
        return setUnmodifiableSet;
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

    protected UResourceBundle handleGet(String str, HashMap<String, String> map, UResourceBundle uResourceBundle) {
        return null;
    }

    protected UResourceBundle handleGet(int i, HashMap<String, String> map, UResourceBundle uResourceBundle) {
        return null;
    }

    protected String[] handleGetStringArray() {
        return null;
    }

    protected Enumeration<String> handleGetKeys() {
        return null;
    }

    @Override
    protected Object handleGetObject(String str) {
        return handleGetObjectImpl(str, this);
    }

    private Object handleGetObjectImpl(String str, UResourceBundle uResourceBundle) {
        Object objResolveObject = resolveObject(str, uResourceBundle);
        if (objResolveObject == null) {
            UResourceBundle parent = getParent();
            if (parent != null) {
                objResolveObject = parent.handleGetObjectImpl(str, uResourceBundle);
            }
            if (objResolveObject == null) {
                throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + str, getClass().getName(), str);
            }
        }
        return objResolveObject;
    }

    private Object resolveObject(String str, UResourceBundle uResourceBundle) {
        if (getType() == 0) {
            return getString();
        }
        UResourceBundle uResourceBundleHandleGet = handleGet(str, (HashMap<String, String>) null, uResourceBundle);
        if (uResourceBundleHandleGet != null) {
            if (uResourceBundleHandleGet.getType() == 0) {
                return uResourceBundleHandleGet.getString();
            }
            try {
                if (uResourceBundleHandleGet.getType() == 8) {
                    return uResourceBundleHandleGet.handleGetStringArray();
                }
            } catch (UResourceTypeMismatchException e) {
                return uResourceBundleHandleGet;
            }
        }
        return uResourceBundleHandleGet;
    }

    @Deprecated
    protected boolean isTopLevelResource() {
        return true;
    }
}
