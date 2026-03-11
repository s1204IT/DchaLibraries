package com.android.systemui.tuner;

import android.R;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService;

public class BatteryPreference extends DropDownPreference implements TunerService.Tunable {
    private final String mBattery;
    private boolean mBatteryEnabled;
    private ArraySet<String> mBlacklist;
    private boolean mHasPercentage;
    private boolean mHasSetValue;

    public BatteryPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBattery = context.getString(R.string.mediasize_iso_b6);
        setEntryValues(new CharSequence[]{"percent", "default", "disabled"});
    }

    @Override
    public void onAttached() {
        super.onAttached();
        this.mHasPercentage = Settings.System.getInt(getContext().getContentResolver(), "status_bar_show_battery_percent", 0) != 0;
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
    }

    @Override
    public void onDetached() {
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        super.onDetached();
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        if ("icon_blacklist".equals(str)) {
            this.mBlacklist = StatusBarIconController.getIconBlacklist(str2);
            this.mBatteryEnabled = !this.mBlacklist.contains(this.mBattery);
        }
        if (!this.mHasSetValue) {
            this.mHasSetValue = true;
            if (this.mBatteryEnabled && this.mHasPercentage) {
                setValue("percent");
            } else if (this.mBatteryEnabled) {
                setValue("default");
            } else {
                setValue("disabled");
            }
        }
    }

    @Override
    protected boolean persistString(String str) {
        boolean zEquals = "percent".equals(str);
        MetricsLogger.action(getContext(), 237, zEquals);
        Settings.System.putInt(getContext().getContentResolver(), "status_bar_show_battery_percent", zEquals ? 1 : 0);
        if ("disabled".equals(str)) {
            this.mBlacklist.add(this.mBattery);
        } else {
            this.mBlacklist.remove(this.mBattery);
        }
        ((TunerService) Dependency.get(TunerService.class)).setValue("icon_blacklist", TextUtils.join(",", this.mBlacklist));
        return true;
    }
}
