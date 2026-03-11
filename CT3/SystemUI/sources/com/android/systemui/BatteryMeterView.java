package com.android.systemui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.tuner.TunerService;

public class BatteryMeterView extends ImageView implements BatteryController.BatteryStateChangeCallback, TunerService.Tunable {
    private BatteryController mBatteryController;
    private final BatteryMeterDrawable mDrawable;
    private final String mSlotBattery;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView, defStyle, 0);
        int frameColor = atts.getColor(0, context.getColor(R.color.batterymeter_frame_color));
        this.mDrawable = new BatteryMeterDrawable(context, new Handler(), frameColor);
        atts.recycle();
        this.mSlotBattery = context.getString(android.R.string.config_systemWifiCoexManager);
        setImageDrawable(this.mDrawable);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!"icon_blacklist".equals(key)) {
            return;
        }
        ArraySet<String> icons = StatusBarIconController.getIconBlacklist(newValue);
        setVisibility(icons.contains(this.mSlotBattery) ? 8 : 0);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mBatteryController.addStateChangedCallback(this);
        this.mDrawable.startListening();
        TunerService.get(getContext()).addTunable(this, "icon_blacklist");
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mBatteryController.removeStateChangedCallback(this);
        this.mDrawable.stopListening();
        TunerService.get(getContext()).removeTunable(this);
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        setContentDescription(getContext().getString(charging ? R.string.accessibility_battery_level_charging : R.string.accessibility_battery_level, Integer.valueOf(level)));
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
    }

    public void setBatteryController(BatteryController mBatteryController) {
        this.mBatteryController = mBatteryController;
        this.mDrawable.setBatteryController(mBatteryController);
    }

    public void setDarkIntensity(float f) {
        this.mDrawable.setDarkIntensity(f);
    }
}
