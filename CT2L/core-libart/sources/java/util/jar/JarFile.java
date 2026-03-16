package java.util.jar;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarVerifier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import libcore.io.Streams;

public class JarFile extends ZipFile {
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    static final String META_DIR = "META-INF/";
    private boolean closed;
    private Manifest manifest;
    private byte[] manifestBytes;
    JarVerifier verifier;

    static final class JarFileInputStream extends FilterInputStream {
        private long count;
        private boolean done;
        private final JarVerifier.VerifierEntry entry;

        JarFileInputStream(InputStream is, long size, JarVerifier.VerifierEntry e) {
            super(is);
            this.done = false;
            this.entry = e;
            this.count = size;
        }

        @Override
        public int read() throws IOException {
            if (this.done) {
                return -1;
            }
            if (this.count > 0) {
                int r = super.read();
                if (r != -1) {
                    this.entry.write(r);
                    this.count--;
                } else {
                    this.count = 0L;
                }
                if (this.count == 0) {
                    this.done = true;
                    this.entry.verify();
                    return r;
                }
                return r;
            }
            this.done = true;
            this.entry.verify();
            return -1;
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            if (this.done) {
                return -1;
            }
            if (this.count > 0) {
                int r = super.read(buffer, byteOffset, byteCount);
                if (r != -1) {
                    int size = r;
                    if (this.count < size) {
                        size = (int) this.count;
                    }
                    this.entry.write(buffer, byteOffset, size);
                    this.count -= (long) size;
                } else {
                    this.count = 0L;
                }
                if (this.count == 0) {
                    this.done = true;
                    this.entry.verify();
                    return r;
                }
                return r;
            }
            this.done = true;
            this.entry.verify();
            return -1;
        }

        @Override
        public int available() throws IOException {
            if (this.done) {
                return 0;
            }
            return super.available();
        }

        @Override
        public long skip(long byteCount) throws IOException {
            return Streams.skipByReading(this, byteCount);
        }
    }

    static final class JarFileEnumerator implements Enumeration<JarEntry> {
        final JarFile jf;
        final Enumeration<? extends ZipEntry> ze;

        JarFileEnumerator(Enumeration<? extends ZipEntry> zenum, JarFile jf) {
            this.ze = zenum;
            this.jf = jf;
        }

        @Override
        public boolean hasMoreElements() {
            return this.ze.hasMoreElements();
        }

        @Override
        public JarEntry nextElement() {
            return new JarEntry(this.ze.nextElement(), this.jf);
        }
    }

    public JarFile(File file) throws IOException {
        this(file, true);
    }

    public JarFile(File file, boolean verify) throws IOException {
        this(file, verify, 1);
    }

    public JarFile(File file, boolean verify, int mode) throws IOException {
        super(file, mode);
        this.closed = false;
        HashMap<String, byte[]> metaEntries = readMetaEntries(this, verify);
        if (verify && metaEntries.containsKey(MANIFEST_NAME) && metaEntries.size() > 1) {
            this.manifest = new Manifest(metaEntries.get(MANIFEST_NAME), true);
            this.verifier = new JarVerifier(getName(), this.manifest, metaEntries);
        } else {
            this.verifier = null;
            this.manifestBytes = metaEntries.get(MANIFEST_NAME);
        }
    }

    public JarFile(String filename) throws IOException {
        this(filename, true);
    }

    public JarFile(String filename, boolean verify) throws IOException {
        this(new File(filename), verify, 1);
    }

    @Override
    public Enumeration<JarEntry> entries() {
        return new JarFileEnumerator(super.entries(), this);
    }

    public JarEntry getJarEntry(String name) {
        return (JarEntry) getEntry(name);
    }

    public Manifest getManifest() throws IOException {
        if (this.closed) {
            throw new IllegalStateException("JarFile has been closed");
        }
        if (this.manifest != null) {
            return this.manifest;
        }
        if (this.manifestBytes == null) {
            return null;
        }
        this.manifest = new Manifest(this.manifestBytes, false);
        this.manifestBytes = null;
        return this.manifest;
    }

    static HashMap<String, byte[]> readMetaEntries(ZipFile zipFile, boolean verificationRequired) throws IOException {
        List<ZipEntry> metaEntries = getMetaEntries(zipFile);
        HashMap<String, byte[]> metaEntriesMap = new HashMap<>();
        for (ZipEntry entry : metaEntries) {
            String entryName = entry.getName();
            if (entryName.equalsIgnoreCase(MANIFEST_NAME) && !metaEntriesMap.containsKey(MANIFEST_NAME)) {
                metaEntriesMap.put(MANIFEST_NAME, Streams.readFully(zipFile.getInputStream(entry)));
                if (!verificationRequired) {
                    break;
                }
            } else if (verificationRequired && (endsWithIgnoreCase(entryName, ".SF") || endsWithIgnoreCase(entryName, ".DSA") || endsWithIgnoreCase(entryName, ".RSA") || endsWithIgnoreCase(entryName, ".EC"))) {
                InputStream is = zipFile.getInputStream(entry);
                metaEntriesMap.put(entryName.toUpperCase(Locale.US), Streams.readFully(is));
            }
        }
        return metaEntriesMap;
    }

    private static boolean endsWithIgnoreCase(String s, String suffix) {
        return s.regionMatches(true, s.length() - suffix.length(), suffix, 0, suffix.length());
    }

    @Override
    public InputStream getInputStream(ZipEntry ze) throws IOException {
        JarVerifier.VerifierEntry entry;
        if (this.manifestBytes != null) {
            getManifest();
        }
        if (this.verifier != null && this.verifier.readCertificates()) {
            this.verifier.removeMetaEntries();
            this.manifest.removeChunks();
            if (!this.verifier.isSignedJar()) {
                this.verifier = null;
            }
        }
        InputStream in = super.getInputStream(ze);
        if (in == null) {
            return null;
        }
        return (this.verifier == null || ze.getSize() == -1 || (entry = this.verifier.initEntry(ze.getName())) == null) ? in : new JarFileInputStream(in, ze.getSize(), entry);
    }

    @Override
    public ZipEntry getEntry(String name) {
        ZipEntry ze = super.getEntry(name);
        return ze == null ? ze : new JarEntry(ze, this);
    }

    private static List<ZipEntry> getMetaEntries(ZipFile zipFile) {
        List<ZipEntry> list = new ArrayList<>(8);
        Enumeration<? extends ZipEntry> allEntries = zipFile.entries();
        while (allEntries.hasMoreElements()) {
            ZipEntry ze = allEntries.nextElement();
            if (ze.getName().startsWith(META_DIR) && ze.getName().length() > META_DIR.length()) {
                list.add(ze);
            }
        }
        return list;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.closed = true;
    }
}
