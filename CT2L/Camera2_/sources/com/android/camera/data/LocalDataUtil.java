package com.android.camera.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import com.android.camera.debug.Log;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class LocalDataUtil {
    private static final Log.Tag TAG = new Log.Tag("LocalDataUtil");

    public static boolean isMimeTypeVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public static boolean isMimeTypeImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public static Point decodeBitmapDimension(String path) throws Throwable {
        InputStream is;
        Point size = null;
        InputStream is2 = null;
        try {
            try {
                is = new FileInputStream(path);
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        }
        try {
            size = decodeBitmapDimension(is);
            if (is != null) {
                try {
                    is.close();
                    is2 = is;
                } catch (IOException e2) {
                    is2 = is;
                }
            } else {
                is2 = is;
            }
        } catch (FileNotFoundException e3) {
            e = e3;
            is2 = is;
            e.printStackTrace();
            if (is2 != null) {
                try {
                    is2.close();
                } catch (IOException e4) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            is2 = is;
            if (is2 != null) {
                try {
                    is2.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
        return size;
    }

    public static Point decodeBitmapDimension(InputStream is) {
        BitmapFactory.Options justBoundsOpts = new BitmapFactory.Options();
        justBoundsOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, justBoundsOpts);
        if (justBoundsOpts.outWidth > 0 && justBoundsOpts.outHeight > 0) {
            Point size = new Point(justBoundsOpts.outWidth, justBoundsOpts.outHeight);
            return size;
        }
        Log.e(TAG, "Bitmap dimension decoding failed");
        return null;
    }

    public static Bitmap loadImageThumbnailFromStream(InputStream stream, int imageWidth, int imageHeight, int widthBound, int heightBound, int orientation, int maximumPixels) {
        byte[] decodeBuffer = new byte[32768];
        if (orientation % 180 != 0) {
            imageHeight = imageWidth;
            imageWidth = imageHeight;
        }
        int targetWidth = imageWidth;
        int targetHeight = imageHeight;
        int sampleSize = 1;
        while (true) {
            if (targetHeight <= heightBound && targetWidth <= widthBound && targetHeight <= 3379 && targetWidth <= 3379 && targetHeight * targetWidth <= maximumPixels) {
                break;
            }
            sampleSize <<= 1;
            targetWidth = imageWidth / sampleSize;
            targetHeight = imageWidth / sampleSize;
        }
        if ((heightBound > 3379 || widthBound > 3379) && targetWidth * targetHeight < maximumPixels / 4 && sampleSize > 1) {
            sampleSize >>= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize;
        opts.inTempStorage = decodeBuffer;
        Bitmap b = BitmapFactory.decodeStream(stream, null, opts);
        if (b == null) {
            return null;
        }
        if (b.getWidth() > 3379 || b.getHeight() > 3379) {
            int maxEdge = Math.max(b.getWidth(), b.getHeight());
            b = Bitmap.createScaledBitmap(b, (b.getWidth() * 3379) / maxEdge, (b.getHeight() * 3379) / maxEdge, false);
        }
        if (orientation != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(orientation);
            b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, false);
        }
        return b;
    }

    public static Bitmap loadVideoThumbnail(String path) throws IOException {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] data = retriever.getEmbeddedPicture();
            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
            if (bitmap == null) {
                bitmap = retriever.getFrameAtTime();
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "MediaMetadataRetriever.setDataSource() fail:" + e.getMessage());
        }
        retriever.release();
        return bitmap;
    }
}
