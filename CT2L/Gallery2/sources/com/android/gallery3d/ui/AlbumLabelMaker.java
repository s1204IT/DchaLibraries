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

public class AlbumLabelMaker {
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

    public AlbumLabelMaker(Context context, AlbumSetSlotRenderer.LabelSpec spec) {
        this.mContext = context;
        this.mSpec = spec;
        this.mTitlePaint = getTextPaint(spec.titleFontSize, spec.titleColor, false);
        this.mCountPaint = getTextPaint(spec.countFontSize, spec.countColor, false);
    }

    public static int getBorderSize() {
        return 0;
    }

    private Bitmap getOverlayAlbumIcon(int sourceType) {
        switch (sourceType) {
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

    private static TextPaint getTextPaint(int textSize, int color, boolean isBold) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(1));
        }
        return paint;
    }

    private class LazyLoadedBitmap {
        private Bitmap mBitmap;
        private int mResId;

        public LazyLoadedBitmap(int resId) {
            this.mResId = resId;
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

    public synchronized void setLabelWidth(int width) {
        if (this.mLabelWidth != width) {
            this.mLabelWidth = width;
            this.mBitmapWidth = width + 0;
            this.mBitmapHeight = this.mSpec.labelBackgroundHeight + 0;
        }
    }

    public ThreadPool.Job<Bitmap> requestLabel(String title, String count, int sourceType) {
        return new AlbumLabelJob(title, count, sourceType);
    }

    static void drawText(Canvas canvas, int x, int y, String text, int lengthLimit, TextPaint p) {
        synchronized (p) {
            canvas.drawText(TextUtils.ellipsize(text, p, lengthLimit, TextUtils.TruncateAt.END).toString(), x, y - p.getFontMetricsInt().ascent, p);
        }
    }

    private class AlbumLabelJob implements ThreadPool.Job<Bitmap> {
        private final String mCount;
        private final int mSourceType;
        private final String mTitle;

        public AlbumLabelJob(String title, String count, int sourceType) {
            this.mTitle = title;
            this.mCount = count;
            this.mSourceType = sourceType;
        }

        @Override
        public Bitmap run(ThreadPool.JobContext jc) {
            int labelWidth;
            Bitmap bitmap;
            AlbumSetSlotRenderer.LabelSpec s = AlbumLabelMaker.this.mSpec;
            String title = this.mTitle;
            String count = this.mCount;
            Bitmap icon = AlbumLabelMaker.this.getOverlayAlbumIcon(this.mSourceType);
            synchronized (this) {
                labelWidth = AlbumLabelMaker.this.mLabelWidth;
                bitmap = GalleryBitmapPool.getInstance().get(AlbumLabelMaker.this.mBitmapWidth, AlbumLabelMaker.this.mBitmapHeight);
            }
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(labelWidth + 0, s.labelBackgroundHeight + 0, Bitmap.Config.ARGB_8888);
            }
            Canvas canvas = new Canvas(bitmap);
            canvas.clipRect(0, 0, bitmap.getWidth() + 0, bitmap.getHeight() + 0);
            canvas.drawColor(AlbumLabelMaker.this.mSpec.backgroundColor, PorterDuff.Mode.SRC);
            canvas.translate(0.0f, 0.0f);
            if (jc.isCancelled()) {
                return null;
            }
            int x = s.leftMargin + s.iconSize;
            int y = (s.labelBackgroundHeight - s.titleFontSize) / 2;
            AlbumLabelMaker.drawText(canvas, x, y, title, ((labelWidth - s.leftMargin) - x) - s.titleRightMargin, AlbumLabelMaker.this.mTitlePaint);
            if (jc.isCancelled()) {
                return null;
            }
            int x2 = labelWidth - s.titleRightMargin;
            int y2 = (s.labelBackgroundHeight - s.countFontSize) / 2;
            AlbumLabelMaker.drawText(canvas, x2, y2, count, labelWidth - x2, AlbumLabelMaker.this.mCountPaint);
            if (icon != null) {
                if (jc.isCancelled()) {
                    return null;
                }
                float scale = s.iconSize / icon.getWidth();
                canvas.translate(s.leftMargin, (s.labelBackgroundHeight - Math.round(icon.getHeight() * scale)) / 2.0f);
                canvas.scale(scale, scale);
                canvas.drawBitmap(icon, 0.0f, 0.0f, (Paint) null);
                return bitmap;
            }
            return bitmap;
        }
    }
}
