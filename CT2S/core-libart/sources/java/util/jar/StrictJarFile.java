package java.util.jar;

import dalvik.system.CloseGuard;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.JarVerifier;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import libcore.io.IoUtils;
import libcore.io.Streams;

public final class StrictJarFile {
    private boolean closed;
    private final CloseGuard guard = CloseGuard.get();
    private final boolean isSigned;
    private final Manifest manifest;
    private final long nativeHandle;
    private final RandomAccessFile raf;
    private final JarVerifier verifier;

    private static native void nativeClose(long j);

    private static native ZipEntry nativeFindEntry(long j, String str);

    private static native ZipEntry nativeNextEntry(long j);

    private static native long nativeOpenJarFile(String str) throws IOException;

    private static native long nativeStartIteration(long j, String str);

    public StrictJarFile(String fileName) throws IOException {
        this.nativeHandle = nativeOpenJarFile(fileName);
        this.raf = new RandomAccessFile(fileName, "r");
        try {
            HashMap<String, byte[]> metaEntries = getMetaEntries();
            this.manifest = new Manifest(metaEntries.get(JarFile.MANIFEST_NAME), true);
            this.verifier = new JarVerifier(fileName, this.manifest, metaEntries);
            this.isSigned = this.verifier.readCertificates() && this.verifier.isSignedJar();
            this.guard.open("close");
        } catch (IOException ioe) {
            nativeClose(this.nativeHandle);
            throw ioe;
        }
    }

    public Manifest getManifest() {
        return this.manifest;
    }

    public Iterator<ZipEntry> iterator() throws IOException {
        return new EntryIterator(this.nativeHandle, "");
    }

    public ZipEntry findEntry(String name) {
        return nativeFindEntry(this.nativeHandle, name);
    }

    public Certificate[][] getCertificateChains(ZipEntry ze) {
        return this.isSigned ? this.verifier.getCertificateChains(ze.getName()) : (Certificate[][]) null;
    }

    @Deprecated
    public Certificate[] getCertificates(ZipEntry ze) {
        if (this.isSigned) {
            Certificate[][] certChains = this.verifier.getCertificateChains(ze.getName());
            int count = 0;
            for (Certificate[] certificateArr : certChains) {
                count += certificateArr.length;
            }
            Certificate[] certs = new Certificate[count];
            int i = 0;
            for (Certificate[] chain : certChains) {
                System.arraycopy(chain, 0, certs, i, chain.length);
                i += chain.length;
            }
            return certs;
        }
        return null;
    }

    public InputStream getInputStream(ZipEntry ze) {
        JarVerifier.VerifierEntry entry;
        InputStream is = getZipInputStream(ze);
        if (this.isSigned && (entry = this.verifier.initEntry(ze.getName())) != null) {
            return new JarFile.JarFileInputStream(is, ze.getSize(), entry);
        }
        return is;
    }

    public void close() throws IOException {
        if (!this.closed) {
            this.guard.close();
            nativeClose(this.nativeHandle);
            IoUtils.closeQuietly(this.raf);
            this.closed = true;
        }
    }

    private InputStream getZipInputStream(ZipEntry ze) {
        if (ze.getMethod() == 0) {
            return new ZipFile.RAFStream(this.raf, ze.getDataOffset(), ze.getDataOffset() + ze.getSize());
        }
        ZipFile.RAFStream wrapped = new ZipFile.RAFStream(this.raf, ze.getDataOffset(), ze.getDataOffset() + ze.getCompressedSize());
        int bufSize = Math.max(1024, (int) Math.min(ze.getSize(), 65535L));
        return new ZipFile.ZipInflaterInputStream(wrapped, new Inflater(true), bufSize, ze);
    }

    static final class EntryIterator implements Iterator<ZipEntry> {
        private final long iterationHandle;
        private ZipEntry nextEntry;

        EntryIterator(long nativeHandle, String prefix) throws IOException {
            this.iterationHandle = StrictJarFile.nativeStartIteration(nativeHandle, prefix);
        }

        @Override
        public ZipEntry next() {
            if (this.nextEntry == null) {
                ZipEntry ze = StrictJarFile.nativeNextEntry(this.iterationHandle);
                return ze;
            }
            ZipEntry ze2 = this.nextEntry;
            this.nextEntry = null;
            return ze2;
        }

        @Override
        public boolean hasNext() {
            if (this.nextEntry != null) {
                return true;
            }
            ZipEntry ze = StrictJarFile.nativeNextEntry(this.iterationHandle);
            if (ze == null) {
                return false;
            }
            this.nextEntry = ze;
            return true;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private HashMap<String, byte[]> getMetaEntries() throws IOException {
        HashMap<String, byte[]> metaEntries = new HashMap<>();
        Iterator<ZipEntry> entryIterator = new EntryIterator(this.nativeHandle, "META-INF/");
        while (entryIterator.hasNext()) {
            ZipEntry entry = entryIterator.next();
            metaEntries.put(entry.getName(), Streams.readFully(getInputStream(entry)));
        }
        return metaEntries;
    }
}
