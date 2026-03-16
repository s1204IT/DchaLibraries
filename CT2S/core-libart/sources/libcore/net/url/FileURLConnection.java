package libcore.net.url;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import libcore.net.UriCodec;

public class FileURLConnection extends URLConnection {
    private static final int CONTENT_LENGTH_VALUE_IDX = 3;
    private static final int CONTENT_TYPE_VALUE_IDX = 1;
    private static final Comparator<String> HEADER_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            if (a == b) {
                return 0;
            }
            if (a == null) {
                return -1;
            }
            if (b == null) {
                return 1;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        }
    };
    private static final int LAST_MODIFIED_VALUE_IDX = 5;
    private String filename;
    private Map<String, List<String>> headerFields;
    private final String[] headerKeysAndValues;
    private InputStream is;
    private boolean isDir;
    private long lastModified;
    private long length;
    private FilePermission permission;

    public FileURLConnection(URL url) {
        super(url);
        this.length = -1L;
        this.lastModified = -1L;
        this.filename = url.getFile();
        if (this.filename == null) {
            this.filename = "";
        }
        this.filename = UriCodec.decode(this.filename);
        this.headerKeysAndValues = new String[]{"content-type", null, "content-length", null, "last-modified", null};
    }

    @Override
    public void connect() throws IOException {
        File f = new File(this.filename);
        IOException error = null;
        if (f.isDirectory()) {
            this.isDir = true;
            this.is = getDirectoryListing(f);
            this.lastModified = f.lastModified();
            this.headerKeysAndValues[1] = "text/html";
        } else {
            try {
                this.is = new BufferedInputStream(new FileInputStream(f));
            } catch (IOException ioe) {
                error = ioe;
            }
            if (error == null) {
                this.length = f.length();
                this.lastModified = f.lastModified();
                this.headerKeysAndValues[1] = getContentTypeForPlainFiles();
            } else {
                this.headerKeysAndValues[1] = "content/unknown";
            }
        }
        this.headerKeysAndValues[3] = String.valueOf(this.length);
        this.headerKeysAndValues[5] = String.valueOf(this.lastModified);
        this.connected = true;
        if (error != null) {
            throw error;
        }
    }

    @Override
    public String getHeaderField(String key) {
        if (!this.connected) {
            try {
                connect();
            } catch (IOException e) {
                return null;
            }
        }
        for (int i = 0; i < this.headerKeysAndValues.length; i += 2) {
            if (this.headerKeysAndValues[i].equalsIgnoreCase(key)) {
                return this.headerKeysAndValues[i + 1];
            }
        }
        return null;
    }

    @Override
    public String getHeaderFieldKey(int position) {
        if (!this.connected) {
            try {
                connect();
            } catch (IOException e) {
                return null;
            }
        }
        if (position < 0 || position > this.headerKeysAndValues.length / 2) {
            return null;
        }
        return this.headerKeysAndValues[position * 2];
    }

    @Override
    public String getHeaderField(int position) {
        if (!this.connected) {
            try {
                connect();
            } catch (IOException e) {
                return null;
            }
        }
        if (position < 0 || position > this.headerKeysAndValues.length / 2) {
            return null;
        }
        return this.headerKeysAndValues[(position * 2) + 1];
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        if (this.headerFields == null) {
            TreeMap<String, List<String>> headerFieldsMap = new TreeMap<>(HEADER_COMPARATOR);
            for (int i = 0; i < this.headerKeysAndValues.length; i += 2) {
                headerFieldsMap.put(this.headerKeysAndValues[i], Collections.singletonList(this.headerKeysAndValues[i + 1]));
            }
            this.headerFields = Collections.unmodifiableMap(headerFieldsMap);
        }
        return this.headerFields;
    }

    @Override
    public int getContentLength() {
        long length = getContentLengthLong();
        if (length <= 2147483647L) {
            return (int) length;
        }
        return -1;
    }

    private long getContentLengthLong() {
        try {
            if (!this.connected) {
                connect();
            }
        } catch (IOException e) {
        }
        return this.length;
    }

    @Override
    public String getContentType() {
        return getHeaderField(0);
    }

    private String getContentTypeForPlainFiles() {
        String result = guessContentTypeFromName(this.url.getFile());
        if (result != null) {
            return result;
        }
        try {
            result = guessContentTypeFromStream(this.is);
        } catch (IOException e) {
        }
        return result != null ? result : "content/unknown";
    }

    private InputStream getDirectoryListing(File f) {
        String[] fileList = f.list();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        out.print("<title>Directory Listing</title>\n");
        out.print("<base href=\"file:");
        out.print(f.getPath().replace('\\', '/') + "/\"><h1>" + f.getPath() + "</h1>\n<hr>\n");
        for (String str : fileList) {
            out.print(str + "<br>\n");
        }
        out.close();
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!this.connected) {
            connect();
        }
        return this.is;
    }

    @Override
    public Permission getPermission() throws IOException {
        if (this.permission == null) {
            String path = this.filename;
            if (File.separatorChar != '/') {
                path = path.replace('/', File.separatorChar);
            }
            this.permission = new FilePermission(path, "read");
        }
        return this.permission;
    }
}
