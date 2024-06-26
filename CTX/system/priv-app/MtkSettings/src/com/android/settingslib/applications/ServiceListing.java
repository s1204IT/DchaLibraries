package com.android.settingslib.applications;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Slog;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class ServiceListing {
    private final List<Callback> mCallbacks;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final HashSet<ComponentName> mEnabledServices;
    private final String mIntentAction;
    private boolean mListening;
    private final String mNoun;
    private final BroadcastReceiver mPackageReceiver;
    private final String mPermission;
    private final List<ServiceInfo> mServices;
    private final String mSetting;
    private final ContentObserver mSettingsObserver;
    private final String mTag;

    /* loaded from: classes.dex */
    public interface Callback {
        void onServicesReloaded(List<ServiceInfo> list);
    }

    private ServiceListing(Context context, String str, String str2, String str3, String str4, String str5) {
        this.mEnabledServices = new HashSet<>();
        this.mServices = new ArrayList();
        this.mCallbacks = new ArrayList();
        this.mSettingsObserver = new ContentObserver(new Handler()) { // from class: com.android.settingslib.applications.ServiceListing.1
            @Override // android.database.ContentObserver
            public void onChange(boolean z, Uri uri) {
                ServiceListing.this.reload();
            }
        };
        this.mPackageReceiver = new BroadcastReceiver() { // from class: com.android.settingslib.applications.ServiceListing.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                ServiceListing.this.reload();
            }
        };
        this.mContentResolver = context.getContentResolver();
        this.mContext = context;
        this.mTag = str;
        this.mSetting = str2;
        this.mIntentAction = str3;
        this.mPermission = str4;
        this.mNoun = str5;
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void setListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        if (this.mListening) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            intentFilter.addDataScheme("package");
            this.mContext.registerReceiver(this.mPackageReceiver, intentFilter);
            this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(this.mSetting), false, this.mSettingsObserver);
            return;
        }
        this.mContext.unregisterReceiver(this.mPackageReceiver);
        this.mContentResolver.unregisterContentObserver(this.mSettingsObserver);
    }

    private void saveEnabledServices() {
        Iterator<ComponentName> it = this.mEnabledServices.iterator();
        StringBuilder sb = null;
        while (it.hasNext()) {
            ComponentName next = it.next();
            if (sb == null) {
                sb = new StringBuilder();
            } else {
                sb.append(':');
            }
            sb.append(next.flattenToString());
        }
        Settings.Secure.putString(this.mContentResolver, this.mSetting, sb != null ? sb.toString() : "");
    }

    private void loadEnabledServices() {
        this.mEnabledServices.clear();
        String string = Settings.Secure.getString(this.mContentResolver, this.mSetting);
        if (string != null && !"".equals(string)) {
            for (String str : string.split(":")) {
                ComponentName unflattenFromString = ComponentName.unflattenFromString(str);
                if (unflattenFromString != null) {
                    this.mEnabledServices.add(unflattenFromString);
                }
            }
        }
    }

    public void reload() {
        loadEnabledServices();
        this.mServices.clear();
        for (ResolveInfo resolveInfo : new PackageManagerWrapper(this.mContext.getPackageManager()).queryIntentServicesAsUser(new Intent(this.mIntentAction), 132, ActivityManager.getCurrentUser())) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (!this.mPermission.equals(serviceInfo.permission)) {
                String str = this.mTag;
                Slog.w(str, "Skipping " + this.mNoun + " service " + serviceInfo.packageName + "/" + serviceInfo.name + ": it does not require the permission " + this.mPermission);
            } else {
                this.mServices.add(serviceInfo);
            }
        }
        for (Callback callback : this.mCallbacks) {
            callback.onServicesReloaded(this.mServices);
        }
    }

    public boolean isEnabled(ComponentName componentName) {
        return this.mEnabledServices.contains(componentName);
    }

    public void setEnabled(ComponentName componentName, boolean z) {
        if (z) {
            this.mEnabledServices.add(componentName);
        } else {
            this.mEnabledServices.remove(componentName);
        }
        saveEnabledServices();
    }

    /* loaded from: classes.dex */
    public static class Builder {
        private final Context mContext;
        private String mIntentAction;
        private String mNoun;
        private String mPermission;
        private String mSetting;
        private String mTag;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setTag(String str) {
            this.mTag = str;
            return this;
        }

        public Builder setSetting(String str) {
            this.mSetting = str;
            return this;
        }

        public Builder setIntentAction(String str) {
            this.mIntentAction = str;
            return this;
        }

        public Builder setPermission(String str) {
            this.mPermission = str;
            return this;
        }

        public Builder setNoun(String str) {
            this.mNoun = str;
            return this;
        }

        public ServiceListing build() {
            return new ServiceListing(this.mContext, this.mTag, this.mSetting, this.mIntentAction, this.mPermission, this.mNoun);
        }
    }
}
