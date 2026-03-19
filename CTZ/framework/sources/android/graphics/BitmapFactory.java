package android.graphics;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Trace;
import android.util.Log;
import android.util.TypedValue;
import com.mediatek.dcfDecoder.MTKDcfDecoderFactory;
import com.mediatek.dcfDecoder.MTKDcfDecoderManager;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapFactory {
    private static final int DECODE_BUFFER_SIZE = 16384;
    private static MTKDcfDecoderManager mMTKDcfDecoderManager = MTKDcfDecoderFactory.getInstance().makeMTKDcfDecoderManager();

    private static native Bitmap nativeDecodeAsset(long j, Rect rect, Options options);

    private static native Bitmap nativeDecodeByteArray(byte[] bArr, int i, int i2, Options options);

    private static native Bitmap nativeDecodeFileDescriptor(FileDescriptor fileDescriptor, Rect rect, Options options);

    private static native Bitmap nativeDecodeStream(InputStream inputStream, byte[] bArr, Rect rect, Options options);

    private static native boolean nativeIsSeekable(FileDescriptor fileDescriptor);

    public static class Options {
        public Bitmap inBitmap;
        public int inDensity;
        public boolean inDither;

        @Deprecated
        public boolean inInputShareable;
        public boolean inJustDecodeBounds;
        public boolean inMutable;

        @Deprecated
        public boolean inPreferQualityOverSpeed;

        @Deprecated
        public boolean inPurgeable;
        public int inSampleSize;
        public int inScreenDensity;
        public int inTargetDensity;
        public byte[] inTempStorage;

        @Deprecated
        public boolean mCancel;
        public ColorSpace outColorSpace;
        public Bitmap.Config outConfig;
        public int outHeight;
        public String outMimeType;
        public int outWidth;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public ColorSpace inPreferredColorSpace = null;
        public boolean inScaled = true;
        public boolean inPremultiplied = true;
        public boolean inPostProc = false;
        public int inPostProcFlag = 0;

        @Deprecated
        public void requestCancelDecode() {
            this.mCancel = true;
        }

        static void validate(Options options) {
            if (options == null) {
                return;
            }
            if (options.inBitmap != null && options.inBitmap.getConfig() == Bitmap.Config.HARDWARE) {
                throw new IllegalArgumentException("Bitmaps with Config.HARWARE are always immutable");
            }
            if (options.inMutable && options.inPreferredConfig == Bitmap.Config.HARDWARE) {
                throw new IllegalArgumentException("Bitmaps with Config.HARDWARE cannot be decoded into - they are immutable");
            }
            if (options.inPreferredColorSpace != null) {
                if (!(options.inPreferredColorSpace instanceof ColorSpace.Rgb)) {
                    throw new IllegalArgumentException("The destination color space must use the RGB color model");
                }
                if (((ColorSpace.Rgb) options.inPreferredColorSpace).getTransferParameters() == null) {
                    throw new IllegalArgumentException("The destination color space must use an ICC parametric transfer function");
                }
            }
        }
    }

    public static Bitmap decodeFile(String str, Options options) throws Throwable {
        FileInputStream fileInputStream;
        Options.validate(options);
        try {
            fileInputStream = new FileInputStream(str);
            try {
                try {
                    Bitmap bitmapDecodeStream = decodeStream(fileInputStream, null, options);
                    try {
                        fileInputStream.close();
                        return bitmapDecodeStream;
                    } catch (IOException e) {
                        return bitmapDecodeStream;
                    }
                } catch (Exception e2) {
                    e = e2;
                    Log.e("BitmapFactory", "Unable to decode stream: " + e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e3) {
                        }
                    }
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e = e5;
            fileInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            fileInputStream = null;
            if (fileInputStream != null) {
            }
            throw th;
        }
    }

    public static Bitmap decodeFile(String str) {
        return decodeFile(str, null);
    }

    public static Bitmap decodeResourceStream(Resources resources, TypedValue typedValue, InputStream inputStream, Rect rect, Options options) {
        Options.validate(options);
        if (options == null) {
            options = new Options();
        }
        if (options.inDensity == 0 && typedValue != null) {
            int i = typedValue.density;
            if (i == 0) {
                options.inDensity = 160;
            } else if (i != 65535) {
                options.inDensity = i;
            }
        }
        if (options.inTargetDensity == 0 && resources != null) {
            options.inTargetDensity = resources.getDisplayMetrics().densityDpi;
        }
        return decodeStream(inputStream, rect, options);
    }

    public static Bitmap decodeResource(Resources resources, int i, Options options) throws Throwable {
        InputStream inputStreamOpenRawResource;
        Bitmap bitmapDecodeResourceStream;
        Options.validate(options);
        InputStream inputStream = null;
        try {
            TypedValue typedValue = new TypedValue();
            inputStreamOpenRawResource = resources.openRawResource(i, typedValue);
            try {
                bitmapDecodeResourceStream = decodeResourceStream(resources, typedValue, inputStreamOpenRawResource, null, options);
                if (inputStreamOpenRawResource != null) {
                    try {
                        inputStreamOpenRawResource.close();
                    } catch (IOException e) {
                    }
                }
            } catch (Exception e2) {
                if (inputStreamOpenRawResource != null) {
                    try {
                        inputStreamOpenRawResource.close();
                    } catch (IOException e3) {
                    }
                }
                bitmapDecodeResourceStream = null;
            } catch (Throwable th) {
                th = th;
                inputStream = inputStreamOpenRawResource;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            inputStreamOpenRawResource = null;
        } catch (Throwable th2) {
            th = th2;
        }
        if (bitmapDecodeResourceStream == null && options != null && options.inBitmap != null) {
            throw new IllegalArgumentException("Problem decoding into existing bitmap");
        }
        return bitmapDecodeResourceStream;
    }

    public static Bitmap decodeResource(Resources resources, int i) {
        return decodeResource(resources, i, null);
    }

    public static Bitmap decodeByteArray(byte[] bArr, int i, int i2, Options options) {
        if ((i | i2) < 0 || bArr.length < i + i2) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Options.validate(options);
        Trace.traceBegin(2L, "decodeBitmap");
        try {
            Bitmap bitmapNativeDecodeByteArray = nativeDecodeByteArray(bArr, i, i2, options);
            if (bitmapNativeDecodeByteArray == null && options != null && options.inBitmap != null) {
                throw new IllegalArgumentException("Problem decoding into existing bitmap");
            }
            setDensityFromOptions(bitmapNativeDecodeByteArray, options);
            if (bitmapNativeDecodeByteArray == null) {
                return mMTKDcfDecoderManager.decodeDrmImageIfNeededImpl(bArr, options);
            }
            return bitmapNativeDecodeByteArray;
        } finally {
            Trace.traceEnd(2L);
        }
    }

    public static Bitmap decodeByteArray(byte[] bArr, int i, int i2) {
        return decodeByteArray(bArr, i, i2, null);
    }

    private static void setDensityFromOptions(Bitmap bitmap, Options options) {
        if (bitmap == null || options == null) {
            return;
        }
        int i = options.inDensity;
        if (i != 0) {
            bitmap.setDensity(i);
            int i2 = options.inTargetDensity;
            if (i2 == 0 || i == i2 || i == options.inScreenDensity) {
                return;
            }
            byte[] ninePatchChunk = bitmap.getNinePatchChunk();
            boolean z = ninePatchChunk != null && NinePatch.isNinePatchChunk(ninePatchChunk);
            if (options.inScaled || z) {
                bitmap.setDensity(i2);
                return;
            }
            return;
        }
        if (options.inBitmap != null) {
            bitmap.setDensity(Bitmap.getDefaultDensity());
        }
    }

    public static Bitmap decodeStream(InputStream inputStream, Rect rect, Options options) {
        Bitmap bitmapDecodeStreamInternal;
        if (inputStream == null) {
            return null;
        }
        Options.validate(options);
        Trace.traceBegin(2L, "decodeBitmap");
        try {
            if (inputStream instanceof AssetManager.AssetInputStream) {
                bitmapDecodeStreamInternal = nativeDecodeAsset(((AssetManager.AssetInputStream) inputStream).getNativeAsset(), rect, options);
            } else {
                bitmapDecodeStreamInternal = decodeStreamInternal(inputStream, rect, options);
            }
            if (bitmapDecodeStreamInternal == null && options != null && options.inBitmap != null) {
                throw new IllegalArgumentException("Problem decoding into existing bitmap");
            }
            setDensityFromOptions(bitmapDecodeStreamInternal, options);
            return bitmapDecodeStreamInternal;
        } finally {
            Trace.traceEnd(2L);
        }
    }

    private static Bitmap decodeStreamInternal(InputStream inputStream, Rect rect, Options options) {
        byte[] bArr = options != null ? options.inTempStorage : null;
        if (bArr == null) {
            bArr = new byte[16384];
        }
        Bitmap bitmapNativeDecodeStream = nativeDecodeStream(inputStream, bArr, rect, options);
        if (bitmapNativeDecodeStream == null) {
            return mMTKDcfDecoderManager.decodeDrmImageIfNeededImpl(bArr, inputStream, options);
        }
        return bitmapNativeDecodeStream;
    }

    public static Bitmap decodeStream(InputStream inputStream) {
        return decodeStream(inputStream, null, null);
    }

    public static Bitmap decodeFileDescriptor(FileDescriptor fileDescriptor, Rect rect, Options options) {
        Bitmap bitmapDecodeStreamInternal;
        Options.validate(options);
        Trace.traceBegin(2L, "decodeFileDescriptor");
        try {
            if (nativeIsSeekable(fileDescriptor)) {
                bitmapDecodeStreamInternal = nativeDecodeFileDescriptor(fileDescriptor, rect, options);
            } else {
                FileInputStream fileInputStream = new FileInputStream(fileDescriptor);
                try {
                    bitmapDecodeStreamInternal = decodeStreamInternal(fileInputStream, rect, options);
                } finally {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th) {
                    }
                }
            }
            if (bitmapDecodeStreamInternal == null && options != null && options.inBitmap != null) {
                throw new IllegalArgumentException("Problem decoding into existing bitmap");
            }
            setDensityFromOptions(bitmapDecodeStreamInternal, options);
            if (bitmapDecodeStreamInternal == null) {
                return mMTKDcfDecoderManager.decodeDrmImageIfNeededImpl(fileDescriptor, options);
            }
            return bitmapDecodeStreamInternal;
        } finally {
            Trace.traceEnd(2L);
        }
    }

    public static Bitmap decodeFileDescriptor(FileDescriptor fileDescriptor) {
        return decodeFileDescriptor(fileDescriptor, null, null);
    }
}
