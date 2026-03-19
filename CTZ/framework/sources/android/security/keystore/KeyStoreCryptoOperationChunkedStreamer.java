package android.security.keystore;

import android.os.IBinder;
import android.security.KeyStore;
import android.security.KeyStoreException;
import android.security.keymaster.OperationResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.ProviderException;
import libcore.util.EmptyArray;

class KeyStoreCryptoOperationChunkedStreamer implements KeyStoreCryptoOperationStreamer {
    private static final int DEFAULT_MAX_CHUNK_SIZE = 65536;
    private byte[] mBuffered;
    private int mBufferedLength;
    private int mBufferedOffset;
    private long mConsumedInputSizeBytes;
    private final Stream mKeyStoreStream;
    private final int mMaxChunkSize;
    private long mProducedOutputSizeBytes;

    interface Stream {
        OperationResult finish(byte[] bArr, byte[] bArr2);

        OperationResult update(byte[] bArr);
    }

    public KeyStoreCryptoOperationChunkedStreamer(Stream stream) {
        this(stream, 65536);
    }

    public KeyStoreCryptoOperationChunkedStreamer(Stream stream, int i) {
        this.mBuffered = EmptyArray.BYTE;
        this.mKeyStoreStream = stream;
        this.mMaxChunkSize = i;
    }

    @Override
    public byte[] update(byte[] bArr, int i, int i2) throws KeyStoreException {
        byte[] byteArray;
        byte[] bArrConcat;
        int length;
        byte[] byteArray2;
        if (i2 == 0) {
            return EmptyArray.BYTE;
        }
        ByteArrayOutputStream byteArrayOutputStream = null;
        while (i2 > 0) {
            if (this.mBufferedLength + i2 > this.mMaxChunkSize) {
                int i3 = this.mMaxChunkSize - this.mBufferedLength;
                bArrConcat = ArrayUtils.concat(this.mBuffered, this.mBufferedOffset, this.mBufferedLength, bArr, i, i3);
                length = i3;
            } else if (this.mBufferedLength == 0 && i == 0 && i2 == bArr.length) {
                length = bArr.length;
                bArrConcat = bArr;
            } else {
                bArrConcat = ArrayUtils.concat(this.mBuffered, this.mBufferedOffset, this.mBufferedLength, bArr, i, i2);
                length = i2;
            }
            i += length;
            i2 -= length;
            this.mConsumedInputSizeBytes += (long) length;
            OperationResult operationResultUpdate = this.mKeyStoreStream.update(bArrConcat);
            if (operationResultUpdate == null) {
                throw new KeyStoreConnectException();
            }
            if (operationResultUpdate.resultCode != 1) {
                throw KeyStore.getKeyStoreException(operationResultUpdate.resultCode);
            }
            if (operationResultUpdate.inputConsumed == bArrConcat.length) {
                this.mBuffered = EmptyArray.BYTE;
                this.mBufferedOffset = 0;
                this.mBufferedLength = 0;
            } else if (operationResultUpdate.inputConsumed <= 0) {
                if (i2 > 0) {
                    throw new KeyStoreException(-1000, "Keystore consumed nothing from max-sized chunk: " + bArrConcat.length + " bytes");
                }
                this.mBuffered = bArrConcat;
                this.mBufferedOffset = 0;
                this.mBufferedLength = bArrConcat.length;
            } else if (operationResultUpdate.inputConsumed < bArrConcat.length) {
                this.mBuffered = bArrConcat;
                this.mBufferedOffset = operationResultUpdate.inputConsumed;
                this.mBufferedLength = bArrConcat.length - operationResultUpdate.inputConsumed;
            } else {
                throw new KeyStoreException(-1000, "Keystore consumed more input than provided. Provided: " + bArrConcat.length + ", consumed: " + operationResultUpdate.inputConsumed);
            }
            if (operationResultUpdate.output != null && operationResultUpdate.output.length > 0) {
                if (i2 > 0) {
                    if (byteArrayOutputStream == null) {
                        byteArrayOutputStream = new ByteArrayOutputStream();
                        try {
                            byteArrayOutputStream.write(operationResultUpdate.output);
                        } catch (IOException e) {
                            throw new ProviderException("Failed to buffer output", e);
                        }
                    } else {
                        continue;
                    }
                } else {
                    if (byteArrayOutputStream == null) {
                        byteArray2 = operationResultUpdate.output;
                    } else {
                        try {
                            byteArrayOutputStream.write(operationResultUpdate.output);
                            byteArray2 = byteArrayOutputStream.toByteArray();
                        } catch (IOException e2) {
                            throw new ProviderException("Failed to buffer output", e2);
                        }
                    }
                    this.mProducedOutputSizeBytes += (long) byteArray2.length;
                    return byteArray2;
                }
            }
        }
        if (byteArrayOutputStream == null) {
            byteArray = EmptyArray.BYTE;
        } else {
            byteArray = byteArrayOutputStream.toByteArray();
        }
        this.mProducedOutputSizeBytes += (long) byteArray.length;
        return byteArray;
    }

    @Override
    public byte[] doFinal(byte[] bArr, int i, int i2, byte[] bArr2, byte[] bArr3) throws KeyStoreException {
        if (i2 == 0) {
            bArr = EmptyArray.BYTE;
            i = 0;
        }
        byte[] bArrConcat = ArrayUtils.concat(update(bArr, i, i2), flush());
        OperationResult operationResultFinish = this.mKeyStoreStream.finish(bArr2, bArr3);
        if (operationResultFinish == null) {
            throw new KeyStoreConnectException();
        }
        if (operationResultFinish.resultCode != 1) {
            throw KeyStore.getKeyStoreException(operationResultFinish.resultCode);
        }
        this.mProducedOutputSizeBytes += (long) operationResultFinish.output.length;
        return ArrayUtils.concat(bArrConcat, operationResultFinish.output);
    }

    public byte[] flush() throws KeyStoreException {
        String str;
        if (this.mBufferedLength <= 0) {
            return EmptyArray.BYTE;
        }
        ByteArrayOutputStream byteArrayOutputStream = null;
        while (this.mBufferedLength > 0) {
            byte[] bArrSubarray = ArrayUtils.subarray(this.mBuffered, this.mBufferedOffset, this.mBufferedLength);
            OperationResult operationResultUpdate = this.mKeyStoreStream.update(bArrSubarray);
            if (operationResultUpdate == null) {
                throw new KeyStoreConnectException();
            }
            if (operationResultUpdate.resultCode != 1) {
                throw KeyStore.getKeyStoreException(operationResultUpdate.resultCode);
            }
            if (operationResultUpdate.inputConsumed <= 0) {
                break;
            }
            if (operationResultUpdate.inputConsumed >= bArrSubarray.length) {
                this.mBuffered = EmptyArray.BYTE;
                this.mBufferedOffset = 0;
                this.mBufferedLength = 0;
            } else {
                this.mBuffered = bArrSubarray;
                this.mBufferedOffset = operationResultUpdate.inputConsumed;
                this.mBufferedLength = bArrSubarray.length - operationResultUpdate.inputConsumed;
            }
            if (operationResultUpdate.inputConsumed > bArrSubarray.length) {
                throw new KeyStoreException(-1000, "Keystore consumed more input than provided. Provided: " + bArrSubarray.length + ", consumed: " + operationResultUpdate.inputConsumed);
            }
            if (operationResultUpdate.output != null && operationResultUpdate.output.length > 0) {
                if (byteArrayOutputStream == null) {
                    if (this.mBufferedLength == 0) {
                        this.mProducedOutputSizeBytes += (long) operationResultUpdate.output.length;
                        return operationResultUpdate.output;
                    }
                    byteArrayOutputStream = new ByteArrayOutputStream();
                }
                try {
                    byteArrayOutputStream.write(operationResultUpdate.output);
                } catch (IOException e) {
                    throw new ProviderException("Failed to buffer output", e);
                }
            }
        }
        if (this.mBufferedLength > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Keystore failed to consume last ");
            if (this.mBufferedLength != 1) {
                str = this.mBufferedLength + " bytes";
            } else {
                str = "byte";
            }
            sb.append(str);
            sb.append(" of input");
            throw new KeyStoreException(-21, sb.toString());
        }
        byte[] byteArray = byteArrayOutputStream != null ? byteArrayOutputStream.toByteArray() : EmptyArray.BYTE;
        this.mProducedOutputSizeBytes += (long) byteArray.length;
        return byteArray;
    }

    @Override
    public long getConsumedInputSizeBytes() {
        return this.mConsumedInputSizeBytes;
    }

    @Override
    public long getProducedOutputSizeBytes() {
        return this.mProducedOutputSizeBytes;
    }

    public static class MainDataStream implements Stream {
        private final KeyStore mKeyStore;
        private final IBinder mOperationToken;

        public MainDataStream(KeyStore keyStore, IBinder iBinder) {
            this.mKeyStore = keyStore;
            this.mOperationToken = iBinder;
        }

        @Override
        public OperationResult update(byte[] bArr) {
            return this.mKeyStore.update(this.mOperationToken, null, bArr);
        }

        @Override
        public OperationResult finish(byte[] bArr, byte[] bArr2) {
            return this.mKeyStore.finish(this.mOperationToken, null, bArr, bArr2);
        }
    }
}
