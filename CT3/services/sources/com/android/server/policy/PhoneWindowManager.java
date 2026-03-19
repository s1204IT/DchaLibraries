package com.android.server.policy;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.IUiModeManager;
import android.app.ProfilerInfo;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.service.dreams.IDreamManager;
import android.telecom.TelecomManager;
import android.util.EventLog;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.IApplicationToken;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerInternal;
import android.view.WindowManagerPolicy;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.IShortcutService;
import com.android.internal.policy.PhoneWindow;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.ScreenShapeHelper;
import com.android.internal.widget.PointerLocationView;
import com.android.server.GestureLauncherService;
import com.android.server.LocalServices;
import com.android.server.NetworkManagementService;
import com.android.server.audio.AudioService;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.SystemGesturesPointerEventListener;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.mediatek.anrmanager.ANRManager;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

public class PhoneWindowManager implements WindowManagerPolicy {
    static final int APPLICATION_ABOVE_SUB_PANEL_SUBLAYER = 3;
    static final int APPLICATION_MEDIA_OVERLAY_SUBLAYER = -1;
    static final int APPLICATION_MEDIA_SUBLAYER = -2;
    static final int APPLICATION_PANEL_SUBLAYER = 1;
    static final int APPLICATION_SUB_PANEL_SUBLAYER = 2;
    private static final int BRIGHTNESS_STEPS = 10;
    private static final int DISMISS_KEYGUARD_CONTINUE = 2;
    private static final int DISMISS_KEYGUARD_NONE = 0;
    private static final int DISMISS_KEYGUARD_START = 1;
    private static final int DISMISS_SCREEN_PINNING_KEY_CODE = 4;
    static final int DOUBLE_TAP_HOME_NOTHING = 0;
    static final int DOUBLE_TAP_HOME_RECENT_SYSTEM_UI = 1;
    static final boolean ENABLE_DESK_DOCK_HOME_CAPTURE = false;
    public static final String IPO_DISABLE = "android.intent.action.ACTION_BOOT_IPO";
    public static final String IPO_ENABLE = "android.intent.action.ACTION_SHUTDOWN_IPO";
    static final boolean IS_USER_BUILD;
    private static final float KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER = 2.5f;
    static final int KEY_DISPATCH_MODE_ALL_DISABLE = 1;
    static final int KEY_DISPATCH_MODE_ALL_ENABLE = 0;
    static final int KEY_DISPATCH_MODE_HOME_DISABLE = 2;
    static final int LAST_LONG_PRESS_HOME_BEHAVIOR = 2;
    static final int LONG_PRESS_BACK_GO_TO_VOICE_ASSIST = 1;
    static final int LONG_PRESS_BACK_NOTHING = 0;
    static final int LONG_PRESS_HOME_ASSIST = 2;
    static final int LONG_PRESS_HOME_NOTHING = 0;
    static final int LONG_PRESS_HOME_RECENT_SYSTEM_UI = 1;
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int LONG_PRESS_POWER_NOTHING = 0;
    static final int LONG_PRESS_POWER_SHUT_OFF = 2;
    static final int LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM = 3;
    private static final int MSG_BACK_LONG_PRESS = 18;
    private static final int MSG_DISABLE_POINTER_LOCATION = 2;
    private static final int MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK = 4;
    private static final int MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK = 3;
    private static final int MSG_DISPATCH_SHOW_GLOBAL_ACTIONS = 10;
    private static final int MSG_DISPATCH_SHOW_RECENTS = 9;
    private static final int MSG_DISPOSE_INPUT_CONSUMER = 19;
    private static final int MSG_ENABLE_POINTER_LOCATION = 1;
    private static final int MSG_HIDE_BOOT_MESSAGE = 11;
    private static final int MSG_KEYGUARD_DRAWN_COMPLETE = 5;
    private static final int MSG_KEYGUARD_DRAWN_TIMEOUT = 6;
    private static final int MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK = 12;
    private static final int MSG_POWER_DELAYED_PRESS = 13;
    private static final int MSG_POWER_LONG_PRESS = 14;
    private static final int MSG_REQUEST_TRANSIENT_BARS = 16;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_NAVIGATION = 1;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_STATUS = 0;
    private static final int MSG_SHOW_TV_PICTURE_IN_PICTURE_MENU = 17;
    private static final int MSG_UPDATE_DREAMING_SLEEP_TOKEN = 15;
    private static final int MSG_WINDOW_MANAGER_DRAWN_COMPLETE = 7;
    static final int MULTI_PRESS_POWER_BRIGHTNESS_BOOST = 2;
    static final int MULTI_PRESS_POWER_NOTHING = 0;
    static final int MULTI_PRESS_POWER_THEATER_MODE = 1;
    static final int NAV_BAR_OPAQUE_WHEN_FREEFORM_OR_DOCKED = 0;
    static final int NAV_BAR_TRANSLUCENT_WHEN_FREEFORM_OPAQUE_OTHERWISE = 1;
    private static final String NORMAL_BOOT_ACTION = "android.intent.action.normal.boot";
    private static final String NORMAL_SHUTDOWN_ACTION = "android.intent.action.normal.shutdown";
    private static final long PANIC_GESTURE_EXPIRATION = 30000;
    static final boolean PRINT_ANIM = false;
    private static final long SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS = 150;
    static final int SHORT_PRESS_POWER_GO_HOME = 4;
    static final int SHORT_PRESS_POWER_GO_TO_SLEEP = 1;
    static final int SHORT_PRESS_POWER_NOTHING = 0;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP = 2;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME = 3;
    static final int SHORT_PRESS_SLEEP_GO_TO_SLEEP = 0;
    static final int SHORT_PRESS_SLEEP_GO_TO_SLEEP_AND_GO_HOME = 1;
    static final int SHORT_PRESS_WINDOW_NOTHING = 0;
    static final int SHORT_PRESS_WINDOW_PICTURE_IN_PICTURE = 1;
    static final boolean SHOW_PROCESSES_ON_ALT_MENU = false;
    static final boolean SHOW_STARTING_ANIMATIONS = true;
    public static final String STK_USERACTIVITY = "android.intent.action.stk.USER_ACTIVITY";
    public static final String STK_USERACTIVITY_ENABLE = "android.intent.action.stk.USER_ACTIVITY.enable";
    public static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";
    public static final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    public static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    public static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    static final int SYSTEM_UI_CHANGING_LAYOUT = -1073709042;
    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_SCREENSHOT_ERROR_RECEIVER = "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver";
    private static final String SYSUI_SCREENSHOT_SERVICE = "com.android.systemui.screenshot.TakeScreenshotService";
    static final String TAG = "WindowManager";
    static final int WAITING_FOR_DRAWN_TIMEOUT = 1000;
    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK;
    static final Rect mTmpContentFrame;
    static final Rect mTmpDecorFrame;
    static final Rect mTmpDisplayFrame;
    static final Rect mTmpNavigationFrame;
    static final Rect mTmpOutsetFrame;
    static final Rect mTmpOverscanFrame;
    static final Rect mTmpParentFrame;
    private static final Rect mTmpRect;
    static final Rect mTmpStableFrame;
    static final Rect mTmpVisibleFrame;
    boolean isUspEnable;
    boolean mAccelerometerDefault;
    AccessibilityManager mAccessibilityManager;
    ActivityManagerInternal mActivityManagerInternal;
    boolean mAllowLockscreenWhenOn;
    private boolean mAllowTheaterModeWakeFromCameraLens;
    private boolean mAllowTheaterModeWakeFromKey;
    private boolean mAllowTheaterModeWakeFromLidSwitch;
    private boolean mAllowTheaterModeWakeFromMotion;
    private boolean mAllowTheaterModeWakeFromMotionWhenNotDreaming;
    private boolean mAllowTheaterModeWakeFromPowerKey;
    private boolean mAllowTheaterModeWakeFromWakeGesture;
    boolean mAppLaunchTimeEnabled;
    AppOpsManager mAppOpsManager;
    boolean mAssistKeyLongPressed;
    boolean mAwake;
    volatile boolean mBackKeyHandled;
    volatile boolean mBeganFromNonInteractive;
    boolean mBootMessageNeedsHiding;
    PowerManager.WakeLock mBroadcastWakeLock;
    BurnInProtectionHelper mBurnInProtectionHelper;
    long[] mCalendarDateVibePattern;
    volatile boolean mCameraGestureTriggeredDuringGoingToSleep;
    boolean mCarDockEnablesAccelerometer;
    Intent mCarDockIntent;
    int mCarDockRotation;
    long[] mClockTickVibePattern;
    boolean mConsumeSearchKeyUp;
    int mContentBottom;
    int mContentLeft;
    int mContentRight;
    int mContentTop;
    Context mContext;
    long[] mContextClickVibePattern;
    int mCurBottom;
    int mCurLeft;
    int mCurRight;
    int mCurTop;
    private int mCurrentUserId;
    private boolean mDeferBindKeyguard;
    int mDemoHdmiRotation;
    boolean mDemoHdmiRotationLock;
    int mDemoRotation;
    boolean mDemoRotationLock;
    boolean mDeskDockEnablesAccelerometer;
    Intent mDeskDockIntent;
    int mDeskDockRotation;
    Display mDisplay;
    private int mDisplayRotation;
    int mDockBottom;
    int mDockLayer;
    int mDockLeft;
    int mDockRight;
    int mDockTop;
    int mDoublePressOnPowerBehavior;
    private int mDoubleTapOnHomeBehavior;
    DreamManagerInternal mDreamManagerInternal;
    boolean mDreamingLockscreen;
    ActivityManagerInternal.SleepToken mDreamingSleepToken;
    boolean mDreamingSleepTokenNeeded;
    volatile boolean mEndCallKeyHandled;
    int mEndcallBehavior;
    IApplicationToken mFocusedApp;
    WindowManagerPolicy.WindowState mFocusedWindow;
    boolean mForceShowSystemBars;
    boolean mForceStatusBar;
    boolean mForceStatusBarFromKeyguard;
    private boolean mForceStatusBarTransparent;
    boolean mForcingShowNavBar;
    int mForcingShowNavBarLayer;
    GlobalActions mGlobalActions;
    private GlobalKeyManager mGlobalKeyManager;
    private boolean mGoToSleepOnButtonPressTheaterMode;
    volatile boolean mGoingToSleep;
    Handler mHandler;
    private boolean mHasFeatureWatch;
    boolean mHaveBuiltInKeyboard;
    boolean mHavePendingMediaKeyRepeatWithWakeLock;
    HdmiControl mHdmiControl;
    boolean mHdmiPlugged;
    boolean mHideLockScreen;
    boolean mHomeConsumed;
    boolean mHomeDoubleTapPending;
    Intent mHomeIntent;
    boolean mHomePressed;
    private ImmersiveModeConfirmation mImmersiveModeConfirmation;
    int mIncallPowerBehavior;
    int mInitialMetaState;
    InputManagerInternal mInputManagerInternal;
    private long mKeyRemappingSendFakeKeyDownTime;
    private boolean mKeyRemappingVolumeDownLongPressed;
    private boolean mKeyRemappingVolumeUpLongPressed;
    long[] mKeyboardTapVibePattern;
    KeyguardServiceDelegate mKeyguardDelegate;
    boolean mKeyguardDrawComplete;
    private boolean mKeyguardDrawnOnce;
    private boolean mKeyguardHidden;
    volatile boolean mKeyguardOccluded;
    private WindowManagerPolicy.WindowState mKeyguardScrim;
    boolean mKeyguardSecure;
    boolean mKeyguardSecureIncludingHidden;
    boolean mLanguageSwitchKeyPressed;
    int mLastDockedStackSysUiFlags;
    int mLastFullscreenStackSysUiFlags;
    int mLastSystemUiFlags;
    boolean mLidControlsScreenLock;
    boolean mLidControlsSleep;
    int mLidKeyboardAccessibility;
    int mLidNavigationAccessibility;
    int mLidOpenRotation;
    int mLockScreenTimeout;
    boolean mLockScreenTimerActive;
    int mLongPressOnBackBehavior;
    private int mLongPressOnHomeBehavior;
    int mLongPressOnPowerBehavior;
    long[] mLongPressVibePattern;
    int mMetaState;
    MyOrientationListener mOrientationListener;
    int mOverscanScreenHeight;
    int mOverscanScreenLeft;
    int mOverscanScreenTop;
    int mOverscanScreenWidth;
    boolean mPendingCapsLockToggle;
    boolean mPendingMetaAction;
    private long mPendingPanicGestureUptime;
    PointerLocationView mPointerLocationView;
    volatile boolean mPowerKeyHandled;
    volatile int mPowerKeyPressCounter;
    PowerManager.WakeLock mPowerKeyWakeLock;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    boolean mPreloadedRecentApps;
    int mRecentAppsHeldModifiers;
    volatile boolean mRecentsVisible;
    int mRestrictedOverscanScreenHeight;
    int mRestrictedOverscanScreenLeft;
    int mRestrictedOverscanScreenTop;
    int mRestrictedOverscanScreenWidth;
    int mRestrictedScreenHeight;
    int mRestrictedScreenLeft;
    int mRestrictedScreenTop;
    int mRestrictedScreenWidth;
    boolean mSafeMode;
    long[] mSafeModeDisabledVibePattern;
    long[] mSafeModeEnabledVibePattern;
    ActivityManagerInternal.SleepToken mScreenOffSleepToken;
    boolean mScreenOnEarly;
    boolean mScreenOnFully;
    WindowManagerPolicy.ScreenOnListener mScreenOnListener;
    private boolean mScreenshotChordEnabled;
    private long mScreenshotChordPowerKeyTime;
    private boolean mScreenshotChordPowerKeyTriggered;
    private boolean mScreenshotChordVolumeDownKeyConsumed;
    private long mScreenshotChordVolumeDownKeyTime;
    private boolean mScreenshotChordVolumeDownKeyTriggered;
    private boolean mScreenshotChordVolumeUpKeyTriggered;
    boolean mSearchKeyShortcutPending;
    SearchManager mSearchManager;
    private boolean mSecureDismissingKeyguard;
    SettingsObserver mSettingsObserver;
    int mShortPressOnPowerBehavior;
    int mShortPressOnSleepBehavior;
    int mShortPressWindowBehavior;
    ShortcutManager mShortcutManager;
    boolean mShowingDream;
    boolean mShowingLockscreen;
    int mStableBottom;
    int mStableFullscreenBottom;
    int mStableFullscreenLeft;
    int mStableFullscreenRight;
    int mStableFullscreenTop;
    int mStableLeft;
    int mStableRight;
    int mStableTop;
    int mStatusBarHeight;
    int mStatusBarLayer;
    StatusBarManagerInternal mStatusBarManagerInternal;
    IStatusBarService mStatusBarService;
    boolean mSupportAutoRotation;
    private boolean mSupportLongPressPowerWhenNonInteractive;
    boolean mSystemBooted;
    int mSystemBottom;
    private SystemGesturesPointerEventListener mSystemGestures;
    int mSystemLeft;
    boolean mSystemReady;
    int mSystemRight;
    int mSystemTop;
    WindowManagerPolicy.WindowState mTopDockedOpaqueOrDimmingWindowState;
    WindowManagerPolicy.WindowState mTopDockedOpaqueWindowState;
    WindowManagerPolicy.WindowState mTopFullscreenOpaqueOrDimmingWindowState;
    WindowManagerPolicy.WindowState mTopFullscreenOpaqueWindowState;
    boolean mTopIsFullscreen;
    int mTriplePressOnPowerBehavior;
    volatile boolean mTvPictureInPictureVisible;
    int mUiMode;
    IUiModeManager mUiModeManager;
    int mUndockedHdmiRotation;
    int mUnrestrictedScreenHeight;
    int mUnrestrictedScreenLeft;
    int mUnrestrictedScreenTop;
    int mUnrestrictedScreenWidth;
    boolean mUseTvRouting;
    Vibrator mVibrator;
    long[] mVirtualKeyVibePattern;
    int mVoiceContentBottom;
    int mVoiceContentLeft;
    int mVoiceContentRight;
    int mVoiceContentTop;
    boolean mWakeGestureEnabledSetting;
    MyWakeGestureListener mWakeGestureListener;
    private WindowManagerPolicy.WindowState mWinDismissingKeyguard;
    private WindowManagerPolicy.WindowState mWinShowWhenLocked;
    IWindowManager mWindowManager;
    boolean mWindowManagerDrawComplete;
    WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    WindowManagerInternal mWindowManagerInternal;
    static boolean DEBUG = false;
    static boolean localLOGV = false;
    static boolean DEBUG_INPUT = false;
    static boolean DEBUG_KEYGUARD = false;
    static boolean DEBUG_LAYOUT = false;
    static boolean DEBUG_STARTING_WINDOW = false;
    static boolean DEBUG_WAKEUP = false;
    static boolean DEBUG_ORIENTATION = false;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    static SparseArray<String> sApplicationLaunchKeyCategories = new SparseArray<>();
    private final Object mLock = new Object();
    final Object mServiceAquireLock = new Object();
    boolean mEnableShiftMenuBugReports = false;
    WindowManagerPolicy.WindowState mStatusBar = null;
    WindowManagerPolicy.WindowState mNavigationBar = null;
    boolean mHasNavigationBar = false;
    boolean mCanHideNavigationBar = false;
    boolean mNavigationBarCanMove = false;
    boolean mNavigationBarOnBottom = true;
    int[] mNavigationBarHeightForRotationDefault = new int[4];
    int[] mNavigationBarWidthForRotationDefault = new int[4];
    int[] mNavigationBarHeightForRotationInCarMode = new int[4];
    int[] mNavigationBarWidthForRotationInCarMode = new int[4];
    private LongSparseArray<IShortcutService> mShortcutKeyServices = new LongSparseArray<>();
    private boolean mEnableCarDockHomeCapture = true;
    final Runnable mWindowManagerDrawCallback = new Runnable() {
        @Override
        public void run() {
            if (PhoneWindowManager.DEBUG_WAKEUP) {
                Slog.i(PhoneWindowManager.TAG, "All windows ready for display!");
            }
            PhoneWindowManager.this.mHandler.sendEmptyMessage(7);
        }
    };
    final KeyguardServiceDelegate.DrawnListener mKeyguardDrawnCallback = new KeyguardServiceDelegate.DrawnListener() {
        @Override
        public void onDrawn() {
            if (PhoneWindowManager.DEBUG_WAKEUP) {
                Slog.d(PhoneWindowManager.TAG, "mKeyguardDelegate.ShowListener.onDrawn.");
            }
            PhoneWindowManager.this.mHandler.sendEmptyMessage(5);
        }
    };
    WindowManagerPolicy.WindowState mLastInputMethodWindow = null;
    WindowManagerPolicy.WindowState mLastInputMethodTargetWindow = null;
    int mLidState = -1;
    int mCameraLensCoverState = -1;
    int mDockMode = 0;
    private boolean mForceDefaultOrientation = false;
    int mUserRotationMode = 0;
    int mUserRotation = 0;
    int mAllowAllRotations = -1;
    boolean mOrientationSensorEnabled = false;
    int mCurrentAppOrientation = -1;
    boolean mHasSoftInput = false;
    boolean mTranslucentDecorEnabled = true;
    int mPointerLocationMode = 0;
    int mResettingSystemUiFlags = 0;
    int mForceClearedSystemUiFlags = 0;
    final Rect mNonDockedStackBounds = new Rect();
    final Rect mDockedStackBounds = new Rect();
    final Rect mLastNonDockedStackBounds = new Rect();
    final Rect mLastDockedStackBounds = new Rect();
    boolean mLastFocusNeedsMenu = false;
    WindowManagerPolicy.InputConsumer mInputConsumer = null;
    HashSet<IApplicationToken> mAppsToBeHidden = new HashSet<>();
    HashSet<IApplicationToken> mAppsThatDismissKeyguard = new HashSet<>();
    int mNavBarOpacityMode = 0;
    int mDismissKeyguard = 0;
    int mHideNavigationBar = 0;
    int mLandscapeRotation = 0;
    int mSeascapeRotation = 0;
    int mPortraitRotation = 0;
    int mUpsideDownRotation = 0;
    int mOverscanLeft = 0;
    int mOverscanTop = 0;
    int mOverscanRight = 0;
    int mOverscanBottom = 0;
    private final SparseArray<KeyCharacterMap.FallbackAction> mFallbackActions = new SparseArray<>();
    private final LogDecelerateInterpolator mLogDecelerateInterpolator = new LogDecelerateInterpolator(100, 0);
    private final MutableBoolean mTmpBoolean = new MutableBoolean(false);
    private UEventObserver mHDMIObserver = new UEventObserver() {
        public void onUEvent(UEventObserver.UEvent event) {
            PhoneWindowManager.this.setHdmiPlugged("1".equals(event.get("SWITCH_STATE")));
        }
    };
    private final StatusBarController mStatusBarController = new StatusBarController();
    private final BarController mNavigationBarController = new BarController("NavigationBar", 134217728, 536870912, Integer.MIN_VALUE, 2, 134217728, PackageManagerService.DumpState.DUMP_VERSION);
    private final Runnable mEndCallLongPress = new Runnable() {
        @Override
        public void run() {
            PhoneWindowManager.this.mEndCallKeyHandled = true;
            if (!PhoneWindowManager.this.performHapticFeedbackLw(null, 0, false)) {
                PhoneWindowManager.this.performAuditoryFeedbackForAccessibilityIfNeed();
            }
            PhoneWindowManager.this.showGlobalActionsInternal();
        }
    };
    private final ScreenshotRunnable mScreenshotRunnable = new ScreenshotRunnable(this, null);
    private final Runnable mHomeDoubleTapTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!PhoneWindowManager.this.mHomeDoubleTapPending) {
                return;
            }
            PhoneWindowManager.this.mHomeDoubleTapPending = false;
            PhoneWindowManager.this.handleShortPressOnHome();
        }
    };
    private final Runnable mClearHideNavigationFlag = new Runnable() {
        @Override
        public void run() {
            synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                PhoneWindowManager.this.mForceClearedSystemUiFlags &= -3;
            }
            PhoneWindowManager.this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
        }
    };
    final InputEventReceiver.Factory mHideNavInputEventReceiverFactory = new InputEventReceiver.Factory() {
        public InputEventReceiver createInputEventReceiver(InputChannel inputChannel, Looper looper) {
            return PhoneWindowManager.this.new HideNavInputEventReceiver(inputChannel, looper);
        }
    };
    final Object mScreenshotLock = new Object();
    ServiceConnection mScreenshotConnection = null;
    final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (PhoneWindowManager.this.mScreenshotLock) {
                if (PhoneWindowManager.this.mScreenshotConnection != null) {
                    PhoneWindowManager.this.mContext.unbindService(PhoneWindowManager.this.mScreenshotConnection);
                    PhoneWindowManager.this.mScreenshotConnection = null;
                    PhoneWindowManager.this.notifyScreenshotError();
                }
            }
        }
    };
    BroadcastReceiver mDockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.DOCK_EVENT".equals(intent.getAction())) {
                PhoneWindowManager.this.mDockMode = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
            } else {
                try {
                    IUiModeManager uiModeService = IUiModeManager.Stub.asInterface(ServiceManager.getService("uimode"));
                    PhoneWindowManager.this.mUiMode = uiModeService.getCurrentModeType();
                } catch (RemoteException e) {
                }
            }
            PhoneWindowManager.this.updateRotation(true);
            synchronized (PhoneWindowManager.this.mLock) {
                PhoneWindowManager.this.updateOrientationListenerLp();
            }
        }
    };
    BroadcastReceiver mDreamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.DREAMING_STARTED".equals(intent.getAction())) {
                Slog.v(PhoneWindowManager.TAG, "*** onDreamingStarted");
                if (PhoneWindowManager.this.mKeyguardDelegate == null) {
                    return;
                }
                PhoneWindowManager.this.mKeyguardDelegate.onDreamingStarted();
                return;
            }
            if (!"android.intent.action.DREAMING_STOPPED".equals(intent.getAction())) {
                return;
            }
            Slog.v(PhoneWindowManager.TAG, "*** onDreamingStopped");
            if (PhoneWindowManager.this.mKeyguardDelegate == null) {
                return;
            }
            PhoneWindowManager.this.mKeyguardDelegate.onDreamingStopped();
        }
    };
    BroadcastReceiver mMultiuserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                return;
            }
            PhoneWindowManager.this.mSettingsObserver.onChange(false);
            synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                PhoneWindowManager.this.mLastSystemUiFlags = 0;
                PhoneWindowManager.this.updateSystemUiVisibilityLw();
            }
        }
    };
    private final Runnable mHiddenNavPanic = new Runnable() {
        @Override
        public void run() {
            synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                if (!PhoneWindowManager.this.isUserSetupComplete()) {
                    return;
                }
                PhoneWindowManager.this.mPendingPanicGestureUptime = SystemClock.uptimeMillis();
                PhoneWindowManager.this.mNavigationBarController.showTransient();
            }
        }
    };
    ProgressDialog mBootMsgDialog = null;
    ScreenLockTimeout mScreenLockTimeout = new ScreenLockTimeout();
    private boolean mIsAlarmBoot = isAlarmBoot();
    private boolean mIsShutDown = false;
    BroadcastReceiver mPoweroffAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(PhoneWindowManager.TAG, "mIpoEventReceiver -- onReceive -- entry");
            String action = intent.getAction();
            SystemProperties.set("sys.boot.reason", "0");
            PhoneWindowManager.this.mIsAlarmBoot = false;
            if (action.equals(PhoneWindowManager.NORMAL_SHUTDOWN_ACTION)) {
                Log.v(PhoneWindowManager.TAG, "Receive NORMAL_SHUTDOWN_ACTION");
                PhoneWindowManager.this.mIsShutDown = true;
            } else {
                if (!PhoneWindowManager.NORMAL_BOOT_ACTION.equals(action)) {
                    return;
                }
                Log.v(PhoneWindowManager.TAG, "Receive NORMAL_BOOT_ACTION");
                SystemProperties.set("service.bootanim.exit", "0");
                SystemProperties.set("ctl.start", "bootanim");
            }
        }
    };
    final Object mKeyDispatchLock = new Object();
    int mIPOUserRotation = 0;
    BroadcastReceiver mIpoEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(PhoneWindowManager.TAG, "mIpoEventReceiver -- onReceive -- entry");
            String action = intent.getAction();
            if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                Log.v(PhoneWindowManager.TAG, "Receive IPO_ENABLE");
                PhoneWindowManager.this.ipoSystemShutdown();
            } else if (action.equals("android.intent.action.ACTION_BOOT_IPO")) {
                Log.v(PhoneWindowManager.TAG, "Receive IPO_DISABLE");
                PhoneWindowManager.this.ipoSystemBooted();
            } else {
                Log.v(PhoneWindowManager.TAG, "Receive Fake Intent");
            }
        }
    };
    boolean mIsStkUserActivityEnabled = false;
    private Object mStkLock = new Object();
    BroadcastReceiver mStkUserActivityEnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(PhoneWindowManager.TAG, "mStkUserActivityEnReceiver -- onReceive -- entry");
            synchronized (PhoneWindowManager.this.mStkLock) {
                if (action.equals(PhoneWindowManager.STK_USERACTIVITY_ENABLE)) {
                    if (PhoneWindowManager.DEBUG_INPUT) {
                        Log.v(PhoneWindowManager.TAG, "Receive STK_ENABLE");
                    }
                    boolean enabled = intent.getBooleanExtra(AudioService.CONNECT_INTENT_KEY_STATE, false);
                    if (enabled != PhoneWindowManager.this.mIsStkUserActivityEnabled) {
                        PhoneWindowManager.this.mIsStkUserActivityEnabled = enabled;
                    }
                } else if (PhoneWindowManager.DEBUG_INPUT) {
                    Log.e(PhoneWindowManager.TAG, "Receive Fake Intent");
                }
                if (PhoneWindowManager.DEBUG_INPUT) {
                    Log.v(PhoneWindowManager.TAG, "mStkUserActivityEnReceiver -- onReceive -- exist " + PhoneWindowManager.this.mIsStkUserActivityEnabled);
                }
            }
        }
    };
    Runnable mNotifyStk = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(PhoneWindowManager.STK_USERACTIVITY);
            PhoneWindowManager.this.mContext.sendBroadcast(intent);
        }
    };
    int mScreenOffReason = -1;
    int mKeyDispatcMode = 0;
    private Runnable mKeyRemappingVolumeDownLongPress_Test = new Runnable() {
        @Override
        public void run() {
            KeyEvent keyEvent = new KeyEvent(1, 4);
            InputManager inputManager = (InputManager) PhoneWindowManager.this.mContext.getSystemService("input");
            Log.d(PhoneWindowManager.TAG, ">>>>>>>> InjectEvent Start");
            inputManager.injectInputEvent(keyEvent, 2);
            try {
                Log.d(PhoneWindowManager.TAG, "***** Sleeping.");
                Thread.sleep(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                Log.d(PhoneWindowManager.TAG, "***** Waking up.");
            } catch (IllegalArgumentException e) {
                Log.d(PhoneWindowManager.TAG, "IllegalArgumentException: ", e);
            } catch (InterruptedException e2) {
                Log.d(PhoneWindowManager.TAG, "InterruptedException: ", e2);
            } catch (SecurityException e3) {
                Log.d(PhoneWindowManager.TAG, "SecurityException: ", e3);
            }
            Log.d(PhoneWindowManager.TAG, "<<<<<<<< InjectEvent End");
        }
    };
    private Runnable mKeyRemappingVolumeUpLongPress = new Runnable() {
        @Override
        public void run() {
            PhoneWindowManager.this.showRecentApps(false);
            PhoneWindowManager.this.mKeyRemappingVolumeUpLongPressed = true;
        }
    };
    private Runnable mKeyRemappingVolumeDownLongPress = new Runnable() {
        @Override
        public void run() {
            PhoneWindowManager.this.keyRemappingSendFakeKeyEvent(0, 82);
            PhoneWindowManager.this.keyRemappingSendFakeKeyEvent(1, 82);
            PhoneWindowManager.this.mKeyRemappingVolumeDownLongPressed = true;
        }
    };

    public PhoneWindowManager() {
        this.mAppLaunchTimeEnabled = 1 == SystemProperties.getInt("ro.mtk_perf_response_time", 0);
        this.isUspEnable = "no".equals(SystemProperties.get("ro.mtk_carrierexpress_pack", "no")) ? false : true;
    }

    static {
        boolean zEquals;
        sApplicationLaunchKeyCategories.append(64, "android.intent.category.APP_BROWSER");
        sApplicationLaunchKeyCategories.append(65, "android.intent.category.APP_EMAIL");
        sApplicationLaunchKeyCategories.append(207, "android.intent.category.APP_CONTACTS");
        sApplicationLaunchKeyCategories.append(208, "android.intent.category.APP_CALENDAR");
        sApplicationLaunchKeyCategories.append(209, "android.intent.category.APP_MUSIC");
        sApplicationLaunchKeyCategories.append(210, "android.intent.category.APP_CALCULATOR");
        mTmpParentFrame = new Rect();
        mTmpDisplayFrame = new Rect();
        mTmpOverscanFrame = new Rect();
        mTmpContentFrame = new Rect();
        mTmpVisibleFrame = new Rect();
        mTmpDecorFrame = new Rect();
        mTmpStableFrame = new Rect();
        mTmpNavigationFrame = new Rect();
        mTmpOutsetFrame = new Rect();
        mTmpRect = new Rect();
        WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = new int[]{2003, 2010};
        if ("user".equals(Build.TYPE)) {
            zEquals = true;
        } else {
            zEquals = "userdebug".equals(Build.TYPE);
        }
        IS_USER_BUILD = zEquals;
    }

    private class PolicyHandler extends Handler {
        PolicyHandler(PhoneWindowManager this$0, PolicyHandler policyHandler) {
            this();
        }

        private PolicyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PhoneWindowManager.this.enablePointerLocation();
                    break;
                case 2:
                    PhoneWindowManager.this.disablePointerLocation();
                    break;
                case 3:
                    PhoneWindowManager.this.dispatchMediaKeyWithWakeLock((KeyEvent) msg.obj);
                    break;
                case 4:
                    PhoneWindowManager.this.dispatchMediaKeyRepeatWithWakeLock((KeyEvent) msg.obj);
                    break;
                case 5:
                    if (PhoneWindowManager.DEBUG_WAKEUP) {
                        Slog.w(PhoneWindowManager.TAG, "Setting mKeyguardDrawComplete");
                    }
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    break;
                case 6:
                    Slog.w(PhoneWindowManager.TAG, "Keyguard drawn timeout. Setting mKeyguardDrawComplete");
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    break;
                case 7:
                    if (PhoneWindowManager.DEBUG_WAKEUP) {
                        Slog.w(PhoneWindowManager.TAG, "Setting mWindowManagerDrawComplete");
                    }
                    PhoneWindowManager.this.finishWindowsDrawn();
                    break;
                case 9:
                    PhoneWindowManager.this.showRecentApps(false, msg.arg1 != 0);
                    break;
                case 10:
                    PhoneWindowManager.this.showGlobalActionsInternal();
                    break;
                case 11:
                    PhoneWindowManager.this.handleHideBootMessage();
                    break;
                case 12:
                    PhoneWindowManager.this.launchVoiceAssistWithWakeLock(msg.arg1 != 0);
                    break;
                case 13:
                    PhoneWindowManager.this.powerPress(((Long) msg.obj).longValue(), msg.arg1 != 0, msg.arg2);
                    PhoneWindowManager.this.finishPowerKeyPress();
                    break;
                case 14:
                    PhoneWindowManager.this.powerLongPress();
                    break;
                case 15:
                    PhoneWindowManager.this.updateDreamingSleepToken(msg.arg1 != 0);
                    break;
                case 16:
                    WindowManagerPolicy.WindowState targetBar = msg.arg1 == 0 ? PhoneWindowManager.this.mStatusBar : PhoneWindowManager.this.mNavigationBar;
                    if (targetBar != null) {
                        PhoneWindowManager.this.requestTransientBars(targetBar);
                    }
                    break;
                case 17:
                    PhoneWindowManager.this.showTvPictureInPictureMenuInternal();
                    break;
                case 18:
                    PhoneWindowManager.this.backLongPress();
                    break;
                case 19:
                    PhoneWindowManager.this.disposeInputConsumer((WindowManagerPolicy.InputConsumer) msg.obj);
                    break;
            }
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = PhoneWindowManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("end_button_behavior"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("incall_power_button_behavior"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("wake_gesture_enabled"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("user_rotation"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("screen_off_timeout"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("pointer_location"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("immersive_mode_confirmations"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("policy_control"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("hide_navigation_bar"), false, this, -1);
            PhoneWindowManager.this.updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            PhoneWindowManager.this.updateSettings();
            PhoneWindowManager.this.updateRotation(false);
        }
    }

    class MyWakeGestureListener extends WakeGestureListener {
        MyWakeGestureListener(Context context, Handler handler) {
            super(context, handler);
        }

        @Override
        public void onWakeUp() {
            synchronized (PhoneWindowManager.this.mLock) {
                if (PhoneWindowManager.this.shouldEnableWakeGestureLp()) {
                    PhoneWindowManager.this.performHapticFeedbackLw(null, 1, false);
                    PhoneWindowManager.this.wakeUp(SystemClock.uptimeMillis(), PhoneWindowManager.this.mAllowTheaterModeWakeFromWakeGesture, "android.policy:GESTURE");
                }
            }
        }
    }

    class MyOrientationListener extends WindowOrientationListener {
        private final Runnable mUpdateRotationRunnable;

        MyOrientationListener(Context context, Handler handler) {
            super(context, handler);
            this.mUpdateRotationRunnable = new Runnable() {
                @Override
                public void run() {
                    PhoneWindowManager.this.mPowerManagerInternal.powerHint(2, 0);
                    PhoneWindowManager.this.updateRotation(false);
                }
            };
        }

        @Override
        public void onProposedRotationChanged(int rotation) {
            if (PhoneWindowManager.localLOGV) {
                Slog.v(PhoneWindowManager.TAG, "onProposedRotationChanged, rotation=" + rotation);
            }
            PhoneWindowManager.this.mHandler.post(this.mUpdateRotationRunnable);
        }
    }

    IStatusBarService getStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    StatusBarManagerInternal getStatusBarManagerInternal() {
        StatusBarManagerInternal statusBarManagerInternal;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarManagerInternal == null) {
                this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            }
            statusBarManagerInternal = this.mStatusBarManagerInternal;
        }
        return statusBarManagerInternal;
    }

    boolean needSensorRunningLp() {
        if (this.mSupportAutoRotation && (this.mCurrentAppOrientation == 4 || this.mCurrentAppOrientation == 10 || this.mCurrentAppOrientation == 7 || this.mCurrentAppOrientation == 6)) {
            return true;
        }
        if ((this.mCarDockEnablesAccelerometer && this.mDockMode == 2) || (this.mDeskDockEnablesAccelerometer && (this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == 4))) {
            return true;
        }
        if (this.mUserRotationMode == 1) {
            return false;
        }
        return this.mSupportAutoRotation;
    }

    void updateOrientationListenerLp() {
        if (this.mOrientationListener.canDetectOrientation()) {
            if (localLOGV) {
                Slog.v(TAG, "mScreenOnEarly=" + this.mScreenOnEarly + ", mAwake=" + this.mAwake + ", mCurrentAppOrientation=" + this.mCurrentAppOrientation + ", mOrientationSensorEnabled=" + this.mOrientationSensorEnabled + ", mKeyguardDrawComplete=" + this.mKeyguardDrawComplete + ", mWindowManagerDrawComplete=" + this.mWindowManagerDrawComplete);
            }
            boolean disable = true;
            if (this.mScreenOnEarly && this.mAwake && this.mKeyguardDrawComplete && this.mWindowManagerDrawComplete && needSensorRunningLp()) {
                disable = false;
                if (!this.mOrientationSensorEnabled) {
                    this.mOrientationListener.enable();
                    if (localLOGV) {
                        Slog.v(TAG, "Enabling listeners");
                    }
                    this.mOrientationSensorEnabled = true;
                }
            }
            if (disable && this.mOrientationSensorEnabled) {
                this.mOrientationListener.disable();
                if (localLOGV) {
                    Slog.v(TAG, "Disabling listeners");
                }
                this.mOrientationSensorEnabled = false;
            }
        }
    }

    private void interceptPowerKeyDown(KeyEvent event, boolean interactive) {
        if (!this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.acquire();
        }
        if (this.mPowerKeyPressCounter != 0) {
            this.mHandler.removeMessages(13);
        }
        boolean panic = this.mImmersiveModeConfirmation.onPowerKeyDown(interactive, SystemClock.elapsedRealtime(), isImmersiveMode(this.mLastSystemUiFlags));
        if (panic) {
            this.mHandler.post(this.mHiddenNavPanic);
        }
        if (interactive && !this.mScreenshotChordPowerKeyTriggered && (event.getFlags() & 1024) == 0) {
            this.mScreenshotChordPowerKeyTriggered = true;
            this.mScreenshotChordPowerKeyTime = event.getDownTime();
            interceptScreenshotChord();
        }
        TelecomManager telecomManager = getTelecommService();
        boolean hungUp = false;
        if (telecomManager != null) {
            if (telecomManager.isRinging()) {
                telecomManager.silenceRinger();
            } else if ((this.mIncallPowerBehavior & 2) != 0 && telecomManager.isInCall() && interactive) {
                hungUp = telecomManager.endCall();
            }
        }
        GestureLauncherService gestureService = (GestureLauncherService) LocalServices.getService(GestureLauncherService.class);
        boolean gesturedServiceIntercepted = false;
        if (gestureService != null) {
            gesturedServiceIntercepted = gestureService.interceptPowerKeyDown(event, interactive, this.mTmpBoolean);
            if (this.mTmpBoolean.value && this.mGoingToSleep) {
                this.mCameraGestureTriggeredDuringGoingToSleep = true;
            }
        }
        if (hungUp || this.mScreenshotChordVolumeDownKeyTriggered || this.mScreenshotChordVolumeUpKeyTriggered) {
            gesturedServiceIntercepted = true;
        }
        this.mPowerKeyHandled = gesturedServiceIntercepted;
        if (this.mPowerKeyHandled) {
            return;
        }
        if (interactive) {
            if (!hasLongPressOnPowerBehavior()) {
                return;
            }
            Message msg = this.mHandler.obtainMessage(14);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
            return;
        }
        wakeUpFromPowerKey(event.getDownTime());
        if (this.mSupportLongPressPowerWhenNonInteractive && hasLongPressOnPowerBehavior()) {
            Message msg2 = this.mHandler.obtainMessage(14);
            msg2.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg2, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
            this.mBeganFromNonInteractive = true;
            return;
        }
        int maxCount = getMaxMultiPressPowerCount();
        if (maxCount <= 1) {
            this.mPowerKeyHandled = true;
        } else {
            this.mBeganFromNonInteractive = true;
        }
    }

    private void interceptPowerKeyUp(KeyEvent event, boolean interactive, boolean canceled) {
        boolean z = !canceled ? this.mPowerKeyHandled : true;
        this.mScreenshotChordPowerKeyTriggered = false;
        cancelPendingScreenshotChordAction();
        cancelPendingPowerKeyAction();
        if (!z) {
            this.mPowerKeyPressCounter++;
            int maxCount = getMaxMultiPressPowerCount();
            long eventTime = event.getDownTime();
            if (this.mPowerKeyPressCounter < maxCount) {
                Message msg = this.mHandler.obtainMessage(13, interactive ? 1 : 0, this.mPowerKeyPressCounter, Long.valueOf(eventTime));
                msg.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(msg, ViewConfiguration.getDoubleTapTimeout());
                return;
            }
            powerPress(eventTime, interactive, this.mPowerKeyPressCounter);
        }
        finishPowerKeyPress();
    }

    private void finishPowerKeyPress() {
        this.mBeganFromNonInteractive = false;
        this.mPowerKeyPressCounter = 0;
        if (!this.mPowerKeyWakeLock.isHeld()) {
            return;
        }
        this.mPowerKeyWakeLock.release();
    }

    private void cancelPendingPowerKeyAction() {
        if (this.mPowerKeyHandled) {
            return;
        }
        this.mPowerKeyHandled = true;
        this.mHandler.removeMessages(14);
    }

    private void cancelPendingBackKeyAction() {
        if (this.mBackKeyHandled) {
            return;
        }
        this.mBackKeyHandled = true;
        this.mHandler.removeMessages(18);
    }

    private void powerPress(long eventTime, boolean interactive, int count) {
        if (this.mScreenOnEarly && !this.mScreenOnFully) {
            Slog.i(TAG, "Suppressed redundant power key press while already in the process of turning the screen on.");
        }
        if (count == 2) {
            powerMultiPressAction(eventTime, interactive, this.mDoublePressOnPowerBehavior);
            return;
        }
        if (count == 3) {
            powerMultiPressAction(eventTime, interactive, this.mTriplePressOnPowerBehavior);
            return;
        }
        if (!interactive || this.mBeganFromNonInteractive) {
            return;
        }
        switch (this.mShortPressOnPowerBehavior) {
            case 1:
                this.mPowerManager.goToSleep(eventTime, 4, 0);
                break;
            case 2:
                this.mPowerManager.goToSleep(eventTime, 4, 1);
                break;
            case 3:
                this.mPowerManager.goToSleep(eventTime, 4, 1);
                launchHomeFromHotKey();
                break;
            case 4:
                launchHomeFromHotKey(true, false);
                break;
        }
    }

    private void powerMultiPressAction(long eventTime, boolean interactive, int behavior) {
        switch (behavior) {
            case 1:
                if (!isUserSetupComplete()) {
                    Slog.i(TAG, "Ignoring toggling theater mode - device not setup.");
                    break;
                } else if (isTheaterModeEnabled()) {
                    Slog.i(TAG, "Toggling theater mode off.");
                    Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 0);
                    if (!interactive) {
                        wakeUpFromPowerKey(eventTime);
                    }
                    break;
                } else {
                    Slog.i(TAG, "Toggling theater mode on.");
                    Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 1);
                    if (this.mGoToSleepOnButtonPressTheaterMode && interactive) {
                        this.mPowerManager.goToSleep(eventTime, 4, 0);
                        break;
                    }
                }
                break;
            case 2:
                Slog.i(TAG, "Starting brightness boost.");
                if (!interactive) {
                    wakeUpFromPowerKey(eventTime);
                }
                this.mPowerManager.boostScreenBrightness(eventTime);
                break;
        }
    }

    private int getMaxMultiPressPowerCount() {
        if (this.mTriplePressOnPowerBehavior != 0) {
            return 3;
        }
        if (this.mDoublePressOnPowerBehavior != 0) {
            return 2;
        }
        return 1;
    }

    private void powerLongPress() {
        int behavior = getResolvedLongPressOnPowerBehavior();
        switch (behavior) {
            case 1:
                this.mPowerKeyHandled = true;
                if (!performHapticFeedbackLw(null, 0, false)) {
                    performAuditoryFeedbackForAccessibilityIfNeed();
                }
                showGlobalActionsInternal();
                break;
            case 2:
            case 3:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                this.mWindowManagerFuncs.shutdown(behavior == 2);
                break;
        }
    }

    private void backLongPress() {
        this.mBackKeyHandled = true;
        switch (this.mLongPressOnBackBehavior) {
            case 1:
                Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
                startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                break;
        }
    }

    private void disposeInputConsumer(WindowManagerPolicy.InputConsumer inputConsumer) {
        if (inputConsumer == null) {
            return;
        }
        inputConsumer.dismiss();
    }

    private void sleepPress(long eventTime) {
        if (this.mShortPressOnSleepBehavior != 1) {
            return;
        }
        launchHomeFromHotKey(false, true);
    }

    private void sleepRelease(long eventTime) {
        switch (this.mShortPressOnSleepBehavior) {
            case 0:
            case 1:
                Slog.i(TAG, "sleepRelease() calling goToSleep(GO_TO_SLEEP_REASON_SLEEP_BUTTON)");
                this.mPowerManager.goToSleep(eventTime, 6, 0);
                break;
        }
    }

    private int getResolvedLongPressOnPowerBehavior() {
        if (FactoryTest.isLongPressOnPowerOffEnabled()) {
            return 3;
        }
        return this.mLongPressOnPowerBehavior;
    }

    private boolean hasLongPressOnPowerBehavior() {
        return getResolvedLongPressOnPowerBehavior() != 0;
    }

    private boolean hasLongPressOnBackBehavior() {
        return this.mLongPressOnBackBehavior != 0;
    }

    private void interceptScreenshotChord() {
        Log.d("screen_capture_on", "interceptScreenshotChord  mScreenshotChordEnabled: " + this.mScreenshotChordEnabled);
        if (!this.mScreenshotChordEnabled || !this.mScreenshotChordVolumeDownKeyTriggered || !this.mScreenshotChordPowerKeyTriggered || this.mScreenshotChordVolumeUpKeyTriggered) {
            return;
        }
        Log.d("screen_capture_on", "interceptScreenshotChord in");
        long now = SystemClock.uptimeMillis();
        if (now > this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS || now > this.mScreenshotChordPowerKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
            return;
        }
        this.mScreenshotChordVolumeDownKeyConsumed = true;
        cancelPendingPowerKeyAction();
        String ScreenCaptureOn = Settings.System.getString(this.mContext.getContentResolver(), "screen_capture_on");
        if (!ScreenCaptureOn.equals("1")) {
            return;
        }
        this.mScreenshotRunnable.setScreenshotType(1);
        this.mHandler.postDelayed(this.mScreenshotRunnable, getScreenshotChordLongPressDelay());
    }

    private long getScreenshotChordLongPressDelay() {
        if (this.mKeyguardDelegate.isShowing()) {
            return (long) (ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout() * KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER);
        }
        return ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout();
    }

    private void cancelPendingScreenshotChordAction() {
        this.mHandler.removeCallbacks(this.mScreenshotRunnable);
    }

    private class ScreenshotRunnable implements Runnable {
        private int mScreenshotType;

        ScreenshotRunnable(PhoneWindowManager this$0, ScreenshotRunnable screenshotRunnable) {
            this();
        }

        private ScreenshotRunnable() {
            this.mScreenshotType = 1;
        }

        public void setScreenshotType(int screenshotType) {
            this.mScreenshotType = screenshotType;
        }

        @Override
        public void run() {
            PhoneWindowManager.this.takeScreenshot(this.mScreenshotType);
        }
    }

    public void showGlobalActions() {
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(10);
    }

    void showGlobalActionsInternal() {
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
        if (this.mGlobalActions == null) {
            this.mGlobalActions = new GlobalActions(this.mContext, this.mWindowManagerFuncs);
        }
        boolean keyguardShowing = isKeyguardShowingAndNotOccluded();
        this.mGlobalActions.showDialog(keyguardShowing, isDeviceProvisioned());
        if (!keyguardShowing) {
            return;
        }
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
    }

    boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    private void handleShortPressOnHome() {
        getHdmiControl().turnOnTv();
        if (this.mDreamManagerInternal != null && this.mDreamManagerInternal.isDreaming()) {
            this.mDreamManagerInternal.stopDream(false);
        } else {
            launchHomeFromHotKey();
        }
    }

    private HdmiControl getHdmiControl() {
        HdmiControl hdmiControl = null;
        if (this.mHdmiControl == null) {
            HdmiControlManager manager = (HdmiControlManager) this.mContext.getSystemService("hdmi_control");
            HdmiPlaybackClient client = null;
            if (manager != null) {
                client = manager.getPlaybackClient();
            }
            this.mHdmiControl = new HdmiControl(client, hdmiControl);
        }
        return this.mHdmiControl;
    }

    private static class HdmiControl {
        private final HdmiPlaybackClient mClient;

        HdmiControl(HdmiPlaybackClient client, HdmiControl hdmiControl) {
            this(client);
        }

        private HdmiControl(HdmiPlaybackClient client) {
            this.mClient = client;
        }

        public void turnOnTv() {
            if (this.mClient == null) {
                return;
            }
            this.mClient.oneTouchPlay(new HdmiPlaybackClient.OneTouchPlayCallback() {
                public void onComplete(int result) {
                    if (result == 0) {
                        return;
                    }
                    Log.w(PhoneWindowManager.TAG, "One touch play failed: " + result);
                }
            });
        }
    }

    private void handleLongPressOnHome(int deviceId) {
        if (this.mLongPressOnHomeBehavior == 0) {
        }
        this.mHomeConsumed = true;
        performHapticFeedbackLw(null, 0, false);
        switch (this.mLongPressOnHomeBehavior) {
            case 1:
                toggleRecentApps();
                break;
            case 2:
                launchAssistAction(null, deviceId);
                break;
            default:
                Log.w(TAG, "Undefined home long press behavior: " + this.mLongPressOnHomeBehavior);
                break;
        }
    }

    private void handleDoubleTapOnHome() {
        if (this.mDoubleTapOnHomeBehavior != 1) {
            return;
        }
        this.mHomeConsumed = true;
        toggleRecentApps();
    }

    private void showTvPictureInPictureMenu(KeyEvent event) {
        if (DEBUG_INPUT) {
            Log.d(TAG, "showTvPictureInPictureMenu event=" + event);
        }
        this.mHandler.removeMessages(17);
        Message msg = this.mHandler.obtainMessage(17);
        msg.setAsynchronous(true);
        msg.sendToTarget();
    }

    private void showTvPictureInPictureMenuInternal() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.showTvPictureInPictureMenu();
    }

    private boolean isRoundWindow() {
        return this.mContext.getResources().getConfiguration().isScreenRound();
    }

    public void init(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) throws Throwable {
        int minHorizontal;
        int maxHorizontal;
        int minVertical;
        int maxVertical;
        int maxRadius;
        boolean z;
        this.mContext = context;
        this.mWindowManager = windowManager;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mHasFeatureWatch = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        boolean burnInProtectionEnabled = context.getResources().getBoolean(R.^attr-private.needsDefaultBackgrounds);
        boolean burnInProtectionDevMode = SystemProperties.getBoolean("persist.debug.force_burn_in", false);
        if (burnInProtectionEnabled || burnInProtectionDevMode) {
            if (burnInProtectionDevMode) {
                minHorizontal = -8;
                maxHorizontal = 8;
                minVertical = -8;
                maxVertical = -4;
                maxRadius = isRoundWindow() ? 6 : -1;
            } else {
                Resources resources = context.getResources();
                minHorizontal = resources.getInteger(R.integer.config_dropboxLowPriorityBroadcastRateLimitPeriod);
                maxHorizontal = resources.getInteger(R.integer.config_dynamicPowerSavingsDefaultDisableThreshold);
                minVertical = resources.getInteger(R.integer.config_emergency_call_wait_for_connection_timeout_millis);
                maxVertical = resources.getInteger(R.integer.config_esim_bootstrap_data_limit_bytes);
                maxRadius = resources.getInteger(R.integer.config_dreamsBatteryLevelMinimumWhenPowered);
            }
            this.mBurnInProtectionHelper = new BurnInProtectionHelper(context, minHorizontal, maxHorizontal, minVertical, maxVertical, maxRadius);
        }
        this.mHandler = new PolicyHandler(this, null);
        this.mWakeGestureListener = new MyWakeGestureListener(this.mContext, this.mHandler);
        this.mOrientationListener = new MyOrientationListener(this.mContext, this.mHandler);
        try {
            this.mOrientationListener.setCurrentRotation(windowManager.getRotation());
        } catch (RemoteException e) {
        }
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
        this.mShortcutManager = new ShortcutManager(context);
        this.mUiMode = context.getResources().getInteger(R.integer.config_chooser_max_targets_per_row);
        this.mHomeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mHomeIntent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270532608);
        this.mEnableCarDockHomeCapture = context.getResources().getBoolean(R.^attr-private.dialogTitleIconsDecorLayout);
        this.mCarDockIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mCarDockIntent.addCategory("android.intent.category.CAR_DOCK");
        this.mCarDockIntent.addFlags(270532608);
        this.mDeskDockIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mDeskDockIntent.addCategory("android.intent.category.DESK_DOCK");
        this.mDeskDockIntent.addFlags(270532608);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mBroadcastWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mBroadcastWakeLock");
        this.mPowerKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mPowerKeyWakeLock");
        this.mEnableShiftMenuBugReports = "1".equals(SystemProperties.get("ro.debuggable"));
        this.mSupportAutoRotation = this.mContext.getResources().getBoolean(R.^attr-private.cornerRadius);
        this.mLidOpenRotation = readRotation(R.integer.config_burnInProtectionMinHorizontalOffset);
        this.mCarDockRotation = readRotation(R.integer.config_carDockKeepsScreenOn);
        this.mDeskDockRotation = readRotation(R.integer.config_cameraLiftTriggerSensorType);
        this.mUndockedHdmiRotation = readRotation(R.integer.config_cdma_3waycall_flash_delay);
        this.mCarDockEnablesAccelerometer = this.mContext.getResources().getBoolean(R.^attr-private.dialogTitleDecorLayout);
        this.mDeskDockEnablesAccelerometer = this.mContext.getResources().getBoolean(R.^attr-private.dialogMode);
        this.mLidKeyboardAccessibility = this.mContext.getResources().getInteger(R.integer.config_burnInProtectionMinVerticalOffset);
        this.mLidNavigationAccessibility = this.mContext.getResources().getInteger(R.integer.config_cameraLaunchGestureSensorType);
        this.mLidControlsScreenLock = this.mContext.getResources().getBoolean(R.^attr-private.defaultQueryHint);
        this.mLidControlsSleep = this.mContext.getResources().getBoolean(R.^attr-private.dialogCustomTitleDecorLayout);
        this.mTranslucentDecorEnabled = this.mContext.getResources().getBoolean(R.^attr-private.emulated);
        this.mAllowTheaterModeWakeFromKey = this.mContext.getResources().getBoolean(R.^attr-private.colorPopupBackground);
        if (this.mAllowTheaterModeWakeFromKey) {
            z = true;
        } else {
            z = this.mContext.getResources().getBoolean(R.^attr-private.colorListDivider);
        }
        this.mAllowTheaterModeWakeFromPowerKey = z;
        this.mAllowTheaterModeWakeFromMotion = this.mContext.getResources().getBoolean(R.^attr-private.colorProgressBackgroundNormal);
        this.mAllowTheaterModeWakeFromMotionWhenNotDreaming = this.mContext.getResources().getBoolean(R.^attr-private.colorSurface);
        this.mAllowTheaterModeWakeFromCameraLens = this.mContext.getResources().getBoolean(R.^attr-private.colorAccentTertiaryVariant);
        this.mAllowTheaterModeWakeFromLidSwitch = this.mContext.getResources().getBoolean(R.^attr-private.colorSurfaceHeader);
        this.mAllowTheaterModeWakeFromWakeGesture = this.mContext.getResources().getBoolean(R.^attr-private.colorAccentTertiary);
        this.mGoToSleepOnButtonPressTheaterMode = this.mContext.getResources().getBoolean(R.^attr-private.colorSwitchThumbNormal);
        this.mSupportLongPressPowerWhenNonInteractive = this.mContext.getResources().getBoolean(R.^attr-private.controllerType);
        this.mLongPressOnBackBehavior = this.mContext.getResources().getInteger(R.integer.config_customizedMaxCachedProcesses);
        this.mShortPressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_datagram_wait_for_connected_state_for_last_message_timeout_millis);
        this.mLongPressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_cursorWindowSize);
        this.mDoublePressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_datagram_wait_for_connected_state_timeout_millis);
        this.mTriplePressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_datause_notification_type);
        this.mShortPressOnSleepBehavior = this.mContext.getResources().getInteger(R.integer.config_datause_polling_period_sec);
        this.mUseTvRouting = AudioSystem.getPlatformType(this.mContext) == 2;
        readConfigurationDependentBehaviors();
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        IntentFilter filter = new IntentFilter();
        filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_ENTER_DESK_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_DESK_MODE);
        filter.addAction("android.intent.action.DOCK_EVENT");
        Intent intent = context.registerReceiver(this.mDockReceiver, filter);
        if (intent != null) {
            this.mDockMode = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
        }
        IntentFilter ipoEventFilter = new IntentFilter();
        ipoEventFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        ipoEventFilter.addAction("android.intent.action.ACTION_BOOT_IPO");
        context.registerReceiver(this.mIpoEventReceiver, ipoEventFilter);
        IntentFilter poweroffAlarmFilter = new IntentFilter();
        poweroffAlarmFilter.addAction(NORMAL_SHUTDOWN_ACTION);
        poweroffAlarmFilter.addAction(NORMAL_BOOT_ACTION);
        context.registerReceiver(this.mPoweroffAlarmReceiver, poweroffAlarmFilter);
        IntentFilter stkUserActivityFilter = new IntentFilter();
        stkUserActivityFilter.addAction(STK_USERACTIVITY_ENABLE);
        context.registerReceiver(this.mStkUserActivityEnReceiver, stkUserActivityFilter);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.DREAMING_STARTED");
        filter2.addAction("android.intent.action.DREAMING_STOPPED");
        context.registerReceiver(this.mDreamReceiver, filter2);
        context.registerReceiver(this.mMultiuserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        this.mSystemGestures = new SystemGesturesPointerEventListener(context, new SystemGesturesPointerEventListener.Callbacks() {
            @Override
            public void onSwipeFromTop() {
                if (isGestureIsolated() || PhoneWindowManager.this.mStatusBar == null) {
                    return;
                }
                PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mStatusBar);
            }

            @Override
            public void onSwipeFromBottom() {
                if (isGestureIsolated() || PhoneWindowManager.this.mNavigationBar == null || !PhoneWindowManager.this.mNavigationBarOnBottom || PhoneWindowManager.this.mHideNavigationBar != 0) {
                    return;
                }
                PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
            }

            @Override
            public void onSwipeFromRight() {
                if (isGestureIsolated() || PhoneWindowManager.this.mNavigationBar == null || PhoneWindowManager.this.mNavigationBarOnBottom || PhoneWindowManager.this.mHideNavigationBar != 0) {
                    return;
                }
                PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
            }

            @Override
            public void onFling(int duration) {
                if (PhoneWindowManager.this.mPowerManagerInternal == null) {
                    return;
                }
                PhoneWindowManager.this.mPowerManagerInternal.powerHint(2, duration);
            }

            @Override
            public void onDebug() {
            }

            @Override
            public void onDown() {
                PhoneWindowManager.this.mOrientationListener.onTouchStart();
            }

            @Override
            public void onUpOrCancel() {
                PhoneWindowManager.this.mOrientationListener.onTouchEnd();
            }

            @Override
            public void onMouseHoverAtTop() {
                if (isGestureIsolated()) {
                    return;
                }
                PhoneWindowManager.this.mHandler.removeMessages(16);
                Message msg = PhoneWindowManager.this.mHandler.obtainMessage(16);
                msg.arg1 = 0;
                PhoneWindowManager.this.mHandler.sendMessageDelayed(msg, 500L);
            }

            @Override
            public void onMouseHoverAtBottom() {
                if (isGestureIsolated()) {
                    return;
                }
                PhoneWindowManager.this.mHandler.removeMessages(16);
                Message msg = PhoneWindowManager.this.mHandler.obtainMessage(16);
                msg.arg1 = 1;
                PhoneWindowManager.this.mHandler.sendMessageDelayed(msg, 500L);
            }

            @Override
            public void onMouseLeaveFromEdge() {
                PhoneWindowManager.this.mHandler.removeMessages(16);
            }

            private boolean isGestureIsolated() {
                WindowManagerPolicy.WindowState win = PhoneWindowManager.this.mFocusedWindow != null ? PhoneWindowManager.this.mFocusedWindow : PhoneWindowManager.this.mTopFullscreenOpaqueWindowState;
                return (win == null || (win.getSystemUiVisibility() & 16777216) == 0) ? false : true;
            }
        });
        this.mImmersiveModeConfirmation = new ImmersiveModeConfirmation(this.mContext);
        this.mWindowManagerFuncs.registerPointerEventListener(this.mSystemGestures);
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mLongPressVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_availableEMValueOptions);
        this.mVirtualKeyVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_backGestureInsetScales);
        this.mKeyboardTapVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_backupHealthConnectDataAndSettingsKnownSigners);
        this.mClockTickVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_batteryPackageTypeService);
        this.mCalendarDateVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_batteryPackageTypeSystem);
        this.mSafeModeDisabledVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_bg_current_drain_high_threshold_to_bg_restricted);
        this.mSafeModeEnabledVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_bg_current_drain_high_threshold_to_restricted_bucket);
        this.mContextClickVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_bg_current_drain_threshold_to_restricted_bucket);
        this.mScreenshotChordEnabled = this.mContext.getResources().getBoolean(R.^attr-private.colorAccentSecondary);
        this.mGlobalKeyManager = new GlobalKeyManager(this.mContext);
        initializeHdmiState();
        if (!this.mPowerManager.isInteractive()) {
            startedGoingToSleep(2);
            finishedGoingToSleep(2);
        }
        this.mWindowManagerInternal.registerAppTransitionListener(this.mStatusBarController.getAppTransitionListener());
    }

    private void readConfigurationDependentBehaviors() {
        Resources res = this.mContext.getResources();
        this.mLongPressOnHomeBehavior = res.getInteger(R.integer.config_defaultMediaVibrationIntensity);
        if (this.mLongPressOnHomeBehavior < 0 || this.mLongPressOnHomeBehavior > 2) {
            this.mLongPressOnHomeBehavior = 0;
        }
        this.mDoubleTapOnHomeBehavior = res.getInteger(R.integer.config_defaultMinEmergencyGestureTapDurationMillis);
        if (this.mDoubleTapOnHomeBehavior < 0 || this.mDoubleTapOnHomeBehavior > 1) {
            this.mDoubleTapOnHomeBehavior = 0;
        }
        this.mShortPressWindowBehavior = 0;
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.picture_in_picture")) {
            this.mShortPressWindowBehavior = 1;
        }
        this.mNavBarOpacityMode = res.getInteger(R.integer.config_extraFreeKbytesAdjust);
    }

    public void setInitialDisplaySize(Display display, int width, int height, int density) {
        int shortSize;
        int longSize;
        if (this.mContext == null || display.getDisplayId() != 0) {
            return;
        }
        this.mDisplay = display;
        Resources res = this.mContext.getResources();
        if (width > height) {
            shortSize = height;
            longSize = width;
            this.mLandscapeRotation = 0;
            this.mSeascapeRotation = 2;
            if (res.getBoolean(R.^attr-private.daySelectorColor)) {
                this.mPortraitRotation = 1;
                this.mUpsideDownRotation = 3;
            } else {
                this.mPortraitRotation = 3;
                this.mUpsideDownRotation = 1;
            }
        } else {
            shortSize = width;
            longSize = height;
            this.mPortraitRotation = 0;
            this.mUpsideDownRotation = 2;
            if (res.getBoolean(R.^attr-private.daySelectorColor)) {
                this.mLandscapeRotation = 3;
                this.mSeascapeRotation = 1;
            } else {
                this.mLandscapeRotation = 1;
                this.mSeascapeRotation = 3;
            }
        }
        int shortSizeDp = (shortSize * 160) / density;
        int longSizeDp = (longSize * 160) / density;
        this.mNavigationBarCanMove = width != height && shortSizeDp < 600;
        this.mHasNavigationBar = res.getBoolean(R.^attr-private.hideWheelUntilFocused);
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            this.mHasNavigationBar = false;
        } else if ("0".equals(navBarOverride)) {
            this.mHasNavigationBar = true;
        }
        if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
            this.mDemoHdmiRotation = this.mPortraitRotation;
        } else {
            this.mDemoHdmiRotation = this.mLandscapeRotation;
        }
        this.mDemoHdmiRotationLock = SystemProperties.getBoolean("persist.demo.hdmirotationlock", false);
        if ("portrait".equals(SystemProperties.get("persist.demo.remoterotation"))) {
            this.mDemoRotation = this.mPortraitRotation;
        } else {
            this.mDemoRotation = this.mLandscapeRotation;
        }
        this.mDemoRotationLock = SystemProperties.getBoolean("persist.demo.rotationlock", false);
        boolean z = longSizeDp >= 960 && shortSizeDp >= 720 && res.getBoolean(R.^attr-private.legacyLayout) && !"true".equals(SystemProperties.get("config.override_forced_orient"));
        this.mForceDefaultOrientation = z;
    }

    private boolean canHideNavigationBar() {
        return this.mHasNavigationBar;
    }

    public boolean isDefaultOrientationForced() {
        return this.mForceDefaultOrientation;
    }

    public void setDisplayOverscan(Display display, int left, int top, int right, int bottom) {
        if (display.getDisplayId() != 0) {
            return;
        }
        this.mOverscanLeft = left;
        this.mOverscanTop = top;
        this.mOverscanRight = right;
        this.mOverscanBottom = bottom;
    }

    public void updateSettings() {
        int userRotationMode;
        int pointerLocation;
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean updateRotation = false;
        synchronized (this.mLock) {
            this.mEndcallBehavior = Settings.System.getIntForUser(resolver, "end_button_behavior", 2, -2);
            this.mIncallPowerBehavior = Settings.Secure.getIntForUser(resolver, "incall_power_button_behavior", 1, -2);
            boolean wakeGestureEnabledSetting = Settings.Secure.getIntForUser(resolver, "wake_gesture_enabled", 0, -2) != 0;
            if (this.mWakeGestureEnabledSetting != wakeGestureEnabledSetting) {
                this.mWakeGestureEnabledSetting = wakeGestureEnabledSetting;
                updateWakeGestureListenerLp();
            }
            int userRotation = Settings.System.getIntForUser(resolver, "user_rotation", 0, -2);
            if (this.mUserRotation != userRotation) {
                this.mUserRotation = userRotation;
                updateRotation = true;
            }
            if (Settings.System.getIntForUser(resolver, "accelerometer_rotation", 0, -2) != 0) {
                userRotationMode = 0;
            } else {
                userRotationMode = 1;
            }
            if (this.mUserRotationMode != userRotationMode) {
                this.mUserRotationMode = userRotationMode;
                updateRotation = true;
                updateOrientationListenerLp();
            }
            if (this.mSystemReady && this.mPointerLocationMode != (pointerLocation = Settings.System.getIntForUser(resolver, "pointer_location", 0, -2))) {
                this.mPointerLocationMode = pointerLocation;
                this.mHandler.sendEmptyMessage(pointerLocation != 0 ? 1 : 2);
            }
            this.mLockScreenTimeout = Settings.System.getIntForUser(resolver, "screen_off_timeout", 0, -2);
            String imId = Settings.Secure.getStringForUser(resolver, "default_input_method", -2);
            boolean hasSoftInput = imId != null && imId.length() > 0;
            if (this.mHasSoftInput != hasSoftInput) {
                this.mHasSoftInput = hasSoftInput;
                updateRotation = true;
            }
            int hideNavigationBar = Settings.System.getIntForUser(resolver, "hide_navigation_bar", 0, -2);
            if (hideNavigationBar == 1) {
                int forceShowNavigationBar = 1;
                if (Settings.System.getIntForUser(resolver, "dcha_state", 0, -2) != 3 || Settings.Global.getInt(resolver, "adb_enabled", 0) != 1) {
                    forceShowNavigationBar = 0;
                } else {
                    try {
                        this.mContext.getPackageManager().getPackageInfo("android.deviceadmin.cts", 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        forceShowNavigationBar = 0;
                    }
                }
                if (forceShowNavigationBar == 1) {
                    hideNavigationBar = 0;
                }
            }
            if (this.mHideNavigationBar != hideNavigationBar) {
                this.mHideNavigationBar = hideNavigationBar;
                updateRotation = true;
            }
            if (this.mImmersiveModeConfirmation != null) {
                this.mImmersiveModeConfirmation.loadSetting(this.mCurrentUserId);
            }
        }
        synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            PolicyControl.reloadFromSetting(this.mContext);
        }
        if (!updateRotation) {
            return;
        }
        updateRotation(true);
    }

    private void updateWakeGestureListenerLp() {
        if (shouldEnableWakeGestureLp()) {
            this.mWakeGestureListener.requestWakeUpTrigger();
        } else {
            this.mWakeGestureListener.cancelWakeUpTrigger();
        }
    }

    private boolean shouldEnableWakeGestureLp() {
        if (!this.mWakeGestureEnabledSetting || this.mAwake) {
            return false;
        }
        if (this.mLidControlsSleep && this.mLidState == 0) {
            return false;
        }
        return this.mWakeGestureListener.isSupported();
    }

    private void enablePointerLocation() {
        if (this.mPointerLocationView != null) {
            return;
        }
        this.mPointerLocationView = new PointerLocationView(this.mContext);
        this.mPointerLocationView.setPrintCoords(false);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1);
        lp.type = 2015;
        lp.flags = 1304;
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
            lp.privateFlags |= 2;
        }
        lp.format = -3;
        lp.setTitle("PointerLocation");
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        lp.inputFeatures |= 2;
        wm.addView(this.mPointerLocationView, lp);
        this.mWindowManagerFuncs.registerPointerEventListener(this.mPointerLocationView);
    }

    private void disablePointerLocation() {
        if (this.mPointerLocationView == null) {
            return;
        }
        this.mWindowManagerFuncs.unregisterPointerEventListener(this.mPointerLocationView);
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        wm.removeView(this.mPointerLocationView);
        this.mPointerLocationView = null;
    }

    private int readRotation(int resID) {
        try {
            int rotation = this.mContext.getResources().getInteger(resID);
            switch (rotation) {
                case 0:
                    return 0;
                case 90:
                    return 1;
                case 180:
                    return 2;
                case 270:
                    return 3;
                default:
                    return -1;
            }
        } catch (Resources.NotFoundException e) {
            return -1;
        }
    }

    public int checkAddPermission(WindowManager.LayoutParams attrs, int[] outAppOp) {
        ApplicationInfo appInfo;
        int type = attrs.type;
        outAppOp[0] = -1;
        if ((type < 1 || type > 99) && ((type < 1000 || type > 1999) && (type < 2000 || type > 2999))) {
            return -10;
        }
        if (type < 2000 || type > 2999) {
            return 0;
        }
        String permission = null;
        switch (type) {
            case 2002:
            case 2003:
            case 2006:
            case 2007:
            case 2010:
                permission = "android.permission.SYSTEM_ALERT_WINDOW";
                outAppOp[0] = 24;
                break;
            case 2005:
                outAppOp[0] = 45;
                break;
            case 2011:
            case 2013:
            case 2023:
            case 2030:
            case 2031:
            case 2032:
            case 2035:
            case 2037:
                break;
            default:
                permission = "android.permission.INTERNAL_SYSTEM_WINDOW";
                break;
        }
        if (permission != null) {
            if ("android.permission.SYSTEM_ALERT_WINDOW".equals(permission)) {
                int callingUid = Binder.getCallingUid();
                if (callingUid == 1000) {
                    return 0;
                }
                int mode = this.mAppOpsManager.checkOpNoThrow(outAppOp[0], callingUid, attrs.packageName);
                switch (mode) {
                    case 0:
                    case 1:
                        return 0;
                    case 2:
                        try {
                            appInfo = this.mContext.getPackageManager().getApplicationInfo(attrs.packageName, UserHandle.getUserId(callingUid));
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                        return appInfo.targetSdkVersion < 23 ? 0 : -8;
                    default:
                        return this.mContext.checkCallingPermission(permission) != 0 ? -8 : 0;
                }
            }
            if (this.mContext.checkCallingOrSelfPermission(permission) != 0) {
                return -8;
            }
        }
        return 0;
    }

    public boolean checkShowToOwnerOnly(WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case 3:
            case 2000:
            case 2001:
            case 2002:
            case 2007:
            case 2008:
            case 2009:
            case 2014:
            case 2017:
            case 2018:
            case 2019:
            case 2020:
            case 2021:
            case 2022:
            case 2024:
            case 2026:
            case 2027:
            case 2029:
            case 2030:
            case 2034:
                break;
            default:
                if ((attrs.privateFlags & 16) == 0) {
                    return true;
                }
                break;
        }
        return this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0;
    }

    public void adjustWindowParamsLw(WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case 2000:
                if (this.mKeyguardHidden) {
                    attrs.flags &= -1048577;
                    attrs.privateFlags &= -1025;
                }
                break;
            case 2006:
            case 2015:
                attrs.flags |= 24;
                attrs.flags &= -262145;
                break;
        }
        if (attrs.type != 2000) {
            attrs.privateFlags &= -1025;
        }
        if (!ActivityManager.isHighEndGfx()) {
            return;
        }
        if ((attrs.flags & Integer.MIN_VALUE) != 0) {
            attrs.subtreeSystemUiVisibility |= 512;
        }
        boolean forceWindowDrawsStatusBarBackground = (attrs.privateFlags & PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0;
        if ((attrs.flags & Integer.MIN_VALUE) == 0 && (!forceWindowDrawsStatusBarBackground || attrs.height != -1 || attrs.width != -1)) {
            return;
        }
        attrs.subtreeSystemUiVisibility |= 1024;
    }

    void readLidState() {
        this.mLidState = this.mWindowManagerFuncs.getLidState();
    }

    private void readCameraLensCoverState() {
        this.mCameraLensCoverState = this.mWindowManagerFuncs.getCameraLensCoverState();
    }

    private boolean isHidden(int accessibilityMode) {
        switch (accessibilityMode) {
            case 1:
                return this.mLidState == 0;
            case 2:
                return this.mLidState == 1;
            default:
                return false;
        }
    }

    public void adjustConfigurationLw(Configuration config, int keyboardPresence, int navigationPresence) {
        this.mHaveBuiltInKeyboard = (keyboardPresence & 1) != 0;
        readConfigurationDependentBehaviors();
        readLidState();
        if (config.keyboard == 1 || (keyboardPresence == 1 && isHidden(this.mLidKeyboardAccessibility))) {
            config.hardKeyboardHidden = 2;
            if (!this.mHasSoftInput) {
                config.keyboardHidden = 2;
            }
        }
        if (config.navigation != 1 && (navigationPresence != 1 || !isHidden(this.mLidNavigationAccessibility))) {
            return;
        }
        config.navigationHidden = 2;
    }

    public void onConfigurationChanged() {
        Resources res = this.mContext.getResources();
        this.mStatusBarHeight = res.getDimensionPixelSize(R.dimen.accessibility_touch_slop);
        int[] iArr = this.mNavigationBarHeightForRotationDefault;
        int i = this.mPortraitRotation;
        int dimensionPixelSize = res.getDimensionPixelSize(R.dimen.accessibility_window_magnifier_min_size);
        this.mNavigationBarHeightForRotationDefault[this.mUpsideDownRotation] = dimensionPixelSize;
        iArr[i] = dimensionPixelSize;
        int[] iArr2 = this.mNavigationBarHeightForRotationDefault;
        int i2 = this.mLandscapeRotation;
        int dimensionPixelSize2 = res.getDimensionPixelSize(R.dimen.action_bar_button_margin);
        this.mNavigationBarHeightForRotationDefault[this.mSeascapeRotation] = dimensionPixelSize2;
        iArr2[i2] = dimensionPixelSize2;
        int[] iArr3 = this.mNavigationBarWidthForRotationDefault;
        int i3 = this.mPortraitRotation;
        int dimensionPixelSize3 = res.getDimensionPixelSize(R.dimen.action_bar_button_max_width);
        this.mNavigationBarWidthForRotationDefault[this.mSeascapeRotation] = dimensionPixelSize3;
        this.mNavigationBarWidthForRotationDefault[this.mLandscapeRotation] = dimensionPixelSize3;
        this.mNavigationBarWidthForRotationDefault[this.mUpsideDownRotation] = dimensionPixelSize3;
        iArr3[i3] = dimensionPixelSize3;
        int[] iArr4 = this.mNavigationBarHeightForRotationInCarMode;
        int i4 = this.mPortraitRotation;
        int dimensionPixelSize4 = res.getDimensionPixelSize(R.dimen.action_bar_content_inset_material);
        this.mNavigationBarHeightForRotationInCarMode[this.mUpsideDownRotation] = dimensionPixelSize4;
        iArr4[i4] = dimensionPixelSize4;
        int[] iArr5 = this.mNavigationBarHeightForRotationInCarMode;
        int i5 = this.mLandscapeRotation;
        int dimensionPixelSize5 = res.getDimensionPixelSize(R.dimen.action_bar_content_inset_with_nav);
        this.mNavigationBarHeightForRotationInCarMode[this.mSeascapeRotation] = dimensionPixelSize5;
        iArr5[i5] = dimensionPixelSize5;
        int[] iArr6 = this.mNavigationBarWidthForRotationInCarMode;
        int i6 = this.mPortraitRotation;
        int dimensionPixelSize6 = res.getDimensionPixelSize(R.dimen.action_bar_default_height);
        this.mNavigationBarWidthForRotationInCarMode[this.mSeascapeRotation] = dimensionPixelSize6;
        this.mNavigationBarWidthForRotationInCarMode[this.mLandscapeRotation] = dimensionPixelSize6;
        this.mNavigationBarWidthForRotationInCarMode[this.mUpsideDownRotation] = dimensionPixelSize6;
        iArr6[i6] = dimensionPixelSize6;
    }

    public int windowTypeToLayerLw(int type) {
        if (type >= 1 && type <= 99) {
            return 2;
        }
        switch (type) {
            case 2000:
                break;
            case 2001:
            case 2033:
                break;
            case 2002:
                break;
            case 2003:
                break;
            case 2004:
            case 2025:
            case 2028:
            default:
                Log.e(TAG, "Unknown window type: " + type);
                break;
            case 2005:
                break;
            case 2006:
                break;
            case 2007:
                break;
            case 2008:
                break;
            case 2009:
                break;
            case 2010:
                break;
            case 2011:
                break;
            case 2012:
                break;
            case 2013:
                break;
            case 2014:
                break;
            case 2015:
                break;
            case 2016:
                break;
            case 2017:
                break;
            case 2018:
                break;
            case 2019:
                break;
            case 2020:
                break;
            case 2021:
                break;
            case 2022:
                break;
            case 2023:
                break;
            case 2024:
                break;
            case 2026:
                break;
            case 2027:
                break;
            case 2029:
                break;
            case 2030:
                break;
            case 2031:
                break;
            case 2032:
                break;
            case 2034:
                break;
            case 2035:
                break;
            case 2036:
                break;
            case 2037:
                break;
        }
        return 2;
    }

    public int subWindowTypeToLayerLw(int type) {
        switch (type) {
            case 1000:
            case 1003:
                return 1;
            case ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG:
                return -2;
            case ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG:
                return 2;
            case 1004:
                return -1;
            case 1005:
                return 3;
            default:
                Log.e(TAG, "Unknown sub-window type: " + type);
                return 0;
        }
    }

    public int getMaxWallpaperLayer() {
        return windowTypeToLayerLw(2000);
    }

    private int getNavigationBarWidth(int rotation, int uiMode) {
        if ((uiMode & 15) == 3) {
            return this.mNavigationBarWidthForRotationInCarMode[rotation];
        }
        return this.mNavigationBarWidthForRotationDefault[rotation];
    }

    public int getNonDecorDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode) {
        if (this.mHasNavigationBar && this.mNavigationBarCanMove && fullWidth > fullHeight) {
            return fullWidth - getNavigationBarWidth(rotation, uiMode);
        }
        return fullWidth;
    }

    private int getNavigationBarHeight(int rotation, int uiMode) {
        if ((uiMode & 15) == 3) {
            return this.mNavigationBarHeightForRotationInCarMode[rotation];
        }
        return this.mNavigationBarHeightForRotationDefault[rotation];
    }

    public int getNonDecorDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode) {
        if (this.mHasNavigationBar && (!this.mNavigationBarCanMove || fullWidth < fullHeight)) {
            return fullHeight - getNavigationBarHeight(rotation, uiMode);
        }
        return fullHeight;
    }

    public int getConfigDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode) {
        return getNonDecorDisplayWidth(fullWidth, fullHeight, rotation, uiMode);
    }

    public int getConfigDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode) {
        return getNonDecorDisplayHeight(fullWidth, fullHeight, rotation, uiMode) - this.mStatusBarHeight;
    }

    public boolean isForceHiding(WindowManager.LayoutParams attrs) {
        if ((attrs.privateFlags & 1024) == 0) {
            return (isKeyguardHostWindow(attrs) && isKeyguardShowingAndNotOccluded()) || attrs.type == 2029;
        }
        return true;
    }

    public boolean isKeyguardHostWindow(WindowManager.LayoutParams attrs) {
        return attrs.type == 2000;
    }

    public boolean canBeForceHidden(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case 2000:
            case 2013:
            case 2019:
            case 2023:
            case 2029:
                break;
            default:
                if (windowTypeToLayerLw(win.getBaseType()) < windowTypeToLayerLw(2000)) {
                }
                break;
        }
        return false;
    }

    public WindowManagerPolicy.WindowState getWinShowWhenLockedLw() {
        return this.mWinShowWhenLocked;
    }

    public View addStartingWindow(IBinder appToken, String packageName, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, Configuration overrideConfig) {
        if (packageName == null) {
            return null;
        }
        WindowManager windowManager = null;
        View view = null;
        try {
            try {
                Context context = this.mContext;
                if (DEBUG_STARTING_WINDOW) {
                    Slog.d(TAG, "addStartingWindow " + packageName + ": nonLocalizedLabel=" + nonLocalizedLabel + " theme=" + Integer.toHexString(theme));
                }
                if (theme != context.getThemeResId() || labelRes != 0) {
                    try {
                        context = context.createPackageContext(packageName, 0);
                        context.setTheme(theme);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                if (overrideConfig != null && overrideConfig != Configuration.EMPTY) {
                    if (DEBUG_STARTING_WINDOW) {
                        Slog.d(TAG, "addStartingWindow: creating context based on overrideConfig" + overrideConfig + " for starting window");
                    }
                    Context overrideContext = context.createConfigurationContext(overrideConfig);
                    overrideContext.setTheme(theme);
                    TypedArray typedArray = overrideContext.obtainStyledAttributes(com.android.internal.R.styleable.Window);
                    int resId = typedArray.getResourceId(1, 0);
                    if (resId != 0 && overrideContext.getDrawable(resId) != null) {
                        if (DEBUG_STARTING_WINDOW) {
                            Slog.d(TAG, "addStartingWindow: apply overrideConfig" + overrideConfig + " to starting window resId=" + resId);
                        }
                        context = overrideContext;
                    }
                }
                PhoneWindow win = new PhoneWindow(context);
                win.setIsStartingWindow(true);
                CharSequence label = context.getResources().getText(labelRes, null);
                if (label != null) {
                    win.setTitle(label, true);
                } else {
                    win.setTitle(nonLocalizedLabel, false);
                }
                win.setType(3);
                synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
                    if (this.mKeyguardHidden) {
                        windowFlags |= PackageManagerService.DumpState.DUMP_FROZEN;
                    }
                }
                win.setFlags(windowFlags | 16 | 8 | PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS, windowFlags | 16 | 8 | PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS);
                win.setDefaultIcon(icon);
                win.setDefaultLogo(logo);
                win.setLayout(-1, -1);
                WindowManager.LayoutParams params = win.getAttributes();
                params.token = appToken;
                params.packageName = packageName;
                params.windowAnimations = win.getWindowStyle().getResourceId(8, 0);
                params.privateFlags |= 1;
                params.privateFlags |= 16;
                if (!compatInfo.supportsScreen()) {
                    params.privateFlags |= 128;
                }
                params.setTitle("Starting " + packageName);
                WindowManager wm = (WindowManager) context.getSystemService("window");
                View view2 = win.getDecorView();
                if (DEBUG_STARTING_WINDOW) {
                    Slog.d(TAG, "Adding starting window for " + packageName + " / " + appToken + ": " + (view2.getParent() != null ? view2 : null));
                }
                wm.addView(view2, params);
                if (this.mAppLaunchTimeEnabled) {
                    WindowManagerGlobal.getInstance().doTraversal(view2, true);
                }
                View view3 = view2.getParent() != null ? view2 : null;
                if (view2 != null && view2.getParent() == null) {
                    Log.w(TAG, "view not successfully added to wm, removing view");
                    wm.removeViewImmediate(view2);
                }
                return view3;
            } catch (WindowManager.BadTokenException e2) {
                Log.w(TAG, appToken + " already running, starting window not displayed. " + e2.getMessage());
                if (0 == 0 || view.getParent() != null) {
                    return null;
                }
                Log.w(TAG, "view not successfully added to wm, removing view");
                windowManager.removeViewImmediate(null);
                return null;
            } catch (RuntimeException e3) {
                Log.w(TAG, appToken + " failed creating starting window", e3);
                if (0 == 0 || view.getParent() != null) {
                    return null;
                }
                Log.w(TAG, "view not successfully added to wm, removing view");
                windowManager.removeViewImmediate(null);
                return null;
            }
        } catch (Throwable th) {
            if (0 != 0 && view.getParent() == null) {
                Log.w(TAG, "view not successfully added to wm, removing view");
                windowManager.removeViewImmediate(null);
            }
            throw th;
        }
    }

    public void removeStartingWindow(IBinder appToken, View window) {
        if (DEBUG_STARTING_WINDOW) {
            Slog.v(TAG, "Removing starting window for " + appToken + ": " + window + " Callers=" + Debug.getCallers(4));
        }
        if (window == null) {
            return;
        }
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        wm.removeView(window);
    }

    public int prepareAddWindowLw(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case 2000:
                this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
                if (this.mStatusBar != null && this.mStatusBar.isAlive()) {
                    return -7;
                }
                this.mStatusBar = win;
                this.mStatusBarController.setWindow(win);
                return 0;
            case 2014:
            case 2017:
            case 2024:
            case 2033:
                this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
                return 0;
            case 2019:
                this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
                if (this.mNavigationBar != null && this.mNavigationBar.isAlive()) {
                    return -7;
                }
                this.mNavigationBar = win;
                this.mNavigationBarController.setWindow(win);
                if (DEBUG_LAYOUT) {
                    Slog.i(TAG, "NAVIGATION BAR: " + this.mNavigationBar);
                    return 0;
                }
                return 0;
            case 2029:
                if (this.mKeyguardScrim != null) {
                    return -7;
                }
                this.mKeyguardScrim = win;
                return 0;
            default:
                return 0;
        }
    }

    public void removeWindowLw(WindowManagerPolicy.WindowState win) {
        if (this.mStatusBar == win) {
            this.mStatusBar = null;
            this.mStatusBarController.setWindow(null);
            this.mKeyguardDelegate.showScrim();
        } else if (this.mKeyguardScrim == win) {
            Log.v(TAG, "Removing keyguard scrim");
            this.mKeyguardScrim = null;
        }
        if (this.mNavigationBar != win) {
            return;
        }
        this.mNavigationBar = null;
        this.mNavigationBarController.setWindow(null);
    }

    public int selectAnimationLw(WindowManagerPolicy.WindowState win, int transit) {
        if (win == this.mStatusBar) {
            boolean isKeyguard = (win.getAttrs().privateFlags & 1024) != 0;
            if (transit == 2 || transit == 4) {
                if (isKeyguard) {
                    return -1;
                }
                return R.anim.fast_fade_out;
            }
            if (transit == 1 || transit == 3) {
                if (isKeyguard) {
                    return -1;
                }
                return R.anim.fast_fade_in;
            }
        } else if (win == this.mNavigationBar) {
            if (win.getAttrs().windowAnimations != 0) {
                return 0;
            }
            if (this.mNavigationBarOnBottom) {
                if (transit == 2 || transit == 4) {
                    return R.anim.dialog_enter;
                }
                if (transit == 1 || transit == 3) {
                    return R.anim.date_picker_fade_out_material;
                }
            } else {
                if (transit == 2 || transit == 4) {
                    return R.anim.dream_activity_open_exit;
                }
                if (transit == 1 || transit == 3) {
                    return R.anim.dream_activity_open_enter;
                }
            }
        } else if (win.getAttrs().type == 2034) {
            return selectDockedDividerAnimationLw(win, transit);
        }
        if (transit == 5) {
            if (win.hasAppShownWindows()) {
                return R.anim.activity_translucent_close_exit;
            }
        } else if (win.getAttrs().type == 2023 && this.mDreamingLockscreen && transit == 1) {
            return -1;
        }
        return 0;
    }

    private int selectDockedDividerAnimationLw(WindowManagerPolicy.WindowState win, int transit) {
        int insets = this.mWindowManagerFuncs.getDockedDividerInsetsLw();
        Rect frame = win.getFrameLw();
        boolean behindNavBar = this.mNavigationBar != null ? (!this.mNavigationBarOnBottom || frame.top + insets < this.mNavigationBar.getFrameLw().top) ? !this.mNavigationBarOnBottom && frame.left + insets >= this.mNavigationBar.getFrameLw().left : true : false;
        boolean landscape = frame.height() > frame.width();
        boolean offscreenLandscape = landscape ? frame.right - insets <= 0 || frame.left + insets >= win.getDisplayFrameLw().right : false;
        boolean offscreenPortrait = !landscape ? frame.top - insets <= 0 || frame.bottom + insets >= win.getDisplayFrameLw().bottom : false;
        boolean z = !offscreenLandscape ? offscreenPortrait : true;
        if (behindNavBar || z) {
            return 0;
        }
        if (transit == 1 || transit == 3) {
            return R.anim.fade_in;
        }
        if (transit == 2) {
            return R.anim.fade_out;
        }
        return 0;
    }

    public void selectRotationAnimationLw(int[] anim) {
        if (this.mTopFullscreenOpaqueWindowState != null && this.mTopIsFullscreen) {
            switch (this.mTopFullscreenOpaqueWindowState.getAttrs().rotationAnimation) {
                case 1:
                    anim[0] = 17432685;
                    anim[1] = 17432683;
                    break;
                case 2:
                    anim[0] = 17432684;
                    anim[1] = 17432683;
                    break;
                default:
                    anim[1] = 0;
                    anim[0] = 0;
                    break;
            }
            return;
        }
        anim[1] = 0;
        anim[0] = 0;
    }

    public boolean validateRotationAnimationLw(int exitAnimId, int enterAnimId, boolean forceDefault) {
        switch (exitAnimId) {
            case R.anim.overlay_task_fragment_close_to_bottom:
            case R.anim.overlay_task_fragment_close_to_left:
                if (forceDefault) {
                    return false;
                }
                int[] anim = new int[2];
                selectRotationAnimationLw(anim);
                return exitAnimId == anim[0] && enterAnimId == anim[1];
            default:
                return true;
        }
    }

    public Animation createForceHideEnterAnimation(boolean onWallpaper, boolean goingToNotificationShade) {
        int i;
        if (goingToNotificationShade) {
            return AnimationUtils.loadAnimation(this.mContext, R.anim.ic_signal_wifi_transient_animation_3);
        }
        Context context = this.mContext;
        if (onWallpaper) {
            i = R.anim.ic_signal_wifi_transient_animation_4;
        } else {
            i = R.anim.ic_signal_wifi_transient_animation_2;
        }
        AnimationSet set = (AnimationSet) AnimationUtils.loadAnimation(context, i);
        List<Animation> animations = set.getAnimations();
        for (int i2 = animations.size() - 1; i2 >= 0; i2--) {
            animations.get(i2).setInterpolator(this.mLogDecelerateInterpolator);
        }
        return set;
    }

    public Animation createForceHideWallpaperExitAnimation(boolean goingToNotificationShade) {
        if (goingToNotificationShade) {
            return null;
        }
        return AnimationUtils.loadAnimation(this.mContext, R.anim.ic_signal_wifi_transient_animation_7);
    }

    private static void awakenDreams() {
        IDreamManager dreamManager = getDreamManager();
        if (dreamManager == null) {
            return;
        }
        try {
            dreamManager.awaken();
        } catch (RemoteException e) {
        }
    }

    static IDreamManager getDreamManager() {
        return IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
    }

    TelecomManager getTelecommService() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    static IAudioService getAudioService() {
        IAudioService audioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
        if (audioService == null) {
            Log.w(TAG, "Unable to find IAudioService interface.");
        }
        return audioService;
    }

    boolean keyguardOn() {
        if (isKeyguardShowingAndNotOccluded()) {
            return true;
        }
        return inKeyguardRestrictedKeyInputMode();
    }

    public long interceptKeyBeforeDispatching(WindowManagerPolicy.WindowState win, KeyEvent event, int policyFlags) {
        Intent voiceIntent;
        IStatusBarService service;
        String category;
        Intent shortcutIntent;
        boolean keyguardOn = keyguardOn();
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        int metaState = event.getMetaState();
        int flags = event.getFlags();
        boolean down = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        if (!IS_USER_BUILD || DEBUG_INPUT) {
            Log.d(TAG, "interceptKeyTi keyCode=" + keyCode + " down=" + down + " repeatCount=" + repeatCount + " keyguardOn=" + keyguardOn + " mHomePressed=" + this.mHomePressed + " canceled=" + canceled + " metaState:" + metaState);
        }
        if (this.mScreenshotChordEnabled && (flags & 1024) == 0) {
            if (this.mScreenshotChordVolumeDownKeyTriggered && !this.mScreenshotChordPowerKeyTriggered) {
                long now = SystemClock.uptimeMillis();
                long timeoutTime = this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (now < timeoutTime) {
                    return timeoutTime - now;
                }
            }
            if (keyCode == 25 && this.mScreenshotChordVolumeDownKeyConsumed) {
                Log.d("screen_capture_on", "keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && mScreenshotChordVolumeDownKeyConsumed");
                if (down) {
                    return -1L;
                }
                this.mScreenshotChordVolumeDownKeyConsumed = false;
                return -1L;
            }
        }
        if (!this.mHasNavigationBar && (flags & 1024) == 0 && keyCode == 4 && down && repeatCount == 1) {
            interceptDismissPinningChord();
        }
        if (this.mPendingMetaAction && !KeyEvent.isMetaKey(keyCode)) {
            this.mPendingMetaAction = false;
        }
        if (this.mPendingCapsLockToggle && !KeyEvent.isMetaKey(keyCode) && !KeyEvent.isAltKey(keyCode)) {
            this.mPendingCapsLockToggle = false;
        }
        if (keyCode == 3) {
            if (!down) {
                cancelPreloadRecentApps();
                this.mHomePressed = false;
                if (this.mHomeConsumed) {
                    this.mHomeConsumed = false;
                    return -1L;
                }
                if (canceled) {
                    Log.i(TAG, "Ignoring HOME; event canceled.");
                    return -1L;
                }
                TelecomManager telecomManager = getTelecommService();
                if (telecomManager != null && telecomManager.isRinging()) {
                    Log.i(TAG, "Ignoring HOME; there's a ringing incoming call.");
                    return -1L;
                }
                if (this.mDoubleTapOnHomeBehavior == 0) {
                    handleShortPressOnHome();
                    return -1L;
                }
                this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                this.mHomeDoubleTapPending = true;
                this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, ViewConfiguration.getDoubleTapTimeout());
                return -1L;
            }
            WindowManager.LayoutParams attrs = win != null ? win.getAttrs() : null;
            if (attrs != null) {
                int type = attrs.type;
                if (type == 2029 || type == 2009 || (attrs.privateFlags & 1024) != 0) {
                    return 0L;
                }
                int typeCount = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
                for (int i = 0; i < typeCount; i++) {
                    if (type == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i]) {
                        return -1L;
                    }
                }
            }
            if (repeatCount != 0) {
                if ((event.getFlags() & 128) == 0 || keyguardOn) {
                    return -1L;
                }
                handleLongPressOnHome(event.getDeviceId());
                return -1L;
            }
            this.mHomePressed = true;
            if (this.mHomeDoubleTapPending) {
                this.mHomeDoubleTapPending = false;
                this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                handleDoubleTapOnHome();
                return -1L;
            }
            if (this.mLongPressOnHomeBehavior != 1 && this.mDoubleTapOnHomeBehavior != 1) {
                return -1L;
            }
            preloadRecentApps();
            return -1L;
        }
        if (keyCode != 82) {
            if (keyCode == 84) {
                if (down) {
                    if (repeatCount != 0) {
                        return 0L;
                    }
                    this.mSearchKeyShortcutPending = true;
                    this.mConsumeSearchKeyUp = false;
                    return 0L;
                }
                this.mSearchKeyShortcutPending = false;
                if (!this.mConsumeSearchKeyUp) {
                    return 0L;
                }
                this.mConsumeSearchKeyUp = false;
                return -1L;
            }
            if (keyCode == 187) {
                if (keyguardOn) {
                    return -1L;
                }
                if (down && repeatCount == 0) {
                    preloadRecentApps();
                    return -1L;
                }
                if (down) {
                    return -1L;
                }
                toggleRecentApps();
                return -1L;
            }
            if (keyCode == 42 && event.isMetaPressed()) {
                if (down && (service = getStatusBarService()) != null) {
                    try {
                        service.expandNotificationsPanel();
                    } catch (RemoteException e) {
                    }
                }
            } else if (keyCode == 47 && event.isMetaPressed() && event.isCtrlPressed()) {
                if (down && repeatCount == 0) {
                    this.mScreenshotRunnable.setScreenshotType(event.isShiftPressed() ? 2 : 1);
                    this.mHandler.post(this.mScreenshotRunnable);
                    return -1L;
                }
            } else if (keyCode != 76 || !event.isMetaPressed()) {
                if (keyCode == 219) {
                    if (!down) {
                        if (this.mAssistKeyLongPressed) {
                            this.mAssistKeyLongPressed = false;
                            return -1L;
                        }
                        if (keyguardOn) {
                            return -1L;
                        }
                        launchAssistAction(null, event.getDeviceId());
                        return -1L;
                    }
                    if (repeatCount == 0) {
                        this.mAssistKeyLongPressed = false;
                        return -1L;
                    }
                    if (repeatCount != 1) {
                        return -1L;
                    }
                    this.mAssistKeyLongPressed = true;
                    if (keyguardOn) {
                        return -1L;
                    }
                    launchAssistLongPressAction();
                    return -1L;
                }
                if (keyCode != 231) {
                    if (keyCode == 120) {
                        if (!down || repeatCount != 0) {
                            return -1L;
                        }
                        String ScreenCaptureOn = Settings.System.getString(this.mContext.getContentResolver(), "screen_capture_on");
                        if (!ScreenCaptureOn.equals("1")) {
                            return -1L;
                        }
                        this.mScreenshotRunnable.setScreenshotType(1);
                        this.mHandler.post(this.mScreenshotRunnable);
                        return -1L;
                    }
                    if (keyCode == 221 || keyCode == 220) {
                        if (!down) {
                            return -1L;
                        }
                        int direction = keyCode == 221 ? 1 : -1;
                        int auto = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                        if (auto != 0) {
                            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                        }
                        int min = this.mPowerManager.getMinimumScreenBrightnessSetting();
                        int max = this.mPowerManager.getMaximumScreenBrightnessSetting();
                        int step = ((((max - min) + 10) - 1) / 10) * direction;
                        int brightness = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mPowerManager.getDefaultScreenBrightnessSetting(), -3);
                        Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", Math.max(min, Math.min(max, brightness + step)), -3);
                        startActivityAsUser(new Intent("android.intent.action.SHOW_BRIGHTNESS_DIALOG"), UserHandle.CURRENT_OR_SELF);
                        return -1L;
                    }
                    if ((keyCode == 24 || keyCode == 25 || keyCode == 164) && this.mUseTvRouting) {
                        dispatchDirectAudioEvent(event);
                        return -1L;
                    }
                } else if (!down) {
                    if (keyguardOn) {
                        IDeviceIdleController dic = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
                        if (dic != null) {
                            try {
                                dic.exitIdle("voice-search");
                            } catch (RemoteException e2) {
                            }
                        }
                        voiceIntent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
                        voiceIntent.putExtra("android.speech.extras.EXTRA_SECURE", true);
                    } else {
                        voiceIntent = new Intent("android.speech.action.WEB_SEARCH");
                    }
                    int dcha_state = BenesseExtension.getDchaState();
                    if (dcha_state == 0) {
                        startActivityAsUser(voiceIntent, UserHandle.CURRENT_OR_SELF);
                    }
                }
            } else if (down && repeatCount == 0 && !isKeyguardLocked()) {
                toggleKeyboardShortcutsMenu(event.getDeviceId());
            }
        } else if (down && repeatCount == 0 && this.mEnableShiftMenuBugReports && (metaState & 1) == 1) {
            this.mContext.sendOrderedBroadcastAsUser(new Intent("android.intent.action.BUG_REPORT"), UserHandle.CURRENT, null, null, null, 0, null, null);
            return -1L;
        }
        boolean actionTriggered = false;
        if (KeyEvent.isModifierKey(keyCode)) {
            if (!this.mPendingCapsLockToggle) {
                this.mInitialMetaState = this.mMetaState;
                this.mPendingCapsLockToggle = true;
            } else if (event.getAction() == 1) {
                int altOnMask = this.mMetaState & 50;
                int metaOnMask = this.mMetaState & 458752;
                if (metaOnMask != 0 && altOnMask != 0 && this.mInitialMetaState == (this.mMetaState ^ (altOnMask | metaOnMask))) {
                    this.mInputManagerInternal.toggleCapsLock(event.getDeviceId());
                    actionTriggered = true;
                }
                this.mPendingCapsLockToggle = false;
            }
        }
        this.mMetaState = metaState;
        if (actionTriggered) {
            return -1L;
        }
        if (KeyEvent.isMetaKey(keyCode)) {
            if (down) {
                this.mPendingMetaAction = true;
                return -1L;
            }
            if (!this.mPendingMetaAction) {
                return -1L;
            }
            launchAssistAction("android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD", event.getDeviceId());
            return -1L;
        }
        if (this.mSearchKeyShortcutPending) {
            KeyCharacterMap kcm = event.getKeyCharacterMap();
            if (kcm.isPrintingKey(keyCode)) {
                this.mConsumeSearchKeyUp = true;
                this.mSearchKeyShortcutPending = false;
                if (!down || repeatCount != 0 || keyguardOn) {
                    return -1L;
                }
                Intent shortcutIntent2 = this.mShortcutManager.getIntent(kcm, keyCode, metaState);
                if (shortcutIntent2 == null) {
                    Slog.i(TAG, "Dropping unregistered shortcut key combination: SEARCH+" + KeyEvent.keyCodeToString(keyCode));
                    return -1L;
                }
                shortcutIntent2.addFlags(268435456);
                try {
                    startActivityAsUser(shortcutIntent2, UserHandle.CURRENT);
                    dismissKeyboardShortcutsMenu();
                    return -1L;
                } catch (ActivityNotFoundException ex) {
                    Slog.w(TAG, "Dropping shortcut key combination because the activity to which it is registered was not found: SEARCH+" + KeyEvent.keyCodeToString(keyCode), ex);
                    return -1L;
                }
            }
        }
        if (down && repeatCount == 0 && !keyguardOn && (65536 & metaState) != 0) {
            KeyCharacterMap kcm2 = event.getKeyCharacterMap();
            if (kcm2.isPrintingKey(keyCode) && (shortcutIntent = this.mShortcutManager.getIntent(kcm2, keyCode, (-458753) & metaState)) != null) {
                shortcutIntent.addFlags(268435456);
                try {
                    startActivityAsUser(shortcutIntent, UserHandle.CURRENT);
                    dismissKeyboardShortcutsMenu();
                    return -1L;
                } catch (ActivityNotFoundException ex2) {
                    Slog.w(TAG, "Dropping shortcut key combination because the activity to which it is registered was not found: META+" + KeyEvent.keyCodeToString(keyCode), ex2);
                    return -1L;
                }
            }
        }
        if (down && repeatCount == 0 && !keyguardOn && (category = sApplicationLaunchKeyCategories.get(keyCode)) != null) {
            Intent intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", category);
            intent.setFlags(268435456);
            try {
                startActivityAsUser(intent, UserHandle.CURRENT);
                dismissKeyboardShortcutsMenu();
                return -1L;
            } catch (ActivityNotFoundException ex3) {
                Slog.w(TAG, "Dropping application launch key because the activity to which it is registered was not found: keyCode=" + keyCode + ", category=" + category, ex3);
                return -1L;
            }
        }
        int dcha_state2 = BenesseExtension.getDchaState();
        if (dcha_state2 == 0) {
            if (down && repeatCount == 0 && keyCode == 61) {
                if (this.mRecentAppsHeldModifiers == 0 && !keyguardOn && isUserSetupComplete()) {
                    int shiftlessModifiers = event.getModifiers() & (-194);
                    if (KeyEvent.metaStateHasModifiers(shiftlessModifiers, 2)) {
                        this.mRecentAppsHeldModifiers = shiftlessModifiers;
                        showRecentApps(true, false);
                        return -1L;
                    }
                }
            } else if (!down && this.mRecentAppsHeldModifiers != 0 && (this.mRecentAppsHeldModifiers & metaState) == 0) {
                this.mRecentAppsHeldModifiers = 0;
                hideRecentApps(true, false);
            }
        }
        if (down && repeatCount == 0 && (keyCode == 204 || (keyCode == 62 && (458752 & metaState) != 0))) {
            boolean forwardDirection = (metaState & HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS) == 0;
            this.mWindowManagerFuncs.switchInputMethod(forwardDirection);
            return -1L;
        }
        if (this.mLanguageSwitchKeyPressed && !down && (keyCode == 204 || keyCode == 62)) {
            this.mLanguageSwitchKeyPressed = false;
            return -1L;
        }
        if (isValidGlobalKey(keyCode) && this.mGlobalKeyManager.handleGlobalKey(this.mContext, keyCode, event)) {
            return -1L;
        }
        if (down) {
            long shortcutCode = keyCode;
            if (event.isCtrlPressed()) {
                shortcutCode |= 17592186044416L;
            }
            if (event.isAltPressed()) {
                shortcutCode |= 8589934592L;
            }
            if (event.isShiftPressed()) {
                shortcutCode |= 4294967296L;
            }
            if (event.isMetaPressed()) {
                shortcutCode |= 281474976710656L;
            }
            IShortcutService shortcutService = this.mShortcutKeyServices.get(shortcutCode);
            if (shortcutService != null) {
                try {
                    if (!isUserSetupComplete()) {
                        return -1L;
                    }
                    shortcutService.notifyShortcutKeyPressed(shortcutCode);
                    return -1L;
                } catch (RemoteException e3) {
                    this.mShortcutKeyServices.delete(shortcutCode);
                    return -1L;
                }
            }
        }
        return (65536 & metaState) != 0 ? -1L : 0L;
    }

    public KeyEvent dispatchUnhandledKey(WindowManagerPolicy.WindowState win, KeyEvent event, int policyFlags) {
        if (DEBUG_INPUT) {
            Slog.d(TAG, "Unhandled key: win=" + win + ", action=" + event.getAction() + ", flags=" + event.getFlags() + ", keyCode=" + event.getKeyCode() + ", scanCode=" + event.getScanCode() + ", metaState=" + event.getMetaState() + ", repeatCount=" + event.getRepeatCount() + ", policyFlags=" + policyFlags);
        }
        KeyEvent fallbackEvent = null;
        if ((event.getFlags() & 1024) == 0) {
            KeyCharacterMap kcm = event.getKeyCharacterMap();
            int keyCode = event.getKeyCode();
            int metaState = event.getMetaState();
            boolean initialDown = event.getAction() == 0 && event.getRepeatCount() == 0;
            KeyCharacterMap.FallbackAction fallbackAction = initialDown ? kcm.getFallbackAction(keyCode, metaState) : this.mFallbackActions.get(keyCode);
            if (fallbackAction != null) {
                if (DEBUG_INPUT) {
                    Slog.d(TAG, "Fallback: keyCode=" + fallbackAction.keyCode + " metaState=" + Integer.toHexString(fallbackAction.metaState));
                }
                int flags = event.getFlags() | 1024;
                fallbackEvent = KeyEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), fallbackAction.keyCode, event.getRepeatCount(), fallbackAction.metaState, event.getDeviceId(), event.getScanCode(), flags, event.getSource(), null);
                if (!interceptFallback(win, fallbackEvent, policyFlags)) {
                    fallbackEvent.recycle();
                    fallbackEvent = null;
                }
                if (initialDown) {
                    this.mFallbackActions.put(keyCode, fallbackAction);
                } else if (event.getAction() == 1) {
                    this.mFallbackActions.remove(keyCode);
                    fallbackAction.recycle();
                }
            }
        }
        if (DEBUG_INPUT) {
            if (fallbackEvent == null) {
                Slog.d(TAG, "No fallback.");
            } else {
                Slog.d(TAG, "Performing fallback: " + fallbackEvent);
            }
        }
        return fallbackEvent;
    }

    private boolean interceptFallback(WindowManagerPolicy.WindowState win, KeyEvent fallbackEvent, int policyFlags) {
        int actions = interceptKeyBeforeQueueing(fallbackEvent, policyFlags);
        if ((actions & 1) != 0) {
            long delayMillis = interceptKeyBeforeDispatching(win, fallbackEvent, policyFlags);
            if (delayMillis == 0) {
                return true;
            }
        }
        return false;
    }

    public void registerShortcutKey(long shortcutCode, IShortcutService shortcutService) throws RemoteException {
        synchronized (this.mLock) {
            IShortcutService service = this.mShortcutKeyServices.get(shortcutCode);
            if (service != null && service.asBinder().pingBinder()) {
                throw new RemoteException("Key already exists.");
            }
            this.mShortcutKeyServices.put(shortcutCode, shortcutService);
        }
    }

    private void launchAssistLongPressAction() {
        performHapticFeedbackLw(null, 0, false);
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        Intent intent = new Intent("android.intent.action.SEARCH_LONG_PRESS");
        intent.setFlags(268435456);
        try {
            SearchManager searchManager = getSearchManager();
            if (searchManager != null) {
                searchManager.stopSearch();
            }
            startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (ActivityNotFoundException e) {
            Slog.w(TAG, "No activity to handle assist long press action.", e);
        }
    }

    private void launchAssistAction(String hint, int deviceId) {
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        if (!isUserSetupComplete()) {
            return;
        }
        Bundle args = null;
        if (deviceId > Integer.MIN_VALUE) {
            args = new Bundle();
            args.putInt("android.intent.extra.ASSIST_INPUT_DEVICE_ID", deviceId);
        }
        if ((this.mContext.getResources().getConfiguration().uiMode & 15) == 4) {
            ((SearchManager) this.mContext.getSystemService("search")).launchLegacyAssist(hint, UserHandle.myUserId(), args);
            return;
        }
        if (hint != null) {
            if (args == null) {
                args = new Bundle();
            }
            args.putBoolean(hint, true);
        }
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.startAssist(args);
    }

    private void startActivityAsUser(Intent intent, UserHandle handle) {
        if (isUserSetupComplete()) {
            this.mContext.startActivityAsUser(intent, handle);
        } else {
            Slog.i(TAG, "Not starting activity because user setup is in progress: " + intent);
        }
    }

    private SearchManager getSearchManager() {
        if (this.mSearchManager == null) {
            this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
        }
        return this.mSearchManager;
    }

    private void preloadRecentApps() {
        this.mPreloadedRecentApps = true;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.preloadRecentApps();
    }

    private void cancelPreloadRecentApps() {
        if (!this.mPreloadedRecentApps) {
            return;
        }
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.cancelPreloadRecentApps();
    }

    private void toggleRecentApps() {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.toggleRecentApps();
    }

    public void showRecentApps(boolean fromHome) {
        this.mHandler.removeMessages(9);
        this.mHandler.obtainMessage(9, fromHome ? 1 : 0, 0).sendToTarget();
    }

    private void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.showRecentApps(triggeredFromAltTab, fromHome);
    }

    private void toggleKeyboardShortcutsMenu(int deviceId) {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.toggleKeyboardShortcutsMenu(deviceId);
    }

    private void dismissKeyboardShortcutsMenu() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.dismissKeyboardShortcutsMenu();
    }

    private void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHome) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar == null) {
            return;
        }
        statusbar.hideRecentApps(triggeredFromAltTab, triggeredFromHome);
    }

    void launchHomeFromHotKey() {
        launchHomeFromHotKey(true, true);
    }

    void launchHomeFromHotKey(final boolean awakenFromDreams, boolean respectKeyguard) {
        if (respectKeyguard) {
            if (isKeyguardShowingAndNotOccluded()) {
                return;
            }
            if (!this.mHideLockScreen && this.mKeyguardDelegate.isInputRestricted()) {
                this.mKeyguardDelegate.verifyUnlock(new WindowManagerPolicy.OnKeyguardExitResult() {
                    public void onKeyguardExitResult(boolean success) {
                        if (!success) {
                            return;
                        }
                        try {
                            ActivityManagerNative.getDefault().stopAppSwitches();
                        } catch (RemoteException e) {
                        }
                        PhoneWindowManager.this.sendCloseSystemWindows(PhoneWindowManager.SYSTEM_DIALOG_REASON_HOME_KEY);
                        PhoneWindowManager.this.startDockOrHome(true, awakenFromDreams);
                    }
                });
                return;
            }
        }
        try {
            ActivityManagerNative.getDefault().stopAppSwitches();
        } catch (RemoteException e) {
        }
        if (this.mRecentsVisible) {
            if (awakenFromDreams) {
                awakenDreams();
            }
            hideRecentApps(false, true);
        } else {
            sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
            startDockOrHome(true, awakenFromDreams);
        }
    }

    final class HideNavInputEventReceiver extends InputEventReceiver {
        public HideNavInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent event) {
            try {
                if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
                    MotionEvent motionEvent = (MotionEvent) event;
                    if (motionEvent.getAction() == 0) {
                        boolean changed = false;
                        synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                            if (PhoneWindowManager.this.mInputConsumer == null) {
                                return;
                            }
                            int newVal = PhoneWindowManager.this.mResettingSystemUiFlags | 2 | 1 | 4;
                            if (PhoneWindowManager.this.mResettingSystemUiFlags != newVal) {
                                PhoneWindowManager.this.mResettingSystemUiFlags = newVal;
                                changed = true;
                            }
                            int newVal2 = PhoneWindowManager.this.mForceClearedSystemUiFlags | 2;
                            if (PhoneWindowManager.this.mForceClearedSystemUiFlags != newVal2) {
                                PhoneWindowManager.this.mForceClearedSystemUiFlags = newVal2;
                                changed = true;
                                PhoneWindowManager.this.mHandler.postDelayed(PhoneWindowManager.this.mClearHideNavigationFlag, 1000L);
                            }
                            if (changed) {
                                PhoneWindowManager.this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
                            }
                        }
                    }
                }
            } finally {
                finishInputEvent(event, false);
            }
        }
    }

    public int adjustSystemUiVisibilityLw(int visibility) {
        this.mStatusBarController.adjustSystemUiVisibilityLw(this.mLastSystemUiFlags, visibility);
        this.mNavigationBarController.adjustSystemUiVisibilityLw(this.mLastSystemUiFlags, visibility);
        this.mRecentsVisible = (visibility & PackageManagerService.DumpState.DUMP_KEYSETS) > 0;
        this.mTvPictureInPictureVisible = (65536 & visibility) > 0;
        this.mResettingSystemUiFlags &= visibility;
        return (~this.mResettingSystemUiFlags) & visibility & (~this.mForceClearedSystemUiFlags);
    }

    public boolean getInsetHintLw(WindowManager.LayoutParams attrs, Rect taskBounds, int displayRotation, int displayWidth, int displayHeight, Rect outContentInsets, Rect outStableInsets, Rect outOutsets) {
        int availRight;
        int availBottom;
        int outset;
        int fl = PolicyControl.getWindowFlags(null, attrs);
        int sysuiVis = PolicyControl.getSystemUiVisibility(null, attrs);
        int systemUiVisibility = sysuiVis | attrs.subtreeSystemUiVisibility;
        boolean useOutsets = outOutsets != null ? shouldUseOutsets(attrs, fl) : false;
        if (useOutsets && (outset = ScreenShapeHelper.getWindowOutsetBottomPx(this.mContext.getResources())) > 0) {
            if (displayRotation == 0) {
                outOutsets.bottom += outset;
            } else if (displayRotation == 1) {
                outOutsets.right += outset;
            } else if (displayRotation == 2) {
                outOutsets.top += outset;
            } else if (displayRotation == 3) {
                outOutsets.left += outset;
            }
        }
        if ((65792 & fl) == 65792) {
            if (canHideNavigationBar() && (systemUiVisibility & 512) != 0) {
                availRight = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                availBottom = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
            } else {
                availRight = this.mRestrictedScreenLeft + this.mRestrictedScreenWidth;
                availBottom = this.mRestrictedScreenTop + this.mRestrictedScreenHeight;
            }
            if ((systemUiVisibility & 256) != 0) {
                if ((fl & 1024) != 0) {
                    outContentInsets.set(this.mStableFullscreenLeft, this.mStableFullscreenTop, availRight - this.mStableFullscreenRight, availBottom - this.mStableFullscreenBottom);
                } else {
                    outContentInsets.set(this.mStableLeft, this.mStableTop, availRight - this.mStableRight, availBottom - this.mStableBottom);
                }
            } else if ((fl & 1024) != 0 || (33554432 & fl) != 0) {
                outContentInsets.setEmpty();
            } else if ((systemUiVisibility & 1028) == 0) {
                outContentInsets.set(this.mCurLeft, this.mCurTop, availRight - this.mCurRight, availBottom - this.mCurBottom);
            } else {
                outContentInsets.set(this.mCurLeft, this.mCurTop, availRight - this.mCurRight, availBottom - this.mCurBottom);
            }
            outStableInsets.set(this.mStableLeft, this.mStableTop, availRight - this.mStableRight, availBottom - this.mStableBottom);
            if (taskBounds != null) {
                calculateRelevantTaskInsets(taskBounds, outContentInsets, displayWidth, displayHeight);
                calculateRelevantTaskInsets(taskBounds, outStableInsets, displayWidth, displayHeight);
            }
            return this.mForceShowSystemBars;
        }
        outContentInsets.setEmpty();
        outStableInsets.setEmpty();
        return this.mForceShowSystemBars;
    }

    private void calculateRelevantTaskInsets(Rect taskBounds, Rect inOutInsets, int displayWidth, int displayHeight) {
        mTmpRect.set(0, 0, displayWidth, displayHeight);
        mTmpRect.inset(inOutInsets);
        mTmpRect.intersect(taskBounds);
        int leftInset = mTmpRect.left - taskBounds.left;
        int topInset = mTmpRect.top - taskBounds.top;
        int rightInset = taskBounds.right - mTmpRect.right;
        int bottomInset = taskBounds.bottom - mTmpRect.bottom;
        inOutInsets.set(leftInset, topInset, rightInset, bottomInset);
    }

    private boolean shouldUseOutsets(WindowManager.LayoutParams attrs, int fl) {
        return attrs.type == 2013 || (33555456 & fl) != 0;
    }

    public void beginLayoutLw(boolean isDefaultDisplay, int displayWidth, int displayHeight, int displayRotation, int uiMode) {
        int overscanLeft;
        int overscanTop;
        int overscanRight;
        int overscanBottom;
        this.mDisplayRotation = displayRotation;
        if (isDefaultDisplay) {
            switch (displayRotation) {
                case 1:
                    overscanLeft = this.mOverscanTop;
                    overscanTop = this.mOverscanRight;
                    overscanRight = this.mOverscanBottom;
                    overscanBottom = this.mOverscanLeft;
                    break;
                case 2:
                    overscanLeft = this.mOverscanRight;
                    overscanTop = this.mOverscanBottom;
                    overscanRight = this.mOverscanLeft;
                    overscanBottom = this.mOverscanTop;
                    break;
                case 3:
                    overscanLeft = this.mOverscanBottom;
                    overscanTop = this.mOverscanLeft;
                    overscanRight = this.mOverscanTop;
                    overscanBottom = this.mOverscanRight;
                    break;
                default:
                    overscanLeft = this.mOverscanLeft;
                    overscanTop = this.mOverscanTop;
                    overscanRight = this.mOverscanRight;
                    overscanBottom = this.mOverscanBottom;
                    break;
            }
        } else {
            overscanLeft = 0;
            overscanTop = 0;
            overscanRight = 0;
            overscanBottom = 0;
        }
        this.mRestrictedOverscanScreenLeft = 0;
        this.mOverscanScreenLeft = 0;
        this.mRestrictedOverscanScreenTop = 0;
        this.mOverscanScreenTop = 0;
        this.mRestrictedOverscanScreenWidth = displayWidth;
        this.mOverscanScreenWidth = displayWidth;
        this.mRestrictedOverscanScreenHeight = displayHeight;
        this.mOverscanScreenHeight = displayHeight;
        this.mSystemLeft = 0;
        this.mSystemTop = 0;
        this.mSystemRight = displayWidth;
        this.mSystemBottom = displayHeight;
        this.mUnrestrictedScreenLeft = overscanLeft;
        this.mUnrestrictedScreenTop = overscanTop;
        this.mUnrestrictedScreenWidth = (displayWidth - overscanLeft) - overscanRight;
        this.mUnrestrictedScreenHeight = (displayHeight - overscanTop) - overscanBottom;
        this.mRestrictedScreenLeft = this.mUnrestrictedScreenLeft;
        this.mRestrictedScreenTop = this.mUnrestrictedScreenTop;
        int i = this.mUnrestrictedScreenWidth;
        this.mSystemGestures.screenWidth = i;
        this.mRestrictedScreenWidth = i;
        int i2 = this.mUnrestrictedScreenHeight;
        this.mSystemGestures.screenHeight = i2;
        this.mRestrictedScreenHeight = i2;
        int i3 = this.mUnrestrictedScreenLeft;
        this.mCurLeft = i3;
        this.mStableFullscreenLeft = i3;
        this.mStableLeft = i3;
        this.mVoiceContentLeft = i3;
        this.mContentLeft = i3;
        this.mDockLeft = i3;
        int i4 = this.mUnrestrictedScreenTop;
        this.mCurTop = i4;
        this.mStableFullscreenTop = i4;
        this.mStableTop = i4;
        this.mVoiceContentTop = i4;
        this.mContentTop = i4;
        this.mDockTop = i4;
        int i5 = displayWidth - overscanRight;
        this.mCurRight = i5;
        this.mStableFullscreenRight = i5;
        this.mStableRight = i5;
        this.mVoiceContentRight = i5;
        this.mContentRight = i5;
        this.mDockRight = i5;
        int i6 = displayHeight - overscanBottom;
        this.mCurBottom = i6;
        this.mStableFullscreenBottom = i6;
        this.mStableBottom = i6;
        this.mVoiceContentBottom = i6;
        this.mContentBottom = i6;
        this.mDockBottom = i6;
        this.mDockLayer = 268435456;
        this.mStatusBarLayer = -1;
        Rect pf = mTmpParentFrame;
        Rect df = mTmpDisplayFrame;
        Rect of = mTmpOverscanFrame;
        Rect vf = mTmpVisibleFrame;
        Rect dcf = mTmpDecorFrame;
        int i7 = this.mDockLeft;
        vf.left = i7;
        of.left = i7;
        df.left = i7;
        pf.left = i7;
        int i8 = this.mDockTop;
        vf.top = i8;
        of.top = i8;
        df.top = i8;
        pf.top = i8;
        int i9 = this.mDockRight;
        vf.right = i9;
        of.right = i9;
        df.right = i9;
        pf.right = i9;
        int i10 = this.mDockBottom;
        vf.bottom = i10;
        of.bottom = i10;
        df.bottom = i10;
        pf.bottom = i10;
        dcf.setEmpty();
        if (!isDefaultDisplay) {
            return;
        }
        int sysui = this.mLastSystemUiFlags;
        boolean navVisible = (sysui & 2) == 0 && this.mHideNavigationBar == 0;
        boolean navTranslucent = ((-2147450880) & sysui) != 0;
        boolean immersive = ((sysui & PackageManagerService.DumpState.DUMP_VERIFIERS) == 0 && this.mHideNavigationBar == 0) ? false : true;
        boolean immersiveSticky = (sysui & 4096) != 0;
        boolean z = !immersive ? immersiveSticky : true;
        boolean navTranslucent2 = navTranslucent & (!immersiveSticky);
        boolean isKeyguardShowing = isStatusBarKeyguard() && !this.mHideLockScreen;
        if (!isKeyguardShowing) {
            navTranslucent2 &= areTranslucentBarsAllowed();
        }
        boolean statusBarExpandedNotKeyguard = !isKeyguardShowing && this.mStatusBar != null && this.mHideNavigationBar == 0 && this.mStatusBar.getAttrs().height == -1 && this.mStatusBar.getAttrs().width == -1;
        if (navVisible || z) {
            if (this.mInputConsumer != null) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(19, this.mInputConsumer));
                this.mInputConsumer = null;
            }
        } else if (this.mInputConsumer == null) {
            this.mInputConsumer = this.mWindowManagerFuncs.addInputConsumer(this.mHandler.getLooper(), this.mHideNavInputEventReceiverFactory);
        }
        boolean updateSysUiVisibility = layoutNavigationBar(displayWidth, displayHeight, displayRotation, uiMode, overscanRight, overscanBottom, dcf, navVisible | (!canHideNavigationBar()), navTranslucent2, z, statusBarExpandedNotKeyguard);
        if (DEBUG_LAYOUT) {
            Slog.i(TAG, String.format("mDock rect: (%d,%d - %d,%d)", Integer.valueOf(this.mDockLeft), Integer.valueOf(this.mDockTop), Integer.valueOf(this.mDockRight), Integer.valueOf(this.mDockBottom)));
        }
        if (!(updateSysUiVisibility | layoutStatusBar(pf, df, of, vf, dcf, sysui, isKeyguardShowing))) {
            return;
        }
        updateSystemUiVisibilityLw();
    }

    private boolean layoutStatusBar(Rect pf, Rect df, Rect of, Rect vf, Rect dcf, int sysui, boolean isKeyguardShowing) {
        if (this.mStatusBar != null) {
            int i = this.mUnrestrictedScreenLeft;
            of.left = i;
            df.left = i;
            pf.left = i;
            int i2 = this.mUnrestrictedScreenTop;
            of.top = i2;
            df.top = i2;
            pf.top = i2;
            int i3 = this.mUnrestrictedScreenWidth + this.mUnrestrictedScreenLeft;
            of.right = i3;
            df.right = i3;
            pf.right = i3;
            int i4 = this.mUnrestrictedScreenHeight + this.mUnrestrictedScreenTop;
            of.bottom = i4;
            df.bottom = i4;
            pf.bottom = i4;
            vf.left = this.mStableLeft;
            vf.top = this.mStableTop;
            vf.right = this.mStableRight;
            vf.bottom = this.mStableBottom;
            this.mStatusBarLayer = this.mStatusBar.getSurfaceLayer();
            this.mStatusBar.computeFrameLw(pf, df, vf, vf, vf, dcf, vf, vf);
            this.mStableTop = this.mUnrestrictedScreenTop + this.mStatusBarHeight;
            boolean statusBarTransient = (67108864 & sysui) != 0;
            boolean statusBarTranslucent = (1073741832 & sysui) != 0;
            if (!isKeyguardShowing) {
                statusBarTranslucent &= areTranslucentBarsAllowed();
            }
            if (this.mStatusBar.isVisibleLw() && !statusBarTransient) {
                this.mDockTop = this.mUnrestrictedScreenTop + this.mStatusBarHeight;
                int i5 = this.mDockTop;
                this.mCurTop = i5;
                this.mVoiceContentTop = i5;
                this.mContentTop = i5;
                int i6 = this.mDockBottom;
                this.mCurBottom = i6;
                this.mVoiceContentBottom = i6;
                this.mContentBottom = i6;
                int i7 = this.mDockLeft;
                this.mCurLeft = i7;
                this.mVoiceContentLeft = i7;
                this.mContentLeft = i7;
                int i8 = this.mDockRight;
                this.mCurRight = i8;
                this.mVoiceContentRight = i8;
                this.mContentRight = i8;
                if (DEBUG_LAYOUT) {
                    Slog.v(TAG, "Status bar: " + String.format("dock=[%d,%d][%d,%d] content=[%d,%d][%d,%d] cur=[%d,%d][%d,%d]", Integer.valueOf(this.mDockLeft), Integer.valueOf(this.mDockTop), Integer.valueOf(this.mDockRight), Integer.valueOf(this.mDockBottom), Integer.valueOf(this.mContentLeft), Integer.valueOf(this.mContentTop), Integer.valueOf(this.mContentRight), Integer.valueOf(this.mContentBottom), Integer.valueOf(this.mCurLeft), Integer.valueOf(this.mCurTop), Integer.valueOf(this.mCurRight), Integer.valueOf(this.mCurBottom)));
                }
            }
            if (this.mStatusBar.isVisibleLw() && !this.mStatusBar.isAnimatingLw() && !statusBarTransient && !statusBarTranslucent && !this.mStatusBarController.wasRecentlyTranslucent()) {
                this.mSystemTop = this.mUnrestrictedScreenTop + this.mStatusBarHeight;
            }
            if (this.mStatusBarController.checkHiddenLw()) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean layoutNavigationBar(int displayWidth, int displayHeight, int displayRotation, int uiMode, int overscanRight, int overscanBottom, Rect dcf, boolean navVisible, boolean navTranslucent, boolean navAllowedHidden, boolean statusBarExpandedNotKeyguard) {
        if (this.mNavigationBar != null) {
            boolean transientNavBarShowing = this.mNavigationBarController.isTransientShowing();
            this.mNavigationBarOnBottom = isNavigationBarOnBottom(displayWidth, displayHeight);
            if (this.mNavigationBarOnBottom) {
                int top = (displayHeight - overscanBottom) - getNavigationBarHeight(displayRotation, uiMode);
                mTmpNavigationFrame.set(0, top, displayWidth, displayHeight - overscanBottom);
                int i = mTmpNavigationFrame.top;
                this.mStableFullscreenBottom = i;
                this.mStableBottom = i;
                if (transientNavBarShowing) {
                    this.mNavigationBarController.setBarShowingLw(true);
                } else if (navVisible) {
                    if (!this.mIsAlarmBoot && !this.mIsShutDown) {
                        this.mNavigationBarController.setBarShowingLw(true);
                        this.mDockBottom = mTmpNavigationFrame.top;
                        this.mRestrictedScreenHeight = this.mDockBottom - this.mRestrictedScreenTop;
                        this.mRestrictedOverscanScreenHeight = this.mDockBottom - this.mRestrictedOverscanScreenTop;
                    }
                } else {
                    this.mNavigationBarController.setBarShowingLw(statusBarExpandedNotKeyguard);
                }
                if (navVisible && !navTranslucent && !navAllowedHidden && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent()) {
                    this.mSystemBottom = mTmpNavigationFrame.top;
                }
            } else {
                int left = (displayWidth - overscanRight) - getNavigationBarWidth(displayRotation, uiMode);
                mTmpNavigationFrame.set(left, 0, displayWidth - overscanRight, displayHeight);
                int i2 = mTmpNavigationFrame.left;
                this.mStableFullscreenRight = i2;
                this.mStableRight = i2;
                if (transientNavBarShowing) {
                    this.mNavigationBarController.setBarShowingLw(true);
                } else if (navVisible) {
                    if (!this.mIsAlarmBoot && !this.mIsShutDown) {
                        this.mNavigationBarController.setBarShowingLw(true);
                        this.mDockRight = mTmpNavigationFrame.left;
                        this.mRestrictedScreenWidth = this.mDockRight - this.mRestrictedScreenLeft;
                        this.mRestrictedOverscanScreenWidth = this.mDockRight - this.mRestrictedOverscanScreenLeft;
                    }
                } else {
                    this.mNavigationBarController.setBarShowingLw(statusBarExpandedNotKeyguard);
                }
                if (navVisible && !navTranslucent && !navAllowedHidden && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent()) {
                    this.mSystemRight = mTmpNavigationFrame.left;
                }
            }
            int i3 = this.mDockTop;
            this.mCurTop = i3;
            this.mVoiceContentTop = i3;
            this.mContentTop = i3;
            int i4 = this.mDockBottom;
            this.mCurBottom = i4;
            this.mVoiceContentBottom = i4;
            this.mContentBottom = i4;
            int i5 = this.mDockLeft;
            this.mCurLeft = i5;
            this.mVoiceContentLeft = i5;
            this.mContentLeft = i5;
            int i6 = this.mDockRight;
            this.mCurRight = i6;
            this.mVoiceContentRight = i6;
            this.mContentRight = i6;
            this.mStatusBarLayer = this.mNavigationBar.getSurfaceLayer();
            this.mNavigationBar.computeFrameLw(mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, dcf, mTmpNavigationFrame, mTmpNavigationFrame);
            if (DEBUG_LAYOUT) {
                Slog.i(TAG, "mNavigationBar frame: " + mTmpNavigationFrame);
            }
            if (this.mNavigationBarController.checkHiddenLw()) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean isNavigationBarOnBottom(int displayWidth, int displayHeight) {
        return !this.mNavigationBarCanMove || displayWidth < displayHeight;
    }

    public int getSystemDecorLayerLw() {
        if (this.mStatusBar != null && this.mStatusBar.isVisibleLw()) {
            return this.mStatusBar.getSurfaceLayer();
        }
        if (this.mNavigationBar != null && this.mNavigationBar.isVisibleLw()) {
            return this.mNavigationBar.getSurfaceLayer();
        }
        return 0;
    }

    public void getContentRectLw(Rect r) {
        r.set(this.mContentLeft, this.mContentTop, this.mContentRight, this.mContentBottom);
    }

    void setAttachedWindowFrames(WindowManagerPolicy.WindowState win, int fl, int adjust, WindowManagerPolicy.WindowState attached, boolean insetDecors, Rect pf, Rect df, Rect of, Rect cf, Rect vf) {
        if (win.getSurfaceLayer() > this.mDockLayer && attached.getSurfaceLayer() < this.mDockLayer) {
            int i = this.mDockLeft;
            vf.left = i;
            cf.left = i;
            of.left = i;
            df.left = i;
            int i2 = this.mDockTop;
            vf.top = i2;
            cf.top = i2;
            of.top = i2;
            df.top = i2;
            int i3 = this.mDockRight;
            vf.right = i3;
            cf.right = i3;
            of.right = i3;
            df.right = i3;
            int i4 = this.mDockBottom;
            vf.bottom = i4;
            cf.bottom = i4;
            of.bottom = i4;
            df.bottom = i4;
        } else {
            if (adjust != 16) {
                cf.set((1073741824 & fl) != 0 ? attached.getContentFrameLw() : attached.getOverscanFrameLw());
            } else {
                cf.set(attached.getContentFrameLw());
                if (attached.isVoiceInteraction()) {
                    if (cf.left < this.mVoiceContentLeft) {
                        cf.left = this.mVoiceContentLeft;
                    }
                    if (cf.top < this.mVoiceContentTop) {
                        cf.top = this.mVoiceContentTop;
                    }
                    if (cf.right > this.mVoiceContentRight) {
                        cf.right = this.mVoiceContentRight;
                    }
                    if (cf.bottom > this.mVoiceContentBottom) {
                        cf.bottom = this.mVoiceContentBottom;
                    }
                } else if (attached.getSurfaceLayer() < this.mDockLayer) {
                    if (cf.left < this.mContentLeft) {
                        cf.left = this.mContentLeft;
                    }
                    if (cf.top < this.mContentTop) {
                        cf.top = this.mContentTop;
                    }
                    if (cf.right > this.mContentRight) {
                        cf.right = this.mContentRight;
                    }
                    if (cf.bottom > this.mContentBottom) {
                        cf.bottom = this.mContentBottom;
                    }
                }
            }
            df.set(insetDecors ? attached.getDisplayFrameLw() : cf);
            if (insetDecors) {
                cf = attached.getOverscanFrameLw();
            }
            of.set(cf);
            vf.set(attached.getVisibleFrameLw());
        }
        if ((fl & 256) == 0) {
            df = attached.getFrameLw();
        }
        pf.set(df);
    }

    private void applyStableConstraints(int sysui, int fl, Rect r) {
        if ((sysui & 256) == 0) {
            return;
        }
        if ((fl & 1024) != 0) {
            if (r.left < this.mStableFullscreenLeft) {
                r.left = this.mStableFullscreenLeft;
            }
            if (r.top < this.mStableFullscreenTop) {
                r.top = this.mStableFullscreenTop;
            }
            if (r.right > this.mStableFullscreenRight) {
                r.right = this.mStableFullscreenRight;
            }
            if (r.bottom > this.mStableFullscreenBottom) {
                r.bottom = this.mStableFullscreenBottom;
                return;
            }
            return;
        }
        if (r.left < this.mStableLeft) {
            r.left = this.mStableLeft;
        }
        if (r.top < this.mStableTop) {
            r.top = this.mStableTop;
        }
        if (r.right > this.mStableRight) {
            r.right = this.mStableRight;
        }
        if (r.bottom > this.mStableBottom) {
            r.bottom = this.mStableBottom;
        }
    }

    private boolean canReceiveInput(WindowManagerPolicy.WindowState win) {
        boolean notFocusable = (win.getAttrs().flags & 8) != 0;
        boolean altFocusableIm = (win.getAttrs().flags & PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0;
        boolean notFocusableForIm = notFocusable ^ altFocusableIm;
        return !notFocusableForIm;
    }

    public void layoutWindowLw(WindowManagerPolicy.WindowState win, WindowManagerPolicy.WindowState attached) {
        if ((win != this.mStatusBar || canReceiveInput(win)) && win != this.mNavigationBar) {
            WindowManager.LayoutParams attrs = win.getAttrs();
            boolean isDefaultDisplay = win.isDefaultDisplay();
            boolean needsToOffsetInputMethodTarget = isDefaultDisplay && win == this.mLastInputMethodTargetWindow && this.mLastInputMethodWindow != null;
            if (needsToOffsetInputMethodTarget) {
                if (DEBUG_LAYOUT) {
                    Slog.i(TAG, "Offset ime target window by the last ime window state");
                }
                offsetInputMethodWindowLw(this.mLastInputMethodWindow);
            }
            int fl = PolicyControl.getWindowFlags(win, attrs);
            int pfl = attrs.privateFlags;
            int sim = attrs.softInputMode;
            int sysUiFl = PolicyControl.getSystemUiVisibility(win, null);
            Rect pf = mTmpParentFrame;
            Rect df = mTmpDisplayFrame;
            Rect of = mTmpOverscanFrame;
            Rect cf = mTmpContentFrame;
            Rect vf = mTmpVisibleFrame;
            Rect dcf = mTmpDecorFrame;
            Rect sf = mTmpStableFrame;
            Rect osf = null;
            dcf.setEmpty();
            boolean zIsVisibleLw = (isDefaultDisplay && this.mHasNavigationBar && this.mNavigationBar != null) ? this.mNavigationBar.isVisibleLw() : false;
            int adjust = sim & 240;
            if (isDefaultDisplay) {
                sf.set(this.mStableLeft, this.mStableTop, this.mStableRight, this.mStableBottom);
            } else {
                sf.set(this.mOverscanLeft, this.mOverscanTop, this.mOverscanRight, this.mOverscanBottom);
            }
            if (isDefaultDisplay) {
                if (attrs.type == 2011) {
                    int i = this.mDockLeft;
                    vf.left = i;
                    cf.left = i;
                    of.left = i;
                    df.left = i;
                    pf.left = i;
                    int i2 = this.mDockTop;
                    vf.top = i2;
                    cf.top = i2;
                    of.top = i2;
                    df.top = i2;
                    pf.top = i2;
                    int i3 = this.mDockRight;
                    vf.right = i3;
                    cf.right = i3;
                    of.right = i3;
                    df.right = i3;
                    pf.right = i3;
                    int i4 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                    of.bottom = i4;
                    df.bottom = i4;
                    pf.bottom = i4;
                    int i5 = this.mStableBottom;
                    vf.bottom = i5;
                    cf.bottom = i5;
                    if (this.mStatusBar != null && this.mFocusedWindow == this.mStatusBar && canReceiveInput(this.mStatusBar)) {
                        int i6 = this.mStableRight;
                        vf.right = i6;
                        cf.right = i6;
                        of.right = i6;
                        df.right = i6;
                        pf.right = i6;
                    }
                    attrs.gravity = 80;
                    this.mDockLayer = win.getSurfaceLayer();
                } else if (attrs.type == 2031) {
                    int i7 = this.mUnrestrictedScreenLeft;
                    of.left = i7;
                    df.left = i7;
                    pf.left = i7;
                    int i8 = this.mUnrestrictedScreenTop;
                    of.top = i8;
                    df.top = i8;
                    pf.top = i8;
                    int i9 = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                    of.right = i9;
                    df.right = i9;
                    pf.right = i9;
                    int i10 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                    of.bottom = i10;
                    df.bottom = i10;
                    pf.bottom = i10;
                    if (adjust != 16) {
                        cf.left = this.mDockLeft;
                        cf.top = this.mDockTop;
                        cf.right = this.mDockRight;
                        cf.bottom = this.mDockBottom;
                    } else {
                        cf.left = this.mContentLeft;
                        cf.top = this.mContentTop;
                        cf.right = this.mContentRight;
                        cf.bottom = this.mContentBottom;
                    }
                    if (adjust != 48) {
                        vf.left = this.mCurLeft;
                        vf.top = this.mCurTop;
                        vf.right = this.mCurRight;
                        vf.bottom = this.mCurBottom;
                    } else {
                        vf.set(cf);
                    }
                } else if (win == this.mStatusBar) {
                    int i11 = this.mUnrestrictedScreenLeft;
                    of.left = i11;
                    df.left = i11;
                    pf.left = i11;
                    int i12 = this.mUnrestrictedScreenTop;
                    of.top = i12;
                    df.top = i12;
                    pf.top = i12;
                    int i13 = this.mUnrestrictedScreenWidth + this.mUnrestrictedScreenLeft;
                    of.right = i13;
                    df.right = i13;
                    pf.right = i13;
                    int i14 = this.mUnrestrictedScreenHeight + this.mUnrestrictedScreenTop;
                    of.bottom = i14;
                    df.bottom = i14;
                    pf.bottom = i14;
                    int i15 = this.mStableLeft;
                    vf.left = i15;
                    cf.left = i15;
                    int i16 = this.mStableTop;
                    vf.top = i16;
                    cf.top = i16;
                    int i17 = this.mStableRight;
                    vf.right = i17;
                    cf.right = i17;
                    vf.bottom = this.mStableBottom;
                    if (adjust == 16) {
                        cf.bottom = this.mContentBottom;
                    } else {
                        cf.bottom = this.mDockBottom;
                        vf.bottom = this.mContentBottom;
                    }
                } else {
                    dcf.left = this.mSystemLeft;
                    dcf.top = this.mSystemTop;
                    dcf.right = this.mSystemRight;
                    dcf.bottom = this.mSystemBottom;
                    boolean inheritTranslucentDecor = (attrs.privateFlags & 512) != 0;
                    boolean isAppWindow = attrs.type >= 1 && attrs.type <= 99;
                    boolean topAtRest = win == this.mTopFullscreenOpaqueWindowState && !win.isAnimatingLw();
                    if (isAppWindow && !inheritTranslucentDecor && !topAtRest) {
                        if ((sysUiFl & 4) == 0 && (fl & 1024) == 0 && (67108864 & fl) == 0 && (Integer.MIN_VALUE & fl) == 0 && (131072 & pfl) == 0) {
                            dcf.top = this.mStableTop;
                        }
                        if ((134217728 & fl) == 0 && (sysUiFl & 2) == 0 && this.mHideNavigationBar == 0 && (Integer.MIN_VALUE & fl) == 0) {
                            dcf.bottom = this.mStableBottom;
                            dcf.right = this.mStableRight;
                        }
                    }
                    if ((65792 & fl) == 65792) {
                        if (DEBUG_LAYOUT) {
                            Slog.v(TAG, "layoutWindowLw(" + attrs.getTitle() + "): IN_SCREEN, INSET_DECOR, sim=#" + Integer.toHexString(adjust) + ", type=" + attrs.type + ", flag=" + fl + ", canHideNavigationBar=" + canHideNavigationBar() + ", sysUiFl=" + sysUiFl);
                        }
                        if (attached != null) {
                            setAttachedWindowFrames(win, fl, adjust, attached, true, pf, df, of, cf, vf);
                        } else {
                            if (attrs.type == 2014 || attrs.type == 2017) {
                                int i18 = zIsVisibleLw ? this.mDockLeft : this.mUnrestrictedScreenLeft;
                                of.left = i18;
                                df.left = i18;
                                pf.left = i18;
                                int i19 = this.mUnrestrictedScreenTop;
                                of.top = i19;
                                df.top = i19;
                                pf.top = i19;
                                int i20 = zIsVisibleLw ? this.mRestrictedScreenLeft + this.mRestrictedScreenWidth : this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                                of.right = i20;
                                df.right = i20;
                                pf.right = i20;
                                int i21 = zIsVisibleLw ? this.mRestrictedScreenTop + this.mRestrictedScreenHeight : this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                                of.bottom = i21;
                                df.bottom = i21;
                                pf.bottom = i21;
                                if (DEBUG_LAYOUT) {
                                    Slog.v(TAG, String.format("Laying out status bar window: (%d,%d - %d,%d)", Integer.valueOf(pf.left), Integer.valueOf(pf.top), Integer.valueOf(pf.right), Integer.valueOf(pf.bottom)));
                                }
                            } else if ((33554432 & fl) != 0 && attrs.type >= 1 && attrs.type <= 1999) {
                                int i22 = this.mOverscanScreenLeft;
                                of.left = i22;
                                df.left = i22;
                                pf.left = i22;
                                int i23 = this.mOverscanScreenTop;
                                of.top = i23;
                                df.top = i23;
                                pf.top = i23;
                                int i24 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                                of.right = i24;
                                df.right = i24;
                                pf.right = i24;
                                int i25 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                                of.bottom = i25;
                                df.bottom = i25;
                                pf.bottom = i25;
                            } else if (!canHideNavigationBar() || (sysUiFl & 512) == 0 || (attrs.type != 2037 && (attrs.type < 1 || attrs.type > 1999))) {
                                int i26 = this.mRestrictedOverscanScreenLeft;
                                df.left = i26;
                                pf.left = i26;
                                int i27 = this.mRestrictedOverscanScreenTop;
                                df.top = i27;
                                pf.top = i27;
                                int i28 = this.mRestrictedOverscanScreenLeft + this.mRestrictedOverscanScreenWidth;
                                df.right = i28;
                                pf.right = i28;
                                int i29 = this.mRestrictedOverscanScreenTop + this.mRestrictedOverscanScreenHeight;
                                df.bottom = i29;
                                pf.bottom = i29;
                                of.left = this.mUnrestrictedScreenLeft;
                                of.top = this.mUnrestrictedScreenTop;
                                of.right = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                                of.bottom = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                            } else {
                                int i30 = this.mOverscanScreenLeft;
                                df.left = i30;
                                pf.left = i30;
                                int i31 = this.mOverscanScreenTop;
                                df.top = i31;
                                pf.top = i31;
                                int i32 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                                df.right = i32;
                                pf.right = i32;
                                int i33 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                                df.bottom = i33;
                                pf.bottom = i33;
                                of.left = this.mUnrestrictedScreenLeft;
                                of.top = this.mUnrestrictedScreenTop;
                                of.right = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                                of.bottom = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                            }
                            if ((fl & 1024) != 0) {
                                cf.left = this.mRestrictedScreenLeft;
                                cf.top = this.mRestrictedScreenTop;
                                cf.right = this.mRestrictedScreenLeft + this.mRestrictedScreenWidth;
                                cf.bottom = this.mRestrictedScreenTop + this.mRestrictedScreenHeight;
                            } else if (win.isVoiceInteraction()) {
                                cf.left = this.mVoiceContentLeft;
                                cf.top = this.mVoiceContentTop;
                                cf.right = this.mVoiceContentRight;
                                cf.bottom = this.mVoiceContentBottom;
                            } else if (adjust != 16) {
                                cf.left = this.mDockLeft;
                                cf.top = this.mDockTop;
                                cf.right = this.mDockRight;
                                cf.bottom = this.mDockBottom;
                            } else {
                                cf.left = this.mContentLeft;
                                cf.top = this.mContentTop;
                                cf.right = this.mContentRight;
                                cf.bottom = this.mContentBottom;
                            }
                            applyStableConstraints(sysUiFl, fl, cf);
                            if (adjust != 48) {
                                vf.left = this.mCurLeft;
                                vf.top = this.mCurTop;
                                vf.right = this.mCurRight;
                                vf.bottom = this.mCurBottom;
                            } else {
                                vf.set(cf);
                            }
                        }
                    } else if ((fl & 256) != 0 || (sysUiFl & 1536) != 0) {
                        if (DEBUG_LAYOUT) {
                            Slog.v(TAG, "layoutWindowLw(" + attrs.getTitle() + "): IN_SCREEN, type=" + attrs.type + ", flag=" + fl + ", canHideNavigationBar=" + canHideNavigationBar() + ", sysUiFl=" + sysUiFl);
                        }
                        if (attrs.type == 2014 || attrs.type == 2017 || attrs.type == 2020) {
                            int i34 = zIsVisibleLw ? this.mDockLeft : this.mUnrestrictedScreenLeft;
                            cf.left = i34;
                            of.left = i34;
                            df.left = i34;
                            pf.left = i34;
                            int i35 = this.mUnrestrictedScreenTop;
                            cf.top = i35;
                            of.top = i35;
                            df.top = i35;
                            pf.top = i35;
                            int i36 = zIsVisibleLw ? this.mRestrictedScreenLeft + this.mRestrictedScreenWidth : this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                            cf.right = i36;
                            of.right = i36;
                            df.right = i36;
                            pf.right = i36;
                            int i37 = zIsVisibleLw ? this.mRestrictedScreenTop + this.mRestrictedScreenHeight : this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                            cf.bottom = i37;
                            of.bottom = i37;
                            df.bottom = i37;
                            pf.bottom = i37;
                            if (DEBUG_LAYOUT) {
                                Slog.v(TAG, String.format("Laying out IN_SCREEN status bar window: (%d,%d - %d,%d)", Integer.valueOf(pf.left), Integer.valueOf(pf.top), Integer.valueOf(pf.right), Integer.valueOf(pf.bottom)));
                            }
                        } else if (attrs.type == 2019 || attrs.type == 2024) {
                            int i38 = this.mUnrestrictedScreenLeft;
                            of.left = i38;
                            df.left = i38;
                            pf.left = i38;
                            int i39 = this.mUnrestrictedScreenTop;
                            of.top = i39;
                            df.top = i39;
                            pf.top = i39;
                            int i40 = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                            of.right = i40;
                            df.right = i40;
                            pf.right = i40;
                            int i41 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                            of.bottom = i41;
                            df.bottom = i41;
                            pf.bottom = i41;
                            if (DEBUG_LAYOUT) {
                                Slog.v(TAG, String.format("Laying out navigation bar window: (%d,%d - %d,%d)", Integer.valueOf(pf.left), Integer.valueOf(pf.top), Integer.valueOf(pf.right), Integer.valueOf(pf.bottom)));
                            }
                        } else if ((attrs.type == 2015 || attrs.type == 2021 || attrs.type == 2036) && (fl & 1024) != 0) {
                            int i42 = this.mOverscanScreenLeft;
                            cf.left = i42;
                            of.left = i42;
                            df.left = i42;
                            pf.left = i42;
                            int i43 = this.mOverscanScreenTop;
                            cf.top = i43;
                            of.top = i43;
                            df.top = i43;
                            pf.top = i43;
                            int i44 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                            cf.right = i44;
                            of.right = i44;
                            df.right = i44;
                            pf.right = i44;
                            int i45 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                            cf.bottom = i45;
                            of.bottom = i45;
                            df.bottom = i45;
                            pf.bottom = i45;
                        } else if (attrs.type == 2021) {
                            int i46 = this.mOverscanScreenLeft;
                            cf.left = i46;
                            of.left = i46;
                            df.left = i46;
                            pf.left = i46;
                            int i47 = this.mOverscanScreenTop;
                            cf.top = i47;
                            of.top = i47;
                            df.top = i47;
                            pf.top = i47;
                            int i48 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                            cf.right = i48;
                            of.right = i48;
                            df.right = i48;
                            pf.right = i48;
                            int i49 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                            cf.bottom = i49;
                            of.bottom = i49;
                            df.bottom = i49;
                            pf.bottom = i49;
                        } else if (attrs.type == 2013) {
                            int i50 = this.mOverscanScreenLeft;
                            df.left = i50;
                            pf.left = i50;
                            int i51 = this.mOverscanScreenTop;
                            df.top = i51;
                            pf.top = i51;
                            int i52 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                            df.right = i52;
                            pf.right = i52;
                            int i53 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                            df.bottom = i53;
                            pf.bottom = i53;
                            int i54 = this.mUnrestrictedScreenLeft;
                            cf.left = i54;
                            of.left = i54;
                            int i55 = this.mUnrestrictedScreenTop;
                            cf.top = i55;
                            of.top = i55;
                            int i56 = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                            cf.right = i56;
                            of.right = i56;
                            int i57 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                            cf.bottom = i57;
                            of.bottom = i57;
                        } else if ((33554432 & fl) != 0 && attrs.type >= 1 && attrs.type <= 1999) {
                            int i58 = this.mOverscanScreenLeft;
                            cf.left = i58;
                            of.left = i58;
                            df.left = i58;
                            pf.left = i58;
                            int i59 = this.mOverscanScreenTop;
                            cf.top = i59;
                            of.top = i59;
                            df.top = i59;
                            pf.top = i59;
                            int i60 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                            cf.right = i60;
                            of.right = i60;
                            df.right = i60;
                            pf.right = i60;
                            int i61 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                            cf.bottom = i61;
                            of.bottom = i61;
                            df.bottom = i61;
                            pf.bottom = i61;
                        } else if (canHideNavigationBar() && (sysUiFl & 512) != 0 && (attrs.type == 2000 || attrs.type == 2005 || attrs.type == 2034 || attrs.type == 2033 || (attrs.type >= 1 && attrs.type <= 1999))) {
                            int i62 = this.mUnrestrictedScreenLeft;
                            cf.left = i62;
                            of.left = i62;
                            df.left = i62;
                            pf.left = i62;
                            int i63 = this.mUnrestrictedScreenTop;
                            cf.top = i63;
                            of.top = i63;
                            df.top = i63;
                            pf.top = i63;
                            int i64 = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                            cf.right = i64;
                            of.right = i64;
                            df.right = i64;
                            pf.right = i64;
                            int i65 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                            cf.bottom = i65;
                            of.bottom = i65;
                            df.bottom = i65;
                            pf.bottom = i65;
                        } else if ((sysUiFl & 1024) != 0) {
                            int i66 = this.mRestrictedScreenLeft;
                            of.left = i66;
                            df.left = i66;
                            pf.left = i66;
                            int i67 = this.mRestrictedScreenTop;
                            of.top = i67;
                            df.top = i67;
                            pf.top = i67;
                            int i68 = this.mRestrictedScreenLeft + this.mRestrictedScreenWidth;
                            of.right = i68;
                            df.right = i68;
                            pf.right = i68;
                            int i69 = this.mRestrictedScreenTop + this.mRestrictedScreenHeight;
                            of.bottom = i69;
                            df.bottom = i69;
                            pf.bottom = i69;
                            if (adjust != 16) {
                                cf.left = this.mDockLeft;
                                cf.top = this.mDockTop;
                                cf.right = this.mDockRight;
                                cf.bottom = this.mDockBottom;
                            } else {
                                cf.left = this.mContentLeft;
                                cf.top = this.mContentTop;
                                cf.right = this.mContentRight;
                                cf.bottom = this.mContentBottom;
                            }
                        } else {
                            int i70 = this.mRestrictedScreenLeft;
                            cf.left = i70;
                            of.left = i70;
                            df.left = i70;
                            pf.left = i70;
                            int i71 = this.mRestrictedScreenTop;
                            cf.top = i71;
                            of.top = i71;
                            df.top = i71;
                            pf.top = i71;
                            int i72 = this.mRestrictedScreenLeft + this.mRestrictedScreenWidth;
                            cf.right = i72;
                            of.right = i72;
                            df.right = i72;
                            pf.right = i72;
                            int i73 = this.mRestrictedScreenTop + this.mRestrictedScreenHeight;
                            cf.bottom = i73;
                            of.bottom = i73;
                            df.bottom = i73;
                            pf.bottom = i73;
                        }
                        applyStableConstraints(sysUiFl, fl, cf);
                        if (adjust != 48) {
                            vf.left = this.mCurLeft;
                            vf.top = this.mCurTop;
                            vf.right = this.mCurRight;
                            vf.bottom = this.mCurBottom;
                        } else {
                            vf.set(cf);
                        }
                    } else if (attached != null) {
                        if (DEBUG_LAYOUT) {
                            Slog.v(TAG, "layoutWindowLw(" + attrs.getTitle() + "): attached to " + attached);
                        }
                        setAttachedWindowFrames(win, fl, adjust, attached, false, pf, df, of, cf, vf);
                    } else {
                        if (DEBUG_LAYOUT) {
                            Slog.v(TAG, "layoutWindowLw(" + attrs.getTitle() + "): normal window");
                        }
                        if (attrs.type == 2014 || attrs.type == 2020) {
                            int i74 = this.mRestrictedScreenLeft;
                            cf.left = i74;
                            of.left = i74;
                            df.left = i74;
                            pf.left = i74;
                            int i75 = this.mRestrictedScreenTop;
                            cf.top = i75;
                            of.top = i75;
                            df.top = i75;
                            pf.top = i75;
                            int i76 = this.mRestrictedScreenLeft + this.mRestrictedScreenWidth;
                            cf.right = i76;
                            of.right = i76;
                            df.right = i76;
                            pf.right = i76;
                            int i77 = this.mRestrictedScreenTop + this.mRestrictedScreenHeight;
                            cf.bottom = i77;
                            of.bottom = i77;
                            df.bottom = i77;
                            pf.bottom = i77;
                        } else if (attrs.type == 2005 || attrs.type == 2003) {
                            int i78 = this.mStableLeft;
                            cf.left = i78;
                            of.left = i78;
                            df.left = i78;
                            pf.left = i78;
                            int i79 = this.mStableTop;
                            cf.top = i79;
                            of.top = i79;
                            df.top = i79;
                            pf.top = i79;
                            int i80 = this.mStableRight;
                            cf.right = i80;
                            of.right = i80;
                            df.right = i80;
                            pf.right = i80;
                            int i81 = this.mStableBottom;
                            cf.bottom = i81;
                            of.bottom = i81;
                            df.bottom = i81;
                            pf.bottom = i81;
                        } else {
                            pf.left = this.mContentLeft;
                            pf.top = this.mContentTop;
                            pf.right = this.mContentRight;
                            pf.bottom = this.mContentBottom;
                            if (win.isVoiceInteraction()) {
                                int i82 = this.mVoiceContentLeft;
                                cf.left = i82;
                                of.left = i82;
                                df.left = i82;
                                int i83 = this.mVoiceContentTop;
                                cf.top = i83;
                                of.top = i83;
                                df.top = i83;
                                int i84 = this.mVoiceContentRight;
                                cf.right = i84;
                                of.right = i84;
                                df.right = i84;
                                int i85 = this.mVoiceContentBottom;
                                cf.bottom = i85;
                                of.bottom = i85;
                                df.bottom = i85;
                            } else if (adjust != 16) {
                                int i86 = this.mDockLeft;
                                cf.left = i86;
                                of.left = i86;
                                df.left = i86;
                                int i87 = this.mDockTop;
                                cf.top = i87;
                                of.top = i87;
                                df.top = i87;
                                int i88 = this.mDockRight;
                                cf.right = i88;
                                of.right = i88;
                                df.right = i88;
                                int i89 = this.mDockBottom;
                                cf.bottom = i89;
                                of.bottom = i89;
                                df.bottom = i89;
                            } else {
                                int i90 = this.mContentLeft;
                                cf.left = i90;
                                of.left = i90;
                                df.left = i90;
                                int i91 = this.mContentTop;
                                cf.top = i91;
                                of.top = i91;
                                df.top = i91;
                                int i92 = this.mContentRight;
                                cf.right = i92;
                                of.right = i92;
                                df.right = i92;
                                int i93 = this.mContentBottom;
                                cf.bottom = i93;
                                of.bottom = i93;
                                df.bottom = i93;
                            }
                            if (adjust != 48) {
                                vf.left = this.mCurLeft;
                                vf.top = this.mCurTop;
                                vf.right = this.mCurRight;
                                vf.bottom = this.mCurBottom;
                            } else {
                                vf.set(cf);
                            }
                        }
                    }
                }
            } else if (attached != null) {
                setAttachedWindowFrames(win, fl, adjust, attached, true, pf, df, of, cf, vf);
            } else {
                int i94 = this.mOverscanScreenLeft;
                cf.left = i94;
                of.left = i94;
                df.left = i94;
                pf.left = i94;
                int i95 = this.mOverscanScreenTop;
                cf.top = i95;
                of.top = i95;
                df.top = i95;
                pf.top = i95;
                int i96 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                cf.right = i96;
                of.right = i96;
                df.right = i96;
                pf.right = i96;
                int i97 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                cf.bottom = i97;
                of.bottom = i97;
                df.bottom = i97;
                pf.bottom = i97;
            }
            if ((fl & 512) != 0 && attrs.type != 2010 && !win.isInMultiWindowMode()) {
                df.top = -10000;
                df.left = -10000;
                df.bottom = 10000;
                df.right = 10000;
                if (attrs.type != 2013) {
                    vf.top = -10000;
                    vf.left = -10000;
                    cf.top = -10000;
                    cf.left = -10000;
                    of.top = -10000;
                    of.left = -10000;
                    vf.bottom = 10000;
                    vf.right = 10000;
                    cf.bottom = 10000;
                    cf.right = 10000;
                    of.bottom = 10000;
                    of.right = 10000;
                }
            }
            boolean useOutsets = shouldUseOutsets(attrs, fl);
            if (isDefaultDisplay && useOutsets) {
                osf = mTmpOutsetFrame;
                osf.set(cf.left, cf.top, cf.right, cf.bottom);
                int outset = ScreenShapeHelper.getWindowOutsetBottomPx(this.mContext.getResources());
                if (outset > 0) {
                    int rotation = this.mDisplayRotation;
                    if (rotation == 0) {
                        osf.bottom += outset;
                    } else if (rotation == 1) {
                        osf.right += outset;
                    } else if (rotation == 2) {
                        osf.top -= outset;
                    } else if (rotation == 3) {
                        osf.left -= outset;
                    }
                    if (DEBUG_LAYOUT) {
                        Slog.v(TAG, "applying bottom outset of " + outset + " with rotation " + rotation + ", result: " + osf);
                    }
                }
            }
            if (DEBUG_LAYOUT) {
                Slog.v(TAG, "Compute frame " + attrs.getTitle() + ": sim=#" + Integer.toHexString(sim) + " attach=" + attached + " type=" + attrs.type + String.format(" flags=0x%08x", Integer.valueOf(fl)) + " pf=" + pf.toShortString() + " df=" + df.toShortString() + " of=" + of.toShortString() + " cf=" + cf.toShortString() + " vf=" + vf.toShortString() + " dcf=" + dcf.toShortString() + " sf=" + sf.toShortString() + " osf=" + (osf == null ? "null" : osf.toShortString()));
            }
            win.computeFrameLw(pf, df, of, cf, vf, dcf, sf, osf);
            if (attrs.type == 2011 && win.isVisibleOrBehindKeyguardLw() && win.isDisplayedLw() && !win.getGivenInsetsPendingLw()) {
                setLastInputMethodWindowLw(null, null);
                offsetInputMethodWindowLw(win);
            }
            if (attrs.type == 2031 && win.isVisibleOrBehindKeyguardLw() && !win.getGivenInsetsPendingLw()) {
                offsetVoiceInputWindowLw(win);
            }
        }
    }

    private void offsetInputMethodWindowLw(WindowManagerPolicy.WindowState win) {
        int top = Math.max(win.getDisplayFrameLw().top, win.getContentFrameLw().top) + win.getGivenContentInsetsLw().top;
        if (this.mContentBottom > top) {
            this.mContentBottom = top;
        }
        if (this.mVoiceContentBottom > top) {
            this.mVoiceContentBottom = top;
        }
        int top2 = win.getVisibleFrameLw().top + win.getGivenVisibleInsetsLw().top;
        if (this.mCurBottom > top2) {
            this.mCurBottom = top2;
        }
        if (DEBUG_LAYOUT) {
            Slog.v(TAG, "Input method: mDockBottom=" + this.mDockBottom + " mContentBottom=" + this.mContentBottom + " mCurBottom=" + this.mCurBottom);
        }
    }

    private void offsetVoiceInputWindowLw(WindowManagerPolicy.WindowState win) {
        int top = Math.max(win.getDisplayFrameLw().top, win.getContentFrameLw().top) + win.getGivenContentInsetsLw().top;
        if (this.mVoiceContentBottom <= top) {
            return;
        }
        this.mVoiceContentBottom = top;
    }

    public void finishLayoutLw() {
    }

    public void beginPostLayoutPolicyLw(int displayWidth, int displayHeight) {
        boolean zIsShowing = false;
        this.mTopFullscreenOpaqueWindowState = null;
        this.mTopFullscreenOpaqueOrDimmingWindowState = null;
        this.mTopDockedOpaqueWindowState = null;
        this.mTopDockedOpaqueOrDimmingWindowState = null;
        this.mAppsToBeHidden.clear();
        this.mAppsThatDismissKeyguard.clear();
        this.mForceStatusBar = false;
        this.mForceStatusBarFromKeyguard = false;
        this.mForceStatusBarTransparent = false;
        this.mForcingShowNavBar = false;
        this.mForcingShowNavBarLayer = -1;
        this.mHideLockScreen = false;
        this.mAllowLockscreenWhenOn = false;
        this.mDismissKeyguard = 0;
        this.mShowingLockscreen = false;
        this.mShowingDream = false;
        this.mWinShowWhenLocked = null;
        this.mKeyguardSecure = isKeyguardSecure(this.mCurrentUserId);
        if (this.mKeyguardSecure && this.mKeyguardDelegate != null) {
            zIsShowing = this.mKeyguardDelegate.isShowing();
        }
        this.mKeyguardSecureIncludingHidden = zIsShowing;
        if (this.mDreamManagerInternal != null) {
            return;
        }
        this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
    }

    public void applyPostLayoutPolicyLw(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, WindowManagerPolicy.WindowState attached) {
        if (DEBUG_LAYOUT) {
            Slog.i(TAG, "applyPostLayoutPolicyLw Win " + win + ": isVisibleOrBehindKeyguardLw=" + win.isVisibleOrBehindKeyguardLw() + ", win.isVisibleLw()=" + win.isVisibleLw() + ", win.hasDrawnLw()=" + win.hasDrawnLw() + ", win.isDrawnLw()=" + win.isDrawnLw() + ", attrs.type=" + attrs.type + ", attrs.privateFlags=" + attrs.privateFlags + ", fl=" + PolicyControl.getWindowFlags(win, attrs) + ", stackId=" + win.getStackId() + ", mTopFullscreenOpaqueWindowState=" + this.mTopFullscreenOpaqueWindowState + ", win.isVisibleOrBehindKeyguardLw()=" + win.isVisibleOrBehindKeyguardLw() + ", win.isGoneForLayoutLw()=" + win.isGoneForLayoutLw() + ", attached=" + attached + ", isFullscreen=" + isFullscreen(attrs) + ", normallyFullscreenWindows=" + ActivityManager.StackId.normallyFullscreenWindows(win.getStackId()) + ", mDreamingLockscreen=" + this.mDreamingLockscreen + ", mShowingDream=" + this.mShowingDream);
        }
        int fl = PolicyControl.getWindowFlags(win, attrs);
        if (this.mTopFullscreenOpaqueWindowState == null && win.isVisibleLw() && attrs.type == 2011) {
            this.mForcingShowNavBar = true;
            this.mForcingShowNavBarLayer = win.getSurfaceLayer();
        }
        if (attrs.type == 2000) {
            if ((attrs.privateFlags & 1024) != 0) {
                this.mForceStatusBarFromKeyguard = true;
                this.mShowingLockscreen = true;
            }
            if ((attrs.privateFlags & 4096) != 0) {
                this.mForceStatusBarTransparent = true;
            }
        }
        boolean appWindow = attrs.type >= 1 && attrs.type < 2000;
        boolean showWhenLocked = (524288 & fl) != 0;
        boolean dismissKeyguard = (4194304 & fl) != 0;
        int stackId = win.getStackId();
        if (this.mTopFullscreenOpaqueWindowState == null && win.isVisibleOrBehindKeyguardLw() && !win.isGoneForLayoutLw()) {
            if ((fl & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0) {
                if ((attrs.privateFlags & 1024) != 0) {
                    this.mForceStatusBarFromKeyguard = true;
                } else {
                    this.mForceStatusBar = true;
                }
            }
            if (attrs.type == 2023 && (!this.mDreamingLockscreen || (win.isVisibleLw() && win.hasDrawnLw()))) {
                this.mShowingDream = true;
                appWindow = true;
            }
            IApplicationToken appToken = win.getAppToken();
            if (appWindow && attached == null) {
                if (showWhenLocked) {
                    this.mAppsToBeHidden.remove(appToken);
                    this.mAppsThatDismissKeyguard.remove(appToken);
                    if (this.mAppsToBeHidden.isEmpty()) {
                        if (dismissKeyguard && !this.mKeyguardSecure) {
                            this.mAppsThatDismissKeyguard.add(appToken);
                        } else if (win.isDrawnLw() || win.hasAppShownWindows()) {
                            this.mWinShowWhenLocked = win;
                            this.mHideLockScreen = true;
                            this.mForceStatusBarFromKeyguard = false;
                        }
                    }
                } else if (dismissKeyguard) {
                    if (this.mKeyguardSecure) {
                        this.mAppsToBeHidden.add(appToken);
                    } else {
                        this.mAppsToBeHidden.remove(appToken);
                    }
                    this.mAppsThatDismissKeyguard.add(appToken);
                } else {
                    this.mAppsToBeHidden.add(appToken);
                }
                if (isFullscreen(attrs) && ActivityManager.StackId.normallyFullscreenWindows(stackId)) {
                    if (DEBUG_LAYOUT) {
                        Slog.v(TAG, "Fullscreen window: " + win);
                    }
                    this.mTopFullscreenOpaqueWindowState = win;
                    if (this.mTopFullscreenOpaqueOrDimmingWindowState == null) {
                        this.mTopFullscreenOpaqueOrDimmingWindowState = win;
                    }
                    if (!this.mAppsThatDismissKeyguard.isEmpty() && this.mDismissKeyguard == 0) {
                        if (DEBUG_LAYOUT) {
                            Slog.v(TAG, "Setting mDismissKeyguard true by win " + win);
                        }
                        this.mDismissKeyguard = (this.mWinDismissingKeyguard == win && this.mSecureDismissingKeyguard == this.mKeyguardSecure) ? 2 : 1;
                        this.mWinDismissingKeyguard = win;
                        this.mSecureDismissingKeyguard = this.mKeyguardSecure;
                        this.mForceStatusBarFromKeyguard = this.mShowingLockscreen ? this.mKeyguardSecure : false;
                    } else if (this.mAppsToBeHidden.isEmpty() && showWhenLocked && (win.isDrawnLw() || win.hasAppShownWindows())) {
                        if (DEBUG_LAYOUT) {
                            Slog.v(TAG, "Setting mHideLockScreen to true by win " + win);
                        }
                        this.mHideLockScreen = true;
                        this.mForceStatusBarFromKeyguard = false;
                    }
                    if ((fl & 1) != 0) {
                        this.mAllowLockscreenWhenOn = true;
                    }
                }
                if (!this.mKeyguardHidden && this.mWinShowWhenLocked != null && this.mWinShowWhenLocked.getAppToken() != win.getAppToken() && (attrs.flags & PackageManagerService.DumpState.DUMP_FROZEN) == 0) {
                    win.hideLw(false);
                }
            }
        } else if (this.mTopFullscreenOpaqueWindowState == null && this.mWinShowWhenLocked == null && win.isAnimatingLw() && appWindow && showWhenLocked && this.mKeyguardHidden) {
            this.mHideLockScreen = true;
            this.mWinShowWhenLocked = win;
        }
        boolean reallyVisible = win.isVisibleOrBehindKeyguardLw() && !win.isGoneForLayoutLw();
        if (this.mTopFullscreenOpaqueOrDimmingWindowState == null && reallyVisible && win.isDimming() && ActivityManager.StackId.normallyFullscreenWindows(stackId)) {
            this.mTopFullscreenOpaqueOrDimmingWindowState = win;
        }
        if (this.mTopDockedOpaqueWindowState == null && reallyVisible && appWindow && attached == null && isFullscreen(attrs) && stackId == 3) {
            this.mTopDockedOpaqueWindowState = win;
            if (this.mTopDockedOpaqueOrDimmingWindowState == null) {
                this.mTopDockedOpaqueOrDimmingWindowState = win;
            }
        }
        if (this.mTopDockedOpaqueOrDimmingWindowState == null && reallyVisible && win.isDimming() && stackId == 3) {
            this.mTopDockedOpaqueOrDimmingWindowState = win;
        }
    }

    private boolean isFullscreen(WindowManager.LayoutParams attrs) {
        return attrs.x == 0 && attrs.y == 0 && attrs.width == -1 && attrs.height == -1;
    }

    public int finishPostLayoutPolicyLw() {
        boolean shouldBeTransparent;
        if (this.mWinShowWhenLocked != null && this.mTopFullscreenOpaqueWindowState != null && this.mWinShowWhenLocked.getAppToken() != this.mTopFullscreenOpaqueWindowState.getAppToken() && isKeyguardLocked()) {
            this.mWinShowWhenLocked.getAttrs().flags |= PackageManagerService.DumpState.DUMP_DEXOPT;
            if (this.mTopFullscreenOpaqueWindowState != null) {
                this.mTopFullscreenOpaqueWindowState.hideLw(false);
            }
            this.mTopFullscreenOpaqueWindowState = this.mWinShowWhenLocked;
        }
        boolean topIsFullscreen = false;
        WindowManager.LayoutParams attrs = this.mTopFullscreenOpaqueWindowState != null ? this.mTopFullscreenOpaqueWindowState.getAttrs() : null;
        if (!this.mShowingDream) {
            this.mDreamingLockscreen = this.mShowingLockscreen;
            if (this.mDreamingSleepTokenNeeded) {
                this.mDreamingSleepTokenNeeded = false;
                this.mHandler.obtainMessage(15, 0, 1).sendToTarget();
            }
        } else if (!this.mDreamingSleepTokenNeeded) {
            this.mDreamingSleepTokenNeeded = true;
            this.mHandler.obtainMessage(15, 1, 1).sendToTarget();
        }
        if (this.mStatusBar != null) {
            if (DEBUG_LAYOUT) {
                Slog.i(TAG, "force=" + this.mForceStatusBar + " forcefkg=" + this.mForceStatusBarFromKeyguard + " top=" + this.mTopFullscreenOpaqueWindowState + " dream=" + (this.mDreamManagerInternal != null ? Boolean.valueOf(this.mDreamManagerInternal.isDreaming()) : "null"));
            }
            if (!this.mForceStatusBarTransparent || this.mForceStatusBar) {
                shouldBeTransparent = false;
            } else {
                shouldBeTransparent = !this.mForceStatusBarFromKeyguard;
            }
            if (!shouldBeTransparent) {
                this.mStatusBarController.setShowTransparent(false);
            } else if (!this.mStatusBar.isVisibleLw()) {
                this.mStatusBarController.setShowTransparent(true);
            }
            WindowManager.LayoutParams statusBarAttrs = this.mStatusBar.getAttrs();
            boolean statusBarExpanded = statusBarAttrs.height == -1 && statusBarAttrs.width == -1;
            if (this.mDreamManagerInternal != null && this.mDreamManagerInternal.isDreaming()) {
                if (DEBUG_LAYOUT) {
                    Slog.v(TAG, "** HIDING status bar: dreaming");
                }
                if (this.mStatusBarController.setBarShowingLw(false)) {
                    changes = 1;
                }
            } else if (this.mForceStatusBar || this.mForceStatusBarFromKeyguard || this.mForceStatusBarTransparent || statusBarExpanded) {
                if (DEBUG_LAYOUT) {
                    Slog.v(TAG, "Showing status bar: forced");
                }
                changes = this.mStatusBarController.setBarShowingLw(true) ? 1 : 0;
                topIsFullscreen = this.mTopIsFullscreen ? this.mStatusBar.isAnimatingLw() : false;
                if (this.mForceStatusBarFromKeyguard && this.mStatusBarController.isTransientShowing()) {
                    this.mStatusBarController.updateVisibilityLw(false, this.mLastSystemUiFlags, this.mLastSystemUiFlags);
                }
                if (statusBarExpanded && this.mHideNavigationBar == 0 && this.mNavigationBar != null && this.mNavigationBarController.setBarShowingLw(true)) {
                    changes |= 1;
                }
            } else if (this.mTopFullscreenOpaqueWindowState != null) {
                int fl = PolicyControl.getWindowFlags(null, attrs);
                if (localLOGV) {
                    Slog.d(TAG, "frame: " + this.mTopFullscreenOpaqueWindowState.getFrameLw() + " shown position: " + this.mTopFullscreenOpaqueWindowState.getShownPositionLw());
                    Slog.d(TAG, "attr: " + this.mTopFullscreenOpaqueWindowState.getAttrs() + " lp.flags=0x" + Integer.toHexString(fl));
                }
                topIsFullscreen = ((fl & 1024) == 0 && (this.mLastSystemUiFlags & 4) == 0) ? false : true;
                if (this.mStatusBarController.isTransientShowing()) {
                    if (this.mStatusBarController.setBarShowingLw(true)) {
                        changes = 1;
                    }
                } else if (!topIsFullscreen || this.mWindowManagerInternal.isStackVisible(2) || this.mWindowManagerInternal.isStackVisible(3)) {
                    if (DEBUG_LAYOUT) {
                        Slog.v(TAG, "** SHOWING status bar: top is not fullscreen");
                    }
                    if (this.mStatusBarController.setBarShowingLw(true)) {
                        changes = 1;
                    }
                } else {
                    if (DEBUG_LAYOUT) {
                        Slog.v(TAG, "** HIDING status bar");
                    }
                    if (this.mStatusBarController.setBarShowingLw(false)) {
                        changes = 1;
                    } else if (DEBUG_LAYOUT) {
                        Slog.v(TAG, "Status bar already hiding");
                    }
                }
            }
        }
        if (this.mTopIsFullscreen != topIsFullscreen) {
            if (!topIsFullscreen) {
                changes |= 1;
            }
            this.mTopIsFullscreen = topIsFullscreen;
        }
        if (this.mKeyguardDelegate != null && this.mStatusBar != null) {
            if (localLOGV) {
                Slog.v(TAG, "finishPostLayoutPolicyLw: mHideKeyguard=" + this.mHideLockScreen + " mDismissKeyguard=" + this.mDismissKeyguard + " mKeyguardDelegate.isSecure()= " + this.mKeyguardDelegate.isSecure(this.mCurrentUserId));
            }
            if (this.mDismissKeyguard != 0 && !this.mKeyguardSecure) {
                this.mKeyguardHidden = true;
                if (setKeyguardOccludedLw(true)) {
                    changes |= 7;
                }
                if (this.mKeyguardDelegate.isShowing()) {
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            PhoneWindowManager.this.mKeyguardDelegate.keyguardDone(false, false);
                        }
                    });
                }
            } else if (this.mHideLockScreen) {
                this.mKeyguardHidden = true;
                this.mWinDismissingKeyguard = null;
                if (setKeyguardOccludedLw(true)) {
                    changes |= 7;
                }
            } else if (this.mDismissKeyguard != 0) {
                this.mKeyguardHidden = false;
                if (setKeyguardOccludedLw(false)) {
                    changes |= 7;
                }
                if (this.mDismissKeyguard == 1) {
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            PhoneWindowManager.this.mKeyguardDelegate.dismiss();
                        }
                    });
                }
            } else {
                this.mWinDismissingKeyguard = null;
                this.mSecureDismissingKeyguard = false;
                this.mKeyguardHidden = false;
                if (setKeyguardOccludedLw(false)) {
                    changes |= 7;
                }
            }
        }
        if ((updateSystemUiVisibilityLw() & SYSTEM_UI_CHANGING_LAYOUT) != 0) {
            changes |= 1;
        }
        updateLockScreenTimeout();
        return changes;
    }

    private boolean setKeyguardOccludedLw(boolean isOccluded) {
        boolean wasOccluded = this.mKeyguardOccluded;
        boolean showing = this.mKeyguardDelegate.isShowing();
        if (wasOccluded && !isOccluded && showing) {
            this.mKeyguardOccluded = false;
            this.mKeyguardDelegate.setOccluded(false);
            this.mStatusBar.getAttrs().privateFlags |= 1024;
            return true;
        }
        if (wasOccluded || !isOccluded || !showing) {
            return false;
        }
        this.mKeyguardOccluded = true;
        this.mKeyguardDelegate.setOccluded(true);
        this.mStatusBar.getAttrs().privateFlags &= -1025;
        this.mStatusBar.getAttrs().flags &= -1048577;
        return true;
    }

    private boolean isStatusBarKeyguard() {
        return (this.mStatusBar == null || (this.mStatusBar.getAttrs().privateFlags & 1024) == 0) ? false : true;
    }

    public boolean allowAppAnimationsLw() {
        if (isStatusBarKeyguard() || this.mShowingDream) {
            return false;
        }
        return true;
    }

    public int focusChangedLw(WindowManagerPolicy.WindowState lastFocus, WindowManagerPolicy.WindowState newFocus) {
        this.mFocusedWindow = newFocus;
        return (updateSystemUiVisibilityLw() & SYSTEM_UI_CHANGING_LAYOUT) != 0 ? 1 : 0;
    }

    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        int newLidState = lidOpen ? 1 : 0;
        if (newLidState == this.mLidState) {
            return;
        }
        this.mLidState = newLidState;
        applyLidSwitchState();
        updateRotation(true);
        if (lidOpen) {
            wakeUp(SystemClock.uptimeMillis(), this.mAllowTheaterModeWakeFromLidSwitch, "android.policy:LID");
        } else {
            if (this.mLidControlsSleep) {
                return;
            }
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    public void notifyCameraLensCoverSwitchChanged(long whenNanos, boolean lensCovered) {
        Intent intent;
        int lensCoverState = lensCovered ? 1 : 0;
        if (this.mCameraLensCoverState == lensCoverState) {
            return;
        }
        if (this.mCameraLensCoverState == 1 && lensCoverState == 0) {
            boolean keyguardActive = this.mKeyguardDelegate != null ? this.mKeyguardDelegate.isShowing() : false;
            if (keyguardActive) {
                intent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE");
            } else {
                intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            }
            wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromCameraLens, "android.policy:CAMERA_COVER");
            startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        }
        this.mCameraLensCoverState = lensCoverState;
    }

    void setHdmiPlugged(boolean plugged) {
        if (this.mHdmiPlugged == plugged) {
            return;
        }
        this.mHdmiPlugged = plugged;
        updateRotation(true, true);
        Intent intent = new Intent("android.intent.action.HDMI_PLUGGED");
        intent.addFlags(67108864);
        intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, plugged);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    void initializeHdmiState() throws Throwable {
        FileReader reader;
        if (new File("/sys/devices/virtual/switch/hdmi/state").exists()) {
            this.mHDMIObserver.startObserving("DEVPATH=/devices/virtual/switch/hdmi");
            FileReader reader2 = null;
            try {
                try {
                    reader = new FileReader("/sys/class/switch/hdmi/state");
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e) {
                ex = e;
            } catch (NumberFormatException e2) {
                ex = e2;
            }
            try {
                char[] buf = new char[15];
                int n = reader.read(buf);
                plugged = n > 1 ? Integer.parseInt(new String(buf, 0, n + (-1))) != 0 : false;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (IOException e4) {
                ex = e4;
                reader2 = reader;
                Slog.w(TAG, "Couldn't read hdmi state from /sys/class/switch/hdmi/state: " + ex);
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (NumberFormatException e6) {
                ex = e6;
                reader2 = reader;
                Slog.w(TAG, "Couldn't read hdmi state from /sys/class/switch/hdmi/state: " + ex);
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e7) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e8) {
                    }
                }
                throw th;
            }
        }
        this.mHdmiPlugged = !plugged;
        setHdmiPlugged(this.mHdmiPlugged ? false : true);
    }

    private void takeScreenshot(final int screenshotType) {
        synchronized (this.mScreenshotLock) {
            if (this.mScreenshotConnection != null) {
                return;
            }
            ComponentName serviceComponent = new ComponentName(SYSUI_PACKAGE, SYSUI_SCREENSHOT_SERVICE);
            Intent serviceIntent = new Intent();
            serviceIntent.setComponent(serviceComponent);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (PhoneWindowManager.this.mScreenshotLock) {
                        if (PhoneWindowManager.this.mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain((Handler) null, screenshotType);
                        Handler h = new Handler(PhoneWindowManager.this.mHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg2) {
                                synchronized (PhoneWindowManager.this.mScreenshotLock) {
                                    if (PhoneWindowManager.this.mScreenshotConnection == this) {
                                        PhoneWindowManager.this.mContext.unbindService(PhoneWindowManager.this.mScreenshotConnection);
                                        PhoneWindowManager.this.mScreenshotConnection = null;
                                        PhoneWindowManager.this.mHandler.removeCallbacks(PhoneWindowManager.this.mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg2 = 0;
                        msg.arg1 = 0;
                        if (PhoneWindowManager.this.mStatusBar != null && PhoneWindowManager.this.mStatusBar.isVisibleLw()) {
                            msg.arg1 = 1;
                        }
                        if (PhoneWindowManager.this.mNavigationBar != null && PhoneWindowManager.this.mNavigationBar.isVisibleLw()) {
                            msg.arg2 = 1;
                        }
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    PhoneWindowManager.this.notifyScreenshotError();
                }
            };
            if (this.mContext.bindServiceAsUser(serviceIntent, conn, 33554433, UserHandle.CURRENT)) {
                this.mScreenshotConnection = conn;
                this.mHandler.postDelayed(this.mScreenshotTimeout, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            }
        }
    }

    private void notifyScreenshotError() {
        ComponentName errorComponent = new ComponentName(SYSUI_PACKAGE, SYSUI_SCREENSHOT_ERROR_RECEIVER);
        Intent errorIntent = new Intent("android.intent.action.USER_PRESENT");
        errorIntent.setComponent(errorComponent);
        errorIntent.addFlags(335544320);
        this.mContext.sendBroadcastAsUser(errorIntent, UserHandle.CURRENT);
    }

    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        int result;
        TelecomManager telecomManager;
        TelecomManager telecomManager2;
        if (!this.mSystemBooted || interceptKeyBeforeHandling(event)) {
            return 0;
        }
        if (26 == event.getKeyCode() && this.mIsAlarmBoot) {
            return 0;
        }
        synchronized (this.mKeyDispatchLock) {
            if (1 == this.mKeyDispatcMode) {
                return 0;
            }
            boolean interactive = (536870912 & policyFlags) != 0;
            boolean down = event.getAction() == 0;
            boolean canceled = event.isCanceled();
            int keyCode = event.getKeyCode();
            boolean isInjected = (16777216 & policyFlags) != 0;
            boolean zIsKeyguardShowingAndNotOccluded = this.mKeyguardDelegate == null ? false : interactive ? isKeyguardShowingAndNotOccluded() : this.mKeyguardDelegate.isShowing();
            boolean zIsWakeKey = (policyFlags & 1) == 0 ? event.isWakeKey() : true;
            if (interactive || (isInjected && !zIsWakeKey)) {
                result = 1;
                zIsWakeKey = false;
            } else if (interactive || !shouldDispatchInputWhenNonInteractive()) {
                result = 0;
                if (zIsWakeKey && (!down || !isWakeKeyWhenScreenOff(keyCode))) {
                    zIsWakeKey = false;
                }
            } else {
                result = 1;
            }
            if (isValidGlobalKey(keyCode) && this.mGlobalKeyManager.shouldHandleGlobalKey(keyCode, event)) {
                if (zIsWakeKey) {
                    wakeUp(event.getEventTime(), this.mAllowTheaterModeWakeFromKey, "android.policy:KEY");
                }
                return result;
            }
            boolean useHapticFeedback = down && (policyFlags & 2) != 0 && event.getRepeatCount() == 0;
            if (!IS_USER_BUILD) {
                Log.d(TAG, "interceptKeyTq keycode=" + keyCode + " interactive=" + interactive + " keyguardActive=" + zIsKeyguardShowingAndNotOccluded + " policyFlags=" + Integer.toHexString(policyFlags) + " down =" + down + " canceled = " + canceled + " isWakeKey=" + zIsWakeKey + " mVolumeDownKeyTriggered =" + this.mScreenshotChordVolumeDownKeyTriggered + " mVolumeUpKeyTriggered =" + this.mScreenshotChordVolumeUpKeyTriggered + " result = " + result + " useHapticFeedback = " + useHapticFeedback + " isInjected = " + isInjected);
            }
            switch (keyCode) {
                case 4:
                    if (!down) {
                        boolean handled = this.mBackKeyHandled;
                        cancelPendingBackKeyAction();
                        if (handled) {
                            result &= -2;
                        }
                    } else {
                        this.mBackKeyHandled = false;
                        if (hasLongPressOnBackBehavior()) {
                            Message msg = this.mHandler.obtainMessage(18);
                            msg.setAsynchronous(true);
                            this.mHandler.sendMessageDelayed(msg, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                        }
                    }
                    break;
                case 5:
                    if (down && (telecomManager = getTelecommService()) != null && telecomManager.isRinging()) {
                        Log.i(TAG, "interceptKeyBeforeQueueing: CALL key-down while ringing: Answer the call!");
                        telecomManager.acceptRingingCall();
                        result &= -2;
                    }
                    break;
                case 6:
                    result &= -2;
                    if (down) {
                        TelecomManager telecomManager3 = getTelecommService();
                        boolean hungUp = telecomManager3 != null ? telecomManager3.endCall() : false;
                        if (interactive && !hungUp) {
                            this.mEndCallKeyHandled = false;
                            this.mHandler.postDelayed(this.mEndCallLongPress, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                        } else {
                            this.mEndCallKeyHandled = true;
                        }
                    } else if (!this.mEndCallKeyHandled) {
                        this.mHandler.removeCallbacks(this.mEndCallLongPress);
                        if (!canceled && (((this.mEndcallBehavior & 1) == 0 || !goHome()) && (this.mEndcallBehavior & 2) != 0)) {
                            this.mPowerManager.goToSleep(event.getEventTime(), 4, 0);
                            zIsWakeKey = false;
                        }
                    }
                    break;
                case WindowManagerService.H.WAITING_FOR_DRAWN_TIMEOUT:
                case 25:
                case 164:
                    if (keyCode == 25) {
                        if (!down) {
                            this.mScreenshotChordVolumeDownKeyTriggered = false;
                            cancelPendingScreenshotChordAction();
                        } else if (interactive && !this.mScreenshotChordVolumeDownKeyTriggered && (event.getFlags() & 1024) == 0) {
                            this.mScreenshotChordVolumeDownKeyTriggered = true;
                            this.mScreenshotChordVolumeDownKeyTime = event.getDownTime();
                            this.mScreenshotChordVolumeDownKeyConsumed = false;
                            cancelPendingPowerKeyAction();
                            interceptScreenshotChord();
                        }
                    } else if (keyCode == 24) {
                        if (!IS_USER_BUILD && SystemProperties.get("persist.sys.anr_sys_key").equals("1")) {
                            this.mHandler.postDelayed(this.mKeyRemappingVolumeDownLongPress_Test, 0L);
                        }
                        if (!down) {
                            this.mScreenshotChordVolumeUpKeyTriggered = false;
                            cancelPendingScreenshotChordAction();
                        } else if (interactive && !this.mScreenshotChordVolumeUpKeyTriggered && (event.getFlags() & 1024) == 0) {
                            this.mScreenshotChordVolumeUpKeyTriggered = true;
                            cancelPendingPowerKeyAction();
                            cancelPendingScreenshotChordAction();
                        }
                    }
                    if (!down || (telecomManager2 = getTelecommService()) == null) {
                        if (this.mUseTvRouting) {
                            result |= 1;
                        } else if ((result & 1) == 0) {
                            MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(event, true);
                        }
                        break;
                    } else if (telecomManager2.isRinging()) {
                        Log.i(TAG, "interceptKeyBeforeQueueing: VOLUME key-down while ringing: Silence ringer!");
                        telecomManager2.silenceRinger();
                        result &= -2;
                        break;
                    } else if (telecomManager2.isInCall() && (result & 1) == 0) {
                        MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(event, false);
                        break;
                    }
                    break;
                case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                    result &= -2;
                    zIsWakeKey = false;
                    if (!down) {
                        interceptPowerKeyUp(event, interactive, canceled);
                    } else {
                        interceptPowerKeyDown(event, interactive);
                    }
                    break;
                case HdmiCecKeycode.CEC_KEYCODE_RESERVED:
                case HdmiCecKeycode.CEC_KEYCODE_INITIAL_CONFIGURATION:
                case HdmiCecKeycode.CEC_KEYCODE_SELECT_BROADCAST_TYPE:
                case HdmiCecKeycode.CEC_KEYCODE_SELECT_SOUND_PRESENTATION:
                case 88:
                case 89:
                case 90:
                case 91:
                case 126:
                case 127:
                case 130:
                case NetworkManagementService.NetdResponseCode.DnsProxyQueryResult:
                    if (MediaSessionLegacyHelper.getHelper(this.mContext).isGlobalPriorityActive()) {
                        result &= -2;
                    }
                    if ((result & 1) == 0) {
                        this.mBroadcastWakeLock.acquire();
                        Message msg2 = this.mHandler.obtainMessage(3, new KeyEvent(event));
                        msg2.setAsynchronous(true);
                        msg2.sendToTarget();
                    }
                    break;
                case 171:
                    if (this.mShortPressWindowBehavior == 1 && this.mTvPictureInPictureVisible) {
                        if (!down) {
                            showTvPictureInPictureMenu(event);
                        }
                        result &= -2;
                    }
                    break;
                case NetworkManagementService.NetdResponseCode.ClatdStatusResult:
                    result &= -2;
                    zIsWakeKey = false;
                    if (!this.mPowerManager.isInteractive()) {
                        useHapticFeedback = false;
                    }
                    if (!down) {
                        sleepRelease(event.getEventTime());
                    } else {
                        sleepPress(event.getEventTime());
                    }
                    break;
                case 224:
                    result &= -2;
                    zIsWakeKey = true;
                    break;
                case 231:
                    if ((result & 1) == 0 && !down) {
                        this.mBroadcastWakeLock.acquire();
                        Message msg3 = this.mHandler.obtainMessage(12, zIsKeyguardShowingAndNotOccluded ? 1 : 0, 0);
                        msg3.setAsynchronous(true);
                        msg3.sendToTarget();
                    }
                    break;
                case 276:
                    result &= -2;
                    zIsWakeKey = false;
                    if (!down) {
                        this.mPowerManagerInternal.setUserInactiveOverrideFromWindowManager();
                    }
                    break;
            }
            if (useHapticFeedback) {
                performHapticFeedbackLw(null, 1, false);
            }
            if (zIsWakeKey) {
                wakeUp(event.getEventTime(), this.mAllowTheaterModeWakeFromKey, "android.policy:KEY");
            }
            return result;
        }
    }

    private static boolean isValidGlobalKey(int keyCode) {
        switch (keyCode) {
            case WindowManagerService.H.DO_ANIMATION_CALLBACK:
            case NetworkManagementService.NetdResponseCode.ClatdStatusResult:
            case 224:
                return false;
            default:
                return true;
        }
    }

    private boolean isWakeKeyWhenScreenOff(int keyCode) {
        switch (keyCode) {
            case WindowManagerService.H.WAITING_FOR_DRAWN_TIMEOUT:
            case 25:
            case 164:
                if (this.mDockMode == 0) {
                    break;
                }
                break;
            case WindowManagerService.H.DO_DISPLAY_ADDED:
            case HdmiCecKeycode.CEC_KEYCODE_RESERVED:
            case HdmiCecKeycode.CEC_KEYCODE_INITIAL_CONFIGURATION:
            case HdmiCecKeycode.CEC_KEYCODE_SELECT_BROADCAST_TYPE:
            case HdmiCecKeycode.CEC_KEYCODE_SELECT_SOUND_PRESENTATION:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
            case NetworkManagementService.NetdResponseCode.DnsProxyQueryResult:
                break;
        }
        return true;
    }

    public int interceptMotionBeforeQueueingNonInteractive(long whenNanos, int policyFlags) {
        if ((policyFlags & 1) != 0 && wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromMotion, "android.policy:MOTION")) {
            return 0;
        }
        if (shouldDispatchInputWhenNonInteractive()) {
            return 1;
        }
        if (isTheaterModeEnabled() && (policyFlags & 1) != 0) {
            wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromMotionWhenNotDreaming, "android.policy:MOTION");
        }
        return 0;
    }

    private boolean shouldDispatchInputWhenNonInteractive() {
        boolean displayOff = this.mDisplay == null || this.mDisplay.getState() == 1;
        if (displayOff && !this.mHasFeatureWatch) {
            return false;
        }
        if (isKeyguardShowingAndNotOccluded() && !displayOff) {
            return true;
        }
        IDreamManager dreamManager = getDreamManager();
        if (dreamManager != null) {
            try {
                if (dreamManager.isDreaming()) {
                    return true;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException when checking if dreaming", e);
            }
        }
        return false;
    }

    private void dispatchDirectAudioEvent(KeyEvent event) {
        if (event.getAction() != 0) {
        }
        int keyCode = event.getKeyCode();
        String pkgName = this.mContext.getOpPackageName();
        switch (keyCode) {
            case WindowManagerService.H.WAITING_FOR_DRAWN_TIMEOUT:
                try {
                    getAudioService().adjustSuggestedStreamVolume(1, Integer.MIN_VALUE, 4101, pkgName, TAG);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error dispatching volume up in dispatchTvAudioEvent.", e);
                    return;
                }
                break;
            case 25:
                try {
                    getAudioService().adjustSuggestedStreamVolume(-1, Integer.MIN_VALUE, 4101, pkgName, TAG);
                } catch (RemoteException e2) {
                    Log.e(TAG, "Error dispatching volume down in dispatchTvAudioEvent.", e2);
                    return;
                }
                break;
            case 164:
                try {
                    if (event.getRepeatCount() == 0) {
                        getAudioService().adjustSuggestedStreamVolume(101, Integer.MIN_VALUE, 4101, pkgName, TAG);
                    }
                } catch (RemoteException e3) {
                    Log.e(TAG, "Error dispatching mute in dispatchTvAudioEvent.", e3);
                    return;
                }
                break;
        }
    }

    void dispatchMediaKeyWithWakeLock(KeyEvent event) {
        if (DEBUG_INPUT) {
            Slog.d(TAG, "dispatchMediaKeyWithWakeLock: " + event);
        }
        if (this.mHavePendingMediaKeyRepeatWithWakeLock) {
            if (DEBUG_INPUT) {
                Slog.d(TAG, "dispatchMediaKeyWithWakeLock: canceled repeat");
            }
            this.mHandler.removeMessages(4);
            this.mHavePendingMediaKeyRepeatWithWakeLock = false;
            this.mBroadcastWakeLock.release();
        }
        dispatchMediaKeyWithWakeLockToAudioService(event);
        if (event.getAction() == 0 && event.getRepeatCount() == 0) {
            this.mHavePendingMediaKeyRepeatWithWakeLock = true;
            Message msg = this.mHandler.obtainMessage(4, event);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg, ViewConfiguration.getKeyRepeatTimeout());
            return;
        }
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyRepeatWithWakeLock(KeyEvent event) {
        this.mHavePendingMediaKeyRepeatWithWakeLock = false;
        KeyEvent repeatEvent = KeyEvent.changeTimeRepeat(event, SystemClock.uptimeMillis(), 1, event.getFlags() | 128);
        if (DEBUG_INPUT) {
            Slog.d(TAG, "dispatchMediaKeyRepeatWithWakeLock: " + repeatEvent);
        }
        dispatchMediaKeyWithWakeLockToAudioService(repeatEvent);
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyWithWakeLockToAudioService(KeyEvent event) {
        if (!ActivityManagerNative.isSystemReady()) {
            return;
        }
        MediaSessionLegacyHelper.getHelper(this.mContext).sendMediaButtonEvent(event, true);
    }

    void launchVoiceAssistWithWakeLock(boolean keyguardActive) {
        IDeviceIdleController dic = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        if (dic != null) {
            try {
                dic.exitIdle("voice-search");
            } catch (RemoteException e) {
            }
        }
        Intent voiceIntent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
        voiceIntent.putExtra("android.speech.extras.EXTRA_SECURE", keyguardActive);
        startActivityAsUser(voiceIntent, UserHandle.CURRENT_OR_SELF);
        this.mBroadcastWakeLock.release();
    }

    private void requestTransientBars(WindowManagerPolicy.WindowState swipeTarget) {
        synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            if (!isUserSetupComplete()) {
                return;
            }
            boolean sb = this.mStatusBarController.checkShowTransientBarLw();
            boolean nb = this.mNavigationBarController.checkShowTransientBarLw();
            if (sb || nb) {
                if (!nb && swipeTarget == this.mNavigationBar) {
                    if (DEBUG) {
                        Slog.d(TAG, "Not showing transient bar, wrong swipe target");
                    }
                    return;
                }
                if (sb) {
                    this.mStatusBarController.showTransient();
                }
                if (nb) {
                    this.mNavigationBarController.showTransient();
                }
                this.mImmersiveModeConfirmation.confirmCurrentPrompt();
                updateSystemUiVisibilityLw();
            }
        }
    }

    public void startedGoingToSleep(int why) {
        if (DEBUG_WAKEUP) {
            Slog.i(TAG, "Started going to sleep... (why=" + why + ")");
        }
        this.mCameraGestureTriggeredDuringGoingToSleep = false;
        this.mGoingToSleep = true;
        if (this.mKeyguardDelegate == null) {
            return;
        }
        this.mKeyguardDelegate.onStartedGoingToSleep(why);
    }

    public void finishedGoingToSleep(int why) {
        EventLog.writeEvent(70000, 0);
        if (DEBUG_WAKEUP) {
            Slog.i(TAG, "Finished going to sleep... (why=" + why + ")");
        }
        MetricsLogger.histogram(this.mContext, "screen_timeout", this.mLockScreenTimeout / 1000);
        this.mGoingToSleep = false;
        synchronized (this.mLock) {
            this.mAwake = false;
            updateWakeGestureListenerLp();
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onFinishedGoingToSleep(why, this.mCameraGestureTriggeredDuringGoingToSleep);
        }
        this.mCameraGestureTriggeredDuringGoingToSleep = false;
    }

    public void startedWakingUp() {
        EventLog.writeEvent(70000, 1);
        if (DEBUG_WAKEUP) {
            Slog.i(TAG, "Started waking up...");
        }
        synchronized (this.mLock) {
            this.mAwake = true;
            updateWakeGestureListenerLp();
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
        if (this.mKeyguardDelegate == null) {
            return;
        }
        this.mKeyguardDelegate.onStartedWakingUp();
    }

    public void finishedWakingUp() {
        if (DEBUG_WAKEUP) {
            Slog.i(TAG, "Finished waking up...");
        }
    }

    private void wakeUpFromPowerKey(long eventTime) {
        wakeUp(eventTime, this.mAllowTheaterModeWakeFromPowerKey, "android.policy:POWER");
    }

    private boolean wakeUp(long wakeTime, boolean wakeInTheaterMode, String reason) {
        boolean theaterModeEnabled = isTheaterModeEnabled();
        if (!wakeInTheaterMode && theaterModeEnabled) {
            return false;
        }
        if (theaterModeEnabled) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 0);
        }
        this.mPowerManager.wakeUp(wakeTime, reason);
        return true;
    }

    private void finishKeyguardDrawn() {
        synchronized (this.mLock) {
            if (!this.mScreenOnEarly || this.mKeyguardDrawComplete) {
                return;
            }
            this.mKeyguardDrawComplete = true;
            if (this.mKeyguardDelegate != null) {
                this.mHandler.removeMessages(6);
            }
            this.mWindowManagerDrawComplete = false;
            this.mWindowManagerInternal.waitForAllWindowsDrawn(this.mWindowManagerDrawCallback, 1000L);
        }
    }

    public void screenTurnedOff() {
        if (DEBUG_WAKEUP) {
            Slog.i(TAG, "Screen turned off...");
        }
        updateScreenOffSleepToken(true);
        synchronized (this.mLock) {
            this.mScreenOnEarly = false;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = null;
            updateOrientationListenerLp();
            if (this.mKeyguardDelegate != null) {
                this.mKeyguardDelegate.onScreenTurnedOff();
            }
        }
    }

    public void screenTurningOn(WindowManagerPolicy.ScreenOnListener screenOnListener) {
        if (DEBUG_WAKEUP) {
            Slog.i(TAG, "Screen turning on...");
        }
        updateScreenOffSleepToken(false);
        synchronized (this.mLock) {
            this.mScreenOnEarly = true;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = screenOnListener;
            if (this.mKeyguardDelegate != null) {
                this.mHandler.removeMessages(6);
                this.mHandler.sendEmptyMessageDelayed(6, 1000L);
                this.mKeyguardDelegate.onScreenTurningOn(this.mKeyguardDrawnCallback);
            } else {
                if (DEBUG_WAKEUP) {
                    Slog.d(TAG, "null mKeyguardDelegate: setting mKeyguardDrawComplete.");
                }
                finishKeyguardDrawn();
            }
        }
    }

    public void screenTurnedOn() {
        synchronized (this.mLock) {
            if (this.mKeyguardDelegate != null) {
                this.mKeyguardDelegate.onScreenTurnedOn();
            }
        }
    }

    private void finishWindowsDrawn() {
        synchronized (this.mLock) {
            if (!this.mScreenOnEarly || this.mWindowManagerDrawComplete) {
                return;
            }
            this.mWindowManagerDrawComplete = true;
            finishScreenTurningOn();
        }
    }

    private void finishScreenTurningOn() {
        boolean enableScreen;
        synchronized (this.mLock) {
            updateOrientationListenerLp();
        }
        synchronized (this.mLock) {
            if (DEBUG_WAKEUP) {
                Slog.d(TAG, "finishScreenTurningOn: mAwake=" + this.mAwake + ", mScreenOnEarly=" + this.mScreenOnEarly + ", mScreenOnFully=" + this.mScreenOnFully + ", mKeyguardDrawComplete=" + this.mKeyguardDrawComplete + ", mWindowManagerDrawComplete=" + this.mWindowManagerDrawComplete);
            }
            if (this.mScreenOnFully || !this.mScreenOnEarly || !this.mWindowManagerDrawComplete || (this.mAwake && !this.mKeyguardDrawComplete)) {
                return;
            }
            if (DEBUG_WAKEUP) {
                Slog.i(TAG, "Finished screen turning on...");
            }
            WindowManagerPolicy.ScreenOnListener listener = this.mScreenOnListener;
            this.mScreenOnListener = null;
            this.mScreenOnFully = true;
            if (this.mKeyguardDrawnOnce || !this.mAwake) {
                enableScreen = false;
            } else {
                this.mKeyguardDrawnOnce = true;
                enableScreen = true;
                if (this.mBootMessageNeedsHiding) {
                    this.mBootMessageNeedsHiding = false;
                    hideBootMessages();
                }
            }
            if (listener != null) {
                listener.onScreenOn();
            }
            if (enableScreen) {
                try {
                    this.mWindowManager.enableScreenIfNeeded();
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void handleHideBootMessage() {
        synchronized (this.mLock) {
            if (!this.mKeyguardDrawnOnce) {
                this.mBootMessageNeedsHiding = true;
            } else {
                if (this.mBootMsgDialog == null) {
                    return;
                }
                if (DEBUG_WAKEUP) {
                    Slog.d(TAG, "handleHideBootMessage: dismissing");
                }
                this.mBootMsgDialog.dismiss();
                this.mBootMsgDialog = null;
            }
        }
    }

    public boolean isScreenOn() {
        return this.mScreenOnFully;
    }

    public void enableKeyguard(boolean enabled) {
        if (this.mKeyguardDelegate == null) {
            return;
        }
        this.mKeyguardDelegate.setKeyguardEnabled(enabled);
    }

    public void exitKeyguardSecurely(WindowManagerPolicy.OnKeyguardExitResult callback) {
        if (this.mKeyguardDelegate == null) {
            return;
        }
        this.mKeyguardDelegate.verifyUnlock(callback);
    }

    private boolean isKeyguardShowingAndNotOccluded() {
        return (this.mKeyguardDelegate == null || !this.mKeyguardDelegate.isShowing() || this.mKeyguardOccluded) ? false : true;
    }

    public boolean isKeyguardLocked() {
        return keyguardOn();
    }

    public boolean isKeyguardSecure(int userId) {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isSecure(userId);
    }

    public boolean isKeyguardShowingOrOccluded() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isShowing();
    }

    public boolean inKeyguardRestrictedKeyInputMode() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isInputRestricted();
    }

    public void dismissKeyguardLw() {
        if (this.mKeyguardDelegate == null || !this.mKeyguardDelegate.isShowing()) {
            return;
        }
        if (DEBUG_KEYGUARD) {
            Slog.d(TAG, "PWM.dismissKeyguardLw");
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PhoneWindowManager.this.mKeyguardDelegate.dismiss();
            }
        });
    }

    public void notifyActivityDrawnForKeyguardLw() {
        if (this.mKeyguardDelegate == null) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PhoneWindowManager.this.mKeyguardDelegate.onActivityDrawn();
            }
        });
    }

    public boolean isKeyguardDrawnLw() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mKeyguardDrawnOnce;
        }
        return z;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        if (this.mKeyguardDelegate == null) {
            return;
        }
        if (DEBUG_KEYGUARD) {
            Slog.d(TAG, "PWM.startKeyguardExitAnimation");
        }
        this.mKeyguardDelegate.startKeyguardExitAnimation(startTime, fadeoutDuration);
    }

    public void getStableInsetsLw(int displayRotation, int displayWidth, int displayHeight, Rect outInsets) {
        outInsets.setEmpty();
        getNonDecorInsetsLw(displayRotation, displayWidth, displayHeight, outInsets);
        if (this.mStatusBar == null) {
            return;
        }
        outInsets.top = this.mStatusBarHeight;
    }

    public void getNonDecorInsetsLw(int displayRotation, int displayWidth, int displayHeight, Rect outInsets) {
        outInsets.setEmpty();
        if (this.mNavigationBar == null) {
            return;
        }
        if (isNavigationBarOnBottom(displayWidth, displayHeight)) {
            outInsets.bottom = getNavigationBarHeight(displayRotation, this.mUiMode);
        } else {
            outInsets.right = getNavigationBarWidth(displayRotation, this.mUiMode);
        }
    }

    public boolean isNavBarForcedShownLw(WindowManagerPolicy.WindowState windowState) {
        return this.mForceShowSystemBars;
    }

    public boolean isDockSideAllowed(int dockSide) {
        return !this.mNavigationBarCanMove ? dockSide == 2 || dockSide == 1 || dockSide == 3 : dockSide == 2 || dockSide == 1;
    }

    void sendCloseSystemWindows() {
        PhoneWindow.sendCloseSystemWindows(this.mContext, (String) null);
    }

    void sendCloseSystemWindows(String reason) {
        PhoneWindow.sendCloseSystemWindows(this.mContext, reason);
    }

    public int rotationForOrientationLw(int orientation, int lastRotation) {
        int preferredRotation;
        if (this.mForceDefaultOrientation) {
            return 0;
        }
        synchronized (this.mLock) {
            int sensorRotation = this.mOrientationListener.getProposedRotation();
            if (sensorRotation < 0) {
                sensorRotation = lastRotation;
            }
            if (this.mLidState == 1 && this.mLidOpenRotation >= 0) {
                preferredRotation = this.mLidOpenRotation;
            } else if (this.mDockMode == 2 && (this.mCarDockEnablesAccelerometer || this.mCarDockRotation >= 0)) {
                preferredRotation = this.mCarDockEnablesAccelerometer ? sensorRotation : this.mCarDockRotation;
            } else if ((this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == 4) && (this.mDeskDockEnablesAccelerometer || this.mDeskDockRotation >= 0)) {
                preferredRotation = this.mDeskDockEnablesAccelerometer ? sensorRotation : this.mDeskDockRotation;
            } else if (this.mHdmiPlugged && this.mDemoHdmiRotationLock) {
                preferredRotation = this.mDemoHdmiRotation;
            } else if (this.mHdmiPlugged && this.mDockMode == 0 && this.mUndockedHdmiRotation >= 0) {
                preferredRotation = this.mUndockedHdmiRotation;
            } else if (this.mDemoRotationLock) {
                preferredRotation = this.mDemoRotation;
            } else if (orientation == 14) {
                preferredRotation = lastRotation;
            } else if (!this.mSupportAutoRotation) {
                preferredRotation = -1;
            } else if ((this.mUserRotationMode == 0 && (orientation == 2 || orientation == -1 || orientation == 11 || orientation == 12 || orientation == 13)) || orientation == 4 || orientation == 10 || orientation == 6 || orientation == 7) {
                if (this.mAllowAllRotations < 0) {
                    this.mAllowAllRotations = this.mContext.getResources().getBoolean(R.^attr-private.dayHighlightColor) ? 1 : 0;
                }
                preferredRotation = (sensorRotation != 2 || this.mAllowAllRotations == 1 || orientation == 10 || orientation == 13) ? sensorRotation : lastRotation;
            } else {
                preferredRotation = (this.mUserRotationMode != 1 || orientation == 5) ? -1 : this.mUserRotation;
            }
            if (DEBUG_ORIENTATION) {
                Slog.v(TAG, "rotationForOrientationLw(appReqQrientation = " + orientation + ", lastOrientation = " + lastRotation + ", sensorRotation = " + sensorRotation + ", UserRotation = " + this.mUserRotation + ", LidState = " + this.mLidState + ", DockMode = " + this.mDockMode + ", DeskDockEnable = " + this.mDeskDockEnablesAccelerometer + ", CarDockEnable = " + this.mCarDockEnablesAccelerometer + ", HdmiPlugged = " + this.mHdmiPlugged + ", Accelerometer = " + this.mAccelerometerDefault + ", AllowAllRotations = " + this.mAllowAllRotations + ")");
            }
            switch (orientation) {
                case 0:
                    if (isLandscapeOrSeascape(preferredRotation)) {
                        return preferredRotation;
                    }
                    return this.mLandscapeRotation;
                case 1:
                    if (isAnyPortrait(preferredRotation)) {
                        return preferredRotation;
                    }
                    return this.mPortraitRotation;
                case 2:
                case 3:
                case 4:
                case 5:
                case 10:
                default:
                    if (preferredRotation >= 0) {
                        return preferredRotation;
                    }
                    return 0;
                case 6:
                case 11:
                    if (isLandscapeOrSeascape(preferredRotation)) {
                        return preferredRotation;
                    }
                    if (isLandscapeOrSeascape(lastRotation)) {
                        return lastRotation;
                    }
                    return this.mLandscapeRotation;
                case 7:
                case 12:
                    if (isAnyPortrait(preferredRotation)) {
                        return preferredRotation;
                    }
                    if (isAnyPortrait(lastRotation)) {
                        return lastRotation;
                    }
                    return this.mPortraitRotation;
                case 8:
                    if (isLandscapeOrSeascape(preferredRotation)) {
                        return preferredRotation;
                    }
                    return this.mSeascapeRotation;
                case 9:
                    if (isAnyPortrait(preferredRotation)) {
                        return preferredRotation;
                    }
                    return this.mUpsideDownRotation;
            }
        }
    }

    public boolean rotationHasCompatibleMetricsLw(int orientation, int rotation) {
        switch (orientation) {
            case 0:
            case 6:
            case 8:
                return isLandscapeOrSeascape(rotation);
            case 1:
            case 7:
            case 9:
                return isAnyPortrait(rotation);
            case 2:
            case 3:
            case 4:
            case 5:
            default:
                return true;
        }
    }

    public void setRotationLw(int rotation) {
        this.mOrientationListener.setCurrentRotation(rotation);
    }

    private boolean isLandscapeOrSeascape(int rotation) {
        return rotation == this.mLandscapeRotation || rotation == this.mSeascapeRotation;
    }

    private boolean isAnyPortrait(int rotation) {
        return rotation == this.mPortraitRotation || rotation == this.mUpsideDownRotation;
    }

    public int getUserRotationMode() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 0, -2) != 0 ? 0 : 1;
    }

    public void setUserRotationMode(int mode, int rot) {
        ContentResolver res = this.mContext.getContentResolver();
        if (mode == 1) {
            Settings.System.putIntForUser(res, "user_rotation", rot, -2);
            Settings.System.putIntForUser(res, "accelerometer_rotation", 0, -2);
        } else {
            Settings.System.putIntForUser(res, "accelerometer_rotation", 1, -2);
        }
    }

    public void setSafeMode(boolean safeMode) {
        int i;
        this.mSafeMode = safeMode;
        if (safeMode) {
            i = 10001;
        } else {
            i = 10000;
        }
        performHapticFeedbackLw(null, i, true);
    }

    static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i = 0; i < ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    public void systemReady() {
        boolean bindKeyguardNow;
        this.mKeyguardDelegate = new KeyguardServiceDelegate(this.mContext);
        this.mKeyguardDelegate.onSystemReady();
        readCameraLensCoverState();
        updateUiMode();
        synchronized (this.mLock) {
            updateOrientationListenerLp();
            this.mSystemReady = true;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PhoneWindowManager.this.updateSettings();
                }
            });
            bindKeyguardNow = this.mDeferBindKeyguard;
            if (bindKeyguardNow) {
                this.mDeferBindKeyguard = false;
            }
        }
        if (bindKeyguardNow) {
            this.mKeyguardDelegate.bindService(this.mContext);
            this.mKeyguardDelegate.onBootCompleted();
        }
        this.mSystemGestures.systemReady();
        this.mImmersiveModeConfirmation.systemReady();
    }

    public void systemBooted() {
        boolean bindKeyguardNow = false;
        synchronized (this.mLock) {
            if (this.mKeyguardDelegate != null) {
                bindKeyguardNow = true;
            } else {
                this.mDeferBindKeyguard = true;
            }
        }
        if (bindKeyguardNow) {
            this.mKeyguardDelegate.bindService(this.mContext);
            this.mKeyguardDelegate.onBootCompleted();
        }
        synchronized (this.mLock) {
            this.mSystemBooted = true;
        }
        startedWakingUp();
        screenTurningOn(null);
        screenTurnedOn();
    }

    public void showBootMessage(final CharSequence msg, boolean always) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int theme;
                if (PhoneWindowManager.this.mBootMsgDialog == null) {
                    if (PhoneWindowManager.this.mHasFeatureWatch) {
                        theme = R.style.Widget.Holo.Light.QuickContactBadge.WindowLarge;
                    } else if (PhoneWindowManager.this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.television")) {
                        theme = R.style.Widget.DeviceDefault.TimePicker;
                    } else {
                        theme = 0;
                    }
                    PhoneWindowManager.this.mBootMsgDialog = new ProgressDialog(PhoneWindowManager.this.mContext, theme) {
                        @Override
                        public boolean dispatchKeyEvent(KeyEvent event) {
                            return true;
                        }

                        @Override
                        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                            return true;
                        }

                        @Override
                        public boolean dispatchTouchEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override
                        public boolean dispatchTrackballEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override
                        public boolean dispatchGenericMotionEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override
                        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                            return true;
                        }
                    };
                    if (PhoneWindowManager.this.mContext.getPackageManager().isUpgrade()) {
                        PhoneWindowManager.this.mBootMsgDialog.setTitle(R.string.enable_explore_by_touch_warning_title);
                    } else {
                        PhoneWindowManager.this.mBootMsgDialog.setTitle(R.string.error_handwriting_unsupported);
                    }
                    PhoneWindowManager.this.mBootMsgDialog.setProgressStyle(0);
                    PhoneWindowManager.this.mBootMsgDialog.setIndeterminate(true);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setType(2021);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().addFlags(258);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setDimAmount(1.0f);
                    WindowManager.LayoutParams lp = PhoneWindowManager.this.mBootMsgDialog.getWindow().getAttributes();
                    lp.screenOrientation = 5;
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setAttributes(lp);
                    PhoneWindowManager.this.mBootMsgDialog.setCancelable(false);
                    PhoneWindowManager.this.mBootMsgDialog.show();
                }
                PhoneWindowManager.this.mBootMsgDialog.setMessage(msg);
            }
        });
    }

    public void hideBootMessages() {
        this.mHandler.sendEmptyMessage(11);
    }

    public void userActivity() {
        synchronized (this.mStkLock) {
            if (this.mIsStkUserActivityEnabled) {
                this.mHandler.post(this.mNotifyStk);
            }
        }
        synchronized (this.mScreenLockTimeout) {
            if (this.mLockScreenTimerActive) {
                this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                this.mHandler.postDelayed(this.mScreenLockTimeout, this.mLockScreenTimeout);
            }
        }
    }

    class ScreenLockTimeout implements Runnable {
        Bundle options;

        ScreenLockTimeout() {
        }

        @Override
        public void run() {
            synchronized (this) {
                if (PhoneWindowManager.localLOGV) {
                    Log.v(PhoneWindowManager.TAG, "mScreenLockTimeout activating keyguard");
                }
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.doKeyguardTimeout(this.options);
                }
                PhoneWindowManager.this.mLockScreenTimerActive = false;
                this.options = null;
            }
        }

        public void setLockOptions(Bundle options) {
            this.options = options;
        }
    }

    public void lockNow(Bundle options) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
        this.mHandler.removeCallbacks(this.mScreenLockTimeout);
        if (options != null) {
            this.mScreenLockTimeout.setLockOptions(options);
        }
        this.mHandler.post(this.mScreenLockTimeout);
    }

    private void updateLockScreenTimeout() {
        synchronized (this.mScreenLockTimeout) {
            boolean zIsSecure = (this.mAllowLockscreenWhenOn && this.mAwake && this.mKeyguardDelegate != null) ? this.mKeyguardDelegate.isSecure(this.mCurrentUserId) : false;
            if (this.mLockScreenTimerActive != zIsSecure) {
                if (zIsSecure) {
                    if (localLOGV) {
                        Log.v(TAG, "setting lockscreen timer");
                    }
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                    this.mHandler.postDelayed(this.mScreenLockTimeout, this.mLockScreenTimeout);
                } else {
                    if (localLOGV) {
                        Log.v(TAG, "clearing lockscreen timer");
                    }
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                }
                this.mLockScreenTimerActive = zIsSecure;
            }
        }
    }

    private void updateDreamingSleepToken(boolean acquire) {
        if (acquire) {
            if (this.mDreamingSleepToken != null) {
                return;
            }
            this.mDreamingSleepToken = this.mActivityManagerInternal.acquireSleepToken("Dream");
        } else {
            if (this.mDreamingSleepToken == null) {
                return;
            }
            this.mDreamingSleepToken.release();
            this.mDreamingSleepToken = null;
        }
    }

    private void updateScreenOffSleepToken(boolean acquire) {
        if (acquire) {
            if (this.mScreenOffSleepToken != null) {
                return;
            }
            this.mScreenOffSleepToken = this.mActivityManagerInternal.acquireSleepToken("ScreenOff");
        } else {
            if (this.mScreenOffSleepToken == null) {
                return;
            }
            this.mScreenOffSleepToken.release();
            this.mScreenOffSleepToken = null;
        }
    }

    public void enableScreenAfterBoot() {
        readLidState();
        applyLidSwitchState();
        updateRotation(true);
    }

    private void applyLidSwitchState() {
        if (this.mLidState == 0 && this.mLidControlsSleep) {
            this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 3, 1);
        } else if (this.mLidState == 0 && this.mLidControlsScreenLock) {
            this.mWindowManagerFuncs.lockDeviceNow();
        }
        synchronized (this.mLock) {
            updateWakeGestureListenerLp();
        }
    }

    void updateUiMode() {
        if (this.mUiModeManager == null) {
            this.mUiModeManager = IUiModeManager.Stub.asInterface(ServiceManager.getService("uimode"));
        }
        try {
            this.mUiMode = this.mUiModeManager.getCurrentModeType();
        } catch (RemoteException e) {
        }
    }

    void updateRotation(boolean alwaysSendConfiguration) {
        try {
            this.mWindowManager.updateRotation(alwaysSendConfiguration, false);
        } catch (RemoteException e) {
        }
    }

    void updateRotation(boolean alwaysSendConfiguration, boolean forceRelayout) {
        try {
            this.mWindowManager.updateRotation(alwaysSendConfiguration, forceRelayout);
        } catch (RemoteException e) {
        }
    }

    Intent createHomeDockIntent() {
        Intent intent;
        if (this.mUiMode == 3) {
            if (!this.mEnableCarDockHomeCapture) {
                intent = null;
            } else {
                Intent intent2 = this.mCarDockIntent;
                intent = intent2;
            }
        } else if (this.mUiMode == 2 || this.mUiMode != 6 || (this.mDockMode != 1 && this.mDockMode != 4 && this.mDockMode != 3)) {
            intent = null;
        } else {
            Intent intent3 = this.mDeskDockIntent;
            intent = intent3;
        }
        if (intent == null) {
            return null;
        }
        ActivityInfo ai = null;
        ResolveInfo info = this.mContext.getPackageManager().resolveActivityAsUser(intent, 65664, this.mCurrentUserId);
        if (info != null) {
            ai = info.activityInfo;
        }
        if (ai == null || ai.metaData == null || !ai.metaData.getBoolean("android.dock_home")) {
            return null;
        }
        Intent intent4 = new Intent(intent);
        intent4.setClassName(ai.packageName, ai.name);
        return intent4;
    }

    void startDockOrHome(boolean fromHomeKey, boolean awakenFromDreams) {
        Intent intent;
        if (awakenFromDreams) {
            awakenDreams();
        }
        Intent dock = createHomeDockIntent();
        if (dock != null) {
            if (fromHomeKey) {
                try {
                    dock.putExtra("android.intent.extra.FROM_HOME_KEY", fromHomeKey);
                } catch (ActivityNotFoundException e) {
                }
            }
            startActivityAsUser(dock, UserHandle.CURRENT);
            return;
        }
        if (fromHomeKey) {
            intent = new Intent(this.mHomeIntent);
            intent.putExtra("android.intent.extra.FROM_HOME_KEY", fromHomeKey);
        } else {
            intent = this.mHomeIntent;
        }
        startActivityAsUser(intent, UserHandle.CURRENT);
    }

    boolean goHome() {
        if (!isUserSetupComplete()) {
            Slog.i(TAG, "Not going home because user setup is in progress.");
            return false;
        }
        try {
            if (SystemProperties.getInt("persist.sys.uts-test-mode", 0) == 1) {
                Log.d(TAG, "UTS-TEST-MODE");
            } else {
                ActivityManagerNative.getDefault().stopAppSwitches();
                sendCloseSystemWindows();
                Intent dock = createHomeDockIntent();
                if (dock != null) {
                    int result = ActivityManagerNative.getDefault().startActivityAsUser((IApplicationThread) null, (String) null, dock, dock.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 1, (ProfilerInfo) null, (Bundle) null, -2);
                    if (result == 1) {
                        return false;
                    }
                }
            }
            int result2 = ActivityManagerNative.getDefault().startActivityAsUser((IApplicationThread) null, (String) null, this.mHomeIntent, this.mHomeIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 1, (ProfilerInfo) null, (Bundle) null, -2);
            if (result2 == 1) {
                return false;
            }
            return true;
        } catch (RemoteException e) {
            return true;
        }
    }

    public void setCurrentOrientationLw(int newOrientation) {
        synchronized (this.mLock) {
            if (newOrientation != this.mCurrentAppOrientation) {
                this.mCurrentAppOrientation = newOrientation;
                updateOrientationListenerLp();
            }
        }
    }

    private void performAuditoryFeedbackForAccessibilityIfNeed() {
        if (!isGlobalAccessibilityGestureEnabled()) {
            return;
        }
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (audioManager.isSilentMode()) {
            return;
        }
        Ringtone ringTone = RingtoneManager.getRingtone(this.mContext, Settings.System.DEFAULT_NOTIFICATION_URI);
        ringTone.setStreamType(3);
        ringTone.play();
    }

    private boolean isTheaterModeEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
    }

    private boolean isGlobalAccessibilityGestureEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "enable_accessibility_global_gesture_enabled", 0) == 1;
    }

    public boolean performHapticFeedbackLw(WindowManagerPolicy.WindowState win, int effectId, boolean always) {
        long[] pattern;
        int owningUid;
        String owningPackage;
        if (!this.mVibrator.hasVibrator()) {
            return false;
        }
        boolean hapticsDisabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) == 0;
        if (hapticsDisabled && !always) {
            return false;
        }
        switch (effectId) {
            case 0:
                pattern = this.mLongPressVibePattern;
                break;
            case 1:
                pattern = this.mVirtualKeyVibePattern;
                break;
            case 3:
                pattern = this.mKeyboardTapVibePattern;
                break;
            case 4:
                pattern = this.mClockTickVibePattern;
                break;
            case 5:
                pattern = this.mCalendarDateVibePattern;
                break;
            case 6:
                pattern = this.mContextClickVibePattern;
                break;
            case 10000:
                pattern = this.mSafeModeDisabledVibePattern;
                break;
            case 10001:
                pattern = this.mSafeModeEnabledVibePattern;
                break;
            default:
                return false;
        }
        if (win != null) {
            owningUid = win.getOwningUid();
            owningPackage = win.getOwningPackage();
        } else {
            owningUid = Process.myUid();
            owningPackage = this.mContext.getOpPackageName();
        }
        if (pattern.length == 1) {
            this.mVibrator.vibrate(owningUid, owningPackage, pattern[0], VIBRATION_ATTRIBUTES);
        } else {
            this.mVibrator.vibrate(owningUid, owningPackage, pattern, -1, VIBRATION_ATTRIBUTES);
        }
        return true;
    }

    public void keepScreenOnStartedLw() {
    }

    public void keepScreenOnStoppedLw() {
        if (!isKeyguardShowingAndNotOccluded()) {
            return;
        }
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
    }

    private int updateSystemUiVisibilityLw() {
        final WindowManagerPolicy.WindowState win = this.mFocusedWindow != null ? this.mFocusedWindow : this.mTopFullscreenOpaqueWindowState;
        if (win == null) {
            return 0;
        }
        if ((win.getAttrs().privateFlags & 1024) != 0 && this.mHideLockScreen) {
            return 0;
        }
        int tmpVisibility = PolicyControl.getSystemUiVisibility(win, null) & (~this.mResettingSystemUiFlags) & (~this.mForceClearedSystemUiFlags);
        if (this.mForcingShowNavBar && win.getSurfaceLayer() < this.mForcingShowNavBarLayer) {
            tmpVisibility &= ~PolicyControl.adjustClearableFlags(win, 7);
        }
        final int fullscreenVisibility = updateLightStatusBarLw(0, this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState);
        final int dockedVisibility = updateLightStatusBarLw(0, this.mTopDockedOpaqueWindowState, this.mTopDockedOpaqueOrDimmingWindowState);
        this.mWindowManagerFuncs.getStackBounds(0, this.mNonDockedStackBounds);
        this.mWindowManagerFuncs.getStackBounds(3, this.mDockedStackBounds);
        final int visibility = updateSystemBarsLw(win, this.mLastSystemUiFlags, tmpVisibility);
        int diff = visibility ^ this.mLastSystemUiFlags;
        int fullscreenDiff = fullscreenVisibility ^ this.mLastFullscreenStackSysUiFlags;
        int dockedDiff = dockedVisibility ^ this.mLastDockedStackSysUiFlags;
        final boolean needsMenu = win.getNeedsMenuLw(this.mTopFullscreenOpaqueWindowState);
        if (diff == 0 && fullscreenDiff == 0 && dockedDiff == 0 && this.mLastFocusNeedsMenu == needsMenu && this.mFocusedApp == win.getAppToken() && this.mLastNonDockedStackBounds.equals(this.mNonDockedStackBounds) && this.mLastDockedStackBounds.equals(this.mDockedStackBounds)) {
            return 0;
        }
        this.mLastSystemUiFlags = visibility;
        this.mLastFullscreenStackSysUiFlags = fullscreenVisibility;
        this.mLastDockedStackSysUiFlags = dockedVisibility;
        this.mLastFocusNeedsMenu = needsMenu;
        this.mFocusedApp = win.getAppToken();
        final Rect fullscreenStackBounds = new Rect(this.mNonDockedStackBounds);
        final Rect dockedStackBounds = new Rect(this.mDockedStackBounds);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                StatusBarManagerInternal statusbar = PhoneWindowManager.this.getStatusBarManagerInternal();
                if (statusbar == null) {
                    return;
                }
                statusbar.setSystemUiVisibility(visibility, fullscreenVisibility, dockedVisibility, -1, fullscreenStackBounds, dockedStackBounds, win.toString());
                statusbar.topAppWindowChanged(needsMenu);
            }
        });
        return diff;
    }

    private int updateLightStatusBarLw(int vis, WindowManagerPolicy.WindowState opaque, WindowManagerPolicy.WindowState opaqueOrDimming) {
        WindowManagerPolicy.WindowState statusColorWin = (!isStatusBarKeyguard() || this.mHideLockScreen) ? opaqueOrDimming : this.mStatusBar;
        if (statusColorWin != null) {
            if (statusColorWin == opaque) {
                return (vis & (-8193)) | (PolicyControl.getSystemUiVisibility(statusColorWin, null) & PackageManagerService.DumpState.DUMP_PREFERRED_XML);
            }
            if (statusColorWin != null && statusColorWin.isDimming()) {
                return vis & (-8193);
            }
            return vis;
        }
        return vis;
    }

    private boolean drawsSystemBarBackground(WindowManagerPolicy.WindowState win) {
        return win == null || (win.getAttrs().flags & Integer.MIN_VALUE) != 0;
    }

    private boolean forcesDrawStatusBarBackground(WindowManagerPolicy.WindowState win) {
        return win == null || (win.getAttrs().privateFlags & PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0;
    }

    private int updateSystemBarsLw(WindowManagerPolicy.WindowState win, int oldVis, int vis) {
        WindowManagerPolicy.WindowState fullscreenTransWin;
        boolean zForcesDrawStatusBarBackground;
        boolean zForcesDrawStatusBarBackground2;
        boolean transientStatusBarAllowed;
        boolean z;
        boolean z2;
        boolean dockedStackVisible = this.mWindowManagerInternal.isStackVisible(3);
        boolean freeformStackVisible = this.mWindowManagerInternal.isStackVisible(2);
        boolean resizing = this.mWindowManagerInternal.isDockedDividerResizing();
        this.mForceShowSystemBars = (dockedStackVisible || freeformStackVisible) ? true : resizing;
        boolean forceOpaqueStatusBar = this.mForceShowSystemBars && !this.mForceStatusBarFromKeyguard;
        if (isStatusBarKeyguard() && !this.mHideLockScreen) {
            fullscreenTransWin = this.mStatusBar;
        } else {
            fullscreenTransWin = this.mTopFullscreenOpaqueWindowState;
        }
        int vis2 = this.mNavigationBarController.applyTranslucentFlagLw(fullscreenTransWin, this.mStatusBarController.applyTranslucentFlagLw(fullscreenTransWin, vis, oldVis), oldVis);
        int dockedVis = this.mStatusBarController.applyTranslucentFlagLw(this.mTopDockedOpaqueWindowState, 0, 0);
        if (drawsSystemBarBackground(this.mTopFullscreenOpaqueWindowState) && (1073741824 & vis2) == 0) {
            zForcesDrawStatusBarBackground = true;
        } else {
            zForcesDrawStatusBarBackground = forcesDrawStatusBarBackground(this.mTopFullscreenOpaqueWindowState);
        }
        if (drawsSystemBarBackground(this.mTopDockedOpaqueWindowState) && (1073741824 & dockedVis) == 0) {
            zForcesDrawStatusBarBackground2 = true;
        } else {
            zForcesDrawStatusBarBackground2 = forcesDrawStatusBarBackground(this.mTopDockedOpaqueWindowState);
        }
        int type = win.getAttrs().type;
        boolean statusBarHasFocus = type == 2000;
        if (statusBarHasFocus && !isStatusBarKeyguard()) {
            int flags = 14342;
            if (this.mHideLockScreen) {
                flags = -1073727482;
            }
            vis2 = ((~flags) & vis2) | (oldVis & flags);
        }
        if (zForcesDrawStatusBarBackground && zForcesDrawStatusBarBackground2) {
            vis2 = (vis2 | 8) & (-1073741825);
        } else if ((!areTranslucentBarsAllowed() && fullscreenTransWin != this.mStatusBar) || forceOpaqueStatusBar) {
            vis2 &= -1073741833;
        }
        int vis3 = configureNavBarOpacity(vis2, dockedStackVisible, freeformStackVisible, resizing);
        boolean immersiveSticky = (vis3 & 4096) != 0;
        boolean hideStatusBarWM = (this.mTopFullscreenOpaqueWindowState == null || (PolicyControl.getWindowFlags(this.mTopFullscreenOpaqueWindowState, null) & 1024) == 0) ? false : true;
        boolean hideStatusBarSysui = (vis3 & 4) != 0;
        boolean hideNavBarSysui = (this.mHideNavigationBar == 0 && (vis3 & 2) == 0) ? false : true;
        if (this.mStatusBar == null) {
            transientStatusBarAllowed = false;
        } else if (statusBarHasFocus) {
            transientStatusBarAllowed = true;
        } else if (this.mForceShowSystemBars) {
            transientStatusBarAllowed = false;
        } else if (hideStatusBarWM) {
            transientStatusBarAllowed = true;
        } else {
            transientStatusBarAllowed = hideStatusBarSysui ? immersiveSticky : false;
        }
        if (this.mNavigationBar == null || this.mForceShowSystemBars || !hideNavBarSysui) {
            z = false;
        } else {
            z = immersiveSticky;
        }
        long now = SystemClock.uptimeMillis();
        boolean pendingPanic = this.mPendingPanicGestureUptime != 0 && now - this.mPendingPanicGestureUptime <= PANIC_GESTURE_EXPIRATION;
        if (pendingPanic && hideNavBarSysui && !isStatusBarKeyguard() && this.mKeyguardDrawComplete) {
            this.mPendingPanicGestureUptime = 0L;
            this.mStatusBarController.showTransient();
            this.mNavigationBarController.showTransient();
        }
        if (!this.mStatusBarController.isTransientShowRequested() || transientStatusBarAllowed) {
            z2 = false;
        } else {
            z2 = hideStatusBarSysui;
        }
        boolean denyTransientNav = this.mNavigationBarController.isTransientShowRequested() && !z;
        if (z2 || denyTransientNav || this.mForceShowSystemBars) {
            clearClearableFlagsLw();
            vis3 &= -8;
        }
        boolean immersive = (vis3 & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0;
        boolean z3 = !immersive ? (vis3 & 4096) != 0 : true;
        if (hideNavBarSysui && !z3 && windowTypeToLayerLw(win.getBaseType()) > windowTypeToLayerLw(2022)) {
            vis3 &= -3;
        }
        int vis4 = this.mStatusBarController.updateVisibilityLw(transientStatusBarAllowed, oldVis, vis3);
        boolean oldImmersiveMode = isImmersiveMode(oldVis);
        boolean newImmersiveMode = isImmersiveMode(vis4);
        if (win != null && oldImmersiveMode != newImmersiveMode && (win.getSystemUiVisibility() & 16777216) == 0) {
            String pkg = win.getOwningPackage();
            this.mImmersiveModeConfirmation.immersiveModeChangedLw(pkg, newImmersiveMode, isUserSetupComplete());
        }
        return this.mNavigationBarController.updateVisibilityLw(z, oldVis, vis4);
    }

    private int configureNavBarOpacity(int visibility, boolean dockedStackVisible, boolean freeformStackVisible, boolean isDockedDividerResizing) {
        if (this.mNavBarOpacityMode == 0) {
            if (dockedStackVisible || freeformStackVisible || isDockedDividerResizing) {
                visibility = setNavBarOpaqueFlag(visibility);
            }
        } else if (this.mNavBarOpacityMode == 1) {
            if (!isDockedDividerResizing && freeformStackVisible) {
                visibility = setNavBarTranslucentFlag(visibility);
            } else {
                visibility = setNavBarOpaqueFlag(visibility);
            }
        }
        if (!areTranslucentBarsAllowed()) {
            return visibility & Integer.MAX_VALUE;
        }
        return visibility;
    }

    private int setNavBarOpaqueFlag(int visibility) {
        return visibility & 2147450879;
    }

    private int setNavBarTranslucentFlag(int visibility) {
        return (visibility & (-32769)) | Integer.MIN_VALUE;
    }

    private void clearClearableFlagsLw() {
        int newVal = this.mResettingSystemUiFlags | 7;
        if (newVal == this.mResettingSystemUiFlags) {
            return;
        }
        this.mResettingSystemUiFlags = newVal;
        this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
    }

    private boolean isImmersiveMode(int vis) {
        if (this.mNavigationBar == null || (vis & 2) == 0 || (vis & 6144) == 0) {
            return false;
        }
        if (((vis & 2) == 0 || (vis & 6144) == 0) && this.mHideNavigationBar == 0) {
            return false;
        }
        return canHideNavigationBar();
    }

    private boolean areTranslucentBarsAllowed() {
        return this.mTranslucentDecorEnabled;
    }

    public boolean hasNavigationBar() {
        return this.mHasNavigationBar;
    }

    public void setLastInputMethodWindowLw(WindowManagerPolicy.WindowState ime, WindowManagerPolicy.WindowState target) {
        this.mLastInputMethodWindow = ime;
        this.mLastInputMethodTargetWindow = target;
    }

    public int getInputMethodWindowVisibleHeightLw() {
        return this.mDockBottom - this.mCurBottom;
    }

    public void setCurrentUserLw(int newUserId) {
        this.mCurrentUserId = newUserId;
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.setCurrentUser(newUserId);
        }
        StatusBarManagerInternal statusBar = getStatusBarManagerInternal();
        if (statusBar != null) {
            statusBar.setCurrentUser(newUserId);
        }
        setLastInputMethodWindowLw(null, null);
    }

    public boolean canMagnifyWindow(int windowType) {
        switch (windowType) {
            case 2011:
            case 2012:
            case 2019:
            case 2027:
                return false;
            default:
                return true;
        }
    }

    public boolean isTopLevelWindow(int windowType) {
        return windowType < 1000 || windowType > 1999 || windowType == 1003;
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        pw.print("mIsAlarmBoot=");
        pw.print(this.mIsAlarmBoot);
        pw.print(" mIPOUserRotation=");
        pw.print(this.mIPOUserRotation);
        pw.print(" mIsShutDown=");
        pw.print(this.mIsShutDown);
        pw.print(" mScreenOffReason=");
        pw.print(this.mScreenOffReason);
        pw.print(" mIsAlarmBoot=");
        pw.print(this.mIsAlarmBoot);
        synchronized (this.mKeyDispatchLock) {
            pw.print(" mKeyDispatcMode=");
            pw.println(this.mKeyDispatcMode);
        }
        pw.print(prefix);
        pw.print("mSafeMode=");
        pw.print(this.mSafeMode);
        pw.print(" mSystemReady=");
        pw.print(this.mSystemReady);
        pw.print(" mSystemBooted=");
        pw.println(this.mSystemBooted);
        pw.print(prefix);
        pw.print("mLidState=");
        pw.print(this.mLidState);
        pw.print(" mLidOpenRotation=");
        pw.print(this.mLidOpenRotation);
        pw.print(" mCameraLensCoverState=");
        pw.print(this.mCameraLensCoverState);
        pw.print(" mHdmiPlugged=");
        pw.println(this.mHdmiPlugged);
        if (this.mLastSystemUiFlags != 0 || this.mResettingSystemUiFlags != 0 || this.mForceClearedSystemUiFlags != 0) {
            pw.print(prefix);
            pw.print("mLastSystemUiFlags=0x");
            pw.print(Integer.toHexString(this.mLastSystemUiFlags));
            pw.print(" mResettingSystemUiFlags=0x");
            pw.print(Integer.toHexString(this.mResettingSystemUiFlags));
            pw.print(" mForceClearedSystemUiFlags=0x");
            pw.println(Integer.toHexString(this.mForceClearedSystemUiFlags));
        }
        if (this.mLastFocusNeedsMenu) {
            pw.print(prefix);
            pw.print("mLastFocusNeedsMenu=");
            pw.println(this.mLastFocusNeedsMenu);
        }
        pw.print(prefix);
        pw.print("mWakeGestureEnabledSetting=");
        pw.println(this.mWakeGestureEnabledSetting);
        pw.print(prefix);
        pw.print("mSupportAutoRotation=");
        pw.println(this.mSupportAutoRotation);
        pw.print(prefix);
        pw.print("mUiMode=");
        pw.print(this.mUiMode);
        pw.print(" mDockMode=");
        pw.print(this.mDockMode);
        pw.print(" mEnableCarDockHomeCapture=");
        pw.print(this.mEnableCarDockHomeCapture);
        pw.print(" mCarDockRotation=");
        pw.print(this.mCarDockRotation);
        pw.print(" mDeskDockRotation=");
        pw.println(this.mDeskDockRotation);
        pw.print(prefix);
        pw.print("mUserRotationMode=");
        pw.print(this.mUserRotationMode);
        pw.print(" mUserRotation=");
        pw.print(this.mUserRotation);
        pw.print(" mAllowAllRotations=");
        pw.println(this.mAllowAllRotations);
        pw.print(prefix);
        pw.print("mCurrentAppOrientation=");
        pw.println(this.mCurrentAppOrientation);
        pw.print(prefix);
        pw.print("mCarDockEnablesAccelerometer=");
        pw.print(this.mCarDockEnablesAccelerometer);
        pw.print(" mDeskDockEnablesAccelerometer=");
        pw.println(this.mDeskDockEnablesAccelerometer);
        pw.print(prefix);
        pw.print("mLidKeyboardAccessibility=");
        pw.print(this.mLidKeyboardAccessibility);
        pw.print(" mLidNavigationAccessibility=");
        pw.print(this.mLidNavigationAccessibility);
        pw.print(" mLidControlsScreenLock=");
        pw.println(this.mLidControlsScreenLock);
        pw.print(" mLidControlsSleep=");
        pw.println(this.mLidControlsSleep);
        pw.print(prefix);
        pw.print(" mLongPressOnBackBehavior=");
        pw.println(this.mLongPressOnBackBehavior);
        pw.print(prefix);
        pw.print("mShortPressOnPowerBehavior=");
        pw.print(this.mShortPressOnPowerBehavior);
        pw.print(" mLongPressOnPowerBehavior=");
        pw.println(this.mLongPressOnPowerBehavior);
        pw.print(prefix);
        pw.print("mDoublePressOnPowerBehavior=");
        pw.print(this.mDoublePressOnPowerBehavior);
        pw.print(" mTriplePressOnPowerBehavior=");
        pw.println(this.mTriplePressOnPowerBehavior);
        pw.print(prefix);
        pw.print("mHasSoftInput=");
        pw.println(this.mHasSoftInput);
        pw.print(prefix);
        pw.print("mAwake=");
        pw.println(this.mAwake);
        pw.print(prefix);
        pw.print("mScreenOnEarly=");
        pw.print(this.mScreenOnEarly);
        pw.print(" mScreenOnFully=");
        pw.println(this.mScreenOnFully);
        pw.print(prefix);
        pw.print("mKeyguardDrawComplete=");
        pw.print(this.mKeyguardDrawComplete);
        pw.print(" mWindowManagerDrawComplete=");
        pw.println(this.mWindowManagerDrawComplete);
        pw.print(prefix);
        pw.print("mOrientationSensorEnabled=");
        pw.println(this.mOrientationSensorEnabled);
        pw.print(prefix);
        pw.print("mOverscanScreen=(");
        pw.print(this.mOverscanScreenLeft);
        pw.print(",");
        pw.print(this.mOverscanScreenTop);
        pw.print(") ");
        pw.print(this.mOverscanScreenWidth);
        pw.print("x");
        pw.println(this.mOverscanScreenHeight);
        if (this.mOverscanLeft != 0 || this.mOverscanTop != 0 || this.mOverscanRight != 0 || this.mOverscanBottom != 0) {
            pw.print(prefix);
            pw.print("mOverscan left=");
            pw.print(this.mOverscanLeft);
            pw.print(" top=");
            pw.print(this.mOverscanTop);
            pw.print(" right=");
            pw.print(this.mOverscanRight);
            pw.print(" bottom=");
            pw.println(this.mOverscanBottom);
        }
        pw.print(prefix);
        pw.print("mRestrictedOverscanScreen=(");
        pw.print(this.mRestrictedOverscanScreenLeft);
        pw.print(",");
        pw.print(this.mRestrictedOverscanScreenTop);
        pw.print(") ");
        pw.print(this.mRestrictedOverscanScreenWidth);
        pw.print("x");
        pw.println(this.mRestrictedOverscanScreenHeight);
        pw.print(prefix);
        pw.print("mUnrestrictedScreen=(");
        pw.print(this.mUnrestrictedScreenLeft);
        pw.print(",");
        pw.print(this.mUnrestrictedScreenTop);
        pw.print(") ");
        pw.print(this.mUnrestrictedScreenWidth);
        pw.print("x");
        pw.println(this.mUnrestrictedScreenHeight);
        pw.print(prefix);
        pw.print("mRestrictedScreen=(");
        pw.print(this.mRestrictedScreenLeft);
        pw.print(",");
        pw.print(this.mRestrictedScreenTop);
        pw.print(") ");
        pw.print(this.mRestrictedScreenWidth);
        pw.print("x");
        pw.println(this.mRestrictedScreenHeight);
        pw.print(prefix);
        pw.print("mStableFullscreen=(");
        pw.print(this.mStableFullscreenLeft);
        pw.print(",");
        pw.print(this.mStableFullscreenTop);
        pw.print(")-(");
        pw.print(this.mStableFullscreenRight);
        pw.print(",");
        pw.print(this.mStableFullscreenBottom);
        pw.println(")");
        pw.print(prefix);
        pw.print("mStable=(");
        pw.print(this.mStableLeft);
        pw.print(",");
        pw.print(this.mStableTop);
        pw.print(")-(");
        pw.print(this.mStableRight);
        pw.print(",");
        pw.print(this.mStableBottom);
        pw.println(")");
        pw.print(prefix);
        pw.print("mSystem=(");
        pw.print(this.mSystemLeft);
        pw.print(",");
        pw.print(this.mSystemTop);
        pw.print(")-(");
        pw.print(this.mSystemRight);
        pw.print(",");
        pw.print(this.mSystemBottom);
        pw.println(")");
        pw.print(prefix);
        pw.print("mCur=(");
        pw.print(this.mCurLeft);
        pw.print(",");
        pw.print(this.mCurTop);
        pw.print(")-(");
        pw.print(this.mCurRight);
        pw.print(",");
        pw.print(this.mCurBottom);
        pw.println(")");
        pw.print(prefix);
        pw.print("mContent=(");
        pw.print(this.mContentLeft);
        pw.print(",");
        pw.print(this.mContentTop);
        pw.print(")-(");
        pw.print(this.mContentRight);
        pw.print(",");
        pw.print(this.mContentBottom);
        pw.println(")");
        pw.print(prefix);
        pw.print("mVoiceContent=(");
        pw.print(this.mVoiceContentLeft);
        pw.print(",");
        pw.print(this.mVoiceContentTop);
        pw.print(")-(");
        pw.print(this.mVoiceContentRight);
        pw.print(",");
        pw.print(this.mVoiceContentBottom);
        pw.println(")");
        pw.print(prefix);
        pw.print("mDock=(");
        pw.print(this.mDockLeft);
        pw.print(",");
        pw.print(this.mDockTop);
        pw.print(")-(");
        pw.print(this.mDockRight);
        pw.print(",");
        pw.print(this.mDockBottom);
        pw.println(")");
        pw.print(prefix);
        pw.print("mDockLayer=");
        pw.print(this.mDockLayer);
        pw.print(" mStatusBarLayer=");
        pw.println(this.mStatusBarLayer);
        pw.print(prefix);
        pw.print("mShowingLockscreen=");
        pw.print(this.mShowingLockscreen);
        pw.print(" mShowingDream=");
        pw.print(this.mShowingDream);
        pw.print(" mDreamingLockscreen=");
        pw.print(this.mDreamingLockscreen);
        pw.print(" mDreamingSleepToken=");
        pw.println(this.mDreamingSleepToken);
        if (this.mLastInputMethodWindow != null) {
            pw.print(prefix);
            pw.print("mLastInputMethodWindow=");
            pw.println(this.mLastInputMethodWindow);
        }
        if (this.mLastInputMethodTargetWindow != null) {
            pw.print(prefix);
            pw.print("mLastInputMethodTargetWindow=");
            pw.println(this.mLastInputMethodTargetWindow);
        }
        if (this.mStatusBar != null) {
            pw.print(prefix);
            pw.print("mStatusBar=");
            pw.print(this.mStatusBar);
            pw.print(" isStatusBarKeyguard=");
            pw.println(isStatusBarKeyguard());
        }
        if (this.mNavigationBar != null) {
            pw.print(prefix);
            pw.print("mNavigationBar=");
            pw.println(this.mNavigationBar);
        }
        if (this.mFocusedWindow != null) {
            pw.print(prefix);
            pw.print("mFocusedWindow=");
            pw.println(this.mFocusedWindow);
        }
        if (this.mFocusedApp != null) {
            pw.print(prefix);
            pw.print("mFocusedApp=");
            pw.println(this.mFocusedApp);
        }
        if (this.mWinDismissingKeyguard != null) {
            pw.print(prefix);
            pw.print("mWinDismissingKeyguard=");
            pw.println(this.mWinDismissingKeyguard);
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            pw.print(prefix);
            pw.print("mTopFullscreenOpaqueWindowState=");
            pw.println(this.mTopFullscreenOpaqueWindowState);
        }
        if (this.mTopFullscreenOpaqueOrDimmingWindowState != null) {
            pw.print(prefix);
            pw.print("mTopFullscreenOpaqueOrDimmingWindowState=");
            pw.println(this.mTopFullscreenOpaqueOrDimmingWindowState);
        }
        if (this.mForcingShowNavBar) {
            pw.print(prefix);
            pw.print("mForcingShowNavBar=");
            pw.println(this.mForcingShowNavBar);
            pw.print("mForcingShowNavBarLayer=");
            pw.println(this.mForcingShowNavBarLayer);
        }
        pw.print(prefix);
        pw.print("mTopIsFullscreen=");
        pw.print(this.mTopIsFullscreen);
        pw.print(" mHideLockScreen=");
        pw.println(this.mHideLockScreen);
        pw.print(prefix);
        pw.print("mForceStatusBar=");
        pw.print(this.mForceStatusBar);
        pw.print(" mForceStatusBarFromKeyguard=");
        pw.println(this.mForceStatusBarFromKeyguard);
        pw.print(prefix);
        pw.print("mDismissKeyguard=");
        pw.print(this.mDismissKeyguard);
        pw.print(" mWinDismissingKeyguard=");
        pw.print(this.mWinDismissingKeyguard);
        pw.print(" mHomePressed=");
        pw.println(this.mHomePressed);
        pw.print(prefix);
        pw.print("mAllowLockscreenWhenOn=");
        pw.print(this.mAllowLockscreenWhenOn);
        pw.print(" mLockScreenTimeout=");
        pw.print(this.mLockScreenTimeout);
        pw.print(" mLockScreenTimerActive=");
        pw.println(this.mLockScreenTimerActive);
        pw.print(prefix);
        pw.print("mEndcallBehavior=");
        pw.print(this.mEndcallBehavior);
        pw.print(" mIncallPowerBehavior=");
        pw.print(this.mIncallPowerBehavior);
        pw.print(" mLongPressOnHomeBehavior=");
        pw.println(this.mLongPressOnHomeBehavior);
        pw.print(prefix);
        pw.print("mLandscapeRotation=");
        pw.print(this.mLandscapeRotation);
        pw.print(" mSeascapeRotation=");
        pw.println(this.mSeascapeRotation);
        pw.print(prefix);
        pw.print("mPortraitRotation=");
        pw.print(this.mPortraitRotation);
        pw.print(" mUpsideDownRotation=");
        pw.println(this.mUpsideDownRotation);
        pw.print(prefix);
        pw.print("mDemoHdmiRotation=");
        pw.print(this.mDemoHdmiRotation);
        pw.print(" mDemoHdmiRotationLock=");
        pw.println(this.mDemoHdmiRotationLock);
        pw.print(prefix);
        pw.print("mUndockedHdmiRotation=");
        pw.println(this.mUndockedHdmiRotation);
        this.mGlobalKeyManager.dump(prefix, pw);
        this.mStatusBarController.dump(pw, prefix);
        this.mNavigationBarController.dump(pw, prefix);
        PolicyControl.dump(prefix, pw);
        if (this.mWakeGestureListener != null) {
            this.mWakeGestureListener.dump(pw, prefix);
        }
        if (this.mOrientationListener != null) {
            this.mOrientationListener.dump(pw, prefix);
        }
        if (this.mBurnInProtectionHelper != null) {
            this.mBurnInProtectionHelper.dump(prefix, pw);
        }
        if (this.mKeyguardDelegate == null) {
            return;
        }
        this.mKeyguardDelegate.dump(prefix, pw);
    }

    private boolean isAlarmBoot() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        return bootReason != null && bootReason.equals("1");
    }

    private void ipoSystemBooted() {
        this.mIsAlarmBoot = isAlarmBoot();
        this.mIsShutDown = false;
        this.mHideLockScreen = false;
        this.mScreenshotChordVolumeDownKeyTriggered = false;
        this.mScreenshotChordVolumeUpKeyTriggered = false;
        synchronized (this.mKeyDispatchLock) {
            this.mKeyDispatcMode = 0;
            if (DEBUG_INPUT) {
                Log.v(TAG, "mIpoEventReceiver=" + this.mKeyDispatcMode);
            }
        }
        if (this.mIPOUserRotation == 0) {
            return;
        }
        this.mUserRotation = this.mIPOUserRotation;
        this.mIPOUserRotation = 0;
    }

    private void ipoSystemShutdown() {
        synchronized (this.mKeyDispatchLock) {
            this.mKeyDispatcMode = 1;
            if (DEBUG_INPUT) {
                Log.v(TAG, "mIpoEventReceiver=" + this.mKeyDispatcMode);
            }
        }
        if (this.mUserRotationMode != 1 || this.mUserRotation == 0) {
            return;
        }
        this.mIPOUserRotation = this.mUserRotation;
        this.mUserRotation = 0;
    }

    private void keyRemappingSendFakeKeyEvent(int action, int keyCode) {
        long eventTime = SystemClock.uptimeMillis();
        if (action == 0) {
            this.mKeyRemappingSendFakeKeyDownTime = eventTime;
        }
        KeyEvent keyEvent = new KeyEvent(this.mKeyRemappingSendFakeKeyDownTime, eventTime, action, keyCode, 0);
        InputManager inputManager = (InputManager) this.mContext.getSystemService("input");
        inputManager.injectInputEvent(keyEvent, 0);
    }

    private void interceptDismissPinningChord() {
        IActivityManager activityManager = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
        try {
            if (!activityManager.isInLockTaskMode()) {
                return;
            }
            activityManager.stopLockTaskMode();
        } catch (RemoteException e) {
        }
    }

    public View addFastStartingWindow(IBinder appToken, String packageName, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, Bitmap bitmap) {
        if (packageName == null) {
            return null;
        }
        WindowManager wm = null;
        View view = new View(this.mContext);
        try {
            try {
                try {
                    Context context = this.mContext;
                    if (DEBUG_STARTING_WINDOW) {
                        Slog.d(TAG, "addFastStartingWindow " + packageName + ": nonLocalizedLabel=" + nonLocalizedLabel + " theme=" + Integer.toHexString(theme));
                    }
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams(-1, -1);
                    params.type = 3;
                    params.flags = windowFlags | 16 | 8 | PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS | Integer.MIN_VALUE | 256;
                    TypedArray windowStyle = this.mContext.obtainStyledAttributes(com.android.internal.R.styleable.Window);
                    params.windowAnimations = windowStyle.getResourceId(8, 0);
                    params.token = appToken;
                    params.packageName = packageName;
                    params.privateFlags |= 1;
                    params.privateFlags |= 16;
                    if (!compatInfo.supportsScreen()) {
                        params.privateFlags |= 128;
                    }
                    params.setTitle("FastStarting");
                    wm = (WindowManager) context.getSystemService("window");
                    if (DEBUG_STARTING_WINDOW) {
                        Slog.d(TAG, "Adding starting window for " + packageName + " / " + appToken + ": " + (view.getParent() != null ? view : null));
                    }
                    wm.addView(view, params);
                    if (this.mAppLaunchTimeEnabled) {
                        WindowManagerGlobal.getInstance().doTraversal(view, true);
                    }
                    View view2 = view.getParent() != null ? view : null;
                    if (view != null && view.getParent() == null) {
                        Log.w(TAG, "view not successfully added to wm, removing view");
                        wm.removeViewImmediate(view);
                    }
                    return view2;
                } catch (RuntimeException e) {
                    Log.w(TAG, appToken + " failed creating starting window", e);
                    if (view == null || view.getParent() != null) {
                        return null;
                    }
                    Log.w(TAG, "view not successfully added to wm, removing view");
                    wm.removeViewImmediate(view);
                    return null;
                }
            } catch (WindowManager.BadTokenException e2) {
                Log.w(TAG, appToken + " already running, starting window not displayed. " + e2.getMessage());
                if (view == null || view.getParent() != null) {
                    return null;
                }
                Log.w(TAG, "view not successfully added to wm, removing view");
                wm.removeViewImmediate(view);
                return null;
            }
        } catch (Throwable th) {
            if (view != null && view.getParent() == null) {
                Log.w(TAG, "view not successfully added to wm, removing view");
                wm.removeViewImmediate(view);
            }
            throw th;
        }
    }

    private boolean interceptKeyBeforeHandling(KeyEvent event) {
        return this.isUspEnable && 26 == event.getKeyCode() && (SystemProperties.getInt("persist.mtk_usp_cfg_ctrl", 0) & 4) == 4;
    }
}
