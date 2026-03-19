package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.net.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.StringTokenizer;
import sun.net.ApplicationProxy;
import sun.net.www.protocol.file.Handler;
import sun.security.util.SecurityConstants;

public final class URL implements Serializable {
    static URLStreamHandlerFactory factory = null;
    private static final String protocolPathProp = "java.protocol.handler.pkgs";
    static final long serialVersionUID = -7627629688361524110L;
    private String authority;
    private String file;
    transient URLStreamHandler handler;
    private int hashCode;
    private String host;
    transient InetAddress hostAddress;
    private transient String path;
    private int port;
    private String protocol;
    private transient String query;
    private String ref;
    private transient UrlDeserializedState tempState;
    private transient String userInfo;
    private static final Set<String> BUILTIN_HANDLER_CLASS_NAMES = createBuiltinHandlerClassNames();
    static Hashtable<String, URLStreamHandler> handlers = new Hashtable<>();
    private static Object streamHandlerLock = new Object();
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("protocol", String.class), new ObjectStreamField("host", String.class), new ObjectStreamField("port", Integer.TYPE), new ObjectStreamField("authority", String.class), new ObjectStreamField("file", String.class), new ObjectStreamField("ref", String.class)};

    public URL(String str, String str2, int i, String str3) throws MalformedURLException {
        this(str, str2, i, str3, null);
    }

    public URL(String str, String str2, String str3) throws MalformedURLException {
        this(str, str2, -1, str3);
    }

    public URL(String str, String str2, int i, String str3, URLStreamHandler uRLStreamHandler) throws MalformedURLException {
        String str4;
        SecurityManager securityManager;
        this.port = -1;
        this.hashCode = -1;
        if (uRLStreamHandler != null && (securityManager = System.getSecurityManager()) != null) {
            checkSpecifyHandler(securityManager);
        }
        String lowerCase = str.toLowerCase();
        this.protocol = lowerCase;
        if (str2 != null) {
            if (str2.indexOf(58) >= 0 && !str2.startsWith("[")) {
                str2 = "[" + str2 + "]";
            }
            this.host = str2;
            if (i < -1) {
                throw new MalformedURLException("Invalid port number :" + i);
            }
            this.port = i;
            if (i == -1) {
                str4 = str2;
            } else {
                str4 = str2 + ":" + i;
            }
            this.authority = str4;
        }
        Parts parts = new Parts(str3, str2);
        this.path = parts.getPath();
        this.query = parts.getQuery();
        if (this.query != null) {
            this.file = this.path + "?" + this.query;
        } else {
            this.file = this.path;
        }
        this.ref = parts.getRef();
        if (uRLStreamHandler == null && (uRLStreamHandler = getURLStreamHandler(lowerCase)) == null) {
            throw new MalformedURLException("unknown protocol: " + lowerCase);
        }
        this.handler = uRLStreamHandler;
    }

    public URL(String str) throws MalformedURLException {
        this(null, str);
    }

    public URL(URL url, String str) throws MalformedURLException {
        this(url, str, (URLStreamHandler) null);
    }

    public URL(URL url, String str, URLStreamHandler uRLStreamHandler) throws MalformedURLException {
        boolean z;
        String lowerCase;
        char cCharAt;
        SecurityManager securityManager;
        this.port = -1;
        this.hashCode = -1;
        if (uRLStreamHandler != null && (securityManager = System.getSecurityManager()) != null) {
            checkSpecifyHandler(securityManager);
        }
        try {
            int length = str.length();
            while (length > 0 && str.charAt(length - 1) <= ' ') {
                length--;
            }
            boolean z2 = false;
            int i = 0;
            while (i < length && str.charAt(i) <= ' ') {
                i++;
            }
            i = str.regionMatches(true, i, "url:", 0, 4) ? i + 4 : i;
            if (i >= str.length() || str.charAt(i) != '#') {
                z = false;
            } else {
                z = true;
            }
            int i2 = i;
            while (true) {
                if (z || i2 >= length || (cCharAt = str.charAt(i2)) == '/') {
                    break;
                }
                if (cCharAt != ':') {
                    i2++;
                } else {
                    lowerCase = str.substring(i, i2).toLowerCase();
                    i = isValidProtocol(lowerCase) ? i2 + 1 : i;
                }
            }
            lowerCase = null;
            this.protocol = lowerCase;
            if (url != null && (lowerCase == null || lowerCase.equalsIgnoreCase(url.protocol))) {
                uRLStreamHandler = uRLStreamHandler == null ? url.handler : uRLStreamHandler;
                if (url.path != null && url.path.startsWith("/")) {
                    lowerCase = null;
                }
                if (lowerCase == null) {
                    this.protocol = url.protocol;
                    this.authority = url.authority;
                    this.userInfo = url.userInfo;
                    this.host = url.host;
                    this.port = url.port;
                    this.file = url.file;
                    this.path = url.path;
                    z2 = true;
                }
            }
            if (this.protocol == null) {
                throw new MalformedURLException("no protocol: " + str);
            }
            if (uRLStreamHandler == null && (uRLStreamHandler = getURLStreamHandler(this.protocol)) == null) {
                throw new MalformedURLException("unknown protocol: " + this.protocol);
            }
            this.handler = uRLStreamHandler;
            int iIndexOf = str.indexOf(35, i);
            if (iIndexOf >= 0) {
                this.ref = str.substring(iIndexOf + 1, length);
                length = iIndexOf;
            }
            if (z2 && i == length) {
                this.query = url.query;
                if (this.ref == null) {
                    this.ref = url.ref;
                }
            }
            uRLStreamHandler.parseURL(this, str, i, length);
        } catch (MalformedURLException e) {
            throw e;
        } catch (Exception e2) {
            MalformedURLException malformedURLException = new MalformedURLException(e2.getMessage());
            malformedURLException.initCause(e2);
            throw malformedURLException;
        }
    }

    private boolean isValidProtocol(String str) {
        int length = str.length();
        if (length < 1 || !Character.isLetter(str.charAt(0))) {
            return false;
        }
        for (int i = 1; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (!Character.isLetterOrDigit(cCharAt) && cCharAt != '.' && cCharAt != '+' && cCharAt != '-') {
                return false;
            }
        }
        return true;
    }

    private void checkSpecifyHandler(SecurityManager securityManager) {
        securityManager.checkPermission(SecurityConstants.SPECIFY_HANDLER_PERMISSION);
    }

    void set(String str, String str2, int i, String str3, String str4) {
        synchronized (this) {
            this.protocol = str;
            this.host = str2;
            if (i != -1) {
                str2 = str2 + ":" + i;
            }
            this.authority = str2;
            this.port = i;
            this.file = str3;
            this.ref = str4;
            this.hashCode = -1;
            this.hostAddress = null;
            int iLastIndexOf = str3.lastIndexOf(63);
            if (iLastIndexOf != -1) {
                this.query = str3.substring(iLastIndexOf + 1);
                this.path = str3.substring(0, iLastIndexOf);
            } else {
                this.path = str3;
            }
        }
    }

    void set(String str, String str2, int i, String str3, String str4, String str5, String str6, String str7) {
        String str8;
        synchronized (this) {
            this.protocol = str;
            this.host = str2;
            this.port = i;
            if (str6 == null || str6.isEmpty()) {
                str8 = str5;
            } else {
                str8 = str5 + "?" + str6;
            }
            this.file = str8;
            this.userInfo = str4;
            this.path = str5;
            this.ref = str7;
            this.hashCode = -1;
            this.hostAddress = null;
            this.query = str6;
            this.authority = str3;
        }
    }

    public String getQuery() {
        return this.query;
    }

    public String getPath() {
        return this.path;
    }

    public String getUserInfo() {
        return this.userInfo;
    }

    public String getAuthority() {
        return this.authority;
    }

    public int getPort() {
        return this.port;
    }

    public int getDefaultPort() {
        return this.handler.getDefaultPort();
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getHost() {
        return this.host;
    }

    public String getFile() {
        return this.file;
    }

    public String getRef() {
        return this.ref;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof URL)) {
            return false;
        }
        return this.handler.equals(this, (URL) obj);
    }

    public synchronized int hashCode() {
        if (this.hashCode != -1) {
            return this.hashCode;
        }
        this.hashCode = this.handler.hashCode(this);
        return this.hashCode;
    }

    public boolean sameFile(URL url) {
        return this.handler.sameFile(this, url);
    }

    public String toString() {
        return toExternalForm();
    }

    public String toExternalForm() {
        return this.handler.toExternalForm(this);
    }

    public URI toURI() throws URISyntaxException {
        return new URI(toString());
    }

    public URLConnection openConnection() throws IOException {
        return this.handler.openConnection(this);
    }

    public URLConnection openConnection(Proxy proxy) throws IOException {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy can not be null");
        }
        Proxy proxyCreate = proxy == Proxy.NO_PROXY ? Proxy.NO_PROXY : ApplicationProxy.create(proxy);
        SecurityManager securityManager = System.getSecurityManager();
        if (proxyCreate.type() != Proxy.Type.DIRECT && securityManager != null) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) proxyCreate.address();
            if (inetSocketAddress.isUnresolved()) {
                securityManager.checkConnect(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
            } else {
                securityManager.checkConnect(inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
            }
        }
        return this.handler.openConnection(this, proxyCreate);
    }

    public final InputStream openStream() throws IOException {
        return openConnection().getInputStream();
    }

    public final Object getContent() throws IOException {
        return openConnection().getContent();
    }

    public final Object getContent(Class[] clsArr) throws IOException {
        return openConnection().getContent(clsArr);
    }

    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory uRLStreamHandlerFactory) {
        synchronized (streamHandlerLock) {
            if (factory != null) {
                throw new Error("factory already defined");
            }
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkSetFactory();
            }
            handlers.clear();
            factory = uRLStreamHandlerFactory;
        }
    }

    static URLStreamHandler getURLStreamHandler(String str) throws ClassNotFoundException {
        URLStreamHandler uRLStreamHandlerCreateBuiltinHandler = handlers.get(str);
        if (uRLStreamHandlerCreateBuiltinHandler == null) {
            boolean z = false;
            if (factory != null) {
                uRLStreamHandlerCreateBuiltinHandler = factory.createURLStreamHandler(str);
                z = true;
            }
            if (uRLStreamHandlerCreateBuiltinHandler == null) {
                StringTokenizer stringTokenizer = new StringTokenizer(System.getProperty(protocolPathProp, ""), "|");
                while (uRLStreamHandlerCreateBuiltinHandler == null && stringTokenizer.hasMoreTokens()) {
                    try {
                        String str2 = stringTokenizer.nextToken().trim() + "." + str + ".Handler";
                        Class<?> cls = null;
                        try {
                            cls = Class.forName(str2, true, ClassLoader.getSystemClassLoader());
                        } catch (ClassNotFoundException e) {
                            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                            if (contextClassLoader != null) {
                                cls = Class.forName(str2, true, contextClassLoader);
                            }
                        }
                        if (cls != null) {
                            uRLStreamHandlerCreateBuiltinHandler = (URLStreamHandler) cls.newInstance();
                        }
                    } catch (ReflectiveOperationException e2) {
                    }
                }
            }
            if (uRLStreamHandlerCreateBuiltinHandler == null) {
                try {
                    uRLStreamHandlerCreateBuiltinHandler = createBuiltinHandler(str);
                } catch (Exception e3) {
                    throw new AssertionError(e3);
                }
            }
            synchronized (streamHandlerLock) {
                URLStreamHandler uRLStreamHandlerCreateURLStreamHandler = handlers.get(str);
                if (uRLStreamHandlerCreateURLStreamHandler != null) {
                    return uRLStreamHandlerCreateURLStreamHandler;
                }
                if (!z && factory != null) {
                    uRLStreamHandlerCreateURLStreamHandler = factory.createURLStreamHandler(str);
                }
                if (uRLStreamHandlerCreateURLStreamHandler != null) {
                    uRLStreamHandlerCreateBuiltinHandler = uRLStreamHandlerCreateURLStreamHandler;
                }
                if (uRLStreamHandlerCreateBuiltinHandler != null) {
                    handlers.put(str, uRLStreamHandlerCreateBuiltinHandler);
                }
            }
        }
        return uRLStreamHandlerCreateBuiltinHandler;
    }

    private static URLStreamHandler createBuiltinHandler(String str) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        if (str.equals("file")) {
            return new Handler();
        }
        if (str.equals("ftp")) {
            return new sun.net.www.protocol.ftp.Handler();
        }
        if (str.equals("jar")) {
            return new sun.net.www.protocol.jar.Handler();
        }
        if (str.equals("http")) {
            return (URLStreamHandler) Class.forName("com.android.okhttp.HttpHandler").newInstance();
        }
        if (str.equals("https")) {
            return (URLStreamHandler) Class.forName("com.android.okhttp.HttpsHandler").newInstance();
        }
        return null;
    }

    private static Set<String> createBuiltinHandlerClassNames() {
        HashSet hashSet = new HashSet();
        hashSet.add("sun.net.www.protocol.file.Handler");
        hashSet.add("sun.net.www.protocol.ftp.Handler");
        hashSet.add("sun.net.www.protocol.jar.Handler");
        hashSet.add("com.android.okhttp.HttpHandler");
        hashSet.add("com.android.okhttp.HttpsHandler");
        return Collections.unmodifiableSet(hashSet);
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
    }

    private synchronized void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        String str;
        String str2;
        String str3;
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        String str4 = (String) fields.get("protocol", (Object) null);
        if (getURLStreamHandler(str4) == null) {
            throw new IOException("unknown protocol: " + str4);
        }
        String str5 = (String) fields.get("host", (Object) null);
        int i = fields.get("port", -1);
        String str6 = (String) fields.get("authority", (Object) null);
        String str7 = (String) fields.get("file", (Object) null);
        String str8 = (String) fields.get("ref", (Object) null);
        if (str6 != null || ((str5 == null || str5.length() <= 0) && i == -1)) {
            str = str5;
            str2 = str6;
        } else {
            if (str5 == null) {
                str5 = "";
            }
            if (i != -1) {
                str3 = str5 + ":" + i;
            } else {
                str3 = str5;
            }
            str2 = str3;
            str = str5;
        }
        this.tempState = new UrlDeserializedState(str4, str, i, str2, str7, str8, -1);
    }

    private Object readResolve() throws ObjectStreamException, ClassNotFoundException {
        URLStreamHandler uRLStreamHandler = getURLStreamHandler(this.tempState.getProtocol());
        if (isBuiltinStreamHandler(uRLStreamHandler.getClass().getName())) {
            return fabricateNewURL();
        }
        return setDeserializedFields(uRLStreamHandler);
    }

    private URL setDeserializedFields(URLStreamHandler uRLStreamHandler) {
        String strSubstring;
        int iIndexOf;
        String strSubstring2;
        String strSubstring3;
        String protocol = this.tempState.getProtocol();
        String host = this.tempState.getHost();
        int port = this.tempState.getPort();
        String authority = this.tempState.getAuthority();
        String file = this.tempState.getFile();
        String ref = this.tempState.getRef();
        int hashCode = this.tempState.getHashCode();
        String strSubstring4 = null;
        if (authority == null && ((host != null && host.length() > 0) || port != -1)) {
            if (host == null) {
                host = "";
            }
            if (port != -1) {
                authority = host + ":" + port;
            } else {
                authority = host;
            }
            int iLastIndexOf = host.lastIndexOf(64);
            if (iLastIndexOf != -1) {
                strSubstring3 = host.substring(0, iLastIndexOf);
                host = host.substring(iLastIndexOf + 1);
            } else {
                strSubstring3 = null;
            }
            strSubstring = strSubstring3;
        } else {
            strSubstring = (authority == null || (iIndexOf = authority.indexOf(64)) == -1) ? null : authority.substring(0, iIndexOf);
        }
        if (file != null) {
            int iLastIndexOf2 = file.lastIndexOf(63);
            if (iLastIndexOf2 != -1) {
                strSubstring4 = file.substring(iLastIndexOf2 + 1);
                strSubstring2 = file.substring(0, iLastIndexOf2);
            } else {
                strSubstring2 = file;
            }
        } else {
            strSubstring2 = null;
        }
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.file = file;
        this.authority = authority;
        this.ref = ref;
        this.hashCode = hashCode;
        this.handler = uRLStreamHandler;
        this.query = strSubstring4;
        this.path = strSubstring2;
        this.userInfo = strSubstring;
        return this;
    }

    private URL fabricateNewURL() throws InvalidObjectException {
        String strReconstituteUrlString = this.tempState.reconstituteUrlString();
        try {
            URL url = new URL(strReconstituteUrlString);
            url.setSerializedHashCode(this.tempState.getHashCode());
            resetState();
            return url;
        } catch (MalformedURLException e) {
            resetState();
            InvalidObjectException invalidObjectException = new InvalidObjectException("Malformed URL: " + strReconstituteUrlString);
            invalidObjectException.initCause(e);
            throw invalidObjectException;
        }
    }

    private boolean isBuiltinStreamHandler(String str) {
        return BUILTIN_HANDLER_CLASS_NAMES.contains(str);
    }

    private void resetState() {
        this.protocol = null;
        this.host = null;
        this.port = -1;
        this.file = null;
        this.authority = null;
        this.ref = null;
        this.hashCode = -1;
        this.handler = null;
        this.query = null;
        this.path = null;
        this.userInfo = null;
        this.tempState = null;
    }

    private void setSerializedHashCode(int i) {
        this.hashCode = i;
    }
}
