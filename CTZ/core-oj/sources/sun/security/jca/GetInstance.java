package sun.security.jca;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Iterator;
import java.util.List;

public class GetInstance {
    private GetInstance() {
    }

    public static final class Instance {
        public final Object impl;
        public final Provider provider;

        private Instance(Provider provider, Object obj) {
            this.provider = provider;
            this.impl = obj;
        }

        public Object[] toArray() {
            return new Object[]{this.impl, this.provider};
        }
    }

    public static Provider.Service getService(String str, String str2) throws NoSuchAlgorithmException {
        Provider.Service service = Providers.getProviderList().getService(str, str2);
        if (service == null) {
            throw new NoSuchAlgorithmException(str2 + " " + str + " not available");
        }
        return service;
    }

    public static Provider.Service getService(String str, String str2, String str3) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (str3 == null || str3.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider provider = Providers.getProviderList().getProvider(str3);
        if (provider == null) {
            throw new NoSuchProviderException("no such provider: " + str3);
        }
        Provider.Service service = provider.getService(str, str2);
        if (service == null) {
            throw new NoSuchAlgorithmException("no such algorithm: " + str2 + " for provider " + str3);
        }
        return service;
    }

    public static Provider.Service getService(String str, String str2, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider.Service service = provider.getService(str, str2);
        if (service == null) {
            throw new NoSuchAlgorithmException("no such algorithm: " + str2 + " for provider " + provider.getName());
        }
        return service;
    }

    public static List<Provider.Service> getServices(String str, String str2) {
        return Providers.getProviderList().getServices(str, str2);
    }

    @Deprecated
    public static List<Provider.Service> getServices(String str, List<String> list) {
        return Providers.getProviderList().getServices(str, list);
    }

    public static List<Provider.Service> getServices(List<ServiceId> list) {
        return Providers.getProviderList().getServices(list);
    }

    public static Instance getInstance(String str, Class<?> cls, String str2) throws NoSuchAlgorithmException {
        ProviderList providerList = Providers.getProviderList();
        Provider.Service service = providerList.getService(str, str2);
        if (service == null) {
            throw new NoSuchAlgorithmException(str2 + " " + str + " not available");
        }
        try {
            return getInstance(service, cls);
        } catch (NoSuchAlgorithmException e) {
            e = e;
            for (Provider.Service service2 : providerList.getServices(str, str2)) {
                if (service2 != service) {
                    try {
                        return getInstance(service2, cls);
                    } catch (NoSuchAlgorithmException e2) {
                        e = e2;
                    }
                }
            }
            throw e;
        }
    }

    public static Instance getInstance(String str, Class<?> cls, String str2, Object obj) throws NoSuchAlgorithmException {
        Iterator<Provider.Service> it = getServices(str, str2).iterator();
        NoSuchAlgorithmException e = null;
        while (it.hasNext()) {
            try {
                return getInstance(it.next(), cls, obj);
            } catch (NoSuchAlgorithmException e2) {
                e = e2;
            }
        }
        if (e != null) {
            throw e;
        }
        throw new NoSuchAlgorithmException(str2 + " " + str + " not available");
    }

    public static Instance getInstance(String str, Class<?> cls, String str2, String str3) throws NoSuchAlgorithmException, NoSuchProviderException {
        return getInstance(getService(str, str2, str3), cls);
    }

    public static Instance getInstance(String str, Class<?> cls, String str2, Object obj, String str3) throws NoSuchAlgorithmException, NoSuchProviderException {
        return getInstance(getService(str, str2, str3), cls, obj);
    }

    public static Instance getInstance(String str, Class<?> cls, String str2, Provider provider) throws NoSuchAlgorithmException {
        return getInstance(getService(str, str2, provider), cls);
    }

    public static Instance getInstance(String str, Class<?> cls, String str2, Object obj, Provider provider) throws NoSuchAlgorithmException {
        return getInstance(getService(str, str2, provider), cls, obj);
    }

    public static Instance getInstance(Provider.Service service, Class<?> cls) throws NoSuchAlgorithmException {
        Object objNewInstance = service.newInstance(null);
        checkSuperClass(service, objNewInstance.getClass(), cls);
        return new Instance(service.getProvider(), objNewInstance);
    }

    public static Instance getInstance(Provider.Service service, Class<?> cls, Object obj) throws NoSuchAlgorithmException {
        Object objNewInstance = service.newInstance(obj);
        checkSuperClass(service, objNewInstance.getClass(), cls);
        return new Instance(service.getProvider(), objNewInstance);
    }

    public static void checkSuperClass(Provider.Service service, Class<?> cls, Class<?> cls2) throws NoSuchAlgorithmException {
        if (cls2 != null && !cls2.isAssignableFrom(cls)) {
            throw new NoSuchAlgorithmException("class configured for " + service.getType() + ": " + service.getClassName() + " not a " + service.getType());
        }
    }
}
