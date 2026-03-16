package jp.co.omronsoft.android.emoji;

import android.graphics.Bitmap;

public final class AnimatedEmojiFactory {
    private static final boolean DEBUG_EMOJI = false;
    private static final String TAG = "AnimatedEmojiFactory";
    private String mName;
    private int mNativeAnimatedEmojiFactory;

    public static native AnimatedEmojiFactory createInstance(String str);

    private native void nativeDestructor(int i);

    private native Bitmap nativeGetBitmap(int i, int i2, int i3, float f);

    private native int nativeGetDefaultFrameNumber(int i, int i2);

    private native int[][] nativeGetFontSize(int i);

    private native int nativeGetFrameCount(int i, int i2);

    private native byte[] nativeGetImageBinary(int i, int i2, float f);

    private native int nativeGetMovieDuration(int i);

    private native int[] nativeGetPuaCode(int i);

    private native int nativeGetWidth(int i, int i2, float f);

    private native boolean nativeIsEmoji(int i, int i2);

    private AnimatedEmojiFactory(int nativeAnimatedEmojiFactory, String name) {
        this.mNativeAnimatedEmojiFactory = nativeAnimatedEmojiFactory;
        this.mName = name;
    }

    protected void finalize() throws Throwable {
        try {
            destructor();
        } finally {
            super.finalize();
        }
    }

    public String name() {
        return this.mName;
    }

    private synchronized void destructor() {
        nativeDestructor(this.mNativeAnimatedEmojiFactory);
    }

    public static AnimatedEmojiFactory createInstance() {
        return createInstance(null);
    }

    public synchronized boolean isEmoji(int code) {
        return nativeIsEmoji(this.mNativeAnimatedEmojiFactory, code);
    }

    public synchronized int getFrameCount(int code) {
        return nativeGetFrameCount(this.mNativeAnimatedEmojiFactory, code);
    }

    public synchronized int getDefaultFrameNumber(int code) {
        return nativeGetDefaultFrameNumber(this.mNativeAnimatedEmojiFactory, code);
    }

    public synchronized int getWidth(int code, float size) {
        return nativeGetWidth(this.mNativeAnimatedEmojiFactory, code, size);
    }

    public synchronized Bitmap getBitmap(int code, int frameNo, float size) {
        Bitmap ret;
        ret = nativeGetBitmap(this.mNativeAnimatedEmojiFactory, code, frameNo, size);
        return ret;
    }

    public synchronized int[] getPuaCode() {
        return nativeGetPuaCode(this.mNativeAnimatedEmojiFactory);
    }

    public synchronized int[][] getFontSize() {
        return nativeGetFontSize(this.mNativeAnimatedEmojiFactory);
    }

    public synchronized int getMovieDuration() {
        return nativeGetMovieDuration(this.mNativeAnimatedEmojiFactory);
    }

    public synchronized byte[] getImageBinary(int code, float size) {
        return nativeGetImageBinary(this.mNativeAnimatedEmojiFactory, code, size);
    }

    public static boolean isType2() {
        return true;
    }
}
