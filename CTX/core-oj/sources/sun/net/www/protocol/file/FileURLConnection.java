package sun.net.www.protocol.file;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import sun.net.ProgressMonitor;
import sun.net.ProgressSource;
import sun.net.www.MessageHeader;
import sun.net.www.MeteredStream;
import sun.net.www.ParseUtil;
import sun.net.www.URLConnection;

public class FileURLConnection extends URLConnection {
    String contentType;
    boolean exists;
    File file;
    String filename;
    List<String> files;
    private boolean initializedHeaders;
    InputStream is;
    boolean isDirectory;
    long lastModified;
    long length;
    Permission permission;
    static String CONTENT_LENGTH = "content-length";
    static String CONTENT_TYPE = "content-type";
    static String TEXT_PLAIN = "text/plain";
    static String LAST_MODIFIED = "last-modified";

    protected FileURLConnection(URL url, File file) {
        super(url);
        this.isDirectory = false;
        this.exists = false;
        this.length = -1L;
        this.lastModified = 0L;
        this.initializedHeaders = false;
        this.file = file;
    }

    @Override
    public void connect() throws IOException {
        if (!this.connected) {
            try {
                this.filename = this.file.toString();
                this.isDirectory = this.file.isDirectory();
                if (this.isDirectory) {
                    String[] list = this.file.list();
                    if (list == null) {
                        throw new FileNotFoundException(this.filename + " exists, but is not accessible");
                    }
                    this.files = Arrays.asList(list);
                } else {
                    this.is = new BufferedInputStream(new FileInputStream(this.filename));
                    if (ProgressMonitor.getDefault().shouldMeterInput(this.url, "GET")) {
                        this.is = new MeteredStream(this.is, new ProgressSource(this.url, "GET", this.file.length()), this.file.length());
                    }
                }
                this.connected = true;
            } catch (IOException e) {
                throw e;
            }
        }
    }

    private void initializeHeaders() {
        try {
            connect();
            this.exists = this.file.exists();
        } catch (IOException e) {
        }
        if (!this.initializedHeaders || !this.exists) {
            this.length = this.file.length();
            this.lastModified = this.file.lastModified();
            if (!this.isDirectory) {
                this.contentType = java.net.URLConnection.getFileNameMap().getContentTypeFor(this.filename);
                if (this.contentType != null) {
                    this.properties.add(CONTENT_TYPE, this.contentType);
                }
                this.properties.add(CONTENT_LENGTH, String.valueOf(this.length));
                if (this.lastModified != 0) {
                    Date date = new Date(this.lastModified);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    this.properties.add(LAST_MODIFIED, simpleDateFormat.format(date));
                }
            } else {
                this.properties.add(CONTENT_TYPE, TEXT_PLAIN);
            }
            this.initializedHeaders = true;
        }
    }

    @Override
    public String getHeaderField(String str) {
        initializeHeaders();
        return super.getHeaderField(str);
    }

    @Override
    public String getHeaderField(int i) {
        initializeHeaders();
        return super.getHeaderField(i);
    }

    @Override
    public int getContentLength() {
        initializeHeaders();
        if (this.length > 2147483647L) {
            return -1;
        }
        return (int) this.length;
    }

    @Override
    public long getContentLengthLong() {
        initializeHeaders();
        return this.length;
    }

    @Override
    public String getHeaderFieldKey(int i) {
        initializeHeaders();
        return super.getHeaderFieldKey(i);
    }

    @Override
    public MessageHeader getProperties() {
        initializeHeaders();
        return super.getProperties();
    }

    @Override
    public long getLastModified() {
        initializeHeaders();
        return this.lastModified;
    }

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        connect();
        if (this.is == null) {
            if (this.isDirectory) {
                java.net.URLConnection.getFileNameMap();
                StringBuffer stringBuffer = new StringBuffer();
                if (this.files == null) {
                    throw new FileNotFoundException(this.filename);
                }
                Collections.sort(this.files, Collator.getInstance());
                for (int i = 0; i < this.files.size(); i++) {
                    stringBuffer.append(this.files.get(i));
                    stringBuffer.append("\n");
                }
                this.is = new ByteArrayInputStream(stringBuffer.toString().getBytes());
            } else {
                throw new FileNotFoundException(this.filename);
            }
        }
        return this.is;
    }

    @Override
    public Permission getPermission() throws IOException {
        if (this.permission == null) {
            String strDecode = ParseUtil.decode(this.url.getPath());
            if (File.separatorChar == '/') {
                this.permission = new FilePermission(strDecode, "read");
            } else {
                this.permission = new FilePermission(strDecode.replace('/', File.separatorChar), "read");
            }
        }
        return this.permission;
    }
}
