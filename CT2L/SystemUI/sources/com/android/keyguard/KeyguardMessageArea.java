package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.MutableInt;
import android.view.View;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import java.lang.ref.WeakReference;

class KeyguardMessageArea extends TextView {
    private static final Object ANNOUNCE_TOKEN = new Object();
    Runnable mClearMessageRunnable;
    private Handler mHandler;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private LockPatternUtils mLockPatternUtils;
    CharSequence mMessage;
    private CharSequence mSeparator;
    boolean mShowingBouncer;
    boolean mShowingMessage;
    long mTimeout;
    KeyguardUpdateMonitor mUpdateMonitor;

    public static class Helper implements SecurityMessageDisplay {
        KeyguardMessageArea mMessageArea;

        Helper(View v) {
            this.mMessageArea = (KeyguardMessageArea) v.findViewById(R.id.keyguard_message_area);
            if (this.mMessageArea == null) {
                throw new RuntimeException("Can't find keyguard_message_area in " + v.getClass());
            }
        }

        @Override
        public void setMessage(CharSequence msg, boolean important) {
            if (!TextUtils.isEmpty(msg) && important) {
                this.mMessageArea.mMessage = msg;
                this.mMessageArea.securityMessageChanged();
            }
        }

        @Override
        public void setMessage(int resId, boolean important) {
            if (resId != 0 && important) {
                this.mMessageArea.mMessage = this.mMessageArea.getContext().getResources().getText(resId);
                this.mMessageArea.securityMessageChanged();
            }
        }

        @Override
        public void setMessage(int resId, boolean important, Object... formatArgs) {
            if (resId != 0 && important) {
                this.mMessageArea.mMessage = this.mMessageArea.getContext().getString(resId, formatArgs);
                this.mMessageArea.securityMessageChanged();
            }
        }

        @Override
        public void showBouncer(int duration) {
            this.mMessageArea.hideMessage(duration, false);
            this.mMessageArea.mShowingBouncer = true;
        }

        @Override
        public void hideBouncer(int duration) {
            this.mMessageArea.showMessage(duration);
            this.mMessageArea.mShowingBouncer = false;
        }

        @Override
        public void setTimeout(int timeoutMs) {
            this.mMessageArea.mTimeout = timeoutMs;
        }
    }

    public KeyguardMessageArea(Context context) {
        this(context, null);
    }

    public KeyguardMessageArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowingBouncer = false;
        this.mTimeout = 5000L;
        this.mClearMessageRunnable = new Runnable() {
            @Override
            public void run() {
                KeyguardMessageArea.this.mMessage = null;
                KeyguardMessageArea.this.mShowingMessage = false;
                if (KeyguardMessageArea.this.mShowingBouncer) {
                    KeyguardMessageArea.this.hideMessage(750, true);
                } else {
                    KeyguardMessageArea.this.update();
                }
            }
        };
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onScreenTurnedOff(int why) {
                KeyguardMessageArea.this.setSelected(false);
            }

            @Override
            public void onScreenTurnedOn() {
                KeyguardMessageArea.this.setSelected(true);
            }
        };
        setLayerType(2, null);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        this.mUpdateMonitor.registerCallback(this.mInfoCallback);
        this.mHandler = new Handler(Looper.myLooper());
        this.mSeparator = getResources().getString(android.R.string.mediasize_iso_c0);
        update();
    }

    @Override
    protected void onFinishInflate() {
        boolean screenOn = KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn();
        setSelected(screenOn);
    }

    public void securityMessageChanged() {
        setAlpha(1.0f);
        this.mShowingMessage = true;
        update();
        this.mHandler.removeCallbacks(this.mClearMessageRunnable);
        if (this.mTimeout > 0) {
            this.mHandler.postDelayed(this.mClearMessageRunnable, this.mTimeout);
        }
        this.mHandler.removeCallbacksAndMessages(ANNOUNCE_TOKEN);
        this.mHandler.postAtTime(new AnnounceRunnable(this, getText()), ANNOUNCE_TOKEN, SystemClock.uptimeMillis() + 250);
    }

    void update() {
        MutableInt icon = new MutableInt(0);
        CharSequence status = getCurrentMessage();
        setCompoundDrawablesWithIntrinsicBounds(icon.value, 0, 0, 0);
        setText(status);
    }

    CharSequence getCurrentMessage() {
        if (this.mShowingMessage) {
            return this.mMessage;
        }
        return null;
    }

    private void hideMessage(int duration, boolean thenUpdate) {
        if (duration > 0) {
            Animator anim = ObjectAnimator.ofFloat(this, "alpha", 0.0f);
            anim.setDuration(duration);
            if (thenUpdate) {
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        KeyguardMessageArea.this.update();
                    }
                });
            }
            anim.start();
            return;
        }
        setAlpha(0.0f);
        if (thenUpdate) {
            update();
        }
    }

    private void showMessage(int duration) {
        if (duration > 0) {
            Animator anim = ObjectAnimator.ofFloat(this, "alpha", 1.0f);
            anim.setDuration(duration);
            anim.start();
            return;
        }
        setAlpha(1.0f);
    }

    private static class AnnounceRunnable implements Runnable {
        private final WeakReference<View> mHost;
        private final CharSequence mTextToAnnounce;

        public AnnounceRunnable(View host, CharSequence textToAnnounce) {
            this.mHost = new WeakReference<>(host);
            this.mTextToAnnounce = textToAnnounce;
        }

        @Override
        public void run() {
            View host = this.mHost.get();
            if (host != null) {
                host.announceForAccessibility(this.mTextToAnnounce);
            }
        }
    }
}
