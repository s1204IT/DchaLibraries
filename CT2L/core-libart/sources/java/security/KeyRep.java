package java.security;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyRep implements Serializable {
    private static final long serialVersionUID = -4757683898830641853L;
    private final String algorithm;
    private byte[] encoded;
    private final String format;
    private final Type type;

    public enum Type {
        SECRET,
        PUBLIC,
        PRIVATE
    }

    public KeyRep(Type type, String algorithm, String format, byte[] encoded) {
        this.type = type;
        this.algorithm = algorithm;
        this.format = format;
        this.encoded = encoded;
        if (this.type == null) {
            throw new NullPointerException("type == null");
        }
        if (this.algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        if (this.format == null) {
            throw new NullPointerException("format == null");
        }
        if (this.encoded == null) {
            throw new NullPointerException("encoded == null");
        }
    }

    protected Object readResolve() throws ObjectStreamException {
        switch (this.type) {
            case SECRET:
                if ("RAW".equals(this.format)) {
                    try {
                        return new SecretKeySpec(this.encoded, this.algorithm);
                    } catch (IllegalArgumentException e) {
                        throw new NotSerializableException("Could not create SecretKeySpec: " + e);
                    }
                }
                throw new NotSerializableException("unrecognized type/format combination: " + this.type + "/" + this.format);
            case PUBLIC:
                if ("X.509".equals(this.format)) {
                    try {
                        KeyFactory kf = KeyFactory.getInstance(this.algorithm);
                        return kf.generatePublic(new X509EncodedKeySpec(this.encoded));
                    } catch (NoSuchAlgorithmException e2) {
                        throw new NotSerializableException("Could not resolve key: " + e2);
                    } catch (InvalidKeySpecException e3) {
                        throw new NotSerializableException("Could not resolve key: " + e3);
                    }
                }
                throw new NotSerializableException("unrecognized type/format combination: " + this.type + "/" + this.format);
            case PRIVATE:
                if ("PKCS#8".equals(this.format)) {
                    try {
                        KeyFactory kf2 = KeyFactory.getInstance(this.algorithm);
                        return kf2.generatePrivate(new PKCS8EncodedKeySpec(this.encoded));
                    } catch (NoSuchAlgorithmException e4) {
                        throw new NotSerializableException("Could not resolve key: " + e4);
                    } catch (InvalidKeySpecException e5) {
                        throw new NotSerializableException("Could not resolve key: " + e5);
                    }
                }
                throw new NotSerializableException("unrecognized type/format combination: " + this.type + "/" + this.format);
            default:
                throw new NotSerializableException("unrecognized key type: " + this.type);
        }
    }

    private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
        is.defaultReadObject();
        byte[] new_encoded = new byte[this.encoded.length];
        System.arraycopy(this.encoded, 0, new_encoded, 0, new_encoded.length);
        this.encoded = new_encoded;
    }
}
