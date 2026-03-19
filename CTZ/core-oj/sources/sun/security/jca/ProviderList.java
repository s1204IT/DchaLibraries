package sun.security.jca;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import sun.security.util.Debug;

public final class ProviderList {
    private volatile boolean allLoaded;
    private final ProviderConfig[] configs;
    private final List<Provider> userList;
    static final Debug debug = Debug.getInstance("jca", "ProviderList");
    private static final ProviderConfig[] PC0 = new ProviderConfig[0];
    private static final Provider[] P0 = new Provider[0];
    static final ProviderList EMPTY = new ProviderList(PC0, true);
    private static final Provider EMPTY_PROVIDER = new Provider("##Empty##", 1.0d, "initialization in progress") {
        private static final long serialVersionUID = 1151354171352296389L;

        @Override
        public Provider.Service getService(String str, String str2) {
            return null;
        }
    };

    static ProviderList fromSecurityProperties() {
        return (ProviderList) AccessController.doPrivileged(new PrivilegedAction<ProviderList>() {
            @Override
            public ProviderList run() {
                return new ProviderList();
            }
        });
    }

    public static ProviderList add(ProviderList providerList, Provider provider) {
        return insertAt(providerList, provider, -1);
    }

    public static ProviderList insertAt(ProviderList providerList, Provider provider, int i) {
        if (providerList.getProvider(provider.getName()) != null) {
            return providerList;
        }
        ArrayList arrayList = new ArrayList(Arrays.asList(providerList.configs));
        int size = arrayList.size();
        if (i >= 0 && i <= size) {
            size = i;
        }
        arrayList.add(size, new ProviderConfig(provider));
        return new ProviderList((ProviderConfig[]) arrayList.toArray(PC0), true);
    }

    public static ProviderList remove(ProviderList providerList, String str) {
        if (providerList.getProvider(str) == null) {
            return providerList;
        }
        ProviderConfig[] providerConfigArr = new ProviderConfig[providerList.size() - 1];
        int i = 0;
        for (ProviderConfig providerConfig : providerList.configs) {
            if (!providerConfig.getProvider().getName().equals(str)) {
                providerConfigArr[i] = providerConfig;
                i++;
            }
        }
        return new ProviderList(providerConfigArr, true);
    }

    public static ProviderList newList(Provider... providerArr) {
        ProviderConfig[] providerConfigArr = new ProviderConfig[providerArr.length];
        for (int i = 0; i < providerArr.length; i++) {
            providerConfigArr[i] = new ProviderConfig(providerArr[i]);
        }
        return new ProviderList(providerConfigArr, true);
    }

    private ProviderList(ProviderConfig[] providerConfigArr, boolean z) {
        this.userList = new AbstractList<Provider>() {
            @Override
            public int size() {
                return ProviderList.this.configs.length;
            }

            @Override
            public Provider get(int i) {
                return ProviderList.this.getProvider(i);
            }
        };
        this.configs = providerConfigArr;
        this.allLoaded = z;
    }

    private ProviderList() {
        ProviderConfig providerConfig;
        this.userList = new AbstractList<Provider>() {
            @Override
            public int size() {
                return ProviderList.this.configs.length;
            }

            @Override
            public Provider get(int i) {
                return ProviderList.this.getProvider(i);
            }
        };
        ArrayList arrayList = new ArrayList();
        int i = 1;
        while (true) {
            String property = Security.getProperty("security.provider." + i);
            if (property == null) {
                break;
            }
            String strTrim = property.trim();
            if (strTrim.length() == 0) {
                System.err.println("invalid entry for security.provider." + i);
                break;
            }
            int iIndexOf = strTrim.indexOf(32);
            if (iIndexOf == -1) {
                providerConfig = new ProviderConfig(strTrim);
            } else {
                providerConfig = new ProviderConfig(strTrim.substring(0, iIndexOf), strTrim.substring(iIndexOf + 1).trim());
            }
            if (!arrayList.contains(providerConfig)) {
                arrayList.add(providerConfig);
            }
            i++;
        }
        this.configs = (ProviderConfig[]) arrayList.toArray(PC0);
        if (debug != null) {
            debug.println("provider configuration: " + ((Object) arrayList));
        }
    }

    ProviderList getJarList(String[] strArr) {
        ArrayList arrayList = new ArrayList();
        for (String str : strArr) {
            ProviderConfig providerConfig = new ProviderConfig(str);
            ProviderConfig[] providerConfigArr = this.configs;
            int length = providerConfigArr.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    ProviderConfig providerConfig2 = providerConfigArr[i];
                    if (!providerConfig2.equals(providerConfig)) {
                        i++;
                    } else {
                        providerConfig = providerConfig2;
                        break;
                    }
                }
            }
            arrayList.add(providerConfig);
        }
        return new ProviderList((ProviderConfig[]) arrayList.toArray(PC0), false);
    }

    public int size() {
        return this.configs.length;
    }

    Provider getProvider(int i) {
        Provider provider = this.configs[i].getProvider();
        return provider != null ? provider : EMPTY_PROVIDER;
    }

    public List<Provider> providers() {
        return this.userList;
    }

    private ProviderConfig getProviderConfig(String str) {
        int index = getIndex(str);
        if (index != -1) {
            return this.configs[index];
        }
        return null;
    }

    public Provider getProvider(String str) {
        ProviderConfig providerConfig = getProviderConfig(str);
        if (providerConfig == null) {
            return null;
        }
        return providerConfig.getProvider();
    }

    public int getIndex(String str) {
        for (int i = 0; i < this.configs.length; i++) {
            if (getProvider(i).getName().equals(str)) {
                return i;
            }
        }
        return -1;
    }

    private int loadAll() {
        if (this.allLoaded) {
            return this.configs.length;
        }
        if (debug != null) {
            debug.println("Loading all providers");
            new Exception("Call trace").printStackTrace();
        }
        int i = 0;
        for (int i2 = 0; i2 < this.configs.length; i2++) {
            if (this.configs[i2].getProvider() != null) {
                i++;
            }
        }
        if (i == this.configs.length) {
            this.allLoaded = true;
        }
        return i;
    }

    ProviderList removeInvalid() {
        int iLoadAll = loadAll();
        if (iLoadAll == this.configs.length) {
            return this;
        }
        ProviderConfig[] providerConfigArr = new ProviderConfig[iLoadAll];
        int i = 0;
        for (int i2 = 0; i2 < this.configs.length; i2++) {
            ProviderConfig providerConfig = this.configs[i2];
            if (providerConfig.isLoaded()) {
                providerConfigArr[i] = providerConfig;
                i++;
            }
        }
        return new ProviderList(providerConfigArr, true);
    }

    public Provider[] toArray() {
        return (Provider[]) providers().toArray(P0);
    }

    public String toString() {
        return Arrays.asList(this.configs).toString();
    }

    public Provider.Service getService(String str, String str2) {
        for (int i = 0; i < this.configs.length; i++) {
            Provider.Service service = getProvider(i).getService(str, str2);
            if (service != null) {
                return service;
            }
        }
        return null;
    }

    public List<Provider.Service> getServices(String str, String str2) {
        return new ServiceList(str, str2);
    }

    @Deprecated
    public List<Provider.Service> getServices(String str, List<String> list) {
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new ServiceId(str, it.next()));
        }
        return getServices(arrayList);
    }

    public List<Provider.Service> getServices(List<ServiceId> list) {
        return new ServiceList(list);
    }

    private final class ServiceList extends AbstractList<Provider.Service> {
        private final String algorithm;
        private Provider.Service firstService;
        private final List<ServiceId> ids;
        private int providerIndex;
        private List<Provider.Service> services;
        private final String type;

        ServiceList(String str, String str2) {
            this.type = str;
            this.algorithm = str2;
            this.ids = null;
        }

        ServiceList(List<ServiceId> list) {
            this.type = null;
            this.algorithm = null;
            this.ids = list;
        }

        private void addService(Provider.Service service) {
            if (this.firstService == null) {
                this.firstService = service;
                return;
            }
            if (this.services == null) {
                this.services = new ArrayList(4);
                this.services.add(this.firstService);
            }
            this.services.add(service);
        }

        private Provider.Service tryGet(int i) {
            while (true) {
                if (i == 0 && this.firstService != null) {
                    return this.firstService;
                }
                if (this.services == null || this.services.size() <= i) {
                    if (this.providerIndex >= ProviderList.this.configs.length) {
                        return null;
                    }
                    ProviderList providerList = ProviderList.this;
                    int i2 = this.providerIndex;
                    this.providerIndex = i2 + 1;
                    Provider provider = providerList.getProvider(i2);
                    if (this.type != null) {
                        Provider.Service service = provider.getService(this.type, this.algorithm);
                        if (service != null) {
                            addService(service);
                        }
                    } else {
                        for (ServiceId serviceId : this.ids) {
                            Provider.Service service2 = provider.getService(serviceId.type, serviceId.algorithm);
                            if (service2 != null) {
                                addService(service2);
                            }
                        }
                    }
                } else {
                    return this.services.get(i);
                }
            }
        }

        @Override
        public Provider.Service get(int i) {
            Provider.Service serviceTryGet = tryGet(i);
            if (serviceTryGet == null) {
                throw new IndexOutOfBoundsException();
            }
            return serviceTryGet;
        }

        @Override
        public int size() {
            int size;
            if (this.services != null) {
                size = this.services.size();
            } else if (this.firstService == null) {
                size = 0;
            } else {
                size = 1;
            }
            while (tryGet(size) != null) {
                size++;
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            return tryGet(0) == null;
        }

        @Override
        public Iterator<Provider.Service> iterator() {
            return new Iterator<Provider.Service>() {
                int index;

                @Override
                public boolean hasNext() {
                    return ServiceList.this.tryGet(this.index) != null;
                }

                @Override
                public Provider.Service next() {
                    Provider.Service serviceTryGet = ServiceList.this.tryGet(this.index);
                    if (serviceTryGet == null) {
                        throw new NoSuchElementException();
                    }
                    this.index++;
                    return serviceTryGet;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
