package com.android.server.wm;

import android.R;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemService;
import android.os.Trace;
import android.os.WorkSource;
import android.powerhint.PowerHintManager;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.IApplicationToken;
import android.view.IInputFilter;
import android.view.IOnKeyguardExitResult;
import android.view.IRotationWatcher;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowManager;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MagnificationSpec;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.View;
import android.view.WindowContentFrameStats;
import android.view.WindowManager;
import android.view.WindowManagerInternal;
import android.view.WindowManagerPolicy;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import com.android.internal.app.IBatteryStats;
import com.android.internal.policy.PolicyManager;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.WindowManagerPolicyThread;
import com.android.server.AttributeCache;
import com.android.server.DisplayThread;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.am.BatteryStatsService;
import com.android.server.am.ProcessList;
import com.android.server.input.InputManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.voiceinteraction.SoundTriggerHelper;
import com.android.server.wm.WindowStateAnimator;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class WindowManagerService extends IWindowManager.Stub implements Watchdog.Monitor, WindowManagerPolicy.WindowManagerFuncs {
    static final int ADJUST_WALLPAPER_LAYERS_CHANGED = 2;
    static final int ADJUST_WALLPAPER_VISIBILITY_CHANGED = 4;
    private static final int BOOT_ANIMATION_POLL_INTERVAL = 200;
    private static final String BOOT_ANIMATION_SERVICE = "bootanim";
    static final boolean CUSTOM_SCREEN_ROTATION = true;
    static final boolean DEBUG = false;
    static final boolean DEBUG_ADD_REMOVE = false;
    static final boolean DEBUG_ANIM = false;
    static final boolean DEBUG_APP_ORIENTATION = false;
    static final boolean DEBUG_APP_TRANSITIONS = false;
    static final boolean DEBUG_BOOT = false;
    static final boolean DEBUG_CONFIGURATION = false;
    static final boolean DEBUG_DISPLAY = false;
    static final boolean DEBUG_DRAG = false;
    static final boolean DEBUG_FOCUS = false;
    static final boolean DEBUG_FOCUS_LIGHT = false;
    static final boolean DEBUG_INPUT = false;
    static final boolean DEBUG_INPUT_METHOD = false;
    static final boolean DEBUG_KEYGUARD = false;
    static final boolean DEBUG_LAYERS = false;
    static final boolean DEBUG_LAYOUT = false;
    static final boolean DEBUG_LAYOUT_REPEATS = true;
    static final boolean DEBUG_ORIENTATION = false;
    static final boolean DEBUG_REORDER = false;
    static final boolean DEBUG_RESIZE = false;
    static final boolean DEBUG_SCREENSHOT = false;
    static final boolean DEBUG_SCREEN_ON = false;
    static final boolean DEBUG_STACK = false;
    static final boolean DEBUG_STARTING_WINDOW = false;
    static final boolean DEBUG_SURFACE_TRACE = false;
    static final boolean DEBUG_TASK_MOVEMENT = false;
    static final boolean DEBUG_TOKEN_MOVEMENT = false;
    static final boolean DEBUG_VISIBILITY = false;
    static final boolean DEBUG_WALLPAPER = false;
    static final boolean DEBUG_WALLPAPER_LIGHT = false;
    static final boolean DEBUG_WINDOW_MOVEMENT = false;
    static final boolean DEBUG_WINDOW_TRACE = false;
    static final int DEFAULT_FADE_IN_OUT_DURATION = 400;
    static final long DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS = 5000000000L;
    private static final String DENSITY_OVERRIDE = "ro.config.density_override";
    static final int FREEZE_LAYER = 2000001;
    static final boolean HIDE_STACK_CRAWLS = true;
    private static final int INPUT_DEVICES_READY_FOR_SAFE_MODE_DETECTION_TIMEOUT_MILLIS = 1000;
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    static final int LAYER_OFFSET_BLUR = 2;
    static final int LAYER_OFFSET_DIM = 1;
    static final int LAYER_OFFSET_FOCUSED_STACK = 1;
    static final int LAYER_OFFSET_THUMBNAIL = 4;
    static final int LAYOUT_REPEAT_THRESHOLD = 4;
    static final int MASK_LAYER = 2000000;
    static final int MAX_ANIMATION_DURATION = 10000;
    private static final int MAX_SCREENSHOT_RETRIES = 3;
    static final boolean PROFILE_ORIENTATION = false;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    static final boolean SHOW_LIGHT_TRANSACTIONS = false;
    static final boolean SHOW_SURFACE_ALLOC = false;
    static final boolean SHOW_TRANSACTIONS = false;
    private static final String SIZE_OVERRIDE = "ro.config.size_override";
    public static final float STACK_WEIGHT_MAX = 0.8f;
    public static final float STACK_WEIGHT_MIN = 0.2f;
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    private static final String SYSTEM_SECURE = "ro.secure";
    private static final int SYSTEM_UI_FLAGS_LAYOUT_STABLE_FULLSCREEN = 1280;
    static final String TAG = "WindowManager";
    static final int TYPE_LAYER_MULTIPLIER = 10000;
    static final int TYPE_LAYER_OFFSET = 1000;
    static final int UPDATE_FOCUS_NORMAL = 0;
    static final int UPDATE_FOCUS_PLACING_SURFACES = 2;
    static final int UPDATE_FOCUS_WILL_ASSIGN_LAYERS = 1;
    static final int UPDATE_FOCUS_WILL_PLACE_SURFACES = 3;
    static final long WALLPAPER_TIMEOUT = 150;
    static final long WALLPAPER_TIMEOUT_RECOVERY = 10000;
    static final int WINDOW_FREEZE_TIMEOUT_DURATION = 2000;
    static final int WINDOW_LAYER_MULTIPLIER = 5;
    static final boolean localLOGV = false;
    AccessibilityController mAccessibilityController;
    final IActivityManager mActivityManager;
    final boolean mAllowBootMessages;
    boolean mAllowTheaterModeWakeFromLayout;
    boolean mAltOrientation;
    boolean mAnimateWallpaperWithTarget;
    boolean mAnimationScheduled;
    boolean mAnimationsDisabled;
    final WindowAnimator mAnimator;
    float mAnimatorDurationScaleSetting;
    final AppOpsManager mAppOps;
    final AppTransition mAppTransition;
    int mAppsFreezingScreen;
    final IBatteryStats mBatteryStats;
    boolean mBootAnimationStopped;
    final BroadcastReceiver mBroadcastReceiver;
    final Choreographer mChoreographer;
    CircularDisplayMask mCircularDisplayMask;
    boolean mClientFreezingScreen;
    final ArraySet<AppWindowToken> mClosingApps;
    final DisplayMetrics mCompatDisplayMetrics;
    float mCompatibleScreenScale;
    final Context mContext;
    Configuration mCurConfiguration;
    WindowState mCurrentFocus;
    int[] mCurrentProfileIds;
    int mCurrentUserId;
    int mDeferredRotationPauseCount;
    final ArrayList<WindowState> mDestroySurface;
    SparseArray<DisplayContent> mDisplayContents;
    boolean mDisplayEnabled;
    long mDisplayFreezeTime;
    boolean mDisplayFrozen;
    final DisplayManager mDisplayManager;
    final DisplayManagerInternal mDisplayManagerInternal;
    final DisplayMetrics mDisplayMetrics;
    boolean mDisplayReady;
    final DisplaySettings mDisplaySettings;
    DragState mDragState;
    EmulatorDisplayOverlay mEmulatorDisplayOverlay;
    int mEnterAnimId;
    private boolean mEventDispatchingEnabled;
    int mExitAnimId;
    final ArrayList<FakeWindowImpl> mFakeWindows;
    final ArrayList<AppWindowToken> mFinishedStarting;
    boolean mFocusMayChange;
    AppWindowToken mFocusedApp;
    FocusedStackFrame mFocusedStackFrame;
    int mFocusedStackLayer;
    boolean mForceDisplayEnabled;
    ArrayList<WindowState> mForceRemoves;
    int mForcedAppOrientation;
    final SurfaceSession mFxSession;
    final H mH;
    boolean mHardKeyboardAvailable;
    OnHardKeyboardStatusChangeListener mHardKeyboardStatusChangeListener;
    final boolean mHasPermanentDpad;
    final boolean mHaveInputMethods;
    private ArrayList<WindowState> mHidingNonSystemOverlayWindows;
    Session mHoldingScreenOn;
    PowerManager.WakeLock mHoldingScreenWakeLock;
    private boolean mInLayout;
    boolean mInTouchMode;
    final LayoutFields mInnerFields;
    final InputManagerService mInputManager;
    int mInputMethodAnimLayerAdjustment;
    final ArrayList<WindowState> mInputMethodDialogs;
    IInputMethodManager mInputMethodManager;
    WindowState mInputMethodTarget;
    boolean mInputMethodTargetWaitingAnim;
    WindowState mInputMethodWindow;
    final InputMonitor mInputMonitor;
    boolean mIsTouchDevice;
    private final KeyguardDisableHandler mKeyguardDisableHandler;
    private boolean mKeyguardWaitingForActivityDrawn;
    String mLastANRState;
    int mLastDisplayFreezeDuration;
    Object mLastFinishedFreezeSource;
    WindowState mLastFocus;
    int mLastStatusBarVisibility;
    int mLastWallpaperDisplayOffsetX;
    int mLastWallpaperDisplayOffsetY;
    long mLastWallpaperTimeoutTime;
    float mLastWallpaperX;
    float mLastWallpaperXStep;
    float mLastWallpaperY;
    float mLastWallpaperYStep;
    int mLastWindowForcedOrientation;
    private int mLayoutRepeatCount;
    int mLayoutSeq;
    final boolean mLimitedAlphaCompositing;
    ArrayList<WindowState> mLosingFocus;
    WindowState mLowerWallpaperTarget;
    final boolean mOnlyCore;
    final ArraySet<AppWindowToken> mOpeningApps;
    final ArrayList<WindowState> mPendingRemove;
    WindowState[] mPendingRemoveTmp;
    final ArraySet<TaskStack> mPendingStacksRemove;
    private final PointerEventDispatcher mPointerEventDispatcher;
    final WindowManagerPolicy mPolicy;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    final DisplayMetrics mRealDisplayMetrics;
    WindowState[] mRebuildTmp;
    final ArrayList<WindowState> mRelayoutWhileAnimating;
    final ArrayList<WindowState> mResizingWindows;
    int mRotation;
    ArrayList<RotationWatcher> mRotationWatchers;
    boolean mSafeMode;
    SparseArray<Boolean> mScreenCaptureDisabled;
    private final PowerManager.WakeLock mScreenFrozenLock;
    final Rect mScreenRect;
    final ArraySet<Session> mSessions;
    SettingsObserver mSettingsObserver;
    boolean mShowImeWithHardKeyboard;
    boolean mShowingBootMessages;
    boolean mSkipAppTransitionAnimation;
    SparseArray<TaskStack> mStackIdToStack;
    boolean mStartingIconInTransition;
    StrictModeFlash mStrictModeFlash;
    boolean mSystemBooted;
    int mSystemDecorLayer;
    SparseArray<Task> mTaskIdToTask;
    final Configuration mTempConfiguration;
    private WindowContentFrameStats mTempWindowRenderStats;
    final Rect mTmpContentRect;
    final DisplayMetrics mTmpDisplayMetrics;
    final float[] mTmpFloats;
    final HashMap<IBinder, WindowToken> mTokenMap;
    private int mTransactionSequence;
    float mTransitionAnimationScaleSetting;
    boolean mTraversalScheduled;
    boolean mTurnOnScreen;
    WindowState mUpperWallpaperTarget;
    private ViewServer mViewServer;
    boolean mWaitingForConfig;
    ArrayList<WindowState> mWaitingForDrawn;
    Runnable mWaitingForDrawnCallback;
    WindowState mWaitingOnWallpaper;
    int mWallpaperAnimLayerAdjustment;
    WindowState mWallpaperTarget;
    final ArrayList<WindowToken> mWallpaperTokens;
    Watermark mWatermark;
    float mWindowAnimationScaleSetting;
    private final ArrayList<WindowChangeListener> mWindowChangeListeners;
    final HashMap<IBinder, WindowState> mWindowMap;
    private boolean mWindowsChanged;
    boolean mWindowsFreezingScreen;

    public interface OnHardKeyboardStatusChangeListener {
        void onHardKeyboardStatusChange(boolean z);
    }

    public interface WindowChangeListener {
        void focusChanged();

        void windowsChanged();
    }

    class RotationWatcher {
        IBinder.DeathRecipient deathRecipient;
        IRotationWatcher watcher;

        RotationWatcher(IRotationWatcher w, IBinder.DeathRecipient d) {
            this.watcher = w;
            this.deathRecipient = d;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri mDisplayInversionEnabledUri;
        private final Uri mShowImeWithHardKeyboardUri;

        public SettingsObserver() {
            super(new Handler());
            this.mShowImeWithHardKeyboardUri = Settings.Secure.getUriFor("show_ime_with_hard_keyboard");
            this.mDisplayInversionEnabledUri = Settings.Secure.getUriFor("accessibility_display_inversion_enabled");
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.mShowImeWithHardKeyboardUri, false, this);
            resolver.registerContentObserver(this.mDisplayInversionEnabledUri, false, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (this.mShowImeWithHardKeyboardUri.equals(uri)) {
                WindowManagerService.this.updateShowImeWithHardKeyboard();
            } else if (this.mDisplayInversionEnabledUri.equals(uri)) {
                WindowManagerService.this.updateCircularDisplayMaskIfNeeded();
            }
        }
    }

    class LayoutFields {
        static final int SET_FORCE_HIDING_CHANGED = 4;
        static final int SET_ORIENTATION_CHANGE_COMPLETE = 8;
        static final int SET_TURN_ON_SCREEN = 16;
        static final int SET_UPDATE_ROTATION = 1;
        static final int SET_WALLPAPER_ACTION_PENDING = 32;
        static final int SET_WALLPAPER_MAY_CHANGE = 2;
        boolean mWallpaperForceHidingChanged = false;
        boolean mWallpaperMayChange = false;
        boolean mOrientationChangeComplete = true;
        Object mLastWindowFreezeSource = null;
        private Session mHoldScreen = null;
        private boolean mObscured = false;
        private boolean mSyswin = false;
        private float mScreenBrightness = -1.0f;
        private float mButtonBrightness = -1.0f;
        private long mUserActivityTimeout = -1;
        private boolean mUpdateRotation = false;
        boolean mWallpaperActionPending = false;
        boolean mDisplayHasContent = false;
        boolean mObscureApplicationContentOnSecondaryDisplays = false;
        float mPreferredRefreshRate = 0.0f;

        LayoutFields() {
        }
    }

    final class DragInputEventReceiver extends InputEventReceiver {
        public DragInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent event) {
            boolean handled = false;
            try {
                if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0 && WindowManagerService.this.mDragState != null) {
                    MotionEvent motionEvent = (MotionEvent) event;
                    boolean endDrag = false;
                    float newX = motionEvent.getRawX();
                    float newY = motionEvent.getRawY();
                    switch (motionEvent.getAction()) {
                        case 0:
                        default:
                            if (endDrag) {
                                synchronized (WindowManagerService.this.mWindowMap) {
                                    WindowManagerService.this.mDragState.endDragLw();
                                }
                            }
                            handled = true;
                            break;
                        case 1:
                            synchronized (WindowManagerService.this.mWindowMap) {
                                endDrag = WindowManagerService.this.mDragState.notifyDropLw(newX, newY);
                                break;
                            }
                            if (endDrag) {
                            }
                            handled = true;
                            break;
                        case 2:
                            synchronized (WindowManagerService.this.mWindowMap) {
                                WindowManagerService.this.mDragState.notifyMoveLw(newX, newY);
                                break;
                            }
                            if (endDrag) {
                            }
                            handled = true;
                            break;
                        case 3:
                            endDrag = true;
                            if (endDrag) {
                            }
                            handled = true;
                            break;
                    }
                }
            } catch (Exception e) {
                Slog.e(WindowManagerService.TAG, "Exception caught by drag handleMotion", e);
            } finally {
                finishInputEvent(event, false);
            }
        }
    }

    public static WindowManagerService main(final Context context, final InputManagerService im, final boolean haveInputMethods, final boolean showBootMsgs, final boolean onlyCore) {
        final WindowManagerService[] holder = new WindowManagerService[1];
        DisplayThread.getHandler().runWithScissors(new Runnable() {
            @Override
            public void run() {
                holder[0] = new WindowManagerService(context, im, haveInputMethods, showBootMsgs, onlyCore);
            }
        }, 0L);
        return holder[0];
    }

    private void initPolicy() {
        UiThread.getHandler().runWithScissors(new Runnable() {
            @Override
            public void run() {
                WindowManagerPolicyThread.set(Thread.currentThread(), Looper.myLooper());
                WindowManagerService.this.mPolicy.init(WindowManagerService.this.mContext, WindowManagerService.this, WindowManagerService.this);
                WindowManagerService.this.mAnimator.mAboveUniverseLayer = (WindowManagerService.this.mPolicy.getAboveUniverseLayer() * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 1000;
            }
        }, 0L);
    }

    private WindowManagerService(Context context, InputManagerService inputManager, boolean haveInputMethods, boolean showBootMsgs, boolean onlyCore) {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                    WindowManagerService.this.mKeyguardDisableHandler.sendEmptyMessage(3);
                }
            }
        };
        this.mCurrentProfileIds = new int[]{0};
        this.mPolicy = PolicyManager.makeNewWindowManager();
        this.mSessions = new ArraySet<>();
        this.mWindowMap = new HashMap<>();
        this.mTokenMap = new HashMap<>();
        this.mFinishedStarting = new ArrayList<>();
        this.mFakeWindows = new ArrayList<>();
        this.mResizingWindows = new ArrayList<>();
        this.mPendingRemove = new ArrayList<>();
        this.mPendingStacksRemove = new ArraySet<>();
        this.mPendingRemoveTmp = new WindowState[20];
        this.mDestroySurface = new ArrayList<>();
        this.mLosingFocus = new ArrayList<>();
        this.mHidingNonSystemOverlayWindows = new ArrayList<>();
        this.mWaitingForDrawn = new ArrayList<>();
        this.mRelayoutWhileAnimating = new ArrayList<>();
        this.mRebuildTmp = new WindowState[20];
        this.mScreenCaptureDisabled = new SparseArray<>();
        this.mTmpFloats = new float[9];
        this.mTmpContentRect = new Rect();
        this.mDisplayEnabled = false;
        this.mSystemBooted = false;
        this.mForceDisplayEnabled = false;
        this.mShowingBootMessages = false;
        this.mBootAnimationStopped = false;
        this.mDisplayContents = new SparseArray<>(2);
        this.mRotation = SystemProperties.getInt("ro.wm.default_rotation", 0);
        this.mForcedAppOrientation = -1;
        this.mAltOrientation = false;
        this.mRotationWatchers = new ArrayList<>();
        this.mSystemDecorLayer = 0;
        this.mScreenRect = new Rect();
        this.mTraversalScheduled = false;
        this.mDisplayFrozen = false;
        this.mDisplayFreezeTime = 0L;
        this.mLastDisplayFreezeDuration = 0;
        this.mLastFinishedFreezeSource = null;
        this.mWaitingForConfig = false;
        this.mWindowsFreezingScreen = false;
        this.mClientFreezingScreen = false;
        this.mAppsFreezingScreen = 0;
        this.mLastWindowForcedOrientation = -1;
        this.mLayoutSeq = 0;
        this.mLastStatusBarVisibility = 0;
        this.mCurConfiguration = new Configuration();
        this.mStartingIconInTransition = false;
        this.mSkipAppTransitionAnimation = false;
        this.mOpeningApps = new ArraySet<>();
        this.mClosingApps = new ArraySet<>();
        this.mDisplayMetrics = new DisplayMetrics();
        this.mRealDisplayMetrics = new DisplayMetrics();
        this.mTmpDisplayMetrics = new DisplayMetrics();
        this.mCompatDisplayMetrics = new DisplayMetrics();
        this.mH = new H();
        this.mChoreographer = Choreographer.getInstance();
        this.mCurrentFocus = null;
        this.mLastFocus = null;
        this.mInputMethodTarget = null;
        this.mInputMethodWindow = null;
        this.mInputMethodDialogs = new ArrayList<>();
        this.mWallpaperTokens = new ArrayList<>();
        this.mWallpaperTarget = null;
        this.mLowerWallpaperTarget = null;
        this.mUpperWallpaperTarget = null;
        this.mLastWallpaperX = -1.0f;
        this.mLastWallpaperY = -1.0f;
        this.mLastWallpaperXStep = -1.0f;
        this.mLastWallpaperYStep = -1.0f;
        this.mLastWallpaperDisplayOffsetX = SoundTriggerHelper.STATUS_ERROR;
        this.mLastWallpaperDisplayOffsetY = SoundTriggerHelper.STATUS_ERROR;
        this.mFocusedApp = null;
        this.mWindowAnimationScaleSetting = 1.0f;
        this.mTransitionAnimationScaleSetting = 1.0f;
        this.mAnimatorDurationScaleSetting = 1.0f;
        this.mAnimationsDisabled = false;
        this.mDragState = null;
        this.mInnerFields = new LayoutFields();
        this.mTaskIdToTask = new SparseArray<>();
        this.mStackIdToStack = new SparseArray<>();
        this.mWindowChangeListeners = new ArrayList<>();
        this.mWindowsChanged = false;
        this.mTempConfiguration = new Configuration();
        this.mInputMonitor = new InputMonitor(this);
        this.mInLayout = false;
        this.mContext = context;
        this.mHaveInputMethods = haveInputMethods;
        this.mAllowBootMessages = showBootMsgs;
        this.mOnlyCore = onlyCore;
        this.mLimitedAlphaCompositing = context.getResources().getBoolean(R.^attr-private.activityOpenRemoteViewsEnterAnimation);
        this.mHasPermanentDpad = context.getResources().getBoolean(R.^attr-private.keepDotActivated);
        this.mInTouchMode = context.getResources().getBoolean(R.^attr-private.majorWeightMax);
        this.mInputManager = inputManager;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplaySettings = new DisplaySettings();
        this.mDisplaySettings.readSettingsLocked();
        LocalServices.addService(WindowManagerPolicy.class, this.mPolicy);
        this.mPointerEventDispatcher = new PointerEventDispatcher(this.mInputManager.monitorInput(TAG));
        this.mFxSession = new SurfaceSession();
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        Display[] displays = this.mDisplayManager.getDisplays();
        for (Display display : displays) {
            createDisplayContentLocked(display);
        }
        this.mKeyguardDisableHandler = new KeyguardDisableHandler(this.mContext, this.mPolicy);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
            public void onLowPowerModeChanged(boolean enabled) {
                synchronized (WindowManagerService.this.mWindowMap) {
                    if (WindowManagerService.this.mAnimationsDisabled != enabled) {
                        WindowManagerService.this.mAnimationsDisabled = enabled;
                        WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                    }
                }
            }
        });
        this.mAnimationsDisabled = this.mPowerManagerInternal.getLowPowerModeEnabled();
        this.mScreenFrozenLock = this.mPowerManager.newWakeLock(1, "SCREEN_FROZEN");
        this.mScreenFrozenLock.setReferenceCounted(false);
        this.mAppTransition = new AppTransition(context, this.mH);
        this.mActivityManager = ActivityManagerNative.getDefault();
        this.mBatteryStats = BatteryStatsService.getService();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        AppOpsManager.OnOpChangedListener onOpChangedListener = new AppOpsManager.OnOpChangedInternalListener() {
            public void onOpChanged(int op, String packageName) {
                WindowManagerService.this.updateAppOpsState();
            }
        };
        this.mAppOps.startWatchingMode(24, (String) null, onOpChangedListener);
        this.mAppOps.startWatchingMode(45, (String) null, onOpChangedListener);
        this.mWindowAnimationScaleSetting = Settings.Global.getFloat(context.getContentResolver(), "window_animation_scale", this.mWindowAnimationScaleSetting);
        this.mTransitionAnimationScaleSetting = Settings.Global.getFloat(context.getContentResolver(), "transition_animation_scale", this.mTransitionAnimationScaleSetting);
        setAnimatorDurationScale(Settings.Global.getFloat(context.getContentResolver(), "animator_duration_scale", this.mAnimatorDurationScaleSetting));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mSettingsObserver = new SettingsObserver();
        updateShowImeWithHardKeyboard();
        this.mHoldingScreenWakeLock = this.mPowerManager.newWakeLock(536870922, TAG);
        this.mHoldingScreenWakeLock.setReferenceCounted(false);
        this.mAnimator = new WindowAnimator(this);
        this.mAllowTheaterModeWakeFromLayout = context.getResources().getBoolean(R.^attr-private.colorProgressBackgroundNormal);
        LocalServices.addService(WindowManagerInternal.class, new LocalService());
        initPolicy();
        Watchdog.getInstance().addMonitor(this);
        SurfaceControl.openTransaction();
        try {
            createWatermarkInTransaction();
            this.mFocusedStackFrame = new FocusedStackFrame(getDefaultDisplayContentLocked().getDisplay(), this.mFxSession);
            SurfaceControl.closeTransaction();
            updateCircularDisplayMaskIfNeeded();
            showEmulatorDisplayOverlayIfNeeded();
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
            throw th;
        }
    }

    public InputMonitor getInputMonitor() {
        return this.mInputMonitor;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Window Manager Crash", e);
            }
            throw e;
        }
    }

    private void placeWindowAfter(WindowState pos, WindowState window) {
        WindowList windows = pos.getWindowList();
        int i = windows.indexOf(pos);
        Slog.v(TAG, "Adding window " + window + " at " + (i + 1) + " of " + windows.size() + " (after " + pos + ")");
        windows.add(i + 1, window);
        this.mWindowsChanged = true;
    }

    private void placeWindowBefore(WindowState pos, WindowState window) {
        WindowList windows = pos.getWindowList();
        int i = windows.indexOf(pos);
        Slog.v(TAG, "Adding window " + window + " at " + i + " of " + windows.size() + " (before " + pos + ")");
        if (i < 0) {
            Slog.w(TAG, "placeWindowBefore: Unable to find " + pos + " in " + windows);
            i = 0;
        }
        windows.add(i, window);
        this.mWindowsChanged = true;
    }

    private int findIdxBasedOnAppTokens(WindowState win) {
        WindowList windows = win.getWindowList();
        for (int j = windows.size() - 1; j >= 0; j--) {
            WindowState wentry = windows.get(j);
            if (wentry.mAppToken == win.mAppToken) {
                return j;
            }
        }
        return -1;
    }

    WindowList getTokenWindowsOnDisplay(WindowToken token, DisplayContent displayContent) {
        WindowList windowList = new WindowList();
        int count = token.windows.size();
        for (int i = 0; i < count; i++) {
            WindowState win = token.windows.get(i);
            if (win.getDisplayContent() == displayContent) {
                windowList.add(win);
            }
        }
        return windowList;
    }

    private int indexOfWinInWindowList(WindowState targetWin, WindowList windows) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowState w = windows.get(i);
            if (w != targetWin) {
                if (!w.mChildWindows.isEmpty() && indexOfWinInWindowList(targetWin, w.mChildWindows) >= 0) {
                    return i;
                }
            } else {
                return i;
            }
        }
        return -1;
    }

    private int addAppWindowToListLocked(WindowState win) {
        int NC;
        int tokenWindowsPos;
        IWindow iWindow = win.mClient;
        WindowToken token = win.mToken;
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent == null) {
            return 0;
        }
        WindowList windows = win.getWindowList();
        int N = windows.size();
        WindowList tokenWindowList = getTokenWindowsOnDisplay(token, displayContent);
        int windowListPos = tokenWindowList.size();
        if (!tokenWindowList.isEmpty()) {
            if (win.mAttrs.type == 1) {
                WindowState lowestWindow = tokenWindowList.get(0);
                placeWindowBefore(lowestWindow, win);
                int tokenWindowsPos2 = indexOfWinInWindowList(lowestWindow, token.windows);
                return tokenWindowsPos2;
            }
            AppWindowToken atoken = win.mAppToken;
            WindowState lastWindow = tokenWindowList.get(windowListPos - 1);
            if (atoken != null && lastWindow == atoken.startingWindow) {
                placeWindowBefore(lastWindow, win);
                int tokenWindowsPos3 = indexOfWinInWindowList(lastWindow, token.windows);
                return tokenWindowsPos3;
            }
            int newIdx = findIdxBasedOnAppTokens(win);
            Slog.v(TAG, "not Base app: Adding window " + win + " at " + (newIdx + 1) + " of " + N);
            windows.add(newIdx + 1, win);
            if (newIdx < 0) {
                tokenWindowsPos = 0;
            } else {
                tokenWindowsPos = indexOfWinInWindowList(windows.get(newIdx), token.windows) + 1;
            }
            this.mWindowsChanged = true;
            return tokenWindowsPos;
        }
        WindowState pos = null;
        ArrayList<Task> tasks = displayContent.getTasks();
        int tokenNdx = -1;
        int taskNdx = tasks.size() - 1;
        while (taskNdx >= 0) {
            AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
            tokenNdx = tokens.size() - 1;
            while (true) {
                if (tokenNdx < 0) {
                    break;
                }
                AppWindowToken t = tokens.get(tokenNdx);
                if (t == token) {
                    tokenNdx--;
                    if (tokenNdx < 0 && taskNdx - 1 >= 0) {
                        tokenNdx = tasks.get(taskNdx).mAppTokens.size() - 1;
                    }
                } else {
                    WindowList tokenWindowList2 = getTokenWindowsOnDisplay(t, displayContent);
                    if (!t.sendingToBottom && tokenWindowList2.size() > 0) {
                        WindowState pos2 = tokenWindowList2.get(0);
                        pos = pos2;
                    }
                    tokenNdx--;
                }
            }
            if (tokenNdx >= 0) {
                break;
            }
            taskNdx--;
        }
        if (pos != null) {
            WindowToken atoken2 = this.mTokenMap.get(pos.mClient.asBinder());
            if (atoken2 != null) {
                WindowList tokenWindowList3 = getTokenWindowsOnDisplay(atoken2, displayContent);
                if (tokenWindowList3.size() > 0) {
                    WindowState bottom = tokenWindowList3.get(0);
                    if (bottom.mSubLayer < 0) {
                        pos = bottom;
                    }
                }
            }
            placeWindowBefore(pos, win);
            return 0;
        }
        while (taskNdx >= 0) {
            AppTokenList tokens2 = tasks.get(taskNdx).mAppTokens;
            while (true) {
                if (tokenNdx < 0) {
                    break;
                }
                WindowList tokenWindowList4 = getTokenWindowsOnDisplay((AppWindowToken) tokens2.get(tokenNdx), displayContent);
                int NW = tokenWindowList4.size();
                if (NW <= 0) {
                    tokenNdx--;
                } else {
                    WindowState pos3 = tokenWindowList4.get(NW - 1);
                    pos = pos3;
                    break;
                }
            }
            if (tokenNdx >= 0) {
                break;
            }
            taskNdx--;
        }
        if (pos != null) {
            WindowToken atoken3 = this.mTokenMap.get(pos.mClient.asBinder());
            if (atoken3 != null && (NC = atoken3.windows.size()) > 0) {
                WindowState top = atoken3.windows.get(NC - 1);
                if (top.mSubLayer >= 0) {
                    pos = top;
                }
            }
            placeWindowAfter(pos, win);
            return 0;
        }
        int myLayer = win.mBaseLayer;
        int i = N - 1;
        while (i >= 0) {
            WindowState w = windows.get(i);
            if (w.mBaseLayer <= myLayer) {
                break;
            }
            i--;
        }
        Slog.v(TAG, "Based on layer: Adding window " + win + " at " + (i + 1) + " of " + N);
        windows.add(i + 1, win);
        this.mWindowsChanged = true;
        return 0;
    }

    private void addFreeWindowToListLocked(WindowState win) {
        WindowList windows = win.getWindowList();
        int myLayer = win.mBaseLayer;
        int i = windows.size() - 1;
        while (i >= 0 && windows.get(i).mBaseLayer > myLayer) {
            i--;
        }
        windows.add(i + 1, win);
        this.mWindowsChanged = true;
    }

    private void addAttachedWindowToListLocked(WindowState win, boolean addToToken) {
        WindowToken token = win.mToken;
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent != null) {
            WindowState attached = win.mAttachedWindow;
            WindowList tokenWindowList = getTokenWindowsOnDisplay(token, displayContent);
            int NA = tokenWindowList.size();
            int sublayer = win.mSubLayer;
            int largestSublayer = SoundTriggerHelper.STATUS_ERROR;
            WindowState windowWithLargestSublayer = null;
            int i = 0;
            while (true) {
                if (i >= NA) {
                    break;
                }
                WindowState w = tokenWindowList.get(i);
                int wSublayer = w.mSubLayer;
                if (wSublayer >= largestSublayer) {
                    largestSublayer = wSublayer;
                    windowWithLargestSublayer = w;
                }
                if (sublayer < 0) {
                    if (wSublayer < sublayer) {
                        i++;
                    } else {
                        if (addToToken) {
                            token.windows.add(i, win);
                        }
                        if (wSublayer >= 0) {
                            w = attached;
                        }
                        placeWindowBefore(w, win);
                    }
                } else if (wSublayer <= sublayer) {
                    i++;
                } else {
                    if (addToToken) {
                        token.windows.add(i, win);
                    }
                    placeWindowBefore(w, win);
                }
            }
            if (i >= NA) {
                if (addToToken) {
                    token.windows.add(win);
                }
                if (sublayer < 0) {
                    placeWindowBefore(attached, win);
                    return;
                }
                if (largestSublayer < 0) {
                    windowWithLargestSublayer = attached;
                }
                placeWindowAfter(windowWithLargestSublayer, win);
            }
        }
    }

    private void addWindowToListInOrderLocked(WindowState win, boolean addToToken) {
        if (win.mAttachedWindow == null) {
            WindowToken token = win.mToken;
            int tokenWindowsPos = 0;
            if (token.appWindowToken != null) {
                tokenWindowsPos = addAppWindowToListLocked(win);
            } else {
                addFreeWindowToListLocked(win);
            }
            if (addToToken) {
                token.windows.add(tokenWindowsPos, win);
            }
        } else {
            addAttachedWindowToListLocked(win, addToToken);
        }
        if (win.mAppToken != null && addToToken) {
            win.mAppToken.allAppWindows.add(win);
        }
    }

    static boolean canBeImeTarget(WindowState w) {
        int fl = w.mAttrs.flags & 131080;
        if (fl == 0 || fl == 131080 || w.mAttrs.type == 3) {
            return w.isVisibleOrAdding();
        }
        return false;
    }

    int findDesiredInputMethodWindowIndexLocked(boolean willMove) {
        WindowList windows = getDefaultWindowListLocked();
        WindowState w = null;
        int i = windows.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            WindowState win = (WindowState) windows.get(i);
            if (canBeImeTarget(win)) {
                w = win;
                if (!willMove && w.mAttrs.type == 3 && i > 0) {
                    WindowState wb = (WindowState) windows.get(i - 1);
                    if (wb.mAppToken == w.mAppToken && canBeImeTarget(wb)) {
                        i--;
                        w = wb;
                    }
                }
            } else {
                i--;
            }
        }
        WindowState curTarget = this.mInputMethodTarget;
        if (curTarget != null && curTarget.isDisplayedLw() && curTarget.isClosing() && (w == null || curTarget.mWinAnimator.mAnimLayer > w.mWinAnimator.mAnimLayer)) {
            return windows.indexOf(curTarget) + 1;
        }
        if (willMove && w != null) {
            AppWindowToken token = curTarget == null ? null : curTarget.mAppToken;
            if (token != null) {
                WindowState highestTarget = null;
                int highestPos = 0;
                if (token.mAppAnimator.animating || token.mAppAnimator.animation != null) {
                    WindowList curWindows = curTarget.getWindowList();
                    for (int pos = curWindows.indexOf(curTarget); pos >= 0; pos--) {
                        WindowState win2 = curWindows.get(pos);
                        if (win2.mAppToken != token) {
                            break;
                        }
                        if (!win2.mRemoved && (highestTarget == null || win2.mWinAnimator.mAnimLayer > highestTarget.mWinAnimator.mAnimLayer)) {
                            highestTarget = win2;
                            highestPos = pos;
                        }
                    }
                }
                if (highestTarget != null) {
                    if (this.mAppTransition.isTransitionSet()) {
                        this.mInputMethodTargetWaitingAnim = true;
                        this.mInputMethodTarget = highestTarget;
                        return highestPos + 1;
                    }
                    if (highestTarget.mWinAnimator.isAnimating() && highestTarget.mWinAnimator.mAnimLayer > w.mWinAnimator.mAnimLayer) {
                        this.mInputMethodTargetWaitingAnim = true;
                        this.mInputMethodTarget = highestTarget;
                        return highestPos + 1;
                    }
                }
            }
        }
        if (w != null) {
            if (willMove) {
                this.mInputMethodTarget = w;
                this.mInputMethodTargetWaitingAnim = false;
                if (w.mAppToken != null) {
                    setInputMethodAnimLayerAdjustment(w.mAppToken.mAppAnimator.animLayerAdjustment);
                } else {
                    setInputMethodAnimLayerAdjustment(0);
                }
            }
            return i + 1;
        }
        if (willMove) {
            this.mInputMethodTarget = null;
            setInputMethodAnimLayerAdjustment(0);
        }
        return -1;
    }

    void addInputMethodWindowToListLocked(WindowState win) {
        int pos = findDesiredInputMethodWindowIndexLocked(true);
        if (pos >= 0) {
            win.mTargetAppToken = this.mInputMethodTarget.mAppToken;
            getDefaultWindowListLocked().add(pos, win);
            this.mWindowsChanged = true;
            moveInputMethodDialogsLocked(pos + 1);
            return;
        }
        win.mTargetAppToken = null;
        addWindowToListInOrderLocked(win, true);
        moveInputMethodDialogsLocked(pos);
    }

    void setInputMethodAnimLayerAdjustment(int adj) {
        this.mInputMethodAnimLayerAdjustment = adj;
        WindowState imw = this.mInputMethodWindow;
        if (imw != null) {
            imw.mWinAnimator.mAnimLayer = imw.mLayer + adj;
            int wi = imw.mChildWindows.size();
            while (wi > 0) {
                wi--;
                WindowState cw = imw.mChildWindows.get(wi);
                cw.mWinAnimator.mAnimLayer = cw.mLayer + adj;
            }
        }
        int di = this.mInputMethodDialogs.size();
        while (di > 0) {
            di--;
            WindowState imw2 = this.mInputMethodDialogs.get(di);
            imw2.mWinAnimator.mAnimLayer = imw2.mLayer + adj;
        }
    }

    private int tmpRemoveWindowLocked(int interestingPos, WindowState win) {
        WindowList windows = win.getWindowList();
        int wpos = windows.indexOf(win);
        if (wpos >= 0) {
            if (wpos < interestingPos) {
                interestingPos--;
            }
            windows.remove(wpos);
            this.mWindowsChanged = true;
            int NC = win.mChildWindows.size();
            while (NC > 0) {
                NC--;
                WindowState cw = win.mChildWindows.get(NC);
                int cpos = windows.indexOf(cw);
                if (cpos >= 0) {
                    if (cpos < interestingPos) {
                        interestingPos--;
                    }
                    windows.remove(cpos);
                }
            }
        }
        return interestingPos;
    }

    private void reAddWindowToListInOrderLocked(WindowState win) {
        addWindowToListInOrderLocked(win, false);
        WindowList windows = win.getWindowList();
        int wpos = windows.indexOf(win);
        if (wpos >= 0) {
            windows.remove(wpos);
            this.mWindowsChanged = true;
            reAddWindowLocked(wpos, win);
        }
    }

    void logWindowList(WindowList windows, String prefix) {
        int N = windows.size();
        while (N > 0) {
            N--;
            Slog.v(TAG, prefix + "#" + N + ": " + windows.get(N));
        }
    }

    void moveInputMethodDialogsLocked(int pos) {
        WindowState wp;
        ArrayList<WindowState> dialogs = this.mInputMethodDialogs;
        WindowList windows = getDefaultWindowListLocked();
        int N = dialogs.size();
        for (int i = 0; i < N; i++) {
            pos = tmpRemoveWindowLocked(pos, dialogs.get(i));
        }
        if (pos >= 0) {
            AppWindowToken targetAppToken = this.mInputMethodTarget.mAppToken;
            if (this.mInputMethodWindow != null) {
                while (pos < windows.size() && ((wp = windows.get(pos)) == this.mInputMethodWindow || wp.mAttachedWindow == this.mInputMethodWindow)) {
                    pos++;
                }
            }
            for (int i2 = 0; i2 < N; i2++) {
                WindowState win = dialogs.get(i2);
                win.mTargetAppToken = targetAppToken;
                pos = reAddWindowLocked(pos, win);
            }
            return;
        }
        for (int i3 = 0; i3 < N; i3++) {
            WindowState win2 = dialogs.get(i3);
            win2.mTargetAppToken = null;
            reAddWindowToListInOrderLocked(win2);
        }
    }

    boolean moveInputMethodWindowsIfNeededLocked(boolean needAssignLayers) {
        WindowState imWin = this.mInputMethodWindow;
        int DN = this.mInputMethodDialogs.size();
        if (imWin == null && DN == 0) {
            return false;
        }
        WindowList windows = getDefaultWindowListLocked();
        int imPos = findDesiredInputMethodWindowIndexLocked(true);
        if (imPos >= 0) {
            int N = windows.size();
            WindowState firstImWin = imPos < N ? windows.get(imPos) : null;
            WindowState baseImWin = imWin != null ? imWin : this.mInputMethodDialogs.get(0);
            if (baseImWin.mChildWindows.size() > 0) {
                WindowState cw = baseImWin.mChildWindows.get(0);
                if (cw.mSubLayer < 0) {
                    baseImWin = cw;
                }
            }
            if (firstImWin == baseImWin) {
                int pos = imPos + 1;
                while (pos < N && windows.get(pos).mIsImWindow) {
                    pos++;
                }
                int pos2 = pos + 1;
                while (pos2 < N && !windows.get(pos2).mIsImWindow) {
                    pos2++;
                }
                if (pos2 >= N) {
                    if (imWin != null) {
                        imWin.mTargetAppToken = this.mInputMethodTarget.mAppToken;
                    }
                    return false;
                }
            }
            if (imWin != null) {
                int imPos2 = tmpRemoveWindowLocked(imPos, imWin);
                imWin.mTargetAppToken = this.mInputMethodTarget.mAppToken;
                reAddWindowLocked(imPos2, imWin);
                if (DN > 0) {
                    moveInputMethodDialogsLocked(imPos2 + 1);
                }
            } else {
                moveInputMethodDialogsLocked(imPos);
            }
        } else if (imWin != null) {
            tmpRemoveWindowLocked(0, imWin);
            imWin.mTargetAppToken = null;
            reAddWindowToListInOrderLocked(imWin);
            if (DN > 0) {
                moveInputMethodDialogsLocked(-1);
            }
        } else {
            moveInputMethodDialogsLocked(-1);
        }
        if (needAssignLayers) {
            assignLayersLocked(windows);
        }
        return true;
    }

    final boolean isWallpaperVisible(WindowState wallpaperTarget) {
        return ((wallpaperTarget == null || (wallpaperTarget.mObscured && (wallpaperTarget.mAppToken == null || wallpaperTarget.mAppToken.mAppAnimator.animation == null))) && this.mUpperWallpaperTarget == null && this.mLowerWallpaperTarget == null) ? false : true;
    }

    int adjustWallpaperWindowsLocked() {
        WindowState foundW;
        int oldI;
        this.mInnerFields.mWallpaperMayChange = false;
        boolean targetChanged = false;
        DisplayInfo displayInfo = getDefaultDisplayContentLocked().getDisplayInfo();
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        WindowList windows = getDefaultWindowListLocked();
        int N = windows.size();
        WindowState w = null;
        WindowState foundW2 = null;
        int foundI = 0;
        WindowState topCurW = null;
        int topCurI = 0;
        int windowDetachedI = -1;
        int i = N;
        while (i > 0) {
            i--;
            WindowState w2 = windows.get(i);
            w = w2;
            if (w.mAttrs.type == 2013) {
                if (topCurW == null) {
                    topCurW = w;
                    topCurI = i;
                }
            } else {
                topCurW = null;
                if (w == this.mAnimator.mWindowDetachedWallpaper || w.mAppToken == null || !w.mAppToken.hidden || w.mAppToken.mAppAnimator.animation != null) {
                    boolean hasWallpaper = (w.mAttrs.flags & 1048576) != 0 || (w.mAppToken != null && w.mWinAnimator.mKeyguardGoingAwayAnimation);
                    if (hasWallpaper && w.isOnScreen() && (this.mWallpaperTarget == w || w.isDrawFinishedLw())) {
                        foundW2 = w;
                        foundI = i;
                        if (w != this.mWallpaperTarget || !w.mWinAnimator.isAnimating()) {
                            break;
                        }
                    } else if (w == this.mAnimator.mWindowDetachedWallpaper) {
                        windowDetachedI = i;
                    }
                }
            }
        }
        if (foundW2 == null && windowDetachedI >= 0) {
            foundW2 = w;
            foundI = windowDetachedI;
        }
        if (this.mWallpaperTarget != foundW2 && (this.mLowerWallpaperTarget == null || this.mLowerWallpaperTarget != foundW2)) {
            this.mLowerWallpaperTarget = null;
            this.mUpperWallpaperTarget = null;
            WindowState oldW = this.mWallpaperTarget;
            this.mWallpaperTarget = foundW2;
            targetChanged = true;
            if (foundW2 != null && oldW != null) {
                boolean oldAnim = oldW.isAnimatingLw();
                boolean foundAnim = foundW2.isAnimatingLw();
                if (foundAnim && oldAnim && (oldI = windows.indexOf(oldW)) >= 0) {
                    if (foundW2.mAppToken != null && foundW2.mAppToken.hiddenRequested) {
                        this.mWallpaperTarget = oldW;
                        foundW2 = oldW;
                        foundI = oldI;
                    } else if (foundI > oldI) {
                        this.mUpperWallpaperTarget = foundW2;
                        this.mLowerWallpaperTarget = oldW;
                        foundW2 = oldW;
                        foundI = oldI;
                    } else {
                        this.mUpperWallpaperTarget = oldW;
                        this.mLowerWallpaperTarget = foundW2;
                    }
                }
            }
        } else if (this.mLowerWallpaperTarget != null && (!this.mLowerWallpaperTarget.isAnimatingLw() || !this.mUpperWallpaperTarget.isAnimatingLw())) {
            this.mLowerWallpaperTarget = null;
            this.mUpperWallpaperTarget = null;
            this.mWallpaperTarget = foundW2;
            targetChanged = true;
        }
        boolean visible = foundW2 != null;
        if (visible) {
            visible = isWallpaperVisible(foundW2);
            this.mWallpaperAnimLayerAdjustment = (this.mLowerWallpaperTarget != null || foundW2.mAppToken == null) ? 0 : foundW2.mAppToken.mAppAnimator.animLayerAdjustment;
            int maxLayer = (this.mPolicy.getMaxWallpaperLayer() * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 1000;
            while (foundI > 0) {
                WindowState wb = windows.get(foundI - 1);
                if (wb.mBaseLayer < maxLayer && wb.mAttachedWindow != foundW2 && ((foundW2.mAttachedWindow == null || wb.mAttachedWindow != foundW2.mAttachedWindow) && (wb.mAttrs.type != 3 || foundW2.mToken == null || wb.mToken != foundW2.mToken))) {
                    break;
                }
                foundW2 = wb;
                foundI--;
            }
        }
        if (foundW2 == null && topCurW != null) {
            foundW = topCurW;
            foundI = topCurI + 1;
        } else {
            foundW = foundI > 0 ? windows.get(foundI - 1) : null;
        }
        if (visible) {
            if (this.mWallpaperTarget.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = this.mWallpaperTarget.mWallpaperX;
                this.mLastWallpaperXStep = this.mWallpaperTarget.mWallpaperXStep;
            }
            if (this.mWallpaperTarget.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = this.mWallpaperTarget.mWallpaperY;
                this.mLastWallpaperYStep = this.mWallpaperTarget.mWallpaperYStep;
            }
            if (this.mWallpaperTarget.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = this.mWallpaperTarget.mWallpaperDisplayOffsetX;
            }
            if (this.mWallpaperTarget.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = this.mWallpaperTarget.mWallpaperDisplayOffsetY;
            }
        }
        int changed = 0;
        int curTokenIndex = this.mWallpaperTokens.size();
        while (curTokenIndex > 0) {
            curTokenIndex--;
            WindowToken token = this.mWallpaperTokens.get(curTokenIndex);
            if (token.hidden == visible) {
                changed |= 4;
                token.hidden = !visible;
                getDefaultDisplayContentLocked().layoutNeeded = true;
            }
            int curWallpaperIndex = token.windows.size();
            while (curWallpaperIndex > 0) {
                curWallpaperIndex--;
                WindowState wallpaper = token.windows.get(curWallpaperIndex);
                if (visible) {
                    updateWallpaperOffsetLocked(wallpaper, dw, dh, false);
                }
                dispatchWallpaperVisibility(wallpaper, visible);
                wallpaper.mWinAnimator.mAnimLayer = wallpaper.mLayer + this.mWallpaperAnimLayerAdjustment;
                if (wallpaper == foundW) {
                    foundI--;
                    foundW = foundI > 0 ? windows.get(foundI - 1) : null;
                } else {
                    int oldIndex = windows.indexOf(wallpaper);
                    if (oldIndex >= 0) {
                        windows.remove(oldIndex);
                        this.mWindowsChanged = true;
                        if (oldIndex < foundI) {
                            foundI--;
                        }
                    }
                    int insertionIndex = 0;
                    if (visible && foundW != null) {
                        int type = foundW.mAttrs.type;
                        int privateFlags = foundW.mAttrs.privateFlags;
                        if ((privateFlags & 1024) != 0 || type == 2029) {
                            insertionIndex = windows.indexOf(foundW);
                        }
                    }
                    windows.add(insertionIndex, wallpaper);
                    this.mWindowsChanged = true;
                    changed |= 2;
                }
            }
        }
        if (targetChanged) {
        }
        return changed;
    }

    void setWallpaperAnimLayerAdjustmentLocked(int adj) {
        this.mWallpaperAnimLayerAdjustment = adj;
        int curTokenIndex = this.mWallpaperTokens.size();
        while (curTokenIndex > 0) {
            curTokenIndex--;
            WindowToken token = this.mWallpaperTokens.get(curTokenIndex);
            int curWallpaperIndex = token.windows.size();
            while (curWallpaperIndex > 0) {
                curWallpaperIndex--;
                WindowState wallpaper = token.windows.get(curWallpaperIndex);
                wallpaper.mWinAnimator.mAnimLayer = wallpaper.mLayer + adj;
            }
        }
    }

    boolean updateWallpaperOffsetLocked(WindowState wallpaperWin, int dw, int dh, boolean sync) {
        boolean rawChanged = false;
        float wpx = this.mLastWallpaperX >= 0.0f ? this.mLastWallpaperX : 0.5f;
        float wpxs = this.mLastWallpaperXStep >= 0.0f ? this.mLastWallpaperXStep : -1.0f;
        int availw = (wallpaperWin.mFrame.right - wallpaperWin.mFrame.left) - dw;
        int offset = availw > 0 ? -((int) ((availw * wpx) + 0.5f)) : 0;
        if (this.mLastWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
            offset += this.mLastWallpaperDisplayOffsetX;
        }
        boolean changed = wallpaperWin.mXOffset != offset;
        if (changed) {
            wallpaperWin.mXOffset = offset;
        }
        if (wallpaperWin.mWallpaperX != wpx || wallpaperWin.mWallpaperXStep != wpxs) {
            wallpaperWin.mWallpaperX = wpx;
            wallpaperWin.mWallpaperXStep = wpxs;
            rawChanged = true;
        }
        float wpy = this.mLastWallpaperY >= 0.0f ? this.mLastWallpaperY : 0.5f;
        float wpys = this.mLastWallpaperYStep >= 0.0f ? this.mLastWallpaperYStep : -1.0f;
        int availh = (wallpaperWin.mFrame.bottom - wallpaperWin.mFrame.top) - dh;
        int offset2 = availh > 0 ? -((int) ((availh * wpy) + 0.5f)) : 0;
        if (this.mLastWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            offset2 += this.mLastWallpaperDisplayOffsetY;
        }
        if (wallpaperWin.mYOffset != offset2) {
            changed = true;
            wallpaperWin.mYOffset = offset2;
        }
        if (wallpaperWin.mWallpaperY != wpy || wallpaperWin.mWallpaperYStep != wpys) {
            wallpaperWin.mWallpaperY = wpy;
            wallpaperWin.mWallpaperYStep = wpys;
            rawChanged = true;
        }
        if (rawChanged && (wallpaperWin.mAttrs.privateFlags & 4) != 0) {
            if (sync) {
                try {
                    this.mWaitingOnWallpaper = wallpaperWin;
                } catch (RemoteException e) {
                }
            }
            wallpaperWin.mClient.dispatchWallpaperOffsets(wallpaperWin.mWallpaperX, wallpaperWin.mWallpaperY, wallpaperWin.mWallpaperXStep, wallpaperWin.mWallpaperYStep, sync);
            if (sync && this.mWaitingOnWallpaper != null) {
                long start = SystemClock.uptimeMillis();
                if (this.mLastWallpaperTimeoutTime + WALLPAPER_TIMEOUT_RECOVERY < start) {
                    try {
                        this.mWindowMap.wait(WALLPAPER_TIMEOUT);
                    } catch (InterruptedException e2) {
                    }
                    if (WALLPAPER_TIMEOUT + start < SystemClock.uptimeMillis()) {
                        Slog.i(TAG, "Timeout waiting for wallpaper to offset: " + wallpaperWin);
                        this.mLastWallpaperTimeoutTime = start;
                    }
                }
                this.mWaitingOnWallpaper = null;
            }
        }
        return changed;
    }

    void wallpaperOffsetsComplete(IBinder window) {
        synchronized (this.mWindowMap) {
            if (this.mWaitingOnWallpaper != null && this.mWaitingOnWallpaper.mClient.asBinder() == window) {
                this.mWaitingOnWallpaper = null;
                this.mWindowMap.notifyAll();
            }
        }
    }

    void updateWallpaperOffsetLocked(WindowState changingTarget, boolean sync) {
        DisplayContent displayContent = changingTarget.getDisplayContent();
        if (displayContent != null) {
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            int dw = displayInfo.logicalWidth;
            int dh = displayInfo.logicalHeight;
            WindowState target = this.mWallpaperTarget;
            if (target != null) {
                if (target.mWallpaperX >= 0.0f) {
                    this.mLastWallpaperX = target.mWallpaperX;
                } else if (changingTarget.mWallpaperX >= 0.0f) {
                    this.mLastWallpaperX = changingTarget.mWallpaperX;
                }
                if (target.mWallpaperY >= 0.0f) {
                    this.mLastWallpaperY = target.mWallpaperY;
                } else if (changingTarget.mWallpaperY >= 0.0f) {
                    this.mLastWallpaperY = changingTarget.mWallpaperY;
                }
                if (target.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                    this.mLastWallpaperDisplayOffsetX = target.mWallpaperDisplayOffsetX;
                } else if (changingTarget.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                    this.mLastWallpaperDisplayOffsetX = changingTarget.mWallpaperDisplayOffsetX;
                }
                if (target.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                    this.mLastWallpaperDisplayOffsetY = target.mWallpaperDisplayOffsetY;
                } else if (changingTarget.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                    this.mLastWallpaperDisplayOffsetY = changingTarget.mWallpaperDisplayOffsetY;
                }
            }
            int curTokenIndex = this.mWallpaperTokens.size();
            while (curTokenIndex > 0) {
                curTokenIndex--;
                WindowToken token = this.mWallpaperTokens.get(curTokenIndex);
                int curWallpaperIndex = token.windows.size();
                while (curWallpaperIndex > 0) {
                    curWallpaperIndex--;
                    WindowState wallpaper = token.windows.get(curWallpaperIndex);
                    if (updateWallpaperOffsetLocked(wallpaper, dw, dh, sync)) {
                        WindowStateAnimator winAnimator = wallpaper.mWinAnimator;
                        winAnimator.computeShownFrameLocked();
                        winAnimator.setWallpaperOffset(wallpaper.mShownFrame);
                        sync = false;
                    }
                }
            }
        }
    }

    void dispatchWallpaperVisibility(WindowState wallpaper, boolean visible) {
        if (wallpaper.mWallpaperVisible != visible) {
            wallpaper.mWallpaperVisible = visible;
            try {
                wallpaper.mClient.dispatchAppVisibility(visible);
            } catch (RemoteException e) {
            }
        }
    }

    void updateWallpaperVisibilityLocked() {
        boolean visible = isWallpaperVisible(this.mWallpaperTarget);
        DisplayContent displayContent = this.mWallpaperTarget.getDisplayContent();
        if (displayContent != null) {
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            int dw = displayInfo.logicalWidth;
            int dh = displayInfo.logicalHeight;
            int curTokenIndex = this.mWallpaperTokens.size();
            while (curTokenIndex > 0) {
                curTokenIndex--;
                WindowToken token = this.mWallpaperTokens.get(curTokenIndex);
                if (token.hidden == visible) {
                    token.hidden = !visible;
                    getDefaultDisplayContentLocked().layoutNeeded = true;
                }
                int curWallpaperIndex = token.windows.size();
                while (curWallpaperIndex > 0) {
                    curWallpaperIndex--;
                    WindowState wallpaper = token.windows.get(curWallpaperIndex);
                    if (visible) {
                        updateWallpaperOffsetLocked(wallpaper, dw, dh, false);
                    }
                    dispatchWallpaperVisibility(wallpaper, visible);
                }
            }
        }
    }

    public int addWindow(Session session, IWindow client, int seq, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets, InputChannel outInputChannel) {
        int[] appOp = new int[1];
        int res = this.mPolicy.checkAddPermission(attrs, appOp);
        if (res != 0) {
            return res;
        }
        boolean reportNewConfig = false;
        WindowState attachedWindow = null;
        int type = attrs.type;
        synchronized (this.mWindowMap) {
            try {
                if (!this.mDisplayReady) {
                    throw new IllegalStateException("Display has not been initialialized");
                }
                DisplayContent displayContent = getDisplayContentLocked(displayId);
                if (displayContent == null) {
                    Slog.w(TAG, "Attempted to add window to a display that does not exist: " + displayId + ".  Aborting.");
                    return -9;
                }
                if (!displayContent.hasAccess(session.mUid)) {
                    Slog.w(TAG, "Attempted to add window to a display for which the application does not have access: " + displayId + ".  Aborting.");
                    return -9;
                }
                if (this.mWindowMap.containsKey(client.asBinder())) {
                    Slog.w(TAG, "Window " + client + " is already added");
                    return -5;
                }
                if (type >= 1000 && type <= 1999) {
                    attachedWindow = windowForClientLocked((Session) null, attrs.token, false);
                    if (attachedWindow == null) {
                        Slog.w(TAG, "Attempted to add window with token that is not a window: " + attrs.token + ".  Aborting.");
                        return -2;
                    }
                    if (attachedWindow.mAttrs.type >= 1000 && attachedWindow.mAttrs.type <= 1999) {
                        Slog.w(TAG, "Attempted to add window with token that is a sub-window: " + attrs.token + ".  Aborting.");
                        return -2;
                    }
                }
                if (type == 2030 && !displayContent.isPrivate()) {
                    Slog.w(TAG, "Attempted to add private presentation window to a non-private display.  Aborting.");
                    return -8;
                }
                boolean addToken = false;
                WindowToken token = this.mTokenMap.get(attrs.token);
                if (token == null) {
                    if (type >= 1 && type <= 99) {
                        Slog.w(TAG, "Attempted to add application window with unknown token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                    if (type == 2011) {
                        Slog.w(TAG, "Attempted to add input method window with unknown token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                    if (type == 2031) {
                        Slog.w(TAG, "Attempted to add voice interaction window with unknown token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                    if (type == 2013) {
                        Slog.w(TAG, "Attempted to add wallpaper window with unknown token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                    if (type == 2023) {
                        Slog.w(TAG, "Attempted to add Dream window with unknown token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                    if (type == 2032) {
                        Slog.w(TAG, "Attempted to add Accessibility overlay window with unknown token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                    token = new WindowToken(this, attrs.token, -1, false);
                    addToken = true;
                } else if (type >= 1 && type <= 99) {
                    AppWindowToken atoken = token.appWindowToken;
                    if (atoken == null) {
                        Slog.w(TAG, "Attempted to add window with non-application token " + token + ".  Aborting.");
                        return -3;
                    }
                    if (atoken.removed) {
                        Slog.w(TAG, "Attempted to add window with exiting application token " + token + ".  Aborting.");
                        return -4;
                    }
                    if (type == 3 && atoken.firstWindowDrawn) {
                        return -6;
                    }
                } else if (type == 2011) {
                    if (token.windowType != 2011) {
                        Slog.w(TAG, "Attempted to add input method window with bad token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                } else if (type == 2031) {
                    if (token.windowType != 2031) {
                        Slog.w(TAG, "Attempted to add voice interaction window with bad token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                } else if (type == 2013) {
                    if (token.windowType != 2013) {
                        Slog.w(TAG, "Attempted to add wallpaper window with bad token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                } else if (type == 2023) {
                    if (token.windowType != 2023) {
                        Slog.w(TAG, "Attempted to add Dream window with bad token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                } else if (type == 2032) {
                    if (token.windowType != 2032) {
                        Slog.w(TAG, "Attempted to add Accessibility overlay window with bad token " + attrs.token + ".  Aborting.");
                        return -1;
                    }
                } else if (token.appWindowToken != null) {
                    Slog.w(TAG, "Non-null appWindowToken for system window of type=" + type);
                    attrs.token = null;
                    token = new WindowToken(this, null, -1, false);
                    addToken = true;
                }
                WindowState win = new WindowState(this, session, client, token, attachedWindow, appOp[0], seq, attrs, viewVisibility, displayContent);
                if (win.mDeathRecipient == null) {
                    Slog.w(TAG, "Adding window client " + client.asBinder() + " that is dead, aborting.");
                    return -4;
                }
                if (win.getDisplayContent() == null) {
                    Slog.w(TAG, "Adding window to Display that has been removed.");
                    return -9;
                }
                this.mPolicy.adjustWindowParamsLw(win.mAttrs);
                win.setShowToOwnerOnlyLocked(this.mPolicy.checkShowToOwnerOnly(attrs));
                int res2 = this.mPolicy.prepareAddWindowLw(win, attrs);
                if (res2 != 0) {
                    return res2;
                }
                if (outInputChannel != null && (attrs.inputFeatures & 2) == 0) {
                    String name = win.makeInputChannelName();
                    InputChannel[] inputChannels = InputChannel.openInputChannelPair(name);
                    win.setInputChannel(inputChannels[0]);
                    inputChannels[1].transferTo(outInputChannel);
                    this.mInputManager.registerInputChannel(win.mInputChannel, win.mInputWindowHandle);
                }
                int res3 = 0;
                long origId = Binder.clearCallingIdentity();
                if (addToken) {
                    this.mTokenMap.put(attrs.token, token);
                }
                win.attach();
                this.mWindowMap.put(client.asBinder(), win);
                if (win.mAppOp != -1 && this.mAppOps.startOpNoThrow(win.mAppOp, win.getOwningUid(), win.getOwningPackage()) != 0) {
                    win.setAppOpVisibilityLw(false);
                }
                boolean hideSystemAlertWindows = !this.mHidingNonSystemOverlayWindows.isEmpty();
                win.setForceHideNonSystemOverlayWindowIfNeeded(hideSystemAlertWindows);
                if (type == 3 && token.appWindowToken != null) {
                    token.appWindowToken.startingWindow = win;
                }
                boolean imMayMove = true;
                if (type == 2011) {
                    win.mGivenInsetsPending = true;
                    this.mInputMethodWindow = win;
                    addInputMethodWindowToListLocked(win);
                    imMayMove = false;
                } else if (type == 2012) {
                    this.mInputMethodDialogs.add(win);
                    addWindowToListInOrderLocked(win, true);
                    moveInputMethodDialogsLocked(findDesiredInputMethodWindowIndexLocked(true));
                    imMayMove = false;
                } else {
                    addWindowToListInOrderLocked(win, true);
                    if (type == 2013) {
                        this.mLastWallpaperTimeoutTime = 0L;
                        displayContent.pendingLayoutChanges |= 4;
                    } else if ((attrs.flags & 1048576) != 0) {
                        displayContent.pendingLayoutChanges |= 4;
                    } else if (this.mWallpaperTarget != null && this.mWallpaperTarget.mLayer >= win.mBaseLayer) {
                        displayContent.pendingLayoutChanges |= 4;
                    }
                }
                WindowStateAnimator winAnimator = win.mWinAnimator;
                winAnimator.mEnterAnimationPending = true;
                winAnimator.mEnteringAnimation = true;
                if (displayContent.isDefaultDisplay) {
                    this.mPolicy.getInsetHintLw(win.mAttrs, outContentInsets, outStableInsets);
                } else {
                    outContentInsets.setEmpty();
                    outStableInsets.setEmpty();
                }
                if (this.mInTouchMode) {
                    res3 = 0 | 1;
                }
                if (win.mAppToken == null || !win.mAppToken.clientHidden) {
                    res3 |= 2;
                }
                this.mInputMonitor.setUpdateInputWindowsNeededLw();
                boolean focusChanged = false;
                if (win.canReceiveKeys() && (focusChanged = updateFocusedWindowLocked(1, false))) {
                    imMayMove = false;
                }
                if (imMayMove) {
                    moveInputMethodWindowsIfNeededLocked(false);
                }
                assignLayersLocked(displayContent.getWindowList());
                if (focusChanged) {
                    this.mInputMonitor.setInputFocusLw(this.mCurrentFocus, false);
                }
                this.mInputMonitor.updateInputWindowsLw(false);
                if (win.isVisibleOrAdding() && updateOrientationFromAppTokensLocked(false)) {
                    reportNewConfig = true;
                }
                if (reportNewConfig) {
                    sendNewConfiguration();
                }
                Binder.restoreCallingIdentity(origId);
                return res3;
            }
        }
    }

    boolean isScreenCaptureDisabledLocked(int userId) {
        Boolean disabled = this.mScreenCaptureDisabled.get(userId);
        if (disabled == null) {
            return false;
        }
        return disabled.booleanValue();
    }

    public void setScreenCaptureDisabled(int userId, boolean disabled) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException("Only system can call setScreenCaptureDisabled.");
        }
        synchronized (this.mWindowMap) {
            this.mScreenCaptureDisabled.put(userId, Boolean.valueOf(disabled));
        }
    }

    public void removeWindow(Session session, IWindow client) {
        synchronized (this.mWindowMap) {
            WindowState win = windowForClientLocked(session, client, false);
            if (win != null) {
                removeWindowLocked(session, win);
            }
        }
    }

    public void removeWindowLocked(Session session, WindowState win) {
        if (win.mAttrs.type == 3) {
        }
        long origId = Binder.clearCallingIdentity();
        win.disposeInputChannel();
        boolean wasVisible = false;
        if (win.mHasSurface && okToDisplay()) {
            wasVisible = win.isWinVisibleLw();
            if (wasVisible) {
                int transit = 2;
                if (win.mAttrs.type == 3) {
                    transit = 5;
                }
                if (win.mWinAnimator.applyAnimationLocked(transit, false)) {
                    win.mExiting = true;
                }
                if (this.mAccessibilityController != null && win.getDisplayId() == 0) {
                    this.mAccessibilityController.onWindowTransitionLocked(win, transit);
                }
            }
            if (win.mExiting || win.mWinAnimator.isAnimating()) {
                win.mExiting = true;
                win.mRemoveOnExit = true;
                DisplayContent displayContent = win.getDisplayContent();
                if (displayContent != null) {
                    displayContent.layoutNeeded = true;
                }
                boolean focusChanged = updateFocusedWindowLocked(3, false);
                performLayoutAndPlaceSurfacesLocked();
                if (win.mAppToken != null) {
                    win.mAppToken.updateReportedVisibilityLocked();
                }
                if (focusChanged) {
                    this.mInputMonitor.updateInputWindowsLw(false);
                }
                Binder.restoreCallingIdentity(origId);
                return;
            }
        }
        removeWindowInnerLocked(session, win);
        if (wasVisible && updateOrientationFromAppTokensLocked(false)) {
            this.mH.sendEmptyMessage(18);
        }
        updateFocusedWindowLocked(0, true);
        Binder.restoreCallingIdentity(origId);
    }

    void removeWindowInnerLocked(Session session, WindowState win) {
        if (!win.mRemoved) {
            for (int i = win.mChildWindows.size() - 1; i >= 0; i--) {
                WindowState cwin = win.mChildWindows.get(i);
                Slog.w(TAG, "Force-removing child win " + cwin + " from container " + win);
                removeWindowInnerLocked(cwin.mSession, cwin);
            }
            win.mRemoved = true;
            if (this.mInputMethodTarget == win) {
                moveInputMethodWindowsIfNeededLocked(false);
            }
            this.mPolicy.removeWindowLw(win);
            win.removeLocked();
            this.mWindowMap.remove(win.mClient.asBinder());
            if (win.mAppOp != -1) {
                this.mAppOps.finishOp(win.mAppOp, win.getOwningUid(), win.getOwningPackage());
            }
            this.mPendingRemove.remove(win);
            this.mResizingWindows.remove(win);
            updateNonSystemOverlayWindowsVisibilityIfNeeded(win, false);
            this.mWindowsChanged = true;
            if (this.mInputMethodWindow == win) {
                this.mInputMethodWindow = null;
            } else if (win.mAttrs.type == 2012) {
                this.mInputMethodDialogs.remove(win);
            }
            WindowToken token = win.mToken;
            AppWindowToken atoken = win.mAppToken;
            token.windows.remove(win);
            if (atoken != null) {
                atoken.allAppWindows.remove(win);
            }
            if (token.windows.size() == 0) {
                if (!token.explicit) {
                    this.mTokenMap.remove(token.token);
                } else if (atoken != null) {
                    atoken.firstWindowDrawn = false;
                }
            }
            if (atoken != null) {
                if (atoken.startingWindow == win) {
                    scheduleRemoveStartingWindowLocked(atoken);
                } else if (atoken.allAppWindows.size() == 0 && atoken.startingData != null) {
                    atoken.startingData = null;
                } else if (atoken.allAppWindows.size() == 1 && atoken.startingView != null) {
                    scheduleRemoveStartingWindowLocked(atoken);
                }
            }
            if (win.mAttrs.type == 2013) {
                this.mLastWallpaperTimeoutTime = 0L;
                getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
            } else if ((win.mAttrs.flags & 1048576) != 0) {
                getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
            }
            WindowList windows = win.getWindowList();
            if (windows != null) {
                windows.remove(win);
                if (!this.mInLayout) {
                    assignLayersLocked(windows);
                    DisplayContent displayContent = win.getDisplayContent();
                    if (displayContent != null) {
                        displayContent.layoutNeeded = true;
                    }
                    performLayoutAndPlaceSurfacesLocked();
                    if (win.mAppToken != null) {
                        win.mAppToken.updateReportedVisibilityLocked();
                    }
                }
            }
            this.mInputMonitor.updateInputWindowsLw(true);
        }
    }

    public void updateAppOpsState() {
        synchronized (this.mWindowMap) {
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                int numWindows = windows.size();
                for (int winNdx = 0; winNdx < numWindows; winNdx++) {
                    WindowState win = windows.get(winNdx);
                    if (win.mAppOp != -1) {
                        int mode = this.mAppOps.checkOpNoThrow(win.mAppOp, win.getOwningUid(), win.getOwningPackage());
                        win.setAppOpVisibilityLw(mode == 0);
                    }
                }
            }
        }
    }

    static void logSurface(WindowState w, String msg, RuntimeException where) {
        String str = "  SURFACE " + msg + ": " + w;
        if (where != null) {
            Slog.i(TAG, str, where);
        } else {
            Slog.i(TAG, str);
        }
    }

    static void logSurface(SurfaceControl s, String title, String msg, RuntimeException where) {
        String str = "  SURFACE " + s + ": " + msg + " / " + title;
        if (where != null) {
            Slog.i(TAG, str, where);
        } else {
            Slog.i(TAG, str);
        }
    }

    void setTransparentRegionWindow(Session session, IWindow client, Region region) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState w = windowForClientLocked(session, client, false);
                if (w != null && w.mHasSurface) {
                    w.mWinAnimator.setTransparentRegionHintLocked(region);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void setInsetsWindow(Session session, IWindow client, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableRegion) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState w = windowForClientLocked(session, client, false);
                if (w != null) {
                    w.mGivenInsetsPending = false;
                    w.mGivenContentInsets.set(contentInsets);
                    w.mGivenVisibleInsets.set(visibleInsets);
                    w.mGivenTouchableRegion.set(touchableRegion);
                    w.mTouchableInsets = touchableInsets;
                    if (w.mGlobalScale != 1.0f) {
                        w.mGivenContentInsets.scale(w.mGlobalScale);
                        w.mGivenVisibleInsets.scale(w.mGlobalScale);
                        w.mGivenTouchableRegion.scale(w.mGlobalScale);
                    }
                    DisplayContent displayContent = w.getDisplayContent();
                    if (displayContent != null) {
                        displayContent.layoutNeeded = true;
                    }
                    performLayoutAndPlaceSurfacesLocked();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void getWindowDisplayFrame(Session session, IWindow client, Rect outDisplayFrame) {
        synchronized (this.mWindowMap) {
            WindowState win = windowForClientLocked(session, client, false);
            if (win == null) {
                outDisplayFrame.setEmpty();
            } else {
                outDisplayFrame.set(win.mDisplayFrame);
            }
        }
    }

    public void setWindowWallpaperPositionLocked(WindowState window, float x, float y, float xStep, float yStep) {
        if (window.mWallpaperX != x || window.mWallpaperY != y) {
            window.mWallpaperX = x;
            window.mWallpaperY = y;
            window.mWallpaperXStep = xStep;
            window.mWallpaperYStep = yStep;
            updateWallpaperOffsetLocked(window, true);
        }
    }

    void wallpaperCommandComplete(IBinder window, Bundle result) {
        synchronized (this.mWindowMap) {
            if (this.mWaitingOnWallpaper != null && this.mWaitingOnWallpaper.mClient.asBinder() == window) {
                this.mWaitingOnWallpaper = null;
                this.mWindowMap.notifyAll();
            }
        }
    }

    public void setWindowWallpaperDisplayOffsetLocked(WindowState window, int x, int y) {
        if (window.mWallpaperDisplayOffsetX != x || window.mWallpaperDisplayOffsetY != y) {
            window.mWallpaperDisplayOffsetX = x;
            window.mWallpaperDisplayOffsetY = y;
            updateWallpaperOffsetLocked(window, true);
        }
    }

    public Bundle sendWindowWallpaperCommandLocked(WindowState window, String action, int x, int y, int z, Bundle extras, boolean sync) {
        if (window == this.mWallpaperTarget || window == this.mLowerWallpaperTarget || window == this.mUpperWallpaperTarget) {
            int curTokenIndex = this.mWallpaperTokens.size();
            while (curTokenIndex > 0) {
                curTokenIndex--;
                WindowToken token = this.mWallpaperTokens.get(curTokenIndex);
                int curWallpaperIndex = token.windows.size();
                while (curWallpaperIndex > 0) {
                    curWallpaperIndex--;
                    WindowState wallpaper = token.windows.get(curWallpaperIndex);
                    try {
                        wallpaper.mClient.dispatchWallpaperCommand(action, x, y, z, extras, sync);
                        sync = false;
                    } catch (RemoteException e) {
                    }
                }
            }
            if (sync) {
            }
            return null;
        }
        return null;
    }

    public void setUniverseTransformLocked(WindowState window, float alpha, float offx, float offy, float dsdx, float dtdx, float dsdy, float dtdy) {
        Transformation transform = window.mWinAnimator.mUniverseTransform;
        transform.setAlpha(alpha);
        Matrix matrix = transform.getMatrix();
        matrix.getValues(this.mTmpFloats);
        this.mTmpFloats[2] = offx;
        this.mTmpFloats[5] = offy;
        this.mTmpFloats[0] = dsdx;
        this.mTmpFloats[3] = dtdx;
        this.mTmpFloats[1] = dsdy;
        this.mTmpFloats[4] = dtdy;
        matrix.setValues(this.mTmpFloats);
        DisplayContent displayContent = window.getDisplayContent();
        if (displayContent != null) {
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            RectF dispRect = new RectF(0.0f, 0.0f, displayInfo.logicalWidth, displayInfo.logicalHeight);
            matrix.mapRect(dispRect);
            window.mGivenTouchableRegion.set(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
            window.mGivenTouchableRegion.op((int) dispRect.left, (int) dispRect.top, (int) dispRect.right, (int) dispRect.bottom, Region.Op.DIFFERENCE);
            window.mTouchableInsets = 3;
            displayContent.layoutNeeded = true;
            performLayoutAndPlaceSurfacesLocked();
        }
    }

    public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) {
        WindowState window;
        synchronized (this.mWindowMap) {
            if (this.mAccessibilityController != null && (window = this.mWindowMap.get(token)) != null && window.getDisplayId() == 0) {
                this.mAccessibilityController.onRectangleOnScreenRequestedLocked(rectangle);
            }
        }
    }

    public IWindowId getWindowId(IBinder token) {
        IWindowId iWindowId;
        synchronized (this.mWindowMap) {
            WindowState window = this.mWindowMap.get(token);
            iWindowId = window != null ? window.mWindowId : null;
        }
        return iWindowId;
    }

    public int relayoutWindow(Session session, IWindow client, int seq, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, int flags, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Configuration outConfig, Surface outSurface) {
        boolean toBeDisplayed = false;
        boolean surfaceChanged = false;
        boolean hasStatusBarPermission = this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0;
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mWindowMap) {
            WindowState win = windowForClientLocked(session, client, false);
            if (win == null) {
                return 0;
            }
            WindowStateAnimator winAnimator = win.mWinAnimator;
            if (viewVisibility != 8 && (win.mRequestedWidth != requestedWidth || win.mRequestedHeight != requestedHeight)) {
                win.mLayoutNeeded = true;
                win.mRequestedWidth = requestedWidth;
                win.mRequestedHeight = requestedHeight;
            }
            if (attrs != null) {
                this.mPolicy.adjustWindowParamsLw(attrs);
            }
            int systemUiVisibility = 0;
            if (attrs != null) {
                systemUiVisibility = attrs.systemUiVisibility | attrs.subtreeSystemUiVisibility;
                if ((67043328 & systemUiVisibility) != 0 && !hasStatusBarPermission) {
                    systemUiVisibility &= -67043329;
                }
            }
            if (attrs != null && seq == win.mSeq) {
                win.mSystemUiVisibility = systemUiVisibility;
            }
            winAnimator.mSurfaceDestroyDeferred = (flags & 2) != 0;
            int attrChanges = 0;
            int flagChanges = 0;
            if (attrs != null) {
                if (win.mAttrs.type != attrs.type) {
                    throw new IllegalArgumentException("Window type can not be changed after the window is added.");
                }
                WindowManager.LayoutParams layoutParams = win.mAttrs;
                flagChanges = layoutParams.flags ^ attrs.flags;
                layoutParams.flags = flagChanges;
                attrChanges = win.mAttrs.copyFrom(attrs);
                if ((attrChanges & 16385) != 0) {
                    win.mLayoutNeeded = true;
                }
            }
            win.mEnforceSizeCompat = (win.mAttrs.privateFlags & 128) != 0;
            if ((attrChanges & 128) != 0) {
                winAnimator.mAlpha = attrs.alpha;
            }
            boolean scaledWindow = (win.mAttrs.flags & 16384) != 0;
            if (scaledWindow) {
                win.mHScale = attrs.width != requestedWidth ? attrs.width / requestedWidth : 1.0f;
                win.mVScale = attrs.height != requestedHeight ? attrs.height / requestedHeight : 1.0f;
            } else {
                win.mVScale = 1.0f;
                win.mHScale = 1.0f;
            }
            boolean imMayMove = (131080 & flagChanges) != 0;
            boolean isDefaultDisplay = win.isDefaultDisplay();
            boolean focusMayChange = isDefaultDisplay && !(win.mViewVisibility == viewVisibility && (flagChanges & 8) == 0 && win.mRelayoutCalled);
            boolean wallpaperMayMove = (win.mViewVisibility == viewVisibility || (win.mAttrs.flags & 1048576) == 0) ? false : true;
            boolean wallpaperMayMove2 = wallpaperMayMove | ((1048576 & flagChanges) != 0);
            win.mRelayoutCalled = true;
            int oldVisibility = win.mViewVisibility;
            win.mViewVisibility = viewVisibility;
            if (viewVisibility == 0 && (win.mAppToken == null || !win.mAppToken.clientHidden)) {
                toBeDisplayed = !win.isVisibleLw();
                if (win.mExiting) {
                    winAnimator.cancelExitAnimationForNextAnimationLocked();
                    win.mExiting = false;
                }
                if (win.mDestroying) {
                    win.mDestroying = false;
                    this.mDestroySurface.remove(win);
                }
                if (oldVisibility == 8) {
                    winAnimator.mEnterAnimationPending = true;
                }
                winAnimator.mEnteringAnimation = true;
                if (toBeDisplayed) {
                    if (win.isDrawnLw() && okToDisplay()) {
                        winAnimator.applyEnterAnimationLocked();
                    }
                    if ((win.mAttrs.flags & 2097152) != 0) {
                        win.mTurnOnScreen = true;
                    }
                    if (win.isConfigChanged()) {
                        outConfig.setTo(this.mCurConfiguration);
                    }
                }
                if ((attrChanges & 8) != 0) {
                    winAnimator.destroySurfaceLocked();
                    toBeDisplayed = true;
                    surfaceChanged = true;
                }
                try {
                    if (!win.mHasSurface) {
                        surfaceChanged = true;
                    }
                    SurfaceControl surfaceControl = winAnimator.createSurfaceLocked();
                    if (surfaceControl != null) {
                        outSurface.copyFrom(surfaceControl);
                    } else {
                        outSurface.release();
                    }
                    if (toBeDisplayed) {
                        focusMayChange = isDefaultDisplay;
                    }
                    if (win.mAttrs.type == 2011 && this.mInputMethodWindow == null) {
                        this.mInputMethodWindow = win;
                        imMayMove = true;
                    }
                    if (win.mAttrs.type == 1 && win.mAppToken != null && win.mAppToken.startingWindow != null) {
                        WindowManager.LayoutParams sa = win.mAppToken.startingWindow.mAttrs;
                        sa.flags = (sa.flags & (-4718594)) | (win.mAttrs.flags & 4718593);
                    }
                } catch (Exception e) {
                    this.mInputMonitor.updateInputWindowsLw(true);
                    Slog.w(TAG, "Exception thrown when creating surface for client " + client + " (" + ((Object) win.mAttrs.getTitle()) + ")", e);
                    Binder.restoreCallingIdentity(origId);
                    return 0;
                }
            } else {
                winAnimator.mEnterAnimationPending = false;
                winAnimator.mEnteringAnimation = false;
                if (winAnimator.mSurfaceControl != null && !win.mExiting) {
                    surfaceChanged = true;
                    int transit = 2;
                    if (win.mAttrs.type == 3) {
                        transit = 5;
                    }
                    if (win.isWinVisibleLw() && winAnimator.applyAnimationLocked(transit, false)) {
                        focusMayChange = isDefaultDisplay;
                        win.mExiting = true;
                    } else if (win.mWinAnimator.isAnimating()) {
                        win.mExiting = true;
                    } else if (win == this.mWallpaperTarget) {
                        win.mExiting = true;
                        win.mWinAnimator.mAnimating = true;
                    } else {
                        if (this.mInputMethodWindow == win) {
                            this.mInputMethodWindow = null;
                        }
                        winAnimator.destroySurfaceLocked();
                    }
                    if (this.mAccessibilityController != null && win.getDisplayId() == 0) {
                        this.mAccessibilityController.onWindowTransitionLocked(win, transit);
                    }
                }
                outSurface.release();
            }
            if (focusMayChange && updateFocusedWindowLocked(3, false)) {
                imMayMove = false;
            }
            if (imMayMove && (moveInputMethodWindowsIfNeededLocked(false) || toBeDisplayed)) {
                assignLayersLocked(win.getWindowList());
            }
            if (wallpaperMayMove2) {
                getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
            }
            DisplayContent displayContent = win.getDisplayContent();
            if (displayContent != null) {
                displayContent.layoutNeeded = true;
            }
            win.mGivenInsetsPending = (flags & 1) != 0;
            boolean configChanged = updateOrientationFromAppTokensLocked(false);
            performLayoutAndPlaceSurfacesLocked();
            if (toBeDisplayed && win.mIsWallpaper) {
                DisplayInfo displayInfo = getDefaultDisplayInfoLocked();
                updateWallpaperOffsetLocked(win, displayInfo.logicalWidth, displayInfo.logicalHeight, false);
            }
            if (win.mAppToken != null) {
                win.mAppToken.updateReportedVisibilityLocked();
            }
            outFrame.set(win.mCompatFrame);
            outOverscanInsets.set(win.mOverscanInsets);
            outContentInsets.set(win.mContentInsets);
            outVisibleInsets.set(win.mVisibleInsets);
            outStableInsets.set(win.mStableInsets);
            boolean inTouchMode = this.mInTouchMode;
            boolean animating = this.mAnimator.mAnimating && win.mWinAnimator.isAnimating();
            if (animating && !this.mRelayoutWhileAnimating.contains(win)) {
                this.mRelayoutWhileAnimating.add(win);
            }
            this.mInputMonitor.updateInputWindowsLw(true);
            if (configChanged) {
                sendNewConfiguration();
            }
            Binder.restoreCallingIdentity(origId);
            return (animating ? 8 : 0) | (inTouchMode ? 1 : 0) | (toBeDisplayed ? 2 : 0) | (surfaceChanged ? 4 : 0);
        }
    }

    public void performDeferredDestroyWindow(Session session, IWindow client) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState win = windowForClientLocked(session, client, false);
                if (win != null) {
                    win.mWinAnimator.destroyDeferredSurfaceLocked();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean outOfMemoryWindow(Session session, IWindow client) {
        boolean zReclaimSomeSurfaceMemoryLocked = false;
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState win = windowForClientLocked(session, client, false);
                if (win != null) {
                    zReclaimSomeSurfaceMemoryLocked = reclaimSomeSurfaceMemoryLocked(win.mWinAnimator, "from-client", false);
                }
            }
            return zReclaimSomeSurfaceMemoryLocked;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void finishDrawingWindow(Session session, IWindow client) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState win = windowForClientLocked(session, client, false);
                if (win != null && win.mWinAnimator.finishDrawingLocked()) {
                    if ((win.mAttrs.flags & 1048576) != 0) {
                        getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
                    }
                    DisplayContent displayContent = win.getDisplayContent();
                    if (displayContent != null) {
                        displayContent.layoutNeeded = true;
                    }
                    requestTraversalLocked();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private boolean applyAnimationLocked(AppWindowToken atoken, WindowManager.LayoutParams lp, int transit, boolean enter, boolean isVoiceInteraction) {
        if (okToDisplay()) {
            DisplayInfo displayInfo = getDefaultDisplayInfoLocked();
            int width = displayInfo.appWidth;
            int height = displayInfo.appHeight;
            WindowState win = atoken.findMainWindow();
            Rect containingFrame = new Rect(0, 0, width, height);
            Rect contentInsets = new Rect();
            boolean isFullScreen = true;
            if (win != null) {
                if (win.mContainingFrame != null) {
                    containingFrame.set(win.mContainingFrame);
                }
                if (win.mContentInsets != null) {
                    contentInsets.set(win.mContentInsets);
                }
                isFullScreen = (win.mSystemUiVisibility & SYSTEM_UI_FLAGS_LAYOUT_STABLE_FULLSCREEN) == SYSTEM_UI_FLAGS_LAYOUT_STABLE_FULLSCREEN || (win.mAttrs.flags & SoundTriggerHelper.STATUS_ERROR) != 0;
            }
            if (atoken.mLaunchTaskBehind) {
                enter = false;
            }
            Animation a = this.mAppTransition.loadAnimation(lp, transit, enter, width, height, this.mCurConfiguration.orientation, containingFrame, contentInsets, isFullScreen, isVoiceInteraction);
            if (a != null) {
                atoken.mAppAnimator.setAnimation(a, width, height);
            }
        } else {
            atoken.mAppAnimator.clearAnimation();
        }
        return atoken.mAppAnimator.animation != null;
    }

    public void validateAppTokens(int stackId, List<TaskGroup> tasks) {
        synchronized (this.mWindowMap) {
            int t = tasks.size() - 1;
            if (t < 0) {
                Slog.w(TAG, "validateAppTokens: empty task list");
                return;
            }
            TaskGroup task = tasks.get(0);
            int taskId = task.taskId;
            Task targetTask = this.mTaskIdToTask.get(taskId);
            DisplayContent displayContent = targetTask.getDisplayContent();
            if (displayContent == null) {
                Slog.w(TAG, "validateAppTokens: no Display for taskId=" + taskId);
                return;
            }
            ArrayList<Task> localTasks = this.mStackIdToStack.get(stackId).getTasks();
            int taskNdx = localTasks.size() - 1;
            while (taskNdx >= 0 && t >= 0) {
                AppTokenList localTokens = localTasks.get(taskNdx).mAppTokens;
                TaskGroup task2 = tasks.get(t);
                TaskGroup task3 = task2;
                List<IApplicationToken> tokens = task3.tokens;
                DisplayContent lastDisplayContent = displayContent;
                displayContent = this.mTaskIdToTask.get(taskId).getDisplayContent();
                if (displayContent != lastDisplayContent) {
                    Slog.w(TAG, "validateAppTokens: displayContent changed in TaskGroup list!");
                    return;
                }
                int tokenNdx = localTokens.size() - 1;
                int v = task3.tokens.size() - 1;
                while (tokenNdx >= 0 && v >= 0) {
                    AppWindowToken atoken = localTokens.get(tokenNdx);
                    if (atoken.removed) {
                        tokenNdx--;
                    } else {
                        if (tokens.get(v) != atoken.token) {
                            break;
                        }
                        tokenNdx--;
                        v--;
                    }
                }
                if (tokenNdx >= 0 || v >= 0) {
                    break;
                }
                taskNdx--;
                t--;
            }
            if (taskNdx >= 0 || t >= 0) {
                Slog.w(TAG, "validateAppTokens: Mismatch! ActivityManager=" + tasks);
                Slog.w(TAG, "validateAppTokens: Mismatch! WindowManager=" + localTasks);
                Slog.w(TAG, "validateAppTokens: Mismatch! Callers=" + Debug.getCallers(4));
            }
        }
    }

    public void validateStackOrder(Integer[] remoteStackIds) {
    }

    boolean checkCallingPermission(String permission, String func) {
        if (Binder.getCallingPid() == Process.myPid() || this.mContext.checkCallingPermission(permission) == 0) {
            return true;
        }
        String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + permission;
        Slog.w(TAG, msg);
        return false;
    }

    boolean okToDisplay() {
        return !this.mDisplayFrozen && this.mDisplayEnabled && this.mPolicy.isScreenOn();
    }

    AppWindowToken findAppWindowToken(IBinder token) {
        WindowToken wtoken = this.mTokenMap.get(token);
        if (wtoken == null) {
            return null;
        }
        return wtoken.appWindowToken;
    }

    public void addWindowToken(IBinder token, int type) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "addWindowToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (this.mTokenMap.get(token) != null) {
                Slog.w(TAG, "Attempted to add existing input method token: " + token);
                return;
            }
            WindowToken wtoken = new WindowToken(this, token, type, true);
            this.mTokenMap.put(token, wtoken);
            if (type == 2013) {
                this.mWallpaperTokens.add(wtoken);
            }
        }
    }

    public void removeWindowToken(IBinder token) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "removeWindowToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = null;
            WindowToken wtoken = this.mTokenMap.remove(token);
            if (wtoken != null) {
                boolean delayed = false;
                if (!wtoken.hidden) {
                    int N = wtoken.windows.size();
                    boolean changed = false;
                    for (int i = 0; i < N; i++) {
                        WindowState win = wtoken.windows.get(i);
                        displayContent = win.getDisplayContent();
                        if (win.mWinAnimator.isAnimating()) {
                            delayed = true;
                        }
                        if (win.isVisibleNow()) {
                            win.mWinAnimator.applyAnimationLocked(2, false);
                            if (this.mAccessibilityController != null && win.isDefaultDisplay()) {
                                this.mAccessibilityController.onWindowTransitionLocked(win, 2);
                            }
                            changed = true;
                            if (displayContent != null) {
                                displayContent.layoutNeeded = true;
                            }
                        }
                    }
                    wtoken.hidden = true;
                    if (changed) {
                        performLayoutAndPlaceSurfacesLocked();
                        updateFocusedWindowLocked(0, false);
                    }
                    if (delayed) {
                        if (displayContent != null) {
                            displayContent.mExitingTokens.add(wtoken);
                        }
                    } else if (wtoken.windowType == 2013) {
                        this.mWallpaperTokens.remove(wtoken);
                    }
                }
                this.mInputMonitor.updateInputWindowsLw(true);
            } else {
                Slog.w(TAG, "Attempted to remove non-existing token: " + token);
            }
        }
        Binder.restoreCallingIdentity(origId);
    }

    private Task createTask(int taskId, int stackId, int userId, AppWindowToken atoken) {
        TaskStack stack = this.mStackIdToStack.get(stackId);
        if (stack == null) {
            throw new IllegalArgumentException("addAppToken: invalid stackId=" + stackId);
        }
        EventLog.writeEvent(EventLogTags.WM_TASK_CREATED, Integer.valueOf(taskId), Integer.valueOf(stackId));
        Task task = new Task(atoken, stack, userId);
        this.mTaskIdToTask.put(taskId, task);
        stack.addTask(task, atoken.mLaunchTaskBehind ? false : true);
        return task;
    }

    public void addAppToken(int addPos, IApplicationToken token, int taskId, int stackId, int requestedOrientation, boolean fullscreen, boolean showWhenLocked, int userId, int configChanges, boolean voiceInteraction, boolean launchTaskBehind) {
        long inputDispatchingTimeoutNanos;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "addAppToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        try {
            inputDispatchingTimeoutNanos = token.getKeyDispatchingTimeout() * 1000000;
        } catch (RemoteException ex) {
            Slog.w(TAG, "Could not get dispatching timeout.", ex);
            inputDispatchingTimeoutNanos = DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS;
        }
        synchronized (this.mWindowMap) {
            if (findAppWindowToken(token.asBinder()) != null) {
                Slog.w(TAG, "Attempted to add existing app token: " + token);
                return;
            }
            AppWindowToken atoken = new AppWindowToken(this, token, voiceInteraction);
            atoken.inputDispatchingTimeoutNanos = inputDispatchingTimeoutNanos;
            atoken.groupId = taskId;
            atoken.appFullscreen = fullscreen;
            atoken.showWhenLocked = showWhenLocked;
            atoken.requestedOrientation = requestedOrientation;
            atoken.layoutConfigChanges = (configChanges & 1152) != 0;
            atoken.mLaunchTaskBehind = launchTaskBehind;
            Slog.v(TAG, "addAppToken: " + atoken + " to stack=" + stackId + " task=" + taskId + " at " + addPos);
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                createTask(taskId, stackId, userId, atoken);
            } else {
                task.addAppToken(addPos, atoken);
            }
            this.mTokenMap.put(token.asBinder(), atoken);
            atoken.hidden = true;
            atoken.hiddenRequested = true;
        }
    }

    public void setAppGroupId(IBinder token, int groupId) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppGroupId()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken atoken = findAppWindowToken(token);
            if (atoken == null) {
                Slog.w(TAG, "Attempted to set group id of non-existing app token: " + token);
                return;
            }
            Task oldTask = this.mTaskIdToTask.get(atoken.groupId);
            oldTask.removeAppToken(atoken);
            atoken.groupId = groupId;
            Task newTask = this.mTaskIdToTask.get(groupId);
            if (newTask == null) {
                createTask(groupId, oldTask.mStack.mStackId, oldTask.mUserId, atoken);
            } else {
                newTask.mAppTokens.add(atoken);
            }
        }
    }

    public int getOrientationFromWindowsLocked() {
        int req;
        if (this.mDisplayFrozen || this.mOpeningApps.size() > 0 || this.mClosingApps.size() > 0) {
            return this.mLastWindowForcedOrientation;
        }
        WindowList windows = getDefaultWindowListLocked();
        int pos = windows.size() - 1;
        while (pos >= 0) {
            WindowState win = windows.get(pos);
            pos--;
            if (win.mAppToken != null) {
                this.mLastWindowForcedOrientation = -1;
                return -1;
            }
            if (win.isVisibleLw() && win.mPolicyVisibilityAfterAnim && (req = win.mAttrs.screenOrientation) != -1 && req != 3) {
                this.mLastWindowForcedOrientation = req;
                return req;
            }
        }
        this.mLastWindowForcedOrientation = -1;
        return -1;
    }

    public int getOrientationFromAppTokensLocked() {
        int lastOrientation = -1;
        boolean findingBehind = false;
        boolean lastFullscreen = false;
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        ArrayList<Task> tasks = displayContent.getTasks();
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
            int firstToken = tokens.size() - 1;
            for (int tokenNdx = firstToken; tokenNdx >= 0; tokenNdx--) {
                AppWindowToken atoken = tokens.get(tokenNdx);
                if (findingBehind || atoken.hidden || !atoken.hiddenRequested) {
                    if (tokenNdx == firstToken && lastOrientation != 3 && lastFullscreen) {
                        return lastOrientation;
                    }
                    if (!atoken.hiddenRequested && !atoken.willBeHidden) {
                        if (tokenNdx == 0) {
                            lastOrientation = atoken.requestedOrientation;
                        }
                        int or = atoken.requestedOrientation;
                        lastFullscreen = atoken.appFullscreen;
                        if (!lastFullscreen || or == 3) {
                            if (or == -1 || or == 3) {
                                findingBehind |= or == 3;
                            } else {
                                return or;
                            }
                        } else {
                            return or;
                        }
                    }
                }
            }
        }
        return -1;
    }

    public Configuration updateOrientationFromAppTokens(Configuration currentConfig, IBinder freezeThisOneIfNeeded) {
        Configuration config;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "updateOrientationFromAppTokens()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        long ident = Binder.clearCallingIdentity();
        synchronized (this.mWindowMap) {
            config = updateOrientationFromAppTokensLocked(currentConfig, freezeThisOneIfNeeded);
        }
        Binder.restoreCallingIdentity(ident);
        return config;
    }

    private Configuration updateOrientationFromAppTokensLocked(Configuration currentConfig, IBinder freezeThisOneIfNeeded) {
        AppWindowToken atoken;
        if (updateOrientationFromAppTokensLocked(false)) {
            if (freezeThisOneIfNeeded != null && (atoken = findAppWindowToken(freezeThisOneIfNeeded)) != null) {
                startAppFreezingScreenLocked(atoken);
            }
            Configuration config = computeNewConfigurationLocked();
            return config;
        }
        if (currentConfig == null) {
            return null;
        }
        this.mTempConfiguration.setToDefaults();
        this.mTempConfiguration.fontScale = currentConfig.fontScale;
        if (!computeScreenConfigurationLocked(this.mTempConfiguration) || currentConfig.diff(this.mTempConfiguration) == 0) {
            return null;
        }
        this.mWaitingForConfig = true;
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        displayContent.layoutNeeded = true;
        int[] anim = new int[2];
        if (displayContent.isDimming()) {
            anim[1] = 0;
            anim[0] = 0;
        } else {
            this.mPolicy.selectRotationAnimationLw(anim);
        }
        startFreezingDisplayLocked(false, anim[0], anim[1]);
        Configuration config2 = new Configuration(this.mTempConfiguration);
        return config2;
    }

    boolean updateOrientationFromAppTokensLocked(boolean inTransaction) {
        long ident = Binder.clearCallingIdentity();
        try {
            int req = getOrientationFromWindowsLocked();
            if (req == -1) {
                req = getOrientationFromAppTokensLocked();
            }
            if (req != this.mForcedAppOrientation) {
                this.mForcedAppOrientation = req;
                this.mPolicy.setCurrentOrientationLw(req);
                if (updateRotationUncheckedLocked(inTransaction)) {
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setNewConfiguration(Configuration config) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setNewConfiguration()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            this.mCurConfiguration = new Configuration(config);
            if (this.mWaitingForConfig) {
                this.mWaitingForConfig = false;
                this.mLastFinishedFreezeSource = "new-config";
            }
            performLayoutAndPlaceSurfacesLocked();
        }
    }

    public void setAppOrientation(IApplicationToken token, int requestedOrientation) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppOrientation()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken atoken = findAppWindowToken(token.asBinder());
            if (atoken == null) {
                Slog.w(TAG, "Attempted to set orientation of non-existing app token: " + token);
            } else {
                atoken.requestedOrientation = requestedOrientation;
            }
        }
    }

    public int getAppOrientation(IApplicationToken token) {
        int i;
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = findAppWindowToken(token.asBinder());
            i = wtoken == null ? -1 : wtoken.requestedOrientation;
        }
        return i;
    }

    void setFocusedStackLayer() {
        this.mFocusedStackLayer = 0;
        if (this.mFocusedApp != null) {
            WindowList windows = this.mFocusedApp.allAppWindows;
            for (int i = windows.size() - 1; i >= 0; i--) {
                WindowState win = windows.get(i);
                int animLayer = win.mWinAnimator.mAnimLayer;
                if (win.mAttachedWindow == null && win.isVisibleLw() && animLayer > this.mFocusedStackLayer) {
                    this.mFocusedStackLayer = animLayer + 1;
                }
            }
        }
        this.mFocusedStackFrame.setLayer(this.mFocusedStackLayer);
    }

    void setFocusedStackFrame() {
        TaskStack stack;
        if (this.mFocusedApp != null) {
            Task task = this.mTaskIdToTask.get(this.mFocusedApp.groupId);
            stack = task.mStack;
            DisplayContent displayContent = task.getDisplayContent();
            if (displayContent != null) {
                displayContent.setTouchExcludeRegion(stack);
            }
        } else {
            stack = null;
        }
        SurfaceControl.openTransaction();
        try {
            if (stack == null) {
                this.mFocusedStackFrame.setVisibility(false);
            } else {
                this.mFocusedStackFrame.setBounds(stack);
                boolean multipleStacks = stack.isFullscreen() ? false : true;
                this.mFocusedStackFrame.setVisibility(multipleStacks);
            }
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    public void setFocusedApp(IBinder token, boolean moveFocusNow) {
        AppWindowToken newFocus;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setFocusedApp()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (token == null) {
                newFocus = null;
            } else {
                newFocus = findAppWindowToken(token);
                if (newFocus == null) {
                    Slog.w(TAG, "Attempted to set focus to non-existing app token: " + token);
                }
            }
            boolean changed = this.mFocusedApp != newFocus;
            if (changed) {
                this.mFocusedApp = newFocus;
                this.mInputMonitor.setFocusedAppLw(newFocus);
            }
            if (moveFocusNow && changed) {
                long origId = Binder.clearCallingIdentity();
                updateFocusedWindowLocked(0, true);
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public void prepareAppTransition(int transit, boolean alwaysKeepCurrent) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "prepareAppTransition()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (!this.mAppTransition.isTransitionSet() || this.mAppTransition.isTransitionNone()) {
                this.mAppTransition.setAppTransition(transit);
            } else if (!alwaysKeepCurrent) {
                if (transit == 8 && this.mAppTransition.isTransitionEqual(9)) {
                    this.mAppTransition.setAppTransition(transit);
                } else if (transit == 6 && this.mAppTransition.isTransitionEqual(7)) {
                    this.mAppTransition.setAppTransition(transit);
                }
            }
            if (okToDisplay()) {
                this.mAppTransition.prepare();
                this.mStartingIconInTransition = false;
                this.mSkipAppTransitionAnimation = false;
                this.mH.removeMessages(13);
                this.mH.sendEmptyMessageDelayed(13, 5000L);
            }
        }
    }

    public int getPendingAppTransition() {
        return this.mAppTransition.getAppTransition();
    }

    public void overridePendingAppTransition(String packageName, int enterAnim, int exitAnim, IRemoteCallback startedCallback) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overridePendingAppTransition(packageName, enterAnim, exitAnim, startedCallback);
        }
    }

    public void overridePendingAppTransitionScaleUp(int startX, int startY, int startWidth, int startHeight) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overridePendingAppTransitionScaleUp(startX, startY, startWidth, startHeight);
        }
    }

    public void overridePendingAppTransitionThumb(Bitmap srcThumb, int startX, int startY, IRemoteCallback startedCallback, boolean scaleUp) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overridePendingAppTransitionThumb(srcThumb, startX, startY, startedCallback, scaleUp);
        }
    }

    public void overridePendingAppTransitionAspectScaledThumb(Bitmap srcThumb, int startX, int startY, int targetWidth, int targetHeight, IRemoteCallback startedCallback, boolean scaleUp) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overridePendingAppTransitionAspectScaledThumb(srcThumb, startX, startY, targetWidth, targetHeight, startedCallback, scaleUp);
        }
    }

    public void overridePendingAppTransitionInPlace(String packageName, int anim) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overrideInPlaceAppTransition(packageName, anim);
        }
    }

    public void executeAppTransition() {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "executeAppTransition()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (this.mAppTransition.isTransitionSet()) {
                this.mAppTransition.setReady();
                long origId = Binder.clearCallingIdentity();
                try {
                    performLayoutAndPlaceSurfacesLocked();
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }
    }

    public void setAppStartingWindow(IBinder token, String pkg, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, IBinder transferFrom, boolean createIfNeeded) {
        AppWindowToken ttoken;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppStartingWindow()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = findAppWindowToken(token);
            if (wtoken == null) {
                Slog.w(TAG, "Attempted to set icon of non-existing app token: " + token);
                return;
            }
            if (okToDisplay()) {
                if (wtoken.startingData == null) {
                    if (transferFrom != null && (ttoken = findAppWindowToken(transferFrom)) != null) {
                        WindowState startingWindow = ttoken.startingWindow;
                        if (startingWindow != null) {
                            if (this.mStartingIconInTransition) {
                                this.mSkipAppTransitionAnimation = true;
                            }
                            long origId = Binder.clearCallingIdentity();
                            wtoken.startingData = ttoken.startingData;
                            wtoken.startingView = ttoken.startingView;
                            wtoken.startingDisplayed = ttoken.startingDisplayed;
                            ttoken.startingDisplayed = false;
                            wtoken.startingWindow = startingWindow;
                            wtoken.reportedVisible = ttoken.reportedVisible;
                            ttoken.startingData = null;
                            ttoken.startingView = null;
                            ttoken.startingWindow = null;
                            ttoken.startingMoved = true;
                            startingWindow.mToken = wtoken;
                            startingWindow.mRootToken = wtoken;
                            startingWindow.mAppToken = wtoken;
                            startingWindow.mWinAnimator.mAppAnimator = wtoken.mAppAnimator;
                            startingWindow.getWindowList().remove(startingWindow);
                            this.mWindowsChanged = true;
                            ttoken.windows.remove(startingWindow);
                            ttoken.allAppWindows.remove(startingWindow);
                            addWindowToListInOrderLocked(startingWindow, true);
                            if (ttoken.allDrawn) {
                                wtoken.allDrawn = true;
                                wtoken.deferClearAllDrawn = ttoken.deferClearAllDrawn;
                            }
                            if (ttoken.firstWindowDrawn) {
                                wtoken.firstWindowDrawn = true;
                            }
                            if (!ttoken.hidden) {
                                wtoken.hidden = false;
                                wtoken.hiddenRequested = false;
                                wtoken.willBeHidden = false;
                            }
                            if (wtoken.clientHidden != ttoken.clientHidden) {
                                wtoken.clientHidden = ttoken.clientHidden;
                                wtoken.sendAppVisibilityToClients();
                            }
                            AppWindowAnimator tAppAnimator = ttoken.mAppAnimator;
                            AppWindowAnimator wAppAnimator = wtoken.mAppAnimator;
                            if (tAppAnimator.animation != null) {
                                wAppAnimator.animation = tAppAnimator.animation;
                                wAppAnimator.animating = tAppAnimator.animating;
                                wAppAnimator.animLayerAdjustment = tAppAnimator.animLayerAdjustment;
                                tAppAnimator.animation = null;
                                tAppAnimator.animLayerAdjustment = 0;
                                wAppAnimator.updateLayers();
                                tAppAnimator.updateLayers();
                            }
                            updateFocusedWindowLocked(3, true);
                            getDefaultDisplayContentLocked().layoutNeeded = true;
                            performLayoutAndPlaceSurfacesLocked();
                            Binder.restoreCallingIdentity(origId);
                            return;
                        }
                        if (ttoken.startingData != null) {
                            wtoken.startingData = ttoken.startingData;
                            ttoken.startingData = null;
                            ttoken.startingMoved = true;
                            Message m = this.mH.obtainMessage(5, wtoken);
                            this.mH.sendMessageAtFrontOfQueue(m);
                            return;
                        }
                        AppWindowAnimator tAppAnimator2 = ttoken.mAppAnimator;
                        AppWindowAnimator wAppAnimator2 = wtoken.mAppAnimator;
                        if (tAppAnimator2.thumbnail != null) {
                            if (wAppAnimator2.thumbnail != null) {
                                wAppAnimator2.thumbnail.destroy();
                            }
                            wAppAnimator2.thumbnail = tAppAnimator2.thumbnail;
                            wAppAnimator2.thumbnailX = tAppAnimator2.thumbnailX;
                            wAppAnimator2.thumbnailY = tAppAnimator2.thumbnailY;
                            wAppAnimator2.thumbnailLayer = tAppAnimator2.thumbnailLayer;
                            wAppAnimator2.thumbnailAnimation = tAppAnimator2.thumbnailAnimation;
                            tAppAnimator2.thumbnail = null;
                        }
                    }
                    if (createIfNeeded) {
                        if (theme != 0) {
                            AttributeCache.Entry ent = AttributeCache.instance().get(pkg, theme, com.android.internal.R.styleable.Window, this.mCurrentUserId);
                            if (ent != null) {
                                if (!ent.array.getBoolean(5, false)) {
                                    if (!ent.array.getBoolean(4, false)) {
                                        if (ent.array.getBoolean(14, false)) {
                                            if (this.mWallpaperTarget == null) {
                                                windowFlags |= 1048576;
                                            } else {
                                                return;
                                            }
                                        }
                                    } else {
                                        return;
                                    }
                                } else {
                                    return;
                                }
                            } else {
                                return;
                            }
                        }
                        this.mStartingIconInTransition = true;
                        wtoken.startingData = new StartingData(pkg, theme, compatInfo, nonLocalizedLabel, labelRes, icon, logo, windowFlags);
                        Message m2 = this.mH.obtainMessage(5, wtoken);
                        this.mH.sendMessageAtFrontOfQueue(m2);
                    }
                }
            }
        }
    }

    public void removeAppStartingWindow(IBinder token) {
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = this.mTokenMap.get(token).appWindowToken;
            if (wtoken.startingWindow != null) {
                scheduleRemoveStartingWindowLocked(wtoken);
            }
        }
    }

    public void setAppWillBeHidden(IBinder token) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppWillBeHidden()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = findAppWindowToken(token);
            if (wtoken == null) {
                Slog.w(TAG, "Attempted to set will be hidden of non-existing app token: " + token);
            } else {
                wtoken.willBeHidden = true;
            }
        }
    }

    public void setAppFullscreen(IBinder token, boolean toOpaque) {
        synchronized (this.mWindowMap) {
            AppWindowToken atoken = findAppWindowToken(token);
            if (atoken != null) {
                atoken.appFullscreen = toOpaque;
                setWindowOpaqueLocked(token, toOpaque);
                requestTraversalLocked();
            }
        }
    }

    public void setWindowOpaque(IBinder token, boolean isOpaque) {
        synchronized (this.mWindowMap) {
            setWindowOpaqueLocked(token, isOpaque);
        }
    }

    public void setWindowOpaqueLocked(IBinder token, boolean isOpaque) {
        WindowState win;
        AppWindowToken wtoken = findAppWindowToken(token);
        if (wtoken != null && (win = wtoken.findMainWindow()) != null) {
            win.mWinAnimator.setOpaqueLocked(isOpaque);
        }
    }

    boolean setTokenVisibilityLocked(AppWindowToken wtoken, WindowManager.LayoutParams lp, boolean visible, int transit, boolean performLayout, boolean isVoiceInteraction) {
        boolean delayed = false;
        if (wtoken.clientHidden == visible) {
            wtoken.clientHidden = !visible;
            wtoken.sendAppVisibilityToClients();
        }
        wtoken.willBeHidden = false;
        if (wtoken.hidden == visible) {
            boolean changed = false;
            boolean runningAppAnimation = false;
            if (transit != -1) {
                if (wtoken.mAppAnimator.animation == AppWindowAnimator.sDummyAnimation) {
                    wtoken.mAppAnimator.animation = null;
                }
                if (applyAnimationLocked(wtoken, lp, transit, visible, isVoiceInteraction)) {
                    runningAppAnimation = true;
                    delayed = true;
                }
                WindowState window = wtoken.findMainWindow();
                if (window != null && this.mAccessibilityController != null && window.getDisplayId() == 0) {
                    this.mAccessibilityController.onAppWindowTransitionLocked(window, transit);
                }
                changed = true;
            }
            int N = wtoken.allAppWindows.size();
            for (int i = 0; i < N; i++) {
                WindowState win = wtoken.allAppWindows.get(i);
                if (win != wtoken.startingWindow) {
                    if (visible) {
                        if (!win.isVisibleNow()) {
                            if (!runningAppAnimation) {
                                win.mWinAnimator.applyAnimationLocked(1, true);
                                if (this.mAccessibilityController != null && win.getDisplayId() == 0) {
                                    this.mAccessibilityController.onWindowTransitionLocked(win, 1);
                                }
                            }
                            changed = true;
                            DisplayContent displayContent = win.getDisplayContent();
                            if (displayContent != null) {
                                displayContent.layoutNeeded = true;
                            }
                        }
                    } else if (win.isVisibleNow()) {
                        if (!runningAppAnimation) {
                            win.mWinAnimator.applyAnimationLocked(2, false);
                            if (this.mAccessibilityController != null && win.getDisplayId() == 0) {
                                this.mAccessibilityController.onWindowTransitionLocked(win, 2);
                            }
                        }
                        changed = true;
                        DisplayContent displayContent2 = win.getDisplayContent();
                        if (displayContent2 != null) {
                            displayContent2.layoutNeeded = true;
                        }
                    }
                }
            }
            boolean z = !visible;
            wtoken.hiddenRequested = z;
            wtoken.hidden = z;
            if (!visible) {
                unsetAppFreezingScreenLocked(wtoken, true, true);
            } else {
                WindowState swin = wtoken.startingWindow;
                if (swin != null && !swin.isDrawnLw()) {
                    swin.mPolicyVisibility = false;
                    swin.mPolicyVisibilityAfterAnim = false;
                }
            }
            if (changed) {
                this.mInputMonitor.setUpdateInputWindowsNeededLw();
                if (performLayout) {
                    updateFocusedWindowLocked(3, false);
                    performLayoutAndPlaceSurfacesLocked();
                }
                this.mInputMonitor.updateInputWindowsLw(false);
            }
        }
        if (wtoken.mAppAnimator.animation != null) {
            delayed = true;
        }
        for (int i2 = wtoken.allAppWindows.size() - 1; i2 >= 0 && !delayed; i2--) {
            if (wtoken.allAppWindows.get(i2).mWinAnimator.isWindowAnimating()) {
                delayed = true;
            }
        }
        return delayed;
    }

    void updateTokenInPlaceLocked(AppWindowToken wtoken, int transit) {
        if (transit != -1) {
            if (wtoken.mAppAnimator.animation == AppWindowAnimator.sDummyAnimation) {
                wtoken.mAppAnimator.animation = null;
            }
            applyAnimationLocked(wtoken, null, transit, false, false);
        }
    }

    public void setAppVisibility(IBinder token, boolean visible) {
        WindowState win;
        AppWindowToken focusedToken;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppVisibility()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = findAppWindowToken(token);
            if (wtoken == null) {
                Slog.w(TAG, "Attempted to set visibility of non-existing app token: " + token);
                return;
            }
            if (okToDisplay() && this.mAppTransition.isTransitionSet()) {
                wtoken.hiddenRequested = visible ? false : true;
                if (!wtoken.startingDisplayed) {
                    wtoken.mAppAnimator.setDummyAnimation();
                }
                this.mOpeningApps.remove(wtoken);
                this.mClosingApps.remove(wtoken);
                wtoken.waitingToHide = false;
                wtoken.waitingToShow = false;
                wtoken.inPendingTransaction = true;
                if (visible) {
                    this.mOpeningApps.add(wtoken);
                    wtoken.startingMoved = false;
                    wtoken.mEnteringAnimation = true;
                    if (wtoken.hidden) {
                        wtoken.allDrawn = false;
                        wtoken.deferClearAllDrawn = false;
                        wtoken.waitingToShow = true;
                        if (wtoken.clientHidden) {
                            wtoken.clientHidden = false;
                            wtoken.sendAppVisibilityToClients();
                        }
                    }
                } else {
                    this.mClosingApps.add(wtoken);
                    wtoken.mEnteringAnimation = false;
                    if (!wtoken.hidden) {
                        wtoken.waitingToHide = true;
                    }
                }
                if (this.mAppTransition.getAppTransition() == 16 && (win = findFocusedWindowLocked(getDefaultDisplayContentLocked())) != null && (focusedToken = win.mAppToken) != null) {
                    focusedToken.hidden = true;
                    this.mOpeningApps.add(focusedToken);
                }
                return;
            }
            long origId = Binder.clearCallingIdentity();
            setTokenVisibilityLocked(wtoken, null, visible, -1, true, wtoken.voiceInteraction);
            wtoken.updateReportedVisibilityLocked();
            Binder.restoreCallingIdentity(origId);
        }
    }

    void unsetAppFreezingScreenLocked(AppWindowToken wtoken, boolean unfreezeSurfaceNow, boolean force) {
        if (wtoken.mAppAnimator.freezingScreen) {
            int N = wtoken.allAppWindows.size();
            boolean unfrozeWindows = false;
            for (int i = 0; i < N; i++) {
                WindowState w = wtoken.allAppWindows.get(i);
                if (w.mAppFreezing) {
                    w.mAppFreezing = false;
                    if (w.mHasSurface && !w.mOrientationChanging) {
                        w.mOrientationChanging = true;
                        this.mInnerFields.mOrientationChangeComplete = false;
                    }
                    w.mLastFreezeDuration = 0;
                    unfrozeWindows = true;
                    DisplayContent displayContent = w.getDisplayContent();
                    if (displayContent != null) {
                        displayContent.layoutNeeded = true;
                    }
                }
            }
            if (force || unfrozeWindows) {
                wtoken.mAppAnimator.freezingScreen = false;
                wtoken.mAppAnimator.lastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
                this.mAppsFreezingScreen--;
                this.mLastFinishedFreezeSource = wtoken;
            }
            if (unfreezeSurfaceNow) {
                if (unfrozeWindows) {
                    performLayoutAndPlaceSurfacesLocked();
                }
                stopFreezingDisplayLocked();
            }
        }
    }

    private void startAppFreezingScreenLocked(AppWindowToken wtoken) {
        if (!wtoken.hiddenRequested) {
            if (!wtoken.mAppAnimator.freezingScreen) {
                wtoken.mAppAnimator.freezingScreen = true;
                wtoken.mAppAnimator.lastFreezeDuration = 0;
                this.mAppsFreezingScreen++;
                if (this.mAppsFreezingScreen == 1) {
                    startFreezingDisplayLocked(false, 0, 0);
                    this.mH.removeMessages(17);
                    this.mH.sendEmptyMessageDelayed(17, 5000L);
                }
            }
            int N = wtoken.allAppWindows.size();
            for (int i = 0; i < N; i++) {
                WindowState w = wtoken.allAppWindows.get(i);
                w.mAppFreezing = true;
            }
        }
    }

    public void startAppFreezingScreen(IBinder token, int configChanges) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppFreezingScreen()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (configChanges == 0) {
                if (!okToDisplay()) {
                    AppWindowToken wtoken = findAppWindowToken(token);
                    if (wtoken == null || wtoken.appToken == null) {
                        Slog.w(TAG, "Attempted to freeze screen with non-existing app token: " + wtoken);
                    } else {
                        long origId = Binder.clearCallingIdentity();
                        startAppFreezingScreenLocked(wtoken);
                        Binder.restoreCallingIdentity(origId);
                    }
                }
            }
        }
    }

    public void stopAppFreezingScreen(IBinder token, boolean force) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppFreezingScreen()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = findAppWindowToken(token);
            if (wtoken != null && wtoken.appToken != null) {
                long origId = Binder.clearCallingIdentity();
                unsetAppFreezingScreenLocked(wtoken, true, force);
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    void removeAppFromTaskLocked(AppWindowToken wtoken) {
        wtoken.removeAllWindows();
        Task task = this.mTaskIdToTask.get(wtoken.groupId);
        if (task != null && !task.removeAppToken(wtoken)) {
            Slog.e(TAG, "removeAppFromTaskLocked: token=" + wtoken + " not found.");
        }
    }

    public void removeAppToken(IBinder token) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "removeAppToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        AppWindowToken wtoken = null;
        AppWindowToken startingToken = null;
        boolean delayed = false;
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mWindowMap) {
            WindowToken basewtoken = this.mTokenMap.remove(token);
            if (basewtoken != null && (wtoken = basewtoken.appWindowToken) != null) {
                delayed = setTokenVisibilityLocked(wtoken, null, false, -1, true, wtoken.voiceInteraction);
                wtoken.inPendingTransaction = false;
                this.mOpeningApps.remove(wtoken);
                wtoken.waitingToShow = false;
                if (this.mClosingApps.contains(wtoken)) {
                    delayed = true;
                } else if (this.mAppTransition.isTransitionSet()) {
                    this.mClosingApps.add(wtoken);
                    wtoken.waitingToHide = true;
                    delayed = true;
                }
                TaskStack stack = this.mTaskIdToTask.get(wtoken.groupId).mStack;
                if (delayed && !wtoken.allAppWindows.isEmpty()) {
                    stack.mExitingAppTokens.add(wtoken);
                    wtoken.mDeferRemoval = true;
                } else {
                    wtoken.mAppAnimator.clearAnimation();
                    wtoken.mAppAnimator.animating = false;
                    removeAppFromTaskLocked(wtoken);
                }
                wtoken.removed = true;
                if (wtoken.startingData != null) {
                    startingToken = wtoken;
                }
                unsetAppFreezingScreenLocked(wtoken, true, true);
                if (this.mFocusedApp == wtoken) {
                    this.mFocusedApp = null;
                    updateFocusedWindowLocked(0, true);
                    this.mInputMonitor.setFocusedAppLw(null);
                }
            } else {
                Slog.w(TAG, "Attempted to remove non-existing app token: " + token);
            }
            if (!delayed && wtoken != null) {
                wtoken.updateReportedVisibilityLocked();
            }
            scheduleRemoveStartingWindowLocked(startingToken);
        }
        Binder.restoreCallingIdentity(origId);
    }

    void scheduleRemoveStartingWindowLocked(AppWindowToken wtoken) {
        if (!this.mH.hasMessages(6, wtoken) && wtoken != null && wtoken.startingWindow != null) {
            Message m = this.mH.obtainMessage(6, wtoken);
            this.mH.sendMessage(m);
        }
    }

    private boolean tmpRemoveAppWindowsLocked(WindowToken token) {
        WindowList windows = token.windows;
        int NW = windows.size();
        if (NW > 0) {
            this.mWindowsChanged = true;
        }
        for (int i = 0; i < NW; i++) {
            WindowState win = windows.get(i);
            win.getWindowList().remove(win);
            int j = win.mChildWindows.size();
            while (j > 0) {
                j--;
                WindowState cwin = win.mChildWindows.get(j);
                cwin.getWindowList().remove(cwin);
            }
        }
        return NW > 0;
    }

    void dumpAppTokensLocked() {
        int numStacks = this.mStackIdToStack.size();
        for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
            TaskStack stack = this.mStackIdToStack.valueAt(stackNdx);
            Slog.v(TAG, "  Stack #" + stack.mStackId + " tasks from bottom to top:");
            ArrayList<Task> tasks = stack.getTasks();
            int numTasks = tasks.size();
            for (int taskNdx = 0; taskNdx < numTasks; taskNdx++) {
                Task task = tasks.get(taskNdx);
                Slog.v(TAG, "    Task #" + task.taskId + " activities from bottom to top:");
                AppTokenList tokens = task.mAppTokens;
                int numTokens = tokens.size();
                for (int tokenNdx = 0; tokenNdx < numTokens; tokenNdx++) {
                    Slog.v(TAG, "      activity #" + tokenNdx + ": " + tokens.get(tokenNdx).token);
                }
            }
        }
    }

    void dumpWindowsLocked() {
        int numDisplays = this.mDisplayContents.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
            Slog.v(TAG, " Display #" + displayContent.getDisplayId());
            WindowList windows = displayContent.getWindowList();
            for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                Slog.v(TAG, "  #" + winNdx + ": " + windows.get(winNdx));
            }
        }
    }

    private int findAppWindowInsertionPointLocked(AppWindowToken target) {
        int taskId = target.groupId;
        Task targetTask = this.mTaskIdToTask.get(taskId);
        if (targetTask == null) {
            Slog.w(TAG, "findAppWindowInsertionPointLocked: no Task for " + target + " taskId=" + taskId);
            return 0;
        }
        DisplayContent displayContent = targetTask.getDisplayContent();
        if (displayContent == null) {
            Slog.w(TAG, "findAppWindowInsertionPointLocked: no DisplayContent for " + target);
            return 0;
        }
        WindowList windows = displayContent.getWindowList();
        int NW = windows.size();
        boolean found = false;
        ArrayList<Task> tasks = displayContent.getTasks();
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            Task task = tasks.get(taskNdx);
            if (found || task.taskId == taskId) {
                AppTokenList tokens = task.mAppTokens;
                for (int tokenNdx = tokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                    AppWindowToken wtoken = tokens.get(tokenNdx);
                    if (!found && wtoken == target) {
                        found = true;
                    }
                    if (found && !wtoken.sendingToBottom) {
                        for (int i = wtoken.windows.size() - 1; i >= 0; i--) {
                            WindowState win = wtoken.windows.get(i);
                            for (int j = win.mChildWindows.size() - 1; j >= 0; j--) {
                                WindowState cwin = win.mChildWindows.get(j);
                                if (cwin.mSubLayer >= 0) {
                                    for (int pos = NW - 1; pos >= 0; pos--) {
                                        if (windows.get(pos) == cwin) {
                                            return pos + 1;
                                        }
                                    }
                                }
                            }
                            for (int pos2 = NW - 1; pos2 >= 0; pos2--) {
                                if (windows.get(pos2) == win) {
                                    return pos2 + 1;
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int pos3 = NW - 1; pos3 >= 0; pos3--) {
            if (windows.get(pos3).mIsWallpaper) {
                return pos3 + 1;
            }
        }
        return 0;
    }

    private final int reAddWindowLocked(int index, WindowState win) {
        WindowList windows = win.getWindowList();
        int NCW = win.mChildWindows.size();
        boolean added = false;
        for (int j = 0; j < NCW; j++) {
            WindowState cwin = win.mChildWindows.get(j);
            if (!added && cwin.mSubLayer >= 0) {
                win.mRebuilding = false;
                windows.add(index, win);
                index++;
                added = true;
            }
            cwin.mRebuilding = false;
            windows.add(index, cwin);
            index++;
        }
        if (!added) {
            win.mRebuilding = false;
            windows.add(index, win);
            index++;
        }
        this.mWindowsChanged = true;
        return index;
    }

    private final int reAddAppWindowsLocked(DisplayContent displayContent, int index, WindowToken token) {
        int NW = token.windows.size();
        for (int i = 0; i < NW; i++) {
            WindowState win = token.windows.get(i);
            DisplayContent winDisplayContent = win.getDisplayContent();
            if (winDisplayContent == displayContent || winDisplayContent == null) {
                win.mDisplayContent = displayContent;
                index = reAddWindowLocked(index, win);
            }
        }
        return index;
    }

    void tmpRemoveTaskWindowsLocked(Task task) {
        AppTokenList tokens = task.mAppTokens;
        for (int tokenNdx = tokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
            tmpRemoveAppWindowsLocked(tokens.get(tokenNdx));
        }
    }

    void moveStackWindowsLocked(DisplayContent displayContent) {
        ArrayList<Task> tasks = displayContent.getTasks();
        int numTasks = tasks.size();
        for (int taskNdx = 0; taskNdx < numTasks; taskNdx++) {
            tmpRemoveTaskWindowsLocked(tasks.get(taskNdx));
        }
        for (int taskNdx2 = 0; taskNdx2 < numTasks; taskNdx2++) {
            AppTokenList tokens = tasks.get(taskNdx2).mAppTokens;
            int numTokens = tokens.size();
            if (numTokens != 0) {
                int pos = findAppWindowInsertionPointLocked(tokens.get(0));
                for (int tokenNdx = 0; tokenNdx < numTokens; tokenNdx++) {
                    AppWindowToken wtoken = tokens.get(tokenNdx);
                    if (wtoken != null) {
                        int newPos = reAddAppWindowsLocked(displayContent, pos, wtoken);
                        if (newPos != pos) {
                            displayContent.layoutNeeded = true;
                        }
                        pos = newPos;
                    }
                }
            }
        }
        if (!updateFocusedWindowLocked(3, false)) {
            assignLayersLocked(displayContent.getWindowList());
        }
        this.mInputMonitor.setUpdateInputWindowsNeededLw();
        performLayoutAndPlaceSurfacesLocked();
        this.mInputMonitor.updateInputWindowsLw(false);
    }

    public void moveTaskToTop(int taskId) {
        TaskStack homeStack;
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                Task task = this.mTaskIdToTask.get(taskId);
                if (task != null) {
                    TaskStack stack = task.mStack;
                    DisplayContent displayContent = task.getDisplayContent();
                    displayContent.moveStack(stack, true);
                    if (displayContent.isDefaultDisplay && (homeStack = displayContent.getHomeStack()) != stack) {
                        displayContent.moveStack(homeStack, false);
                    }
                    stack.moveTaskToTop(task);
                    if (this.mAppTransition.isTransitionSet()) {
                        task.setSendingToBottom(false);
                    }
                    moveStackWindowsLocked(displayContent);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void moveTaskToBottom(int taskId) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                Task task = this.mTaskIdToTask.get(taskId);
                if (task == null) {
                    Slog.e(TAG, "moveTaskToBottom: taskId=" + taskId + " not found in mTaskIdToTask");
                    return;
                }
                TaskStack stack = task.mStack;
                stack.moveTaskToBottom(task);
                if (this.mAppTransition.isTransitionSet()) {
                    task.setSendingToBottom(true);
                }
                moveStackWindowsLocked(stack.getDisplayContent());
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void attachStack(int stackId, int displayId) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                DisplayContent displayContent = this.mDisplayContents.get(displayId);
                if (displayContent != null) {
                    TaskStack stack = this.mStackIdToStack.get(stackId);
                    if (stack == null) {
                        stack = new TaskStack(this, stackId);
                        this.mStackIdToStack.put(stackId, stack);
                    }
                    stack.attachDisplayContent(displayContent);
                    displayContent.attachStack(stack);
                    moveStackWindowsLocked(displayContent);
                    WindowList windows = displayContent.getWindowList();
                    for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                        windows.get(winNdx).reportResized();
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void detachStackLocked(DisplayContent displayContent, TaskStack stack) {
        displayContent.detachStack(stack);
        stack.detachDisplay();
    }

    public void detachStack(int stackId) {
        DisplayContent displayContent;
        synchronized (this.mWindowMap) {
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack != null && (displayContent = stack.getDisplayContent()) != null) {
                if (stack.isAnimating()) {
                    stack.mDeferDetach = true;
                    return;
                }
                detachStackLocked(displayContent, stack);
            }
        }
    }

    public void removeStack(int stackId) {
        this.mStackIdToStack.remove(stackId);
    }

    void removeTaskLocked(Task task) {
        int taskId = task.taskId;
        TaskStack stack = task.mStack;
        if (!task.mAppTokens.isEmpty() && stack.isAnimating()) {
            task.mDeferRemoval = true;
            return;
        }
        EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(taskId), "removeTask");
        task.mDeferRemoval = false;
        stack.removeTask(task);
        this.mTaskIdToTask.delete(task.taskId);
        ArrayList<AppWindowToken> exitingApps = stack.mExitingAppTokens;
        for (int appNdx = exitingApps.size() - 1; appNdx >= 0; appNdx--) {
            AppWindowToken wtoken = exitingApps.get(appNdx);
            if (wtoken.groupId == taskId) {
                wtoken.mDeferRemoval = false;
                exitingApps.remove(appNdx);
            }
        }
    }

    public void removeTask(int taskId) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task != null) {
                removeTaskLocked(task);
            }
        }
    }

    public void addTask(int taskId, int stackId, boolean toTop) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task != null) {
                TaskStack stack = this.mStackIdToStack.get(stackId);
                stack.addTask(task, toTop);
                DisplayContent displayContent = stack.getDisplayContent();
                displayContent.layoutNeeded = true;
                performLayoutAndPlaceSurfacesLocked();
            }
        }
    }

    public void resizeStack(int stackId, Rect bounds) {
        synchronized (this.mWindowMap) {
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack == null) {
                throw new IllegalArgumentException("resizeStack: stackId " + stackId + " not found.");
            }
            if (stack.setBounds(bounds)) {
                stack.resizeWindows();
                stack.getDisplayContent().layoutNeeded = true;
                performLayoutAndPlaceSurfacesLocked();
            }
        }
    }

    public void getStackBounds(int stackId, Rect bounds) {
        TaskStack stack = this.mStackIdToStack.get(stackId);
        if (stack != null) {
            stack.getBounds(bounds);
        } else {
            bounds.setEmpty();
        }
    }

    public void startFreezingScreen(int exitAnim, int enterAnim) {
        if (!checkCallingPermission("android.permission.FREEZE_SCREEN", "startFreezingScreen()")) {
            throw new SecurityException("Requires FREEZE_SCREEN permission");
        }
        synchronized (this.mWindowMap) {
            if (!this.mClientFreezingScreen) {
                this.mClientFreezingScreen = true;
                long origId = Binder.clearCallingIdentity();
                try {
                    startFreezingDisplayLocked(false, exitAnim, enterAnim);
                    this.mH.removeMessages(30);
                    this.mH.sendEmptyMessageDelayed(30, 5000L);
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }
    }

    public void stopFreezingScreen() {
        if (!checkCallingPermission("android.permission.FREEZE_SCREEN", "stopFreezingScreen()")) {
            throw new SecurityException("Requires FREEZE_SCREEN permission");
        }
        synchronized (this.mWindowMap) {
            if (this.mClientFreezingScreen) {
                this.mClientFreezingScreen = false;
                this.mLastFinishedFreezeSource = "client";
                long origId = Binder.clearCallingIdentity();
                try {
                    stopFreezingDisplayLocked();
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }
    }

    public void disableKeyguard(IBinder token, String tag) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        if (token == null) {
            throw new IllegalArgumentException("token == null");
        }
        this.mKeyguardDisableHandler.sendMessage(this.mKeyguardDisableHandler.obtainMessage(1, new Pair(token, tag)));
    }

    public void reenableKeyguard(IBinder token) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        if (token == null) {
            throw new IllegalArgumentException("token == null");
        }
        this.mKeyguardDisableHandler.sendMessage(this.mKeyguardDisableHandler.obtainMessage(2, token));
    }

    public void exitKeyguardSecurely(final IOnKeyguardExitResult callback) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback == null");
        }
        this.mPolicy.exitKeyguardSecurely(new WindowManagerPolicy.OnKeyguardExitResult() {
            public void onKeyguardExitResult(boolean success) {
                try {
                    callback.onKeyguardExitResult(success);
                } catch (RemoteException e) {
                }
            }
        });
    }

    public boolean inKeyguardRestrictedInputMode() {
        return this.mPolicy.inKeyguardRestrictedKeyInputMode();
    }

    public boolean isKeyguardLocked() {
        return this.mPolicy.isKeyguardLocked();
    }

    public boolean isKeyguardSecure() {
        long origId = Binder.clearCallingIdentity();
        try {
            return this.mPolicy.isKeyguardSecure();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void dismissKeyguard() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        synchronized (this.mWindowMap) {
            this.mPolicy.dismissKeyguardLw();
        }
    }

    public void keyguardGoingAway(boolean disableWindowAnimations, boolean keyguardGoingToNotificationShade) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        synchronized (this.mWindowMap) {
            this.mAnimator.mKeyguardGoingAway = true;
            this.mAnimator.mKeyguardGoingAwayToNotificationShade = keyguardGoingToNotificationShade;
            this.mAnimator.mKeyguardGoingAwayDisableWindowAnimations = disableWindowAnimations;
            requestTraversalLocked();
        }
    }

    public void keyguardWaitingForActivityDrawn() {
        synchronized (this.mWindowMap) {
            this.mKeyguardWaitingForActivityDrawn = true;
        }
    }

    public void notifyActivityDrawnForKeyguard() {
        synchronized (this.mWindowMap) {
            if (this.mKeyguardWaitingForActivityDrawn) {
                this.mPolicy.notifyActivityDrawnForKeyguardLw();
                this.mKeyguardWaitingForActivityDrawn = false;
            }
        }
    }

    void showGlobalActions() {
        this.mPolicy.showGlobalActions();
    }

    public void closeSystemDialogs(String reason) {
        synchronized (this.mWindowMap) {
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                int numWindows = windows.size();
                for (int winNdx = 0; winNdx < numWindows; winNdx++) {
                    WindowState w = windows.get(winNdx);
                    if (w.mHasSurface) {
                        try {
                            w.mClient.closeSystemDialogs(reason);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        }
    }

    static float fixScale(float scale) {
        if (scale < 0.0f) {
            scale = 0.0f;
        } else if (scale > 20.0f) {
            scale = 20.0f;
        }
        return Math.abs(scale);
    }

    public void setAnimationScale(int which, float scale) {
        if (!checkCallingPermission("android.permission.SET_ANIMATION_SCALE", "setAnimationScale()")) {
            throw new SecurityException("Requires SET_ANIMATION_SCALE permission");
        }
        float scale2 = fixScale(scale);
        switch (which) {
            case 0:
                this.mWindowAnimationScaleSetting = scale2;
                break;
            case 1:
                this.mTransitionAnimationScaleSetting = scale2;
                break;
            case 2:
                this.mAnimatorDurationScaleSetting = scale2;
                break;
        }
        this.mH.sendEmptyMessage(14);
    }

    public void setAnimationScales(float[] scales) {
        if (!checkCallingPermission("android.permission.SET_ANIMATION_SCALE", "setAnimationScale()")) {
            throw new SecurityException("Requires SET_ANIMATION_SCALE permission");
        }
        if (scales != null) {
            if (scales.length >= 1) {
                this.mWindowAnimationScaleSetting = fixScale(scales[0]);
            }
            if (scales.length >= 2) {
                this.mTransitionAnimationScaleSetting = fixScale(scales[1]);
            }
            if (scales.length >= 3) {
                this.mAnimatorDurationScaleSetting = fixScale(scales[2]);
                dispatchNewAnimatorScaleLocked(null);
            }
        }
        this.mH.sendEmptyMessage(14);
    }

    private void setAnimatorDurationScale(float scale) {
        this.mAnimatorDurationScaleSetting = scale;
        ValueAnimator.setDurationScale(scale);
    }

    public float getWindowAnimationScaleLocked() {
        if (this.mAnimationsDisabled) {
            return 0.0f;
        }
        return this.mWindowAnimationScaleSetting;
    }

    public float getTransitionAnimationScaleLocked() {
        if (this.mAnimationsDisabled) {
            return 0.0f;
        }
        return this.mTransitionAnimationScaleSetting;
    }

    public float getAnimationScale(int which) {
        switch (which) {
            case 0:
                return this.mWindowAnimationScaleSetting;
            case 1:
                return this.mTransitionAnimationScaleSetting;
            case 2:
                return this.mAnimatorDurationScaleSetting;
            default:
                return 0.0f;
        }
    }

    public float[] getAnimationScales() {
        return new float[]{this.mWindowAnimationScaleSetting, this.mTransitionAnimationScaleSetting, this.mAnimatorDurationScaleSetting};
    }

    public float getCurrentAnimatorScale() {
        float f;
        synchronized (this.mWindowMap) {
            f = this.mAnimationsDisabled ? 0.0f : this.mAnimatorDurationScaleSetting;
        }
        return f;
    }

    void dispatchNewAnimatorScaleLocked(Session session) {
        this.mH.obtainMessage(34, session).sendToTarget();
    }

    public void registerPointerEventListener(WindowManagerPolicy.PointerEventListener listener) {
        this.mPointerEventDispatcher.registerInputEventListener(listener);
    }

    public void unregisterPointerEventListener(WindowManagerPolicy.PointerEventListener listener) {
        this.mPointerEventDispatcher.unregisterInputEventListener(listener);
    }

    public int getLidState() {
        int sw = this.mInputManager.getSwitchState(-1, -256, 0);
        if (sw > 0) {
            return 0;
        }
        return sw == 0 ? 1 : -1;
    }

    public int getCameraLensCoverState() {
        int sw = this.mInputManager.getSwitchState(-1, -256, 9);
        if (sw > 0) {
            return 1;
        }
        return sw == 0 ? 0 : -1;
    }

    public void switchKeyboardLayout(int deviceId, int direction) {
        this.mInputManager.switchKeyboardLayout(deviceId, direction);
    }

    public void shutdown(boolean confirm) {
        ShutdownThread.shutdown(this.mContext, confirm);
    }

    public void rebootSafeMode(boolean confirm) {
        ShutdownThread.rebootSafeMode(this.mContext, confirm);
    }

    public void setCurrentProfileIds(int[] currentProfileIds) {
        synchronized (this.mWindowMap) {
            this.mCurrentProfileIds = currentProfileIds;
        }
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        synchronized (this.mWindowMap) {
            this.mCurrentUserId = newUserId;
            this.mCurrentProfileIds = currentProfileIds;
            this.mAppTransition.setCurrentUser(newUserId);
            this.mPolicy.setCurrentUserLw(newUserId);
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
                displayContent.switchUserStacks(newUserId);
                rebuildAppWindowListLocked(displayContent);
            }
            performLayoutAndPlaceSurfacesLocked();
        }
    }

    boolean isCurrentProfileLocked(int userId) {
        if (userId == this.mCurrentUserId) {
            return true;
        }
        for (int i = 0; i < this.mCurrentProfileIds.length; i++) {
            if (this.mCurrentProfileIds[i] == userId) {
                return true;
            }
        }
        return false;
    }

    public void enableScreenAfterBoot() {
        synchronized (this.mWindowMap) {
            if (!this.mSystemBooted) {
                this.mSystemBooted = true;
                hideBootMessagesLocked();
                this.mH.sendEmptyMessageDelayed(23, 30000L);
                this.mPolicy.systemBooted();
                performEnableScreen();
            }
        }
    }

    public void enableScreenIfNeeded() {
        synchronized (this.mWindowMap) {
            enableScreenIfNeededLocked();
        }
    }

    void enableScreenIfNeededLocked() {
        if (!this.mDisplayEnabled) {
            if (this.mSystemBooted || this.mShowingBootMessages) {
                this.mH.sendEmptyMessage(16);
            }
        }
    }

    public void performBootTimeout() {
        synchronized (this.mWindowMap) {
            if (!this.mDisplayEnabled) {
                Slog.w(TAG, "***** BOOT TIMEOUT: forcing display enabled");
                this.mForceDisplayEnabled = true;
                performEnableScreen();
            }
        }
    }

    private boolean checkWaitingForWindowsLocked() {
        boolean haveBootMsg = false;
        boolean haveApp = false;
        boolean haveWallpaper = false;
        boolean wallpaperEnabled = this.mContext.getResources().getBoolean(R.^attr-private.dreamActivityOpenExitAnimation) && !this.mOnlyCore;
        boolean haveKeyguard = true;
        WindowList windows = getDefaultWindowListLocked();
        int N = windows.size();
        for (int i = 0; i < N; i++) {
            WindowState w = windows.get(i);
            if (w.isVisibleLw() && !w.mObscured && !w.isDrawnLw()) {
                return true;
            }
            if (w.isDrawnLw()) {
                if (w.mAttrs.type == 2021) {
                    haveBootMsg = true;
                } else if (w.mAttrs.type == 2) {
                    haveApp = true;
                } else if (w.mAttrs.type == 2013) {
                    haveWallpaper = true;
                } else if (w.mAttrs.type == WINDOW_FREEZE_TIMEOUT_DURATION) {
                    haveKeyguard = this.mPolicy.isKeyguardDrawnLw();
                }
            }
        }
        if (!this.mSystemBooted && !haveBootMsg) {
            return true;
        }
        if (this.mSystemBooted) {
            if (!haveApp && !haveKeyguard) {
                return true;
            }
            if (wallpaperEnabled && !haveWallpaper) {
                return true;
            }
        }
        return false;
    }

    public void performEnableScreen() {
        synchronized (this.mWindowMap) {
            if (!this.mDisplayEnabled) {
                if (this.mSystemBooted || this.mShowingBootMessages) {
                    if (this.mForceDisplayEnabled || !checkWaitingForWindowsLocked()) {
                        if (!this.mBootAnimationStopped) {
                            try {
                                IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                                if (surfaceFlinger != null) {
                                    Parcel data = Parcel.obtain();
                                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                                    surfaceFlinger.transact(1, data, null, 0);
                                    data.recycle();
                                }
                            } catch (RemoteException e) {
                                Slog.e(TAG, "Boot completed: SurfaceFlinger is dead!");
                            }
                            this.mBootAnimationStopped = true;
                        }
                        if (this.mForceDisplayEnabled || checkBootAnimationCompleteLocked()) {
                            this.mDisplayEnabled = true;
                            this.mInputMonitor.setEventDispatchingLw(this.mEventDispatchingEnabled);
                            try {
                                this.mActivityManager.bootAnimationComplete();
                            } catch (RemoteException e2) {
                            }
                            this.mPolicy.enableScreenAfterBoot();
                            updateRotationUnchecked(false, false);
                        }
                    }
                }
            }
        }
    }

    private boolean checkBootAnimationCompleteLocked() {
        if (!SystemService.isRunning(BOOT_ANIMATION_SERVICE)) {
            return true;
        }
        this.mH.removeMessages(37);
        this.mH.sendEmptyMessageDelayed(37, 200L);
        return false;
    }

    public void showBootMessage(CharSequence msg, boolean always) {
        boolean first = false;
        synchronized (this.mWindowMap) {
            if (this.mAllowBootMessages) {
                if (!this.mShowingBootMessages) {
                    if (always) {
                        first = true;
                    } else {
                        return;
                    }
                }
                if (!this.mSystemBooted) {
                    this.mShowingBootMessages = true;
                    this.mPolicy.showBootMessage(msg, always);
                    if (first) {
                        performEnableScreen();
                    }
                }
            }
        }
    }

    public void hideBootMessagesLocked() {
        if (this.mShowingBootMessages) {
            this.mShowingBootMessages = false;
            this.mPolicy.hideBootMessages();
        }
    }

    public void setInTouchMode(boolean mode) {
        synchronized (this.mWindowMap) {
            this.mInTouchMode = mode;
        }
    }

    public void updateCircularDisplayMaskIfNeeded() {
        if (this.mContext.getResources().getBoolean(R.^attr-private.keyboardViewStyle) && this.mContext.getResources().getBoolean(R.^attr-private.layout_alwaysShow)) {
            int inversionState = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_inversion_enabled", 0, this.mCurrentUserId);
            int showMask = inversionState != 1 ? 1 : 0;
            Message m = this.mH.obtainMessage(35);
            m.arg1 = showMask;
            this.mH.sendMessage(m);
        }
    }

    public void showEmulatorDisplayOverlayIfNeeded() {
        if (this.mContext.getResources().getBoolean(R.^attr-private.layout_childType) && SystemProperties.getBoolean(PROPERTY_EMULATOR_CIRCULAR, false) && Build.HARDWARE.contains("goldfish")) {
            this.mH.sendMessage(this.mH.obtainMessage(36));
        }
    }

    public void showCircularMask(boolean visible) {
        synchronized (this.mWindowMap) {
            SurfaceControl.openTransaction();
            try {
                if (visible) {
                    if (this.mCircularDisplayMask == null) {
                        int screenOffset = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_label2_size);
                        this.mCircularDisplayMask = new CircularDisplayMask(getDefaultDisplayContentLocked().getDisplay(), this.mFxSession, (this.mPolicy.windowTypeToLayerLw(2018) * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 10, screenOffset);
                    }
                    this.mCircularDisplayMask.setVisibility(true);
                } else if (this.mCircularDisplayMask != null) {
                    this.mCircularDisplayMask.setVisibility(false);
                    this.mCircularDisplayMask = null;
                }
            } finally {
                SurfaceControl.closeTransaction();
            }
        }
    }

    public void showEmulatorDisplayOverlay() {
        synchronized (this.mWindowMap) {
            SurfaceControl.openTransaction();
            try {
                if (this.mEmulatorDisplayOverlay == null) {
                    this.mEmulatorDisplayOverlay = new EmulatorDisplayOverlay(this.mContext, getDefaultDisplayContentLocked().getDisplay(), this.mFxSession, (this.mPolicy.windowTypeToLayerLw(2018) * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 10);
                }
                this.mEmulatorDisplayOverlay.setVisibility(true);
            } finally {
                SurfaceControl.closeTransaction();
            }
        }
    }

    public void showStrictModeViolation(boolean on) {
        int pid = Binder.getCallingPid();
        this.mH.sendMessage(this.mH.obtainMessage(25, on ? 1 : 0, pid));
    }

    private void showStrictModeViolation(int arg, int pid) {
        boolean on = arg != 0;
        synchronized (this.mWindowMap) {
            if (on) {
                boolean isVisible = false;
                int numDisplays = this.mDisplayContents.size();
                for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                    WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                    int numWindows = windows.size();
                    int winNdx = 0;
                    while (true) {
                        if (winNdx < numWindows) {
                            WindowState ws = windows.get(winNdx);
                            if (ws.mSession.mPid != pid || !ws.isVisibleLw()) {
                                winNdx++;
                            } else {
                                isVisible = true;
                                break;
                            }
                        }
                    }
                }
                if (!isVisible) {
                    return;
                }
                SurfaceControl.openTransaction();
                try {
                    if (this.mStrictModeFlash == null) {
                        this.mStrictModeFlash = new StrictModeFlash(getDefaultDisplayContentLocked().getDisplay(), this.mFxSession);
                    }
                    this.mStrictModeFlash.setVisibility(on);
                    return;
                } finally {
                    SurfaceControl.closeTransaction();
                }
            }
            SurfaceControl.openTransaction();
            if (this.mStrictModeFlash == null) {
            }
            this.mStrictModeFlash.setVisibility(on);
            return;
        }
    }

    public void setStrictModeVisualIndicatorPreference(String value) {
        SystemProperties.set("persist.sys.strictmode.visual", value);
    }

    private static void convertCropForSurfaceFlinger(Rect crop, int rot, int dw, int dh) {
        if (rot == 1) {
            int tmp = crop.top;
            crop.top = dw - crop.right;
            crop.right = crop.bottom;
            crop.bottom = dw - crop.left;
            crop.left = tmp;
            return;
        }
        if (rot == 2) {
            int tmp2 = crop.top;
            crop.top = dh - crop.bottom;
            crop.bottom = dh - tmp2;
            int tmp3 = crop.right;
            crop.right = dw - crop.left;
            crop.left = dw - tmp3;
            return;
        }
        if (rot == 3) {
            int tmp4 = crop.top;
            crop.top = crop.left;
            crop.left = dh - crop.bottom;
            crop.bottom = crop.right;
            crop.right = dh - tmp4;
        }
    }

    public Bitmap screenshotApplications(IBinder appToken, int displayId, int width, int height, boolean force565) {
        boolean screenshotReady;
        int minLayer;
        WindowStateAnimator winAnim;
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "screenshotApplications()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        DisplayContent displayContent = getDisplayContentLocked(displayId);
        if (displayContent == null) {
            return null;
        }
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        if (dw == 0 || dh == 0) {
            return null;
        }
        int maxLayer = 0;
        Rect frame = new Rect();
        Rect stackBounds = new Rect();
        if (appToken == null) {
            screenshotReady = true;
            minLayer = 0;
        } else {
            screenshotReady = false;
            minLayer = Integer.MAX_VALUE;
        }
        boolean appIsImTarget = (this.mInputMethodTarget == null || this.mInputMethodTarget.mAppToken == null || this.mInputMethodTarget.mAppToken.appToken == null || this.mInputMethodTarget.mAppToken.appToken.asBinder() != appToken) ? false : true;
        int aboveAppLayer = ((this.mPolicy.windowTypeToLayerLw(2) + 1) * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 1000;
        int retryCount = 0;
        while (true) {
            int retryCount2 = retryCount + 1;
            if (retryCount > 0) {
                maxLayer = 0;
                minLayer = Integer.MAX_VALUE;
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                }
            }
            synchronized (this.mWindowMap) {
                WindowState appWin = null;
                WindowList windows = displayContent.getWindowList();
                for (int i = windows.size() - 1; i >= 0; i--) {
                    WindowState ws = windows.get(i);
                    if (ws.mHasSurface && ws.mLayer < aboveAppLayer) {
                        if (ws.mIsImWindow) {
                            if (appIsImTarget) {
                                winAnim = ws.mWinAnimator;
                                if (maxLayer < winAnim.mSurfaceLayer) {
                                    maxLayer = winAnim.mSurfaceLayer;
                                }
                                if (minLayer > winAnim.mSurfaceLayer) {
                                    minLayer = winAnim.mSurfaceLayer;
                                }
                                if (!ws.mIsWallpaper) {
                                    Rect wf = ws.mFrame;
                                    Rect cr = ws.mContentInsets;
                                    int left = wf.left + cr.left;
                                    int top = wf.top + cr.top;
                                    int right = wf.right - cr.right;
                                    int bottom = wf.bottom - cr.bottom;
                                    frame.union(left, top, right, bottom);
                                    ws.getStackBounds(stackBounds);
                                    frame.intersect(stackBounds);
                                }
                                if (ws.mAppToken == null && ws.mAppToken.token == appToken && ws.isDisplayedLw()) {
                                    screenshotReady = true;
                                }
                            }
                        } else if (ws.mIsWallpaper) {
                            if (appWin == null) {
                            }
                        } else if (appToken != null) {
                            if (ws.mAppToken != null && ws.mAppToken.token == appToken) {
                                appWin = ws;
                                winAnim = ws.mWinAnimator;
                                if (maxLayer < winAnim.mSurfaceLayer) {
                                }
                                if (minLayer > winAnim.mSurfaceLayer) {
                                }
                                if (!ws.mIsWallpaper) {
                                }
                                if (ws.mAppToken == null) {
                                }
                            }
                        }
                    }
                }
                if (appToken != null && appWin == null) {
                    return null;
                }
                if (!screenshotReady) {
                    if (retryCount2 > 3) {
                        Slog.i(TAG, "Screenshot max retries " + retryCount2 + " of " + appToken + " appWin=" + (appWin == null ? "null" : appWin + " drawState=" + appWin.mWinAnimator.mDrawState));
                        return null;
                    }
                } else {
                    if (maxLayer == 0) {
                        return null;
                    }
                    frame.intersect(0, 0, dw, dh);
                    Rect crop = new Rect(frame);
                    if (width / frame.width() < height / frame.height()) {
                        int cropWidth = (int) ((width / height) * frame.height());
                        crop.right = crop.left + cropWidth;
                    } else {
                        int cropHeight = (int) ((height / width) * frame.width());
                        crop.bottom = crop.top + cropHeight;
                    }
                    int rot = getDefaultDisplayContentLocked().getDisplay().getRotation();
                    if (rot == 1 || rot == 3) {
                        rot = rot == 1 ? 3 : 1;
                    }
                    convertCropForSurfaceFlinger(crop, rot, dw, dh);
                    ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(0);
                    boolean inRotation = screenRotationAnimation != null && screenRotationAnimation.isAnimating();
                    Bitmap bm = SurfaceControl.screenshot(crop, width, height, minLayer, maxLayer, inRotation, rot);
                    if (bm == null) {
                        Slog.w(TAG, "Screenshot failure taking screenshot for (" + dw + "x" + dh + ") to layer " + maxLayer);
                        return null;
                    }
                    Bitmap bitmapCopy = bm.copy(bm.getConfig(), true);
                    bm.recycle();
                    return bitmapCopy;
                }
            }
            retryCount = retryCount2;
        }
    }

    public void freezeRotation(int rotation) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "freezeRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        if (rotation < -1 || rotation > 3) {
            throw new IllegalArgumentException("Rotation argument must be -1 or a valid rotation constant.");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            WindowManagerPolicy windowManagerPolicy = this.mPolicy;
            if (rotation == -1) {
                rotation = this.mRotation;
            }
            windowManagerPolicy.setUserRotationMode(1, rotation);
            Binder.restoreCallingIdentity(origId);
            updateRotationUnchecked(false, false);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
            throw th;
        }
    }

    public void thawRotation() {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "thawRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            this.mPolicy.setUserRotationMode(0, 777);
            Binder.restoreCallingIdentity(origId);
            updateRotationUnchecked(false, false);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
            throw th;
        }
    }

    public void updateRotation(boolean alwaysSendConfiguration, boolean forceRelayout) {
        updateRotationUnchecked(alwaysSendConfiguration, forceRelayout);
    }

    void pauseRotationLocked() {
        this.mDeferredRotationPauseCount++;
    }

    void resumeRotationLocked() {
        if (this.mDeferredRotationPauseCount > 0) {
            this.mDeferredRotationPauseCount--;
            if (this.mDeferredRotationPauseCount == 0) {
                boolean changed = updateRotationUncheckedLocked(false);
                if (changed) {
                    this.mH.sendEmptyMessage(18);
                }
            }
        }
    }

    public void updateRotationUnchecked(boolean alwaysSendConfiguration, boolean forceRelayout) {
        boolean changed;
        long origId = Binder.clearCallingIdentity();
        Slog.v(TAG, "PowerHint: rotation boost");
        PowerHintManager phm = new PowerHintManager();
        double phmId = phm.obtainDurablePowerHintTimer();
        phm.sendDurablePowerHint("rotation", phmId, WINDOW_FREEZE_TIMEOUT_DURATION, "enable");
        synchronized (this.mWindowMap) {
            changed = updateRotationUncheckedLocked(false);
            if (!changed || forceRelayout) {
                getDefaultDisplayContentLocked().layoutNeeded = true;
                performLayoutAndPlaceSurfacesLocked();
            }
        }
        if (changed || alwaysSendConfiguration) {
            sendNewConfiguration();
        }
        Binder.restoreCallingIdentity(origId);
    }

    public boolean updateRotationUncheckedLocked(boolean inTransaction) {
        if (this.mDeferredRotationPauseCount > 0) {
            return false;
        }
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(0);
        if ((screenRotationAnimation != null && screenRotationAnimation.isAnimating()) || !this.mDisplayEnabled) {
            return false;
        }
        int rotation = this.mPolicy.rotationForOrientationLw(this.mForcedAppOrientation, this.mRotation);
        boolean altOrientation = !this.mPolicy.rotationHasCompatibleMetricsLw(this.mForcedAppOrientation, rotation);
        if (this.mRotation == rotation && this.mAltOrientation == altOrientation) {
            return false;
        }
        this.mRotation = rotation;
        this.mAltOrientation = altOrientation;
        this.mPolicy.setRotationLw(this.mRotation);
        this.mWindowsFreezingScreen = true;
        this.mH.removeMessages(11);
        this.mH.sendEmptyMessageDelayed(11, 2000L);
        this.mWaitingForConfig = true;
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        displayContent.layoutNeeded = true;
        int[] anim = new int[2];
        if (displayContent.isDimming()) {
            anim[1] = 0;
            anim[0] = 0;
        } else {
            this.mPolicy.selectRotationAnimationLw(anim);
        }
        startFreezingDisplayLocked(inTransaction, anim[0], anim[1]);
        ScreenRotationAnimation screenRotationAnimation2 = this.mAnimator.getScreenRotationAnimationLocked(0);
        computeScreenConfigurationLocked(null);
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        if (!inTransaction) {
            SurfaceControl.openTransaction();
        }
        if (screenRotationAnimation2 != null) {
            try {
                if (screenRotationAnimation2.hasScreenshot() && screenRotationAnimation2.setRotationInTransaction(rotation, this.mFxSession, WALLPAPER_TIMEOUT_RECOVERY, getTransitionAnimationScaleLocked(), displayInfo.logicalWidth, displayInfo.logicalHeight)) {
                    scheduleAnimationLocked();
                }
            } finally {
                if (!inTransaction) {
                    SurfaceControl.closeTransaction();
                }
            }
        }
        this.mDisplayManagerInternal.performTraversalInTransactionFromWindowManager();
        WindowList windows = displayContent.getWindowList();
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowState w = windows.get(i);
            if (w.mHasSurface) {
                w.mOrientationChanging = true;
                this.mInnerFields.mOrientationChangeComplete = false;
            }
            w.mLastFreezeDuration = 0;
        }
        for (int i2 = this.mRotationWatchers.size() - 1; i2 >= 0; i2--) {
            try {
                this.mRotationWatchers.get(i2).watcher.onRotationChanged(rotation);
            } catch (RemoteException e) {
            }
        }
        if (screenRotationAnimation2 == null && this.mAccessibilityController != null && displayContent.getDisplayId() == 0) {
            this.mAccessibilityController.onRotationChangedLocked(getDefaultDisplayContentLocked(), rotation);
        }
        return true;
    }

    public int getRotation() {
        return this.mRotation;
    }

    public boolean isRotationFrozen() {
        return this.mPolicy.getUserRotationMode() == 1;
    }

    public int watchRotation(IRotationWatcher watcher) {
        int i;
        final IBinder watcherBinder = watcher.asBinder();
        IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                synchronized (WindowManagerService.this.mWindowMap) {
                    int i2 = 0;
                    while (i2 < WindowManagerService.this.mRotationWatchers.size()) {
                        if (watcherBinder == WindowManagerService.this.mRotationWatchers.get(i2).watcher.asBinder()) {
                            RotationWatcher removed = WindowManagerService.this.mRotationWatchers.remove(i2);
                            IBinder binder = removed.watcher.asBinder();
                            if (binder != null) {
                                binder.unlinkToDeath(this, 0);
                            }
                            i2--;
                        }
                        i2++;
                    }
                }
            }
        };
        synchronized (this.mWindowMap) {
            try {
                watcher.asBinder().linkToDeath(dr, 0);
                this.mRotationWatchers.add(new RotationWatcher(watcher, dr));
            } catch (RemoteException e) {
            }
            i = this.mRotation;
        }
        return i;
    }

    public void removeRotationWatcher(IRotationWatcher watcher) {
        IBinder watcherBinder = watcher.asBinder();
        synchronized (this.mWindowMap) {
            int i = 0;
            while (i < this.mRotationWatchers.size()) {
                RotationWatcher rotationWatcher = this.mRotationWatchers.get(i);
                if (watcherBinder == rotationWatcher.watcher.asBinder()) {
                    RotationWatcher removed = this.mRotationWatchers.remove(i);
                    IBinder binder = removed.watcher.asBinder();
                    if (binder != null) {
                        binder.unlinkToDeath(removed.deathRecipient, 0);
                    }
                    i--;
                }
                i++;
            }
        }
    }

    public int getPreferredOptionsPanelGravity() {
        synchronized (this.mWindowMap) {
            int rotation = getRotation();
            DisplayContent displayContent = getDefaultDisplayContentLocked();
            if (displayContent.mInitialDisplayWidth < displayContent.mInitialDisplayHeight) {
                switch (rotation) {
                    case 1:
                        return 85;
                    case 2:
                        return 81;
                    case 3:
                        return 8388691;
                    default:
                        return 81;
                }
            }
            switch (rotation) {
                case 1:
                    return 81;
                case 2:
                    return 8388691;
                case 3:
                    return 81;
                default:
                    return 85;
            }
        }
    }

    public boolean startViewServer(int port) {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "startViewServer") || port < 1024) {
            return false;
        }
        if (this.mViewServer != null) {
            if (this.mViewServer.isRunning()) {
                return false;
            }
            try {
                return this.mViewServer.start();
            } catch (IOException e) {
                Slog.w(TAG, "View server did not start");
                return false;
            }
        }
        try {
            this.mViewServer = new ViewServer(this, port);
            return this.mViewServer.start();
        } catch (IOException e2) {
            Slog.w(TAG, "View server did not start");
            return false;
        }
    }

    private boolean isSystemSecure() {
        return "1".equals(SystemProperties.get(SYSTEM_SECURE, "1")) && "0".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
    }

    public boolean stopViewServer() {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "stopViewServer") || this.mViewServer == null) {
            return false;
        }
        return this.mViewServer.stop();
    }

    public boolean isViewServerRunning() {
        return !isSystemSecure() && checkCallingPermission("android.permission.DUMP", "isViewServerRunning") && this.mViewServer != null && this.mViewServer.isRunning();
    }

    boolean viewServerListWindows(Socket client) throws Throwable {
        if (isSystemSecure()) {
            return false;
        }
        WindowList windows = new WindowList();
        synchronized (this.mWindowMap) {
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
                windows.addAll(displayContent.getWindowList());
            }
        }
        BufferedWriter out = null;
        try {
            OutputStream clientStream = client.getOutputStream();
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(clientStream), PackageManagerService.DumpState.DUMP_INSTALLS);
            try {
                int count = windows.size();
                for (int i = 0; i < count; i++) {
                    WindowState w = windows.get(i);
                    out2.write(Integer.toHexString(System.identityHashCode(w)));
                    out2.write(32);
                    out2.append(w.mAttrs.getTitle());
                    out2.write(10);
                }
                out2.write("DONE.\n");
                out2.flush();
                if (out2 == null) {
                    return true;
                }
                try {
                    out2.close();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            } catch (Exception e2) {
                out = out2;
                if (out == null) {
                    return false;
                }
                try {
                    out.close();
                    return false;
                } catch (IOException e3) {
                    return false;
                }
            } catch (Throwable th) {
                th = th;
                out = out2;
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    boolean viewServerGetFocusedWindow(Socket client) throws Throwable {
        if (isSystemSecure()) {
            return false;
        }
        WindowState focusedWindow = getFocusedWindow();
        BufferedWriter out = null;
        try {
            OutputStream clientStream = client.getOutputStream();
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(clientStream), PackageManagerService.DumpState.DUMP_INSTALLS);
            if (focusedWindow != null) {
                try {
                    out2.write(Integer.toHexString(System.identityHashCode(focusedWindow)));
                    out2.write(32);
                    out2.append(focusedWindow.mAttrs.getTitle());
                } catch (Exception e) {
                    out = out2;
                    if (out == null) {
                        return false;
                    }
                    try {
                        out.close();
                        return false;
                    } catch (IOException e2) {
                        return false;
                    }
                } catch (Throwable th) {
                    th = th;
                    out = out2;
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e3) {
                        }
                    }
                    throw th;
                }
            }
            out2.write(10);
            out2.flush();
            if (out2 == null) {
                return true;
            }
            try {
                out2.close();
                return true;
            } catch (IOException e4) {
                return false;
            }
        } catch (Exception e5) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    boolean viewServerWindowCommand(Socket client, String command, String parameters) throws Throwable {
        if (isSystemSecure()) {
            return false;
        }
        Parcel data = null;
        Parcel reply = null;
        BufferedWriter out = null;
        try {
            try {
                int index = parameters.indexOf(32);
                if (index == -1) {
                    index = parameters.length();
                }
                String code = parameters.substring(0, index);
                int hashCode = (int) Long.parseLong(code, 16);
                parameters = index < parameters.length() ? parameters.substring(index + 1) : "";
                WindowState window = findWindow(hashCode);
                if (window == null) {
                    if (0 != 0) {
                        data.recycle();
                    }
                    if (0 != 0) {
                        reply.recycle();
                    }
                    if (0 == 0) {
                        return false;
                    }
                    try {
                        out.close();
                        return false;
                    } catch (IOException e) {
                        return false;
                    }
                }
                data = Parcel.obtain();
                data.writeInterfaceToken("android.view.IWindow");
                data.writeString(command);
                data.writeString(parameters);
                data.writeInt(1);
                ParcelFileDescriptor.fromSocket(client).writeToParcel(data, 0);
                reply = Parcel.obtain();
                IBinder binder = window.mClient.asBinder();
                binder.transact(1, data, reply, 0);
                reply.readException();
                if (!client.isOutputShutdown()) {
                    BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                    try {
                        out2.write("DONE\n");
                        out2.flush();
                        out = out2;
                    } catch (Exception e2) {
                        e = e2;
                        out = out2;
                    } catch (Throwable th) {
                        th = th;
                        out = out2;
                        if (data != null) {
                            data.recycle();
                        }
                        if (reply != null) {
                            reply.recycle();
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e3) {
                            }
                        }
                        throw th;
                    }
                }
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                }
                if (out == null) {
                    return true;
                }
                try {
                    out.close();
                    return true;
                } catch (IOException e4) {
                    return true;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Exception e5) {
            e = e5;
        }
        Slog.w(TAG, "Could not send command " + command + " with parameters " + parameters, e);
        if (data != null) {
            data.recycle();
        }
        if (reply != null) {
            reply.recycle();
        }
        if (out == null) {
            return false;
        }
        try {
            out.close();
            return false;
        } catch (IOException e6) {
            return false;
        }
    }

    public void addWindowChangeListener(WindowChangeListener listener) {
        synchronized (this.mWindowMap) {
            this.mWindowChangeListeners.add(listener);
        }
    }

    public void removeWindowChangeListener(WindowChangeListener listener) {
        synchronized (this.mWindowMap) {
            this.mWindowChangeListeners.remove(listener);
        }
    }

    private void notifyWindowsChanged() {
        synchronized (this.mWindowMap) {
            if (!this.mWindowChangeListeners.isEmpty()) {
                WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                for (WindowChangeListener windowChangeListener : windowChangeListeners) {
                    windowChangeListener.windowsChanged();
                }
            }
        }
    }

    private void notifyFocusChanged() {
        synchronized (this.mWindowMap) {
            if (!this.mWindowChangeListeners.isEmpty()) {
                WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                for (WindowChangeListener windowChangeListener : windowChangeListeners) {
                    windowChangeListener.focusChanged();
                }
            }
        }
    }

    private WindowState findWindow(int hashCode) {
        WindowState w;
        if (hashCode == -1) {
            return getFocusedWindow();
        }
        synchronized (this.mWindowMap) {
            int numDisplays = this.mDisplayContents.size();
            int displayNdx = 0;
            loop0: while (true) {
                if (displayNdx < numDisplays) {
                    WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                    int numWindows = windows.size();
                    for (int winNdx = 0; winNdx < numWindows; winNdx++) {
                        w = windows.get(winNdx);
                        if (System.identityHashCode(w) == hashCode) {
                            break loop0;
                        }
                    }
                    displayNdx++;
                } else {
                    w = null;
                    break;
                }
            }
        }
        return w;
    }

    void sendNewConfiguration() {
        try {
            this.mActivityManager.updateConfiguration((Configuration) null);
        } catch (RemoteException e) {
        }
    }

    public Configuration computeNewConfiguration() {
        Configuration config;
        synchronized (this.mWindowMap) {
            config = computeNewConfigurationLocked();
            if (config == null && this.mWaitingForConfig) {
                this.mWaitingForConfig = false;
                this.mLastFinishedFreezeSource = "new-config";
                performLayoutAndPlaceSurfacesLocked();
            }
        }
        return config;
    }

    Configuration computeNewConfigurationLocked() {
        Configuration config = new Configuration();
        config.fontScale = 0.0f;
        if (!computeScreenConfigurationLocked(config)) {
            return null;
        }
        return config;
    }

    private void adjustDisplaySizeRanges(DisplayInfo displayInfo, int rotation, int dw, int dh) {
        int width = this.mPolicy.getConfigDisplayWidth(dw, dh, rotation);
        if (width < displayInfo.smallestNominalAppWidth) {
            displayInfo.smallestNominalAppWidth = width;
        }
        if (width > displayInfo.largestNominalAppWidth) {
            displayInfo.largestNominalAppWidth = width;
        }
        int height = this.mPolicy.getConfigDisplayHeight(dw, dh, rotation);
        if (height < displayInfo.smallestNominalAppHeight) {
            displayInfo.smallestNominalAppHeight = height;
        }
        if (height > displayInfo.largestNominalAppHeight) {
            displayInfo.largestNominalAppHeight = height;
        }
    }

    private int reduceConfigLayout(int curLayout, int rotation, float density, int dw, int dh) {
        int w = this.mPolicy.getNonDecorDisplayWidth(dw, dh, rotation);
        int h = this.mPolicy.getNonDecorDisplayHeight(dw, dh, rotation);
        int longSize = w;
        int shortSize = h;
        if (longSize < shortSize) {
            longSize = shortSize;
            shortSize = longSize;
        }
        return Configuration.reduceScreenLayout(curLayout, (int) (longSize / density), (int) (shortSize / density));
    }

    private void computeSizeRangesAndScreenLayout(DisplayInfo displayInfo, boolean rotated, int dw, int dh, float density, Configuration outConfig) {
        int unrotDw;
        int unrotDh;
        if (rotated) {
            unrotDw = dh;
            unrotDh = dw;
        } else {
            unrotDw = dw;
            unrotDh = dh;
        }
        displayInfo.smallestNominalAppWidth = 1073741824;
        displayInfo.smallestNominalAppHeight = 1073741824;
        displayInfo.largestNominalAppWidth = 0;
        displayInfo.largestNominalAppHeight = 0;
        adjustDisplaySizeRanges(displayInfo, 0, unrotDw, unrotDh);
        adjustDisplaySizeRanges(displayInfo, 1, unrotDh, unrotDw);
        adjustDisplaySizeRanges(displayInfo, 2, unrotDw, unrotDh);
        adjustDisplaySizeRanges(displayInfo, 3, unrotDh, unrotDw);
        int sl = Configuration.resetScreenLayout(outConfig.screenLayout);
        int sl2 = reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(sl, 0, density, unrotDw, unrotDh), 1, density, unrotDh, unrotDw), 2, density, unrotDw, unrotDh), 3, density, unrotDh, unrotDw);
        outConfig.smallestScreenWidthDp = (int) (displayInfo.smallestNominalAppWidth / density);
        outConfig.screenLayout = sl2;
    }

    private int reduceCompatConfigWidthSize(int curSize, int rotation, DisplayMetrics dm, int dw, int dh) {
        dm.noncompatWidthPixels = this.mPolicy.getNonDecorDisplayWidth(dw, dh, rotation);
        dm.noncompatHeightPixels = this.mPolicy.getNonDecorDisplayHeight(dw, dh, rotation);
        float scale = CompatibilityInfo.computeCompatibleScaling(dm, (DisplayMetrics) null);
        int size = (int) (((dm.noncompatWidthPixels / scale) / dm.density) + 0.5f);
        if (curSize == 0 || size < curSize) {
            return size;
        }
        return curSize;
    }

    private int computeCompatSmallestWidth(boolean rotated, DisplayMetrics dm, int dw, int dh) {
        int unrotDw;
        int unrotDh;
        this.mTmpDisplayMetrics.setTo(dm);
        DisplayMetrics tmpDm = this.mTmpDisplayMetrics;
        if (rotated) {
            unrotDw = dh;
            unrotDh = dw;
        } else {
            unrotDw = dw;
            unrotDh = dh;
        }
        int sw = reduceCompatConfigWidthSize(0, 0, tmpDm, unrotDw, unrotDh);
        return reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(sw, 1, tmpDm, unrotDh, unrotDw), 2, tmpDm, unrotDw, unrotDh), 3, tmpDm, unrotDh, unrotDw);
    }

    boolean computeScreenConfigurationLocked(Configuration config) {
        if (!this.mDisplayReady) {
            return false;
        }
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        boolean rotated = this.mRotation == 1 || this.mRotation == 3;
        int realdw = rotated ? displayContent.mBaseDisplayHeight : displayContent.mBaseDisplayWidth;
        int realdh = rotated ? displayContent.mBaseDisplayWidth : displayContent.mBaseDisplayHeight;
        int dw = realdw;
        int dh = realdh;
        if (this.mAltOrientation) {
            if (realdw > realdh) {
                int maxw = (int) (realdh / 1.3f);
                if (maxw < realdw) {
                    dw = maxw;
                }
            } else {
                int maxh = (int) (realdw / 1.3f);
                if (maxh < realdh) {
                    dh = maxh;
                }
            }
        }
        if (config != null) {
            config.orientation = dw <= dh ? 1 : 2;
        }
        int appWidth = this.mPolicy.getNonDecorDisplayWidth(dw, dh, this.mRotation);
        int appHeight = this.mPolicy.getNonDecorDisplayHeight(dw, dh, this.mRotation);
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        synchronized (displayContent.mDisplaySizeLock) {
            displayInfo.rotation = this.mRotation;
            displayInfo.logicalWidth = dw;
            displayInfo.logicalHeight = dh;
            displayInfo.logicalDensityDpi = displayContent.mBaseDisplayDensity;
            displayInfo.appWidth = appWidth;
            displayInfo.appHeight = appHeight;
            displayInfo.getLogicalMetrics(this.mRealDisplayMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, (IBinder) null);
            displayInfo.getAppMetrics(this.mDisplayMetrics);
            this.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(displayContent.getDisplayId(), displayInfo);
        }
        DisplayMetrics dm = this.mDisplayMetrics;
        this.mCompatibleScreenScale = CompatibilityInfo.computeCompatibleScaling(dm, this.mCompatDisplayMetrics);
        if (config != null) {
            config.screenWidthDp = (int) (this.mPolicy.getConfigDisplayWidth(dw, dh, this.mRotation) / dm.density);
            config.screenHeightDp = (int) (this.mPolicy.getConfigDisplayHeight(dw, dh, this.mRotation) / dm.density);
            computeSizeRangesAndScreenLayout(displayInfo, rotated, dw, dh, dm.density, config);
            config.compatScreenWidthDp = (int) (config.screenWidthDp / this.mCompatibleScreenScale);
            config.compatScreenHeightDp = (int) (config.screenHeightDp / this.mCompatibleScreenScale);
            config.compatSmallestScreenWidthDp = computeCompatSmallestWidth(rotated, dm, dw, dh);
            config.densityDpi = displayContent.mBaseDisplayDensity;
            config.touchscreen = 1;
            config.keyboard = 1;
            config.navigation = 1;
            int keyboardPresence = 0;
            int navigationPresence = 0;
            InputDevice[] devices = this.mInputManager.getInputDevices();
            for (InputDevice device : devices) {
                if (!device.isVirtual()) {
                    int sources = device.getSources();
                    int presenceFlag = device.isExternal() ? 2 : 1;
                    if (this.mIsTouchDevice) {
                        if ((sources & 4098) == 4098) {
                            config.touchscreen = 3;
                        }
                    } else {
                        config.touchscreen = 1;
                    }
                    if ((65540 & sources) == 65540) {
                        config.navigation = 3;
                        navigationPresence |= presenceFlag;
                    } else if ((sources & 513) == 513 && config.navigation == 1) {
                        config.navigation = 2;
                        navigationPresence |= presenceFlag;
                    }
                    if (device.getKeyboardType() == 2) {
                        config.keyboard = 2;
                        keyboardPresence |= presenceFlag;
                    }
                }
            }
            if (config.navigation == 1 && this.mHasPermanentDpad) {
                config.navigation = 2;
                navigationPresence |= 1;
            }
            boolean hardKeyboardAvailable = config.keyboard != 1;
            if (hardKeyboardAvailable != this.mHardKeyboardAvailable) {
                this.mHardKeyboardAvailable = hardKeyboardAvailable;
                this.mH.removeMessages(22);
                this.mH.sendEmptyMessage(22);
            }
            if (this.mShowImeWithHardKeyboard) {
                config.keyboard = 1;
            }
            config.keyboardHidden = 1;
            config.hardKeyboardHidden = 1;
            config.navigationHidden = 1;
            this.mPolicy.adjustConfigurationLw(config, keyboardPresence, navigationPresence);
        }
        return true;
    }

    public boolean isHardKeyboardAvailable() {
        boolean z;
        synchronized (this.mWindowMap) {
            z = this.mHardKeyboardAvailable;
        }
        return z;
    }

    public void updateShowImeWithHardKeyboard() {
        boolean showImeWithHardKeyboard = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "show_ime_with_hard_keyboard", 0, this.mCurrentUserId) == 1;
        synchronized (this.mWindowMap) {
            if (this.mShowImeWithHardKeyboard != showImeWithHardKeyboard) {
                this.mShowImeWithHardKeyboard = showImeWithHardKeyboard;
                this.mH.sendEmptyMessage(18);
            }
        }
    }

    public void setOnHardKeyboardStatusChangeListener(OnHardKeyboardStatusChangeListener listener) {
        synchronized (this.mWindowMap) {
            this.mHardKeyboardStatusChangeListener = listener;
        }
    }

    void notifyHardKeyboardStatusChange() {
        OnHardKeyboardStatusChangeListener listener;
        boolean available;
        synchronized (this.mWindowMap) {
            listener = this.mHardKeyboardStatusChangeListener;
            available = this.mHardKeyboardAvailable;
        }
        if (listener != null) {
            listener.onHardKeyboardStatusChange(available);
        }
    }

    IBinder prepareDragSurface(IWindow window, SurfaceSession session, int flags, int width, int height, Surface outSurface) throws Throwable {
        IBinder token;
        Binder.getCallingPid();
        long origId = Binder.clearCallingIdentity();
        IBinder token2 = null;
        try {
            try {
                synchronized (this.mWindowMap) {
                    try {
                        if (this.mDragState == null) {
                            DisplayContent displayContent = getDefaultDisplayContentLocked();
                            Display display = displayContent.getDisplay();
                            SurfaceControl surface = new SurfaceControl(session, "drag surface", width, height, -3, 4);
                            surface.setLayerStack(display.getLayerStack());
                            outSurface.copyFrom(surface);
                            IBinder winBinder = window.asBinder();
                            token = new Binder();
                            try {
                                this.mDragState = new DragState(this, token, surface, 0, winBinder);
                                DragState dragState = this.mDragState;
                                token2 = new Binder();
                                dragState.mToken = token2;
                                this.mH.removeMessages(20, winBinder);
                                Message msg = this.mH.obtainMessage(20, winBinder);
                                this.mH.sendMessageDelayed(msg, 5000L);
                                token = token2;
                            } catch (Surface.OutOfResourcesException e) {
                                e = e;
                                Slog.e(TAG, "Can't allocate drag surface w=" + width + " h=" + height, e);
                                if (this.mDragState != null) {
                                    this.mDragState.reset();
                                    this.mDragState = null;
                                }
                            }
                        } else {
                            Slog.w(TAG, "Drag already in progress");
                            token = null;
                        }
                    } catch (Surface.OutOfResourcesException e2) {
                        e = e2;
                        token = token2;
                    } catch (Throwable th) {
                        th = th;
                        try {
                            throw th;
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(origId);
                            throw th;
                        }
                    }
                    Binder.restoreCallingIdentity(origId);
                    return token;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    public void pauseKeyDispatching(IBinder _token) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "pauseKeyDispatching()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            WindowToken token = this.mTokenMap.get(_token);
            if (token != null) {
                this.mInputMonitor.pauseDispatchingLw(token);
            }
        }
    }

    public void resumeKeyDispatching(IBinder _token) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "resumeKeyDispatching()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            WindowToken token = this.mTokenMap.get(_token);
            if (token != null) {
                this.mInputMonitor.resumeDispatchingLw(token);
            }
        }
    }

    public void setEventDispatching(boolean enabled) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setEventDispatching()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            this.mEventDispatchingEnabled = enabled;
            if (this.mDisplayEnabled) {
                this.mInputMonitor.setEventDispatchingLw(enabled);
            }
        }
    }

    private WindowState getFocusedWindow() {
        WindowState focusedWindowLocked;
        synchronized (this.mWindowMap) {
            focusedWindowLocked = getFocusedWindowLocked();
        }
        return focusedWindowLocked;
    }

    private WindowState getFocusedWindowLocked() {
        return this.mCurrentFocus;
    }

    public boolean detectSafeMode() {
        boolean z = true;
        if (!this.mInputMonitor.waitForInputDevicesReady(1000L)) {
            Slog.w(TAG, "Devices still not ready after waiting 1000 milliseconds before attempting to detect safe mode.");
        }
        int menuState = this.mInputManager.getKeyCodeState(-1, -256, 82);
        int sState = this.mInputManager.getKeyCodeState(-1, -256, 47);
        int dpadState = this.mInputManager.getKeyCodeState(-1, 513, 23);
        int trackballState = this.mInputManager.getScanCodeState(-1, 65540, InputManagerService.BTN_MOUSE);
        int volumeDownState = this.mInputManager.getKeyCodeState(-1, -256, 25);
        if (menuState <= 0 && sState <= 0 && dpadState <= 0 && trackballState <= 0 && volumeDownState <= 0) {
            z = false;
        }
        this.mSafeMode = z;
        try {
            if (SystemProperties.getInt(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, 0) != 0) {
                this.mSafeMode = true;
                SystemProperties.set(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, "");
            }
        } catch (IllegalArgumentException e) {
        }
        int dcha_state = BenesseExtension.getDchaState();
        if (dcha_state != 0 && Build.TYPE.equals("user")) {
            this.mSafeMode = false;
        }
        SystemProperties.set("com.benesse.dcha_state", String.valueOf(dcha_state));
        if (this.mSafeMode) {
            Log.i(TAG, "SAFE MODE ENABLED (menu=" + menuState + " s=" + sState + " dpad=" + dpadState + " trackball=" + trackballState + ")");
        } else {
            Log.i(TAG, "SAFE MODE not enabled");
        }
        this.mPolicy.setSafeMode(this.mSafeMode);
        return this.mSafeMode;
    }

    public void displayReady() {
        displayReady(0);
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDefaultDisplayContentLocked();
            readForcedDisplaySizeAndDensityLocked(displayContent);
            this.mDisplayReady = true;
        }
        try {
            this.mActivityManager.updateConfiguration((Configuration) null);
        } catch (RemoteException e) {
        }
        synchronized (this.mWindowMap) {
            this.mIsTouchDevice = this.mContext.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
            configureDisplayPolicyLocked(getDefaultDisplayContentLocked());
        }
        try {
            this.mActivityManager.updateConfiguration((Configuration) null);
        } catch (RemoteException e2) {
        }
    }

    private void displayReady(int displayId) {
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null) {
                this.mAnimator.addDisplayLocked(displayId);
                synchronized (displayContent.mDisplaySizeLock) {
                    DisplayInfo displayInfo = displayContent.getDisplayInfo();
                    DisplayInfo newDisplayInfo = this.mDisplayManagerInternal.getDisplayInfo(displayId);
                    if (newDisplayInfo != null) {
                        displayInfo.copyFrom(newDisplayInfo);
                    }
                    displayContent.mInitialDisplayWidth = displayInfo.logicalWidth;
                    displayContent.mInitialDisplayHeight = displayInfo.logicalHeight;
                    displayContent.mInitialDisplayDensity = displayInfo.logicalDensityDpi;
                    displayContent.mBaseDisplayWidth = displayContent.mInitialDisplayWidth;
                    displayContent.mBaseDisplayHeight = displayContent.mInitialDisplayHeight;
                    displayContent.mBaseDisplayDensity = displayContent.mInitialDisplayDensity;
                    displayContent.mBaseDisplayRect.set(0, 0, displayContent.mBaseDisplayWidth, displayContent.mBaseDisplayHeight);
                }
            }
        }
    }

    public void systemReady() {
        this.mPolicy.systemReady();
    }

    final class H extends Handler {
        public static final int ADD_STARTING = 5;
        public static final int ALL_WINDOWS_DRAWN = 33;
        public static final int APP_FREEZE_TIMEOUT = 17;
        public static final int APP_TRANSITION_TIMEOUT = 13;
        public static final int BOOT_TIMEOUT = 23;
        public static final int CHECK_IF_BOOT_ANIMATION_FINISHED = 37;
        public static final int CLIENT_FREEZE_TIMEOUT = 30;
        public static final int DO_ANIMATION_CALLBACK = 26;
        public static final int DO_DISPLAY_ADDED = 27;
        public static final int DO_DISPLAY_CHANGED = 29;
        public static final int DO_DISPLAY_REMOVED = 28;
        public static final int DO_TRAVERSAL = 4;
        public static final int DRAG_END_TIMEOUT = 21;
        public static final int DRAG_START_TIMEOUT = 20;
        public static final int ENABLE_SCREEN = 16;
        public static final int FINISHED_STARTING = 7;
        public static final int FORCE_GC = 15;
        public static final int NEW_ANIMATOR_SCALE = 34;
        public static final int NOTIFY_ACTIVITY_DRAWN = 32;
        public static final int PERSIST_ANIMATION_SCALE = 14;
        public static final int REMOVE_STARTING = 6;
        public static final int REPORT_APPLICATION_TOKEN_DRAWN = 9;
        public static final int REPORT_APPLICATION_TOKEN_WINDOWS = 8;
        public static final int REPORT_FOCUS_CHANGE = 2;
        public static final int REPORT_HARD_KEYBOARD_STATUS_CHANGE = 22;
        public static final int REPORT_LOSING_FOCUS = 3;
        public static final int REPORT_WINDOWS_CHANGE = 19;
        public static final int RESET_ANR_MESSAGE = 38;
        public static final int SEND_NEW_CONFIGURATION = 18;
        public static final int SHOW_CIRCULAR_DISPLAY_MASK = 35;
        public static final int SHOW_EMULATOR_DISPLAY_OVERLAY = 36;
        public static final int SHOW_STRICT_MODE_VIOLATION = 25;
        public static final int TAP_OUTSIDE_STACK = 31;
        public static final int WAITING_FOR_DRAWN_TIMEOUT = 24;
        public static final int WINDOW_FREEZE_TIMEOUT = 11;

        H() {
        }

        @Override
        public void handleMessage(Message msg) {
            boolean bootAnimationComplete;
            Runnable callback;
            Runnable callback2;
            ArrayList<WindowState> losers;
            switch (msg.what) {
                case 2:
                    AccessibilityController accessibilityController = null;
                    synchronized (WindowManagerService.this.mWindowMap) {
                        if (WindowManagerService.this.mAccessibilityController != null && WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayId() == 0) {
                            accessibilityController = WindowManagerService.this.mAccessibilityController;
                        }
                        WindowState lastFocus = WindowManagerService.this.mLastFocus;
                        WindowState newFocus = WindowManagerService.this.mCurrentFocus;
                        if (lastFocus != newFocus) {
                            WindowManagerService.this.mLastFocus = newFocus;
                            if (newFocus != null && lastFocus != null && !newFocus.isDisplayedLw()) {
                                WindowManagerService.this.mLosingFocus.add(lastFocus);
                                lastFocus = null;
                            }
                            if (accessibilityController != null) {
                                accessibilityController.onWindowFocusChangedNotLocked();
                            }
                            if (newFocus != null) {
                                newFocus.reportFocusChangedSerialized(true, WindowManagerService.this.mInTouchMode);
                                WindowManagerService.this.notifyFocusChanged();
                            }
                            if (lastFocus != null) {
                                lastFocus.reportFocusChangedSerialized(false, WindowManagerService.this.mInTouchMode);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                case 3:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        losers = WindowManagerService.this.mLosingFocus;
                        WindowManagerService.this.mLosingFocus = new ArrayList<>();
                        break;
                    }
                    int N = losers.size();
                    for (int i = 0; i < N; i++) {
                        losers.get(i).reportFocusChangedSerialized(false, WindowManagerService.this.mInTouchMode);
                    }
                    return;
                case 4:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        WindowManagerService.this.mTraversalScheduled = false;
                        WindowManagerService.this.performLayoutAndPlaceSurfacesLocked();
                        break;
                    }
                    return;
                case 5:
                    AppWindowToken wtoken = (AppWindowToken) msg.obj;
                    StartingData sd = wtoken.startingData;
                    if (sd != null) {
                        View view = null;
                        try {
                            view = WindowManagerService.this.mPolicy.addStartingWindow(wtoken.token, sd.pkg, sd.theme, sd.compatInfo, sd.nonLocalizedLabel, sd.labelRes, sd.icon, sd.logo, sd.windowFlags);
                            break;
                        } catch (Exception e) {
                            Slog.w(WindowManagerService.TAG, "Exception when adding starting window", e);
                        }
                        if (view != null) {
                            boolean abort = false;
                            synchronized (WindowManagerService.this.mWindowMap) {
                                if (wtoken.removed || wtoken.startingData == null) {
                                    if (wtoken.startingWindow != null) {
                                        wtoken.startingWindow = null;
                                        wtoken.startingData = null;
                                        abort = true;
                                    }
                                } else {
                                    wtoken.startingView = view;
                                }
                                break;
                            }
                            if (abort) {
                                try {
                                    WindowManagerService.this.mPolicy.removeStartingWindow(wtoken.token, view);
                                    return;
                                } catch (Exception e2) {
                                    Slog.w(WindowManagerService.TAG, "Exception when removing starting window", e2);
                                    return;
                                }
                            }
                            return;
                        }
                        return;
                    }
                    return;
                case 6:
                    AppWindowToken wtoken2 = (AppWindowToken) msg.obj;
                    IBinder token = null;
                    View view2 = null;
                    synchronized (WindowManagerService.this.mWindowMap) {
                        if (wtoken2.startingWindow != null) {
                            view2 = wtoken2.startingView;
                            token = wtoken2.token;
                            wtoken2.startingData = null;
                            wtoken2.startingView = null;
                            wtoken2.startingWindow = null;
                            wtoken2.startingDisplayed = false;
                        }
                        break;
                    }
                    if (view2 != null) {
                        try {
                            WindowManagerService.this.mPolicy.removeStartingWindow(token, view2);
                            return;
                        } catch (Exception e3) {
                            Slog.w(WindowManagerService.TAG, "Exception when removing starting window", e3);
                            return;
                        }
                    }
                    return;
                case 7:
                    while (true) {
                        synchronized (WindowManagerService.this.mWindowMap) {
                            int N2 = WindowManagerService.this.mFinishedStarting.size();
                            if (N2 > 0) {
                                AppWindowToken wtoken3 = WindowManagerService.this.mFinishedStarting.remove(N2 - 1);
                                if (wtoken3.startingWindow != null) {
                                    View view3 = wtoken3.startingView;
                                    IBinder token2 = wtoken3.token;
                                    wtoken3.startingData = null;
                                    wtoken3.startingView = null;
                                    wtoken3.startingWindow = null;
                                    wtoken3.startingDisplayed = false;
                                    break;
                                }
                            } else {
                                return;
                            }
                        }
                    }
                    break;
                case 8:
                    AppWindowToken wtoken4 = (AppWindowToken) msg.obj;
                    boolean nowVisible = msg.arg1 != 0;
                    if (msg.arg2 != 0) {
                    }
                    try {
                        if (nowVisible) {
                            wtoken4.appToken.windowsVisible();
                        } else {
                            wtoken4.appToken.windowsGone();
                        }
                        return;
                    } catch (RemoteException e4) {
                        return;
                    }
                case 9:
                    try {
                        ((AppWindowToken) msg.obj).appToken.windowsDrawn();
                        return;
                    } catch (RemoteException e5) {
                        return;
                    }
                case 10:
                case 12:
                case 31:
                default:
                    return;
                case 11:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        Slog.w(WindowManagerService.TAG, "Window freeze timeout expired.");
                        WindowList windows = WindowManagerService.this.getDefaultWindowListLocked();
                        int i2 = windows.size();
                        while (i2 > 0) {
                            i2--;
                            WindowState w = windows.get(i2);
                            if (w.mOrientationChanging) {
                                w.mOrientationChanging = false;
                                w.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - WindowManagerService.this.mDisplayFreezeTime);
                                Slog.w(WindowManagerService.TAG, "Force clearing orientation change: " + w);
                            }
                        }
                        WindowManagerService.this.performLayoutAndPlaceSurfacesLocked();
                        break;
                    }
                    return;
                case 13:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        if (WindowManagerService.this.mAppTransition.isTransitionSet()) {
                            WindowManagerService.this.mAppTransition.setTimeout();
                            WindowManagerService.this.performLayoutAndPlaceSurfacesLocked();
                        }
                        break;
                    }
                    return;
                case 14:
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                    return;
                case 15:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        if (WindowManagerService.this.mAnimator.mAnimating || WindowManagerService.this.mAnimationScheduled) {
                            sendEmptyMessageDelayed(15, 2000L);
                        } else if (!WindowManagerService.this.mDisplayFrozen) {
                            Runtime.getRuntime().gc();
                        }
                    }
                    return;
                case 16:
                    WindowManagerService.this.performEnableScreen();
                    return;
                case 17:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        Slog.w(WindowManagerService.TAG, "App freeze timeout expired.");
                        int numStacks = WindowManagerService.this.mStackIdToStack.size();
                        for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                            TaskStack stack = WindowManagerService.this.mStackIdToStack.valueAt(stackNdx);
                            ArrayList<Task> tasks = stack.getTasks();
                            for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                                AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
                                for (int tokenNdx = tokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                                    AppWindowToken tok = tokens.get(tokenNdx);
                                    if (tok.mAppAnimator.freezingScreen) {
                                        Slog.w(WindowManagerService.TAG, "Force clearing freeze: " + tok);
                                        WindowManagerService.this.unsetAppFreezingScreenLocked(tok, true, true);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    return;
                case SEND_NEW_CONFIGURATION:
                    removeMessages(18);
                    WindowManagerService.this.sendNewConfiguration();
                    return;
                case REPORT_WINDOWS_CHANGE:
                    if (WindowManagerService.this.mWindowsChanged) {
                        synchronized (WindowManagerService.this.mWindowMap) {
                            WindowManagerService.this.mWindowsChanged = false;
                            break;
                        }
                        WindowManagerService.this.notifyWindowsChanged();
                        return;
                    }
                    return;
                case DRAG_START_TIMEOUT:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        if (WindowManagerService.this.mDragState != null) {
                            WindowManagerService.this.mDragState.unregister();
                            WindowManagerService.this.mInputMonitor.updateInputWindowsLw(true);
                            WindowManagerService.this.mDragState.reset();
                            WindowManagerService.this.mDragState = null;
                        }
                        break;
                    }
                    return;
                case DRAG_END_TIMEOUT:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        if (WindowManagerService.this.mDragState != null) {
                            WindowManagerService.this.mDragState.mDragResult = false;
                            WindowManagerService.this.mDragState.endDragLw();
                        }
                        break;
                    }
                    return;
                case REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                    WindowManagerService.this.notifyHardKeyboardStatusChange();
                    return;
                case BOOT_TIMEOUT:
                    WindowManagerService.this.performBootTimeout();
                    return;
                case WAITING_FOR_DRAWN_TIMEOUT:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        Slog.w(WindowManagerService.TAG, "Timeout waiting for drawn: undrawn=" + WindowManagerService.this.mWaitingForDrawn);
                        WindowManagerService.this.mWaitingForDrawn.clear();
                        callback2 = WindowManagerService.this.mWaitingForDrawnCallback;
                        WindowManagerService.this.mWaitingForDrawnCallback = null;
                        break;
                    }
                    if (callback2 != null) {
                        callback2.run();
                        return;
                    }
                    return;
                case SHOW_STRICT_MODE_VIOLATION:
                    WindowManagerService.this.showStrictModeViolation(msg.arg1, msg.arg2);
                    return;
                case DO_ANIMATION_CALLBACK:
                    try {
                        ((IRemoteCallback) msg.obj).sendResult((Bundle) null);
                        return;
                    } catch (RemoteException e6) {
                        return;
                    }
                case DO_DISPLAY_ADDED:
                    WindowManagerService.this.handleDisplayAdded(msg.arg1);
                    return;
                case DO_DISPLAY_REMOVED:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        WindowManagerService.this.handleDisplayRemovedLocked(msg.arg1);
                        break;
                    }
                    return;
                case 29:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        WindowManagerService.this.handleDisplayChangedLocked(msg.arg1);
                        break;
                    }
                    return;
                case 30:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        if (WindowManagerService.this.mClientFreezingScreen) {
                            WindowManagerService.this.mClientFreezingScreen = false;
                            WindowManagerService.this.mLastFinishedFreezeSource = "client-timeout";
                            WindowManagerService.this.stopFreezingDisplayLocked();
                        }
                        break;
                    }
                    return;
                case 32:
                    try {
                        WindowManagerService.this.mActivityManager.notifyActivityDrawn((IBinder) msg.obj);
                        return;
                    } catch (RemoteException e7) {
                        return;
                    }
                case 33:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        callback = WindowManagerService.this.mWaitingForDrawnCallback;
                        WindowManagerService.this.mWaitingForDrawnCallback = null;
                        break;
                    }
                    if (callback != null) {
                        callback.run();
                    }
                    break;
                case 34:
                    break;
                case 35:
                    WindowManagerService.this.showCircularMask(msg.arg1 == 1);
                    return;
                case 36:
                    WindowManagerService.this.showEmulatorDisplayOverlay();
                    return;
                case 37:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        bootAnimationComplete = WindowManagerService.this.checkBootAnimationCompleteLocked();
                        break;
                    }
                    if (bootAnimationComplete) {
                        WindowManagerService.this.performEnableScreen();
                        return;
                    }
                    return;
                case 38:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        WindowManagerService.this.mLastANRState = null;
                        break;
                    }
                    return;
            }
            float scale = WindowManagerService.this.getCurrentAnimatorScale();
            ValueAnimator.setDurationScale(scale);
            Session session = (Session) msg.obj;
            if (session != null) {
                try {
                    session.mCallback.onAnimatorScaleChanged(scale);
                    return;
                } catch (RemoteException e8) {
                    return;
                }
            }
            ArrayList<IWindowSessionCallback> callbacks = new ArrayList<>();
            synchronized (WindowManagerService.this.mWindowMap) {
                for (int i3 = 0; i3 < WindowManagerService.this.mSessions.size(); i3++) {
                    callbacks.add(WindowManagerService.this.mSessions.valueAt(i3).mCallback);
                }
            }
            for (int i4 = 0; i4 < callbacks.size(); i4++) {
                try {
                    callbacks.get(i4).onAnimatorScaleChanged(scale);
                } catch (RemoteException e9) {
                }
            }
        }
    }

    public IWindowSession openSession(IWindowSessionCallback callback, IInputMethodClient client, IInputContext inputContext) {
        if (client == null) {
            throw new IllegalArgumentException("null client");
        }
        if (inputContext == null) {
            throw new IllegalArgumentException("null inputContext");
        }
        Session session = new Session(this, callback, client, inputContext);
        return session;
    }

    public boolean inputMethodClientHasFocus(IInputMethodClient client) {
        WindowState imFocus;
        synchronized (this.mWindowMap) {
            int idx = findDesiredInputMethodWindowIndexLocked(false);
            if (idx > 0 && (imFocus = getDefaultWindowListLocked().get(idx - 1)) != null) {
                if (imFocus.mAttrs.type == 3 && imFocus.mAppToken != null) {
                    int i = 0;
                    while (true) {
                        if (i >= imFocus.mAppToken.windows.size()) {
                            break;
                        }
                        WindowState w = imFocus.mAppToken.windows.get(i);
                        if (w == imFocus) {
                            i++;
                        } else {
                            Log.i(TAG, "Switching to real app window: " + w);
                            imFocus = w;
                            break;
                        }
                    }
                }
                if (imFocus.mSession.mClient != null && imFocus.mSession.mClient.asBinder() == client.asBinder()) {
                    return true;
                }
            }
            return (this.mCurrentFocus == null || this.mCurrentFocus.mSession.mClient == null || this.mCurrentFocus.mSession.mClient.asBinder() != client.asBinder()) ? false : true;
        }
    }

    public void getInitialDisplaySize(int displayId, Point size) {
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                synchronized (displayContent.mDisplaySizeLock) {
                    size.x = displayContent.mInitialDisplayWidth;
                    size.y = displayContent.mInitialDisplayHeight;
                }
            }
        }
    }

    public void getBaseDisplaySize(int displayId, Point size) {
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                synchronized (displayContent.mDisplaySizeLock) {
                    size.x = displayContent.mBaseDisplayWidth;
                    size.y = displayContent.mBaseDisplayHeight;
                }
            }
        }
    }

    public void setForcedDisplaySize(int displayId, int width, int height) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (displayId != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                DisplayContent displayContent = getDisplayContentLocked(displayId);
                if (displayContent != null) {
                    int width2 = Math.min(Math.max(width, BOOT_ANIMATION_POLL_INTERVAL), displayContent.mInitialDisplayWidth * 2);
                    int height2 = Math.min(Math.max(height, BOOT_ANIMATION_POLL_INTERVAL), displayContent.mInitialDisplayHeight * 2);
                    setForcedDisplaySizeLocked(displayContent, width2, height2);
                    Settings.Global.putString(this.mContext.getContentResolver(), "display_size_forced", width2 + "," + height2);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void readForcedDisplaySizeAndDensityLocked(DisplayContent displayContent) {
        int pos;
        String sizeStr = Settings.Global.getString(this.mContext.getContentResolver(), "display_size_forced");
        if (sizeStr == null || sizeStr.length() == 0) {
            sizeStr = SystemProperties.get(SIZE_OVERRIDE, (String) null);
        }
        if (sizeStr != null && sizeStr.length() > 0 && (pos = sizeStr.indexOf(44)) > 0 && sizeStr.lastIndexOf(44) == pos) {
            try {
                int width = Integer.parseInt(sizeStr.substring(0, pos));
                int height = Integer.parseInt(sizeStr.substring(pos + 1));
                synchronized (displayContent.mDisplaySizeLock) {
                    if (displayContent.mBaseDisplayWidth != width || displayContent.mBaseDisplayHeight != height) {
                        Slog.i(TAG, "FORCED DISPLAY SIZE: " + width + "x" + height);
                        displayContent.mBaseDisplayWidth = width;
                        displayContent.mBaseDisplayHeight = height;
                    }
                }
            } catch (NumberFormatException e) {
            }
        }
        String densityStr = Settings.Global.getString(this.mContext.getContentResolver(), "display_density_forced");
        if (densityStr == null || densityStr.length() == 0) {
            densityStr = SystemProperties.get(DENSITY_OVERRIDE, (String) null);
        }
        if (densityStr != null && densityStr.length() > 0) {
            try {
                int density = Integer.parseInt(densityStr);
                synchronized (displayContent.mDisplaySizeLock) {
                    if (displayContent.mBaseDisplayDensity != density) {
                        Slog.i(TAG, "FORCED DISPLAY DENSITY: " + density);
                        displayContent.mBaseDisplayDensity = density;
                    }
                }
            } catch (NumberFormatException e2) {
            }
        }
    }

    private void setForcedDisplaySizeLocked(DisplayContent displayContent, int width, int height) {
        Slog.i(TAG, "Using new display size: " + width + "x" + height);
        synchronized (displayContent.mDisplaySizeLock) {
            displayContent.mBaseDisplayWidth = width;
            displayContent.mBaseDisplayHeight = height;
        }
        reconfigureDisplayLocked(displayContent);
    }

    public void clearForcedDisplaySize(int displayId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (displayId != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                DisplayContent displayContent = getDisplayContentLocked(displayId);
                if (displayContent != null) {
                    setForcedDisplaySizeLocked(displayContent, displayContent.mInitialDisplayWidth, displayContent.mInitialDisplayHeight);
                    Settings.Global.putString(this.mContext.getContentResolver(), "display_size_forced", "");
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getInitialDisplayDensity(int displayId) {
        int i;
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                synchronized (displayContent.mDisplaySizeLock) {
                    i = displayContent.mInitialDisplayDensity;
                }
                return i;
            }
            return -1;
        }
    }

    public int getBaseDisplayDensity(int displayId) {
        int i;
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                synchronized (displayContent.mDisplaySizeLock) {
                    i = displayContent.mBaseDisplayDensity;
                }
                return i;
            }
            return -1;
        }
    }

    public void setForcedDisplayDensity(int displayId, int density) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (displayId != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                DisplayContent displayContent = getDisplayContentLocked(displayId);
                if (displayContent != null) {
                    setForcedDisplayDensityLocked(displayContent, density);
                    Settings.Global.putString(this.mContext.getContentResolver(), "display_density_forced", Integer.toString(density));
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setForcedDisplayDensityLocked(DisplayContent displayContent, int density) {
        Slog.i(TAG, "Using new display density: " + density);
        synchronized (displayContent.mDisplaySizeLock) {
            displayContent.mBaseDisplayDensity = density;
        }
        reconfigureDisplayLocked(displayContent);
    }

    public void clearForcedDisplayDensity(int displayId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (displayId != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                DisplayContent displayContent = getDisplayContentLocked(displayId);
                if (displayContent != null) {
                    setForcedDisplayDensityLocked(displayContent, displayContent.mInitialDisplayDensity);
                    Settings.Global.putString(this.mContext.getContentResolver(), "display_density_forced", "");
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void reconfigureDisplayLocked(DisplayContent displayContent) {
        configureDisplayPolicyLocked(displayContent);
        displayContent.layoutNeeded = true;
        boolean configChanged = updateOrientationFromAppTokensLocked(false);
        this.mTempConfiguration.setToDefaults();
        this.mTempConfiguration.fontScale = this.mCurConfiguration.fontScale;
        if (computeScreenConfigurationLocked(this.mTempConfiguration) && this.mCurConfiguration.diff(this.mTempConfiguration) != 0) {
            configChanged = true;
        }
        if (configChanged) {
            this.mWaitingForConfig = true;
            startFreezingDisplayLocked(false, 0, 0);
            this.mH.sendEmptyMessage(18);
        }
        performLayoutAndPlaceSurfacesLocked();
    }

    private void configureDisplayPolicyLocked(DisplayContent displayContent) {
        this.mPolicy.setInitialDisplaySize(displayContent.getDisplay(), displayContent.mBaseDisplayWidth, displayContent.mBaseDisplayHeight, displayContent.mBaseDisplayDensity);
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        this.mPolicy.setDisplayOverscan(displayContent.getDisplay(), displayInfo.overscanLeft, displayInfo.overscanTop, displayInfo.overscanRight, displayInfo.overscanBottom);
    }

    public void setOverscan(int displayId, int left, int top, int right, int bottom) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                DisplayContent displayContent = getDisplayContentLocked(displayId);
                if (displayContent != null) {
                    setOverscanLocked(displayContent, left, top, right, bottom);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setOverscanLocked(DisplayContent displayContent, int left, int top, int right, int bottom) {
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        synchronized (displayContent.mDisplaySizeLock) {
            displayInfo.overscanLeft = left;
            displayInfo.overscanTop = top;
            displayInfo.overscanRight = right;
            displayInfo.overscanBottom = bottom;
        }
        this.mDisplaySettings.setOverscanLocked(displayInfo.uniqueId, left, top, right, bottom);
        this.mDisplaySettings.writeSettingsLocked();
        reconfigureDisplayLocked(displayContent);
    }

    final WindowState windowForClientLocked(Session session, IWindow client, boolean throwOnError) {
        return windowForClientLocked(session, client.asBinder(), throwOnError);
    }

    final WindowState windowForClientLocked(Session session, IBinder client, boolean throwOnError) {
        WindowState win = this.mWindowMap.get(client);
        if (win == null) {
            RuntimeException ex = new IllegalArgumentException("Requested window " + client + " does not exist");
            if (throwOnError) {
                throw ex;
            }
            Slog.w(TAG, "Failed looking up window", ex);
            return null;
        }
        if (session != null && win.mSession != session) {
            RuntimeException ex2 = new IllegalArgumentException("Requested window " + client + " is in session " + win.mSession + ", not " + session);
            if (throwOnError) {
                throw ex2;
            }
            Slog.w(TAG, "Failed looking up window", ex2);
            return null;
        }
        return win;
    }

    final void rebuildAppWindowListLocked() {
        rebuildAppWindowListLocked(getDefaultDisplayContentLocked());
    }

    private void rebuildAppWindowListLocked(DisplayContent displayContent) {
        WindowList windows = displayContent.getWindowList();
        int NW = windows.size();
        int lastBelow = -1;
        int numRemoved = 0;
        if (this.mRebuildTmp.length < NW) {
            this.mRebuildTmp = new WindowState[NW + 10];
        }
        int i = 0;
        while (i < NW) {
            WindowState w = windows.get(i);
            if (w.mAppToken != null) {
                WindowState win = windows.remove(i);
                win.mRebuilding = true;
                this.mRebuildTmp[numRemoved] = win;
                this.mWindowsChanged = true;
                NW--;
                numRemoved++;
            } else {
                if (lastBelow == i - 1 && (w.mAttrs.type == 2013 || w.mAttrs.type == 2025)) {
                    lastBelow = i;
                }
                i++;
            }
        }
        int lastBelow2 = lastBelow + 1;
        int i2 = lastBelow2;
        ArrayList<TaskStack> stacks = displayContent.getStacks();
        int numStacks = stacks.size();
        for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
            AppTokenList exitingAppTokens = stacks.get(stackNdx).mExitingAppTokens;
            int NT = exitingAppTokens.size();
            for (int j = 0; j < NT; j++) {
                i2 = reAddAppWindowsLocked(displayContent, i2, exitingAppTokens.get(j));
            }
        }
        for (int stackNdx2 = 0; stackNdx2 < numStacks; stackNdx2++) {
            ArrayList<Task> tasks = stacks.get(stackNdx2).getTasks();
            int numTasks = tasks.size();
            for (int taskNdx = 0; taskNdx < numTasks; taskNdx++) {
                AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
                int numTokens = tokens.size();
                for (int tokenNdx = 0; tokenNdx < numTokens; tokenNdx++) {
                    AppWindowToken wtoken = tokens.get(tokenNdx);
                    if (!wtoken.mDeferRemoval) {
                        i2 = reAddAppWindowsLocked(displayContent, i2, wtoken);
                    }
                }
            }
        }
        int i3 = i2 - lastBelow2;
        if (i3 != numRemoved) {
            Slog.w(TAG, "On display=" + displayContent.getDisplayId() + " Rebuild removed " + numRemoved + " windows but added " + i3, new RuntimeException("here").fillInStackTrace());
            for (int i4 = 0; i4 < numRemoved; i4++) {
                WindowState ws = this.mRebuildTmp[i4];
                if (ws.mRebuilding) {
                    StringWriter sw = new StringWriter();
                    FastPrintWriter fastPrintWriter = new FastPrintWriter(sw, false, 1024);
                    ws.dump(fastPrintWriter, "", true);
                    fastPrintWriter.flush();
                    Slog.w(TAG, "This window was lost: " + ws);
                    Slog.w(TAG, sw.toString());
                    ws.mWinAnimator.destroySurfaceLocked();
                }
            }
            Slog.w(TAG, "Current app token list:");
            dumpAppTokensLocked();
            Slog.w(TAG, "Final window list:");
            dumpWindowsLocked();
        }
    }

    private final void assignLayersLocked(WindowList windows) {
        int N = windows.size();
        int curBaseLayer = 0;
        int curLayer = 0;
        boolean anyLayerChanged = false;
        for (int i = 0; i < N; i++) {
            WindowState w = windows.get(i);
            WindowStateAnimator winAnimator = w.mWinAnimator;
            boolean layerChanged = false;
            int oldLayer = w.mLayer;
            if (w.mBaseLayer == curBaseLayer || w.mIsImWindow || (i > 0 && w.mIsWallpaper)) {
                curLayer += 5;
                w.mLayer = curLayer;
            } else {
                curLayer = w.mBaseLayer;
                curBaseLayer = curLayer;
                w.mLayer = curLayer;
            }
            if (w.mLayer != oldLayer) {
                layerChanged = true;
                anyLayerChanged = true;
            }
            AppWindowToken wtoken = w.mAppToken;
            int oldLayer2 = winAnimator.mAnimLayer;
            if (w.mTargetAppToken != null) {
                winAnimator.mAnimLayer = w.mLayer + w.mTargetAppToken.mAppAnimator.animLayerAdjustment;
            } else if (wtoken != null) {
                winAnimator.mAnimLayer = w.mLayer + wtoken.mAppAnimator.animLayerAdjustment;
            } else {
                winAnimator.mAnimLayer = w.mLayer;
            }
            if (w.mIsImWindow) {
                winAnimator.mAnimLayer += this.mInputMethodAnimLayerAdjustment;
            } else if (w.mIsWallpaper) {
                winAnimator.mAnimLayer += this.mWallpaperAnimLayerAdjustment;
            }
            if (winAnimator.mAnimLayer != oldLayer2) {
                layerChanged = true;
                anyLayerChanged = true;
            }
            TaskStack stack = w.getStack();
            if (layerChanged && stack != null && stack.isDimming(winAnimator)) {
                scheduleAnimationLocked();
            }
        }
        if (this.mAccessibilityController != null && anyLayerChanged && windows.get(windows.size() - 1).getDisplayId() == 0) {
            this.mAccessibilityController.onWindowLayersChangedLocked();
        }
    }

    private final void performLayoutAndPlaceSurfacesLocked() {
        int loopCount = 6;
        do {
            this.mTraversalScheduled = false;
            performLayoutAndPlaceSurfacesLockedLoop();
            this.mH.removeMessages(4);
            loopCount--;
            if (!this.mTraversalScheduled) {
                break;
            }
        } while (loopCount > 0);
        this.mInnerFields.mWallpaperActionPending = false;
    }

    private final void performLayoutAndPlaceSurfacesLockedLoop() {
        if (this.mInLayout) {
            Slog.w(TAG, "performLayoutAndPlaceSurfacesLocked called while in layout. Callers=" + Debug.getCallers(3));
            return;
        }
        if (!this.mWaitingForConfig && this.mDisplayReady) {
            Trace.traceBegin(32L, "wmLayout");
            this.mInLayout = true;
            boolean recoveringMemory = false;
            try {
                if (this.mForceRemoves != null) {
                    recoveringMemory = true;
                    for (int i = 0; i < this.mForceRemoves.size(); i++) {
                        WindowState ws = this.mForceRemoves.get(i);
                        Slog.i(TAG, "Force removing: " + ws);
                        removeWindowInnerLocked(ws.mSession, ws);
                    }
                    this.mForceRemoves = null;
                    Slog.w(TAG, "Due to memory failure, waiting a bit for next layout");
                    Object tmp = new Object();
                    synchronized (tmp) {
                        try {
                            tmp.wait(250L);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } catch (RuntimeException e2) {
                Slog.wtf(TAG, "Unhandled exception while force removing for memory", e2);
            }
            try {
                performLayoutAndPlaceSurfacesLockedInner(recoveringMemory);
                this.mInLayout = false;
                if (needsLayout()) {
                    int i2 = this.mLayoutRepeatCount + 1;
                    this.mLayoutRepeatCount = i2;
                    if (i2 < 6) {
                        requestTraversalLocked();
                    } else {
                        Slog.e(TAG, "Performed 6 layouts in a row. Skipping");
                        this.mLayoutRepeatCount = 0;
                    }
                } else {
                    this.mLayoutRepeatCount = 0;
                }
                if (this.mWindowsChanged && !this.mWindowChangeListeners.isEmpty()) {
                    this.mH.removeMessages(19);
                    this.mH.sendEmptyMessage(19);
                }
            } catch (RuntimeException e3) {
                this.mInLayout = false;
                Slog.wtf(TAG, "Unhandled exception while laying out windows", e3);
            }
            Trace.traceEnd(32L);
        }
    }

    private final void performLayoutLockedInner(DisplayContent displayContent, boolean initial, boolean updateInputWindows) {
        if (displayContent.layoutNeeded) {
            displayContent.layoutNeeded = false;
            WindowList windows = displayContent.getWindowList();
            boolean isDefaultDisplay = displayContent.isDefaultDisplay;
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            int dw = displayInfo.logicalWidth;
            int dh = displayInfo.logicalHeight;
            int NFW = this.mFakeWindows.size();
            for (int i = 0; i < NFW; i++) {
                this.mFakeWindows.get(i).layout(dw, dh);
            }
            int N = windows.size();
            WindowStateAnimator universeBackground = null;
            this.mPolicy.beginLayoutLw(isDefaultDisplay, dw, dh, this.mRotation);
            if (isDefaultDisplay) {
                this.mSystemDecorLayer = this.mPolicy.getSystemDecorLayerLw();
                this.mScreenRect.set(0, 0, dw, dh);
            }
            this.mPolicy.getContentRectLw(this.mTmpContentRect);
            displayContent.resize(this.mTmpContentRect);
            int seq = this.mLayoutSeq + 1;
            if (seq < 0) {
                seq = 0;
            }
            this.mLayoutSeq = seq;
            boolean behindDream = false;
            int topAttached = -1;
            for (int i2 = N - 1; i2 >= 0; i2--) {
                WindowState win = windows.get(i2);
                boolean gone = (behindDream && this.mPolicy.canBeForceHidden(win, win.mAttrs)) || win.isGoneForLayoutLw();
                if (!gone || !win.mHaveFrame || win.mLayoutNeeded || (((win.isConfigChanged() || win.setInsetsChanged()) && ((win.mAttrs.privateFlags & 1024) != 0 || (win.mAppToken != null && win.mAppToken.layoutConfigChanges))) || win.mAttrs.type == 2025)) {
                    if (!win.mLayoutAttached) {
                        if (initial) {
                            win.mContentChanged = false;
                        }
                        if (win.mAttrs.type == 2023) {
                            behindDream = true;
                        }
                        win.mLayoutNeeded = false;
                        win.prelayout();
                        this.mPolicy.layoutWindowLw(win, (WindowManagerPolicy.WindowState) null);
                        win.mLayoutSeq = seq;
                    } else if (topAttached < 0) {
                        topAttached = i2;
                    }
                }
                if (win.mViewVisibility == 0 && win.mAttrs.type == 2025 && universeBackground == null) {
                    universeBackground = win.mWinAnimator;
                }
            }
            if (this.mAnimator.mUniverseBackground != universeBackground) {
                this.mFocusMayChange = true;
                this.mAnimator.mUniverseBackground = universeBackground;
            }
            boolean attachedBehindDream = false;
            for (int i3 = topAttached; i3 >= 0; i3--) {
                WindowState win2 = windows.get(i3);
                if (win2.mLayoutAttached) {
                    if ((!attachedBehindDream || !this.mPolicy.canBeForceHidden(win2, win2.mAttrs)) && ((win2.mViewVisibility != 8 && win2.mRelayoutCalled) || !win2.mHaveFrame || win2.mLayoutNeeded)) {
                        if (initial) {
                            win2.mContentChanged = false;
                        }
                        win2.mLayoutNeeded = false;
                        win2.prelayout();
                        this.mPolicy.layoutWindowLw(win2, win2.mAttachedWindow);
                        win2.mLayoutSeq = seq;
                    }
                } else if (win2.mAttrs.type == 2023) {
                    attachedBehindDream = behindDream;
                }
            }
            this.mInputMonitor.setUpdateInputWindowsNeededLw();
            if (updateInputWindows) {
                this.mInputMonitor.updateInputWindowsLw(false);
            }
            this.mPolicy.finishLayoutLw();
        }
    }

    void makeWindowFreezingScreenIfNeededLocked(WindowState w) {
        if (!okToDisplay()) {
            w.mOrientationChanging = true;
            w.mLastFreezeDuration = 0;
            this.mInnerFields.mOrientationChangeComplete = false;
            if (!this.mWindowsFreezingScreen) {
                this.mWindowsFreezingScreen = true;
                this.mH.removeMessages(11);
                this.mH.sendEmptyMessageDelayed(11, 2000L);
            }
        }
    }

    public int handleAppTransitionReadyLocked(WindowList windows) {
        AppWindowToken appWindowToken;
        AppWindowToken upperWallpaperAppToken;
        Animation anim;
        WindowState win;
        AppWindowToken wtoken;
        WindowState ws;
        int changes = 0;
        int NN = this.mOpeningApps.size();
        boolean goodToGo = true;
        if (!this.mDisplayFrozen && !this.mAppTransition.isTimeout()) {
            for (int i = 0; i < NN && goodToGo; i++) {
                AppWindowToken wtoken2 = this.mOpeningApps.valueAt(i);
                if (!wtoken2.allDrawn && !wtoken2.startingDisplayed && !wtoken2.startingMoved) {
                    goodToGo = false;
                }
            }
        }
        if (goodToGo) {
            int transit = this.mAppTransition.getAppTransition();
            if (this.mSkipAppTransitionAnimation) {
                transit = -1;
            }
            this.mAppTransition.goodToGo();
            this.mStartingIconInTransition = false;
            this.mSkipAppTransitionAnimation = false;
            this.mH.removeMessages(13);
            rebuildAppWindowListLocked();
            WindowState oldWallpaper = (this.mWallpaperTarget == null || !this.mWallpaperTarget.mWinAnimator.isAnimating() || this.mWallpaperTarget.mWinAnimator.isDummyAnimation()) ? this.mWallpaperTarget : null;
            this.mInnerFields.mWallpaperMayChange = false;
            WindowManager.LayoutParams animLp = null;
            int bestAnimLayer = -1;
            boolean fullscreenAnim = false;
            boolean voiceInteraction = false;
            boolean openingAppHasWallpaper = false;
            boolean closingAppHasWallpaper = false;
            if (this.mLowerWallpaperTarget == null) {
                upperWallpaperAppToken = null;
                appWindowToken = null;
            } else {
                appWindowToken = this.mLowerWallpaperTarget.mAppToken;
                upperWallpaperAppToken = this.mUpperWallpaperTarget.mAppToken;
            }
            int NC = this.mClosingApps.size();
            int NN2 = NC + this.mOpeningApps.size();
            for (int i2 = 0; i2 < NN2; i2++) {
                if (i2 < NC) {
                    wtoken = this.mClosingApps.valueAt(i2);
                    if (wtoken == appWindowToken || wtoken == upperWallpaperAppToken) {
                        closingAppHasWallpaper = true;
                    }
                } else {
                    wtoken = this.mOpeningApps.valueAt(i2 - NC);
                    if (wtoken == appWindowToken || wtoken == upperWallpaperAppToken) {
                        openingAppHasWallpaper = true;
                    }
                }
                voiceInteraction |= wtoken.voiceInteraction;
                if (wtoken.appFullscreen) {
                    WindowState ws2 = wtoken.findMainWindow();
                    if (ws2 != null) {
                        animLp = ws2.mAttrs;
                        bestAnimLayer = ws2.mLayer;
                        fullscreenAnim = true;
                    }
                } else if (!fullscreenAnim && (ws = wtoken.findMainWindow()) != null && ws.mLayer > bestAnimLayer) {
                    animLp = ws.mAttrs;
                    bestAnimLayer = ws.mLayer;
                }
            }
            this.mAnimateWallpaperWithTarget = false;
            if (closingAppHasWallpaper && openingAppHasWallpaper) {
                switch (transit) {
                    case 6:
                    case 8:
                    case 10:
                        transit = 14;
                        break;
                    case 7:
                    case 9:
                    case 11:
                        transit = 15;
                        break;
                }
            } else if (oldWallpaper != null && !this.mOpeningApps.isEmpty() && !this.mOpeningApps.contains(oldWallpaper.mAppToken)) {
                transit = 12;
            } else if (this.mWallpaperTarget != null && this.mWallpaperTarget.isVisibleLw()) {
                transit = 13;
            } else {
                this.mAnimateWallpaperWithTarget = true;
            }
            if (!this.mPolicy.allowAppAnimationsLw()) {
                animLp = null;
            }
            AppWindowToken topOpeningApp = null;
            AppWindowToken topClosingApp = null;
            int topOpeningLayer = 0;
            int topClosingLayer = 0;
            if (transit == 17 && (win = findFocusedWindowLocked(getDefaultDisplayContentLocked())) != null) {
                AppWindowToken wtoken3 = win.mAppToken;
                AppWindowAnimator appAnimator = wtoken3.mAppAnimator;
                appAnimator.clearThumbnail();
                appAnimator.animation = null;
                updateTokenInPlaceLocked(wtoken3, transit);
                wtoken3.updateReportedVisibilityLocked();
                appAnimator.mAllAppWinAnimators.clear();
                int N = wtoken3.allAppWindows.size();
                for (int j = 0; j < N; j++) {
                    appAnimator.mAllAppWinAnimators.add(wtoken3.allAppWindows.get(j).mWinAnimator);
                }
                this.mAnimator.mAnimating |= appAnimator.showAllWindowsLocked();
            }
            int NN3 = this.mOpeningApps.size();
            for (int i3 = 0; i3 < NN3; i3++) {
                AppWindowToken wtoken4 = this.mOpeningApps.valueAt(i3);
                AppWindowAnimator appAnimator2 = wtoken4.mAppAnimator;
                appAnimator2.clearThumbnail();
                appAnimator2.animation = null;
                wtoken4.inPendingTransaction = false;
                setTokenVisibilityLocked(wtoken4, animLp, true, transit, false, voiceInteraction);
                wtoken4.updateReportedVisibilityLocked();
                wtoken4.waitingToShow = false;
                appAnimator2.mAllAppWinAnimators.clear();
                int N2 = wtoken4.allAppWindows.size();
                for (int j2 = 0; j2 < N2; j2++) {
                    appAnimator2.mAllAppWinAnimators.add(wtoken4.allAppWindows.get(j2).mWinAnimator);
                }
                this.mAnimator.mAnimating |= appAnimator2.showAllWindowsLocked();
                if (animLp != null) {
                    int layer = -1;
                    for (int j3 = 0; j3 < wtoken4.windows.size(); j3++) {
                        WindowState win2 = wtoken4.windows.get(j3);
                        if (win2.mWinAnimator.mAnimLayer > layer) {
                            layer = win2.mWinAnimator.mAnimLayer;
                        }
                    }
                    if (topOpeningApp == null || layer > topOpeningLayer) {
                        topOpeningApp = wtoken4;
                        topOpeningLayer = layer;
                    }
                }
            }
            int NN4 = this.mClosingApps.size();
            for (int i4 = 0; i4 < NN4; i4++) {
                AppWindowToken wtoken5 = this.mClosingApps.valueAt(i4);
                AppWindowAnimator appAnimator3 = wtoken5.mAppAnimator;
                appAnimator3.clearThumbnail();
                appAnimator3.animation = null;
                wtoken5.inPendingTransaction = false;
                setTokenVisibilityLocked(wtoken5, animLp, false, transit, false, voiceInteraction);
                wtoken5.updateReportedVisibilityLocked();
                wtoken5.waitingToHide = false;
                wtoken5.allDrawn = true;
                wtoken5.deferClearAllDrawn = false;
                if (wtoken5.startingWindow != null && !wtoken5.startingWindow.mExiting) {
                    scheduleRemoveStartingWindowLocked(wtoken5);
                }
                if (animLp != null) {
                    int layer2 = -1;
                    for (int j4 = 0; j4 < wtoken5.windows.size(); j4++) {
                        WindowState win3 = wtoken5.windows.get(j4);
                        if (win3.mWinAnimator.mAnimLayer > layer2) {
                            layer2 = win3.mWinAnimator.mAnimLayer;
                        }
                    }
                    if (topClosingApp == null || layer2 > topClosingLayer) {
                        topClosingApp = wtoken5;
                        topClosingLayer = layer2;
                    }
                }
            }
            AppWindowAnimator openingAppAnimator = topOpeningApp == null ? null : topOpeningApp.mAppAnimator;
            if (topClosingApp != null) {
                AppWindowAnimator appWindowAnimator = topClosingApp.mAppAnimator;
            }
            Bitmap nextAppTransitionThumbnail = this.mAppTransition.getNextAppTransitionThumbnail();
            if (nextAppTransitionThumbnail != null && openingAppAnimator != null && openingAppAnimator.animation != null && nextAppTransitionThumbnail.getConfig() != Bitmap.Config.ALPHA_8) {
                Rect dirty = new Rect(0, 0, nextAppTransitionThumbnail.getWidth(), nextAppTransitionThumbnail.getHeight());
                try {
                    DisplayContent displayContent = getDefaultDisplayContentLocked();
                    Display display = displayContent.getDisplay();
                    DisplayInfo displayInfo = displayContent.getDisplayInfo();
                    SurfaceControl surfaceControl = new SurfaceControl(this.mFxSession, "thumbnail anim", dirty.width(), dirty.height(), -3, 4);
                    surfaceControl.setLayerStack(display.getLayerStack());
                    Surface drawSurface = new Surface();
                    drawSurface.copyFrom(surfaceControl);
                    Canvas c = drawSurface.lockCanvas(dirty);
                    c.drawBitmap(nextAppTransitionThumbnail, 0.0f, 0.0f, (Paint) null);
                    drawSurface.unlockCanvasAndPost(c);
                    drawSurface.release();
                    if (this.mAppTransition.isNextThumbnailTransitionAspectScaled()) {
                        anim = this.mAppTransition.createThumbnailAspectScaleAnimationLocked(displayInfo.appWidth, displayInfo.appHeight, displayInfo.logicalWidth, transit);
                        openingAppAnimator.thumbnailForceAboveLayer = Math.max(topOpeningLayer, topClosingLayer);
                        openingAppAnimator.deferThumbnailDestruction = !this.mAppTransition.isNextThumbnailTransitionScaleUp();
                    } else {
                        anim = this.mAppTransition.createThumbnailScaleAnimationLocked(displayInfo.appWidth, displayInfo.appHeight, transit);
                    }
                    anim.restrictDuration(WALLPAPER_TIMEOUT_RECOVERY);
                    anim.scaleCurrentDuration(getTransitionAnimationScaleLocked());
                    openingAppAnimator.thumbnail = surfaceControl;
                    openingAppAnimator.thumbnailLayer = topOpeningLayer;
                    openingAppAnimator.thumbnailAnimation = anim;
                    openingAppAnimator.thumbnailX = this.mAppTransition.getStartingX();
                    openingAppAnimator.thumbnailY = this.mAppTransition.getStartingY();
                } catch (Surface.OutOfResourcesException e) {
                    Slog.e(TAG, "Can't allocate thumbnail/Canvas surface w=" + dirty.width() + " h=" + dirty.height(), e);
                    openingAppAnimator.clearThumbnail();
                }
            }
            this.mAppTransition.postAnimationCallback();
            this.mAppTransition.clear();
            this.mOpeningApps.clear();
            this.mClosingApps.clear();
            changes = 0 | 3;
            getDefaultDisplayContentLocked().layoutNeeded = true;
            if (windows == getDefaultWindowListLocked() && !moveInputMethodWindowsIfNeededLocked(true)) {
                assignLayersLocked(windows);
            }
            updateFocusedWindowLocked(2, true);
            this.mFocusMayChange = false;
            notifyActivityDrawnForKeyguard();
        }
        return changes;
    }

    private int handleAnimatingStoppedAndTransitionLocked() {
        this.mAppTransition.setIdle();
        ArrayList<TaskStack> stacks = getDefaultDisplayContentLocked().getStacks();
        for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
            ArrayList<Task> tasks = stacks.get(stackNdx).getTasks();
            for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
                for (int tokenNdx = tokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                    tokens.get(tokenNdx).sendingToBottom = false;
                }
            }
        }
        rebuildAppWindowListLocked();
        int changes = 0 | 1;
        moveInputMethodWindowsIfNeededLocked(true);
        this.mInnerFields.mWallpaperMayChange = true;
        this.mFocusMayChange = true;
        return changes;
    }

    private void updateResizingWindows(WindowState w) {
        WindowStateAnimator winAnimator = w.mWinAnimator;
        if (w.mHasSurface && w.mLayoutSeq == this.mLayoutSeq) {
            w.setInsetsChanged();
            boolean configChanged = w.isConfigChanged();
            w.mLastFrame.set(w.mFrame);
            if (w.mContentInsetsChanged || w.mVisibleInsetsChanged || winAnimator.mSurfaceResized || configChanged) {
                w.mLastOverscanInsets.set(w.mOverscanInsets);
                w.mLastContentInsets.set(w.mContentInsets);
                w.mLastVisibleInsets.set(w.mVisibleInsets);
                w.mLastStableInsets.set(w.mStableInsets);
                makeWindowFreezingScreenIfNeededLocked(w);
                if (w.mOrientationChanging) {
                    winAnimator.mDrawState = 1;
                    if (w.mAppToken != null) {
                        w.mAppToken.allDrawn = false;
                        w.mAppToken.deferClearAllDrawn = false;
                    }
                }
                if (!this.mResizingWindows.contains(w)) {
                    this.mResizingWindows.add(w);
                    return;
                }
                return;
            }
            if (w.mOrientationChanging && w.isDrawnLw()) {
                w.mOrientationChanging = false;
                w.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
            }
        }
    }

    private void handleNotObscuredLocked(WindowState w, long currentTime, int innerDw, int innerDh) {
        WindowManager.LayoutParams attrs = w.mAttrs;
        int attrFlags = attrs.flags;
        boolean canBeSeen = w.isDisplayedLw();
        boolean opaqueDrawn = canBeSeen && w.isOpaqueDrawn();
        if (opaqueDrawn && w.isFullscreen(innerDw, innerDh)) {
            this.mInnerFields.mObscured = true;
        }
        if (w.mHasSurface) {
            if ((attrFlags & 128) != 0) {
                this.mInnerFields.mHoldScreen = w.mSession;
            }
            if (!this.mInnerFields.mSyswin && w.mAttrs.screenBrightness >= 0.0f && this.mInnerFields.mScreenBrightness < 0.0f) {
                this.mInnerFields.mScreenBrightness = w.mAttrs.screenBrightness;
            }
            if (!this.mInnerFields.mSyswin && w.mAttrs.buttonBrightness >= 0.0f && this.mInnerFields.mButtonBrightness < 0.0f) {
                this.mInnerFields.mButtonBrightness = w.mAttrs.buttonBrightness;
            }
            if (!this.mInnerFields.mSyswin && w.mAttrs.userActivityTimeout >= 0 && this.mInnerFields.mUserActivityTimeout < 0) {
                this.mInnerFields.mUserActivityTimeout = w.mAttrs.userActivityTimeout;
            }
            int type = attrs.type;
            if (canBeSeen && (type == 2008 || type == 2010 || (attrs.privateFlags & 1024) != 0)) {
                this.mInnerFields.mSyswin = true;
            }
            if (canBeSeen) {
                DisplayContent displayContent = w.getDisplayContent();
                if (displayContent != null && displayContent.isDefaultDisplay) {
                    if (type == 2023 || (attrs.privateFlags & 1024) != 0) {
                        this.mInnerFields.mObscureApplicationContentOnSecondaryDisplays = true;
                    }
                    this.mInnerFields.mDisplayHasContent = true;
                } else if (displayContent != null && (!this.mInnerFields.mObscureApplicationContentOnSecondaryDisplays || (this.mInnerFields.mObscured && type == 2009))) {
                    this.mInnerFields.mDisplayHasContent = true;
                }
                if (this.mInnerFields.mPreferredRefreshRate == 0.0f && w.mAttrs.preferredRefreshRate != 0.0f) {
                    this.mInnerFields.mPreferredRefreshRate = w.mAttrs.preferredRefreshRate;
                }
            }
        }
    }

    private void handleFlagDimBehind(WindowState w) {
        WindowManager.LayoutParams attrs = w.mAttrs;
        if ((attrs.flags & 2) != 0 && w.isDisplayedLw() && !w.mExiting) {
            WindowStateAnimator winAnimator = w.mWinAnimator;
            TaskStack stack = w.getStack();
            if (stack != null) {
                stack.setDimmingTag();
                if (!stack.isDimming(winAnimator)) {
                    stack.startDimmingIfNeeded(winAnimator);
                }
            }
        }
    }

    private void updateAllDrawnLocked(DisplayContent displayContent) {
        int numInteresting;
        ArrayList<TaskStack> stacks = displayContent.getStacks();
        for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
            ArrayList<Task> tasks = stacks.get(stackNdx).getTasks();
            for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
                for (int tokenNdx = tokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                    AppWindowToken wtoken = tokens.get(tokenNdx);
                    if (!wtoken.allDrawn && (numInteresting = wtoken.numInterestingWindows) > 0 && wtoken.numDrawnWindows >= numInteresting) {
                        wtoken.allDrawn = true;
                        displayContent.layoutNeeded = true;
                        this.mH.obtainMessage(32, wtoken.token).sendToTarget();
                    }
                }
            }
        }
    }

    private final void performLayoutAndPlaceSurfacesLockedInner(boolean recoveringMemory) {
        long currentTime = SystemClock.uptimeMillis();
        boolean updateInputWindowsNeeded = false;
        if (this.mFocusMayChange) {
            this.mFocusMayChange = false;
            updateInputWindowsNeeded = updateFocusedWindowLocked(3, false);
        }
        int numDisplays = this.mDisplayContents.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
            for (int i = displayContent.mExitingTokens.size() - 1; i >= 0; i--) {
                displayContent.mExitingTokens.get(i).hasVisible = false;
            }
        }
        for (int stackNdx = this.mStackIdToStack.size() - 1; stackNdx >= 0; stackNdx--) {
            AppTokenList exitingAppTokens = this.mStackIdToStack.valueAt(stackNdx).mExitingAppTokens;
            for (int tokenNdx = exitingAppTokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                exitingAppTokens.get(tokenNdx).hasVisible = false;
            }
        }
        this.mInnerFields.mHoldScreen = null;
        this.mInnerFields.mScreenBrightness = -1.0f;
        this.mInnerFields.mButtonBrightness = -1.0f;
        this.mInnerFields.mUserActivityTimeout = -1L;
        this.mInnerFields.mObscureApplicationContentOnSecondaryDisplays = false;
        this.mTransactionSequence++;
        DisplayContent defaultDisplay = getDefaultDisplayContentLocked();
        DisplayInfo defaultInfo = defaultDisplay.getDisplayInfo();
        int defaultDw = defaultInfo.logicalWidth;
        int defaultDh = defaultInfo.logicalHeight;
        SurfaceControl.openTransaction();
        try {
            if (this.mWatermark != null) {
                this.mWatermark.positionSurface(defaultDw, defaultDh);
            }
            if (this.mStrictModeFlash != null) {
                this.mStrictModeFlash.positionSurface(defaultDw, defaultDh);
            }
            if (this.mCircularDisplayMask != null) {
                this.mCircularDisplayMask.positionSurface(defaultDw, defaultDh, this.mRotation);
            }
            if (this.mEmulatorDisplayOverlay != null) {
                this.mEmulatorDisplayOverlay.positionSurface(defaultDw, defaultDh, this.mRotation);
            }
            boolean focusDisplayed = false;
            for (int displayNdx2 = 0; displayNdx2 < numDisplays; displayNdx2++) {
                DisplayContent displayContent2 = this.mDisplayContents.valueAt(displayNdx2);
                boolean updateAllDrawn = false;
                WindowList windows = displayContent2.getWindowList();
                DisplayInfo displayInfo = displayContent2.getDisplayInfo();
                int displayId = displayContent2.getDisplayId();
                int dw = displayInfo.logicalWidth;
                int dh = displayInfo.logicalHeight;
                int innerDw = displayInfo.appWidth;
                int innerDh = displayInfo.appHeight;
                boolean isDefaultDisplay = displayId == 0;
                this.mInnerFields.mDisplayHasContent = false;
                this.mInnerFields.mPreferredRefreshRate = 0.0f;
                int repeats = 0;
                while (true) {
                    repeats++;
                    if (repeats > 6) {
                        Slog.w(TAG, "Animation repeat aborted after too many iterations");
                        displayContent2.layoutNeeded = false;
                        break;
                    }
                    debugLayoutRepeats("On entry to LockedInner", displayContent2.pendingLayoutChanges);
                    if ((displayContent2.pendingLayoutChanges & 4) != 0 && (adjustWallpaperWindowsLocked() & 2) != 0) {
                        assignLayersLocked(windows);
                        displayContent2.layoutNeeded = true;
                    }
                    if (isDefaultDisplay && (displayContent2.pendingLayoutChanges & 2) != 0 && updateOrientationFromAppTokensLocked(true)) {
                        displayContent2.layoutNeeded = true;
                        this.mH.sendEmptyMessage(18);
                    }
                    if ((displayContent2.pendingLayoutChanges & 1) != 0) {
                        displayContent2.layoutNeeded = true;
                    }
                    if (repeats < 4) {
                        performLayoutLockedInner(displayContent2, repeats == 1, false);
                    } else {
                        Slog.w(TAG, "Layout repeat skipped after too many iterations");
                    }
                    displayContent2.pendingLayoutChanges = 0;
                    debugLayoutRepeats("loop number " + this.mLayoutRepeatCount, displayContent2.pendingLayoutChanges);
                    if (isDefaultDisplay) {
                        this.mPolicy.beginPostLayoutPolicyLw(dw, dh);
                        for (int i2 = windows.size() - 1; i2 >= 0; i2--) {
                            WindowState w = windows.get(i2);
                            if (w.mHasSurface) {
                                this.mPolicy.applyPostLayoutPolicyLw(w, w.mAttrs, w.mAttachedWindow);
                            }
                        }
                        displayContent2.pendingLayoutChanges |= this.mPolicy.finishPostLayoutPolicyLw();
                        debugLayoutRepeats("after finishPostLayoutPolicyLw", displayContent2.pendingLayoutChanges);
                    }
                    if (displayContent2.pendingLayoutChanges == 0) {
                        break;
                    }
                }
                this.mInnerFields.mObscured = false;
                this.mInnerFields.mSyswin = false;
                displayContent2.resetDimming();
                boolean someoneLosingFocus = !this.mLosingFocus.isEmpty();
                for (int i3 = windows.size() - 1; i3 >= 0; i3--) {
                    WindowState w2 = windows.get(i3);
                    TaskStack stack = w2.getStack();
                    if (stack != null || w2.getAttrs().type == 2030) {
                        boolean obscuredChanged = w2.mObscured != this.mInnerFields.mObscured;
                        w2.mObscured = this.mInnerFields.mObscured;
                        if (!this.mInnerFields.mObscured) {
                            handleNotObscuredLocked(w2, currentTime, innerDw, innerDh);
                        }
                        if (stack != null && !stack.testDimmingTag()) {
                            handleFlagDimBehind(w2);
                        }
                        if (isDefaultDisplay && obscuredChanged && this.mWallpaperTarget == w2 && w2.isVisibleLw()) {
                            updateWallpaperVisibilityLocked();
                        }
                        WindowStateAnimator winAnimator = w2.mWinAnimator;
                        if (w2.mHasSurface && w2.shouldAnimateMove()) {
                            Animation a = AnimationUtils.loadAnimation(this.mContext, R.anim.recents_fade_out);
                            winAnimator.setAnimation(a);
                            winAnimator.mAnimDw = w2.mLastFrame.left - w2.mFrame.left;
                            winAnimator.mAnimDh = w2.mLastFrame.top - w2.mFrame.top;
                            if (this.mAccessibilityController != null && displayId == 0) {
                                this.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
                            }
                            try {
                                w2.mClient.moved(w2.mFrame.left, w2.mFrame.top);
                            } catch (RemoteException e) {
                            }
                        }
                        w2.mContentChanged = false;
                        if (w2.mHasSurface && !w2.isHiddenFromUserLocked()) {
                            boolean committed = winAnimator.commitFinishDrawingLocked(currentTime);
                            if (isDefaultDisplay && committed) {
                                if (w2.mAttrs.type == 2023) {
                                    displayContent2.pendingLayoutChanges |= 1;
                                    debugLayoutRepeats("dream and commitFinishDrawingLocked true", displayContent2.pendingLayoutChanges);
                                }
                                if ((w2.mAttrs.flags & 1048576) != 0) {
                                    this.mInnerFields.mWallpaperMayChange = true;
                                    displayContent2.pendingLayoutChanges |= 4;
                                    debugLayoutRepeats("wallpaper and commitFinishDrawingLocked true", displayContent2.pendingLayoutChanges);
                                }
                            }
                            winAnimator.setSurfaceBoundariesLocked(recoveringMemory);
                            AppWindowToken atoken = w2.mAppToken;
                            if (atoken != null && (!atoken.allDrawn || atoken.mAppAnimator.freezingScreen)) {
                                if (atoken.lastTransactionSequence != this.mTransactionSequence) {
                                    atoken.lastTransactionSequence = this.mTransactionSequence;
                                    atoken.numDrawnWindows = 0;
                                    atoken.numInterestingWindows = 0;
                                    atoken.startingDisplayed = false;
                                }
                                if ((w2.isOnScreenIgnoringKeyguard() || winAnimator.mAttrType == 1) && !w2.mExiting && !w2.mDestroying) {
                                    if (w2 != atoken.startingWindow) {
                                        if (!atoken.mAppAnimator.freezingScreen || !w2.mAppFreezing) {
                                            atoken.numInterestingWindows++;
                                            if (w2.isDrawnLw()) {
                                                atoken.numDrawnWindows++;
                                                updateAllDrawn = true;
                                            }
                                        }
                                    } else if (w2.isDrawnLw()) {
                                        atoken.startingDisplayed = true;
                                    }
                                }
                            }
                        }
                        if (isDefaultDisplay && someoneLosingFocus && w2 == this.mCurrentFocus && w2.isDisplayedLw()) {
                            focusDisplayed = true;
                        }
                        updateResizingWindows(w2);
                    }
                }
                this.mDisplayManagerInternal.setDisplayProperties(displayId, this.mInnerFields.mDisplayHasContent, this.mInnerFields.mPreferredRefreshRate, true);
                getDisplayContentLocked(displayId).stopDimmingIfNeeded();
                if (updateAllDrawn) {
                    updateAllDrawnLocked(displayContent2);
                }
            }
            if (focusDisplayed) {
                this.mH.sendEmptyMessage(3);
            }
            this.mDisplayManagerInternal.performTraversalInTransactionFromWindowManager();
        } catch (RuntimeException e2) {
            Slog.wtf(TAG, "Unhandled exception in Window Manager", e2);
        } finally {
            SurfaceControl.closeTransaction();
        }
        WindowList defaultWindows = defaultDisplay.getWindowList();
        if (this.mAppTransition.isReady()) {
            defaultDisplay.pendingLayoutChanges |= handleAppTransitionReadyLocked(defaultWindows);
            debugLayoutRepeats("after handleAppTransitionReadyLocked", defaultDisplay.pendingLayoutChanges);
        }
        if (!this.mAnimator.mAnimating && this.mAppTransition.isRunning()) {
            defaultDisplay.pendingLayoutChanges |= handleAnimatingStoppedAndTransitionLocked();
            debugLayoutRepeats("after handleAnimStopAndXitionLock", defaultDisplay.pendingLayoutChanges);
        }
        if (this.mInnerFields.mWallpaperForceHidingChanged && defaultDisplay.pendingLayoutChanges == 0 && !this.mAppTransition.isReady()) {
            defaultDisplay.pendingLayoutChanges |= 1;
            debugLayoutRepeats("after animateAwayWallpaperLocked", defaultDisplay.pendingLayoutChanges);
        }
        this.mInnerFields.mWallpaperForceHidingChanged = false;
        if (this.mInnerFields.mWallpaperMayChange) {
            defaultDisplay.pendingLayoutChanges |= 4;
            debugLayoutRepeats("WallpaperMayChange", defaultDisplay.pendingLayoutChanges);
        }
        if (this.mFocusMayChange) {
            this.mFocusMayChange = false;
            if (updateFocusedWindowLocked(2, false)) {
                updateInputWindowsNeeded = true;
                defaultDisplay.pendingLayoutChanges |= 8;
            }
        }
        if (needsLayout()) {
            defaultDisplay.pendingLayoutChanges |= 1;
            debugLayoutRepeats("mLayoutNeeded", defaultDisplay.pendingLayoutChanges);
        }
        for (int i4 = this.mResizingWindows.size() - 1; i4 >= 0; i4--) {
            WindowState win = this.mResizingWindows.get(i4);
            if (!win.mAppFreezing) {
                win.reportResized();
                this.mResizingWindows.remove(i4);
            }
        }
        if (this.mInnerFields.mOrientationChangeComplete) {
            if (this.mWindowsFreezingScreen) {
                this.mWindowsFreezingScreen = false;
                this.mLastFinishedFreezeSource = this.mInnerFields.mLastWindowFreezeSource;
                this.mH.removeMessages(11);
            }
            stopFreezingDisplayLocked();
        }
        boolean wallpaperDestroyed = false;
        int i5 = this.mDestroySurface.size();
        if (i5 > 0) {
            do {
                i5--;
                WindowState win2 = this.mDestroySurface.get(i5);
                win2.mDestroying = false;
                if (this.mInputMethodWindow == win2) {
                    this.mInputMethodWindow = null;
                }
                if (win2 == this.mWallpaperTarget) {
                    wallpaperDestroyed = true;
                }
                win2.mWinAnimator.destroySurfaceLocked();
            } while (i5 > 0);
            this.mDestroySurface.clear();
        }
        for (int displayNdx3 = 0; displayNdx3 < numDisplays; displayNdx3++) {
            ArrayList<WindowToken> exitingTokens = this.mDisplayContents.valueAt(displayNdx3).mExitingTokens;
            for (int i6 = exitingTokens.size() - 1; i6 >= 0; i6--) {
                WindowToken token = exitingTokens.get(i6);
                if (!token.hasVisible) {
                    exitingTokens.remove(i6);
                    if (token.windowType == 2013) {
                        this.mWallpaperTokens.remove(token);
                    }
                }
            }
        }
        for (int stackNdx2 = this.mStackIdToStack.size() - 1; stackNdx2 >= 0; stackNdx2--) {
            AppTokenList exitingAppTokens2 = this.mStackIdToStack.valueAt(stackNdx2).mExitingAppTokens;
            for (int i7 = exitingAppTokens2.size() - 1; i7 >= 0; i7--) {
                AppWindowToken token2 = exitingAppTokens2.get(i7);
                if (!token2.hasVisible && !this.mClosingApps.contains(token2) && (!token2.mDeferRemoval || token2.allAppWindows.isEmpty())) {
                    token2.mAppAnimator.clearAnimation();
                    token2.mAppAnimator.animating = false;
                    removeAppFromTaskLocked(token2);
                    exitingAppTokens2.remove(i7);
                    Task task = this.mTaskIdToTask.get(token2.groupId);
                    if (task != null && task.mDeferRemoval && task.mAppTokens.isEmpty()) {
                        removeTaskLocked(task);
                    }
                }
            }
        }
        if (!this.mAnimator.mAnimating && this.mRelayoutWhileAnimating.size() > 0) {
            for (int j = this.mRelayoutWhileAnimating.size() - 1; j >= 0; j--) {
                try {
                    this.mRelayoutWhileAnimating.get(j).mClient.doneAnimating();
                } catch (RemoteException e3) {
                }
            }
            this.mRelayoutWhileAnimating.clear();
        }
        if (wallpaperDestroyed) {
            defaultDisplay.pendingLayoutChanges |= 4;
            defaultDisplay.layoutNeeded = true;
        }
        for (int displayNdx4 = 0; displayNdx4 < numDisplays; displayNdx4++) {
            DisplayContent displayContent3 = this.mDisplayContents.valueAt(displayNdx4);
            if (displayContent3.pendingLayoutChanges != 0) {
                displayContent3.layoutNeeded = true;
            }
        }
        this.mInputMonitor.updateInputWindowsLw(true);
        setHoldScreenLocked(this.mInnerFields.mHoldScreen);
        if (!this.mDisplayFrozen) {
            if (this.mInnerFields.mScreenBrightness >= 0.0f && this.mInnerFields.mScreenBrightness <= 1.0f) {
                this.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(toBrightnessOverride(this.mInnerFields.mScreenBrightness));
            } else {
                this.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(-1);
            }
            if (this.mInnerFields.mButtonBrightness >= 0.0f && this.mInnerFields.mButtonBrightness <= 1.0f) {
                this.mPowerManagerInternal.setButtonBrightnessOverrideFromWindowManager(toBrightnessOverride(this.mInnerFields.mButtonBrightness));
            } else {
                this.mPowerManagerInternal.setButtonBrightnessOverrideFromWindowManager(-1);
            }
            this.mPowerManagerInternal.setUserActivityTimeoutOverrideFromWindowManager(this.mInnerFields.mUserActivityTimeout);
        }
        if (this.mTurnOnScreen) {
            if (this.mAllowTheaterModeWakeFromLayout || Settings.Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 0) {
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
            }
            this.mTurnOnScreen = false;
        }
        if (this.mInnerFields.mUpdateRotation) {
            if (!updateRotationUncheckedLocked(false)) {
                this.mInnerFields.mUpdateRotation = false;
            } else {
                this.mH.sendEmptyMessage(18);
            }
        }
        if (this.mWaitingForDrawnCallback != null || (this.mInnerFields.mOrientationChangeComplete && !defaultDisplay.layoutNeeded && !this.mInnerFields.mUpdateRotation)) {
            checkDrawnWindowsLocked();
        }
        int N = this.mPendingRemove.size();
        if (N > 0) {
            if (this.mPendingRemoveTmp.length < N) {
                this.mPendingRemoveTmp = new WindowState[N + 10];
            }
            this.mPendingRemove.toArray(this.mPendingRemoveTmp);
            this.mPendingRemove.clear();
            DisplayContentList displayList = new DisplayContentList();
            for (int i8 = 0; i8 < N; i8++) {
                WindowState w3 = this.mPendingRemoveTmp[i8];
                removeWindowInnerLocked(w3.mSession, w3);
                DisplayContent displayContent4 = w3.getDisplayContent();
                if (displayContent4 != null && !displayList.contains(displayContent4)) {
                    displayList.add(displayContent4);
                }
            }
            for (DisplayContent displayContent5 : displayList) {
                assignLayersLocked(displayContent5.getWindowList());
                displayContent5.layoutNeeded = true;
            }
        }
        for (int displayNdx5 = this.mDisplayContents.size() - 1; displayNdx5 >= 0; displayNdx5--) {
            this.mDisplayContents.valueAt(displayNdx5).checkForDeferredActions();
        }
        if (updateInputWindowsNeeded) {
            this.mInputMonitor.updateInputWindowsLw(false);
        }
        setFocusedStackFrame();
        enableScreenIfNeededLocked();
        scheduleAnimationLocked();
    }

    private int toBrightnessOverride(float value) {
        return (int) (255.0f * value);
    }

    void checkDrawnWindowsLocked() {
        if (!this.mWaitingForDrawn.isEmpty() && this.mWaitingForDrawnCallback != null) {
            for (int j = this.mWaitingForDrawn.size() - 1; j >= 0; j--) {
                WindowState win = this.mWaitingForDrawn.get(j);
                if (win.mRemoved || !win.mHasSurface) {
                    this.mWaitingForDrawn.remove(win);
                } else if (win.hasDrawnLw()) {
                    this.mWaitingForDrawn.remove(win);
                }
            }
            if (this.mWaitingForDrawn.isEmpty()) {
                this.mH.removeMessages(24);
                this.mH.sendEmptyMessage(33);
            }
        }
    }

    void setHoldScreenLocked(Session newHoldScreen) {
        boolean hold = newHoldScreen != null;
        if (hold && this.mHoldingScreenOn != newHoldScreen) {
            this.mHoldingScreenWakeLock.setWorkSource(new WorkSource(newHoldScreen.mUid));
        }
        this.mHoldingScreenOn = newHoldScreen;
        boolean state = this.mHoldingScreenWakeLock.isHeld();
        if (hold != state) {
            if (hold) {
                this.mHoldingScreenWakeLock.acquire();
                this.mPolicy.keepScreenOnStartedLw();
            } else {
                this.mPolicy.keepScreenOnStoppedLw();
                this.mHoldingScreenWakeLock.release();
            }
        }
    }

    void requestTraversal() {
        synchronized (this.mWindowMap) {
            requestTraversalLocked();
        }
    }

    void requestTraversalLocked() {
        if (!this.mTraversalScheduled) {
            this.mTraversalScheduled = true;
            this.mH.sendEmptyMessage(4);
        }
    }

    void scheduleAnimationLocked() {
        if (!this.mAnimationScheduled) {
            this.mAnimationScheduled = true;
            this.mChoreographer.postCallback(1, this.mAnimator.mAnimationRunnable, null);
        }
    }

    private boolean needsLayout() {
        int numDisplays = this.mDisplayContents.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
            if (displayContent.layoutNeeded) {
                return true;
            }
        }
        return false;
    }

    boolean copyAnimToLayoutParamsLocked() {
        boolean doRequest = false;
        int bulkUpdateParams = this.mAnimator.mBulkUpdateParams;
        if ((bulkUpdateParams & 1) != 0) {
            this.mInnerFields.mUpdateRotation = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 2) != 0) {
            this.mInnerFields.mWallpaperMayChange = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 4) != 0) {
            this.mInnerFields.mWallpaperForceHidingChanged = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 8) == 0) {
            this.mInnerFields.mOrientationChangeComplete = false;
        } else {
            this.mInnerFields.mOrientationChangeComplete = true;
            this.mInnerFields.mLastWindowFreezeSource = this.mAnimator.mLastWindowFreezeSource;
            if (this.mWindowsFreezingScreen) {
                doRequest = true;
            }
        }
        if ((bulkUpdateParams & 16) != 0) {
            this.mTurnOnScreen = true;
        }
        if ((bulkUpdateParams & 32) != 0) {
            this.mInnerFields.mWallpaperActionPending = true;
        }
        return doRequest;
    }

    int adjustAnimationBackground(WindowStateAnimator winAnimator) {
        WindowList windows = winAnimator.mWin.getWindowList();
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowState testWin = windows.get(i);
            if (testWin.mIsWallpaper && testWin.isVisibleNow()) {
                return testWin.mWinAnimator.mAnimLayer;
            }
        }
        return winAnimator.mAnimLayer;
    }

    boolean reclaimSomeSurfaceMemoryLocked(WindowStateAnimator winAnimator, String operation, boolean secure) {
        SurfaceControl surface = winAnimator.mSurfaceControl;
        boolean leakedSurface = false;
        boolean killedApps = false;
        EventLog.writeEvent(EventLogTags.WM_NO_SURFACE_MEMORY, winAnimator.mWin.toString(), Integer.valueOf(winAnimator.mSession.mPid), operation);
        if (this.mForceRemoves == null) {
            this.mForceRemoves = new ArrayList<>();
        }
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            Slog.i(TAG, "Out of memory for surface!  Looking for leaks...");
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                int numWindows = windows.size();
                for (int winNdx = 0; winNdx < numWindows; winNdx++) {
                    WindowState ws = windows.get(winNdx);
                    WindowStateAnimator wsa = ws.mWinAnimator;
                    if (wsa.mSurfaceControl != null) {
                        if (!this.mSessions.contains(wsa.mSession)) {
                            Slog.w(TAG, "LEAKED SURFACE (session doesn't exist): " + ws + " surface=" + wsa.mSurfaceControl + " token=" + ws.mToken + " pid=" + ws.mSession.mPid + " uid=" + ws.mSession.mUid);
                            wsa.mSurfaceControl.destroy();
                            wsa.mSurfaceShown = false;
                            wsa.mSurfaceControl = null;
                            ws.mHasSurface = false;
                            this.mForceRemoves.add(ws);
                            leakedSurface = true;
                        } else if (ws.mAppToken != null && ws.mAppToken.clientHidden) {
                            Slog.w(TAG, "LEAKED SURFACE (app token hidden): " + ws + " surface=" + wsa.mSurfaceControl + " token=" + ws.mAppToken);
                            wsa.mSurfaceControl.destroy();
                            wsa.mSurfaceShown = false;
                            wsa.mSurfaceControl = null;
                            ws.mHasSurface = false;
                            leakedSurface = true;
                        }
                    }
                }
            }
            if (!leakedSurface) {
                Slog.w(TAG, "No leaked surfaces; killing applicatons!");
                SparseIntArray pidCandidates = new SparseIntArray();
                for (int displayNdx2 = 0; displayNdx2 < numDisplays; displayNdx2++) {
                    WindowList windows2 = this.mDisplayContents.valueAt(displayNdx2).getWindowList();
                    int numWindows2 = windows2.size();
                    for (int winNdx2 = 0; winNdx2 < numWindows2; winNdx2++) {
                        WindowState ws2 = windows2.get(winNdx2);
                        if (!this.mForceRemoves.contains(ws2)) {
                            WindowStateAnimator wsa2 = ws2.mWinAnimator;
                            if (wsa2.mSurfaceControl != null) {
                                pidCandidates.append(wsa2.mSession.mPid, wsa2.mSession.mPid);
                            }
                        }
                    }
                    if (pidCandidates.size() > 0) {
                        int[] pids = new int[pidCandidates.size()];
                        for (int i = 0; i < pids.length; i++) {
                            pids[i] = pidCandidates.keyAt(i);
                        }
                        try {
                            if (this.mActivityManager.killPids(pids, "Free memory", secure)) {
                                killedApps = true;
                            }
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
            if (leakedSurface || killedApps) {
                Slog.w(TAG, "Looks like we have reclaimed some memory, clearing surface for retry.");
                if (surface != null) {
                    surface.destroy();
                    winAnimator.mSurfaceShown = false;
                    winAnimator.mSurfaceControl = null;
                    winAnimator.mWin.mHasSurface = false;
                    scheduleRemoveStartingWindowLocked(winAnimator.mWin.mAppToken);
                }
                try {
                    winAnimator.mWin.mClient.dispatchGetNewSurface();
                } catch (RemoteException e2) {
                }
            }
            return leakedSurface || killedApps;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private boolean updateFocusedWindowLocked(int mode, boolean updateInputWindows) {
        boolean z = false;
        WindowState newFocus = computeFocusedWindowLocked();
        if (this.mCurrentFocus == newFocus) {
            return false;
        }
        Trace.traceBegin(32L, "wmUpdateFocus");
        this.mH.removeMessages(2);
        this.mH.sendEmptyMessage(2);
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        if (mode != 1 && mode != 3) {
            z = true;
        }
        boolean imWindowChanged = moveInputMethodWindowsIfNeededLocked(z);
        if (imWindowChanged) {
            displayContent.layoutNeeded = true;
            newFocus = computeFocusedWindowLocked();
        }
        WindowState oldFocus = this.mCurrentFocus;
        this.mCurrentFocus = newFocus;
        this.mLosingFocus.remove(newFocus);
        int focusChanged = this.mPolicy.focusChangedLw(oldFocus, newFocus);
        if (imWindowChanged && oldFocus != this.mInputMethodWindow) {
            if (mode == 2) {
                performLayoutLockedInner(displayContent, true, updateInputWindows);
                focusChanged &= -2;
            } else if (mode == 3) {
                assignLayersLocked(displayContent.getWindowList());
            }
        }
        if ((focusChanged & 1) != 0) {
            displayContent.layoutNeeded = true;
            if (mode == 2) {
                performLayoutLockedInner(displayContent, true, updateInputWindows);
            }
        }
        if (mode != 1) {
            this.mInputMonitor.setInputFocusLw(this.mCurrentFocus, updateInputWindows);
        }
        Trace.traceEnd(32L);
        return true;
    }

    private WindowState computeFocusedWindowLocked() {
        if (this.mAnimator.mUniverseBackground != null && this.mAnimator.mUniverseBackground.mWin.canReceiveKeys()) {
            return this.mAnimator.mUniverseBackground.mWin;
        }
        int displayCount = this.mDisplayContents.size();
        for (int i = 0; i < displayCount; i++) {
            DisplayContent displayContent = this.mDisplayContents.valueAt(i);
            WindowState win = findFocusedWindowLocked(displayContent);
            if (win != null) {
                return win;
            }
        }
        return null;
    }

    private WindowState findFocusedWindowLocked(DisplayContent displayContent) {
        WindowList windows = displayContent.getWindowList();
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowState win = windows.get(i);
            AppWindowToken wtoken = win.mAppToken;
            if ((wtoken == null || (!wtoken.removed && !wtoken.sendingToBottom)) && win.canReceiveKeys()) {
                if (wtoken != null && win.mAttrs.type != 3 && this.mFocusedApp != null) {
                    ArrayList<Task> tasks = displayContent.getTasks();
                    for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                        AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
                        int tokenNdx = tokens.size() - 1;
                        while (tokenNdx >= 0) {
                            AppWindowToken token = tokens.get(tokenNdx);
                            if (wtoken == token) {
                                break;
                            }
                            if (this.mFocusedApp == token) {
                                return null;
                            }
                            tokenNdx--;
                        }
                        if (tokenNdx >= 0) {
                            return win;
                        }
                    }
                    return win;
                }
                return win;
            }
        }
        return null;
    }

    private void startFreezingDisplayLocked(boolean inTransaction, int exitAnim, int enterAnim) {
        if (!this.mDisplayFrozen && this.mDisplayReady && this.mPolicy.isScreenOn()) {
            this.mScreenFrozenLock.acquire();
            this.mDisplayFrozen = true;
            this.mDisplayFreezeTime = SystemClock.elapsedRealtime();
            this.mLastFinishedFreezeSource = null;
            this.mInputMonitor.freezeInputDispatchingLw();
            this.mPolicy.setLastInputMethodWindowLw((WindowManagerPolicy.WindowState) null, (WindowManagerPolicy.WindowState) null);
            if (this.mAppTransition.isTransitionSet()) {
                this.mAppTransition.freeze();
            }
            this.mExitAnimId = exitAnim;
            this.mEnterAnimId = enterAnim;
            DisplayContent displayContent = getDefaultDisplayContentLocked();
            int displayId = displayContent.getDisplayId();
            ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(displayId);
            if (screenRotationAnimation != null) {
                screenRotationAnimation.kill();
            }
            boolean isSecure = false;
            WindowList windows = getDefaultWindowListLocked();
            int N = windows.size();
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                WindowState ws = windows.get(i);
                if (!ws.isOnScreen() || (ws.mAttrs.flags & PackageManagerService.DumpState.DUMP_INSTALLS) == 0) {
                    i++;
                } else {
                    isSecure = true;
                    break;
                }
            }
            displayContent.updateDisplayInfo();
            this.mAnimator.setScreenRotationAnimationLocked(displayId, new ScreenRotationAnimation(this.mContext, displayContent, this.mFxSession, inTransaction, this.mPolicy.isDefaultOrientationForced(), isSecure));
        }
    }

    private void stopFreezingDisplayLocked() {
        if (this.mDisplayFrozen && !this.mWaitingForConfig && this.mAppsFreezingScreen <= 0 && !this.mWindowsFreezingScreen && !this.mClientFreezingScreen) {
            this.mDisplayFrozen = false;
            this.mLastDisplayFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
            StringBuilder sb = new StringBuilder(128);
            sb.append("Screen frozen for ");
            TimeUtils.formatDuration(this.mLastDisplayFreezeDuration, sb);
            if (this.mLastFinishedFreezeSource != null) {
                sb.append(" due to ");
                sb.append(this.mLastFinishedFreezeSource);
            }
            Slog.i(TAG, sb.toString());
            this.mH.removeMessages(17);
            this.mH.removeMessages(30);
            boolean updateRotation = false;
            DisplayContent displayContent = getDefaultDisplayContentLocked();
            int displayId = displayContent.getDisplayId();
            ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(displayId);
            if (screenRotationAnimation != null && screenRotationAnimation.hasScreenshot()) {
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                boolean isDimming = displayContent.isDimming();
                if (!this.mPolicy.validateRotationAnimationLw(this.mExitAnimId, this.mEnterAnimId, isDimming)) {
                    this.mEnterAnimId = 0;
                    this.mExitAnimId = 0;
                }
                if (screenRotationAnimation.dismiss(this.mFxSession, WALLPAPER_TIMEOUT_RECOVERY, getTransitionAnimationScaleLocked(), displayInfo.logicalWidth, displayInfo.logicalHeight, this.mExitAnimId, this.mEnterAnimId)) {
                    scheduleAnimationLocked();
                } else {
                    screenRotationAnimation.kill();
                    this.mAnimator.setScreenRotationAnimationLocked(displayId, null);
                    updateRotation = true;
                }
            } else {
                if (screenRotationAnimation != null) {
                    screenRotationAnimation.kill();
                    this.mAnimator.setScreenRotationAnimationLocked(displayId, null);
                }
                updateRotation = true;
            }
            this.mInputMonitor.thawInputDispatchingLw();
            boolean configChanged = updateOrientationFromAppTokensLocked(false);
            this.mH.removeMessages(15);
            this.mH.sendEmptyMessageDelayed(15, 2000L);
            this.mScreenFrozenLock.release();
            if (updateRotation) {
                configChanged |= updateRotationUncheckedLocked(false);
            }
            if (configChanged) {
                this.mH.sendEmptyMessage(18);
            }
        }
    }

    static int getPropertyInt(String[] tokens, int index, int defUnits, int defDps, DisplayMetrics dm) {
        String str;
        if (index < tokens.length && (str = tokens[index]) != null && str.length() > 0) {
            try {
                return Integer.parseInt(str);
            } catch (Exception e) {
            }
        }
        return defUnits == 0 ? defDps : (int) TypedValue.applyDimension(defUnits, defDps, dm);
    }

    void createWatermarkInTransaction() throws Throwable {
        String[] toks;
        if (this.mWatermark != null) {
            return;
        }
        File file = new File("/system/etc/setup.conf");
        FileInputStream in = null;
        DataInputStream ind = null;
        try {
            FileInputStream in2 = new FileInputStream(file);
            try {
                DataInputStream ind2 = new DataInputStream(in2);
                try {
                    String line = ind2.readLine();
                    if (line != null && (toks = line.split("%")) != null && toks.length > 0) {
                        this.mWatermark = new Watermark(getDefaultDisplayContentLocked().getDisplay(), this.mRealDisplayMetrics, this.mFxSession, toks);
                    }
                    if (ind2 != null) {
                        try {
                            ind2.close();
                        } catch (IOException e) {
                        }
                    } else if (in2 != null) {
                        try {
                            in2.close();
                        } catch (IOException e2) {
                        }
                    }
                } catch (FileNotFoundException e3) {
                    ind = ind2;
                    in = in2;
                    if (ind != null) {
                        try {
                            ind.close();
                        } catch (IOException e4) {
                        }
                    } else if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e5) {
                        }
                    }
                } catch (IOException e6) {
                    ind = ind2;
                    in = in2;
                    if (ind != null) {
                        try {
                            ind.close();
                        } catch (IOException e7) {
                        }
                    } else if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e8) {
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    ind = ind2;
                    in = in2;
                    if (ind != null) {
                        try {
                            ind.close();
                        } catch (IOException e9) {
                        }
                    } else if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e10) {
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e11) {
                in = in2;
            } catch (IOException e12) {
                in = in2;
            } catch (Throwable th2) {
                th = th2;
                in = in2;
            }
        } catch (FileNotFoundException e13) {
        } catch (IOException e14) {
        } catch (Throwable th3) {
            th = th3;
        }
    }

    public void statusBarVisibilityChanged(int visibility) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") != 0) {
            throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
        }
        synchronized (this.mWindowMap) {
            this.mLastStatusBarVisibility = visibility;
            updateStatusBarVisibilityLocked(this.mPolicy.adjustSystemUiVisibilityLw(visibility));
        }
    }

    void updateStatusBarVisibilityLocked(int visibility) {
        this.mInputManager.setSystemUiVisibility(visibility);
        WindowList windows = getDefaultWindowListLocked();
        int N = windows.size();
        for (int i = 0; i < N; i++) {
            WindowState ws = windows.get(i);
            try {
                int curValue = ws.mSystemUiVisibility;
                int diff = (curValue ^ visibility) & 7 & (visibility ^ (-1));
                int newValue = ((diff ^ (-1)) & curValue) | (visibility & diff);
                if (newValue != curValue) {
                    ws.mSeq++;
                    ws.mSystemUiVisibility = newValue;
                }
                if (newValue != curValue || ws.mAttrs.hasSystemUiListeners) {
                    ws.mClient.dispatchSystemUiVisibilityChanged(ws.mSeq, visibility, newValue, diff);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public void reevaluateStatusBarVisibility() {
        synchronized (this.mWindowMap) {
            int visibility = this.mPolicy.adjustSystemUiVisibilityLw(this.mLastStatusBarVisibility);
            updateStatusBarVisibilityLocked(visibility);
            performLayoutAndPlaceSurfacesLocked();
        }
    }

    public WindowManagerPolicy.FakeWindow addFakeWindow(Looper looper, InputEventReceiver.Factory inputEventReceiverFactory, String name, int windowType, int layoutParamsFlags, int layoutParamsPrivateFlags, boolean canReceiveKeys, boolean hasFocus, boolean touchFullscreen) {
        FakeWindowImpl fw;
        synchronized (this.mWindowMap) {
            fw = new FakeWindowImpl(this, looper, inputEventReceiverFactory, name, windowType, layoutParamsFlags, canReceiveKeys, hasFocus, touchFullscreen);
            while (0 < this.mFakeWindows.size() && this.mFakeWindows.get(0).mWindowLayer > fw.mWindowLayer) {
            }
            this.mFakeWindows.add(0, fw);
            this.mInputMonitor.updateInputWindowsLw(true);
        }
        return fw;
    }

    boolean removeFakeWindowLocked(WindowManagerPolicy.FakeWindow window) {
        boolean z = true;
        synchronized (this.mWindowMap) {
            if (this.mFakeWindows.remove(window)) {
                this.mInputMonitor.updateInputWindowsLw(true);
            } else {
                z = false;
            }
        }
        return z;
    }

    public void saveLastInputMethodWindowForTransition() {
        synchronized (this.mWindowMap) {
            getDefaultDisplayContentLocked();
            if (this.mInputMethodWindow != null) {
                this.mPolicy.setLastInputMethodWindowLw(this.mInputMethodWindow, this.mInputMethodTarget);
            }
        }
    }

    public int getInputMethodWindowVisibleHeight() {
        int inputMethodWindowVisibleHeightLw;
        synchronized (this.mWindowMap) {
            inputMethodWindowVisibleHeightLw = this.mPolicy.getInputMethodWindowVisibleHeightLw();
        }
        return inputMethodWindowVisibleHeightLw;
    }

    public boolean hasNavigationBar() {
        return this.mPolicy.hasNavigationBar();
    }

    public void lockNow(Bundle options) {
        this.mPolicy.lockNow(options);
    }

    public void showRecentApps() {
        this.mPolicy.showRecentApps();
    }

    public boolean isSafeModeEnabled() {
        return this.mSafeMode;
    }

    public boolean clearWindowContentFrameStats(IBinder token) {
        boolean zClearContentFrameStats = false;
        if (!checkCallingPermission("android.permission.FRAME_STATS", "clearWindowContentFrameStats()")) {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        synchronized (this.mWindowMap) {
            WindowState windowState = this.mWindowMap.get(token);
            if (windowState != null) {
                SurfaceControl surfaceControl = windowState.mWinAnimator.mSurfaceControl;
                if (surfaceControl != null) {
                    zClearContentFrameStats = surfaceControl.clearContentFrameStats();
                }
            }
        }
        return zClearContentFrameStats;
    }

    public WindowContentFrameStats getWindowContentFrameStats(IBinder token) {
        if (!checkCallingPermission("android.permission.FRAME_STATS", "getWindowContentFrameStats()")) {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        synchronized (this.mWindowMap) {
            WindowState windowState = this.mWindowMap.get(token);
            if (windowState == null) {
                return null;
            }
            SurfaceControl surfaceControl = windowState.mWinAnimator.mSurfaceControl;
            if (surfaceControl == null) {
                return null;
            }
            if (this.mTempWindowRenderStats == null) {
                this.mTempWindowRenderStats = new WindowContentFrameStats();
            }
            WindowContentFrameStats stats = this.mTempWindowRenderStats;
            if (surfaceControl.getContentFrameStats(stats)) {
                return stats;
            }
            return null;
        }
    }

    void dumpPolicyLocked(PrintWriter pw, String[] args, boolean dumpAll) {
        pw.println("WINDOW MANAGER POLICY STATE (dumpsys window policy)");
        this.mPolicy.dump("    ", pw, args);
    }

    void dumpAnimatorLocked(PrintWriter pw, String[] args, boolean dumpAll) {
        pw.println("WINDOW MANAGER ANIMATOR STATE (dumpsys window animator)");
        this.mAnimator.dumpLocked(pw, "    ", dumpAll);
    }

    void dumpTokensLocked(PrintWriter pw, boolean dumpAll) {
        pw.println("WINDOW MANAGER TOKENS (dumpsys window tokens)");
        if (this.mTokenMap.size() > 0) {
            pw.println("  All tokens:");
            for (WindowToken token : this.mTokenMap.values()) {
                pw.print("  ");
                pw.print(token);
                if (dumpAll) {
                    pw.println(':');
                    token.dump(pw, "    ");
                } else {
                    pw.println();
                }
            }
        }
        if (this.mWallpaperTokens.size() > 0) {
            pw.println();
            pw.println("  Wallpaper tokens:");
            for (int i = this.mWallpaperTokens.size() - 1; i >= 0; i--) {
                WindowToken token2 = this.mWallpaperTokens.get(i);
                pw.print("  Wallpaper #");
                pw.print(i);
                pw.print(' ');
                pw.print(token2);
                if (dumpAll) {
                    pw.println(':');
                    token2.dump(pw, "    ");
                } else {
                    pw.println();
                }
            }
        }
        if (this.mFinishedStarting.size() > 0) {
            pw.println();
            pw.println("  Finishing start of application tokens:");
            for (int i2 = this.mFinishedStarting.size() - 1; i2 >= 0; i2--) {
                WindowToken token3 = this.mFinishedStarting.get(i2);
                pw.print("  Finished Starting #");
                pw.print(i2);
                pw.print(' ');
                pw.print(token3);
                if (dumpAll) {
                    pw.println(':');
                    token3.dump(pw, "    ");
                } else {
                    pw.println();
                }
            }
        }
        if (this.mOpeningApps.size() > 0 || this.mClosingApps.size() > 0) {
            pw.println();
            if (this.mOpeningApps.size() > 0) {
                pw.print("  mOpeningApps=");
                pw.println(this.mOpeningApps);
            }
            if (this.mClosingApps.size() > 0) {
                pw.print("  mClosingApps=");
                pw.println(this.mClosingApps);
            }
        }
    }

    void dumpSessionsLocked(PrintWriter pw, boolean dumpAll) {
        pw.println("WINDOW MANAGER SESSIONS (dumpsys window sessions)");
        for (int i = 0; i < this.mSessions.size(); i++) {
            Session s = this.mSessions.valueAt(i);
            pw.print("  Session ");
            pw.print(s);
            pw.println(':');
            s.dump(pw, "    ");
        }
    }

    void dumpDisplayContentsLocked(PrintWriter pw, boolean dumpAll) {
        pw.println("WINDOW MANAGER DISPLAY CONTENTS (dumpsys window displays)");
        if (this.mDisplayReady) {
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
                displayContent.dump("  ", pw);
            }
            return;
        }
        pw.println("  NO DISPLAY");
    }

    void dumpWindowsLocked(PrintWriter pw, boolean dumpAll, ArrayList<WindowState> windows) {
        pw.println("WINDOW MANAGER WINDOWS (dumpsys window windows)");
        dumpWindowsNoHeaderLocked(pw, dumpAll, windows);
    }

    void dumpWindowsNoHeaderLocked(PrintWriter pw, boolean dumpAll, ArrayList<WindowState> windows) {
        int numDisplays = this.mDisplayContents.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            WindowList windowList = this.mDisplayContents.valueAt(displayNdx).getWindowList();
            for (int winNdx = windowList.size() - 1; winNdx >= 0; winNdx--) {
                WindowState w = windowList.get(winNdx);
                if (windows == null || windows.contains(w)) {
                    pw.print("  Window #");
                    pw.print(winNdx);
                    pw.print(' ');
                    pw.print(w);
                    pw.println(":");
                    w.dump(pw, "    ", dumpAll || windows != null);
                }
            }
        }
        if (this.mInputMethodDialogs.size() > 0) {
            pw.println();
            pw.println("  Input method dialogs:");
            for (int i = this.mInputMethodDialogs.size() - 1; i >= 0; i--) {
                WindowState w2 = this.mInputMethodDialogs.get(i);
                if (windows == null || windows.contains(w2)) {
                    pw.print("  IM Dialog #");
                    pw.print(i);
                    pw.print(": ");
                    pw.println(w2);
                }
            }
        }
        if (this.mPendingRemove.size() > 0) {
            pw.println();
            pw.println("  Remove pending for:");
            for (int i2 = this.mPendingRemove.size() - 1; i2 >= 0; i2--) {
                WindowState w3 = this.mPendingRemove.get(i2);
                if (windows == null || windows.contains(w3)) {
                    pw.print("  Remove #");
                    pw.print(i2);
                    pw.print(' ');
                    pw.print(w3);
                    if (dumpAll) {
                        pw.println(":");
                        w3.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mForceRemoves != null && this.mForceRemoves.size() > 0) {
            pw.println();
            pw.println("  Windows force removing:");
            for (int i3 = this.mForceRemoves.size() - 1; i3 >= 0; i3--) {
                WindowState w4 = this.mForceRemoves.get(i3);
                pw.print("  Removing #");
                pw.print(i3);
                pw.print(' ');
                pw.print(w4);
                if (dumpAll) {
                    pw.println(":");
                    w4.dump(pw, "    ", true);
                } else {
                    pw.println();
                }
            }
        }
        if (this.mDestroySurface.size() > 0) {
            pw.println();
            pw.println("  Windows waiting to destroy their surface:");
            for (int i4 = this.mDestroySurface.size() - 1; i4 >= 0; i4--) {
                WindowState w5 = this.mDestroySurface.get(i4);
                if (windows == null || windows.contains(w5)) {
                    pw.print("  Destroy #");
                    pw.print(i4);
                    pw.print(' ');
                    pw.print(w5);
                    if (dumpAll) {
                        pw.println(":");
                        w5.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mLosingFocus.size() > 0) {
            pw.println();
            pw.println("  Windows losing focus:");
            for (int i5 = this.mLosingFocus.size() - 1; i5 >= 0; i5--) {
                WindowState w6 = this.mLosingFocus.get(i5);
                if (windows == null || windows.contains(w6)) {
                    pw.print("  Losing #");
                    pw.print(i5);
                    pw.print(' ');
                    pw.print(w6);
                    if (dumpAll) {
                        pw.println(":");
                        w6.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mResizingWindows.size() > 0) {
            pw.println();
            pw.println("  Windows waiting to resize:");
            for (int i6 = this.mResizingWindows.size() - 1; i6 >= 0; i6--) {
                WindowState w7 = this.mResizingWindows.get(i6);
                if (windows == null || windows.contains(w7)) {
                    pw.print("  Resizing #");
                    pw.print(i6);
                    pw.print(' ');
                    pw.print(w7);
                    if (dumpAll) {
                        pw.println(":");
                        w7.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mWaitingForDrawn.size() > 0) {
            pw.println();
            pw.println("  Clients waiting for these windows to be drawn:");
            for (int i7 = this.mWaitingForDrawn.size() - 1; i7 >= 0; i7--) {
                WindowState win = this.mWaitingForDrawn.get(i7);
                pw.print("  Waiting #");
                pw.print(i7);
                pw.print(' ');
                pw.print(win);
            }
        }
        pw.println();
        pw.print("  mCurConfiguration=");
        pw.println(this.mCurConfiguration);
        pw.print("  mHasPermanentDpad=");
        pw.println(this.mHasPermanentDpad);
        pw.print("  mCurrentFocus=");
        pw.println(this.mCurrentFocus);
        if (this.mLastFocus != this.mCurrentFocus) {
            pw.print("  mLastFocus=");
            pw.println(this.mLastFocus);
        }
        pw.print("  mFocusedApp=");
        pw.println(this.mFocusedApp);
        if (this.mInputMethodTarget != null) {
            pw.print("  mInputMethodTarget=");
            pw.println(this.mInputMethodTarget);
        }
        pw.print("  mInTouchMode=");
        pw.print(this.mInTouchMode);
        pw.print(" mLayoutSeq=");
        pw.println(this.mLayoutSeq);
        pw.print("  mLastDisplayFreezeDuration=");
        TimeUtils.formatDuration(this.mLastDisplayFreezeDuration, pw);
        if (this.mLastFinishedFreezeSource != null) {
            pw.print(" due to ");
            pw.print(this.mLastFinishedFreezeSource);
        }
        pw.println();
        if (dumpAll) {
            pw.print("  mSystemDecorLayer=");
            pw.print(this.mSystemDecorLayer);
            pw.print(" mScreenRect=");
            pw.println(this.mScreenRect.toShortString());
            if (this.mLastStatusBarVisibility != 0) {
                pw.print("  mLastStatusBarVisibility=0x");
                pw.println(Integer.toHexString(this.mLastStatusBarVisibility));
            }
            if (this.mInputMethodWindow != null) {
                pw.print("  mInputMethodWindow=");
                pw.println(this.mInputMethodWindow);
            }
            pw.print("  mWallpaperTarget=");
            pw.println(this.mWallpaperTarget);
            if (this.mLowerWallpaperTarget != null || this.mUpperWallpaperTarget != null) {
                pw.print("  mLowerWallpaperTarget=");
                pw.println(this.mLowerWallpaperTarget);
                pw.print("  mUpperWallpaperTarget=");
                pw.println(this.mUpperWallpaperTarget);
            }
            pw.print("  mLastWallpaperX=");
            pw.print(this.mLastWallpaperX);
            pw.print(" mLastWallpaperY=");
            pw.println(this.mLastWallpaperY);
            if (this.mLastWallpaperDisplayOffsetX != Integer.MIN_VALUE || this.mLastWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                pw.print("  mLastWallpaperDisplayOffsetX=");
                pw.print(this.mLastWallpaperDisplayOffsetX);
                pw.print(" mLastWallpaperDisplayOffsetY=");
                pw.println(this.mLastWallpaperDisplayOffsetY);
            }
            if (this.mInputMethodAnimLayerAdjustment != 0 || this.mWallpaperAnimLayerAdjustment != 0) {
                pw.print("  mInputMethodAnimLayerAdjustment=");
                pw.print(this.mInputMethodAnimLayerAdjustment);
                pw.print("  mWallpaperAnimLayerAdjustment=");
                pw.println(this.mWallpaperAnimLayerAdjustment);
            }
            pw.print("  mSystemBooted=");
            pw.print(this.mSystemBooted);
            pw.print(" mDisplayEnabled=");
            pw.println(this.mDisplayEnabled);
            if (needsLayout()) {
                pw.print("  layoutNeeded on displays=");
                for (int displayNdx2 = 0; displayNdx2 < numDisplays; displayNdx2++) {
                    DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx2);
                    if (displayContent.layoutNeeded) {
                        pw.print(displayContent.getDisplayId());
                    }
                }
                pw.println();
            }
            pw.print("  mTransactionSequence=");
            pw.println(this.mTransactionSequence);
            pw.print("  mDisplayFrozen=");
            pw.print(this.mDisplayFrozen);
            pw.print(" windows=");
            pw.print(this.mWindowsFreezingScreen);
            pw.print(" client=");
            pw.print(this.mClientFreezingScreen);
            pw.print(" apps=");
            pw.print(this.mAppsFreezingScreen);
            pw.print(" waitingForConfig=");
            pw.println(this.mWaitingForConfig);
            pw.print("  mRotation=");
            pw.print(this.mRotation);
            pw.print(" mAltOrientation=");
            pw.println(this.mAltOrientation);
            pw.print("  mLastWindowForcedOrientation=");
            pw.print(this.mLastWindowForcedOrientation);
            pw.print(" mForcedAppOrientation=");
            pw.println(this.mForcedAppOrientation);
            pw.print("  mDeferredRotationPauseCount=");
            pw.println(this.mDeferredRotationPauseCount);
            pw.print("  Animation settings: disabled=");
            pw.print(this.mAnimationsDisabled);
            pw.print(" window=");
            pw.print(this.mWindowAnimationScaleSetting);
            pw.print(" transition=");
            pw.print(this.mTransitionAnimationScaleSetting);
            pw.print(" animator=");
            pw.println(this.mAnimatorDurationScaleSetting);
            pw.print("  mTraversalScheduled=");
            pw.println(this.mTraversalScheduled);
            pw.print("  mStartingIconInTransition=");
            pw.print(this.mStartingIconInTransition);
            pw.print(" mSkipAppTransitionAnimation=");
            pw.println(this.mSkipAppTransitionAnimation);
            pw.println("  mLayoutToAnim:");
            this.mAppTransition.dump(pw);
        }
    }

    boolean dumpWindows(PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        WindowList windows = new WindowList();
        if ("visible".equals(name)) {
            synchronized (this.mWindowMap) {
                int numDisplays = this.mDisplayContents.size();
                for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                    WindowList windowList = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                    for (int winNdx = windowList.size() - 1; winNdx >= 0; winNdx--) {
                        WindowState w = windowList.get(winNdx);
                        if (w.mWinAnimator.mSurfaceShown) {
                            windows.add(w);
                        }
                    }
                }
            }
        } else {
            int objectId = 0;
            try {
                objectId = Integer.parseInt(name, 16);
                name = null;
            } catch (RuntimeException e) {
            }
            synchronized (this.mWindowMap) {
                int numDisplays2 = this.mDisplayContents.size();
                for (int displayNdx2 = 0; displayNdx2 < numDisplays2; displayNdx2++) {
                    WindowList windowList2 = this.mDisplayContents.valueAt(displayNdx2).getWindowList();
                    for (int winNdx2 = windowList2.size() - 1; winNdx2 >= 0; winNdx2--) {
                        WindowState w2 = windowList2.get(winNdx2);
                        if (name != null) {
                            if (w2.mAttrs.getTitle().toString().contains(name)) {
                                windows.add(w2);
                            }
                        } else if (System.identityHashCode(w2) == objectId) {
                            windows.add(w2);
                        }
                    }
                }
            }
        }
        if (windows.size() <= 0) {
            return false;
        }
        synchronized (this.mWindowMap) {
            dumpWindowsLocked(pw, dumpAll, windows);
        }
        return true;
    }

    void dumpLastANRLocked(PrintWriter pw) {
        pw.println("WINDOW MANAGER LAST ANR (dumpsys window lastanr)");
        if (this.mLastANRState == null) {
            pw.println("  <no ANR has occurred since boot>");
        } else {
            pw.println(this.mLastANRState);
        }
    }

    public void saveANRStateLocked(AppWindowToken appWindowToken, WindowState windowState, String reason) {
        StringWriter sw = new StringWriter();
        FastPrintWriter fastPrintWriter = new FastPrintWriter(sw, false, 1024);
        fastPrintWriter.println("  ANR time: " + DateFormat.getInstance().format(new Date()));
        if (appWindowToken != null) {
            fastPrintWriter.println("  Application at fault: " + appWindowToken.stringName);
        }
        if (windowState != null) {
            fastPrintWriter.println("  Window at fault: " + ((Object) windowState.mAttrs.getTitle()));
        }
        if (reason != null) {
            fastPrintWriter.println("  Reason: " + reason);
        }
        fastPrintWriter.println();
        dumpWindowsNoHeaderLocked(fastPrintWriter, true, null);
        fastPrintWriter.println();
        fastPrintWriter.println("Last ANR continued");
        dumpDisplayContentsLocked(fastPrintWriter, true);
        fastPrintWriter.close();
        this.mLastANRState = sw.toString();
        this.mH.removeMessages(38);
        this.mH.sendEmptyMessageDelayed(38, 7200000L);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String opt;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WindowManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        boolean dumpAll = false;
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
            opti++;
            if ("-a".equals(opt)) {
                dumpAll = true;
            } else {
                if ("-h".equals(opt)) {
                    pw.println("Window manager dump options:");
                    pw.println("  [-a] [-h] [cmd] ...");
                    pw.println("  cmd may be one of:");
                    pw.println("    l[astanr]: last ANR information");
                    pw.println("    p[policy]: policy state");
                    pw.println("    a[animator]: animator state");
                    pw.println("    s[essions]: active sessions");
                    pw.println("    surfaces: active surfaces (debugging enabled only)");
                    pw.println("    d[isplays]: active display contents");
                    pw.println("    t[okens]: token list");
                    pw.println("    w[indows]: window list");
                    pw.println("  cmd may also be a NAME to dump windows.  NAME may");
                    pw.println("    be a partial substring in a window name, a");
                    pw.println("    Window hex object identifier, or");
                    pw.println("    \"all\" for all windows, or");
                    pw.println("    \"visible\" for the visible windows.");
                    pw.println("  -a: include all available server state.");
                    return;
                }
                pw.println("Unknown argument: " + opt + "; use -h for help");
            }
        }
        if (opti < args.length) {
            String cmd = args[opti];
            int opti2 = opti + 1;
            if ("lastanr".equals(cmd) || "l".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    dumpLastANRLocked(pw);
                }
                return;
            }
            if ("policy".equals(cmd) || "p".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    dumpPolicyLocked(pw, args, true);
                }
                return;
            }
            if ("animator".equals(cmd) || "a".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    dumpAnimatorLocked(pw, args, true);
                }
                return;
            }
            if ("sessions".equals(cmd) || "s".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    dumpSessionsLocked(pw, true);
                }
                return;
            }
            if ("surfaces".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    WindowStateAnimator.SurfaceTrace.dumpAllSurfaces(pw, null);
                }
                return;
            }
            if ("displays".equals(cmd) || "d".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    dumpDisplayContentsLocked(pw, true);
                }
                return;
            }
            if ("tokens".equals(cmd) || "t".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    dumpTokensLocked(pw, true);
                }
                return;
            }
            if ("windows".equals(cmd) || "w".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    dumpWindowsLocked(pw, true, null);
                }
                return;
            } else if ("all".equals(cmd) || "a".equals(cmd)) {
                synchronized (this.mWindowMap) {
                    dumpWindowsLocked(pw, true, null);
                }
                return;
            } else {
                if (!dumpWindows(pw, cmd, args, opti2, dumpAll)) {
                    pw.println("Bad window command, or no windows match: " + cmd);
                    pw.println("Use -h for help.");
                    return;
                }
                return;
            }
        }
        synchronized (this.mWindowMap) {
            pw.println();
            if (dumpAll) {
                pw.println("-------------------------------------------------------------------------------");
            }
            dumpLastANRLocked(pw);
            pw.println();
            if (dumpAll) {
                pw.println("-------------------------------------------------------------------------------");
            }
            dumpPolicyLocked(pw, args, dumpAll);
            pw.println();
            if (dumpAll) {
                pw.println("-------------------------------------------------------------------------------");
            }
            dumpAnimatorLocked(pw, args, dumpAll);
            pw.println();
            if (dumpAll) {
                pw.println("-------------------------------------------------------------------------------");
            }
            dumpSessionsLocked(pw, dumpAll);
            pw.println();
            if (dumpAll) {
                pw.println("-------------------------------------------------------------------------------");
            }
            WindowStateAnimator.SurfaceTrace.dumpAllSurfaces(pw, dumpAll ? "-------------------------------------------------------------------------------" : null);
            pw.println();
            if (dumpAll) {
                pw.println("-------------------------------------------------------------------------------");
            }
            dumpDisplayContentsLocked(pw, dumpAll);
            pw.println();
            if (dumpAll) {
                pw.println("-------------------------------------------------------------------------------");
            }
            dumpTokensLocked(pw, dumpAll);
            pw.println();
            if (dumpAll) {
                pw.println("-------------------------------------------------------------------------------");
            }
            dumpWindowsLocked(pw, dumpAll, null);
        }
    }

    @Override
    public void monitor() {
        synchronized (this.mWindowMap) {
        }
    }

    void debugLayoutRepeats(String msg, int pendingLayoutChanges) {
        if (this.mLayoutRepeatCount >= 4) {
            Slog.v(TAG, "Layouts looping: " + msg + ", mPendingLayoutChanges = 0x" + Integer.toHexString(pendingLayoutChanges));
        }
    }

    private DisplayContent newDisplayContentLocked(Display display) {
        DisplayContent displayContent = new DisplayContent(display, this);
        int displayId = display.getDisplayId();
        this.mDisplayContents.put(displayId, displayContent);
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        Rect rect = new Rect();
        this.mDisplaySettings.getOverscanLocked(displayInfo.name, displayInfo.uniqueId, rect);
        synchronized (displayContent.mDisplaySizeLock) {
            displayInfo.overscanLeft = rect.left;
            displayInfo.overscanTop = rect.top;
            displayInfo.overscanRight = rect.right;
            displayInfo.overscanBottom = rect.bottom;
            this.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(displayId, displayInfo);
        }
        configureDisplayPolicyLocked(displayContent);
        if (displayId == 0) {
            displayContent.mTapDetector = new StackTapPointerEventListener(this, displayContent);
            registerPointerEventListener(displayContent.mTapDetector);
        }
        return displayContent;
    }

    public void createDisplayContentLocked(Display display) {
        if (display == null) {
            throw new IllegalArgumentException("getDisplayContent: display must not be null");
        }
        getDisplayContentLocked(display.getDisplayId());
    }

    public DisplayContent getDisplayContentLocked(int displayId) {
        Display display;
        DisplayContent displayContent = this.mDisplayContents.get(displayId);
        if (displayContent == null && (display = this.mDisplayManager.getDisplay(displayId)) != null) {
            return newDisplayContentLocked(display);
        }
        return displayContent;
    }

    public DisplayContent getDefaultDisplayContentLocked() {
        return getDisplayContentLocked(0);
    }

    public WindowList getDefaultWindowListLocked() {
        return getDefaultDisplayContentLocked().getWindowList();
    }

    public DisplayInfo getDefaultDisplayInfoLocked() {
        return getDefaultDisplayContentLocked().getDisplayInfo();
    }

    public WindowList getWindowListLocked(Display display) {
        return getWindowListLocked(display.getDisplayId());
    }

    public WindowList getWindowListLocked(int displayId) {
        DisplayContent displayContent = getDisplayContentLocked(displayId);
        if (displayContent != null) {
            return displayContent.getWindowList();
        }
        return null;
    }

    public void onDisplayAdded(int displayId) {
        this.mH.sendMessage(this.mH.obtainMessage(27, displayId, 0));
    }

    public void handleDisplayAdded(int displayId) {
        synchronized (this.mWindowMap) {
            Display display = this.mDisplayManager.getDisplay(displayId);
            if (display != null) {
                createDisplayContentLocked(display);
                displayReady(displayId);
            }
            requestTraversalLocked();
        }
    }

    public void onDisplayRemoved(int displayId) {
        this.mH.sendMessage(this.mH.obtainMessage(28, displayId, 0));
    }

    private void handleDisplayRemovedLocked(int displayId) {
        DisplayContent displayContent = getDisplayContentLocked(displayId);
        if (displayContent != null) {
            if (displayContent.isAnimating()) {
                displayContent.mDeferredRemoval = true;
                return;
            }
            this.mDisplayContents.delete(displayId);
            displayContent.close();
            if (displayId == 0) {
                unregisterPointerEventListener(displayContent.mTapDetector);
            }
        }
        this.mAnimator.removeDisplayLocked(displayId);
        requestTraversalLocked();
    }

    public void onDisplayChanged(int displayId) {
        this.mH.sendMessage(this.mH.obtainMessage(29, displayId, 0));
    }

    private void handleDisplayChangedLocked(int displayId) {
        DisplayContent displayContent = getDisplayContentLocked(displayId);
        if (displayContent != null) {
            displayContent.updateDisplayInfo();
        }
        requestTraversalLocked();
    }

    public Object getWindowManagerLock() {
        return this.mWindowMap;
    }

    void updateNonSystemOverlayWindowsVisibilityIfNeeded(WindowState win, boolean surfaceShown) {
        if (win.hideNonSystemOverlayWindowsWhenVisible()) {
            boolean systemAlertWindowsHidden = !this.mHidingNonSystemOverlayWindows.isEmpty();
            if (surfaceShown) {
                if (!this.mHidingNonSystemOverlayWindows.contains(win)) {
                    this.mHidingNonSystemOverlayWindows.add(win);
                }
            } else {
                this.mHidingNonSystemOverlayWindows.remove(win);
            }
            boolean hideSystemAlertWindows = this.mHidingNonSystemOverlayWindows.isEmpty() ? false : true;
            if (systemAlertWindowsHidden != hideSystemAlertWindows) {
                int numDisplays = this.mDisplayContents.size();
                for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                    WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                    int numWindows = windows.size();
                    for (int winNdx = 0; winNdx < numWindows; winNdx++) {
                        WindowState w = windows.get(winNdx);
                        w.setForceHideNonSystemOverlayWindowIfNeeded(hideSystemAlertWindows);
                    }
                }
            }
        }
    }

    private final class LocalService extends WindowManagerInternal {
        private LocalService() {
        }

        public void requestTraversalFromDisplayManager() {
            WindowManagerService.this.requestTraversal();
        }

        public void setMagnificationSpec(MagnificationSpec spec) {
            synchronized (WindowManagerService.this.mWindowMap) {
                if (WindowManagerService.this.mAccessibilityController != null) {
                    WindowManagerService.this.mAccessibilityController.setMagnificationSpecLocked(spec);
                } else {
                    throw new IllegalStateException("Magnification callbacks not set!");
                }
            }
            if (Binder.getCallingPid() != Process.myPid()) {
                spec.recycle();
            }
        }

        public MagnificationSpec getCompatibleMagnificationSpecForWindow(IBinder windowToken) {
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowState windowState = WindowManagerService.this.mWindowMap.get(windowToken);
                if (windowState == null) {
                    return null;
                }
                MagnificationSpec spec = null;
                if (WindowManagerService.this.mAccessibilityController != null) {
                    spec = WindowManagerService.this.mAccessibilityController.getMagnificationSpecForWindowLocked(windowState);
                }
                if ((spec == null || spec.isNop()) && windowState.mGlobalScale == 1.0f) {
                    return null;
                }
                MagnificationSpec spec2 = spec == null ? MagnificationSpec.obtain() : MagnificationSpec.obtain(spec);
                spec2.scale *= windowState.mGlobalScale;
                return spec2;
            }
        }

        public void setMagnificationCallbacks(WindowManagerInternal.MagnificationCallbacks callbacks) {
            synchronized (WindowManagerService.this.mWindowMap) {
                if (WindowManagerService.this.mAccessibilityController == null) {
                    WindowManagerService.this.mAccessibilityController = new AccessibilityController(WindowManagerService.this);
                }
                WindowManagerService.this.mAccessibilityController.setMagnificationCallbacksLocked(callbacks);
                if (!WindowManagerService.this.mAccessibilityController.hasCallbacksLocked()) {
                    WindowManagerService.this.mAccessibilityController = null;
                }
            }
        }

        public void setWindowsForAccessibilityCallback(WindowManagerInternal.WindowsForAccessibilityCallback callback) {
            synchronized (WindowManagerService.this.mWindowMap) {
                if (WindowManagerService.this.mAccessibilityController == null) {
                    WindowManagerService.this.mAccessibilityController = new AccessibilityController(WindowManagerService.this);
                }
                WindowManagerService.this.mAccessibilityController.setWindowsForAccessibilityCallback(callback);
                if (!WindowManagerService.this.mAccessibilityController.hasCallbacksLocked()) {
                    WindowManagerService.this.mAccessibilityController = null;
                }
            }
        }

        public void setInputFilter(IInputFilter filter) {
            WindowManagerService.this.mInputManager.setInputFilter(filter);
        }

        public IBinder getFocusedWindowToken() {
            IBinder iBinderAsBinder;
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowState windowState = WindowManagerService.this.getFocusedWindowLocked();
                iBinderAsBinder = windowState != null ? windowState.mClient.asBinder() : null;
            }
            return iBinderAsBinder;
        }

        public boolean isKeyguardLocked() {
            return WindowManagerService.this.isKeyguardLocked();
        }

        public void showGlobalActions() {
            WindowManagerService.this.showGlobalActions();
        }

        public void getWindowFrame(IBinder token, Rect outBounds) {
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowState windowState = WindowManagerService.this.mWindowMap.get(token);
                if (windowState != null) {
                    outBounds.set(windowState.mFrame);
                } else {
                    outBounds.setEmpty();
                }
            }
        }

        public void waitForAllWindowsDrawn(Runnable callback, long timeout) {
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowManagerService.this.mWaitingForDrawnCallback = callback;
                WindowList windows = WindowManagerService.this.getDefaultWindowListLocked();
                for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                    WindowState win = windows.get(winNdx);
                    if (win.isVisibleLw() && (win.mAppToken != null || WindowManagerService.this.mPolicy.isForceHiding(win.mAttrs))) {
                        win.mWinAnimator.mDrawState = 1;
                        win.mLastContentInsets.set(-1, -1, -1, -1);
                        WindowManagerService.this.mWaitingForDrawn.add(win);
                    }
                }
                WindowManagerService.this.requestTraversalLocked();
            }
            WindowManagerService.this.mH.removeMessages(24);
            if (WindowManagerService.this.mWaitingForDrawn.isEmpty()) {
                callback.run();
            } else {
                WindowManagerService.this.mH.sendEmptyMessageDelayed(24, timeout);
                WindowManagerService.this.checkDrawnWindowsLocked();
            }
        }

        public void addWindowToken(IBinder token, int type) {
            WindowManagerService.this.addWindowToken(token, type);
        }

        public void removeWindowToken(IBinder token, boolean removeWindows) {
            synchronized (WindowManagerService.this.mWindowMap) {
                if (removeWindows) {
                    WindowToken wtoken = WindowManagerService.this.mTokenMap.remove(token);
                    if (wtoken != null) {
                        wtoken.removeAllWindows();
                    }
                    WindowManagerService.this.removeWindowToken(token);
                } else {
                    WindowManagerService.this.removeWindowToken(token);
                }
            }
        }
    }
}
