package com.android.systemui.qs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import com.android.systemui.qs.tileimpl.SlashImageView;

/* loaded from: classes.dex */
public class AlphaControlledSignalTileView extends SignalTileView {
    public AlphaControlledSignalTileView(Context context) {
        super(context);
    }

    @Override // com.android.systemui.qs.SignalTileView
    protected SlashImageView createSlashImageView(Context context) {
        return new AlphaControlledSlashImageView(context);
    }

    public static class AlphaControlledSlashImageView extends SlashImageView {
        public AlphaControlledSlashImageView(Context context) {
            super(context);
        }

        public void setFinalImageTintList(ColorStateList colorStateList) {
            super.setImageTintList(colorStateList);
            SlashDrawable slash = getSlash();
            if (slash != null) {
                ((AlphaControlledSlashDrawable) slash).setFinalTintList(colorStateList);
            }
        }

        @Override // com.android.systemui.qs.tileimpl.SlashImageView
        protected void ensureSlashDrawable() {
            if (getSlash() == null) {
                AlphaControlledSlashDrawable alphaControlledSlashDrawable = new AlphaControlledSlashDrawable(getDrawable());
                setSlash(alphaControlledSlashDrawable);
                alphaControlledSlashDrawable.setAnimationEnabled(getAnimationEnabled());
                setImageViewDrawable(alphaControlledSlashDrawable);
            }
        }
    }

    public static class AlphaControlledSlashDrawable extends SlashDrawable {
        AlphaControlledSlashDrawable(Drawable drawable) {
            super(drawable);
        }

        @Override // com.android.systemui.qs.SlashDrawable
        protected void setDrawableTintList(ColorStateList colorStateList) {
        }

        public void setFinalTintList(ColorStateList colorStateList) {
            super.setDrawableTintList(colorStateList);
        }
    }
}
