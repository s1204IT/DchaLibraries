package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.StreamCipher;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.jcajce.PKCS12Key;
import com.android.org.bouncycastle.jcajce.PKCS12KeyWithParameters;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE;
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
    private Class[] availableSpecs;
    private StreamCipher cipher;
    private int digest;
    private int ivLength;
    private ParametersWithIV ivParam;
    private int keySizeInBits;
    private String pbeAlgorithm;
    private PBEParameterSpec pbeSpec;

    protected BaseStreamCipher(StreamCipher engine, int ivLength) {
        this(engine, ivLength, -1, -1);
    }

    protected BaseStreamCipher(StreamCipher engine, int ivLength, int keySizeInBits, int digest) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class};
        this.ivLength = 0;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.cipher = engine;
        this.ivLength = ivLength;
        this.keySizeInBits = keySizeInBits;
        this.digest = digest;
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
                AlgorithmParameters engineParams = createParametersInstance(this.pbeAlgorithm);
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
        if (mode.equalsIgnoreCase("ECB")) {
        } else {
            throw new IllegalArgumentException("can't support mode " + mode);
        }
    }

    @Override
    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        if (padding.equalsIgnoreCase("NoPadding")) {
        } else {
            throw new NoSuchPaddingException("Padding " + padding + " unknown.");
        }
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        CipherParameters cipherParameters;
        CipherParameters keyParameter;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.engineParams = null;
        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Key for algorithm " + key.getAlgorithm() + " not suitable for symmetric enryption.");
        }
        if (key instanceof PKCS12Key) {
            this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
            if ((key instanceof PKCS12KeyWithParameters) && this.pbeSpec == null) {
                this.pbeSpec = new PBEParameterSpec(key.getSalt(), key.getIterationCount());
            }
            keyParameter = PBE.Util.makePBEParameters(key.getEncoded(), 2, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
        } else if (key instanceof BCPBEKey) {
            if (key.getOID() != null) {
                this.pbeAlgorithm = key.getOID().getId();
            } else {
                this.pbeAlgorithm = key.getAlgorithm();
            }
            if (key.getParam() != null) {
                CipherParameters param = key.getParam();
                this.pbeSpec = new PBEParameterSpec(key.getSalt(), key.getIterationCount());
                cipherParameters = param;
            } else if (algorithmParameterSpec instanceof PBEParameterSpec) {
                CipherParameters cipherParametersMakePBEParameters = PBE.Util.makePBEParameters(key, algorithmParameterSpec, this.cipher.getAlgorithmName());
                this.pbeSpec = algorithmParameterSpec;
                cipherParameters = cipherParametersMakePBEParameters;
            } else {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
            keyParameter = cipherParameters;
            if (key.getIvSize() != 0) {
                this.ivParam = (ParametersWithIV) cipherParameters;
                keyParameter = cipherParameters;
            }
        } else if (algorithmParameterSpec == 0) {
            if (this.digest > 0) {
                throw new InvalidKeyException("Algorithm requires a PBE key");
            }
            keyParameter = new KeyParameter(key.getEncoded());
        } else if (algorithmParameterSpec instanceof IvParameterSpec) {
            ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key.getEncoded()), algorithmParameterSpec.getIV());
            this.ivParam = parametersWithIV;
            keyParameter = parametersWithIV;
        } else {
            throw new InvalidAlgorithmParameterException("unknown parameter type.");
        }
        CipherParameters cipherParameters2 = keyParameter;
        if (this.ivLength != 0) {
            boolean z = keyParameter instanceof ParametersWithIV;
            cipherParameters2 = keyParameter;
            if (!z) {
                SecureRandom secureRandom2 = secureRandom;
                if (secureRandom == null) {
                    secureRandom2 = new SecureRandom();
                }
                if (i == 1 || i == 3) {
                    byte[] bArr = new byte[this.ivLength];
                    secureRandom2.nextBytes(bArr);
                    ParametersWithIV parametersWithIV2 = new ParametersWithIV(keyParameter, bArr);
                    this.ivParam = parametersWithIV2;
                    cipherParameters2 = parametersWithIV2;
                } else {
                    throw new InvalidAlgorithmParameterException("no IV set when one expected");
                }
            }
        }
        try {
            switch (i) {
                case 1:
                case 3:
                    this.cipher.init(true, cipherParameters2);
                    return;
                case 2:
                case 4:
                    this.cipher.init(false, cipherParameters2);
                    return;
                default:
                    throw new InvalidParameterException("unknown opmode " + i + " passed");
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
        if (outputOffset + inputLen > output.length) {
            throw new ShortBufferException("output buffer too short for input.");
        }
        try {
            this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
            return inputLen;
        } catch (DataLengthException e) {
            throw new IllegalStateException(e.getMessage());
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
        return new byte[0];
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        if (outputOffset + inputLen > output.length) {
            throw new ShortBufferException("output buffer too short for input.");
        }
        if (inputLen != 0) {
            this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
        }
        this.cipher.reset();
        return inputLen;
    }
}
