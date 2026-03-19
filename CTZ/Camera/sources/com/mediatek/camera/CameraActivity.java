package com.mediatek.camera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.IModeListener;
import com.mediatek.camera.common.mode.ModeManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.PriorityConcurrentSkipListMap;
import com.mediatek.camera.common.widget.RotateLayout;
import com.mediatek.camera.portability.pq.PictureQuality;
import com.mediatek.camera.ui.CameraAppUI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CameraActivity extends PermissionActivity implements IApp {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CameraActivity.class.getSimpleName());
    private CameraAppUI mCameraAppUI;
    private IModeListener mIModeListener;
    private boolean mIsResumed;
    private OrientationEventListener mOrientationListener;
    protected Uri mUri;
    private PriorityConcurrentSkipListMap<String, IApp.KeyEventListener> mKeyEventListeners = new PriorityConcurrentSkipListMap<>(true);
    private PriorityConcurrentSkipListMap<String, IApp.BackPressedListener> mBackPressedListeners = new PriorityConcurrentSkipListMap<>(true);
    private final List<IApp.OnOrientationChangeListener> mOnOrientationListeners = new ArrayList();
    private int mOrientation = 0;
    protected IAppUiListener.OnThumbnailClickedListener mThumbnailClickedListener = new IAppUiListener.OnThumbnailClickedListener() {
        @Override
        public void onThumbnailClicked() {
            CameraActivity.this.goToGallery(CameraActivity.this.mUri);
        }
    };
    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            LogHelper.d(CameraActivity.TAG, "handleMessage what = " + message.what + " arg1 = " + message.arg1);
            switch (message.what) {
                case 0:
                    CameraActivity.this.getWindow().clearFlags(128);
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    if (message.arg1 == 1) {
                        CameraActivity.this.keepScreenOn();
                    } else {
                        CameraActivity.this.keepScreenOnForAWhile();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onNewIntentTasks(Intent intent) {
        super.onNewIntentTasks(intent);
    }

    @Override
    protected void onCreateTasks(Bundle bundle) {
        if (!isThirdPartyIntent(this) && !isOpenFront(this)) {
            CameraUtil.launchCamera(this);
        }
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onCreate").start();
        super.onCreateTasks(bundle);
        getWindow().getDecorView().setSystemUiVisibility(1792);
        setContentView(R.layout.activity_main);
        this.mOrientationListener = new OrientationEventListenerImpl(this);
        this.mCameraAppUI = new CameraAppUI(this);
        iPerformanceProfileStart.mark("CameraAppUI initialized.");
        this.mCameraAppUI.onCreate();
        iPerformanceProfileStart.mark("CameraAppUI.onCreate done.");
        this.mIModeListener = new ModeManager();
        this.mIModeListener.create(this);
        iPerformanceProfileStart.mark("ModeManager.create done.");
        iPerformanceProfileStart.stop();
    }

    @Override
    protected void onStartTasks() {
        super.onStartTasks();
    }

    @Override
    protected void onResumeTasks() {
        CameraDeviceManagerFactory.setCurrentActivity(this);
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(TAG, "onResume").start();
        this.mIsResumed = true;
        this.mOrientationListener.enable();
        super.onResumeTasks();
        PictureQuality.enterCameraMode();
        this.mIModeListener.resume();
        iPerformanceProfileStart.mark("ModeManager resume done.");
        this.mCameraAppUI.onResume();
        iPerformanceProfileStart.mark("CameraAppUI resume done.");
        this.mCameraAppUI.setThumbnailClickedListener(this.mThumbnailClickedListener);
        keepScreenOnForAWhile();
        iPerformanceProfileStart.stop();
    }

    @Override
    protected void onPauseTasks() {
        this.mIsResumed = false;
        super.onPauseTasks();
        PictureQuality.exitCameraMode();
        this.mIModeListener.pause();
        this.mCameraAppUI.onPause();
        this.mOrientationListener.disable();
        resetScreenOn();
    }

    @Override
    protected void onStopTasks() {
        super.onStopTasks();
    }

    @Override
    protected void onDestroyTasks() {
        super.onDestroyTasks();
        this.mIModeListener.destroy();
        this.mCameraAppUI.onDestroy();
        CameraDeviceManagerFactory.release(this);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        RotateLayout rotateLayout = (RotateLayout) findViewById(R.id.app_ui);
        LogHelper.d(TAG, "onConfigurationChanged orientation = " + configuration.orientation);
        if (rotateLayout != null) {
            if (configuration.orientation == 1) {
                rotateLayout.setOrientation(90, false);
            } else if (configuration.orientation == 2) {
                rotateLayout.setOrientation(0, false);
            }
            this.mCameraAppUI.onConfigurationChanged(configuration);
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        Iterator<Map.Entry<String, IApp.KeyEventListener>> it = this.mKeyEventListeners.entrySet().iterator();
        while (it.hasNext()) {
            IApp.KeyEventListener value = it.next().getValue();
            if (value != null && value.onKeyDown(i, keyEvent)) {
                return true;
            }
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        Iterator<Map.Entry<String, IApp.KeyEventListener>> it = this.mKeyEventListeners.entrySet().iterator();
        while (it.hasNext()) {
            IApp.KeyEventListener value = it.next().getValue();
            if (value != null && value.onKeyUp(i, keyEvent)) {
                return true;
            }
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public void onBackPressed() {
        Iterator<Map.Entry<String, IApp.BackPressedListener>> it = this.mBackPressedListeners.entrySet().iterator();
        while (it.hasNext()) {
            IApp.BackPressedListener value = it.next().getValue();
            if (value != null && value.onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public void onUserInteraction() {
        if (this.mIModeListener == null || !this.mIModeListener.onUserInteraction()) {
            super.onUserInteraction();
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public IAppUi getAppUi() {
        return this.mCameraAppUI;
    }

    @Override
    public void enableKeepScreenOn(boolean z) {
        LogHelper.d(TAG, "enableKeepScreenOn enabled " + z);
        if (this.mIsResumed) {
            this.mMainHandler.removeMessages(1);
            Message messageObtain = Message.obtain();
            messageObtain.arg1 = z ? 1 : 0;
            messageObtain.what = 1;
            this.mMainHandler.sendMessage(messageObtain);
        }
    }

    @Override
    public void notifyNewMedia(Uri uri, boolean z) {
        this.mUri = uri;
    }

    @Override
    public boolean notifyCameraSelected(String str) {
        return this.mIModeListener.onCameraSelected(str);
    }

    @Override
    public void registerKeyEventListener(IApp.KeyEventListener keyEventListener, int i) {
        if (keyEventListener == null) {
            LogHelper.e(TAG, "registerKeyEventListener error [why null]");
        }
        PriorityConcurrentSkipListMap<String, IApp.KeyEventListener> priorityConcurrentSkipListMap = this.mKeyEventListeners;
        PriorityConcurrentSkipListMap<String, IApp.KeyEventListener> priorityConcurrentSkipListMap2 = this.mKeyEventListeners;
        priorityConcurrentSkipListMap.put(PriorityConcurrentSkipListMap.getPriorityKey(i, keyEventListener), keyEventListener);
    }

    @Override
    public void registerBackPressedListener(IApp.BackPressedListener backPressedListener, int i) {
        if (backPressedListener == null) {
            LogHelper.e(TAG, "registerKeyEventListener error [why null]");
        }
        PriorityConcurrentSkipListMap<String, IApp.BackPressedListener> priorityConcurrentSkipListMap = this.mBackPressedListeners;
        PriorityConcurrentSkipListMap<String, IApp.BackPressedListener> priorityConcurrentSkipListMap2 = this.mBackPressedListeners;
        priorityConcurrentSkipListMap.put(PriorityConcurrentSkipListMap.getPriorityKey(i, backPressedListener), backPressedListener);
    }

    @Override
    public void unRegisterKeyEventListener(IApp.KeyEventListener keyEventListener) {
        if (keyEventListener == null) {
            LogHelper.e(TAG, "unRegisterKeyEventListener error [why null]");
        }
        if (this.mKeyEventListeners.containsValue(keyEventListener)) {
            this.mKeyEventListeners.remove(this.mKeyEventListeners.findKey(keyEventListener));
        }
    }

    @Override
    public void unRegisterBackPressedListener(IApp.BackPressedListener backPressedListener) {
        if (backPressedListener == null) {
            LogHelper.e(TAG, "unRegisterBackPressedListener error [why null]");
        }
        if (this.mBackPressedListeners.containsValue(backPressedListener)) {
            this.mBackPressedListeners.remove(this.mBackPressedListeners.findKey(backPressedListener));
        }
    }

    @Override
    public void registerOnOrientationChangeListener(IApp.OnOrientationChangeListener onOrientationChangeListener) {
        synchronized (this.mOnOrientationListeners) {
            if (!this.mOnOrientationListeners.contains(onOrientationChangeListener)) {
                if (this.mOrientation != -1) {
                    onOrientationChangeListener.onOrientationChanged(this.mOrientation);
                }
                this.mOnOrientationListeners.add(onOrientationChangeListener);
            }
        }
    }

    @Override
    public void unregisterOnOrientationChangeListener(IApp.OnOrientationChangeListener onOrientationChangeListener) {
        synchronized (this.mOnOrientationListeners) {
            if (this.mOnOrientationListeners.contains(onOrientationChangeListener)) {
                this.mOnOrientationListeners.remove(onOrientationChangeListener);
            }
        }
    }

    @Override
    public int getGSensorOrientation() {
        int i;
        synchronized (this.mOnOrientationListeners) {
            i = this.mOrientation;
        }
        return i;
    }

    @Override
    public void enableGSensorOrientation() {
        if (this.mIsResumed) {
            this.mOrientationListener.enable();
        }
    }

    @Override
    public void disableGSensorOrientation() {
        this.mOrientationListener.disable();
    }

    protected void goToGallery(Uri uri) {
        if (uri == null) {
            LogHelper.d(TAG, "uri is null, can not go to gallery");
            return;
        }
        String type = getContentResolver().getType(uri);
        LogHelper.d(TAG, "[goToGallery] uri: " + uri + ", mimeType = " + type);
        Intent intent = new Intent("com.android.camera.action.REVIEW");
        intent.setDataAndType(uri, type);
        intent.putExtra("isCamera", true);
        ActivityManager activityManager = (ActivityManager) getSystemService("activity");
        if (Build.VERSION.SDK_INT >= 23 && 2 == activityManager.getLockTaskModeState()) {
            intent.addFlags(134742016);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogHelper.e(TAG, "[startGalleryActivity] Couldn't view ", e);
        }
    }

    private void resetScreenOn() {
        this.mMainHandler.removeMessages(1);
        this.mMainHandler.removeMessages(0);
        getWindow().clearFlags(128);
    }

    private void keepScreenOnForAWhile() {
        this.mMainHandler.removeMessages(0);
        getWindow().addFlags(128);
        this.mMainHandler.sendEmptyMessageDelayed(0, 120000L);
    }

    private void keepScreenOn() {
        this.mMainHandler.removeMessages(0);
        getWindow().addFlags(128);
    }

    private class OrientationEventListenerImpl extends OrientationEventListener {
        public OrientationEventListenerImpl(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int i) {
            if (i != -1) {
                synchronized (CameraActivity.this.mOnOrientationListeners) {
                    int iRoundOrientation = CameraActivity.roundOrientation(i, CameraActivity.this.mOrientation);
                    if (CameraActivity.this.mOrientation != iRoundOrientation) {
                        CameraActivity.this.mOrientation = iRoundOrientation;
                        LogHelper.i(CameraActivity.TAG, "mOrientation = " + CameraActivity.this.mOrientation);
                        Iterator it = CameraActivity.this.mOnOrientationListeners.iterator();
                        while (it.hasNext()) {
                            ((IApp.OnOrientationChangeListener) it.next()).onOrientationChanged(CameraActivity.this.mOrientation);
                        }
                    }
                }
            }
        }
    }

    private static int roundOrientation(int i, int i2) {
        boolean z = true;
        if (i2 != -1) {
            int iAbs = Math.abs(i - i2);
            if (Math.min(iAbs, 360 - iAbs) < 50) {
                z = false;
            }
        }
        if (z) {
            return (((i + 45) / 90) * 90) % 360;
        }
        return i2;
    }

    private boolean isThirdPartyIntent(Activity activity) {
        String action = activity.getIntent().getAction();
        return "android.media.action.IMAGE_CAPTURE".equals(action) || "android.media.action.IMAGE_CAPTURE_SECURE".equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action);
    }

    private boolean isOpenFront(Activity activity) {
        Intent intent = activity.getIntent();
        return intent.getBooleanExtra("android.intent.extra.USE_FRONT_CAMERA", false) || intent.getBooleanExtra("com.google.assistant.extra.USE_FRONT_CAMERA", false);
    }
}
