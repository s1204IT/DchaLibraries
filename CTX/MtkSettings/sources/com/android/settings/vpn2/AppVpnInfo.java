package com.android.settings.vpn2;

import com.android.internal.util.Preconditions;
import java.util.Objects;

class AppVpnInfo implements Comparable {
    public final String packageName;
    public final int userId;

    public AppVpnInfo(int i, String str) {
        this.userId = i;
        this.packageName = (String) Preconditions.checkNotNull(str);
    }

    @Override
    public int compareTo(Object obj) {
        AppVpnInfo appVpnInfo = (AppVpnInfo) obj;
        int iCompareTo = this.packageName.compareTo(appVpnInfo.packageName);
        if (iCompareTo == 0) {
            return appVpnInfo.userId - this.userId;
        }
        return iCompareTo;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AppVpnInfo)) {
            return false;
        }
        AppVpnInfo appVpnInfo = (AppVpnInfo) obj;
        return this.userId == appVpnInfo.userId && Objects.equals(this.packageName, appVpnInfo.packageName);
    }

    public int hashCode() {
        return Objects.hash(this.packageName, Integer.valueOf(this.userId));
    }
}
