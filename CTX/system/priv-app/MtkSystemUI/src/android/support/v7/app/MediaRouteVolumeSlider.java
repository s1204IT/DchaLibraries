package android.support.v7.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.appcompat.R;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.Log;
/* loaded from: classes.dex */
class MediaRouteVolumeSlider extends AppCompatSeekBar {
    private int mColor;
    private final float mDisabledAlpha;
    private boolean mHideThumb;
    private Drawable mThumb;

    public MediaRouteVolumeSlider(Context context) {
        this(context, null);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarStyle);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mDisabledAlpha = MediaRouterThemeHelper.getDisabledAlpha(context);
    }

    @Override // android.support.v7.widget.AppCompatSeekBar, android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int alpha = isEnabled() ? 255 : (int) (255.0f * this.mDisabledAlpha);
        this.mThumb.setColorFilter(this.mColor, PorterDuff.Mode.SRC_IN);
        this.mThumb.setAlpha(alpha);
        getProgressDrawable().setColorFilter(this.mColor, PorterDuff.Mode.SRC_IN);
        getProgressDrawable().setAlpha(alpha);
    }

    @Override // android.widget.AbsSeekBar
    public void setThumb(Drawable thumb) {
        this.mThumb = thumb;
        super.setThumb(this.mHideThumb ? null : this.mThumb);
    }

    public void setHideThumb(boolean hideThumb) {
        if (this.mHideThumb == hideThumb) {
            return;
        }
        this.mHideThumb = hideThumb;
        super.setThumb(this.mHideThumb ? null : this.mThumb);
    }

    public void setColor(int color) {
        if (this.mColor == color) {
            return;
        }
        if (Color.alpha(color) != 255) {
            Log.e("MediaRouteVolumeSlider", "Volume slider color cannot be translucent: #" + Integer.toHexString(color));
        }
        this.mColor = color;
    }
}
