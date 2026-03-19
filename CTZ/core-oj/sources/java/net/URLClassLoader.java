package java.net;

import java.io.Closeable;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import sun.misc.Resource;
import sun.misc.URLClassPath;
import sun.net.www.ParseUtil;
import sun.net.www.protocol.file.FileURLConnection;
import sun.security.util.SecurityConstants;
import sun.util.locale.LanguageTag;

public class URLClassLoader extends SecureClassLoader implements Closeable {
    private final AccessControlContext acc;
    private WeakHashMap<Closeable, Void> closeables;
    private final URLClassPath ucp;

    public URLClassLoader(URL[] urlArr, ClassLoader classLoader) {
        super(classLoader);
        this.closeables = new WeakHashMap<>();
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkCreateClassLoader();
        }
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urlArr, this.acc);
    }

    URLClassLoader(URL[] urlArr, ClassLoader classLoader, AccessControlContext accessControlContext) {
        super(classLoader);
        this.closeables = new WeakHashMap<>();
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkCreateClassLoader();
        }
        this.acc = accessControlContext;
        this.ucp = new URLClassPath(urlArr, accessControlContext);
    }

    public URLClassLoader(URL[] urlArr) {
        this.closeables = new WeakHashMap<>();
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkCreateClassLoader();
        }
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urlArr, this.acc);
    }

    URLClassLoader(URL[] urlArr, AccessControlContext accessControlContext) {
        this.closeables = new WeakHashMap<>();
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkCreateClassLoader();
        }
        this.acc = accessControlContext;
        this.ucp = new URLClassPath(urlArr, accessControlContext);
    }

    public URLClassLoader(URL[] urlArr, ClassLoader classLoader, URLStreamHandlerFactory uRLStreamHandlerFactory) {
        super(classLoader);
        this.closeables = new WeakHashMap<>();
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkCreateClassLoader();
        }
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urlArr, uRLStreamHandlerFactory, this.acc);
    }

    @Override
    public InputStream getResourceAsStream(String str) {
        URL resource = getResource(str);
        if (resource == null) {
            return null;
        }
        try {
            URLConnection uRLConnectionOpenConnection = resource.openConnection();
            InputStream inputStream = uRLConnectionOpenConnection.getInputStream();
            if (uRLConnectionOpenConnection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) uRLConnectionOpenConnection).getJarFile();
                synchronized (this.closeables) {
                    if (!this.closeables.containsKey(jarFile)) {
                        this.closeables.put(jarFile, null);
                    }
                }
            } else if (uRLConnectionOpenConnection instanceof FileURLConnection) {
                synchronized (this.closeables) {
                    this.closeables.put(inputStream, null);
                }
            }
            return inputStream;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("closeClassLoader"));
        }
        List<IOException> listCloseLoaders = this.ucp.closeLoaders();
        synchronized (this.closeables) {
            Iterator<Closeable> it = this.closeables.keySet().iterator();
            while (it.hasNext()) {
                try {
                    it.next().close();
                } catch (IOException e) {
                    listCloseLoaders.add(e);
                }
            }
            this.closeables.clear();
        }
        if (listCloseLoaders.isEmpty()) {
            return;
        }
        IOException iOExceptionRemove = listCloseLoaders.remove(0);
        Iterator<IOException> it2 = listCloseLoaders.iterator();
        while (it2.hasNext()) {
            iOExceptionRemove.addSuppressed(it2.next());
        }
        throw iOExceptionRemove;
    }

    protected void addURL(URL url) {
        this.ucp.addURL(url);
    }

    public URL[] getURLs() {
        return this.ucp.getURLs();
    }

    @Override
    protected Class<?> findClass(final String str) throws ClassNotFoundException {
        try {
            Class<?> cls = (Class) AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                @Override
                public Class<?> run() throws ClassNotFoundException {
                    Resource resource = URLClassLoader.this.ucp.getResource(str.replace('.', '/').concat(".class"), false);
                    if (resource != null) {
                        try {
                            return URLClassLoader.this.defineClass(str, resource);
                        } catch (IOException e) {
                            throw new ClassNotFoundException(str, e);
                        }
                    }
                    return null;
                }
            }, this.acc);
            if (cls == null) {
                throw new ClassNotFoundException(str);
            }
            return cls;
        } catch (PrivilegedActionException e) {
            throw ((ClassNotFoundException) e.getException());
        }
    }

    private Package getAndVerifyPackage(String str, Manifest manifest, URL url) {
        Package r0 = getPackage(str);
        if (r0 != null) {
            if (r0.isSealed()) {
                if (!r0.isSealed(url)) {
                    throw new SecurityException("sealing violation: package " + str + " is sealed");
                }
            } else if (manifest != null && isSealed(str, manifest)) {
                throw new SecurityException("sealing violation: can't seal package " + str + ": already loaded");
            }
        }
        return r0;
    }

    private void definePackageInternal(String str, Manifest manifest, URL url) {
        if (getAndVerifyPackage(str, manifest, url) == null) {
            try {
                if (manifest != null) {
                    definePackage(str, manifest, url);
                } else {
                    definePackage(str, null, null, null, null, null, null, null);
                }
            } catch (IllegalArgumentException e) {
                if (getAndVerifyPackage(str, manifest, url) == null) {
                    throw new AssertionError((Object) ("Cannot find package " + str));
                }
            }
        }
    }

    private Class<?> defineClass(String str, Resource resource) throws Throwable {
        System.nanoTime();
        int iLastIndexOf = str.lastIndexOf(46);
        URL codeSourceURL = resource.getCodeSourceURL();
        if (iLastIndexOf != -1) {
            definePackageInternal(str.substring(0, iLastIndexOf), resource.getManifest(), codeSourceURL);
        }
        ByteBuffer byteBuffer = resource.getByteBuffer();
        if (byteBuffer != null) {
            return defineClass(str, byteBuffer, new CodeSource(codeSourceURL, resource.getCodeSigners()));
        }
        byte[] bytes = resource.getBytes();
        return defineClass(str, bytes, 0, bytes.length, new CodeSource(codeSourceURL, resource.getCodeSigners()));
    }

    protected Package definePackage(String str, Manifest manifest, URL url) throws IllegalArgumentException {
        String value;
        String value2;
        String value3;
        String value4;
        String value5;
        String value6;
        String value7;
        URL url2;
        Attributes attributes = manifest.getAttributes(str.replace('.', '/').concat("/"));
        if (attributes != null) {
            value2 = attributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
            value3 = attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
            value4 = attributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            value5 = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            value6 = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            value7 = attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
            value = attributes.getValue(Attributes.Name.SEALED);
        } else {
            value = null;
            value2 = null;
            value3 = null;
            value4 = null;
            value5 = null;
            value6 = null;
            value7 = null;
        }
        Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes != null) {
            if (value2 == null) {
                value2 = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
            }
            if (value3 == null) {
                value3 = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
            }
            if (value4 == null) {
                value4 = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            }
            if (value5 == null) {
                value5 = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            }
            if (value6 == null) {
                value6 = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
            if (value7 == null) {
                value7 = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
            }
            if (value == null) {
                value = mainAttributes.getValue(Attributes.Name.SEALED);
            }
        }
        String str2 = value6;
        String str3 = value7;
        String str4 = value4;
        String str5 = value5;
        String str6 = value2;
        String str7 = value3;
        if (!"true".equalsIgnoreCase(value)) {
            url2 = null;
        } else {
            url2 = url;
        }
        return definePackage(str, str6, str7, str4, str5, str2, str3, url2);
    }

    private boolean isSealed(String str, Manifest manifest) {
        String value;
        Attributes mainAttributes;
        Attributes attributes = manifest.getAttributes(str.replace('.', '/').concat("/"));
        if (attributes != null) {
            value = attributes.getValue(Attributes.Name.SEALED);
        } else {
            value = null;
        }
        if (value == null && (mainAttributes = manifest.getMainAttributes()) != null) {
            value = mainAttributes.getValue(Attributes.Name.SEALED);
        }
        return "true".equalsIgnoreCase(value);
    }

    @Override
    public URL findResource(final String str) {
        URL url = (URL) AccessController.doPrivileged(new PrivilegedAction<URL>() {
            @Override
            public URL run() {
                return URLClassLoader.this.ucp.findResource(str, true);
            }
        }, this.acc);
        if (url != null) {
            return this.ucp.checkURL(url);
        }
        return null;
    }

    @Override
    public Enumeration<URL> findResources(String str) throws IOException {
        final Enumeration<URL> enumerationFindResources = this.ucp.findResources(str, true);
        return new Enumeration<URL>() {
            private URL url = null;

            private boolean next() {
                if (this.url != null) {
                    return true;
                }
                do {
                    URL url = (URL) AccessController.doPrivileged(new PrivilegedAction<URL>() {
                        @Override
                        public URL run() {
                            if (!enumerationFindResources.hasMoreElements()) {
                                return null;
                            }
                            return (URL) enumerationFindResources.nextElement();
                        }
                    }, URLClassLoader.this.acc);
                    if (url == null) {
                        break;
                    }
                    this.url = URLClassLoader.this.ucp.checkURL(url);
                } while (this.url == null);
                return this.url != null;
            }

            @Override
            public URL nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                URL url = this.url;
                this.url = null;
                return url;
            }

            @Override
            public boolean hasMoreElements() {
                return next();
            }
        };
    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codeSource) throws IOException {
        URLConnection uRLConnectionOpenConnection;
        PermissionCollection permissions = super.getPermissions(codeSource);
        URL location = codeSource.getLocation();
        final Permission socketPermission = null;
        try {
            uRLConnectionOpenConnection = location.openConnection();
            socketPermission = uRLConnectionOpenConnection.getPermission();
        } catch (IOException e) {
            uRLConnectionOpenConnection = null;
        }
        if (socketPermission instanceof FilePermission) {
            String name = socketPermission.getName();
            if (name.endsWith(File.separator)) {
                socketPermission = new FilePermission(name + LanguageTag.SEP, "read");
            }
        } else if (socketPermission == null && location.getProtocol().equals("file")) {
            String strDecode = ParseUtil.decode(location.getFile().replace('/', File.separatorChar));
            if (strDecode.endsWith(File.separator)) {
                strDecode = strDecode + LanguageTag.SEP;
            }
            socketPermission = new FilePermission(strDecode, "read");
        } else {
            if (uRLConnectionOpenConnection instanceof JarURLConnection) {
                location = ((JarURLConnection) uRLConnectionOpenConnection).getJarFileURL();
            }
            String host = location.getHost();
            if (host != null && host.length() > 0) {
                socketPermission = new SocketPermission(host, SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION);
            }
        }
        if (socketPermission != null) {
            final SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() throws SecurityException {
                        securityManager.checkPermission(socketPermission);
                        return null;
                    }
                }, this.acc);
            }
            permissions.add(socketPermission);
        }
        return permissions;
    }

    public static URLClassLoader newInstance(final URL[] urlArr, final ClassLoader classLoader) {
        final AccessControlContext context = AccessController.getContext();
        return (URLClassLoader) AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                return new FactoryURLClassLoader(urlArr, classLoader, context);
            }
        });
    }

    public static URLClassLoader newInstance(final URL[] urlArr) {
        final AccessControlContext context = AccessController.getContext();
        return (URLClassLoader) AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                return new FactoryURLClassLoader(urlArr, context);
            }
        });
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}
