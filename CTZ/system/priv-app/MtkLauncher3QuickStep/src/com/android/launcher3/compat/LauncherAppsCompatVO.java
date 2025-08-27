package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.compat.ShortcutConfigActivityInfo;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.util.LooperExecutor;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.List;

@TargetApi(26)
/* loaded from: classes.dex */
public class LauncherAppsCompatVO extends LauncherAppsCompatVL {
    LauncherAppsCompatVO(Context context) {
        super(context);
    }

    @Override // com.android.launcher3.compat.LauncherAppsCompatVL, com.android.launcher3.compat.LauncherAppsCompat
    public ApplicationInfo getApplicationInfo(String str, int i, UserHandle userHandle) throws PackageManager.NameNotFoundException {
        try {
            ApplicationInfo applicationInfo = this.mLauncherApps.getApplicationInfo(str, i, userHandle);
            if ((applicationInfo.flags & 8388608) != 0) {
                if (applicationInfo.enabled) {
                    return applicationInfo;
                }
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override // com.android.launcher3.compat.LauncherAppsCompatVL, com.android.launcher3.compat.LauncherAppsCompat
    public List<ShortcutConfigActivityInfo> getCustomShortcutActivityList(@Nullable PackageUserKey packageUserKey) {
        String str;
        List<UserHandle> userProfiles;
        ArrayList arrayList = new ArrayList();
        UserHandle userHandleMyUserHandle = Process.myUserHandle();
        if (packageUserKey == null) {
            userProfiles = UserManagerCompat.getInstance(this.mContext).getUserProfiles();
            str = null;
        } else {
            ArrayList arrayList2 = new ArrayList(1);
            arrayList2.add(packageUserKey.mUser);
            str = packageUserKey.mPackageName;
            userProfiles = arrayList2;
        }
        for (UserHandle userHandle : userProfiles) {
            boolean zEquals = userHandleMyUserHandle.equals(userHandle);
            for (LauncherActivityInfo launcherActivityInfo : this.mLauncherApps.getShortcutConfigActivityList(str, userHandle)) {
                if (zEquals || launcherActivityInfo.getApplicationInfo().targetSdkVersion >= 26) {
                    arrayList.add(new ShortcutConfigActivityInfo.ShortcutConfigActivityInfoVO(launcherActivityInfo));
                }
            }
        }
        return arrayList;
    }

    @Nullable
    public static ShortcutInfo createShortcutInfoFromPinItemRequest(Context context, final LauncherApps.PinItemRequest pinItemRequest, final long j) {
        if (pinItemRequest == null || pinItemRequest.getRequestType() != 1 || !pinItemRequest.isValid()) {
            return null;
        }
        if (j <= 0) {
            if (!pinItemRequest.accept()) {
                return null;
            }
        } else {
            new LooperExecutor(LauncherModel.getWorkerLooper()).execute(new Runnable() { // from class: com.android.launcher3.compat.LauncherAppsCompatVO.1
                @Override // java.lang.Runnable
                public void run() throws InterruptedException {
                    try {
                        Thread.sleep(j);
                    } catch (InterruptedException e) {
                    }
                    if (pinItemRequest.isValid()) {
                        pinItemRequest.accept();
                    }
                }
            });
        }
        ShortcutInfoCompat shortcutInfoCompat = new ShortcutInfoCompat(pinItemRequest.getShortcutInfo());
        ShortcutInfo shortcutInfo = new ShortcutInfo(shortcutInfoCompat, context);
        LauncherIcons launcherIconsObtain = LauncherIcons.obtain(context);
        launcherIconsObtain.createShortcutIcon(shortcutInfoCompat, false).applyTo(shortcutInfo);
        launcherIconsObtain.recycle();
        LauncherAppState.getInstance(context).getModel().updateAndBindShortcutInfo(shortcutInfo, shortcutInfoCompat);
        return shortcutInfo;
    }

    public static LauncherApps.PinItemRequest getPinItemRequest(Intent intent) {
        Parcelable parcelableExtra = intent.getParcelableExtra("android.content.pm.extra.PIN_ITEM_REQUEST");
        if (parcelableExtra instanceof LauncherApps.PinItemRequest) {
            return (LauncherApps.PinItemRequest) parcelableExtra;
        }
        return null;
    }
}
