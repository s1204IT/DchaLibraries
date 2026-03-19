package com.android.org.conscrypt;

import com.android.org.conscrypt.ct.CTConstants;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

final class CryptoUpcalls {
    private static final Logger logger = Logger.getLogger(CryptoUpcalls.class.getName());

    private CryptoUpcalls() {
    }

    private static boolean isOurProvider(Provider provider) {
        return provider.getClass().getPackage().equals(CryptoUpcalls.class.getPackage());
    }

    private static ArrayList<Provider> getExternalProviders(String str) {
        ArrayList<Provider> arrayList = new ArrayList<>(1);
        for (Provider provider : Security.getProviders(str)) {
            if (!isOurProvider(provider)) {
                arrayList.add(provider);
            }
        }
        if (arrayList.isEmpty()) {
            logger.warning("Could not find external provider for algorithm: " + str);
        }
        return arrayList;
    }

    static byte[] rawSignDigestWithPrivateKey(PrivateKey privateKey, byte[] bArr) {
        String str;
        Signature signature;
        String algorithm = privateKey.getAlgorithm();
        if ("RSA".equals(algorithm)) {
            str = "NONEwithRSA";
        } else if ("EC".equals(algorithm)) {
            str = "NONEwithECDSA";
        } else {
            throw new RuntimeException("Unexpected key type: " + privateKey.toString());
        }
        try {
            signature = Signature.getInstance(str);
            signature.initSign(privateKey);
            if (isOurProvider(signature.getProvider())) {
                signature = null;
            }
        } catch (InvalidKeyException e) {
            logger.warning("Preferred provider doesn't support key:");
            e.printStackTrace();
            signature = null;
        } catch (NoSuchAlgorithmException e2) {
            logger.warning("Unsupported signature algorithm: " + str);
            return null;
        }
        if (signature == null) {
            Iterator<Provider> it = getExternalProviders("Signature." + str).iterator();
            while (it.hasNext()) {
                try {
                    signature = Signature.getInstance(str, it.next());
                    signature.initSign(privateKey);
                    break;
                } catch (InvalidKeyException | NoSuchAlgorithmException e3) {
                    signature = null;
                }
            }
            if (signature == null) {
                logger.warning("Could not find provider for algorithm: " + str);
                return null;
            }
        }
        try {
            signature.update(bArr);
            return signature.sign();
        } catch (Exception e4) {
            logger.log(Level.WARNING, "Exception while signing message with " + privateKey.getAlgorithm() + " private key:", (Throwable) e4);
            return null;
        }
    }

    static byte[] rsaDecryptWithPrivateKey(PrivateKey privateKey, int i, byte[] bArr) {
        String str;
        Cipher cipher;
        String algorithm = privateKey.getAlgorithm();
        if (!"RSA".equals(algorithm)) {
            logger.warning("Unexpected key type: " + algorithm);
            return null;
        }
        if (i == 1) {
            str = "PKCS1Padding";
        } else {
            switch (i) {
                case CTConstants.CERTIFICATE_LENGTH_BYTES:
                    str = "NoPadding";
                    break;
                case 4:
                    str = "OAEPPadding";
                    break;
                default:
                    logger.warning("Unsupported OpenSSL/BoringSSL padding: " + i);
                    return null;
            }
        }
        String str2 = "RSA/ECB/" + str;
        try {
            cipher = Cipher.getInstance(str2);
            cipher.init(2, privateKey);
            if (isOurProvider(cipher.getProvider())) {
                cipher = null;
            }
        } catch (InvalidKeyException e) {
            logger.log(Level.WARNING, "Preferred provider doesn't support key:", (Throwable) e);
            cipher = null;
        } catch (NoSuchAlgorithmException e2) {
            logger.warning("Unsupported cipher algorithm: " + str2);
            return null;
        } catch (NoSuchPaddingException e3) {
            logger.warning("Unsupported cipher algorithm: " + str2);
            return null;
        }
        if (cipher == null) {
            Iterator<Provider> it = getExternalProviders("Cipher." + str2).iterator();
            while (it.hasNext()) {
                try {
                    cipher = Cipher.getInstance(str2, it.next());
                    cipher.init(2, privateKey);
                    if (cipher == null) {
                        logger.warning("Could not find provider for algorithm: " + str2);
                        return null;
                    }
                } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e4) {
                    cipher = null;
                }
            }
            if (cipher == null) {
            }
        }
        try {
            return cipher.doFinal(bArr);
        } catch (Exception e5) {
            logger.log(Level.WARNING, "Exception while decrypting message with " + privateKey.getAlgorithm() + " private key using " + str2 + ":", (Throwable) e5);
            return null;
        }
    }
}
