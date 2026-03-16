package javax.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import libcore.io.IoUtils;

public class SealedObject implements Serializable {
    private static final long serialVersionUID = 4482838265551344752L;
    protected byte[] encodedParams;
    private byte[] encryptedContent;
    private String paramsAlg;
    private String sealAlg;

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();
        this.encodedParams = getSafeCopy(fields, "encodedParams");
        this.encryptedContent = getSafeCopy(fields, "encryptedContent");
        this.paramsAlg = (String) fields.get("paramsAlg", (Object) null);
        this.sealAlg = (String) fields.get("sealAlg", (Object) null);
    }

    private static byte[] getSafeCopy(ObjectInputStream.GetField fields, String fieldName) throws IOException {
        byte[] fieldValue = (byte[]) fields.get(fieldName, (Object) null);
        if (fieldValue != null) {
            return (byte[]) fieldValue.clone();
        }
        return null;
    }

    public SealedObject(Serializable object, Cipher c) throws Throwable {
        ByteArrayOutputStream bos;
        ObjectOutputStream oos;
        if (c == null) {
            throw new NullPointerException("c == null");
        }
        ObjectOutputStream oos2 = null;
        try {
            try {
                bos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bos);
            } catch (Throwable th) {
                th = th;
            }
        } catch (BadPaddingException e) {
            e = e;
        }
        try {
            oos.writeObject(object);
            oos.flush();
            AlgorithmParameters ap = c.getParameters();
            this.encodedParams = ap == null ? null : ap.getEncoded();
            this.paramsAlg = ap != null ? ap.getAlgorithm() : null;
            this.sealAlg = c.getAlgorithm();
            this.encryptedContent = c.doFinal(bos.toByteArray());
            IoUtils.closeQuietly(oos);
        } catch (BadPaddingException e2) {
            e = e2;
            throw new IOException(e.toString());
        } catch (Throwable th2) {
            th = th2;
            oos2 = oos;
            IoUtils.closeQuietly(oos2);
            throw th;
        }
    }

    protected SealedObject(SealedObject so) {
        if (so == null) {
            throw new NullPointerException("so == null");
        }
        this.encryptedContent = so.encryptedContent != null ? (byte[]) so.encryptedContent.clone() : null;
        this.encodedParams = so.encodedParams != null ? (byte[]) so.encodedParams.clone() : null;
        this.sealAlg = so.sealAlg;
        this.paramsAlg = so.paramsAlg;
    }

    public final String getAlgorithm() {
        return this.sealAlg;
    }

    public final Object getObject(Key key) throws NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        try {
            Cipher cipher = Cipher.getInstance(this.sealAlg);
            if (this.paramsAlg != null && this.paramsAlg.length() != 0) {
                AlgorithmParameters params = AlgorithmParameters.getInstance(this.paramsAlg);
                params.init(this.encodedParams);
                cipher.init(2, key, params);
            } else {
                cipher.init(2, key);
            }
            byte[] serialized = cipher.doFinal(this.encryptedContent);
            return readSerialized(serialized);
        } catch (IllegalStateException e) {
            throw new NoSuchAlgorithmException(e.toString());
        } catch (InvalidAlgorithmParameterException e2) {
            throw new NoSuchAlgorithmException(e2.toString());
        } catch (BadPaddingException e3) {
            throw new NoSuchAlgorithmException(e3.toString());
        } catch (IllegalBlockSizeException e4) {
            throw new NoSuchAlgorithmException(e4.toString());
        } catch (NoSuchPaddingException e5) {
            throw new NoSuchAlgorithmException(e5.toString());
        }
    }

    public final Object getObject(Cipher c) throws BadPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException {
        if (c == null) {
            throw new NullPointerException("c == null");
        }
        byte[] serialized = c.doFinal(this.encryptedContent);
        return readSerialized(serialized);
    }

    public final Object getObject(Key key, String provider) throws NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("provider name empty or null");
        }
        try {
            Cipher cipher = Cipher.getInstance(this.sealAlg, provider);
            if (this.paramsAlg != null && this.paramsAlg.length() != 0) {
                AlgorithmParameters params = AlgorithmParameters.getInstance(this.paramsAlg);
                params.init(this.encodedParams);
                cipher.init(2, key, params);
            } else {
                cipher.init(2, key);
            }
            byte[] serialized = cipher.doFinal(this.encryptedContent);
            return readSerialized(serialized);
        } catch (IllegalStateException e) {
            throw new NoSuchAlgorithmException(e.toString());
        } catch (InvalidAlgorithmParameterException e2) {
            throw new NoSuchAlgorithmException(e2.toString());
        } catch (BadPaddingException e3) {
            throw new NoSuchAlgorithmException(e3.toString());
        } catch (IllegalBlockSizeException e4) {
            throw new NoSuchAlgorithmException(e4.toString());
        } catch (NoSuchPaddingException e5) {
            throw new NoSuchAlgorithmException(e5.toString());
        }
    }

    private static Object readSerialized(byte[] serialized) throws Throwable {
        ObjectInputStream ois;
        ObjectInputStream ois2 = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(serialized));
        } catch (Throwable th) {
            th = th;
        }
        try {
            Object object = ois.readObject();
            IoUtils.closeQuietly(ois);
            return object;
        } catch (Throwable th2) {
            th = th2;
            ois2 = ois;
            IoUtils.closeQuietly(ois2);
            throw th;
        }
    }
}
