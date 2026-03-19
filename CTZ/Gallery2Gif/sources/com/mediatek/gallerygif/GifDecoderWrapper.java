package com.mediatek.gallerygif;

import android.graphics.Bitmap;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.mediatek.gallerybasic.util.DebugUtils;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.gallerybasic.util.Utils;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;

public class GifDecoderWrapper {
    private static final int BUFFER_SIZE = 16384;
    private static final boolean ENABLE_DEBUG = SystemPropertyUtils.get("gifdecoder.debug").equals("1");
    public static final int INVALID_VALUE = -1;
    public static final int MINIMUM_DURATION = 10;
    private static final String TAG = "MtkGallery2/GifDecoderWrapper";
    private GifDecoder mGifDecoder;

    private GifDecoderWrapper(GifDecoder gifDecoder) {
        this.mGifDecoder = gifDecoder;
    }

    public static GifDecoderWrapper createGifDecoderWrapper(String str) throws Throwable {
        FileInputStream fileInputStream;
        try {
            if (str == null) {
                return null;
            }
            try {
                Log.d(TAG, "<createGifDecoderWrapper> filePath: " + str);
                fileInputStream = new FileInputStream(str);
                try {
                    byte[] bArrInputStreamToBytes = inputStreamToBytes(fileInputStream);
                    if (bArrInputStreamToBytes != null && bArrInputStreamToBytes.length > 0) {
                        GifDecoder gifDecoder = new GifDecoder(new GalleryBitmapProvider());
                        gifDecoder.setData(new GifHeaderParser().setData(bArrInputStreamToBytes).parseHeader(), bArrInputStreamToBytes);
                        GifDecoderWrapper gifDecoderWrapper = new GifDecoderWrapper(gifDecoder);
                        Utils.closeSilently(fileInputStream);
                        return gifDecoderWrapper;
                    }
                    Utils.closeSilently(fileInputStream);
                    return null;
                } catch (FileNotFoundException e) {
                    e = e;
                    Log.e(TAG, "<createGifDecoderWrapper> FileNotFoundException", e);
                    Utils.closeSilently(fileInputStream);
                    return null;
                } catch (IllegalArgumentException e2) {
                    e = e2;
                    Log.e(TAG, "<createGifDecoderWrapper> IllegalArgumentException", e);
                    Utils.closeSilently(fileInputStream);
                    return null;
                } catch (BufferUnderflowException e3) {
                    e = e3;
                    Log.e(TAG, "<createGifDecoderWrapper> BufferUnderflowException", e);
                    Utils.closeSilently(fileInputStream);
                    return null;
                }
            } catch (FileNotFoundException e4) {
                e = e4;
                fileInputStream = null;
            } catch (IllegalArgumentException e5) {
                e = e5;
                fileInputStream = null;
            } catch (BufferUnderflowException e6) {
                e = e6;
                fileInputStream = null;
            } catch (Throwable th) {
                th = th;
                Utils.closeSilently((Closeable) null);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public static GifDecoderWrapper createGifDecoderWrapper(byte[] bArr, int i, int i2) {
        if (bArr == null || bArr.length <= 0) {
            return null;
        }
        try {
            Log.d(TAG, "<createGifDecoderWrapper> buffer: " + bArr);
            GifDecoder gifDecoder = new GifDecoder(new GalleryBitmapProvider());
            gifDecoder.setData(new GifHeaderParser().setData(bArr).parseHeader(), bArr);
            return new GifDecoderWrapper(gifDecoder);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "<createGifDecoderWrapper> IllegalArgumentException", e);
            return null;
        } catch (BufferUnderflowException e2) {
            Log.e(TAG, "<createGifDecoderWrapper> BufferUnderflowException", e2);
            return null;
        }
    }

    public static GifDecoderWrapper createGifDecoderWrapper(InputStream inputStream) {
        byte[] bArrInputStreamToBytes;
        if (inputStream == null || (bArrInputStreamToBytes = inputStreamToBytes(inputStream)) == null || bArrInputStreamToBytes.length <= 0) {
            return null;
        }
        try {
            Log.d(TAG, "<createGifDecoderWrapper> is: " + inputStream);
            GifDecoder gifDecoder = new GifDecoder(new GalleryBitmapProvider());
            gifDecoder.setData(new GifHeaderParser().setData(bArrInputStreamToBytes).parseHeader(), bArrInputStreamToBytes);
            return new GifDecoderWrapper(gifDecoder);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "<createGifDecoderWrapper> IllegalArgumentException", e);
            return null;
        } catch (BufferUnderflowException e2) {
            Log.e(TAG, "<createGifDecoderWrapper> BufferUnderflowException", e2);
            return null;
        }
    }

    public static GifDecoderWrapper createGifDecoderWrapper(FileDescriptor fileDescriptor) throws Throwable {
        FileInputStream fileInputStream;
        try {
            if (fileDescriptor == null) {
                return null;
            }
            try {
                fileInputStream = new FileInputStream(fileDescriptor);
                try {
                    byte[] bArrInputStreamToBytes = inputStreamToBytes(fileInputStream);
                    if (bArrInputStreamToBytes != null && bArrInputStreamToBytes.length > 0) {
                        Log.d(TAG, "<createGifDecoderWrapper> fd: " + fileDescriptor);
                        GifDecoder gifDecoder = new GifDecoder(new GalleryBitmapProvider());
                        gifDecoder.setData(new GifHeaderParser().setData(bArrInputStreamToBytes).parseHeader(), bArrInputStreamToBytes);
                        GifDecoderWrapper gifDecoderWrapper = new GifDecoderWrapper(gifDecoder);
                        Utils.closeSilently(fileInputStream);
                        return gifDecoderWrapper;
                    }
                    Utils.closeSilently(fileInputStream);
                    return null;
                } catch (IllegalArgumentException e) {
                    e = e;
                    Log.e(TAG, "<createGifDecoderWrapper> IllegalArgumentException", e);
                    Utils.closeSilently(fileInputStream);
                    return null;
                } catch (BufferUnderflowException e2) {
                    e = e2;
                    Log.e(TAG, "<createGifDecoderWrapper> BufferUnderflowException", e);
                    Utils.closeSilently(fileInputStream);
                    return null;
                }
            } catch (IllegalArgumentException e3) {
                e = e3;
                fileInputStream = null;
            } catch (BufferUnderflowException e4) {
                e = e4;
                fileInputStream = null;
            } catch (Throwable th) {
                th = th;
                Utils.closeSilently((Closeable) null);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public void close() {
        if (this.mGifDecoder == null) {
            return;
        }
        Log.d(TAG, "<close>");
        this.mGifDecoder.clear();
        this.mGifDecoder = null;
    }

    public int getWidth() {
        if (this.mGifDecoder == null) {
            return -1;
        }
        return this.mGifDecoder.getWidth();
    }

    public int getHeight() {
        if (this.mGifDecoder == null) {
            return -1;
        }
        return this.mGifDecoder.getHeight();
    }

    public int getTotalFrameCount() {
        if (this.mGifDecoder == null) {
            return -1;
        }
        return this.mGifDecoder.getFrameCount();
    }

    public int getFrameDuration(int i) {
        if (this.mGifDecoder == null) {
            return -1;
        }
        return Math.max(this.mGifDecoder.getDelay(i), 10);
    }

    public Bitmap getFrameBitmap(int i) throws Throwable {
        if (this.mGifDecoder == null) {
            return null;
        }
        this.mGifDecoder.advance();
        try {
            Bitmap nextFrame = this.mGifDecoder.getNextFrame();
            if (ENABLE_DEBUG) {
                DebugUtils.dumpBitmap(nextFrame, i + "");
            }
            return nextFrame;
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "<getFrameBitmap> IllegalArgumentException:" + e);
            return null;
        }
    }

    private static byte[] inputStreamToBytes(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
        try {
            byte[] bArr = new byte[BUFFER_SIZE];
            while (true) {
                int i = inputStream.read(bArr);
                if (i != -1) {
                    byteArrayOutputStream.write(bArr, 0, i);
                } else {
                    byteArrayOutputStream.flush();
                    return byteArrayOutputStream.toByteArray();
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "<inputStreamToBytes>", e);
            return null;
        }
    }

    private static class GalleryBitmapProvider implements GifDecoder.BitmapProvider {
        private GalleryBitmapProvider() {
        }

        @Override
        public Bitmap obtain(int i, int i2, Bitmap.Config config) {
            return null;
        }

        @Override
        public void release(Bitmap bitmap) {
        }
    }
}
