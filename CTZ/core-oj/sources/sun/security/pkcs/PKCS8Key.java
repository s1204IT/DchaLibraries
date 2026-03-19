package sun.security.pkcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyRep;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import sun.security.util.Debug;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;

public class PKCS8Key implements PrivateKey {
    private static final long serialVersionUID = -3836890099307167124L;
    public static final BigInteger version = BigInteger.ZERO;
    protected AlgorithmId algid;
    protected byte[] encodedKey;
    protected byte[] key;

    public PKCS8Key() {
    }

    private PKCS8Key(AlgorithmId algorithmId, byte[] bArr) throws InvalidKeyException {
        this.algid = algorithmId;
        this.key = bArr;
        encode();
    }

    public static PKCS8Key parse(DerValue derValue) throws IOException {
        PrivateKey key = parseKey(derValue);
        if (key instanceof PKCS8Key) {
            return (PKCS8Key) key;
        }
        throw new IOException("Provider did not return PKCS8Key");
    }

    public static PrivateKey parseKey(DerValue derValue) throws IOException {
        if (derValue.tag != 48) {
            throw new IOException("corrupt private key");
        }
        BigInteger bigInteger = derValue.data.getBigInteger();
        if (!version.equals(bigInteger)) {
            throw new IOException("version mismatch: (supported: " + Debug.toHexString(version) + ", parsed: " + Debug.toHexString(bigInteger));
        }
        try {
            PrivateKey privateKeyBuildPKCS8Key = buildPKCS8Key(AlgorithmId.parse(derValue.data.getDerValue()), derValue.data.getOctetString());
            if (derValue.data.available() != 0) {
                throw new IOException("excess private key");
            }
            return privateKeyBuildPKCS8Key;
        } catch (InvalidKeyException e) {
            throw new IOException("corrupt private key");
        }
    }

    protected void parseKeyBits() throws InvalidKeyException, IOException {
        encode();
    }

    static PrivateKey buildPKCS8Key(AlgorithmId algorithmId, byte[] bArr) throws IOException, InvalidKeyException {
        Provider provider;
        Class<?> clsLoadClass;
        DerOutputStream derOutputStream = new DerOutputStream();
        encode(derOutputStream, algorithmId, bArr);
        try {
            return KeyFactory.getInstance(algorithmId.getName()).generatePrivate(new PKCS8EncodedKeySpec(derOutputStream.toByteArray()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            String str = "";
            try {
                try {
                    try {
                        provider = Security.getProvider("SUN");
                    } catch (ClassNotFoundException e2) {
                    }
                } catch (InstantiationException e3) {
                }
                if (provider == null) {
                    throw new InstantiationException();
                }
                String property = provider.getProperty("PrivateKey.PKCS#8." + algorithmId.getName());
                try {
                    if (property == null) {
                        throw new InstantiationException();
                    }
                    Object objNewInstance = null;
                    try {
                        clsLoadClass = Class.forName(property);
                    } catch (ClassNotFoundException e4) {
                        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                        if (systemClassLoader != null) {
                            clsLoadClass = systemClassLoader.loadClass(property);
                        } else {
                            clsLoadClass = null;
                        }
                    }
                    if (clsLoadClass != null) {
                        objNewInstance = clsLoadClass.newInstance();
                    }
                    if (objNewInstance instanceof PKCS8Key) {
                        PKCS8Key pKCS8Key = (PKCS8Key) objNewInstance;
                        pKCS8Key.algid = algorithmId;
                        pKCS8Key.key = bArr;
                        pKCS8Key.parseKeyBits();
                        return pKCS8Key;
                    }
                    PKCS8Key pKCS8Key2 = new PKCS8Key();
                    pKCS8Key2.algid = algorithmId;
                    pKCS8Key2.key = bArr;
                    return pKCS8Key2;
                } catch (IllegalAccessException e5) {
                    str = property;
                    throw new IOException(str + " [internal error]");
                }
            } catch (IllegalAccessException e6) {
            }
        }
    }

    @Override
    public String getAlgorithm() {
        return this.algid.getName();
    }

    public AlgorithmId getAlgorithmId() {
        return this.algid;
    }

    public final void encode(DerOutputStream derOutputStream) throws IOException {
        encode(derOutputStream, this.algid, this.key);
    }

    @Override
    public synchronized byte[] getEncoded() throws InvalidKeyException {
        byte[] bArrEncode;
        bArrEncode = null;
        try {
            bArrEncode = encode();
        } catch (InvalidKeyException e) {
        }
        return bArrEncode;
    }

    @Override
    public String getFormat() {
        return "PKCS#8";
    }

    public byte[] encode() throws InvalidKeyException {
        if (this.encodedKey == null) {
            try {
                DerOutputStream derOutputStream = new DerOutputStream();
                encode(derOutputStream);
                this.encodedKey = derOutputStream.toByteArray();
            } catch (IOException e) {
                throw new InvalidKeyException("IOException : " + e.getMessage());
            }
        }
        return (byte[]) this.encodedKey.clone();
    }

    public void decode(InputStream inputStream) throws InvalidKeyException {
        try {
            DerValue derValue = new DerValue(inputStream);
            if (derValue.tag != 48) {
                throw new InvalidKeyException("invalid key format");
            }
            BigInteger bigInteger = derValue.data.getBigInteger();
            if (!bigInteger.equals(version)) {
                throw new IOException("version mismatch: (supported: " + Debug.toHexString(version) + ", parsed: " + Debug.toHexString(bigInteger));
            }
            this.algid = AlgorithmId.parse(derValue.data.getDerValue());
            this.key = derValue.data.getOctetString();
            parseKeyBits();
            derValue.data.available();
        } catch (IOException e) {
            throw new InvalidKeyException("IOException : " + e.getMessage());
        }
    }

    public void decode(byte[] bArr) throws InvalidKeyException {
        decode(new ByteArrayInputStream(bArr));
    }

    protected Object writeReplace() throws ObjectStreamException {
        return new KeyRep(KeyRep.Type.PRIVATE, getAlgorithm(), getFormat(), getEncoded());
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException {
        try {
            decode(objectInputStream);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new IOException("deserialized key is invalid: " + e.getMessage());
        }
    }

    static void encode(DerOutputStream derOutputStream, AlgorithmId algorithmId, byte[] bArr) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream2.putInteger(version);
        algorithmId.encode(derOutputStream2);
        derOutputStream2.putOctetString(bArr);
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    public boolean equals(Object obj) throws InvalidKeyException {
        byte[] encoded;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Key)) {
            return false;
        }
        if (this.encodedKey != null) {
            encoded = this.encodedKey;
        } else {
            encoded = getEncoded();
        }
        byte[] encoded2 = ((Key) obj).getEncoded();
        if (encoded.length != encoded2.length) {
            return false;
        }
        for (int i = 0; i < encoded.length; i++) {
            if (encoded[i] != encoded2[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() throws InvalidKeyException {
        byte[] encoded = getEncoded();
        int i = 0;
        for (int i2 = 1; i2 < encoded.length; i2++) {
            i += encoded[i2] * i2;
        }
        return i;
    }
}
