package libcore.net.url;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.Permission;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import libcore.net.UriCodec;

public class JarURLConnectionImpl extends JarURLConnection {
    private static final HashMap<URL, JarFile> jarCache = new HashMap<>();
    private boolean closed;
    private JarEntry jarEntry;
    private JarFile jarFile;
    private URL jarFileURL;
    private InputStream jarInput;

    public JarURLConnectionImpl(URL url) throws IOException {
        super(url);
        this.jarFileURL = getJarFileURL();
        this.jarFileURLConnection = this.jarFileURL.openConnection();
    }

    @Override
    public void connect() throws Throwable {
        if (!this.connected) {
            findJarFile();
            findJarEntry();
            this.connected = true;
        }
    }

    @Override
    public JarFile getJarFile() throws Throwable {
        connect();
        return this.jarFile;
    }

    private void findJarFile() throws Throwable {
        if (getUseCaches()) {
            synchronized (jarCache) {
                this.jarFile = jarCache.get(this.jarFileURL);
            }
            if (this.jarFile == null) {
                JarFile jar = openJarFile();
                synchronized (jarCache) {
                    this.jarFile = jarCache.get(this.jarFileURL);
                    if (this.jarFile == null) {
                        jarCache.put(this.jarFileURL, jar);
                        this.jarFile = jar;
                    } else {
                        jar.close();
                    }
                }
            }
        } else {
            this.jarFile = openJarFile();
        }
        if (this.jarFile == null) {
            throw new IOException();
        }
    }

    private JarFile openJarFile() throws Throwable {
        File tempJar;
        FileOutputStream fos;
        if (this.jarFileURL.getProtocol().equals("file")) {
            String decodedFile = UriCodec.decode(this.jarFileURL.getFile());
            return new JarFile(new File(decodedFile), true, 1);
        }
        InputStream is = this.jarFileURL.openConnection().getInputStream();
        FileOutputStream fos2 = null;
        try {
            try {
                tempJar = File.createTempFile("hyjar_", ".tmp", null);
                tempJar.deleteOnExit();
                fos = new FileOutputStream(tempJar);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            byte[] buf = new byte[4096];
            while (true) {
                int nbytes = is.read(buf);
                if (nbytes <= -1) {
                    break;
                }
                fos.write(buf, 0, nbytes);
            }
            fos.close();
            JarFile jarFile = new JarFile(tempJar, true, 5);
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2) {
                    if (is == null) {
                        return null;
                    }
                    is.close();
                    return null;
                } catch (Throwable th3) {
                    th = th3;
                    if (is != null) {
                        is.close();
                    }
                    throw th;
                }
            }
            if (is != null) {
                is.close();
            }
            return jarFile;
        } catch (IOException e3) {
            fos2 = fos;
            if (fos2 != null) {
                try {
                    fos2.close();
                } catch (IOException e4) {
                    if (is == null) {
                        return null;
                    }
                    is.close();
                    return null;
                }
            }
            if (is == null) {
                return null;
            }
            is.close();
            return null;
        } catch (Throwable th4) {
            th = th4;
            fos2 = fos;
            if (fos2 != null) {
                try {
                    fos2.close();
                } catch (IOException e5) {
                    if (is == null) {
                        return null;
                    }
                    is.close();
                    return null;
                }
            }
            throw th;
        }
    }

    @Override
    public JarEntry getJarEntry() throws Throwable {
        connect();
        return this.jarEntry;
    }

    private void findJarEntry() throws IOException {
        if (getEntryName() != null) {
            this.jarEntry = this.jarFile.getJarEntry(getEntryName());
            if (this.jarEntry == null) {
                throw new FileNotFoundException(getEntryName());
            }
        }
    }

    @Override
    public InputStream getInputStream() throws Throwable {
        if (this.closed) {
            throw new IllegalStateException("JarURLConnection InputStream has been closed");
        }
        connect();
        if (this.jarInput != null) {
            return this.jarInput;
        }
        if (this.jarEntry == null) {
            throw new IOException("Jar entry not specified");
        }
        JarURLConnectionInputStream jarURLConnectionInputStream = new JarURLConnectionInputStream(this.jarFile.getInputStream(this.jarEntry), this.jarFile);
        this.jarInput = jarURLConnectionInputStream;
        return jarURLConnectionInputStream;
    }

    @Override
    public String getContentType() throws Throwable {
        if (this.url.getFile().endsWith("!/")) {
            return "x-java/jar";
        }
        String cType = null;
        String entryName = getEntryName();
        if (entryName != null) {
            cType = guessContentTypeFromName(entryName);
        } else {
            try {
                connect();
                cType = this.jarFileURLConnection.getContentType();
            } catch (IOException e) {
            }
        }
        if (cType == null) {
            return "content/unknown";
        }
        return cType;
    }

    @Override
    public int getContentLength() throws Throwable {
        int size;
        try {
            connect();
            if (this.jarEntry == null) {
                size = this.jarFileURLConnection.getContentLength();
            } else {
                size = (int) getJarEntry().getSize();
            }
            return size;
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public Object getContent() throws Throwable {
        connect();
        return this.jarEntry == null ? this.jarFile : super.getContent();
    }

    @Override
    public Permission getPermission() throws IOException {
        return this.jarFileURLConnection.getPermission();
    }

    @Override
    public boolean getUseCaches() {
        return this.jarFileURLConnection.getUseCaches();
    }

    @Override
    public void setUseCaches(boolean usecaches) {
        this.jarFileURLConnection.setUseCaches(usecaches);
    }

    @Override
    public boolean getDefaultUseCaches() {
        return this.jarFileURLConnection.getDefaultUseCaches();
    }

    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
        this.jarFileURLConnection.setDefaultUseCaches(defaultusecaches);
    }

    private class JarURLConnectionInputStream extends FilterInputStream {
        final JarFile jarFile;

        protected JarURLConnectionInputStream(InputStream in, JarFile file) {
            super(in);
            this.jarFile = file;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (!JarURLConnectionImpl.this.getUseCaches()) {
                JarURLConnectionImpl.this.closed = true;
                this.jarFile.close();
            }
        }
    }
}
