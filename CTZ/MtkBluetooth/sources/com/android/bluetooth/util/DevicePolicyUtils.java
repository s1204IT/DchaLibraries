package com.android.bluetooth.util;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;

public final class DevicePolicyUtils {
    private static boolean isBluetoothWorkContactSharingDisabled(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        for (UserInfo userInfo : ((UserManager) context.getSystemService("user")).getProfiles(UserHandle.myUserId())) {
            if (userInfo.isManagedProfile()) {
                return devicePolicyManager.getBluetoothContactSharingDisabled(new UserHandle(userInfo.id));
            }
        }
        return true;
    }

    public static Uri getEnterprisePhoneUri(Context context) {
        return isBluetoothWorkContactSharingDisabled(context) ? ContactsContract.CommonDataKinds.Phone.CONTENT_URI : ContactsContract.CommonDataKinds.Phone.ENTERPRISE_CONTENT_URI;
    }
}
