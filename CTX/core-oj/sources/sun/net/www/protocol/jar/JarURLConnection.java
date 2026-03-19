package sun.net.www.protocol.jar;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarURLConnection extends java.net.JarURLConnection {
    private static final boolean debug = false;
    private static final JarFileFactory factory = JarFileFactory.getInstance();
    private String contentType;
    private String entryName;
    private JarEntry jarEntry;
    private JarFile jarFile;
    private URL jarFileURL;
    private URLConnection jarFileURLConnection;
    private Permission permission;

    public JarURLConnection(URL url, Handler handler) throws IOException {
        super(url);
        this.jarFileURL = getJarFileURL();
        this.jarFileURLConnection = this.jarFileURL.openConnection();
        this.entryName = getEntryName();
    }

    @Override
    public JarFile getJarFile() throws IOException {
        connect();
        return this.jarFile;
    }

    @Override
    public JarEntry getJarEntry() throws IOException {
        connect();
        return this.jarEntry;
    }

    @Override
    public Permission getPermission() throws IOException {
        return this.jarFileURLConnection.getPermission();
    }

    class JarURLInputStream extends FilterInputStream {
        JarURLInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (!JarURLConnection.this.getUseCaches()) {
                    JarURLConnection.this.jarFile.close();
                }
            }
        }
    }

    @Override
    public void connect() throws IOException {
        if (!this.connected) {
            this.jarFile = factory.get(getJarFileURL(), getUseCaches());
            if (getUseCaches()) {
                boolean useCaches = this.jarFileURLConnection.getUseCaches();
                this.jarFileURLConnection = factory.getConnection(this.jarFile);
                this.jarFileURLConnection.setUseCaches(useCaches);
            }
            if (this.entryName != null) {
                this.jarEntry = (JarEntry) this.jarFile.getEntry(this.entryName);
                if (this.jarEntry == null) {
                    try {
                        if (!getUseCaches()) {
                            this.jarFile.close();
                        }
                    } catch (Exception e) {
                    }
                    throw new FileNotFoundException("JAR entry " + this.entryName + " not found in " + this.jarFile.getName());
                }
            }
            this.connected = true;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        if (this.entryName == null) {
            throw new IOException("no entry name specified");
        }
        if (this.jarEntry == null) {
            throw new FileNotFoundException("JAR entry " + this.entryName + " not found in " + this.jarFile.getName());
        }
        return new JarURLInputStream(this.jarFile.getInputStream(this.jarEntry));
    }

    @Override
    public int getContentLength() {
        long contentLengthLong = getContentLengthLong();
        if (contentLengthLong > 2147483647L) {
            return -1;
        }
        return (int) contentLengthLong;
    }

    @Override
    public long getContentLengthLong() {
        long size;
        try {
            connect();
            if (this.jarEntry == null) {
                size = this.jarFileURLConnection.getContentLengthLong();
            } else {
                size = getJarEntry().getSize();
            }
            return size;
        } catch (IOException e) {
            return -1L;
        }
    }

    @Override
    public Object getContent() throws IOException {
        connect();
        if (this.entryName == null) {
            return this.jarFile;
        }
        return super.getContent();
    }

    @Override
    public String getContentType() {
        if (this.contentType == null) {
            if (this.entryName == null) {
                this.contentType = "x-java/jar";
            } else {
                try {
                    connect();
                    InputStream inputStream = this.jarFile.getInputStream(this.jarEntry);
                    this.contentType = guessContentTypeFromStream(new BufferedInputStream(inputStream));
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            if (this.contentType == null) {
                this.contentType = guessContentTypeFromName(this.entryName);
            }
            if (this.contentType == null) {
                this.contentType = "content/unknown";
            }
        }
        return this.contentType;
    }

    @Override
    public String getHeaderField(String str) {
        return this.jarFileURLConnection.getHeaderField(str);
    }

    @Override
    public void setRequestProperty(String str, String str2) {
        this.jarFileURLConnection.setRequestProperty(str, str2);
    }

    @Override
    public String getRequestProperty(String str) {
        return this.jarFileURLConnection.getRequestProperty(str);
    }

    @Override
    public void addRequestProperty(String str, String str2) {
        this.jarFileURLConnection.addRequestProperty(str, str2);
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return this.jarFileURLConnection.getRequestProperties();
    }

    @Override
    public void setAllowUserInteraction(boolean z) {
        this.jarFileURLConnection.setAllowUserInteraction(z);
    }

    @Override
    public boolean getAllowUserInteraction() {
        return this.jarFileURLConnection.getAllowUserInteraction();
    }

    @Override
    public void setUseCaches(boolean z) {
        this.jarFileURLConnection.setUseCaches(z);
    }

    @Override
    public boolean getUseCaches() {
        return this.jarFileURLConnection.getUseCaches();
    }

    @Override
    public void setIfModifiedSince(long j) {
        this.jarFileURLConnection.setIfModifiedSince(j);
    }

    @Override
    public void setDefaultUseCaches(boolean z) {
        this.jarFileURLConnection.setDefaultUseCaches(z);
    }

    @Override
    public boolean getDefaultUseCaches() {
        return this.jarFileURLConnection.getDefaultUseCaches();
    }
}
