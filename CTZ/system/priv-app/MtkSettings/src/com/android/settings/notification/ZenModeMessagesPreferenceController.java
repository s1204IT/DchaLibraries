package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeMessagesPreferenceController extends AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {
    private final ZenModeBackend mBackend;
    private final String[] mListValues;
    private ListPreference mPreference;

    public ZenModeMessagesPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, "zen_mode_messages", lifecycle);
        this.mBackend = ZenModeBackend.getInstance(context);
        this.mListValues = context.getResources().getStringArray(R.array.zen_mode_contacts_values);
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "zen_mode_messages";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (ListPreference) preferenceScreen.findPreference("zen_mode_messages");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateFromContactsValue(preference);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mBackend.saveSenders(4, ZenModeBackend.getSettingFromPrefKey(obj.toString()));
        updateFromContactsValue(preference);
        return true;
    }

    private void updateFromContactsValue(Preference preference) {
        this.mPreference = (ListPreference) preference;
        switch (getZenMode()) {
            case 2:
            case 3:
                this.mPreference.setEnabled(false);
                this.mPreference.setValue("zen_mode_from_none");
                this.mPreference.setSummary(this.mBackend.getContactsSummary(-1));
                break;
            default:
                preference.setEnabled(true);
                preference.setSummary(this.mBackend.getContactsSummary(4));
                this.mPreference.setValue(this.mListValues[getIndexOfSendersValue(ZenModeBackend.getKeyFromSetting(this.mBackend.getPriorityMessageSenders()))]);
                break;
        }
    }

    @VisibleForTesting
    protected int getIndexOfSendersValue(String str) {
        for (int i = 0; i < this.mListValues.length; i++) {
            if (TextUtils.equals(str, this.mListValues[i])) {
                return i;
            }
        }
        return 3;
    }
}
