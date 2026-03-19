package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.Permission;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import sun.net.www.MessageHeader;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;
import sun.security.x509.InvalidityDateExtension;

public abstract class URLConnection {
    private static final String contentClassPrefix = "sun.net.www.content";
    private static final String contentPathProp = "java.content.handler.pkgs";
    static ContentHandlerFactory factory;
    private static FileNameMap fileNameMap;
    private int connectTimeout;
    private int readTimeout;
    private MessageHeader requests;
    protected URL url;
    private int writeTimeout;
    private static boolean defaultAllowUserInteraction = false;
    private static boolean defaultUseCaches = true;
    private static Hashtable<String, ContentHandler> handlers = new Hashtable<>();
    protected boolean doInput = true;
    protected boolean doOutput = false;
    protected boolean allowUserInteraction = defaultAllowUserInteraction;
    protected boolean useCaches = defaultUseCaches;
    protected long ifModifiedSince = 0;
    protected boolean connected = false;

    public abstract void connect() throws IOException;

    public static synchronized FileNameMap getFileNameMap() {
        if (fileNameMap == null) {
            fileNameMap = new DefaultFileNameMap();
        }
        return fileNameMap;
    }

    public static void setFileNameMap(FileNameMap fileNameMap2) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSetFactory();
        }
        fileNameMap = fileNameMap2;
    }

    public void setConnectTimeout(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        this.connectTimeout = i;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setReadTimeout(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        this.readTimeout = i;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public void setWriteTimeout(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        this.writeTimeout = i;
    }

    public int getWriteTimeout() {
        return this.writeTimeout;
    }

    protected URLConnection(URL url) {
        this.url = url;
    }

    public URL getURL() {
        return this.url;
    }

    public int getContentLength() {
        long contentLengthLong = getContentLengthLong();
        if (contentLengthLong > 2147483647L) {
            return -1;
        }
        return (int) contentLengthLong;
    }

    public long getContentLengthLong() {
        return getHeaderFieldLong("content-length", -1L);
    }

    public String getContentType() {
        return getHeaderField("content-type");
    }

    public String getContentEncoding() {
        return getHeaderField("content-encoding");
    }

    public long getExpiration() {
        return getHeaderFieldDate("expires", 0L);
    }

    public long getDate() {
        return getHeaderFieldDate(InvalidityDateExtension.DATE, 0L);
    }

    public long getLastModified() {
        return getHeaderFieldDate("last-modified", 0L);
    }

    public String getHeaderField(String str) {
        return null;
    }

    public Map<String, List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }

    public int getHeaderFieldInt(String str, int i) {
        try {
            return Integer.parseInt(getHeaderField(str));
        } catch (Exception e) {
            return i;
        }
    }

    public long getHeaderFieldLong(String str, long j) {
        try {
            return Long.parseLong(getHeaderField(str));
        } catch (Exception e) {
            return j;
        }
    }

    public long getHeaderFieldDate(String str, long j) {
        try {
            return Date.parse(getHeaderField(str));
        } catch (Exception e) {
            return j;
        }
    }

    public String getHeaderFieldKey(int i) {
        return null;
    }

    public String getHeaderField(int i) {
        return null;
    }

    public Object getContent() throws IOException {
        getInputStream();
        return getContentHandler().getContent(this);
    }

    public Object getContent(Class[] clsArr) throws IOException {
        getInputStream();
        return getContentHandler().getContent(this, clsArr);
    }

    public Permission getPermission() throws IOException {
        return SecurityConstants.ALL_PERMISSION;
    }

    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support input");
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support output");
    }

    public String toString() {
        return getClass().getName() + ":" + ((Object) this.url);
    }

    public void setDoInput(boolean z) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        this.doInput = z;
    }

    public boolean getDoInput() {
        return this.doInput;
    }

    public void setDoOutput(boolean z) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        this.doOutput = z;
    }

    public boolean getDoOutput() {
        return this.doOutput;
    }

    public void setAllowUserInteraction(boolean z) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        this.allowUserInteraction = z;
    }

    public boolean getAllowUserInteraction() {
        return this.allowUserInteraction;
    }

    public static void setDefaultAllowUserInteraction(boolean z) {
        defaultAllowUserInteraction = z;
    }

    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }

    public void setUseCaches(boolean z) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        this.useCaches = z;
    }

    public boolean getUseCaches() {
        return this.useCaches;
    }

    public void setIfModifiedSince(long j) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        this.ifModifiedSince = j;
    }

    public long getIfModifiedSince() {
        return this.ifModifiedSince;
    }

    public boolean getDefaultUseCaches() {
        return defaultUseCaches;
    }

    public void setDefaultUseCaches(boolean z) {
        defaultUseCaches = z;
    }

    public void setRequestProperty(String str, String str2) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        if (str == null) {
            throw new NullPointerException("key is null");
        }
        if (this.requests == null) {
            this.requests = new MessageHeader();
        }
        this.requests.set(str, str2);
    }

    public void addRequestProperty(String str, String str2) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        if (str == null) {
            throw new NullPointerException("key is null");
        }
        if (this.requests == null) {
            this.requests = new MessageHeader();
        }
        this.requests.add(str, str2);
    }

    public String getRequestProperty(String str) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        if (this.requests == null) {
            return null;
        }
        return this.requests.findValue(str);
    }

    public Map<String, List<String>> getRequestProperties() {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        if (this.requests == null) {
            return Collections.emptyMap();
        }
        return this.requests.getHeaders(null);
    }

    @Deprecated
    public static void setDefaultRequestProperty(String str, String str2) {
    }

    @Deprecated
    public static String getDefaultRequestProperty(String str) {
        return null;
    }

    public static synchronized void setContentHandlerFactory(ContentHandlerFactory contentHandlerFactory) {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSetFactory();
        }
        factory = contentHandlerFactory;
    }

    synchronized ContentHandler getContentHandler() throws IOException {
        ContentHandler contentHandler;
        String strStripOffParameters = stripOffParameters(getContentType());
        ContentHandler contentHandlerLookupContentHandlerClassFor = null;
        if (strStripOffParameters == null && (strStripOffParameters = guessContentTypeFromName(this.url.getFile())) == null) {
            strStripOffParameters = guessContentTypeFromStream(getInputStream());
        }
        if (strStripOffParameters == null) {
            return UnknownContentHandler.INSTANCE;
        }
        try {
            contentHandler = handlers.get(strStripOffParameters);
        } catch (Exception e) {
        }
        if (contentHandler != null) {
            return contentHandler;
        }
        contentHandlerLookupContentHandlerClassFor = contentHandler;
        if (factory != null) {
            contentHandlerLookupContentHandlerClassFor = factory.createContentHandler(strStripOffParameters);
        }
        if (contentHandlerLookupContentHandlerClassFor == null) {
            try {
                contentHandlerLookupContentHandlerClassFor = lookupContentHandlerClassFor(strStripOffParameters);
            } catch (Exception e2) {
                e2.printStackTrace();
                contentHandlerLookupContentHandlerClassFor = UnknownContentHandler.INSTANCE;
            }
            handlers.put(strStripOffParameters, contentHandlerLookupContentHandlerClassFor);
        }
        return contentHandlerLookupContentHandlerClassFor;
    }

    private String stripOffParameters(String str) {
        if (str == null) {
            return null;
        }
        int iIndexOf = str.indexOf(59);
        if (iIndexOf > 0) {
            return str.substring(0, iIndexOf);
        }
        return str;
    }

    private ContentHandler lookupContentHandlerClassFor(String str) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        String strTypeToPackageName = typeToPackageName(str);
        StringTokenizer stringTokenizer = new StringTokenizer(getContentHandlerPkgPrefixes(), "|");
        while (stringTokenizer.hasMoreTokens()) {
            try {
                String str2 = stringTokenizer.nextToken().trim() + "." + strTypeToPackageName;
                Class<?> clsLoadClass = null;
                try {
                    clsLoadClass = Class.forName(str2);
                } catch (ClassNotFoundException e) {
                    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                    if (systemClassLoader != null) {
                        clsLoadClass = systemClassLoader.loadClass(str2);
                    }
                }
                if (clsLoadClass != null) {
                    return (ContentHandler) clsLoadClass.newInstance();
                }
                continue;
            } catch (Exception e2) {
            }
        }
        return UnknownContentHandler.INSTANCE;
    }

    private String typeToPackageName(String str) {
        String lowerCase = str.toLowerCase();
        int length = lowerCase.length();
        char[] cArr = new char[length];
        lowerCase.getChars(0, length, cArr, 0);
        for (int i = 0; i < length; i++) {
            char c = cArr[i];
            if (c == '/') {
                cArr[i] = '.';
            } else if (('A' > c || c > 'Z') && (('a' > c || c > 'z') && ('0' > c || c > '9'))) {
                cArr[i] = '_';
            }
        }
        return new String(cArr);
    }

    private String getContentHandlerPkgPrefixes() {
        String str = (String) AccessController.doPrivileged(new GetPropertyAction(contentPathProp, ""));
        if (str != "") {
            str = str + "|";
        }
        return str + contentClassPrefix;
    }

    public static String guessContentTypeFromName(String str) {
        return getFileNameMap().getContentTypeFor(str);
    }

    public static String guessContentTypeFromStream(InputStream inputStream) throws IOException {
        int i;
        int i2;
        if (!inputStream.markSupported()) {
            return null;
        }
        inputStream.mark(16);
        int i3 = inputStream.read();
        int i4 = inputStream.read();
        int i5 = inputStream.read();
        int i6 = inputStream.read();
        int i7 = inputStream.read();
        int i8 = inputStream.read();
        int i9 = inputStream.read();
        int i10 = inputStream.read();
        int i11 = inputStream.read();
        int i12 = inputStream.read();
        int i13 = inputStream.read();
        int i14 = inputStream.read();
        int i15 = inputStream.read();
        int i16 = inputStream.read();
        int i17 = inputStream.read();
        int i18 = inputStream.read();
        inputStream.reset();
        if (i3 == 202 && i4 == 254 && i5 == 186 && i6 == 190) {
            return "application/java-vm";
        }
        if (i3 == 172 && i4 == 237) {
            return "application/x-java-serialized-object";
        }
        if (i3 == 60) {
            if (i4 == 33) {
                return "text/html";
            }
            if (i4 == 104) {
                if (i5 == 116 && i6 == 109 && i7 == 108) {
                    return "text/html";
                }
                if (i5 == 101 && i6 == 97 && i7 == 100) {
                    return "text/html";
                }
            }
            if (i4 == 98 && i5 == 111 && i6 == 100 && i7 == 121) {
                return "text/html";
            }
            if (i4 == 72) {
                if (i5 == 84 && i6 == 77 && i7 == 76) {
                    return "text/html";
                }
                if (i5 == 69 && i6 == 65 && i7 == 68) {
                    return "text/html";
                }
            }
            if (i4 == 66 && i5 == 79 && i6 == 68 && i7 == 89) {
                return "text/html";
            }
            if (i4 == 63 && i5 == 120 && i6 == 109 && i7 == 108 && i8 == 32) {
                return "application/xml";
            }
        }
        if (i3 == 239 && i4 == 187 && i5 == 191 && i6 == 60 && i7 == 63 && i8 == 120) {
            return "application/xml";
        }
        if (i3 == 254 && i4 == 255 && i5 == 0 && i6 == 60 && i7 == 0 && i8 == 63 && i9 == 0 && i10 == 120) {
            return "application/xml";
        }
        if (i3 == 255 && i4 == 254 && i5 == 60 && i6 == 0 && i7 == 63 && i8 == 0 && i9 == 120 && i10 == 0) {
            return "application/xml";
        }
        if (i3 == 0 && i4 == 0 && i5 == 254 && i6 == 255 && i7 == 0 && i8 == 0 && i9 == 0 && i10 == 60 && i11 == 0 && i12 == 0 && i13 == 0 && i14 == 63 && i15 == 0 && i16 == 0 && i17 == 0) {
            i = i18;
            if (i == 120) {
                return "application/xml";
            }
        } else {
            i = i18;
        }
        if (i3 == 255 && i4 == 254 && i5 == 0 && i6 == 0 && i7 == 60 && i8 == 0 && i9 == 0 && i10 == 0 && i11 == 63 && i12 == 0 && i13 == 0 && i14 == 0 && i15 == 120 && i16 == 0 && i17 == 0 && i == 0) {
            return "application/xml";
        }
        if (i3 == 71 && i4 == 73 && i5 == 70 && i6 == 56) {
            return "image/gif";
        }
        if (i3 == 35 && i4 == 100 && i5 == 101 && i6 == 102) {
            return "image/x-bitmap";
        }
        if (i3 == 33 && i4 == 32 && i5 == 88 && i6 == 80 && i7 == 77 && i8 == 50) {
            return "image/x-pixmap";
        }
        if (i3 == 137 && i4 == 80 && i5 == 78 && i6 == 71 && i7 == 13 && i8 == 10 && i9 == 26 && i10 == 10) {
            return "image/png";
        }
        if (i3 == 255 && i4 == 216 && i5 == 255) {
            if (i6 == 224 || i6 == 238) {
                return "image/jpeg";
            }
            if (i6 == 225 && i9 == 69 && i10 == 120 && i11 == 105 && i12 == 102 && i13 == 0) {
                return "image/jpeg";
            }
        }
        if (i3 == 208 && i4 == 207 && i5 == 17 && i6 == 224 && i7 == 161 && i8 == 177 && i9 == 26 && i10 == 225 && checkfpx(inputStream)) {
            return "image/vnd.fpx";
        }
        if (i3 == 46 && i4 == 115 && i5 == 110) {
            i2 = 100;
            if (i6 == 100) {
                return "audio/basic";
            }
        } else {
            i2 = 100;
        }
        if (i3 == i2 && i4 == 110 && i5 == 115 && i6 == 46) {
            return "audio/basic";
        }
        if (i3 == 82 && i4 == 73 && i5 == 70 && i6 == 70) {
            return "audio/x-wav";
        }
        return null;
    }

    private static boolean checkfpx(InputStream inputStream) throws IOException {
        int i;
        int i2;
        inputStream.mark(256);
        long jSkipForward = skipForward(inputStream, 28L);
        if (jSkipForward < 28) {
            inputStream.reset();
            return false;
        }
        int[] iArr = new int[16];
        if (readBytes(iArr, 2, inputStream) < 0) {
            inputStream.reset();
            return false;
        }
        int i3 = iArr[0];
        long j = jSkipForward + 2;
        if (readBytes(iArr, 2, inputStream) < 0) {
            inputStream.reset();
            return false;
        }
        if (i3 == 254) {
            i = iArr[0] + (iArr[1] << 8);
        } else {
            i = (iArr[0] << 8) + iArr[1];
        }
        long j2 = 48 - (j + 2);
        if (skipForward(inputStream, j2) >= j2) {
            if (readBytes(iArr, 4, inputStream) >= 0) {
                if (i3 == 254) {
                    i2 = iArr[0] + (iArr[1] << 8) + (iArr[2] << 16) + (iArr[3] << 24);
                } else {
                    i2 = (iArr[0] << 24) + (iArr[1] << 16) + (iArr[2] << 8) + iArr[3];
                }
                inputStream.reset();
                long j3 = 512 + (((long) (1 << i)) * ((long) i2)) + 80;
                if (j3 < 0) {
                    return false;
                }
                inputStream.mark(((int) j3) + 48);
                if (skipForward(inputStream, j3) < j3) {
                    inputStream.reset();
                    return false;
                }
                if (readBytes(iArr, 16, inputStream) >= 0) {
                    if (i3 == 254 && iArr[0] == 0 && iArr[2] == 97 && iArr[3] == 86 && iArr[4] == 84 && iArr[5] == 193 && iArr[6] == 206 && iArr[7] == 17 && iArr[8] == 133 && iArr[9] == 83 && iArr[10] == 0 && iArr[11] == 170 && iArr[12] == 0 && iArr[13] == 161 && iArr[14] == 249 && iArr[15] == 91) {
                        inputStream.reset();
                        return true;
                    }
                    if (iArr[3] == 0 && iArr[1] == 97 && iArr[0] == 86 && iArr[5] == 84 && iArr[4] == 193 && iArr[7] == 206 && iArr[6] == 17 && iArr[8] == 133 && iArr[9] == 83 && iArr[10] == 0 && iArr[11] == 170 && iArr[12] == 0 && iArr[13] == 161 && iArr[14] == 249 && iArr[15] == 91) {
                        inputStream.reset();
                        return true;
                    }
                    inputStream.reset();
                    return false;
                }
                inputStream.reset();
                return false;
            }
            inputStream.reset();
            return false;
        }
        inputStream.reset();
        return false;
    }

    private static int readBytes(int[] iArr, int i, InputStream inputStream) throws IOException {
        byte[] bArr = new byte[i];
        if (inputStream.read(bArr, 0, i) < i) {
            return -1;
        }
        for (int i2 = 0; i2 < i; i2++) {
            iArr[i2] = bArr[i2] & Character.DIRECTIONALITY_UNDEFINED;
        }
        return 0;
    }

    private static long skipForward(InputStream inputStream, long j) throws IOException {
        long j2 = 0;
        while (j2 != j) {
            long jSkip = inputStream.skip(j - j2);
            if (jSkip <= 0) {
                if (inputStream.read() == -1) {
                    return j2;
                }
                j2++;
            }
            j2 += jSkip;
        }
        return j2;
    }
}
