package com.android.settings.vpn2;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import com.android.internal.net.VpnConfig;

public class AppPreference extends ManageablePreference {
    public static final int STATE_DISCONNECTED = STATE_NONE;
    private final String mName;
    private final String mPackageName;

    public AppPreference(Context context, int i, String str) {
        Drawable defaultActivityIcon;
        String string;
        super(context, null);
        Drawable drawable = null;
        super.setUserId(i);
        this.mPackageName = str;
        try {
            Context userContext = getUserContext();
            PackageManager packageManager = userContext.getPackageManager();
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(this.mPackageName, 0);
                if (packageInfo != null) {
                    Drawable drawableLoadIcon = packageInfo.applicationInfo.loadIcon(packageManager);
                    try {
                        string = VpnConfig.getVpnLabel(userContext, this.mPackageName).toString();
                        drawable = drawableLoadIcon;
                    } catch (PackageManager.NameNotFoundException e) {
                        drawable = drawableLoadIcon;
                    }
                } else {
                    string = str;
                }
                str = string;
            } catch (PackageManager.NameNotFoundException e2) {
            }
            if (drawable == null) {
                defaultActivityIcon = packageManager.getDefaultActivityIcon();
            } else {
                defaultActivityIcon = drawable;
            }
        } catch (PackageManager.NameNotFoundException e3) {
            defaultActivityIcon = drawable;
        }
        this.mName = str;
        setTitle(this.mName);
        setIcon(defaultActivityIcon);
    }

    public PackageInfo getPackageInfo() {
        try {
            return getUserContext().getPackageManager().getPackageInfo(this.mPackageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public String getLabel() {
        return this.mName;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    private Context getUserContext() throws PackageManager.NameNotFoundException {
        return getContext().createPackageContextAsUser(getContext().getPackageName(), 0, UserHandle.of(this.mUserId));
    }

    @Override
    public int compareTo(Preference preference) {
        if (preference instanceof AppPreference) {
            AppPreference appPreference = (AppPreference) preference;
            int i = appPreference.mState - this.mState;
            if (i == 0) {
                int iCompareToIgnoreCase = this.mName.compareToIgnoreCase(appPreference.mName);
                if (iCompareToIgnoreCase == 0) {
                    int iCompareTo = this.mPackageName.compareTo(appPreference.mPackageName);
                    if (iCompareTo == 0) {
                        return this.mUserId - appPreference.mUserId;
                    }
                    return iCompareTo;
                }
                return iCompareToIgnoreCase;
            }
            return i;
        }
        if (preference instanceof LegacyVpnPreference) {
            return -((LegacyVpnPreference) preference).compareTo((Preference) this);
        }
        return super.compareTo(preference);
    }
}
