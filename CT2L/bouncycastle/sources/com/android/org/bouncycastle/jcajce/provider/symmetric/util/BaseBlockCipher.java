package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.asn1.cms.GCMParameters;
import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.BufferedBlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.modes.AEADBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CBCBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CCMBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CFBBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CTSBlockCipher;
import com.android.org.bouncycastle.crypto.modes.GCMBlockCipher;
import com.android.org.bouncycastle.crypto.modes.OFBBlockCipher;
import com.android.org.bouncycastle.crypto.modes.SICBlockCipher;
import com.android.org.bouncycastle.crypto.paddings.BlockCipherPadding;
import com.android.org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import com.android.org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import com.android.org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import com.android.org.bouncycastle.crypto.paddings.TBCPadding;
import com.android.org.bouncycastle.crypto.paddings.X923Padding;
import com.android.org.bouncycastle.crypto.paddings.ZeroBytePadding;
import com.android.org.bouncycastle.crypto.params.AEADParameters;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.util.Strings;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;

public class BaseBlockCipher extends BaseWrapCipher implements PBE {
    private static final Class gcmSpecClass = lookup("javax.crypto.spec.GCMParameterSpec");
    private AEADParameters aeadParams;
    private Class[] availableSpecs;
    private BlockCipher baseEngine;
    private GenericBlockCipher cipher;
    private BlockCipherProvider engineProvider;
    private int ivLength;
    private ParametersWithIV ivParam;
    private String modeName;
    private boolean padded;
    private String pbeAlgorithm;
    private PBEParameterSpec pbeSpec;

    private interface GenericBlockCipher {
        int doFinal(byte[] bArr, int i) throws IllegalStateException, InvalidCipherTextException;

        String getAlgorithmName();

        int getOutputSize(int i);

        BlockCipher getUnderlyingCipher();

        int getUpdateOutputSize(int i);

        void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException;

        int processByte(byte b, byte[] bArr, int i) throws DataLengthException;

        int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException;

        void updateAAD(byte[] bArr, int i, int i2);

        boolean wrapOnNoPadding();
    }

    private static Class lookup(String className) {
        try {
            return BaseBlockCipher.class.getClassLoader().loadClass(className);
        } catch (Exception e) {
            return null;
        }
    }

    protected BaseBlockCipher(BlockCipher engine) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class, gcmSpecClass};
        this.ivLength = 0;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine;
        this.cipher = new BufferedGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(BlockCipherProvider provider) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class, gcmSpecClass};
        this.ivLength = 0;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = provider.get();
        this.engineProvider = provider;
        this.cipher = new BufferedGenericBlockCipher(provider.get());
    }

    protected BaseBlockCipher(AEADBlockCipher engine) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class, gcmSpecClass};
        this.ivLength = 0;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine.getUnderlyingCipher();
        this.ivLength = this.baseEngine.getBlockSize();
        this.cipher = new AEADGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(BlockCipher engine, int ivLength) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class, gcmSpecClass};
        this.ivLength = 0;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine;
        this.cipher = new BufferedGenericBlockCipher(engine);
        this.ivLength = ivLength / 8;
    }

    protected BaseBlockCipher(BufferedBlockCipher engine, int ivLength) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class, gcmSpecClass};
        this.ivLength = 0;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine.getUnderlyingCipher();
        this.cipher = new BufferedGenericBlockCipher(engine);
        this.ivLength = ivLength / 8;
    }

    @Override
    protected int engineGetBlockSize() {
        return this.baseEngine.getBlockSize();
    }

    @Override
    protected byte[] engineGetIV() {
        if (this.aeadParams != null) {
            return this.aeadParams.getNonce();
        }
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
        return this.cipher.getOutputSize(inputLen);
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        if (this.engineParams == null) {
            if (this.pbeSpec != null) {
                try {
                    this.engineParams = AlgorithmParameters.getInstance(this.pbeAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
                    this.engineParams.init(this.pbeSpec);
                } catch (Exception e) {
                    return null;
                }
            } else if (this.ivParam != null) {
                String name = this.cipher.getUnderlyingCipher().getAlgorithmName();
                if (name.indexOf(47) >= 0) {
                    name = name.substring(0, name.indexOf(47));
                }
                try {
                    this.engineParams = AlgorithmParameters.getInstance(name, BouncyCastleProvider.PROVIDER_NAME);
                    this.engineParams.init(this.ivParam.getIV());
                } catch (Exception e2) {
                    throw new RuntimeException(e2.toString());
                }
            } else if (this.aeadParams != null) {
                try {
                    this.engineParams = AlgorithmParameters.getInstance("GCM", BouncyCastleProvider.PROVIDER_NAME);
                    this.engineParams.init(new GCMParameters(this.aeadParams.getNonce(), this.aeadParams.getMacSize()).getEncoded());
                } catch (Exception e3) {
                    throw new RuntimeException(e3.toString());
                }
            }
        }
        return this.engineParams;
    }

    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        this.modeName = Strings.toUpperCase(mode);
        if (this.modeName.equals("ECB")) {
            this.ivLength = 0;
            this.cipher = new BufferedGenericBlockCipher(this.baseEngine);
            return;
        }
        if (this.modeName.equals("CBC")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new BufferedGenericBlockCipher(new CBCBlockCipher(this.baseEngine));
            return;
        }
        if (this.modeName.startsWith("OFB")) {
            this.ivLength = this.baseEngine.getBlockSize();
            if (this.modeName.length() != 3) {
                int wordSize = Integer.parseInt(this.modeName.substring(3));
                this.cipher = new BufferedGenericBlockCipher(new OFBBlockCipher(this.baseEngine, wordSize));
                return;
            } else {
                this.cipher = new BufferedGenericBlockCipher(new OFBBlockCipher(this.baseEngine, this.baseEngine.getBlockSize() * 8));
                return;
            }
        }
        if (this.modeName.startsWith("CFB")) {
            this.ivLength = this.baseEngine.getBlockSize();
            if (this.modeName.length() != 3) {
                int wordSize2 = Integer.parseInt(this.modeName.substring(3));
                this.cipher = new BufferedGenericBlockCipher(new CFBBlockCipher(this.baseEngine, wordSize2));
                return;
            } else {
                this.cipher = new BufferedGenericBlockCipher(new CFBBlockCipher(this.baseEngine, this.baseEngine.getBlockSize() * 8));
                return;
            }
        }
        if (this.modeName.startsWith("CTR")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(new SICBlockCipher(this.baseEngine)));
            return;
        }
        if (this.modeName.startsWith("CTS")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(new CBCBlockCipher(this.baseEngine)));
        } else if (this.modeName.startsWith("CCM")) {
            this.ivLength = 13;
            this.cipher = new AEADGenericBlockCipher(new CCMBlockCipher(this.baseEngine));
        } else {
            if (this.modeName.startsWith("GCM")) {
                this.ivLength = this.baseEngine.getBlockSize();
                this.cipher = new AEADGenericBlockCipher(new GCMBlockCipher(this.baseEngine));
                return;
            }
            throw new NoSuchAlgorithmException("can't support mode " + mode);
        }
    }

    @Override
    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        String paddingName = Strings.toUpperCase(padding);
        if (paddingName.equals("NOPADDING")) {
            if (this.cipher.wrapOnNoPadding()) {
                this.cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(this.cipher.getUnderlyingCipher()));
                return;
            }
            return;
        }
        if (paddingName.equals("WITHCTS")) {
            this.cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(this.cipher.getUnderlyingCipher()));
            return;
        }
        this.padded = true;
        if (isAEADModeName(this.modeName)) {
            throw new NoSuchPaddingException("Only NoPadding can be used with AEAD modes.");
        }
        if (paddingName.equals("PKCS5PADDING") || paddingName.equals("PKCS7PADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher());
            return;
        }
        if (paddingName.equals("ZEROBYTEPADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ZeroBytePadding());
            return;
        }
        if (paddingName.equals("ISO10126PADDING") || paddingName.equals("ISO10126-2PADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO10126d2Padding());
            return;
        }
        if (paddingName.equals("X9.23PADDING") || paddingName.equals("X923PADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new X923Padding());
            return;
        }
        if (paddingName.equals("ISO7816-4PADDING") || paddingName.equals("ISO9797-1PADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO7816d4Padding());
        } else {
            if (paddingName.equals("TBCPADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new TBCPadding());
                return;
            }
            throw new NoSuchPaddingException("Padding " + padding + " unknown.");
        }
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        CipherParameters keyParameter;
        CipherParameters cipherParameters;
        CipherParameters parametersWithIV;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.engineParams = null;
        this.aeadParams = null;
        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Key for algorithm " + key.getAlgorithm() + " not suitable for symmetric enryption.");
        }
        if (algorithmParameterSpec == null && this.baseEngine.getAlgorithmName().startsWith("RC5-64")) {
            throw new InvalidAlgorithmParameterException("RC5 requires an RC5ParametersSpec to be passed in.");
        }
        if (key instanceof BCPBEKey) {
            BCPBEKey bCPBEKey = (BCPBEKey) key;
            if (bCPBEKey.getOID() != null) {
                this.pbeAlgorithm = bCPBEKey.getOID().getId();
            } else {
                this.pbeAlgorithm = bCPBEKey.getAlgorithm();
            }
            if (bCPBEKey.getParam() != null) {
                CipherParameters param = bCPBEKey.getParam();
                parametersWithIV = param;
                if (algorithmParameterSpec instanceof IvParameterSpec) {
                    parametersWithIV = new ParametersWithIV(param, ((IvParameterSpec) algorithmParameterSpec).getIV());
                }
            } else if (algorithmParameterSpec instanceof PBEParameterSpec) {
                this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
                parametersWithIV = PBE.Util.makePBEParameters(bCPBEKey, algorithmParameterSpec, this.cipher.getUnderlyingCipher().getAlgorithmName());
            } else {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
            boolean z = parametersWithIV instanceof ParametersWithIV;
            keyParameter = parametersWithIV;
            if (z) {
                this.ivParam = (ParametersWithIV) parametersWithIV;
                keyParameter = parametersWithIV;
            }
        } else if (algorithmParameterSpec == null) {
            keyParameter = new KeyParameter(key.getEncoded());
        } else if (algorithmParameterSpec instanceof IvParameterSpec) {
            if (this.ivLength != 0) {
                IvParameterSpec ivParameterSpec = (IvParameterSpec) algorithmParameterSpec;
                if (ivParameterSpec.getIV().length != this.ivLength && !isAEADModeName(this.modeName)) {
                    throw new InvalidAlgorithmParameterException("IV must be " + this.ivLength + " bytes long.");
                }
                ParametersWithIV parametersWithIV2 = new ParametersWithIV(new KeyParameter(key.getEncoded()), ivParameterSpec.getIV());
                this.ivParam = parametersWithIV2;
                keyParameter = parametersWithIV2;
            } else {
                if (this.modeName != null && this.modeName.equals("ECB")) {
                    throw new InvalidAlgorithmParameterException("ECB mode does not use an IV");
                }
                keyParameter = new KeyParameter(key.getEncoded());
            }
        } else if (gcmSpecClass != null && gcmSpecClass.isInstance(algorithmParameterSpec)) {
            if (!isAEADModeName(this.modeName) && !(this.cipher instanceof AEADGenericBlockCipher)) {
                throw new InvalidAlgorithmParameterException("GCMParameterSpec can only be used with AEAD modes.");
            }
            try {
                AEADParameters aEADParameters = new AEADParameters(new KeyParameter(key.getEncoded()), ((Integer) gcmSpecClass.getDeclaredMethod("getTLen", new Class[0]).invoke(algorithmParameterSpec, new Object[0])).intValue(), (byte[]) gcmSpecClass.getDeclaredMethod("getIV", new Class[0]).invoke(algorithmParameterSpec, new Object[0]));
                this.aeadParams = aEADParameters;
                keyParameter = aEADParameters;
            } catch (Exception e) {
                throw new InvalidAlgorithmParameterException("Cannot process GCMParameterSpec.");
            }
        } else {
            throw new InvalidAlgorithmParameterException("unknown parameter type.");
        }
        if (this.ivLength == 0 || (keyParameter instanceof ParametersWithIV) || (keyParameter instanceof AEADParameters)) {
            cipherParameters = keyParameter;
        } else {
            SecureRandom secureRandom2 = secureRandom;
            if (secureRandom2 == null) {
                secureRandom2 = new SecureRandom();
            }
            if (i == 1 || i == 3) {
                byte[] bArr = new byte[this.ivLength];
                secureRandom2.nextBytes(bArr);
                ParametersWithIV parametersWithIV3 = new ParametersWithIV(keyParameter, bArr);
                this.ivParam = parametersWithIV3;
                cipherParameters = parametersWithIV3;
            } else {
                if (this.cipher.getUnderlyingCipher().getAlgorithmName().indexOf("PGPCFB") < 0) {
                    throw new InvalidAlgorithmParameterException("no IV set when one expected");
                }
                cipherParameters = keyParameter;
            }
        }
        CipherParameters parametersWithRandom = (secureRandom == null || !this.padded) ? cipherParameters : new ParametersWithRandom(cipherParameters, secureRandom);
        try {
            switch (i) {
                case 1:
                case 3:
                    this.cipher.init(true, parametersWithRandom);
                    return;
                case 2:
                case 4:
                    this.cipher.init(false, parametersWithRandom);
                    return;
                default:
                    throw new InvalidParameterException("unknown opmode " + i + " passed");
            }
        } catch (Exception e2) {
            throw new InvalidKeyException(e2.getMessage());
        }
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec paramSpec = null;
        if (params != null) {
            for (int i = 0; i != this.availableSpecs.length; i++) {
                if (this.availableSpecs[i] != null) {
                    try {
                        paramSpec = params.getParameterSpec(this.availableSpecs[i]);
                        break;
                    } catch (Exception e) {
                    }
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
    protected void engineUpdateAAD(byte[] input, int offset, int length) {
        this.cipher.updateAAD(input, offset, length);
    }

    @Override
    protected void engineUpdateAAD(ByteBuffer bytebuffer) {
        int offset = bytebuffer.arrayOffset() + bytebuffer.position();
        int length = bytebuffer.limit() - bytebuffer.position();
        engineUpdateAAD(bytebuffer.array(), offset, length);
    }

    @Override
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        int length = this.cipher.getUpdateOutputSize(inputLen);
        if (length > 0) {
            byte[] out = new byte[length];
            int len = this.cipher.processBytes(input, inputOffset, inputLen, out, 0);
            if (len == 0) {
                return null;
            }
            if (len == out.length) {
                return out;
            }
            byte[] tmp = new byte[len];
            System.arraycopy(out, 0, tmp, 0, len);
            return tmp;
        }
        this.cipher.processBytes(input, inputOffset, inputLen, null, 0);
        return null;
    }

    @Override
    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        try {
            return this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
        } catch (DataLengthException e) {
            throw new ShortBufferException(e.getMessage());
        }
    }

    @Override
    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws BadPaddingException, IllegalBlockSizeException {
        int len = 0;
        byte[] tmp = new byte[engineGetOutputSize(inputLen)];
        if (inputLen != 0) {
            len = this.cipher.processBytes(input, inputOffset, inputLen, tmp, 0);
        }
        try {
            int len2 = len + this.cipher.doFinal(tmp, len);
            if (len2 != tmp.length) {
                byte[] out = new byte[len2];
                System.arraycopy(tmp, 0, out, 0, len2);
                return out;
            }
            return tmp;
        } catch (DataLengthException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        } catch (InvalidCipherTextException e2) {
            throw new BadPaddingException(e2.getMessage());
        }
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        int len = 0;
        if (inputLen != 0) {
            try {
                len = this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
            } catch (DataLengthException e) {
                throw new IllegalBlockSizeException(e.getMessage());
            } catch (InvalidCipherTextException e2) {
                throw new BadPaddingException(e2.getMessage());
            } catch (OutputLengthException e3) {
                throw new ShortBufferException(e3.getMessage());
            }
        }
        return this.cipher.doFinal(output, outputOffset + len) + len;
    }

    private boolean isAEADModeName(String modeName) {
        return "CCM".equals(modeName) || "GCM".equals(modeName);
    }

    private static class BufferedGenericBlockCipher implements GenericBlockCipher {
        private BufferedBlockCipher cipher;

        BufferedGenericBlockCipher(BufferedBlockCipher cipher) {
            this.cipher = cipher;
        }

        BufferedGenericBlockCipher(BlockCipher cipher) {
            this.cipher = new PaddedBufferedBlockCipher(cipher);
        }

        BufferedGenericBlockCipher(BlockCipher cipher, BlockCipherPadding padding) {
            this.cipher = new PaddedBufferedBlockCipher(cipher, padding);
        }

        @Override
        public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
            this.cipher.init(forEncryption, params);
        }

        @Override
        public boolean wrapOnNoPadding() {
            return !(this.cipher instanceof CTSBlockCipher);
        }

        @Override
        public String getAlgorithmName() {
            return this.cipher.getUnderlyingCipher().getAlgorithmName();
        }

        @Override
        public BlockCipher getUnderlyingCipher() {
            return this.cipher.getUnderlyingCipher();
        }

        @Override
        public int getOutputSize(int len) {
            return this.cipher.getOutputSize(len);
        }

        @Override
        public int getUpdateOutputSize(int len) {
            return this.cipher.getUpdateOutputSize(len);
        }

        @Override
        public void updateAAD(byte[] input, int offset, int length) {
            throw new UnsupportedOperationException("AAD is not supported in the current mode.");
        }

        @Override
        public int processByte(byte in, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processByte(in, out, outOff);
        }

        @Override
        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processBytes(in, inOff, len, out, outOff);
        }

        @Override
        public int doFinal(byte[] out, int outOff) throws IllegalStateException, InvalidCipherTextException {
            return this.cipher.doFinal(out, outOff);
        }
    }

    private static class AEADGenericBlockCipher implements GenericBlockCipher {
        private AEADBlockCipher cipher;

        AEADGenericBlockCipher(AEADBlockCipher cipher) {
            this.cipher = cipher;
        }

        @Override
        public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
            this.cipher.init(forEncryption, params);
        }

        @Override
        public String getAlgorithmName() {
            return this.cipher.getUnderlyingCipher().getAlgorithmName();
        }

        @Override
        public boolean wrapOnNoPadding() {
            return false;
        }

        @Override
        public BlockCipher getUnderlyingCipher() {
            return this.cipher.getUnderlyingCipher();
        }

        @Override
        public int getOutputSize(int len) {
            return this.cipher.getOutputSize(len);
        }

        @Override
        public int getUpdateOutputSize(int len) {
            return this.cipher.getUpdateOutputSize(len);
        }

        @Override
        public void updateAAD(byte[] input, int offset, int length) {
            this.cipher.processAADBytes(input, offset, length);
        }

        @Override
        public int processByte(byte in, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processByte(in, out, outOff);
        }

        @Override
        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processBytes(in, inOff, len, out, outOff);
        }

        @Override
        public int doFinal(byte[] out, int outOff) throws IllegalStateException, InvalidCipherTextException {
            return this.cipher.doFinal(out, outOff);
        }
    }
}
