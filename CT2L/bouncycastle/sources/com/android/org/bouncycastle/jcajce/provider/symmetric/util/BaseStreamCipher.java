package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.StreamBlockCipher;
import com.android.org.bouncycastle.crypto.StreamCipher;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;

public class BaseStreamCipher extends BaseWrapCipher implements PBE {
    private StreamCipher cipher;
    private int ivLength;
    private ParametersWithIV ivParam;
    private Class[] availableSpecs = {IvParameterSpec.class, PBEParameterSpec.class};
    private PBEParameterSpec pbeSpec = null;
    private String pbeAlgorithm = null;

    protected BaseStreamCipher(StreamCipher engine, int ivLength) {
        this.ivLength = 0;
        this.cipher = engine;
        this.ivLength = ivLength;
    }

    protected BaseStreamCipher(BlockCipher engine, int ivLength) {
        this.ivLength = 0;
        this.ivLength = ivLength;
        this.cipher = new StreamBlockCipher(engine);
    }

    @Override
    protected int engineGetBlockSize() {
        return 0;
    }

    @Override
    protected byte[] engineGetIV() {
        if (this.ivParam != null) {
            return this.ivParam.getIV();
        }
        return null;
    }

    @Override
    protected int engineGetKeySize(Key key) {
        return key.getEncoded().length * 8;
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return inputLen;
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        if (this.engineParams == null && this.pbeSpec != null) {
            try {
                AlgorithmParameters engineParams = AlgorithmParameters.getInstance(this.pbeAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
                engineParams.init(this.pbeSpec);
                return engineParams;
            } catch (Exception e) {
                return null;
            }
        }
        return this.engineParams;
    }

    @Override
    protected void engineSetMode(String mode) {
        if (!mode.equalsIgnoreCase("ECB")) {
            throw new IllegalArgumentException("can't support mode " + mode);
        }
    }

    @Override
    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        if (!padding.equalsIgnoreCase("NoPadding")) {
            throw new NoSuchPaddingException("Padding " + padding + " unknown.");
        }
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        CipherParameters param;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.engineParams = null;
        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Key for algorithm " + key.getAlgorithm() + " not suitable for symmetric enryption.");
        }
        if (key instanceof BCPBEKey) {
            BCPBEKey k = (BCPBEKey) key;
            if (k.getOID() != null) {
                this.pbeAlgorithm = k.getOID().getId();
            } else {
                this.pbeAlgorithm = k.getAlgorithm();
            }
            if (k.getParam() != null) {
                param = k.getParam();
                this.pbeSpec = new PBEParameterSpec(k.getSalt(), k.getIterationCount());
            } else if (params instanceof PBEParameterSpec) {
                param = PBE.Util.makePBEParameters(k, params, this.cipher.getAlgorithmName());
                this.pbeSpec = (PBEParameterSpec) params;
            } else {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
            if (k.getIvSize() != 0) {
                this.ivParam = (ParametersWithIV) param;
            }
        } else if (params == null) {
            param = new KeyParameter(key.getEncoded());
        } else if (params instanceof IvParameterSpec) {
            param = new ParametersWithIV(new KeyParameter(key.getEncoded()), ((IvParameterSpec) params).getIV());
            this.ivParam = (ParametersWithIV) param;
        } else {
            throw new InvalidAlgorithmParameterException("unknown parameter type.");
        }
        if (this.ivLength != 0 && !(param instanceof ParametersWithIV)) {
            SecureRandom ivRandom = random;
            if (ivRandom == null) {
                ivRandom = new SecureRandom();
            }
            if (opmode == 1 || opmode == 3) {
                byte[] iv = new byte[this.ivLength];
                ivRandom.nextBytes(iv);
                CipherParameters param2 = new ParametersWithIV(param, iv);
                this.ivParam = (ParametersWithIV) param2;
                param = param2;
            } else {
                throw new InvalidAlgorithmParameterException("no IV set when one expected");
            }
        }
        try {
            switch (opmode) {
                case 1:
                case 3:
                    this.cipher.init(true, param);
                    return;
                case 2:
                case 4:
                    this.cipher.init(false, param);
                    return;
                default:
                    throw new InvalidParameterException("unknown opmode " + opmode + " passed");
            }
        } catch (Exception e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec paramSpec = null;
        if (params != null) {
            for (int i = 0; i != this.availableSpecs.length; i++) {
                try {
                    paramSpec = params.getParameterSpec(this.availableSpecs[i]);
                    break;
                } catch (Exception e) {
                }
            }
            if (paramSpec == null) {
                throw new InvalidAlgorithmParameterException("can't handle parameter " + params.toString());
            }
        }
        engineInit(opmode, key, paramSpec, random);
        this.engineParams = params;
    }

    @Override
    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        try {
            engineInit(opmode, key, (AlgorithmParameterSpec) null, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    @Override
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        byte[] out = new byte[inputLen];
        this.cipher.processBytes(input, inputOffset, inputLen, out, 0);
        return out;
    }

    @Override
    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        try {
            this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
            return inputLen;
        } catch (DataLengthException e) {
            throw new ShortBufferException(e.getMessage());
        }
    }

    @Override
    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) {
        if (inputLen != 0) {
            byte[] out = engineUpdate(input, inputOffset, inputLen);
            this.cipher.reset();
            return out;
        }
        this.cipher.reset();
        byte[] out2 = new byte[0];
        return out2;
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) {
        if (inputLen != 0) {
            this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
        }
        this.cipher.reset();
        return inputLen;
    }
}
