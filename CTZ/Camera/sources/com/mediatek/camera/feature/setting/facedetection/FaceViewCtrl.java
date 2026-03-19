package com.mediatek.camera.feature.setting.facedetection;

import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.widget.PreviewFrameLayout;
import com.mediatek.camera.portability.SystemProperties;

public class FaceViewCtrl implements StatusMonitor.StatusChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FaceViewCtrl.class.getSimpleName());
    private IApp mApp;
    private IAppUi mAppUi;
    private Animation mFaceExitAnim;
    private FrameLayout mFaceLayout;
    private int mFaceNum;
    private FaceView mFaceView;
    private MainHandler mMainHandler;
    private PreviewFrameLayout mRootViewGroup;
    private boolean mIsEnable = true;
    private boolean mHideViewWhenFaceCountNotChange = true;
    private FaceViewState mFaceViewState = FaceViewState.STATE_UNINIT;
    private WaitFocusState mWaitFocusState = WaitFocusState.WAIT_NOTHING;

    private enum FaceViewState {
        STATE_INIT,
        STATE_UNINIT
    }

    private enum WaitFocusState {
        WAIT_PASSIVE_SCAN,
        WAIT_PASSIVE_DONE,
        WAIT_NOTHING
    }

    @Override
    public void onStatusChanged(String str, String str2) {
        if (!str.equals("key_focus_state")) {
            return;
        }
        if (str2.equals("PASSIVE_SCAN") || str2.equals("PASSIVE_FOCUSED") || str2.equals("PASSIVE_UNFOCUSED")) {
            this.mMainHandler.obtainMessage(7, str2).sendToTarget();
        }
    }

    public void init(IApp iApp) {
        this.mApp = iApp;
        this.mAppUi = iApp.getAppUi();
        this.mMainHandler = new MainHandler(iApp.getActivity().getMainLooper());
        this.mMainHandler.sendEmptyMessage(0);
        this.mFaceExitAnim = AnimationUtils.loadAnimation(iApp.getActivity(), R.anim.face_exit);
        if (SystemProperties.getInt("vendor.mtk.camera.app.3a.debug", 0) == 1) {
            LogHelper.d(TAG, "[init] roi debug mode, set mHideViewWhenFaceCountNotChange = false");
            this.mHideViewWhenFaceCountNotChange = false;
        }
    }

    public void unInit() {
        this.mMainHandler.sendEmptyMessage(1);
    }

    public void onPreviewAreaChanged(RectF rectF) {
        this.mMainHandler.obtainMessage(4, rectF).sendToTarget();
    }

    public void enableFaceView(boolean z) {
        this.mIsEnable = z;
        if (!z) {
            if (this.mFaceView != null) {
                this.mFaceView.resetReallyShown();
            }
            this.mMainHandler.sendEmptyMessage(2);
        }
    }

    public void onDetectedFaceUpdate(Face[] faceArr) {
        if (!this.mIsEnable) {
            return;
        }
        if (faceArr != null && faceArr.length > 0) {
            this.mMainHandler.obtainMessage(6, faceArr).sendToTarget();
        } else {
            this.mMainHandler.obtainMessage(3).sendToTarget();
        }
    }

    public void onPreviewStatus(boolean z) {
        if (!z) {
            this.mMainHandler.sendEmptyMessage(2);
        }
    }

    public void updateFaceDisplayOrientation(int i, int i2) {
        this.mMainHandler.obtainMessage(5, i, i2).sendToTarget();
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    FaceViewCtrl.this.initFaceView();
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    FaceViewCtrl.this.unInitFaceView();
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    FaceViewCtrl.this.hideView();
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    FaceViewCtrl.this.mFaceNum = 0;
                    FaceViewCtrl.this.hideView();
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    FaceViewCtrl.this.setFaceViewPreviewSize((RectF) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    FaceViewCtrl.this.setFaceViewDisplayOrientation(message.arg1, message.arg2);
                    break;
                case Camera2Proxy.TEMPLATE_MANUAL:
                    FaceViewCtrl.this.updateFacesViewByFace((Face[]) message.obj);
                    break;
                case 7:
                    FaceViewCtrl.this.updateFacesViewByFocus((String) message.obj);
                    break;
            }
        }
    }

    private void setFaceViewDisplayOrientation(int i, int i2) {
        updateViewDisplayOrientation(i, i2);
    }

    private void setFaceViewPreviewSize(RectF rectF) {
        updateViewPreviewSize(Math.abs(((int) rectF.right) - ((int) rectF.left)), Math.abs(((int) rectF.top) - ((int) rectF.bottom)));
    }

    private void updateFacesViewByFace(Face[] faceArr) {
        if (!this.mIsEnable) {
            LogHelper.d(TAG, "[updateFacesViewByFace] mIsEnable is false, ignore this time");
            return;
        }
        if (faceArr != null && faceArr.length > 0 && this.mFaceViewState == FaceViewState.STATE_INIT) {
            if (this.mHideViewWhenFaceCountNotChange && faceArr.length == this.mFaceNum && this.mFaceView.hasReallyShown()) {
                if (this.mFaceView.getVisibility() != 0) {
                    this.mMainHandler.removeMessages(2);
                    this.mWaitFocusState = WaitFocusState.WAIT_NOTHING;
                } else if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.removeMessages(2);
                    LogHelper.d(TAG, "[updateFacesViewByFace] new face num = " + faceArr.length + ", clear hide msg, send hide msg delay 1500 ms");
                    this.mMainHandler.sendEmptyMessageDelayed(2, 1500L);
                }
            } else {
                LogHelper.d(TAG, "[updateFacesViewByFace] new face num = " + faceArr.length + ", clear hide msg, show view right now");
                this.mMainHandler.removeMessages(2);
                this.mWaitFocusState = WaitFocusState.WAIT_PASSIVE_SCAN;
                showView();
                this.mFaceView.resetReallyShown();
            }
            this.mFaceView.setFaces(faceArr);
            this.mFaceNum = faceArr.length;
        }
    }

    private void updateFacesViewByFocus(String str) {
        LogHelper.d(TAG, "[updateFacesViewByFocus] enter, focusState = " + str + ", mWaitFocusState = " + this.mWaitFocusState);
        if (!this.mIsEnable) {
            LogHelper.d(TAG, "[updateFacesViewByFocus] mIsEnable is false, ignore this time");
            return;
        }
        if (this.mFaceNum <= 0) {
            LogHelper.d(TAG, "[updateFacesViewByFocus] face num <= 0, ignore this time");
            return;
        }
        if (this.mFaceViewState != FaceViewState.STATE_INIT) {
            LogHelper.d(TAG, "[updateFacesViewByFocus] face view not init, ignore this time");
            return;
        }
        if (str.equals("PASSIVE_SCAN") && this.mWaitFocusState == WaitFocusState.WAIT_PASSIVE_SCAN) {
            this.mWaitFocusState = WaitFocusState.WAIT_PASSIVE_DONE;
            LogHelper.d(TAG, "[updateFacesViewByFocus] clear hide msg, send hide msg delay 3000 ms");
            this.mMainHandler.removeMessages(2);
            this.mMainHandler.sendEmptyMessageDelayed(2, 3000L);
        } else if ((str.equals("PASSIVE_FOCUSED") || str.equals("PASSIVE_UNFOCUSED")) && this.mWaitFocusState == WaitFocusState.WAIT_PASSIVE_DONE) {
            this.mWaitFocusState = WaitFocusState.WAIT_NOTHING;
            LogHelper.d(TAG, "[updateFacesViewByFocus] clear hide msg, hide view right now");
            this.mMainHandler.removeMessages(2);
            hideView();
        }
        LogHelper.d(TAG, "[updateFacesViewByFocus] exit, mWaitFocusState = " + this.mWaitFocusState);
    }

    private void initFaceView() {
        this.mRootViewGroup = this.mAppUi.getPreviewFrameLayout();
        this.mFaceLayout = (FrameLayout) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.face_view, (ViewGroup) this.mRootViewGroup, true);
        this.mFaceView = (FaceView) this.mFaceLayout.findViewById(R.id.face_view);
        this.mFaceViewState = FaceViewState.STATE_INIT;
        this.mRootViewGroup.registerView(this.mFaceView, 10);
    }

    private void showView() {
        if (this.mFaceView != null && this.mFaceView.getVisibility() != 0) {
            LogHelper.d(TAG, "[showView]");
            this.mFaceView.setVisibility(0);
        }
    }

    private void hideView() {
        if (this.mFaceView != null && this.mFaceView.getVisibility() == 0) {
            LogHelper.d(TAG, "[hideView]");
            this.mFaceExitAnim.reset();
            this.mFaceView.clearAnimation();
            this.mFaceView.startAnimation(this.mFaceExitAnim);
            this.mFaceView.setVisibility(4);
        }
    }

    private void unInitFaceView() {
        this.mRootViewGroup.unRegisterView(this.mFaceView);
        this.mFaceView.setVisibility(8);
        this.mRootViewGroup.removeView(this.mFaceView);
        this.mFaceViewState = FaceViewState.STATE_UNINIT;
        this.mFaceView = null;
    }

    private void updateViewDisplayOrientation(int i, int i2) {
        if (this.mFaceView != null) {
            this.mFaceView.setDisplayOrientation(i, i2);
        }
    }

    private void updateViewPreviewSize(int i, int i2) {
        if (this.mFaceView != null) {
            this.mFaceView.updatePreviewSize(i, i2);
        }
    }
}
