package com.mediatek.camera.feature.setting.selftimer;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.RotateImageView;
import com.mediatek.camera.common.widget.RotateStrokeTextView;
import com.mediatek.camera.feature.setting.selftimer.ISelfTimerViewListener;
import java.util.Locale;

public class SelfTimerCtrl {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SelfTimerCtrl.class.getSimpleName());
    private IApp mApp;
    private IAppUi mAppUi;
    private Animation mCountDownAnim;
    private volatile int mCurSelfTimerNum;
    private Locale mLocale;
    private MainHandler mMainHandler;
    private ViewGroup mRootViewGroup;
    private RotateImageView mSelfTimerIndicatorView;
    private ISelfTimerViewListener.OnSelfTimerListener mSelfTimerListener;
    private SelfTimerSettingView mSelfTimerSettingView = new SelfTimerSettingView();
    private RotateStrokeTextView mSelfTimerTextView;
    private View mSelfTimerView;
    private SelfTimerSoundManager mSoundManager;
    private int mStartSelfTimerNum;

    public void init(IApp iApp) {
        this.mApp = iApp;
        this.mAppUi = iApp.getAppUi();
        this.mMainHandler = new MainHandler(iApp.getActivity().getMainLooper());
        this.mSoundManager = new SelfTimerSoundManager(iApp);
        this.mLocale = iApp.getActivity().getResources().getConfiguration().locale;
        this.mCountDownAnim = AnimationUtils.loadAnimation(iApp.getActivity(), R.anim.count_down_exit);
    }

    public void unInit() {
        this.mMainHandler.sendEmptyMessage(1);
        this.mMainHandler.sendEmptyMessage(3);
        this.mMainHandler.removeMessages(4);
        this.mMainHandler.removeMessages(5);
        this.mSoundManager.release();
    }

    public void setSelfTimerListener(ISelfTimerViewListener.OnSelfTimerListener onSelfTimerListener) {
        this.mSelfTimerListener = onSelfTimerListener;
    }

    public SelfTimerSettingView getSelfTimerSettingView() {
        return this.mSelfTimerSettingView;
    }

    public void initResBySwitch(String str) {
        if ("0".equals(str)) {
            this.mMainHandler.sendEmptyMessage(1);
            this.mSoundManager.release();
        } else {
            this.mMainHandler.obtainMessage(0, str).sendToTarget();
            this.mSoundManager.load();
        }
    }

    public void showSelfTimerView(String str) {
        if ("0".equals(str)) {
            this.mMainHandler.sendEmptyMessage(3);
        } else {
            this.mMainHandler.obtainMessage(2, str).sendToTarget();
        }
    }

    public void onOrientationChanged(int i) {
        if (this.mMainHandler == null) {
            return;
        }
        this.mMainHandler.obtainMessage(5, Integer.valueOf(i)).sendToTarget();
    }

    public boolean onInterrupt() {
        if (this.mMainHandler == null || this.mCurSelfTimerNum <= 0) {
            return false;
        }
        this.mSoundManager.stop();
        this.mMainHandler.sendEmptyMessage(3);
        this.mSelfTimerListener.onTimerInterrupt();
        return true;
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    SelfTimerCtrl.this.initIndicatorView((String) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    SelfTimerCtrl.this.unInitIndicatorView();
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    SelfTimerCtrl.this.startSelfTimer((String) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    SelfTimerCtrl.this.stopSelfTimer();
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    SelfTimerCtrl.this.updateSelfTimer();
                    break;
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    SelfTimerCtrl.this.updateOrientation(((Integer) message.obj).intValue());
                    break;
            }
        }
    }

    private void initIndicatorView(String str) {
        if (this.mSelfTimerIndicatorView == null) {
            this.mSelfTimerIndicatorView = (RotateImageView) this.mApp.getActivity().getLayoutInflater().inflate(R.layout.self_timer_indicator, (ViewGroup) null);
        }
        if ("2".equals(str)) {
            this.mSelfTimerIndicatorView.setImageResource(R.drawable.ic_selftimer_indicator_2);
        } else {
            this.mSelfTimerIndicatorView.setImageResource(R.drawable.ic_selftimer_indicator_10);
        }
        this.mAppUi.addToIndicatorView(this.mSelfTimerIndicatorView, 6);
    }

    private void unInitIndicatorView() {
        if (this.mSelfTimerIndicatorView != null) {
            this.mAppUi.removeFromIndicatorView(this.mSelfTimerIndicatorView);
        }
    }

    private void updateOrientation(int i) {
        if (this.mSelfTimerTextView != null) {
            CameraUtil.rotateRotateLayoutChildView(this.mApp.getActivity(), this.mSelfTimerView, i, true);
        }
    }

    private void startSelfTimer(String str) {
        this.mRootViewGroup = this.mApp.getAppUi().getModeRootView();
        this.mSelfTimerView = this.mApp.getActivity().getLayoutInflater().inflate(R.layout.self_timer_view, this.mRootViewGroup, true);
        this.mSelfTimerTextView = (RotateStrokeTextView) this.mSelfTimerView.findViewById(R.id.self_timer_num);
        CameraUtil.rotateRotateLayoutChildView(this.mApp.getActivity(), this.mSelfTimerView, this.mApp.getGSensorOrientation(), false);
        this.mSelfTimerTextView.setVisibility(0);
        this.mStartSelfTimerNum = Integer.parseInt(str);
        this.mCurSelfTimerNum = this.mStartSelfTimerNum + 1;
        this.mSelfTimerListener.onTimerStart();
        this.mMainHandler.sendEmptyMessage(4);
    }

    private void stopSelfTimer() {
        if (this.mSelfTimerView != null) {
            this.mSelfTimerTextView.setVisibility(4);
            this.mMainHandler.removeMessages(4);
            this.mRootViewGroup.removeView(this.mSelfTimerView);
            this.mSelfTimerListener.onTimerInterrupt();
            this.mStartSelfTimerNum = 0;
            this.mCurSelfTimerNum = 0;
        }
    }

    private void updateSelfTimer() {
        this.mCurSelfTimerNum--;
        LogHelper.d(TAG, "[updateSelfTimer] mCurSelfTimerNum: " + this.mCurSelfTimerNum);
        if (this.mCurSelfTimerNum > 0) {
            this.mSelfTimerTextView.setText(String.format(this.mLocale, "%d", Integer.valueOf(this.mCurSelfTimerNum)));
            this.mCountDownAnim.reset();
            this.mSelfTimerTextView.clearAnimation();
            this.mSelfTimerTextView.startAnimation(this.mCountDownAnim);
            if (this.mCurSelfTimerNum == 1) {
                this.mSoundManager.play(1);
            } else if (this.mCurSelfTimerNum <= 3) {
                this.mSoundManager.play(0);
            }
            this.mMainHandler.sendEmptyMessageDelayed(4, 1000L);
            return;
        }
        this.mSoundManager.stop();
        this.mMainHandler.sendEmptyMessage(3);
        this.mSelfTimerListener.onTimerDone();
    }
}
