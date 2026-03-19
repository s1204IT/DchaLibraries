package sun.security.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import sun.misc.HexDumpEncoder;
import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class X509Key implements PublicKey {
    private static final long serialVersionUID = -5359250853002055002L;
    protected AlgorithmId algid;
    protected byte[] encodedKey;

    @Deprecated
    protected byte[] key = null;

    @Deprecated
    private int unusedBits = 0;
    private BitArray bitStringKey = null;

    public X509Key() {
    }

    private X509Key(AlgorithmId algorithmId, BitArray bitArray) throws InvalidKeyException {
        this.algid = algorithmId;
        setKey(bitArray);
        encode();
    }

    protected void setKey(BitArray bitArray) {
        this.bitStringKey = (BitArray) bitArray.clone();
        this.key = bitArray.toByteArray();
        int length = bitArray.length() % 8;
        this.unusedBits = length == 0 ? 0 : 8 - length;
    }

    protected BitArray getKey() {
        this.bitStringKey = new BitArray((this.key.length * 8) - this.unusedBits, this.key);
        return (BitArray) this.bitStringKey.clone();
    }

    public static PublicKey parse(DerValue derValue) throws IOException {
        if (derValue.tag != 48) {
            throw new IOException("corrupt subject key");
        }
        try {
            PublicKey publicKeyBuildX509Key = buildX509Key(AlgorithmId.parse(derValue.data.getDerValue()), derValue.data.getUnalignedBitString());
            if (derValue.data.available() != 0) {
                throw new IOException("excess subject key");
            }
            return publicKeyBuildX509Key;
        } catch (InvalidKeyException e) {
            throw new IOException("subject key, " + e.getMessage(), e);
        }
    }

    protected void parseKeyBits() throws InvalidKeyException, IOException {
        encode();
    }

    static PublicKey buildX509Key(AlgorithmId algorithmId, BitArray bitArray) throws IOException, InvalidKeyException {
        Provider provider;
        Class<?> clsLoadClass;
        DerOutputStream derOutputStream = new DerOutputStream();
        encode(derOutputStream, algorithmId, bitArray);
        try {
            return KeyFactory.getInstance(algorithmId.getName()).generatePublic(new X509EncodedKeySpec(derOutputStream.toByteArray()));
        } catch (NoSuchAlgorithmException e) {
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
                String property = provider.getProperty("PublicKey.X.509." + algorithmId.getName());
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
                    if (objNewInstance instanceof X509Key) {
                        X509Key x509Key = (X509Key) objNewInstance;
                        x509Key.algid = algorithmId;
                        x509Key.setKey(bitArray);
                        x509Key.parseKeyBits();
                        return x509Key;
                    }
                    return new X509Key(algorithmId, bitArray);
                } catch (IllegalAccessException e5) {
                    str = property;
                    throw new IOException(str + " [internal error]");
                }
            } catch (IllegalAccessException e6) {
            }
        } catch (InvalidKeySpecException e7) {
            throw new InvalidKeyException(e7.getMessage(), e7);
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
        encode(derOutputStream, this.algid, getKey());
    }

    @Override
    public byte[] getEncoded() {
        try {
            return (byte[]) getEncodedInternal().clone();
        } catch (InvalidKeyException e) {
            return null;
        }
    }

    public byte[] getEncodedInternal() throws InvalidKeyException {
        byte[] byteArray = this.encodedKey;
        if (byteArray == null) {
            try {
                DerOutputStream derOutputStream = new DerOutputStream();
                encode(derOutputStream);
                byteArray = derOutputStream.toByteArray();
                this.encodedKey = byteArray;
            } catch (IOException e) {
                throw new InvalidKeyException("IOException : " + e.getMessage());
            }
        }
        return byteArray;
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    public byte[] encode() throws InvalidKeyException {
        return (byte[]) getEncodedInternal().clone();
    }

    public String toString() {
        return "algorithm = " + this.algid.toString() + ", unparsed keybits = \n" + new HexDumpEncoder().encodeBuffer(this.key);
    }

    public void decode(InputStream inputStream) throws InvalidKeyException {
        try {
            DerValue derValue = new DerValue(inputStream);
            if (derValue.tag != 48) {
                throw new InvalidKeyException("invalid key format");
            }
            this.algid = AlgorithmId.parse(derValue.data.getDerValue());
            setKey(derValue.data.getUnalignedBitString());
            parseKeyBits();
            if (derValue.data.available() != 0) {
                throw new InvalidKeyException("excess key data");
            }
        } catch (IOException e) {
            throw new InvalidKeyException("IOException: " + e.getMessage());
        }
    }

    public void decode(byte[] bArr) throws InvalidKeyException {
        decode(new ByteArrayInputStream(bArr));
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.write(getEncoded());
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException {
        try {
            decode(objectInputStream);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new IOException("deserialized key is invalid: " + e.getMessage());
        }
    }

    public boolean equals(Object obj) {
        byte[] encoded;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Key)) {
            return false;
        }
        try {
            byte[] encodedInternal = getEncodedInternal();
            if (obj instanceof X509Key) {
                encoded = ((X509Key) obj).getEncodedInternal();
            } else {
                encoded = ((Key) obj).getEncoded();
            }
            return Arrays.equals(encodedInternal, encoded);
        } catch (InvalidKeyException e) {
            return false;
        }
    }

    public int hashCode() {
        try {
            byte[] encodedInternal = getEncodedInternal();
            int length = encodedInternal.length;
            for (byte b : encodedInternal) {
                length += (b & Character.DIRECTIONALITY_UNDEFINED) * 37;
            }
            return length;
        } catch (InvalidKeyException e) {
            return 0;
        }
    }

    static void encode(DerOutputStream derOutputStream, AlgorithmId algorithmId, BitArray bitArray) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        algorithmId.encode(derOutputStream2);
        derOutputStream2.putUnalignedBitString(bitArray);
        derOutputStream.write((byte) 48, derOutputStream2);
    }
}
