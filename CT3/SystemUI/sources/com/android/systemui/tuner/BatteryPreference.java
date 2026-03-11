package com.android.systemui.tuner;

import android.R;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService;

public class BatteryPreference extends DropDownPreference implements TunerService.Tunable {
    private final String mBattery;
    private boolean mBatteryEnabled;
    private ArraySet<String> mBlacklist;
    private boolean mHasPercentage;
    private boolean mHasSetValue;

    public BatteryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBattery = context.getString(R.string.config_systemWifiCoexManager);
        setEntryValues(new CharSequence[]{"percent", "default", "disabled"});
    }

    @Override
    public void onAttached() {
        super.onAttached();
        TunerService.get(getContext()).addTunable(this, "icon_blacklist");
        this.mHasPercentage = Settings.System.getInt(getContext().getContentResolver(), "status_bar_show_battery_percent", 0) != 0;
    }

    @Override
    public void onDetached() {
        TunerService.get(getContext()).removeTunable(this);
        super.onDetached();
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if ("icon_blacklist".equals(key)) {
            this.mBlacklist = StatusBarIconController.getIconBlacklist(newValue);
            this.mBatteryEnabled = !this.mBlacklist.contains(this.mBattery);
        }
        if (this.mHasSetValue) {
            return;
        }
        this.mHasSetValue = true;
        if (this.mBatteryEnabled && this.mHasPercentage) {
            setValue("percent");
        } else if (this.mBatteryEnabled) {
            setValue("default");
        } else {
            setValue("disabled");
        }
    }

    @Override
    protected boolean persistString(String value) {
        boolean v = "percent".equals(value);
        MetricsLogger.action(getContext(), 237, v);
        Settings.System.putInt(getContext().getContentResolver(), "status_bar_show_battery_percent", v ? 1 : 0);
        if ("disabled".equals(value)) {
            this.mBlacklist.add(this.mBattery);
        } else {
            this.mBlacklist.remove(this.mBattery);
        }
        TunerService.get(getContext()).setValue("icon_blacklist", TextUtils.join(",", this.mBlacklist));
        return true;
    }
}
