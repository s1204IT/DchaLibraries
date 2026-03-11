package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SecureCameraLaunchManager {
    private CameraManager mCameraManager;
    private Context mContext;
    private KeyguardBottomAreaView mKeyguardBottomArea;
    private LockPatternUtils mLockPatternUtils;
    private Handler mHandler = new Handler();
    private CameraAvailabilityCallback mCameraAvailabilityCallback = new CameraAvailabilityCallback();
    private Map<String, Boolean> mCameraAvailabilityMap = new HashMap();
    private boolean mWaitingToLaunchSecureCamera = false;
    private Runnable mLaunchCameraRunnable = new Runnable() {
        @Override
        public void run() {
            if (SecureCameraLaunchManager.this.mWaitingToLaunchSecureCamera) {
                Log.w("SecureCameraLaunchManager", "Timeout waiting for camera availability");
                SecureCameraLaunchManager.this.mKeyguardBottomArea.launchCamera();
                SecureCameraLaunchManager.this.mWaitingToLaunchSecureCamera = false;
            }
        }
    };

    private class CameraAvailabilityCallback extends CameraManager.AvailabilityCallback {
        private CameraAvailabilityCallback() {
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            SecureCameraLaunchManager.this.mCameraAvailabilityMap.put(cameraId, false);
        }

        @Override
        public void onCameraAvailable(String cameraId) {
            SecureCameraLaunchManager.this.mCameraAvailabilityMap.put(cameraId, true);
            if (SecureCameraLaunchManager.this.mWaitingToLaunchSecureCamera && SecureCameraLaunchManager.this.areAllCamerasAvailable()) {
                SecureCameraLaunchManager.this.mKeyguardBottomArea.launchCamera();
                SecureCameraLaunchManager.this.mWaitingToLaunchSecureCamera = false;
                SecureCameraLaunchManager.this.mHandler.removeCallbacks(SecureCameraLaunchManager.this.mLaunchCameraRunnable);
            }
        }
    }

    public SecureCameraLaunchManager(Context context, KeyguardBottomAreaView keyguardBottomArea) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mKeyguardBottomArea = keyguardBottomArea;
        this.mCameraManager = (CameraManager) context.getSystemService("camera");
    }

    public void create() {
        this.mCameraManager.registerAvailabilityCallback(this.mCameraAvailabilityCallback, this.mHandler);
    }

    public void destroy() {
        this.mCameraManager.unregisterAvailabilityCallback(this.mCameraAvailabilityCallback);
    }

    public void onSwipingStarted() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setAction("com.android.systemui.statusbar.phone.CLOSE_CAMERA");
                intent.addFlags(67108864);
                SecureCameraLaunchManager.this.mContext.sendBroadcast(intent);
            }
        });
    }

    public void startSecureCameraLaunch() {
        if (areAllCamerasAvailable() || targetWillWaitForCameraAvailable()) {
            this.mKeyguardBottomArea.launchCamera();
        } else {
            this.mWaitingToLaunchSecureCamera = true;
            this.mHandler.postDelayed(this.mLaunchCameraRunnable, 1000L);
        }
    }

    public boolean areAllCamerasAvailable() {
        Iterator<Boolean> it = this.mCameraAvailabilityMap.values().iterator();
        while (it.hasNext()) {
            boolean cameraAvailable = it.next().booleanValue();
            if (!cameraAvailable) {
                return false;
            }
        }
        return true;
    }

    private boolean targetWillWaitForCameraAvailable() {
        ResolveInfo resolved;
        Intent intent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
        PackageManager packageManager = this.mContext.getPackageManager();
        List appList = packageManager.queryIntentActivitiesAsUser(intent, 65536, this.mLockPatternUtils.getCurrentUser());
        if (appList.size() == 0 || (resolved = packageManager.resolveActivityAsUser(intent, 65664, this.mLockPatternUtils.getCurrentUser())) == null || resolved.activityInfo == null || wouldLaunchResolverActivity(resolved, appList) || resolved.activityInfo.metaData == null || resolved.activityInfo.metaData.isEmpty()) {
            return false;
        }
        return resolved.activityInfo.metaData.getBoolean("com.android.systemui.statusbar.phone.will_wait_for_camera_available");
    }

    private boolean wouldLaunchResolverActivity(ResolveInfo resolved, List<ResolveInfo> appList) {
        for (int i = 0; i < appList.size(); i++) {
            ResolveInfo tmp = appList.get(i);
            if (tmp.activityInfo.name.equals(resolved.activityInfo.name) && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                return false;
            }
        }
        return true;
    }
}
