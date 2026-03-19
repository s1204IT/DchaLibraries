package android.graphics;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;
import android.util.TypedValue;
import com.mediatek.dcfdecoder.DcfDecoder;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapFactory {
    private static final int DECODE_BUFFER_SIZE = 65536;

    private static native Bitmap nativeDecodeAsset(long j, Rect rect, Options options);

    private static native Bitmap nativeDecodeByteArray(byte[] bArr, int i, int i2, Options options);

    private static native Bitmap nativeDecodeFileDescriptor(FileDescriptor fileDescriptor, Rect rect, Options options);

    private static native Bitmap nativeDecodeStream(InputStream inputStream, byte[] bArr, Rect rect, Options options);

    private static native boolean nativeIsSeekable(FileDescriptor fileDescriptor);

    public static class Options {
        public Bitmap inBitmap;
        public int inDensity;

        @Deprecated
        public boolean inInputShareable;
        public boolean inJustDecodeBounds;
        public boolean inMutable;
        public boolean inPreferQualityOverSpeed;

        @Deprecated
        public boolean inPurgeable;
        public int inSampleSize;
        public int inScreenDensity;
        public int inTargetDensity;
        public byte[] inTempStorage;
        public boolean mCancel;
        public int outHeight;
        public String outMimeType;
        public int outWidth;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public boolean inDither = false;
        public boolean inScaled = true;
        public boolean inPremultiplied = true;
        public int inPreferSize = 0;
        public boolean inPostProc = false;
        public int inPostProcFlag = 0;

        public void requestCancelDecode() {
            this.mCancel = true;
        }
    }

    public static Bitmap decodeFile(String pathName, Options opts) throws Throwable {
        InputStream stream;
        Bitmap bm = null;
        InputStream stream2 = null;
        try {
            try {
                Log.d("BitmapFactory", "decodeFile() pathName = " + pathName);
                stream = new FileInputStream(pathName);
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            bm = decodeStream(stream, null, opts);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e2) {
                }
            }
        } catch (Exception e3) {
            e = e3;
            stream2 = stream;
            Log.e("BitmapFactory", "Unable to decode stream: " + e);
            if (stream2 != null) {
                try {
                    stream2.close();
                } catch (IOException e4) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            stream2 = stream;
            if (stream2 != null) {
                try {
                    stream2.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
        return bm;
    }

    public static Bitmap decodeFile(String pathName) {
        return decodeFile(pathName, null);
    }

    public static Bitmap decodeResourceStream(Resources res, TypedValue value, InputStream is, Rect pad, Options opts) {
        if (opts == null) {
            opts = new Options();
        }
        if (opts.inDensity == 0 && value != null) {
            int density = value.density;
            if (density == 0) {
                opts.inDensity = 160;
            } else if (density != 65535) {
                opts.inDensity = density;
            }
        }
        if (opts.inTargetDensity == 0 && res != null) {
            opts.inTargetDensity = res.getDisplayMetrics().densityDpi;
        }
        return decodeStream(is, pad, opts);
    }

    public static Bitmap decodeResource(Resources res, int id, Options opts) {
        Bitmap bm = null;
        InputStream is = null;
        try {
            TypedValue value = new TypedValue();
            is = res.openRawResource(id, value);
            bm = decodeResourceStream(res, value, is, null, opts);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        } catch (Exception e2) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e3) {
                }
            }
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
        if (bm == null && opts != null && opts.inBitmap != null) {
            throw new IllegalArgumentException("Problem decoding into existing bitmap");
        }
        return bm;
    }

    public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts) {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Trace.traceBegin(2L, "decodeBitmap");
        try {
            Bitmap bm = nativeDecodeByteArray(data, offset, length, opts);
            if (bm == null && opts != null && opts.inBitmap != null) {
                throw new IllegalArgumentException("Problem decoding into existing bitmap");
            }
            setDensityFromOptions(bm, opts);
            if (bm == null) {
                return DcfDecoder.decodeDrmImageIfNeeded(data, opts);
            }
            return bm;
        } finally {
            Trace.traceEnd(2L);
        }
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }

    private static void setDensityFromOptions(Bitmap outputBitmap, Options opts) {
        if (outputBitmap == null || opts == null) {
            return;
        }
        int density = opts.inDensity;
        if (density != 0) {
            outputBitmap.setDensity(density);
            int targetDensity = opts.inTargetDensity;
            if (targetDensity == 0 || density == targetDensity || density == opts.inScreenDensity) {
                return;
            }
            byte[] np = outputBitmap.getNinePatchChunk();
            boolean zIsNinePatchChunk = np != null ? NinePatch.isNinePatchChunk(np) : false;
            if (!opts.inScaled && !zIsNinePatchChunk) {
                return;
            }
            outputBitmap.setDensity(targetDensity);
            return;
        }
        if (opts.inBitmap == null) {
            return;
        }
        outputBitmap.setDensity(Bitmap.getDefaultDensity());
    }

    public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts) {
        Bitmap bm;
        if (is == null) {
            return null;
        }
        Trace.traceBegin(2L, "decodeBitmap");
        try {
            if (is instanceof AssetManager.AssetInputStream) {
                long asset = ((AssetManager.AssetInputStream) is).getNativeAsset();
                bm = nativeDecodeAsset(asset, outPadding, opts);
            } else {
                bm = decodeStreamInternal(is, outPadding, opts);
            }
            if (bm == null && opts != null && opts.inBitmap != null) {
                throw new IllegalArgumentException("Problem decoding into existing bitmap");
            }
            setDensityFromOptions(bm, opts);
            return bm;
        } finally {
            Trace.traceEnd(2L);
        }
    }

    private static Bitmap decodeStreamInternal(InputStream is, Rect outPadding, Options opts) {
        byte[] tempStorage = opts != null ? opts.inTempStorage : null;
        if (tempStorage == null) {
            tempStorage = new byte[65536];
        }
        Bitmap bm = nativeDecodeStream(is, tempStorage, outPadding, opts);
        if (bm == null) {
            return DcfDecoder.decodeDrmImageIfNeeded(tempStorage, is, opts);
        }
        return bm;
    }

    public static Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null);
    }

    public static Bitmap decodeFileDescriptor(FileDescriptor fd, Rect outPadding, Options opts) {
        Bitmap bm;
        Trace.traceBegin(2L, "decodeFileDescriptor");
        try {
            if (nativeIsSeekable(fd)) {
                bm = nativeDecodeFileDescriptor(fd, outPadding, opts);
            } else {
                FileInputStream fis = new FileInputStream(fd);
                try {
                    bm = decodeStreamInternal(fis, outPadding, opts);
                    try {
                        fis.close();
                    } catch (Throwable th) {
                    }
                } catch (Throwable th2) {
                    try {
                        fis.close();
                    } catch (Throwable th3) {
                    }
                    throw th2;
                }
            }
            if (bm == null && opts != null && opts.inBitmap != null) {
                throw new IllegalArgumentException("Problem decoding into existing bitmap");
            }
            setDensityFromOptions(bm, opts);
            if (bm == null) {
                return DcfDecoder.decodeDrmImageIfNeeded(fd, opts);
            }
            return bm;
        } finally {
            Trace.traceEnd(2L);
        }
    }

    public static Bitmap decodeFileDescriptor(FileDescriptor fd) {
        return decodeFileDescriptor(fd, null, null);
    }
}
