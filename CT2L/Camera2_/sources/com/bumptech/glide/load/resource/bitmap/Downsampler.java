package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;
import com.bumptech.glide.util.ByteArrayPool;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;

public abstract class Downsampler implements BitmapDecoder<InputStream> {
    public static Downsampler AT_LEAST = null;
    public static Downsampler AT_MOST = null;
    private static final boolean CAN_RECYCLE;
    private static final int MARK_POSITION = 5242880;
    public static Downsampler NONE = null;
    private static final Queue<BitmapFactory.Options> OPTIONS_QUEUE;
    private static final String TAG = "Downsampler";
    private static final Set<ImageHeaderParser.ImageType> TYPES_THAT_USE_POOL;

    protected abstract int getSampleSize(int i, int i2, int i3, int i4);

    static {
        CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;
        TYPES_THAT_USE_POOL = EnumSet.of(ImageHeaderParser.ImageType.JPEG, ImageHeaderParser.ImageType.PNG_A, ImageHeaderParser.ImageType.PNG);
        OPTIONS_QUEUE = new ArrayDeque();
        AT_LEAST = new Downsampler() {
            @Override
            public Bitmap decode(InputStream inputStream, BitmapPool bitmapPool, int i, int i2, DecodeFormat decodeFormat) throws Exception {
                return super.decode(inputStream, bitmapPool, i, i2, decodeFormat);
            }

            @Override
            protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
                return Math.min(inHeight / outHeight, inWidth / outWidth);
            }

            @Override
            public String getId() {
                return "AT_LEAST.com.bumptech.glide.load.data.bitmap";
            }
        };
        AT_MOST = new Downsampler() {
            @Override
            public Bitmap decode(InputStream inputStream, BitmapPool bitmapPool, int i, int i2, DecodeFormat decodeFormat) throws Exception {
                return super.decode(inputStream, bitmapPool, i, i2, decodeFormat);
            }

            @Override
            protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
                return Math.max(inHeight / outHeight, inWidth / outWidth);
            }

            @Override
            public String getId() {
                return "AT_MOST.com.bumptech.glide.load.data.bitmap";
            }
        };
        NONE = new Downsampler() {
            @Override
            public Bitmap decode(InputStream inputStream, BitmapPool bitmapPool, int i, int i2, DecodeFormat decodeFormat) throws Exception {
                return super.decode(inputStream, bitmapPool, i, i2, decodeFormat);
            }

            @Override
            protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
                return 0;
            }

            @Override
            public String getId() {
                return "NONE.com.bumptech.glide.load.data.bitmap";
            }
        };
    }

    @TargetApi(11)
    private static synchronized BitmapFactory.Options getDefaultOptions() {
        BitmapFactory.Options decodeBitmapOptions;
        decodeBitmapOptions = OPTIONS_QUEUE.poll();
        if (decodeBitmapOptions == null) {
            decodeBitmapOptions = new BitmapFactory.Options();
            resetOptions(decodeBitmapOptions);
        }
        return decodeBitmapOptions;
    }

    private static void releaseOptions(BitmapFactory.Options decodeBitmapOptions) {
        resetOptions(decodeBitmapOptions);
        OPTIONS_QUEUE.offer(decodeBitmapOptions);
    }

    @TargetApi(11)
    private static void resetOptions(BitmapFactory.Options decodeBitmapOptions) {
        decodeBitmapOptions.inTempStorage = null;
        decodeBitmapOptions.inDither = false;
        decodeBitmapOptions.inScaled = false;
        decodeBitmapOptions.inSampleSize = 1;
        decodeBitmapOptions.inPreferredConfig = null;
        decodeBitmapOptions.inJustDecodeBounds = false;
        if (CAN_RECYCLE) {
            decodeBitmapOptions.inBitmap = null;
            decodeBitmapOptions.inMutable = true;
        }
    }

    @Override
    public Bitmap decode(InputStream is, BitmapPool pool, int outWidth, int outHeight, DecodeFormat decodeFormat) {
        int sampleSize;
        ByteArrayPool byteArrayPool = ByteArrayPool.get();
        byte[] bytesForOptions = byteArrayPool.getBytes();
        byte[] bytesForStream = byteArrayPool.getBytes();
        RecyclableBufferedInputStream bis = new RecyclableBufferedInputStream(is, bytesForStream);
        bis.mark(MARK_POSITION);
        int orientation = 0;
        try {
            orientation = new ImageHeaderParser(bis).getOrientation();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bis.reset();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        BitmapFactory.Options options = getDefaultOptions();
        options.inTempStorage = bytesForOptions;
        int[] inDimens = getDimensions(bis, options);
        int inWidth = inDimens[0];
        int inHeight = inDimens[1];
        int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);
        if (degreesToRotate == 90 || degreesToRotate == 270) {
            sampleSize = getSampleSize(inHeight, inWidth, outWidth, outHeight);
        } else {
            sampleSize = getSampleSize(inWidth, inHeight, outWidth, outHeight);
        }
        Bitmap downsampled = downsampleWithSize(bis, options, pool, inWidth, inHeight, sampleSize, decodeFormat);
        Bitmap rotated = null;
        if (downsampled != null) {
            rotated = TransformationUtils.rotateImageExif(downsampled, pool, orientation);
            if (downsampled != rotated && !pool.put(downsampled)) {
                downsampled.recycle();
            }
        }
        byteArrayPool.releaseBytes(bytesForOptions);
        byteArrayPool.releaseBytes(bytesForStream);
        releaseOptions(options);
        return rotated;
    }

    protected Bitmap downsampleWithSize(RecyclableBufferedInputStream bis, BitmapFactory.Options options, BitmapPool pool, int inWidth, int inHeight, int sampleSize, DecodeFormat decodeFormat) {
        Bitmap.Config config = getConfig(bis, decodeFormat);
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = config;
        if ((options.inSampleSize == 1 || Build.VERSION.SDK_INT >= 19) && shouldUsePool(bis)) {
            setInBitmap(options, pool.get(inWidth, inHeight, config));
        }
        return decodeStream(bis, options);
    }

    private boolean shouldUsePool(RecyclableBufferedInputStream bis) {
        boolean zContains;
        if (Build.VERSION.SDK_INT >= 19) {
            return true;
        }
        bis.mark(1024);
        try {
            try {
                ImageHeaderParser.ImageType type = new ImageHeaderParser(bis).getType();
                zContains = TYPES_THAT_USE_POOL.contains(type);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    bis.reset();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                zContains = false;
            }
            return zContains;
        } finally {
            try {
                bis.reset();
            } catch (IOException e3) {
                e3.printStackTrace();
            }
        }
    }

    private Bitmap.Config getConfig(RecyclableBufferedInputStream bis, DecodeFormat format) {
        if (format == DecodeFormat.ALWAYS_ARGB_8888) {
            return Bitmap.Config.ARGB_8888;
        }
        boolean hasAlpha = false;
        bis.mark(1024);
        try {
            try {
                hasAlpha = new ImageHeaderParser(bis).hasAlpha();
            } finally {
                try {
                    bis.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e2) {
            e2.printStackTrace();
            try {
                bis.reset();
            } catch (IOException e3) {
                e3.printStackTrace();
            }
        }
        return hasAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
    }

    public int[] getDimensions(RecyclableBufferedInputStream bis, BitmapFactory.Options options) {
        options.inJustDecodeBounds = true;
        decodeStream(bis, options);
        options.inJustDecodeBounds = false;
        return new int[]{options.outWidth, options.outHeight};
    }

    private Bitmap decodeStream(RecyclableBufferedInputStream bis, BitmapFactory.Options options) {
        if (options.inJustDecodeBounds) {
            bis.mark(MARK_POSITION);
        }
        Bitmap result = BitmapFactory.decodeStream(bis, null, options);
        try {
            if (options.inJustDecodeBounds) {
                bis.reset();
                bis.clearMark();
            } else {
                bis.close();
            }
        } catch (IOException e) {
            if (Log.isLoggable(TAG, 6)) {
                Log.e(TAG, "Exception loading inDecodeBounds=" + options.inJustDecodeBounds + " sample=" + options.inSampleSize, e);
            }
        }
        return result;
    }

    @TargetApi(11)
    private static void setInBitmap(BitmapFactory.Options options, Bitmap recycled) {
        if (Build.VERSION.SDK_INT >= 11) {
            options.inBitmap = recycled;
        }
    }
}
