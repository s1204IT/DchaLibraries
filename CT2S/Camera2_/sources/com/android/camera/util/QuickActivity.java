package com.android.camera.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.android.camera.debug.Log;

public abstract class QuickActivity extends Activity {
    private static final long ON_RESUME_DELAY_MILLIS = 20;
    private static final Log.Tag TAG = new Log.Tag("QuickActivity");
    private Handler mMainHandler;
    private boolean mPaused;
    private boolean mCanceledResumeTasks = false;
    private final Runnable mOnResumeTasks = new Runnable() {
        @Override
        public void run() {
            QuickActivity.this.logLifecycle("onResumeTasks", true);
            if (QuickActivity.this.mPaused) {
                QuickActivity.this.onResumeTasks();
                QuickActivity.this.mPaused = false;
                QuickActivity.this.mCanceledResumeTasks = false;
            }
            QuickActivity.this.logLifecycle("onResumeTasks", false);
        }
    };

    @Override
    protected final void onNewIntent(Intent intent) {
        logLifecycle("onNewIntent", true);
        Log.v(TAG, "Intent Action = " + intent.getAction());
        setIntent(intent);
        super.onNewIntent(intent);
        onNewIntentTasks(intent);
        logLifecycle("onNewIntent", false);
    }

    @Override
    protected final void onCreate(Bundle bundle) {
        logLifecycle("onCreate", true);
        Log.v(TAG, "Intent Action = " + getIntent().getAction());
        super.onCreate(bundle);
        this.mMainHandler = new Handler(getMainLooper());
        onCreateTasks(bundle);
        this.mPaused = true;
        logLifecycle("onCreate", false);
    }

    @Override
    protected final void onStart() {
        logLifecycle("onStart", true);
        onStartTasks();
        super.onStart();
        logLifecycle("onStart", false);
    }

    @Override
    protected final void onResume() {
        logLifecycle("onResume", true);
        this.mMainHandler.removeCallbacks(this.mOnResumeTasks);
        if (delayOnResumeOnStart() && !this.mCanceledResumeTasks) {
            this.mMainHandler.postDelayed(this.mOnResumeTasks, ON_RESUME_DELAY_MILLIS);
        } else if (this.mPaused) {
            onResumeTasks();
            this.mPaused = false;
            this.mCanceledResumeTasks = false;
        }
        super.onResume();
        logLifecycle("onResume", false);
    }

    @Override
    protected final void onPause() {
        logLifecycle("onPause", true);
        this.mMainHandler.removeCallbacks(this.mOnResumeTasks);
        if (!this.mPaused) {
            onPauseTasks();
            this.mPaused = true;
        } else {
            this.mCanceledResumeTasks = true;
        }
        super.onPause();
        logLifecycle("onPause", false);
    }

    @Override
    protected final void onStop() {
        if (isChangingConfigurations()) {
            Log.v(TAG, "changing configurations");
        }
        logLifecycle("onStop", true);
        onStopTasks();
        super.onStop();
        logLifecycle("onStop", false);
    }

    @Override
    protected final void onRestart() {
        logLifecycle("onRestart", true);
        super.onRestart();
        logLifecycle("onRestart", false);
    }

    @Override
    protected final void onDestroy() {
        logLifecycle("onDestroy", true);
        onDestroyTasks();
        super.onDestroy();
        logLifecycle("onDestroy", false);
    }

    private void logLifecycle(String methodName, boolean start) {
        String prefix = start ? "START" : "END";
        Log.v(TAG, prefix + " " + methodName + ": Activity = " + toString());
    }

    private boolean delayOnResumeOnStart() {
        String action = getIntent().getAction();
        boolean isSecureLockscreenCamera = "android.media.action.STILL_IMAGE_CAMERA_SECURE".equals(action);
        return isSecureLockscreenCamera;
    }

    protected void onNewIntentTasks(Intent newIntent) {
    }

    protected void onCreateTasks(Bundle savedInstanceState) {
    }

    protected void onStartTasks() {
    }

    protected void onResumeTasks() {
    }

    protected void onPauseTasks() {
    }

    protected void onStopTasks() {
    }

    protected void onDestroyTasks() {
    }
}
