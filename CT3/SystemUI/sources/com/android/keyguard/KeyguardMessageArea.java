package com.android.keyguard;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.keyguard.KeyguardSecurityModel;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import java.lang.ref.WeakReference;

public class KeyguardMessageArea extends TextView implements SecurityMessageDisplay {
    private static final Object ANNOUNCE_TOKEN = new Object();
    private final Runnable mClearMessageRunnable;
    private final int mDefaultColor;
    private final Handler mHandler;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    CharSequence mMessage;
    private int mNextMessageColor;
    private KeyguardSecurityModel mSecurityModel;
    private CharSequence mSeparator;
    long mTimeout;
    private final KeyguardUpdateMonitor mUpdateMonitor;

    public KeyguardMessageArea(Context context) {
        this(context, null);
    }

    public KeyguardMessageArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTimeout = 5000L;
        this.mNextMessageColor = -1;
        this.mClearMessageRunnable = new Runnable() {
            @Override
            public void run() {
                KeyguardMessageArea.this.mMessage = null;
                KeyguardMessageArea.this.update();
            }
        };
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onFinishedGoingToSleep(int why) {
                KeyguardMessageArea.this.setSelected(false);
            }

            @Override
            public void onStartedWakingUp() {
                KeyguardMessageArea.this.setSelected(true);
            }
        };
        setLayerType(2, null);
        this.mSecurityModel = new KeyguardSecurityModel(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        this.mUpdateMonitor.registerCallback(this.mInfoCallback);
        this.mHandler = new Handler(Looper.myLooper());
        this.mSeparator = getResources().getString(R.string.install_carrier_app_notification_button);
        this.mDefaultColor = getCurrentTextColor();
        update();
    }

    @Override
    public void setNextMessageColor(int color) {
        this.mNextMessageColor = color;
    }

    @Override
    public void setMessage(CharSequence msg, boolean important) {
        if (!TextUtils.isEmpty(msg) && important) {
            securityMessageChanged(msg);
        } else {
            clearMessage();
        }
    }

    @Override
    public void setMessage(int resId, boolean important) {
        if (resId != 0 && important) {
            CharSequence message = getContext().getResources().getText(resId);
            securityMessageChanged(message);
        } else {
            clearMessage();
        }
    }

    @Override
    public void setMessage(int resId, boolean important, Object... formatArgs) {
        if (resId != 0 && important) {
            String message = getContext().getString(resId, formatArgs);
            securityMessageChanged(message);
        } else {
            clearMessage();
        }
    }

    @Override
    public void setTimeout(int timeoutMs) {
        this.mTimeout = timeoutMs;
    }

    public static SecurityMessageDisplay findSecurityMessageDisplay(View v) {
        KeyguardMessageArea messageArea = (KeyguardMessageArea) v.findViewById(R$id.keyguard_message_area);
        if (messageArea == null) {
            throw new RuntimeException("Can't find keyguard_message_area in " + v.getClass());
        }
        return messageArea;
    }

    @Override
    protected void onFinishInflate() {
        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive();
        setSelected(shouldMarquee);
    }

    private void securityMessageChanged(CharSequence message) {
        this.mMessage = message;
        update();
        this.mHandler.removeCallbacks(this.mClearMessageRunnable);
        if (this.mTimeout > 0) {
            this.mHandler.postDelayed(this.mClearMessageRunnable, this.mTimeout);
        }
        this.mHandler.removeCallbacksAndMessages(ANNOUNCE_TOKEN);
        this.mHandler.postAtTime(new AnnounceRunnable(this, getText()), ANNOUNCE_TOKEN, SystemClock.uptimeMillis() + 250);
    }

    private void clearMessage() {
        this.mHandler.removeCallbacks(this.mClearMessageRunnable);
        this.mHandler.post(this.mClearMessageRunnable);
    }

    public void update() {
        CharSequence status = this.mMessage;
        setVisibility(TextUtils.isEmpty(status) ? 4 : 0);
        if (this.mSecurityModel.getSecurityMode() == KeyguardSecurityModel.SecurityMode.AntiTheft) {
            setText(AntiTheftManager.getAntiTheftMessageAreaText(status, this.mSeparator));
        } else {
            setText(status);
        }
        int color = this.mDefaultColor;
        if (this.mNextMessageColor != -1) {
            color = this.mNextMessageColor;
            this.mNextMessageColor = -1;
        }
        setTextColor(color);
    }

    private static class AnnounceRunnable implements Runnable {
        private final WeakReference<View> mHost;
        private final CharSequence mTextToAnnounce;

        AnnounceRunnable(View host, CharSequence textToAnnounce) {
            this.mHost = new WeakReference<>(host);
            this.mTextToAnnounce = textToAnnounce;
        }

        @Override
        public void run() {
            View host = this.mHost.get();
            if (host == null) {
                return;
            }
            host.announceForAccessibility(this.mTextToAnnounce);
        }
    }
}
