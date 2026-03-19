package java.security;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import sun.security.jca.GetInstance;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

public final class Security {
    private static final Map<String, Class<?>> spiMap;
    private static final AtomicInteger version = new AtomicInteger();
    private static final Properties props = new Properties();

    static {
        boolean z = false;
        BufferedInputStream bufferedInputStream = null;
        try {
            try {
                InputStream resourceAsStream = Security.class.getResourceAsStream("security.properties");
                if (resourceAsStream == null) {
                    System.logE("Could not find 'security.properties'.");
                } else {
                    BufferedInputStream bufferedInputStream2 = new BufferedInputStream(resourceAsStream);
                    try {
                        props.load(bufferedInputStream2);
                        z = true;
                        bufferedInputStream = bufferedInputStream2;
                    } catch (IOException e) {
                        e = e;
                        bufferedInputStream = bufferedInputStream2;
                        System.logE("Could not load 'security.properties'", e);
                        if (bufferedInputStream != null) {
                        }
                    } catch (Throwable th) {
                        th = th;
                        bufferedInputStream = bufferedInputStream2;
                        if (bufferedInputStream != null) {
                            try {
                                bufferedInputStream.close();
                            } catch (IOException e2) {
                            }
                        }
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e3) {
            e = e3;
        }
        if (bufferedInputStream != null) {
            try {
                bufferedInputStream.close();
            } catch (IOException e4) {
            }
        }
        if (!z) {
            initializeStatic();
        }
        spiMap = new ConcurrentHashMap();
    }

    private static class ProviderProperty {
        String className;
        Provider provider;

        private ProviderProperty() {
        }
    }

    private static void initializeStatic() {
        props.put("security.provider.1", "com.android.org.conscrypt.OpenSSLProvider");
        props.put("security.provider.2", "sun.security.provider.CertPathProvider");
        props.put("security.provider.3", "com.android.org.bouncycastle.jce.provider.BouncyCastleProvider");
        props.put("security.provider.4", "com.android.org.conscrypt.JSSEProvider");
    }

    private Security() {
    }

    private static ProviderProperty getProviderProperty(String str) {
        List<Provider> listProviders = Providers.getProviderList().providers();
        int i = 0;
        while (true) {
            if (i >= listProviders.size()) {
                return null;
            }
            Provider provider = listProviders.get(i);
            String property = provider.getProperty(str);
            if (property == null) {
                Enumeration<Object> enumerationKeys = provider.keys();
                while (true) {
                    if (!enumerationKeys.hasMoreElements() || property != null) {
                        break;
                    }
                    String str2 = (String) enumerationKeys.nextElement();
                    if (str.equalsIgnoreCase(str2)) {
                        property = provider.getProperty(str2);
                        break;
                    }
                }
            }
            if (property == null) {
                i++;
            } else {
                ProviderProperty providerProperty = new ProviderProperty();
                providerProperty.className = property;
                providerProperty.provider = provider;
                return providerProperty;
            }
        }
    }

    private static String getProviderProperty(String str, Provider provider) {
        String property = provider.getProperty(str);
        if (property == null) {
            Enumeration<Object> enumerationKeys = provider.keys();
            while (enumerationKeys.hasMoreElements() && property == null) {
                String str2 = (String) enumerationKeys.nextElement();
                if (str.equalsIgnoreCase(str2)) {
                    return provider.getProperty(str2);
                }
            }
            return property;
        }
        return property;
    }

    @Deprecated
    public static String getAlgorithmProperty(String str, String str2) {
        ProviderProperty providerProperty = getProviderProperty("Alg." + str2 + "." + str);
        if (providerProperty != null) {
            return providerProperty.className;
        }
        return null;
    }

    public static synchronized int insertProviderAt(Provider provider, int i) {
        String name = provider.getName();
        ProviderList fullProviderList = Providers.getFullProviderList();
        ProviderList providerListInsertAt = ProviderList.insertAt(fullProviderList, provider, i - 1);
        if (fullProviderList == providerListInsertAt) {
            return -1;
        }
        increaseVersion();
        Providers.setProviderList(providerListInsertAt);
        return providerListInsertAt.getIndex(name) + 1;
    }

    public static int addProvider(Provider provider) {
        return insertProviderAt(provider, 0);
    }

    public static synchronized void removeProvider(String str) {
        Providers.setProviderList(ProviderList.remove(Providers.getFullProviderList(), str));
        increaseVersion();
    }

    public static Provider[] getProviders() {
        return Providers.getFullProviderList().toArray();
    }

    public static Provider getProvider(String str) {
        return Providers.getProviderList().getProvider(str);
    }

    public static Provider[] getProviders(String str) {
        String strSubstring;
        int iIndexOf = str.indexOf(58);
        if (iIndexOf == -1) {
            strSubstring = "";
        } else {
            String strSubstring2 = str.substring(0, iIndexOf);
            strSubstring = str.substring(iIndexOf + 1);
            str = strSubstring2;
        }
        Hashtable hashtable = new Hashtable(1);
        hashtable.put(str, strSubstring);
        return getProviders(hashtable);
    }

    public static Provider[] getProviders(Map<String, String> map) {
        int i;
        Provider[] providers = getProviders();
        Set<String> setKeySet = map.keySet();
        LinkedHashSet<Provider> linkedHashSet = new LinkedHashSet<>(5);
        if (setKeySet == null || providers == null) {
            return providers;
        }
        Iterator<String> it = setKeySet.iterator();
        boolean z = true;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            String next = it.next();
            LinkedHashSet<Provider> allQualifyingCandidates = getAllQualifyingCandidates(next, map.get(next), providers);
            if (z) {
                linkedHashSet = allQualifyingCandidates;
                z = false;
            }
            if (allQualifyingCandidates == null || allQualifyingCandidates.isEmpty()) {
                break;
            }
            Iterator<Provider> it2 = linkedHashSet.iterator();
            while (it2.hasNext()) {
                if (!allQualifyingCandidates.contains(it2.next())) {
                    it2.remove();
                }
            }
        }
        linkedHashSet = null;
        if (linkedHashSet == null || linkedHashSet.isEmpty()) {
            return null;
        }
        Object[] array = linkedHashSet.toArray();
        Provider[] providerArr = new Provider[array.length];
        for (i = 0; i < providerArr.length; i++) {
            providerArr[i] = (Provider) array[i];
        }
        return providerArr;
    }

    private static Class<?> getSpiClass(String str) {
        Class<?> cls = spiMap.get(str);
        if (cls != null) {
            return cls;
        }
        try {
            Class<?> cls2 = Class.forName("java.security." + str + "Spi");
            spiMap.put(str, cls2);
            return cls2;
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Spi class not found", e);
        }
    }

    static Object[] getImpl(String str, String str2, String str3) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (str3 == null) {
            return GetInstance.getInstance(str2, getSpiClass(str2), str).toArray();
        }
        return GetInstance.getInstance(str2, getSpiClass(str2), str, str3).toArray();
    }

    static Object[] getImpl(String str, String str2, String str3, Object obj) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        if (str3 == null) {
            return GetInstance.getInstance(str2, getSpiClass(str2), str, obj).toArray();
        }
        return GetInstance.getInstance(str2, getSpiClass(str2), str, obj, str3).toArray();
    }

    static Object[] getImpl(String str, String str2, Provider provider) throws NoSuchAlgorithmException {
        return GetInstance.getInstance(str2, getSpiClass(str2), str, provider).toArray();
    }

    static Object[] getImpl(String str, String str2, Provider provider, Object obj) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return GetInstance.getInstance(str2, getSpiClass(str2), str, obj, provider).toArray();
    }

    public static String getProperty(String str) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new SecurityPermission("getProperty." + str));
        }
        String property = props.getProperty(str);
        if (property != null) {
            return property.trim();
        }
        return property;
    }

    public static void setProperty(String str, String str2) {
        props.put(str, str2);
        increaseVersion();
        invalidateSMCache(str);
    }

    private static void invalidateSMCache(String str) {
        final boolean zEquals = str.equals("package.access");
        boolean zEquals2 = str.equals("package.definition");
        if (zEquals || zEquals2) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    boolean zIsAccessible;
                    Field field;
                    try {
                        Class<?> cls = Class.forName("java.lang.SecurityManager", false, null);
                        if (zEquals) {
                            Field declaredField = cls.getDeclaredField("packageAccessValid");
                            zIsAccessible = declaredField.isAccessible();
                            declaredField.setAccessible(true);
                            field = declaredField;
                        } else {
                            Field declaredField2 = cls.getDeclaredField("packageDefinitionValid");
                            zIsAccessible = declaredField2.isAccessible();
                            declaredField2.setAccessible(true);
                            field = declaredField2;
                        }
                        field.setBoolean(field, false);
                        field.setAccessible(zIsAccessible);
                    } catch (Exception e) {
                    }
                    return null;
                }
            });
        }
    }

    private static LinkedHashSet<Provider> getAllQualifyingCandidates(String str, String str2, Provider[] providerArr) {
        String[] filterComponents = getFilterComponents(str, str2);
        return getProvidersNotUsingCache(filterComponents[0], filterComponents[1], filterComponents[2], str2, providerArr);
    }

    private static LinkedHashSet<Provider> getProvidersNotUsingCache(String str, String str2, String str3, String str4, Provider[] providerArr) {
        LinkedHashSet<Provider> linkedHashSet = new LinkedHashSet<>(5);
        for (int i = 0; i < providerArr.length; i++) {
            if (isCriterionSatisfied(providerArr[i], str, str2, str3, str4)) {
                linkedHashSet.add(providerArr[i]);
            }
        }
        return linkedHashSet;
    }

    private static boolean isCriterionSatisfied(Provider provider, String str, String str2, String str3, String str4) {
        String str5 = str + '.' + str2;
        if (str3 != null) {
            str5 = str5 + ' ' + str3;
        }
        String providerProperty = getProviderProperty(str5, provider);
        if (providerProperty == null) {
            String providerProperty2 = getProviderProperty("Alg.Alias." + str + "." + str2, provider);
            if (providerProperty2 != null) {
                String str6 = str + "." + providerProperty2;
                if (str3 != null) {
                    str6 = str6 + ' ' + str3;
                }
                providerProperty = getProviderProperty(str6, provider);
            }
            if (providerProperty == null) {
                return false;
            }
        }
        if (str3 == null) {
            return true;
        }
        if (isStandardAttr(str3)) {
            return isConstraintSatisfied(str3, str4, providerProperty);
        }
        return str4.equalsIgnoreCase(providerProperty);
    }

    private static boolean isStandardAttr(String str) {
        return str.equalsIgnoreCase("KeySize") || str.equalsIgnoreCase("ImplementedIn");
    }

    private static boolean isConstraintSatisfied(String str, String str2, String str3) {
        if (str.equalsIgnoreCase("KeySize")) {
            return Integer.parseInt(str2) <= Integer.parseInt(str3);
        }
        if (str.equalsIgnoreCase("ImplementedIn")) {
            return str2.equalsIgnoreCase(str3);
        }
        return false;
    }

    static String[] getFilterComponents(String str, String str2) {
        String strSubstring;
        int iIndexOf = str.indexOf(46);
        if (iIndexOf < 0) {
            throw new InvalidParameterException("Invalid filter");
        }
        String strSubstring2 = str.substring(0, iIndexOf);
        String strTrim = null;
        if (str2.length() == 0) {
            strSubstring = str.substring(iIndexOf + 1).trim();
            if (strSubstring.length() == 0) {
                throw new InvalidParameterException("Invalid filter");
            }
        } else {
            int iIndexOf2 = str.indexOf(32);
            if (iIndexOf2 == -1) {
                throw new InvalidParameterException("Invalid filter");
            }
            strTrim = str.substring(iIndexOf2 + 1).trim();
            if (strTrim.length() == 0) {
                throw new InvalidParameterException("Invalid filter");
            }
            if (iIndexOf2 < iIndexOf || iIndexOf == iIndexOf2 - 1) {
                throw new InvalidParameterException("Invalid filter");
            }
            strSubstring = str.substring(iIndexOf + 1, iIndexOf2);
        }
        return new String[]{strSubstring2, strSubstring, strTrim};
    }

    public static Set<String> getAlgorithms(String str) {
        if (str == null || str.length() == 0 || str.endsWith(".")) {
            return Collections.emptySet();
        }
        HashSet hashSet = new HashSet();
        for (Provider provider : getProviders()) {
            Enumeration<Object> enumerationKeys = provider.keys();
            while (enumerationKeys.hasMoreElements()) {
                String upperCase = ((String) enumerationKeys.nextElement()).toUpperCase(Locale.ENGLISH);
                if (upperCase.startsWith(str.toUpperCase(Locale.ENGLISH)) && upperCase.indexOf(" ") < 0) {
                    hashSet.add(upperCase.substring(str.length() + 1));
                }
            }
        }
        return Collections.unmodifiableSet(hashSet);
    }

    public static void increaseVersion() {
        version.incrementAndGet();
    }

    public static int getVersion() {
        return version.get();
    }
}
