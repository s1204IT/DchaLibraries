package java.util.jar;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import sun.misc.JarIndex;
import sun.security.util.ManifestEntryVerifier;

public class JarInputStream extends ZipInputStream {
    private final boolean doVerify;
    private JarEntry first;
    private JarVerifier jv;
    private Manifest man;
    private ManifestEntryVerifier mev;
    private boolean tryManifest;

    public JarInputStream(InputStream inputStream) throws IOException {
        this(inputStream, true);
    }

    public JarInputStream(InputStream inputStream, boolean z) throws IOException {
        super(inputStream);
        this.doVerify = z;
        JarEntry jarEntry = (JarEntry) super.getNextEntry();
        if (jarEntry != null && jarEntry.getName().equalsIgnoreCase("META-INF/")) {
            jarEntry = (JarEntry) super.getNextEntry();
        }
        this.first = checkManifest(jarEntry);
    }

    private JarEntry checkManifest(JarEntry jarEntry) throws IOException {
        if (jarEntry != null && JarFile.MANIFEST_NAME.equalsIgnoreCase(jarEntry.getName())) {
            this.man = new Manifest();
            byte[] bytes = getBytes(new BufferedInputStream(this));
            this.man.read(new ByteArrayInputStream(bytes));
            closeEntry();
            if (this.doVerify) {
                this.jv = new JarVerifier(bytes);
                this.mev = new ManifestEntryVerifier(this.man);
            }
            return (JarEntry) super.getNextEntry();
        }
        return jarEntry;
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[8192];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);
        while (true) {
            int i = inputStream.read(bArr, 0, bArr.length);
            if (i != -1) {
                byteArrayOutputStream.write(bArr, 0, i);
            } else {
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    public Manifest getManifest() {
        return this.man;
    }

    @Override
    public ZipEntry getNextEntry() throws IOException {
        JarEntry jarEntryCheckManifest;
        if (this.first == null) {
            jarEntryCheckManifest = (JarEntry) super.getNextEntry();
            if (this.tryManifest) {
                jarEntryCheckManifest = checkManifest(jarEntryCheckManifest);
                this.tryManifest = false;
            }
        } else {
            jarEntryCheckManifest = this.first;
            if (this.first.getName().equalsIgnoreCase(JarIndex.INDEX_NAME)) {
                this.tryManifest = true;
            }
            this.first = null;
        }
        if (this.jv != null && jarEntryCheckManifest != null) {
            if (this.jv.nothingToVerify()) {
                this.jv = null;
                this.mev = null;
            } else {
                this.jv.beginEntry(jarEntryCheckManifest, this.mev);
            }
        }
        return jarEntryCheckManifest;
    }

    public JarEntry getNextJarEntry() throws IOException {
        return (JarEntry) getNextEntry();
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        if (this.first == null) {
            i3 = super.read(bArr, i, i2);
        } else {
            i3 = -1;
        }
        if (this.jv != null) {
            this.jv.update(i3, bArr, i, i2, this.mev);
        }
        return i3;
    }

    @Override
    protected ZipEntry createZipEntry(String str) {
        JarEntry jarEntry = new JarEntry(str);
        if (this.man != null) {
            jarEntry.attr = this.man.getAttributes(str);
        }
        return jarEntry;
    }
}
