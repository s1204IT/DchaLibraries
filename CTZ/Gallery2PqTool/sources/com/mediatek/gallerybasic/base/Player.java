package com.mediatek.gallerybasic.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MTexture;
import com.mediatek.gallerybasic.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class Player {
    public static final int MSG_FRAME_AVAILABLE = 1;
    public static final int MSG_NOTIFY = 0;
    public static final int MSG_PLAY_FRAME = 2;
    private static final String TAG = "MtkGallery2/Player";
    private static final LooperThread sLooperThread = new LooperThread("Player-LooperThread");
    protected Context mContext;
    protected OnFrameAvailableListener mFrameAvailableListener;
    protected MediaData mMediaData;
    protected OutputType mOutputType;
    protected ArrayList<PlayListener> mPlayListeners;
    protected TaskCanceller mTaskCanceller;
    protected EventHandler mThreadHandler;
    private volatile State mState = State.RELEASED;
    protected EventHandler mMainThreadHandler = new EventHandler(this, Looper.getMainLooper());

    public interface OnFrameAvailableListener {
        void onFrameAvailable(Player player);
    }

    public enum OutputType {
        TEXTURE,
        BITMAP
    }

    public interface PlayListener {
        void onChange(Player player, int i, int i2, Object obj);
    }

    public enum State {
        PREPARED,
        PLAYING,
        RELEASED
    }

    public interface TaskCanceller {
        boolean isCancelled();
    }

    protected abstract boolean onPause();

    protected abstract boolean onPrepare();

    protected abstract void onRelease();

    protected abstract boolean onStart();

    protected abstract boolean onStop();

    static {
        sLooperThread.start();
    }

    static class LooperThread extends Thread {
        private Looper mLooper;

        public LooperThread(String str) {
            super(str);
        }

        @Override
        public void run() {
            Log.d(Player.TAG, "<LooperThread.run>");
            Looper.prepare();
            this.mLooper = Looper.myLooper();
            Looper.loop();
        }

        public Looper getLooper() {
            return this.mLooper;
        }
    }

    public Player(Context context, MediaData mediaData, OutputType outputType) {
        this.mOutputType = OutputType.TEXTURE;
        this.mContext = context;
        Looper looper = sLooperThread.getLooper();
        while (looper == null) {
            Log.d(TAG, "<Player> looper is null, wait 5 ms");
            try {
                Thread.sleep(5L);
            } catch (InterruptedException e) {
                Log.d(TAG, "<Player> Thread.sleep InterruptedException");
            }
            looper = sLooperThread.getLooper();
        }
        this.mThreadHandler = new EventHandler(this, looper);
        this.mMediaData = mediaData;
        this.mPlayListeners = new ArrayList<>();
        this.mOutputType = outputType;
    }

    public Context getContext() {
        return this.mContext;
    }

    public State getState() {
        return this.mState;
    }

    public MTexture getTexture(MGLCanvas mGLCanvas) {
        return null;
    }

    public Bitmap getBitmap() {
        return null;
    }

    public void registerPlayListener(PlayListener playListener) {
        this.mPlayListeners.add(playListener);
    }

    public void unRegisterPlayListener(PlayListener playListener) {
        this.mPlayListeners.remove(playListener);
    }

    public void clearAllPlayListener() {
        this.mPlayListeners.clear();
    }

    public OutputType getOutputType() {
        return this.mOutputType;
    }

    public int getOutputWidth() {
        return 0;
    }

    public int getOutputHeight() {
        return 0;
    }

    public boolean isSkipAnimationWhenUpdateSize() {
        return false;
    }

    protected void sendNotify(int i, int i2, Object obj) {
        Message message = new Message();
        message.what = 0;
        message.arg1 = i;
        message.arg2 = i2;
        message.obj = obj;
        this.mMainThreadHandler.sendMessage(message);
    }

    protected void sendNotify(int i) {
        sendNotify(i, 0, null);
    }

    protected void sendFrameAvailable() {
        if (this.mFrameAvailableListener == null) {
            return;
        }
        this.mMainThreadHandler.removeMessages(1);
        this.mMainThreadHandler.sendEmptyMessage(1);
    }

    protected void sendPlayFrameDelayed(int i) {
        if (i == 0) {
            this.mThreadHandler.sendEmptyMessage(2);
        } else {
            this.mThreadHandler.sendEmptyMessageDelayed(2, i);
        }
    }

    protected void removeAllMessages() {
        this.mMainThreadHandler.removeMessages(2);
        this.mMainThreadHandler.removeMessages(1);
        this.mMainThreadHandler.removeMessages(0);
        this.mThreadHandler.removeMessages(2);
        this.mThreadHandler.removeMessages(1);
        this.mThreadHandler.removeMessages(0);
    }

    protected void onPlayFrame() {
    }

    protected class EventHandler extends Handler {
        private Player mPlayer;

        public EventHandler(Player player, Looper looper) {
            super(looper);
            this.mPlayer = player;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    Iterator<PlayListener> it = Player.this.mPlayListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onChange(this.mPlayer, message.arg1, message.arg2, message.obj);
                    }
                    return;
                case 1:
                    if (Player.this.mFrameAvailableListener != null) {
                        Player.this.mFrameAvailableListener.onFrameAvailable(Player.this);
                        return;
                    }
                    return;
                case 2:
                    Player.this.onPlayFrame();
                    return;
                default:
                    throw new IllegalArgumentException("Invalid message.what = " + message.what);
            }
        }
    }

    public void setOnFrameAvailableListener(OnFrameAvailableListener onFrameAvailableListener) {
        this.mFrameAvailableListener = onFrameAvailableListener;
    }

    public boolean prepare() {
        boolean zOnPrepare = onPrepare();
        if (zOnPrepare) {
            this.mState = State.PREPARED;
        }
        return zOnPrepare;
    }

    public boolean start() {
        boolean zOnStart = onStart();
        if (zOnStart) {
            this.mState = State.PLAYING;
        }
        return zOnStart;
    }

    public boolean pause() {
        return onPause();
    }

    public boolean stop() {
        boolean zOnStop = onStop();
        if (zOnStop) {
            this.mState = State.PREPARED;
        }
        return zOnStop;
    }

    public void release() {
        onRelease();
        this.mState = State.RELEASED;
    }

    public void setBuffer(byte[] bArr) {
    }

    public void setTaskCanceller(TaskCanceller taskCanceller) {
        this.mTaskCanceller = taskCanceller;
    }

    public void onCancel() {
    }
}
