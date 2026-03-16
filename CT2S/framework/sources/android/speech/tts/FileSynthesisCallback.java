package android.speech.tts;

import android.media.AudioFormat;
import android.speech.tts.TextToSpeechService;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

class FileSynthesisCallback extends AbstractSynthesisCallback {
    private static final boolean DBG = false;
    private static final int MAX_AUDIO_BUFFER_SIZE = 8192;
    private static final String TAG = "FileSynthesisRequest";
    private static final short WAV_FORMAT_PCM = 1;
    private static final int WAV_HEADER_LENGTH = 44;
    private int mAudioFormat;
    private final Object mCallerIdentity;
    private int mChannelCount;
    private final TextToSpeechService.UtteranceProgressDispatcher mDispatcher;
    private boolean mDone;
    private FileChannel mFileChannel;
    private int mSampleRateInHz;
    private boolean mStarted;
    private final Object mStateLock;
    protected int mStatusCode;

    FileSynthesisCallback(FileChannel fileChannel, TextToSpeechService.UtteranceProgressDispatcher dispatcher, Object callerIdentity, boolean clientIsUsingV2) {
        super(clientIsUsingV2);
        this.mStateLock = new Object();
        this.mStarted = false;
        this.mDone = false;
        this.mFileChannel = fileChannel;
        this.mDispatcher = dispatcher;
        this.mCallerIdentity = callerIdentity;
        this.mStatusCode = 0;
    }

    @Override
    void stop() {
        synchronized (this.mStateLock) {
            if (!this.mDone) {
                if (this.mStatusCode != -2) {
                    this.mStatusCode = -2;
                    cleanUp();
                    if (this.mDispatcher != null) {
                        this.mDispatcher.dispatchOnStop();
                    }
                }
            }
        }
    }

    private void cleanUp() {
        closeFile();
    }

    private void closeFile() {
        this.mFileChannel = null;
    }

    @Override
    public int getMaxBufferSize() {
        return 8192;
    }

    @Override
    public int start(int sampleRateInHz, int audioFormat, int channelCount) {
        synchronized (this.mStateLock) {
            if (this.mStatusCode == -2) {
                return errorCodeOnStop();
            }
            if (this.mStatusCode != 0) {
                return -1;
            }
            if (this.mStarted) {
                Log.e(TAG, "Start called twice");
                return -1;
            }
            this.mStarted = true;
            this.mSampleRateInHz = sampleRateInHz;
            this.mAudioFormat = audioFormat;
            this.mChannelCount = channelCount;
            if (this.mDispatcher != null) {
                this.mDispatcher.dispatchOnStart();
            }
            FileChannel fileChannel = this.mFileChannel;
            try {
                fileChannel.write(ByteBuffer.allocate(44));
                return 0;
            } catch (IOException ex) {
                Log.e(TAG, "Failed to write wav header to output file descriptor", ex);
                synchronized (this.mStateLock) {
                    cleanUp();
                    this.mStatusCode = -5;
                    return -1;
                }
            }
        }
    }

    @Override
    public int audioAvailable(byte[] buffer, int offset, int length) {
        synchronized (this.mStateLock) {
            if (this.mStatusCode == -2) {
                return errorCodeOnStop();
            }
            if (this.mStatusCode != 0) {
                return -1;
            }
            if (this.mFileChannel == null) {
                Log.e(TAG, "File not open");
                this.mStatusCode = -5;
                return -1;
            }
            if (!this.mStarted) {
                Log.e(TAG, "Start method was not called");
                return -1;
            }
            FileChannel fileChannel = this.mFileChannel;
            try {
                fileChannel.write(ByteBuffer.wrap(buffer, offset, length));
                return 0;
            } catch (IOException ex) {
                Log.e(TAG, "Failed to write to output file descriptor", ex);
                synchronized (this.mStateLock) {
                    cleanUp();
                    this.mStatusCode = -5;
                    return -1;
                }
            }
        }
    }

    @Override
    public int done() {
        synchronized (this.mStateLock) {
            if (this.mDone) {
                Log.w(TAG, "Duplicate call to done()");
                return -1;
            }
            if (this.mStatusCode == -2) {
                return errorCodeOnStop();
            }
            if (this.mDispatcher != null && this.mStatusCode != 0 && this.mStatusCode != -2) {
                this.mDispatcher.dispatchOnError(this.mStatusCode);
                return -1;
            }
            if (this.mFileChannel == null) {
                Log.e(TAG, "File not open");
                return -1;
            }
            this.mDone = true;
            FileChannel fileChannel = this.mFileChannel;
            int sampleRateInHz = this.mSampleRateInHz;
            int audioFormat = this.mAudioFormat;
            int channelCount = this.mChannelCount;
            try {
                fileChannel.position(0L);
                int dataLength = (int) (fileChannel.size() - 44);
                fileChannel.write(makeWavHeader(sampleRateInHz, audioFormat, channelCount, dataLength));
                synchronized (this.mStateLock) {
                    closeFile();
                    if (this.mDispatcher != null) {
                        this.mDispatcher.dispatchOnSuccess();
                    }
                }
                return 0;
            } catch (IOException ex) {
                Log.e(TAG, "Failed to write to output file descriptor", ex);
                synchronized (this.mStateLock) {
                    cleanUp();
                    return -1;
                }
            }
        }
    }

    @Override
    public void error() {
        error(-3);
    }

    @Override
    public void error(int errorCode) {
        synchronized (this.mStateLock) {
            if (!this.mDone) {
                cleanUp();
                this.mStatusCode = errorCode;
            }
        }
    }

    @Override
    public boolean hasStarted() {
        boolean z;
        synchronized (this.mStateLock) {
            z = this.mStarted;
        }
        return z;
    }

    @Override
    public boolean hasFinished() {
        boolean z;
        synchronized (this.mStateLock) {
            z = this.mDone;
        }
        return z;
    }

    private ByteBuffer makeWavHeader(int sampleRateInHz, int audioFormat, int channelCount, int dataLength) {
        int sampleSizeInBytes = AudioFormat.getBytesPerSample(audioFormat);
        int byteRate = sampleRateInHz * sampleSizeInBytes * channelCount;
        short blockAlign = (short) (sampleSizeInBytes * channelCount);
        short bitsPerSample = (short) (sampleSizeInBytes * 8);
        byte[] headerBuf = new byte[44];
        ByteBuffer header = ByteBuffer.wrap(headerBuf);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{82, 73, 70, 70});
        header.putInt((dataLength + 44) - 8);
        header.put(new byte[]{87, 65, 86, 69});
        header.put(new byte[]{102, 109, 116, 32});
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) channelCount);
        header.putInt(sampleRateInHz);
        header.putInt(byteRate);
        header.putShort(blockAlign);
        header.putShort(bitsPerSample);
        header.put(new byte[]{100, 97, 116, 97});
        header.putInt(dataLength);
        header.flip();
        return header;
    }
}
