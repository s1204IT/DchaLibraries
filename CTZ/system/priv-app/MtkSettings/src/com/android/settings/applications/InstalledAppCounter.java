package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.UserHandle;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.List;

/* loaded from: classes.dex */
public abstract class InstalledAppCounter extends AppCounter {
    private final int mInstallReason;

    public InstalledAppCounter(Context context, int i, PackageManagerWrapper packageManagerWrapper) {
        super(context, packageManagerWrapper);
        this.mInstallReason = i;
    }

    @Override // com.android.settings.applications.AppCounter
    protected boolean includeInCount(ApplicationInfo applicationInfo) {
        return includeInCount(this.mInstallReason, this.mPm, applicationInfo);
    }

    public static boolean includeInCount(int i, PackageManagerWrapper packageManagerWrapper, ApplicationInfo applicationInfo) {
        int userId = UserHandle.getUserId(applicationInfo.uid);
        if (i != -1 && packageManagerWrapper.getInstallReason(applicationInfo.packageName, new UserHandle(userId)) != i) {
            return false;
        }
        if ((applicationInfo.flags & 128) != 0 || (applicationInfo.flags & 1) == 0) {
            return true;
        }
        List<ResolveInfo> listQueryIntentActivitiesAsUser = packageManagerWrapper.queryIntentActivitiesAsUser(new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER").setPackage(applicationInfo.packageName), 786944, userId);
        return (listQueryIntentActivitiesAsUser == null || listQueryIntentActivitiesAsUser.size() == 0) ? false : true;
    }
}
