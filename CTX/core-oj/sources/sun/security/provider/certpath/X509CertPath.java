package sun.security.provider.certpath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;

public class X509CertPath extends CertPath {
    private static final String COUNT_ENCODING = "count";
    private static final String PKCS7_ENCODING = "PKCS7";
    private static final String PKIPATH_ENCODING = "PkiPath";
    private static final Collection<String> encodingList;
    private static final long serialVersionUID = 4989800333263052980L;
    private List<X509Certificate> certs;

    static {
        ArrayList arrayList = new ArrayList(2);
        arrayList.add(PKIPATH_ENCODING);
        arrayList.add(PKCS7_ENCODING);
        encodingList = Collections.unmodifiableCollection(arrayList);
    }

    public X509CertPath(List<? extends Certificate> list) throws CertificateException {
        super("X.509");
        for (Certificate certificate : list) {
            if (!(certificate instanceof X509Certificate)) {
                throw new CertificateException("List is not all X509Certificates: " + certificate.getClass().getName());
            }
        }
        this.certs = Collections.unmodifiableList(new ArrayList(list));
    }

    public X509CertPath(InputStream inputStream) throws CertificateException {
        this(inputStream, PKIPATH_ENCODING);
    }

    public X509CertPath(InputStream inputStream, String str) throws CertificateException {
        byte b;
        super("X.509");
        int iHashCode = str.hashCode();
        if (iHashCode != 76183020) {
            b = (iHashCode == 1148619507 && str.equals(PKIPATH_ENCODING)) ? (byte) 0 : (byte) -1;
        } else if (str.equals(PKCS7_ENCODING)) {
            b = 1;
        }
        switch (b) {
            case 0:
                this.certs = parsePKIPATH(inputStream);
                return;
            case 1:
                this.certs = parsePKCS7(inputStream);
                return;
            default:
                throw new CertificateException("unsupported encoding");
        }
    }

    private static List<X509Certificate> parsePKIPATH(InputStream inputStream) throws CertificateException {
        if (inputStream == null) {
            throw new CertificateException("input stream is null");
        }
        try {
            DerValue[] sequence = new DerInputStream(readAllBytes(inputStream)).getSequence(3);
            if (sequence.length == 0) {
                return Collections.emptyList();
            }
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            ArrayList arrayList = new ArrayList(sequence.length);
            for (int length = sequence.length - 1; length >= 0; length--) {
                arrayList.add((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(sequence[length].toByteArray())));
            }
            return Collections.unmodifiableList(arrayList);
        } catch (IOException e) {
            throw new CertificateException("IOException parsing PkiPath data: " + ((Object) e), e);
        }
    }

    private static List<X509Certificate> parsePKCS7(InputStream inputStream) throws CertificateException {
        List arrayList;
        if (inputStream == null) {
            throw new CertificateException("input stream is null");
        }
        try {
            if (!inputStream.markSupported()) {
                inputStream = new ByteArrayInputStream(readAllBytes(inputStream));
            }
            X509Certificate[] certificates = new PKCS7(inputStream).getCertificates();
            if (certificates != null) {
                arrayList = Arrays.asList(certificates);
            } else {
                arrayList = new ArrayList(0);
            }
            return Collections.unmodifiableList(arrayList);
        } catch (IOException e) {
            throw new CertificateException("IOException parsing PKCS7 data: " + ((Object) e));
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[8192];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);
        while (true) {
            int i = inputStream.read(bArr);
            if (i != -1) {
                byteArrayOutputStream.write(bArr, 0, i);
            } else {
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException {
        return encodePKIPATH();
    }

    private byte[] encodePKIPATH() throws CertificateEncodingException {
        ListIterator<X509Certificate> listIterator = this.certs.listIterator(this.certs.size());
        try {
            DerOutputStream derOutputStream = new DerOutputStream();
            while (listIterator.hasPrevious()) {
                X509Certificate x509CertificatePrevious = listIterator.previous();
                if (this.certs.lastIndexOf(x509CertificatePrevious) != this.certs.indexOf(x509CertificatePrevious)) {
                    throw new CertificateEncodingException("Duplicate Certificate");
                }
                derOutputStream.write(x509CertificatePrevious.getEncoded());
            }
            DerOutputStream derOutputStream2 = new DerOutputStream();
            derOutputStream2.write((byte) 48, derOutputStream);
            return derOutputStream2.toByteArray();
        } catch (IOException e) {
            throw new CertificateEncodingException("IOException encoding PkiPath data: " + ((Object) e), e);
        }
    }

    private byte[] encodePKCS7() throws CertificateEncodingException {
        PKCS7 pkcs7 = new PKCS7(new AlgorithmId[0], new ContentInfo(ContentInfo.DATA_OID, (DerValue) null), (X509Certificate[]) this.certs.toArray(new X509Certificate[this.certs.size()]), new SignerInfo[0]);
        DerOutputStream derOutputStream = new DerOutputStream();
        try {
            pkcs7.encodeSignedData(derOutputStream);
            return derOutputStream.toByteArray();
        } catch (IOException e) {
            throw new CertificateEncodingException(e.getMessage());
        }
    }

    @Override
    public byte[] getEncoded(String str) throws CertificateEncodingException {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 76183020) {
            b = (iHashCode == 1148619507 && str.equals(PKIPATH_ENCODING)) ? (byte) 0 : (byte) -1;
        } else if (str.equals(PKCS7_ENCODING)) {
            b = 1;
        }
        switch (b) {
            case 0:
                return encodePKIPATH();
            case 1:
                return encodePKCS7();
            default:
                throw new CertificateEncodingException("unsupported encoding");
        }
    }

    public static Iterator<String> getEncodingsStatic() {
        return encodingList.iterator();
    }

    @Override
    public Iterator<String> getEncodings() {
        return getEncodingsStatic();
    }

    @Override
    public List<X509Certificate> getCertificates() {
        return this.certs;
    }
}
