package com.android.settings.dashboard;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import com.android.settings.R;

public class RoundedHomepageIcon extends LayerDrawable {
    int mBackgroundColor;

    public RoundedHomepageIcon(Context context, Drawable drawable) {
        super(new Drawable[]{context.getDrawable(R.drawable.ic_homepage_generic_background), drawable});
        this.mBackgroundColor = -1;
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.dashboard_tile_foreground_image_inset);
        setLayerInset(1, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize);
    }

    public void setBackgroundColor(int i) {
        this.mBackgroundColor = i;
        getDrawable(0).setColorFilter(i, PorterDuff.Mode.SRC_ATOP);
    }
}
