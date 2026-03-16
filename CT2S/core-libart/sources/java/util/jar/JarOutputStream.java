package java.util.jar;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JarOutputStream extends ZipOutputStream {
    private Manifest manifest;

    public JarOutputStream(OutputStream os, Manifest manifest) throws IOException {
        super(os);
        if (manifest == null) {
            throw new NullPointerException("manifest == null");
        }
        this.manifest = manifest;
        ZipEntry ze = new ZipEntry(JarFile.MANIFEST_NAME);
        putNextEntry(ze);
        this.manifest.write(this);
        closeEntry();
    }

    public JarOutputStream(OutputStream os) throws IOException {
        super(os);
    }

    @Override
    public void putNextEntry(ZipEntry ze) throws IOException {
        super.putNextEntry(ze);
    }
}
