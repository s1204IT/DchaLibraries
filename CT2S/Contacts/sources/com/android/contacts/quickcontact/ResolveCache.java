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
    private static final HashSet<String> sPreferResolve = Sets.newHashSet("com.android.email", "com.google.android.email", "com.android.phone", "com.google.android.apps.maps", "com.android.chrome", "com.google.android.browser", "com.android.browser");
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
            IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REPLACED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addDataScheme("package");
            applicationContext.registerReceiver(sInstance.mPackageIntentReceiver, filter);
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

    protected Entry getEntry(String mimeType, Intent intent) {
        Entry entry = this.mCache.get(mimeType);
        if (entry != null) {
            return entry;
        }
        Entry entry2 = new Entry();
        if ("vnd.android.cursor.item/sip_address".equals(mimeType) && !PhoneCapabilityTester.isSipPhone(this.mContext)) {
            intent = null;
        }
        if (intent != null) {
            List<ResolveInfo> matches = this.mPackageManager.queryIntentActivities(intent, 65536);
            ResolveInfo bestResolve = null;
            int size = matches.size();
            if (size == 1) {
                ResolveInfo bestResolve2 = matches.get(0);
                bestResolve = bestResolve2;
            } else if (size > 1) {
                bestResolve = getBestResolve(intent, matches);
            }
            if (bestResolve != null) {
                Drawable icon = bestResolve.loadIcon(this.mPackageManager);
                entry2.bestResolve = bestResolve;
                entry2.icon = icon;
            }
        }
        this.mCache.put(mimeType, entry2);
        return entry2;
    }

    protected ResolveInfo getBestResolve(Intent intent, List<ResolveInfo> matches) {
        ResolveInfo foundResolve = this.mPackageManager.resolveActivity(intent, 65536);
        boolean foundDisambig = (foundResolve.match & 268369920) == 0;
        if (foundDisambig) {
            ResolveInfo firstSystem = null;
            for (ResolveInfo info : matches) {
                boolean isSystem = (info.activityInfo.applicationInfo.flags & 1) != 0;
                boolean isPrefer = sPreferResolve.contains(info.activityInfo.applicationInfo.packageName);
                if (isPrefer) {
                    return info;
                }
                if (isSystem && firstSystem == null) {
                    firstSystem = info;
                }
            }
            if (firstSystem == null) {
                firstSystem = matches.get(0);
            }
            return firstSystem;
        }
        return foundResolve;
    }

    public Drawable getIcon(String mimeType, Intent intent) {
        return getEntry(mimeType, intent).icon;
    }
}
