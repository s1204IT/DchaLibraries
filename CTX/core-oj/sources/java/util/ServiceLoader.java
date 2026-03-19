package java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

public final class ServiceLoader<S> implements Iterable<S> {
    private static final String PREFIX = "META-INF/services/";
    private final ClassLoader loader;
    private ServiceLoader<S>.LazyIterator lookupIterator;
    private LinkedHashMap<String, S> providers = new LinkedHashMap<>();
    private final Class<S> service;

    public void reload() {
        this.providers.clear();
        this.lookupIterator = new LazyIterator(this.service, this.loader);
    }

    private ServiceLoader(Class<S> cls, ClassLoader classLoader) {
        this.service = (Class) Objects.requireNonNull(cls, "Service interface cannot be null");
        this.loader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
        reload();
    }

    private static void fail(Class<?> cls, String str, Throwable th) throws ServiceConfigurationError {
        throw new ServiceConfigurationError(cls.getName() + ": " + str, th);
    }

    private static void fail(Class<?> cls, String str) throws ServiceConfigurationError {
        throw new ServiceConfigurationError(cls.getName() + ": " + str);
    }

    private static void fail(Class<?> cls, URL url, int i, String str) throws ServiceConfigurationError {
        fail(cls, ((Object) url) + ":" + i + ": " + str);
    }

    private int parseLine(Class<?> cls, URL url, BufferedReader bufferedReader, int i, List<String> list) throws ServiceConfigurationError, IOException {
        String line = bufferedReader.readLine();
        if (line == null) {
            return -1;
        }
        int iIndexOf = line.indexOf(35);
        if (iIndexOf >= 0) {
            line = line.substring(0, iIndexOf);
        }
        String strTrim = line.trim();
        int length = strTrim.length();
        if (length != 0) {
            if (strTrim.indexOf(32) >= 0 || strTrim.indexOf(9) >= 0) {
                fail(cls, url, i, "Illegal configuration-file syntax");
            }
            int iCodePointAt = strTrim.codePointAt(0);
            if (!Character.isJavaIdentifierStart(iCodePointAt)) {
                fail(cls, url, i, "Illegal provider-class name: " + strTrim);
            }
            int iCharCount = Character.charCount(iCodePointAt);
            while (iCharCount < length) {
                int iCodePointAt2 = strTrim.codePointAt(iCharCount);
                if (!Character.isJavaIdentifierPart(iCodePointAt2) && iCodePointAt2 != 46) {
                    fail(cls, url, i, "Illegal provider-class name: " + strTrim);
                }
                iCharCount += Character.charCount(iCodePointAt2);
            }
            if (!this.providers.containsKey(strTrim) && !list.contains(strTrim)) {
                list.add(strTrim);
            }
        }
        return i + 1;
    }

    private Iterator<String> parse(Class<?> cls, URL url) throws Throwable {
        InputStream inputStreamOpenStream;
        ArrayList arrayList = new ArrayList();
        BufferedReader bufferedReader = null;
        try {
            try {
                inputStreamOpenStream = url.openStream();
                try {
                    try {
                        BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(inputStreamOpenStream, "utf-8"));
                        int line = 1;
                        do {
                            try {
                                line = parseLine(cls, url, bufferedReader2, line, arrayList);
                            } catch (IOException e) {
                                e = e;
                                bufferedReader = bufferedReader2;
                                fail(cls, "Error reading configuration file", e);
                                if (bufferedReader != null) {
                                    bufferedReader.close();
                                }
                                if (inputStreamOpenStream != null) {
                                    inputStreamOpenStream.close();
                                }
                            } catch (Throwable th) {
                                th = th;
                                bufferedReader = bufferedReader2;
                                if (bufferedReader != null) {
                                    try {
                                        bufferedReader.close();
                                    } catch (IOException e2) {
                                        fail(cls, "Error closing configuration file", e2);
                                        throw th;
                                    }
                                }
                                if (inputStreamOpenStream != null) {
                                    inputStreamOpenStream.close();
                                }
                                throw th;
                            }
                        } while (line >= 0);
                        bufferedReader2.close();
                        if (inputStreamOpenStream != null) {
                            inputStreamOpenStream.close();
                        }
                    } catch (IOException e3) {
                        e = e3;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e4) {
                fail(cls, "Error closing configuration file", e4);
            }
        } catch (IOException e5) {
            e = e5;
            inputStreamOpenStream = null;
        } catch (Throwable th3) {
            th = th3;
            inputStreamOpenStream = null;
        }
        return arrayList.iterator();
    }

    private class LazyIterator implements Iterator<S> {
        Enumeration<URL> configs;
        ClassLoader loader;
        String nextName;
        Iterator<String> pending;
        Class<S> service;

        private LazyIterator(Class<S> cls, ClassLoader classLoader) {
            this.configs = null;
            this.pending = null;
            this.nextName = null;
            this.service = cls;
            this.loader = classLoader;
        }

        private boolean hasNextService() {
            if (this.nextName != null) {
                return true;
            }
            if (this.configs == null) {
                try {
                    String str = ServiceLoader.PREFIX + this.service.getName();
                    if (this.loader == null) {
                        this.configs = ClassLoader.getSystemResources(str);
                    } else {
                        this.configs = this.loader.getResources(str);
                    }
                } catch (IOException e) {
                    ServiceLoader.fail(this.service, "Error locating configuration files", e);
                    if (this.pending != null) {
                    }
                    if (this.configs.hasMoreElements()) {
                    }
                }
            } else if (this.pending != null || !this.pending.hasNext()) {
                if (this.configs.hasMoreElements()) {
                    this.pending = ServiceLoader.this.parse(this.service, this.configs.nextElement());
                } else {
                    return false;
                }
            } else {
                this.nextName = this.pending.next();
                return true;
            }
            if (this.pending != null) {
            }
            if (this.configs.hasMoreElements()) {
            }
        }

        private S nextService() throws ClassNotFoundException {
            if (!hasNextService()) {
                throw new NoSuchElementException();
            }
            String str = this.nextName;
            Class<?> cls = null;
            this.nextName = null;
            try {
                cls = Class.forName(str, false, this.loader);
            } catch (ClassNotFoundException e) {
                ServiceLoader.fail(this.service, "Provider " + str + " not found", e);
            }
            if (!this.service.isAssignableFrom(cls)) {
                ClassCastException classCastException = new ClassCastException(this.service.getCanonicalName() + " is not assignable from " + cls.getCanonicalName());
                ServiceLoader.fail(this.service, "Provider " + str + " not a subtype", classCastException);
            }
            try {
                S sCast = this.service.cast(cls.newInstance());
                ServiceLoader.this.providers.put(str, sCast);
                return sCast;
            } catch (Throwable th) {
                ServiceLoader.fail(this.service, "Provider " + str + " could not be instantiated", th);
                throw new Error();
            }
        }

        @Override
        public boolean hasNext() {
            return hasNextService();
        }

        @Override
        public S next() {
            return (S) nextService();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<S> iterator() {
        return new Iterator<S>() {
            Iterator<Map.Entry<String, S>> knownProviders;

            {
                this.knownProviders = ServiceLoader.this.providers.entrySet().iterator();
            }

            @Override
            public boolean hasNext() {
                if (!this.knownProviders.hasNext()) {
                    return ServiceLoader.this.lookupIterator.hasNext();
                }
                return true;
            }

            @Override
            public S next() {
                if (!this.knownProviders.hasNext()) {
                    return (S) ServiceLoader.this.lookupIterator.next();
                }
                return this.knownProviders.next().getValue();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <S> ServiceLoader<S> load(Class<S> cls, ClassLoader classLoader) {
        return new ServiceLoader<>(cls, classLoader);
    }

    public static <S> ServiceLoader<S> load(Class<S> cls) {
        return load(cls, Thread.currentThread().getContextClassLoader());
    }

    public static <S> ServiceLoader<S> loadInstalled(Class<S> cls) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        ClassLoader classLoader = null;
        while (true) {
            ClassLoader classLoader2 = classLoader;
            classLoader = systemClassLoader;
            if (classLoader != null) {
                systemClassLoader = classLoader.getParent();
            } else {
                return load(cls, classLoader2);
            }
        }
    }

    public static <S> S loadFromSystemProperty(Class<S> cls) {
        try {
            String property = System.getProperty(cls.getName());
            if (property != null) {
                return (S) ClassLoader.getSystemClassLoader().loadClass(property).newInstance();
            }
            return null;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public String toString() {
        return "java.util.ServiceLoader[" + this.service.getName() + "]";
    }
}
