package com.android.org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyFactory extends KeyFactorySpi {
    @Override
    protected java.security.PrivateKey engineGeneratePrivate(java.security.spec.KeySpec r7) throws java.security.spec.InvalidKeySpecException {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.jcajce.provider.asymmetric.x509.KeyFactory.engineGeneratePrivate(java.security.spec.KeySpec):java.security.PrivateKey");
    }

    @Override
    protected java.security.PublicKey engineGeneratePublic(java.security.spec.KeySpec r7) throws java.security.spec.InvalidKeySpecException {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.jcajce.provider.asymmetric.x509.KeyFactory.engineGeneratePublic(java.security.spec.KeySpec):java.security.PublicKey");
    }

    @Override
    protected KeySpec engineGetKeySpec(Key key, Class keySpec) throws InvalidKeySpecException {
        if (keySpec.isAssignableFrom(PKCS8EncodedKeySpec.class) && key.getFormat().equals("PKCS#8")) {
            return new PKCS8EncodedKeySpec(key.getEncoded());
        }
        if (keySpec.isAssignableFrom(X509EncodedKeySpec.class) && key.getFormat().equals("X.509")) {
            return new X509EncodedKeySpec(key.getEncoded());
        }
        throw new InvalidKeySpecException("not implemented yet " + key + " " + keySpec);
    }

    @Override
    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        throw new InvalidKeyException("not implemented yet " + key);
    }
}
