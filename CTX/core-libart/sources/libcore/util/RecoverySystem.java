package libcore.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;

public class RecoverySystem {
    private RecoverySystem() {
    }

    public static void verify(InputStream inputStream, InputStream inputStream2, Set<X509Certificate> set) throws SignatureException, NoSuchAlgorithmException, IOException {
        PKCS7 pkcs7 = new PKCS7(inputStream);
        X509Certificate[] certificates = pkcs7.getCertificates();
        if (certificates == null || certificates.length == 0) {
            throw new SignatureException("signature contains no certificates");
        }
        boolean z = false;
        PublicKey publicKey = certificates[0].getPublicKey();
        SignerInfo[] signerInfos = pkcs7.getSignerInfos();
        if (signerInfos == null || signerInfos.length == 0) {
            throw new SignatureException("signature contains no signedData");
        }
        SignerInfo signerInfo = signerInfos[0];
        Iterator<X509Certificate> it = set.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            } else if (it.next().getPublicKey().equals(publicKey)) {
                z = true;
                break;
            }
        }
        if (!z) {
            throw new SignatureException("signature doesn't match any trusted key");
        }
        if (pkcs7.verify(signerInfo, inputStream2) == null) {
            throw new SignatureException("signature digest verification failed");
        }
    }
}
