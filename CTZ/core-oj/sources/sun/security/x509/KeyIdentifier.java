package sun.security.x509;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import sun.misc.HexDumpEncoder;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class KeyIdentifier {
    private byte[] octetString;

    public KeyIdentifier(byte[] bArr) {
        this.octetString = (byte[]) bArr.clone();
    }

    public KeyIdentifier(DerValue derValue) throws IOException {
        this.octetString = derValue.getOctetString();
    }

    public KeyIdentifier(PublicKey publicKey) throws IOException {
        DerValue derValue = new DerValue(publicKey.getEncoded());
        if (derValue.tag != 48) {
            throw new IOException("PublicKey value is not a valid X.509 public key");
        }
        AlgorithmId.parse(derValue.data.getDerValue());
        byte[] byteArray = derValue.data.getUnalignedBitString().toByteArray();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(byteArray);
            this.octetString = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA1 not supported");
        }
    }

    public byte[] getIdentifier() {
        return (byte[]) this.octetString.clone();
    }

    public String toString() {
        return ("KeyIdentifier [\n" + new HexDumpEncoder().encodeBuffer(this.octetString)) + "]\n";
    }

    void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.putOctetString(this.octetString);
    }

    public int hashCode() {
        int i = 0;
        for (int i2 = 0; i2 < this.octetString.length; i2++) {
            i += this.octetString[i2] * i2;
        }
        return i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof KeyIdentifier)) {
            return false;
        }
        return Arrays.equals(this.octetString, ((KeyIdentifier) obj).octetString);
    }
}
