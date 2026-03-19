package com.android.server.wm;

import android.R;
import android.animation.ValueAnimator;
import android.annotation.IntDef;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputManager;
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
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemService;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.view.AppTransitionAnimationSpec;
import android.view.Choreographer;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IApplicationToken;
import android.view.IDockedStackListener;
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
import android.view.WindowContentFrameStats;
import android.view.WindowManager;
import android.view.WindowManagerInternal;
import android.view.WindowManagerPolicy;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.app.IAssistScreenshotReceiver;
import com.android.internal.os.IResultReceiver;
import com.android.internal.policy.IShortcutService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.WindowManagerPolicyThread;
import com.android.server.AttributeCache;
import com.android.server.DisplayThread;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.input.InputManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.ZenModeHelper;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.power.ShutdownThread;
import com.android.server.wm.WindowSurfaceController;
import com.mediatek.anrappframeworks.ANRAppFrameworks;
import com.mediatek.anrappmanager.ANRAppManager;
import com.mediatek.datashaping.DataShapingUtils;
import com.mediatek.multiwindow.IFreeformStackListener;
import com.mediatek.multiwindow.MultiWindowManager;
import com.mediatek.perfservice.IPerfServiceWrapper;
import com.mediatek.perfservice.PerfServiceWrapper;
import com.mediatek.pq.IAppDetectionService;
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class WindowManagerService extends IWindowManager.Stub implements Watchdog.Monitor, WindowManagerPolicy.WindowManagerFuncs {
    public static final String ALARM_BOOT_DONE = "android.intent.action.normal.boot.done";
    private static final boolean ALWAYS_KEEP_CURRENT = true;
    private static final int ANIMATION_DURATION_SCALE = 2;
    private static final int BOOT_ANIMATION_POLL_INTERVAL = 200;
    private static final String BOOT_ANIMATION_SERVICE = "bootanim";
    static final int CACHE_CONFIG_FIRST_FRAME = 1;
    static final int CACHE_CONFIG_LAST_FRAME = 2;
    static final int CACHE_CONFIG_STARTING_WINDOW = 0;
    static final boolean CUSTOM_SCREEN_ROTATION = true;
    static final long DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS = 30000000000L;
    private static final String DENSITY_OVERRIDE = "ro.config.density_override";
    private static final float DRAG_SHADOW_ALPHA_TRANSPARENT = 0.7071f;
    private static final int INPUT_DEVICES_READY_FOR_SAFE_MODE_DETECTION_TIMEOUT_MILLIS = 1000;
    public static final String IPO_DISABLE = "android.intent.action.ACTION_BOOT_IPO";
    public static final String IPO_ENABLE = "android.intent.action.ACTION_SHUTDOWN_IPO";
    static final boolean IS_USER_BUILD;
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    static final int LAYER_OFFSET_DIM = 1;
    static final int LAYER_OFFSET_THUMBNAIL = 4;
    static final int LAYOUT_REPEAT_THRESHOLD = 4;
    static final int MAX_ANIMATION_DURATION = 10000;
    private static final int MAX_SCREENSHOT_RETRIES = 3;
    public static final String PREBOOT_IPO = "android.intent.action.ACTION_PREBOOT_IPO";
    private static final String PROPERTY_BUILD_DATE_UTC = "ro.build.date.utc";
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    private static final String SIZE_OVERRIDE = "ro.config.size_override";
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    private static final String SYSTEM_SECURE = "ro.secure";
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
    AccessibilityController mAccessibilityController;
    final IActivityManager mActivityManager;
    private final WindowManagerInternal.AppTransitionListener mActivityManagerAppTransitionNotifier;
    final boolean mAllowAnimationsInLowPowerMode;
    final boolean mAllowBootMessages;
    boolean mAllowTheaterModeWakeFromLayout;
    boolean mAltOrientation;
    final ActivityManagerInternal mAmInternal;
    boolean mAnimateWallpaperWithTarget;
    boolean mAnimationScheduled;
    boolean mAnimationsDisabled;
    final WindowAnimator mAnimator;
    float mAnimatorDurationScaleSetting;
    final AppOpsManager mAppOps;
    final AppTransition mAppTransition;
    int mAppsFreezingScreen;
    private final LruCache<String, Bitmap> mBitmaps;
    boolean mBootAnimationStopped;
    private final BoundsAnimationController mBoundsAnimationController;
    final BroadcastReceiver mBroadcastReceiver;
    private final int mCacheBehavior;
    private final ArrayList<Integer> mChangedStackList;
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
    final ArrayList<WindowState> mDestroyPreservedSurface;
    final ArrayList<WindowState> mDestroySurface;
    private final boolean mDisableFastStartingWindow;
    SparseArray<DisplayContent> mDisplayContents;
    boolean mDisplayEnabled;
    long mDisplayFreezeTime;
    boolean mDisplayFrozen;
    final DisplayManager mDisplayManager;
    final DisplayManagerInternal mDisplayManagerInternal;
    final DisplayMetrics mDisplayMetrics;
    boolean mDisplayReady;
    final DisplaySettings mDisplaySettings;
    final Display[] mDisplays;
    Rect mDockedStackCreateBounds;
    int mDockedStackCreateMode;
    DragState mDragState;
    final long mDrawLockTimeoutMillis;
    EmulatorDisplayOverlay mEmulatorDisplayOverlay;
    int mEnterAnimId;
    private boolean mEventDispatchingEnabled;
    int mExitAnimId;
    private final boolean mFastStartingWindowSupport;
    final ArrayList<AppWindowToken> mFinishedEarlyAnim;
    final ArrayList<AppWindowToken> mFinishedStarting;
    boolean mFocusMayChange;
    AppWindowToken mFocusedApp;
    boolean mForceDisplayEnabled;
    final ArrayList<WindowState> mForceRemoves;
    boolean mForceResizableTasks;
    int mForcedAppOrientation;
    private final RemoteCallbackList<IFreeformStackListener> mFreeformStackListeners;
    final SurfaceSession mFxSession;
    final H mH;
    boolean mHardKeyboardAvailable;
    WindowManagerInternal.OnHardKeyboardStatusChangeListener mHardKeyboardStatusChangeListener;
    final boolean mHasPermanentDpad;
    boolean mHasReceiveIPO;
    final boolean mHaveInputMethods;
    private ArrayList<WindowState> mHidingNonSystemOverlayWindows;
    Session mHoldingScreenOn;
    PowerManager.WakeLock mHoldingScreenWakeLock;
    boolean mInTouchMode;
    InputConsumerImpl mInputConsumer;
    final InputManagerService mInputManager;
    final ArrayList<WindowState> mInputMethodDialogs;
    IInputMethodManager mInputMethodManager;
    WindowState mInputMethodTarget;
    boolean mInputMethodTargetWaitingAnim;
    WindowState mInputMethodWindow;
    final InputMonitor mInputMonitor;
    int mIpoRotation;
    private boolean mIsAlarmBooting;
    private boolean mIsPerfBoostEnable;
    private boolean mIsRestoreButtonVisible;
    boolean mIsTouchDevice;
    private boolean mIsUpdateAlarmBootRotation;
    boolean mIsUpdateIpoRotation;
    private final KeyguardDisableHandler mKeyguardDisableHandler;
    private boolean mKeyguardWaitingForActivityDrawn;
    String mLastANRState;
    int mLastDispatchedSystemUiVisibility;
    int mLastDisplayFreezeDuration;
    Object mLastFinishedFreezeSource;
    WindowState mLastFocus;
    int mLastKeyguardForcedOrientation;
    int mLastStatusBarVisibility;
    WindowState mLastWakeLockHoldingWindow;
    WindowState mLastWakeLockObscuringWindow;
    int mLastWindowForcedOrientation;
    final WindowLayersController mLayersController;
    int mLayoutSeq;
    final boolean mLimitedAlphaCompositing;
    ArrayList<WindowState> mLosingFocus;
    private MousePositionTracker mMousePositionTracker;
    final List<IBinder> mNoAnimationNotifyOnTransitionFinished;
    final boolean mOnlyCore;
    final ArraySet<AppWindowToken> mOpeningApps;
    final ArrayList<WindowState> mPendingRemove;
    WindowState[] mPendingRemoveTmp;
    private IPerfServiceWrapper mPerfService;
    private final PointerEventDispatcher mPointerEventDispatcher;
    final WindowManagerPolicy mPolicy;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    final DisplayMetrics mRealDisplayMetrics;
    WindowState[] mRebuildTmp;
    private final DisplayContentList mReconfigureOnConfigurationChanged;
    final ArrayList<AppWindowToken> mReplacingWindowTimeouts;
    final ArrayList<WindowState> mResizingWindows;
    int mRotation;
    ArrayList<RotationWatcher> mRotationWatchers;
    boolean mSafeMode;
    SparseArray<Boolean> mScreenCaptureDisabled;
    private final PowerManager.WakeLock mScreenFrozenLock;
    final Rect mScreenRect;
    final ArraySet<Session> mSessions;
    SettingsObserver mSettingsObserver;
    boolean mShowingBootMessages;
    boolean mSkipAppTransitionAnimation;
    SparseArray<TaskStack> mStackIdToStack;
    StrictModeFlash mStrictModeFlash;
    boolean mSystemBooted;
    int mSystemDecorLayer;
    SparseArray<Task> mTaskIdToTask;
    TaskPositioner mTaskPositioner;
    final Configuration mTempConfiguration;
    private WindowContentFrameStats mTempWindowRenderStats;
    final DisplayMetrics mTmpDisplayMetrics;
    final float[] mTmpFloats;
    final Rect mTmpRect;
    final Rect mTmpRect2;
    final Rect mTmpRect3;
    private final SparseIntArray mTmpTaskIds;
    final ArrayList<WindowState> mTmpWindows;
    final HashMap<IBinder, WindowToken> mTokenMap;
    int mTransactionSequence;
    float mTransitionAnimationScaleSetting;
    boolean mTurnOnScreen;
    private ViewServer mViewServer;
    boolean mWaitingForConfig;
    ArrayList<WindowState> mWaitingForDrawn;
    Runnable mWaitingForDrawnCallback;
    WallpaperController mWallpaperControllerLocked;
    InputConsumerImpl mWallpaperInputConsumer;
    Watermark mWatermark;
    float mWindowAnimationScaleSetting;
    final ArrayList<WindowChangeListener> mWindowChangeListeners;
    final HashMap<IBinder, WindowState> mWindowMap;
    final WindowSurfacePlacer mWindowPlacerLocked;
    boolean mWindowsChanged;
    int mWindowsFreezingScreen;
    static final String TAG = "WindowManager";
    static boolean PROFILE_ORIENTATION = false;
    static boolean localLOGV = WindowManagerDebugConfig.DEBUG;

    @IntDef({0, ZenModeHelper.SUPPRESSED_EFFECT_NOTIFICATIONS, ZenModeHelper.SUPPRESSED_EFFECT_CALLS})
    @Retention(RetentionPolicy.SOURCE)
    private @interface UpdateAnimationScaleMode {
    }

    public interface WindowChangeListener {
        void focusChanged();

        void windowsChanged();
    }

    WindowManagerService(Context context, InputManagerService inputManager, boolean haveInputMethods, boolean showBootMsgs, boolean onlyCore, WindowManagerService windowManagerService) {
        this(context, inputManager, haveInputMethods, showBootMsgs, onlyCore);
    }

    static {
        IS_USER_BUILD = !"user".equals(Build.TYPE) ? "userdebug".equals(Build.TYPE) : true;
    }

    int getDragLayerLocked() {
        return (this.mPolicy.windowTypeToLayerLw(2016) * 10000) + 1000;
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
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.mDisplayInversionEnabledUri, false, this, -1);
            resolver.registerContentObserver(this.mWindowAnimationScaleUri, false, this, -1);
            resolver.registerContentObserver(this.mTransitionAnimationScaleUri, false, this, -1);
            resolver.registerContentObserver(this.mAnimationDurationScaleUri, false, this, -1);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            int mode;
            if (uri == null) {
                return;
            }
            if (this.mDisplayInversionEnabledUri.equals(uri)) {
                WindowManagerService.this.updateCircularDisplayMaskIfNeeded();
                return;
            }
            if (this.mWindowAnimationScaleUri.equals(uri)) {
                mode = 0;
            } else if (this.mTransitionAnimationScaleUri.equals(uri)) {
                mode = 1;
            } else if (this.mAnimationDurationScaleUri.equals(uri)) {
                mode = 2;
            } else {
                return;
            }
            Message m = WindowManagerService.this.mH.obtainMessage(51, mode, 0);
            WindowManagerService.this.mH.sendMessage(m);
        }
    }

    final class DragInputEventReceiver extends InputEventReceiver {
        private boolean mIsStartEvent;
        private boolean mStylusButtonDownAtStart;

        public DragInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
            this.mIsStartEvent = true;
        }

        public void onInputEvent(InputEvent event) {
            boolean handled = false;
            try {
                if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0 && WindowManagerService.this.mDragState != null) {
                    MotionEvent motionEvent = (MotionEvent) event;
                    boolean endDrag = false;
                    float newX = motionEvent.getRawX();
                    float newY = motionEvent.getRawY();
                    boolean isStylusButtonDown = (motionEvent.getButtonState() & 32) != 0;
                    if (this.mIsStartEvent) {
                        if (isStylusButtonDown) {
                            this.mStylusButtonDownAtStart = true;
                        }
                        this.mIsStartEvent = false;
                    }
                    switch (motionEvent.getAction()) {
                        case 0:
                            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                                Slog.w("WindowManager", "Unexpected ACTION_DOWN in drag layer");
                            }
                            if (endDrag) {
                                if (WindowManagerDebugConfig.DEBUG_DRAG) {
                                    Slog.d("WindowManager", "Drag ended; tearing down state");
                                }
                                synchronized (WindowManagerService.this.mWindowMap) {
                                    WindowManagerService.this.mDragState.endDragLw();
                                }
                                this.mStylusButtonDownAtStart = false;
                                this.mIsStartEvent = true;
                            }
                            handled = true;
                            break;
                        case 1:
                            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                                Slog.d("WindowManager", "Got UP on move channel; dropping at " + newX + "," + newY);
                            }
                            synchronized (WindowManagerService.this.mWindowMap) {
                                endDrag = WindowManagerService.this.mDragState.notifyDropLw(newX, newY);
                            }
                            if (endDrag) {
                            }
                            handled = true;
                            break;
                        case 2:
                            if (this.mStylusButtonDownAtStart && !isStylusButtonDown) {
                                if (WindowManagerDebugConfig.DEBUG_DRAG) {
                                    Slog.d("WindowManager", "Button no longer pressed; dropping at " + newX + "," + newY);
                                }
                                synchronized (WindowManagerService.this.mWindowMap) {
                                    endDrag = WindowManagerService.this.mDragState.notifyDropLw(newX, newY);
                                }
                            } else {
                                synchronized (WindowManagerService.this.mWindowMap) {
                                    WindowManagerService.this.mDragState.notifyMoveLw(newX, newY);
                                }
                            }
                            if (endDrag) {
                            }
                            handled = true;
                            break;
                        case 3:
                            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                                Slog.d("WindowManager", "Drag cancelled!");
                            }
                            endDrag = true;
                            if (endDrag) {
                            }
                            handled = true;
                            break;
                        default:
                            if (endDrag) {
                            }
                            handled = true;
                            break;
                    }
                }
            } catch (Exception e) {
                Slog.e("WindowManager", "Exception caught by drag handleMotion", e);
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
                holder[0] = new WindowManagerService(context, im, haveInputMethods, showBootMsgs, onlyCore, null);
            }
        }, 0L);
        return holder[0];
    }

    private void initPolicy() {
        UiThread.getHandler().runWithScissors(new Runnable() {
            @Override
            public void run() {
                WindowManagerPolicyThread.set(Thread.currentThread(), Looper.myLooper());
                if ("eng".equals(Build.TYPE)) {
                    Looper.myLooper().setMessageLogging(ANRAppManager.getDefault(new ANRAppFrameworks()).newMessageLogger(false, Thread.currentThread().getName()));
                }
                WindowManagerService.this.mPolicy.init(WindowManagerService.this.mContext, WindowManagerService.this, WindowManagerService.this);
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
                    return;
                }
                if ("android.intent.action.ACTION_BOOT_IPO".equals(action)) {
                    WindowManagerService.this.mDisplayEnabled = false;
                    SystemProperties.set("service.bootanim.exit", "1");
                    Slog.v(WindowManagerService.TAG, "set 'service.bootanim.exit' = 1");
                    WindowManagerService.this.mIsAlarmBooting = WindowManagerService.this.isAlarmBoot();
                    if (WindowManagerService.this.mIsAlarmBooting) {
                        Slog.v(WindowManagerService.TAG, "Alarm boot is running");
                        WindowManagerService.this.mInputMonitor.setEventDispatchingLw(true);
                    } else {
                        Slog.v(WindowManagerService.TAG, "Alarm boot is not running");
                        WindowManagerService.this.mH.sendMessage(WindowManagerService.this.mH.obtainMessage(16));
                    }
                    if (!WindowManagerService.this.mIsUpdateIpoRotation) {
                        return;
                    }
                    Slog.v(WindowManagerService.TAG, "Update IPO rotation is done");
                    while (!"1".equals(SystemProperties.get("service.bootanim.exit", "0"))) {
                        Slog.v(WindowManagerService.TAG, "service.bootanim.exit = " + SystemProperties.get("service.bootanim.exit", "0"));
                        SystemClock.sleep(100L);
                    }
                    SystemClock.sleep(100L);
                    WindowManagerService.this.mIsUpdateIpoRotation = false;
                    if (WindowManagerService.this.mIpoRotation != -1) {
                        WindowManagerService.this.freezeRotation(WindowManagerService.this.mIpoRotation);
                        WindowManagerService.this.mIpoRotation = -1;
                        return;
                    } else {
                        WindowManagerService.this.thawRotation();
                        return;
                    }
                }
                if ("android.intent.action.ACTION_PREBOOT_IPO".equals(intent.getAction())) {
                    WindowManagerService.this.closeSystemDialogs();
                    WindowManagerService.this.mH.sendMessage(WindowManagerService.this.mH.obtainMessage(54));
                    Slog.v(WindowManagerService.TAG, "UPDATE_IPO_ROTATION");
                    return;
                }
                if (WindowManagerService.ALARM_BOOT_DONE.equals(action)) {
                    WindowManagerService.this.mIsAlarmBooting = false;
                    if (WindowManagerService.this.mRotation != 0) {
                        WindowManagerService.this.mIsUpdateAlarmBootRotation = true;
                    }
                    Slog.v(WindowManagerService.TAG, "Alarm boot is done");
                    WindowManagerService.this.mBootAnimationStopped = false;
                    WindowManagerService.this.mH.sendMessage(WindowManagerService.this.mH.obtainMessage(16));
                    return;
                }
                if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                    WindowManagerService.this.mHasReceiveIPO = true;
                    Slog.v(WindowManagerService.TAG, "IPO_ENABLE, setEventDispatching false");
                    WindowManagerService.this.mInputMonitor.setEventDispatchingLw(false);
                    if (!WindowManagerService.this.isRotationFrozen()) {
                        return;
                    }
                    WindowManagerService.this.mIpoRotation = WindowManagerService.this.getRotation();
                    return;
                }
                if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    if (!WindowManagerService.this.mHasReceiveIPO) {
                        return;
                    }
                    WindowManagerService.this.mRotation = 0;
                    WindowManagerService.this.mPolicy.setRotationLw(WindowManagerService.this.mRotation);
                    Slog.v(WindowManagerService.TAG, "Re-initialize the rotation value to " + WindowManagerService.this.mRotation);
                    WindowManagerService.this.mHasReceiveIPO = false;
                    return;
                }
                if (!"android.intent.action.CONFIGURATION_CHANGED".equals(action)) {
                    return;
                }
                Slog.d(WindowManagerService.TAG, "Configuration changed, remove fast starting window catch");
                WindowManagerService.this.mBitmaps.evictAll();
            }
        };
        this.mCurrentProfileIds = new int[0];
        this.mPolicy = new PhoneWindowManager();
        this.mSessions = new ArraySet<>();
        this.mWindowMap = new HashMap<>();
        this.mTokenMap = new HashMap<>();
        this.mFinishedStarting = new ArrayList<>();
        this.mFinishedEarlyAnim = new ArrayList<>();
        this.mReplacingWindowTimeouts = new ArrayList<>();
        this.mResizingWindows = new ArrayList<>();
        this.mPendingRemove = new ArrayList<>();
        this.mPendingRemoveTmp = new WindowState[20];
        this.mDestroySurface = new ArrayList<>();
        this.mDestroyPreservedSurface = new ArrayList<>();
        this.mLosingFocus = new ArrayList<>();
        this.mForceRemoves = new ArrayList<>();
        this.mHidingNonSystemOverlayWindows = new ArrayList<>();
        this.mWaitingForDrawn = new ArrayList<>();
        this.mRebuildTmp = new WindowState[20];
        this.mScreenCaptureDisabled = new SparseArray<>();
        this.mTmpFloats = new float[9];
        this.mTmpRect = new Rect();
        this.mTmpRect2 = new Rect();
        this.mTmpRect3 = new Rect();
        this.mDisplayEnabled = false;
        this.mSystemBooted = false;
        this.mForceDisplayEnabled = false;
        this.mShowingBootMessages = false;
        this.mBootAnimationStopped = false;
        this.mLastWakeLockHoldingWindow = null;
        this.mLastWakeLockObscuringWindow = null;
        this.mDisplayContents = new SparseArray<>(2);
        this.mRotation = 0;
        this.mForcedAppOrientation = -1;
        this.mAltOrientation = false;
        this.mDockedStackCreateMode = 0;
        this.mTmpTaskIds = new SparseIntArray();
        this.mChangedStackList = new ArrayList<>();
        this.mForceResizableTasks = false;
        this.mRotationWatchers = new ArrayList<>();
        this.mSystemDecorLayer = 0;
        this.mScreenRect = new Rect();
        this.mDisplayFrozen = false;
        this.mDisplayFreezeTime = 0L;
        this.mLastDisplayFreezeDuration = 0;
        this.mLastFinishedFreezeSource = null;
        this.mWaitingForConfig = false;
        this.mWindowsFreezingScreen = 0;
        this.mClientFreezingScreen = false;
        this.mAppsFreezingScreen = 0;
        this.mLastWindowForcedOrientation = -1;
        this.mLastKeyguardForcedOrientation = -1;
        this.mLayoutSeq = 0;
        this.mLastStatusBarVisibility = 0;
        this.mLastDispatchedSystemUiVisibility = 0;
        this.mCurConfiguration = new Configuration();
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
        this.mTmpWindows = new ArrayList<>();
        this.mFocusedApp = null;
        this.mWindowAnimationScaleSetting = 1.0f;
        this.mTransitionAnimationScaleSetting = 1.0f;
        this.mAnimatorDurationScaleSetting = 1.0f;
        this.mAnimationsDisabled = false;
        this.mDragState = null;
        this.mTaskIdToTask = new SparseArray<>();
        this.mStackIdToStack = new SparseArray<>();
        this.mWindowChangeListeners = new ArrayList<>();
        this.mWindowsChanged = false;
        this.mTempConfiguration = new Configuration();
        this.mNoAnimationNotifyOnTransitionFinished = new ArrayList();
        this.mReconfigureOnConfigurationChanged = new DisplayContentList();
        this.mActivityManagerAppTransitionNotifier = new WindowManagerInternal.AppTransitionListener() {
            public void onAppTransitionCancelledLocked() {
                WindowManagerService.this.mH.sendEmptyMessage(48);
            }

            public void onAppTransitionFinishedLocked(IBinder token) {
                WindowManagerService.this.mH.sendEmptyMessage(49);
                AppWindowToken atoken = WindowManagerService.this.findAppWindowToken(token);
                if (atoken == null) {
                    return;
                }
                if (atoken.mLaunchTaskBehind) {
                    try {
                        WindowManagerService.this.mActivityManager.notifyLaunchTaskBehindComplete(atoken.token);
                    } catch (RemoteException e) {
                    }
                    atoken.mLaunchTaskBehind = false;
                    return;
                }
                atoken.updateReportedVisibilityLocked();
                if (!atoken.mEnteringAnimation) {
                    return;
                }
                atoken.mEnteringAnimation = false;
                try {
                    WindowManagerService.this.mActivityManager.notifyEnterAnimationComplete(atoken.token);
                } catch (RemoteException e2) {
                }
            }
        };
        this.mInputMonitor = new InputMonitor(this);
        this.mMousePositionTracker = new MousePositionTracker(null);
        this.mIsPerfBoostEnable = false;
        this.mPerfService = null;
        this.mHasReceiveIPO = false;
        this.mIsAlarmBooting = false;
        this.mIsUpdateAlarmBootRotation = false;
        this.mIsUpdateIpoRotation = false;
        this.mIpoRotation = -1;
        this.mCacheBehavior = 0;
        this.mFastStartingWindowSupport = 1 == SystemProperties.getInt("ro.mtk_perf_fast_start_win", 0);
        this.mDisableFastStartingWindow = 1 == SystemProperties.getInt("debug.disable_fast_start_win", 0);
        this.mBitmaps = new LruCache<>(6);
        this.mFreeformStackListeners = new RemoteCallbackList<>();
        this.mIsRestoreButtonVisible = false;
        this.mContext = context;
        this.mHaveInputMethods = haveInputMethods;
        this.mAllowBootMessages = showBootMsgs;
        this.mOnlyCore = onlyCore;
        this.mLimitedAlphaCompositing = context.getResources().getBoolean(R.^attr-private.activityChooserViewStyle);
        this.mHasPermanentDpad = context.getResources().getBoolean(R.^attr-private.lightY);
        this.mInTouchMode = context.getResources().getBoolean(R.^attr-private.navigationButtonStyle);
        this.mDrawLockTimeoutMillis = context.getResources().getInteger(R.integer.config_drawLockTimeoutMillis);
        this.mAllowAnimationsInLowPowerMode = context.getResources().getBoolean(R.^attr-private.notificationHeaderAppNameVisibility);
        this.mInputManager = inputManager;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplaySettings = new DisplaySettings();
        this.mDisplaySettings.readSettingsLocked();
        this.mWallpaperControllerLocked = new WallpaperController(this);
        this.mWindowPlacerLocked = new WindowSurfacePlacer(this);
        this.mLayersController = new WindowLayersController(this);
        LocalServices.addService(WindowManagerPolicy.class, this.mPolicy);
        this.mPointerEventDispatcher = new PointerEventDispatcher(this.mInputManager.monitorInput("WindowManager"));
        this.mFxSession = new SurfaceSession();
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mDisplays = this.mDisplayManager.getDisplays();
        for (Display display : this.mDisplays) {
            createDisplayContentLocked(display);
        }
        this.mKeyguardDisableHandler = new KeyguardDisableHandler(this.mContext, this.mPolicy);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
            public void onLowPowerModeChanged(boolean enabled) {
                synchronized (WindowManagerService.this.mWindowMap) {
                    if (WindowManagerService.this.mAnimationsDisabled != enabled && !WindowManagerService.this.mAllowAnimationsInLowPowerMode) {
                        WindowManagerService.this.mAnimationsDisabled = enabled;
                        WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                    }
                }
            }
        });
        this.mAnimationsDisabled = this.mPowerManagerInternal.getLowPowerModeEnabled();
        this.mScreenFrozenLock = this.mPowerManager.newWakeLock(1, "SCREEN_FROZEN");
        this.mScreenFrozenLock.setReferenceCounted(false);
        this.mAppTransition = new AppTransition(context, this);
        this.mAppTransition.registerListenerLocked(this.mActivityManagerAppTransitionNotifier);
        this.mBoundsAnimationController = new BoundsAnimationController(this.mAppTransition, UiThread.getHandler());
        this.mActivityManager = ActivityManagerNative.getDefault();
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
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
        filter.addAction("android.intent.action.ACTION_BOOT_IPO");
        filter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        filter.addAction(ALARM_BOOT_DONE);
        filter.addAction("android.intent.action.SCREEN_OFF");
        if (isFastStartingWindowSupport()) {
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        }
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mSettingsObserver = new SettingsObserver();
        this.mHoldingScreenWakeLock = this.mPowerManager.newWakeLock(536870922, "WindowManager");
        this.mHoldingScreenWakeLock.setReferenceCounted(false);
        this.mAnimator = new WindowAnimator(this);
        this.mAllowTheaterModeWakeFromLayout = context.getResources().getBoolean(R.^attr-private.colorSurfaceVariant);
        LocalServices.addService(WindowManagerInternal.class, new LocalService(this, null));
        initPolicy();
        Watchdog.getInstance().addMonitor(this);
        SurfaceControl.openTransaction();
        try {
            createWatermarkInTransaction();
            SurfaceControl.closeTransaction();
            showEmulatorDisplayOverlayIfNeeded();
            this.mIsAlarmBooting = isAlarmBoot();
            Log.v(TAG, "mIsAlarmBooting = " + this.mIsAlarmBooting);
            if (!this.mIsAlarmBooting) {
                return;
            }
            this.mInputMonitor.setEventDispatchingLw(true);
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
                Slog.wtf("WindowManager", "Window Manager Crash", e);
            }
            throw e;
        }
    }

    private void placeWindowAfter(WindowState pos, WindowState window) {
        WindowList windows = pos.getWindowList();
        int i = windows.indexOf(pos);
        if (WindowManagerDebugConfig.DEBUG_FOCUS || WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "Adding window " + window + " at " + (i + 1) + " of " + windows.size() + " (after " + pos + ")");
        }
        windows.add(i + 1, window);
        this.mWindowsChanged = true;
    }

    private void placeWindowBefore(WindowState pos, WindowState window) {
        WindowList windows = pos.getWindowList();
        int i = windows.indexOf(pos);
        if (WindowManagerDebugConfig.DEBUG_FOCUS || WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "Adding window " + window + " at " + i + " of " + windows.size() + " (before " + pos + ")");
        }
        if (i < 0) {
            Slog.w("WindowManager", "placeWindowBefore: Unable to find " + pos + " in " + windows);
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

    private WindowList getTokenWindowsOnDisplay(WindowToken token, DisplayContent displayContent) {
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
            if (w == targetWin) {
                return i;
            }
            if (!w.mChildWindows.isEmpty() && indexOfWinInWindowList(targetWin, w.mChildWindows) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private int addAppWindowToListLocked(WindowState win) {
        int NC;
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent == null) {
            return 0;
        }
        IWindow client = win.mClient;
        WindowToken token = win.mToken;
        WindowList windows = displayContent.getWindowList();
        WindowList tokenWindowList = getTokenWindowsOnDisplay(token, displayContent);
        if (!tokenWindowList.isEmpty()) {
            return addAppWindowToTokenListLocked(win, token, windows, tokenWindowList);
        }
        if (localLOGV) {
            Slog.v("WindowManager", "Figuring out where to add app window " + client.asBinder() + " (token=" + token + ")");
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
                        pos = tokenWindowList2.get(0);
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
            WindowToken atoken = this.mTokenMap.get(pos.mClient.asBinder());
            if (atoken != null) {
                WindowList tokenWindowList3 = getTokenWindowsOnDisplay(atoken, displayContent);
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
                    pos = tokenWindowList4.get(NW - 1);
                    break;
                }
            }
            if (tokenNdx >= 0) {
                break;
            }
            taskNdx--;
        }
        if (pos != null) {
            WindowToken atoken2 = this.mTokenMap.get(pos.mClient.asBinder());
            if (atoken2 != null && (NC = atoken2.windows.size()) > 0) {
                WindowState top = atoken2.windows.get(NC - 1);
                if (top.mSubLayer >= 0) {
                    pos = top;
                }
            }
            placeWindowAfter(pos, win);
            return 0;
        }
        int myLayer = win.mBaseLayer;
        int i = windows.size() - 1;
        while (i >= 0) {
            WindowState w = windows.get(i);
            if (w.mBaseLayer <= myLayer && w.mAttrs.type != 2034) {
                break;
            }
            i--;
        }
        if (WindowManagerDebugConfig.DEBUG_FOCUS || WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "Based on layer: Adding window " + win + " at " + (i + 1) + " of " + windows.size());
        }
        windows.add(i + 1, win);
        this.mWindowsChanged = true;
        return 0;
    }

    private int addAppWindowToTokenListLocked(WindowState win, WindowToken token, WindowList windows, WindowList tokenWindowList) {
        int tokenWindowsPos;
        if (win.mAttrs.type == 1) {
            WindowState lowestWindow = tokenWindowList.get(0);
            placeWindowBefore(lowestWindow, win);
            int tokenWindowsPos2 = indexOfWinInWindowList(lowestWindow, token.windows);
            return tokenWindowsPos2;
        }
        AppWindowToken atoken = win.mAppToken;
        int windowListPos = tokenWindowList.size();
        WindowState lastWindow = tokenWindowList.get(windowListPos - 1);
        if (atoken != null && lastWindow == atoken.startingWindow) {
            placeWindowBefore(lastWindow, win);
            int tokenWindowsPos3 = indexOfWinInWindowList(lastWindow, token.windows);
            return tokenWindowsPos3;
        }
        int newIdx = findIdxBasedOnAppTokens(win);
        if (WindowManagerDebugConfig.DEBUG_FOCUS || WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "not Base app: Adding window " + win + " at " + (newIdx + 1) + " of " + windows.size());
        }
        windows.add(newIdx + 1, win);
        if (newIdx < 0) {
            tokenWindowsPos = 0;
        } else {
            tokenWindowsPos = indexOfWinInWindowList(windows.get(newIdx), token.windows) + 1;
        }
        this.mWindowsChanged = true;
        return tokenWindowsPos;
    }

    private void addFreeWindowToListLocked(WindowState win) {
        WindowList windows = win.getWindowList();
        int myLayer = win.mBaseLayer;
        int i = windows.size() - 1;
        while (i >= 0) {
            WindowState otherWin = windows.get(i);
            if (otherWin.getBaseType() != 2013 && otherWin.mBaseLayer <= myLayer) {
                break;
            } else {
                i--;
            }
        }
        int i2 = i + 1;
        if (WindowManagerDebugConfig.DEBUG_FOCUS || WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "Free window: Adding window " + win + " at " + i2 + " of " + windows.size());
        }
        windows.add(i2, win);
        this.mWindowsChanged = true;
    }

    private void addAttachedWindowToListLocked(WindowState win, boolean addToToken) {
        WindowToken token = win.mToken;
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent == null) {
            return;
        }
        WindowState attached = win.mAttachedWindow;
        WindowList tokenWindowList = getTokenWindowsOnDisplay(token, displayContent);
        int NA = tokenWindowList.size();
        int sublayer = win.mSubLayer;
        int largestSublayer = Integer.MIN_VALUE;
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
                        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                            Slog.v("WindowManager", "Adding " + win + " to " + token);
                        }
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
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        Slog.v("WindowManager", "Adding " + win + " to " + token);
                    }
                    token.windows.add(i, win);
                }
                placeWindowBefore(w, win);
            }
        }
        if (i < NA) {
            return;
        }
        if (addToToken) {
            if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v("WindowManager", "Adding " + win + " to " + token);
            }
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

    private void addWindowToListInOrderLocked(WindowState win, boolean addToToken) {
        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
            Slog.d("WindowManager", "addWindowToListInOrderLocked: win=" + win + " Callers=" + Debug.getCallers(4));
        }
        if (win.mAttachedWindow == null) {
            WindowToken token = win.mToken;
            int tokenWindowsPos = 0;
            if (token.appWindowToken != null) {
                tokenWindowsPos = addAppWindowToListLocked(win);
            } else {
                addFreeWindowToListLocked(win);
            }
            if (addToToken) {
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.v("WindowManager", "Adding " + win + " to " + token);
                }
                token.windows.add(tokenWindowsPos, win);
            }
        } else {
            addAttachedWindowToListLocked(win, addToToken);
        }
        AppWindowToken appToken = win.mAppToken;
        if (appToken == null || !addToToken) {
            return;
        }
        appToken.addWindow(win);
    }

    static boolean canBeImeTarget(WindowState w) {
        int fl = w.mAttrs.flags & 131080;
        int type = w.mAttrs.type;
        if (fl != 0 && fl != 131080 && type != 3) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.i("WindowManager", "isVisibleOrAdding " + w + ": " + w.isVisibleOrAdding());
            if (!w.isVisibleOrAdding()) {
                Slog.i("WindowManager", "  mSurfaceController=" + w.mWinAnimator.mSurfaceController + " relayoutCalled=" + w.mRelayoutCalled + " viewVis=" + w.mViewVisibility + " policyVis=" + w.mPolicyVisibility + " policyVisAfterAnim=" + w.mPolicyVisibilityAfterAnim + " attachHid=" + w.mAttachedHidden + " exiting=" + w.mAnimatingExit + " destroying=" + w.mDestroying);
                if (w.mAppToken != null) {
                    Slog.i("WindowManager", "  mAppToken.hiddenRequested=" + w.mAppToken.hiddenRequested);
                }
            }
        }
        return w.isVisibleOrAdding();
    }

    int findDesiredInputMethodWindowIndexLocked(boolean willMove) {
        int dividerIndex;
        WindowList windows = getDefaultWindowListLocked();
        WindowState w = null;
        WindowState stickyWin = null;
        int i = windows.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            WindowState win = (WindowState) windows.get(i);
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD && willMove) {
                Slog.i("WindowManager", "Checking window @" + i + " " + win + " fl=0x" + Integer.toHexString(win.mAttrs.flags));
            }
            if (MultiWindowManager.isSupported() && stickyWin == null && isStickyByMtk(win)) {
                stickyWin = win;
                if (this.mFocusedApp != win.mAppToken) {
                    Slog.v(TAG, "[BMW]Sticky " + win + " is not a focus window. Therefore, it can't be ime target");
                }
                i--;
            }
            if (canBeImeTarget(win)) {
                w = win;
                if (!willMove && win.mAttrs.type == 3 && i > 0) {
                    WindowState wb = (WindowState) windows.get(i - 1);
                    if (wb.mAppToken == win.mAppToken && canBeImeTarget(wb)) {
                        i--;
                        w = wb;
                    }
                }
            } else {
                i--;
            }
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD && willMove) {
            Slog.v("WindowManager", "Proposed new IME target: " + w);
        }
        WindowState curTarget = this.mInputMethodTarget;
        if (curTarget != null && curTarget.isDisplayedLw() && curTarget.isClosing() && (w == null || curTarget.mWinAnimator.mAnimLayer > w.mWinAnimator.mAnimLayer)) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.v("WindowManager", "Current target higher, not changing");
            }
            return windows.indexOf(curTarget) + 1;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.v("WindowManager", "Desired input method target=" + w + " willMove=" + willMove);
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
                    if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                        Slog.v("WindowManager", this.mAppTransition + " " + highestTarget + " animating=" + highestTarget.mWinAnimator.isAnimationSet() + " layer=" + highestTarget.mWinAnimator.mAnimLayer + " new layer=" + w.mWinAnimator.mAnimLayer);
                    }
                    if (this.mAppTransition.isTransitionSet()) {
                        this.mInputMethodTargetWaitingAnim = true;
                        this.mInputMethodTarget = highestTarget;
                        return highestPos + 1;
                    }
                    if (highestTarget.mWinAnimator.isAnimationSet() && highestTarget.mWinAnimator.mAnimLayer > w.mWinAnimator.mAnimLayer) {
                        this.mInputMethodTargetWaitingAnim = true;
                        this.mInputMethodTarget = highestTarget;
                        return highestPos + 1;
                    }
                }
            }
        }
        if (w == null) {
            if (!willMove) {
                return -1;
            }
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.w("WindowManager", "Moving IM target from " + curTarget + " to null." + (WindowManagerDebugConfig.SHOW_STACK_CRAWLS ? " Callers=" + Debug.getCallers(4) : ""));
            }
            this.mInputMethodTarget = null;
            this.mLayersController.setInputMethodAnimLayerAdjustment(0);
            return -1;
        }
        if (willMove) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.w("WindowManager", "Moving IM target from " + curTarget + " to " + w + (WindowManagerDebugConfig.SHOW_STACK_CRAWLS ? " Callers=" + Debug.getCallers(4) : ""));
            }
            this.mInputMethodTarget = w;
            this.mInputMethodTargetWaitingAnim = false;
            if (w.mAppToken != null) {
                this.mLayersController.setInputMethodAnimLayerAdjustment(w.mAppToken.mAppAnimator.animLayerAdjustment);
            } else {
                this.mLayersController.setInputMethodAnimLayerAdjustment(0);
            }
        }
        WindowState dockedDivider = w.mDisplayContent.mDividerControllerLocked.getWindow();
        if (dockedDivider != null && dockedDivider.isVisibleLw() && (dividerIndex = windows.indexOf(dockedDivider)) > 0 && dividerIndex > i) {
            return dividerIndex + 1;
        }
        if (!MultiWindowManager.isSupported() || stickyWin == null || stickyWin == w) {
            return i + 1;
        }
        Slog.v(TAG, "[BMW]Because of sticky window, move ime over the stick window");
        return windows.indexOf(stickyWin) + 1;
    }

    void addInputMethodWindowToListLocked(WindowState win) {
        int pos = findDesiredInputMethodWindowIndexLocked(true);
        if (pos >= 0) {
            win.mTargetAppToken = this.mInputMethodTarget.mAppToken;
            if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v("WindowManager", "Adding input method window " + win + " at " + pos);
            }
            getDefaultWindowListLocked().add(pos, win);
            this.mWindowsChanged = true;
            moveInputMethodDialogsLocked(pos + 1);
            return;
        }
        win.mTargetAppToken = null;
        addWindowToListInOrderLocked(win, true);
        moveInputMethodDialogsLocked(pos);
    }

    private int tmpRemoveWindowLocked(int interestingPos, WindowState win) {
        WindowList windows = win.getWindowList();
        int wpos = windows.indexOf(win);
        if (wpos >= 0) {
            if (wpos < interestingPos) {
                interestingPos--;
            }
            if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                Slog.v("WindowManager", "Temp removing at " + wpos + ": " + win);
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
                    if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                        Slog.v("WindowManager", "Temp removing child at " + cpos + ": " + cw);
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
        if (wpos < 0) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
            Slog.v("WindowManager", "ReAdd removing from " + wpos + ": " + win);
        }
        windows.remove(wpos);
        this.mWindowsChanged = true;
        reAddWindowLocked(wpos, win);
    }

    void logWindowList(WindowList windows, String prefix) {
        int N = windows.size();
        while (N > 0) {
            N--;
            Slog.v("WindowManager", prefix + "#" + N + ": " + windows.get(N));
        }
    }

    void moveInputMethodDialogsLocked(int pos) {
        WindowState wp;
        ArrayList<WindowState> dialogs = this.mInputMethodDialogs;
        WindowList windows = getDefaultWindowListLocked();
        int N = dialogs.size();
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.v("WindowManager", "Removing " + N + " dialogs w/pos=" + pos);
        }
        for (int i = 0; i < N; i++) {
            pos = tmpRemoveWindowLocked(pos, dialogs.get(i));
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.v("WindowManager", "Window list w/pos=" + pos);
            logWindowList(windows, "  ");
        }
        if (pos >= 0) {
            AppWindowToken targetAppToken = this.mInputMethodTarget.mAppToken;
            if (this.mInputMethodWindow != null) {
                while (pos < windows.size() && ((wp = windows.get(pos)) == this.mInputMethodWindow || wp.mAttachedWindow == this.mInputMethodWindow)) {
                    pos++;
                }
            }
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.v("WindowManager", "Adding " + N + " dialogs at pos=" + pos);
            }
            for (int i2 = 0; i2 < N; i2++) {
                WindowState win = dialogs.get(i2);
                win.mTargetAppToken = targetAppToken;
                pos = reAddWindowLocked(pos, win);
            }
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.v("WindowManager", "Final window list:");
                logWindowList(windows, "  ");
                return;
            }
            return;
        }
        for (int i3 = 0; i3 < N; i3++) {
            WindowState win2 = dialogs.get(i3);
            win2.mTargetAppToken = null;
            reAddWindowToListInOrderLocked(win2);
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.v("WindowManager", "No IM target, final list:");
                logWindowList(windows, "  ");
            }
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
            WindowState windowState = imPos < N ? windows.get(imPos) : null;
            WindowState baseImWin = imWin != null ? imWin : this.mInputMethodDialogs.get(0);
            if (baseImWin.mChildWindows.size() > 0) {
                WindowState cw = baseImWin.mChildWindows.get(0);
                if (cw.mSubLayer < 0) {
                    baseImWin = cw;
                }
            }
            if (windowState == baseImWin) {
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
                        return false;
                    }
                    return false;
                }
            }
            if (imWin != null) {
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    Slog.v("WindowManager", "Moving IM from " + imPos);
                    logWindowList(windows, "  ");
                }
                int imPos2 = tmpRemoveWindowLocked(imPos, imWin);
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    Slog.v("WindowManager", "List after removing with new pos " + imPos2 + ":");
                    logWindowList(windows, "  ");
                }
                imWin.mTargetAppToken = this.mInputMethodTarget.mAppToken;
                reAddWindowLocked(imPos2, imWin);
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    Slog.v("WindowManager", "List after moving IM to " + imPos2 + ":");
                    logWindowList(windows, "  ");
                }
                if (DN > 0) {
                    moveInputMethodDialogsLocked(imPos2 + 1);
                }
            } else {
                moveInputMethodDialogsLocked(imPos);
            }
        } else if (imWin != null) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.v("WindowManager", "Moving IM from " + imPos);
            }
            tmpRemoveWindowLocked(0, imWin);
            imWin.mTargetAppToken = null;
            reAddWindowToListInOrderLocked(imWin);
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.v("WindowManager", "List with no IM target:");
                logWindowList(windows, "  ");
            }
            if (DN > 0) {
                moveInputMethodDialogsLocked(-1);
            }
        } else {
            moveInputMethodDialogsLocked(-1);
        }
        if (needAssignLayers) {
            this.mLayersController.assignLayersLocked(windows);
            return true;
        }
        return true;
    }

    private static boolean excludeWindowTypeFromTapOutTask(int windowType) {
        switch (windowType) {
            case 2000:
            case 2012:
            case 2019:
                return true;
            default:
                return false;
        }
    }

    public int addWindow(Session session, IWindow client, int seq, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, InputChannel outInputChannel) {
        Rect taskBounds;
        int startOpResult;
        int[] appOp = new int[1];
        int res = this.mPolicy.checkAddPermission(attrs, appOp);
        if (res != 0) {
            return res;
        }
        boolean reportNewConfig = false;
        WindowState attachedWindow = null;
        int type = attrs.type;
        synchronized (this.mWindowMap) {
            if (!this.mDisplayReady) {
                throw new IllegalStateException("Display has not been initialialized");
            }
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent == null) {
                Slog.w("WindowManager", "Attempted to add window to a display that does not exist: " + displayId + ".  Aborting.");
                return -9;
            }
            if (!displayContent.hasAccess(session.mUid)) {
                Slog.w("WindowManager", "Attempted to add window to a display for which the application does not have access: " + displayId + ".  Aborting.");
                return -9;
            }
            if (this.mWindowMap.containsKey(client.asBinder())) {
                Slog.w("WindowManager", "Window " + client + " is already added");
                return -5;
            }
            if (type >= 1000 && type <= 1999) {
                attachedWindow = windowForClientLocked((Session) null, attrs.token, false);
                if (attachedWindow == null) {
                    Slog.w("WindowManager", "Attempted to add window with token that is not a window: " + attrs.token + ".  Aborting.");
                    return -2;
                }
                if (attachedWindow.mAttrs.type >= 1000 && attachedWindow.mAttrs.type <= 1999) {
                    Slog.w("WindowManager", "Attempted to add window with token that is a sub-window: " + attrs.token + ".  Aborting.");
                    return -2;
                }
            }
            if (type == 2030 && !displayContent.isPrivate()) {
                Slog.w("WindowManager", "Attempted to add private presentation window to a non-private display.  Aborting.");
                return -8;
            }
            boolean addToken = false;
            WindowToken token = this.mTokenMap.get(attrs.token);
            AppWindowToken atoken = null;
            if (token == null) {
                if (type >= 1 && type <= 99) {
                    Slog.w("WindowManager", "Attempted to add application window with unknown token " + attrs.token + ".  Aborting.");
                    return -1;
                }
                if (type == 2011) {
                    Slog.w("WindowManager", "Attempted to add input method window with unknown token " + attrs.token + ".  Aborting.");
                    return -1;
                }
                if (type == 2031) {
                    Slog.w("WindowManager", "Attempted to add voice interaction window with unknown token " + attrs.token + ".  Aborting.");
                    return -1;
                }
                if (type == 2013) {
                    Slog.w("WindowManager", "Attempted to add wallpaper window with unknown token " + attrs.token + ".  Aborting.");
                    return -1;
                }
                if (type == 2023) {
                    Slog.w("WindowManager", "Attempted to add Dream window with unknown token " + attrs.token + ".  Aborting.");
                    return -1;
                }
                if (type == 2035) {
                    Slog.w("WindowManager", "Attempted to add QS dialog window with unknown token " + attrs.token + ".  Aborting.");
                    return -1;
                }
                if (type == 2032) {
                    Slog.w("WindowManager", "Attempted to add Accessibility overlay window with unknown token " + attrs.token + ".  Aborting.");
                    return -1;
                }
                token = new WindowToken(this, attrs.token, -1, false);
                addToken = true;
            } else if (type >= 1 && type <= 99) {
                atoken = token.appWindowToken;
                if (atoken == null) {
                    Slog.w("WindowManager", "Attempted to add window with non-application token " + token + ".  Aborting.");
                    return -3;
                }
                if (atoken.removed) {
                    Slog.w("WindowManager", "Attempted to add window with exiting application token " + token + ".  Aborting.");
                    return -4;
                }
                if (type == 3 && atoken.firstWindowDrawn) {
                    if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW || localLOGV) {
                        Slog.v("WindowManager", "**** NO NEED TO START: " + attrs.getTitle());
                    }
                    return -6;
                }
            } else if (type == 2011) {
                if (token.windowType != 2011) {
                    Slog.w("WindowManager", "Attempted to add input method window with bad token " + attrs.token + ".  Aborting.");
                    return -1;
                }
            } else if (type == 2031) {
                if (token.windowType != 2031) {
                    Slog.w("WindowManager", "Attempted to add voice interaction window with bad token " + attrs.token + ".  Aborting.");
                    return -1;
                }
            } else if (type == 2013) {
                if (token.windowType != 2013) {
                    Slog.w("WindowManager", "Attempted to add wallpaper window with bad token " + attrs.token + ".  Aborting.");
                    return -1;
                }
            } else if (type == 2023) {
                if (token.windowType != 2023) {
                    Slog.w("WindowManager", "Attempted to add Dream window with bad token " + attrs.token + ".  Aborting.");
                    return -1;
                }
            } else if (type == 2032) {
                if (token.windowType != 2032) {
                    Slog.w("WindowManager", "Attempted to add Accessibility overlay window with bad token " + attrs.token + ".  Aborting.");
                    return -1;
                }
            } else if (type == 2035) {
                if (token.windowType != 2035) {
                    Slog.w("WindowManager", "Attempted to add QS dialog window with bad token " + attrs.token + ".  Aborting.");
                    return -1;
                }
            } else if (token.appWindowToken != null) {
                Slog.w("WindowManager", "Non-null appWindowToken for system window of type=" + type);
                attrs.token = null;
                token = new WindowToken(this, null, -1, false);
                addToken = true;
            }
            WindowState win = new WindowState(this, session, client, token, attachedWindow, appOp[0], seq, attrs, viewVisibility, displayContent);
            if (win.mDeathRecipient == null) {
                Slog.w("WindowManager", "Adding window client " + client.asBinder() + " that is dead, aborting.");
                return -4;
            }
            if (win.getDisplayContent() == null) {
                Slog.w("WindowManager", "Adding window to Display that has been removed.");
                return -9;
            }
            this.mPolicy.adjustWindowParamsLw(win.mAttrs);
            win.setShowToOwnerOnlyLocked(this.mPolicy.checkShowToOwnerOnly(attrs));
            int res2 = this.mPolicy.prepareAddWindowLw(win, attrs);
            if (res2 != 0) {
                return res2;
            }
            boolean openInputChannels = outInputChannel != null && (attrs.inputFeatures & 2) == 0;
            if (openInputChannels) {
                try {
                    win.openInputChannel(outInputChannel);
                } catch (IllegalArgumentException e) {
                    Slog.w(TAG, "handle Input channel erorr", e);
                    return -11;
                }
            }
            int res3 = 0;
            if (excludeWindowTypeFromTapOutTask(type)) {
                displayContent.mTapExcludedWindows.add(win);
            }
            long origId = Binder.clearCallingIdentity();
            if (addToken) {
                this.mTokenMap.put(attrs.token, token);
            }
            win.attach();
            this.mWindowMap.put(client.asBinder(), win);
            if (win.mAppOp != -1 && (startOpResult = this.mAppOps.startOpNoThrow(win.mAppOp, win.getOwningUid(), win.getOwningPackage())) != 0 && startOpResult != 3) {
                win.setAppOpVisibilityLw(false);
            }
            boolean hideSystemAlertWindows = !this.mHidingNonSystemOverlayWindows.isEmpty();
            win.setForceHideNonSystemOverlayWindowIfNeeded(hideSystemAlertWindows);
            if (type == 3 && token.appWindowToken != null) {
                token.appWindowToken.startingWindow = win;
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v("WindowManager", "addWindow: " + token.appWindowToken + " startingWindow=" + win);
                }
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
                    this.mWallpaperControllerLocked.clearLastWallpaperTimeoutTime();
                    displayContent.pendingLayoutChanges |= 4;
                } else if ((attrs.flags & PackageManagerService.DumpState.DUMP_DEXOPT) != 0 || this.mWallpaperControllerLocked.isBelowWallpaperTarget(win)) {
                    displayContent.pendingLayoutChanges |= 4;
                }
            }
            win.applyScrollIfNeeded();
            win.applyAdjustForImeIfNeeded();
            if (type == 2034) {
                getDefaultDisplayContentLocked().getDockedDividerController().setWindow(win);
            }
            WindowStateAnimator winAnimator = win.mWinAnimator;
            winAnimator.mEnterAnimationPending = true;
            winAnimator.mEnteringAnimation = true;
            if (atoken != null && !prepareWindowReplacementTransition(atoken)) {
                prepareNoneTransitionForRelaunching(atoken);
            }
            if (displayContent.isDefaultDisplay) {
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                if (atoken == null || atoken.mTask == null) {
                    taskBounds = null;
                } else {
                    taskBounds = this.mTmpRect;
                    atoken.mTask.getBounds(this.mTmpRect);
                }
                if (this.mPolicy.getInsetHintLw(win.mAttrs, taskBounds, this.mRotation, displayInfo.logicalWidth, displayInfo.logicalHeight, outContentInsets, outStableInsets, outOutsets)) {
                    res3 = 4;
                }
            } else {
                outContentInsets.setEmpty();
                outStableInsets.setEmpty();
            }
            if (this.mInTouchMode) {
                res3 |= 1;
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
            this.mLayersController.assignLayersLocked(displayContent.getWindowList());
            if (focusChanged) {
                this.mInputMonitor.setInputFocusLw(this.mCurrentFocus, false);
            }
            this.mInputMonitor.updateInputWindowsLw(false);
            if (localLOGV || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v("WindowManager", "addWindow: New client " + client.asBinder() + ": window=" + win + " Callers=" + Debug.getCallers(5));
            }
            if (win.isVisibleOrAdding() && updateOrientationFromAppTokensLocked(false)) {
                reportNewConfig = true;
            }
            if (attrs.removeTimeoutMilliseconds > 0) {
                this.mH.sendMessageDelayed(this.mH.obtainMessage(52, win), attrs.removeTimeoutMilliseconds);
            }
            if (reportNewConfig) {
                sendNewConfiguration();
            }
            Binder.restoreCallingIdentity(origId);
            return res3;
        }
    }

    private boolean prepareWindowReplacementTransition(AppWindowToken atoken) {
        atoken.clearAllDrawn();
        WindowState replacedWindow = null;
        for (int i = atoken.windows.size() - 1; i >= 0 && replacedWindow == null; i--) {
            WindowState candidate = atoken.windows.get(i);
            if (candidate.mAnimatingExit && candidate.mWillReplaceWindow && candidate.mAnimateReplacingWindow) {
                replacedWindow = candidate;
            }
        }
        if (replacedWindow == null) {
            return false;
        }
        Rect frame = replacedWindow.mVisibleFrame;
        this.mOpeningApps.add(atoken);
        prepareAppTransition(18, true);
        this.mAppTransition.overridePendingAppTransitionClipReveal(frame.left, frame.top, frame.width(), frame.height());
        executeAppTransition();
        return true;
    }

    private void prepareNoneTransitionForRelaunching(AppWindowToken atoken) {
        if (!this.mDisplayFrozen || this.mOpeningApps.contains(atoken) || !atoken.isRelaunching()) {
            return;
        }
        this.mOpeningApps.add(atoken);
        prepareAppTransition(0, false);
        executeAppTransition();
    }

    boolean isScreenCaptureDisabledLocked(int userId) {
        Boolean disabled = this.mScreenCaptureDisabled.get(userId);
        if (disabled == null) {
            return false;
        }
        return disabled.booleanValue();
    }

    boolean isSecureLocked(WindowState w) {
        return (w.mAttrs.flags & PackageManagerService.DumpState.DUMP_PREFERRED_XML) != 0 || isScreenCaptureDisabledLocked(UserHandle.getUserId(w.mOwnerUid));
    }

    public void setScreenCaptureDisabled(int userId, boolean disabled) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException("Only system can call setScreenCaptureDisabled.");
        }
        synchronized (this.mWindowMap) {
            this.mScreenCaptureDisabled.put(userId, Boolean.valueOf(disabled));
            for (int displayNdx = this.mDisplayContents.size() - 1; displayNdx >= 0; displayNdx--) {
                WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                    WindowState win = windows.get(winNdx);
                    if (win.mHasSurface && userId == UserHandle.getUserId(win.mOwnerUid)) {
                        win.mWinAnimator.setSecureLocked(disabled);
                    }
                }
            }
        }
    }

    private void setupWindowForRemoveOnExit(WindowState win) {
        win.mRemoveOnExit = true;
        win.setDisplayLayoutNeeded();
        boolean focusChanged = updateFocusedWindowLocked(3, false);
        this.mWindowPlacerLocked.performSurfacePlacement();
        if (!focusChanged) {
            return;
        }
        this.mInputMonitor.updateInputWindowsLw(false);
    }

    public void removeWindow(Session session, IWindow client) {
        synchronized (this.mWindowMap) {
            WindowState win = windowForClientLocked(session, client, false);
            if (win == null) {
                return;
            }
            removeWindowLocked(win);
        }
    }

    void removeWindowLocked(WindowState win) {
        removeWindowLocked(win, false);
    }

    void removeWindowLocked(WindowState win, boolean keepVisibleDeadWindow) {
        win.mWindowRemovalAllowed = true;
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v(TAG, "removeWindowLocked: " + win + " callers=" + Debug.getCallers(4));
        }
        boolean startingWindow = win.mAttrs.type == 3;
        if (startingWindow && WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            Slog.d("WindowManager", "Starting window removed " + win);
        }
        if (localLOGV || WindowManagerDebugConfig.DEBUG_FOCUS || (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT && win == this.mCurrentFocus)) {
            Slog.v("WindowManager", "Remove " + win + " client=" + Integer.toHexString(System.identityHashCode(win.mClient.asBinder())) + ", surfaceController=" + win.mWinAnimator.mSurfaceController + " Callers=" + Debug.getCallers(4));
        }
        long origId = Binder.clearCallingIdentity();
        win.disposeInputChannel();
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "Remove " + win + ": mSurfaceController=" + win.mWinAnimator.mSurfaceController + " mAnimatingExit=" + win.mAnimatingExit + " mRemoveOnExit=" + win.mRemoveOnExit + " mHasSurface=" + win.mHasSurface + " surfaceShowing=" + win.mWinAnimator.getShown() + " isAnimationSet=" + win.mWinAnimator.isAnimationSet() + " app-animation=" + (win.mAppToken != null ? win.mAppToken.mAppAnimator.animation : null) + " mWillReplaceWindow=" + win.mWillReplaceWindow + " inPendingTransaction=" + (win.mAppToken != null ? win.mAppToken.inPendingTransaction : false) + " mDisplayFrozen=" + this.mDisplayFrozen + " callers=" + Debug.getCallers(6));
        }
        boolean wasVisible = false;
        if (win.mHasSurface && okToDisplay()) {
            AppWindowToken appToken = win.mAppToken;
            if (win.mWillReplaceWindow) {
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.v("WindowManager", "Preserving " + win + " until the new one is added");
                }
                win.mAnimatingExit = true;
                win.mReplacingRemoveRequested = true;
                Binder.restoreCallingIdentity(origId);
                return;
            }
            if (win.isAnimatingWithSavedSurface() && !appToken.allDrawnExcludingSaved) {
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.d("WindowManager", "removeWindowLocked: delay removal of " + win + " due to early animation");
                }
                setupWindowForRemoveOnExit(win);
                Binder.restoreCallingIdentity(origId);
                return;
            }
            wasVisible = win.isWinVisibleLw();
            if (keepVisibleDeadWindow) {
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.v("WindowManager", "Not removing " + win + " because app died while it's visible");
                }
                win.mAppDied = true;
                win.setDisplayLayoutNeeded();
                this.mWindowPlacerLocked.performSurfacePlacement();
                win.openInputChannel(null);
                this.mInputMonitor.updateInputWindowsLw(true);
                Binder.restoreCallingIdentity(origId);
                return;
            }
            WindowStateAnimator winAnimator = win.mWinAnimator;
            if (wasVisible) {
                int transit = !startingWindow ? 2 : 5;
                if (winAnimator.applyAnimationLocked(transit, false)) {
                    win.mAnimatingExit = true;
                }
                if (this.mAccessibilityController != null && win.getDisplayId() == 0) {
                    this.mAccessibilityController.onWindowTransitionLocked(win, transit);
                }
            }
            boolean isAnimating = winAnimator.isAnimationSet() && !winAnimator.isDummyAnimation();
            boolean lastWindowIsStartingWindow = startingWindow && appToken != null && appToken.allAppWindows.size() == 1;
            if (winAnimator.getShown() && win.mAnimatingExit && (!lastWindowIsStartingWindow || isAnimating)) {
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.v("WindowManager", "Not removing " + win + " due to exit animation ");
                }
                setupWindowForRemoveOnExit(win);
                if (appToken != null) {
                    appToken.updateReportedVisibilityLocked();
                }
                Binder.restoreCallingIdentity(origId);
                return;
            }
        }
        removeWindowInnerLocked(win);
        if (wasVisible && updateOrientationFromAppTokensLocked(false)) {
            this.mH.sendEmptyMessage(18);
        }
        updateFocusedWindowLocked(0, true);
        Binder.restoreCallingIdentity(origId);
    }

    void removeWindowInnerLocked(WindowState win) {
        if (win.mRemoved) {
            if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v("WindowManager", "removeWindowInnerLocked: " + win + " Already removed...");
                return;
            }
            return;
        }
        while (win.mChildWindows.size() > 0) {
            int i = win.mChildWindows.size() - 1;
            WindowState cwin = win.mChildWindows.get(i);
            Slog.w("WindowManager", "Force-removing child win " + cwin + " from container " + win);
            removeWindowInnerLocked(cwin);
        }
        win.mRemoved = true;
        if (this.mInputMethodTarget == win) {
            moveInputMethodWindowsIfNeededLocked(false);
        }
        int type = win.mAttrs.type;
        if (excludeWindowTypeFromTapOutTask(type)) {
            DisplayContent displaycontent = win.getDisplayContent();
            displaycontent.mTapExcludedWindows.remove(win);
        }
        this.mPolicy.removeWindowLw(win);
        win.removeLocked();
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "removeWindowInnerLocked: " + win);
        }
        this.mWindowMap.remove(win.mClient.asBinder());
        if (win.mAppOp != -1) {
            this.mAppOps.finishOp(win.mAppOp, win.getOwningUid(), win.getOwningPackage());
        }
        this.mPendingRemove.remove(win);
        this.mResizingWindows.remove(win);
        updateNonSystemOverlayWindowsVisibilityIfNeeded(win, false);
        this.mWindowsChanged = true;
        if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
            Slog.v("WindowManager", "Final remove of window: " + win);
        }
        if (this.mInputMethodWindow == win) {
            this.mInputMethodWindow = null;
        } else if (win.mAttrs.type == 2012) {
            this.mInputMethodDialogs.remove(win);
        }
        WindowToken token = win.mToken;
        AppWindowToken atoken = win.mAppToken;
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "Removing " + win + " from " + token);
        }
        token.windows.remove(win);
        if (atoken != null) {
            atoken.allAppWindows.remove(win);
        }
        if (localLOGV) {
            Slog.v("WindowManager", "**** Removing window " + win + ": count=" + token.windows.size());
        }
        if (token.windows.size() == 0) {
            if (!token.explicit) {
                this.mTokenMap.remove(token.token);
            } else if (atoken != null) {
                atoken.firstWindowDrawn = false;
                atoken.clearAllDrawn();
            }
        }
        if (atoken != null) {
            if (atoken.startingWindow == win) {
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v("WindowManager", "Notify removed startingWindow " + win);
                }
                scheduleRemoveStartingWindowLocked(atoken);
            } else if (atoken.allAppWindows.size() == 0 && atoken.startingData != null) {
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v("WindowManager", "Nulling last startingWindow");
                }
                atoken.startingData = null;
            } else if (atoken.allAppWindows.size() == 1 && atoken.startingView != null) {
                scheduleRemoveStartingWindowLocked(atoken);
            }
        }
        if (type == 2013) {
            this.mWallpaperControllerLocked.clearLastWallpaperTimeoutTime();
            getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
        } else if ((win.mAttrs.flags & PackageManagerService.DumpState.DUMP_DEXOPT) != 0) {
            getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
        }
        WindowList windows = win.getWindowList();
        if (windows != null) {
            windows.remove(win);
            if (!this.mWindowPlacerLocked.isInLayout()) {
                this.mLayersController.assignLayersLocked(windows);
                win.setDisplayLayoutNeeded();
                this.mWindowPlacerLocked.performSurfacePlacement();
                if (win.mAppToken != null) {
                    win.mAppToken.updateReportedVisibilityLocked();
                }
            }
        }
        this.mInputMonitor.updateInputWindowsLw(true);
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
                        boolean z = mode == 0 || mode == 3;
                        win.setAppOpVisibilityLw(z);
                    }
                }
            }
        }
    }

    static void logSurface(WindowState w, String msg, boolean withStackTrace) {
        String str = "  SURFACE " + msg + ": " + w;
        if (withStackTrace) {
            logWithStack(TAG, str);
        } else {
            Slog.i("WindowManager", str);
        }
    }

    static void logSurface(SurfaceControl s, String title, String msg) {
        String str = "  SURFACE " + s + ": " + msg + " / " + title;
        Slog.i("WindowManager", str);
    }

    static void logWithStack(String tag, String s) {
        RuntimeException e = null;
        if (WindowManagerDebugConfig.SHOW_STACK_CRAWLS) {
            e = new RuntimeException();
            e.fillInStackTrace();
        }
        Slog.i(tag, s, e);
    }

    void setTransparentRegionWindow(Session session, IWindow client, Region region) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState w = windowForClientLocked(session, client, false);
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                    logSurface(w, "transparentRegionHint=" + region, false);
                }
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
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    Slog.d(TAG, "setInsetsWindow " + w + ", contentInsets=" + w.mGivenContentInsets + " -> " + contentInsets + ", visibleInsets=" + w.mGivenVisibleInsets + " -> " + visibleInsets + ", touchableRegion=" + w.mGivenTouchableRegion + " -> " + touchableRegion + ", touchableInsets " + w.mTouchableInsets + " -> " + touchableInsets);
                }
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
                    w.setDisplayLayoutNeeded();
                    this.mWindowPlacerLocked.performSurfacePlacement();
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

    public void pokeDrawLock(Session session, IBinder token) {
        synchronized (this.mWindowMap) {
            WindowState window = windowForClientLocked(session, token, false);
            if (window != null) {
                window.pokeDrawLockLw(this.mDrawLockTimeoutMillis);
            }
        }
    }

    void repositionChild(Session session, IWindow client, int left, int top, int right, int bottom, long frameNumber, Rect outFrame) {
        Trace.traceBegin(32L, "repositionChild");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState win = windowForClientLocked(session, client, false);
                if (win == null) {
                    return;
                }
                if (win.mAttachedWindow == null) {
                    throw new IllegalArgumentException("repositionChild called but window is notattached to a parent win=" + win);
                }
                win.mAttrs.x = left;
                win.mAttrs.y = top;
                win.mAttrs.width = right - left;
                win.mAttrs.height = bottom - top;
                win.setWindowScale(win.mRequestedWidth, win.mRequestedHeight);
                if (win.mHasSurface) {
                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                        Slog.i("WindowManager", ">>> OPEN TRANSACTION repositionChild");
                    }
                    SurfaceControl.openTransaction();
                    try {
                        win.applyGravityAndUpdateFrame(win.mContainingFrame, win.mDisplayFrame);
                        win.mWinAnimator.computeShownFrameLocked();
                        win.mWinAnimator.setSurfaceBoundariesLocked(false);
                        if (frameNumber > 0) {
                            win.mWinAnimator.deferTransactionUntilParentFrame(frameNumber);
                        }
                    } finally {
                        SurfaceControl.closeTransaction();
                        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                            Slog.i("WindowManager", "<<< CLOSE TRANSACTION repositionChild");
                        }
                    }
                }
                Rect rect = win.mCompatFrame;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
            Trace.traceEnd(32L);
        }
    }

    public int relayoutWindow(Session session, IWindow client, int seq, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, int flags, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Rect outBackdropFrame, Configuration outConfig, Surface outSurface) {
        int result = 0;
        boolean hasStatusBarPermission = this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0;
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mWindowMap) {
            WindowState win = windowForClientLocked(session, client, false);
            if (win == null) {
                return 0;
            }
            WindowStateAnimator winAnimator = win.mWinAnimator;
            if (viewVisibility != 8) {
                win.setRequestedSize(requestedWidth, requestedHeight);
            }
            int attrChanges = 0;
            int flagChanges = 0;
            if (attrs != null) {
                this.mPolicy.adjustWindowParamsLw(attrs);
                if (seq == win.mSeq) {
                    int systemUiVisibility = attrs.systemUiVisibility | attrs.subtreeSystemUiVisibility;
                    if ((67043328 & systemUiVisibility) != 0 && !hasStatusBarPermission) {
                        systemUiVisibility &= -67043329;
                    }
                    win.mSystemUiVisibility = systemUiVisibility;
                }
                if (win.mAttrs.type != attrs.type) {
                    Slog.e(TAG, "Window : " + win + "changes the window type!!");
                    Slog.e(TAG, "Original type : " + win.mAttrs.type);
                    Slog.e(TAG, "Changed type : " + attrs.type);
                    throw new IllegalArgumentException("Window type can not be changed after the window is added.");
                }
                if ((attrs.privateFlags & PackageManagerService.DumpState.DUMP_PREFERRED_XML) != 0) {
                    attrs.x = win.mAttrs.x;
                    attrs.y = win.mAttrs.y;
                    attrs.width = win.mAttrs.width;
                    attrs.height = win.mAttrs.height;
                }
                WindowManager.LayoutParams layoutParams = win.mAttrs;
                flagChanges = layoutParams.flags ^ attrs.flags;
                layoutParams.flags = flagChanges;
                attrChanges = win.mAttrs.copyFrom(attrs);
                if ((attrChanges & 16385) != 0) {
                    win.mLayoutNeeded = true;
                }
                if ((524288 & flagChanges) != 0) {
                    updateNonSystemOverlayWindowsVisibilityIfNeeded(win, win.mWinAnimator.getShown());
                }
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v("WindowManager", "Relayout " + win + ": viewVisibility=" + viewVisibility + " req=" + requestedWidth + "x" + requestedHeight + " " + win.mAttrs);
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v(TAG, "Input attr :" + attrs);
            }
            winAnimator.mSurfaceDestroyDeferred = (flags & 2) != 0;
            win.mEnforceSizeCompat = (win.mAttrs.privateFlags & 128) != 0;
            if ((attrChanges & 128) != 0) {
                winAnimator.mAlpha = attrs.alpha;
            }
            win.setWindowScale(win.mRequestedWidth, win.mRequestedHeight);
            if (win.mAttrs.surfaceInsets.left != 0 || win.mAttrs.surfaceInsets.top != 0 || win.mAttrs.surfaceInsets.right != 0 || win.mAttrs.surfaceInsets.bottom != 0) {
                winAnimator.setOpaqueLocked(false);
            }
            boolean imMayMove = (131080 & flagChanges) != 0;
            boolean isDefaultDisplay = win.isDefaultDisplay();
            boolean focusMayChange = isDefaultDisplay ? (win.mViewVisibility == viewVisibility && (flagChanges & 8) == 0 && win.mRelayoutCalled) ? false : true : false;
            boolean wallpaperMayMove = (win.mViewVisibility == viewVisibility || (win.mAttrs.flags & PackageManagerService.DumpState.DUMP_DEXOPT) == 0) ? false : true;
            boolean wallpaperMayMove2 = wallpaperMayMove | ((1048576 & flagChanges) != 0);
            if ((flagChanges & PackageManagerService.DumpState.DUMP_PREFERRED_XML) != 0 && winAnimator.mSurfaceController != null) {
                winAnimator.mSurfaceController.setSecure(isSecureLocked(win));
            }
            win.mRelayoutCalled = true;
            win.mInRelayout = true;
            int oldVisibility = win.mViewVisibility;
            win.mViewVisibility = viewVisibility;
            if (viewVisibility == 0 && oldVisibility != 0 && !IS_USER_BUILD) {
                Slog.i(TAG, "Relayout " + win + ": oldVis=" + oldVisibility + " newVis=" + viewVisibility);
            }
            if (viewVisibility != 0 || (win.mAppToken != null && win.mAppToken.clientHidden)) {
                winAnimator.mEnterAnimationPending = false;
                winAnimator.mEnteringAnimation = false;
                boolean usingSavedSurfaceBeforeVisible = oldVisibility != 0 ? win.isAnimatingWithSavedSurface() : false;
                if ((WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) && winAnimator.hasSurface() && !win.mAnimatingExit && usingSavedSurfaceBeforeVisible) {
                    Slog.d(TAG, "Ignoring layout to invisible when using saved surface " + win);
                }
                if (winAnimator.hasSurface() && !win.mAnimatingExit && !usingSavedSurfaceBeforeVisible) {
                    if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                        Slog.i("WindowManager", "Relayout invis " + win + ": mAnimatingExit=" + win.mAnimatingExit);
                    }
                    if (!win.mWillReplaceWindow) {
                        focusMayChange = tryStartExitingAnimation(win, winAnimator, isDefaultDisplay, focusMayChange);
                    }
                    result = 4;
                }
                outSurface.release();
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.i("WindowManager", "Releasing surface in: " + win);
                }
            } else {
                try {
                    result = createSurfaceControl(outSurface, relayoutVisibleWindow(outConfig, 0, win, winAnimator, attrChanges, oldVisibility), win, winAnimator);
                    if ((result & 2) != 0) {
                        focusMayChange = isDefaultDisplay;
                    }
                    if (win.mAttrs.type == 2011 && this.mInputMethodWindow == null) {
                        this.mInputMethodWindow = win;
                        imMayMove = true;
                    }
                    win.adjustStartingWindowFlags();
                } catch (Exception e) {
                    this.mInputMonitor.updateInputWindowsLw(true);
                    Slog.w("WindowManager", "Exception thrown when creating surface for client " + client + " (" + win.mAttrs.getTitle() + ")", e);
                    Binder.restoreCallingIdentity(origId);
                    return 0;
                }
            }
            if (focusMayChange && updateFocusedWindowLocked(3, false)) {
                imMayMove = false;
            }
            boolean toBeDisplayed = (result & 2) != 0;
            if (imMayMove && (moveInputMethodWindowsIfNeededLocked(false) || toBeDisplayed)) {
                this.mLayersController.assignLayersLocked(win.getWindowList());
            }
            if (wallpaperMayMove2) {
                getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
            }
            win.setDisplayLayoutNeeded();
            win.mGivenInsetsPending = (flags & 1) != 0;
            boolean configChanged = updateOrientationFromAppTokensLocked(false);
            this.mWindowPlacerLocked.performSurfacePlacement();
            if (toBeDisplayed && win.mIsWallpaper) {
                DisplayInfo displayInfo = getDefaultDisplayInfoLocked();
                this.mWallpaperControllerLocked.updateWallpaperOffset(win, displayInfo.logicalWidth, displayInfo.logicalHeight, false);
            }
            if (win.mAppToken != null) {
                win.mAppToken.updateReportedVisibilityLocked();
            }
            if (winAnimator.mReportSurfaceResized) {
                winAnimator.mReportSurfaceResized = false;
                result |= 32;
            }
            if (this.mPolicy.isNavBarForcedShownLw(win)) {
                result |= 64;
            }
            if (!win.isGoneForLayoutLw()) {
                win.mResizedWhileGone = false;
            }
            outFrame.set(win.mCompatFrame);
            outOverscanInsets.set(win.mOverscanInsets);
            outContentInsets.set(win.mContentInsets);
            outVisibleInsets.set(win.mVisibleInsets);
            outStableInsets.set(win.mStableInsets);
            outOutsets.set(win.mOutsets);
            outBackdropFrame.set(win.getBackdropFrame(win.mFrame));
            if (localLOGV) {
                Slog.v("WindowManager", "Relayout given client " + client.asBinder() + ", requestedWidth=" + requestedWidth + ", requestedHeight=" + requestedHeight + ", viewVisibility=" + viewVisibility + "\nRelayout returning frame=" + outFrame + ", surface=" + outSurface);
            }
            if (localLOGV || WindowManagerDebugConfig.DEBUG_FOCUS) {
                Slog.v("WindowManager", "Relayout of " + win + ": focusMayChange=" + focusMayChange);
            }
            int result2 = result | (this.mInTouchMode ? 1 : 0);
            this.mInputMonitor.updateInputWindowsLw(true);
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v("WindowManager", "Relayout complete " + win + ": outFrame=" + outFrame.toShortString());
            }
            win.mInRelayout = false;
            if (configChanged) {
                sendNewConfiguration();
            }
            Binder.restoreCallingIdentity(origId);
            return result2;
        }
    }

    private boolean tryStartExitingAnimation(WindowState win, WindowStateAnimator winAnimator, boolean isDefaultDisplay, boolean focusMayChange) {
        int transit = 2;
        if (win.mAttrs.type == 3) {
            transit = 5;
        }
        if (win.isWinVisibleLw() && winAnimator.applyAnimationLocked(transit, false)) {
            focusMayChange = isDefaultDisplay;
            win.mAnimatingExit = true;
            win.mWinAnimator.mAnimating = true;
        } else if (win.mWinAnimator.isAnimationSet() || this.mWallpaperControllerLocked.isWallpaperTarget(win)) {
            win.mAnimatingExit = true;
            win.mWinAnimator.mAnimating = true;
        } else {
            if (this.mInputMethodWindow == win) {
                this.mInputMethodWindow = null;
            }
            win.destroyOrSaveSurface();
        }
        if (this.mAccessibilityController != null && win.getDisplayId() == 0) {
            this.mAccessibilityController.onWindowTransitionLocked(win, transit);
        }
        return focusMayChange;
    }

    private int createSurfaceControl(Surface outSurface, int result, WindowState win, WindowStateAnimator winAnimator) {
        if (!win.mHasSurface) {
            result |= 4;
        }
        WindowSurfaceController surfaceController = winAnimator.createSurfaceLocked();
        if (surfaceController != null) {
            surfaceController.getSurface(outSurface);
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                Slog.i("WindowManager", "  OUT SURFACE " + outSurface + ": copied");
            }
        } else {
            outSurface.release();
        }
        return result;
    }

    private int relayoutVisibleWindow(Configuration outConfig, int result, WindowState win, WindowStateAnimator winAnimator, int attrChanges, int oldVisibility) {
        int result2 = result | (!win.isVisibleLw() ? 2 : 0);
        if (win.mAnimatingExit) {
            Slog.d(TAG, "relayoutVisibleWindow: " + win + " mAnimatingExit=true, mRemoveOnExit=" + win.mRemoveOnExit + ", mDestroying=" + win.mDestroying);
            winAnimator.cancelExitAnimationForNextAnimationLocked();
            win.mAnimatingExit = false;
        }
        if (win.mDestroying) {
            win.mDestroying = false;
            this.mDestroySurface.remove(win);
        }
        if (oldVisibility == 8) {
            winAnimator.mEnterAnimationPending = true;
        }
        winAnimator.mEnteringAnimation = true;
        if ((result2 & 2) != 0) {
            win.prepareWindowToDisplayDuringRelayout(outConfig);
        }
        if ((attrChanges & 8) != 0 && !winAnimator.tryChangeFormatInPlaceLocked()) {
            winAnimator.preserveSurfaceLocked();
            result2 |= 6;
        }
        if (win.isDragResizeChanged() || win.isResizedWhileNotDragResizing()) {
            win.setDragResizing();
            win.setResizedWhileNotDragResizing(false);
            if (win.mHasSurface && win.mAttachedWindow == null) {
                winAnimator.preserveSurfaceLocked();
                result2 |= 2;
            }
        }
        boolean freeformResizing = win.isDragResizing() && win.getResizeMode() == 0;
        boolean dockedResizing = win.isDragResizing() && win.getResizeMode() == 1;
        int result3 = result2 | (freeformResizing ? 16 : 0) | (dockedResizing ? 8 : 0);
        return win.isAnimatingWithSavedSurface() ? result3 | 2 : result3;
    }

    public void performDeferredDestroyWindow(Session session, IWindow client) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState win = windowForClientLocked(session, client, false);
                if (win == null || win.mWillReplaceWindow) {
                    return;
                }
                win.mWinAnimator.destroyDeferredSurfaceLocked();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean outOfMemoryWindow(Session session, IWindow client) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState win = windowForClientLocked(session, client, false);
                if (win == null) {
                    return false;
                }
                return reclaimSomeSurfaceMemoryLocked(win.mWinAnimator, "from-client", false);
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void finishDrawingWindow(Session session, IWindow client) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                WindowState win = windowForClientLocked(session, client, false);
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.d("WindowManager", "finishDrawingWindow: " + win + " mDrawState=" + (win != null ? win.mWinAnimator.drawStateToString() : "null"));
                }
                if (win != null && win.mWinAnimator.finishDrawingLocked()) {
                    if ((win.mAttrs.flags & PackageManagerService.DumpState.DUMP_DEXOPT) != 0) {
                        getDefaultDisplayContentLocked().pendingLayoutChanges |= 4;
                    }
                    win.setDisplayLayoutNeeded();
                    this.mWindowPlacerLocked.requestTraversal();
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
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v("WindowManager", "applyAnimation: atoken=" + atoken);
            }
            WindowState win = atoken.findMainWindow();
            Rect frame = new Rect(0, 0, width, height);
            Rect displayFrame = new Rect(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
            Rect insets = new Rect();
            Rect surfaceInsets = null;
            boolean zInFreeformWorkspace = win != null ? win.inFreeformWorkspace() : false;
            if (win != null) {
                if (zInFreeformWorkspace) {
                    frame.set(win.mFrame);
                } else {
                    frame.set(win.mContainingFrame);
                }
                surfaceInsets = win.getAttrs().surfaceInsets;
                insets.set(win.mContentInsets);
            }
            if (atoken.mLaunchTaskBehind) {
                enter = false;
            }
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.d("WindowManager", "Loading animation for app transition. transit=" + AppTransition.appTransitionToString(transit) + " enter=" + enter + " frame=" + frame + " insets=" + insets + " surfaceInsets=" + surfaceInsets);
            }
            Animation a = this.mAppTransition.loadAnimation(lp, transit, enter, this.mCurConfiguration.uiMode, this.mCurConfiguration.orientation, frame, displayFrame, insets, surfaceInsets, isVoiceInteraction, zInFreeformWorkspace, atoken.mTask.mTaskId);
            if (a != null) {
                if (WindowManagerDebugConfig.DEBUG_ANIM) {
                    logWithStack(TAG, "Loaded animation " + a + " for " + atoken);
                }
                int containingWidth = frame.width();
                int containingHeight = frame.height();
                atoken.mAppAnimator.setAnimation(a, containingWidth, containingHeight, this.mAppTransition.canSkipFirstFrame(), this.mAppTransition.getAppStackClipMode());
            }
        } else {
            atoken.mAppAnimator.clearAnimation();
        }
        if ("1".equals(SystemProperties.get("ro.globalpq.support"))) {
            WindowState window = null;
            WindowList defaultWindows = getDefaultDisplayContentLocked().getWindowList();
            int j = defaultWindows.size() - 1;
            while (true) {
                if (j < 0) {
                    break;
                }
                WindowState windowState = defaultWindows.get(j);
                if (windowState.mAppToken != null) {
                    window = windowState;
                    break;
                }
                j--;
            }
            if (!enter && window != null) {
                String mForegroundAP = window.mAttrs.packageName.toString();
                IBinder b = ServiceManager.getService("appdetection");
                if (b != null) {
                    IAppDetectionService mAppDetectionService = IAppDetectionService.Stub.asInterface(b);
                    try {
                        mAppDetectionService.updatePQparameterFromPackage(mForegroundAP);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return atoken.mAppAnimator.animation != null;
    }

    public void validateAppTokens(int stackId, List<TaskGroup> tasks) {
        synchronized (this.mWindowMap) {
            int t = tasks.size() - 1;
            if (t < 0) {
                Slog.w("WindowManager", "validateAppTokens: empty task list");
                return;
            }
            TaskGroup task = tasks.get(0);
            int taskId = task.taskId;
            Task targetTask = this.mTaskIdToTask.get(taskId);
            DisplayContent displayContent = targetTask.getDisplayContent();
            if (displayContent == null) {
                Slog.w("WindowManager", "validateAppTokens: no Display for taskId=" + taskId);
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
                    Slog.w("WindowManager", "validateAppTokens: displayContent changed in TaskGroup list!");
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
                Slog.w("WindowManager", "validateAppTokens: Mismatch! ActivityManager=" + tasks);
                Slog.w("WindowManager", "validateAppTokens: Mismatch! WindowManager=" + localTasks);
                Slog.w("WindowManager", "validateAppTokens: Mismatch! Callers=" + Debug.getCallers(4));
            }
        }
    }

    public void validateStackOrder(Integer[] remoteStackIds) {
    }

    private boolean checkCallingPermission(String permission, String func) {
        if (Binder.getCallingPid() == Process.myPid() || this.mContext.checkCallingPermission(permission) == 0) {
            return true;
        }
        String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + permission;
        Slog.w("WindowManager", msg);
        return false;
    }

    boolean okToDisplay() {
        if (this.mDisplayFrozen || !this.mDisplayEnabled) {
            return false;
        }
        return this.mPolicy.isScreenOn();
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
                Slog.w("WindowManager", "Attempted to add existing input method token: " + token);
                return;
            }
            WindowToken wtoken = new WindowToken(this, token, type, true);
            this.mTokenMap.put(token, wtoken);
            if (type == 2013) {
                this.mWallpaperControllerLocked.addWallpaperToken(wtoken);
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
                        if (win.mWinAnimator.isAnimationSet()) {
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
                        this.mWindowPlacerLocked.performSurfacePlacement();
                        updateFocusedWindowLocked(0, false);
                    }
                    if (delayed && displayContent != null) {
                        displayContent.mExitingTokens.add(wtoken);
                    } else if (wtoken.windowType == 2013) {
                        this.mWallpaperControllerLocked.removeWallpaperToken(wtoken);
                    }
                } else if (wtoken.windowType == 2013) {
                    this.mWallpaperControllerLocked.removeWallpaperToken(wtoken);
                }
                this.mInputMonitor.updateInputWindowsLw(true);
            } else {
                Slog.w("WindowManager", "Attempted to remove non-existing token: " + token);
            }
        }
        Binder.restoreCallingIdentity(origId);
    }

    private Task createTaskLocked(int taskId, int stackId, int userId, AppWindowToken atoken, Rect bounds, Configuration config) {
        if (WindowManagerDebugConfig.DEBUG_STACK) {
            Slog.i("WindowManager", "createTaskLocked: taskId=" + taskId + " stackId=" + stackId + " atoken=" + atoken + " bounds=" + bounds);
        }
        TaskStack stack = this.mStackIdToStack.get(stackId);
        if (stack == null) {
            throw new IllegalArgumentException("addAppToken: invalid stackId=" + stackId);
        }
        EventLog.writeEvent(EventLogTags.WM_TASK_CREATED, Integer.valueOf(taskId), Integer.valueOf(stackId));
        Task task = new Task(taskId, stack, userId, this, bounds, config);
        this.mTaskIdToTask.put(taskId, task);
        stack.addTask(task, !atoken.mLaunchTaskBehind, atoken.showForAllUsers);
        return task;
    }

    public void addAppToken(int addPos, IApplicationToken token, int taskId, int stackId, int requestedOrientation, boolean fullscreen, boolean showForAllUsers, int userId, int configChanges, boolean voiceInteraction, boolean launchTaskBehind, Rect taskBounds, Configuration config, int taskResizeMode, boolean alwaysFocusable, boolean homeTask, int targetSdkVersion) {
        long inputDispatchingTimeoutNanos;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "addAppToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        try {
            inputDispatchingTimeoutNanos = token.getKeyDispatchingTimeout() * 1000000;
        } catch (RemoteException ex) {
            Slog.w("WindowManager", "Could not get dispatching timeout.", ex);
            inputDispatchingTimeoutNanos = DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS;
        }
        synchronized (this.mWindowMap) {
            if (findAppWindowToken(token.asBinder()) != null) {
                Slog.w("WindowManager", "Attempted to add existing app token: " + token);
                return;
            }
            AppWindowToken atoken = new AppWindowToken(this, token, voiceInteraction);
            atoken.inputDispatchingTimeoutNanos = inputDispatchingTimeoutNanos;
            atoken.appFullscreen = fullscreen;
            atoken.showForAllUsers = showForAllUsers;
            atoken.targetSdk = targetSdkVersion;
            atoken.requestedOrientation = requestedOrientation;
            atoken.layoutConfigChanges = (configChanges & 1152) != 0;
            atoken.mLaunchTaskBehind = launchTaskBehind;
            atoken.mAlwaysFocusable = alwaysFocusable;
            if (WindowManagerDebugConfig.DEBUG_TOKEN_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v("WindowManager", "addAppToken: " + atoken + " to stack=" + stackId + " task=" + taskId + " at " + addPos);
            }
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                task = createTaskLocked(taskId, stackId, userId, atoken, taskBounds, config);
            }
            task.addAppToken(addPos, atoken, taskResizeMode, homeTask);
            this.mTokenMap.put(token.asBinder(), atoken);
            atoken.hidden = true;
            atoken.hiddenRequested = true;
        }
    }

    public void setAppTask(IBinder token, int taskId, int stackId, Rect taskBounds, Configuration config, int taskResizeMode, boolean homeTask) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppTask()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken atoken = findAppWindowToken(token);
            if (atoken == null) {
                Slog.w("WindowManager", "Attempted to set task id of non-existing app token: " + token);
                return;
            }
            Task oldTask = atoken.mTask;
            oldTask.removeAppToken(atoken);
            Task newTask = this.mTaskIdToTask.get(taskId);
            if (newTask == null) {
                newTask = createTaskLocked(taskId, stackId, oldTask.mUserId, atoken, taskBounds, config);
            }
            newTask.addAppToken(Integer.MAX_VALUE, atoken, taskResizeMode, homeTask);
        }
    }

    public int getOrientationLocked() {
        int req;
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v(TAG, "Checking window orientation:  mDisplayFrozen = " + this.mDisplayFrozen + " mOpeningApps.size() = " + this.mOpeningApps.size() + " mClosingApps.size() = " + this.mClosingApps.size());
        }
        if (!this.mDisplayFrozen) {
            WindowList windows = getDefaultWindowListLocked();
            for (int pos = windows.size() - 1; pos >= 0; pos--) {
                WindowState win = windows.get(pos);
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v(TAG, "" + win);
                    Slog.v(TAG, "screenOrientation = " + win.mAttrs.screenOrientation + " app window token = " + win.mAppToken + " visibility = " + win.isVisibleLw() + " policy visibility after anim = " + win.mPolicyVisibilityAfterAnim + " policy visibility = " + win.mPolicyVisibility + " attach hidden = " + win.mAttachedHidden + " destroying = " + win.mDestroying);
                }
                if (win.mAppToken != null) {
                    break;
                }
                if (win.isVisibleLw() && win.mPolicyVisibilityAfterAnim && (req = win.mAttrs.screenOrientation) != -1 && req != 3) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v("WindowManager", win + " forcing orientation to " + req);
                    }
                    if (this.mPolicy.isKeyguardHostWindow(win.mAttrs)) {
                        this.mLastKeyguardForcedOrientation = req;
                    }
                    this.mLastWindowForcedOrientation = req;
                    return req;
                }
            }
            this.mLastWindowForcedOrientation = -1;
            if (this.mPolicy.isKeyguardLocked()) {
                WindowState winShowWhenLocked = (WindowState) this.mPolicy.getWinShowWhenLockedLw();
                AppWindowToken appShowWhenLocked = winShowWhenLocked != null ? winShowWhenLocked.mAppToken : null;
                if (appShowWhenLocked == null) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v("WindowManager", "No one is requesting an orientation when the screen is locked");
                    }
                    return this.mLastKeyguardForcedOrientation;
                }
                int req2 = appShowWhenLocked.requestedOrientation;
                if (req2 == 3) {
                    req2 = this.mLastKeyguardForcedOrientation;
                }
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Done at " + appShowWhenLocked + " -- show when locked, return " + req2);
                }
                return req2;
            }
        } else if (this.mLastWindowForcedOrientation != -1) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Display is frozen, return " + this.mLastWindowForcedOrientation);
            }
            return this.mLastWindowForcedOrientation;
        }
        return getAppSpecifiedOrientation();
    }

    private int getAppSpecifiedOrientation() {
        boolean zIsStackVisibleLocked;
        int lastOrientation = -1;
        boolean findingBehind = false;
        boolean lastFullscreen = false;
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        ArrayList<Task> tasks = displayContent.getTasks();
        if (isStackVisibleLocked(3)) {
            zIsStackVisibleLocked = true;
        } else {
            zIsStackVisibleLocked = isStackVisibleLocked(2);
        }
        boolean dockMinimized = getDefaultDisplayContentLocked().mDividerControllerLocked.isMinimizedDock();
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
            int firstToken = tokens.size() - 1;
            for (int tokenNdx = firstToken; tokenNdx >= 0; tokenNdx--) {
                AppWindowToken atoken = tokens.get(tokenNdx);
                if (WindowManagerDebugConfig.DEBUG_APP_ORIENTATION) {
                    Slog.v("WindowManager", "Checking app orientation: " + atoken);
                }
                if (!findingBehind && !atoken.hidden && atoken.hiddenRequested) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v("WindowManager", "Skipping " + atoken + " -- going to hide");
                    }
                } else {
                    if (tokenNdx == firstToken && lastOrientation != 3 && lastFullscreen) {
                        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                            Slog.v("WindowManager", "Done at " + atoken + " -- end of group, return " + lastOrientation);
                        }
                        return lastOrientation;
                    }
                    if (atoken.hiddenRequested) {
                        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                            Slog.v("WindowManager", "Skipping " + atoken + " -- hidden on top");
                        }
                    } else if (!zIsStackVisibleLocked || (atoken.mTask.isHomeTask() && dockMinimized)) {
                        if (tokenNdx == 0) {
                            lastOrientation = atoken.requestedOrientation;
                        }
                        int or = atoken.requestedOrientation;
                        lastFullscreen = atoken.appFullscreen;
                        if (lastFullscreen && or != 3) {
                            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                                Slog.v("WindowManager", "Done at " + atoken + " -- full screen, return " + or);
                            }
                            return or;
                        }
                        if (or != -1 && or != 3) {
                            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                                Slog.v("WindowManager", "Done at " + atoken + " -- explicitly set, return " + or);
                            }
                            return or;
                        }
                        findingBehind |= or == 3;
                    }
                }
            }
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "No app is requesting an orientation, return " + this.mForcedAppOrientation);
        }
        if (zIsStackVisibleLocked) {
            return -1;
        }
        return this.mForcedAppOrientation;
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
        if (!this.mDisplayReady) {
            return null;
        }
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
        this.mTempConfiguration.updateFrom(currentConfig);
        computeScreenConfigurationLocked(this.mTempConfiguration);
        if (currentConfig.diff(this.mTempConfiguration) == 0) {
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
            int req = getOrientationLocked();
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

    public int[] setNewConfiguration(Configuration config) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setNewConfiguration()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (this.mWaitingForConfig) {
                this.mWaitingForConfig = false;
                this.mLastFinishedFreezeSource = "new-config";
            }
            boolean configChanged = this.mCurConfiguration.diff(config) != 0;
            if (!configChanged) {
                return null;
            }
            prepareFreezingAllTaskBounds();
            this.mCurConfiguration = new Configuration(config);
            return onConfigurationChanged();
        }
    }

    public Rect getBoundsForNewConfiguration(int stackId) {
        Rect outBounds;
        synchronized (this.mWindowMap) {
            TaskStack stack = this.mStackIdToStack.get(stackId);
            outBounds = new Rect();
            stack.getBoundsForNewConfiguration(outBounds);
        }
        return outBounds;
    }

    private void prepareFreezingAllTaskBounds() {
        for (int i = this.mDisplayContents.size() - 1; i >= 0; i--) {
            ArrayList<TaskStack> stacks = this.mDisplayContents.valueAt(i).getStacks();
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                TaskStack stack = stacks.get(stackNdx);
                stack.prepareFreezingTaskBounds();
            }
        }
    }

    private int[] onConfigurationChanged() {
        this.mPolicy.onConfigurationChanged();
        DisplayContent defaultDisplayContent = getDefaultDisplayContentLocked();
        if (!this.mReconfigureOnConfigurationChanged.contains(defaultDisplayContent)) {
            this.mReconfigureOnConfigurationChanged.add(defaultDisplayContent);
        }
        for (int i = this.mReconfigureOnConfigurationChanged.size() - 1; i >= 0; i--) {
            reconfigureDisplayLocked(this.mReconfigureOnConfigurationChanged.remove(i));
        }
        defaultDisplayContent.getDockedDividerController().onConfigurationChanged();
        this.mChangedStackList.clear();
        for (int stackNdx = this.mStackIdToStack.size() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = this.mStackIdToStack.valueAt(stackNdx);
            if (stack.onConfigurationChanged()) {
                this.mChangedStackList.add(Integer.valueOf(stack.mStackId));
            }
        }
        if (this.mChangedStackList.isEmpty()) {
            return null;
        }
        return ArrayUtils.convertToIntArray(this.mChangedStackList);
    }

    public void setAppOrientation(IApplicationToken token, int requestedOrientation) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppOrientation()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken atoken = findAppWindowToken(token.asBinder());
            if (atoken == null) {
                Slog.w("WindowManager", "Attempted to set orientation of non-existing app token: " + token);
                return;
            }
            atoken.requestedOrientation = requestedOrientation;
            if (!IS_USER_BUILD) {
                Slog.d(TAG, "setAppOrientation to " + requestedOrientation + ", app:" + atoken);
            }
        }
    }

    public int getAppOrientation(IApplicationToken token) {
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = findAppWindowToken(token.asBinder());
            if (wtoken == null) {
                return -1;
            }
            return wtoken.requestedOrientation;
        }
    }

    void setFocusTaskRegionLocked() {
        Task task;
        DisplayContent displayContent;
        if (this.mFocusedApp == null || (displayContent = (task = this.mFocusedApp.mTask).getDisplayContent()) == null) {
            return;
        }
        displayContent.setTouchExcludeRegion(task);
    }

    public void setFocusedApp(IBinder token, boolean moveFocusNow) {
        AppWindowToken newFocus;
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setFocusedApp()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (token == null) {
                if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                    Slog.v("WindowManager", "Clearing focused app, was " + this.mFocusedApp);
                }
                newFocus = null;
            } else {
                newFocus = findAppWindowToken(token);
                if (newFocus == null) {
                    Slog.w("WindowManager", "Attempted to set focus to non-existing app token: " + token);
                }
                if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                    Slog.v("WindowManager", "Set focused app to: " + newFocus + " old focus=" + this.mFocusedApp + " moveFocusNow=" + moveFocusNow);
                }
            }
            boolean changed = this.mFocusedApp != newFocus;
            if (changed) {
                this.mFocusedApp = newFocus;
                this.mInputMonitor.setFocusedAppLw(newFocus);
                setFocusTaskRegionLocked();
            } else if (newFocus != null) {
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
            boolean prepared = this.mAppTransition.prepareAppTransitionLocked(transit, alwaysKeepCurrent);
            if (prepared && okToDisplay()) {
                this.mSkipAppTransitionAnimation = false;
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

    public void overridePendingAppTransitionClipReveal(int startX, int startY, int startWidth, int startHeight) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overridePendingAppTransitionClipReveal(startX, startY, startWidth, startHeight);
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

    public void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] specs, IRemoteCallback onAnimationStartedCallback, IRemoteCallback onAnimationFinishedCallback, boolean scaleUp) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overridePendingAppTransitionMultiThumb(specs, onAnimationStartedCallback, onAnimationFinishedCallback, scaleUp);
            prolongAnimationsFromSpecs(specs, scaleUp);
        }
    }

    void prolongAnimationsFromSpecs(AppTransitionAnimationSpec[] specs, boolean scaleUp) {
        AppWindowToken appToken;
        this.mTmpTaskIds.clear();
        for (int i = specs.length - 1; i >= 0; i--) {
            this.mTmpTaskIds.put(specs[i].taskId, 0);
        }
        for (WindowState win : this.mWindowMap.values()) {
            Task task = win.getTask();
            if (task != null && this.mTmpTaskIds.get(task.mTaskId, -1) != -1 && task.inFreeformWorkspace() && (appToken = win.mAppToken) != null && appToken.mAppAnimator != null) {
                appToken.mAppAnimator.startProlongAnimation(scaleUp ? 2 : 1);
            }
        }
    }

    public void overridePendingAppTransitionInPlace(String packageName, int anim) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overrideInPlaceAppTransition(packageName, anim);
        }
    }

    public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture specsFuture, IRemoteCallback callback, boolean scaleUp) {
        synchronized (this.mWindowMap) {
            this.mAppTransition.overridePendingAppTransitionMultiThumbFuture(specsFuture, callback, scaleUp);
        }
    }

    public void endProlongedAnimations() {
        synchronized (this.mWindowMap) {
            for (WindowState win : this.mWindowMap.values()) {
                AppWindowToken appToken = win.mAppToken;
                if (appToken != null && appToken.mAppAnimator != null) {
                    appToken.mAppAnimator.endProlongedAnimation();
                }
            }
            this.mAppTransition.notifyProlongedAnimationsEnded();
        }
    }

    public void executeAppTransition() {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "executeAppTransition()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.w("WindowManager", "Execute app transition: " + this.mAppTransition + " Callers=" + Debug.getCallers(5));
            }
            if (this.mAppTransition.isTransitionSet()) {
                this.mAppTransition.setReady();
                long origId = Binder.clearCallingIdentity();
                try {
                    this.mWindowPlacerLocked.performSurfacePlacement();
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }
    }

    public boolean setAppStartingWindow(IBinder token, String pkg, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, IBinder transferFrom, boolean createIfNeeded) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppStartingWindow()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "setAppStartingWindow: token=" + token + " pkg=" + pkg + " transferFrom=" + transferFrom);
            }
            AppWindowToken wtoken = findAppWindowToken(token);
            if (wtoken == null) {
                Slog.w("WindowManager", "Attempted to set icon of non-existing app token: " + token);
                return false;
            }
            if (!okToDisplay()) {
                return false;
            }
            if (wtoken.startingData != null) {
                return false;
            }
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Checking theme of starting window: 0x" + Integer.toHexString(theme));
            }
            if (theme != 0) {
                AttributeCache.Entry ent = AttributeCache.instance().get(pkg, theme, com.android.internal.R.styleable.Window, this.mCurrentUserId);
                if (ent == null) {
                    return false;
                }
                boolean windowIsTranslucent = ent.array.getBoolean(5, false);
                boolean windowIsFloating = ent.array.getBoolean(4, false);
                boolean windowShowWallpaper = ent.array.getBoolean(14, false);
                boolean windowDisableStarting = ent.array.getBoolean(12, false);
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v("WindowManager", "Translucent=" + windowIsTranslucent + " Floating=" + windowIsFloating + " ShowWallpaper=" + windowShowWallpaper);
                }
                if (windowIsTranslucent) {
                    return false;
                }
                if (windowIsFloating || windowDisableStarting) {
                    return false;
                }
                if (windowShowWallpaper) {
                    if (this.mWallpaperControllerLocked.getWallpaperTarget() == null) {
                        windowFlags |= PackageManagerService.DumpState.DUMP_DEXOPT;
                    } else {
                        return false;
                    }
                }
            }
            if (transferStartingWindow(transferFrom, wtoken)) {
                return true;
            }
            if (!createIfNeeded) {
                return false;
            }
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Creating StartingData");
            }
            wtoken.startingData = new StartingData(pkg, theme, compatInfo, nonLocalizedLabel, labelRes, icon, logo, windowFlags);
            Message m = this.mH.obtainMessage(5, wtoken);
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Enqueueing ADD_STARTING");
            }
            this.mH.sendMessageAtFrontOfQueue(m);
            return true;
        }
    }

    private boolean transferStartingWindow(IBinder transferFrom, AppWindowToken wtoken) {
        AppWindowToken ttoken;
        if (transferFrom == null || (ttoken = findAppWindowToken(transferFrom)) == null) {
            return false;
        }
        WindowState startingWindow = ttoken.startingWindow;
        if (startingWindow != null && ttoken.startingView != null) {
            this.mSkipAppTransitionAnimation = true;
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Moving existing starting " + startingWindow + " from " + ttoken + " to " + wtoken);
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
            if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE || WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Removing starting window: " + startingWindow);
            }
            startingWindow.getWindowList().remove(startingWindow);
            this.mWindowsChanged = true;
            if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v("WindowManager", "Removing starting " + startingWindow + " from " + ttoken);
            }
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
            }
            if (wtoken.clientHidden != ttoken.clientHidden) {
                wtoken.clientHidden = ttoken.clientHidden;
                wtoken.sendAppVisibilityToClients();
            }
            ttoken.mAppAnimator.transferCurrentAnimation(wtoken.mAppAnimator, startingWindow.mWinAnimator);
            updateFocusedWindowLocked(3, true);
            getDefaultDisplayContentLocked().layoutNeeded = true;
            this.mWindowPlacerLocked.performSurfacePlacement();
            Binder.restoreCallingIdentity(origId);
            return true;
        }
        if (ttoken.startingData != null) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Moving pending starting from " + ttoken + " to " + wtoken);
            }
            wtoken.startingData = ttoken.startingData;
            ttoken.startingData = null;
            ttoken.startingMoved = true;
            Message m = this.mH.obtainMessage(5, wtoken);
            this.mH.sendMessageAtFrontOfQueue(m);
            return true;
        }
        AppWindowAnimator tAppAnimator = ttoken.mAppAnimator;
        AppWindowAnimator wAppAnimator = wtoken.mAppAnimator;
        if (tAppAnimator.thumbnail != null) {
            if (wAppAnimator.thumbnail != null) {
                wAppAnimator.thumbnail.destroy();
            }
            wAppAnimator.thumbnail = tAppAnimator.thumbnail;
            wAppAnimator.thumbnailLayer = tAppAnimator.thumbnailLayer;
            wAppAnimator.thumbnailAnimation = tAppAnimator.thumbnailAnimation;
            tAppAnimator.thumbnail = null;
        }
        return false;
    }

    public void removeAppStartingWindow(IBinder token) {
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = this.mTokenMap.get(token).appWindowToken;
            scheduleRemoveStartingWindowLocked(wtoken);
        }
    }

    public void setAppFullscreen(IBinder token, boolean toOpaque) {
        synchronized (this.mWindowMap) {
            AppWindowToken atoken = findAppWindowToken(token);
            if (atoken != null) {
                atoken.appFullscreen = toOpaque;
                setWindowOpaqueLocked(token, toOpaque);
                this.mWindowPlacerLocked.requestTraversal();
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
        if (wtoken == null || (win = wtoken.findMainWindow()) == null) {
            return;
        }
        win.mWinAnimator.setOpaqueLocked(isOpaque);
    }

    boolean setTokenVisibilityLocked(AppWindowToken wtoken, WindowManager.LayoutParams lp, boolean visible, int transit, boolean performLayout, boolean isVoiceInteraction) {
        boolean delayed = false;
        if (wtoken.clientHidden == visible) {
            wtoken.clientHidden = !visible;
            wtoken.sendAppVisibilityToClients();
        }
        boolean visibilityChanged = false;
        if (wtoken.hidden == visible || ((wtoken.hidden && wtoken.mIsExiting) || (visible && wtoken.waitingForReplacement()))) {
            boolean changed = false;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "Changing app " + wtoken + " hidden=" + wtoken.hidden + " performLayout=" + performLayout);
            }
            boolean runningAppAnimation = false;
            if (transit != -1) {
                if (wtoken.mAppAnimator.animation == AppWindowAnimator.sDummyAnimation) {
                    wtoken.mAppAnimator.setNullAnimation();
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
            int windowsCount = wtoken.allAppWindows.size();
            for (int i = 0; i < windowsCount; i++) {
                WindowState win = wtoken.allAppWindows.get(i);
                if (win == wtoken.startingWindow) {
                    if (!visible && win.isVisibleNow() && wtoken.mAppAnimator.isAnimating()) {
                        win.mAnimatingExit = true;
                        win.mRemoveOnExit = true;
                        win.mWindowRemovalAllowed = true;
                    }
                } else if (visible) {
                    if (!win.isVisibleNow()) {
                        if (!runningAppAnimation) {
                            win.mWinAnimator.applyAnimationLocked(1, true);
                            if (this.mAccessibilityController != null && win.getDisplayId() == 0) {
                                this.mAccessibilityController.onWindowTransitionLocked(win, 1);
                            }
                        }
                        changed = true;
                        win.setDisplayLayoutNeeded();
                    }
                } else if (win.isVisibleNow()) {
                    if (!runningAppAnimation) {
                        win.mWinAnimator.applyAnimationLocked(2, false);
                        if (this.mAccessibilityController != null && win.getDisplayId() == 0) {
                            this.mAccessibilityController.onWindowTransitionLocked(win, 2);
                        }
                    }
                    changed = true;
                    win.setDisplayLayoutNeeded();
                }
            }
            boolean z = !visible;
            wtoken.hiddenRequested = z;
            wtoken.hidden = z;
            visibilityChanged = true;
            if (visible) {
                WindowState swin = wtoken.startingWindow;
                if (swin != null && !swin.isDrawnLw()) {
                    swin.mPolicyVisibility = false;
                    swin.mPolicyVisibilityAfterAnim = false;
                }
            } else {
                unsetAppFreezingScreenLocked(wtoken, true, true);
            }
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "setTokenVisibilityLocked: " + wtoken + ": hidden=" + wtoken.hidden + " hiddenRequested=" + wtoken.hiddenRequested);
            }
            if (changed) {
                this.mInputMonitor.setUpdateInputWindowsNeededLw();
                if (performLayout) {
                    updateFocusedWindowLocked(3, false);
                    this.mWindowPlacerLocked.performSurfacePlacement();
                }
                this.mInputMonitor.updateInputWindowsLw(false);
            }
        }
        if (wtoken.mAppAnimator.animation != null) {
            delayed = true;
        }
        for (int i2 = wtoken.allAppWindows.size() - 1; i2 >= 0 && !delayed; i2--) {
            if (wtoken.allAppWindows.get(i2).mWinAnimator.isWindowAnimationSet()) {
                delayed = true;
            }
        }
        if (visibilityChanged) {
            if (visible && !delayed) {
                wtoken.mEnteringAnimation = true;
                this.mActivityManagerAppTransitionNotifier.onAppTransitionFinishedLocked(wtoken.token);
            }
            if (!this.mClosingApps.contains(wtoken) && !this.mOpeningApps.contains(wtoken)) {
                getDefaultDisplayContentLocked().getDockedDividerController().notifyAppVisibilityChanged();
            }
        }
        return delayed;
    }

    void updateTokenInPlaceLocked(AppWindowToken wtoken, int transit) {
        if (transit == -1) {
            return;
        }
        if (wtoken.mAppAnimator.animation == AppWindowAnimator.sDummyAnimation) {
            wtoken.mAppAnimator.setNullAnimation();
        }
        applyAnimationLocked(wtoken, null, transit, false, false);
    }

    public void notifyAppStopped(IBinder token, boolean stopped) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "notifyAppStopped()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = findAppWindowToken(token);
            if (wtoken == null) {
                Slog.w("WindowManager", "Attempted to set visibility of non-existing app token: " + token);
            } else {
                wtoken.notifyAppStopped(stopped);
            }
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
                Slog.w("WindowManager", "Attempted to set visibility of non-existing app token: " + token);
                return;
            }
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ORIENTATION || !IS_USER_BUILD) {
                Slog.v("WindowManager", "setAppVisibility(" + token + ", visible=" + visible + "): " + this.mAppTransition + " hidden=" + wtoken.hidden + " hiddenRequested=" + wtoken.hiddenRequested + " Callers=" + Debug.getCallers(6));
            }
            this.mOpeningApps.remove(wtoken);
            this.mClosingApps.remove(wtoken);
            wtoken.waitingToShow = false;
            wtoken.hiddenRequested = visible ? false : true;
            if (!visible) {
                wtoken.removeAllDeadWindows();
                wtoken.setVisibleBeforeClientHidden();
            } else if (visible) {
                if (!this.mAppTransition.isTransitionSet() && this.mAppTransition.isReady()) {
                    this.mOpeningApps.add(wtoken);
                }
                wtoken.startingMoved = false;
                if (wtoken.hidden || wtoken.mAppStopped) {
                    wtoken.clearAllDrawn();
                    if (wtoken.hidden) {
                        wtoken.waitingToShow = true;
                    }
                    if (wtoken.clientHidden) {
                        wtoken.clientHidden = false;
                        wtoken.sendAppVisibilityToClients();
                    }
                }
                wtoken.requestUpdateWallpaperIfNeeded();
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.v("WindowManager", "No longer Stopped: " + wtoken);
                }
                wtoken.mAppStopped = false;
            }
            if (!okToDisplay() || !this.mAppTransition.isTransitionSet()) {
                long origId = Binder.clearCallingIdentity();
                wtoken.inPendingTransaction = false;
                setTokenVisibilityLocked(wtoken, null, visible, -1, true, wtoken.voiceInteraction);
                wtoken.updateReportedVisibilityLocked();
                Binder.restoreCallingIdentity(origId);
                return;
            }
            if (wtoken.mAppAnimator.usingTransferredAnimation && wtoken.mAppAnimator.animation == null) {
                Slog.wtf("WindowManager", "Will NOT set dummy animation on: " + wtoken + ", using null transfered animation!");
            }
            if (!wtoken.mAppAnimator.usingTransferredAnimation && (!wtoken.startingDisplayed || this.mSkipAppTransitionAnimation)) {
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.v("WindowManager", "Setting dummy animation on: " + wtoken);
                }
                wtoken.mAppAnimator.setDummyAnimation();
            }
            wtoken.inPendingTransaction = true;
            if (visible) {
                this.mOpeningApps.add(wtoken);
                wtoken.mEnteringAnimation = true;
            } else {
                this.mClosingApps.add(wtoken);
                wtoken.mEnteringAnimation = false;
            }
            if (this.mAppTransition.getAppTransition() == 16 && (win = findFocusedWindowLocked(getDefaultDisplayContentLocked())) != null && (focusedToken = win.mAppToken) != null) {
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.d("WindowManager", "TRANSIT_TASK_OPEN_BEHIND,  adding " + focusedToken + " to mOpeningApps");
                }
                focusedToken.hidden = true;
                this.mOpeningApps.add(focusedToken);
            }
        }
    }

    void unsetAppFreezingScreenLocked(AppWindowToken wtoken, boolean unfreezeSurfaceNow, boolean force) {
        if (!wtoken.mAppAnimator.freezingScreen) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "Clear freezing of " + wtoken + " force=" + force);
        }
        int N = wtoken.allAppWindows.size();
        boolean unfrozeWindows = false;
        for (int i = 0; i < N; i++) {
            WindowState w = wtoken.allAppWindows.get(i);
            if (w.mAppFreezing) {
                w.mAppFreezing = false;
                if (w.mHasSurface && !w.mOrientationChanging && this.mWindowsFreezingScreen != 2) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v("WindowManager", "set mOrientationChanging of " + w);
                    }
                    w.mOrientationChanging = true;
                    this.mWindowPlacerLocked.mOrientationChangeComplete = false;
                }
                w.mLastFreezeDuration = 0;
                unfrozeWindows = true;
                w.setDisplayLayoutNeeded();
            }
        }
        if (force || unfrozeWindows) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "No longer freezing: " + wtoken);
            }
            wtoken.mAppAnimator.freezingScreen = false;
            wtoken.mAppAnimator.lastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
            this.mAppsFreezingScreen--;
            this.mLastFinishedFreezeSource = wtoken;
        }
        if (!unfreezeSurfaceNow) {
            return;
        }
        if (unfrozeWindows) {
            this.mWindowPlacerLocked.performSurfacePlacement();
        }
        stopFreezingDisplayLocked();
    }

    private void startAppFreezingScreenLocked(AppWindowToken wtoken) {
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            logWithStack(TAG, "Set freezing of " + wtoken.appToken + ": hidden=" + wtoken.hidden + " freezing=" + wtoken.mAppAnimator.freezingScreen);
        }
        if (wtoken.hiddenRequested) {
            return;
        }
        if (!wtoken.mAppAnimator.freezingScreen) {
            wtoken.mAppAnimator.freezingScreen = true;
            wtoken.mAppAnimator.lastFreezeDuration = 0;
            this.mAppsFreezingScreen++;
            if (this.mAppsFreezingScreen == 1) {
                startFreezingDisplayLocked(false, 0, 0);
                this.mH.removeMessages(17);
                this.mH.sendEmptyMessageDelayed(17, 2000L);
            }
        }
        int N = wtoken.allAppWindows.size();
        for (int i = 0; i < N; i++) {
            WindowState w = wtoken.allAppWindows.get(i);
            w.mAppFreezing = true;
        }
    }

    public void startAppFreezingScreen(IBinder token, int configChanges) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppFreezingScreen()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            if (configChanges == 0) {
                if (okToDisplay()) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v("WindowManager", "Skipping set freeze of " + token);
                    }
                    return;
                }
            }
            AppWindowToken wtoken = findAppWindowToken(token);
            if (wtoken == null || wtoken.appToken == null) {
                Slog.w("WindowManager", "Attempted to freeze screen with non-existing app token: " + wtoken);
                return;
            }
            long origId = Binder.clearCallingIdentity();
            startAppFreezingScreenLocked(wtoken);
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void stopAppFreezingScreen(IBinder token, boolean force) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setAppFreezingScreen()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mWindowMap) {
            AppWindowToken wtoken = findAppWindowToken(token);
            if (wtoken == null || wtoken.appToken == null) {
                return;
            }
            long origId = Binder.clearCallingIdentity();
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Clear freezing of " + token + ": hidden=" + wtoken.hidden + " freezing=" + wtoken.mAppAnimator.freezingScreen);
            }
            unsetAppFreezingScreenLocked(wtoken, true, force);
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void removeAppToken(IBinder token) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "removeAppToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        AppWindowToken wtoken = null;
        boolean delayed = false;
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mWindowMap) {
            WindowToken basewtoken = this.mTokenMap.remove(token);
            if (basewtoken == null || (wtoken = basewtoken.appWindowToken) == null) {
                Slog.w("WindowManager", "Attempted to remove non-existing app token: " + token);
            } else {
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.v("WindowManager", "Removing app token: " + wtoken);
                }
                delayed = setTokenVisibilityLocked(wtoken, null, false, -1, true, wtoken.voiceInteraction);
                wtoken.inPendingTransaction = false;
                this.mOpeningApps.remove(wtoken);
                wtoken.waitingToShow = false;
                if (this.mClosingApps.contains(wtoken)) {
                    delayed = true;
                } else if (this.mAppTransition.isTransitionSet()) {
                    this.mClosingApps.add(wtoken);
                    delayed = true;
                }
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.v("WindowManager", "Removing app " + wtoken + " delayed=" + delayed + " animation=" + wtoken.mAppAnimator.animation + " animating=" + wtoken.mAppAnimator.animating);
                }
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE || WindowManagerDebugConfig.DEBUG_TOKEN_MOVEMENT) {
                    Slog.v("WindowManager", "removeAppToken: " + wtoken + " delayed=" + delayed + " Callers=" + Debug.getCallers(4));
                }
                TaskStack stack = wtoken.mTask.mStack;
                if (!delayed || wtoken.allAppWindows.isEmpty()) {
                    wtoken.mAppAnimator.clearAnimation();
                    wtoken.mAppAnimator.animating = false;
                    wtoken.removeAppFromTaskLocked();
                } else {
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE || WindowManagerDebugConfig.DEBUG_TOKEN_MOVEMENT) {
                        Slog.v("WindowManager", "removeAppToken make exiting: " + wtoken);
                    }
                    stack.mExitingAppTokens.add(wtoken);
                    wtoken.mIsExiting = true;
                }
                wtoken.removed = true;
                startingToken = wtoken.startingData != null ? wtoken : null;
                unsetAppFreezingScreenLocked(wtoken, true, true);
                if (this.mFocusedApp == wtoken) {
                    if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                        Slog.v("WindowManager", "Removing focused app token:" + wtoken);
                    }
                    this.mFocusedApp = null;
                    updateFocusedWindowLocked(0, true);
                    this.mInputMonitor.setFocusedAppLw(null);
                }
            }
            if (!delayed && wtoken != null) {
                wtoken.updateReportedVisibilityLocked();
            }
            scheduleRemoveStartingWindowLocked(startingToken);
        }
        Binder.restoreCallingIdentity(origId);
    }

    void scheduleRemoveStartingWindowLocked(AppWindowToken wtoken) {
        if (wtoken == null || this.mH.hasMessages(6, wtoken)) {
            return;
        }
        if (wtoken.startingWindow == null) {
            if (wtoken.startingData != null) {
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v("WindowManager", "Clearing startingData for token=" + wtoken);
                }
                wtoken.startingData = null;
                return;
            }
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            Slog.v("WindowManager", Debug.getCallers(1) + ": Schedule remove starting " + wtoken + (wtoken != null ? " startingWindow=" + wtoken.startingWindow : ""));
        }
        Message m = this.mH.obtainMessage(6, wtoken);
        this.mH.sendMessage(m);
    }

    void dumpAppTokensLocked() {
        int numStacks = this.mStackIdToStack.size();
        for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
            TaskStack stack = this.mStackIdToStack.valueAt(stackNdx);
            Slog.v("WindowManager", "  Stack #" + stack.mStackId + " tasks from bottom to top:");
            ArrayList<Task> tasks = stack.getTasks();
            int numTasks = tasks.size();
            for (int taskNdx = 0; taskNdx < numTasks; taskNdx++) {
                Task task = tasks.get(taskNdx);
                Slog.v("WindowManager", "    Task #" + task.mTaskId + " activities from bottom to top:");
                AppTokenList tokens = task.mAppTokens;
                int numTokens = tokens.size();
                for (int tokenNdx = 0; tokenNdx < numTokens; tokenNdx++) {
                    Slog.v("WindowManager", "      activity #" + tokenNdx + ": " + tokens.get(tokenNdx).token);
                }
            }
        }
    }

    void dumpWindowsLocked() {
        int numDisplays = this.mDisplayContents.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
            Slog.v("WindowManager", " Display #" + displayContent.getDisplayId());
            WindowList windows = displayContent.getWindowList();
            for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                Slog.v("WindowManager", "  #" + winNdx + ": " + windows.get(winNdx));
            }
        }
    }

    private final int reAddWindowLocked(int index, WindowState win) {
        WindowList windows = win.getWindowList();
        int NCW = win.mChildWindows.size();
        boolean winAdded = false;
        for (int j = 0; j < NCW; j++) {
            WindowState cwin = win.mChildWindows.get(j);
            if (!winAdded && cwin.mSubLayer >= 0) {
                if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                    Slog.v("WindowManager", "Re-adding child window at " + index + ": " + cwin);
                }
                win.mRebuilding = false;
                windows.add(index, win);
                index++;
                winAdded = true;
            }
            if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                Slog.v("WindowManager", "Re-adding window at " + index + ": " + cwin);
            }
            cwin.mRebuilding = false;
            windows.add(index, cwin);
            index++;
        }
        if (!winAdded) {
            if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                Slog.v("WindowManager", "Re-adding window at " + index + ": " + win);
            }
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

    void moveStackWindowsLocked(DisplayContent displayContent) {
        int tmpNdx;
        WindowState tmp;
        int winNdx;
        WindowState win;
        WindowList windows = displayContent.getWindowList();
        this.mTmpWindows.addAll(windows);
        rebuildAppWindowListLocked(displayContent);
        int tmpSize = this.mTmpWindows.size();
        int winSize = windows.size();
        int tmpNdx2 = 0;
        int winNdx2 = 0;
        while (true) {
            if (tmpNdx2 >= tmpSize || winNdx2 >= winSize) {
                break;
            }
            while (true) {
                tmpNdx = tmpNdx2 + 1;
                tmp = this.mTmpWindows.get(tmpNdx2);
                if (tmpNdx >= tmpSize || tmp.mAppToken == null || !tmp.mAppToken.mIsExiting) {
                    break;
                } else {
                    tmpNdx2 = tmpNdx;
                }
            }
            while (true) {
                winNdx = winNdx2 + 1;
                win = windows.get(winNdx2);
                if (winNdx >= winSize || win.mAppToken == null || !win.mAppToken.mIsExiting) {
                    break;
                } else {
                    winNdx2 = winNdx;
                }
            }
            if (tmp != win) {
                displayContent.layoutNeeded = true;
                winNdx2 = winNdx;
                tmpNdx2 = tmpNdx;
                break;
            }
            winNdx2 = winNdx;
            tmpNdx2 = tmpNdx;
        }
        if (tmpNdx2 != winNdx2) {
            displayContent.layoutNeeded = true;
        }
        this.mTmpWindows.clear();
        if (!updateFocusedWindowLocked(3, false)) {
            this.mLayersController.assignLayersLocked(displayContent.getWindowList());
        }
        this.mInputMonitor.setUpdateInputWindowsNeededLw();
        this.mWindowPlacerLocked.performSurfacePlacement();
        this.mInputMonitor.updateInputWindowsLw(false);
    }

    public void moveTaskToTop(int taskId) {
        TaskStack homeStack;
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                Task task = this.mTaskIdToTask.get(taskId);
                if (task == null) {
                    return;
                }
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
                    Slog.e("WindowManager", "moveTaskToBottom: taskId=" + taskId + " not found in mTaskIdToTask");
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

    boolean isStackVisibleLocked(int stackId) {
        TaskStack stack = this.mStackIdToStack.get(stackId);
        if (stack != null) {
            return stack.isVisibleLocked();
        }
        return false;
    }

    public void setDockedStackCreateState(int mode, Rect bounds) {
        synchronized (this.mWindowMap) {
            setDockedStackCreateStateLocked(mode, bounds);
        }
    }

    void setDockedStackCreateStateLocked(int mode, Rect bounds) {
        this.mDockedStackCreateMode = mode;
        this.mDockedStackCreateBounds = bounds;
    }

    public Rect attachStack(int stackId, int displayId, boolean onTop) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                DisplayContent displayContent = this.mDisplayContents.get(displayId);
                if (displayContent == null) {
                    return null;
                }
                TaskStack stack = this.mStackIdToStack.get(stackId);
                if (stack == null) {
                    if (WindowManagerDebugConfig.DEBUG_STACK) {
                        Slog.d("WindowManager", "attachStack: stackId=" + stackId);
                    }
                    stack = new TaskStack(this, stackId);
                    this.mStackIdToStack.put(stackId, stack);
                    if (stackId == 3) {
                        getDefaultDisplayContentLocked().mDividerControllerLocked.notifyDockedStackExistsChanged(true);
                    }
                }
                stack.attachDisplayContent(displayContent);
                displayContent.attachStack(stack, onTop);
                if (stack.getRawFullscreen()) {
                    return null;
                }
                Rect bounds = new Rect();
                stack.getRawBounds(bounds);
                return bounds;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void detachStackLocked(DisplayContent displayContent, TaskStack stack) {
        displayContent.detachStack(stack);
        stack.detachDisplay();
        if (stack.mStackId != 3) {
            return;
        }
        getDefaultDisplayContentLocked().mDividerControllerLocked.notifyDockedStackExistsChanged(false);
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
        synchronized (this.mWindowMap) {
            this.mStackIdToStack.remove(stackId);
        }
    }

    public void removeTask(int taskId) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i("WindowManager", "removeTask: could not find taskId=" + taskId);
                }
            } else {
                task.removeLocked();
            }
        }
    }

    public void cancelTaskWindowTransition(int taskId) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task != null) {
                task.cancelTaskWindowTransition();
            }
        }
    }

    public void cancelTaskThumbnailTransition(int taskId) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task != null) {
                task.cancelTaskThumbnailTransition();
            }
        }
    }

    public void addTask(int taskId, int stackId, boolean toTop) {
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.DEBUG_STACK) {
                Slog.i("WindowManager", "addTask: adding taskId=" + taskId + " to " + (toTop ? "top" : "bottom"));
            }
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i("WindowManager", "addTask: could not find taskId=" + taskId);
                }
                return;
            }
            TaskStack stack = this.mStackIdToStack.get(stackId);
            stack.addTask(task, toTop);
            DisplayContent displayContent = stack.getDisplayContent();
            displayContent.layoutNeeded = true;
            this.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    public void moveTaskToStack(int taskId, int stackId, boolean toTop) {
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.DEBUG_STACK) {
                Slog.i("WindowManager", "moveTaskToStack: moving taskId=" + taskId + " to stackId=" + stackId + " at " + (toTop ? "top" : "bottom"));
            }
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i("WindowManager", "moveTaskToStack: could not find taskId=" + taskId);
                }
                return;
            }
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack == null) {
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i("WindowManager", "moveTaskToStack: could not find stackId=" + stackId);
                }
            } else {
                task.moveTaskToStack(stack, toTop);
                DisplayContent displayContent = stack.getDisplayContent();
                displayContent.layoutNeeded = true;
                this.mWindowPlacerLocked.performSurfacePlacement();
            }
        }
    }

    public void getStackDockedModeBounds(int stackId, Rect bounds, boolean ignoreVisibility) {
        synchronized (this.mWindowMap) {
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack != null) {
                stack.getStackDockedModeBoundsLocked(bounds, ignoreVisibility);
            } else {
                bounds.setEmpty();
            }
        }
    }

    public void getStackBounds(int stackId, Rect bounds) {
        synchronized (this.mWindowMap) {
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack != null) {
                stack.getBounds(bounds);
            } else {
                bounds.setEmpty();
            }
        }
    }

    public boolean resizeStack(int stackId, Rect bounds, SparseArray<Configuration> configs, SparseArray<Rect> taskBounds, SparseArray<Rect> taskTempInsetBounds) {
        boolean rawFullscreen;
        synchronized (this.mWindowMap) {
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack == null) {
                throw new IllegalArgumentException("resizeStack: stackId " + stackId + " not found.");
            }
            if (stack.setBounds(bounds, configs, taskBounds, taskTempInsetBounds) && stack.isVisibleLocked()) {
                stack.getDisplayContent().layoutNeeded = true;
                this.mWindowPlacerLocked.performSurfacePlacement();
            }
            rawFullscreen = stack.getRawFullscreen();
        }
        return rawFullscreen;
    }

    public void prepareFreezingTaskBounds(int stackId) {
        synchronized (this.mWindowMap) {
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack == null) {
                throw new IllegalArgumentException("prepareFreezingTaskBounds: stackId " + stackId + " not found.");
            }
            stack.prepareFreezingTaskBounds();
        }
    }

    public void positionTaskInStack(int taskId, int stackId, int position, Rect bounds, Configuration config) {
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.DEBUG_STACK) {
                Slog.i("WindowManager", "positionTaskInStack: positioning taskId=" + taskId + " in stackId=" + stackId + " at " + position);
            }
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i("WindowManager", "positionTaskInStack: could not find taskId=" + taskId);
                }
                return;
            }
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack == null) {
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i("WindowManager", "positionTaskInStack: could not find stackId=" + stackId);
                }
            } else {
                task.positionTaskInStack(stack, position, bounds, config);
                DisplayContent displayContent = stack.getDisplayContent();
                displayContent.layoutNeeded = true;
                this.mWindowPlacerLocked.performSurfacePlacement();
            }
        }
    }

    public void resizeTask(int taskId, Rect bounds, Configuration configuration, boolean relayout, boolean forced) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                throw new IllegalArgumentException("resizeTask: taskId " + taskId + " not found.");
            }
            if (task.resizeLocked(bounds, configuration, forced) && relayout) {
                task.getDisplayContent().layoutNeeded = true;
                this.mWindowPlacerLocked.performSurfacePlacement();
            }
        }
    }

    public void setTaskDockedResizing(int taskId, boolean resizing) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                Slog.w(TAG, "setTaskDockedResizing: taskId " + taskId + " not found.");
            } else {
                task.setDragResizing(resizing, 1);
            }
        }
    }

    public void scrollTask(int taskId, Rect bounds) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                throw new IllegalArgumentException("scrollTask: taskId " + taskId + " not found.");
            }
            if (task.scrollLocked(bounds)) {
                task.getDisplayContent().layoutNeeded = true;
                this.mInputMonitor.setUpdateInputWindowsNeededLw();
                this.mWindowPlacerLocked.performSurfacePlacement();
            }
        }
    }

    public void deferSurfaceLayout() {
        synchronized (this.mWindowMap) {
            this.mWindowPlacerLocked.deferLayout();
        }
    }

    public void continueSurfaceLayout() {
        synchronized (this.mWindowMap) {
            this.mWindowPlacerLocked.continueLayout();
        }
    }

    public void getTaskBounds(int taskId, Rect bounds) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task != null) {
                task.getBounds(bounds);
            } else {
                bounds.setEmpty();
            }
        }
    }

    public boolean isValidTaskId(int taskId) {
        boolean z;
        synchronized (this.mWindowMap) {
            z = this.mTaskIdToTask.get(taskId) != null;
        }
        return z;
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
                    this.mH.sendEmptyMessageDelayed(30, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
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
        if (Binder.getCallingUid() != 1000 && isKeyguardSecure()) {
            Log.d("WindowManager", "current mode is SecurityMode, ignore disableKeyguard");
        } else if (Binder.getCallingUserHandle().getIdentifier() != this.mCurrentUserId) {
            Log.d("WindowManager", "non-current user, ignore disableKeyguard");
        } else {
            if (token == null) {
                throw new IllegalArgumentException("token == null");
            }
            this.mKeyguardDisableHandler.sendMessage(this.mKeyguardDisableHandler.obtainMessage(1, new Pair(token, tag)));
        }
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
        int userId = UserHandle.getCallingUserId();
        long origId = Binder.clearCallingIdentity();
        try {
            return this.mPolicy.isKeyguardSecure(userId);
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

    public void keyguardGoingAway(int flags) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
            Slog.d("WindowManager", "keyguardGoingAway: flags=0x" + Integer.toHexString(flags));
        }
        synchronized (this.mWindowMap) {
            this.mAnimator.mKeyguardGoingAway = true;
            this.mAnimator.mKeyguardGoingAwayFlags = flags;
            this.mWindowPlacerLocked.requestTraversal();
        }
    }

    public void keyguardWaitingForActivityDrawn() {
        if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
            Slog.d("WindowManager", "keyguardWaitingForActivityDrawn");
        }
        synchronized (this.mWindowMap) {
            this.mKeyguardWaitingForActivityDrawn = true;
        }
    }

    public void notifyActivityDrawnForKeyguard() {
        if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
            Slog.d("WindowManager", "notifyActivityDrawnForKeyguard: waiting=" + this.mKeyguardWaitingForActivityDrawn + " Callers=" + Debug.getCallers(5));
        }
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

    public void lockDeviceNow() {
        lockNow(null);
    }

    public int getCameraLensCoverState() {
        int sw = this.mInputManager.getSwitchState(-1, -256, 9);
        if (sw > 0) {
            return 1;
        }
        return sw == 0 ? 0 : -1;
    }

    public void switchInputMethod(boolean forwardDirection) {
        InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        if (inputMethodManagerInternal == null) {
            return;
        }
        inputMethodManagerInternal.switchInputMethod(forwardDirection);
    }

    public void shutdown(boolean confirm) {
        ShutdownThread.shutdown(this.mContext, "userrequested", confirm);
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
            this.mPolicy.enableKeyguard(true);
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
                displayContent.switchUserStacks();
                rebuildAppWindowListLocked(displayContent);
            }
            this.mWindowPlacerLocked.performSurfacePlacement();
            DisplayContent displayContent2 = getDefaultDisplayContentLocked();
            displayContent2.mDividerControllerLocked.notifyDockedStackExistsChanged(hasDockedTasksForUser(newUserId));
            if (this.mDisplayReady) {
                int forcedDensity = getForcedDisplayDensityForUserLocked(newUserId);
                int targetDensity = forcedDensity != 0 ? forcedDensity : displayContent2.mInitialDisplayDensity;
                setForcedDisplayDensityLocked(displayContent2, targetDensity);
            }
        }
    }

    boolean hasDockedTasksForUser(int userId) {
        TaskStack stack = this.mStackIdToStack.get(3);
        if (stack == null) {
            return false;
        }
        ArrayList<Task> tasks = stack.getTasks();
        boolean hasUserTask = false;
        for (int i = tasks.size() - 1; i >= 0 && !hasUserTask; i--) {
            Task task = tasks.get(i);
            hasUserTask = task.mUserId == userId;
        }
        return hasUserTask;
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
            if (WindowManagerDebugConfig.DEBUG_BOOT) {
                RuntimeException here = new RuntimeException("here");
                here.fillInStackTrace();
                Slog.i("WindowManager", "enableScreenAfterBoot: mDisplayEnabled=" + this.mDisplayEnabled + " mForceDisplayEnabled=" + this.mForceDisplayEnabled + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted, here);
            }
            if (this.mSystemBooted) {
                return;
            }
            this.mSystemBooted = true;
            hideBootMessagesLocked();
            this.mH.sendEmptyMessageDelayed(23, 30000L);
            this.mPolicy.systemBooted();
            performEnableScreen();
        }
    }

    public void enableScreenIfNeeded() {
        synchronized (this.mWindowMap) {
            enableScreenIfNeededLocked();
        }
    }

    void enableScreenIfNeededLocked() {
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
            if (this.mDisplayEnabled) {
                return;
            }
            Slog.w("WindowManager", "***** BOOT TIMEOUT: forcing display enabled");
            this.mForceDisplayEnabled = true;
            performEnableScreen();
        }
    }

    private boolean checkWaitingForWindowsLocked() {
        boolean haveBootMsg = false;
        boolean haveApp = false;
        boolean haveWallpaper = false;
        boolean wallpaperEnabled = this.mContext.getResources().getBoolean(R.^attr-private.expandActivityOverflowButtonDrawable) && !this.mOnlyCore;
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
                } else if (w.mAttrs.type == 2000) {
                    haveKeyguard = this.mPolicy.isKeyguardDrawnLw();
                }
            }
        }
        if (WindowManagerDebugConfig.DEBUG_SCREEN_ON || WindowManagerDebugConfig.DEBUG_BOOT) {
            Slog.i("WindowManager", "******** booted=" + this.mSystemBooted + " msg=" + this.mShowingBootMessages + " haveBoot=" + haveBootMsg + " haveApp=" + haveApp + " haveWall=" + haveWallpaper + " wallEnabled=" + wallpaperEnabled + " haveKeyguard=" + haveKeyguard);
        }
        if (!this.mSystemBooted && !haveBootMsg) {
            return true;
        }
        if (this.mSystemBooted) {
            return !(haveApp || haveKeyguard) || (wallpaperEnabled && !haveWallpaper);
        }
        return false;
    }

    public void performEnableScreen() {
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.DEBUG_BOOT || !IS_USER_BUILD) {
                Slog.i("WindowManager", "performEnableScreen: mDisplayEnabled=" + this.mDisplayEnabled + " mForceDisplayEnabled=" + this.mForceDisplayEnabled + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted + " mOnlyCore=" + this.mOnlyCore + " mIsAlarmBooting=" + this.mIsAlarmBooting + " mBootAnimationStopped=" + this.mBootAnimationStopped, new RuntimeException("here").fillInStackTrace());
            }
            if (this.mDisplayEnabled) {
                return;
            }
            if (this.mSystemBooted || this.mShowingBootMessages) {
                if (this.mForceDisplayEnabled || !checkWaitingForWindowsLocked()) {
                    if (this.mIsAlarmBooting) {
                        return;
                    }
                    if (!this.mBootAnimationStopped) {
                        Trace.asyncTraceBegin(32L, "Stop bootanim", 0);
                        try {
                            IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                            if (surfaceFlinger != null) {
                                Parcel data = Parcel.obtain();
                                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                                surfaceFlinger.transact(1, data, null, 0);
                                data.recycle();
                                if (!IS_USER_BUILD) {
                                    Slog.d(TAG, "Tell SurfaceFlinger finish boot animation");
                                }
                            }
                        } catch (RemoteException e) {
                            Slog.e("WindowManager", "Boot completed: SurfaceFlinger is dead!");
                        }
                        this.mBootAnimationStopped = true;
                    }
                    if (!this.mForceDisplayEnabled && !checkBootAnimationCompleteLocked()) {
                        if (WindowManagerDebugConfig.DEBUG_BOOT) {
                            Slog.i("WindowManager", "performEnableScreen: Waiting for anim complete");
                        }
                        return;
                    }
                    EventLog.writeEvent(EventLogTags.WM_BOOT_ANIMATION_DONE, SystemClock.uptimeMillis());
                    Trace.asyncTraceEnd(32L, "Stop bootanim", 0);
                    this.mDisplayEnabled = true;
                    if (WindowManagerDebugConfig.DEBUG_SCREEN_ON || WindowManagerDebugConfig.DEBUG_BOOT) {
                        Slog.i("WindowManager", "******************** ENABLING SCREEN!");
                    }
                    this.mInputMonitor.setEventDispatchingLw(true);
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

    public void showBootMessage(CharSequence msg, boolean always) {
        boolean first = false;
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.DEBUG_BOOT) {
                RuntimeException here = new RuntimeException("here");
                here.fillInStackTrace();
                Slog.i("WindowManager", "showBootMessage: msg=" + msg + " always=" + always + " mAllowBootMessages=" + this.mAllowBootMessages + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted, here);
            }
            if (this.mAllowBootMessages) {
                if (!this.mShowingBootMessages) {
                    if (!always) {
                        return;
                    } else {
                        first = true;
                    }
                }
                if (this.mSystemBooted) {
                    return;
                }
                this.mShowingBootMessages = true;
                this.mPolicy.showBootMessage(msg, always);
                if (first) {
                    performEnableScreen();
                }
            }
        }
    }

    public void hideBootMessagesLocked() {
        if (WindowManagerDebugConfig.DEBUG_BOOT) {
            RuntimeException here = new RuntimeException("here");
            here.fillInStackTrace();
            Slog.i("WindowManager", "hideBootMessagesLocked: mDisplayEnabled=" + this.mDisplayEnabled + " mForceDisplayEnabled=" + this.mForceDisplayEnabled + " mShowingBootMessages=" + this.mShowingBootMessages + " mSystemBooted=" + this.mSystemBooted, here);
        }
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

    private void updateCircularDisplayMaskIfNeeded() {
        int currentUserId;
        if (!this.mContext.getResources().getConfiguration().isScreenRound() || !this.mContext.getResources().getBoolean(R.^attr-private.listLayout)) {
            return;
        }
        synchronized (this.mWindowMap) {
            currentUserId = this.mCurrentUserId;
        }
        int inversionState = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_inversion_enabled", 0, currentUserId);
        int showMask = inversionState == 1 ? 0 : 1;
        Message m = this.mH.obtainMessage(35);
        m.arg1 = showMask;
        this.mH.sendMessage(m);
    }

    public void showEmulatorDisplayOverlayIfNeeded() {
        if (this.mContext.getResources().getBoolean(R.^attr-private.lockPatternStyle) && SystemProperties.getBoolean(PROPERTY_EMULATOR_CIRCULAR, false) && Build.IS_EMULATOR) {
            this.mH.sendMessage(this.mH.obtainMessage(36));
        }
    }

    public void showCircularMask(boolean visible) {
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", ">>> OPEN TRANSACTION showCircularMask(visible=" + visible + ")");
            }
            SurfaceControl.openTransaction();
            try {
                if (visible) {
                    if (this.mCircularDisplayMask == null) {
                        int screenOffset = this.mContext.getResources().getInteger(R.integer.config_dozeWakeLockScreenDebounce);
                        int maskThickness = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_list_divider_height);
                        this.mCircularDisplayMask = new CircularDisplayMask(getDefaultDisplayContentLocked().getDisplay(), this.mFxSession, (this.mPolicy.windowTypeToLayerLw(2018) * 10000) + 10, screenOffset, maskThickness);
                    }
                    this.mCircularDisplayMask.setVisibility(true);
                } else if (this.mCircularDisplayMask != null) {
                    this.mCircularDisplayMask.setVisibility(false);
                    this.mCircularDisplayMask = null;
                }
            } finally {
                SurfaceControl.closeTransaction();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", "<<< CLOSE TRANSACTION showCircularMask(visible=" + visible + ")");
                }
            }
        }
    }

    public void showEmulatorDisplayOverlay() {
        synchronized (this.mWindowMap) {
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", ">>> OPEN TRANSACTION showEmulatorDisplayOverlay");
            }
            SurfaceControl.openTransaction();
            try {
                if (this.mEmulatorDisplayOverlay == null) {
                    this.mEmulatorDisplayOverlay = new EmulatorDisplayOverlay(this.mContext, getDefaultDisplayContentLocked().getDisplay(), this.mFxSession, (this.mPolicy.windowTypeToLayerLw(2018) * 10000) + 10);
                }
                this.mEmulatorDisplayOverlay.setVisibility(true);
            } finally {
                SurfaceControl.closeTransaction();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", "<<< CLOSE TRANSACTION showEmulatorDisplayOverlay");
                }
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
            if (!on) {
                if (WindowManagerDebugConfig.SHOW_VERBOSE_TRANSACTIONS) {
                }
                SurfaceControl.openTransaction();
                if (this.mStrictModeFlash == null) {
                }
                this.mStrictModeFlash.setVisibility(on);
                return;
            }
            boolean isVisible = false;
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                int numWindows = windows.size();
                int winNdx = 0;
                while (true) {
                    if (winNdx < numWindows) {
                        WindowState ws = windows.get(winNdx);
                        if (ws.mSession.mPid == pid && ws.isVisibleLw()) {
                            isVisible = true;
                            break;
                        }
                        winNdx++;
                    }
                }
            }
            if (!isVisible) {
                return;
            }
            if (WindowManagerDebugConfig.SHOW_VERBOSE_TRANSACTIONS) {
                Slog.i("WindowManager", ">>> OPEN TRANSACTION showStrictModeViolation");
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
                if (WindowManagerDebugConfig.SHOW_VERBOSE_TRANSACTIONS) {
                    Slog.i("WindowManager", "<<< CLOSE TRANSACTION showStrictModeViolation");
                }
            }
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
        if (rot != 3) {
            return;
        }
        int tmp4 = crop.top;
        crop.top = crop.left;
        crop.left = dh - crop.bottom;
        crop.bottom = crop.right;
        crop.right = dh - tmp4;
    }

    public boolean requestAssistScreenshot(final IAssistScreenshotReceiver receiver) {
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "requestAssistScreenshot()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        FgThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                Bitmap bm = WindowManagerService.this.screenshotApplicationsInner(null, 0, -1, -1, true, 1.0f, Bitmap.Config.ARGB_8888);
                try {
                    receiver.send(bm);
                } catch (RemoteException e) {
                }
            }
        });
        return true;
    }

    public Bitmap screenshotApplications(IBinder appToken, int displayId, int width, int height, float frameScale) {
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "screenshotApplications()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        try {
            Trace.traceBegin(32L, "screenshotApplications");
            return screenshotApplicationsInner(appToken, displayId, width, height, false, frameScale, Bitmap.Config.RGB_565);
        } finally {
            Trace.traceEnd(32L);
        }
    }

    Bitmap screenshotApplicationsInner(IBinder appToken, int displayId, int width, int height, boolean includeFullDisplay, float frameScale, Bitmap.Config config) {
        boolean screenshotReady;
        int minLayer;
        boolean includeImeInScreenshot;
        int layer;
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent == null) {
                if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                    Slog.i("WindowManager", "Screenshot of " + appToken + ": returning null. No Display for displayId=" + displayId);
                }
                return null;
            }
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            int dw = displayInfo.logicalWidth;
            int dh = displayInfo.logicalHeight;
            if (dw == 0 || dh == 0) {
                if (!WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                    return null;
                }
                Slog.i("WindowManager", "Screenshot of " + appToken + ": returning null. logical widthxheight=" + dw + "x" + dh);
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
            synchronized (this.mWindowMap) {
                AppWindowToken appWindowToken = this.mInputMethodTarget != null ? this.mInputMethodTarget.mAppToken : null;
                includeImeInScreenshot = (appWindowToken == null || appWindowToken.appToken == null || appWindowToken.appToken.asBinder() != appToken) ? false : !this.mInputMethodTarget.isInMultiWindowMode();
            }
            int aboveAppLayer = ((this.mPolicy.windowTypeToLayerLw(2) + 1) * 10000) + 1000;
            synchronized (this.mWindowMap) {
                WindowState appWin = null;
                WindowList windows = displayContent.getWindowList();
                for (int i = windows.size() - 1; i >= 0; i--) {
                    WindowState ws = windows.get(i);
                    if (ws.mHasSurface && ws.mLayer < aboveAppLayer) {
                        if (ws.mIsImWindow) {
                            if (includeImeInScreenshot) {
                                WindowStateAnimator winAnim = ws.mWinAnimator;
                                layer = winAnim.mSurfaceController.getLayer();
                                if (maxLayer < layer) {
                                    maxLayer = layer;
                                }
                                if (minLayer > layer) {
                                    minLayer = layer;
                                }
                                if (!includeFullDisplay && !ws.mIsWallpaper) {
                                    Rect wf = ws.mFrame;
                                    Rect cr = ws.mContentInsets;
                                    int left = wf.left + cr.left;
                                    int top = wf.top + cr.top;
                                    int right = wf.right - cr.right;
                                    int bottom = wf.bottom - cr.bottom;
                                    frame.union(left, top, right, bottom);
                                    ws.getVisibleBounds(stackBounds);
                                    if (!Rect.intersects(frame, stackBounds)) {
                                        frame.setEmpty();
                                    }
                                }
                                if (ws.mAppToken != null && ws.mAppToken.token == appToken && ws.isDisplayedLw() && winAnim.getShown()) {
                                    screenshotReady = true;
                                }
                                if (!ws.isObscuringFullscreen(displayInfo)) {
                                    break;
                                }
                            } else {
                                continue;
                            }
                        } else if (!ws.mIsWallpaper) {
                            if (appToken != null) {
                                if (ws.mAppToken != null && ws.mAppToken.token == appToken) {
                                    appWin = ws;
                                }
                            }
                            WindowStateAnimator winAnim2 = ws.mWinAnimator;
                            layer = winAnim2.mSurfaceController.getLayer();
                            if (maxLayer < layer) {
                            }
                            if (minLayer > layer) {
                            }
                            if (!includeFullDisplay) {
                                Rect wf2 = ws.mFrame;
                                Rect cr2 = ws.mContentInsets;
                                int left2 = wf2.left + cr2.left;
                                int top2 = wf2.top + cr2.top;
                                int right2 = wf2.right - cr2.right;
                                int bottom2 = wf2.bottom - cr2.bottom;
                                frame.union(left2, top2, right2, bottom2);
                                ws.getVisibleBounds(stackBounds);
                                if (!Rect.intersects(frame, stackBounds)) {
                                }
                            }
                            if (ws.mAppToken != null) {
                                screenshotReady = true;
                            }
                            if (!ws.isObscuringFullscreen(displayInfo)) {
                            }
                        } else if (appWin == null) {
                        }
                    }
                }
                if (appToken != null && appWin == null) {
                    if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                        Slog.i("WindowManager", "Screenshot: Couldn't find a surface matching " + appToken);
                    }
                    return null;
                }
                if (!screenshotReady) {
                    Slog.i("WindowManager", "Failed to capture screenshot of " + appToken + " appWin=" + (appWin == null ? "null" : appWin + " drawState=" + appWin.mWinAnimator.mDrawState));
                    return null;
                }
                if (maxLayer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                        Slog.i("WindowManager", "Screenshot of " + appToken + ": returning null maxLayer=" + maxLayer);
                    }
                    return null;
                }
                if (includeFullDisplay) {
                    frame.set(0, 0, dw, dh);
                } else if (!frame.intersect(0, 0, dw, dh)) {
                    frame.setEmpty();
                }
                if (frame.isEmpty()) {
                    return null;
                }
                if (width < 0) {
                    width = (int) (frame.width() * frameScale);
                }
                if (height < 0) {
                    height = (int) (frame.height() * frameScale);
                }
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
                float adjWidth = displayContent.mInitialDisplayWidth / displayContent.mBaseDisplayWidth;
                float adjHeight = displayContent.mInitialDisplayHeight / displayContent.mBaseDisplayHeight;
                crop.left = (int) (crop.left * adjWidth);
                crop.right = (int) (crop.right * adjWidth);
                crop.top = (int) (crop.top * adjHeight);
                crop.bottom = (int) (crop.bottom * adjHeight);
                if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                    Slog.i("WindowManager", "Screenshot: " + dw + "x" + dh + " from " + minLayer + " to " + maxLayer + " appToken=" + appToken);
                    for (int i2 = 0; i2 < windows.size(); i2++) {
                        WindowState win = windows.get(i2);
                        WindowSurfaceController controller = win.mWinAnimator.mSurfaceController;
                        Slog.i("WindowManager", win + ": " + win.mLayer + " animLayer=" + win.mWinAnimator.mAnimLayer + " surfaceLayer=" + (controller == null ? "null" : Integer.valueOf(controller.getLayer())));
                    }
                }
                ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(0);
                boolean zIsAnimating = screenRotationAnimation != null ? screenRotationAnimation.isAnimating() : false;
                if (WindowManagerDebugConfig.DEBUG_SCREENSHOT && zIsAnimating) {
                    Slog.v("WindowManager", "Taking screenshot while rotating");
                }
                SurfaceControl.openTransaction();
                SurfaceControl.closeTransactionSync();
                Trace.traceBegin(32L, "wmScreenshot");
                Bitmap bm = SurfaceControl.screenshot(crop, width, height, minLayer, maxLayer, zIsAnimating, rot);
                Trace.traceEnd(32L);
                if (bm == null) {
                    Slog.w("WindowManager", "Screenshot failure taking screenshot for (" + dw + "x" + dh + ") to layer " + maxLayer);
                    return null;
                }
                if (isFastStartingWindowSupport() && isCacheLastFrame()) {
                    Bitmap bmShot = SurfaceControl.screenshot(new Rect(), dw, dh, minLayer, maxLayer, false, 0);
                    setBitmapByToken(appWin.mWinAnimator.mWin.mToken.token, bmShot.copy(bmShot.getConfig(), true));
                    bmShot.recycle();
                }
                if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                    int[] buffer = new int[bm.getWidth() * bm.getHeight()];
                    bm.getPixels(buffer, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight());
                    boolean allBlack = true;
                    int firstColor = buffer[0];
                    int i3 = 0;
                    while (true) {
                        if (i3 >= buffer.length) {
                            break;
                        }
                        if (buffer[i3] != firstColor) {
                            allBlack = false;
                            break;
                        }
                        i3++;
                    }
                    if (allBlack) {
                        Slog.i("WindowManager", "Screenshot " + appWin + " was monochrome(" + Integer.toHexString(firstColor) + ")! mSurfaceLayer=" + (appWin != null ? Integer.valueOf(appWin.mWinAnimator.mSurfaceController.getLayer()) : "null") + " minLayer=" + minLayer + " maxLayer=" + maxLayer);
                    }
                }
                Bitmap ret = bm.createAshmemBitmap(config);
                bm.recycle();
                return ret;
            }
        }
    }

    public void freezeRotation(int rotation) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "freezeRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        if (rotation < -1 || rotation > 3) {
            throw new IllegalArgumentException("Rotation argument must be -1 or a valid rotation constant.");
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "freezeRotation: mRotation=" + this.mRotation);
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
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "thawRotation: mRotation=" + this.mRotation);
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
        if (this.mDeferredRotationPauseCount <= 0) {
            return;
        }
        this.mDeferredRotationPauseCount--;
        if (this.mDeferredRotationPauseCount != 0) {
            return;
        }
        boolean changed = updateRotationUncheckedLocked(false);
        if (!changed) {
            return;
        }
        this.mH.sendEmptyMessage(18);
    }

    public void updateRotationUnchecked(boolean alwaysSendConfiguration, boolean forceRelayout) {
        boolean changed;
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "updateRotationUnchecked(alwaysSendConfiguration=" + alwaysSendConfiguration + ")");
        }
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mWindowMap) {
            changed = updateRotationUncheckedLocked(false);
            if (!changed || forceRelayout) {
                getDefaultDisplayContentLocked().layoutNeeded = true;
                this.mWindowPlacerLocked.performSurfacePlacement();
            }
        }
        if (changed || alwaysSendConfiguration) {
            sendNewConfiguration();
        }
        Binder.restoreCallingIdentity(origId);
    }

    public boolean updateRotationUncheckedLocked(boolean inTransaction) {
        if (this.mDeferredRotationPauseCount > 0) {
            if (!WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                return false;
            }
            Slog.v("WindowManager", "Deferring rotation, rotation is paused.");
            return false;
        }
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(0);
        if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
            if (!WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                return false;
            }
            Slog.v("WindowManager", "Deferring rotation, animation in progress.");
            return false;
        }
        if (!this.mDisplayEnabled && !this.mIsUpdateIpoRotation) {
            if (!WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                return false;
            }
            Slog.v("WindowManager", "Deferring rotation, display is not enabled.");
            return false;
        }
        int rotation = (this.mIsUpdateIpoRotation || this.mIsUpdateAlarmBootRotation) ? 0 : this.mPolicy.rotationForOrientationLw(this.mForcedAppOrientation, this.mRotation);
        boolean altOrientation = !this.mPolicy.rotationHasCompatibleMetricsLw(this.mForcedAppOrientation, rotation);
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "Application requested orientation " + this.mForcedAppOrientation + ", got rotation " + rotation + " which has " + (altOrientation ? "incompatible" : "compatible") + " metrics");
        }
        if (this.mRotation == rotation && this.mAltOrientation == altOrientation) {
            return false;
        }
        Slog.v("WindowManager", "Rotation changed to " + rotation + (altOrientation ? " (alt)" : "") + " from " + this.mRotation + (this.mAltOrientation ? " (alt)" : "") + ", forceApp=" + this.mForcedAppOrientation);
        this.mRotation = rotation;
        this.mAltOrientation = altOrientation;
        this.mPolicy.setRotationLw(this.mRotation);
        this.mWindowsFreezingScreen = 1;
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
        updateDisplayAndOrientationLocked(this.mCurConfiguration.uiMode);
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        if (!inTransaction) {
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                Slog.i("WindowManager", ">>> OPEN TRANSACTION setRotationUnchecked");
            }
            SurfaceControl.openTransaction();
        }
        if (screenRotationAnimation2 != null) {
            try {
                if (screenRotationAnimation2.hasScreenshot() && screenRotationAnimation2.setRotationInTransaction(rotation, this.mFxSession, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, getTransitionAnimationScaleLocked(), displayInfo.logicalWidth, displayInfo.logicalHeight)) {
                    scheduleAnimationLocked();
                }
            } finally {
                if (!inTransaction) {
                    SurfaceControl.closeTransaction();
                    if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                        Slog.i("WindowManager", "<<< CLOSE TRANSACTION setRotationUnchecked");
                    }
                }
            }
        }
        this.mDisplayManagerInternal.performTraversalInTransactionFromWindowManager();
        WindowList windows = displayContent.getWindowList();
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowState w = windows.get(i);
            if (w.mAppToken != null) {
                w.mAppToken.destroySavedSurfaces();
            }
            if (w.mHasSurface) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Set mOrientationChanging of " + w);
                }
                w.mOrientationChanging = true;
                this.mWindowPlacerLocked.mOrientationChangeComplete = false;
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
        if (!this.mIsUpdateAlarmBootRotation) {
            return true;
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v(TAG, "Update power-off alarm Boot rotation is done");
        }
        this.mIsUpdateAlarmBootRotation = false;
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
                    case 0:
                    default:
                        return 81;
                    case 1:
                        return 85;
                    case 2:
                        return 81;
                    case 3:
                        return 8388691;
                }
            }
            switch (rotation) {
                case 0:
                default:
                    return 85;
                case 1:
                    return 81;
                case 2:
                    return 8388691;
                case 3:
                    return 81;
            }
        }
    }

    public boolean startViewServer(int port) {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "startViewServer") || port < 1024) {
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
            this.mViewServer = new ViewServer(this, port);
            return this.mViewServer.start();
        } catch (IOException e2) {
            Slog.w("WindowManager", "View server did not start");
            return false;
        }
    }

    private boolean isSystemSecure() {
        if ("1".equals(SystemProperties.get(SYSTEM_SECURE, "1"))) {
            return "0".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
        }
        return false;
    }

    public boolean stopViewServer() {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "stopViewServer") || this.mViewServer == null) {
            return false;
        }
        return this.mViewServer.stop();
    }

    public boolean isViewServerRunning() {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "isViewServerRunning") || this.mViewServer == null) {
            return false;
        }
        return this.mViewServer.isRunning();
    }

    boolean viewServerListWindows(Socket client) throws Throwable {
        if (isSystemSecure()) {
            return false;
        }
        boolean result = true;
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
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(clientStream), PackageManagerService.DumpState.DUMP_PREFERRED_XML);
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
                if (out2 != null) {
                    try {
                        out2.close();
                    } catch (IOException e) {
                        result = false;
                    }
                }
                return result;
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
        boolean result = true;
        WindowState focusedWindow = getFocusedWindow();
        BufferedWriter out = null;
        try {
            OutputStream clientStream = client.getOutputStream();
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(clientStream), PackageManagerService.DumpState.DUMP_PREFERRED_XML);
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
            if (out2 != null) {
                try {
                    out2.close();
                } catch (IOException e4) {
                    result = false;
                }
            }
            return result;
        } catch (Exception e5) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    boolean viewServerWindowCommand(Socket client, String command, String parameters) throws Throwable {
        WindowState window;
        if (isSystemSecure()) {
            return false;
        }
        boolean success = true;
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
                if (index < parameters.length()) {
                    parameters = parameters.substring(index + 1);
                } else {
                    parameters = "";
                }
                window = findWindow(hashCode);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Exception e) {
            e = e;
        }
        if (window == null) {
            return false;
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
                Slog.w("WindowManager", "Could not send command " + command + " with parameters " + parameters, e);
                success = false;
                if (data != null) {
                }
                if (reply != null) {
                }
                if (out != null) {
                }
            } catch (Throwable th2) {
                th = th2;
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
        if (out != null) {
            try {
                out.close();
            } catch (IOException e4) {
            }
        }
        return success;
        Slog.w("WindowManager", "Could not send command " + command + " with parameters " + parameters, e);
        success = false;
        if (data != null) {
            data.recycle();
        }
        if (reply != null) {
            reply.recycle();
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e5) {
            }
        }
        return success;
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
            if (this.mWindowChangeListeners.isEmpty()) {
                return;
            }
            WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
            for (WindowChangeListener windowChangeListener : windowChangeListeners) {
                windowChangeListener.windowsChanged();
            }
        }
    }

    private void notifyFocusChanged() {
        synchronized (this.mWindowMap) {
            if (this.mWindowChangeListeners.isEmpty()) {
                return;
            }
            WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
            for (WindowChangeListener windowChangeListener : windowChangeListeners) {
                windowChangeListener.focusChanged();
            }
        }
    }

    private WindowState findWindow(int hashCode) {
        if (hashCode == -1) {
            return getFocusedWindow();
        }
        synchronized (this.mWindowMap) {
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                int numWindows = windows.size();
                for (int winNdx = 0; winNdx < numWindows; winNdx++) {
                    WindowState w = windows.get(winNdx);
                    if (System.identityHashCode(w) == hashCode) {
                        return w;
                    }
                }
            }
            return null;
        }
    }

    void sendNewConfiguration() {
        try {
            this.mActivityManager.updateConfiguration((Configuration) null);
        } catch (RemoteException e) {
        }
    }

    public Configuration computeNewConfiguration() {
        Configuration configurationComputeNewConfigurationLocked;
        synchronized (this.mWindowMap) {
            configurationComputeNewConfigurationLocked = computeNewConfigurationLocked();
        }
        return configurationComputeNewConfigurationLocked;
    }

    private Configuration computeNewConfigurationLocked() {
        if (!this.mDisplayReady) {
            return null;
        }
        Configuration config = new Configuration();
        config.fontScale = 0.0f;
        computeScreenConfigurationLocked(config);
        return config;
    }

    private void adjustDisplaySizeRanges(DisplayInfo displayInfo, int rotation, int uiMode, int dw, int dh) {
        int width = this.mPolicy.getConfigDisplayWidth(dw, dh, rotation, uiMode);
        if (width < displayInfo.smallestNominalAppWidth) {
            displayInfo.smallestNominalAppWidth = width;
        }
        if (width > displayInfo.largestNominalAppWidth) {
            displayInfo.largestNominalAppWidth = width;
        }
        int height = this.mPolicy.getConfigDisplayHeight(dw, dh, rotation, uiMode);
        if (height < displayInfo.smallestNominalAppHeight) {
            displayInfo.smallestNominalAppHeight = height;
        }
        if (height <= displayInfo.largestNominalAppHeight) {
            return;
        }
        displayInfo.largestNominalAppHeight = height;
    }

    private int reduceConfigLayout(int curLayout, int rotation, float density, int dw, int dh, int uiMode) {
        int w = this.mPolicy.getNonDecorDisplayWidth(dw, dh, rotation, uiMode);
        int h = this.mPolicy.getNonDecorDisplayHeight(dw, dh, rotation, uiMode);
        int longSize = w;
        int shortSize = h;
        if (w < h) {
            longSize = h;
            shortSize = w;
        }
        return Configuration.reduceScreenLayout(curLayout, (int) (longSize / density), (int) (shortSize / density));
    }

    private void computeSizeRangesAndScreenLayout(DisplayInfo displayInfo, boolean rotated, int uiMode, int dw, int dh, float density, Configuration outConfig) {
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
        adjustDisplaySizeRanges(displayInfo, 0, uiMode, unrotDw, unrotDh);
        adjustDisplaySizeRanges(displayInfo, 1, uiMode, unrotDh, unrotDw);
        adjustDisplaySizeRanges(displayInfo, 2, uiMode, unrotDw, unrotDh);
        adjustDisplaySizeRanges(displayInfo, 3, uiMode, unrotDh, unrotDw);
        int sl = Configuration.resetScreenLayout(outConfig.screenLayout);
        int sl2 = reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(sl, 0, density, unrotDw, unrotDh, uiMode), 1, density, unrotDh, unrotDw, uiMode), 2, density, unrotDw, unrotDh, uiMode), 3, density, unrotDh, unrotDw, uiMode);
        outConfig.smallestScreenWidthDp = (int) (displayInfo.smallestNominalAppWidth / density);
        outConfig.screenLayout = sl2;
    }

    private int reduceCompatConfigWidthSize(int curSize, int rotation, int uiMode, DisplayMetrics dm, int dw, int dh) {
        dm.noncompatWidthPixels = this.mPolicy.getNonDecorDisplayWidth(dw, dh, rotation, uiMode);
        dm.noncompatHeightPixels = this.mPolicy.getNonDecorDisplayHeight(dw, dh, rotation, uiMode);
        float scale = CompatibilityInfo.computeCompatibleScaling(dm, (DisplayMetrics) null);
        int size = (int) (((dm.noncompatWidthPixels / scale) / dm.density) + 0.5f);
        if (curSize == 0 || size < curSize) {
            return size;
        }
        return curSize;
    }

    private int computeCompatSmallestWidth(boolean rotated, int uiMode, DisplayMetrics dm, int dw, int dh) {
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
        int sw = reduceCompatConfigWidthSize(0, 0, uiMode, tmpDm, unrotDw, unrotDh);
        return reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(sw, 1, uiMode, tmpDm, unrotDh, unrotDw), 2, uiMode, tmpDm, unrotDw, unrotDh), 3, uiMode, tmpDm, unrotDh, unrotDw);
    }

    DisplayInfo updateDisplayAndOrientationLocked(int uiMode) {
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
        int appWidth = this.mPolicy.getNonDecorDisplayWidth(dw, dh, this.mRotation, uiMode);
        int appHeight = this.mPolicy.getNonDecorDisplayHeight(dw, dh, this.mRotation, uiMode);
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        displayInfo.rotation = this.mRotation;
        displayInfo.logicalWidth = dw;
        displayInfo.logicalHeight = dh;
        displayInfo.logicalDensityDpi = displayContent.mBaseDisplayDensity;
        displayInfo.appWidth = appWidth;
        displayInfo.appHeight = appHeight;
        int iw = rotated ? displayContent.mInitialDisplayHeight : displayContent.mInitialDisplayWidth;
        int ih = rotated ? displayContent.mInitialDisplayWidth : displayContent.mInitialDisplayHeight;
        displayInfo.physicalXDpi = (displayContent.mInitialPhysicalXDpi * dw) / iw;
        displayInfo.physicalYDpi = (displayContent.mInitialPhysicalYDpi * dh) / ih;
        displayInfo.getLogicalMetrics(this.mRealDisplayMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, (Configuration) null);
        displayInfo.getAppMetrics(this.mDisplayMetrics);
        if (displayContent.mDisplayScalingDisabled) {
            displayInfo.flags |= 1073741824;
        } else {
            displayInfo.flags &= -1073741825;
        }
        this.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(displayContent.getDisplayId(), displayInfo);
        displayContent.mBaseDisplayRect.set(0, 0, dw, dh);
        this.mCompatibleScreenScale = CompatibilityInfo.computeCompatibleScaling(this.mDisplayMetrics, this.mCompatDisplayMetrics);
        return displayInfo;
    }

    void computeScreenConfigurationLocked(Configuration config) {
        int i;
        int presenceFlag;
        DisplayInfo displayInfo = updateDisplayAndOrientationLocked(config.uiMode);
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        config.orientation = dw <= dh ? 1 : 2;
        config.screenWidthDp = (int) (this.mPolicy.getConfigDisplayWidth(dw, dh, this.mRotation, config.uiMode) / this.mDisplayMetrics.density);
        config.screenHeightDp = (int) (this.mPolicy.getConfigDisplayHeight(dw, dh, this.mRotation, config.uiMode) / this.mDisplayMetrics.density);
        boolean rotated = this.mRotation == 1 || this.mRotation == 3;
        computeSizeRangesAndScreenLayout(displayInfo, rotated, config.uiMode, dw, dh, this.mDisplayMetrics.density, config);
        int i2 = config.screenLayout & (-769);
        if ((displayInfo.flags & 16) != 0) {
            i = 512;
        } else {
            i = 256;
        }
        config.screenLayout = i | i2;
        config.compatScreenWidthDp = (int) (config.screenWidthDp / this.mCompatibleScreenScale);
        config.compatScreenHeightDp = (int) (config.screenHeightDp / this.mCompatibleScreenScale);
        config.compatSmallestScreenWidthDp = computeCompatSmallestWidth(rotated, config.uiMode, this.mDisplayMetrics, dw, dh);
        config.densityDpi = displayInfo.logicalDensityDpi;
        config.touchscreen = 1;
        config.keyboard = 1;
        config.navigation = 1;
        int keyboardPresence = 0;
        int navigationPresence = 0;
        InputDevice[] devices = this.mInputManager.getInputDevices();
        for (InputDevice device : devices) {
            if (!device.isVirtual()) {
                int sources = device.getSources();
                if (device.isExternal()) {
                    presenceFlag = 2;
                } else {
                    presenceFlag = 1;
                }
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
        config.keyboardHidden = 1;
        config.hardKeyboardHidden = 1;
        config.navigationHidden = 1;
        this.mPolicy.adjustConfigurationLw(config, keyboardPresence, navigationPresence);
    }

    void notifyHardKeyboardStatusChange() {
        WindowManagerInternal.OnHardKeyboardStatusChangeListener listener;
        boolean available;
        synchronized (this.mWindowMap) {
            listener = this.mHardKeyboardStatusChangeListener;
            available = this.mHardKeyboardAvailable;
        }
        if (listener == null) {
            return;
        }
        listener.onHardKeyboardStatusChange(available);
    }

    boolean startMovingTask(IWindow window, float startX, float startY) {
        synchronized (this.mWindowMap) {
            WindowState win = windowForClientLocked((Session) null, window, false);
            if (!startPositioningLocked(win, false, startX, startY)) {
                return false;
            }
            try {
                this.mActivityManager.setFocusedTask(win.getTask().mTaskId);
                return true;
            } catch (RemoteException e) {
                return true;
            }
        }
    }

    private void startScrollingTask(DisplayContent displayContent, int startX, int startY) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "startScrollingTask: {" + startX + ", " + startY + "}");
        }
        Task task = null;
        synchronized (this.mWindowMap) {
            int taskId = displayContent.taskIdFromPoint(startX, startY);
            if (taskId >= 0) {
                task = this.mTaskIdToTask.get(taskId);
            }
            if (task != null && task.isDockedInEffect()) {
                if (startPositioningLocked(task.getTopVisibleAppMainWindow(), false, startX, startY)) {
                    try {
                        this.mActivityManager.setFocusedTask(task.mTaskId);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    private void handleTapOutsideTask(DisplayContent displayContent, int x, int y) {
        int taskId;
        synchronized (this.mWindowMap) {
            Task task = displayContent.findTaskForControlPoint(x, y);
            if (task != null) {
                if (!startPositioningLocked(task.getTopVisibleAppMainWindow(), true, x, y)) {
                    return;
                } else {
                    taskId = task.mTaskId;
                }
            } else {
                taskId = displayContent.taskIdFromPoint(x, y);
            }
            if (taskId < 0) {
                return;
            }
            try {
                this.mActivityManager.setFocusedTask(taskId);
            } catch (RemoteException e) {
            }
        }
    }

    private boolean startPositioningLocked(WindowState win, boolean resize, float startX, float startY) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "startPositioningLocked: win=" + win + ", resize=" + resize + ", {" + startX + ", " + startY + "}");
        }
        if (win == null || win.getAppToken() == null) {
            Slog.w("WindowManager", "startPositioningLocked: Bad window " + win);
            return false;
        }
        if (win.mInputChannel == null) {
            Slog.wtf("WindowManager", "startPositioningLocked: " + win + " has no input channel,  probably being removed");
            return false;
        }
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent == null) {
            Slog.w("WindowManager", "startPositioningLocked: Invalid display content " + win);
            return false;
        }
        Display display = displayContent.getDisplay();
        this.mTaskPositioner = new TaskPositioner(this);
        this.mTaskPositioner.register(display);
        this.mInputMonitor.updateInputWindowsLw(true);
        WindowState transferFocusFromWin = win;
        if (this.mCurrentFocus != null && this.mCurrentFocus != win && this.mCurrentFocus.mAppToken == win.mAppToken) {
            transferFocusFromWin = this.mCurrentFocus;
        }
        if (this.mInputManager.transferTouchFocus(transferFocusFromWin.mInputChannel, this.mTaskPositioner.mServerChannel)) {
            this.mTaskPositioner.startDragLocked(win, resize, startX, startY);
            return true;
        }
        Slog.e("WindowManager", "startPositioningLocked: Unable to transfer touch focus");
        this.mTaskPositioner.unregister();
        this.mTaskPositioner = null;
        this.mInputMonitor.updateInputWindowsLw(true);
        return false;
    }

    private void finishPositioning() {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "finishPositioning");
        }
        synchronized (this.mWindowMap) {
            if (this.mTaskPositioner != null) {
                this.mTaskPositioner.unregister();
                this.mTaskPositioner = null;
                this.mInputMonitor.updateInputWindowsLw(true);
            }
        }
    }

    void adjustForImeIfNeeded(DisplayContent displayContent) {
        WindowState imeWin = this.mInputMethodWindow;
        boolean imeVisible = imeWin != null && imeWin.isVisibleLw() && imeWin.isDisplayedLw() && !displayContent.mDividerControllerLocked.isImeHideRequested();
        boolean dockVisible = isStackVisibleLocked(3);
        TaskStack imeTargetStack = getImeFocusStackLocked();
        int imeDockSide = (!dockVisible || imeTargetStack == null) ? -1 : imeTargetStack.getDockSide();
        boolean imeOnTop = imeDockSide == 2;
        boolean imeOnBottom = imeDockSide == 4;
        boolean dockMinimized = displayContent.mDividerControllerLocked.isMinimizedDock();
        int imeHeight = this.mPolicy.getInputMethodWindowVisibleHeightLw();
        boolean imeHeightChanged = imeVisible && imeHeight != displayContent.mDividerControllerLocked.getImeHeightAdjustedFor();
        if (imeVisible && dockVisible && ((imeOnTop || imeOnBottom) && !dockMinimized)) {
            ArrayList<TaskStack> stacks = displayContent.getStacks();
            for (int i = stacks.size() - 1; i >= 0; i--) {
                TaskStack stack = stacks.get(i);
                boolean isDockedOnBottom = stack.getDockSide() == 4;
                if (stack.isVisibleLocked() && (imeOnBottom || isDockedOnBottom)) {
                    stack.setAdjustedForIme(imeWin, imeOnBottom ? imeHeightChanged : false);
                } else {
                    stack.resetAdjustedForIme(false);
                }
            }
            displayContent.mDividerControllerLocked.setAdjustedForIme(imeOnBottom, true, true, imeWin, imeHeight);
            return;
        }
        ArrayList<TaskStack> stacks2 = displayContent.getStacks();
        for (int i2 = stacks2.size() - 1; i2 >= 0; i2--) {
            stacks2.get(i2).resetAdjustedForIme(!dockVisible);
        }
        displayContent.mDividerControllerLocked.setAdjustedForIme(false, false, dockVisible, imeWin, imeHeight);
    }

    IBinder prepareDragSurface(IWindow window, SurfaceSession session, int flags, int width, int height, Surface outSurface) throws Throwable {
        IBinder token;
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d("WindowManager", "prepare drag surface: w=" + width + " h=" + height + " flags=" + Integer.toHexString(flags) + " win=" + window + " asbinder=" + window.asBinder());
        }
        int callerPid = Binder.getCallingPid();
        int callerUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                synchronized (this.mWindowMap) {
                    try {
                        if (this.mDragState == null) {
                            DisplayContent displayContent = getDefaultDisplayContentLocked();
                            Display display = displayContent.getDisplay();
                            SurfaceControl surface = new SurfaceControl(session, "drag surface", width, height, -3, 4);
                            surface.setLayerStack(display.getLayerStack());
                            float alpha = (flags & 512) == 0 ? DRAG_SHADOW_ALPHA_TRANSPARENT : 1.0f;
                            surface.setAlpha(alpha);
                            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                                Slog.i("WindowManager", "  DRAG " + surface + ": CREATE");
                            }
                            outSurface.copyFrom(surface);
                            IBinder winBinder = window.asBinder();
                            token = new Binder();
                            try {
                                try {
                                    this.mDragState = new DragState(this, token, surface, flags, winBinder);
                                    this.mDragState.mPid = callerPid;
                                    this.mDragState.mUid = callerUid;
                                    this.mDragState.mOriginalAlpha = alpha;
                                    IBinder token2 = new Binder();
                                    this.mDragState.mToken = token2;
                                    this.mH.removeMessages(20, winBinder);
                                    Message msg = this.mH.obtainMessage(20, winBinder);
                                    this.mH.sendMessageDelayed(msg, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                                    token = token2;
                                } catch (Surface.OutOfResourcesException e) {
                                    e = e;
                                    Slog.e("WindowManager", "Can't allocate drag surface w=" + width + " h=" + height, e);
                                    if (this.mDragState != null) {
                                        this.mDragState.reset();
                                        this.mDragState = null;
                                    }
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } else {
                            Slog.w("WindowManager", "Drag already in progress");
                            token = null;
                        }
                    } catch (Surface.OutOfResourcesException e2) {
                        e = e2;
                        token = null;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                    Binder.restoreCallingIdentity(origId);
                    return token;
                }
            } catch (Throwable th3) {
                th = th3;
                Binder.restoreCallingIdentity(origId);
                throw th;
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

    TaskStack getImeFocusStackLocked() {
        if (this.mFocusedApp == null || this.mFocusedApp.mTask == null) {
            return null;
        }
        return this.mFocusedApp.mTask.mStack;
    }

    private void showAuditSafeModeNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this.mContext, 0, new Intent("android.intent.action.VIEW", Uri.parse("https://support.google.com/nexus/answer/2852139")), 0);
        String title = this.mContext.getString(R.string.media_route_status_scanning);
        Notification notification = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.stat_sys_warning).setWhen(0L).setOngoing(true).setTicker(title).setLocalOnly(true).setPriority(1).setVisibility(1).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(this.mContext.getString(R.string.mediasize_chinese_om_dai_pa_kai)).setContentIntent(pendingIntent).build();
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        notificationManager.notifyAsUser(null, R.string.media_route_status_scanning, notification, UserHandle.ALL);
    }

    public boolean detectSafeMode() {
        if (!this.mInputMonitor.waitForInputDevicesReady(1000L)) {
            Slog.w("WindowManager", "Devices still not ready after waiting 1000 milliseconds before attempting to detect safe mode.");
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "safe_boot_disallowed", 0) != 0) {
            return false;
        }
        int menuState = this.mInputManager.getKeyCodeState(-1, -256, 82);
        int sState = this.mInputManager.getKeyCodeState(-1, -256, 47);
        int dpadState = this.mInputManager.getKeyCodeState(-1, 513, 23);
        int trackballState = this.mInputManager.getScanCodeState(-1, 65540, InputManagerService.BTN_MOUSE);
        int volumeDownState = this.mInputManager.getKeyCodeState(-1, -256, 25);
        boolean z = menuState > 0 || sState > 0 || dpadState > 0 || trackballState > 0 || volumeDownState > 0;
        this.mSafeMode = z;
        try {
            if (SystemProperties.getInt(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, 0) != 0 || SystemProperties.getInt(ShutdownThread.RO_SAFEMODE_PROPERTY, 0) != 0) {
                int auditSafeMode = SystemProperties.getInt(ShutdownThread.AUDIT_SAFEMODE_PROPERTY, 0);
                if (auditSafeMode == 0) {
                    this.mSafeMode = true;
                    SystemProperties.set(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, "");
                } else {
                    int buildDate = SystemProperties.getInt(PROPERTY_BUILD_DATE_UTC, 0);
                    if (auditSafeMode >= buildDate) {
                        this.mSafeMode = true;
                        showAuditSafeModeNotification();
                    } else {
                        SystemProperties.set(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, "");
                        SystemProperties.set(ShutdownThread.AUDIT_SAFEMODE_PROPERTY, "");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
        }
        int dcha_state = BenesseExtension.getDchaState();
        if (dcha_state != 0 && Build.TYPE.equals("user")) {
            this.mSafeMode = false;
        }
        SystemProperties.set("persist.sys.bc.dcha_state", String.valueOf(dcha_state));
        if (this.mSafeMode) {
            Log.i("WindowManager", "SAFE MODE ENABLED (menu=" + menuState + " s=" + sState + " dpad=" + dpadState + " trackball=" + trackballState + ")");
            SystemProperties.set(ShutdownThread.RO_SAFEMODE_PROPERTY, "1");
        } else {
            Log.i("WindowManager", "SAFE MODE not enabled");
        }
        this.mPolicy.setSafeMode(this.mSafeMode);
        return this.mSafeMode;
    }

    public void displayReady() {
        for (Display display : this.mDisplays) {
            displayReady(display.getDisplayId());
        }
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDefaultDisplayContentLocked();
            readForcedDisplayPropertiesLocked(displayContent);
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
        updateCircularDisplayMaskIfNeeded();
    }

    private void displayReady(int displayId) {
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null) {
                this.mAnimator.addDisplayLocked(displayId);
                displayContent.initializeDisplayBaseInfo();
                if (displayContent.mTapDetector != null) {
                    displayContent.mTapDetector.init();
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
        public static final int CACHE_STARTING_WINDOW = 55;
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
        public static final int FINISH_TASK_POSITIONING = 40;
        public static final int FORCE_GC = 15;
        public static final int NEW_ANIMATOR_SCALE = 34;
        public static final int NOTIFY_ACTIVITY_DRAWN = 32;
        public static final int NOTIFY_APP_TRANSITION_CANCELLED = 48;
        public static final int NOTIFY_APP_TRANSITION_FINISHED = 49;
        public static final int NOTIFY_APP_TRANSITION_STARTING = 47;
        public static final int NOTIFY_DOCKED_STACK_MINIMIZED_CHANGED = 53;
        public static final int NOTIFY_STARTING_WINDOW_DRAWN = 50;
        public static final int PERSIST_ANIMATION_SCALE = 14;
        public static final int REMOVE_STARTING = 6;
        public static final int REPORT_APPLICATION_TOKEN_DRAWN = 9;
        public static final int REPORT_APPLICATION_TOKEN_WINDOWS = 8;
        public static final int REPORT_FOCUS_CHANGE = 2;
        public static final int REPORT_HARD_KEYBOARD_STATUS_CHANGE = 22;
        public static final int REPORT_LOSING_FOCUS = 3;
        public static final int REPORT_WINDOWS_CHANGE = 19;
        public static final int RESET_ANR_MESSAGE = 38;
        public static final int RESIZE_STACK = 42;
        public static final int RESIZE_TASK = 43;
        public static final int SEND_NEW_CONFIGURATION = 18;
        public static final int SHOW_CIRCULAR_DISPLAY_MASK = 35;
        public static final int SHOW_EMULATOR_DISPLAY_OVERLAY = 36;
        public static final int SHOW_STRICT_MODE_VIOLATION = 25;
        public static final int TAP_OUTSIDE_TASK = 31;
        public static final int TWO_FINGER_SCROLL_START = 44;
        public static final int UNUSED = 0;
        public static final int UPDATE_ANIMATION_SCALE = 51;
        public static final int UPDATE_DOCKED_STACK_DIVIDER = 41;
        public static final int UPDATE_IPO_ROTATION = 54;
        public static final int WAITING_FOR_DRAWN_TIMEOUT = 24;
        public static final int WALLPAPER_DRAW_PENDING_TIMEOUT = 39;
        public static final int WINDOW_FREEZE_TIMEOUT = 11;
        public static final int WINDOW_REMOVE_TIMEOUT = 52;
        public static final int WINDOW_REPLACEMENT_TIMEOUT = 46;

        H() {
        }

        @Override
        public void handleMessage(android.os.Message r51) {
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.WindowManagerService.H.handleMessage(android.os.Message):void");
        }
    }

    void destroyPreservedSurfaceLocked() {
        for (int i = this.mDestroyPreservedSurface.size() - 1; i >= 0; i--) {
            WindowState w = this.mDestroyPreservedSurface.get(i);
            w.mWinAnimator.destroyPreservedSurfaceLocked();
        }
        this.mDestroyPreservedSurface.clear();
    }

    void stopUsingSavedSurfaceLocked() {
        for (int i = this.mFinishedEarlyAnim.size() - 1; i >= 0; i--) {
            AppWindowToken wtoken = this.mFinishedEarlyAnim.get(i);
            wtoken.stopUsingSavedSurfaceLocked();
        }
        this.mFinishedEarlyAnim.clear();
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
        synchronized (this.mWindowMap) {
            int idx = findDesiredInputMethodWindowIndexLocked(false);
            if (idx > 0) {
                WindowState imFocus = getDefaultWindowListLocked().get(idx - 1);
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    Slog.i("WindowManager", "Desired input method target: " + imFocus);
                    Slog.i("WindowManager", "Current focus: " + this.mCurrentFocus);
                    Slog.i("WindowManager", "Last focus: " + this.mLastFocus);
                }
                if (imFocus != null) {
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
                                Log.i("WindowManager", "Switching to real app window: " + w);
                                imFocus = w;
                                break;
                            }
                        }
                    }
                    if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                        Slog.i("WindowManager", "IM target client: " + imFocus.mSession.mClient);
                        if (imFocus.mSession.mClient != null) {
                            Slog.i("WindowManager", "IM target client binder: " + imFocus.mSession.mClient.asBinder());
                            Slog.i("WindowManager", "Requesting client binder: " + client.asBinder());
                        }
                    }
                    if (imFocus.mSession.mClient != null && imFocus.mSession.mClient.asBinder() == client.asBinder()) {
                        return true;
                    }
                }
            }
            if (this.mCurrentFocus != null && this.mCurrentFocus.mSession.mClient != null) {
                if (this.mCurrentFocus.mSession.mClient.asBinder() == client.asBinder()) {
                    return true;
                }
            }
            return false;
        }
    }

    public void getInitialDisplaySize(int displayId, Point size) {
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                size.x = displayContent.mInitialDisplayWidth;
                size.y = displayContent.mInitialDisplayHeight;
            }
        }
    }

    public void getBaseDisplaySize(int displayId, Point size) {
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                size.x = displayContent.mBaseDisplayWidth;
                size.y = displayContent.mBaseDisplayHeight;
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

    public void setForcedDisplayScalingMode(int displayId, int mode) {
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
                    if (mode < 0 || mode > 1) {
                        mode = 0;
                    }
                    setForcedDisplayScalingModeLocked(displayContent, mode);
                    Settings.Global.putInt(this.mContext.getContentResolver(), "display_scaling_force", mode);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setForcedDisplayScalingModeLocked(DisplayContent displayContent, int mode) {
        Slog.i("WindowManager", "Using display scaling mode: " + (mode == 0 ? "auto" : "off"));
        displayContent.mDisplayScalingDisabled = mode != 0;
        reconfigureDisplayLocked(displayContent);
    }

    private void readForcedDisplayPropertiesLocked(DisplayContent displayContent) {
        int pos;
        String sizeStr = Settings.Global.getString(this.mContext.getContentResolver(), "display_size_forced");
        if (sizeStr == null || sizeStr.length() == 0) {
            sizeStr = SystemProperties.get(SIZE_OVERRIDE, (String) null);
        }
        if (sizeStr != null && sizeStr.length() > 0 && (pos = sizeStr.indexOf(44)) > 0 && sizeStr.lastIndexOf(44) == pos) {
            try {
                int width = Integer.parseInt(sizeStr.substring(0, pos));
                int height = Integer.parseInt(sizeStr.substring(pos + 1));
                if (displayContent.mBaseDisplayWidth != width || displayContent.mBaseDisplayHeight != height) {
                    Slog.i("WindowManager", "FORCED DISPLAY SIZE: " + width + "x" + height);
                    displayContent.mBaseDisplayWidth = width;
                    displayContent.mBaseDisplayHeight = height;
                }
            } catch (NumberFormatException e) {
            }
        }
        int density = getForcedDisplayDensityForUserLocked(this.mCurrentUserId);
        if (density != 0) {
            displayContent.mBaseDisplayDensity = density;
        }
        int mode = Settings.Global.getInt(this.mContext.getContentResolver(), "display_scaling_force", 0);
        if (mode == 0) {
            return;
        }
        Slog.i("WindowManager", "FORCED DISPLAY SCALING DISABLED");
        displayContent.mDisplayScalingDisabled = true;
    }

    private void setForcedDisplaySizeLocked(DisplayContent displayContent, int width, int height) {
        Slog.i("WindowManager", "Using new display size: " + width + "x" + height);
        displayContent.mBaseDisplayWidth = width;
        displayContent.mBaseDisplayHeight = height;
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
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                return displayContent.mInitialDisplayDensity;
            }
            return -1;
        }
    }

    public int getBaseDisplayDensity(int displayId) {
        synchronized (this.mWindowMap) {
            DisplayContent displayContent = getDisplayContentLocked(displayId);
            if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                return displayContent.mBaseDisplayDensity;
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
                    Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "display_density_forced", Integer.toString(density), this.mCurrentUserId);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
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
                    Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "display_density_forced", "", this.mCurrentUserId);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private int getForcedDisplayDensityForUserLocked(int userId) {
        String densityStr = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", userId);
        if (densityStr == null || densityStr.length() == 0) {
            densityStr = SystemProperties.get(DENSITY_OVERRIDE, (String) null);
        }
        if (densityStr != null && densityStr.length() > 0) {
            try {
                return Integer.parseInt(densityStr);
            } catch (NumberFormatException e) {
            }
        }
        return 0;
    }

    private void setForcedDisplayDensityLocked(DisplayContent displayContent, int density) {
        displayContent.mBaseDisplayDensity = density;
        reconfigureDisplayLocked(displayContent);
    }

    private void reconfigureDisplayLocked(DisplayContent displayContent) {
        if (!this.mDisplayReady) {
            return;
        }
        configureDisplayPolicyLocked(displayContent);
        displayContent.layoutNeeded = true;
        boolean configChanged = updateOrientationFromAppTokensLocked(false);
        this.mTempConfiguration.setToDefaults();
        this.mTempConfiguration.updateFrom(this.mCurConfiguration);
        computeScreenConfigurationLocked(this.mTempConfiguration);
        if (configChanged | (this.mCurConfiguration.diff(this.mTempConfiguration) != 0)) {
            this.mWaitingForConfig = true;
            startFreezingDisplayLocked(false, 0, 0);
            this.mH.sendEmptyMessage(18);
            if (!this.mReconfigureOnConfigurationChanged.contains(displayContent)) {
                this.mReconfigureOnConfigurationChanged.add(displayContent);
            }
        }
        this.mWindowPlacerLocked.performSurfacePlacement();
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
        displayInfo.overscanLeft = left;
        displayInfo.overscanTop = top;
        displayInfo.overscanRight = right;
        displayInfo.overscanBottom = bottom;
        this.mDisplaySettings.setOverscanLocked(displayInfo.uniqueId, displayInfo.name, left, top, right, bottom);
        this.mDisplaySettings.writeSettingsLocked();
        reconfigureDisplayLocked(displayContent);
    }

    final WindowState windowForClientLocked(Session session, IWindow client, boolean throwOnError) {
        return windowForClientLocked(session, client.asBinder(), throwOnError);
    }

    final WindowState windowForClientLocked(Session session, IBinder client, boolean throwOnError) {
        WindowState win = this.mWindowMap.get(client);
        if (localLOGV) {
            Slog.v("WindowManager", "Looking up client " + client + ": " + win);
        }
        if (win == null) {
            RuntimeException ex = new IllegalArgumentException("Requested window " + client + " does not exist");
            if (throwOnError) {
                throw ex;
            }
            Slog.w("WindowManager", "Failed looking up window", ex);
            return null;
        }
        if (session != null && win.mSession != session) {
            RuntimeException ex2 = new IllegalArgumentException("Requested window " + client + " is in session " + win.mSession + ", not " + session);
            if (throwOnError) {
                throw ex2;
            }
            Slog.w("WindowManager", "Failed looking up window", ex2);
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
                if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                    Slog.v("WindowManager", "Rebuild removing window: " + win);
                }
                NW--;
                numRemoved++;
            } else {
                if (lastBelow == i - 1 && w.mAttrs.type == 2013) {
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
                    if (!wtoken.mIsExiting || wtoken.waitingForReplacement()) {
                        i2 = reAddAppWindowsLocked(displayContent, i2, wtoken);
                    }
                }
            }
        }
        int i3 = i2 - lastBelow2;
        if (i3 != numRemoved) {
            displayContent.layoutNeeded = true;
            Slog.w("WindowManager", "On display=" + displayContent.getDisplayId() + " Rebuild removed " + numRemoved + " windows but added " + i3 + " rebuildAppWindowListLocked()  callers=" + Debug.getCallers(10));
            for (int i4 = 0; i4 < numRemoved; i4++) {
                WindowState ws = this.mRebuildTmp[i4];
                if (ws.mRebuilding) {
                    StringWriter sw = new StringWriter();
                    FastPrintWriter fastPrintWriter = new FastPrintWriter(sw, false, 1024);
                    ws.dump(fastPrintWriter, "", true);
                    fastPrintWriter.flush();
                    Slog.w("WindowManager", "This window was lost: " + ws);
                    Slog.w("WindowManager", sw.toString());
                    ws.mWinAnimator.destroySurfaceLocked();
                }
            }
            Slog.w("WindowManager", "Current app token list:");
            dumpAppTokensLocked();
            Slog.w("WindowManager", "Final window list:");
            dumpWindowsLocked();
        }
        Arrays.fill(this.mRebuildTmp, (Object) null);
    }

    void makeWindowFreezingScreenIfNeededLocked(WindowState w) {
        if (okToDisplay() || this.mWindowsFreezingScreen == 2) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "Changing surface while display frozen: " + w);
        }
        w.mOrientationChanging = true;
        w.mLastFreezeDuration = 0;
        this.mWindowPlacerLocked.mOrientationChangeComplete = false;
        if (this.mWindowsFreezingScreen != 0) {
            return;
        }
        this.mWindowsFreezingScreen = 1;
        this.mH.removeMessages(11);
        this.mH.sendEmptyMessageDelayed(11, 2000L);
    }

    int handleAnimatingStoppedAndTransitionLocked() {
        this.mAppTransition.setIdle();
        for (int i = this.mNoAnimationNotifyOnTransitionFinished.size() - 1; i >= 0; i--) {
            IBinder token = this.mNoAnimationNotifyOnTransitionFinished.get(i);
            this.mAppTransition.notifyAppTransitionFinishedLocked(token);
        }
        this.mNoAnimationNotifyOnTransitionFinished.clear();
        this.mWallpaperControllerLocked.hideDeferredWallpapersIfNeeded();
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
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.v("WindowManager", "Wallpaper layer changed: assigning layers + relayout");
        }
        moveInputMethodWindowsIfNeededLocked(true);
        this.mWindowPlacerLocked.mWallpaperMayChange = true;
        this.mFocusMayChange = true;
        return 1;
    }

    void updateResizingWindows(WindowState w) {
        WindowStateAnimator winAnimator = w.mWinAnimator;
        if (w.mHasSurface && w.mLayoutSeq == this.mLayoutSeq && !w.isGoneForLayoutLw()) {
            Task task = w.getTask();
            if (task == null || !task.mStack.getBoundsAnimating()) {
                w.setInsetsChanged();
                boolean configChanged = w.isConfigChanged();
                if (WindowManagerDebugConfig.DEBUG_CONFIGURATION && configChanged) {
                    Slog.v("WindowManager", "Win " + w + " config changed: " + this.mCurConfiguration);
                }
                boolean dragResizingChanged = w.isDragResizeChanged() && !w.isDragResizingChangeReported();
                if (localLOGV) {
                    Slog.v("WindowManager", "Resizing " + w + ": configChanged=" + configChanged + " dragResizingChanged=" + dragResizingChanged + " last=" + w.mLastFrame + " frame=" + w.mFrame);
                }
                w.mLastFrame.set(w.mFrame);
                if (!w.mContentInsetsChanged && !w.mVisibleInsetsChanged && !winAnimator.mSurfaceResized && !w.mOutsetsChanged && !configChanged && !dragResizingChanged && w.isResizedWhileNotDragResizingReported()) {
                    if (w.mOrientationChanging && w.isDrawnLw()) {
                        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                            Slog.v("WindowManager", "Orientation not waiting for draw in " + w + ", surfaceController " + winAnimator.mSurfaceController);
                        }
                        w.mOrientationChanging = false;
                        w.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
                        return;
                    }
                    return;
                }
                if (WindowManagerDebugConfig.DEBUG_RESIZE || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Resize reasons for w=" + w + ":  contentInsetsChanged=" + w.mContentInsetsChanged + " " + w.mContentInsets.toShortString() + " visibleInsetsChanged=" + w.mVisibleInsetsChanged + " " + w.mVisibleInsets.toShortString() + " stableInsetsChanged=" + w.mStableInsetsChanged + " " + w.mStableInsets.toShortString() + " outsetsChanged=" + w.mOutsetsChanged + " " + w.mOutsets.toShortString() + " surfaceResized=" + winAnimator.mSurfaceResized + " configChanged=" + configChanged + " dragResizingChanged=" + dragResizingChanged + " resizedWhileNotDragResizingReported=" + w.isResizedWhileNotDragResizingReported() + " contentInsets=" + w.mContentInsets + " visibleInsets=" + w.mVisibleInsets);
                }
                if (w.mAppToken != null && w.mAppDied) {
                    w.mAppToken.removeAllDeadWindows();
                    return;
                }
                w.mLastOverscanInsets.set(w.mOverscanInsets);
                w.mLastContentInsets.set(w.mContentInsets);
                w.mLastVisibleInsets.set(w.mVisibleInsets);
                w.mLastStableInsets.set(w.mStableInsets);
                w.mLastOutsets.set(w.mOutsets);
                makeWindowFreezingScreenIfNeededLocked(w);
                if (w.mOrientationChanging || dragResizingChanged || w.isResizedWhileNotDragResizing()) {
                    if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE || WindowManagerDebugConfig.DEBUG_ANIM || WindowManagerDebugConfig.DEBUG_ORIENTATION || WindowManagerDebugConfig.DEBUG_RESIZE) {
                        Slog.v("WindowManager", "Orientation or resize start waiting for draw, mDrawState=DRAW_PENDING in " + w + ", surfaceController " + winAnimator.mSurfaceController);
                    }
                    winAnimator.mDrawState = 1;
                    if (w.mAppToken != null) {
                        w.mAppToken.clearAllDrawn();
                    }
                }
                if (this.mResizingWindows.contains(w)) {
                    return;
                }
                if (WindowManagerDebugConfig.DEBUG_RESIZE || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Resizing window " + w);
                }
                this.mResizingWindows.add(w);
            }
        }
    }

    void checkDrawnWindowsLocked() {
        if (this.mWaitingForDrawn.isEmpty() || this.mWaitingForDrawnCallback == null) {
            return;
        }
        for (int j = this.mWaitingForDrawn.size() - 1; j >= 0; j--) {
            WindowState win = this.mWaitingForDrawn.get(j);
            if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                Slog.i("WindowManager", "Waiting for drawn " + win + ": removed=" + win.mRemoved + " visible=" + win.isVisibleLw() + " mHasSurface=" + win.mHasSurface + " drawState=" + win.mWinAnimator.mDrawState);
            }
            if (win.mRemoved || !win.mHasSurface || !win.mPolicyVisibility) {
                if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                    Slog.w("WindowManager", "Aborted waiting for drawn: " + win);
                }
                this.mWaitingForDrawn.remove(win);
            } else if (win.hasDrawnLw()) {
                if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                    Slog.d("WindowManager", "Window drawn win=" + win);
                }
                this.mWaitingForDrawn.remove(win);
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

    void setHoldScreenLocked(Session newHoldScreen) {
        boolean hold = newHoldScreen != null;
        if (hold && this.mHoldingScreenOn != newHoldScreen) {
            this.mHoldingScreenWakeLock.setWorkSource(new WorkSource(newHoldScreen.mUid));
        }
        this.mHoldingScreenOn = newHoldScreen;
        boolean state = this.mHoldingScreenWakeLock.isHeld();
        if (hold == state) {
            return;
        }
        if (hold) {
            if (WindowManagerDebugConfig.DEBUG_KEEP_SCREEN_ON) {
                Slog.d("DebugKeepScreenOn", "Acquiring screen wakelock due to " + this.mWindowPlacerLocked.mHoldScreenWindow);
            }
            this.mLastWakeLockHoldingWindow = this.mWindowPlacerLocked.mHoldScreenWindow;
            this.mLastWakeLockObscuringWindow = null;
            this.mHoldingScreenWakeLock.acquire();
            this.mPolicy.keepScreenOnStartedLw();
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_KEEP_SCREEN_ON) {
            Slog.d("DebugKeepScreenOn", "Releasing screen wakelock, obscured by " + this.mWindowPlacerLocked.mObsuringWindow);
        }
        this.mLastWakeLockHoldingWindow = null;
        this.mLastWakeLockObscuringWindow = this.mWindowPlacerLocked.mObsuringWindow;
        this.mPolicy.keepScreenOnStoppedLw();
        this.mHoldingScreenWakeLock.release();
    }

    void requestTraversal() {
        synchronized (this.mWindowMap) {
            this.mWindowPlacerLocked.requestTraversal();
        }
    }

    void scheduleAnimationLocked() {
        if (this.mAnimationScheduled) {
            return;
        }
        this.mAnimationScheduled = true;
        this.mChoreographer.postFrameCallback(this.mAnimator.mAnimationFrameCallback);
    }

    boolean needsLayout() {
        int numDisplays = this.mDisplayContents.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = this.mDisplayContents.valueAt(displayNdx);
            if (displayContent.layoutNeeded) {
                return true;
            }
        }
        return false;
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
        WindowSurfaceController surfaceController = winAnimator.mSurfaceController;
        boolean leakedSurface = false;
        boolean killedApps = false;
        EventLog.writeEvent(EventLogTags.WM_NO_SURFACE_MEMORY, winAnimator.mWin.toString(), Integer.valueOf(winAnimator.mSession.mPid), operation);
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            Slog.i("WindowManager", "Out of memory for surface!  Looking for leaks...");
            int numDisplays = this.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                WindowList windows = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                int numWindows = windows.size();
                for (int winNdx = 0; winNdx < numWindows; winNdx++) {
                    WindowState ws = windows.get(winNdx);
                    WindowStateAnimator wsa = ws.mWinAnimator;
                    if (wsa.mSurfaceController != null) {
                        if (!this.mSessions.contains(wsa.mSession)) {
                            Slog.w("WindowManager", "LEAKED SURFACE (session doesn't exist): " + ws + " surface=" + wsa.mSurfaceController + " token=" + ws.mToken + " pid=" + ws.mSession.mPid + " uid=" + ws.mSession.mUid);
                            wsa.destroySurface();
                            this.mForceRemoves.add(ws);
                            leakedSurface = true;
                        } else if (ws.mAppToken != null && ws.mAppToken.clientHidden) {
                            Slog.w("WindowManager", "LEAKED SURFACE (app token hidden): " + ws + " surface=" + wsa.mSurfaceController + " token=" + ws.mAppToken + " saved=" + ws.hasSavedSurface());
                            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                                logSurface(ws, "LEAK DESTROY", false);
                            }
                            wsa.destroySurface();
                            leakedSurface = true;
                        }
                    }
                }
            }
            if (!leakedSurface) {
                Slog.w("WindowManager", "No leaked surfaces; killing applicatons!");
                SparseIntArray pidCandidates = new SparseIntArray();
                for (int displayNdx2 = 0; displayNdx2 < numDisplays; displayNdx2++) {
                    WindowList windows2 = this.mDisplayContents.valueAt(displayNdx2).getWindowList();
                    int numWindows2 = windows2.size();
                    for (int winNdx2 = 0; winNdx2 < numWindows2; winNdx2++) {
                        WindowState ws2 = windows2.get(winNdx2);
                        if (!this.mForceRemoves.contains(ws2)) {
                            WindowStateAnimator wsa2 = ws2.mWinAnimator;
                            if (wsa2.mSurfaceController != null) {
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
                Slog.w("WindowManager", "Looks like we have reclaimed some memory, clearing surface for retry.");
                if (surfaceController != null) {
                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                        logSurface(winAnimator.mWin, "RECOVER DESTROY", false);
                    }
                    winAnimator.destroySurface();
                    scheduleRemoveStartingWindowLocked(winAnimator.mWin.mAppToken);
                }
                try {
                    winAnimator.mWin.mClient.dispatchGetNewSurface();
                } catch (RemoteException e2) {
                }
            }
            if (leakedSurface) {
                return true;
            }
            return killedApps;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    boolean updateFocusedWindowLocked(int mode, boolean updateInputWindows) {
        WindowState newFocus = computeFocusedWindowLocked();
        if (this.mCurrentFocus == newFocus) {
            return false;
        }
        Trace.traceBegin(32L, "wmUpdateFocus");
        this.mH.removeMessages(2);
        this.mH.sendEmptyMessage(2);
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        boolean z = (mode == 1 || mode == 3) ? false : true;
        boolean imWindowChanged = moveInputMethodWindowsIfNeededLocked(z);
        if (imWindowChanged) {
            displayContent.layoutNeeded = true;
            newFocus = computeFocusedWindowLocked();
        }
        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT || localLOGV) {
            Slog.v("WindowManager", "Changing focus from " + this.mCurrentFocus + " to " + newFocus + " Callers=" + Debug.getCallers(4));
        }
        WindowState oldFocus = this.mCurrentFocus;
        this.mCurrentFocus = newFocus;
        this.mLosingFocus.remove(newFocus);
        int focusChanged = this.mPolicy.focusChangedLw(oldFocus, newFocus);
        if (imWindowChanged && oldFocus != this.mInputMethodWindow) {
            if (mode == 2) {
                this.mWindowPlacerLocked.performLayoutLockedInner(displayContent, true, updateInputWindows);
                focusChanged &= -2;
            } else if (mode == 3) {
                this.mLayersController.assignLayersLocked(displayContent.getWindowList());
            }
        }
        if ((focusChanged & 1) != 0) {
            displayContent.layoutNeeded = true;
            if (mode == 2) {
                this.mWindowPlacerLocked.performLayoutLockedInner(displayContent, true, updateInputWindows);
            }
        }
        if (mode != 1) {
            this.mInputMonitor.setInputFocusLw(this.mCurrentFocus, updateInputWindows);
        }
        adjustForImeIfNeeded(displayContent);
        Trace.traceEnd(32L);
        return true;
    }

    private WindowState computeFocusedWindowLocked() {
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

    WindowState findFocusedWindowLocked(DisplayContent displayContent) {
        WindowList windows = displayContent.getWindowList();
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowState win = windows.get(i);
            if (localLOGV || WindowManagerDebugConfig.DEBUG_FOCUS) {
                Slog.v("WindowManager", "Looking for focus: " + i + " = " + win + ", flags=" + win.mAttrs.flags + ", canReceive=" + win.canReceiveKeys());
            }
            if (win.canReceiveKeys()) {
                if (MultiWindowManager.isSupported() && isStickyByMtk(win) && this.mFocusedApp != win.mAppToken) {
                    Slog.v(TAG, "[BMW] Skipping " + win.mAppToken + " because it belongs to stick stack");
                } else {
                    AppWindowToken wtoken = win.mAppToken;
                    if (wtoken == null || !(wtoken.removed || wtoken.sendingToBottom)) {
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
                                    if (this.mFocusedApp == token && token.windowsAreFocusable()) {
                                        if (localLOGV || WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                                            Slog.v("WindowManager", "findFocusedWindow: Reached focused app=" + this.mFocusedApp + " target=" + wtoken);
                                        }
                                        return null;
                                    }
                                    tokenNdx--;
                                }
                                if (tokenNdx >= 0) {
                                    break;
                                }
                            }
                        }
                        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                            Slog.v("WindowManager", "findFocusedWindow: Found new focus @ " + i + " = " + win);
                        }
                        return win;
                    }
                    if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                        Slog.v("WindowManager", "Skipping " + wtoken + " because " + (wtoken.removed ? "removed" : "sendingToBottom"));
                    }
                }
            }
        }
        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
            Slog.v("WindowManager", "findFocusedWindow: No focusable windows.");
        }
        return null;
    }

    private void startFreezingDisplayLocked(boolean inTransaction, int exitAnim, int enterAnim) {
        if (!this.mDisplayFrozen && this.mDisplayReady && this.mPolicy.isScreenOn()) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.d("WindowManager", "startFreezingDisplayLocked: inTransaction=" + inTransaction + " exitAnim=" + exitAnim + " enterAnim=" + enterAnim + " called by " + Debug.getCallers(8));
            }
            this.mScreenFrozenLock.acquire();
            this.mDisplayFrozen = true;
            this.mDisplayFreezeTime = SystemClock.elapsedRealtime();
            this.mLastFinishedFreezeSource = null;
            this.mInputMonitor.freezeInputDispatchingLw();
            this.mPolicy.setLastInputMethodWindowLw((WindowManagerPolicy.WindowState) null, (WindowManagerPolicy.WindowState) null);
            if (this.mAppTransition.isTransitionSet()) {
                this.mAppTransition.freeze();
            }
            if (PROFILE_ORIENTATION) {
                File file = new File("/data/system/frozen");
                Debug.startMethodTracing(file.toString(), 8388608);
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
                if (ws.isOnScreen() && (ws.mAttrs.flags & PackageManagerService.DumpState.DUMP_PREFERRED_XML) != 0) {
                    isSecure = true;
                    break;
                }
                i++;
            }
            displayContent.updateDisplayInfo();
            this.mAnimator.setScreenRotationAnimationLocked(displayId, new ScreenRotationAnimation(this.mContext, displayContent, this.mFxSession, inTransaction, this.mPolicy.isDefaultOrientationForced(), isSecure));
        }
    }

    void stopFreezingDisplayLocked() {
        if (this.mDisplayFrozen) {
            if (this.mWaitingForConfig || this.mAppsFreezingScreen > 0 || this.mWindowsFreezingScreen == 1 || this.mClientFreezingScreen || !this.mOpeningApps.isEmpty()) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.d("WindowManager", "stopFreezingDisplayLocked: Returning mWaitingForConfig=" + this.mWaitingForConfig + ", mAppsFreezingScreen=" + this.mAppsFreezingScreen + ", mWindowsFreezingScreen=" + this.mWindowsFreezingScreen + ", mClientFreezingScreen=" + this.mClientFreezingScreen + ", mOpeningApps.size()=" + this.mOpeningApps.size());
                    return;
                }
                return;
            }
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.d("WindowManager", "stopFreezingDisplayLocked: Unfreezing now");
            }
            this.mDisplayFrozen = false;
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
            boolean updateRotation = false;
            DisplayContent displayContent = getDefaultDisplayContentLocked();
            int displayId = displayContent.getDisplayId();
            ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(displayId);
            if (screenRotationAnimation == null || !screenRotationAnimation.hasScreenshot()) {
                if (screenRotationAnimation != null) {
                    screenRotationAnimation.kill();
                    this.mAnimator.setScreenRotationAnimationLocked(displayId, null);
                }
                updateRotation = true;
            } else {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.i("WindowManager", "**** Dismissing screen rotation animation");
                }
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                boolean isDimming = displayContent.isDimming();
                if (!this.mPolicy.validateRotationAnimationLw(this.mExitAnimId, this.mEnterAnimId, isDimming)) {
                    this.mEnterAnimId = 0;
                    this.mExitAnimId = 0;
                }
                if (screenRotationAnimation.dismiss(this.mFxSession, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, getTransitionAnimationScaleLocked(), displayInfo.logicalWidth, displayInfo.logicalHeight, this.mExitAnimId, this.mEnterAnimId)) {
                    scheduleAnimationLocked();
                } else {
                    screenRotationAnimation.kill();
                    this.mAnimator.setScreenRotationAnimationLocked(displayId, null);
                    updateRotation = true;
                }
            }
            this.mInputMonitor.thawInputDispatchingLw();
            boolean configChanged = updateOrientationFromAppTokensLocked(false);
            this.mH.removeMessages(15);
            this.mH.sendEmptyMessageDelayed(15, 2000L);
            this.mScreenFrozenLock.release();
            if (updateRotation) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.d("WindowManager", "Performing post-rotate rotation");
                }
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
                int val = Integer.parseInt(str);
                return val;
            } catch (Exception e) {
            }
        }
        if (defUnits == 0) {
            return defDps;
        }
        int val2 = (int) TypedValue.applyDimension(defUnits, defDps, dm);
        return val2;
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

    boolean updateStatusBarVisibilityLocked(int visibility) {
        if (this.mLastDispatchedSystemUiVisibility == visibility) {
            return false;
        }
        int globalDiff = (this.mLastDispatchedSystemUiVisibility ^ visibility) & 7 & (~visibility);
        this.mLastDispatchedSystemUiVisibility = visibility;
        this.mInputManager.setSystemUiVisibility(visibility);
        WindowList windows = getDefaultWindowListLocked();
        int N = windows.size();
        for (int i = 0; i < N; i++) {
            WindowState ws = windows.get(i);
            try {
                int curValue = ws.mSystemUiVisibility;
                int diff = (curValue ^ visibility) & globalDiff;
                int newValue = ((~diff) & curValue) | (visibility & diff);
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
        return true;
    }

    public void reevaluateStatusBarVisibility() {
        synchronized (this.mWindowMap) {
            int visibility = this.mPolicy.adjustSystemUiVisibilityLw(this.mLastStatusBarVisibility);
            if (updateStatusBarVisibilityLocked(visibility)) {
                this.mWindowPlacerLocked.requestTraversal();
            }
        }
    }

    private static final class HideNavInputConsumer extends InputConsumerImpl implements WindowManagerPolicy.InputConsumer {
        private final InputEventReceiver mInputEventReceiver;

        HideNavInputConsumer(WindowManagerService service, Looper looper, InputEventReceiver.Factory inputEventReceiverFactory) {
            super(service, "input consumer", null);
            this.mInputEventReceiver = inputEventReceiverFactory.createInputEventReceiver(this.mClientChannel, looper);
        }

        public void dismiss() {
            if (!this.mService.removeInputConsumer()) {
                return;
            }
            synchronized (this.mService.mWindowMap) {
                this.mInputEventReceiver.dispose();
                disposeChannelsLw();
            }
        }
    }

    public WindowManagerPolicy.InputConsumer addInputConsumer(Looper looper, InputEventReceiver.Factory inputEventReceiverFactory) {
        HideNavInputConsumer inputConsumerImpl;
        synchronized (this.mWindowMap) {
            inputConsumerImpl = new HideNavInputConsumer(this, looper, inputEventReceiverFactory);
            this.mInputConsumer = inputConsumerImpl;
            this.mInputMonitor.updateInputWindowsLw(true);
        }
        return inputConsumerImpl;
    }

    boolean removeInputConsumer() {
        synchronized (this.mWindowMap) {
            if (this.mInputConsumer != null) {
                this.mInputConsumer = null;
                this.mInputMonitor.updateInputWindowsLw(true);
                return true;
            }
            return false;
        }
    }

    public void createWallpaperInputConsumer(InputChannel inputChannel) {
        synchronized (this.mWindowMap) {
            this.mWallpaperInputConsumer = new InputConsumerImpl(this, "wallpaper input", inputChannel);
            this.mWallpaperInputConsumer.mWindowHandle.hasWallpaper = true;
            this.mInputMonitor.updateInputWindowsLw(true);
        }
    }

    public void removeWallpaperInputConsumer() {
        synchronized (this.mWindowMap) {
            if (this.mWallpaperInputConsumer != null) {
                this.mWallpaperInputConsumer.disposeChannelsLw();
                this.mWallpaperInputConsumer = null;
                this.mInputMonitor.updateInputWindowsLw(true);
            }
        }
    }

    public boolean hasNavigationBar() {
        return this.mPolicy.hasNavigationBar();
    }

    public void lockNow(Bundle options) {
        this.mPolicy.lockNow(options);
    }

    public void showRecentApps(boolean fromHome) {
        this.mPolicy.showRecentApps(fromHome);
    }

    public boolean isSafeModeEnabled() {
        return this.mSafeMode;
    }

    public boolean clearWindowContentFrameStats(IBinder token) {
        if (!checkCallingPermission("android.permission.FRAME_STATS", "clearWindowContentFrameStats()")) {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        synchronized (this.mWindowMap) {
            WindowState windowState = this.mWindowMap.get(token);
            if (windowState == null) {
                return false;
            }
            WindowSurfaceController surfaceController = windowState.mWinAnimator.mSurfaceController;
            if (surfaceController == null) {
                return false;
            }
            return surfaceController.clearWindowContentFrameStats();
        }
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
            WindowSurfaceController surfaceController = windowState.mWinAnimator.mSurfaceController;
            if (surfaceController == null) {
                return null;
            }
            if (this.mTempWindowRenderStats == null) {
                this.mTempWindowRenderStats = new WindowContentFrameStats();
            }
            WindowContentFrameStats stats = this.mTempWindowRenderStats;
            if (surfaceController.getWindowContentFrameStats(stats)) {
                return stats;
            }
            return null;
        }
    }

    public void notifyAppRelaunching(IBinder token) {
        synchronized (this.mWindowMap) {
            AppWindowToken appWindow = findAppWindowToken(token);
            if (appWindow != null) {
                appWindow.startRelaunching();
            }
        }
    }

    public void notifyAppRelaunchingFinished(IBinder token) {
        synchronized (this.mWindowMap) {
            AppWindowToken appWindow = findAppWindowToken(token);
            if (appWindow != null) {
                appWindow.finishRelaunching();
            }
        }
    }

    public void notifyAppRelaunchingCleared(IBinder token) {
        synchronized (this.mWindowMap) {
            AppWindowToken appWindow = findAppWindowToken(token);
            if (appWindow != null) {
                appWindow.clearRelaunching();
            }
        }
    }

    public int getDockedDividerInsetsLw() {
        return getDefaultDisplayContentLocked().getDockedDividerController().getContentInsets();
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
        if (!this.mTokenMap.isEmpty()) {
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
        this.mWallpaperControllerLocked.dumpTokens(pw, "  ", dumpAll);
        if (!this.mFinishedStarting.isEmpty()) {
            pw.println();
            pw.println("  Finishing start of application tokens:");
            for (int i = this.mFinishedStarting.size() - 1; i >= 0; i--) {
                WindowToken token2 = this.mFinishedStarting.get(i);
                pw.print("  Finished Starting #");
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
        if (this.mOpeningApps.isEmpty() && this.mClosingApps.isEmpty()) {
            return;
        }
        pw.println();
        if (this.mOpeningApps.size() > 0) {
            pw.print("  mOpeningApps=");
            pw.println(this.mOpeningApps);
        }
        if (this.mClosingApps.size() <= 0) {
            return;
        }
        pw.print("  mClosingApps=");
        pw.println(this.mClosingApps);
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
        pw.print("  mLastWakeLockHoldingWindow=");
        pw.print(this.mLastWakeLockHoldingWindow);
        pw.print(" mLastWakeLockObscuringWindow=");
        pw.print(this.mLastWakeLockObscuringWindow);
        pw.println();
        this.mInputMonitor.dump(pw, "  ");
        if (!dumpAll) {
            return;
        }
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
        this.mWindowPlacerLocked.dump(pw, "  ");
        this.mWallpaperControllerLocked.dump(pw, "  ");
        this.mLayersController.dump(pw, "  ");
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
        pw.print(" mSkipAppTransitionAnimation=");
        pw.println(this.mSkipAppTransitionAnimation);
        pw.println("  mLayoutToAnim:");
        this.mAppTransition.dump(pw, "    ");
    }

    boolean dumpWindows(PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        HashMap<IBinder, WindowState> map;
        WindowList windows = new WindowList();
        if ("apps".equals(name) || "visible".equals(name) || "visible-apps".equals(name)) {
            boolean appsOnly = name.contains("apps");
            boolean visibleOnly = name.contains("visible");
            map = this.mWindowMap;
            synchronized (map) {
                if (appsOnly) {
                    dumpDisplayContentsLocked(pw, true);
                }
                int numDisplays = this.mDisplayContents.size();
                for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                    WindowList windowList = this.mDisplayContents.valueAt(displayNdx).getWindowList();
                    for (int winNdx = windowList.size() - 1; winNdx >= 0; winNdx--) {
                        WindowState w = windowList.get(winNdx);
                        if ((!visibleOnly || w.mWinAnimator.getShown()) && (!appsOnly || w.mAppToken != null)) {
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
            map = this.mWindowMap;
            synchronized (map) {
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
            fastPrintWriter.println("  Window at fault: " + windowState.mAttrs.getTitle());
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
                    pw.println("    i[input]: input subsystem state");
                    pw.println("    p[policy]: policy state");
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
                    pw.println("    \"visible-apps\" for the visible app windows.");
                    pw.println("  -a: include all available server state.");
                    return;
                }
                if ("-d".equals(opt)) {
                    runDebug(pw, args, opti);
                    return;
                }
                pw.println("Unknown argument: " + opt + "; use -h for help");
            }
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        pw.println("Dump time : " + df.format(new Date()));
        if (opti >= args.length) {
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
                WindowSurfaceController.SurfaceTrace.dumpAllSurfaces(pw, dumpAll ? "-------------------------------------------------------------------------------" : null);
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
            return;
        }
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
                WindowSurfaceController.SurfaceTrace.dumpAllSurfaces(pw, null);
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
        } else if ("all".equals(cmd) || "a".equals(cmd)) {
            synchronized (this.mWindowMap) {
                dumpWindowsLocked(pw, true, null);
            }
        } else {
            if (dumpWindows(pw, cmd, args, opti2, dumpAll)) {
                return;
            }
            pw.println("Bad window command, or no windows match: " + cmd);
            pw.println("Use -h for help.");
        }
    }

    private void runDebug(PrintWriter pw, String[] args, int opti) {
        int mode;
        String cmd = "help";
        if (opti < args.length) {
            cmd = args[opti];
            opti++;
        }
        if ("help".equals(cmd)) {
            mode = 0;
            pw.println("Window manager debug options:");
            pw.println("  -d enable <zone zone ...> : enable the debug zone");
            pw.println("  -d disable <zone zone ...> : disable the debug zone");
            pw.println("zone may be some of:");
            pw.println("  a[all]");
        } else if ("enable".equals(cmd)) {
            mode = 1;
        } else {
            if (!"disable".equals(cmd)) {
                pw.println("Unknown debug argument: " + cmd + "; use \"-d help\" for help");
                return;
            }
            mode = 2;
        }
        boolean setAll = false;
        Field[] fields = WindowManagerDebugConfig.class.getDeclaredFields();
        Field[] fieldsPolicy = PhoneWindowManager.class.getDeclaredFields();
        while (!setAll) {
            if (mode != 0 && opti >= args.length) {
                return;
            }
            if (opti < args.length) {
                cmd = args[opti];
                opti++;
            }
            setAll = (mode == 0 || "all".equals(cmd)) ? true : "a".equals(cmd);
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                if (name != null && (name.contains("DEBUG") || name.contains("SHOW") || name.equals("localLOGV"))) {
                    if (!setAll) {
                        try {
                            if (name.equals(cmd)) {
                                if (mode != 0) {
                                    fields[i].setBoolean(null, mode == 1);
                                    if (name.equals("localLOGV")) {
                                        localLOGV = mode == 1;
                                    }
                                    int j = 0;
                                    while (true) {
                                        if (j >= fieldsPolicy.length) {
                                            break;
                                        }
                                        String pname = fieldsPolicy[j].getName();
                                        if (!pname.equals(name)) {
                                            j++;
                                        } else {
                                            fieldsPolicy[j].setAccessible(true);
                                            fieldsPolicy[j].setBoolean(null, mode == 1);
                                        }
                                    }
                                }
                                pw.println(String.format("  %s = %b", name, Boolean.valueOf(fields[i].getBoolean(null))));
                            }
                        } catch (IllegalAccessException e) {
                            Slog.e(TAG, name + " setBoolean failed", e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void monitor() {
        synchronized (this.mWindowMap) {
        }
    }

    private DisplayContent newDisplayContentLocked(Display display) {
        DisplayContent displayContent = new DisplayContent(display, this);
        int displayId = display.getDisplayId();
        if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
            Slog.v("WindowManager", "Adding display=" + display);
        }
        this.mDisplayContents.put(displayId, displayContent);
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        Rect rect = new Rect();
        this.mDisplaySettings.getOverscanLocked(displayInfo.name, displayInfo.uniqueId, rect);
        displayInfo.overscanLeft = rect.left;
        displayInfo.overscanTop = rect.top;
        displayInfo.overscanRight = rect.right;
        displayInfo.overscanBottom = rect.bottom;
        this.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(displayId, displayInfo);
        configureDisplayPolicyLocked(displayContent);
        if (displayId == 0) {
            displayContent.mTapDetector = new TaskTapPointerEventListener(this, displayContent);
            registerPointerEventListener(displayContent.mTapDetector);
            registerPointerEventListener(this.mMousePositionTracker);
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
            this.mWindowPlacerLocked.requestTraversal();
        }
    }

    public void onDisplayRemoved(int displayId) {
        Slog.v("WindowManager", "onDisplayRemoved id = " + displayId + " Callers=" + Debug.getCallers(3));
        this.mH.sendMessage(this.mH.obtainMessage(28, displayId, 0));
    }

    private void handleDisplayRemovedLocked(int displayId) {
        DisplayContent displayContent = getDisplayContentLocked(displayId);
        if (displayContent != null) {
            if (displayContent.isAnimating()) {
                displayContent.mDeferredRemoval = true;
                return;
            }
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                Slog.v("WindowManager", "Removing display=" + displayContent);
            }
            this.mDisplayContents.delete(displayId);
            displayContent.close();
            if (displayId == 0) {
                unregisterPointerEventListener(displayContent.mTapDetector);
                unregisterPointerEventListener(this.mMousePositionTracker);
            }
        }
        this.mAnimator.removeDisplayLocked(displayId);
        this.mWindowPlacerLocked.requestTraversal();
    }

    public void onDisplayChanged(int displayId) {
        this.mH.sendMessage(this.mH.obtainMessage(29, displayId, 0));
    }

    private void handleDisplayChangedLocked(int displayId) {
        DisplayContent displayContent = getDisplayContentLocked(displayId);
        if (displayContent != null) {
            displayContent.updateDisplayInfo();
        }
        this.mWindowPlacerLocked.requestTraversal();
    }

    public Object getWindowManagerLock() {
        return this.mWindowMap;
    }

    public void setReplacingWindow(IBinder token, boolean animate) {
        synchronized (this.mWindowMap) {
            AppWindowToken appWindowToken = findAppWindowToken(token);
            if (appWindowToken == null || !appWindowToken.isVisible()) {
                Slog.w("WindowManager", "Attempted to set replacing window on non-existing app token " + token);
            } else {
                appWindowToken.setReplacingWindows(animate);
            }
        }
    }

    public void setReplacingWindows(IBinder token, boolean childrenOnly) {
        synchronized (this.mWindowMap) {
            AppWindowToken appWindowToken = findAppWindowToken(token);
            if (appWindowToken == null || !appWindowToken.isVisible()) {
                Slog.w("WindowManager", "Attempted to set replacing window on non-existing app token " + token);
                return;
            }
            if (childrenOnly) {
                appWindowToken.setReplacingChildren();
            } else {
                appWindowToken.setReplacingWindows(false);
            }
            scheduleClearReplacingWindowIfNeeded(token, true);
        }
    }

    public void scheduleClearReplacingWindowIfNeeded(IBinder token, boolean replacing) {
        synchronized (this.mWindowMap) {
            AppWindowToken appWindowToken = findAppWindowToken(token);
            if (appWindowToken == null) {
                Slog.w("WindowManager", "Attempted to reset replacing window on non-existing app token " + token);
                return;
            }
            if (replacing) {
                scheduleReplacingWindowTimeouts(appWindowToken);
            } else {
                appWindowToken.resetReplacingWindows();
            }
        }
    }

    void scheduleReplacingWindowTimeouts(AppWindowToken appWindowToken) {
        if (!this.mReplacingWindowTimeouts.contains(appWindowToken)) {
            this.mReplacingWindowTimeouts.add(appWindowToken);
        }
        this.mH.removeMessages(46);
        this.mH.sendEmptyMessageDelayed(46, 2000L);
    }

    public int getDockedStackSide() {
        int dockSide;
        synchronized (this.mWindowMap) {
            TaskStack dockedStack = getDefaultDisplayContentLocked().getDockedStackVisibleForUserLocked();
            dockSide = dockedStack == null ? -1 : dockedStack.getDockSide();
        }
        return dockSide;
    }

    public void setDockedStackResizing(boolean resizing) {
        synchronized (this.mWindowMap) {
            getDefaultDisplayContentLocked().getDockedDividerController().setResizing(resizing);
            requestTraversal();
        }
    }

    public void setDockedStackDividerTouchRegion(Rect touchRegion) {
        synchronized (this.mWindowMap) {
            getDefaultDisplayContentLocked().getDockedDividerController().setTouchRegion(touchRegion);
            setFocusTaskRegionLocked();
        }
    }

    public void setResizeDimLayer(boolean visible, int targetStackId, float alpha) {
        synchronized (this.mWindowMap) {
            getDefaultDisplayContentLocked().getDockedDividerController().setResizeDimLayer(visible, targetStackId, alpha);
        }
    }

    public void animateResizePinnedStack(final Rect bounds, final int animationDuration) {
        synchronized (this.mWindowMap) {
            final TaskStack stack = this.mStackIdToStack.get(4);
            if (stack == null) {
                Slog.w(TAG, "animateResizePinnedStack: stackId 4 not found.");
                return;
            }
            final Rect originalBounds = new Rect();
            stack.getBounds(originalBounds);
            UiThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    WindowManagerService.this.mBoundsAnimationController.animateBounds(stack, originalBounds, bounds, animationDuration);
                }
            });
        }
    }

    public void setTaskResizeable(int taskId, int resizeMode) {
        synchronized (this.mWindowMap) {
            Task task = this.mTaskIdToTask.get(taskId);
            if (task != null) {
                task.setResizeable(resizeMode);
            }
        }
    }

    public void setForceResizableTasks(boolean forceResizableTasks) {
        synchronized (this.mWindowMap) {
            this.mForceResizableTasks = forceResizableTasks;
        }
    }

    static int dipToPixel(int dip, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(1, dip, displayMetrics);
    }

    public void registerDockedStackListener(IDockedStackListener listener) {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerDockedStackListener()")) {
            return;
        }
        getDefaultDisplayContentLocked().mDividerControllerLocked.registerDockedStackListener(listener);
    }

    public void requestAppKeyboardShortcuts(IResultReceiver receiver, int deviceId) {
        try {
            WindowState focusedWindow = getFocusedWindow();
            if (focusedWindow == null || focusedWindow.mClient == null) {
                return;
            }
            getFocusedWindow().mClient.requestAppKeyboardShortcuts(receiver, deviceId);
        } catch (RemoteException e) {
        }
    }

    public void getStableInsets(Rect outInsets) throws RemoteException {
        synchronized (this.mWindowMap) {
            getStableInsetsLocked(outInsets);
        }
    }

    void getStableInsetsLocked(Rect outInsets) {
        DisplayInfo di = getDefaultDisplayInfoLocked();
        this.mPolicy.getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, outInsets);
    }

    private void getNonDecorInsetsLocked(Rect outInsets) {
        DisplayInfo di = getDefaultDisplayInfoLocked();
        this.mPolicy.getNonDecorInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, outInsets);
    }

    public void subtractStableInsets(Rect inOutBounds) {
        synchronized (this.mWindowMap) {
            getStableInsetsLocked(this.mTmpRect2);
            DisplayInfo di = getDefaultDisplayInfoLocked();
            this.mTmpRect.set(0, 0, di.logicalWidth, di.logicalHeight);
            subtractInsets(this.mTmpRect, this.mTmpRect2, inOutBounds);
        }
    }

    public void subtractNonDecorInsets(Rect inOutBounds) {
        synchronized (this.mWindowMap) {
            getNonDecorInsetsLocked(this.mTmpRect2);
            DisplayInfo di = getDefaultDisplayInfoLocked();
            this.mTmpRect.set(0, 0, di.logicalWidth, di.logicalHeight);
            subtractInsets(this.mTmpRect, this.mTmpRect2, inOutBounds);
        }
    }

    void subtractInsets(Rect display, Rect insets, Rect inOutBounds) {
        this.mTmpRect3.set(display);
        this.mTmpRect3.inset(insets);
        inOutBounds.intersect(this.mTmpRect3);
    }

    public int getSmallestWidthForTaskBounds(Rect bounds) {
        int smallestWidthDpForBounds;
        synchronized (this.mWindowMap) {
            smallestWidthDpForBounds = getDefaultDisplayContentLocked().getDockedDividerController().getSmallestWidthDpForBounds(bounds);
        }
        return smallestWidthDpForBounds;
    }

    private static class MousePositionTracker implements WindowManagerPolicy.PointerEventListener {
        private boolean mLatestEventWasMouse;
        private float mLatestMouseX;
        private float mLatestMouseY;

        MousePositionTracker(MousePositionTracker mousePositionTracker) {
            this();
        }

        private MousePositionTracker() {
        }

        void updatePosition(float x, float y) {
            synchronized (this) {
                this.mLatestEventWasMouse = true;
                this.mLatestMouseX = x;
                this.mLatestMouseY = y;
            }
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (motionEvent.isFromSource(8194)) {
                updatePosition(motionEvent.getRawX(), motionEvent.getRawY());
            } else {
                synchronized (this) {
                    this.mLatestEventWasMouse = false;
                }
            }
        }
    }

    void updatePointerIcon(IWindow client) {
        synchronized (this.mMousePositionTracker) {
            if (!this.mMousePositionTracker.mLatestEventWasMouse) {
                return;
            }
            float mouseX = this.mMousePositionTracker.mLatestMouseX;
            float mouseY = this.mMousePositionTracker.mLatestMouseY;
            synchronized (this.mWindowMap) {
                if (this.mDragState != null) {
                    return;
                }
                WindowState callingWin = windowForClientLocked((Session) null, client, false);
                if (callingWin == null) {
                    Slog.w("WindowManager", "Bad requesting window " + client);
                    return;
                }
                DisplayContent displayContent = callingWin.getDisplayContent();
                if (displayContent == null) {
                    return;
                }
                WindowState windowUnderPointer = displayContent.getTouchableWinAtPointLocked(mouseX, mouseY);
                if (windowUnderPointer != callingWin) {
                    return;
                }
                try {
                    windowUnderPointer.mClient.updatePointerIcon(windowUnderPointer.translateToWindowX(mouseX), windowUnderPointer.translateToWindowY(mouseY));
                } catch (RemoteException e) {
                    Slog.w("WindowManager", "unable to update pointer icon");
                }
            }
        }
    }

    void restorePointerIconLocked(DisplayContent displayContent, float latestX, float latestY) {
        this.mMousePositionTracker.updatePosition(latestX, latestY);
        WindowState windowUnderPointer = displayContent.getTouchableWinAtPointLocked(latestX, latestY);
        if (windowUnderPointer != null) {
            try {
                windowUnderPointer.mClient.updatePointerIcon(windowUnderPointer.translateToWindowX(latestX), windowUnderPointer.translateToWindowY(latestY));
                return;
            } catch (RemoteException e) {
                Slog.w("WindowManager", "unable to restore pointer icon");
                return;
            }
        }
        InputManager.getInstance().setPointerIconType(1000);
    }

    public void registerShortcutKey(long shortcutCode, IShortcutService shortcutKeyReceiver) throws RemoteException {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerShortcutKey")) {
            throw new SecurityException("Requires REGISTER_WINDOW_MANAGER_LISTENERS permission");
        }
        this.mPolicy.registerShortcutKey(shortcutCode, shortcutKeyReceiver);
    }

    private final class LocalService extends WindowManagerInternal {
        LocalService(WindowManagerService this$0, LocalService localService) {
            this();
        }

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
            if (Binder.getCallingPid() == Process.myPid()) {
                return;
            }
            spec.recycle();
        }

        public void getMagnificationRegion(Region magnificationRegion) {
            synchronized (WindowManagerService.this.mWindowMap) {
                if (WindowManagerService.this.mAccessibilityController != null) {
                    WindowManagerService.this.mAccessibilityController.getMagnificationRegionLocked(magnificationRegion);
                } else {
                    throw new IllegalStateException("Magnification callbacks not set!");
                }
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
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowState windowState = WindowManagerService.this.getFocusedWindowLocked();
                if (windowState == null) {
                    return null;
                }
                return windowState.mClient.asBinder();
            }
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
            boolean allWindowsDrawn = false;
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowManagerService.this.mWaitingForDrawnCallback = callback;
                WindowList windows = WindowManagerService.this.getDefaultWindowListLocked();
                for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                    WindowState win = windows.get(winNdx);
                    boolean isForceHiding = WindowManagerService.this.mPolicy.isForceHiding(win.mAttrs);
                    if (win.isVisibleLw() && (win.mAppToken != null || isForceHiding)) {
                        win.mWinAnimator.mDrawState = 1;
                        win.mLastContentInsets.set(-1, -1, -1, -1);
                        WindowManagerService.this.mWaitingForDrawn.add(win);
                        if (isForceHiding) {
                            break;
                        }
                    }
                }
                WindowManagerService.this.mWindowPlacerLocked.requestTraversal();
                WindowManagerService.this.mH.removeMessages(24);
                if (WindowManagerService.this.mWaitingForDrawn.isEmpty()) {
                    allWindowsDrawn = true;
                } else {
                    WindowManagerService.this.mH.sendEmptyMessageDelayed(24, timeout);
                    WindowManagerService.this.checkDrawnWindowsLocked();
                }
            }
            if (!allWindowsDrawn) {
                return;
            }
            callback.run();
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

        public void registerAppTransitionListener(WindowManagerInternal.AppTransitionListener listener) {
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowManagerService.this.mAppTransition.registerListenerLocked(listener);
            }
        }

        public int getInputMethodWindowVisibleHeight() {
            int inputMethodWindowVisibleHeightLw;
            synchronized (WindowManagerService.this.mWindowMap) {
                inputMethodWindowVisibleHeightLw = WindowManagerService.this.mPolicy.getInputMethodWindowVisibleHeightLw();
            }
            return inputMethodWindowVisibleHeightLw;
        }

        public void saveLastInputMethodWindowForTransition() {
            synchronized (WindowManagerService.this.mWindowMap) {
                if (WindowManagerService.this.mInputMethodWindow != null) {
                    WindowManagerService.this.mPolicy.setLastInputMethodWindowLw(WindowManagerService.this.mInputMethodWindow, WindowManagerService.this.mInputMethodTarget);
                }
            }
        }

        public void clearLastInputMethodWindowForTransition() {
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowManagerService.this.mPolicy.setLastInputMethodWindowLw((WindowManagerPolicy.WindowState) null, (WindowManagerPolicy.WindowState) null);
            }
        }

        public boolean isHardKeyboardAvailable() {
            boolean z;
            synchronized (WindowManagerService.this.mWindowMap) {
                z = WindowManagerService.this.mHardKeyboardAvailable;
            }
            return z;
        }

        public void setOnHardKeyboardStatusChangeListener(WindowManagerInternal.OnHardKeyboardStatusChangeListener listener) {
            synchronized (WindowManagerService.this.mWindowMap) {
                WindowManagerService.this.mHardKeyboardStatusChangeListener = listener;
            }
        }

        public boolean isStackVisible(int stackId) {
            boolean zIsStackVisibleLocked;
            synchronized (WindowManagerService.this.mWindowMap) {
                zIsStackVisibleLocked = WindowManagerService.this.isStackVisibleLocked(stackId);
            }
            return zIsStackVisibleLocked;
        }

        public boolean isDockedDividerResizing() {
            boolean zIsResizing;
            synchronized (WindowManagerService.this.mWindowMap) {
                zIsResizing = WindowManagerService.this.getDefaultDisplayContentLocked().getDockedDividerController().isResizing();
            }
            return zIsResizing;
        }
    }

    boolean isAlarmBoot() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        return bootReason != null && bootReason.equals("1");
    }

    private void closeSystemDialogs() {
        WindowList windows = getDefaultWindowListLocked();
        synchronized (this.mWindowMap) {
            for (int i = windows.size() - 1; i >= 0; i--) {
                WindowState w = windows.get(i);
                if (w.mHasSurface && (w.mAttrs.type == 2008 || w.mAttrs.type == 2010 || w.mAttrs.type == 2003 || w.mAttrs.type == 2009)) {
                    removeWindow(w.mSession, w.mClient);
                }
            }
        }
    }

    public boolean isCacheStartingWindow() {
        return true;
    }

    public boolean isCacheFirstFrame() {
        return false;
    }

    public boolean isCacheLastFrame() {
        return false;
    }

    public boolean isFastStartingWindowSupport() {
        if (this.mDisableFastStartingWindow) {
            return false;
        }
        return this.mFastStartingWindowSupport;
    }

    public boolean hasBitmapByToken(IBinder token) {
        return getBitmapByToken(token) != null;
    }

    public Bitmap getBitmapByToken(IBinder token) {
        if (getRotation() == 0) {
            if (token != null) {
                try {
                    ComponentName activityName = this.mActivityManager.getActivityClassForToken(token);
                    String string = activityName == null ? null : activityName.toString();
                    if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                        Slog.v(TAG, "getBitmapByToken ok token =" + token + ", name = " + string);
                    }
                    if (string != null) {
                        return this.mBitmaps.get(string);
                    }
                } catch (RemoteException e) {
                    Slog.d(TAG, "getBitmapByToken failed", e);
                }
            } else if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v(TAG, "getBitmapByToken null " + token);
            }
        } else if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            Slog.v(TAG, "getBitmapByToken rot null " + token);
        }
        return null;
    }

    public void setBitmapByToken(IBinder token, Bitmap bitmap) {
        if (getRotation() == 0) {
            if (token != null && bitmap != null) {
                try {
                    ComponentName activityName = this.mActivityManager.getActivityClassForToken(token);
                    String string = activityName == null ? null : activityName.toString();
                    if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                        Slog.v(TAG, "setBitmapByToken ok, token =" + token + ", name = " + string);
                    }
                    if (string == null) {
                        return;
                    }
                    this.mBitmaps.put(string, bitmap);
                    return;
                } catch (RemoteException e) {
                    Slog.d(TAG, "setBitmapByToken failed", e);
                    return;
                }
            }
            if (!WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                return;
            }
            Slog.v(TAG, "setBitmapByToken null " + token);
            return;
        }
        if (!WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            return;
        }
        Slog.v(TAG, "setBitmapByToken rot null " + token);
    }

    public void cacheStartingWindow(AppWindowToken appToken) {
        Message m = this.mH.obtainMessage(55, appToken);
        this.mH.removeMessages(55);
        this.mH.sendMessage(m);
    }

    public void setRotationBoost(boolean enable) {
        if (this.mPerfService == null) {
            this.mPerfService = new PerfServiceWrapper((Context) null);
        }
        if (enable && !this.mIsPerfBoostEnable) {
            this.mPerfService.boostEnable(5);
        } else if (!enable && this.mIsPerfBoostEnable) {
            this.mPerfService.boostDisable(5);
        }
        this.mIsPerfBoostEnable = enable;
    }

    public void stickWindow(int stackId, int taskId, boolean isSticky) {
        synchronized (this.mWindowMap) {
            if (MultiWindowManager.DEBUG) {
                Slog.d("WindowManager", "stickWindow, stackId = " + stackId + ", taskId = " + taskId + ", isSticky = " + isSticky);
            }
            Task task = this.mTaskIdToTask.get(taskId);
            if (task == null) {
                if (MultiWindowManager.DEBUG) {
                    Slog.d("WindowManager", "positionTaskInStack: could not find taskId=" + taskId);
                }
                return;
            }
            TaskStack stack = this.mStackIdToStack.get(stackId);
            if (stack == null) {
                if (MultiWindowManager.DEBUG) {
                    Slog.d("WindowManager", "positionTaskInStack: could not find stackId=" + stackId);
                }
            } else {
                task.mSticky = isSticky;
            }
        }
    }

    private boolean isStickyByMtk(WindowState win) {
        if (win == null || win.mAppToken == null || win.getStack() == null || win.getTask() == null || win.getStack().mStackId != 2) {
            return false;
        }
        return win.getTask().mSticky;
    }

    public void registerFreeformStackListener(IFreeformStackListener listener) {
        this.mFreeformStackListeners.register(listener);
    }

    void showOrHideRestoreButton(WindowState win) {
        boolean isShown = win != null && win.mFrame != null && win.mFrame.left == 0 && win.mFrame.top == 0 && win.mFrame.width() == this.mDisplayMetrics.widthPixels && win.getTask() != null && win.getTask().isResizeable() && win.getStack().mStackId == 1;
        if (isShown == this.mIsRestoreButtonVisible) {
            return;
        }
        this.mIsRestoreButtonVisible = isShown;
        try {
            int size = this.mFreeformStackListeners.beginBroadcast();
            for (int i = 0; i < size; i++) {
                IFreeformStackListener listener = this.mFreeformStackListeners.getBroadcastItem(i);
                try {
                    listener.onShowRestoreButtonChanged(isShown);
                } catch (RemoteException e) {
                    Slog.e("WindowManager", "Error delivering show restore button changed event.", e);
                }
            }
            this.mFreeformStackListeners.finishBroadcast();
        } catch (Exception e2) {
            Slog.e("WindowManager", "Error delivering show restore button changed event.", e2);
        }
    }

    void updateNonSystemOverlayWindowsVisibilityIfNeeded(WindowState win, boolean surfaceShown) {
        if (!win.hideNonSystemOverlayWindowsWhenVisible() && !this.mHidingNonSystemOverlayWindows.contains(win)) {
            return;
        }
        boolean systemAlertWindowsHidden = !this.mHidingNonSystemOverlayWindows.isEmpty();
        if (surfaceShown) {
            if (!this.mHidingNonSystemOverlayWindows.contains(win)) {
                this.mHidingNonSystemOverlayWindows.add(win);
            }
        } else {
            this.mHidingNonSystemOverlayWindows.remove(win);
        }
        boolean hideSystemAlertWindows = !this.mHidingNonSystemOverlayWindows.isEmpty();
        if (systemAlertWindowsHidden == hideSystemAlertWindows) {
            return;
        }
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
