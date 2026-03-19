package com.android.org.conscrypt;

import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactorySpi;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class DESEDESecretKeyFactory extends SecretKeyFactorySpi {
    @Override
    protected SecretKey engineGenerateSecret(KeySpec keySpec) throws InvalidKeySpecException {
        if (keySpec == null) {
            throw new InvalidKeySpecException("Null KeySpec");
        }
        if (keySpec instanceof SecretKeySpec) {
            SecretKeySpec secretKeySpec = (SecretKeySpec) keySpec;
            try {
                if (!DESedeKeySpec.isParityAdjusted(secretKeySpec.getEncoded(), 0)) {
                    throw new InvalidKeySpecException("SecretKeySpec is not a parity-adjusted DESEDE key");
                }
                return secretKeySpec;
            } catch (InvalidKeyException e) {
                throw new InvalidKeySpecException(e);
            }
        }
        if (keySpec instanceof DESedeKeySpec) {
            return new SecretKeySpec(((DESedeKeySpec) keySpec).getKey(), "DESEDE");
        }
        throw new InvalidKeySpecException("Unsupported KeySpec class: " + keySpec.getClass().getName());
    }

    @Override
    protected KeySpec engineGetKeySpec(SecretKey secretKey, Class cls) throws InvalidKeySpecException {
        if (secretKey == null) {
            throw new InvalidKeySpecException("Null SecretKey");
        }
        if (cls == SecretKeySpec.class) {
            try {
                if (!DESedeKeySpec.isParityAdjusted(secretKey.getEncoded(), 0)) {
                    throw new InvalidKeySpecException("SecretKey is not a parity-adjusted DESEDE key");
                }
                if (secretKey instanceof SecretKeySpec) {
                    return (KeySpec) secretKey;
                }
                return new SecretKeySpec(secretKey.getEncoded(), "DESEDE");
            } catch (InvalidKeyException e) {
                throw new InvalidKeySpecException(e);
            }
        }
        if (cls == DESedeKeySpec.class) {
            try {
                return new DESedeKeySpec(secretKey.getEncoded());
            } catch (InvalidKeyException e2) {
                throw new InvalidKeySpecException(e2);
            }
        }
        throw new InvalidKeySpecException("Unsupported KeySpec class: " + cls);
    }

    @Override
    protected SecretKey engineTranslateKey(SecretKey secretKey) throws InvalidKeyException {
        if (secretKey == null) {
            throw new InvalidKeyException("Null SecretKey");
        }
        return new SecretKeySpec(secretKey.getEncoded(), secretKey.getAlgorithm());
    }
}
