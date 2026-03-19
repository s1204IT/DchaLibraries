package android.speech.tts;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.speech.tts.TextToSpeechService;
import android.util.Log;

class BlockingAudioTrack {
    private static final boolean DBG = false;
    private static final long MAX_PROGRESS_WAIT_MS = 2500;
    private static final long MAX_SLEEP_TIME_MS = 2500;
    private static final int MIN_AUDIO_BUFFER_SIZE = 8192;
    private static final long MIN_SLEEP_TIME_MS = 20;
    private static final String TAG = "TTS.BlockingAudioTrack";
    private final int mAudioFormat;
    private final TextToSpeechService.AudioOutputParams mAudioParams;
    private final int mBytesPerFrame;
    private int mBytesWritten;
    private final int mChannelCount;
    private final int mSampleRateInHz;
    private int mSessionId;
    private Object mAudioTrackLock = new Object();
    private boolean mIsShortUtterance = false;
    private int mAudioBufferSize = 0;
    private AudioTrack mAudioTrack = null;
    private volatile boolean mStopped = false;

    BlockingAudioTrack(TextToSpeechService.AudioOutputParams audioOutputParams, int i, int i2, int i3) {
        this.mBytesWritten = 0;
        this.mAudioParams = audioOutputParams;
        this.mSampleRateInHz = i;
        this.mAudioFormat = i2;
        this.mChannelCount = i3;
        this.mBytesPerFrame = AudioFormat.getBytesPerSample(this.mAudioFormat) * this.mChannelCount;
        this.mBytesWritten = 0;
    }

    public boolean init() {
        AudioTrack audioTrackCreateStreamingAudioTrack = createStreamingAudioTrack();
        synchronized (this.mAudioTrackLock) {
            this.mAudioTrack = audioTrackCreateStreamingAudioTrack;
        }
        if (audioTrackCreateStreamingAudioTrack == null) {
            return false;
        }
        return true;
    }

    public void stop() {
        synchronized (this.mAudioTrackLock) {
            if (this.mAudioTrack != null) {
                this.mAudioTrack.stop();
            }
            this.mStopped = true;
        }
    }

    public int write(byte[] bArr) {
        AudioTrack audioTrack;
        synchronized (this.mAudioTrackLock) {
            audioTrack = this.mAudioTrack;
        }
        if (audioTrack == null || this.mStopped) {
            return -1;
        }
        int iWriteToAudioTrack = writeToAudioTrack(audioTrack, bArr);
        this.mBytesWritten += iWriteToAudioTrack;
        return iWriteToAudioTrack;
    }

    public void waitAndRelease() {
        AudioTrack audioTrack;
        synchronized (this.mAudioTrackLock) {
            audioTrack = this.mAudioTrack;
        }
        if (audioTrack == null) {
            return;
        }
        if (this.mBytesWritten < this.mAudioBufferSize && !this.mStopped) {
            this.mIsShortUtterance = true;
            audioTrack.stop();
        }
        if (!this.mStopped) {
            blockUntilDone(this.mAudioTrack);
        }
        synchronized (this.mAudioTrackLock) {
            this.mAudioTrack = null;
        }
        audioTrack.release();
    }

    static int getChannelConfig(int i) {
        if (i == 1) {
            return 4;
        }
        if (i == 2) {
            return 12;
        }
        return 0;
    }

    long getAudioLengthMs(int i) {
        return ((i / this.mBytesPerFrame) * 1000) / this.mSampleRateInHz;
    }

    private static int writeToAudioTrack(AudioTrack audioTrack, byte[] bArr) {
        int iWrite;
        if (audioTrack.getPlayState() != 3) {
            audioTrack.play();
        }
        int i = 0;
        while (i < bArr.length && (iWrite = audioTrack.write(bArr, i, bArr.length)) > 0) {
            i += iWrite;
        }
        return i;
    }

    private AudioTrack createStreamingAudioTrack() {
        int channelConfig = getChannelConfig(this.mChannelCount);
        int iMax = Math.max(8192, AudioTrack.getMinBufferSize(this.mSampleRateInHz, channelConfig, this.mAudioFormat));
        AudioTrack audioTrack = new AudioTrack(this.mAudioParams.mAudioAttributes, new AudioFormat.Builder().setChannelMask(channelConfig).setEncoding(this.mAudioFormat).setSampleRate(this.mSampleRateInHz).build(), iMax, 1, this.mAudioParams.mSessionId);
        if (audioTrack.getState() != 1) {
            Log.w(TAG, "Unable to create audio track.");
            audioTrack.release();
            return null;
        }
        this.mAudioBufferSize = iMax;
        setupVolume(audioTrack, this.mAudioParams.mVolume, this.mAudioParams.mPan);
        return audioTrack;
    }

    private void blockUntilDone(AudioTrack audioTrack) {
        if (this.mBytesWritten <= 0) {
            return;
        }
        if (this.mIsShortUtterance) {
            blockUntilEstimatedCompletion();
        } else {
            blockUntilCompletion(audioTrack);
        }
    }

    private void blockUntilEstimatedCompletion() {
        try {
            Thread.sleep(((this.mBytesWritten / this.mBytesPerFrame) * 1000) / this.mSampleRateInHz);
        } catch (InterruptedException e) {
        }
    }

    private void blockUntilCompletion(AudioTrack audioTrack) {
        int i = this.mBytesWritten / this.mBytesPerFrame;
        int i2 = -1;
        long j = 0;
        while (true) {
            int playbackHeadPosition = audioTrack.getPlaybackHeadPosition();
            if (playbackHeadPosition < i && audioTrack.getPlayState() == 3 && !this.mStopped) {
                long jClip = clip(((i - playbackHeadPosition) * 1000) / audioTrack.getSampleRate(), MIN_SLEEP_TIME_MS, 2500L);
                if (playbackHeadPosition == i2) {
                    j += jClip;
                    if (j > 2500) {
                        Log.w(TAG, "Waited unsuccessfully for 2500ms for AudioTrack to make progress, Aborting");
                        return;
                    }
                } else {
                    j = 0;
                }
                try {
                    Thread.sleep(jClip);
                    i2 = playbackHeadPosition;
                } catch (InterruptedException e) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static void setupVolume(AudioTrack audioTrack, float f, float f2) {
        float f3;
        float fClip = clip(f, 0.0f, 1.0f);
        float fClip2 = clip(f2, -1.0f, 1.0f);
        if (fClip2 > 0.0f) {
            float f4 = fClip * (1.0f - fClip2);
            f3 = fClip;
            fClip = f4;
        } else if (fClip2 < 0.0f) {
            f3 = fClip * (1.0f + fClip2);
        } else {
            f3 = fClip;
        }
        if (audioTrack.setStereoVolume(fClip, f3) != 0) {
            Log.e(TAG, "Failed to set volume");
        }
    }

    private static final long clip(long j, long j2, long j3) {
        return j < j2 ? j2 : j < j3 ? j : j3;
    }

    private static final float clip(float f, float f2, float f3) {
        return f < f2 ? f2 : f < f3 ? f : f3;
    }

    public void setPlaybackPositionUpdateListener(AudioTrack.OnPlaybackPositionUpdateListener onPlaybackPositionUpdateListener) {
        synchronized (this.mAudioTrackLock) {
            if (this.mAudioTrack != null) {
                this.mAudioTrack.setPlaybackPositionUpdateListener(onPlaybackPositionUpdateListener);
            }
        }
    }

    public void setNotificationMarkerPosition(int i) {
        synchronized (this.mAudioTrackLock) {
            if (this.mAudioTrack != null) {
                this.mAudioTrack.setNotificationMarkerPosition(i);
            }
        }
    }
}
