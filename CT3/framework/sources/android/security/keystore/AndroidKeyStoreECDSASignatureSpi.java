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
        protected KeyStoreCryptoOperationStreamer createMainDataStreamer(KeyStore keyStore, IBinder operationToken) {
            return new TruncateToFieldSizeMessageStreamer(super.createMainDataStreamer(keyStore, operationToken), getGroupSizeBits(), null);
        }

        private static class TruncateToFieldSizeMessageStreamer implements KeyStoreCryptoOperationStreamer {
            private long mConsumedInputSizeBytes;
            private final KeyStoreCryptoOperationStreamer mDelegate;
            private final int mGroupSizeBits;
            private final ByteArrayOutputStream mInputBuffer;

            TruncateToFieldSizeMessageStreamer(KeyStoreCryptoOperationStreamer delegate, int groupSizeBits, TruncateToFieldSizeMessageStreamer truncateToFieldSizeMessageStreamer) {
                this(delegate, groupSizeBits);
            }

            private TruncateToFieldSizeMessageStreamer(KeyStoreCryptoOperationStreamer delegate, int groupSizeBits) {
                this.mInputBuffer = new ByteArrayOutputStream();
                this.mDelegate = delegate;
                this.mGroupSizeBits = groupSizeBits;
            }

            @Override
            public byte[] update(byte[] input, int inputOffset, int inputLength) throws KeyStoreException {
                if (inputLength > 0) {
                    this.mInputBuffer.write(input, inputOffset, inputLength);
                    this.mConsumedInputSizeBytes += (long) inputLength;
                }
                return EmptyArray.BYTE;
            }

            @Override
            public byte[] doFinal(byte[] input, int inputOffset, int inputLength, byte[] signature, byte[] additionalEntropy) throws KeyStoreException {
                if (inputLength > 0) {
                    this.mConsumedInputSizeBytes += (long) inputLength;
                    this.mInputBuffer.write(input, inputOffset, inputLength);
                }
                byte[] bufferedInput = this.mInputBuffer.toByteArray();
                this.mInputBuffer.reset();
                return this.mDelegate.doFinal(bufferedInput, 0, Math.min(bufferedInput.length, (this.mGroupSizeBits + 7) / 8), signature, additionalEntropy);
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

    AndroidKeyStoreECDSASignatureSpi(int keymasterDigest) {
        this.mKeymasterDigest = keymasterDigest;
    }

    @Override
    protected final void initKey(AndroidKeyStoreKey key) throws InvalidKeyException {
        if (!KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(key.getAlgorithm())) {
            throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm() + ". Only" + KeyProperties.KEY_ALGORITHM_EC + " supported");
        }
        KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
        int errorCode = getKeyStore().getKeyCharacteristics(key.getAlias(), null, null, key.getUid(), keyCharacteristics);
        if (errorCode != 1) {
            throw getKeyStore().getInvalidKeyException(key.getAlias(), key.getUid(), errorCode);
        }
        long keySizeBits = keyCharacteristics.getUnsignedInt(KeymasterDefs.KM_TAG_KEY_SIZE, -1L);
        if (keySizeBits == -1) {
            throw new InvalidKeyException("Size of key not known");
        }
        if (keySizeBits > 2147483647L) {
            throw new InvalidKeyException("Key too large: " + keySizeBits + " bits");
        }
        this.mGroupSizeBits = (int) keySizeBits;
        super.initKey(key);
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
    protected final void addAlgorithmSpecificParametersToBegin(KeymasterArguments keymasterArgs) {
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 3);
        keymasterArgs.addEnum(KeymasterDefs.KM_TAG_DIGEST, this.mKeymasterDigest);
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
