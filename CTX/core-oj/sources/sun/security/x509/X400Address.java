package sun.security.x509;

import java.io.IOException;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class X400Address implements GeneralNameInterface {
    byte[] nameValue;

    public X400Address(byte[] bArr) {
        this.nameValue = null;
        this.nameValue = bArr;
    }

    public X400Address(DerValue derValue) throws IOException {
        this.nameValue = null;
        this.nameValue = derValue.toByteArray();
    }

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.putDerValue(new DerValue(this.nameValue));
    }

    public String toString() {
        return "X400Address: <DER-encoded value>";
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) throws UnsupportedOperationException {
        if (generalNameInterface != null && generalNameInterface.getType() == 3) {
            throw new UnsupportedOperationException("Narrowing, widening, and match are not supported for X400Address.");
        }
        return -1;
    }

    @Override
    public int subtreeDepth() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("subtreeDepth not supported for X400Address");
    }
}
