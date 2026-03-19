package com.mediatek.camera;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.utils.CameraUtil;

public abstract class QuickActivity extends AppCompatActivity {
    private static final LogUtil.Tag TAG;
    private Handler mMainHandler;
    private boolean mSkippedFirstOnResume = false;
    protected boolean mStartupOnCreate = false;
    private KeyguardManager mKeyguardManager = null;
    private final Runnable mOnResumeTasks = new Runnable() {
        @Override
        public void run() {
            if (QuickActivity.this.mSkippedFirstOnResume) {
                LogHelper.d(QuickActivity.TAG, "delayed Runnable --> onPermissionResumeTasks()");
                QuickActivity.this.mSkippedFirstOnResume = false;
                QuickActivity.this.onPermissionResumeTasks();
            }
        }
    };

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        TAG = new LogUtil.Tag(QuickActivity.class.getSimpleName());
    }

    @Override
    protected final void onNewIntent(Intent intent) {
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onNewIntent").start();
        setIntent(intent);
        super.onNewIntent(intent);
        onNewIntentTasks(intent);
        iPerformanceProfileStart.stop();
    }

    @Override
    protected final void onCreate(Bundle bundle) {
        LogHelper.i(TAG, "onCreate()");
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onCreate").start();
        this.mStartupOnCreate = true;
        super.onCreate(bundle);
        this.mMainHandler = new Handler(getMainLooper());
        onPermissionCreateTasks(bundle);
        iPerformanceProfileStart.stop();
    }

    @Override
    protected final void onStart() {
        LogHelper.i(TAG, "onStart()");
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onStart").start();
        onPermissionStartTasks();
        super.onStart();
        iPerformanceProfileStart.stop();
    }

    @Override
    protected final void onResume() {
        LogHelper.i(TAG, "onResume()");
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onResume").start();
        startPreWarmService();
        this.mMainHandler.removeCallbacks(this.mOnResumeTasks);
        if (isKeyguardLocked() && !this.mSkippedFirstOnResume) {
            this.mSkippedFirstOnResume = true;
            long j = isKeyguardSecure() ? 30L : 15L;
            LogHelper.d(TAG, "onResume() --> postDelayed(mOnResumeTasks," + j + ")");
            this.mMainHandler.postDelayed(this.mOnResumeTasks, j);
        } else {
            LogHelper.d(TAG, "onResume --> onPermissionResumeTasks()");
            this.mSkippedFirstOnResume = false;
            onPermissionResumeTasks();
        }
        super.onResume();
        iPerformanceProfileStart.stop();
    }

    @Override
    protected final void onPause() {
        LogHelper.i(TAG, "onPause()");
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onPause").start();
        this.mMainHandler.removeCallbacks(this.mOnResumeTasks);
        if (!this.mSkippedFirstOnResume) {
            LogHelper.d(TAG, "onPause --> onPermissionPauseTasks()");
            onPermissionPauseTasks();
        }
        super.onPause();
        this.mStartupOnCreate = false;
        iPerformanceProfileStart.stop();
    }

    @Override
    protected final void onStop() {
        LogHelper.i(TAG, "onStop()");
        if (isChangingConfigurations()) {
            LogHelper.d(TAG, "changing configurations");
        }
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onStop").start();
        onPermissionStopTasks();
        super.onStop();
        iPerformanceProfileStart.stop();
    }

    @Override
    protected final void onRestart() {
        LogHelper.i(TAG, "onRestart()");
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onRestart").start();
        super.onRestart();
        iPerformanceProfileStart.stop();
    }

    @Override
    protected final void onDestroy() {
        LogHelper.i(TAG, "onDestroy()");
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onDestroy").start();
        onPermissionDestroyTasks();
        super.onDestroy();
        iPerformanceProfileStart.stop();
    }

    protected boolean isKeyguardLocked() {
        boolean zIsKeyguardLocked;
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService("keyguard");
        if (keyguardManager != null) {
            zIsKeyguardLocked = keyguardManager.isKeyguardLocked();
        } else {
            zIsKeyguardLocked = false;
        }
        LogHelper.d(TAG, "isKeyguardLocked = " + zIsKeyguardLocked);
        return zIsKeyguardLocked;
    }

    protected boolean isKeyguardSecure() {
        boolean zIsKeyguardSecure;
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService("keyguard");
        if (keyguardManager != null) {
            zIsKeyguardSecure = keyguardManager.isKeyguardSecure();
        } else {
            zIsKeyguardSecure = false;
        }
        LogHelper.d(TAG, "isKeyguardSecure = " + zIsKeyguardSecure);
        return zIsKeyguardSecure;
    }

    protected void onNewIntentTasks(Intent intent) {
    }

    protected void onPermissionCreateTasks(Bundle bundle) {
    }

    protected void onPermissionStartTasks() {
    }

    protected void onPermissionResumeTasks() {
    }

    protected void onPermissionPauseTasks() {
    }

    protected void onPermissionStopTasks() {
    }

    protected void onPermissionDestroyTasks() {
    }

    private void startPreWarmService() {
        if (!CameraUtil.isServiceRun(getApplicationContext(), "com.mediatek.camera.CameraAppService")) {
            CameraUtil.startService(getApplicationContext(), new Intent(getApplicationContext(), (Class<?>) CameraAppService.class));
        }
    }
}
