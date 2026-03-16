package com.android.providers.contacts;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemProperties;
import com.android.providers.contacts.util.MemoryUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class PhotoProcessor {
    private static final Paint WHITE_PAINT = new Paint();
    private static int sMaxDisplayPhotoDim;
    private static int sMaxThumbnailDim;
    private Bitmap mDisplayPhoto;
    private final boolean mForceCropToSquare;
    private final int mMaxDisplayPhotoDim;
    private final int mMaxThumbnailPhotoDim;
    private final Bitmap mOriginal;
    private Bitmap mThumbnailPhoto;

    static {
        WHITE_PAINT.setColor(-1);
        boolean isExpensiveDevice = MemoryUtils.getTotalMemorySize() >= 671088640;
        sMaxThumbnailDim = SystemProperties.getInt("contacts.thumbnail_size", 96);
        sMaxDisplayPhotoDim = SystemProperties.getInt("contacts.display_photo_size", isExpensiveDevice ? 720 : 480);
    }

    public PhotoProcessor(Bitmap original, int maxDisplayPhotoDim, int maxThumbnailPhotoDim) throws IOException {
        this(original, maxDisplayPhotoDim, maxThumbnailPhotoDim, false);
    }

    public PhotoProcessor(byte[] originalBytes, int maxDisplayPhotoDim, int maxThumbnailPhotoDim) throws IOException {
        this(BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length), maxDisplayPhotoDim, maxThumbnailPhotoDim, false);
    }

    public PhotoProcessor(Bitmap original, int maxDisplayPhotoDim, int maxThumbnailPhotoDim, boolean forceCropToSquare) throws IOException {
        this.mOriginal = original;
        this.mMaxDisplayPhotoDim = maxDisplayPhotoDim;
        this.mMaxThumbnailPhotoDim = maxThumbnailPhotoDim;
        this.mForceCropToSquare = forceCropToSquare;
        process();
    }

    public PhotoProcessor(byte[] originalBytes, int maxDisplayPhotoDim, int maxThumbnailPhotoDim, boolean forceCropToSquare) throws IOException {
        this(BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length), maxDisplayPhotoDim, maxThumbnailPhotoDim, forceCropToSquare);
    }

    private void process() throws IOException {
        if (this.mOriginal == null) {
            throw new IOException("Invalid image file");
        }
        this.mDisplayPhoto = getNormalizedBitmap(this.mOriginal, this.mMaxDisplayPhotoDim, this.mForceCropToSquare);
        this.mThumbnailPhoto = getNormalizedBitmap(this.mOriginal, this.mMaxThumbnailPhotoDim, this.mForceCropToSquare);
    }

    static Bitmap getNormalizedBitmap(Bitmap original, int maxDim, boolean forceCropToSquare) throws IOException {
        boolean originalHasAlpha = original.hasAlpha();
        int cropWidth = original.getWidth();
        int cropHeight = original.getHeight();
        int cropLeft = 0;
        int cropTop = 0;
        if (forceCropToSquare && cropWidth != cropHeight) {
            if (cropHeight > cropWidth) {
                cropTop = (cropHeight - cropWidth) / 2;
                cropHeight = cropWidth;
            } else {
                cropLeft = (cropWidth - cropHeight) / 2;
                cropWidth = cropHeight;
            }
        }
        float scaleFactor = Math.min(1.0f, maxDim / Math.max(cropWidth, cropHeight));
        if (scaleFactor < 1.0f || cropLeft != 0 || cropTop != 0 || originalHasAlpha) {
            int newWidth = (int) (cropWidth * scaleFactor);
            int newHeight = (int) (cropHeight * scaleFactor);
            if (newWidth <= 0 || newHeight <= 0) {
                throw new IOException("Invalid bitmap dimensions");
            }
            Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(scaledBitmap);
            if (originalHasAlpha) {
                c.drawRect(0.0f, 0.0f, scaledBitmap.getWidth(), scaledBitmap.getHeight(), WHITE_PAINT);
            }
            Rect src = new Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight);
            RectF dst = new RectF(0.0f, 0.0f, scaledBitmap.getWidth(), scaledBitmap.getHeight());
            c.drawBitmap(original, src, dst, (Paint) null);
            return scaledBitmap;
        }
        return original;
    }

    private byte[] getCompressedBytes(Bitmap b, int quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean compressed = b.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        baos.flush();
        baos.close();
        byte[] result = baos.toByteArray();
        if (!compressed) {
            throw new IOException("Unable to compress image");
        }
        return result;
    }

    public Bitmap getDisplayPhoto() {
        return this.mDisplayPhoto;
    }

    public byte[] getDisplayPhotoBytes() throws IOException {
        return getCompressedBytes(this.mDisplayPhoto, 75);
    }

    public byte[] getThumbnailPhotoBytes() throws IOException {
        boolean hasDisplayPhoto = this.mDisplayPhoto != null && (this.mDisplayPhoto.getWidth() > this.mThumbnailPhoto.getWidth() || this.mDisplayPhoto.getHeight() > this.mThumbnailPhoto.getHeight());
        return getCompressedBytes(this.mThumbnailPhoto, hasDisplayPhoto ? 90 : 95);
    }

    public int getMaxThumbnailPhotoDim() {
        return this.mMaxThumbnailPhotoDim;
    }

    public static int getMaxThumbnailSize() {
        return sMaxThumbnailDim;
    }

    public static int getMaxDisplayPhotoSize() {
        return sMaxDisplayPhotoDim;
    }
}
