package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.VolumeShaper;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.AndroidRuntimeException;
import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class SoundPool extends PlayerBase {
    private static final boolean DEBUG;
    private static final int SAMPLE_LOADED = 1;
    private static final String TAG = "SoundPool";
    private final AudioAttributes mAttributes;
    private EventHandler mEventHandler;
    private boolean mHasAppOpsPlayAudio;
    private final Object mLock;
    private long mNativeContext;
    private OnLoadCompleteListener mOnLoadCompleteListener;

    public interface OnLoadCompleteListener {
        void onLoadComplete(SoundPool soundPool, int i, int i2);
    }

    private final native int _load(FileDescriptor fileDescriptor, long j, long j2, int i);

    private final native void _mute(boolean z);

    private final native int _play(int i, float f, float f2, int i2, int i3, float f3);

    private final native void _setVolume(int i, float f, float f2);

    private final native void native_release();

    private final native int native_setup(Object obj, int i, Object obj2);

    public final native void autoPause();

    public final native void autoResume();

    public final native void pause(int i);

    public final native void resume(int i);

    public final native void setLoop(int i, int i2);

    public final native void setPriority(int i, int i2);

    public final native void setRate(int i, float f);

    public final native void stop(int i);

    public final native boolean unload(int i);

    static {
        System.loadLibrary("soundpool");
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
    }

    public SoundPool(int i, int i2, int i3) {
        this(i, new AudioAttributes.Builder().setInternalLegacyStreamType(i2).build());
        PlayerBase.deprecateStreamTypeForPlayback(i2, TAG, "SoundPool()");
    }

    private SoundPool(int i, AudioAttributes audioAttributes) {
        super(audioAttributes, 3);
        if (native_setup(new WeakReference(this), i, audioAttributes) != 0) {
            throw new RuntimeException("Native setup failed");
        }
        this.mLock = new Object();
        this.mAttributes = audioAttributes;
        baseRegisterPlayer();
    }

    public final void release() {
        baseRelease();
        native_release();
    }

    protected void finalize() {
        release();
    }

    public int load(String str, int i) {
        int i_load;
        try {
            File file = new File(str);
            ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(file, 268435456);
            if (parcelFileDescriptorOpen == null) {
                return 0;
            }
            i_load = _load(parcelFileDescriptorOpen.getFileDescriptor(), 0L, file.length(), i);
            try {
                parcelFileDescriptorOpen.close();
                return i_load;
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            i_load = 0;
        }
        Log.e(TAG, "error loading " + str);
        return i_load;
    }

    public int load(Context context, int i, int i2) {
        AssetFileDescriptor assetFileDescriptorOpenRawResourceFd = context.getResources().openRawResourceFd(i);
        if (assetFileDescriptorOpenRawResourceFd != null) {
            int i_load = _load(assetFileDescriptorOpenRawResourceFd.getFileDescriptor(), assetFileDescriptorOpenRawResourceFd.getStartOffset(), assetFileDescriptorOpenRawResourceFd.getLength(), i2);
            try {
                assetFileDescriptorOpenRawResourceFd.close();
                return i_load;
            } catch (IOException e) {
                return i_load;
            }
        }
        return 0;
    }

    public int load(AssetFileDescriptor assetFileDescriptor, int i) {
        if (assetFileDescriptor != null) {
            long length = assetFileDescriptor.getLength();
            if (length < 0) {
                throw new AndroidRuntimeException("no length for fd");
            }
            return _load(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), length, i);
        }
        return 0;
    }

    public int load(FileDescriptor fileDescriptor, long j, long j2, int i) {
        return _load(fileDescriptor, j, j2, i);
    }

    public final int play(int i, float f, float f2, int i2, int i3, float f3) {
        baseStart();
        return _play(i, f, f2, i2, i3, f3);
    }

    public final void setVolume(int i, float f, float f2) {
        _setVolume(i, f, f2);
    }

    @Override
    int playerApplyVolumeShaper(VolumeShaper.Configuration configuration, VolumeShaper.Operation operation) {
        return -1;
    }

    @Override
    VolumeShaper.State playerGetVolumeShaperState(int i) {
        return null;
    }

    @Override
    void playerSetVolume(boolean z, float f, float f2) {
        _mute(z);
    }

    @Override
    int playerSetAuxEffectSendLevel(boolean z, float f) {
        return 0;
    }

    @Override
    void playerStart() {
    }

    @Override
    void playerPause() {
    }

    @Override
    void playerStop() {
    }

    public void setVolume(int i, float f) {
        setVolume(i, f, f);
    }

    public void setOnLoadCompleteListener(OnLoadCompleteListener onLoadCompleteListener) {
        synchronized (this.mLock) {
            try {
                if (onLoadCompleteListener != null) {
                    Looper looperMyLooper = Looper.myLooper();
                    if (looperMyLooper != null) {
                        this.mEventHandler = new EventHandler(looperMyLooper);
                    } else {
                        Looper mainLooper = Looper.getMainLooper();
                        if (mainLooper != null) {
                            this.mEventHandler = new EventHandler(mainLooper);
                        } else {
                            this.mEventHandler = null;
                        }
                    }
                } else {
                    this.mEventHandler = null;
                }
                this.mOnLoadCompleteListener = onLoadCompleteListener;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static void postEventFromNative(Object obj, int i, int i2, int i3, Object obj2) {
        SoundPool soundPool = (SoundPool) ((WeakReference) obj).get();
        if (soundPool != null && soundPool.mEventHandler != null) {
            soundPool.mEventHandler.sendMessage(soundPool.mEventHandler.obtainMessage(i, i2, i3, obj2));
        }
    }

    private final class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                if (SoundPool.DEBUG) {
                    Log.d(SoundPool.TAG, "Sample " + message.arg1 + " loaded");
                }
                synchronized (SoundPool.this.mLock) {
                    if (SoundPool.this.mOnLoadCompleteListener != null) {
                        SoundPool.this.mOnLoadCompleteListener.onLoadComplete(SoundPool.this, message.arg1, message.arg2);
                    }
                }
                return;
            }
            Log.e(SoundPool.TAG, "Unknown message type " + message.what);
        }
    }

    public static class Builder {
        private AudioAttributes mAudioAttributes;
        private int mMaxStreams = 1;

        public Builder setMaxStreams(int i) throws IllegalArgumentException {
            if (i <= 0) {
                throw new IllegalArgumentException("Strictly positive value required for the maximum number of streams");
            }
            this.mMaxStreams = i;
            return this;
        }

        public Builder setAudioAttributes(AudioAttributes audioAttributes) throws IllegalArgumentException {
            if (audioAttributes == null) {
                throw new IllegalArgumentException("Invalid null AudioAttributes");
            }
            this.mAudioAttributes = audioAttributes;
            return this;
        }

        public SoundPool build() {
            if (this.mAudioAttributes == null) {
                this.mAudioAttributes = new AudioAttributes.Builder().setUsage(1).build();
            }
            return new SoundPool(this.mMaxStreams, this.mAudioAttributes);
        }
    }
}
