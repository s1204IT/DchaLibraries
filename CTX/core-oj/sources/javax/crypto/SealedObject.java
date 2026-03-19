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

public class SealedObject implements Serializable {
    static final long serialVersionUID = 4482838265551344752L;
    protected byte[] encodedParams;
    private byte[] encryptedContent;
    private String paramsAlg;
    private String sealAlg;

    public SealedObject(Serializable serializable, Cipher cipher) throws IllegalBlockSizeException, IOException {
        this.encryptedContent = null;
        this.sealAlg = null;
        this.paramsAlg = null;
        this.encodedParams = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        try {
            objectOutputStream.writeObject(serializable);
            objectOutputStream.flush();
            try {
                this.encryptedContent = cipher.doFinal(byteArrayOutputStream.toByteArray());
            } catch (BadPaddingException e) {
            }
            if (cipher.getParameters() != null) {
                this.encodedParams = cipher.getParameters().getEncoded();
                this.paramsAlg = cipher.getParameters().getAlgorithm();
            }
            this.sealAlg = cipher.getAlgorithm();
        } finally {
            objectOutputStream.close();
        }
    }

    protected SealedObject(SealedObject sealedObject) {
        this.encryptedContent = null;
        this.sealAlg = null;
        this.paramsAlg = null;
        this.encodedParams = null;
        this.encryptedContent = (byte[]) sealedObject.encryptedContent.clone();
        this.sealAlg = sealedObject.sealAlg;
        this.paramsAlg = sealedObject.paramsAlg;
        if (sealedObject.encodedParams != null) {
            this.encodedParams = (byte[]) sealedObject.encodedParams.clone();
        } else {
            this.encodedParams = null;
        }
    }

    public final String getAlgorithm() {
        return this.sealAlg;
    }

    public final Object getObject(Key key) throws NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        try {
            return unseal(key, null);
        } catch (NoSuchProviderException e) {
            throw new NoSuchAlgorithmException("algorithm not found");
        } catch (BadPaddingException e2) {
            throw new InvalidKeyException(e2.getMessage());
        } catch (IllegalBlockSizeException e3) {
            throw new InvalidKeyException(e3.getMessage());
        }
    }

    public final Object getObject(Cipher cipher) throws BadPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException {
        extObjectInputStream extobjectinputstream = new extObjectInputStream(new ByteArrayInputStream(cipher.doFinal(this.encryptedContent)));
        try {
            return extobjectinputstream.readObject();
        } finally {
            extobjectinputstream.close();
        }
    }

    public final Object getObject(Key key, String str) throws NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException, NoSuchProviderException {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        try {
            return unseal(key, str);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    private Object unseal(Key key, String str) throws BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException, NoSuchProviderException {
        AlgorithmParameters algorithmParameters;
        Cipher cipher;
        if (this.encodedParams != null) {
            try {
                if (str != 0) {
                    algorithmParameters = AlgorithmParameters.getInstance(this.paramsAlg, (String) str);
                } else {
                    algorithmParameters = AlgorithmParameters.getInstance(this.paramsAlg);
                }
                algorithmParameters.init(this.encodedParams);
            } catch (NoSuchProviderException e) {
                if (str == 0) {
                    throw new NoSuchAlgorithmException(this.paramsAlg + " not found");
                }
                throw new NoSuchProviderException(e.getMessage());
            }
        } else {
            algorithmParameters = null;
        }
        try {
            if (str != 0) {
                cipher = Cipher.getInstance(this.sealAlg, (String) str);
            } else {
                cipher = Cipher.getInstance(this.sealAlg);
            }
            str = 2;
            try {
                if (algorithmParameters != null) {
                    cipher.init(2, key, algorithmParameters);
                } else {
                    cipher.init(2, key);
                }
                extObjectInputStream extobjectinputstream = new extObjectInputStream(new ByteArrayInputStream(cipher.doFinal(this.encryptedContent)));
                try {
                    return extobjectinputstream.readObject();
                } finally {
                    extobjectinputstream.close();
                }
            } catch (InvalidAlgorithmParameterException e2) {
                throw new RuntimeException(e2.getMessage());
            }
        } catch (NoSuchProviderException e3) {
            if (str == 0) {
                throw new NoSuchAlgorithmException(this.sealAlg + " not found");
            }
            throw new NoSuchProviderException(e3.getMessage());
        } catch (NoSuchPaddingException e4) {
            throw new NoSuchAlgorithmException("Padding that was used in sealing operation not available");
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.encryptedContent != null) {
            this.encryptedContent = (byte[]) this.encryptedContent.clone();
        }
        if (this.encodedParams != null) {
            this.encodedParams = (byte[]) this.encodedParams.clone();
        }
    }
}
