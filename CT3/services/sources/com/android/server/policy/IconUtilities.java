package com.android.server.policy;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.TableMaskFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public final class IconUtilities {
    private static final String TAG = "IconUtilities";
    private static final int[] sColors = {-65536, -16711936, -16776961};
    private final DisplayMetrics mDisplayMetrics;
    private int mIconHeight;
    private int mIconTextureHeight;
    private int mIconTextureWidth;
    private int mIconWidth;
    private final Paint mPaint = new Paint();
    private final Paint mBlurPaint = new Paint();
    private final Paint mGlowColorPressedPaint = new Paint();
    private final Paint mGlowColorFocusedPaint = new Paint();
    private final Rect mOldBounds = new Rect();
    private final Canvas mCanvas = new Canvas();
    private int mColorIndex = 0;

    public IconUtilities(Context context) {
        this.mIconWidth = -1;
        this.mIconHeight = -1;
        this.mIconTextureWidth = -1;
        this.mIconTextureHeight = -1;
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        this.mDisplayMetrics = metrics;
        float density = metrics.density;
        float blurPx = 5.0f * density;
        int dimension = (int) resources.getDimension(R.dimen.app_icon_size);
        this.mIconHeight = dimension;
        this.mIconWidth = dimension;
        int i = this.mIconWidth + ((int) (2.0f * blurPx));
        this.mIconTextureHeight = i;
        this.mIconTextureWidth = i;
        this.mBlurPaint.setMaskFilter(new BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL));
        TypedValue value = new TypedValue();
        this.mGlowColorPressedPaint.setColor(context.getTheme().resolveAttribute(R.attr.colorPressedHighlight, value, true) ? value.data : -15616);
        this.mGlowColorPressedPaint.setMaskFilter(TableMaskFilter.CreateClipTable(0, 30));
        this.mGlowColorFocusedPaint.setColor(context.getTheme().resolveAttribute(R.attr.colorFocusedHighlight, value, true) ? value.data : -29184);
        this.mGlowColorFocusedPaint.setMaskFilter(TableMaskFilter.CreateClipTable(0, 30));
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.2f);
        this.mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    public Drawable createIconDrawable(Drawable src) {
        Bitmap scaled = createIconBitmap(src);
        StateListDrawable result = new StateListDrawable();
        result.addState(new int[]{R.attr.state_focused}, new BitmapDrawable(createSelectedBitmap(scaled, false)));
        result.addState(new int[]{R.attr.state_pressed}, new BitmapDrawable(createSelectedBitmap(scaled, true)));
        result.addState(new int[0], new BitmapDrawable(scaled));
        result.setBounds(0, 0, this.mIconTextureWidth, this.mIconTextureHeight);
        return result;
    }

    public Bitmap createIconBitmap(Drawable icon) {
        int width = this.mIconWidth;
        int height = this.mIconHeight;
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (icon instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            if (bitmapDrawable.getBitmap().getDensity() == 0) {
                bitmapDrawable.setTargetDensity(this.mDisplayMetrics);
            }
        }
        int sourceWidth = icon.getIntrinsicWidth();
        int sourceHeight = icon.getIntrinsicHeight();
        if (sourceWidth > 0 && sourceHeight > 0) {
            if (width < sourceWidth || height < sourceHeight) {
                float ratio = sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (width / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (height * ratio);
                }
            } else if (sourceWidth < width && sourceHeight < height) {
                width = sourceWidth;
                height = sourceHeight;
            }
        }
        int textureWidth = this.mIconTextureWidth;
        int textureHeight = this.mIconTextureHeight;
        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = this.mCanvas;
        canvas.setBitmap(bitmap);
        int left = (textureWidth - width) / 2;
        int top = (textureHeight - height) / 2;
        this.mOldBounds.set(icon.getBounds());
        icon.setBounds(left, top, left + width, top + height);
        icon.draw(canvas);
        icon.setBounds(this.mOldBounds);
        return bitmap;
    }

    private Bitmap createSelectedBitmap(Bitmap src, boolean pressed) {
        Bitmap result = Bitmap.createBitmap(this.mIconTextureWidth, this.mIconTextureHeight, Bitmap.Config.ARGB_8888);
        Canvas dest = new Canvas(result);
        dest.drawColor(0, PorterDuff.Mode.CLEAR);
        int[] xy = new int[2];
        Bitmap mask = src.extractAlpha(this.mBlurPaint, xy);
        dest.drawBitmap(mask, xy[0], xy[1], pressed ? this.mGlowColorPressedPaint : this.mGlowColorFocusedPaint);
        mask.recycle();
        dest.drawBitmap(src, 0.0f, 0.0f, this.mPaint);
        dest.setBitmap(null);
        return result;
    }
}
