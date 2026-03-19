package javax.crypto;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

final class NullCipherSpi extends CipherSpi {
    protected NullCipherSpi() {
    }

    @Override
    public void engineSetMode(String str) {
    }

    @Override
    public void engineSetPadding(String str) {
    }

    @Override
    protected int engineGetBlockSize() {
        return 1;
    }

    @Override
    protected int engineGetOutputSize(int i) {
        return i;
    }

    @Override
    protected byte[] engineGetIV() {
        return new byte[8];
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    @Override
    protected void engineInit(int i, Key key, SecureRandom secureRandom) {
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) {
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) {
    }

    @Override
    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            return null;
        }
        byte[] bArr2 = new byte[i2];
        System.arraycopy(bArr, i, bArr2, 0, i2);
        return bArr2;
    }

    @Override
    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        if (bArr == null) {
            return 0;
        }
        System.arraycopy(bArr, i, bArr2, i3, i2);
        return i2;
    }

    @Override
    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) {
        return engineUpdate(bArr, i, i2);
    }

    @Override
    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        return engineUpdate(bArr, i, i2, bArr2, i3);
    }

    @Override
    protected int engineGetKeySize(Key key) {
        return 0;
    }
}
