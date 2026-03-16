package com.android.camera;

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.camera.app.AppController;
import com.android.camera.app.MotionManager;
import com.android.camera.debug.Log;
import com.android.camera.one.Settings3A;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsManager;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.UsageStatistics;
import com.android.ex.camera2.portability.CameraCapabilities;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FocusOverlayManager implements PreviewStatusListener.PreviewAreaChangedListener, MotionManager.MotionListener {
    private static final int RESET_TOUCH_FOCUS = 0;
    private static final int STATE_FAIL = 4;
    private static final int STATE_FOCUSING = 1;
    private static final int STATE_FOCUSING_SNAP_ON_FINISH = 2;
    private static final int STATE_IDLE = 0;
    private static final int STATE_SUCCESS = 3;
    private boolean mAeAwbLock;
    private final AppController mAppController;
    private CameraCapabilities mCapabilities;
    private final List<CameraCapabilities.FocusMode> mDefaultFocusModes;
    private int mDisplayOrientation;
    private List<Camera.Area> mFocusArea;
    private boolean mFocusAreaSupported;
    private boolean mFocusLocked;
    private CameraCapabilities.FocusMode mFocusMode;
    private final Handler mHandler;
    private boolean mInitialized;
    Listener mListener;
    private boolean mLockAeAwbNeeded;
    private List<Camera.Area> mMeteringArea;
    private boolean mMeteringAreaSupported;
    private boolean mMirror;
    private CameraCapabilities.FocusMode mOverrideFocusMode;
    private boolean mPreviousMoving;
    private final SettingsManager mSettingsManager;
    private TouchCoordinate mTouchCoordinate;
    private long mTouchTime;
    private final FocusUI mUI;
    private static final Log.Tag TAG = new Log.Tag("FocusOverlayMgr");
    private static final int RESET_TOUCH_FOCUS_DELAY_MILLIS = Settings3A.getFocusHoldMillis();
    public static final float AF_REGION_BOX = Settings3A.getAutoFocusRegionWidth();
    public static final float AE_REGION_BOX = Settings3A.getMeteringRegionWidth();
    private int mState = 0;
    private final Rect mPreviewRect = new Rect(0, 0, 0, 0);
    private final Matrix mMatrix = new Matrix();

    public interface FocusUI {
        void clearFocus();

        boolean hasFaces();

        void onFocusFailed();

        void onFocusStarted();

        void onFocusSucceeded();

        void pauseFaceDetection();

        void resumeFaceDetection();

        void setFocusPosition(int i, int i2, boolean z);

        void setFocusPosition(int i, int i2, boolean z, int i3, int i4);

        void setPassiveFocusSuccess(boolean z);

        void showDebugMessage(String str);
    }

    public interface Listener {
        void autoFocus();

        void cancelAutoFocus();

        boolean capture();

        void setFocusParameters();

        void startFaceDetection();

        void stopFaceDetection();
    }

    private static class MainHandler extends Handler {
        final WeakReference<FocusOverlayManager> mManager;

        public MainHandler(FocusOverlayManager manager, Looper looper) {
            super(looper);
            this.mManager = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            FocusOverlayManager manager = this.mManager.get();
            if (manager != null) {
                switch (msg.what) {
                    case 0:
                        manager.cancelAutoFocus();
                        manager.mListener.startFaceDetection();
                        break;
                }
            }
        }
    }

    public FocusOverlayManager(AppController appController, List<CameraCapabilities.FocusMode> defaultFocusModes, CameraCapabilities capabilities, Listener listener, boolean mirror, Looper looper, FocusUI ui) {
        this.mAppController = appController;
        this.mSettingsManager = appController.getSettingsManager();
        this.mHandler = new MainHandler(this, looper);
        this.mDefaultFocusModes = new ArrayList(defaultFocusModes);
        updateCapabilities(capabilities);
        this.mListener = listener;
        setMirror(mirror);
        this.mUI = ui;
        this.mFocusLocked = false;
    }

    public void updateCapabilities(CameraCapabilities capabilities) {
        if (capabilities != null) {
            this.mCapabilities = capabilities;
            this.mFocusAreaSupported = this.mCapabilities.supports(CameraCapabilities.Feature.FOCUS_AREA);
            this.mMeteringAreaSupported = this.mCapabilities.supports(CameraCapabilities.Feature.METERING_AREA);
            this.mLockAeAwbNeeded = this.mCapabilities.supports(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK) || this.mCapabilities.supports(CameraCapabilities.Feature.AUTO_WHITE_BALANCE_LOCK);
        }
    }

    public void setPreviewRect(Rect previewRect) {
        if (!this.mPreviewRect.equals(previewRect)) {
            this.mPreviewRect.set(previewRect);
            setMatrix();
        }
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        setPreviewRect(CameraUtil.rectFToRect(previewArea));
    }

    public Rect getPreviewRect() {
        return new Rect(this.mPreviewRect);
    }

    public void setMirror(boolean mirror) {
        this.mMirror = mirror;
        setMatrix();
    }

    public void setDisplayOrientation(int displayOrientation) {
        this.mDisplayOrientation = displayOrientation;
        setMatrix();
    }

    private void setMatrix() {
        if (this.mPreviewRect.width() != 0 && this.mPreviewRect.height() != 0) {
            Matrix matrix = new Matrix();
            CameraUtil.prepareMatrix(matrix, this.mMirror, this.mDisplayOrientation, getPreviewRect());
            matrix.invert(this.mMatrix);
            this.mInitialized = true;
        }
    }

    private void lockAeAwbIfNeeded() {
        if (this.mLockAeAwbNeeded && !this.mAeAwbLock) {
            this.mAeAwbLock = true;
            this.mListener.setFocusParameters();
        }
    }

    private void unlockAeAwbIfNeeded() {
        if (this.mLockAeAwbNeeded && this.mAeAwbLock && this.mState != 2) {
            this.mAeAwbLock = false;
            this.mListener.setFocusParameters();
        }
    }

    public void onShutterUp(CameraCapabilities.FocusMode currentFocusMode) {
        if (this.mInitialized) {
            if (needAutoFocusCall(currentFocusMode) && (this.mState == 1 || this.mState == 3 || this.mState == 4)) {
                cancelAutoFocus();
            }
            unlockAeAwbIfNeeded();
        }
    }

    public void focusAndCapture(CameraCapabilities.FocusMode currentFocusMode) {
        if (this.mInitialized) {
            if (!needAutoFocusCall(currentFocusMode)) {
                capture();
                return;
            }
            if (this.mState == 3 || this.mState == 4) {
                capture();
            } else if (this.mState == 1) {
                this.mState = 2;
            } else if (this.mState == 0) {
                autoFocusAndCapture();
            }
        }
    }

    public void onAutoFocus(boolean focused, boolean shutterButtonPressed) {
        if (this.mState == 2) {
            if (focused) {
                this.mState = 3;
            } else {
                this.mState = 4;
            }
            updateFocusUI();
            capture();
            return;
        }
        if (this.mState == 1) {
            if (focused) {
                this.mState = 3;
            } else {
                this.mState = 4;
            }
            updateFocusUI();
            if (this.mFocusArea != null) {
                this.mFocusLocked = true;
                this.mHandler.sendEmptyMessageDelayed(0, RESET_TOUCH_FOCUS_DELAY_MILLIS);
            }
            if (shutterButtonPressed) {
                lockAeAwbIfNeeded();
                return;
            }
            return;
        }
        if (this.mState == 0) {
        }
    }

    public void onAutoFocusMoving(boolean moving) {
        if (this.mInitialized) {
            if (this.mUI.hasFaces()) {
                this.mUI.clearFocus();
                return;
            }
            if (this.mState == 0) {
                if (moving && !this.mPreviousMoving) {
                    this.mUI.setFocusPosition(this.mPreviewRect.centerX(), this.mPreviewRect.centerY(), true, getAFRegionEdge(), getAERegionEdge());
                    this.mUI.onFocusStarted();
                } else if (!moving) {
                    this.mUI.onFocusSucceeded();
                }
                this.mPreviousMoving = moving;
            }
        }
    }

    private int getAFRegionEdge() {
        return (int) (Math.min(this.mPreviewRect.width(), this.mPreviewRect.height()) * AF_REGION_BOX);
    }

    private int getAERegionEdge() {
        return (int) (Math.min(this.mPreviewRect.width(), this.mPreviewRect.height()) * AE_REGION_BOX);
    }

    @TargetApi(14)
    private void initializeFocusAreas(int x, int y) {
        if (this.mFocusArea == null) {
            this.mFocusArea = new ArrayList();
            this.mFocusArea.add(new Camera.Area(new Rect(), 1));
        }
        calculateTapArea(x, y, getAFRegionEdge(), this.mFocusArea.get(0).rect);
    }

    @TargetApi(14)
    private void initializeMeteringAreas(int x, int y) {
        if (this.mMeteringArea == null) {
            this.mMeteringArea = new ArrayList();
            this.mMeteringArea.add(new Camera.Area(new Rect(), 1));
        }
        calculateTapArea(x, y, getAERegionEdge(), this.mMeteringArea.get(0).rect);
    }

    public void onSingleTapUp(int x, int y) {
        if (this.mInitialized && this.mState != 2) {
            if (this.mFocusArea != null && (this.mState == 1 || this.mState == 3 || this.mState == 4)) {
                cancelAutoFocus();
            }
            if (this.mPreviewRect.width() != 0 && this.mPreviewRect.height() != 0) {
                if (this.mFocusAreaSupported) {
                    initializeFocusAreas(x, y);
                }
                if (this.mMeteringAreaSupported) {
                    initializeMeteringAreas(x, y);
                }
                this.mUI.setFocusPosition(x, y, false, getAFRegionEdge(), getAERegionEdge());
                this.mTouchCoordinate = new TouchCoordinate(x, y, this.mPreviewRect.width(), this.mPreviewRect.height());
                this.mTouchTime = System.currentTimeMillis();
                this.mListener.stopFaceDetection();
                this.mListener.setFocusParameters();
                if (this.mFocusAreaSupported) {
                    autoFocus();
                    return;
                }
                updateFocusUI();
                this.mHandler.removeMessages(0);
                this.mHandler.sendEmptyMessageDelayed(0, RESET_TOUCH_FOCUS_DELAY_MILLIS);
            }
        }
    }

    public void onPreviewStarted() {
        this.mState = 0;
        resetTouchFocus();
    }

    public void onPreviewStopped() {
        this.mState = 0;
        updateFocusUI();
    }

    public void onCameraReleased() {
        onPreviewStopped();
    }

    @Override
    public void onMoving() {
        if (this.mFocusLocked) {
            Log.d(TAG, "onMoving: Early focus unlock.");
            cancelAutoFocus();
        }
    }

    private void autoFocus(int focusingState) {
        this.mListener.autoFocus();
        this.mState = focusingState;
        this.mUI.pauseFaceDetection();
        updateFocusUI();
        this.mHandler.removeMessages(0);
    }

    private void autoFocus() {
        autoFocus(1);
    }

    private void autoFocusAndCapture() {
        autoFocus(2);
    }

    private void cancelAutoFocus() {
        Log.v(TAG, "Cancel autofocus.");
        resetTouchFocus();
        this.mListener.cancelAutoFocus();
        this.mUI.resumeFaceDetection();
        this.mState = 0;
        this.mFocusLocked = false;
        updateFocusUI();
        this.mHandler.removeMessages(0);
    }

    private void capture() {
        if (this.mListener.capture()) {
            this.mState = 0;
            this.mHandler.removeMessages(0);
        }
    }

    public CameraCapabilities.FocusMode getFocusMode(CameraCapabilities.FocusMode currentFocusMode) {
        if (this.mOverrideFocusMode != null) {
            Log.v(TAG, "returning override focus: " + this.mOverrideFocusMode);
            return this.mOverrideFocusMode;
        }
        if (this.mCapabilities == null) {
            Log.v(TAG, "no capabilities, returning default AUTO focus mode");
            return CameraCapabilities.FocusMode.AUTO;
        }
        if (this.mFocusAreaSupported && this.mFocusArea != null) {
            Log.v(TAG, "in tap to focus, returning AUTO focus mode");
            this.mFocusMode = CameraCapabilities.FocusMode.AUTO;
        } else {
            String focusSetting = this.mSettingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_FOCUS_MODE);
            Log.v(TAG, "stored focus setting for camera: " + focusSetting);
            this.mFocusMode = this.mCapabilities.getStringifier().focusModeFromString(focusSetting);
            Log.v(TAG, "focus mode resolved from setting: " + this.mFocusMode);
            if (this.mFocusMode == null) {
                Iterator<CameraCapabilities.FocusMode> it = this.mDefaultFocusModes.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    CameraCapabilities.FocusMode mode = it.next();
                    if (this.mCapabilities.supports(mode)) {
                        this.mFocusMode = mode;
                        Log.v(TAG, "selected supported focus mode from default list" + mode);
                        break;
                    }
                }
            }
        }
        if (!this.mCapabilities.supports(this.mFocusMode)) {
            if (this.mCapabilities.supports(CameraCapabilities.FocusMode.AUTO)) {
                Log.v(TAG, "no supported focus mode, falling back to AUTO");
                this.mFocusMode = CameraCapabilities.FocusMode.AUTO;
            } else {
                Log.v(TAG, "no supported focus mode, falling back to current: " + currentFocusMode);
                this.mFocusMode = currentFocusMode;
            }
        }
        return this.mFocusMode;
    }

    public List<Camera.Area> getFocusAreas() {
        return this.mFocusArea;
    }

    public List<Camera.Area> getMeteringAreas() {
        return this.mMeteringArea;
    }

    public void updateFocusUI() {
        if (this.mInitialized) {
            if (this.mState == 0) {
                if (this.mFocusArea == null) {
                    this.mUI.clearFocus();
                    return;
                } else {
                    this.mUI.onFocusStarted();
                    return;
                }
            }
            if (this.mState == 1) {
                this.mUI.onFocusStarted();
                return;
            }
            if (this.mFocusMode == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                this.mUI.onFocusSucceeded();
            } else if (this.mState == 3) {
                this.mUI.onFocusSucceeded();
            } else if (this.mState == 4) {
                this.mUI.onFocusFailed();
            }
        }
    }

    public void resetTouchFocus() {
        if (this.mInitialized) {
            this.mUI.clearFocus();
            this.mFocusArea = null;
            this.mMeteringArea = null;
            this.mListener.setFocusParameters();
            if (this.mTouchCoordinate != null) {
                UsageStatistics.instance().tapToFocus(this.mTouchCoordinate, Float.valueOf(0.001f * (System.currentTimeMillis() - this.mTouchTime)));
                this.mTouchCoordinate = null;
            }
        }
    }

    private void calculateTapArea(int x, int y, int size, Rect rect) {
        int left = CameraUtil.clamp(x - (size / 2), this.mPreviewRect.left, this.mPreviewRect.right - size);
        int top = CameraUtil.clamp(y - (size / 2), this.mPreviewRect.top, this.mPreviewRect.bottom - size);
        RectF rectF = new RectF(left, top, left + size, top + size);
        this.mMatrix.mapRect(rectF);
        CameraUtil.rectFToRect(rectF, rect);
    }

    int getFocusState() {
        return this.mState;
    }

    public boolean isFocusCompleted() {
        return this.mState == 3 || this.mState == 4;
    }

    public boolean isFocusingSnapOnFinish() {
        return this.mState == 2;
    }

    public void removeMessages() {
        this.mHandler.removeMessages(0);
    }

    public void overrideFocusMode(CameraCapabilities.FocusMode focusMode) {
        this.mOverrideFocusMode = focusMode;
    }

    public void setAeAwbLock(boolean lock) {
        this.mAeAwbLock = lock;
    }

    public boolean getAeAwbLock() {
        return this.mAeAwbLock;
    }

    private boolean needAutoFocusCall(CameraCapabilities.FocusMode focusMode) {
        return (focusMode == CameraCapabilities.FocusMode.INFINITY || focusMode == CameraCapabilities.FocusMode.FIXED || focusMode == CameraCapabilities.FocusMode.EXTENDED_DOF) ? false : true;
    }
}
