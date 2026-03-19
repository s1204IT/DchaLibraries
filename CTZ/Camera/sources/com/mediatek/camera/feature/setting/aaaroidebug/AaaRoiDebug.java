package com.mediatek.camera.feature.setting.aaaroidebug;

import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.CoordinatesTransform;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.setting.aaaroidebug.AaaRoiDebugCaptureRequestConfig;

public class AaaRoiDebug extends SettingBase implements IAppUiListener.OnPreviewAreaChangedListener, IApp.OnOrientationChangeListener, AaaRoiDebugCaptureRequestConfig.DebugInfoListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AaaRoiDebug.class.getSimpleName());
    private Rect[][] mAaaRois;
    private Object mAaaRoisLock = new Object();
    private AaaRoiDebugCaptureRequestConfig mCaptureRequestConfig;
    private FrameLayout mDebugLayout;
    private int mDisplayOrientation;
    private Handler mMainHandler;
    private boolean mMirror;
    private Handler mModeHandler;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private ColorRectView[] mRectViews;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        synchronized (this.mAaaRoisLock) {
            this.mAaaRois = new Rect[3][];
        }
        this.mModeHandler = new Handler(Looper.myLooper());
        this.mMainHandler = new MainHandler(iApp.getActivity().getMainLooper());
        this.mMainHandler.sendMessage(this.mMainHandler.obtainMessage(1));
        this.mApp.registerOnOrientationChangeListener(this);
        this.mApp.getAppUi().registerOnPreviewAreaChangedListener(this);
        updateDisplayOrientation();
    }

    @Override
    public void unInit() {
        this.mApp.unregisterOnOrientationChangeListener(this);
        this.mApp.getAppUi().unregisterOnPreviewAreaChangedListener(this);
        this.mMainHandler.sendMessage(this.mMainHandler.obtainMessage(2));
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_3a_roi_debug";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        return null;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mCaptureRequestConfig == null) {
            this.mCaptureRequestConfig = new AaaRoiDebugCaptureRequestConfig();
            this.mCaptureRequestConfig.setDebugInfoListener(this);
        }
        return this.mCaptureRequestConfig;
    }

    @Override
    public void onRangeUpdate(Rect[] rectArr, Rect[] rectArr2, Rect[] rectArr3, Rect rect) {
        calculateViewRect(new Rect[][]{rectArr, rectArr2, rectArr3}, rect);
        if (!this.mMainHandler.hasMessages(3)) {
            this.mMainHandler.sendMessage(this.mMainHandler.obtainMessage(3));
        }
    }

    @Override
    public void onOrientationChanged(int i) {
        this.mModeHandler.post(new Runnable() {
            @Override
            public void run() {
                AaaRoiDebug.this.updateDisplayOrientation();
            }
        });
    }

    @Override
    public void onPreviewAreaChanged(final RectF rectF, Size size) {
        this.mModeHandler.post(new Runnable() {
            @Override
            public void run() {
                AaaRoiDebug.this.mPreviewWidth = Math.abs(((int) rectF.right) - ((int) rectF.left));
                AaaRoiDebug.this.mPreviewHeight = Math.abs(((int) rectF.top) - ((int) rectF.bottom));
            }
        });
    }

    class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    AaaRoiDebug.this.initView();
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    AaaRoiDebug.this.unInitView();
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    AaaRoiDebug.this.updateViewRect();
                    break;
            }
        }
    }

    private void initView() {
        this.mDebugLayout = (FrameLayout) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.aaaroidebug_view, (ViewGroup) this.mAppUi.getPreviewFrameLayout(), true);
        this.mRectViews = new ColorRectView[3];
        this.mRectViews[0] = (ColorRectView) this.mDebugLayout.findViewById(R.id.aeRect);
        this.mRectViews[1] = (ColorRectView) this.mDebugLayout.findViewById(R.id.afRect);
        this.mRectViews[2] = (ColorRectView) this.mDebugLayout.findViewById(R.id.awbRect);
    }

    private void unInitView() {
        this.mAppUi.getPreviewFrameLayout().removeViewInLayout(this.mDebugLayout);
    }

    private void calculateViewRect(Rect[][] rectArr, Rect rect) {
        for (int i = 0; i < rectArr.length; i++) {
            if (rectArr[i] == null || rectArr[i].length == 0) {
                synchronized (this.mAaaRoisLock) {
                    this.mAaaRois[i] = null;
                }
            } else {
                Rect[] rectArr2 = new Rect[rectArr[i].length];
                for (int i2 = 0; i2 < rectArr[i].length; i2++) {
                    rectArr2[i2] = CoordinatesTransform.normalizedPreviewToUi(CoordinatesTransform.sensorToNormalizedPreview(rectArr[i][i2], this.mPreviewWidth, this.mPreviewHeight, rect), this.mPreviewWidth, this.mPreviewHeight, this.mDisplayOrientation, this.mMirror);
                }
                synchronized (this.mAaaRoisLock) {
                    this.mAaaRois[i] = rectArr2;
                }
            }
        }
    }

    private void updateViewRect() {
        synchronized (this.mAaaRoisLock) {
            for (int i = 0; i < this.mAaaRois.length; i++) {
                this.mRectViews[i].setRects(this.mAaaRois[i]);
            }
        }
    }

    private void updateDisplayOrientation() {
        this.mDisplayOrientation = CameraUtil.getDisplayOrientationFromDeviceSpec(CameraUtil.getDisplayRotation(this.mApp.getActivity()), Integer.valueOf(this.mSettingController.getCameraId()).intValue(), this.mApp.getActivity());
        this.mMirror = Integer.valueOf(this.mSettingController.getCameraId()).intValue() == 1;
    }
}
