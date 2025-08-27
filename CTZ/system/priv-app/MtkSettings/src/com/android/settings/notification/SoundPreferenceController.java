package com.android.settings.notification;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationSettingsBase;

/* loaded from: classes.dex */
public class SoundPreferenceController extends NotificationPreferenceController implements PreferenceManager.OnActivityResultListener, Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final SettingsPreferenceFragment mFragment;
    private final NotificationSettingsBase.ImportanceListener mListener;
    private NotificationSoundPreference mPreference;

    public SoundPreferenceController(Context context, SettingsPreferenceFragment settingsPreferenceFragment, NotificationSettingsBase.ImportanceListener importanceListener, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
        this.mFragment = settingsPreferenceFragment;
        this.mListener = importanceListener;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "ringtone";
    }

    @Override // com.android.settings.notification.NotificationPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return super.isAvailable() && this.mChannel != null && checkCanBeVisible(3) && !isDefaultChannel();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (NotificationSoundPreference) preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (this.mAppRow != null && this.mChannel != null) {
            NotificationSoundPreference notificationSoundPreference = (NotificationSoundPreference) preference;
            notificationSoundPreference.setEnabled(this.mAdmin == null && isChannelConfigurable());
            notificationSoundPreference.setRingtone(this.mChannel.getSound());
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mChannel != null) {
            this.mChannel.setSound((Uri) obj, this.mChannel.getAudioAttributes());
            saveChannel();
            return true;
        }
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if ("ringtone".equals(preference.getKey()) && this.mFragment != null) {
            NotificationSoundPreference notificationSoundPreference = (NotificationSoundPreference) preference;
            notificationSoundPreference.onPrepareRingtonePickerIntent(notificationSoundPreference.getIntent());
            this.mFragment.startActivityForResult(preference.getIntent(), 200);
            return true;
        }
        return false;
    }

    @Override // android.preference.PreferenceManager.OnActivityResultListener
    public boolean onActivityResult(int i, int i2, Intent intent) {
        if (200 == i) {
            if (this.mPreference != null) {
                this.mPreference.onActivityResult(i, i2, intent);
            }
            this.mListener.onImportanceChanged();
            return true;
        }
        return false;
    }

    protected static boolean hasValidSound(NotificationChannel notificationChannel) {
        return (notificationChannel == null || notificationChannel.getSound() == null || Uri.EMPTY.equals(notificationChannel.getSound())) ? false : true;
    }
}
