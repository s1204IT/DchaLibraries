package com.android.org.conscrypt;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;

public class OpenSSLSignature extends SignatureSpi {
    private OpenSSLDigestContext ctx;
    private final EngineType engineType;
    private final long evpAlgorithm;
    private OpenSSLKey key;
    private boolean signing;
    private final byte[] singleByte;

    private enum EngineType {
        RSA,
        DSA,
        EC
    }

    private OpenSSLSignature(long algorithm, EngineType engineType) throws NoSuchAlgorithmException {
        this.singleByte = new byte[1];
        this.engineType = engineType;
        this.evpAlgorithm = algorithm;
    }

    private final void resetContext() {
        OpenSSLDigestContext ctxLocal = new OpenSSLDigestContext(NativeCrypto.EVP_MD_CTX_create());
        NativeCrypto.EVP_MD_CTX_init(ctxLocal);
        if (this.signing) {
            enableDSASignatureNonceHardeningIfApplicable();
            NativeCrypto.EVP_SignInit(ctxLocal, this.evpAlgorithm);
        } else {
            NativeCrypto.EVP_VerifyInit(ctxLocal, this.evpAlgorithm);
        }
        this.ctx = ctxLocal;
    }

    @Override
    protected void engineUpdate(byte input) {
        this.singleByte[0] = input;
        engineUpdate(this.singleByte, 0, 1);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        OpenSSLDigestContext ctxLocal = this.ctx;
        if (this.signing) {
            NativeCrypto.EVP_SignUpdate(ctxLocal, input, offset, len);
        } else {
            NativeCrypto.EVP_VerifyUpdate(ctxLocal, input, offset, len);
        }
    }

    @Override
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }

    private void checkEngineType(OpenSSLKey pkey) throws InvalidKeyException {
        int pkeyType = NativeCrypto.EVP_PKEY_type(pkey.getPkeyContext());
        switch (this.engineType) {
            case RSA:
                if (pkeyType != 6) {
                    throw new InvalidKeyException("Signature initialized as " + this.engineType + " (not RSA)");
                }
                return;
            case DSA:
                if (pkeyType != 116) {
                    throw new InvalidKeyException("Signature initialized as " + this.engineType + " (not DSA)");
                }
                return;
            case EC:
                if (pkeyType != 408) {
                    throw new InvalidKeyException("Signature initialized as " + this.engineType + " (not EC)");
                }
                return;
            default:
                throw new InvalidKeyException("Key must be of type " + this.engineType);
        }
    }

    private void initInternal(OpenSSLKey newKey, boolean signing) throws InvalidKeyException {
        checkEngineType(newKey);
        this.key = newKey;
        this.signing = signing;
        resetContext();
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        initInternal(OpenSSLKey.fromPrivateKey(privateKey), true);
    }

    private void enableDSASignatureNonceHardeningIfApplicable() {
        OpenSSLKey key = this.key;
        switch (this.engineType) {
            case DSA:
                NativeCrypto.set_DSA_flag_nonce_from_hash(key.getPkeyContext());
                break;
            case EC:
                NativeCrypto.EC_KEY_set_nonce_from_hash(key.getPkeyContext(), true);
                break;
        }
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        initInternal(OpenSSLKey.fromPublicKey(publicKey), false);
    }

    @Override
    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        if (this.key == null) {
            throw new SignatureException("Need DSA or RSA or EC private key");
        }
        OpenSSLDigestContext ctxLocal = this.ctx;
        try {
            try {
                byte[] buffer = new byte[NativeCrypto.EVP_PKEY_size(this.key.getPkeyContext())];
                int bytesWritten = NativeCrypto.EVP_SignFinal(ctxLocal, buffer, 0, this.key.getPkeyContext());
                byte[] signature = new byte[bytesWritten];
                System.arraycopy(buffer, 0, signature, 0, bytesWritten);
                return signature;
            } catch (Exception ex) {
                throw new SignatureException(ex);
            }
        } finally {
            resetContext();
        }
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        if (this.key == null) {
            throw new SignatureException("Need DSA or RSA public key");
        }
        try {
            int result = NativeCrypto.EVP_VerifyFinal(this.ctx, sigBytes, 0, sigBytes.length, this.key.getPkeyContext());
            return result == 1;
        } catch (Exception e) {
            return false;
        } finally {
            resetContext();
        }
    }

    public static final class MD5RSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("RSA-MD5");

        public MD5RSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.RSA);
        }
    }

    public static final class SHA1RSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("RSA-SHA1");

        public SHA1RSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.RSA);
        }
    }

    public static final class SHA224RSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("RSA-SHA224");

        public SHA224RSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.RSA);
        }
    }

    public static final class SHA256RSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("RSA-SHA256");

        public SHA256RSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.RSA);
        }
    }

    public static final class SHA384RSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("RSA-SHA384");

        public SHA384RSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.RSA);
        }
    }

    public static final class SHA512RSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("RSA-SHA512");

        public SHA512RSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.RSA);
        }
    }

    public static final class SHA1DSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("DSA-SHA1");

        public SHA1DSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.DSA);
        }
    }

    public static final class SHA1ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("SHA1");

        public SHA1ECDSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.EC);
        }
    }

    public static final class SHA224ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("SHA224");

        public SHA224ECDSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.EC);
        }
    }

    public static final class SHA256ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("SHA256");

        public SHA256ECDSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.EC);
        }
    }

    public static final class SHA384ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("SHA384");

        public SHA384ECDSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.EC);
        }
    }

    public static final class SHA512ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("SHA512");

        public SHA512ECDSA() throws NoSuchAlgorithmException {
            super(EVP_MD, EngineType.EC);
        }
    }
}
