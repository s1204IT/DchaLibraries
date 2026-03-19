package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.util.Debug;

public abstract class Provider extends Properties {
    private static final String ALIAS_PREFIX_LOWER = "alg.alias.";
    static final long serialVersionUID = -4298000515446427739L;
    private String info;
    private transient boolean initialized;
    private transient boolean legacyChanged;
    private transient Map<ServiceKey, Service> legacyMap;
    private transient Map<String, String> legacyStrings;
    private String name;
    private transient Map<ServiceKey, Service> serviceMap;
    private transient Set<Service> serviceSet;
    private transient boolean servicesChanged;
    private double version;
    private static final Debug debug = Debug.getInstance("provider", "Provider");
    private static final String ALIAS_PREFIX = "Alg.Alias.";
    private static final int ALIAS_LENGTH = ALIAS_PREFIX.length();
    private static volatile ServiceKey previousKey = new ServiceKey("", "", false);
    private static final Map<String, EngineDescription> knownEngines = new HashMap();
    private volatile boolean registered = false;
    private transient Set<Map.Entry<Object, Object>> entrySet = null;
    private transient int entrySetCallCount = 0;

    static {
        addEngine("AlgorithmParameterGenerator", false, null);
        addEngine("AlgorithmParameters", false, null);
        addEngine("KeyFactory", false, null);
        addEngine("KeyPairGenerator", false, null);
        addEngine("KeyStore", false, null);
        addEngine(PKCS9Attribute.MESSAGE_DIGEST_STR, false, null);
        addEngine("SecureRandom", false, null);
        addEngine("Signature", true, null);
        addEngine("CertificateFactory", false, null);
        addEngine("CertPathBuilder", false, null);
        addEngine("CertPathValidator", false, null);
        addEngine("CertStore", false, "java.security.cert.CertStoreParameters");
        addEngine("Cipher", true, null);
        addEngine("ExemptionMechanism", false, null);
        addEngine("Mac", true, null);
        addEngine("KeyAgreement", true, null);
        addEngine("KeyGenerator", false, null);
        addEngine("SecretKeyFactory", false, null);
        addEngine("KeyManagerFactory", false, null);
        addEngine("SSLContext", false, null);
        addEngine("TrustManagerFactory", false, null);
        addEngine("GssApiMechanism", false, null);
        addEngine("SaslClientFactory", false, null);
        addEngine("SaslServerFactory", false, null);
        addEngine("Policy", false, "java.security.Policy$Parameters");
        addEngine("Configuration", false, "javax.security.auth.login.Configuration$Parameters");
        addEngine("XMLSignatureFactory", false, null);
        addEngine("KeyInfoFactory", false, null);
        addEngine("TransformService", false, null);
        addEngine("TerminalFactory", false, "java.lang.Object");
    }

    protected Provider(String str, double d, String str2) {
        this.name = str;
        this.version = d;
        this.info = str2;
        putId();
        this.initialized = true;
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
        check("clearProviderProperties." + this.name);
        if (debug != null) {
            debug.println("Remove " + this.name + " provider properties");
        }
        implClear();
    }

    @Override
    public synchronized void load(InputStream inputStream) throws IOException {
        check("putProviderProperty." + this.name);
        if (debug != null) {
            debug.println("Load " + this.name + " provider properties");
        }
        Properties properties = new Properties();
        properties.load(inputStream);
        implPutAll(properties);
    }

    @Override
    public synchronized void putAll(Map<?, ?> map) {
        check("putProviderProperty." + this.name);
        if (debug != null) {
            debug.println("Put all " + this.name + " provider properties");
        }
        implPutAll(map);
    }

    @Override
    public synchronized Set<Map.Entry<Object, Object>> entrySet() {
        checkInitialized();
        if (this.entrySet == null) {
            int i = this.entrySetCallCount;
            this.entrySetCallCount = i + 1;
            if (i == 0) {
                this.entrySet = Collections.unmodifiableMap(this).entrySet();
            } else {
                return super.entrySet();
            }
        }
        if (this.entrySetCallCount != 2) {
            throw new RuntimeException("Internal error.");
        }
        return this.entrySet;
    }

    @Override
    public Set<Object> keySet() {
        checkInitialized();
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public Collection<Object> values() {
        checkInitialized();
        return Collections.unmodifiableCollection(super.values());
    }

    @Override
    public synchronized Object put(Object obj, Object obj2) {
        check("putProviderProperty." + this.name);
        if (debug != null) {
            debug.println("Set " + this.name + " provider property [" + obj + "/" + obj2 + "]");
        }
        return implPut(obj, obj2);
    }

    @Override
    public synchronized Object putIfAbsent(Object obj, Object obj2) {
        check("putProviderProperty." + this.name);
        if (debug != null) {
            debug.println("Set " + this.name + " provider property [" + obj + "/" + obj2 + "]");
        }
        return implPutIfAbsent(obj, obj2);
    }

    @Override
    public synchronized Object remove(Object obj) {
        check("removeProviderProperty." + this.name);
        if (debug != null) {
            debug.println("Remove " + this.name + " provider property " + obj);
        }
        return implRemove(obj);
    }

    @Override
    public synchronized boolean remove(Object obj, Object obj2) {
        check("removeProviderProperty." + this.name);
        if (debug != null) {
            debug.println("Remove " + this.name + " provider property " + obj);
        }
        return implRemove(obj, obj2);
    }

    @Override
    public synchronized boolean replace(Object obj, Object obj2, Object obj3) {
        check("putProviderProperty." + this.name);
        if (debug != null) {
            debug.println("Replace " + this.name + " provider property " + obj);
        }
        return implReplace(obj, obj2, obj3);
    }

    @Override
    public synchronized Object replace(Object obj, Object obj2) {
        check("putProviderProperty." + this.name);
        if (debug != null) {
            debug.println("Replace " + this.name + " provider property " + obj);
        }
        return implReplace(obj, obj2);
    }

    @Override
    public synchronized void replaceAll(BiFunction<? super Object, ? super Object, ? extends Object> biFunction) {
        check("putProviderProperty." + this.name);
        if (debug != null) {
            debug.println("ReplaceAll " + this.name + " provider property ");
        }
        implReplaceAll(biFunction);
    }

    @Override
    public synchronized Object compute(Object obj, BiFunction<? super Object, ? super Object, ? extends Object> biFunction) {
        check("putProviderProperty." + this.name);
        check("removeProviderProperty" + this.name);
        if (debug != null) {
            debug.println("Compute " + this.name + " provider property " + obj);
        }
        return implCompute(obj, biFunction);
    }

    @Override
    public synchronized Object computeIfAbsent(Object obj, Function<? super Object, ? extends Object> function) {
        check("putProviderProperty." + this.name);
        check("removeProviderProperty" + this.name);
        if (debug != null) {
            debug.println("ComputeIfAbsent " + this.name + " provider property " + obj);
        }
        return implComputeIfAbsent(obj, function);
    }

    @Override
    public synchronized Object computeIfPresent(Object obj, BiFunction<? super Object, ? super Object, ? extends Object> biFunction) {
        check("putProviderProperty." + this.name);
        check("removeProviderProperty" + this.name);
        if (debug != null) {
            debug.println("ComputeIfPresent " + this.name + " provider property " + obj);
        }
        return implComputeIfPresent(obj, biFunction);
    }

    @Override
    public synchronized Object merge(Object obj, Object obj2, BiFunction<? super Object, ? super Object, ? extends Object> biFunction) {
        check("putProviderProperty." + this.name);
        check("removeProviderProperty" + this.name);
        if (debug != null) {
            debug.println("Merge " + this.name + " provider property " + obj);
        }
        return implMerge(obj, obj2, biFunction);
    }

    @Override
    public Object get(Object obj) {
        checkInitialized();
        return super.get(obj);
    }

    @Override
    public synchronized Object getOrDefault(Object obj, Object obj2) {
        checkInitialized();
        return super.getOrDefault(obj, obj2);
    }

    @Override
    public synchronized void forEach(BiConsumer<? super Object, ? super Object> biConsumer) {
        checkInitialized();
        super.forEach(biConsumer);
    }

    @Override
    public Enumeration<Object> keys() {
        checkInitialized();
        return super.keys();
    }

    @Override
    public Enumeration<Object> elements() {
        checkInitialized();
        return super.elements();
    }

    @Override
    public String getProperty(String str) {
        checkInitialized();
        return super.getProperty(str);
    }

    private void checkInitialized() {
        if (!this.initialized) {
            throw new IllegalStateException();
        }
    }

    private void check(String str) {
        checkInitialized();
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSecurityAccess(str);
        }
    }

    private void putId() {
        super.put("Provider.id name", String.valueOf(this.name));
        super.put("Provider.id version", String.valueOf(this.version));
        super.put("Provider.id info", String.valueOf(this.info));
        super.put("Provider.id className", getClass().getName());
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.registered = false;
        HashMap map = new HashMap();
        for (Map.Entry entry : super.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        this.defaults = null;
        objectInputStream.defaultReadObject();
        implClear();
        this.initialized = true;
        putAll(map);
    }

    private boolean checkLegacy(Object obj) {
        if (this.registered) {
            Security.increaseVersion();
        }
        if (((String) obj).startsWith("Provider.")) {
            return false;
        }
        this.legacyChanged = true;
        if (this.legacyStrings == null) {
            this.legacyStrings = new LinkedHashMap();
        }
        return true;
    }

    private void implPutAll(Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            implPut(entry.getKey(), entry.getValue());
        }
        if (this.registered) {
            Security.increaseVersion();
        }
    }

    private Object implRemove(Object obj) {
        if (obj instanceof String) {
            if (!checkLegacy(obj)) {
                return null;
            }
            this.legacyStrings.remove((String) obj);
        }
        return super.remove(obj);
    }

    private boolean implRemove(Object obj, Object obj2) {
        if ((obj instanceof String) && (obj2 instanceof String)) {
            if (!checkLegacy(obj)) {
                return false;
            }
            this.legacyStrings.remove((String) obj, obj2);
        }
        return super.remove(obj, obj2);
    }

    private boolean implReplace(Object obj, Object obj2, Object obj3) {
        if ((obj instanceof String) && (obj2 instanceof String) && (obj3 instanceof String)) {
            if (!checkLegacy(obj)) {
                return false;
            }
            this.legacyStrings.replace((String) obj, (String) obj2, (String) obj3);
        }
        return super.replace(obj, obj2, obj3);
    }

    private Object implReplace(Object obj, Object obj2) {
        if ((obj instanceof String) && (obj2 instanceof String)) {
            if (!checkLegacy(obj)) {
                return null;
            }
            this.legacyStrings.replace((String) obj, (String) obj2);
        }
        return super.replace(obj, obj2);
    }

    private void implReplaceAll(BiFunction<? super Object, ? super Object, ? extends Object> biFunction) {
        this.legacyChanged = true;
        if (this.legacyStrings == null) {
            this.legacyStrings = new LinkedHashMap();
        } else {
            this.legacyStrings.replaceAll(biFunction);
        }
        super.replaceAll(biFunction);
    }

    private Object implMerge(Object obj, Object obj2, BiFunction<? super Object, ? super Object, ? extends Object> biFunction) {
        if ((obj instanceof String) && (obj2 instanceof String)) {
            if (!checkLegacy(obj)) {
                return null;
            }
            this.legacyStrings.merge((String) obj, (String) obj2, biFunction);
        }
        return super.merge(obj, obj2, biFunction);
    }

    private Object implCompute(Object obj, BiFunction<? super Object, ? super Object, ? extends Object> biFunction) {
        if (obj instanceof String) {
            if (!checkLegacy(obj)) {
                return null;
            }
            this.legacyStrings.compute((String) obj, biFunction);
        }
        return super.compute(obj, biFunction);
    }

    private Object implComputeIfAbsent(Object obj, Function<? super Object, ? extends Object> function) {
        if (obj instanceof String) {
            if (!checkLegacy(obj)) {
                return null;
            }
            this.legacyStrings.computeIfAbsent((String) obj, function);
        }
        return super.computeIfAbsent(obj, function);
    }

    private Object implComputeIfPresent(Object obj, BiFunction<? super Object, ? super Object, ? extends Object> biFunction) {
        if (obj instanceof String) {
            if (!checkLegacy(obj)) {
                return null;
            }
            this.legacyStrings.computeIfPresent((String) obj, biFunction);
        }
        return super.computeIfPresent(obj, biFunction);
    }

    private Object implPut(Object obj, Object obj2) {
        if ((obj instanceof String) && (obj2 instanceof String)) {
            if (!checkLegacy(obj)) {
                return null;
            }
            this.legacyStrings.put((String) obj, (String) obj2);
        }
        return super.put(obj, obj2);
    }

    private Object implPutIfAbsent(Object obj, Object obj2) {
        if ((obj instanceof String) && (obj2 instanceof String)) {
            if (!checkLegacy(obj)) {
                return null;
            }
            this.legacyStrings.putIfAbsent((String) obj, (String) obj2);
        }
        return super.putIfAbsent(obj, obj2);
    }

    private void implClear() {
        if (this.legacyStrings != null) {
            this.legacyStrings.clear();
        }
        if (this.legacyMap != null) {
            this.legacyMap.clear();
        }
        if (this.serviceMap != null) {
            this.serviceMap.clear();
        }
        this.legacyChanged = false;
        this.servicesChanged = false;
        this.serviceSet = null;
        super.clear();
        putId();
        if (this.registered) {
            Security.increaseVersion();
        }
    }

    private static class ServiceKey {
        private final String algorithm;
        private final String originalAlgorithm;
        private final String type;

        private ServiceKey(String str, String str2, boolean z) {
            this.type = str;
            this.originalAlgorithm = str2;
            String upperCase = str2.toUpperCase(Locale.ENGLISH);
            this.algorithm = z ? upperCase.intern() : upperCase;
        }

        public int hashCode() {
            return this.type.hashCode() + this.algorithm.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ServiceKey)) {
                return false;
            }
            ServiceKey serviceKey = (ServiceKey) obj;
            return this.type.equals(serviceKey.type) && this.algorithm.equals(serviceKey.algorithm);
        }

        boolean matches(String str, String str2) {
            return this.type == str && this.originalAlgorithm == str2;
        }
    }

    private void ensureLegacyParsed() {
        if (!this.legacyChanged || this.legacyStrings == null) {
            return;
        }
        this.serviceSet = null;
        if (this.legacyMap == null) {
            this.legacyMap = new LinkedHashMap();
        } else {
            this.legacyMap.clear();
        }
        for (Map.Entry<String, String> entry : this.legacyStrings.entrySet()) {
            parseLegacyPut(entry.getKey(), entry.getValue());
        }
        removeInvalidServices(this.legacyMap);
        this.legacyChanged = false;
    }

    private void removeInvalidServices(Map<ServiceKey, Service> map) {
        Iterator<Map.Entry<ServiceKey, Service>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            if (!it.next().getValue().isValid()) {
                it.remove();
            }
        }
    }

    private String[] getTypeAndAlgorithm(String str) {
        int iIndexOf = str.indexOf(".");
        if (iIndexOf < 1) {
            if (debug != null) {
                debug.println("Ignoring invalid entry in provider " + this.name + ":" + str);
                return null;
            }
            return null;
        }
        return new String[]{str.substring(0, iIndexOf), str.substring(iIndexOf + 1)};
    }

    private void parseLegacyPut(String str, String str2) {
        boolean z = true;
        if (str.toLowerCase(Locale.ENGLISH).startsWith(ALIAS_PREFIX_LOWER)) {
            String[] typeAndAlgorithm = getTypeAndAlgorithm(str.substring(ALIAS_LENGTH));
            if (typeAndAlgorithm == null) {
                return;
            }
            String engineName = getEngineName(typeAndAlgorithm[0]);
            String strIntern = typeAndAlgorithm[1].intern();
            ServiceKey serviceKey = new ServiceKey(engineName, str2, z);
            Service service = this.legacyMap.get(serviceKey);
            if (service == null) {
                service = new Service();
                service.type = engineName;
                service.algorithm = str2;
                this.legacyMap.put(serviceKey, service);
            }
            this.legacyMap.put(new ServiceKey(engineName, strIntern, z), service);
            service.addAlias(strIntern);
            return;
        }
        String[] typeAndAlgorithm2 = getTypeAndAlgorithm(str);
        if (typeAndAlgorithm2 == null) {
            return;
        }
        int iIndexOf = typeAndAlgorithm2[1].indexOf(32);
        if (iIndexOf == -1) {
            String engineName2 = getEngineName(typeAndAlgorithm2[0]);
            String strIntern2 = typeAndAlgorithm2[1].intern();
            ServiceKey serviceKey2 = new ServiceKey(engineName2, strIntern2, z);
            Service service2 = this.legacyMap.get(serviceKey2);
            if (service2 == null) {
                service2 = new Service();
                service2.type = engineName2;
                service2.algorithm = strIntern2;
                this.legacyMap.put(serviceKey2, service2);
            }
            service2.className = str2;
            return;
        }
        String engineName3 = getEngineName(typeAndAlgorithm2[0]);
        String str3 = typeAndAlgorithm2[1];
        String strIntern3 = str3.substring(0, iIndexOf).intern();
        String strSubstring = str3.substring(iIndexOf + 1);
        while (strSubstring.startsWith(" ")) {
            strSubstring = strSubstring.substring(1);
        }
        String strIntern4 = strSubstring.intern();
        ServiceKey serviceKey3 = new ServiceKey(engineName3, strIntern3, z);
        Service service3 = this.legacyMap.get(serviceKey3);
        if (service3 == null) {
            service3 = new Service();
            service3.type = engineName3;
            service3.algorithm = strIntern3;
            this.legacyMap.put(serviceKey3, service3);
        }
        service3.addAttribute(strIntern4, str2);
    }

    public synchronized Service getService(String str, String str2) {
        Service service;
        checkInitialized();
        ServiceKey serviceKey = previousKey;
        Object[] objArr = 0;
        if (!serviceKey.matches(str, str2)) {
            serviceKey = new ServiceKey(str, str2, false);
            previousKey = serviceKey;
        }
        if (this.serviceMap != null && (service = this.serviceMap.get(serviceKey)) != null) {
            return service;
        }
        ensureLegacyParsed();
        return this.legacyMap != null ? this.legacyMap.get(serviceKey) : null;
    }

    public synchronized Set<Service> getServices() {
        checkInitialized();
        if (this.legacyChanged || this.servicesChanged) {
            this.serviceSet = null;
        }
        if (this.serviceSet == null) {
            ensureLegacyParsed();
            LinkedHashSet linkedHashSet = new LinkedHashSet();
            if (this.serviceMap != null) {
                linkedHashSet.addAll(this.serviceMap.values());
            }
            if (this.legacyMap != null) {
                linkedHashSet.addAll(this.legacyMap.values());
            }
            this.serviceSet = Collections.unmodifiableSet(linkedHashSet);
            this.servicesChanged = false;
        }
        return this.serviceSet;
    }

    protected synchronized void putService(Service service) {
        check("putProviderProperty." + this.name);
        if (debug != null) {
            debug.println(this.name + ".putService(): " + ((Object) service));
        }
        if (service == null) {
            throw new NullPointerException();
        }
        if (service.getProvider() != this) {
            throw new IllegalArgumentException("service.getProvider() must match this Provider object");
        }
        if (this.serviceMap == null) {
            this.serviceMap = new LinkedHashMap();
        }
        boolean z = true;
        this.servicesChanged = true;
        String type = service.getType();
        ServiceKey serviceKey = new ServiceKey(type, service.getAlgorithm(), z);
        implRemoveService(this.serviceMap.get(serviceKey));
        this.serviceMap.put(serviceKey, service);
        Iterator it = service.getAliases().iterator();
        while (it.hasNext()) {
            this.serviceMap.put(new ServiceKey(type, (String) it.next(), z), service);
        }
        putPropertyStrings(service);
    }

    private void putPropertyStrings(Service service) {
        String type = service.getType();
        String algorithm = service.getAlgorithm();
        super.put(type + "." + algorithm, service.getClassName());
        Iterator it = service.getAliases().iterator();
        while (it.hasNext()) {
            super.put(ALIAS_PREFIX + type + "." + ((String) it.next()), algorithm);
        }
        for (Map.Entry entry : service.attributes.entrySet()) {
            super.put(type + "." + algorithm + " " + entry.getKey(), entry.getValue());
        }
        if (this.registered) {
            Security.increaseVersion();
        }
    }

    private void removePropertyStrings(Service service) {
        String type = service.getType();
        String algorithm = service.getAlgorithm();
        super.remove(type + "." + algorithm);
        Iterator it = service.getAliases().iterator();
        while (it.hasNext()) {
            super.remove(ALIAS_PREFIX + type + "." + ((String) it.next()));
        }
        Iterator it2 = service.attributes.entrySet().iterator();
        while (it2.hasNext()) {
            super.remove(type + "." + algorithm + " " + ((Map.Entry) it2.next()).getKey());
        }
        if (this.registered) {
            Security.increaseVersion();
        }
    }

    protected synchronized void removeService(Service service) {
        check("removeProviderProperty." + this.name);
        if (debug != null) {
            debug.println(this.name + ".removeService(): " + ((Object) service));
        }
        if (service == null) {
            throw new NullPointerException();
        }
        implRemoveService(service);
    }

    private void implRemoveService(Service service) {
        if (service == null || this.serviceMap == null) {
            return;
        }
        String type = service.getType();
        boolean z = false;
        ServiceKey serviceKey = new ServiceKey(type, service.getAlgorithm(), z);
        if (service != this.serviceMap.get(serviceKey)) {
            return;
        }
        this.servicesChanged = true;
        this.serviceMap.remove(serviceKey);
        Iterator it = service.getAliases().iterator();
        while (it.hasNext()) {
            this.serviceMap.remove(new ServiceKey(type, (String) it.next(), z));
        }
        removePropertyStrings(service);
    }

    private static class UString {
        final String lowerString;
        final String string;

        UString(String str) {
            this.string = str;
            this.lowerString = str.toLowerCase(Locale.ENGLISH);
        }

        public int hashCode() {
            return this.lowerString.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof UString)) {
                return false;
            }
            return this.lowerString.equals(((UString) obj).lowerString);
        }

        public String toString() {
            return this.string;
        }
    }

    private static class EngineDescription {
        private volatile Class<?> constructorParameterClass;
        final String constructorParameterClassName;
        final String name;
        final boolean supportsParameter;

        EngineDescription(String str, boolean z, String str2) {
            this.name = str;
            this.supportsParameter = z;
            this.constructorParameterClassName = str2;
        }

        Class<?> getConstructorParameterClass() throws ClassNotFoundException {
            Class<?> cls = this.constructorParameterClass;
            if (cls == null) {
                Class<?> cls2 = Class.forName(this.constructorParameterClassName);
                this.constructorParameterClass = cls2;
                return cls2;
            }
            return cls;
        }
    }

    private static void addEngine(String str, boolean z, String str2) {
        EngineDescription engineDescription = new EngineDescription(str, z, str2);
        knownEngines.put(str.toLowerCase(Locale.ENGLISH), engineDescription);
        knownEngines.put(str, engineDescription);
    }

    private static String getEngineName(String str) {
        EngineDescription engineDescription = knownEngines.get(str);
        if (engineDescription == null) {
            engineDescription = knownEngines.get(str.toLowerCase(Locale.ENGLISH));
        }
        return engineDescription == null ? str : engineDescription.name;
    }

    public static class Service {
        private static final Class<?>[] CLASS0 = new Class[0];
        private String algorithm;
        private List<String> aliases;
        private Map<UString, String> attributes;
        private String className;
        private volatile Reference<Class<?>> classRef;
        private volatile Boolean hasKeyAttributes;
        private final Provider provider;
        private boolean registered;
        private Class[] supportedClasses;
        private String[] supportedFormats;
        private String type;

        private Service(Provider provider) {
            this.provider = provider;
            this.aliases = Collections.emptyList();
            this.attributes = Collections.emptyMap();
        }

        private boolean isValid() {
            return (this.type == null || this.algorithm == null || this.className == null) ? false : true;
        }

        private void addAlias(String str) {
            if (this.aliases.isEmpty()) {
                this.aliases = new ArrayList(2);
            }
            this.aliases.add(str);
        }

        void addAttribute(String str, String str2) {
            if (this.attributes.isEmpty()) {
                this.attributes = new HashMap(8);
            }
            this.attributes.put(new UString(str), str2);
        }

        public Service(Provider provider, String str, String str2, String str3, List<String> list, Map<String, String> map) {
            if (provider == null || str == null || str2 == null || str3 == null) {
                throw new NullPointerException();
            }
            this.provider = provider;
            this.type = Provider.getEngineName(str);
            this.algorithm = str2;
            this.className = str3;
            if (list == null) {
                this.aliases = Collections.emptyList();
            } else {
                this.aliases = new ArrayList(list);
            }
            if (map == null) {
                this.attributes = Collections.emptyMap();
                return;
            }
            this.attributes = new HashMap();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                this.attributes.put(new UString(entry.getKey()), entry.getValue());
            }
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

        private final List<String> getAliases() {
            return this.aliases;
        }

        public final String getAttribute(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            return this.attributes.get(new UString(str));
        }

        public Object newInstance(Object obj) throws NoSuchAlgorithmException {
            if (!this.registered) {
                if (this.provider.getService(this.type, this.algorithm) != this) {
                    throw new NoSuchAlgorithmException("Service not registered with Provider " + this.provider.getName() + ": " + ((Object) this));
                }
                this.registered = true;
            }
            try {
                EngineDescription engineDescription = (EngineDescription) Provider.knownEngines.get(this.type);
                if (engineDescription == null) {
                    return newInstanceGeneric(obj);
                }
                if (engineDescription.constructorParameterClassName == null) {
                    if (obj != null) {
                        throw new InvalidParameterException("constructorParameter not used with " + this.type + " engines");
                    }
                    return getImplClass().getConstructor(new Class[0]).newInstance(new Object[0]);
                }
                Class<?> constructorParameterClass = engineDescription.getConstructorParameterClass();
                if (obj != null && !constructorParameterClass.isAssignableFrom(obj.getClass())) {
                    throw new InvalidParameterException("constructorParameter must be instanceof " + engineDescription.constructorParameterClassName.replace('$', '.') + " for engine type " + this.type);
                }
                return getImplClass().getConstructor(constructorParameterClass).newInstance(obj);
            } catch (InvocationTargetException e) {
                throw new NoSuchAlgorithmException("Error constructing implementation (algorithm: " + this.algorithm + ", provider: " + this.provider.getName() + ", class: " + this.className + ")", e.getCause());
            } catch (NoSuchAlgorithmException e2) {
                throw e2;
            } catch (Exception e3) {
                throw new NoSuchAlgorithmException("Error constructing implementation (algorithm: " + this.algorithm + ", provider: " + this.provider.getName() + ", class: " + this.className + ")", e3);
            }
        }

        private Class<?> getImplClass() throws NoSuchAlgorithmException {
            try {
                Reference<Class<?>> reference = this.classRef;
                Class<?> clsLoadClass = reference == null ? null : reference.get();
                if (clsLoadClass == null) {
                    ClassLoader classLoader = this.provider.getClass().getClassLoader();
                    if (classLoader == null) {
                        clsLoadClass = Class.forName(this.className);
                    } else {
                        clsLoadClass = classLoader.loadClass(this.className);
                    }
                    if (!Modifier.isPublic(clsLoadClass.getModifiers())) {
                        throw new NoSuchAlgorithmException("class configured for " + this.type + " (provider: " + this.provider.getName() + ") is not public.");
                    }
                    this.classRef = new WeakReference(clsLoadClass);
                }
                return clsLoadClass;
            } catch (ClassNotFoundException e) {
                throw new NoSuchAlgorithmException("class configured for " + this.type + " (provider: " + this.provider.getName() + ") cannot be found.", e);
            }
        }

        private Object newInstanceGeneric(Object obj) throws Exception {
            Class<?> implClass = getImplClass();
            if (obj == null) {
                try {
                    return implClass.getConstructor(new Class[0]).newInstance(new Object[0]);
                } catch (NoSuchMethodException e) {
                    throw new NoSuchAlgorithmException("No public no-arg constructor found in class " + this.className);
                }
            }
            Class<?> cls = obj.getClass();
            for (Constructor<?> constructor : implClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(cls)) {
                    return constructor.newInstance(obj);
                }
            }
            throw new NoSuchAlgorithmException("No public constructor matching " + cls.getName() + " found in class " + this.className);
        }

        public boolean supportsParameter(Object obj) {
            EngineDescription engineDescription = (EngineDescription) Provider.knownEngines.get(this.type);
            if (engineDescription == null) {
                return true;
            }
            if (!engineDescription.supportsParameter) {
                throw new InvalidParameterException("supportsParameter() not used with " + this.type + " engines");
            }
            if (obj != null && !(obj instanceof Key)) {
                throw new InvalidParameterException("Parameter must be instanceof Key for engine " + this.type);
            }
            if (!hasKeyAttributes()) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            Key key = (Key) obj;
            if (supportsKeyFormat(key) || supportsKeyClass(key)) {
                return true;
            }
            return false;
        }

        private boolean hasKeyAttributes() {
            Boolean boolValueOf = this.hasKeyAttributes;
            if (boolValueOf == null) {
                synchronized (this) {
                    String attribute = getAttribute("SupportedKeyFormats");
                    if (attribute != null) {
                        this.supportedFormats = attribute.split("\\|");
                    }
                    String attribute2 = getAttribute("SupportedKeyClasses");
                    boolean z = false;
                    if (attribute2 != null) {
                        String[] strArrSplit = attribute2.split("\\|");
                        ArrayList arrayList = new ArrayList(strArrSplit.length);
                        for (String str : strArrSplit) {
                            Class<?> keyClass = getKeyClass(str);
                            if (keyClass != null) {
                                arrayList.add(keyClass);
                            }
                        }
                        this.supportedClasses = (Class[]) arrayList.toArray(CLASS0);
                    }
                    if (this.supportedFormats != null || this.supportedClasses != null) {
                        z = true;
                    }
                    boolValueOf = Boolean.valueOf(z);
                    this.hasKeyAttributes = boolValueOf;
                }
            }
            return boolValueOf.booleanValue();
        }

        private Class<?> getKeyClass(String str) {
            try {
                return Class.forName(str);
            } catch (ClassNotFoundException e) {
                try {
                    ClassLoader classLoader = this.provider.getClass().getClassLoader();
                    if (classLoader != null) {
                        return classLoader.loadClass(str);
                    }
                    return null;
                } catch (ClassNotFoundException e2) {
                    return null;
                }
            }
        }

        private boolean supportsKeyFormat(Key key) {
            String format;
            if (this.supportedFormats == null || (format = key.getFormat()) == null) {
                return false;
            }
            for (String str : this.supportedFormats) {
                if (str.equals(format)) {
                    return true;
                }
            }
            return false;
        }

        private boolean supportsKeyClass(Key key) {
            if (this.supportedClasses == null) {
                return false;
            }
            Class<?> cls = key.getClass();
            for (Class cls2 : this.supportedClasses) {
                if (cls2.isAssignableFrom(cls)) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            String str;
            String str2;
            if (this.aliases.isEmpty()) {
                str = "";
            } else {
                str = "\r\n  aliases: " + this.aliases.toString();
            }
            if (this.attributes.isEmpty()) {
                str2 = "";
            } else {
                str2 = "\r\n  attributes: " + this.attributes.toString();
            }
            return this.provider.getName() + ": " + this.type + "." + this.algorithm + " -> " + this.className + str + str2 + "\r\n";
        }
    }

    public void setRegistered() {
        this.registered = true;
    }

    public void setUnregistered() {
        this.registered = false;
    }

    public boolean isRegistered() {
        return this.registered;
    }

    public synchronized void warmUpServiceProvision() {
        checkInitialized();
        ensureLegacyParsed();
        getServices();
    }
}
