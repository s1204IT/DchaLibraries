package com.android.settings.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.text.TextUtils;
import java.util.Objects;

/* loaded from: classes.dex */
public class UserAppInfo {
    public final ApplicationInfo appInfo;
    public final UserInfo userInfo;

    public UserAppInfo(UserInfo userInfo, ApplicationInfo applicationInfo) {
        this.userInfo = userInfo;
        this.appInfo = applicationInfo;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UserAppInfo userAppInfo = (UserAppInfo) obj;
        if (userAppInfo.userInfo.id == this.userInfo.id && TextUtils.equals(userAppInfo.appInfo.packageName, this.appInfo.packageName)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.userInfo.id), this.appInfo.packageName);
    }
}
