package com.android.settings.fuelgauge;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.graph.BatteryMeterDrawableBase;

/* loaded from: classes.dex */
public class BatteryMeterView extends ImageView {
    ColorFilter mAccentColorFilter;
    BatteryMeterDrawable mDrawable;
    ColorFilter mErrorColorFilter;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        int color = context.getColor(R.color.meter_background_color);
        this.mAccentColorFilter = new PorterDuffColorFilter(Utils.getColorAttr(context, android.R.attr.colorAccent), PorterDuff.Mode.SRC_IN);
        this.mErrorColorFilter = new PorterDuffColorFilter(context.getColor(R.color.battery_icon_color_error), PorterDuff.Mode.SRC_IN);
        this.mDrawable = new BatteryMeterDrawable(context, color);
        this.mDrawable.setShowPercent(false);
        this.mDrawable.setBatteryColorFilter(this.mAccentColorFilter);
        this.mDrawable.setWarningColorFilter(new PorterDuffColorFilter(-1, PorterDuff.Mode.SRC_IN));
        setImageDrawable(this.mDrawable);
    }

    public void setBatteryLevel(int i) {
        this.mDrawable.setBatteryLevel(i);
        if (i < this.mDrawable.getCriticalLevel()) {
            this.mDrawable.setBatteryColorFilter(this.mErrorColorFilter);
        } else {
            this.mDrawable.setBatteryColorFilter(this.mAccentColorFilter);
        }
    }

    public int getBatteryLevel() {
        return this.mDrawable.getBatteryLevel();
    }

    public void setCharging(boolean z) {
        this.mDrawable.setCharging(z);
        postInvalidate();
    }

    public boolean getCharging() {
        return this.mDrawable.getCharging();
    }

    public static class BatteryMeterDrawable extends BatteryMeterDrawableBase {
        private final int mIntrinsicHeight;
        private final int mIntrinsicWidth;

        public BatteryMeterDrawable(Context context, int i) {
            super(context, i);
            this.mIntrinsicWidth = context.getResources().getDimensionPixelSize(R.dimen.battery_meter_width);
            this.mIntrinsicHeight = context.getResources().getDimensionPixelSize(R.dimen.battery_meter_height);
        }

        @Override // com.android.settingslib.graph.BatteryMeterDrawableBase, android.graphics.drawable.Drawable
        public int getIntrinsicWidth() {
            return this.mIntrinsicWidth;
        }

        @Override // com.android.settingslib.graph.BatteryMeterDrawableBase, android.graphics.drawable.Drawable
        public int getIntrinsicHeight() {
            return this.mIntrinsicHeight;
        }

        public void setWarningColorFilter(ColorFilter colorFilter) {
            this.mWarningTextPaint.setColorFilter(colorFilter);
        }

        public void setBatteryColorFilter(ColorFilter colorFilter) {
            this.mFramePaint.setColorFilter(colorFilter);
            this.mBatteryPaint.setColorFilter(colorFilter);
            this.mBoltPaint.setColorFilter(colorFilter);
        }
    }
}
