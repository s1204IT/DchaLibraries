package android.security.keystore;

import android.os.IBinder;
import android.security.KeyStore;
import android.security.KeyStoreException;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import libcore.util.EmptyArray;

abstract class AndroidKeyStoreECDSASignatureSpi extends AndroidKeyStoreSignatureSpiBase {
    private int mGroupSizeBits = -1;
    private final int mKeymasterDigest;

    public static final class NONE extends AndroidKeyStoreECDSASignatureSpi {
        public NONE() {
            super(0);
        }

        @Override
        protected KeyStoreCryptoOperationStreamer createMainDataStreamer(KeyStore keyStore, IBinder iBinder) {
            return new TruncateToFieldSizeMessageStreamer(super.createMainDataStreamer(keyStore, iBinder), getGroupSizeBits());
        }

        private static class TruncateToFieldSizeMessageStreamer implements KeyStoreCryptoOperationStreamer {
            private long mConsumedInputSizeBytes;
            private final KeyStoreCryptoOperationStreamer mDelegate;
            private final int mGroupSizeBits;
            private final ByteArrayOutputStream mInputBuffer;

            private TruncateToFieldSizeMessageStreamer(KeyStoreCryptoOperationStreamer keyStoreCryptoOperationStreamer, int i) {
                this.mInputBuffer = new ByteArrayOutputStream();
                this.mDelegate = keyStoreCryptoOperationStreamer;
                this.mGroupSizeBits = i;
            }

            @Override
            public byte[] update(byte[] bArr, int i, int i2) throws KeyStoreException {
                if (i2 > 0) {
                    this.mInputBuffer.write(bArr, i, i2);
                    this.mConsumedInputSizeBytes += (long) i2;
                }
                return EmptyArray.BYTE;
            }

            @Override
            public byte[] doFinal(byte[] bArr, int i, int i2, byte[] bArr2, byte[] bArr3) throws KeyStoreException {
                if (i2 > 0) {
                    this.mConsumedInputSizeBytes += (long) i2;
                    this.mInputBuffer.write(bArr, i, i2);
                }
                byte[] byteArray = this.mInputBuffer.toByteArray();
                this.mInputBuffer.reset();
                return this.mDelegate.doFinal(byteArray, 0, Math.min(byteArray.length, (this.mGroupSizeBits + 7) / 8), bArr2, bArr3);
            }

            @Override
            public long getConsumedInputSizeBytes() {
                return this.mConsumedInputSizeBytes;
            }

            @Override
            public long getProducedOutputSizeBytes() {
                return this.mDelegate.getProducedOutputSizeBytes();
            }
        }
    }

    public static final class SHA1 extends AndroidKeyStoreECDSASignatureSpi {
        public SHA1() {
            super(2);
        }
    }

    public static final class SHA224 extends AndroidKeyStoreECDSASignatureSpi {
        public SHA224() {
            super(3);
        }
    }

    public static final class SHA256 extends AndroidKeyStoreECDSASignatureSpi {
        public SHA256() {
            super(4);
        }
    }

    public static final class SHA384 extends AndroidKeyStoreECDSASignatureSpi {
        public SHA384() {
            super(5);
        }
    }

    public static final class SHA512 extends AndroidKeyStoreECDSASignatureSpi {
        public SHA512() {
            super(6);
        }
    }

    AndroidKeyStoreECDSASignatureSpi(int i) {
        this.mKeymasterDigest = i;
    }

    @Override
    protected final void initKey(AndroidKeyStoreKey androidKeyStoreKey) throws InvalidKeyException {
        if (!KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(androidKeyStoreKey.getAlgorithm())) {
            throw new InvalidKeyException("Unsupported key algorithm: " + androidKeyStoreKey.getAlgorithm() + ". Only" + KeyProperties.KEY_ALGORITHM_EC + " supported");
        }
        KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
        int keyCharacteristics2 = getKeyStore().getKeyCharacteristics(androidKeyStoreKey.getAlias(), null, null, androidKeyStoreKey.getUid(), keyCharacteristics);
        if (keyCharacteristics2 != 1) {
            throw getKeyStore().getInvalidKeyException(androidKeyStoreKey.getAlias(), androidKeyStoreKey.getUid(), keyCharacteristics2);
        }
        long unsignedInt = keyCharacteristics.getUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, -1L);
        if (unsignedInt == -1) {
            throw new InvalidKeyException("Size of key not known");
        }
        if (unsignedInt > 2147483647L) {
            throw new InvalidKeyException("Key too large: " + unsignedInt + " bits");
        }
        this.mGroupSizeBits = (int) unsignedInt;
        super.initKey(androidKeyStoreKey);
    }

    @Override
    protected final void resetAll() {
        this.mGroupSizeBits = -1;
        super.resetAll();
    }

    @Override
    protected final void resetWhilePreservingInitState() {
        super.resetWhilePreservingInitState();
    }

    @Override
    protected final void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArguments) {
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 3);
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigest);
    }

    @Override
    protected final int getAdditionalEntropyAmountForSign() {
        return (this.mGroupSizeBits + 7) / 8;
    }

    protected final int getGroupSizeBits() {
        if (this.mGroupSizeBits == -1) {
            throw new IllegalStateException("Not initialized");
        }
        return this.mGroupSizeBits;
    }
}
