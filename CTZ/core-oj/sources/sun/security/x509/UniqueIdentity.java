package sun.security.x509;

import java.io.IOException;
import sun.security.util.BitArray;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class UniqueIdentity {
    private BitArray id;

    public UniqueIdentity(BitArray bitArray) {
        this.id = bitArray;
    }

    public UniqueIdentity(byte[] bArr) {
        this.id = new BitArray(bArr.length * 8, bArr);
    }

    public UniqueIdentity(DerInputStream derInputStream) throws IOException {
        this.id = derInputStream.getDerValue().getUnalignedBitString(true);
    }

    public UniqueIdentity(DerValue derValue) throws IOException {
        this.id = derValue.getUnalignedBitString(true);
    }

    public String toString() {
        return "UniqueIdentity:" + this.id.toString() + "\n";
    }

    public void encode(DerOutputStream derOutputStream, byte b) throws IOException {
        byte[] byteArray = this.id.toByteArray();
        int length = (byteArray.length * 8) - this.id.length();
        derOutputStream.write(b);
        derOutputStream.putLength(byteArray.length + 1);
        derOutputStream.write(length);
        derOutputStream.write(byteArray);
    }

    public boolean[] getId() {
        if (this.id == null) {
            return null;
        }
        return this.id.toBooleanArray();
    }
}
