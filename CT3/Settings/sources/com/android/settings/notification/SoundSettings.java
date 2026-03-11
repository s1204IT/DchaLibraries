package com.android.settings.notification;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.preference.SeekBarVolumizer;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.notification.VolumeSeekBarPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IAudioProfileExt;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SoundSettings extends SettingsPreferenceFragment implements Indexable {
    private Preference mAlarmRingtonePreference;
    private AudioManager mAudioManager;
    private Context mContext;
    private IAudioProfileExt mExt;
    private Preference mNotificationRingtonePreference;
    private Preference mPhoneRingtonePreference;
    private PackageManager mPm;
    private RingtonePreference mRequestPreference;
    private VolumeSeekBarPreference mRingOrNotificationPreference;
    private ComponentName mSuppressor;
    private UserManager mUserManager;
    private TwoStatePreference mVibrateWhenRinging;
    private Vibrator mVibrator;
    private boolean mVoiceCapable;
    private static final String[] RESTRICTED_KEYS = {"media_volume", "alarm_volume", "ring_volume", "notification_volume", "zen_mode"};
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.sound_settings;
            return Arrays.asList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> rt = new ArrayList<>();
            if (Utils.isVoiceCapable(context)) {
                rt.add("notification_volume");
            } else {
                rt.add("ring_volume");
                rt.add("ringtone");
                rt.add("wifi_display");
                rt.add("vibrate_when_ringing");
            }
            PackageManager pm = context.getPackageManager();
            UserManager um = (UserManager) context.getSystemService("user");
            boolean isCellBroadcastAppLinkEnabled = context.getResources().getBoolean(android.R.^attr-private.interpolatorX);
            if (isCellBroadcastAppLinkEnabled) {
                try {
                    if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver") == 2) {
                        isCellBroadcastAppLinkEnabled = false;
                    }
                } catch (IllegalArgumentException e) {
                    isCellBroadcastAppLinkEnabled = false;
                }
            }
            if (!um.isAdminUser() || !isCellBroadcastAppLinkEnabled) {
                rt.add("cell_broadcast_settings");
            }
            return rt;
        }
    };
    private final VolumePreferenceCallback mVolumeCallback = new VolumePreferenceCallback(this, null);
    private final H mHandler = new H(this, 0 == true ? 1 : 0);
    private final SettingsObserver mSettingsObserver = new SettingsObserver();
    private final Receiver mReceiver = new Receiver(this, 0 == true ? 1 : 0);
    private final ArrayList<VolumeSeekBarPreference> mVolumePrefs = new ArrayList<>();
    private int mRingerMode = -1;
    private final Runnable mLookupRingtoneNames = new Runnable() {
        @Override
        public void run() {
            CharSequence summary;
            CharSequence summary2;
            CharSequence summary3;
            if (SoundSettings.this.mPhoneRingtonePreference != null && (summary3 = SoundSettings.updateRingtoneName(SoundSettings.this.mContext, 1)) != null) {
                SoundSettings.this.mHandler.obtainMessage(1, summary3).sendToTarget();
            }
            if (SoundSettings.this.mNotificationRingtonePreference != null && (summary2 = SoundSettings.updateRingtoneName(SoundSettings.this.mContext, 2)) != null) {
                SoundSettings.this.mHandler.obtainMessage(2, summary2).sendToTarget();
            }
            if (SoundSettings.this.mAlarmRingtonePreference == null || (summary = SoundSettings.updateRingtoneName(SoundSettings.this.mContext, 4)) == null) {
                return;
            }
            SoundSettings.this.mHandler.obtainMessage(6, summary).sendToTarget();
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 336;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        this.mExt = UtilsExt.getAudioProfilePlugin(this.mContext);
        this.mPm = getPackageManager();
        this.mUserManager = UserManager.get(getContext());
        this.mVoiceCapable = Utils.isVoiceCapable(this.mContext);
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mVibrator = (Vibrator) getActivity().getSystemService("vibrator");
        if (this.mVibrator != null && !this.mVibrator.hasVibrator()) {
            this.mVibrator = null;
        }
        addPreferencesFromResource(R.xml.sound_settings);
        this.mExt.addCustomizedPreference(getPreferenceScreen());
        initVolumePreference("media_volume", 3, android.R.drawable.combobox_disabled);
        initVolumePreference("alarm_volume", 4, android.R.drawable.code_lock_left);
        if (this.mVoiceCapable) {
            this.mRingOrNotificationPreference = initVolumePreference("ring_volume", 2, android.R.drawable.contact_header_bg);
            removePreference("notification_volume");
        } else {
            this.mRingOrNotificationPreference = initVolumePreference("notification_volume", 5, android.R.drawable.contact_header_bg);
            removePreference("ring_volume");
        }
        if (!this.mPm.hasSystemFeature("android.hardware.wifi.direct")) {
            getPreferenceScreen().removePreference(findPreference("wifi_display"));
        }
        boolean isCellBroadcastAppLinkEnabled = getResources().getBoolean(android.R.^attr-private.interpolatorX);
        if (isCellBroadcastAppLinkEnabled) {
            try {
                if (this.mPm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver") == 2) {
                    isCellBroadcastAppLinkEnabled = false;
                }
            } catch (IllegalArgumentException e) {
                isCellBroadcastAppLinkEnabled = false;
            }
        }
        if (!this.mUserManager.isAdminUser() || !isCellBroadcastAppLinkEnabled || RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_config_cell_broadcasts", UserHandle.myUserId())) {
            removePreference("cell_broadcast_settings");
        }
        initRingtones();
        initVibrateWhenRinging();
        updateRingerMode();
        updateEffectsSuppressor();
        if (savedInstanceState == null) {
            return;
        }
        String selectedPreference = savedInstanceState.getString("selected_preference", null);
        if (TextUtils.isEmpty(selectedPreference)) {
            return;
        }
        this.mRequestPreference = (RingtonePreference) findPreference(selectedPreference);
    }

    @Override
    public void onResume() {
        super.onResume();
        lookupRingtoneNames();
        this.mSettingsObserver.register(true);
        this.mReceiver.register(true);
        updateRingOrNotificationPreference();
        updateEffectsSuppressor();
        updateVibrateWhenRinging();
        for (VolumeSeekBarPreference volumePref : this.mVolumePrefs) {
            volumePref.onActivityResume();
        }
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_adjust_volume", UserHandle.myUserId());
        boolean hasBaseRestriction = RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_adjust_volume", UserHandle.myUserId());
        for (String key : RESTRICTED_KEYS) {
            Preference pref = findPreference(key);
            if (pref != null) {
                pref.setEnabled(!hasBaseRestriction);
            }
            if ((pref instanceof RestrictedPreference) && !hasBaseRestriction) {
                ((RestrictedPreference) pref).setDisabledByAdmin(admin);
            }
        }
        RestrictedPreference broadcastSettingsPref = (RestrictedPreference) findPreference("cell_broadcast_settings");
        if (broadcastSettingsPref != null) {
            broadcastSettingsPref.checkRestrictionAndSetDisabled("no_config_cell_broadcasts");
        }
        this.mExt.onAudioProfileSettingResumed(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        for (VolumeSeekBarPreference volumePref : this.mVolumePrefs) {
            volumePref.onActivityPause();
        }
        this.mVolumeCallback.stopSample();
        this.mSettingsObserver.register(false);
        this.mReceiver.register(false);
        this.mExt.onAudioProfileSettingPaused(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            this.mRequestPreference = (RingtonePreference) preference;
            this.mRequestPreference.onPrepareRingtonePickerIntent(this.mRequestPreference.getIntent());
            startActivityForResult(preference.getIntent(), 200);
            return true;
        }
        if (this.mExt.onPreferenceTreeClick(preference)) {
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (this.mRequestPreference == null) {
            return;
        }
        this.mRequestPreference.onActivityResult(requestCode, resultCode, data);
        this.mRequestPreference = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mRequestPreference == null) {
            return;
        }
        outState.putString("selected_preference", this.mRequestPreference.getKey());
    }

    private VolumeSeekBarPreference initVolumePreference(String key, int stream, int muteIcon) {
        VolumeSeekBarPreference volumePref = (VolumeSeekBarPreference) findPreference(key);
        volumePref.setCallback(this.mVolumeCallback);
        volumePref.setStream(stream);
        this.mVolumePrefs.add(volumePref);
        volumePref.setMuteIcon(muteIcon);
        return volumePref;
    }

    private void updateRingOrNotificationPreference() {
        int i;
        VolumeSeekBarPreference volumeSeekBarPreference = this.mRingOrNotificationPreference;
        if (this.mSuppressor != null) {
            i = android.R.drawable.contact_header_bg;
        } else if (this.mRingerMode == 1 || wasRingerModeVibrate()) {
            i = android.R.drawable.control_background_32dp_material;
        } else {
            i = android.R.drawable.config_scrollbarTrackVertical;
        }
        volumeSeekBarPreference.showIcon(i);
    }

    private boolean wasRingerModeVibrate() {
        return this.mVibrator != null && this.mRingerMode == 0 && this.mAudioManager.getLastAudibleStreamVolume(2) == 0;
    }

    public void updateRingerMode() {
        int ringerMode = this.mAudioManager.getRingerModeInternal();
        if (this.mRingerMode == ringerMode) {
            return;
        }
        this.mRingerMode = ringerMode;
        updateRingOrNotificationPreference();
    }

    public void updateEffectsSuppressor() {
        String string;
        ComponentName suppressor = NotificationManager.from(this.mContext).getEffectsSuppressor();
        if (Objects.equals(suppressor, this.mSuppressor)) {
            return;
        }
        this.mSuppressor = suppressor;
        if (this.mRingOrNotificationPreference != null) {
            if (suppressor != null) {
                string = this.mContext.getString(android.R.string.lockscreen_pattern_instructions, getSuppressorCaption(suppressor));
            } else {
                string = null;
            }
            this.mRingOrNotificationPreference.setSuppressionText(string);
        }
        updateRingOrNotificationPreference();
    }

    private String getSuppressorCaption(ComponentName suppressor) {
        CharSequence seq;
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ServiceInfo info = pm.getServiceInfo(suppressor, 0);
            if (info != null && (seq = info.loadLabel(pm)) != null) {
                String str = seq.toString().trim();
                if (str.length() > 0) {
                    return str;
                }
            }
        } catch (Throwable e) {
            Log.w("SoundSettings", "Error loading suppressor caption", e);
        }
        return suppressor.getPackageName();
    }

    private final class VolumePreferenceCallback implements VolumeSeekBarPreference.Callback {
        private SeekBarVolumizer mCurrent;

        VolumePreferenceCallback(SoundSettings this$0, VolumePreferenceCallback volumePreferenceCallback) {
            this();
        }

        private VolumePreferenceCallback() {
        }

        @Override
        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (this.mCurrent != null && this.mCurrent != sbv) {
                this.mCurrent.stopSample();
            }
            this.mCurrent = sbv;
            if (this.mCurrent == null) {
                return;
            }
            SoundSettings.this.mHandler.removeMessages(3);
            SoundSettings.this.mHandler.sendEmptyMessageDelayed(3, 2000L);
        }

        @Override
        public void onStreamValueChanged(int stream, int progress) {
        }

        public void stopSample() {
            if (this.mCurrent == null) {
                return;
            }
            this.mCurrent.stopSample();
        }
    }

    private void initRingtones() {
        this.mPhoneRingtonePreference = getPreferenceScreen().findPreference("ringtone");
        if (this.mPhoneRingtonePreference != null && !this.mVoiceCapable) {
            getPreferenceScreen().removePreference(this.mPhoneRingtonePreference);
            this.mPhoneRingtonePreference = null;
        }
        this.mNotificationRingtonePreference = getPreferenceScreen().findPreference("notification_ringtone");
        this.mAlarmRingtonePreference = getPreferenceScreen().findPreference("alarm_ringtone");
    }

    private void lookupRingtoneNames() {
        AsyncTask.execute(this.mLookupRingtoneNames);
    }

    public static CharSequence updateRingtoneName(Context context, int type) {
        if (context == null) {
            Log.e("SoundSettings", "Unable to update ringtone name, no context provided");
            return null;
        }
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        CharSequence summary = context.getString(android.R.string.ext_media_status_formatting);
        if (ringtoneUri == null) {
            return context.getString(android.R.string.ext_media_status_checking);
        }
        Cursor cursor = null;
        try {
            if ("media".equals(ringtoneUri.getAuthority())) {
                cursor = context.getContentResolver().query(ringtoneUri, new String[]{"title"}, null, null, null);
            } else if ("content".equals(ringtoneUri.getScheme())) {
                cursor = context.getContentResolver().query(ringtoneUri, new String[]{"_display_name"}, null, null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                summary = cursor.getString(0);
            }
            if (cursor == null) {
                return summary;
            }
            cursor.close();
            return summary;
        } catch (SQLiteException e) {
            if (cursor == null) {
                return summary;
            }
            cursor.close();
            return summary;
        } catch (IllegalArgumentException e2) {
            if (cursor == null) {
                return summary;
            }
            cursor.close();
            return summary;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private void initVibrateWhenRinging() {
        this.mVibrateWhenRinging = (TwoStatePreference) getPreferenceScreen().findPreference("vibrate_when_ringing");
        if (this.mVibrateWhenRinging == null) {
            Log.i("SoundSettings", "Preference not found: vibrate_when_ringing");
            return;
        }
        if (!this.mVoiceCapable) {
            getPreferenceScreen().removePreference(this.mVibrateWhenRinging);
            this.mVibrateWhenRinging = null;
        } else {
            this.mVibrateWhenRinging.setPersistent(false);
            updateVibrateWhenRinging();
            this.mVibrateWhenRinging.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean val = ((Boolean) newValue).booleanValue();
                    return Settings.System.putInt(SoundSettings.this.getContentResolver(), "vibrate_when_ringing", val ? 1 : 0);
                }
            });
        }
    }

    public void updateVibrateWhenRinging() {
        if (this.mVibrateWhenRinging == null) {
            return;
        }
        this.mVibrateWhenRinging.setChecked(Settings.System.getInt(getContentResolver(), "vibrate_when_ringing", 0) != 0);
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri VIBRATE_WHEN_RINGING_URI;

        public SettingsObserver() {
            super(SoundSettings.this.mHandler);
            this.VIBRATE_WHEN_RINGING_URI = Settings.System.getUriFor("vibrate_when_ringing");
        }

        public void register(boolean register) {
            ContentResolver cr = SoundSettings.this.getContentResolver();
            if (register) {
                cr.registerContentObserver(this.VIBRATE_WHEN_RINGING_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (!this.VIBRATE_WHEN_RINGING_URI.equals(uri)) {
                return;
            }
            SoundSettings.this.updateVibrateWhenRinging();
        }
    }

    private final class H extends Handler {
        H(SoundSettings this$0, H h) {
            this();
        }

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    SoundSettings.this.mPhoneRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    SoundSettings.this.mNotificationRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                    SoundSettings.this.mVolumeCallback.stopSample();
                    break;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    SoundSettings.this.updateEffectsSuppressor();
                    break;
                case 5:
                    SoundSettings.this.updateRingerMode();
                    break;
                case 6:
                    SoundSettings.this.mAlarmRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
            }
        }
    }

    private class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        Receiver(SoundSettings this$0, Receiver receiver) {
            this();
        }

        private Receiver() {
        }

        public void register(boolean register) {
            if (this.mRegistered == register) {
                return;
            }
            if (register) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
                filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
                SoundSettings.this.mContext.registerReceiver(this, filter);
            } else {
                SoundSettings.this.mContext.unregisterReceiver(this);
            }
            this.mRegistered = register;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED".equals(action)) {
                SoundSettings.this.mHandler.sendEmptyMessage(4);
            } else {
                if (!"android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION".equals(action)) {
                    return;
                }
                SoundSettings.this.mHandler.sendEmptyMessage(5);
            }
        }
    }

    private static class SummaryProvider extends BroadcastReceiver implements SummaryLoader.SummaryProvider {
        private final AudioManager mAudioManager;
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final int maxVolume;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
            this.maxVolume = this.mAudioManager.getStreamMaxVolume(2);
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.media.VOLUME_CHANGED_ACTION");
                filter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
                filter.addAction("android.media.RINGER_MODE_CHANGED");
                filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
                filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
                filter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
                this.mContext.registerReceiver(this, filter);
                return;
            }
            this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String percent = NumberFormat.getPercentInstance().format(((double) this.mAudioManager.getStreamVolume(2)) / ((double) this.maxVolume));
            this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.sound_settings_summary, percent));
        }
    }
}
