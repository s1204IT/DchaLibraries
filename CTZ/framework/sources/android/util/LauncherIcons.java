package android.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.LayerDrawable;
import com.android.internal.R;

public final class LauncherIcons {
    private static final int AMBIENT_SHADOW_ALPHA = 30;
    private static final float ICON_SIZE_BLUR_FACTOR = 0.010416667f;
    private static final float ICON_SIZE_KEY_SHADOW_DELTA_FACTOR = 0.020833334f;
    private static final int KEY_SHADOW_ALPHA = 61;
    private final int mIconSize;
    private final Resources mRes;
    private final SparseArray<Bitmap> mShadowCache = new SparseArray<>();

    public LauncherIcons(Context context) {
        this.mRes = context.getResources();
        this.mIconSize = this.mRes.getDimensionPixelSize(17104896);
    }

    public Drawable wrapIconDrawableWithShadow(Drawable drawable) {
        if (!(drawable instanceof AdaptiveIconDrawable)) {
            return drawable;
        }
        return new ShadowDrawable(getShadowBitmap((AdaptiveIconDrawable) drawable), drawable);
    }

    private Bitmap getShadowBitmap(AdaptiveIconDrawable adaptiveIconDrawable) {
        int iMax = Math.max(this.mIconSize, adaptiveIconDrawable.getIntrinsicHeight());
        synchronized (this.mShadowCache) {
            Bitmap bitmap = this.mShadowCache.get(iMax);
            if (bitmap != null) {
                return bitmap;
            }
            adaptiveIconDrawable.setBounds(0, 0, iMax, iMax);
            float f = iMax;
            float f2 = ICON_SIZE_BLUR_FACTOR * f;
            float f3 = ICON_SIZE_KEY_SHADOW_DELTA_FACTOR * f;
            int i = (int) (f + (2.0f * f2) + f3);
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            canvas.translate((f3 / 2.0f) + f2, f2);
            Paint paint = new Paint(1);
            paint.setColor(0);
            paint.setShadowLayer(f2, 0.0f, 0.0f, 503316480);
            canvas.drawPath(adaptiveIconDrawable.getIconMask(), paint);
            canvas.translate(0.0f, f3);
            paint.setShadowLayer(f2, 0.0f, 0.0f, 1023410176);
            canvas.drawPath(adaptiveIconDrawable.getIconMask(), paint);
            canvas.setBitmap(null);
            synchronized (this.mShadowCache) {
                this.mShadowCache.put(iMax, bitmapCreateBitmap);
            }
            return bitmapCreateBitmap;
        }
    }

    public Drawable getBadgeDrawable(int i, int i2) {
        return getBadgedDrawable(null, i, i2);
    }

    public Drawable getBadgedDrawable(Drawable drawable, int i, int i2) {
        Drawable[] drawableArr;
        Resources system = Resources.getSystem();
        Drawable drawable2 = system.getDrawable(R.drawable.ic_corp_icon_badge_shadow);
        Drawable drawableMutate = system.getDrawable(R.drawable.ic_corp_icon_badge_color).getConstantState().newDrawable().mutate();
        Drawable drawable3 = system.getDrawable(i);
        drawable3.setTint(i2);
        if (drawable == null) {
            drawableArr = new Drawable[]{drawable2, drawableMutate, drawable3};
        } else {
            drawableArr = new Drawable[]{drawable, drawable2, drawableMutate, drawable3};
        }
        return new LayerDrawable(drawableArr);
    }

    private static class ShadowDrawable extends DrawableWrapper {
        final MyConstantState mState;

        public ShadowDrawable(Bitmap bitmap, Drawable drawable) {
            super(drawable);
            this.mState = new MyConstantState(bitmap, drawable.getConstantState());
        }

        ShadowDrawable(MyConstantState myConstantState) {
            super(myConstantState.mChildState.newDrawable());
            this.mState = myConstantState;
        }

        @Override
        public Drawable.ConstantState getConstantState() {
            return this.mState;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(this.mState.mShadow, (Rect) null, getBounds(), this.mState.mPaint);
            canvas.save();
            canvas.translate(r0.width() * 0.9599999f * LauncherIcons.ICON_SIZE_KEY_SHADOW_DELTA_FACTOR, r0.height() * 0.9599999f * LauncherIcons.ICON_SIZE_BLUR_FACTOR);
            canvas.scale(0.9599999f, 0.9599999f);
            super.draw(canvas);
            canvas.restore();
        }

        private static class MyConstantState extends Drawable.ConstantState {
            final Drawable.ConstantState mChildState;
            final Paint mPaint = new Paint(2);
            final Bitmap mShadow;

            MyConstantState(Bitmap bitmap, Drawable.ConstantState constantState) {
                this.mShadow = bitmap;
                this.mChildState = constantState;
            }

            @Override
            public Drawable newDrawable() {
                return new ShadowDrawable(this);
            }

            @Override
            public int getChangingConfigurations() {
                return this.mChildState.getChangingConfigurations();
            }
        }
    }
}
