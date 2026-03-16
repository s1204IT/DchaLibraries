package javax.crypto;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import org.apache.harmony.security.asn1.ASN1Any;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1SetOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.utils.AlgNameMapper;
import org.apache.harmony.security.x509.AlgorithmIdentifier;

public class EncryptedPrivateKeyInfo {
    private String algName;
    private final AlgorithmParameters algParameters;
    private volatile byte[] encoded;
    private final byte[] encryptedData;
    private String oid;
    private static final byte[] nullParam = {5, 0};
    private static final ASN1Sequence asn1 = new ASN1Sequence(new ASN1Type[]{AlgorithmIdentifier.ASN1, ASN1OctetString.getInstance()}) {
        @Override
        protected void getValues(Object object, Object[] values) {
            EncryptedPrivateKeyInfo epki = (EncryptedPrivateKeyInfo) object;
            try {
                byte[] algParmsEncoded = epki.algParameters == null ? EncryptedPrivateKeyInfo.nullParam : epki.algParameters.getEncoded();
                values[0] = new AlgorithmIdentifier(epki.oid, algParmsEncoded);
                values[1] = epki.encryptedData;
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    };
    private static final ASN1SetOf ASN1Attributes = new ASN1SetOf(ASN1Any.getInstance());
    private static final ASN1Sequence ASN1PrivateKeyInfo = new ASN1Sequence(new ASN1Type[]{ASN1Integer.getInstance(), AlgorithmIdentifier.ASN1, ASN1OctetString.getInstance(), new ASN1Implicit(0, ASN1Attributes)}) {
        {
            setOptional(3);
        }
    };

    public EncryptedPrivateKeyInfo(byte[] encoded) throws IOException {
        if (encoded == null) {
            throw new NullPointerException("encoded == null");
        }
        this.encoded = new byte[encoded.length];
        System.arraycopy(encoded, 0, this.encoded, 0, encoded.length);
        Object[] values = (Object[]) asn1.decode(encoded);
        AlgorithmIdentifier aId = (AlgorithmIdentifier) values[0];
        this.algName = aId.getAlgorithm();
        boolean mappingExists = mapAlgName();
        AlgorithmParameters aParams = null;
        byte[] params = aId.getParameters();
        if (params != null && !isNullValue(params)) {
            try {
                aParams = AlgorithmParameters.getInstance(this.algName);
                aParams.init(aId.getParameters());
                if (!mappingExists) {
                    this.algName = aParams.getAlgorithm();
                }
            } catch (NoSuchAlgorithmException e) {
            }
        }
        this.algParameters = aParams;
        this.encryptedData = (byte[]) values[1];
    }

    private static boolean isNullValue(byte[] toCheck) {
        return toCheck[0] == 5 && toCheck[1] == 0;
    }

    public EncryptedPrivateKeyInfo(String encryptionAlgorithmName, byte[] encryptedData) throws NoSuchAlgorithmException {
        if (encryptionAlgorithmName == null) {
            throw new NullPointerException("encryptionAlgorithmName == null");
        }
        this.algName = encryptionAlgorithmName;
        if (!mapAlgName()) {
            throw new NoSuchAlgorithmException("Unsupported algorithm: " + this.algName);
        }
        if (encryptedData == null) {
            throw new NullPointerException("encryptedData == null");
        }
        if (encryptedData.length == 0) {
            throw new IllegalArgumentException("encryptedData.length == 0");
        }
        this.encryptedData = new byte[encryptedData.length];
        System.arraycopy(encryptedData, 0, this.encryptedData, 0, encryptedData.length);
        this.algParameters = null;
    }

    public EncryptedPrivateKeyInfo(AlgorithmParameters algParams, byte[] encryptedData) throws NoSuchAlgorithmException {
        if (algParams == null) {
            throw new NullPointerException("algParams == null");
        }
        this.algParameters = algParams;
        if (encryptedData == null) {
            throw new NullPointerException("encryptedData == null");
        }
        if (encryptedData.length == 0) {
            throw new IllegalArgumentException("encryptedData.length == 0");
        }
        this.encryptedData = new byte[encryptedData.length];
        System.arraycopy(encryptedData, 0, this.encryptedData, 0, encryptedData.length);
        this.algName = this.algParameters.getAlgorithm();
        if (!mapAlgName()) {
            throw new NoSuchAlgorithmException("Unsupported algorithm: " + this.algName);
        }
    }

    public String getAlgName() {
        return this.algName;
    }

    public AlgorithmParameters getAlgParameters() {
        return this.algParameters;
    }

    public byte[] getEncryptedData() {
        byte[] ret = new byte[this.encryptedData.length];
        System.arraycopy(this.encryptedData, 0, ret, 0, this.encryptedData.length);
        return ret;
    }

    public PKCS8EncodedKeySpec getKeySpec(Cipher cipher) throws InvalidKeySpecException {
        if (cipher == null) {
            throw new NullPointerException("cipher == null");
        }
        try {
            byte[] decryptedData = cipher.doFinal(this.encryptedData);
            try {
                ASN1PrivateKeyInfo.verify(decryptedData);
                return new PKCS8EncodedKeySpec(decryptedData);
            } catch (IOException e) {
                throw new InvalidKeySpecException("Decrypted data does not represent valid PKCS#8 PrivateKeyInfo");
            }
        } catch (IllegalStateException e2) {
            throw new InvalidKeySpecException(e2.getMessage());
        } catch (BadPaddingException e3) {
            throw new InvalidKeySpecException(e3.getMessage());
        } catch (IllegalBlockSizeException e4) {
            throw new InvalidKeySpecException(e4.getMessage());
        }
    }

    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey) throws NoSuchAlgorithmException, InvalidKeyException {
        if (decryptKey == null) {
            throw new NullPointerException("decryptKey == null");
        }
        try {
            Cipher cipher = Cipher.getInstance(this.algName);
            if (this.algParameters == null) {
                cipher.init(2, decryptKey);
            } else {
                cipher.init(2, decryptKey, this.algParameters);
            }
            byte[] decryptedData = cipher.doFinal(this.encryptedData);
            try {
                ASN1PrivateKeyInfo.verify(decryptedData);
                return new PKCS8EncodedKeySpec(decryptedData);
            } catch (IOException e) {
                throw invalidKey();
            }
        } catch (IllegalStateException e2) {
            throw new InvalidKeyException(e2.getMessage());
        } catch (InvalidAlgorithmParameterException e3) {
            throw new NoSuchAlgorithmException(e3.getMessage());
        } catch (BadPaddingException e4) {
            throw new InvalidKeyException(e4.getMessage());
        } catch (IllegalBlockSizeException e5) {
            throw new InvalidKeyException(e5.getMessage());
        } catch (NoSuchPaddingException e6) {
            throw new NoSuchAlgorithmException(e6.getMessage());
        }
    }

    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey, String providerName) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        if (decryptKey == null) {
            throw new NullPointerException("decryptKey == null");
        }
        if (providerName == null) {
            throw new NullPointerException("providerName == null");
        }
        try {
            Cipher cipher = Cipher.getInstance(this.algName, providerName);
            if (this.algParameters == null) {
                cipher.init(2, decryptKey);
            } else {
                cipher.init(2, decryptKey, this.algParameters);
            }
            byte[] decryptedData = cipher.doFinal(this.encryptedData);
            try {
                ASN1PrivateKeyInfo.verify(decryptedData);
                return new PKCS8EncodedKeySpec(decryptedData);
            } catch (IOException e) {
                throw invalidKey();
            }
        } catch (IllegalStateException e2) {
            throw new InvalidKeyException(e2.getMessage());
        } catch (InvalidAlgorithmParameterException e3) {
            throw new NoSuchAlgorithmException(e3.getMessage());
        } catch (BadPaddingException e4) {
            throw new InvalidKeyException(e4.getMessage());
        } catch (IllegalBlockSizeException e5) {
            throw new InvalidKeyException(e5.getMessage());
        } catch (NoSuchPaddingException e6) {
            throw new NoSuchAlgorithmException(e6.getMessage());
        }
    }

    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey, Provider provider) throws NoSuchAlgorithmException, InvalidKeyException {
        if (decryptKey == null) {
            throw new NullPointerException("decryptKey == null");
        }
        if (provider == null) {
            throw new NullPointerException("provider == null");
        }
        try {
            Cipher cipher = Cipher.getInstance(this.algName, provider);
            if (this.algParameters == null) {
                cipher.init(2, decryptKey);
            } else {
                cipher.init(2, decryptKey, this.algParameters);
            }
            byte[] decryptedData = cipher.doFinal(this.encryptedData);
            try {
                ASN1PrivateKeyInfo.verify(decryptedData);
                return new PKCS8EncodedKeySpec(decryptedData);
            } catch (IOException e) {
                throw invalidKey();
            }
        } catch (IllegalStateException e2) {
            throw new InvalidKeyException(e2.getMessage());
        } catch (InvalidAlgorithmParameterException e3) {
            throw new NoSuchAlgorithmException(e3.getMessage());
        } catch (BadPaddingException e4) {
            throw new InvalidKeyException(e4.getMessage());
        } catch (IllegalBlockSizeException e5) {
            throw new InvalidKeyException(e5.getMessage());
        } catch (NoSuchPaddingException e6) {
            throw new NoSuchAlgorithmException(e6.getMessage());
        }
    }

    private InvalidKeyException invalidKey() throws InvalidKeyException {
        throw new InvalidKeyException("Decrypted data does not represent valid PKCS#8 PrivateKeyInfo");
    }

    public byte[] getEncoded() throws IOException {
        if (this.encoded == null) {
            this.encoded = asn1.encode(this);
        }
        byte[] ret = new byte[this.encoded.length];
        System.arraycopy(this.encoded, 0, ret, 0, this.encoded.length);
        return ret;
    }

    private boolean mapAlgName() {
        if (AlgNameMapper.isOID(this.algName)) {
            this.oid = AlgNameMapper.normalize(this.algName);
            this.algName = AlgNameMapper.map2AlgName(this.oid);
            if (this.algName == null) {
                this.algName = this.oid;
            }
        } else {
            String stdName = AlgNameMapper.getStandardName(this.algName);
            this.oid = AlgNameMapper.map2OID(this.algName);
            if (this.oid == null) {
                if (stdName == null) {
                    return false;
                }
                this.oid = AlgNameMapper.map2OID(stdName);
                if (this.oid == null) {
                    return false;
                }
                this.algName = stdName;
            } else if (stdName != null) {
                this.algName = stdName;
            }
        }
        return true;
    }
}
