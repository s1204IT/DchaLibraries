package com.mediatek.camera.feature.mode.vsdof.view;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.RotateLayout;

public class SdofViewCtrl {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SdofViewCtrl.class.getSimpleName());
    private IApp mApp;
    private SeekBar mDofBar;
    private IAppUi.HintInfo mGuideHint;
    private MainHandler mMainHandler;
    private long mProcessTime;
    private ViewGroup mRootViewGroup;
    private RotateLayout mSdofLayout;
    private RelativeLayout mSdofView;
    private TextView mTextView;
    private ViewChangeListener mViewChangeListener;
    private int mLevel = 7;
    private int mDofLevel = 15;
    private int mProgress = 50;
    private SeekBar.OnSeekBarChangeListener mChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            int iRound = Math.round((i * (SdofViewCtrl.this.mDofLevel - 1)) / 100);
            if (jCurrentTimeMillis - SdofViewCtrl.this.mProcessTime >= 50 && iRound != SdofViewCtrl.this.mLevel) {
                SdofViewCtrl.this.mLevel = iRound;
                LogHelper.d(SdofViewCtrl.TAG, "onProgressChanged level = " + SdofViewCtrl.this.mLevel);
                SdofViewCtrl.this.mViewChangeListener.onVsDofLevelChanged(SdofViewCtrl.this.mLevel);
            }
            SdofViewCtrl.this.mProcessTime = System.currentTimeMillis();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            SdofViewCtrl.this.mProgress = seekBar.getProgress();
            SdofViewCtrl.this.mLevel = Math.round((SdofViewCtrl.this.mProgress * (SdofViewCtrl.this.mDofLevel - 1)) / 100);
            LogHelper.i(SdofViewCtrl.TAG, "onStopTrackingTouch level = " + SdofViewCtrl.this.mLevel);
            SdofViewCtrl.this.mViewChangeListener.onVsDofLevelChanged(SdofViewCtrl.this.mLevel);
        }
    };

    public interface ViewChangeListener {
        void onVsDofLevelChanged(int i);
    }

    public void init(IApp iApp) {
        int[] staticKeyResult;
        this.mApp = iApp;
        this.mGuideHint = new IAppUi.HintInfo();
        this.mGuideHint.mBackground = this.mApp.getActivity().getDrawable(this.mApp.getActivity().getResources().getIdentifier("hint_text_background", "drawable", this.mApp.getActivity().getPackageName()));
        this.mGuideHint.mType = IAppUi.HintType.TYPE_AUTO_HIDE;
        this.mGuideHint.mDelayTime = 5000;
        this.mMainHandler = new MainHandler(iApp.getActivity().getMainLooper());
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(this.mApp.getActivity(), CameraApiHelper.getLogicalCameraId());
        if (cameraCharacteristics == null || (staticKeyResult = CameraUtil.getStaticKeyResult(cameraCharacteristics, "com.mediatek.stereofeature.supporteddoflevel")) == null || staticKeyResult.length == 0 || staticKeyResult[0] == 0 || staticKeyResult[0] == 1) {
            return;
        }
        LogHelper.i(TAG, "[init] dofLevel value " + staticKeyResult[0]);
        if (staticKeyResult[0] != this.mDofLevel) {
            this.mDofLevel = staticKeyResult[0];
        }
    }

    public void showView() {
        if (this.mMainHandler != null) {
            this.mMainHandler.sendEmptyMessage(2);
            this.mViewChangeListener.onVsDofLevelChanged(this.mLevel);
        }
    }

    public void unInit() {
        if (this.mMainHandler != null) {
            this.mMainHandler.sendEmptyMessage(1);
            this.mMainHandler.sendEmptyMessage(3);
            this.mMainHandler.removeMessages(6);
        }
    }

    public void onOrientationChanged(int i) {
        if (this.mMainHandler != null) {
            this.mMainHandler.obtainMessage(4, Integer.valueOf(i)).sendToTarget();
        }
    }

    public void setViewChangeListener(ViewChangeListener viewChangeListener) {
        this.mViewChangeListener = viewChangeListener;
    }

    public void showWarningView(int i) {
        if (this.mMainHandler != null) {
            this.mMainHandler.obtainMessage(6, Integer.valueOf(i)).sendToTarget();
        }
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 0) {
                switch (i) {
                    case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                        SdofViewCtrl.this.initView();
                        break;
                    case Camera2Proxy.TEMPLATE_RECORD:
                        SdofViewCtrl.this.unInitView();
                        break;
                    case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                        SdofViewCtrl.this.updateOrientation(((Integer) message.obj).intValue());
                        break;
                    case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                        SdofViewCtrl.this.mTextView.setVisibility(4);
                        break;
                    case Camera2Proxy.TEMPLATE_MANUAL:
                        SdofViewCtrl.this.showGuideView(((Integer) message.obj).intValue());
                        break;
                }
            }
            SdofViewCtrl.this.mTextView.setVisibility(0);
        }
    }

    private void initView() {
        this.mRootViewGroup = this.mApp.getAppUi().getModeRootView();
        this.mSdofLayout = (RotateLayout) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.sdof_view, this.mRootViewGroup, false).findViewById(R.id.sdof_rotate_layout);
        this.mSdofView = (RelativeLayout) this.mSdofLayout.findViewById(R.id.sdof_bottom_controls);
        this.mTextView = (TextView) this.mSdofLayout.findViewById(R.id.dof_text_view);
        this.mDofBar = (SeekBar) this.mSdofLayout.findViewById(R.id.sdof_bar);
        this.mDofBar.setVisibility(0);
        this.mProgress = 700 / (this.mDofLevel - 1);
        this.mDofBar.setProgress(this.mProgress);
        this.mDofBar.setOnSeekBarChangeListener(this.mChangeListener);
        this.mRootViewGroup.addView(this.mSdofLayout);
    }

    private void unInitView() {
        this.mLevel = 7;
        this.mProgress = 700 / (this.mDofLevel - 1);
        if (this.mRootViewGroup != null) {
            this.mRootViewGroup.removeView(this.mSdofLayout);
        }
        this.mSdofLayout = null;
        this.mSdofView = null;
        this.mApp.getAppUi().hideScreenHint(this.mGuideHint);
    }

    private void showGuideView(int i) {
        int i2 = 0;
        if (i == Integer.MIN_VALUE) {
            i2 = R.string.dual_camera_too_far_toast;
        } else if (i == 4) {
            i2 = R.string.dual_camera_too_close_toast;
        } else {
            switch (i) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    i2 = R.string.dual_camera_lens_covered_toast;
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    i2 = R.string.dual_camera_lowlight_toast;
                    break;
            }
        }
        if (i2 != 0) {
            this.mGuideHint.mHintText = this.mApp.getActivity().getString(i2);
            this.mApp.getAppUi().showScreenHint(this.mGuideHint);
        }
    }

    private void updateOrientation(int i) {
        if (this.mSdofView == null) {
            LogHelper.w(TAG, "[updateOrientation] view is null!");
            return;
        }
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mSdofView.getLayoutParams();
        if (i == 0) {
            layoutParams.setMargins(((ViewGroup.MarginLayoutParams) layoutParams).leftMargin, ((ViewGroup.MarginLayoutParams) layoutParams).topMargin, ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin, dpToPixel(130));
        } else if (i == 90) {
            layoutParams.setMargins(((ViewGroup.MarginLayoutParams) layoutParams).leftMargin, ((ViewGroup.MarginLayoutParams) layoutParams).topMargin, ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin, dpToPixel(40));
        } else if (i != 180) {
            if (i == 270) {
            }
        }
        this.mSdofView.setLayoutParams(layoutParams);
        CameraUtil.rotateRotateLayoutChildView(this.mApp.getActivity(), this.mSdofLayout, i, true);
    }

    private int dpToPixel(int i) {
        return (int) ((i * this.mApp.getActivity().getResources().getDisplayMetrics().density) + 0.5f);
    }
}
