package java.util.jar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Spliterators;
import java.util.jar.JarVerifier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sun.misc.IOUtils;
import sun.security.util.ManifestEntryVerifier;
import sun.security.util.SignatureFileVerifier;

public class JarFile extends ZipFile {
    private static final char[] CLASSPATH_CHARS = {'c', 'l', 'a', 's', 's', '-', 'p', 'a', 't', 'h'};
    private static final int[] CLASSPATH_LASTOCC = new int[128];
    private static final int[] CLASSPATH_OPTOSFT = new int[10];
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    static final String META_DIR = "META-INF/";
    private volatile boolean hasCheckedSpecialAttributes;
    private boolean hasClassPathAttribute;
    private JarVerifier jv;
    private boolean jvInitialized;
    private JarEntry manEntry;
    private Manifest manifest;
    private boolean verify;

    private native String[] getMetaInfEntryNames();

    public JarFile(String str) throws IOException {
        this(new File(str), true, 1);
    }

    public JarFile(String str, boolean z) throws IOException {
        this(new File(str), z, 1);
    }

    public JarFile(File file) throws IOException {
        this(file, true, 1);
    }

    public JarFile(File file, boolean z) throws IOException {
        this(file, z, 1);
    }

    public JarFile(File file, boolean z, int i) throws IOException {
        super(file, i);
        this.verify = z;
    }

    public Manifest getManifest() throws IOException {
        return getManifestFromReference();
    }

    private synchronized Manifest getManifestFromReference() throws IOException {
        JarEntry manEntry;
        if (this.manifest == null && (manEntry = getManEntry()) != null) {
            if (this.verify) {
                byte[] bytes = getBytes(manEntry);
                this.manifest = new Manifest(new ByteArrayInputStream(bytes));
                if (!this.jvInitialized) {
                    this.jv = new JarVerifier(bytes);
                }
            } else {
                this.manifest = new Manifest(super.getInputStream(manEntry));
            }
        }
        return this.manifest;
    }

    public JarEntry getJarEntry(String str) {
        return (JarEntry) getEntry(str);
    }

    @Override
    public ZipEntry getEntry(String str) {
        ZipEntry entry = super.getEntry(str);
        if (entry != null) {
            return new JarFileEntry(entry);
        }
        return null;
    }

    private class JarEntryIterator implements Enumeration<JarEntry>, Iterator<JarEntry> {
        final Enumeration<? extends ZipEntry> e;

        private JarEntryIterator() {
            this.e = JarFile.super.entries();
        }

        @Override
        public boolean hasNext() {
            return this.e.hasMoreElements();
        }

        @Override
        public JarEntry next() {
            return JarFile.this.new JarFileEntry(this.e.nextElement());
        }

        @Override
        public boolean hasMoreElements() {
            return hasNext();
        }

        @Override
        public JarEntry nextElement() {
            return next();
        }
    }

    @Override
    public Enumeration<JarEntry> entries() {
        return new JarEntryIterator();
    }

    @Override
    public Stream<JarEntry> stream() {
        return StreamSupport.stream(Spliterators.spliterator(new JarEntryIterator(), size(), 1297), false);
    }

    private class JarFileEntry extends JarEntry {
        JarFileEntry(ZipEntry zipEntry) {
            super(zipEntry);
        }

        @Override
        public Attributes getAttributes() throws IOException {
            Manifest manifest = JarFile.this.getManifest();
            if (manifest != null) {
                return manifest.getAttributes(getName());
            }
            return null;
        }

        @Override
        public Certificate[] getCertificates() {
            try {
                JarFile.this.maybeInstantiateVerifier();
                if (this.certs == null && JarFile.this.jv != null) {
                    this.certs = JarFile.this.jv.getCerts(JarFile.this, this);
                }
                if (this.certs == null) {
                    return null;
                }
                return (Certificate[]) this.certs.clone();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public CodeSigner[] getCodeSigners() {
            try {
                JarFile.this.maybeInstantiateVerifier();
                if (this.signers == null && JarFile.this.jv != null) {
                    this.signers = JarFile.this.jv.getCodeSigners(JarFile.this, this);
                }
                if (this.signers == null) {
                    return null;
                }
                return (CodeSigner[]) this.signers.clone();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void maybeInstantiateVerifier() throws IOException {
        if (this.jv == null && this.verify) {
            String[] metaInfEntryNames = getMetaInfEntryNames();
            if (metaInfEntryNames != null) {
                for (String str : metaInfEntryNames) {
                    String upperCase = str.toUpperCase(Locale.ENGLISH);
                    if (upperCase.endsWith(".DSA") || upperCase.endsWith(".RSA") || upperCase.endsWith(".EC") || upperCase.endsWith(".SF")) {
                        getManifest();
                        return;
                    }
                }
            }
            this.verify = false;
        }
    }

    private void initializeVerifier() {
        try {
            String[] metaInfEntryNames = getMetaInfEntryNames();
            if (metaInfEntryNames != null) {
                ManifestEntryVerifier manifestEntryVerifier = null;
                for (int i = 0; i < metaInfEntryNames.length; i++) {
                    String upperCase = metaInfEntryNames[i].toUpperCase(Locale.ENGLISH);
                    if (MANIFEST_NAME.equals(upperCase) || SignatureFileVerifier.isBlockOrSF(upperCase)) {
                        JarEntry jarEntry = getJarEntry(metaInfEntryNames[i]);
                        if (jarEntry == null) {
                            throw new JarException("corrupted jar file");
                        }
                        if (manifestEntryVerifier == null) {
                            manifestEntryVerifier = new ManifestEntryVerifier(getManifestFromReference());
                        }
                        ManifestEntryVerifier manifestEntryVerifier2 = manifestEntryVerifier;
                        byte[] bytes = getBytes(jarEntry);
                        if (bytes != null && bytes.length > 0) {
                            this.jv.beginEntry(jarEntry, manifestEntryVerifier2);
                            this.jv.update(bytes.length, bytes, 0, bytes.length, manifestEntryVerifier2);
                            this.jv.update(-1, null, 0, 0, manifestEntryVerifier2);
                        }
                        manifestEntryVerifier = manifestEntryVerifier2;
                    }
                }
            }
        } catch (IOException e) {
            this.jv = null;
            this.verify = false;
            if (JarVerifier.debug != null) {
                JarVerifier.debug.println("jarfile parsing error!");
                e.printStackTrace();
            }
        }
        if (this.jv != null) {
            this.jv.doneWithMeta();
            if (JarVerifier.debug != null) {
                JarVerifier.debug.println("done with meta!");
            }
            if (this.jv.nothingToVerify()) {
                if (JarVerifier.debug != null) {
                    JarVerifier.debug.println("nothing to verify!");
                }
                this.jv = null;
                this.verify = false;
            }
        }
    }

    private byte[] getBytes(ZipEntry zipEntry) throws IOException {
        InputStream inputStream = super.getInputStream(zipEntry);
        Throwable th = null;
        try {
            byte[] fully = IOUtils.readFully(inputStream, (int) zipEntry.getSize(), true);
            if (inputStream != null) {
                inputStream.close();
            }
            return fully;
        } catch (Throwable th2) {
            if (inputStream != null) {
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    inputStream.close();
                }
            }
            throw th2;
        }
    }

    @Override
    public synchronized InputStream getInputStream(ZipEntry zipEntry) throws IOException {
        maybeInstantiateVerifier();
        if (this.jv == null) {
            return super.getInputStream(zipEntry);
        }
        if (!this.jvInitialized) {
            initializeVerifier();
            this.jvInitialized = true;
            if (this.jv == null) {
                return super.getInputStream(zipEntry);
            }
        }
        return new JarVerifier.VerifierStream(getManifestFromReference(), zipEntry instanceof JarFileEntry ? (JarEntry) zipEntry : getJarEntry(zipEntry.getName()), super.getInputStream(zipEntry), this.jv);
    }

    static {
        CLASSPATH_LASTOCC[99] = 1;
        CLASSPATH_LASTOCC[108] = 2;
        CLASSPATH_LASTOCC[115] = 5;
        CLASSPATH_LASTOCC[45] = 6;
        CLASSPATH_LASTOCC[112] = 7;
        CLASSPATH_LASTOCC[97] = 8;
        CLASSPATH_LASTOCC[116] = 9;
        CLASSPATH_LASTOCC[104] = 10;
        for (int i = 0; i < 9; i++) {
            CLASSPATH_OPTOSFT[i] = 10;
        }
        CLASSPATH_OPTOSFT[9] = 1;
    }

    private synchronized JarEntry getManEntry() {
        String[] metaInfEntryNames;
        if (this.manEntry == null) {
            this.manEntry = getJarEntry(MANIFEST_NAME);
            if (this.manEntry == null && (metaInfEntryNames = getMetaInfEntryNames()) != null) {
                int i = 0;
                while (true) {
                    if (i >= metaInfEntryNames.length) {
                        break;
                    }
                    if (!MANIFEST_NAME.equals(metaInfEntryNames[i].toUpperCase(Locale.ENGLISH))) {
                        i++;
                    } else {
                        this.manEntry = getJarEntry(metaInfEntryNames[i]);
                        break;
                    }
                }
            }
        }
        return this.manEntry;
    }

    public boolean hasClassPathAttribute() throws IOException {
        checkForSpecialAttributes();
        return this.hasClassPathAttribute;
    }

    private boolean match(char[] cArr, byte[] bArr, int[] iArr, int[] iArr2) {
        int length = cArr.length;
        int length2 = bArr.length - length;
        int iMax = 0;
        while (iMax <= length2) {
            int i = length - 1;
            while (i >= 0) {
                char c = (char) bArr[iMax + i];
                if (((c - 'A') | ('Z' - c)) >= 0) {
                    c = (char) (c + ' ');
                }
                if (c != cArr[i]) {
                    break;
                }
                i--;
            }
            return true;
        }
        return false;
    }

    private void checkForSpecialAttributes() throws IOException {
        if (this.hasCheckedSpecialAttributes) {
            return;
        }
        JarEntry manEntry = getManEntry();
        if (manEntry != null) {
            if (match(CLASSPATH_CHARS, getBytes(manEntry), CLASSPATH_LASTOCC, CLASSPATH_OPTOSFT)) {
                this.hasClassPathAttribute = true;
            }
        }
        this.hasCheckedSpecialAttributes = true;
    }

    JarEntry newEntry(ZipEntry zipEntry) {
        return new JarFileEntry(zipEntry);
    }
}
