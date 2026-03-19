package com.android.org.conscrypt;

import com.android.org.conscrypt.OpenSSLX509CertificateFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class OpenSSLX509CertPath extends CertPath {

    private static final int[] f5xce6738da = null;
    private static final int PUSHBACK_SIZE = 64;
    private final List<? extends X509Certificate> mCertificates;
    private static final byte[] PKCS7_MARKER = {45, 45, 45, 45, 45, 66, 69, 71, 73, 78, 32, 80, 75, 67, 83, 55};
    private static final List<String> ALL_ENCODINGS = Collections.unmodifiableList(Arrays.asList(Encoding.PKI_PATH.apiName, Encoding.PKCS7.apiName));
    private static final Encoding DEFAULT_ENCODING = Encoding.PKI_PATH;

    private static int[] m11x7a430eb6() {
        if (f5xce6738da != null) {
            return f5xce6738da;
        }
        int[] iArr = new int[Encoding.valuesCustom().length];
        try {
            iArr[Encoding.PKCS7.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Encoding.PKI_PATH.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f5xce6738da = iArr;
        return iArr;
    }

    private enum Encoding {
        PKI_PATH("PkiPath"),
        PKCS7("PKCS7");

        private final String apiName;

        public static Encoding[] valuesCustom() {
            return values();
        }

        Encoding(String apiName) {
            this.apiName = apiName;
        }

        static Encoding findByApiName(String apiName) throws CertificateEncodingException {
            for (Encoding element : valuesCustom()) {
                if (element.apiName.equals(apiName)) {
                    return element;
                }
            }
            return null;
        }
    }

    static Iterator<String> getEncodingsIterator() {
        return ALL_ENCODINGS.iterator();
    }

    protected OpenSSLX509CertPath(List<? extends X509Certificate> certificates) {
        super("X.509");
        this.mCertificates = certificates;
    }

    @Override
    public List<? extends Certificate> getCertificates() {
        return Collections.unmodifiableList(this.mCertificates);
    }

    private byte[] getEncoded(Encoding encoding) throws CertificateEncodingException {
        OpenSSLX509Certificate[] certs = new OpenSSLX509Certificate[this.mCertificates.size()];
        long[] certRefs = new long[certs.length];
        int i = 0;
        for (int j = certs.length - 1; j >= 0; j--) {
            X509Certificate cert = this.mCertificates.get(i);
            if (cert instanceof OpenSSLX509Certificate) {
                certs[j] = (OpenSSLX509Certificate) cert;
            } else {
                certs[j] = OpenSSLX509Certificate.fromX509Der(cert.getEncoded());
            }
            certRefs[j] = certs[j].getContext();
            i++;
        }
        switch (m11x7a430eb6()[encoding.ordinal()]) {
            case 1:
                return NativeCrypto.i2d_PKCS7(certRefs);
            case 2:
                return NativeCrypto.ASN1_seq_pack_X509(certRefs);
            default:
                throw new CertificateEncodingException("Unknown encoding");
        }
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException {
        return getEncoded(DEFAULT_ENCODING);
    }

    @Override
    public byte[] getEncoded(String encoding) throws CertificateEncodingException {
        Encoding enc = Encoding.findByApiName(encoding);
        if (enc == null) {
            throw new CertificateEncodingException("Invalid encoding: " + encoding);
        }
        return getEncoded(enc);
    }

    @Override
    public Iterator<String> getEncodings() {
        return getEncodingsIterator();
    }

    private static CertPath fromPkiPathEncoding(InputStream inStream) throws CertificateException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(inStream, true);
        boolean markable = inStream.markSupported();
        if (markable) {
            inStream.mark(64);
        }
        try {
            try {
                long[] certRefs = NativeCrypto.ASN1_seq_unpack_X509_bio(bis.getBioContext());
                if (certRefs == null) {
                    return new OpenSSLX509CertPath(Collections.emptyList());
                }
                List<org.conscrypt.OpenSSLX509Certificate> certs = new ArrayList<>(certRefs.length);
                for (int i = certRefs.length - 1; i >= 0; i--) {
                    if (certRefs[i] != 0) {
                        certs.add(new OpenSSLX509Certificate(certRefs[i]));
                    }
                }
                return new OpenSSLX509CertPath(certs);
            } catch (Exception e) {
                if (markable) {
                    try {
                        inStream.reset();
                    } catch (IOException e2) {
                    }
                }
                throw new CertificateException(e);
            }
        } finally {
            bis.release();
        }
    }

    private static CertPath fromPkcs7Encoding(InputStream inStream) throws CertificateException {
        if (inStream != null) {
            try {
                if (inStream.available() != 0) {
                    boolean markable = inStream.markSupported();
                    if (markable) {
                        inStream.mark(64);
                    }
                    PushbackInputStream pbis = new PushbackInputStream(inStream, 64);
                    try {
                        byte[] buffer = new byte[PKCS7_MARKER.length];
                        int len = pbis.read(buffer);
                        if (len < 0) {
                            throw new OpenSSLX509CertificateFactory.ParsingException("inStream is empty");
                        }
                        pbis.unread(buffer, 0, len);
                        if (len == PKCS7_MARKER.length && Arrays.equals(PKCS7_MARKER, buffer)) {
                            return new OpenSSLX509CertPath(OpenSSLX509Certificate.fromPkcs7PemInputStream(pbis));
                        }
                        return new OpenSSLX509CertPath(OpenSSLX509Certificate.fromPkcs7DerInputStream(pbis));
                    } catch (Exception e) {
                        if (markable) {
                            try {
                                inStream.reset();
                            } catch (IOException e2) {
                            }
                        }
                        throw new CertificateException(e);
                    }
                }
            } catch (IOException e3) {
                throw new CertificateException("Problem reading input stream", e3);
            }
        }
        return new OpenSSLX509CertPath(Collections.emptyList());
    }

    private static CertPath fromEncoding(InputStream inStream, Encoding encoding) throws CertificateException {
        switch (m11x7a430eb6()[encoding.ordinal()]) {
            case 1:
                return fromPkcs7Encoding(inStream);
            case 2:
                return fromPkiPathEncoding(inStream);
            default:
                throw new CertificateEncodingException("Unknown encoding");
        }
    }

    public static CertPath fromEncoding(InputStream inStream, String encoding) throws CertificateException {
        if (inStream == null) {
            throw new CertificateException("inStream == null");
        }
        Encoding enc = Encoding.findByApiName(encoding);
        if (enc == null) {
            throw new CertificateException("Invalid encoding: " + encoding);
        }
        return fromEncoding(inStream, enc);
    }

    public static CertPath fromEncoding(InputStream inStream) throws CertificateException {
        if (inStream == null) {
            throw new CertificateException("inStream == null");
        }
        return fromEncoding(inStream, DEFAULT_ENCODING);
    }
}
