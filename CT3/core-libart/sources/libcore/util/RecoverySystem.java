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

    public static void verify(InputStream blockStream, InputStream contentStream, Set<X509Certificate> trustedCerts) throws SignatureException, NoSuchAlgorithmException, IOException {
        PKCS7 block = new PKCS7(blockStream);
        X509Certificate[] certificates = block.getCertificates();
        if (certificates == null || certificates.length == 0) {
            throw new SignatureException("signature contains no certificates");
        }
        X509Certificate cert = certificates[0];
        PublicKey signatureKey = cert.getPublicKey();
        SignerInfo[] signerInfos = block.getSignerInfos();
        if (signerInfos == null || signerInfos.length == 0) {
            throw new SignatureException("signature contains no signedData");
        }
        SignerInfo signerInfo = signerInfos[0];
        boolean verified = false;
        Iterator c$iterator = trustedCerts.iterator();
        while (true) {
            if (!c$iterator.hasNext()) {
                break;
            }
            X509Certificate c = (X509Certificate) c$iterator.next();
            if (c.getPublicKey().equals(signatureKey)) {
                verified = true;
                break;
            }
        }
        if (!verified) {
            throw new SignatureException("signature doesn't match any trusted key");
        }
        SignerInfo verifyResult = block.verify(signerInfo, contentStream);
        if (verifyResult != null) {
        } else {
            throw new SignatureException("signature digest verification failed");
        }
    }
}
