package com.android.org.bouncycastle.jcajce.provider.asymmetric.util;

import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.Wrapper;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class BaseCipherSpi extends CipherSpi {
    private byte[] iv;
    private int ivSize;
    private Class[] availableSpecs = {IvParameterSpec.class, PBEParameterSpec.class};
    protected AlgorithmParameters engineParams = null;
    protected Wrapper wrapEngine = null;

    protected BaseCipherSpi() {
    }

    @Override
    protected int engineGetBlockSize() {
        return 0;
    }

    @Override
    protected byte[] engineGetIV() {
        return null;
    }

    @Override
    protected int engineGetKeySize(Key key) {
        return key.getEncoded().length;
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return -1;
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException("can't support mode " + mode);
    }

    @Override
    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        throw new NoSuchPaddingException("Padding " + padding + " unknown.");
    }

    @Override
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        byte[] encoded = key.getEncoded();
        if (encoded == null) {
            throw new InvalidKeyException("Cannot wrap key, null encoding.");
        }
        try {
            return this.wrapEngine == null ? engineDoFinal(encoded, 0, encoded.length) : this.wrapEngine.wrap(encoded, 0, encoded.length);
        } catch (BadPaddingException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        }
    }

    @Override
    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws InvalidKeyException {
        byte[] encoded;
        Key keyGeneratePrivate;
        try {
            if (this.wrapEngine == null) {
                encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
            } else {
                encoded = this.wrapEngine.unwrap(wrappedKey, 0, wrappedKey.length);
            }
            if (wrappedKeyType == 3) {
                return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
            }
            if (wrappedKeyAlgorithm.equals("") && wrappedKeyType == 2) {
                try {
                    PrivateKeyInfo in = PrivateKeyInfo.getInstance(encoded);
                    PrivateKey privKey = BouncyCastleProvider.getPrivateKey(in);
                    if (privKey == null) {
                        throw new InvalidKeyException("algorithm " + in.getPrivateKeyAlgorithm().getAlgorithm() + " not supported");
                    }
                    return privKey;
                } catch (Exception e) {
                    throw new InvalidKeyException("Invalid key encoding.");
                }
            }
            try {
                KeyFactory kf = KeyFactory.getInstance(wrappedKeyAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
                if (wrappedKeyType == 1) {
                    keyGeneratePrivate = kf.generatePublic(new X509EncodedKeySpec(encoded));
                } else if (wrappedKeyType == 2) {
                    keyGeneratePrivate = kf.generatePrivate(new PKCS8EncodedKeySpec(encoded));
                } else {
                    throw new InvalidKeyException("Unknown key type " + wrappedKeyType);
                }
                return keyGeneratePrivate;
            } catch (NoSuchAlgorithmException e2) {
                throw new InvalidKeyException("Unknown key type " + e2.getMessage());
            } catch (NoSuchProviderException e3) {
                throw new InvalidKeyException("Unknown key type " + e3.getMessage());
            } catch (InvalidKeySpecException e22) {
                throw new InvalidKeyException("Unknown key type " + e22.getMessage());
            }
        } catch (InvalidCipherTextException e4) {
            throw new InvalidKeyException(e4.getMessage());
        } catch (BadPaddingException e5) {
            throw new InvalidKeyException(e5.getMessage());
        } catch (IllegalBlockSizeException e23) {
            throw new InvalidKeyException(e23.getMessage());
        }
    }
}
