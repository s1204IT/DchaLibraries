package com.android.server.wm;

import android.R;
import android.animation.AnimationHandler;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IAssistDataReceiver;
import android.app.admin.DevicePolicyCache;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.hardware.configstore.V1_0.ISurfaceFlingerConfigs;
import android.hardware.configstore.V1_0.OptionalBool;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputManager;
import android.net.Uri;
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
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemService;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.AppTransitionAnimationSpec;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IDockedStackListener;
import android.view.IInputFilter;
import android.view.IOnKeyguardExitResult;
import android.view.IPinnedStackListener;
import android.view.IRecentsAnimationRunner;
import android.view.IRotationWatcher;
import android.view.IWallpaperVisibilityListener;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowManager;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.InputEventReceiver;
import android.view.MagnificationSpec;
import android.view.MotionEvent;
import android.view.RemoteAnimationAdapter;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowContentFrameStats;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.internal.os.IResultReceiver;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.LatencyTracker;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.WindowManagerPolicyThread;
import com.android.server.AnimationThread;
import com.android.server.DisplayThread;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.UiModeManagerService;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.input.InputManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.ShutdownThread;
import com.android.server.usage.AppStandbyController;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.RecentsAnimationController;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowState;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.powerhal.PowerHalManager;
import com.mediatek.server.wm.WindowManagerDebugger;
import com.mediatek.server.wm.WmsExt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WindowManagerService extends IWindowManager.Stub implements Watchdog.Monitor, WindowManagerPolicy.WindowManagerFuncs {
    private static final boolean ALWAYS_KEEP_CURRENT = true;
    private static final int ANIMATION_DURATION_SCALE = 2;
    private static final int BOOT_ANIMATION_POLL_INTERVAL = 200;
    private static final String BOOT_ANIMATION_SERVICE = "bootanim";
    static final boolean CUSTOM_SCREEN_ROTATION = true;
    static final long DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS = 30000000000L;
    private static final String DENSITY_OVERRIDE = "ro.config.density_override";
    private static final int INPUT_DEVICES_READY_FOR_SAFE_MODE_DETECTION_TIMEOUT_MILLIS = 1000;
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    static final int LAYER_OFFSET_DIM = 1;
    static final int LAYER_OFFSET_THUMBNAIL = 4;
    static final int LAYOUT_REPEAT_THRESHOLD = 4;
    static final int MAX_ANIMATION_DURATION = 10000;
    private static final int MAX_SCREENSHOT_RETRIES = 3;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    static final int SEAMLESS_ROTATION_TIMEOUT_DURATION = 2000;
    private static final String SIZE_OVERRIDE = "ro.config.size_override";
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    private static final String SYSTEM_SECURE = "ro.secure";
    static final String TAG = "WindowManager";
    private static final int TRANSITION_ANIMATION_SCALE = 1;
    static final int TYPE_LAYER_MULTIPLIER = 10000;
    static final int TYPE_LAYER_OFFSET = 1000;
    static final int UPDATE_FOCUS_NORMAL = 0;
    static final int UPDATE_FOCUS_PLACING_SURFACES = 2;
    static final int UPDATE_FOCUS_WILL_ASSIGN_LAYERS = 1;
    static final int UPDATE_FOCUS_WILL_PLACE_SURFACES = 3;
    static final int WINDOWS_FREEZING_SCREENS_ACTIVE = 1;
    static final int WINDOWS_FREEZING_SCREENS_NONE = 0;
    static final int WINDOWS_FREEZING_SCREENS_TIMEOUT = 2;
    private static final int WINDOW_ANIMATION_SCALE = 0;
    static final int WINDOW_FREEZE_TIMEOUT_DURATION = 2000;
    static final int WINDOW_LAYER_MULTIPLIER = 5;
    static final int WINDOW_REPLACEMENT_TIMEOUT_DURATION = 2000;
    private static WindowManagerService sInstance;
    AccessibilityController mAccessibilityController;
    final IActivityManager mActivityManager;
    final boolean mAllowAnimationsInLowPowerMode;
    final boolean mAllowBootMessages;
    boolean mAllowTheaterModeWakeFromLayout;
    final ActivityManagerInternal mAmInternal;
    private boolean mAnimationsDisabled;
    final WindowAnimator mAnimator;
    final AppOpsManager mAppOps;
    final AppTransition mAppTransition;
    final BoundsAnimationController mBoundsAnimationController;
    CircularDisplayMask mCircularDisplayMask;
    final Context mContext;
    int mCurrentUserId;
    int mDeferredRotationPauseCount;
    boolean mDisableTransitionAnimation;
    final DisplayManager mDisplayManager;
    final DisplayManagerInternal mDisplayManagerInternal;
    boolean mDisplayReady;
    final DisplaySettings mDisplaySettings;
    Rect mDockedStackCreateBounds;
    final DragDropController mDragDropController;
    final long mDrawLockTimeoutMillis;
    EmulatorDisplayOverlay mEmulatorDisplayOverlay;
    private int mEnterAnimId;
    private boolean mEventDispatchingEnabled;
    private int mExitAnimId;
    boolean mFocusMayChange;
    private int mFrozenDisplayId;
    boolean mHardKeyboardAvailable;
    WindowManagerInternal.OnHardKeyboardStatusChangeListener mHardKeyboardStatusChangeListener;
    final boolean mHasPermanentDpad;
    private boolean mHasWideColorGamutSupport;
    final boolean mHaveInputMethods;
    private Session mHoldingScreenOn;
    private PowerManager.WakeLock mHoldingScreenWakeLock;
    boolean mInTouchMode;
    final InputManagerService mInputManager;
    IInputMethodManager mInputMethodManager;
    boolean mInputMethodTargetWaitingAnim;
    boolean mIsTouchDevice;
    private final KeyguardDisableHandler mKeyguardDisableHandler;
    boolean mKeyguardGoingAway;
    boolean mKeyguardOrAodShowingOnDefaultDisplay;
    String mLastANRState;
    private final LatencyTracker mLatencyTracker;
    final boolean mLimitedAlphaCompositing;
    final int mMaxUiWidth;
    MousePositionTracker mMousePositionTracker;
    final boolean mOnlyCore;
    final PackageManagerInternal mPmInternal;
    private final PointerEventDispatcher mPointerEventDispatcher;
    final WindowManagerPolicy mPolicy;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    private RecentsAnimationController mRecentsAnimationController;
    RootWindowContainer mRoot;
    boolean mSafeMode;
    private final PowerManager.WakeLock mScreenFrozenLock;
    SettingsObserver mSettingsObserver;
    StrictModeFlash mStrictModeFlash;
    final SurfaceAnimationRunner mSurfaceAnimationRunner;
    final TaskPositioningController mTaskPositioningController;
    final TaskSnapshotController mTaskSnapshotController;
    private WindowContentFrameStats mTempWindowRenderStats;
    int mTransactionSequence;
    private float mTransitionAnimationScaleSetting;
    private ViewServer mViewServer;
    Runnable mWaitingForDrawnCallback;
    Watermark mWatermark;
    private float mWindowAnimationScaleSetting;
    final WindowSurfacePlacer mWindowPlacerLocked;
    final WindowTracing mWindowTracing;
    static boolean PROFILE_ORIENTATION = false;
    public static boolean localLOGV = WindowManagerDebugConfig.DEBUG;
    static WindowManagerThreadPriorityBooster sThreadPriorityBooster = new WindowManagerThreadPriorityBooster();
    int mVr2dDisplayId = -1;
    public PowerHalManager mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();
    public WindowManagerDebugger mWindowManagerDebugger = MtkSystemServiceFactory.getInstance().makeWindowManagerDebugger();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (((action.hashCode() == 988075300 && action.equals("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED")) ? (byte) 0 : (byte) -1) == 0) {
                WindowManagerService.this.mKeyguardDisableHandler.sendEmptyMessage(3);
            }
        }
    };
    private final PriorityDump.PriorityDumper mPriorityDumper = new PriorityDump.PriorityDumper() {
        @Override
        public void dumpCritical(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
            WindowManagerService.this.doDump(fileDescriptor, printWriter, new String[]{"-a"}, z);
        }

        @Override
        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
            WindowManagerService.this.doDump(fileDescriptor, printWriter, strArr, z);
        }
    };
    int[] mCurrentProfileIds = new int[0];
    boolean mShowAlertWindowNotifications = true;
    final ArraySet<Session> mSessions = new ArraySet<>();
    final WindowHashMap mWindowMap = new WindowHashMap();
    final ArrayList<AppWindowToken> mFinishedStarting = new ArrayList<>();
    final ArrayList<AppWindowToken> mWindowReplacementTimeouts = new ArrayList<>();
    final ArrayList<WindowState> mResizingWindows = new ArrayList<>();
    final ArrayList<WindowState> mPendingRemove = new ArrayList<>();
    WindowState[] mPendingRemoveTmp = new WindowState[20];
    final ArrayList<WindowState> mDestroySurface = new ArrayList<>();
    final ArrayList<WindowState> mDestroyPreservedSurface = new ArrayList<>();
    ArrayList<WindowState> mLosingFocus = new ArrayList<>();
    final ArrayList<WindowState> mForceRemoves = new ArrayList<>();
    ArrayList<WindowState> mWaitingForDrawn = new ArrayList<>();
    private ArrayList<WindowState> mHidingNonSystemOverlayWindows = new ArrayList<>();
    final float[] mTmpFloats = new float[9];
    final Rect mTmpRect = new Rect();
    final Rect mTmpRect2 = new Rect();
    final Rect mTmpRect3 = new Rect();
    final RectF mTmpRectF = new RectF();
    final Matrix mTmpTransform = new Matrix();
    boolean mDisplayEnabled = false;
    boolean mSystemBooted = false;
    boolean mForceDisplayEnabled = false;
    boolean mShowingBootMessages = false;
    boolean mBootAnimationStopped = false;
    WindowState mLastWakeLockHoldingWindow = null;
    WindowState mLastWakeLockObscuringWindow = null;
    int mDockedStackCreateMode = 0;
    boolean mForceResizableTasks = false;
    boolean mSupportsPictureInPicture = false;
    ArrayList<RotationWatcher> mRotationWatchers = new ArrayList<>();
    final WallpaperVisibilityListeners mWallpaperVisibilityListeners = new WallpaperVisibilityListeners();
    int mSystemDecorLayer = 0;
    final Rect mScreenRect = new Rect();
    boolean mDisplayFrozen = false;
    long mDisplayFreezeTime = 0;
    int mLastDisplayFreezeDuration = 0;
    Object mLastFinishedFreezeSource = null;
    boolean mWaitingForConfig = false;
    boolean mSwitchingUser = false;
    int mWindowsFreezingScreen = 0;
    boolean mClientFreezingScreen = false;
    int mAppsFreezingScreen = 0;
    int mLastStatusBarVisibility = 0;
    int mLastDispatchedSystemUiVisibility = 0;
    boolean mSkipAppTransitionAnimation = false;
    final ArraySet<AppWindowToken> mOpeningApps = new ArraySet<>();
    final ArraySet<AppWindowToken> mClosingApps = new ArraySet<>();
    final UnknownAppVisibilityController mUnknownAppVisibilityController = new UnknownAppVisibilityController(this);
    final H mH = new H();
    final Handler mAnimationHandler = new Handler(AnimationThread.getHandler().getLooper());
    WindowState mCurrentFocus = null;
    WindowState mLastFocus = null;
    private final ArrayList<WindowState> mWinAddedSinceNullFocus = new ArrayList<>();
    private final ArrayList<WindowState> mWinRemovedSinceNullFocus = new ArrayList<>();
    WindowState mInputMethodTarget = null;
    WindowState mInputMethodWindow = null;
    private int mSeamlessRotationCount = 0;
    private boolean mRotatingSeamlessly = false;
    AppWindowToken mFocusedApp = null;
    private float mAnimatorDurationScaleSetting = 1.0f;
    final ArrayMap<AnimationAdapter, SurfaceAnimator> mAnimationTransferMap = new ArrayMap<>();
    final ArrayList<WindowChangeListener> mWindowChangeListeners = new ArrayList<>();
    boolean mWindowsChanged = false;
    final Configuration mTempConfiguration = new Configuration();
    final List<IBinder> mNoAnimationNotifyOnTransitionFinished = new ArrayList();
    SurfaceBuilderFactory mSurfaceBuilderFactory = new SurfaceBuilderFactory() {
        @Override
        public final SurfaceControl.Builder make(SurfaceSession surfaceSession) {
            return WindowManagerService.m32lambda$XZU3HlCFtHp_gydNmNMeRmQMCI(surfaceSession);
        }
    };
    TransactionFactory mTransactionFactory = new TransactionFactory() {
        @Override
        public final SurfaceControl.Transaction make() {
            return WindowManagerService.lambda$hBnABSAsqXWvQ0zKwHWE4BZ3Mc0();
        }
    };
    private final SurfaceControl.Transaction mTransaction = this.mTransactionFactory.make();
    final WindowManagerInternal.AppTransitionListener mActivityManagerAppTransitionNotifier = new WindowManagerInternal.AppTransitionListener() {
        @Override
        public void onAppTransitionCancelledLocked(int i) {
            WindowManagerService.this.mH.sendEmptyMessage(48);
        }

        @Override
        public void onAppTransitionFinishedLocked(IBinder iBinder) {
            WindowManagerService.this.mH.sendEmptyMessage(49);
            AppWindowToken appWindowToken = WindowManagerService.this.mRoot.getAppWindowToken(iBinder);
            if (appWindowToken == null) {
                return;
            }
            if (appWindowToken.mLaunchTaskBehind) {
                try {
                    WindowManagerService.this.mActivityManager.notifyLaunchTaskBehindComplete(appWindowToken.token);
                } catch (RemoteException e) {
                }
                appWindowToken.mLaunchTaskBehind = false;
                return;
            }
            appWindowToken.updateReportedVisibilityLocked();
            if (appWindowToken.mEnteringAnimation) {
                if (WindowManagerService.this.getRecentsAnimationController() != null && WindowManagerService.this.getRecentsAnimationController().isTargetApp(appWindowToken)) {
                    return;
                }
                appWindowToken.mEnteringAnimation = false;
                try {
                    WindowManagerService.this.mActivityManager.notifyEnterAnimationComplete(appWindowToken.token);
                } catch (RemoteException e2) {
                }
            }
        }
    };
    final ArrayList<AppFreezeListener> mAppFreezeListeners = new ArrayList<>();
    final InputMonitor mInputMonitor = new InputMonitor(this);
    private WmsExt mWmsExt = MtkSystemServiceFactory.getInstance().makeWmsExt();

    interface AppFreezeListener {
        void onAppFreezeTimeout();
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface UpdateAnimationScaleMode {
    }

    public interface WindowChangeListener {
        void focusChanged();

        void windowsChanged();
    }

    public static SurfaceControl.Builder m32lambda$XZU3HlCFtHp_gydNmNMeRmQMCI(SurfaceSession surfaceSession) {
        return new SurfaceControl.Builder(surfaceSession);
    }

    public static SurfaceControl.Transaction lambda$hBnABSAsqXWvQ0zKwHWE4BZ3Mc0() {
        return new SurfaceControl.Transaction();
    }

    int getDragLayerLocked() {
        return (this.mPolicy.getWindowLayerFromTypeLw(2016) * 10000) + 1000;
    }

    class RotationWatcher {
        final IBinder.DeathRecipient mDeathRecipient;
        final int mDisplayId;
        final IRotationWatcher mWatcher;

        RotationWatcher(IRotationWatcher iRotationWatcher, IBinder.DeathRecipient deathRecipient, int i) {
            this.mWatcher = iRotationWatcher;
            this.mDeathRecipient = deathRecipient;
            this.mDisplayId = i;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri mAnimationDurationScaleUri;
        private final Uri mDisplayInversionEnabledUri;
        private final Uri mTransitionAnimationScaleUri;
        private final Uri mWindowAnimationScaleUri;

        public SettingsObserver() {
            super(new Handler());
            this.mDisplayInversionEnabledUri = Settings.Secure.getUriFor("accessibility_display_inversion_enabled");
            this.mWindowAnimationScaleUri = Settings.Global.getUriFor("window_animation_scale");
            this.mTransitionAnimationScaleUri = Settings.Global.getUriFor("transition_animation_scale");
            this.mAnimationDurationScaleUri = Settings.Global.getUriFor("animator_duration_scale");
            ContentResolver contentResolver = WindowManagerService.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(this.mDisplayInversionEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mWindowAnimationScaleUri, false, this, -1);
            contentResolver.registerContentObserver(this.mTransitionAnimationScaleUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAnimationDurationScaleUri, false, this, -1);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            int i;
            if (uri == null) {
                return;
            }
            if (this.mDisplayInversionEnabledUri.equals(uri)) {
                WindowManagerService.this.updateCircularDisplayMaskIfNeeded();
                return;
            }
            if (!this.mWindowAnimationScaleUri.equals(uri)) {
                if (this.mTransitionAnimationScaleUri.equals(uri)) {
                    i = 1;
                } else if (this.mAnimationDurationScaleUri.equals(uri)) {
                    i = 2;
                } else {
                    return;
                }
            } else {
                i = 0;
            }
            WindowManagerService.this.mH.sendMessage(WindowManagerService.this.mH.obtainMessage(51, i, 0));
        }
    }

    static void boostPriorityForLockedSection() {
        sThreadPriorityBooster.boost();
    }

    static void resetPriorityAfterLockedSection() {
        sThreadPriorityBooster.reset();
    }

    void openSurfaceTransaction() {
        try {
            Trace.traceBegin(32L, "openSurfaceTransaction");
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    SurfaceControl.openTransaction();
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Trace.traceEnd(32L);
        }
    }

    void closeSurfaceTransaction(String str) {
        try {
            Trace.traceBegin(32L, "closeSurfaceTransaction");
            synchronized (this.mWindowMap) {
                try {
                    try {
                        boostPriorityForLockedSection();
                        traceStateLocked(str);
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                } finally {
                    SurfaceControl.closeTransaction();
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Trace.traceEnd(32L);
        }
    }

    static WindowManagerService getInstance() {
        return sInstance;
    }

    public static WindowManagerService main(final Context context, final InputManagerService inputManagerService, final boolean z, final boolean z2, final boolean z3, final WindowManagerPolicy windowManagerPolicy) {
        DisplayThread.getHandler().runWithScissors(new Runnable() {
            @Override
            public final void run() {
                WindowManagerService.sInstance = new WindowManagerService(context, inputManagerService, z, z2, z3, windowManagerPolicy);
            }
        }, 0L);
        return sInstance;
    }

    private void initPolicy() {
        UiThread.getHandler().runWithScissors(new Runnable() {
            @Override
            public void run() {
                WindowManagerPolicyThread.set(Thread.currentThread(), Looper.myLooper());
                WindowManagerService.this.mPolicy.init(WindowManagerService.this.mContext, WindowManagerService.this, WindowManagerService.this);
            }
        }, 0L);
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new WindowManagerShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    private WindowManagerService(Context context, InputManagerService inputManagerService, boolean z, boolean z2, boolean z3, WindowManagerPolicy windowManagerPolicy) {
        PointerEventDispatcher pointerEventDispatcher;
        this.mDisableTransitionAnimation = false;
        this.mWindowAnimationScaleSetting = 1.0f;
        this.mTransitionAnimationScaleSetting = 1.0f;
        this.mAnimationsDisabled = false;
        this.mMousePositionTracker = new MousePositionTracker();
        LockGuard.installLock(this, 5);
        this.mContext = context;
        this.mHaveInputMethods = z;
        this.mAllowBootMessages = z2;
        this.mOnlyCore = z3;
        this.mLimitedAlphaCompositing = context.getResources().getBoolean(R.^attr-private.minorWeightMin);
        this.mHasPermanentDpad = context.getResources().getBoolean(R.^attr-private.itemColor);
        this.mInTouchMode = context.getResources().getBoolean(R.^attr-private.defaultQueryHint);
        this.mDrawLockTimeoutMillis = context.getResources().getInteger(R.integer.config_burnInProtectionMinHorizontalOffset);
        this.mAllowAnimationsInLowPowerMode = context.getResources().getBoolean(R.^attr-private.accessibilityFocusedDrawable);
        this.mMaxUiWidth = context.getResources().getInteger(R.integer.config_defaultNightDisplayAutoMode);
        this.mDisableTransitionAnimation = context.getResources().getBoolean(R.^attr-private.dreamActivityOpenEnterAnimation);
        this.mInputManager = inputManagerService;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplaySettings = new DisplaySettings();
        this.mDisplaySettings.readSettingsLocked();
        this.mPolicy = windowManagerPolicy;
        this.mAnimator = new WindowAnimator(this);
        this.mRoot = new RootWindowContainer(this);
        this.mWindowPlacerLocked = new WindowSurfacePlacer(this);
        this.mTaskSnapshotController = new TaskSnapshotController(this);
        this.mWindowTracing = WindowTracing.createDefaultAndStartLooper(context);
        LocalServices.addService(WindowManagerPolicy.class, this.mPolicy);
        if (this.mInputManager != null) {
            InputChannel inputChannelMonitorInput = this.mInputManager.monitorInput("WindowManager");
            if (inputChannelMonitorInput == null) {
                pointerEventDispatcher = null;
            } else {
                pointerEventDispatcher = new PointerEventDispatcher(inputChannelMonitorInput);
            }
            this.mPointerEventDispatcher = pointerEventDispatcher;
        } else {
            this.mPointerEventDispatcher = null;
        }
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mKeyguardDisableHandler = new KeyguardDisableHandler(this.mContext, this.mPolicy);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        if (this.mPowerManagerInternal != null) {
            this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
                public int getServiceType() {
                    return 3;
                }

                public void onLowPowerModeChanged(PowerSaveState powerSaveState) {
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            boolean z4 = powerSaveState.batterySaverEnabled;
                            if (WindowManagerService.this.mAnimationsDisabled != z4 && !WindowManagerService.this.mAllowAnimationsInLowPowerMode) {
                                WindowManagerService.this.mAnimationsDisabled = z4;
                                WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                            }
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            });
            this.mAnimationsDisabled = this.mPowerManagerInternal.getLowPowerState(3).batterySaverEnabled;
        }
        this.mScreenFrozenLock = this.mPowerManager.newWakeLock(1, "SCREEN_FROZEN");
        this.mScreenFrozenLock.setReferenceCounted(false);
        this.mAppTransition = new AppTransition(context, this);
        this.mAppTransition.registerListenerLocked(this.mActivityManagerAppTransitionNotifier);
        AnimationHandler animationHandler = new AnimationHandler();
        animationHandler.setProvider(new SfVsyncFrameCallbackProvider());
        this.mBoundsAnimationController = new BoundsAnimationController(context, this.mAppTransition, AnimationThread.getHandler(), animationHandler);
        this.mActivityManager = ActivityManager.getService();
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        AppOpsManager.OnOpChangedListener onOpChangedListener = new AppOpsManager.OnOpChangedInternalListener() {
            public void onOpChanged(int i, String str) {
                WindowManagerService.this.updateAppOpsState();
            }
        };
        this.mAppOps.startWatchingMode(24, (String) null, onOpChangedListener);
        this.mAppOps.startWatchingMode(45, (String) null, onOpChangedListener);
        this.mPmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        intentFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        context.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String[] stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                WindowManagerService.this.updateHiddenWhileSuspendedState(new ArraySet(Arrays.asList(stringArrayExtra)), "android.intent.action.PACKAGES_SUSPENDED".equals(intent.getAction()));
            }
        }, UserHandle.ALL, intentFilter, null, null);
        this.mWindowAnimationScaleSetting = Settings.Global.getFloat(context.getContentResolver(), "window_animation_scale", this.mWindowAnimationScaleSetting);
        this.mTransitionAnimationScaleSetting = Settings.Global.getFloat(context.getContentResolver(), "transition_animation_scale", context.getResources().getFloat(R.dimen.activity_embedding_divider_touch_target_width));
        setAnimatorDurationScale(Settings.Global.getFloat(context.getContentResolver(), "animator_duration_scale", this.mAnimatorDurationScaleSetting));
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter2);
        this.mLatencyTracker = LatencyTracker.getInstance(context);
        this.mSettingsObserver = new SettingsObserver();
        this.mHoldingScreenWakeLock = this.mPowerManager.newWakeLock(536870922, "WindowManager");
        this.mHoldingScreenWakeLock.setReferenceCounted(false);
        this.mSurfaceAnimationRunner = new SurfaceAnimationRunner();
        this.mAllowTheaterModeWakeFromLayout = context.getResources().getBoolean(R.^attr-private.autofillDatasetPickerMaxWidth);
        this.mTaskPositioningController = new TaskPositioningController(this, this.mInputManager, this.mInputMonitor, this.mActivityManager, this.mH.getLooper());
        this.mDragDropController = new DragDropController(this, this.mH.getLooper());
        LocalServices.addService(WindowManagerInternal.class, new LocalService());
    }

    public void onInitReady() {
        initPolicy();
        Watchdog.getInstance().addMonitor(this);
        openSurfaceTransaction();
        try {
            createWatermarkInTransaction();
            closeSurfaceTransaction("createWatermarkInTransaction");
            showEmulatorDisplayOverlayIfNeeded();
            if (this.mWmsExt.isAppResolutionTunerSupport()) {
                this.mWmsExt.loadResolutionTunerAppList();
            }
        } catch (Throwable th) {
            closeSurfaceTransaction("createWatermarkInTransaction");
            throw th;
        }
    }

    public InputMonitor getInputMonitor() {
        return this.mInputMonitor;
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        try {
            return super.onTransact(i, parcel, parcel2, i2);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf("WindowManager", "Window Manager Crash", e);
            }
            throw e;
        }
    }

    static boolean excludeWindowTypeFromTapOutTask(int i) {
        if (i == 2000 || i == 2012 || i == 2019) {
            return true;
        }
        return false;
    }

    public int addWindow(Session session, IWindow iWindow, int i, WindowManager.LayoutParams layoutParams, int i2, int i3, Rect rect, Rect rect2, Rect rect3, Rect rect4, DisplayCutout.ParcelableWrapper parcelableWrapper, InputChannel inputChannel) throws Throwable {
        WindowState windowState;
        DisplayContent displayContent;
        WindowHashMap windowHashMap;
        int i4;
        WindowState windowState2;
        AppWindowToken appWindowToken;
        WindowToken windowToken;
        boolean z;
        AppWindowToken appWindowToken2;
        WindowState windowState3;
        DisplayContent displayContent2;
        boolean z2;
        int i5;
        DisplayFrames displayFrames;
        Rect rect5;
        boolean z3;
        boolean zUpdateFocusedWindowLocked;
        int[] iArr = new int[1];
        int iCheckAddPermission = this.mPolicy.checkAddPermission(layoutParams, iArr);
        if (iCheckAddPermission != 0) {
            return iCheckAddPermission;
        }
        int callingUid = Binder.getCallingUid();
        int i6 = layoutParams.type;
        WindowHashMap windowHashMap2 = this.mWindowMap;
        synchronized (windowHashMap2) {
            try {
                try {
                    boostPriorityForLockedSection();
                    if (!this.mDisplayReady) {
                        throw new IllegalStateException("Display has not been initialialized");
                    }
                    DisplayContent displayContentOrCreate = getDisplayContentOrCreate(i3);
                    if (displayContentOrCreate == null) {
                        Slog.w("WindowManager", "Attempted to add window to a display that does not exist: " + i3 + ".  Aborting.");
                        resetPriorityAfterLockedSection();
                        return -9;
                    }
                    if (!displayContentOrCreate.hasAccess(session.mUid) && !this.mDisplayManagerInternal.isUidPresentOnDisplay(session.mUid, i3)) {
                        Slog.w("WindowManager", "Attempted to add window to a display for which the application does not have access: " + i3 + ".  Aborting.");
                        resetPriorityAfterLockedSection();
                        return -9;
                    }
                    if (this.mWindowMap.containsKey(iWindow.asBinder())) {
                        Slog.w("WindowManager", "Window " + iWindow + " is already added");
                        resetPriorityAfterLockedSection();
                        return -5;
                    }
                    if (i6 < 1000 || i6 > 1999) {
                        windowState = null;
                    } else {
                        WindowState windowStateWindowForClientLocked = windowForClientLocked((Session) null, layoutParams.token, false);
                        if (windowStateWindowForClientLocked == null) {
                            Slog.w("WindowManager", "Attempted to add window with token that is not a window: " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -2;
                        }
                        if (windowStateWindowForClientLocked.mAttrs.type >= 1000 && windowStateWindowForClientLocked.mAttrs.type <= 1999) {
                            Slog.w("WindowManager", "Attempted to add window with token that is a sub-window: " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -2;
                        }
                        windowState = windowStateWindowForClientLocked;
                    }
                    if (i6 == 2030 && !displayContentOrCreate.isPrivate()) {
                        Slog.w("WindowManager", "Attempted to add private presentation window to a non-private display.  Aborting.");
                        resetPriorityAfterLockedSection();
                        return -8;
                    }
                    if (i6 == 2037 && !displayContentOrCreate.getDisplay().isPublicPresentation()) {
                        Slog.w("WindowManager", "Attempted to add presentation window to a non-suitable display.  Aborting.");
                        resetPriorityAfterLockedSection();
                        return -9;
                    }
                    boolean z4 = windowState != null;
                    WindowToken windowToken2 = displayContentOrCreate.getWindowToken(z4 ? windowState.mAttrs.token : layoutParams.token);
                    int i7 = z4 ? windowState.mAttrs.type : i6;
                    if (windowToken2 != null) {
                        WindowState windowState4 = windowState;
                        displayContent = displayContentOrCreate;
                        windowHashMap = windowHashMap2;
                        if (i7 < 1 || i7 > 99) {
                            i4 = i6;
                            if (i7 == 2011) {
                                if (windowToken2.windowType != 2011) {
                                    Slog.w("WindowManager", "Attempted to add input method window with bad token " + layoutParams.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                }
                            } else if (i7 == 2031) {
                                if (windowToken2.windowType != 2031) {
                                    Slog.w("WindowManager", "Attempted to add voice interaction window with bad token " + layoutParams.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                }
                            } else if (i7 == 2013) {
                                if (windowToken2.windowType != 2013) {
                                    Slog.w("WindowManager", "Attempted to add wallpaper window with bad token " + layoutParams.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                }
                            } else if (i7 == 2023) {
                                if (windowToken2.windowType != 2023) {
                                    Slog.w("WindowManager", "Attempted to add Dream window with bad token " + layoutParams.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                }
                            } else if (i7 == 2032) {
                                if (windowToken2.windowType != 2032) {
                                    Slog.w("WindowManager", "Attempted to add Accessibility overlay window with bad token " + layoutParams.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                }
                            } else {
                                if (i4 == 2005) {
                                    boolean zDoesAddToastWindowRequireToken = doesAddToastWindowRequireToken(layoutParams.packageName, callingUid, windowState4);
                                    if (zDoesAddToastWindowRequireToken && windowToken2.windowType != 2005) {
                                        Slog.w("WindowManager", "Attempted to add a toast window with bad token " + layoutParams.token + ".  Aborting.");
                                        resetPriorityAfterLockedSection();
                                        return -1;
                                    }
                                    z = zDoesAddToastWindowRequireToken;
                                    windowToken = windowToken2;
                                    windowState2 = windowState4;
                                    appWindowToken2 = null;
                                    appWindowToken = null;
                                    AppWindowToken appWindowToken3 = appWindowToken2;
                                    WindowToken windowToken3 = windowToken;
                                    int i8 = i4;
                                    windowState3 = new WindowState(this, session, iWindow, windowToken, windowState2, iArr[0], i, layoutParams, i2, session.mUid, session.mCanAddInternalSystemWindow);
                                    if (windowState3.mDeathRecipient == null) {
                                        Slog.w("WindowManager", "Adding window client " + iWindow.asBinder() + " that is dead, aborting.");
                                        resetPriorityAfterLockedSection();
                                        return -4;
                                    }
                                    if (windowState3.getDisplayContent() == null) {
                                        Slog.w("WindowManager", "Adding window to Display that has been removed.");
                                        resetPriorityAfterLockedSection();
                                        return -9;
                                    }
                                    this.mPolicy.adjustWindowParamsLw(windowState3, windowState3.mAttrs, this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE") == 0);
                                    windowState3.setShowToOwnerOnlyLocked(this.mPolicy.checkShowToOwnerOnly(layoutParams));
                                    int iPrepareAddWindowLw = this.mPolicy.prepareAddWindowLw(windowState3, layoutParams);
                                    if (iPrepareAddWindowLw != 0) {
                                        resetPriorityAfterLockedSection();
                                        return iPrepareAddWindowLw;
                                    }
                                    boolean z5 = inputChannel != null && (layoutParams.inputFeatures & 2) == 0;
                                    if (callingUid != 1000) {
                                        Slog.e("WindowManager", "App trying to use insecure INPUT_FEATURE_NO_INPUT_CHANNEL flag. Ignoring");
                                        z5 = true;
                                    }
                                    if (z5) {
                                        windowState3.openInputChannel(inputChannel);
                                    }
                                    if (i8 == 2005) {
                                        if (!getDefaultDisplayContentLocked().canAddToastWindowForUid(callingUid)) {
                                            Slog.w("WindowManager", "Adding more than one toast window for UID at a time.");
                                            resetPriorityAfterLockedSection();
                                            return -5;
                                        }
                                        if (z || (layoutParams.flags & 8) == 0 || this.mCurrentFocus == null || this.mCurrentFocus.mOwnerUid != callingUid) {
                                            this.mH.sendMessageDelayed(this.mH.obtainMessage(52, windowState3), windowState3.mAttrs.hideTimeoutMilliseconds);
                                        }
                                    }
                                    if (this.mCurrentFocus == null) {
                                        this.mWinAddedSinceNullFocus.add(windowState3);
                                    }
                                    if (excludeWindowTypeFromTapOutTask(i8)) {
                                        displayContent2 = displayContent;
                                        displayContent2.mTapExcludedWindows.add(windowState3);
                                    } else {
                                        displayContent2 = displayContent;
                                    }
                                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                                    windowState3.attach();
                                    this.mWindowMap.put(iWindow.asBinder(), windowState3);
                                    windowState3.initAppOpsState();
                                    windowState3.setHiddenWhileSuspended(this.mPmInternal.isPackageSuspended(windowState3.getOwningPackage(), UserHandle.getUserId(windowState3.getOwningUid())));
                                    windowState3.setForceHideNonSystemOverlayWindowIfNeeded(!this.mHidingNonSystemOverlayWindows.isEmpty());
                                    AppWindowToken appWindowTokenAsAppWindowToken = windowToken3.asAppWindowToken();
                                    if (i8 == 3 && appWindowTokenAsAppWindowToken != null) {
                                        appWindowTokenAsAppWindowToken.startingWindow = windowState3;
                                        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                                            Slog.v("WindowManager", "addWindow: " + appWindowTokenAsAppWindowToken + " startingWindow=" + windowState3);
                                        }
                                    }
                                    windowState3.mToken.addWindow(windowState3);
                                    int i9 = 4;
                                    if (i8 == 2011) {
                                        windowState3.mGivenInsetsPending = true;
                                        setInputMethodWindowLocked(windowState3);
                                    } else {
                                        if (i8 != 2012) {
                                            if (i8 == 2013) {
                                                displayContent2.mWallpaperController.clearLastWallpaperTimeoutTime();
                                                displayContent2.pendingLayoutChanges |= 4;
                                            } else if ((layoutParams.flags & DumpState.DUMP_DEXOPT) != 0 || displayContent2.mWallpaperController.isBelowWallpaperTarget(windowState3)) {
                                                displayContent2.pendingLayoutChanges |= 4;
                                            }
                                            z2 = true;
                                            windowState3.applyAdjustForImeIfNeeded();
                                            if (i8 != 2034) {
                                                i5 = i3;
                                                this.mRoot.getDisplayContent(i5).getDockedDividerController().setWindow(windowState3);
                                            } else {
                                                i5 = i3;
                                            }
                                            WindowStateAnimator windowStateAnimator = windowState3.mWinAnimator;
                                            windowStateAnimator.mEnterAnimationPending = true;
                                            windowStateAnimator.mEnteringAnimation = true;
                                            if (appWindowToken3 != null && appWindowToken3.isVisible() && !prepareWindowReplacementTransition(appWindowToken3)) {
                                                prepareNoneTransitionForRelaunching(appWindowToken3);
                                            }
                                            displayFrames = displayContent2.mDisplayFrames;
                                            DisplayInfo displayInfo = displayContent2.getDisplayInfo();
                                            displayFrames.onDisplayInfoUpdated(displayInfo, displayContent2.calculateDisplayCutoutForRotation(displayInfo.rotation));
                                            if (appWindowToken3 != null || appWindowToken3.getTask() == null) {
                                                rect5 = appWindowToken;
                                            } else {
                                                Rect rect6 = this.mTmpRect;
                                                appWindowToken3.getTask().getBounds(this.mTmpRect);
                                                rect5 = rect6;
                                            }
                                            if (this.mPolicy.getLayoutHintLw(windowState3.mAttrs, rect5, displayFrames, rect, rect2, rect3, rect4, parcelableWrapper)) {
                                                i9 = 0;
                                            }
                                            if (this.mInTouchMode) {
                                                i9 |= 1;
                                            }
                                            if (windowState3.mAppToken != null || !windowState3.mAppToken.isClientHidden()) {
                                                i9 |= 2;
                                            }
                                            this.mInputMonitor.setUpdateInputWindowsNeededLw();
                                            if (windowState3.canReceiveKeys()) {
                                                z3 = false;
                                                zUpdateFocusedWindowLocked = false;
                                            } else {
                                                z3 = false;
                                                zUpdateFocusedWindowLocked = updateFocusedWindowLocked(1, false);
                                                if (zUpdateFocusedWindowLocked) {
                                                    z2 = false;
                                                }
                                            }
                                            if (z2) {
                                                displayContent2.computeImeTarget(true);
                                            }
                                            windowState3.getParent().assignChildLayers();
                                            if (zUpdateFocusedWindowLocked) {
                                                this.mInputMonitor.setInputFocusLw(this.mCurrentFocus, z3);
                                            }
                                            this.mInputMonitor.updateInputWindowsLw(z3);
                                            if (!localLOGV || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                                                Slog.v("WindowManager", "addWindow: New client " + iWindow.asBinder() + ": window=" + windowState3 + " Callers=" + Debug.getCallers(5));
                                            }
                                            if (windowState3.isVisibleOrAdding() && updateOrientationFromAppTokensLocked(i5)) {
                                                z3 = true;
                                            }
                                            resetPriorityAfterLockedSection();
                                            if (z3) {
                                                sendNewConfiguration(i5);
                                            }
                                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                                            return i9;
                                        }
                                        displayContent2.computeImeTarget(true);
                                    }
                                    z2 = false;
                                    windowState3.applyAdjustForImeIfNeeded();
                                    if (i8 != 2034) {
                                    }
                                    WindowStateAnimator windowStateAnimator2 = windowState3.mWinAnimator;
                                    windowStateAnimator2.mEnterAnimationPending = true;
                                    windowStateAnimator2.mEnteringAnimation = true;
                                    if (appWindowToken3 != null) {
                                        prepareNoneTransitionForRelaunching(appWindowToken3);
                                    }
                                    displayFrames = displayContent2.mDisplayFrames;
                                    DisplayInfo displayInfo2 = displayContent2.getDisplayInfo();
                                    displayFrames.onDisplayInfoUpdated(displayInfo2, displayContent2.calculateDisplayCutoutForRotation(displayInfo2.rotation));
                                    if (appWindowToken3 != null) {
                                        rect5 = appWindowToken;
                                    }
                                    if (this.mPolicy.getLayoutHintLw(windowState3.mAttrs, rect5, displayFrames, rect, rect2, rect3, rect4, parcelableWrapper)) {
                                    }
                                    if (this.mInTouchMode) {
                                    }
                                    if (windowState3.mAppToken != null) {
                                        i9 |= 2;
                                    }
                                    this.mInputMonitor.setUpdateInputWindowsNeededLw();
                                    if (windowState3.canReceiveKeys()) {
                                    }
                                    if (z2) {
                                    }
                                    windowState3.getParent().assignChildLayers();
                                    if (zUpdateFocusedWindowLocked) {
                                    }
                                    this.mInputMonitor.updateInputWindowsLw(z3);
                                    if (!localLOGV) {
                                        Slog.v("WindowManager", "addWindow: New client " + iWindow.asBinder() + ": window=" + windowState3 + " Callers=" + Debug.getCallers(5));
                                    }
                                    if (windowState3.isVisibleOrAdding()) {
                                        z3 = true;
                                    }
                                    resetPriorityAfterLockedSection();
                                    if (z3) {
                                    }
                                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                                    return i9;
                                }
                                if (i4 == 2035) {
                                    if (windowToken2.windowType != 2035) {
                                        Slog.w("WindowManager", "Attempted to add QS dialog window with bad token " + layoutParams.token + ".  Aborting.");
                                        resetPriorityAfterLockedSection();
                                        return -1;
                                    }
                                } else if (windowToken2.asAppWindowToken() != null) {
                                    Slog.w("WindowManager", "Non-null appWindowToken for system window of rootType=" + i7);
                                    layoutParams.token = null;
                                    windowState2 = windowState4;
                                    appWindowToken = null;
                                    windowToken = new WindowToken(this, iWindow.asBinder(), i4, false, displayContent, session.mCanAddInternalSystemWindow);
                                    appWindowToken2 = appWindowToken;
                                    z = false;
                                    AppWindowToken appWindowToken32 = appWindowToken2;
                                    WindowToken windowToken32 = windowToken;
                                    int i82 = i4;
                                    windowState3 = new WindowState(this, session, iWindow, windowToken, windowState2, iArr[0], i, layoutParams, i2, session.mUid, session.mCanAddInternalSystemWindow);
                                    if (windowState3.mDeathRecipient == null) {
                                    }
                                }
                                windowState2 = windowState4;
                                appWindowToken = null;
                                windowToken = windowToken2;
                                appWindowToken2 = appWindowToken;
                                z = false;
                                AppWindowToken appWindowToken322 = appWindowToken2;
                                WindowToken windowToken322 = windowToken;
                                int i822 = i4;
                                windowState3 = new WindowState(this, session, iWindow, windowToken, windowState2, iArr[0], i, layoutParams, i2, session.mUid, session.mCanAddInternalSystemWindow);
                                if (windowState3.mDeathRecipient == null) {
                                }
                            }
                            windowState2 = windowState4;
                            appWindowToken = null;
                            windowToken = windowToken2;
                            appWindowToken2 = appWindowToken;
                            z = false;
                            AppWindowToken appWindowToken3222 = appWindowToken2;
                            WindowToken windowToken3222 = windowToken;
                            int i8222 = i4;
                            windowState3 = new WindowState(this, session, iWindow, windowToken, windowState2, iArr[0], i, layoutParams, i2, session.mUid, session.mCanAddInternalSystemWindow);
                            if (windowState3.mDeathRecipient == null) {
                            }
                        } else {
                            AppWindowToken appWindowTokenAsAppWindowToken2 = windowToken2.asAppWindowToken();
                            if (appWindowTokenAsAppWindowToken2 == null) {
                                Slog.w("WindowManager", "Attempted to add window with non-application token " + windowToken2 + ".  Aborting.");
                                resetPriorityAfterLockedSection();
                                return -3;
                            }
                            if (appWindowTokenAsAppWindowToken2.removed) {
                                Slog.w("WindowManager", "Attempted to add window with exiting application token " + windowToken2 + ".  Aborting.");
                                resetPriorityAfterLockedSection();
                                return -4;
                            }
                            i4 = i6;
                            if (i4 == 3 && appWindowTokenAsAppWindowToken2.startingWindow != null) {
                                Slog.w("WindowManager", "Attempted to add starting window to token with already existing starting window");
                                resetPriorityAfterLockedSection();
                                return -5;
                            }
                            windowToken = windowToken2;
                            appWindowToken2 = appWindowTokenAsAppWindowToken2;
                            windowState2 = windowState4;
                        }
                    } else {
                        if (i7 >= 1 && i7 <= 99) {
                            Slog.w("WindowManager", "Attempted to add application window with unknown token " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -1;
                        }
                        if (i7 == 2011) {
                            Slog.w("WindowManager", "Attempted to add input method window with unknown token " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -1;
                        }
                        if (i7 == 2031) {
                            Slog.w("WindowManager", "Attempted to add voice interaction window with unknown token " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -1;
                        }
                        if (i7 == 2013) {
                            Slog.w("WindowManager", "Attempted to add wallpaper window with unknown token " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -1;
                        }
                        if (i7 == 2023) {
                            Slog.w("WindowManager", "Attempted to add Dream window with unknown token " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -1;
                        }
                        if (i7 == 2035) {
                            Slog.w("WindowManager", "Attempted to add QS dialog window with unknown token " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -1;
                        }
                        if (i7 == 2032) {
                            Slog.w("WindowManager", "Attempted to add Accessibility overlay window with unknown token " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -1;
                        }
                        if (i6 == 2005 && doesAddToastWindowRequireToken(layoutParams.packageName, callingUid, windowState)) {
                            Slog.w("WindowManager", "Attempted to add a toast window with unknown token " + layoutParams.token + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -1;
                        }
                        WindowState windowState5 = windowState;
                        displayContent = displayContentOrCreate;
                        windowHashMap = windowHashMap2;
                        windowToken = new WindowToken(this, layoutParams.token != null ? layoutParams.token : iWindow.asBinder(), i6, false, displayContentOrCreate, session.mCanAddInternalSystemWindow, (layoutParams.privateFlags & DumpState.DUMP_DEXOPT) != 0);
                        windowState2 = windowState5;
                        i4 = i6;
                        appWindowToken2 = null;
                    }
                    z = false;
                    appWindowToken = null;
                    AppWindowToken appWindowToken32222 = appWindowToken2;
                    WindowToken windowToken32222 = windowToken;
                    int i82222 = i4;
                    windowState3 = new WindowState(this, session, iWindow, windowToken, windowState2, iArr[0], i, layoutParams, i2, session.mUid, session.mCanAddInternalSystemWindow);
                    if (windowState3.mDeathRecipient == null) {
                    }
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private DisplayContent getDisplayContentOrCreate(int i) {
        Display display;
        DisplayContent displayContent = this.mRoot.getDisplayContent(i);
        if (displayContent == null && (display = this.mDisplayManager.getDisplay(i)) != null) {
            return this.mRoot.createDisplayContent(display, null);
        }
        return displayContent;
    }

    private boolean doesAddToastWindowRequireToken(String str, int i, WindowState windowState) {
        ApplicationInfo applicationInfoAsUser;
        if (windowState != null) {
            return windowState.mAppToken != null && windowState.mAppToken.mTargetSdk >= 26;
        }
        try {
            applicationInfoAsUser = this.mContext.getPackageManager().getApplicationInfoAsUser(str, 0, UserHandle.getUserId(i));
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (applicationInfoAsUser.uid == i) {
            return applicationInfoAsUser.targetSdkVersion >= 26;
        }
        throw new SecurityException("Package " + str + " not in UID " + i);
    }

    private boolean prepareWindowReplacementTransition(AppWindowToken appWindowToken) {
        appWindowToken.clearAllDrawn();
        WindowState replacingWindow = appWindowToken.getReplacingWindow();
        if (replacingWindow == null) {
            return false;
        }
        Rect rect = replacingWindow.mVisibleFrame;
        this.mOpeningApps.add(appWindowToken);
        prepareAppTransition(18, true);
        this.mAppTransition.overridePendingAppTransitionClipReveal(rect.left, rect.top, rect.width(), rect.height());
        executeAppTransition();
        return true;
    }

    private void prepareNoneTransitionForRelaunching(AppWindowToken appWindowToken) {
        if (this.mDisplayFrozen && !this.mOpeningApps.contains(appWindowToken) && appWindowToken.isRelaunching()) {
            this.mOpeningApps.add(appWindowToken);
            prepareAppTransition(0, false);
            executeAppTransition();
        }
    }

    boolean isSecureLocked(WindowState windowState) {
        return (windowState.mAttrs.flags & 8192) != 0 || DevicePolicyCache.getInstance().getScreenCaptureDisabled(UserHandle.getUserId(windowState.mOwnerUid));
    }

    public void refreshScreenCaptureDisabled(int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only system can call refreshScreenCaptureDisabled.");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.setSecureSurfaceState(i, DevicePolicyCache.getInstance().getScreenCaptureDisabled(i));
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void removeWindow(Session session, IWindow iWindow) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState windowStateWindowForClientLocked = windowForClientLocked(session, iWindow, false);
                if (windowStateWindowForClientLocked == null) {
                    resetPriorityAfterLockedSection();
                } else {
                    windowStateWindowForClientLocked.removeIfPossible();
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void postWindowRemoveCleanupLocked(WindowState windowState) {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "postWindowRemoveCleanupLocked: " + windowState);
        }
        this.mWindowMap.remove(windowState.mClient.asBinder());
        markForSeamlessRotation(windowState, false);
        windowState.resetAppOpsState();
        if (this.mCurrentFocus == null) {
            this.mWinRemovedSinceNullFocus.add(windowState);
        }
        this.mPendingRemove.remove(windowState);
        this.mResizingWindows.remove(windowState);
        updateNonSystemOverlayWindowsVisibilityIfNeeded(windowState, false);
        this.mWindowsChanged = true;
        if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
            Slog.v("WindowManager", "Final remove of window: " + windowState);
        }
        if (this.mInputMethodWindow == windowState) {
            setInputMethodWindowLocked(null);
        }
        WindowToken windowToken = windowState.mToken;
        AppWindowToken appWindowToken = windowState.mAppToken;
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "Removing " + windowState + " from " + windowToken);
        }
        if (windowToken.isEmpty()) {
            if (!windowToken.mPersistOnEmpty) {
                windowToken.removeImmediately();
            } else if (appWindowToken != null) {
                appWindowToken.firstWindowDrawn = false;
                appWindowToken.clearAllDrawn();
                TaskStack stack = appWindowToken.getStack();
                if (stack != null) {
                    stack.mExitingAppTokens.remove(appWindowToken);
                }
            }
        }
        if (appWindowToken != null) {
            appWindowToken.postWindowRemoveStartingWindowCleanup(windowState);
        }
        DisplayContent displayContent = windowState.getDisplayContent();
        if (windowState.mAttrs.type == 2013) {
            displayContent.mWallpaperController.clearLastWallpaperTimeoutTime();
            displayContent.pendingLayoutChanges |= 4;
        } else if ((windowState.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
            displayContent.pendingLayoutChanges |= 4;
        }
        if (displayContent != null && !this.mWindowPlacerLocked.isInLayout()) {
            displayContent.assignWindowLayers(true);
            if (this.mCurrentFocus == windowState) {
                this.mFocusMayChange = true;
            }
            this.mWindowPlacerLocked.performSurfacePlacement();
            if (windowState.mAppToken != null) {
                windowState.mAppToken.updateReportedVisibilityLocked();
            }
        }
        this.mInputMonitor.updateInputWindowsLw(true);
    }

    void setInputMethodWindowLocked(WindowState windowState) {
        this.mInputMethodWindow = windowState;
        (windowState != null ? windowState.getDisplayContent() : getDefaultDisplayContentLocked()).computeImeTarget(true);
    }

    private void updateHiddenWhileSuspendedState(ArraySet<String> arraySet, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.updateHiddenWhileSuspendedState(arraySet, z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private void updateAppOpsState() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.updateAppOpsState();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    static void logSurface(WindowState windowState, String str, boolean z) {
        String str2 = "  SURFACE " + str + ": " + windowState;
        if (z) {
            logWithStack("WindowManager", str2);
        } else {
            Slog.i("WindowManager", str2);
        }
    }

    static void logSurface(SurfaceControl surfaceControl, String str, String str2) {
        Slog.i("WindowManager", "  SURFACE " + surfaceControl + ": " + str2 + " / " + str);
    }

    static void logWithStack(String str, String str2) {
        RuntimeException runtimeException;
        if (WindowManagerDebugConfig.SHOW_STACK_CRAWLS) {
            runtimeException = new RuntimeException();
            runtimeException.fillInStackTrace();
        } else {
            runtimeException = null;
        }
        Slog.i(str, str2, runtimeException);
    }

    void setTransparentRegionWindow(Session session, IWindow iWindow, Region region) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    WindowState windowStateWindowForClientLocked = windowForClientLocked(session, iWindow, false);
                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                        logSurface(windowStateWindowForClientLocked, "transparentRegionHint=" + region, false);
                    }
                    if (windowStateWindowForClientLocked != null && windowStateWindowForClientLocked.mHasSurface) {
                        windowStateWindowForClientLocked.mWinAnimator.setTransparentRegionHintLocked(region);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void setInsetsWindow(Session session, IWindow iWindow, int i, Rect rect, Rect rect2, Region region) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    WindowState windowStateWindowForClientLocked = windowForClientLocked(session, iWindow, false);
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                        Slog.d("WindowManager", "setInsetsWindow " + windowStateWindowForClientLocked + ", contentInsets=" + windowStateWindowForClientLocked.mGivenContentInsets + " -> " + rect + ", visibleInsets=" + windowStateWindowForClientLocked.mGivenVisibleInsets + " -> " + rect2 + ", touchableRegion=" + windowStateWindowForClientLocked.mGivenTouchableRegion + " -> " + region + ", touchableInsets " + windowStateWindowForClientLocked.mTouchableInsets + " -> " + i);
                    }
                    if (windowStateWindowForClientLocked != null) {
                        windowStateWindowForClientLocked.mGivenInsetsPending = false;
                        windowStateWindowForClientLocked.mGivenContentInsets.set(rect);
                        windowStateWindowForClientLocked.mGivenVisibleInsets.set(rect2);
                        windowStateWindowForClientLocked.mGivenTouchableRegion.set(region);
                        windowStateWindowForClientLocked.mTouchableInsets = i;
                        if (windowStateWindowForClientLocked.mGlobalScale != 1.0f) {
                            windowStateWindowForClientLocked.mGivenContentInsets.scale(windowStateWindowForClientLocked.mGlobalScale);
                            windowStateWindowForClientLocked.mGivenVisibleInsets.scale(windowStateWindowForClientLocked.mGlobalScale);
                            windowStateWindowForClientLocked.mGivenTouchableRegion.scale(windowStateWindowForClientLocked.mGlobalScale);
                        }
                        windowStateWindowForClientLocked.setDisplayLayoutNeeded();
                        this.mWindowPlacerLocked.performSurfacePlacement();
                        if (this.mAccessibilityController != null && windowStateWindowForClientLocked.getDisplayContent().getDisplayId() == 0) {
                            this.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
                        }
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void getWindowDisplayFrame(Session session, IWindow iWindow, Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState windowStateWindowForClientLocked = windowForClientLocked(session, iWindow, false);
                if (windowStateWindowForClientLocked == null) {
                    rect.setEmpty();
                    resetPriorityAfterLockedSection();
                } else {
                    rect.set(windowStateWindowForClientLocked.mDisplayFrame);
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void onRectangleOnScreenRequested(IBinder iBinder, Rect rect) {
        WindowState windowState;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mAccessibilityController != null && (windowState = this.mWindowMap.get(iBinder)) != null && windowState.getDisplayId() == 0) {
                    this.mAccessibilityController.onRectangleOnScreenRequestedLocked(rect);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public IWindowId getWindowId(IBinder iBinder) {
        WindowState.WindowId windowId;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState windowState = this.mWindowMap.get(iBinder);
                windowId = windowState != null ? windowState.mWindowId : null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return windowId;
    }

    public void pokeDrawLock(Session session, IBinder iBinder) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState windowStateWindowForClientLocked = windowForClientLocked(session, iBinder, false);
                if (windowStateWindowForClientLocked != null) {
                    windowStateWindowForClientLocked.pokeDrawLockLw(this.mDrawLockTimeoutMillis);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public int relayoutWindow(Session session, IWindow iWindow, int i, WindowManager.LayoutParams layoutParams, int i2, int i3, int i4, int i5, long j, Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, Rect rect7, DisplayCutout.ParcelableWrapper parcelableWrapper, MergedConfiguration mergedConfiguration, Surface surface) throws Throwable {
        WindowHashMap windowHashMap;
        int i6;
        int i7;
        int i8;
        int i9;
        WindowState windowState;
        int i10;
        long j2;
        boolean z;
        boolean z2;
        boolean zTryStartExitingAnimation;
        boolean z3;
        boolean z4;
        int i11;
        boolean z5;
        int iCreateSurfaceControl;
        boolean z6;
        long j3;
        Surface surface2;
        MergedConfiguration mergedConfiguration2;
        boolean z7;
        boolean z8 = this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0;
        boolean z9 = this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE") == 0;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        WindowHashMap windowHashMap2 = this.mWindowMap;
        synchronized (windowHashMap2) {
            try {
                boostPriorityForLockedSection();
                WindowState windowStateWindowForClientLocked = windowForClientLocked(session, iWindow, false);
                if (windowStateWindowForClientLocked != null) {
                    int displayId = windowStateWindowForClientLocked.getDisplayId();
                    WindowStateAnimator windowStateAnimator = windowStateWindowForClientLocked.mWinAnimator;
                    if (i4 != 8) {
                        windowStateWindowForClientLocked.setRequestedSize(i2, i3);
                    }
                    try {
                        windowStateWindowForClientLocked.setFrameNumber(j);
                        if (layoutParams != null) {
                            this.mPolicy.adjustWindowParamsLw(windowStateWindowForClientLocked, layoutParams, z9);
                            if (i == windowStateWindowForClientLocked.mSeq) {
                                if (this.mWmsExt.isFullScreenCropState(this.mFocusedApp)) {
                                    layoutParams.systemUiVisibility &= -8193;
                                    layoutParams.subtreeSystemUiVisibility &= -8193;
                                }
                                int i12 = layoutParams.systemUiVisibility | layoutParams.subtreeSystemUiVisibility;
                                if ((67043328 & i12) != 0 && !z8) {
                                    i12 &= -67043329;
                                }
                                windowStateWindowForClientLocked.mSystemUiVisibility = i12;
                            }
                            if (windowStateWindowForClientLocked.mAttrs.type != layoutParams.type) {
                                this.mWindowManagerDebugger.debugRelayoutWindow("WindowManager", windowStateWindowForClientLocked, windowStateWindowForClientLocked.mAttrs.type, layoutParams.type);
                                throw new IllegalArgumentException("Window type can not be changed after the window is added.");
                            }
                            if ((layoutParams.privateFlags & 8192) != 0) {
                                layoutParams.x = windowStateWindowForClientLocked.mAttrs.x;
                                layoutParams.y = windowStateWindowForClientLocked.mAttrs.y;
                                layoutParams.width = windowStateWindowForClientLocked.mAttrs.width;
                                layoutParams.height = windowStateWindowForClientLocked.mAttrs.height;
                            }
                            WindowManager.LayoutParams layoutParams2 = windowStateWindowForClientLocked.mAttrs;
                            int i13 = layoutParams.flags ^ layoutParams2.flags;
                            layoutParams2.flags = i13;
                            int iCopyFrom = windowStateWindowForClientLocked.mAttrs.copyFrom(layoutParams);
                            if ((iCopyFrom & 16385) != 0) {
                                windowStateWindowForClientLocked.mLayoutNeeded = true;
                            }
                            if (windowStateWindowForClientLocked.mAppToken != null && ((i13 & DumpState.DUMP_FROZEN) != 0 || (4194304 & i13) != 0)) {
                                windowStateWindowForClientLocked.mAppToken.checkKeyguardFlagsChanged();
                            }
                            if ((33554432 & iCopyFrom) != 0 && this.mAccessibilityController != null && windowStateWindowForClientLocked.getDisplayId() == 0) {
                                this.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
                            }
                            if ((i13 & DumpState.DUMP_FROZEN) != 0) {
                                updateNonSystemOverlayWindowsVisibilityIfNeeded(windowStateWindowForClientLocked, windowStateWindowForClientLocked.mWinAnimator.getShown());
                            }
                            i6 = i13;
                            i7 = iCopyFrom;
                        } else {
                            i6 = 0;
                            i7 = 0;
                        }
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            Slog.v("WindowManager", "Relayout " + windowStateWindowForClientLocked + ": viewVisibility=" + i4 + " req=" + i2 + "x" + i3 + " " + windowStateWindowForClientLocked.mAttrs);
                        }
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            this.mWindowManagerDebugger.debugInputAttr("WindowManager", layoutParams);
                        }
                        windowStateAnimator.mSurfaceDestroyDeferred = (i5 & 2) != 0;
                        windowStateWindowForClientLocked.mEnforceSizeCompat = (windowStateWindowForClientLocked.mAttrs.privateFlags & 128) != 0;
                        if (this.mWmsExt.isAppResolutionTunerSupport()) {
                            i8 = i6;
                            i9 = displayId;
                            j2 = jClearCallingIdentity;
                            i10 = i7;
                            windowState = windowStateWindowForClientLocked;
                            windowHashMap = windowHashMap2;
                            z = true;
                            try {
                                this.mWmsExt.setWindowScaleByWL(windowStateWindowForClientLocked, windowStateWindowForClientLocked.getDisplayInfo(), windowStateWindowForClientLocked.mAttrs, i2, i3);
                            } catch (Throwable th) {
                                th = th;
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } else {
                            i8 = i6;
                            i9 = displayId;
                            windowState = windowStateWindowForClientLocked;
                            i10 = i7;
                            j2 = jClearCallingIdentity;
                            windowHashMap = windowHashMap2;
                            z = true;
                        }
                        if ((i10 & 128) != 0) {
                            windowStateAnimator.mAlpha = layoutParams.alpha;
                        }
                        windowState.setWindowScale(windowState.mRequestedWidth, windowState.mRequestedHeight);
                        if (windowState.mAttrs.surfaceInsets.left != 0 || windowState.mAttrs.surfaceInsets.top != 0 || windowState.mAttrs.surfaceInsets.right != 0 || windowState.mAttrs.surfaceInsets.bottom != 0) {
                            z2 = false;
                            windowStateAnimator.setOpaqueLocked(false);
                        } else {
                            z2 = false;
                        }
                        int i14 = windowState.mViewVisibility;
                        boolean z10 = ((131080 & i8) != 0 || (((i14 == 4 || i14 == 8) && i4 == 0) ? z : z2)) ? z : z2;
                        boolean zIsDefaultDisplay = windowState.isDefaultDisplay();
                        boolean z11 = (!zIsDefaultDisplay || (windowState.mViewVisibility == i4 && (i8 & 8) == 0 && windowState.mRelayoutCalled)) ? z2 : z;
                        boolean z12 = ((windowState.mViewVisibility == i4 || (windowState.mAttrs.flags & DumpState.DUMP_DEXOPT) == 0) ? z2 : z) | ((1048576 & i8) != 0 ? z : z2);
                        if ((i8 & 8192) != 0 && windowStateAnimator.mSurfaceController != null) {
                            windowStateAnimator.mSurfaceController.setSecure(isSecureLocked(windowState));
                        }
                        windowState.mRelayoutCalled = z;
                        windowState.mInRelayout = z;
                        windowState.mViewVisibility = i4;
                        if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                            WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
                            if (WindowManagerDebugger.WMS_DEBUG_LOG_OFF) {
                                RuntimeException runtimeException = new RuntimeException();
                                runtimeException.fillInStackTrace();
                                Slog.i("WindowManager", "Relayout " + windowState + ": oldVis=" + i14 + " newVis=" + i4, runtimeException);
                            }
                        }
                        windowState.setDisplayLayoutNeeded();
                        windowState.mGivenInsetsPending = (i5 & 1) != 0 ? z : z2;
                        WindowManagerDebugger windowManagerDebugger2 = this.mWindowManagerDebugger;
                        if (WindowManagerDebugger.WMS_DEBUG_USER) {
                            zTryStartExitingAnimation = z11;
                            z3 = zIsDefaultDisplay;
                            i11 = 4;
                            z4 = z2;
                            this.mWindowManagerDebugger.debugViewVisibility("WindowManager", windowState, i4, i14, zTryStartExitingAnimation);
                        } else {
                            zTryStartExitingAnimation = z11;
                            z3 = zIsDefaultDisplay;
                            z4 = z2;
                            i11 = 4;
                        }
                        boolean z13 = (i4 == 0 && (windowState.mAppToken == null || windowState.mAttrs.type == 3 || !windowState.mAppToken.isClientHidden())) ? z : z4;
                        if (z13 || !windowStateAnimator.hasSurface() || windowState.mAnimatingExit) {
                            z5 = zTryStartExitingAnimation;
                            iCreateSurfaceControl = z4;
                        } else {
                            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                Slog.i("WindowManager", "Relayout invis " + windowState + ": mAnimatingExit=" + windowState.mAnimatingExit);
                            }
                            if (!windowState.mWillReplaceWindow) {
                                zTryStartExitingAnimation = tryStartExitingAnimation(windowState, windowStateAnimator, z3, zTryStartExitingAnimation);
                            }
                            z5 = zTryStartExitingAnimation;
                            iCreateSurfaceControl = i11;
                        }
                        this.mWindowPlacerLocked.performSurfacePlacement(z);
                        if (z13) {
                            Trace.traceBegin(32L, "relayoutWindow: viewVisibility_1");
                            int iRelayoutVisibleWindow = windowState.relayoutVisibleWindow(iCreateSurfaceControl, i10, i14);
                            z6 = z4;
                            surface2 = surface;
                            try {
                                iCreateSurfaceControl = createSurfaceControl(surface2, iRelayoutVisibleWindow, windowState, windowStateAnimator);
                                if ((iCreateSurfaceControl & 2) != 0) {
                                    z5 = z3;
                                }
                                if (windowState.mAttrs.type == 2011 && this.mInputMethodWindow == null) {
                                    setInputMethodWindowLocked(windowState);
                                    z10 = z;
                                }
                                windowState.adjustStartingWindowFlags();
                                Trace.traceEnd(32L);
                                j3 = j2;
                            } catch (Exception e) {
                                this.mInputMonitor.updateInputWindowsLw(z);
                                Slog.w("WindowManager", "Exception thrown when creating surface for client " + iWindow + " (" + ((Object) windowState.mAttrs.getTitle()) + ")", e);
                                Binder.restoreCallingIdentity(j2);
                                resetPriorityAfterLockedSection();
                                return z6 ? 1 : 0;
                            }
                        } else {
                            z6 = z4;
                            j3 = j2;
                            surface2 = surface;
                            Trace.traceBegin(32L, "relayoutWindow: viewVisibility_2");
                            windowStateAnimator.mEnterAnimationPending = z6;
                            windowStateAnimator.mEnteringAnimation = z6;
                            if (i4 == 0 && windowStateAnimator.hasSurface()) {
                                Trace.traceBegin(32L, "relayoutWindow: getSurface");
                                windowStateAnimator.mSurfaceController.getSurface(surface2);
                                Trace.traceEnd(32L);
                            } else {
                                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                    Slog.i("WindowManager", "Releasing surface in: " + windowState);
                                }
                                try {
                                    Trace.traceBegin(32L, "wmReleaseOutSurface_" + ((Object) windowState.mAttrs.getTitle()));
                                    surface.release();
                                    Trace.traceEnd(32L);
                                } catch (Throwable th2) {
                                    Trace.traceEnd(32L);
                                    throw th2;
                                }
                            }
                            Trace.traceEnd(32L);
                        }
                        if (z5 && updateFocusedWindowLocked(3, z6)) {
                            z10 = z6;
                        }
                        boolean z14 = (iCreateSurfaceControl & 2) != 0 ? true : z6;
                        DisplayContent displayContent = windowState.getDisplayContent();
                        if (z10) {
                            displayContent.computeImeTarget(true);
                            if (z14) {
                                displayContent.assignWindowLayers(z6);
                            }
                        }
                        if (z12) {
                            windowState.getDisplayContent().pendingLayoutChanges |= 4;
                        }
                        if (windowState.mAppToken != null) {
                            this.mUnknownAppVisibilityController.notifyRelayouted(windowState.mAppToken);
                        }
                        Trace.traceBegin(32L, "relayoutWindow: updateOrientationFromAppTokens");
                        int i15 = i9;
                        boolean zUpdateOrientationFromAppTokensLocked = updateOrientationFromAppTokensLocked(i15);
                        Trace.traceEnd(32L);
                        if (z14 && windowState.mIsWallpaper) {
                            DisplayInfo displayInfo = windowState.getDisplayContent().getDisplayInfo();
                            displayContent.mWallpaperController.updateWallpaperOffset(windowState, displayInfo.logicalWidth, displayInfo.logicalHeight, false);
                        }
                        if (windowState.mAppToken != null) {
                            windowState.mAppToken.updateReportedVisibilityLocked();
                        }
                        if (windowStateAnimator.mReportSurfaceResized) {
                            windowStateAnimator.mReportSurfaceResized = false;
                            iCreateSurfaceControl |= 32;
                        }
                        if (this.mPolicy.isNavBarForcedShownLw(windowState)) {
                            iCreateSurfaceControl |= 64;
                        }
                        if (!windowState.isGoneForLayoutLw()) {
                            windowState.mResizedWhileGone = false;
                        }
                        if (z13) {
                            mergedConfiguration2 = mergedConfiguration;
                            windowState.getMergedConfiguration(mergedConfiguration2);
                        } else {
                            mergedConfiguration2 = mergedConfiguration;
                            windowState.getLastReportedMergedConfiguration(mergedConfiguration2);
                        }
                        windowState.setLastReportedMergedConfiguration(mergedConfiguration2);
                        windowState.updateLastInsetValues();
                        rect.set(windowState.mCompatFrame);
                        rect2.set(windowState.mOverscanInsets);
                        rect3.set(windowState.mContentInsets);
                        windowState.mLastRelayoutContentInsets.set(windowState.mContentInsets);
                        rect4.set(windowState.mVisibleInsets);
                        rect5.set(windowState.mStableInsets);
                        parcelableWrapper.set(windowState.mDisplayCutout.getDisplayCutout());
                        rect6.set(windowState.mOutsets);
                        rect7.set(windowState.getBackdropFrame(windowState.mFrame));
                        if (localLOGV) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Relayout given client ");
                            sb.append(iWindow.asBinder());
                            sb.append(", requestedWidth=");
                            z7 = true;
                            sb.append(i2);
                            sb.append(", requestedHeight=");
                            sb.append(i3);
                            sb.append(", viewVisibility=");
                            sb.append(i4);
                            sb.append("\nRelayout returning frame=");
                            sb.append(rect);
                            sb.append(", surface=");
                            sb.append(surface2);
                            Slog.v("WindowManager", sb.toString());
                        } else {
                            z7 = true;
                        }
                        if (localLOGV || WindowManagerDebugConfig.DEBUG_FOCUS) {
                            Slog.v("WindowManager", "Relayout of " + windowState + ": focusMayChange=" + z5);
                        }
                        int i16 = (this.mInTouchMode ? 1 : 0) | iCreateSurfaceControl;
                        this.mInputMonitor.updateInputWindowsLw(z7);
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            Slog.v("WindowManager", "Relayout complete " + windowState + ": outFrame=" + rect.toShortString());
                        }
                        windowState.mInRelayout = false;
                        resetPriorityAfterLockedSection();
                        if (zUpdateOrientationFromAppTokensLocked) {
                            Trace.traceBegin(32L, "relayoutWindow: sendNewConfiguration");
                            sendNewConfiguration(i15);
                            Trace.traceEnd(32L);
                        }
                        Binder.restoreCallingIdentity(j3);
                        return i16;
                    } catch (Throwable th3) {
                        th = th3;
                        windowHashMap = windowHashMap2;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                windowHashMap = windowHashMap2;
            }
        }
        resetPriorityAfterLockedSection();
        return 0;
    }

    private boolean tryStartExitingAnimation(WindowState windowState, WindowStateAnimator windowStateAnimator, boolean z, boolean z2) {
        int i;
        if (windowState.mAttrs.type == 3) {
            i = 5;
        } else {
            i = 2;
        }
        if (windowState.isWinVisibleLw() && windowStateAnimator.applyAnimationLocked(i, false)) {
            windowState.mAnimatingExit = true;
        } else {
            if (windowState.mWinAnimator.isAnimationSet() || windowState.getDisplayContent().mWallpaperController.isWallpaperTarget(windowState)) {
                windowState.mAnimatingExit = true;
            } else {
                if (this.mInputMethodWindow == windowState) {
                    setInputMethodWindowLocked(null);
                }
                boolean z3 = windowState.mAppToken != null ? windowState.mAppToken.mAppStopped : true;
                windowState.mDestroying = true;
                windowState.destroySurface(false, z3);
            }
            z = z2;
        }
        if (this.mAccessibilityController != null && windowState.getDisplayId() == 0) {
            this.mAccessibilityController.onWindowTransitionLocked(windowState, i);
        }
        SurfaceControl.openTransaction();
        windowStateAnimator.detachChildren();
        SurfaceControl.closeTransaction();
        return z;
    }

    private int createSurfaceControl(Surface surface, int i, WindowState windowState, WindowStateAnimator windowStateAnimator) {
        if (!windowState.mHasSurface) {
            i |= 4;
        }
        try {
            Trace.traceBegin(32L, "createSurfaceControl");
            WindowSurfaceController windowSurfaceControllerCreateSurfaceLocked = windowStateAnimator.createSurfaceLocked(windowState.mAttrs.type, windowState.mOwnerUid);
            if (windowSurfaceControllerCreateSurfaceLocked != null) {
                windowSurfaceControllerCreateSurfaceLocked.getSurface(surface);
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                    Slog.i("WindowManager", "  OUT SURFACE " + surface + ": copied");
                }
            } else {
                Slog.w("WindowManager", "Failed to create surface control for " + windowState);
                surface.release();
            }
            return i;
        } finally {
            Trace.traceEnd(32L);
        }
    }

    public boolean outOfMemoryWindow(Session session, IWindow iWindow) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    WindowState windowStateWindowForClientLocked = windowForClientLocked(session, iWindow, false);
                    if (windowStateWindowForClientLocked != null) {
                        boolean zReclaimSomeSurfaceMemory = this.mRoot.reclaimSomeSurfaceMemory(windowStateWindowForClientLocked.mWinAnimator, "from-client", false);
                        resetPriorityAfterLockedSection();
                        return zReclaimSomeSurfaceMemory;
                    }
                    resetPriorityAfterLockedSection();
                    return false;
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void finishDrawingWindow(Session session, IWindow iWindow) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    WindowState windowStateWindowForClientLocked = windowForClientLocked(session, iWindow, false);
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("finishDrawingWindow: ");
                        sb.append(windowStateWindowForClientLocked);
                        sb.append(" mDrawState=");
                        sb.append(windowStateWindowForClientLocked != null ? windowStateWindowForClientLocked.mWinAnimator.drawStateToString() : "null");
                        Slog.d("WindowManager", sb.toString());
                    }
                    if (windowStateWindowForClientLocked != null && windowStateWindowForClientLocked.mWinAnimator.finishDrawingLocked()) {
                        if ((windowStateWindowForClientLocked.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
                            windowStateWindowForClientLocked.getDisplayContent().pendingLayoutChanges |= 4;
                        }
                        windowStateWindowForClientLocked.setDisplayLayoutNeeded();
                        this.mWindowPlacerLocked.requestTraversal();
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    boolean checkCallingPermission(String str, String str2) {
        if (Binder.getCallingPid() == Process.myPid() || this.mContext.checkCallingPermission(str) == 0) {
            return true;
        }
        Slog.w("WindowManager", "Permission Denial: " + str2 + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + str);
        return false;
    }

    public void addWindowToken(IBinder iBinder, int i, int i2) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "addWindowToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i2);
                WindowToken windowToken = displayContent.getWindowToken(iBinder);
                if (windowToken != null) {
                    Slog.w("WindowManager", "addWindowToken: Attempted to add binder token: " + iBinder + " for already created window token: " + windowToken + " displayId=" + i2);
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (i == 2013) {
                    new WallpaperWindowToken(this, iBinder, true, displayContent, true);
                } else {
                    new WindowToken(this, iBinder, i, true, displayContent, true);
                }
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void removeWindowToken(IBinder iBinder, int i) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "removeWindowToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                    if (displayContent == null) {
                        Slog.w("WindowManager", "removeWindowToken: Attempted to remove token: " + iBinder + " for non-exiting displayId=" + i);
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    if (displayContent.removeWindowToken(iBinder) != null) {
                        this.mInputMonitor.updateInputWindowsLw(true);
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    Slog.w("WindowManager", "removeWindowToken: Attempted to remove non-existing token: " + iBinder);
                    resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public Configuration updateOrientationFromAppTokens(Configuration configuration, IBinder iBinder, int i) {
        return updateOrientationFromAppTokens(configuration, iBinder, i, false);
    }

    public Configuration updateOrientationFromAppTokens(Configuration configuration, IBinder iBinder, int i, boolean z) {
        Configuration configurationUpdateOrientationFromAppTokensLocked;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "updateOrientationFromAppTokens()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    configurationUpdateOrientationFromAppTokensLocked = updateOrientationFromAppTokensLocked(configuration, iBinder, i, z);
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            return configurationUpdateOrientationFromAppTokensLocked;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Configuration updateOrientationFromAppTokensLocked(Configuration configuration, IBinder iBinder, int i, boolean z) {
        AppWindowToken appWindowToken;
        if (!this.mDisplayReady) {
            return null;
        }
        if (updateOrientationFromAppTokensLocked(i, z)) {
            if (iBinder != null && !this.mRoot.mOrientationChangeComplete && (appWindowToken = this.mRoot.getAppWindowToken(iBinder)) != null) {
                appWindowToken.startFreezingScreen();
            }
            return computeNewConfigurationLocked(i);
        }
        if (configuration == null) {
            return null;
        }
        this.mTempConfiguration.unset();
        this.mTempConfiguration.updateFrom(configuration);
        DisplayContent displayContent = this.mRoot.getDisplayContent(i);
        displayContent.computeScreenConfiguration(this.mTempConfiguration);
        if (configuration.diff(this.mTempConfiguration) == 0) {
            return null;
        }
        this.mWaitingForConfig = true;
        displayContent.setLayoutNeeded();
        int[] iArr = new int[2];
        this.mPolicy.selectRotationAnimationLw(iArr);
        startFreezingDisplayLocked(iArr[0], iArr[1], displayContent);
        return new Configuration(this.mTempConfiguration);
    }

    boolean updateOrientationFromAppTokensLocked(int i) {
        return updateOrientationFromAppTokensLocked(i, false);
    }

    boolean updateOrientationFromAppTokensLocked(int i, boolean z) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            DisplayContent displayContent = this.mRoot.getDisplayContent(i);
            int orientation = displayContent.getOrientation();
            if (orientation != displayContent.getLastOrientation() || z) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "updateOrientation: req= " + orientation + ", mLastOrientation= " + displayContent.getLastOrientation(), new Throwable("updateOrientation"));
                }
                displayContent.setLastOrientation(orientation);
                if (displayContent.isDefaultDisplay) {
                    this.mPolicy.setCurrentOrientationLw(orientation);
                }
                return displayContent.updateRotationUnchecked(z);
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    boolean rotationNeedsUpdateLocked() {
        DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
        int lastOrientation = defaultDisplayContentLocked.getLastOrientation();
        int rotation = defaultDisplayContentLocked.getRotation();
        boolean altOrientation = defaultDisplayContentLocked.getAltOrientation();
        int iRotationForOrientationLw = this.mPolicy.rotationForOrientationLw(lastOrientation, rotation, true);
        return (rotation == iRotationForOrientationLw && altOrientation == (this.mPolicy.rotationHasCompatibleMetricsLw(lastOrientation, iRotationForOrientationLw) ^ true)) ? false : true;
    }

    public int[] setNewDisplayOverrideConfiguration(Configuration configuration, int i) {
        int[] displayOverrideConfigurationIfNeeded;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setNewDisplayOverrideConfiguration()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mWaitingForConfig) {
                    this.mWaitingForConfig = false;
                    this.mLastFinishedFreezeSource = "new-config";
                }
                displayOverrideConfigurationIfNeeded = this.mRoot.setDisplayOverrideConfigurationIfNeeded(configuration, i);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return displayOverrideConfigurationIfNeeded;
    }

    void setFocusTaskRegionLocked(AppWindowToken appWindowToken) {
        Task task = this.mFocusedApp != null ? this.mFocusedApp.getTask() : null;
        Task task2 = appWindowToken != null ? appWindowToken.getTask() : null;
        DisplayContent displayContent = task != null ? task.getDisplayContent() : null;
        DisplayContent displayContent2 = task2 != null ? task2.getDisplayContent() : null;
        if (displayContent2 != null && displayContent2 != displayContent) {
            displayContent2.setTouchExcludeRegion(null);
        }
        if (displayContent != null) {
            displayContent.setTouchExcludeRegion(task);
        }
    }

    public void setFocusedApp(IBinder iBinder, boolean z) {
        AppWindowToken appWindowToken;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setFocusedApp()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (iBinder == null) {
                    if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                        Slog.v("WindowManager", "Clearing focused app, was " + this.mFocusedApp);
                    }
                    appWindowToken = null;
                } else {
                    AppWindowToken appWindowToken2 = this.mRoot.getAppWindowToken(iBinder);
                    if (appWindowToken2 == null) {
                        Slog.w("WindowManager", "Attempted to set focus to non-existing app token: " + iBinder);
                    }
                    if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                        Slog.v("WindowManager", "Set focused app to: " + appWindowToken2 + " old focus=" + this.mFocusedApp + " moveFocusNow=" + z);
                    }
                    appWindowToken = appWindowToken2;
                }
                boolean z2 = this.mFocusedApp != appWindowToken;
                if (z2) {
                    AppWindowToken appWindowToken3 = this.mFocusedApp;
                    this.mFocusedApp = appWindowToken;
                    this.mInputMonitor.setFocusedAppLw(appWindowToken);
                    setFocusTaskRegionLocked(appWindowToken3);
                    if (this.mWmsExt.isFullScreenCropState(this.mFocusedApp)) {
                        Slog.w("WindowManager", " update display when set new focus");
                        DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
                        defaultDisplayContentLocked.updateDisplayAndOrientation(defaultDisplayContentLocked.getConfiguration().uiMode);
                    }
                }
                if (z && z2) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    updateFocusedWindowLocked(0, true);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void prepareAppTransition(int i, boolean z) {
        prepareAppTransition(i, z, 0, false);
    }

    public void prepareAppTransition(int i, boolean z, int i2, boolean z2) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "prepareAppTransition()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                boolean zPrepareAppTransitionLocked = this.mAppTransition.prepareAppTransitionLocked(i, z, i2, z2);
                DisplayContent displayContent = this.mRoot.getDisplayContent(0);
                if (zPrepareAppTransitionLocked && displayContent != null && displayContent.okToAnimate()) {
                    this.mSkipAppTransitionAnimation = false;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public int getPendingAppTransition() {
        return this.mAppTransition.getAppTransition();
    }

    public void overridePendingAppTransition(String str, int i, int i2, IRemoteCallback iRemoteCallback) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransition(str, i, i2, iRemoteCallback);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionScaleUp(int i, int i2, int i3, int i4) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionScaleUp(i, i2, i3, i4);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionClipReveal(int i, int i2, int i3, int i4) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionClipReveal(i, i2, i3, i4);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionThumb(GraphicBuffer graphicBuffer, int i, int i2, IRemoteCallback iRemoteCallback, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionThumb(graphicBuffer, i, i2, iRemoteCallback, z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionAspectScaledThumb(GraphicBuffer graphicBuffer, int i, int i2, int i3, int i4, IRemoteCallback iRemoteCallback, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionAspectScaledThumb(graphicBuffer, i, i2, i3, i4, iRemoteCallback, z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] appTransitionAnimationSpecArr, IRemoteCallback iRemoteCallback, IRemoteCallback iRemoteCallback2, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionMultiThumb(appTransitionAnimationSpecArr, iRemoteCallback, iRemoteCallback2, z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionStartCrossProfileApps() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionStartCrossProfileApps();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionInPlace(String str, int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overrideInPlaceAppTransition(str, i);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture, IRemoteCallback iRemoteCallback, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionMultiThumbFuture(iAppTransitionAnimationSpecsFuture, iRemoteCallback, z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) {
        if (!checkCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "overridePendingAppTransitionRemote()")) {
            throw new SecurityException("Requires CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionRemote(remoteAnimationAdapter);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void endProlongedAnimations() {
    }

    public void executeAppTransition() {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "executeAppTransition()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.w("WindowManager", "Execute app transition: " + this.mAppTransition + " Callers=" + Debug.getCallers(5));
                }
                if (this.mAppTransition.isTransitionSet()) {
                    this.mAppTransition.setReady();
                    this.mWindowPlacerLocked.requestTraversal();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void initializeRecentsAnimation(int i, IRecentsAnimationRunner iRecentsAnimationRunner, RecentsAnimationController.RecentsAnimationCallbacks recentsAnimationCallbacks, int i2, SparseBooleanArray sparseBooleanArray) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRecentsAnimationController = new RecentsAnimationController(this, iRecentsAnimationRunner, recentsAnimationCallbacks, i2);
                this.mAppTransition.updateBooster();
                this.mRecentsAnimationController.initialize(i, sparseBooleanArray);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public RecentsAnimationController getRecentsAnimationController() {
        return this.mRecentsAnimationController;
    }

    public boolean canStartRecentsAnimation() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mAppTransition.isTransitionSet()) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void cancelRecentsAnimationSynchronously(@RecentsAnimationController.ReorderMode int i, String str) {
        if (this.mRecentsAnimationController != null) {
            this.mRecentsAnimationController.cancelAnimationSynchronously(i, str);
        }
    }

    public void cleanupRecentsAnimation(@RecentsAnimationController.ReorderMode int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mRecentsAnimationController != null) {
                    this.mRecentsAnimationController.cleanupAnimation(i);
                    this.mRecentsAnimationController = null;
                    this.mAppTransition.updateBooster();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setAppFullscreen(IBinder iBinder, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                if (appWindowToken != null) {
                    appWindowToken.setFillsParent(z);
                    setWindowOpaqueLocked(iBinder, z);
                    this.mWindowPlacerLocked.requestTraversal();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setWindowOpaque(IBinder iBinder, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                setWindowOpaqueLocked(iBinder, z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private void setWindowOpaqueLocked(IBinder iBinder, boolean z) {
        WindowState windowStateFindMainWindow;
        AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
        if (appWindowToken != null && (windowStateFindMainWindow = appWindowToken.findMainWindow()) != null) {
            windowStateFindMainWindow.mWinAnimator.setOpaqueLocked(z);
        }
    }

    public void setDockedStackCreateState(int i, Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                setDockedStackCreateStateLocked(i, rect);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void setDockedStackCreateStateLocked(int i, Rect rect) {
        this.mDockedStackCreateMode = i;
        this.mDockedStackCreateBounds = rect;
    }

    public void checkSplitScreenMinimizedChanged(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().checkMinimizeChanged(z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean isValidPictureInPictureAspectRatio(int i, float f) {
        return this.mRoot.getDisplayContent(i).getPinnedStackController().isValidPictureInPictureAspectRatio(f);
    }

    @Override
    public void getStackBounds(int i, int i2, Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                TaskStack stack = this.mRoot.getStack(i, i2);
                if (stack != null) {
                    stack.getBounds(rect);
                    resetPriorityAfterLockedSection();
                } else {
                    rect.setEmpty();
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @Override
    public void notifyShowingDreamChanged() {
        notifyKeyguardFlagsChanged(null);
    }

    @Override
    public WindowManagerPolicy.WindowState getInputMethodWindowLw() {
        return this.mInputMethodWindow;
    }

    @Override
    public void notifyKeyguardTrustedChanged() {
        this.mH.sendEmptyMessage(57);
    }

    @Override
    public void screenTurningOff(WindowManagerPolicy.ScreenOffListener screenOffListener) {
        this.mTaskSnapshotController.screenTurningOff(screenOffListener);
    }

    @Override
    public void triggerAnimationFailsafe() {
        this.mH.sendEmptyMessage(60);
    }

    @Override
    public void onKeyguardShowingAndNotOccludedChanged() {
        this.mH.sendEmptyMessage(61);
    }

    public void deferSurfaceLayout() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowPlacerLocked.deferLayout();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void continueSurfaceLayout() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowPlacerLocked.continueLayout();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean containsShowWhenLockedWindow(IBinder iBinder) {
        boolean z;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                z = appWindowToken != null && appWindowToken.containsShowWhenLockedWindow();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return z;
    }

    public boolean containsDismissKeyguardWindow(IBinder iBinder) {
        boolean z;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                z = appWindowToken != null && appWindowToken.containsDismissKeyguardWindow();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return z;
    }

    void notifyKeyguardFlagsChanged(final Runnable runnable) {
        Runnable runnable2;
        if (runnable != null) {
            runnable2 = new Runnable() {
                @Override
                public final void run() {
                    WindowManagerService.lambda$notifyKeyguardFlagsChanged$1(this.f$0, runnable);
                }
            };
        } else {
            runnable2 = null;
        }
        this.mH.obtainMessage(56, runnable2).sendToTarget();
    }

    public static void lambda$notifyKeyguardFlagsChanged$1(WindowManagerService windowManagerService, Runnable runnable) {
        synchronized (windowManagerService.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                runnable.run();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean isKeyguardTrusted() {
        boolean zIsKeyguardTrustedLw;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                zIsKeyguardTrustedLw = this.mPolicy.isKeyguardTrustedLw();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zIsKeyguardTrustedLw;
    }

    public void setKeyguardGoingAway(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mKeyguardGoingAway = z;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setKeyguardOrAodShowingOnDefaultDisplay(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mKeyguardOrAodShowingOnDefaultDisplay = z;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void startFreezingScreen(int i, int i2) {
        if (!checkCallingPermission("android.permission.FREEZE_SCREEN", "startFreezingScreen()")) {
            throw new SecurityException("Requires FREEZE_SCREEN permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (!this.mClientFreezingScreen) {
                    this.mClientFreezingScreen = true;
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        startFreezingDisplayLocked(i, i2);
                        this.mH.removeMessages(30);
                        this.mH.sendEmptyMessageDelayed(30, 5000L);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void stopFreezingScreen() {
        if (!checkCallingPermission("android.permission.FREEZE_SCREEN", "stopFreezingScreen()")) {
            throw new SecurityException("Requires FREEZE_SCREEN permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mClientFreezingScreen) {
                    this.mClientFreezingScreen = false;
                    this.mLastFinishedFreezeSource = "client";
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        stopFreezingDisplayLocked();
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void disableKeyguard(IBinder iBinder, String str) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        if (Binder.getCallingUid() != 1000 && isKeyguardSecure()) {
            Log.d("WindowManager", "current mode is SecurityMode, ignore disableKeyguard");
        } else if (!isCurrentProfileLocked(UserHandle.getCallingUserId())) {
            Log.d("WindowManager", "non-current profiles, ignore disableKeyguard");
        } else {
            if (iBinder == null) {
                throw new IllegalArgumentException("token == null");
            }
            this.mKeyguardDisableHandler.sendMessage(this.mKeyguardDisableHandler.obtainMessage(1, new Pair(iBinder, str)));
        }
    }

    public void reenableKeyguard(IBinder iBinder) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        if (iBinder == null) {
            throw new IllegalArgumentException("token == null");
        }
        this.mKeyguardDisableHandler.sendMessage(this.mKeyguardDisableHandler.obtainMessage(2, iBinder));
    }

    public void exitKeyguardSecurely(final IOnKeyguardExitResult iOnKeyguardExitResult) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        if (iOnKeyguardExitResult == null) {
            throw new IllegalArgumentException("callback == null");
        }
        this.mPolicy.exitKeyguardSecurely(new WindowManagerPolicy.OnKeyguardExitResult() {
            @Override
            public void onKeyguardExitResult(boolean z) {
                try {
                    iOnKeyguardExitResult.onKeyguardExitResult(z);
                } catch (RemoteException e) {
                }
            }
        });
    }

    public boolean isKeyguardLocked() {
        return this.mPolicy.isKeyguardLocked();
    }

    public boolean isKeyguardShowingAndNotOccluded() {
        return this.mPolicy.isKeyguardShowingAndNotOccluded();
    }

    public boolean isKeyguardSecure() {
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mPolicy.isKeyguardSecure(callingUserId);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean isShowingDream() {
        boolean zIsShowingDreamLw;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                zIsShowingDreamLw = this.mPolicy.isShowingDreamLw();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zIsShowingDreamLw;
    }

    public void dismissKeyguard(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
        if (!checkCallingPermission("android.permission.CONTROL_KEYGUARD", "dismissKeyguard")) {
            throw new SecurityException("Requires CONTROL_KEYGUARD permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.dismissKeyguardLw(iKeyguardDismissCallback, charSequence);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void onKeyguardOccludedChanged(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.onKeyguardOccludedChangedLw(z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setSwitchingUser(boolean z) {
        if (!checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "setSwitchingUser()")) {
            throw new SecurityException("Requires INTERACT_ACROSS_USERS_FULL permission");
        }
        this.mPolicy.setSwitchingUser(z);
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mSwitchingUser = z;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void showGlobalActions() {
        this.mPolicy.showGlobalActions();
    }

    public void closeSystemDialogs(String str) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.closeSystemDialogs(str);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    static float fixScale(float f) {
        if (f < 0.0f) {
            f = 0.0f;
        } else if (f > 20.0f) {
            f = 20.0f;
        }
        return Math.abs(f);
    }

    public void setAnimationScale(int i, float f) {
        if (!checkCallingPermission("android.permission.SET_ANIMATION_SCALE", "setAnimationScale()")) {
            throw new SecurityException("Requires SET_ANIMATION_SCALE permission");
        }
        float fFixScale = fixScale(f);
        switch (i) {
            case 0:
                this.mWindowAnimationScaleSetting = fFixScale;
                break;
            case 1:
                this.mTransitionAnimationScaleSetting = fFixScale;
                break;
            case 2:
                this.mAnimatorDurationScaleSetting = fFixScale;
                break;
        }
        this.mH.sendEmptyMessage(14);
    }

    public void setAnimationScales(float[] fArr) {
        if (!checkCallingPermission("android.permission.SET_ANIMATION_SCALE", "setAnimationScale()")) {
            throw new SecurityException("Requires SET_ANIMATION_SCALE permission");
        }
        if (fArr != null) {
            if (fArr.length >= 1) {
                this.mWindowAnimationScaleSetting = fixScale(fArr[0]);
            }
            if (fArr.length >= 2) {
                this.mTransitionAnimationScaleSetting = fixScale(fArr[1]);
            }
            if (fArr.length >= 3) {
                this.mAnimatorDurationScaleSetting = fixScale(fArr[2]);
                dispatchNewAnimatorScaleLocked(null);
            }
        }
        this.mH.sendEmptyMessage(14);
    }

    private void setAnimatorDurationScale(float f) {
        this.mAnimatorDurationScaleSetting = f;
        ValueAnimator.setDurationScale(f);
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

    public float getAnimationScale(int i) {
        switch (i) {
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
            try {
                boostPriorityForLockedSection();
                f = this.mAnimationsDisabled ? 0.0f : this.mAnimatorDurationScaleSetting;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return f;
    }

    void dispatchNewAnimatorScaleLocked(Session session) {
        this.mH.obtainMessage(34, session).sendToTarget();
    }

    @Override
    public void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener pointerEventListener) {
        this.mPointerEventDispatcher.registerInputEventListener(pointerEventListener);
    }

    @Override
    public void unregisterPointerEventListener(WindowManagerPolicyConstants.PointerEventListener pointerEventListener) {
        this.mPointerEventDispatcher.unregisterInputEventListener(pointerEventListener);
    }

    boolean canDispatchPointerEvents() {
        return this.mPointerEventDispatcher != null;
    }

    @Override
    public int getLidState() {
        int switchState = this.mInputManager.getSwitchState(-1, -256, 0);
        if (switchState > 0) {
            return 0;
        }
        return switchState == 0 ? 1 : -1;
    }

    @Override
    public void lockDeviceNow() {
        lockNow(null);
    }

    @Override
    public int getCameraLensCoverState() {
        int switchState = this.mInputManager.getSwitchState(-1, -256, 9);
        if (switchState > 0) {
            return 1;
        }
        return switchState == 0 ? 0 : -1;
    }

    @Override
    public void switchKeyboardLayout(int i, int i2) {
        this.mInputManager.switchKeyboardLayout(i, i2);
    }

    @Override
    public void switchInputMethod(boolean z) {
        InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        if (inputMethodManagerInternal != null) {
            inputMethodManagerInternal.switchInputMethod(z);
        }
    }

    @Override
    public void shutdown(boolean z) {
        ShutdownThread.shutdown(ActivityThread.currentActivityThread().getSystemUiContext(), "userrequested", z);
    }

    @Override
    public void reboot(boolean z) {
        ShutdownThread.reboot(ActivityThread.currentActivityThread().getSystemUiContext(), "userrequested", z);
    }

    @Override
    public void rebootSafeMode(boolean z) {
        ShutdownThread.rebootSafeMode(ActivityThread.currentActivityThread().getSystemUiContext(), z);
    }

    public void setCurrentProfileIds(int[] iArr) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mCurrentProfileIds = iArr;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setCurrentUser(int i, int[] iArr) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mCurrentUserId = i;
                this.mCurrentProfileIds = iArr;
                this.mAppTransition.setCurrentUser(i);
                this.mPolicy.setCurrentUserLw(i);
                boolean z = true;
                this.mPolicy.enableKeyguard(true);
                this.mRoot.switchUser();
                this.mWindowPlacerLocked.performSurfacePlacement();
                DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
                TaskStack splitScreenPrimaryStackIgnoringVisibility = defaultDisplayContentLocked.getSplitScreenPrimaryStackIgnoringVisibility();
                DockedStackDividerController dockedStackDividerController = defaultDisplayContentLocked.mDividerControllerLocked;
                if (splitScreenPrimaryStackIgnoringVisibility == null || !splitScreenPrimaryStackIgnoringVisibility.hasTaskForUser(i)) {
                    z = false;
                }
                dockedStackDividerController.notifyDockedStackExistsChanged(z);
                if (this.mDisplayReady) {
                    int forcedDisplayDensityForUserLocked = getForcedDisplayDensityForUserLocked(i);
                    if (forcedDisplayDensityForUserLocked == 0) {
                        forcedDisplayDensityForUserLocked = defaultDisplayContentLocked.mInitialDisplayDensity;
                    }
                    setForcedDisplayDensityLocked(defaultDisplayContentLocked, forcedDisplayDensityForUserLocked);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    boolean isCurrentProfileLocked(int i) {
        if (i == this.mCurrentUserId) {
            return true;
        }
        for (int i2 = 0; i2 < this.mCurrentProfileIds.length; i2++) {
            if (this.mCurrentProfileIds[i2] == i) {
                return true;
            }
        }
        return false;
    }

    public void enableScreenAfterBoot() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_BOOT) {
                    RuntimeException runtimeException = new RuntimeException("here");
                    runtimeException.fillInStackTrace();
                    Slog.i("WindowManager", "enableScreenAfterBoot: mDisplayEnabled=" + this.mDisplayEnabled + " mForceDisplayEnabled=" + this.mForceDisplayEnabled + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted, runtimeException);
                }
                if (this.mSystemBooted) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mSystemBooted = true;
                hideBootMessagesLocked();
                this.mH.sendEmptyMessageDelayed(23, 30000L);
                resetPriorityAfterLockedSection();
                this.mPolicy.systemBooted();
                performEnableScreen();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void enableScreenIfNeeded() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                enableScreenIfNeededLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void enableScreenIfNeededLocked() {
        if (WindowManagerDebugConfig.DEBUG_BOOT) {
            WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
            if (WindowManagerDebugger.WMS_DEBUG_LOG_OFF) {
                RuntimeException runtimeException = new RuntimeException("here");
                runtimeException.fillInStackTrace();
                Slog.i("WindowManager", "enableScreenIfNeededLocked: mDisplayEnabled=" + this.mDisplayEnabled + " mForceDisplayEnabled=" + this.mForceDisplayEnabled + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted, runtimeException);
            }
        }
        if (this.mDisplayEnabled) {
            return;
        }
        if (!this.mSystemBooted && !this.mShowingBootMessages) {
            return;
        }
        this.mH.sendEmptyMessage(16);
    }

    public void performBootTimeout() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mDisplayEnabled) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                Slog.w("WindowManager", "***** BOOT TIMEOUT: forcing display enabled");
                this.mForceDisplayEnabled = true;
                resetPriorityAfterLockedSection();
                performEnableScreen();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void onSystemUiStarted() {
        this.mPolicy.onSystemUiStarted();
    }

    private void performEnableScreen() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (!WindowManagerDebugConfig.DEBUG_BOOT) {
                    WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
                    if (WindowManagerDebugger.WMS_DEBUG_ENG) {
                        Slog.i("WindowManager", "performEnableScreen: mDisplayEnabled=" + this.mDisplayEnabled + " mForceDisplayEnabled=" + this.mForceDisplayEnabled + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted + " mOnlyCore=" + this.mOnlyCore, new RuntimeException("here").fillInStackTrace());
                    }
                }
                if (this.mDisplayEnabled) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (!this.mSystemBooted && !this.mShowingBootMessages) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (!this.mShowingBootMessages && !this.mPolicy.canDismissBootAnimation()) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (!this.mForceDisplayEnabled && getDefaultDisplayContentLocked().checkWaitingForWindows()) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (!this.mBootAnimationStopped) {
                    Trace.asyncTraceBegin(32L, "Stop bootanim", 0);
                    SystemProperties.set("service.bootanim.exit", "1");
                    this.mBootAnimationStopped = true;
                }
                if (!this.mForceDisplayEnabled && !checkBootAnimationCompleteLocked()) {
                    if (WindowManagerDebugConfig.DEBUG_BOOT) {
                        Slog.i("WindowManager", "performEnableScreen: Waiting for anim complete");
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                try {
                    IBinder service = ServiceManager.getService("SurfaceFlinger");
                    if (service != null) {
                        Slog.i("WindowManager", "******* TELLING SURFACE FLINGER WE ARE BOOTED!");
                        Parcel parcelObtain = Parcel.obtain();
                        parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
                        service.transact(1, parcelObtain, null, 0);
                        parcelObtain.recycle();
                        WindowManagerDebugger windowManagerDebugger2 = this.mWindowManagerDebugger;
                        if (WindowManagerDebugger.WMS_DEBUG_ENG) {
                            Slog.d("WindowManager", "Tell SurfaceFlinger finish boot animation");
                        }
                    }
                } catch (RemoteException e) {
                    Slog.e("WindowManager", "Boot completed: SurfaceFlinger is dead!");
                }
                EventLog.writeEvent(EventLogTags.WM_BOOT_ANIMATION_DONE, SystemClock.uptimeMillis());
                Trace.asyncTraceEnd(32L, "Stop bootanim", 0);
                this.mDisplayEnabled = true;
                if (WindowManagerDebugConfig.DEBUG_SCREEN_ON || WindowManagerDebugConfig.DEBUG_BOOT) {
                    Slog.i("WindowManager", "******************** ENABLING SCREEN!");
                }
                this.mInputMonitor.setEventDispatchingLw(this.mEventDispatchingEnabled);
                resetPriorityAfterLockedSection();
                try {
                    this.mActivityManager.bootAnimationComplete();
                } catch (RemoteException e2) {
                }
                this.mPolicy.enableScreenAfterBoot();
                updateRotationUnchecked(false, false);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private boolean checkBootAnimationCompleteLocked() {
        if (SystemService.isRunning(BOOT_ANIMATION_SERVICE)) {
            this.mH.removeMessages(37);
            this.mH.sendEmptyMessageDelayed(37, 200L);
            if (WindowManagerDebugConfig.DEBUG_BOOT) {
                Slog.i("WindowManager", "checkBootAnimationComplete: Waiting for anim complete");
                return false;
            }
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_BOOT) {
            Slog.i("WindowManager", "checkBootAnimationComplete: Animation complete!");
            return true;
        }
        return true;
    }

    public void showBootMessage(CharSequence charSequence, boolean z) {
        boolean z2;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_BOOT) {
                    RuntimeException runtimeException = new RuntimeException("here");
                    runtimeException.fillInStackTrace();
                    Slog.i("WindowManager", "showBootMessage: msg=" + ((Object) charSequence) + " always=" + z + " mAllowBootMessages=" + this.mAllowBootMessages + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted, runtimeException);
                }
                if (!this.mAllowBootMessages) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (this.mShowingBootMessages) {
                    z2 = false;
                } else {
                    if (!z) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    z2 = true;
                }
                if (this.mSystemBooted) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mShowingBootMessages = true;
                this.mPolicy.showBootMessage(charSequence, z);
                resetPriorityAfterLockedSection();
                if (z2) {
                    performEnableScreen();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void hideBootMessagesLocked() {
        if (WindowManagerDebugConfig.DEBUG_BOOT) {
            RuntimeException runtimeException = new RuntimeException("here");
            runtimeException.fillInStackTrace();
            Slog.i("WindowManager", "hideBootMessagesLocked: mDisplayEnabled=" + this.mDisplayEnabled + " mForceDisplayEnabled=" + this.mForceDisplayEnabled + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted, runtimeException);
        }
        if (this.mShowingBootMessages) {
            this.mShowingBootMessages = false;
            this.mPolicy.hideBootMessages();
        }
    }

    public void setInTouchMode(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mInTouchMode = z;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private void updateCircularDisplayMaskIfNeeded() {
        int i;
        if (this.mContext.getResources().getConfiguration().isScreenRound() && this.mContext.getResources().getBoolean(R.^attr-private.screenLayout)) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    i = this.mCurrentUserId;
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            int intForUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_inversion_enabled", 0, i);
            int i2 = 1;
            if (intForUser == 1) {
                i2 = 0;
            }
            Message messageObtainMessage = this.mH.obtainMessage(35);
            messageObtainMessage.arg1 = i2;
            this.mH.sendMessage(messageObtainMessage);
        }
    }

    public void showEmulatorDisplayOverlayIfNeeded() {
        if (this.mContext.getResources().getBoolean(R.^attr-private.request) && SystemProperties.getBoolean(PROPERTY_EMULATOR_CIRCULAR, false) && Build.IS_EMULATOR) {
            this.mH.sendMessage(this.mH.obtainMessage(36));
        }
    }

    public void showCircularMask(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", ">>> OPEN TRANSACTION showCircularMask(visible=" + z + ")");
                }
                openSurfaceTransaction();
                try {
                    if (z) {
                        if (this.mCircularDisplayMask == null) {
                            this.mCircularDisplayMask = new CircularDisplayMask(getDefaultDisplayContentLocked(), (this.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 10, this.mContext.getResources().getInteger(R.integer.config_mediaOutputSwitchDialogVersion), this.mContext.getResources().getDimensionPixelSize(R.dimen.activity_embedding_divider_handle_width_pressed));
                        }
                        this.mCircularDisplayMask.setVisibility(true);
                    } else if (this.mCircularDisplayMask != null) {
                        this.mCircularDisplayMask.setVisibility(false);
                        this.mCircularDisplayMask = null;
                    }
                    closeSurfaceTransaction("showCircularMask");
                    if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                        Slog.i("WindowManager", "<<< CLOSE TRANSACTION showCircularMask(visible=" + z + ")");
                    }
                } catch (Throwable th) {
                    closeSurfaceTransaction("showCircularMask");
                    if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                        Slog.i("WindowManager", "<<< CLOSE TRANSACTION showCircularMask(visible=" + z + ")");
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                resetPriorityAfterLockedSection();
                throw th2;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void showEmulatorDisplayOverlay() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", ">>> OPEN TRANSACTION showEmulatorDisplayOverlay");
                }
                openSurfaceTransaction();
                try {
                    if (this.mEmulatorDisplayOverlay == null) {
                        this.mEmulatorDisplayOverlay = new EmulatorDisplayOverlay(this.mContext, getDefaultDisplayContentLocked(), (this.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 10);
                    }
                    this.mEmulatorDisplayOverlay.setVisibility(true);
                } finally {
                    closeSurfaceTransaction("showEmulatorDisplayOverlay");
                    if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                        Slog.i("WindowManager", "<<< CLOSE TRANSACTION showEmulatorDisplayOverlay");
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void showStrictModeViolation(boolean z) {
        int callingPid = Binder.getCallingPid();
        if (!z) {
            this.mH.sendMessage(this.mH.obtainMessage(25, 0, callingPid));
        } else {
            this.mH.sendMessage(this.mH.obtainMessage(25, 1, callingPid));
            this.mH.sendMessageDelayed(this.mH.obtainMessage(25, 0, callingPid), 1000L);
        }
    }

    private void showStrictModeViolation(int i, int i2) {
        boolean z = i != 0;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (z && !this.mRoot.canShowStrictModeViolation(i2)) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (WindowManagerDebugConfig.SHOW_VERBOSE_TRANSACTIONS) {
                    Slog.i("WindowManager", ">>> OPEN TRANSACTION showStrictModeViolation");
                }
                SurfaceControl.openTransaction();
                try {
                    if (this.mStrictModeFlash == null) {
                        this.mStrictModeFlash = new StrictModeFlash(getDefaultDisplayContentLocked());
                    }
                    this.mStrictModeFlash.setVisibility(z);
                    resetPriorityAfterLockedSection();
                } finally {
                    SurfaceControl.closeTransaction();
                    if (WindowManagerDebugConfig.SHOW_VERBOSE_TRANSACTIONS) {
                        Slog.i("WindowManager", "<<< CLOSE TRANSACTION showStrictModeViolation");
                    }
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setStrictModeVisualIndicatorPreference(String str) {
        SystemProperties.set("persist.sys.strictmode.visual", str);
    }

    public Bitmap screenshotWallpaper() {
        Bitmap bitmapScreenshotWallpaperLocked;
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "screenshotWallpaper()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        try {
            Trace.traceBegin(32L, "screenshotWallpaper");
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    bitmapScreenshotWallpaperLocked = this.mRoot.mWallpaperController.screenshotWallpaperLocked();
                } finally {
                }
            }
            resetPriorityAfterLockedSection();
            return bitmapScreenshotWallpaperLocked;
        } finally {
            Trace.traceEnd(32L);
        }
    }

    public boolean requestAssistScreenshot(final IAssistDataReceiver iAssistDataReceiver) {
        final Bitmap bitmapScreenshotDisplayLocked;
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "requestAssistScreenshot()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(0);
                if (displayContent == null) {
                    if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                        Slog.i("WindowManager", "Screenshot returning null. No Display for displayId=0");
                    }
                    bitmapScreenshotDisplayLocked = null;
                } else {
                    bitmapScreenshotDisplayLocked = displayContent.screenshotDisplayLocked(Bitmap.Config.ARGB_8888);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        FgThread.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                iAssistDataReceiver.onHandleAssistScreenshot(bitmapScreenshotDisplayLocked);
            }
        });
        return true;
    }

    public ActivityManager.TaskSnapshot getTaskSnapshot(int i, int i2, boolean z) {
        return this.mTaskSnapshotController.getSnapshot(i, i2, true, z);
    }

    public void removeObsoleteTaskFiles(ArraySet<Integer> arraySet, int[] iArr) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mTaskSnapshotController.removeObsoleteTaskFiles(arraySet, iArr);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void freezeRotation(int i) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "freezeRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        if (i < -1 || i > 3) {
            throw new IllegalArgumentException("Rotation argument must be -1 or a valid rotation constant.");
        }
        int defaultDisplayRotation = getDefaultDisplayRotation();
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "freezeRotation: mRotation=" + defaultDisplayRotation);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            WindowManagerPolicy windowManagerPolicy = this.mPolicy;
            if (i == -1) {
                i = defaultDisplayRotation;
            }
            windowManagerPolicy.setUserRotationMode(1, i);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            updateRotationUnchecked(false, false);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public void thawRotation() {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "thawRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "thawRotation: mRotation=" + getDefaultDisplayRotation());
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mPolicy.setUserRotationMode(0, 777);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            updateRotationUnchecked(false, false);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public void updateRotation(boolean z, boolean z2) {
        updateRotationUnchecked(z, z2);
    }

    void pauseRotationLocked() {
        this.mDeferredRotationPauseCount++;
    }

    void resumeRotationLocked() {
        if (this.mDeferredRotationPauseCount > 0) {
            this.mDeferredRotationPauseCount--;
            if (this.mDeferredRotationPauseCount == 0) {
                DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
                if (defaultDisplayContentLocked.updateRotationUnchecked()) {
                    this.mH.obtainMessage(18, Integer.valueOf(defaultDisplayContentLocked.getDisplayId())).sendToTarget();
                }
            }
        }
    }

    private void updateRotationUnchecked(boolean z, boolean z2) {
        boolean zUpdateRotationUnchecked;
        int displayId;
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "updateRotationUnchecked: alwaysSendConfiguration=" + z + " forceRelayout=" + z2);
        }
        Trace.traceBegin(32L, "updateRotation");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
                    Trace.traceBegin(32L, "updateRotation: display");
                    zUpdateRotationUnchecked = defaultDisplayContentLocked.updateRotationUnchecked();
                    Trace.traceEnd(32L);
                    if (!zUpdateRotationUnchecked || z2) {
                        defaultDisplayContentLocked.setLayoutNeeded();
                        Trace.traceBegin(32L, "updateRotation: performSurfacePlacement");
                        this.mWindowPlacerLocked.performSurfacePlacement();
                        Trace.traceEnd(32L);
                    }
                    displayId = defaultDisplayContentLocked.getDisplayId();
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            if (zUpdateRotationUnchecked || z) {
                Trace.traceBegin(32L, "updateRotation: sendNewConfiguration");
                sendNewConfiguration(displayId);
                Trace.traceEnd(32L);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Trace.traceEnd(32L);
        }
    }

    public int getDefaultDisplayRotation() {
        int rotation;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                rotation = getDefaultDisplayContentLocked().getRotation();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return rotation;
    }

    public boolean isRotationFrozen() {
        return this.mPolicy.getUserRotationMode() == 1;
    }

    public int watchRotation(IRotationWatcher iRotationWatcher, int i) {
        int defaultDisplayRotation;
        final IBinder iBinderAsBinder = iRotationWatcher.asBinder();
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                synchronized (WindowManagerService.this.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        int i2 = 0;
                        while (i2 < WindowManagerService.this.mRotationWatchers.size()) {
                            if (iBinderAsBinder == WindowManagerService.this.mRotationWatchers.get(i2).mWatcher.asBinder()) {
                                IBinder iBinderAsBinder2 = WindowManagerService.this.mRotationWatchers.remove(i2).mWatcher.asBinder();
                                if (iBinderAsBinder2 != null) {
                                    iBinderAsBinder2.unlinkToDeath(this, 0);
                                }
                                i2--;
                            }
                            i2++;
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        };
        synchronized (this.mWindowMap) {
            try {
                try {
                    boostPriorityForLockedSection();
                    iRotationWatcher.asBinder().linkToDeath(deathRecipient, 0);
                    this.mRotationWatchers.add(new RotationWatcher(iRotationWatcher, deathRecipient, i));
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (RemoteException e) {
            }
            defaultDisplayRotation = getDefaultDisplayRotation();
        }
        resetPriorityAfterLockedSection();
        return defaultDisplayRotation;
    }

    public void removeRotationWatcher(IRotationWatcher iRotationWatcher) {
        IBinder iBinderAsBinder = iRotationWatcher.asBinder();
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                int i = 0;
                while (i < this.mRotationWatchers.size()) {
                    if (iBinderAsBinder == this.mRotationWatchers.get(i).mWatcher.asBinder()) {
                        RotationWatcher rotationWatcherRemove = this.mRotationWatchers.remove(i);
                        IBinder iBinderAsBinder2 = rotationWatcherRemove.mWatcher.asBinder();
                        if (iBinderAsBinder2 != null) {
                            iBinderAsBinder2.unlinkToDeath(rotationWatcherRemove.mDeathRecipient, 0);
                        }
                        i--;
                    }
                    i++;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean registerWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) {
        boolean zIsWallpaperVisible;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent == null) {
                    throw new IllegalArgumentException("Trying to register visibility event for invalid display: " + i);
                }
                this.mWallpaperVisibilityListeners.registerWallpaperVisibilityListener(iWallpaperVisibilityListener, i);
                zIsWallpaperVisible = displayContent.mWallpaperController.isWallpaperVisible();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zIsWallpaperVisible;
    }

    public void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWallpaperVisibilityListeners.unregisterWallpaperVisibilityListener(iWallpaperVisibilityListener, i);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public int getPreferredOptionsPanelGravity() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
                int rotation = defaultDisplayContentLocked.getRotation();
                if (defaultDisplayContentLocked.mInitialDisplayWidth < defaultDisplayContentLocked.mInitialDisplayHeight) {
                    switch (rotation) {
                        case 1:
                            resetPriorityAfterLockedSection();
                            return 85;
                        case 2:
                            resetPriorityAfterLockedSection();
                            return 81;
                        case 3:
                            resetPriorityAfterLockedSection();
                            return 8388691;
                        default:
                            return 81;
                    }
                }
                switch (rotation) {
                    case 1:
                        resetPriorityAfterLockedSection();
                        return 81;
                    case 2:
                        resetPriorityAfterLockedSection();
                        return 8388691;
                    case 3:
                        resetPriorityAfterLockedSection();
                        return 81;
                    default:
                        resetPriorityAfterLockedSection();
                        return 85;
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean startViewServer(int i) {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "startViewServer") || i < 1024) {
            return false;
        }
        if (this.mViewServer != null) {
            if (!this.mViewServer.isRunning()) {
                try {
                    return this.mViewServer.start();
                } catch (IOException e) {
                    Slog.w("WindowManager", "View server did not start");
                }
            }
            return false;
        }
        try {
            this.mViewServer = new ViewServer(this, i);
            return this.mViewServer.start();
        } catch (IOException e2) {
            Slog.w("WindowManager", "View server did not start");
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

    boolean viewServerListWindows(Socket socket) throws Throwable {
        BufferedWriter bufferedWriter;
        boolean z = false;
        if (isSystemSecure()) {
            return false;
        }
        final ArrayList arrayList = new ArrayList();
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllWindows(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        arrayList.add((WindowState) obj);
                    }
                }, false);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        try {
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()), 8192);
            } catch (IOException e) {
            }
            try {
                int size = arrayList.size();
                for (int i = 0; i < size; i++) {
                    WindowState windowState = (WindowState) arrayList.get(i);
                    bufferedWriter.write(Integer.toHexString(System.identityHashCode(windowState)));
                    bufferedWriter.write(32);
                    bufferedWriter.append(windowState.mAttrs.getTitle());
                    bufferedWriter.write(10);
                }
                bufferedWriter.write("DONE.\n");
                bufferedWriter.flush();
                bufferedWriter.close();
                z = true;
            } catch (Exception e2) {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                return z;
            } catch (Throwable th2) {
                th = th2;
                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (IOException e3) {
                    }
                }
                throw th;
            }
        } catch (Exception e4) {
            bufferedWriter = null;
        } catch (Throwable th3) {
            th = th3;
            bufferedWriter = null;
        }
        return z;
    }

    boolean viewServerGetFocusedWindow(Socket socket) throws Throwable {
        BufferedWriter bufferedWriter;
        boolean z = false;
        if (isSystemSecure()) {
            return false;
        }
        WindowState focusedWindow = getFocusedWindow();
        try {
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()), 8192);
                if (focusedWindow != null) {
                    try {
                        bufferedWriter.write(Integer.toHexString(System.identityHashCode(focusedWindow)));
                        bufferedWriter.write(32);
                        bufferedWriter.append(focusedWindow.mAttrs.getTitle());
                    } catch (Exception e) {
                        if (bufferedWriter != null) {
                            bufferedWriter.close();
                        }
                        return z;
                    } catch (Throwable th) {
                        th = th;
                        if (bufferedWriter != null) {
                            try {
                                bufferedWriter.close();
                            } catch (IOException e2) {
                            }
                        }
                        throw th;
                    }
                }
                bufferedWriter.write(10);
                bufferedWriter.flush();
                bufferedWriter.close();
                z = true;
            } catch (IOException e3) {
            }
        } catch (Exception e4) {
            bufferedWriter = null;
        } catch (Throwable th2) {
            th = th2;
            bufferedWriter = null;
        }
        return z;
    }

    boolean viewServerWindowCommand(Socket socket, String str, String str2) throws Throwable {
        Parcel parcelObtain;
        Parcel parcelObtain2;
        BufferedWriter bufferedWriter;
        if (isSystemSecure()) {
            return false;
        }
        BufferedWriter bufferedWriter2 = null;
        bufferedWriter2 = null;
        bufferedWriter2 = null;
        Parcel parcel = null;
        try {
            int iIndexOf = str2.indexOf(32);
            if (iIndexOf == -1) {
                iIndexOf = str2.length();
            }
            int i = (int) Long.parseLong(str2.substring(0, iIndexOf), 16);
            str2 = iIndexOf < str2.length() ? str2.substring(iIndexOf + 1) : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            WindowState windowStateFindWindow = findWindow(i);
            if (windowStateFindWindow == null) {
                return false;
            }
            parcelObtain = Parcel.obtain();
            try {
                parcelObtain.writeInterfaceToken("android.view.IWindow");
                parcelObtain.writeString(str);
                parcelObtain.writeString(str2);
                parcelObtain.writeInt(1);
                ParcelFileDescriptor.fromSocket(socket).writeToParcel(parcelObtain, 0);
                parcelObtain2 = Parcel.obtain();
                try {
                    windowStateFindWindow.mClient.asBinder().transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (socket.isOutputShutdown()) {
                        bufferedWriter = null;
                    } else {
                        bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        try {
                            bufferedWriter.write("DONE\n");
                            bufferedWriter.flush();
                        } catch (Exception e) {
                            e = e;
                            parcel = parcelObtain;
                            Slog.w("WindowManager", "Could not send command " + str + " with parameters " + str2, e);
                            if (parcel != null) {
                            }
                            if (parcelObtain2 != null) {
                            }
                            if (bufferedWriter != null) {
                            }
                        } catch (Throwable th) {
                            th = th;
                            bufferedWriter2 = bufferedWriter;
                            if (parcelObtain != null) {
                            }
                            if (parcelObtain2 != null) {
                            }
                            if (bufferedWriter2 != null) {
                            }
                            throw th;
                        }
                    }
                    if (parcelObtain != null) {
                        parcelObtain.recycle();
                    }
                    if (parcelObtain2 != null) {
                        parcelObtain2.recycle();
                    }
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (IOException e2) {
                        }
                    }
                    return true;
                } catch (Exception e3) {
                    e = e3;
                    bufferedWriter = null;
                } catch (Throwable th2) {
                    th = th2;
                    if (parcelObtain != null) {
                    }
                    if (parcelObtain2 != null) {
                    }
                    if (bufferedWriter2 != null) {
                    }
                    throw th;
                }
            } catch (Exception e4) {
                e = e4;
                bufferedWriter = null;
                parcelObtain2 = null;
            } catch (Throwable th3) {
                th = th3;
                parcelObtain2 = null;
            }
        } catch (Exception e5) {
            e = e5;
            bufferedWriter = null;
            parcelObtain2 = null;
        } catch (Throwable th4) {
            th = th4;
            parcelObtain = null;
            parcelObtain2 = null;
        }
        try {
            Slog.w("WindowManager", "Could not send command " + str + " with parameters " + str2, e);
            if (parcel != null) {
                parcel.recycle();
            }
            if (parcelObtain2 != null) {
                parcelObtain2.recycle();
            }
            if (bufferedWriter != null) {
                return false;
            }
            try {
                bufferedWriter.close();
                return false;
            } catch (IOException e6) {
                return false;
            }
        } catch (Throwable th5) {
            th = th5;
            parcelObtain = parcel;
            bufferedWriter2 = bufferedWriter;
            if (parcelObtain != null) {
                parcelObtain.recycle();
            }
            if (parcelObtain2 != null) {
                parcelObtain2.recycle();
            }
            if (bufferedWriter2 != null) {
                try {
                    bufferedWriter2.close();
                } catch (IOException e7) {
                }
            }
            throw th;
        }
    }

    public void addWindowChangeListener(WindowChangeListener windowChangeListener) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowChangeListeners.add(windowChangeListener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void removeWindowChangeListener(WindowChangeListener windowChangeListener) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowChangeListeners.remove(windowChangeListener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private void notifyWindowsChanged() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mWindowChangeListeners.isEmpty()) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                WindowChangeListener[] windowChangeListenerArr = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                resetPriorityAfterLockedSection();
                for (WindowChangeListener windowChangeListener : windowChangeListenerArr) {
                    windowChangeListener.windowsChanged();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void notifyFocusChanged() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mWindowChangeListeners.isEmpty()) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                WindowChangeListener[] windowChangeListenerArr = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                resetPriorityAfterLockedSection();
                for (WindowChangeListener windowChangeListener : windowChangeListenerArr) {
                    windowChangeListener.focusChanged();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private WindowState findWindow(final int i) {
        WindowState window;
        if (i == -1) {
            return getFocusedWindow();
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                window = this.mRoot.getWindow(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return WindowManagerService.lambda$findWindow$4(i, (WindowState) obj);
                    }
                });
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return window;
    }

    static boolean lambda$findWindow$4(int i, WindowState windowState) {
        return System.identityHashCode(windowState) == i;
    }

    void sendNewConfiguration(int i) {
        try {
            if (!this.mActivityManager.updateDisplayOverrideConfiguration((Configuration) null, i)) {
                synchronized (this.mWindowMap) {
                    try {
                        boostPriorityForLockedSection();
                        if (this.mWaitingForConfig) {
                            this.mWaitingForConfig = false;
                            this.mLastFinishedFreezeSource = "config-unchanged";
                            DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                            if (displayContent != null) {
                                displayContent.setLayoutNeeded();
                            }
                            this.mWindowPlacerLocked.performSurfacePlacement();
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
            }
        } catch (RemoteException e) {
        }
    }

    public Configuration computeNewConfiguration(int i) {
        Configuration configurationComputeNewConfigurationLocked;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                configurationComputeNewConfigurationLocked = computeNewConfigurationLocked(i);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return configurationComputeNewConfigurationLocked;
    }

    private Configuration computeNewConfigurationLocked(int i) {
        if (!this.mDisplayReady) {
            return null;
        }
        Configuration configuration = new Configuration();
        this.mRoot.getDisplayContent(i).computeScreenConfiguration(configuration);
        return configuration;
    }

    void notifyHardKeyboardStatusChange() {
        WindowManagerInternal.OnHardKeyboardStatusChangeListener onHardKeyboardStatusChangeListener;
        boolean z;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                onHardKeyboardStatusChangeListener = this.mHardKeyboardStatusChangeListener;
                z = this.mHardKeyboardAvailable;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (onHardKeyboardStatusChangeListener != null) {
            onHardKeyboardStatusChangeListener.onHardKeyboardStatusChange(z);
        }
    }

    public void setEventDispatching(boolean z) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setEventDispatching()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mEventDispatchingEnabled = z;
                if (this.mDisplayEnabled) {
                    this.mInputMonitor.setEventDispatchingLw(z);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private WindowState getFocusedWindow() {
        WindowState focusedWindowLocked;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                focusedWindowLocked = getFocusedWindowLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return focusedWindowLocked;
    }

    private WindowState getFocusedWindowLocked() {
        return this.mCurrentFocus;
    }

    TaskStack getImeFocusStackLocked() {
        if (this.mFocusedApp == null || this.mFocusedApp.getTask() == null) {
            return null;
        }
        return this.mFocusedApp.getTask().mStack;
    }

    public boolean detectSafeMode() {
        boolean z;
        if (!this.mInputMonitor.waitForInputDevicesReady(1000L)) {
            Slog.w("WindowManager", "Devices still not ready after waiting 1000 milliseconds before attempting to detect safe mode.");
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "safe_boot_disallowed", 0) != 0) {
            return false;
        }
        int keyCodeState = this.mInputManager.getKeyCodeState(-1, -256, 82);
        int keyCodeState2 = this.mInputManager.getKeyCodeState(-1, -256, 47);
        int keyCodeState3 = this.mInputManager.getKeyCodeState(-1, UsbTerminalTypes.TERMINAL_IN_MIC, 23);
        int scanCodeState = this.mInputManager.getScanCodeState(-1, 65540, 272);
        int keyCodeState4 = this.mInputManager.getKeyCodeState(-1, -256, 25);
        if (keyCodeState <= 0 && keyCodeState2 <= 0 && keyCodeState3 <= 0 && scanCodeState <= 0 && keyCodeState4 <= 0) {
            z = false;
        } else {
            z = true;
        }
        this.mSafeMode = z;
        try {
            if (SystemProperties.getInt(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, 0) != 0 || SystemProperties.getInt(ShutdownThread.RO_SAFEMODE_PROPERTY, 0) != 0) {
                this.mSafeMode = true;
                SystemProperties.set(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
        } catch (IllegalArgumentException e) {
        }
        if (this.mSafeMode) {
            Log.i("WindowManager", "SAFE MODE ENABLED (menu=" + keyCodeState + " s=" + keyCodeState2 + " dpad=" + keyCodeState3 + " trackball=" + scanCodeState + ")");
            if (SystemProperties.getInt(ShutdownThread.RO_SAFEMODE_PROPERTY, 0) == 0) {
                SystemProperties.set(ShutdownThread.RO_SAFEMODE_PROPERTY, "1");
            }
        } else {
            Log.i("WindowManager", "SAFE MODE not enabled");
        }
        this.mPolicy.setSafeMode(this.mSafeMode);
        return this.mSafeMode;
    }

    public void displayReady() {
        Settings.Secure.putString(this.mContext.getContentResolver(), "display_density_forced", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        Settings.Global.putString(this.mContext.getContentResolver(), "display_size_forced", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        int size = this.mRoot.mChildren.size();
        for (int i = 0; i < size; i++) {
            displayReady(((DisplayContent) this.mRoot.mChildren.get(i)).getDisplayId());
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
                if (this.mMaxUiWidth > 0) {
                    defaultDisplayContentLocked.setMaxUiWidth(this.mMaxUiWidth);
                }
                readForcedDisplayPropertiesLocked(defaultDisplayContentLocked);
                this.mDisplayReady = true;
            } finally {
            }
        }
        resetPriorityAfterLockedSection();
        try {
            this.mActivityManager.updateConfiguration((Configuration) null);
        } catch (RemoteException e) {
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mIsTouchDevice = this.mContext.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
                getDefaultDisplayContentLocked().configureDisplayPolicy();
            } finally {
            }
        }
        resetPriorityAfterLockedSection();
        try {
            this.mActivityManager.updateConfiguration((Configuration) null);
        } catch (RemoteException e2) {
        }
        updateCircularDisplayMaskIfNeeded();
    }

    private void displayReady(int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent != null) {
                    this.mAnimator.addDisplayLocked(i);
                    displayContent.initializeDisplayBaseInfo();
                    reconfigureDisplayLocked(displayContent);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void systemReady() {
        this.mPolicy.systemReady();
        this.mTaskSnapshotController.systemReady();
        this.mHasWideColorGamutSupport = queryWideColorGamutSupport();
    }

    private static boolean queryWideColorGamutSupport() {
        try {
            OptionalBool optionalBoolHasWideColorDisplay = ISurfaceFlingerConfigs.getService().hasWideColorDisplay();
            if (optionalBoolHasWideColorDisplay != null) {
                return optionalBoolHasWideColorDisplay.value;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    final class H extends Handler {
        public static final int ALL_WINDOWS_DRAWN = 33;
        public static final int ANIMATION_FAILSAFE = 60;
        public static final int APP_FREEZE_TIMEOUT = 17;
        public static final int APP_TRANSITION_TIMEOUT = 13;
        public static final int BOOT_TIMEOUT = 23;
        public static final int CHECK_IF_BOOT_ANIMATION_FINISHED = 37;
        public static final int CLIENT_FREEZE_TIMEOUT = 30;
        public static final int DO_ANIMATION_CALLBACK = 26;
        public static final int ENABLE_SCREEN = 16;
        public static final int FORCE_GC = 15;
        public static final int NEW_ANIMATOR_SCALE = 34;
        public static final int NOTIFY_ACTIVITY_DRAWN = 32;
        public static final int NOTIFY_APP_TRANSITION_CANCELLED = 48;
        public static final int NOTIFY_APP_TRANSITION_FINISHED = 49;
        public static final int NOTIFY_APP_TRANSITION_STARTING = 47;
        public static final int NOTIFY_DOCKED_STACK_MINIMIZED_CHANGED = 53;
        public static final int NOTIFY_KEYGUARD_FLAGS_CHANGED = 56;
        public static final int NOTIFY_KEYGUARD_TRUSTED_CHANGED = 57;
        public static final int PERSIST_ANIMATION_SCALE = 14;
        public static final int RECOMPUTE_FOCUS = 61;
        public static final int REPORT_FOCUS_CHANGE = 2;
        public static final int REPORT_HARD_KEYBOARD_STATUS_CHANGE = 22;
        public static final int REPORT_LOSING_FOCUS = 3;
        public static final int REPORT_WINDOWS_CHANGE = 19;
        public static final int RESET_ANR_MESSAGE = 38;
        public static final int RESTORE_POINTER_ICON = 55;
        public static final int SEAMLESS_ROTATION_TIMEOUT = 54;
        public static final int SEND_NEW_CONFIGURATION = 18;
        public static final int SET_HAS_OVERLAY_UI = 58;
        public static final int SET_RUNNING_REMOTE_ANIMATION = 59;
        public static final int SHOW_CIRCULAR_DISPLAY_MASK = 35;
        public static final int SHOW_EMULATOR_DISPLAY_OVERLAY = 36;
        public static final int SHOW_STRICT_MODE_VIOLATION = 25;
        public static final int UNUSED = 0;
        public static final int UPDATE_ANIMATION_SCALE = 51;
        public static final int UPDATE_DOCKED_STACK_DIVIDER = 41;
        public static final int WAITING_FOR_DRAWN_TIMEOUT = 24;
        public static final int WALLPAPER_DRAW_PENDING_TIMEOUT = 39;
        public static final int WINDOW_FREEZE_TIMEOUT = 11;
        public static final int WINDOW_HIDE_TIMEOUT = 52;
        public static final int WINDOW_REPLACEMENT_TIMEOUT = 46;

        H() {
        }

        @Override
        public void handleMessage(Message message) {
            AccessibilityController accessibilityController;
            ArrayList<WindowState> arrayList;
            Runnable runnable;
            Runnable runnable2;
            boolean zCheckBootAnimationCompleteLocked;
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                Slog.v("WindowManager", "handleMessage: entry what=" + message.what);
            }
            WindowState windowState = null;
            switch (message.what) {
                case 2:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mAccessibilityController != null && WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayId() == 0) {
                                accessibilityController = WindowManagerService.this.mAccessibilityController;
                            } else {
                                accessibilityController = null;
                            }
                            WindowState windowState2 = WindowManagerService.this.mLastFocus;
                            WindowState windowState3 = WindowManagerService.this.mCurrentFocus;
                            if (windowState2 == windowState3) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            WindowManagerService.this.mLastFocus = windowState3;
                            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                                Slog.i("WindowManager", "Focus moving from " + windowState2 + " to " + windowState3);
                            }
                            if (windowState3 != null && windowState2 != null && !windowState3.isDisplayedLw()) {
                                if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                    Slog.i("WindowManager", "Delaying loss of focus...");
                                }
                                WindowManagerService.this.mLosingFocus.add(windowState2);
                            } else {
                                windowState = windowState2;
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            if (accessibilityController != null) {
                                accessibilityController.onWindowFocusChangedNotLocked();
                            }
                            if (windowState3 != null) {
                                if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                    Slog.i("WindowManager", "Gaining focus: " + windowState3);
                                }
                                windowState3.reportFocusChangedSerialized(true, WindowManagerService.this.mInTouchMode);
                                WindowManagerService.this.notifyFocusChanged();
                            }
                            if (windowState != null) {
                                if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                    Slog.i("WindowManager", "Losing focus: " + windowState);
                                }
                                windowState.reportFocusChangedSerialized(false, WindowManagerService.this.mInTouchMode);
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    break;
                case 3:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            arrayList = WindowManagerService.this.mLosingFocus;
                            WindowManagerService.this.mLosingFocus = new ArrayList<>();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    int size = arrayList.size();
                    for (int i = 0; i < size; i++) {
                        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                            Slog.i("WindowManager", "Losing delayed focus: " + arrayList.get(i));
                        }
                        arrayList.get(i).reportFocusChangedSerialized(false, WindowManagerService.this.mInTouchMode);
                    }
                    break;
                case 11:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowManagerService.this.getDefaultDisplayContentLocked().onWindowFreezeTimeout();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 13:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mAppTransition.isTransitionSet() || !WindowManagerService.this.mOpeningApps.isEmpty() || !WindowManagerService.this.mClosingApps.isEmpty()) {
                                if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                                    WindowManagerDebugger windowManagerDebugger = WindowManagerService.this.mWindowManagerDebugger;
                                    if (WindowManagerDebugger.WMS_DEBUG_USER) {
                                        Slog.v("WindowManager", "*** APP TRANSITION TIMEOUT. isTransitionSet()=" + WindowManagerService.this.mAppTransition.isTransitionSet() + " mOpeningApps.size()=" + WindowManagerService.this.mOpeningApps.size() + " mClosingApps.size()=" + WindowManagerService.this.mClosingApps.size());
                                    }
                                    WindowManagerService.this.mAppTransition.setTimeout();
                                    WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                                }
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 14:
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                    break;
                case 15:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (!WindowManagerService.this.mAnimator.isAnimating() && !WindowManagerService.this.mAnimator.isAnimationScheduled()) {
                                if (WindowManagerService.this.mDisplayFrozen) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return;
                                } else {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    Runtime.getRuntime().gc();
                                }
                                break;
                            }
                            sendEmptyMessageDelayed(15, 2000L);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                case 16:
                    WindowManagerService.this.performEnableScreen();
                    break;
                case 17:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            Slog.w("WindowManager", "App freeze timeout expired.");
                            WindowManagerService.this.mWindowsFreezingScreen = 2;
                            for (int size2 = WindowManagerService.this.mAppFreezeListeners.size() - 1; size2 >= 0; size2--) {
                                WindowManagerService.this.mAppFreezeListeners.get(size2).onAppFreezeTimeout();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 18:
                    removeMessages(18, message.obj);
                    int iIntValue = ((Integer) message.obj).intValue();
                    if (WindowManagerService.this.mRoot.getDisplayContent(iIntValue) != null) {
                        WindowManagerService.this.sendNewConfiguration(iIntValue);
                    } else if (WindowManagerDebugConfig.DEBUG_CONFIGURATION) {
                        Slog.w("WindowManager", "Trying to send configuration to non-existing displayId=" + iIntValue);
                    }
                    break;
                case REPORT_WINDOWS_CHANGE:
                    if (WindowManagerService.this.mWindowsChanged) {
                        synchronized (WindowManagerService.this.mWindowMap) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                WindowManagerService.this.mWindowsChanged = false;
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                            break;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        WindowManagerService.this.notifyWindowsChanged();
                    }
                    break;
                case REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                    WindowManagerService.this.notifyHardKeyboardStatusChange();
                    break;
                case BOOT_TIMEOUT:
                    WindowManagerService.this.performBootTimeout();
                    break;
                case 24:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            Slog.w("WindowManager", "Timeout waiting for drawn: undrawn=" + WindowManagerService.this.mWaitingForDrawn);
                            WindowManagerService.this.mWaitingForDrawn.clear();
                            runnable = WindowManagerService.this.mWaitingForDrawnCallback;
                            WindowManagerService.this.mWaitingForDrawnCallback = null;
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (runnable != null) {
                        runnable.run();
                    }
                    break;
                case SHOW_STRICT_MODE_VIOLATION:
                    WindowManagerService.this.showStrictModeViolation(message.arg1, message.arg2);
                    break;
                case DO_ANIMATION_CALLBACK:
                    try {
                        ((IRemoteCallback) message.obj).sendResult((Bundle) null);
                    } catch (RemoteException e) {
                    }
                    break;
                case 30:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mClientFreezingScreen) {
                                WindowManagerService.this.mClientFreezingScreen = false;
                                WindowManagerService.this.mLastFinishedFreezeSource = "client-timeout";
                                WindowManagerService.this.stopFreezingDisplayLocked();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 32:
                    try {
                        WindowManagerService.this.mActivityManager.notifyActivityDrawn((IBinder) message.obj);
                    } catch (RemoteException e2) {
                    }
                    break;
                case 33:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            runnable2 = WindowManagerService.this.mWaitingForDrawnCallback;
                            WindowManagerService.this.mWaitingForDrawnCallback = null;
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (runnable2 != null) {
                        runnable2.run();
                    }
                    break;
                case 34:
                    float currentAnimatorScale = WindowManagerService.this.getCurrentAnimatorScale();
                    ValueAnimator.setDurationScale(currentAnimatorScale);
                    Session session = (Session) message.obj;
                    if (session != null) {
                        try {
                            session.mCallback.onAnimatorScaleChanged(currentAnimatorScale);
                        } catch (RemoteException e3) {
                        }
                    } else {
                        ArrayList arrayList2 = new ArrayList();
                        synchronized (WindowManagerService.this.mWindowMap) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                for (int i2 = 0; i2 < WindowManagerService.this.mSessions.size(); i2++) {
                                    arrayList2.add(WindowManagerService.this.mSessions.valueAt(i2).mCallback);
                                }
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                            break;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        for (int i3 = 0; i3 < arrayList2.size(); i3++) {
                            try {
                                ((IWindowSessionCallback) arrayList2.get(i3)).onAnimatorScaleChanged(currentAnimatorScale);
                            } catch (RemoteException e4) {
                            }
                        }
                    }
                    break;
                case 35:
                    WindowManagerService.this.showCircularMask(message.arg1 == 1);
                    break;
                case 36:
                    WindowManagerService.this.showEmulatorDisplayOverlay();
                    break;
                case 37:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerDebugConfig.DEBUG_BOOT) {
                                Slog.i("WindowManager", "CHECK_IF_BOOT_ANIMATION_FINISHED:");
                            }
                            zCheckBootAnimationCompleteLocked = WindowManagerService.this.checkBootAnimationCompleteLocked();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (zCheckBootAnimationCompleteLocked) {
                        WindowManagerService.this.performEnableScreen();
                    }
                    break;
                case 38:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowManagerService.this.mLastANRState = null;
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    WindowManagerService.this.mAmInternal.clearSavedANRState();
                    break;
                case 39:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mRoot.mWallpaperController.processWallpaperDrawPendingTimeout()) {
                                WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 41:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            DisplayContent defaultDisplayContentLocked = WindowManagerService.this.getDefaultDisplayContentLocked();
                            defaultDisplayContentLocked.getDockedDividerController().reevaluateVisibility(false);
                            defaultDisplayContentLocked.adjustForImeIfNeeded();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case WINDOW_REPLACEMENT_TIMEOUT:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            for (int size3 = WindowManagerService.this.mWindowReplacementTimeouts.size() - 1; size3 >= 0; size3--) {
                                WindowManagerService.this.mWindowReplacementTimeouts.get(size3).onWindowReplacementTimeout();
                            }
                            WindowManagerService.this.mWindowReplacementTimeouts.clear();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 47:
                    WindowManagerService.this.mAmInternal.notifyAppTransitionStarting((SparseIntArray) message.obj, message.getWhen());
                    break;
                case 48:
                    WindowManagerService.this.mAmInternal.notifyAppTransitionCancelled();
                    break;
                case 49:
                    WindowManagerService.this.mAmInternal.notifyAppTransitionFinished();
                    break;
                case 51:
                    switch (message.arg1) {
                        case 0:
                            WindowManagerService.this.mWindowAnimationScaleSetting = Settings.Global.getFloat(WindowManagerService.this.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                            break;
                        case 1:
                            WindowManagerService.this.mTransitionAnimationScaleSetting = Settings.Global.getFloat(WindowManagerService.this.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                            break;
                        case 2:
                            WindowManagerService.this.mAnimatorDurationScaleSetting = Settings.Global.getFloat(WindowManagerService.this.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                            WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                            break;
                    }
                    break;
                case 52:
                    WindowState windowState4 = (WindowState) message.obj;
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            windowState4.mAttrs.flags &= -129;
                            windowState4.hidePermanentlyLw();
                            windowState4.setDisplayLayoutNeeded();
                            WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 53:
                    WindowManagerService.this.mAmInternal.notifyDockedStackMinimizedChanged(message.arg1 == 1);
                    break;
                case 54:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowManagerService.this.getDefaultDisplayContentLocked().onSeamlessRotationTimeout();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 55:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowManagerService.this.restorePointerIconLocked((DisplayContent) message.obj, message.arg1, message.arg2);
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 56:
                    WindowManagerService.this.mAmInternal.notifyKeyguardFlagsChanged((Runnable) message.obj);
                    break;
                case NOTIFY_KEYGUARD_TRUSTED_CHANGED:
                    WindowManagerService.this.mAmInternal.notifyKeyguardTrustedChanged();
                    break;
                case SET_HAS_OVERLAY_UI:
                    WindowManagerService.this.mAmInternal.setHasOverlayUi(message.arg1, message.arg2 == 1);
                    break;
                case SET_RUNNING_REMOTE_ANIMATION:
                    WindowManagerService.this.mAmInternal.setRunningRemoteAnimation(message.arg1, message.arg2 == 1);
                    break;
                case 60:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mRecentsAnimationController != null) {
                                WindowManagerService.this.mRecentsAnimationController.scheduleFailsafe();
                            }
                        } finally {
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case RECOMPUTE_FOCUS:
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowManagerService.this.updateFocusedWindowLocked(0, true);
                        } finally {
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
            }
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                Slog.v("WindowManager", "handleMessage: exit");
            }
        }
    }

    void destroyPreservedSurfaceLocked() {
        for (int size = this.mDestroyPreservedSurface.size() - 1; size >= 0; size--) {
            this.mDestroyPreservedSurface.get(size).mWinAnimator.destroyPreservedSurfaceLocked();
        }
        this.mDestroyPreservedSurface.clear();
    }

    public IWindowSession openSession(IWindowSessionCallback iWindowSessionCallback, IInputMethodClient iInputMethodClient, IInputContext iInputContext) {
        if (iInputMethodClient == null) {
            throw new IllegalArgumentException("null client");
        }
        if (iInputContext == null) {
            throw new IllegalArgumentException("null inputContext");
        }
        return new Session(this, iWindowSessionCallback, iInputMethodClient, iInputContext);
    }

    public boolean inputMethodClientHasFocus(IInputMethodClient iInputMethodClient) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (getDefaultDisplayContentLocked().inputMethodClientHasFocus(iInputMethodClient)) {
                    resetPriorityAfterLockedSection();
                    return true;
                }
                if (this.mCurrentFocus == null || this.mCurrentFocus.mSession.mClient == null || this.mCurrentFocus.mSession.mClient.asBinder() != iInputMethodClient.asBinder()) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void getInitialDisplaySize(int i, Point point) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    point.x = displayContent.mInitialDisplayWidth;
                    point.y = displayContent.mInitialDisplayHeight;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void getBaseDisplaySize(int i, Point point) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    point.x = displayContent.mBaseDisplayWidth;
                    point.y = displayContent.mBaseDisplayHeight;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setForcedDisplaySize(int i, int i2, int i3) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (i != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                    if (displayContent != null) {
                        int iMin = Math.min(Math.max(i2, 200), displayContent.mInitialDisplayWidth * 2);
                        int iMin2 = Math.min(Math.max(i3, 200), displayContent.mInitialDisplayHeight * 2);
                        setForcedDisplaySizeLocked(displayContent, iMin, iMin2);
                        Settings.Global.putString(this.mContext.getContentResolver(), "display_size_forced", iMin + "," + iMin2);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setForcedDisplayScalingMode(int i, int i2) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (i != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                    if (displayContent != null) {
                        if (i2 < 0 || i2 > 1) {
                            i2 = 0;
                        }
                        setForcedDisplayScalingModeLocked(displayContent, i2);
                        Settings.Global.putInt(this.mContext.getContentResolver(), "display_scaling_force", i2);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void setForcedDisplayScalingModeLocked(DisplayContent displayContent, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("Using display scaling mode: ");
        sb.append(i == 0 ? UiModeManagerService.Shell.NIGHT_MODE_STR_AUTO : "off");
        Slog.i("WindowManager", sb.toString());
        displayContent.mDisplayScalingDisabled = i != 0;
        reconfigureDisplayLocked(displayContent);
    }

    private void readForcedDisplayPropertiesLocked(DisplayContent displayContent) {
        int iIndexOf;
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "display_size_forced");
        if (string == null || string.length() == 0) {
            string = SystemProperties.get(SIZE_OVERRIDE, (String) null);
        }
        if (string != null && string.length() > 0 && (iIndexOf = string.indexOf(44)) > 0 && string.lastIndexOf(44) == iIndexOf) {
            try {
                int i = Integer.parseInt(string.substring(0, iIndexOf));
                int i2 = Integer.parseInt(string.substring(iIndexOf + 1));
                if (displayContent.mBaseDisplayWidth != i || displayContent.mBaseDisplayHeight != i2) {
                    Slog.i("WindowManager", "FORCED DISPLAY SIZE: " + i + "x" + i2);
                    displayContent.updateBaseDisplayMetrics(i, i2, displayContent.mBaseDisplayDensity);
                }
            } catch (NumberFormatException e) {
            }
        }
        int forcedDisplayDensityForUserLocked = getForcedDisplayDensityForUserLocked(this.mCurrentUserId);
        if (forcedDisplayDensityForUserLocked != 0) {
            displayContent.mBaseDisplayDensity = forcedDisplayDensityForUserLocked;
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "display_scaling_force", 0) != 0) {
            Slog.i("WindowManager", "FORCED DISPLAY SCALING DISABLED");
            displayContent.mDisplayScalingDisabled = true;
        }
    }

    private void setForcedDisplaySizeLocked(DisplayContent displayContent, int i, int i2) {
        Slog.i("WindowManager", "Using new display size: " + i + "x" + i2);
        displayContent.updateBaseDisplayMetrics(i, i2, displayContent.mBaseDisplayDensity);
        reconfigureDisplayLocked(displayContent);
    }

    public void clearForcedDisplaySize(int i) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (i != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                    if (displayContent != null) {
                        setForcedDisplaySizeLocked(displayContent, displayContent.mInitialDisplayWidth, displayContent.mInitialDisplayHeight);
                        Settings.Global.putString(this.mContext.getContentResolver(), "display_size_forced", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int getInitialDisplayDensity(int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    int i2 = displayContent.mInitialDisplayDensity;
                    resetPriorityAfterLockedSection();
                    return i2;
                }
                resetPriorityAfterLockedSection();
                return -1;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getBaseDisplayDensity(int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    int i2 = displayContent.mBaseDisplayDensity;
                    resetPriorityAfterLockedSection();
                    return i2;
                }
                resetPriorityAfterLockedSection();
                return -1;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setForcedDisplayDensityForUser(int i, int i2, int i3) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (i != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i3, false, true, "setForcedDisplayDensityForUser", null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                    if (displayContent != null && this.mCurrentUserId == iHandleIncomingUser) {
                        setForcedDisplayDensityLocked(displayContent, i2);
                    }
                    Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "display_density_forced", Integer.toString(i2), iHandleIncomingUser);
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void clearForcedDisplayDensityForUser(int i, int i2) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        if (i != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i2, false, true, "clearForcedDisplayDensityForUser", null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                    if (displayContent != null && this.mCurrentUserId == iHandleIncomingUser) {
                        setForcedDisplayDensityLocked(displayContent, displayContent.mInitialDisplayDensity);
                    }
                    Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "display_density_forced", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, iHandleIncomingUser);
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private int getForcedDisplayDensityForUserLocked(int i) {
        String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", i);
        if (stringForUser == null || stringForUser.length() == 0) {
            stringForUser = SystemProperties.get(DENSITY_OVERRIDE, (String) null);
        }
        if (stringForUser != null && stringForUser.length() > 0) {
            try {
                return Integer.parseInt(stringForUser);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private void setForcedDisplayDensityLocked(DisplayContent displayContent, int i) {
        displayContent.mBaseDisplayDensity = i;
        reconfigureDisplayLocked(displayContent);
    }

    void reconfigureDisplayLocked(DisplayContent displayContent) {
        if (!displayContent.isReady()) {
            return;
        }
        displayContent.configureDisplayPolicy();
        displayContent.setLayoutNeeded();
        int displayId = displayContent.getDisplayId();
        boolean zUpdateOrientationFromAppTokensLocked = updateOrientationFromAppTokensLocked(displayId);
        Configuration configuration = displayContent.getConfiguration();
        this.mTempConfiguration.setTo(configuration);
        displayContent.computeScreenConfiguration(this.mTempConfiguration);
        if (zUpdateOrientationFromAppTokensLocked | (configuration.diff(this.mTempConfiguration) != 0)) {
            this.mWaitingForConfig = true;
            startFreezingDisplayLocked(0, 0, displayContent);
            this.mH.obtainMessage(18, Integer.valueOf(displayId)).sendToTarget();
        }
        this.mWindowPlacerLocked.performSurfacePlacement();
    }

    public void getDisplaysInFocusOrder(SparseIntArray sparseIntArray) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.getDisplaysInFocusOrder(sparseIntArray);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setOverscan(int i, int i2, int i3, int i4, int i5) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                    if (displayContent != null) {
                        setOverscanLocked(displayContent, i2, i3, i4, i5);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void setOverscanLocked(DisplayContent displayContent, int i, int i2, int i3, int i4) {
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        displayInfo.overscanLeft = i;
        displayInfo.overscanTop = i2;
        displayInfo.overscanRight = i3;
        displayInfo.overscanBottom = i4;
        this.mDisplaySettings.setOverscanLocked(displayInfo.uniqueId, displayInfo.name, i, i2, i3, i4);
        this.mDisplaySettings.writeSettingsLocked();
        reconfigureDisplayLocked(displayContent);
    }

    public void startWindowTrace() {
        try {
            this.mWindowTracing.startTrace(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopWindowTrace() {
        this.mWindowTracing.stopTrace(null);
    }

    public boolean isWindowTraceEnabled() {
        return this.mWindowTracing.isEnabled();
    }

    final WindowState windowForClientLocked(Session session, IWindow iWindow, boolean z) {
        return windowForClientLocked(session, iWindow.asBinder(), z);
    }

    final WindowState windowForClientLocked(Session session, IBinder iBinder, boolean z) {
        WindowState windowState = this.mWindowMap.get(iBinder);
        if (localLOGV) {
            Slog.v("WindowManager", "Looking up client " + iBinder + ": " + windowState);
        }
        if (windowState == null) {
            if (z) {
                throw new IllegalArgumentException("Requested window " + iBinder + " does not exist");
            }
            Slog.w("WindowManager", "Failed looking up window callers=" + Debug.getCallers(3));
            return null;
        }
        if (session != null && windowState.mSession != session) {
            if (z) {
                throw new IllegalArgumentException("Requested window " + iBinder + " is in session " + windowState.mSession + ", not " + session);
            }
            Slog.w("WindowManager", "Failed looking up window callers=" + Debug.getCallers(3));
            return null;
        }
        return windowState;
    }

    void makeWindowFreezingScreenIfNeededLocked(WindowState windowState) {
        if (!windowState.mToken.okToDisplay() && this.mWindowsFreezingScreen != 2) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Changing surface while display frozen: " + windowState);
            }
            windowState.setOrientationChanging(true);
            windowState.mLastFreezeDuration = 0;
            this.mRoot.mOrientationChangeComplete = false;
            if (this.mWindowsFreezingScreen == 0) {
                this.mWindowsFreezingScreen = 1;
                this.mH.removeMessages(11);
                this.mH.sendEmptyMessageDelayed(11, 2000L);
            }
        }
    }

    int handleAnimatingStoppedAndTransitionLocked() {
        this.mAppTransition.setIdle();
        for (int size = this.mNoAnimationNotifyOnTransitionFinished.size() - 1; size >= 0; size--) {
            this.mAppTransition.notifyAppTransitionFinishedLocked(this.mNoAnimationNotifyOnTransitionFinished.get(size));
        }
        this.mNoAnimationNotifyOnTransitionFinished.clear();
        DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
        defaultDisplayContentLocked.mWallpaperController.hideDeferredWallpapersIfNeeded();
        defaultDisplayContentLocked.onAppTransitionDone();
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.v("WindowManager", "Wallpaper layer changed: assigning layers + relayout");
        }
        defaultDisplayContentLocked.computeImeTarget(true);
        this.mRoot.mWallpaperMayChange = true;
        this.mFocusMayChange = true;
        return 1;
    }

    void checkDrawnWindowsLocked() {
        if (this.mWaitingForDrawn.isEmpty() || this.mWaitingForDrawnCallback == null) {
            return;
        }
        for (int size = this.mWaitingForDrawn.size() - 1; size >= 0; size--) {
            WindowState windowState = this.mWaitingForDrawn.get(size);
            if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                Slog.i("WindowManager", "Waiting for drawn " + windowState + ": removed=" + windowState.mRemoved + " visible=" + windowState.isVisibleLw() + " mHasSurface=" + windowState.mHasSurface + " drawState=" + windowState.mWinAnimator.mDrawState);
            }
            if (windowState.mRemoved || !windowState.mHasSurface || !windowState.mPolicyVisibility) {
                if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                    Slog.w("WindowManager", "Aborted waiting for drawn: " + windowState);
                }
                this.mWaitingForDrawn.remove(windowState);
            } else if (windowState.hasDrawnLw()) {
                if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                    Slog.d("WindowManager", "Window drawn win=" + windowState);
                }
                this.mWaitingForDrawn.remove(windowState);
            }
        }
        if (this.mWaitingForDrawn.isEmpty()) {
            if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                Slog.d("WindowManager", "All windows drawn!");
            }
            this.mH.removeMessages(24);
            this.mH.sendEmptyMessage(33);
        }
    }

    void setHoldScreenLocked(Session session) {
        boolean z = session != null;
        if (z && this.mHoldingScreenOn != session) {
            this.mHoldingScreenWakeLock.setWorkSource(new WorkSource(session.mUid));
        }
        this.mHoldingScreenOn = session;
        if (z != this.mHoldingScreenWakeLock.isHeld()) {
            if (z) {
                if (WindowManagerDebugConfig.DEBUG_KEEP_SCREEN_ON) {
                    Slog.d("DebugKeepScreenOn", "Acquiring screen wakelock due to " + this.mRoot.mHoldScreenWindow);
                }
                this.mLastWakeLockHoldingWindow = this.mRoot.mHoldScreenWindow;
                this.mLastWakeLockObscuringWindow = null;
                this.mHoldingScreenWakeLock.acquire();
                this.mPolicy.keepScreenOnStartedLw();
                return;
            }
            if (WindowManagerDebugConfig.DEBUG_KEEP_SCREEN_ON) {
                Slog.d("DebugKeepScreenOn", "Releasing screen wakelock, obscured by " + this.mRoot.mObscuringWindow);
            }
            this.mLastWakeLockHoldingWindow = null;
            this.mLastWakeLockObscuringWindow = this.mRoot.mObscuringWindow;
            this.mPolicy.keepScreenOnStoppedLw();
            this.mHoldingScreenWakeLock.release();
        }
    }

    void requestTraversal() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowPlacerLocked.requestTraversal();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void scheduleAnimationLocked() {
        if (this.mAnimator != null) {
            this.mAnimator.scheduleAnimation();
        }
    }

    boolean updateFocusedWindowLocked(int i, boolean z) {
        boolean z2;
        WindowState windowStateComputeFocusedWindow = this.mRoot.computeFocusedWindow();
        if (this.mCurrentFocus == windowStateComputeFocusedWindow) {
            return false;
        }
        Trace.traceBegin(32L, "wmUpdateFocus");
        this.mH.removeMessages(2);
        this.mH.sendEmptyMessage(2);
        DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
        if (this.mInputMethodWindow != null) {
            z2 = this.mInputMethodTarget != defaultDisplayContentLocked.computeImeTarget(true);
            if (i != 1 && i != 3) {
                int i2 = this.mInputMethodWindow.mWinAnimator.mAnimLayer;
                defaultDisplayContentLocked.assignWindowLayers(false);
                z2 |= i2 != this.mInputMethodWindow.mWinAnimator.mAnimLayer;
            }
        } else {
            z2 = false;
        }
        if (z2) {
            this.mWindowsChanged = true;
            defaultDisplayContentLocked.setLayoutNeeded();
            windowStateComputeFocusedWindow = this.mRoot.computeFocusedWindow();
        }
        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT || localLOGV) {
            Slog.v("WindowManager", "Changing focus from " + this.mCurrentFocus + " to " + windowStateComputeFocusedWindow + " Callers=" + Debug.getCallers(4));
        }
        WindowState windowState = this.mCurrentFocus;
        this.mCurrentFocus = windowStateComputeFocusedWindow;
        this.mLosingFocus.remove(windowStateComputeFocusedWindow);
        if (this.mCurrentFocus != null) {
            this.mWinAddedSinceNullFocus.clear();
            this.mWinRemovedSinceNullFocus.clear();
        }
        int iFocusChangedLw = this.mPolicy.focusChangedLw(windowState, windowStateComputeFocusedWindow);
        if (z2 && windowState != this.mInputMethodWindow) {
            if (i == 2) {
                defaultDisplayContentLocked.performLayout(true, z);
                iFocusChangedLw &= -2;
            } else if (i == 3) {
                defaultDisplayContentLocked.assignWindowLayers(false);
            }
        }
        if ((iFocusChangedLw & 1) != 0) {
            defaultDisplayContentLocked.setLayoutNeeded();
            if (i == 2) {
                defaultDisplayContentLocked.performLayout(true, z);
            }
        }
        if (i != 1) {
            this.mInputMonitor.setInputFocusLw(this.mCurrentFocus, z);
        }
        defaultDisplayContentLocked.adjustForImeIfNeeded();
        defaultDisplayContentLocked.scheduleToastWindowsTimeoutIfNeededLocked(windowState, windowStateComputeFocusedWindow);
        Trace.traceEnd(32L);
        return true;
    }

    void startFreezingDisplayLocked(int i, int i2) {
        startFreezingDisplayLocked(i, i2, getDefaultDisplayContentLocked());
    }

    void startFreezingDisplayLocked(int i, int i2, DisplayContent displayContent) {
        if (this.mDisplayFrozen || this.mRotatingSeamlessly || !displayContent.isReady() || !this.mPolicy.isScreenOn() || !displayContent.okToAnimate()) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.d("WindowManager", "startFreezingDisplayLocked: exitAnim=" + i + " enterAnim=" + i2 + " called by " + Debug.getCallers(8));
        }
        this.mScreenFrozenLock.acquire();
        this.mDisplayFrozen = true;
        this.mDisplayFreezeTime = SystemClock.elapsedRealtime();
        this.mLastFinishedFreezeSource = null;
        this.mFrozenDisplayId = displayContent.getDisplayId();
        this.mInputMonitor.freezeInputDispatchingLw();
        this.mPolicy.setLastInputMethodWindowLw(null, null);
        if (this.mAppTransition.isTransitionSet()) {
            this.mAppTransition.freeze();
        }
        if (PROFILE_ORIENTATION) {
            Debug.startMethodTracing(new File("/data/system/frozen").toString(), DumpState.DUMP_VOLUMES);
        }
        this.mLatencyTracker.onActionStart(6);
        if (displayContent.isDefaultDisplay) {
            this.mExitAnimId = i;
            this.mEnterAnimId = i2;
            ScreenRotationAnimation screenRotationAnimationLocked = this.mAnimator.getScreenRotationAnimationLocked(this.mFrozenDisplayId);
            if (screenRotationAnimationLocked != null) {
                screenRotationAnimationLocked.kill();
            }
            boolean zHasSecureWindowOnScreen = displayContent.hasSecureWindowOnScreen();
            displayContent.updateDisplayInfo();
            this.mAnimator.setScreenRotationAnimationLocked(this.mFrozenDisplayId, new ScreenRotationAnimation(this.mContext, displayContent, this.mPolicy.isDefaultOrientationForced(), zHasSecureWindowOnScreen, this));
        }
    }

    void stopFreezingDisplayLocked() {
        if (!this.mDisplayFrozen) {
            return;
        }
        if (!this.mWaitingForConfig && this.mAppsFreezingScreen <= 0) {
            boolean z = true;
            if (this.mWindowsFreezingScreen != 1 && !this.mClientFreezingScreen && this.mOpeningApps.isEmpty()) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.d("WindowManager", "stopFreezingDisplayLocked: Unfreezing now");
                }
                DisplayContent displayContent = this.mRoot.getDisplayContent(this.mFrozenDisplayId);
                int i = this.mFrozenDisplayId;
                this.mFrozenDisplayId = -1;
                this.mDisplayFrozen = false;
                this.mInputMonitor.thawInputDispatchingLw();
                this.mLastDisplayFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
                StringBuilder sb = new StringBuilder(128);
                sb.append("Screen frozen for ");
                TimeUtils.formatDuration(this.mLastDisplayFreezeDuration, sb);
                if (this.mLastFinishedFreezeSource != null) {
                    sb.append(" due to ");
                    sb.append(this.mLastFinishedFreezeSource);
                }
                Slog.i("WindowManager", sb.toString());
                this.mH.removeMessages(17);
                this.mH.removeMessages(30);
                if (PROFILE_ORIENTATION) {
                    Debug.stopMethodTracing();
                }
                ScreenRotationAnimation screenRotationAnimationLocked = this.mAnimator.getScreenRotationAnimationLocked(i);
                if (screenRotationAnimationLocked != null && screenRotationAnimationLocked.hasScreenshot()) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.i("WindowManager", "**** Dismissing screen rotation animation");
                    }
                    DisplayInfo displayInfo = displayContent.getDisplayInfo();
                    if (!this.mPolicy.validateRotationAnimationLw(this.mExitAnimId, this.mEnterAnimId, false)) {
                        this.mEnterAnimId = 0;
                        this.mExitAnimId = 0;
                    }
                    if (screenRotationAnimationLocked.dismiss(this.mTransaction, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, getTransitionAnimationScaleLocked(), displayInfo.logicalWidth, displayInfo.logicalHeight, this.mExitAnimId, this.mEnterAnimId)) {
                        this.mTransaction.apply();
                        scheduleAnimationLocked();
                        z = false;
                    } else {
                        screenRotationAnimationLocked.kill();
                        this.mAnimator.setScreenRotationAnimationLocked(i, null);
                    }
                } else if (screenRotationAnimationLocked != null) {
                    screenRotationAnimationLocked.kill();
                    this.mAnimator.setScreenRotationAnimationLocked(i, null);
                }
                boolean zUpdateOrientationFromAppTokensLocked = updateOrientationFromAppTokensLocked(i);
                this.mH.removeMessages(15);
                this.mH.sendEmptyMessageDelayed(15, 2000L);
                this.mScreenFrozenLock.release();
                if (z) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.d("WindowManager", "Performing post-rotate rotation");
                    }
                    zUpdateOrientationFromAppTokensLocked |= displayContent.updateRotationUnchecked();
                }
                if (zUpdateOrientationFromAppTokensLocked) {
                    this.mH.obtainMessage(18, Integer.valueOf(i)).sendToTarget();
                }
                this.mLatencyTracker.onActionEnd(6);
                return;
            }
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.d("WindowManager", "stopFreezingDisplayLocked: Returning mWaitingForConfig=" + this.mWaitingForConfig + ", mAppsFreezingScreen=" + this.mAppsFreezingScreen + ", mWindowsFreezingScreen=" + this.mWindowsFreezingScreen + ", mClientFreezingScreen=" + this.mClientFreezingScreen + ", mOpeningApps.size()=" + this.mOpeningApps.size());
        }
    }

    static int getPropertyInt(String[] strArr, int i, int i2, int i3, DisplayMetrics displayMetrics) {
        String str;
        if (i < strArr.length && (str = strArr[i]) != null && str.length() > 0) {
            try {
                return Integer.parseInt(str);
            } catch (Exception e) {
            }
        }
        if (i2 == 0) {
            return i3;
        }
        return (int) TypedValue.applyDimension(i2, i3, displayMetrics);
    }

    void createWatermarkInTransaction() {
        if (r7.mWatermark != null) {
            return;
        } else {
            r2 = new java.io.FileInputStream(new java.io.File("/system/etc/setup.conf"));
            r0 = new java.io.DataInputStream(r2);
            r1 = r0.readLine();
            if (r1 != null && r1 != null && r1.length > 0) {
                r3 = getDefaultDisplayContentLocked();
                r7.mWatermark = new com.android.server.wm.Watermark(r3, r3.mRealDisplayMetrics, r1);
            }
            r0.close();
            while (true) {
                return;
            }
        }
    }

    public void setRecentsVisibility(boolean z) {
        this.mAmInternal.enforceCallerIsRecentsOrHasPermission("android.permission.STATUS_BAR", "setRecentsVisibility()");
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.setRecentsVisibilityLw(z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setPipVisibility(boolean z) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") != 0) {
            throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.setPipVisibilityLw(z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setShelfHeight(boolean z, int i) {
        this.mAmInternal.enforceCallerIsRecentsOrHasPermission("android.permission.STATUS_BAR", "setShelfHeight()");
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getPinnedStackController().setAdjustedForShelf(z, i);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void statusBarVisibilityChanged(int i) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") != 0) {
            throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mLastStatusBarVisibility = i;
                updateStatusBarVisibilityLocked(this.mPolicy.adjustSystemUiVisibilityLw(i));
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setNavBarVirtualKeyHapticFeedbackEnabled(boolean z) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") != 0) {
            throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.setNavBarVirtualKeyHapticFeedbackEnabledLw(z);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private boolean updateStatusBarVisibilityLocked(int i) {
        if (this.mLastDispatchedSystemUiVisibility == i) {
            return false;
        }
        int i2 = (this.mLastDispatchedSystemUiVisibility ^ i) & 7 & (~i);
        this.mLastDispatchedSystemUiVisibility = i;
        this.mInputManager.setSystemUiVisibility(i);
        getDefaultDisplayContentLocked().updateSystemUiVisibility(i, i2);
        return true;
    }

    @Override
    public void reevaluateStatusBarVisibility() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (updateStatusBarVisibilityLocked(this.mPolicy.adjustSystemUiVisibilityLw(this.mLastStatusBarVisibility))) {
                    this.mWindowPlacerLocked.requestTraversal();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public int getNavBarPosition() {
        int navBarPosition;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().performLayout(false, false);
                navBarPosition = this.mPolicy.getNavBarPosition();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return navBarPosition;
    }

    @Override
    public WindowManagerPolicy.InputConsumer createInputConsumer(Looper looper, String str, InputEventReceiver.Factory factory) {
        WindowManagerPolicy.InputConsumer inputConsumerCreateInputConsumer;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                inputConsumerCreateInputConsumer = this.mInputMonitor.createInputConsumer(looper, str, factory);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return inputConsumerCreateInputConsumer;
    }

    public void createInputConsumer(IBinder iBinder, String str, InputChannel inputChannel) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mInputMonitor.createInputConsumer(iBinder, str, inputChannel, Binder.getCallingPid(), Binder.getCallingUserHandle());
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean destroyInputConsumer(String str) {
        boolean zDestroyInputConsumer;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                zDestroyInputConsumer = this.mInputMonitor.destroyInputConsumer(str);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return zDestroyInputConsumer;
    }

    public Region getCurrentImeTouchRegion() {
        Region region;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.RESTRICTED_VR_ACCESS") != 0) {
            throw new SecurityException("getCurrentImeTouchRegion is restricted to VR services");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                region = new Region();
                if (this.mInputMethodWindow != null) {
                    this.mInputMethodWindow.getTouchableRegion(region);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return region;
    }

    public boolean hasNavigationBar() {
        return this.mPolicy.hasNavigationBar();
    }

    public void lockNow(Bundle bundle) {
        this.mPolicy.lockNow(bundle);
    }

    public void showRecentApps() {
        this.mPolicy.showRecentApps();
    }

    public boolean isSafeModeEnabled() {
        return this.mSafeMode;
    }

    public boolean clearWindowContentFrameStats(IBinder iBinder) {
        if (!checkCallingPermission("android.permission.FRAME_STATS", "clearWindowContentFrameStats()")) {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState windowState = this.mWindowMap.get(iBinder);
                if (windowState == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                WindowSurfaceController windowSurfaceController = windowState.mWinAnimator.mSurfaceController;
                if (windowSurfaceController == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean zClearWindowContentFrameStats = windowSurfaceController.clearWindowContentFrameStats();
                resetPriorityAfterLockedSection();
                return zClearWindowContentFrameStats;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public WindowContentFrameStats getWindowContentFrameStats(IBinder iBinder) {
        if (!checkCallingPermission("android.permission.FRAME_STATS", "getWindowContentFrameStats()")) {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState windowState = this.mWindowMap.get(iBinder);
                if (windowState == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                WindowSurfaceController windowSurfaceController = windowState.mWinAnimator.mSurfaceController;
                if (windowSurfaceController == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                if (this.mTempWindowRenderStats == null) {
                    this.mTempWindowRenderStats = new WindowContentFrameStats();
                }
                WindowContentFrameStats windowContentFrameStats = this.mTempWindowRenderStats;
                if (windowSurfaceController.getWindowContentFrameStats(windowContentFrameStats)) {
                    resetPriorityAfterLockedSection();
                    return windowContentFrameStats;
                }
                resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void notifyAppRelaunching(IBinder iBinder) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                if (appWindowToken != null) {
                    appWindowToken.startRelaunching();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void notifyAppRelaunchingFinished(IBinder iBinder) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                if (appWindowToken != null) {
                    appWindowToken.finishRelaunching();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void notifyAppRelaunchesCleared(IBinder iBinder) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                if (appWindowToken != null) {
                    appWindowToken.clearRelaunching();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void notifyAppResumedFinished(IBinder iBinder) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                if (appWindowToken != null) {
                    this.mUnknownAppVisibilityController.notifyAppResumedFinished(appWindowToken);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void notifyTaskRemovedFromRecents(int i, int i2) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mTaskSnapshotController.notifyTaskRemovedFromRecents(i, i2);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    @Override
    public int getDockedDividerInsetsLw() {
        return getDefaultDisplayContentLocked().getDockedDividerController().getContentInsets();
    }

    private void dumpPolicyLocked(PrintWriter printWriter, String[] strArr, boolean z) {
        printWriter.println("WINDOW MANAGER POLICY STATE (dumpsys window policy)");
        this.mPolicy.dump("    ", printWriter, strArr);
    }

    private void dumpAnimatorLocked(PrintWriter printWriter, String[] strArr, boolean z) {
        printWriter.println("WINDOW MANAGER ANIMATOR STATE (dumpsys window animator)");
        this.mAnimator.dumpLocked(printWriter, "    ", z);
    }

    private void dumpTokensLocked(PrintWriter printWriter, boolean z) {
        printWriter.println("WINDOW MANAGER TOKENS (dumpsys window tokens)");
        this.mRoot.dumpTokens(printWriter, z);
        if (!this.mOpeningApps.isEmpty() || !this.mClosingApps.isEmpty()) {
            printWriter.println();
            if (this.mOpeningApps.size() > 0) {
                printWriter.print("  mOpeningApps=");
                printWriter.println(this.mOpeningApps);
            }
            if (this.mClosingApps.size() > 0) {
                printWriter.print("  mClosingApps=");
                printWriter.println(this.mClosingApps);
            }
        }
    }

    private void dumpSessionsLocked(PrintWriter printWriter, boolean z) {
        printWriter.println("WINDOW MANAGER SESSIONS (dumpsys window sessions)");
        for (int i = 0; i < this.mSessions.size(); i++) {
            Session sessionValueAt = this.mSessions.valueAt(i);
            printWriter.print("  Session ");
            printWriter.print(sessionValueAt);
            printWriter.println(':');
            sessionValueAt.dump(printWriter, "    ");
        }
    }

    void writeToProtoLocked(ProtoOutputStream protoOutputStream, boolean z) {
        this.mPolicy.writeToProto(protoOutputStream, 1146756268033L);
        this.mRoot.writeToProto(protoOutputStream, 1146756268034L, z);
        if (this.mCurrentFocus != null) {
            this.mCurrentFocus.writeIdentifierToProto(protoOutputStream, 1146756268035L);
        }
        if (this.mFocusedApp != null) {
            this.mFocusedApp.writeNameToProto(protoOutputStream, 1138166333444L);
        }
        if (this.mInputMethodWindow != null) {
            this.mInputMethodWindow.writeIdentifierToProto(protoOutputStream, 1146756268037L);
        }
        protoOutputStream.write(1133871366150L, this.mDisplayFrozen);
        DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
        protoOutputStream.write(1120986464263L, defaultDisplayContentLocked.getRotation());
        protoOutputStream.write(1120986464264L, defaultDisplayContentLocked.getLastOrientation());
        this.mAppTransition.writeToProto(protoOutputStream, 1146756268041L);
    }

    void traceStateLocked(String str) {
        Trace.traceBegin(32L, "traceStateLocked");
        try {
            try {
                this.mWindowTracing.traceStateLocked(str, this);
            } catch (Exception e) {
                Log.wtf("WindowManager", "Exception while tracing state", e);
            }
        } finally {
            Trace.traceEnd(32L);
        }
    }

    private void dumpWindowsLocked(PrintWriter printWriter, boolean z, ArrayList<WindowState> arrayList) {
        printWriter.println("WINDOW MANAGER WINDOWS (dumpsys window windows)");
        dumpWindowsNoHeaderLocked(printWriter, z, arrayList);
    }

    private void dumpWindowsNoHeaderLocked(PrintWriter printWriter, boolean z, ArrayList<WindowState> arrayList) {
        this.mRoot.dumpWindowsNoHeader(printWriter, z, arrayList);
        if (!this.mHidingNonSystemOverlayWindows.isEmpty()) {
            printWriter.println();
            printWriter.println("  Hiding System Alert Windows:");
            for (int size = this.mHidingNonSystemOverlayWindows.size() - 1; size >= 0; size--) {
                WindowState windowState = this.mHidingNonSystemOverlayWindows.get(size);
                printWriter.print("  #");
                printWriter.print(size);
                printWriter.print(' ');
                printWriter.print(windowState);
                if (z) {
                    printWriter.println(":");
                    windowState.dump(printWriter, "    ", true);
                } else {
                    printWriter.println();
                }
            }
        }
        if (this.mPendingRemove.size() > 0) {
            printWriter.println();
            printWriter.println("  Remove pending for:");
            for (int size2 = this.mPendingRemove.size() - 1; size2 >= 0; size2--) {
                WindowState windowState2 = this.mPendingRemove.get(size2);
                if (arrayList == null || arrayList.contains(windowState2)) {
                    printWriter.print("  Remove #");
                    printWriter.print(size2);
                    printWriter.print(' ');
                    printWriter.print(windowState2);
                    if (z) {
                        printWriter.println(":");
                        windowState2.dump(printWriter, "    ", true);
                    } else {
                        printWriter.println();
                    }
                }
            }
        }
        if (this.mForceRemoves != null && this.mForceRemoves.size() > 0) {
            printWriter.println();
            printWriter.println("  Windows force removing:");
            for (int size3 = this.mForceRemoves.size() - 1; size3 >= 0; size3--) {
                WindowState windowState3 = this.mForceRemoves.get(size3);
                printWriter.print("  Removing #");
                printWriter.print(size3);
                printWriter.print(' ');
                printWriter.print(windowState3);
                if (z) {
                    printWriter.println(":");
                    windowState3.dump(printWriter, "    ", true);
                } else {
                    printWriter.println();
                }
            }
        }
        if (this.mDestroySurface.size() > 0) {
            printWriter.println();
            printWriter.println("  Windows waiting to destroy their surface:");
            for (int size4 = this.mDestroySurface.size() - 1; size4 >= 0; size4--) {
                WindowState windowState4 = this.mDestroySurface.get(size4);
                if (arrayList == null || arrayList.contains(windowState4)) {
                    printWriter.print("  Destroy #");
                    printWriter.print(size4);
                    printWriter.print(' ');
                    printWriter.print(windowState4);
                    if (z) {
                        printWriter.println(":");
                        windowState4.dump(printWriter, "    ", true);
                    } else {
                        printWriter.println();
                    }
                }
            }
        }
        if (this.mLosingFocus.size() > 0) {
            printWriter.println();
            printWriter.println("  Windows losing focus:");
            for (int size5 = this.mLosingFocus.size() - 1; size5 >= 0; size5--) {
                WindowState windowState5 = this.mLosingFocus.get(size5);
                if (arrayList == null || arrayList.contains(windowState5)) {
                    printWriter.print("  Losing #");
                    printWriter.print(size5);
                    printWriter.print(' ');
                    printWriter.print(windowState5);
                    if (z) {
                        printWriter.println(":");
                        windowState5.dump(printWriter, "    ", true);
                    } else {
                        printWriter.println();
                    }
                }
            }
        }
        if (this.mResizingWindows.size() > 0) {
            printWriter.println();
            printWriter.println("  Windows waiting to resize:");
            for (int size6 = this.mResizingWindows.size() - 1; size6 >= 0; size6--) {
                WindowState windowState6 = this.mResizingWindows.get(size6);
                if (arrayList == null || arrayList.contains(windowState6)) {
                    printWriter.print("  Resizing #");
                    printWriter.print(size6);
                    printWriter.print(' ');
                    printWriter.print(windowState6);
                    if (z) {
                        printWriter.println(":");
                        windowState6.dump(printWriter, "    ", true);
                    } else {
                        printWriter.println();
                    }
                }
            }
        }
        if (this.mWaitingForDrawn.size() > 0) {
            printWriter.println();
            printWriter.println("  Clients waiting for these windows to be drawn:");
            for (int size7 = this.mWaitingForDrawn.size() - 1; size7 >= 0; size7--) {
                Object obj = (WindowState) this.mWaitingForDrawn.get(size7);
                printWriter.print("  Waiting #");
                printWriter.print(size7);
                printWriter.print(' ');
                printWriter.print(obj);
            }
        }
        printWriter.println();
        printWriter.print("  mGlobalConfiguration=");
        printWriter.println(this.mRoot.getConfiguration());
        printWriter.print("  mHasPermanentDpad=");
        printWriter.println(this.mHasPermanentDpad);
        printWriter.print("  mCurrentFocus=");
        printWriter.println(this.mCurrentFocus);
        if (this.mLastFocus != this.mCurrentFocus) {
            printWriter.print("  mLastFocus=");
            printWriter.println(this.mLastFocus);
        }
        printWriter.print("  mFocusedApp=");
        printWriter.println(this.mFocusedApp);
        if (this.mInputMethodTarget != null) {
            printWriter.print("  mInputMethodTarget=");
            printWriter.println(this.mInputMethodTarget);
        }
        printWriter.print("  mInTouchMode=");
        printWriter.println(this.mInTouchMode);
        printWriter.print("  mLastDisplayFreezeDuration=");
        TimeUtils.formatDuration(this.mLastDisplayFreezeDuration, printWriter);
        if (this.mLastFinishedFreezeSource != null) {
            printWriter.print(" due to ");
            printWriter.print(this.mLastFinishedFreezeSource);
        }
        printWriter.println();
        printWriter.print("  mLastWakeLockHoldingWindow=");
        printWriter.print(this.mLastWakeLockHoldingWindow);
        printWriter.print(" mLastWakeLockObscuringWindow=");
        printWriter.print(this.mLastWakeLockObscuringWindow);
        printWriter.println();
        this.mInputMonitor.dump(printWriter, "  ");
        this.mUnknownAppVisibilityController.dump(printWriter, "  ");
        this.mTaskSnapshotController.dump(printWriter, "  ");
        if (z) {
            printWriter.print("  mSystemDecorLayer=");
            printWriter.print(this.mSystemDecorLayer);
            printWriter.print(" mScreenRect=");
            printWriter.println(this.mScreenRect.toShortString());
            if (this.mLastStatusBarVisibility != 0) {
                printWriter.print("  mLastStatusBarVisibility=0x");
                printWriter.println(Integer.toHexString(this.mLastStatusBarVisibility));
            }
            if (this.mInputMethodWindow != null) {
                printWriter.print("  mInputMethodWindow=");
                printWriter.println(this.mInputMethodWindow);
            }
            this.mWindowPlacerLocked.dump(printWriter, "  ");
            this.mRoot.mWallpaperController.dump(printWriter, "  ");
            printWriter.print("  mSystemBooted=");
            printWriter.print(this.mSystemBooted);
            printWriter.print(" mDisplayEnabled=");
            printWriter.println(this.mDisplayEnabled);
            this.mRoot.dumpLayoutNeededDisplayIds(printWriter);
            printWriter.print("  mTransactionSequence=");
            printWriter.println(this.mTransactionSequence);
            printWriter.print("  mDisplayFrozen=");
            printWriter.print(this.mDisplayFrozen);
            printWriter.print(" windows=");
            printWriter.print(this.mWindowsFreezingScreen);
            printWriter.print(" client=");
            printWriter.print(this.mClientFreezingScreen);
            printWriter.print(" apps=");
            printWriter.print(this.mAppsFreezingScreen);
            printWriter.print(" waitingForConfig=");
            printWriter.println(this.mWaitingForConfig);
            DisplayContent defaultDisplayContentLocked = getDefaultDisplayContentLocked();
            printWriter.print("  mRotation=");
            printWriter.print(defaultDisplayContentLocked.getRotation());
            printWriter.print(" mAltOrientation=");
            printWriter.println(defaultDisplayContentLocked.getAltOrientation());
            printWriter.print("  mLastWindowForcedOrientation=");
            printWriter.print(defaultDisplayContentLocked.getLastWindowForcedOrientation());
            printWriter.print(" mLastOrientation=");
            printWriter.println(defaultDisplayContentLocked.getLastOrientation());
            printWriter.print("  mDeferredRotationPauseCount=");
            printWriter.println(this.mDeferredRotationPauseCount);
            printWriter.print("  Animation settings: disabled=");
            printWriter.print(this.mAnimationsDisabled);
            printWriter.print(" window=");
            printWriter.print(this.mWindowAnimationScaleSetting);
            printWriter.print(" transition=");
            printWriter.print(this.mTransitionAnimationScaleSetting);
            printWriter.print(" animator=");
            printWriter.println(this.mAnimatorDurationScaleSetting);
            printWriter.print("  mSkipAppTransitionAnimation=");
            printWriter.println(this.mSkipAppTransitionAnimation);
            printWriter.println("  mLayoutToAnim:");
            this.mAppTransition.dump(printWriter, "    ");
            if (this.mRecentsAnimationController != null) {
                printWriter.print("  mRecentsAnimationController=");
                printWriter.println(this.mRecentsAnimationController);
                this.mRecentsAnimationController.dump(printWriter, "    ");
            }
        }
    }

    private boolean dumpWindows(PrintWriter printWriter, String str, String[] strArr, int i, boolean z) {
        final ArrayList<WindowState> arrayList = new ArrayList<>();
        if ("apps".equals(str) || "visible".equals(str) || "visible-apps".equals(str)) {
            final boolean zContains = str.contains("apps");
            final boolean zContains2 = str.contains("visible");
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    if (zContains) {
                        this.mRoot.dumpDisplayContents(printWriter);
                    }
                    this.mRoot.forAllWindows(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            WindowManagerService.lambda$dumpWindows$5(zContains2, zContains, arrayList, (WindowState) obj);
                        }
                    }, true);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            resetPriorityAfterLockedSection();
        } else {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mRoot.getWindowsByName(arrayList, str);
                } finally {
                }
            }
            resetPriorityAfterLockedSection();
        }
        if (arrayList.size() <= 0) {
            return false;
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                dumpWindowsLocked(printWriter, z, arrayList);
            } finally {
            }
        }
        resetPriorityAfterLockedSection();
        return true;
    }

    static void lambda$dumpWindows$5(boolean z, boolean z2, ArrayList arrayList, WindowState windowState) {
        if (!z || windowState.mWinAnimator.getShown()) {
            if (!z2 || windowState.mAppToken != null) {
                arrayList.add(windowState);
            }
        }
    }

    private void dumpLastANRLocked(PrintWriter printWriter) {
        printWriter.println("WINDOW MANAGER LAST ANR (dumpsys window lastanr)");
        if (this.mLastANRState == null) {
            printWriter.println("  <no ANR has occurred since boot>");
        } else {
            printWriter.println(this.mLastANRState);
        }
    }

    void saveANRStateLocked(AppWindowToken appWindowToken, WindowState windowState, String str) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter fastPrintWriter = new FastPrintWriter(stringWriter, false, 1024);
        fastPrintWriter.println("  ANR time: " + DateFormat.getDateTimeInstance().format(new Date()));
        if (appWindowToken != null) {
            fastPrintWriter.println("  Application at fault: " + appWindowToken.stringName);
        }
        if (windowState != null) {
            fastPrintWriter.println("  Window at fault: " + ((Object) windowState.mAttrs.getTitle()));
        }
        if (str != null) {
            fastPrintWriter.println("  Reason: " + str);
        }
        if (!this.mWinAddedSinceNullFocus.isEmpty()) {
            fastPrintWriter.println("  Windows added since null focus: " + this.mWinAddedSinceNullFocus);
        }
        if (!this.mWinRemovedSinceNullFocus.isEmpty()) {
            fastPrintWriter.println("  Windows removed since null focus: " + this.mWinRemovedSinceNullFocus);
        }
        fastPrintWriter.println();
        dumpWindowsNoHeaderLocked(fastPrintWriter, true, null);
        fastPrintWriter.println();
        fastPrintWriter.println("Last ANR continued");
        this.mRoot.dumpDisplayContents(fastPrintWriter);
        fastPrintWriter.close();
        this.mLastANRState = stringWriter.toString();
        this.mH.removeMessages(38);
        this.mH.sendEmptyMessageDelayed(38, AppStandbyController.SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        PriorityDump.dump(this.mPriorityDumper, fileDescriptor, printWriter, strArr);
    }

    private void doDump(FileDescriptor fileDescriptor, final PrintWriter printWriter, String[] strArr, boolean z) {
        String str;
        if (DumpUtils.checkDumpPermission(this.mContext, "WindowManager", printWriter)) {
            int i = 0;
            boolean z2 = false;
            while (i < strArr.length && (str = strArr[i]) != null && str.length() > 0 && str.charAt(0) == '-') {
                i++;
                if (!"-a".equals(str)) {
                    if ("-h".equals(str)) {
                        printWriter.println("Window manager dump options:");
                        printWriter.println("  [-a] [-h] [cmd] ...");
                        printWriter.println("  cmd may be one of:");
                        printWriter.println("    l[astanr]: last ANR information");
                        printWriter.println("    p[policy]: policy state");
                        printWriter.println("    a[animator]: animator state");
                        printWriter.println("    s[essions]: active sessions");
                        printWriter.println("    surfaces: active surfaces (debugging enabled only)");
                        printWriter.println("    d[isplays]: active display contents");
                        printWriter.println("    t[okens]: token list");
                        printWriter.println("    w[indows]: window list");
                        printWriter.println("  cmd may also be a NAME to dump windows.  NAME may");
                        printWriter.println("    be a partial substring in a window name, a");
                        printWriter.println("    Window hex object identifier, or");
                        printWriter.println("    \"all\" for all windows, or");
                        printWriter.println("    \"visible\" for the visible windows.");
                        printWriter.println("    \"visible-apps\" for the visible app windows.");
                        printWriter.println("  -a: include all available server state.");
                        printWriter.println("  --proto: output dump in protocol buffer format.");
                        return;
                    }
                    if ("-d".equals(str)) {
                        this.mWindowManagerDebugger.runDebug(printWriter, strArr, i);
                        return;
                    }
                    printWriter.println("Unknown argument: " + str + "; use -h for help");
                } else {
                    z2 = true;
                }
            }
            if (z) {
                ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
                synchronized (this.mWindowMap) {
                    try {
                        boostPriorityForLockedSection();
                        writeToProtoLocked(protoOutputStream, false);
                    } finally {
                    }
                }
                resetPriorityAfterLockedSection();
                protoOutputStream.flush();
                return;
            }
            printWriter.println("Dump time : " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").format(new Date()));
            if (i < strArr.length) {
                String str2 = strArr[i];
                int i2 = i + 1;
                if ("lastanr".equals(str2) || "l".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpLastANRLocked(printWriter);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if ("policy".equals(str2) || "p".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpPolicyLocked(printWriter, strArr, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if ("animator".equals(str2) || "a".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpAnimatorLocked(printWriter, strArr, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if ("sessions".equals(str2) || "s".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpSessionsLocked(printWriter, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if ("displays".equals(str2) || "d".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            this.mRoot.dumpDisplayContents(printWriter);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if ("tokens".equals(str2) || "t".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpTokensLocked(printWriter, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if ("windows".equals(str2) || "w".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpWindowsLocked(printWriter, true, null);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if ("all".equals(str2) || "a".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpWindowsLocked(printWriter, true, null);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if ("containers".equals(str2)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            this.mRoot.dumpChildrenNames(printWriter, " ");
                            printWriter.println(" ");
                            this.mRoot.forAllWindows(new Consumer() {
                                @Override
                                public final void accept(Object obj) {
                                    printWriter.println((WindowState) obj);
                                }
                            }, true);
                        } finally {
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (!dumpWindows(printWriter, str2, strArr, i2, z2)) {
                    printWriter.println("Bad window command, or no windows match: " + str2);
                    printWriter.println("Use -h for help.");
                    return;
                }
                return;
            }
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    printWriter.println();
                    if (z2) {
                        printWriter.println("-------------------------------------------------------------------------------");
                    }
                    dumpLastANRLocked(printWriter);
                    printWriter.println();
                    if (z2) {
                        printWriter.println("-------------------------------------------------------------------------------");
                    }
                    dumpPolicyLocked(printWriter, strArr, z2);
                    printWriter.println();
                    if (z2) {
                        printWriter.println("-------------------------------------------------------------------------------");
                    }
                    dumpAnimatorLocked(printWriter, strArr, z2);
                    printWriter.println();
                    if (z2) {
                        printWriter.println("-------------------------------------------------------------------------------");
                    }
                    dumpSessionsLocked(printWriter, z2);
                    printWriter.println();
                    if (z2) {
                        printWriter.println("-------------------------------------------------------------------------------");
                    }
                    if (z2) {
                        printWriter.println("-------------------------------------------------------------------------------");
                    }
                    this.mRoot.dumpDisplayContents(printWriter);
                    printWriter.println();
                    if (z2) {
                        printWriter.println("-------------------------------------------------------------------------------");
                    }
                    dumpTokensLocked(printWriter, z2);
                    printWriter.println();
                    if (z2) {
                        printWriter.println("-------------------------------------------------------------------------------");
                    }
                    dumpWindowsLocked(printWriter, z2, null);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            resetPriorityAfterLockedSection();
        }
    }

    @Override
    public void monitor() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    DisplayContent getDefaultDisplayContentLocked() {
        return this.mRoot.getDisplayContent(0);
    }

    public void onDisplayAdded(int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mDisplayManager.getDisplay(i) != null) {
                    displayReady(i);
                }
                this.mWindowPlacerLocked.requestTraversal();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void onDisplayRemoved(int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAnimator.removeDisplayLocked(i);
                this.mWindowPlacerLocked.requestTraversal();
                WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
                if (WindowManagerDebugger.WMS_DEBUG_USER) {
                    Slog.v("WindowManager", "onDisplayRemoved id = " + i + " Callers=" + Debug.getCallers(3));
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void onOverlayChanged() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.onOverlayChangedLw();
                getDefaultDisplayContentLocked().updateDisplayInfo();
                requestTraversal();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void onDisplayChanged(int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent != null) {
                    displayContent.updateDisplayInfo();
                }
                this.mWindowPlacerLocked.requestTraversal();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    @Override
    public Object getWindowManagerLock() {
        return this.mWindowMap;
    }

    public void setWillReplaceWindow(IBinder iBinder, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                if (appWindowToken == null) {
                    Slog.w("WindowManager", "Attempted to set replacing window on non-existing app token " + iBinder);
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (!appWindowToken.hasContentToDisplay()) {
                    Slog.w("WindowManager", "Attempted to set replacing window on app token with no content" + iBinder);
                    resetPriorityAfterLockedSection();
                    return;
                }
                appWindowToken.setWillReplaceWindows(z);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void setWillReplaceWindows(IBinder iBinder, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                if (appWindowToken == null) {
                    Slog.w("WindowManager", "Attempted to set replacing window on non-existing app token " + iBinder);
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (!appWindowToken.hasContentToDisplay()) {
                    Slog.w("WindowManager", "Attempted to set replacing window on app token with no content" + iBinder);
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (z) {
                    appWindowToken.setWillReplaceChildWindows();
                } else {
                    appWindowToken.setWillReplaceWindows(false);
                }
                scheduleClearWillReplaceWindows(iBinder, true);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void scheduleClearWillReplaceWindows(IBinder iBinder, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(iBinder);
                if (appWindowToken == null) {
                    Slog.w("WindowManager", "Attempted to reset replacing window on non-existing app token " + iBinder);
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (z) {
                    scheduleWindowReplacementTimeouts(appWindowToken);
                } else {
                    appWindowToken.clearWillReplaceWindows();
                }
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void scheduleWindowReplacementTimeouts(AppWindowToken appWindowToken) {
        if (!this.mWindowReplacementTimeouts.contains(appWindowToken)) {
            this.mWindowReplacementTimeouts.add(appWindowToken);
        }
        this.mH.removeMessages(46);
        this.mH.sendEmptyMessageDelayed(46, 2000L);
    }

    public int getDockedStackSide() {
        int dockSide;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                TaskStack splitScreenPrimaryStackIgnoringVisibility = getDefaultDisplayContentLocked().getSplitScreenPrimaryStackIgnoringVisibility();
                dockSide = splitScreenPrimaryStackIgnoringVisibility == null ? -1 : splitScreenPrimaryStackIgnoringVisibility.getDockSide();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return dockSide;
    }

    public void setDockedStackResizing(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().setResizing(z);
                requestTraversal();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setDockedStackDividerTouchRegion(Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().setTouchRegion(rect);
                setFocusTaskRegionLocked(null);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setResizeDimLayer(boolean z, int i, float f) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().setResizeDimLayer(z, i, f);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setForceResizableTasks(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mForceResizableTasks = z;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setSupportsPictureInPicture(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mSupportsPictureInPicture = z;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    static int dipToPixel(int i, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(1, i, displayMetrics);
    }

    public void registerDockedStackListener(IDockedStackListener iDockedStackListener) {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerDockedStackListener()")) {
            return;
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().mDividerControllerLocked.registerDockedStackListener(iDockedStackListener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void registerPinnedStackListener(int i, IPinnedStackListener iPinnedStackListener) {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerPinnedStackListener()") || !this.mSupportsPictureInPicture) {
            return;
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.getDisplayContent(i).getPinnedStackController().registerPinnedStackListener(iPinnedStackListener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void requestAppKeyboardShortcuts(IResultReceiver iResultReceiver, int i) {
        try {
            WindowState focusedWindow = getFocusedWindow();
            if (focusedWindow != null && focusedWindow.mClient != null) {
                getFocusedWindow().mClient.requestAppKeyboardShortcuts(iResultReceiver, i);
            }
        } catch (RemoteException e) {
        }
    }

    public void getStableInsets(int i, Rect rect) throws RemoteException {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getStableInsetsLocked(i, rect);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void getStableInsetsLocked(int i, Rect rect) {
        rect.setEmpty();
        DisplayContent displayContent = this.mRoot.getDisplayContent(i);
        if (displayContent != null) {
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            this.mPolicy.getStableInsetsLw(displayInfo.rotation, displayInfo.logicalWidth, displayInfo.logicalHeight, displayInfo.displayCutout, rect);
        }
    }

    void intersectDisplayInsetBounds(Rect rect, Rect rect2, Rect rect3) {
        this.mTmpRect3.set(rect);
        this.mTmpRect3.inset(rect2);
        rect3.intersect(this.mTmpRect3);
    }

    private static class MousePositionTracker implements WindowManagerPolicyConstants.PointerEventListener {
        private boolean mLatestEventWasMouse;
        private float mLatestMouseX;
        private float mLatestMouseY;

        private MousePositionTracker() {
        }

        void updatePosition(float f, float f2) {
            synchronized (this) {
                this.mLatestEventWasMouse = true;
                this.mLatestMouseX = f;
                this.mLatestMouseY = f2;
            }
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (motionEvent.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
                updatePosition(motionEvent.getRawX(), motionEvent.getRawY());
            } else {
                synchronized (this) {
                    this.mLatestEventWasMouse = false;
                }
            }
        }
    }

    void updatePointerIcon(IWindow iWindow) {
        synchronized (this.mMousePositionTracker) {
            if (this.mMousePositionTracker.mLatestEventWasMouse) {
                float f = this.mMousePositionTracker.mLatestMouseX;
                float f2 = this.mMousePositionTracker.mLatestMouseY;
                synchronized (this.mWindowMap) {
                    try {
                        boostPriorityForLockedSection();
                        if (this.mDragDropController.dragDropActiveLocked()) {
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        WindowState windowStateWindowForClientLocked = windowForClientLocked((Session) null, iWindow, false);
                        if (windowStateWindowForClientLocked == null) {
                            Slog.w("WindowManager", "Bad requesting window " + iWindow);
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        DisplayContent displayContent = windowStateWindowForClientLocked.getDisplayContent();
                        if (displayContent == null) {
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        WindowState touchableWinAtPointLocked = displayContent.getTouchableWinAtPointLocked(f, f2);
                        if (touchableWinAtPointLocked != windowStateWindowForClientLocked) {
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        try {
                            touchableWinAtPointLocked.mClient.updatePointerIcon(touchableWinAtPointLocked.translateToWindowX(f), touchableWinAtPointLocked.translateToWindowY(f2));
                        } catch (RemoteException e) {
                            Slog.w("WindowManager", "unable to update pointer icon");
                        }
                        resetPriorityAfterLockedSection();
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            }
        }
    }

    void restorePointerIconLocked(DisplayContent displayContent, float f, float f2) {
        this.mMousePositionTracker.updatePosition(f, f2);
        WindowState touchableWinAtPointLocked = displayContent.getTouchableWinAtPointLocked(f, f2);
        if (touchableWinAtPointLocked != null) {
            try {
                touchableWinAtPointLocked.mClient.updatePointerIcon(touchableWinAtPointLocked.translateToWindowX(f), touchableWinAtPointLocked.translateToWindowY(f2));
                return;
            } catch (RemoteException e) {
                Slog.w("WindowManager", "unable to restore pointer icon");
                return;
            }
        }
        InputManager.getInstance().setPointerIconType(1000);
    }

    void updateTapExcludeRegion(IWindow iWindow, int i, int i2, int i3, int i4, int i5) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState windowStateWindowForClientLocked = windowForClientLocked((Session) null, iWindow, false);
                if (windowStateWindowForClientLocked == null) {
                    Slog.w("WindowManager", "Bad requesting window " + iWindow);
                    resetPriorityAfterLockedSection();
                    return;
                }
                windowStateWindowForClientLocked.updateTapExcludeRegion(i, i2, i3, i4, i5);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void dontOverrideDisplayInfo(int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContentOrCreate = getDisplayContentOrCreate(i);
                if (displayContentOrCreate == null) {
                    throw new IllegalArgumentException("Trying to register a non existent display.");
                }
                displayContentOrCreate.mShouldOverrideDisplayConfiguration = false;
                this.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(i, (DisplayInfo) null);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void registerShortcutKey(long j, IShortcutService iShortcutService) throws RemoteException {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerShortcutKey")) {
            throw new SecurityException("Requires REGISTER_WINDOW_MANAGER_LISTENERS permission");
        }
        this.mPolicy.registerShortcutKey(j, iShortcutService);
    }

    public void requestUserActivityNotification() {
        if (!checkCallingPermission("android.permission.USER_ACTIVITY", "requestUserActivityNotification()")) {
            throw new SecurityException("Requires USER_ACTIVITY permission");
        }
        this.mPolicy.requestUserActivityNotification();
    }

    void markForSeamlessRotation(WindowState windowState, boolean z) {
        if (z == windowState.mSeamlesslyRotated) {
            return;
        }
        windowState.mSeamlesslyRotated = z;
        if (z) {
            this.mSeamlessRotationCount++;
        } else {
            this.mSeamlessRotationCount--;
        }
        if (this.mSeamlessRotationCount == 0) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.i("WindowManager", "Performing post-rotate rotation after seamless rotation");
            }
            finishSeamlessRotation();
            DisplayContent displayContent = windowState.getDisplayContent();
            if (displayContent.updateRotationUnchecked()) {
                this.mH.obtainMessage(18, Integer.valueOf(displayContent.getDisplayId())).sendToTarget();
            }
        }
    }

    private final class LocalService extends WindowManagerInternal {
        private LocalService() {
        }

        @Override
        public void requestTraversalFromDisplayManager() {
            WindowManagerService.this.requestTraversal();
        }

        @Override
        public void setMagnificationSpec(MagnificationSpec magnificationSpec) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.setMagnificationSpecLocked(magnificationSpec);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            if (Binder.getCallingPid() != Process.myPid()) {
                magnificationSpec.recycle();
            }
        }

        @Override
        public void setForceShowMagnifiableBounds(boolean z) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.setForceShowMagnifiableBoundsLocked(z);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void getMagnificationRegion(Region region) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.getMagnificationRegionLocked(region);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public MagnificationSpec getCompatibleMagnificationSpecForWindow(IBinder iBinder) {
            MagnificationSpec magnificationSpecForWindowLocked;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = WindowManagerService.this.mWindowMap.get(iBinder);
                    if (windowState == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        magnificationSpecForWindowLocked = WindowManagerService.this.mAccessibilityController.getMagnificationSpecForWindowLocked(windowState);
                    } else {
                        magnificationSpecForWindowLocked = null;
                    }
                    if ((magnificationSpecForWindowLocked == null || magnificationSpecForWindowLocked.isNop()) && windowState.mGlobalScale == 1.0f) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    MagnificationSpec magnificationSpecObtain = magnificationSpecForWindowLocked == null ? MagnificationSpec.obtain() : MagnificationSpec.obtain(magnificationSpecForWindowLocked);
                    magnificationSpecObtain.scale *= windowState.mGlobalScale;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return magnificationSpecObtain;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override
        public void setMagnificationCallbacks(WindowManagerInternal.MagnificationCallbacks magnificationCallbacks) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController == null) {
                        WindowManagerService.this.mAccessibilityController = new AccessibilityController(WindowManagerService.this);
                    }
                    WindowManagerService.this.mAccessibilityController.setMagnificationCallbacksLocked(magnificationCallbacks);
                    if (!WindowManagerService.this.mAccessibilityController.hasCallbacksLocked()) {
                        WindowManagerService.this.mAccessibilityController = null;
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void setWindowsForAccessibilityCallback(WindowManagerInternal.WindowsForAccessibilityCallback windowsForAccessibilityCallback) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController == null) {
                        WindowManagerService.this.mAccessibilityController = new AccessibilityController(WindowManagerService.this);
                    }
                    WindowManagerService.this.mAccessibilityController.setWindowsForAccessibilityCallback(windowsForAccessibilityCallback);
                    if (!WindowManagerService.this.mAccessibilityController.hasCallbacksLocked()) {
                        WindowManagerService.this.mAccessibilityController = null;
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void setInputFilter(IInputFilter iInputFilter) {
            WindowManagerService.this.mInputManager.setInputFilter(iInputFilter);
        }

        @Override
        public IBinder getFocusedWindowToken() {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState focusedWindowLocked = WindowManagerService.this.getFocusedWindowLocked();
                    if (focusedWindowLocked != null) {
                        IBinder iBinderAsBinder = focusedWindowLocked.mClient.asBinder();
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return iBinderAsBinder;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override
        public boolean isKeyguardLocked() {
            return WindowManagerService.this.isKeyguardLocked();
        }

        @Override
        public boolean isKeyguardShowingAndNotOccluded() {
            return WindowManagerService.this.isKeyguardShowingAndNotOccluded();
        }

        @Override
        public void showGlobalActions() {
            WindowManagerService.this.showGlobalActions();
        }

        @Override
        public void getWindowFrame(IBinder iBinder, Rect rect) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = WindowManagerService.this.mWindowMap.get(iBinder);
                    if (windowState != null) {
                        rect.set(windowState.mFrame);
                    } else {
                        rect.setEmpty();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void waitForAllWindowsDrawn(Runnable runnable, long j) {
            boolean z;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mWaitingForDrawnCallback = runnable;
                    WindowManagerService.this.getDefaultDisplayContentLocked().waitForAllWindowsDrawn();
                    WindowManagerService.this.mWindowPlacerLocked.requestTraversal();
                    WindowManagerService.this.mH.removeMessages(24);
                    if (WindowManagerService.this.mWaitingForDrawn.isEmpty()) {
                        z = true;
                    } else {
                        WindowManagerService.this.mH.sendEmptyMessageDelayed(24, j);
                        WindowManagerService.this.checkDrawnWindowsLocked();
                        z = false;
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            if (z) {
                runnable.run();
            }
        }

        @Override
        public void addWindowToken(IBinder iBinder, int i, int i2) {
            WindowManagerService.this.addWindowToken(iBinder, i, i2);
        }

        @Override
        public void removeWindowToken(IBinder iBinder, boolean z, int i) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (z) {
                        DisplayContent displayContent = WindowManagerService.this.mRoot.getDisplayContent(i);
                        if (displayContent == null) {
                            Slog.w("WindowManager", "removeWindowToken: Attempted to remove token: " + iBinder + " for non-exiting displayId=" + i);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        WindowToken windowTokenRemoveWindowToken = displayContent.removeWindowToken(iBinder);
                        if (windowTokenRemoveWindowToken == null) {
                            Slog.w("WindowManager", "removeWindowToken: Attempted to remove non-existing token: " + iBinder);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        windowTokenRemoveWindowToken.removeAllWindowsIfPossible();
                    }
                    WindowManagerService.this.removeWindowToken(iBinder, i);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override
        public void registerAppTransitionListener(WindowManagerInternal.AppTransitionListener appTransitionListener) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mAppTransition.registerListenerLocked(appTransitionListener);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public int getInputMethodWindowVisibleHeight() {
            int inputMethodWindowVisibleHeight;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    inputMethodWindowVisibleHeight = WindowManagerService.this.getDefaultDisplayContentLocked().mDisplayFrames.getInputMethodWindowVisibleHeight();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return inputMethodWindowVisibleHeight;
        }

        @Override
        public void saveLastInputMethodWindowForTransition() {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mInputMethodWindow != null) {
                        WindowManagerService.this.mPolicy.setLastInputMethodWindowLw(WindowManagerService.this.mInputMethodWindow, WindowManagerService.this.mInputMethodTarget);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void clearLastInputMethodWindowForTransition() {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mPolicy.setLastInputMethodWindowLw(null, null);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void updateInputMethodWindowStatus(IBinder iBinder, boolean z, boolean z2, IBinder iBinder2) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.w("WindowManager", "updateInputMethodWindowStatus: imeToken=" + iBinder + " dismissImeOnBackKeyPressed=" + z2 + " imeWindowVisible=" + z + " targetWindowToken=" + iBinder2);
            }
            WindowManagerService.this.mPolicy.setDismissImeOnBackKeyPressed(z2);
        }

        @Override
        public boolean isHardKeyboardAvailable() {
            boolean z;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    z = WindowManagerService.this.mHardKeyboardAvailable;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        }

        @Override
        public void setOnHardKeyboardStatusChangeListener(WindowManagerInternal.OnHardKeyboardStatusChangeListener onHardKeyboardStatusChangeListener) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mHardKeyboardStatusChangeListener = onHardKeyboardStatusChangeListener;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public boolean isStackVisible(int i) {
            boolean zIsStackVisible;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    zIsStackVisible = WindowManagerService.this.getDefaultDisplayContentLocked().isStackVisible(i);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return zIsStackVisible;
        }

        @Override
        public boolean isDockedDividerResizing() {
            boolean zIsResizing;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    zIsResizing = WindowManagerService.this.getDefaultDisplayContentLocked().getDockedDividerController().isResizing();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return zIsResizing;
        }

        @Override
        public void computeWindowsForAccessibility() {
            AccessibilityController accessibilityController;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    accessibilityController = WindowManagerService.this.mAccessibilityController;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            if (accessibilityController != null) {
                accessibilityController.performComputeChangedWindowsNotLocked();
            }
        }

        @Override
        public void setVr2dDisplayId(int i) {
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                Slog.d("WindowManager", "setVr2dDisplayId called for: " + i);
            }
            synchronized (WindowManagerService.this) {
                WindowManagerService.this.mVr2dDisplayId = i;
            }
        }

        @Override
        public void registerDragDropControllerCallback(WindowManagerInternal.IDragDropCallback iDragDropCallback) {
            WindowManagerService.this.mDragDropController.registerCallback(iDragDropCallback);
        }

        @Override
        public void lockNow() {
            WindowManagerService.this.lockNow(null);
        }

        @Override
        public int getWindowOwnerUserId(IBinder iBinder) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = WindowManagerService.this.mWindowMap.get(iBinder);
                    if (windowState != null) {
                        int userId = UserHandle.getUserId(windowState.mOwnerUid);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return userId;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -10000;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }
    }

    void registerAppFreezeListener(AppFreezeListener appFreezeListener) {
        if (!this.mAppFreezeListeners.contains(appFreezeListener)) {
            this.mAppFreezeListeners.add(appFreezeListener);
        }
    }

    void unregisterAppFreezeListener(AppFreezeListener appFreezeListener) {
        this.mAppFreezeListeners.remove(appFreezeListener);
    }

    public void inSurfaceTransaction(Runnable runnable) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                SurfaceControl.openTransaction();
                try {
                    runnable.run();
                } finally {
                    SurfaceControl.closeTransaction();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void disableNonVrUi(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                boolean z2 = !z;
                if (z2 == this.mShowAlertWindowNotifications) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mShowAlertWindowNotifications = z2;
                for (int size = this.mSessions.size() - 1; size >= 0; size--) {
                    this.mSessions.valueAt(size).setShowingAlertWindowNotificationAllowed(this.mShowAlertWindowNotifications);
                }
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    boolean hasWideColorGamutSupport() {
        return this.mHasWideColorGamutSupport && SystemProperties.getInt("persist.sys.sf.native_mode", 0) != 1;
    }

    void updateNonSystemOverlayWindowsVisibilityIfNeeded(WindowState windowState, boolean z) {
        if (!windowState.hideNonSystemOverlayWindowsWhenVisible() && !this.mHidingNonSystemOverlayWindows.contains(windowState)) {
            return;
        }
        boolean z2 = !this.mHidingNonSystemOverlayWindows.isEmpty();
        if (z) {
            if (!this.mHidingNonSystemOverlayWindows.contains(windowState)) {
                this.mHidingNonSystemOverlayWindows.add(windowState);
            }
        } else {
            this.mHidingNonSystemOverlayWindows.remove(windowState);
        }
        final boolean z3 = !this.mHidingNonSystemOverlayWindows.isEmpty();
        if (z2 == z3) {
            return;
        }
        this.mRoot.forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((WindowState) obj).setForceHideNonSystemOverlayWindowIfNeeded(z3);
            }
        }, false);
    }

    public void applyMagnificationSpec(MagnificationSpec magnificationSpec) {
        getDefaultDisplayContentLocked().applyMagnificationSpec(magnificationSpec);
    }

    SurfaceControl.Builder makeSurfaceBuilder(SurfaceSession surfaceSession) {
        return this.mSurfaceBuilderFactory.make(surfaceSession);
    }

    void sendSetRunningRemoteAnimation(int i, boolean z) {
        this.mH.obtainMessage(59, i, z ? 1 : 0).sendToTarget();
    }

    void startSeamlessRotation() {
        this.mSeamlessRotationCount = 0;
        this.mRotatingSeamlessly = true;
    }

    void finishSeamlessRotation() {
        this.mRotatingSeamlessly = false;
    }

    public void onLockTaskStateChanged(int i) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.onLockTaskStateChangedLw(i);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setAodShowing(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mPolicy.setAodShowing(z)) {
                    this.mWindowPlacerLocked.performSurfacePlacement();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }
}
