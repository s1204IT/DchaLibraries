package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.ShadowKeyDrawable;

public class KeyButtonDrawable extends LayerDrawable {
    private final boolean mHasDarkDrawable;

    public static KeyButtonDrawable create(Context context, Drawable drawable, Drawable drawable2, boolean z) {
        if (drawable2 != null) {
            ShadowKeyDrawable shadowKeyDrawable = new ShadowKeyDrawable(drawable.mutate());
            ShadowKeyDrawable shadowKeyDrawable2 = new ShadowKeyDrawable(drawable2.mutate());
            if (z) {
                Resources resources = context.getResources();
                shadowKeyDrawable.setShadowProperties(resources.getDimensionPixelSize(R.dimen.nav_key_button_shadow_offset_x), resources.getDimensionPixelSize(R.dimen.nav_key_button_shadow_offset_y), resources.getDimensionPixelSize(R.dimen.nav_key_button_shadow_radius), context.getColor(R.color.nav_key_button_shadow_color));
            }
            return new KeyButtonDrawable(new Drawable[]{shadowKeyDrawable, shadowKeyDrawable2});
        }
        return new KeyButtonDrawable(new Drawable[]{new ShadowKeyDrawable(drawable.mutate())});
    }

    protected KeyButtonDrawable(Drawable[] drawableArr) {
        super(drawableArr);
        for (int i = 0; i < drawableArr.length; i++) {
            setLayerGravity(i, 17);
        }
        mutate();
        this.mHasDarkDrawable = drawableArr.length > 1;
        setDarkIntensity(0.0f);
    }

    public void setDarkIntensity(float f) {
        if (!this.mHasDarkDrawable) {
            return;
        }
        getDrawable(0).setAlpha((int) ((1.0f - f) * 255.0f));
        getDrawable(1).setAlpha((int) (f * 255.0f));
        invalidateSelf();
    }

    public void setRotation(float f) {
        if (getDrawable(0) instanceof ShadowKeyDrawable) {
            ((ShadowKeyDrawable) getDrawable(0)).setRotation(f);
        }
        if (this.mHasDarkDrawable && (getDrawable(1) instanceof ShadowKeyDrawable)) {
            ((ShadowKeyDrawable) getDrawable(1)).setRotation(f);
        }
    }
}
