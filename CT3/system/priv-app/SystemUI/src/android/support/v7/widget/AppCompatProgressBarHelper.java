package android.support.v7.widget;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.support.v4.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import android.widget.ProgressBar;
/* loaded from: a.zip:android/support/v7/widget/AppCompatProgressBarHelper.class */
class AppCompatProgressBarHelper {
    private static final int[] TINT_ATTRS = {16843067, 16843068};
    final AppCompatDrawableManager mDrawableManager;
    private Bitmap mSampleTile;
    private final ProgressBar mView;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppCompatProgressBarHelper(ProgressBar progressBar, AppCompatDrawableManager appCompatDrawableManager) {
        this.mView = progressBar;
        this.mDrawableManager = appCompatDrawableManager;
    }

    private Shape getDrawableShape() {
        return new RoundRectShape(new float[]{5.0f, 5.0f, 5.0f, 5.0f, 5.0f, 5.0f, 5.0f, 5.0f}, null, null);
    }

    private Drawable tileify(Drawable drawable, boolean z) {
        if (drawable instanceof DrawableWrapper) {
            Drawable wrappedDrawable = ((DrawableWrapper) drawable).getWrappedDrawable();
            if (wrappedDrawable != null) {
                ((DrawableWrapper) drawable).setWrappedDrawable(tileify(wrappedDrawable, z));
            }
        } else if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            int numberOfLayers = layerDrawable.getNumberOfLayers();
            Drawable[] drawableArr = new Drawable[numberOfLayers];
            for (int i = 0; i < numberOfLayers; i++) {
                int id = layerDrawable.getId(i);
                drawableArr[i] = tileify(layerDrawable.getDrawable(i), id == 16908301 || id == 16908303);
            }
            LayerDrawable layerDrawable2 = new LayerDrawable(drawableArr);
            for (int i2 = 0; i2 < numberOfLayers; i2++) {
                layerDrawable2.setId(i2, layerDrawable.getId(i2));
            }
            return layerDrawable2;
        } else if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (this.mSampleTile == null) {
                this.mSampleTile = bitmap;
            }
            ShapeDrawable shapeDrawable = new ShapeDrawable(getDrawableShape());
            shapeDrawable.getPaint().setShader(new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP));
            shapeDrawable.getPaint().setColorFilter(bitmapDrawable.getPaint().getColorFilter());
            Drawable drawable2 = shapeDrawable;
            if (z) {
                drawable2 = new ClipDrawable(shapeDrawable, 3, 1);
            }
            return drawable2;
        }
        return drawable;
    }

    private Drawable tileifyIndeterminate(Drawable drawable) {
        AnimationDrawable animationDrawable = drawable;
        if (drawable instanceof AnimationDrawable) {
            AnimationDrawable animationDrawable2 = (AnimationDrawable) drawable;
            int numberOfFrames = animationDrawable2.getNumberOfFrames();
            AnimationDrawable animationDrawable3 = new AnimationDrawable();
            animationDrawable3.setOneShot(animationDrawable2.isOneShot());
            for (int i = 0; i < numberOfFrames; i++) {
                Drawable tileify = tileify(animationDrawable2.getFrame(i), true);
                tileify.setLevel(10000);
                animationDrawable3.addFrame(tileify, animationDrawable2.getDuration(i));
            }
            animationDrawable3.setLevel(10000);
            animationDrawable = animationDrawable3;
        }
        return animationDrawable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bitmap getSampleTime() {
        return this.mSampleTile;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadFromAttributes(AttributeSet attributeSet, int i) {
        TintTypedArray obtainStyledAttributes = TintTypedArray.obtainStyledAttributes(this.mView.getContext(), attributeSet, TINT_ATTRS, i, 0);
        Drawable drawableIfKnown = obtainStyledAttributes.getDrawableIfKnown(0);
        if (drawableIfKnown != null) {
            this.mView.setIndeterminateDrawable(tileifyIndeterminate(drawableIfKnown));
        }
        Drawable drawableIfKnown2 = obtainStyledAttributes.getDrawableIfKnown(1);
        if (drawableIfKnown2 != null) {
            this.mView.setProgressDrawable(tileify(drawableIfKnown2, false));
        }
        obtainStyledAttributes.recycle();
    }
}
