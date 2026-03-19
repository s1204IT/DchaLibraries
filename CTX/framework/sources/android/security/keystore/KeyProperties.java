package android.security.keystore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import libcore.util.EmptyArray;

public abstract class KeyProperties {
    public static final String BLOCK_MODE_CBC = "CBC";
    public static final String BLOCK_MODE_CTR = "CTR";
    public static final String BLOCK_MODE_ECB = "ECB";
    public static final String BLOCK_MODE_GCM = "GCM";
    public static final String DIGEST_MD5 = "MD5";
    public static final String DIGEST_NONE = "NONE";
    public static final String DIGEST_SHA1 = "SHA-1";
    public static final String DIGEST_SHA224 = "SHA-224";
    public static final String DIGEST_SHA256 = "SHA-256";
    public static final String DIGEST_SHA384 = "SHA-384";
    public static final String DIGEST_SHA512 = "SHA-512";
    public static final String ENCRYPTION_PADDING_NONE = "NoPadding";
    public static final String ENCRYPTION_PADDING_PKCS7 = "PKCS7Padding";
    public static final String ENCRYPTION_PADDING_RSA_OAEP = "OAEPPadding";
    public static final String ENCRYPTION_PADDING_RSA_PKCS1 = "PKCS1Padding";

    @Deprecated
    public static final String KEY_ALGORITHM_3DES = "DESede";
    public static final String KEY_ALGORITHM_AES = "AES";
    public static final String KEY_ALGORITHM_EC = "EC";
    public static final String KEY_ALGORITHM_HMAC_SHA1 = "HmacSHA1";
    public static final String KEY_ALGORITHM_HMAC_SHA224 = "HmacSHA224";
    public static final String KEY_ALGORITHM_HMAC_SHA256 = "HmacSHA256";
    public static final String KEY_ALGORITHM_HMAC_SHA384 = "HmacSHA384";
    public static final String KEY_ALGORITHM_HMAC_SHA512 = "HmacSHA512";
    public static final String KEY_ALGORITHM_RSA = "RSA";
    public static final int ORIGIN_GENERATED = 1;
    public static final int ORIGIN_IMPORTED = 2;
    public static final int ORIGIN_SECURELY_IMPORTED = 8;
    public static final int ORIGIN_UNKNOWN = 4;
    public static final int PURPOSE_DECRYPT = 2;
    public static final int PURPOSE_ENCRYPT = 1;
    public static final int PURPOSE_SIGN = 4;
    public static final int PURPOSE_VERIFY = 8;
    public static final int PURPOSE_WRAP_KEY = 32;
    public static final String SIGNATURE_PADDING_RSA_PKCS1 = "PKCS1";
    public static final String SIGNATURE_PADDING_RSA_PSS = "PSS";

    @Retention(RetentionPolicy.SOURCE)
    public @interface BlockModeEnum {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface DigestEnum {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface EncryptionPaddingEnum {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface KeyAlgorithmEnum {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface OriginEnum {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PurposeEnum {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SignaturePaddingEnum {
    }

    private KeyProperties() {
    }

    public static abstract class Purpose {
        private Purpose() {
        }

        public static int toKeymaster(int i) {
            if (i == 4) {
                return 2;
            }
            if (i == 8) {
                return 3;
            }
            if (i != 32) {
                switch (i) {
                    case 1:
                        return 0;
                    case 2:
                        return 1;
                    default:
                        throw new IllegalArgumentException("Unknown purpose: " + i);
                }
            }
            return 5;
        }

        public static int fromKeymaster(int i) {
            if (i != 5) {
                switch (i) {
                    case 0:
                        return 1;
                    case 1:
                        return 2;
                    case 2:
                        return 4;
                    case 3:
                        return 8;
                    default:
                        throw new IllegalArgumentException("Unknown purpose: " + i);
                }
            }
            return 32;
        }

        public static int[] allToKeymaster(int i) {
            int[] setFlags = KeyProperties.getSetFlags(i);
            for (int i2 = 0; i2 < setFlags.length; i2++) {
                setFlags[i2] = toKeymaster(setFlags[i2]);
            }
            return setFlags;
        }

        public static int allFromKeymaster(Collection<Integer> collection) {
            Iterator<Integer> it = collection.iterator();
            int iFromKeymaster = 0;
            while (it.hasNext()) {
                iFromKeymaster |= fromKeymaster(it.next().intValue());
            }
            return iFromKeymaster;
        }
    }

    public static abstract class KeyAlgorithm {
        private KeyAlgorithm() {
        }

        public static int toKeymasterAsymmetricKeyAlgorithm(String str) {
            if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(str)) {
                return 3;
            }
            if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(str)) {
                return 1;
            }
            throw new IllegalArgumentException("Unsupported key algorithm: " + str);
        }

        public static String fromKeymasterAsymmetricKeyAlgorithm(int i) {
            if (i == 1) {
                return KeyProperties.KEY_ALGORITHM_RSA;
            }
            if (i == 3) {
                return KeyProperties.KEY_ALGORITHM_EC;
            }
            throw new IllegalArgumentException("Unsupported key algorithm: " + i);
        }

        public static int toKeymasterSecretKeyAlgorithm(String str) {
            if ("AES".equalsIgnoreCase(str)) {
                return 32;
            }
            if (KeyProperties.KEY_ALGORITHM_3DES.equalsIgnoreCase(str)) {
                return 33;
            }
            if (str.toUpperCase(Locale.US).startsWith("HMAC")) {
                return 128;
            }
            throw new IllegalArgumentException("Unsupported secret key algorithm: " + str);
        }

        public static String fromKeymasterSecretKeyAlgorithm(int i, int i2) {
            if (i != 128) {
                switch (i) {
                    case 32:
                        return "AES";
                    case 33:
                        return KeyProperties.KEY_ALGORITHM_3DES;
                    default:
                        throw new IllegalArgumentException("Unsupported key algorithm: " + i);
                }
            }
            switch (i2) {
                case 2:
                    return KeyProperties.KEY_ALGORITHM_HMAC_SHA1;
                case 3:
                    return KeyProperties.KEY_ALGORITHM_HMAC_SHA224;
                case 4:
                    return KeyProperties.KEY_ALGORITHM_HMAC_SHA256;
                case 5:
                    return KeyProperties.KEY_ALGORITHM_HMAC_SHA384;
                case 6:
                    return KeyProperties.KEY_ALGORITHM_HMAC_SHA512;
                default:
                    throw new IllegalArgumentException("Unsupported HMAC digest: " + Digest.fromKeymaster(i2));
            }
        }

        public static int toKeymasterDigest(String str) {
            String strSubstring;
            String upperCase = str.toUpperCase(Locale.US);
            if (!upperCase.startsWith("HMAC")) {
                return -1;
            }
            strSubstring = upperCase.substring("HMAC".length());
            switch (strSubstring) {
                case "SHA1":
                    return 2;
                case "SHA224":
                    return 3;
                case "SHA256":
                    return 4;
                case "SHA384":
                    return 5;
                case "SHA512":
                    return 6;
                default:
                    throw new IllegalArgumentException("Unsupported HMAC digest: " + strSubstring);
            }
        }
    }

    public static abstract class BlockMode {
        private BlockMode() {
        }

        public static int toKeymaster(String str) {
            if (KeyProperties.BLOCK_MODE_ECB.equalsIgnoreCase(str)) {
                return 1;
            }
            if (KeyProperties.BLOCK_MODE_CBC.equalsIgnoreCase(str)) {
                return 2;
            }
            if (KeyProperties.BLOCK_MODE_CTR.equalsIgnoreCase(str)) {
                return 3;
            }
            if (KeyProperties.BLOCK_MODE_GCM.equalsIgnoreCase(str)) {
                return 32;
            }
            throw new IllegalArgumentException("Unsupported block mode: " + str);
        }

        public static String fromKeymaster(int i) {
            if (i != 32) {
                switch (i) {
                    case 1:
                        return KeyProperties.BLOCK_MODE_ECB;
                    case 2:
                        return KeyProperties.BLOCK_MODE_CBC;
                    case 3:
                        return KeyProperties.BLOCK_MODE_CTR;
                    default:
                        throw new IllegalArgumentException("Unsupported block mode: " + i);
                }
            }
            return KeyProperties.BLOCK_MODE_GCM;
        }

        public static String[] allFromKeymaster(Collection<Integer> collection) {
            if (collection == null || collection.isEmpty()) {
                return EmptyArray.STRING;
            }
            String[] strArr = new String[collection.size()];
            int i = 0;
            Iterator<Integer> it = collection.iterator();
            while (it.hasNext()) {
                strArr[i] = fromKeymaster(it.next().intValue());
                i++;
            }
            return strArr;
        }

        public static int[] allToKeymaster(String[] strArr) {
            if (strArr == null || strArr.length == 0) {
                return EmptyArray.INT;
            }
            int[] iArr = new int[strArr.length];
            for (int i = 0; i < strArr.length; i++) {
                iArr[i] = toKeymaster(strArr[i]);
            }
            return iArr;
        }
    }

    public static abstract class EncryptionPadding {
        private EncryptionPadding() {
        }

        public static int toKeymaster(String str) {
            if (KeyProperties.ENCRYPTION_PADDING_NONE.equalsIgnoreCase(str)) {
                return 1;
            }
            if (KeyProperties.ENCRYPTION_PADDING_PKCS7.equalsIgnoreCase(str)) {
                return 64;
            }
            if (KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1.equalsIgnoreCase(str)) {
                return 4;
            }
            if (KeyProperties.ENCRYPTION_PADDING_RSA_OAEP.equalsIgnoreCase(str)) {
                return 2;
            }
            throw new IllegalArgumentException("Unsupported encryption padding scheme: " + str);
        }

        public static String fromKeymaster(int i) {
            if (i == 4) {
                return KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
            }
            if (i != 64) {
                switch (i) {
                    case 1:
                        return KeyProperties.ENCRYPTION_PADDING_NONE;
                    case 2:
                        return KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
                    default:
                        throw new IllegalArgumentException("Unsupported encryption padding: " + i);
                }
            }
            return KeyProperties.ENCRYPTION_PADDING_PKCS7;
        }

        public static int[] allToKeymaster(String[] strArr) {
            if (strArr == null || strArr.length == 0) {
                return EmptyArray.INT;
            }
            int[] iArr = new int[strArr.length];
            for (int i = 0; i < strArr.length; i++) {
                iArr[i] = toKeymaster(strArr[i]);
            }
            return iArr;
        }
    }

    static abstract class SignaturePadding {
        private SignaturePadding() {
        }

        static int toKeymaster(String str) {
            byte b;
            String upperCase = str.toUpperCase(Locale.US);
            int iHashCode = upperCase.hashCode();
            if (iHashCode != 79536) {
                b = (iHashCode == 76183014 && upperCase.equals(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)) ? (byte) 0 : (byte) -1;
            } else if (upperCase.equals(KeyProperties.SIGNATURE_PADDING_RSA_PSS)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    return 5;
                case 1:
                    return 3;
                default:
                    throw new IllegalArgumentException("Unsupported signature padding scheme: " + str);
            }
        }

        static String fromKeymaster(int i) {
            if (i == 3) {
                return KeyProperties.SIGNATURE_PADDING_RSA_PSS;
            }
            if (i == 5) {
                return KeyProperties.SIGNATURE_PADDING_RSA_PKCS1;
            }
            throw new IllegalArgumentException("Unsupported signature padding: " + i);
        }

        static int[] allToKeymaster(String[] strArr) {
            if (strArr == null || strArr.length == 0) {
                return EmptyArray.INT;
            }
            int[] iArr = new int[strArr.length];
            for (int i = 0; i < strArr.length; i++) {
                iArr[i] = toKeymaster(strArr[i]);
            }
            return iArr;
        }
    }

    public static abstract class Digest {
        private Digest() {
        }

        public static int toKeymaster(String str) {
            switch (str.toUpperCase(Locale.US)) {
                case "SHA-1":
                    return 2;
                case "SHA-224":
                    return 3;
                case "SHA-256":
                    return 4;
                case "SHA-384":
                    return 5;
                case "SHA-512":
                    return 6;
                case "NONE":
                    return 0;
                case "MD5":
                    return 1;
                default:
                    throw new IllegalArgumentException("Unsupported digest algorithm: " + str);
            }
        }

        public static String fromKeymaster(int i) {
            switch (i) {
                case 0:
                    return KeyProperties.DIGEST_NONE;
                case 1:
                    return KeyProperties.DIGEST_MD5;
                case 2:
                    return KeyProperties.DIGEST_SHA1;
                case 3:
                    return KeyProperties.DIGEST_SHA224;
                case 4:
                    return KeyProperties.DIGEST_SHA256;
                case 5:
                    return KeyProperties.DIGEST_SHA384;
                case 6:
                    return KeyProperties.DIGEST_SHA512;
                default:
                    throw new IllegalArgumentException("Unsupported digest algorithm: " + i);
            }
        }

        public static String fromKeymasterToSignatureAlgorithmDigest(int i) {
            switch (i) {
                case 0:
                    return KeyProperties.DIGEST_NONE;
                case 1:
                    return KeyProperties.DIGEST_MD5;
                case 2:
                    return "SHA1";
                case 3:
                    return "SHA224";
                case 4:
                    return "SHA256";
                case 5:
                    return "SHA384";
                case 6:
                    return "SHA512";
                default:
                    throw new IllegalArgumentException("Unsupported digest algorithm: " + i);
            }
        }

        public static String[] allFromKeymaster(Collection<Integer> collection) {
            if (collection.isEmpty()) {
                return EmptyArray.STRING;
            }
            String[] strArr = new String[collection.size()];
            int i = 0;
            Iterator<Integer> it = collection.iterator();
            while (it.hasNext()) {
                strArr[i] = fromKeymaster(it.next().intValue());
                i++;
            }
            return strArr;
        }

        public static int[] allToKeymaster(String[] strArr) {
            if (strArr == null || strArr.length == 0) {
                return EmptyArray.INT;
            }
            int[] iArr = new int[strArr.length];
            int i = 0;
            for (String str : strArr) {
                iArr[i] = toKeymaster(str);
                i++;
            }
            return iArr;
        }
    }

    public static abstract class Origin {
        private Origin() {
        }

        public static int fromKeymaster(int i) {
            if (i == 0) {
                return 1;
            }
            switch (i) {
                case 2:
                    return 2;
                case 3:
                    return 4;
                case 4:
                    return 8;
                default:
                    throw new IllegalArgumentException("Unknown origin: " + i);
            }
        }
    }

    private static int[] getSetFlags(int i) {
        if (i == 0) {
            return EmptyArray.INT;
        }
        int[] iArr = new int[getSetBitCount(i)];
        int i2 = 0;
        int i3 = 1;
        while (i != 0) {
            if ((i & 1) != 0) {
                iArr[i2] = i3;
                i2++;
            }
            i >>>= 1;
            i3 <<= 1;
        }
        return iArr;
    }

    private static int getSetBitCount(int i) {
        int i2 = 0;
        if (i == 0) {
            return 0;
        }
        while (i != 0) {
            if ((i & 1) != 0) {
                i2++;
            }
            i >>>= 1;
        }
        return i2;
    }
}
