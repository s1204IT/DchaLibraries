package com.android.org.conscrypt;

import com.android.org.conscrypt.OpenSSLX509CertificateFactory;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public final class InternalUtil {
    public static PublicKey logKeyToPublicKey(byte[] bArr) throws NoSuchAlgorithmException {
        try {
            return new OpenSSLKey(NativeCrypto.EVP_parse_public_key(bArr)).getPublicKey();
        } catch (OpenSSLX509CertificateFactory.ParsingException e) {
            throw new NoSuchAlgorithmException(e);
        }
    }

    public static PublicKey readPublicKeyPem(InputStream inputStream) throws NoSuchAlgorithmException, InvalidKeyException {
        return OpenSSLKey.fromPublicKeyPemInputStream(inputStream).getPublicKey();
    }

    public static byte[] getOcspSingleExtension(byte[] bArr, String str, OpenSSLX509Certificate openSSLX509Certificate, OpenSSLX509Certificate openSSLX509Certificate2) {
        return NativeCrypto.get_ocsp_single_extension(bArr, str, openSSLX509Certificate.getContext(), openSSLX509Certificate, openSSLX509Certificate2.getContext(), openSSLX509Certificate2);
    }

    private InternalUtil() {
    }
}
