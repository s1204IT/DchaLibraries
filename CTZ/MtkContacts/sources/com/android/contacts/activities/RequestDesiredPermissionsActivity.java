package com.android.contacts.activities;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import java.util.ArrayList;

public class RequestDesiredPermissionsActivity extends RequestPermissionsActivityBase {
    private static String[] sDesiredPermissions;

    @Override
    protected String[] getPermissions() {
        return getPermissions(getPackageManager());
    }

    public static boolean startPermissionActivity(Activity activity) {
        Bundle extras = activity.getIntent().getExtras();
        if (extras == null || !extras.getBoolean("started_permissions_activity", false)) {
            return startPermissionActivity(activity, getPermissions(activity.getPackageManager()), RequestDesiredPermissionsActivity.class);
        }
        return false;
    }

    private static String[] getPermissions(PackageManager packageManager) {
        if (sDesiredPermissions == null) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("android.permission.READ_CALENDAR");
            if (packageManager.hasSystemFeature("android.hardware.telephony")) {
                arrayList.add("android.permission.READ_SMS");
            }
            sDesiredPermissions = (String[]) arrayList.toArray(new String[0]);
        }
        return sDesiredPermissions;
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        this.mPreviousActivityIntent.setFlags(65536);
        startActivity(this.mPreviousActivityIntent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }
}
