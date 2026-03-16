package java.security;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.harmony.security.fortress.Engine;
import org.apache.harmony.security.fortress.SecurityAccess;
import org.apache.harmony.security.fortress.Services;

public final class Security {
    private static final Properties secprops = new Properties();

    static {
        boolean loaded = false;
        try {
            InputStream configStream = Security.class.getResourceAsStream("security.properties");
            InputStream input = new BufferedInputStream(configStream);
            secprops.load(input);
            loaded = true;
            configStream.close();
        } catch (Exception ex) {
            System.logE("Could not load 'security.properties'", ex);
        }
        if (!loaded) {
            registerDefaultProviders();
        }
        Engine.door = new SecurityDoor();
    }

    private Security() {
    }

    private static void registerDefaultProviders() {
        secprops.put("security.provider.1", "com.android.org.conscrypt.OpenSSLProvider");
        secprops.put("security.provider.2", "com.android.org.bouncycastle.jce.provider.BouncyCastleProvider");
        secprops.put("security.provider.3", "org.apache.harmony.security.provider.crypto.CryptoProvider");
        secprops.put("security.provider.4", "com.android.org.conscrypt.JSSEProvider");
    }

    @Deprecated
    public static String getAlgorithmProperty(String algName, String propName) {
        if (algName == null || propName == null) {
            return null;
        }
        String prop = "Alg." + propName + "." + algName;
        Provider[] providers = getProviders();
        for (Provider provider : providers) {
            Enumeration<?> e = provider.propertyNames();
            while (e.hasMoreElements()) {
                String propertyName = (String) e.nextElement();
                if (propertyName.equalsIgnoreCase(prop)) {
                    return provider.getProperty(propertyName);
                }
            }
        }
        return null;
    }

    public static synchronized int insertProviderAt(Provider provider, int position) {
        int iInsertProviderAt;
        if (getProvider(provider.getName()) != null) {
            iInsertProviderAt = -1;
        } else {
            iInsertProviderAt = Services.insertProviderAt(provider, position);
            renumProviders();
        }
        return iInsertProviderAt;
    }

    public static int addProvider(Provider provider) {
        return insertProviderAt(provider, 0);
    }

    public static synchronized void removeProvider(String name) {
        Provider p;
        if (name != null) {
            if (name.length() != 0 && (p = getProvider(name)) != null) {
                Services.removeProvider(p.getProviderNumber());
                renumProviders();
                p.setProviderNumber(-1);
            }
        }
    }

    public static synchronized Provider[] getProviders() {
        ArrayList<Provider> providers;
        providers = Services.getProviders();
        return (Provider[]) providers.toArray(new Provider[providers.size()]);
    }

    public static synchronized Provider getProvider(String name) {
        return Services.getProvider(name);
    }

    public static Provider[] getProviders(String filter) {
        if (filter == null) {
            throw new NullPointerException("filter == null");
        }
        if (filter.length() == 0) {
            throw new InvalidParameterException();
        }
        HashMap<String, String> hm = new HashMap<>();
        int i = filter.indexOf(58);
        if (i == filter.length() - 1 || i == 0) {
            throw new InvalidParameterException();
        }
        if (i < 1) {
            hm.put(filter, "");
        } else {
            hm.put(filter.substring(0, i), filter.substring(i + 1));
        }
        return getProviders(hm);
    }

    public static synchronized Provider[] getProviders(Map<String, String> filter) {
        Provider[] providerArr = null;
        synchronized (Security.class) {
            if (filter == null) {
                throw new NullPointerException("filter == null");
            }
            if (!filter.isEmpty()) {
                ArrayList<Provider> result = new ArrayList<>(Services.getProviders());
                Set<Map.Entry<String, String>> keys = filter.entrySet();
                for (Map.Entry<String, String> entry : keys) {
                    String key = entry.getKey();
                    String val = entry.getValue();
                    String attribute = null;
                    int i = key.indexOf(32);
                    int j = key.indexOf(46);
                    if (j == -1) {
                        throw new InvalidParameterException();
                    }
                    if (i == -1) {
                        if (val.length() != 0) {
                            throw new InvalidParameterException();
                        }
                    } else {
                        if (val.length() == 0) {
                            throw new InvalidParameterException();
                        }
                        attribute = key.substring(i + 1);
                        if (attribute.trim().length() == 0) {
                            throw new InvalidParameterException();
                        }
                        key = key.substring(0, i);
                    }
                    String serv = key.substring(0, j);
                    String alg = key.substring(j + 1);
                    if (serv.length() == 0 || alg.length() == 0) {
                        throw new InvalidParameterException();
                    }
                    filterProviders(result, serv, alg, attribute, val);
                }
                if (result.size() > 0) {
                    providerArr = (Provider[]) result.toArray(new Provider[result.size()]);
                }
            }
        }
        return providerArr;
    }

    private static void filterProviders(ArrayList<Provider> providers, String service, String algorithm, String attribute, String attrValue) {
        Iterator<Provider> it = providers.iterator();
        while (it.hasNext()) {
            Provider p = it.next();
            if (!p.implementsAlg(service, algorithm, attribute, attrValue)) {
                it.remove();
            }
        }
    }

    public static String getProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        String property = secprops.getProperty(key);
        if (property != null) {
            return property.trim();
        }
        return property;
    }

    public static void setProperty(String key, String value) {
        Services.setNeedRefresh();
        secprops.put(key, value);
    }

    public static Set<String> getAlgorithms(String serviceName) {
        Set<String> result = new HashSet<>();
        if (serviceName != null) {
            Provider[] arr$ = getProviders();
            for (Provider provider : arr$) {
                for (Provider.Service service : provider.getServices()) {
                    if (service.getType().equalsIgnoreCase(serviceName)) {
                        result.add(service.getAlgorithm());
                    }
                }
            }
        }
        return result;
    }

    private static void renumProviders() {
        ArrayList<Provider> providers = Services.getProviders();
        for (int i = 0; i < providers.size(); i++) {
            providers.get(i).setProviderNumber(i + 1);
        }
    }

    private static class SecurityDoor implements SecurityAccess {
        private SecurityDoor() {
        }

        @Override
        public void renumProviders() {
            Security.renumProviders();
        }

        @Override
        public List<String> getAliases(Provider.Service s) {
            return s.getAliases();
        }

        @Override
        public Provider.Service getService(Provider p, String type) {
            return p.getService(type);
        }
    }
}
