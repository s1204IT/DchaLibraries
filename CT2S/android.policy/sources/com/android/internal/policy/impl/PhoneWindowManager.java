package com.android.internal.policy.impl;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.IAudioService;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerInternal;
import android.view.WindowManagerPolicy;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import com.android.internal.policy.PolicyManager;
import com.android.internal.policy.impl.SystemGesturesPointerEventListener;
import com.android.internal.policy.impl.keyguard.KeyguardServiceDelegate;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.widget.PointerLocationView;
import com.android.server.LocalServices;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

public class PhoneWindowManager implements WindowManagerPolicy {
    static final int APPLICATION_MEDIA_OVERLAY_SUBLAYER = -1;
    static final int APPLICATION_MEDIA_SUBLAYER = -2;
    static final int APPLICATION_PANEL_SUBLAYER = 1;
    static final int APPLICATION_SUB_PANEL_SUBLAYER = 2;
    private static final int BRIGHTNESS_STEPS = 10;
    static final boolean DEBUG = false;
    static final boolean DEBUG_INPUT = false;
    static final boolean DEBUG_KEYGUARD = false;
    static final boolean DEBUG_LAYOUT = false;
    static final boolean DEBUG_STARTING_WINDOW = false;
    static final boolean DEBUG_WAKEUP = false;
    private static final int DISMISS_KEYGUARD_CONTINUE = 2;
    private static final int DISMISS_KEYGUARD_NONE = 0;
    private static final int DISMISS_KEYGUARD_START = 1;
    static final int DOUBLE_TAP_HOME_NOTHING = 0;
    static final int DOUBLE_TAP_HOME_RECENT_SYSTEM_UI = 1;
    static final boolean ENABLE_CAR_DOCK_HOME_CAPTURE = true;
    static final boolean ENABLE_DESK_DOCK_HOME_CAPTURE = false;
    private static final float KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER = 2.5f;
    private static final int LONGPRESS_VOLUME_KEY_INTERVAL = 500;
    static final int LONG_PRESS_HOME_ASSIST = 2;
    static final int LONG_PRESS_HOME_NOTHING = 0;
    static final int LONG_PRESS_HOME_RECENT_SYSTEM_UI = 1;
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int LONG_PRESS_POWER_NOTHING = 0;
    static final int LONG_PRESS_POWER_SHUT_OFF = 2;
    static final int LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM = 3;
    private static final int MSG_DISABLE_POINTER_LOCATION = 2;
    private static final int MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK = 3;
    private static final int MSG_DISPATCH_SHOW_GLOBAL_ACTIONS = 10;
    private static final int MSG_DISPATCH_SHOW_RECENTS = 9;
    private static final int MSG_ENABLE_POINTER_LOCATION = 1;
    private static final int MSG_HIDE_BOOT_MESSAGE = 11;
    private static final int MSG_KEYGUARD_DRAWN_COMPLETE = 5;
    private static final int MSG_KEYGUARD_DRAWN_TIMEOUT = 6;
    private static final int MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK = 12;
    private static final int MSG_POWER_LONG_PRESS = 14;
    private static final int MSG_WINDOW_MANAGER_DRAWN_COMPLETE = 7;
    static final int MULTI_PRESS_POWER_BRIGHTNESS_BOOST = 2;
    static final int MULTI_PRESS_POWER_NOTHING = 0;
    static final int MULTI_PRESS_POWER_THEATER_MODE = 1;
    static final boolean PRINT_ANIM = false;
    private static final long SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS = 150;
    static final int SHORT_PRESS_POWER_GO_TO_SLEEP = 1;
    static final int SHORT_PRESS_POWER_NOTHING = 0;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP = 2;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME = 3;
    static final boolean SHOW_PROCESSES_ON_ALT_MENU = false;
    static final boolean SHOW_STARTING_ANIMATIONS = true;
    public static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";
    public static final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    public static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    public static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    static final int SYSTEM_UI_CHANGING_LAYOUT = -1073709050;
    static final String TAG = "WindowManager";
    static final int WAITING_FOR_DRAWN_TIMEOUT = 1000;
    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK;
    static final boolean localLOGV = false;
    static final Rect mTmpContentFrame;
    static final Rect mTmpDecorFrame;
    static final Rect mTmpDisplayFrame;
    static final Rect mTmpNavigationFrame;
    static final Rect mTmpOverscanFrame;
    static final Rect mTmpParentFrame;
    static final Rect mTmpStableFrame;
    static final Rect mTmpVisibleFrame;
    boolean mAccelerometerDefault;
    AccessibilityManager mAccessibilityManager;
    boolean mAllowLockscreenWhenOn;
    private boolean mAllowTheaterModeWakeFromCameraLens;
    private boolean mAllowTheaterModeWakeFromKey;
    private boolean mAllowTheaterModeWakeFromLidSwitch;
    private boolean mAllowTheaterModeWakeFromMotion;
    private boolean mAllowTheaterModeWakeFromMotionWhenNotDreaming;
    private boolean mAllowTheaterModeWakeFromPowerKey;
    private boolean mAllowTheaterModeWakeFromWakeGesture;
    boolean mAssistKeyLongPressed;
    boolean mAwake;
    volatile boolean mBeganFromNonInteractive;
    boolean mBootMessageNeedsHiding;
    PowerManager.WakeLock mBroadcastWakeLock;
    long[] mCalendarDateVibePattern;
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
    int mCurBottom;
    int mCurLeft;
    int mCurRight;
    int mCurTop;
    private int mCurrentUserId;
    int mDemoHdmiRotation;
    boolean mDemoHdmiRotationLock;
    int mDemoRotation;
    boolean mDemoRotationLock;
    boolean mDeskDockEnablesAccelerometer;
    Intent mDeskDockIntent;
    int mDeskDockRotation;
    Display mDisplay;
    int mDockBottom;
    int mDockLayer;
    int mDockLeft;
    int mDockRight;
    int mDockTop;
    int mDoublePressOnPowerBehavior;
    private int mDoubleTapOnHomeBehavior;
    DreamManagerInternal mDreamManagerInternal;
    boolean mDreamingLockscreen;
    volatile boolean mEndCallKeyHandled;
    int mEndcallBehavior;
    IApplicationToken mFocusedApp;
    WindowManagerPolicy.WindowState mFocusedWindow;
    boolean mForceStatusBar;
    boolean mForceStatusBarFromKeyguard;
    boolean mForcingShowNavBar;
    int mForcingShowNavBarLayer;
    GlobalActions mGlobalActions;
    private GlobalKeyManager mGlobalKeyManager;
    private boolean mGoToSleepOnButtonPressTheaterMode;
    Handler mHandler;
    boolean mHaveBuiltInKeyboard;
    boolean mHavePendingMediaKeyRepeatWithWakeLock;
    boolean mHdmiPlugged;
    boolean mHideLockScreen;
    boolean mHomeConsumed;
    boolean mHomeDoubleTapPending;
    Intent mHomeIntent;
    boolean mHomePressed;
    private ImmersiveModeConfirmation mImmersiveModeConfirmation;
    int mIncallPowerBehavior;
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
    int mLastSystemUiFlags;
    boolean mLidControlsSleep;
    int mLidKeyboardAccessibility;
    int mLidNavigationAccessibility;
    int mLidOpenRotation;
    int mLockScreenTimeout;
    boolean mLockScreenTimerActive;
    private int mLongPressOnHomeBehavior;
    int mLongPressOnPowerBehavior;
    long[] mLongPressVibePattern;
    private KeyEvent mLongpressEvent;
    MyOrientationListener mOrientationListener;
    int mOverscanScreenHeight;
    int mOverscanScreenLeft;
    int mOverscanScreenTop;
    int mOverscanScreenWidth;
    boolean mPendingMetaAction;
    PointerLocationView mPointerLocationView;
    volatile boolean mPowerKeyHandled;
    volatile int mPowerKeyPressCounter;
    PowerManager.WakeLock mPowerKeyWakeLock;
    PowerManager mPowerManager;
    boolean mPreloadedRecentApps;
    int mRecentAppsHeldModifiers;
    boolean mRecentsVisible;
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
    SettingsObserver mSettingsObserver;
    int mShortPressOnPowerBehavior;
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
    IStatusBarService mStatusBarService;
    boolean mSupportAutoRotation;
    boolean mSystemBooted;
    int mSystemBottom;
    private SystemGesturesPointerEventListener mSystemGestures;
    int mSystemLeft;
    boolean mSystemReady;
    int mSystemRight;
    int mSystemTop;
    WindowManagerPolicy.WindowState mTopFullscreenOpaqueWindowState;
    boolean mTopIsFullscreen;
    int mTriplePressOnPowerBehavior;
    int mUiMode;
    IUiModeManager mUiModeManager;
    int mUndockedHdmiRotation;
    int mUnrestrictedScreenHeight;
    int mUnrestrictedScreenLeft;
    int mUnrestrictedScreenTop;
    int mUnrestrictedScreenWidth;
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
    private static final int MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK = 4;
    private static final int MSG_POWER_DELAYED_PRESS = 13;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK).setUsage(MSG_POWER_DELAYED_PRESS).build();
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
    int[] mNavigationBarHeightForRotation = new int[MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK];
    int[] mNavigationBarWidthForRotation = new int[MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK];
    final Runnable mWindowManagerDrawCallback = new Runnable() {
        @Override
        public void run() {
            PhoneWindowManager.this.mHandler.sendEmptyMessage(PhoneWindowManager.MSG_WINDOW_MANAGER_DRAWN_COMPLETE);
        }
    };
    final KeyguardServiceDelegate.ShowListener mKeyguardDelegateCallback = new KeyguardServiceDelegate.ShowListener() {
        @Override
        public void onShown(IBinder windowToken) {
            PhoneWindowManager.this.mHandler.sendEmptyMessage(PhoneWindowManager.MSG_KEYGUARD_DRAWN_COMPLETE);
        }
    };
    WindowManagerPolicy.WindowState mLastInputMethodWindow = null;
    WindowManagerPolicy.WindowState mLastInputMethodTargetWindow = null;
    int mLidState = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
    int mCameraLensCoverState = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
    int mDockMode = 0;
    private boolean mForceDefaultOrientation = false;
    int mDefaultRotation = SystemProperties.getInt("ro.wm.default_rotation", 0);
    int mUserRotationMode = 0;
    int mUserRotation = 0;
    int mAllowAllRotations = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
    boolean mOrientationSensorEnabled = false;
    int mCurrentAppOrientation = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
    boolean mHasSoftInput = false;
    boolean mTranslucentDecorEnabled = true;
    int mPointerLocationMode = 0;
    int mResettingSystemUiFlags = 0;
    int mForceClearedSystemUiFlags = 0;
    boolean mLastFocusNeedsMenu = false;
    WindowManagerPolicy.FakeWindow mHideNavFakeWindow = null;
    HashSet<IApplicationToken> mAppsToBeHidden = new HashSet<>();
    HashSet<IApplicationToken> mAppsThatDismissKeyguard = new HashSet<>();
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
    private UEventObserver mHDMIObserver = new UEventObserver() {
        public void onUEvent(UEventObserver.UEvent event) {
            PhoneWindowManager.this.setHdmiPlugged("1".equals(event.get("SWITCH_STATE")));
        }
    };
    private final BarController mStatusBarController = new BarController("StatusBar", 67108864, 268435456, 1073741824, 1, 67108864);
    private final BarController mNavigationBarController = new BarController("NavigationBar", 134217728, 536870912, Integer.MIN_VALUE, 2, 134217728);
    private final Runnable mVolumeUpDownRunnable = new Runnable() {
        @Override
        public void run() {
            boolean musicOnly = true;
            TelecomManager telecomManager = PhoneWindowManager.this.getTelecommService();
            if (telecomManager != null && telecomManager.isInCall()) {
                musicOnly = false;
            }
            MediaSessionLegacyHelper.getHelper(PhoneWindowManager.this.mContext).sendVolumeKeyEvent(PhoneWindowManager.this.mLongpressEvent, musicOnly);
            PhoneWindowManager.this.mHandler.postDelayed(PhoneWindowManager.this.mVolumeUpDownRunnable, 500L);
        }
    };
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
    private final Runnable mScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            PhoneWindowManager.this.takeScreenshot();
        }
    };
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
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.onDreamingStarted();
                }
            } else if ("android.intent.action.DREAMING_STOPPED".equals(intent.getAction()) && PhoneWindowManager.this.mKeyguardDelegate != null) {
                PhoneWindowManager.this.mKeyguardDelegate.onDreamingStopped();
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
    private final Runnable mRequestTransientNav = new Runnable() {
        @Override
        public void run() {
            PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
        }
    };
    ProgressDialog mBootMsgDialog = null;
    ScreenLockTimeout mScreenLockTimeout = new ScreenLockTimeout();

    static {
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
        WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = new int[]{2003, 2010};
    }

    private class PolicyHandler extends Handler {
        private PolicyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                    PhoneWindowManager.this.enablePointerLocation();
                    break;
                case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                    PhoneWindowManager.this.disablePointerLocation();
                    break;
                case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
                    PhoneWindowManager.this.dispatchMediaKeyWithWakeLock((KeyEvent) msg.obj);
                    break;
                case PhoneWindowManager.MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK:
                    PhoneWindowManager.this.dispatchMediaKeyRepeatWithWakeLock((KeyEvent) msg.obj);
                    break;
                case PhoneWindowManager.MSG_KEYGUARD_DRAWN_COMPLETE:
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    break;
                case PhoneWindowManager.MSG_KEYGUARD_DRAWN_TIMEOUT:
                    Slog.w(PhoneWindowManager.TAG, "Keyguard drawn timeout. Setting mKeyguardDrawComplete");
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    break;
                case PhoneWindowManager.MSG_WINDOW_MANAGER_DRAWN_COMPLETE:
                    PhoneWindowManager.this.finishWindowsDrawn();
                    break;
                case PhoneWindowManager.MSG_DISPATCH_SHOW_RECENTS:
                    PhoneWindowManager.this.showRecentApps(false);
                    break;
                case 10:
                    PhoneWindowManager.this.showGlobalActionsInternal();
                    break;
                case PhoneWindowManager.MSG_HIDE_BOOT_MESSAGE:
                    PhoneWindowManager.this.handleHideBootMessage();
                    break;
                case PhoneWindowManager.MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK:
                    PhoneWindowManager.this.launchVoiceAssistWithWakeLock(msg.arg1 != 0);
                    break;
                case PhoneWindowManager.MSG_POWER_DELAYED_PRESS:
                    PhoneWindowManager.this.powerPress(((Long) msg.obj).longValue(), msg.arg1 != 0, msg.arg2);
                    PhoneWindowManager.this.finishPowerKeyPress();
                    break;
                case PhoneWindowManager.MSG_POWER_LONG_PRESS:
                    PhoneWindowManager.this.powerLongPress();
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
            resolver.registerContentObserver(Settings.System.getUriFor("end_button_behavior"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.Secure.getUriFor("incall_power_button_behavior"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.Secure.getUriFor("wake_gesture_enabled"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.System.getUriFor("user_rotation"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.System.getUriFor("screen_off_timeout"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.System.getUriFor("pointer_location"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.Secure.getUriFor("immersive_mode_confirmations"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.Global.getUriFor("policy_control"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
            resolver.registerContentObserver(Settings.System.getUriFor("hide_navigation_bar"), false, this, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER);
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
                    PhoneWindowManager.this.wakeUp(SystemClock.uptimeMillis(), PhoneWindowManager.this.mAllowTheaterModeWakeFromWakeGesture);
                }
            }
        }
    }

    class MyOrientationListener extends WindowOrientationListener {
        MyOrientationListener(Context context, Handler handler) {
            super(context, handler);
        }

        @Override
        public void onProposedRotationChanged(int rotation) {
            PhoneWindowManager.this.updateRotation(false);
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

    boolean needSensorRunningLp() {
        if (this.mSupportAutoRotation && (this.mCurrentAppOrientation == MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK || this.mCurrentAppOrientation == 10 || this.mCurrentAppOrientation == MSG_WINDOW_MANAGER_DRAWN_COMPLETE || this.mCurrentAppOrientation == MSG_KEYGUARD_DRAWN_TIMEOUT)) {
            return true;
        }
        if (this.mCarDockEnablesAccelerometer && this.mDockMode == 2) {
            return true;
        }
        if (this.mDeskDockEnablesAccelerometer && (this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK)) {
            return true;
        }
        if (this.mUserRotationMode == 1) {
            return false;
        }
        return this.mSupportAutoRotation;
    }

    void updateOrientationListenerLp() {
        if (this.mOrientationListener.canDetectOrientation()) {
            boolean disable = true;
            if (this.mScreenOnEarly && this.mAwake && needSensorRunningLp()) {
                disable = false;
                if (!this.mOrientationSensorEnabled) {
                    this.mOrientationListener.enable();
                    this.mOrientationSensorEnabled = true;
                }
            }
            if (disable && this.mOrientationSensorEnabled) {
                this.mOrientationListener.disable();
                this.mOrientationSensorEnabled = false;
            }
        }
    }

    private void interceptPowerKeyDown(KeyEvent event, boolean interactive) {
        if (!this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.acquire();
        }
        if (this.mPowerKeyPressCounter != 0) {
            this.mHandler.removeMessages(MSG_POWER_DELAYED_PRESS);
        }
        boolean panic = this.mImmersiveModeConfirmation.onPowerKeyDown(interactive, event.getDownTime(), isImmersiveMode(this.mLastSystemUiFlags));
        if (panic) {
            this.mHandler.post(this.mRequestTransientNav);
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
        this.mPowerKeyHandled = hungUp || this.mScreenshotChordVolumeDownKeyTriggered || this.mScreenshotChordVolumeUpKeyTriggered;
        if (!this.mPowerKeyHandled) {
            if (interactive) {
                if (hasLongPressOnPowerBehavior()) {
                    Message msg = this.mHandler.obtainMessage(MSG_POWER_LONG_PRESS);
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageDelayed(msg, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                    return;
                }
                return;
            }
            wakeUpFromPowerKey(event.getDownTime());
            int maxCount = getMaxMultiPressPowerCount();
            if (maxCount <= 1) {
                this.mPowerKeyHandled = true;
            } else {
                this.mBeganFromNonInteractive = true;
            }
        }
    }

    private void interceptPowerKeyUp(KeyEvent event, boolean interactive, boolean canceled) {
        boolean handled = canceled || this.mPowerKeyHandled;
        this.mScreenshotChordPowerKeyTriggered = false;
        cancelPendingScreenshotChordAction();
        cancelPendingPowerKeyAction();
        if (!handled) {
            this.mPowerKeyPressCounter++;
            int maxCount = getMaxMultiPressPowerCount();
            long eventTime = event.getDownTime();
            if (this.mPowerKeyPressCounter < maxCount) {
                Message msg = this.mHandler.obtainMessage(MSG_POWER_DELAYED_PRESS, interactive ? 1 : 0, this.mPowerKeyPressCounter, Long.valueOf(eventTime));
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
        if (this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.release();
        }
    }

    private void cancelPendingPowerKeyAction() {
        if (!this.mPowerKeyHandled) {
            this.mPowerKeyHandled = true;
            this.mHandler.removeMessages(MSG_POWER_LONG_PRESS);
        }
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
        if (interactive && !this.mBeganFromNonInteractive) {
            switch (this.mShortPressOnPowerBehavior) {
                case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                    this.mPowerManager.goToSleep(eventTime, MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK, 0);
                    break;
                case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                    this.mPowerManager.goToSleep(eventTime, MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK, 1);
                    break;
                case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
                    this.mPowerManager.goToSleep(eventTime, MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK, 1);
                    launchHomeFromHotKey();
                    break;
            }
        }
    }

    private void powerMultiPressAction(long eventTime, boolean interactive, int behavior) {
        switch (behavior) {
            case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                if (isTheaterModeEnabled()) {
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
                        this.mPowerManager.goToSleep(eventTime, MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK, 0);
                        break;
                    }
                }
                break;
            case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                Slog.i(TAG, "Starting brightness boost.");
                if (!interactive) {
                    wakeUpFromPowerKey(eventTime);
                }
                this.mPowerManager.boostScreenBrightness(eventTime);
                break;
        }
    }

    private void cancelVolumeUpDownRunnable() {
        this.mHandler.removeCallbacks(this.mVolumeUpDownRunnable);
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
            case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                this.mPowerKeyHandled = true;
                if (!performHapticFeedbackLw(null, 0, false)) {
                    performAuditoryFeedbackForAccessibilityIfNeed();
                }
                showGlobalActionsInternal();
                break;
            case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
            case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                this.mWindowManagerFuncs.shutdown(behavior == 2);
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

    private void interceptScreenshotChord() {
        String ScreenCaptureOn = Settings.System.getString(this.mContext.getContentResolver(), "screen_capture_on");
        if (ScreenCaptureOn.equals("1") && this.mScreenshotChordVolumeDownKeyTriggered && this.mScreenshotChordPowerKeyTriggered && !this.mScreenshotChordVolumeUpKeyTriggered) {
            long now = SystemClock.uptimeMillis();
            if (now <= this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS && now <= this.mScreenshotChordPowerKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mScreenshotChordVolumeDownKeyConsumed = true;
                cancelPendingPowerKeyAction();
                this.mHandler.postDelayed(this.mScreenshotRunnable, getScreenshotChordLongPressDelay());
            }
        }
    }

    private long getScreenshotChordLongPressDelay() {
        return this.mKeyguardDelegate.isShowing() ? (long) (KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER * ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout()) : ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout();
    }

    private void cancelPendingScreenshotChordAction() {
        this.mHandler.removeCallbacks(this.mScreenshotRunnable);
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
        if (keyguardShowing) {
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, APPLICATION_MEDIA_SUBLAYER) != 0;
    }

    private void handleShortPressOnHome() {
        if (this.mDreamManagerInternal != null && this.mDreamManagerInternal.isDreaming()) {
            this.mDreamManagerInternal.stopDream(false);
        } else {
            launchHomeFromHotKey();
        }
    }

    private void handleLongPressOnHome() {
        if (this.mLongPressOnHomeBehavior != 0) {
            this.mHomeConsumed = true;
            performHapticFeedbackLw(null, 0, false);
            if (this.mLongPressOnHomeBehavior == 1) {
                toggleRecentApps();
            } else if (this.mLongPressOnHomeBehavior == 2) {
                launchAssistAction();
            }
        }
    }

    private void handleDoubleTapOnHome() {
        if (this.mDoubleTapOnHomeBehavior == 1) {
            this.mHomeConsumed = true;
            toggleRecentApps();
        }
    }

    public void init(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) throws Throwable {
        this.mContext = context;
        this.mWindowManager = windowManager;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
        this.mHandler = new PolicyHandler();
        this.mWakeGestureListener = new MyWakeGestureListener(this.mContext, this.mHandler);
        this.mOrientationListener = new MyOrientationListener(this.mContext, this.mHandler);
        try {
            this.mOrientationListener.setCurrentRotation(windowManager.getRotation());
        } catch (RemoteException e) {
        }
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
        this.mShortcutManager = new ShortcutManager(context, this.mHandler);
        this.mShortcutManager.observe();
        this.mUiMode = context.getResources().getInteger(R.integer.config_burnInProtectionMinHorizontalOffset);
        this.mHomeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mHomeIntent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270532608);
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
        this.mSupportAutoRotation = this.mContext.getResources().getBoolean(R.^attr-private.colorSurfaceHeader);
        this.mLidOpenRotation = readRotation(R.integer.config_bluetooth_operating_voltage_mv);
        this.mCarDockRotation = readRotation(R.integer.config_burnInProtectionMaxHorizontalOffset);
        this.mDeskDockRotation = readRotation(R.integer.config_brightness_ramp_rate_fast);
        this.mUndockedHdmiRotation = readRotation(R.integer.config_burnInProtectionMaxVerticalOffset);
        this.mCarDockEnablesAccelerometer = this.mContext.getResources().getBoolean(R.^attr-private.cornerRadius);
        this.mDeskDockEnablesAccelerometer = this.mContext.getResources().getBoolean(R.^attr-private.controllerType);
        this.mLidKeyboardAccessibility = this.mContext.getResources().getInteger(R.integer.config_bluetooth_rx_cur_ma);
        this.mLidNavigationAccessibility = this.mContext.getResources().getInteger(R.integer.config_bluetooth_tx_cur_ma);
        this.mLidControlsSleep = this.mContext.getResources().getBoolean(R.^attr-private.colorSwitchThumbNormal);
        this.mTranslucentDecorEnabled = this.mContext.getResources().getBoolean(R.^attr-private.dotActivatedColor);
        this.mAllowTheaterModeWakeFromKey = this.mContext.getResources().getBoolean(R.^attr-private.colorAccentSecondaryVariant);
        this.mAllowTheaterModeWakeFromPowerKey = this.mAllowTheaterModeWakeFromKey || this.mContext.getResources().getBoolean(R.^attr-private.colorAccentSecondary);
        this.mAllowTheaterModeWakeFromMotion = this.mContext.getResources().getBoolean(R.^attr-private.colorAccentTertiary);
        this.mAllowTheaterModeWakeFromMotionWhenNotDreaming = this.mContext.getResources().getBoolean(R.^attr-private.colorAccentTertiaryVariant);
        this.mAllowTheaterModeWakeFromCameraLens = this.mContext.getResources().getBoolean(R.^attr-private.colorAccentPrimaryVariant);
        this.mAllowTheaterModeWakeFromLidSwitch = this.mContext.getResources().getBoolean(R.^attr-private.colorListDivider);
        this.mAllowTheaterModeWakeFromWakeGesture = this.mContext.getResources().getBoolean(R.^attr-private.colorAccentPrimary);
        this.mGoToSleepOnButtonPressTheaterMode = this.mContext.getResources().getBoolean(R.^attr-private.colorSurface);
        this.mShortPressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_cameraLaunchGestureSensorType);
        this.mLongPressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_burnInProtectionMinVerticalOffset);
        this.mDoublePressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_cameraLiftTriggerSensorType);
        this.mTriplePressOnPowerBehavior = this.mContext.getResources().getInteger(R.integer.config_cameraPrivacyLightAlsAveragingIntervalMillis);
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
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.DREAMING_STARTED");
        filter2.addAction("android.intent.action.DREAMING_STOPPED");
        context.registerReceiver(this.mDreamReceiver, filter2);
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
                if (PhoneWindowManager.this.mNavigationBar != null && PhoneWindowManager.this.mNavigationBarOnBottom && PhoneWindowManager.this.mHideNavigationBar == 0) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
                }
            }

            @Override
            public void onSwipeFromRight() {
                if (PhoneWindowManager.this.mNavigationBar != null && !PhoneWindowManager.this.mNavigationBarOnBottom && PhoneWindowManager.this.mHideNavigationBar == 0) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
                }
            }

            @Override
            public void onDebug() {
            }
        });
        this.mImmersiveModeConfirmation = new ImmersiveModeConfirmation(this.mContext);
        this.mWindowManagerFuncs.registerPointerEventListener(this.mSystemGestures);
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mLongPressVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_autoRotationTiltTolerance);
        this.mVirtualKeyVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_autoTimeSourcesPriority);
        this.mKeyboardTapVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_availableColorModes);
        this.mClockTickVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_availableEMValueOptions);
        this.mCalendarDateVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_backGestureInsetScales);
        this.mSafeModeDisabledVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_backupHealthConnectDataAndSettingsKnownSigners);
        this.mSafeModeEnabledVibePattern = getLongIntArray(this.mContext.getResources(), R.array.config_batteryPackageTypeService);
        this.mScreenshotChordEnabled = this.mContext.getResources().getBoolean(R.^attr-private.clickColor);
        this.mGlobalKeyManager = new GlobalKeyManager(this.mContext);
        initializeHdmiState();
        if (!this.mPowerManager.isInteractive()) {
            goingToSleep(2);
        }
    }

    private void readConfigurationDependentBehaviors() {
        this.mLongPressOnHomeBehavior = this.mContext.getResources().getInteger(R.integer.config_datause_throttle_kbitsps);
        if (this.mLongPressOnHomeBehavior < 0 || this.mLongPressOnHomeBehavior > 2) {
            this.mLongPressOnHomeBehavior = 0;
        }
        this.mDoubleTapOnHomeBehavior = this.mContext.getResources().getInteger(R.integer.config_debugSystemServerPssThresholdBytes);
        if (this.mDoubleTapOnHomeBehavior < 0 || this.mDoubleTapOnHomeBehavior > 1) {
            this.mDoubleTapOnHomeBehavior = 0;
        }
    }

    public void setInitialDisplaySize(Display display, int width, int height, int density) {
        int shortSize;
        int longSize;
        if (this.mContext != null && display.getDisplayId() == 0) {
            this.mDisplay = display;
            Resources res = this.mContext.getResources();
            if (width > height) {
                shortSize = height;
                longSize = width;
                this.mLandscapeRotation = 0;
                this.mSeascapeRotation = 2;
                if (res.getBoolean(R.^attr-private.colorSurfaceVariant)) {
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
                if (res.getBoolean(R.^attr-private.colorSurfaceVariant)) {
                    this.mLandscapeRotation = 3;
                    this.mSeascapeRotation = 1;
                } else {
                    this.mLandscapeRotation = 1;
                    this.mSeascapeRotation = 3;
                }
            }
            this.mStatusBarHeight = res.getDimensionPixelSize(R.dimen.accessibility_focus_highlight_stroke_width);
            int[] iArr = this.mNavigationBarHeightForRotation;
            int i = this.mPortraitRotation;
            int[] iArr2 = this.mNavigationBarHeightForRotation;
            int i2 = this.mUpsideDownRotation;
            int dimensionPixelSize = res.getDimensionPixelSize(R.dimen.accessibility_fullscreen_magnification_gesture_edge_slop);
            iArr2[i2] = dimensionPixelSize;
            iArr[i] = dimensionPixelSize;
            int[] iArr3 = this.mNavigationBarHeightForRotation;
            int i3 = this.mLandscapeRotation;
            int[] iArr4 = this.mNavigationBarHeightForRotation;
            int i4 = this.mSeascapeRotation;
            int dimensionPixelSize2 = res.getDimensionPixelSize(R.dimen.accessibility_icon_foreground_padding_ratio);
            iArr4[i4] = dimensionPixelSize2;
            iArr3[i3] = dimensionPixelSize2;
            int[] iArr5 = this.mNavigationBarWidthForRotation;
            int i5 = this.mPortraitRotation;
            int[] iArr6 = this.mNavigationBarWidthForRotation;
            int i6 = this.mUpsideDownRotation;
            int[] iArr7 = this.mNavigationBarWidthForRotation;
            int i7 = this.mLandscapeRotation;
            int[] iArr8 = this.mNavigationBarWidthForRotation;
            int i8 = this.mSeascapeRotation;
            int dimensionPixelSize3 = res.getDimensionPixelSize(R.dimen.accessibility_magnification_indicator_width);
            iArr8[i8] = dimensionPixelSize3;
            iArr7[i7] = dimensionPixelSize3;
            iArr6[i6] = dimensionPixelSize3;
            iArr5[i5] = dimensionPixelSize3;
            int shortSizeDp = (shortSize * 160) / density;
            int longSizeDp = (longSize * 160) / density;
            this.mNavigationBarCanMove = shortSizeDp < 600;
            this.mHasNavigationBar = res.getBoolean(R.^attr-private.fromBottom);
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
            this.mForceDefaultOrientation = longSizeDp >= 960 && shortSizeDp >= 720 && res.getBoolean(R.^attr-private.itemColor) && !"true".equals(SystemProperties.get("config.override_forced_orient"));
        }
    }

    private boolean canHideNavigationBar() {
        return this.mHasNavigationBar && !this.mAccessibilityManager.isTouchExplorationEnabled();
    }

    public boolean isDefaultOrientationForced() {
        return this.mForceDefaultOrientation;
    }

    public void setDisplayOverscan(Display display, int left, int top, int right, int bottom) {
        if (display.getDisplayId() == 0) {
            this.mOverscanLeft = left;
            this.mOverscanTop = top;
            this.mOverscanRight = right;
            this.mOverscanBottom = bottom;
        }
    }

    public void updateSettings() {
        int pointerLocation;
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean updateRotation = false;
        synchronized (this.mLock) {
            this.mEndcallBehavior = Settings.System.getIntForUser(resolver, "end_button_behavior", 2, APPLICATION_MEDIA_SUBLAYER);
            this.mIncallPowerBehavior = Settings.Secure.getIntForUser(resolver, "incall_power_button_behavior", 1, APPLICATION_MEDIA_SUBLAYER);
            boolean wakeGestureEnabledSetting = Settings.Secure.getIntForUser(resolver, "wake_gesture_enabled", 0, APPLICATION_MEDIA_SUBLAYER) != 0;
            if (this.mWakeGestureEnabledSetting != wakeGestureEnabledSetting) {
                this.mWakeGestureEnabledSetting = wakeGestureEnabledSetting;
                updateWakeGestureListenerLp();
            }
            int userRotation = Settings.System.getIntForUser(resolver, "user_rotation", 0, APPLICATION_MEDIA_SUBLAYER);
            if (this.mUserRotation != userRotation) {
                this.mUserRotation = userRotation;
                updateRotation = true;
            }
            int userRotationMode = Settings.System.getIntForUser(resolver, "accelerometer_rotation", 0, APPLICATION_MEDIA_SUBLAYER) != 0 ? 0 : 1;
            if (this.mUserRotationMode != userRotationMode) {
                this.mUserRotationMode = userRotationMode;
                updateRotation = true;
                updateOrientationListenerLp();
            }
            if (this.mSystemReady && this.mPointerLocationMode != (pointerLocation = Settings.System.getIntForUser(resolver, "pointer_location", 0, APPLICATION_MEDIA_SUBLAYER))) {
                this.mPointerLocationMode = pointerLocation;
                this.mHandler.sendEmptyMessage(pointerLocation != 0 ? 1 : 2);
            }
            this.mLockScreenTimeout = Settings.System.getIntForUser(resolver, "screen_off_timeout", 0, APPLICATION_MEDIA_SUBLAYER);
            String imId = Settings.Secure.getStringForUser(resolver, "default_input_method", APPLICATION_MEDIA_SUBLAYER);
            boolean hasSoftInput = imId != null && imId.length() > 0;
            if (this.mHasSoftInput != hasSoftInput) {
                this.mHasSoftInput = hasSoftInput;
                updateRotation = true;
            }
            int hideNavigationBar = Settings.System.getIntForUser(resolver, "hide_navigation_bar", 0, APPLICATION_MEDIA_SUBLAYER);
            if (hideNavigationBar == 1) {
                int forceShowNavigationBar = 1;
                if (Settings.System.getIntForUser(resolver, "dcha_state", 0, APPLICATION_MEDIA_SUBLAYER) != 3 || Settings.Global.getInt(resolver, "adb_enabled", 0) != 1) {
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
            PolicyControl.reloadFromSetting(this.mContext);
        }
        if (updateRotation) {
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
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(APPLICATION_MEDIA_OVERLAY_SUBLAYER, APPLICATION_MEDIA_OVERLAY_SUBLAYER);
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
    }

    private void disablePointerLocation() {
        if (this.mPointerLocationView != null) {
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.mPointerLocationView);
            WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
            wm.removeView(this.mPointerLocationView);
            this.mPointerLocationView = null;
        }
    }

    private int readRotation(int resID) {
        int rotation;
        try {
            rotation = this.mContext.getResources().getInteger(resID);
        } catch (Resources.NotFoundException e) {
        }
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
                return APPLICATION_MEDIA_OVERLAY_SUBLAYER;
        }
    }

    public int checkAddPermission(WindowManager.LayoutParams attrs, int[] outAppOp) {
        int type = attrs.type;
        outAppOp[0] = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
        if ((type < 1 || type > 99) && ((type < WAITING_FOR_DRAWN_TIMEOUT || type > 1999) && (type < 2000 || type > 2999))) {
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
                break;
            default:
                permission = "android.permission.INTERNAL_SYSTEM_WINDOW";
                break;
        }
        return (permission == null || this.mContext.checkCallingOrSelfPermission(permission) == 0) ? 0 : -8;
    }

    public boolean checkShowToOwnerOnly(WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
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
            case 2025:
            case 2026:
            case 2027:
            case 2029:
            case 2030:
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
        if (ActivityManager.isHighEndGfx() && (attrs.flags & Integer.MIN_VALUE) != 0) {
            attrs.subtreeSystemUiVisibility |= 1536;
        }
    }

    void readLidState() {
        this.mLidState = this.mWindowManagerFuncs.getLidState();
    }

    private void readCameraLensCoverState() {
        this.mCameraLensCoverState = this.mWindowManagerFuncs.getCameraLensCoverState();
    }

    private boolean isHidden(int accessibilityMode) {
        switch (accessibilityMode) {
            case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                return this.mLidState == 0;
            case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                return this.mLidState == 1;
            default:
                return false;
        }
    }

    public void adjustConfigurationLw(Configuration config, int keyboardPresence, int navigationPresence) {
        this.mHaveBuiltInKeyboard = (keyboardPresence & 1) != 0;
        readConfigurationDependentBehaviors();
        readLidState();
        applyLidSwitchState();
        if (config.keyboard == 1 || (keyboardPresence == 1 && isHidden(this.mLidKeyboardAccessibility))) {
            config.hardKeyboardHidden = 2;
            if (!this.mHasSoftInput) {
                config.keyboardHidden = 2;
            }
        }
        if (config.navigation == 1 || (navigationPresence == 1 && isHidden(this.mLidNavigationAccessibility))) {
            config.navigationHidden = 2;
        }
    }

    public int windowTypeToLayerLw(int type) {
        if (type >= 1 && type <= 99) {
            return 2;
        }
        switch (type) {
            case 2000:
                break;
            case 2001:
                break;
            case 2002:
                break;
            case 2003:
                break;
            case 2004:
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
            case 2030:
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
            case 2025:
                break;
            case 2026:
                break;
            case 2027:
                break;
            case 2029:
                break;
            case 2031:
                break;
            case 2032:
                break;
        }
        return 2;
    }

    public int subWindowTypeToLayerLw(int type) {
        switch (type) {
            case WAITING_FOR_DRAWN_TIMEOUT:
            case 1003:
                return 1;
            case 1001:
                return APPLICATION_MEDIA_SUBLAYER;
            case 1002:
                return 2;
            case 1004:
                return APPLICATION_MEDIA_OVERLAY_SUBLAYER;
            default:
                Log.e(TAG, "Unknown sub-window type: " + type);
                return 0;
        }
    }

    public int getMaxWallpaperLayer() {
        return windowTypeToLayerLw(2000);
    }

    public int getAboveUniverseLayer() {
        return windowTypeToLayerLw(2010);
    }

    public int getNonDecorDisplayWidth(int fullWidth, int fullHeight, int rotation) {
        if (this.mHasNavigationBar && this.mNavigationBarCanMove && fullWidth > fullHeight) {
            return fullWidth - this.mNavigationBarWidthForRotation[rotation];
        }
        return fullWidth;
    }

    public int getNonDecorDisplayHeight(int fullWidth, int fullHeight, int rotation) {
        if (this.mHasNavigationBar) {
            if (!this.mNavigationBarCanMove || fullWidth < fullHeight) {
                return fullHeight - this.mNavigationBarHeightForRotation[rotation];
            }
            return fullHeight;
        }
        return fullHeight;
    }

    public int getConfigDisplayWidth(int fullWidth, int fullHeight, int rotation) {
        return getNonDecorDisplayWidth(fullWidth, fullHeight, rotation);
    }

    public int getConfigDisplayHeight(int fullWidth, int fullHeight, int rotation) {
        return getNonDecorDisplayHeight(fullWidth, fullHeight, rotation) - this.mStatusBarHeight;
    }

    public boolean isForceHiding(WindowManager.LayoutParams attrs) {
        return (attrs.privateFlags & 1024) != 0 || (isKeyguardHostWindow(attrs) && this.mKeyguardDelegate != null && this.mKeyguardDelegate.isShowing()) || attrs.type == 2029;
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
            case 2025:
            case 2029:
                return false;
            default:
                return true;
        }
    }

    public WindowManagerPolicy.WindowState getWinShowWhenLockedLw() {
        return this.mWinShowWhenLocked;
    }

    public View addStartingWindow(IBinder appToken, String packageName, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags) {
        if (packageName == null) {
            return null;
        }
        WindowManager wm = null;
        View view = null;
        try {
            try {
                Context context = this.mContext;
                if (theme != context.getThemeResId() || labelRes != 0) {
                    try {
                        context = context.createPackageContext(packageName, 0);
                        context.setTheme(theme);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                Window win = PolicyManager.makeNewWindow(context);
                TypedArray ta = win.getWindowStyle();
                if (ta.getBoolean(MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK, false) || ta.getBoolean(MSG_POWER_LONG_PRESS, false)) {
                    if (0 == 0 || view.getParent() != null) {
                        return null;
                    }
                    Log.w(TAG, "view not successfully added to wm, removing view");
                    wm.removeViewImmediate(null);
                    return null;
                }
                Resources r = context.getResources();
                win.setTitle(r.getText(labelRes, nonLocalizedLabel));
                win.setType(3);
                win.setFlags(windowFlags | 16 | 8 | 131072, windowFlags | 16 | 8 | 131072);
                win.setDefaultIcon(icon);
                win.setDefaultLogo(logo);
                win.setLayout(APPLICATION_MEDIA_OVERLAY_SUBLAYER, APPLICATION_MEDIA_OVERLAY_SUBLAYER);
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
                wm = (WindowManager) context.getSystemService("window");
                view = win.getDecorView();
                if (win.isFloating()) {
                    if (view == null || view.getParent() != null) {
                        return null;
                    }
                    Log.w(TAG, "view not successfully added to wm, removing view");
                    wm.removeViewImmediate(view);
                    return null;
                }
                wm.addView(view, params);
                View view2 = view.getParent() != null ? view : null;
                if (view == null || view.getParent() != null) {
                    return view2;
                }
                Log.w(TAG, "view not successfully added to wm, removing view");
                wm.removeViewImmediate(view);
                return view2;
            } catch (WindowManager.BadTokenException e2) {
                Log.w(TAG, appToken + " already running, starting window not displayed. " + e2.getMessage());
                if (view != null && view.getParent() == null) {
                    Log.w(TAG, "view not successfully added to wm, removing view");
                    wm.removeViewImmediate(view);
                }
                return null;
            } catch (RuntimeException e3) {
                Log.w(TAG, appToken + " failed creating starting window", e3);
                if (view != null && view.getParent() == null) {
                    Log.w(TAG, "view not successfully added to wm, removing view");
                    wm.removeViewImmediate(view);
                }
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

    public void removeStartingWindow(IBinder appToken, View window) {
        if (window != null) {
            WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
            wm.removeView(window);
        }
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
                this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
                return 0;
            case 2017:
                this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
                return 0;
            case 2019:
                this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
                if (this.mNavigationBar != null && this.mNavigationBar.isAlive()) {
                    return -7;
                }
                this.mNavigationBar = win;
                this.mNavigationBarController.setWindow(win);
                return 0;
            case 2024:
                this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
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
        if (this.mNavigationBar == win) {
            this.mNavigationBar = null;
            this.mNavigationBarController.setWindow(null);
        }
    }

    public int selectAnimationLw(WindowManagerPolicy.WindowState win, int transit) {
        if (win == this.mStatusBar) {
            boolean isKeyguard = (win.getAttrs().privateFlags & 1024) != 0;
            if (transit == 2 || transit == MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK) {
                return isKeyguard ? APPLICATION_MEDIA_OVERLAY_SUBLAYER : R.anim.btn_radio_to_on_mtrl_dot_group_animation;
            }
            if (transit == 1 || transit == 3) {
                return !isKeyguard ? R.anim.btn_radio_to_off_mtrl_ring_outer_path_animation : APPLICATION_MEDIA_OVERLAY_SUBLAYER;
            }
        } else if (win == this.mNavigationBar) {
            if (this.mNavigationBarOnBottom) {
                if (transit == 2 || transit == MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK) {
                    return R.anim.btn_checkbox_to_unchecked_box_inner_merged_animation;
                }
                if (transit == 1 || transit == 3) {
                    return R.anim.btn_checkbox_to_checked_icon_null_animation;
                }
            } else {
                if (transit == 2 || transit == MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK) {
                    return R.anim.btn_radio_to_off_mtrl_ring_outer_animation;
                }
                if (transit == 1 || transit == 3) {
                    return R.anim.btn_radio_to_off_mtrl_dot_group_animation;
                }
            }
        }
        if (transit == MSG_KEYGUARD_DRAWN_COMPLETE) {
            if (win.hasAppShownWindows()) {
                return R.anim.activity_translucent_close_exit;
            }
        } else if (win.getAttrs().type == 2023 && this.mDreamingLockscreen && transit == 1) {
            return APPLICATION_MEDIA_OVERLAY_SUBLAYER;
        }
        return 0;
    }

    public void selectRotationAnimationLw(int[] anim) {
        if (this.mTopFullscreenOpaqueWindowState != null && this.mTopIsFullscreen) {
            switch (this.mTopFullscreenOpaqueWindowState.getAttrs().rotationAnimation) {
                case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                    anim[0] = 17432645;
                    anim[1] = 17432643;
                    break;
                case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                    anim[0] = 17432644;
                    anim[1] = 17432643;
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
            case R.anim.ft_avd_tooverflow_rectangle_path_1_animation:
            case R.anim.ft_avd_tooverflow_rectangle_path_2_animation:
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
        if (goingToNotificationShade) {
            return AnimationUtils.loadAnimation(this.mContext, R.anim.ft_avd_toarrow_rectangle_1_animation);
        }
        AnimationSet set = (AnimationSet) AnimationUtils.loadAnimation(this.mContext, onWallpaper ? R.anim.ft_avd_toarrow_rectangle_1_pivot_0_animation : R.anim.flat_button_state_list_anim_material);
        List<Animation> animations = set.getAnimations();
        for (int i = animations.size() + APPLICATION_MEDIA_OVERLAY_SUBLAYER; i >= 0; i += APPLICATION_MEDIA_OVERLAY_SUBLAYER) {
            animations.get(i).setInterpolator(this.mLogDecelerateInterpolator);
        }
        return set;
    }

    public Animation createForceHideWallpaperExitAnimation(boolean goingToNotificationShade) {
        if (goingToNotificationShade) {
            return null;
        }
        return AnimationUtils.loadAnimation(this.mContext, R.anim.ft_avd_toarrow_rectangle_2_pivot_0_animation);
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
        IAudioService audioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
        if (audioService == null) {
            Log.w(TAG, "Unable to find IAudioService interface.");
        }
        return audioService;
    }

    boolean keyguardOn() {
        return isKeyguardShowingAndNotOccluded() || inKeyguardRestrictedKeyInputMode();
    }

    public long interceptKeyBeforeDispatching(WindowManagerPolicy.WindowState win, KeyEvent event, int policyFlags) {
        Intent voiceIntent;
        String category;
        Intent shortcutIntent;
        boolean keyguardOn = keyguardOn();
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        int metaState = event.getMetaState();
        int flags = event.getFlags();
        boolean down = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        String ScreenCaptureOn = Settings.System.getString(this.mContext.getContentResolver(), "screen_capture_on");
        if (ScreenCaptureOn.equals("1") && (flags & 1024) == 0) {
            if (this.mScreenshotChordVolumeDownKeyTriggered && !this.mScreenshotChordPowerKeyTriggered) {
                long now = SystemClock.uptimeMillis();
                long timeoutTime = this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (now < timeoutTime) {
                    return timeoutTime - now;
                }
            }
            if (keyCode == 25 && this.mScreenshotChordVolumeDownKeyConsumed) {
                if (!down) {
                    this.mScreenshotChordVolumeDownKeyConsumed = false;
                }
                return -1L;
            }
        }
        if (this.mPendingMetaAction && !KeyEvent.isMetaKey(keyCode)) {
            this.mPendingMetaAction = false;
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
                if (this.mDoubleTapOnHomeBehavior != 0) {
                    this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                    this.mHomeDoubleTapPending = true;
                    this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, ViewConfiguration.getDoubleTapTimeout());
                    return -1L;
                }
                handleShortPressOnHome();
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
            if (repeatCount == 0) {
                this.mHomePressed = true;
                if (this.mHomeDoubleTapPending) {
                    this.mHomeDoubleTapPending = false;
                    this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                    handleDoubleTapOnHome();
                } else if (this.mLongPressOnHomeBehavior == 1 || this.mDoubleTapOnHomeBehavior == 1) {
                    preloadRecentApps();
                }
            } else if ((event.getFlags() & 128) != 0 && !keyguardOn) {
                handleLongPressOnHome();
            }
            return -1L;
        }
        if (keyCode == 82) {
            if (down && repeatCount == 0 && this.mEnableShiftMenuBugReports && (metaState & 1) == 1) {
                this.mContext.sendOrderedBroadcastAsUser(new Intent("android.intent.action.BUG_REPORT"), UserHandle.CURRENT, null, null, null, 0, null, null);
                return -1L;
            }
        } else {
            if (keyCode == 84) {
                if (down) {
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
                if (!keyguardOn) {
                    if (down && repeatCount == 0) {
                        preloadRecentApps();
                    } else if (!down) {
                        toggleRecentApps();
                    }
                }
                return -1L;
            }
            if (keyCode == 219) {
                if (down) {
                    if (repeatCount == 0) {
                        this.mAssistKeyLongPressed = false;
                    } else if (repeatCount == 1) {
                        this.mAssistKeyLongPressed = true;
                        if (!keyguardOn) {
                            launchAssistLongPressAction();
                        }
                    }
                } else if (this.mAssistKeyLongPressed) {
                    this.mAssistKeyLongPressed = false;
                } else if (!keyguardOn) {
                    launchAssistAction();
                }
                return -1L;
            }
            if (keyCode == 231) {
                if (!down) {
                    if (!keyguardOn) {
                        voiceIntent = new Intent("android.speech.action.WEB_SEARCH");
                    } else {
                        voiceIntent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
                        voiceIntent.putExtra("android.speech.extras.EXTRA_SECURE", true);
                    }
                    startActivityAsUser(voiceIntent, UserHandle.CURRENT_OR_SELF);
                }
            } else {
                if (keyCode == 120) {
                    if (down && repeatCount == 0 && ScreenCaptureOn.equals("1")) {
                        this.mHandler.post(this.mScreenshotRunnable);
                    }
                    return -1L;
                }
                if (keyCode == 221 || keyCode == 220) {
                    if (down) {
                        int direction = keyCode == 221 ? 1 : APPLICATION_MEDIA_OVERLAY_SUBLAYER;
                        int auto = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                        if (auto != 0) {
                            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                        }
                        int min = this.mPowerManager.getMinimumScreenBrightnessSetting();
                        int max = this.mPowerManager.getMaximumScreenBrightnessSetting();
                        int step = ((((max - min) + 10) + APPLICATION_MEDIA_OVERLAY_SUBLAYER) / 10) * direction;
                        int brightness = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mPowerManager.getDefaultScreenBrightnessSetting(), -3);
                        Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", Math.max(min, Math.min(max, brightness + step)), -3);
                        startActivityAsUser(new Intent("android.intent.action.SHOW_BRIGHTNESS_DIALOG"), UserHandle.CURRENT_OR_SELF);
                    }
                    return -1L;
                }
                if (KeyEvent.isMetaKey(keyCode)) {
                    if (down) {
                        this.mPendingMetaAction = true;
                    } else if (this.mPendingMetaAction) {
                        launchAssistAction("android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD");
                    }
                    return -1L;
                }
            }
        }
        if (this.mSearchKeyShortcutPending) {
            KeyCharacterMap kcm = event.getKeyCharacterMap();
            if (kcm.isPrintingKey(keyCode)) {
                this.mConsumeSearchKeyUp = true;
                this.mSearchKeyShortcutPending = false;
                if (down && repeatCount == 0 && !keyguardOn) {
                    Intent shortcutIntent2 = this.mShortcutManager.getIntent(kcm, keyCode, metaState);
                    if (shortcutIntent2 != null) {
                        shortcutIntent2.addFlags(268435456);
                        try {
                            startActivityAsUser(shortcutIntent2, UserHandle.CURRENT);
                        } catch (ActivityNotFoundException ex) {
                            Slog.w(TAG, "Dropping shortcut key combination because the activity to which it is registered was not found: SEARCH+" + KeyEvent.keyCodeToString(keyCode), ex);
                        }
                    } else {
                        Slog.i(TAG, "Dropping unregistered shortcut key combination: SEARCH+" + KeyEvent.keyCodeToString(keyCode));
                    }
                }
                return -1L;
            }
        }
        if (down && repeatCount == 0 && !keyguardOn && (65536 & metaState) != 0) {
            KeyCharacterMap kcm2 = event.getKeyCharacterMap();
            if (kcm2.isPrintingKey(keyCode) && (shortcutIntent = this.mShortcutManager.getIntent(kcm2, keyCode, (-458753) & metaState)) != null) {
                shortcutIntent.addFlags(268435456);
                try {
                    startActivityAsUser(shortcutIntent, UserHandle.CURRENT);
                } catch (ActivityNotFoundException ex2) {
                    Slog.w(TAG, "Dropping shortcut key combination because the activity to which it is registered was not found: META+" + KeyEvent.keyCodeToString(keyCode), ex2);
                }
                return -1L;
            }
        }
        if (down && repeatCount == 0 && !keyguardOn && (category = sApplicationLaunchKeyCategories.get(keyCode)) != null) {
            Intent intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", category);
            intent.setFlags(268435456);
            try {
                startActivityAsUser(intent, UserHandle.CURRENT);
            } catch (ActivityNotFoundException ex3) {
                Slog.w(TAG, "Dropping application launch key because the activity to which it is registered was not found: keyCode=" + keyCode + ", category=" + category, ex3);
            }
            return -1L;
        }
        if (down && repeatCount == 0 && keyCode == 61) {
            if (this.mRecentAppsHeldModifiers == 0 && !keyguardOn && isUserSetupComplete()) {
                int shiftlessModifiers = event.getModifiers() & (-194);
                if (KeyEvent.metaStateHasModifiers(shiftlessModifiers, 2)) {
                    if (BenesseExtension.getDchaState() != 0) {
                        return -1L;
                    }
                    this.mRecentAppsHeldModifiers = shiftlessModifiers;
                    showRecentApps(true);
                    return -1L;
                }
            }
        } else if (!down && this.mRecentAppsHeldModifiers != 0 && (this.mRecentAppsHeldModifiers & metaState) == 0) {
            this.mRecentAppsHeldModifiers = 0;
            hideRecentApps(true, false);
        }
        if (down && repeatCount == 0 && (keyCode == 204 || (keyCode == 62 && (metaState & 28672) != 0))) {
            int direction2 = (metaState & 193) != 0 ? APPLICATION_MEDIA_OVERLAY_SUBLAYER : 1;
            this.mWindowManagerFuncs.switchKeyboardLayout(event.getDeviceId(), direction2);
            return -1L;
        }
        if (this.mLanguageSwitchKeyPressed && !down && (keyCode == 204 || keyCode == 62)) {
            this.mLanguageSwitchKeyPressed = false;
            return -1L;
        }
        if ((isValidGlobalKey(keyCode) && this.mGlobalKeyManager.handleGlobalKey(this.mContext, keyCode, event)) || (65536 & metaState) != 0) {
            return -1L;
        }
        return 0L;
    }

    public KeyEvent dispatchUnhandledKey(WindowManagerPolicy.WindowState win, KeyEvent event, int policyFlags) {
        KeyCharacterMap.FallbackAction fallbackAction;
        KeyEvent fallbackEvent = null;
        if ((event.getFlags() & 1024) == 0) {
            KeyCharacterMap kcm = event.getKeyCharacterMap();
            int keyCode = event.getKeyCode();
            int metaState = event.getMetaState();
            boolean initialDown = event.getAction() == 0 && event.getRepeatCount() == 0;
            if (initialDown) {
                fallbackAction = kcm.getFallbackAction(keyCode, metaState);
            } else {
                fallbackAction = this.mFallbackActions.get(keyCode);
            }
            if (fallbackAction != null) {
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

    private void launchAssistAction() {
        launchAssistAction(null);
    }

    private void launchAssistAction(String hint) {
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        Intent intent = ((SearchManager) this.mContext.getSystemService("search")).getAssistIntent(this.mContext, true, APPLICATION_MEDIA_SUBLAYER);
        if (intent != null) {
            if (hint != null) {
                intent.putExtra(hint, true);
            }
            intent.setFlags(872415232);
            try {
                startActivityAsUser(intent, UserHandle.CURRENT);
            } catch (ActivityNotFoundException e) {
                Slog.w(TAG, "No activity to handle assist action.", e);
            }
        }
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
        try {
            IStatusBarService statusbar = getStatusBarService();
            if (statusbar != null) {
                statusbar.preloadRecentApps();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException when preloading recent apps", e);
            this.mStatusBarService = null;
        }
    }

    private void cancelPreloadRecentApps() {
        if (this.mPreloadedRecentApps) {
            this.mPreloadedRecentApps = false;
            try {
                IStatusBarService statusbar = getStatusBarService();
                if (statusbar != null) {
                    statusbar.cancelPreloadRecentApps();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException when cancelling recent apps preload", e);
                this.mStatusBarService = null;
            }
        }
    }

    private void toggleRecentApps() {
        this.mPreloadedRecentApps = false;
        try {
            IStatusBarService statusbar = getStatusBarService();
            if (statusbar != null) {
                statusbar.toggleRecentApps();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException when toggling recent apps", e);
            this.mStatusBarService = null;
        }
    }

    public void showRecentApps() {
        this.mHandler.removeMessages(MSG_DISPATCH_SHOW_RECENTS);
        this.mHandler.sendEmptyMessage(MSG_DISPATCH_SHOW_RECENTS);
    }

    private void showRecentApps(boolean triggeredFromAltTab) {
        this.mPreloadedRecentApps = false;
        try {
            IStatusBarService statusbar = getStatusBarService();
            if (statusbar != null) {
                statusbar.showRecentApps(triggeredFromAltTab);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException when showing recent apps", e);
            this.mStatusBarService = null;
        }
    }

    private void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHome) {
        this.mPreloadedRecentApps = false;
        try {
            IStatusBarService statusbar = getStatusBarService();
            if (statusbar != null) {
                statusbar.hideRecentApps(triggeredFromAltTab, triggeredFromHome);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException when closing recent apps", e);
            this.mStatusBarService = null;
        }
    }

    void launchHomeFromHotKey() {
        if (!isKeyguardShowingAndNotOccluded()) {
            if (!this.mHideLockScreen && this.mKeyguardDelegate.isInputRestricted()) {
                this.mKeyguardDelegate.verifyUnlock(new WindowManagerPolicy.OnKeyguardExitResult() {
                    public void onKeyguardExitResult(boolean success) {
                        if (success) {
                            try {
                                ActivityManagerNative.getDefault().stopAppSwitches();
                            } catch (RemoteException e) {
                            }
                            PhoneWindowManager.this.sendCloseSystemWindows(PhoneWindowManager.SYSTEM_DIALOG_REASON_HOME_KEY);
                            PhoneWindowManager.this.startDockOrHome();
                        }
                    }
                });
                return;
            }
            try {
                ActivityManagerNative.getDefault().stopAppSwitches();
            } catch (RemoteException e) {
            }
            if (this.mRecentsVisible) {
                awakenDreams();
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
                hideRecentApps(false, true);
            } else {
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
                startDockOrHome();
            }
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
                            int newVal = PhoneWindowManager.this.mResettingSystemUiFlags | 2 | 1 | PhoneWindowManager.MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK;
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
                        }
                        if (changed) {
                            PhoneWindowManager.this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
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
        this.mRecentsVisible = (visibility & 16384) > 0;
        this.mResettingSystemUiFlags &= visibility;
        return (this.mResettingSystemUiFlags ^ APPLICATION_MEDIA_OVERLAY_SUBLAYER) & visibility & (this.mForceClearedSystemUiFlags ^ APPLICATION_MEDIA_OVERLAY_SUBLAYER);
    }

    public void getInsetHintLw(WindowManager.LayoutParams attrs, Rect outContentInsets, Rect outStableInsets) {
        int availRight;
        int availBottom;
        int fl = PolicyControl.getWindowFlags(null, attrs);
        int sysuiVis = PolicyControl.getSystemUiVisibility(null, attrs);
        int systemUiVisibility = sysuiVis | attrs.subtreeSystemUiVisibility;
        if ((fl & 65792) == 65792) {
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
            return;
        }
        outContentInsets.setEmpty();
        outStableInsets.setEmpty();
    }

    public void beginLayoutLw(boolean isDefaultDisplay, int displayWidth, int displayHeight, int displayRotation) {
        int overscanLeft;
        int overscanTop;
        int overscanRight;
        int overscanBottom;
        if (isDefaultDisplay) {
            switch (displayRotation) {
                case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                    overscanLeft = this.mOverscanTop;
                    overscanTop = this.mOverscanRight;
                    overscanRight = this.mOverscanBottom;
                    overscanBottom = this.mOverscanLeft;
                    break;
                case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                    overscanLeft = this.mOverscanRight;
                    overscanTop = this.mOverscanBottom;
                    overscanRight = this.mOverscanLeft;
                    overscanBottom = this.mOverscanTop;
                    break;
                case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
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
        SystemGesturesPointerEventListener systemGesturesPointerEventListener = this.mSystemGestures;
        int i = this.mUnrestrictedScreenWidth;
        systemGesturesPointerEventListener.screenWidth = i;
        this.mRestrictedScreenWidth = i;
        SystemGesturesPointerEventListener systemGesturesPointerEventListener2 = this.mSystemGestures;
        int i2 = this.mUnrestrictedScreenHeight;
        systemGesturesPointerEventListener2.screenHeight = i2;
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
        this.mStatusBarLayer = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
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
        if (isDefaultDisplay) {
            int sysui = this.mLastSystemUiFlags;
            boolean navVisible = (sysui & 2) == 0 && this.mHideNavigationBar == 0;
            if (this.mTopFullscreenOpaqueWindowState != null) {
                this.mTopFullscreenOpaqueWindowState.getAttrs();
            }
            boolean navTranslucent = ((-2147450880) & sysui) != 0;
            boolean immersive = ((sysui & 2048) == 0 && this.mHideNavigationBar == 0) ? false : true;
            boolean immersiveSticky = (sysui & 4096) != 0;
            boolean navAllowedHidden = immersive || immersiveSticky;
            boolean navTranslucent2 = navTranslucent & (!immersiveSticky);
            boolean isKeyguardShowing = isStatusBarKeyguard() && !this.mHideLockScreen;
            if (!isKeyguardShowing) {
                navTranslucent2 &= areTranslucentBarsAllowed();
            }
            if (navVisible || navAllowedHidden) {
                if (this.mHideNavFakeWindow != null) {
                    this.mHideNavFakeWindow.dismiss();
                    this.mHideNavFakeWindow = null;
                }
            } else if (this.mHideNavFakeWindow == null) {
                this.mHideNavFakeWindow = this.mWindowManagerFuncs.addFakeWindow(this.mHandler.getLooper(), this.mHideNavInputEventReceiverFactory, "hidden nav", 2022, 0, 0, false, false, true);
            }
            boolean navVisible2 = navVisible | (!canHideNavigationBar());
            boolean updateSysUiVisibility = false;
            if (this.mNavigationBar != null) {
                boolean transientNavBarShowing = this.mNavigationBarController.isTransientShowing();
                this.mNavigationBarOnBottom = !this.mNavigationBarCanMove || displayWidth < displayHeight;
                if (this.mNavigationBarOnBottom) {
                    int top = (displayHeight - overscanBottom) - this.mNavigationBarHeightForRotation[displayRotation];
                    mTmpNavigationFrame.set(0, top, displayWidth, displayHeight - overscanBottom);
                    int i11 = mTmpNavigationFrame.top;
                    this.mStableFullscreenBottom = i11;
                    this.mStableBottom = i11;
                    if (transientNavBarShowing) {
                        this.mNavigationBarController.setBarShowingLw(true);
                    } else if (navVisible2) {
                        this.mNavigationBarController.setBarShowingLw(true);
                        this.mDockBottom = mTmpNavigationFrame.top;
                        this.mRestrictedScreenHeight = this.mDockBottom - this.mRestrictedScreenTop;
                        this.mRestrictedOverscanScreenHeight = this.mDockBottom - this.mRestrictedOverscanScreenTop;
                    } else {
                        this.mNavigationBarController.setBarShowingLw(false);
                    }
                    if (navVisible2 && !navTranslucent2 && !navAllowedHidden && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent()) {
                        this.mSystemBottom = mTmpNavigationFrame.top;
                    }
                } else {
                    int left = (displayWidth - overscanRight) - this.mNavigationBarWidthForRotation[displayRotation];
                    mTmpNavigationFrame.set(left, 0, displayWidth - overscanRight, displayHeight);
                    int i12 = mTmpNavigationFrame.left;
                    this.mStableFullscreenRight = i12;
                    this.mStableRight = i12;
                    if (transientNavBarShowing) {
                        this.mNavigationBarController.setBarShowingLw(true);
                    } else if (navVisible2) {
                        this.mNavigationBarController.setBarShowingLw(true);
                        this.mDockRight = mTmpNavigationFrame.left;
                        this.mRestrictedScreenWidth = this.mDockRight - this.mRestrictedScreenLeft;
                        this.mRestrictedOverscanScreenWidth = this.mDockRight - this.mRestrictedOverscanScreenLeft;
                    } else {
                        this.mNavigationBarController.setBarShowingLw(false);
                    }
                    if (navVisible2 && !navTranslucent2 && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent()) {
                        this.mSystemRight = mTmpNavigationFrame.left;
                    }
                }
                int i13 = this.mDockTop;
                this.mCurTop = i13;
                this.mVoiceContentTop = i13;
                this.mContentTop = i13;
                int i14 = this.mDockBottom;
                this.mCurBottom = i14;
                this.mVoiceContentBottom = i14;
                this.mContentBottom = i14;
                int i15 = this.mDockLeft;
                this.mCurLeft = i15;
                this.mVoiceContentLeft = i15;
                this.mContentLeft = i15;
                int i16 = this.mDockRight;
                this.mCurRight = i16;
                this.mVoiceContentRight = i16;
                this.mContentRight = i16;
                this.mStatusBarLayer = this.mNavigationBar.getSurfaceLayer();
                this.mNavigationBar.computeFrameLw(mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, dcf, mTmpNavigationFrame);
                if (this.mNavigationBarController.checkHiddenLw()) {
                    updateSysUiVisibility = true;
                }
            }
            if (this.mStatusBar != null) {
                int i17 = this.mUnrestrictedScreenLeft;
                of.left = i17;
                df.left = i17;
                pf.left = i17;
                int i18 = this.mUnrestrictedScreenTop;
                of.top = i18;
                df.top = i18;
                pf.top = i18;
                int i19 = this.mUnrestrictedScreenWidth + this.mUnrestrictedScreenLeft;
                of.right = i19;
                df.right = i19;
                pf.right = i19;
                int i20 = this.mUnrestrictedScreenHeight + this.mUnrestrictedScreenTop;
                of.bottom = i20;
                df.bottom = i20;
                pf.bottom = i20;
                vf.left = this.mStableLeft;
                vf.top = this.mStableTop;
                vf.right = this.mStableRight;
                vf.bottom = this.mStableBottom;
                this.mStatusBarLayer = this.mStatusBar.getSurfaceLayer();
                this.mStatusBar.computeFrameLw(pf, df, vf, vf, vf, dcf, vf);
                this.mStableTop = this.mUnrestrictedScreenTop + this.mStatusBarHeight;
                boolean statusBarTransient = (67108864 & sysui) != 0;
                boolean statusBarTranslucent = (1073774592 & sysui) != 0;
                if (!isKeyguardShowing) {
                    statusBarTranslucent &= areTranslucentBarsAllowed();
                }
                if (this.mStatusBar.isVisibleLw() && !statusBarTransient) {
                    this.mDockTop = this.mUnrestrictedScreenTop + this.mStatusBarHeight;
                    int i21 = this.mDockTop;
                    this.mCurTop = i21;
                    this.mVoiceContentTop = i21;
                    this.mContentTop = i21;
                    int i22 = this.mDockBottom;
                    this.mCurBottom = i22;
                    this.mVoiceContentBottom = i22;
                    this.mContentBottom = i22;
                    int i23 = this.mDockLeft;
                    this.mCurLeft = i23;
                    this.mVoiceContentLeft = i23;
                    this.mContentLeft = i23;
                    int i24 = this.mDockRight;
                    this.mCurRight = i24;
                    this.mVoiceContentRight = i24;
                    this.mContentRight = i24;
                }
                if (this.mStatusBar.isVisibleLw() && !this.mStatusBar.isAnimatingLw() && !statusBarTransient && !statusBarTranslucent && !this.mStatusBarController.wasRecentlyTranslucent()) {
                    this.mSystemTop = this.mUnrestrictedScreenTop + this.mStatusBarHeight;
                }
                if (this.mStatusBarController.checkHiddenLw()) {
                    updateSysUiVisibility = true;
                }
            }
            if (updateSysUiVisibility) {
                updateSystemUiVisibilityLw();
            }
        }
    }

    public int getSystemDecorLayerLw() {
        if (this.mStatusBar != null) {
            return this.mStatusBar.getSurfaceLayer();
        }
        if (this.mNavigationBar != null) {
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
        if ((sysui & 256) != 0) {
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
    }

    public void layoutWindowLw(WindowManagerPolicy.WindowState win, WindowManagerPolicy.WindowState attached) {
        WindowManager.LayoutParams attrs = win.getAttrs();
        if ((win != this.mStatusBar || (attrs.privateFlags & 1024) != 0) && win != this.mNavigationBar) {
            boolean isDefaultDisplay = win.isDefaultDisplay();
            boolean needsToOffsetInputMethodTarget = isDefaultDisplay && win == this.mLastInputMethodTargetWindow && this.mLastInputMethodWindow != null;
            if (needsToOffsetInputMethodTarget) {
                offsetInputMethodWindowLw(this.mLastInputMethodWindow);
            }
            int fl = PolicyControl.getWindowFlags(win, attrs);
            int sim = attrs.softInputMode;
            int sysUiFl = PolicyControl.getSystemUiVisibility(win, null);
            Rect pf = mTmpParentFrame;
            Rect df = mTmpDisplayFrame;
            Rect of = mTmpOverscanFrame;
            Rect cf = mTmpContentFrame;
            Rect vf = mTmpVisibleFrame;
            Rect dcf = mTmpDecorFrame;
            Rect sf = mTmpStableFrame;
            dcf.setEmpty();
            boolean hasNavBar = isDefaultDisplay && this.mHasNavigationBar && this.mNavigationBar != null && this.mNavigationBar.isVisibleLw();
            int adjust = sim & 240;
            if (isDefaultDisplay) {
                sf.set(this.mStableLeft, this.mStableTop, this.mStableRight, this.mStableBottom);
            } else {
                sf.set(this.mOverscanLeft, this.mOverscanTop, this.mOverscanRight, this.mOverscanBottom);
            }
            if (!isDefaultDisplay) {
                if (attached != null) {
                    setAttachedWindowFrames(win, fl, adjust, attached, true, pf, df, of, cf, vf);
                } else {
                    int i = this.mOverscanScreenLeft;
                    cf.left = i;
                    of.left = i;
                    df.left = i;
                    pf.left = i;
                    int i2 = this.mOverscanScreenTop;
                    cf.top = i2;
                    of.top = i2;
                    df.top = i2;
                    pf.top = i2;
                    int i3 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                    cf.right = i3;
                    of.right = i3;
                    df.right = i3;
                    pf.right = i3;
                    int i4 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                    cf.bottom = i4;
                    of.bottom = i4;
                    df.bottom = i4;
                    pf.bottom = i4;
                }
            } else if (attrs.type == 2011) {
                int i5 = this.mDockLeft;
                vf.left = i5;
                cf.left = i5;
                of.left = i5;
                df.left = i5;
                pf.left = i5;
                int i6 = this.mDockTop;
                vf.top = i6;
                cf.top = i6;
                of.top = i6;
                df.top = i6;
                pf.top = i6;
                int i7 = this.mDockRight;
                vf.right = i7;
                cf.right = i7;
                of.right = i7;
                df.right = i7;
                pf.right = i7;
                int i8 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                of.bottom = i8;
                df.bottom = i8;
                pf.bottom = i8;
                int i9 = this.mStableBottom;
                vf.bottom = i9;
                cf.bottom = i9;
                attrs.gravity = 80;
                this.mDockLayer = win.getSurfaceLayer();
            } else if (win == this.mStatusBar && (attrs.privateFlags & 1024) != 0) {
                int i10 = this.mUnrestrictedScreenLeft;
                of.left = i10;
                df.left = i10;
                pf.left = i10;
                int i11 = this.mUnrestrictedScreenTop;
                of.top = i11;
                df.top = i11;
                pf.top = i11;
                int i12 = this.mUnrestrictedScreenWidth + this.mUnrestrictedScreenLeft;
                of.right = i12;
                df.right = i12;
                pf.right = i12;
                int i13 = this.mUnrestrictedScreenHeight + this.mUnrestrictedScreenTop;
                of.bottom = i13;
                df.bottom = i13;
                pf.bottom = i13;
                int i14 = this.mStableLeft;
                vf.left = i14;
                cf.left = i14;
                int i15 = this.mStableTop;
                vf.top = i15;
                cf.top = i15;
                int i16 = this.mStableRight;
                vf.right = i16;
                cf.right = i16;
                vf.bottom = this.mStableBottom;
                cf.bottom = this.mContentBottom;
            } else {
                dcf.left = this.mSystemLeft;
                dcf.top = this.mSystemTop;
                dcf.right = this.mSystemRight;
                dcf.bottom = this.mSystemBottom;
                boolean inheritTranslucentDecor = (attrs.privateFlags & 512) != 0;
                boolean isAppWindow = attrs.type >= 1 && attrs.type <= 99;
                boolean topAtRest = win == this.mTopFullscreenOpaqueWindowState && !win.isAnimatingLw();
                if (isAppWindow && !inheritTranslucentDecor && !topAtRest) {
                    if ((sysUiFl & MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK) == 0 && (fl & 1024) == 0 && (67108864 & fl) == 0 && (Integer.MIN_VALUE & fl) == 0) {
                        dcf.top = this.mStableTop;
                    }
                    if ((134217728 & fl) == 0 && (sysUiFl & 2) == 0 && this.mHideNavigationBar == 0 && (Integer.MIN_VALUE & fl) == 0) {
                        dcf.bottom = this.mStableBottom;
                        dcf.right = this.mStableRight;
                    }
                }
                if ((65792 & fl) == 65792) {
                    if (attached != null) {
                        setAttachedWindowFrames(win, fl, adjust, attached, true, pf, df, of, cf, vf);
                    } else {
                        if (attrs.type == 2014 || attrs.type == 2017) {
                            int i17 = hasNavBar ? this.mDockLeft : this.mUnrestrictedScreenLeft;
                            of.left = i17;
                            df.left = i17;
                            pf.left = i17;
                            int i18 = this.mUnrestrictedScreenTop;
                            of.top = i18;
                            df.top = i18;
                            pf.top = i18;
                            int i19 = hasNavBar ? this.mRestrictedScreenLeft + this.mRestrictedScreenWidth : this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                            of.right = i19;
                            df.right = i19;
                            pf.right = i19;
                            int i20 = hasNavBar ? this.mRestrictedScreenTop + this.mRestrictedScreenHeight : this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                            of.bottom = i20;
                            df.bottom = i20;
                            pf.bottom = i20;
                        } else if ((33554432 & fl) != 0 && attrs.type >= 1 && attrs.type <= 1999) {
                            int i21 = this.mOverscanScreenLeft;
                            of.left = i21;
                            df.left = i21;
                            pf.left = i21;
                            int i22 = this.mOverscanScreenTop;
                            of.top = i22;
                            df.top = i22;
                            pf.top = i22;
                            int i23 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                            of.right = i23;
                            df.right = i23;
                            pf.right = i23;
                            int i24 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                            of.bottom = i24;
                            df.bottom = i24;
                            pf.bottom = i24;
                        } else if (canHideNavigationBar() && (sysUiFl & 512) != 0 && attrs.type >= 1 && attrs.type <= 1999) {
                            int i25 = this.mOverscanScreenLeft;
                            df.left = i25;
                            pf.left = i25;
                            int i26 = this.mOverscanScreenTop;
                            df.top = i26;
                            pf.top = i26;
                            int i27 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                            df.right = i27;
                            pf.right = i27;
                            int i28 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                            df.bottom = i28;
                            pf.bottom = i28;
                            of.left = this.mUnrestrictedScreenLeft;
                            of.top = this.mUnrestrictedScreenTop;
                            of.right = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                            of.bottom = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                        } else {
                            int i29 = this.mRestrictedOverscanScreenLeft;
                            df.left = i29;
                            pf.left = i29;
                            int i30 = this.mRestrictedOverscanScreenTop;
                            df.top = i30;
                            pf.top = i30;
                            int i31 = this.mRestrictedOverscanScreenLeft + this.mRestrictedOverscanScreenWidth;
                            df.right = i31;
                            pf.right = i31;
                            int i32 = this.mRestrictedOverscanScreenTop + this.mRestrictedOverscanScreenHeight;
                            df.bottom = i32;
                            pf.bottom = i32;
                            of.left = this.mUnrestrictedScreenLeft;
                            of.top = this.mUnrestrictedScreenTop;
                            of.right = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                            of.bottom = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                        }
                        if ((fl & 1024) == 0) {
                            if (win.isVoiceInteraction()) {
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
                        } else {
                            cf.left = this.mRestrictedScreenLeft;
                            cf.top = this.mRestrictedScreenTop;
                            cf.right = this.mRestrictedScreenLeft + this.mRestrictedScreenWidth;
                            cf.bottom = this.mRestrictedScreenTop + this.mRestrictedScreenHeight;
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
                    if (attrs.type == 2014 || attrs.type == 2017) {
                        int i33 = hasNavBar ? this.mDockLeft : this.mUnrestrictedScreenLeft;
                        cf.left = i33;
                        of.left = i33;
                        df.left = i33;
                        pf.left = i33;
                        int i34 = this.mUnrestrictedScreenTop;
                        cf.top = i34;
                        of.top = i34;
                        df.top = i34;
                        pf.top = i34;
                        int i35 = hasNavBar ? this.mRestrictedScreenLeft + this.mRestrictedScreenWidth : this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                        cf.right = i35;
                        of.right = i35;
                        df.right = i35;
                        pf.right = i35;
                        int i36 = hasNavBar ? this.mRestrictedScreenTop + this.mRestrictedScreenHeight : this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                        cf.bottom = i36;
                        of.bottom = i36;
                        df.bottom = i36;
                        pf.bottom = i36;
                    } else if (attrs.type == 2019 || attrs.type == 2024) {
                        int i37 = this.mUnrestrictedScreenLeft;
                        of.left = i37;
                        df.left = i37;
                        pf.left = i37;
                        int i38 = this.mUnrestrictedScreenTop;
                        of.top = i38;
                        df.top = i38;
                        pf.top = i38;
                        int i39 = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                        of.right = i39;
                        df.right = i39;
                        pf.right = i39;
                        int i40 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                        of.bottom = i40;
                        df.bottom = i40;
                        pf.bottom = i40;
                    } else if ((attrs.type == 2015 || attrs.type == 2021) && (fl & 1024) != 0) {
                        int i41 = this.mOverscanScreenLeft;
                        cf.left = i41;
                        of.left = i41;
                        df.left = i41;
                        pf.left = i41;
                        int i42 = this.mOverscanScreenTop;
                        cf.top = i42;
                        of.top = i42;
                        df.top = i42;
                        pf.top = i42;
                        int i43 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                        cf.right = i43;
                        of.right = i43;
                        df.right = i43;
                        pf.right = i43;
                        int i44 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                        cf.bottom = i44;
                        of.bottom = i44;
                        df.bottom = i44;
                        pf.bottom = i44;
                    } else if (attrs.type == 2021 || attrs.type == 2025) {
                        int i45 = this.mOverscanScreenLeft;
                        cf.left = i45;
                        of.left = i45;
                        df.left = i45;
                        pf.left = i45;
                        int i46 = this.mOverscanScreenTop;
                        cf.top = i46;
                        of.top = i46;
                        df.top = i46;
                        pf.top = i46;
                        int i47 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                        cf.right = i47;
                        of.right = i47;
                        df.right = i47;
                        pf.right = i47;
                        int i48 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                        cf.bottom = i48;
                        of.bottom = i48;
                        df.bottom = i48;
                        pf.bottom = i48;
                    } else if (attrs.type == 2013) {
                        int i49 = this.mOverscanScreenLeft;
                        df.left = i49;
                        pf.left = i49;
                        int i50 = this.mOverscanScreenTop;
                        df.top = i50;
                        pf.top = i50;
                        int i51 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                        df.right = i51;
                        pf.right = i51;
                        int i52 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                        df.bottom = i52;
                        pf.bottom = i52;
                        int i53 = this.mUnrestrictedScreenLeft;
                        cf.left = i53;
                        of.left = i53;
                        int i54 = this.mUnrestrictedScreenTop;
                        cf.top = i54;
                        of.top = i54;
                        int i55 = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                        cf.right = i55;
                        of.right = i55;
                        int i56 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                        cf.bottom = i56;
                        of.bottom = i56;
                    } else if ((33554432 & fl) != 0 && attrs.type >= 1 && attrs.type <= 1999) {
                        int i57 = this.mOverscanScreenLeft;
                        cf.left = i57;
                        of.left = i57;
                        df.left = i57;
                        pf.left = i57;
                        int i58 = this.mOverscanScreenTop;
                        cf.top = i58;
                        of.top = i58;
                        df.top = i58;
                        pf.top = i58;
                        int i59 = this.mOverscanScreenLeft + this.mOverscanScreenWidth;
                        cf.right = i59;
                        of.right = i59;
                        df.right = i59;
                        pf.right = i59;
                        int i60 = this.mOverscanScreenTop + this.mOverscanScreenHeight;
                        cf.bottom = i60;
                        of.bottom = i60;
                        df.bottom = i60;
                        pf.bottom = i60;
                    } else if (canHideNavigationBar() && (sysUiFl & 512) != 0 && (attrs.type == 2000 || attrs.type == 2005 || (attrs.type >= 1 && attrs.type <= 1999))) {
                        int i61 = this.mUnrestrictedScreenLeft;
                        cf.left = i61;
                        of.left = i61;
                        df.left = i61;
                        pf.left = i61;
                        int i62 = this.mUnrestrictedScreenTop;
                        cf.top = i62;
                        of.top = i62;
                        df.top = i62;
                        pf.top = i62;
                        int i63 = this.mUnrestrictedScreenLeft + this.mUnrestrictedScreenWidth;
                        cf.right = i63;
                        of.right = i63;
                        df.right = i63;
                        pf.right = i63;
                        int i64 = this.mUnrestrictedScreenTop + this.mUnrestrictedScreenHeight;
                        cf.bottom = i64;
                        of.bottom = i64;
                        df.bottom = i64;
                        pf.bottom = i64;
                    } else {
                        int i65 = this.mRestrictedScreenLeft;
                        cf.left = i65;
                        of.left = i65;
                        df.left = i65;
                        pf.left = i65;
                        int i66 = this.mRestrictedScreenTop;
                        cf.top = i66;
                        of.top = i66;
                        df.top = i66;
                        pf.top = i66;
                        int i67 = this.mRestrictedScreenLeft + this.mRestrictedScreenWidth;
                        cf.right = i67;
                        of.right = i67;
                        df.right = i67;
                        pf.right = i67;
                        int i68 = this.mRestrictedScreenTop + this.mRestrictedScreenHeight;
                        cf.bottom = i68;
                        of.bottom = i68;
                        df.bottom = i68;
                        pf.bottom = i68;
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
                    setAttachedWindowFrames(win, fl, adjust, attached, false, pf, df, of, cf, vf);
                } else if (attrs.type == 2014) {
                    int i69 = this.mRestrictedScreenLeft;
                    cf.left = i69;
                    of.left = i69;
                    df.left = i69;
                    pf.left = i69;
                    int i70 = this.mRestrictedScreenTop;
                    cf.top = i70;
                    of.top = i70;
                    df.top = i70;
                    pf.top = i70;
                    int i71 = this.mRestrictedScreenLeft + this.mRestrictedScreenWidth;
                    cf.right = i71;
                    of.right = i71;
                    df.right = i71;
                    pf.right = i71;
                    int i72 = this.mRestrictedScreenTop + this.mRestrictedScreenHeight;
                    cf.bottom = i72;
                    of.bottom = i72;
                    df.bottom = i72;
                    pf.bottom = i72;
                } else if (attrs.type == 2005 || attrs.type == 2003 || attrs.type == 2020) {
                    int i73 = this.mStableLeft;
                    cf.left = i73;
                    of.left = i73;
                    df.left = i73;
                    pf.left = i73;
                    int i74 = this.mStableTop;
                    cf.top = i74;
                    of.top = i74;
                    df.top = i74;
                    pf.top = i74;
                    int i75 = this.mStableRight;
                    cf.right = i75;
                    of.right = i75;
                    df.right = i75;
                    pf.right = i75;
                    int i76 = this.mStableBottom;
                    cf.bottom = i76;
                    of.bottom = i76;
                    df.bottom = i76;
                    pf.bottom = i76;
                } else {
                    pf.left = this.mContentLeft;
                    pf.top = this.mContentTop;
                    pf.right = this.mContentRight;
                    pf.bottom = this.mContentBottom;
                    if (win.isVoiceInteraction()) {
                        int i77 = this.mVoiceContentLeft;
                        cf.left = i77;
                        of.left = i77;
                        df.left = i77;
                        int i78 = this.mVoiceContentTop;
                        cf.top = i78;
                        of.top = i78;
                        df.top = i78;
                        int i79 = this.mVoiceContentRight;
                        cf.right = i79;
                        of.right = i79;
                        df.right = i79;
                        int i80 = this.mVoiceContentBottom;
                        cf.bottom = i80;
                        of.bottom = i80;
                        df.bottom = i80;
                    } else if (adjust != 16) {
                        int i81 = this.mDockLeft;
                        cf.left = i81;
                        of.left = i81;
                        df.left = i81;
                        int i82 = this.mDockTop;
                        cf.top = i82;
                        of.top = i82;
                        df.top = i82;
                        int i83 = this.mDockRight;
                        cf.right = i83;
                        of.right = i83;
                        df.right = i83;
                        int i84 = this.mDockBottom;
                        cf.bottom = i84;
                        of.bottom = i84;
                        df.bottom = i84;
                    } else {
                        int i85 = this.mContentLeft;
                        cf.left = i85;
                        of.left = i85;
                        df.left = i85;
                        int i86 = this.mContentTop;
                        cf.top = i86;
                        of.top = i86;
                        df.top = i86;
                        int i87 = this.mContentRight;
                        cf.right = i87;
                        of.right = i87;
                        df.right = i87;
                        int i88 = this.mContentBottom;
                        cf.bottom = i88;
                        of.bottom = i88;
                        df.bottom = i88;
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
            if ((fl & 512) != 0 && attrs.type != 2010) {
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
            win.computeFrameLw(pf, df, of, cf, vf, dcf, sf);
            if (attrs.type == 2011 && win.isVisibleOrBehindKeyguardLw() && !win.getGivenInsetsPendingLw()) {
                setLastInputMethodWindowLw(null, null);
                offsetInputMethodWindowLw(win);
            }
            if (attrs.type == 2031 && win.isVisibleOrBehindKeyguardLw() && !win.getGivenInsetsPendingLw()) {
                offsetVoiceInputWindowLw(win);
            }
        }
    }

    private void offsetInputMethodWindowLw(WindowManagerPolicy.WindowState win) {
        int top = win.getContentFrameLw().top + win.getGivenContentInsetsLw().top;
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
    }

    private void offsetVoiceInputWindowLw(WindowManagerPolicy.WindowState win) {
        int gravity = win.getAttrs().gravity;
        switch (gravity & MSG_KEYGUARD_DRAWN_TIMEOUT) {
            case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                int right = win.getContentFrameLw().right - win.getGivenContentInsetsLw().right;
                if (this.mVoiceContentLeft < right) {
                    this.mVoiceContentLeft = right;
                }
                break;
            case MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK:
                int left = win.getContentFrameLw().left - win.getGivenContentInsetsLw().left;
                if (this.mVoiceContentRight < left) {
                    this.mVoiceContentRight = left;
                }
                break;
        }
        switch (gravity & 96) {
            case 32:
                int bottom = win.getContentFrameLw().bottom - win.getGivenContentInsetsLw().bottom;
                if (this.mVoiceContentTop < bottom) {
                    this.mVoiceContentTop = bottom;
                }
                break;
            case 64:
                int top = win.getContentFrameLw().top - win.getGivenContentInsetsLw().top;
                if (this.mVoiceContentBottom < top) {
                    this.mVoiceContentBottom = top;
                }
                break;
        }
    }

    public void finishLayoutLw() {
    }

    public void beginPostLayoutPolicyLw(int displayWidth, int displayHeight) {
        boolean z = false;
        this.mTopFullscreenOpaqueWindowState = null;
        this.mAppsToBeHidden.clear();
        this.mAppsThatDismissKeyguard.clear();
        this.mForceStatusBar = false;
        this.mForceStatusBarFromKeyguard = false;
        this.mForcingShowNavBar = false;
        this.mForcingShowNavBarLayer = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
        this.mHideLockScreen = false;
        this.mAllowLockscreenWhenOn = false;
        this.mDismissKeyguard = 0;
        this.mShowingLockscreen = false;
        this.mShowingDream = false;
        this.mWinShowWhenLocked = null;
        this.mKeyguardSecure = isKeyguardSecure();
        if (this.mKeyguardSecure && this.mKeyguardDelegate != null && this.mKeyguardDelegate.isShowing()) {
            z = true;
        }
        this.mKeyguardSecureIncludingHidden = z;
    }

    public void applyPostLayoutPolicyLw(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, WindowManagerPolicy.WindowState attached) {
        int fl = PolicyControl.getWindowFlags(win, attrs);
        if (this.mTopFullscreenOpaqueWindowState == null && win.isVisibleLw() && attrs.type == 2011) {
            this.mForcingShowNavBar = true;
            this.mForcingShowNavBarLayer = win.getSurfaceLayer();
        }
        if (attrs.type == 2000 && (attrs.privateFlags & 1024) != 0) {
            this.mForceStatusBarFromKeyguard = true;
        }
        if (this.mTopFullscreenOpaqueWindowState == null && win.isVisibleOrBehindKeyguardLw() && !win.isGoneForLayoutLw()) {
            if ((fl & 2048) != 0) {
                if ((attrs.privateFlags & 1024) != 0) {
                    this.mForceStatusBarFromKeyguard = true;
                } else {
                    this.mForceStatusBar = true;
                }
            }
            if ((attrs.privateFlags & 1024) != 0) {
                this.mShowingLockscreen = true;
            }
            boolean appWindow = attrs.type >= 1 && attrs.type < 2000;
            if (attrs.type == 2023 && (!this.mDreamingLockscreen || (win.isVisibleLw() && win.hasDrawnLw()))) {
                this.mShowingDream = true;
                appWindow = true;
            }
            boolean showWhenLocked = (524288 & fl) != 0;
            boolean dismissKeyguard = (4194304 & fl) != 0;
            IApplicationToken appToken = win.getAppToken();
            if (appWindow && attached == null) {
                if (showWhenLocked) {
                    this.mAppsToBeHidden.remove(appToken);
                    this.mAppsThatDismissKeyguard.remove(appToken);
                    if (this.mAppsToBeHidden.isEmpty()) {
                        if (dismissKeyguard && !this.mKeyguardSecure) {
                            this.mAppsThatDismissKeyguard.add(appToken);
                        } else {
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
                if (attrs.x == 0 && attrs.y == 0 && attrs.width == APPLICATION_MEDIA_OVERLAY_SUBLAYER && attrs.height == APPLICATION_MEDIA_OVERLAY_SUBLAYER) {
                    this.mTopFullscreenOpaqueWindowState = win;
                    if (!this.mAppsThatDismissKeyguard.isEmpty() && this.mDismissKeyguard == 0) {
                        this.mDismissKeyguard = this.mWinDismissingKeyguard == win ? 2 : 1;
                        this.mWinDismissingKeyguard = win;
                        this.mForceStatusBarFromKeyguard = this.mShowingLockscreen && this.mKeyguardSecure;
                    } else if (this.mAppsToBeHidden.isEmpty() && showWhenLocked) {
                        this.mHideLockScreen = true;
                        this.mForceStatusBarFromKeyguard = false;
                    }
                    if ((fl & 1) != 0) {
                        this.mAllowLockscreenWhenOn = true;
                    }
                }
                if (this.mWinShowWhenLocked != null && this.mWinShowWhenLocked.getAppToken() != win.getAppToken()) {
                    win.hideLw(false);
                }
            }
        }
    }

    public int finishPostLayoutPolicyLw() {
        if (this.mWinShowWhenLocked != null && this.mWinShowWhenLocked != this.mTopFullscreenOpaqueWindowState) {
            this.mWinShowWhenLocked.getAttrs().flags |= 1048576;
            this.mTopFullscreenOpaqueWindowState.hideLw(false);
            this.mTopFullscreenOpaqueWindowState = this.mWinShowWhenLocked;
        }
        int changes = 0;
        boolean topIsFullscreen = false;
        WindowManager.LayoutParams lp = this.mTopFullscreenOpaqueWindowState != null ? this.mTopFullscreenOpaqueWindowState.getAttrs() : null;
        if (!this.mShowingDream) {
            this.mDreamingLockscreen = this.mShowingLockscreen;
        }
        if (this.mStatusBar != null) {
            if (this.mForceStatusBar || this.mForceStatusBarFromKeyguard) {
                if (this.mStatusBarController.setBarShowingLw(true)) {
                    changes = 0 | 1;
                }
                topIsFullscreen = this.mTopIsFullscreen && this.mStatusBar.isAnimatingLw();
                if (this.mForceStatusBarFromKeyguard && this.mStatusBarController.isTransientShowing()) {
                    this.mStatusBarController.updateVisibilityLw(false, this.mLastSystemUiFlags, this.mLastSystemUiFlags);
                }
            } else if (this.mTopFullscreenOpaqueWindowState != null) {
                int fl = PolicyControl.getWindowFlags(null, lp);
                topIsFullscreen = ((fl & 1024) == 0 && (this.mLastSystemUiFlags & MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK) == 0) ? false : true;
                if (this.mStatusBarController.isTransientShowing()) {
                    if (this.mStatusBarController.setBarShowingLw(true)) {
                        changes = 0 | 1;
                    }
                } else if (topIsFullscreen) {
                    if (this.mStatusBarController.setBarShowingLw(false)) {
                        changes = 0 | 1;
                    }
                } else if (this.mStatusBarController.setBarShowingLw(true)) {
                    changes = 0 | 1;
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
            if (this.mDismissKeyguard != 0 && !this.mKeyguardSecure) {
                this.mKeyguardHidden = true;
                if (setKeyguardOccludedLw(true)) {
                    changes |= MSG_WINDOW_MANAGER_DRAWN_COMPLETE;
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
                if (setKeyguardOccludedLw(true)) {
                    changes |= MSG_WINDOW_MANAGER_DRAWN_COMPLETE;
                }
            } else if (this.mDismissKeyguard != 0) {
                if (this.mDismissKeyguard == 1) {
                    this.mKeyguardHidden = false;
                    if (setKeyguardOccludedLw(false)) {
                        changes |= MSG_WINDOW_MANAGER_DRAWN_COMPLETE;
                    }
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            PhoneWindowManager.this.mKeyguardDelegate.dismiss();
                        }
                    });
                }
            } else {
                this.mWinDismissingKeyguard = null;
                this.mKeyguardHidden = false;
                if (setKeyguardOccludedLw(false)) {
                    changes |= MSG_WINDOW_MANAGER_DRAWN_COMPLETE;
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
            this.mStatusBar.getAttrs().flags |= 1048576;
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
        return (isStatusBarKeyguard() || this.mShowingDream) ? false : true;
    }

    public int focusChangedLw(WindowManagerPolicy.WindowState lastFocus, WindowManagerPolicy.WindowState newFocus) {
        this.mFocusedWindow = newFocus;
        return (updateSystemUiVisibilityLw() & SYSTEM_UI_CHANGING_LAYOUT) != 0 ? 1 : 0;
    }

    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        int newLidState = lidOpen ? 1 : 0;
        if (newLidState != this.mLidState) {
            this.mLidState = newLidState;
            applyLidSwitchState();
            updateRotation(true);
            if (lidOpen) {
                wakeUp(SystemClock.uptimeMillis(), this.mAllowTheaterModeWakeFromLidSwitch);
            } else if (!this.mLidControlsSleep) {
                this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
            }
        }
    }

    public void notifyCameraLensCoverSwitchChanged(long whenNanos, boolean lensCovered) {
        Intent intent;
        int lensCoverState = lensCovered ? 1 : 0;
        if (this.mCameraLensCoverState != lensCoverState) {
            if (this.mCameraLensCoverState == 1 && lensCoverState == 0) {
                boolean keyguardActive = this.mKeyguardDelegate == null ? false : this.mKeyguardDelegate.isShowing();
                if (keyguardActive) {
                    intent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE");
                } else {
                    intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
                }
                wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromCameraLens);
                startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
            }
            this.mCameraLensCoverState = lensCoverState;
        }
    }

    void setHdmiPlugged(boolean plugged) {
        if (this.mHdmiPlugged != plugged) {
            this.mHdmiPlugged = plugged;
            updateRotation(true, true);
            Intent intent = new Intent("android.intent.action.HDMI_PLUGGED");
            intent.addFlags(67108864);
            intent.putExtra("state", plugged);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
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
                plugged = n > 1 ? Integer.parseInt(new String(buf, 0, n + APPLICATION_MEDIA_OVERLAY_SUBLAYER)) != 0 : false;
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

    private void takeScreenshot() {
        synchronized (this.mScreenshotLock) {
            if (this.mScreenshotConnection == null) {
                ComponentName cn = new ComponentName(KeyguardServiceDelegate.KEYGUARD_PACKAGE, "com.android.systemui.screenshot.TakeScreenshotService");
                Intent intent = new Intent();
                intent.setComponent(cn);
                ServiceConnection conn = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        synchronized (PhoneWindowManager.this.mScreenshotLock) {
                            if (PhoneWindowManager.this.mScreenshotConnection == this) {
                                Messenger messenger = new Messenger(service);
                                Message msg = Message.obtain((Handler) null, 1);
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
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                    }
                };
                if (this.mContext.bindServiceAsUser(intent, conn, 1, UserHandle.CURRENT)) {
                    this.mScreenshotConnection = conn;
                    this.mHandler.postDelayed(this.mScreenshotTimeout, 10000L);
                }
            }
        }
    }

    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        boolean keyguardActive;
        int result;
        TelecomManager telecomManager;
        if (!this.mSystemBooted) {
            return 0;
        }
        boolean interactive = (536870912 & policyFlags) != 0;
        boolean down = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        int keyCode = event.getKeyCode();
        boolean isInjected = (16777216 & policyFlags) != 0;
        if (this.mKeyguardDelegate == null) {
            keyguardActive = false;
        } else {
            keyguardActive = interactive ? isKeyguardShowingAndNotOccluded() : this.mKeyguardDelegate.isShowing();
        }
        boolean isWakeKey = (policyFlags & 1) != 0 || event.isWakeKey();
        if (interactive || (isInjected && !isWakeKey)) {
            result = 1;
            isWakeKey = false;
        } else if (!interactive && shouldDispatchInputWhenNonInteractive()) {
            result = 1;
        } else {
            result = 0;
            if (isWakeKey && (!down || !isWakeKeyWhenScreenOff(keyCode))) {
                isWakeKey = false;
            }
        }
        if (isValidGlobalKey(keyCode) && this.mGlobalKeyManager.shouldHandleGlobalKey(keyCode, event)) {
            if (isWakeKey) {
                wakeUp(event.getEventTime(), this.mAllowTheaterModeWakeFromKey);
                return result;
            }
            return result;
        }
        boolean useHapticFeedback = down && (policyFlags & 2) != 0 && event.getRepeatCount() == 0;
        switch (keyCode) {
            case MSG_KEYGUARD_DRAWN_COMPLETE:
                if (down && (telecomManager = getTelecommService()) != null && telecomManager.isRinging()) {
                    Log.i(TAG, "interceptKeyBeforeQueueing: CALL key-down while ringing: Answer the call!");
                    telecomManager.acceptRingingCall();
                    result &= APPLICATION_MEDIA_SUBLAYER;
                }
                break;
            case MSG_KEYGUARD_DRAWN_TIMEOUT:
                result &= APPLICATION_MEDIA_SUBLAYER;
                if (down) {
                    TelecomManager telecomManager2 = getTelecommService();
                    boolean hungUp = false;
                    if (telecomManager2 != null) {
                        hungUp = telecomManager2.endCall();
                    }
                    if (interactive && !hungUp) {
                        this.mEndCallKeyHandled = false;
                        this.mHandler.postDelayed(this.mEndCallLongPress, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                    } else {
                        this.mEndCallKeyHandled = true;
                    }
                } else if (!this.mEndCallKeyHandled) {
                    this.mHandler.removeCallbacks(this.mEndCallLongPress);
                    if (!canceled && (((this.mEndcallBehavior & 1) == 0 || !goHome()) && (this.mEndcallBehavior & 2) != 0)) {
                        this.mPowerManager.goToSleep(event.getEventTime(), MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK, 0);
                        isWakeKey = false;
                    }
                }
                break;
            case 24:
            case 25:
            case 164:
                if (keyCode == 25) {
                    if (down) {
                        if (interactive && !this.mScreenshotChordVolumeDownKeyTriggered && (event.getFlags() & 1024) == 0) {
                            this.mScreenshotChordVolumeDownKeyTriggered = true;
                            this.mScreenshotChordVolumeDownKeyTime = event.getDownTime();
                            this.mScreenshotChordVolumeDownKeyConsumed = false;
                            cancelPendingPowerKeyAction();
                            interceptScreenshotChord();
                        }
                    } else {
                        this.mScreenshotChordVolumeDownKeyTriggered = false;
                        cancelVolumeUpDownRunnable();
                        cancelPendingScreenshotChordAction();
                    }
                } else if (keyCode == 24) {
                    if (down) {
                        if (interactive && !this.mScreenshotChordVolumeUpKeyTriggered && (event.getFlags() & 1024) == 0) {
                            this.mScreenshotChordVolumeUpKeyTriggered = true;
                            cancelPendingPowerKeyAction();
                            cancelPendingScreenshotChordAction();
                        }
                    } else {
                        this.mScreenshotChordVolumeUpKeyTriggered = false;
                        cancelVolumeUpDownRunnable();
                        cancelPendingScreenshotChordAction();
                    }
                }
                if (down) {
                    TelecomManager telecomManager3 = getTelecommService();
                    if (telecomManager3 != null) {
                        if (telecomManager3.isRinging()) {
                            Log.i(TAG, "interceptKeyBeforeQueueing: VOLUME key-down while ringing: Silence ringer!");
                            telecomManager3.silenceRinger();
                            result &= APPLICATION_MEDIA_SUBLAYER;
                            break;
                        } else if (telecomManager3.isInCall() && (result & 1) == 0) {
                            this.mLongpressEvent = event;
                            this.mHandler.post(this.mVolumeUpDownRunnable);
                            break;
                        }
                    } else {
                        if ((result & 1) == 0) {
                            this.mLongpressEvent = event;
                            this.mHandler.post(this.mVolumeUpDownRunnable);
                        }
                        break;
                    }
                }
                break;
            case 26:
                result &= APPLICATION_MEDIA_SUBLAYER;
                isWakeKey = false;
                if (down) {
                    interceptPowerKeyDown(event, interactive);
                } else {
                    interceptPowerKeyUp(event, interactive, canceled);
                }
                break;
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
            case 222:
                if (MediaSessionLegacyHelper.getHelper(this.mContext).isGlobalPriorityActive()) {
                    result &= APPLICATION_MEDIA_SUBLAYER;
                }
                if ((result & 1) == 0) {
                    this.mBroadcastWakeLock.acquire();
                    Message msg = this.mHandler.obtainMessage(3, new KeyEvent(event));
                    msg.setAsynchronous(true);
                    msg.sendToTarget();
                }
                break;
            case 223:
                result &= APPLICATION_MEDIA_SUBLAYER;
                if (!this.mPowerManager.isInteractive()) {
                    useHapticFeedback = false;
                }
                this.mPowerManager.goToSleep(event.getEventTime(), MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK, 0);
                isWakeKey = false;
                break;
            case 224:
                result &= APPLICATION_MEDIA_SUBLAYER;
                isWakeKey = true;
                break;
            case 231:
                if ((result & 1) == 0 && !down) {
                    this.mBroadcastWakeLock.acquire();
                    Message msg2 = this.mHandler.obtainMessage(MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK, keyguardActive ? 1 : 0, 0);
                    msg2.setAsynchronous(true);
                    msg2.sendToTarget();
                }
                break;
        }
        if (useHapticFeedback) {
            performHapticFeedbackLw(null, 1, false);
        }
        if (isWakeKey) {
            wakeUp(event.getEventTime(), this.mAllowTheaterModeWakeFromKey);
            return result;
        }
        return result;
    }

    private static boolean isValidGlobalKey(int keyCode) {
        switch (keyCode) {
            case 26:
            case 223:
            case 224:
                return false;
            default:
                return true;
        }
    }

    private boolean isWakeKeyWhenScreenOff(int keyCode) {
        switch (keyCode) {
            case 24:
            case 25:
            case 164:
                if (this.mDockMode == 0) {
                }
                break;
            case 27:
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
            case 222:
                break;
        }
        return true;
    }

    public int interceptMotionBeforeQueueingNonInteractive(long whenNanos, int policyFlags) {
        if ((policyFlags & 1) != 0 && wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromMotion)) {
            return 0;
        }
        if (shouldDispatchInputWhenNonInteractive()) {
            return 1;
        }
        if (!isTheaterModeEnabled() || (policyFlags & 1) == 0) {
            return 0;
        }
        wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromMotionWhenNotDreaming);
        return 0;
    }

    private boolean shouldDispatchInputWhenNonInteractive() {
        if (isKeyguardShowingAndNotOccluded() && this.mDisplay != null && this.mDisplay.getState() != 1) {
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

    void dispatchMediaKeyWithWakeLock(KeyEvent event) {
        if (this.mHavePendingMediaKeyRepeatWithWakeLock) {
            this.mHandler.removeMessages(MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK);
            this.mHavePendingMediaKeyRepeatWithWakeLock = false;
            this.mBroadcastWakeLock.release();
        }
        dispatchMediaKeyWithWakeLockToAudioService(event);
        if (event.getAction() == 0 && event.getRepeatCount() == 0) {
            this.mHavePendingMediaKeyRepeatWithWakeLock = true;
            Message msg = this.mHandler.obtainMessage(MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK, event);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg, ViewConfiguration.getKeyRepeatTimeout());
            return;
        }
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyRepeatWithWakeLock(KeyEvent event) {
        this.mHavePendingMediaKeyRepeatWithWakeLock = false;
        KeyEvent repeatEvent = KeyEvent.changeTimeRepeat(event, SystemClock.uptimeMillis(), 1, event.getFlags() | 128);
        dispatchMediaKeyWithWakeLockToAudioService(repeatEvent);
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyWithWakeLockToAudioService(KeyEvent event) {
        if (ActivityManagerNative.isSystemReady()) {
            MediaSessionLegacyHelper.getHelper(this.mContext).sendMediaButtonEvent(event, true);
        }
    }

    void launchVoiceAssistWithWakeLock(boolean keyguardActive) {
        Intent voiceIntent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
        voiceIntent.putExtra("android.speech.extras.EXTRA_SECURE", keyguardActive);
        startActivityAsUser(voiceIntent, UserHandle.CURRENT_OR_SELF);
        this.mBroadcastWakeLock.release();
    }

    private void requestTransientBars(WindowManagerPolicy.WindowState swipeTarget) {
        synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            if (isUserSetupComplete()) {
                boolean sb = this.mStatusBarController.checkShowTransientBarLw();
                boolean nb = this.mNavigationBarController.checkShowTransientBarLw();
                if (sb || nb) {
                    WindowManagerPolicy.WindowState barTarget = sb ? this.mStatusBar : this.mNavigationBar;
                    if (!(sb ^ nb) || barTarget == swipeTarget) {
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
        }
    }

    public void goingToSleep(int why) {
        EventLog.writeEvent(70000, 0);
        synchronized (this.mLock) {
            this.mAwake = false;
            this.mKeyguardDrawComplete = false;
            updateWakeGestureListenerLp();
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onScreenTurnedOff(why);
        }
    }

    private void wakeUpFromPowerKey(long eventTime) {
        wakeUp(eventTime, this.mAllowTheaterModeWakeFromPowerKey);
    }

    private boolean wakeUp(long wakeTime, boolean wakeInTheaterMode) {
        if (!wakeInTheaterMode && isTheaterModeEnabled()) {
            return false;
        }
        this.mPowerManager.wakeUp(wakeTime);
        return true;
    }

    public void wakingUp() {
        EventLog.writeEvent(70000, 1);
        synchronized (this.mLock) {
            this.mAwake = true;
            this.mKeyguardDrawComplete = false;
            if (this.mKeyguardDelegate != null) {
                this.mHandler.removeMessages(MSG_KEYGUARD_DRAWN_TIMEOUT);
                this.mHandler.sendEmptyMessageDelayed(MSG_KEYGUARD_DRAWN_TIMEOUT, 1000L);
            }
            updateWakeGestureListenerLp();
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onScreenTurnedOn(this.mKeyguardDelegateCallback);
        } else {
            finishKeyguardDrawn();
        }
    }

    private void finishKeyguardDrawn() {
        synchronized (this.mLock) {
            if (this.mAwake && !this.mKeyguardDrawComplete) {
                this.mKeyguardDrawComplete = true;
                if (this.mKeyguardDelegate != null) {
                    this.mHandler.removeMessages(MSG_KEYGUARD_DRAWN_TIMEOUT);
                }
                finishScreenTurningOn();
            }
        }
    }

    public void screenTurnedOff() {
        synchronized (this.mLock) {
            this.mScreenOnEarly = false;
            this.mScreenOnFully = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = null;
            updateOrientationListenerLp();
        }
    }

    public void screenTurningOn(WindowManagerPolicy.ScreenOnListener screenOnListener) {
        synchronized (this.mLock) {
            this.mScreenOnEarly = true;
            this.mScreenOnFully = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = screenOnListener;
            updateOrientationListenerLp();
        }
        this.mWindowManagerInternal.waitForAllWindowsDrawn(this.mWindowManagerDrawCallback, 1000L);
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
        boolean enableScreen;
        synchronized (this.mLock) {
            if (!this.mScreenOnFully && this.mScreenOnEarly && this.mWindowManagerDrawComplete && (!this.mAwake || this.mKeyguardDrawComplete)) {
                WindowManagerPolicy.ScreenOnListener listener = this.mScreenOnListener;
                this.mScreenOnListener = null;
                this.mScreenOnFully = true;
                if (!this.mKeyguardDrawnOnce && this.mAwake) {
                    this.mKeyguardDrawnOnce = true;
                    enableScreen = true;
                    if (this.mBootMessageNeedsHiding) {
                        this.mBootMessageNeedsHiding = false;
                        hideBootMessages();
                    }
                } else {
                    enableScreen = false;
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
    }

    private void handleHideBootMessage() {
        synchronized (this.mLock) {
            if (!this.mKeyguardDrawnOnce) {
                this.mBootMessageNeedsHiding = true;
            } else if (this.mBootMsgDialog != null) {
                this.mBootMsgDialog.dismiss();
                this.mBootMsgDialog = null;
            }
        }
    }

    public boolean isScreenOn() {
        return this.mScreenOnFully;
    }

    public void enableKeyguard(boolean enabled) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.setKeyguardEnabled(enabled);
        }
    }

    public void exitKeyguardSecurely(WindowManagerPolicy.OnKeyguardExitResult callback) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.verifyUnlock(callback);
        }
    }

    private boolean isKeyguardShowingAndNotOccluded() {
        return (this.mKeyguardDelegate == null || !this.mKeyguardDelegate.isShowing() || this.mKeyguardOccluded) ? false : true;
    }

    public boolean isKeyguardLocked() {
        return keyguardOn();
    }

    public boolean isKeyguardSecure() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isSecure();
    }

    public boolean inKeyguardRestrictedKeyInputMode() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isInputRestricted();
    }

    public void dismissKeyguardLw() {
        if (this.mKeyguardDelegate != null && this.mKeyguardDelegate.isShowing()) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PhoneWindowManager.this.mKeyguardDelegate.dismiss();
                }
            });
        }
    }

    public void notifyActivityDrawnForKeyguardLw() {
        if (this.mKeyguardDelegate != null) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PhoneWindowManager.this.mKeyguardDelegate.onActivityDrawn();
                }
            });
        }
    }

    public boolean isKeyguardDrawnLw() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mKeyguardDrawnOnce;
        }
        return z;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }
    }

    void sendCloseSystemWindows() {
        sendCloseSystemWindows(this.mContext, null);
    }

    void sendCloseSystemWindows(String reason) {
        sendCloseSystemWindows(this.mContext, reason);
    }

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    public int rotationForOrientationLw(int orientation, int lastRotation) {
        int preferredRotation;
        if (this.mForceDefaultOrientation) {
            return this.mDefaultRotation;
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
            } else if ((this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK) && (this.mDeskDockEnablesAccelerometer || this.mDeskDockRotation >= 0)) {
                preferredRotation = this.mDeskDockEnablesAccelerometer ? sensorRotation : this.mDeskDockRotation;
            } else if (this.mHdmiPlugged && this.mDemoHdmiRotationLock) {
                preferredRotation = this.mDemoHdmiRotation;
            } else if (this.mHdmiPlugged && this.mDockMode == 0 && this.mUndockedHdmiRotation >= 0) {
                preferredRotation = this.mUndockedHdmiRotation;
            } else if (this.mDemoRotationLock) {
                preferredRotation = this.mDemoRotation;
            } else if (orientation == MSG_POWER_LONG_PRESS) {
                preferredRotation = lastRotation;
            } else if (!this.mSupportAutoRotation) {
                preferredRotation = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
            } else if ((this.mUserRotationMode == 0 && (orientation == 2 || orientation == APPLICATION_MEDIA_OVERLAY_SUBLAYER || orientation == MSG_HIDE_BOOT_MESSAGE || orientation == MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK || orientation == MSG_POWER_DELAYED_PRESS)) || orientation == MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK || orientation == 10 || orientation == MSG_KEYGUARD_DRAWN_TIMEOUT || orientation == MSG_WINDOW_MANAGER_DRAWN_COMPLETE) {
                if (this.mAllowAllRotations < 0) {
                    this.mAllowAllRotations = this.mContext.getResources().getBoolean(R.^attr-private.colorSurfaceHighlight) ? 1 : 0;
                }
                if (sensorRotation != 2 || this.mAllowAllRotations == 1 || orientation == 10 || orientation == MSG_POWER_DELAYED_PRESS) {
                    preferredRotation = sensorRotation;
                } else {
                    preferredRotation = lastRotation;
                }
            } else if (this.mUserRotationMode == 1 && orientation != MSG_KEYGUARD_DRAWN_COMPLETE) {
                preferredRotation = this.mUserRotation;
            } else {
                preferredRotation = APPLICATION_MEDIA_OVERLAY_SUBLAYER;
            }
            switch (orientation) {
                case 0:
                    if (!isLandscapeOrSeascape(preferredRotation)) {
                        int preferredRotation2 = this.mLandscapeRotation;
                        return preferredRotation2;
                    }
                    return preferredRotation;
                case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                    if (!isAnyPortrait(preferredRotation)) {
                        int preferredRotation3 = this.mPortraitRotation;
                        return preferredRotation3;
                    }
                    return preferredRotation;
                case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
                case MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK:
                case MSG_KEYGUARD_DRAWN_COMPLETE:
                case 10:
                default:
                    if (preferredRotation >= 0) {
                        return preferredRotation;
                    }
                    int preferredRotation4 = this.mDefaultRotation;
                    return preferredRotation4;
                case MSG_KEYGUARD_DRAWN_TIMEOUT:
                case MSG_HIDE_BOOT_MESSAGE:
                    if (!isLandscapeOrSeascape(preferredRotation)) {
                        if (isLandscapeOrSeascape(lastRotation)) {
                            return lastRotation;
                        }
                        int preferredRotation5 = this.mLandscapeRotation;
                        return preferredRotation5;
                    }
                    return preferredRotation;
                case MSG_WINDOW_MANAGER_DRAWN_COMPLETE:
                case MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK:
                    if (!isAnyPortrait(preferredRotation)) {
                        if (isAnyPortrait(lastRotation)) {
                            return lastRotation;
                        }
                        int preferredRotation6 = this.mPortraitRotation;
                        return preferredRotation6;
                    }
                    return preferredRotation;
                case 8:
                    if (!isLandscapeOrSeascape(preferredRotation)) {
                        int preferredRotation7 = this.mSeascapeRotation;
                        return preferredRotation7;
                    }
                    return preferredRotation;
                case MSG_DISPATCH_SHOW_RECENTS:
                    if (!isAnyPortrait(preferredRotation)) {
                        int preferredRotation8 = this.mUpsideDownRotation;
                        return preferredRotation8;
                    }
                    return preferredRotation;
            }
        }
    }

    public boolean rotationHasCompatibleMetricsLw(int orientation, int rotation) {
        switch (orientation) {
            case 0:
            case MSG_KEYGUARD_DRAWN_TIMEOUT:
            case 8:
                return isLandscapeOrSeascape(rotation);
            case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
            case MSG_WINDOW_MANAGER_DRAWN_COMPLETE:
            case MSG_DISPATCH_SHOW_RECENTS:
                return isAnyPortrait(rotation);
            case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
            case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
            case MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK:
            case MSG_KEYGUARD_DRAWN_COMPLETE:
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
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 0, APPLICATION_MEDIA_SUBLAYER) != 0 ? 0 : 1;
    }

    public void setUserRotationMode(int mode, int rot) {
        ContentResolver res = this.mContext.getContentResolver();
        if (mode == 1) {
            Settings.System.putIntForUser(res, "user_rotation", rot, APPLICATION_MEDIA_SUBLAYER);
            Settings.System.putIntForUser(res, "accelerometer_rotation", 0, APPLICATION_MEDIA_SUBLAYER);
        } else {
            Settings.System.putIntForUser(res, "accelerometer_rotation", 1, APPLICATION_MEDIA_SUBLAYER);
        }
    }

    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
        performHapticFeedbackLw(null, safeMode ? 10001 : 10000, true);
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
        }
    }

    public void systemBooted() {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.bindService(this.mContext);
            this.mKeyguardDelegate.onBootCompleted();
        }
        synchronized (this.mLock) {
            this.mSystemBooted = true;
        }
        wakingUp();
        screenTurningOn(null);
    }

    public void showBootMessage(final CharSequence msg, boolean always) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int theme;
                if (PhoneWindowManager.this.mBootMsgDialog == null) {
                    if (PhoneWindowManager.this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch")) {
                        theme = R.style.Widget.Holo.Light.FastScroll;
                    } else if (PhoneWindowManager.this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.television")) {
                        theme = R.style.Widget.DeviceDefault.TextView.ListSeparator;
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
                        PhoneWindowManager.this.mBootMsgDialog.setTitle(R.string.hearing_device_status_active);
                    } else {
                        PhoneWindowManager.this.mBootMsgDialog.setTitle(R.string.hearing_device_status_connected);
                    }
                    PhoneWindowManager.this.mBootMsgDialog.setProgressStyle(0);
                    PhoneWindowManager.this.mBootMsgDialog.setIndeterminate(true);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setType(2021);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().addFlags(258);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setDimAmount(1.0f);
                    WindowManager.LayoutParams lp = PhoneWindowManager.this.mBootMsgDialog.getWindow().getAttributes();
                    lp.screenOrientation = PhoneWindowManager.MSG_KEYGUARD_DRAWN_COMPLETE;
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setAttributes(lp);
                    PhoneWindowManager.this.mBootMsgDialog.setCancelable(false);
                    PhoneWindowManager.this.mBootMsgDialog.show();
                }
                PhoneWindowManager.this.mBootMsgDialog.setMessage(msg);
            }
        });
    }

    public void hideBootMessages() {
        this.mHandler.sendEmptyMessage(MSG_HIDE_BOOT_MESSAGE);
    }

    public void userActivity() {
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
            boolean enable = this.mAllowLockscreenWhenOn && this.mAwake && this.mKeyguardDelegate != null && this.mKeyguardDelegate.isSecure();
            if (this.mLockScreenTimerActive != enable) {
                if (enable) {
                    this.mHandler.postDelayed(this.mScreenLockTimeout, this.mLockScreenTimeout);
                } else {
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                }
                this.mLockScreenTimerActive = enable;
            }
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
            Intent intent2 = this.mCarDockIntent;
            intent = intent2;
        } else if (this.mUiMode == 2 || this.mUiMode != MSG_KEYGUARD_DRAWN_TIMEOUT || (this.mDockMode != 1 && this.mDockMode != MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK && this.mDockMode != 3)) {
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

    void startDockOrHome() {
        awakenDreams();
        Intent dock = createHomeDockIntent();
        if (dock != null) {
            try {
                startActivityAsUser(dock, UserHandle.CURRENT);
                return;
            } catch (ActivityNotFoundException e) {
            }
        }
        startActivityAsUser(this.mHomeIntent, UserHandle.CURRENT);
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
                    int result = ActivityManagerNative.getDefault().startActivityAsUser((IApplicationThread) null, (String) null, dock, dock.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 1, (ProfilerInfo) null, (Bundle) null, APPLICATION_MEDIA_SUBLAYER);
                    if (result == 1) {
                        return false;
                    }
                }
            }
            int result2 = ActivityManagerNative.getDefault().startActivityAsUser((IApplicationThread) null, (String) null, this.mHomeIntent, this.mHomeIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 1, (ProfilerInfo) null, (Bundle) null, APPLICATION_MEDIA_SUBLAYER);
            if (result2 == 1) {
                return false;
            }
        } catch (RemoteException e) {
        }
        return true;
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
        if (isGlobalAccessibilityGestureEnabled()) {
            AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
            if (!audioManager.isSilentMode()) {
                Ringtone ringTone = RingtoneManager.getRingtone(this.mContext, Settings.System.DEFAULT_NOTIFICATION_URI);
                ringTone.setStreamType(3);
                ringTone.play();
            }
        }
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
        boolean hapticsDisabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, APPLICATION_MEDIA_SUBLAYER) == 0;
        if (hapticsDisabled && !always) {
            return false;
        }
        switch (effectId) {
            case 0:
                pattern = this.mLongPressVibePattern;
                break;
            case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                pattern = this.mVirtualKeyVibePattern;
                break;
            case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
                pattern = this.mKeyboardTapVibePattern;
                break;
            case MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK:
                pattern = this.mClockTickVibePattern;
                break;
            case MSG_KEYGUARD_DRAWN_COMPLETE:
                pattern = this.mCalendarDateVibePattern;
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
            this.mVibrator.vibrate(owningUid, owningPackage, pattern, APPLICATION_MEDIA_OVERLAY_SUBLAYER, VIBRATION_ATTRIBUTES);
        }
        return true;
    }

    public void keepScreenOnStartedLw() {
    }

    public void keepScreenOnStoppedLw() {
        if (isKeyguardShowingAndNotOccluded()) {
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    private int updateSystemUiVisibilityLw() {
        final WindowManagerPolicy.WindowState win = this.mFocusedWindow != null ? this.mFocusedWindow : this.mTopFullscreenOpaqueWindowState;
        if (win == null) {
            return 0;
        }
        if ((win.getAttrs().privateFlags & 1024) != 0 && this.mHideLockScreen) {
            return 0;
        }
        int tmpVisibility = PolicyControl.getSystemUiVisibility(win, null) & (this.mResettingSystemUiFlags ^ APPLICATION_MEDIA_OVERLAY_SUBLAYER) & (this.mForceClearedSystemUiFlags ^ APPLICATION_MEDIA_OVERLAY_SUBLAYER);
        if (this.mForcingShowNavBar && win.getSurfaceLayer() < this.mForcingShowNavBarLayer) {
            tmpVisibility &= PolicyControl.adjustClearableFlags(win, MSG_WINDOW_MANAGER_DRAWN_COMPLETE) ^ APPLICATION_MEDIA_OVERLAY_SUBLAYER;
        }
        final int visibility = updateSystemBarsLw(win, this.mLastSystemUiFlags, tmpVisibility);
        int diff = visibility ^ this.mLastSystemUiFlags;
        final boolean needsMenu = win.getNeedsMenuLw(this.mTopFullscreenOpaqueWindowState);
        if (diff == 0 && this.mLastFocusNeedsMenu == needsMenu && this.mFocusedApp == win.getAppToken()) {
            return 0;
        }
        this.mLastSystemUiFlags = visibility;
        this.mLastFocusNeedsMenu = needsMenu;
        this.mFocusedApp = win.getAppToken();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    IStatusBarService statusbar = PhoneWindowManager.this.getStatusBarService();
                    if (statusbar != null) {
                        statusbar.setSystemUiVisibility(visibility, PhoneWindowManager.APPLICATION_MEDIA_OVERLAY_SUBLAYER, win.toString());
                        statusbar.topAppWindowChanged(needsMenu);
                    }
                } catch (RemoteException e) {
                    PhoneWindowManager.this.mStatusBarService = null;
                }
            }
        });
        return diff;
    }

    private int updateSystemBarsLw(WindowManagerPolicy.WindowState win, int oldVis, int vis) {
        WindowManagerPolicy.WindowState transWin = (!isStatusBarKeyguard() || this.mHideLockScreen) ? this.mTopFullscreenOpaqueWindowState : this.mStatusBar;
        int vis2 = this.mNavigationBarController.applyTranslucentFlagLw(transWin, this.mStatusBarController.applyTranslucentFlagLw(transWin, vis, oldVis), oldVis);
        boolean statusBarHasFocus = win.getAttrs().type == 2000;
        if (statusBarHasFocus && !isStatusBarKeyguard()) {
            int flags = this.mHideLockScreen ? 6150 | (-1073741824) : 6150;
            vis2 = ((flags ^ APPLICATION_MEDIA_OVERLAY_SUBLAYER) & vis2) | (oldVis & flags);
        }
        if (!areTranslucentBarsAllowed() && transWin != this.mStatusBar) {
            vis2 &= 1073709055;
        }
        boolean immersiveSticky = (vis2 & 4096) != 0;
        boolean hideStatusBarWM = (this.mTopFullscreenOpaqueWindowState == null || (PolicyControl.getWindowFlags(this.mTopFullscreenOpaqueWindowState, null) & 1024) == 0) ? false : true;
        boolean hideStatusBarSysui = (vis2 & MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK) != 0;
        boolean hideNavBarSysui = (this.mHideNavigationBar == 0 && (vis2 & 2) == 0) ? false : true;
        boolean transientStatusBarAllowed = this.mStatusBar != null && (hideStatusBarWM || ((hideStatusBarSysui && immersiveSticky) || statusBarHasFocus));
        boolean transientNavBarAllowed = this.mNavigationBar != null && hideNavBarSysui && immersiveSticky;
        boolean denyTransientStatus = this.mStatusBarController.isTransientShowRequested() && !transientStatusBarAllowed && hideStatusBarSysui;
        boolean denyTransientNav = this.mNavigationBarController.isTransientShowRequested() && !transientNavBarAllowed;
        if (denyTransientStatus || denyTransientNav) {
            clearClearableFlagsLw();
        }
        int vis3 = this.mStatusBarController.updateVisibilityLw(transientStatusBarAllowed, oldVis, vis2);
        boolean oldImmersiveMode = isImmersiveMode(oldVis);
        boolean newImmersiveMode = isImmersiveMode(vis3);
        if (win != null && oldImmersiveMode != newImmersiveMode) {
            String pkg = win.getOwningPackage();
            this.mImmersiveModeConfirmation.immersiveModeChanged(pkg, newImmersiveMode, isUserSetupComplete());
        }
        return this.mNavigationBarController.updateVisibilityLw(transientNavBarAllowed, oldVis, vis3);
    }

    private void clearClearableFlagsLw() {
        int newVal = this.mResettingSystemUiFlags | MSG_WINDOW_MANAGER_DRAWN_COMPLETE;
        if (newVal != this.mResettingSystemUiFlags) {
            this.mResettingSystemUiFlags = newVal;
            this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
        }
    }

    private boolean isImmersiveMode(int vis) {
        return (this.mNavigationBar == null || (vis & 2) == 0 || (vis & 6144) == 0 || (((vis & 2) == 0 || (vis & 6144) == 0) && this.mHideNavigationBar == 0) || !canHideNavigationBar()) ? false : true;
    }

    private boolean areTranslucentBarsAllowed() {
        return this.mTranslucentDecorEnabled && !this.mAccessibilityManager.isTouchExplorationEnabled();
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
        if (this.mStatusBarService != null) {
            try {
                this.mStatusBarService.setCurrentUser(newUserId);
            } catch (RemoteException e) {
            }
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
        return windowType < WAITING_FOR_DRAWN_TIMEOUT || windowType > 1999 || windowType == 1003;
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
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
        pw.print(" mLidControlsSleep=");
        pw.println(this.mLidControlsSleep);
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
        pw.println(this.mDreamingLockscreen);
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
            pw.println(this.mStatusBar);
            pw.print(prefix);
            pw.print("isStatusBarKeyguard=");
            pw.print(isStatusBarKeyguard());
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
    }
}
