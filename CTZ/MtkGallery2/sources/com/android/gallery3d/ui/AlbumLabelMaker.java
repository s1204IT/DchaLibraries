package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.galleryportable.TraceHelper;

public class AlbumLabelMaker {
    private static final int BORDER_SIZE = 0;
    private int mBitmapHeight;
    private int mBitmapWidth;
    private final Context mContext;
    private final TextPaint mCountPaint;
    private int mLabelWidth;
    private final AlbumSetSlotRenderer.LabelSpec mSpec;
    private final TextPaint mTitlePaint;
    private final LazyLoadedBitmap mLocalSetIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_folder);
    private final LazyLoadedBitmap mPicasaIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_picasa);
    private final LazyLoadedBitmap mCameraIcon = new LazyLoadedBitmap(R.drawable.frame_overlay_gallery_camera);

    public AlbumLabelMaker(Context context, AlbumSetSlotRenderer.LabelSpec labelSpec) {
        this.mContext = context;
        this.mSpec = labelSpec;
        this.mTitlePaint = getTextPaint(labelSpec.titleFontSize, labelSpec.titleColor, false);
        this.mCountPaint = getTextPaint(labelSpec.countFontSize, labelSpec.countColor, false);
    }

    public static int getBorderSize() {
        return 0;
    }

    private Bitmap getOverlayAlbumIcon(int i) {
        switch (i) {
            case 1:
                return this.mLocalSetIcon.get();
            case 2:
                return this.mPicasaIcon.get();
            case 3:
                return this.mCameraIcon.get();
            default:
                return null;
        }
    }

    private static TextPaint getTextPaint(int i, int i2, boolean z) {
        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(i);
        textPaint.setAntiAlias(true);
        textPaint.setColor(i2);
        if (z) {
            textPaint.setTypeface(Typeface.defaultFromStyle(1));
        }
        return textPaint;
    }

    private class LazyLoadedBitmap {
        private Bitmap mBitmap;
        private int mResId;

        public LazyLoadedBitmap(int i) {
            this.mResId = i;
        }

        public synchronized Bitmap get() {
            if (this.mBitmap == null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                this.mBitmap = BitmapFactory.decodeResource(AlbumLabelMaker.this.mContext.getResources(), this.mResId, options);
            }
            return this.mBitmap;
        }
    }

    public synchronized void setLabelWidth(int i) {
        if (this.mLabelWidth == i) {
            return;
        }
        this.mLabelWidth = i;
        this.mBitmapWidth = i + 0;
        this.mBitmapHeight = this.mSpec.labelBackgroundHeight + 0;
    }

    public ThreadPool.Job<Bitmap> requestLabel(String str, String str2, int i) {
        return new AlbumLabelJob(str, str2, i);
    }

    static void drawText(Canvas canvas, int i, int i2, String str, int i3, TextPaint textPaint) {
        synchronized (textPaint) {
            canvas.drawText(TextUtils.ellipsize(str, textPaint, i3, TextUtils.TruncateAt.END).toString(), i, i2 - textPaint.getFontMetricsInt().ascent, textPaint);
        }
    }

    private class AlbumLabelJob implements ThreadPool.Job<Bitmap> {
        private final String mCount;
        private final boolean mIsFancyLayout;
        private final boolean mIsLandCamera;
        private final int mSourceType;
        private final String mTitle;

        public AlbumLabelJob(String str, String str2, int i, boolean z, boolean z2) {
            this.mTitle = str;
            this.mCount = str2;
            this.mSourceType = i;
            this.mIsLandCamera = z;
            this.mIsFancyLayout = z2;
        }

        public AlbumLabelJob(String str, String str2, int i) {
            this.mTitle = str;
            this.mCount = str2;
            this.mSourceType = i;
            this.mIsLandCamera = false;
            this.mIsFancyLayout = false;
        }

        @Override
        public Bitmap run(ThreadPool.JobContext jobContext) {
            int slotWidthAtFancyMode;
            Bitmap bitmapCreateBitmap;
            int fullScreenLabelWidth;
            float f;
            TraceHelper.beginSection(">>>>AlbumLabelMaker-AlbumLabelJob.run");
            AlbumSetSlotRenderer.LabelSpec labelSpec = AlbumLabelMaker.this.mSpec;
            String str = this.mTitle;
            String str2 = this.mCount;
            Bitmap overlayAlbumIcon = AlbumLabelMaker.this.getOverlayAlbumIcon(this.mSourceType);
            synchronized (this) {
                if (!FancyHelper.isFancyLayoutSupported() || !this.mIsFancyLayout || !this.mIsLandCamera) {
                    if (AlbumLabelMaker.this.mLabelWidth > 1) {
                        slotWidthAtFancyMode = AlbumLabelMaker.this.mLabelWidth;
                    } else {
                        slotWidthAtFancyMode = FancyHelper.getSlotWidthAtFancyMode();
                    }
                    bitmapCreateBitmap = GalleryBitmapPool.getInstance().get(AlbumLabelMaker.this.mBitmapWidth, AlbumLabelMaker.this.mBitmapHeight);
                    fullScreenLabelWidth = slotWidthAtFancyMode;
                } else {
                    fullScreenLabelWidth = FancyHelper.getFullScreenLabelWidth(AlbumLabelMaker.this.mLabelWidth);
                    bitmapCreateBitmap = null;
                }
            }
            if (bitmapCreateBitmap == null) {
                bitmapCreateBitmap = Bitmap.createBitmap(fullScreenLabelWidth + 0, labelSpec.labelBackgroundHeight + 0, Bitmap.Config.ARGB_8888);
            }
            Bitmap bitmap = bitmapCreateBitmap;
            Canvas canvas = new Canvas(bitmap);
            canvas.clipRect(0, 0, bitmap.getWidth() - 0, bitmap.getHeight() - 0);
            canvas.drawColor(AlbumLabelMaker.this.mSpec.backgroundColor, PorterDuff.Mode.SRC);
            canvas.translate(0.0f, 0.0f);
            if (jobContext.isCancelled()) {
                TraceHelper.endSection();
                return null;
            }
            int i = labelSpec.iconSize + labelSpec.leftMargin;
            AlbumLabelMaker.drawText(canvas, i, (labelSpec.labelBackgroundHeight - labelSpec.titleFontSize) / 2, str, ((fullScreenLabelWidth - labelSpec.leftMargin) - i) - labelSpec.titleRightMargin, AlbumLabelMaker.this.mTitlePaint);
            if (jobContext.isCancelled()) {
                TraceHelper.endSection();
                return null;
            }
            int i2 = fullScreenLabelWidth - labelSpec.titleRightMargin;
            int i3 = (labelSpec.labelBackgroundHeight - labelSpec.countFontSize) / 2;
            if (FancyHelper.isFancyLayoutSupported()) {
                f = 0.0f;
                AlbumLabelMaker.drawText(canvas, i2, i3, str2, fullScreenLabelWidth - i2, AlbumLabelMaker.this.mTitlePaint);
            } else {
                f = 0.0f;
                AlbumLabelMaker.drawText(canvas, i2, i3, str2, fullScreenLabelWidth - i2, AlbumLabelMaker.this.mCountPaint);
            }
            if (overlayAlbumIcon != null) {
                if (jobContext.isCancelled()) {
                    TraceHelper.endSection();
                    return null;
                }
                float width = labelSpec.iconSize / overlayAlbumIcon.getWidth();
                canvas.translate(labelSpec.leftMargin, (labelSpec.labelBackgroundHeight - Math.round(overlayAlbumIcon.getHeight() * width)) / 2.0f);
                canvas.scale(width, width);
                canvas.drawBitmap(overlayAlbumIcon, f, f, (Paint) null);
            }
            TraceHelper.endSection();
            return bitmap;
        }
    }

    public void recycleLabel(Bitmap bitmap) {
        GalleryBitmapPool.getInstance().put(bitmap);
    }

    public ThreadPool.Job<Bitmap> requestLabel(String str, String str2, int i, boolean z, boolean z2) {
        return new AlbumLabelJob(str, str2, i, z, z2);
    }
}
