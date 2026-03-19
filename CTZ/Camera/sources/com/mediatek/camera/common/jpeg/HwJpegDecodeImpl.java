package com.mediatek.camera.common.jpeg;

import android.graphics.SurfaceTexture;
import android.util.Log;

public class HwJpegDecodeImpl extends JpegDecoder {
    private static final String TAG = HwJpegDecodeImpl.class.getSimpleName();
    private long mNativeContext;

    private static native void nativeClassInit();

    private native void nativeDecode(byte[] bArr);

    private native void nativeRelease();

    private native void nativeSetup(int i, int i2, int i3, byte[] bArr);

    private native void nativeSetup(SurfaceTexture surfaceTexture);

    static {
        System.loadLibrary("jni_jpegdecoder");
        nativeClassInit();
    }

    HwJpegDecodeImpl(SurfaceTexture surfaceTexture) {
        nativeSetup(surfaceTexture);
    }

    HwJpegDecodeImpl(int i, int i2, int i3, byte[] bArr) {
        nativeSetup(i, i2, i3, bArr);
    }

    @Override
    public void decode(byte[] bArr) {
        Log.i(TAG, "[decode], jpegData:" + bArr);
        nativeDecode(bArr);
    }

    @Override
    public void release() {
        Log.i(TAG, "[release]");
        nativeRelease();
    }
}
