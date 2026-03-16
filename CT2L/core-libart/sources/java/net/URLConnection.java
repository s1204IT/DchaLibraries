package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AllPermission;
import java.security.Permission;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class URLConnection {
    private static ContentHandlerFactory contentHandlerFactory;
    private static boolean defaultAllowUserInteraction;
    private static FileNameMap fileNameMap;
    protected boolean connected;
    private String contentType;
    protected boolean doOutput;
    protected long ifModifiedSince;
    protected URL url;
    private static boolean defaultUseCaches = true;
    static Hashtable<String, Object> contentHandlers = new Hashtable<>();
    ContentHandler defaultHandler = new DefaultContentHandler();
    private long lastModified = -1;
    protected boolean useCaches = defaultUseCaches;
    protected boolean doInput = true;
    protected boolean allowUserInteraction = defaultAllowUserInteraction;
    private int readTimeout = 0;
    private int connectTimeout = 0;

    public abstract void connect() throws IOException;

    protected URLConnection(URL url) {
        this.url = url;
    }

    public boolean getAllowUserInteraction() {
        return this.allowUserInteraction;
    }

    public Object getContent() throws IOException {
        if (!this.connected) {
            connect();
        }
        String contentType = getContentType();
        this.contentType = contentType;
        if (contentType == null) {
            String strGuessContentTypeFromName = guessContentTypeFromName(this.url.getFile());
            this.contentType = strGuessContentTypeFromName;
            if (strGuessContentTypeFromName == null) {
                this.contentType = guessContentTypeFromStream(getInputStream());
            }
        }
        if (this.contentType != null) {
            return getContentHandler(this.contentType).getContent(this);
        }
        return null;
    }

    public Object getContent(Class[] types) throws IOException {
        if (!this.connected) {
            connect();
        }
        String contentType = getContentType();
        this.contentType = contentType;
        if (contentType == null) {
            String strGuessContentTypeFromName = guessContentTypeFromName(this.url.getFile());
            this.contentType = strGuessContentTypeFromName;
            if (strGuessContentTypeFromName == null) {
                this.contentType = guessContentTypeFromStream(getInputStream());
            }
        }
        if (this.contentType != null) {
            return getContentHandler(this.contentType).getContent(this, types);
        }
        return null;
    }

    public String getContentEncoding() {
        return getHeaderField("Content-Encoding");
    }

    private ContentHandler getContentHandler(String type) throws Throwable {
        String typeString = parseTypeString(type.replace('/', '.'));
        Object cHandler = contentHandlers.get(type);
        if (cHandler != null) {
            return (ContentHandler) cHandler;
        }
        if (contentHandlerFactory != null) {
            Object cHandler2 = contentHandlerFactory.createContentHandler(type);
            contentHandlers.put(type, cHandler2);
            return (ContentHandler) cHandler2;
        }
        String packageList = System.getProperty("java.content.handler.pkgs");
        if (packageList != null) {
            String[] arr$ = packageList.split("\\|");
            for (String packageName : arr$) {
                String className = packageName + "." + typeString;
                try {
                    Class<?> klass = Class.forName(className, true, ClassLoader.getSystemClassLoader());
                    cHandler = klass.newInstance();
                } catch (ClassNotFoundException e) {
                } catch (IllegalAccessException e2) {
                } catch (InstantiationException e3) {
                }
            }
        }
        if (cHandler == null) {
            try {
                String className2 = "org.apache.harmony.awt.www.content." + typeString;
                cHandler = Class.forName(className2).newInstance();
            } catch (ClassNotFoundException e4) {
            } catch (IllegalAccessException e5) {
            } catch (InstantiationException e6) {
            }
        }
        if (cHandler != null) {
            if (!(cHandler instanceof ContentHandler)) {
                throw new UnknownServiceException();
            }
            contentHandlers.put(type, cHandler);
            return (ContentHandler) cHandler;
        }
        return this.defaultHandler;
    }

    public int getContentLength() {
        return getHeaderFieldInt("Content-Length", -1);
    }

    public String getContentType() {
        return getHeaderField("Content-Type");
    }

    public long getDate() {
        return getHeaderFieldDate("Date", 0L);
    }

    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }

    @Deprecated
    public static String getDefaultRequestProperty(String field) {
        return null;
    }

    public boolean getDefaultUseCaches() {
        return defaultUseCaches;
    }

    public boolean getDoInput() {
        return this.doInput;
    }

    public boolean getDoOutput() {
        return this.doOutput;
    }

    public long getExpiration() {
        return getHeaderFieldDate("Expires", 0L);
    }

    public static FileNameMap getFileNameMap() {
        FileNameMap fileNameMap2;
        synchronized (URLConnection.class) {
            if (fileNameMap == null) {
                fileNameMap = new DefaultFileNameMap();
            }
            fileNameMap2 = fileNameMap;
        }
        return fileNameMap2;
    }

    public String getHeaderField(int pos) {
        return null;
    }

    public Map<String, List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }

    public Map<String, List<String>> getRequestProperties() {
        checkNotConnected();
        return Collections.emptyMap();
    }

    private void checkNotConnected() {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
    }

    public void addRequestProperty(String field, String newValue) {
        checkNotConnected();
        if (field == null) {
            throw new NullPointerException("field == null");
        }
    }

    public String getHeaderField(String key) {
        return null;
    }

    public long getHeaderFieldDate(String field, long defaultValue) {
        String date = getHeaderField(field);
        if (date != null) {
            try {
                return Date.parse(date);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public int getHeaderFieldInt(String field, int defaultValue) {
        try {
            int defaultValue2 = Integer.parseInt(getHeaderField(field));
            return defaultValue2;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getHeaderFieldKey(int posn) {
        return null;
    }

    public long getIfModifiedSince() {
        return this.ifModifiedSince;
    }

    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException("Does not support writing to the input stream");
    }

    public long getLastModified() {
        if (this.lastModified != -1) {
            return this.lastModified;
        }
        long headerFieldDate = getHeaderFieldDate("Last-Modified", 0L);
        this.lastModified = headerFieldDate;
        return headerFieldDate;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException("Does not support writing to the output stream");
    }

    public Permission getPermission() throws IOException {
        return new AllPermission();
    }

    public String getRequestProperty(String field) {
        checkNotConnected();
        return null;
    }

    public URL getURL() {
        return this.url;
    }

    public boolean getUseCaches() {
        return this.useCaches;
    }

    public static String guessContentTypeFromName(String url) {
        return getFileNameMap().getContentTypeFor(url);
    }

    public static String guessContentTypeFromStream(InputStream is) throws IOException {
        if (!is.markSupported()) {
            return null;
        }
        is.mark(64);
        byte[] bytes = new byte[64];
        int length = is.read(bytes);
        is.reset();
        if (length == -1) {
            return null;
        }
        String encoding = "US-ASCII";
        int start = 0;
        if (length > 1) {
            if (bytes[0] == -1 && bytes[1] == -2) {
                encoding = "UTF-16LE";
                start = 2;
                length -= length & 1;
            }
            if (bytes[0] == -2 && bytes[1] == -1) {
                encoding = "UTF-16BE";
                start = 2;
                length -= length & 1;
            }
            if (length > 2) {
                if (bytes[0] == -17 && bytes[1] == -69 && bytes[2] == -65) {
                    encoding = "UTF-8";
                    start = 3;
                }
                if (length > 3) {
                    if (bytes[0] == 0 && bytes[1] == 0 && bytes[2] == -2 && bytes[3] == -1) {
                        encoding = "UTF-32BE";
                        start = 4;
                        length -= length & 3;
                    }
                    if (bytes[0] == -1 && bytes[1] == -2 && bytes[2] == 0 && bytes[3] == 0) {
                        encoding = "UTF-32LE";
                        start = 4;
                        length -= length & 3;
                    }
                }
            }
        }
        String header = new String(bytes, start, length - start, encoding);
        if (header.startsWith("PK")) {
            return "application/zip";
        }
        if (header.startsWith("GI")) {
            return "image/gif";
        }
        String textHeader = header.trim().toUpperCase(Locale.US);
        if (textHeader.startsWith("<!DOCTYPE HTML") || textHeader.startsWith("<HTML") || textHeader.startsWith("<HEAD") || textHeader.startsWith("<BODY") || textHeader.startsWith("<HEAD")) {
            return "text/html";
        }
        if (textHeader.startsWith("<?XML")) {
            return "application/xml";
        }
        return null;
    }

    private String parseTypeString(String typeString) {
        StringBuilder result = new StringBuilder(typeString);
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c) && c != '.') {
                result.setCharAt(i, '_');
            }
        }
        return result.toString();
    }

    public void setAllowUserInteraction(boolean newValue) {
        checkNotConnected();
        this.allowUserInteraction = newValue;
    }

    public static synchronized void setContentHandlerFactory(ContentHandlerFactory contentFactory) {
        if (contentHandlerFactory != null) {
            throw new Error("Factory already set");
        }
        contentHandlerFactory = contentFactory;
    }

    public static void setDefaultAllowUserInteraction(boolean allows) {
        defaultAllowUserInteraction = allows;
    }

    @Deprecated
    public static void setDefaultRequestProperty(String field, String value) {
    }

    public void setDefaultUseCaches(boolean newValue) {
        defaultUseCaches = newValue;
    }

    public void setDoInput(boolean newValue) {
        checkNotConnected();
        this.doInput = newValue;
    }

    public void setDoOutput(boolean newValue) {
        checkNotConnected();
        this.doOutput = newValue;
    }

    public static void setFileNameMap(FileNameMap map) {
        synchronized (URLConnection.class) {
            fileNameMap = map;
        }
    }

    public void setIfModifiedSince(long newValue) {
        checkNotConnected();
        this.ifModifiedSince = newValue;
    }

    public void setRequestProperty(String field, String newValue) {
        checkNotConnected();
        if (field == null) {
            throw new NullPointerException("field == null");
        }
    }

    public void setUseCaches(boolean newValue) {
        checkNotConnected();
        this.useCaches = newValue;
    }

    public void setConnectTimeout(int timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis < 0");
        }
        this.connectTimeout = timeoutMillis;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setReadTimeout(int timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis < 0");
        }
        this.readTimeout = timeoutMillis;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public String toString() {
        return getClass().getName() + ":" + this.url.toString();
    }

    static class DefaultContentHandler extends ContentHandler {
        DefaultContentHandler() {
        }

        @Override
        public Object getContent(URLConnection u) throws IOException {
            return u.getInputStream();
        }
    }
}
