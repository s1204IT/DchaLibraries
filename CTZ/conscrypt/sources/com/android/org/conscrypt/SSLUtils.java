package com.android.org.conscrypt;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

final class SSLUtils {
    private static final String KEY_TYPE_EC = "EC";
    private static final String KEY_TYPE_RSA = "RSA";
    private static final int MAX_ENCRYPTION_OVERHEAD_DIFF = 2147483562;
    private static final int MAX_ENCRYPTION_OVERHEAD_LENGTH = 85;
    private static final int MAX_PROTOCOL_LENGTH = 255;
    static final boolean USE_ENGINE_SOCKET_BY_DEFAULT = Boolean.parseBoolean(System.getProperty("com.android.org.conscrypt.useEngineSocketByDefault", "false"));
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    enum SessionType {
        OPEN_SSL(1),
        OPEN_SSL_WITH_OCSP(2),
        OPEN_SSL_WITH_TLS_SCT(3);

        final int value;

        SessionType(int i) {
            this.value = i;
        }

        static boolean isSupportedType(int i) {
            return i == OPEN_SSL.value || i == OPEN_SSL_WITH_OCSP.value || i == OPEN_SSL_WITH_TLS_SCT.value;
        }
    }

    static final class EngineStates {
        static final int STATE_CLOSED = 8;
        static final int STATE_CLOSED_INBOUND = 6;
        static final int STATE_CLOSED_OUTBOUND = 7;
        static final int STATE_HANDSHAKE_COMPLETED = 3;
        static final int STATE_HANDSHAKE_STARTED = 2;
        static final int STATE_MODE_SET = 1;
        static final int STATE_NEW = 0;
        static final int STATE_READY = 5;
        static final int STATE_READY_HANDSHAKE_CUT_THROUGH = 4;

        private EngineStates() {
        }
    }

    static SSLSession unwrapSession(SSLSession sSLSession) {
        while (sSLSession instanceof SessionDecorator) {
            sSLSession = ((SessionDecorator) sSLSession).getDelegate();
        }
        return sSLSession;
    }

    static X509Certificate[] decodeX509CertificateChain(byte[][] bArr) throws CertificateException {
        CertificateFactory certificateFactory = getCertificateFactory();
        int length = bArr.length;
        X509Certificate[] x509CertificateArr = new X509Certificate[length];
        for (int i = 0; i < length; i++) {
            x509CertificateArr[i] = decodeX509Certificate(certificateFactory, bArr[i]);
        }
        return x509CertificateArr;
    }

    private static CertificateFactory getCertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            return null;
        }
    }

    private static X509Certificate decodeX509Certificate(CertificateFactory certificateFactory, byte[] bArr) throws CertificateException {
        if (certificateFactory != null) {
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(bArr));
        }
        return OpenSSLX509Certificate.fromX509Der(bArr);
    }

    static String getServerX509KeyType(long j) throws SSLException {
        String strSSL_CIPHER_get_kx_name = NativeCrypto.SSL_CIPHER_get_kx_name(j);
        if (strSSL_CIPHER_get_kx_name.equals(KEY_TYPE_RSA) || strSSL_CIPHER_get_kx_name.equals("DHE_RSA") || strSSL_CIPHER_get_kx_name.equals("ECDHE_RSA")) {
            return KEY_TYPE_RSA;
        }
        if (strSSL_CIPHER_get_kx_name.equals("ECDHE_ECDSA")) {
            return KEY_TYPE_EC;
        }
        return null;
    }

    static String getClientKeyType(byte b) {
        if (b == 1) {
            return KEY_TYPE_RSA;
        }
        if (b == 64) {
            return KEY_TYPE_EC;
        }
        return null;
    }

    static Set<String> getSupportedClientKeyTypes(byte[] bArr) {
        HashSet hashSet = new HashSet(bArr.length);
        for (byte b : bArr) {
            String clientKeyType = getClientKeyType(b);
            if (clientKeyType != null) {
                hashSet.add(clientKeyType);
            }
        }
        return hashSet;
    }

    static byte[][] encodeIssuerX509Principals(X509Certificate[] x509CertificateArr) throws CertificateEncodingException {
        byte[][] bArr = new byte[x509CertificateArr.length][];
        for (int i = 0; i < x509CertificateArr.length; i++) {
            bArr[i] = x509CertificateArr[i].getIssuerX500Principal().getEncoded();
        }
        return bArr;
    }

    static javax.security.cert.X509Certificate[] toCertificateChain(X509Certificate[] x509CertificateArr) throws SSLPeerUnverifiedException {
        try {
            javax.security.cert.X509Certificate[] x509CertificateArr2 = new javax.security.cert.X509Certificate[x509CertificateArr.length];
            for (int i = 0; i < x509CertificateArr.length; i++) {
                x509CertificateArr2[i] = javax.security.cert.X509Certificate.getInstance(x509CertificateArr[i].getEncoded());
            }
            return x509CertificateArr2;
        } catch (CertificateEncodingException e) {
            SSLPeerUnverifiedException sSLPeerUnverifiedException = new SSLPeerUnverifiedException(e.getMessage());
            sSLPeerUnverifiedException.initCause(sSLPeerUnverifiedException);
            throw sSLPeerUnverifiedException;
        } catch (javax.security.cert.CertificateException e2) {
            SSLPeerUnverifiedException sSLPeerUnverifiedException2 = new SSLPeerUnverifiedException(e2.getMessage());
            sSLPeerUnverifiedException2.initCause(sSLPeerUnverifiedException2);
            throw sSLPeerUnverifiedException2;
        }
    }

    static int calculateOutNetBufSize(int i) {
        return Math.min(16709, MAX_ENCRYPTION_OVERHEAD_LENGTH + Math.min(MAX_ENCRYPTION_OVERHEAD_DIFF, i));
    }

    static SSLHandshakeException toSSLHandshakeException(Throwable th) {
        if (th instanceof SSLHandshakeException) {
            return (SSLHandshakeException) th;
        }
        return (SSLHandshakeException) new SSLHandshakeException(th.getMessage()).initCause(th);
    }

    static SSLException toSSLException(Throwable th) {
        if (th instanceof SSLException) {
            return (SSLException) th;
        }
        return new SSLException(th);
    }

    static String toProtocolString(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        return new String(bArr, US_ASCII);
    }

    static byte[] toProtocolBytes(String str) {
        if (str == null) {
            return null;
        }
        return str.getBytes(US_ASCII);
    }

    static String[] decodeProtocols(byte[] bArr) {
        String str;
        String string;
        if (bArr.length == 0) {
            return EmptyArray.STRING;
        }
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        while (i2 < bArr.length) {
            byte b = bArr[i2];
            if (b < 0 || b > bArr.length - i2) {
                StringBuilder sb = new StringBuilder();
                sb.append("Protocol has invalid length (");
                sb.append((int) b);
                sb.append(" at position ");
                sb.append(i2);
                sb.append("): ");
                if (bArr.length < 50) {
                    string = Arrays.toString(bArr);
                } else {
                    string = bArr.length + " byte array";
                }
                sb.append(string);
                throw new IllegalArgumentException(sb.toString());
            }
            i3++;
            i2 += 1 + b;
        }
        String[] strArr = new String[i3];
        int i4 = 0;
        while (i < bArr.length) {
            byte b2 = bArr[i];
            int i5 = i4 + 1;
            if (b2 > 0) {
                str = new String(bArr, i + 1, b2, US_ASCII);
            } else {
                str = "";
            }
            strArr[i4] = str;
            i += b2 + 1;
            i4 = i5;
        }
        return strArr;
    }

    static byte[] encodeProtocols(String[] strArr) {
        if (strArr == null) {
            throw new IllegalArgumentException("protocols array must be non-null");
        }
        if (strArr.length == 0) {
            return EmptyArray.BYTE;
        }
        int i = 0;
        for (int i2 = 0; i2 < strArr.length; i2++) {
            if (strArr[i2] == null) {
                throw new IllegalArgumentException("protocol[" + i2 + "] is null");
            }
            int length = strArr[i2].length();
            if (length == 0 || length > MAX_PROTOCOL_LENGTH) {
                throw new IllegalArgumentException("protocol[" + i2 + "] has invalid length: " + length);
            }
            i += 1 + length;
        }
        byte[] bArr = new byte[i];
        int i3 = 0;
        int i4 = 0;
        while (i3 < strArr.length) {
            String str = strArr[i3];
            int length2 = str.length();
            int i5 = i4 + 1;
            bArr[i4] = (byte) length2;
            int i6 = 0;
            while (i6 < length2) {
                char cCharAt = str.charAt(i6);
                if (cCharAt <= 127) {
                    bArr[i5] = (byte) cCharAt;
                    i6++;
                    i5++;
                } else {
                    throw new IllegalArgumentException("Protocol contains invalid character: " + cCharAt + "(protocol=" + str + ")");
                }
            }
            i3++;
            i4 = i5;
        }
        return bArr;
    }

    static int getEncryptedPacketLength(ByteBuffer[] byteBufferArr, int i) {
        ByteBuffer byteBuffer = byteBufferArr[i];
        if (byteBuffer.remaining() >= 5) {
            return getEncryptedPacketLength(byteBuffer);
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(5);
        while (true) {
            int i2 = i + 1;
            ByteBuffer byteBuffer2 = byteBufferArr[i];
            int iPosition = byteBuffer2.position();
            int iLimit = byteBuffer2.limit();
            if (byteBuffer2.remaining() > byteBufferAllocate.remaining()) {
                byteBuffer2.limit(byteBufferAllocate.remaining() + iPosition);
            }
            try {
                byteBufferAllocate.put(byteBuffer2);
                byteBuffer2.limit(iLimit);
                byteBuffer2.position(iPosition);
                if (byteBufferAllocate.hasRemaining()) {
                    i = i2;
                } else {
                    byteBufferAllocate.flip();
                    return getEncryptedPacketLength(byteBufferAllocate);
                }
            } catch (Throwable th) {
                byteBuffer2.limit(iLimit);
                byteBuffer2.position(iPosition);
                throw th;
            }
        }
    }

    private static int getEncryptedPacketLength(ByteBuffer byteBuffer) {
        int iPosition = byteBuffer.position();
        switch (unsignedByte(byteBuffer.get(iPosition))) {
            case 20:
            case 21:
            case 22:
            case 23:
                if (unsignedByte(byteBuffer.get(iPosition + 1)) == 3 && (r4 = unsignedShort(byteBuffer.getShort(iPosition + 3)) + 5) > 5) {
                }
                break;
        }
        return -1;
    }

    private static short unsignedByte(byte b) {
        return (short) (b & 255);
    }

    private static int unsignedShort(short s) {
        return s & 65535;
    }

    private SSLUtils() {
    }
}
