package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import com.android.systemui.R;

public class KeyButtonView extends ImageView {
    private Animator mAnimateToQuiescent;
    private AudioManager mAudioManager;
    private final Runnable mCheckLongPress;
    private int mCode;
    private long mDownTime;
    private float mDrawingAlpha;
    private float mQuiescentAlpha;
    private boolean mSupportsLongpress;
    private int mTouchSlop;

    public KeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.mDrawingAlpha = 1.0f;
        this.mQuiescentAlpha = 1.0f;
        this.mSupportsLongpress = true;
        this.mAnimateToQuiescent = new ObjectAnimator();
        this.mCheckLongPress = new Runnable() {
            @Override
            public void run() {
                if (KeyButtonView.this.isPressed()) {
                    if (KeyButtonView.this.isLongClickable()) {
                        KeyButtonView.this.performLongClick();
                    } else {
                        KeyButtonView.this.sendEvent(0, 128);
                        KeyButtonView.this.sendAccessibilityEvent(2);
                    }
                }
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView, defStyle, 0);
        this.mCode = a.getInteger(0, 0);
        this.mSupportsLongpress = a.getBoolean(1, true);
        setDrawingAlpha(this.mQuiescentAlpha);
        a.recycle();
        setClickable(true);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        setBackground(new KeyButtonRipple(context, this));
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (this.mCode != 0) {
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, null));
            if (this.mSupportsLongpress) {
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(32, null));
            }
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0) {
            jumpDrawablesToCurrentState();
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == 16 && this.mCode != 0) {
            sendEvent(0, 0, SystemClock.uptimeMillis());
            sendEvent(1, 0);
            sendAccessibilityEvent(1);
            playSoundEffect(0);
            return true;
        }
        if (action == 32 && this.mCode != 0 && this.mSupportsLongpress) {
            sendEvent(0, 128);
            sendEvent(1, 0);
            sendAccessibilityEvent(2);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    public void setQuiescentAlpha(float alpha, boolean animate) {
        this.mAnimateToQuiescent.cancel();
        float alpha2 = Math.min(Math.max(alpha, 0.0f), 1.0f);
        if (alpha2 != this.mQuiescentAlpha || alpha2 != this.mDrawingAlpha) {
            this.mQuiescentAlpha = alpha2;
            if (animate) {
                this.mAnimateToQuiescent = animateToQuiescent();
                this.mAnimateToQuiescent.start();
            } else {
                setDrawingAlpha(this.mQuiescentAlpha);
            }
        }
    }

    private ObjectAnimator animateToQuiescent() {
        return ObjectAnimator.ofFloat(this, "drawingAlpha", this.mQuiescentAlpha);
    }

    public float getQuiescentAlpha() {
        return this.mQuiescentAlpha;
    }

    public float getDrawingAlpha() {
        return this.mDrawingAlpha;
    }

    public void setDrawingAlpha(float x) {
        setImageAlpha((int) (255.0f * x));
        this.mDrawingAlpha = x;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean z = false;
        int action = ev.getAction();
        switch (action) {
            case 0:
                this.mDownTime = SystemClock.uptimeMillis();
                setPressed(true);
                if (this.mCode != 0) {
                    sendEvent(0, 0, this.mDownTime);
                } else {
                    performHapticFeedback(1);
                }
                if (this.mSupportsLongpress) {
                    removeCallbacks(this.mCheckLongPress);
                    postDelayed(this.mCheckLongPress, ViewConfiguration.getLongPressTimeout());
                }
                return true;
            case 1:
                boolean doIt = isPressed();
                setPressed(false);
                if (this.mCode != 0) {
                    if (doIt) {
                        sendEvent(1, 0);
                        sendAccessibilityEvent(1);
                        playSoundEffect(0);
                    } else {
                        sendEvent(1, 32);
                    }
                } else if (doIt) {
                    performClick();
                }
                if (this.mSupportsLongpress) {
                    removeCallbacks(this.mCheckLongPress);
                }
                return true;
            case 2:
                int x = (int) ev.getX();
                int y = (int) ev.getY();
                if (x >= (-this.mTouchSlop) && x < getWidth() + this.mTouchSlop && y >= (-this.mTouchSlop) && y < getHeight() + this.mTouchSlop) {
                    z = true;
                }
                setPressed(z);
                return true;
            case 3:
                setPressed(false);
                if (this.mCode != 0) {
                    sendEvent(1, 32);
                }
                if (this.mSupportsLongpress) {
                    removeCallbacks(this.mCheckLongPress);
                }
                return true;
            default:
                return true;
        }
    }

    @Override
    public void playSoundEffect(int soundConstant) {
        this.mAudioManager.playSoundEffect(soundConstant, ActivityManager.getCurrentUser());
    }

    public void sendEvent(int action, int flags) {
        sendEvent(action, flags, SystemClock.uptimeMillis());
    }

    void sendEvent(int action, int flags, long when) {
        int repeatCount = (flags & 128) != 0 ? 1 : 0;
        KeyEvent ev = new KeyEvent(this.mDownTime, when, action, this.mCode, repeatCount, 0, -1, 0, flags | 8 | 64, 257);
        InputManager.getInstance().injectInputEvent(ev, 0);
    }
}
