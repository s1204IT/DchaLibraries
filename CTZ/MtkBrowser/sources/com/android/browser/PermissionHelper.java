package com.android.browser;

import android.app.Activity;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionHelper {
    private static PermissionHelper sInstance;
    private Activity mActivity;
    private List<PermissionCallback> mListeners = new ArrayList();
    private Set<String> requestingPermissions = new HashSet();
    private static final boolean DEBUG = Browser.ENGONLY;
    private static final String[] ALL_PERMISSIONS = {"android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION", "android.permission.RECORD_AUDIO", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.GET_ACCOUNTS"};

    public interface PermissionCallback {
        void onPermissionsResult(int i, String[] strArr, int[] iArr);
    }

    public static PermissionHelper getInstance() {
        return sInstance;
    }

    public static void init(Activity activity) {
        sInstance = new PermissionHelper(activity);
    }

    private PermissionHelper(Activity activity) {
        this.mActivity = activity;
    }

    public void addListener(PermissionCallback permissionCallback) {
        if (!this.mListeners.contains(permissionCallback)) {
            this.mListeners.add(permissionCallback);
        }
    }

    public void requestPermissions(List<String> list, PermissionCallback permissionCallback) {
        if (DEBUG) {
            Log.d("browser/PermissionHelper", "requestBrowserPermission start...! " + list.toString());
        }
        if (list.size() > 0) {
            addListener(permissionCallback);
            synchronized (this.requestingPermissions) {
                if (this.requestingPermissions.size() == 0) {
                    this.mActivity.requestPermissions((String[]) list.toArray(new String[list.size()]), 1000);
                }
                this.requestingPermissions.addAll(list);
            }
        }
    }

    public List<String> getAllUngrantedPermissions() {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < ALL_PERMISSIONS.length; i++) {
            if (!checkPermission(ALL_PERMISSIONS[i])) {
                arrayList.add(ALL_PERMISSIONS[i]);
            }
        }
        return arrayList;
    }

    public List<String> getUngrantedPermissions(String[] strArr) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < strArr.length; i++) {
            if (!checkPermission(strArr[i])) {
                arrayList.add(strArr[i]);
            }
        }
        return arrayList;
    }

    public boolean checkPermission(String str) {
        if (this.mActivity.checkSelfPermission(str) == 0) {
            return true;
        }
        return false;
    }

    public void onPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (DEBUG) {
            Log.d("browser/PermissionHelper", " onPermissionsResult .. " + i);
        }
        synchronized (this.requestingPermissions) {
            for (int i2 = 0; i2 < this.mListeners.size(); i2++) {
                this.mListeners.get(i2).onPermissionsResult(i, strArr, iArr);
                for (String str : strArr) {
                    this.requestingPermissions.remove(str);
                }
            }
            if (DEBUG) {
                Log.d("browser/PermissionHelper", " onPermissionsResult .. requestingPermissions.size() = " + this.requestingPermissions.size());
            }
            if (this.requestingPermissions.size() == 0) {
                this.mListeners.clear();
            } else {
                Log.d("browser/PermissionHelper", " onPermissionsResult re-request ");
                this.mActivity.requestPermissions((String[]) this.requestingPermissions.toArray(new String[this.requestingPermissions.size()]), 1000);
            }
        }
    }
}
