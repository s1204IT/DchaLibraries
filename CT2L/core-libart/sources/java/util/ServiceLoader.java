package java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import libcore.io.IoUtils;

public final class ServiceLoader<S> implements Iterable<S> {
    private final ClassLoader classLoader;
    private final Class<S> service;
    private final Set<URL> services;

    private ServiceLoader(Class<S> service, ClassLoader classLoader) {
        if (service == null) {
            throw new NullPointerException("service == null");
        }
        this.service = service;
        this.classLoader = classLoader;
        this.services = new HashSet();
        reload();
    }

    public void reload() {
        internalLoad();
    }

    @Override
    public Iterator<S> iterator() {
        return new ServiceIterator(this);
    }

    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return new ServiceLoader<>(service, classLoader);
    }

    private void internalLoad() {
        this.services.clear();
        try {
            String name = "META-INF/services/" + this.service.getName();
            this.services.addAll(Collections.list(this.classLoader.getResources(name)));
        } catch (IOException e) {
        }
    }

    public static <S> ServiceLoader<S> load(Class<S> service) {
        return load(service, Thread.currentThread().getContextClassLoader());
    }

    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (cl != null) {
            while (cl.getParent() != null) {
                cl = cl.getParent();
            }
        }
        return load(service, cl);
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
        return "ServiceLoader for " + this.service.getName();
    }

    private class ServiceIterator implements Iterator<S> {
        private final ClassLoader classLoader;
        private boolean isRead = false;
        private LinkedList<String> queue = new LinkedList<>();
        private final Class<S> service;
        private final Set<URL> services;

        public ServiceIterator(ServiceLoader<S> sl) {
            this.classLoader = ((ServiceLoader) sl).classLoader;
            this.service = ((ServiceLoader) sl).service;
            this.services = ((ServiceLoader) sl).services;
        }

        @Override
        public boolean hasNext() throws Throwable {
            if (!this.isRead) {
                readClass();
            }
            return (this.queue == null || this.queue.isEmpty()) ? false : true;
        }

        @Override
        public S next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String className = this.queue.remove();
            try {
                return this.service.cast(this.classLoader.loadClass(className).newInstance());
            } catch (Exception e) {
                throw new ServiceConfigurationError("Couldn't instantiate class " + className, e);
            }
        }

        private void readClass() throws Throwable {
            for (URL url : this.services) {
                BufferedReader reader = null;
                try {
                    try {
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                        while (true) {
                            try {
                                String line = reader2.readLine();
                                if (line == null) {
                                    break;
                                }
                                int commentStart = line.indexOf(35);
                                if (commentStart != -1) {
                                    line = line.substring(0, commentStart);
                                }
                                String line2 = line.trim();
                                if (!line2.isEmpty()) {
                                    checkValidJavaClassName(line2);
                                    if (!this.queue.contains(line2)) {
                                        this.queue.add(line2);
                                    }
                                }
                            } catch (Exception e) {
                                e = e;
                                reader = reader2;
                                throw new ServiceConfigurationError("Couldn't read " + url, e);
                            } catch (Throwable th) {
                                th = th;
                                reader = reader2;
                                IoUtils.closeQuietly(reader);
                                throw th;
                            }
                        }
                        this.isRead = true;
                        IoUtils.closeQuietly(reader2);
                    } catch (Exception e2) {
                        e = e2;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void checkValidJavaClassName(String className) {
            for (int i = 0; i < className.length(); i++) {
                char ch = className.charAt(i);
                if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
                    throw new ServiceConfigurationError("Bad character '" + ch + "' in class name");
                }
            }
        }
    }
}
