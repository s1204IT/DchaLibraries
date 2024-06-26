package com.android.systemui.tuner;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.util.ArrayUtils;
import com.android.systemui.Dependency;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.leak.LeakDetector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes.dex */
public class TunerServiceImpl extends TunerService {
    private static final String[] RESET_BLACKLIST = {"sysui_qs_tiles", "doze_always_on"};
    private ContentResolver mContentResolver;
    private final Context mContext;
    private int mCurrentUser;
    private final HashSet<TunerService.Tunable> mTunables;
    private CurrentUserTracker mUserTracker;
    private final Observer mObserver = new Observer();
    private final ArrayMap<Uri, String> mListeningUris = new ArrayMap<>();
    private final ConcurrentHashMap<String, Set<TunerService.Tunable>> mTunableLookup = new ConcurrentHashMap<>();

    public TunerServiceImpl(Context context) {
        this.mTunables = LeakDetector.ENABLED ? new HashSet<>() : null;
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        for (UserInfo userInfo : UserManager.get(this.mContext).getUsers()) {
            this.mCurrentUser = userInfo.getUserHandle().getIdentifier();
            if (getValue("sysui_tuner_version", 0) != 4) {
                upgradeTuner(getValue("sysui_tuner_version", 0), 4);
            }
        }
        this.mCurrentUser = ActivityManager.getCurrentUser();
        this.mUserTracker = new CurrentUserTracker(this.mContext) { // from class: com.android.systemui.tuner.TunerServiceImpl.1
            @Override // com.android.systemui.settings.CurrentUserTracker
            public void onUserSwitched(int i) {
                TunerServiceImpl.this.mCurrentUser = i;
                TunerServiceImpl.this.reloadAll();
                TunerServiceImpl.this.reregisterAll();
            }
        };
        this.mUserTracker.startTracking();
    }

    private void upgradeTuner(int i, int i2) {
        String value;
        if (i < 1 && (value = getValue("icon_blacklist")) != null) {
            ArraySet<String> iconBlacklist = StatusBarIconController.getIconBlacklist(value);
            iconBlacklist.add("rotate");
            iconBlacklist.add("headset");
            Settings.Secure.putStringForUser(this.mContentResolver, "icon_blacklist", TextUtils.join(",", iconBlacklist), this.mCurrentUser);
        }
        if (i < 2) {
            setTunerEnabled(this.mContext, false);
        }
        if (i < 4) {
            new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)).postDelayed(new Runnable() { // from class: com.android.systemui.tuner.-$$Lambda$TunerServiceImpl$6-LaNwUHcRc1UGw8s8y1YCsTRbQ
                @Override // java.lang.Runnable
                public final void run() {
                    TunerServiceImpl.this.clearAll();
                }
            }, 5000L);
        }
        setValue("sysui_tuner_version", i2);
    }

    @Override // com.android.systemui.tuner.TunerService
    public String getValue(String str) {
        return Settings.Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser);
    }

    @Override // com.android.systemui.tuner.TunerService
    public void setValue(String str, String str2) {
        Settings.Secure.putStringForUser(this.mContentResolver, str, str2, this.mCurrentUser);
    }

    @Override // com.android.systemui.tuner.TunerService
    public int getValue(String str, int i) {
        return Settings.Secure.getIntForUser(this.mContentResolver, str, i, this.mCurrentUser);
    }

    @Override // com.android.systemui.tuner.TunerService
    public String getValue(String str, String str2) {
        String stringForUser = Settings.Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser);
        return stringForUser == null ? str2 : stringForUser;
    }

    @Override // com.android.systemui.tuner.TunerService
    public void setValue(String str, int i) {
        Settings.Secure.putIntForUser(this.mContentResolver, str, i, this.mCurrentUser);
    }

    @Override // com.android.systemui.tuner.TunerService
    public void addTunable(TunerService.Tunable tunable, String... strArr) {
        for (String str : strArr) {
            addTunable(tunable, str);
        }
    }

    private void addTunable(TunerService.Tunable tunable, String str) {
        if (!this.mTunableLookup.containsKey(str)) {
            this.mTunableLookup.put(str, new ArraySet());
        }
        this.mTunableLookup.get(str).add(tunable);
        if (LeakDetector.ENABLED) {
            this.mTunables.add(tunable);
            ((LeakDetector) Dependency.get(LeakDetector.class)).trackCollection(this.mTunables, "TunerService.mTunables");
        }
        Uri uriFor = Settings.Secure.getUriFor(str);
        if (!this.mListeningUris.containsKey(uriFor)) {
            this.mListeningUris.put(uriFor, str);
            this.mContentResolver.registerContentObserver(uriFor, false, this.mObserver, this.mCurrentUser);
        }
        tunable.onTuningChanged(str, Settings.Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser));
    }

    @Override // com.android.systemui.tuner.TunerService
    public void removeTunable(TunerService.Tunable tunable) {
        for (Set<TunerService.Tunable> set : this.mTunableLookup.values()) {
            set.remove(tunable);
        }
        if (LeakDetector.ENABLED) {
            this.mTunables.remove(tunable);
        }
    }

    protected void reregisterAll() {
        if (this.mListeningUris.size() == 0) {
            return;
        }
        this.mContentResolver.unregisterContentObserver(this.mObserver);
        for (Uri uri : this.mListeningUris.keySet()) {
            this.mContentResolver.registerContentObserver(uri, false, this.mObserver, this.mCurrentUser);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reloadSetting(Uri uri) {
        String str = this.mListeningUris.get(uri);
        Set<TunerService.Tunable> set = this.mTunableLookup.get(str);
        if (set == null) {
            return;
        }
        String stringForUser = Settings.Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser);
        for (TunerService.Tunable tunable : set) {
            tunable.onTuningChanged(str, stringForUser);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reloadAll() {
        for (String str : this.mTunableLookup.keySet()) {
            String stringForUser = Settings.Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser);
            for (TunerService.Tunable tunable : this.mTunableLookup.get(str)) {
                tunable.onTuningChanged(str, stringForUser);
            }
        }
    }

    @Override // com.android.systemui.tuner.TunerService
    public void clearAll() {
        Settings.Global.putString(this.mContentResolver, "sysui_demo_allowed", null);
        Intent intent = new Intent("com.android.systemui.demo");
        intent.putExtra("command", "exit");
        this.mContext.sendBroadcast(intent);
        for (String str : this.mTunableLookup.keySet()) {
            if (!ArrayUtils.contains(RESET_BLACKLIST, str)) {
                Settings.Secure.putString(this.mContentResolver, str, null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class Observer extends ContentObserver {
        public Observer() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri, int i) {
            if (i == ActivityManager.getCurrentUser()) {
                TunerServiceImpl.this.reloadSetting(uri);
            }
        }
    }
}
