package org.apache.harmony.security.fortress;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Locale;

public final class Engine {
    public static SecurityAccess door;
    private volatile ServiceCacheEntry serviceCache;
    private final String serviceName;

    private static final class ServiceCacheEntry {
        private final String algorithm;
        private final int cacheVersion;
        private final ArrayList<Provider.Service> services;

        private ServiceCacheEntry(String algorithm, int cacheVersion, ArrayList<Provider.Service> services) {
            this.algorithm = algorithm;
            this.cacheVersion = cacheVersion;
            this.services = services;
        }
    }

    public static final class SpiAndProvider {
        public final Provider provider;
        public final Object spi;

        private SpiAndProvider(Object spi, Provider provider) {
            this.spi = spi;
            this.provider = provider;
        }
    }

    public Engine(String serviceName) {
        this.serviceName = serviceName;
    }

    public SpiAndProvider getInstance(String algorithm, Object param) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("Null algorithm name");
        }
        ArrayList<Provider.Service> services = getServices(algorithm);
        if (services == null) {
            throw notFound(this.serviceName, algorithm);
        }
        return new SpiAndProvider(services.get(0).newInstance(param), services.get(0).getProvider());
    }

    public SpiAndProvider getInstance(Provider.Service service, String param) throws NoSuchAlgorithmException {
        return new SpiAndProvider(service.newInstance(param), service.getProvider());
    }

    public ArrayList<Provider.Service> getServices(String algorithm) {
        int newCacheVersion = Services.getCacheVersion();
        ServiceCacheEntry cacheEntry = this.serviceCache;
        String algoUC = algorithm.toUpperCase(Locale.US);
        if (cacheEntry != null && cacheEntry.algorithm.equalsIgnoreCase(algoUC) && newCacheVersion == cacheEntry.cacheVersion) {
            return cacheEntry.services;
        }
        String name = this.serviceName + "." + algoUC;
        ArrayList<Provider.Service> services = Services.getServices(name);
        this.serviceCache = new ServiceCacheEntry(algoUC, newCacheVersion, services);
        return services;
    }

    public Object getInstance(String algorithm, Provider provider, Object param) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("algorithm == null");
        }
        Provider.Service service = provider.getService(this.serviceName, algorithm);
        if (service == null) {
            throw notFound(this.serviceName, algorithm);
        }
        return service.newInstance(param);
    }

    private NoSuchAlgorithmException notFound(String serviceName, String algorithm) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException(serviceName + " " + algorithm + " implementation not found");
    }
}
