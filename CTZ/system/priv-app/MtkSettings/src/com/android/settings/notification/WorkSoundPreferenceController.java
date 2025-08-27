package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class WorkSoundPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private final AudioHelper mHelper;
    private int mManagedProfileId;
    private final BroadcastReceiver mManagedProfileReceiver;
    private final SoundSettings mParent;
    private final UserManager mUserManager;
    private final boolean mVoiceCapable;
    private Preference mWorkAlarmRingtonePreference;
    private Preference mWorkNotificationRingtonePreference;
    private Preference mWorkPhoneRingtonePreference;
    private PreferenceGroup mWorkPreferenceCategory;
    private TwoStatePreference mWorkUsePersonalSounds;

    public WorkSoundPreferenceController(Context context, SoundSettings soundSettings, Lifecycle lifecycle) {
        this(context, soundSettings, lifecycle, new AudioHelper(context));
    }

    @VisibleForTesting
    WorkSoundPreferenceController(Context context, SoundSettings soundSettings, Lifecycle lifecycle, AudioHelper audioHelper) {
        super(context);
        this.mManagedProfileReceiver = new BroadcastReceiver() { // from class: com.android.settings.notification.WorkSoundPreferenceController.1
            /* JADX WARN: Removed duplicated region for block: B:13:0x0033  */
            @Override // android.content.BroadcastReceiver
            /*
                Code decompiled incorrectly, please refer to instructions dump.
            */
            public void onReceive(Context context2, Intent intent) {
                char c;
                int identifier = ((UserHandle) intent.getExtra("android.intent.extra.USER")).getIdentifier();
                String action = intent.getAction();
                int iHashCode = action.hashCode();
                if (iHashCode != -385593787) {
                    c = (iHashCode == 1051477093 && action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) ? (char) 1 : (char) 65535;
                } else if (action.equals("android.intent.action.MANAGED_PROFILE_ADDED")) {
                    c = 0;
                }
                switch (c) {
                    case 0:
                        WorkSoundPreferenceController.this.onManagedProfileAdded(identifier);
                        break;
                    case 1:
                        WorkSoundPreferenceController.this.onManagedProfileRemoved(identifier);
                        break;
                }
            }
        };
        this.mUserManager = UserManager.get(context);
        this.mVoiceCapable = Utils.isVoiceCapable(this.mContext);
        this.mParent = soundSettings;
        this.mHelper = audioHelper;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mWorkPreferenceCategory = (PreferenceGroup) preferenceScreen.findPreference("sound_work_settings_section");
        if (this.mWorkPreferenceCategory != null) {
            this.mWorkPreferenceCategory.setVisible(isAvailable());
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiver(this.mManagedProfileReceiver, intentFilter);
        this.mManagedProfileId = this.mHelper.getManagedProfileId(this.mUserManager);
        updateWorkPreferences();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        this.mContext.unregisterReceiver(this.mManagedProfileReceiver);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "sound_work_settings_section";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mHelper.getManagedProfileId(this.mUserManager) != -10000 && shouldShowRingtoneSettings();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        int i;
        if (!"work_ringtone".equals(preference.getKey())) {
            if ("work_notification_ringtone".equals(preference.getKey())) {
                i = 2;
            } else {
                if (!"work_alarm_ringtone".equals(preference.getKey())) {
                    return true;
                }
                i = 4;
            }
        } else {
            i = 1;
        }
        preference.setSummary(updateRingtoneName(getManagedProfileContext(), i));
        return true;
    }

    private boolean shouldShowRingtoneSettings() {
        return !this.mHelper.isSingleVolume();
    }

    private CharSequence updateRingtoneName(Context context, int i) {
        if (context == null || !this.mHelper.isUserUnlocked(this.mUserManager, context.getUserId())) {
            return this.mContext.getString(R.string.managed_profile_not_available_label);
        }
        return Ringtone.getTitle(context, RingtoneManager.getActualDefaultRingtoneUri(context, i), false, true);
    }

    private Context getManagedProfileContext() {
        if (this.mManagedProfileId == -10000) {
            return null;
        }
        return this.mHelper.createPackageContextAsUser(this.mManagedProfileId);
    }

    private DefaultRingtonePreference initWorkPreference(PreferenceGroup preferenceGroup, String str) {
        DefaultRingtonePreference defaultRingtonePreference = (DefaultRingtonePreference) preferenceGroup.findPreference(str);
        defaultRingtonePreference.setOnPreferenceChangeListener(this);
        defaultRingtonePreference.setUserId(this.mManagedProfileId);
        return defaultRingtonePreference;
    }

    private void updateWorkPreferences() {
        if (this.mWorkPreferenceCategory == null) {
            return;
        }
        boolean zIsAvailable = isAvailable();
        this.mWorkPreferenceCategory.setVisible(zIsAvailable);
        if (!zIsAvailable) {
            return;
        }
        if (this.mWorkUsePersonalSounds == null) {
            this.mWorkUsePersonalSounds = (TwoStatePreference) this.mWorkPreferenceCategory.findPreference("work_use_personal_sounds");
            this.mWorkUsePersonalSounds.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.android.settings.notification.-$$Lambda$WorkSoundPreferenceController$XBbO1oM_StZ54wAnUJEnnExa5OU
                @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
                public final boolean onPreferenceChange(Preference preference, Object obj) {
                    return WorkSoundPreferenceController.lambda$updateWorkPreferences$0(this.f$0, preference, obj);
                }
            });
        }
        if (this.mWorkPhoneRingtonePreference == null) {
            this.mWorkPhoneRingtonePreference = initWorkPreference(this.mWorkPreferenceCategory, "work_ringtone");
        }
        if (this.mWorkNotificationRingtonePreference == null) {
            this.mWorkNotificationRingtonePreference = initWorkPreference(this.mWorkPreferenceCategory, "work_notification_ringtone");
        }
        if (this.mWorkAlarmRingtonePreference == null) {
            this.mWorkAlarmRingtonePreference = initWorkPreference(this.mWorkPreferenceCategory, "work_alarm_ringtone");
        }
        if (!this.mVoiceCapable) {
            this.mWorkPhoneRingtonePreference.setVisible(false);
            this.mWorkPhoneRingtonePreference = null;
        }
        if (Settings.Secure.getIntForUser(getManagedProfileContext().getContentResolver(), "sync_parent_sounds", 0, this.mManagedProfileId) == 1) {
            enableWorkSyncSettings();
        } else {
            disableWorkSyncSettings();
        }
    }

    public static /* synthetic */ boolean lambda$updateWorkPreferences$0(WorkSoundPreferenceController workSoundPreferenceController, Preference preference, Object obj) {
        if (((Boolean) obj).booleanValue()) {
            UnifyWorkDialogFragment.show(workSoundPreferenceController.mParent);
            return false;
        }
        workSoundPreferenceController.disableWorkSync();
        return true;
    }

    void enableWorkSync() {
        RingtoneManager.enableSyncFromParent(getManagedProfileContext());
        enableWorkSyncSettings();
    }

    private void enableWorkSyncSettings() {
        this.mWorkUsePersonalSounds.setChecked(true);
        if (this.mWorkPhoneRingtonePreference != null) {
            this.mWorkPhoneRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
        }
        this.mWorkNotificationRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
        this.mWorkAlarmRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
    }

    private void disableWorkSync() {
        RingtoneManager.disableSyncFromParent(getManagedProfileContext());
        disableWorkSyncSettings();
    }

    private void disableWorkSyncSettings() {
        if (this.mWorkPhoneRingtonePreference != null) {
            this.mWorkPhoneRingtonePreference.setEnabled(true);
        }
        this.mWorkNotificationRingtonePreference.setEnabled(true);
        this.mWorkAlarmRingtonePreference.setEnabled(true);
        updateWorkRingtoneSummaries();
    }

    private void updateWorkRingtoneSummaries() {
        Context managedProfileContext = getManagedProfileContext();
        if (this.mWorkPhoneRingtonePreference != null) {
            this.mWorkPhoneRingtonePreference.setSummary(updateRingtoneName(managedProfileContext, 1));
        }
        this.mWorkNotificationRingtonePreference.setSummary(updateRingtoneName(managedProfileContext, 2));
        this.mWorkAlarmRingtonePreference.setSummary(updateRingtoneName(managedProfileContext, 4));
    }

    public void onManagedProfileAdded(int i) {
        if (this.mManagedProfileId == -10000) {
            this.mManagedProfileId = i;
            updateWorkPreferences();
        }
    }

    public void onManagedProfileRemoved(int i) {
        if (this.mManagedProfileId == i) {
            this.mManagedProfileId = this.mHelper.getManagedProfileId(this.mUserManager);
            updateWorkPreferences();
        }
    }

    public static class UnifyWorkDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 553;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.work_sync_dialog_title).setMessage(R.string.work_sync_dialog_message).setPositiveButton(R.string.work_sync_dialog_yes, this).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null).create();
        }

        public static void show(SoundSettings soundSettings) {
            FragmentManager fragmentManager = soundSettings.getFragmentManager();
            if (fragmentManager.findFragmentByTag("UnifyWorkDialogFragment") == null) {
                UnifyWorkDialogFragment unifyWorkDialogFragment = new UnifyWorkDialogFragment();
                unifyWorkDialogFragment.setTargetFragment(soundSettings, 200);
                unifyWorkDialogFragment.show(fragmentManager, "UnifyWorkDialogFragment");
            }
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            SoundSettings soundSettings = (SoundSettings) getTargetFragment();
            if (soundSettings.isAdded()) {
                soundSettings.enableWorkSync();
            }
        }
    }
}
