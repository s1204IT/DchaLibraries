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
import com.android.systemui.statusbar.policy.UserInfoController;

/* loaded from: classes.dex */
public class LockIcon extends KeyguardAffordanceView implements UserInfoController.OnUserInfoChangedListener {
    private AccessibilityController mAccessibilityController;
    private int mDensity;
    private boolean mDeviceInteractive;
    private final Runnable mDrawOffTimeout;
    private boolean mHasFaceUnlockIcon;
    private boolean mHasFingerPrintIcon;
    private boolean mLastDeviceInteractive;
    private boolean mLastScreenOn;
    private int mLastState;
    private boolean mScreenOn;
    private boolean mTransientFpError;
    private TrustDrawable mTrustDrawable;
    private final UnlockMethodCache mUnlockMethodCache;
    private Drawable mUserAvatarIcon;

    public LockIcon(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLastState = 0;
        this.mDrawOffTimeout = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$LockIcon$0dMARpPtuLHOj252cR_FbaJx3Kc
            @Override // java.lang.Runnable
            public final void run() throws Resources.NotFoundException {
                this.f$0.update(true);
            }
        };
        this.mTrustDrawable = new TrustDrawable(context);
        setBackground(this.mTrustDrawable);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(context);
    }

    @Override // android.view.View
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (isShown()) {
            this.mTrustDrawable.start();
        } else {
            this.mTrustDrawable.stop();
        }
    }

    @Override // android.widget.ImageView, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mTrustDrawable.stop();
    }

    @Override // com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener
    public void onUserInfoChanged(String str, Drawable drawable, String str2) throws Resources.NotFoundException {
        this.mUserAvatarIcon = drawable;
        update();
    }

    public void setTransientFpError(boolean z) throws Resources.NotFoundException {
        this.mTransientFpError = z;
        update();
    }

    public void setDeviceInteractive(boolean z) throws Resources.NotFoundException {
        this.mDeviceInteractive = z;
        update();
    }

    public void setScreenOn(boolean z) throws Resources.NotFoundException {
        this.mScreenOn = z;
        update();
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration configuration) throws Resources.NotFoundException {
        super.onConfigurationChanged(configuration);
        int i = configuration.densityDpi;
        if (i != this.mDensity) {
            this.mDensity = i;
            this.mTrustDrawable.stop();
            this.mTrustDrawable = new TrustDrawable(getContext());
            setBackground(this.mTrustDrawable);
            update();
        }
    }

    public void update() throws Resources.NotFoundException {
        update(false);
    }

    public void update(boolean z) throws Resources.NotFoundException {
        boolean z2;
        boolean z3;
        Drawable iconForState;
        AnimatedVectorDrawable animatedVectorDrawable;
        Drawable intrinsicSizeDrawable;
        int dimensionPixelSize;
        if (isShown() && KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive()) {
            this.mTrustDrawable.start();
        } else {
            this.mTrustDrawable.stop();
        }
        int state = getState();
        boolean z4 = state == 3 || state == 4;
        this.mHasFaceUnlockIcon = state == 2;
        if (state != this.mLastState || this.mDeviceInteractive != this.mLastDeviceInteractive || this.mScreenOn != this.mLastScreenOn || z) {
            int animationResForTransition = getAnimationResForTransition(this.mLastState, state, this.mLastDeviceInteractive, this.mDeviceInteractive, this.mLastScreenOn, this.mScreenOn);
            boolean z5 = animationResForTransition != -1;
            if (animationResForTransition != R.drawable.lockscreen_fingerprint_draw_off_animation) {
                if (animationResForTransition != R.drawable.trusted_state_to_error_animation) {
                    if (animationResForTransition == R.drawable.error_to_trustedstate_animation) {
                        z4 = true;
                        z2 = false;
                    } else {
                        z2 = z4;
                    }
                    z3 = z2;
                } else {
                    z3 = true;
                    z4 = true;
                    z2 = false;
                }
            } else {
                z2 = true;
                z3 = true;
                z4 = true;
            }
            if (z5) {
                iconForState = this.mContext.getDrawable(animationResForTransition);
            } else {
                iconForState = getIconForState(state, this.mScreenOn, this.mDeviceInteractive);
            }
            if (iconForState instanceof AnimatedVectorDrawable) {
                animatedVectorDrawable = (AnimatedVectorDrawable) iconForState;
            } else {
                animatedVectorDrawable = null;
            }
            int dimensionPixelSize2 = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_icon_height);
            int dimensionPixelSize3 = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_icon_width);
            if (!z4 && (iconForState.getIntrinsicHeight() != dimensionPixelSize2 || iconForState.getIntrinsicWidth() != dimensionPixelSize3)) {
                intrinsicSizeDrawable = new IntrinsicSizeDrawable(iconForState, dimensionPixelSize3, dimensionPixelSize2);
            } else {
                intrinsicSizeDrawable = iconForState;
            }
            if (!z2) {
                dimensionPixelSize = 0;
            } else {
                dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.fingerprint_icon_additional_padding);
            }
            setPaddingRelative(0, 0, 0, dimensionPixelSize);
            setRestingAlpha(z4 ? 1.0f : 0.5f);
            setImageDrawable(intrinsicSizeDrawable, false);
            if (this.mHasFaceUnlockIcon) {
                announceForAccessibility(getContext().getString(R.string.accessibility_scanning_face));
            }
            this.mHasFingerPrintIcon = z4;
            if (animatedVectorDrawable != null && z5) {
                animatedVectorDrawable.forceAnimationOnUI();
                animatedVectorDrawable.start();
            }
            if (animationResForTransition == R.drawable.lockscreen_fingerprint_draw_off_animation) {
                removeCallbacks(this.mDrawOffTimeout);
                postDelayed(this.mDrawOffTimeout, 800L);
            } else {
                removeCallbacks(this.mDrawOffTimeout);
            }
            this.mLastState = state;
            this.mLastDeviceInteractive = this.mDeviceInteractive;
            this.mLastScreenOn = this.mScreenOn;
        } else {
            z3 = z4;
        }
        this.mTrustDrawable.setTrustManaged(this.mUnlockMethodCache.isTrustManaged() && !z3);
        updateClickability();
    }

    private void updateClickability() {
        if (this.mAccessibilityController == null) {
            return;
        }
        boolean zIsAccessibilityEnabled = this.mAccessibilityController.isAccessibilityEnabled();
        boolean z = this.mUnlockMethodCache.isTrustManaged() && !zIsAccessibilityEnabled;
        boolean z2 = this.mUnlockMethodCache.isTrustManaged() && !z;
        setClickable(z || zIsAccessibilityEnabled);
        setLongClickable(z2);
        setFocusable(this.mAccessibilityController.isAccessibilityEnabled());
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (this.mHasFingerPrintIcon) {
            accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, getContext().getString(R.string.accessibility_unlock_without_fingerprint)));
            accessibilityNodeInfo.setHintText(getContext().getString(R.string.accessibility_waiting_for_fingerprint));
        } else if (this.mHasFaceUnlockIcon) {
            accessibilityNodeInfo.setClassName(LockIcon.class.getName());
            accessibilityNodeInfo.setContentDescription(getContext().getString(R.string.accessibility_scanning_face));
        }
    }

    public void setAccessibilityController(AccessibilityController accessibilityController) {
        this.mAccessibilityController = accessibilityController;
    }

    private Drawable getIconForState(int i, boolean z, boolean z2) {
        int i2;
        switch (i) {
            case 0:
                i2 = R.drawable.ic_lock_24dp;
                break;
            case 1:
                if (this.mUnlockMethodCache.isTrustManaged() && this.mUnlockMethodCache.isTrusted() && this.mUserAvatarIcon != null) {
                    return this.mUserAvatarIcon;
                }
                i2 = R.drawable.ic_lock_open_24dp;
                break;
            case 2:
                i2 = R.drawable.ic_face_unlock;
                break;
            case 3:
                if (z && z2) {
                    i2 = R.drawable.ic_fingerprint;
                    break;
                } else {
                    i2 = R.drawable.lockscreen_fingerprint_draw_on_animation;
                    break;
                }
            case 4:
                i2 = R.drawable.ic_fingerprint_error;
                break;
            default:
                throw new IllegalArgumentException();
        }
        return this.mContext.getDrawable(i2);
    }

    private int getAnimationResForTransition(int i, int i2, boolean z, boolean z2, boolean z3, boolean z4) {
        if (i == 3 && i2 == 4) {
            return R.drawable.lockscreen_fingerprint_fp_to_error_state_animation;
        }
        if (i == 1 && i2 == 4) {
            return R.drawable.trusted_state_to_error_animation;
        }
        if (i == 4 && i2 == 1) {
            return R.drawable.error_to_trustedstate_animation;
        }
        if (i == 4 && i2 == 3) {
            return R.drawable.lockscreen_fingerprint_error_state_to_fp_animation;
        }
        if (i == 3 && i2 == 1 && !this.mUnlockMethodCache.isTrusted()) {
            return R.drawable.lockscreen_fingerprint_draw_off_animation;
        }
        if (i2 != 3) {
            return -1;
        }
        if (!z3 && z4 && z2) {
            return R.drawable.lockscreen_fingerprint_draw_on_animation;
        }
        if (z4 && !z && z2) {
            return R.drawable.lockscreen_fingerprint_draw_on_animation;
        }
        return -1;
    }

    private int getState() {
        KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean zIsFingerprintDetectionRunning = keyguardUpdateMonitor.isFingerprintDetectionRunning();
        boolean zIsUnlockingWithFingerprintAllowed = keyguardUpdateMonitor.isUnlockingWithFingerprintAllowed();
        if (this.mTransientFpError) {
            return 4;
        }
        if (this.mUnlockMethodCache.canSkipBouncer()) {
            return 1;
        }
        if (this.mUnlockMethodCache.isFaceUnlockRunning()) {
            return 2;
        }
        if (zIsFingerprintDetectionRunning && zIsUnlockingWithFingerprintAllowed) {
            return 3;
        }
        return 0;
    }

    private static class IntrinsicSizeDrawable extends InsetDrawable {
        private final int mIntrinsicHeight;
        private final int mIntrinsicWidth;

        public IntrinsicSizeDrawable(Drawable drawable, int i, int i2) {
            super(drawable, 0);
            this.mIntrinsicWidth = i;
            this.mIntrinsicHeight = i2;
        }

        @Override // android.graphics.drawable.InsetDrawable, android.graphics.drawable.DrawableWrapper, android.graphics.drawable.Drawable
        public int getIntrinsicWidth() {
            return this.mIntrinsicWidth;
        }

        @Override // android.graphics.drawable.InsetDrawable, android.graphics.drawable.DrawableWrapper, android.graphics.drawable.Drawable
        public int getIntrinsicHeight() {
            return this.mIntrinsicHeight;
        }
    }
}
