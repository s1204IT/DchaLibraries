package com.android.systemui;

import android.animation.ArgbEvaluator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settingslib.graph.BatteryMeterDrawableBase;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.IconLogger;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.Utils;
import java.text.NumberFormat;

public class BatteryMeterView extends LinearLayout implements BatteryController.BatteryStateChangeCallback, ConfigurationController.ConfigurationListener, DarkIconDispatcher.DarkReceiver, TunerService.Tunable {
    private BatteryController mBatteryController;
    private final ImageView mBatteryIconView;
    private TextView mBatteryPercentView;
    private float mDarkIntensity;
    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;
    private final BatteryMeterDrawableBase mDrawable;
    private boolean mForceShowPercent;
    private int mLevel;
    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;
    private int mNonAdaptedBackgroundColor;
    private int mNonAdaptedForegroundColor;
    private SettingObserver mSettingObserver;
    private final String mSlotBattery;
    private int mTextColor;
    private boolean mUseWallpaperTextColors;
    private int mUser;
    private final CurrentUserTracker mUserTracker;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setOrientation(0);
        setGravity(8388627);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.BatteryMeterView, i, 0);
        this.mDrawable = new BatteryMeterDrawableBase(context, typedArrayObtainStyledAttributes.getColor(0, context.getColor(R.color.meter_background_color)));
        typedArrayObtainStyledAttributes.recycle();
        this.mSettingObserver = new SettingObserver(new Handler(context.getMainLooper()));
        addOnAttachStateChangeListener(new Utils.DisableStateTracker(0, 2));
        this.mSlotBattery = context.getString(android.R.string.mediasize_iso_b6);
        this.mBatteryIconView = new ImageView(context);
        this.mBatteryIconView.setImageDrawable(this.mDrawable);
        ViewGroup.MarginLayoutParams marginLayoutParams = new ViewGroup.MarginLayoutParams(getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_width), getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_height));
        marginLayoutParams.setMargins(0, 0, 0, getResources().getDimensionPixelOffset(R.dimen.battery_margin_bottom));
        addView(this.mBatteryIconView, marginLayoutParams);
        updateShowPercent();
        setColorsFromContext(context);
        onDarkChanged(new Rect(), 0.0f, -1);
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            @Override
            public void onUserSwitched(int i2) {
                BatteryMeterView.this.mUser = i2;
                BatteryMeterView.this.getContext().getContentResolver().unregisterContentObserver(BatteryMeterView.this.mSettingObserver);
                BatteryMeterView.this.getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_show_battery_percent"), false, BatteryMeterView.this.mSettingObserver, i2);
            }
        };
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setForceShowPercent(boolean z) {
        this.mForceShowPercent = z;
        updateShowPercent();
    }

    public void useWallpaperTextColor(boolean z) {
        if (z == this.mUseWallpaperTextColors) {
            return;
        }
        this.mUseWallpaperTextColors = z;
        if (this.mUseWallpaperTextColors) {
            updateColors(com.android.settingslib.Utils.getColorAttr(this.mContext, R.attr.wallpaperTextColor), com.android.settingslib.Utils.getColorAttr(this.mContext, R.attr.wallpaperTextColorSecondary));
        } else {
            updateColors(this.mNonAdaptedForegroundColor, this.mNonAdaptedBackgroundColor);
        }
    }

    public void setColorsFromContext(Context context) {
        if (context == null) {
            return;
        }
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, com.android.settingslib.Utils.getThemeAttr(context, R.attr.darkIconTheme));
        ContextThemeWrapper contextThemeWrapper2 = new ContextThemeWrapper(context, com.android.settingslib.Utils.getThemeAttr(context, R.attr.lightIconTheme));
        this.mDarkModeBackgroundColor = com.android.settingslib.Utils.getColorAttr(contextThemeWrapper, R.attr.backgroundColor);
        this.mDarkModeFillColor = com.android.settingslib.Utils.getColorAttr(contextThemeWrapper, R.attr.fillColor);
        this.mLightModeBackgroundColor = com.android.settingslib.Utils.getColorAttr(contextThemeWrapper2, R.attr.backgroundColor);
        this.mLightModeFillColor = com.android.settingslib.Utils.getColorAttr(contextThemeWrapper2, R.attr.fillColor);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        if ("icon_blacklist".equals(str)) {
            boolean zContains = StatusBarIconController.getIconBlacklist(str2).contains(this.mSlotBattery);
            ((IconLogger) Dependency.get(IconLogger.class)).onIconVisibility(this.mSlotBattery, !zContains);
            setVisibility(zContains ? 8 : 0);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mBatteryController.addCallback(this);
        this.mUser = ActivityManager.getCurrentUser();
        getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_show_battery_percent"), false, this.mSettingObserver, this.mUser);
        updateShowPercent();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mUserTracker.startTracking();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mUserTracker.stopTracking();
        this.mBatteryController.removeCallback(this);
        getContext().getContentResolver().unregisterContentObserver(this.mSettingObserver);
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    @Override
    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        this.mDrawable.setBatteryLevel(i);
        this.mDrawable.setCharging(z && z2);
        this.mLevel = i;
        updatePercentText();
        setContentDescription(getContext().getString(z2 ? R.string.accessibility_battery_level_charging : R.string.accessibility_battery_level, Integer.valueOf(i)));
    }

    @Override
    public void onPowerSaveChanged(boolean z) {
        this.mDrawable.setPowerSave(z);
    }

    private TextView loadPercentView() {
        return (TextView) LayoutInflater.from(getContext()).inflate(R.layout.battery_percentage_view, (ViewGroup) null);
    }

    private void updatePercentText() {
        if (this.mBatteryPercentView != null) {
            this.mBatteryPercentView.setText(NumberFormat.getPercentInstance().format(this.mLevel / 100.0f));
        }
    }

    public void updateShowPercent() {
        boolean z = this.mBatteryPercentView != null;
        if (Settings.System.getIntForUser(getContext().getContentResolver(), "status_bar_show_battery_percent", 0, this.mUser) != 0 || this.mForceShowPercent) {
            if (!z) {
                this.mBatteryPercentView = loadPercentView();
                if (this.mTextColor != 0) {
                    this.mBatteryPercentView.setTextColor(this.mTextColor);
                }
                updatePercentText();
                addView(this.mBatteryPercentView, new ViewGroup.LayoutParams(-2, -1));
                return;
            }
            return;
        }
        if (z) {
            removeView(this.mBatteryPercentView);
            this.mBatteryPercentView = null;
        }
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        scaleBatteryMeterViews();
    }

    private void scaleBatteryMeterViews() {
        Resources resources = getContext().getResources();
        TypedValue typedValue = new TypedValue();
        resources.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float f = typedValue.getFloat();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
        int dimensionPixelSize3 = resources.getDimensionPixelSize(R.dimen.battery_margin_bottom);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (dimensionPixelSize2 * f), (int) (dimensionPixelSize * f));
        layoutParams.setMargins(0, 0, 0, dimensionPixelSize3);
        this.mBatteryIconView.setLayoutParams(layoutParams);
        FontSizeUtils.updateFontSize(this.mBatteryPercentView, R.dimen.qs_time_expanded_size);
    }

    @Override
    public void onDarkChanged(Rect rect, float f, int i) {
        this.mDarkIntensity = f;
        if (!DarkIconDispatcher.isInArea(rect, this)) {
            f = 0.0f;
        }
        this.mNonAdaptedForegroundColor = getColorForDarkIntensity(f, this.mLightModeFillColor, this.mDarkModeFillColor);
        this.mNonAdaptedBackgroundColor = getColorForDarkIntensity(f, this.mLightModeBackgroundColor, this.mDarkModeBackgroundColor);
        if (!this.mUseWallpaperTextColors) {
            updateColors(this.mNonAdaptedForegroundColor, this.mNonAdaptedBackgroundColor);
        }
    }

    private void updateColors(int i, int i2) {
        this.mDrawable.setColors(i, i2);
        this.mTextColor = i;
        if (this.mBatteryPercentView != null) {
            this.mBatteryPercentView.setTextColor(i);
        }
    }

    public void setFillColor(int i) {
        if (this.mLightModeFillColor == i) {
            return;
        }
        this.mLightModeFillColor = i;
        onDarkChanged(new Rect(), this.mDarkIntensity, -1);
    }

    private int getColorForDarkIntensity(float f, int i, int i2) {
        return ((Integer) ArgbEvaluator.getInstance().evaluate(f, Integer.valueOf(i), Integer.valueOf(i2))).intValue();
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            BatteryMeterView.this.updateShowPercent();
        }
    }
}
