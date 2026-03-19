package com.mediatek.camera.feature.setting;

import android.app.Activity;
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
import com.mediatek.camera.common.widget.RotateStrokeTextView;

public class ContinuousShotView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ContinuousShotView.class.getSimpleName());
    private IApp mApp;
    private ViewGroup mContinuousShotRoot;
    private ContinuousShotIndicatorState mIndicatorState;
    private Handler mIndicatorViewHandler;
    private boolean mIsSupported = true;
    private ViewGroup mRootViewGroup;
    private RotateStrokeTextView mRotateStrokeTextView;

    private enum ContinuousShotIndicatorState {
        INIT,
        SHOW,
        HIDE,
        UNINT
    }

    public void initIndicatorView(IApp iApp) {
        if (!this.mIsSupported) {
            return;
        }
        this.mApp = iApp;
        this.mIndicatorViewHandler = new MainHandler(iApp.getActivity().getMainLooper());
        this.mIndicatorState = ContinuousShotIndicatorState.INIT;
        this.mIndicatorViewHandler.sendEmptyMessage(1000);
    }

    public void setIndicatorViewOrientation(int i) {
        if (!this.mIsSupported) {
            return;
        }
        setOrientation(i);
    }

    public void showIndicatorView(int i) {
        if (this.mIndicatorViewHandler != null && this.mIsSupported) {
            LogHelper.d(TAG, "showIndicatorView(), num = " + i);
            this.mIndicatorViewHandler.obtainMessage(1002, Integer.valueOf(i)).sendToTarget();
            this.mIndicatorState = ContinuousShotIndicatorState.SHOW;
        }
    }

    public void hideIndicatorView() {
        if (this.mIndicatorViewHandler != null && this.mIsSupported && this.mIndicatorState == ContinuousShotIndicatorState.SHOW) {
            this.mIndicatorViewHandler.sendEmptyMessage(1003);
            this.mIndicatorState = ContinuousShotIndicatorState.HIDE;
        }
    }

    public void unInitIndicatorView() {
        if (this.mIndicatorViewHandler != null && this.mIsSupported) {
            this.mIndicatorViewHandler.sendEmptyMessage(1001);
            this.mIndicatorState = ContinuousShotIndicatorState.UNINT;
        }
    }

    public void clearIndicatorAllMessage() {
        if (this.mIndicatorViewHandler != null && this.mIsSupported) {
            this.mIndicatorViewHandler.removeMessages(1000);
            this.mIndicatorViewHandler.removeMessages(1002);
            this.mIndicatorViewHandler.removeMessages(1003);
            this.mIndicatorViewHandler.removeMessages(1001);
        }
    }

    public void clearIndicatorMessage(int i) {
        if (this.mIndicatorViewHandler != null && this.mIsSupported) {
            this.mIndicatorViewHandler.removeMessages(i);
        }
    }

    private class MainHandler extends Handler {
        MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(ContinuousShotView.TAG, "[handleMessage]msg.what = " + message.what + ", msg.obj = " + message.obj);
            switch (message.what) {
                case 1000:
                    ContinuousShotView.this.init(ContinuousShotView.this.mApp.getActivity(), ContinuousShotView.this.mApp.getAppUi());
                    break;
                case 1001:
                    ContinuousShotView.this.unInit();
                    break;
                case 1002:
                    ContinuousShotView.this.show(String.valueOf(message.obj));
                    break;
                case 1003:
                    ContinuousShotView.this.hide();
                    break;
            }
        }
    }

    private void init(Activity activity, IAppUi iAppUi) {
        this.mRootViewGroup = iAppUi.getModeRootView();
        View viewInflate = activity.getLayoutInflater().inflate(R.layout.continuous_shot_view, this.mRootViewGroup, true);
        this.mContinuousShotRoot = (ViewGroup) viewInflate.findViewById(R.id.continuous_root);
        this.mRotateStrokeTextView = (RotateStrokeTextView) viewInflate.findViewById(R.id.shot_num);
        LogHelper.d(TAG, "[init] mRotateStrokeTextView = " + this.mRotateStrokeTextView);
    }

    private void show(String str) {
        if (this.mRotateStrokeTextView != null) {
            LogHelper.d(TAG, "[show] msg = " + str);
            this.mRotateStrokeTextView.setText(str);
            this.mRotateStrokeTextView.setVisibility(0);
        }
    }

    private void hide() {
        if (this.mRotateStrokeTextView != null) {
            this.mRotateStrokeTextView.setVisibility(4);
        }
    }

    private void setOrientation(int i) {
        if (this.mRotateStrokeTextView != null) {
            this.mRotateStrokeTextView.setOrientation(i, false);
        }
    }

    private void unInit() {
        if (this.mRootViewGroup != null) {
            this.mRotateStrokeTextView.setVisibility(8);
            this.mRootViewGroup.removeView(this.mRotateStrokeTextView);
            this.mRootViewGroup.removeView(this.mContinuousShotRoot);
            this.mRotateStrokeTextView = null;
            this.mContinuousShotRoot = null;
            this.mRootViewGroup = null;
        }
    }
}
