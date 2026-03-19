package com.android.server.location;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocationManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public final class LocationBlacklist extends ContentObserver {
    private static final String BLACKLIST_CONFIG_NAME = "locationPackagePrefixBlacklist";
    private static final boolean D = LocationManagerService.D;
    private static final String TAG = "LocationBlacklist";
    private static final String WHITELIST_CONFIG_NAME = "locationPackagePrefixWhitelist";
    private String[] mBlacklist;
    private final Context mContext;
    private int mCurrentUserId;
    private final Object mLock;
    private String[] mWhitelist;

    public LocationBlacklist(Context context, Handler handler) {
        super(handler);
        this.mLock = new Object();
        this.mWhitelist = new String[0];
        this.mBlacklist = new String[0];
        this.mCurrentUserId = 0;
        this.mContext = context;
    }

    public void init() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(BLACKLIST_CONFIG_NAME), false, this, -1);
        reloadBlacklist();
    }

    private void reloadBlacklistLocked() {
        this.mWhitelist = getStringArrayLocked(WHITELIST_CONFIG_NAME);
        if (D) {
            Slog.d(TAG, "whitelist: " + Arrays.toString(this.mWhitelist));
        }
        this.mBlacklist = getStringArrayLocked(BLACKLIST_CONFIG_NAME);
        if (D) {
            Slog.d(TAG, "blacklist: " + Arrays.toString(this.mBlacklist));
        }
    }

    private void reloadBlacklist() {
        synchronized (this.mLock) {
            reloadBlacklistLocked();
        }
    }

    public boolean isBlacklisted(String str) {
        synchronized (this.mLock) {
            for (String str2 : this.mBlacklist) {
                if (str.startsWith(str2) && !inWhitelist(str)) {
                    if (D) {
                        Log.d(TAG, "dropping location (blacklisted): " + str + " matches " + str2);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private boolean inWhitelist(String str) {
        synchronized (this.mLock) {
            for (String str2 : this.mWhitelist) {
                if (str.startsWith(str2)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void onChange(boolean z) {
        reloadBlacklist();
    }

    public void switchUser(int i) {
        synchronized (this.mLock) {
            this.mCurrentUserId = i;
            reloadBlacklistLocked();
        }
    }

    private String[] getStringArrayLocked(String str) {
        String stringForUser;
        synchronized (this.mLock) {
            stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), str, this.mCurrentUserId);
        }
        if (stringForUser == null) {
            return new String[0];
        }
        String[] strArrSplit = stringForUser.split(",");
        ArrayList arrayList = new ArrayList();
        for (String str2 : strArrSplit) {
            String strTrim = str2.trim();
            if (!strTrim.isEmpty()) {
                arrayList.add(strTrim);
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("mWhitelist=" + Arrays.toString(this.mWhitelist) + " mBlacklist=" + Arrays.toString(this.mBlacklist));
    }
}
