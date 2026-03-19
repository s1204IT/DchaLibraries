package java.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import sun.security.x509.X509CertImpl;

public final class SignedObject implements Serializable {
    private static final long serialVersionUID = 720502720485447167L;
    private byte[] content;
    private byte[] signature;
    private String thealgorithm;

    public SignedObject(Serializable serializable, PrivateKey privateKey, Signature signature) throws SignatureException, IOException, InvalidKeyException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(serializable);
        objectOutputStream.flush();
        objectOutputStream.close();
        this.content = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        sign(privateKey, signature);
    }

    public Object getObject() throws ClassNotFoundException, IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.content);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        Object object = objectInputStream.readObject();
        byteArrayInputStream.close();
        objectInputStream.close();
        return object;
    }

    public byte[] getSignature() {
        return (byte[]) this.signature.clone();
    }

    public String getAlgorithm() {
        return this.thealgorithm;
    }

    public boolean verify(PublicKey publicKey, Signature signature) throws SignatureException, InvalidKeyException {
        signature.initVerify(publicKey);
        signature.update((byte[]) this.content.clone());
        return signature.verify((byte[]) this.signature.clone());
    }

    private void sign(PrivateKey privateKey, Signature signature) throws SignatureException, InvalidKeyException {
        signature.initSign(privateKey);
        signature.update((byte[]) this.content.clone());
        this.signature = (byte[]) signature.sign().clone();
        this.thealgorithm = signature.getAlgorithm();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        this.content = (byte[]) ((byte[]) fields.get("content", (Object) null)).clone();
        this.signature = (byte[]) ((byte[]) fields.get(X509CertImpl.SIGNATURE, (Object) null)).clone();
        this.thealgorithm = (String) fields.get("thealgorithm", (Object) null);
    }
}
