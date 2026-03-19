package com.android.server.policy;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.IUiModeManager;
import android.app.ProfilerInfo;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.media.AudioAttributes;
import android.media.AudioManagerInternal;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.service.dreams.IDreamManager;
import android.service.vr.IPersistentVrStateCallbacks;
import android.telecom.TelecomManager;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.IApplicationToken;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.autofill.AutofillManagerInternal;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.internal.policy.KeyguardDismissCallback;
import com.android.internal.policy.PhoneWindow;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ScreenShapeHelper;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.widget.PointerLocationView;
import com.android.server.GestureLauncherService;
import com.android.server.LocalServices;
import com.android.server.NetworkManagementService;
import com.android.server.SystemServiceManager;
import com.android.server.UiModeManagerService;
import com.android.server.audio.AudioService;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.pm.DumpState;
import com.android.server.policy.BarController;
import com.android.server.policy.SystemGesturesPointerEventListener;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.policy.keyguard.KeyguardStateMonitor;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.vr.VrManagerInternal;
import com.android.server.wm.AppTransition;
import com.android.server.wm.DisplayFrames;
import com.android.server.wm.WindowManagerInternal;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.powerhal.PowerHalManager;
import com.mediatek.server.wm.WindowManagerDebugger;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

public class PhoneWindowManager implements WindowManagerPolicy {
    static final boolean ALTERNATE_CAR_MODE_NAV_SIZE = false;
    private static final int BRIGHTNESS_STEPS = 10;
    private static final long BUGREPORT_TV_GESTURE_TIMEOUT_MILLIS = 1000;
    static final int DOUBLE_TAP_HOME_NOTHING = 0;
    static final int DOUBLE_TAP_HOME_RECENT_SYSTEM_UI = 1;
    static final boolean ENABLE_DESK_DOCK_HOME_CAPTURE = false;
    static final boolean ENABLE_VR_HEADSET_HOME_CAPTURE = true;
    private static final float KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER = 2.5f;
    static final int LAST_LONG_PRESS_HOME_BEHAVIOR = 2;
    static final int LONG_PRESS_BACK_GO_TO_VOICE_ASSIST = 1;
    static final int LONG_PRESS_BACK_NOTHING = 0;
    static final int LONG_PRESS_HOME_ALL_APPS = 1;
    static final int LONG_PRESS_HOME_ASSIST = 2;
    static final int LONG_PRESS_HOME_NOTHING = 0;
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int LONG_PRESS_POWER_GO_TO_VOICE_ASSIST = 4;
    static final int LONG_PRESS_POWER_NOTHING = 0;
    static final int LONG_PRESS_POWER_SHUT_OFF = 2;
    static final int LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM = 3;
    private static final int MSG_ACCESSIBILITY_SHORTCUT = 20;
    private static final int MSG_ACCESSIBILITY_TV = 22;
    private static final int MSG_BACK_LONG_PRESS = 18;
    private static final int MSG_BUGREPORT_TV = 21;
    private static final int MSG_DISABLE_POINTER_LOCATION = 2;
    private static final int MSG_DISPATCH_BACK_KEY_TO_AUTOFILL = 23;
    private static final int MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK = 4;
    private static final int MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK = 3;
    private static final int MSG_DISPATCH_SHOW_GLOBAL_ACTIONS = 10;
    private static final int MSG_DISPATCH_SHOW_RECENTS = 9;
    private static final int MSG_DISPOSE_INPUT_CONSUMER = 19;
    private static final int MSG_ENABLE_POINTER_LOCATION = 1;
    private static final int MSG_HANDLE_ALL_APPS = 25;
    private static final int MSG_HIDE_BOOT_MESSAGE = 11;
    private static final int MSG_KEYGUARD_DRAWN_COMPLETE = 5;
    private static final int MSG_KEYGUARD_DRAWN_TIMEOUT = 6;
    private static final int MSG_LAUNCH_ASSIST = 26;
    private static final int MSG_LAUNCH_ASSIST_LONG_PRESS = 27;
    private static final int MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK = 12;
    private static final int MSG_NOTIFY_USER_ACTIVITY = 29;
    private static final int MSG_POWER_DELAYED_PRESS = 13;
    private static final int MSG_POWER_LONG_PRESS = 14;
    private static final int MSG_POWER_VERY_LONG_PRESS = 28;
    private static final int MSG_REQUEST_TRANSIENT_BARS = 16;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_NAVIGATION = 1;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_STATUS = 0;
    private static final int MSG_RINGER_TOGGLE_CHORD = 30;
    private static final int MSG_SHOW_PICTURE_IN_PICTURE_MENU = 17;
    private static final int MSG_SYSTEM_KEY_PRESS = 24;
    private static final int MSG_UPDATE_DREAMING_SLEEP_TOKEN = 15;
    private static final int MSG_WINDOW_MANAGER_DRAWN_COMPLETE = 7;
    static final int MULTI_PRESS_POWER_BRIGHTNESS_BOOST = 2;
    static final int MULTI_PRESS_POWER_NOTHING = 0;
    static final int MULTI_PRESS_POWER_THEATER_MODE = 1;
    static final int NAV_BAR_OPAQUE_WHEN_FREEFORM_OR_DOCKED = 0;
    static final int NAV_BAR_TRANSLUCENT_WHEN_FREEFORM_OPAQUE_OTHERWISE = 1;
    private static final long PANIC_GESTURE_EXPIRATION = 30000;
    static final int PENDING_KEY_NULL = -1;
    static final boolean PRINT_ANIM = false;
    private static final long SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS = 150;
    static final int SHORT_PRESS_POWER_CLOSE_IME_OR_GO_HOME = 5;
    static final int SHORT_PRESS_POWER_GO_HOME = 4;
    static final int SHORT_PRESS_POWER_GO_TO_SLEEP = 1;
    static final int SHORT_PRESS_POWER_NOTHING = 0;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP = 2;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME = 3;
    static final int SHORT_PRESS_SLEEP_GO_TO_SLEEP = 0;
    static final int SHORT_PRESS_SLEEP_GO_TO_SLEEP_AND_GO_HOME = 1;
    static final int SHORT_PRESS_WINDOW_NOTHING = 0;
    static final int SHORT_PRESS_WINDOW_PICTURE_IN_PICTURE = 1;
    public static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";
    public static final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    public static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    public static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    public static final String SYSTEM_DIALOG_REASON_SCREENSHOT = "screenshot";
    static final int SYSTEM_UI_CHANGING_LAYOUT = -1073709042;
    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_SCREENSHOT_ERROR_RECEIVER = "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver";
    private static final String SYSUI_SCREENSHOT_SERVICE = "com.android.systemui.screenshot.TakeScreenshotService";
    static final String TAG = "WindowManager";
    public static final int TOAST_WINDOW_TIMEOUT = 3500;
    private static final int USER_ACTIVITY_NOTIFICATION_DELAY = 200;
    static final int VERY_LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int VERY_LONG_PRESS_POWER_NOTHING = 0;
    static final int WAITING_FOR_DRAWN_TIMEOUT = 1000;
    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK;
    static final Rect mTmpContentFrame;
    static final Rect mTmpDecorFrame;
    private static final Rect mTmpDisplayCutoutSafeExceptMaybeBarsRect;
    static final Rect mTmpDisplayFrame;
    static final Rect mTmpNavigationFrame;
    static final Rect mTmpOutsetFrame;
    static final Rect mTmpOverscanFrame;
    static final Rect mTmpParentFrame;
    private static final Rect mTmpRect;
    static final Rect mTmpStableFrame;
    static final Rect mTmpVisibleFrame;
    private boolean mA11yShortcutChordVolumeUpKeyConsumed;
    private long mA11yShortcutChordVolumeUpKeyTime;
    private boolean mA11yShortcutChordVolumeUpKeyTriggered;
    AccessibilityManager mAccessibilityManager;
    private AccessibilityShortcutController mAccessibilityShortcutController;
    private boolean mAccessibilityTvKey1Pressed;
    private boolean mAccessibilityTvKey2Pressed;
    private boolean mAccessibilityTvScheduled;
    ActivityManagerInternal mActivityManagerInternal;
    boolean mAllowLockscreenWhenOn;
    boolean mAllowStartActivityForLongPressOnPowerDuringSetup;
    private boolean mAllowTheaterModeWakeFromCameraLens;
    private boolean mAllowTheaterModeWakeFromKey;
    private boolean mAllowTheaterModeWakeFromLidSwitch;
    private boolean mAllowTheaterModeWakeFromMotion;
    private boolean mAllowTheaterModeWakeFromMotionWhenNotDreaming;
    private boolean mAllowTheaterModeWakeFromPowerKey;
    private boolean mAllowTheaterModeWakeFromWakeGesture;
    private boolean mAodShowing;
    AppOpsManager mAppOpsManager;
    AudioManagerInternal mAudioManagerInternal;
    AutofillManagerInternal mAutofillManagerInternal;
    volatile boolean mAwake;
    volatile boolean mBackKeyHandled;
    volatile boolean mBeganFromNonInteractive;
    boolean mBootMessageNeedsHiding;
    PowerManager.WakeLock mBroadcastWakeLock;
    private boolean mBugreportTvKey1Pressed;
    private boolean mBugreportTvKey2Pressed;
    private boolean mBugreportTvScheduled;
    BurnInProtectionHelper mBurnInProtectionHelper;
    long[] mCalendarDateVibePattern;
    volatile boolean mCameraGestureTriggeredDuringGoingToSleep;
    boolean mCarDockEnablesAccelerometer;
    Intent mCarDockIntent;
    int mCarDockRotation;
    boolean mConsumeSearchKeyUp;
    Context mContext;
    private int mCurrentUserId;
    int mDemoHdmiRotation;
    boolean mDemoHdmiRotationLock;
    int mDemoRotation;
    boolean mDemoRotationLock;
    boolean mDeskDockEnablesAccelerometer;
    Intent mDeskDockIntent;
    int mDeskDockRotation;
    private volatile boolean mDismissImeOnBackKeyPressed;
    Display mDisplay;
    int mDockLayer;
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
    boolean mForceHideNavBar;
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
    private boolean mHandleVolumeKeysInWM;
    Handler mHandler;
    private boolean mHasFeatureLeanback;
    private boolean mHasFeatureWatch;
    boolean mHaveBuiltInKeyboard;
    boolean mHavePendingMediaKeyRepeatWithWakeLock;
    HdmiControl mHdmiControl;
    boolean mHdmiPlugged;
    boolean mHomeConsumed;
    boolean mHomeDoubleTapPending;
    Intent mHomeIntent;
    boolean mHomePressed;
    private ImmersiveModeConfirmation mImmersiveModeConfirmation;
    int mIncallBackBehavior;
    int mIncallPowerBehavior;
    int mInitialMetaState;
    InputManagerInternal mInputManagerInternal;
    InputMethodManagerInternal mInputMethodManagerInternal;
    private boolean mKeyguardBound;
    KeyguardServiceDelegate mKeyguardDelegate;
    boolean mKeyguardDrawComplete;
    private boolean mKeyguardDrawnOnce;
    volatile boolean mKeyguardOccluded;
    private boolean mKeyguardOccludedChanged;
    boolean mLanguageSwitchKeyPressed;
    int mLastDockedStackSysUiFlags;
    int mLastFullscreenStackSysUiFlags;
    private boolean mLastShowingDream;
    int mLastSystemUiFlags;
    private boolean mLastWindowSleepTokenNeeded;
    boolean mLidControlsScreenLock;
    boolean mLidControlsSleep;
    int mLidKeyboardAccessibility;
    int mLidNavigationAccessibility;
    int mLidOpenRotation;
    int mLockScreenTimeout;
    boolean mLockScreenTimerActive;
    MetricsLogger mLogger;
    int mLongPressOnBackBehavior;
    private int mLongPressOnHomeBehavior;
    int mLongPressOnPowerBehavior;
    long[] mLongPressVibePattern;
    int mMetaState;
    private boolean mNotifyUserActivity;
    MyOrientationListener mOrientationListener;
    boolean mPendingCapsLockToggle;
    private boolean mPendingKeyguardOccluded;
    boolean mPendingMetaAction;
    private long mPendingPanicGestureUptime;
    private volatile boolean mPersistentVrModeEnabled;
    volatile boolean mPictureInPictureVisible;
    PointerLocationView mPointerLocationView;
    volatile boolean mPowerKeyHandled;
    volatile int mPowerKeyPressCounter;
    PowerManager.WakeLock mPowerKeyWakeLock;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    boolean mPreloadedRecentApps;
    int mRecentAppsHeldModifiers;
    volatile boolean mRecentsVisible;
    volatile boolean mRequestedOrGoingToSleep;
    boolean mSafeMode;
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
    private ScreenshotHelper mScreenshotHelper;
    boolean mSearchKeyShortcutPending;
    SearchManager mSearchManager;
    SettingsObserver mSettingsObserver;
    int mShortPressOnPowerBehavior;
    int mShortPressOnSleepBehavior;
    int mShortPressOnWindowBehavior;
    ShortcutManager mShortcutManager;
    int mShowRotationSuggestions;
    boolean mShowingDream;
    int mStatusBarLayer;
    StatusBarManagerInternal mStatusBarManagerInternal;
    IStatusBarService mStatusBarService;
    boolean mSupportAutoRotation;
    private boolean mSupportLongPressPowerWhenNonInteractive;
    boolean mSystemBooted;

    @VisibleForTesting
    SystemGesturesPointerEventListener mSystemGestures;
    boolean mSystemNavigationKeysEnabled;
    boolean mSystemReady;
    WindowManagerPolicy.WindowState mTopDockedOpaqueOrDimmingWindowState;
    WindowManagerPolicy.WindowState mTopDockedOpaqueWindowState;
    WindowManagerPolicy.WindowState mTopFullscreenOpaqueOrDimmingWindowState;
    WindowManagerPolicy.WindowState mTopFullscreenOpaqueWindowState;
    boolean mTopIsFullscreen;
    int mTriplePressOnPowerBehavior;
    int mUiMode;
    IUiModeManager mUiModeManager;
    int mUndockedHdmiRotation;
    boolean mUseTvRouting;
    int mVeryLongPressOnPowerBehavior;
    int mVeryLongPressTimeout;
    Vibrator mVibrator;
    Intent mVrHeadsetHomeIntent;
    volatile VrManagerInternal mVrManagerInternal;
    boolean mWakeGestureEnabledSetting;
    MyWakeGestureListener mWakeGestureListener;
    IWindowManager mWindowManager;
    boolean mWindowManagerDrawComplete;
    WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    WindowManagerInternal mWindowManagerInternal;

    @GuardedBy("mHandler")
    private ActivityManagerInternal.SleepToken mWindowSleepToken;
    private boolean mWindowSleepTokenNeeded;
    static boolean DEBUG = false;
    static boolean localLOGV = false;
    static boolean DEBUG_INPUT = false;
    static boolean DEBUG_KEYGUARD = false;
    static boolean DEBUG_LAYOUT = false;
    static boolean DEBUG_SPLASH_SCREEN = false;
    static boolean DEBUG_WAKEUP = false;
    static boolean SHOW_SPLASH_SCREENS = true;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    static SparseArray<String> sApplicationLaunchKeyCategories = new SparseArray<>();
    private WindowManagerDebugger mWindowManagerDebugger = MtkSystemServiceFactory.getInstance().makeWindowManagerDebugger();
    private final Object mLock = new Object();
    final Object mServiceAquireLock = new Object();
    boolean mEnableShiftMenuBugReports = false;
    private final ArraySet<WindowManagerPolicy.WindowState> mScreenDecorWindows = new ArraySet<>();
    WindowManagerPolicy.WindowState mStatusBar = null;
    private final int[] mStatusBarHeightForRotation = new int[4];
    WindowManagerPolicy.WindowState mNavigationBar = null;
    boolean mHasNavigationBar = false;
    boolean mNavigationBarCanMove = false;
    int mNavigationBarPosition = 4;
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
                Slog.i("WindowManager", "All windows ready for display!");
            }
            PhoneWindowManager.this.mHandler.sendEmptyMessage(7);
        }
    };
    final KeyguardServiceDelegate.DrawnListener mKeyguardDrawnCallback = new KeyguardServiceDelegate.DrawnListener() {
        @Override
        public void onDrawn() {
            if (PhoneWindowManager.DEBUG_WAKEUP) {
                Slog.d("WindowManager", "mKeyguardDelegate.ShowListener.onDrawn.");
            }
            PhoneWindowManager.this.mHandler.sendEmptyMessage(5);
        }
    };
    WindowManagerPolicy.WindowState mLastInputMethodWindow = null;
    WindowManagerPolicy.WindowState mLastInputMethodTargetWindow = null;
    volatile boolean mNavBarVirtualKeyHapticFeedbackEnabled = true;
    volatile int mPendingWakeKey = -1;
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
    int mNavBarOpacityMode = 0;
    int mLandscapeRotation = 0;
    int mSeascapeRotation = 0;
    int mPortraitRotation = 0;
    int mUpsideDownRotation = 0;
    private int mRingerToggleChord = 0;
    private final SparseArray<KeyCharacterMap.FallbackAction> mFallbackActions = new SparseArray<>();
    private final LogDecelerateInterpolator mLogDecelerateInterpolator = new LogDecelerateInterpolator(100, 0);
    private final MutableBoolean mTmpBoolean = new MutableBoolean(false);
    private UEventObserver mHDMIObserver = new UEventObserver() {
        public void onUEvent(UEventObserver.UEvent uEvent) {
            PhoneWindowManager.this.setHdmiPlugged("1".equals(uEvent.get("SWITCH_STATE")));
        }
    };
    final IPersistentVrStateCallbacks mPersistentVrModeListener = new IPersistentVrStateCallbacks.Stub() {
        public void onPersistentVrStateChanged(boolean z) {
            PhoneWindowManager.this.mPersistentVrModeEnabled = z;
        }
    };
    private final StatusBarController mStatusBarController = new StatusBarController();
    private final BarController mNavigationBarController = new BarController("NavigationBar", 134217728, 536870912, Integer.MIN_VALUE, 2, 134217728, 32768);
    private final BarController.OnBarVisibilityChangedListener mNavBarVisibilityListener = new BarController.OnBarVisibilityChangedListener() {
        @Override
        public void onBarVisibilityChanged(boolean z) {
            PhoneWindowManager.this.mAccessibilityManager.notifyAccessibilityButtonVisibilityChanged(z);
        }
    };
    private final Runnable mAcquireSleepTokenRunnable = new Runnable() {
        @Override
        public final void run() {
            PhoneWindowManager.lambda$new$0(this.f$0);
        }
    };
    private final Runnable mReleaseSleepTokenRunnable = new Runnable() {
        @Override
        public final void run() {
            PhoneWindowManager.lambda$new$1(this.f$0);
        }
    };
    private final Runnable mEndCallLongPress = new Runnable() {
        @Override
        public void run() {
            PhoneWindowManager.this.mEndCallKeyHandled = true;
            PhoneWindowManager.this.performHapticFeedbackLw(null, 0, false);
            PhoneWindowManager.this.showGlobalActionsInternal();
        }
    };
    private final ScreenshotRunnable mScreenshotRunnable = new ScreenshotRunnable();
    private final Runnable mHomeDoubleTapTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (PhoneWindowManager.this.mHomeDoubleTapPending) {
                PhoneWindowManager.this.mHomeDoubleTapPending = false;
                PhoneWindowManager.this.handleShortPressOnHome();
            }
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
    BroadcastReceiver mDockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.DOCK_EVENT".equals(intent.getAction())) {
                PhoneWindowManager.this.mDockMode = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
            } else {
                try {
                    IUiModeManager iUiModeManagerAsInterface = IUiModeManager.Stub.asInterface(ServiceManager.getService("uimode"));
                    PhoneWindowManager.this.mUiMode = iUiModeManagerAsInterface.getCurrentModeType();
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
                Slog.v("WindowManager", "*** onDreamingStarted");
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.onDreamingStarted();
                    return;
                }
                return;
            }
            if ("android.intent.action.DREAMING_STOPPED".equals(intent.getAction())) {
                Slog.v("WindowManager", "*** onDreamingStopped");
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.onDreamingStopped();
                }
            }
        }
    };
    BroadcastReceiver mMultiuserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                PhoneWindowManager.this.mSettingsObserver.onChange(false);
                synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                    PhoneWindowManager.this.mLastSystemUiFlags = 0;
                    PhoneWindowManager.this.updateSystemUiVisibilityLw();
                }
            }
        }
    };
    private final Runnable mHiddenNavPanic = new Runnable() {
        @Override
        public void run() {
            synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                if (PhoneWindowManager.this.isUserSetupComplete()) {
                    PhoneWindowManager.this.mPendingPanicGestureUptime = SystemClock.uptimeMillis();
                    if (!PhoneWindowManager.isNavBarEmpty(PhoneWindowManager.this.mLastSystemUiFlags)) {
                        PhoneWindowManager.this.mNavigationBarController.showTransient();
                    }
                }
            }
        }
    };
    ProgressDialog mBootMsgDialog = null;
    ScreenLockTimeout mScreenLockTimeout = new ScreenLockTimeout();
    private WmsExt mWmsExt = MtkSystemServiceFactory.getInstance().makeWmsExt();

    static {
        sApplicationLaunchKeyCategories.append(64, "android.intent.category.APP_BROWSER");
        sApplicationLaunchKeyCategories.append(65, "android.intent.category.APP_EMAIL");
        sApplicationLaunchKeyCategories.append(207, "android.intent.category.APP_CONTACTS");
        sApplicationLaunchKeyCategories.append(208, "android.intent.category.APP_CALENDAR");
        sApplicationLaunchKeyCategories.append(209, "android.intent.category.APP_MUSIC");
        sApplicationLaunchKeyCategories.append(NetworkManagementService.NetdResponseCode.TetherStatusResult, "android.intent.category.APP_CALCULATOR");
        mTmpParentFrame = new Rect();
        mTmpDisplayFrame = new Rect();
        mTmpOverscanFrame = new Rect();
        mTmpContentFrame = new Rect();
        mTmpVisibleFrame = new Rect();
        mTmpDecorFrame = new Rect();
        mTmpStableFrame = new Rect();
        mTmpNavigationFrame = new Rect();
        mTmpOutsetFrame = new Rect();
        mTmpDisplayCutoutSafeExceptMaybeBarsRect = new Rect();
        mTmpRect = new Rect();
        WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = new int[]{2003, 2010};
    }

    private class PolicyHandler extends Handler {
        private PolicyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    PhoneWindowManager.this.enablePointerLocation();
                    return;
                case 2:
                    PhoneWindowManager.this.disablePointerLocation();
                    return;
                case 3:
                    PhoneWindowManager.this.dispatchMediaKeyWithWakeLock((KeyEvent) message.obj);
                    return;
                case 4:
                    PhoneWindowManager.this.dispatchMediaKeyRepeatWithWakeLock((KeyEvent) message.obj);
                    return;
                case 5:
                    if (PhoneWindowManager.DEBUG_WAKEUP) {
                        Slog.w("WindowManager", "Setting mKeyguardDrawComplete");
                    }
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    return;
                case 6:
                    Slog.w("WindowManager", "Keyguard drawn timeout. Setting mKeyguardDrawComplete");
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    return;
                case 7:
                    if (PhoneWindowManager.DEBUG_WAKEUP) {
                        Slog.w("WindowManager", "Setting mWindowManagerDrawComplete");
                    }
                    PhoneWindowManager.this.finishWindowsDrawn();
                    return;
                case 8:
                default:
                    return;
                case 9:
                    PhoneWindowManager.this.showRecentApps(false);
                    return;
                case 10:
                    PhoneWindowManager.this.showGlobalActionsInternal();
                    return;
                case 11:
                    PhoneWindowManager.this.handleHideBootMessage();
                    return;
                case 12:
                    PhoneWindowManager.this.launchVoiceAssistWithWakeLock();
                    return;
                case 13:
                    PhoneWindowManager.this.powerPress(((Long) message.obj).longValue(), message.arg1 != 0, message.arg2);
                    PhoneWindowManager.this.finishPowerKeyPress();
                    return;
                case 14:
                    PhoneWindowManager.this.powerLongPress();
                    return;
                case 15:
                    PhoneWindowManager.this.updateDreamingSleepToken(message.arg1 != 0);
                    return;
                case 16:
                    WindowManagerPolicy.WindowState windowState = message.arg1 == 0 ? PhoneWindowManager.this.mStatusBar : PhoneWindowManager.this.mNavigationBar;
                    if (windowState != null) {
                        PhoneWindowManager.this.requestTransientBars(windowState);
                        return;
                    }
                    return;
                case 17:
                    PhoneWindowManager.this.showPictureInPictureMenuInternal();
                    return;
                case 18:
                    PhoneWindowManager.this.backLongPress();
                    return;
                case 19:
                    PhoneWindowManager.this.disposeInputConsumer((WindowManagerPolicy.InputConsumer) message.obj);
                    return;
                case 20:
                    PhoneWindowManager.this.accessibilityShortcutActivated();
                    return;
                case 21:
                    PhoneWindowManager.this.requestFullBugreport();
                    return;
                case 22:
                    if (PhoneWindowManager.this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(false)) {
                        PhoneWindowManager.this.accessibilityShortcutActivated();
                        return;
                    }
                    return;
                case 23:
                    PhoneWindowManager.this.mAutofillManagerInternal.onBackKeyPressed();
                    return;
                case 24:
                    PhoneWindowManager.this.sendSystemKeyToStatusBar(message.arg1);
                    return;
                case 25:
                    PhoneWindowManager.this.launchAllAppsAction();
                    return;
                case 26:
                    PhoneWindowManager.this.launchAssistAction((String) message.obj, message.arg1);
                    return;
                case PhoneWindowManager.MSG_LAUNCH_ASSIST_LONG_PRESS:
                    PhoneWindowManager.this.launchAssistLongPressAction();
                    return;
                case 28:
                    PhoneWindowManager.this.powerVeryLongPress();
                    return;
                case 29:
                    removeMessages(29);
                    Intent intent = new Intent("android.intent.action.USER_ACTIVITY_NOTIFICATION");
                    intent.addFlags(1073741824);
                    PhoneWindowManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.USER_ACTIVITY");
                    break;
                case 30:
                    break;
            }
            PhoneWindowManager.this.handleRingerChordGesture();
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver contentResolver = PhoneWindowManager.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(Settings.System.getUriFor("end_button_behavior"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("incall_power_button_behavior"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("incall_back_button_behavior"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("wake_gesture_enabled"), false, this, -1);
            contentResolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this, -1);
            contentResolver.registerContentObserver(Settings.System.getUriFor("user_rotation"), false, this, -1);
            contentResolver.registerContentObserver(Settings.System.getUriFor("screen_off_timeout"), false, this, -1);
            contentResolver.registerContentObserver(Settings.System.getUriFor("pointer_location"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("immersive_mode_confirmations"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("show_rotation_suggestions"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("volume_hush_gesture"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Global.getUriFor("policy_control"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("system_navigation_keys_enabled"), false, this, -1);
            contentResolver.registerContentObserver(Settings.System.getUriFor("hide_navigation_bar"), false, this, -1);
            PhoneWindowManager.this.updateSettings();
        }

        @Override
        public void onChange(boolean z) {
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
        private SparseArray<Runnable> mRunnableCache;

        MyOrientationListener(Context context, Handler handler) {
            super(context, handler);
            this.mRunnableCache = new SparseArray<>(5);
        }

        private class UpdateRunnable implements Runnable {
            private final int mRotation;

            UpdateRunnable(int i) {
                this.mRotation = i;
            }

            @Override
            public void run() {
                PhoneWindowManager.this.mPowerManagerInternal.powerHint(2, 0);
                if (PhoneWindowManager.this.isRotationChoicePossible(PhoneWindowManager.this.mCurrentAppOrientation)) {
                    PhoneWindowManager.this.sendProposedRotationChangeToStatusBarInternal(this.mRotation, PhoneWindowManager.this.isValidRotationChoice(PhoneWindowManager.this.mCurrentAppOrientation, this.mRotation));
                } else {
                    PhoneWindowManager.this.updateRotation(false);
                }
            }
        }

        @Override
        public void onProposedRotationChanged(int i) {
            if (PhoneWindowManager.localLOGV) {
                Slog.v("WindowManager", "onProposedRotationChanged, rotation=" + i);
            }
            Runnable updateRunnable = this.mRunnableCache.get(i, null);
            if (updateRunnable == null) {
                updateRunnable = new UpdateRunnable(i);
                this.mRunnableCache.put(i, updateRunnable);
            }
            PhoneWindowManager.this.mHandler.post(updateRunnable);
        }
    }

    public static void lambda$new$0(PhoneWindowManager phoneWindowManager) {
        if (phoneWindowManager.mWindowSleepToken != null) {
            return;
        }
        phoneWindowManager.mWindowSleepToken = phoneWindowManager.mActivityManagerInternal.acquireSleepToken("WindowSleepToken", 0);
    }

    public static void lambda$new$1(PhoneWindowManager phoneWindowManager) {
        if (phoneWindowManager.mWindowSleepToken == null) {
            return;
        }
        phoneWindowManager.mWindowSleepToken.release();
        phoneWindowManager.mWindowSleepToken = null;
    }

    private void handleRingerChordGesture() {
        if (this.mRingerToggleChord == 0) {
            return;
        }
        getAudioManagerInternal();
        this.mAudioManagerInternal.silenceRingerModeInternal("volume_hush");
        Settings.Secure.putInt(this.mContext.getContentResolver(), "hush_gesture_used", 1);
        this.mLogger.action(1440, this.mRingerToggleChord);
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

    AudioManagerInternal getAudioManagerInternal() {
        AudioManagerInternal audioManagerInternal;
        synchronized (this.mServiceAquireLock) {
            if (this.mAudioManagerInternal == null) {
                this.mAudioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
            }
            audioManagerInternal = this.mAudioManagerInternal;
        }
        return audioManagerInternal;
    }

    boolean needSensorRunningLp() {
        if (this.mSupportAutoRotation && (this.mCurrentAppOrientation == 4 || this.mCurrentAppOrientation == 10 || this.mCurrentAppOrientation == 7 || this.mCurrentAppOrientation == 6)) {
            return true;
        }
        if ((this.mCarDockEnablesAccelerometer && this.mDockMode == 2) || (this.mDeskDockEnablesAccelerometer && (this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == 4))) {
            return true;
        }
        if (this.mUserRotationMode == 1) {
            return !ActivityManager.isLowRamDeviceStatic() && this.mSupportAutoRotation && this.mShowRotationSuggestions == 1;
        }
        return this.mSupportAutoRotation;
    }

    void updateOrientationListenerLp() {
        if (!this.mOrientationListener.canDetectOrientation()) {
            return;
        }
        if (localLOGV) {
            Slog.v("WindowManager", "mScreenOnEarly=" + this.mScreenOnEarly + ", mAwake=" + this.mAwake + ", mCurrentAppOrientation=" + this.mCurrentAppOrientation + ", mOrientationSensorEnabled=" + this.mOrientationSensorEnabled + ", mKeyguardDrawComplete=" + this.mKeyguardDrawComplete + ", mWindowManagerDrawComplete=" + this.mWindowManagerDrawComplete);
        }
        boolean z = true;
        if (this.mScreenOnEarly && this.mAwake && this.mKeyguardDrawComplete && this.mWindowManagerDrawComplete && needSensorRunningLp()) {
            if (!this.mOrientationSensorEnabled) {
                this.mOrientationListener.enable(true);
                if (localLOGV) {
                    Slog.v("WindowManager", "Enabling listeners");
                }
                this.mOrientationSensorEnabled = true;
            }
            z = false;
        }
        if (z && this.mOrientationSensorEnabled) {
            this.mOrientationListener.disable();
            if (localLOGV) {
                Slog.v("WindowManager", "Disabling listeners");
            }
            this.mOrientationSensorEnabled = false;
        }
    }

    private void interceptBackKeyDown() {
        MetricsLogger.count(this.mContext, "key_back_down", 1);
        this.mBackKeyHandled = false;
        if (hasLongPressOnBackBehavior()) {
            Message messageObtainMessage = this.mHandler.obtainMessage(18);
            messageObtainMessage.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(messageObtainMessage, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
        }
    }

    private boolean interceptBackKeyUp(KeyEvent keyEvent) {
        TelecomManager telecommService;
        boolean z = this.mBackKeyHandled;
        cancelPendingBackKeyAction();
        if (this.mHasFeatureWatch && (telecommService = getTelecommService()) != null) {
            if (telecommService.isRinging()) {
                telecommService.silenceRinger();
                return false;
            }
            if ((this.mIncallBackBehavior & 1) != 0 && telecommService.isInCall()) {
                return telecommService.endCall();
            }
        }
        if (this.mAutofillManagerInternal != null && keyEvent.getKeyCode() == 4) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(23));
        }
        return z;
    }

    private void interceptPowerKeyDown(KeyEvent keyEvent, boolean z) {
        boolean zEndCall;
        boolean zInterceptPowerKeyDown;
        if (!this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.acquire();
        }
        if (this.mPowerKeyPressCounter != 0) {
            this.mHandler.removeMessages(13);
        }
        if (this.mImmersiveModeConfirmation.onPowerKeyDown(z, SystemClock.elapsedRealtime(), isImmersiveMode(this.mLastSystemUiFlags), isNavBarEmpty(this.mLastSystemUiFlags))) {
            this.mHandler.post(this.mHiddenNavPanic);
        }
        Handler handler = this.mHandler;
        WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs = this.mWindowManagerFuncs;
        Objects.requireNonNull(windowManagerFuncs);
        handler.post(new $$Lambda$oXa0y3A00RiQs6KTPBgpkGtgw(windowManagerFuncs));
        if (z && !this.mScreenshotChordPowerKeyTriggered && (keyEvent.getFlags() & 1024) == 0) {
            this.mScreenshotChordPowerKeyTriggered = true;
            this.mScreenshotChordPowerKeyTime = keyEvent.getDownTime();
            interceptScreenshotChord();
            interceptRingerToggleChord();
        }
        TelecomManager telecommService = getTelecommService();
        boolean z2 = false;
        if (telecommService != null) {
            if (telecommService.isRinging()) {
                telecommService.silenceRinger();
            } else if ((this.mIncallPowerBehavior & 2) != 0 && telecommService.isInCall() && z) {
                zEndCall = telecommService.endCall();
            }
            zEndCall = false;
        } else {
            zEndCall = false;
        }
        GestureLauncherService gestureLauncherService = (GestureLauncherService) LocalServices.getService(GestureLauncherService.class);
        if (gestureLauncherService != null) {
            zInterceptPowerKeyDown = gestureLauncherService.interceptPowerKeyDown(keyEvent, z, this.mTmpBoolean);
            if (this.mTmpBoolean.value && this.mRequestedOrGoingToSleep) {
                this.mCameraGestureTriggeredDuringGoingToSleep = true;
            }
        } else {
            zInterceptPowerKeyDown = false;
        }
        sendSystemKeyToStatusBarAsync(keyEvent.getKeyCode());
        if (zEndCall || this.mScreenshotChordVolumeDownKeyTriggered || this.mA11yShortcutChordVolumeUpKeyTriggered || zInterceptPowerKeyDown) {
            z2 = true;
        }
        this.mPowerKeyHandled = z2;
        if (this.mPowerKeyHandled) {
            return;
        }
        if (z) {
            if (hasLongPressOnPowerBehavior()) {
                if ((keyEvent.getFlags() & 128) != 0) {
                    powerLongPress();
                    return;
                }
                Message messageObtainMessage = this.mHandler.obtainMessage(14);
                messageObtainMessage.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(messageObtainMessage, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                if (hasVeryLongPressOnPowerBehavior()) {
                    Message messageObtainMessage2 = this.mHandler.obtainMessage(28);
                    messageObtainMessage2.setAsynchronous(true);
                    this.mHandler.sendMessageDelayed(messageObtainMessage2, this.mVeryLongPressTimeout);
                    return;
                }
                return;
            }
            return;
        }
        wakeUpFromPowerKey(keyEvent.getDownTime());
        if (this.mSupportLongPressPowerWhenNonInteractive && hasLongPressOnPowerBehavior()) {
            if ((keyEvent.getFlags() & 128) != 0) {
                powerLongPress();
            } else {
                Message messageObtainMessage3 = this.mHandler.obtainMessage(14);
                messageObtainMessage3.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(messageObtainMessage3, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                if (hasVeryLongPressOnPowerBehavior()) {
                    Message messageObtainMessage4 = this.mHandler.obtainMessage(28);
                    messageObtainMessage4.setAsynchronous(true);
                    this.mHandler.sendMessageDelayed(messageObtainMessage4, this.mVeryLongPressTimeout);
                }
            }
            this.mBeganFromNonInteractive = true;
            return;
        }
        if (getMaxMultiPressPowerCount() <= 1) {
            this.mPowerKeyHandled = true;
        } else {
            this.mBeganFromNonInteractive = true;
        }
    }

    private void interceptPowerKeyUp(KeyEvent keyEvent, boolean z, boolean z2) {
        Object[] objArr = z2 || this.mPowerKeyHandled;
        this.mScreenshotChordPowerKeyTriggered = false;
        cancelPendingScreenshotChordAction();
        cancelPendingPowerKeyAction();
        if (objArr == false) {
            this.mPowerKeyPressCounter++;
            int maxMultiPressPowerCount = getMaxMultiPressPowerCount();
            long downTime = keyEvent.getDownTime();
            if (this.mPowerKeyPressCounter < maxMultiPressPowerCount) {
                Message messageObtainMessage = this.mHandler.obtainMessage(13, z ? 1 : 0, this.mPowerKeyPressCounter, Long.valueOf(downTime));
                messageObtainMessage.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(messageObtainMessage, ViewConfiguration.getMultiPressTimeout());
                return;
            }
            powerPress(downTime, z, this.mPowerKeyPressCounter);
        }
        finishPowerKeyPress();
    }

    private void finishPowerKeyPress() {
        this.mBeganFromNonInteractive = false;
        this.mPowerKeyPressCounter = 0;
        if (this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.release();
        }
    }

    private void cancelPendingPowerKeyAction() {
        if (!this.mPowerKeyHandled) {
            this.mPowerKeyHandled = true;
            this.mHandler.removeMessages(14);
        }
        if (hasVeryLongPressOnPowerBehavior()) {
            this.mHandler.removeMessages(28);
        }
    }

    private void cancelPendingBackKeyAction() {
        if (!this.mBackKeyHandled) {
            this.mBackKeyHandled = true;
            this.mHandler.removeMessages(18);
        }
    }

    private void powerPress(long j, boolean z, int i) {
        if (this.mScreenOnEarly && !this.mScreenOnFully) {
            Slog.i("WindowManager", "Suppressed redundant power key press while already in the process of turning the screen on.");
        }
        Slog.d("WindowManager", "powerPress: eventTime=" + j + " interactive=" + z + " count=" + i + " beganFromNonInteractive=" + this.mBeganFromNonInteractive + " mShortPressOnPowerBehavior=" + this.mShortPressOnPowerBehavior);
        if (i == 2) {
            powerMultiPressAction(j, z, this.mDoublePressOnPowerBehavior);
            return;
        }
        if (i == 3) {
            powerMultiPressAction(j, z, this.mTriplePressOnPowerBehavior);
            return;
        }
        if (z && !this.mBeganFromNonInteractive) {
            switch (this.mShortPressOnPowerBehavior) {
                case 1:
                    goToSleep(j, 4, 0);
                    break;
                case 2:
                    goToSleep(j, 4, 1);
                    break;
                case 3:
                    goToSleep(j, 4, 1);
                    launchHomeFromHotKey();
                    break;
                case 4:
                    shortPressPowerGoHome();
                    break;
                case 5:
                    if (this.mDismissImeOnBackKeyPressed) {
                        if (this.mInputMethodManagerInternal == null) {
                            this.mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
                        }
                        if (this.mInputMethodManagerInternal != null) {
                            this.mInputMethodManagerInternal.hideCurrentInputMethod();
                        }
                    } else {
                        shortPressPowerGoHome();
                    }
                    break;
            }
        }
    }

    private void goToSleep(long j, int i, int i2) {
        this.mRequestedOrGoingToSleep = true;
        this.mPowerManager.goToSleep(j, i, i2);
    }

    private void shortPressPowerGoHome() {
        launchHomeFromHotKey(true, false);
        if (isKeyguardShowingAndNotOccluded()) {
            this.mKeyguardDelegate.onShortPowerPressedGoHome();
        }
    }

    private void powerMultiPressAction(long j, boolean z, int i) {
        switch (i) {
            case 1:
                if (!isUserSetupComplete()) {
                    Slog.i("WindowManager", "Ignoring toggling theater mode - device not setup.");
                    break;
                } else if (isTheaterModeEnabled()) {
                    Slog.i("WindowManager", "Toggling theater mode off.");
                    Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 0);
                    if (!z) {
                        wakeUpFromPowerKey(j);
                    }
                    break;
                } else {
                    Slog.i("WindowManager", "Toggling theater mode on.");
                    Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 1);
                    if (this.mGoToSleepOnButtonPressTheaterMode && z) {
                        goToSleep(j, 4, 0);
                        break;
                    }
                }
                break;
            case 2:
                Slog.i("WindowManager", "Starting brightness boost.");
                if (!z) {
                    wakeUpFromPowerKey(j);
                }
                this.mPowerManager.boostScreenBrightness(j);
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
        int resolvedLongPressOnPowerBehavior = getResolvedLongPressOnPowerBehavior();
        switch (resolvedLongPressOnPowerBehavior) {
            case 1:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                showGlobalActionsInternal();
                break;
            case 2:
            case 3:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                this.mWindowManagerFuncs.shutdown(resolvedLongPressOnPowerBehavior == 2);
                break;
            case 4:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                if (!(this.mKeyguardDelegate != null ? this.mKeyguardDelegate.isShowing() : false)) {
                    Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
                    if (this.mAllowStartActivityForLongPressOnPowerDuringSetup) {
                        this.mContext.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                    } else {
                        startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                    }
                }
                break;
        }
    }

    private void powerVeryLongPress() {
        switch (this.mVeryLongPressOnPowerBehavior) {
            case 1:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                showGlobalActionsInternal();
                break;
        }
    }

    private void backLongPress() {
        boolean zIsShowing;
        this.mBackKeyHandled = true;
        switch (this.mLongPressOnBackBehavior) {
            case 1:
                if (this.mKeyguardDelegate == null) {
                    zIsShowing = false;
                } else {
                    zIsShowing = this.mKeyguardDelegate.isShowing();
                }
                if (!zIsShowing) {
                    startActivityAsUser(new Intent("android.intent.action.VOICE_ASSIST"), UserHandle.CURRENT_OR_SELF);
                }
                break;
        }
    }

    private void accessibilityShortcutActivated() {
        this.mAccessibilityShortcutController.performAccessibilityShortcut();
    }

    private void disposeInputConsumer(WindowManagerPolicy.InputConsumer inputConsumer) {
        if (inputConsumer != null) {
            inputConsumer.dismiss();
        }
    }

    private void sleepPress() {
        if (this.mShortPressOnSleepBehavior == 1) {
            launchHomeFromHotKey(false, true);
        }
    }

    private void sleepRelease(long j) {
        switch (this.mShortPressOnSleepBehavior) {
            case 0:
            case 1:
                Slog.i("WindowManager", "sleepRelease() calling goToSleep(GO_TO_SLEEP_REASON_SLEEP_BUTTON)");
                goToSleep(j, 6, 0);
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

    private boolean hasVeryLongPressOnPowerBehavior() {
        return this.mVeryLongPressOnPowerBehavior != 0;
    }

    private boolean hasLongPressOnBackBehavior() {
        return this.mLongPressOnBackBehavior != 0;
    }

    private void interceptScreenshotChord() {
        if (this.mScreenshotChordEnabled && this.mScreenshotChordVolumeDownKeyTriggered && this.mScreenshotChordPowerKeyTriggered && !this.mA11yShortcutChordVolumeUpKeyTriggered) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (jUptimeMillis <= this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS && jUptimeMillis <= this.mScreenshotChordPowerKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mScreenshotChordVolumeDownKeyConsumed = true;
                cancelPendingPowerKeyAction();
                this.mScreenshotRunnable.setScreenshotType(1);
                this.mHandler.postDelayed(this.mScreenshotRunnable, getScreenshotChordLongPressDelay());
            }
        }
    }

    private void interceptAccessibilityShortcutChord() {
        if (this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(isKeyguardLocked()) && this.mScreenshotChordVolumeDownKeyTriggered && this.mA11yShortcutChordVolumeUpKeyTriggered && !this.mScreenshotChordPowerKeyTriggered) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (jUptimeMillis <= this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS && jUptimeMillis <= this.mA11yShortcutChordVolumeUpKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mScreenshotChordVolumeDownKeyConsumed = true;
                this.mA11yShortcutChordVolumeUpKeyConsumed = true;
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(20), getAccessibilityShortcutTimeout());
            }
        }
    }

    private void interceptRingerToggleChord() {
        if (this.mRingerToggleChord != 0 && this.mScreenshotChordPowerKeyTriggered && this.mA11yShortcutChordVolumeUpKeyTriggered) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (jUptimeMillis <= this.mA11yShortcutChordVolumeUpKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS && jUptimeMillis <= this.mScreenshotChordPowerKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mA11yShortcutChordVolumeUpKeyConsumed = true;
                cancelPendingPowerKeyAction();
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(30), getRingerToggleChordDelay());
            }
        }
    }

    private long getAccessibilityShortcutTimeout() {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(this.mContext);
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_shortcut_dialog_shown", 0, this.mCurrentUserId) == 0) {
            return viewConfiguration.getAccessibilityShortcutKeyTimeout();
        }
        return viewConfiguration.getAccessibilityShortcutKeyTimeoutAfterConfirmation();
    }

    private long getScreenshotChordLongPressDelay() {
        if (this.mKeyguardDelegate.isShowing()) {
            return (long) (KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER * ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
        }
        return ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout();
    }

    private long getRingerToggleChordDelay() {
        return ViewConfiguration.getTapTimeout();
    }

    private void cancelPendingScreenshotChordAction() {
        this.mHandler.removeCallbacks(this.mScreenshotRunnable);
    }

    private void cancelPendingAccessibilityShortcutAction() {
        this.mHandler.removeMessages(20);
    }

    private void cancelPendingRingerToggleChordAction() {
        this.mHandler.removeMessages(30);
    }

    private class ScreenshotRunnable implements Runnable {
        private int mScreenshotType;

        private ScreenshotRunnable() {
            this.mScreenshotType = 1;
        }

        public void setScreenshotType(int i) {
            this.mScreenshotType = i;
        }

        @Override
        public void run() {
            if (Settings.System.getInt(PhoneWindowManager.this.mContext.getContentResolver(), "allow_screen_shot", 1) != 1) {
                return;
            }
            PhoneWindowManager.this.mScreenshotHelper.takeScreenshot(this.mScreenshotType, PhoneWindowManager.this.mStatusBar != null && PhoneWindowManager.this.mStatusBar.isVisibleLw(), PhoneWindowManager.this.mNavigationBar != null && PhoneWindowManager.this.mNavigationBar.isVisibleLw(), PhoneWindowManager.this.mHandler);
        }
    }

    @Override
    public void showGlobalActions() {
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(10);
    }

    void showGlobalActionsInternal() {
        if (this.mGlobalActions == null) {
            this.mGlobalActions = new GlobalActions(this.mContext, this.mWindowManagerFuncs);
        }
        boolean zIsKeyguardShowingAndNotOccluded = isKeyguardShowingAndNotOccluded();
        this.mGlobalActions.showDialog(zIsKeyguardShowingAndNotOccluded, isDeviceProvisioned());
        if (zIsKeyguardShowingAndNotOccluded) {
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    boolean isUserSetupComplete() {
        boolean z = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
        if (this.mHasFeatureLeanback) {
            return z & isTvUserSetupComplete();
        }
        return z;
    }

    private boolean isTvUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "tv_user_setup_complete", 0, -2) != 0;
    }

    private void handleShortPressOnHome() {
        HdmiControl hdmiControl = getHdmiControl();
        if (hdmiControl != null) {
            hdmiControl.turnOnTv();
        }
        if (this.mDreamManagerInternal != null && this.mDreamManagerInternal.isDreaming()) {
            this.mDreamManagerInternal.stopDream(false);
        } else {
            launchHomeFromHotKey();
        }
    }

    private HdmiControl getHdmiControl() {
        HdmiPlaybackClient playbackClient;
        if (this.mHdmiControl == null) {
            if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.hdmi.cec")) {
                return null;
            }
            HdmiControlManager hdmiControlManager = (HdmiControlManager) this.mContext.getSystemService("hdmi_control");
            if (hdmiControlManager != null) {
                playbackClient = hdmiControlManager.getPlaybackClient();
            } else {
                playbackClient = null;
            }
            this.mHdmiControl = new HdmiControl(playbackClient);
        }
        return this.mHdmiControl;
    }

    private static class HdmiControl {
        private final HdmiPlaybackClient mClient;

        private HdmiControl(HdmiPlaybackClient hdmiPlaybackClient) {
            this.mClient = hdmiPlaybackClient;
        }

        public void turnOnTv() {
            if (this.mClient == null) {
                return;
            }
            this.mClient.oneTouchPlay(new HdmiPlaybackClient.OneTouchPlayCallback() {
                public void onComplete(int i) {
                    if (i != 0) {
                        Log.w("WindowManager", "One touch play failed: " + i);
                    }
                }
            });
        }
    }

    private void handleLongPressOnHome(int i) {
        if (this.mLongPressOnHomeBehavior == 0) {
        }
        this.mHomeConsumed = true;
        performHapticFeedbackLw(null, 0, false);
        switch (this.mLongPressOnHomeBehavior) {
            case 1:
                launchAllAppsAction();
                break;
            case 2:
                launchAssistAction(null, i);
                break;
            default:
                Log.w("WindowManager", "Undefined home long press behavior: " + this.mLongPressOnHomeBehavior);
                break;
        }
    }

    private void launchAllAppsAction() {
        Intent intent = new Intent("android.intent.action.ALL_APPS");
        if (this.mHasFeatureLeanback) {
            PackageManager packageManager = this.mContext.getPackageManager();
            Intent intent2 = new Intent("android.intent.action.MAIN");
            intent2.addCategory("android.intent.category.HOME");
            ResolveInfo resolveInfoResolveActivityAsUser = packageManager.resolveActivityAsUser(intent2, DumpState.DUMP_DEXOPT, this.mCurrentUserId);
            if (resolveInfoResolveActivityAsUser != null) {
                intent.setPackage(resolveInfoResolveActivityAsUser.activityInfo.packageName);
            }
        }
        startActivityAsUser(intent, UserHandle.CURRENT);
    }

    private void handleDoubleTapOnHome() {
        if (this.mDoubleTapOnHomeBehavior == 1) {
            this.mHomeConsumed = true;
            toggleRecentApps();
        }
    }

    private void showPictureInPictureMenu(KeyEvent keyEvent) {
        if (DEBUG_INPUT) {
            Log.d("WindowManager", "showPictureInPictureMenu event=" + keyEvent);
        }
        this.mHandler.removeMessages(17);
        Message messageObtainMessage = this.mHandler.obtainMessage(17);
        messageObtainMessage.setAsynchronous(true);
        messageObtainMessage.sendToTarget();
    }

    private void showPictureInPictureMenuInternal() {
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.showPictureInPictureMenu();
        }
    }

    private boolean isRoundWindow() {
        return this.mContext.getResources().getConfiguration().isScreenRound();
    }

    @Override
    public void init(Context context, IWindowManager iWindowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        int integer;
        int i;
        int i2;
        int i3;
        int i4;
        this.mContext = context;
        this.mWindowManager = iWindowManager;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mHasFeatureWatch = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        this.mHasFeatureLeanback = this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        this.mAccessibilityShortcutController = new AccessibilityShortcutController(this.mContext, new Handler(), this.mCurrentUserId);
        this.mLogger = new MetricsLogger();
        boolean z = context.getResources().getBoolean(R.^attr-private.floatingToolbarOpenDrawable);
        boolean z2 = SystemProperties.getBoolean("persist.debug.force_burn_in", false);
        if (z || z2) {
            if (!z2) {
                Resources resources = context.getResources();
                int integer2 = resources.getInteger(R.integer.config_audio_notif_vol_default);
                int integer3 = resources.getInteger(R.integer.config_attentiveTimeout);
                int integer4 = resources.getInteger(R.integer.config_audio_notif_vol_steps);
                int integer5 = resources.getInteger(R.integer.config_audio_alarm_min_vol);
                integer = resources.getInteger(R.integer.config_attentiveWarningDuration);
                i = integer2;
                i2 = integer3;
                i3 = integer4;
                i4 = integer5;
            } else {
                i = -8;
                i3 = -8;
                i2 = 8;
                i4 = -4;
                integer = isRoundWindow() ? 6 : -1;
            }
            this.mBurnInProtectionHelper = new BurnInProtectionHelper(context, i, i2, i3, i4, integer);
        }
        this.mHandler = new PolicyHandler();
        this.mWakeGestureListener = new MyWakeGestureListener(this.mContext, this.mHandler);
        this.mOrientationListener = new MyOrientationListener(this.mContext, this.mHandler);
        try {
            this.mOrientationListener.setCurrentRotation(iWindowManager.getDefaultDisplayRotation());
        } catch (RemoteException e) {
        }
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
        this.mShortcutManager = new ShortcutManager(context);
        this.mUiMode = context.getResources().getInteger(R.integer.config_bluetooth_idle_cur_ma);
        this.mHomeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mHomeIntent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270532608);
        this.mEnableCarDockHomeCapture = context.getResources().getBoolean(R.^attr-private.foregroundInsidePadding);
        this.mCarDockIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mCarDockIntent.addCategory("android.intent.category.CAR_DOCK");
        this.mCarDockIntent.addFlags(270532608);
        this.mDeskDockIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mDeskDockIntent.addCategory("android.intent.category.DESK_DOCK");
        this.mDeskDockIntent.addFlags(270532608);
        this.mVrHeadsetHomeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mVrHeadsetHomeIntent.addCategory("android.intent.category.VR_HOME");
        this.mVrHeadsetHomeIntent.addFlags(270532608);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mBroadcastWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mBroadcastWakeLock");
        this.mPowerKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mPowerKeyWakeLock");
        this.mEnableShiftMenuBugReports = "1".equals(SystemProperties.get("ro.debuggable"));
        this.mSupportAutoRotation = this.mContext.getResources().getBoolean(R.^attr-private.pageSpacing);
        this.mLidOpenRotation = readRotation(R.integer.config_datause_polling_period_sec);
        this.mCarDockRotation = readRotation(R.integer.config_autoBrightnessDarkeningLightDebounce);
        this.mDeskDockRotation = readRotation(R.integer.config_bluetooth_tx_cur_ma);
        this.mUndockedHdmiRotation = readRotation(R.integer.config_extraFreeKbytesAbsolute);
        this.mCarDockEnablesAccelerometer = this.mContext.getResources().getBoolean(R.^attr-private.colorSurface);
        this.mDeskDockEnablesAccelerometer = this.mContext.getResources().getBoolean(R.^attr-private.dialogTitleIconsDecorLayout);
        this.mLidKeyboardAccessibility = this.mContext.getResources().getInteger(R.integer.config_datagram_wait_for_connected_state_timeout_millis);
        this.mLidNavigationAccessibility = this.mContext.getResources().getInteger(R.integer.config_datause_notification_type);
        this.mLidControlsScreenLock = this.mContext.getResources().getBoolean(R.^attr-private.layout_alwaysShow);
        this.mLidControlsSleep = this.mContext.getResources().getBoolean(R.^attr-private.layout_childType);
        this.mTranslucentDecorEnabled = this.mContext.getResources().getBoolean(R.^attr-private.headerTextColor);
        this.mAllowTheaterModeWakeFromKey = this.mContext.getResources().getBoolean(R.^attr-private.allowAutoRevokePermissionsExemption);
        this.mAllowTheaterModeWakeFromPowerKey = this.mAllowTheaterModeWakeFromKey || this.mContext.getResources().getBoolean(R.^attr-private.aspect);
        this.mAllowTheaterModeWakeFromMotion = this.mContext.getResources().getBoolean(R.^attr-private.allowStacking);
        this.mAllowTheaterModeWakeFromMotionWhenNotDreaming = this.mContext.getResources().getBoolean(R.^attr-private.alwaysFocusable);
        this.mAllowTheaterModeWakeFromCameraLens = this.mContext.getResources().getBoolean(R.^attr-private.adjustable);
        this.mAllowTheaterModeWakeFromLidSwitch = this.mContext.getResources().getBoolean(R.^attr-private.allowMassStorage);
        this.mAllowTheaterModeWakeFromWakeGesture = this.mContext.getResources().getBoolean(R.^attr-private.alertDialogCenterButtons);
        this.mGoToSleepOnButtonPressTheaterMode = this.mContext.getResources().getBoolean(R.^attr-private.interpolatorX);
        this.mSupportLongPressPowerWhenNonInteractive = this.mContext.getResources().getBoolean(R.^attr-private.panelMenuListWidth);
        this.mLongPressOnBackBehavior = this.mContext.getResources().getInteger(R.integer.config_debugSystemServerPssThresholdBytes);
        this.mShortPressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_dreamCloseAnimationDuration);
        this.mLongPressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_defaultAlarmVibrationIntensity);
        this.mVeryLongPressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_faceMaxTemplatesPerUser);
        this.mDoublePressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_brightness_ramp_rate_slow);
        this.mTriplePressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_externalDisplayPeakWidth);
        this.mShortPressOnSleepBehavior = this.mContext.getResources().getInteger(R.integer.config_dreamOpenAnimationDuration);
        this.mVeryLongPressTimeout = this.mContext.getResources().getInteger(R.integer.config_fingerprintMaxTemplatesPerUser);
        this.mAllowStartActivityForLongPressOnPowerDuringSetup = this.mContext.getResources().getBoolean(R.^attr-private.activityOpenRemoteViewsEnterAnimation);
        this.mUseTvRouting = AudioSystem.getPlatformType(this.mContext) == 2;
        this.mHandleVolumeKeysInWM = this.mContext.getResources().getBoolean(R.^attr-private.interpolatorZ);
        readConfigurationDependentBehaviors();
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
        intentFilter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
        intentFilter.addAction(UiModeManager.ACTION_ENTER_DESK_MODE);
        intentFilter.addAction(UiModeManager.ACTION_EXIT_DESK_MODE);
        intentFilter.addAction("android.intent.action.DOCK_EVENT");
        Intent intentRegisterReceiver = context.registerReceiver(this.mDockReceiver, intentFilter);
        if (intentRegisterReceiver != null) {
            this.mDockMode = intentRegisterReceiver.getIntExtra("android.intent.extra.DOCK_STATE", 0);
        }
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.DREAMING_STARTED");
        intentFilter2.addAction("android.intent.action.DREAMING_STOPPED");
        context.registerReceiver(this.mDreamReceiver, intentFilter2);
        context.registerReceiver(this.mMultiuserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        this.mSystemGestures = new SystemGesturesPointerEventListener(context, new SystemGesturesPointerEventListener.Callbacks() {
            @Override
            public void onSwipeFromTop() {
                if (PhoneWindowManager.this.mStatusBar != null) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mStatusBar);
                }
            }

            @Override
            public void onSwipeFromBottom() {
                if (PhoneWindowManager.this.mForceHideNavBar) {
                    return;
                }
                if (PhoneWindowManager.this.mNavigationBar != null && PhoneWindowManager.this.mNavigationBarPosition == 4) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
                }
                if ("1".equals(SystemProperties.get("ro.config.simplelauncher"))) {
                    InputManager.getInstance().injectInputEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 0, 4, 0, 0, -1, 0, 72, UsbTerminalTypes.TERMINAL_USB_STREAMING), 0);
                    InputManager.getInstance().injectInputEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 1, 4, 0, 0, -1, 0, 72, UsbTerminalTypes.TERMINAL_USB_STREAMING), 0);
                }
            }

            @Override
            public void onSwipeFromRight() {
                if (!PhoneWindowManager.this.mForceHideNavBar && PhoneWindowManager.this.mNavigationBar != null && PhoneWindowManager.this.mNavigationBarPosition == 2) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
                }
            }

            @Override
            public void onSwipeFromLeft() {
                if (PhoneWindowManager.this.mForceHideNavBar) {
                    return;
                }
                if (PhoneWindowManager.this.mNavigationBar != null && PhoneWindowManager.this.mNavigationBarPosition == 1) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
                }
                if ("1".equals(SystemProperties.get("ro.config.simplelauncher"))) {
                    InputManager.getInstance().injectInputEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 0, 4, 0, 0, -1, 0, 72, UsbTerminalTypes.TERMINAL_USB_STREAMING), 0);
                    InputManager.getInstance().injectInputEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 1, 4, 0, 0, -1, 0, 72, UsbTerminalTypes.TERMINAL_USB_STREAMING), 0);
                }
            }

            @Override
            public void onFling(int i5) {
                if (PhoneWindowManager.this.mPowerManagerInternal != null) {
                    PhoneWindowManager.this.mPowerManagerInternal.powerHint(2, i5);
                }
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
                PhoneWindowManager.this.mHandler.removeMessages(16);
                Message messageObtainMessage = PhoneWindowManager.this.mHandler.obtainMessage(16);
                messageObtainMessage.arg1 = 0;
                PhoneWindowManager.this.mHandler.sendMessageDelayed(messageObtainMessage, 500L);
            }

            @Override
            public void onMouseHoverAtBottom() {
                PhoneWindowManager.this.mHandler.removeMessages(16);
                Message messageObtainMessage = PhoneWindowManager.this.mHandler.obtainMessage(16);
                messageObtainMessage.arg1 = 1;
                PhoneWindowManager.this.mHandler.sendMessageDelayed(messageObtainMessage, 500L);
            }

            @Override
            public void onMouseLeaveFromEdge() {
                PhoneWindowManager.this.mHandler.removeMessages(16);
            }
        });
        this.mImmersiveModeConfirmation = new ImmersiveModeConfirmation(this.mContext);
        this.mWindowManagerFuncs.registerPointerEventListener(this.mSystemGestures);
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mLongPressVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_cameraPrivacyLightColors);
        this.mCalendarDateVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_autoBrightnessLcdBacklightValues_doze);
        this.mSafeModeEnabledVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_defaultFirstUserRestrictions);
        this.mScreenshotChordEnabled = this.mContext.getResources().getBoolean(R.^attr-private.headerRemoveIconIfEmpty);
        this.mGlobalKeyManager = new GlobalKeyManager(this.mContext);
        initializeHdmiState();
        if (!this.mPowerManager.isInteractive()) {
            startedGoingToSleep(2);
            finishedGoingToSleep(2);
        }
        this.mWindowManagerInternal.registerAppTransitionListener(this.mStatusBarController.getAppTransitionListener());
        this.mWindowManagerInternal.registerAppTransitionListener(new WindowManagerInternal.AppTransitionListener() {
            @Override
            public int onAppTransitionStartingLocked(int i5, IBinder iBinder, IBinder iBinder2, long j, long j2, long j3) {
                return PhoneWindowManager.this.handleStartTransitionForKeyguardLw(i5, j);
            }

            @Override
            public void onAppTransitionCancelledLocked(int i5) {
                PhoneWindowManager.this.handleStartTransitionForKeyguardLw(i5, 0L);
            }
        });
        this.mKeyguardDelegate = new KeyguardServiceDelegate(this.mContext, new KeyguardStateMonitor.StateCallback() {
            @Override
            public void onTrustedChanged() {
                PhoneWindowManager.this.mWindowManagerFuncs.notifyKeyguardTrustedChanged();
            }

            @Override
            public void onShowingChanged() {
                PhoneWindowManager.this.mWindowManagerFuncs.onKeyguardShowingAndNotOccludedChanged();
            }
        });
        this.mScreenshotHelper = new ScreenshotHelper(this.mContext);
    }

    private void readConfigurationDependentBehaviors() {
        Resources resources = this.mContext.getResources();
        this.mLongPressOnHomeBehavior = resources.getInteger(R.integer.config_defaultActionModeHideDurationMillis);
        if (this.mLongPressOnHomeBehavior < 0 || this.mLongPressOnHomeBehavior > 2) {
            this.mLongPressOnHomeBehavior = 0;
        }
        this.mDoubleTapOnHomeBehavior = resources.getInteger(R.integer.config_burnInProtectionMaxHorizontalOffset);
        if (this.mDoubleTapOnHomeBehavior < 0 || this.mDoubleTapOnHomeBehavior > 1) {
            this.mDoubleTapOnHomeBehavior = 0;
        }
        this.mShortPressOnWindowBehavior = 0;
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.picture_in_picture")) {
            this.mShortPressOnWindowBehavior = 1;
        }
        this.mNavBarOpacityMode = resources.getInteger(R.integer.config_defaultRefreshRateInHbmSunlight);
    }

    @Override
    public void setInitialDisplaySize(Display display, int i, int i2, int i3) {
        int i4;
        int i5;
        if (this.mContext == null || display.getDisplayId() != 0) {
            return;
        }
        this.mDisplay = display;
        Resources resources = this.mContext.getResources();
        if (i > i2) {
            this.mLandscapeRotation = 0;
            this.mSeascapeRotation = 2;
            if (resources.getBoolean(R.^attr-private.maxCollapsedHeight)) {
                this.mPortraitRotation = 1;
                this.mUpsideDownRotation = 3;
            } else {
                this.mPortraitRotation = 3;
                this.mUpsideDownRotation = 1;
            }
            i5 = i;
            i4 = i2;
        } else {
            this.mPortraitRotation = 0;
            this.mUpsideDownRotation = 2;
            if (resources.getBoolean(R.^attr-private.maxCollapsedHeight)) {
                this.mLandscapeRotation = 3;
                this.mSeascapeRotation = 1;
            } else {
                this.mLandscapeRotation = 1;
                this.mSeascapeRotation = 3;
            }
            i4 = i;
            i5 = i2;
        }
        int i6 = (i4 * 160) / i3;
        int i7 = (i5 * 160) / i3;
        this.mNavigationBarCanMove = i != i2 && i6 < 600;
        this.mHasNavigationBar = resources.getBoolean(R.^attr-private.mountPoint);
        String str = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(str)) {
            this.mHasNavigationBar = false;
        } else if ("0".equals(str)) {
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
        this.mForceDefaultOrientation = ((i7 >= 960 && i6 >= 720) || this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive") || this.mContext.getPackageManager().hasSystemFeature("android.software.leanback")) && resources.getBoolean(R.^attr-private.internalMaxWidth) && !"true".equals(SystemProperties.get("config.override_forced_orient"));
    }

    private boolean canHideNavigationBar() {
        return this.mHasNavigationBar;
    }

    @Override
    public boolean isDefaultOrientationForced() {
        return this.mForceDefaultOrientation;
    }

    public void updateSettings() {
        boolean z;
        int intForUser;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        synchronized (this.mLock) {
            this.mEndcallBehavior = Settings.System.getIntForUser(contentResolver, "end_button_behavior", 2, -2);
            this.mIncallPowerBehavior = Settings.Secure.getIntForUser(contentResolver, "incall_power_button_behavior", 1, -2);
            this.mIncallBackBehavior = Settings.Secure.getIntForUser(contentResolver, "incall_back_button_behavior", 0, -2);
            this.mSystemNavigationKeysEnabled = Settings.Secure.getIntForUser(contentResolver, "system_navigation_keys_enabled", 0, -2) == 1;
            this.mRingerToggleChord = Settings.Secure.getIntForUser(contentResolver, "volume_hush_gesture", 0, -2);
            if (!this.mContext.getResources().getBoolean(R.^attr-private.position)) {
                this.mRingerToggleChord = 0;
            }
            int intForUser2 = Settings.Secure.getIntForUser(contentResolver, "show_rotation_suggestions", 1, -2);
            if (this.mShowRotationSuggestions != intForUser2) {
                this.mShowRotationSuggestions = intForUser2;
                updateOrientationListenerLp();
            }
            boolean z2 = Settings.Secure.getIntForUser(contentResolver, "wake_gesture_enabled", 0, -2) != 0;
            if (this.mWakeGestureEnabledSetting != z2) {
                this.mWakeGestureEnabledSetting = z2;
                updateWakeGestureListenerLp();
            }
            int intForUser3 = Settings.System.getIntForUser(contentResolver, "user_rotation", 0, -2);
            if (this.mUserRotation != intForUser3) {
                this.mUserRotation = intForUser3;
                z = true;
            } else {
                z = false;
            }
            int i = Settings.System.getIntForUser(contentResolver, "accelerometer_rotation", 0, -2) != 0 ? 0 : 1;
            if (this.mUserRotationMode != i) {
                this.mUserRotationMode = i;
                updateOrientationListenerLp();
                z = true;
            }
            if (this.mSystemReady && this.mPointerLocationMode != (intForUser = Settings.System.getIntForUser(contentResolver, "pointer_location", 0, -2))) {
                this.mPointerLocationMode = intForUser;
                this.mHandler.sendEmptyMessage(intForUser != 0 ? 1 : 2);
            }
            this.mLockScreenTimeout = Settings.System.getIntForUser(contentResolver, "screen_off_timeout", 0, -2);
            String stringForUser = Settings.Secure.getStringForUser(contentResolver, "default_input_method", -2);
            boolean z3 = stringForUser != null && stringForUser.length() > 0;
            if (this.mHasSoftInput != z3) {
                this.mHasSoftInput = z3;
                z = true;
            }
            if (this.mImmersiveModeConfirmation != null) {
                this.mImmersiveModeConfirmation.loadSetting(this.mCurrentUserId);
            }
            boolean z4 = Settings.System.getIntForUser(contentResolver, "hide_navigation_bar", 0, -2) != 0;
            if (this.mForceHideNavBar != z4) {
                this.mForceHideNavBar = z4;
                z = true;
            }
        }
        synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            PolicyControl.reloadFromSetting(this.mContext);
        }
        if (z) {
            updateRotation(true);
        }
    }

    private void updateWakeGestureListenerLp() {
        if (shouldEnableWakeGestureLp()) {
            this.mWakeGestureListener.requestWakeUpTrigger();
        } else {
            this.mWakeGestureListener.cancelWakeUpTrigger();
        }
    }

    private boolean shouldEnableWakeGestureLp() {
        return this.mWakeGestureEnabledSetting && !this.mAwake && !(this.mLidControlsSleep && this.mLidState == 0) && this.mWakeGestureListener.isSupported();
    }

    private void enablePointerLocation() {
        if (this.mPointerLocationView == null) {
            this.mPointerLocationView = new PointerLocationView(this.mContext);
            this.mPointerLocationView.setPrintCoords(false);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1);
            layoutParams.type = 2015;
            layoutParams.flags = 1304;
            layoutParams.layoutInDisplayCutoutMode = 1;
            if (ActivityManager.isHighEndGfx()) {
                layoutParams.flags |= DumpState.DUMP_SERVICE_PERMISSIONS;
                layoutParams.privateFlags |= 2;
            }
            layoutParams.format = -3;
            layoutParams.setTitle("PointerLocation");
            WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
            layoutParams.inputFeatures |= 2;
            windowManager.addView(this.mPointerLocationView, layoutParams);
            this.mWindowManagerFuncs.registerPointerEventListener(this.mPointerLocationView);
        }
    }

    private void disablePointerLocation() {
        if (this.mPointerLocationView != null) {
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.mPointerLocationView);
            ((WindowManager) this.mContext.getSystemService("window")).removeView(this.mPointerLocationView);
            this.mPointerLocationView = null;
        }
    }

    private int readRotation(int i) {
        try {
            int integer = this.mContext.getResources().getInteger(i);
            if (integer == 0) {
                return 0;
            }
            if (integer == 90) {
                return 1;
            }
            if (integer == 180) {
                return 2;
            }
            if (integer == 270) {
                return 3;
            }
            return -1;
        } catch (Resources.NotFoundException e) {
            return -1;
        }
    }

    @Override
    public int checkAddPermission(WindowManager.LayoutParams layoutParams, int[] iArr) {
        ApplicationInfo applicationInfoAsUser;
        int i = layoutParams.type;
        if (((layoutParams.privateFlags & DumpState.DUMP_DEXOPT) != 0) && this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0) {
            return -8;
        }
        iArr[0] = -1;
        if ((i < 1 || i > 99) && ((i < 1000 || i > 1999) && (i < 2000 || i > 2999))) {
            return -10;
        }
        if (i < 2000 || i > 2999) {
            return 0;
        }
        if (!WindowManager.LayoutParams.isSystemAlertWindowType(i)) {
            if (i == 2005) {
                iArr[0] = 45;
                return 0;
            }
            if (i != 2011 && i != 2013 && i != 2023 && i != 2035 && i != 2037) {
                switch (i) {
                    case 2030:
                    case 2031:
                    case 2032:
                        break;
                    default:
                        if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0) {
                            break;
                        }
                        break;
                }
                return -8;
            }
            return 0;
        }
        iArr[0] = 24;
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) == 1000) {
            return 0;
        }
        try {
            applicationInfoAsUser = this.mContext.getPackageManager().getApplicationInfoAsUser(layoutParams.packageName, 0, UserHandle.getUserId(callingUid));
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfoAsUser = null;
        }
        if (applicationInfoAsUser == null || (i != 2038 && applicationInfoAsUser.targetSdkVersion >= 26)) {
            return this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") == 0 ? 0 : -8;
        }
        switch (this.mAppOpsManager.noteOpNoThrow(iArr[0], callingUid, layoutParams.packageName)) {
            case 0:
            case 1:
                break;
            case 2:
                if (applicationInfoAsUser.targetSdkVersion < 23) {
                }
                break;
            default:
                if (this.mContext.checkCallingOrSelfPermission("android.permission.SYSTEM_ALERT_WINDOW") != 0) {
                    break;
                }
                break;
        }
        return -8;
    }

    @Override
    public boolean checkShowToOwnerOnly(WindowManager.LayoutParams layoutParams) {
        int i = layoutParams.type;
        if (i != 3 && i != 2014 && i != 2024 && i != 2030 && i != 2034 && i != 2037) {
            switch (i) {
                case PowerHalManager.ROTATE_BOOST_TIME:
                case 2001:
                case 2002:
                    break;
                default:
                    switch (i) {
                        case 2007:
                        case 2008:
                        case 2009:
                            break;
                        default:
                            switch (i) {
                                case 2017:
                                case 2018:
                                case 2019:
                                case 2020:
                                case 2021:
                                case 2022:
                                    break;
                                default:
                                    switch (i) {
                                        case 2026:
                                        case 2027:
                                            break;
                                        default:
                                            if ((layoutParams.privateFlags & 16) == 0) {
                                                return true;
                                            }
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
                    break;
            }
        }
        return this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0;
    }

    @Override
    public void adjustWindowParamsLw(WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams, boolean z) {
        boolean z2;
        if ((layoutParams.privateFlags & DumpState.DUMP_CHANGES) == 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        if (this.mScreenDecorWindows.contains(windowState)) {
            if (!z2) {
                this.mScreenDecorWindows.remove(windowState);
            }
        } else if (z2 && z) {
            this.mScreenDecorWindows.add(windowState);
        }
        int i = layoutParams.type;
        if (i != 2000) {
            if (i == 2013) {
                layoutParams.layoutInDisplayCutoutMode = 1;
            } else if (i == 2015) {
                layoutParams.flags |= 24;
                layoutParams.flags &= -262145;
            } else if (i != 2023) {
                if (i == 2036) {
                    layoutParams.flags |= 8;
                } else {
                    switch (i) {
                        case 2005:
                            if (layoutParams.hideTimeoutMilliseconds < 0 || layoutParams.hideTimeoutMilliseconds > 3500) {
                                layoutParams.hideTimeoutMilliseconds = 3500L;
                            }
                            layoutParams.windowAnimations = R.style.Animation.Toast;
                            layoutParams.flags |= 16;
                            break;
                    }
                }
            }
        } else if (this.mKeyguardOccluded) {
            layoutParams.flags &= -1048577;
            layoutParams.privateFlags &= -1025;
        }
        if (layoutParams.type != 2000) {
            layoutParams.privateFlags &= -1025;
        }
    }

    private int getImpliedSysUiFlagsForLayout(WindowManager.LayoutParams layoutParams) {
        int i;
        if ((layoutParams.flags & Integer.MIN_VALUE) != 0) {
            i = 512;
        } else {
            i = 0;
        }
        boolean z = (layoutParams.privateFlags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0;
        if ((Integer.MIN_VALUE & layoutParams.flags) != 0 || (z && layoutParams.height == -1 && layoutParams.width == -1)) {
            return i | 1024;
        }
        return i;
    }

    void readLidState() {
        this.mLidState = this.mWindowManagerFuncs.getLidState();
    }

    private void readCameraLensCoverState() {
        this.mCameraLensCoverState = this.mWindowManagerFuncs.getCameraLensCoverState();
    }

    private boolean isHidden(int i) {
        switch (i) {
            case 1:
                if (this.mLidState != 0) {
                    break;
                }
                break;
            case 2:
                if (this.mLidState != 1) {
                    break;
                }
                break;
        }
        return false;
    }

    @Override
    public void adjustConfigurationLw(Configuration configuration, int i, int i2) {
        this.mHaveBuiltInKeyboard = (i & 1) != 0;
        readConfigurationDependentBehaviors();
        readLidState();
        if (configuration.keyboard == 1 || (i == 1 && isHidden(this.mLidKeyboardAccessibility))) {
            configuration.hardKeyboardHidden = 2;
            if (!this.mHasSoftInput) {
                configuration.keyboardHidden = 2;
            }
        }
        if (configuration.navigation == 1 || (i2 == 1 && isHidden(this.mLidNavigationAccessibility))) {
            configuration.navigationHidden = 2;
        }
    }

    @Override
    public void onOverlayChangedLw() {
        onConfigurationChanged();
    }

    @Override
    public void onConfigurationChanged() {
        Resources resources = getSystemUiContext().getResources();
        int[] iArr = this.mStatusBarHeightForRotation;
        int i = this.mPortraitRotation;
        int[] iArr2 = this.mStatusBarHeightForRotation;
        int i2 = this.mUpsideDownRotation;
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.handwriting_bounds_offset_bottom);
        iArr2[i2] = dimensionPixelSize;
        iArr[i] = dimensionPixelSize;
        int[] iArr3 = this.mStatusBarHeightForRotation;
        int i3 = this.mLandscapeRotation;
        int[] iArr4 = this.mStatusBarHeightForRotation;
        int i4 = this.mSeascapeRotation;
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.glowpadview_target_placement_radius);
        iArr4[i4] = dimensionPixelSize2;
        iArr3[i3] = dimensionPixelSize2;
        int[] iArr5 = this.mNavigationBarHeightForRotationDefault;
        int i5 = this.mPortraitRotation;
        int[] iArr6 = this.mNavigationBarHeightForRotationDefault;
        int i6 = this.mUpsideDownRotation;
        int dimensionPixelSize3 = resources.getDimensionPixelSize(R.dimen.config_viewMaxRotaryEncoderFlingVelocity);
        iArr6[i6] = dimensionPixelSize3;
        iArr5[i5] = dimensionPixelSize3;
        int[] iArr7 = this.mNavigationBarHeightForRotationDefault;
        int i7 = this.mLandscapeRotation;
        int[] iArr8 = this.mNavigationBarHeightForRotationDefault;
        int i8 = this.mSeascapeRotation;
        int dimensionPixelSize4 = resources.getDimensionPixelSize(R.dimen.config_viewMinRotaryEncoderFlingVelocity);
        iArr8[i8] = dimensionPixelSize4;
        iArr7[i7] = dimensionPixelSize4;
        int[] iArr9 = this.mNavigationBarWidthForRotationDefault;
        int i9 = this.mPortraitRotation;
        int[] iArr10 = this.mNavigationBarWidthForRotationDefault;
        int i10 = this.mUpsideDownRotation;
        int[] iArr11 = this.mNavigationBarWidthForRotationDefault;
        int i11 = this.mLandscapeRotation;
        int[] iArr12 = this.mNavigationBarWidthForRotationDefault;
        int i12 = this.mSeascapeRotation;
        int dimensionPixelSize5 = resources.getDimensionPixelSize(R.dimen.config_wallpaperMinScale);
        iArr12[i12] = dimensionPixelSize5;
        iArr11[i11] = dimensionPixelSize5;
        iArr10[i10] = dimensionPixelSize5;
        iArr9[i9] = dimensionPixelSize5;
    }

    @VisibleForTesting
    Context getSystemUiContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    @Override
    public int getMaxWallpaperLayer() {
        return getWindowLayerFromTypeLw(PowerHalManager.ROTATE_BOOST_TIME);
    }

    private int getNavigationBarWidth(int i, int i2) {
        return this.mNavigationBarWidthForRotationDefault[i];
    }

    @Override
    public int getNonDecorDisplayWidth(int i, int i2, int i3, int i4, int i5, DisplayCutout displayCutout) {
        if (i5 == 0 && this.mHasNavigationBar && this.mNavigationBarCanMove && i > i2) {
            i -= getNavigationBarWidth(i3, i4);
        }
        if (displayCutout != null) {
            return i - (displayCutout.getSafeInsetLeft() + displayCutout.getSafeInsetRight());
        }
        return i;
    }

    private int getNavigationBarHeight(int i, int i2) {
        return this.mNavigationBarHeightForRotationDefault[i];
    }

    @Override
    public int getNonDecorDisplayHeight(int i, int i2, int i3, int i4, int i5, DisplayCutout displayCutout) {
        if (i5 == 0 && this.mHasNavigationBar && (!this.mNavigationBarCanMove || i < i2)) {
            i2 -= getNavigationBarHeight(i3, i4);
        }
        if (displayCutout != null) {
            return i2 - (displayCutout.getSafeInsetTop() + displayCutout.getSafeInsetBottom());
        }
        return i2;
    }

    @Override
    public int getConfigDisplayWidth(int i, int i2, int i3, int i4, int i5, DisplayCutout displayCutout) {
        return getNonDecorDisplayWidth(i, i2, i3, i4, i5, displayCutout);
    }

    @Override
    public int getConfigDisplayHeight(int i, int i2, int i3, int i4, int i5, DisplayCutout displayCutout) {
        if (i5 == 0) {
            int iMax = this.mStatusBarHeightForRotation[i3];
            if (displayCutout != null) {
                iMax = Math.max(0, iMax - displayCutout.getSafeInsetTop());
            }
            return getNonDecorDisplayHeight(i, i2, i3, i4, i5, displayCutout) - iMax;
        }
        return i2;
    }

    @Override
    public boolean isKeyguardHostWindow(WindowManager.LayoutParams layoutParams) {
        return layoutParams.type == 2000;
    }

    @Override
    public boolean canBeHiddenByKeyguardLw(WindowManagerPolicy.WindowState windowState) {
        int i = windowState.getAttrs().type;
        return (i == 2000 || i == 2013 || i == 2019 || i == 2023 || getWindowLayerLw(windowState) >= getWindowLayerFromTypeLw(PowerHalManager.ROTATE_BOOST_TIME)) ? false : true;
    }

    private boolean shouldBeHiddenByKeyguard(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2) {
        if (windowState.getAppToken() != null) {
            return false;
        }
        WindowManager.LayoutParams attrs = windowState.getAttrs();
        boolean z = (windowState.isInputMethodWindow() || windowState2 == this) && (windowState2 != null && windowState2.isVisibleLw() && ((windowState2.getAttrs().flags & DumpState.DUMP_FROZEN) != 0 || !canBeHiddenByKeyguardLw(windowState2)));
        if (isKeyguardLocked() && isKeyguardOccluded()) {
            z |= ((524288 & attrs.flags) == 0 && (attrs.privateFlags & 256) == 0) ? false : true;
        }
        return (isKeyguardLocked() && !z && windowState.getDisplayId() == 0) || (attrs.type == 2034 && !this.mWindowManagerInternal.isStackVisible(3)) || (windowState.isInputMethodWindow() && (this.mAodShowing || !this.mWindowManagerDrawComplete));
    }

    @Override
    public WindowManagerPolicy.StartingSurface addSplashScreen(IBinder iBinder, String str, int i, CompatibilityInfo compatibilityInfo, CharSequence charSequence, int i2, int i3, int i4, int i5, Configuration configuration, int i6) throws Throwable {
        View decorView;
        ?? r1;
        int i7;
        ?? r12 = this;
        ?? r8 = 0;
        r8 = 0;
        if (!SHOW_SPLASH_SCREENS) {
            return null;
        }
        try {
            if (str == null) {
                return null;
            }
            try {
                Context context = r12.mContext;
                if (DEBUG_SPLASH_SCREEN) {
                    Slog.d("WindowManager", "addSplashScreen " + str + ": nonLocalizedLabel=" + ((Object) charSequence) + " theme=" + Integer.toHexString(i));
                }
                Context displayContext = r12.getDisplayContext(context, i6);
                if (displayContext == null) {
                    return null;
                }
                if (i != displayContext.getThemeResId() || i2 != 0) {
                    try {
                        Context contextCreatePackageContext = displayContext.createPackageContext(str, 4);
                        try {
                            contextCreatePackageContext.setTheme(i);
                            displayContext = contextCreatePackageContext;
                        } catch (PackageManager.NameNotFoundException e) {
                            displayContext = contextCreatePackageContext;
                        }
                    } catch (PackageManager.NameNotFoundException e2) {
                    }
                }
                if (configuration != null && !configuration.equals(Configuration.EMPTY)) {
                    if (DEBUG_SPLASH_SCREEN) {
                        Slog.d("WindowManager", "addSplashScreen: creating context based on overrideConfig" + configuration + " for splash screen");
                    }
                    Context contextCreateConfigurationContext = displayContext.createConfigurationContext(configuration);
                    contextCreateConfigurationContext.setTheme(i);
                    TypedArray typedArrayObtainStyledAttributes = contextCreateConfigurationContext.obtainStyledAttributes(com.android.internal.R.styleable.Window);
                    int resourceId = typedArrayObtainStyledAttributes.getResourceId(1, 0);
                    if (resourceId != 0 && contextCreateConfigurationContext.getDrawable(resourceId) != null) {
                        if (DEBUG_SPLASH_SCREEN) {
                            Slog.d("WindowManager", "addSplashScreen: apply overrideConfig" + configuration + " to starting window resId=" + resourceId);
                        }
                        displayContext = contextCreateConfigurationContext;
                    }
                    typedArrayObtainStyledAttributes.recycle();
                }
                PhoneWindow phoneWindow = new PhoneWindow(displayContext);
                phoneWindow.setIsStartingWindow(true);
                CharSequence text = displayContext.getResources().getText(i2, null);
                if (text != null) {
                    phoneWindow.setTitle(text, true);
                } else {
                    phoneWindow.setTitle(charSequence, false);
                }
                phoneWindow.setType(3);
                synchronized (r12.mWindowManagerFuncs.getWindowManagerLock()) {
                    i7 = r12.mKeyguardOccluded ? i5 | DumpState.DUMP_FROZEN : i5;
                }
                int i8 = i7 | 16 | 8 | DumpState.DUMP_INTENT_FILTER_VERIFIERS;
                phoneWindow.setFlags(i8, i8);
                phoneWindow.setDefaultIcon(i3);
                phoneWindow.setDefaultLogo(i4);
                phoneWindow.setLayout(-1, -1);
                WindowManager.LayoutParams attributes = phoneWindow.getAttributes();
                attributes.token = iBinder;
                attributes.packageName = str;
                attributes.windowAnimations = phoneWindow.getWindowStyle().getResourceId(8, 0);
                attributes.privateFlags |= 1;
                attributes.privateFlags |= 16;
                if (!compatibilityInfo.supportsScreen()) {
                    attributes.privateFlags |= 128;
                }
                attributes.setTitle("Splash Screen " + str);
                r12.addSplashscreenContent(phoneWindow, displayContext);
                r12 = (WindowManager) displayContext.getSystemService("window");
                try {
                    decorView = phoneWindow.getDecorView();
                    try {
                        if (DEBUG_SPLASH_SCREEN) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Adding splash screen window for ");
                            sb.append(str);
                            sb.append(" / ");
                            sb.append(iBinder);
                            sb.append(": ");
                            sb.append(decorView.getParent() != null ? decorView : null);
                            Slog.d("WindowManager", sb.toString());
                        }
                        r12.addView(decorView, attributes);
                        SplashScreenSurface splashScreenSurface = decorView.getParent() != null ? new SplashScreenSurface(decorView, iBinder) : null;
                        if (decorView != null && decorView.getParent() == null) {
                            Log.w("WindowManager", "view not successfully added to wm, removing view");
                            r12.removeViewImmediate(decorView);
                        }
                        return splashScreenSurface;
                    } catch (WindowManager.BadTokenException e3) {
                        e = e3;
                    } catch (RuntimeException e4) {
                        e = e4;
                        Log.w("WindowManager", iBinder + " failed creating starting window", e);
                        if (decorView != null) {
                            r1 = r12;
                            if (decorView.getParent() == null) {
                            }
                        }
                        return null;
                    }
                } catch (WindowManager.BadTokenException e5) {
                    e = e5;
                    decorView = null;
                } catch (RuntimeException e6) {
                    e = e6;
                    decorView = null;
                } catch (Throwable th) {
                    th = th;
                    if (r8 != 0 && r8.getParent() == null) {
                        Log.w("WindowManager", "view not successfully added to wm, removing view");
                        r12.removeViewImmediate(r8);
                    }
                    throw th;
                }
            } catch (WindowManager.BadTokenException e7) {
                e = e7;
                r12 = 0;
                decorView = null;
            } catch (RuntimeException e8) {
                e = e8;
                r12 = 0;
                decorView = null;
            } catch (Throwable th2) {
                th = th2;
                r12 = 0;
            }
            Log.w("WindowManager", iBinder + " already running, starting window not displayed. " + e.getMessage());
            if (decorView != null) {
                r1 = r12;
                if (decorView.getParent() == null) {
                    Log.w("WindowManager", "view not successfully added to wm, removing view");
                    r1.removeViewImmediate(decorView);
                }
            }
            return null;
        } catch (Throwable th3) {
            th = th3;
            r8 = i;
        }
    }

    private void addSplashscreenContent(PhoneWindow phoneWindow, Context context) {
        Drawable drawable;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(com.android.internal.R.styleable.Window);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(48, 0);
        typedArrayObtainStyledAttributes.recycle();
        if (resourceId == 0 || (drawable = context.getDrawable(resourceId)) == null) {
            return;
        }
        View view = new View(context);
        view.setBackground(drawable);
        phoneWindow.setContentView(view);
    }

    private Context getDisplayContext(Context context, int i) {
        if (i == 0) {
            return context;
        }
        Display display = ((DisplayManager) context.getSystemService("display")).getDisplay(i);
        if (display == null) {
            return null;
        }
        return context.createDisplayContext(display);
    }

    @Override
    public int prepareAddWindowLw(WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams) {
        if ((layoutParams.privateFlags & DumpState.DUMP_CHANGES) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
            this.mScreenDecorWindows.add(windowState);
        }
        int i = layoutParams.type;
        if (i == 2000) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
            if (this.mStatusBar != null && this.mStatusBar.isAlive()) {
                return -7;
            }
            this.mStatusBar = windowState;
            this.mStatusBarController.setWindow(windowState);
            setKeyguardOccludedLw(this.mKeyguardOccluded, true);
            return 0;
        }
        if (i != 2014 && i != 2017) {
            if (i == 2019) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
                if (this.mNavigationBar != null && this.mNavigationBar.isAlive()) {
                    return -7;
                }
                this.mNavigationBar = windowState;
                this.mNavigationBarController.setWindow(windowState);
                this.mNavigationBarController.setOnBarVisibilityChangedListener(this.mNavBarVisibilityListener, true);
                if (DEBUG_LAYOUT) {
                    Slog.i("WindowManager", "NAVIGATION BAR: " + this.mNavigationBar);
                    return 0;
                }
                return 0;
            }
            if (i != 2024 && i != 2033) {
                return 0;
            }
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
        return 0;
    }

    @Override
    public void removeWindowLw(WindowManagerPolicy.WindowState windowState) {
        if (this.mStatusBar == windowState) {
            this.mStatusBar = null;
            this.mStatusBarController.setWindow(null);
        } else if (this.mNavigationBar == windowState) {
            this.mNavigationBar = null;
            this.mNavigationBarController.setWindow(null);
        }
        this.mScreenDecorWindows.remove(windowState);
    }

    @Override
    public int selectAnimationLw(WindowManagerPolicy.WindowState windowState, int i) {
        if (windowState == this.mStatusBar) {
            boolean z = (windowState.getAttrs().privateFlags & 1024) != 0;
            boolean z2 = windowState.getAttrs().height == -1 && windowState.getAttrs().width == -1;
            if (z || z2) {
                return -1;
            }
            if (i == 2 || i == 4) {
                return R.anim.ft_avd_toarrow_rectangle_1_pivot_animation;
            }
            if (i == 1 || i == 3) {
                return R.anim.ft_avd_toarrow_rectangle_1_pivot_0_animation;
            }
        } else if (windowState == this.mNavigationBar) {
            if (windowState.getAttrs().windowAnimations != 0) {
                return 0;
            }
            if (this.mNavigationBarPosition == 4) {
                if (i == 2 || i == 4) {
                    if (isKeyguardShowingAndNotOccluded()) {
                        return R.anim.dream_activity_open_exit;
                    }
                    return R.anim.dream_activity_open_enter;
                }
                if (i == 1 || i == 3) {
                    return R.anim.dream_activity_close_exit;
                }
            } else if (this.mNavigationBarPosition == 2) {
                if (i == 2 || i == 4) {
                    return R.anim.ft_avd_toarrow_rectangle_1_animation;
                }
                if (i == 1 || i == 3) {
                    return R.anim.flat_button_state_list_anim_material;
                }
            } else if (this.mNavigationBarPosition == 1) {
                if (i == 2 || i == 4) {
                    return R.anim.fast_fade_out;
                }
                if (i == 1 || i == 3) {
                    return R.anim.fast_fade_in;
                }
            }
        } else if (windowState.getAttrs().type == 2034) {
            return selectDockedDividerAnimationLw(windowState, i);
        }
        if (i == 5) {
            if (windowState.hasAppShownWindows()) {
                return R.anim.app_starting_exit;
            }
        } else if (windowState.getAttrs().type == 2023 && this.mDreamingLockscreen && i == 1) {
            return -1;
        }
        return 0;
    }

    private int selectDockedDividerAnimationLw(WindowManagerPolicy.WindowState windowState, int i) {
        int dockedDividerInsetsLw = this.mWindowManagerFuncs.getDockedDividerInsetsLw();
        Rect frameLw = windowState.getFrameLw();
        boolean z = this.mNavigationBar != null && ((this.mNavigationBarPosition == 4 && frameLw.top + dockedDividerInsetsLw >= this.mNavigationBar.getFrameLw().top) || ((this.mNavigationBarPosition == 2 && frameLw.left + dockedDividerInsetsLw >= this.mNavigationBar.getFrameLw().left) || (this.mNavigationBarPosition == 1 && frameLw.right - dockedDividerInsetsLw <= this.mNavigationBar.getFrameLw().right)));
        boolean z2 = frameLw.height() > frameLw.width();
        boolean z3 = (z2 && (frameLw.right - dockedDividerInsetsLw <= 0 || frameLw.left + dockedDividerInsetsLw >= windowState.getDisplayFrameLw().right)) || (!z2 && (frameLw.top - dockedDividerInsetsLw <= 0 || frameLw.bottom + dockedDividerInsetsLw >= windowState.getDisplayFrameLw().bottom));
        if (z || z3) {
            return 0;
        }
        if (i == 1 || i == 3) {
            return R.anim.fade_in;
        }
        if (i == 2) {
            return R.anim.fade_out;
        }
        return 0;
    }

    @Override
    public void selectRotationAnimationLw(int[] iArr) {
        if ((this.mScreenOnFully && okToAnimate()) ? false : true) {
            iArr[0] = 17432689;
            iArr[1] = 17432688;
            return;
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            int rotationAnimationHint = this.mTopFullscreenOpaqueWindowState.getRotationAnimationHint();
            if (rotationAnimationHint < 0 && this.mTopIsFullscreen) {
                rotationAnimationHint = this.mTopFullscreenOpaqueWindowState.getAttrs().rotationAnimation;
            }
            switch (rotationAnimationHint) {
                case 1:
                case 3:
                    iArr[0] = 17432690;
                    iArr[1] = 17432688;
                    break;
                case 2:
                    iArr[0] = 17432689;
                    iArr[1] = 17432688;
                    break;
                default:
                    iArr[1] = 0;
                    iArr[0] = 0;
                    break;
            }
            return;
        }
        iArr[1] = 0;
        iArr[0] = 0;
    }

    @Override
    public boolean validateRotationAnimationLw(int i, int i2, boolean z) {
        switch (i) {
            case R.anim.overlay_task_fragment_open_from_left:
            case R.anim.overlay_task_fragment_open_from_right:
                if (z) {
                    return false;
                }
                int[] iArr = new int[2];
                selectRotationAnimationLw(iArr);
                if (i == iArr[0] && i2 == iArr[1]) {
                    return true;
                }
                return false;
            default:
                return true;
        }
    }

    @Override
    public Animation createHiddenByKeyguardExit(boolean z, boolean z2) {
        int i;
        if (z2) {
            return AnimationUtils.loadAnimation(this.mContext, R.anim.ic_signal_wifi_transient_animation_8);
        }
        Context context = this.mContext;
        if (z) {
            i = R.anim.input_method_enter;
        } else {
            i = R.anim.ic_signal_wifi_transient_animation_7;
        }
        AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(context, i);
        List<Animation> animations = animationSet.getAnimations();
        for (int size = animations.size() - 1; size >= 0; size--) {
            animations.get(size).setInterpolator(this.mLogDecelerateInterpolator);
        }
        return animationSet;
    }

    @Override
    public Animation createKeyguardWallpaperExit(boolean z) {
        if (z) {
            return null;
        }
        return AnimationUtils.loadAnimation(this.mContext, R.anim.input_method_extract_exit);
    }

    private static void awakenDreams() {
        IDreamManager dreamManager = getDreamManager();
        if (dreamManager != null) {
            try {
                dreamManager.awaken();
            } catch (RemoteException e) {
            }
        }
    }

    static IDreamManager getDreamManager() {
        return IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
    }

    TelecomManager getTelecommService() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    static IAudioService getAudioService() {
        IAudioService iAudioServiceAsInterface = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
        if (iAudioServiceAsInterface == null) {
            Log.w("WindowManager", "Unable to find IAudioService interface.");
        }
        return iAudioServiceAsInterface;
    }

    boolean keyguardOn() {
        return isKeyguardShowingAndNotOccluded() || inKeyguardRestrictedKeyInputMode();
    }

    @Override
    public long interceptKeyBeforeDispatching(WindowManagerPolicy.WindowState windowState, KeyEvent keyEvent, int i) {
        int i2;
        InputDevice device;
        IStatusBarService statusBarService;
        boolean z;
        String str;
        Intent intent;
        boolean zKeyguardOn = keyguardOn();
        int keyCode = keyEvent.getKeyCode();
        int repeatCount = keyEvent.getRepeatCount();
        int metaState = keyEvent.getMetaState();
        int flags = keyEvent.getFlags();
        boolean z2 = keyEvent.getAction() == 0;
        boolean zIsCanceled = keyEvent.isCanceled();
        if (!DEBUG_INPUT) {
            WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
            if (WindowManagerDebugger.WMS_DEBUG_ENG) {
                Log.d("WindowManager", "interceptKeyTi keyCode=" + keyCode + " down=" + z2 + " repeatCount=" + repeatCount + " keyguardOn=" + zKeyguardOn + " mHomePressed=" + this.mHomePressed + " canceled=" + zIsCanceled + " metaState:" + metaState);
            }
        }
        if (this.mScreenshotChordEnabled && (flags & 1024) == 0) {
            if (this.mScreenshotChordVolumeDownKeyTriggered && !this.mScreenshotChordPowerKeyTriggered) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                long j = this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (jUptimeMillis < j) {
                    return j - jUptimeMillis;
                }
            }
            if (keyCode == 25 && this.mScreenshotChordVolumeDownKeyConsumed) {
                if (!z2) {
                    this.mScreenshotChordVolumeDownKeyConsumed = false;
                }
                return -1L;
            }
        }
        if (this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(false) && (flags & 1024) == 0) {
            if (this.mScreenshotChordVolumeDownKeyTriggered ^ this.mA11yShortcutChordVolumeUpKeyTriggered) {
                long jUptimeMillis2 = SystemClock.uptimeMillis();
                long j2 = (this.mScreenshotChordVolumeDownKeyTriggered ? this.mScreenshotChordVolumeDownKeyTime : this.mA11yShortcutChordVolumeUpKeyTime) + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (jUptimeMillis2 < j2) {
                    return j2 - jUptimeMillis2;
                }
            }
            if (keyCode == 25 && this.mScreenshotChordVolumeDownKeyConsumed) {
                if (!z2) {
                    this.mScreenshotChordVolumeDownKeyConsumed = false;
                }
                return -1L;
            }
            if (keyCode == 24 && this.mA11yShortcutChordVolumeUpKeyConsumed) {
                if (!z2) {
                    this.mA11yShortcutChordVolumeUpKeyConsumed = false;
                }
                return -1L;
            }
        }
        if (this.mRingerToggleChord != 0 && (flags & 1024) == 0) {
            if (this.mA11yShortcutChordVolumeUpKeyTriggered && !this.mScreenshotChordPowerKeyTriggered) {
                long jUptimeMillis3 = SystemClock.uptimeMillis();
                long j3 = this.mA11yShortcutChordVolumeUpKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (jUptimeMillis3 < j3) {
                    return j3 - jUptimeMillis3;
                }
            }
            if (keyCode == 24 && this.mA11yShortcutChordVolumeUpKeyConsumed) {
                if (!z2) {
                    this.mA11yShortcutChordVolumeUpKeyConsumed = false;
                    return -1L;
                }
                return -1L;
            }
        }
        if (this.mPendingMetaAction && !KeyEvent.isMetaKey(keyCode)) {
            this.mPendingMetaAction = false;
        }
        if (this.mPendingCapsLockToggle && !KeyEvent.isMetaKey(keyCode) && !KeyEvent.isAltKey(keyCode)) {
            this.mPendingCapsLockToggle = false;
        }
        if (keyCode == 3) {
            if (!z2) {
                cancelPreloadRecentApps();
                this.mHomePressed = false;
                if (this.mHomeConsumed) {
                    this.mHomeConsumed = false;
                    return -1L;
                }
                if (zIsCanceled) {
                    Log.i("WindowManager", "Ignoring HOME; event canceled.");
                    return -1L;
                }
                if (this.mDoubleTapOnHomeBehavior != 0) {
                    this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                    this.mHomeDoubleTapPending = true;
                    this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, ViewConfiguration.getDoubleTapTimeout());
                    return -1L;
                }
                handleShortPressOnHome();
                return -1L;
            }
            WindowManager.LayoutParams attrs = windowState != null ? windowState.getAttrs() : null;
            if (attrs != null) {
                int i3 = attrs.type;
                if (i3 == 2009 || (attrs.privateFlags & 1024) != 0) {
                    return 0L;
                }
                int length = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
                for (int i4 = 0; i4 < length; i4++) {
                    if (i3 == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i4]) {
                        return -1L;
                    }
                }
            }
            if (repeatCount == 0) {
                this.mHomePressed = true;
                if (this.mHomeDoubleTapPending) {
                    this.mHomeDoubleTapPending = false;
                    this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                    handleDoubleTapOnHome();
                    return -1L;
                }
                if (this.mDoubleTapOnHomeBehavior == 1) {
                    preloadRecentApps();
                    return -1L;
                }
                return -1L;
            }
            if ((keyEvent.getFlags() & 128) != 0 && !zKeyguardOn) {
                handleLongPressOnHome(keyEvent.getDeviceId());
                return -1L;
            }
            return -1L;
        }
        if (keyCode == 82) {
            if (z2 && repeatCount == 0 && this.mEnableShiftMenuBugReports && (metaState & 1) == 1) {
                this.mContext.sendOrderedBroadcastAsUser(new Intent("android.intent.action.BUG_REPORT"), UserHandle.CURRENT, null, null, null, 0, null, null);
                return -1L;
            }
        } else {
            if (keyCode == 84) {
                if (z2) {
                    if (repeatCount == 0) {
                        this.mSearchKeyShortcutPending = true;
                        this.mConsumeSearchKeyUp = false;
                    }
                } else {
                    this.mSearchKeyShortcutPending = false;
                    if (this.mConsumeSearchKeyUp) {
                        this.mConsumeSearchKeyUp = false;
                        return -1L;
                    }
                }
                return 0L;
            }
            if (keyCode == 187) {
                if (!zKeyguardOn) {
                    if (z2 && repeatCount == 0) {
                        preloadRecentApps();
                        return -1L;
                    }
                    if (!z2) {
                        toggleRecentApps();
                        return -1L;
                    }
                    return -1L;
                }
                return -1L;
            }
            if (keyCode == 42 && keyEvent.isMetaPressed()) {
                if (z2 && (statusBarService = getStatusBarService()) != null) {
                    try {
                        statusBarService.expandNotificationsPanel();
                    } catch (RemoteException e) {
                    }
                }
            } else if (keyCode == 47 && keyEvent.isMetaPressed() && keyEvent.isCtrlPressed()) {
                if (z2 && repeatCount == 0) {
                    this.mScreenshotRunnable.setScreenshotType(keyEvent.isShiftPressed() ? 2 : 1);
                    this.mHandler.post(this.mScreenshotRunnable);
                    return -1L;
                }
            } else if (keyCode == 76 && keyEvent.isMetaPressed()) {
                if (z2 && repeatCount == 0 && !isKeyguardLocked()) {
                    toggleKeyboardShortcutsMenu(keyEvent.getDeviceId());
                }
            } else {
                if (keyCode == 219) {
                    Slog.wtf("WindowManager", "KEYCODE_ASSIST should be handled in interceptKeyBeforeQueueing");
                    return -1L;
                }
                if (keyCode == 231) {
                    Slog.wtf("WindowManager", "KEYCODE_VOICE_ASSIST should be handled in interceptKeyBeforeQueueing");
                    return -1L;
                }
                if (keyCode == 120) {
                    if (z2 && repeatCount == 0) {
                        this.mScreenshotRunnable.setScreenshotType(1);
                        this.mHandler.post(this.mScreenshotRunnable);
                        return -1L;
                    }
                    return -1L;
                }
                if (keyCode == 221 || keyCode == 220) {
                    if (z2) {
                        if (keyCode != 221) {
                            i2 = -1;
                        } else {
                            i2 = 1;
                        }
                        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3) != 0) {
                            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                        }
                        int minimumScreenBrightnessSetting = this.mPowerManager.getMinimumScreenBrightnessSetting();
                        int maximumScreenBrightnessSetting = this.mPowerManager.getMaximumScreenBrightnessSetting();
                        Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", Math.max(minimumScreenBrightnessSetting, Math.min(maximumScreenBrightnessSetting, Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mPowerManager.getDefaultScreenBrightnessSetting(), -3) + (((((maximumScreenBrightnessSetting - minimumScreenBrightnessSetting) + 10) - 1) / 10) * i2))), -3);
                        startActivityAsUser(new Intent("com.android.intent.action.SHOW_BRIGHTNESS_DIALOG"), UserHandle.CURRENT_OR_SELF);
                        return -1L;
                    }
                    return -1L;
                }
                if (keyCode == 24 || keyCode == 25 || keyCode == 164) {
                    if (this.mUseTvRouting || this.mHandleVolumeKeysInWM) {
                        dispatchDirectAudioEvent(keyEvent);
                        return -1L;
                    }
                    if (this.mPersistentVrModeEnabled && (device = keyEvent.getDevice()) != null && !device.isExternal()) {
                        return -1L;
                    }
                } else {
                    if (keyCode == 61 && keyEvent.isMetaPressed()) {
                        return 0L;
                    }
                    if (this.mHasFeatureLeanback && interceptBugreportGestureTv(keyCode, z2)) {
                        return -1L;
                    }
                    if (this.mHasFeatureLeanback && interceptAccessibilityGestureTv(keyCode, z2)) {
                        return -1L;
                    }
                    if (keyCode == 284) {
                        if (!z2) {
                            this.mHandler.removeMessages(25);
                            Message messageObtainMessage = this.mHandler.obtainMessage(25);
                            messageObtainMessage.setAsynchronous(true);
                            messageObtainMessage.sendToTarget();
                            return -1L;
                        }
                        return -1L;
                    }
                }
            }
        }
        if (KeyEvent.isModifierKey(keyCode)) {
            if (!this.mPendingCapsLockToggle) {
                this.mInitialMetaState = this.mMetaState;
                this.mPendingCapsLockToggle = true;
            } else if (keyEvent.getAction() == 1) {
                int i5 = this.mMetaState & 50;
                int i6 = this.mMetaState & 458752;
                if (i6 == 0 || i5 == 0) {
                    z = false;
                    this.mPendingCapsLockToggle = false;
                } else {
                    if (this.mInitialMetaState == ((i5 | i6) ^ this.mMetaState)) {
                        this.mInputManagerInternal.toggleCapsLock(keyEvent.getDeviceId());
                        z = true;
                    }
                    this.mPendingCapsLockToggle = false;
                }
            }
            z = false;
        } else {
            z = false;
        }
        this.mMetaState = metaState;
        if (z) {
            return -1L;
        }
        if (KeyEvent.isMetaKey(keyCode)) {
            if (z2) {
                this.mPendingMetaAction = true;
                return -1L;
            }
            if (this.mPendingMetaAction) {
                launchAssistAction("android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD", keyEvent.getDeviceId());
                return -1L;
            }
            return -1L;
        }
        if (this.mSearchKeyShortcutPending) {
            KeyCharacterMap keyCharacterMap = keyEvent.getKeyCharacterMap();
            if (keyCharacterMap.isPrintingKey(keyCode)) {
                this.mConsumeSearchKeyUp = true;
                this.mSearchKeyShortcutPending = false;
                if (z2 && repeatCount == 0 && !zKeyguardOn) {
                    Intent intent2 = this.mShortcutManager.getIntent(keyCharacterMap, keyCode, metaState);
                    if (intent2 != null) {
                        intent2.addFlags(268435456);
                        try {
                            startActivityAsUser(intent2, UserHandle.CURRENT);
                            dismissKeyboardShortcutsMenu();
                            return -1L;
                        } catch (ActivityNotFoundException e2) {
                            Slog.w("WindowManager", "Dropping shortcut key combination because the activity to which it is registered was not found: SEARCH+" + KeyEvent.keyCodeToString(keyCode), e2);
                            return -1L;
                        }
                    }
                    Slog.i("WindowManager", "Dropping unregistered shortcut key combination: SEARCH+" + KeyEvent.keyCodeToString(keyCode));
                    return -1L;
                }
                return -1L;
            }
        }
        if (z2 && repeatCount == 0 && !zKeyguardOn && (65536 & metaState) != 0) {
            KeyCharacterMap keyCharacterMap2 = keyEvent.getKeyCharacterMap();
            if (keyCharacterMap2.isPrintingKey(keyCode) && (intent = this.mShortcutManager.getIntent(keyCharacterMap2, keyCode, (-458753) & metaState)) != null) {
                intent.addFlags(268435456);
                try {
                    startActivityAsUser(intent, UserHandle.CURRENT);
                    dismissKeyboardShortcutsMenu();
                    return -1L;
                } catch (ActivityNotFoundException e3) {
                    Slog.w("WindowManager", "Dropping shortcut key combination because the activity to which it is registered was not found: META+" + KeyEvent.keyCodeToString(keyCode), e3);
                    return -1L;
                }
            }
        }
        if (z2 && repeatCount == 0 && !zKeyguardOn && (str = sApplicationLaunchKeyCategories.get(keyCode)) != null) {
            Intent intentMakeMainSelectorActivity = Intent.makeMainSelectorActivity("android.intent.action.MAIN", str);
            intentMakeMainSelectorActivity.setFlags(268435456);
            try {
                startActivityAsUser(intentMakeMainSelectorActivity, UserHandle.CURRENT);
                dismissKeyboardShortcutsMenu();
                return -1L;
            } catch (ActivityNotFoundException e4) {
                Slog.w("WindowManager", "Dropping application launch key because the activity to which it is registered was not found: keyCode=" + keyCode + ", category=" + str, e4);
                return -1L;
            }
        }
        if (z2 && repeatCount == 0 && keyCode == 61) {
            if (this.mRecentAppsHeldModifiers == 0 && !zKeyguardOn && isUserSetupComplete()) {
                int modifiers = keyEvent.getModifiers() & (-194);
                if (KeyEvent.metaStateHasModifiers(modifiers, 2)) {
                    if (BenesseExtension.getDchaState() != 0) {
                        return -1L;
                    }
                    this.mRecentAppsHeldModifiers = modifiers;
                    showRecentApps(true);
                    return -1L;
                }
            }
        } else if (!z2 && this.mRecentAppsHeldModifiers != 0 && (this.mRecentAppsHeldModifiers & metaState) == 0) {
            this.mRecentAppsHeldModifiers = 0;
            hideRecentApps(true, false);
        }
        if (z2 && repeatCount == 0 && keyCode == 62 && (metaState & 28672) != 0) {
            this.mWindowManagerFuncs.switchKeyboardLayout(keyEvent.getDeviceId(), (metaState & HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS) != 0 ? -1 : 1);
            return -1L;
        }
        if (z2 && repeatCount == 0 && (keyCode == 204 || (keyCode == 62 && (metaState & 458752) != 0))) {
            this.mWindowManagerFuncs.switchInputMethod((metaState & HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS) == 0);
            return -1L;
        }
        if (this.mLanguageSwitchKeyPressed && !z2 && (keyCode == 204 || keyCode == 62)) {
            this.mLanguageSwitchKeyPressed = false;
            return -1L;
        }
        if (isValidGlobalKey(keyCode) && this.mGlobalKeyManager.handleGlobalKey(this.mContext, keyCode, keyEvent)) {
            return -1L;
        }
        if (z2) {
            long j4 = keyCode;
            if (keyEvent.isCtrlPressed()) {
                j4 |= 17592186044416L;
            }
            if (keyEvent.isAltPressed()) {
                j4 |= 8589934592L;
            }
            if (keyEvent.isShiftPressed()) {
                j4 |= 4294967296L;
            }
            if (keyEvent.isMetaPressed()) {
                j4 |= 281474976710656L;
            }
            IShortcutService iShortcutService = this.mShortcutKeyServices.get(j4);
            if (iShortcutService != null) {
                try {
                    if (isUserSetupComplete()) {
                        iShortcutService.notifyShortcutKeyPressed(j4);
                        return -1L;
                    }
                    return -1L;
                } catch (RemoteException e5) {
                    this.mShortcutKeyServices.delete(j4);
                    return -1L;
                }
            }
        }
        return (65536 & metaState) != 0 ? -1L : 0L;
    }

    private boolean interceptBugreportGestureTv(int i, boolean z) {
        if (i == 23) {
            this.mBugreportTvKey1Pressed = z;
        } else if (i == 4) {
            this.mBugreportTvKey2Pressed = z;
        }
        if (this.mBugreportTvKey1Pressed && this.mBugreportTvKey2Pressed) {
            if (!this.mBugreportTvScheduled) {
                this.mBugreportTvScheduled = true;
                Message messageObtain = Message.obtain(this.mHandler, 21);
                messageObtain.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(messageObtain, 1000L);
            }
        } else if (this.mBugreportTvScheduled) {
            this.mHandler.removeMessages(21);
            this.mBugreportTvScheduled = false;
        }
        return this.mBugreportTvScheduled;
    }

    private boolean interceptAccessibilityGestureTv(int i, boolean z) {
        if (i == 4) {
            this.mAccessibilityTvKey1Pressed = z;
        } else if (i == 20) {
            this.mAccessibilityTvKey2Pressed = z;
        }
        if (this.mAccessibilityTvKey1Pressed && this.mAccessibilityTvKey2Pressed) {
            if (!this.mAccessibilityTvScheduled) {
                this.mAccessibilityTvScheduled = true;
                Message messageObtain = Message.obtain(this.mHandler, 22);
                messageObtain.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(messageObtain, getAccessibilityShortcutTimeout());
            }
        } else if (this.mAccessibilityTvScheduled) {
            this.mHandler.removeMessages(22);
            this.mAccessibilityTvScheduled = false;
        }
        return this.mAccessibilityTvScheduled;
    }

    private void requestFullBugreport() {
        if ("1".equals(SystemProperties.get("ro.debuggable")) || Settings.Global.getInt(this.mContext.getContentResolver(), "development_settings_enabled", 0) == 1) {
            try {
                ActivityManager.getService().requestBugReport(0);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error taking bugreport", e);
            }
        }
    }

    @Override
    public KeyEvent dispatchUnhandledKey(WindowManagerPolicy.WindowState windowState, KeyEvent keyEvent, int i) {
        KeyEvent keyEventObtain;
        boolean z;
        KeyCharacterMap.FallbackAction fallbackAction;
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "Unhandled key: win=" + windowState + ", action=" + keyEvent.getAction() + ", flags=" + keyEvent.getFlags() + ", keyCode=" + keyEvent.getKeyCode() + ", scanCode=" + keyEvent.getScanCode() + ", metaState=" + keyEvent.getMetaState() + ", repeatCount=" + keyEvent.getRepeatCount() + ", policyFlags=" + i);
        }
        if ((keyEvent.getFlags() & 1024) == 0) {
            KeyCharacterMap keyCharacterMap = keyEvent.getKeyCharacterMap();
            int keyCode = keyEvent.getKeyCode();
            int metaState = keyEvent.getMetaState();
            if (keyEvent.getAction() != 0 || keyEvent.getRepeatCount() != 0) {
                z = false;
            } else {
                z = true;
            }
            if (z) {
                fallbackAction = keyCharacterMap.getFallbackAction(keyCode, metaState);
            } else {
                fallbackAction = this.mFallbackActions.get(keyCode);
            }
            if (fallbackAction != null) {
                if (DEBUG_INPUT) {
                    Slog.d("WindowManager", "Fallback: keyCode=" + fallbackAction.keyCode + " metaState=" + Integer.toHexString(fallbackAction.metaState));
                }
                keyEventObtain = KeyEvent.obtain(keyEvent.getDownTime(), keyEvent.getEventTime(), keyEvent.getAction(), fallbackAction.keyCode, keyEvent.getRepeatCount(), fallbackAction.metaState, keyEvent.getDeviceId(), keyEvent.getScanCode(), keyEvent.getFlags() | 1024, keyEvent.getSource(), null);
                if (!interceptFallback(windowState, keyEventObtain, i)) {
                    keyEventObtain.recycle();
                    keyEventObtain = null;
                }
                if (z) {
                    this.mFallbackActions.put(keyCode, fallbackAction);
                } else if (keyEvent.getAction() == 1) {
                    this.mFallbackActions.remove(keyCode);
                    fallbackAction.recycle();
                }
            } else {
                keyEventObtain = null;
            }
        }
        if (DEBUG_INPUT) {
            if (keyEventObtain == null) {
                Slog.d("WindowManager", "No fallback.");
            } else {
                Slog.d("WindowManager", "Performing fallback: " + keyEventObtain);
            }
        }
        return keyEventObtain;
    }

    private boolean interceptFallback(WindowManagerPolicy.WindowState windowState, KeyEvent keyEvent, int i) {
        if ((interceptKeyBeforeQueueing(keyEvent, i) & 1) != 0 && interceptKeyBeforeDispatching(windowState, keyEvent, i) == 0) {
            return true;
        }
        return false;
    }

    @Override
    public void registerShortcutKey(long j, IShortcutService iShortcutService) throws RemoteException {
        synchronized (this.mLock) {
            IShortcutService iShortcutService2 = this.mShortcutKeyServices.get(j);
            if (iShortcutService2 != null && iShortcutService2.asBinder().pingBinder()) {
                throw new RemoteException("Key already exists.");
            }
            this.mShortcutKeyServices.put(j, iShortcutService);
        }
    }

    @Override
    public void onKeyguardOccludedChangedLw(boolean z) {
        if (this.mKeyguardDelegate != null && this.mKeyguardDelegate.isShowing()) {
            this.mPendingKeyguardOccluded = z;
            this.mKeyguardOccludedChanged = true;
        } else {
            setKeyguardOccludedLw(z, false);
        }
    }

    private int handleStartTransitionForKeyguardLw(int i, long j) {
        if (this.mKeyguardOccludedChanged) {
            if (DEBUG_KEYGUARD) {
                Slog.d("WindowManager", "transition/occluded changed occluded=" + this.mPendingKeyguardOccluded);
            }
            this.mKeyguardOccludedChanged = false;
            if (setKeyguardOccludedLw(this.mPendingKeyguardOccluded, false)) {
                return 5;
            }
        }
        if (AppTransition.isKeyguardGoingAwayTransit(i)) {
            if (DEBUG_KEYGUARD) {
                Slog.d("WindowManager", "Starting keyguard exit animation");
            }
            startKeyguardExitAnimation(SystemClock.uptimeMillis(), j);
        }
        return 0;
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
            Slog.w("WindowManager", "No activity to handle assist long press action.", e);
        }
    }

    private void launchAssistAction(String str, int i) {
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        if (!isUserSetupComplete()) {
            return;
        }
        Bundle bundle = null;
        if (i > Integer.MIN_VALUE) {
            bundle = new Bundle();
            bundle.putInt("android.intent.extra.ASSIST_INPUT_DEVICE_ID", i);
        }
        if ((this.mContext.getResources().getConfiguration().uiMode & 15) == 4) {
            ((SearchManager) this.mContext.getSystemService("search")).launchLegacyAssist(str, UserHandle.myUserId(), bundle);
            return;
        }
        if (str != null) {
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putBoolean(str, true);
        }
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.startAssist(bundle);
        }
    }

    private void startActivityAsUser(Intent intent, UserHandle userHandle) {
        if (isUserSetupComplete()) {
            this.mContext.startActivityAsUser(intent, userHandle);
            return;
        }
        Slog.i("WindowManager", "Not starting activity because user setup is in progress: " + intent);
    }

    private SearchManager getSearchManager() {
        if (this.mSearchManager == null) {
            this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
        }
        return this.mSearchManager;
    }

    private void preloadRecentApps() {
        this.mPreloadedRecentApps = true;
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.preloadRecentApps();
        }
    }

    private void cancelPreloadRecentApps() {
        if (this.mPreloadedRecentApps) {
            this.mPreloadedRecentApps = false;
            StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
            if (statusBarManagerInternal != null) {
                statusBarManagerInternal.cancelPreloadRecentApps();
            }
        }
    }

    private void toggleRecentApps() {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.toggleRecentApps();
        }
    }

    @Override
    public void showRecentApps() {
        this.mHandler.removeMessages(9);
        this.mHandler.obtainMessage(9).sendToTarget();
    }

    private void showRecentApps(boolean z) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.showRecentApps(z);
        }
    }

    private void toggleKeyboardShortcutsMenu(int i) {
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.toggleKeyboardShortcutsMenu(i);
        }
    }

    private void dismissKeyboardShortcutsMenu() {
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.dismissKeyboardShortcutsMenu();
        }
    }

    private void hideRecentApps(boolean z, boolean z2) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.hideRecentApps(z, z2);
        }
    }

    void launchHomeFromHotKey() {
        launchHomeFromHotKey(true, true);
    }

    void launchHomeFromHotKey(final boolean z, boolean z2) {
        Handler handler = this.mHandler;
        WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs = this.mWindowManagerFuncs;
        Objects.requireNonNull(windowManagerFuncs);
        handler.post(new $$Lambda$oXa0y3A00RiQs6KTPBgpkGtgw(windowManagerFuncs));
        if (z2) {
            if (isKeyguardShowingAndNotOccluded()) {
                return;
            }
            if (this.mKeyguardOccluded && this.mKeyguardDelegate.isShowing()) {
                this.mKeyguardDelegate.dismiss(new AnonymousClass11(z), null);
                return;
            } else if (!this.mKeyguardOccluded && this.mKeyguardDelegate.isInputRestricted()) {
                this.mKeyguardDelegate.verifyUnlock(new WindowManagerPolicy.OnKeyguardExitResult() {
                    @Override
                    public void onKeyguardExitResult(boolean z3) {
                        if (z3) {
                            PhoneWindowManager.this.startDockOrHome(true, z);
                        }
                    }
                });
                return;
            }
        }
        if (this.mRecentsVisible) {
            try {
                ActivityManager.getService().stopAppSwitches();
            } catch (RemoteException e) {
            }
            if (z) {
                awakenDreams();
            }
            hideRecentApps(false, true);
            return;
        }
        startDockOrHome(true, z);
    }

    class AnonymousClass11 extends KeyguardDismissCallback {
        final boolean val$awakenFromDreams;

        AnonymousClass11(boolean z) {
            this.val$awakenFromDreams = z;
        }

        public void onDismissSucceeded() throws RemoteException {
            Handler handler = PhoneWindowManager.this.mHandler;
            final boolean z = this.val$awakenFromDreams;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    PhoneWindowManager.this.startDockOrHome(true, z);
                }
            });
        }
    }

    final class HideNavInputEventReceiver extends InputEventReceiver {
        public HideNavInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent inputEvent, int i) {
            boolean z;
            try {
                if ((inputEvent instanceof MotionEvent) && (inputEvent.getSource() & 2) != 0 && ((MotionEvent) inputEvent).getAction() == 0) {
                    synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                        if (PhoneWindowManager.this.mInputConsumer == null) {
                            return;
                        }
                        int i2 = PhoneWindowManager.this.mResettingSystemUiFlags | 2 | 1 | 4;
                        if (PhoneWindowManager.this.mResettingSystemUiFlags == i2) {
                            z = false;
                        } else {
                            PhoneWindowManager.this.mResettingSystemUiFlags = i2;
                            z = true;
                        }
                        int i3 = PhoneWindowManager.this.mForceClearedSystemUiFlags | 2;
                        if (PhoneWindowManager.this.mForceClearedSystemUiFlags != i3) {
                            PhoneWindowManager.this.mForceClearedSystemUiFlags = i3;
                            PhoneWindowManager.this.mHandler.postDelayed(PhoneWindowManager.this.mClearHideNavigationFlag, 1000L);
                            z = true;
                        }
                        if (z) {
                            PhoneWindowManager.this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
                        }
                    }
                }
            } finally {
                finishInputEvent(inputEvent, false);
            }
        }
    }

    @Override
    public void setRecentsVisibilityLw(boolean z) {
        this.mRecentsVisible = z;
    }

    @Override
    public void setPipVisibilityLw(boolean z) {
        this.mPictureInPictureVisible = z;
    }

    @Override
    public void setNavBarVirtualKeyHapticFeedbackEnabledLw(boolean z) {
        this.mNavBarVirtualKeyHapticFeedbackEnabled = z;
    }

    @Override
    public int adjustSystemUiVisibilityLw(int i) {
        this.mStatusBarController.adjustSystemUiVisibilityLw(this.mLastSystemUiFlags, i);
        this.mNavigationBarController.adjustSystemUiVisibilityLw(this.mLastSystemUiFlags, i);
        this.mResettingSystemUiFlags &= i;
        return i & (~this.mResettingSystemUiFlags) & (~this.mForceClearedSystemUiFlags);
    }

    @Override
    public boolean getLayoutHintLw(WindowManager.LayoutParams layoutParams, Rect rect, DisplayFrames displayFrames, Rect rect2, Rect rect3, Rect rect4, Rect rect5, DisplayCutout.ParcelableWrapper parcelableWrapper) {
        int i;
        int i2;
        int windowOutsetBottomPx;
        int windowFlags = PolicyControl.getWindowFlags(null, layoutParams);
        int i3 = layoutParams.privateFlags;
        int systemUiVisibility = PolicyControl.getSystemUiVisibility(null, layoutParams) | getImpliedSysUiFlagsForLayout(layoutParams);
        int i4 = displayFrames.mRotation;
        int i5 = displayFrames.mDisplayWidth;
        int i6 = displayFrames.mDisplayHeight;
        if ((rect5 != null && shouldUseOutsets(layoutParams, windowFlags)) && (windowOutsetBottomPx = ScreenShapeHelper.getWindowOutsetBottomPx(this.mContext.getResources())) > 0) {
            if (i4 == 0) {
                rect5.bottom += windowOutsetBottomPx;
            } else if (i4 == 1) {
                rect5.right += windowOutsetBottomPx;
            } else if (i4 == 2) {
                rect5.top += windowOutsetBottomPx;
            } else if (i4 == 3) {
                rect5.left += windowOutsetBottomPx;
            }
        }
        boolean z = (windowFlags & 256) != 0;
        boolean z2 = z && (65536 & windowFlags) != 0;
        boolean z3 = (i3 & DumpState.DUMP_CHANGES) != 0;
        if (z2 && !z3) {
            if (canHideNavigationBar() && (systemUiVisibility & 512) != 0) {
                rect2.set(displayFrames.mUnrestricted);
                i = displayFrames.mUnrestricted.right;
                i2 = displayFrames.mUnrestricted.bottom;
            } else {
                rect2.set(displayFrames.mRestricted);
                i = displayFrames.mRestricted.right;
                i2 = displayFrames.mRestricted.bottom;
            }
            rect4.set(displayFrames.mStable.left, displayFrames.mStable.top, i - displayFrames.mStable.right, i2 - displayFrames.mStable.bottom);
            if ((systemUiVisibility & 256) != 0) {
                if ((windowFlags & 1024) != 0) {
                    rect3.set(displayFrames.mStableFullscreen.left, displayFrames.mStableFullscreen.top, i - displayFrames.mStableFullscreen.right, i2 - displayFrames.mStableFullscreen.bottom);
                } else {
                    rect3.set(rect4);
                }
            } else if ((windowFlags & 1024) != 0 || (33554432 & windowFlags) != 0) {
                rect3.setEmpty();
            } else {
                rect3.set(displayFrames.mCurrent.left, displayFrames.mCurrent.top, i - displayFrames.mCurrent.right, i2 - displayFrames.mCurrent.bottom);
            }
            if (rect != null) {
                calculateRelevantTaskInsets(rect, rect3, i5, i6);
                calculateRelevantTaskInsets(rect, rect4, i5, i6);
                rect2.intersect(rect);
            }
            parcelableWrapper.set(displayFrames.mDisplayCutout.calculateRelativeTo(rect2).getDisplayCutout());
            return this.mForceShowSystemBars;
        }
        if (z) {
            rect2.set(displayFrames.mUnrestricted);
        } else {
            rect2.set(displayFrames.mStable);
        }
        if (rect != null) {
            rect2.intersect(rect);
        }
        rect3.setEmpty();
        rect4.setEmpty();
        parcelableWrapper.set(DisplayCutout.NO_CUTOUT);
        return this.mForceShowSystemBars;
    }

    private void calculateRelevantTaskInsets(Rect rect, Rect rect2, int i, int i2) {
        mTmpRect.set(0, 0, i, i2);
        mTmpRect.inset(rect2);
        mTmpRect.intersect(rect);
        rect2.set(mTmpRect.left - rect.left, mTmpRect.top - rect.top, rect.right - mTmpRect.right, rect.bottom - mTmpRect.bottom);
    }

    private boolean shouldUseOutsets(WindowManager.LayoutParams layoutParams, int i) {
        return layoutParams.type == 2013 || (33555456 & i) != 0;
    }

    @Override
    public void beginLayoutLw(DisplayFrames displayFrames, int i) {
        displayFrames.onBeginLayout();
        this.mSystemGestures.screenWidth = displayFrames.mUnrestricted.width();
        this.mSystemGestures.screenHeight = displayFrames.mUnrestricted.height();
        this.mDockLayer = 268435456;
        this.mStatusBarLayer = -1;
        Rect rect = mTmpParentFrame;
        Rect rect2 = mTmpDisplayFrame;
        Rect rect3 = mTmpOverscanFrame;
        Rect rect4 = mTmpVisibleFrame;
        Rect rect5 = mTmpDecorFrame;
        rect4.set(displayFrames.mDock);
        rect3.set(displayFrames.mDock);
        rect2.set(displayFrames.mDock);
        rect.set(displayFrames.mDock);
        rect5.setEmpty();
        if (displayFrames.mDisplayId == 0) {
            if (this.mWmsExt.isFullscreenSwitchSupport() && this.mWmsExt.isFocusWindowReady(this.mFocusedWindow)) {
                this.mLastSystemUiFlags &= -8193;
            }
            int i2 = this.mLastSystemUiFlags;
            boolean z = (i2 & 2) == 0 && !this.mForceHideNavBar;
            boolean z2 = ((-2147450880) & i2) != 0;
            boolean z3 = (i2 & 2048) != 0 || this.mForceHideNavBar;
            boolean z4 = (i2 & 4096) != 0;
            boolean z5 = z3 || z4;
            boolean zAreTranslucentBarsAllowed = z2 & (!z4);
            boolean z6 = isStatusBarKeyguard() && !this.mKeyguardOccluded;
            if (!z6) {
                zAreTranslucentBarsAllowed &= areTranslucentBarsAllowed();
            }
            boolean z7 = zAreTranslucentBarsAllowed;
            boolean z8 = (z6 || this.mStatusBar == null || this.mForceHideNavBar || this.mStatusBar.getAttrs().height != -1 || this.mStatusBar.getAttrs().width != -1) ? false : true;
            if (z || z5) {
                if (this.mInputConsumer != null) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(19, this.mInputConsumer));
                    this.mInputConsumer = null;
                }
            } else if (this.mInputConsumer == null && this.mStatusBar != null && canHideNavigationBar()) {
                this.mInputConsumer = this.mWindowManagerFuncs.createInputConsumer(this.mHandler.getLooper(), "nav_input_consumer", new InputEventReceiver.Factory() {
                    public final InputEventReceiver createInputEventReceiver(InputChannel inputChannel, Looper looper) {
                        return PhoneWindowManager.lambda$beginLayoutLw$2(this.f$0, inputChannel, looper);
                    }
                });
                InputManager.getInstance().setPointerIconType(0);
            }
            boolean zLayoutNavigationBar = layoutNavigationBar(displayFrames, i, rect5, z | (!canHideNavigationBar()), z7, z5, z8);
            if (DEBUG_LAYOUT) {
                Slog.i("WindowManager", "mDock rect:" + displayFrames.mDock);
            }
            if (zLayoutNavigationBar | layoutStatusBar(displayFrames, rect, rect2, rect3, rect4, rect5, i2, z6)) {
                updateSystemUiVisibilityLw();
            }
        }
        layoutScreenDecorWindows(displayFrames, rect, rect2, rect5);
        if (displayFrames.mDisplayCutoutSafe.top > displayFrames.mUnrestricted.top) {
            displayFrames.mDisplayCutoutSafe.top = Math.max(displayFrames.mDisplayCutoutSafe.top, displayFrames.mStable.top);
        }
    }

    public static InputEventReceiver lambda$beginLayoutLw$2(PhoneWindowManager phoneWindowManager, InputChannel inputChannel, Looper looper) {
        return phoneWindowManager.new HideNavInputEventReceiver(inputChannel, looper);
    }

    private void layoutScreenDecorWindows(DisplayFrames displayFrames, Rect rect, Rect rect2, Rect rect3) {
        if (this.mScreenDecorWindows.isEmpty()) {
            return;
        }
        int i = displayFrames.mDisplayId;
        Rect rect4 = displayFrames.mDock;
        int i2 = displayFrames.mDisplayHeight;
        int i3 = displayFrames.mDisplayWidth;
        for (int size = this.mScreenDecorWindows.size() - 1; size >= 0; size--) {
            WindowManagerPolicy.WindowState windowStateValueAt = this.mScreenDecorWindows.valueAt(size);
            if (windowStateValueAt.getDisplayId() == i && windowStateValueAt.isVisibleLw()) {
                windowStateValueAt.computeFrameLw(rect, rect2, rect2, rect2, rect2, rect3, rect2, rect2, displayFrames.mDisplayCutout, false);
                Rect frameLw = windowStateValueAt.getFrameLw();
                if (frameLw.left <= 0 && frameLw.top <= 0) {
                    if (frameLw.bottom >= i2) {
                        rect4.left = Math.max(frameLw.right, rect4.left);
                    } else if (frameLw.right >= i3) {
                        rect4.top = Math.max(frameLw.bottom, rect4.top);
                    } else {
                        Slog.w("WindowManager", "layoutScreenDecorWindows: Ignoring decor win=" + windowStateValueAt + " not docked on left or top of display. frame=" + frameLw + " displayWidth=" + i3 + " displayHeight=" + i2);
                    }
                } else if (frameLw.right >= i3 && frameLw.bottom >= i2) {
                    if (frameLw.top <= 0) {
                        rect4.right = Math.min(frameLw.left, rect4.right);
                    } else if (frameLw.left <= 0) {
                        rect4.bottom = Math.min(frameLw.top, rect4.bottom);
                    } else {
                        Slog.w("WindowManager", "layoutScreenDecorWindows: Ignoring decor win=" + windowStateValueAt + " not docked on right or bottom of display. frame=" + frameLw + " displayWidth=" + i3 + " displayHeight=" + i2);
                    }
                } else {
                    Slog.w("WindowManager", "layoutScreenDecorWindows: Ignoring decor win=" + windowStateValueAt + " not docked on one of the sides of the display. frame=" + frameLw + " displayWidth=" + i3 + " displayHeight=" + i2);
                }
            }
        }
        displayFrames.mRestricted.set(rect4);
        displayFrames.mCurrent.set(rect4);
        displayFrames.mVoiceContent.set(rect4);
        displayFrames.mSystem.set(rect4);
        displayFrames.mContent.set(rect4);
        displayFrames.mRestrictedOverscan.set(rect4);
    }

    private boolean layoutStatusBar(DisplayFrames displayFrames, Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, int i, boolean z) {
        if (this.mStatusBar == null) {
            return false;
        }
        rect3.set(displayFrames.mUnrestricted);
        rect2.set(displayFrames.mUnrestricted);
        rect.set(displayFrames.mUnrestricted);
        rect4.set(displayFrames.mStable);
        this.mStatusBarLayer = this.mStatusBar.getSurfaceLayer();
        this.mStatusBar.computeFrameLw(rect, rect2, rect4, rect4, rect4, rect5, rect4, rect4, displayFrames.mDisplayCutout, false);
        displayFrames.mStable.top = displayFrames.mUnrestricted.top + this.mStatusBarHeightForRotation[displayFrames.mRotation];
        displayFrames.mStable.top = Math.max(displayFrames.mStable.top, displayFrames.mDisplayCutoutSafe.top);
        mTmpRect.set(this.mStatusBar.getContentFrameLw());
        mTmpRect.intersect(displayFrames.mDisplayCutoutSafe);
        mTmpRect.top = this.mStatusBar.getContentFrameLw().top;
        mTmpRect.bottom = displayFrames.mStable.top;
        this.mStatusBarController.setContentFrame(mTmpRect);
        boolean z2 = (i & 67108864) != 0;
        boolean zAreTranslucentBarsAllowed = (i & 1073741832) != 0;
        if (!z) {
            zAreTranslucentBarsAllowed &= areTranslucentBarsAllowed();
        }
        if (this.mStatusBar.isVisibleLw() && !z2) {
            Rect rect6 = displayFrames.mDock;
            rect6.top = displayFrames.mStable.top;
            displayFrames.mContent.set(rect6);
            displayFrames.mVoiceContent.set(rect6);
            displayFrames.mCurrent.set(rect6);
            if (DEBUG_LAYOUT) {
                Slog.v("WindowManager", "Status bar: " + String.format("dock=%s content=%s cur=%s", rect6.toString(), displayFrames.mContent.toString(), displayFrames.mCurrent.toString()));
            }
            if (!this.mStatusBar.isAnimatingLw() && !zAreTranslucentBarsAllowed && !this.mStatusBarController.wasRecentlyTranslucent()) {
                displayFrames.mSystem.top = displayFrames.mStable.top;
            }
        }
        return this.mStatusBarController.checkHiddenLw();
    }

    private boolean layoutNavigationBar(DisplayFrames displayFrames, int i, Rect rect, boolean z, boolean z2, boolean z3, boolean z4) {
        if (this.mNavigationBar == null) {
            return false;
        }
        boolean zIsTransientShowing = this.mNavigationBarController.isTransientShowing();
        int i2 = displayFrames.mRotation;
        int i3 = displayFrames.mDisplayHeight;
        int i4 = displayFrames.mDisplayWidth;
        Rect rect2 = displayFrames.mDock;
        this.mNavigationBarPosition = navigationBarPosition(i4, i3, i2);
        Rect rect3 = mTmpRect;
        rect3.set(displayFrames.mUnrestricted);
        rect3.intersectUnchecked(displayFrames.mDisplayCutoutSafe);
        if (this.mNavigationBarPosition == 4) {
            int navigationBarHeight = rect3.bottom - getNavigationBarHeight(i2, i);
            mTmpNavigationFrame.set(0, navigationBarHeight, i4, displayFrames.mUnrestricted.bottom);
            Rect rect4 = displayFrames.mStable;
            displayFrames.mStableFullscreen.bottom = navigationBarHeight;
            rect4.bottom = navigationBarHeight;
            if (zIsTransientShowing) {
                this.mNavigationBarController.setBarShowingLw(true);
            } else if (!z) {
                this.mNavigationBarController.setBarShowingLw(z4);
            } else {
                this.mNavigationBarController.setBarShowingLw(true);
                Rect rect5 = displayFrames.mRestricted;
                displayFrames.mRestrictedOverscan.bottom = navigationBarHeight;
                rect5.bottom = navigationBarHeight;
                rect2.bottom = navigationBarHeight;
            }
            if (z && !z2 && !z3 && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent()) {
                displayFrames.mSystem.bottom = navigationBarHeight;
            }
        } else if (this.mNavigationBarPosition == 2) {
            int navigationBarWidth = rect3.right - getNavigationBarWidth(i2, i);
            mTmpNavigationFrame.set(navigationBarWidth, 0, displayFrames.mUnrestricted.right, i3);
            Rect rect6 = displayFrames.mStable;
            displayFrames.mStableFullscreen.right = navigationBarWidth;
            rect6.right = navigationBarWidth;
            if (zIsTransientShowing) {
                this.mNavigationBarController.setBarShowingLw(true);
            } else if (!z) {
                this.mNavigationBarController.setBarShowingLw(z4);
            } else {
                this.mNavigationBarController.setBarShowingLw(true);
                Rect rect7 = displayFrames.mRestricted;
                displayFrames.mRestrictedOverscan.right = navigationBarWidth;
                rect7.right = navigationBarWidth;
                rect2.right = navigationBarWidth;
            }
            if (z && !z2 && !z3 && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent()) {
                displayFrames.mSystem.right = navigationBarWidth;
            }
        } else if (this.mNavigationBarPosition == 1) {
            int navigationBarWidth2 = rect3.left + getNavigationBarWidth(i2, i);
            mTmpNavigationFrame.set(displayFrames.mUnrestricted.left, 0, navigationBarWidth2, i3);
            Rect rect8 = displayFrames.mStable;
            displayFrames.mStableFullscreen.left = navigationBarWidth2;
            rect8.left = navigationBarWidth2;
            if (zIsTransientShowing) {
                this.mNavigationBarController.setBarShowingLw(true);
            } else if (!z) {
                this.mNavigationBarController.setBarShowingLw(z4);
            } else {
                this.mNavigationBarController.setBarShowingLw(true);
                Rect rect9 = displayFrames.mRestricted;
                displayFrames.mRestrictedOverscan.left = navigationBarWidth2;
                rect9.left = navigationBarWidth2;
                rect2.left = navigationBarWidth2;
            }
            if (z && !z2 && !z3 && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent()) {
                displayFrames.mSystem.left = navigationBarWidth2;
            }
        }
        displayFrames.mCurrent.set(rect2);
        displayFrames.mVoiceContent.set(rect2);
        displayFrames.mContent.set(rect2);
        this.mStatusBarLayer = this.mNavigationBar.getSurfaceLayer();
        this.mNavigationBar.computeFrameLw(mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, displayFrames.mDisplayCutoutSafe, mTmpNavigationFrame, rect, mTmpNavigationFrame, displayFrames.mDisplayCutoutSafe, displayFrames.mDisplayCutout, false);
        this.mNavigationBarController.setContentFrame(this.mNavigationBar.getContentFrameLw());
        if (DEBUG_LAYOUT) {
            Slog.i("WindowManager", "mNavigationBar frame: " + mTmpNavigationFrame);
        }
        return this.mNavigationBarController.checkHiddenLw();
    }

    private int navigationBarPosition(int i, int i2, int i3) {
        if (this.mNavigationBarCanMove && i > i2) {
            if (i3 == 3) {
                return 1;
            }
            return 2;
        }
        return 4;
    }

    @Override
    public int getSystemDecorLayerLw() {
        if (this.mStatusBar != null && this.mStatusBar.isVisibleLw()) {
            return this.mStatusBar.getSurfaceLayer();
        }
        if (this.mNavigationBar != null && this.mNavigationBar.isVisibleLw()) {
            return this.mNavigationBar.getSurfaceLayer();
        }
        return 0;
    }

    private void setAttachedWindowFrames(WindowManagerPolicy.WindowState windowState, int i, int i2, WindowManagerPolicy.WindowState windowState2, boolean z, Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, DisplayFrames displayFrames) {
        if (!windowState.isInputMethodTarget() && windowState2.isInputMethodTarget()) {
            rect5.set(displayFrames.mDock);
            rect4.set(displayFrames.mDock);
            rect3.set(displayFrames.mDock);
            rect2.set(displayFrames.mDock);
        } else {
            if (i2 != 16) {
                rect4.set((1073741824 & i) != 0 ? windowState2.getContentFrameLw() : windowState2.getOverscanFrameLw());
            } else {
                rect4.set(windowState2.getContentFrameLw());
                if (windowState2.isVoiceInteraction()) {
                    rect4.intersectUnchecked(displayFrames.mVoiceContent);
                } else if (windowState.isInputMethodTarget() || windowState2.isInputMethodTarget()) {
                    rect4.intersectUnchecked(displayFrames.mContent);
                }
            }
            rect2.set(z ? windowState2.getDisplayFrameLw() : rect4);
            if (z) {
                rect4 = windowState2.getOverscanFrameLw();
            }
            rect3.set(rect4);
            rect5.set(windowState2.getVisibleFrameLw());
        }
        if ((i & 256) == 0) {
            rect2 = windowState2.getFrameLw();
        }
        rect.set(rect2);
    }

    private void applyStableConstraints(int i, int i2, Rect rect, DisplayFrames displayFrames) {
        if ((i & 256) == 0) {
            return;
        }
        if ((i2 & 1024) != 0) {
            rect.intersectUnchecked(displayFrames.mStableFullscreen);
        } else {
            rect.intersectUnchecked(displayFrames.mStable);
        }
    }

    private boolean canReceiveInput(WindowManagerPolicy.WindowState windowState) {
        return !(((windowState.getAttrs().flags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0) ^ ((windowState.getAttrs().flags & 8) != 0));
    }

    @Override
    public void layoutWindowLw(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2, DisplayFrames displayFrames) {
        Rect rect;
        int i;
        boolean z;
        boolean z2;
        int i2;
        Rect rect2;
        Rect rect3;
        Rect rect4;
        Rect rect5;
        Rect rect6;
        Rect rect7;
        Rect rect8;
        Rect rect9;
        WindowManager.LayoutParams layoutParams;
        int i3;
        int i4;
        PhoneWindowManager phoneWindowManager;
        int i5;
        int i6;
        WindowManager.LayoutParams layoutParams2;
        int i7;
        int i8;
        Rect rect10;
        Rect rect11;
        int i9;
        int i10;
        int i11;
        int i12;
        Rect rect12;
        int i13;
        Rect rect13;
        Rect rect14;
        char c;
        int i14;
        Rect rect15;
        Rect rect16;
        int i15;
        int i16;
        int i17;
        int i18;
        int i19;
        boolean z3;
        boolean z4;
        boolean z5;
        Rect rect17;
        Rect rect18;
        Rect rect19;
        int i20;
        PhoneWindowManager phoneWindowManager2;
        Rect rect20;
        boolean z6;
        if ((windowState == this.mStatusBar && !canReceiveInput(windowState)) || windowState == this.mNavigationBar || this.mScreenDecorWindows.contains(windowState)) {
            return;
        }
        if (this.mWmsExt.isFullscreenSwitchSupport()) {
            Rect switchFrame = this.mWmsExt.getSwitchFrame(windowState, this.mFocusedWindow, displayFrames.mOverscan.right - displayFrames.mOverscan.left, displayFrames.mOverscan.bottom - displayFrames.mOverscan.top);
            if (switchFrame != null && (switchFrame.top != 0 || switchFrame.left != 0)) {
                updateRect(displayFrames, switchFrame.left, switchFrame.top, switchFrame.right, switchFrame.bottom);
            }
            rect = switchFrame;
        } else {
            rect = null;
        }
        WindowManager.LayoutParams attrs = windowState.getAttrs();
        boolean zIsDefaultDisplay = windowState.isDefaultDisplay();
        if (zIsDefaultDisplay && windowState == this.mLastInputMethodTargetWindow && this.mLastInputMethodWindow != null) {
            if (DEBUG_LAYOUT) {
                Slog.i("WindowManager", "Offset ime target window by the last ime window state");
            }
            offsetInputMethodWindowLw(this.mLastInputMethodWindow, displayFrames);
        }
        int i21 = attrs.type;
        int windowFlags = PolicyControl.getWindowFlags(windowState, attrs);
        int i22 = attrs.privateFlags;
        int i23 = attrs.softInputMode;
        int systemUiVisibility = PolicyControl.getSystemUiVisibility(null, attrs);
        int impliedSysUiFlagsForLayout = systemUiVisibility | getImpliedSysUiFlagsForLayout(attrs);
        Rect rect21 = mTmpParentFrame;
        Rect rect22 = mTmpDisplayFrame;
        Rect rect23 = mTmpOverscanFrame;
        Rect rect24 = mTmpContentFrame;
        Rect rect25 = mTmpVisibleFrame;
        Rect rect26 = rect;
        Rect rect27 = mTmpDecorFrame;
        Rect rect28 = mTmpStableFrame;
        rect27.setEmpty();
        if (zIsDefaultDisplay) {
            i = i22;
            if (this.mHasNavigationBar && this.mNavigationBar != null && this.mNavigationBar.isVisibleLw()) {
                z = true;
            }
            int i24 = i23 & 240;
            int i25 = windowFlags & 1024;
            boolean z7 = i25 == 0 || (systemUiVisibility & 4) != 0;
            z2 = (windowFlags & 256) != 256;
            boolean z8 = (65536 & windowFlags) != 65536;
            rect28.set(displayFrames.mStable);
            if (i21 != 2011) {
                rect25.set(displayFrames.mDock);
                rect24.set(displayFrames.mDock);
                rect23.set(displayFrames.mDock);
                rect22.set(displayFrames.mDock);
                rect21.set(displayFrames.mDock);
                int i26 = displayFrames.mUnrestricted.bottom;
                rect23.bottom = i26;
                rect22.bottom = i26;
                rect21.bottom = i26;
                int i27 = displayFrames.mStable.bottom;
                rect25.bottom = i27;
                rect24.bottom = i27;
                if (this.mStatusBar != null && this.mFocusedWindow == this.mStatusBar && canReceiveInput(this.mStatusBar)) {
                    if (this.mNavigationBarPosition == 2) {
                        int i28 = displayFrames.mStable.right;
                        rect25.right = i28;
                        rect24.right = i28;
                        rect23.right = i28;
                        rect22.right = i28;
                        rect21.right = i28;
                    } else if (this.mNavigationBarPosition == 1) {
                        int i29 = displayFrames.mStable.left;
                        rect25.left = i29;
                        rect24.left = i29;
                        rect23.left = i29;
                        rect22.left = i29;
                        rect21.left = i29;
                    }
                }
                attrs.gravity = 80;
                this.mDockLayer = windowState.getSurfaceLayer();
            } else if (i21 == 2031) {
                rect23.set(displayFrames.mUnrestricted);
                rect22.set(displayFrames.mUnrestricted);
                rect21.set(displayFrames.mUnrestricted);
                if (i24 != 16) {
                    rect24.set(displayFrames.mDock);
                } else {
                    rect24.set(displayFrames.mContent);
                }
                if (i24 != 48) {
                    rect25.set(displayFrames.mCurrent);
                } else {
                    rect25.set(rect24);
                }
            } else {
                if (i21 != 2013) {
                    i2 = i23;
                    if (windowState == this.mStatusBar) {
                        rect23.set(displayFrames.mUnrestricted);
                        rect22.set(displayFrames.mUnrestricted);
                        rect21.set(displayFrames.mUnrestricted);
                        rect24.set(displayFrames.mStable);
                        rect25.set(displayFrames.mStable);
                        if (i24 == 16) {
                            rect24.bottom = displayFrames.mContent.bottom;
                        } else {
                            rect24.bottom = displayFrames.mDock.bottom;
                            rect25.bottom = displayFrames.mContent.bottom;
                        }
                        layoutParams = attrs;
                        rect2 = rect27;
                        rect6 = rect25;
                        phoneWindowManager = this;
                        rect3 = rect26;
                        rect4 = rect28;
                        i4 = windowFlags;
                        i3 = i21;
                        rect5 = rect21;
                        rect7 = rect24;
                        rect8 = rect22;
                        rect9 = rect23;
                    } else {
                        rect27.set(displayFrames.mSystem);
                        boolean z9 = (attrs.privateFlags & 512) != 0;
                        boolean z10 = i21 >= 1 && i21 <= 99;
                        boolean z11 = windowState == this.mTopFullscreenOpaqueWindowState && !windowState.isAnimatingLw();
                        if (z10 && !z9 && !z11) {
                            if ((impliedSysUiFlagsForLayout & 4) == 0 && i25 == 0 && (windowFlags & 67108864) == 0) {
                                i16 = Integer.MIN_VALUE;
                                if ((windowFlags & Integer.MIN_VALUE) == 0 && (i & DumpState.DUMP_INTENT_FILTER_VERIFIERS) == 0) {
                                    rect27.top = displayFrames.mStable.top;
                                }
                            } else {
                                i16 = Integer.MIN_VALUE;
                            }
                            if ((windowFlags & 134217728) == 0 && (impliedSysUiFlagsForLayout & 2) == 0 && !this.mForceHideNavBar && (windowFlags & i16) == 0) {
                                rect27.bottom = displayFrames.mStable.bottom;
                                rect27.right = displayFrames.mStable.right;
                            }
                        }
                        if (z2 && z8) {
                            if (DEBUG_LAYOUT) {
                                Slog.v("WindowManager", "layoutWindowLw(" + ((Object) attrs.getTitle()) + "): IN_SCREEN, INSET_DECOR");
                            }
                            if (DEBUG_LAYOUT) {
                                i7 = i24;
                                i11 = 2014;
                                i8 = i21;
                                i9 = impliedSysUiFlagsForLayout;
                                rect10 = rect23;
                                rect11 = rect21;
                                i10 = windowFlags;
                                this.mWindowManagerDebugger.debugLayoutWindowLw("WindowManager", i7, attrs.type, windowFlags, canHideNavigationBar(), i9);
                            } else {
                                i7 = i24;
                                i8 = i21;
                                rect10 = rect23;
                                rect11 = rect21;
                                i9 = impliedSysUiFlagsForLayout;
                                i10 = windowFlags;
                                i11 = 2014;
                            }
                            if (windowState2 != null) {
                                i4 = i10;
                                rect4 = rect28;
                                rect2 = rect27;
                                rect3 = rect26;
                                setAttachedWindowFrames(windowState, i4, i7, windowState2, true, rect11, rect22, rect10, rect24, rect25, displayFrames);
                                phoneWindowManager = this;
                                i3 = i8;
                                rect9 = rect10;
                                rect5 = rect11;
                                rect8 = rect22;
                                rect7 = rect24;
                                layoutParams = attrs;
                                rect6 = rect25;
                            } else {
                                rect4 = rect28;
                                rect2 = rect27;
                                int i30 = i11;
                                rect3 = rect26;
                                int i31 = i8;
                                if (i31 == i30 || i31 == 2017) {
                                    i12 = i9;
                                    rect12 = rect10;
                                    i13 = i10;
                                    rect13 = rect11;
                                    rect14 = rect22;
                                    int i32 = (z ? displayFrames.mDock : displayFrames.mUnrestricted).left;
                                    rect12.left = i32;
                                    rect14.left = i32;
                                    rect13.left = i32;
                                    int i33 = displayFrames.mUnrestricted.top;
                                    rect12.top = i33;
                                    rect14.top = i33;
                                    rect13.top = i33;
                                    int i34 = z ? displayFrames.mRestricted.right : displayFrames.mUnrestricted.right;
                                    rect12.right = i34;
                                    rect14.right = i34;
                                    rect13.right = i34;
                                    int i35 = z ? displayFrames.mRestricted.bottom : displayFrames.mUnrestricted.bottom;
                                    rect12.bottom = i35;
                                    rect14.bottom = i35;
                                    rect13.bottom = i35;
                                    if (DEBUG_LAYOUT) {
                                        c = 3;
                                        Slog.v("WindowManager", String.format("Laying out status bar window: (%d,%d - %d,%d)", Integer.valueOf(rect13.left), Integer.valueOf(rect13.top), Integer.valueOf(rect13.right), Integer.valueOf(rect13.bottom)));
                                    }
                                    if (i25 == 0) {
                                        i14 = i7;
                                        rect15 = rect24;
                                        rect15.set(displayFrames.mRestricted);
                                    } else if (windowState.isVoiceInteraction()) {
                                        rect15 = rect24;
                                        rect15.set(displayFrames.mVoiceContent);
                                        i14 = i7;
                                    } else {
                                        rect15 = rect24;
                                        i14 = i7;
                                        if (i14 != 16) {
                                            rect15.set(displayFrames.mDock);
                                        } else {
                                            rect15.set(displayFrames.mContent);
                                        }
                                    }
                                    applyStableConstraints(i12, i13, rect15, displayFrames);
                                    if (i14 == 48) {
                                        rect16 = rect25;
                                        rect16.set(displayFrames.mCurrent);
                                    } else {
                                        rect16 = rect25;
                                        rect16.set(rect15);
                                    }
                                    rect5 = rect13;
                                    phoneWindowManager = this;
                                    i3 = i31;
                                    rect7 = rect15;
                                    layoutParams = attrs;
                                    int i36 = i13;
                                    rect6 = rect16;
                                    i4 = i36;
                                    Rect rect29 = rect12;
                                    rect8 = rect14;
                                    rect9 = rect29;
                                } else {
                                    i13 = i10;
                                    if ((33554432 & i13) != 0) {
                                        i15 = 1;
                                        if (i31 < 1 || i31 > 1999) {
                                            rect12 = rect10;
                                            rect13 = rect11;
                                            rect14 = rect22;
                                        } else {
                                            rect12 = rect10;
                                            rect12.set(displayFrames.mOverscan);
                                            rect14 = rect22;
                                            rect14.set(displayFrames.mOverscan);
                                            rect13 = rect11;
                                            rect13.set(displayFrames.mOverscan);
                                            i12 = i9;
                                        }
                                    } else {
                                        rect12 = rect10;
                                        rect13 = rect11;
                                        rect14 = rect22;
                                        i15 = 1;
                                    }
                                    if (canHideNavigationBar()) {
                                        i12 = i9;
                                        if ((i12 & 512) != 0 && ((i31 >= i15 && i31 <= 1999) || i31 == 2020)) {
                                            rect14.set(displayFrames.mOverscan);
                                            rect13.set(displayFrames.mOverscan);
                                            rect12.set(displayFrames.mUnrestricted);
                                        }
                                    } else {
                                        i12 = i9;
                                    }
                                    rect14.set(displayFrames.mRestrictedOverscan);
                                    rect13.set(displayFrames.mRestrictedOverscan);
                                    rect12.set(displayFrames.mUnrestricted);
                                }
                                c = 3;
                                if (i25 == 0) {
                                }
                                applyStableConstraints(i12, i13, rect15, displayFrames);
                                if (i14 == 48) {
                                }
                                rect5 = rect13;
                                phoneWindowManager = this;
                                i3 = i31;
                                rect7 = rect15;
                                layoutParams = attrs;
                                int i362 = i13;
                                rect6 = rect16;
                                i4 = i362;
                                Rect rect292 = rect12;
                                rect8 = rect14;
                                rect9 = rect292;
                            }
                        } else {
                            rect2 = rect27;
                            rect3 = rect26;
                            rect4 = rect28;
                            if (z2 || (impliedSysUiFlagsForLayout & 1536) != 0) {
                                rect5 = rect21;
                                rect6 = rect25;
                                rect7 = rect24;
                                rect8 = rect22;
                                rect9 = rect23;
                                if (DEBUG_LAYOUT) {
                                    Slog.v("WindowManager", "layoutWindowLw(" + ((Object) attrs.getTitle()) + "): IN_SCREEN");
                                }
                                if (i21 == 2014 || i21 == 2017) {
                                    layoutParams = attrs;
                                    i3 = i21;
                                    i4 = windowFlags;
                                    phoneWindowManager = this;
                                    rect7.set(displayFrames.mUnrestricted);
                                    rect9.set(displayFrames.mUnrestricted);
                                    rect8.set(displayFrames.mUnrestricted);
                                    rect5.set(displayFrames.mUnrestricted);
                                    if (z) {
                                        int i37 = displayFrames.mDock.left;
                                        rect7.left = i37;
                                        rect9.left = i37;
                                        rect8.left = i37;
                                        rect5.left = i37;
                                        int i38 = displayFrames.mRestricted.right;
                                        rect7.right = i38;
                                        rect9.right = i38;
                                        rect8.right = i38;
                                        rect5.right = i38;
                                        int i39 = displayFrames.mRestricted.bottom;
                                        rect7.bottom = i39;
                                        rect9.bottom = i39;
                                        rect8.bottom = i39;
                                        rect5.bottom = i39;
                                    }
                                    if (DEBUG_LAYOUT) {
                                        Slog.v("WindowManager", String.format("Laying out IN_SCREEN status bar window: (%d,%d - %d,%d)", Integer.valueOf(rect5.left), Integer.valueOf(rect5.top), Integer.valueOf(rect5.right), Integer.valueOf(rect5.bottom)));
                                    }
                                } else {
                                    if (i21 == 2019 || i21 == 2024) {
                                        layoutParams = attrs;
                                        i4 = windowFlags;
                                        phoneWindowManager = this;
                                        rect9.set(displayFrames.mUnrestricted);
                                        rect8.set(displayFrames.mUnrestricted);
                                        rect5.set(displayFrames.mUnrestricted);
                                        if (DEBUG_LAYOUT) {
                                            i3 = i21;
                                            Slog.v("WindowManager", String.format("Laying out navigation bar window: (%d,%d - %d,%d)", Integer.valueOf(rect5.left), Integer.valueOf(rect5.top), Integer.valueOf(rect5.right), Integer.valueOf(rect5.bottom)));
                                        }
                                    } else if (((i21 == 2015 || i21 == 2036) && i25 != 0) || i21 == 2021) {
                                        rect7.set(displayFrames.mOverscan);
                                        rect9.set(displayFrames.mOverscan);
                                        rect8.set(displayFrames.mOverscan);
                                        rect5.set(displayFrames.mOverscan);
                                        layoutParams = attrs;
                                        i3 = i21;
                                        i4 = windowFlags;
                                        phoneWindowManager = this;
                                    } else {
                                        i4 = windowFlags;
                                        if ((33554432 & i4) != 0) {
                                            i5 = 1;
                                            if (i21 >= 1 && i21 <= 1999) {
                                                rect7.set(displayFrames.mOverscan);
                                                rect9.set(displayFrames.mOverscan);
                                                rect8.set(displayFrames.mOverscan);
                                                rect5.set(displayFrames.mOverscan);
                                                layoutParams = attrs;
                                                i3 = i21;
                                                phoneWindowManager = this;
                                            }
                                        } else {
                                            i5 = 1;
                                        }
                                        layoutParams = attrs;
                                        phoneWindowManager = this;
                                        if (canHideNavigationBar() && (impliedSysUiFlagsForLayout & 512) != 0 && (i21 == 2000 || i21 == 2005 || i21 == 2034 || i21 == 2033 || (i21 >= i5 && i21 <= 1999))) {
                                            rect7.set(displayFrames.mUnrestricted);
                                            rect9.set(displayFrames.mUnrestricted);
                                            rect8.set(displayFrames.mUnrestricted);
                                            rect5.set(displayFrames.mUnrestricted);
                                        } else if ((impliedSysUiFlagsForLayout & 1024) != 0) {
                                            rect9.set(displayFrames.mRestricted);
                                            rect8.set(displayFrames.mRestricted);
                                            rect5.set(displayFrames.mRestricted);
                                            if (i24 != 16) {
                                                rect7.set(displayFrames.mDock);
                                            } else {
                                                rect7.set(displayFrames.mContent);
                                            }
                                        } else {
                                            rect7.set(displayFrames.mRestricted);
                                            rect9.set(displayFrames.mRestricted);
                                            rect8.set(displayFrames.mRestricted);
                                            rect5.set(displayFrames.mRestricted);
                                        }
                                    }
                                    i3 = i21;
                                }
                                phoneWindowManager.applyStableConstraints(impliedSysUiFlagsForLayout, i4, rect7, displayFrames);
                                if (i24 != 48) {
                                    rect6.set(displayFrames.mCurrent);
                                } else {
                                    rect6.set(rect7);
                                }
                            } else {
                                if (windowState2 != null) {
                                    if (DEBUG_LAYOUT) {
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("layoutWindowLw(");
                                        layoutParams2 = attrs;
                                        sb.append((Object) layoutParams2.getTitle());
                                        sb.append("): attached to ");
                                        sb.append(windowState2);
                                        Slog.v("WindowManager", sb.toString());
                                    } else {
                                        layoutParams2 = attrs;
                                    }
                                    i6 = windowFlags;
                                    rect6 = rect25;
                                    setAttachedWindowFrames(windowState, windowFlags, i24, windowState2, false, rect21, rect22, rect23, rect24, rect6, displayFrames);
                                    layoutParams = layoutParams2;
                                    i3 = i21;
                                    rect7 = rect24;
                                    rect8 = rect22;
                                    rect9 = rect23;
                                    rect5 = rect21;
                                } else {
                                    i6 = windowFlags;
                                    if (DEBUG_LAYOUT) {
                                        Slog.v("WindowManager", "layoutWindowLw(" + ((Object) attrs.getTitle()) + "): normal window");
                                    }
                                    if (i21 == 2014) {
                                        rect7 = rect24;
                                        rect7.set(displayFrames.mRestricted);
                                        rect9 = rect23;
                                        rect9.set(displayFrames.mRestricted);
                                        rect8 = rect22;
                                        rect8.set(displayFrames.mRestricted);
                                        rect5 = rect21;
                                        rect5.set(displayFrames.mRestricted);
                                        layoutParams = attrs;
                                        i3 = i21;
                                        rect6 = rect25;
                                    } else {
                                        rect7 = rect24;
                                        rect8 = rect22;
                                        rect9 = rect23;
                                        rect5 = rect21;
                                        if (i21 == 2005 || i21 == 2003) {
                                            rect6 = rect25;
                                            rect7.set(displayFrames.mStable);
                                            rect9.set(displayFrames.mStable);
                                            rect8.set(displayFrames.mStable);
                                            rect5.set(displayFrames.mStable);
                                        } else {
                                            rect5.set(displayFrames.mContent);
                                            if (windowState.isVoiceInteraction()) {
                                                rect7.set(displayFrames.mVoiceContent);
                                                rect9.set(displayFrames.mVoiceContent);
                                                rect8.set(displayFrames.mVoiceContent);
                                            } else if (i24 != 16) {
                                                rect7.set(displayFrames.mDock);
                                                rect9.set(displayFrames.mDock);
                                                rect8.set(displayFrames.mDock);
                                            } else {
                                                rect7.set(displayFrames.mContent);
                                                rect9.set(displayFrames.mContent);
                                                rect8.set(displayFrames.mContent);
                                            }
                                            if (i24 != 48) {
                                                rect6 = rect25;
                                                rect6.set(displayFrames.mCurrent);
                                            } else {
                                                rect6 = rect25;
                                                rect6.set(rect7);
                                            }
                                        }
                                        layoutParams = attrs;
                                        i3 = i21;
                                    }
                                }
                                i4 = i6;
                                phoneWindowManager = this;
                            }
                        }
                    }
                    i17 = layoutParams.layoutInDisplayCutoutMode;
                    boolean z12 = windowState2 == null && !z2;
                    boolean z13 = (systemUiVisibility & 2) == 0 || phoneWindowManager.mForceHideNavBar;
                    if (layoutParams.isFullscreen() && z2) {
                        i18 = i3;
                        i19 = 1;
                        if (i18 != 1) {
                            z3 = true;
                        }
                        if (i17 != i19) {
                            Rect rect30 = mTmpDisplayCutoutSafeExceptMaybeBarsRect;
                            rect30.set(displayFrames.mDisplayCutoutSafe);
                            if (z2 && z8 && !z7 && i17 == 0) {
                                rect30.top = Integer.MIN_VALUE;
                            }
                            if (z2 && z8 && !z13 && i17 == 0) {
                                int i40 = phoneWindowManager.mNavigationBarPosition;
                                if (i40 != 4) {
                                    switch (i40) {
                                        case 1:
                                            rect30.left = Integer.MIN_VALUE;
                                            break;
                                        case 2:
                                            rect30.right = Integer.MAX_VALUE;
                                            break;
                                    }
                                } else {
                                    rect30.bottom = Integer.MAX_VALUE;
                                }
                            }
                            if (i18 == 2011 && phoneWindowManager.mNavigationBarPosition == 4) {
                                rect30.bottom = Integer.MAX_VALUE;
                            }
                            if (z12 || z3) {
                                z6 = false;
                            } else {
                                mTmpRect.set(rect5);
                                rect5.intersectUnchecked(rect30);
                                z6 = false | (!mTmpRect.equals(rect5));
                            }
                            rect8.intersectUnchecked(rect30);
                            z4 = z6;
                        } else {
                            z4 = false;
                        }
                        rect7.intersectUnchecked(displayFrames.mDisplayCutoutSafe);
                        if ((i4 & 512) != 0 && i18 != 2010) {
                            if (!windowState.isInMultiWindowMode()) {
                                rect8.top = -10000;
                                rect8.left = -10000;
                                rect8.bottom = 10000;
                                rect8.right = 10000;
                                if (i18 != 2013) {
                                    rect6.top = -10000;
                                    rect6.left = -10000;
                                    rect7.top = -10000;
                                    rect7.left = -10000;
                                    rect9.top = -10000;
                                    rect9.left = -10000;
                                    rect6.bottom = 10000;
                                    rect6.right = 10000;
                                    rect7.bottom = 10000;
                                    rect7.right = 10000;
                                    rect9.bottom = 10000;
                                    rect9.right = 10000;
                                }
                            }
                        }
                        boolean zShouldUseOutsets = phoneWindowManager.shouldUseOutsets(layoutParams, i4);
                        if (zIsDefaultDisplay && zShouldUseOutsets) {
                            Rect rect31 = mTmpOutsetFrame;
                            z5 = z4;
                            rect31.set(rect7.left, rect7.top, rect7.right, rect7.bottom);
                            int windowOutsetBottomPx = ScreenShapeHelper.getWindowOutsetBottomPx(phoneWindowManager.mContext.getResources());
                            if (windowOutsetBottomPx > 0) {
                                int i41 = displayFrames.mRotation;
                                if (i41 == 0) {
                                    rect31.bottom += windowOutsetBottomPx;
                                } else if (i41 == 1) {
                                    rect31.right += windowOutsetBottomPx;
                                } else if (i41 == 2) {
                                    rect31.top -= windowOutsetBottomPx;
                                } else if (i41 == 3) {
                                    rect31.left -= windowOutsetBottomPx;
                                }
                                if (DEBUG_LAYOUT) {
                                    Slog.v("WindowManager", "applying bottom outset of " + windowOutsetBottomPx + " with rotation " + i41 + ", result: " + rect31);
                                }
                            }
                            rect17 = rect31;
                        } else {
                            z5 = z4;
                            rect17 = null;
                        }
                        if (DEBUG_LAYOUT) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Compute frame ");
                            sb2.append((Object) layoutParams.getTitle());
                            sb2.append(": sim=#");
                            sb2.append(Integer.toHexString(i2));
                            sb2.append(" attach=");
                            sb2.append(windowState2);
                            sb2.append(" type=");
                            sb2.append(i18);
                            sb2.append(String.format(" flags=0x%08x", Integer.valueOf(i4)));
                            sb2.append(" pf=");
                            sb2.append(rect5.toShortString());
                            sb2.append(" df=");
                            sb2.append(rect8.toShortString());
                            sb2.append(" of=");
                            sb2.append(rect9.toShortString());
                            sb2.append(" cf=");
                            sb2.append(rect7.toShortString());
                            sb2.append(" vf=");
                            sb2.append(rect6.toShortString());
                            sb2.append(" dcf=");
                            rect19 = rect2;
                            sb2.append(rect19.toShortString());
                            sb2.append(" sf=");
                            rect18 = rect4;
                            sb2.append(rect18.toShortString());
                            sb2.append(" osf=");
                            sb2.append(rect17 == null ? "null" : rect17.toShortString());
                            Slog.v("WindowManager", sb2.toString());
                        } else {
                            rect18 = rect4;
                            rect19 = rect2;
                        }
                        if (!phoneWindowManager.mWmsExt.isFullscreenSwitchSupport() || (rect20 = rect3) == null || (rect20.top == 0 && rect20.left == 0)) {
                            i20 = 2011;
                        } else {
                            int i42 = -rect20.left;
                            int i43 = -rect20.top;
                            int i44 = -rect20.right;
                            int i45 = -rect20.bottom;
                            PhoneWindowManager phoneWindowManager3 = phoneWindowManager;
                            i20 = 2011;
                            phoneWindowManager3.updateRect(displayFrames, i42, i43, i44, i45);
                        }
                        windowState.computeFrameLw(rect5, rect8, rect9, rect7, rect6, rect19, rect18, rect17, displayFrames.mDisplayCutout, z5);
                        if (i18 == i20 && windowState.isVisibleLw() && !windowState.getGivenInsetsPendingLw()) {
                            phoneWindowManager2 = this;
                            phoneWindowManager2.setLastInputMethodWindowLw(null, null);
                            phoneWindowManager2.offsetInputMethodWindowLw(windowState, displayFrames);
                        } else {
                            phoneWindowManager2 = this;
                        }
                        if (i18 == 2031 && windowState.isVisibleLw() && !windowState.getGivenInsetsPendingLw()) {
                            phoneWindowManager2.offsetVoiceInputWindowLw(windowState, displayFrames);
                            return;
                        }
                        return;
                    }
                    i18 = i3;
                    i19 = 1;
                    z3 = false;
                    if (i17 != i19) {
                    }
                    rect7.intersectUnchecked(displayFrames.mDisplayCutoutSafe);
                    if ((i4 & 512) != 0) {
                    }
                    boolean zShouldUseOutsets2 = phoneWindowManager.shouldUseOutsets(layoutParams, i4);
                    if (zIsDefaultDisplay) {
                        z5 = z4;
                        rect17 = null;
                    }
                    if (DEBUG_LAYOUT) {
                    }
                    if (phoneWindowManager.mWmsExt.isFullscreenSwitchSupport()) {
                        i20 = 2011;
                    }
                    windowState.computeFrameLw(rect5, rect8, rect9, rect7, rect6, rect19, rect18, rect17, displayFrames.mDisplayCutout, z5);
                    if (i18 == i20) {
                        phoneWindowManager2 = this;
                    }
                    if (i18 == 2031) {
                        return;
                    } else {
                        return;
                    }
                }
                i2 = i23;
                layoutWallpaper(displayFrames, rect21, rect22, rect23, rect24);
                layoutParams = attrs;
                rect2 = rect27;
                rect6 = rect25;
                phoneWindowManager = this;
                rect3 = rect26;
                rect4 = rect28;
                i4 = windowFlags;
                i3 = i21;
                rect5 = rect21;
                rect7 = rect24;
                rect8 = rect22;
                rect9 = rect23;
                i17 = layoutParams.layoutInDisplayCutoutMode;
                if (windowState2 == null) {
                }
                if ((systemUiVisibility & 2) == 0) {
                }
                if (layoutParams.isFullscreen()) {
                    i18 = i3;
                    i19 = 1;
                    z3 = false;
                }
                if (i17 != i19) {
                }
                rect7.intersectUnchecked(displayFrames.mDisplayCutoutSafe);
                if ((i4 & 512) != 0) {
                }
                boolean zShouldUseOutsets22 = phoneWindowManager.shouldUseOutsets(layoutParams, i4);
                if (zIsDefaultDisplay) {
                }
                if (DEBUG_LAYOUT) {
                }
                if (phoneWindowManager.mWmsExt.isFullscreenSwitchSupport()) {
                }
                windowState.computeFrameLw(rect5, rect8, rect9, rect7, rect6, rect19, rect18, rect17, displayFrames.mDisplayCutout, z5);
                if (i18 == i20) {
                }
                if (i18 == 2031) {
                }
            }
            i3 = i21;
            layoutParams = attrs;
            rect2 = rect27;
            rect6 = rect25;
            phoneWindowManager = this;
            rect3 = rect26;
            i2 = i23;
            rect4 = rect28;
            rect9 = rect23;
            rect5 = rect21;
            rect8 = rect22;
            i4 = windowFlags;
            rect7 = rect24;
            i17 = layoutParams.layoutInDisplayCutoutMode;
            if (windowState2 == null) {
            }
            if ((systemUiVisibility & 2) == 0) {
            }
            if (layoutParams.isFullscreen()) {
            }
            if (i17 != i19) {
            }
            rect7.intersectUnchecked(displayFrames.mDisplayCutoutSafe);
            if ((i4 & 512) != 0) {
            }
            boolean zShouldUseOutsets222 = phoneWindowManager.shouldUseOutsets(layoutParams, i4);
            if (zIsDefaultDisplay) {
            }
            if (DEBUG_LAYOUT) {
            }
            if (phoneWindowManager.mWmsExt.isFullscreenSwitchSupport()) {
            }
            windowState.computeFrameLw(rect5, rect8, rect9, rect7, rect6, rect19, rect18, rect17, displayFrames.mDisplayCutout, z5);
            if (i18 == i20) {
            }
            if (i18 == 2031) {
            }
        } else {
            i = i22;
        }
        z = false;
        int i242 = i23 & 240;
        int i252 = windowFlags & 1024;
        if (i252 == 0) {
        }
        if ((windowFlags & 256) != 256) {
        }
        if ((65536 & windowFlags) != 65536) {
        }
        rect28.set(displayFrames.mStable);
        if (i21 != 2011) {
        }
        i3 = i21;
        layoutParams = attrs;
        rect2 = rect27;
        rect6 = rect25;
        phoneWindowManager = this;
        rect3 = rect26;
        i2 = i23;
        rect4 = rect28;
        rect9 = rect23;
        rect5 = rect21;
        rect8 = rect22;
        i4 = windowFlags;
        rect7 = rect24;
        i17 = layoutParams.layoutInDisplayCutoutMode;
        if (windowState2 == null) {
        }
        if ((systemUiVisibility & 2) == 0) {
        }
        if (layoutParams.isFullscreen()) {
        }
        if (i17 != i19) {
        }
        rect7.intersectUnchecked(displayFrames.mDisplayCutoutSafe);
        if ((i4 & 512) != 0) {
        }
        boolean zShouldUseOutsets2222 = phoneWindowManager.shouldUseOutsets(layoutParams, i4);
        if (zIsDefaultDisplay) {
        }
        if (DEBUG_LAYOUT) {
        }
        if (phoneWindowManager.mWmsExt.isFullscreenSwitchSupport()) {
        }
        windowState.computeFrameLw(rect5, rect8, rect9, rect7, rect6, rect19, rect18, rect17, displayFrames.mDisplayCutout, z5);
        if (i18 == i20) {
        }
        if (i18 == 2031) {
        }
    }

    private void layoutWallpaper(DisplayFrames displayFrames, Rect rect, Rect rect2, Rect rect3, Rect rect4) {
        rect2.set(displayFrames.mOverscan);
        rect.set(displayFrames.mOverscan);
        rect4.set(displayFrames.mUnrestricted);
        rect3.set(displayFrames.mUnrestricted);
    }

    private void offsetInputMethodWindowLw(WindowManagerPolicy.WindowState windowState, DisplayFrames displayFrames) {
        int iMax = Math.max(windowState.getDisplayFrameLw().top, windowState.getContentFrameLw().top) + windowState.getGivenContentInsetsLw().top;
        displayFrames.mContent.bottom = Math.min(displayFrames.mContent.bottom, iMax);
        displayFrames.mVoiceContent.bottom = Math.min(displayFrames.mVoiceContent.bottom, iMax);
        displayFrames.mCurrent.bottom = Math.min(displayFrames.mCurrent.bottom, windowState.getVisibleFrameLw().top + windowState.getGivenVisibleInsetsLw().top);
        if (DEBUG_LAYOUT) {
            Slog.v("WindowManager", "Input method: mDockBottom=" + displayFrames.mDock.bottom + " mContentBottom=" + displayFrames.mContent.bottom + " mCurBottom=" + displayFrames.mCurrent.bottom);
        }
    }

    private void offsetVoiceInputWindowLw(WindowManagerPolicy.WindowState windowState, DisplayFrames displayFrames) {
        int iMax = Math.max(windowState.getDisplayFrameLw().top, windowState.getContentFrameLw().top) + windowState.getGivenContentInsetsLw().top;
        displayFrames.mVoiceContent.bottom = Math.min(displayFrames.mVoiceContent.bottom, iMax);
    }

    @Override
    public void finishLayoutLw() {
        if (this.mWmsExt.isFullscreenSwitchSupport()) {
            this.mWmsExt.resetSwitchFrame();
        }
    }

    @Override
    public void beginPostLayoutPolicyLw(int i, int i2) {
        this.mTopFullscreenOpaqueWindowState = null;
        this.mTopFullscreenOpaqueOrDimmingWindowState = null;
        this.mTopDockedOpaqueWindowState = null;
        this.mTopDockedOpaqueOrDimmingWindowState = null;
        this.mForceStatusBar = false;
        this.mForceStatusBarFromKeyguard = false;
        this.mForceStatusBarTransparent = false;
        this.mForcingShowNavBar = false;
        this.mForcingShowNavBarLayer = -1;
        this.mAllowLockscreenWhenOn = false;
        this.mShowingDream = false;
        this.mWindowSleepTokenNeeded = false;
        if (this.mDreamManagerInternal == null) {
            this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
        }
    }

    @Override
    public void applyPostLayoutPolicyLw(WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams, WindowManagerPolicy.WindowState windowState2, WindowManagerPolicy.WindowState windowState3) {
        boolean zCanAffectSystemUiFlags = windowState.canAffectSystemUiFlags();
        if (DEBUG_LAYOUT) {
            Slog.i("WindowManager", "Win " + windowState + ": affectsSystemUi=" + zCanAffectSystemUiFlags);
        }
        if (DEBUG_LAYOUT) {
            this.mWindowManagerDebugger.debugApplyPostLayoutPolicyLw("WindowManager", windowState, layoutParams, this.mTopFullscreenOpaqueWindowState, windowState2, windowState3, this.mDreamingLockscreen, this.mShowingDream);
        }
        applyKeyguardPolicyLw(windowState, windowState3);
        int windowFlags = PolicyControl.getWindowFlags(windowState, layoutParams);
        if (this.mTopFullscreenOpaqueWindowState == null && zCanAffectSystemUiFlags && layoutParams.type == 2011) {
            this.mForcingShowNavBar = true;
            this.mForcingShowNavBarLayer = windowState.getSurfaceLayer();
        }
        if (layoutParams.type == 2000) {
            if ((layoutParams.privateFlags & 1024) != 0) {
                this.mForceStatusBarFromKeyguard = true;
            }
            if ((layoutParams.privateFlags & 4096) != 0) {
                this.mForceStatusBarTransparent = true;
            }
        }
        boolean z = layoutParams.type >= 1 && layoutParams.type < 2000;
        int windowingMode = windowState.getWindowingMode();
        boolean z2 = windowingMode == 1 || windowingMode == 4;
        if (this.mTopFullscreenOpaqueWindowState == null && zCanAffectSystemUiFlags) {
            if ((windowFlags & 2048) != 0) {
                this.mForceStatusBar = true;
            }
            if (layoutParams.type == 2023 && (!this.mDreamingLockscreen || (windowState.isVisibleLw() && windowState.hasDrawnLw()))) {
                this.mShowingDream = true;
                z = true;
            }
            if (z && windowState2 == null && layoutParams.isFullscreen() && z2) {
                if (DEBUG_LAYOUT) {
                    Slog.v("WindowManager", "Fullscreen window: " + windowState);
                }
                this.mTopFullscreenOpaqueWindowState = windowState;
                if (this.mTopFullscreenOpaqueOrDimmingWindowState == null) {
                    this.mTopFullscreenOpaqueOrDimmingWindowState = windowState;
                }
                if ((windowFlags & 1) != 0) {
                    this.mAllowLockscreenWhenOn = true;
                }
            }
        }
        if (zCanAffectSystemUiFlags && windowState.getAttrs().type == 2031) {
            if (this.mTopFullscreenOpaqueWindowState == null) {
                this.mTopFullscreenOpaqueWindowState = windowState;
                if (this.mTopFullscreenOpaqueOrDimmingWindowState == null) {
                    this.mTopFullscreenOpaqueOrDimmingWindowState = windowState;
                }
            }
            if (this.mTopDockedOpaqueWindowState == null) {
                this.mTopDockedOpaqueWindowState = windowState;
                if (this.mTopDockedOpaqueOrDimmingWindowState == null) {
                    this.mTopDockedOpaqueOrDimmingWindowState = windowState;
                }
            }
        }
        if (this.mTopFullscreenOpaqueOrDimmingWindowState == null && zCanAffectSystemUiFlags && windowState.isDimming() && z2) {
            this.mTopFullscreenOpaqueOrDimmingWindowState = windowState;
        }
        if (this.mTopDockedOpaqueWindowState == null && zCanAffectSystemUiFlags && z && windowState2 == null && layoutParams.isFullscreen() && windowingMode == 3) {
            this.mTopDockedOpaqueWindowState = windowState;
            if (this.mTopDockedOpaqueOrDimmingWindowState == null) {
                this.mTopDockedOpaqueOrDimmingWindowState = windowState;
            }
        }
        if (this.mTopDockedOpaqueOrDimmingWindowState == null && zCanAffectSystemUiFlags && windowState.isDimming() && windowingMode == 3) {
            this.mTopDockedOpaqueOrDimmingWindowState = windowState;
        }
        if (windowState.isVisibleLw() && (layoutParams.privateFlags & DumpState.DUMP_COMPILER_STATS) != 0 && windowState.canAcquireSleepToken()) {
            this.mWindowSleepTokenNeeded = true;
        }
    }

    private void applyKeyguardPolicyLw(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2) {
        if (canBeHiddenByKeyguardLw(windowState)) {
            if (shouldBeHiddenByKeyguard(windowState, windowState2)) {
                windowState.hideLw(false);
            } else {
                windowState.showLw(false);
            }
        }
    }

    @Override
    public int finishPostLayoutPolicyLw() {
        boolean z;
        boolean z2;
        boolean z3;
        if (this.mTopFullscreenOpaqueWindowState != null) {
            this.mTopFullscreenOpaqueWindowState.getAttrs();
        }
        boolean z4 = false;
        if (!this.mShowingDream) {
            this.mDreamingLockscreen = isKeyguardShowingAndNotOccluded();
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
                StringBuilder sb = new StringBuilder();
                sb.append("force=");
                sb.append(this.mForceStatusBar);
                sb.append(" forcefkg=");
                sb.append(this.mForceStatusBarFromKeyguard);
                sb.append(" top=");
                sb.append(this.mTopFullscreenOpaqueWindowState);
                sb.append(" dream=");
                sb.append(this.mDreamManagerInternal != null ? Boolean.valueOf(this.mDreamManagerInternal.isDreaming()) : "null");
                Slog.i("WindowManager", sb.toString());
            }
            if (!((!this.mForceStatusBarTransparent || this.mForceStatusBar || this.mForceStatusBarFromKeyguard) ? false : true)) {
                this.mStatusBarController.setShowTransparent(false);
            } else if (!this.mStatusBar.isVisibleLw()) {
                this.mStatusBarController.setShowTransparent(true);
            }
            WindowManager.LayoutParams attrs = this.mStatusBar.getAttrs();
            boolean z5 = attrs.height == -1 && attrs.width == -1 && (this.mTopFullscreenOpaqueWindowState == null || this.mTopFullscreenOpaqueWindowState.getAttrs().type != 2023);
            boolean z6 = topAppHidesStatusBar();
            if (this.mDreamManagerInternal != null && this.mDreamManagerInternal.isDreaming()) {
                if (DEBUG_LAYOUT) {
                    Slog.v("WindowManager", "** HIDING status bar: dreaming");
                }
                if (this.mStatusBarController.setBarShowingLw(false)) {
                    z3 = true;
                    z2 = false;
                    z4 = z3;
                }
                z2 = false;
            } else if (this.mForceStatusBar || this.mForceStatusBarFromKeyguard || this.mForceStatusBarTransparent || z5) {
                if (DEBUG_LAYOUT) {
                    Slog.v("WindowManager", "Showing status bar: forced");
                }
                boolean z7 = this.mStatusBarController.setBarShowingLw(true);
                z2 = this.mTopIsFullscreen && this.mStatusBar.isAnimatingLw();
                if ((this.mForceStatusBarFromKeyguard || z5) && this.mStatusBarController.isTransientShowing()) {
                    this.mStatusBarController.updateVisibilityLw(false, this.mLastSystemUiFlags, this.mLastSystemUiFlags);
                }
                z4 = z7;
            } else if (this.mTopFullscreenOpaqueWindowState == null) {
                z2 = false;
            } else if (this.mStatusBarController.isTransientShowing()) {
                if (this.mStatusBarController.setBarShowingLw(true)) {
                    z4 = true;
                }
                z2 = z6;
            } else if (!z6 || this.mWindowManagerInternal.isStackVisible(5) || this.mWindowManagerInternal.isStackVisible(3)) {
                if (DEBUG_LAYOUT) {
                    Slog.v("WindowManager", "** SHOWING status bar: top is not fullscreen");
                }
                z3 = this.mStatusBarController.setBarShowingLw(true);
                z2 = z6;
                z6 = false;
                z4 = z3;
            } else {
                if (DEBUG_LAYOUT) {
                    Slog.v("WindowManager", "** HIDING status bar");
                }
                if (!this.mStatusBarController.setBarShowingLw(false)) {
                    if (DEBUG_LAYOUT) {
                        Slog.v("WindowManager", "Status bar already hiding");
                    }
                }
                z2 = z6;
            }
            this.mStatusBarController.setTopAppHidesStatusBar(z6);
            z = z4;
            z4 = z2;
        } else {
            z = false;
        }
        boolean z8 = z;
        boolean z9 = z;
        if (this.mTopIsFullscreen != z4) {
            if (!z4) {
                z8 = (z ? 1 : 0) | true;
            }
            this.mTopIsFullscreen = z4;
            z9 = z8;
        }
        int i = z9;
        if ((updateSystemUiVisibilityLw() & SYSTEM_UI_CHANGING_LAYOUT) != 0) {
            i = (z9 ? 1 : 0) | true;
        }
        if (this.mShowingDream != this.mLastShowingDream) {
            this.mLastShowingDream = this.mShowingDream;
            this.mWindowManagerFuncs.notifyShowingDreamChanged();
        }
        updateWindowSleepToken();
        updateLockScreenTimeout();
        return i;
    }

    private void updateWindowSleepToken() {
        if (this.mWindowSleepTokenNeeded && !this.mLastWindowSleepTokenNeeded) {
            this.mHandler.removeCallbacks(this.mReleaseSleepTokenRunnable);
            this.mHandler.post(this.mAcquireSleepTokenRunnable);
        } else if (!this.mWindowSleepTokenNeeded && this.mLastWindowSleepTokenNeeded) {
            this.mHandler.removeCallbacks(this.mAcquireSleepTokenRunnable);
            this.mHandler.post(this.mReleaseSleepTokenRunnable);
        }
        this.mLastWindowSleepTokenNeeded = this.mWindowSleepTokenNeeded;
    }

    private boolean topAppHidesStatusBar() {
        if (this.mTopFullscreenOpaqueWindowState == null) {
            return false;
        }
        int windowFlags = PolicyControl.getWindowFlags(null, this.mTopFullscreenOpaqueWindowState.getAttrs());
        if (localLOGV) {
            Slog.d("WindowManager", "frame: " + this.mTopFullscreenOpaqueWindowState.getFrameLw());
            Slog.d("WindowManager", "attr: " + this.mTopFullscreenOpaqueWindowState.getAttrs() + " lp.flags=0x" + Integer.toHexString(windowFlags));
        }
        return ((windowFlags & 1024) == 0 && (this.mLastSystemUiFlags & 4) == 0) ? false : true;
    }

    private boolean setKeyguardOccludedLw(boolean z, boolean z2) {
        if (DEBUG_KEYGUARD) {
            Slog.d("WindowManager", "setKeyguardOccluded occluded=" + z);
        }
        boolean z3 = this.mKeyguardOccluded;
        boolean zIsShowing = this.mKeyguardDelegate.isShowing();
        boolean z4 = z3 != z || z2;
        if (!z && z4 && zIsShowing) {
            this.mKeyguardOccluded = false;
            this.mKeyguardDelegate.setOccluded(false, true);
            if (this.mStatusBar != null) {
                this.mStatusBar.getAttrs().privateFlags |= 1024;
                if (!this.mKeyguardDelegate.hasLockscreenWallpaper()) {
                    this.mStatusBar.getAttrs().flags |= DumpState.DUMP_DEXOPT;
                }
            }
            return true;
        }
        if (z && z4 && zIsShowing) {
            this.mKeyguardOccluded = true;
            this.mKeyguardDelegate.setOccluded(true, false);
            if (this.mStatusBar != null) {
                this.mStatusBar.getAttrs().privateFlags &= -1025;
                this.mStatusBar.getAttrs().flags &= -1048577;
            }
            return true;
        }
        if (!z4) {
            return false;
        }
        this.mKeyguardOccluded = z;
        this.mKeyguardDelegate.setOccluded(z, false);
        return false;
    }

    private boolean isStatusBarKeyguard() {
        return (this.mStatusBar == null || (this.mStatusBar.getAttrs().privateFlags & 1024) == 0) ? false : true;
    }

    @Override
    public boolean allowAppAnimationsLw() {
        return !this.mShowingDream;
    }

    @Override
    public int focusChangedLw(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2) {
        this.mFocusedWindow = windowState2;
        if ((updateSystemUiVisibilityLw() & SYSTEM_UI_CHANGING_LAYOUT) != 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public void notifyLidSwitchChanged(long j, boolean z) {
        if (z == this.mLidState) {
            return;
        }
        this.mLidState = z ? 1 : 0;
        applyLidSwitchState();
        updateRotation(true);
        if (z) {
            wakeUp(SystemClock.uptimeMillis(), this.mAllowTheaterModeWakeFromLidSwitch, "android.policy:LID");
        } else if (!this.mLidControlsSleep) {
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    @Override
    public void notifyCameraLensCoverSwitchChanged(long j, boolean z) {
        Intent intent;
        if (this.mCameraLensCoverState == z) {
            return;
        }
        if (this.mCameraLensCoverState == 1 && !z) {
            if (this.mKeyguardDelegate == null ? false : this.mKeyguardDelegate.isShowing()) {
                intent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE");
            } else {
                intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            }
            wakeUp(j / 1000000, this.mAllowTheaterModeWakeFromCameraLens, "android.policy:CAMERA_COVER");
            startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        }
        this.mCameraLensCoverState = z ? 1 : 0;
    }

    void setHdmiPlugged(boolean z) {
        if (this.mHdmiPlugged != z) {
            this.mHdmiPlugged = z;
            updateRotation(true, true);
            Intent intent = new Intent("android.intent.action.HDMI_PLUGGED");
            intent.addFlags(67108864);
            intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, z);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    void initializeHdmiState() {
        int iAllowThreadDiskReadsMask = StrictMode.allowThreadDiskReadsMask();
        try {
            initializeHdmiStateInternal();
        } finally {
            StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
        }
    }

    void initializeHdmiStateInternal() {
        r1 = false;
        if (new java.io.File("/sys/devices/virtual/switch/hdmi/state").exists()) {
            r3 = "DEVPATH=/devices/virtual/switch/hdmi";
            r8.mHDMIObserver.startObserving("DEVPATH=/devices/virtual/switch/hdmi");
            r3 = new java.io.FileReader("/sys/class/switch/hdmi/state");
            r0 = new char[15];
            r4 = r3.read(r0);
            if (r4 > 1) {
                if (java.lang.Integer.parseInt(new java.lang.String(r0, 0, r4 - 1)) != 0) {
                    r1 = true;
                }
            }
            r3.close();
            while (true) {
            }
        }
        r8.mHdmiPlugged = r1 ^ true;
        setHdmiPlugged(r8.mHdmiPlugged ^ true);
        return;
    }

    @Override
    public int interceptKeyBeforeQueueing(KeyEvent keyEvent, int i) {
        int i2;
        boolean z;
        int i3;
        boolean z2;
        boolean z3;
        int i4;
        boolean z4;
        TelecomManager telecommService;
        int mode;
        if (!this.mSystemBooted) {
            return 0;
        }
        boolean z5 = (i & 536870912) != 0;
        boolean z6 = keyEvent.getAction() == 0;
        boolean zIsCanceled = keyEvent.isCanceled();
        int keyCode = keyEvent.getKeyCode();
        boolean z7 = (i & DumpState.DUMP_SERVICE_PERMISSIONS) != 0;
        boolean zIsKeyguardShowingAndNotOccluded = this.mKeyguardDelegate == null ? false : z5 ? isKeyguardShowingAndNotOccluded() : this.mKeyguardDelegate.isShowing();
        if (DEBUG_INPUT) {
            Log.d("WindowManager", "interceptKeyTq keycode=" + keyCode + " interactive=" + z5 + " keyguardActive=" + zIsKeyguardShowingAndNotOccluded + " policyFlags=" + Integer.toHexString(i));
        }
        boolean z8 = (i & 1) != 0 || keyEvent.isWakeKey();
        if (z5 || (z7 && !z8)) {
            if (z5) {
                int i5 = (keyCode != this.mPendingWakeKey || z6) ? 1 : 0;
                this.mPendingWakeKey = -1;
                i2 = i5;
            } else {
                i2 = 1;
            }
            z = false;
        } else if (z5 || !shouldDispatchInputWhenNonInteractive(keyEvent)) {
            if (z8 && (!z6 || !isWakeKeyWhenScreenOff(keyCode))) {
                z8 = false;
            }
            if (z8 && z6) {
                this.mPendingWakeKey = keyCode;
            }
            z = z8;
            i2 = 0;
        } else {
            this.mPendingWakeKey = -1;
            z = z8;
            i2 = 1;
        }
        if (isValidGlobalKey(keyCode) && this.mGlobalKeyManager.shouldHandleGlobalKey(keyCode, keyEvent)) {
            if (z) {
                wakeUp(keyEvent.getEventTime(), this.mAllowTheaterModeWakeFromKey, "android.policy:KEY");
            }
            return i2;
        }
        boolean z9 = z6 && (i & 2) != 0 && (!((keyEvent.getFlags() & 64) != 0) || this.mNavBarVirtualKeyHapticFeedbackEnabled) && keyEvent.getRepeatCount() == 0;
        WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
        if (WindowManagerDebugger.WMS_DEBUG_ENG) {
            i3 = keyCode;
            z2 = zIsCanceled;
            z3 = z5;
            this.mWindowManagerDebugger.debugInterceptKeyBeforeQueueing("WindowManager", keyCode, z5, zIsKeyguardShowingAndNotOccluded, i, z6, zIsCanceled, z, this.mScreenshotChordVolumeDownKeyTriggered, i2, z9, z7);
        } else {
            i3 = keyCode;
            z2 = zIsCanceled;
            z3 = z5;
        }
        switch (i3) {
            case 4:
                i4 = 1;
                z4 = false;
                if (z6) {
                    interceptBackKeyDown();
                } else if (interceptBackKeyUp(keyEvent)) {
                    i2 &= -2;
                }
                break;
            case 5:
                i4 = 1;
                z4 = false;
                if (z6 && (telecommService = getTelecommService()) != null && telecommService.isRinging()) {
                    Log.i("WindowManager", "interceptKeyBeforeQueueing: CALL key-down while ringing: Answer the call!");
                    telecommService.acceptRingingCall();
                    i2 &= -2;
                }
                break;
            case 6:
                boolean z10 = z2;
                i4 = 1;
                z4 = false;
                i2 &= -2;
                if (z6) {
                    TelecomManager telecommService2 = getTelecommService();
                    boolean zEndCall = telecommService2 != null ? telecommService2.endCall() : false;
                    if (z3 && !zEndCall) {
                        this.mEndCallKeyHandled = false;
                        this.mHandler.postDelayed(this.mEndCallLongPress, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                    } else {
                        this.mEndCallKeyHandled = true;
                    }
                } else if (!this.mEndCallKeyHandled) {
                    this.mHandler.removeCallbacks(this.mEndCallLongPress);
                    if (!z10 && (((this.mEndcallBehavior & 1) == 0 || !goHome()) && (this.mEndcallBehavior & 2) != 0)) {
                        goToSleep(keyEvent.getEventTime(), 4, 0);
                        z = z4;
                    }
                }
                break;
            default:
                switch (i3) {
                    case 24:
                    case 25:
                        int i6 = i3;
                        i4 = 1;
                        z4 = false;
                        if (i6 == 25) {
                            if (z6) {
                                cancelPendingRingerToggleChordAction();
                                if (z3 && !this.mScreenshotChordVolumeDownKeyTriggered && (keyEvent.getFlags() & 1024) == 0) {
                                    this.mScreenshotChordVolumeDownKeyTriggered = true;
                                    this.mScreenshotChordVolumeDownKeyTime = keyEvent.getDownTime();
                                    this.mScreenshotChordVolumeDownKeyConsumed = false;
                                    cancelPendingPowerKeyAction();
                                    interceptScreenshotChord();
                                    interceptAccessibilityShortcutChord();
                                }
                            } else {
                                this.mScreenshotChordVolumeDownKeyTriggered = false;
                                cancelPendingScreenshotChordAction();
                                cancelPendingAccessibilityShortcutAction();
                            }
                        } else if (i6 == 24) {
                            if (!z6) {
                                this.mA11yShortcutChordVolumeUpKeyTriggered = false;
                                cancelPendingScreenshotChordAction();
                                cancelPendingAccessibilityShortcutAction();
                                cancelPendingRingerToggleChordAction();
                            } else if (z3 && !this.mA11yShortcutChordVolumeUpKeyTriggered && (keyEvent.getFlags() & 1024) == 0) {
                                this.mA11yShortcutChordVolumeUpKeyTriggered = true;
                                this.mA11yShortcutChordVolumeUpKeyTime = keyEvent.getDownTime();
                                this.mA11yShortcutChordVolumeUpKeyConsumed = false;
                                cancelPendingPowerKeyAction();
                                cancelPendingScreenshotChordAction();
                                cancelPendingRingerToggleChordAction();
                                interceptAccessibilityShortcutChord();
                                interceptRingerToggleChord();
                            }
                        }
                        if (z6) {
                            sendSystemKeyToStatusBarAsync(keyEvent.getKeyCode());
                            TelecomManager telecommService3 = getTelecommService();
                            if (telecommService3 == null || this.mHandleVolumeKeysInWM || !telecommService3.isRinging()) {
                                try {
                                    mode = getAudioService().getMode();
                                } catch (Exception e) {
                                    Log.e("WindowManager", "Error getting AudioService in interceptKeyBeforeQueueing.", e);
                                    mode = 0;
                                }
                                if (((telecommService3 != null && telecommService3.isInCall()) || mode == 3) && (i2 & 1) == 0) {
                                    MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(keyEvent, Integer.MIN_VALUE, false);
                                } else if (this.mUseTvRouting || this.mHandleVolumeKeysInWM) {
                                    i2 |= 1;
                                } else if ((i2 & 1) == 0) {
                                    MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(keyEvent, Integer.MIN_VALUE, true);
                                }
                            } else {
                                Log.i("WindowManager", "interceptKeyBeforeQueueing: VOLUME key-down while ringing: Silence ringer!");
                                telecommService3.silenceRinger();
                                i2 &= -2;
                            }
                            break;
                        }
                        break;
                    case 26:
                        i4 = 1;
                        z4 = false;
                        cancelPendingAccessibilityShortcutAction();
                        i2 &= -2;
                        if (z6) {
                            interceptPowerKeyDown(keyEvent, z3);
                        } else {
                            interceptPowerKeyUp(keyEvent, z3, z2);
                        }
                        z = z4;
                        break;
                    default:
                        switch (i3) {
                            default:
                                switch (i3) {
                                    default:
                                        switch (i3) {
                                            case NetworkManagementService.NetdResponseCode.DnsProxyQueryResult:
                                                break;
                                            case NetworkManagementService.NetdResponseCode.ClatdStatusResult:
                                                i4 = 1;
                                                z4 = false;
                                                i2 &= -2;
                                                if (!this.mPowerManager.isInteractive()) {
                                                    z9 = false;
                                                }
                                                if (z6) {
                                                    sleepPress();
                                                } else {
                                                    sleepRelease(keyEvent.getEventTime());
                                                }
                                                z = z4;
                                                break;
                                            case UsbDescriptor.CLASSID_WIRELESS:
                                                i4 = 1;
                                                z4 = false;
                                                i2 &= -2;
                                                z = true;
                                                break;
                                            default:
                                                switch (i3) {
                                                    case 280:
                                                    case 281:
                                                    case 282:
                                                    case 283:
                                                        i4 = 1;
                                                        z4 = false;
                                                        i2 &= -2;
                                                        interceptSystemNavigationKey(keyEvent);
                                                        break;
                                                    default:
                                                        switch (i3) {
                                                            case HdmiCecKeycode.CEC_KEYCODE_RESERVED:
                                                            case 130:
                                                                break;
                                                            case 164:
                                                                break;
                                                            case 171:
                                                                i4 = 1;
                                                                z4 = false;
                                                                if (this.mShortPressOnWindowBehavior == 1 && this.mPictureInPictureVisible) {
                                                                    if (!z6) {
                                                                        showPictureInPictureMenu(keyEvent);
                                                                    }
                                                                    i2 &= -2;
                                                                }
                                                                break;
                                                            case 219:
                                                                i4 = 1;
                                                                boolean z11 = keyEvent.getRepeatCount() > 0;
                                                                if (z6 && z11) {
                                                                    Message messageObtainMessage = this.mHandler.obtainMessage(MSG_LAUNCH_ASSIST_LONG_PRESS);
                                                                    messageObtainMessage.setAsynchronous(true);
                                                                    messageObtainMessage.sendToTarget();
                                                                }
                                                                if (z6 || z11) {
                                                                    z4 = false;
                                                                } else {
                                                                    z4 = false;
                                                                    Message messageObtainMessage2 = this.mHandler.obtainMessage(26, keyEvent.getDeviceId(), 0, null);
                                                                    messageObtainMessage2.setAsynchronous(true);
                                                                    messageObtainMessage2.sendToTarget();
                                                                }
                                                                i2 &= -2;
                                                                break;
                                                            case 231:
                                                                if (z6) {
                                                                    i4 = 1;
                                                                } else {
                                                                    this.mBroadcastWakeLock.acquire();
                                                                    Message messageObtainMessage3 = this.mHandler.obtainMessage(12);
                                                                    i4 = 1;
                                                                    messageObtainMessage3.setAsynchronous(true);
                                                                    messageObtainMessage3.sendToTarget();
                                                                }
                                                                i2 &= -2;
                                                                z4 = false;
                                                                break;
                                                            case 276:
                                                                i2 &= -2;
                                                                if (!z6) {
                                                                    this.mPowerManagerInternal.setUserInactiveOverrideFromWindowManager();
                                                                }
                                                                i4 = 1;
                                                                z4 = false;
                                                                z = false;
                                                                break;
                                                            default:
                                                                i4 = 1;
                                                                z4 = false;
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                    case 126:
                                    case 127:
                                        i4 = 1;
                                        z4 = false;
                                        if (MediaSessionLegacyHelper.getHelper(this.mContext).isGlobalPriorityActive()) {
                                            i2 &= -2;
                                        }
                                        if ((i2 & 1) == 0) {
                                            this.mBroadcastWakeLock.acquire();
                                            Message messageObtainMessage4 = this.mHandler.obtainMessage(3, new KeyEvent(keyEvent));
                                            messageObtainMessage4.setAsynchronous(true);
                                            messageObtainMessage4.sendToTarget();
                                        }
                                        break;
                                }
                            case HdmiCecKeycode.CEC_KEYCODE_INITIAL_CONFIGURATION:
                            case HdmiCecKeycode.CEC_KEYCODE_SELECT_BROADCAST_TYPE:
                            case HdmiCecKeycode.CEC_KEYCODE_SELECT_SOUND_PRESENTATION:
                            case 88:
                            case 89:
                            case 90:
                            case 91:
                                break;
                        }
                        break;
                }
                break;
        }
        if (z9) {
            performHapticFeedbackLw(null, i4, z4);
        }
        if (z) {
            wakeUp(keyEvent.getEventTime(), this.mAllowTheaterModeWakeFromKey, "android.policy:KEY");
        }
        return i2;
    }

    private void interceptSystemNavigationKey(KeyEvent keyEvent) {
        if (keyEvent.getAction() == 1) {
            if ((!this.mAccessibilityManager.isEnabled() || !this.mAccessibilityManager.sendFingerprintGesture(keyEvent.getKeyCode())) && this.mSystemNavigationKeysEnabled) {
                sendSystemKeyToStatusBarAsync(keyEvent.getKeyCode());
            }
        }
    }

    private void sendSystemKeyToStatusBar(int i) {
        IStatusBarService statusBarService = getStatusBarService();
        if (statusBarService != null) {
            try {
                statusBarService.handleSystemKey(i);
            } catch (RemoteException e) {
            }
        }
    }

    private void sendSystemKeyToStatusBarAsync(int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(24, i, 0);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void sendProposedRotationChangeToStatusBarInternal(int i, boolean z) {
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.onProposedRotationChanged(i, z);
        }
    }

    private static boolean isValidGlobalKey(int i) {
        if (i != 26) {
            switch (i) {
                case NetworkManagementService.NetdResponseCode.ClatdStatusResult:
                case UsbDescriptor.CLASSID_WIRELESS:
                    return false;
                default:
                    return true;
            }
        }
        return false;
    }

    private boolean isWakeKeyWhenScreenOff(int r4) {
        if (r4 != com.android.server.policy.PhoneWindowManager.MSG_LAUNCH_ASSIST_LONG_PRESS && r4 != 79 && r4 != 130) {
            if (r4 != 164) {
                if (r4 != 222) {
                    switch (r4) {
                        case 24:
                        case 25:
                        default:
                            switch (r4) {
                                default:
                                    switch (r4) {
                                    }
                                    return true;
                                case com.android.server.hdmi.HdmiCecKeycode.CEC_KEYCODE_INITIAL_CONFIGURATION:
                                case com.android.server.hdmi.HdmiCecKeycode.CEC_KEYCODE_SELECT_BROADCAST_TYPE:
                                case com.android.server.hdmi.HdmiCecKeycode.CEC_KEYCODE_SELECT_SOUND_PRESENTATION:
                                case 88:
                                case 89:
                                case 90:
                                case 91:
                                    return false;
                            }
                    }
                }
            }
            if (r3.mDockMode != 0) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public int interceptMotionBeforeQueueingNonInteractive(long j, int i) {
        int i2 = i & 1;
        if (i2 != 0 && wakeUp(j / 1000000, this.mAllowTheaterModeWakeFromMotion, "android.policy:MOTION")) {
            return 0;
        }
        if (shouldDispatchInputWhenNonInteractive(null)) {
            return 1;
        }
        if (isTheaterModeEnabled() && i2 != 0) {
            wakeUp(j / 1000000, this.mAllowTheaterModeWakeFromMotionWhenNotDreaming, "android.policy:MOTION");
        }
        return 0;
    }

    private boolean shouldDispatchInputWhenNonInteractive(KeyEvent keyEvent) {
        IDreamManager dreamManager;
        boolean z = this.mDisplay == null || this.mDisplay.getState() == 1;
        if (z && !this.mHasFeatureWatch) {
            return false;
        }
        if (isKeyguardShowingAndNotOccluded() && !z) {
            return true;
        }
        if ((!this.mHasFeatureWatch || keyEvent == null || (keyEvent.getKeyCode() != 4 && keyEvent.getKeyCode() != 264)) && (dreamManager = getDreamManager()) != null) {
            try {
                if (dreamManager.isDreaming()) {
                    return true;
                }
            } catch (RemoteException e) {
                Slog.e("WindowManager", "RemoteException when checking if dreaming", e);
            }
        }
        return false;
    }

    private void dispatchDirectAudioEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() != 0) {
            return;
        }
        int keyCode = keyEvent.getKeyCode();
        String opPackageName = this.mContext.getOpPackageName();
        if (keyCode != 164) {
            switch (keyCode) {
                case 24:
                    try {
                        getAudioService().adjustSuggestedStreamVolume(1, Integer.MIN_VALUE, 4101, opPackageName, "WindowManager");
                    } catch (Exception e) {
                        Log.e("WindowManager", "Error dispatching volume up in dispatchTvAudioEvent.", e);
                        return;
                    }
                    break;
                case 25:
                    try {
                        getAudioService().adjustSuggestedStreamVolume(-1, Integer.MIN_VALUE, 4101, opPackageName, "WindowManager");
                    } catch (Exception e2) {
                        Log.e("WindowManager", "Error dispatching volume down in dispatchTvAudioEvent.", e2);
                        return;
                    }
                    break;
            }
            return;
        }
        try {
            if (keyEvent.getRepeatCount() == 0) {
                getAudioService().adjustSuggestedStreamVolume(101, Integer.MIN_VALUE, 4101, opPackageName, "WindowManager");
            }
        } catch (Exception e3) {
            Log.e("WindowManager", "Error dispatching mute in dispatchTvAudioEvent.", e3);
        }
    }

    void dispatchMediaKeyWithWakeLock(KeyEvent keyEvent) {
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "dispatchMediaKeyWithWakeLock: " + keyEvent);
        }
        if (this.mHavePendingMediaKeyRepeatWithWakeLock) {
            if (DEBUG_INPUT) {
                Slog.d("WindowManager", "dispatchMediaKeyWithWakeLock: canceled repeat");
            }
            this.mHandler.removeMessages(4);
            this.mHavePendingMediaKeyRepeatWithWakeLock = false;
            this.mBroadcastWakeLock.release();
        }
        dispatchMediaKeyWithWakeLockToAudioService(keyEvent);
        if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
            this.mHavePendingMediaKeyRepeatWithWakeLock = true;
            Message messageObtainMessage = this.mHandler.obtainMessage(4, keyEvent);
            messageObtainMessage.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(messageObtainMessage, ViewConfiguration.getKeyRepeatTimeout());
            return;
        }
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyRepeatWithWakeLock(KeyEvent keyEvent) {
        this.mHavePendingMediaKeyRepeatWithWakeLock = false;
        KeyEvent keyEventChangeTimeRepeat = KeyEvent.changeTimeRepeat(keyEvent, SystemClock.uptimeMillis(), 1, keyEvent.getFlags() | 128);
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "dispatchMediaKeyRepeatWithWakeLock: " + keyEventChangeTimeRepeat);
        }
        dispatchMediaKeyWithWakeLockToAudioService(keyEventChangeTimeRepeat);
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyWithWakeLockToAudioService(KeyEvent keyEvent) {
        if (this.mActivityManagerInternal.isSystemReady()) {
            MediaSessionLegacyHelper.getHelper(this.mContext).sendMediaButtonEvent(keyEvent, true);
        }
    }

    void launchVoiceAssistWithWakeLock() {
        Intent intent;
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        if (!keyguardOn()) {
            intent = new Intent("android.speech.action.WEB_SEARCH");
        } else {
            IDeviceIdleController iDeviceIdleControllerAsInterface = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
            if (iDeviceIdleControllerAsInterface != null) {
                try {
                    iDeviceIdleControllerAsInterface.exitIdle("voice-search");
                } catch (RemoteException e) {
                }
            }
            intent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
            intent.putExtra("android.speech.extras.EXTRA_SECURE", true);
        }
        startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        this.mBroadcastWakeLock.release();
    }

    private void requestTransientBars(WindowManagerPolicy.WindowState windowState) {
        synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            if (isUserSetupComplete()) {
                boolean zCheckShowTransientBarLw = this.mStatusBarController.checkShowTransientBarLw();
                boolean z = this.mNavigationBarController.checkShowTransientBarLw() && !isNavBarEmpty(this.mLastSystemUiFlags);
                if (zCheckShowTransientBarLw || z) {
                    if (!z && windowState == this.mNavigationBar) {
                        if (DEBUG) {
                            Slog.d("WindowManager", "Not showing transient bar, wrong swipe target");
                        }
                        return;
                    }
                    if (zCheckShowTransientBarLw) {
                        this.mStatusBarController.showTransient();
                    }
                    if (z) {
                        this.mNavigationBarController.showTransient();
                    }
                    this.mImmersiveModeConfirmation.confirmCurrentPrompt();
                    updateSystemUiVisibilityLw();
                }
            }
        }
    }

    @Override
    public void startedGoingToSleep(int i) {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Started going to sleep... (why=" + i + ")");
        }
        this.mGoingToSleep = true;
        this.mRequestedOrGoingToSleep = true;
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onStartedGoingToSleep(i);
        }
    }

    @Override
    public void finishedGoingToSleep(int i) {
        EventLog.writeEvent(70000, 0);
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Finished going to sleep... (why=" + i + ")");
        }
        MetricsLogger.histogram(this.mContext, "screen_timeout", this.mLockScreenTimeout / 1000);
        this.mGoingToSleep = false;
        this.mRequestedOrGoingToSleep = false;
        synchronized (this.mLock) {
            this.mAwake = false;
            updateWakeGestureListenerLp();
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onFinishedGoingToSleep(i, this.mCameraGestureTriggeredDuringGoingToSleep);
        }
        this.mCameraGestureTriggeredDuringGoingToSleep = false;
    }

    @Override
    public void startedWakingUp() {
        EventLog.writeEvent(70000, 1);
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Started waking up...");
        }
        synchronized (this.mLock) {
            this.mAwake = true;
            updateWakeGestureListenerLp();
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onStartedWakingUp();
        }
    }

    @Override
    public void finishedWakingUp() {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Finished waking up...");
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onFinishedWakingUp();
        }
    }

    private void wakeUpFromPowerKey(long j) {
        wakeUp(j, this.mAllowTheaterModeWakeFromPowerKey, "android.policy:POWER");
    }

    private boolean wakeUp(long j, boolean z, String str) {
        boolean zIsTheaterModeEnabled = isTheaterModeEnabled();
        if (!z && zIsTheaterModeEnabled) {
            return false;
        }
        if (zIsTheaterModeEnabled) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 0);
        }
        this.mPowerManager.wakeUp(j, str);
        return true;
    }

    private void finishKeyguardDrawn() {
        synchronized (this.mLock) {
            if (this.mScreenOnEarly && !this.mKeyguardDrawComplete) {
                this.mKeyguardDrawComplete = true;
                if (this.mKeyguardDelegate != null) {
                    this.mHandler.removeMessages(6);
                }
                this.mWindowManagerDrawComplete = false;
                this.mWindowManagerInternal.waitForAllWindowsDrawn(this.mWindowManagerDrawCallback, 1000L);
            }
        }
    }

    @Override
    public void screenTurnedOff() {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Screen turned off...");
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
        reportScreenStateToVrManager(false);
    }

    private long getKeyguardDrawnTimeout() {
        return ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).isBootCompleted() ? 1000L : 5000L;
    }

    @Override
    public void screenTurningOn(WindowManagerPolicy.ScreenOnListener screenOnListener) {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Screen turning on...");
        }
        updateScreenOffSleepToken(false);
        synchronized (this.mLock) {
            this.mScreenOnEarly = true;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = screenOnListener;
            if (this.mKeyguardDelegate != null && this.mKeyguardDelegate.hasKeyguard()) {
                this.mHandler.removeMessages(6);
                this.mHandler.sendEmptyMessageDelayed(6, getKeyguardDrawnTimeout());
                this.mKeyguardDelegate.onScreenTurningOn(this.mKeyguardDrawnCallback);
            } else {
                if (DEBUG_WAKEUP) {
                    Slog.d("WindowManager", "null mKeyguardDelegate: setting mKeyguardDrawComplete.");
                }
                finishKeyguardDrawn();
            }
        }
    }

    @Override
    public void screenTurnedOn() {
        synchronized (this.mLock) {
            if (this.mKeyguardDelegate != null) {
                this.mKeyguardDelegate.onScreenTurnedOn();
            }
        }
        reportScreenStateToVrManager(true);
    }

    @Override
    public void screenTurningOff(WindowManagerPolicy.ScreenOffListener screenOffListener) {
        this.mWindowManagerFuncs.screenTurningOff(screenOffListener);
        synchronized (this.mLock) {
            if (this.mKeyguardDelegate != null) {
                this.mKeyguardDelegate.onScreenTurningOff();
            }
        }
    }

    private void reportScreenStateToVrManager(boolean z) {
        if (this.mVrManagerInternal == null) {
            return;
        }
        this.mVrManagerInternal.onScreenStateChanged(z);
    }

    private void finishWindowsDrawn() {
        synchronized (this.mLock) {
            if (this.mScreenOnEarly && !this.mWindowManagerDrawComplete) {
                this.mWindowManagerDrawComplete = true;
                finishScreenTurningOn();
            }
        }
    }

    private void finishScreenTurningOn() {
        synchronized (this.mLock) {
            updateOrientationListenerLp();
        }
        synchronized (this.mLock) {
            if (DEBUG_WAKEUP) {
                Slog.d("WindowManager", "finishScreenTurningOn: mAwake=" + this.mAwake + ", mScreenOnEarly=" + this.mScreenOnEarly + ", mScreenOnFully=" + this.mScreenOnFully + ", mKeyguardDrawComplete=" + this.mKeyguardDrawComplete + ", mWindowManagerDrawComplete=" + this.mWindowManagerDrawComplete);
            }
            if (!this.mScreenOnFully && this.mScreenOnEarly && this.mWindowManagerDrawComplete && (!this.mAwake || this.mKeyguardDrawComplete)) {
                if (DEBUG_WAKEUP) {
                    Slog.i("WindowManager", "Finished screen turning on...");
                }
                WindowManagerPolicy.ScreenOnListener screenOnListener = this.mScreenOnListener;
                this.mScreenOnListener = null;
                boolean z = true;
                this.mScreenOnFully = true;
                if (!this.mKeyguardDrawnOnce && this.mAwake) {
                    this.mKeyguardDrawnOnce = true;
                    if (this.mBootMessageNeedsHiding) {
                        this.mBootMessageNeedsHiding = false;
                        hideBootMessages();
                    }
                } else {
                    z = false;
                }
                if (screenOnListener != null) {
                    screenOnListener.onScreenOn();
                }
                if (z) {
                    try {
                        this.mWindowManager.enableScreenIfNeeded();
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    private void handleHideBootMessage() {
        synchronized (this.mLock) {
            if (!this.mKeyguardDrawnOnce) {
                this.mBootMessageNeedsHiding = true;
            } else if (this.mBootMsgDialog != null) {
                if (DEBUG_WAKEUP) {
                    Slog.d("WindowManager", "handleHideBootMessage: dismissing");
                }
                this.mBootMsgDialog.dismiss();
                this.mBootMsgDialog = null;
            }
        }
    }

    @Override
    public boolean isScreenOn() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mScreenOnEarly;
        }
        return z;
    }

    @Override
    public boolean okToAnimate() {
        return this.mAwake && !this.mGoingToSleep;
    }

    @Override
    public void enableKeyguard(boolean z) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.setKeyguardEnabled(z);
        }
    }

    @Override
    public void exitKeyguardSecurely(WindowManagerPolicy.OnKeyguardExitResult onKeyguardExitResult) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.verifyUnlock(onKeyguardExitResult);
        }
    }

    @Override
    public boolean isKeyguardShowingAndNotOccluded() {
        return (this.mKeyguardDelegate == null || !this.mKeyguardDelegate.isShowing() || this.mKeyguardOccluded) ? false : true;
    }

    @Override
    public boolean isKeyguardTrustedLw() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isTrusted();
    }

    @Override
    public boolean isKeyguardLocked() {
        return keyguardOn();
    }

    @Override
    public boolean isKeyguardSecure(int i) {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isSecure(i);
    }

    @Override
    public boolean isKeyguardOccluded() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardOccluded;
    }

    @Override
    public boolean inKeyguardRestrictedKeyInputMode() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isInputRestricted();
    }

    @Override
    public void dismissKeyguardLw(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
        if (this.mKeyguardDelegate != null && this.mKeyguardDelegate.isShowing()) {
            if (DEBUG_KEYGUARD) {
                Slog.d("WindowManager", "PWM.dismissKeyguardLw");
            }
            this.mKeyguardDelegate.dismiss(iKeyguardDismissCallback, charSequence);
        } else if (iKeyguardDismissCallback != null) {
            try {
                iKeyguardDismissCallback.onDismissError();
            } catch (RemoteException e) {
                Slog.w("WindowManager", "Failed to call callback", e);
            }
        }
    }

    @Override
    public boolean isKeyguardDrawnLw() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mKeyguardDrawnOnce;
        }
        return z;
    }

    @Override
    public boolean isShowingDreamLw() {
        return this.mShowingDream;
    }

    @Override
    public void startKeyguardExitAnimation(long j, long j2) {
        if (this.mKeyguardDelegate != null) {
            if (DEBUG_KEYGUARD) {
                Slog.d("WindowManager", "PWM.startKeyguardExitAnimation");
            }
            this.mKeyguardDelegate.startKeyguardExitAnimation(j, j2);
        }
    }

    @Override
    public void getStableInsetsLw(int i, int i2, int i3, DisplayCutout displayCutout, Rect rect) {
        rect.setEmpty();
        getNonDecorInsetsLw(i, i2, i3, displayCutout, rect);
        rect.top = Math.max(rect.top, this.mStatusBarHeightForRotation[i]);
    }

    @Override
    public void getNonDecorInsetsLw(int i, int i2, int i3, DisplayCutout displayCutout, Rect rect) {
        rect.setEmpty();
        if (this.mHasNavigationBar) {
            int iNavigationBarPosition = navigationBarPosition(i2, i3, i);
            if (iNavigationBarPosition == 4) {
                rect.bottom = getNavigationBarHeight(i, this.mUiMode);
            } else if (iNavigationBarPosition == 2) {
                rect.right = getNavigationBarWidth(i, this.mUiMode);
            } else if (iNavigationBarPosition == 1) {
                rect.left = getNavigationBarWidth(i, this.mUiMode);
            }
        }
        if (displayCutout != null) {
            rect.left += displayCutout.getSafeInsetLeft();
            rect.top += displayCutout.getSafeInsetTop();
            rect.right += displayCutout.getSafeInsetRight();
            rect.bottom += displayCutout.getSafeInsetBottom();
        }
    }

    @Override
    public boolean isNavBarForcedShownLw(WindowManagerPolicy.WindowState windowState) {
        return this.mForceShowSystemBars;
    }

    @Override
    public int getNavBarPosition() {
        return this.mNavigationBarPosition;
    }

    @Override
    public boolean isDockSideAllowed(int i, int i2, int i3, int i4, int i5) {
        return isDockSideAllowed(i, i2, navigationBarPosition(i3, i4, i5), this.mNavigationBarCanMove);
    }

    @VisibleForTesting
    static boolean isDockSideAllowed(int i, int i2, int i3, boolean z) {
        if (i == 2) {
            return true;
        }
        if (z) {
            if (i == 1 && i3 == 2) {
                return true;
            }
            return i == 3 && i3 == 1;
        }
        if (i == i2) {
            return true;
        }
        return i == 1 && i2 == 2;
    }

    void sendCloseSystemWindows() {
        PhoneWindow.sendCloseSystemWindows(this.mContext, (String) null);
    }

    void sendCloseSystemWindows(String str) {
        PhoneWindow.sendCloseSystemWindows(this.mContext, str);
    }

    @Override
    public int rotationForOrientationLw(int i, int i2, boolean z) {
        int i3;
        if (this.mForceDefaultOrientation) {
            return 0;
        }
        synchronized (this.mLock) {
            int proposedRotation = this.mOrientationListener.getProposedRotation();
            if (proposedRotation < 0) {
                proposedRotation = i2;
            }
            if (!z) {
                i3 = 0;
            } else if (this.mLidState == 1 && this.mLidOpenRotation >= 0) {
                i3 = this.mLidOpenRotation;
            } else if (this.mDockMode != 2 || (!this.mCarDockEnablesAccelerometer && this.mCarDockRotation < 0)) {
                if ((this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == 4) && (this.mDeskDockEnablesAccelerometer || this.mDeskDockRotation >= 0)) {
                    i3 = this.mDeskDockEnablesAccelerometer ? proposedRotation : this.mDeskDockRotation;
                } else if (this.mHdmiPlugged && this.mDemoHdmiRotationLock) {
                    i3 = this.mDemoHdmiRotation;
                } else if (this.mHdmiPlugged && this.mDockMode == 0 && this.mUndockedHdmiRotation >= 0) {
                    i3 = this.mUndockedHdmiRotation;
                } else if (this.mDemoRotationLock) {
                    i3 = this.mDemoRotation;
                } else if (this.mPersistentVrModeEnabled) {
                    i3 = this.mPortraitRotation;
                } else {
                    if (i != 14) {
                        if (this.mSupportAutoRotation) {
                            if ((this.mUserRotationMode != 0 || (i != 2 && i != -1 && i != 11 && i != 12 && i != 13)) && i != 4 && i != 10 && i != 6 && i != 7) {
                                i3 = (this.mUserRotationMode != 1 || i == 5) ? -1 : this.mUserRotation;
                            }
                            if (this.mAllowAllRotations < 0) {
                                this.mAllowAllRotations = this.mContext.getResources().getBoolean(R.^attr-private.__removed6) ? 1 : 0;
                            }
                            if (proposedRotation != 2 || this.mAllowAllRotations == 1 || i == 10 || i == 13) {
                            }
                        }
                    }
                    i3 = i2;
                }
            } else if (!this.mCarDockEnablesAccelerometer) {
                i3 = this.mCarDockRotation;
            }
            switch (i) {
                case 0:
                    if (isLandscapeOrSeascape(i3)) {
                        return i3;
                    }
                    return this.mLandscapeRotation;
                case 1:
                    if (isAnyPortrait(i3)) {
                        return i3;
                    }
                    return this.mPortraitRotation;
                case 2:
                case 3:
                case 4:
                case 5:
                case 10:
                default:
                    if (i3 >= 0) {
                        return i3;
                    }
                    return 0;
                case 6:
                case 11:
                    if (isLandscapeOrSeascape(i3)) {
                        return i3;
                    }
                    if (isLandscapeOrSeascape(i2)) {
                        return i2;
                    }
                    return this.mLandscapeRotation;
                case 7:
                case 12:
                    if (isAnyPortrait(i3)) {
                        return i3;
                    }
                    if (isAnyPortrait(i2)) {
                        return i2;
                    }
                    return this.mPortraitRotation;
                case 8:
                    if (isLandscapeOrSeascape(i3)) {
                        return i3;
                    }
                    return this.mSeascapeRotation;
                case 9:
                    if (isAnyPortrait(i3)) {
                        return i3;
                    }
                    return this.mUpsideDownRotation;
            }
        }
    }

    @Override
    public boolean rotationHasCompatibleMetricsLw(int i, int i2) {
        switch (i) {
            case 0:
                return isLandscapeOrSeascape(i2);
            case 1:
                return isAnyPortrait(i2);
            default:
                switch (i) {
                    case 6:
                    case 8:
                        break;
                    case 7:
                    case 9:
                        break;
                    default:
                        return true;
                }
                break;
        }
    }

    @Override
    public void setRotationLw(int i) {
        this.mOrientationListener.setCurrentRotation(i);
    }

    public boolean isRotationChoicePossible(int i) {
        if (this.mUserRotationMode != 1 || this.mForceDefaultOrientation) {
            return false;
        }
        if (this.mLidState == 1 && this.mLidOpenRotation >= 0) {
            return false;
        }
        if (this.mDockMode == 2 && !this.mCarDockEnablesAccelerometer) {
            return false;
        }
        if ((this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == 4) && !this.mDeskDockEnablesAccelerometer) {
            return false;
        }
        if (this.mHdmiPlugged && this.mDemoHdmiRotationLock) {
            return false;
        }
        if ((this.mHdmiPlugged && this.mDockMode == 0 && this.mUndockedHdmiRotation >= 0) || this.mDemoRotationLock || this.mPersistentVrModeEnabled || !this.mSupportAutoRotation) {
            return false;
        }
        if (i != -1 && i != 2) {
            switch (i) {
            }
            return false;
        }
        return true;
    }

    public boolean isValidRotationChoice(int i, int i2) {
        if (i == -1 || i == 2) {
            return i2 >= 0 && i2 != this.mUpsideDownRotation;
        }
        switch (i) {
            case 12:
                if (i2 != this.mPortraitRotation) {
                    break;
                }
                break;
            case 13:
                if (i2 < 0) {
                    break;
                }
                break;
        }
        return false;
    }

    private boolean isLandscapeOrSeascape(int i) {
        return i == this.mLandscapeRotation || i == this.mSeascapeRotation;
    }

    private boolean isAnyPortrait(int i) {
        return i == this.mPortraitRotation || i == this.mUpsideDownRotation;
    }

    @Override
    public int getUserRotationMode() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 0, -2) != 0 ? 0 : 1;
    }

    @Override
    public void setUserRotationMode(int i, int i2) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (i != 1) {
            Settings.System.putIntForUser(contentResolver, "accelerometer_rotation", 1, -2);
        } else {
            Settings.System.putIntForUser(contentResolver, "user_rotation", i2, -2);
            Settings.System.putIntForUser(contentResolver, "accelerometer_rotation", 0, -2);
        }
    }

    @Override
    public void setSafeMode(boolean z) {
        this.mSafeMode = z;
        if (z) {
            performHapticFeedbackLw(null, 10001, true);
        }
    }

    static long[] getLongIntArray(Resources resources, int i) {
        return ArrayUtils.convertToLongArray(resources.getIntArray(i));
    }

    private void bindKeyguard() {
        synchronized (this.mLock) {
            if (this.mKeyguardBound) {
                return;
            }
            this.mKeyguardBound = true;
            this.mKeyguardDelegate.bindService(this.mContext);
        }
    }

    @Override
    public void onSystemUiStarted() {
        bindKeyguard();
    }

    @Override
    public void systemReady() {
        this.mKeyguardDelegate.onSystemReady();
        this.mVrManagerInternal = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        if (this.mVrManagerInternal != null) {
            this.mVrManagerInternal.addPersistentVrModeStateListener(this.mPersistentVrModeListener);
        }
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
            if (this.mSystemBooted) {
                this.mKeyguardDelegate.onBootCompleted();
            }
        }
        this.mSystemGestures.systemReady();
        this.mImmersiveModeConfirmation.systemReady();
        this.mAutofillManagerInternal = (AutofillManagerInternal) LocalServices.getService(AutofillManagerInternal.class);
    }

    @Override
    public void systemBooted() {
        bindKeyguard();
        synchronized (this.mLock) {
            this.mSystemBooted = true;
            if (this.mSystemReady) {
                this.mKeyguardDelegate.onBootCompleted();
            }
        }
        startedWakingUp();
        screenTurningOn(null);
        screenTurnedOn();
    }

    @Override
    public boolean canDismissBootAnimation() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mKeyguardDrawComplete;
        }
        return z;
    }

    @Override
    public void showBootMessage(final CharSequence charSequence, boolean z) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int i;
                if (PhoneWindowManager.this.mBootMsgDialog == null) {
                    if (PhoneWindowManager.this.mContext.getPackageManager().hasSystemFeature("android.software.leanback")) {
                        i = R.style.TextAppearance.StatusBar.Ticker;
                    } else {
                        i = 0;
                    }
                    PhoneWindowManager.this.mBootMsgDialog = new ProgressDialog(PhoneWindowManager.this.mContext, i) {
                        @Override
                        public boolean dispatchKeyEvent(KeyEvent keyEvent) {
                            return true;
                        }

                        @Override
                        public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
                            return true;
                        }

                        @Override
                        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
                            return true;
                        }

                        @Override
                        public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
                            return true;
                        }

                        @Override
                        public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
                            return true;
                        }

                        @Override
                        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
                            return true;
                        }
                    };
                    if (PhoneWindowManager.this.mContext.getPackageManager().isUpgrade()) {
                        PhoneWindowManager.this.mBootMsgDialog.setTitle(R.string.PERSOSUBSTATE_RUIM_NETWORK1_PUK_IN_PROGRESS);
                    } else {
                        PhoneWindowManager.this.mBootMsgDialog.setTitle(R.string.PERSOSUBSTATE_RUIM_HRPD_PUK_SUCCESS);
                    }
                    PhoneWindowManager.this.mBootMsgDialog.setProgressStyle(0);
                    PhoneWindowManager.this.mBootMsgDialog.setIndeterminate(true);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setType(2021);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().addFlags(258);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setDimAmount(1.0f);
                    WindowManager.LayoutParams attributes = PhoneWindowManager.this.mBootMsgDialog.getWindow().getAttributes();
                    attributes.screenOrientation = 5;
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setAttributes(attributes);
                    PhoneWindowManager.this.mBootMsgDialog.setCancelable(false);
                    PhoneWindowManager.this.mBootMsgDialog.show();
                }
                PhoneWindowManager.this.mBootMsgDialog.setMessage(charSequence);
            }
        });
    }

    @Override
    public void hideBootMessages() {
        this.mHandler.sendEmptyMessage(11);
    }

    @Override
    public void requestUserActivityNotification() {
        if (!this.mNotifyUserActivity && !this.mHandler.hasMessages(29)) {
            this.mNotifyUserActivity = true;
        }
    }

    @Override
    public void userActivity() {
        synchronized (this.mScreenLockTimeout) {
            if (this.mLockScreenTimerActive) {
                this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                this.mHandler.postDelayed(this.mScreenLockTimeout, this.mLockScreenTimeout);
            }
        }
        if (this.mAwake && this.mNotifyUserActivity) {
            this.mHandler.sendEmptyMessageDelayed(29, 200L);
            this.mNotifyUserActivity = false;
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
                    Log.v("WindowManager", "mScreenLockTimeout activating keyguard");
                }
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.doKeyguardTimeout(this.options);
                }
                PhoneWindowManager.this.mLockScreenTimerActive = false;
                this.options = null;
            }
        }

        public void setLockOptions(Bundle bundle) {
            this.options = bundle;
        }
    }

    @Override
    public void lockNow(Bundle bundle) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
        this.mHandler.removeCallbacks(this.mScreenLockTimeout);
        if (bundle != null) {
            this.mScreenLockTimeout.setLockOptions(bundle);
        }
        this.mHandler.post(this.mScreenLockTimeout);
    }

    private void updateLockScreenTimeout() {
        synchronized (this.mScreenLockTimeout) {
            boolean z = this.mAllowLockscreenWhenOn && this.mAwake && this.mKeyguardDelegate != null && this.mKeyguardDelegate.isSecure(this.mCurrentUserId);
            if (this.mLockScreenTimerActive != z) {
                if (z) {
                    if (localLOGV) {
                        Log.v("WindowManager", "setting lockscreen timer");
                    }
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                    this.mHandler.postDelayed(this.mScreenLockTimeout, this.mLockScreenTimeout);
                } else {
                    if (localLOGV) {
                        Log.v("WindowManager", "clearing lockscreen timer");
                    }
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                }
                this.mLockScreenTimerActive = z;
            }
        }
    }

    private void updateDreamingSleepToken(boolean z) {
        if (z) {
            if (this.mDreamingSleepToken == null) {
                this.mDreamingSleepToken = this.mActivityManagerInternal.acquireSleepToken("Dream", 0);
            }
        } else if (this.mDreamingSleepToken != null) {
            this.mDreamingSleepToken.release();
            this.mDreamingSleepToken = null;
        }
    }

    private void updateScreenOffSleepToken(boolean z) {
        if (z) {
            if (this.mScreenOffSleepToken == null) {
                this.mScreenOffSleepToken = this.mActivityManagerInternal.acquireSleepToken("ScreenOff", 0);
            }
        } else if (this.mScreenOffSleepToken != null) {
            this.mScreenOffSleepToken.release();
            this.mScreenOffSleepToken = null;
        }
    }

    @Override
    public void enableScreenAfterBoot() {
        readLidState();
        applyLidSwitchState();
        updateRotation(true);
    }

    private void applyLidSwitchState() {
        if (this.mLidState == 0 && this.mLidControlsSleep) {
            goToSleep(SystemClock.uptimeMillis(), 3, 1);
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

    void updateRotation(boolean z) {
        try {
            this.mWindowManager.updateRotation(z, false);
        } catch (RemoteException e) {
        }
    }

    void updateRotation(boolean z, boolean z2) {
        try {
            this.mWindowManager.updateRotation(z, z2);
        } catch (RemoteException e) {
        }
    }

    Intent createHomeDockIntent() {
        Intent intent;
        ActivityInfo activityInfo;
        if (this.mUiMode == 3) {
            if (this.mEnableCarDockHomeCapture) {
                intent = this.mCarDockIntent;
            } else {
                intent = null;
            }
        } else if (this.mUiMode != 2) {
            if (this.mUiMode == 6 && (this.mDockMode == 1 || this.mDockMode == 4 || this.mDockMode == 3)) {
                intent = this.mDeskDockIntent;
            } else if (this.mUiMode == 7) {
                intent = this.mVrHeadsetHomeIntent;
            }
        }
        if (intent == null) {
            return null;
        }
        ResolveInfo resolveInfoResolveActivityAsUser = this.mContext.getPackageManager().resolveActivityAsUser(intent, 65664, this.mCurrentUserId);
        if (resolveInfoResolveActivityAsUser != null) {
            activityInfo = resolveInfoResolveActivityAsUser.activityInfo;
        } else {
            activityInfo = null;
        }
        if (activityInfo == null || activityInfo.metaData == null || !activityInfo.metaData.getBoolean("android.dock_home")) {
            return null;
        }
        Intent intent2 = new Intent(intent);
        intent2.setClassName(activityInfo.packageName, activityInfo.name);
        return intent2;
    }

    void startDockOrHome(boolean z, boolean z2) {
        Intent intent;
        try {
            ActivityManager.getService().stopAppSwitches();
        } catch (RemoteException e) {
        }
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
        if (z2) {
            awakenDreams();
        }
        Intent intentCreateHomeDockIntent = createHomeDockIntent();
        if (intentCreateHomeDockIntent != null) {
            if (z) {
                try {
                    intentCreateHomeDockIntent.putExtra("android.intent.extra.FROM_HOME_KEY", z);
                } catch (ActivityNotFoundException e2) {
                }
            }
            startActivityAsUser(intentCreateHomeDockIntent, UserHandle.CURRENT);
            return;
        }
        if (z) {
            intent = new Intent(this.mHomeIntent);
            intent.putExtra("android.intent.extra.FROM_HOME_KEY", z);
        } else {
            intent = this.mHomeIntent;
        }
        startActivityAsUser(intent, UserHandle.CURRENT);
    }

    boolean goHome() {
        if (!isUserSetupComplete()) {
            Slog.i("WindowManager", "Not going home because user setup is in progress.");
            return false;
        }
        try {
            if (SystemProperties.getInt("persist.sys.uts-test-mode", 0) == 1) {
                Log.d("WindowManager", "UTS-TEST-MODE");
            } else {
                ActivityManager.getService().stopAppSwitches();
                sendCloseSystemWindows();
                Intent intentCreateHomeDockIntent = createHomeDockIntent();
                if (intentCreateHomeDockIntent != null && ActivityManager.getService().startActivityAsUser((IApplicationThread) null, (String) null, intentCreateHomeDockIntent, intentCreateHomeDockIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 1, (ProfilerInfo) null, (Bundle) null, -2) == 1) {
                    return false;
                }
            }
        } catch (RemoteException e) {
        }
        return ActivityManager.getService().startActivityAsUser((IApplicationThread) null, (String) null, this.mHomeIntent, this.mHomeIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 1, (ProfilerInfo) null, (Bundle) null, -2) != 1;
    }

    @Override
    public void setCurrentOrientationLw(int i) {
        synchronized (this.mLock) {
            if (i != this.mCurrentAppOrientation) {
                this.mCurrentAppOrientation = i;
                updateOrientationListenerLp();
            }
        }
    }

    private boolean isTheaterModeEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
    }

    @Override
    public boolean performHapticFeedbackLw(WindowManagerPolicy.WindowState windowState, int i, boolean z) {
        VibrationEffect vibrationEffect;
        int iMyUid;
        String opPackageName;
        if (!this.mVibrator.hasVibrator()) {
            return false;
        }
        if (((Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) == 0) && !z) || (vibrationEffect = getVibrationEffect(i)) == null) {
            return false;
        }
        if (windowState != null) {
            iMyUid = windowState.getOwningUid();
            opPackageName = windowState.getOwningPackage();
        } else {
            iMyUid = Process.myUid();
            opPackageName = this.mContext.getOpPackageName();
        }
        this.mVibrator.vibrate(iMyUid, opPackageName, vibrationEffect, VIBRATION_ATTRIBUTES);
        return true;
    }

    private VibrationEffect getVibrationEffect(int i) {
        long[] jArr;
        if (i != 10001) {
            switch (i) {
                case 0:
                    return VibrationEffect.get(5);
                case 1:
                    return VibrationEffect.get(0);
                default:
                    switch (i) {
                        case 3:
                        case 12:
                        case 15:
                        case 16:
                            break;
                        case 4:
                        case 6:
                            return VibrationEffect.get(2);
                        case 5:
                            jArr = this.mCalendarDateVibePattern;
                            break;
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 13:
                            return VibrationEffect.get(2, false);
                        case 14:
                            break;
                        case 17:
                            return VibrationEffect.get(1);
                        default:
                            return null;
                    }
                    break;
            }
        } else {
            jArr = this.mSafeModeEnabledVibePattern;
        }
        if (jArr.length == 0) {
            return null;
        }
        if (jArr.length == 1) {
            return VibrationEffect.createOneShot(jArr[0], -1);
        }
        return VibrationEffect.createWaveform(jArr, -1);
    }

    @Override
    public void keepScreenOnStartedLw() {
    }

    @Override
    public void keepScreenOnStoppedLw() {
        if (isKeyguardShowingAndNotOccluded()) {
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    private int updateSystemUiVisibilityLw() {
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow != null ? this.mFocusedWindow : this.mTopFullscreenOpaqueWindowState;
        if (windowState == null) {
            return 0;
        }
        if (windowState.getAttrs().token == this.mImmersiveModeConfirmation.getWindowToken()) {
            windowState = isStatusBarKeyguard() ? this.mStatusBar : this.mTopFullscreenOpaqueWindowState;
            if (windowState == null) {
                return 0;
            }
        }
        final WindowManagerPolicy.WindowState windowState2 = windowState;
        if ((windowState2.getAttrs().privateFlags & 1024) != 0 && this.mKeyguardOccluded) {
            return 0;
        }
        int systemUiVisibility = PolicyControl.getSystemUiVisibility(windowState2, null) & (~this.mResettingSystemUiFlags) & (~this.mForceClearedSystemUiFlags);
        if (this.mForcingShowNavBar && windowState2.getSurfaceLayer() < this.mForcingShowNavBarLayer) {
            systemUiVisibility &= ~PolicyControl.adjustClearableFlags(windowState2, 7);
        }
        final int iUpdateLightStatusBarLw = updateLightStatusBarLw(0, this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState);
        final int iUpdateLightStatusBarLw2 = updateLightStatusBarLw(0, this.mTopDockedOpaqueWindowState, this.mTopDockedOpaqueOrDimmingWindowState);
        this.mWindowManagerFuncs.getStackBounds(0, 2, this.mNonDockedStackBounds);
        this.mWindowManagerFuncs.getStackBounds(3, 1, this.mDockedStackBounds);
        final int iUpdateSystemBarsLw = updateSystemBarsLw(windowState2, this.mLastSystemUiFlags, systemUiVisibility);
        int i = this.mLastSystemUiFlags ^ iUpdateSystemBarsLw;
        int i2 = this.mLastFullscreenStackSysUiFlags ^ iUpdateLightStatusBarLw;
        int i3 = this.mLastDockedStackSysUiFlags ^ iUpdateLightStatusBarLw2;
        final boolean needsMenuLw = windowState2.getNeedsMenuLw(this.mTopFullscreenOpaqueWindowState);
        if (i == 0 && i2 == 0 && i3 == 0 && this.mLastFocusNeedsMenu == needsMenuLw && this.mFocusedApp == windowState2.getAppToken() && this.mLastNonDockedStackBounds.equals(this.mNonDockedStackBounds) && this.mLastDockedStackBounds.equals(this.mDockedStackBounds)) {
            return 0;
        }
        this.mLastSystemUiFlags = iUpdateSystemBarsLw;
        this.mLastFullscreenStackSysUiFlags = iUpdateLightStatusBarLw;
        this.mLastDockedStackSysUiFlags = iUpdateLightStatusBarLw2;
        this.mLastFocusNeedsMenu = needsMenuLw;
        this.mFocusedApp = windowState2.getAppToken();
        final Rect rect = new Rect(this.mNonDockedStackBounds);
        final Rect rect2 = new Rect(this.mDockedStackBounds);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                StatusBarManagerInternal statusBarManagerInternal = PhoneWindowManager.this.getStatusBarManagerInternal();
                if (statusBarManagerInternal != null) {
                    statusBarManagerInternal.setSystemUiVisibility(iUpdateSystemBarsLw, iUpdateLightStatusBarLw, iUpdateLightStatusBarLw2, -1, rect, rect2, windowState2.toString());
                    statusBarManagerInternal.topAppWindowChanged(needsMenuLw);
                }
            }
        });
        return i;
    }

    private int updateLightStatusBarLw(int i, WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2) {
        boolean z = isStatusBarKeyguard() && !this.mKeyguardOccluded;
        if (z) {
            windowState2 = this.mStatusBar;
        }
        if (windowState2 != null && (windowState2 == windowState || z)) {
            return (i & (-8193)) | (PolicyControl.getSystemUiVisibility(windowState2, null) & 8192);
        }
        if (windowState2 != null && windowState2.isDimming()) {
            return i & (-8193);
        }
        return i;
    }

    @VisibleForTesting
    static WindowManagerPolicy.WindowState chooseNavigationColorWindowLw(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2, WindowManagerPolicy.WindowState windowState3, int i) {
        boolean z = windowState3 != null && windowState3.isVisibleLw() && i == 4 && (PolicyControl.getWindowFlags(windowState3, null) & Integer.MIN_VALUE) != 0;
        if (windowState != null && windowState2 == windowState) {
            return z ? windowState3 : windowState;
        }
        if (windowState2 == null || !windowState2.isDimming()) {
            if (z) {
                return windowState3;
            }
            return null;
        }
        if (z && WindowManager.LayoutParams.mayUseInputMethod(PolicyControl.getWindowFlags(windowState2, null))) {
            return windowState3;
        }
        return windowState2;
    }

    @VisibleForTesting
    static int updateLightNavigationBarLw(int i, WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2, WindowManagerPolicy.WindowState windowState3, WindowManagerPolicy.WindowState windowState4) {
        if (windowState4 != null) {
            if (windowState4 == windowState3 || windowState4 == windowState) {
                return (i & (-17)) | (PolicyControl.getSystemUiVisibility(windowState4, null) & 16);
            }
            if (windowState4 == windowState2 && windowState4.isDimming()) {
                return i & (-17);
            }
            return i;
        }
        return i;
    }

    private int updateSystemBarsLw(WindowManagerPolicy.WindowState windowState, int i, int i2) {
        WindowManagerPolicy.WindowState windowState2;
        boolean zIsStackVisible = this.mWindowManagerInternal.isStackVisible(3);
        boolean zIsStackVisible2 = this.mWindowManagerInternal.isStackVisible(5);
        boolean zIsDockedDividerResizing = this.mWindowManagerInternal.isDockedDividerResizing();
        boolean z = true;
        this.mForceShowSystemBars = zIsStackVisible || zIsStackVisible2 || zIsDockedDividerResizing;
        boolean z2 = this.mForceShowSystemBars && !this.mForceStatusBarFromKeyguard;
        if (isStatusBarKeyguard() && !this.mKeyguardOccluded) {
            windowState2 = this.mStatusBar;
        } else {
            windowState2 = this.mTopFullscreenOpaqueWindowState;
        }
        int iApplyTranslucentFlagLw = this.mNavigationBarController.applyTranslucentFlagLw(windowState2, this.mStatusBarController.applyTranslucentFlagLw(windowState2, i2, i), i);
        int iApplyTranslucentFlagLw2 = this.mStatusBarController.applyTranslucentFlagLw(this.mTopDockedOpaqueWindowState, 0, 0);
        boolean zDrawsStatusBarBackground = drawsStatusBarBackground(iApplyTranslucentFlagLw, this.mTopFullscreenOpaqueWindowState);
        boolean zDrawsStatusBarBackground2 = drawsStatusBarBackground(iApplyTranslucentFlagLw2, this.mTopDockedOpaqueWindowState);
        boolean z3 = windowState.getAttrs().type == 2000;
        if (z3 && !isStatusBarKeyguard()) {
            int i3 = 14342;
            if (this.mKeyguardOccluded) {
                i3 = -1073727482;
            }
            iApplyTranslucentFlagLw = (iApplyTranslucentFlagLw & (~i3)) | (i3 & i);
        }
        if (zDrawsStatusBarBackground && zDrawsStatusBarBackground2) {
            iApplyTranslucentFlagLw = (iApplyTranslucentFlagLw | 8) & (-1073741825);
        } else if ((!areTranslucentBarsAllowed() && windowState2 != this.mStatusBar) || z2) {
            iApplyTranslucentFlagLw &= -1073741833;
        }
        int iConfigureNavBarOpacity = configureNavBarOpacity(iApplyTranslucentFlagLw, zIsStackVisible, zIsStackVisible2, zIsDockedDividerResizing);
        boolean z4 = (iConfigureNavBarOpacity & 4096) != 0;
        boolean z5 = (this.mTopFullscreenOpaqueWindowState == null || (PolicyControl.getWindowFlags(this.mTopFullscreenOpaqueWindowState, null) & 1024) == 0) ? false : true;
        boolean z6 = (iConfigureNavBarOpacity & 4) != 0;
        boolean z7 = (iConfigureNavBarOpacity & 2) != 0 || this.mForceHideNavBar;
        boolean z8 = this.mStatusBar != null && (z3 || (!this.mForceShowSystemBars && (z5 || (z6 && z4))));
        boolean z9 = this.mNavigationBar != null && !this.mForceShowSystemBars && z7 && z4;
        if ((this.mPendingPanicGestureUptime != 0 && SystemClock.uptimeMillis() - this.mPendingPanicGestureUptime <= 30000) && z7 && !isStatusBarKeyguard() && this.mKeyguardDrawComplete) {
            this.mPendingPanicGestureUptime = 0L;
            this.mStatusBarController.showTransient();
            if (!isNavBarEmpty(iConfigureNavBarOpacity)) {
                this.mNavigationBarController.showTransient();
            }
        }
        boolean z10 = this.mStatusBarController.isTransientShowRequested() && !z8 && z6;
        boolean z11 = this.mNavigationBarController.isTransientShowRequested() && !z9;
        if (z10 || z11 || this.mForceShowSystemBars) {
            clearClearableFlagsLw();
            iConfigureNavBarOpacity &= -8;
        }
        boolean z12 = (iConfigureNavBarOpacity & 2048) != 0;
        boolean z13 = (iConfigureNavBarOpacity & 4096) != 0;
        if (!z12 && !z13) {
            z = false;
        }
        if (z7 && !z && getWindowLayerLw(windowState) > getWindowLayerFromTypeLw(2022)) {
            iConfigureNavBarOpacity &= -3;
        }
        int iUpdateVisibilityLw = this.mStatusBarController.updateVisibilityLw(z8, i, iConfigureNavBarOpacity);
        boolean zIsImmersiveMode = isImmersiveMode(i);
        boolean zIsImmersiveMode2 = isImmersiveMode(iUpdateVisibilityLw);
        if (windowState != null && zIsImmersiveMode != zIsImmersiveMode2) {
            this.mImmersiveModeConfirmation.immersiveModeChangedLw(windowState.getOwningPackage(), zIsImmersiveMode2, isUserSetupComplete(), isNavBarEmpty(windowState.getSystemUiVisibility()));
        }
        return updateLightNavigationBarLw(this.mNavigationBarController.updateVisibilityLw(z9, i, iUpdateVisibilityLw), this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState, this.mWindowManagerFuncs.getInputMethodWindowLw(), chooseNavigationColorWindowLw(this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState, this.mWindowManagerFuncs.getInputMethodWindowLw(), this.mNavigationBarPosition));
    }

    private boolean drawsStatusBarBackground(int i, WindowManagerPolicy.WindowState windowState) {
        if (!this.mStatusBarController.isTransparentAllowed(windowState)) {
            return false;
        }
        if (windowState == null) {
            return true;
        }
        boolean z = (windowState.getAttrs().flags & Integer.MIN_VALUE) != 0;
        if ((windowState.getAttrs().privateFlags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0) {
            return true;
        }
        return z && (i & 1073741824) == 0;
    }

    private int configureNavBarOpacity(int i, boolean z, boolean z2, boolean z3) {
        if (this.mNavBarOpacityMode == 0) {
            if (z || z2 || z3) {
                i = setNavBarOpaqueFlag(i);
            }
        } else if (this.mNavBarOpacityMode == 1) {
            if (!z3 && z2) {
                i = setNavBarTranslucentFlag(i);
            } else {
                i = setNavBarOpaqueFlag(i);
            }
        }
        if (!areTranslucentBarsAllowed()) {
            return i & Integer.MAX_VALUE;
        }
        return i;
    }

    private int setNavBarOpaqueFlag(int i) {
        return i & 2147450879;
    }

    private int setNavBarTranslucentFlag(int i) {
        return (i & (-32769)) | Integer.MIN_VALUE;
    }

    private void clearClearableFlagsLw() {
        int i = this.mResettingSystemUiFlags | 7;
        if (i != this.mResettingSystemUiFlags) {
            this.mResettingSystemUiFlags = i;
            this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
        }
    }

    private boolean isImmersiveMode(int i) {
        return this.mNavigationBar != null && (this.mForceHideNavBar || !((i & 2) == 0 || (i & 6144) == 0 || !canHideNavigationBar()));
    }

    private static boolean isNavBarEmpty(int i) {
        return (i & 23068672) == 23068672;
    }

    private boolean areTranslucentBarsAllowed() {
        return this.mTranslucentDecorEnabled;
    }

    @Override
    public boolean hasNavigationBar() {
        return this.mHasNavigationBar;
    }

    @Override
    public void setLastInputMethodWindowLw(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2) {
        this.mLastInputMethodWindow = windowState;
        this.mLastInputMethodTargetWindow = windowState2;
    }

    @Override
    public void setDismissImeOnBackKeyPressed(boolean z) {
        this.mDismissImeOnBackKeyPressed = z;
    }

    @Override
    public void setCurrentUserLw(int i) {
        this.mCurrentUserId = i;
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.setCurrentUser(i);
        }
        if (this.mAccessibilityShortcutController != null) {
            this.mAccessibilityShortcutController.setCurrentUser(i);
        }
        StatusBarManagerInternal statusBarManagerInternal = getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.setCurrentUser(i);
        }
        setLastInputMethodWindowLw(null, null);
    }

    @Override
    public void setSwitchingUser(boolean z) {
        this.mKeyguardDelegate.setSwitchingUser(z);
    }

    @Override
    public boolean isTopLevelWindow(int i) {
        return i < 1000 || i > 1999 || i == 1003;
    }

    @Override
    public boolean shouldRotateSeamlessly(int i, int i2) {
        WindowManagerPolicy.WindowState windowState;
        if (i == this.mUpsideDownRotation || i2 == this.mUpsideDownRotation || !this.mNavigationBarCanMove) {
            return false;
        }
        int i3 = i2 - i;
        if (i3 < 0) {
            i3 += 4;
        }
        return (i3 == 2 || (windowState = this.mTopFullscreenOpaqueWindowState) != this.mFocusedWindow || windowState == null || windowState.isAnimatingLw() || (windowState.getAttrs().rotationAnimation != 2 && windowState.getAttrs().rotationAnimation != 3)) ? false : true;
    }

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.mLastSystemUiFlags);
        protoOutputStream.write(1159641169922L, this.mUserRotationMode);
        protoOutputStream.write(1159641169923L, this.mUserRotation);
        protoOutputStream.write(1159641169924L, this.mCurrentAppOrientation);
        protoOutputStream.write(1133871366149L, this.mScreenOnFully);
        protoOutputStream.write(1133871366150L, this.mKeyguardDrawComplete);
        protoOutputStream.write(1133871366151L, this.mWindowManagerDrawComplete);
        if (this.mFocusedApp != null) {
            protoOutputStream.write(1138166333448L, this.mFocusedApp.toString());
        }
        if (this.mFocusedWindow != null) {
            this.mFocusedWindow.writeIdentifierToProto(protoOutputStream, 1146756268041L);
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            this.mTopFullscreenOpaqueWindowState.writeIdentifierToProto(protoOutputStream, 1146756268042L);
        }
        if (this.mTopFullscreenOpaqueOrDimmingWindowState != null) {
            this.mTopFullscreenOpaqueOrDimmingWindowState.writeIdentifierToProto(protoOutputStream, 1146756268043L);
        }
        protoOutputStream.write(1133871366156L, this.mKeyguardOccluded);
        protoOutputStream.write(1133871366157L, this.mKeyguardOccludedChanged);
        protoOutputStream.write(1133871366158L, this.mPendingKeyguardOccluded);
        protoOutputStream.write(1133871366159L, this.mForceStatusBar);
        protoOutputStream.write(1133871366160L, this.mForceStatusBarFromKeyguard);
        this.mStatusBarController.writeToProto(protoOutputStream, 1146756268049L);
        this.mNavigationBarController.writeToProto(protoOutputStream, 1146756268050L);
        if (this.mOrientationListener != null) {
            this.mOrientationListener.writeToProto(protoOutputStream, 1146756268051L);
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.writeToProto(protoOutputStream, 1146756268052L);
        }
        protoOutputStream.end(jStart);
    }

    @Override
    public void dump(String str, PrintWriter printWriter, String[] strArr) {
        printWriter.print(str);
        printWriter.print("mSafeMode=");
        printWriter.print(this.mSafeMode);
        printWriter.print(" mSystemReady=");
        printWriter.print(this.mSystemReady);
        printWriter.print(" mSystemBooted=");
        printWriter.println(this.mSystemBooted);
        printWriter.print(str);
        printWriter.print("mLidState=");
        printWriter.print(WindowManagerPolicy.WindowManagerFuncs.lidStateToString(this.mLidState));
        printWriter.print(" mLidOpenRotation=");
        printWriter.println(Surface.rotationToString(this.mLidOpenRotation));
        printWriter.print(str);
        printWriter.print("mCameraLensCoverState=");
        printWriter.print(WindowManagerPolicy.WindowManagerFuncs.cameraLensStateToString(this.mCameraLensCoverState));
        printWriter.print(" mHdmiPlugged=");
        printWriter.println(this.mHdmiPlugged);
        if (this.mLastSystemUiFlags != 0 || this.mResettingSystemUiFlags != 0 || this.mForceClearedSystemUiFlags != 0) {
            printWriter.print(str);
            printWriter.print("mLastSystemUiFlags=0x");
            printWriter.print(Integer.toHexString(this.mLastSystemUiFlags));
            printWriter.print(" mResettingSystemUiFlags=0x");
            printWriter.print(Integer.toHexString(this.mResettingSystemUiFlags));
            printWriter.print(" mForceClearedSystemUiFlags=0x");
            printWriter.println(Integer.toHexString(this.mForceClearedSystemUiFlags));
        }
        if (this.mLastFocusNeedsMenu) {
            printWriter.print(str);
            printWriter.print("mLastFocusNeedsMenu=");
            printWriter.println(this.mLastFocusNeedsMenu);
        }
        printWriter.print(str);
        printWriter.print("mWakeGestureEnabledSetting=");
        printWriter.println(this.mWakeGestureEnabledSetting);
        printWriter.print(str);
        printWriter.print("mSupportAutoRotation=");
        printWriter.print(this.mSupportAutoRotation);
        printWriter.print(" mOrientationSensorEnabled=");
        printWriter.println(this.mOrientationSensorEnabled);
        printWriter.print(str);
        printWriter.print("mUiMode=");
        printWriter.print(Configuration.uiModeToString(this.mUiMode));
        printWriter.print(" mDockMode=");
        printWriter.println(Intent.dockStateToString(this.mDockMode));
        printWriter.print(str);
        printWriter.print("mEnableCarDockHomeCapture=");
        printWriter.print(this.mEnableCarDockHomeCapture);
        printWriter.print(" mCarDockRotation=");
        printWriter.print(Surface.rotationToString(this.mCarDockRotation));
        printWriter.print(" mDeskDockRotation=");
        printWriter.println(Surface.rotationToString(this.mDeskDockRotation));
        printWriter.print(str);
        printWriter.print("mUserRotationMode=");
        printWriter.print(WindowManagerPolicy.userRotationModeToString(this.mUserRotationMode));
        printWriter.print(" mUserRotation=");
        printWriter.print(Surface.rotationToString(this.mUserRotation));
        printWriter.print(" mAllowAllRotations=");
        printWriter.println(allowAllRotationsToString(this.mAllowAllRotations));
        printWriter.print(str);
        printWriter.print("mCurrentAppOrientation=");
        printWriter.println(ActivityInfo.screenOrientationToString(this.mCurrentAppOrientation));
        printWriter.print(str);
        printWriter.print("mCarDockEnablesAccelerometer=");
        printWriter.print(this.mCarDockEnablesAccelerometer);
        printWriter.print(" mDeskDockEnablesAccelerometer=");
        printWriter.println(this.mDeskDockEnablesAccelerometer);
        printWriter.print(str);
        printWriter.print("mLidKeyboardAccessibility=");
        printWriter.print(this.mLidKeyboardAccessibility);
        printWriter.print(" mLidNavigationAccessibility=");
        printWriter.print(this.mLidNavigationAccessibility);
        printWriter.print(" mLidControlsScreenLock=");
        printWriter.println(this.mLidControlsScreenLock);
        printWriter.print(str);
        printWriter.print("mLidControlsSleep=");
        printWriter.println(this.mLidControlsSleep);
        printWriter.print(str);
        printWriter.print("mLongPressOnBackBehavior=");
        printWriter.println(longPressOnBackBehaviorToString(this.mLongPressOnBackBehavior));
        printWriter.print(str);
        printWriter.print("mLongPressOnHomeBehavior=");
        printWriter.println(longPressOnHomeBehaviorToString(this.mLongPressOnHomeBehavior));
        printWriter.print(str);
        printWriter.print("mDoubleTapOnHomeBehavior=");
        printWriter.println(doubleTapOnHomeBehaviorToString(this.mDoubleTapOnHomeBehavior));
        printWriter.print(str);
        printWriter.print("mShortPressOnPowerBehavior=");
        printWriter.println(shortPressOnPowerBehaviorToString(this.mShortPressOnPowerBehavior));
        printWriter.print(str);
        printWriter.print("mLongPressOnPowerBehavior=");
        printWriter.println(longPressOnPowerBehaviorToString(this.mLongPressOnPowerBehavior));
        printWriter.print(str);
        printWriter.print("mVeryLongPressOnPowerBehavior=");
        printWriter.println(veryLongPressOnPowerBehaviorToString(this.mVeryLongPressOnPowerBehavior));
        printWriter.print(str);
        printWriter.print("mDoublePressOnPowerBehavior=");
        printWriter.println(multiPressOnPowerBehaviorToString(this.mDoublePressOnPowerBehavior));
        printWriter.print(str);
        printWriter.print("mTriplePressOnPowerBehavior=");
        printWriter.println(multiPressOnPowerBehaviorToString(this.mTriplePressOnPowerBehavior));
        printWriter.print(str);
        printWriter.print("mShortPressOnSleepBehavior=");
        printWriter.println(shortPressOnSleepBehaviorToString(this.mShortPressOnSleepBehavior));
        printWriter.print(str);
        printWriter.print("mShortPressOnWindowBehavior=");
        printWriter.println(shortPressOnWindowBehaviorToString(this.mShortPressOnWindowBehavior));
        printWriter.print(str);
        printWriter.print("mAllowStartActivityForLongPressOnPowerDuringSetup=");
        printWriter.println(this.mAllowStartActivityForLongPressOnPowerDuringSetup);
        printWriter.print(str);
        printWriter.print("mHasSoftInput=");
        printWriter.print(this.mHasSoftInput);
        printWriter.print(" mDismissImeOnBackKeyPressed=");
        printWriter.println(this.mDismissImeOnBackKeyPressed);
        printWriter.print(str);
        printWriter.print("mIncallPowerBehavior=");
        printWriter.print(incallPowerBehaviorToString(this.mIncallPowerBehavior));
        printWriter.print(" mIncallBackBehavior=");
        printWriter.print(incallBackBehaviorToString(this.mIncallBackBehavior));
        printWriter.print(" mEndcallBehavior=");
        printWriter.println(endcallBehaviorToString(this.mEndcallBehavior));
        printWriter.print(str);
        printWriter.print("mHomePressed=");
        printWriter.println(this.mHomePressed);
        printWriter.print(str);
        printWriter.print("mAwake=");
        printWriter.print(this.mAwake);
        printWriter.print("mScreenOnEarly=");
        printWriter.print(this.mScreenOnEarly);
        printWriter.print(" mScreenOnFully=");
        printWriter.println(this.mScreenOnFully);
        printWriter.print(str);
        printWriter.print("mKeyguardDrawComplete=");
        printWriter.print(this.mKeyguardDrawComplete);
        printWriter.print(" mWindowManagerDrawComplete=");
        printWriter.println(this.mWindowManagerDrawComplete);
        printWriter.print(str);
        printWriter.print("mDockLayer=");
        printWriter.print(this.mDockLayer);
        printWriter.print(" mStatusBarLayer=");
        printWriter.println(this.mStatusBarLayer);
        printWriter.print(str);
        printWriter.print("mShowingDream=");
        printWriter.print(this.mShowingDream);
        printWriter.print(" mDreamingLockscreen=");
        printWriter.print(this.mDreamingLockscreen);
        printWriter.print(" mDreamingSleepToken=");
        printWriter.println(this.mDreamingSleepToken);
        if (this.mLastInputMethodWindow != null) {
            printWriter.print(str);
            printWriter.print("mLastInputMethodWindow=");
            printWriter.println(this.mLastInputMethodWindow);
        }
        if (this.mLastInputMethodTargetWindow != null) {
            printWriter.print(str);
            printWriter.print("mLastInputMethodTargetWindow=");
            printWriter.println(this.mLastInputMethodTargetWindow);
        }
        if (this.mStatusBar != null) {
            printWriter.print(str);
            printWriter.print("mStatusBar=");
            printWriter.print(this.mStatusBar);
            printWriter.print(" isStatusBarKeyguard=");
            printWriter.println(isStatusBarKeyguard());
        }
        if (this.mNavigationBar != null) {
            printWriter.print(str);
            printWriter.print("mNavigationBar=");
            printWriter.println(this.mNavigationBar);
        }
        if (this.mFocusedWindow != null) {
            printWriter.print(str);
            printWriter.print("mFocusedWindow=");
            printWriter.println(this.mFocusedWindow);
        }
        if (this.mFocusedApp != null) {
            printWriter.print(str);
            printWriter.print("mFocusedApp=");
            printWriter.println(this.mFocusedApp);
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            printWriter.print(str);
            printWriter.print("mTopFullscreenOpaqueWindowState=");
            printWriter.println(this.mTopFullscreenOpaqueWindowState);
        }
        if (this.mTopFullscreenOpaqueOrDimmingWindowState != null) {
            printWriter.print(str);
            printWriter.print("mTopFullscreenOpaqueOrDimmingWindowState=");
            printWriter.println(this.mTopFullscreenOpaqueOrDimmingWindowState);
        }
        if (this.mForcingShowNavBar) {
            printWriter.print(str);
            printWriter.print("mForcingShowNavBar=");
            printWriter.println(this.mForcingShowNavBar);
            printWriter.print("mForcingShowNavBarLayer=");
            printWriter.println(this.mForcingShowNavBarLayer);
        }
        printWriter.print(str);
        printWriter.print("mTopIsFullscreen=");
        printWriter.print(this.mTopIsFullscreen);
        printWriter.print(" mKeyguardOccluded=");
        printWriter.println(this.mKeyguardOccluded);
        printWriter.print(str);
        printWriter.print("mKeyguardOccludedChanged=");
        printWriter.print(this.mKeyguardOccludedChanged);
        printWriter.print(" mPendingKeyguardOccluded=");
        printWriter.println(this.mPendingKeyguardOccluded);
        printWriter.print(str);
        printWriter.print("mForceStatusBar=");
        printWriter.print(this.mForceStatusBar);
        printWriter.print(" mForceStatusBarFromKeyguard=");
        printWriter.println(this.mForceStatusBarFromKeyguard);
        printWriter.print(str);
        printWriter.print("mAllowLockscreenWhenOn=");
        printWriter.print(this.mAllowLockscreenWhenOn);
        printWriter.print(" mLockScreenTimeout=");
        printWriter.print(this.mLockScreenTimeout);
        printWriter.print(" mLockScreenTimerActive=");
        printWriter.println(this.mLockScreenTimerActive);
        printWriter.print(str);
        printWriter.print("mLandscapeRotation=");
        printWriter.print(Surface.rotationToString(this.mLandscapeRotation));
        printWriter.print(" mSeascapeRotation=");
        printWriter.println(Surface.rotationToString(this.mSeascapeRotation));
        printWriter.print(str);
        printWriter.print("mPortraitRotation=");
        printWriter.print(Surface.rotationToString(this.mPortraitRotation));
        printWriter.print(" mUpsideDownRotation=");
        printWriter.println(Surface.rotationToString(this.mUpsideDownRotation));
        printWriter.print(str);
        printWriter.print("mDemoHdmiRotation=");
        printWriter.print(Surface.rotationToString(this.mDemoHdmiRotation));
        printWriter.print(" mDemoHdmiRotationLock=");
        printWriter.println(this.mDemoHdmiRotationLock);
        printWriter.print(str);
        printWriter.print("mUndockedHdmiRotation=");
        printWriter.println(Surface.rotationToString(this.mUndockedHdmiRotation));
        if (this.mHasFeatureLeanback) {
            printWriter.print(str);
            printWriter.print("mAccessibilityTvKey1Pressed=");
            printWriter.println(this.mAccessibilityTvKey1Pressed);
            printWriter.print(str);
            printWriter.print("mAccessibilityTvKey2Pressed=");
            printWriter.println(this.mAccessibilityTvKey2Pressed);
            printWriter.print(str);
            printWriter.print("mAccessibilityTvScheduled=");
            printWriter.println(this.mAccessibilityTvScheduled);
        }
        this.mGlobalKeyManager.dump(str, printWriter);
        this.mStatusBarController.dump(printWriter, str);
        this.mNavigationBarController.dump(printWriter, str);
        PolicyControl.dump(str, printWriter);
        if (this.mWakeGestureListener != null) {
            this.mWakeGestureListener.dump(printWriter, str);
        }
        if (this.mOrientationListener != null) {
            this.mOrientationListener.dump(printWriter, str);
        }
        if (this.mBurnInProtectionHelper != null) {
            this.mBurnInProtectionHelper.dump(str, printWriter);
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.dump(str, printWriter);
        }
        printWriter.print(str);
        printWriter.println("Looper state:");
        this.mHandler.getLooper().dump(new PrintWriterPrinter(printWriter), str + "  ");
    }

    private static String allowAllRotationsToString(int i) {
        switch (i) {
            case -1:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
            case 0:
                return "false";
            case 1:
                return "true";
            default:
                return Integer.toString(i);
        }
    }

    private static String endcallBehaviorToString(int i) {
        StringBuilder sb = new StringBuilder();
        if ((i & 1) != 0) {
            sb.append("home|");
        }
        if ((i & 2) != 0) {
            sb.append("sleep|");
        }
        int length = sb.length();
        if (length == 0) {
            return "<nothing>";
        }
        return sb.substring(0, length - 1);
    }

    private static String incallPowerBehaviorToString(int i) {
        if ((i & 2) != 0) {
            return "hangup";
        }
        return "sleep";
    }

    private static String incallBackBehaviorToString(int i) {
        if ((i & 1) != 0) {
            return "hangup";
        }
        return "<nothing>";
    }

    private static String longPressOnBackBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "LONG_PRESS_BACK_NOTHING";
            case 1:
                return "LONG_PRESS_BACK_GO_TO_VOICE_ASSIST";
            default:
                return Integer.toString(i);
        }
    }

    private static String longPressOnHomeBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "LONG_PRESS_HOME_NOTHING";
            case 1:
                return "LONG_PRESS_HOME_ALL_APPS";
            case 2:
                return "LONG_PRESS_HOME_ASSIST";
            default:
                return Integer.toString(i);
        }
    }

    private static String doubleTapOnHomeBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "DOUBLE_TAP_HOME_NOTHING";
            case 1:
                return "DOUBLE_TAP_HOME_RECENT_SYSTEM_UI";
            default:
                return Integer.toString(i);
        }
    }

    private static String shortPressOnPowerBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "SHORT_PRESS_POWER_NOTHING";
            case 1:
                return "SHORT_PRESS_POWER_GO_TO_SLEEP";
            case 2:
                return "SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP";
            case 3:
                return "SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME";
            case 4:
                return "SHORT_PRESS_POWER_GO_HOME";
            case 5:
                return "SHORT_PRESS_POWER_CLOSE_IME_OR_GO_HOME";
            default:
                return Integer.toString(i);
        }
    }

    private static String longPressOnPowerBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "LONG_PRESS_POWER_NOTHING";
            case 1:
                return "LONG_PRESS_POWER_GLOBAL_ACTIONS";
            case 2:
                return "LONG_PRESS_POWER_SHUT_OFF";
            case 3:
                return "LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM";
            default:
                return Integer.toString(i);
        }
    }

    private static String veryLongPressOnPowerBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "VERY_LONG_PRESS_POWER_NOTHING";
            case 1:
                return "VERY_LONG_PRESS_POWER_GLOBAL_ACTIONS";
            default:
                return Integer.toString(i);
        }
    }

    private static String multiPressOnPowerBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "MULTI_PRESS_POWER_NOTHING";
            case 1:
                return "MULTI_PRESS_POWER_THEATER_MODE";
            case 2:
                return "MULTI_PRESS_POWER_BRIGHTNESS_BOOST";
            default:
                return Integer.toString(i);
        }
    }

    private static String shortPressOnSleepBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "SHORT_PRESS_SLEEP_GO_TO_SLEEP";
            case 1:
                return "SHORT_PRESS_SLEEP_GO_TO_SLEEP_AND_GO_HOME";
            default:
                return Integer.toString(i);
        }
    }

    private static String shortPressOnWindowBehaviorToString(int i) {
        switch (i) {
            case 0:
                return "SHORT_PRESS_WINDOW_NOTHING";
            case 1:
                return "SHORT_PRESS_WINDOW_PICTURE_IN_PICTURE";
            default:
                return Integer.toString(i);
        }
    }

    @Override
    public void onLockTaskStateChangedLw(int i) {
        this.mImmersiveModeConfirmation.onLockTaskModeChangedLw(i);
    }

    private void updateRect(DisplayFrames displayFrames, int i, int i2, int i3, int i4) {
        displayFrames.mStable.left += i;
        displayFrames.mStable.top += i2;
        displayFrames.mStable.right -= i3;
        displayFrames.mStable.bottom -= i4;
        displayFrames.mDock.left += i;
        displayFrames.mDock.top += i2;
        displayFrames.mDock.right -= i3;
        displayFrames.mDock.bottom -= i4;
        displayFrames.mSystem.left = displayFrames.mDock.left;
        displayFrames.mSystem.top = displayFrames.mDock.top;
        displayFrames.mSystem.right = displayFrames.mDock.right;
        displayFrames.mSystem.bottom = displayFrames.mDock.bottom;
        displayFrames.mStableFullscreen.left += i;
        displayFrames.mStableFullscreen.top += i2;
        displayFrames.mStableFullscreen.right -= i3;
        displayFrames.mStableFullscreen.bottom -= i4;
        displayFrames.mContent.left += i;
        displayFrames.mContent.top += i2;
        displayFrames.mContent.right -= i3;
        displayFrames.mContent.bottom -= i4;
        displayFrames.mCurrent.left += i;
        displayFrames.mCurrent.top += i2;
        displayFrames.mCurrent.right -= i3;
        displayFrames.mCurrent.bottom -= i4;
        displayFrames.mOverscan.left += i;
        displayFrames.mOverscan.top += i2;
        int i5 = i3 + i;
        displayFrames.mOverscan.right -= i5;
        int i6 = i4 + i2;
        displayFrames.mOverscan.bottom -= i6;
        displayFrames.mUnrestricted.left += i;
        displayFrames.mUnrestricted.top += i2;
        displayFrames.mUnrestricted.right -= i5;
        displayFrames.mUnrestricted.bottom -= i6;
        displayFrames.mRestricted.left += i;
        displayFrames.mRestricted.top += i2;
        displayFrames.mRestricted.right -= i5;
        displayFrames.mRestricted.bottom -= i6;
        displayFrames.mRestrictedOverscan.left += i;
        displayFrames.mRestrictedOverscan.top += i2;
        displayFrames.mRestrictedOverscan.right -= i5;
        displayFrames.mRestrictedOverscan.bottom -= i6;
    }

    @Override
    public boolean setAodShowing(boolean z) {
        if (this.mAodShowing != z) {
            this.mAodShowing = z;
            return true;
        }
        return false;
    }
}
