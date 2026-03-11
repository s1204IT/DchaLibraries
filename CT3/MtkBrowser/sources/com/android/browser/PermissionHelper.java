package com.android.browser;

import android.app.Activity;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionHelper {
    private static final String[] ALL_PERMISSIONS = {"android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION", "android.permission.RECORD_AUDIO", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.GET_ACCOUNTS"};
    private static PermissionHelper sInstance;
    private Activity mActivity;
    private List<PermissionCallback> mListeners = new ArrayList();
    private Set<String> requestingPermissions = new HashSet();

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

    public void addListener(PermissionCallback listener) {
        if (this.mListeners.contains(listener)) {
            return;
        }
        this.mListeners.add(listener);
    }

    public void requestPermissions(List<String> permissionsRequestList, PermissionCallback listener) {
        Log.d("browser/PermissionHelper", " requestBrowserPermission start ......! " + permissionsRequestList.toString());
        if (permissionsRequestList.size() <= 0) {
            return;
        }
        addListener(listener);
        synchronized (this.requestingPermissions) {
            if (this.requestingPermissions.size() == 0) {
                this.mActivity.requestPermissions((String[]) permissionsRequestList.toArray(new String[permissionsRequestList.size()]), 1000);
            }
            this.requestingPermissions.addAll(permissionsRequestList);
        }
    }

    public List<String> getAllUngrantedPermissions() {
        List<String> permissionsRequestList = new ArrayList<>();
        for (int i = 0; i < ALL_PERMISSIONS.length; i++) {
            if (!checkPermission(ALL_PERMISSIONS[i])) {
                permissionsRequestList.add(ALL_PERMISSIONS[i]);
            }
        }
        return permissionsRequestList;
    }

    public List<String> getUngrantedPermissions(String[] permissions) {
        List<String> permissionsRequestList = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (!checkPermission(permissions[i])) {
                permissionsRequestList.add(permissions[i]);
            }
        }
        return permissionsRequestList;
    }

    public boolean checkPermission(String permission) {
        if (this.mActivity.checkSelfPermission(permission) != 0) {
            return false;
        }
        return true;
    }

    public void onPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("browser/PermissionHelper", " onPermissionsResult .. " + requestCode);
        synchronized (this.requestingPermissions) {
            for (int i = 0; i < this.mListeners.size(); i++) {
                this.mListeners.get(i).onPermissionsResult(requestCode, permissions, grantResults);
                for (String p : permissions) {
                    this.requestingPermissions.remove(p);
                }
            }
            Log.d("browser/PermissionHelper", " onPermissionsResult .. requestingPermissions.size() = " + this.requestingPermissions.size());
            if (this.requestingPermissions.size() == 0) {
                this.mListeners.clear();
            } else {
                Log.d("browser/PermissionHelper", " onPermissionsResult re-request ");
                this.mActivity.requestPermissions((String[]) this.requestingPermissions.toArray(new String[this.requestingPermissions.size()]), 1000);
            }
        }
    }
}
