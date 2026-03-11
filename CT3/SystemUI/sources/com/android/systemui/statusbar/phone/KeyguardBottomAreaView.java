package com.android.systemui.statusbar.phone;

import android.app.ActivityManagerNative;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyButton;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.EventLogTags;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.ActivityStarter;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.mediatek.keyguard.Plugin.KeyguardPluginFactory;
import com.mediatek.keyguard.ext.IEmergencyButtonExt;

public class KeyguardBottomAreaView extends FrameLayout implements View.OnClickListener, UnlockMethodCache.OnUnlockMethodChangedListener, AccessibilityController.AccessibilityStateChangedCallback, View.OnLongClickListener {
    private AccessibilityController mAccessibilityController;
    private View.AccessibilityDelegate mAccessibilityDelegate;
    private ActivityStarter mActivityStarter;
    private AssistManager mAssistManager;
    private KeyguardAffordanceView mCameraImageView;
    private View mCameraPreview;
    private final BroadcastReceiver mDevicePolicyReceiver;
    private EmergencyButton mEmergencyButton;
    private IEmergencyButtonExt mEmergencyButtonExt;
    private FlashlightController mFlashlightController;
    private KeyguardIndicationController mIndicationController;
    private TextView mIndicationText;
    private KeyguardAffordanceView mLeftAffordanceView;
    private boolean mLeftIsVoiceAssist;
    private View mLeftPreview;
    private LockIcon mLockIcon;
    private LockPatternUtils mLockPatternUtils;
    private PhoneStatusBar mPhoneStatusBar;
    private ViewGroup mPreviewContainer;
    private PreviewInflater mPreviewInflater;
    private boolean mPrewarmBound;
    private final ServiceConnection mPrewarmConnection;
    private Messenger mPrewarmMessenger;
    private UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private boolean mUserSetupComplete;
    private static final Intent SECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
    public static final Intent INSECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA");
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
        this.mPrewarmConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                KeyguardBottomAreaView.this.mPrewarmMessenger = new Messenger(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                KeyguardBottomAreaView.this.mPrewarmMessenger = null;
            }
        };
        this.mAccessibilityDelegate = new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                String label = null;
                if (host == KeyguardBottomAreaView.this.mLockIcon) {
                    label = KeyguardBottomAreaView.this.getResources().getString(R.string.unlock_label);
                } else if (host == KeyguardBottomAreaView.this.mCameraImageView) {
                    label = KeyguardBottomAreaView.this.getResources().getString(R.string.camera_label);
                } else if (host == KeyguardBottomAreaView.this.mLeftAffordanceView) {
                    if (KeyguardBottomAreaView.this.mLeftIsVoiceAssist) {
                        label = KeyguardBottomAreaView.this.getResources().getString(R.string.voice_assist_label);
                    } else {
                        label = KeyguardBottomAreaView.this.getResources().getString(R.string.phone_label);
                    }
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
                    if (host == KeyguardBottomAreaView.this.mCameraImageView) {
                        KeyguardBottomAreaView.this.launchCamera("lockscreen_affordance");
                        return true;
                    }
                    if (host == KeyguardBottomAreaView.this.mLeftAffordanceView) {
                        KeyguardBottomAreaView.this.launchLeftAffordance();
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
            public void onStartedWakingUp() {
                KeyguardBottomAreaView.this.mLockIcon.setDeviceInteractive(true);
            }

            @Override
            public void onFinishedGoingToSleep(int why) {
                KeyguardBottomAreaView.this.mLockIcon.setDeviceInteractive(false);
            }

            @Override
            public void onScreenTurnedOn() {
                KeyguardBottomAreaView.this.mLockIcon.setScreenOn(true);
            }

            @Override
            public void onScreenTurnedOff() {
                KeyguardBottomAreaView.this.mLockIcon.setScreenOn(false);
            }

            @Override
            public void onKeyguardVisibilityChanged(boolean showing) {
                KeyguardBottomAreaView.this.mLockIcon.update();
            }

            @Override
            public void onFingerprintRunningStateChanged(boolean running) {
                KeyguardBottomAreaView.this.mLockIcon.update();
            }

            @Override
            public void onStrongAuthStateChanged(int userId) {
                KeyguardBottomAreaView.this.mLockIcon.update();
            }
        };
        this.mEmergencyButtonExt = KeyguardPluginFactory.getEmergencyButtonExt(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPreviewContainer = (ViewGroup) findViewById(R.id.preview_container);
        this.mCameraImageView = (KeyguardAffordanceView) findViewById(R.id.camera_button);
        this.mLeftAffordanceView = (KeyguardAffordanceView) findViewById(R.id.left_button);
        this.mLockIcon = (LockIcon) findViewById(R.id.lock_icon);
        this.mIndicationText = (TextView) findViewById(R.id.keyguard_indication_text);
        this.mEmergencyButton = (EmergencyButton) findViewById(R.id.notification_keyguard_emergency_call_button);
        watchForCameraPolicyChanges();
        updateCameraVisibility();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(getContext());
        this.mUnlockMethodCache.addListener(this);
        this.mLockIcon.update();
        setClipChildren(false);
        setClipToPadding(false);
        this.mPreviewInflater = new PreviewInflater(this.mContext, new LockPatternUtils(this.mContext));
        inflateCameraPreview();
        this.mLockIcon.setOnClickListener(this);
        this.mLockIcon.setOnLongClickListener(this);
        this.mCameraImageView.setOnClickListener(this);
        this.mLeftAffordanceView.setOnClickListener(this);
        initAccessibility();
    }

    private void initAccessibility() {
        this.mLockIcon.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mLeftAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mCameraImageView.setAccessibilityDelegate(this.mAccessibilityDelegate);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getResources().getDimensionPixelSize(R.dimen.keyguard_indication_margin_bottom);
        this.mIndicationText.setTextSize(0, getResources().getDimensionPixelSize(android.R.dimen.config_screenBrightnessSettingMinimumFloat));
        getRightView().setContentDescription(getResources().getString(R.string.accessibility_camera_button));
        getLockIcon().setContentDescription(getResources().getString(R.string.accessibility_unlock_button));
        getLeftView().setContentDescription(getResources().getString(R.string.accessibility_phone_button));
        ViewGroup.LayoutParams lp = this.mCameraImageView.getLayoutParams();
        lp.width = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_width);
        lp.height = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_height);
        this.mCameraImageView.setLayoutParams(lp);
        this.mCameraImageView.setImageDrawable(this.mContext.getDrawable(R.drawable.ic_camera_alt_24dp));
        ViewGroup.LayoutParams lp2 = this.mLockIcon.getLayoutParams();
        lp2.width = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_width);
        lp2.height = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_height);
        this.mLockIcon.setLayoutParams(lp2);
        this.mLockIcon.update(true);
        ViewGroup.LayoutParams lp3 = this.mLeftAffordanceView.getLayoutParams();
        lp3.width = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_width);
        lp3.height = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_height);
        this.mLeftAffordanceView.setLayoutParams(lp3);
        updateLeftAffordanceIcon();
    }

    public void setActivityStarter(ActivityStarter activityStarter) {
        this.mActivityStarter = activityStarter;
    }

    public void setFlashlightController(FlashlightController flashlightController) {
        this.mFlashlightController = flashlightController;
    }

    public void setAccessibilityController(AccessibilityController accessibilityController) {
        this.mAccessibilityController = accessibilityController;
        this.mLockIcon.setAccessibilityController(accessibilityController);
        accessibilityController.addStateChangedCallback(this);
    }

    public void setPhoneStatusBar(PhoneStatusBar phoneStatusBar) {
        this.mPhoneStatusBar = phoneStatusBar;
        updateCameraVisibility();
    }

    public void setUserSetupComplete(boolean userSetupComplete) {
        this.mUserSetupComplete = userSetupComplete;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
    }

    private Intent getCameraIntent() {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean canSkipBouncer = updateMonitor.getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser());
        boolean secure = this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser());
        return (!secure || canSkipBouncer) ? INSECURE_CAMERA_INTENT : SECURE_CAMERA_INTENT;
    }

    public ResolveInfo resolveCameraIntent() {
        return this.mContext.getPackageManager().resolveActivityAsUser(getCameraIntent(), 65536, KeyguardUpdateMonitor.getCurrentUser());
    }

    public void updateCameraVisibility() {
        boolean z;
        if (this.mCameraImageView == null) {
            return;
        }
        ResolveInfo resolved = resolveCameraIntent();
        boolean isCameraDisabled = (this.mPhoneStatusBar == null || this.mPhoneStatusBar.isCameraAllowedByAdmin()) ? false : true;
        if (isCameraDisabled || resolved == null || !getResources().getBoolean(R.bool.config_keyguardShowCameraAffordance)) {
            z = false;
        } else {
            z = this.mUserSetupComplete;
        }
        this.mCameraImageView.setVisibility(z ? 0 : 8);
    }

    private void updateLeftAffordanceIcon() {
        int drawableId;
        int contentDescription;
        this.mLeftIsVoiceAssist = canLaunchVoiceAssist();
        boolean visible = this.mUserSetupComplete;
        if (this.mLeftIsVoiceAssist) {
            drawableId = R.drawable.ic_mic_26dp;
            contentDescription = R.string.accessibility_voice_assist_button;
        } else {
            visible &= isPhoneVisible();
            drawableId = R.drawable.ic_phone_24dp;
            contentDescription = R.string.accessibility_phone_button;
        }
        this.mLeftAffordanceView.setVisibility(visible ? 0 : 8);
        this.mLeftAffordanceView.setImageDrawable(this.mContext.getDrawable(drawableId));
        this.mLeftAffordanceView.setContentDescription(this.mContext.getString(contentDescription));
    }

    public boolean isLeftVoiceAssist() {
        return this.mLeftIsVoiceAssist;
    }

    private boolean isPhoneVisible() {
        PackageManager pm = this.mContext.getPackageManager();
        return pm.hasSystemFeature("android.hardware.telephony") && pm.resolveActivity(PHONE_INTENT, 0) != null;
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
        this.mLeftAffordanceView.setClickable(touchExplorationEnabled);
        this.mCameraImageView.setFocusable(accessibilityEnabled);
        this.mLeftAffordanceView.setFocusable(accessibilityEnabled);
        this.mLockIcon.update();
    }

    @Override
    public void onClick(View v) {
        if (v == this.mCameraImageView) {
            launchCamera("lockscreen_affordance");
        } else if (v == this.mLeftAffordanceView) {
            launchLeftAffordance();
        }
        if (v != this.mLockIcon) {
            return;
        }
        if (!this.mAccessibilityController.isAccessibilityEnabled()) {
            handleTrustCircleClick();
        } else {
            this.mPhoneStatusBar.animateCollapsePanels(0, true);
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
        this.mLockPatternUtils.requireCredentialEntry(KeyguardUpdateMonitor.getCurrentUser());
    }

    public void bindCameraPrewarmService() {
        String clazz;
        Intent intent = getCameraIntent();
        ActivityInfo targetInfo = PreviewInflater.getTargetActivityInfo(this.mContext, intent, KeyguardUpdateMonitor.getCurrentUser());
        if (targetInfo == null || targetInfo.metaData == null || (clazz = targetInfo.metaData.getString("android.media.still_image_camera_preview_service")) == null) {
            return;
        }
        Intent serviceIntent = new Intent();
        serviceIntent.setClassName(targetInfo.packageName, clazz);
        serviceIntent.setAction("android.service.media.CameraPrewarmService.ACTION_PREWARM");
        try {
            if (!getContext().bindServiceAsUser(serviceIntent, this.mPrewarmConnection, 67108865, new UserHandle(-2))) {
                return;
            }
            this.mPrewarmBound = true;
        } catch (SecurityException e) {
            Log.w("PhoneStatusBar/KeyguardBottomAreaView", "Unable to bind to prewarm service package=" + targetInfo.packageName + " class=" + clazz, e);
        }
    }

    public void unbindCameraPrewarmService(boolean launched) {
        if (!this.mPrewarmBound) {
            return;
        }
        if (this.mPrewarmMessenger != null && launched) {
            try {
                this.mPrewarmMessenger.send(Message.obtain((Handler) null, 1));
            } catch (RemoteException e) {
                Log.w("PhoneStatusBar/KeyguardBottomAreaView", "Error sending camera fired message", e);
            }
        }
        this.mContext.unbindService(this.mPrewarmConnection);
        this.mPrewarmBound = false;
    }

    public void launchCamera(String source) {
        final Intent intent = getCameraIntent();
        intent.putExtra("com.android.systemui.camera_launch_source", source);
        boolean wouldLaunchResolverActivity = PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent, KeyguardUpdateMonitor.getCurrentUser());
        if (intent == SECURE_CAMERA_INTENT && !wouldLaunchResolverActivity) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    int result = -6;
                    try {
                        intent.setFlags(67108864);
                        result = ActivityManagerNative.getDefault().startActivityAsUser((IApplicationThread) null, KeyguardBottomAreaView.this.getContext().getBasePackageName(), intent, intent.resolveTypeIfNeeded(KeyguardBottomAreaView.this.getContext().getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, (Bundle) null, UserHandle.CURRENT.getIdentifier());
                    } catch (RemoteException e) {
                        Log.w("PhoneStatusBar/KeyguardBottomAreaView", "Unable to start camera activity", e);
                    }
                    KeyguardBottomAreaView.this.mActivityStarter.preventNextAnimation();
                    final boolean launched = KeyguardBottomAreaView.isSuccessfulLaunch(result);
                    KeyguardBottomAreaView.this.post(new Runnable() {
                        @Override
                        public void run() {
                            KeyguardBottomAreaView.this.unbindCameraPrewarmService(launched);
                        }
                    });
                }
            });
        } else {
            this.mActivityStarter.startActivity(intent, false, new ActivityStarter.Callback() {
                @Override
                public void onActivityStarted(int resultCode) {
                    KeyguardBottomAreaView.this.unbindCameraPrewarmService(KeyguardBottomAreaView.isSuccessfulLaunch(resultCode));
                }
            });
        }
    }

    public static boolean isSuccessfulLaunch(int result) {
        return result == 0 || result == 3 || result == 2;
    }

    public void launchLeftAffordance() {
        if (this.mLeftIsVoiceAssist) {
            launchVoiceAssist();
        } else {
            launchPhone();
        }
    }

    private void launchVoiceAssist() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                KeyguardBottomAreaView.this.mAssistManager.launchVoiceAssistFromKeyguard();
                KeyguardBottomAreaView.this.mActivityStarter.preventNextAnimation();
            }
        };
        if (this.mPhoneStatusBar.isKeyguardCurrentlySecure()) {
            AsyncTask.execute(runnable);
        } else {
            this.mPhoneStatusBar.executeRunnableDismissingKeyguard(runnable, null, false, false, true);
        }
    }

    private boolean canLaunchVoiceAssist() {
        return this.mAssistManager.canVoiceAssistBeLaunchedFromKeyguard();
    }

    private void launchPhone() {
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
        if (changedView != this || visibility != 0) {
            return;
        }
        this.mLockIcon.update();
        updateCameraVisibility();
    }

    public KeyguardAffordanceView getLeftView() {
        return this.mLeftAffordanceView;
    }

    public KeyguardAffordanceView getRightView() {
        return this.mCameraImageView;
    }

    public View getLeftPreview() {
        return this.mLeftPreview;
    }

    public View getRightPreview() {
        return this.mCameraPreview;
    }

    public LockIcon getLockIcon() {
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
        this.mLockIcon.update();
        updateCameraVisibility();
    }

    private void inflateCameraPreview() {
        this.mCameraPreview = this.mPreviewInflater.inflatePreview(getCameraIntent());
        if (this.mCameraPreview == null) {
            return;
        }
        this.mPreviewContainer.addView(this.mCameraPreview);
        this.mCameraPreview.setVisibility(4);
    }

    private void updateLeftPreview() {
        View previewBefore = this.mLeftPreview;
        if (previewBefore != null) {
            this.mPreviewContainer.removeView(previewBefore);
        }
        if (this.mLeftIsVoiceAssist) {
            this.mLeftPreview = this.mPreviewInflater.inflatePreviewFromService(this.mAssistManager.getVoiceInteractorComponentName());
        } else {
            this.mLeftPreview = this.mPreviewInflater.inflatePreview(PHONE_INTENT);
        }
        if (this.mLeftPreview == null) {
            return;
        }
        this.mPreviewContainer.addView(this.mLeftPreview);
        this.mLeftPreview.setVisibility(4);
    }

    public void startFinishDozeAnimation() {
        long delay = 0;
        if (this.mLeftAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mLeftAffordanceView, 0L);
            delay = 48;
        }
        startFinishDozeAnimationElement(this.mLockIcon, delay);
        long delay2 = delay + 48;
        if (this.mCameraImageView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mCameraImageView, delay2);
        }
        this.mIndicationText.setAlpha(0.0f);
        this.mIndicationText.animate().alpha(1.0f).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setDuration(700L);
    }

    private void startFinishDozeAnimationElement(View element, long delay) {
        element.setAlpha(0.0f);
        element.setTranslationY(element.getHeight() / 2);
        element.animate().alpha(1.0f).translationY(0.0f).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(delay).setDuration(250L);
    }

    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        this.mIndicationController = keyguardIndicationController;
    }

    public void setAssistManager(AssistManager assistManager) {
        this.mAssistManager = assistManager;
        updateLeftAffordance();
    }

    public void updateLeftAffordance() {
        updateLeftAffordanceIcon();
        updateLeftPreview();
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        this.mEmergencyButtonExt.setEmergencyButtonVisibility(this.mEmergencyButton, alpha);
    }
}
