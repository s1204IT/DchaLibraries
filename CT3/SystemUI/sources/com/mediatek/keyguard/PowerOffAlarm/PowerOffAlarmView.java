package com.mediatek.keyguard.PowerOffAlarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardSecurityView;
import com.android.keyguard.R$drawable;
import com.android.keyguard.R$id;
import com.mediatek.keyguard.PowerOffAlarm.multiwaveview.GlowPadView;

public class PowerOffAlarmView extends RelativeLayout implements KeyguardSecurityView, GlowPadView.OnTriggerListener {
    private final int DELAY_TIME_SECONDS;
    private KeyguardSecurityCallback mCallback;
    private Context mContext;
    private int mFailedPatternAttemptsSinceLastTimeout;
    private GlowPadView mGlowPadView;
    private final Handler mHandler;
    private boolean mIsDocked;
    private boolean mIsRegistered;
    private LockPatternUtils mLockPatternUtils;
    private boolean mPingEnabled;
    private final BroadcastReceiver mReceiver;
    private TextView mTitleView;
    private int mTotalFailedPatternAttempts;

    public PowerOffAlarmView(Context context) {
        this(context, null);
    }

    public PowerOffAlarmView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.DELAY_TIME_SECONDS = 7;
        this.mFailedPatternAttemptsSinceLastTimeout = 0;
        this.mTotalFailedPatternAttempts = 0;
        this.mTitleView = null;
        this.mIsRegistered = false;
        this.mIsDocked = false;
        this.mPingEnabled = true;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 99:
                        if (PowerOffAlarmView.this.mTitleView != null) {
                            PowerOffAlarmView.this.mTitleView.setText(msg.getData().getString("label"));
                        }
                        break;
                    case 101:
                        PowerOffAlarmView.this.triggerPing();
                        break;
                }
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                Log.v("PowerOffAlarmView", "receive action : " + action);
                if ("update.power.off.alarm.label".equals(action)) {
                    Message msg = new Message();
                    msg.what = 99;
                    Bundle data = new Bundle();
                    data.putString("label", intent.getStringExtra("label"));
                    msg.setData(data);
                    PowerOffAlarmView.this.mHandler.sendMessage(msg);
                    return;
                }
                if (!PowerOffAlarmManager.isAlarmBoot()) {
                    return;
                }
                PowerOffAlarmView.this.snooze();
            }
        };
        this.mContext = context;
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    @Override
    protected void onFinishInflate() {
        LockPatternUtils lockPatternUtils;
        super.onFinishInflate();
        Log.w("PowerOffAlarmView", "onFinishInflate ... ");
        setKeepScreenOn(true);
        this.mTitleView = (TextView) findViewById(R$id.alertTitle);
        this.mGlowPadView = (GlowPadView) findViewById(R$id.glow_pad_view);
        this.mGlowPadView.setOnTriggerListener(this);
        setFocusableInTouchMode(true);
        triggerPing();
        IntentFilter ifilter = new IntentFilter("android.intent.action.DOCK_EVENT");
        Intent dockStatus = this.mContext.registerReceiver(null, ifilter);
        if (dockStatus != null) {
            this.mIsDocked = dockStatus.getIntExtra("android.intent.extra.DOCK_STATE", -1) != 0;
        }
        IntentFilter filter = new IntentFilter("alarm_killed");
        filter.addAction("com.android.deskclock.ALARM_SNOOZE");
        filter.addAction("com.android.deskclock.ALARM_DISMISS");
        filter.addAction("update.power.off.alarm.label");
        this.mContext.registerReceiver(this.mReceiver, filter);
        if (this.mLockPatternUtils == null) {
            lockPatternUtils = new LockPatternUtils(this.mContext);
        } else {
            lockPatternUtils = this.mLockPatternUtils;
        }
        this.mLockPatternUtils = lockPatternUtils;
        enableEventDispatching(true);
    }

    @Override
    public void onTrigger(View v, int target) {
        int resId = this.mGlowPadView.getResourceIdForTarget(target);
        if (resId == R$drawable.mtk_ic_alarm_alert_snooze) {
            snooze();
            return;
        }
        if (resId == R$drawable.mtk_ic_alarm_alert_dismiss_pwroff) {
            powerOff();
        } else if (resId == R$drawable.mtk_ic_alarm_alert_dismiss_pwron) {
            powerOn();
        } else {
            Log.e("PowerOffAlarmView", "Trigger detected on unhandled resource. Skipping.");
        }
    }

    public void triggerPing() {
        if (!this.mPingEnabled) {
            return;
        }
        this.mGlowPadView.ping();
        this.mHandler.sendEmptyMessageDelayed(101, 1200L);
    }

    public void snooze() {
        Log.d("PowerOffAlarmView", "snooze selected");
        sendBR("com.android.deskclock.SNOOZE_ALARM");
    }

    private void powerOn() {
        enableEventDispatching(false);
        Log.d("PowerOffAlarmView", "powerOn selected");
        sendBR("com.android.deskclock.POWER_ON_ALARM");
        sendBR("android.intent.action.normal.boot");
    }

    private void powerOff() {
        Log.d("PowerOffAlarmView", "powerOff selected");
        sendBR("com.android.deskclock.DISMISS_ALARM");
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        return result;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume(int reason) {
        reset();
        Log.v("PowerOffAlarmView", "onResume");
    }

    @Override
    public void onDetachedFromWindow() {
        Log.v("PowerOffAlarmView", "onDetachedFromWindow ....");
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    @Override
    public void showPromptReason(int reason) {
    }

    @Override
    public void showMessage(String message, int color) {
    }

    private void enableEventDispatching(boolean flag) {
        try {
            IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            if (wm == null) {
                return;
            }
            wm.setEventDispatching(flag);
        } catch (RemoteException e) {
            Log.w("PowerOffAlarmView", e.toString());
        }
    }

    private void sendBR(String action) {
        Log.w("PowerOffAlarmView", "send BR: " + action);
        this.mContext.sendBroadcast(new Intent(action));
    }

    @Override
    public void onGrabbed(View v, int handle) {
    }

    @Override
    public void onReleased(View v, int handle) {
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {
    }

    @Override
    public void onFinishFinalAnimation() {
    }

    public void reset() {
    }

    @Override
    public void startAppearAnimation() {
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (PowerOffAlarmManager.isAlarmBoot()) {
            switch (keyCode) {
                case 24:
                    Log.d("PowerOffAlarmView", "onKeyDown() - KeyEvent.KEYCODE_VOLUME_UP, do nothing.");
                    return true;
                case 25:
                    Log.d("PowerOffAlarmView", "onKeyDown() - KeyEvent.KEYCODE_VOLUME_DOWN, do nothing.");
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (PowerOffAlarmManager.isAlarmBoot()) {
            switch (keyCode) {
                case 24:
                    Log.d("PowerOffAlarmView", "onKeyUp() - KeyEvent.KEYCODE_VOLUME_UP, do nothing.");
                    return true;
                case 25:
                    Log.d("PowerOffAlarmView", "onKeyUp() - KeyEvent.KEYCODE_VOLUME_DOWN, do nothing.");
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
