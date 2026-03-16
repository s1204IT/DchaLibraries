package java.util.jar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.jar.JarVerifier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import libcore.io.Streams;

public class JarInputStream extends ZipInputStream {
    private JarEntry currentJarEntry;
    private boolean isMeta;
    private Manifest manifest;
    private JarEntry pendingJarEntry;
    private OutputStream verStream;
    private boolean verified;
    private JarVerifier verifier;

    public JarInputStream(InputStream stream, boolean verify) throws Exception {
        super(stream);
        this.verified = false;
        this.verifier = null;
        this.pendingJarEntry = null;
        this.currentJarEntry = null;
        if (getNextJarEntry() != null) {
            if (this.currentJarEntry.getName().equalsIgnoreCase("META-INF/")) {
                closeEntry();
                getNextJarEntry();
            }
            if (this.currentJarEntry.getName().equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
                byte[] manifestBytes = Streams.readFullyNoClose(this);
                this.manifest = new Manifest(manifestBytes, verify);
                closeEntry();
                if (verify) {
                    HashMap<String, byte[]> metaEntries = new HashMap<>();
                    metaEntries.put(JarFile.MANIFEST_NAME, manifestBytes);
                    this.verifier = new JarVerifier("JarInputStream", this.manifest, metaEntries);
                }
            }
            this.pendingJarEntry = this.currentJarEntry;
            this.currentJarEntry = null;
        }
    }

    public JarInputStream(InputStream stream) throws IOException {
        this(stream, true);
    }

    public Manifest getManifest() {
        return this.manifest;
    }

    public JarEntry getNextJarEntry() throws IOException {
        return (JarEntry) getNextEntry();
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (this.currentJarEntry == null) {
            return -1;
        }
        int r = super.read(buffer, byteOffset, byteCount);
        if (this.verifier != null && this.verStream != null && !this.verified) {
            if (r == -1) {
                this.verified = true;
                if (this.isMeta) {
                    this.verifier.addMetaEntry(this.currentJarEntry.getName(), ((ByteArrayOutputStream) this.verStream).toByteArray());
                    try {
                        this.verifier.readCertificates();
                        return r;
                    } catch (SecurityException e) {
                        this.verifier = null;
                        throw e;
                    }
                }
                ((JarVerifier.VerifierEntry) this.verStream).verify();
                return r;
            }
            this.verStream.write(buffer, byteOffset, r);
            return r;
        }
        return r;
    }

    @Override
    public ZipEntry getNextEntry() throws IOException {
        if (this.pendingJarEntry != null) {
            JarEntry pending = this.pendingJarEntry;
            this.pendingJarEntry = null;
            this.currentJarEntry = pending;
            return pending;
        }
        this.currentJarEntry = (JarEntry) super.getNextEntry();
        if (this.currentJarEntry == null) {
            return null;
        }
        if (this.verifier != null) {
            this.isMeta = this.currentJarEntry.getName().toUpperCase(Locale.US).startsWith("META-INF/");
            if (this.isMeta) {
                int entrySize = (int) this.currentJarEntry.getSize();
                if (entrySize <= 0) {
                    entrySize = 8192;
                }
                this.verStream = new ByteArrayOutputStream(entrySize);
            } else {
                this.verStream = this.verifier.initEntry(this.currentJarEntry.getName());
            }
        }
        this.verified = false;
        return this.currentJarEntry;
    }

    @Override
    public void closeEntry() throws Exception {
        if (this.pendingJarEntry == null) {
            super.closeEntry();
            this.currentJarEntry = null;
        }
    }

    @Override
    protected ZipEntry createZipEntry(String name) {
        JarEntry entry = new JarEntry(name);
        if (this.manifest != null) {
            entry.setAttributes(this.manifest.getAttributes(name));
        }
        return entry;
    }
}
