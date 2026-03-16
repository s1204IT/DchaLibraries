package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.harmony.security.fortress.Services;

public abstract class Provider extends Properties {
    private static final long serialVersionUID = -4298000515446427739L;
    private transient LinkedHashMap<String, Service> aliasTable;
    private transient LinkedHashMap<Object, Object> changedProperties;
    private String info;
    private transient String lastAlgorithm;
    private transient String lastServiceName;
    private transient Service lastServicesByType;
    private transient Set<Service> lastServicesSet;
    private transient String lastType;
    private String name;
    private transient LinkedHashMap<String, Service> propertyAliasTable;
    private transient LinkedHashMap<String, Service> propertyServiceTable;
    private transient int providerNumber = -1;
    private transient Service returnedService;
    private transient LinkedHashMap<String, Service> serviceTable;
    private double version;
    private transient String versionString;

    protected Provider(String name, double version, String info) {
        this.name = name;
        this.version = version;
        this.info = info;
        this.versionString = String.valueOf(version);
        putProviderInfo();
    }

    public String getName() {
        return this.name;
    }

    public double getVersion() {
        return this.version;
    }

    public String getInfo() {
        return this.info;
    }

    @Override
    public String toString() {
        return this.name + " version " + this.version;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        if (this.serviceTable != null) {
            this.serviceTable.clear();
        }
        if (this.propertyServiceTable != null) {
            this.propertyServiceTable.clear();
        }
        if (this.aliasTable != null) {
            this.aliasTable.clear();
        }
        if (this.propertyAliasTable != null) {
            this.propertyAliasTable.clear();
        }
        this.changedProperties = null;
        putProviderInfo();
        if (this.providerNumber != -1) {
            Services.setNeedRefresh();
        }
        servicesChanged();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        Properties tmp = new Properties();
        tmp.load(inStream);
        myPutAll(tmp);
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        myPutAll(t);
    }

    private void myPutAll(Map<?, ?> t) {
        if (this.changedProperties == null) {
            this.changedProperties = new LinkedHashMap<>();
        }
        for (Map.Entry<?, ?> entry : t.entrySet()) {
            Object key = entry.getKey();
            if (!(key instanceof String) || !((String) key).startsWith("Provider.")) {
                Object value = entry.getValue();
                super.put(key, value);
                if (this.changedProperties.remove(key) == null) {
                    removeFromPropertyServiceTable(key);
                }
                this.changedProperties.put(key, value);
            }
        }
        if (this.providerNumber != -1) {
            Services.setNeedRefresh();
        }
    }

    @Override
    public synchronized Set<Map.Entry<Object, Object>> entrySet() {
        return Collections.unmodifiableSet(super.entrySet());
    }

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public Collection<Object> values() {
        return Collections.unmodifiableCollection(super.values());
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        Object objPut;
        if ((key instanceof String) && ((String) key).startsWith("Provider.")) {
            objPut = null;
        } else {
            if (this.providerNumber != -1) {
                Services.setNeedRefresh();
            }
            if (this.changedProperties != null && this.changedProperties.remove(key) == null) {
                removeFromPropertyServiceTable(key);
            }
            if (this.changedProperties == null) {
                this.changedProperties = new LinkedHashMap<>();
            }
            this.changedProperties.put(key, value);
            objPut = super.put(key, value);
        }
        return objPut;
    }

    @Override
    public synchronized Object remove(Object key) {
        Object objRemove;
        if ((key instanceof String) && ((String) key).startsWith("Provider.")) {
            objRemove = null;
        } else {
            if (this.providerNumber != -1) {
                Services.setNeedRefresh();
            }
            if (this.changedProperties != null && this.changedProperties.remove(key) == null) {
                removeFromPropertyServiceTable(key);
                if (this.changedProperties.size() == 0) {
                    this.changedProperties = null;
                }
            }
            objRemove = super.remove(key);
        }
        return objRemove;
    }

    boolean implementsAlg(String serv, String alg, String attribute, String val) {
        String alg2;
        String servAlg = serv + "." + alg;
        String prop = getPropertyIgnoreCase(servAlg);
        if (prop == null && (alg2 = getPropertyIgnoreCase("Alg.Alias." + servAlg)) != null) {
            servAlg = serv + "." + alg2;
            prop = getPropertyIgnoreCase(servAlg);
        }
        if (prop != null) {
            if (attribute == null) {
                return true;
            }
            return checkAttribute(servAlg, attribute, val);
        }
        return false;
    }

    private boolean checkAttribute(String servAlg, String attribute, String val) {
        String attributeValue = getPropertyIgnoreCase(servAlg + ' ' + attribute);
        if (attributeValue != null) {
            if (attribute.equalsIgnoreCase("KeySize")) {
                if (Integer.parseInt(attributeValue) >= Integer.parseInt(val)) {
                    return true;
                }
            } else if (attributeValue.equalsIgnoreCase(val)) {
                return true;
            }
        }
        return false;
    }

    void setProviderNumber(int n) {
        this.providerNumber = n;
    }

    int getProviderNumber() {
        return this.providerNumber;
    }

    synchronized Service getService(String type) {
        Service service;
        updatePropertyServiceTable();
        if (this.lastServicesByType != null && type.equals(this.lastType)) {
            service = this.lastServicesByType;
        } else {
            Iterator<Service> it = getServices().iterator();
            while (true) {
                if (it.hasNext()) {
                    service = it.next();
                    if (type.equals(service.type)) {
                        this.lastType = type;
                        this.lastServicesByType = service;
                        break;
                    }
                } else {
                    service = null;
                    break;
                }
            }
        }
        return service;
    }

    public synchronized Service getService(String type, String algorithm) {
        Object obj;
        Service service;
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        if (type.equals(this.lastServiceName) && algorithm.equalsIgnoreCase(this.lastAlgorithm)) {
            service = this.returnedService;
        } else {
            String key = key(type, algorithm);
            Object o = null;
            if (this.serviceTable != null) {
                o = this.serviceTable.get(key);
            }
            if (o != null || this.aliasTable == null) {
                obj = o;
            } else {
                Object o2 = this.aliasTable.get(key);
                obj = o2;
            }
            if (obj == null) {
                updatePropertyServiceTable();
            }
            if (obj == null && this.propertyServiceTable != null) {
                Object o3 = this.propertyServiceTable.get(key);
                obj = o3;
            }
            if (obj == null && this.propertyAliasTable != null) {
                Object o4 = this.propertyAliasTable.get(key);
                obj = o4;
            }
            if (obj != null) {
                this.lastServiceName = type;
                this.lastAlgorithm = algorithm;
                this.returnedService = (Service) obj;
                service = this.returnedService;
            } else {
                service = null;
            }
        }
        return service;
    }

    public synchronized Set<Service> getServices() {
        Set<Service> set;
        updatePropertyServiceTable();
        if (this.lastServicesSet != null) {
            set = this.lastServicesSet;
        } else {
            if (this.serviceTable != null) {
                this.lastServicesSet = new LinkedHashSet(this.serviceTable.values());
            } else {
                this.lastServicesSet = new LinkedHashSet();
            }
            if (this.propertyServiceTable != null) {
                this.lastServicesSet.addAll(this.propertyServiceTable.values());
            }
            this.lastServicesSet = Collections.unmodifiableSet(this.lastServicesSet);
            set = this.lastServicesSet;
        }
        return set;
    }

    protected synchronized void putService(Service s) {
        if (s == null) {
            throw new NullPointerException("s == null");
        }
        if (!"Provider".equals(s.getType())) {
            servicesChanged();
            if (this.serviceTable == null) {
                this.serviceTable = new LinkedHashMap<>(128);
            }
            this.serviceTable.put(key(s.type, s.algorithm), s);
            if (s.aliases != null) {
                if (this.aliasTable == null) {
                    this.aliasTable = new LinkedHashMap<>(256);
                }
                for (String alias : s.getAliases()) {
                    this.aliasTable.put(key(s.type, alias), s);
                }
            }
            serviceInfoToProperties(s);
        }
    }

    protected synchronized void removeService(Service s) {
        if (s == null) {
            throw new NullPointerException("s == null");
        }
        servicesChanged();
        if (this.serviceTable != null) {
            this.serviceTable.remove(key(s.type, s.algorithm));
        }
        if (this.aliasTable != null && s.aliases != null) {
            for (String alias : s.getAliases()) {
                this.aliasTable.remove(key(s.type, alias));
            }
        }
        serviceInfoFromProperties(s);
    }

    private void serviceInfoToProperties(Service s) {
        super.put(s.type + "." + s.algorithm, s.className);
        if (s.aliases != null) {
            Iterator<String> i = s.aliases.iterator();
            while (i.hasNext()) {
                super.put("Alg.Alias." + s.type + "." + i.next(), s.algorithm);
            }
        }
        if (s.attributes != null) {
            for (Map.Entry<String, String> entry : s.attributes.entrySet()) {
                super.put(s.type + "." + s.algorithm + " " + entry.getKey(), entry.getValue());
            }
        }
        if (this.providerNumber != -1) {
            Services.setNeedRefresh();
        }
    }

    private void serviceInfoFromProperties(Service s) {
        super.remove(s.type + "." + s.algorithm);
        if (s.aliases != null) {
            Iterator<String> i = s.aliases.iterator();
            while (i.hasNext()) {
                super.remove("Alg.Alias." + s.type + "." + i.next());
            }
        }
        if (s.attributes != null) {
            for (Map.Entry<String, String> entry : s.attributes.entrySet()) {
                super.remove(s.type + "." + s.algorithm + " " + entry.getKey());
            }
        }
        if (this.providerNumber != -1) {
            Services.setNeedRefresh();
        }
    }

    private void removeFromPropertyServiceTable(Object key) {
        Object o;
        Service ser;
        if (key != null && (key instanceof String)) {
            String k = (String) key;
            if (!k.startsWith("Provider.")) {
                if (k.startsWith("Alg.Alias.")) {
                    String service_alias = k.substring(10);
                    int i = service_alias.indexOf(46);
                    String serviceName = service_alias.substring(0, i);
                    String aliasName = service_alias.substring(i + 1);
                    if (this.propertyAliasTable != null) {
                        this.propertyAliasTable.remove(key(serviceName, aliasName));
                    }
                    if (this.propertyServiceTable != null) {
                        for (Service s : this.propertyServiceTable.values()) {
                            if (s.aliases.contains(aliasName)) {
                                s.aliases.remove(aliasName);
                                return;
                            }
                        }
                        return;
                    }
                    return;
                }
                int j = k.indexOf(46);
                if (j != -1) {
                    int i2 = k.indexOf(32);
                    if (i2 == -1) {
                        String serviceName2 = k.substring(0, j);
                        String algorithm = k.substring(j + 1);
                        if (this.propertyServiceTable == null || (ser = this.propertyServiceTable.remove(key(serviceName2, algorithm))) == null || this.propertyAliasTable == null || ser.aliases == null) {
                            return;
                        }
                        for (String alias : ser.aliases) {
                            this.propertyAliasTable.remove(key(serviceName2, alias));
                        }
                        return;
                    }
                    String attribute = k.substring(i2 + 1);
                    String serviceName3 = k.substring(0, j);
                    String algorithm2 = k.substring(j + 1, i2);
                    if (this.propertyServiceTable == null || (o = this.propertyServiceTable.get(key(serviceName3, algorithm2))) == null) {
                        return;
                    }
                    ((Service) o).attributes.remove(attribute);
                }
            }
        }
    }

    private void updatePropertyServiceTable() {
        if (this.changedProperties != null && !this.changedProperties.isEmpty()) {
            for (Map.Entry<Object, Object> entry : this.changedProperties.entrySet()) {
                Object _key = entry.getKey();
                Object _value = entry.getValue();
                if (_key != null && _value != null && (_key instanceof String) && (_value instanceof String)) {
                    String key = (String) _key;
                    String value = (String) _value;
                    if (!key.startsWith("Provider")) {
                        if (key.startsWith("Alg.Alias.")) {
                            String service_alias = key.substring(10);
                            int i = service_alias.indexOf(46);
                            String serviceName = service_alias.substring(0, i);
                            String aliasName = service_alias.substring(i + 1);
                            String propertyServiceTableKey = key(serviceName, value);
                            Object o = null;
                            if (this.propertyServiceTable == null) {
                                this.propertyServiceTable = new LinkedHashMap<>(128);
                            } else {
                                o = this.propertyServiceTable.get(propertyServiceTableKey);
                            }
                            if (o == null) {
                                String className = (String) this.changedProperties.get(serviceName + "." + value);
                                if (className != null) {
                                    List<String> l = new ArrayList<>();
                                    l.add(aliasName);
                                    Service s = new Service(this, serviceName, value, className, l, new HashMap());
                                    this.propertyServiceTable.put(propertyServiceTableKey, s);
                                    if (this.propertyAliasTable == null) {
                                        this.propertyAliasTable = new LinkedHashMap<>(256);
                                    }
                                    this.propertyAliasTable.put(key(serviceName, aliasName), s);
                                }
                            } else {
                                Service s2 = (Service) o;
                                s2.addAlias(aliasName);
                                if (this.propertyAliasTable == null) {
                                    this.propertyAliasTable = new LinkedHashMap<>(256);
                                }
                                this.propertyAliasTable.put(key(serviceName, aliasName), s2);
                            }
                        } else {
                            int j = key.indexOf(46);
                            if (j != -1) {
                                int i2 = key.indexOf(32);
                                if (i2 == -1) {
                                    String serviceName2 = key.substring(0, j);
                                    String algorithm = key.substring(j + 1);
                                    String propertyServiceTableKey2 = key(serviceName2, algorithm);
                                    Object o2 = null;
                                    if (this.propertyServiceTable != null) {
                                        o2 = this.propertyServiceTable.get(propertyServiceTableKey2);
                                    }
                                    if (o2 == null) {
                                        Service s3 = new Service(this, serviceName2, algorithm, value, Collections.emptyList(), Collections.emptyMap());
                                        if (this.propertyServiceTable == null) {
                                            this.propertyServiceTable = new LinkedHashMap<>(128);
                                        }
                                        this.propertyServiceTable.put(propertyServiceTableKey2, s3);
                                    } else {
                                        ((Service) o2).className = value;
                                    }
                                } else {
                                    String serviceName3 = key.substring(0, j);
                                    String algorithm2 = key.substring(j + 1, i2);
                                    String attribute = key.substring(i2 + 1);
                                    String propertyServiceTableKey3 = key(serviceName3, algorithm2);
                                    Object o3 = null;
                                    if (this.propertyServiceTable != null) {
                                        o3 = this.propertyServiceTable.get(propertyServiceTableKey3);
                                    }
                                    if (o3 != null) {
                                        ((Service) o3).putAttribute(attribute, value);
                                    } else {
                                        String className2 = (String) this.changedProperties.get(serviceName3 + "." + algorithm2);
                                        if (className2 != null) {
                                            Map<String, String> m = new HashMap<>();
                                            m.put(attribute, value);
                                            Service s4 = new Service(this, serviceName3, algorithm2, className2, new ArrayList(), m);
                                            if (this.propertyServiceTable == null) {
                                                this.propertyServiceTable = new LinkedHashMap<>(128);
                                            }
                                            this.propertyServiceTable.put(propertyServiceTableKey3, s4);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            servicesChanged();
            this.changedProperties = null;
        }
    }

    private void servicesChanged() {
        this.lastServicesByType = null;
        this.lastServiceName = null;
        this.lastServicesSet = null;
    }

    private void putProviderInfo() {
        super.put("Provider.id name", this.name != null ? this.name : "null");
        super.put("Provider.id version", this.versionString);
        super.put("Provider.id info", this.info != null ? this.info : "null");
        super.put("Provider.id className", getClass().getName());
    }

    private String getPropertyIgnoreCase(String key) {
        String res = getProperty(key);
        if (res == null) {
            Enumeration<?> e = propertyNames();
            while (e.hasMoreElements()) {
                String propertyName = (String) e.nextElement();
                if (key.equalsIgnoreCase(propertyName)) {
                    return getProperty(propertyName);
                }
            }
            return null;
        }
        return res;
    }

    private static String key(String type, String algorithm) {
        return type + '.' + algorithm.toUpperCase(Locale.US);
    }

    public static class Service {
        private static final String ATTR_SUPPORTED_KEY_CLASSES = "SupportedKeyClasses";
        private static final String ATTR_SUPPORTED_KEY_FORMATS = "SupportedKeyFormats";
        private static final HashMap<String, Class<?>> constructorParameterClasses;
        private static final HashMap<String, Boolean> supportsParameterTypes = new HashMap<>();
        private String algorithm;
        private List<String> aliases;
        private Map<String, String> attributes;
        private String className;
        private Class<?> implementation;
        private Class<?>[] keyClasses;
        private String[] keyFormats;
        private String lastClassName;
        private Provider provider;
        private volatile boolean supportedKeysInitialized;
        private String type;

        static {
            supportsParameterTypes.put("AlgorithmParameterGenerator", false);
            supportsParameterTypes.put("AlgorithmParameters", false);
            supportsParameterTypes.put("CertificateFactory", false);
            supportsParameterTypes.put("CertPathBuilder", false);
            supportsParameterTypes.put("CertPathValidator", false);
            supportsParameterTypes.put("CertStore", false);
            supportsParameterTypes.put("KeyFactory", false);
            supportsParameterTypes.put("KeyGenerator", false);
            supportsParameterTypes.put("KeyManagerFactory", false);
            supportsParameterTypes.put("KeyPairGenerator", false);
            supportsParameterTypes.put("KeyStore", false);
            supportsParameterTypes.put("MessageDigest", false);
            supportsParameterTypes.put("SecretKeyFactory", false);
            supportsParameterTypes.put("SecureRandom", false);
            supportsParameterTypes.put("SSLContext", false);
            supportsParameterTypes.put("TrustManagerFactory", false);
            supportsParameterTypes.put("Cipher", true);
            supportsParameterTypes.put("KeyAgreement", true);
            supportsParameterTypes.put("Mac", true);
            supportsParameterTypes.put("Signature", true);
            constructorParameterClasses = new HashMap<>();
            constructorParameterClasses.put("CertStore", loadClassOrThrow("java.security.cert.CertStoreParameters"));
            constructorParameterClasses.put("AlgorithmParameterGenerator", null);
            constructorParameterClasses.put("AlgorithmParameters", null);
            constructorParameterClasses.put("CertificateFactory", null);
            constructorParameterClasses.put("CertPathBuilder", null);
            constructorParameterClasses.put("CertPathValidator", null);
            constructorParameterClasses.put("KeyFactory", null);
            constructorParameterClasses.put("KeyGenerator", null);
            constructorParameterClasses.put("KeyManagerFactory", null);
            constructorParameterClasses.put("KeyPairGenerator", null);
            constructorParameterClasses.put("KeyStore", null);
            constructorParameterClasses.put("MessageDigest", null);
            constructorParameterClasses.put("SecretKeyFactory", null);
            constructorParameterClasses.put("SecureRandom", null);
            constructorParameterClasses.put("SSLContext", null);
            constructorParameterClasses.put("TrustManagerFactory", null);
            constructorParameterClasses.put("Cipher", null);
            constructorParameterClasses.put("KeyAgreement", null);
            constructorParameterClasses.put("Mac", null);
            constructorParameterClasses.put("Signature", null);
        }

        private static Class<?> loadClassOrThrow(String className) {
            try {
                return Provider.class.getClassLoader().loadClass(className);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        public Service(Provider provider, String type, String algorithm, String className, List<String> aliases, Map<String, String> attributes) {
            if (provider == null) {
                throw new NullPointerException("provider == null");
            }
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            if (algorithm == null) {
                throw new NullPointerException("algorithm == null");
            }
            if (className == null) {
                throw new NullPointerException("className == null");
            }
            this.provider = provider;
            this.type = type;
            this.algorithm = algorithm;
            this.className = className;
            if (aliases != null && aliases.size() == 0) {
                aliases = Collections.emptyList();
            }
            this.aliases = aliases;
            if (attributes != null && attributes.size() == 0) {
                attributes = Collections.emptyMap();
            }
            this.attributes = attributes;
        }

        void addAlias(String alias) {
            if (this.aliases == null || this.aliases.size() == 0) {
                this.aliases = new ArrayList();
            }
            this.aliases.add(alias);
        }

        void putAttribute(String name, String value) {
            if (this.attributes == null || this.attributes.size() == 0) {
                this.attributes = new HashMap();
            }
            this.attributes.put(name, value);
        }

        public final String getType() {
            return this.type;
        }

        public final String getAlgorithm() {
            return this.algorithm;
        }

        public final Provider getProvider() {
            return this.provider;
        }

        public final String getClassName() {
            return this.className;
        }

        public final String getAttribute(String name) {
            if (name == null) {
                throw new NullPointerException("name == null");
            }
            if (this.attributes == null) {
                return null;
            }
            return this.attributes.get(name);
        }

        List<String> getAliases() {
            if (this.aliases == null) {
                this.aliases = new ArrayList(0);
            }
            return this.aliases;
        }

        public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
            if (this.implementation == null || !this.className.equals(this.lastClassName)) {
                ClassLoader cl = this.provider.getClass().getClassLoader();
                if (cl == null) {
                    cl = ClassLoader.getSystemClassLoader();
                }
                try {
                    this.implementation = Class.forName(this.className, true, cl);
                    this.lastClassName = this.className;
                } catch (Exception e) {
                    throw new NoSuchAlgorithmException(this.type + " " + this.algorithm + " implementation not found: " + e);
                }
            }
            if (!constructorParameterClasses.containsKey(this.type)) {
                if (constructorParameter == null) {
                    return newInstanceNoParameter();
                }
                return newInstanceWithParameter(constructorParameter, constructorParameter.getClass());
            }
            if (constructorParameter == null) {
                return newInstanceNoParameter();
            }
            Class<?> expectedClass = constructorParameterClasses.get(this.type);
            if (expectedClass == null) {
                throw new IllegalArgumentException("Constructor parameter not supported for " + this.type);
            }
            if (!expectedClass.isAssignableFrom(constructorParameter.getClass())) {
                throw new IllegalArgumentException("Expecting constructor parameter of type " + expectedClass.getName() + " but was " + constructorParameter.getClass().getName());
            }
            return newInstanceWithParameter(constructorParameter, expectedClass);
        }

        private Object newInstanceWithParameter(Object constructorParameter, Class<?> parameterClass) throws NoSuchAlgorithmException {
            try {
                Class<?>[] parameterTypes = {parameterClass};
                Object[] initargs = {constructorParameter};
                return this.implementation.getConstructor(parameterTypes).newInstance(initargs);
            } catch (Exception e) {
                throw new NoSuchAlgorithmException(this.type + " " + this.algorithm + " implementation not found", e);
            }
        }

        private Object newInstanceNoParameter() throws NoSuchAlgorithmException {
            try {
                return this.implementation.newInstance();
            } catch (Exception e) {
                throw new NoSuchAlgorithmException(this.type + " " + this.algorithm + " implementation not found", e);
            }
        }

        public boolean supportsParameter(Object parameter) {
            Boolean supportsParameter = supportsParameterTypes.get(this.type);
            if (supportsParameter == null) {
                return true;
            }
            if (!supportsParameter.booleanValue()) {
                throw new InvalidParameterException("Cannot use a parameter with " + this.type);
            }
            if (parameter != null && !(parameter instanceof Key)) {
                throw new InvalidParameterException("Parameter should be of type Key");
            }
            ensureSupportedKeysInitialized();
            if (this.keyClasses == null && this.keyFormats == null) {
                return true;
            }
            if (parameter == null) {
                return false;
            }
            Key keyParam = (Key) parameter;
            if (this.keyClasses == null || !isInArray(this.keyClasses, keyParam.getClass())) {
                return this.keyFormats != null && isInArray(this.keyFormats, keyParam.getFormat());
            }
            return true;
        }

        private void ensureSupportedKeysInitialized() {
            if (!this.supportedKeysInitialized) {
                String supportedClassesString = getAttribute(ATTR_SUPPORTED_KEY_CLASSES);
                if (supportedClassesString != null) {
                    String[] keyClassNames = supportedClassesString.split("\\|");
                    ArrayList<Class<?>> supportedClassList = new ArrayList<>(keyClassNames.length);
                    ClassLoader classLoader = getProvider().getClass().getClassLoader();
                    for (String keyClassName : keyClassNames) {
                        try {
                            Class<?> keyClass = classLoader.loadClass(keyClassName);
                            if (Key.class.isAssignableFrom(keyClass)) {
                                supportedClassList.add(keyClass);
                            }
                        } catch (ClassNotFoundException e) {
                        }
                    }
                    this.keyClasses = (Class[]) supportedClassList.toArray(new Class[supportedClassList.size()]);
                }
                String supportedFormatString = getAttribute(ATTR_SUPPORTED_KEY_FORMATS);
                if (supportedFormatString != null) {
                    this.keyFormats = supportedFormatString.split("\\|");
                }
                this.supportedKeysInitialized = true;
            }
        }

        private static <T> boolean isInArray(T[] itemList, T target) {
            if (target == null) {
                return false;
            }
            for (T item : itemList) {
                if (target.equals(item)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isInArray(Class<?>[] itemList, Class<?> target) {
            if (target == null) {
                return false;
            }
            for (Class<?> item : itemList) {
                if (item.isAssignableFrom(target)) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            String result = "Provider " + this.provider.getName() + " Service " + this.type + "." + this.algorithm + " " + this.className;
            if (this.aliases != null) {
                result = result + "\nAliases " + this.aliases.toString();
            }
            if (this.attributes != null) {
                return result + "\nAttributes " + this.attributes.toString();
            }
            return result;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.versionString = String.valueOf(this.version);
        this.providerNumber = -1;
    }
}
