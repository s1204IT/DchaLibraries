package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityOptions;
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
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyButton;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.IntentButtonProvider;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.tuner.LockscreenFragment;
import com.android.systemui.tuner.TunerService;
import com.mediatek.keyguard.ext.IEmergencyButtonExt;
import com.mediatek.keyguard.ext.OpKeyguardCustomizationFactoryBase;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeyguardBottomAreaView extends FrameLayout implements View.OnClickListener, View.OnLongClickListener, UnlockMethodCache.OnUnlockMethodChangedListener, AccessibilityController.AccessibilityStateChangedCallback {
    private AccessibilityController mAccessibilityController;
    private View.AccessibilityDelegate mAccessibilityDelegate;
    private ActivityStarter mActivityStarter;
    private KeyguardAffordanceHelper mAffordanceHelper;
    private AssistManager mAssistManager;
    private int mBurnInXOffset;
    private View mCameraPreview;
    private float mDarkAmount;
    private final BroadcastReceiver mDevicePolicyReceiver;
    private boolean mDozing;
    private EmergencyButton mEmergencyButton;
    private IEmergencyButtonExt mEmergencyButtonExt;
    private TextView mEnterpriseDisclosure;
    private FlashlightController mFlashlightController;
    private ViewGroup mIndicationArea;
    private int mIndicationBottomMargin;
    private int mIndicationBottomMarginAmbient;
    private KeyguardIndicationController mIndicationController;
    private TextView mIndicationText;
    private KeyguardAffordanceView mLeftAffordanceView;
    private Drawable mLeftAssistIcon;
    private IntentButtonProvider.IntentButton mLeftButton;
    private String mLeftButtonStr;
    private ExtensionController.Extension<IntentButtonProvider.IntentButton> mLeftExtension;
    private boolean mLeftIsVoiceAssist;
    private View mLeftPreview;
    private LockIcon mLockIcon;
    private LockPatternUtils mLockPatternUtils;
    private LockscreenGestureLogger mLockscreenGestureLogger;
    private ViewGroup mOverlayContainer;
    private ViewGroup mPreviewContainer;
    private PreviewInflater mPreviewInflater;
    private boolean mPrewarmBound;
    private final ServiceConnection mPrewarmConnection;
    private Messenger mPrewarmMessenger;
    private KeyguardAffordanceView mRightAffordanceView;
    private IntentButtonProvider.IntentButton mRightButton;
    private String mRightButtonStr;
    private ExtensionController.Extension<IntentButtonProvider.IntentButton> mRightExtension;
    private StatusBar mStatusBar;
    private UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private boolean mUserSetupComplete;
    private static final Intent SECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
    public static final Intent INSECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA");
    private static final Intent PHONE_INTENT = new Intent("android.intent.action.DIAL");

    public KeyguardBottomAreaView(Context context) {
        this(context, null);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mPrewarmConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                KeyguardBottomAreaView.this.mPrewarmMessenger = new Messenger(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                KeyguardBottomAreaView.this.mPrewarmMessenger = null;
            }
        };
        this.mRightButton = new DefaultRightButton();
        this.mLeftButton = new DefaultLeftButton();
        this.mLockscreenGestureLogger = new LockscreenGestureLogger();
        this.mAccessibilityDelegate = new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                String string;
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                if (view != KeyguardBottomAreaView.this.mLockIcon) {
                    if (view != KeyguardBottomAreaView.this.mRightAffordanceView) {
                        if (view == KeyguardBottomAreaView.this.mLeftAffordanceView) {
                            if (KeyguardBottomAreaView.this.mLeftIsVoiceAssist) {
                                string = KeyguardBottomAreaView.this.getResources().getString(R.string.voice_assist_label);
                            } else {
                                string = KeyguardBottomAreaView.this.getResources().getString(R.string.phone_label);
                            }
                        } else {
                            string = null;
                        }
                    } else {
                        string = KeyguardBottomAreaView.this.getResources().getString(R.string.camera_label);
                    }
                } else {
                    string = KeyguardBottomAreaView.this.getResources().getString(R.string.unlock_label);
                }
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, string));
            }

            @Override
            public boolean performAccessibilityAction(View view, int i3, Bundle bundle) {
                if (i3 == 16) {
                    if (view == KeyguardBottomAreaView.this.mLockIcon) {
                        KeyguardBottomAreaView.this.mStatusBar.animateCollapsePanels(2, true);
                        return true;
                    }
                    if (view != KeyguardBottomAreaView.this.mRightAffordanceView) {
                        if (view == KeyguardBottomAreaView.this.mLeftAffordanceView) {
                            KeyguardBottomAreaView.this.launchLeftAffordance();
                            return true;
                        }
                    } else {
                        KeyguardBottomAreaView.this.launchCamera("lockscreen_affordance");
                        return true;
                    }
                }
                return super.performAccessibilityAction(view, i3, bundle);
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
            public void onUserSwitchComplete(int i3) {
                KeyguardBottomAreaView.this.updateCameraVisibility();
            }

            @Override
            public void onStartedWakingUp() {
                KeyguardBottomAreaView.this.mLockIcon.setDeviceInteractive(true);
            }

            @Override
            public void onFinishedGoingToSleep(int i3) {
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
            public void onKeyguardVisibilityChanged(boolean z) {
                KeyguardBottomAreaView.this.mLockIcon.update();
            }

            @Override
            public void onFingerprintRunningStateChanged(boolean z) {
                KeyguardBottomAreaView.this.mLockIcon.update();
            }

            @Override
            public void onStrongAuthStateChanged(int i3) {
                KeyguardBottomAreaView.this.mLockIcon.update();
            }

            @Override
            public void onUserUnlocked() {
                KeyguardBottomAreaView.this.inflateCameraPreview();
                KeyguardBottomAreaView.this.updateCameraVisibility();
                KeyguardBottomAreaView.this.updateLeftAffordance();
            }
        };
        this.mEmergencyButtonExt = OpKeyguardCustomizationFactoryBase.getOpFactory(context).makeEmergencyButton();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPreviewContainer = (ViewGroup) findViewById(R.id.preview_container);
        this.mOverlayContainer = (ViewGroup) findViewById(R.id.overlay_container);
        this.mRightAffordanceView = (KeyguardAffordanceView) findViewById(R.id.camera_button);
        this.mLeftAffordanceView = (KeyguardAffordanceView) findViewById(R.id.left_button);
        this.mLockIcon = (LockIcon) findViewById(R.id.lock_icon);
        this.mIndicationArea = (ViewGroup) findViewById(R.id.keyguard_indication_area);
        this.mEmergencyButton = (EmergencyButton) findViewById(R.id.notification_keyguard_emergency_call_button);
        this.mEnterpriseDisclosure = (TextView) findViewById(R.id.keyguard_indication_enterprise_disclosure);
        this.mIndicationText = (TextView) findViewById(R.id.keyguard_indication_text);
        this.mIndicationBottomMargin = getResources().getDimensionPixelSize(R.dimen.keyguard_indication_margin_bottom);
        this.mIndicationBottomMarginAmbient = getResources().getDimensionPixelSize(R.dimen.keyguard_indication_margin_bottom_ambient);
        updateCameraVisibility();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(getContext());
        this.mUnlockMethodCache.addListener(this);
        KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mLockIcon.setScreenOn(keyguardUpdateMonitor.isScreenOn());
        this.mLockIcon.setDeviceInteractive(keyguardUpdateMonitor.isDeviceInteractive());
        this.mLockIcon.update();
        setClipChildren(false);
        setClipToPadding(false);
        this.mPreviewInflater = new PreviewInflater(this.mContext, new LockPatternUtils(this.mContext));
        inflateCameraPreview();
        this.mLockIcon.setOnClickListener(this);
        this.mLockIcon.setOnLongClickListener(this);
        this.mRightAffordanceView.setOnClickListener(this);
        this.mLeftAffordanceView.setOnClickListener(this);
        initAccessibility();
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mFlashlightController = (FlashlightController) Dependency.get(FlashlightController.class);
        this.mAccessibilityController = (AccessibilityController) Dependency.get(AccessibilityController.class);
        this.mAssistManager = (AssistManager) Dependency.get(AssistManager.class);
        this.mLockIcon.setAccessibilityController(this.mAccessibilityController);
        updateLeftAffordance();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAccessibilityController.addStateChangedCallback(this);
        this.mRightExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(IntentButtonProvider.IntentButton.class).withPlugin(IntentButtonProvider.class, "com.android.systemui.action.PLUGIN_LOCKSCREEN_RIGHT_BUTTON", new ExtensionController.PluginConverter() {
            @Override
            public final Object getInterfaceFromPlugin(Object obj) {
                return ((IntentButtonProvider) obj).getIntentButton();
            }
        }).withTunerFactory(new LockscreenFragment.LockButtonFactory(this.mContext, "sysui_keyguard_right")).withDefault(new Supplier() {
            @Override
            public final Object get() {
                return KeyguardBottomAreaView.lambda$onAttachedToWindow$1(this.f$0);
            }
        }).withCallback(new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.setRightButton((IntentButtonProvider.IntentButton) obj);
            }
        }).build();
        this.mLeftExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(IntentButtonProvider.IntentButton.class).withPlugin(IntentButtonProvider.class, "com.android.systemui.action.PLUGIN_LOCKSCREEN_LEFT_BUTTON", new ExtensionController.PluginConverter() {
            @Override
            public final Object getInterfaceFromPlugin(Object obj) {
                return ((IntentButtonProvider) obj).getIntentButton();
            }
        }).withTunerFactory(new LockscreenFragment.LockButtonFactory(this.mContext, "sysui_keyguard_left")).withDefault(new Supplier() {
            @Override
            public final Object get() {
                return KeyguardBottomAreaView.lambda$onAttachedToWindow$4(this.f$0);
            }
        }).withCallback(new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.setLeftButton((IntentButtonProvider.IntentButton) obj);
            }
        }).build();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        getContext().registerReceiverAsUser(this.mDevicePolicyReceiver, UserHandle.ALL, intentFilter, null, null);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    public static IntentButtonProvider.IntentButton lambda$onAttachedToWindow$1(KeyguardBottomAreaView keyguardBottomAreaView) {
        return new DefaultRightButton();
    }

    public static IntentButtonProvider.IntentButton lambda$onAttachedToWindow$4(KeyguardBottomAreaView keyguardBottomAreaView) {
        return new DefaultLeftButton();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAccessibilityController.removeStateChangedCallback(this);
        this.mRightExtension.destroy();
        this.mLeftExtension.destroy();
        getContext().unregisterReceiver(this.mDevicePolicyReceiver);
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallback);
    }

    private void initAccessibility() {
        this.mLockIcon.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mLeftAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mRightAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mIndicationBottomMargin = getResources().getDimensionPixelSize(R.dimen.keyguard_indication_margin_bottom);
        this.mIndicationBottomMarginAmbient = getResources().getDimensionPixelSize(R.dimen.keyguard_indication_margin_bottom_ambient);
        this.mEnterpriseDisclosure.setTextSize(0, getResources().getDimensionPixelSize(android.R.dimen.indeterminate_progress_alpha_01));
        this.mIndicationText.setTextSize(0, getResources().getDimensionPixelSize(android.R.dimen.indeterminate_progress_alpha_01));
        ViewGroup.LayoutParams layoutParams = this.mRightAffordanceView.getLayoutParams();
        layoutParams.width = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_width);
        layoutParams.height = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_height);
        this.mRightAffordanceView.setLayoutParams(layoutParams);
        updateRightAffordanceIcon();
        ViewGroup.LayoutParams layoutParams2 = this.mLockIcon.getLayoutParams();
        layoutParams2.width = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_width);
        layoutParams2.height = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_height);
        this.mLockIcon.setLayoutParams(layoutParams2);
        this.mLockIcon.setContentDescription(getContext().getText(R.string.accessibility_unlock_button));
        this.mLockIcon.update(true);
        ViewGroup.LayoutParams layoutParams3 = this.mLeftAffordanceView.getLayoutParams();
        layoutParams3.width = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_width);
        layoutParams3.height = getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_height);
        this.mLeftAffordanceView.setLayoutParams(layoutParams3);
        updateLeftAffordanceIcon();
    }

    private void updateRightAffordanceIcon() {
        IntentButtonProvider.IntentButton.IconState icon = this.mRightButton.getIcon();
        this.mRightAffordanceView.setVisibility((this.mDozing || !icon.isVisible) ? 8 : 0);
        this.mRightAffordanceView.setImageDrawable(icon.drawable, icon.tint);
        this.mRightAffordanceView.setContentDescription(icon.contentDescription);
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
        updateCameraVisibility();
    }

    public void setAffordanceHelper(KeyguardAffordanceHelper keyguardAffordanceHelper) {
        this.mAffordanceHelper = keyguardAffordanceHelper;
    }

    public void setUserSetupComplete(boolean z) {
        this.mUserSetupComplete = z;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
    }

    private Intent getCameraIntent() {
        return this.mRightButton.getIntent();
    }

    public ResolveInfo resolveCameraIntent() {
        return this.mContext.getPackageManager().resolveActivityAsUser(getCameraIntent(), 65536, KeyguardUpdateMonitor.getCurrentUser());
    }

    private void updateCameraVisibility() {
        if (this.mRightAffordanceView == null) {
            return;
        }
        this.mRightAffordanceView.setVisibility((this.mDozing || !this.mRightButton.getIcon().isVisible) ? 8 : 0);
    }

    private void updateLeftAffordanceIcon() {
        IntentButtonProvider.IntentButton.IconState icon = this.mLeftButton.getIcon();
        this.mLeftAffordanceView.setVisibility((this.mDozing || !icon.isVisible) ? 8 : 0);
        this.mLeftAffordanceView.setImageDrawable(icon.drawable, icon.tint);
        this.mLeftAffordanceView.setContentDescription(icon.contentDescription);
    }

    public boolean isLeftVoiceAssist() {
        return this.mLeftIsVoiceAssist;
    }

    private boolean isPhoneVisible() {
        PackageManager packageManager = this.mContext.getPackageManager();
        return packageManager.hasSystemFeature("android.hardware.telephony") && packageManager.resolveActivity(PHONE_INTENT, 0) != null;
    }

    @Override
    public void onStateChanged(boolean z, boolean z2) {
        this.mRightAffordanceView.setClickable(z2);
        this.mLeftAffordanceView.setClickable(z2);
        this.mRightAffordanceView.setFocusable(z);
        this.mLeftAffordanceView.setFocusable(z);
        this.mLockIcon.update();
    }

    @Override
    public void onClick(View view) {
        if (view == this.mRightAffordanceView) {
            launchCamera("lockscreen_affordance");
        } else if (view == this.mLeftAffordanceView) {
            launchLeftAffordance();
        }
        if (view == this.mLockIcon) {
            if (!this.mAccessibilityController.isAccessibilityEnabled()) {
                handleTrustCircleClick();
            } else {
                this.mStatusBar.animateCollapsePanels(0, true);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        handleTrustCircleClick();
        return true;
    }

    private void handleTrustCircleClick() {
        this.mLockscreenGestureLogger.write(191, 0, 0);
        this.mIndicationController.showTransientIndication(R.string.keyguard_indication_trust_disabled);
        this.mLockPatternUtils.requireCredentialEntry(KeyguardUpdateMonitor.getCurrentUser());
    }

    public void bindCameraPrewarmService() {
        String string;
        ActivityInfo targetActivityInfo = PreviewInflater.getTargetActivityInfo(this.mContext, getCameraIntent(), KeyguardUpdateMonitor.getCurrentUser(), true);
        if (targetActivityInfo != null && targetActivityInfo.metaData != null && (string = targetActivityInfo.metaData.getString("android.media.still_image_camera_preview_service")) != null) {
            Intent intent = new Intent();
            intent.setClassName(targetActivityInfo.packageName, string);
            intent.setAction("android.service.media.CameraPrewarmService.ACTION_PREWARM");
            try {
                if (getContext().bindServiceAsUser(intent, this.mPrewarmConnection, 67108865, new UserHandle(-2))) {
                    this.mPrewarmBound = true;
                }
            } catch (SecurityException e) {
                Log.w("StatusBar/KeyguardBottomAreaView", "Unable to bind to prewarm service package=" + targetActivityInfo.packageName + " class=" + string, e);
            }
        }
    }

    public void unbindCameraPrewarmService(boolean z) {
        if (this.mPrewarmBound) {
            if (this.mPrewarmMessenger != null && z) {
                try {
                    this.mPrewarmMessenger.send(Message.obtain((Handler) null, 1));
                } catch (RemoteException e) {
                    Log.w("StatusBar/KeyguardBottomAreaView", "Error sending camera fired message", e);
                }
            }
            this.mContext.unbindService(this.mPrewarmConnection);
            this.mPrewarmBound = false;
        }
    }

    public void launchCamera(String str) {
        final Intent cameraIntent = getCameraIntent();
        cameraIntent.putExtra("com.android.systemui.camera_launch_source", str);
        boolean zWouldLaunchResolverActivity = PreviewInflater.wouldLaunchResolverActivity(this.mContext, cameraIntent, KeyguardUpdateMonitor.getCurrentUser());
        if (cameraIntent == SECURE_CAMERA_INTENT && !zWouldLaunchResolverActivity) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    int iStartActivityAsUser;
                    ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
                    activityOptionsMakeBasic.setDisallowEnterPictureInPictureWhileLaunching(true);
                    activityOptionsMakeBasic.setRotationAnimationHint(3);
                    try {
                        cameraIntent.setFlags(67108864);
                        iStartActivityAsUser = ActivityManager.getService().startActivityAsUser((IApplicationThread) null, KeyguardBottomAreaView.this.getContext().getBasePackageName(), cameraIntent, cameraIntent.resolveTypeIfNeeded(KeyguardBottomAreaView.this.getContext().getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, activityOptionsMakeBasic.toBundle(), UserHandle.CURRENT.getIdentifier());
                    } catch (RemoteException e) {
                        Log.w("StatusBar/KeyguardBottomAreaView", "Unable to start camera activity", e);
                        iStartActivityAsUser = -96;
                    }
                    final boolean zIsSuccessfulLaunch = KeyguardBottomAreaView.isSuccessfulLaunch(iStartActivityAsUser);
                    KeyguardBottomAreaView.this.post(new Runnable() {
                        @Override
                        public void run() {
                            KeyguardBottomAreaView.this.unbindCameraPrewarmService(zIsSuccessfulLaunch);
                        }
                    });
                }
            });
        } else {
            this.mActivityStarter.startActivity(cameraIntent, false, new ActivityStarter.Callback() {
                @Override
                public void onActivityStarted(int i) {
                    KeyguardBottomAreaView.this.unbindCameraPrewarmService(KeyguardBottomAreaView.isSuccessfulLaunch(i));
                }
            });
        }
    }

    public void setDarkAmount(float f) {
        if (f == this.mDarkAmount) {
            return;
        }
        this.mDarkAmount = f;
        if (f == 0.0f) {
            this.mIndicationBottomMarginAmbient = getResources().getDimensionPixelSize(R.dimen.keyguard_indication_margin_bottom_ambient) + ((int) (Math.random() * ((double) this.mIndicationText.getTextSize())));
        }
        this.mIndicationArea.setAlpha(MathUtils.lerp(1.0f, 0.7f, f));
        this.mIndicationArea.setTranslationY(MathUtils.lerp(0.0f, this.mIndicationBottomMargin - this.mIndicationBottomMarginAmbient, f));
    }

    private static boolean isSuccessfulLaunch(int i) {
        return i == 0 || i == 3 || i == 2;
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
            }
        };
        if (this.mStatusBar.isKeyguardCurrentlySecure()) {
            AsyncTask.execute(runnable);
        } else {
            this.mStatusBar.executeRunnableDismissingKeyguard(runnable, null, (TextUtils.isEmpty(this.mRightButtonStr) || ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_keyguard_right_unlock", 1) == 0) ? false : true, false, true);
        }
    }

    private boolean canLaunchVoiceAssist() {
        return this.mAssistManager.canVoiceAssistBeLaunchedFromKeyguard();
    }

    private void launchPhone() {
        final TelecomManager telecomManagerFrom = TelecomManager.from(this.mContext);
        if (telecomManagerFrom.isInCall()) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    telecomManagerFrom.showInCallScreen(false);
                }
            });
        } else {
            this.mActivityStarter.startActivity(this.mLeftButton.getIntent(), (TextUtils.isEmpty(this.mLeftButtonStr) || ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_keyguard_left_unlock", 1) == 0) ? false : true);
        }
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (view == this && i == 0) {
            this.mLockIcon.update();
            updateCameraVisibility();
        }
    }

    public KeyguardAffordanceView getLeftView() {
        return this.mLeftAffordanceView;
    }

    public KeyguardAffordanceView getRightView() {
        return this.mRightAffordanceView;
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

    public View getIndicationArea() {
        return this.mIndicationArea;
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
        boolean z;
        View view = this.mCameraPreview;
        if (view != null) {
            this.mPreviewContainer.removeView(view);
            z = view.getVisibility() == 0;
        }
        this.mCameraPreview = this.mPreviewInflater.inflatePreview(getCameraIntent());
        if (this.mCameraPreview != null) {
            this.mPreviewContainer.addView(this.mCameraPreview);
            this.mCameraPreview.setVisibility(z ? 0 : 4);
        }
        if (this.mAffordanceHelper != null) {
            this.mAffordanceHelper.updatePreviews();
        }
    }

    private void updateLeftPreview() {
        View view = this.mLeftPreview;
        if (view != null) {
            this.mPreviewContainer.removeView(view);
        }
        if (this.mLeftIsVoiceAssist) {
            this.mLeftPreview = this.mPreviewInflater.inflatePreviewFromService(this.mAssistManager.getVoiceInteractorComponentName());
        } else {
            this.mLeftPreview = this.mPreviewInflater.inflatePreview(this.mLeftButton.getIntent());
        }
        if (this.mLeftPreview != null) {
            this.mPreviewContainer.addView(this.mLeftPreview);
            this.mLeftPreview.setVisibility(4);
        }
        if (this.mAffordanceHelper != null) {
            this.mAffordanceHelper.updatePreviews();
        }
    }

    public void startFinishDozeAnimation() {
        long j = 0;
        if (this.mLeftAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mLeftAffordanceView, 0L);
            j = 48;
        }
        startFinishDozeAnimationElement(this.mLockIcon, j);
        long j2 = j + 48;
        if (this.mRightAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mRightAffordanceView, j2);
        }
    }

    private void startFinishDozeAnimationElement(View view, long j) {
        view.setAlpha(0.0f);
        view.setTranslationY(view.getHeight() / 2);
        view.animate().alpha(1.0f).translationY(0.0f).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(j).setDuration(250L);
    }

    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        this.mIndicationController = keyguardIndicationController;
    }

    public void updateLeftAffordance() {
        updateLeftAffordanceIcon();
        updateLeftPreview();
    }

    public void onKeyguardShowingChanged() {
        updateLeftAffordance();
        inflateCameraPreview();
    }

    private void setRightButton(IntentButtonProvider.IntentButton intentButton) {
        this.mRightButton = intentButton;
        updateRightAffordanceIcon();
        updateCameraVisibility();
        inflateCameraPreview();
    }

    private void setLeftButton(IntentButtonProvider.IntentButton intentButton) {
        this.mLeftButton = intentButton;
        if (!(this.mLeftButton instanceof DefaultLeftButton)) {
            this.mLeftIsVoiceAssist = false;
        }
        updateLeftAffordance();
    }

    public void setDozing(boolean z, boolean z2) {
        this.mDozing = z;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
        if (z) {
            this.mLockIcon.setVisibility(4);
            this.mOverlayContainer.setVisibility(4);
            return;
        }
        this.mLockIcon.setVisibility(0);
        this.mOverlayContainer.setVisibility(0);
        if (z2) {
            startFinishDozeAnimation();
        }
    }

    public void dozeTimeTick() {
        if (this.mDarkAmount == 1.0f) {
            this.mIndicationArea.setTranslationY((this.mIndicationBottomMargin - this.mIndicationBottomMarginAmbient) + (((float) Math.random()) * 5.0f));
        }
    }

    public void setBurnInXOffset(int i) {
        if (this.mBurnInXOffset == i) {
            return;
        }
        this.mBurnInXOffset = i;
        this.mIndicationArea.setTranslationX(i);
    }

    private class DefaultLeftButton implements IntentButtonProvider.IntentButton {
        private IntentButtonProvider.IntentButton.IconState mIconState;

        private DefaultLeftButton() {
            this.mIconState = new IntentButtonProvider.IntentButton.IconState();
        }

        @Override
        public IntentButtonProvider.IntentButton.IconState getIcon() {
            KeyguardBottomAreaView.this.mLeftIsVoiceAssist = KeyguardBottomAreaView.this.canLaunchVoiceAssist();
            boolean z = KeyguardBottomAreaView.this.getResources().getBoolean(R.bool.config_keyguardShowLeftAffordance);
            boolean z2 = false;
            if (KeyguardBottomAreaView.this.mLeftIsVoiceAssist) {
                IntentButtonProvider.IntentButton.IconState iconState = this.mIconState;
                if (KeyguardBottomAreaView.this.mUserSetupComplete && z) {
                    z2 = true;
                }
                iconState.isVisible = z2;
                if (KeyguardBottomAreaView.this.mLeftAssistIcon == null) {
                    this.mIconState.drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R.drawable.ic_mic_26dp);
                } else {
                    this.mIconState.drawable = KeyguardBottomAreaView.this.mLeftAssistIcon;
                }
                this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R.string.accessibility_voice_assist_button);
            } else {
                IntentButtonProvider.IntentButton.IconState iconState2 = this.mIconState;
                if (KeyguardBottomAreaView.this.mUserSetupComplete && z && KeyguardBottomAreaView.this.isPhoneVisible()) {
                    z2 = true;
                }
                iconState2.isVisible = z2;
                this.mIconState.drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R.drawable.ic_phone_24dp);
                this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R.string.accessibility_phone_button);
            }
            return this.mIconState;
        }

        @Override
        public Intent getIntent() {
            return KeyguardBottomAreaView.PHONE_INTENT;
        }
    }

    private class DefaultRightButton implements IntentButtonProvider.IntentButton {
        private IntentButtonProvider.IntentButton.IconState mIconState;

        private DefaultRightButton() {
            this.mIconState = new IntentButtonProvider.IntentButton.IconState();
        }

        @Override
        public IntentButtonProvider.IntentButton.IconState getIcon() {
            ResolveInfo resolveInfoResolveCameraIntent = KeyguardBottomAreaView.this.resolveCameraIntent();
            boolean z = false;
            boolean z2 = (KeyguardBottomAreaView.this.mStatusBar == null || KeyguardBottomAreaView.this.mStatusBar.isCameraAllowedByAdmin()) ? false : true;
            IntentButtonProvider.IntentButton.IconState iconState = this.mIconState;
            if (!z2 && resolveInfoResolveCameraIntent != null && KeyguardBottomAreaView.this.getResources().getBoolean(R.bool.config_keyguardShowCameraAffordance) && KeyguardBottomAreaView.this.mUserSetupComplete) {
                z = true;
            }
            iconState.isVisible = z;
            this.mIconState.drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R.drawable.ic_camera_alt_24dp);
            this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R.string.accessibility_camera_button);
            return this.mIconState;
        }

        @Override
        public Intent getIntent() {
            return (!KeyguardBottomAreaView.this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser()) || KeyguardUpdateMonitor.getInstance(KeyguardBottomAreaView.this.mContext).getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser())) ? KeyguardBottomAreaView.INSECURE_CAMERA_INTENT : KeyguardBottomAreaView.SECURE_CAMERA_INTENT;
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        int safeInsetBottom = windowInsets.getDisplayCutout() != null ? windowInsets.getDisplayCutout().getSafeInsetBottom() : 0;
        if (isPaddingRelative()) {
            setPaddingRelative(getPaddingStart(), getPaddingTop(), getPaddingEnd(), safeInsetBottom);
        } else {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), safeInsetBottom);
        }
        return windowInsets;
    }

    @Override
    public void setAlpha(float f) {
        super.setAlpha(f);
        this.mEmergencyButtonExt.setEmergencyButtonVisibility(this.mEmergencyButton, f);
    }
}
