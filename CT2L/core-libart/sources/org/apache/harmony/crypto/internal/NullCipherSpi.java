package org.apache.harmony.crypto.internal;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

public class NullCipherSpi extends CipherSpi {
    @Override
    public void engineSetMode(String arg0) throws NoSuchAlgorithmException {
    }

    @Override
    public void engineSetPadding(String arg0) throws NoSuchPaddingException {
    }

    @Override
    public int engineGetBlockSize() {
        return 1;
    }

    @Override
    public int engineGetOutputSize(int inputLen) {
        return inputLen;
    }

    @Override
    public byte[] engineGetIV() {
        return new byte[8];
    }

    @Override
    public AlgorithmParameters engineGetParameters() {
        return null;
    }

    @Override
    public void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
    }

    @Override
    public void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
    }

    @Override
    public void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
    }

    @Override
    public byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        if (input == null) {
            return null;
        }
        byte[] result = new byte[inputLen];
        System.arraycopy(input, inputOffset, result, 0, inputLen);
        return result;
    }

    @Override
    public int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        if (input == null) {
            return 0;
        }
        System.arraycopy(input, inputOffset, output, outputOffset, inputLen);
        return inputLen;
    }

    @Override
    public int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        if (output == null) {
            throw new NullPointerException("output == null");
        }
        int result = input.limit() - input.position();
        try {
            output.put(input);
            return result;
        } catch (BufferOverflowException e) {
            throw new ShortBufferException("output buffer too small");
        }
    }

    @Override
    public byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws BadPaddingException, IllegalBlockSizeException {
        if (input == null) {
            return null;
        }
        return engineUpdate(input, inputOffset, inputLen);
    }

    @Override
    public int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        int result = engineUpdate(input, inputOffset, inputLen, output, outputOffset);
        return result;
    }

    @Override
    public int engineDoFinal(ByteBuffer input, ByteBuffer output) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        return engineUpdate(input, output);
    }

    @Override
    public byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }
}
