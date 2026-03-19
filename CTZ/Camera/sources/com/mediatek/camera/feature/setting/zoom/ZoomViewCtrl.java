package com.mediatek.camera.feature.setting.zoom;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.RotateLayout;

public class ZoomViewCtrl {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ZoomViewCtrl.class.getSimpleName());
    private IApp mApp;
    private MainHandler mMainHandler;
    private ViewGroup mRootViewGroup;
    private TextView mTextView;
    private IAppUi.HintInfo mZoomIndicatorHint;
    private RotateLayout mZoomView;

    public void init(IApp iApp) {
        LogHelper.d(TAG, "[init]");
        this.mApp = iApp;
        this.mMainHandler = new MainHandler(iApp.getActivity().getMainLooper());
        this.mMainHandler.sendEmptyMessage(2);
    }

    public void unInit() {
        this.mMainHandler.sendEmptyMessage(1);
        this.mMainHandler.sendEmptyMessage(3);
    }

    public void showView(String str) {
        this.mMainHandler.obtainMessage(0, str).sendToTarget();
    }

    public void hideView() {
        this.mMainHandler.sendEmptyMessage(5);
    }

    public void resetView() {
        this.mMainHandler.sendEmptyMessageDelayed(1, 3000L);
    }

    public void onOrientationChanged(int i) {
        if (this.mMainHandler == null) {
            return;
        }
        this.mMainHandler.obtainMessage(4, Integer.valueOf(i)).sendToTarget();
    }

    public void clearInvalidView() {
        this.mMainHandler.removeMessages(1);
        this.mMainHandler.removeMessages(0);
        this.mApp.getAppUi().setUIVisibility(4, 4);
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    ZoomViewCtrl.this.show((String) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    ZoomViewCtrl.this.reset();
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    ZoomViewCtrl.this.initView();
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    ZoomViewCtrl.this.unInitView();
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    ZoomViewCtrl.this.updateOrientation(((Integer) message.obj).intValue());
                    break;
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    ZoomViewCtrl.this.hide();
                    break;
            }
        }
    }

    private void show(String str) {
        if (this.mZoomView == null) {
            return;
        }
        this.mZoomIndicatorHint.mHintText = str;
        this.mApp.getAppUi().showScreenHint(this.mZoomIndicatorHint);
    }

    private void hide() {
        if (this.mZoomView == null) {
            return;
        }
        this.mApp.getAppUi().hideScreenHint(this.mZoomIndicatorHint);
    }

    private void reset() {
        if (this.mZoomView == null) {
            return;
        }
        this.mApp.getAppUi().hideScreenHint(this.mZoomIndicatorHint);
        this.mApp.getAppUi().setUIVisibility(4, 0);
    }

    private void initView() {
        this.mRootViewGroup = this.mApp.getAppUi().getModeRootView();
        this.mZoomView = (RotateLayout) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.zoom_view, this.mRootViewGroup, false).findViewById(R.id.zoom_rotate_layout);
        this.mTextView = (TextView) this.mZoomView.findViewById(R.id.zoom_ratio);
        this.mRootViewGroup.addView(this.mZoomView);
        this.mZoomIndicatorHint = new IAppUi.HintInfo();
        this.mZoomIndicatorHint.mType = IAppUi.HintType.TYPE_AUTO_HIDE;
        this.mZoomIndicatorHint.mDelayTime = 3000;
    }

    private void unInitView() {
        this.mRootViewGroup.removeView(this.mZoomView);
        this.mZoomView = null;
    }

    private void updateOrientation(int i) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mTextView.getLayoutParams();
        if (i != 0) {
            if (i == 90) {
                layoutParams.setMargins(((ViewGroup.MarginLayoutParams) layoutParams).leftMargin, dpToPixel(2), ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin, ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin);
            } else if (i == 180) {
                layoutParams.setMargins(((ViewGroup.MarginLayoutParams) layoutParams).leftMargin, dpToPixel(120), ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin, ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin);
            } else if (i == 270) {
            }
        } else if (CameraUtil.isTablet()) {
            layoutParams.setMargins(((ViewGroup.MarginLayoutParams) layoutParams).leftMargin, dpToPixel(100), ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin, ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin);
        } else {
            layoutParams.setMargins(((ViewGroup.MarginLayoutParams) layoutParams).leftMargin, dpToPixel(40), ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin, ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin);
        }
        this.mTextView.setLayoutParams(layoutParams);
        CameraUtil.rotateRotateLayoutChildView(this.mApp.getActivity(), this.mZoomView, i, true);
    }

    private int dpToPixel(int i) {
        return (int) ((i * this.mApp.getActivity().getResources().getDisplayMetrics().density) + 0.5f);
    }
}
