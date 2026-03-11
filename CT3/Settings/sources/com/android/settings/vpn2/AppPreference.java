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

    public AppPreference(Context context, int userId, String packageName) {
        super(context, null);
        super.setUserId(userId);
        this.mPackageName = packageName;
        String label = packageName;
        Drawable icon = null;
        try {
            Context userContext = getUserContext();
            PackageManager pm = userContext.getPackageManager();
            try {
                PackageInfo pkgInfo = pm.getPackageInfo(this.mPackageName, 0);
                if (pkgInfo != null) {
                    icon = pkgInfo.applicationInfo.loadIcon(pm);
                    label = VpnConfig.getVpnLabel(userContext, this.mPackageName).toString();
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            if (icon == null) {
                icon = pm.getDefaultActivityIcon();
            }
        } catch (PackageManager.NameNotFoundException e2) {
        }
        this.mName = label;
        setTitle(this.mName);
        setIcon(icon);
    }

    public PackageInfo getPackageInfo() {
        try {
            PackageManager pm = getUserContext().getPackageManager();
            return pm.getPackageInfo(this.mPackageName, 0);
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
        UserHandle user = UserHandle.of(this.mUserId);
        return getContext().createPackageContextAsUser(getContext().getPackageName(), 0, user);
    }

    @Override
    public int compareTo(Preference preference) {
        if (preference instanceof AppPreference) {
            AppPreference another = (AppPreference) preference;
            int result = another.mState - this.mState;
            if (result == 0) {
                int result2 = this.mName.compareToIgnoreCase(another.mName);
                if (result2 == 0) {
                    int result3 = this.mPackageName.compareTo(another.mPackageName);
                    if (result3 == 0) {
                        return this.mUserId - another.mUserId;
                    }
                    return result3;
                }
                return result2;
            }
            return result;
        }
        if (preference instanceof LegacyVpnPreference) {
            return -((LegacyVpnPreference) preference).compareTo((Preference) this);
        }
        return super.compareTo(preference);
    }
}
