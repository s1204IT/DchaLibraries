package java.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class SignedObject implements Serializable {
    private static final long serialVersionUID = 720502720485447167L;
    private byte[] content;
    private byte[] signature;
    private String thealgorithm;

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        byte[] tmp = new byte[this.content.length];
        System.arraycopy(this.content, 0, tmp, 0, this.content.length);
        this.content = tmp;
        byte[] tmp2 = new byte[this.signature.length];
        System.arraycopy(this.signature, 0, tmp2, 0, this.signature.length);
        this.signature = tmp2;
    }

    public SignedObject(Serializable object, PrivateKey signingKey, Signature signingEngine) throws SignatureException, IOException, InvalidKeyException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        try {
            oos.writeObject(object);
            oos.flush();
            oos.close();
            this.content = baos.toByteArray();
            signingEngine.initSign(signingKey);
            this.thealgorithm = signingEngine.getAlgorithm();
            signingEngine.update(this.content);
            this.signature = signingEngine.sign();
        } catch (Throwable th) {
            oos.close();
            throw th;
        }
    }

    public Object getObject() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(this.content));
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }

    public byte[] getSignature() {
        byte[] sig = new byte[this.signature.length];
        System.arraycopy(this.signature, 0, sig, 0, this.signature.length);
        return sig;
    }

    public String getAlgorithm() {
        return this.thealgorithm;
    }

    public boolean verify(PublicKey verificationKey, Signature verificationEngine) throws SignatureException, InvalidKeyException {
        verificationEngine.initVerify(verificationKey);
        verificationEngine.update(this.content);
        return verificationEngine.verify(this.signature);
    }
}
