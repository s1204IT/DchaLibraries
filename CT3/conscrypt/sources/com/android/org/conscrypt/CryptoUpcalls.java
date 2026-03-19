package com.android.org.conscrypt;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public final class CryptoUpcalls {
    private CryptoUpcalls() {
    }

    private static boolean isOurProvider(Provider p) {
        return p.getClass().getPackage().equals(CryptoUpcalls.class.getPackage());
    }

    private static ArrayList<Provider> getExternalProviders(String algorithm) {
        ArrayList<Provider> providers = new ArrayList<>(1);
        for (Provider p : Security.getProviders(algorithm)) {
            if (!isOurProvider(p)) {
                providers.add(p);
            }
        }
        if (providers.isEmpty()) {
            System.err.println("Could not find external provider for algorithm: " + algorithm);
        }
        return providers;
    }

    public static byte[] rawSignDigestWithPrivateKey(PrivateKey javaKey, byte[] message) {
        String algorithm;
        Signature signature;
        String keyAlgorithm = javaKey.getAlgorithm();
        if ("RSA".equals(keyAlgorithm)) {
            algorithm = "NONEwithRSA";
        } else if ("EC".equals(keyAlgorithm)) {
            algorithm = "NONEwithECDSA";
        } else {
            throw new RuntimeException("Unexpected key type: " + javaKey.toString());
        }
        try {
            signature = Signature.getInstance(algorithm);
            signature.initSign(javaKey);
            if (isOurProvider(signature.getProvider())) {
                signature = null;
            }
        } catch (InvalidKeyException e) {
            System.err.println("Preferred provider doesn't support key:");
            e.printStackTrace();
            signature = null;
        } catch (NoSuchAlgorithmException e2) {
            System.err.println("Unsupported signature algorithm: " + algorithm);
            return null;
        }
        if (signature == null) {
            ArrayList<Provider> providers = getExternalProviders("Signature." + algorithm);
            for (Provider p : providers) {
                try {
                    signature = Signature.getInstance(algorithm, p);
                    signature.initSign(javaKey);
                    break;
                } catch (InvalidKeyException | NoSuchAlgorithmException e3) {
                    signature = null;
                }
            }
            if (signature == null) {
                System.err.println("Could not find provider for algorithm: " + algorithm);
                return null;
            }
        }
        try {
            signature.update(message);
            return signature.sign();
        } catch (Exception e4) {
            System.err.println("Exception while signing message with " + javaKey.getAlgorithm() + " private key:");
            e4.printStackTrace();
            return null;
        }
    }

    public static byte[] rsaDecryptWithPrivateKey(PrivateKey javaKey, int openSSLPadding, byte[] input) {
        String jcaPadding;
        Cipher c;
        String keyAlgorithm = javaKey.getAlgorithm();
        if (!"RSA".equals(keyAlgorithm)) {
            System.err.println("Unexpected key type: " + keyAlgorithm);
            return null;
        }
        switch (openSSLPadding) {
            case 1:
                jcaPadding = "PKCS1Padding";
                break;
            case 2:
            default:
                System.err.println("Unsupported OpenSSL/BoringSSL padding: " + openSSLPadding);
                return null;
            case 3:
                jcaPadding = "NoPadding";
                break;
            case 4:
                jcaPadding = "OAEPPadding";
                break;
        }
        String transformation = "RSA/ECB/" + jcaPadding;
        try {
            c = Cipher.getInstance(transformation);
            c.init(2, javaKey);
            if (isOurProvider(c.getProvider())) {
                c = null;
            }
        } catch (InvalidKeyException e) {
            System.err.println("Preferred provider doesn't support key:");
            e.printStackTrace();
            c = null;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e2) {
            System.err.println("Unsupported cipher algorithm: " + transformation);
            return null;
        }
        if (c == null) {
            ArrayList<Provider> providers = getExternalProviders("Cipher." + transformation);
            for (Provider p : providers) {
                try {
                    c = Cipher.getInstance(transformation, p);
                    c.init(2, javaKey);
                    if (c == null) {
                        System.err.println("Could not find provider for algorithm: " + transformation);
                        return null;
                    }
                } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e3) {
                    c = null;
                }
            }
            if (c == null) {
            }
        }
        try {
            return c.doFinal(input);
        } catch (Exception e4) {
            System.err.println("Exception while decrypting message with " + javaKey.getAlgorithm() + " private key using " + transformation + ":");
            e4.printStackTrace();
            return null;
        }
    }
}
