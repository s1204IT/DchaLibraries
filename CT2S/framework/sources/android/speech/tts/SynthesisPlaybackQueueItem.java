package android.speech.tts;

import android.speech.tts.TextToSpeechService;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class SynthesisPlaybackQueueItem extends PlaybackQueueItem {
    private static final boolean DBG = false;
    private static final long MAX_UNCONSUMED_AUDIO_MS = 500;
    private static final String TAG = "TTS.SynthQueueItem";
    private final BlockingAudioTrack mAudioTrack;
    private final LinkedList<ListEntry> mDataBufferList;
    private volatile boolean mDone;
    private final Lock mListLock;
    private final AbstractEventLogger mLogger;
    private final Condition mNotFull;
    private final Condition mReadReady;
    private volatile int mStatusCode;
    private volatile boolean mStopped;
    private int mUnconsumedBytes;

    SynthesisPlaybackQueueItem(TextToSpeechService.AudioOutputParams audioParams, int sampleRate, int audioFormat, int channelCount, TextToSpeechService.UtteranceProgressDispatcher dispatcher, Object callerIdentity, AbstractEventLogger logger) {
        super(dispatcher, callerIdentity);
        this.mListLock = new ReentrantLock();
        this.mReadReady = this.mListLock.newCondition();
        this.mNotFull = this.mListLock.newCondition();
        this.mDataBufferList = new LinkedList<>();
        this.mUnconsumedBytes = 0;
        this.mStopped = false;
        this.mDone = false;
        this.mStatusCode = 0;
        this.mAudioTrack = new BlockingAudioTrack(audioParams, sampleRate, audioFormat, channelCount);
        this.mLogger = logger;
    }

    @Override
    public void run() {
        TextToSpeechService.UtteranceProgressDispatcher dispatcher = getDispatcher();
        dispatcher.dispatchOnStart();
        if (!this.mAudioTrack.init()) {
            dispatcher.dispatchOnError(-5);
            return;
        }
        while (true) {
            try {
                byte[] buffer = take();
                if (buffer == null) {
                    break;
                }
                this.mAudioTrack.write(buffer);
                this.mLogger.onAudioDataWritten();
            } catch (InterruptedException e) {
            }
        }
        this.mAudioTrack.waitAndRelease();
        if (this.mStatusCode == 0) {
            dispatcher.dispatchOnSuccess();
        } else if (this.mStatusCode == -2) {
            dispatcher.dispatchOnStop();
        } else {
            dispatcher.dispatchOnError(this.mStatusCode);
        }
        this.mLogger.onCompleted(this.mStatusCode);
    }

    @Override
    void stop(int statusCode) {
        try {
            this.mListLock.lock();
            this.mStopped = true;
            this.mStatusCode = statusCode;
            this.mReadReady.signal();
            this.mNotFull.signal();
            this.mListLock.unlock();
            this.mAudioTrack.stop();
        } catch (Throwable th) {
            this.mListLock.unlock();
            throw th;
        }
    }

    void done() {
        try {
            this.mListLock.lock();
            this.mDone = true;
            this.mReadReady.signal();
            this.mNotFull.signal();
        } finally {
            this.mListLock.unlock();
        }
    }

    void put(byte[] buffer) throws InterruptedException {
        try {
            this.mListLock.lock();
            while (unconsumedAudioMs > MAX_UNCONSUMED_AUDIO_MS && !this.mStopped) {
                this.mNotFull.await();
            }
            if (!this.mStopped) {
                this.mDataBufferList.add(new ListEntry(buffer));
                this.mUnconsumedBytes += buffer.length;
                this.mReadReady.signal();
            }
        } finally {
            this.mListLock.unlock();
        }
    }

    private byte[] take() throws InterruptedException {
        byte[] bArr = null;
        try {
            this.mListLock.lock();
            while (this.mDataBufferList.size() == 0 && !this.mStopped && !this.mDone) {
                this.mReadReady.await();
            }
            if (!this.mStopped) {
                ListEntry entry = this.mDataBufferList.poll();
                if (entry != null) {
                    this.mUnconsumedBytes -= entry.mBytes.length;
                    this.mNotFull.signal();
                    bArr = entry.mBytes;
                }
            }
            return bArr;
        } finally {
            this.mListLock.unlock();
        }
    }

    static final class ListEntry {
        final byte[] mBytes;

        ListEntry(byte[] bytes) {
            this.mBytes = bytes;
        }
    }
}
