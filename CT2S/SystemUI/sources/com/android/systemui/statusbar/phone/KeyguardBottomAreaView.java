package com.android.systemui.statusbar.phone;

import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.PreviewInflater;

public class KeyguardBottomAreaView extends FrameLayout implements View.OnClickListener, View.OnLongClickListener, UnlockMethodCache.OnUnlockMethodChangedListener, AccessibilityController.AccessibilityStateChangedCallback {
    private AccessibilityController mAccessibilityController;
    private View.AccessibilityDelegate mAccessibilityDelegate;
    private ActivityStarter mActivityStarter;
    private KeyguardAffordanceView mCameraImageView;
    private View mCameraPreview;
    private final BroadcastReceiver mDevicePolicyReceiver;
    private FlashlightController mFlashlightController;
    private KeyguardIndicationController mIndicationController;
    private TextView mIndicationText;
    private int mLastUnlockIconRes;
    private final Interpolator mLinearOutSlowInInterpolator;
    private KeyguardAffordanceView mLockIcon;
    private LockPatternUtils mLockPatternUtils;
    private KeyguardAffordanceView mPhoneImageView;
    private View mPhonePreview;
    private PhoneStatusBar mPhoneStatusBar;
    private ViewGroup mPreviewContainer;
    private PreviewInflater mPreviewInflater;
    private final TrustDrawable mTrustDrawable;
    private UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private static final Intent SECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
    private static final Intent INSECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA");
    private static final Intent PHONE_INTENT = new Intent("android.intent.action.DIAL");

    public KeyguardBottomAreaView(Context context) {
        this(context, null);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mLastUnlockIconRes = 0;
        this.mAccessibilityDelegate = new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                String label = null;
                if (host != KeyguardBottomAreaView.this.mLockIcon) {
                    if (host != KeyguardBottomAreaView.this.mCameraImageView) {
                        if (host == KeyguardBottomAreaView.this.mPhoneImageView) {
                            label = KeyguardBottomAreaView.this.getResources().getString(R.string.phone_label);
                        }
                    } else {
                        label = KeyguardBottomAreaView.this.getResources().getString(R.string.camera_label);
                    }
                } else {
                    label = KeyguardBottomAreaView.this.getResources().getString(R.string.unlock_label);
                }
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, label));
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == 16) {
                    if (host == KeyguardBottomAreaView.this.mLockIcon) {
                        KeyguardBottomAreaView.this.mPhoneStatusBar.animateCollapsePanels(2, true);
                        return true;
                    }
                    if (host != KeyguardBottomAreaView.this.mCameraImageView) {
                        if (host == KeyguardBottomAreaView.this.mPhoneImageView) {
                            KeyguardBottomAreaView.this.launchPhone();
                            return true;
                        }
                    } else {
                        KeyguardBottomAreaView.this.launchCamera();
                        return true;
                    }
                }
                return super.performAccessibilityAction(host, action, args);
            }
        };
        this.mDevicePolicyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                KeyguardBottomAreaView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        KeyguardBottomAreaView.this.updateCameraVisibility();
                    }
                });
            }
        };
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onUserSwitchComplete(int userId) {
                KeyguardBottomAreaView.this.updateCameraVisibility();
            }

            @Override
            public void onScreenTurnedOn() {
                KeyguardBottomAreaView.this.updateLockIcon();
            }

            @Override
            public void onScreenTurnedOff(int why) {
                KeyguardBottomAreaView.this.updateLockIcon();
            }

            @Override
            public void onKeyguardVisibilityChanged(boolean showing) {
                KeyguardBottomAreaView.this.updateLockIcon();
            }
        };
        this.mTrustDrawable = new TrustDrawable(this.mContext);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPreviewContainer = (ViewGroup) findViewById(R.id.preview_container);
        this.mCameraImageView = (KeyguardAffordanceView) findViewById(R.id.camera_button);
        this.mPhoneImageView = (KeyguardAffordanceView) findViewById(R.id.phone_button);
        this.mLockIcon = (KeyguardAffordanceView) findViewById(R.id.lock_icon);
        this.mIndicationText = (TextView) findViewById(R.id.keyguard_indication_text);
        watchForCameraPolicyChanges();
        updateCameraVisibility();
        updatePhoneVisibility();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(getContext());
        this.mUnlockMethodCache.addListener(this);
        updateLockIcon();
        setClipChildren(false);
        setClipToPadding(false);
        this.mPreviewInflater = new PreviewInflater(this.mContext, new LockPatternUtils(this.mContext));
        inflatePreviews();
        this.mLockIcon.setOnClickListener(this);
        this.mLockIcon.setBackground(this.mTrustDrawable);
        this.mLockIcon.setOnLongClickListener(this);
        this.mCameraImageView.setOnClickListener(this);
        this.mPhoneImageView.setOnClickListener(this);
        initAccessibility();
    }

    private void initAccessibility() {
        this.mLockIcon.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mPhoneImageView.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mCameraImageView.setAccessibilityDelegate(this.mAccessibilityDelegate);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int indicationBottomMargin = getResources().getDimensionPixelSize(R.dimen.keyguard_indication_margin_bottom);
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) this.mIndicationText.getLayoutParams();
        if (mlp.bottomMargin != indicationBottomMargin) {
            mlp.bottomMargin = indicationBottomMargin;
            this.mIndicationText.setLayoutParams(mlp);
        }
        this.mIndicationText.setTextSize(0, getResources().getDimensionPixelSize(android.R.dimen.config_hoverTapSlop));
    }

    public void setActivityStarter(ActivityStarter activityStarter) {
        this.mActivityStarter = activityStarter;
    }

    public void setFlashlightController(FlashlightController flashlightController) {
        this.mFlashlightController = flashlightController;
    }

    public void setAccessibilityController(AccessibilityController accessibilityController) {
        this.mAccessibilityController = accessibilityController;
        accessibilityController.addStateChangedCallback(this);
    }

    public void setPhoneStatusBar(PhoneStatusBar phoneStatusBar) {
        this.mPhoneStatusBar = phoneStatusBar;
    }

    private Intent getCameraIntent() {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean currentUserHasTrust = updateMonitor.getUserHasTrust(this.mLockPatternUtils.getCurrentUser());
        return (!this.mLockPatternUtils.isSecure() || currentUserHasTrust) ? INSECURE_CAMERA_INTENT : SECURE_CAMERA_INTENT;
    }

    public void updateCameraVisibility() {
        ResolveInfo resolved = this.mContext.getPackageManager().resolveActivityAsUser(getCameraIntent(), 65536, this.mLockPatternUtils.getCurrentUser());
        boolean visible = (isCameraDisabledByDpm() || resolved == null || !getResources().getBoolean(R.bool.config_keyguardShowCameraAffordance)) ? false : true;
        this.mCameraImageView.setVisibility(visible ? 0 : 8);
    }

    private void updatePhoneVisibility() {
        boolean visible = isPhoneVisible();
        this.mPhoneImageView.setVisibility(visible ? 0 : 8);
    }

    private boolean isPhoneVisible() {
        PackageManager pm = this.mContext.getPackageManager();
        return pm.hasSystemFeature("android.hardware.telephony") && pm.resolveActivity(PHONE_INTENT, 0) != null;
    }

    private boolean isCameraDisabledByDpm() {
        DevicePolicyManager dpm = (DevicePolicyManager) getContext().getSystemService("device_policy");
        if (dpm == null) {
            return false;
        }
        try {
            int userId = ActivityManagerNative.getDefault().getCurrentUser().id;
            int disabledFlags = dpm.getKeyguardDisabledFeatures(null, userId);
            boolean disabledBecauseKeyguardSecure = (disabledFlags & 2) != 0 && this.mPhoneStatusBar.isKeyguardSecure();
            return dpm.getCameraDisabled(null) || disabledBecauseKeyguardSecure;
        } catch (RemoteException e) {
            Log.e("PhoneStatusBar/KeyguardBottomAreaView", "Can't get userId", e);
            return false;
        }
    }

    private void watchForCameraPolicyChanges() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        getContext().registerReceiverAsUser(this.mDevicePolicyReceiver, UserHandle.ALL, filter, null, null);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    @Override
    public void onStateChanged(boolean accessibilityEnabled, boolean touchExplorationEnabled) {
        this.mCameraImageView.setClickable(touchExplorationEnabled);
        this.mPhoneImageView.setClickable(touchExplorationEnabled);
        this.mCameraImageView.setFocusable(accessibilityEnabled);
        this.mPhoneImageView.setFocusable(accessibilityEnabled);
        updateLockIconClickability();
    }

    private void updateLockIconClickability() {
        if (this.mAccessibilityController != null) {
            boolean clickToUnlock = this.mAccessibilityController.isTouchExplorationEnabled();
            boolean clickToForceLock = this.mUnlockMethodCache.isTrustManaged() && !this.mAccessibilityController.isAccessibilityEnabled();
            boolean longClickToForceLock = this.mUnlockMethodCache.isTrustManaged() && !clickToForceLock;
            this.mLockIcon.setClickable(clickToForceLock || clickToUnlock);
            this.mLockIcon.setLongClickable(longClickToForceLock);
            this.mLockIcon.setFocusable(this.mAccessibilityController.isAccessibilityEnabled());
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.mCameraImageView) {
            launchCamera();
        } else if (v == this.mPhoneImageView) {
            launchPhone();
        }
        if (v == this.mLockIcon) {
            if (!this.mAccessibilityController.isAccessibilityEnabled()) {
                handleTrustCircleClick();
            } else {
                this.mPhoneStatusBar.animateCollapsePanels(0, true);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        handleTrustCircleClick();
        return true;
    }

    private void handleTrustCircleClick() {
        EventLogTags.writeSysuiLockscreenGesture(6, 0, 0);
        this.mIndicationController.showTransientIndication(R.string.keyguard_indication_trust_disabled);
        this.mLockPatternUtils.requireCredentialEntry(this.mLockPatternUtils.getCurrentUser());
    }

    public void launchCamera() {
        this.mFlashlightController.killFlashlight();
        Intent intent = getCameraIntent();
        boolean wouldLaunchResolverActivity = PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent, this.mLockPatternUtils.getCurrentUser());
        if (intent == SECURE_CAMERA_INTENT && !wouldLaunchResolverActivity) {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } else {
            this.mActivityStarter.startActivity(intent, false);
        }
    }

    public void launchPhone() {
        final TelecomManager tm = TelecomManager.from(this.mContext);
        if (tm.isInCall()) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    tm.showInCallScreen(false);
                }
            });
        } else {
            this.mActivityStarter.startActivity(PHONE_INTENT, false);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (isShown()) {
            this.mTrustDrawable.start();
        } else {
            this.mTrustDrawable.stop();
        }
        if (changedView == this && visibility == 0) {
            updateLockIcon();
            updateCameraVisibility();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mTrustDrawable.stop();
    }

    public void updateLockIcon() {
        int iconRes;
        boolean visible = isShown() && KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn();
        if (visible) {
            this.mTrustDrawable.start();
        } else {
            this.mTrustDrawable.stop();
        }
        if (visible) {
            if (this.mUnlockMethodCache.isFaceUnlockRunning()) {
                iconRes = android.R.drawable.dialog_middle_holo_dark;
            } else {
                iconRes = this.mUnlockMethodCache.isCurrentlyInsecure() ? R.drawable.ic_lock_open_24dp : R.drawable.ic_lock_24dp;
            }
            if (this.mLastUnlockIconRes != iconRes) {
                Drawable icon = this.mContext.getDrawable(iconRes);
                int iconHeight = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_icon_height);
                int iconWidth = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_icon_width);
                if (icon.getIntrinsicHeight() != iconHeight || icon.getIntrinsicWidth() != iconWidth) {
                    icon = new IntrinsicSizeDrawable(icon, iconWidth, iconHeight);
                }
                this.mLockIcon.setImageDrawable(icon);
            }
            boolean trustManaged = this.mUnlockMethodCache.isTrustManaged();
            this.mTrustDrawable.setTrustManaged(trustManaged);
            updateLockIconClickability();
        }
    }

    public KeyguardAffordanceView getPhoneView() {
        return this.mPhoneImageView;
    }

    public KeyguardAffordanceView getCameraView() {
        return this.mCameraImageView;
    }

    public View getPhonePreview() {
        return this.mPhonePreview;
    }

    public View getCameraPreview() {
        return this.mCameraPreview;
    }

    public KeyguardAffordanceView getLockIcon() {
        return this.mLockIcon;
    }

    public View getIndicationView() {
        return this.mIndicationText;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onUnlockMethodStateChanged() {
        updateLockIcon();
        updateCameraVisibility();
    }

    private void inflatePreviews() {
        this.mPhonePreview = this.mPreviewInflater.inflatePreview(PHONE_INTENT);
        this.mCameraPreview = this.mPreviewInflater.inflatePreview(getCameraIntent());
        if (this.mPhonePreview != null) {
            this.mPreviewContainer.addView(this.mPhonePreview);
            this.mPhonePreview.setVisibility(4);
        }
        if (this.mCameraPreview != null) {
            this.mPreviewContainer.addView(this.mCameraPreview);
            this.mCameraPreview.setVisibility(4);
        }
    }

    public void startFinishDozeAnimation() {
        long delay = 0;
        if (this.mPhoneImageView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mPhoneImageView, 0L);
            delay = 0 + 48;
        }
        startFinishDozeAnimationElement(this.mLockIcon, delay);
        long delay2 = delay + 48;
        if (this.mCameraImageView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mCameraImageView, delay2);
        }
        this.mIndicationText.setAlpha(0.0f);
        this.mIndicationText.animate().alpha(1.0f).setInterpolator(this.mLinearOutSlowInInterpolator).setDuration(700L);
    }

    private void startFinishDozeAnimationElement(View element, long delay) {
        element.setAlpha(0.0f);
        element.setTranslationY(element.getHeight() / 2);
        element.animate().alpha(1.0f).translationY(0.0f).setInterpolator(this.mLinearOutSlowInInterpolator).setStartDelay(delay).setDuration(250L);
    }

    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        this.mIndicationController = keyguardIndicationController;
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
