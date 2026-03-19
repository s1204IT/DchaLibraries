package com.android.contacts.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.util.PermissionsUtil;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class RequestPermissionsActivity extends RequestPermissionsActivityBase {
    private static final String[] REQUIRED_PERMISSIONS = {PermissionsUtil.CONTACTS, "android.permission.READ_CALL_LOG", "android.permission.READ_PHONE_STATE", "android.permission.WRITE_CONTACTS", PermissionsUtil.PHONE, "android.permission.GET_ACCOUNTS"};
    private static String[] sRequiredPermissions;

    @Override
    protected String[] getPermissions() {
        return getPermissions(getPackageManager());
    }

    public static boolean hasRequiredPermissions(Context context) {
        return hasPermissions(context, getPermissions(context.getPackageManager()));
    }

    public static boolean startPermissionActivityIfNeeded(Activity activity) {
        return startPermissionActivity(activity, getPermissions(activity.getPackageManager()), RequestPermissionsActivity.class);
    }

    private static String[] getPermissions(PackageManager packageManager) {
        if (sRequiredPermissions == null) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("android.permission.GET_ACCOUNTS");
            arrayList.add(PermissionsUtil.CONTACTS);
            arrayList.add("android.permission.WRITE_CONTACTS");
            if (packageManager.hasSystemFeature("android.hardware.telephony")) {
                arrayList.add(PermissionsUtil.PHONE);
                arrayList.add("android.permission.READ_CALL_LOG");
                arrayList.add("android.permission.READ_PHONE_STATE");
                arrayList.add("android.permission.PROCESS_OUTGOING_CALLS");
            }
            sRequiredPermissions = (String[]) arrayList.toArray(new String[0]);
        }
        return sRequiredPermissions;
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        Log.d("RequestPermissionsActivity", "[onRequestPermissionsResult]mIsCallerSelf=" + this.mIsCallerSelf + ", permissions=" + strArr);
        if (strArr == null || strArr.length <= 0 || !isAllGranted(strArr, iArr)) {
            Toast.makeText(this, R.string.missing_required_permission, 0).show();
            finish();
            return;
        }
        this.mPreviousActivityIntent.setFlags(65536);
        if (this.mIsCallerSelf) {
            startActivityForResult(this.mPreviousActivityIntent, 0);
        } else {
            startActivity(this.mPreviousActivityIntent);
        }
        finish();
        overridePendingTransition(0, 0);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("broadcastPermissionsGranted"));
    }

    public static boolean hasBasicPermissions(Context context) {
        return hasPermissions(context, REQUIRED_PERMISSIONS);
    }
}
