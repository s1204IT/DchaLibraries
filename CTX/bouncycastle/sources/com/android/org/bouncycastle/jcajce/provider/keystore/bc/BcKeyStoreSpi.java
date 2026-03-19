package com.android.org.bouncycastle.jcajce.provider.keystore.bc;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.PBEParametersGenerator;
import com.android.org.bouncycastle.crypto.digests.SHA1Digest;
import com.android.org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import com.android.org.bouncycastle.crypto.io.DigestInputStream;
import com.android.org.bouncycastle.crypto.io.DigestOutputStream;
import com.android.org.bouncycastle.crypto.io.MacInputStream;
import com.android.org.bouncycastle.crypto.io.MacOutputStream;
import com.android.org.bouncycastle.crypto.macs.HMac;
import com.android.org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.interfaces.BCKeyStore;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.io.Streams;
import com.android.org.bouncycastle.util.io.TeeOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class BcKeyStoreSpi extends KeyStoreSpi implements BCKeyStore {
    static final int CERTIFICATE = 1;
    static final int KEY = 2;
    private static final String KEY_CIPHER = "PBEWithSHAAnd3-KeyTripleDES-CBC";
    static final int KEY_PRIVATE = 0;
    static final int KEY_PUBLIC = 1;
    private static final int KEY_SALT_SIZE = 20;
    static final int KEY_SECRET = 2;
    private static final int MIN_ITERATIONS = 1024;
    static final int NULL = 0;
    static final int SEALED = 4;
    static final int SECRET = 3;
    private static final String STORE_CIPHER = "PBEWithSHAAndTwofish-CBC";
    private static final int STORE_SALT_SIZE = 20;
    private static final int STORE_VERSION = 2;
    protected int version;
    protected Hashtable table = new Hashtable();
    protected SecureRandom random = new SecureRandom();
    private final JcaJceHelper helper = new DefaultJcaJceHelper();

    public BcKeyStoreSpi(int i) {
        this.version = i;
    }

    private class StoreEntry {
        String alias;
        Certificate[] certChain;
        Date date;
        Object obj;
        int type;

        StoreEntry(String str, Certificate certificate) {
            this.date = new Date();
            this.type = 1;
            this.alias = str;
            this.obj = certificate;
            this.certChain = null;
        }

        StoreEntry(String str, byte[] bArr, Certificate[] certificateArr) {
            this.date = new Date();
            this.type = 3;
            this.alias = str;
            this.obj = bArr;
            this.certChain = certificateArr;
        }

        StoreEntry(String str, Key key, char[] cArr, Certificate[] certificateArr) throws Exception {
            this.date = new Date();
            this.type = 4;
            this.alias = str;
            this.certChain = certificateArr;
            byte[] bArr = new byte[20];
            BcKeyStoreSpi.this.random.setSeed(System.currentTimeMillis());
            BcKeyStoreSpi.this.random.nextBytes(bArr);
            int iNextInt = BcKeyStoreSpi.MIN_ITERATIONS + (BcKeyStoreSpi.this.random.nextInt() & 1023);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeInt(bArr.length);
            dataOutputStream.write(bArr);
            dataOutputStream.writeInt(iNextInt);
            DataOutputStream dataOutputStream2 = new DataOutputStream(new CipherOutputStream(dataOutputStream, BcKeyStoreSpi.this.makePBECipher(BcKeyStoreSpi.KEY_CIPHER, 1, cArr, bArr, iNextInt)));
            BcKeyStoreSpi.this.encodeKey(key, dataOutputStream2);
            dataOutputStream2.close();
            this.obj = byteArrayOutputStream.toByteArray();
        }

        StoreEntry(String str, Date date, int i, Object obj) {
            this.date = new Date();
            this.alias = str;
            this.date = date;
            this.type = i;
            this.obj = obj;
        }

        StoreEntry(String str, Date date, int i, Object obj, Certificate[] certificateArr) {
            this.date = new Date();
            this.alias = str;
            this.date = date;
            this.type = i;
            this.obj = obj;
            this.certChain = certificateArr;
        }

        int getType() {
            return this.type;
        }

        String getAlias() {
            return this.alias;
        }

        Object getObject() {
            return this.obj;
        }

        Object getObject(char[] cArr) throws UnrecoverableKeyException, NoSuchAlgorithmException {
            Key keyDecodeKey;
            if ((cArr == null || cArr.length == 0) && (this.obj instanceof Key)) {
                return this.obj;
            }
            if (this.type == 4) {
                DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream((byte[]) this.obj));
                try {
                    byte[] bArr = new byte[dataInputStream.readInt()];
                    dataInputStream.readFully(bArr);
                    try {
                        return BcKeyStoreSpi.this.decodeKey(new DataInputStream(new CipherInputStream(dataInputStream, BcKeyStoreSpi.this.makePBECipher(BcKeyStoreSpi.KEY_CIPHER, 2, cArr, bArr, dataInputStream.readInt()))));
                    } catch (Exception e) {
                        DataInputStream dataInputStream2 = new DataInputStream(new ByteArrayInputStream((byte[]) this.obj));
                        byte[] bArr2 = new byte[dataInputStream2.readInt()];
                        dataInputStream2.readFully(bArr2);
                        int i = dataInputStream2.readInt();
                        try {
                            keyDecodeKey = BcKeyStoreSpi.this.decodeKey(new DataInputStream(new CipherInputStream(dataInputStream2, BcKeyStoreSpi.this.makePBECipher("BrokenPBEWithSHAAnd3-KeyTripleDES-CBC", 2, cArr, bArr2, i))));
                        } catch (Exception e2) {
                            DataInputStream dataInputStream3 = new DataInputStream(new ByteArrayInputStream((byte[]) this.obj));
                            bArr2 = new byte[dataInputStream3.readInt()];
                            dataInputStream3.readFully(bArr2);
                            i = dataInputStream3.readInt();
                            keyDecodeKey = BcKeyStoreSpi.this.decodeKey(new DataInputStream(new CipherInputStream(dataInputStream3, BcKeyStoreSpi.this.makePBECipher("OldPBEWithSHAAnd3-KeyTripleDES-CBC", 2, cArr, bArr2, i))));
                        }
                        byte[] bArr3 = bArr2;
                        int i2 = i;
                        if (keyDecodeKey != null) {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                            dataOutputStream.writeInt(bArr3.length);
                            dataOutputStream.write(bArr3);
                            dataOutputStream.writeInt(i2);
                            DataOutputStream dataOutputStream2 = new DataOutputStream(new CipherOutputStream(dataOutputStream, BcKeyStoreSpi.this.makePBECipher(BcKeyStoreSpi.KEY_CIPHER, 1, cArr, bArr3, i2)));
                            BcKeyStoreSpi.this.encodeKey(keyDecodeKey, dataOutputStream2);
                            dataOutputStream2.close();
                            this.obj = byteArrayOutputStream.toByteArray();
                            return keyDecodeKey;
                        }
                        throw new UnrecoverableKeyException("no match");
                    }
                } catch (Exception e3) {
                    throw new UnrecoverableKeyException("no match");
                }
            }
            throw new RuntimeException("forget something!");
        }

        Certificate[] getCertificateChain() {
            return this.certChain;
        }

        Date getDate() {
            return this.date;
        }
    }

    private void encodeCertificate(Certificate certificate, DataOutputStream dataOutputStream) throws IOException {
        try {
            byte[] encoded = certificate.getEncoded();
            dataOutputStream.writeUTF(certificate.getType());
            dataOutputStream.writeInt(encoded.length);
            dataOutputStream.write(encoded);
        } catch (CertificateEncodingException e) {
            throw new IOException(e.toString());
        }
    }

    private Certificate decodeCertificate(DataInputStream dataInputStream) throws IOException {
        String utf = dataInputStream.readUTF();
        byte[] bArr = new byte[dataInputStream.readInt()];
        dataInputStream.readFully(bArr);
        try {
            return this.helper.createCertificateFactory(utf).generateCertificate(new ByteArrayInputStream(bArr));
        } catch (NoSuchProviderException e) {
            throw new IOException(e.toString());
        } catch (CertificateException e2) {
            throw new IOException(e2.toString());
        }
    }

    private void encodeKey(Key key, DataOutputStream dataOutputStream) throws IOException {
        byte[] encoded = key.getEncoded();
        if (key instanceof PrivateKey) {
            dataOutputStream.write(0);
        } else if (key instanceof PublicKey) {
            dataOutputStream.write(1);
        } else {
            dataOutputStream.write(2);
        }
        dataOutputStream.writeUTF(key.getFormat());
        dataOutputStream.writeUTF(key.getAlgorithm());
        dataOutputStream.writeInt(encoded.length);
        dataOutputStream.write(encoded);
    }

    private Key decodeKey(DataInputStream dataInputStream) throws IOException {
        KeySpec pKCS8EncodedKeySpec;
        int i = dataInputStream.read();
        String utf = dataInputStream.readUTF();
        String utf2 = dataInputStream.readUTF();
        byte[] bArr = new byte[dataInputStream.readInt()];
        dataInputStream.readFully(bArr);
        if (utf.equals("PKCS#8") || utf.equals("PKCS8")) {
            pKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(bArr);
        } else if (utf.equals("X.509") || utf.equals("X509")) {
            pKCS8EncodedKeySpec = new X509EncodedKeySpec(bArr);
        } else {
            if (utf.equals("RAW")) {
                return new SecretKeySpec(bArr, utf2);
            }
            throw new IOException("Key format " + utf + " not recognised!");
        }
        try {
            switch (i) {
                case 0:
                    return this.helper.createKeyFactory(utf2).generatePrivate(pKCS8EncodedKeySpec);
                case 1:
                    return this.helper.createKeyFactory(utf2).generatePublic(pKCS8EncodedKeySpec);
                case 2:
                    return this.helper.createSecretKeyFactory(utf2).generateSecret(pKCS8EncodedKeySpec);
                default:
                    throw new IOException("Key type " + i + " not recognised!");
            }
        } catch (Exception e) {
            throw new IOException("Exception creating key: " + e.toString());
        }
    }

    protected Cipher makePBECipher(String str, int i, char[] cArr, byte[] bArr, int i2) throws IOException {
        try {
            PBEKeySpec pBEKeySpec = new PBEKeySpec(cArr);
            SecretKeyFactory secretKeyFactoryCreateSecretKeyFactory = this.helper.createSecretKeyFactory(str);
            PBEParameterSpec pBEParameterSpec = new PBEParameterSpec(bArr, i2);
            Cipher cipherCreateCipher = this.helper.createCipher(str);
            cipherCreateCipher.init(i, secretKeyFactoryCreateSecretKeyFactory.generateSecret(pBEKeySpec), pBEParameterSpec);
            return cipherCreateCipher;
        } catch (Exception e) {
            throw new IOException("Error initialising store of key store: " + e);
        }
    }

    @Override
    public void setRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
    }

    @Override
    public Enumeration engineAliases() {
        return this.table.keys();
    }

    @Override
    public boolean engineContainsAlias(String str) {
        return this.table.get(str) != null;
    }

    @Override
    public void engineDeleteEntry(String str) throws KeyStoreException {
        if (this.table.get(str) == null) {
            return;
        }
        this.table.remove(str);
    }

    @Override
    public Certificate engineGetCertificate(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        if (storeEntry != null) {
            if (storeEntry.getType() == 1) {
                return (Certificate) storeEntry.getObject();
            }
            Certificate[] certificateChain = storeEntry.getCertificateChain();
            if (certificateChain != null) {
                return certificateChain[0];
            }
            return null;
        }
        return null;
    }

    @Override
    public String engineGetCertificateAlias(Certificate certificate) {
        Enumeration enumerationElements = this.table.elements();
        while (enumerationElements.hasMoreElements()) {
            StoreEntry storeEntry = (StoreEntry) enumerationElements.nextElement();
            if (storeEntry.getObject() instanceof Certificate) {
                if (((Certificate) storeEntry.getObject()).equals(certificate)) {
                    return storeEntry.getAlias();
                }
            } else {
                Certificate[] certificateChain = storeEntry.getCertificateChain();
                if (certificateChain != null && certificateChain[0].equals(certificate)) {
                    return storeEntry.getAlias();
                }
            }
        }
        return null;
    }

    @Override
    public Certificate[] engineGetCertificateChain(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        if (storeEntry != null) {
            return storeEntry.getCertificateChain();
        }
        return null;
    }

    @Override
    public Date engineGetCreationDate(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        if (storeEntry != null) {
            return storeEntry.getDate();
        }
        return null;
    }

    @Override
    public Key engineGetKey(String str, char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        if (storeEntry == null || storeEntry.getType() == 1) {
            return null;
        }
        return (Key) storeEntry.getObject(cArr);
    }

    @Override
    public boolean engineIsCertificateEntry(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        return storeEntry != null && storeEntry.getType() == 1;
    }

    @Override
    public boolean engineIsKeyEntry(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        return (storeEntry == null || storeEntry.getType() == 1) ? false : true;
    }

    @Override
    public void engineSetCertificateEntry(String str, Certificate certificate) throws KeyStoreException {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        if (storeEntry != null && storeEntry.getType() != 1) {
            throw new KeyStoreException("key store already has a key entry with alias " + str);
        }
        this.table.put(str, new StoreEntry(str, certificate));
    }

    @Override
    public void engineSetKeyEntry(String str, byte[] bArr, Certificate[] certificateArr) throws KeyStoreException {
        this.table.put(str, new StoreEntry(str, bArr, certificateArr));
    }

    @Override
    public void engineSetKeyEntry(String str, Key key, char[] cArr, Certificate[] certificateArr) throws KeyStoreException {
        if ((key instanceof PrivateKey) && certificateArr == null) {
            throw new KeyStoreException("no certificate chain for private key");
        }
        try {
            this.table.put(str, new StoreEntry(str, key, cArr, certificateArr));
        } catch (Exception e) {
            throw new KeyStoreException(e.toString());
        }
    }

    @Override
    public int engineSize() {
        return this.table.size();
    }

    protected void loadStore(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        for (int i = dataInputStream.read(); i > 0; i = dataInputStream.read()) {
            String utf = dataInputStream.readUTF();
            Date date = new Date(dataInputStream.readLong());
            int i2 = dataInputStream.readInt();
            Certificate[] certificateArr = null;
            if (i2 != 0) {
                certificateArr = new Certificate[i2];
                for (int i3 = 0; i3 != i2; i3++) {
                    certificateArr[i3] = decodeCertificate(dataInputStream);
                }
            }
            Certificate[] certificateArr2 = certificateArr;
            switch (i) {
                case 1:
                    this.table.put(utf, new StoreEntry(utf, date, 1, decodeCertificate(dataInputStream)));
                    break;
                case 2:
                    this.table.put(utf, new StoreEntry(utf, date, 2, decodeKey(dataInputStream), certificateArr2));
                    break;
                case 3:
                case 4:
                    byte[] bArr = new byte[dataInputStream.readInt()];
                    dataInputStream.readFully(bArr);
                    this.table.put(utf, new StoreEntry(utf, date, i, bArr, certificateArr2));
                    break;
                default:
                    throw new RuntimeException("Unknown object type in store.");
            }
        }
    }

    protected void saveStore(OutputStream outputStream) throws IOException {
        Enumeration enumerationElements = this.table.elements();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        while (true) {
            if (enumerationElements.hasMoreElements()) {
                StoreEntry storeEntry = (StoreEntry) enumerationElements.nextElement();
                dataOutputStream.write(storeEntry.getType());
                dataOutputStream.writeUTF(storeEntry.getAlias());
                dataOutputStream.writeLong(storeEntry.getDate().getTime());
                Certificate[] certificateChain = storeEntry.getCertificateChain();
                if (certificateChain == null) {
                    dataOutputStream.writeInt(0);
                } else {
                    dataOutputStream.writeInt(certificateChain.length);
                    for (int i = 0; i != certificateChain.length; i++) {
                        encodeCertificate(certificateChain[i], dataOutputStream);
                    }
                }
                switch (storeEntry.getType()) {
                    case 1:
                        encodeCertificate((Certificate) storeEntry.getObject(), dataOutputStream);
                        break;
                    case 2:
                        encodeKey((Key) storeEntry.getObject(), dataOutputStream);
                        break;
                    case 3:
                    case 4:
                        byte[] bArr = (byte[]) storeEntry.getObject();
                        dataOutputStream.writeInt(bArr.length);
                        dataOutputStream.write(bArr);
                        break;
                    default:
                        throw new RuntimeException("Unknown object type in store.");
                }
            } else {
                dataOutputStream.write(0);
                return;
            }
        }
    }

    @Override
    public void engineLoad(InputStream inputStream, char[] cArr) throws IOException {
        CipherParameters cipherParametersGenerateDerivedMacParameters;
        this.table.clear();
        if (inputStream == null) {
            return;
        }
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int i = dataInputStream.readInt();
        if (i != 2 && i != 0 && i != 1) {
            throw new IOException("Wrong version of key store.");
        }
        int i2 = dataInputStream.readInt();
        if (i2 <= 0) {
            throw new IOException("Invalid salt detected");
        }
        byte[] bArr = new byte[i2];
        dataInputStream.readFully(bArr);
        int i3 = dataInputStream.readInt();
        HMac hMac = new HMac(new SHA1Digest());
        if (cArr != null && cArr.length != 0) {
            byte[] bArrPKCS12PasswordToBytes = PBEParametersGenerator.PKCS12PasswordToBytes(cArr);
            PKCS12ParametersGenerator pKCS12ParametersGenerator = new PKCS12ParametersGenerator(new SHA1Digest());
            pKCS12ParametersGenerator.init(bArrPKCS12PasswordToBytes, bArr, i3);
            if (i != 2) {
                cipherParametersGenerateDerivedMacParameters = pKCS12ParametersGenerator.generateDerivedMacParameters(hMac.getMacSize());
            } else {
                cipherParametersGenerateDerivedMacParameters = pKCS12ParametersGenerator.generateDerivedMacParameters(hMac.getMacSize() * 8);
            }
            Arrays.fill(bArrPKCS12PasswordToBytes, (byte) 0);
            hMac.init(cipherParametersGenerateDerivedMacParameters);
            loadStore(new MacInputStream(dataInputStream, hMac));
            byte[] bArr2 = new byte[hMac.getMacSize()];
            hMac.doFinal(bArr2, 0);
            byte[] bArr3 = new byte[hMac.getMacSize()];
            dataInputStream.readFully(bArr3);
            if (!Arrays.constantTimeAreEqual(bArr2, bArr3)) {
                this.table.clear();
                throw new IOException("KeyStore integrity check failed.");
            }
            return;
        }
        loadStore(dataInputStream);
        dataInputStream.readFully(new byte[hMac.getMacSize()]);
    }

    @Override
    public void engineStore(OutputStream outputStream, char[] cArr) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        byte[] bArr = new byte[20];
        int iNextInt = MIN_ITERATIONS + (this.random.nextInt() & 1023);
        this.random.nextBytes(bArr);
        dataOutputStream.writeInt(this.version);
        dataOutputStream.writeInt(bArr.length);
        dataOutputStream.write(bArr);
        dataOutputStream.writeInt(iNextInt);
        HMac hMac = new HMac(new SHA1Digest());
        MacOutputStream macOutputStream = new MacOutputStream(hMac);
        PKCS12ParametersGenerator pKCS12ParametersGenerator = new PKCS12ParametersGenerator(new SHA1Digest());
        byte[] bArrPKCS12PasswordToBytes = PBEParametersGenerator.PKCS12PasswordToBytes(cArr);
        pKCS12ParametersGenerator.init(bArrPKCS12PasswordToBytes, bArr, iNextInt);
        if (this.version < 2) {
            hMac.init(pKCS12ParametersGenerator.generateDerivedMacParameters(hMac.getMacSize()));
        } else {
            hMac.init(pKCS12ParametersGenerator.generateDerivedMacParameters(hMac.getMacSize() * 8));
        }
        for (int i = 0; i != bArrPKCS12PasswordToBytes.length; i++) {
            bArrPKCS12PasswordToBytes[i] = 0;
        }
        saveStore(new TeeOutputStream(dataOutputStream, macOutputStream));
        byte[] bArr2 = new byte[hMac.getMacSize()];
        hMac.doFinal(bArr2, 0);
        dataOutputStream.write(bArr2);
        dataOutputStream.close();
    }

    public static class BouncyCastleStore extends BcKeyStoreSpi {
        public BouncyCastleStore() {
            super(1);
        }

        @Override
        public void engineLoad(InputStream inputStream, char[] cArr) throws IOException {
            String str;
            this.table.clear();
            if (inputStream == null) {
                return;
            }
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int i = dataInputStream.readInt();
            if (i != 2 && i != 0 && i != 1) {
                throw new IOException("Wrong version of key store.");
            }
            byte[] bArr = new byte[dataInputStream.readInt()];
            if (bArr.length != 20) {
                throw new IOException("Key store corrupted.");
            }
            dataInputStream.readFully(bArr);
            int i2 = dataInputStream.readInt();
            if (i2 < 0 || i2 > 4096) {
                throw new IOException("Key store corrupted.");
            }
            if (i == 0) {
                str = "OldPBEWithSHAAndTwofish-CBC";
            } else {
                str = BcKeyStoreSpi.STORE_CIPHER;
            }
            CipherInputStream cipherInputStream = new CipherInputStream(dataInputStream, makePBECipher(str, 2, cArr, bArr, i2));
            SHA1Digest sHA1Digest = new SHA1Digest();
            loadStore(new DigestInputStream(cipherInputStream, sHA1Digest));
            byte[] bArr2 = new byte[sHA1Digest.getDigestSize()];
            sHA1Digest.doFinal(bArr2, 0);
            byte[] bArr3 = new byte[sHA1Digest.getDigestSize()];
            Streams.readFully(cipherInputStream, bArr3);
            if (!Arrays.constantTimeAreEqual(bArr2, bArr3)) {
                this.table.clear();
                throw new IOException("KeyStore integrity check failed.");
            }
        }

        @Override
        public void engineStore(OutputStream outputStream, char[] cArr) throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            byte[] bArr = new byte[20];
            int iNextInt = BcKeyStoreSpi.MIN_ITERATIONS + (this.random.nextInt() & 1023);
            this.random.nextBytes(bArr);
            dataOutputStream.writeInt(this.version);
            dataOutputStream.writeInt(bArr.length);
            dataOutputStream.write(bArr);
            dataOutputStream.writeInt(iNextInt);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(dataOutputStream, makePBECipher(BcKeyStoreSpi.STORE_CIPHER, 1, cArr, bArr, iNextInt));
            DigestOutputStream digestOutputStream = new DigestOutputStream(new SHA1Digest());
            saveStore(new TeeOutputStream(cipherOutputStream, digestOutputStream));
            cipherOutputStream.write(digestOutputStream.getDigest());
            cipherOutputStream.close();
        }
    }

    static Provider getBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
            return Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        }
        return new BouncyCastleProvider();
    }

    public static class Std extends BcKeyStoreSpi {
        public Std() {
            super(2);
        }
    }

    public static class Version1 extends BcKeyStoreSpi {
        public Version1() {
            super(1);
        }
    }
}
