package java.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import libcore.io.IoUtils;
import libcore.io.Streams;

@FindBugsSuppressWarnings({"DMI_COLLECTION_OF_URLS", "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
public class URLClassLoader extends SecureClassLoader {
    private URLStreamHandlerFactory factory;
    ArrayList<URLHandler> handlerList;
    Map<URL, URLHandler> handlerMap;
    ArrayList<URL> originalUrls;
    List<URL> searchList;

    static class IndexFile {
        private HashMap<String, ArrayList<URL>> map;

        static IndexFile readIndexFile(JarFile jf, JarEntry indexEntry, URL url) throws Throwable {
            ArrayList<URL> list;
            BufferedReader in = null;
            InputStream is = null;
            try {
                String parentURLString = getParentURL(url).toExternalForm();
                String prefix = "jar:" + parentURLString + "/";
                is = jf.getInputStream(indexEntry);
                BufferedReader in2 = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                try {
                    HashMap<String, ArrayList<URL>> pre_map = new HashMap<>();
                    if (in2.readLine() == null) {
                        IoUtils.closeQuietly(in2);
                        IoUtils.closeQuietly(is);
                        return null;
                    }
                    if (in2.readLine() == null) {
                        IoUtils.closeQuietly(in2);
                        IoUtils.closeQuietly(is);
                        return null;
                    }
                    loop0: while (true) {
                        String line = in2.readLine();
                        if (line == null) {
                            break;
                        }
                        URL jar = new URL(prefix + line + "!/");
                        while (true) {
                            String line2 = in2.readLine();
                            if (line2 == null) {
                                break loop0;
                            }
                            if (line2.isEmpty()) {
                                break;
                            }
                            if (pre_map.containsKey(line2)) {
                                list = pre_map.get(line2);
                            } else {
                                list = new ArrayList<>();
                                pre_map.put(line2, list);
                            }
                            list.add(jar);
                        }
                    }
                    if (pre_map.isEmpty()) {
                        IoUtils.closeQuietly(in2);
                        IoUtils.closeQuietly(is);
                        return null;
                    }
                    IndexFile indexFile = new IndexFile(pre_map);
                    IoUtils.closeQuietly(in2);
                    IoUtils.closeQuietly(is);
                    return indexFile;
                } catch (MalformedURLException e) {
                    in = in2;
                    IoUtils.closeQuietly(in);
                    IoUtils.closeQuietly(is);
                    return null;
                } catch (IOException e2) {
                    in = in2;
                    IoUtils.closeQuietly(in);
                    IoUtils.closeQuietly(is);
                    return null;
                } catch (Throwable th) {
                    th = th;
                    in = in2;
                    IoUtils.closeQuietly(in);
                    IoUtils.closeQuietly(is);
                    throw th;
                }
            } catch (MalformedURLException e3) {
            } catch (IOException e4) {
            } catch (Throwable th2) {
                th = th2;
            }
        }

        private static URL getParentURL(URL url) throws IOException {
            URL fileURL = ((JarURLConnection) url.openConnection()).getJarFileURL();
            String file = fileURL.getFile();
            String parentFile = new File(file).getParent().replace(File.separatorChar, '/');
            if (parentFile.charAt(0) != '/') {
                parentFile = "/" + parentFile;
            }
            URL parentURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), parentFile);
            return parentURL;
        }

        public IndexFile(HashMap<String, ArrayList<URL>> map) {
            this.map = map;
        }

        ArrayList<URL> get(String name) {
            return this.map.get(name);
        }
    }

    class URLHandler {
        URL codeSourceUrl;
        URL url;

        public URLHandler(URL url) {
            this.url = url;
            this.codeSourceUrl = url;
        }

        void findResources(String name, ArrayList<URL> resources) {
            URL res = findResource(name);
            if (res != null && !resources.contains(res)) {
                resources.add(res);
            }
        }

        Class<?> findClass(String packageName, String name, String origName) {
            URL resURL = targetURL(this.url, name);
            if (resURL != null) {
                try {
                    InputStream is = resURL.openStream();
                    return createClass(is, packageName, origName);
                } catch (IOException e) {
                }
            }
            return null;
        }

        Class<?> createClass(InputStream is, String packageName, String origName) {
            if (is == null) {
                return null;
            }
            try {
                byte[] clBuf = Streams.readFully(is);
                if (packageName != null) {
                    String packageDotName = packageName.replace('/', '.');
                    Package packageObj = URLClassLoader.this.getPackage(packageDotName);
                    if (packageObj == null) {
                        URLClassLoader.this.definePackage(packageDotName, null, null, null, null, null, null, null);
                    } else if (packageObj.isSealed()) {
                        throw new SecurityException("Package is sealed");
                    }
                }
                return URLClassLoader.this.defineClass(origName, clBuf, 0, clBuf.length, new CodeSource(this.codeSourceUrl, (Certificate[]) null));
            } catch (IOException e) {
                return null;
            }
        }

        URL findResource(String name) {
            URL resURL = targetURL(this.url, name);
            if (resURL != null) {
                try {
                    URLConnection uc = resURL.openConnection();
                    uc.getInputStream().close();
                    if (resURL.getProtocol().equals("http")) {
                        int code = ((HttpURLConnection) uc).getResponseCode();
                        if (code >= 200 && code < 300) {
                            return resURL;
                        }
                    } else {
                        return resURL;
                    }
                } catch (IOException e) {
                    return null;
                } catch (SecurityException e2) {
                    return null;
                }
            }
            return null;
        }

        URL targetURL(URL base, String name) {
            try {
                StringBuilder fileBuilder = new StringBuilder();
                fileBuilder.append(base.getFile());
                URI.PATH_ENCODER.appendEncoded(fileBuilder, name);
                String file = fileBuilder.toString();
                return new URL(base.getProtocol(), base.getHost(), base.getPort(), file, null);
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    class URLJarHandler extends URLHandler {
        final IndexFile index;
        final JarFile jf;
        final String prefixName;
        final Map<URL, URLHandler> subHandlers;

        public URLJarHandler(URL url, URL jarURL, JarFile jf, String prefixName) {
            super(url);
            this.subHandlers = new HashMap();
            this.jf = jf;
            this.prefixName = prefixName;
            this.codeSourceUrl = jarURL;
            JarEntry je = jf.getJarEntry("META-INF/INDEX.LIST");
            this.index = je == null ? null : IndexFile.readIndexFile(jf, je, url);
        }

        public URLJarHandler(URL url, URL jarURL, JarFile jf, String prefixName, IndexFile index) {
            super(url);
            this.subHandlers = new HashMap();
            this.jf = jf;
            this.prefixName = prefixName;
            this.index = index;
            this.codeSourceUrl = jarURL;
        }

        IndexFile getIndex() {
            return this.index;
        }

        @Override
        void findResources(String name, ArrayList<URL> resources) {
            URL res = findResourceInOwn(name);
            if (res != null && !resources.contains(res)) {
                resources.add(res);
            }
            if (this.index != null) {
                int pos = name.lastIndexOf("/");
                String indexedName = pos > 0 ? name.substring(0, pos) : name;
                ArrayList<URL> urls = this.index.get(indexedName);
                if (urls != null) {
                    urls.remove(this.url);
                    for (URL url : urls) {
                        URLHandler h = getSubHandler(url);
                        if (h != null) {
                            h.findResources(name, resources);
                        }
                    }
                }
            }
        }

        @Override
        Class<?> findClass(String packageName, String name, String origName) {
            ArrayList<URL> urls;
            Class<?> res;
            String entryName = this.prefixName + name;
            JarEntry entry = this.jf.getJarEntry(entryName);
            if (entry != null) {
                try {
                    Manifest manifest = this.jf.getManifest();
                    return createClass(entry, manifest, packageName, origName);
                } catch (IOException e) {
                }
            }
            if (this.index != null) {
                if (packageName == null) {
                    urls = this.index.get(name);
                } else {
                    urls = this.index.get(packageName);
                }
                if (urls != null) {
                    urls.remove(this.url);
                    for (URL url : urls) {
                        URLHandler h = getSubHandler(url);
                        if (h != null && (res = h.findClass(packageName, name, origName)) != null) {
                            return res;
                        }
                    }
                }
            }
            return null;
        }

        private Class<?> createClass(JarEntry entry, Manifest manifest, String packageName, String origName) {
            try {
                InputStream is = this.jf.getInputStream(entry);
                byte[] clBuf = Streams.readFully(is);
                if (packageName != null) {
                    String packageDotName = packageName.replace('/', '.');
                    Package packageObj = URLClassLoader.this.getPackage(packageDotName);
                    if (packageObj == null) {
                        if (manifest == null) {
                            URLClassLoader.this.definePackage(packageDotName, null, null, null, null, null, null, null);
                        } else {
                            URLClassLoader.this.definePackage(packageDotName, manifest, this.codeSourceUrl);
                        }
                    } else {
                        boolean exception = packageObj.isSealed();
                        if (manifest != null && URLClassLoader.this.isSealed(manifest, packageName + "/")) {
                            exception = !packageObj.isSealed(this.codeSourceUrl);
                        }
                        if (exception) {
                            throw new SecurityException(String.format("Package %s is sealed", packageName));
                        }
                    }
                }
                CodeSource codeS = new CodeSource(this.codeSourceUrl, entry.getCertificates());
                return URLClassLoader.this.defineClass(origName, clBuf, 0, clBuf.length, codeS);
            } catch (IOException e) {
                return null;
            }
        }

        URL findResourceInOwn(String name) {
            String entryName = this.prefixName + name;
            if (this.jf.getEntry(entryName) != null) {
                return targetURL(this.url, name);
            }
            return null;
        }

        @Override
        URL findResource(String name) {
            URL res;
            URL res2 = findResourceInOwn(name);
            if (res2 != null) {
                return res2;
            }
            if (this.index != null) {
                int pos = name.lastIndexOf("/");
                String indexedName = pos > 0 ? name.substring(0, pos) : name;
                ArrayList<URL> urls = this.index.get(indexedName);
                if (urls != null) {
                    urls.remove(this.url);
                    for (URL url : urls) {
                        URLHandler h = getSubHandler(url);
                        if (h != null && (res = h.findResource(name)) != null) {
                            return res;
                        }
                    }
                }
            }
            return null;
        }

        private synchronized URLHandler getSubHandler(URL url) {
            URLHandler sub;
            URLHandler sub2;
            URLHandler sub3 = this.subHandlers.get(url);
            if (sub3 != null) {
                sub2 = sub3;
            } else {
                String protocol = url.getProtocol();
                if (protocol.equals("jar")) {
                    sub = URLClassLoader.this.createURLJarHandler(url);
                } else {
                    sub = protocol.equals("file") ? createURLSubJarHandler(url) : URLClassLoader.this.createURLHandler(url);
                }
                if (sub != null) {
                    this.subHandlers.put(url, sub);
                }
                sub2 = sub;
            }
            return sub2;
        }

        private URLHandler createURLSubJarHandler(URL url) {
            String prefixName;
            String file = url.getFile();
            if (url.getFile().endsWith("!/")) {
                prefixName = "";
            } else {
                int sepIdx = file.lastIndexOf("!/");
                if (sepIdx == -1) {
                    return null;
                }
                prefixName = file.substring(sepIdx + 2);
            }
            try {
                URL jarURL = ((JarURLConnection) url.openConnection()).getJarFileURL();
                JarURLConnection juc = (JarURLConnection) new URL("jar", "", jarURL.toExternalForm() + "!/").openConnection();
                JarFile jf = juc.getJarFile();
                return URLClassLoader.this.new URLJarHandler(url, jarURL, jf, prefixName, null);
            } catch (IOException e) {
                return null;
            }
        }
    }

    class URLFileHandler extends URLHandler {
        private String prefix;

        public URLFileHandler(URL url) {
            super(url);
            String baseFile = url.getFile();
            String host = url.getHost();
            int hostLength = host != null ? host.length() : 0;
            StringBuilder buf = new StringBuilder(hostLength + 2 + baseFile.length());
            if (hostLength > 0) {
                buf.append("//").append(host);
            }
            buf.append(baseFile);
            this.prefix = buf.toString();
        }

        @Override
        Class<?> findClass(String packageName, String name, String origName) {
            String filename = this.prefix + name;
            try {
                File file = new File(URLDecoder.decode(filename, "UTF-8"));
                if (!file.exists()) {
                    return null;
                }
                try {
                    InputStream is = new FileInputStream(file);
                    return createClass(is, packageName, origName);
                } catch (FileNotFoundException e) {
                    return null;
                }
            } catch (UnsupportedEncodingException e2) {
                return null;
            } catch (IllegalArgumentException e3) {
                return null;
            }
        }

        @Override
        URL findResource(String name) {
            int idx = 0;
            while (idx < name.length() && (name.charAt(idx) == '/' || name.charAt(idx) == '\\')) {
                idx++;
            }
            if (idx > 0) {
                name = name.substring(idx);
            }
            try {
                String filename = URLDecoder.decode(this.prefix, "UTF-8") + name;
                if (new File(filename).exists()) {
                    return targetURL(this.url, name);
                }
                return null;
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }

    public URLClassLoader(URL[] urls) {
        this(urls, ClassLoader.getSystemClassLoader(), null);
    }

    public URLClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, null);
    }

    protected void addURL(URL url) {
        try {
            this.originalUrls.add(url);
            this.searchList.add(createSearchURL(url));
        } catch (MalformedURLException e) {
        }
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (name == null) {
            return null;
        }
        ArrayList<URL> result = new ArrayList<>();
        int n = 0;
        while (true) {
            int n2 = n + 1;
            URLHandler handler = getHandler(n);
            if (handler != null) {
                handler.findResources(name, result);
                n = n2;
            } else {
                return Collections.enumeration(result);
            }
        }
    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        PermissionCollection pc = super.getPermissions(codesource);
        URL u = codesource.getLocation();
        if (u.getProtocol().equals("jar")) {
            try {
                u = ((JarURLConnection) u.openConnection()).getJarFileURL();
            } catch (IOException e) {
            }
        }
        if (u.getProtocol().equals("file")) {
            String path = u.getFile();
            String host = u.getHost();
            if (host != null && host.length() > 0) {
                path = "//" + host + path;
            }
            if (File.separatorChar != '/') {
                path = path.replace('/', File.separatorChar);
            }
            if (isDirectory(u)) {
                pc.add(new FilePermission(path + "-", "read"));
            } else {
                pc.add(new FilePermission(path, "read"));
            }
        } else {
            String host2 = u.getHost();
            if (host2.length() == 0) {
                host2 = "localhost";
            }
            pc.add(new SocketPermission(host2, "connect, accept"));
        }
        return pc;
    }

    public URL[] getURLs() {
        return (URL[]) this.originalUrls.toArray(new URL[this.originalUrls.size()]);
    }

    private static boolean isDirectory(URL url) {
        String file = url.getFile();
        return file.length() > 0 && file.charAt(file.length() + (-1)) == '/';
    }

    public static URLClassLoader newInstance(URL[] urls) {
        return new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
    }

    public static URLClassLoader newInstance(URL[] urls, ClassLoader parentCl) {
        return new URLClassLoader(urls, parentCl);
    }

    public URLClassLoader(URL[] searchUrls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(parent);
        this.handlerMap = new HashMap();
        this.factory = factory;
        int nbUrls = searchUrls.length;
        this.originalUrls = new ArrayList<>(nbUrls);
        this.handlerList = new ArrayList<>(nbUrls);
        this.searchList = Collections.synchronizedList(new ArrayList(nbUrls));
        for (int i = 0; i < nbUrls; i++) {
            this.originalUrls.add(searchUrls[i]);
            try {
                this.searchList.add(createSearchURL(searchUrls[i]));
            } catch (MalformedURLException e) {
            }
        }
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        String partialName = className.replace('.', '/');
        String classFileName = partialName + ".class";
        String packageName = null;
        partialName.lastIndexOf(47);
        int position = partialName.lastIndexOf(47);
        if (position != -1) {
            packageName = partialName.substring(0, position);
        }
        int n = 0;
        while (true) {
            int n2 = n + 1;
            URLHandler handler = getHandler(n);
            if (handler != null) {
                Class<?> res = handler.findClass(packageName, classFileName, className);
                if (res != null) {
                    return res;
                }
                n = n2;
            } else {
                throw new ClassNotFoundException(className);
            }
        }
    }

    private URL createSearchURL(URL url) throws MalformedURLException {
        if (url != null) {
            String protocol = url.getProtocol();
            if (!isDirectory(url) && !protocol.equals("jar")) {
                if (this.factory == null) {
                    return new URL("jar", "", -1, url.toString() + "!/");
                }
                return new URL("jar", "", -1, url.toString() + "!/", this.factory.createURLStreamHandler("jar"));
            }
            return url;
        }
        return url;
    }

    @Override
    public URL findResource(String name) {
        if (name == null) {
            return null;
        }
        int n = 0;
        while (true) {
            int n2 = n + 1;
            URLHandler handler = getHandler(n);
            if (handler == null) {
                return null;
            }
            URL res = handler.findResource(name);
            if (res != null) {
                return res;
            }
            n = n2;
        }
    }

    private URLHandler getHandler(int num) {
        if (num < this.handlerList.size()) {
            return this.handlerList.get(num);
        }
        makeNewHandler();
        if (num < this.handlerList.size()) {
            return this.handlerList.get(num);
        }
        return null;
    }

    private synchronized void makeNewHandler() {
        URLHandler result;
        while (true) {
            if (this.searchList.isEmpty()) {
                break;
            }
            URL nextCandidate = this.searchList.remove(0);
            if (nextCandidate == null) {
                throw new NullPointerException("nextCandidate == null");
            }
            if (!this.handlerMap.containsKey(nextCandidate)) {
                String protocol = nextCandidate.getProtocol();
                if (protocol.equals("jar")) {
                    result = createURLJarHandler(nextCandidate);
                } else if (protocol.equals("file")) {
                    result = createURLFileHandler(nextCandidate);
                } else {
                    result = createURLHandler(nextCandidate);
                }
                if (result != null) {
                    this.handlerMap.put(nextCandidate, result);
                    this.handlerList.add(result);
                    break;
                }
            }
        }
    }

    private URLHandler createURLHandler(URL url) {
        return new URLHandler(url);
    }

    private URLHandler createURLFileHandler(URL url) {
        return new URLFileHandler(url);
    }

    private URLHandler createURLJarHandler(URL url) {
        String prefixName;
        String classpath;
        String file = url.getFile();
        if (url.getFile().endsWith("!/")) {
            prefixName = "";
        } else {
            int sepIdx = file.lastIndexOf("!/");
            if (sepIdx == -1) {
                return null;
            }
            prefixName = file.substring(sepIdx + 2);
        }
        try {
            URL jarURL = ((JarURLConnection) url.openConnection()).getJarFileURL();
            JarURLConnection juc = (JarURLConnection) new URL("jar", "", jarURL.toExternalForm() + "!/").openConnection();
            JarFile jf = juc.getJarFile();
            URLJarHandler jarH = new URLJarHandler(url, jarURL, jf, prefixName);
            if (jarH.getIndex() == null) {
                try {
                    Manifest manifest = jf.getManifest();
                    if (manifest != null && (classpath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH)) != null) {
                        this.searchList.addAll(0, getInternalURLs(url, classpath));
                        return jarH;
                    }
                    return jarH;
                } catch (IOException e) {
                    return jarH;
                }
            }
            return jarH;
        } catch (IOException e2) {
            return null;
        }
    }

    protected Package definePackage(String packageName, Manifest manifest, URL url) throws IllegalArgumentException {
        Attributes mainAttributes = manifest.getMainAttributes();
        String dirName = packageName.replace('.', '/') + "/";
        Attributes packageAttributes = manifest.getAttributes(dirName);
        boolean noEntry = false;
        if (packageAttributes == null) {
            noEntry = true;
            packageAttributes = mainAttributes;
        }
        String specificationTitle = packageAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
        if (specificationTitle == null && !noEntry) {
            specificationTitle = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
        }
        String specificationVersion = packageAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
        if (specificationVersion == null && !noEntry) {
            specificationVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
        }
        String specificationVendor = packageAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
        if (specificationVendor == null && !noEntry) {
            specificationVendor = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
        }
        String implementationTitle = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        if (implementationTitle == null && !noEntry) {
            implementationTitle = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        }
        String implementationVersion = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        if (implementationVersion == null && !noEntry) {
            implementationVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        }
        String implementationVendor = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
        if (implementationVendor == null && !noEntry) {
            implementationVendor = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
        }
        return definePackage(packageName, specificationTitle, specificationVersion, specificationVendor, implementationTitle, implementationVersion, implementationVendor, isSealed(manifest, dirName) ? url : null);
    }

    private boolean isSealed(Manifest manifest, String dirName) {
        String value;
        Attributes attributes = manifest.getAttributes(dirName);
        if (attributes != null && (value = attributes.getValue(Attributes.Name.SEALED)) != null) {
            return value.equalsIgnoreCase("true");
        }
        Attributes mainAttributes = manifest.getMainAttributes();
        String value2 = mainAttributes.getValue(Attributes.Name.SEALED);
        return value2 != null && value2.equalsIgnoreCase("true");
    }

    private ArrayList<URL> getInternalURLs(URL root, String classpath) {
        StringTokenizer tokenizer = new StringTokenizer(classpath);
        ArrayList<URL> addedURLs = new ArrayList<>();
        String file = root.getFile();
        int jarIndex = file.lastIndexOf("!/") - 1;
        int index = file.lastIndexOf("/", jarIndex) + 1;
        if (index == 0) {
            index = file.lastIndexOf(System.getProperty("file.separator"), jarIndex) + 1;
        }
        String file2 = file.substring(0, index);
        while (tokenizer.hasMoreElements()) {
            String element = tokenizer.nextToken();
            if (!element.isEmpty()) {
                try {
                    URL url = new URL(new URL(file2), element);
                    addedURLs.add(createSearchURL(url));
                } catch (MalformedURLException e) {
                }
            }
        }
        return addedURLs;
    }
}
