package com.android.settings.notification;

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
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.SeekBarVolumizer;
import android.preference.TwoStatePreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.notification.VolumeSeekBarPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class NotificationSettings extends SettingsPreferenceFragment implements Indexable {
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.notification_settings;
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
                rt.add("vibrate_when_ringing");
            }
            return rt;
        }
    };
    private AudioManager mAudioManager;
    private Context mContext;
    private final H mHandler;
    private DropDownPreference mLockscreen;
    private int mLockscreenSelectedValue;
    private Preference mNotificationAccess;
    private TwoStatePreference mNotificationPulse;
    private Preference mNotificationRingtonePreference;
    private PackageManager mPM;
    private Preference mPhoneRingtonePreference;
    private final Receiver mReceiver;
    private VolumeSeekBarPreference mRingOrNotificationPreference;
    private boolean mSecure;
    private ComponentName mSuppressor;
    private TwoStatePreference mVibrateWhenRinging;
    private Vibrator mVibrator;
    private boolean mVoiceCapable;
    private final VolumePreferenceCallback mVolumeCallback;
    private final SettingsObserver mSettingsObserver = new SettingsObserver();
    private final ArrayList<VolumeSeekBarPreference> mVolumePrefs = new ArrayList<>();
    private int mRingerMode = -1;
    private final Runnable mLookupRingtoneNames = new Runnable() {
        @Override
        public void run() {
            CharSequence summary;
            CharSequence summary2;
            if (NotificationSettings.this.mPhoneRingtonePreference != null && (summary2 = NotificationSettings.updateRingtoneName(NotificationSettings.this.mContext, 1)) != null) {
                NotificationSettings.this.mHandler.obtainMessage(1, summary2).sendToTarget();
            }
            if (NotificationSettings.this.mNotificationRingtonePreference != null && (summary = NotificationSettings.updateRingtoneName(NotificationSettings.this.mContext, 2)) != null) {
                NotificationSettings.this.mHandler.obtainMessage(2, summary).sendToTarget();
            }
        }
    };

    public NotificationSettings() {
        this.mVolumeCallback = new VolumePreferenceCallback();
        this.mHandler = new H();
        this.mReceiver = new Receiver();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        this.mPM = this.mContext.getPackageManager();
        this.mVoiceCapable = Utils.isVoiceCapable(this.mContext);
        this.mSecure = new LockPatternUtils(getActivity()).isSecure();
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mVibrator = (Vibrator) getActivity().getSystemService("vibrator");
        if (this.mVibrator != null && !this.mVibrator.hasVibrator()) {
            this.mVibrator = null;
        }
        addPreferencesFromResource(R.xml.notification_settings);
        PreferenceCategory sound = (PreferenceCategory) findPreference("sound");
        initVolumePreference("media_volume", 3, android.R.drawable.dropdown_focused_holo_dark);
        initVolumePreference("alarm_volume", 4, android.R.drawable.divider_horizontal_bright_opaque);
        if (this.mVoiceCapable) {
            this.mRingOrNotificationPreference = initVolumePreference("ring_volume", 2, android.R.drawable.dropdown_disabled_focused_holo_light);
            sound.removePreference(sound.findPreference("notification_volume"));
        } else {
            this.mRingOrNotificationPreference = initVolumePreference("notification_volume", 5, android.R.drawable.dropdown_disabled_focused_holo_light);
            sound.removePreference(sound.findPreference("ring_volume"));
        }
        initRingtones(sound);
        initVibrateWhenRinging(sound);
        PreferenceCategory notification = (PreferenceCategory) findPreference("notification");
        initPulse(notification);
        initLockscreenNotifications(notification);
        this.mNotificationAccess = findPreference("manage_notification_access");
        refreshNotificationListeners();
        updateRingerMode();
        updateEffectsSuppressor();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshNotificationListeners();
        lookupRingtoneNames();
        this.mSettingsObserver.register(true);
        this.mReceiver.register(true);
        updateRingOrNotificationPreference();
        updateEffectsSuppressor();
        for (VolumeSeekBarPreference volumePref : this.mVolumePrefs) {
            volumePref.onActivityResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mVolumeCallback.stopSample();
        this.mSettingsObserver.register(false);
        this.mReceiver.register(false);
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
            i = android.R.drawable.dropdown_disabled_focused_holo_light;
        } else {
            i = this.mRingerMode == 1 ? android.R.drawable.dropdown_disabled_holo_dark : android.R.drawable.dropdown_disabled_focused_holo_dark;
        }
        volumeSeekBarPreference.showIcon(i);
    }

    private void updateRingerMode() {
        int ringerMode = this.mAudioManager.getRingerModeInternal();
        if (this.mRingerMode != ringerMode) {
            this.mRingerMode = ringerMode;
            updateRingOrNotificationPreference();
        }
    }

    private void updateEffectsSuppressor() {
        ComponentName suppressor = NotificationManager.from(this.mContext).getEffectsSuppressor();
        if (!Objects.equals(suppressor, this.mSuppressor)) {
            this.mSuppressor = suppressor;
            if (this.mRingOrNotificationPreference != null) {
                String text = suppressor != null ? this.mContext.getString(android.R.string.network_switch_type_name_unknown, getSuppressorCaption(suppressor)) : null;
                this.mRingOrNotificationPreference.setSuppressionText(text);
            }
            updateRingOrNotificationPreference();
        }
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
            Log.w("NotificationSettings", "Error loading suppressor caption", e);
        }
        return suppressor.getPackageName();
    }

    private final class VolumePreferenceCallback implements VolumeSeekBarPreference.Callback {
        private SeekBarVolumizer mCurrent;

        private VolumePreferenceCallback() {
        }

        @Override
        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (this.mCurrent != null && this.mCurrent != sbv) {
                this.mCurrent.stopSample();
            }
            this.mCurrent = sbv;
            if (this.mCurrent != null) {
                NotificationSettings.this.mHandler.removeMessages(3);
                NotificationSettings.this.mHandler.sendEmptyMessageDelayed(3, 2000L);
            }
        }

        @Override
        public void onStreamValueChanged(int stream, int progress) {
        }

        public void stopSample() {
            if (this.mCurrent != null) {
                this.mCurrent.stopSample();
            }
        }
    }

    private void initRingtones(PreferenceCategory root) {
        this.mPhoneRingtonePreference = root.findPreference("ringtone");
        if (this.mPhoneRingtonePreference != null && !this.mVoiceCapable) {
            root.removePreference(this.mPhoneRingtonePreference);
            this.mPhoneRingtonePreference = null;
        }
        this.mNotificationRingtonePreference = root.findPreference("notification_ringtone");
    }

    private void lookupRingtoneNames() {
        AsyncTask.execute(this.mLookupRingtoneNames);
    }

    private static CharSequence updateRingtoneName(Context context, int type) {
        if (context == null) {
            Log.e("NotificationSettings", "Unable to update ringtone name, no context provided");
            return null;
        }
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        CharSequence summary = context.getString(android.R.string.imProtocolGoogleTalk);
        if (ringtoneUri == null) {
            return context.getString(android.R.string.imProtocolAim);
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

    private void initVibrateWhenRinging(PreferenceCategory root) {
        this.mVibrateWhenRinging = (TwoStatePreference) root.findPreference("vibrate_when_ringing");
        if (this.mVibrateWhenRinging == null) {
            Log.i("NotificationSettings", "Preference not found: vibrate_when_ringing");
            return;
        }
        if (!this.mVoiceCapable) {
            root.removePreference(this.mVibrateWhenRinging);
            this.mVibrateWhenRinging = null;
        } else {
            this.mVibrateWhenRinging.setPersistent(false);
            updateVibrateWhenRinging();
            this.mVibrateWhenRinging.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean val = ((Boolean) newValue).booleanValue();
                    return Settings.System.putInt(NotificationSettings.this.getContentResolver(), "vibrate_when_ringing", val ? 1 : 0);
                }
            });
        }
    }

    private void updateVibrateWhenRinging() {
        if (this.mVibrateWhenRinging != null) {
            this.mVibrateWhenRinging.setChecked(Settings.System.getInt(getContentResolver(), "vibrate_when_ringing", 0) != 0);
        }
    }

    private void initPulse(PreferenceCategory parent) {
        this.mNotificationPulse = (TwoStatePreference) parent.findPreference("notification_pulse");
        if (this.mNotificationPulse == null) {
            Log.i("NotificationSettings", "Preference not found: notification_pulse");
        } else if (!getResources().getBoolean(android.R.^attr-private.dialogCustomTitleDecorLayout)) {
            parent.removePreference(this.mNotificationPulse);
        } else {
            updatePulse();
            this.mNotificationPulse.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean val = ((Boolean) newValue).booleanValue();
                    return Settings.System.putInt(NotificationSettings.this.getContentResolver(), "notification_light_pulse", val ? 1 : 0);
                }
            });
        }
    }

    private void updatePulse() {
        if (this.mNotificationPulse != null) {
            try {
                this.mNotificationPulse.setChecked(Settings.System.getInt(getContentResolver(), "notification_light_pulse") == 1);
            } catch (Settings.SettingNotFoundException e) {
                Log.e("NotificationSettings", "notification_light_pulse not found");
            }
        }
    }

    private void initLockscreenNotifications(PreferenceCategory parent) {
        this.mLockscreen = (DropDownPreference) parent.findPreference("lock_screen_notifications");
        if (this.mLockscreen == null) {
            Log.i("NotificationSettings", "Preference not found: lock_screen_notifications");
            return;
        }
        this.mLockscreen.addItem(R.string.lock_screen_notifications_summary_show, Integer.valueOf(R.string.lock_screen_notifications_summary_show));
        if (this.mSecure) {
            this.mLockscreen.addItem(R.string.lock_screen_notifications_summary_hide, Integer.valueOf(R.string.lock_screen_notifications_summary_hide));
        }
        this.mLockscreen.addItem(R.string.lock_screen_notifications_summary_disable, Integer.valueOf(R.string.lock_screen_notifications_summary_disable));
        updateLockscreenNotifications();
        this.mLockscreen.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                int val = ((Integer) value).intValue();
                if (val != NotificationSettings.this.mLockscreenSelectedValue) {
                    boolean enabled = val != R.string.lock_screen_notifications_summary_disable;
                    boolean show = val == R.string.lock_screen_notifications_summary_show;
                    Settings.Secure.putInt(NotificationSettings.this.getContentResolver(), "lock_screen_allow_private_notifications", show ? 1 : 0);
                    Settings.Secure.putInt(NotificationSettings.this.getContentResolver(), "lock_screen_show_notifications", enabled ? 1 : 0);
                    NotificationSettings.this.mLockscreenSelectedValue = val;
                }
                return true;
            }
        });
    }

    private void updateLockscreenNotifications() {
        int i;
        if (this.mLockscreen != null) {
            boolean enabled = getLockscreenNotificationsEnabled();
            boolean allowPrivate = !this.mSecure || getLockscreenAllowPrivateNotifications();
            if (enabled) {
                i = allowPrivate ? R.string.lock_screen_notifications_summary_show : R.string.lock_screen_notifications_summary_hide;
            } else {
                i = R.string.lock_screen_notifications_summary_disable;
            }
            this.mLockscreenSelectedValue = i;
            this.mLockscreen.setSelectedValue(Integer.valueOf(this.mLockscreenSelectedValue));
        }
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(), "lock_screen_show_notifications", 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(), "lock_screen_allow_private_notifications", 0) != 0;
    }

    private void refreshNotificationListeners() {
        if (this.mNotificationAccess != null) {
            int total = NotificationAccessSettings.getListenersCount(this.mPM);
            if (total == 0) {
                getPreferenceScreen().removePreference(this.mNotificationAccess);
                return;
            }
            int n = NotificationAccessSettings.getEnabledListenersCount(this.mContext);
            if (n == 0) {
                this.mNotificationAccess.setSummary(getResources().getString(R.string.manage_notification_access_summary_zero));
            } else {
                this.mNotificationAccess.setSummary(String.format(getResources().getQuantityString(R.plurals.manage_notification_access_summary_nonzero, n, Integer.valueOf(n)), new Object[0]));
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri LOCK_SCREEN_PRIVATE_URI;
        private final Uri LOCK_SCREEN_SHOW_URI;
        private final Uri NOTIFICATION_LIGHT_PULSE_URI;
        private final Uri VIBRATE_WHEN_RINGING_URI;

        public SettingsObserver() {
            super(NotificationSettings.this.mHandler);
            this.VIBRATE_WHEN_RINGING_URI = Settings.System.getUriFor("vibrate_when_ringing");
            this.NOTIFICATION_LIGHT_PULSE_URI = Settings.System.getUriFor("notification_light_pulse");
            this.LOCK_SCREEN_PRIVATE_URI = Settings.Secure.getUriFor("lock_screen_allow_private_notifications");
            this.LOCK_SCREEN_SHOW_URI = Settings.Secure.getUriFor("lock_screen_show_notifications");
        }

        public void register(boolean register) {
            ContentResolver cr = NotificationSettings.this.getContentResolver();
            if (register) {
                cr.registerContentObserver(this.VIBRATE_WHEN_RINGING_URI, false, this);
                cr.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, false, this);
                cr.registerContentObserver(this.LOCK_SCREEN_PRIVATE_URI, false, this);
                cr.registerContentObserver(this.LOCK_SCREEN_SHOW_URI, false, this);
                return;
            }
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (this.VIBRATE_WHEN_RINGING_URI.equals(uri)) {
                NotificationSettings.this.updateVibrateWhenRinging();
            }
            if (this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                NotificationSettings.this.updatePulse();
            }
            if (this.LOCK_SCREEN_PRIVATE_URI.equals(uri) || this.LOCK_SCREEN_SHOW_URI.equals(uri)) {
                NotificationSettings.this.updateLockscreenNotifications();
            }
        }
    }

    private final class H extends Handler {
        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    NotificationSettings.this.mPhoneRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
                case 2:
                    NotificationSettings.this.mNotificationRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
                case 3:
                    NotificationSettings.this.mVolumeCallback.stopSample();
                    break;
                case 4:
                    NotificationSettings.this.updateEffectsSuppressor();
                    break;
                case 5:
                    NotificationSettings.this.updateRingerMode();
                    break;
            }
        }
    }

    private class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        private Receiver() {
        }

        public void register(boolean register) {
            if (this.mRegistered != register) {
                if (!register) {
                    NotificationSettings.this.mContext.unregisterReceiver(this);
                } else {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
                    filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
                    NotificationSettings.this.mContext.registerReceiver(this, filter);
                }
                this.mRegistered = register;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED".equals(action)) {
                NotificationSettings.this.mHandler.sendEmptyMessage(4);
            } else if ("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION".equals(action)) {
                NotificationSettings.this.mHandler.sendEmptyMessage(5);
            }
        }
    }
}
