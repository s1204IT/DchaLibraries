package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.policy.AccessibilityController;

public class LockIcon extends KeyguardAffordanceView {
    private AccessibilityController mAccessibilityController;
    private int mDensity;
    private boolean mDeviceInteractive;
    private boolean mHasFingerPrintIcon;
    private boolean mLastDeviceInteractive;
    private boolean mLastScreenOn;
    private int mLastState;
    private boolean mScreenOn;
    private boolean mTransientFpError;
    private TrustDrawable mTrustDrawable;
    private final UnlockMethodCache mUnlockMethodCache;

    public LockIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLastState = 0;
        this.mTrustDrawable = new TrustDrawable(context);
        setBackground(this.mTrustDrawable);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(context);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (isShown()) {
            this.mTrustDrawable.start();
        } else {
            this.mTrustDrawable.stop();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mTrustDrawable.stop();
    }

    public void setTransientFpError(boolean transientFpError) {
        this.mTransientFpError = transientFpError;
        update();
    }

    public void setDeviceInteractive(boolean deviceInteractive) {
        this.mDeviceInteractive = deviceInteractive;
        update();
    }

    public void setScreenOn(boolean screenOn) {
        this.mScreenOn = screenOn;
        update();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int density = newConfig.densityDpi;
        if (density == this.mDensity) {
            return;
        }
        this.mDensity = density;
        this.mTrustDrawable.stop();
        this.mTrustDrawable = new TrustDrawable(getContext());
        setBackground(this.mTrustDrawable);
        update();
    }

    public void update() {
        update(false);
    }

    public void update(boolean force) {
        boolean visible;
        AnimatedVectorDrawable animatedVectorDrawable;
        int dimensionPixelSize;
        int i;
        if (!isShown()) {
            visible = false;
        } else {
            visible = KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive();
        }
        if (visible) {
            this.mTrustDrawable.start();
        } else {
            this.mTrustDrawable.stop();
        }
        int state = getState();
        boolean anyFingerprintIcon = state == 3 || state == 4;
        boolean useAdditionalPadding = anyFingerprintIcon;
        boolean trustHidden = anyFingerprintIcon;
        if (state != this.mLastState || this.mDeviceInteractive != this.mLastDeviceInteractive || this.mScreenOn != this.mLastScreenOn || force) {
            boolean isAnim = true;
            int iconRes = getAnimationResForTransition(this.mLastState, state, this.mLastDeviceInteractive, this.mDeviceInteractive, this.mLastScreenOn, this.mScreenOn);
            if (iconRes == R.drawable.lockscreen_fingerprint_draw_off_animation) {
                anyFingerprintIcon = true;
                useAdditionalPadding = true;
                trustHidden = true;
            } else if (iconRes == R.drawable.trusted_state_to_error_animation) {
                anyFingerprintIcon = true;
                useAdditionalPadding = false;
                trustHidden = true;
            } else if (iconRes == R.drawable.error_to_trustedstate_animation) {
                anyFingerprintIcon = true;
                useAdditionalPadding = false;
                trustHidden = false;
            }
            if (iconRes == -1) {
                iconRes = getIconForState(state, this.mScreenOn, this.mDeviceInteractive);
                isAnim = false;
            }
            Drawable icon = this.mContext.getDrawable(iconRes);
            if (icon instanceof AnimatedVectorDrawable) {
                animatedVectorDrawable = (AnimatedVectorDrawable) icon;
            } else {
                animatedVectorDrawable = null;
            }
            int iconHeight = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_icon_height);
            int iconWidth = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_icon_width);
            if (!anyFingerprintIcon && (icon.getIntrinsicHeight() != iconHeight || icon.getIntrinsicWidth() != iconWidth)) {
                icon = new IntrinsicSizeDrawable(icon, iconWidth, iconHeight);
            }
            if (useAdditionalPadding) {
                dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.fingerprint_icon_additional_padding);
            } else {
                dimensionPixelSize = 0;
            }
            setPaddingRelative(0, 0, 0, dimensionPixelSize);
            setRestingAlpha(anyFingerprintIcon ? 1.0f : 0.5f);
            setImageDrawable(icon);
            Resources resources = getResources();
            if (anyFingerprintIcon) {
                i = R.string.accessibility_unlock_button_fingerprint;
            } else {
                i = R.string.accessibility_unlock_button;
            }
            String contentDescription = resources.getString(i);
            setContentDescription(contentDescription);
            this.mHasFingerPrintIcon = anyFingerprintIcon;
            if (animatedVectorDrawable != null && isAnim) {
                animatedVectorDrawable.start();
            }
            this.mLastState = state;
            this.mLastDeviceInteractive = this.mDeviceInteractive;
            this.mLastScreenOn = this.mScreenOn;
        }
        boolean trustManaged = this.mUnlockMethodCache.isTrustManaged() && !trustHidden;
        this.mTrustDrawable.setTrustManaged(trustManaged);
        updateClickability();
    }

    private void updateClickability() {
        if (this.mAccessibilityController == null) {
            return;
        }
        boolean clickToUnlock = this.mAccessibilityController.isTouchExplorationEnabled();
        boolean clickToForceLock = this.mUnlockMethodCache.isTrustManaged() && !this.mAccessibilityController.isAccessibilityEnabled();
        boolean longClickToForceLock = this.mUnlockMethodCache.isTrustManaged() && !clickToForceLock;
        if (clickToForceLock) {
            clickToUnlock = true;
        }
        setClickable(clickToUnlock);
        setLongClickable(longClickToForceLock);
        setFocusable(this.mAccessibilityController.isAccessibilityEnabled());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (!this.mHasFingerPrintIcon) {
            return;
        }
        info.setClassName(LockIcon.class.getName());
        AccessibilityNodeInfo.AccessibilityAction unlock = new AccessibilityNodeInfo.AccessibilityAction(16, getContext().getString(R.string.accessibility_unlock_without_fingerprint));
        info.addAction(unlock);
    }

    public void setAccessibilityController(AccessibilityController accessibilityController) {
        this.mAccessibilityController = accessibilityController;
    }

    private int getIconForState(int state, boolean screenOn, boolean deviceInteractive) {
        switch (state) {
            case 0:
                return R.drawable.ic_lock_24dp;
            case 1:
                return R.drawable.ic_lock_open_24dp;
            case 2:
                return android.R.drawable.clock_dial;
            case 3:
                if (screenOn && deviceInteractive) {
                    return R.drawable.ic_fingerprint;
                }
                return R.drawable.lockscreen_fingerprint_draw_on_animation;
            case 4:
                return R.drawable.ic_fingerprint_error;
            default:
                throw new IllegalArgumentException();
        }
    }

    private int getAnimationResForTransition(int oldState, int newState, boolean oldDeviceInteractive, boolean deviceInteractive, boolean oldScreenOn, boolean screenOn) {
        if (oldState == 3 && newState == 4) {
            return R.drawable.lockscreen_fingerprint_fp_to_error_state_animation;
        }
        if (oldState == 1 && newState == 4) {
            return R.drawable.trusted_state_to_error_animation;
        }
        if (oldState == 4 && newState == 1) {
            return R.drawable.error_to_trustedstate_animation;
        }
        if (oldState == 4 && newState == 3) {
            return R.drawable.lockscreen_fingerprint_error_state_to_fp_animation;
        }
        if (oldState == 3 && newState == 1 && !this.mUnlockMethodCache.isTrusted()) {
            return R.drawable.lockscreen_fingerprint_draw_off_animation;
        }
        if (newState != 3) {
            return -1;
        }
        if (oldScreenOn || !screenOn || !deviceInteractive) {
            if (screenOn && !oldDeviceInteractive && deviceInteractive) {
                return R.drawable.lockscreen_fingerprint_draw_on_animation;
            }
            return -1;
        }
        return R.drawable.lockscreen_fingerprint_draw_on_animation;
    }

    private int getState() {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean fingerprintRunning = updateMonitor.isFingerprintDetectionRunning();
        boolean unlockingAllowed = updateMonitor.isUnlockingWithFingerprintAllowed();
        if (this.mTransientFpError) {
            return 4;
        }
        if (this.mUnlockMethodCache.canSkipBouncer()) {
            return 1;
        }
        if (this.mUnlockMethodCache.isFaceUnlockRunning()) {
            return 2;
        }
        if (fingerprintRunning && unlockingAllowed) {
            return 3;
        }
        return 0;
    }

    private static class IntrinsicSizeDrawable extends InsetDrawable {
        private final int mIntrinsicHeight;
        private final int mIntrinsicWidth;

        public IntrinsicSizeDrawable(Drawable drawable, int intrinsicWidth, int intrinsicHeight) {
            super(drawable, 0);
            this.mIntrinsicWidth = intrinsicWidth;
            this.mIntrinsicHeight = intrinsicHeight;
        }

        @Override
        public int getIntrinsicWidth() {
            return this.mIntrinsicWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return this.mIntrinsicHeight;
        }
    }
}
