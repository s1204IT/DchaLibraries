package com.mediatek.camera.common.jpeg;

import android.graphics.SurfaceTexture;

public abstract class JpegDecoder {
    public abstract void decode(byte[] bArr);

    public abstract void release();

    public static JpegDecoder newInstance(SurfaceTexture surfaceTexture) {
        return new HwJpegDecodeImpl(surfaceTexture);
    }
}
