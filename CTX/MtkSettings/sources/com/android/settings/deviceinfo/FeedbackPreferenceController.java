package com.android.settings.deviceinfo;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;

public class FeedbackPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final Intent intent;
    private final Fragment mHost;

    public FeedbackPreferenceController(Fragment fragment, Context context) {
        super(context);
        this.mHost = fragment;
        this.intent = new Intent("android.intent.action.BUG_REPORT");
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(DeviceInfoUtils.getFeedbackReporterPackage(this.mContext));
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        this.intent.setPackage(DeviceInfoUtils.getFeedbackReporterPackage(this.mContext));
        preference.setIntent(this.intent);
        if (isAvailable() && !preference.isVisible()) {
            preference.setVisible(true);
        } else if (!isAvailable() && preference.isVisible()) {
            preference.setVisible(false);
        }
    }

    @Override
    public String getPreferenceKey() {
        return "device_feedback";
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), "device_feedback") || !isAvailable()) {
            return false;
        }
        this.mHost.startActivityForResult(this.intent, 0);
        return true;
    }
}
