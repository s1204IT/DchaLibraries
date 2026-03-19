package java.security;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;
import sun.security.jca.JCAUtil;

public abstract class SignatureSpi {
    protected SecureRandom appRandom = null;

    @Deprecated
    protected abstract Object engineGetParameter(String str) throws InvalidParameterException;

    protected abstract void engineInitSign(PrivateKey privateKey) throws InvalidKeyException;

    protected abstract void engineInitVerify(PublicKey publicKey) throws InvalidKeyException;

    @Deprecated
    protected abstract void engineSetParameter(String str, Object obj) throws InvalidParameterException;

    protected abstract byte[] engineSign() throws SignatureException;

    protected abstract void engineUpdate(byte b) throws SignatureException;

    protected abstract void engineUpdate(byte[] bArr, int i, int i2) throws SignatureException;

    protected abstract boolean engineVerify(byte[] bArr) throws SignatureException;

    protected void engineInitSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
        this.appRandom = secureRandom;
        engineInitSign(privateKey);
    }

    protected void engineUpdate(ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return;
        }
        try {
            if (byteBuffer.hasArray()) {
                byte[] bArrArray = byteBuffer.array();
                int iArrayOffset = byteBuffer.arrayOffset();
                int iPosition = byteBuffer.position();
                int iLimit = byteBuffer.limit();
                engineUpdate(bArrArray, iArrayOffset + iPosition, iLimit - iPosition);
                byteBuffer.position(iLimit);
                return;
            }
            int iRemaining = byteBuffer.remaining();
            byte[] bArr = new byte[JCAUtil.getTempArraySize(iRemaining)];
            while (iRemaining > 0) {
                int iMin = Math.min(iRemaining, bArr.length);
                byteBuffer.get(bArr, 0, iMin);
                engineUpdate(bArr, 0, iMin);
                iRemaining -= iMin;
            }
        } catch (SignatureException e) {
            throw new ProviderException("update() failed", e);
        }
    }

    protected int engineSign(byte[] bArr, int i, int i2) throws SignatureException {
        byte[] bArrEngineSign = engineSign();
        if (i2 < bArrEngineSign.length) {
            throw new SignatureException("partial signatures not returned");
        }
        if (bArr.length - i < bArrEngineSign.length) {
            throw new SignatureException("insufficient space in the output buffer to store the signature");
        }
        System.arraycopy(bArrEngineSign, 0, bArr, i, bArrEngineSign.length);
        return bArrEngineSign.length;
    }

    protected boolean engineVerify(byte[] bArr, int i, int i2) throws SignatureException {
        byte[] bArr2 = new byte[i2];
        System.arraycopy(bArr, i, bArr2, 0, i2);
        return engineVerify(bArr2);
    }

    protected void engineSetParameter(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException();
    }

    protected AlgorithmParameters engineGetParameters() {
        throw new UnsupportedOperationException();
    }

    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
