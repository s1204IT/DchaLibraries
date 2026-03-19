package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.galleryportable.RuntimePermissionUtils;
import java.util.ArrayList;

public class RequestPermissionActivity extends Activity {
    private static final int PERMISSIONS_REQUEST_ALL_PERMISSIONS = 0;
    public static final String PREVIOUS_ACTIVITY_INTENT = "previous_intent";
    public static final String STARTED_PERMISSIONS_ACTIVITY = "started_permissions_activity";
    private static final String TAG = "VP_PermissionActivity";
    private ArrayList<String> mPermissionsNeeded = new ArrayList<>();
    private Intent mPreviousActivityIntent;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.v(TAG, "onCreate()");
        super.onCreate(bundle);
        this.mPreviousActivityIntent = (Intent) getIntent().getExtras().get(PREVIOUS_ACTIVITY_INTENT);
        if (bundle == null) {
            requestPermissions(this.mPreviousActivityIntent.getData());
        }
    }

    public static boolean startPermissionActivity(Activity activity) {
        Log.v(TAG, "startPermissionActivity from " + activity);
        boolean z = true;
        if (!hasPermissions(activity, activity.getIntent().getData())) {
            Intent intent = new Intent(activity, (Class<?>) RequestPermissionActivity.class);
            activity.getIntent().putExtra(STARTED_PERMISSIONS_ACTIVITY, true);
            intent.putExtra(PREVIOUS_ACTIVITY_INTENT, activity.getIntent());
            activity.startActivity(intent);
            activity.finish();
        } else {
            z = false;
        }
        Log.v(TAG, "startPermissionActivity return " + z);
        return z;
    }

    private static boolean hasPermissions(Context context, Uri uri) {
        boolean z = true;
        if (uri != null && !uri.toString().startsWith("content://media") && !uri.toString().startsWith("file")) {
            Log.d(TAG, "no need check permission, directly return");
            return true;
        }
        ArrayList<String> arrayList = new ArrayList();
        arrayList.add("android.permission.READ_EXTERNAL_STORAGE");
        arrayList.add("android.permission.WRITE_EXTERNAL_STORAGE");
        for (String str : arrayList) {
            if (ContextCompat.checkSelfPermission(context, str) != 0) {
                Log.v(TAG, "permission[" + str + "] is required");
                z = false;
            }
        }
        Log.v(TAG, "hasPermissions return " + z);
        return z;
    }

    private void requestPermissions(Uri uri) {
        Log.v(TAG, "requestPermissions  uri = " + uri);
        this.mPermissionsNeeded.clear();
        this.mPermissionsNeeded.add("android.permission.READ_EXTERNAL_STORAGE");
        this.mPermissionsNeeded.add("android.permission.WRITE_EXTERNAL_STORAGE");
        ArrayList arrayList = new ArrayList();
        for (String str : this.mPermissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, str) != 0) {
                arrayList.add(str);
            }
        }
        if (arrayList.size() == 0) {
            throw new RuntimeException("Request permission activity was called even though all permissions are satisfied.");
        }
        ActivityCompat.requestPermissions(this, (String[]) arrayList.toArray(new String[arrayList.size()]), 0);
    }

    private boolean isAllGranted(String[] strArr, int[] iArr) {
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < strArr.length) {
                if (iArr[i] != 0 && isPermissionRequired(strArr[i])) {
                    break;
                }
                i++;
            } else {
                z = true;
                break;
            }
        }
        Log.v(TAG, "isAllGranted, return " + z);
        return z;
    }

    private boolean isPermissionRequired(String str) {
        boolean zContains = this.mPermissionsNeeded.contains(str);
        Log.v(TAG, "isPermissionRequired, permission = " + str + ", ret = " + zContains);
        return zContains;
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (strArr != null && strArr.length > 0 && isAllGranted(strArr, iArr)) {
            Log.v(TAG, "onRequestPermissionsResult, start previous activity");
            this.mPreviousActivityIntent.setFlags(65536);
            startActivity(this.mPreviousActivityIntent);
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        Log.v(TAG, "onRequestPermissionsResult, deny request permission");
        try {
            Toast.makeText(this, RuntimePermissionUtils.getDeniedPermissionString(this), 0).show();
        } catch (Exception e) {
            Log.e(TAG, "resource 'denied_required_permission' not existed");
            e.printStackTrace();
        }
        finish();
    }
}
