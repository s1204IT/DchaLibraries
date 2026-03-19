package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import sun.misc.HexDumpEncoder;
import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class CertificateExtensions implements CertAttrSet<Extension> {
    public static final String IDENT = "x509.info.extensions";
    public static final String NAME = "extensions";
    private Map<String, Extension> unparseableExtensions;
    private static final Debug debug = Debug.getInstance(X509CertImpl.NAME);
    private static Class[] PARAMS = {Boolean.class, Object.class};
    private Map<String, Extension> map = Collections.synchronizedMap(new TreeMap());
    private boolean unsupportedCritExt = false;

    public CertificateExtensions() {
    }

    public CertificateExtensions(DerInputStream derInputStream) throws IOException {
        init(derInputStream);
    }

    private void init(DerInputStream derInputStream) throws IOException {
        for (DerValue derValue : derInputStream.getSequence(5)) {
            parseExtension(new Extension(derValue));
        }
    }

    private void parseExtension(Extension extension) throws IOException {
        try {
            Class<?> cls = OIDMap.getClass(extension.getExtensionId());
            if (cls == null) {
                if (extension.isCritical()) {
                    this.unsupportedCritExt = true;
                }
                if (this.map.put(extension.getExtensionId().toString(), extension) == null) {
                    return;
                } else {
                    throw new IOException("Duplicate extensions not allowed");
                }
            }
            CertAttrSet certAttrSet = (CertAttrSet) cls.getConstructor(PARAMS).newInstance(Boolean.valueOf(extension.isCritical()), extension.getExtensionValue());
            if (this.map.put(certAttrSet.getName(), (Extension) certAttrSet) != null) {
                throw new IOException("Duplicate extensions not allowed");
            }
        } catch (IOException e) {
            throw e;
        } catch (InvocationTargetException e2) {
            Throwable targetException = e2.getTargetException();
            if (!extension.isCritical()) {
                if (this.unparseableExtensions == null) {
                    this.unparseableExtensions = new TreeMap();
                }
                this.unparseableExtensions.put(extension.getExtensionId().toString(), new UnparseableExtension(extension, targetException));
                if (debug != null) {
                    debug.println("Error parsing extension: " + ((Object) extension));
                    targetException.printStackTrace();
                    System.err.println(new HexDumpEncoder().encodeBuffer(extension.getExtensionValue()));
                    return;
                }
                return;
            }
            if (targetException instanceof IOException) {
                throw ((IOException) targetException);
            }
            throw new IOException(targetException);
        } catch (Exception e3) {
            throw new IOException(e3);
        }
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException, CertificateException {
        encode(outputStream, false);
    }

    public void encode(OutputStream outputStream, boolean z) throws IOException, CertificateException {
        DerOutputStream derOutputStream;
        DerOutputStream derOutputStream2 = new DerOutputStream();
        Object[] array = this.map.values().toArray();
        for (int i = 0; i < array.length; i++) {
            if (array[i] instanceof CertAttrSet) {
                ((CertAttrSet) array[i]).encode(derOutputStream2);
            } else if (array[i] instanceof Extension) {
                ((Extension) array[i]).encode(derOutputStream2);
            } else {
                throw new CertificateException("Illegal extension object");
            }
        }
        DerOutputStream derOutputStream3 = new DerOutputStream();
        derOutputStream3.write((byte) 48, derOutputStream2);
        if (!z) {
            derOutputStream = new DerOutputStream();
            derOutputStream.write(DerValue.createTag((byte) -128, true, (byte) 3), derOutputStream3);
        } else {
            derOutputStream = derOutputStream3;
        }
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (obj instanceof Extension) {
            this.map.put(str, (Extension) obj);
            return;
        }
        throw new IOException("Unknown extension type.");
    }

    @Override
    public Extension get(String str) throws IOException {
        Extension extension = this.map.get(str);
        if (extension == null) {
            throw new IOException("No extension found with name " + str);
        }
        return extension;
    }

    Extension getExtension(String str) {
        return this.map.get(str);
    }

    @Override
    public void delete(String str) throws IOException {
        if (this.map.get(str) == null) {
            throw new IOException("No extension found with name " + str);
        }
        this.map.remove(str);
    }

    public String getNameByOid(ObjectIdentifier objectIdentifier) throws IOException {
        for (String str : this.map.keySet()) {
            if (this.map.get(str).getExtensionId().equals((Object) objectIdentifier)) {
                return str;
            }
        }
        return null;
    }

    @Override
    public Enumeration<Extension> getElements() {
        return Collections.enumeration(this.map.values());
    }

    public Collection<Extension> getAllExtensions() {
        return this.map.values();
    }

    public Map<String, Extension> getUnparseableExtensions() {
        if (this.unparseableExtensions == null) {
            return Collections.emptyMap();
        }
        return this.unparseableExtensions;
    }

    @Override
    public String getName() {
        return "extensions";
    }

    public boolean hasUnsupportedCriticalExtension() {
        return this.unsupportedCritExt;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CertificateExtensions)) {
            return false;
        }
        CertificateExtensions certificateExtensions = (CertificateExtensions) obj;
        Object[] array = certificateExtensions.getAllExtensions().toArray();
        int length = array.length;
        if (length != this.map.size()) {
            return false;
        }
        String string = null;
        for (int i = 0; i < length; i++) {
            if (array[i] instanceof CertAttrSet) {
                string = ((CertAttrSet) array[i]).getName();
            }
            Extension extension = (Extension) array[i];
            if (string == null) {
                string = extension.getExtensionId().toString();
            }
            Extension extension2 = this.map.get(string);
            if (extension2 == null || !extension2.equals(extension)) {
                return false;
            }
        }
        return getUnparseableExtensions().equals(certificateExtensions.getUnparseableExtensions());
    }

    public int hashCode() {
        return this.map.hashCode() + getUnparseableExtensions().hashCode();
    }

    @Override
    public String toString() {
        return this.map.toString();
    }
}
