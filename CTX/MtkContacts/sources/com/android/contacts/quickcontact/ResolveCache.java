package com.android.contacts.quickcontact;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import com.android.contacts.util.PhoneCapabilityTester;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ResolveCache {
    private static ResolveCache sInstance;
    private static final HashSet<String> sPreferResolve = Sets.newHashSet("com.android.email", "com.google.android.email", "com.android.phone", "com.google.android.apps.maps", "com.android.chrome", "org.chromium.webview_shell", "com.google.android.browser", "com.android.browser");
    private final Context mContext;
    private final PackageManager mPackageManager;
    private BroadcastReceiver mPackageIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ResolveCache.flush();
        }
    };
    private HashMap<String, Entry> mCache = new HashMap<>();

    public static synchronized ResolveCache getInstance(Context context) {
        if (sInstance == null) {
            Context applicationContext = context.getApplicationContext();
            sInstance = new ResolveCache(applicationContext);
            IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addDataScheme("package");
            applicationContext.registerReceiver(sInstance.mPackageIntentReceiver, intentFilter);
        }
        return sInstance;
    }

    private static synchronized void flush() {
        sInstance = null;
    }

    private static class Entry {
        public ResolveInfo bestResolve;
        public Drawable icon;

        private Entry() {
        }
    }

    private ResolveCache(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    protected Entry getEntry(String str, Intent intent) {
        Entry entry = this.mCache.get(str);
        if (entry != null) {
            return entry;
        }
        ResolveInfo bestResolve = null;
        Entry entry2 = new Entry();
        if ("vnd.android.cursor.item/sip_address".equals(str) && !PhoneCapabilityTester.isSipPhone(this.mContext)) {
            intent = null;
        }
        if (intent != null) {
            List<ResolveInfo> listQueryIntentActivities = this.mPackageManager.queryIntentActivities(intent, 65536);
            int size = listQueryIntentActivities.size();
            if (size == 1) {
                bestResolve = listQueryIntentActivities.get(0);
            } else if (size > 1) {
                bestResolve = getBestResolve(intent, listQueryIntentActivities);
            }
            if (bestResolve != null) {
                Drawable drawableLoadIcon = bestResolve.loadIcon(this.mPackageManager);
                entry2.bestResolve = bestResolve;
                entry2.icon = drawableLoadIcon;
            }
        }
        this.mCache.put(str, entry2);
        return entry2;
    }

    protected ResolveInfo getBestResolve(Intent intent, List<ResolveInfo> list) {
        ResolveInfo resolveInfoResolveActivity = this.mPackageManager.resolveActivity(intent, 65536);
        if (!((resolveInfoResolveActivity.match & 268369920) == 0)) {
            return resolveInfoResolveActivity;
        }
        ResolveInfo resolveInfo = null;
        for (ResolveInfo resolveInfo2 : list) {
            boolean z = (resolveInfo2.activityInfo.applicationInfo.flags & 1) != 0;
            if (sPreferResolve.contains(resolveInfo2.activityInfo.applicationInfo.packageName)) {
                return resolveInfo2;
            }
            if (z && resolveInfo == null) {
                resolveInfo = resolveInfo2;
            }
        }
        return resolveInfo != null ? resolveInfo : list.get(0);
    }

    public Drawable getIcon(String str, Intent intent) {
        return getEntry(str, intent).icon;
    }
}
