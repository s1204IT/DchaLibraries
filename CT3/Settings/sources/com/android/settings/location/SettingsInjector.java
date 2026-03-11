package com.android.settings.location;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.settings.DimmableIconPreference;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;

class SettingsInjector {
    private final Context mContext;
    private final Set<Setting> mSettings = new HashSet();
    private final Handler mHandler = new StatusLoadingHandler(this, null);

    public SettingsInjector(Context context) {
        this.mContext = context;
    }

    private List<InjectedSetting> getSettings(UserHandle userHandle) {
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent("android.location.SettingInjectorService");
        int profileId = userHandle.getIdentifier();
        List<ResolveInfo> resolveInfos = pm.queryIntentServicesAsUser(intent, 128, profileId);
        if (Log.isLoggable("SettingsInjector", 3)) {
            Log.d("SettingsInjector", "Found services for profile id " + profileId + ": " + resolveInfos);
        }
        List<InjectedSetting> settings = new ArrayList<>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            try {
                InjectedSetting setting = parseServiceInfo(resolveInfo, userHandle, pm);
                if (setting == null) {
                    Log.w("SettingsInjector", "Unable to load service info " + resolveInfo);
                } else {
                    settings.add(setting);
                }
            } catch (IOException e) {
                Log.w("SettingsInjector", "Unable to load service info " + resolveInfo, e);
            } catch (XmlPullParserException e2) {
                Log.w("SettingsInjector", "Unable to load service info " + resolveInfo, e2);
            }
        }
        if (Log.isLoggable("SettingsInjector", 3)) {
            Log.d("SettingsInjector", "Loaded settings for profile id " + profileId + ": " + settings);
        }
        return settings;
    }

    private static InjectedSetting parseServiceInfo(ResolveInfo service, UserHandle userHandle, PackageManager pm) throws XmlPullParserException, IOException {
        int type;
        ServiceInfo si = service.serviceInfo;
        ApplicationInfo ai = si.applicationInfo;
        if ((ai.flags & 1) == 0 && Log.isLoggable("SettingsInjector", 5)) {
            Log.w("SettingsInjector", "Ignoring attempt to inject setting from app not in system image: " + service);
            return null;
        }
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                XmlResourceParser parser = si.loadXmlMetaData(pm, "android.location.SettingInjectorService");
                if (parser == null) {
                    throw new XmlPullParserException("No android.location.SettingInjectorService meta-data for " + service + ": " + si);
                }
                AttributeSet attrs = Xml.asAttributeSet(parser);
                do {
                    type = parser.next();
                    if (type == 1) {
                        break;
                    }
                } while (type != 2);
                String nodeName = parser.getName();
                if (!"injected-location-setting".equals(nodeName)) {
                    throw new XmlPullParserException("Meta-data does not start with injected-location-setting tag");
                }
                Resources res = pm.getResourcesForApplicationAsUser(si.packageName, userHandle.getIdentifier());
                InjectedSetting attributes = parseAttributes(si.packageName, si.name, userHandle, res, attrs);
                if (parser != null) {
                    parser.close();
                }
                return attributes;
            } catch (PackageManager.NameNotFoundException e) {
                throw new XmlPullParserException("Unable to load resources for package " + si.packageName);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                xmlResourceParser.close();
            }
            throw th;
        }
    }

    private static InjectedSetting parseAttributes(String packageName, String className, UserHandle userHandle, Resources res, AttributeSet attrs) {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.SettingInjectorService);
        try {
            String title = sa.getString(1);
            int iconId = sa.getResourceId(0, 0);
            String settingsActivity = sa.getString(2);
            if (Log.isLoggable("SettingsInjector", 3)) {
                Log.d("SettingsInjector", "parsed title: " + title + ", iconId: " + iconId + ", settingsActivity: " + settingsActivity);
            }
            return InjectedSetting.newInstance(packageName, className, title, iconId, userHandle, settingsActivity);
        } finally {
            sa.recycle();
        }
    }

    public List<Preference> getInjectedSettings(int profileId) {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        List<UserHandle> profiles = um.getUserProfiles();
        ArrayList<Preference> prefs = new ArrayList<>();
        int profileCount = profiles.size();
        for (int i = 0; i < profileCount; i++) {
            UserHandle userHandle = profiles.get(i);
            if (profileId == -2 || profileId == userHandle.getIdentifier()) {
                Iterable<InjectedSetting> settings = getSettings(userHandle);
                for (InjectedSetting setting : settings) {
                    Preference pref = addServiceSetting(prefs, setting);
                    this.mSettings.add(new Setting(this, setting, pref, null));
                }
            }
        }
        reloadStatusMessages();
        return prefs;
    }

    public void reloadStatusMessages() {
        if (Log.isLoggable("SettingsInjector", 3)) {
            Log.d("SettingsInjector", "reloadingStatusMessages: " + this.mSettings);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
    }

    private Preference addServiceSetting(List<Preference> prefs, InjectedSetting info) {
        PackageManager pm = this.mContext.getPackageManager();
        Drawable appIcon = pm.getDrawable(info.packageName, info.iconId, null);
        Drawable icon = pm.getUserBadgedIcon(appIcon, info.mUserHandle);
        CharSequence badgedAppLabel = pm.getUserBadgedLabel(info.title, info.mUserHandle);
        if (info.title.contentEquals(badgedAppLabel)) {
            badgedAppLabel = null;
        }
        Preference pref = new DimmableIconPreference(this.mContext, badgedAppLabel);
        pref.setTitle(info.title);
        pref.setSummary((CharSequence) null);
        pref.setIcon(icon);
        pref.setOnPreferenceClickListener(new ServiceSettingClickedListener(info));
        prefs.add(pref);
        return pref;
    }

    private class ServiceSettingClickedListener implements Preference.OnPreferenceClickListener {
        private InjectedSetting mInfo;

        public ServiceSettingClickedListener(InjectedSetting info) {
            this.mInfo = info;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent settingIntent = new Intent();
            settingIntent.setClassName(this.mInfo.packageName, this.mInfo.settingsActivity);
            settingIntent.setFlags(268468224);
            SettingsInjector.this.mContext.startActivityAsUser(settingIntent, this.mInfo.mUserHandle);
            return true;
        }
    }

    private final class StatusLoadingHandler extends Handler {
        private boolean mReloadRequested;
        private Set<Setting> mSettingsBeingLoaded;
        private Set<Setting> mSettingsToLoad;
        private Set<Setting> mTimedOutSettings;

        StatusLoadingHandler(SettingsInjector this$0, StatusLoadingHandler statusLoadingHandler) {
            this();
        }

        private StatusLoadingHandler() {
            this.mSettingsToLoad = new HashSet();
            this.mSettingsBeingLoaded = new HashSet();
            this.mTimedOutSettings = new HashSet();
        }

        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable("SettingsInjector", 3)) {
                Log.d("SettingsInjector", "handleMessage start: " + msg + ", " + this);
            }
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    this.mReloadRequested = true;
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    Setting receivedSetting = (Setting) msg.obj;
                    receivedSetting.maybeLogElapsedTime();
                    this.mSettingsBeingLoaded.remove(receivedSetting);
                    this.mTimedOutSettings.remove(receivedSetting);
                    removeMessages(3, receivedSetting);
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                    Setting timedOutSetting = (Setting) msg.obj;
                    this.mSettingsBeingLoaded.remove(timedOutSetting);
                    this.mTimedOutSettings.add(timedOutSetting);
                    if (Log.isLoggable("SettingsInjector", 5)) {
                        Log.w("SettingsInjector", "Timed out after " + timedOutSetting.getElapsedTime() + " millis trying to get status for: " + timedOutSetting);
                    }
                    break;
                default:
                    Log.wtf("SettingsInjector", "Unexpected what: " + msg);
                    break;
            }
            if (this.mSettingsBeingLoaded.size() > 0 || this.mTimedOutSettings.size() > 1) {
                if (Log.isLoggable("SettingsInjector", 2)) {
                    Log.v("SettingsInjector", "too many services already live for " + msg + ", " + this);
                    return;
                }
                return;
            }
            if (this.mReloadRequested && this.mSettingsToLoad.isEmpty() && this.mSettingsBeingLoaded.isEmpty() && this.mTimedOutSettings.isEmpty()) {
                if (Log.isLoggable("SettingsInjector", 2)) {
                    Log.v("SettingsInjector", "reloading because idle and reload requesteed " + msg + ", " + this);
                }
                this.mSettingsToLoad.addAll(SettingsInjector.this.mSettings);
                this.mReloadRequested = false;
            }
            Iterator<Setting> iter = this.mSettingsToLoad.iterator();
            if (!iter.hasNext()) {
                if (Log.isLoggable("SettingsInjector", 2)) {
                    Log.v("SettingsInjector", "nothing left to do for " + msg + ", " + this);
                    return;
                }
                return;
            }
            Setting setting = iter.next();
            iter.remove();
            setting.startService();
            this.mSettingsBeingLoaded.add(setting);
            Message timeoutMsg = obtainMessage(3, setting);
            sendMessageDelayed(timeoutMsg, 1000L);
            if (!Log.isLoggable("SettingsInjector", 3)) {
                return;
            }
            Log.d("SettingsInjector", "handleMessage end " + msg + ", " + this + ", started loading " + setting);
        }

        @Override
        public String toString() {
            return "StatusLoadingHandler{mSettingsToLoad=" + this.mSettingsToLoad + ", mSettingsBeingLoaded=" + this.mSettingsBeingLoaded + ", mTimedOutSettings=" + this.mTimedOutSettings + ", mReloadRequested=" + this.mReloadRequested + '}';
        }
    }

    private final class Setting {
        public final Preference preference;
        public final InjectedSetting setting;
        public long startMillis;

        Setting(SettingsInjector this$0, InjectedSetting setting, Preference preference, Setting setting2) {
            this(setting, preference);
        }

        private Setting(InjectedSetting setting, Preference preference) {
            this.setting = setting;
            this.preference = preference;
        }

        public String toString() {
            return "Setting{setting=" + this.setting + ", preference=" + this.preference + '}';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof Setting) {
                return this.setting.equals(((Setting) o).setting);
            }
            return false;
        }

        public int hashCode() {
            return this.setting.hashCode();
        }

        public void startService() {
            ActivityManager am = (ActivityManager) SettingsInjector.this.mContext.getSystemService("activity");
            if (!am.isUserRunning(this.setting.mUserHandle.getIdentifier())) {
                if (Log.isLoggable("SettingsInjector", 2)) {
                    Log.v("SettingsInjector", "Cannot start service as user " + this.setting.mUserHandle.getIdentifier() + " is not running");
                    return;
                }
                return;
            }
            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle bundle = msg.getData();
                    boolean enabled = bundle.getBoolean("enabled", true);
                    if (Log.isLoggable("SettingsInjector", 3)) {
                        Log.d("SettingsInjector", Setting.this.setting + ": received " + msg + ", bundle: " + bundle);
                    }
                    Setting.this.preference.setSummary((CharSequence) null);
                    Setting.this.preference.setEnabled(enabled);
                    SettingsInjector.this.mHandler.sendMessage(SettingsInjector.this.mHandler.obtainMessage(2, Setting.this));
                }
            };
            Messenger messenger = new Messenger(handler);
            Intent intent = this.setting.getServiceIntent();
            intent.putExtra("messenger", messenger);
            if (Log.isLoggable("SettingsInjector", 3)) {
                Log.d("SettingsInjector", this.setting + ": sending update intent: " + intent + ", handler: " + handler);
                this.startMillis = SystemClock.elapsedRealtime();
            } else {
                this.startMillis = 0L;
            }
            SettingsInjector.this.mContext.startServiceAsUser(intent, this.setting.mUserHandle);
        }

        public long getElapsedTime() {
            long end = SystemClock.elapsedRealtime();
            return end - this.startMillis;
        }

        public void maybeLogElapsedTime() {
            if (!Log.isLoggable("SettingsInjector", 3) || this.startMillis == 0) {
                return;
            }
            long elapsed = getElapsedTime();
            Log.d("SettingsInjector", this + " update took " + elapsed + " millis");
        }
    }
}
