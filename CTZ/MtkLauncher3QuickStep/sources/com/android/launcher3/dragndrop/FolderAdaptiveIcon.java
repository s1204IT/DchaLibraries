package com.android.launcher3.dragndrop;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.MainThreadExecutor;
import com.android.launcher3.R;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.PreviewBackground;
import com.android.launcher3.graphics.BitmapRenderer;
import com.android.launcher3.util.Preconditions;
import java.util.concurrent.Callable;

@TargetApi(26)
public class FolderAdaptiveIcon extends AdaptiveIconDrawable {
    private static final String TAG = "FolderAdaptiveIcon";
    private final Drawable mBadge;
    private final Path mMask;

    private FolderAdaptiveIcon(Drawable drawable, Drawable drawable2, Drawable drawable3, Path path) {
        super(drawable, drawable2);
        this.mBadge = drawable3;
        this.mMask = path;
    }

    @Override
    public Path getIconMask() {
        return this.mMask;
    }

    public Drawable getBadge() {
        return this.mBadge;
    }

    public static FolderAdaptiveIcon createFolderAdaptiveIcon(final Launcher launcher, final long j, final Point point) {
        Preconditions.assertNonUiThread();
        int dimensionPixelSize = launcher.getResources().getDimensionPixelSize(R.dimen.blur_size_medium_outline);
        final Bitmap bitmapCreateBitmap = Bitmap.createBitmap(point.x - dimensionPixelSize, point.y - dimensionPixelSize, Bitmap.Config.ARGB_8888);
        try {
            return (FolderAdaptiveIcon) new MainThreadExecutor().submit(new Callable() {
                @Override
                public final Object call() {
                    return FolderAdaptiveIcon.lambda$createFolderAdaptiveIcon$0(launcher, j, bitmapCreateBitmap, point);
                }
            }).get();
        } catch (Exception e) {
            Log.e(TAG, "Unable to create folder icon", e);
            return null;
        }
    }

    static FolderAdaptiveIcon lambda$createFolderAdaptiveIcon$0(Launcher launcher, long j, Bitmap bitmap, Point point) throws Exception {
        FolderIcon folderIconFindFolderIcon = launcher.findFolderIcon(j);
        if (folderIconFindFolderIcon == null) {
            return null;
        }
        return createDrawableOnUiThread(folderIconFindFolderIcon, bitmap, point);
    }

    private static FolderAdaptiveIcon createDrawableOnUiThread(final FolderIcon folderIcon, Bitmap bitmap, Point point) {
        Preconditions.assertUIThread();
        float dimension = folderIcon.getResources().getDimension(R.dimen.blur_size_medium_outline) / 2.0f;
        Canvas canvas = new Canvas();
        PreviewBackground folderBackground = folderIcon.getFolderBackground();
        canvas.setBitmap(bitmap);
        folderBackground.drawShadow(canvas);
        folderBackground.drawBackgroundStroke(canvas);
        folderIcon.drawBadge(canvas);
        float extraInsetFraction = 1.0f + (2.0f * AdaptiveIconDrawable.getExtraInsetFraction());
        int i = (int) (point.x * extraInsetFraction);
        int i2 = (int) (point.y * extraInsetFraction);
        float extraInsetFraction2 = AdaptiveIconDrawable.getExtraInsetFraction() / extraInsetFraction;
        final float f = i * extraInsetFraction2;
        final float f2 = extraInsetFraction2 * i2;
        Bitmap bitmapCreateHardwareBitmap = BitmapRenderer.createHardwareBitmap(i, i2, new BitmapRenderer.Renderer() {
            @Override
            public final void draw(Canvas canvas2) {
                FolderAdaptiveIcon.lambda$createDrawableOnUiThread$1(f, f2, folderIcon, canvas2);
            }
        });
        Path path = new Path();
        Matrix matrix = new Matrix();
        matrix.setTranslate(dimension, dimension);
        folderBackground.getClipPath().transform(matrix, path);
        return new FolderAdaptiveIcon(new ColorDrawable(folderBackground.getBgColor()), new ShiftedBitmapDrawable(bitmapCreateHardwareBitmap, dimension - f, dimension - f2), new ShiftedBitmapDrawable(bitmap, dimension, dimension), path);
    }

    static void lambda$createDrawableOnUiThread$1(float f, float f2, FolderIcon folderIcon, Canvas canvas) {
        int iSave = canvas.save();
        canvas.translate(f, f2);
        folderIcon.getPreviewItemManager().draw(canvas);
        canvas.restoreToCount(iSave);
    }

    private static class ShiftedBitmapDrawable extends Drawable {
        private final Bitmap mBitmap;
        private final Paint mPaint = new Paint(2);
        private final float mShiftX;
        private final float mShiftY;

        ShiftedBitmapDrawable(Bitmap bitmap, float f, float f2) {
            this.mBitmap = bitmap;
            this.mShiftX = f;
            this.mShiftY = f2;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(this.mBitmap, this.mShiftX, this.mShiftY, this.mPaint);
        }

        @Override
        public void setAlpha(int i) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            this.mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return -3;
        }
    }
}
