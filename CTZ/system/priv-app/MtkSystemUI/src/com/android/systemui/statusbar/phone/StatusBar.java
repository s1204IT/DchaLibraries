package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.IApplicationThread;
import android.app.IWallpaperManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProfilerInfo;
import android.app.StatusBarManager;
import android.app.TaskStackBuilder;
import android.app.WallpaperColors;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.RemoteAnimationAdapter;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.MessagingGroup;
import com.android.internal.widget.MessagingMessage;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.ActivityStarterDelegate;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.EventLogTags;
import com.android.systemui.Interpolators;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.charging.WirelessChargingAnimation;
import com.android.systemui.classifier.FalsingLog;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.doze.DozeReceiver;
import com.android.systemui.fragments.ExtensionFragmentListener;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.qs.QSFragment;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.car.CarQSFragment;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.AppTransitionFinishedEvent;
import com.android.systemui.recents.events.activity.UndockingTaskEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.AppOpsListener;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.FooterView;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyboardShortcuts;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationContentView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationEntryManager;
import com.android.systemui.statusbar.NotificationGutsManager;
import com.android.systemui.statusbar.NotificationInfo;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationLogger;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.NotificationViewHierarchyManager;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.notification.AboveShelfObserver;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.HeadsUpUtil;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.volume.VolumeComponent;
import com.mediatek.systemui.ext.IStatusBarPlmnPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/* loaded from: classes.dex */
public class StatusBar extends SystemUI implements ColorExtractor.OnColorsChangedListener, DemoMode, ActivityStarter, CommandQueue.Callbacks, DragDownHelper.DragDownCallback, NotificationPresenter, UnlockMethodCache.OnUnlockMethodChangedListener, ConfigurationController.ConfigurationListener, OnHeadsUpChangedListener, ZenModeController.Callback {
    private static final boolean ONLY_CORE_APPS;
    private AboveShelfObserver mAboveShelfObserver;
    protected AccessibilityManager mAccessibilityManager;
    protected ActivityLaunchAnimator mActivityLaunchAnimator;
    private View mAmbientIndicationContainer;
    protected AppOpsListener mAppOpsListener;
    protected AssistManager mAssistManager;
    private boolean mAutohideSuspended;
    protected BackDropView mBackdrop;
    protected ImageView mBackdropBack;
    protected ImageView mBackdropFront;
    protected IStatusBarService mBarService;
    private BatteryController mBatteryController;
    protected boolean mBouncerShowing;
    private boolean mBouncerWasShowingWhenHidden;
    private BrightnessMirrorController mBrightnessMirrorController;
    private boolean mBrightnessMirrorVisible;
    private long[] mCameraLaunchGestureVibePattern;
    protected boolean mClearAllEnabled;
    private SysuiColorExtractor mColorExtractor;
    protected CommandQueue mCommandQueue;
    private boolean mDemoMode;
    private boolean mDemoModeAllowed;
    protected boolean mDeviceInteractive;
    protected DevicePolicyManager mDevicePolicyManager;
    protected Display mDisplay;
    protected DozeScrimController mDozeScrimController;
    protected boolean mDozing;
    private boolean mDozingRequested;
    private ExpandableNotificationRow mDraggedDownRow;
    protected EmptyShadeView mEmptyShadeView;
    protected NotificationEntryManager mEntryManager;
    private boolean mExpandedVisible;
    protected FalsingManager mFalsingManager;
    protected FingerprintUnlockController mFingerprintUnlockController;
    protected FooterView mFooterView;
    private PowerManager.WakeLock mGestureWakeLock;
    protected NotificationGroupManager mGroupManager;
    private NotificationGutsManager mGutsManager;
    private HeadsUpAppearanceController mHeadsUpAppearanceController;
    protected HeadsUpManagerPhone mHeadsUpManager;
    private boolean mHideIconsForBouncer;
    protected StatusBarIconController mIconController;
    private PhoneStatusBarPolicy mIconPolicy;
    private int mInteractingWindows;
    private boolean mIsKeyguard;
    private boolean mIsOccluded;
    protected boolean mKeyguardFadingAway;
    protected long mKeyguardFadingAwayDelay;
    protected long mKeyguardFadingAwayDuration;
    KeyguardIndicationController mKeyguardIndicationController;
    protected KeyguardManager mKeyguardManager;
    private boolean mKeyguardRequested;
    private KeyguardStatusBarView mKeyguardStatusBar;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    protected KeyguardViewMediator mKeyguardViewMediator;
    private ViewMediatorCallback mKeyguardViewMediatorCallback;
    private int mLastCameraLaunchSource;
    private int mLastLoggedStateFingerprint;
    private boolean mLaunchCameraOnFinishedGoingToSleep;
    private boolean mLaunchCameraOnScreenTurningOn;
    private Runnable mLaunchTransitionEndRunnable;
    protected boolean mLaunchTransitionFadingAway;
    private boolean mLeaveOpenOnKeyguardHide;
    private LightBarController mLightBarController;
    private LockPatternUtils mLockPatternUtils;
    protected NotificationLockscreenUserManager mLockscreenUserManager;
    protected LockscreenWallpaper mLockscreenWallpaper;
    private int mMaxAllowedKeyguardNotifications;
    private int mMaxKeyguardNotifications;
    private NotificationMediaManager mMediaManager;
    private NavigationBarFragment mNavigationBar;
    private View mNavigationBarView;
    private NetworkController mNetworkController;
    private boolean mNoAnimationOnNextBarModeChange;
    protected NotificationIconAreaController mNotificationIconAreaController;
    protected NotificationListener mNotificationListener;
    protected NotificationLogger mNotificationLogger;
    protected NotificationPanelView mNotificationPanel;
    protected NotificationShelf mNotificationShelf;
    private IOverlayManager mOverlayManager;
    protected boolean mPanelExpanded;
    private View mPendingRemoteInputView;
    private View mPendingWorkRemoteInputView;
    protected PowerManager mPowerManager;
    private QSPanel mQSPanel;
    protected RecentsComponent mRecents;
    private boolean mReinflateNotificationsOnUserSwitched;
    protected NotificationRemoteInputManager mRemoteInputManager;
    private View mReportRejectedTouch;
    private ScreenLifecycle mScreenLifecycle;
    private ScreenPinningNotify mScreenPinningNotify;
    private ScreenPinningRequest mScreenPinningRequest;
    protected ScrimController mScrimController;
    protected boolean mScrimSrcModeEnabled;
    private StatusBarSignalPolicy mSignalPolicy;
    protected NotificationStackScrollLayout mStackScroller;
    protected int mState;
    protected StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private int mStatusBarMode;
    private LogMaker mStatusBarStateLog;
    protected PhoneStatusBarView mStatusBarView;
    protected StatusBarWindowView mStatusBarWindow;
    private boolean mStatusBarWindowHidden;
    protected StatusBarWindowManager mStatusBarWindowManager;
    private boolean mTopHidesStatusBar;
    protected UnlockMethodCache mUnlockMethodCache;
    protected UserSwitcherController mUserSwitcherController;
    private boolean mVibrateOnOpening;
    private Vibrator mVibrator;
    private VibratorHelper mVibratorHelper;
    protected NotificationViewHierarchyManager mViewHierarchyManager;
    protected boolean mVisible;
    private boolean mVisibleToUser;
    protected VisualStabilityManager mVisualStabilityManager;
    private VolumeComponent mVolumeComponent;
    protected boolean mVrMode;
    private boolean mWakeUpComingFromTouch;
    private PointF mWakeUpTouchLocation;

    @VisibleForTesting
    WakefulnessLifecycle mWakefulnessLifecycle;
    private boolean mWereIconsJustHidden;
    protected WindowManager mWindowManager;
    protected IWindowManager mWindowManagerService;
    private ZenModeController mZenController;
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = SystemProperties.getBoolean("debug.child_notifs", true);
    public static final boolean DEBUG = FeatureOptions.LOG_ENABLE;
    public static final boolean CHATTY = DEBUG;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private OpSystemUICustomizationFactoryBase mSystemUIFactoryBase = null;
    private int mNaturalBarHeight = -1;
    private final Point mCurrentDisplaySize = new Point();
    private int mStatusBarWindowState = 0;
    private DozeServiceHost mDozeServiceHost = new DozeServiceHost();
    private final Object mQueueLock = new Object();
    private RemoteInputQuickSettingsDisabler mRemoteInputQuickSettingsDisabler = (RemoteInputQuickSettingsDisabler) Dependency.get(RemoteInputQuickSettingsDisabler.class);
    private final int[] mAbsPos = new int[2];
    private final ArrayList<Runnable> mPostCollapseRunnables = new ArrayList<>();
    private int mDisabled1 = 0;
    private int mDisabled2 = 0;
    private int mSystemUiVisibility = 0;
    private final Rect mLastFullscreenStackBounds = new Rect();
    private final Rect mLastDockedStackBounds = new Rect();
    private final Rect mTmpRect = new Rect();
    private int mLastDispatchedSystemUiVisibility = -1;
    private final DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private final GestureRecorder mGestureRec = null;
    private final MetricsLogger mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);

    @VisibleForTesting
    protected boolean mUserSetup = false;
    private final DeviceProvisionedController.DeviceProvisionedListener mUserSetupObserver = new DeviceProvisionedController.DeviceProvisionedListener() { // from class: com.android.systemui.statusbar.phone.StatusBar.1
        AnonymousClass1() {
        }

        @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
        public void onUserSetupChanged() {
            boolean zIsUserSetup = StatusBar.this.mDeviceProvisionedController.isUserSetup(StatusBar.this.mDeviceProvisionedController.getCurrentUser());
            if (zIsUserSetup != StatusBar.this.mUserSetup) {
                StatusBar.this.mUserSetup = zIsUserSetup;
                if (!StatusBar.this.mUserSetup && StatusBar.this.mStatusBarView != null) {
                    StatusBar.this.animateCollapseQuickSettings();
                }
                if (StatusBar.this.mNotificationPanel != null) {
                    StatusBar.this.mNotificationPanel.setUserSetupComplete(StatusBar.this.mUserSetup);
                }
                StatusBar.this.updateQsExpansionEnabled();
            }
        }
    };
    protected final H mHandler = createHandler();
    private final UiOffloadThread mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
    private final Runnable mAutohide = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$Xuhkv64bZHfI7FOWJTQgAefazRk
        @Override // java.lang.Runnable
        public final void run() {
            StatusBar.lambda$new$0(this.f$0);
        }
    };
    protected final PorterDuffXfermode mSrcXferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    protected final PorterDuffXfermode mSrcOverXferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private BroadcastReceiver mWallpaperChangedReceiver = new BroadcastReceiver() { // from class: com.android.systemui.statusbar.phone.StatusBar.2
        AnonymousClass2() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            WallpaperManager wallpaperManager = (WallpaperManager) context.getSystemService(WallpaperManager.class);
            if (wallpaperManager == null) {
                Log.w("StatusBar", "WallpaperManager not available");
                return;
            }
            WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
            boolean z = wallpaperInfo != null && wallpaperInfo.getSupportsAmbientMode();
            StatusBar.this.mStatusBarWindowManager.setWallpaperSupportsAmbientMode(z);
            StatusBar.this.mScrimController.setWallpaperSupportsAmbientMode(z);
        }
    };
    private final int[] mTmpInt2 = new int[2];
    private final ScrimController.Callback mUnlockScrimCallback = new ScrimController.Callback() { // from class: com.android.systemui.statusbar.phone.StatusBar.3
        AnonymousClass3() {
        }

        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onFinished() {
            if (StatusBar.this.mStatusBarKeyguardViewManager == null) {
                Log.w("StatusBar", "Tried to notify keyguard visibility when mStatusBarKeyguardViewManager was null");
            } else if (StatusBar.this.mKeyguardFadingAway) {
                StatusBar.this.mStatusBarKeyguardViewManager.onKeyguardFadedAway();
            }
        }

        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onCancelled() {
            onFinished();
        }
    };
    private KeyguardMonitorImpl mKeyguardMonitor = (KeyguardMonitorImpl) Dependency.get(KeyguardMonitor.class);
    private final LockscreenGestureLogger mLockscreenGestureLogger = new LockscreenGestureLogger();
    private final View.OnClickListener mGoToLockedShadeListener = new View.OnClickListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$nk2TlfrOg2jlsjvFTJOkIoJCn4k
        @Override // android.view.View.OnClickListener
        public final void onClick(View view) {
            StatusBar.lambda$new$1(this.f$0, view);
        }
    };
    private final KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() { // from class: com.android.systemui.statusbar.phone.StatusBar.4
        AnonymousClass4() {
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onDreamingStateChanged(boolean z) {
            if (z) {
                StatusBar.this.maybeEscalateHeadsUp();
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onStrongAuthStateChanged(int i) {
            super.onStrongAuthStateChanged(i);
            StatusBar.this.mEntryManager.updateNotifications();
        }
    };
    protected final Runnable mHideBackdropFront = new Runnable() { // from class: com.android.systemui.statusbar.phone.StatusBar.7
        AnonymousClass7() {
        }

        @Override // java.lang.Runnable
        public void run() {
            StatusBar.this.mBackdropFront.setVisibility(4);
            StatusBar.this.mBackdropFront.animate().cancel();
            StatusBar.this.mBackdropFront.setImageDrawable(null);
        }
    };
    private final Runnable mAnimateCollapsePanels = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$hcoUGmHpwgtk12ln4V8HNBe6RFA
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.animateCollapsePanels();
        }
    };
    private final Runnable mCheckBarModes = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$KBnY14rlKZ6x8gvk_goBuFrr5eE
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.checkBarModes();
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.systemui.statusbar.phone.StatusBar.10
        AnonymousClass10() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (StatusBar.DEBUG) {
                Log.v("StatusBar", "onReceive: " + intent);
            }
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                KeyboardShortcuts.dismiss();
                if (StatusBar.this.mRemoteInputManager.getController() != null) {
                    StatusBar.this.mRemoteInputManager.getController().closeRemoteInputs();
                }
                if (StatusBar.this.mLockscreenUserManager.isCurrentProfile(getSendingUserId())) {
                    int i = 0;
                    String stringExtra = intent.getStringExtra("reason");
                    if (stringExtra != null && stringExtra.equals("recentapps")) {
                        i = 2;
                    }
                    StatusBar.this.animateCollapsePanels(i);
                    return;
                }
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                StatusBar.this.finishBarAnimations();
                StatusBar.this.resetUserExpandedStates();
            } else if ("android.app.action.SHOW_DEVICE_MONITORING_DIALOG".equals(action)) {
                StatusBar.this.mQSPanel.showDeviceMonitoringDialog();
            }
        }
    };
    private final BroadcastReceiver mDemoReceiver = new BroadcastReceiver() { // from class: com.android.systemui.statusbar.phone.StatusBar.11
        AnonymousClass11() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (StatusBar.DEBUG) {
                Log.v("StatusBar", "onReceive: " + intent);
            }
            String action = intent.getAction();
            if ("com.android.systemui.demo".equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String lowerCase = extras.getString("command", "").trim().toLowerCase();
                    if (lowerCase.length() > 0) {
                        try {
                            StatusBar.this.dispatchDemoCommand(lowerCase, extras);
                            return;
                        } catch (Throwable th) {
                            Log.w("StatusBar", "Error running demo command, intent=" + intent, th);
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            "fake_artwork".equals(action);
        }
    };
    final Runnable mStartTracing = new Runnable() { // from class: com.android.systemui.statusbar.phone.StatusBar.12
        AnonymousClass12() {
        }

        @Override // java.lang.Runnable
        public void run() {
            StatusBar.this.vibrate();
            SystemClock.sleep(250L);
            Log.d("StatusBar", "startTracing");
            Debug.startMethodTracing("/data/statusbar-traces/trace");
            StatusBar.this.mHandler.postDelayed(StatusBar.this.mStopTracing, 10000L);
        }
    };
    final Runnable mStopTracing = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$U2-_-jPHs6zuUW-29dyh2rf2vqM
        @Override // java.lang.Runnable
        public final void run() {
            StatusBar.lambda$new$28(this.f$0);
        }
    };
    final WakefulnessLifecycle.Observer mWakefulnessObserver = new AnonymousClass14();
    final ScreenLifecycle.Observer mScreenObserver = new ScreenLifecycle.Observer() { // from class: com.android.systemui.statusbar.phone.StatusBar.15
        AnonymousClass15() {
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurningOn() {
            StatusBar.this.mFalsingManager.onScreenTurningOn();
            StatusBar.this.mNotificationPanel.onScreenTurningOn();
            if (StatusBar.this.mLaunchCameraOnScreenTurningOn) {
                StatusBar.this.mNotificationPanel.launchCamera(false, StatusBar.this.mLastCameraLaunchSource);
                StatusBar.this.mLaunchCameraOnScreenTurningOn = false;
            }
            StatusBar.this.updateScrimController();
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOn() {
            StatusBar.this.mScrimController.onScreenTurnedOn();
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOff() {
            StatusBar.this.mFalsingManager.onScreenOff();
            StatusBar.this.mScrimController.onScreenTurnedOff();
            if (!StatusBar.this.isPulsing()) {
                StatusBar.this.updateIsKeyguard();
            }
        }
    };
    private DeviceProvisionedController mDeviceProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
    private final IVrStateCallbacks mVrStateCallbacks = new IVrStateCallbacks.Stub() { // from class: com.android.systemui.statusbar.phone.StatusBar.16
        AnonymousClass16() {
        }

        public void onVrStateChanged(boolean z) {
            StatusBar.this.mVrMode = z;
        }
    };
    private final BroadcastReceiver mBannerActionBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.systemui.statusbar.phone.StatusBar.17
        AnonymousClass17() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.systemui.statusbar.banner_action_cancel".equals(action) || "com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                ((NotificationManager) StatusBar.this.mContext.getSystemService("notification")).cancel(5);
                Settings.Secure.putInt(StatusBar.this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                if ("com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                    StatusBar.this.animateCollapsePanels(2, true);
                    if (BenesseExtension.getDchaState() != 0) {
                        return;
                    }
                    StatusBar.this.mContext.startActivity(new Intent("android.settings.ACTION_APP_NOTIFICATION_REDACTION").addFlags(268435456));
                }
            }
        }
    };
    private final Runnable mAutoDim = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$hDeelqePS5c93c_smCoceo_JbeI
        @Override // java.lang.Runnable
        public final void run() {
            StatusBar.lambda$new$49(this.f$0);
        }
    };
    private final NotificationInfo.CheckSaveListener mCheckSaveListener = new NotificationInfo.CheckSaveListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$FrwKL82awCvfsO6eEeQB614Yo5M
        @Override // com.android.systemui.statusbar.NotificationInfo.CheckSaveListener
        public final void checkSave(Runnable runnable, StatusBarNotification statusBarNotification) {
            StatusBar.lambda$new$51(this.f$0, runnable, statusBarNotification);
        }
    };
    private IStatusBarPlmnPlugin mStatusBarPlmnPlugin = null;
    private View mCustomizeCarrierLabel = null;

    static {
        boolean zIsOnlyCoreApps;
        try {
            zIsOnlyCoreApps = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException e) {
            zIsOnlyCoreApps = false;
        }
        ONLY_CORE_APPS = zIsOnlyCoreApps;
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$1 */
    class AnonymousClass1 implements DeviceProvisionedController.DeviceProvisionedListener {
        AnonymousClass1() {
        }

        @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
        public void onUserSetupChanged() {
            boolean zIsUserSetup = StatusBar.this.mDeviceProvisionedController.isUserSetup(StatusBar.this.mDeviceProvisionedController.getCurrentUser());
            if (zIsUserSetup != StatusBar.this.mUserSetup) {
                StatusBar.this.mUserSetup = zIsUserSetup;
                if (!StatusBar.this.mUserSetup && StatusBar.this.mStatusBarView != null) {
                    StatusBar.this.animateCollapseQuickSettings();
                }
                if (StatusBar.this.mNotificationPanel != null) {
                    StatusBar.this.mNotificationPanel.setUserSetupComplete(StatusBar.this.mUserSetup);
                }
                StatusBar.this.updateQsExpansionEnabled();
            }
        }
    }

    public static /* synthetic */ void lambda$new$0(StatusBar statusBar) {
        int i = statusBar.mSystemUiVisibility & (-201326593);
        if (statusBar.mSystemUiVisibility != i) {
            statusBar.notifyUiVisibilityChanged(i);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$2 */
    class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            WallpaperManager wallpaperManager = (WallpaperManager) context.getSystemService(WallpaperManager.class);
            if (wallpaperManager == null) {
                Log.w("StatusBar", "WallpaperManager not available");
                return;
            }
            WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
            boolean z = wallpaperInfo != null && wallpaperInfo.getSupportsAmbientMode();
            StatusBar.this.mStatusBarWindowManager.setWallpaperSupportsAmbientMode(z);
            StatusBar.this.mScrimController.setWallpaperSupportsAmbientMode(z);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$3 */
    class AnonymousClass3 implements ScrimController.Callback {
        AnonymousClass3() {
        }

        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onFinished() {
            if (StatusBar.this.mStatusBarKeyguardViewManager == null) {
                Log.w("StatusBar", "Tried to notify keyguard visibility when mStatusBarKeyguardViewManager was null");
            } else if (StatusBar.this.mKeyguardFadingAway) {
                StatusBar.this.mStatusBarKeyguardViewManager.onKeyguardFadedAway();
            }
        }

        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onCancelled() {
            onFinished();
        }
    }

    public static /* synthetic */ void lambda$new$1(StatusBar statusBar, View view) {
        if (statusBar.mState == 1) {
            statusBar.wakeUpIfDozing(SystemClock.uptimeMillis(), view);
            statusBar.goToLockedShade(null);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$4 */
    class AnonymousClass4 extends KeyguardUpdateMonitorCallback {
        AnonymousClass4() {
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onDreamingStateChanged(boolean z) {
            if (z) {
                StatusBar.this.maybeEscalateHeadsUp();
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onStrongAuthStateChanged(int i) {
            super.onStrongAuthStateChanged(i);
            StatusBar.this.mEntryManager.updateNotifications();
        }
    }

    @Override // com.android.systemui.SystemUI
    public void start() throws Resources.NotFoundException {
        this.mGroupManager = (NotificationGroupManager) Dependency.get(NotificationGroupManager.class);
        this.mVisualStabilityManager = (VisualStabilityManager) Dependency.get(VisualStabilityManager.class);
        this.mNotificationLogger = (NotificationLogger) Dependency.get(NotificationLogger.class);
        this.mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        this.mNotificationListener = (NotificationListener) Dependency.get(NotificationListener.class);
        this.mGroupManager = (NotificationGroupManager) Dependency.get(NotificationGroupManager.class);
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mUserSwitcherController = (UserSwitcherController) Dependency.get(UserSwitcherController.class);
        this.mScreenLifecycle = (ScreenLifecycle) Dependency.get(ScreenLifecycle.class);
        this.mScreenLifecycle.addObserver(this.mScreenObserver);
        this.mWakefulnessLifecycle = (WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class);
        this.mWakefulnessLifecycle.addObserver(this.mWakefulnessObserver);
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mAssistManager = (AssistManager) Dependency.get(AssistManager.class);
        this.mOverlayManager = IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay"));
        this.mLockscreenUserManager = (NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class);
        this.mGutsManager = (NotificationGutsManager) Dependency.get(NotificationGutsManager.class);
        this.mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
        this.mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        this.mViewHierarchyManager = (NotificationViewHierarchyManager) Dependency.get(NotificationViewHierarchyManager.class);
        this.mAppOpsListener = (AppOpsListener) Dependency.get(AppOpsListener.class);
        this.mAppOpsListener.setUpWithPresenter(this, this.mEntryManager);
        this.mZenController = (ZenModeController) Dependency.get(ZenModeController.class);
        this.mKeyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        this.mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);
        this.mColorExtractor.addOnColorsChangedListener(this);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        updateDisplaySize();
        Resources resources = this.mContext.getResources();
        this.mVibrateOnOpening = this.mContext.getResources().getBoolean(R.bool.config_vibrateOnIconAnimation);
        this.mVibratorHelper = (VibratorHelper) Dependency.get(VibratorHelper.class);
        this.mScrimSrcModeEnabled = resources.getBoolean(R.bool.config_status_bar_scrim_behind_use_src);
        this.mClearAllEnabled = resources.getBoolean(R.bool.config_enableNotificationsClearAll);
        DateTimeView.setReceiverHandler((Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        putComponent(StatusBar.class, this);
        this.mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mDeviceProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mRecents = (RecentsComponent) getComponent(Recents.class);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mMediaManager.setUpWithPresenter(this, this.mEntryManager);
        this.mCommandQueue = (CommandQueue) getComponent(CommandQueue.class);
        this.mCommandQueue.addCallbacks(this);
        int[] iArr = new int[9];
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        Rect rect = new Rect();
        Rect rect2 = new Rect();
        try {
            this.mBarService.registerStatusBar(this.mCommandQueue, arrayList2, arrayList3, iArr, arrayList, rect, rect2);
        } catch (RemoteException e) {
        }
        createAndAddWindows();
        this.mContext.registerReceiver(this.mWallpaperChangedReceiver, new IntentFilter("android.intent.action.WALLPAPER_CHANGED"));
        this.mWallpaperChangedReceiver.onReceive(this.mContext, null);
        this.mLockscreenUserManager.setUpWithPresenter(this, this.mEntryManager);
        this.mCommandQueue.disable(iArr[0], iArr[6], false);
        setSystemUiVisibility(iArr[1], iArr[7], iArr[8], -1, rect, rect2);
        topAppWindowChanged(iArr[2] != 0);
        setImeWindowStatus((IBinder) arrayList.get(0), iArr[3], iArr[4], iArr[5] != 0);
        int size = arrayList2.size();
        for (int i = 0; i < size; i++) {
            this.mCommandQueue.setIcon((String) arrayList2.get(i), (StatusBarIcon) arrayList3.get(i));
        }
        this.mNotificationListener.setUpWithPresenter(this, this.mEntryManager);
        if (DEBUG) {
            Log.d("StatusBar", String.format("init: icons=%d disabled=0x%08x lights=0x%08x menu=0x%08x imeButton=0x%08x", Integer.valueOf(arrayList3.size()), Integer.valueOf(iArr[0]), Integer.valueOf(iArr[1]), Integer.valueOf(iArr[2]), Integer.valueOf(iArr[3])));
        }
        setHeadsUpUser(this.mLockscreenUserManager.getCurrentUserId());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.systemui.statusbar.banner_action_cancel");
        intentFilter.addAction("com.android.systemui.statusbar.banner_action_setup");
        this.mContext.registerReceiver(this.mBannerActionBroadcastReceiver, intentFilter, "com.android.systemui.permission.SELF", null);
        try {
            IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager")).registerListener(this.mVrStateCallbacks);
        } catch (RemoteException e2) {
            Slog.e("StatusBar", "Failed to register VR mode state listener: " + e2);
        }
        try {
            IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper")).setInAmbientMode(false, false);
        } catch (RemoteException e3) {
        }
        this.mIconPolicy = new PhoneStatusBarPolicy(this.mContext, this.mIconController);
        this.mSignalPolicy = new StatusBarSignalPolicy(this.mContext, this.mIconController);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mUnlockMethodCache.addListener(this);
        startKeyguard();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateCallback);
        putComponent(DozeHost.class, this.mDozeServiceHost);
        this.mScreenPinningRequest = new ScreenPinningRequest(this.mContext);
        this.mFalsingManager = FalsingManager.getInstance(this.mContext);
        ((ActivityStarterDelegate) Dependency.get(ActivityStarterDelegate.class)).setActivityStarterImpl(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    protected void makeStatusBarView() throws Resources.NotFoundException {
        Context context = this.mContext;
        updateDisplaySize();
        updateResources();
        updateTheme();
        inflateStatusBarWindow(context);
        this.mStatusBarWindow.setService(this);
        this.mStatusBarWindow.setOnTouchListener(getStatusBarWindowTouchListener());
        this.mNotificationPanel = (NotificationPanelView) this.mStatusBarWindow.findViewById(R.id.notification_panel);
        this.mStackScroller = (NotificationStackScrollLayout) this.mStatusBarWindow.findViewById(R.id.notification_stack_scroller);
        this.mZenController.addCallback(this);
        this.mActivityLaunchAnimator = new ActivityLaunchAnimator(this.mStatusBarWindow, this, this.mNotificationPanel, this.mStackScroller);
        this.mGutsManager.setUpWithPresenter(this, this.mEntryManager, this.mStackScroller, this.mCheckSaveListener, new NotificationGutsManager.OnSettingsClickListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$EGLDayN0ppRMZI1envjIVfb7R_0
            @Override // com.android.systemui.statusbar.NotificationGutsManager.OnSettingsClickListener
            public final void onClick(String str) {
                this.f$0.mBarService.onNotificationSettingsViewed(str);
            }
        });
        this.mNotificationLogger.setUpWithEntryManager(this.mEntryManager, this.mStackScroller);
        this.mNotificationPanel.setStatusBar(this);
        this.mNotificationPanel.setGroupManager(this.mGroupManager);
        this.mAboveShelfObserver = new AboveShelfObserver(this.mStackScroller);
        this.mAboveShelfObserver.setListener((AboveShelfObserver.HasViewAboveShelfChangedListener) this.mStatusBarWindow.findViewById(R.id.notification_container_parent));
        this.mKeyguardStatusBar = (KeyguardStatusBarView) this.mStatusBarWindow.findViewById(R.id.keyguard_header);
        this.mNotificationIconAreaController = SystemUIFactory.getInstance().createNotificationIconAreaController(context, this);
        inflateShelf();
        this.mNotificationIconAreaController.setupShelf(this.mNotificationShelf);
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mNotificationIconAreaController);
        FragmentHostManager.get(this.mStatusBarWindow).addTagListener("CollapsedStatusBarFragment", new FragmentHostManager.FragmentListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$TPJyILujZ88K3rKFmgzHGHpbtLo
            @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
            public final void onFragmentViewCreated(String str, Fragment fragment) {
                StatusBar.lambda$makeStatusBarView$3(this.f$0, str, fragment);
            }
        }).getFragmentManager().beginTransaction().replace(R.id.status_bar_container, new CollapsedStatusBarFragment(), "CollapsedStatusBarFragment").commit();
        this.mIconController = (StatusBarIconController) Dependency.get(StatusBarIconController.class);
        this.mHeadsUpManager = new HeadsUpManagerPhone(context, this.mStatusBarWindow, this.mGroupManager, this, this.mVisualStabilityManager);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this.mHeadsUpManager);
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpManager.addListener(this.mNotificationPanel);
        this.mHeadsUpManager.addListener(this.mGroupManager);
        this.mHeadsUpManager.addListener(this.mVisualStabilityManager);
        this.mNotificationPanel.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setHeadsUpManager(this.mHeadsUpManager);
        putComponent(HeadsUpManager.class, this.mHeadsUpManager);
        this.mEntryManager.setUpWithPresenter(this, this.mStackScroller, this, this.mHeadsUpManager);
        this.mViewHierarchyManager.setUpWithPresenter(this, this.mEntryManager, this.mStackScroller);
        try {
            boolean zHasNavigationBar = this.mWindowManagerService.hasNavigationBar();
            if (DEBUG) {
                Log.v("StatusBar", "hasNavigationBar=" + zHasNavigationBar);
            }
            if (zHasNavigationBar) {
                createNavigationBar();
            }
        } catch (RemoteException e) {
        }
        this.mScreenPinningNotify = new ScreenPinningNotify(this.mContext);
        this.mStackScroller.setLongPressListener(this.mEntryManager.getNotificationLongClicker());
        this.mStackScroller.setStatusBar(this);
        this.mStackScroller.setGroupManager(this.mGroupManager);
        this.mStackScroller.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setOnGroupChangeListener(this.mStackScroller);
        this.mVisualStabilityManager.setVisibilityLocationProvider(this.mStackScroller);
        inflateEmptyShadeView();
        inflateFooterView();
        this.mBackdrop = (BackDropView) this.mStatusBarWindow.findViewById(R.id.backdrop);
        this.mBackdropFront = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_front);
        this.mBackdropBack = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_back);
        this.mLockscreenWallpaper = new LockscreenWallpaper(this.mContext, this, this.mHandler);
        this.mKeyguardIndicationController = SystemUIFactory.getInstance().createKeyguardIndicationController(this.mContext, (ViewGroup) this.mStatusBarWindow.findViewById(R.id.keyguard_indication_area), this.mNotificationPanel.getLockIcon());
        this.mNotificationPanel.setKeyguardIndicationController(this.mKeyguardIndicationController);
        this.mAmbientIndicationContainer = this.mStatusBarWindow.findViewById(R.id.ambient_indication_container);
        setAreThereNotifications();
        this.mBatteryController.addCallback(new BatteryController.BatteryStateChangeCallback() { // from class: com.android.systemui.statusbar.phone.StatusBar.5
            AnonymousClass5() {
            }

            @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
            public void onPowerSaveChanged(boolean z) {
                StatusBar.this.mHandler.post(StatusBar.this.mCheckBarModes);
                if (StatusBar.this.mDozeServiceHost != null) {
                    StatusBar.this.mDozeServiceHost.firePowerSaveChanged(z);
                }
            }

            @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
            public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
            }
        });
        this.mLightBarController = (LightBarController) Dependency.get(LightBarController.class);
        if (this.mNavigationBar != null) {
            this.mNavigationBar.setLightBarController(this.mLightBarController);
        }
        this.mScrimController = SystemUIFactory.getInstance().createScrimController((ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_behind), (ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_in_front), this.mLockscreenWallpaper, new TriConsumer() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$CPTthYwWNrh8z0d-UzTtbuHfZd4
            public final void accept(Object obj, Object obj2, Object obj3) {
                this.f$0.mLightBarController.setScrimState((ScrimState) obj, ((Float) obj2).floatValue(), (ColorExtractor.GradientColors) obj3);
            }
        }, new Consumer() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$aLenH7zcwVaIHX6ie2fIXARtA4g
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                StatusBar.lambda$makeStatusBarView$5(this.f$0, (Integer) obj);
            }
        }, DozeParameters.getInstance(this.mContext), (AlarmManager) this.mContext.getSystemService(AlarmManager.class));
        if (this.mScrimSrcModeEnabled) {
            Runnable runnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$EdGxnh5D2TWPQa657dfBiOwfR50
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBar.lambda$makeStatusBarView$6(this.f$0);
                }
            };
            this.mBackdrop.setOnVisibilityChangedRunnable(runnable);
            runnable.run();
        }
        this.mStackScroller.setScrimController(this.mScrimController);
        this.mDozeScrimController = new DozeScrimController(this.mScrimController, context, DozeParameters.getInstance(context));
        this.mVolumeComponent = (VolumeComponent) getComponent(VolumeComponent.class);
        SIMHelper.setContext(context);
        this.mStatusBarPlmnPlugin = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeStatusBarPlmn(context);
        if (supportCustomizeCarrierLabel()) {
            this.mCustomizeCarrierLabel = this.mStatusBarPlmnPlugin.customizeCarrierLabel(this.mNotificationPanel, null);
        }
        this.mNotificationPanel.setUserSetupComplete(this.mUserSetup);
        if (UserManager.get(this.mContext).isUserSwitcherEnabled()) {
            createUserSwitcher();
        }
        View viewFindViewById = this.mStatusBarWindow.findViewById(R.id.qs_frame);
        if (viewFindViewById != null) {
            FragmentHostManager fragmentHostManager = FragmentHostManager.get(viewFindViewById);
            ExtensionFragmentListener.attachExtensonToFragment(viewFindViewById, QS.TAG, R.id.qs_frame, ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(QS.class).withPlugin(QS.class).withFeature("android.hardware.type.automotive", new Supplier() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$8CIXyGMqrW3leEv17MCPM15_8RU
                @Override // java.util.function.Supplier
                public final Object get() {
                    return new CarQSFragment();
                }
            }).withDefault(new Supplier() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$5DhEg0l8l_X2iMYdu6k_V3Zid54
                @Override // java.util.function.Supplier
                public final Object get() {
                    return new QSFragment();
                }
            }).build());
            final QSTileHost qSTileHostCreateQSTileHost = SystemUIFactory.getInstance().createQSTileHost(this.mContext, this, this.mIconController);
            this.mBrightnessMirrorController = new BrightnessMirrorController(this.mStatusBarWindow, new Consumer() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$0m7F6e2QtJDG3hy0Y3EVPv_U6WQ
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    StatusBar.lambda$makeStatusBarView$7(this.f$0, (Boolean) obj);
                }
            });
            fragmentHostManager.addTagListener(QS.TAG, new FragmentHostManager.FragmentListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$bkd2NbKjjDMBWKcdJ2fG14LddEs
                @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
                public final void onFragmentViewCreated(String str, Fragment fragment) {
                    StatusBar.lambda$makeStatusBarView$8(this.f$0, qSTileHostCreateQSTileHost, str, fragment);
                }
            });
        }
        this.mReportRejectedTouch = this.mStatusBarWindow.findViewById(R.id.report_rejected_touch);
        if (this.mReportRejectedTouch != null) {
            updateReportRejectedTouchVisibility();
            this.mReportRejectedTouch.setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$ggtzWYldpP6XbhwYmX0SNphBaak
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    StatusBar.lambda$makeStatusBarView$9(this.f$0, view);
                }
            });
        }
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (!powerManager.isScreenOn()) {
            this.mBroadcastReceiver.onReceive(this.mContext, new Intent("android.intent.action.SCREEN_OFF"));
        }
        this.mGestureWakeLock = powerManager.newWakeLock(10, "GestureWakeLock");
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        int[] intArray = this.mContext.getResources().getIntArray(R.array.config_cameraLaunchGestureVibePattern);
        this.mCameraLaunchGestureVibePattern = new long[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            this.mCameraLaunchGestureVibePattern[i] = intArray[i];
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.app.action.SHOW_DEVICE_MONITORING_DIALOG");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.android.systemui.demo");
        context.registerReceiverAsUser(this.mDemoReceiver, UserHandle.ALL, intentFilter2, "android.permission.DUMP", null);
        this.mDeviceProvisionedController.addCallback(this.mUserSetupObserver);
        this.mUserSetupObserver.onUserSetupChanged();
        ThreadedRenderer.overrideProperty("disableProfileBars", "true");
        ThreadedRenderer.overrideProperty("ambientRatio", String.valueOf(1.5f));
    }

    public static /* synthetic */ void lambda$makeStatusBarView$3(StatusBar statusBar, String str, Fragment fragment) {
        ((CollapsedStatusBarFragment) fragment).initNotificationIconArea(statusBar.mNotificationIconAreaController);
        statusBar.mStatusBarView = (PhoneStatusBarView) fragment.getView();
        statusBar.mStatusBarView.setBar(statusBar);
        statusBar.mStatusBarView.setPanel(statusBar.mNotificationPanel);
        statusBar.mStatusBarView.setScrimController(statusBar.mScrimController);
        statusBar.mStatusBarView.setBouncerShowing(statusBar.mBouncerShowing);
        if (statusBar.mHeadsUpAppearanceController != null) {
            statusBar.mHeadsUpAppearanceController.destroy();
        }
        statusBar.mHeadsUpAppearanceController = new HeadsUpAppearanceController(statusBar.mNotificationIconAreaController, statusBar.mHeadsUpManager, statusBar.mStatusBarWindow);
        statusBar.setAreThereNotifications();
        statusBar.checkBarModes();
        statusBar.attachPlmnPlugin();
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$5 */
    class AnonymousClass5 implements BatteryController.BatteryStateChangeCallback {
        AnonymousClass5() {
        }

        @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
        public void onPowerSaveChanged(boolean z) {
            StatusBar.this.mHandler.post(StatusBar.this.mCheckBarModes);
            if (StatusBar.this.mDozeServiceHost != null) {
                StatusBar.this.mDozeServiceHost.firePowerSaveChanged(z);
            }
        }

        @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
        public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        }
    }

    public static /* synthetic */ void lambda$makeStatusBarView$5(StatusBar statusBar, Integer num) {
        if (statusBar.mStatusBarWindowManager != null) {
            statusBar.mStatusBarWindowManager.setScrimsVisibility(num.intValue());
        }
    }

    public static /* synthetic */ void lambda$makeStatusBarView$6(StatusBar statusBar) {
        boolean z = statusBar.mBackdrop.getVisibility() != 0;
        statusBar.mScrimController.setDrawBehindAsSrc(z);
        statusBar.mStackScroller.setDrawBackgroundAsSrc(z);
    }

    public static /* synthetic */ void lambda$makeStatusBarView$7(StatusBar statusBar, Boolean bool) {
        statusBar.mBrightnessMirrorVisible = bool.booleanValue();
        statusBar.updateScrimController();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: android.app.Fragment */
    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ void lambda$makeStatusBarView$8(StatusBar statusBar, QSTileHost qSTileHost, String str, Fragment fragment) {
        QS qs = (QS) fragment;
        if (qs instanceof QSFragment) {
            QSFragment qSFragment = (QSFragment) qs;
            qSFragment.setHost(qSTileHost);
            statusBar.mQSPanel = qSFragment.getQsPanel();
            statusBar.mQSPanel.setBrightnessMirror(statusBar.mBrightnessMirrorController);
            statusBar.mKeyguardStatusBar.setQSPanel(statusBar.mQSPanel);
        }
    }

    public static /* synthetic */ void lambda$makeStatusBarView$9(StatusBar statusBar, View view) {
        Uri uriReportRejectedTouch = statusBar.mFalsingManager.reportRejectedTouch();
        if (uriReportRejectedTouch == null) {
            return;
        }
        StringWriter stringWriter = new StringWriter();
        stringWriter.write("Build info: ");
        stringWriter.write(SystemProperties.get("ro.build.description"));
        stringWriter.write("\nSerial number: ");
        stringWriter.write(SystemProperties.get("ro.serialno"));
        stringWriter.write("\n");
        PrintWriter printWriter = new PrintWriter(stringWriter);
        FalsingLog.dump(printWriter);
        printWriter.flush();
        statusBar.startActivityDismissingKeyguard(Intent.createChooser(new Intent("android.intent.action.SEND").setType("*/*").putExtra("android.intent.extra.SUBJECT", "Rejected touch report").putExtra("android.intent.extra.STREAM", uriReportRejectedTouch).putExtra("android.intent.extra.TEXT", stringWriter.toString()), "Share rejected touch report").addFlags(268435456), true, true);
    }

    protected void createNavigationBar() {
        this.mNavigationBarView = NavigationBarFragment.create(this.mContext, new FragmentHostManager.FragmentListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$1lxmukd3g368Ipe-wYASjfRCObk
            @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
            public final void onFragmentViewCreated(String str, Fragment fragment) {
                StatusBar.lambda$createNavigationBar$10(this.f$0, str, fragment);
            }
        });
    }

    public static /* synthetic */ void lambda$createNavigationBar$10(StatusBar statusBar, String str, Fragment fragment) {
        statusBar.mNavigationBar = (NavigationBarFragment) fragment;
        if (statusBar.mLightBarController != null) {
            statusBar.mNavigationBar.setLightBarController(statusBar.mLightBarController);
        }
        statusBar.mNavigationBar.setCurrentSysuiVisibility(statusBar.mSystemUiVisibility);
    }

    protected View.OnTouchListener getStatusBarWindowTouchListener() {
        return new View.OnTouchListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$sjd-NB4fQvJpW0ZlghyyFoEGyww
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return StatusBar.lambda$getStatusBarWindowTouchListener$11(this.f$0, view, motionEvent);
            }
        };
    }

    public static /* synthetic */ boolean lambda$getStatusBarWindowTouchListener$11(StatusBar statusBar, View view, MotionEvent motionEvent) {
        statusBar.checkUserAutohide(motionEvent);
        statusBar.mRemoteInputManager.checkRemoteInputOutside(motionEvent);
        if (motionEvent.getAction() == 0 && statusBar.mExpandedVisible) {
            statusBar.animateCollapsePanels();
        }
        return statusBar.mStatusBarWindow.onTouchEvent(motionEvent);
    }

    private void inflateShelf() {
        this.mNotificationShelf = (NotificationShelf) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_shelf, (ViewGroup) this.mStackScroller, false);
        this.mNotificationShelf.setOnActivatedListener(this);
        this.mStackScroller.setShelf(this.mNotificationShelf);
        this.mNotificationShelf.setOnClickListener(this.mGoToLockedShadeListener);
        this.mNotificationShelf.setStatusBarState(this.mState);
    }

    public void onDensityOrFontScaleChanged() {
        MessagingMessage.dropCache();
        MessagingGroup.dropCache();
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isSwitchingUser()) {
            this.mEntryManager.updateNotificationsOnDensityOrFontScaleChanged();
        } else {
            this.mReinflateNotificationsOnUserSwitched = true;
        }
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.onDensityOrFontScaleChanged();
        }
        this.mStatusBarKeyguardViewManager.onDensityOrFontScaleChanged();
        ((UserInfoControllerImpl) Dependency.get(UserInfoController.class)).onDensityOrFontScaleChanged();
        ((UserSwitcherController) Dependency.get(UserSwitcherController.class)).onDensityOrFontScaleChanged();
        if (this.mKeyguardUserSwitcher != null) {
            this.mKeyguardUserSwitcher.onDensityOrFontScaleChanged();
        }
        this.mNotificationIconAreaController.onDensityOrFontScaleChanged(this.mContext);
        this.mHeadsUpManager.onDensityOrFontScaleChanged();
        reevaluateStyles();
    }

    private void onThemeChanged() {
        reevaluateStyles();
        this.mNotificationPanel.onThemeChanged();
        if (this.mKeyguardStatusBar != null) {
            this.mKeyguardStatusBar.onThemeChanged();
        }
        this.mKeyguardIndicationController = SystemUIFactory.getInstance().createKeyguardIndicationController(this.mContext, (ViewGroup) this.mStatusBarWindow.findViewById(R.id.keyguard_indication_area), this.mNotificationPanel.getLockIcon());
        this.mNotificationPanel.setKeyguardIndicationController(this.mKeyguardIndicationController);
        this.mKeyguardIndicationController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mKeyguardIndicationController.setVisible(this.mState == 1);
        this.mKeyguardIndicationController.setDozing(this.mDozing);
        if (this.mStatusBarKeyguardViewManager != null) {
            this.mStatusBarKeyguardViewManager.onThemeChanged();
        }
        if (this.mAmbientIndicationContainer instanceof AutoReinflateContainer) {
            ((AutoReinflateContainer) this.mAmbientIndicationContainer).inflateLayout();
        }
    }

    protected void reevaluateStyles() {
        inflateFooterView();
        updateFooter();
        inflateEmptyShadeView();
        updateEmptyShadeView();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onOverlayChanged() {
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.onOverlayChanged();
        }
    }

    private void inflateEmptyShadeView() {
        if (this.mStackScroller == null) {
            return;
        }
        this.mEmptyShadeView = (EmptyShadeView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_no_notifications, (ViewGroup) this.mStackScroller, false);
        this.mEmptyShadeView.setText(R.string.empty_shade_text);
        this.mStackScroller.setEmptyShadeView(this.mEmptyShadeView);
    }

    private void inflateFooterView() {
        if (this.mStackScroller == null) {
            return;
        }
        this.mFooterView = (FooterView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_footer, (ViewGroup) this.mStackScroller, false);
        this.mFooterView.setDismissButtonClickListener(new View.OnClickListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$HeNtWn6ER-i55fzNijMTFX8MRhM
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                StatusBar.lambda$inflateFooterView$12(this.f$0, view);
            }
        });
        this.mFooterView.setManageButtonClickListener(new View.OnClickListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$VvTfjQyycJ7XkvNtPALBHaxz3UA
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.manageNotifications();
            }
        });
        this.mStackScroller.setFooterView(this.mFooterView);
    }

    public static /* synthetic */ void lambda$inflateFooterView$12(StatusBar statusBar, View view) {
        statusBar.mMetricsLogger.action(148);
        statusBar.clearAllNotifications();
    }

    protected void createUserSwitcher() {
        this.mKeyguardUserSwitcher = new KeyguardUserSwitcher(this.mContext, (ViewStub) this.mStatusBarWindow.findViewById(R.id.keyguard_user_switcher), this.mKeyguardStatusBar, this.mNotificationPanel);
    }

    protected void inflateStatusBarWindow(Context context) {
        this.mStatusBarWindow = (StatusBarWindowView) View.inflate(context, R.layout.super_status_bar, null);
    }

    public void manageNotifications() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        startActivity(new Intent("android.settings.ALL_APPS_NOTIFICATION_SETTINGS"), true, true, 536870912);
    }

    /* JADX WARN: Removed duplicated region for block: B:111:0x009d A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:83:0x0059  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x0060  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void clearAllNotifications() {
        List<ExpandableNotificationRow> notificationChildren;
        int childCount = this.mStackScroller.getChildCount();
        ArrayList<View> arrayList = new ArrayList<>(childCount);
        final ArrayList arrayList2 = new ArrayList(childCount);
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mStackScroller.getChildAt(i);
            if (childAt instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) childAt;
                boolean clipBounds = childAt.getClipBounds(this.mTmpRect);
                boolean z = true;
                if (this.mStackScroller.canChildBeDismissed(childAt)) {
                    arrayList2.add(expandableNotificationRow);
                    if (childAt.getVisibility() == 0 && (!clipBounds || this.mTmpRect.height() > 0)) {
                        arrayList.add(childAt);
                    } else {
                        z = false;
                    }
                    notificationChildren = expandableNotificationRow.getNotificationChildren();
                    if (notificationChildren == null) {
                        for (ExpandableNotificationRow expandableNotificationRow2 : notificationChildren) {
                            arrayList2.add(expandableNotificationRow2);
                            if (z && expandableNotificationRow.areChildrenExpanded() && this.mStackScroller.canChildBeDismissed(expandableNotificationRow2)) {
                                boolean clipBounds2 = expandableNotificationRow2.getClipBounds(this.mTmpRect);
                                if (expandableNotificationRow2.getVisibility() == 0 && (!clipBounds2 || this.mTmpRect.height() > 0)) {
                                    arrayList.add(expandableNotificationRow2);
                                }
                            }
                        }
                    }
                } else {
                    if (childAt.getVisibility() != 0 || (clipBounds && this.mTmpRect.height() <= 0)) {
                    }
                    notificationChildren = expandableNotificationRow.getNotificationChildren();
                    if (notificationChildren == null) {
                    }
                }
            }
        }
        if (arrayList2.isEmpty()) {
            animateCollapsePanels(0);
        } else {
            addPostCollapseAction(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$KPpPJ6lHyblaC5nDSDIGtQv6sSs
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBar.lambda$clearAllNotifications$14(this.f$0, arrayList2);
                }
            });
            performDismissAllAnimations(arrayList);
        }
    }

    public static /* synthetic */ void lambda$clearAllNotifications$14(StatusBar statusBar, ArrayList arrayList) {
        statusBar.mStackScroller.setDismissAllInProgress(false);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) it.next();
            if (statusBar.mStackScroller.canChildBeDismissed(expandableNotificationRow)) {
                statusBar.mEntryManager.removeNotification(expandableNotificationRow.getEntry().key, null);
            } else {
                expandableNotificationRow.resetTranslation();
            }
        }
        try {
            statusBar.mBarService.onClearAllNotifications(statusBar.mLockscreenUserManager.getCurrentUserId());
        } catch (Exception e) {
        }
    }

    private void performDismissAllAnimations(ArrayList<View> arrayList) {
        Runnable runnable;
        Runnable runnable2 = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$hz39WvlFDjmu3UFNVirp_TJxYPU
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.animateCollapsePanels(0);
            }
        };
        if (arrayList.isEmpty()) {
            runnable2.run();
            return;
        }
        this.mStackScroller.setDismissAllInProgress(true);
        int iMax = 140;
        int i = 180;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            View view = arrayList.get(size);
            if (size != 0) {
                runnable = null;
            } else {
                runnable = runnable2;
            }
            this.mStackScroller.dismissViewAnimated(view, runnable, i, 260L);
            iMax = Math.max(50, iMax - 10);
            i += iMax;
        }
    }

    protected void startKeyguard() {
        Trace.beginSection("StatusBar#startKeyguard");
        KeyguardViewMediator keyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        this.mFingerprintUnlockController = new FingerprintUnlockController(this.mContext, this.mDozeScrimController, keyguardViewMediator, this.mScrimController, this, UnlockMethodCache.getInstance(this.mContext));
        this.mStatusBarKeyguardViewManager = keyguardViewMediator.registerStatusBar(this, getBouncerContainer(), this.mNotificationPanel, this.mFingerprintUnlockController);
        this.mKeyguardIndicationController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mFingerprintUnlockController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mRemoteInputManager.getController().addCallback(this.mStatusBarKeyguardViewManager);
        this.mKeyguardViewMediatorCallback = keyguardViewMediator.getViewMediatorCallback();
        this.mLightBarController.setFingerprintUnlockController(this.mFingerprintUnlockController);
        ((KeyguardDismissUtil) Dependency.get(KeyguardDismissUtil.class)).setDismissHandler(new KeyguardDismissHandler() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$9MqmIExNr864CrvDQmmxl0ZneDg
            @Override // com.android.systemui.statusbar.phone.KeyguardDismissHandler
            public final void executeWhenUnlocked(KeyguardHostView.OnDismissAction onDismissAction) {
                this.f$0.executeWhenUnlocked(onDismissAction);
            }
        });
        Trace.endSection();
    }

    protected View getStatusBarView() {
        return this.mStatusBarView;
    }

    public StatusBarWindowView getStatusBarWindow() {
        return this.mStatusBarWindow;
    }

    protected ViewGroup getBouncerContainer() {
        return this.mStatusBarWindow;
    }

    public int getStatusBarHeight() {
        if (this.mNaturalBarHeight < 0) {
            this.mNaturalBarHeight = this.mContext.getResources().getDimensionPixelSize(android.R.dimen.indeterminate_progress_alpha_16);
        }
        return this.mNaturalBarHeight;
    }

    protected boolean toggleSplitScreenMode(int i, int i2) {
        if (this.mRecents == null) {
            return false;
        }
        if (WindowManagerProxy.getInstance().getDockSide() == -1) {
            int navBarPosition = WindowManagerWrapper.getInstance().getNavBarPosition();
            if (navBarPosition == -1) {
                return false;
            }
            return this.mRecents.splitPrimaryTask(-1, navBarPosition == 1 ? 1 : 0, null, i);
        }
        Divider divider = (Divider) getComponent(Divider.class);
        if (divider != null && divider.isMinimized() && !divider.isHomeStackResizable()) {
            return false;
        }
        EventBus.getDefault().send(new UndockingTaskEvent());
        if (i2 != -1) {
            this.mMetricsLogger.action(i2);
        }
        return true;
    }

    @Override // com.android.systemui.statusbar.NotificationEntryManager.Callback
    public void onPerformRemoveNotification(StatusBarNotification statusBarNotification) {
        if (this.mStackScroller.hasPulsingNotifications() && !this.mHeadsUpManager.hasHeadsUpNotifications()) {
            this.mDozeScrimController.pulseOutNow();
        }
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public void updateNotificationViews() {
        if (this.mStackScroller == null || this.mScrimController == null) {
            return;
        }
        if (isCollapsing()) {
            addPostCollapseAction(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$GA5FozNehiplNux-Psv9796exeM
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.updateNotificationViews();
                }
            });
            return;
        }
        this.mViewHierarchyManager.updateNotificationViews();
        updateSpeedBumpIndex();
        updateFooter();
        updateEmptyShadeView();
        updateQsExpansionEnabled();
        this.mNotificationIconAreaController.updateNotificationIcons();
    }

    @Override // com.android.systemui.statusbar.NotificationEntryManager.Callback
    public void onNotificationAdded(NotificationData.Entry entry) {
        setAreThereNotifications();
    }

    @Override // com.android.systemui.statusbar.NotificationEntryManager.Callback
    public void onNotificationUpdated(StatusBarNotification statusBarNotification) {
        setAreThereNotifications();
    }

    @Override // com.android.systemui.statusbar.NotificationEntryManager.Callback
    public void onNotificationRemoved(String str, StatusBarNotification statusBarNotification) {
        if (statusBarNotification != null && !hasActiveNotifications() && !this.mNotificationPanel.isTracking() && !this.mNotificationPanel.isQsExpanded()) {
            if (this.mState == 0) {
                animateCollapsePanels();
            } else if (this.mState == 2 && !isCollapsing()) {
                goToKeyguard();
            }
        }
        setAreThereNotifications();
    }

    private void updateQsExpansionEnabled() {
        this.mNotificationPanel.setQsExpansionEnabled(isDeviceProvisioned() && (this.mUserSetup || this.mUserSwitcherController == null || !this.mUserSwitcherController.isSimpleUserSwitcher()) && (this.mDisabled2 & 4) == 0 && (this.mDisabled2 & 1) == 0 && !this.mDozing && !ONLY_CORE_APPS);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void addQsTile(ComponentName componentName) {
        if (this.mQSPanel != null && this.mQSPanel.getHost() != null) {
            this.mQSPanel.getHost().addTile(componentName);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void remQsTile(ComponentName componentName) {
        if (this.mQSPanel != null && this.mQSPanel.getHost() != null) {
            this.mQSPanel.getHost().removeTile(componentName);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void clickTile(ComponentName componentName) {
        this.mQSPanel.clickTile(componentName);
    }

    @VisibleForTesting
    protected void updateFooter() {
        boolean z = false;
        boolean z2 = this.mClearAllEnabled && hasActiveClearableNotifications();
        if ((z2 || this.mEntryManager.getNotificationData().getActiveNotifications().size() != 0) && this.mState != 1 && !this.mRemoteInputManager.getController().isRemoteInputActive()) {
            z = true;
        }
        this.mStackScroller.updateFooterView(z, z2);
    }

    private boolean hasActiveClearableNotifications() {
        int childCount = this.mStackScroller.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mStackScroller.getChildAt(i);
            if ((childAt instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) childAt).canViewBeDismissed()) {
                return true;
            }
        }
        return false;
    }

    private void updateEmptyShadeView() {
        this.mNotificationPanel.showEmptyShadeView(this.mState != 1 && this.mEntryManager.getNotificationData().getActiveNotifications().size() == 0);
    }

    private void updateSpeedBumpIndex() {
        int childCount = this.mStackScroller.getChildCount();
        boolean z = false;
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = this.mStackScroller.getChildAt(i3);
            if (childAt.getVisibility() != 8 && (childAt instanceof ExpandableNotificationRow)) {
                i2++;
                if (!this.mEntryManager.getNotificationData().isAmbient(((ExpandableNotificationRow) childAt).getStatusBarNotification().getKey())) {
                    i = i2;
                }
            }
        }
        if (i == childCount) {
            z = true;
        }
        this.mStackScroller.updateSpeedBumpIndex(i, z);
    }

    public static boolean isTopLevelChild(NotificationData.Entry entry) {
        return entry.row.getParent() instanceof NotificationStackScrollLayout;
    }

    public boolean areNotificationsHidden() {
        return this.mZenController.areNotificationsHiddenInShade();
    }

    public void requestNotificationUpdate() {
        this.mEntryManager.updateNotifications();
    }

    protected void setAreThereNotifications() {
        if (this.mStatusBarView != null) {
            View viewFindViewById = this.mStatusBarView.findViewById(R.id.notification_lights_out);
            boolean z = hasActiveNotifications() && !areLightsOn();
            if (z != (viewFindViewById.getAlpha() == 1.0f)) {
                if (z) {
                    viewFindViewById.setAlpha(0.0f);
                    viewFindViewById.setVisibility(0);
                }
                viewFindViewById.animate().alpha(z ? 1.0f : 0.0f).setDuration(z ? 750L : 250L).setInterpolator(new AccelerateInterpolator(2.0f)).setListener(z ? null : new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.StatusBar.6
                    final /* synthetic */ View val$nlo;

                    AnonymousClass6(View viewFindViewById2) {
                        view = viewFindViewById2;
                    }

                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        view.setVisibility(8);
                    }
                }).start();
            }
        }
        this.mMediaManager.findAndUpdateMediaNotifications();
        updateCarrierLabelVisibility(false);
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$6 */
    class AnonymousClass6 extends AnimatorListenerAdapter {
        final /* synthetic */ View val$nlo;

        AnonymousClass6(View viewFindViewById2) {
            view = viewFindViewById2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            view.setVisibility(8);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$7 */
    class AnonymousClass7 implements Runnable {
        AnonymousClass7() {
        }

        @Override // java.lang.Runnable
        public void run() {
            StatusBar.this.mBackdropFront.setVisibility(4);
            StatusBar.this.mBackdropFront.animate().cancel();
            StatusBar.this.mBackdropFront.setImageDrawable(null);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:119:0x004e  */
    /* JADX WARN: Removed duplicated region for block: B:129:0x0073 A[PHI: r0
  0x0073: PHI (r0v9 android.graphics.drawable.Drawable) = 
  (r0v8 android.graphics.drawable.Drawable)
  (r0v8 android.graphics.drawable.Drawable)
  (r0v17 android.graphics.drawable.Drawable)
  (r0v17 android.graphics.drawable.Drawable)
 binds: [B:120:0x004f, B:122:0x0057, B:124:0x0066, B:126:0x006e] A[DONT_GENERATE, DONT_INLINE]] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updateMediaMetaData(boolean z, boolean z2) {
        Drawable wallpaperDrawable;
        boolean z3;
        Bitmap bitmap;
        Trace.beginSection("StatusBar#updateMediaMetaData");
        if (this.mBackdrop == null) {
            Trace.endSection();
            return;
        }
        boolean z4 = this.mFingerprintUnlockController != null && this.mFingerprintUnlockController.isWakeAndUnlock();
        if (this.mLaunchTransitionFadingAway || z4) {
            this.mBackdrop.setVisibility(4);
            Trace.endSection();
            return;
        }
        MediaMetadata mediaMetadata = this.mMediaManager.getMediaMetadata();
        if (mediaMetadata != null) {
            Bitmap bitmap2 = mediaMetadata.getBitmap("android.media.metadata.ART");
            if (bitmap2 == null) {
                bitmap2 = mediaMetadata.getBitmap("android.media.metadata.ALBUM_ART");
            }
            if (bitmap2 != null) {
                wallpaperDrawable = new BitmapDrawable(this.mBackdropBack.getResources(), bitmap2);
            }
        } else {
            wallpaperDrawable = null;
        }
        if (wallpaperDrawable == null && (bitmap = this.mLockscreenWallpaper.getBitmap()) != null) {
            wallpaperDrawable = new LockscreenWallpaper.WallpaperDrawable(this.mBackdropBack.getResources(), bitmap);
            if (this.mStatusBarKeyguardViewManager != null && this.mStatusBarKeyguardViewManager.isShowing()) {
                z3 = true;
            }
        } else {
            z3 = false;
        }
        boolean z5 = this.mStatusBarKeyguardViewManager != null && this.mStatusBarKeyguardViewManager.isOccluded();
        if ((wallpaperDrawable != null) && !this.mDozing && ((this.mState != 0 || z3) && this.mFingerprintUnlockController.getMode() != 2 && !z5)) {
            if (this.mBackdrop.getVisibility() != 0) {
                this.mBackdrop.setVisibility(0);
                if (z2) {
                    this.mBackdrop.setAlpha(0.002f);
                    this.mBackdrop.animate().alpha(1.0f);
                } else {
                    this.mBackdrop.animate().cancel();
                    this.mBackdrop.setAlpha(1.0f);
                }
                this.mStatusBarWindowManager.setBackdropShowing(true);
                this.mColorExtractor.setMediaBackdropVisible(true);
                z = true;
            }
            if (z) {
                if (this.mBackdropBack.getDrawable() != null) {
                    this.mBackdropFront.setImageDrawable(this.mBackdropBack.getDrawable().getConstantState().newDrawable(this.mBackdropFront.getResources()).mutate());
                    if (this.mScrimSrcModeEnabled) {
                        this.mBackdropFront.getDrawable().mutate().setXfermode(this.mSrcOverXferMode);
                    }
                    this.mBackdropFront.setAlpha(1.0f);
                    this.mBackdropFront.setVisibility(0);
                } else {
                    this.mBackdropFront.setVisibility(4);
                }
                this.mBackdropBack.setImageDrawable(wallpaperDrawable);
                if (this.mScrimSrcModeEnabled) {
                    this.mBackdropBack.getDrawable().mutate().setXfermode(this.mSrcXferMode);
                }
                if (this.mBackdropFront.getVisibility() == 0) {
                    this.mBackdropFront.animate().setDuration(250L).alpha(0.0f).withEndAction(this.mHideBackdropFront);
                }
            }
        } else if (this.mBackdrop.getVisibility() != 8) {
            this.mColorExtractor.setMediaBackdropVisible(false);
            boolean z6 = this.mDozing && !ScrimState.AOD.getAnimateChange();
            if (this.mFingerprintUnlockController.getMode() == 2 || z5 || z6) {
                this.mBackdrop.setVisibility(8);
                this.mBackdropBack.setImageDrawable(null);
                this.mStatusBarWindowManager.setBackdropShowing(false);
            } else {
                this.mStatusBarWindowManager.setBackdropShowing(false);
                this.mBackdrop.animate().alpha(0.002f).setInterpolator(Interpolators.ACCELERATE_DECELERATE).setDuration(300L).setStartDelay(0L).withEndAction(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$wlXP8P-mZcZXCFMAEy6t46bsTzE
                    @Override // java.lang.Runnable
                    public final void run() {
                        StatusBar.lambda$updateMediaMetaData$16(this.f$0);
                    }
                });
                if (this.mKeyguardFadingAway) {
                    this.mBackdrop.animate().setDuration(this.mKeyguardFadingAwayDuration / 2).setStartDelay(this.mKeyguardFadingAwayDelay).setInterpolator(Interpolators.LINEAR).start();
                }
            }
        }
        Trace.endSection();
    }

    public static /* synthetic */ void lambda$updateMediaMetaData$16(StatusBar statusBar) {
        statusBar.mBackdrop.setVisibility(8);
        statusBar.mBackdropFront.animate().cancel();
        statusBar.mBackdropBack.setImageDrawable(null);
        statusBar.mHandler.post(statusBar.mHideBackdropFront);
    }

    private void updateReportRejectedTouchVisibility() {
        if (this.mReportRejectedTouch == null) {
            return;
        }
        this.mReportRejectedTouch.setVisibility((this.mState == 1 && !this.mDozing && this.mFalsingManager.isReportingEnabled()) ? 0 : 4);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void disable(int i, int i2, boolean z) {
        int iAdjustDisableFlags = this.mRemoteInputQuickSettingsDisabler.adjustDisableFlags(i2);
        int i3 = this.mStatusBarWindowState;
        int i4 = this.mDisabled1;
        int i5 = i ^ i4;
        this.mDisabled1 = i;
        int i6 = this.mDisabled2;
        int i7 = iAdjustDisableFlags ^ i6;
        this.mDisabled2 = iAdjustDisableFlags;
        if (DEBUG) {
            Log.d("StatusBar", String.format("disable1: 0x%08x -> 0x%08x (diff1: 0x%08x)", Integer.valueOf(i4), Integer.valueOf(i), Integer.valueOf(i5)));
            Log.d("StatusBar", String.format("disable2: 0x%08x -> 0x%08x (diff2: 0x%08x)", Integer.valueOf(i6), Integer.valueOf(iAdjustDisableFlags), Integer.valueOf(i7)));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("disable<");
        int i8 = i & 65536;
        sb.append(i8 != 0 ? 'E' : 'e');
        int i9 = 65536 & i5;
        sb.append(i9 != 0 ? '!' : ' ');
        sb.append((i & 131072) != 0 ? 'I' : 'i');
        sb.append((131072 & i5) != 0 ? '!' : ' ');
        int i10 = i & 262144;
        sb.append(i10 != 0 ? 'A' : 'a');
        int i11 = i5 & 262144;
        sb.append(i11 != 0 ? '!' : ' ');
        sb.append((i & 1048576) != 0 ? 'S' : 's');
        sb.append((i5 & 1048576) != 0 ? '!' : ' ');
        sb.append((4194304 & i) != 0 ? 'B' : 'b');
        sb.append((4194304 & i5) != 0 ? '!' : ' ');
        sb.append((2097152 & i) != 0 ? 'H' : 'h');
        sb.append((2097152 & i5) != 0 ? '!' : ' ');
        int i12 = 16777216 & i;
        sb.append(i12 != 0 ? 'R' : 'r');
        int i13 = 16777216 & i5;
        sb.append(i13 != 0 ? '!' : ' ');
        sb.append((8388608 & i) != 0 ? 'C' : 'c');
        sb.append((8388608 & i5) != 0 ? '!' : ' ');
        sb.append((33554432 & i) != 0 ? 'S' : 's');
        sb.append((i5 & 33554432) != 0 ? '!' : ' ');
        sb.append("> disable2<");
        sb.append((iAdjustDisableFlags & 1) != 0 ? 'Q' : 'q');
        int i14 = i7 & 1;
        sb.append(i14 != 0 ? '!' : ' ');
        sb.append((iAdjustDisableFlags & 2) != 0 ? 'I' : 'i');
        sb.append((i7 & 2) != 0 ? '!' : ' ');
        sb.append((iAdjustDisableFlags & 4) != 0 ? 'N' : 'n');
        int i15 = i7 & 4;
        sb.append(i15 != 0 ? '!' : ' ');
        sb.append('>');
        Log.d("StatusBar", sb.toString());
        if (i9 != 0 && i8 != 0) {
            animateCollapsePanels();
        }
        if (i13 != 0) {
            if (i12 != 0) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
                if (this.mStatusBarPlmnPlugin != null) {
                    this.mStatusBarPlmnPlugin.setPlmnVisibility(8);
                }
            } else if (this.mStatusBarPlmnPlugin != null) {
                this.mStatusBarPlmnPlugin.setPlmnVisibility(0);
            }
        }
        if (i11 != 0) {
            this.mEntryManager.setDisableNotificationAlerts(i10 != 0);
        }
        if (i14 != 0) {
            updateQsExpansionEnabled();
        }
        if (i15 != 0) {
            updateQsExpansionEnabled();
            if ((i & 4) != 0) {
                animateCollapsePanels();
            }
        }
    }

    public void recomputeDisableFlags(boolean z) {
        this.mCommandQueue.recomputeDisableFlags(z);
    }

    protected H createHandler() {
        return new H();
    }

    private void startActivity(Intent intent, boolean z, boolean z2, int i) {
        startActivityDismissingKeyguard(intent, z, z2, i);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z) {
        startActivityDismissingKeyguard(intent, false, z);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z, boolean z2) {
        startActivityDismissingKeyguard(intent, z, z2);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z, ActivityStarter.Callback callback) {
        startActivityDismissingKeyguard(intent, false, z, false, callback, 0);
    }

    public void setQsExpanded(boolean z) {
        int i;
        this.mStatusBarWindowManager.setQsExpanded(z);
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if (z) {
            i = 4;
        } else {
            i = 0;
        }
        notificationPanelView.setStatusAccessibilityImportance(i);
    }

    public boolean isGoingToNotificationShade() {
        return this.mLeaveOpenOnKeyguardHide;
    }

    public boolean isWakeUpComingFromTouch() {
        return this.mWakeUpComingFromTouch;
    }

    public boolean isFalsingThresholdNeeded() {
        return getBarState() == 1;
    }

    @Override // com.android.systemui.statusbar.NotificationData.Environment
    public boolean isDozing() {
        return this.mDozing && this.mStackScroller.isFullyDark();
    }

    @Override // com.android.systemui.statusbar.NotificationEntryManager.Callback
    public boolean shouldPeek(NotificationData.Entry entry, StatusBarNotification statusBarNotification) {
        if (this.mIsOccluded && !isDozing()) {
            boolean z = this.mLockscreenUserManager.isLockscreenPublicMode(this.mLockscreenUserManager.getCurrentUserId()) || this.mLockscreenUserManager.isLockscreenPublicMode(statusBarNotification.getUserId());
            boolean zNeedsRedaction = this.mLockscreenUserManager.needsRedaction(entry);
            if (z && zNeedsRedaction) {
                return false;
            }
        }
        if (!panelsEnabled()) {
            if (DEBUG) {
                Log.d("StatusBar", "No peeking: disabled panel : " + statusBarNotification.getKey());
            }
            return false;
        }
        if (statusBarNotification.getNotification().fullScreenIntent == null) {
            return true;
        }
        if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
            if (DEBUG) {
                Log.d("StatusBar", "No peeking: accessible fullscreen: " + statusBarNotification.getKey());
            }
            return false;
        }
        if (isDozing()) {
            return false;
        }
        return !this.mStatusBarKeyguardViewManager.isShowing() || this.mStatusBarKeyguardViewManager.isOccluded();
    }

    @Override // com.android.systemui.statusbar.NotificationData.Environment
    public String getCurrentMediaNotificationKey() {
        return this.mMediaManager.getMediaNotificationKey();
    }

    public boolean isScrimSrcModeEnabled() {
        return this.mScrimSrcModeEnabled;
    }

    public void onKeyguardViewManagerStatesUpdated() {
        logStateToEventlog();
    }

    @Override // com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener
    public void onUnlockMethodStateChanged() {
        logStateToEventlog();
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinnedModeChanged(boolean z) {
        if (z) {
            this.mStatusBarWindowManager.setHeadsUpShowing(true);
            this.mStatusBarWindowManager.setForceStatusBarVisible(true);
            if (this.mNotificationPanel.isFullyCollapsed()) {
                this.mNotificationPanel.requestLayout();
                this.mStatusBarWindowManager.setForceWindowCollapsed(true);
                this.mNotificationPanel.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$ay-YAvh4VjN9pBbLWxpXGraAy_o
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.mStatusBarWindowManager.setForceWindowCollapsed(false);
                    }
                });
                return;
            }
            return;
        }
        if (this.mNotificationPanel.isFullyCollapsed() && !this.mNotificationPanel.isTracking()) {
            this.mHeadsUpManager.setHeadsUpGoingAway(true);
            this.mStackScroller.runAfterAnimationFinished(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$bAFPzCYA0o8c0tOSJ9q0jO-POoQ
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBar.lambda$onHeadsUpPinnedModeChanged$18(this.f$0);
                }
            });
        } else {
            this.mStatusBarWindowManager.setHeadsUpShowing(false);
        }
    }

    public static /* synthetic */ void lambda$onHeadsUpPinnedModeChanged$18(StatusBar statusBar) {
        if (!statusBar.mHeadsUpManager.hasPinnedHeadsUp()) {
            statusBar.mStatusBarWindowManager.setHeadsUpShowing(false);
            statusBar.mHeadsUpManager.setHeadsUpGoingAway(false);
        }
        statusBar.mRemoteInputManager.removeRemoteInputEntriesKeptUntilCollapsed();
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinned(ExpandableNotificationRow expandableNotificationRow) {
        dismissVolumeDialog();
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpUnPinned(ExpandableNotificationRow expandableNotificationRow) {
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean z) {
        this.mEntryManager.onHeadsUpStateChanged(entry, z);
        if (z) {
            this.mDozeServiceHost.fireNotificationHeadsUp();
        }
    }

    protected void setHeadsUpUser(int i) {
        if (this.mHeadsUpManager != null) {
            this.mHeadsUpManager.setUser(i);
        }
    }

    public boolean isKeyguardCurrentlySecure() {
        return !this.mUnlockMethodCache.canSkipBouncer();
    }

    public void setPanelExpanded(boolean z) {
        this.mPanelExpanded = z;
        updateHideIconsForBouncer(false);
        this.mStatusBarWindowManager.setPanelExpanded(z);
        this.mVisualStabilityManager.setPanelExpanded(z);
        if (z && getBarState() != 1) {
            if (DEBUG) {
                Log.v("StatusBar", "clearing notification effects from setExpandedHeight");
            }
            clearNotificationEffects();
        }
        if (!z) {
            this.mRemoteInputManager.removeRemoteInputEntriesKeptUntilCollapsed();
        }
    }

    public NotificationStackScrollLayout getNotificationScrollLayout() {
        return this.mStackScroller;
    }

    public boolean isPulsing() {
        return this.mDozeScrimController != null && this.mDozeScrimController.isPulsing();
    }

    public boolean isLaunchTransitionFadingAway() {
        return this.mLaunchTransitionFadingAway;
    }

    public boolean hideStatusBarIconsWhenExpanded() {
        return this.mNotificationPanel.hideStatusBarIconsWhenExpanded();
    }

    public void onColorsChanged(ColorExtractor colorExtractor, int i) {
        updateTheme();
    }

    public boolean isUsingDarkTheme() {
        OverlayInfo overlayInfo;
        try {
            overlayInfo = this.mOverlayManager.getOverlayInfo("com.android.systemui.theme.dark", this.mLockscreenUserManager.getCurrentUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
            overlayInfo = null;
        }
        return overlayInfo != null && overlayInfo.isEnabled();
    }

    public View getAmbientIndicationContainer() {
        return this.mAmbientIndicationContainer;
    }

    public void setOccluded(boolean z) {
        this.mIsOccluded = z;
        this.mScrimController.setKeyguardOccluded(z);
        updateHideIconsForBouncer(false);
    }

    public boolean hideStatusBarIconsForBouncer() {
        return this.mHideIconsForBouncer || this.mWereIconsJustHidden;
    }

    private void updateHideIconsForBouncer(boolean z) {
        boolean z2 = (this.mTopHidesStatusBar && this.mIsOccluded && (this.mStatusBarWindowHidden || this.mBouncerShowing)) || (!this.mPanelExpanded && !this.mIsOccluded && this.mBouncerShowing);
        if (this.mHideIconsForBouncer != z2) {
            this.mHideIconsForBouncer = z2;
            if (!z2 && this.mBouncerWasShowingWhenHidden) {
                this.mWereIconsJustHidden = true;
                this.mHandler.postDelayed(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$GPKlymtdaSWvEyRtwTRSFeYuvWc
                    @Override // java.lang.Runnable
                    public final void run() {
                        StatusBar.lambda$updateHideIconsForBouncer$19(this.f$0);
                    }
                }, 500L);
            } else {
                recomputeDisableFlags(z);
            }
        }
        if (z2) {
            this.mBouncerWasShowingWhenHidden = this.mBouncerShowing;
        }
    }

    public static /* synthetic */ void lambda$updateHideIconsForBouncer$19(StatusBar statusBar) {
        statusBar.mWereIconsJustHidden = false;
        statusBar.recomputeDisableFlags(true);
    }

    public void onLaunchAnimationCancelled() {
        if (!isCollapsing()) {
            onClosingFinished();
        }
    }

    protected class H extends Handler {
        protected H() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 1000:
                    StatusBar.this.animateExpandNotificationsPanel();
                    break;
                case 1001:
                    StatusBar.this.animateCollapsePanels();
                    break;
                case 1002:
                    StatusBar.this.animateExpandSettingsPanel((String) message.obj);
                    break;
                case 1003:
                    StatusBar.this.onLaunchTransitionTimeout();
                    break;
                default:
                    switch (i) {
                        case 1026:
                            StatusBar.this.toggleKeyboardShortcuts(message.arg1);
                            break;
                        case 1027:
                            StatusBar.this.dismissKeyboardShortcuts();
                            break;
                    }
            }
        }
    }

    public void maybeEscalateHeadsUp() {
        this.mHeadsUpManager.getAllEntries().forEach(new Consumer() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$M-v6L0bCMELm_outZVi3FdfiHBc
            @Override // java.util.function.Consumer
            public final void accept(Object obj) throws PendingIntent.CanceledException {
                StatusBar.lambda$maybeEscalateHeadsUp$20((NotificationData.Entry) obj);
            }
        });
        this.mHeadsUpManager.releaseAllImmediately();
    }

    static /* synthetic */ void lambda$maybeEscalateHeadsUp$20(NotificationData.Entry entry) throws PendingIntent.CanceledException {
        StatusBarNotification statusBarNotification = entry.notification;
        Notification notification = statusBarNotification.getNotification();
        if (notification.fullScreenIntent != null) {
            if (DEBUG) {
                Log.d("StatusBar", "converting a heads up to fullScreen");
            }
            try {
                EventLog.writeEvent(36003, statusBarNotification.getKey());
                notification.fullScreenIntent.send();
                entry.notifyFullScreenIntentLaunched();
            } catch (PendingIntent.CanceledException e) {
            }
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void handleSystemKey(int i) {
        if (panelsEnabled() && this.mKeyguardMonitor.isDeviceInteractive()) {
            if ((!this.mKeyguardMonitor.isShowing() || this.mKeyguardMonitor.isOccluded()) && this.mUserSetup) {
                if (280 == i) {
                    this.mMetricsLogger.action(493);
                    this.mNotificationPanel.collapse(false, 1.0f);
                    return;
                }
                if (281 == i) {
                    this.mMetricsLogger.action(494);
                    if (this.mNotificationPanel.isFullyCollapsed()) {
                        if (this.mVibrateOnOpening) {
                            this.mVibratorHelper.vibrate(2);
                        }
                        this.mNotificationPanel.expand(true);
                        this.mMetricsLogger.count("panel_open", 1);
                        return;
                    }
                    if (!this.mNotificationPanel.isInSettings() && !this.mNotificationPanel.isExpanding()) {
                        this.mNotificationPanel.flingSettings(0.0f, true);
                        this.mMetricsLogger.count("panel_open_qs", 1);
                    }
                }
            }
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showPinningEnterExitToast(boolean z) {
        if (z) {
            this.mScreenPinningNotify.showPinningStartToast();
        } else {
            this.mScreenPinningNotify.showPinningExitToast();
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showPinningEscapeToast() {
        this.mScreenPinningNotify.showEscapeToast(getNavigationBarView() == null || getNavigationBarView().isRecentsButtonVisible());
    }

    boolean panelsEnabled() {
        return (this.mDisabled1 & 65536) == 0 && (this.mDisabled2 & 4) == 0 && !ONLY_CORE_APPS;
    }

    void makeExpandedVisible(boolean z) {
        if (!z && (this.mExpandedVisible || !panelsEnabled())) {
            return;
        }
        this.mExpandedVisible = true;
        updateCarrierLabelVisibility(true);
        this.mStatusBarWindowManager.setPanelVisible(true);
        visibilityChanged(true);
        recomputeDisableFlags(!z);
        setInteracting(1, true);
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(0);
    }

    public void postAnimateCollapsePanels() {
        this.mHandler.post(this.mAnimateCollapsePanels);
    }

    public void postAnimateForceCollapsePanels() {
        this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$ozJF_5mXhWqttO3c_hxUQ5zEj6A
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.animateCollapsePanels(0, true);
            }
        });
    }

    public void postAnimateOpenPanels() {
        this.mHandler.sendEmptyMessage(1002);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void togglePanel() {
        if (this.mPanelExpanded) {
            animateCollapsePanels();
        } else {
            animateExpandNotificationsPanel();
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void animateCollapsePanels(int i) {
        animateCollapsePanels(i, false, false, 1.0f);
    }

    public void animateCollapsePanels(int i, boolean z) {
        animateCollapsePanels(i, z, false, 1.0f);
    }

    public void animateCollapsePanels(int i, boolean z, boolean z2) {
        animateCollapsePanels(i, z, z2, 1.0f);
    }

    public void animateCollapsePanels(int i, boolean z, boolean z2, float f) {
        if (!z && this.mState != 0) {
            runPostCollapseRunnables();
            return;
        }
        if ((i & 2) == 0 && !this.mHandler.hasMessages(1020)) {
            this.mHandler.removeMessages(1020);
            this.mHandler.sendEmptyMessage(1020);
        }
        Log.v("StatusBar", "mStatusBarWindow: " + this.mStatusBarWindow + " canPanelBeCollapsed(): " + this.mNotificationPanel.canPanelBeCollapsed());
        if (this.mStatusBarWindow != null && this.mNotificationPanel.canPanelBeCollapsed()) {
            this.mStatusBarWindowManager.setStatusBarFocusable(false);
            this.mStatusBarWindow.cancelExpandHelper();
            this.mStatusBarView.collapsePanel(true, z2, f);
        }
    }

    private void runPostCollapseRunnables() {
        ArrayList arrayList = new ArrayList(this.mPostCollapseRunnables);
        this.mPostCollapseRunnables.clear();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ((Runnable) arrayList.get(i)).run();
        }
        this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    public void animateExpandNotificationsPanel() {
        if (!panelsEnabled()) {
            return;
        }
        this.mNotificationPanel.expandWithoutQs();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void animateExpandSettingsPanel(String str) {
        if (panelsEnabled() && this.mUserSetup) {
            if (str != null) {
                this.mQSPanel.openDetails(str);
            }
            this.mNotificationPanel.expandWithQs();
        }
    }

    public void animateCollapseQuickSettings() {
        if (this.mState == 0) {
            this.mStatusBarView.collapsePanel(true, false, 1.0f);
        }
    }

    void makeExpandedInvisible() {
        if (!this.mExpandedVisible || this.mStatusBarWindow == null) {
            return;
        }
        this.mStatusBarView.collapsePanel(false, false, 1.0f);
        this.mNotificationPanel.closeQs();
        this.mExpandedVisible = false;
        visibilityChanged(false);
        this.mStatusBarWindowManager.setPanelVisible(false);
        this.mStatusBarWindowManager.setForceStatusBarVisible(false);
        this.mGutsManager.closeAndSaveGuts(true, true, true, -1, -1, true);
        runPostCollapseRunnables();
        setInteracting(1, false);
        showBouncerIfKeyguard();
        recomputeDisableFlags(this.mNotificationPanel.hideStatusBarIconsWhenExpanded());
        if (!this.mStatusBarKeyguardViewManager.isShowing()) {
            WindowManagerGlobal.getInstance().trimMemory(20);
        }
    }

    public boolean interceptTouchEvent(MotionEvent motionEvent) {
        if (CHATTY && motionEvent.getAction() != 2) {
            Log.d("StatusBar", String.format("panel: %s at (%f, %f) mDisabled1=0x%08x mDisabled2=0x%08x", MotionEvent.actionToString(motionEvent.getAction()), Float.valueOf(motionEvent.getRawX()), Float.valueOf(motionEvent.getRawY()), Integer.valueOf(this.mDisabled1), Integer.valueOf(this.mDisabled2)));
        }
        if (this.mStatusBarWindowState == 0) {
            if ((motionEvent.getAction() == 1 || motionEvent.getAction() == 3) && !this.mExpandedVisible) {
                setInteracting(1, false);
            } else {
                setInteracting(1, true);
            }
        }
        return false;
    }

    public GestureRecorder getGestureRecorder() {
        return this.mGestureRec;
    }

    public FingerprintUnlockController getFingerprintUnlockController() {
        return this.mFingerprintUnlockController;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setWindowState(int i, int i2) {
        boolean z = i2 == 0;
        if (this.mStatusBarWindow != null && i == 1 && this.mStatusBarWindowState != i2) {
            this.mStatusBarWindowState = i2;
            if (!z && this.mState == 0) {
                this.mStatusBarView.collapsePanel(false, false, 1.0f);
            }
            if (this.mStatusBarView != null) {
                this.mStatusBarWindowHidden = i2 == 2;
                updateHideIconsForBouncer(false);
            }
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setSystemUiVisibility(int i, int i2, int i3, int i4, Rect rect, Rect rect2) {
        int i5 = this.mSystemUiVisibility;
        int i6 = ((~i4) & i5) | (i & i4);
        int i7 = i6 ^ i5;
        if (DEBUG) {
            Log.d("StatusBar", String.format("setSystemUiVisibility vis=%s mask=%s oldVal=%s newVal=%s diff=%s", Integer.toHexString(i), Integer.toHexString(i4), Integer.toHexString(i5), Integer.toHexString(i6), Integer.toHexString(i7)));
        }
        if (i7 != 0) {
            this.mSystemUiVisibility = i6;
            if ((i7 & 1) != 0) {
                setAreThereNotifications();
            }
            if ((268435456 & i) != 0) {
                this.mSystemUiVisibility &= -268435457;
                this.mNoAnimationOnNextBarModeChange = true;
            }
            int iComputeStatusBarMode = computeStatusBarMode(i5, i6);
            z = iComputeStatusBarMode != -1;
            if (z && iComputeStatusBarMode != this.mStatusBarMode) {
                this.mStatusBarMode = iComputeStatusBarMode;
                checkBarModes();
                touchAutoHide();
            }
            if ((i & 536870912) != 0) {
                this.mSystemUiVisibility &= -536870913;
            }
            notifyUiVisibilityChanged(this.mSystemUiVisibility);
        }
        this.mLightBarController.onSystemUiVisibilityChanged(i2, i3, i4, rect, rect2, z, this.mStatusBarMode);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showWirelessChargingAnimation(int i) {
        if (this.mDozing || this.mKeyguardManager.isKeyguardLocked()) {
            WirelessChargingAnimation.makeWirelessChargingAnimation(this.mContext, null, i, new WirelessChargingAnimation.Callback() { // from class: com.android.systemui.statusbar.phone.StatusBar.8
                AnonymousClass8() {
                }

                @Override // com.android.systemui.charging.WirelessChargingAnimation.Callback
                public void onAnimationStarting() {
                    CrossFadeHelper.fadeOut(StatusBar.this.mNotificationPanel, 1.0f);
                }

                @Override // com.android.systemui.charging.WirelessChargingAnimation.Callback
                public void onAnimationEnded() {
                    CrossFadeHelper.fadeIn(StatusBar.this.mNotificationPanel);
                }
            }, this.mDozing).show();
        } else {
            WirelessChargingAnimation.makeWirelessChargingAnimation(this.mContext, null, i, null, false).show();
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$8 */
    class AnonymousClass8 implements WirelessChargingAnimation.Callback {
        AnonymousClass8() {
        }

        @Override // com.android.systemui.charging.WirelessChargingAnimation.Callback
        public void onAnimationStarting() {
            CrossFadeHelper.fadeOut(StatusBar.this.mNotificationPanel, 1.0f);
        }

        @Override // com.android.systemui.charging.WirelessChargingAnimation.Callback
        public void onAnimationEnded() {
            CrossFadeHelper.fadeIn(StatusBar.this.mNotificationPanel);
        }
    }

    void touchAutoHide() {
        if (this.mStatusBarMode == 1 || (this.mNavigationBar != null && this.mNavigationBar.isSemiTransparent())) {
            scheduleAutohide();
        } else {
            cancelAutohide();
        }
    }

    protected int computeStatusBarMode(int i, int i2) {
        return computeBarMode(i, i2, 67108864, 1073741824, 8);
    }

    protected BarTransitions getStatusBarTransitions() {
        return this.mStatusBarView.getBarTransitions();
    }

    protected int computeBarMode(int i, int i2, int i3, int i4, int i5) {
        int iBarMode = barMode(i, i3, i4, i5);
        int iBarMode2 = barMode(i2, i3, i4, i5);
        if (iBarMode == iBarMode2) {
            return -1;
        }
        return iBarMode2;
    }

    private int barMode(int i, int i2, int i3, int i4) {
        int i5 = 1 | i4;
        if ((i2 & i) != 0) {
            return 1;
        }
        if ((i & i3) != 0) {
            return 2;
        }
        if ((i & i5) == i5) {
            return 6;
        }
        if ((i & i4) != 0) {
            return 4;
        }
        return (i & 1) != 0 ? 3 : 0;
    }

    void checkBarModes() {
        if (this.mDemoMode) {
            return;
        }
        if (this.mStatusBarView != null) {
            checkBarMode(this.mStatusBarMode, this.mStatusBarWindowState, getStatusBarTransitions());
        }
        if (this.mNavigationBar != null) {
            this.mNavigationBar.checkNavBarModes();
        }
        this.mNoAnimationOnNextBarModeChange = false;
    }

    void attachPlmnPlugin() {
        try {
            if (this.mStatusBarPlmnPlugin == null) {
                this.mSystemUIFactoryBase = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext);
                this.mStatusBarPlmnPlugin = this.mSystemUIFactoryBase.makeStatusBarPlmn(this.mContext);
                Log.d("StatusBar", "create plugin mStatusBarPlmnPlugin = " + this.mStatusBarPlmnPlugin);
            }
        } catch (Exception e) {
            Log.e("StatusBar", "mStatusBarPlmnPlugin init failed = " + e);
        }
        try {
            this.mStatusBarPlmnPlugin.addPlmn((LinearLayout) this.mStatusBarView.findViewById(R.id.status_bar_contents), this.mContext);
            this.mStatusBarPlmnPlugin.setPlmnVisibility(8);
        } catch (Exception e2) {
            Log.e("StatusBar", "exception = " + e2);
        }
    }

    void setQsScrimEnabled(boolean z) {
        this.mNotificationPanel.setQsScrimEnabled(z);
    }

    void checkBarMode(int i, int i2, BarTransitions barTransitions) {
        barTransitions.transitionTo(i, (this.mNoAnimationOnNextBarModeChange || !this.mDeviceInteractive || i2 == 2) ? false : true);
    }

    private void finishBarAnimations() {
        if (this.mStatusBarView != null) {
            this.mStatusBarView.getBarTransitions().finishAnimations();
        }
        if (this.mNavigationBar != null) {
            this.mNavigationBar.finishBarAnimations();
        }
    }

    public void setInteracting(int i, boolean z) {
        int i2;
        boolean z2 = ((this.mInteractingWindows & i) != 0) != z;
        if (z) {
            i2 = this.mInteractingWindows | i;
        } else {
            i2 = this.mInteractingWindows & (~i);
        }
        this.mInteractingWindows = i2;
        if (this.mInteractingWindows != 0) {
            suspendAutohide();
        } else {
            resumeSuspendedAutohide();
        }
        if (z2 && z && i == 2) {
            touchAutoDim();
            dismissVolumeDialog();
        }
        checkBarModes();
    }

    private void dismissVolumeDialog() {
        if (this.mVolumeComponent != null) {
            this.mVolumeComponent.dismissNow();
        }
    }

    private void resumeSuspendedAutohide() {
        if (this.mAutohideSuspended) {
            scheduleAutohide();
            this.mHandler.postDelayed(this.mCheckBarModes, 500L);
        }
    }

    private void suspendAutohide() {
        this.mHandler.removeCallbacks(this.mAutohide);
        this.mHandler.removeCallbacks(this.mCheckBarModes);
        this.mAutohideSuspended = (this.mSystemUiVisibility & 201326592) != 0;
    }

    private void cancelAutohide() {
        this.mAutohideSuspended = false;
        this.mHandler.removeCallbacks(this.mAutohide);
    }

    private void scheduleAutohide() {
        cancelAutohide();
        this.mHandler.postDelayed(this.mAutohide, 2250L);
    }

    public void touchAutoDim() {
        if (this.mNavigationBar != null) {
            this.mNavigationBar.getBarTransitions().setAutoDim(false);
        }
        this.mHandler.removeCallbacks(this.mAutoDim);
        if (this.mState != 1 && this.mState != 2) {
            this.mHandler.postDelayed(this.mAutoDim, 2250L);
        }
    }

    void checkUserAutohide(MotionEvent motionEvent) {
        if ((this.mSystemUiVisibility & 201326592) != 0 && motionEvent.getAction() == 4 && motionEvent.getX() == 0.0f && motionEvent.getY() == 0.0f && !this.mRemoteInputManager.getController().isRemoteInputActive()) {
            userAutohide();
        }
    }

    private void userAutohide() {
        cancelAutohide();
        this.mHandler.postDelayed(this.mAutohide, 350L);
    }

    private boolean areLightsOn() {
        return (this.mSystemUiVisibility & 1) == 0;
    }

    public void setLightsOn(boolean z) {
        Log.v("StatusBar", "setLightsOn(" + z + ")");
        if (z) {
            setSystemUiVisibility(0, 0, 0, 1, this.mLastFullscreenStackBounds, this.mLastDockedStackBounds);
        } else {
            setSystemUiVisibility(1, 0, 0, 1, this.mLastFullscreenStackBounds, this.mLastDockedStackBounds);
        }
    }

    private void notifyUiVisibilityChanged(int i) {
        try {
            if (this.mLastDispatchedSystemUiVisibility != i) {
                this.mWindowManagerService.statusBarVisibilityChanged(i);
                this.mLastDispatchedSystemUiVisibility = i;
            }
        } catch (RemoteException e) {
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void topAppWindowChanged(boolean z) {
        if (z) {
            setLightsOn(true);
        }
    }

    public static String viewInfo(View view) {
        return "[(" + view.getLeft() + "," + view.getTop() + ")(" + view.getRight() + "," + view.getBottom() + ") " + view.getWidth() + "x" + view.getHeight() + "]";
    }

    @Override // com.android.systemui.SystemUI
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mQueueLock) {
            printWriter.println("Current Status Bar state:");
            printWriter.println("  mExpandedVisible=" + this.mExpandedVisible);
            printWriter.println("  mDisplayMetrics=" + this.mDisplayMetrics);
            printWriter.println("  mStackScroller: " + viewInfo(this.mStackScroller));
            printWriter.println("  mStackScroller: " + viewInfo(this.mStackScroller) + " scroll " + this.mStackScroller.getScrollX() + "," + this.mStackScroller.getScrollY());
        }
        printWriter.print("  mInteractingWindows=");
        printWriter.println(this.mInteractingWindows);
        printWriter.print("  mStatusBarWindowState=");
        printWriter.println(StatusBarManager.windowStateToString(this.mStatusBarWindowState));
        printWriter.print("  mStatusBarMode=");
        printWriter.println(BarTransitions.modeToString(this.mStatusBarMode));
        printWriter.print("  mDozing=");
        printWriter.println(this.mDozing);
        printWriter.print("  mZenMode=");
        printWriter.println(Settings.Global.zenModeToString(Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0)));
        if (this.mStatusBarView != null) {
            dumpBarTransitions(printWriter, "mStatusBarView", this.mStatusBarView.getBarTransitions());
        }
        printWriter.println("  StatusBarWindowView: ");
        if (this.mStatusBarWindow != null) {
            this.mStatusBarWindow.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  mMediaManager: ");
        if (this.mMediaManager != null) {
            this.mMediaManager.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  Panels: ");
        if (this.mNotificationPanel != null) {
            printWriter.println("    mNotificationPanel=" + this.mNotificationPanel + " params=" + this.mNotificationPanel.getLayoutParams().debug(""));
            printWriter.print("      ");
            this.mNotificationPanel.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  mStackScroller: ");
        if (this.mStackScroller != null) {
            printWriter.print("      ");
            this.mStackScroller.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  Theme:");
        if (this.mOverlayManager == null) {
            printWriter.println("    overlay manager not initialized!");
        } else {
            printWriter.println("    dark overlay on: " + isUsingDarkTheme());
        }
        printWriter.println("    light wallpaper theme: " + (this.mContext.getThemeResId() == 2131886646));
        DozeLog.dump(printWriter);
        if (this.mFingerprintUnlockController != null) {
            this.mFingerprintUnlockController.dump(printWriter);
        }
        if (this.mKeyguardIndicationController != null) {
            this.mKeyguardIndicationController.dump(fileDescriptor, printWriter, strArr);
        }
        if (this.mScrimController != null) {
            this.mScrimController.dump(fileDescriptor, printWriter, strArr);
        }
        if (this.mStatusBarKeyguardViewManager != null) {
            this.mStatusBarKeyguardViewManager.dump(printWriter);
        }
        synchronized (this.mEntryManager.getNotificationData()) {
            this.mEntryManager.getNotificationData().dump(printWriter, "  ");
        }
        if (this.mHeadsUpManager != null) {
            this.mHeadsUpManager.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("  mHeadsUpManager: null");
        }
        if (this.mGroupManager != null) {
            this.mGroupManager.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("  mGroupManager: null");
        }
        if (this.mLightBarController != null) {
            this.mLightBarController.dump(fileDescriptor, printWriter, strArr);
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext) != null) {
            KeyguardUpdateMonitor.getInstance(this.mContext).dump(fileDescriptor, printWriter, strArr);
        }
        FalsingManager.getInstance(this.mContext).dump(printWriter);
        FalsingLog.dump(printWriter);
        printWriter.println("SharedPreferences:");
        for (Map.Entry<String, ?> entry : Prefs.getAll(this.mContext).entrySet()) {
            printWriter.print("  ");
            printWriter.print(entry.getKey());
            printWriter.print("=");
            printWriter.println(entry.getValue());
        }
    }

    static void dumpBarTransitions(PrintWriter printWriter, String str, BarTransitions barTransitions) {
        printWriter.print("  ");
        printWriter.print(str);
        printWriter.print(".BarTransitions.mMode=");
        printWriter.println(BarTransitions.modeToString(barTransitions.getMode()));
    }

    public void createAndAddWindows() throws Resources.NotFoundException {
        addStatusBarWindow();
    }

    private void addStatusBarWindow() throws Resources.NotFoundException {
        makeStatusBarView();
        this.mStatusBarWindowManager = (StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class);
        this.mRemoteInputManager.setUpWithPresenter(this, this.mEntryManager, this, new RemoteInputController.Delegate() { // from class: com.android.systemui.statusbar.phone.StatusBar.9
            AnonymousClass9() {
            }

            @Override // com.android.systemui.statusbar.RemoteInputController.Delegate
            public void setRemoteInputActive(NotificationData.Entry entry, boolean z) {
                StatusBar.this.mHeadsUpManager.setRemoteInputActive(entry, z);
                entry.row.notifyHeightChanged(true);
                StatusBar.this.updateFooter();
            }

            @Override // com.android.systemui.statusbar.RemoteInputController.Delegate
            public void lockScrollTo(NotificationData.Entry entry) {
                StatusBar.this.mStackScroller.lockScrollTo(entry.row);
            }

            @Override // com.android.systemui.statusbar.RemoteInputController.Delegate
            public void requestDisallowLongPressAndDismiss() {
                StatusBar.this.mStackScroller.requestDisallowLongPress();
                StatusBar.this.mStackScroller.requestDisallowDismiss();
            }
        });
        this.mRemoteInputManager.getController().addCallback(this.mStatusBarWindowManager);
        this.mStatusBarWindowManager.add(this.mStatusBarWindow, getStatusBarHeight());
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$9 */
    class AnonymousClass9 implements RemoteInputController.Delegate {
        AnonymousClass9() {
        }

        @Override // com.android.systemui.statusbar.RemoteInputController.Delegate
        public void setRemoteInputActive(NotificationData.Entry entry, boolean z) {
            StatusBar.this.mHeadsUpManager.setRemoteInputActive(entry, z);
            entry.row.notifyHeightChanged(true);
            StatusBar.this.updateFooter();
        }

        @Override // com.android.systemui.statusbar.RemoteInputController.Delegate
        public void lockScrollTo(NotificationData.Entry entry) {
            StatusBar.this.mStackScroller.lockScrollTo(entry.row);
        }

        @Override // com.android.systemui.statusbar.RemoteInputController.Delegate
        public void requestDisallowLongPressAndDismiss() {
            StatusBar.this.mStackScroller.requestDisallowLongPress();
            StatusBar.this.mStackScroller.requestDisallowDismiss();
        }
    }

    void updateDisplaySize() {
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        this.mDisplay.getSize(this.mCurrentDisplaySize);
    }

    float getDisplayDensity() {
        return this.mDisplayMetrics.density;
    }

    float getDisplayWidth() {
        return this.mDisplayMetrics.widthPixels;
    }

    float getDisplayHeight() {
        return this.mDisplayMetrics.heightPixels;
    }

    int getRotation() {
        return this.mDisplay.getRotation();
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean z, boolean z2, int i) {
        startActivityDismissingKeyguard(intent, z, z2, false, null, i);
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean z, boolean z2) {
        startActivityDismissingKeyguard(intent, z, z2, 0);
    }

    public void startActivityDismissingKeyguard(final Intent intent, boolean z, boolean z2, final boolean z3, final ActivityStarter.Callback callback, final int i) {
        if (!z || isDeviceProvisioned()) {
            executeRunnableDismissingKeyguard(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$m7gLn0oHyYCe-AWuyLsfac1q_Y8
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBar.lambda$startActivityDismissingKeyguard$23(this.f$0, intent, i, z3, callback);
                }
            }, new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$1nGExZ91Gpu3EQgUFwihi2varHw
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBar.lambda$startActivityDismissingKeyguard$24(callback);
                }
            }, z2, PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent, this.mLockscreenUserManager.getCurrentUserId()), true);
        }
    }

    public static /* synthetic */ void lambda$startActivityDismissingKeyguard$23(StatusBar statusBar, Intent intent, int i, boolean z, ActivityStarter.Callback callback) {
        int iStartActivityAsUser;
        statusBar.mAssistManager.hideAssist();
        intent.setFlags(335544320);
        intent.addFlags(i);
        ActivityOptions activityOptions = new ActivityOptions(statusBar.getActivityOptions(null));
        activityOptions.setDisallowEnterPictureInPictureWhileLaunching(z);
        if (intent == KeyguardBottomAreaView.INSECURE_CAMERA_INTENT) {
            activityOptions.setRotationAnimationHint(3);
        }
        try {
            iStartActivityAsUser = ActivityManager.getService().startActivityAsUser((IApplicationThread) null, statusBar.mContext.getBasePackageName(), intent, intent.resolveTypeIfNeeded(statusBar.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, activityOptions.toBundle(), UserHandle.CURRENT.getIdentifier());
        } catch (RemoteException e) {
            Log.w("StatusBar", "Unable to start activity", e);
            iStartActivityAsUser = -96;
        }
        if (callback != null) {
            callback.onActivityStarted(iStartActivityAsUser);
        }
    }

    static /* synthetic */ void lambda$startActivityDismissingKeyguard$24(ActivityStarter.Callback callback) {
        if (callback != null) {
            callback.onActivityStarted(-96);
        }
    }

    public void readyForKeyguardDone() {
        this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    public void executeRunnableDismissingKeyguard(final Runnable runnable, Runnable runnable2, final boolean z, boolean z2, final boolean z3) {
        dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$2o5DqwU7a8_lWJ0tlZXd7MAULTQ
            @Override // com.android.keyguard.KeyguardHostView.OnDismissAction
            public final boolean onDismiss() {
                return StatusBar.lambda$executeRunnableDismissingKeyguard$25(this.f$0, runnable, z, z3);
            }
        }, runnable2, z2);
    }

    public static /* synthetic */ boolean lambda$executeRunnableDismissingKeyguard$25(final StatusBar statusBar, Runnable runnable, boolean z, boolean z2) {
        if (runnable != null) {
            if (statusBar.mStatusBarKeyguardViewManager.isShowing() && statusBar.mStatusBarKeyguardViewManager.isOccluded()) {
                statusBar.mStatusBarKeyguardViewManager.addAfterKeyguardGoneRunnable(runnable);
            } else {
                AsyncTask.execute(runnable);
            }
        }
        if (z) {
            if (statusBar.mExpandedVisible) {
                statusBar.animateCollapsePanels(2, true, true);
            } else {
                statusBar.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$h1YVkfulr3o8W-Bsc2YTikmPmYI
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.runPostCollapseRunnables();
                    }
                });
            }
        } else if (statusBar.isInLaunchTransition() && statusBar.mNotificationPanel.isLaunchTransitionFinished()) {
            H h = statusBar.mHandler;
            final StatusBarKeyguardViewManager statusBarKeyguardViewManager = statusBar.mStatusBarKeyguardViewManager;
            Objects.requireNonNull(statusBarKeyguardViewManager);
            h.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$JQMd1r5WuAA5n3kv4yv5u3MFjI8
                @Override // java.lang.Runnable
                public final void run() {
                    statusBarKeyguardViewManager.readyForKeyguardDone();
                }
            });
        }
        return z2;
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$10 */
    class AnonymousClass10 extends BroadcastReceiver {
        AnonymousClass10() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (StatusBar.DEBUG) {
                Log.v("StatusBar", "onReceive: " + intent);
            }
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                KeyboardShortcuts.dismiss();
                if (StatusBar.this.mRemoteInputManager.getController() != null) {
                    StatusBar.this.mRemoteInputManager.getController().closeRemoteInputs();
                }
                if (StatusBar.this.mLockscreenUserManager.isCurrentProfile(getSendingUserId())) {
                    int i = 0;
                    String stringExtra = intent.getStringExtra("reason");
                    if (stringExtra != null && stringExtra.equals("recentapps")) {
                        i = 2;
                    }
                    StatusBar.this.animateCollapsePanels(i);
                    return;
                }
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                StatusBar.this.finishBarAnimations();
                StatusBar.this.resetUserExpandedStates();
            } else if ("android.app.action.SHOW_DEVICE_MONITORING_DIALOG".equals(action)) {
                StatusBar.this.mQSPanel.showDeviceMonitoringDialog();
            }
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$11 */
    class AnonymousClass11 extends BroadcastReceiver {
        AnonymousClass11() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (StatusBar.DEBUG) {
                Log.v("StatusBar", "onReceive: " + intent);
            }
            String action = intent.getAction();
            if ("com.android.systemui.demo".equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String lowerCase = extras.getString("command", "").trim().toLowerCase();
                    if (lowerCase.length() > 0) {
                        try {
                            StatusBar.this.dispatchDemoCommand(lowerCase, extras);
                            return;
                        } catch (Throwable th) {
                            Log.w("StatusBar", "Error running demo command, intent=" + intent, th);
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            "fake_artwork".equals(action);
        }
    }

    public void resetUserExpandedStates() {
        ArrayList<NotificationData.Entry> activeNotifications = this.mEntryManager.getNotificationData().getActiveNotifications();
        int size = activeNotifications.size();
        for (int i = 0; i < size; i++) {
            NotificationData.Entry entry = activeNotifications.get(i);
            if (entry.row != null) {
                entry.row.resetUserExpansion();
            }
        }
    }

    private void executeWhenUnlocked(KeyguardHostView.OnDismissAction onDismissAction) {
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mLeaveOpenOnKeyguardHide = true;
        }
        dismissKeyguardThenExecute(onDismissAction, null, false);
    }

    protected void dismissKeyguardThenExecute(KeyguardHostView.OnDismissAction onDismissAction, boolean z) {
        dismissKeyguardThenExecute(onDismissAction, null, z);
    }

    private void dismissKeyguardThenExecute(KeyguardHostView.OnDismissAction onDismissAction, Runnable runnable, boolean z) {
        if (this.mWakefulnessLifecycle.getWakefulness() == 0 && this.mUnlockMethodCache.canSkipBouncer() && !this.mLeaveOpenOnKeyguardHide && isPulsing()) {
            this.mFingerprintUnlockController.startWakeAndUnlock(2);
        }
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mStatusBarKeyguardViewManager.dismissWithAction(onDismissAction, runnable, z);
        } else {
            onDismissAction.onDismiss();
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onConfigChanged(Configuration configuration) {
        updateResources();
        updateDisplaySize();
        if (DEBUG) {
            Log.v("StatusBar", "configuration changed: " + this.mContext.getResources().getConfiguration());
        }
        this.mViewHierarchyManager.updateRowStates();
        this.mScreenPinningRequest.onConfigurationChanged();
    }

    public void onUserSwitched(int i) {
        setHeadsUpUser(i);
        animateCollapsePanels();
        updatePublicMode();
        this.mEntryManager.getNotificationData().filterAndSort();
        if (this.mReinflateNotificationsOnUserSwitched) {
            this.mEntryManager.updateNotificationsOnDensityOrFontScaleChanged();
            this.mReinflateNotificationsOnUserSwitched = false;
        }
        updateNotificationViews();
        this.mMediaManager.clearCurrentMediaNotification();
        setLockscreenUser(i);
    }

    public NotificationLockscreenUserManager getNotificationLockscreenUserManager() {
        return this.mLockscreenUserManager;
    }

    @Override // com.android.systemui.statusbar.NotificationEntryManager.Callback
    public void onBindRow(NotificationData.Entry entry, PackageManager packageManager, StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow) {
        expandableNotificationRow.setAboveShelfChangedListener(this.mAboveShelfObserver);
        expandableNotificationRow.setSecureStateProvider(new BooleanSupplier() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$Er5d2KQJ-uxw_IoSsj98m9LUD4U
            @Override // java.util.function.BooleanSupplier
            public final boolean getAsBoolean() {
                return this.f$0.isKeyguardCurrentlySecure();
            }
        });
    }

    protected void setLockscreenUser(int i) {
        this.mLockscreenWallpaper.setCurrentUser(i);
        this.mScrimController.setCurrentUser(i);
        updateMediaMetaData(true, false);
    }

    void updateResources() {
        if (this.mQSPanel != null) {
            this.mQSPanel.updateResources();
        }
        loadDimens();
        if (this.mStatusBarView != null) {
            this.mStatusBarView.updateResources();
        }
        if (this.mNotificationPanel != null) {
            this.mNotificationPanel.updateResources();
        }
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.updateResources();
        }
    }

    protected void loadDimens() {
        Resources resources = this.mContext.getResources();
        int i = this.mNaturalBarHeight;
        this.mNaturalBarHeight = resources.getDimensionPixelSize(android.R.dimen.indeterminate_progress_alpha_16);
        if (this.mStatusBarWindowManager != null && this.mNaturalBarHeight != i) {
            this.mStatusBarWindowManager.setBarHeight(this.mNaturalBarHeight);
        }
        this.mMaxAllowedKeyguardNotifications = resources.getInteger(R.integer.keyguard_max_notification_count);
        if (DEBUG) {
            Log.v("StatusBar", "defineSlots");
        }
    }

    protected void handleVisibleToUserChanged(boolean z) {
        if (z) {
            handleVisibleToUserChangedImpl(z);
            this.mNotificationLogger.startNotificationLogging();
        } else {
            this.mNotificationLogger.stopNotificationLogging();
            handleVisibleToUserChangedImpl(z);
        }
    }

    void handlePeekToExpandTransistion() {
        try {
            this.mBarService.onPanelRevealed(false, this.mEntryManager.getNotificationData().getActiveNotifications().size());
        } catch (RemoteException e) {
        }
    }

    private void handleVisibleToUserChangedImpl(boolean z) {
        final boolean z2;
        if (z) {
            boolean zHasPinnedHeadsUp = this.mHeadsUpManager.hasPinnedHeadsUp();
            if (isPresenterFullyCollapsed() || (this.mState != 0 && this.mState != 2)) {
                z2 = false;
            } else {
                z2 = true;
            }
            final int size = (zHasPinnedHeadsUp && isPresenterFullyCollapsed()) ? 1 : this.mEntryManager.getNotificationData().getActiveNotifications().size();
            this.mUiOffloadThread.submit(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$L7CWWY68vvaxHKvHpan4z_UceUU
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.mBarService.onPanelRevealed(z2, size);
                }
            });
            return;
        }
        this.mUiOffloadThread.submit(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$J1ouXjVsiGbik-dFy8e1gkHbor8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.mBarService.onPanelHidden();
            }
        });
    }

    private void logStateToEventlog() {
        boolean zIsShowing = this.mStatusBarKeyguardViewManager.isShowing();
        boolean zIsOccluded = this.mStatusBarKeyguardViewManager.isOccluded();
        boolean zIsBouncerShowing = this.mStatusBarKeyguardViewManager.isBouncerShowing();
        boolean zIsMethodSecure = this.mUnlockMethodCache.isMethodSecure();
        boolean zCanSkipBouncer = this.mUnlockMethodCache.canSkipBouncer();
        int loggingFingerprint = getLoggingFingerprint(this.mState, zIsShowing, zIsOccluded, zIsBouncerShowing, zIsMethodSecure, zCanSkipBouncer);
        if (loggingFingerprint != this.mLastLoggedStateFingerprint) {
            if (this.mStatusBarStateLog == null) {
                this.mStatusBarStateLog = new LogMaker(0);
            }
            this.mMetricsLogger.write(this.mStatusBarStateLog.setCategory(zIsBouncerShowing ? 197 : 196).setType(zIsShowing ? 1 : 2).setSubtype(zIsMethodSecure ? 1 : 0));
            EventLogTags.writeSysuiStatusBarState(this.mState, zIsShowing ? 1 : 0, zIsOccluded ? 1 : 0, zIsBouncerShowing ? 1 : 0, zIsMethodSecure ? 1 : 0, zCanSkipBouncer ? 1 : 0);
            this.mLastLoggedStateFingerprint = loggingFingerprint;
        }
    }

    private static int getLoggingFingerprint(int i, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
        return (i & 255) | ((z ? 1 : 0) << 8) | ((z2 ? 1 : 0) << 9) | ((z3 ? 1 : 0) << 10) | ((z4 ? 1 : 0) << 11) | ((z5 ? 1 : 0) << 12);
    }

    void vibrate() {
        ((Vibrator) this.mContext.getSystemService("vibrator")).vibrate(250L, VIBRATION_ATTRIBUTES);
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$12 */
    class AnonymousClass12 implements Runnable {
        AnonymousClass12() {
        }

        @Override // java.lang.Runnable
        public void run() {
            StatusBar.this.vibrate();
            SystemClock.sleep(250L);
            Log.d("StatusBar", "startTracing");
            Debug.startMethodTracing("/data/statusbar-traces/trace");
            StatusBar.this.mHandler.postDelayed(StatusBar.this.mStopTracing, 10000L);
        }
    }

    public static /* synthetic */ void lambda$new$28(StatusBar statusBar) {
        Debug.stopMethodTracing();
        Log.d("StatusBar", "stopTracing");
        statusBar.vibrate();
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postQSRunnableDismissingKeyguard(final Runnable runnable) {
        this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$VX0QSn8fcyOHYuFyA6Hcoa2Oyxs
            @Override // java.lang.Runnable
            public final void run() {
                StatusBar.lambda$postQSRunnableDismissingKeyguard$30(this.f$0, runnable);
            }
        });
    }

    public static /* synthetic */ void lambda$postQSRunnableDismissingKeyguard$30(final StatusBar statusBar, final Runnable runnable) {
        statusBar.mLeaveOpenOnKeyguardHide = true;
        statusBar.executeRunnableDismissingKeyguard(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$nBgz7pREWZfdv3WheeRgDYm-DKk
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.mHandler.post(runnable);
            }
        }, null, false, false, false);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postStartActivityDismissingKeyguard(final PendingIntent pendingIntent) {
        this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$12UpvPh4ei1S2fz4WfmdUVU3L0Q
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.startPendingIntentDismissingKeyguard(pendingIntent);
            }
        });
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postStartActivityDismissingKeyguard(final Intent intent, int i) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$N82XexCiKemcup5oDmr0rEQGLr8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.handleStartActivityDismissingKeyguard(intent, true);
            }
        }, i);
    }

    private void handleStartActivityDismissingKeyguard(Intent intent, boolean z) {
        startActivityDismissingKeyguard(intent, z, true);
    }

    @Override // com.android.systemui.DemoMode
    public void dispatchDemoCommand(String str, Bundle bundle) {
        int i = 0;
        if (!this.mDemoModeAllowed) {
            this.mDemoModeAllowed = Settings.Global.getInt(this.mContext.getContentResolver(), "sysui_demo_allowed", 0) != 0;
        }
        if (this.mDemoModeAllowed) {
            if (str.equals("enter")) {
                this.mDemoMode = true;
            } else if (str.equals("exit")) {
                this.mDemoMode = false;
                checkBarModes();
            } else if (!this.mDemoMode) {
                dispatchDemoCommand("enter", new Bundle());
            }
            boolean z = str.equals("enter") || str.equals("exit");
            if ((z || str.equals("volume")) && this.mVolumeComponent != null) {
                this.mVolumeComponent.dispatchDemoCommand(str, bundle);
            }
            if (z || str.equals("clock")) {
                dispatchDemoCommandToView(str, bundle, R.id.clock);
            }
            if (z || str.equals("battery")) {
                this.mBatteryController.dispatchDemoCommand(str, bundle);
            }
            if (z || str.equals("status")) {
                ((StatusBarIconControllerImpl) this.mIconController).dispatchDemoCommand(str, bundle);
            }
            if (this.mNetworkController != null && (z || str.equals("network"))) {
                this.mNetworkController.dispatchDemoCommand(str, bundle);
            }
            if (z || str.equals("notifications")) {
                View viewFindViewById = this.mStatusBarView == null ? null : this.mStatusBarView.findViewById(R.id.notification_icon_area);
                if (viewFindViewById != null) {
                    viewFindViewById.setVisibility((this.mDemoMode && "false".equals(bundle.getString("visible"))) ? 4 : 0);
                }
            }
            if (str.equals("bars")) {
                String string = bundle.getString("mode");
                if (!"opaque".equals(string)) {
                    if ("translucent".equals(string)) {
                        i = 2;
                    } else {
                        i = "semi-transparent".equals(string) ? 1 : "transparent".equals(string) ? 4 : "warning".equals(string) ? 5 : -1;
                    }
                }
                if (i != -1) {
                    if (this.mStatusBarView != null) {
                        this.mStatusBarView.getBarTransitions().transitionTo(i, true);
                    }
                    if (this.mNavigationBar != null) {
                        this.mNavigationBar.getBarTransitions().transitionTo(i, true);
                    }
                }
            }
            if (z || str.equals("operator")) {
                dispatchDemoCommandToView(str, bundle, R.id.operator_name);
            }
        }
    }

    private void dispatchDemoCommandToView(String str, Bundle bundle, int i) {
        if (this.mStatusBarView == null) {
            return;
        }
        KeyEvent.Callback callbackFindViewById = this.mStatusBarView.findViewById(i);
        if (callbackFindViewById instanceof DemoMode) {
            ((DemoMode) callbackFindViewById).dispatchDemoCommand(str, bundle);
        }
    }

    public int getBarState() {
        return this.mState;
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public boolean isPresenterFullyCollapsed() {
        return this.mNotificationPanel.isFullyCollapsed();
    }

    public void showKeyguard() {
        this.mKeyguardRequested = true;
        this.mLeaveOpenOnKeyguardHide = false;
        this.mPendingRemoteInputView = null;
        updateIsKeyguard();
        this.mAssistManager.onLockscreenShown();
    }

    public boolean hideKeyguard() {
        this.mKeyguardRequested = false;
        return updateIsKeyguard();
    }

    public boolean isFullScreenUserSwitcherState() {
        return this.mState == 3;
    }

    private boolean updateIsKeyguard() {
        boolean z = this.mFingerprintUnlockController.getMode() == 1;
        boolean z2 = this.mDozingRequested && (!this.mDeviceInteractive || (isGoingToSleep() && (isScreenFullyOff() || this.mIsKeyguard)));
        boolean z3 = (this.mKeyguardRequested || z2) && !z;
        if (z2) {
            updatePanelExpansionForKeyguard();
        }
        if (z3) {
            if (!isGoingToSleep() || this.mScreenLifecycle.getScreenState() != 3) {
                showKeyguardImpl();
            }
            return false;
        }
        return hideKeyguardImpl();
    }

    public void showKeyguardImpl() {
        this.mIsKeyguard = true;
        if (this.mLaunchTransitionFadingAway) {
            this.mNotificationPanel.animate().cancel();
            onLaunchTransitionFadingEnded();
        }
        this.mHandler.removeMessages(1003);
        if (this.mUserSwitcherController != null && this.mUserSwitcherController.useFullscreenUserSwitcher()) {
            setBarState(3);
        } else {
            setBarState(1);
        }
        updateKeyguardState(false, false);
        updatePanelExpansionForKeyguard();
        if (this.mDraggedDownRow != null) {
            this.mDraggedDownRow.setUserLocked(false);
            this.mDraggedDownRow.notifyHeightChanged(false);
            this.mDraggedDownRow = null;
        }
    }

    private void updatePanelExpansionForKeyguard() {
        if (this.mState == 1 && this.mFingerprintUnlockController.getMode() != 1) {
            instantExpandNotificationsPanel();
        } else if (this.mState == 3) {
            instantCollapseNotificationPanel();
        }
    }

    private void onLaunchTransitionFadingEnded() {
        this.mNotificationPanel.setAlpha(1.0f);
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        runLaunchTransitionEndRunnable();
        this.mLaunchTransitionFadingAway = false;
        updateMediaMetaData(true, true);
    }

    public boolean isCollapsing() {
        return this.mNotificationPanel.isCollapsing() || this.mActivityLaunchAnimator.isAnimationPending();
    }

    public void addPostCollapseAction(Runnable runnable) {
        this.mPostCollapseRunnables.add(runnable);
    }

    public boolean isInLaunchTransition() {
        return this.mNotificationPanel.isLaunchTransitionRunning() || this.mNotificationPanel.isLaunchTransitionFinished();
    }

    public void fadeKeyguardAfterLaunchTransition(final Runnable runnable, Runnable runnable2) {
        this.mHandler.removeMessages(1003);
        this.mLaunchTransitionEndRunnable = runnable2;
        Runnable runnable3 = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$VGVVEb9vvRl3UTavYqmJZkWnKBw
            @Override // java.lang.Runnable
            public final void run() {
                StatusBar.lambda$fadeKeyguardAfterLaunchTransition$33(this.f$0, runnable);
            }
        };
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            this.mNotificationPanel.setLaunchTransitionEndRunnable(runnable3);
        } else {
            runnable3.run();
        }
    }

    public static /* synthetic */ void lambda$fadeKeyguardAfterLaunchTransition$33(final StatusBar statusBar, Runnable runnable) {
        statusBar.mLaunchTransitionFadingAway = true;
        if (runnable != null) {
            runnable.run();
        }
        statusBar.updateScrimController();
        statusBar.updateMediaMetaData(false, true);
        statusBar.mNotificationPanel.setAlpha(1.0f);
        statusBar.mStackScroller.setParentNotFullyVisible(true);
        statusBar.mNotificationPanel.animate().alpha(0.0f).setStartDelay(100L).setDuration(300L).withLayer().withEndAction(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$GDSEpzokV1v2-uNGuP8V5K9Jrjw
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.onLaunchTransitionFadingEnded();
            }
        });
        statusBar.mCommandQueue.appTransitionStarting(SystemClock.uptimeMillis(), 120L, true);
    }

    public void fadeKeyguardWhilePulsing() {
        this.mNotificationPanel.notifyStartFading();
        this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(0L).setDuration(96L).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$WXAwBNkDB04G_NTCGmQtREbS760
            @Override // java.lang.Runnable
            public final void run() {
                StatusBar.lambda$fadeKeyguardWhilePulsing$34(this.f$0);
            }
        }).start();
    }

    public static /* synthetic */ void lambda$fadeKeyguardWhilePulsing$34(StatusBar statusBar) {
        statusBar.hideKeyguard();
        statusBar.mStatusBarKeyguardViewManager.onKeyguardFadedAway();
    }

    public void animateKeyguardUnoccluding() {
        this.mNotificationPanel.setExpandedFraction(0.0f);
        animateExpandNotificationsPanel();
    }

    public void startLaunchTransitionTimeout() {
        this.mHandler.sendEmptyMessageDelayed(1003, 5000L);
    }

    private void onLaunchTransitionTimeout() {
        Log.w("StatusBar", "Launch transition: Timeout!");
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        this.mNotificationPanel.resetViews();
        this.mStatusBarKeyguardViewManager.reset(true);
    }

    private void runLaunchTransitionEndRunnable() {
        if (this.mLaunchTransitionEndRunnable != null) {
            Runnable runnable = this.mLaunchTransitionEndRunnable;
            this.mLaunchTransitionEndRunnable = null;
            runnable.run();
        }
    }

    public boolean hideKeyguardImpl() {
        this.mIsKeyguard = false;
        Trace.beginSection("StatusBar#hideKeyguard");
        boolean z = this.mLeaveOpenOnKeyguardHide;
        setBarState(0);
        View view = null;
        if (this.mLeaveOpenOnKeyguardHide) {
            if (!this.mKeyguardRequested) {
                this.mLeaveOpenOnKeyguardHide = false;
            }
            long jCalculateGoingToFullShadeDelay = calculateGoingToFullShadeDelay();
            this.mNotificationPanel.animateToFullShade(jCalculateGoingToFullShadeDelay);
            if (this.mDraggedDownRow != null) {
                this.mDraggedDownRow.setUserLocked(false);
                this.mDraggedDownRow = null;
            }
            if (!this.mKeyguardRequested) {
                View view2 = this.mPendingRemoteInputView;
                this.mPendingRemoteInputView = null;
                view = view2;
            }
            if (this.mNavigationBar != null) {
                this.mNavigationBar.disableAnimationsDuringHide(jCalculateGoingToFullShadeDelay);
            }
        } else if (!this.mNotificationPanel.isCollapsing()) {
            instantCollapseNotificationPanel();
        }
        updateKeyguardState(z, false);
        if (view != null && view.isAttachedToWindow()) {
            view.callOnClick();
        }
        if (this.mQSPanel != null) {
            this.mQSPanel.refreshAllTiles();
        }
        this.mHandler.removeMessages(1003);
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
        this.mNotificationPanel.animate().cancel();
        this.mNotificationPanel.setAlpha(1.0f);
        Trace.endSection();
        return z;
    }

    private void releaseGestureWakeLock() {
        if (this.mGestureWakeLock.isHeld()) {
            this.mGestureWakeLock.release();
        }
    }

    public long calculateGoingToFullShadeDelay() {
        return this.mKeyguardFadingAwayDelay + this.mKeyguardFadingAwayDuration;
    }

    public void keyguardGoingAway() {
        this.mKeyguardMonitor.notifyKeyguardGoingAway(true);
        this.mCommandQueue.appTransitionPending(true);
    }

    public void setKeyguardFadingAway(long j, long j2, long j3) {
        this.mKeyguardFadingAway = true;
        this.mKeyguardFadingAwayDelay = j2;
        this.mKeyguardFadingAwayDuration = j3;
        this.mCommandQueue.appTransitionStarting((j + j3) - 120, 120L, true);
        recomputeDisableFlags(j3 > 0);
        this.mCommandQueue.appTransitionStarting(j - 120, 120L, true);
        this.mKeyguardMonitor.notifyKeyguardFadingAway(j2, j3);
    }

    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    public void finishKeyguardFadingAway() {
        this.mKeyguardFadingAway = false;
        this.mKeyguardMonitor.notifyKeyguardDoneFading();
        this.mScrimController.setExpansionAffectsAlpha(true);
    }

    private void updatePublicMode() {
        boolean z;
        boolean zIsDeviceLocked;
        if (!this.mStatusBarKeyguardViewManager.isShowing() || !this.mStatusBarKeyguardViewManager.isSecure(this.mLockscreenUserManager.getCurrentUserId())) {
            z = false;
        } else {
            z = true;
        }
        SparseArray<UserInfo> currentProfiles = this.mLockscreenUserManager.getCurrentProfiles();
        for (int size = currentProfiles.size() - 1; size >= 0; size--) {
            int i = currentProfiles.valueAt(size).id;
            if (!z && i != this.mLockscreenUserManager.getCurrentUserId() && this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i) && this.mStatusBarKeyguardViewManager.isSecure(i)) {
                zIsDeviceLocked = this.mKeyguardManager.isDeviceLocked(i);
            } else {
                zIsDeviceLocked = z;
            }
            this.mLockscreenUserManager.setLockscreenPublicMode(zIsDeviceLocked, i);
        }
    }

    protected void updateKeyguardState(boolean z, boolean z2) {
        Trace.beginSection("StatusBar#updateKeyguardState");
        if (this.mState == 1) {
            this.mKeyguardIndicationController.setVisible(true);
            this.mNotificationPanel.resetViews();
            if (this.mKeyguardUserSwitcher != null) {
                this.mKeyguardUserSwitcher.setKeyguard(true, z2);
            }
            if (this.mStatusBarView != null) {
                this.mStatusBarView.removePendingHideExpandedRunnables();
            }
            if (this.mAmbientIndicationContainer != null) {
                this.mAmbientIndicationContainer.setVisibility(0);
            }
        } else {
            this.mKeyguardIndicationController.setVisible(false);
            if (this.mKeyguardUserSwitcher != null) {
                this.mKeyguardUserSwitcher.setKeyguard(false, z || this.mState == 2 || z2);
            }
            if (this.mAmbientIndicationContainer != null) {
                this.mAmbientIndicationContainer.setVisibility(4);
            }
        }
        this.mNotificationPanel.setBarState(this.mState, this.mKeyguardFadingAway, z);
        updateTheme();
        updateDozingState();
        updatePublicMode();
        updateStackScrollerState(z, z2);
        this.mEntryManager.updateNotifications();
        checkBarModes();
        updateCarrierLabelVisibility(false);
        updateScrimController();
        updateMediaMetaData(false, this.mState != 1);
        this.mKeyguardMonitor.notifyKeyguardState(this.mStatusBarKeyguardViewManager.isShowing(), this.mUnlockMethodCache.isMethodSecure(), this.mStatusBarKeyguardViewManager.isOccluded());
        Trace.endSection();
    }

    protected void updateTheme() {
        final boolean z = false;
        boolean z2 = this.mStackScroller != null;
        WallpaperColors wallpaperColors = this.mColorExtractor.getWallpaperColors(1);
        int i = 2;
        if (wallpaperColors != null && (wallpaperColors.getColorHints() & 2) != 0) {
            z = true;
        }
        if (isUsingDarkTheme() != z) {
            this.mUiOffloadThread.submit(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$ivKR6xDECCYeSWtxcJR-qPFaXgc
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBar.lambda$updateTheme$35(this.f$0, z);
                }
            });
        }
        int i2 = this.mColorExtractor.getColors(2, true).supportsDarkText() ? 2131886646 : 2131886642;
        if (this.mContext.getThemeResId() != i2) {
            this.mContext.setTheme(i2);
            if (z2) {
                onThemeChanged();
            }
        }
        if (z2) {
            if (this.mState != 1 && this.mState != 2) {
                i = 1;
            }
            boolean zSupportsDarkText = this.mColorExtractor.getColors(i, true).supportsDarkText();
            this.mStackScroller.updateDecorViews(zSupportsDarkText);
            this.mStatusBarWindowManager.setKeyguardDark(zSupportsDarkText);
        }
    }

    public static /* synthetic */ void lambda$updateTheme$35(StatusBar statusBar, boolean z) {
        try {
            statusBar.mOverlayManager.setEnabled("com.android.systemui.theme.dark", z, statusBar.mLockscreenUserManager.getCurrentUserId());
        } catch (RemoteException e) {
            Log.w("StatusBar", "Can't change theme", e);
        }
    }

    private void updateDozingState() {
        Trace.traceCounter(4096L, "dozing", this.mDozing ? 1 : 0);
        Trace.beginSection("StatusBar#updateDozingState");
        boolean z = (!this.mDozing && this.mDozeServiceHost.shouldAnimateWakeup()) || (this.mDozing && this.mDozeServiceHost.shouldAnimateScreenOff() && this.mStatusBarKeyguardViewManager.isGoingToSleepVisibleNotOccluded());
        this.mStackScroller.setDark(this.mDozing, z, this.mWakeUpTouchLocation);
        this.mDozeScrimController.setDozing(this.mDozing);
        this.mKeyguardIndicationController.setDozing(this.mDozing);
        this.mNotificationPanel.setDozing(this.mDozing, z);
        updateQsExpansionEnabled();
        Trace.endSection();
    }

    public void updateStackScrollerState(boolean z, boolean z2) {
        if (this.mStackScroller == null) {
            return;
        }
        boolean z3 = this.mState == 1;
        boolean zIsAnyProfilePublicMode = this.mLockscreenUserManager.isAnyProfilePublicMode();
        if (this.mHeadsUpAppearanceController != null) {
            this.mHeadsUpAppearanceController.setPublicMode(zIsAnyProfilePublicMode);
        }
        this.mStackScroller.setHideSensitive(zIsAnyProfilePublicMode, z);
        this.mStackScroller.setDimmed(z3, z2);
        this.mStackScroller.setExpandingEnabled(!z3);
        ActivatableNotificationView activatedChild = this.mStackScroller.getActivatedChild();
        this.mStackScroller.setActivatedChild(null);
        if (activatedChild != null) {
            activatedChild.makeInactive(false);
        }
    }

    public void userActivity() {
        if (this.mState == 1) {
            this.mKeyguardViewMediatorCallback.userActivity();
        }
    }

    public boolean interceptMediaKey(KeyEvent keyEvent) {
        return this.mState == 1 && this.mStatusBarKeyguardViewManager.interceptMediaKey(keyEvent);
    }

    protected boolean shouldUnlockOnMenuPressed() {
        return this.mDeviceInteractive && this.mState != 0 && this.mStatusBarKeyguardViewManager.shouldDismissOnMenuPressed();
    }

    public boolean onMenuPressed() {
        if (shouldUnlockOnMenuPressed()) {
            animateCollapsePanels(2, true);
            return true;
        }
        return false;
    }

    public void endAffordanceLaunch() {
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
    }

    public boolean onBackPressed() {
        boolean z = this.mScrimController.getState() == ScrimState.BOUNCER_SCRIMMED;
        if (this.mStatusBarKeyguardViewManager.onBackPressed(z)) {
            if (!z) {
                this.mNotificationPanel.expandWithoutQs();
            }
            return true;
        }
        if (this.mNotificationPanel.isQsExpanded()) {
            if (this.mNotificationPanel.isQsDetailShowing()) {
                this.mNotificationPanel.closeQsDetail();
            } else {
                this.mNotificationPanel.animateCloseQs();
            }
            return true;
        }
        if (this.mState == 1 || this.mState == 2) {
            return this.mKeyguardUserSwitcher != null && this.mKeyguardUserSwitcher.hideIfNotSimple(true);
        }
        animateCollapsePanels();
        return true;
    }

    public boolean onSpacePressed() {
        if (this.mDeviceInteractive && this.mState != 0) {
            animateCollapsePanels(2, true);
            return true;
        }
        return false;
    }

    private void showBouncerIfKeyguard() {
        if ((this.mState == 1 || this.mState == 2) && !this.mKeyguardViewMediator.isHiding()) {
            showBouncer(true);
        }
    }

    protected void showBouncer(boolean z) {
        this.mStatusBarKeyguardViewManager.showBouncer(z);
    }

    private void instantExpandNotificationsPanel() {
        makeExpandedVisible(true);
        this.mNotificationPanel.expand(false);
        recomputeDisableFlags(false);
    }

    private void instantCollapseNotificationPanel() {
        this.mNotificationPanel.instantCollapse();
        runPostCollapseRunnables();
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView.OnActivatedListener
    public void onActivated(ActivatableNotificationView activatableNotificationView) {
        onActivated((View) activatableNotificationView);
        this.mStackScroller.setActivatedChild(activatableNotificationView);
    }

    public void onActivated(View view) {
        this.mLockscreenGestureLogger.write(192, 0, 0);
        this.mKeyguardIndicationController.showTransientIndication(R.string.notification_tap_again);
        ActivatableNotificationView activatedChild = this.mStackScroller.getActivatedChild();
        if (activatedChild != null) {
            activatedChild.makeInactive(true);
        }
    }

    public void setBarState(int i) {
        if (i != this.mState && this.mVisible && (i == 2 || (i == 0 && isGoingToNotificationShade()))) {
            clearNotificationEffects();
        }
        if (i == 1) {
            this.mRemoteInputManager.removeRemoteInputEntriesKeptUntilCollapsed();
            maybeEscalateHeadsUp();
        }
        this.mState = i;
        this.mGroupManager.setStatusBarState(i);
        this.mHeadsUpManager.setStatusBarState(i);
        this.mFalsingManager.setStatusBarState(i);
        this.mStatusBarWindowManager.setStatusBarState(i);
        this.mStackScroller.setStatusBarState(i);
        updateReportRejectedTouchVisibility();
        updateDozing();
        updateTheme();
        touchAutoDim();
        this.mNotificationShelf.setStatusBarState(i);
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView.OnActivatedListener
    public void onActivationReset(ActivatableNotificationView activatableNotificationView) {
        if (activatableNotificationView == this.mStackScroller.getActivatedChild()) {
            this.mStackScroller.setActivatedChild(null);
            onActivationReset((View) activatableNotificationView);
        }
    }

    public void onActivationReset(View view) {
        this.mKeyguardIndicationController.hideTransientIndication();
    }

    public void onTrackingStarted() {
        runPostCollapseRunnables();
    }

    public void onClosingFinished() {
        runPostCollapseRunnables();
        if (!isPresenterFullyCollapsed()) {
            this.mStatusBarWindowManager.setStatusBarFocusable(true);
        }
    }

    public void onUnlockHintStarted() {
        this.mFalsingManager.onUnlockHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R.string.keyguard_unlock);
    }

    public void onHintFinished() {
        this.mKeyguardIndicationController.hideTransientIndicationDelayed(1200L);
    }

    public void onCameraHintStarted() {
        this.mFalsingManager.onCameraHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R.string.camera_hint);
    }

    public void onVoiceAssistHintStarted() {
        this.mFalsingManager.onLeftAffordanceHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R.string.voice_hint);
    }

    public void onPhoneHintStarted() {
        this.mFalsingManager.onLeftAffordanceHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R.string.phone_hint);
    }

    public void onTrackingStopped(boolean z) {
        if ((this.mState == 1 || this.mState == 2) && !z && !this.mUnlockMethodCache.canSkipBouncer()) {
            showBouncer(false);
        }
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public int getMaxNotificationsWhileLocked(boolean z) {
        if (z) {
            this.mMaxKeyguardNotifications = Math.max(1, this.mNotificationPanel.computeMaxKeyguardNotifications(this.mMaxAllowedKeyguardNotifications));
            return this.mMaxKeyguardNotifications;
        }
        return this.mMaxKeyguardNotifications;
    }

    public NavigationBarView getNavigationBarView() {
        if (this.mNavigationBar != null) {
            return (NavigationBarView) this.mNavigationBar.getView();
        }
        return null;
    }

    @Override // com.android.systemui.statusbar.DragDownHelper.DragDownCallback
    public boolean onDraggedDown(View view, int i) {
        if (this.mState != 1 || !hasActiveNotifications() || (isDozing() && !isPulsing())) {
            return false;
        }
        this.mLockscreenGestureLogger.write(187, (int) (i / this.mDisplayMetrics.density), 0);
        goToLockedShade(view);
        if (view instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) view).onExpandedByGesture(true);
        }
        return true;
    }

    @Override // com.android.systemui.statusbar.DragDownHelper.DragDownCallback
    public void onDragDownReset() {
        this.mStackScroller.setDimmed(true, true);
        this.mStackScroller.resetScrollPosition();
        this.mStackScroller.resetCheckSnoozeLeavebehind();
    }

    @Override // com.android.systemui.statusbar.DragDownHelper.DragDownCallback
    public void onCrossedThreshold(boolean z) {
        this.mStackScroller.setDimmed(!z, true);
    }

    @Override // com.android.systemui.statusbar.DragDownHelper.DragDownCallback
    public void onTouchSlopExceeded() {
        this.mStackScroller.cancelLongPress();
        this.mStackScroller.checkSnoozeLeavebehind();
    }

    @Override // com.android.systemui.statusbar.DragDownHelper.DragDownCallback
    public void setEmptyDragAmount(float f) {
        this.mNotificationPanel.setEmptyDragAmount(f);
    }

    @Override // com.android.systemui.statusbar.DragDownHelper.DragDownCallback
    public boolean isFalsingCheckNeeded() {
        return this.mState == 1;
    }

    public void goToLockedShade(View view) {
        ExpandableNotificationRow expandableNotificationRow;
        if ((this.mDisabled2 & 4) != 0) {
            return;
        }
        int currentUserId = this.mLockscreenUserManager.getCurrentUserId();
        if (view instanceof ExpandableNotificationRow) {
            expandableNotificationRow = (ExpandableNotificationRow) view;
            expandableNotificationRow.setUserExpanded(true, true);
            expandableNotificationRow.setGroupExpansionChanging(true);
            if (expandableNotificationRow.getStatusBarNotification() != null) {
                currentUserId = expandableNotificationRow.getStatusBarNotification().getUserId();
            }
        } else {
            expandableNotificationRow = null;
        }
        boolean z = (this.mLockscreenUserManager.userAllowsPrivateNotificationsInPublic(this.mLockscreenUserManager.getCurrentUserId()) && this.mLockscreenUserManager.shouldShowLockscreenNotifications() && !this.mFalsingManager.shouldEnforceBouncer()) ? false : true;
        if (this.mLockscreenUserManager.isLockscreenPublicMode(currentUserId) && z) {
            this.mLeaveOpenOnKeyguardHide = true;
            showBouncerIfKeyguard();
            this.mDraggedDownRow = expandableNotificationRow;
            this.mPendingRemoteInputView = null;
            return;
        }
        this.mNotificationPanel.animateToFullShade(0L);
        setBarState(2);
        updateKeyguardState(false, false);
    }

    public void onLockedNotificationImportanceChange(KeyguardHostView.OnDismissAction onDismissAction) {
        this.mLeaveOpenOnKeyguardHide = true;
        dismissKeyguardThenExecute(onDismissAction, true);
    }

    @Override // com.android.systemui.statusbar.NotificationRemoteInputManager.Callback
    public void onLockedRemoteInput(ExpandableNotificationRow expandableNotificationRow, View view) {
        this.mLeaveOpenOnKeyguardHide = true;
        showBouncer(true);
        this.mPendingRemoteInputView = view;
    }

    @Override // com.android.systemui.statusbar.NotificationRemoteInputManager.Callback
    public void onMakeExpandedVisibleForRemoteInput(ExpandableNotificationRow expandableNotificationRow, final View view) {
        if (isKeyguardShowing()) {
            onLockedRemoteInput(expandableNotificationRow, view);
            return;
        }
        expandableNotificationRow.setUserExpanded(true);
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        Objects.requireNonNull(view);
        privateLayout.setOnExpandedVisibleListener(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$MVkYf3B-uVxXy7rxrXvHR4SUXEU
            @Override // java.lang.Runnable
            public final void run() {
                view.performClick();
            }
        });
    }

    @Override // com.android.systemui.statusbar.NotificationRemoteInputManager.Callback
    public boolean shouldHandleRemoteInput(View view, PendingIntent pendingIntent) {
        return (this.mDisabled2 & 4) != 0;
    }

    @Override // com.android.systemui.statusbar.NotificationRemoteInputManager.Callback
    public boolean handleRemoteViewClick(View view, PendingIntent pendingIntent, Intent intent, final NotificationRemoteInputManager.ClickHandler clickHandler) {
        if (pendingIntent.isActivity()) {
            dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$fzTDbkcnYrHu8s7M4OkG8bm4evI
                @Override // com.android.keyguard.KeyguardHostView.OnDismissAction
                public final boolean onDismiss() {
                    return StatusBar.lambda$handleRemoteViewClick$36(this.f$0, clickHandler);
                }
            }, PreviewInflater.wouldLaunchResolverActivity(this.mContext, pendingIntent.getIntent(), this.mLockscreenUserManager.getCurrentUserId()));
            return true;
        }
        return clickHandler.handleClick();
    }

    public static /* synthetic */ boolean lambda$handleRemoteViewClick$36(StatusBar statusBar, NotificationRemoteInputManager.ClickHandler clickHandler) {
        try {
            ActivityManager.getService().resumeAppSwitches();
        } catch (RemoteException e) {
        }
        if (!clickHandler.handleClick() || statusBar.mNotificationPanel.isFullyCollapsed()) {
            return false;
        }
        statusBar.animateCollapsePanels(2, true);
        statusBar.visibilityChanged(false);
        statusBar.mAssistManager.hideAssist();
        return true;
    }

    protected boolean startWorkChallengeIfNecessary(int i, IntentSender intentSender, String str) {
        this.mPendingWorkRemoteInputView = null;
        Intent intentCreateConfirmDeviceCredentialIntent = this.mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, i);
        if (intentCreateConfirmDeviceCredentialIntent == null) {
            return false;
        }
        Intent intent = new Intent("com.android.systemui.statusbar.work_challenge_unlocked_notification_action");
        intent.putExtra("android.intent.extra.INTENT", intentSender);
        intent.putExtra("android.intent.extra.INDEX", str);
        intent.setPackage(this.mContext.getPackageName());
        intentCreateConfirmDeviceCredentialIntent.putExtra("android.intent.extra.INTENT", PendingIntent.getBroadcast(this.mContext, 0, intent, 1409286144).getIntentSender());
        try {
            ActivityManager.getService().startConfirmDeviceCredentialIntent(intentCreateConfirmDeviceCredentialIntent, (Bundle) null);
            return true;
        } catch (RemoteException e) {
            return true;
        }
    }

    @Override // com.android.systemui.statusbar.NotificationRemoteInputManager.Callback
    public void onLockedWorkRemoteInput(int i, ExpandableNotificationRow expandableNotificationRow, View view) {
        animateCollapsePanels();
        startWorkChallengeIfNecessary(i, null, null);
        this.mPendingWorkRemoteInputView = view;
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public void onWorkChallengeChanged() {
        updatePublicMode();
        this.mEntryManager.updateNotifications();
        if (this.mPendingWorkRemoteInputView != null && !this.mLockscreenUserManager.isAnyProfilePublicMode()) {
            this.mNotificationPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.systemui.statusbar.phone.StatusBar.13
                final /* synthetic */ Runnable val$clickPendingViewRunnable;

                AnonymousClass13(Runnable runnable) {
                    runnable = runnable;
                }

                @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
                public void onGlobalLayout() {
                    if (StatusBar.this.mNotificationPanel.mStatusBar.getStatusBarWindow().getHeight() != StatusBar.this.mNotificationPanel.mStatusBar.getStatusBarHeight()) {
                        StatusBar.this.mNotificationPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        StatusBar.this.mNotificationPanel.post(runnable);
                    }
                }
            });
            instantExpandNotificationsPanel();
        }
    }

    public static /* synthetic */ void lambda$onWorkChallengeChanged$39(final StatusBar statusBar) {
        View view = statusBar.mPendingWorkRemoteInputView;
        if (view == null) {
            return;
        }
        ViewParent parent = view.getParent();
        while (!(parent instanceof ExpandableNotificationRow)) {
            if (parent == null) {
                return;
            } else {
                parent = parent.getParent();
            }
        }
        final ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) parent;
        ViewParent parent2 = expandableNotificationRow.getParent();
        if (parent2 instanceof NotificationStackScrollLayout) {
            final NotificationStackScrollLayout notificationStackScrollLayout = (NotificationStackScrollLayout) parent2;
            expandableNotificationRow.makeActionsVisibile();
            expandableNotificationRow.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$eoWDQ0ohibR1pIAC4B1Lhj__ei0
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBar.lambda$onWorkChallengeChanged$38(this.f$0, notificationStackScrollLayout, expandableNotificationRow);
                }
            });
        }
    }

    public static /* synthetic */ void lambda$onWorkChallengeChanged$38(final StatusBar statusBar, final NotificationStackScrollLayout notificationStackScrollLayout, ExpandableNotificationRow expandableNotificationRow) {
        Runnable runnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$j96cSNpAdKazwZ2nGsaARsdXzOk
            @Override // java.lang.Runnable
            public final void run() {
                StatusBar.lambda$onWorkChallengeChanged$37(this.f$0, notificationStackScrollLayout);
            }
        };
        if (notificationStackScrollLayout.scrollTo(expandableNotificationRow)) {
            notificationStackScrollLayout.setFinishScrollingCallback(runnable);
        } else {
            runnable.run();
        }
    }

    public static /* synthetic */ void lambda$onWorkChallengeChanged$37(StatusBar statusBar, NotificationStackScrollLayout notificationStackScrollLayout) {
        statusBar.mPendingWorkRemoteInputView.callOnClick();
        statusBar.mPendingWorkRemoteInputView = null;
        notificationStackScrollLayout.setFinishScrollingCallback(null);
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$13 */
    class AnonymousClass13 implements ViewTreeObserver.OnGlobalLayoutListener {
        final /* synthetic */ Runnable val$clickPendingViewRunnable;

        AnonymousClass13(Runnable runnable) {
            runnable = runnable;
        }

        @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
        public void onGlobalLayout() {
            if (StatusBar.this.mNotificationPanel.mStatusBar.getStatusBarWindow().getHeight() != StatusBar.this.mNotificationPanel.mStatusBar.getStatusBarHeight()) {
                StatusBar.this.mNotificationPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                StatusBar.this.mNotificationPanel.post(runnable);
            }
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableNotificationRow.OnExpandClickListener
    public void onExpandClicked(NotificationData.Entry entry, boolean z) {
        this.mHeadsUpManager.setExpanded(entry, z);
        if (this.mState == 1 && z) {
            goToLockedShade(entry.row);
        }
    }

    public void goToKeyguard() {
        if (this.mState == 2) {
            this.mStackScroller.onGoToKeyguard();
            setBarState(1);
            updateKeyguardState(false, true);
        }
    }

    public long getKeyguardFadingAwayDelay() {
        return this.mKeyguardFadingAwayDelay;
    }

    public long getKeyguardFadingAwayDuration() {
        return this.mKeyguardFadingAwayDuration;
    }

    public void setBouncerShowing(boolean z) {
        this.mBouncerShowing = z;
        if (this.mStatusBarView != null) {
            this.mStatusBarView.setBouncerShowing(z);
        }
        updateHideIconsForBouncer(true);
        recomputeDisableFlags(true);
        updateScrimController();
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$14 */
    class AnonymousClass14 implements WakefulnessLifecycle.Observer {
        AnonymousClass14() {
        }

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onFinishedGoingToSleep() {
            StatusBar.this.mNotificationPanel.onAffordanceLaunchEnded();
            StatusBar.this.releaseGestureWakeLock();
            StatusBar.this.mLaunchCameraOnScreenTurningOn = false;
            StatusBar.this.mDeviceInteractive = false;
            StatusBar.this.mWakeUpComingFromTouch = false;
            StatusBar.this.mWakeUpTouchLocation = null;
            StatusBar.this.mStackScroller.setAnimationsEnabled(false);
            StatusBar.this.mVisualStabilityManager.setScreenOn(false);
            StatusBar.this.updateVisibleToUser();
            StatusBar.this.mNotificationPanel.setTouchDisabled(true);
            StatusBar.this.mStatusBarWindow.cancelCurrentTouch();
            if (StatusBar.this.mLaunchCameraOnFinishedGoingToSleep) {
                StatusBar.this.mLaunchCameraOnFinishedGoingToSleep = false;
                StatusBar.this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$14$nWBD2D2g4XfqSRRa62h1yqCV30I
                    @Override // java.lang.Runnable
                    public final void run() {
                        StatusBar.AnonymousClass14 anonymousClass14 = this.f$0;
                        StatusBar.this.onCameraLaunchGestureDetected(StatusBar.this.mLastCameraLaunchSource);
                    }
                });
            }
            StatusBar.this.updateIsKeyguard();
        }

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onStartedGoingToSleep() {
            StatusBar.this.notifyHeadsUpGoingToSleep();
            StatusBar.this.dismissVolumeDialog();
        }

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onStartedWakingUp() {
            StatusBar.this.mDeviceInteractive = true;
            StatusBar.this.mStackScroller.setAnimationsEnabled(true);
            StatusBar.this.mVisualStabilityManager.setScreenOn(true);
            StatusBar.this.mNotificationPanel.setTouchDisabled(false);
            StatusBar.this.mDozeServiceHost.stopDozing();
            StatusBar.this.updateVisibleToUser();
            StatusBar.this.updateIsKeyguard();
            StatusBar.this.updateScrimController();
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$15 */
    class AnonymousClass15 implements ScreenLifecycle.Observer {
        AnonymousClass15() {
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurningOn() {
            StatusBar.this.mFalsingManager.onScreenTurningOn();
            StatusBar.this.mNotificationPanel.onScreenTurningOn();
            if (StatusBar.this.mLaunchCameraOnScreenTurningOn) {
                StatusBar.this.mNotificationPanel.launchCamera(false, StatusBar.this.mLastCameraLaunchSource);
                StatusBar.this.mLaunchCameraOnScreenTurningOn = false;
            }
            StatusBar.this.updateScrimController();
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOn() {
            StatusBar.this.mScrimController.onScreenTurnedOn();
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOff() {
            StatusBar.this.mFalsingManager.onScreenOff();
            StatusBar.this.mScrimController.onScreenTurnedOff();
            if (!StatusBar.this.isPulsing()) {
                StatusBar.this.updateIsKeyguard();
            }
        }
    }

    public int getWakefulnessState() {
        return this.mWakefulnessLifecycle.getWakefulness();
    }

    private void vibrateForCameraGesture() {
        this.mVibrator.vibrate(this.mCameraLaunchGestureVibePattern, -1);
    }

    public boolean isScreenFullyOff() {
        return this.mScreenLifecycle.getScreenState() == 0;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showScreenPinningRequest(int i) {
        if (this.mKeyguardMonitor.isShowing()) {
            return;
        }
        showScreenPinningRequest(i, true);
    }

    public void showScreenPinningRequest(int i, boolean z) {
        this.mScreenPinningRequest.showPrompt(i, z);
    }

    public boolean hasActiveNotifications() {
        return !this.mEntryManager.getNotificationData().getActiveNotifications().isEmpty();
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public void wakeUpIfDozing(long j, View view) {
        if (this.mDozing) {
            ((PowerManager) this.mContext.getSystemService("power")).wakeUp(j, "com.android.systemui:NODOZE");
            this.mWakeUpComingFromTouch = true;
            view.getLocationInWindow(this.mTmpInt2);
            this.mWakeUpTouchLocation = new PointF(this.mTmpInt2[0] + (view.getWidth() / 2), this.mTmpInt2[1] + (view.getHeight() / 2));
            this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
            this.mFalsingManager.onScreenOnFromTouch();
        }
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public boolean isDeviceLocked(int i) {
        return this.mKeyguardManager.isDeviceLocked(i);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void appTransitionCancelled() {
        EventBus.getDefault().send(new AppTransitionFinishedEvent());
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void appTransitionFinished() {
        EventBus.getDefault().send(new AppTransitionFinishedEvent());
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onCameraLaunchGestureDetected(int i) {
        boolean z;
        this.mLastCameraLaunchSource = i;
        if (isGoingToSleep()) {
            this.mLaunchCameraOnFinishedGoingToSleep = true;
            return;
        }
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if (this.mStatusBarKeyguardViewManager.isShowing() && this.mExpandedVisible) {
            z = true;
        } else {
            z = false;
        }
        if (!notificationPanelView.canCameraGestureBeLaunched(z)) {
            return;
        }
        if (!this.mDeviceInteractive) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE");
            this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
        }
        vibrateForCameraGesture();
        if (!this.mStatusBarKeyguardViewManager.isShowing()) {
            startActivityDismissingKeyguard(KeyguardBottomAreaView.INSECURE_CAMERA_INTENT, false, true, true, null, 0);
            return;
        }
        if (!this.mDeviceInteractive) {
            this.mGestureWakeLock.acquire(6000L);
        }
        if (isScreenTurningOnOrOn()) {
            if (this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                this.mStatusBarKeyguardViewManager.reset(true);
            }
            if (!this.mStatusBarKeyguardViewManager.isSecure() && this.mStatusBarKeyguardViewManager.isOccluded()) {
                Slog.d("StatusBar", "Non-Secure unlock, no need to launch camera");
                return;
            } else {
                this.mNotificationPanel.launchCamera(this.mDeviceInteractive, i);
                updateScrimController();
                return;
            }
        }
        this.mLaunchCameraOnScreenTurningOn = true;
    }

    boolean isCameraAllowedByAdmin() {
        if (this.mDevicePolicyManager.getCameraDisabled(null, this.mLockscreenUserManager.getCurrentUserId())) {
            return false;
        }
        return !(this.mStatusBarKeyguardViewManager == null || (isKeyguardShowing() && isKeyguardSecure())) || (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mLockscreenUserManager.getCurrentUserId()) & 2) == 0;
    }

    private boolean isGoingToSleep() {
        return this.mWakefulnessLifecycle.getWakefulness() == 3;
    }

    private boolean isScreenTurningOnOrOn() {
        return this.mScreenLifecycle.getScreenState() == 1 || this.mScreenLifecycle.getScreenState() == 2;
    }

    public void notifyFpAuthModeChanged() {
        updateDozing();
        updateScrimController();
    }

    private void updateDozing() {
        Trace.beginSection("StatusBar#updateDozing");
        boolean z = false;
        boolean z2 = (this.mDozingRequested && this.mState == 1) || this.mFingerprintUnlockController.getMode() == 2;
        boolean alwaysOn = DozeParameters.getInstance(this.mContext).getAlwaysOn();
        if (this.mFingerprintUnlockController.getMode() == 1) {
            z2 = false;
        }
        if (this.mDozing != z2) {
            this.mDozing = z2;
            KeyguardViewMediator keyguardViewMediator = this.mKeyguardViewMediator;
            if (this.mDozing && alwaysOn) {
                z = true;
            }
            keyguardViewMediator.setAodShowing(z);
            this.mStatusBarWindowManager.setDozing(this.mDozing);
            this.mStatusBarKeyguardViewManager.setDozing(this.mDozing);
            if (this.mAmbientIndicationContainer instanceof DozeReceiver) {
                ((DozeReceiver) this.mAmbientIndicationContainer).setDozing(this.mDozing);
            }
            this.mEntryManager.updateNotifications();
            updateDozingState();
            updateReportRejectedTouchVisibility();
        }
        Trace.endSection();
    }

    @VisibleForTesting
    void updateScrimController() {
        Trace.beginSection("StatusBar#updateScrimController");
        boolean zIsWakeAndUnlock = this.mFingerprintUnlockController.isWakeAndUnlock();
        this.mScrimController.setExpansionAffectsAlpha(!this.mFingerprintUnlockController.isFingerprintUnlock());
        if (this.mBouncerShowing) {
            this.mScrimController.transitionTo((this.mIsOccluded || this.mStatusBarKeyguardViewManager.bouncerNeedsScrimming() || this.mStatusBarKeyguardViewManager.willDismissWithAction() || this.mStatusBarKeyguardViewManager.isFullscreenBouncer()) ? ScrimState.BOUNCER_SCRIMMED : ScrimState.BOUNCER);
        } else if (this.mLaunchCameraOnScreenTurningOn || isInLaunchTransition()) {
            this.mScrimController.transitionTo(ScrimState.UNLOCKED, this.mUnlockScrimCallback);
        } else if (this.mBrightnessMirrorVisible) {
            this.mScrimController.transitionTo(ScrimState.BRIGHTNESS_MIRROR);
        } else if (!isPulsing()) {
            if (this.mDozing) {
                this.mScrimController.transitionTo(ScrimState.AOD);
            } else if (this.mIsKeyguard && !zIsWakeAndUnlock) {
                this.mScrimController.transitionTo(ScrimState.KEYGUARD);
            } else {
                this.mScrimController.transitionTo(ScrimState.UNLOCKED, this.mUnlockScrimCallback);
            }
        }
        Trace.endSection();
    }

    public boolean isKeyguardShowing() {
        if (this.mStatusBarKeyguardViewManager == null) {
            Slog.i("StatusBar", "isKeyguardShowing() called before startKeyguard(), returning true");
            return true;
        }
        return this.mStatusBarKeyguardViewManager.isShowing();
    }

    private final class DozeServiceHost implements DozeHost {
        private boolean mAnimateScreenOff;
        private boolean mAnimateWakeup;
        private final ArrayList<DozeHost.Callback> mCallbacks;
        private boolean mIgnoreTouchWhilePulsing;

        private DozeServiceHost() {
            this.mCallbacks = new ArrayList<>();
        }

        /* synthetic */ DozeServiceHost(StatusBar statusBar, AnonymousClass1 anonymousClass1) {
            this();
        }

        public String toString() {
            return "PSB.DozeServiceHost[mCallbacks=" + this.mCallbacks.size() + "]";
        }

        public void firePowerSaveChanged(boolean z) {
            Iterator<DozeHost.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onPowerSaveChanged(z);
            }
        }

        public void fireNotificationHeadsUp() {
            Iterator<DozeHost.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onNotificationHeadsUp();
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void addCallback(DozeHost.Callback callback) {
            this.mCallbacks.add(callback);
        }

        @Override // com.android.systemui.doze.DozeHost
        public void removeCallback(DozeHost.Callback callback) {
            this.mCallbacks.remove(callback);
        }

        @Override // com.android.systemui.doze.DozeHost
        public void startDozing() {
            if (!StatusBar.this.mDozingRequested) {
                StatusBar.this.mDozingRequested = true;
                DozeLog.traceDozing(StatusBar.this.mContext, StatusBar.this.mDozing);
                StatusBar.this.updateDozing();
                StatusBar.this.updateIsKeyguard();
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void pulseWhileDozing(DozeHost.PulseCallback pulseCallback, int i) {
            if (i == 5) {
                StatusBar.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:NODOZE");
                StatusBar.this.startAssist(new Bundle());
            } else {
                StatusBar.this.mDozeScrimController.pulse(new DozeHost.PulseCallback() { // from class: com.android.systemui.statusbar.phone.StatusBar.DozeServiceHost.1
                    final /* synthetic */ DozeHost.PulseCallback val$callback;

                    AnonymousClass1(DozeHost.PulseCallback pulseCallback2) {
                        pulseCallback = pulseCallback2;
                    }

                    @Override // com.android.systemui.doze.DozeHost.PulseCallback
                    public void onPulseStarted() {
                        pulseCallback.onPulseStarted();
                        if (StatusBar.this.mHeadsUpManager.hasHeadsUpNotifications()) {
                            setPulsing(true);
                        }
                    }

                    @Override // com.android.systemui.doze.DozeHost.PulseCallback
                    public void onPulseFinished() {
                        pulseCallback.onPulseFinished();
                        setPulsing(false);
                    }

                    private void setPulsing(boolean z) {
                        StatusBar.this.mNotificationPanel.setPulsing(z);
                        StatusBar.this.mVisualStabilityManager.setPulsing(z);
                        DozeServiceHost.this.mIgnoreTouchWhilePulsing = false;
                    }
                }, i);
            }
        }

        /* renamed from: com.android.systemui.statusbar.phone.StatusBar$DozeServiceHost$1 */
        class AnonymousClass1 implements DozeHost.PulseCallback {
            final /* synthetic */ DozeHost.PulseCallback val$callback;

            AnonymousClass1(DozeHost.PulseCallback pulseCallback2) {
                pulseCallback = pulseCallback2;
            }

            @Override // com.android.systemui.doze.DozeHost.PulseCallback
            public void onPulseStarted() {
                pulseCallback.onPulseStarted();
                if (StatusBar.this.mHeadsUpManager.hasHeadsUpNotifications()) {
                    setPulsing(true);
                }
            }

            @Override // com.android.systemui.doze.DozeHost.PulseCallback
            public void onPulseFinished() {
                pulseCallback.onPulseFinished();
                setPulsing(false);
            }

            private void setPulsing(boolean z) {
                StatusBar.this.mNotificationPanel.setPulsing(z);
                StatusBar.this.mVisualStabilityManager.setPulsing(z);
                DozeServiceHost.this.mIgnoreTouchWhilePulsing = false;
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void stopDozing() {
            if (StatusBar.this.mDozingRequested) {
                StatusBar.this.mDozingRequested = false;
                DozeLog.traceDozing(StatusBar.this.mContext, StatusBar.this.mDozing);
                StatusBar.this.mWakefulnessLifecycle.dispatchStartedWakingUp();
                StatusBar.this.updateDozing();
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void onIgnoreTouchWhilePulsing(boolean z) {
            if (z != this.mIgnoreTouchWhilePulsing) {
                DozeLog.tracePulseTouchDisabledByProx(StatusBar.this.mContext, z);
            }
            this.mIgnoreTouchWhilePulsing = z;
            if (StatusBar.this.isDozing() && z) {
                StatusBar.this.mStatusBarWindow.cancelCurrentTouch();
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void dozeTimeTick() {
            StatusBar.this.mNotificationPanel.dozeTimeTick();
        }

        @Override // com.android.systemui.doze.DozeHost
        public boolean isPowerSaveActive() {
            return StatusBar.this.mBatteryController.isAodPowerSave();
        }

        @Override // com.android.systemui.doze.DozeHost
        public boolean isPulsingBlocked() {
            return StatusBar.this.mFingerprintUnlockController.getMode() == 1;
        }

        @Override // com.android.systemui.doze.DozeHost
        public boolean isProvisioned() {
            return StatusBar.this.mDeviceProvisionedController.isDeviceProvisioned() && StatusBar.this.mDeviceProvisionedController.isCurrentUserSetup();
        }

        @Override // com.android.systemui.doze.DozeHost
        public boolean isBlockingDoze() {
            if (StatusBar.this.mFingerprintUnlockController.hasPendingAuthentication()) {
                Log.i("StatusBar", "Blocking AOD because fingerprint has authenticated");
                return true;
            }
            return false;
        }

        @Override // com.android.systemui.doze.DozeHost
        public void extendPulse() {
            StatusBar.this.mDozeScrimController.extendPulse();
        }

        @Override // com.android.systemui.doze.DozeHost
        public void setAnimateWakeup(boolean z) {
            if (StatusBar.this.mWakefulnessLifecycle.getWakefulness() == 2 || StatusBar.this.mWakefulnessLifecycle.getWakefulness() == 1) {
                return;
            }
            this.mAnimateWakeup = z;
        }

        @Override // com.android.systemui.doze.DozeHost
        public void setAnimateScreenOff(boolean z) {
            this.mAnimateScreenOff = z;
        }

        @Override // com.android.systemui.doze.DozeHost
        public void onDoubleTap(float f, float f2) {
            if (f > 0.0f && f2 > 0.0f && StatusBar.this.mAmbientIndicationContainer != null && StatusBar.this.mAmbientIndicationContainer.getVisibility() == 0) {
                StatusBar.this.mAmbientIndicationContainer.getLocationOnScreen(StatusBar.this.mTmpInt2);
                float f3 = f - StatusBar.this.mTmpInt2[0];
                float f4 = f2 - StatusBar.this.mTmpInt2[1];
                if (0.0f <= f3 && f3 <= StatusBar.this.mAmbientIndicationContainer.getWidth() && 0.0f <= f4 && f4 <= StatusBar.this.mAmbientIndicationContainer.getHeight()) {
                    dispatchDoubleTap(f3, f4);
                }
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void setDozeScreenBrightness(int i) {
            StatusBar.this.mStatusBarWindowManager.setDozeScreenBrightness(i);
        }

        @Override // com.android.systemui.doze.DozeHost
        public void setAodDimmingScrim(float f) {
            StatusBar.this.mScrimController.setAodFrontScrimAlpha(f);
        }

        public void dispatchDoubleTap(float f, float f2) {
            dispatchTap(StatusBar.this.mAmbientIndicationContainer, f, f2);
            dispatchTap(StatusBar.this.mAmbientIndicationContainer, f, f2);
        }

        private void dispatchTap(View view, float f, float f2) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            dispatchTouchEvent(view, f, f2, jElapsedRealtime, 0);
            dispatchTouchEvent(view, f, f2, jElapsedRealtime, 1);
        }

        private void dispatchTouchEvent(View view, float f, float f2, long j, int i) {
            MotionEvent motionEventObtain = MotionEvent.obtain(j, j, i, f, f2, 0);
            view.dispatchTouchEvent(motionEventObtain);
            motionEventObtain.recycle();
        }

        private boolean shouldAnimateWakeup() {
            return this.mAnimateWakeup;
        }

        public boolean shouldAnimateScreenOff() {
            return this.mAnimateScreenOff;
        }
    }

    public boolean shouldIgnoreTouch() {
        return isDozing() && this.mDozeServiceHost.mIgnoreTouchWhilePulsing;
    }

    public boolean isDeviceInteractive() {
        return this.mDeviceInteractive;
    }

    @Override // com.android.systemui.statusbar.NotificationData.Environment
    public boolean isDeviceProvisioned() {
        return this.mDeviceProvisionedController.isDeviceProvisioned();
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$16 */
    class AnonymousClass16 extends IVrStateCallbacks.Stub {
        AnonymousClass16() {
        }

        public void onVrStateChanged(boolean z) {
            StatusBar.this.mVrMode = z;
        }
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public boolean isDeviceInVrMode() {
        return this.mVrMode;
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$17 */
    class AnonymousClass17 extends BroadcastReceiver {
        AnonymousClass17() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.systemui.statusbar.banner_action_cancel".equals(action) || "com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                ((NotificationManager) StatusBar.this.mContext.getSystemService("notification")).cancel(5);
                Settings.Secure.putInt(StatusBar.this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                if ("com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                    StatusBar.this.animateCollapsePanels(2, true);
                    if (BenesseExtension.getDchaState() != 0) {
                        return;
                    }
                    StatusBar.this.mContext.startActivity(new Intent("android.settings.ACTION_APP_NOTIFICATION_REDACTION").addFlags(268435456));
                }
            }
        }
    }

    @Override // com.android.systemui.statusbar.NotificationEntryManager.Callback
    public void onNotificationClicked(final StatusBarNotification statusBarNotification, final ExpandableNotificationRow expandableNotificationRow) {
        PendingIntent pendingIntent;
        final RemoteInputController controller = this.mRemoteInputManager.getController();
        if (controller.isRemoteInputActive(expandableNotificationRow.getEntry()) && !TextUtils.isEmpty(expandableNotificationRow.getActiveRemoteInputText())) {
            controller.closeRemoteInputs();
            return;
        }
        Notification notification = statusBarNotification.getNotification();
        if (notification.contentIntent != null) {
            pendingIntent = notification.contentIntent;
        } else {
            pendingIntent = notification.fullScreenIntent;
        }
        final PendingIntent pendingIntent2 = pendingIntent;
        final String key = statusBarNotification.getKey();
        boolean z = pendingIntent2.isActivity() && PreviewInflater.wouldLaunchResolverActivity(this.mContext, pendingIntent2.getIntent(), this.mLockscreenUserManager.getCurrentUserId());
        final boolean z2 = this.mIsOccluded;
        dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$cpS9GH1Yu7E8qLhdYAL6i-NlD18
            @Override // com.android.keyguard.KeyguardHostView.OnDismissAction
            public final boolean onDismiss() {
                return StatusBar.lambda$onNotificationClicked$41(this.f$0, key, expandableNotificationRow, statusBarNotification, pendingIntent2, controller, z2);
            }
        }, z);
    }

    /* JADX WARN: Removed duplicated region for block: B:47:0x0045  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static /* synthetic */ boolean lambda$onNotificationClicked$41(final StatusBar statusBar, final String str, ExpandableNotificationRow expandableNotificationRow, final StatusBarNotification statusBarNotification, final PendingIntent pendingIntent, final RemoteInputController remoteInputController, final boolean z) {
        final ExpandableNotificationRow expandableNotificationRow2;
        final StatusBarNotification statusBarNotification2;
        if (statusBar.mHeadsUpManager != null && statusBar.mHeadsUpManager.isHeadsUp(str)) {
            if (statusBar.isPresenterFullyCollapsed()) {
                expandableNotificationRow2 = expandableNotificationRow;
                HeadsUpUtil.setIsClickedHeadsUpNotification(expandableNotificationRow2, true);
            } else {
                expandableNotificationRow2 = expandableNotificationRow;
            }
            statusBar.mHeadsUpManager.releaseImmediately(str);
        } else {
            expandableNotificationRow2 = expandableNotificationRow;
        }
        if (statusBar.shouldAutoCancel(statusBarNotification) && statusBar.mGroupManager.isOnlyChildInGroup(statusBarNotification)) {
            StatusBarNotification statusBarNotification3 = statusBar.mGroupManager.getLogicalGroupSummary(statusBarNotification).getStatusBarNotification();
            if (statusBar.shouldAutoCancel(statusBarNotification3)) {
                statusBarNotification2 = statusBarNotification3;
            }
        } else {
            statusBarNotification2 = null;
        }
        Runnable runnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$j_VWyEw2Y2X4khHBBJnisO4DaR8
            @Override // java.lang.Runnable
            public final void run() {
                StatusBar.lambda$onNotificationClicked$40(this.f$0, pendingIntent, str, expandableNotificationRow2, remoteInputController, z, statusBarNotification2, statusBarNotification);
            }
        };
        if (statusBar.mStatusBarKeyguardViewManager.isShowing() && statusBar.mStatusBarKeyguardViewManager.isOccluded()) {
            statusBar.mStatusBarKeyguardViewManager.addAfterKeyguardGoneRunnable(runnable);
            statusBar.collapsePanel(true);
        } else {
            new Thread(runnable).start();
        }
        return !statusBar.mNotificationPanel.isFullyCollapsed();
    }

    public static /* synthetic */ void lambda$onNotificationClicked$40(StatusBar statusBar, PendingIntent pendingIntent, String str, ExpandableNotificationRow expandableNotificationRow, RemoteInputController remoteInputController, boolean z, StatusBarNotification statusBarNotification, StatusBarNotification statusBarNotification2) {
        CharSequence charSequence;
        try {
            ActivityManager.getService().resumeAppSwitches();
        } catch (RemoteException e) {
        }
        if (pendingIntent != null) {
            if (pendingIntent.isActivity()) {
                int identifier = pendingIntent.getCreatorUserHandle().getIdentifier();
                if (statusBar.mLockPatternUtils.isSeparateProfileChallengeEnabled(identifier) && statusBar.mKeyguardManager.isDeviceLocked(identifier) && statusBar.startWorkChallengeIfNecessary(identifier, pendingIntent.getIntentSender(), str)) {
                    statusBar.collapseOnMainThread();
                    return;
                }
            }
            NotificationData.Entry entry = expandableNotificationRow.getEntry();
            if (!TextUtils.isEmpty(entry.remoteInputText)) {
                charSequence = entry.remoteInputText;
            } else {
                charSequence = null;
            }
            Intent intentPutExtra = (TextUtils.isEmpty(charSequence) || remoteInputController.isSpinning(entry.key)) ? null : new Intent().putExtra("android.remoteInputDraft", charSequence.toString());
            RemoteAnimationAdapter launchAnimation = statusBar.mActivityLaunchAnimator.getLaunchAnimation(expandableNotificationRow, z);
            if (launchAnimation != null) {
                try {
                    ActivityManager.getService().registerRemoteAnimationForNextActivityStart(pendingIntent.getCreatorPackage(), launchAnimation);
                } catch (PendingIntent.CanceledException | RemoteException e2) {
                    Log.w("StatusBar", "Sending contentIntent failed: " + e2);
                }
            }
            statusBar.mActivityLaunchAnimator.setLaunchResult(pendingIntent.sendAndReturnResult(statusBar.mContext, 0, intentPutExtra, null, null, null, statusBar.getActivityOptions(launchAnimation)));
            if (pendingIntent.isActivity()) {
                statusBar.mAssistManager.hideAssist();
            }
        }
        if (statusBar.shouldCollapse()) {
            statusBar.collapseOnMainThread();
        }
        try {
            statusBar.mBarService.onNotificationClick(str, NotificationVisibility.obtain(str, statusBar.mEntryManager.getNotificationData().getRank(str), statusBar.mEntryManager.getNotificationData().getActiveNotifications().size(), true));
        } catch (RemoteException e3) {
        }
        if (statusBarNotification != null) {
            statusBar.removeNotification(statusBarNotification);
        }
        if (statusBar.shouldAutoCancel(statusBarNotification2) || statusBar.mEntryManager.isNotificationKeptForRemoteInput(str)) {
            statusBar.removeNotification(statusBarNotification2);
        }
    }

    private void collapseOnMainThread() {
        if (Looper.getMainLooper().isCurrentThread()) {
            collapsePanel();
        } else {
            this.mStackScroller.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$gz0x-DF8XfLLI3DV9TaqObWTwvs
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.collapsePanel();
                }
            });
        }
    }

    private boolean shouldCollapse() {
        return (this.mState == 0 && this.mActivityLaunchAnimator.isAnimationPending()) ? false : true;
    }

    public void collapsePanel(boolean z) {
        if (z) {
            collapsePanel();
        } else if (!isPresenterFullyCollapsed()) {
            instantCollapseNotificationPanel();
            visibilityChanged(false);
        } else {
            runPostCollapseRunnables();
        }
    }

    private boolean collapsePanel() {
        if (this.mNotificationPanel.isFullyCollapsed()) {
            return false;
        }
        animateCollapsePanels(2, true, true);
        visibilityChanged(false);
        return true;
    }

    private void removeNotification(final StatusBarNotification statusBarNotification) {
        this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$anGzdKBNO9L0XzFdGlBFy1j85B0
            @Override // java.lang.Runnable
            public final void run() {
                StatusBar.lambda$removeNotification$43(this.f$0, statusBarNotification);
            }
        });
    }

    public static /* synthetic */ void lambda$removeNotification$43(final StatusBar statusBar, final StatusBarNotification statusBarNotification) {
        Runnable runnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$gJKreMi_qTVFZxM5HzO_UrLMzKo
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.mEntryManager.performRemoveNotification(statusBarNotification);
            }
        };
        if (statusBar.isCollapsing()) {
            statusBar.addPostCollapseAction(runnable);
        } else {
            runnable.run();
        }
    }

    @Override // com.android.systemui.statusbar.NotificationData.Environment
    public boolean isNotificationForCurrentProfiles(StatusBarNotification statusBarNotification) {
        int userId = statusBarNotification.getUserId();
        boolean z = DEBUG;
        return this.mLockscreenUserManager.isCurrentProfile(userId);
    }

    @Override // com.android.systemui.statusbar.NotificationData.Environment
    public NotificationGroupManager getGroupManager() {
        return this.mGroupManager;
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public void startNotificationGutsIntent(final Intent intent, final int i, final ExpandableNotificationRow expandableNotificationRow) {
        dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$ylzGEDYi80zloswFYhcbw61Vi1I
            @Override // com.android.keyguard.KeyguardHostView.OnDismissAction
            public final boolean onDismiss() {
                return StatusBar.lambda$startNotificationGutsIntent$46(this.f$0, intent, expandableNotificationRow, i);
            }
        }, false);
    }

    public static /* synthetic */ boolean lambda$startNotificationGutsIntent$46(final StatusBar statusBar, final Intent intent, final ExpandableNotificationRow expandableNotificationRow, final int i) {
        AsyncTask.execute(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$lhINUsDojuWVQD9mItU82YC3FuY
            @Override // java.lang.Runnable
            public final void run() {
                StatusBar.lambda$startNotificationGutsIntent$45(this.f$0, intent, expandableNotificationRow, i);
            }
        });
        return true;
    }

    public static /* synthetic */ void lambda$startNotificationGutsIntent$45(final StatusBar statusBar, Intent intent, ExpandableNotificationRow expandableNotificationRow, int i) {
        statusBar.mActivityLaunchAnimator.setLaunchResult(TaskStackBuilder.create(statusBar.mContext).addNextIntentWithParentStack(intent).startActivities(statusBar.getActivityOptions(statusBar.mActivityLaunchAnimator.getLaunchAnimation(expandableNotificationRow, statusBar.mIsOccluded)), new UserHandle(UserHandle.getUserId(i))));
        if (statusBar.shouldCollapse()) {
            statusBar.mStatusBarWindow.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$_vZAkEgFD58rmNKz3HhQR-4wie0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.animateCollapsePanels(2, true);
                }
            });
        }
    }

    public void setNotificationSnoozed(StatusBarNotification statusBarNotification, NotificationSwipeActionHelper.SnoozeOption snoozeOption) {
        if (snoozeOption.getSnoozeCriterion() != null) {
            this.mNotificationListener.snoozeNotification(statusBarNotification.getKey(), snoozeOption.getSnoozeCriterion().getId());
        } else {
            this.mNotificationListener.snoozeNotification(statusBarNotification.getKey(), snoozeOption.getMinutesToSnoozeFor() * 60 * 1000);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void toggleSplitScreen() {
        toggleSplitScreenMode(-1, -1);
    }

    void awakenDreams() {
        SystemServicesProxy.getInstance(this.mContext).awakenDreamsAsync();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void preloadRecentApps() {
        this.mHandler.removeMessages(1022);
        this.mHandler.sendEmptyMessage(1022);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void cancelPreloadRecentApps() {
        this.mHandler.removeMessages(1023);
        this.mHandler.sendEmptyMessage(1023);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void dismissKeyboardShortcutsMenu() {
        this.mHandler.removeMessages(1027);
        this.mHandler.sendEmptyMessage(1027);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void toggleKeyboardShortcutsMenu(int i) {
        this.mHandler.removeMessages(1026);
        this.mHandler.obtainMessage(1026, i, 0).sendToTarget();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setTopAppHidesStatusBar(boolean z) {
        this.mTopHidesStatusBar = z;
        if (!z && this.mWereIconsJustHidden) {
            this.mWereIconsJustHidden = false;
            recomputeDisableFlags(true);
        }
        updateHideIconsForBouncer(true);
    }

    protected void toggleKeyboardShortcuts(int i) {
        KeyboardShortcuts.toggle(this.mContext, i);
    }

    protected void dismissKeyboardShortcuts() {
        KeyboardShortcuts.dismiss();
    }

    @Override // com.android.systemui.statusbar.NotificationData.Environment
    public boolean shouldHideNotifications(int i) {
        return this.mLockscreenUserManager.shouldHideNotifications(i);
    }

    @Override // com.android.systemui.statusbar.NotificationData.Environment
    public boolean shouldHideNotifications(String str) {
        return this.mLockscreenUserManager.shouldHideNotifications(str);
    }

    @Override // com.android.systemui.statusbar.NotificationData.Environment
    public boolean isSecurelyLocked(int i) {
        return this.mLockscreenUserManager.isLockscreenPublicMode(i);
    }

    public void onPanelLaidOut() {
        updateKeyguardMaxNotifications();
    }

    public void updateKeyguardMaxNotifications() {
        if (this.mState == 1 && getMaxNotificationsWhileLocked(false) != getMaxNotificationsWhileLocked(true)) {
            this.mViewHierarchyManager.updateRowStates();
        }
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startPendingIntentDismissingKeyguard(final PendingIntent pendingIntent) {
        if (isDeviceProvisioned()) {
            dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$QlDPR1nqmBKUuYlZzAqmVP6qD_E
                @Override // com.android.keyguard.KeyguardHostView.OnDismissAction
                public final boolean onDismiss() {
                    return StatusBar.lambda$startPendingIntentDismissingKeyguard$48(this.f$0, pendingIntent);
                }
            }, pendingIntent.isActivity() && PreviewInflater.wouldLaunchResolverActivity(this.mContext, pendingIntent.getIntent(), this.mLockscreenUserManager.getCurrentUserId()));
        }
    }

    public static /* synthetic */ boolean lambda$startPendingIntentDismissingKeyguard$48(final StatusBar statusBar, final PendingIntent pendingIntent) {
        new Thread(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$WzR9w2Sq2NZeoIfsG2KA_Koe9lQ
            @Override // java.lang.Runnable
            public final void run() throws PendingIntent.CanceledException {
                StatusBar.lambda$startPendingIntentDismissingKeyguard$47(this.f$0, pendingIntent);
            }
        }).start();
        return statusBar.collapsePanel();
    }

    public static /* synthetic */ void lambda$startPendingIntentDismissingKeyguard$47(StatusBar statusBar, PendingIntent pendingIntent) throws PendingIntent.CanceledException {
        try {
            ActivityManager.getService().resumeAppSwitches();
        } catch (RemoteException e) {
        }
        try {
            pendingIntent.send(null, 0, null, null, null, null, statusBar.getActivityOptions(null));
        } catch (PendingIntent.CanceledException e2) {
            Log.w("StatusBar", "Sending intent failed: " + e2);
        }
        if (pendingIntent.isActivity()) {
            statusBar.mAssistManager.hideAssist();
        }
    }

    private boolean shouldAutoCancel(StatusBarNotification statusBarNotification) {
        int i = statusBarNotification.getNotification().flags;
        return (i & 16) == 16 && (i & 64) == 0;
    }

    protected Bundle getActivityOptions(RemoteAnimationAdapter remoteAnimationAdapter) {
        ActivityOptions activityOptionsMakeBasic;
        if (remoteAnimationAdapter != null) {
            activityOptionsMakeBasic = ActivityOptions.makeRemoteAnimation(remoteAnimationAdapter);
        } else {
            activityOptionsMakeBasic = ActivityOptions.makeBasic();
        }
        activityOptionsMakeBasic.setLaunchWindowingMode(4);
        return activityOptionsMakeBasic.toBundle();
    }

    protected void visibilityChanged(boolean z) {
        if (this.mVisible != z) {
            this.mVisible = z;
            if (!z) {
                this.mGutsManager.closeAndSaveGuts(true, true, true, -1, -1, true);
            }
        }
        updateVisibleToUser();
    }

    protected void updateVisibleToUser() {
        boolean z = this.mVisibleToUser;
        this.mVisibleToUser = this.mVisible && this.mDeviceInteractive;
        if (z != this.mVisibleToUser) {
            handleVisibleToUserChanged(this.mVisibleToUser);
        }
    }

    public void clearNotificationEffects() {
        try {
            this.mBarService.clearNotificationEffects();
        } catch (RemoteException e) {
        }
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public void onUpdateRowStates() {
        int i = 1;
        if (this.mFooterView != null) {
            this.mStackScroller.changeViewPosition(this.mFooterView, this.mStackScroller.getChildCount() - 1);
            i = 2;
        }
        this.mStackScroller.changeViewPosition(this.mEmptyShadeView, this.mStackScroller.getChildCount() - i);
        this.mStackScroller.changeViewPosition(this.mNotificationShelf, this.mStackScroller.getChildCount() - (i + 1));
        this.mScrimController.setNotificationCount(this.mStackScroller.getNotGoneChildCount());
    }

    protected void notifyHeadsUpGoingToSleep() {
        maybeEscalateHeadsUp();
    }

    public boolean isBouncerShowing() {
        return this.mBouncerShowing;
    }

    public static PackageManager getPackageManagerForUser(Context context, int i) {
        if (i >= 0) {
            try {
                context = context.createPackageContextAsUser(context.getPackageName(), 4, new UserHandle(i));
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return context.getPackageManager();
    }

    public boolean isKeyguardSecure() {
        if (this.mStatusBarKeyguardViewManager == null) {
            Slog.w("StatusBar", "isKeyguardSecure() called before startKeyguard(), returning false", new Throwable());
            return false;
        }
        return this.mStatusBarKeyguardViewManager.isSecure();
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onZenChanged(int i) {
        updateEmptyShadeView();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showAssistDisclosure() {
        if (this.mAssistManager != null) {
            this.mAssistManager.showDisclosure();
        }
    }

    public NotificationPanelView getPanel() {
        return this.mNotificationPanel;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void startAssist(Bundle bundle) {
        if (this.mAssistManager != null) {
            this.mAssistManager.startAssist(bundle);
        }
    }

    public static /* synthetic */ void lambda$new$49(StatusBar statusBar) {
        if (statusBar.mNavigationBar != null) {
            statusBar.mNavigationBar.getBarTransitions().setAutoDim(true);
        }
    }

    public NotificationGutsManager getGutsManager() {
        return this.mGutsManager;
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public boolean isPresenterLocked() {
        return this.mState == 1;
    }

    @Override // com.android.systemui.statusbar.NotificationPresenter
    public Handler getHandler() {
        return this.mHandler;
    }

    public static /* synthetic */ void lambda$new$51(StatusBar statusBar, final Runnable runnable, StatusBarNotification statusBarNotification) {
        if (statusBar.mLockscreenUserManager.isLockscreenPublicMode(statusBarNotification.getUser().getIdentifier()) && (statusBar.mState == 1 || statusBar.mState == 2)) {
            statusBar.onLockedNotificationImportanceChange(new KeyguardHostView.OnDismissAction() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$StatusBar$gmnfeggGTJaRjfJHO2GTfe-r4PU
                @Override // com.android.keyguard.KeyguardHostView.OnDismissAction
                public final boolean onDismiss() {
                    return StatusBar.lambda$new$50(runnable);
                }
            });
        } else {
            runnable.run();
        }
    }

    static /* synthetic */ boolean lambda$new$50(Runnable runnable) {
        runnable.run();
        return true;
    }

    private final boolean supportCustomizeCarrierLabel() {
        return this.mStatusBarPlmnPlugin != null && this.mStatusBarPlmnPlugin.supportCustomizeCarrierLabel() && this.mNetworkController != null && this.mNetworkController.hasMobileDataFeature();
    }

    private final void updateCustomizeCarrierLabelVisibility(boolean z) {
        if (DEBUG) {
            Log.d("StatusBar", "updateCustomizeCarrierLabelVisibility(), force = " + z + ", mState = " + this.mState);
        }
        this.mStatusBarPlmnPlugin.updateCarrierLabelVisibility(z, this.mStackScroller.getVisibility() == 0 && this.mState != 1);
    }

    protected void updateCarrierLabelVisibility(boolean z) {
        if (supportCustomizeCarrierLabel()) {
            if (this.mState == 1 || this.mNotificationPanel.isPanelVisibleBecauseOfHeadsUp()) {
                if (this.mCustomizeCarrierLabel != null) {
                    this.mCustomizeCarrierLabel.setVisibility(8);
                    return;
                }
                return;
            }
            updateCustomizeCarrierLabelVisibility(z);
        }
    }
}
