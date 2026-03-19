package com.mediatek.camera.feature.setting.dng;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.widget.RotateImageView;
import java.util.List;

public class DngViewCtrl {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(DngViewCtrl.class.getSimpleName());
    private IApp mApp;
    private IAppUi mAppUi;
    private View mDngIndicatorView;
    private DngSettingView mDngSettingView = new DngSettingView();
    private MainHandler mMainHandler;

    interface OnDngSettingViewListener {
        void onItemViewClick(boolean z);

        boolean onUpdatedValue();
    }

    public void init(IApp iApp) {
        this.mApp = iApp;
        this.mAppUi = iApp.getAppUi();
        this.mMainHandler = new MainHandler(iApp.getActivity().getMainLooper());
    }

    public void setDngSettingViewListener(OnDngSettingViewListener onDngSettingViewListener) {
        this.mDngSettingView.setDngViewListener(onDngSettingViewListener);
    }

    public void setEntryValue(List<String> list) {
        this.mDngSettingView.setEntryValue(list);
    }

    public void setSettingDeviceRequest(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mDngSettingView.setSettingRequester(settingDevice2Requester);
    }

    public void setEnabled(boolean z) {
        this.mDngSettingView.setEnabled(z);
    }

    public DngSettingView getDngSettingView() {
        return this.mDngSettingView;
    }

    public void showDngIndicatorView(boolean z) {
        if (z) {
            this.mMainHandler.sendEmptyMessage(0);
        } else {
            this.mMainHandler.sendEmptyMessage(1);
        }
    }

    public void updateDngView() {
        this.mMainHandler.sendEmptyMessage(2);
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(DngViewCtrl.TAG, "[handleMessage]msg.what = " + message.what);
            switch (message.what) {
                case 0:
                    DngViewCtrl.this.initDngIndicatorView();
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    DngViewCtrl.this.unInitDngIndicatorView();
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    DngViewCtrl.this.updateDngSettingView();
                    break;
            }
        }
    }

    private void initDngIndicatorView() {
        if (this.mDngIndicatorView == null) {
            this.mDngIndicatorView = (RotateImageView) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.dng_indicator, (ViewGroup) null);
        }
        this.mAppUi.addToIndicatorView(this.mDngIndicatorView, 5);
    }

    private void unInitDngIndicatorView() {
        if (this.mDngIndicatorView != null) {
            this.mAppUi.removeFromIndicatorView(this.mDngIndicatorView);
        }
    }

    private void updateDngSettingView() {
        this.mDngSettingView.refreshView();
    }
}
