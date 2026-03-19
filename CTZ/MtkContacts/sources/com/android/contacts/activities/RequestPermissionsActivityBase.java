package com.android.contacts.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Trace;
import android.support.v4.app.ActivityCompat;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.util.PermissionsUtil;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class RequestPermissionsActivityBase extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {
    protected boolean mIsCallerSelf;
    protected Intent mPreviousActivityIntent;

    protected abstract String[] getPermissions();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPreviousActivityIntent = (Intent) getIntent().getExtras().get("previous_intent");
        this.mIsCallerSelf = getIntent().getBooleanExtra("is_caller_self", false);
        if (bundle == null) {
            requestPermissions();
        }
    }

    protected static boolean startPermissionActivity(Activity activity, String[] strArr, Class<?> cls) {
        return startPermissionActivity(activity, strArr, false, cls);
    }

    protected static boolean startPermissionActivity(Activity activity, String[] strArr, boolean z, Class<?> cls) {
        if (!hasPermissions(activity, strArr)) {
            Intent intent = new Intent(activity, cls);
            activity.getIntent().putExtra("started_permissions_activity", true);
            intent.putExtra("previous_intent", activity.getIntent());
            intent.putExtra("is_caller_self", z);
            activity.startActivity(intent);
            activity.finish();
            return true;
        }
        AccountTypeManager.getInstance(activity);
        return false;
    }

    protected boolean isAllGranted(String[] strArr, int[] iArr) {
        for (int i = 0; i < strArr.length; i++) {
            if (iArr[i] != 0 && isPermissionRequired(strArr[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean isPermissionRequired(String str) {
        return Arrays.asList(getPermissions()).contains(str);
    }

    private void requestPermissions() {
        Trace.beginSection("requestPermissions");
        try {
            ArrayList arrayList = new ArrayList();
            for (String str : getPermissions()) {
                if (!PermissionsUtil.hasPermission(this, str)) {
                    arrayList.add(str);
                }
            }
            if (arrayList.size() == 0) {
                Log.e("RequestPermissionsActivityBase", "[requestPermissions] activity=" + getClass().getSimpleName() + ", getPermissions()=" + Arrays.toString(getPermissions()));
                finish();
                return;
            }
            ActivityCompat.requestPermissions(this, (String[]) arrayList.toArray(new String[arrayList.size()]), 1);
        } finally {
            Trace.endSection();
        }
    }

    protected static boolean hasPermissions(Context context, String[] strArr) {
        Trace.beginSection("hasPermission");
        try {
            for (String str : strArr) {
                if (!PermissionsUtil.hasPermission(context, str)) {
                    Log.d("RequestPermissionsActivityBase", "[hasPermission] no permission:" + str);
                    return false;
                }
            }
            Trace.endSection();
            return true;
        } finally {
            Trace.endSection();
        }
    }
}
