package sun.misc;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import sun.net.util.URLUtil;
import sun.net.www.ParseUtil;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

public class URLClassPath {
    private static final boolean DEBUG;
    private static final boolean DEBUG_LOOKUP_CACHE;
    private static final boolean DISABLE_ACC_CHECKING;
    private static final boolean DISABLE_JAR_CHECKING;
    static final String JAVA_VERSION = (String) AccessController.doPrivileged(new GetPropertyAction("java.version"));
    static final String USER_AGENT_JAVA_VERSION = "UA-Java-Version";
    private static volatile boolean lookupCacheEnabled;
    private final AccessControlContext acc;
    private boolean closed;
    private URLStreamHandler jarHandler;
    HashMap<String, Loader> lmap;
    ArrayList<Loader> loaders;
    private ClassLoader lookupCacheLoader;
    private URL[] lookupCacheURLs;
    private ArrayList<URL> path;
    Stack<URL> urls;

    static {
        boolean z = true;
        DEBUG = AccessController.doPrivileged(new GetPropertyAction("sun.misc.URLClassPath.debug")) != null;
        DEBUG_LOOKUP_CACHE = AccessController.doPrivileged(new GetPropertyAction("sun.misc.URLClassPath.debugLookupCache")) != null;
        String str = (String) AccessController.doPrivileged(new GetPropertyAction("sun.misc.URLClassPath.disableJarChecking"));
        DISABLE_JAR_CHECKING = str != null && (str.equals("true") || str.equals(""));
        String str2 = (String) AccessController.doPrivileged(new GetPropertyAction("jdk.net.URLClassPath.disableRestrictedPermissions"));
        if (str2 == null || (!str2.equals("true") && !str2.equals(""))) {
            z = false;
        }
        DISABLE_ACC_CHECKING = z;
        lookupCacheEnabled = false;
    }

    public URLClassPath(URL[] urlArr, URLStreamHandlerFactory uRLStreamHandlerFactory, AccessControlContext accessControlContext) {
        this.path = new ArrayList<>();
        this.urls = new Stack<>();
        this.loaders = new ArrayList<>();
        this.lmap = new HashMap<>();
        this.closed = false;
        for (URL url : urlArr) {
            this.path.add(url);
        }
        push(urlArr);
        if (uRLStreamHandlerFactory != null) {
            this.jarHandler = uRLStreamHandlerFactory.createURLStreamHandler("jar");
        }
        if (DISABLE_ACC_CHECKING) {
            this.acc = null;
        } else {
            this.acc = accessControlContext;
        }
    }

    public URLClassPath(URL[] urlArr) {
        this(urlArr, null, null);
    }

    public URLClassPath(URL[] urlArr, AccessControlContext accessControlContext) {
        this(urlArr, null, accessControlContext);
    }

    public synchronized List<IOException> closeLoaders() {
        if (this.closed) {
            return Collections.emptyList();
        }
        LinkedList linkedList = new LinkedList();
        Iterator<Loader> it = this.loaders.iterator();
        while (it.hasNext()) {
            try {
                it.next().close();
            } catch (IOException e) {
                linkedList.add(e);
            }
        }
        this.closed = true;
        return linkedList;
    }

    public synchronized void addURL(URL url) {
        if (this.closed) {
            return;
        }
        synchronized (this.urls) {
            if (url != null) {
                if (!this.path.contains(url)) {
                    this.urls.add(0, url);
                    this.path.add(url);
                    if (this.lookupCacheURLs != null) {
                        disableAllLookupCaches();
                    }
                }
            }
        }
    }

    public URL[] getURLs() {
        URL[] urlArr;
        synchronized (this.urls) {
            urlArr = (URL[]) this.path.toArray(new URL[this.path.size()]);
        }
        return urlArr;
    }

    public URL findResource(String str, boolean z) {
        int[] lookupCache = getLookupCache(str);
        int i = 0;
        while (true) {
            Loader nextLoader = getNextLoader(lookupCache, i);
            if (nextLoader != null) {
                URL urlFindResource = nextLoader.findResource(str, z);
                if (urlFindResource == null) {
                    i++;
                } else {
                    return urlFindResource;
                }
            } else {
                return null;
            }
        }
    }

    public Resource getResource(String str, boolean z) {
        if (DEBUG) {
            System.err.println("URLClassPath.getResource(\"" + str + "\")");
        }
        int[] lookupCache = getLookupCache(str);
        int i = 0;
        while (true) {
            Loader nextLoader = getNextLoader(lookupCache, i);
            if (nextLoader != null) {
                Resource resource = nextLoader.getResource(str, z);
                if (resource == null) {
                    i++;
                } else {
                    return resource;
                }
            } else {
                return null;
            }
        }
    }

    public Enumeration<URL> findResources(final String str, final boolean z) {
        return new Enumeration<URL>() {
            private int[] cache;
            private int index = 0;
            private URL url = null;

            {
                this.cache = URLClassPath.this.getLookupCache(str);
            }

            private boolean next() {
                if (this.url != null) {
                    return true;
                }
                do {
                    URLClassPath uRLClassPath = URLClassPath.this;
                    int[] iArr = this.cache;
                    int i = this.index;
                    this.index = i + 1;
                    Loader nextLoader = uRLClassPath.getNextLoader(iArr, i);
                    if (nextLoader != null) {
                        this.url = nextLoader.findResource(str, z);
                    } else {
                        return false;
                    }
                } while (this.url == null);
                return true;
            }

            @Override
            public boolean hasMoreElements() {
                return next();
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
        };
    }

    public Resource getResource(String str) {
        return getResource(str, true);
    }

    public Enumeration<Resource> getResources(final String str, final boolean z) {
        return new Enumeration<Resource>() {
            private int[] cache;
            private int index = 0;
            private Resource res = null;

            {
                this.cache = URLClassPath.this.getLookupCache(str);
            }

            private boolean next() {
                if (this.res != null) {
                    return true;
                }
                do {
                    URLClassPath uRLClassPath = URLClassPath.this;
                    int[] iArr = this.cache;
                    int i = this.index;
                    this.index = i + 1;
                    Loader nextLoader = uRLClassPath.getNextLoader(iArr, i);
                    if (nextLoader != null) {
                        this.res = nextLoader.getResource(str, z);
                    } else {
                        return false;
                    }
                } while (this.res == null);
                return true;
            }

            @Override
            public boolean hasMoreElements() {
                return next();
            }

            @Override
            public Resource nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                Resource resource = this.res;
                this.res = null;
                return resource;
            }
        };
    }

    public Enumeration<Resource> getResources(String str) {
        return getResources(str, true);
    }

    synchronized void initLookupCache(ClassLoader classLoader) {
        URL[] lookupCacheURLs = getLookupCacheURLs(classLoader);
        this.lookupCacheURLs = lookupCacheURLs;
        if (lookupCacheURLs != null) {
            this.lookupCacheLoader = classLoader;
        } else {
            disableAllLookupCaches();
        }
    }

    static void disableAllLookupCaches() {
        lookupCacheEnabled = false;
    }

    private URL[] getLookupCacheURLs(ClassLoader classLoader) {
        return null;
    }

    private static int[] getLookupCacheForClassLoader(ClassLoader classLoader, String str) {
        return null;
    }

    private static boolean knownToNotExist0(ClassLoader classLoader, String str) {
        return false;
    }

    synchronized boolean knownToNotExist(String str) {
        if (this.lookupCacheURLs != null && lookupCacheEnabled) {
            return knownToNotExist0(this.lookupCacheLoader, str);
        }
        return false;
    }

    private synchronized int[] getLookupCache(String str) {
        if (this.lookupCacheURLs != null && lookupCacheEnabled) {
            int[] lookupCacheForClassLoader = getLookupCacheForClassLoader(this.lookupCacheLoader, str);
            if (lookupCacheForClassLoader != null && lookupCacheForClassLoader.length > 0) {
                int i = lookupCacheForClassLoader[lookupCacheForClassLoader.length - 1];
                if (!ensureLoaderOpened(i)) {
                    if (DEBUG_LOOKUP_CACHE) {
                        System.out.println("Expanded loaders FAILED " + this.loaders.size() + " for maxindex=" + i);
                    }
                    return null;
                }
            }
            return lookupCacheForClassLoader;
        }
        return null;
    }

    private boolean ensureLoaderOpened(int i) {
        if (this.loaders.size() <= i) {
            if (getLoader(i) == null || !lookupCacheEnabled) {
                return false;
            }
            if (DEBUG_LOOKUP_CACHE) {
                System.out.println("Expanded loaders " + this.loaders.size() + " to index=" + i);
                return true;
            }
            return true;
        }
        return true;
    }

    private synchronized void validateLookupCache(int i, String str) {
        if (this.lookupCacheURLs != null && lookupCacheEnabled) {
            if (i < this.lookupCacheURLs.length && str.equals(URLUtil.urlNoFragString(this.lookupCacheURLs[i]))) {
                return;
            }
            if (DEBUG || DEBUG_LOOKUP_CACHE) {
                System.out.println("WARNING: resource lookup cache invalidated for lookupCacheLoader at " + i);
            }
            disableAllLookupCaches();
        }
    }

    private synchronized Loader getNextLoader(int[] iArr, int i) {
        if (this.closed) {
            return null;
        }
        if (iArr != null) {
            if (i >= iArr.length) {
                return null;
            }
            Loader loader = this.loaders.get(iArr[i]);
            if (DEBUG_LOOKUP_CACHE) {
                System.out.println("HASCACHE: Loading from : " + iArr[i] + " = " + ((Object) loader.getBaseURL()));
            }
            return loader;
        }
        return getLoader(i);
    }

    private synchronized Loader getLoader(int i) {
        URL urlPop;
        if (this.closed) {
            return null;
        }
        while (this.loaders.size() < i + 1) {
            synchronized (this.urls) {
                if (this.urls.empty()) {
                    return null;
                }
                urlPop = this.urls.pop();
            }
            String strUrlNoFragString = URLUtil.urlNoFragString(urlPop);
            if (!this.lmap.containsKey(strUrlNoFragString)) {
                try {
                    Loader loader = getLoader(urlPop);
                    URL[] classPath = loader.getClassPath();
                    if (classPath != null) {
                        push(classPath);
                    }
                    validateLookupCache(this.loaders.size(), strUrlNoFragString);
                    this.loaders.add(loader);
                    this.lmap.put(strUrlNoFragString, loader);
                } catch (IOException e) {
                } catch (SecurityException e2) {
                    if (DEBUG) {
                        System.err.println("Failed to access " + ((Object) urlPop) + ", " + ((Object) e2));
                    }
                }
            }
        }
        if (DEBUG_LOOKUP_CACHE) {
            System.out.println("NOCACHE: Loading from : " + i);
        }
        return this.loaders.get(i);
    }

    private Loader getLoader(final URL url) throws IOException {
        try {
            return (Loader) AccessController.doPrivileged(new PrivilegedExceptionAction<Loader>() {
                @Override
                public Loader run() throws IOException {
                    String file = url.getFile();
                    if (file != null && file.endsWith("/")) {
                        if ("file".equals(url.getProtocol())) {
                            return new FileLoader(url);
                        }
                        return new Loader(url);
                    }
                    return new JarLoader(url, URLClassPath.this.jarHandler, URLClassPath.this.lmap, URLClassPath.this.acc);
                }
            }, this.acc);
        } catch (PrivilegedActionException e) {
            throw ((IOException) e.getException());
        }
    }

    private void push(URL[] urlArr) {
        synchronized (this.urls) {
            for (int length = urlArr.length - 1; length >= 0; length--) {
                this.urls.push(urlArr[length]);
            }
        }
    }

    public static URL[] pathToURLs(String str) {
        StringTokenizer stringTokenizer = new StringTokenizer(str, File.pathSeparator);
        URL[] urlArr = new URL[stringTokenizer.countTokens()];
        int i = 0;
        while (stringTokenizer.hasMoreTokens()) {
            File file = new File(stringTokenizer.nextToken());
            try {
                file = new File(file.getCanonicalPath());
            } catch (IOException e) {
            }
            int i2 = i + 1;
            try {
                urlArr[i] = ParseUtil.fileToEncodedURL(file);
            } catch (IOException e2) {
            }
            i = i2;
        }
        if (urlArr.length == i) {
            return urlArr;
        }
        URL[] urlArr2 = new URL[i];
        System.arraycopy(urlArr, 0, urlArr2, 0, i);
        return urlArr2;
    }

    public URL checkURL(URL url) {
        try {
            check(url);
            return url;
        } catch (Exception e) {
            return null;
        }
    }

    static void check(URL url) throws IOException {
        URLConnection uRLConnectionOpenConnection;
        Permission permission;
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null && (permission = (uRLConnectionOpenConnection = url.openConnection()).getPermission()) != null) {
            try {
                securityManager.checkPermission(permission);
            } catch (SecurityException e) {
                if ((permission instanceof FilePermission) && permission.getActions().indexOf("read") != -1) {
                    securityManager.checkRead(permission.getName());
                } else {
                    if ((permission instanceof SocketPermission) && permission.getActions().indexOf(SecurityConstants.SOCKET_CONNECT_ACTION) != -1) {
                        if (uRLConnectionOpenConnection instanceof JarURLConnection) {
                            url = ((JarURLConnection) uRLConnectionOpenConnection).getJarFileURL();
                        }
                        securityManager.checkConnect(url.getHost(), url.getPort());
                        return;
                    }
                    throw e;
                }
            }
        }
    }

    private static class Loader implements Closeable {
        private final URL base;
        private JarFile jarfile;

        Loader(URL url) {
            this.base = url;
        }

        URL getBaseURL() {
            return this.base;
        }

        URL findResource(String str, boolean z) {
            try {
                URL url = new URL(this.base, ParseUtil.encodePath(str, false));
                if (z) {
                    try {
                        URLClassPath.check(url);
                    } catch (Exception e) {
                        return null;
                    }
                }
                URLConnection uRLConnectionOpenConnection = url.openConnection();
                if (uRLConnectionOpenConnection instanceof HttpURLConnection) {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) uRLConnectionOpenConnection;
                    httpURLConnection.setRequestMethod("HEAD");
                    if (httpURLConnection.getResponseCode() >= 400) {
                        return null;
                    }
                } else {
                    uRLConnectionOpenConnection.setUseCaches(false);
                    uRLConnectionOpenConnection.getInputStream().close();
                }
                return url;
            } catch (MalformedURLException e2) {
                throw new IllegalArgumentException("name");
            }
        }

        Resource getResource(final String str, boolean z) {
            try {
                final URL url = new URL(this.base, ParseUtil.encodePath(str, false));
                if (z) {
                    try {
                        URLClassPath.check(url);
                    } catch (Exception e) {
                        return null;
                    }
                }
                final URLConnection uRLConnectionOpenConnection = url.openConnection();
                uRLConnectionOpenConnection.getInputStream();
                if (uRLConnectionOpenConnection instanceof JarURLConnection) {
                    this.jarfile = JarLoader.checkJar(((JarURLConnection) uRLConnectionOpenConnection).getJarFile());
                }
                return new Resource() {
                    @Override
                    public String getName() {
                        return str;
                    }

                    @Override
                    public URL getURL() {
                        return url;
                    }

                    @Override
                    public URL getCodeSourceURL() {
                        return Loader.this.base;
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return uRLConnectionOpenConnection.getInputStream();
                    }

                    @Override
                    public int getContentLength() throws IOException {
                        return uRLConnectionOpenConnection.getContentLength();
                    }
                };
            } catch (MalformedURLException e2) {
                throw new IllegalArgumentException("name");
            }
        }

        Resource getResource(String str) {
            return getResource(str, true);
        }

        @Override
        public void close() throws IOException {
            if (this.jarfile != null) {
                this.jarfile.close();
            }
        }

        URL[] getClassPath() throws IOException {
            return null;
        }
    }

    static class JarLoader extends Loader {
        private final AccessControlContext acc;
        private boolean closed;
        private final URL csu;
        private URLStreamHandler handler;
        private JarIndex index;
        private JarFile jar;
        private final HashMap<String, Loader> lmap;
        private MetaIndex metaIndex;

        JarLoader(URL url, URLStreamHandler uRLStreamHandler, HashMap<String, Loader> map, AccessControlContext accessControlContext) throws IOException {
            super(new URL("jar", "", -1, ((Object) url) + "!/", uRLStreamHandler));
            this.closed = false;
            this.csu = url;
            this.handler = uRLStreamHandler;
            this.lmap = map;
            this.acc = accessControlContext;
            if (!isOptimizable(url)) {
                ensureOpen();
                return;
            }
            String file = url.getFile();
            if (file != null) {
                File file2 = new File(ParseUtil.decode(file));
                this.metaIndex = MetaIndex.forJar(file2);
                if (this.metaIndex != null && !file2.exists()) {
                    this.metaIndex = null;
                }
            }
            if (this.metaIndex == null) {
                ensureOpen();
            }
        }

        @Override
        public void close() throws IOException {
            if (!this.closed) {
                this.closed = true;
                ensureOpen();
                this.jar.close();
            }
        }

        JarFile getJarFile() {
            return this.jar;
        }

        private boolean isOptimizable(URL url) {
            return "file".equals(url.getProtocol());
        }

        private void ensureOpen() throws IOException {
            if (this.jar == null) {
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws IOException {
                            if (URLClassPath.DEBUG) {
                                System.err.println("Opening " + ((Object) JarLoader.this.csu));
                                Thread.dumpStack();
                            }
                            JarLoader.this.jar = JarLoader.this.getJarFile(JarLoader.this.csu);
                            JarLoader.this.index = JarIndex.getJarIndex(JarLoader.this.jar, JarLoader.this.metaIndex);
                            if (JarLoader.this.index != null) {
                                for (String str : JarLoader.this.index.getJarFiles()) {
                                    try {
                                        String strUrlNoFragString = URLUtil.urlNoFragString(new URL(JarLoader.this.csu, str));
                                        if (!JarLoader.this.lmap.containsKey(strUrlNoFragString)) {
                                            JarLoader.this.lmap.put(strUrlNoFragString, null);
                                        }
                                    } catch (MalformedURLException e) {
                                    }
                                }
                            }
                            return null;
                        }
                    }, this.acc);
                } catch (PrivilegedActionException e) {
                    throw ((IOException) e.getException());
                }
            }
        }

        static JarFile checkJar(JarFile jarFile) throws IOException {
            if (System.getSecurityManager() != null && !URLClassPath.DISABLE_JAR_CHECKING && !jarFile.startsWithLocHeader()) {
                IOException iOException = new IOException("Invalid Jar file");
                try {
                    jarFile.close();
                    throw iOException;
                } catch (IOException e) {
                    iOException.addSuppressed(e);
                    throw iOException;
                }
            }
            return jarFile;
        }

        private JarFile getJarFile(URL url) throws IOException {
            if (isOptimizable(url)) {
                FileURLMapper fileURLMapper = new FileURLMapper(url);
                if (!fileURLMapper.exists()) {
                    throw new FileNotFoundException(fileURLMapper.getPath());
                }
                return checkJar(new JarFile(fileURLMapper.getPath()));
            }
            URLConnection uRLConnectionOpenConnection = getBaseURL().openConnection();
            uRLConnectionOpenConnection.setRequestProperty(URLClassPath.USER_AGENT_JAVA_VERSION, URLClassPath.JAVA_VERSION);
            return checkJar(((JarURLConnection) uRLConnectionOpenConnection).getJarFile());
        }

        JarIndex getIndex() {
            try {
                ensureOpen();
                return this.index;
            } catch (IOException e) {
                throw new InternalError(e);
            }
        }

        Resource checkResource(final String str, boolean z, final JarEntry jarEntry) {
            try {
                final URL url = new URL(getBaseURL(), ParseUtil.encodePath(str, false));
                if (z) {
                    URLClassPath.check(url);
                }
                return new Resource() {
                    @Override
                    public String getName() {
                        return str;
                    }

                    @Override
                    public URL getURL() {
                        return url;
                    }

                    @Override
                    public URL getCodeSourceURL() {
                        return JarLoader.this.csu;
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return JarLoader.this.jar.getInputStream(jarEntry);
                    }

                    @Override
                    public int getContentLength() {
                        return (int) jarEntry.getSize();
                    }

                    @Override
                    public Manifest getManifest() throws IOException {
                        return JarLoader.this.jar.getManifest();
                    }

                    @Override
                    public Certificate[] getCertificates() {
                        return jarEntry.getCertificates();
                    }

                    @Override
                    public CodeSigner[] getCodeSigners() {
                        return jarEntry.getCodeSigners();
                    }
                };
            } catch (MalformedURLException e) {
                return null;
            } catch (IOException e2) {
                return null;
            } catch (AccessControlException e3) {
                return null;
            }
        }

        boolean validIndex(String str) {
            int iLastIndexOf = str.lastIndexOf("/");
            if (iLastIndexOf != -1) {
                str = str.substring(0, iLastIndexOf);
            }
            Enumeration<JarEntry> enumerationEntries = this.jar.entries();
            while (enumerationEntries.hasMoreElements()) {
                String name = enumerationEntries.nextElement().getName();
                int iLastIndexOf2 = name.lastIndexOf("/");
                if (iLastIndexOf2 != -1) {
                    name = name.substring(0, iLastIndexOf2);
                }
                if (name.equals(str)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        URL findResource(String str, boolean z) {
            Resource resource = getResource(str, z);
            if (resource != null) {
                return resource.getURL();
            }
            return null;
        }

        @Override
        Resource getResource(String str, boolean z) {
            if (this.metaIndex != null && !this.metaIndex.mayContain(str)) {
                return null;
            }
            try {
                ensureOpen();
                JarEntry jarEntry = this.jar.getJarEntry(str);
                if (jarEntry != null) {
                    return checkResource(str, z, jarEntry);
                }
                if (this.index == null) {
                    return null;
                }
                return getResource(str, z, new HashSet());
            } catch (IOException e) {
                throw new InternalError(e);
            }
        }

        Resource getResource(String str, boolean z, Set<String> set) {
            JarLoader jarLoader;
            boolean z2;
            Resource resource;
            String strSubstring;
            LinkedList<String> linkedList = this.index.get(str);
            if (linkedList == null) {
                return null;
            }
            int i = 0;
            do {
                int size = linkedList.size();
                String[] strArr = (String[]) linkedList.toArray(new String[size]);
                while (i < size) {
                    int i2 = i + 1;
                    String str2 = strArr[i];
                    try {
                        final URL url = new URL(this.csu, str2);
                        String strUrlNoFragString = URLUtil.urlNoFragString(url);
                        jarLoader = (JarLoader) this.lmap.get(strUrlNoFragString);
                        if (jarLoader == null) {
                            jarLoader = (JarLoader) AccessController.doPrivileged(new PrivilegedExceptionAction<JarLoader>() {
                                @Override
                                public JarLoader run() throws IOException {
                                    return new JarLoader(url, JarLoader.this.handler, JarLoader.this.lmap, JarLoader.this.acc);
                                }
                            }, this.acc);
                            JarIndex index = jarLoader.getIndex();
                            if (index != null) {
                                int iLastIndexOf = str2.lastIndexOf("/");
                                JarIndex jarIndex = this.index;
                                if (iLastIndexOf != -1) {
                                    strSubstring = str2.substring(0, iLastIndexOf + 1);
                                } else {
                                    strSubstring = null;
                                }
                                index.merge(jarIndex, strSubstring);
                            }
                            this.lmap.put(strUrlNoFragString, jarLoader);
                        }
                        z2 = !set.add(URLUtil.urlNoFragString(url));
                        if (!z2) {
                            try {
                                jarLoader.ensureOpen();
                                JarEntry jarEntry = jarLoader.jar.getJarEntry(str);
                                if (jarEntry != null) {
                                    return jarLoader.checkResource(str, z, jarEntry);
                                }
                                if (!jarLoader.validIndex(str)) {
                                    throw new InvalidJarIndexException("Invalid index");
                                }
                            } catch (IOException e) {
                                throw new InternalError(e);
                            }
                        }
                    } catch (MalformedURLException e2) {
                    } catch (PrivilegedActionException e3) {
                    }
                    if (z2 || jarLoader == this || jarLoader.getIndex() == null || (resource = jarLoader.getResource(str, z, set)) == null) {
                        i = i2;
                    } else {
                        return resource;
                    }
                }
                linkedList = this.index.get(str);
            } while (i < linkedList.size());
            return null;
        }

        @Override
        URL[] getClassPath() throws IOException {
            Manifest manifest;
            Attributes mainAttributes;
            String value;
            if (this.index != null || this.metaIndex != null) {
                return null;
            }
            ensureOpen();
            parseExtensionsDependencies();
            if (!this.jar.hasClassPathAttribute() || (manifest = this.jar.getManifest()) == null || (mainAttributes = manifest.getMainAttributes()) == null || (value = mainAttributes.getValue(Attributes.Name.CLASS_PATH)) == null) {
                return null;
            }
            return parseClassPath(this.csu, value);
        }

        private void parseExtensionsDependencies() throws IOException {
        }

        private URL[] parseClassPath(URL url, String str) throws MalformedURLException {
            StringTokenizer stringTokenizer = new StringTokenizer(str);
            URL[] urlArr = new URL[stringTokenizer.countTokens()];
            int i = 0;
            while (stringTokenizer.hasMoreTokens()) {
                urlArr[i] = new URL(url, stringTokenizer.nextToken());
                i++;
            }
            return urlArr;
        }
    }

    private static class FileLoader extends Loader {
        private File dir;

        FileLoader(URL url) throws IOException {
            super(url);
            if (!"file".equals(url.getProtocol())) {
                throw new IllegalArgumentException("url");
            }
            this.dir = new File(ParseUtil.decode(url.getFile().replace('/', File.separatorChar))).getCanonicalFile();
        }

        @Override
        URL findResource(String str, boolean z) {
            Resource resource = getResource(str, z);
            if (resource != null) {
                return resource.getURL();
            }
            return null;
        }

        @Override
        Resource getResource(final String str, boolean z) {
            final File file;
            try {
                URL url = new URL(getBaseURL(), ".");
                final URL url2 = new URL(getBaseURL(), ParseUtil.encodePath(str, false));
                if (!url2.getFile().startsWith(url.getFile())) {
                    return null;
                }
                if (z) {
                    URLClassPath.check(url2);
                }
                if (str.indexOf("..") != -1) {
                    file = new File(this.dir, str.replace('/', File.separatorChar)).getCanonicalFile();
                    if (!file.getPath().startsWith(this.dir.getPath())) {
                        return null;
                    }
                } else {
                    file = new File(this.dir, str.replace('/', File.separatorChar));
                }
                if (!file.exists()) {
                    return null;
                }
                return new Resource() {
                    @Override
                    public String getName() {
                        return str;
                    }

                    @Override
                    public URL getURL() {
                        return url2;
                    }

                    @Override
                    public URL getCodeSourceURL() {
                        return FileLoader.this.getBaseURL();
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new FileInputStream(file);
                    }

                    @Override
                    public int getContentLength() throws IOException {
                        return (int) file.length();
                    }
                };
            } catch (Exception e) {
                return null;
            }
        }
    }
}
