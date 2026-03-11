package com.android.settings.applications.appinfo;

import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;

public abstract class DefaultAppShortcutPreferenceControllerBase extends BasePreferenceController {
    protected final String mPackageName;

    protected abstract boolean hasAppCapability();

    protected abstract boolean isDefaultApp();

    public DefaultAppShortcutPreferenceControllerBase(Context context, String str, String str2) {
        super(context, str);
        this.mPackageName = str2;
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserManager.get(this.mContext).isManagedProfile()) {
            return 3;
        }
        return hasAppCapability() ? 0 : 2;
    }

    @Override
    public CharSequence getSummary() {
        return this.mContext.getText(isDefaultApp() ? R.string.yes : R.string.no);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(this.mPreferenceKey, preference.getKey())) {
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putString(":settings:fragment_args_key", this.mPreferenceKey);
        new SubSettingLauncher(this.mContext).setDestination(DefaultAppSettings.class.getName()).setArguments(bundle).setTitle(R.string.configure_apps).setSourceMetricsCategory(0).launch();
        return true;
    }
}
