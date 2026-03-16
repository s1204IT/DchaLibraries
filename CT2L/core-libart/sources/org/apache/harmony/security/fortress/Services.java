package org.apache.harmony.security.fortress;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Services {
    private static Provider.Service cachedSecureRandomService;
    private static boolean needRefresh;
    private static final HashMap<String, ArrayList<Provider.Service>> services = new HashMap<>(600);
    private static int cacheVersion = 1;
    private static final ArrayList<Provider> providers = new ArrayList<>(20);
    private static final HashMap<String, Provider> providersNames = new HashMap<>(20);

    static {
        int i = 1;
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        while (true) {
            int i2 = i + 1;
            String providerClassName = Security.getProperty("security.provider." + i);
            if (providerClassName == null) {
                Engine.door.renumProviders();
                return;
            }
            try {
                Class<?> providerClass = Class.forName(providerClassName.trim(), true, cl);
                Provider p = (Provider) providerClass.newInstance();
                providers.add(p);
                providersNames.put(p.getName(), p);
                initServiceInfo(p);
                i = i2;
            } catch (ClassNotFoundException e) {
                i = i2;
            } catch (IllegalAccessException e2) {
                i = i2;
            } catch (InstantiationException e3) {
                i = i2;
            }
        }
    }

    public static synchronized ArrayList<Provider> getProviders() {
        return providers;
    }

    public static synchronized Provider getProvider(String name) {
        return name == null ? null : providersNames.get(name);
    }

    public static synchronized int insertProviderAt(Provider provider, int position) {
        int size = providers.size();
        if (position < 1 || position > size) {
            position = size + 1;
        }
        providers.add(position - 1, provider);
        providersNames.put(provider.getName(), provider);
        setNeedRefresh();
        return position;
    }

    public static synchronized void removeProvider(int providerNumber) {
        Provider p = providers.remove(providerNumber - 1);
        providersNames.remove(p.getName());
        setNeedRefresh();
    }

    public static synchronized void initServiceInfo(Provider p) {
        for (Provider.Service service : p.getServices()) {
            String type = service.getType();
            if (cachedSecureRandomService == null && type.equals("SecureRandom")) {
                cachedSecureRandomService = service;
            }
            String key = type + "." + service.getAlgorithm().toUpperCase(Locale.US);
            appendServiceLocked(key, service);
            for (String alias : Engine.door.getAliases(service)) {
                String key2 = type + "." + alias.toUpperCase(Locale.US);
                appendServiceLocked(key2, service);
            }
        }
    }

    private static void appendServiceLocked(String key, Provider.Service service) {
        ArrayList<Provider.Service> serviceList = services.get(key);
        if (serviceList == null) {
            serviceList = new ArrayList<>(1);
            services.put(key, serviceList);
        }
        serviceList.add(service);
    }

    public static synchronized boolean isEmpty() {
        return services.isEmpty();
    }

    public static synchronized ArrayList<Provider.Service> getServices(String key) {
        return services.get(key);
    }

    public static synchronized Provider.Service getSecureRandomService() {
        getCacheVersion();
        return cachedSecureRandomService;
    }

    public static synchronized void setNeedRefresh() {
        needRefresh = true;
    }

    public static synchronized int getCacheVersion() {
        if (needRefresh) {
            cacheVersion++;
            synchronized (services) {
                services.clear();
            }
            cachedSecureRandomService = null;
            for (Provider p : providers) {
                initServiceInfo(p);
            }
            needRefresh = false;
        }
        return cacheVersion;
    }
}
