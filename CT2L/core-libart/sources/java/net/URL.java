package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;
import libcore.net.url.FileHandler;
import libcore.net.url.FtpHandler;
import libcore.net.url.JarHandler;
import libcore.net.url.UrlUtils;

public final class URL implements Serializable {
    private static final long serialVersionUID = -7627629688361524110L;
    private static URLStreamHandlerFactory streamHandlerFactory;
    private static final Hashtable<String, URLStreamHandler> streamHandlers = new Hashtable<>();
    private String authority;
    private String file;
    private transient int hashCode;
    private String host;
    private transient String path;
    private int port;
    private String protocol;
    private transient String query;
    private String ref;
    transient URLStreamHandler streamHandler;
    private transient String userInfo;

    public static synchronized void setURLStreamHandlerFactory(URLStreamHandlerFactory factory) {
        if (streamHandlerFactory != null) {
            throw new Error("Factory already set");
        }
        streamHandlers.clear();
        streamHandlerFactory = factory;
    }

    public URL(String spec) throws MalformedURLException {
        this((URL) null, spec, (URLStreamHandler) null);
    }

    public URL(URL context, String spec) throws MalformedURLException {
        this(context, spec, (URLStreamHandler) null);
    }

    public URL(URL context, String spec, URLStreamHandler handler) throws MalformedURLException {
        this.port = -1;
        if (spec == null) {
            throw new MalformedURLException();
        }
        if (handler != null) {
            this.streamHandler = handler;
        }
        String spec2 = spec.trim();
        this.protocol = UrlUtils.getSchemePrefix(spec2);
        int schemeSpecificPartStart = this.protocol != null ? this.protocol.length() + 1 : 0;
        if (this.protocol != null && context != null && !this.protocol.equals(context.protocol)) {
            context = null;
        }
        if (context != null) {
            set(context.protocol, context.getHost(), context.getPort(), context.getAuthority(), context.getUserInfo(), context.getPath(), context.getQuery(), context.getRef());
            if (this.streamHandler == null) {
                this.streamHandler = context.streamHandler;
            }
        } else if (this.protocol == null) {
            throw new MalformedURLException("Protocol not found: " + spec2);
        }
        if (this.streamHandler == null) {
            setupStreamHandler();
            if (this.streamHandler == null) {
                throw new MalformedURLException("Unknown protocol: " + this.protocol);
            }
        }
        try {
            this.streamHandler.parseURL(this, spec2, schemeSpecificPartStart, spec2.length());
        } catch (Exception e) {
            throw new MalformedURLException(e.toString());
        }
    }

    public URL(String protocol, String host, String file) throws MalformedURLException {
        this(protocol, host, -1, file, null);
    }

    public URL(String protocol, String host, int port, String file) throws MalformedURLException {
        this(protocol, host, port, file, null);
    }

    public URL(String protocol, String host, int port, String file, URLStreamHandler handler) throws MalformedURLException {
        this.port = -1;
        if (port < -1) {
            throw new MalformedURLException("port < -1: " + port);
        }
        if (protocol == null) {
            throw new NullPointerException("protocol == null");
        }
        if (host != null && host.contains(":") && host.charAt(0) != '[') {
            host = "[" + host + "]";
        }
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        String file2 = UrlUtils.authoritySafePath(host, file);
        int hash = file2.indexOf("#");
        if (hash != -1) {
            this.file = file2.substring(0, hash);
            this.ref = file2.substring(hash + 1);
        } else {
            this.file = file2;
        }
        fixURL(false);
        if (handler == null) {
            setupStreamHandler();
            if (this.streamHandler == null) {
                throw new MalformedURLException("Unknown protocol: " + protocol);
            }
            return;
        }
        this.streamHandler = handler;
    }

    void fixURL(boolean fixHost) {
        int index;
        int index2;
        if (this.host != null && this.host.length() > 0) {
            this.authority = this.host;
            if (this.port != -1) {
                this.authority += ":" + this.port;
            }
        }
        if (fixHost) {
            if (this.host != null && (index2 = this.host.lastIndexOf(64)) > -1) {
                this.userInfo = this.host.substring(0, index2);
                this.host = this.host.substring(index2 + 1);
            } else {
                this.userInfo = null;
            }
        }
        if (this.file != null && (index = this.file.indexOf(63)) > -1) {
            this.query = this.file.substring(index + 1);
            this.path = this.file.substring(0, index);
        } else {
            this.query = null;
            this.path = this.file;
        }
    }

    protected void set(String protocol, String host, int port, String file, String ref) {
        if (this.protocol == null) {
            this.protocol = protocol;
        }
        this.host = host;
        this.file = file;
        this.port = port;
        this.ref = ref;
        this.hashCode = 0;
        fixURL(true);
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (getClass() == o.getClass()) {
            return this.streamHandler.equals(this, (URL) o);
        }
        return false;
    }

    public boolean sameFile(URL otherURL) {
        return this.streamHandler.sameFile(this, otherURL);
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = this.streamHandler.hashCode(this);
        }
        return this.hashCode;
    }

    void setupStreamHandler() {
        this.streamHandler = streamHandlers.get(this.protocol);
        if (this.streamHandler == null) {
            if (streamHandlerFactory != null) {
                this.streamHandler = streamHandlerFactory.createURLStreamHandler(this.protocol);
                if (this.streamHandler != null) {
                    streamHandlers.put(this.protocol, this.streamHandler);
                    return;
                }
            }
            String packageList = System.getProperty("java.protocol.handler.pkgs");
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (packageList != null && contextClassLoader != null) {
                String[] arr$ = packageList.split("\\|");
                for (String packageName : arr$) {
                    String className = packageName + "." + this.protocol + ".Handler";
                    try {
                        Class<?> c = contextClassLoader.loadClass(className);
                        this.streamHandler = (URLStreamHandler) c.newInstance();
                        if (this.streamHandler != null) {
                            streamHandlers.put(this.protocol, this.streamHandler);
                            return;
                        }
                        return;
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    }
                }
            }
            if (this.protocol.equals("file")) {
                this.streamHandler = new FileHandler();
            } else if (this.protocol.equals("ftp")) {
                this.streamHandler = new FtpHandler();
            } else if (this.protocol.equals("http")) {
                try {
                    this.streamHandler = (URLStreamHandler) Class.forName("com.android.okhttp.HttpHandler").newInstance();
                } catch (Exception e2) {
                    throw new AssertionError(e2);
                }
            } else if (this.protocol.equals("https")) {
                try {
                    this.streamHandler = (URLStreamHandler) Class.forName("com.android.okhttp.HttpsHandler").newInstance();
                } catch (Exception e3) {
                    throw new AssertionError(e3);
                }
            } else if (this.protocol.equals("jar")) {
                this.streamHandler = new JarHandler();
            }
            if (this.streamHandler != null) {
                streamHandlers.put(this.protocol, this.streamHandler);
            }
        }
    }

    public final Object getContent() throws IOException {
        return openConnection().getContent();
    }

    public final Object getContent(Class[] types) throws IOException {
        return openConnection().getContent(types);
    }

    public final InputStream openStream() throws IOException {
        return openConnection().getInputStream();
    }

    public URLConnection openConnection() throws IOException {
        return this.streamHandler.openConnection(this);
    }

    public URLConnection openConnection(Proxy proxy) throws IOException {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy == null");
        }
        return this.streamHandler.openConnection(this, proxy);
    }

    public URI toURI() throws URISyntaxException {
        return new URI(toExternalForm());
    }

    public URI toURILenient() throws URISyntaxException {
        if (this.streamHandler == null) {
            throw new IllegalStateException(this.protocol);
        }
        return new URI(this.streamHandler.toExternalForm(this, true));
    }

    public String toString() {
        return toExternalForm();
    }

    public String toExternalForm() {
        return this.streamHandler == null ? "unknown protocol(" + this.protocol + ")://" + this.host + this.file : this.streamHandler.toExternalForm(this);
    }

    private void readObject(ObjectInputStream stream) throws IOException {
        int index;
        try {
            stream.defaultReadObject();
            if (this.host != null && this.authority == null) {
                fixURL(true);
            } else if (this.authority != null) {
                int index2 = this.authority.lastIndexOf(64);
                if (index2 > -1) {
                    this.userInfo = this.authority.substring(0, index2);
                }
                if (this.file != null && (index = this.file.indexOf(63)) > -1) {
                    this.query = this.file.substring(index + 1);
                    this.path = this.file.substring(0, index);
                } else {
                    this.path = this.file;
                }
            }
            setupStreamHandler();
            if (this.streamHandler == null) {
                throw new IOException("Unknown protocol: " + this.protocol);
            }
            this.hashCode = 0;
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    public int getEffectivePort() {
        return URI.getEffectivePort(this.protocol, this.port);
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getAuthority() {
        return this.authority;
    }

    public String getUserInfo() {
        return this.userInfo;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public int getDefaultPort() {
        return this.streamHandler.getDefaultPort();
    }

    public String getFile() {
        return this.file;
    }

    public String getPath() {
        return this.path;
    }

    public String getQuery() {
        return this.query;
    }

    public String getRef() {
        return this.ref;
    }

    protected void set(String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref) {
        String file = path;
        if (query != null && !query.isEmpty()) {
            file = file + "?" + query;
        }
        set(protocol, host, port, file, ref);
        this.authority = authority;
        this.userInfo = userInfo;
        this.path = path;
        this.query = query;
    }
}
