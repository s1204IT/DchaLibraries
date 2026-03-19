package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

public class OpenSSLSignature extends SignatureSpi {

    private static final int[] f4x8e49097e = null;
    private NativeRef.EVP_MD_CTX ctx;
    private final EngineType engineType;
    private final long evpMdRef;
    private long evpPkeyCtx;
    private OpenSSLKey key;
    private boolean signing;
    private final byte[] singleByte;

    private static int[] m4xf6ed0822() {
        if (f4x8e49097e != null) {
            return f4x8e49097e;
        }
        int[] iArr = new int[EngineType.valuesCustom().length];
        try {
            iArr[EngineType.EC.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[EngineType.RSA.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f4x8e49097e = iArr;
        return iArr;
    }

    OpenSSLSignature(long evpMdRef, EngineType engineType, OpenSSLSignature openSSLSignature) {
        this(evpMdRef, engineType);
    }

    private enum EngineType {
        RSA,
        EC;

        public static EngineType[] valuesCustom() {
            return values();
        }
    }

    private OpenSSLSignature(long evpMdRef, EngineType engineType) {
        this.singleByte = new byte[1];
        this.engineType = engineType;
        this.evpMdRef = evpMdRef;
    }

    private final void resetContext() {
        NativeRef.EVP_MD_CTX ctxLocal = new NativeRef.EVP_MD_CTX(NativeCrypto.EVP_MD_CTX_create());
        if (this.signing) {
            enableDSASignatureNonceHardeningIfApplicable();
            this.evpPkeyCtx = NativeCrypto.EVP_DigestSignInit(ctxLocal, this.evpMdRef, this.key.getNativeRef());
        } else {
            this.evpPkeyCtx = NativeCrypto.EVP_DigestVerifyInit(ctxLocal, this.evpMdRef, this.key.getNativeRef());
        }
        configureEVP_PKEY_CTX(this.evpPkeyCtx);
        this.ctx = ctxLocal;
    }

    protected void configureEVP_PKEY_CTX(long ctx) {
    }

    @Override
    protected void engineUpdate(byte input) {
        this.singleByte[0] = input;
        engineUpdate(this.singleByte, 0, 1);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        NativeRef.EVP_MD_CTX ctxLocal = this.ctx;
        if (this.signing) {
            NativeCrypto.EVP_DigestSignUpdate(ctxLocal, input, offset, len);
        } else {
            NativeCrypto.EVP_DigestVerifyUpdate(ctxLocal, input, offset, len);
        }
    }

    @Override
    protected void engineUpdate(ByteBuffer input) {
        if (!input.hasRemaining()) {
            return;
        }
        if (!input.isDirect()) {
            super.engineUpdate(input);
            return;
        }
        long baseAddress = NativeCrypto.getDirectBufferAddress(input);
        if (baseAddress == 0) {
            super.engineUpdate(input);
            return;
        }
        int position = input.position();
        if (position < 0) {
            throw new RuntimeException("Negative position");
        }
        long ptr = baseAddress + ((long) position);
        int len = input.remaining();
        if (len < 0) {
            throw new RuntimeException("Negative remaining amount");
        }
        NativeRef.EVP_MD_CTX ctxLocal = this.ctx;
        if (this.signing) {
            NativeCrypto.EVP_DigestSignUpdateDirect(ctxLocal, ptr, len);
        } else {
            NativeCrypto.EVP_DigestVerifyUpdateDirect(ctxLocal, ptr, len);
        }
        input.position(position + len);
    }

    @Override
    @Deprecated
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }

    private void checkEngineType(OpenSSLKey pkey) throws InvalidKeyException {
        int pkeyType = NativeCrypto.EVP_PKEY_type(pkey.getNativeRef());
        switch (m4xf6ed0822()[this.engineType.ordinal()]) {
            case 1:
                if (pkeyType == 408) {
                    return;
                } else {
                    throw new InvalidKeyException("Signature initialized as " + this.engineType + " (not EC)");
                }
            case 2:
                if (pkeyType == 6) {
                    return;
                } else {
                    throw new InvalidKeyException("Signature initialized as " + this.engineType + " (not RSA)");
                }
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
        switch (m4xf6ed0822()[this.engineType.ordinal()]) {
            case 1:
                NativeCrypto.EC_KEY_set_nonce_from_hash(key.getNativeRef(), true);
                break;
        }
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        initInternal(OpenSSLKey.fromPublicKey(publicKey), false);
    }

    @Override
    @Deprecated
    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        NativeRef.EVP_MD_CTX ctxLocal = this.ctx;
        try {
            try {
                return NativeCrypto.EVP_DigestSignFinal(ctxLocal);
            } catch (Exception ex) {
                throw new SignatureException(ex);
            }
        } finally {
            resetContext();
        }
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        NativeRef.EVP_MD_CTX ctxLocal = this.ctx;
        try {
            try {
                return NativeCrypto.EVP_DigestVerifyFinal(ctxLocal, sigBytes, 0, sigBytes.length);
            } catch (Exception ex) {
                throw new SignatureException(ex);
            }
        } finally {
            resetContext();
        }
    }

    protected final long getEVP_PKEY_CTX() {
        return this.evpPkeyCtx;
    }

    static abstract class RSAPKCS1Padding extends OpenSSLSignature {
        RSAPKCS1Padding(long evpMdRef) {
            super(evpMdRef, EngineType.RSA, null);
        }

        @Override
        protected final void configureEVP_PKEY_CTX(long ctx) {
            NativeCrypto.EVP_PKEY_CTX_set_rsa_padding(ctx, 1);
        }
    }

    public static final class MD5RSA extends RSAPKCS1Padding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("md5");

        public MD5RSA() {
            super(EVP_MD);
        }
    }

    public static final class SHA1RSA extends RSAPKCS1Padding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha1");

        public SHA1RSA() {
            super(EVP_MD);
        }
    }

    public static final class SHA224RSA extends RSAPKCS1Padding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha224");

        public SHA224RSA() {
            super(EVP_MD);
        }
    }

    public static final class SHA256RSA extends RSAPKCS1Padding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha256");

        public SHA256RSA() {
            super(EVP_MD);
        }
    }

    public static final class SHA384RSA extends RSAPKCS1Padding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha384");

        public SHA384RSA() {
            super(EVP_MD);
        }
    }

    public static final class SHA512RSA extends RSAPKCS1Padding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha512");

        public SHA512RSA() {
            super(EVP_MD);
        }
    }

    public static final class SHA1ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha1");

        public SHA1ECDSA() {
            super(EVP_MD, EngineType.EC, null);
        }
    }

    public static final class SHA224ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha224");

        public SHA224ECDSA() {
            super(EVP_MD, EngineType.EC, null);
        }
    }

    public static final class SHA256ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha256");

        public SHA256ECDSA() {
            super(EVP_MD, EngineType.EC, null);
        }
    }

    public static final class SHA384ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha384");

        public SHA384ECDSA() {
            super(EVP_MD, EngineType.EC, null);
        }
    }

    public static final class SHA512ECDSA extends OpenSSLSignature {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha512");

        public SHA512ECDSA() {
            super(EVP_MD, EngineType.EC, null);
        }
    }

    static abstract class RSAPSSPadding extends OpenSSLSignature {
        private static final String MGF1_ALGORITHM_NAME = "MGF1";
        private static final String MGF1_OID = "1.2.840.113549.1.1.8";
        private static final int TRAILER_FIELD_BC_ID = 1;
        private final String contentDigestAlgorithm;
        private String mgf1DigestAlgorithm;
        private long mgf1EvpMdRef;
        private int saltSizeBytes;

        public RSAPSSPadding(long contentDigestEvpMdRef, String contentDigestAlgorithm, int saltSizeBytes) {
            super(contentDigestEvpMdRef, EngineType.RSA, null);
            this.contentDigestAlgorithm = contentDigestAlgorithm;
            this.mgf1DigestAlgorithm = contentDigestAlgorithm;
            this.mgf1EvpMdRef = contentDigestEvpMdRef;
            this.saltSizeBytes = saltSizeBytes;
        }

        @Override
        protected final void configureEVP_PKEY_CTX(long ctx) {
            NativeCrypto.EVP_PKEY_CTX_set_rsa_padding(ctx, 6);
            NativeCrypto.EVP_PKEY_CTX_set_rsa_mgf1_md(ctx, this.mgf1EvpMdRef);
            NativeCrypto.EVP_PKEY_CTX_set_rsa_pss_saltlen(ctx, this.saltSizeBytes);
        }

        @Override
        protected final void engineSetParameter(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
            if (!(algorithmParameterSpec instanceof PSSParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Unsupported parameter: " + algorithmParameterSpec + ". Only " + PSSParameterSpec.class.getName() + " supported");
            }
            String specContentDigest = getJcaDigestAlgorithmStandardName(algorithmParameterSpec.getDigestAlgorithm());
            if (specContentDigest == null) {
                throw new InvalidAlgorithmParameterException("Unsupported content digest algorithm: " + algorithmParameterSpec.getDigestAlgorithm());
            }
            if (!this.contentDigestAlgorithm.equalsIgnoreCase(specContentDigest)) {
                throw new InvalidAlgorithmParameterException("Changing content digest algorithm not supported");
            }
            String specMgfAlgorithm = algorithmParameterSpec.getMGFAlgorithm();
            if (!MGF1_ALGORITHM_NAME.equalsIgnoreCase(specMgfAlgorithm) && !MGF1_OID.equals(specMgfAlgorithm)) {
                throw new InvalidAlgorithmParameterException("Unsupported MGF algorithm: " + specMgfAlgorithm + ". Only " + MGF1_ALGORITHM_NAME + " supported");
            }
            AlgorithmParameterSpec mgfSpec = algorithmParameterSpec.getMGFParameters();
            if (!(mgfSpec instanceof MGF1ParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Unsupported MGF parameters: " + mgfSpec + ". Only " + MGF1ParameterSpec.class.getName() + " supported");
            }
            MGF1ParameterSpec specMgf1Spec = (MGF1ParameterSpec) algorithmParameterSpec.getMGFParameters();
            String specMgf1Digest = getJcaDigestAlgorithmStandardName(specMgf1Spec.getDigestAlgorithm());
            if (specMgf1Digest == null) {
                throw new InvalidAlgorithmParameterException("Unsupported MGF1 digest algorithm: " + specMgf1Spec.getDigestAlgorithm());
            }
            try {
                long specMgf1EvpMdRef = getEVP_MDByJcaDigestAlgorithmStandardName(specMgf1Digest);
                int specSaltSizeBytes = algorithmParameterSpec.getSaltLength();
                if (specSaltSizeBytes < 0) {
                    throw new InvalidAlgorithmParameterException("Salt length must be non-negative: " + specSaltSizeBytes);
                }
                int specTrailer = algorithmParameterSpec.getTrailerField();
                if (specTrailer != 1) {
                    throw new InvalidAlgorithmParameterException("Unsupported trailer field: " + specTrailer + ". Only 1 supported");
                }
                this.mgf1DigestAlgorithm = specMgf1Digest;
                this.mgf1EvpMdRef = specMgf1EvpMdRef;
                this.saltSizeBytes = specSaltSizeBytes;
                long ctx = getEVP_PKEY_CTX();
                if (ctx == 0) {
                    return;
                }
                configureEVP_PKEY_CTX(ctx);
            } catch (NoSuchAlgorithmException e) {
                throw new ProviderException("Failed to obtain EVP_MD for " + specMgf1Digest, e);
            }
        }

        @Override
        protected final AlgorithmParameters engineGetParameters() {
            try {
                AlgorithmParameters result = AlgorithmParameters.getInstance("PSS");
                result.init(new PSSParameterSpec(this.contentDigestAlgorithm, MGF1_ALGORITHM_NAME, new MGF1ParameterSpec(this.mgf1DigestAlgorithm), this.saltSizeBytes, 1));
                return result;
            } catch (NoSuchAlgorithmException | InvalidParameterSpecException e) {
                throw new ProviderException("Failed to create PSS AlgorithmParameters", e);
            }
        }

        private static String getJcaDigestAlgorithmStandardName(String algorithm) {
            if ("SHA-256".equalsIgnoreCase(algorithm) || "2.16.840.1.101.3.4.2.1".equals(algorithm)) {
                return "SHA-256";
            }
            if ("SHA-512".equalsIgnoreCase(algorithm) || "2.16.840.1.101.3.4.2.3".equals(algorithm)) {
                return "SHA-512";
            }
            if ("SHA-1".equalsIgnoreCase(algorithm) || "1.3.14.3.2.26".equals(algorithm)) {
                return "SHA-1";
            }
            if ("SHA-384".equalsIgnoreCase(algorithm) || "2.16.840.1.101.3.4.2.2".equals(algorithm)) {
                return "SHA-384";
            }
            if ("SHA-224".equalsIgnoreCase(algorithm) || "2.16.840.1.101.3.4.2.4".equals(algorithm)) {
                return "SHA-224";
            }
            return null;
        }

        private static long getEVP_MDByJcaDigestAlgorithmStandardName(String algorithm) throws NoSuchAlgorithmException {
            if ("SHA-256".equalsIgnoreCase(algorithm)) {
                return NativeCrypto.EVP_get_digestbyname("sha256");
            }
            if ("SHA-512".equalsIgnoreCase(algorithm)) {
                return NativeCrypto.EVP_get_digestbyname("sha512");
            }
            if ("SHA-1".equalsIgnoreCase(algorithm)) {
                return NativeCrypto.EVP_get_digestbyname("sha1");
            }
            if ("SHA-384".equalsIgnoreCase(algorithm)) {
                return NativeCrypto.EVP_get_digestbyname("sha384");
            }
            if ("SHA-224".equalsIgnoreCase(algorithm)) {
                return NativeCrypto.EVP_get_digestbyname("sha224");
            }
            throw new NoSuchAlgorithmException("Unsupported algorithm: " + algorithm);
        }
    }

    public static final class SHA1RSAPSS extends RSAPSSPadding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha1");

        public SHA1RSAPSS() {
            super(EVP_MD, "SHA-1", 20);
        }
    }

    public static final class SHA224RSAPSS extends RSAPSSPadding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha224");

        public SHA224RSAPSS() {
            super(EVP_MD, "SHA-224", 28);
        }
    }

    public static final class SHA256RSAPSS extends RSAPSSPadding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha256");

        public SHA256RSAPSS() {
            super(EVP_MD, "SHA-256", 32);
        }
    }

    public static final class SHA384RSAPSS extends RSAPSSPadding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha384");

        public SHA384RSAPSS() {
            super(EVP_MD, "SHA-384", 48);
        }
    }

    public static final class SHA512RSAPSS extends RSAPSSPadding {
        private static final long EVP_MD = NativeCrypto.EVP_get_digestbyname("sha512");

        public SHA512RSAPSS() {
            super(EVP_MD, "SHA-512", 64);
        }
    }
}
