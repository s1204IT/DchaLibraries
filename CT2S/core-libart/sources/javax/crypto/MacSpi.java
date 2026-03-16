package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

public abstract class MacSpi {
    protected abstract byte[] engineDoFinal();

    protected abstract int engineGetMacLength();

    protected abstract void engineInit(Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract void engineReset();

    protected abstract void engineUpdate(byte b);

    protected abstract void engineUpdate(byte[] bArr, int i, int i2);

    protected void engineUpdate(ByteBuffer input) {
        if (input.hasRemaining()) {
            if (input.hasArray()) {
                byte[] bInput = input.array();
                int offset = input.arrayOffset();
                int position = input.position();
                int limit = input.limit();
                engineUpdate(bInput, offset + position, limit - position);
                input.position(limit);
                return;
            }
            byte[] bInput2 = new byte[input.limit() - input.position()];
            input.get(bInput2);
            engineUpdate(bInput2, 0, bInput2.length);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
