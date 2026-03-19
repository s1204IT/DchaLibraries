package com.android.contacts.activities;

import android.app.Activity;
import android.content.Intent;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.util.PermissionsUtil;
import com.mediatek.contacts.util.Log;

public class RequestImportVCardPermissionsActivity extends RequestPermissionsActivity {
    private static final String[] REQUIRED_PERMISSIONS = {"android.permission.GET_ACCOUNTS", PermissionsUtil.CONTACTS, "android.permission.WRITE_CONTACTS", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.READ_PHONE_STATE"};

    @Override
    protected String[] getPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    public static boolean startPermissionActivity(Activity activity, boolean z) {
        return startPermissionActivityEx(activity, REQUIRED_PERMISSIONS, z, RequestImportVCardPermissionsActivity.class);
    }

    protected static boolean startPermissionActivityEx(Activity activity, String[] strArr, boolean z, Class<?> cls) {
        if (!hasPermissions(activity, strArr)) {
            Intent intent = new Intent(activity, cls);
            activity.getIntent().putExtra("started_permissions_activity", true);
            intent.putExtra("previous_intent", activity.getIntent());
            intent.putExtra("is_caller_self", z);
            activity.startActivity(intent);
            activity.setResult(11112);
            activity.finish();
            Log.d("RequestImportVCardPermissionsActivity", "[startPermissionActivityEx]" + activity.toString() + " finsih,result code: 11112");
            return true;
        }
        AccountTypeManager.getInstance(activity);
        return false;
    }
}
