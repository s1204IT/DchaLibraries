package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.PBEParametersGenerator;
import com.android.org.bouncycastle.crypto.digests.AndroidDigestFactory;
import com.android.org.bouncycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import com.android.org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import com.android.org.bouncycastle.crypto.generators.PKCS5S1ParametersGenerator;
import com.android.org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import com.android.org.bouncycastle.crypto.params.DESParameters;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public interface PBE {
    public static final int MD5 = 0;
    public static final int OPENSSL = 3;
    public static final int PKCS12 = 2;
    public static final int PKCS5S1 = 0;
    public static final int PKCS5S1_UTF8 = 4;
    public static final int PKCS5S2 = 1;
    public static final int PKCS5S2_UTF8 = 5;
    public static final int SHA1 = 1;
    public static final int SHA224 = 7;
    public static final int SHA256 = 4;
    public static final int SHA384 = 8;
    public static final int SHA512 = 9;

    public static class Util {
        private static PBEParametersGenerator makePBEGenerator(int i, int i2) {
            if (i == 0 || i == 4) {
                switch (i2) {
                    case 0:
                        return new PKCS5S1ParametersGenerator(AndroidDigestFactory.getMD5());
                    case 1:
                        return new PKCS5S1ParametersGenerator(AndroidDigestFactory.getSHA1());
                    default:
                        throw new IllegalStateException("PKCS5 scheme 1 only supports MD2, MD5 and SHA1.");
                }
            }
            if (i == 1 || i == 5) {
                switch (i2) {
                    case 0:
                        return new PKCS5S2ParametersGenerator(AndroidDigestFactory.getMD5());
                    case 1:
                        return new PKCS5S2ParametersGenerator(AndroidDigestFactory.getSHA1());
                    case 2:
                    case 3:
                    case 5:
                    case 6:
                    default:
                        throw new IllegalStateException("unknown digest scheme for PBE PKCS5S2 encryption.");
                    case 4:
                        return new PKCS5S2ParametersGenerator(AndroidDigestFactory.getSHA256());
                    case 7:
                        return new PKCS5S2ParametersGenerator(AndroidDigestFactory.getSHA224());
                    case 8:
                        return new PKCS5S2ParametersGenerator(AndroidDigestFactory.getSHA384());
                    case 9:
                        return new PKCS5S2ParametersGenerator(AndroidDigestFactory.getSHA512());
                }
            }
            if (i == 2) {
                switch (i2) {
                    case 0:
                        return new PKCS12ParametersGenerator(AndroidDigestFactory.getMD5());
                    case 1:
                        return new PKCS12ParametersGenerator(AndroidDigestFactory.getSHA1());
                    case 2:
                    case 3:
                    case 5:
                    case 6:
                    default:
                        throw new IllegalStateException("unknown digest scheme for PBE encryption.");
                    case 4:
                        return new PKCS12ParametersGenerator(AndroidDigestFactory.getSHA256());
                    case 7:
                        return new PKCS12ParametersGenerator(AndroidDigestFactory.getSHA224());
                    case 8:
                        return new PKCS12ParametersGenerator(AndroidDigestFactory.getSHA384());
                    case 9:
                        return new PKCS12ParametersGenerator(AndroidDigestFactory.getSHA512());
                }
            }
            return new OpenSSLPBEParametersGenerator();
        }

        public static CipherParameters makePBEParameters(byte[] bArr, int i, int i2, int i3, int i4, AlgorithmParameterSpec algorithmParameterSpec, String str) throws InvalidAlgorithmParameterException {
            CipherParameters cipherParametersGenerateDerivedParameters;
            if (algorithmParameterSpec == null || !(algorithmParameterSpec instanceof PBEParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Need a PBEParameter spec with a PBE key.");
            }
            PBEParameterSpec pBEParameterSpec = (PBEParameterSpec) algorithmParameterSpec;
            PBEParametersGenerator pBEParametersGeneratorMakePBEGenerator = makePBEGenerator(i, i2);
            pBEParametersGeneratorMakePBEGenerator.init(bArr, pBEParameterSpec.getSalt(), pBEParameterSpec.getIterationCount());
            if (i4 != 0) {
                cipherParametersGenerateDerivedParameters = pBEParametersGeneratorMakePBEGenerator.generateDerivedParameters(i3, i4);
                AlgorithmParameterSpec parameterSpecFromPBEParameterSpec = getParameterSpecFromPBEParameterSpec(pBEParameterSpec);
                if ((i == 1 || i == 5) && (parameterSpecFromPBEParameterSpec instanceof IvParameterSpec)) {
                    cipherParametersGenerateDerivedParameters = new ParametersWithIV((KeyParameter) ((ParametersWithIV) cipherParametersGenerateDerivedParameters).getParameters(), ((IvParameterSpec) parameterSpecFromPBEParameterSpec).getIV());
                }
            } else {
                cipherParametersGenerateDerivedParameters = pBEParametersGeneratorMakePBEGenerator.generateDerivedParameters(i3);
            }
            if (str.startsWith("DES")) {
                if (cipherParametersGenerateDerivedParameters instanceof ParametersWithIV) {
                    DESParameters.setOddParity(((KeyParameter) ((ParametersWithIV) cipherParametersGenerateDerivedParameters).getParameters()).getKey());
                } else {
                    DESParameters.setOddParity(((KeyParameter) cipherParametersGenerateDerivedParameters).getKey());
                }
            }
            return cipherParametersGenerateDerivedParameters;
        }

        public static CipherParameters makePBEParameters(BCPBEKey bCPBEKey, AlgorithmParameterSpec algorithmParameterSpec, String str) {
            CipherParameters cipherParametersGenerateDerivedParameters;
            if (algorithmParameterSpec == null || !(algorithmParameterSpec instanceof PBEParameterSpec)) {
                throw new IllegalArgumentException("Need a PBEParameter spec with a PBE key.");
            }
            PBEParameterSpec pBEParameterSpec = (PBEParameterSpec) algorithmParameterSpec;
            PBEParametersGenerator pBEParametersGeneratorMakePBEGenerator = makePBEGenerator(bCPBEKey.getType(), bCPBEKey.getDigest());
            byte[] encoded = bCPBEKey.getEncoded();
            if (bCPBEKey.shouldTryWrongPKCS12()) {
                encoded = new byte[2];
            }
            pBEParametersGeneratorMakePBEGenerator.init(encoded, pBEParameterSpec.getSalt(), pBEParameterSpec.getIterationCount());
            if (bCPBEKey.getIvSize() != 0) {
                CipherParameters cipherParametersGenerateDerivedParameters2 = pBEParametersGeneratorMakePBEGenerator.generateDerivedParameters(bCPBEKey.getKeySize(), bCPBEKey.getIvSize());
                AlgorithmParameterSpec parameterSpecFromPBEParameterSpec = getParameterSpecFromPBEParameterSpec(pBEParameterSpec);
                if ((bCPBEKey.getType() == 1 || bCPBEKey.getType() == 5) && (parameterSpecFromPBEParameterSpec instanceof IvParameterSpec)) {
                    cipherParametersGenerateDerivedParameters = new ParametersWithIV((KeyParameter) ((ParametersWithIV) cipherParametersGenerateDerivedParameters2).getParameters(), ((IvParameterSpec) parameterSpecFromPBEParameterSpec).getIV());
                } else {
                    cipherParametersGenerateDerivedParameters = cipherParametersGenerateDerivedParameters2;
                }
            } else {
                cipherParametersGenerateDerivedParameters = pBEParametersGeneratorMakePBEGenerator.generateDerivedParameters(bCPBEKey.getKeySize());
            }
            if (str.startsWith("DES")) {
                if (cipherParametersGenerateDerivedParameters instanceof ParametersWithIV) {
                    DESParameters.setOddParity(((KeyParameter) ((ParametersWithIV) cipherParametersGenerateDerivedParameters).getParameters()).getKey());
                } else {
                    DESParameters.setOddParity(((KeyParameter) cipherParametersGenerateDerivedParameters).getKey());
                }
            }
            return cipherParametersGenerateDerivedParameters;
        }

        public static CipherParameters makePBEMacParameters(BCPBEKey bCPBEKey, AlgorithmParameterSpec algorithmParameterSpec) {
            if (algorithmParameterSpec == null || !(algorithmParameterSpec instanceof PBEParameterSpec)) {
                throw new IllegalArgumentException("Need a PBEParameter spec with a PBE key.");
            }
            PBEParameterSpec pBEParameterSpec = (PBEParameterSpec) algorithmParameterSpec;
            PBEParametersGenerator pBEParametersGeneratorMakePBEGenerator = makePBEGenerator(bCPBEKey.getType(), bCPBEKey.getDigest());
            pBEParametersGeneratorMakePBEGenerator.init(bCPBEKey.getEncoded(), pBEParameterSpec.getSalt(), pBEParameterSpec.getIterationCount());
            return pBEParametersGeneratorMakePBEGenerator.generateDerivedMacParameters(bCPBEKey.getKeySize());
        }

        public static CipherParameters makePBEMacParameters(PBEKeySpec pBEKeySpec, int i, int i2, int i3) {
            PBEParametersGenerator pBEParametersGeneratorMakePBEGenerator = makePBEGenerator(i, i2);
            byte[] bArrConvertPassword = convertPassword(i, pBEKeySpec);
            pBEParametersGeneratorMakePBEGenerator.init(bArrConvertPassword, pBEKeySpec.getSalt(), pBEKeySpec.getIterationCount());
            CipherParameters cipherParametersGenerateDerivedMacParameters = pBEParametersGeneratorMakePBEGenerator.generateDerivedMacParameters(i3);
            for (int i4 = 0; i4 != bArrConvertPassword.length; i4++) {
                bArrConvertPassword[i4] = 0;
            }
            return cipherParametersGenerateDerivedMacParameters;
        }

        public static CipherParameters makePBEParameters(PBEKeySpec pBEKeySpec, int i, int i2, int i3, int i4) {
            CipherParameters cipherParametersGenerateDerivedParameters;
            PBEParametersGenerator pBEParametersGeneratorMakePBEGenerator = makePBEGenerator(i, i2);
            byte[] bArrConvertPassword = convertPassword(i, pBEKeySpec);
            pBEParametersGeneratorMakePBEGenerator.init(bArrConvertPassword, pBEKeySpec.getSalt(), pBEKeySpec.getIterationCount());
            if (i4 != 0) {
                cipherParametersGenerateDerivedParameters = pBEParametersGeneratorMakePBEGenerator.generateDerivedParameters(i3, i4);
            } else {
                cipherParametersGenerateDerivedParameters = pBEParametersGeneratorMakePBEGenerator.generateDerivedParameters(i3);
            }
            for (int i5 = 0; i5 != bArrConvertPassword.length; i5++) {
                bArrConvertPassword[i5] = 0;
            }
            return cipherParametersGenerateDerivedParameters;
        }

        public static CipherParameters makePBEMacParameters(SecretKey secretKey, int i, int i2, int i3, PBEParameterSpec pBEParameterSpec) {
            PBEParametersGenerator pBEParametersGeneratorMakePBEGenerator = makePBEGenerator(i, i2);
            byte[] encoded = secretKey.getEncoded();
            pBEParametersGeneratorMakePBEGenerator.init(secretKey.getEncoded(), pBEParameterSpec.getSalt(), pBEParameterSpec.getIterationCount());
            CipherParameters cipherParametersGenerateDerivedMacParameters = pBEParametersGeneratorMakePBEGenerator.generateDerivedMacParameters(i3);
            for (int i4 = 0; i4 != encoded.length; i4++) {
                encoded[i4] = 0;
            }
            return cipherParametersGenerateDerivedMacParameters;
        }

        public static AlgorithmParameterSpec getParameterSpecFromPBEParameterSpec(PBEParameterSpec pBEParameterSpec) {
            try {
                return (AlgorithmParameterSpec) PBE.class.getClassLoader().loadClass("javax.crypto.spec.PBEParameterSpec").getMethod("getParameterSpec", new Class[0]).invoke(pBEParameterSpec, new Object[0]);
            } catch (Exception e) {
                return null;
            }
        }

        private static byte[] convertPassword(int i, PBEKeySpec pBEKeySpec) {
            if (i == 2) {
                return PBEParametersGenerator.PKCS12PasswordToBytes(pBEKeySpec.getPassword());
            }
            if (i == 5 || i == 4) {
                return PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(pBEKeySpec.getPassword());
            }
            return PBEParametersGenerator.PKCS5PasswordToBytes(pBEKeySpec.getPassword());
        }
    }
}
