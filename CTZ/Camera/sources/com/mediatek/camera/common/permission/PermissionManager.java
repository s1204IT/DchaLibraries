package com.mediatek.camera.common.permission;

import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PermissionManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PermissionManager.class.getSimpleName());
    private final Activity mActivity;
    private List<String> mAllPermissionList = new ArrayList();
    private List<String> mLaunchPermissionList = new ArrayList();
    private List<String> mLocationPermissionList = new ArrayList();

    public PermissionManager(Activity activity) {
        this.mActivity = activity;
        initCameraAllPermissionList();
        initCameraLaunchPermissionList();
        initCameraLocationPermissionList();
    }

    public boolean checkCameraLaunchPermissions() {
        if (getNeedCheckPermissionList(this.mLaunchPermissionList).size() > 0) {
            return false;
        }
        LogHelper.d(TAG, "CheckCameraPermissions(), all on");
        return true;
    }

    public boolean checkCameraLocationPermissions() {
        if (getNeedCheckPermissionList(this.mLocationPermissionList).size() > 0) {
            return false;
        }
        LogHelper.d(TAG, "checkCameraLocationPermissions(), all on");
        return true;
    }

    public boolean requestCameraAllPermissions() {
        List<String> needCheckPermissionList = getNeedCheckPermissionList(this.mAllPermissionList);
        if (needCheckPermissionList.size() > 0) {
            LogHelper.d(TAG, "requestCameraAllPermissions(), user check");
            ActivityCompat.requestPermissions(this.mActivity, (String[]) needCheckPermissionList.toArray(new String[needCheckPermissionList.size()]), 100);
            return false;
        }
        LogHelper.d(TAG, "requestCameraAllPermissions(), all on");
        return true;
    }

    public int getCameraLaunchPermissionRequestCode() {
        return 100;
    }

    public boolean isCameraLaunchPermissionsResultReady(String[] strArr, int[] iArr) {
        HashMap map = new HashMap();
        map.put("android.permission.CAMERA", 0);
        map.put("android.permission.RECORD_AUDIO", 0);
        map.put("android.permission.WRITE_EXTERNAL_STORAGE", 0);
        map.put("android.permission.READ_EXTERNAL_STORAGE", 0);
        for (int i = 0; i < strArr.length; i++) {
            map.put(strArr[i], Integer.valueOf(iArr[i]));
        }
        if (((Integer) map.get("android.permission.CAMERA")).intValue() != 0 || ((Integer) map.get("android.permission.RECORD_AUDIO")).intValue() != 0 || ((Integer) map.get("android.permission.WRITE_EXTERNAL_STORAGE")).intValue() != 0 || ((Integer) map.get("android.permission.READ_EXTERNAL_STORAGE")).intValue() != 0) {
            return false;
        }
        return true;
    }

    private void initCameraAllPermissionList() {
        this.mAllPermissionList.add("android.permission.CAMERA");
        this.mAllPermissionList.add("android.permission.RECORD_AUDIO");
        this.mAllPermissionList.add("android.permission.WRITE_EXTERNAL_STORAGE");
        this.mAllPermissionList.add("android.permission.READ_EXTERNAL_STORAGE");
        this.mAllPermissionList.add("android.permission.ACCESS_COARSE_LOCATION");
        this.mAllPermissionList.add("android.permission.ACCESS_FINE_LOCATION");
    }

    private void initCameraLaunchPermissionList() {
        this.mLaunchPermissionList.add("android.permission.CAMERA");
        this.mLaunchPermissionList.add("android.permission.RECORD_AUDIO");
        this.mLaunchPermissionList.add("android.permission.WRITE_EXTERNAL_STORAGE");
        this.mLaunchPermissionList.add("android.permission.READ_EXTERNAL_STORAGE");
    }

    private void initCameraLocationPermissionList() {
        this.mLocationPermissionList.add("android.permission.ACCESS_COARSE_LOCATION");
        this.mLocationPermissionList.add("android.permission.ACCESS_FINE_LOCATION");
    }

    private List<String> getNeedCheckPermissionList(List<String> list) {
        if (list.size() <= 0) {
            return list;
        }
        ArrayList arrayList = new ArrayList();
        for (String str : list) {
            if (ContextCompat.checkSelfPermission(this.mActivity, str) != 0) {
                LogHelper.d(TAG, "getNeedCheckPermissionList() permission =" + str);
                arrayList.add(str);
            }
        }
        LogHelper.d(TAG, "getNeedCheckPermissionList() listSize =" + arrayList.size());
        return arrayList;
    }
}
