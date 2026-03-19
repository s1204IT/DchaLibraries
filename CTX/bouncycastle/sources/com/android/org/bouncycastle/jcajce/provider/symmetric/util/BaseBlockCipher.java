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
import com.android.org.bouncycastle.jcajce.PKCS12Key;
import com.android.org.bouncycastle.jcajce.PKCS12KeyWithParameters;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE;
import com.android.org.bouncycastle.jcajce.spec.AEADParameterSpec;
import com.android.org.bouncycastle.util.Strings;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class BaseBlockCipher extends BaseWrapCipher implements PBE {
    private static final Class gcmSpecClass = lookup("javax.crypto.spec.GCMParameterSpec");
    private AEADParameters aeadParams;
    private Class[] availableSpecs;
    private BlockCipher baseEngine;
    private GenericBlockCipher cipher;
    private int digest;
    private BlockCipherProvider engineProvider;
    private boolean fixedIv;
    private int ivLength;
    private ParametersWithIV ivParam;
    private int keySizeInBits;
    private String modeName;
    private boolean padded;
    private String pbeAlgorithm;
    private PBEParameterSpec pbeSpec;
    private int scheme;

    private interface GenericBlockCipher {
        int doFinal(byte[] bArr, int i) throws IllegalStateException, BadPaddingException;

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

    private static Class lookup(String str) {
        try {
            return BaseBlockCipher.class.getClassLoader().loadClass(str);
        } catch (Exception e) {
            return null;
        }
    }

    protected BaseBlockCipher(BlockCipher blockCipher) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = blockCipher;
        this.cipher = new BufferedGenericBlockCipher(blockCipher);
    }

    protected BaseBlockCipher(BlockCipher blockCipher, int i, int i2, int i3, int i4) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = blockCipher;
        this.scheme = i;
        this.digest = i2;
        this.keySizeInBits = i3;
        this.ivLength = i4;
        this.cipher = new BufferedGenericBlockCipher(blockCipher);
    }

    protected BaseBlockCipher(BlockCipherProvider blockCipherProvider) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = blockCipherProvider.get();
        this.engineProvider = blockCipherProvider;
        this.cipher = new BufferedGenericBlockCipher(blockCipherProvider.get());
    }

    protected BaseBlockCipher(AEADBlockCipher aEADBlockCipher) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = aEADBlockCipher.getUnderlyingCipher();
        this.ivLength = this.baseEngine.getBlockSize();
        this.cipher = new AEADGenericBlockCipher(aEADBlockCipher);
    }

    protected BaseBlockCipher(AEADBlockCipher aEADBlockCipher, boolean z, int i) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = aEADBlockCipher.getUnderlyingCipher();
        this.fixedIv = z;
        this.ivLength = i;
        this.cipher = new AEADGenericBlockCipher(aEADBlockCipher);
    }

    protected BaseBlockCipher(BlockCipher blockCipher, int i) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = blockCipher;
        this.cipher = new BufferedGenericBlockCipher(blockCipher);
        this.ivLength = i / 8;
    }

    protected BaseBlockCipher(BufferedBlockCipher bufferedBlockCipher, int i) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = bufferedBlockCipher.getUnderlyingCipher();
        this.cipher = new BufferedGenericBlockCipher(bufferedBlockCipher);
        this.ivLength = i / 8;
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
    protected int engineGetOutputSize(int i) {
        return this.cipher.getOutputSize(i);
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        if (this.engineParams == null) {
            if (this.pbeSpec != null) {
                try {
                    this.engineParams = createParametersInstance(this.pbeAlgorithm);
                    this.engineParams.init(this.pbeSpec);
                } catch (Exception e) {
                    return null;
                }
            } else if (this.aeadParams != null) {
                try {
                    this.engineParams = createParametersInstance("GCM");
                    this.engineParams.init(new GCMParameters(this.aeadParams.getNonce(), this.aeadParams.getMacSize() / 8).getEncoded());
                } catch (Exception e2) {
                    throw new RuntimeException(e2.toString());
                }
            } else if (this.ivParam != null) {
                String algorithmName = this.cipher.getUnderlyingCipher().getAlgorithmName();
                if (algorithmName.indexOf(47) >= 0) {
                    algorithmName = algorithmName.substring(0, algorithmName.indexOf(47));
                }
                try {
                    this.engineParams = createParametersInstance(algorithmName);
                    this.engineParams.init(new IvParameterSpec(this.ivParam.getIV()));
                } catch (Exception e3) {
                    throw new RuntimeException(e3.toString());
                }
            }
        }
        return this.engineParams;
    }

    @Override
    protected void engineSetMode(String str) throws NoSuchAlgorithmException {
        this.modeName = Strings.toUpperCase(str);
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
                this.cipher = new BufferedGenericBlockCipher(new OFBBlockCipher(this.baseEngine, Integer.parseInt(this.modeName.substring(3))));
                return;
            } else {
                this.cipher = new BufferedGenericBlockCipher(new OFBBlockCipher(this.baseEngine, 8 * this.baseEngine.getBlockSize()));
                return;
            }
        }
        if (this.modeName.startsWith("CFB")) {
            this.ivLength = this.baseEngine.getBlockSize();
            if (this.modeName.length() != 3) {
                this.cipher = new BufferedGenericBlockCipher(new CFBBlockCipher(this.baseEngine, Integer.parseInt(this.modeName.substring(3))));
                return;
            } else {
                this.cipher = new BufferedGenericBlockCipher(new CFBBlockCipher(this.baseEngine, 8 * this.baseEngine.getBlockSize()));
                return;
            }
        }
        if (this.modeName.startsWith("CTR")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.fixedIv = false;
            this.cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(new SICBlockCipher(this.baseEngine)));
            return;
        }
        if (this.modeName.startsWith("CTS")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(new CBCBlockCipher(this.baseEngine)));
            return;
        }
        if (this.modeName.startsWith("CCM")) {
            this.ivLength = 13;
            this.cipher = new AEADGenericBlockCipher(new CCMBlockCipher(this.baseEngine));
        } else if (this.modeName.startsWith("GCM")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new AEADGenericBlockCipher(new GCMBlockCipher(this.baseEngine));
        } else {
            throw new NoSuchAlgorithmException("can't support mode " + str);
        }
    }

    @Override
    protected void engineSetPadding(String str) throws NoSuchPaddingException {
        String upperCase = Strings.toUpperCase(str);
        if (upperCase.equals("NOPADDING")) {
            if (this.cipher.wrapOnNoPadding()) {
                this.cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(this.cipher.getUnderlyingCipher()));
                return;
            }
            return;
        }
        if (upperCase.equals("WITHCTS")) {
            this.cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(this.cipher.getUnderlyingCipher()));
            return;
        }
        this.padded = true;
        if (isAEADModeName(this.modeName)) {
            throw new NoSuchPaddingException("Only NoPadding can be used with AEAD modes.");
        }
        if (upperCase.equals("PKCS5PADDING") || upperCase.equals("PKCS7PADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher());
            return;
        }
        if (upperCase.equals("ZEROBYTEPADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ZeroBytePadding());
            return;
        }
        if (upperCase.equals("ISO10126PADDING") || upperCase.equals("ISO10126-2PADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO10126d2Padding());
            return;
        }
        if (upperCase.equals("X9.23PADDING") || upperCase.equals("X923PADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new X923Padding());
            return;
        }
        if (upperCase.equals("ISO7816-4PADDING") || upperCase.equals("ISO9797-1PADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO7816d4Padding());
            return;
        }
        if (upperCase.equals("TBCPADDING")) {
            this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new TBCPadding());
            return;
        }
        throw new NoSuchPaddingException("Padding " + str + " unknown.");
    }

    private boolean isBCPBEKeyWithoutIV(Key key) {
        return (key instanceof BCPBEKey) && !(((BCPBEKey) key).getParam() instanceof ParametersWithIV);
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        CipherParameters cipherParametersMakePBEParameters;
        ?? aEADParameters;
        KeyParameter keyParameter;
        ParametersWithIV parametersWithIV;
        int i2;
        ?? parametersWithRandom;
        SecureRandom secureRandom2;
        KeyParameter keyParameter2;
        CipherParameters cipherParametersMakePBEParameters2;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.engineParams = null;
        this.aeadParams = null;
        if (!(key instanceof SecretKey)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Key for algorithm ");
            sb.append(key != null ? key.getAlgorithm() : null);
            sb.append(" not suitable for symmetric enryption.");
            throw new InvalidKeyException(sb.toString());
        }
        if (algorithmParameterSpec == null && this.baseEngine.getAlgorithmName().startsWith("RC5-64")) {
            throw new InvalidAlgorithmParameterException("RC5 requires an RC5ParametersSpec to be passed in.");
        }
        if ((this.scheme == 2 || (key instanceof PKCS12Key)) && !isBCPBEKeyWithoutIV(key)) {
            try {
                SecretKey secretKey = (SecretKey) key;
                if (algorithmParameterSpec instanceof PBEParameterSpec) {
                    this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
                }
                boolean z = secretKey instanceof PBEKey;
                if (z && this.pbeSpec == null) {
                    PBEKey pBEKey = (PBEKey) secretKey;
                    if (pBEKey.getSalt() == null) {
                        throw new InvalidAlgorithmParameterException("PBEKey requires parameters to specify salt");
                    }
                    this.pbeSpec = new PBEParameterSpec(pBEKey.getSalt(), pBEKey.getIterationCount());
                }
                if (this.pbeSpec == null && !z) {
                    throw new InvalidKeyException("Algorithm requires a PBE key");
                }
                if (key instanceof BCPBEKey) {
                    CipherParameters param = ((BCPBEKey) key).getParam();
                    boolean z2 = param instanceof ParametersWithIV;
                    cipherParametersMakePBEParameters = param;
                    if (!z2) {
                        if (param == null) {
                            throw new AssertionError("Unreachable code");
                        }
                        throw new InvalidKeyException("Algorithm requires a PBE key suitable for PKCS12");
                    }
                } else {
                    cipherParametersMakePBEParameters = PBE.Util.makePBEParameters(secretKey.getEncoded(), 2, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
                }
                boolean z3 = cipherParametersMakePBEParameters instanceof ParametersWithIV;
                aEADParameters = cipherParametersMakePBEParameters;
                if (z3) {
                    this.ivParam = (ParametersWithIV) cipherParametersMakePBEParameters;
                    aEADParameters = cipherParametersMakePBEParameters;
                }
            } catch (Exception e) {
                throw new InvalidKeyException("PKCS12 requires a SecretKey/PBEKey");
            }
        } else if (key instanceof BCPBEKey) {
            BCPBEKey bCPBEKey = (BCPBEKey) key;
            if (bCPBEKey.getOID() != null) {
                this.pbeAlgorithm = bCPBEKey.getOID().getId();
            } else {
                this.pbeAlgorithm = bCPBEKey.getAlgorithm();
            }
            if (bCPBEKey.getParam() != null) {
                cipherParametersMakePBEParameters2 = adjustParameters(algorithmParameterSpec, bCPBEKey.getParam());
            } else if (algorithmParameterSpec instanceof PBEParameterSpec) {
                this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
                if (this.pbeSpec.getSalt().length != 0 && this.pbeSpec.getIterationCount() > 0) {
                    bCPBEKey = new BCPBEKey(bCPBEKey.getAlgorithm(), bCPBEKey.getOID(), bCPBEKey.getType(), bCPBEKey.getDigest(), bCPBEKey.getKeySize(), bCPBEKey.getIvSize(), new PBEKeySpec(bCPBEKey.getPassword(), this.pbeSpec.getSalt(), this.pbeSpec.getIterationCount(), bCPBEKey.getKeySize()), null);
                }
                cipherParametersMakePBEParameters2 = PBE.Util.makePBEParameters(bCPBEKey, algorithmParameterSpec, this.cipher.getUnderlyingCipher().getAlgorithmName());
            } else {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
            boolean z4 = cipherParametersMakePBEParameters2 instanceof ParametersWithIV;
            aEADParameters = cipherParametersMakePBEParameters2;
            if (z4) {
                this.ivParam = (ParametersWithIV) cipherParametersMakePBEParameters2;
                aEADParameters = cipherParametersMakePBEParameters2;
            }
        } else if (key instanceof PBEKey) {
            PBEKey pBEKey2 = (PBEKey) key;
            this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
            if ((pBEKey2 instanceof PKCS12KeyWithParameters) && this.pbeSpec == null) {
                this.pbeSpec = new PBEParameterSpec(pBEKey2.getSalt(), pBEKey2.getIterationCount());
            }
            CipherParameters cipherParametersMakePBEParameters3 = PBE.Util.makePBEParameters(pBEKey2.getEncoded(), this.scheme, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
            boolean z5 = cipherParametersMakePBEParameters3 instanceof ParametersWithIV;
            aEADParameters = cipherParametersMakePBEParameters3;
            if (z5) {
                this.ivParam = (ParametersWithIV) cipherParametersMakePBEParameters3;
                aEADParameters = cipherParametersMakePBEParameters3;
            }
        } else {
            if (this.scheme == 0 || this.scheme == 4 || this.scheme == 1 || this.scheme == 5) {
                throw new InvalidKeyException("Algorithm requires a PBE key");
            }
            aEADParameters = new KeyParameter(key.getEncoded());
        }
        if (algorithmParameterSpec instanceof AEADParameterSpec) {
            if (!isAEADModeName(this.modeName) && !(this.cipher instanceof AEADGenericBlockCipher)) {
                throw new InvalidAlgorithmParameterException("AEADParameterSpec can only be used with AEAD modes.");
            }
            AEADParameterSpec aEADParameterSpec = (AEADParameterSpec) algorithmParameterSpec;
            if (aEADParameters instanceof ParametersWithIV) {
                keyParameter2 = (KeyParameter) ((ParametersWithIV) aEADParameters).getParameters();
            } else {
                keyParameter2 = (KeyParameter) aEADParameters;
            }
            aEADParameters = new AEADParameters(keyParameter2, aEADParameterSpec.getMacSizeInBits(), aEADParameterSpec.getNonce(), aEADParameterSpec.getAssociatedData());
            this.aeadParams = aEADParameters;
        } else if (algorithmParameterSpec instanceof IvParameterSpec) {
            if (this.ivLength != 0) {
                IvParameterSpec ivParameterSpec = (IvParameterSpec) algorithmParameterSpec;
                if (ivParameterSpec.getIV().length != this.ivLength && !(this.cipher instanceof AEADGenericBlockCipher) && this.fixedIv) {
                    throw new InvalidAlgorithmParameterException("IV must be " + this.ivLength + " bytes long.");
                }
                if (aEADParameters instanceof ParametersWithIV) {
                    parametersWithIV = new ParametersWithIV(((ParametersWithIV) aEADParameters).getParameters(), ivParameterSpec.getIV());
                } else {
                    parametersWithIV = new ParametersWithIV(aEADParameters, ivParameterSpec.getIV());
                }
                aEADParameters = parametersWithIV;
                this.ivParam = (ParametersWithIV) aEADParameters;
            } else if (this.modeName != null && this.modeName.equals("ECB")) {
                throw new InvalidAlgorithmParameterException("ECB mode does not use an IV");
            }
        } else if (gcmSpecClass != null && gcmSpecClass.isInstance(algorithmParameterSpec)) {
            if (!isAEADModeName(this.modeName) && !(this.cipher instanceof AEADGenericBlockCipher)) {
                throw new InvalidAlgorithmParameterException("GCMParameterSpec can only be used with AEAD modes.");
            }
            try {
                Method declaredMethod = gcmSpecClass.getDeclaredMethod("getTLen", new Class[0]);
                Method declaredMethod2 = gcmSpecClass.getDeclaredMethod("getIV", new Class[0]);
                if (aEADParameters instanceof ParametersWithIV) {
                    keyParameter = (KeyParameter) ((ParametersWithIV) aEADParameters).getParameters();
                } else {
                    keyParameter = (KeyParameter) aEADParameters;
                }
                AEADParameters aEADParameters2 = new AEADParameters(keyParameter, ((Integer) declaredMethod.invoke(algorithmParameterSpec, new Object[0])).intValue(), (byte[]) declaredMethod2.invoke(algorithmParameterSpec, new Object[0]));
                this.aeadParams = aEADParameters2;
                aEADParameters = aEADParameters2;
            } catch (Exception e2) {
                throw new InvalidAlgorithmParameterException("Cannot process GCMParameterSpec.");
            }
        } else if (algorithmParameterSpec != null && !(algorithmParameterSpec instanceof PBEParameterSpec)) {
            throw new InvalidAlgorithmParameterException("unknown parameter type.");
        }
        try {
            if (this.ivLength != 0 && !(aEADParameters instanceof ParametersWithIV) && !(aEADParameters instanceof AEADParameters)) {
                if (secureRandom == null) {
                    secureRandom2 = new SecureRandom();
                } else {
                    secureRandom2 = secureRandom;
                }
                i2 = i;
                if (i2 == 1 || i2 == 3) {
                    byte[] bArr = new byte[this.ivLength];
                    if (!isBCPBEKeyWithoutIV(key)) {
                        secureRandom2.nextBytes(bArr);
                    } else {
                        System.err.println(" ******** DEPRECATED FUNCTIONALITY ********");
                        System.err.println(" * You have initialized a cipher with a PBE key with no IV and");
                        System.err.println(" * have not provided an IV in the AlgorithmParameterSpec.  This");
                        System.err.println(" * configuration is deprecated.  The cipher will be initialized");
                        System.err.println(" * with an all-zero IV, but in a future release this call will");
                        System.err.println(" * throw an exception.");
                        new InvalidAlgorithmParameterException("No IV set when using PBE key").printStackTrace(System.err);
                    }
                    ParametersWithIV parametersWithIV2 = new ParametersWithIV(aEADParameters, bArr);
                    this.ivParam = parametersWithIV2;
                    parametersWithRandom = parametersWithIV2;
                } else if (this.cipher.getUnderlyingCipher().getAlgorithmName().indexOf("PGPCFB") < 0) {
                    if (!isBCPBEKeyWithoutIV(key)) {
                        throw new InvalidAlgorithmParameterException("no IV set when one expected");
                    }
                    System.err.println(" ******** DEPRECATED FUNCTIONALITY ********");
                    System.err.println(" * You have initialized a cipher with a PBE key with no IV and");
                    System.err.println(" * have not provided an IV in the AlgorithmParameterSpec.  This");
                    System.err.println(" * configuration is deprecated.  The cipher will be initialized");
                    System.err.println(" * with an all-zero IV, but in a future release this call will");
                    System.err.println(" * throw an exception.");
                    new InvalidAlgorithmParameterException("No IV set when using PBE key").printStackTrace(System.err);
                    ParametersWithIV parametersWithIV3 = new ParametersWithIV(aEADParameters, new byte[this.ivLength]);
                    this.ivParam = parametersWithIV3;
                    parametersWithRandom = parametersWithIV3;
                }
                if (secureRandom != null && this.padded) {
                    parametersWithRandom = new ParametersWithRandom(parametersWithRandom, secureRandom);
                }
                switch (i2) {
                    case 1:
                    case 3:
                        this.cipher.init(true, parametersWithRandom);
                        break;
                    case 2:
                    case 4:
                        this.cipher.init(false, parametersWithRandom);
                        break;
                    default:
                        throw new InvalidParameterException("unknown opmode " + i2 + " passed");
                }
                if (!(this.cipher instanceof AEADGenericBlockCipher) && this.aeadParams == null) {
                    this.aeadParams = new AEADParameters((KeyParameter) this.ivParam.getParameters(), ((AEADGenericBlockCipher) this.cipher).cipher.getMac().length * 8, this.ivParam.getIV());
                    return;
                }
                return;
            }
            i2 = i;
            switch (i2) {
            }
            if (!(this.cipher instanceof AEADGenericBlockCipher)) {
                return;
            } else {
                return;
            }
        } catch (Exception e3) {
            throw new InvalidKeyOrParametersException(e3.getMessage(), e3);
        }
        parametersWithRandom = aEADParameters;
        if (secureRandom != null) {
            parametersWithRandom = new ParametersWithRandom(parametersWithRandom, secureRandom);
        }
    }

    private CipherParameters adjustParameters(AlgorithmParameterSpec algorithmParameterSpec, CipherParameters cipherParameters) {
        if (cipherParameters instanceof ParametersWithIV) {
            CipherParameters parameters = ((ParametersWithIV) cipherParameters).getParameters();
            if (algorithmParameterSpec instanceof IvParameterSpec) {
                this.ivParam = new ParametersWithIV(parameters, ((IvParameterSpec) algorithmParameterSpec).getIV());
                return this.ivParam;
            }
            return cipherParameters;
        }
        if (algorithmParameterSpec instanceof IvParameterSpec) {
            this.ivParam = new ParametersWithIV(cipherParameters, ((IvParameterSpec) algorithmParameterSpec).getIV());
            return this.ivParam;
        }
        return cipherParameters;
    }

    @Override
    protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidParameterSpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec parameterSpec = null;
        if (algorithmParameters != null) {
            int i2 = 0;
            while (true) {
                if (i2 == this.availableSpecs.length) {
                    break;
                }
                if (this.availableSpecs[i2] != null) {
                    try {
                        parameterSpec = algorithmParameters.getParameterSpec(this.availableSpecs[i2]);
                        break;
                    } catch (Exception e) {
                        i2++;
                    }
                }
                i2++;
            }
            if (parameterSpec == null) {
                throw new InvalidAlgorithmParameterException("can't handle parameter " + algorithmParameters.toString());
            }
        }
        engineInit(i, key, parameterSpec, secureRandom);
        this.engineParams = algorithmParameters;
    }

    @Override
    protected void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        try {
            engineInit(i, key, (AlgorithmParameterSpec) null, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    @Override
    protected void engineUpdateAAD(byte[] bArr, int i, int i2) {
        this.cipher.updateAAD(bArr, i, i2);
    }

    @Override
    protected void engineUpdateAAD(ByteBuffer byteBuffer) {
        engineUpdateAAD(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.limit() - byteBuffer.position());
    }

    @Override
    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        int updateOutputSize = this.cipher.getUpdateOutputSize(i2);
        if (updateOutputSize > 0) {
            byte[] bArr2 = new byte[updateOutputSize];
            int iProcessBytes = this.cipher.processBytes(bArr, i, i2, bArr2, 0);
            if (iProcessBytes == 0) {
                return null;
            }
            if (iProcessBytes != bArr2.length) {
                byte[] bArr3 = new byte[iProcessBytes];
                System.arraycopy(bArr2, 0, bArr3, 0, iProcessBytes);
                return bArr3;
            }
            return bArr2;
        }
        this.cipher.processBytes(bArr, i, i2, null, 0);
        return null;
    }

    @Override
    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException {
        if (this.cipher.getUpdateOutputSize(i2) + i3 > bArr2.length) {
            throw new ShortBufferException("output buffer too short for input.");
        }
        try {
            return this.cipher.processBytes(bArr, i, i2, bArr2, i3);
        } catch (DataLengthException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    @Override
    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) throws BadPaddingException, IllegalBlockSizeException {
        int iProcessBytes;
        byte[] bArr2 = new byte[engineGetOutputSize(i2)];
        if (i2 != 0) {
            iProcessBytes = this.cipher.processBytes(bArr, i, i2, bArr2, 0);
        } else {
            iProcessBytes = 0;
        }
        try {
            int iDoFinal = iProcessBytes + this.cipher.doFinal(bArr2, iProcessBytes);
            if (iDoFinal == bArr2.length) {
                return bArr2;
            }
            byte[] bArr3 = new byte[iDoFinal];
            System.arraycopy(bArr2, 0, bArr3, 0, iDoFinal);
            return bArr3;
        } catch (DataLengthException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        }
    }

    @Override
    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        int iProcessBytes;
        if (engineGetOutputSize(i2) + i3 > bArr2.length) {
            throw new ShortBufferException("output buffer too short for input.");
        }
        if (i2 != 0) {
            try {
                iProcessBytes = this.cipher.processBytes(bArr, i, i2, bArr2, i3);
            } catch (OutputLengthException e) {
                throw new IllegalBlockSizeException(e.getMessage());
            } catch (DataLengthException e2) {
                throw new IllegalBlockSizeException(e2.getMessage());
            }
        } else {
            iProcessBytes = 0;
        }
        return iProcessBytes + this.cipher.doFinal(bArr2, i3 + iProcessBytes);
    }

    private boolean isAEADModeName(String str) {
        return "CCM".equals(str) || "GCM".equals(str);
    }

    private static class BufferedGenericBlockCipher implements GenericBlockCipher {
        private BufferedBlockCipher cipher;

        BufferedGenericBlockCipher(BufferedBlockCipher bufferedBlockCipher) {
            this.cipher = bufferedBlockCipher;
        }

        BufferedGenericBlockCipher(BlockCipher blockCipher) {
            this.cipher = new PaddedBufferedBlockCipher(blockCipher);
        }

        BufferedGenericBlockCipher(BlockCipher blockCipher, BlockCipherPadding blockCipherPadding) {
            this.cipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);
        }

        @Override
        public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
            this.cipher.init(z, cipherParameters);
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
        public int getOutputSize(int i) {
            return this.cipher.getOutputSize(i);
        }

        @Override
        public int getUpdateOutputSize(int i) {
            return this.cipher.getUpdateOutputSize(i);
        }

        @Override
        public void updateAAD(byte[] bArr, int i, int i2) {
            throw new UnsupportedOperationException("AAD is not supported in the current mode.");
        }

        @Override
        public int processByte(byte b, byte[] bArr, int i) throws DataLengthException {
            return this.cipher.processByte(b, bArr, i);
        }

        @Override
        public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
            return this.cipher.processBytes(bArr, i, i2, bArr2, i3);
        }

        @Override
        public int doFinal(byte[] bArr, int i) throws IllegalStateException, BadPaddingException {
            try {
                return this.cipher.doFinal(bArr, i);
            } catch (InvalidCipherTextException e) {
                throw new BadPaddingException(e.getMessage());
            }
        }
    }

    private static class AEADGenericBlockCipher implements GenericBlockCipher {
        private static final Constructor aeadBadTagConstructor;
        private AEADBlockCipher cipher;

        static {
            Class clsLookup = BaseBlockCipher.lookup("javax.crypto.AEADBadTagException");
            if (clsLookup != null) {
                aeadBadTagConstructor = findExceptionConstructor(clsLookup);
            } else {
                aeadBadTagConstructor = null;
            }
        }

        private static Constructor findExceptionConstructor(Class cls) {
            try {
                return cls.getConstructor(String.class);
            } catch (Exception e) {
                return null;
            }
        }

        AEADGenericBlockCipher(AEADBlockCipher aEADBlockCipher) {
            this.cipher = aEADBlockCipher;
        }

        @Override
        public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
            this.cipher.init(z, cipherParameters);
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
        public int getOutputSize(int i) {
            return this.cipher.getOutputSize(i);
        }

        @Override
        public int getUpdateOutputSize(int i) {
            return this.cipher.getUpdateOutputSize(i);
        }

        @Override
        public void updateAAD(byte[] bArr, int i, int i2) {
            this.cipher.processAADBytes(bArr, i, i2);
        }

        @Override
        public int processByte(byte b, byte[] bArr, int i) throws DataLengthException {
            return this.cipher.processByte(b, bArr, i);
        }

        @Override
        public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
            return this.cipher.processBytes(bArr, i, i2, bArr2, i3);
        }

        @Override
        public int doFinal(byte[] bArr, int i) throws IllegalStateException, BadPaddingException {
            try {
                return this.cipher.doFinal(bArr, i);
            } catch (InvalidCipherTextException e) {
                if (aeadBadTagConstructor != null) {
                    BadPaddingException badPaddingException = null;
                    try {
                        badPaddingException = (BadPaddingException) aeadBadTagConstructor.newInstance(e.getMessage());
                    } catch (Exception e2) {
                    }
                    if (badPaddingException != null) {
                        throw badPaddingException;
                    }
                }
                throw new BadPaddingException(e.getMessage());
            }
        }
    }

    private static class InvalidKeyOrParametersException extends InvalidKeyException {
        private final Throwable cause;

        InvalidKeyOrParametersException(String str, Throwable th) {
            super(str);
            this.cause = th;
        }

        @Override
        public Throwable getCause() {
            return this.cause;
        }
    }
}
