package javax.crypto;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

public abstract class CipherSpi {
    protected abstract int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException;

    protected abstract byte[] engineDoFinal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException;

    protected abstract int engineGetBlockSize();

    protected abstract byte[] engineGetIV();

    protected abstract int engineGetOutputSize(int i);

    protected abstract AlgorithmParameters engineGetParameters();

    protected abstract void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException;

    protected abstract void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract void engineSetMode(String str) throws NoSuchAlgorithmException;

    protected abstract void engineSetPadding(String str) throws NoSuchPaddingException;

    protected abstract int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException;

    protected abstract byte[] engineUpdate(byte[] bArr, int i, int i2);

    protected int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        byte[] bOutput;
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        if (output == null) {
            throw new NullPointerException("output == null");
        }
        int position = input.position();
        int limit = input.limit();
        if (limit - position <= 0) {
            return 0;
        }
        if (input.hasArray()) {
            byte[] bInput = input.array();
            int offset = input.arrayOffset();
            bOutput = engineUpdate(bInput, offset + position, limit - position);
            input.position(limit);
        } else {
            byte[] bInput2 = new byte[limit - position];
            input.get(bInput2);
            bOutput = engineUpdate(bInput2, 0, limit - position);
        }
        if (bOutput == null) {
            return 0;
        }
        if (output.remaining() < bOutput.length) {
            throw new ShortBufferException("output buffer too small");
        }
        try {
            output.put(bOutput);
            return bOutput.length;
        } catch (BufferOverflowException e) {
            throw new ShortBufferException("output buffer too small");
        }
    }

    protected void engineUpdateAAD(byte[] input, int inputOffset, int inputLen) {
        throw new UnsupportedOperationException("This cipher does not support Authenticated Encryption with Additional Data");
    }

    protected void engineUpdateAAD(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        int position = input.position();
        int limit = input.limit();
        if (limit - position > 0) {
            if (input.hasArray()) {
                byte[] bInput = input.array();
                int offset = input.arrayOffset();
                engineUpdateAAD(bInput, offset + position, limit - position);
                input.position(limit);
                return;
            }
            int len = limit - position;
            byte[] bInput2 = new byte[len];
            input.get(bInput2);
            engineUpdateAAD(bInput2, 0, len);
        }
    }

    protected int engineDoFinal(ByteBuffer input, ByteBuffer output) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        byte[] bOutput;
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        if (output == null) {
            throw new NullPointerException("output == null");
        }
        int position = input.position();
        int limit = input.limit();
        if (limit - position <= 0) {
            return 0;
        }
        if (input.hasArray()) {
            byte[] bInput = input.array();
            int offset = input.arrayOffset();
            bOutput = engineDoFinal(bInput, offset + position, limit - position);
            input.position(limit);
        } else {
            byte[] bInput2 = new byte[limit - position];
            input.get(bInput2);
            bOutput = engineDoFinal(bInput2, 0, limit - position);
        }
        if (output.remaining() < bOutput.length) {
            throw new ShortBufferException("output buffer too small");
        }
        try {
            output.put(bOutput);
            return bOutput.length;
        } catch (BufferOverflowException e) {
            throw new ShortBufferException("output buffer too small");
        }
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }
}
