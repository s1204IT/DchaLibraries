package android.media;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class MediaSync {
    private static final int CB_RETURN_AUDIO_BUFFER = 1;
    private static final int EVENT_CALLBACK = 1;
    private static final int EVENT_SET_CALLBACK = 2;
    public static final int MEDIASYNC_ERROR_AUDIOTRACK_FAIL = 1;
    public static final int MEDIASYNC_ERROR_SURFACE_FAIL = 2;
    private static final String TAG = "MediaSync";
    private long mNativeContext;
    private final Object mCallbackLock = new Object();
    private Handler mCallbackHandler = null;
    private Callback mCallback = null;
    private final Object mOnErrorListenerLock = new Object();
    private Handler mOnErrorListenerHandler = null;
    private OnErrorListener mOnErrorListener = null;
    private Thread mAudioThread = null;
    private Handler mAudioHandler = null;
    private Looper mAudioLooper = null;
    private final Object mAudioLock = new Object();
    private AudioTrack mAudioTrack = null;
    private List<AudioBuffer> mAudioBuffers = new LinkedList();
    private float mPlaybackRate = 0.0f;

    public static abstract class Callback {
        public abstract void onAudioBufferConsumed(MediaSync mediaSync, ByteBuffer byteBuffer, int i);
    }

    public interface OnErrorListener {
        void onError(MediaSync mediaSync, int i, int i2);
    }

    private final native void native_finalize();

    private final native void native_flush();

    private final native long native_getPlayTimeForPendingAudioFrames();

    private final native boolean native_getTimestamp(MediaTimestamp mediaTimestamp);

    private static final native void native_init();

    private final native void native_release();

    private final native void native_setAudioTrack(AudioTrack audioTrack);

    private native float native_setPlaybackParams(PlaybackParams playbackParams);

    private final native void native_setSurface(Surface surface);

    private native float native_setSyncParams(SyncParams syncParams);

    private final native void native_setup();

    private final native void native_updateQueuedAudioData(int i, long j);

    public final native Surface createInputSurface();

    public native PlaybackParams getPlaybackParams();

    public native SyncParams getSyncParams();

    private static class AudioBuffer {
        public int mBufferIndex;
        public ByteBuffer mByteBuffer;
        long mPresentationTimeUs;

        public AudioBuffer(ByteBuffer byteBuffer, int i, long j) {
            this.mByteBuffer = byteBuffer;
            this.mBufferIndex = i;
            this.mPresentationTimeUs = j;
        }
    }

    public MediaSync() {
        native_setup();
    }

    protected void finalize() {
        native_finalize();
    }

    public final void release() {
        returnAudioBuffers();
        if (this.mAudioThread != null && this.mAudioLooper != null) {
            this.mAudioLooper.quit();
        }
        setCallback(null, null);
        native_release();
    }

    public void setCallback(Callback callback, Handler handler) {
        synchronized (this.mCallbackLock) {
            try {
                if (handler != null) {
                    this.mCallbackHandler = handler;
                } else {
                    Looper looperMyLooper = Looper.myLooper();
                    if (looperMyLooper == null) {
                        looperMyLooper = Looper.getMainLooper();
                    }
                    if (looperMyLooper == null) {
                        this.mCallbackHandler = null;
                    } else {
                        this.mCallbackHandler = new Handler(looperMyLooper);
                    }
                }
                this.mCallback = callback;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void setOnErrorListener(OnErrorListener onErrorListener, Handler handler) {
        synchronized (this.mOnErrorListenerLock) {
            try {
                if (handler != null) {
                    this.mOnErrorListenerHandler = handler;
                } else {
                    Looper looperMyLooper = Looper.myLooper();
                    if (looperMyLooper == null) {
                        looperMyLooper = Looper.getMainLooper();
                    }
                    if (looperMyLooper == null) {
                        this.mOnErrorListenerHandler = null;
                    } else {
                        this.mOnErrorListenerHandler = new Handler(looperMyLooper);
                    }
                }
                this.mOnErrorListener = onErrorListener;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void setSurface(Surface surface) {
        native_setSurface(surface);
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        native_setAudioTrack(audioTrack);
        this.mAudioTrack = audioTrack;
        if (audioTrack != null && this.mAudioThread == null) {
            createAudioThread();
        }
    }

    public void setPlaybackParams(PlaybackParams playbackParams) {
        synchronized (this.mAudioLock) {
            this.mPlaybackRate = native_setPlaybackParams(playbackParams);
        }
        if (this.mPlaybackRate != 0.0d && this.mAudioThread != null) {
            postRenderAudio(0L);
        }
    }

    public void setSyncParams(SyncParams syncParams) {
        synchronized (this.mAudioLock) {
            this.mPlaybackRate = native_setSyncParams(syncParams);
        }
        if (this.mPlaybackRate != 0.0d && this.mAudioThread != null) {
            postRenderAudio(0L);
        }
    }

    public void flush() {
        synchronized (this.mAudioLock) {
            this.mAudioBuffers.clear();
            this.mCallbackHandler.removeCallbacksAndMessages(null);
        }
        if (this.mAudioTrack != null) {
            this.mAudioTrack.pause();
            this.mAudioTrack.flush();
            this.mAudioTrack.stop();
        }
        native_flush();
    }

    public MediaTimestamp getTimestamp() {
        try {
            MediaTimestamp mediaTimestamp = new MediaTimestamp();
            if (!native_getTimestamp(mediaTimestamp)) {
                return null;
            }
            return mediaTimestamp;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public void queueAudio(ByteBuffer byteBuffer, int i, long j) {
        if (this.mAudioTrack == null || this.mAudioThread == null) {
            throw new IllegalStateException("AudioTrack is NOT set or audio thread is not created");
        }
        synchronized (this.mAudioLock) {
            this.mAudioBuffers.add(new AudioBuffer(byteBuffer, i, j));
        }
        if (this.mPlaybackRate != 0.0d) {
            postRenderAudio(0L);
        }
    }

    private void postRenderAudio(long j) {
        this.mAudioHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (MediaSync.this.mAudioLock) {
                    if (MediaSync.this.mPlaybackRate == 0.0d) {
                        return;
                    }
                    if (MediaSync.this.mAudioBuffers.isEmpty()) {
                        return;
                    }
                    AudioBuffer audioBuffer = (AudioBuffer) MediaSync.this.mAudioBuffers.get(0);
                    int iRemaining = audioBuffer.mByteBuffer.remaining();
                    if (iRemaining > 0 && MediaSync.this.mAudioTrack.getPlayState() != 3) {
                        try {
                            MediaSync.this.mAudioTrack.play();
                        } catch (IllegalStateException e) {
                            Log.w(MediaSync.TAG, "could not start audio track");
                        }
                    }
                    int iWrite = MediaSync.this.mAudioTrack.write(audioBuffer.mByteBuffer, iRemaining, 1);
                    if (iWrite > 0) {
                        if (audioBuffer.mPresentationTimeUs != -1) {
                            MediaSync.this.native_updateQueuedAudioData(iRemaining, audioBuffer.mPresentationTimeUs);
                            audioBuffer.mPresentationTimeUs = -1L;
                        }
                        if (iWrite == iRemaining) {
                            MediaSync.this.postReturnByteBuffer(audioBuffer);
                            MediaSync.this.mAudioBuffers.remove(0);
                            if (!MediaSync.this.mAudioBuffers.isEmpty()) {
                                MediaSync.this.postRenderAudio(0L);
                            }
                            return;
                        }
                    }
                    MediaSync.this.postRenderAudio(TimeUnit.MICROSECONDS.toMillis(MediaSync.this.native_getPlayTimeForPendingAudioFrames()) / 2);
                }
            }
        }, j);
    }

    private final void postReturnByteBuffer(final AudioBuffer audioBuffer) {
        synchronized (this.mCallbackLock) {
            if (this.mCallbackHandler != null) {
                this.mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (MediaSync.this.mCallbackLock) {
                            Callback callback = MediaSync.this.mCallback;
                            if (MediaSync.this.mCallbackHandler != null && MediaSync.this.mCallbackHandler.getLooper().getThread() == Thread.currentThread()) {
                                if (callback != null) {
                                    callback.onAudioBufferConsumed(this, audioBuffer.mByteBuffer, audioBuffer.mBufferIndex);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private final void returnAudioBuffers() {
        synchronized (this.mAudioLock) {
            Iterator<AudioBuffer> it = this.mAudioBuffers.iterator();
            while (it.hasNext()) {
                postReturnByteBuffer(it.next());
            }
            this.mAudioBuffers.clear();
        }
    }

    private void createAudioThread() {
        this.mAudioThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                synchronized (MediaSync.this.mAudioLock) {
                    MediaSync.this.mAudioLooper = Looper.myLooper();
                    MediaSync.this.mAudioHandler = new Handler();
                    MediaSync.this.mAudioLock.notify();
                }
                Looper.loop();
            }
        };
        this.mAudioThread.start();
        synchronized (this.mAudioLock) {
            try {
                this.mAudioLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }
}
