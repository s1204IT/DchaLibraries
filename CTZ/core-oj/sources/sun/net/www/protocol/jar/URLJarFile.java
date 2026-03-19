package sun.net.www.protocol.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import sun.net.www.ParseUtil;

public class URLJarFile extends JarFile {
    private static int BUF_SIZE = 2048;
    private URLJarFileCloseController closeController;
    private Attributes superAttr;
    private Map<String, Attributes> superEntries;
    private Manifest superMan;

    public interface URLJarFileCloseController {
        void close(JarFile jarFile);
    }

    static JarFile getJarFile(URL url) throws IOException {
        return getJarFile(url, null);
    }

    static JarFile getJarFile(URL url, URLJarFileCloseController uRLJarFileCloseController) throws IOException {
        if (isFileURL(url)) {
            return new URLJarFile(url, uRLJarFileCloseController);
        }
        return retrieve(url, uRLJarFileCloseController);
    }

    public URLJarFile(File file) throws IOException {
        this(file, (URLJarFileCloseController) null);
    }

    public URLJarFile(File file, URLJarFileCloseController uRLJarFileCloseController) throws IOException {
        super(file, true, 5);
        this.closeController = null;
        this.closeController = uRLJarFileCloseController;
    }

    private URLJarFile(URL url, URLJarFileCloseController uRLJarFileCloseController) throws IOException {
        super(ParseUtil.decode(url.getFile()));
        this.closeController = null;
        this.closeController = uRLJarFileCloseController;
    }

    private static boolean isFileURL(URL url) {
        if (url.getProtocol().equalsIgnoreCase("file")) {
            String host = url.getHost();
            if (host == null || host.equals("") || host.equals("~") || host.equalsIgnoreCase("localhost")) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    protected void finalize() throws IOException {
        close();
    }

    @Override
    public ZipEntry getEntry(String str) {
        ZipEntry entry = super.getEntry(str);
        if (entry != null) {
            if (entry instanceof JarEntry) {
                return new URLJarFileEntry((JarEntry) entry);
            }
            throw new InternalError(((Object) super.getClass()) + " returned unexpected entry type " + ((Object) entry.getClass()));
        }
        return null;
    }

    @Override
    public Manifest getManifest() throws IOException {
        if (!isSuperMan()) {
            return null;
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putAll((Map) this.superAttr.clone());
        if (this.superEntries != null) {
            Map<String, Attributes> entries = manifest.getEntries();
            for (String str : this.superEntries.keySet()) {
                entries.put(str, (Attributes) this.superEntries.get(str).clone());
            }
        }
        return manifest;
    }

    @Override
    public void close() throws IOException {
        if (this.closeController != null) {
            this.closeController.close(this);
        }
        super.close();
    }

    private synchronized boolean isSuperMan() throws IOException {
        if (this.superMan == null) {
            this.superMan = super.getManifest();
        }
        if (this.superMan != null) {
            this.superAttr = this.superMan.getMainAttributes();
            this.superEntries = this.superMan.getEntries();
            return true;
        }
        return false;
    }

    private static JarFile retrieve(URL url) throws IOException {
        return retrieve(url, null);
    }

    private static JarFile retrieve(URL url, final URLJarFileCloseController uRLJarFileCloseController) throws IOException {
        try {
            final InputStream inputStream = url.openConnection().getInputStream();
            Throwable th = null;
            try {
                try {
                    JarFile jarFile = (JarFile) AccessController.doPrivileged(new PrivilegedExceptionAction<JarFile>() {
                        @Override
                        public JarFile run() throws IOException {
                            Path pathCreateTempFile = Files.createTempFile("jar_cache", null, new FileAttribute[0]);
                            try {
                                Files.copy(inputStream, pathCreateTempFile, StandardCopyOption.REPLACE_EXISTING);
                                URLJarFile uRLJarFile = new URLJarFile(pathCreateTempFile.toFile(), uRLJarFileCloseController);
                                pathCreateTempFile.toFile().deleteOnExit();
                                return uRLJarFile;
                            } catch (Throwable th2) {
                                try {
                                    Files.delete(pathCreateTempFile);
                                } catch (IOException e) {
                                    th2.addSuppressed(e);
                                }
                                throw th2;
                            }
                        }
                    });
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    return jarFile;
                } finally {
                }
            } finally {
            }
        } catch (PrivilegedActionException e) {
            throw ((IOException) e.getException());
        }
    }

    private class URLJarFileEntry extends JarEntry {
        private JarEntry je;

        URLJarFileEntry(JarEntry jarEntry) {
            super(jarEntry);
            this.je = jarEntry;
        }

        @Override
        public Attributes getAttributes() throws IOException {
            Map map;
            Attributes attributes;
            if (URLJarFile.this.isSuperMan() && (map = URLJarFile.this.superEntries) != null && (attributes = (Attributes) map.get(getName())) != null) {
                return (Attributes) attributes.clone();
            }
            return null;
        }

        @Override
        public Certificate[] getCertificates() {
            Certificate[] certificates = this.je.getCertificates();
            if (certificates == null) {
                return null;
            }
            return (Certificate[]) certificates.clone();
        }

        @Override
        public CodeSigner[] getCodeSigners() {
            CodeSigner[] codeSigners = this.je.getCodeSigners();
            if (codeSigners == null) {
                return null;
            }
            return (CodeSigner[]) codeSigners.clone();
        }
    }
}
