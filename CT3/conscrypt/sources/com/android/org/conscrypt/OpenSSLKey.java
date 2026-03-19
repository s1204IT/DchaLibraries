package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class OpenSSLKey {
    private final String alias;
    private final NativeRef.EVP_PKEY ctx;
    private final OpenSSLEngine engine;
    private final boolean wrapped;

    public OpenSSLKey(long ctx) {
        this(ctx, false);
    }

    public OpenSSLKey(long ctx, boolean wrapped) {
        this.ctx = new NativeRef.EVP_PKEY(ctx);
        this.engine = null;
        this.alias = null;
        this.wrapped = wrapped;
    }

    public OpenSSLKey(long ctx, OpenSSLEngine engine, String alias) {
        this.ctx = new NativeRef.EVP_PKEY(ctx);
        this.engine = engine;
        this.alias = alias;
        this.wrapped = false;
    }

    public NativeRef.EVP_PKEY getNativeRef() {
        return this.ctx;
    }

    OpenSSLEngine getEngine() {
        return this.engine;
    }

    boolean isEngineBased() {
        return this.engine != null;
    }

    public String getAlias() {
        return this.alias;
    }

    public boolean isWrapped() {
        return this.wrapped;
    }

    public static OpenSSLKey fromPrivateKey(PrivateKey key) throws InvalidKeyException {
        if (key instanceof OpenSSLKeyHolder) {
            return ((OpenSSLKeyHolder) key).getOpenSSLKey();
        }
        String keyFormat = key.getFormat();
        if (keyFormat == null) {
            return wrapPrivateKey(key);
        }
        if (!"PKCS#8".equals(key.getFormat())) {
            throw new InvalidKeyException("Unknown key format " + keyFormat);
        }
        byte[] encoded = key.getEncoded();
        if (encoded == null) {
            throw new InvalidKeyException("Key encoding is null");
        }
        return new OpenSSLKey(NativeCrypto.d2i_PKCS8_PRIV_KEY_INFO(key.getEncoded()));
    }

    public static OpenSSLKey fromPrivateKeyPemInputStream(InputStream is) throws InvalidKeyException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is, true);
        try {
            try {
                long keyCtx = NativeCrypto.PEM_read_bio_PrivateKey(bis.getBioContext());
                if (keyCtx != 0) {
                    return new OpenSSLKey(keyCtx);
                }
                return null;
            } catch (Exception e) {
                throw new InvalidKeyException(e);
            }
        } finally {
            bis.release();
        }
    }

    public static OpenSSLKey fromPrivateKeyForTLSStackOnly(PrivateKey privateKey, PublicKey publicKey) throws InvalidKeyException {
        OpenSSLKey result = getOpenSSLKey(privateKey);
        if (result != null) {
            return result;
        }
        OpenSSLKey result2 = fromKeyMaterial(privateKey);
        if (result2 != null) {
            return result2;
        }
        return wrapJCAPrivateKeyForTLSStackOnly(privateKey, publicKey);
    }

    public static OpenSSLKey fromECPrivateKeyForTLSStackOnly(PrivateKey key, ECParameterSpec ecParams) throws InvalidKeyException {
        OpenSSLKey result = getOpenSSLKey(key);
        if (result != null) {
            return result;
        }
        OpenSSLKey result2 = fromKeyMaterial(key);
        if (result2 != null) {
            return result2;
        }
        return OpenSSLECPrivateKey.wrapJCAPrivateKeyForTLSStackOnly(key, ecParams);
    }

    private static OpenSSLKey getOpenSSLKey(PrivateKey key) {
        if (key instanceof OpenSSLKeyHolder) {
            return ((OpenSSLKeyHolder) key).getOpenSSLKey();
        }
        if ("RSA".equals(key.getAlgorithm())) {
            return Platform.wrapRsaKey(key);
        }
        return null;
    }

    private static OpenSSLKey fromKeyMaterial(PrivateKey key) {
        byte[] encoded;
        if ("PKCS#8".equals(key.getFormat()) && (encoded = key.getEncoded()) != null) {
            return new OpenSSLKey(NativeCrypto.d2i_PKCS8_PRIV_KEY_INFO(encoded));
        }
        return null;
    }

    private static OpenSSLKey wrapJCAPrivateKeyForTLSStackOnly(PrivateKey privateKey, PublicKey publicKey) throws InvalidKeyException {
        String keyAlgorithm = privateKey.getAlgorithm();
        if ("RSA".equals(keyAlgorithm)) {
            return OpenSSLRSAPrivateKey.wrapJCAPrivateKeyForTLSStackOnly(privateKey, publicKey);
        }
        if ("EC".equals(keyAlgorithm)) {
            return OpenSSLECPrivateKey.wrapJCAPrivateKeyForTLSStackOnly(privateKey, publicKey);
        }
        throw new InvalidKeyException("Unsupported key algorithm: " + keyAlgorithm);
    }

    private static OpenSSLKey wrapPrivateKey(PrivateKey key) throws InvalidKeyException {
        if (key instanceof RSAPrivateKey) {
            return OpenSSLRSAPrivateKey.wrapPlatformKey((RSAPrivateKey) key);
        }
        if (key instanceof ECPrivateKey) {
            return OpenSSLECPrivateKey.wrapPlatformKey((ECPrivateKey) key);
        }
        throw new InvalidKeyException("Unknown key type: " + key.toString());
    }

    public static OpenSSLKey fromPublicKey(PublicKey key) throws InvalidKeyException {
        if (key instanceof OpenSSLKeyHolder) {
            return ((OpenSSLKeyHolder) key).getOpenSSLKey();
        }
        if (!"X.509".equals(key.getFormat())) {
            throw new InvalidKeyException("Unknown key format " + key.getFormat());
        }
        byte[] encoded = key.getEncoded();
        if (encoded == null) {
            throw new InvalidKeyException("Key encoding is null");
        }
        try {
            return new OpenSSLKey(NativeCrypto.d2i_PUBKEY(key.getEncoded()));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    public static OpenSSLKey fromPublicKeyPemInputStream(InputStream is) throws InvalidKeyException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is, true);
        try {
            try {
                long keyCtx = NativeCrypto.PEM_read_bio_PUBKEY(bis.getBioContext());
                if (keyCtx != 0) {
                    return new OpenSSLKey(keyCtx);
                }
                return null;
            } catch (Exception e) {
                throw new InvalidKeyException(e);
            }
        } finally {
            bis.release();
        }
    }

    public PublicKey getPublicKey() throws NoSuchAlgorithmException {
        switch (NativeCrypto.EVP_PKEY_type(this.ctx)) {
            case 6:
                return new OpenSSLRSAPublicKey(this);
            case NativeConstants.EVP_PKEY_EC:
                return new OpenSSLECPublicKey(this);
            default:
                throw new NoSuchAlgorithmException("unknown PKEY type");
        }
    }

    static PublicKey getPublicKey(X509EncodedKeySpec keySpec, int type) throws InvalidKeySpecException {
        try {
            OpenSSLKey key = new OpenSSLKey(NativeCrypto.d2i_PUBKEY(keySpec.getEncoded()));
            if (NativeCrypto.EVP_PKEY_type(key.getNativeRef()) != type) {
                throw new InvalidKeySpecException("Unexpected key type");
            }
            try {
                return key.getPublicKey();
            } catch (NoSuchAlgorithmException e) {
                throw new InvalidKeySpecException(e);
            }
        } catch (Exception e2) {
            throw new InvalidKeySpecException(e2);
        }
    }

    public PrivateKey getPrivateKey() throws NoSuchAlgorithmException {
        switch (NativeCrypto.EVP_PKEY_type(this.ctx)) {
            case 6:
                return new OpenSSLRSAPrivateKey(this);
            case NativeConstants.EVP_PKEY_EC:
                return new OpenSSLECPrivateKey(this);
            default:
                throw new NoSuchAlgorithmException("unknown PKEY type");
        }
    }

    static PrivateKey getPrivateKey(PKCS8EncodedKeySpec keySpec, int type) throws InvalidKeySpecException {
        try {
            OpenSSLKey key = new OpenSSLKey(NativeCrypto.d2i_PKCS8_PRIV_KEY_INFO(keySpec.getEncoded()));
            if (NativeCrypto.EVP_PKEY_type(key.getNativeRef()) != type) {
                throw new InvalidKeySpecException("Unexpected key type");
            }
            try {
                return key.getPrivateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new InvalidKeySpecException(e);
            }
        } catch (Exception e2) {
            throw new InvalidKeySpecException(e2);
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof OpenSSLKey)) {
            return false;
        }
        OpenSSLKey other = (OpenSSLKey) o;
        if (this.ctx.equals(other.getNativeRef())) {
            return true;
        }
        if (this.engine == null) {
            if (other.getEngine() != null) {
                return false;
            }
        } else {
            if (!this.engine.equals(other.getEngine())) {
                return false;
            }
            if (this.alias != null) {
                return this.alias.equals(other.getAlias());
            }
            if (other.getAlias() != null) {
                return false;
            }
        }
        return NativeCrypto.EVP_PKEY_cmp(this.ctx, other.getNativeRef()) == 1;
    }

    public int hashCode() {
        int hash = this.ctx.hashCode() + 17;
        return (hash * 31) + ((int) (this.engine == null ? 0L : this.engine.getEngineContext()));
    }
}
