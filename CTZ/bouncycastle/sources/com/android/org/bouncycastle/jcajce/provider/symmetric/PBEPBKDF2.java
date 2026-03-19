package com.android.org.bouncycastle.jcajce.provider.symmetric;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseSecretKeyFactory;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE;
import com.android.org.bouncycastle.jcajce.provider.util.AlgorithmProvider;
import com.android.org.bouncycastle.jcajce.spec.PBKDF2KeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEKeySpec;

public class PBEPBKDF2 {
    private PBEPBKDF2() {
    }

    public static class BasePBKDF2 extends BaseSecretKeyFactory {
        private int defaultDigest;
        private int ivSizeInBits;
        private int keySizeInBits;
        private int scheme;

        public BasePBKDF2(String str, int i) {
            this(str, i, 1);
        }

        private BasePBKDF2(String str, int i, int i2, int i3, int i4) {
            super(str, PKCSObjectIdentifiers.id_PBKDF2);
            this.scheme = i;
            this.keySizeInBits = i3;
            this.ivSizeInBits = i4;
            this.defaultDigest = i2;
        }

        private BasePBKDF2(String str, int i, int i2) {
            this(str, i, i2, 0, 0);
        }

        @Override
        protected SecretKey engineGenerateSecret(KeySpec keySpec) throws InvalidKeySpecException {
            if (keySpec instanceof PBEKeySpec) {
                PBEKeySpec pBEKeySpec = (PBEKeySpec) keySpec;
                if (pBEKeySpec.getSalt() == null && pBEKeySpec.getIterationCount() == 0 && pBEKeySpec.getKeyLength() == 0 && pBEKeySpec.getPassword().length > 0 && this.keySizeInBits != 0) {
                    return new BCPBEKey(this.algName, this.algOid, this.scheme, this.defaultDigest, this.keySizeInBits, this.ivSizeInBits, pBEKeySpec, null);
                }
                if (pBEKeySpec.getSalt() == null) {
                    throw new InvalidKeySpecException("missing required salt");
                }
                if (pBEKeySpec.getIterationCount() <= 0) {
                    throw new InvalidKeySpecException("positive iteration count required: " + pBEKeySpec.getIterationCount());
                }
                if (pBEKeySpec.getKeyLength() <= 0) {
                    throw new InvalidKeySpecException("positive key length required: " + pBEKeySpec.getKeyLength());
                }
                if (pBEKeySpec.getPassword().length == 0) {
                    throw new IllegalArgumentException("password empty");
                }
                if (pBEKeySpec instanceof PBKDF2KeySpec) {
                    int digestCode = getDigestCode(((PBKDF2KeySpec) pBEKeySpec).getPrf().getAlgorithm());
                    int keyLength = pBEKeySpec.getKeyLength();
                    return new BCPBEKey(this.algName, this.algOid, this.scheme, digestCode, keyLength, -1, pBEKeySpec, PBE.Util.makePBEMacParameters(pBEKeySpec, this.scheme, digestCode, keyLength));
                }
                int i = this.defaultDigest;
                int keyLength2 = pBEKeySpec.getKeyLength();
                return new BCPBEKey(this.algName, this.algOid, this.scheme, i, keyLength2, -1, pBEKeySpec, PBE.Util.makePBEMacParameters(pBEKeySpec, this.scheme, i, keyLength2));
            }
            throw new InvalidKeySpecException("Invalid KeySpec");
        }

        private int getDigestCode(ASN1ObjectIdentifier aSN1ObjectIdentifier) throws InvalidKeySpecException {
            if (aSN1ObjectIdentifier.equals(PKCSObjectIdentifiers.id_hmacWithSHA1)) {
                return 1;
            }
            if (aSN1ObjectIdentifier.equals(PKCSObjectIdentifiers.id_hmacWithSHA256)) {
                return 4;
            }
            if (aSN1ObjectIdentifier.equals(PKCSObjectIdentifiers.id_hmacWithSHA224)) {
                return 7;
            }
            if (aSN1ObjectIdentifier.equals(PKCSObjectIdentifiers.id_hmacWithSHA384)) {
                return 8;
            }
            if (aSN1ObjectIdentifier.equals(PKCSObjectIdentifiers.id_hmacWithSHA512)) {
                return 9;
            }
            throw new InvalidKeySpecException("Invalid KeySpec: unknown PRF algorithm " + aSN1ObjectIdentifier);
        }
    }

    public static class BasePBKDF2WithHmacSHA1 extends BasePBKDF2 {
        public BasePBKDF2WithHmacSHA1(String str, int i) {
            super(str, i, 1);
        }
    }

    public static class PBKDF2WithHmacSHA1UTF8 extends BasePBKDF2WithHmacSHA1 {
        public PBKDF2WithHmacSHA1UTF8() {
            super("PBKDF2WithHmacSHA1", 5);
        }
    }

    public static class PBKDF2WithHmacSHA18BIT extends BasePBKDF2WithHmacSHA1 {
        public PBKDF2WithHmacSHA18BIT() {
            super("PBKDF2WithHmacSHA1And8bit", 1);
        }
    }

    public static class BasePBKDF2WithHmacSHA224 extends BasePBKDF2 {
        public BasePBKDF2WithHmacSHA224(String str, int i) {
            super(str, i, 7);
        }
    }

    public static class PBKDF2WithHmacSHA224UTF8 extends BasePBKDF2WithHmacSHA224 {
        public PBKDF2WithHmacSHA224UTF8() {
            super("PBKDF2WithHmacSHA224", 5);
        }
    }

    public static class BasePBKDF2WithHmacSHA256 extends BasePBKDF2 {
        public BasePBKDF2WithHmacSHA256(String str, int i) {
            super(str, i, 4);
        }
    }

    public static class PBKDF2WithHmacSHA256UTF8 extends BasePBKDF2WithHmacSHA256 {
        public PBKDF2WithHmacSHA256UTF8() {
            super("PBKDF2WithHmacSHA256", 5);
        }
    }

    public static class BasePBKDF2WithHmacSHA384 extends BasePBKDF2 {
        public BasePBKDF2WithHmacSHA384(String str, int i) {
            super(str, i, 8);
        }
    }

    public static class PBKDF2WithHmacSHA384UTF8 extends BasePBKDF2WithHmacSHA384 {
        public PBKDF2WithHmacSHA384UTF8() {
            super("PBKDF2WithHmacSHA384", 5);
        }
    }

    public static class BasePBKDF2WithHmacSHA512 extends BasePBKDF2 {
        public BasePBKDF2WithHmacSHA512(String str, int i) {
            super(str, i, 9);
        }
    }

    public static class PBKDF2WithHmacSHA512UTF8 extends BasePBKDF2WithHmacSHA512 {
        public PBKDF2WithHmacSHA512UTF8() {
            super("PBKDF2WithHmacSHA512", 5);
        }
    }

    public static class PBEWithHmacSHA1AndAES_128 extends BasePBKDF2 {
        public PBEWithHmacSHA1AndAES_128() {
            super("PBEWithHmacSHA1AndAES_128", 5, 1, 128, 128);
        }
    }

    public static class PBEWithHmacSHA224AndAES_128 extends BasePBKDF2 {
        public PBEWithHmacSHA224AndAES_128() {
            super("PBEWithHmacSHA224AndAES_128", 5, 7, 128, 128);
        }
    }

    public static class PBEWithHmacSHA256AndAES_128 extends BasePBKDF2 {
        public PBEWithHmacSHA256AndAES_128() {
            super("PBEWithHmacSHA256AndAES_128", 5, 4, 128, 128);
        }
    }

    public static class PBEWithHmacSHA384AndAES_128 extends BasePBKDF2 {
        public PBEWithHmacSHA384AndAES_128() {
            super("PBEWithHmacSHA384AndAES_128", 5, 8, 128, 128);
        }
    }

    public static class PBEWithHmacSHA512AndAES_128 extends BasePBKDF2 {
        public PBEWithHmacSHA512AndAES_128() {
            super("PBEWithHmacSHA512AndAES_128", 5, 9, 128, 128);
        }
    }

    public static class PBEWithHmacSHA1AndAES_256 extends BasePBKDF2 {
        public PBEWithHmacSHA1AndAES_256() {
            super("PBEWithHmacSHA1AndAES_256", 5, 1, 256, 128);
        }
    }

    public static class PBEWithHmacSHA224AndAES_256 extends BasePBKDF2 {
        public PBEWithHmacSHA224AndAES_256() {
            super("PBEWithHmacSHA224AndAES_256", 5, 7, 256, 128);
        }
    }

    public static class PBEWithHmacSHA256AndAES_256 extends BasePBKDF2 {
        public PBEWithHmacSHA256AndAES_256() {
            super("PBEWithHmacSHA256AndAES_256", 5, 4, 256, 128);
        }
    }

    public static class PBEWithHmacSHA384AndAES_256 extends BasePBKDF2 {
        public PBEWithHmacSHA384AndAES_256() {
            super("PBEWithHmacSHA384AndAES_256", 5, 8, 256, 128);
        }
    }

    public static class PBEWithHmacSHA512AndAES_256 extends BasePBKDF2 {
        public PBEWithHmacSHA512AndAES_256() {
            super("PBEWithHmacSHA512AndAES_256", 5, 9, 256, 128);
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PREFIX = PBEPBKDF2.class.getName();

        @Override
        public void configure(ConfigurableProvider configurableProvider) {
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBKDF2WithHmacSHA1AndUTF8", "PBKDF2WithHmacSHA1");
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBKDF2with8BIT", "PBKDF2WithHmacSHA1And8BIT");
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBKDF2withASCII", "PBKDF2WithHmacSHA1And8BIT");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBKDF2WithHmacSHA1", PREFIX + "$PBKDF2WithHmacSHA1UTF8");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBKDF2WithHmacSHA224", PREFIX + "$PBKDF2WithHmacSHA224UTF8");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBKDF2WithHmacSHA256", PREFIX + "$PBKDF2WithHmacSHA256UTF8");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBKDF2WithHmacSHA384", PREFIX + "$PBKDF2WithHmacSHA384UTF8");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBKDF2WithHmacSHA512", PREFIX + "$PBKDF2WithHmacSHA512UTF8");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA1AndAES_128", PREFIX + "$PBEWithHmacSHA1AndAES_128");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA224AndAES_128", PREFIX + "$PBEWithHmacSHA224AndAES_128");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA256AndAES_128", PREFIX + "$PBEWithHmacSHA256AndAES_128");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA384AndAES_128", PREFIX + "$PBEWithHmacSHA384AndAES_128");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA512AndAES_128", PREFIX + "$PBEWithHmacSHA512AndAES_128");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA1AndAES_256", PREFIX + "$PBEWithHmacSHA1AndAES_256");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA224AndAES_256", PREFIX + "$PBEWithHmacSHA224AndAES_256");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA256AndAES_256", PREFIX + "$PBEWithHmacSHA256AndAES_256");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA384AndAES_256", PREFIX + "$PBEWithHmacSHA384AndAES_256");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWithHmacSHA512AndAES_256", PREFIX + "$PBEWithHmacSHA512AndAES_256");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBKDF2WithHmacSHA1And8BIT", PREFIX + "$PBKDF2WithHmacSHA18BIT");
        }
    }
}
