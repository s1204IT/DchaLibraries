package com.android.camera.util;

import android.media.Image;
import java.nio.ByteBuffer;

public class JpegUtilNative {
    public static final int ERROR_OUT_BUF_TOO_SMALL = -1;

    private static native int compressJpegFromYUV420pNative(int i, int i2, Object obj, int i3, int i4, Object obj2, int i5, int i6, Object obj3, int i7, int i8, Object obj4, int i9, int i10);

    static {
        System.loadLibrary("jni_jpegutil");
    }

    public static int compressJpegFromYUV420p(int width, int height, ByteBuffer yBuf, int yPStride, int yRStride, ByteBuffer cbBuf, int cbPStride, int cbRStride, ByteBuffer crBuf, int crPStride, int crRStride, ByteBuffer outBuf, int quality) {
        return compressJpegFromYUV420pNative(width, height, yBuf, yPStride, yRStride, cbBuf, cbPStride, cbRStride, crBuf, crPStride, crRStride, outBuf, outBuf.capacity(), quality);
    }

    public static int compressJpegFromYUV420Image(Image img, ByteBuffer outBuf, int quality) {
        if (img.getFormat() != 35) {
            throw new RuntimeException("Unsupported Image Format.");
        }
        if (img.getPlanes().length != 3) {
            throw new RuntimeException("Output buffer must be direct.");
        }
        if (!outBuf.isDirect()) {
            throw new RuntimeException("Output buffer must be direct.");
        }
        ByteBuffer[] planeBuf = new ByteBuffer[3];
        int[] pixelStride = new int[3];
        int[] rowStride = new int[3];
        for (int i = 0; i < 3; i++) {
            Image.Plane plane = img.getPlanes()[i];
            if (!plane.getBuffer().isDirect()) {
                return -1;
            }
            planeBuf[i] = plane.getBuffer();
            pixelStride[i] = plane.getPixelStride();
            rowStride[i] = plane.getRowStride();
        }
        outBuf.clear();
        int numBytesWritten = compressJpegFromYUV420p(img.getWidth(), img.getHeight(), planeBuf[0], pixelStride[0], rowStride[0], planeBuf[1], pixelStride[1], rowStride[1], planeBuf[2], pixelStride[2], rowStride[2], outBuf, quality);
        outBuf.limit(numBytesWritten);
        return numBytesWritten;
    }
}
