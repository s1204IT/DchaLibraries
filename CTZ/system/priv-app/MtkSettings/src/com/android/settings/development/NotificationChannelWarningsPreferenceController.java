package com.android.settings.development;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class NotificationChannelWarningsPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int DEBUGGING_DISABLED = 0;
    static final int DEBUGGING_ENABLED = 1;
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;

    public NotificationChannelWarningsPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "show_notification_channel_warnings";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "show_notification_channel_warnings", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "show_notification_channel_warnings", isDebuggable() ? 1 : 0) != 0);
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(this.mContext.getContentResolver(), "show_notification_channel_warnings", 0);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }

    boolean isDebuggable() {
        return Build.IS_DEBUGGABLE;
    }
}
