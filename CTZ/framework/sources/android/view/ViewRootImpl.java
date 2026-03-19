package android.view;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.ResourcesManager;
import android.bluetooth.BluetoothClass;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.TtmlUtils;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.telecom.Logging.Session;
import android.util.AndroidRuntimeException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LongArray;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Choreographer;
import android.view.DisplayCutout;
import android.view.IWindow;
import android.view.InputDevice;
import android.view.InputQueue;
import android.view.KeyCharacterMap;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.IResultReceiver;
import com.android.internal.os.SomeArgs;
import com.android.internal.policy.PhoneFallbackEventHandler;
import com.android.internal.util.Preconditions;
import com.android.internal.view.BaseSurfaceHolder;
import com.android.internal.view.RootViewSurfaceTaker;
import com.android.internal.view.SurfaceCallbackHelper;
import com.mediatek.perfframe.PerfFrameInfoFactory;
import com.mediatek.perfframe.PerfFrameInfoManager;
import com.mediatek.view.ViewDebugManager;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

public final class ViewRootImpl implements ViewParent, View.AttachInfo.Callbacks, ThreadedRenderer.DrawCallbacks {
    private static final int MAX_QUEUED_INPUT_EVENT_POOL_SIZE = 10;
    static final int MAX_TRACKBALL_DELAY = 250;
    private static final int MSG_CHECK_FOCUS = 13;
    private static final int MSG_CLEAR_ACCESSIBILITY_FOCUS_HOST = 21;
    private static final int MSG_CLOSE_SYSTEM_DIALOGS = 14;
    private static final int MSG_DIE = 3;
    private static final int MSG_DISPATCH_APP_VISIBILITY = 8;
    private static final int MSG_DISPATCH_DRAG_EVENT = 15;
    private static final int MSG_DISPATCH_DRAG_LOCATION_EVENT = 16;
    private static final int MSG_DISPATCH_GET_NEW_SURFACE = 9;
    private static final int MSG_DISPATCH_INPUT_EVENT = 7;
    private static final int MSG_DISPATCH_KEY_FROM_AUTOFILL = 12;
    private static final int MSG_DISPATCH_KEY_FROM_IME = 11;
    private static final int MSG_DISPATCH_SYSTEM_UI_VISIBILITY = 17;
    private static final int MSG_DISPATCH_WINDOW_SHOWN = 25;
    private static final int MSG_DRAW_FINISHED = 29;
    private static final int MSG_INVALIDATE = 1;
    private static final int MSG_INVALIDATE_RECT = 2;
    private static final int MSG_INVALIDATE_WORLD = 22;
    private static final int MSG_POINTER_CAPTURE_CHANGED = 28;
    private static final int MSG_PROCESS_INPUT_EVENTS = 19;
    private static final int MSG_REQUEST_KEYBOARD_SHORTCUTS = 26;
    private static final int MSG_RESIZED = 4;
    private static final int MSG_RESIZED_REPORT = 5;
    private static final int MSG_SYNTHESIZE_INPUT_EVENT = 24;
    private static final int MSG_UPDATE_CONFIGURATION = 18;
    private static final int MSG_UPDATE_POINTER_ICON = 27;
    private static final int MSG_WINDOW_FOCUS_CHANGED = 6;
    private static final int MSG_WINDOW_MOVED = 23;
    private static final boolean MT_RENDERER_AVAILABLE = true;
    public static final String PROPERTY_EMULATOR_WIN_OUTSET_BOTTOM_PX = "ro.emu.win_outset_bottom_px";
    private static final String PROPERTY_PROFILE_RENDERING = "viewroot.profile_rendering";
    private static final String TAG = "ViewRootImpl";
    private static boolean sAlwaysAssignFocus;
    View mAccessibilityFocusedHost;
    AccessibilityNodeInfo mAccessibilityFocusedVirtualView;
    AccessibilityInteractionController mAccessibilityInteractionController;
    final AccessibilityManager mAccessibilityManager;
    private ActivityConfigCallback mActivityConfigCallback;
    private boolean mActivityRelaunched;
    boolean mAdded;
    boolean mAddedTouchMode;
    private boolean mAppVisibilityChanged;
    boolean mApplyInsetsRequested;
    final View.AttachInfo mAttachInfo;
    AudioManager mAudioManager;
    final String mBasePackageName;
    private int mCanvasOffsetX;
    private int mCanvasOffsetY;
    Choreographer mChoreographer;
    int mClientWindowLayoutFlags;
    final ConsumeBatchedInputImmediatelyRunnable mConsumeBatchedInputImmediatelyRunnable;
    boolean mConsumeBatchedInputImmediatelyScheduled;
    boolean mConsumeBatchedInputScheduled;
    final ConsumeBatchedInputRunnable mConsumedBatchedInputRunnable;
    final Context mContext;
    int mCurScrollY;
    View mCurrentDragView;
    private final int mDensity;
    Rect mDirty;
    Display mDisplay;
    private final DisplayManager.DisplayListener mDisplayListener;
    final DisplayManager mDisplayManager;
    ClipDescription mDragDescription;
    private boolean mDragResizing;
    boolean mDrawingAllowed;
    int mDrawsNeededToReport;
    FallbackEventHandler mFallbackEventHandler;
    boolean mFirst;
    InputStage mFirstInputStage;
    InputStage mFirstPostImeInputStage;
    private boolean mForceNextConfigUpdate;
    boolean mForceNextWindowRelayout;
    private int mFpsNumFrames;
    public int mFrame;
    boolean mFullRedrawNeeded;
    boolean mHadWindowFocus;
    final ViewRootHandler mHandler;
    int mHardwareXOffset;
    int mHardwareYOffset;
    boolean mHasHadWindowFocus;
    int mHeight;
    final HighContrastTextManager mHighContrastTextManager;
    public long mIdent;
    InputChannel mInputChannel;
    protected final InputEventConsistencyVerifier mInputEventConsistencyVerifier;
    WindowInputEventReceiver mInputEventReceiver;
    InputQueue mInputQueue;
    InputQueue.Callback mInputQueueCallback;
    final InvalidateOnAnimationRunnable mInvalidateOnAnimationRunnable;
    private boolean mInvalidateRootRequested;
    public boolean mIsAnimating;
    boolean mIsCreating;
    boolean mIsDrawing;
    boolean mIsInTraversal;
    boolean mLastOverscanRequested;
    WeakReference<View> mLastScrolledFocus;
    int mLastSystemUiVisibility;
    int mLastTouchSource;
    boolean mLastWasImTarget;
    private WindowInsets mLastWindowInsets;
    boolean mLayoutRequested;
    volatile Object mLocalDragState;
    final WindowLeaked mLocation;
    boolean mLostWindowFocus;
    private boolean mNeedsRendererSetup;
    boolean mNewSurfaceNeeded;
    private ThreadedRenderer.FrameDrawingCallback mNextRtFrameCallback;
    private final int mNoncompatDensity;
    boolean mPendingAlwaysConsumeNavBar;
    int mPendingInputEventCount;
    QueuedInputEvent mPendingInputEventHead;
    QueuedInputEvent mPendingInputEventTail;
    private ArrayList<LayoutTransition> mPendingTransitions;
    boolean mPointerCapture;
    final Region mPreviousTransparentRegion;
    boolean mProcessInputEventsScheduled;
    private boolean mProfile;
    private boolean mProfileRendering;
    private QueuedInputEvent mQueuedInputEventPool;
    private int mQueuedInputEventPoolSize;
    private boolean mRemoved;
    private Choreographer.FrameCallback mRenderProfiler;
    private boolean mRenderProfilingEnabled;
    boolean mReportNextDraw;
    private int mResizeMode;
    boolean mScrollMayChange;
    int mScrollY;
    Scroller mScroller;
    SendWindowContentChangedAccessibilityEvent mSendWindowContentChangedAccessibilityEvent;
    int mSeq;
    int mSoftInputMode;
    BaseSurfaceHolder mSurfaceHolder;
    SurfaceHolder.Callback2 mSurfaceHolderCallback;
    InputStage mSyntheticInputStage;
    private String mTag;
    final int mTargetSdkVersion;
    HashSet<View> mTempHashSet;
    final Rect mTempRect;
    final Thread mThread;
    CompatibilityInfo.Translator mTranslator;
    final Region mTransparentRegion;
    int mTraversalBarrier;
    final TraversalRunnable mTraversalRunnable;
    public boolean mTraversalScheduled;
    boolean mUnbufferedInputDispatch;

    @GuardedBy("this")
    boolean mUpcomingInTouchMode;

    @GuardedBy("this")
    boolean mUpcomingWindowFocus;
    private boolean mUseMTRenderer;
    View mView;
    final ViewConfiguration mViewConfiguration;
    private int mViewLayoutDirectionInitial;
    int mViewVisibility;
    final Rect mVisRect;
    int mWidth;
    boolean mWillDrawSoon;
    final Rect mWinFrame;
    final W mWindow;
    CountDownLatch mWindowDrawCountDown;

    @GuardedBy("this")
    boolean mWindowFocusChanged;
    final IWindowSession mWindowSession;
    private final ArrayList<WindowStoppedCallback> mWindowStoppedCallbacks;
    public static boolean DBG = false;
    public static boolean LOCAL_LOGV = false;
    public static boolean DEBUG_DRAW = LOCAL_LOGV;
    public static boolean DEBUG_LAYOUT = LOCAL_LOGV;
    public static boolean DEBUG_DIALOG = LOCAL_LOGV;
    public static boolean DEBUG_INPUT_RESIZE = LOCAL_LOGV;
    public static boolean DEBUG_ORIENTATION = LOCAL_LOGV;
    public static boolean DEBUG_TRACKBALL = LOCAL_LOGV;
    public static boolean DEBUG_IMF = LOCAL_LOGV;
    public static boolean DEBUG_CONFIGURATION = LOCAL_LOGV;
    public static boolean DEBUG_FPS = false;
    public static boolean DEBUG_INPUT_STAGES = LOCAL_LOGV;
    public static boolean DEBUG_KEEP_SCREEN_ON = LOCAL_LOGV;
    private static PerfFrameInfoManager mPerfFrameInfoManager = PerfFrameInfoFactory.getInstance().makePerfFrameInfoManager();
    static final ThreadLocal<HandlerActionQueue> sRunQueues = new ThreadLocal<>();
    static final ArrayList<Runnable> sFirstDrawHandlers = new ArrayList<>();
    static boolean sFirstDrawComplete = false;
    private static final ArrayList<ConfigChangedCallback> sConfigCallbacks = new ArrayList<>();
    private static boolean sCompatibilityDone = false;
    static final Interpolator mResizeInterpolator = new AccelerateDecelerateInterpolator();

    @GuardedBy("mWindowCallbacks")
    final ArrayList<WindowCallbacks> mWindowCallbacks = new ArrayList<>();
    final int[] mTmpLocation = new int[2];
    final TypedValue mTmpValue = new TypedValue();
    public final WindowManager.LayoutParams mWindowAttributes = new WindowManager.LayoutParams();
    boolean mAppVisible = true;
    private boolean mForceDecorViewVisibility = false;
    int mOrigWindowType = -1;
    boolean mStopped = false;
    boolean mIsAmbientMode = false;
    boolean mPausedForTransition = false;
    boolean mLastInCompatMode = false;
    String mPendingInputEventQueueLengthCounterName = "pq";
    private final UnhandledKeyManager mUnhandledKeyManager = new UnhandledKeyManager();
    boolean mWindowAttributesChanged = false;
    int mWindowAttributesChangesFlag = 0;
    public final Surface mSurface = new Surface();
    final Rect mPendingOverscanInsets = new Rect();
    final Rect mPendingVisibleInsets = new Rect();
    final Rect mPendingStableInsets = new Rect();
    final Rect mPendingContentInsets = new Rect();
    final Rect mPendingOutsets = new Rect();
    final Rect mPendingBackDropFrame = new Rect();
    final DisplayCutout.ParcelableWrapper mPendingDisplayCutout = new DisplayCutout.ParcelableWrapper(DisplayCutout.NO_CUTOUT);
    final ViewTreeObserver.InternalInsetsInfo mLastGivenInsets = new ViewTreeObserver.InternalInsetsInfo();
    final Rect mDispatchContentInsets = new Rect();
    final Rect mDispatchStableInsets = new Rect();
    DisplayCutout mDispatchDisplayCutout = DisplayCutout.NO_CUTOUT;
    private final Configuration mLastConfigurationFromResources = new Configuration();
    private final MergedConfiguration mLastReportedMergedConfiguration = new MergedConfiguration();
    private final MergedConfiguration mPendingMergedConfiguration = new MergedConfiguration();
    final PointF mDragPoint = new PointF();
    final PointF mLastTouchPoint = new PointF();
    private long mFpsStartTime = -1;
    private long mFpsPrevTime = -1;
    private int mPointerIconType = 1;
    private PointerIcon mCustomPointerIcon = null;
    final AccessibilityInteractionConnectionManager mAccessibilityInteractionConnectionManager = new AccessibilityInteractionConnectionManager();
    private boolean mInLayout = false;
    ArrayList<View> mLayoutRequesters = new ArrayList<>();
    boolean mHandlingLayoutInLayoutRequest = false;

    public interface ActivityConfigCallback {
        void onConfigurationChanged(Configuration configuration, int i);
    }

    public interface ConfigChangedCallback {
        void onConfigurationChanged(Configuration configuration);
    }

    interface WindowStoppedCallback {
        void windowStopped(boolean z);
    }

    static final class SystemUiVisibilityInfo {
        int globalVisibility;
        int localChanges;
        int localValue;
        int seq;

        SystemUiVisibilityInfo() {
        }
    }

    public ViewRootImpl(Context context, Display display) {
        this.mInputEventConsistencyVerifier = InputEventConsistencyVerifier.isInstrumentationEnabled() ? new InputEventConsistencyVerifier(this, 0) : null;
        this.mTag = TAG;
        this.mProfile = false;
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayChanged(int i) {
                int i2;
                int state;
                if (ViewRootImpl.this.mView != null && ViewRootImpl.this.mDisplay.getDisplayId() == i && (i2 = ViewRootImpl.this.mAttachInfo.mDisplayState) != (state = ViewRootImpl.this.mDisplay.getState())) {
                    ViewRootImpl.this.mAttachInfo.mDisplayState = state;
                    ViewRootImpl.this.pokeDrawLockIfNeeded();
                    if (i2 != 0) {
                        int viewScreenState = toViewScreenState(i2);
                        int viewScreenState2 = toViewScreenState(state);
                        if (viewScreenState != viewScreenState2) {
                            ViewRootImpl.this.mView.dispatchScreenStateChanged(viewScreenState2);
                        }
                        if (i2 == 1) {
                            ViewRootImpl.this.mFullRedrawNeeded = true;
                            ViewRootImpl.this.scheduleTraversals();
                        }
                    }
                }
            }

            @Override
            public void onDisplayRemoved(int i) {
            }

            @Override
            public void onDisplayAdded(int i) {
            }

            private int toViewScreenState(int i) {
                if (i != 1) {
                    return 1;
                }
                return 0;
            }
        };
        this.mWindowStoppedCallbacks = new ArrayList<>();
        this.mDrawsNeededToReport = 0;
        this.mHandler = new ViewRootHandler();
        this.mTraversalRunnable = new TraversalRunnable();
        this.mConsumedBatchedInputRunnable = new ConsumeBatchedInputRunnable();
        this.mConsumeBatchedInputImmediatelyRunnable = new ConsumeBatchedInputImmediatelyRunnable();
        this.mInvalidateOnAnimationRunnable = new InvalidateOnAnimationRunnable();
        this.mFrame = 0;
        this.mContext = context;
        this.mWindowSession = WindowManagerGlobal.getWindowSession();
        this.mDisplay = display;
        this.mBasePackageName = context.getBasePackageName();
        this.mThread = Thread.currentThread();
        this.mLocation = new WindowLeaked(null);
        this.mLocation.fillInStackTrace();
        this.mWidth = -1;
        this.mHeight = -1;
        this.mDirty = new Rect();
        this.mTempRect = new Rect();
        this.mVisRect = new Rect();
        this.mWinFrame = new Rect();
        this.mWindow = new W(this);
        this.mTargetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        this.mViewVisibility = 8;
        this.mTransparentRegion = new Region();
        this.mPreviousTransparentRegion = new Region();
        this.mFirst = true;
        this.mAdded = false;
        this.mAttachInfo = new View.AttachInfo(this.mWindowSession, this.mWindow, display, this, this.mHandler, this, context);
        this.mAccessibilityManager = AccessibilityManager.getInstance(context);
        this.mAccessibilityManager.addAccessibilityStateChangeListener(this.mAccessibilityInteractionConnectionManager, this.mHandler);
        this.mHighContrastTextManager = new HighContrastTextManager();
        this.mAccessibilityManager.addHighTextContrastStateChangeListener(this.mHighContrastTextManager, this.mHandler);
        this.mViewConfiguration = ViewConfiguration.get(context);
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
        this.mNoncompatDensity = context.getResources().getDisplayMetrics().noncompatDensityDpi;
        this.mFallbackEventHandler = new PhoneFallbackEventHandler(context);
        this.mChoreographer = Choreographer.getInstance();
        this.mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (!sCompatibilityDone) {
            sAlwaysAssignFocus = this.mTargetSdkVersion < 28;
            sCompatibilityDone = true;
        }
        loadSystemProperties();
        ViewDebugManager.getInstance().debugViewRootConstruct(this.mTag, context, this.mThread, this.mChoreographer, this.mTraversalRunnable, this);
    }

    public static void addFirstDrawHandler(Runnable runnable) {
        synchronized (sFirstDrawHandlers) {
            if (!sFirstDrawComplete) {
                sFirstDrawHandlers.add(runnable);
            }
        }
    }

    public static void addConfigCallback(ConfigChangedCallback configChangedCallback) {
        synchronized (sConfigCallbacks) {
            sConfigCallbacks.add(configChangedCallback);
        }
    }

    public void setActivityConfigCallback(ActivityConfigCallback activityConfigCallback) {
        this.mActivityConfigCallback = activityConfigCallback;
    }

    public void addWindowCallbacks(WindowCallbacks windowCallbacks) {
        synchronized (this.mWindowCallbacks) {
            this.mWindowCallbacks.add(windowCallbacks);
        }
    }

    public void removeWindowCallbacks(WindowCallbacks windowCallbacks) {
        synchronized (this.mWindowCallbacks) {
            this.mWindowCallbacks.remove(windowCallbacks);
        }
    }

    public void reportDrawFinish() {
        if (this.mWindowDrawCountDown != null) {
            this.mWindowDrawCountDown.countDown();
        }
    }

    public void profile() {
        this.mProfile = true;
    }

    static boolean isInTouchMode() {
        IWindowSession iWindowSessionPeekWindowSession = WindowManagerGlobal.peekWindowSession();
        if (iWindowSessionPeekWindowSession != null) {
            try {
                return iWindowSessionPeekWindowSession.getInTouchMode();
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }

    public void notifyChildRebuilt() {
        if (this.mView instanceof RootViewSurfaceTaker) {
            if (this.mSurfaceHolderCallback != null) {
                this.mSurfaceHolder.removeCallback(this.mSurfaceHolderCallback);
            }
            this.mSurfaceHolderCallback = ((RootViewSurfaceTaker) this.mView).willYouTakeTheSurface();
            if (this.mSurfaceHolderCallback != null) {
                this.mSurfaceHolder = new TakenSurfaceHolder();
                this.mSurfaceHolder.setFormat(0);
                this.mSurfaceHolder.addCallback(this.mSurfaceHolderCallback);
            } else {
                this.mSurfaceHolder = null;
            }
            this.mInputQueueCallback = ((RootViewSurfaceTaker) this.mView).willYouTakeTheInputQueue();
            if (this.mInputQueueCallback != null) {
                this.mInputQueueCallback.onInputQueueCreated(this.mInputQueue);
            }
        }
    }

    public void setView(View view, WindowManager.LayoutParams layoutParams, View view2) {
        boolean z;
        synchronized (this) {
            if (this.mView == null) {
                this.mView = view;
                this.mAttachInfo.mDisplayState = this.mDisplay.getState();
                this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
                this.mViewLayoutDirectionInitial = this.mView.getRawLayoutDirection();
                this.mFallbackEventHandler.setView(view);
                this.mWindowAttributes.copyFrom(layoutParams);
                if (this.mWindowAttributes.packageName == null) {
                    this.mWindowAttributes.packageName = this.mBasePackageName;
                }
                WindowManager.LayoutParams layoutParams2 = this.mWindowAttributes;
                setTag();
                if (DEBUG_KEEP_SCREEN_ON && (this.mClientWindowLayoutFlags & 128) != 0 && (layoutParams2.flags & 128) == 0) {
                    Slog.d(this.mTag, "setView: FLAG_KEEP_SCREEN_ON changed from true to false!");
                }
                this.mClientWindowLayoutFlags = layoutParams2.flags;
                setAccessibilityFocus(null, null);
                if (view instanceof RootViewSurfaceTaker) {
                    this.mSurfaceHolderCallback = ((RootViewSurfaceTaker) view).willYouTakeTheSurface();
                    if (this.mSurfaceHolderCallback != null) {
                        this.mSurfaceHolder = new TakenSurfaceHolder();
                        this.mSurfaceHolder.setFormat(0);
                        this.mSurfaceHolder.addCallback(this.mSurfaceHolderCallback);
                    }
                }
                if (!layoutParams2.hasManualSurfaceInsets) {
                    layoutParams2.setSurfaceInsets(view, false, true);
                }
                CompatibilityInfo compatibilityInfo = this.mDisplay.getDisplayAdjustments().getCompatibilityInfo();
                this.mTranslator = compatibilityInfo.getTranslator();
                if (this.mSurfaceHolder == null) {
                    enableHardwareAcceleration(layoutParams2);
                    boolean z2 = this.mAttachInfo.mThreadedRenderer != null;
                    if (this.mUseMTRenderer != z2) {
                        endDragResizing();
                        this.mUseMTRenderer = z2;
                    }
                }
                if (this.mTranslator != null) {
                    this.mSurface.setCompatibilityTranslator(this.mTranslator);
                    layoutParams2.backup();
                    this.mTranslator.translateWindowLayout(layoutParams2);
                    z = true;
                } else {
                    z = false;
                }
                if (DEBUG_LAYOUT || ViewDebugManager.DEBUG_LIFECYCLE) {
                    Log.d(this.mTag, "WindowLayout in setView:" + layoutParams2 + ",mView = " + this.mView + ",compatibilityInfo = " + compatibilityInfo + ", this = " + this);
                }
                if (!compatibilityInfo.supportsScreen()) {
                    layoutParams2.privateFlags |= 128;
                    this.mLastInCompatMode = true;
                }
                this.mSoftInputMode = layoutParams2.softInputMode;
                this.mWindowAttributesChanged = true;
                this.mWindowAttributesChangesFlag = -1;
                this.mAttachInfo.mRootView = view;
                this.mAttachInfo.mScalingRequired = this.mTranslator != null;
                this.mAttachInfo.mApplicationScale = this.mTranslator == null ? 1.0f : this.mTranslator.applicationScale;
                if (view2 != null) {
                    this.mAttachInfo.mPanelParentWindowToken = view2.getApplicationWindowToken();
                }
                this.mAdded = true;
                requestLayout();
                this.mInputChannel = new InputChannel();
                this.mForceDecorViewVisibility = (this.mWindowAttributes.privateFlags & 16384) != 0;
                try {
                    try {
                        this.mOrigWindowType = this.mWindowAttributes.type;
                        this.mAttachInfo.mRecomputeGlobalAttributes = true;
                        collectViewAttributes();
                        int iAddToDisplay = this.mWindowSession.addToDisplay(this.mWindow, this.mSeq, this.mWindowAttributes, getHostVisibility(), this.mDisplay.getDisplayId(), this.mWinFrame, this.mAttachInfo.mContentInsets, this.mAttachInfo.mStableInsets, this.mAttachInfo.mOutsets, this.mAttachInfo.mDisplayCutout, this.mInputChannel);
                        if (this.mTranslator != null) {
                            this.mTranslator.translateRectInScreenToAppWindow(this.mAttachInfo.mContentInsets);
                        }
                        this.mPendingOverscanInsets.set(0, 0, 0, 0);
                        this.mPendingContentInsets.set(this.mAttachInfo.mContentInsets);
                        this.mPendingStableInsets.set(this.mAttachInfo.mStableInsets);
                        this.mPendingDisplayCutout.set(this.mAttachInfo.mDisplayCutout);
                        this.mPendingVisibleInsets.set(0, 0, 0, 0);
                        this.mAttachInfo.mAlwaysConsumeNavBar = (iAddToDisplay & 4) != 0;
                        this.mPendingAlwaysConsumeNavBar = this.mAttachInfo.mAlwaysConsumeNavBar;
                        if (DEBUG_LAYOUT) {
                            Log.v(this.mTag, "Added window " + this.mWindow + ", mPendingContentInsets = " + this.mPendingContentInsets + ", mPendingStableInsets = " + this.mPendingStableInsets);
                        }
                        if (iAddToDisplay < 0) {
                            this.mAttachInfo.mRootView = null;
                            this.mAdded = false;
                            this.mFallbackEventHandler.setView(null);
                            unscheduleTraversals();
                            setAccessibilityFocus(null, null);
                            switch (iAddToDisplay) {
                                case -10:
                                    throw new WindowManager.InvalidDisplayException("Unable to add window " + this.mWindow + " -- the specified window type " + this.mWindowAttributes.type + " is not valid");
                                case -9:
                                    throw new WindowManager.InvalidDisplayException("Unable to add window " + this.mWindow + " -- the specified display can not be found");
                                case -8:
                                    throw new WindowManager.BadTokenException("Unable to add window " + this.mWindow + " -- permission denied for window type " + this.mWindowAttributes.type);
                                case -7:
                                    throw new WindowManager.BadTokenException("Unable to add window " + this.mWindow + " -- another window of type " + this.mWindowAttributes.type + " already exists");
                                case -6:
                                    return;
                                case -5:
                                    throw new WindowManager.BadTokenException("Unable to add window -- window " + this.mWindow + " has already been added");
                                case -4:
                                    throw new WindowManager.BadTokenException("Unable to add window -- app for token " + layoutParams2.token + " is exiting");
                                case -3:
                                    throw new WindowManager.BadTokenException("Unable to add window -- token " + layoutParams2.token + " is not for an application");
                                case -2:
                                case -1:
                                    throw new WindowManager.BadTokenException("Unable to add window -- token " + layoutParams2.token + " is not valid; is your activity running?");
                                default:
                                    throw new RuntimeException("Unable to add window -- unknown error code " + iAddToDisplay);
                            }
                        }
                        if (view instanceof RootViewSurfaceTaker) {
                            this.mInputQueueCallback = ((RootViewSurfaceTaker) view).willYouTakeTheInputQueue();
                        }
                        if (this.mInputChannel.isValid()) {
                            if (this.mInputQueueCallback != null) {
                                this.mInputQueue = new InputQueue();
                                this.mInputQueueCallback.onInputQueueCreated(this.mInputQueue);
                            }
                            this.mInputEventReceiver = new WindowInputEventReceiver(this.mInputChannel, Looper.myLooper());
                        }
                        view.assignParent(this);
                        this.mAddedTouchMode = (iAddToDisplay & 1) != 0;
                        this.mAppVisible = (iAddToDisplay & 2) != 0;
                        if (this.mAccessibilityManager.isEnabled()) {
                            this.mAccessibilityInteractionConnectionManager.ensureConnection();
                        }
                        if (view.getImportantForAccessibility() == 0) {
                            view.setImportantForAccessibility(1);
                        }
                        CharSequence title = layoutParams2.getTitle();
                        this.mSyntheticInputStage = new SyntheticInputStage();
                        EarlyPostImeInputStage earlyPostImeInputStage = new EarlyPostImeInputStage(new NativePostImeInputStage(new ViewPostImeInputStage(this.mSyntheticInputStage), "aq:native-post-ime:" + ((Object) title)));
                        this.mFirstInputStage = new NativePreImeInputStage(new ViewPreImeInputStage(new ImeInputStage(earlyPostImeInputStage, "aq:ime:" + ((Object) title))), "aq:native-pre-ime:" + ((Object) title));
                        this.mFirstPostImeInputStage = earlyPostImeInputStage;
                        this.mPendingInputEventQueueLengthCounterName = "aq:pending:" + ((Object) title);
                    } catch (RemoteException e) {
                        this.mAdded = false;
                        this.mView = null;
                        this.mAttachInfo.mRootView = null;
                        this.mInputChannel = null;
                        this.mFallbackEventHandler.setView(null);
                        unscheduleTraversals();
                        setAccessibilityFocus(null, null);
                        throw new RuntimeException("Adding window failed", e);
                    }
                } finally {
                    if (z) {
                        layoutParams2.restore();
                    }
                }
            }
        }
    }

    private void setTag() {
        String[] strArrSplit = this.mWindowAttributes.getTitle().toString().split("\\.");
        if (strArrSplit.length > 0) {
            this.mTag = "ViewRootImpl[" + strArrSplit[strArrSplit.length - 1] + "]";
        }
    }

    private boolean isInLocalFocusMode() {
        return (this.mWindowAttributes.flags & 268435456) != 0;
    }

    public int getWindowFlags() {
        return this.mWindowAttributes.flags;
    }

    public int getDisplayId() {
        return this.mDisplay.getDisplayId();
    }

    public CharSequence getTitle() {
        return this.mWindowAttributes.getTitle();
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    void destroyHardwareResources() {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.destroyHardwareResources(this.mView);
            this.mAttachInfo.mThreadedRenderer.destroy();
        }
    }

    public void detachFunctor(long j) {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.stopDrawing();
        }
    }

    public static void invokeFunctor(long j, boolean z) {
        ThreadedRenderer.invokeFunctor(j, z);
    }

    public void registerAnimatingRenderNode(RenderNode renderNode) {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.registerAnimatingRenderNode(renderNode);
            return;
        }
        if (this.mAttachInfo.mPendingAnimatingRenderNodes == null) {
            this.mAttachInfo.mPendingAnimatingRenderNodes = new ArrayList();
        }
        this.mAttachInfo.mPendingAnimatingRenderNodes.add(renderNode);
    }

    public void registerVectorDrawableAnimator(AnimatedVectorDrawable.VectorDrawableAnimatorRT vectorDrawableAnimatorRT) {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.registerVectorDrawableAnimator(vectorDrawableAnimatorRT);
        }
    }

    public void registerRtFrameCallback(ThreadedRenderer.FrameDrawingCallback frameDrawingCallback) {
        this.mNextRtFrameCallback = frameDrawingCallback;
    }

    private void enableHardwareAcceleration(WindowManager.LayoutParams layoutParams) {
        boolean z = false;
        this.mAttachInfo.mHardwareAccelerated = false;
        this.mAttachInfo.mHardwareAccelerationRequested = false;
        if (this.mTranslator != null) {
            return;
        }
        if (!ViewDebugManager.getInstance().debugForceHWDraw((layoutParams.flags & 16777216) != 0) || !ThreadedRenderer.isAvailable()) {
            return;
        }
        boolean z2 = (layoutParams.privateFlags & 1) != 0;
        boolean z3 = (layoutParams.privateFlags & 2) != 0;
        if (z2) {
            this.mAttachInfo.mHardwareAccelerationRequested = true;
        } else if (!ThreadedRenderer.sRendererDisabled || (ThreadedRenderer.sSystemRendererDisabled && z3)) {
            if (this.mAttachInfo.mThreadedRenderer != null) {
                this.mAttachInfo.mThreadedRenderer.destroy();
            }
            Rect rect = layoutParams.surfaceInsets;
            boolean z4 = layoutParams.format != -1 || (rect.left != 0 || rect.right != 0 || rect.top != 0 || rect.bottom != 0);
            if (this.mContext.getResources().getConfiguration().isScreenWideColorGamut() && layoutParams.getColorMode() == 1) {
                z = true;
            }
            this.mAttachInfo.mThreadedRenderer = ThreadedRenderer.create(this.mContext, z4, layoutParams.getTitle().toString());
            this.mAttachInfo.mThreadedRenderer.setWideGamut(z);
            if (this.mAttachInfo.mThreadedRenderer != null) {
                View.AttachInfo attachInfo = this.mAttachInfo;
                this.mAttachInfo.mHardwareAccelerationRequested = true;
                attachInfo.mHardwareAccelerated = true;
            }
        }
        if (ViewDebugManager.DEBUG_USER) {
            Log.d(this.mTag, "hardware acceleration = " + this.mAttachInfo.mHardwareAccelerated + " , fakeHwAccelerated = " + z2 + ", sRendererDisabled = " + ThreadedRenderer.sRendererDisabled + ", forceHwAccelerated = " + z3 + ", sSystemRendererDisabled = " + ThreadedRenderer.sSystemRendererDisabled);
        }
    }

    public View getView() {
        return this.mView;
    }

    final WindowLeaked getLocation() {
        return this.mLocation;
    }

    void setLayoutParams(WindowManager.LayoutParams layoutParams, boolean z) {
        synchronized (this) {
            int i = this.mWindowAttributes.surfaceInsets.left;
            int i2 = this.mWindowAttributes.surfaceInsets.top;
            int i3 = this.mWindowAttributes.surfaceInsets.right;
            int i4 = this.mWindowAttributes.surfaceInsets.bottom;
            int i5 = this.mWindowAttributes.softInputMode;
            boolean z2 = this.mWindowAttributes.hasManualSurfaceInsets;
            if (DEBUG_KEEP_SCREEN_ON && (this.mClientWindowLayoutFlags & 128) != 0 && (layoutParams.flags & 128) == 0) {
                Slog.d(this.mTag, "setLayoutParams: FLAG_KEEP_SCREEN_ON from true to false!");
            }
            this.mClientWindowLayoutFlags = layoutParams.flags;
            int i6 = this.mWindowAttributes.privateFlags & 128;
            layoutParams.systemUiVisibility = this.mWindowAttributes.systemUiVisibility;
            layoutParams.subtreeSystemUiVisibility = this.mWindowAttributes.subtreeSystemUiVisibility;
            this.mWindowAttributesChangesFlag = this.mWindowAttributes.copyFrom(layoutParams);
            if ((this.mWindowAttributesChangesFlag & 524288) != 0) {
                this.mAttachInfo.mRecomputeGlobalAttributes = true;
            }
            if ((this.mWindowAttributesChangesFlag & 1) != 0) {
                this.mAttachInfo.mNeedsUpdateLightCenter = true;
            }
            if (this.mWindowAttributes.packageName == null) {
                this.mWindowAttributes.packageName = this.mBasePackageName;
            }
            WindowManager.LayoutParams layoutParams2 = this.mWindowAttributes;
            layoutParams2.privateFlags = i6 | layoutParams2.privateFlags;
            if (this.mWindowAttributes.preservePreviousSurfaceInsets) {
                this.mWindowAttributes.surfaceInsets.set(i, i2, i3, i4);
                this.mWindowAttributes.hasManualSurfaceInsets = z2;
            } else if (this.mWindowAttributes.surfaceInsets.left != i || this.mWindowAttributes.surfaceInsets.top != i2 || this.mWindowAttributes.surfaceInsets.right != i3 || this.mWindowAttributes.surfaceInsets.bottom != i4) {
                this.mNeedsRendererSetup = true;
            }
            applyKeepScreenOnFlag(this.mWindowAttributes);
            if (z) {
                this.mSoftInputMode = layoutParams.softInputMode;
                requestLayout();
            }
            if ((layoutParams.softInputMode & 240) == 0) {
                this.mWindowAttributes.softInputMode = (this.mWindowAttributes.softInputMode & (-241)) | (i5 & 240);
            }
            this.mWindowAttributesChanged = true;
            scheduleTraversals();
        }
        if (DEBUG_IMF) {
            Log.d(this.mTag, "setLayoutParams: attrs = " + layoutParams + ", mSoftInputMode = " + this.mSoftInputMode + ", mWindowAttributes = " + this.mWindowAttributes + ", this = " + this);
        }
    }

    void handleAppVisibility(boolean z) {
        if (DEBUG_LAYOUT) {
            Log.d(this.mTag, "handleAppVisibility: visible=" + z + ", mAppVisible=" + this.mAppVisible + ", this = " + this);
        }
        if (this.mAppVisible != z) {
            this.mAppVisible = z;
            this.mAppVisibilityChanged = true;
            scheduleTraversals();
            if (!this.mAppVisible) {
                WindowManagerGlobal.trimForeground();
            }
        }
    }

    void handleGetNewSurface() {
        this.mNewSurfaceNeeded = true;
        this.mFullRedrawNeeded = true;
        scheduleTraversals();
    }

    public void onMovedToDisplay(int i, Configuration configuration) {
        if (this.mDisplay.getDisplayId() == i) {
            return;
        }
        this.mDisplay = ResourcesManager.getInstance().getAdjustedDisplay(i, this.mView.getResources());
        this.mAttachInfo.mDisplayState = this.mDisplay.getState();
        this.mView.dispatchMovedToDisplay(this.mDisplay, configuration);
    }

    void pokeDrawLockIfNeeded() {
        int i = this.mAttachInfo.mDisplayState;
        if (this.mView != null && this.mAdded && this.mTraversalScheduled) {
            if (i == 3 || i == 4) {
                try {
                    this.mWindowSession.pokeDrawLock(this.mWindow);
                } catch (RemoteException e) {
                }
            }
        }
    }

    @Override
    public void requestFitSystemWindows() {
        checkThread();
        this.mApplyInsetsRequested = true;
        scheduleTraversals();
    }

    @Override
    public void requestLayout() {
        if (!this.mHandlingLayoutInLayoutRequest) {
            if (ViewDebugManager.DEBUG_REQUESTLAYOUT) {
                Log.d(this.mTag, "requestLayout: mView = " + this.mView + ", this = " + this, new Throwable("requestLayout"));
            }
            checkThread();
            this.mLayoutRequested = true;
            scheduleTraversals();
        }
    }

    @Override
    public boolean isLayoutRequested() {
        return this.mLayoutRequested;
    }

    @Override
    public void onDescendantInvalidated(View view, View view2) {
        if ((view2.mPrivateFlags & 64) != 0) {
            this.mIsAnimating = true;
        }
        invalidate();
    }

    void invalidate() {
        this.mDirty.set(0, 0, this.mWidth, this.mHeight);
        if (!this.mWillDrawSoon) {
            scheduleTraversals();
        }
    }

    void invalidateWorld(View view) {
        view.invalidate();
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                invalidateWorld(viewGroup.getChildAt(i));
            }
        }
    }

    @Override
    public void invalidateChild(View view, Rect rect) {
        invalidateChildInParent(null, rect);
    }

    @Override
    public ViewParent invalidateChildInParent(int[] iArr, Rect rect) {
        checkThread();
        if (DEBUG_DRAW) {
            Log.v(this.mTag, "Invalidate child: " + rect);
        }
        if (rect == null) {
            invalidate();
            return null;
        }
        if (rect.isEmpty() && !this.mIsAnimating) {
            return null;
        }
        if (this.mCurScrollY != 0 || this.mTranslator != null) {
            this.mTempRect.set(rect);
            rect = this.mTempRect;
            if (this.mCurScrollY != 0) {
                rect.offset(0, -this.mCurScrollY);
            }
            if (this.mTranslator != null) {
                this.mTranslator.translateRectInAppWindowToScreen(rect);
            }
            if (this.mAttachInfo.mScalingRequired) {
                rect.inset(-1, -1);
            }
        }
        invalidateRectOnScreen(rect);
        return null;
    }

    private void invalidateRectOnScreen(Rect rect) {
        Rect rect2 = this.mDirty;
        if (!rect2.isEmpty() && !rect2.contains(rect)) {
            this.mAttachInfo.mSetIgnoreDirtyState = true;
            this.mAttachInfo.mIgnoreDirtyState = true;
        }
        rect2.union(rect.left, rect.top, rect.right, rect.bottom);
        float f = this.mAttachInfo.mApplicationScale;
        boolean zIntersect = rect2.intersect(0, 0, (int) ((this.mWidth * f) + 0.5f), (int) ((this.mHeight * f) + 0.5f));
        if (!zIntersect) {
            rect2.setEmpty();
        }
        if (!this.mWillDrawSoon && (zIntersect || this.mIsAnimating)) {
            scheduleTraversals();
            return;
        }
        if (DEBUG_DRAW) {
            Log.v(this.mTag, "Invalidate child: Do not scheduleTraversals, mWillDrawSoon =" + this.mWillDrawSoon + ", intersected =" + zIntersect + ", mIsAnimating =" + this.mIsAnimating);
        }
    }

    public void setIsAmbientMode(boolean z) {
        this.mIsAmbientMode = z;
    }

    void addWindowStoppedCallback(WindowStoppedCallback windowStoppedCallback) {
        this.mWindowStoppedCallbacks.add(windowStoppedCallback);
    }

    void removeWindowStoppedCallback(WindowStoppedCallback windowStoppedCallback) {
        this.mWindowStoppedCallbacks.remove(windowStoppedCallback);
    }

    void setWindowStopped(boolean z) {
        if (this.mStopped != z) {
            this.mStopped = z;
            ThreadedRenderer threadedRenderer = this.mAttachInfo.mThreadedRenderer;
            if (threadedRenderer != null) {
                if (DEBUG_DRAW) {
                    Log.d(this.mTag, "WindowStopped on " + ((Object) getTitle()) + " set to " + this.mStopped);
                }
                threadedRenderer.setStopped(this.mStopped);
            }
            if (!this.mStopped) {
                this.mNewSurfaceNeeded = true;
                scheduleTraversals();
            } else if (threadedRenderer != null) {
                threadedRenderer.destroyHardwareResources(this.mView);
            }
            for (int i = 0; i < this.mWindowStoppedCallbacks.size(); i++) {
                this.mWindowStoppedCallbacks.get(i).windowStopped(z);
            }
            if (this.mStopped) {
                this.mSurface.release();
            }
        }
    }

    public void setPausedForTransition(boolean z) {
        this.mPausedForTransition = z;
    }

    @Override
    public ViewParent getParent() {
        return null;
    }

    @Override
    public boolean getChildVisibleRect(View view, Rect rect, Point point) {
        if (view != this.mView) {
            throw new RuntimeException("child is not mine, honest!");
        }
        return rect.intersect(0, 0, this.mWidth, this.mHeight);
    }

    @Override
    public void bringChildToFront(View view) {
    }

    int getHostVisibility() {
        if (this.mAppVisible || this.mForceDecorViewVisibility) {
            return this.mView.getVisibility();
        }
        return 8;
    }

    public void requestTransitionStart(LayoutTransition layoutTransition) {
        if (this.mPendingTransitions == null || !this.mPendingTransitions.contains(layoutTransition)) {
            if (this.mPendingTransitions == null) {
                this.mPendingTransitions = new ArrayList<>();
            }
            this.mPendingTransitions.add(layoutTransition);
        }
    }

    void notifyRendererOfFramePending() {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.notifyFramePending();
        }
    }

    void scheduleTraversals() {
        if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
            Trace.traceBegin(8L, "scheduleTraversals In");
        }
        if (!this.mTraversalScheduled) {
            this.mTraversalScheduled = true;
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Trace.traceBegin(8L, "scheduleTraversals occurred");
            }
            this.mTraversalBarrier = this.mHandler.getLooper().getQueue().postSyncBarrier();
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Log.v(this.mTag, "scheduleTraversals: mTraversalBarrier = " + this.mTraversalBarrier + ",this = " + this, new Throwable("scheduleTraversals"));
            }
            this.mChoreographer.postCallback(2, this.mTraversalRunnable, null);
            if (!this.mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            notifyRendererOfFramePending();
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Trace.traceEnd(8L);
            }
            pokeDrawLockIfNeeded();
        }
        if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
            Trace.traceEnd(8L);
        }
    }

    void unscheduleTraversals() {
        if (this.mTraversalScheduled) {
            this.mTraversalScheduled = false;
            this.mHandler.getLooper().getQueue().removeSyncBarrier(this.mTraversalBarrier);
            this.mChoreographer.removeCallbacks(2, this.mTraversalRunnable, null);
        }
    }

    void doTraversal() {
        if (ViewDebugManager.DEBUG_LIFECYCLE || ViewDebugManager.DEBUG_ENG) {
            Log.v(this.mTag, "doTraversal: mTraversalScheduled = " + this.mTraversalScheduled + " mFisrt = " + this.mFirst + ",mTraversalBarrier = " + this.mTraversalBarrier + ",this = " + this);
        }
        if (this.mTraversalScheduled) {
            this.mTraversalScheduled = false;
            this.mHandler.getLooper().getQueue().removeSyncBarrier(this.mTraversalBarrier);
            if (this.mProfile) {
                Debug.startMethodTracing("ViewAncestor");
            }
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Trace.traceBegin(8L, "doTraversal");
            }
            performTraversals();
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Trace.traceEnd(8L);
            }
            if (this.mProfile) {
                Debug.stopMethodTracing();
                this.mProfile = false;
            }
        }
    }

    private void applyKeepScreenOnFlag(WindowManager.LayoutParams layoutParams) {
        if (this.mAttachInfo.mKeepScreenOn) {
            layoutParams.flags |= 128;
        } else {
            layoutParams.flags = (layoutParams.flags & (-129)) | (this.mClientWindowLayoutFlags & 128);
        }
    }

    private boolean collectViewAttributes() {
        if (this.mAttachInfo.mRecomputeGlobalAttributes) {
            this.mAttachInfo.mRecomputeGlobalAttributes = false;
            boolean z = this.mAttachInfo.mKeepScreenOn;
            this.mAttachInfo.mKeepScreenOn = false;
            this.mAttachInfo.mSystemUiVisibility = 0;
            this.mAttachInfo.mHasSystemUiListeners = false;
            this.mView.dispatchCollectViewAttributes(this.mAttachInfo, 0);
            this.mAttachInfo.mSystemUiVisibility &= ~this.mAttachInfo.mDisabledSystemUiVisibility;
            WindowManager.LayoutParams layoutParams = this.mWindowAttributes;
            this.mAttachInfo.mSystemUiVisibility |= getImpliedSystemUiVisibility(layoutParams);
            if (this.mAttachInfo.mKeepScreenOn != z || this.mAttachInfo.mSystemUiVisibility != layoutParams.subtreeSystemUiVisibility || this.mAttachInfo.mHasSystemUiListeners != layoutParams.hasSystemUiListeners) {
                applyKeepScreenOnFlag(layoutParams);
                layoutParams.subtreeSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                layoutParams.hasSystemUiListeners = this.mAttachInfo.mHasSystemUiListeners;
                this.mView.dispatchWindowSystemUiVisiblityChanged(this.mAttachInfo.mSystemUiVisibility);
                return true;
            }
        }
        return false;
    }

    private int getImpliedSystemUiVisibility(WindowManager.LayoutParams layoutParams) {
        int i;
        if ((layoutParams.flags & 67108864) != 0) {
            i = 1280;
        } else {
            i = 0;
        }
        if ((layoutParams.flags & 134217728) != 0) {
            return i | 768;
        }
        return i;
    }

    private boolean measureHierarchy(View view, WindowManager.LayoutParams layoutParams, Resources resources, int i, int i2) {
        boolean z;
        int dimension;
        if (DEBUG_ORIENTATION || DEBUG_LAYOUT) {
            Log.v(this.mTag, "Measuring " + view + " in display " + i + "x" + i2 + Session.TRUNCATE_STRING);
        }
        boolean z2 = false;
        if (layoutParams.width == -2) {
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            resources.getValue(R.dimen.config_prefDialogWidth, this.mTmpValue, true);
            if (this.mTmpValue.type == 5) {
                dimension = (int) this.mTmpValue.getDimension(displayMetrics);
            } else {
                dimension = 0;
            }
            if (DEBUG_DIALOG) {
                Log.v(this.mTag, "Window " + this.mView + ": baseSize=" + dimension + ", desiredWindowWidth=" + i);
            }
            if (dimension != 0 && i > dimension) {
                int rootMeasureSpec = getRootMeasureSpec(dimension, layoutParams.width);
                int rootMeasureSpec2 = getRootMeasureSpec(i2, layoutParams.height);
                performMeasure(rootMeasureSpec, rootMeasureSpec2);
                if (DEBUG_DIALOG) {
                    Log.v(this.mTag, "Window " + this.mView + ": measured (" + view.getMeasuredWidth() + "," + view.getMeasuredHeight() + ") from width spec: " + View.MeasureSpec.toString(rootMeasureSpec) + " and height spec: " + View.MeasureSpec.toString(rootMeasureSpec2));
                }
                if ((view.getMeasuredWidthAndState() & 16777216) != 0) {
                    int i3 = (dimension + i) / 2;
                    if (DEBUG_DIALOG) {
                        Log.v(this.mTag, "Window " + this.mView + ": next baseSize=" + i3);
                    }
                    performMeasure(getRootMeasureSpec(i3, layoutParams.width), rootMeasureSpec2);
                    if (DEBUG_DIALOG) {
                        Log.v(this.mTag, "Window " + this.mView + ": measured (" + view.getMeasuredWidth() + "," + view.getMeasuredHeight() + ")");
                    }
                    if ((view.getMeasuredWidthAndState() & 16777216) == 0) {
                        if (DEBUG_DIALOG) {
                            Log.v(this.mTag, "Good!");
                        }
                    }
                    z = false;
                }
                z = true;
            } else {
                z = false;
            }
        }
        if (!z) {
            performMeasure(getRootMeasureSpec(i, layoutParams.width), getRootMeasureSpec(i2, layoutParams.height));
            if (this.mWidth != view.getMeasuredWidth() || this.mHeight != view.getMeasuredHeight()) {
                z2 = true;
            }
        }
        if (DBG) {
            System.out.println("======================================");
            System.out.println("performTraversals -- after measure");
            view.debug();
        }
        if (DEBUG_ORIENTATION || ViewDebugManager.DEBUG_LAYOUT) {
            Log.v(this.mTag, "ViewRoot measure-: host measured size = (" + view.getMeasuredWidth() + "x" + view.getMeasuredHeight() + "), windowSizeMayChange = " + z2 + ", this = " + this);
        }
        return z2;
    }

    void transformMatrixToGlobal(Matrix matrix) {
        matrix.preTranslate(this.mAttachInfo.mWindowLeft, this.mAttachInfo.mWindowTop);
    }

    void transformMatrixToLocal(Matrix matrix) {
        matrix.postTranslate(-this.mAttachInfo.mWindowLeft, -this.mAttachInfo.mWindowTop);
    }

    WindowInsets getWindowInsets(boolean z) {
        if (this.mLastWindowInsets == null || z) {
            this.mDispatchContentInsets.set(this.mAttachInfo.mContentInsets);
            this.mDispatchStableInsets.set(this.mAttachInfo.mStableInsets);
            this.mDispatchDisplayCutout = this.mAttachInfo.mDisplayCutout.get();
            Rect rect = this.mDispatchContentInsets;
            Rect rect2 = this.mDispatchStableInsets;
            DisplayCutout displayCutout = this.mDispatchDisplayCutout;
            if (!z && (!this.mPendingContentInsets.equals(rect) || !this.mPendingStableInsets.equals(rect2) || !this.mPendingDisplayCutout.get().equals(displayCutout))) {
                rect = this.mPendingContentInsets;
                rect2 = this.mPendingStableInsets;
                displayCutout = this.mPendingDisplayCutout.get();
            }
            DisplayCutout displayCutout2 = displayCutout;
            Rect rect3 = this.mAttachInfo.mOutsets;
            if (rect3.left > 0 || rect3.top > 0 || rect3.right > 0 || rect3.bottom > 0) {
                rect = new Rect(rect.left + rect3.left, rect.top + rect3.top, rect.right + rect3.right, rect.bottom + rect3.bottom);
            }
            this.mLastWindowInsets = new WindowInsets(ensureInsetsNonNegative(rect, "content"), null, ensureInsetsNonNegative(rect2, "stable"), this.mContext.getResources().getConfiguration().isScreenRound(), this.mAttachInfo.mAlwaysConsumeNavBar, displayCutout2);
        }
        return this.mLastWindowInsets;
    }

    private Rect ensureInsetsNonNegative(Rect rect, String str) {
        if (rect.left < 0 || rect.top < 0 || rect.right < 0 || rect.bottom < 0) {
            Log.wtf(this.mTag, "Negative " + str + "Insets: " + rect + ", mFirst=" + this.mFirst);
            return new Rect(Math.max(0, rect.left), Math.max(0, rect.top), Math.max(0, rect.right), Math.max(0, rect.bottom));
        }
        return rect;
    }

    void dispatchApplyInsets(View view) {
        WindowInsets windowInsets = getWindowInsets(true);
        if (!(this.mWindowAttributes.layoutInDisplayCutoutMode == 1)) {
            windowInsets = windowInsets.consumeDisplayCutout();
        }
        view.dispatchApplyWindowInsets(windowInsets);
    }

    private static boolean shouldUseDisplaySize(WindowManager.LayoutParams layoutParams) {
        return layoutParams.type == 2014 || layoutParams.type == 2011 || layoutParams.type == 2020;
    }

    private int dipToPx(int i) {
        return (int) ((this.mContext.getResources().getDisplayMetrics().density * i) + 0.5f);
    }

    private void performTraversals() {
        boolean z;
        boolean z2;
        WindowManager.LayoutParams layoutParams;
        int iWidth;
        int iHeight;
        boolean z3;
        boolean z4;
        Rect rect;
        boolean zMeasureHierarchy;
        int i;
        int i2;
        boolean z5;
        int i3;
        int i4;
        boolean z6;
        boolean z7;
        boolean z8;
        boolean z9;
        boolean z10;
        boolean z11;
        Rect rect2;
        WindowManager.LayoutParams layoutParams2;
        int i5;
        boolean z12;
        boolean z13;
        boolean z14;
        boolean z15;
        int i6;
        boolean z16;
        boolean z17;
        boolean z18;
        int iRelayoutWindow;
        boolean z19;
        boolean z20;
        int i7;
        ?? r20;
        int i8;
        Rect rect3;
        WindowManager.LayoutParams layoutParams3;
        ThreadedRenderer threadedRenderer;
        int iMakeMeasureSpec;
        int i9;
        boolean z21;
        boolean z22;
        SurfaceHolder.Callback[] callbacks;
        ?? r202;
        boolean z23;
        boolean z24;
        boolean z25;
        boolean z26;
        boolean z27;
        boolean z28;
        boolean z29;
        boolean z30;
        boolean z31;
        boolean z32;
        boolean z33;
        boolean z34;
        boolean z35;
        boolean z36;
        boolean z37;
        boolean z38;
        boolean z39;
        boolean zInitialize;
        boolean z40;
        ?? r203;
        boolean z41;
        boolean z42;
        boolean z43;
        boolean z44;
        boolean z45;
        boolean z46;
        boolean z47;
        boolean zMayUseInputMethod;
        Rect translatedContentInsets;
        Rect translatedVisibleInsets;
        Region translatedTouchableArea;
        boolean z48;
        int iDipToPx;
        int iDipToPx2;
        int i10;
        boolean z49;
        boolean z50;
        int i11;
        View view = this.mView;
        if (view == null || !this.mAdded) {
            return;
        }
        if (DBG) {
            System.out.println("======================================");
            System.out.println("performTraversals");
            view.debug();
        }
        this.mIsInTraversal = true;
        this.mWillDrawSoon = true;
        WindowManager.LayoutParams layoutParams4 = this.mWindowAttributes;
        int hostVisibility = getHostVisibility();
        boolean z51 = !this.mFirst && (this.mViewVisibility != hostVisibility || this.mNewSurfaceNeeded || this.mAppVisibilityChanged);
        this.mAppVisibilityChanged = false;
        if (this.mFirst) {
            z = false;
        } else if ((this.mViewVisibility == 0) != (hostVisibility == 0)) {
            z = true;
        }
        WindowManager.LayoutParams layoutParams5 = null;
        if (this.mWindowAttributesChanged) {
            this.mWindowAttributesChanged = false;
            z2 = true;
            layoutParams5 = layoutParams4;
        } else {
            z2 = false;
        }
        if (this.mDisplay.getDisplayAdjustments().getCompatibilityInfo().supportsScreen() == this.mLastInCompatMode) {
            this.mFullRedrawNeeded = true;
            this.mLayoutRequested = true;
            if (this.mLastInCompatMode) {
                layoutParams4.privateFlags &= -129;
                this.mLastInCompatMode = false;
            } else {
                layoutParams4.privateFlags |= 128;
                this.mLastInCompatMode = true;
            }
            layoutParams = layoutParams4;
        } else {
            layoutParams = layoutParams5;
        }
        this.mWindowAttributesChangesFlag = 0;
        Rect rect4 = this.mWinFrame;
        if (!this.mFirst) {
            iWidth = rect4.width();
            iHeight = rect4.height();
            if (iWidth != this.mWidth || iHeight != this.mHeight) {
                if (DEBUG_ORIENTATION) {
                    Log.v(this.mTag, "View " + view + " resized to: " + rect4);
                }
                this.mFullRedrawNeeded = true;
                this.mLayoutRequested = true;
                z3 = true;
            }
            if (z51) {
                this.mAttachInfo.mWindowVisibility = hostVisibility;
                view.dispatchWindowVisibilityChanged(hostVisibility);
                if (z) {
                    view.dispatchVisibilityAggregated(hostVisibility == 0);
                }
                if (hostVisibility != 0 || this.mNewSurfaceNeeded) {
                    endDragResizing();
                    destroyHardwareResources();
                }
                if (hostVisibility == 8) {
                    this.mHasHadWindowFocus = false;
                }
            }
            if (this.mAttachInfo.mWindowVisibility != 0) {
                view.clearAccessibilityFocus();
            }
            getRunQueue().executeActions(this.mAttachInfo.mHandler);
            z4 = !this.mLayoutRequested && (!this.mStopped || this.mReportNextDraw);
            if (z4) {
                rect = rect4;
                zMeasureHierarchy = z3;
                i = iHeight;
                i2 = iWidth;
                z5 = false;
            } else {
                Resources resources = this.mView.getContext().getResources();
                if (this.mFirst) {
                    this.mAttachInfo.mInTouchMode = !this.mAddedTouchMode;
                    ensureTouchModeLocally(this.mAddedTouchMode);
                    z50 = z3;
                    i10 = iHeight;
                    i11 = iWidth;
                    z49 = false;
                } else {
                    boolean z52 = !this.mPendingOverscanInsets.equals(this.mAttachInfo.mOverscanInsets);
                    if (this.mPendingContentInsets.equals(this.mAttachInfo.mContentInsets)) {
                        z48 = z52;
                    } else {
                        if (DEBUG_LAYOUT) {
                            Log.v(this.mTag, "Content insets changing from " + this.mPendingContentInsets + " to: " + this.mAttachInfo.mContentInsets);
                        }
                        z48 = true;
                    }
                    if (!this.mPendingStableInsets.equals(this.mAttachInfo.mStableInsets)) {
                        z48 = true;
                    }
                    if (!this.mPendingDisplayCutout.equals(this.mAttachInfo.mDisplayCutout)) {
                        z48 = true;
                    }
                    if (!this.mPendingVisibleInsets.equals(this.mAttachInfo.mVisibleInsets)) {
                        this.mAttachInfo.mVisibleInsets.set(this.mPendingVisibleInsets);
                        if (DEBUG_LAYOUT) {
                            Log.v(this.mTag, "Visible insets changing to: " + this.mAttachInfo.mVisibleInsets + ", mWinFrame = " + this.mWinFrame);
                        }
                    }
                    if (!this.mPendingOutsets.equals(this.mAttachInfo.mOutsets)) {
                        z48 = true;
                    }
                    if (this.mPendingAlwaysConsumeNavBar != this.mAttachInfo.mAlwaysConsumeNavBar) {
                        z48 = true;
                    }
                    if (layoutParams4.width == -2 || layoutParams4.height == -2) {
                        if (shouldUseDisplaySize(layoutParams4)) {
                            Point point = new Point();
                            this.mDisplay.getRealSize(point);
                            iDipToPx = point.x;
                            iDipToPx2 = point.y;
                        } else {
                            Configuration configuration = resources.getConfiguration();
                            iDipToPx = dipToPx(configuration.screenWidthDp);
                            iDipToPx2 = dipToPx(configuration.screenHeightDp);
                        }
                        i10 = iDipToPx2;
                        z49 = z48;
                        z50 = true;
                        i11 = iDipToPx;
                    } else {
                        z50 = z3;
                        i10 = iHeight;
                        z49 = z48;
                        i11 = iWidth;
                    }
                }
                int i12 = i11;
                r202 = i11;
                rect = rect4;
                i = i10;
                zMeasureHierarchy = measureHierarchy(view, layoutParams4, resources, i12, i) | z50;
                z5 = z49;
                i2 = r202 == true ? 1 : 0;
            }
            if (collectViewAttributes()) {
                layoutParams = layoutParams4;
            }
            if (this.mAttachInfo.mForceReportNewAttributes) {
                this.mAttachInfo.mForceReportNewAttributes = false;
                layoutParams = layoutParams4;
            }
            if (!this.mFirst || this.mAttachInfo.mViewVisibilityChanged) {
                this.mAttachInfo.mViewVisibilityChanged = false;
                i3 = this.mSoftInputMode & 240;
                if (i3 == 0) {
                    int size = this.mAttachInfo.mScrollContainers.size();
                    int i13 = i3;
                    for (int i14 = 0; i14 < size; i14++) {
                        if (this.mAttachInfo.mScrollContainers.get(i14).isShown()) {
                            i13 = 16;
                        }
                    }
                    if (i13 == 0) {
                        i13 = 32;
                    }
                    if ((layoutParams4.softInputMode & 240) != i13) {
                        layoutParams4.softInputMode = (layoutParams4.softInputMode & (-241)) | i13;
                        layoutParams = layoutParams4;
                    }
                }
            }
            if (layoutParams != null) {
                if ((view.mPrivateFlags & 512) != 0 && !PixelFormat.formatHasAlpha(layoutParams.format)) {
                    layoutParams.format = -3;
                }
                this.mAttachInfo.mOverscanRequested = (layoutParams.flags & 33554432) != 0;
            }
            if (!this.mApplyInsetsRequested) {
                this.mApplyInsetsRequested = false;
                this.mLastOverscanRequested = this.mAttachInfo.mOverscanRequested;
                dispatchApplyInsets(view);
                if (this.mLayoutRequested) {
                    z6 = z2;
                    z7 = z5;
                    i4 = i;
                    zMeasureHierarchy |= measureHierarchy(view, layoutParams4, this.mView.getContext().getResources(), i2 == true ? 1 : 0, i);
                } else {
                    i4 = i;
                    z6 = z2;
                    z7 = z5;
                }
            }
            if (z4) {
                this.mLayoutRequested = false;
            }
            boolean z53 = ((z4 || !zMeasureHierarchy || (this.mWidth == view.getMeasuredWidth() && this.mHeight == view.getMeasuredHeight() && ((layoutParams4.width != -2 || rect.width() >= i2 || rect.width() == this.mWidth) && (layoutParams4.height != -2 || rect.height() >= i4 || rect.height() == this.mHeight)))) ? false : true) | (!this.mDragResizing && this.mResizeMode == 0) | this.mActivityRelaunched;
            z8 = !this.mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners() || this.mAttachInfo.mHasNonEmptyGivenInternalInsets;
            int generationId = this.mSurface.getGenerationId();
            z9 = hostVisibility != 0;
            boolean z54 = this.mForceNextWindowRelayout;
            if (DEBUG_LAYOUT) {
                Log.v(this.mTag, "ViewRoot adjustSize+ : mFirst = " + this.mFirst + ", windowShouldResize = " + z53 + ", insetsChanged = " + z7 + ", viewVisibilityChanged = " + z51 + ", params = " + layoutParams + ", mForceNextWindowRelayout = " + this.mForceNextWindowRelayout + ", isViewVisible = " + z9);
            }
            if (!this.mFirst || z53 || z7 || z51 || layoutParams != null || this.mForceNextWindowRelayout) {
                this.mForceNextWindowRelayout = false;
                z10 = z9 ? false : z8 && (this.mFirst || z51);
                if (this.mSurfaceHolder != null) {
                    this.mSurfaceHolder.mSurfaceLock.lock();
                    this.mDrawingAllowed = true;
                }
                boolean zIsValid = this.mSurface.isValid();
                try {
                    if (DEBUG_LAYOUT) {
                        try {
                            Log.i(this.mTag, "host=w:" + view.getMeasuredWidth() + ", h:" + view.getMeasuredHeight() + ", params=" + layoutParams + " surface=" + this.mSurface + ",hadSurface = " + zIsValid);
                        } catch (RemoteException e) {
                            e = e;
                            z23 = z9;
                            rect2 = rect;
                            layoutParams2 = layoutParams4;
                            i5 = hostVisibility;
                            z12 = z8;
                            z13 = z51;
                            z14 = z10;
                            z16 = z6;
                            z17 = false;
                            z18 = false;
                            iRelayoutWindow = 0;
                            z24 = z23;
                            z20 = false;
                            i7 = 0;
                            z15 = zIsValid;
                            r202 = z24;
                            i6 = generationId;
                            Log.e(this.mTag, "RemoteException happens in " + this, e);
                            r20 = r202;
                            boolean z55 = z18;
                            i8 = iRelayoutWindow;
                            if (DEBUG_ORIENTATION) {
                            }
                            if (z22) {
                            }
                            if (z41) {
                            }
                            if (DEBUG_LAYOUT) {
                            }
                            if (z41) {
                            }
                            if (z42) {
                            }
                            if (z12) {
                            }
                            if (this.mFirst) {
                            }
                            if (z13) {
                            }
                            if (this.mAttachInfo.mHasWindowFocus) {
                            }
                            if (z44) {
                            }
                            if (z45) {
                            }
                            if (z43) {
                            }
                            this.mFirst = false;
                            this.mWillDrawSoon = false;
                            this.mNewSurfaceNeeded = false;
                            this.mActivityRelaunched = false;
                            this.mViewVisibility = i5;
                            this.mHadWindowFocus = z44;
                            if (z44) {
                            }
                            if ((i8 & 2) != 0) {
                            }
                            if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                            }
                            if (!z47) {
                            }
                            if (DEBUG_DRAW) {
                            }
                            this.mIsInTraversal = false;
                        }
                    }
                    if (Trace.isTagEnabled(8L)) {
                        Trace.traceBegin(8L, "relayoutWindow");
                    }
                    if (this.mAttachInfo.mThreadedRenderer == null) {
                        if (this.mAttachInfo.mThreadedRenderer.pauseSurface(this.mSurface)) {
                            z40 = z9;
                            try {
                                this.mDirty.set(0, 0, this.mWidth, this.mHeight);
                                z40 = z40;
                            } catch (RemoteException e2) {
                                e = e2;
                                rect2 = rect;
                                layoutParams2 = layoutParams4;
                                i5 = hostVisibility;
                                z12 = z8;
                                z13 = z51;
                                z14 = z10;
                                z16 = z6;
                                z23 = z40;
                                z17 = false;
                                z18 = false;
                                iRelayoutWindow = 0;
                                z24 = z23;
                                z20 = false;
                                i7 = 0;
                                z15 = zIsValid;
                                r202 = z24;
                                i6 = generationId;
                                Log.e(this.mTag, "RemoteException happens in " + this, e);
                                r20 = r202;
                                boolean z552 = z18;
                                i8 = iRelayoutWindow;
                                if (DEBUG_ORIENTATION) {
                                }
                                if (z22) {
                                }
                                if (z41) {
                                }
                                if (DEBUG_LAYOUT) {
                                }
                                if (z41) {
                                }
                                if (z42) {
                                }
                                if (z12) {
                                }
                                if (this.mFirst) {
                                }
                                if (z13) {
                                }
                                if (this.mAttachInfo.mHasWindowFocus) {
                                }
                                if (z44) {
                                }
                                if (z45) {
                                }
                                if (z43) {
                                }
                                this.mFirst = false;
                                this.mWillDrawSoon = false;
                                this.mNewSurfaceNeeded = false;
                                this.mActivityRelaunched = false;
                                this.mViewVisibility = i5;
                                this.mHadWindowFocus = z44;
                                if (z44) {
                                }
                                if ((i8 & 2) != 0) {
                                }
                                if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                                }
                                if (!z47) {
                                }
                                if (DEBUG_DRAW) {
                                }
                                this.mIsInTraversal = false;
                            }
                        } else {
                            z40 = z9;
                        }
                        this.mChoreographer.mFrameInfo.addFlags(1L);
                        z25 = z40;
                    } else {
                        z25 = z9;
                    }
                    try {
                        iRelayoutWindow = relayoutWindow(layoutParams, hostVisibility, z10);
                        try {
                            if (DEBUG_LAYOUT) {
                                try {
                                    Log.v(this.mTag, "relayout: frame=" + rect.toShortString() + " overscan=" + this.mPendingOverscanInsets.toShortString() + " content=" + this.mPendingContentInsets.toShortString() + " visible=" + this.mPendingVisibleInsets.toShortString() + " stable=" + this.mPendingStableInsets.toShortString() + " cutout=" + this.mPendingDisplayCutout.get().toString() + " outsets=" + this.mPendingOutsets.toShortString() + " surface=" + this.mSurface + " valid = " + this.mSurface.isValid() + " surfaceGenerationId = " + generationId + " relayoutResult = " + iRelayoutWindow);
                                } catch (RemoteException e3) {
                                    e = e3;
                                    rect2 = rect;
                                    layoutParams2 = layoutParams4;
                                    i5 = hostVisibility;
                                    z12 = z8;
                                    z13 = z51;
                                    z14 = z10;
                                    z16 = z6;
                                    z24 = z25;
                                    z17 = false;
                                    z18 = false;
                                    z20 = false;
                                    i7 = 0;
                                    z15 = zIsValid;
                                    r202 = z24;
                                    i6 = generationId;
                                    Log.e(this.mTag, "RemoteException happens in " + this, e);
                                    r20 = r202;
                                    boolean z5522 = z18;
                                    i8 = iRelayoutWindow;
                                    if (DEBUG_ORIENTATION) {
                                    }
                                    if (z22) {
                                    }
                                    if (z41) {
                                    }
                                    if (DEBUG_LAYOUT) {
                                    }
                                    if (z41) {
                                    }
                                    if (z42) {
                                    }
                                    if (z12) {
                                    }
                                    if (this.mFirst) {
                                    }
                                    if (z13) {
                                    }
                                    if (this.mAttachInfo.mHasWindowFocus) {
                                    }
                                    if (z44) {
                                    }
                                    if (z45) {
                                    }
                                    if (z43) {
                                    }
                                    this.mFirst = false;
                                    this.mWillDrawSoon = false;
                                    this.mNewSurfaceNeeded = false;
                                    this.mActivityRelaunched = false;
                                    this.mViewVisibility = i5;
                                    this.mHadWindowFocus = z44;
                                    if (z44) {
                                    }
                                    if ((i8 & 2) != 0) {
                                    }
                                    if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                                    }
                                    if (!z47) {
                                    }
                                    if (DEBUG_DRAW) {
                                    }
                                    this.mIsInTraversal = false;
                                }
                            }
                            if (Trace.isTagEnabled(8L)) {
                                Trace.traceEnd(8L);
                            }
                            if (this.mPendingMergedConfiguration.equals(this.mLastReportedMergedConfiguration)) {
                                if (DEBUG_CONFIGURATION) {
                                    Log.v(this.mTag, "Visible with new config: " + this.mPendingMergedConfiguration.getMergedConfiguration());
                                }
                                performConfigurationChange(this.mPendingMergedConfiguration, !this.mFirst, -1);
                                z20 = true;
                            } else {
                                z20 = false;
                            }
                            try {
                                z26 = !this.mPendingOverscanInsets.equals(this.mAttachInfo.mOverscanInsets);
                                z27 = !this.mPendingContentInsets.equals(this.mAttachInfo.mContentInsets);
                                try {
                                    z32 = !this.mPendingVisibleInsets.equals(this.mAttachInfo.mVisibleInsets);
                                    z14 = z10;
                                    try {
                                        z33 = !this.mPendingStableInsets.equals(this.mAttachInfo.mStableInsets);
                                        i5 = hostVisibility;
                                    } catch (RemoteException e4) {
                                        e = e4;
                                        z28 = z27;
                                        z15 = zIsValid;
                                        rect2 = rect;
                                        layoutParams2 = layoutParams4;
                                        i5 = hostVisibility;
                                    }
                                } catch (RemoteException e5) {
                                    e = e5;
                                    z28 = z27;
                                    rect2 = rect;
                                    layoutParams2 = layoutParams4;
                                    i5 = hostVisibility;
                                    z12 = z8;
                                    z13 = z51;
                                    z14 = z10;
                                    z29 = z25;
                                    z15 = zIsValid;
                                }
                            } catch (RemoteException e6) {
                                e = e6;
                                rect2 = rect;
                                layoutParams2 = layoutParams4;
                                i5 = hostVisibility;
                                z12 = z8;
                                z13 = z51;
                                z14 = z10;
                                r202 = z25;
                                z15 = zIsValid;
                                i6 = generationId;
                                z16 = z6;
                                z17 = false;
                                z18 = false;
                            }
                        } catch (RemoteException e7) {
                            e = e7;
                            rect2 = rect;
                            layoutParams2 = layoutParams4;
                            i5 = hostVisibility;
                            z12 = z8;
                            z13 = z51;
                            z14 = z10;
                            z19 = z25;
                            z15 = zIsValid;
                            i6 = generationId;
                            z16 = z6;
                            z17 = false;
                            z18 = false;
                            z20 = false;
                            r202 = z19;
                            i7 = 0;
                            Log.e(this.mTag, "RemoteException happens in " + this, e);
                            r20 = r202;
                            boolean z55222 = z18;
                            i8 = iRelayoutWindow;
                            if (DEBUG_ORIENTATION) {
                            }
                            if (z22) {
                            }
                            if (z41) {
                            }
                            if (DEBUG_LAYOUT) {
                            }
                            if (z41) {
                            }
                            if (z42) {
                            }
                            if (z12) {
                            }
                            if (this.mFirst) {
                            }
                            if (z13) {
                            }
                            if (this.mAttachInfo.mHasWindowFocus) {
                            }
                            if (z44) {
                            }
                            if (z45) {
                            }
                            if (z43) {
                            }
                            this.mFirst = false;
                            this.mWillDrawSoon = false;
                            this.mNewSurfaceNeeded = false;
                            this.mActivityRelaunched = false;
                            this.mViewVisibility = i5;
                            this.mHadWindowFocus = z44;
                            if (z44) {
                            }
                            if ((i8 & 2) != 0) {
                            }
                            if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                            }
                            if (!z47) {
                            }
                            if (DEBUG_DRAW) {
                            }
                            this.mIsInTraversal = false;
                        }
                    } catch (RemoteException e8) {
                        e = e8;
                        rect2 = rect;
                        layoutParams2 = layoutParams4;
                        i5 = hostVisibility;
                        z12 = z8;
                        z13 = z51;
                        z14 = z10;
                        z11 = z25;
                        z15 = zIsValid;
                        i6 = generationId;
                        z16 = z6;
                        z17 = false;
                        z18 = false;
                        iRelayoutWindow = 0;
                        z19 = z11;
                        z20 = false;
                        r202 = z19;
                        i7 = 0;
                        Log.e(this.mTag, "RemoteException happens in " + this, e);
                        r20 = r202;
                        boolean z552222 = z18;
                        i8 = iRelayoutWindow;
                        if (DEBUG_ORIENTATION) {
                        }
                        if (z22) {
                        }
                        if (z41) {
                        }
                        if (DEBUG_LAYOUT) {
                        }
                        if (z41) {
                        }
                        if (z42) {
                        }
                        if (z12) {
                        }
                        if (this.mFirst) {
                        }
                        if (z13) {
                        }
                        if (this.mAttachInfo.mHasWindowFocus) {
                        }
                        if (z44) {
                        }
                        if (z45) {
                        }
                        if (z43) {
                        }
                        this.mFirst = false;
                        this.mWillDrawSoon = false;
                        this.mNewSurfaceNeeded = false;
                        this.mActivityRelaunched = false;
                        this.mViewVisibility = i5;
                        this.mHadWindowFocus = z44;
                        if (z44) {
                        }
                        if ((i8 & 2) != 0) {
                        }
                        if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                        }
                        if (!z47) {
                        }
                        if (DEBUG_DRAW) {
                        }
                        this.mIsInTraversal = false;
                    }
                } catch (RemoteException e9) {
                    e = e9;
                    z11 = z9;
                    rect2 = rect;
                    layoutParams2 = layoutParams4;
                    i5 = hostVisibility;
                    z12 = z8;
                    z13 = z51;
                    z14 = z10;
                }
                try {
                    z34 = !this.mPendingDisplayCutout.equals(this.mAttachInfo.mDisplayCutout);
                    z13 = z51;
                    try {
                        z35 = !this.mPendingOutsets.equals(this.mAttachInfo.mOutsets);
                        z15 = (iRelayoutWindow & 32) == 0;
                        z16 = z6 | z15;
                        z12 = z8;
                        try {
                            layoutParams2 = layoutParams4;
                            try {
                                z36 = this.mPendingAlwaysConsumeNavBar == this.mAttachInfo.mAlwaysConsumeNavBar;
                            } catch (RemoteException e10) {
                                e = e10;
                                z28 = z27;
                                z15 = zIsValid;
                                rect2 = rect;
                                z31 = z25;
                                i6 = generationId;
                                z18 = z28;
                                z30 = z31;
                                z17 = false;
                                r202 = z30;
                                i7 = 0;
                                Log.e(this.mTag, "RemoteException happens in " + this, e);
                                r20 = r202;
                                boolean z5522222 = z18;
                                i8 = iRelayoutWindow;
                                if (DEBUG_ORIENTATION) {
                                }
                                if (z22) {
                                }
                                if (z41) {
                                }
                                if (DEBUG_LAYOUT) {
                                }
                                if (z41) {
                                }
                                if (z42) {
                                }
                                if (z12) {
                                }
                                if (this.mFirst) {
                                }
                                if (z13) {
                                }
                                if (this.mAttachInfo.mHasWindowFocus) {
                                }
                                if (z44) {
                                }
                                if (z45) {
                                }
                                if (z43) {
                                }
                                this.mFirst = false;
                                this.mWillDrawSoon = false;
                                this.mNewSurfaceNeeded = false;
                                this.mActivityRelaunched = false;
                                this.mViewVisibility = i5;
                                this.mHadWindowFocus = z44;
                                if (z44) {
                                }
                                if ((i8 & 2) != 0) {
                                }
                                if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                                }
                                if (!z47) {
                                }
                                if (DEBUG_DRAW) {
                                }
                                this.mIsInTraversal = false;
                            }
                        } catch (RemoteException e11) {
                            e = e11;
                            z28 = z27;
                            z15 = zIsValid;
                            rect2 = rect;
                            layoutParams2 = layoutParams4;
                        }
                    } catch (RemoteException e12) {
                        e = e12;
                        z28 = z27;
                        z15 = zIsValid;
                        rect2 = rect;
                        layoutParams2 = layoutParams4;
                        z12 = z8;
                        z29 = z25;
                        i6 = generationId;
                        z16 = z6;
                        z31 = z29;
                        z18 = z28;
                        z30 = z31;
                        z17 = false;
                        r202 = z30;
                        i7 = 0;
                        Log.e(this.mTag, "RemoteException happens in " + this, e);
                        r20 = r202;
                        boolean z55222222 = z18;
                        i8 = iRelayoutWindow;
                        if (DEBUG_ORIENTATION) {
                        }
                        if (z22) {
                        }
                        if (z41) {
                        }
                        if (DEBUG_LAYOUT) {
                        }
                        if (z41) {
                        }
                        if (z42) {
                        }
                        if (z12) {
                        }
                        if (this.mFirst) {
                        }
                        if (z13) {
                        }
                        if (this.mAttachInfo.mHasWindowFocus) {
                        }
                        if (z44) {
                        }
                        if (z45) {
                        }
                        if (z43) {
                        }
                        this.mFirst = false;
                        this.mWillDrawSoon = false;
                        this.mNewSurfaceNeeded = false;
                        this.mActivityRelaunched = false;
                        this.mViewVisibility = i5;
                        this.mHadWindowFocus = z44;
                        if (z44) {
                        }
                        if ((i8 & 2) != 0) {
                        }
                        if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                        }
                        if (!z47) {
                        }
                        if (DEBUG_DRAW) {
                        }
                        this.mIsInTraversal = false;
                    }
                } catch (RemoteException e13) {
                    e = e13;
                    z28 = z27;
                    z15 = zIsValid;
                    rect2 = rect;
                    layoutParams2 = layoutParams4;
                    z12 = z8;
                    z13 = z51;
                    z29 = z25;
                    i6 = generationId;
                    z16 = z6;
                    z31 = z29;
                    z18 = z28;
                    z30 = z31;
                    z17 = false;
                    r202 = z30;
                    i7 = 0;
                    Log.e(this.mTag, "RemoteException happens in " + this, e);
                    r20 = r202;
                    boolean z552222222 = z18;
                    i8 = iRelayoutWindow;
                    if (DEBUG_ORIENTATION) {
                    }
                    if (z22) {
                    }
                    if (z41) {
                    }
                    if (DEBUG_LAYOUT) {
                    }
                    if (z41) {
                    }
                    if (z42) {
                    }
                    if (z12) {
                    }
                    if (this.mFirst) {
                    }
                    if (z13) {
                    }
                    if (this.mAttachInfo.mHasWindowFocus) {
                    }
                    if (z44) {
                    }
                    if (z45) {
                    }
                    if (z43) {
                    }
                    this.mFirst = false;
                    this.mWillDrawSoon = false;
                    this.mNewSurfaceNeeded = false;
                    this.mActivityRelaunched = false;
                    this.mViewVisibility = i5;
                    this.mHadWindowFocus = z44;
                    if (z44) {
                    }
                    if ((i8 & 2) != 0) {
                    }
                    if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                    }
                    if (!z47) {
                    }
                    if (DEBUG_DRAW) {
                    }
                    this.mIsInTraversal = false;
                }
                if (z27) {
                    z28 = z27;
                } else {
                    try {
                        z28 = z27;
                        try {
                            this.mAttachInfo.mContentInsets.set(this.mPendingContentInsets);
                            if (DEBUG_LAYOUT) {
                                String str = this.mTag;
                                StringBuilder sb = new StringBuilder();
                                rect2 = rect;
                                try {
                                    sb.append("Content insets changing to: ");
                                    sb.append(this.mAttachInfo.mContentInsets);
                                    Log.v(str, sb.toString());
                                } catch (RemoteException e14) {
                                    e = e14;
                                    z15 = zIsValid;
                                    i6 = generationId;
                                    z31 = z25;
                                    z18 = z28;
                                    z30 = z31;
                                    z17 = false;
                                    r202 = z30;
                                    i7 = 0;
                                    Log.e(this.mTag, "RemoteException happens in " + this, e);
                                    r20 = r202;
                                    boolean z5522222222 = z18;
                                    i8 = iRelayoutWindow;
                                    if (DEBUG_ORIENTATION) {
                                    }
                                    if (z22) {
                                    }
                                    if (z41) {
                                    }
                                    if (DEBUG_LAYOUT) {
                                    }
                                    if (z41) {
                                    }
                                    if (z42) {
                                    }
                                    if (z12) {
                                    }
                                    if (this.mFirst) {
                                    }
                                    if (z13) {
                                    }
                                    if (this.mAttachInfo.mHasWindowFocus) {
                                    }
                                    if (z44) {
                                    }
                                    if (z45) {
                                    }
                                    if (z43) {
                                    }
                                    this.mFirst = false;
                                    this.mWillDrawSoon = false;
                                    this.mNewSurfaceNeeded = false;
                                    this.mActivityRelaunched = false;
                                    this.mViewVisibility = i5;
                                    this.mHadWindowFocus = z44;
                                    if (z44) {
                                    }
                                    if ((i8 & 2) != 0) {
                                    }
                                    if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                                    }
                                    if (!z47) {
                                    }
                                    if (DEBUG_DRAW) {
                                    }
                                    this.mIsInTraversal = false;
                                }
                            }
                            if (z26) {
                                this.mAttachInfo.mOverscanInsets.set(this.mPendingOverscanInsets);
                                if (DEBUG_LAYOUT) {
                                    Log.v(this.mTag, "Overscan insets changing to: " + this.mAttachInfo.mOverscanInsets);
                                }
                                z37 = true;
                            } else {
                                z37 = z28;
                            }
                            if (z33) {
                                try {
                                    this.mAttachInfo.mStableInsets.set(this.mPendingStableInsets);
                                    if (DEBUG_LAYOUT) {
                                        Log.v(this.mTag, "Decor insets changing to: " + this.mAttachInfo.mStableInsets);
                                    }
                                    z37 = true;
                                } catch (RemoteException e15) {
                                    e = e15;
                                    z15 = zIsValid;
                                    z18 = z37;
                                    r202 = z25;
                                    z17 = false;
                                    i7 = 0;
                                    i6 = generationId;
                                    Log.e(this.mTag, "RemoteException happens in " + this, e);
                                    r20 = r202;
                                    boolean z55222222222 = z18;
                                    i8 = iRelayoutWindow;
                                    if (DEBUG_ORIENTATION) {
                                    }
                                    if (z22) {
                                    }
                                    if (z41) {
                                    }
                                    if (DEBUG_LAYOUT) {
                                    }
                                    if (z41) {
                                    }
                                    if (z42) {
                                    }
                                    if (z12) {
                                    }
                                    if (this.mFirst) {
                                    }
                                    if (z13) {
                                    }
                                    if (this.mAttachInfo.mHasWindowFocus) {
                                    }
                                    if (z44) {
                                    }
                                    if (z45) {
                                    }
                                    if (z43) {
                                    }
                                    this.mFirst = false;
                                    this.mWillDrawSoon = false;
                                    this.mNewSurfaceNeeded = false;
                                    this.mActivityRelaunched = false;
                                    this.mViewVisibility = i5;
                                    this.mHadWindowFocus = z44;
                                    if (z44) {
                                    }
                                    if ((i8 & 2) != 0) {
                                    }
                                    if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                                    }
                                    if (!z47) {
                                    }
                                    if (DEBUG_DRAW) {
                                    }
                                    this.mIsInTraversal = false;
                                }
                            }
                            if (z34) {
                                this.mAttachInfo.mDisplayCutout.set(this.mPendingDisplayCutout);
                                if (DEBUG_LAYOUT) {
                                    Log.v(this.mTag, "DisplayCutout changing to: " + this.mAttachInfo.mDisplayCutout);
                                }
                                z37 = true;
                            }
                            if (z36) {
                                this.mAttachInfo.mAlwaysConsumeNavBar = this.mPendingAlwaysConsumeNavBar;
                                z18 = true;
                            } else {
                                z18 = z37;
                            }
                        } catch (RemoteException e16) {
                            e = e16;
                            rect2 = rect;
                            z15 = zIsValid;
                            i6 = generationId;
                            z31 = z25;
                            z18 = z28;
                            z30 = z31;
                            z17 = false;
                            r202 = z30;
                            i7 = 0;
                            Log.e(this.mTag, "RemoteException happens in " + this, e);
                            r20 = r202;
                            boolean z552222222222 = z18;
                            i8 = iRelayoutWindow;
                            if (DEBUG_ORIENTATION) {
                            }
                            if (z22) {
                            }
                            if (z41) {
                            }
                            if (DEBUG_LAYOUT) {
                            }
                            if (z41) {
                            }
                            if (z42) {
                            }
                            if (z12) {
                            }
                            if (this.mFirst) {
                            }
                            if (z13) {
                            }
                            if (this.mAttachInfo.mHasWindowFocus) {
                            }
                            if (z44) {
                            }
                            if (z45) {
                            }
                            if (z43) {
                            }
                            this.mFirst = false;
                            this.mWillDrawSoon = false;
                            this.mNewSurfaceNeeded = false;
                            this.mActivityRelaunched = false;
                            this.mViewVisibility = i5;
                            this.mHadWindowFocus = z44;
                            if (z44) {
                            }
                            if ((i8 & 2) != 0) {
                            }
                            if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
                            }
                            if (!z47) {
                            }
                            if (DEBUG_DRAW) {
                            }
                            this.mIsInTraversal = false;
                        }
                    } catch (RemoteException e17) {
                        e = e17;
                        z28 = z27;
                    }
                    if (!z18) {
                        try {
                            if (this.mLastSystemUiVisibility != this.mAttachInfo.mSystemUiVisibility || this.mApplyInsetsRequested || this.mLastOverscanRequested != this.mAttachInfo.mOverscanRequested || z35) {
                                this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                                this.mLastOverscanRequested = this.mAttachInfo.mOverscanRequested;
                                this.mAttachInfo.mOutsets.set(this.mPendingOutsets);
                                this.mApplyInsetsRequested = false;
                                dispatchApplyInsets(view);
                            }
                            if (z32) {
                                this.mAttachInfo.mVisibleInsets.set(this.mPendingVisibleInsets);
                                if (DEBUG_LAYOUT) {
                                    Log.v(this.mTag, "Visible insets changing to: " + this.mAttachInfo.mVisibleInsets);
                                }
                            }
                        } catch (RemoteException e18) {
                            e = e18;
                            z15 = zIsValid;
                            i6 = generationId;
                            z30 = z25;
                            z17 = false;
                            r202 = z30;
                            i7 = 0;
                        }
                        if (!zIsValid) {
                            if (this.mSurface.isValid()) {
                                try {
                                    this.mFullRedrawNeeded = true;
                                    this.mPreviousTransparentRegion.setEmpty();
                                } catch (RemoteException e19) {
                                    e = e19;
                                    z15 = zIsValid;
                                    i6 = generationId;
                                    r202 = z25;
                                    z17 = false;
                                }
                                if (this.mAttachInfo.mThreadedRenderer != null) {
                                    try {
                                        if (Trace.isTagEnabled(8L)) {
                                            Trace.traceBegin(8L, "HW init");
                                        }
                                        zInitialize = this.mAttachInfo.mThreadedRenderer.initialize(this.mSurface);
                                        if (zInitialize) {
                                            try {
                                                try {
                                                    if ((view.mPrivateFlags & 512) == 0) {
                                                        this.mSurface.allocateBuffers();
                                                    }
                                                } catch (Surface.OutOfResourcesException e20) {
                                                    e = e20;
                                                    handleOutOfResourcesException(e);
                                                    return;
                                                }
                                            } catch (RemoteException e21) {
                                                e = e21;
                                                z17 = zInitialize;
                                                z15 = zIsValid;
                                                i6 = generationId;
                                                r202 = z25;
                                                i7 = 1;
                                            }
                                        }
                                        if (Trace.isTagEnabled(8L)) {
                                            Trace.traceEnd(8L);
                                        }
                                        z17 = zInitialize;
                                        i6 = 1;
                                        boolean z56 = (iRelayoutWindow & 16) != 0;
                                        z38 = z56 || ((iRelayoutWindow & 8) != 0);
                                        try {
                                            try {
                                                if (this.mDragResizing != z38) {
                                                    z15 = zIsValid;
                                                    i7 = i6;
                                                    z39 = z25;
                                                    i6 = generationId;
                                                } else if (z38) {
                                                    this.mResizeMode = z56 ? 0 : 1;
                                                    z15 = zIsValid;
                                                    z39 = z25;
                                                    i7 = i6;
                                                    i6 = generationId;
                                                    startDragResizing(this.mPendingBackDropFrame, this.mWinFrame.equals(this.mPendingBackDropFrame), this.mPendingVisibleInsets, this.mPendingStableInsets, this.mResizeMode);
                                                } else {
                                                    z15 = zIsValid;
                                                    i7 = i6;
                                                    z39 = z25;
                                                    i6 = generationId;
                                                    endDragResizing();
                                                }
                                                r20 = z39;
                                                if (!this.mUseMTRenderer) {
                                                    if (z38) {
                                                        this.mCanvasOffsetX = this.mWinFrame.left;
                                                        this.mCanvasOffsetY = this.mWinFrame.top;
                                                        r20 = z39;
                                                    } else {
                                                        this.mCanvasOffsetY = 0;
                                                        this.mCanvasOffsetX = 0;
                                                        r20 = z39;
                                                    }
                                                }
                                            } catch (RemoteException e22) {
                                                e = e22;
                                                Log.e(this.mTag, "RemoteException happens in " + this, e);
                                                r20 = r202;
                                            }
                                        } catch (RemoteException e23) {
                                            e = e23;
                                            z15 = zIsValid;
                                            i7 = i6;
                                            r202 = z25;
                                            i6 = generationId;
                                        }
                                        boolean z5522222222222 = z18;
                                        i8 = iRelayoutWindow;
                                        if (!DEBUG_ORIENTATION || DEBUG_LAYOUT) {
                                            String str2 = this.mTag;
                                            StringBuilder sb2 = new StringBuilder();
                                            sb2.append("Relayout returned: frame=");
                                            rect3 = rect2;
                                            sb2.append(rect3);
                                            sb2.append(", surface=");
                                            sb2.append(this.mSurface);
                                            Log.v(str2, sb2.toString());
                                        } else {
                                            rect3 = rect2;
                                        }
                                        this.mAttachInfo.mWindowLeft = rect3.left;
                                        this.mAttachInfo.mWindowTop = rect3.top;
                                        if (this.mWidth == rect3.width() || this.mHeight != rect3.height()) {
                                            this.mWidth = rect3.width();
                                            this.mHeight = rect3.height();
                                        }
                                        if (this.mSurfaceHolder == null) {
                                            if (this.mSurface.isValid()) {
                                                this.mSurfaceHolder.mSurface = this.mSurface;
                                            }
                                            this.mSurfaceHolder.setSurfaceFrameSize(this.mWidth, this.mHeight);
                                            this.mSurfaceHolder.mSurfaceLock.unlock();
                                            if (this.mSurface.isValid()) {
                                                if (!z15) {
                                                    this.mSurfaceHolder.ungetCallbacks();
                                                    this.mIsCreating = true;
                                                    SurfaceHolder.Callback[] callbacks2 = this.mSurfaceHolder.getCallbacks();
                                                    if (callbacks2 != null) {
                                                        for (SurfaceHolder.Callback callback : callbacks2) {
                                                            callback.surfaceCreated(this.mSurfaceHolder);
                                                        }
                                                    }
                                                    z16 = true;
                                                }
                                                if ((z16 || i6 != this.mSurface.getGenerationId()) && (callbacks = this.mSurfaceHolder.getCallbacks()) != null) {
                                                    for (SurfaceHolder.Callback callback2 : callbacks) {
                                                        callback2.surfaceChanged(this.mSurfaceHolder, layoutParams2.format, this.mWidth, this.mHeight);
                                                    }
                                                }
                                                layoutParams3 = layoutParams2;
                                                this.mIsCreating = false;
                                            } else {
                                                layoutParams3 = layoutParams2;
                                                if (z15) {
                                                    this.mSurfaceHolder.ungetCallbacks();
                                                    SurfaceHolder.Callback[] callbacks3 = this.mSurfaceHolder.getCallbacks();
                                                    if (callbacks3 != null) {
                                                        for (SurfaceHolder.Callback callback3 : callbacks3) {
                                                            callback3.surfaceDestroyed(this.mSurfaceHolder);
                                                        }
                                                    }
                                                    this.mSurfaceHolder.mSurfaceLock.lock();
                                                    try {
                                                        this.mSurfaceHolder.mSurface = new Surface();
                                                    } finally {
                                                        this.mSurfaceHolder.mSurfaceLock.unlock();
                                                    }
                                                }
                                            }
                                        } else {
                                            layoutParams3 = layoutParams2;
                                        }
                                        threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                                        if (threadedRenderer != null && threadedRenderer.isEnabled() && (z17 || this.mWidth != threadedRenderer.getWidth() || this.mHeight != threadedRenderer.getHeight() || this.mNeedsRendererSetup)) {
                                            threadedRenderer.setup(this.mWidth, this.mHeight, this.mAttachInfo, this.mWindowAttributes.surfaceInsets);
                                            this.mNeedsRendererSetup = false;
                                        }
                                        if (this.mStopped || this.mReportNextDraw) {
                                            if (!ensureTouchModeLocally((i8 & 1) == 0) || this.mWidth != view.getMeasuredWidth() || this.mHeight != view.getMeasuredHeight() || z5522222222222 || z20) {
                                                int rootMeasureSpec = getRootMeasureSpec(this.mWidth, layoutParams3.width);
                                                int rootMeasureSpec2 = getRootMeasureSpec(this.mHeight, layoutParams3.height);
                                                if (DEBUG_LAYOUT) {
                                                    Log.v(this.mTag, "Ooops, something changed!  mWidth=" + this.mWidth + " measuredWidth=" + view.getMeasuredWidth() + " mHeight=" + this.mHeight + " measuredHeight=" + view.getMeasuredHeight() + " coveredInsetsChanged=" + z5522222222222);
                                                }
                                                performMeasure(rootMeasureSpec, rootMeasureSpec2);
                                                int measuredWidth = view.getMeasuredWidth();
                                                int measuredHeight = view.getMeasuredHeight();
                                                if (layoutParams3.horizontalWeight <= 0.0f) {
                                                    int i15 = measuredWidth + ((int) ((this.mWidth - measuredWidth) * layoutParams3.horizontalWeight));
                                                    iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(i15, 1073741824);
                                                    i9 = i15;
                                                    z21 = true;
                                                } else {
                                                    iMakeMeasureSpec = rootMeasureSpec;
                                                    i9 = measuredWidth;
                                                    z21 = false;
                                                }
                                                if (layoutParams3.verticalWeight > 0.0f) {
                                                    measuredHeight += (int) ((this.mHeight - measuredHeight) * layoutParams3.verticalWeight);
                                                    rootMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(measuredHeight, 1073741824);
                                                    z21 = true;
                                                }
                                                if (z21) {
                                                    if (DEBUG_LAYOUT) {
                                                        Log.v(this.mTag, "And hey let's measure once more: width=" + i9 + " height=" + measuredHeight);
                                                    }
                                                    performMeasure(iMakeMeasureSpec, rootMeasureSpec2);
                                                }
                                                z4 = true;
                                            }
                                        }
                                        z22 = z4;
                                        r203 = r20;
                                    } catch (Surface.OutOfResourcesException e24) {
                                        e = e24;
                                        zInitialize = false;
                                    }
                                } else {
                                    i6 = 1;
                                }
                                i7 = 1;
                                Log.e(this.mTag, "RemoteException happens in " + this, e);
                                r20 = r202;
                                boolean z55222222222222 = z18;
                                i8 = iRelayoutWindow;
                                if (DEBUG_ORIENTATION) {
                                    String str22 = this.mTag;
                                    StringBuilder sb22 = new StringBuilder();
                                    sb22.append("Relayout returned: frame=");
                                    rect3 = rect2;
                                    sb22.append(rect3);
                                    sb22.append(", surface=");
                                    sb22.append(this.mSurface);
                                    Log.v(str22, sb22.toString());
                                    this.mAttachInfo.mWindowLeft = rect3.left;
                                    this.mAttachInfo.mWindowTop = rect3.top;
                                    if (this.mWidth == rect3.width()) {
                                        this.mWidth = rect3.width();
                                        this.mHeight = rect3.height();
                                        if (this.mSurfaceHolder == null) {
                                        }
                                        threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                                        if (threadedRenderer != null) {
                                            threadedRenderer.setup(this.mWidth, this.mHeight, this.mAttachInfo, this.mWindowAttributes.surfaceInsets);
                                            this.mNeedsRendererSetup = false;
                                        }
                                        if (this.mStopped) {
                                            if (!ensureTouchModeLocally((i8 & 1) == 0)) {
                                                int rootMeasureSpec3 = getRootMeasureSpec(this.mWidth, layoutParams3.width);
                                                int rootMeasureSpec22 = getRootMeasureSpec(this.mHeight, layoutParams3.height);
                                                if (DEBUG_LAYOUT) {
                                                }
                                                performMeasure(rootMeasureSpec3, rootMeasureSpec22);
                                                int measuredWidth2 = view.getMeasuredWidth();
                                                int measuredHeight2 = view.getMeasuredHeight();
                                                if (layoutParams3.horizontalWeight <= 0.0f) {
                                                }
                                                if (layoutParams3.verticalWeight > 0.0f) {
                                                }
                                                if (z21) {
                                                }
                                                z4 = true;
                                                z22 = z4;
                                                r203 = r20;
                                            }
                                        }
                                    }
                                }
                            }
                            z17 = false;
                            if ((iRelayoutWindow & 16) != 0) {
                            }
                            if (z56) {
                                if (this.mDragResizing != z38) {
                                }
                                r20 = z39;
                                if (!this.mUseMTRenderer) {
                                }
                                boolean z552222222222222 = z18;
                                i8 = iRelayoutWindow;
                                if (DEBUG_ORIENTATION) {
                                }
                            }
                        } else if (!this.mSurface.isValid()) {
                            if (this.mLastScrolledFocus != null) {
                                this.mLastScrolledFocus.clear();
                            }
                            this.mCurScrollY = 0;
                            this.mScrollY = 0;
                            if (this.mView instanceof RootViewSurfaceTaker) {
                                ((RootViewSurfaceTaker) this.mView).onRootViewScrollYChanged(this.mCurScrollY);
                            }
                            if (this.mScroller != null) {
                                this.mScroller.abortAnimation();
                            }
                            if (this.mAttachInfo.mThreadedRenderer != null && this.mAttachInfo.mThreadedRenderer.isEnabled()) {
                                this.mAttachInfo.mThreadedRenderer.destroy();
                            }
                        } else if ((generationId != this.mSurface.getGenerationId() || z15 || z54) && this.mSurfaceHolder == null && this.mAttachInfo.mThreadedRenderer != null) {
                            this.mFullRedrawNeeded = true;
                            try {
                                this.mAttachInfo.mThreadedRenderer.updateSurface(this.mSurface);
                            } catch (Surface.OutOfResourcesException e25) {
                                handleOutOfResourcesException(e25);
                                return;
                            }
                        }
                        i6 = 0;
                        z17 = false;
                        if ((iRelayoutWindow & 16) != 0) {
                        }
                        if (z56) {
                        }
                    }
                }
                rect2 = rect;
                if (z26) {
                }
                if (z33) {
                }
                if (z34) {
                }
                if (z36) {
                }
                if (!z18) {
                }
            } else {
                maybeHandleWindowMove(rect);
                r203 = z9;
                rect3 = rect;
                layoutParams3 = layoutParams4;
                i5 = hostVisibility;
                z12 = z8;
                z13 = z51;
                z22 = z4;
                i8 = 0;
                z14 = false;
                i7 = 0;
            }
            z41 = z22 && (!this.mStopped || this.mReportNextDraw);
            z42 = z41 || this.mAttachInfo.mRecomputeGlobalAttributes;
            if (DEBUG_LAYOUT) {
                Log.v(this.mTag, "ViewRoot layout+ : " + view + ", layoutRequested = " + z22 + ", frame = " + rect3 + ", mStopped = " + this.mStopped + ", host.getMeasuredWidth() = " + view.getMeasuredWidth() + ", host.getMeasuredHeight() = " + view.getMeasuredHeight());
            }
            if (z41) {
                performLayout(layoutParams3, this.mWidth, this.mHeight);
                if ((view.mPrivateFlags & 512) != 0) {
                    view.getLocationInWindow(this.mTmpLocation);
                    this.mTransparentRegion.set(this.mTmpLocation[0], this.mTmpLocation[1], (this.mTmpLocation[0] + view.mRight) - view.mLeft, (this.mTmpLocation[1] + view.mBottom) - view.mTop);
                    view.gatherTransparentRegion(this.mTransparentRegion);
                    if (this.mTranslator != null) {
                        this.mTranslator.translateRegionInWindowToScreen(this.mTransparentRegion);
                    }
                    if (!this.mTransparentRegion.equals(this.mPreviousTransparentRegion)) {
                        this.mPreviousTransparentRegion.set(this.mTransparentRegion);
                        this.mFullRedrawNeeded = true;
                        try {
                            this.mWindowSession.setTransparentRegion(this.mWindow, this.mTransparentRegion);
                            if (ViewDebugManager.DBG_TRANSP) {
                                Log.d(this.mTag, "Set transparent region to WMS, region = " + this.mTransparentRegion);
                            }
                        } catch (RemoteException e26) {
                            Log.e(this.mTag, "Exception in " + this + " when set transparent region.", e26);
                        }
                    }
                }
                if (DBG) {
                    System.out.println("======================================");
                    System.out.println("performTraversals -- after setFrame");
                    view.debug();
                }
            }
            if (z42) {
                this.mAttachInfo.mRecomputeGlobalAttributes = false;
                this.mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
            }
            if (z12) {
                ViewTreeObserver.InternalInsetsInfo internalInsetsInfo = this.mAttachInfo.mGivenInternalInsets;
                internalInsetsInfo.reset();
                this.mAttachInfo.mTreeObserver.dispatchOnComputeInternalInsets(internalInsetsInfo);
                this.mAttachInfo.mHasNonEmptyGivenInternalInsets = !internalInsetsInfo.isEmpty();
                if (z14 || !this.mLastGivenInsets.equals(internalInsetsInfo)) {
                    this.mLastGivenInsets.set(internalInsetsInfo);
                    if (this.mTranslator != null) {
                        translatedContentInsets = this.mTranslator.getTranslatedContentInsets(internalInsetsInfo.contentInsets);
                        translatedVisibleInsets = this.mTranslator.getTranslatedVisibleInsets(internalInsetsInfo.visibleInsets);
                        translatedTouchableArea = this.mTranslator.getTranslatedTouchableArea(internalInsetsInfo.touchableRegion);
                    } else {
                        translatedContentInsets = internalInsetsInfo.contentInsets;
                        translatedVisibleInsets = internalInsetsInfo.visibleInsets;
                        translatedTouchableArea = internalInsetsInfo.touchableRegion;
                    }
                    try {
                        this.mWindowSession.setInsets(this.mWindow, internalInsetsInfo.mTouchableInsets, translatedContentInsets, translatedVisibleInsets, translatedTouchableArea);
                    } catch (RemoteException e27) {
                        Log.e(this.mTag, "RemoteException happens when setInsets, mWindow = " + this.mWindow + ", contentInsets = " + translatedContentInsets + ", visibleInsets = " + translatedVisibleInsets + ", touchableRegion = " + translatedTouchableArea + ", this = " + this, e27);
                    }
                }
            }
            if (this.mFirst) {
                if (sAlwaysAssignFocus || !isInTouchMode()) {
                    if (DEBUG_INPUT_RESIZE) {
                        Log.v(this.mTag, "First: mView.hasFocus()=" + this.mView.hasFocus());
                    }
                    if (this.mView != null) {
                        if (!this.mView.hasFocus()) {
                            this.mView.restoreDefaultFocus();
                            if (DEBUG_INPUT_RESIZE) {
                                Log.v(this.mTag, "First: requested focused view=" + this.mView.findFocus());
                            }
                        } else if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "First: existing focused view=" + this.mView.findFocus());
                        }
                    }
                } else {
                    View viewFindFocus = this.mView.findFocus();
                    if ((viewFindFocus instanceof ViewGroup) && ((ViewGroup) viewFindFocus).getDescendantFocusability() == 262144) {
                        viewFindFocus.restoreDefaultFocus();
                    }
                }
            }
            z43 = (z13 || this.mFirst) && r203 != 0;
            z44 = this.mAttachInfo.mHasWindowFocus && r203 != 0;
            z45 = z44 && this.mLostWindowFocus;
            if (z45) {
                this.mLostWindowFocus = false;
            } else if (!z44 && this.mHadWindowFocus) {
                this.mLostWindowFocus = true;
            }
            if (z43 || z45) {
                if (!(this.mWindowAttributes != null && this.mWindowAttributes.type == 2005)) {
                    view.sendAccessibilityEvent(32);
                }
            }
            this.mFirst = false;
            this.mWillDrawSoon = false;
            this.mNewSurfaceNeeded = false;
            this.mActivityRelaunched = false;
            this.mViewVisibility = i5;
            this.mHadWindowFocus = z44;
            if (!z44 || isInLocalFocusMode() || (zMayUseInputMethod = WindowManager.LayoutParams.mayUseInputMethod(this.mWindowAttributes.flags)) == this.mLastWasImTarget) {
                z46 = true;
            } else {
                this.mLastWasImTarget = zMayUseInputMethod;
                InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
                if (inputMethodManagerPeekInstance != null && zMayUseInputMethod) {
                    inputMethodManagerPeekInstance.onPreWindowFocus(this.mView, z44);
                    z46 = true;
                    inputMethodManagerPeekInstance.onPostWindowFocus(this.mView, this.mView.findFocus(), this.mWindowAttributes.softInputMode, !this.mHasHadWindowFocus, this.mWindowAttributes.flags);
                }
            }
            if ((i8 & 2) != 0) {
                reportNextDraw();
            }
            z47 = (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw() || r203 == 0) ? z46 : false;
            if (!z47 && i7 == 0) {
                if (this.mPendingTransitions != null && this.mPendingTransitions.size() > 0) {
                    for (int i16 = 0; i16 < this.mPendingTransitions.size(); i16++) {
                        this.mPendingTransitions.get(i16).startChangingAnimations();
                    }
                    this.mPendingTransitions.clear();
                }
                performDraw();
            } else if (r203 == 0) {
                scheduleTraversals();
            } else if (this.mPendingTransitions != null && this.mPendingTransitions.size() > 0) {
                for (int i17 = 0; i17 < this.mPendingTransitions.size(); i17++) {
                    this.mPendingTransitions.get(i17).endChangingAnimations();
                }
                this.mPendingTransitions.clear();
            }
            if (DEBUG_DRAW || ViewDebugManager.DEBUG_LIFECYCLE || ViewDebugManager.DEBUG_ENG) {
                ViewDebugManager.getInstance().debugTraveralDone(this.mAttachInfo, this.mAttachInfo.mThreadedRenderer, this.mAttachInfo.mThreadedRenderer != null ? false : this.mAttachInfo.mThreadedRenderer.isEnabled(), this, r203, z47, this.mTag);
            }
            this.mIsInTraversal = false;
        }
        this.mFullRedrawNeeded = true;
        this.mLayoutRequested = true;
        Configuration configuration2 = this.mContext.getResources().getConfiguration();
        if (shouldUseDisplaySize(layoutParams4)) {
            Point point2 = new Point();
            this.mDisplay.getRealSize(point2);
            iWidth = point2.x;
            iHeight = point2.y;
        } else {
            iWidth = this.mWinFrame.width();
            iHeight = this.mWinFrame.height();
        }
        this.mAttachInfo.mUse32BitDrawingCache = true;
        this.mAttachInfo.mHasWindowFocus = false;
        this.mAttachInfo.mWindowVisibility = hostVisibility;
        this.mAttachInfo.mRecomputeGlobalAttributes = false;
        this.mLastConfigurationFromResources.setTo(configuration2);
        this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
        if (this.mViewLayoutDirectionInitial == 2) {
            view.setLayoutDirection(configuration2.getLayoutDirection());
        }
        view.dispatchAttachedToWindow(this.mAttachInfo, 0);
        this.mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
        dispatchApplyInsets(view);
        z3 = false;
        if (z51) {
        }
        if (this.mAttachInfo.mWindowVisibility != 0) {
        }
        getRunQueue().executeActions(this.mAttachInfo.mHandler);
        if (this.mLayoutRequested) {
        }
        if (z4) {
        }
        if (collectViewAttributes()) {
        }
        if (this.mAttachInfo.mForceReportNewAttributes) {
        }
        if (!this.mFirst) {
            this.mAttachInfo.mViewVisibilityChanged = false;
            i3 = this.mSoftInputMode & 240;
            if (i3 == 0) {
            }
        }
        if (layoutParams != null) {
        }
        if (!this.mApplyInsetsRequested) {
        }
        if (z4) {
        }
        boolean z532 = ((z4 || !zMeasureHierarchy || (this.mWidth == view.getMeasuredWidth() && this.mHeight == view.getMeasuredHeight() && ((layoutParams4.width != -2 || rect.width() >= i2 || rect.width() == this.mWidth) && (layoutParams4.height != -2 || rect.height() >= i4 || rect.height() == this.mHeight)))) ? false : true) | (!this.mDragResizing && this.mResizeMode == 0) | this.mActivityRelaunched;
        if (this.mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners()) {
        }
        int generationId2 = this.mSurface.getGenerationId();
        if (hostVisibility != 0) {
        }
        boolean z542 = this.mForceNextWindowRelayout;
        if (DEBUG_LAYOUT) {
        }
        if (this.mFirst) {
            this.mForceNextWindowRelayout = false;
            if (z9) {
            }
            if (this.mSurfaceHolder != null) {
            }
            boolean zIsValid2 = this.mSurface.isValid();
            if (DEBUG_LAYOUT) {
            }
            if (Trace.isTagEnabled(8L)) {
            }
            if (this.mAttachInfo.mThreadedRenderer == null) {
            }
            iRelayoutWindow = relayoutWindow(layoutParams, hostVisibility, z10);
            if (DEBUG_LAYOUT) {
            }
            if (Trace.isTagEnabled(8L)) {
            }
            if (this.mPendingMergedConfiguration.equals(this.mLastReportedMergedConfiguration)) {
            }
            z26 = !this.mPendingOverscanInsets.equals(this.mAttachInfo.mOverscanInsets);
            z27 = !this.mPendingContentInsets.equals(this.mAttachInfo.mContentInsets);
            z32 = !this.mPendingVisibleInsets.equals(this.mAttachInfo.mVisibleInsets);
            z14 = z10;
            z33 = !this.mPendingStableInsets.equals(this.mAttachInfo.mStableInsets);
            i5 = hostVisibility;
            z34 = !this.mPendingDisplayCutout.equals(this.mAttachInfo.mDisplayCutout);
            z13 = z51;
            z35 = !this.mPendingOutsets.equals(this.mAttachInfo.mOutsets);
            if ((iRelayoutWindow & 32) == 0) {
            }
            z16 = z6 | z15;
            z12 = z8;
            layoutParams2 = layoutParams4;
            if (this.mPendingAlwaysConsumeNavBar == this.mAttachInfo.mAlwaysConsumeNavBar) {
            }
            if (z27) {
            }
            rect2 = rect;
            if (z26) {
            }
            if (z33) {
            }
            if (z34) {
            }
            if (z36) {
            }
            if (!z18) {
            }
        }
        if (z22) {
        }
        if (z41) {
        }
        if (DEBUG_LAYOUT) {
        }
        if (z41) {
        }
        if (z42) {
        }
        if (z12) {
        }
        if (this.mFirst) {
        }
        if (z13) {
        }
        if (this.mAttachInfo.mHasWindowFocus) {
        }
        if (z44) {
        }
        if (z45) {
        }
        if (z43) {
            if (this.mWindowAttributes != null) {
                if (!(this.mWindowAttributes != null && this.mWindowAttributes.type == 2005)) {
                }
            }
        }
        this.mFirst = false;
        this.mWillDrawSoon = false;
        this.mNewSurfaceNeeded = false;
        this.mActivityRelaunched = false;
        this.mViewVisibility = i5;
        this.mHadWindowFocus = z44;
        if (z44) {
            z46 = true;
        }
        if ((i8 & 2) != 0) {
        }
        if (this.mAttachInfo.mTreeObserver.dispatchOnPreDraw()) {
        }
        if (!z47) {
            if (r203 == 0) {
            }
        }
        if (DEBUG_DRAW) {
            ViewDebugManager.getInstance().debugTraveralDone(this.mAttachInfo, this.mAttachInfo.mThreadedRenderer, this.mAttachInfo.mThreadedRenderer != null ? false : this.mAttachInfo.mThreadedRenderer.isEnabled(), this, r203, z47, this.mTag);
        }
        this.mIsInTraversal = false;
    }

    private void maybeHandleWindowMove(Rect rect) {
        boolean z;
        if (this.mAttachInfo.mWindowLeft != rect.left || this.mAttachInfo.mWindowTop != rect.top) {
            z = true;
        } else {
            z = false;
        }
        if (z) {
            if (this.mTranslator != null) {
                this.mTranslator.translateRectInScreenToAppWinFrame(rect);
            }
            this.mAttachInfo.mWindowLeft = rect.left;
            this.mAttachInfo.mWindowTop = rect.top;
        }
        if (z || this.mAttachInfo.mNeedsUpdateLightCenter) {
            if (this.mAttachInfo.mThreadedRenderer != null) {
                this.mAttachInfo.mThreadedRenderer.setLightCenter(this.mAttachInfo);
            }
            this.mAttachInfo.mNeedsUpdateLightCenter = false;
        }
    }

    private void handleWindowFocusChanged() {
        synchronized (this) {
            if (this.mWindowFocusChanged) {
                this.mWindowFocusChanged = false;
                boolean z = this.mUpcomingWindowFocus;
                boolean z2 = this.mUpcomingInTouchMode;
                if (this.mAdded) {
                    profileRendering(z);
                    if (z) {
                        ensureTouchModeLocally(z2);
                        if (this.mAttachInfo.mThreadedRenderer != null && this.mSurface.isValid()) {
                            this.mFullRedrawNeeded = true;
                            try {
                                WindowManager.LayoutParams layoutParams = this.mWindowAttributes;
                                this.mAttachInfo.mThreadedRenderer.initializeIfNeeded(this.mWidth, this.mHeight, this.mAttachInfo, this.mSurface, layoutParams != null ? layoutParams.surfaceInsets : null);
                            } catch (Surface.OutOfResourcesException e) {
                                Log.e(this.mTag, "OutOfResourcesException locking surface", e);
                                try {
                                    if (!this.mWindowSession.outOfMemory(this.mWindow)) {
                                        Slog.w(this.mTag, "No processes killed for memory; killing self");
                                        Process.killProcess(Process.myPid());
                                    }
                                } catch (RemoteException e2) {
                                }
                                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6), 500L);
                                return;
                            }
                        }
                    }
                    this.mAttachInfo.mHasWindowFocus = z;
                    this.mLastWasImTarget = WindowManager.LayoutParams.mayUseInputMethod(this.mWindowAttributes.flags);
                    InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
                    if (inputMethodManagerPeekInstance != null && this.mLastWasImTarget && !isInLocalFocusMode()) {
                        inputMethodManagerPeekInstance.onPreWindowFocus(this.mView, z);
                    }
                    if (this.mView != null) {
                        this.mAttachInfo.mKeyDispatchState.reset();
                        this.mView.dispatchWindowFocusChanged(z);
                        this.mAttachInfo.mTreeObserver.dispatchOnWindowFocusChange(z);
                        if (this.mAttachInfo.mTooltipHost != null) {
                            this.mAttachInfo.mTooltipHost.hideTooltip();
                        }
                    }
                    if (z) {
                        if (inputMethodManagerPeekInstance != null && this.mLastWasImTarget && !isInLocalFocusMode()) {
                            inputMethodManagerPeekInstance.onPostWindowFocus(this.mView, this.mView.findFocus(), this.mWindowAttributes.softInputMode, !this.mHasHadWindowFocus, this.mWindowAttributes.flags);
                        }
                        this.mWindowAttributes.softInputMode &= -257;
                        ((WindowManager.LayoutParams) this.mView.getLayoutParams()).softInputMode &= -257;
                        this.mHasHadWindowFocus = true;
                        fireAccessibilityFocusEventIfHasFocusedNode();
                    } else if (this.mPointerCapture) {
                        handlePointerCaptureChanged(false);
                    }
                }
                this.mFirstInputStage.onWindowFocusChanged(z);
            }
        }
    }

    private void fireAccessibilityFocusEventIfHasFocusedNode() {
        View viewFindFocus;
        if (!AccessibilityManager.getInstance(this.mContext).isEnabled() || (viewFindFocus = this.mView.findFocus()) == null) {
            return;
        }
        AccessibilityNodeProvider accessibilityNodeProvider = viewFindFocus.getAccessibilityNodeProvider();
        if (accessibilityNodeProvider == null) {
            viewFindFocus.sendAccessibilityEvent(8);
            return;
        }
        AccessibilityNodeInfo accessibilityNodeInfoFindFocusedVirtualNode = findFocusedVirtualNode(accessibilityNodeProvider);
        if (accessibilityNodeInfoFindFocusedVirtualNode != null) {
            int virtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeInfoFindFocusedVirtualNode.getSourceNodeId());
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(8);
            accessibilityEventObtain.setSource(viewFindFocus, virtualDescendantId);
            accessibilityEventObtain.setPackageName(accessibilityNodeInfoFindFocusedVirtualNode.getPackageName());
            accessibilityEventObtain.setChecked(accessibilityNodeInfoFindFocusedVirtualNode.isChecked());
            accessibilityEventObtain.setContentDescription(accessibilityNodeInfoFindFocusedVirtualNode.getContentDescription());
            accessibilityEventObtain.setPassword(accessibilityNodeInfoFindFocusedVirtualNode.isPassword());
            accessibilityEventObtain.getText().add(accessibilityNodeInfoFindFocusedVirtualNode.getText());
            accessibilityEventObtain.setEnabled(accessibilityNodeInfoFindFocusedVirtualNode.isEnabled());
            viewFindFocus.getParent().requestSendAccessibilityEvent(viewFindFocus, accessibilityEventObtain);
            accessibilityNodeInfoFindFocusedVirtualNode.recycle();
        }
    }

    private AccessibilityNodeInfo findFocusedVirtualNode(AccessibilityNodeProvider accessibilityNodeProvider) {
        AccessibilityNodeInfo accessibilityNodeInfoFindFocus = accessibilityNodeProvider.findFocus(1);
        if (accessibilityNodeInfoFindFocus != null) {
            return accessibilityNodeInfoFindFocus;
        }
        if (!this.mContext.isAutofillCompatibilityEnabled()) {
            return null;
        }
        AccessibilityNodeInfo accessibilityNodeInfoCreateAccessibilityNodeInfo = accessibilityNodeProvider.createAccessibilityNodeInfo(-1);
        if (accessibilityNodeInfoCreateAccessibilityNodeInfo.isFocused()) {
            return accessibilityNodeInfoCreateAccessibilityNodeInfo;
        }
        LinkedList linkedList = new LinkedList();
        linkedList.offer(accessibilityNodeInfoCreateAccessibilityNodeInfo);
        while (!linkedList.isEmpty()) {
            AccessibilityNodeInfo accessibilityNodeInfo = (AccessibilityNodeInfo) linkedList.poll();
            LongArray childNodeIds = accessibilityNodeInfo.getChildNodeIds();
            if (childNodeIds != null && childNodeIds.size() > 0) {
                int size = childNodeIds.size();
                for (int i = 0; i < size; i++) {
                    AccessibilityNodeInfo accessibilityNodeInfoCreateAccessibilityNodeInfo2 = accessibilityNodeProvider.createAccessibilityNodeInfo(AccessibilityNodeInfo.getVirtualDescendantId(childNodeIds.get(i)));
                    if (accessibilityNodeInfoCreateAccessibilityNodeInfo2 != null) {
                        if (accessibilityNodeInfoCreateAccessibilityNodeInfo2.isFocused()) {
                            return accessibilityNodeInfoCreateAccessibilityNodeInfo2;
                        }
                        linkedList.offer(accessibilityNodeInfoCreateAccessibilityNodeInfo2);
                    }
                }
                accessibilityNodeInfo.recycle();
            }
        }
        return null;
    }

    private void handleOutOfResourcesException(Surface.OutOfResourcesException outOfResourcesException) {
        Log.e(this.mTag, "OutOfResourcesException initializing HW surface", outOfResourcesException);
        try {
            if (!this.mWindowSession.outOfMemory(this.mWindow) && Process.myUid() != 1000) {
                Slog.w(this.mTag, "No processes killed for memory; killing self");
                Process.killProcess(Process.myPid());
            }
        } catch (RemoteException e) {
        }
        this.mLayoutRequested = true;
    }

    private void performMeasure(int i, int i2) {
        if (this.mView == null) {
            return;
        }
        Trace.traceBegin(8L, "measure");
        try {
            this.mView.measure(i, i2);
        } finally {
            Trace.traceEnd(8L);
        }
    }

    boolean isInLayout() {
        return this.mInLayout;
    }

    boolean requestLayoutDuringLayout(View view) {
        if (view.mParent == null || view.mAttachInfo == null) {
            return true;
        }
        if (!this.mLayoutRequesters.contains(view)) {
            this.mLayoutRequesters.add(view);
        }
        return !this.mHandlingLayoutInLayoutRequest;
    }

    private void performLayout(WindowManager.LayoutParams layoutParams, int i, int i2) {
        ArrayList<View> validLayoutRequesters;
        this.mLayoutRequested = false;
        this.mScrollMayChange = true;
        this.mInLayout = true;
        View view = this.mView;
        if (view == null) {
            return;
        }
        if (DEBUG_ORIENTATION || DEBUG_LAYOUT) {
            Log.v(this.mTag, "Laying out " + view + " in " + this + " to (" + view.getMeasuredWidth() + ", " + view.getMeasuredHeight() + ")");
        }
        Trace.traceBegin(8L, TtmlUtils.TAG_LAYOUT);
        try {
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            this.mInLayout = false;
            if (this.mLayoutRequesters.size() > 0 && (validLayoutRequesters = getValidLayoutRequesters(this.mLayoutRequesters, false)) != null) {
                this.mHandlingLayoutInLayoutRequest = true;
                int size = validLayoutRequesters.size();
                for (int i3 = 0; i3 < size; i3++) {
                    View view2 = validLayoutRequesters.get(i3);
                    Log.w("View", "requestLayout() improperly called by " + view2 + " during layout: running second layout pass");
                    view2.requestLayout();
                }
                measureHierarchy(view, layoutParams, this.mView.getContext().getResources(), i, i2);
                this.mInLayout = true;
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                this.mHandlingLayoutInLayoutRequest = false;
                final ArrayList<View> validLayoutRequesters2 = getValidLayoutRequesters(this.mLayoutRequesters, true);
                if (validLayoutRequesters2 != null) {
                    getRunQueue().post(new Runnable() {
                        @Override
                        public void run() {
                            int size2 = validLayoutRequesters2.size();
                            for (int i4 = 0; i4 < size2; i4++) {
                                View view3 = (View) validLayoutRequesters2.get(i4);
                                Log.w("View", "requestLayout() improperly called by " + view3 + " during second layout pass: posting in next frame");
                                view3.requestLayout();
                            }
                        }
                    });
                }
            }
            Trace.traceEnd(8L);
            this.mInLayout = false;
        } catch (Throwable th) {
            Trace.traceEnd(8L);
            throw th;
        }
    }

    private ArrayList<View> getValidLayoutRequesters(ArrayList<View> arrayList, boolean z) {
        boolean z2;
        int size = arrayList.size();
        ArrayList<View> arrayList2 = null;
        for (int i = 0; i < size; i++) {
            View view = arrayList.get(i);
            if (view != null && view.mAttachInfo != null && view.mParent != null && (z || (view.mPrivateFlags & 4096) == 4096)) {
                View view2 = view;
                while (true) {
                    if (view2 != null) {
                        if ((view2.mViewFlags & 12) != 8) {
                            if (!(view2.mParent instanceof View)) {
                                view2 = null;
                            } else {
                                view2 = (View) view2.mParent;
                            }
                        } else {
                            z2 = true;
                            break;
                        }
                    } else {
                        z2 = false;
                        break;
                    }
                }
                if (!z2) {
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList<>();
                    }
                    arrayList2.add(view);
                }
            }
        }
        if (!z) {
            for (int i2 = 0; i2 < size; i2++) {
                View view3 = arrayList.get(i2);
                while (view3 != null && (view3.mPrivateFlags & 4096) != 0) {
                    view3.mPrivateFlags &= -4097;
                    if (!(view3.mParent instanceof View)) {
                        view3 = null;
                    } else {
                        view3 = (View) view3.mParent;
                    }
                }
            }
        }
        arrayList.clear();
        return arrayList2;
    }

    @Override
    public void requestTransparentRegion(View view) {
        checkThread();
        if (this.mView == view) {
            this.mView.mPrivateFlags |= 512;
            this.mWindowAttributesChanged = true;
            this.mWindowAttributesChangesFlag = 0;
            requestLayout();
        }
    }

    private static int getRootMeasureSpec(int i, int i2) {
        switch (i2) {
            case -2:
                return View.MeasureSpec.makeMeasureSpec(i, Integer.MIN_VALUE);
            case -1:
                return View.MeasureSpec.makeMeasureSpec(i, 1073741824);
            default:
                return View.MeasureSpec.makeMeasureSpec(i2, 1073741824);
        }
    }

    @Override
    public void onPreDraw(DisplayListCanvas displayListCanvas) {
        if (this.mCurScrollY != 0 && this.mHardwareYOffset != 0 && this.mAttachInfo.mThreadedRenderer.isOpaque()) {
            displayListCanvas.drawColor(-16777216);
        }
        displayListCanvas.translate(-this.mHardwareXOffset, -this.mHardwareYOffset);
    }

    @Override
    public void onPostDraw(DisplayListCanvas displayListCanvas) {
        drawAccessibilityFocusedDrawableIfNeeded(displayListCanvas);
        if (this.mUseMTRenderer) {
            for (int size = this.mWindowCallbacks.size() - 1; size >= 0; size--) {
                this.mWindowCallbacks.get(size).onPostDraw(displayListCanvas);
            }
        }
    }

    void outputDisplayList(View view) {
        view.mRenderNode.output();
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.serializeDisplayListTree();
        }
    }

    private void profileRendering(boolean z) {
        if (this.mProfileRendering) {
            this.mRenderProfilingEnabled = z;
            if (this.mRenderProfiler != null) {
                this.mChoreographer.removeFrameCallback(this.mRenderProfiler);
            }
            if (this.mRenderProfilingEnabled) {
                if (this.mRenderProfiler == null) {
                    this.mRenderProfiler = new Choreographer.FrameCallback() {
                        @Override
                        public void doFrame(long j) {
                            ViewRootImpl.this.mDirty.set(0, 0, ViewRootImpl.this.mWidth, ViewRootImpl.this.mHeight);
                            ViewRootImpl.this.scheduleTraversals();
                            if (ViewRootImpl.this.mRenderProfilingEnabled) {
                                ViewRootImpl.this.mChoreographer.postFrameCallback(ViewRootImpl.this.mRenderProfiler);
                            }
                        }
                    };
                }
                this.mChoreographer.postFrameCallback(this.mRenderProfiler);
                return;
            }
            this.mRenderProfiler = null;
        }
    }

    private void trackFPS() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.mFpsStartTime < 0) {
            this.mFpsPrevTime = jCurrentTimeMillis;
            this.mFpsStartTime = jCurrentTimeMillis;
            this.mFpsNumFrames = 0;
            return;
        }
        this.mFpsNumFrames++;
        String hexString = Integer.toHexString(System.identityHashCode(this));
        long j = jCurrentTimeMillis - this.mFpsPrevTime;
        long j2 = jCurrentTimeMillis - this.mFpsStartTime;
        Log.v(this.mTag, "0x" + hexString + "\tFrame time:\t" + j);
        this.mFpsPrevTime = jCurrentTimeMillis;
        if (j2 > 1000) {
            String str = this.mTag;
            Log.v(str, "0x" + hexString + "\tFPS:\t" + ((this.mFpsNumFrames * 1000.0f) / j2));
            this.mFpsStartTime = jCurrentTimeMillis;
            this.mFpsNumFrames = 0;
        }
    }

    void drawPending() {
        this.mDrawsNeededToReport++;
    }

    void pendingDrawFinished() {
        if (this.mDrawsNeededToReport == 0) {
            throw new RuntimeException("Unbalanced drawPending/pendingDrawFinished calls");
        }
        this.mDrawsNeededToReport--;
        if (this.mDrawsNeededToReport == 0) {
            reportDrawFinished();
        }
    }

    private void postDrawFinished() {
        this.mHandler.sendEmptyMessage(29);
    }

    private void reportDrawFinished() {
        if (Trace.isTagEnabled(8L)) {
            Trace.traceBegin(8L, "finish draw");
        }
        try {
            this.mDrawsNeededToReport = 0;
            this.mWindowSession.finishDrawing(this.mWindow);
        } catch (RemoteException e) {
            if (ViewDebugManager.DEBUG_DRAW) {
                Log.d(this.mTag, "Exception when finish draw window " + this.mWindow + " in " + this, e);
            }
        }
        if (Trace.isTagEnabled(8L)) {
            Trace.traceEnd(8L);
        }
    }

    private void performDraw() {
        boolean z = true;
        if ((this.mAttachInfo.mDisplayState == 1 && !this.mReportNextDraw) || this.mView == null) {
            return;
        }
        boolean z2 = this.mFullRedrawNeeded || this.mReportNextDraw;
        this.mFullRedrawNeeded = false;
        this.mIsDrawing = true;
        Trace.traceBegin(8L, "draw");
        if (this.mReportNextDraw && this.mAttachInfo.mThreadedRenderer != null && this.mAttachInfo.mThreadedRenderer.isEnabled()) {
            this.mAttachInfo.mThreadedRenderer.setFrameCompleteCallback(new ThreadedRenderer.FrameCompleteCallback() {
                @Override
                public final void onFrameComplete(long j) {
                    this.f$0.pendingDrawFinished();
                }
            });
        } else {
            z = false;
        }
        try {
            boolean zDraw = draw(z2);
            if (z && !zDraw) {
                this.mAttachInfo.mThreadedRenderer.setFrameCompleteCallback(null);
                z = false;
            }
            this.mIsDrawing = false;
            Trace.traceEnd(8L);
            if (this.mAttachInfo.mPendingAnimatingRenderNodes != null) {
                int size = this.mAttachInfo.mPendingAnimatingRenderNodes.size();
                for (int i = 0; i < size; i++) {
                    this.mAttachInfo.mPendingAnimatingRenderNodes.get(i).endAllAnimators();
                }
                this.mAttachInfo.mPendingAnimatingRenderNodes.clear();
            }
            if (this.mReportNextDraw) {
                this.mReportNextDraw = false;
                if (this.mWindowDrawCountDown != null) {
                    try {
                        this.mWindowDrawCountDown.await();
                    } catch (InterruptedException e) {
                        Log.e(this.mTag, "Window redraw count down interrupted!");
                    }
                    this.mWindowDrawCountDown = null;
                }
                if (this.mAttachInfo.mThreadedRenderer != null) {
                    this.mAttachInfo.mThreadedRenderer.setStopped(this.mStopped);
                }
                if (LOCAL_LOGV || DEBUG_DRAW) {
                    Log.v(this.mTag, "FINISHED DRAWING: " + ((Object) this.mWindowAttributes.getTitle()));
                }
                if (this.mSurfaceHolder != null && this.mSurface.isValid()) {
                    new SurfaceCallbackHelper(new Runnable() {
                        @Override
                        public final void run() {
                            this.f$0.postDrawFinished();
                        }
                    }).dispatchSurfaceRedrawNeededAsync(this.mSurfaceHolder, this.mSurfaceHolder.getCallbacks());
                } else if (!z) {
                    if (this.mAttachInfo.mThreadedRenderer != null) {
                        this.mAttachInfo.mThreadedRenderer.fence();
                    }
                    pendingDrawFinished();
                }
            }
        } catch (Throwable th) {
            this.mIsDrawing = false;
            Trace.traceEnd(8L);
            throw th;
        }
    }

    private boolean draw(boolean z) {
        int currY;
        boolean z2;
        boolean z3;
        Surface surface = this.mSurface;
        boolean z4 = false;
        if (!surface.isValid()) {
            return false;
        }
        if (DEBUG_FPS) {
            trackFPS();
        }
        if (!sFirstDrawComplete) {
            synchronized (sFirstDrawHandlers) {
                sFirstDrawComplete = true;
                int size = sFirstDrawHandlers.size();
                for (int i = 0; i < size; i++) {
                    this.mHandler.post(sFirstDrawHandlers.get(i));
                }
            }
        }
        scrollToRectOrFocus(null, false);
        if (this.mAttachInfo.mViewScrollChanged) {
            this.mAttachInfo.mViewScrollChanged = false;
            this.mAttachInfo.mTreeObserver.dispatchOnScrollChanged();
        }
        boolean z5 = this.mScroller != null && this.mScroller.computeScrollOffset();
        if (z5) {
            currY = this.mScroller.getCurrY();
        } else {
            currY = this.mScrollY;
        }
        if (this.mCurScrollY != currY) {
            this.mCurScrollY = currY;
            if (this.mView instanceof RootViewSurfaceTaker) {
                ((RootViewSurfaceTaker) this.mView).onRootViewScrollYChanged(this.mCurScrollY);
            }
            z2 = true;
        } else {
            z2 = z;
        }
        float f = this.mAttachInfo.mApplicationScale;
        boolean z6 = this.mAttachInfo.mScalingRequired;
        Rect rect = this.mDirty;
        if (this.mSurfaceHolder != null) {
            rect.setEmpty();
            if (z5 && this.mScroller != null) {
                this.mScroller.abortAnimation();
            }
            return false;
        }
        if (z2) {
            this.mAttachInfo.mIgnoreDirtyState = true;
            rect.set(0, 0, (int) ((this.mWidth * f) + 0.5f), (int) ((this.mHeight * f) + 0.5f));
        }
        if (DEBUG_ORIENTATION || DEBUG_DRAW) {
            Log.v(this.mTag, "Draw " + this.mView + "/" + ((Object) this.mWindowAttributes.getTitle()) + ": dirty={" + rect.left + "," + rect.top + "," + rect.right + "," + rect.bottom + "} surface=" + surface + " surface.isValid()=" + surface.isValid() + ", appScale = " + f + ", width=" + this.mWidth + ", height=" + this.mHeight + ", mScrollY = " + this.mScrollY + ", mCurScrollY = " + this.mCurScrollY + ", animating = " + z5 + ", mIsAnimating = " + this.mIsAnimating + ", this = " + this);
        }
        this.mAttachInfo.mTreeObserver.dispatchOnDraw();
        int i2 = -this.mCanvasOffsetX;
        int i3 = (-this.mCanvasOffsetY) + currY;
        WindowManager.LayoutParams layoutParams = this.mWindowAttributes;
        Rect rect2 = layoutParams != null ? layoutParams.surfaceInsets : null;
        if (rect2 != null) {
            i2 -= rect2.left;
            i3 -= rect2.top;
            rect.offset(rect2.left, rect2.right);
        }
        int i4 = i3;
        int i5 = i2;
        Drawable drawable = this.mAttachInfo.mAccessibilityFocusDrawable;
        if (drawable != null) {
            Rect rect3 = this.mAttachInfo.mTmpInvalRect;
            if (!getAccessibilityFocusedRect(rect3)) {
                rect3.setEmpty();
            }
            z3 = !rect3.equals(drawable.getBounds());
        }
        this.mAttachInfo.mDrawingTime = this.mChoreographer.getFrameTimeNanos() / TimeUtils.NANOS_PER_MS;
        if (!rect.isEmpty() || this.mIsAnimating || z3) {
            if (this.mAttachInfo.mThreadedRenderer != null && this.mAttachInfo.mThreadedRenderer.isEnabled()) {
                boolean z7 = z3 || this.mInvalidateRootRequested;
                this.mInvalidateRootRequested = false;
                this.mIsAnimating = false;
                if (this.mHardwareYOffset != i4 || this.mHardwareXOffset != i5) {
                    this.mHardwareYOffset = i4;
                    this.mHardwareXOffset = i5;
                    z7 = true;
                }
                if (z7) {
                    this.mAttachInfo.mThreadedRenderer.invalidateRoot();
                }
                rect.setEmpty();
                boolean zUpdateContentDrawBounds = updateContentDrawBounds();
                if (this.mReportNextDraw) {
                    this.mAttachInfo.mThreadedRenderer.setStopped(false);
                }
                if (zUpdateContentDrawBounds) {
                    requestDrawWindow();
                }
                ThreadedRenderer.FrameDrawingCallback frameDrawingCallback = this.mNextRtFrameCallback;
                this.mNextRtFrameCallback = null;
                this.mAttachInfo.mThreadedRenderer.draw(this.mView, this.mAttachInfo, this, frameDrawingCallback);
                z4 = true;
            } else {
                if (this.mAttachInfo.mThreadedRenderer != null && !this.mAttachInfo.mThreadedRenderer.isEnabled() && this.mAttachInfo.mThreadedRenderer.isRequested() && this.mSurface.isValid()) {
                    try {
                        this.mAttachInfo.mThreadedRenderer.initializeIfNeeded(this.mWidth, this.mHeight, this.mAttachInfo, this.mSurface, rect2);
                        this.mFullRedrawNeeded = true;
                        scheduleTraversals();
                        return false;
                    } catch (Surface.OutOfResourcesException e) {
                        handleOutOfResourcesException(e);
                        return false;
                    }
                }
                if (!drawSoftware(surface, this.mAttachInfo, i5, i4, z6, rect, rect2)) {
                    if (DEBUG_DRAW) {
                        Log.v(this.mTag, "drawSoftware return: this = " + this);
                    }
                    return false;
                }
            }
        }
        if (z5) {
            this.mFullRedrawNeeded = true;
            scheduleTraversals();
        }
        return z4;
    }

    private boolean drawSoftware(Surface surface, View.AttachInfo attachInfo, int i, int i2, boolean z, Rect rect, Rect rect2) {
        int i3;
        int i4;
        String str;
        StringBuilder sb;
        boolean z2;
        if (rect2 != null) {
            i3 = rect2.left + i;
            i4 = rect2.top + i2;
        } else {
            i3 = i;
            i4 = i2;
        }
        try {
            try {
                rect.offset(-i3, -i4);
                int i5 = rect.left;
                int i6 = rect.top;
                int i7 = rect.right;
                int i8 = rect.bottom;
                if (Trace.isTagEnabled(8L)) {
                    Trace.traceBegin(8L, "drawSoftware lockCanvas");
                }
                Canvas canvasLockCanvas = this.mSurface.lockCanvas(rect);
                if (Trace.isTagEnabled(8L)) {
                    Trace.traceEnd(8L);
                }
                if (i5 != rect.left || i6 != rect.top || i7 != rect.right || i8 != rect.bottom) {
                    attachInfo.mIgnoreDirtyState = true;
                }
                canvasLockCanvas.setDensity(this.mDensity);
                try {
                    if (DEBUG_ORIENTATION || DEBUG_DRAW) {
                        Log.v(this.mTag, "Surface " + surface + " drawing to bitmap w=" + canvasLockCanvas.getWidth() + ", h=" + canvasLockCanvas.getHeight());
                    }
                    if (canvasLockCanvas.isOpaque() && i2 == 0 && i == 0) {
                        z2 = false;
                    } else {
                        z2 = false;
                        canvasLockCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    }
                    rect.setEmpty();
                    this.mIsAnimating = z2;
                    this.mView.mPrivateFlags |= 32;
                    if (DEBUG_DRAW) {
                        Context context = this.mView.getContext();
                        Log.i(this.mTag, "Drawing: package:" + context.getPackageName() + ", metrics=" + context.getResources().getDisplayMetrics() + ", compatibilityInfo=" + context.getResources().getCompatibilityInfo());
                    }
                    try {
                        canvasLockCanvas.translate(-i, -i2);
                        if (this.mTranslator != null) {
                            this.mTranslator.translateCanvas(canvasLockCanvas);
                        }
                        canvasLockCanvas.setScreenDensity(z ? this.mNoncompatDensity : 0);
                        attachInfo.mSetIgnoreDirtyState = false;
                        this.mView.draw(canvasLockCanvas);
                        drawAccessibilityFocusedDrawableIfNeeded(canvasLockCanvas);
                        if (DEBUG_DRAW) {
                            Log.v(this.mTag, "Drawing view end- : mView = " + this.mView + ", this = " + this);
                        }
                        try {
                            surface.unlockCanvasAndPost(canvasLockCanvas);
                            if (LOCAL_LOGV || DEBUG_DRAW) {
                                Log.v(this.mTag, "Surface " + surface + " unlockCanvasAndPost");
                            }
                            mPerfFrameInfoManager.setSoftwareDraw();
                            return true;
                        } catch (IllegalArgumentException e) {
                            e = e;
                            str = this.mTag;
                            sb = new StringBuilder();
                            sb.append("Could not unlock surface, surface = ");
                            sb.append(surface);
                            sb.append(", canvas = ");
                            sb.append(canvasLockCanvas);
                            sb.append(", this = ");
                            sb.append(this);
                            Log.e(str, sb.toString(), e);
                            this.mLayoutRequested = true;
                            return false;
                        }
                    } finally {
                        if (!attachInfo.mSetIgnoreDirtyState) {
                            attachInfo.mIgnoreDirtyState = false;
                        }
                    }
                } catch (Throwable th) {
                    try {
                        surface.unlockCanvasAndPost(canvasLockCanvas);
                        if (LOCAL_LOGV || DEBUG_DRAW) {
                            Log.v(this.mTag, "Surface " + surface + " unlockCanvasAndPost");
                        }
                        throw th;
                    } catch (IllegalArgumentException e2) {
                        e = e2;
                        str = this.mTag;
                        sb = new StringBuilder();
                        sb.append("Could not unlock surface, surface = ");
                        sb.append(surface);
                        sb.append(", canvas = ");
                        sb.append(canvasLockCanvas);
                        sb.append(", this = ");
                        sb.append(this);
                        Log.e(str, sb.toString(), e);
                        this.mLayoutRequested = true;
                        return false;
                    }
                }
            } catch (Surface.OutOfResourcesException e3) {
                handleOutOfResourcesException(e3);
                rect.offset(i3, i4);
                return false;
            } catch (IllegalArgumentException e4) {
                Log.e(this.mTag, "Could not lock surface", e4);
                this.mLayoutRequested = true;
                rect.offset(i3, i4);
                return false;
            }
        } finally {
            rect.offset(i3, i4);
        }
    }

    private void drawAccessibilityFocusedDrawableIfNeeded(Canvas canvas) {
        Rect rect = this.mAttachInfo.mTmpInvalRect;
        if (!getAccessibilityFocusedRect(rect)) {
            if (this.mAttachInfo.mAccessibilityFocusDrawable != null) {
                this.mAttachInfo.mAccessibilityFocusDrawable.setBounds(0, 0, 0, 0);
            }
        } else {
            Drawable accessibilityFocusedDrawable = getAccessibilityFocusedDrawable();
            if (accessibilityFocusedDrawable != null) {
                accessibilityFocusedDrawable.setBounds(rect);
                accessibilityFocusedDrawable.draw(canvas);
            }
        }
    }

    private boolean getAccessibilityFocusedRect(Rect rect) {
        View view;
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(this.mView.mContext);
        if (!accessibilityManager.isEnabled() || !accessibilityManager.isTouchExplorationEnabled() || (view = this.mAccessibilityFocusedHost) == null || view.mAttachInfo == null) {
            return false;
        }
        if (view.getAccessibilityNodeProvider() == null) {
            view.getBoundsOnScreen(rect, true);
        } else {
            if (this.mAccessibilityFocusedVirtualView == null) {
                return false;
            }
            this.mAccessibilityFocusedVirtualView.getBoundsInScreen(rect);
        }
        View.AttachInfo attachInfo = this.mAttachInfo;
        rect.offset(0, attachInfo.mViewRootImpl.mScrollY);
        rect.offset(-attachInfo.mWindowLeft, -attachInfo.mWindowTop);
        if (!rect.intersect(0, 0, attachInfo.mViewRootImpl.mWidth, attachInfo.mViewRootImpl.mHeight)) {
            rect.setEmpty();
        }
        return !rect.isEmpty();
    }

    private Drawable getAccessibilityFocusedDrawable() {
        if (this.mAttachInfo.mAccessibilityFocusDrawable == null) {
            TypedValue typedValue = new TypedValue();
            if (this.mView.mContext.getTheme().resolveAttribute(R.attr.accessibilityFocusedDrawable, typedValue, true)) {
                this.mAttachInfo.mAccessibilityFocusDrawable = this.mView.mContext.getDrawable(typedValue.resourceId);
            }
        }
        return this.mAttachInfo.mAccessibilityFocusDrawable;
    }

    public void requestInvalidateRootRenderNode() {
        this.mInvalidateRootRequested = true;
    }

    boolean scrollToRectOrFocus(Rect rect, boolean z) {
        int height;
        boolean z2;
        Rect rect2 = this.mAttachInfo.mContentInsets;
        Rect rect3 = this.mAttachInfo.mVisibleInsets;
        if (rect3.left > rect2.left || rect3.top > rect2.top || rect3.right > rect2.right || rect3.bottom > rect2.bottom) {
            int i = this.mScrollY;
            View viewFindFocus = this.mView.findFocus();
            if (viewFindFocus == null) {
                return false;
            }
            View view = this.mLastScrolledFocus != null ? this.mLastScrolledFocus.get() : null;
            if (viewFindFocus != view) {
                rect = null;
            }
            if (DEBUG_INPUT_RESIZE) {
                Log.v(this.mTag, "Eval scroll: focus=" + viewFindFocus + " rectangle=" + rect + " ci=" + rect2 + " vi=" + rect3);
            }
            if (viewFindFocus == view && !this.mScrollMayChange && rect == null) {
                if (DEBUG_INPUT_RESIZE) {
                    Log.v(this.mTag, "Keeping scroll y=" + this.mScrollY + " vi=" + rect3.toShortString());
                }
            } else {
                this.mLastScrolledFocus = new WeakReference<>(viewFindFocus);
                this.mScrollMayChange = false;
                if (DEBUG_INPUT_RESIZE) {
                    Log.v(this.mTag, "Need to scroll?");
                }
                if (viewFindFocus.getGlobalVisibleRect(this.mVisRect, null)) {
                    if (DEBUG_INPUT_RESIZE) {
                        Log.v(this.mTag, "Root w=" + this.mView.getWidth() + " h=" + this.mView.getHeight() + " ci=" + rect2.toShortString() + " vi=" + rect3.toShortString());
                    }
                    if (rect == null) {
                        viewFindFocus.getFocusedRect(this.mTempRect);
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "Focus " + viewFindFocus + ": focusRect=" + this.mTempRect.toShortString());
                        }
                        if (this.mView instanceof ViewGroup) {
                            ((ViewGroup) this.mView).offsetDescendantRectToMyCoords(viewFindFocus, this.mTempRect);
                        }
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "Focus in window: focusRect=" + this.mTempRect.toShortString() + " visRect=" + this.mVisRect.toShortString());
                        }
                    } else {
                        this.mTempRect.set(rect);
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "Request scroll to rect: " + this.mTempRect.toShortString() + " visRect=" + this.mVisRect.toShortString());
                        }
                    }
                    if (this.mTempRect.intersect(this.mVisRect)) {
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "Focus window visible rect: " + this.mTempRect.toShortString());
                        }
                        if (this.mTempRect.height() > (this.mView.getHeight() - rect3.top) - rect3.bottom) {
                            if (DEBUG_INPUT_RESIZE) {
                                Log.v(this.mTag, "Too tall; leaving scrollY=" + i);
                            }
                            height = i;
                        } else if (this.mTempRect.top < rect3.top) {
                            height = this.mTempRect.top - rect3.top;
                            if (DEBUG_INPUT_RESIZE) {
                                Log.v(this.mTag, "Top covered; scrollY=" + height);
                            }
                        } else if (this.mTempRect.bottom > this.mView.getHeight() - rect3.bottom) {
                            height = this.mTempRect.bottom - (this.mView.getHeight() - rect3.bottom);
                            if (DEBUG_INPUT_RESIZE) {
                                Log.v(this.mTag, "Bottom covered; scrollY=" + height);
                            }
                        } else {
                            height = 0;
                        }
                        z2 = true;
                    }
                }
            }
            height = i;
            z2 = false;
        } else {
            height = 0;
            z2 = false;
        }
        if (height != this.mScrollY) {
            if (DEBUG_INPUT_RESIZE) {
                Log.v(this.mTag, "Pan scroll changed: old=" + this.mScrollY + " , new=" + height);
            }
            if (!z) {
                if (this.mScroller == null) {
                    this.mScroller = new Scroller(this.mView.getContext());
                }
                this.mScroller.startScroll(0, this.mScrollY, 0, height - this.mScrollY);
            } else if (this.mScroller != null) {
                this.mScroller.abortAnimation();
            }
            this.mScrollY = height;
        }
        return z2;
    }

    public View getAccessibilityFocusedHost() {
        return this.mAccessibilityFocusedHost;
    }

    public AccessibilityNodeInfo getAccessibilityFocusedVirtualView() {
        return this.mAccessibilityFocusedVirtualView;
    }

    void setAccessibilityFocus(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
        if (this.mAccessibilityFocusedVirtualView != null) {
            AccessibilityNodeInfo accessibilityNodeInfo2 = this.mAccessibilityFocusedVirtualView;
            View view2 = this.mAccessibilityFocusedHost;
            this.mAccessibilityFocusedHost = null;
            this.mAccessibilityFocusedVirtualView = null;
            view2.clearAccessibilityFocusNoCallbacks(64);
            AccessibilityNodeProvider accessibilityNodeProvider = view2.getAccessibilityNodeProvider();
            if (accessibilityNodeProvider != null) {
                accessibilityNodeInfo2.getBoundsInParent(this.mTempRect);
                view2.invalidate(this.mTempRect);
                accessibilityNodeProvider.performAction(AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeInfo2.getSourceNodeId()), 128, null);
            }
            accessibilityNodeInfo2.recycle();
        }
        if (this.mAccessibilityFocusedHost != null && this.mAccessibilityFocusedHost != view) {
            this.mAccessibilityFocusedHost.clearAccessibilityFocusNoCallbacks(64);
        }
        this.mAccessibilityFocusedHost = view;
        this.mAccessibilityFocusedVirtualView = accessibilityNodeInfo;
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.invalidateRoot();
        }
    }

    boolean hasPointerCapture() {
        return this.mPointerCapture;
    }

    void requestPointerCapture(boolean z) {
        if (this.mPointerCapture == z) {
            return;
        }
        InputManager.getInstance().requestPointerCapture(this.mAttachInfo.mWindowToken, z);
    }

    private void handlePointerCaptureChanged(boolean z) {
        if (this.mPointerCapture == z) {
            return;
        }
        this.mPointerCapture = z;
        if (this.mView != null) {
            this.mView.dispatchPointerCaptureChanged(z);
        }
    }

    @Override
    public void requestChildFocus(View view, View view2) {
        if (DEBUG_INPUT_RESIZE) {
            Log.v(this.mTag, "Request child " + view + " focus: focus now " + view2 + " in " + this);
        }
        checkThread();
        scheduleTraversals();
    }

    @Override
    public void clearChildFocus(View view) {
        if (DEBUG_INPUT_RESIZE) {
            Log.v(this.mTag, "Clearing child focus");
        }
        checkThread();
        scheduleTraversals();
    }

    @Override
    public ViewParent getParentForAccessibility() {
        return null;
    }

    @Override
    public void focusableViewAvailable(View view) {
        checkThread();
        if (this.mView != null) {
            if (!this.mView.hasFocus()) {
                if (sAlwaysAssignFocus || !this.mAttachInfo.mInTouchMode) {
                    view.requestFocus();
                    return;
                }
                return;
            }
            View viewFindFocus = this.mView.findFocus();
            if ((viewFindFocus instanceof ViewGroup) && ((ViewGroup) viewFindFocus).getDescendantFocusability() == 262144 && isViewDescendantOf(view, viewFindFocus)) {
                view.requestFocus();
            }
        }
    }

    @Override
    public void recomputeViewAttributes(View view) {
        checkThread();
        if (this.mView == view) {
            this.mAttachInfo.mRecomputeGlobalAttributes = true;
            if (!this.mWillDrawSoon) {
                scheduleTraversals();
            }
        }
    }

    void dispatchDetachedFromWindow() {
        this.mFirstInputStage.onDetachedFromWindow();
        if (this.mView != null && this.mView.mAttachInfo != null) {
            this.mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(false);
            this.mView.dispatchDetachedFromWindow();
        }
        this.mAccessibilityInteractionConnectionManager.ensureNoConnection();
        this.mAccessibilityManager.removeAccessibilityStateChangeListener(this.mAccessibilityInteractionConnectionManager);
        this.mAccessibilityManager.removeHighTextContrastStateChangeListener(this.mHighContrastTextManager);
        removeSendWindowContentChangedCallback();
        destroyHardwareRenderer();
        setAccessibilityFocus(null, null);
        this.mView.assignParent(null);
        this.mView = null;
        this.mAttachInfo.mRootView = null;
        this.mSurface.release();
        if (this.mInputQueueCallback != null && this.mInputQueue != null) {
            this.mInputQueueCallback.onInputQueueDestroyed(this.mInputQueue);
            this.mInputQueue.dispose();
            this.mInputQueueCallback = null;
            this.mInputQueue = null;
        }
        if (this.mInputEventReceiver != null) {
            this.mInputEventReceiver.dispose();
            this.mInputEventReceiver = null;
        }
        try {
            this.mWindowSession.remove(this.mWindow);
        } catch (RemoteException e) {
            Log.e(this.mTag, "RemoteException remove window " + this.mWindow + " in " + this, e);
        }
        if (this.mInputChannel != null) {
            this.mInputChannel.dispose();
            this.mInputChannel = null;
        }
        this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
        unscheduleTraversals();
    }

    private void performConfigurationChange(MergedConfiguration mergedConfiguration, boolean z, int i) {
        if (mergedConfiguration == null) {
            throw new IllegalArgumentException("No merged config provided.");
        }
        Configuration globalConfiguration = mergedConfiguration.getGlobalConfiguration();
        Configuration overrideConfiguration = mergedConfiguration.getOverrideConfiguration();
        if (DEBUG_CONFIGURATION) {
            Log.v(this.mTag, "Applying new config to window " + ((Object) this.mWindowAttributes.getTitle()) + ", globalConfig: " + globalConfiguration + ", overrideConfig: " + overrideConfiguration + ", force = " + z + ", this = " + this);
        }
        CompatibilityInfo compatibilityInfo = this.mDisplay.getDisplayAdjustments().getCompatibilityInfo();
        if (!compatibilityInfo.equals(CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO)) {
            Configuration configuration = new Configuration(globalConfiguration);
            compatibilityInfo.applyToConfiguration(this.mNoncompatDensity, configuration);
            globalConfiguration = configuration;
        }
        synchronized (sConfigCallbacks) {
            for (int size = sConfigCallbacks.size() - 1; size >= 0; size--) {
                sConfigCallbacks.get(size).onConfigurationChanged(globalConfiguration);
            }
        }
        this.mLastReportedMergedConfiguration.setConfiguration(globalConfiguration, overrideConfiguration);
        this.mForceNextConfigUpdate = z;
        if (this.mActivityConfigCallback != null) {
            this.mActivityConfigCallback.onConfigurationChanged(overrideConfiguration, i);
        } else {
            updateConfiguration(i);
        }
        this.mForceNextConfigUpdate = false;
    }

    public void updateConfiguration(int i) {
        if (this.mView == null) {
            return;
        }
        Resources resources = this.mView.getResources();
        Configuration configuration = resources.getConfiguration();
        if (i != -1) {
            onMovedToDisplay(i, configuration);
        }
        if (this.mForceNextConfigUpdate || this.mLastConfigurationFromResources.diff(configuration) != 0) {
            this.mDisplay = ResourcesManager.getInstance().getAdjustedDisplay(this.mDisplay.getDisplayId(), resources);
            int layoutDirection = this.mLastConfigurationFromResources.getLayoutDirection();
            int layoutDirection2 = configuration.getLayoutDirection();
            this.mLastConfigurationFromResources.setTo(configuration);
            if (layoutDirection != layoutDirection2 && this.mViewLayoutDirectionInitial == 2) {
                this.mView.setLayoutDirection(layoutDirection2);
            }
            this.mView.dispatchConfigurationChanged(configuration);
            this.mForceNextWindowRelayout = true;
            requestLayout();
        }
    }

    public static boolean isViewDescendantOf(View view, View view2) {
        if (view == view2) {
            return true;
        }
        Object parent = view.getParent();
        return (parent instanceof ViewGroup) && isViewDescendantOf((View) parent, view2);
    }

    private static void forceLayout(View view) {
        view.forceLayout();
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                forceLayout(viewGroup.getChildAt(i));
            }
        }
    }

    final class ViewRootHandler extends Handler {
        ViewRootHandler() {
        }

        @Override
        public String getMessageName(Message message) {
            switch (message.what) {
                case 1:
                    return "MSG_INVALIDATE";
                case 2:
                    return "MSG_INVALIDATE_RECT";
                case 3:
                    return "MSG_DIE";
                case 4:
                    return "MSG_RESIZED";
                case 5:
                    return "MSG_RESIZED_REPORT";
                case 6:
                    return "MSG_WINDOW_FOCUS_CHANGED";
                case 7:
                    return "MSG_DISPATCH_INPUT_EVENT";
                case 8:
                    return "MSG_DISPATCH_APP_VISIBILITY";
                case 9:
                    return "MSG_DISPATCH_GET_NEW_SURFACE";
                case 10:
                case 20:
                case 22:
                case 26:
                default:
                    return super.getMessageName(message);
                case 11:
                    return "MSG_DISPATCH_KEY_FROM_IME";
                case 12:
                    return "MSG_DISPATCH_KEY_FROM_AUTOFILL";
                case 13:
                    return "MSG_CHECK_FOCUS";
                case 14:
                    return "MSG_CLOSE_SYSTEM_DIALOGS";
                case 15:
                    return "MSG_DISPATCH_DRAG_EVENT";
                case 16:
                    return "MSG_DISPATCH_DRAG_LOCATION_EVENT";
                case 17:
                    return "MSG_DISPATCH_SYSTEM_UI_VISIBILITY";
                case 18:
                    return "MSG_UPDATE_CONFIGURATION";
                case 19:
                    return "MSG_PROCESS_INPUT_EVENTS";
                case 21:
                    return "MSG_CLEAR_ACCESSIBILITY_FOCUS_HOST";
                case 23:
                    return "MSG_WINDOW_MOVED";
                case 24:
                    return "MSG_SYNTHESIZE_INPUT_EVENT";
                case 25:
                    return "MSG_DISPATCH_WINDOW_SHOWN";
                case 27:
                    return "MSG_UPDATE_POINTER_ICON";
                case 28:
                    return "MSG_POINTER_CAPTURE_CHANGED";
                case 29:
                    return "MSG_DRAW_FINISHED";
            }
        }

        @Override
        public boolean sendMessageAtTime(Message message, long j) {
            if (message.what == 26 && message.obj == null) {
                throw new NullPointerException("Attempted to call MSG_REQUEST_KEYBOARD_SHORTCUTS with null receiver:");
            }
            return super.sendMessageAtTime(message, j);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    ((View) message.obj).invalidate();
                    return;
                case 2:
                    View.AttachInfo.InvalidateInfo invalidateInfo = (View.AttachInfo.InvalidateInfo) message.obj;
                    invalidateInfo.target.invalidate(invalidateInfo.left, invalidateInfo.top, invalidateInfo.right, invalidateInfo.bottom);
                    invalidateInfo.recycle();
                    return;
                case 3:
                    ViewRootImpl.this.doDie();
                    return;
                case 4:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    if (ViewRootImpl.this.mWinFrame.equals(someArgs.arg1) && ViewRootImpl.this.mPendingOverscanInsets.equals(someArgs.arg5) && ViewRootImpl.this.mPendingContentInsets.equals(someArgs.arg2) && ViewRootImpl.this.mPendingStableInsets.equals(someArgs.arg6) && ViewRootImpl.this.mPendingDisplayCutout.get().equals(someArgs.arg9) && ViewRootImpl.this.mPendingVisibleInsets.equals(someArgs.arg3) && ViewRootImpl.this.mPendingOutsets.equals(someArgs.arg7) && ViewRootImpl.this.mPendingBackDropFrame.equals(someArgs.arg8) && someArgs.arg4 == null && someArgs.argi1 == 0 && ViewRootImpl.this.mDisplay.getDisplayId() == someArgs.argi3) {
                        return;
                    }
                    break;
                case 5:
                    break;
                case 6:
                    ViewRootImpl.this.handleWindowFocusChanged();
                    return;
                case 7:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    ViewRootImpl.this.enqueueInputEvent((InputEvent) someArgs2.arg1, (InputEventReceiver) someArgs2.arg2, 0, true);
                    someArgs2.recycle();
                    return;
                case 8:
                    ViewRootImpl.this.handleAppVisibility(message.arg1 != 0);
                    return;
                case 9:
                    ViewRootImpl.this.handleGetNewSurface();
                    return;
                case 10:
                case 20:
                default:
                    return;
                case 11:
                    if (ViewRootImpl.LOCAL_LOGV || ViewDebugManager.DEBUG_KEY) {
                        Log.v(ViewRootImpl.this.mTag, "Dispatching key " + message.obj + " from IME to " + ViewRootImpl.this.mView + " in " + this);
                    }
                    KeyEvent keyEventChangeFlags = (KeyEvent) message.obj;
                    if ((keyEventChangeFlags.getFlags() & 8) != 0) {
                        keyEventChangeFlags = KeyEvent.changeFlags(keyEventChangeFlags, keyEventChangeFlags.getFlags() & (-9));
                    }
                    ViewRootImpl.this.enqueueInputEvent(keyEventChangeFlags, null, 1, true);
                    return;
                case 12:
                    if (ViewRootImpl.LOCAL_LOGV) {
                        Log.v(ViewRootImpl.TAG, "Dispatching key " + message.obj + " from Autofill to " + ViewRootImpl.this.mView);
                    }
                    ViewRootImpl.this.enqueueInputEvent((KeyEvent) message.obj, null, 0, true);
                    return;
                case 13:
                    InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
                    if (inputMethodManagerPeekInstance != null) {
                        inputMethodManagerPeekInstance.checkFocus();
                        return;
                    }
                    return;
                case 14:
                    if (ViewRootImpl.this.mView != null) {
                        ViewRootImpl.this.mView.onCloseSystemDialogs((String) message.obj);
                        return;
                    }
                    return;
                case 15:
                case 16:
                    DragEvent dragEvent = (DragEvent) message.obj;
                    dragEvent.mLocalState = ViewRootImpl.this.mLocalDragState;
                    ViewRootImpl.this.handleDragEvent(dragEvent);
                    return;
                case 17:
                    ViewRootImpl.this.handleDispatchSystemUiVisibilityChanged((SystemUiVisibilityInfo) message.obj);
                    return;
                case 18:
                    Configuration globalConfiguration = (Configuration) message.obj;
                    if (globalConfiguration.isOtherSeqNewer(ViewRootImpl.this.mLastReportedMergedConfiguration.getMergedConfiguration())) {
                        globalConfiguration = ViewRootImpl.this.mLastReportedMergedConfiguration.getGlobalConfiguration();
                    }
                    ViewRootImpl.this.mPendingMergedConfiguration.setConfiguration(globalConfiguration, ViewRootImpl.this.mLastReportedMergedConfiguration.getOverrideConfiguration());
                    ViewRootImpl.this.performConfigurationChange(ViewRootImpl.this.mPendingMergedConfiguration, false, -1);
                    return;
                case 19:
                    ViewRootImpl.this.mProcessInputEventsScheduled = false;
                    ViewRootImpl.this.doProcessInputEvents();
                    return;
                case 21:
                    ViewRootImpl.this.setAccessibilityFocus(null, null);
                    return;
                case 22:
                    if (ViewRootImpl.this.mView != null) {
                        ViewRootImpl.this.invalidateWorld(ViewRootImpl.this.mView);
                        return;
                    }
                    return;
                case 23:
                    if (ViewRootImpl.this.mAdded) {
                        int iWidth = ViewRootImpl.this.mWinFrame.width();
                        int iHeight = ViewRootImpl.this.mWinFrame.height();
                        int i = message.arg1;
                        int i2 = message.arg2;
                        ViewRootImpl.this.mWinFrame.left = i;
                        ViewRootImpl.this.mWinFrame.right = i + iWidth;
                        ViewRootImpl.this.mWinFrame.top = i2;
                        ViewRootImpl.this.mWinFrame.bottom = i2 + iHeight;
                        ViewRootImpl.this.mPendingBackDropFrame.set(ViewRootImpl.this.mWinFrame);
                        ViewRootImpl.this.maybeHandleWindowMove(ViewRootImpl.this.mWinFrame);
                        return;
                    }
                    return;
                case 24:
                    ViewRootImpl.this.enqueueInputEvent((InputEvent) message.obj, null, 32, true);
                    return;
                case 25:
                    ViewRootImpl.this.handleDispatchWindowShown();
                    return;
                case 26:
                    ViewRootImpl.this.handleRequestKeyboardShortcuts((IResultReceiver) message.obj, message.arg1);
                    return;
                case 27:
                    ViewRootImpl.this.resetPointerIcon((MotionEvent) message.obj);
                    return;
                case 28:
                    ViewRootImpl.this.handlePointerCaptureChanged(message.arg1 != 0);
                    return;
                case 29:
                    ViewRootImpl.this.pendingDrawFinished();
                    return;
            }
            if (ViewRootImpl.this.mAdded) {
                SomeArgs someArgs3 = (SomeArgs) message.obj;
                int i3 = someArgs3.argi3;
                MergedConfiguration mergedConfiguration = (MergedConfiguration) someArgs3.arg4;
                boolean z = ViewRootImpl.this.mDisplay.getDisplayId() != i3;
                if (!ViewRootImpl.this.mLastReportedMergedConfiguration.equals(mergedConfiguration)) {
                    ViewRootImpl.this.performConfigurationChange(mergedConfiguration, false, z ? i3 : -1);
                } else if (z) {
                    ViewRootImpl.this.onMovedToDisplay(i3, ViewRootImpl.this.mLastConfigurationFromResources);
                }
                if (ViewRootImpl.DEBUG_LAYOUT) {
                    Log.d(ViewRootImpl.this.mTag, "Handle RESIZE: message = " + message.what + " ,this = " + ViewRootImpl.this);
                }
                boolean z2 = (ViewRootImpl.this.mWinFrame.equals(someArgs3.arg1) && ViewRootImpl.this.mPendingOverscanInsets.equals(someArgs3.arg5) && ViewRootImpl.this.mPendingContentInsets.equals(someArgs3.arg2) && ViewRootImpl.this.mPendingStableInsets.equals(someArgs3.arg6) && ViewRootImpl.this.mPendingDisplayCutout.get().equals(someArgs3.arg9) && ViewRootImpl.this.mPendingVisibleInsets.equals(someArgs3.arg3) && ViewRootImpl.this.mPendingOutsets.equals(someArgs3.arg7)) ? false : true;
                ViewRootImpl.this.mWinFrame.set((Rect) someArgs3.arg1);
                ViewRootImpl.this.mPendingOverscanInsets.set((Rect) someArgs3.arg5);
                ViewRootImpl.this.mPendingContentInsets.set((Rect) someArgs3.arg2);
                ViewRootImpl.this.mPendingStableInsets.set((Rect) someArgs3.arg6);
                ViewRootImpl.this.mPendingDisplayCutout.set((DisplayCutout) someArgs3.arg9);
                ViewRootImpl.this.mPendingVisibleInsets.set((Rect) someArgs3.arg3);
                ViewRootImpl.this.mPendingOutsets.set((Rect) someArgs3.arg7);
                ViewRootImpl.this.mPendingBackDropFrame.set((Rect) someArgs3.arg8);
                ViewRootImpl.this.mForceNextWindowRelayout = someArgs3.argi1 != 0;
                ViewRootImpl.this.mPendingAlwaysConsumeNavBar = someArgs3.argi2 != 0;
                someArgs3.recycle();
                if (message.what == 5) {
                    ViewRootImpl.this.reportNextDraw();
                }
                if (ViewRootImpl.this.mView != null && z2) {
                    ViewRootImpl.forceLayout(ViewRootImpl.this.mView);
                }
                ViewRootImpl.this.requestLayout();
            }
        }
    }

    boolean ensureTouchMode(boolean z) {
        if (DBG) {
            Log.d("touchmode", "ensureTouchMode(" + z + "), current touch mode is " + this.mAttachInfo.mInTouchMode);
        }
        if (this.mAttachInfo.mInTouchMode == z) {
            return false;
        }
        try {
            this.mWindowSession.setInTouchMode(z);
            return ensureTouchModeLocally(z);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean ensureTouchModeLocally(boolean z) {
        if (DBG) {
            Log.d("touchmode", "ensureTouchModeLocally(" + z + "), current touch mode is " + this.mAttachInfo.mInTouchMode);
        }
        if (this.mAttachInfo.mInTouchMode == z) {
            return false;
        }
        this.mAttachInfo.mInTouchMode = z;
        this.mAttachInfo.mTreeObserver.dispatchOnTouchModeChanged(z);
        return z ? enterTouchMode() : leaveTouchMode();
    }

    private boolean enterTouchMode() {
        View viewFindFocus;
        if (this.mView == null || !this.mView.hasFocus() || (viewFindFocus = this.mView.findFocus()) == null || viewFindFocus.isFocusableInTouchMode()) {
            return false;
        }
        ViewGroup viewGroupFindAncestorToTakeFocusInTouchMode = findAncestorToTakeFocusInTouchMode(viewFindFocus);
        if (viewGroupFindAncestorToTakeFocusInTouchMode != null) {
            return viewGroupFindAncestorToTakeFocusInTouchMode.requestFocus();
        }
        viewFindFocus.clearFocusInternal(null, true, false);
        return true;
    }

    private static ViewGroup findAncestorToTakeFocusInTouchMode(View view) {
        ViewParent parent = view.getParent();
        while (parent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) parent;
            if (viewGroup.getDescendantFocusability() == 262144 && viewGroup.isFocusableInTouchMode()) {
                return viewGroup;
            }
            if (viewGroup.isRootNamespace()) {
                return null;
            }
            parent = viewGroup.getParent();
        }
        return null;
    }

    private boolean leaveTouchMode() {
        if (this.mView == null) {
            return false;
        }
        if (this.mView.hasFocus()) {
            View viewFindFocus = this.mView.findFocus();
            if (!(viewFindFocus instanceof ViewGroup) || ((ViewGroup) viewFindFocus).getDescendantFocusability() != 262144) {
                return false;
            }
        }
        return this.mView.restoreDefaultFocus();
    }

    abstract class InputStage {
        protected static final int FINISH_HANDLED = 1;
        protected static final int FINISH_NOT_HANDLED = 2;
        protected static final int FORWARD = 0;
        private final InputStage mNext;

        public InputStage(InputStage inputStage) {
            this.mNext = inputStage;
        }

        public final void deliver(QueuedInputEvent queuedInputEvent) {
            ViewDebugManager.getInstance().debugInputStageDeliverd(this, System.currentTimeMillis());
            if ((queuedInputEvent.mFlags & 4) != 0) {
                forward(queuedInputEvent);
            } else if (shouldDropInputEvent(queuedInputEvent)) {
                finish(queuedInputEvent, false);
            } else {
                ViewDebugManager.getInstance().debugInputDispatchState(queuedInputEvent.mEvent, toString());
                apply(queuedInputEvent, onProcess(queuedInputEvent));
            }
        }

        protected void finish(QueuedInputEvent queuedInputEvent, boolean z) {
            queuedInputEvent.mFlags |= 4;
            if (z) {
                queuedInputEvent.mFlags |= 8;
            }
            forward(queuedInputEvent);
        }

        protected void forward(QueuedInputEvent queuedInputEvent) {
            onDeliverToNext(queuedInputEvent);
        }

        protected void apply(QueuedInputEvent queuedInputEvent, int i) {
            if (i == 0) {
                forward(queuedInputEvent);
                return;
            }
            if (i == 1) {
                finish(queuedInputEvent, true);
            } else {
                if (i == 2) {
                    finish(queuedInputEvent, false);
                    return;
                }
                throw new IllegalArgumentException("Invalid result: " + i);
            }
        }

        protected int onProcess(QueuedInputEvent queuedInputEvent) {
            return 0;
        }

        protected void onDeliverToNext(QueuedInputEvent queuedInputEvent) {
            if (ViewRootImpl.DEBUG_INPUT_STAGES) {
                Log.v(ViewRootImpl.this.mTag, "Done with " + getClass().getSimpleName() + ". " + queuedInputEvent);
            }
            if (this.mNext == null) {
                ViewRootImpl.this.finishInputEvent(queuedInputEvent);
            } else {
                this.mNext.deliver(queuedInputEvent);
            }
        }

        protected void onWindowFocusChanged(boolean z) {
            if (this.mNext != null) {
                this.mNext.onWindowFocusChanged(z);
            }
        }

        protected void onDetachedFromWindow() {
            if (this.mNext != null) {
                this.mNext.onDetachedFromWindow();
            }
        }

        protected boolean shouldDropInputEvent(QueuedInputEvent queuedInputEvent) {
            if (ViewRootImpl.this.mView == null || !ViewRootImpl.this.mAdded) {
                Slog.w(ViewRootImpl.this.mTag, "Dropping event due to root view being removed: " + queuedInputEvent.mEvent);
                return true;
            }
            if ((ViewRootImpl.this.mAttachInfo.mHasWindowFocus || queuedInputEvent.mEvent.isFromSource(2) || ViewRootImpl.this.isAutofillUiShowing()) && !ViewRootImpl.this.mStopped && ((!ViewRootImpl.this.mIsAmbientMode || queuedInputEvent.mEvent.isFromSource(1)) && (!ViewRootImpl.this.mPausedForTransition || isBack(queuedInputEvent.mEvent)))) {
                return false;
            }
            if (!ViewRootImpl.isTerminalInputEvent(queuedInputEvent.mEvent)) {
                Slog.w(ViewRootImpl.this.mTag, "Dropping event due to no window focus: " + queuedInputEvent.mEvent);
                return true;
            }
            queuedInputEvent.mEvent.cancel();
            Slog.w(ViewRootImpl.this.mTag, "Cancelling event due to no window focus: " + queuedInputEvent.mEvent);
            return false;
        }

        void dump(String str, PrintWriter printWriter) {
            if (this.mNext != null) {
                this.mNext.dump(str, printWriter);
            }
        }

        private boolean isBack(InputEvent inputEvent) {
            return (inputEvent instanceof KeyEvent) && ((KeyEvent) inputEvent).getKeyCode() == 4;
        }
    }

    abstract class AsyncInputStage extends InputStage {
        protected static final int DEFER = 3;
        private QueuedInputEvent mQueueHead;
        private int mQueueLength;
        private QueuedInputEvent mQueueTail;
        private final String mTraceCounter;

        public AsyncInputStage(InputStage inputStage, String str) {
            super(inputStage);
            this.mTraceCounter = str;
        }

        protected void defer(QueuedInputEvent queuedInputEvent) {
            queuedInputEvent.mFlags |= 2;
            enqueue(queuedInputEvent);
        }

        @Override
        protected void forward(QueuedInputEvent queuedInputEvent) {
            QueuedInputEvent queuedInputEvent2;
            queuedInputEvent.mFlags &= -3;
            QueuedInputEvent queuedInputEvent3 = this.mQueueHead;
            if (queuedInputEvent3 == null) {
                super.forward(queuedInputEvent);
                return;
            }
            int deviceId = queuedInputEvent.mEvent.getDeviceId();
            QueuedInputEvent queuedInputEvent4 = null;
            boolean z = false;
            while (true) {
                queuedInputEvent2 = queuedInputEvent4;
                queuedInputEvent4 = queuedInputEvent3;
                if (queuedInputEvent4 == null || queuedInputEvent4 == queuedInputEvent) {
                    break;
                }
                if (!z && deviceId == queuedInputEvent4.mEvent.getDeviceId()) {
                    z = true;
                }
                queuedInputEvent3 = queuedInputEvent4.mNext;
            }
            if (z) {
                if (queuedInputEvent4 == null) {
                    enqueue(queuedInputEvent);
                    return;
                }
                return;
            }
            if (queuedInputEvent4 != null) {
                queuedInputEvent4 = queuedInputEvent4.mNext;
                dequeue(queuedInputEvent, queuedInputEvent2);
            }
            super.forward(queuedInputEvent);
            QueuedInputEvent queuedInputEvent5 = queuedInputEvent2;
            while (true) {
                QueuedInputEvent queuedInputEvent6 = queuedInputEvent4;
                while (queuedInputEvent6 != null) {
                    if (deviceId != queuedInputEvent6.mEvent.getDeviceId()) {
                        QueuedInputEvent queuedInputEvent7 = queuedInputEvent6;
                        queuedInputEvent6 = queuedInputEvent6.mNext;
                        queuedInputEvent5 = queuedInputEvent7;
                    } else if ((queuedInputEvent6.mFlags & 2) == 0) {
                        queuedInputEvent4 = queuedInputEvent6.mNext;
                        dequeue(queuedInputEvent6, queuedInputEvent5);
                        super.forward(queuedInputEvent6);
                    } else {
                        return;
                    }
                }
                return;
            }
        }

        @Override
        protected void apply(QueuedInputEvent queuedInputEvent, int i) {
            if (i == 3) {
                defer(queuedInputEvent);
            } else {
                super.apply(queuedInputEvent, i);
            }
        }

        private void enqueue(QueuedInputEvent queuedInputEvent) {
            if (this.mQueueTail == null) {
                this.mQueueHead = queuedInputEvent;
                this.mQueueTail = queuedInputEvent;
            } else {
                this.mQueueTail.mNext = queuedInputEvent;
                this.mQueueTail = queuedInputEvent;
            }
            this.mQueueLength++;
            Trace.traceCounter(4L, this.mTraceCounter, this.mQueueLength);
        }

        private void dequeue(QueuedInputEvent queuedInputEvent, QueuedInputEvent queuedInputEvent2) {
            if (queuedInputEvent2 == null) {
                this.mQueueHead = queuedInputEvent.mNext;
            } else {
                queuedInputEvent2.mNext = queuedInputEvent.mNext;
            }
            if (this.mQueueTail == queuedInputEvent) {
                this.mQueueTail = queuedInputEvent2;
            }
            queuedInputEvent.mNext = null;
            this.mQueueLength--;
            Trace.traceCounter(4L, this.mTraceCounter, this.mQueueLength);
        }

        @Override
        void dump(String str, PrintWriter printWriter) {
            printWriter.print(str);
            printWriter.print(getClass().getName());
            printWriter.print(": mQueueLength=");
            printWriter.println(this.mQueueLength);
            super.dump(str, printWriter);
        }
    }

    final class NativePreImeInputStage extends AsyncInputStage implements InputQueue.FinishedInputEventCallback {
        public NativePreImeInputStage(InputStage inputStage, String str) {
            super(inputStage, str);
        }

        @Override
        protected int onProcess(QueuedInputEvent queuedInputEvent) {
            if (ViewRootImpl.this.mInputQueue != null && (queuedInputEvent.mEvent instanceof KeyEvent)) {
                ViewRootImpl.this.mInputQueue.sendInputEvent(queuedInputEvent.mEvent, queuedInputEvent, true, this);
                return 3;
            }
            return 0;
        }

        @Override
        public void onFinishedInputEvent(Object obj, boolean z) {
            QueuedInputEvent queuedInputEvent = (QueuedInputEvent) obj;
            if (z) {
                finish(queuedInputEvent, true);
            } else {
                forward(queuedInputEvent);
            }
        }
    }

    final class ViewPreImeInputStage extends InputStage {
        public ViewPreImeInputStage(InputStage inputStage) {
            super(inputStage);
        }

        @Override
        protected int onProcess(QueuedInputEvent queuedInputEvent) {
            if (queuedInputEvent.mEvent instanceof KeyEvent) {
                return processKeyEvent(queuedInputEvent);
            }
            return 0;
        }

        private int processKeyEvent(QueuedInputEvent queuedInputEvent) {
            if (ViewRootImpl.this.mView.dispatchKeyEventPreIme((KeyEvent) queuedInputEvent.mEvent)) {
                return 1;
            }
            return 0;
        }
    }

    final class ImeInputStage extends AsyncInputStage implements InputMethodManager.FinishedInputEventCallback {
        public ImeInputStage(InputStage inputStage, String str) {
            super(inputStage, str);
        }

        @Override
        protected int onProcess(QueuedInputEvent queuedInputEvent) {
            InputMethodManager inputMethodManagerPeekInstance;
            if (!ViewRootImpl.this.mLastWasImTarget || ViewRootImpl.this.isInLocalFocusMode() || (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) == null) {
                return 0;
            }
            InputEvent inputEvent = queuedInputEvent.mEvent;
            if (ViewRootImpl.DEBUG_IMF) {
                Log.v(ViewRootImpl.this.mTag, "Sending input event to IME: " + inputEvent);
            }
            int iDispatchInputEvent = inputMethodManagerPeekInstance.dispatchInputEvent(inputEvent, queuedInputEvent, this, ViewRootImpl.this.mHandler);
            if (iDispatchInputEvent == 1) {
                return 1;
            }
            return iDispatchInputEvent == 0 ? 0 : 3;
        }

        @Override
        public void onFinishedInputEvent(Object obj, boolean z) {
            QueuedInputEvent queuedInputEvent = (QueuedInputEvent) obj;
            if (ViewRootImpl.DEBUG_IMF || ViewDebugManager.DEBUG_INPUT || ViewDebugManager.DEBUG_KEY) {
                Log.d(ViewRootImpl.this.mTag, "IME finishedEvent: handled = " + z + ", event = " + queuedInputEvent + ", viewAncestor = " + this);
            }
            if (z) {
                finish(queuedInputEvent, true);
            } else {
                forward(queuedInputEvent);
            }
        }
    }

    final class EarlyPostImeInputStage extends InputStage {
        public EarlyPostImeInputStage(InputStage inputStage) {
            super(inputStage);
        }

        @Override
        protected int onProcess(QueuedInputEvent queuedInputEvent) {
            if (queuedInputEvent.mEvent instanceof KeyEvent) {
                return processKeyEvent(queuedInputEvent);
            }
            if ((queuedInputEvent.mEvent.getSource() & 2) != 0) {
                return processPointerEvent(queuedInputEvent);
            }
            return 0;
        }

        private int processKeyEvent(QueuedInputEvent queuedInputEvent) {
            KeyEvent keyEvent = (KeyEvent) queuedInputEvent.mEvent;
            if (ViewRootImpl.this.mAttachInfo.mTooltipHost != null) {
                ViewRootImpl.this.mAttachInfo.mTooltipHost.handleTooltipKey(keyEvent);
            }
            if (ViewRootImpl.this.checkForLeavingTouchModeAndConsume(keyEvent)) {
                return 1;
            }
            ViewRootImpl.this.mFallbackEventHandler.preDispatchKeyEvent(keyEvent);
            return 0;
        }

        private int processPointerEvent(QueuedInputEvent queuedInputEvent) {
            AutofillManager autofillManager;
            MotionEvent motionEvent = (MotionEvent) queuedInputEvent.mEvent;
            if (ViewRootImpl.this.mTranslator != null) {
                ViewRootImpl.this.mTranslator.translateEventInScreenToAppWindow(motionEvent);
            }
            int action = motionEvent.getAction();
            if (action == 0 || action == 8) {
                ViewRootImpl.this.ensureTouchMode(motionEvent.isFromSource(4098));
            }
            if (action == 0 && (autofillManager = ViewRootImpl.this.getAutofillManager()) != null) {
                autofillManager.requestHideFillUi();
            }
            if (action == 0 && ViewRootImpl.this.mAttachInfo.mTooltipHost != null) {
                ViewRootImpl.this.mAttachInfo.mTooltipHost.hideTooltip();
            }
            if (ViewRootImpl.this.mCurScrollY != 0) {
                motionEvent.offsetLocation(0.0f, ViewRootImpl.this.mCurScrollY);
            }
            if (motionEvent.isTouchEvent()) {
                ViewRootImpl.this.mLastTouchPoint.x = motionEvent.getRawX();
                ViewRootImpl.this.mLastTouchPoint.y = motionEvent.getRawY();
                ViewRootImpl.this.mLastTouchSource = motionEvent.getSource();
                return 0;
            }
            return 0;
        }
    }

    final class NativePostImeInputStage extends AsyncInputStage implements InputQueue.FinishedInputEventCallback {
        public NativePostImeInputStage(InputStage inputStage, String str) {
            super(inputStage, str);
        }

        @Override
        protected int onProcess(QueuedInputEvent queuedInputEvent) {
            if (ViewRootImpl.this.mInputQueue == null) {
                return 0;
            }
            ViewRootImpl.this.mInputQueue.sendInputEvent(queuedInputEvent.mEvent, queuedInputEvent, false, this);
            return 3;
        }

        @Override
        public void onFinishedInputEvent(Object obj, boolean z) {
            QueuedInputEvent queuedInputEvent = (QueuedInputEvent) obj;
            if (z) {
                finish(queuedInputEvent, true);
            } else {
                forward(queuedInputEvent);
            }
        }
    }

    final class ViewPostImeInputStage extends InputStage {
        public ViewPostImeInputStage(InputStage inputStage) {
            super(inputStage);
        }

        @Override
        protected int onProcess(QueuedInputEvent queuedInputEvent) {
            if (queuedInputEvent.mEvent instanceof KeyEvent) {
                return processKeyEvent(queuedInputEvent);
            }
            int source = queuedInputEvent.mEvent.getSource();
            if ((source & 2) != 0) {
                return processPointerEvent(queuedInputEvent);
            }
            if ((source & 4) != 0) {
                return processTrackballEvent(queuedInputEvent);
            }
            return processGenericMotionEvent(queuedInputEvent);
        }

        @Override
        protected void onDeliverToNext(QueuedInputEvent queuedInputEvent) {
            if (ViewRootImpl.this.mUnbufferedInputDispatch && (queuedInputEvent.mEvent instanceof MotionEvent) && ((MotionEvent) queuedInputEvent.mEvent).isTouchEvent() && ViewRootImpl.isTerminalInputEvent(queuedInputEvent.mEvent)) {
                ViewRootImpl.this.mUnbufferedInputDispatch = false;
                ViewRootImpl.this.scheduleConsumeBatchedInput();
            }
            super.onDeliverToNext(queuedInputEvent);
        }

        private boolean performFocusNavigation(KeyEvent keyEvent) {
            int i;
            int keyCode = keyEvent.getKeyCode();
            if (keyCode != 61) {
                switch (keyCode) {
                    case 19:
                        i = !keyEvent.hasNoModifiers() ? 0 : 33;
                        break;
                    case 20:
                        i = !keyEvent.hasNoModifiers() ? 0 : 130;
                        break;
                    case 21:
                        i = !keyEvent.hasNoModifiers() ? 0 : 17;
                        break;
                    case 22:
                        i = !keyEvent.hasNoModifiers() ? 0 : 66;
                        break;
                    default:
                        i = 0;
                        break;
                }
            } else if (keyEvent.hasNoModifiers()) {
                i = 2;
            } else {
                i = keyEvent.hasModifiers(1) ? 1 : 0;
            }
            if (i != 0) {
                View viewFindFocus = ViewRootImpl.this.mView.findFocus();
                if (viewFindFocus != null) {
                    View viewFocusSearch = viewFindFocus.focusSearch(i);
                    if (viewFocusSearch != null && viewFocusSearch != viewFindFocus) {
                        viewFindFocus.getFocusedRect(ViewRootImpl.this.mTempRect);
                        if (ViewRootImpl.this.mView instanceof ViewGroup) {
                            ((ViewGroup) ViewRootImpl.this.mView).offsetDescendantRectToMyCoords(viewFindFocus, ViewRootImpl.this.mTempRect);
                            ((ViewGroup) ViewRootImpl.this.mView).offsetRectIntoDescendantCoords(viewFocusSearch, ViewRootImpl.this.mTempRect);
                        }
                        if (viewFocusSearch.requestFocus(i, ViewRootImpl.this.mTempRect)) {
                            ViewRootImpl.this.playSoundEffect(SoundEffectConstants.getContantForFocusDirection(i));
                            return true;
                        }
                    }
                    if (ViewRootImpl.this.mView.dispatchUnhandledMove(viewFindFocus, i)) {
                        return true;
                    }
                } else if (ViewRootImpl.this.mView.restoreDefaultFocus()) {
                    return true;
                }
            }
            return false;
        }

        private boolean performKeyboardGroupNavigation(int i) {
            View viewKeyboardNavigationClusterSearch;
            int i2;
            View viewFindFocus = ViewRootImpl.this.mView.findFocus();
            if (viewFindFocus == null && ViewRootImpl.this.mView.restoreDefaultFocus()) {
                return true;
            }
            if (viewFindFocus == null) {
                viewKeyboardNavigationClusterSearch = ViewRootImpl.this.keyboardNavigationClusterSearch(null, i);
            } else {
                viewKeyboardNavigationClusterSearch = viewFindFocus.keyboardNavigationClusterSearch(null, i);
            }
            if (i == 2 || i == 1) {
                i2 = 130;
            } else {
                i2 = i;
            }
            if (viewKeyboardNavigationClusterSearch != null && viewKeyboardNavigationClusterSearch.isRootNamespace()) {
                if (!viewKeyboardNavigationClusterSearch.restoreFocusNotInCluster()) {
                    viewKeyboardNavigationClusterSearch = ViewRootImpl.this.keyboardNavigationClusterSearch(null, i);
                } else {
                    ViewRootImpl.this.playSoundEffect(SoundEffectConstants.getContantForFocusDirection(i));
                    return true;
                }
            }
            if (viewKeyboardNavigationClusterSearch != null && viewKeyboardNavigationClusterSearch.restoreFocusInCluster(i2)) {
                ViewRootImpl.this.playSoundEffect(SoundEffectConstants.getContantForFocusDirection(i));
                return true;
            }
            return false;
        }

        private int processKeyEvent(QueuedInputEvent queuedInputEvent) {
            int i;
            KeyEvent keyEvent = (KeyEvent) queuedInputEvent.mEvent;
            if (ViewRootImpl.this.mUnhandledKeyManager.preViewDispatch(keyEvent)) {
                if (ViewDebugManager.DEBUG_ENG) {
                    Log.v(ViewRootImpl.this.mTag, "App handle dispatchUnique event = " + keyEvent + ", mView = " + ViewRootImpl.this.mView + ", this = " + this);
                }
                return 1;
            }
            if (ViewRootImpl.this.mView.dispatchKeyEvent(keyEvent)) {
                if (ViewDebugManager.DEBUG_ENG) {
                    Log.v(ViewRootImpl.this.mTag, "App handle key event: event = " + keyEvent + ", mView = " + ViewRootImpl.this.mView + ", this = " + this);
                }
                return 1;
            }
            if (shouldDropInputEvent(queuedInputEvent)) {
                return 2;
            }
            if (ViewRootImpl.this.mUnhandledKeyManager.dispatch(ViewRootImpl.this.mView, keyEvent)) {
                return 1;
            }
            if (keyEvent.getAction() != 0 || keyEvent.getKeyCode() != 61) {
                i = 0;
            } else if (!KeyEvent.metaStateHasModifiers(keyEvent.getMetaState(), 65536)) {
                if (KeyEvent.metaStateHasModifiers(keyEvent.getMetaState(), 65537)) {
                    i = 1;
                }
            } else {
                i = 2;
            }
            if (keyEvent.getAction() == 0 && !KeyEvent.metaStateHasNoModifiers(keyEvent.getMetaState()) && keyEvent.getRepeatCount() == 0 && !KeyEvent.isModifierKey(keyEvent.getKeyCode()) && i == 0) {
                if (ViewRootImpl.this.mView.dispatchKeyShortcutEvent(keyEvent)) {
                    return 1;
                }
                if (shouldDropInputEvent(queuedInputEvent)) {
                    return 2;
                }
            }
            if (ViewRootImpl.this.mFallbackEventHandler.dispatchKeyEvent(keyEvent)) {
                return 1;
            }
            if (shouldDropInputEvent(queuedInputEvent)) {
                return 2;
            }
            if (keyEvent.getAction() == 0) {
                if (i != 0) {
                    if (performKeyboardGroupNavigation(i)) {
                        return 1;
                    }
                } else if (performFocusNavigation(keyEvent)) {
                    return 1;
                }
            }
            return 0;
        }

        private int processPointerEvent(QueuedInputEvent queuedInputEvent) {
            MotionEvent motionEvent = (MotionEvent) queuedInputEvent.mEvent;
            ViewRootImpl.this.mAttachInfo.mUnbufferedDispatchRequested = false;
            ViewRootImpl.this.mAttachInfo.mHandlingPointerEvent = true;
            boolean zDispatchPointerEvent = ViewRootImpl.this.mView.dispatchPointerEvent(motionEvent);
            if (zDispatchPointerEvent && ViewDebugManager.DEBUG_ENG) {
                Log.v(ViewRootImpl.this.mTag, "App handle pointer event: event = " + motionEvent + ", mView = " + ViewRootImpl.this.mView + ", this = " + this);
            }
            maybeUpdatePointerIcon(motionEvent);
            ViewRootImpl.this.maybeUpdateTooltip(motionEvent);
            ViewRootImpl.this.mAttachInfo.mHandlingPointerEvent = false;
            if (ViewRootImpl.this.mAttachInfo.mUnbufferedDispatchRequested && !ViewRootImpl.this.mUnbufferedInputDispatch) {
                ViewRootImpl.this.mUnbufferedInputDispatch = true;
                if (ViewRootImpl.this.mConsumeBatchedInputScheduled) {
                    ViewRootImpl.this.scheduleConsumeBatchedInputImmediately();
                }
            }
            return zDispatchPointerEvent ? 1 : 0;
        }

        private void maybeUpdatePointerIcon(MotionEvent motionEvent) {
            if (motionEvent.getPointerCount() == 1 && motionEvent.isFromSource(8194)) {
                if (motionEvent.getActionMasked() == 9 || motionEvent.getActionMasked() == 10) {
                    ViewRootImpl.this.mPointerIconType = 1;
                }
                if (motionEvent.getActionMasked() != 10 && !ViewRootImpl.this.updatePointerIcon(motionEvent) && motionEvent.getActionMasked() == 7) {
                    ViewRootImpl.this.mPointerIconType = 1;
                }
            }
        }

        private int processTrackballEvent(QueuedInputEvent queuedInputEvent) {
            MotionEvent motionEvent = (MotionEvent) queuedInputEvent.mEvent;
            return ((!motionEvent.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE) || (ViewRootImpl.this.hasPointerCapture() && !ViewRootImpl.this.mView.dispatchCapturedPointerEvent(motionEvent))) && !ViewRootImpl.this.mView.dispatchTrackballEvent(motionEvent)) ? 0 : 1;
        }

        private int processGenericMotionEvent(QueuedInputEvent queuedInputEvent) {
            if (ViewRootImpl.this.mView.dispatchGenericMotionEvent((MotionEvent) queuedInputEvent.mEvent)) {
                return 1;
            }
            return 0;
        }
    }

    private void resetPointerIcon(MotionEvent motionEvent) {
        this.mPointerIconType = 1;
        updatePointerIcon(motionEvent);
    }

    private boolean updatePointerIcon(MotionEvent motionEvent) {
        float x = motionEvent.getX(0);
        float y = motionEvent.getY(0);
        if (this.mView == null) {
            Slog.d(this.mTag, "updatePointerIcon called after view was removed");
            return false;
        }
        if (x >= 0.0f && x < this.mView.getWidth() && y >= 0.0f && y < this.mView.getHeight()) {
            PointerIcon pointerIconOnResolvePointerIcon = this.mView.onResolvePointerIcon(motionEvent, 0);
            int type = pointerIconOnResolvePointerIcon != null ? pointerIconOnResolvePointerIcon.getType() : 1000;
            if (this.mPointerIconType != type) {
                this.mPointerIconType = type;
                this.mCustomPointerIcon = null;
                if (this.mPointerIconType != -1) {
                    InputManager.getInstance().setPointerIconType(type);
                    return true;
                }
            }
            if (this.mPointerIconType == -1 && !pointerIconOnResolvePointerIcon.equals(this.mCustomPointerIcon)) {
                this.mCustomPointerIcon = pointerIconOnResolvePointerIcon;
                InputManager.getInstance().setCustomPointerIcon(this.mCustomPointerIcon);
            }
            return true;
        }
        Slog.d(this.mTag, "updatePointerIcon called with position out of bounds");
        return false;
    }

    private void maybeUpdateTooltip(MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() != 1) {
            return;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 9 && actionMasked != 7 && actionMasked != 10) {
            return;
        }
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(this.mContext);
        if (accessibilityManager.isEnabled() && accessibilityManager.isTouchExplorationEnabled()) {
            return;
        }
        if (this.mView == null) {
            Slog.d(this.mTag, "maybeUpdateTooltip called after view was removed");
        } else {
            this.mView.dispatchTooltipHoverEvent(motionEvent);
        }
    }

    final class SyntheticInputStage extends InputStage {
        private final SyntheticJoystickHandler mJoystick;
        private final SyntheticKeyboardHandler mKeyboard;
        private final SyntheticTouchNavigationHandler mTouchNavigation;
        private final SyntheticTrackballHandler mTrackball;

        public SyntheticInputStage() {
            super(null);
            this.mTrackball = ViewRootImpl.this.new SyntheticTrackballHandler();
            this.mJoystick = ViewRootImpl.this.new SyntheticJoystickHandler();
            this.mTouchNavigation = ViewRootImpl.this.new SyntheticTouchNavigationHandler();
            this.mKeyboard = ViewRootImpl.this.new SyntheticKeyboardHandler();
        }

        @Override
        protected int onProcess(QueuedInputEvent queuedInputEvent) {
            queuedInputEvent.mFlags |= 16;
            if (!(queuedInputEvent.mEvent instanceof MotionEvent)) {
                if ((queuedInputEvent.mFlags & 32) != 0) {
                    this.mKeyboard.process((KeyEvent) queuedInputEvent.mEvent);
                    return 1;
                }
                return 0;
            }
            MotionEvent motionEvent = (MotionEvent) queuedInputEvent.mEvent;
            int source = motionEvent.getSource();
            if ((source & 4) != 0) {
                this.mTrackball.process(motionEvent);
                return 1;
            }
            if ((source & 16) != 0) {
                this.mJoystick.process(motionEvent);
                return 1;
            }
            if ((source & 2097152) == 2097152) {
                this.mTouchNavigation.process(motionEvent);
                return 1;
            }
            return 0;
        }

        @Override
        protected void onDeliverToNext(QueuedInputEvent queuedInputEvent) {
            if ((queuedInputEvent.mFlags & 16) == 0 && (queuedInputEvent.mEvent instanceof MotionEvent)) {
                MotionEvent motionEvent = (MotionEvent) queuedInputEvent.mEvent;
                int source = motionEvent.getSource();
                if ((source & 4) != 0) {
                    this.mTrackball.cancel();
                } else if ((source & 16) == 0) {
                    if ((source & 2097152) == 2097152) {
                        this.mTouchNavigation.cancel(motionEvent);
                    }
                } else {
                    this.mJoystick.cancel();
                }
            }
            super.onDeliverToNext(queuedInputEvent);
        }

        @Override
        protected void onWindowFocusChanged(boolean z) {
            if (z) {
                return;
            }
            this.mJoystick.cancel();
        }

        @Override
        protected void onDetachedFromWindow() {
            this.mJoystick.cancel();
        }
    }

    final class SyntheticTrackballHandler {
        private long mLastTime;
        private final TrackballAxis mX = new TrackballAxis();
        private final TrackballAxis mY = new TrackballAxis();

        SyntheticTrackballHandler() {
        }

        public void process(MotionEvent motionEvent) {
            int i;
            long j;
            int i2;
            int iGenerate;
            int i3;
            int i4;
            long jUptimeMillis;
            long jUptimeMillis2 = SystemClock.uptimeMillis();
            if (this.mLastTime + 250 < jUptimeMillis2) {
                this.mX.reset(0);
                this.mY.reset(0);
                this.mLastTime = jUptimeMillis2;
            }
            int action = motionEvent.getAction();
            int metaState = motionEvent.getMetaState();
            switch (action) {
                case 0:
                    i = 0;
                    this.mX.reset(2);
                    this.mY.reset(2);
                    j = jUptimeMillis2;
                    i2 = 2;
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(jUptimeMillis2, jUptimeMillis2, 0, 23, 0, metaState, -1, 0, 1024, 257));
                    break;
                case 1:
                    this.mX.reset(2);
                    this.mY.reset(2);
                    i = 0;
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(jUptimeMillis2, jUptimeMillis2, 1, 23, 0, metaState, -1, 0, 1024, 257));
                    j = jUptimeMillis2;
                    i2 = 2;
                    break;
                default:
                    i = 0;
                    j = jUptimeMillis2;
                    i2 = 2;
                    break;
            }
            if (ViewRootImpl.DEBUG_TRACKBALL) {
                Log.v(ViewRootImpl.this.mTag, "TB X=" + this.mX.position + " step=" + this.mX.step + " dir=" + this.mX.dir + " acc=" + this.mX.acceleration + " move=" + motionEvent.getX() + " / Y=" + this.mY.position + " step=" + this.mY.step + " dir=" + this.mY.dir + " acc=" + this.mY.acceleration + " move=" + motionEvent.getY());
            }
            float fCollect = this.mX.collect(motionEvent.getX(), motionEvent.getEventTime(), "X");
            float fCollect2 = this.mY.collect(motionEvent.getY(), motionEvent.getEventTime(), "Y");
            float f = 1.0f;
            if (fCollect > fCollect2) {
                iGenerate = this.mX.generate();
                if (iGenerate != 0) {
                    i4 = iGenerate > 0 ? 22 : 21;
                    f = this.mX.acceleration;
                    this.mY.reset(i2);
                    i3 = i4;
                }
                i3 = i;
            } else if (fCollect2 > 0.0f) {
                iGenerate = this.mY.generate();
                if (iGenerate != 0) {
                    i4 = iGenerate > 0 ? 20 : 19;
                    f = this.mY.acceleration;
                    this.mX.reset(i2);
                    i3 = i4;
                }
                i3 = i;
            } else {
                iGenerate = i;
                i3 = iGenerate;
            }
            if (i3 != 0) {
                if (iGenerate < 0) {
                    iGenerate = -iGenerate;
                }
                int i5 = (int) (iGenerate * f);
                if (ViewRootImpl.DEBUG_TRACKBALL) {
                    Log.v(ViewRootImpl.this.mTag, "Move: movement=" + iGenerate + " accelMovement=" + i5 + " accel=" + f);
                }
                if (i5 > iGenerate) {
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.this.mTag, "Delivering fake DPAD: " + i3);
                    }
                    int i6 = iGenerate - 1;
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(j, j, 2, i3, i5 - i6, metaState, -1, 0, 1024, 257));
                    jUptimeMillis = j;
                    iGenerate = i6;
                } else {
                    jUptimeMillis = j;
                }
                while (iGenerate > 0) {
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.this.mTag, "Delivering fake DPAD: " + i3);
                    }
                    iGenerate--;
                    jUptimeMillis = SystemClock.uptimeMillis();
                    int i7 = i3;
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, i7, 0, metaState, -1, 0, 1024, 257));
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 1, i7, 0, metaState, -1, 0, 1024, 257));
                }
                this.mLastTime = jUptimeMillis;
            }
        }

        public void cancel() {
            this.mLastTime = -2147483648L;
            if (ViewRootImpl.this.mView != null && ViewRootImpl.this.mAdded) {
                ViewRootImpl.this.ensureTouchMode(false);
            }
        }
    }

    static final class TrackballAxis {
        static final float ACCEL_MOVE_SCALING_FACTOR = 0.025f;
        static final long FAST_MOVE_TIME = 150;
        static final float FIRST_MOVEMENT_THRESHOLD = 0.5f;
        static final float MAX_ACCELERATION = 20.0f;
        static final float SECOND_CUMULATIVE_MOVEMENT_THRESHOLD = 2.0f;
        static final float SUBSEQUENT_INCREMENTAL_MOVEMENT_THRESHOLD = 1.0f;
        int dir;
        int nonAccelMovement;
        float position;
        int step;
        float acceleration = 1.0f;
        long lastMoveTime = 0;

        TrackballAxis() {
        }

        void reset(int i) {
            this.position = 0.0f;
            this.acceleration = 1.0f;
            this.lastMoveTime = 0L;
            this.step = i;
            this.dir = 0;
        }

        float collect(float f, long j, String str) {
            long j2;
            if (f > 0.0f) {
                j2 = (long) (150.0f * f);
                if (this.dir < 0) {
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.TAG, str + " reversed to positive!");
                    }
                    this.position = 0.0f;
                    this.step = 0;
                    this.acceleration = 1.0f;
                    this.lastMoveTime = 0L;
                }
                this.dir = 1;
            } else if (f < 0.0f) {
                j2 = (long) ((-f) * 150.0f);
                if (this.dir > 0) {
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.TAG, str + " reversed to negative!");
                    }
                    this.position = 0.0f;
                    this.step = 0;
                    this.acceleration = 1.0f;
                    this.lastMoveTime = 0L;
                }
                this.dir = -1;
            } else {
                j2 = 0;
            }
            if (j2 > 0) {
                long j3 = j - this.lastMoveTime;
                this.lastMoveTime = j;
                float f2 = this.acceleration;
                if (j3 < j2) {
                    float f3 = (j2 - j3) * ACCEL_MOVE_SCALING_FACTOR;
                    if (f3 > 1.0f) {
                        f2 *= f3;
                    }
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.TAG, str + " accelerate: off=" + f + " normTime=" + j2 + " delta=" + j3 + " scale=" + f3 + " acc=" + f2);
                    }
                    if (f2 >= MAX_ACCELERATION) {
                        f2 = 20.0f;
                    }
                    this.acceleration = f2;
                } else {
                    float f4 = (j3 - j2) * ACCEL_MOVE_SCALING_FACTOR;
                    if (f4 > 1.0f) {
                        f2 /= f4;
                    }
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.TAG, str + " deccelerate: off=" + f + " normTime=" + j2 + " delta=" + j3 + " scale=" + f4 + " acc=" + f2);
                    }
                    if (f2 <= 1.0f) {
                        f2 = 1.0f;
                    }
                    this.acceleration = f2;
                }
            }
            this.position += f;
            return Math.abs(this.position);
        }

        int generate() {
            int i;
            int i2 = 0;
            this.nonAccelMovement = 0;
            while (true) {
                if (this.position < 0.0f) {
                    i = -1;
                } else {
                    i = 1;
                }
                switch (this.step) {
                    case 0:
                        if (Math.abs(this.position) < FIRST_MOVEMENT_THRESHOLD) {
                            return i2;
                        }
                        i2 += i;
                        this.nonAccelMovement += i;
                        this.step = 1;
                        break;
                        break;
                    case 1:
                        if (Math.abs(this.position) < SECOND_CUMULATIVE_MOVEMENT_THRESHOLD) {
                            return i2;
                        }
                        i2 += i;
                        this.nonAccelMovement += i;
                        this.position -= SECOND_CUMULATIVE_MOVEMENT_THRESHOLD * i;
                        this.step = 2;
                        break;
                        break;
                    default:
                        if (Math.abs(this.position) < 1.0f) {
                            return i2;
                        }
                        i2 += i;
                        this.position -= i * 1.0f;
                        float f = this.acceleration * 1.1f;
                        if (f >= MAX_ACCELERATION) {
                            f = this.acceleration;
                        }
                        this.acceleration = f;
                        break;
                        break;
                }
            }
        }
    }

    final class SyntheticJoystickHandler extends Handler {
        private static final int MSG_ENQUEUE_X_AXIS_KEY_REPEAT = 1;
        private static final int MSG_ENQUEUE_Y_AXIS_KEY_REPEAT = 2;
        private final SparseArray<KeyEvent> mDeviceKeyEvents;
        private final JoystickAxesState mJoystickAxesState;

        public SyntheticJoystickHandler() {
            super(true);
            this.mJoystickAxesState = new JoystickAxesState();
            this.mDeviceKeyEvents = new SparseArray<>();
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                case 2:
                    if (ViewRootImpl.this.mAttachInfo.mHasWindowFocus) {
                        KeyEvent keyEvent = (KeyEvent) message.obj;
                        KeyEvent keyEventChangeTimeRepeat = KeyEvent.changeTimeRepeat(keyEvent, SystemClock.uptimeMillis(), keyEvent.getRepeatCount() + 1);
                        ViewRootImpl.this.enqueueInputEvent(keyEventChangeTimeRepeat);
                        Message messageObtainMessage = obtainMessage(message.what, keyEventChangeTimeRepeat);
                        messageObtainMessage.setAsynchronous(true);
                        sendMessageDelayed(messageObtainMessage, ViewConfiguration.getKeyRepeatDelay());
                    }
                    break;
            }
        }

        public void process(MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case 2:
                    update(motionEvent);
                    break;
                case 3:
                    cancel();
                    break;
                default:
                    Log.w(ViewRootImpl.this.mTag, "Unexpected action: " + motionEvent.getActionMasked());
                    break;
            }
        }

        private void cancel() {
            removeMessages(1);
            removeMessages(2);
            for (int i = 0; i < this.mDeviceKeyEvents.size(); i++) {
                KeyEvent keyEventValueAt = this.mDeviceKeyEvents.valueAt(i);
                if (keyEventValueAt != null) {
                    ViewRootImpl.this.enqueueInputEvent(KeyEvent.changeTimeRepeat(keyEventValueAt, SystemClock.uptimeMillis(), 0));
                }
            }
            this.mDeviceKeyEvents.clear();
            this.mJoystickAxesState.resetState();
        }

        private void update(MotionEvent motionEvent) {
            int historySize = motionEvent.getHistorySize();
            for (int i = 0; i < historySize; i++) {
                long historicalEventTime = motionEvent.getHistoricalEventTime(i);
                this.mJoystickAxesState.updateStateForAxis(motionEvent, historicalEventTime, 0, motionEvent.getHistoricalAxisValue(0, 0, i));
                this.mJoystickAxesState.updateStateForAxis(motionEvent, historicalEventTime, 1, motionEvent.getHistoricalAxisValue(1, 0, i));
                this.mJoystickAxesState.updateStateForAxis(motionEvent, historicalEventTime, 15, motionEvent.getHistoricalAxisValue(15, 0, i));
                this.mJoystickAxesState.updateStateForAxis(motionEvent, historicalEventTime, 16, motionEvent.getHistoricalAxisValue(16, 0, i));
            }
            long eventTime = motionEvent.getEventTime();
            this.mJoystickAxesState.updateStateForAxis(motionEvent, eventTime, 0, motionEvent.getAxisValue(0));
            this.mJoystickAxesState.updateStateForAxis(motionEvent, eventTime, 1, motionEvent.getAxisValue(1));
            this.mJoystickAxesState.updateStateForAxis(motionEvent, eventTime, 15, motionEvent.getAxisValue(15));
            this.mJoystickAxesState.updateStateForAxis(motionEvent, eventTime, 16, motionEvent.getAxisValue(16));
        }

        final class JoystickAxesState {
            private static final int STATE_DOWN_OR_RIGHT = 1;
            private static final int STATE_NEUTRAL = 0;
            private static final int STATE_UP_OR_LEFT = -1;
            final int[] mAxisStatesHat = {0, 0};
            final int[] mAxisStatesStick = {0, 0};

            JoystickAxesState() {
            }

            void resetState() {
                this.mAxisStatesHat[0] = 0;
                this.mAxisStatesHat[1] = 0;
                this.mAxisStatesStick[0] = 0;
                this.mAxisStatesStick[1] = 0;
            }

            void updateStateForAxis(MotionEvent motionEvent, long j, int i, float f) {
                int i2;
                char c;
                int i3;
                int iJoystickAxisAndStateToKeycode;
                if (isXAxis(i)) {
                    c = 0;
                    i2 = 1;
                } else {
                    if (!isYAxis(i)) {
                        Log.e(ViewRootImpl.this.mTag, "Unexpected axis " + i + " in updateStateForAxis!");
                        return;
                    }
                    i2 = 2;
                    c = 1;
                }
                int iJoystickAxisValueToState = joystickAxisValueToState(f);
                if (i == 0 || i == 1) {
                    i3 = this.mAxisStatesStick[c];
                } else {
                    i3 = this.mAxisStatesHat[c];
                }
                if (i3 == iJoystickAxisValueToState) {
                    return;
                }
                int metaState = motionEvent.getMetaState();
                int deviceId = motionEvent.getDeviceId();
                int source = motionEvent.getSource();
                if (i3 == 1 || i3 == -1) {
                    int iJoystickAxisAndStateToKeycode2 = joystickAxisAndStateToKeycode(i, i3);
                    if (iJoystickAxisAndStateToKeycode2 != 0) {
                        ViewRootImpl.this.enqueueInputEvent(new KeyEvent(j, j, 1, iJoystickAxisAndStateToKeycode2, 0, metaState, deviceId, 0, 1024, source));
                        deviceId = deviceId;
                        SyntheticJoystickHandler.this.mDeviceKeyEvents.put(deviceId, null);
                    }
                    SyntheticJoystickHandler.this.removeMessages(i2);
                }
                if ((iJoystickAxisValueToState == 1 || iJoystickAxisValueToState == -1) && (iJoystickAxisAndStateToKeycode = joystickAxisAndStateToKeycode(i, iJoystickAxisValueToState)) != 0) {
                    int i4 = deviceId;
                    KeyEvent keyEvent = new KeyEvent(j, j, 0, iJoystickAxisAndStateToKeycode, 0, metaState, i4, 0, 1024, source);
                    ViewRootImpl.this.enqueueInputEvent(keyEvent);
                    Message messageObtainMessage = SyntheticJoystickHandler.this.obtainMessage(i2, keyEvent);
                    messageObtainMessage.setAsynchronous(true);
                    SyntheticJoystickHandler.this.sendMessageDelayed(messageObtainMessage, ViewConfiguration.getKeyRepeatTimeout());
                    SyntheticJoystickHandler.this.mDeviceKeyEvents.put(i4, new KeyEvent(j, j, 1, iJoystickAxisAndStateToKeycode, 0, metaState, i4, 0, BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO, source));
                }
                if (i == 0 || i == 1) {
                    this.mAxisStatesStick[c] = iJoystickAxisValueToState;
                } else {
                    this.mAxisStatesHat[c] = iJoystickAxisValueToState;
                }
            }

            private boolean isXAxis(int i) {
                return i == 0 || i == 15;
            }

            private boolean isYAxis(int i) {
                return i == 1 || i == 16;
            }

            private int joystickAxisAndStateToKeycode(int i, int i2) {
                if (isXAxis(i) && i2 == -1) {
                    return 21;
                }
                if (isXAxis(i) && i2 == 1) {
                    return 22;
                }
                if (isYAxis(i) && i2 == -1) {
                    return 19;
                }
                if (!isYAxis(i) || i2 != 1) {
                    Log.e(ViewRootImpl.this.mTag, "Unknown axis " + i + " or direction " + i2);
                    return 0;
                }
                return 20;
            }

            private int joystickAxisValueToState(float f) {
                if (f >= 0.5f) {
                    return 1;
                }
                if (f <= -0.5f) {
                    return -1;
                }
                return 0;
            }
        }
    }

    final class SyntheticTouchNavigationHandler extends Handler {
        private static final float DEFAULT_HEIGHT_MILLIMETERS = 48.0f;
        private static final float DEFAULT_WIDTH_MILLIMETERS = 48.0f;
        private static final float FLING_TICK_DECAY = 0.8f;
        private static final boolean LOCAL_DEBUG = false;
        private static final String LOCAL_TAG = "SyntheticTouchNavigationHandler";
        private static final float MAX_FLING_VELOCITY_TICKS_PER_SECOND = 20.0f;
        private static final float MIN_FLING_VELOCITY_TICKS_PER_SECOND = 6.0f;
        private static final int TICK_DISTANCE_MILLIMETERS = 12;
        private float mAccumulatedX;
        private float mAccumulatedY;
        private int mActivePointerId;
        private float mConfigMaxFlingVelocity;
        private float mConfigMinFlingVelocity;
        private float mConfigTickDistance;
        private boolean mConsumedMovement;
        private int mCurrentDeviceId;
        private boolean mCurrentDeviceSupported;
        private int mCurrentSource;
        private final Runnable mFlingRunnable;
        private float mFlingVelocity;
        private boolean mFlinging;
        private float mLastX;
        private float mLastY;
        private int mPendingKeyCode;
        private long mPendingKeyDownTime;
        private int mPendingKeyMetaState;
        private int mPendingKeyRepeatCount;
        private float mStartX;
        private float mStartY;
        private VelocityTracker mVelocityTracker;

        static float access$2932(SyntheticTouchNavigationHandler syntheticTouchNavigationHandler, float f) {
            float f2 = syntheticTouchNavigationHandler.mFlingVelocity * f;
            syntheticTouchNavigationHandler.mFlingVelocity = f2;
            return f2;
        }

        public SyntheticTouchNavigationHandler() {
            super(true);
            this.mCurrentDeviceId = -1;
            this.mActivePointerId = -1;
            this.mPendingKeyCode = 0;
            this.mFlingRunnable = new Runnable() {
                @Override
                public void run() {
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    SyntheticTouchNavigationHandler.this.sendKeyDownOrRepeat(jUptimeMillis, SyntheticTouchNavigationHandler.this.mPendingKeyCode, SyntheticTouchNavigationHandler.this.mPendingKeyMetaState);
                    SyntheticTouchNavigationHandler.access$2932(SyntheticTouchNavigationHandler.this, SyntheticTouchNavigationHandler.FLING_TICK_DECAY);
                    if (!SyntheticTouchNavigationHandler.this.postFling(jUptimeMillis)) {
                        SyntheticTouchNavigationHandler.this.mFlinging = false;
                        SyntheticTouchNavigationHandler.this.finishKeys(jUptimeMillis);
                    }
                }
            };
        }

        public void process(MotionEvent motionEvent) {
            long eventTime = motionEvent.getEventTime();
            int deviceId = motionEvent.getDeviceId();
            int source = motionEvent.getSource();
            if (this.mCurrentDeviceId != deviceId || this.mCurrentSource != source) {
                finishKeys(eventTime);
                finishTracking(eventTime);
                this.mCurrentDeviceId = deviceId;
                this.mCurrentSource = source;
                this.mCurrentDeviceSupported = false;
                InputDevice device = motionEvent.getDevice();
                if (device != null) {
                    InputDevice.MotionRange motionRange = device.getMotionRange(0);
                    InputDevice.MotionRange motionRange2 = device.getMotionRange(1);
                    if (motionRange != null && motionRange2 != null) {
                        this.mCurrentDeviceSupported = true;
                        float resolution = motionRange.getResolution();
                        if (resolution <= 0.0f) {
                            resolution = motionRange.getRange() / 48.0f;
                        }
                        float resolution2 = motionRange2.getResolution();
                        if (resolution2 <= 0.0f) {
                            resolution2 = motionRange2.getRange() / 48.0f;
                        }
                        this.mConfigTickDistance = 12.0f * (resolution + resolution2) * 0.5f;
                        this.mConfigMinFlingVelocity = MIN_FLING_VELOCITY_TICKS_PER_SECOND * this.mConfigTickDistance;
                        this.mConfigMaxFlingVelocity = MAX_FLING_VELOCITY_TICKS_PER_SECOND * this.mConfigTickDistance;
                    }
                }
            }
            if (!this.mCurrentDeviceSupported) {
            }
            int actionMasked = motionEvent.getActionMasked();
            switch (actionMasked) {
                case 0:
                    boolean z = this.mFlinging;
                    finishKeys(eventTime);
                    finishTracking(eventTime);
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    this.mVelocityTracker = VelocityTracker.obtain();
                    this.mVelocityTracker.addMovement(motionEvent);
                    this.mStartX = motionEvent.getX();
                    this.mStartY = motionEvent.getY();
                    this.mLastX = this.mStartX;
                    this.mLastY = this.mStartY;
                    this.mAccumulatedX = 0.0f;
                    this.mAccumulatedY = 0.0f;
                    this.mConsumedMovement = z;
                    break;
                case 1:
                case 2:
                    if (this.mActivePointerId >= 0) {
                        int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                        if (iFindPointerIndex < 0) {
                            finishKeys(eventTime);
                            finishTracking(eventTime);
                        } else {
                            this.mVelocityTracker.addMovement(motionEvent);
                            float x = motionEvent.getX(iFindPointerIndex);
                            float y = motionEvent.getY(iFindPointerIndex);
                            this.mAccumulatedX += x - this.mLastX;
                            this.mAccumulatedY += y - this.mLastY;
                            this.mLastX = x;
                            this.mLastY = y;
                            consumeAccumulatedMovement(eventTime, motionEvent.getMetaState());
                            if (actionMasked == 1) {
                                if (this.mConsumedMovement && this.mPendingKeyCode != 0) {
                                    this.mVelocityTracker.computeCurrentVelocity(1000, this.mConfigMaxFlingVelocity);
                                    if (!startFling(eventTime, this.mVelocityTracker.getXVelocity(this.mActivePointerId), this.mVelocityTracker.getYVelocity(this.mActivePointerId))) {
                                        finishKeys(eventTime);
                                    }
                                }
                                finishTracking(eventTime);
                            }
                        }
                        break;
                    }
                    break;
                case 3:
                    finishKeys(eventTime);
                    finishTracking(eventTime);
                    break;
            }
        }

        public void cancel(MotionEvent motionEvent) {
            if (this.mCurrentDeviceId == motionEvent.getDeviceId() && this.mCurrentSource == motionEvent.getSource()) {
                long eventTime = motionEvent.getEventTime();
                finishKeys(eventTime);
                finishTracking(eventTime);
            }
        }

        private void finishKeys(long j) {
            cancelFling();
            sendKeyUp(j);
        }

        private void finishTracking(long j) {
            if (this.mActivePointerId >= 0) {
                this.mActivePointerId = -1;
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
            }
        }

        private void consumeAccumulatedMovement(long j, int i) {
            float fAbs = Math.abs(this.mAccumulatedX);
            float fAbs2 = Math.abs(this.mAccumulatedY);
            if (fAbs >= fAbs2) {
                if (fAbs >= this.mConfigTickDistance) {
                    this.mAccumulatedX = consumeAccumulatedMovement(j, i, this.mAccumulatedX, 21, 22);
                    this.mAccumulatedY = 0.0f;
                    this.mConsumedMovement = true;
                    return;
                }
                return;
            }
            if (fAbs2 >= this.mConfigTickDistance) {
                this.mAccumulatedY = consumeAccumulatedMovement(j, i, this.mAccumulatedY, 19, 20);
                this.mAccumulatedX = 0.0f;
                this.mConsumedMovement = true;
            }
        }

        private float consumeAccumulatedMovement(long j, int i, float f, int i2, int i3) {
            while (f <= (-this.mConfigTickDistance)) {
                sendKeyDownOrRepeat(j, i2, i);
                f += this.mConfigTickDistance;
            }
            while (f >= this.mConfigTickDistance) {
                sendKeyDownOrRepeat(j, i3, i);
                f -= this.mConfigTickDistance;
            }
            return f;
        }

        private void sendKeyDownOrRepeat(long j, int i, int i2) {
            long j2;
            if (this.mPendingKeyCode != i) {
                sendKeyUp(j);
                j2 = j;
                this.mPendingKeyDownTime = j2;
                this.mPendingKeyCode = i;
                this.mPendingKeyRepeatCount = 0;
            } else {
                j2 = j;
                this.mPendingKeyRepeatCount++;
            }
            this.mPendingKeyMetaState = i2;
            ViewRootImpl.this.enqueueInputEvent(new KeyEvent(this.mPendingKeyDownTime, j2, 0, this.mPendingKeyCode, this.mPendingKeyRepeatCount, this.mPendingKeyMetaState, this.mCurrentDeviceId, 1024, this.mCurrentSource));
        }

        private void sendKeyUp(long j) {
            if (this.mPendingKeyCode != 0) {
                ViewRootImpl.this.enqueueInputEvent(new KeyEvent(this.mPendingKeyDownTime, j, 1, this.mPendingKeyCode, 0, this.mPendingKeyMetaState, this.mCurrentDeviceId, 0, 1024, this.mCurrentSource));
                this.mPendingKeyCode = 0;
            }
        }

        private boolean startFling(long j, float f, float f2) {
            switch (this.mPendingKeyCode) {
                case 19:
                    float f3 = -f2;
                    if (f3 < this.mConfigMinFlingVelocity || Math.abs(f) >= this.mConfigMinFlingVelocity) {
                        return false;
                    }
                    this.mFlingVelocity = f3;
                    break;
                case 20:
                    if (f2 < this.mConfigMinFlingVelocity || Math.abs(f) >= this.mConfigMinFlingVelocity) {
                        return false;
                    }
                    this.mFlingVelocity = f2;
                    break;
                case 21:
                    float f4 = -f;
                    if (f4 < this.mConfigMinFlingVelocity || Math.abs(f2) >= this.mConfigMinFlingVelocity) {
                        return false;
                    }
                    this.mFlingVelocity = f4;
                    break;
                case 22:
                    if (f < this.mConfigMinFlingVelocity || Math.abs(f2) >= this.mConfigMinFlingVelocity) {
                        return false;
                    }
                    this.mFlingVelocity = f;
                    break;
            }
            this.mFlinging = postFling(j);
            return this.mFlinging;
        }

        private boolean postFling(long j) {
            if (this.mFlingVelocity >= this.mConfigMinFlingVelocity) {
                postAtTime(this.mFlingRunnable, j + ((long) ((this.mConfigTickDistance / this.mFlingVelocity) * 1000.0f)));
                return true;
            }
            return false;
        }

        private void cancelFling() {
            if (this.mFlinging) {
                removeCallbacks(this.mFlingRunnable);
                this.mFlinging = false;
            }
        }
    }

    final class SyntheticKeyboardHandler {
        SyntheticKeyboardHandler() {
        }

        public void process(KeyEvent keyEvent) {
            KeyCharacterMap.FallbackAction fallbackAction;
            if ((keyEvent.getFlags() & 1024) == 0 && (fallbackAction = keyEvent.getKeyCharacterMap().getFallbackAction(keyEvent.getKeyCode(), keyEvent.getMetaState())) != null) {
                KeyEvent keyEventObtain = KeyEvent.obtain(keyEvent.getDownTime(), keyEvent.getEventTime(), keyEvent.getAction(), fallbackAction.keyCode, keyEvent.getRepeatCount(), fallbackAction.metaState, keyEvent.getDeviceId(), keyEvent.getScanCode(), keyEvent.getFlags() | 1024, keyEvent.getSource(), null);
                fallbackAction.recycle();
                ViewRootImpl.this.enqueueInputEvent(keyEventObtain);
            }
        }
    }

    private static boolean isNavigationKey(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 61:
            case 62:
            case 66:
            case 92:
            case 93:
            case 122:
            case 123:
                return true;
            default:
                return false;
        }
    }

    private static boolean isTypingKey(KeyEvent keyEvent) {
        return keyEvent.getUnicodeChar() > 0;
    }

    private boolean checkForLeavingTouchModeAndConsume(KeyEvent keyEvent) {
        if (!this.mAttachInfo.mInTouchMode) {
            return false;
        }
        int action = keyEvent.getAction();
        if ((action != 0 && action != 2) || (keyEvent.getFlags() & 4) != 0) {
            return false;
        }
        if (isNavigationKey(keyEvent)) {
            return ensureTouchMode(false);
        }
        if (!isTypingKey(keyEvent)) {
            return false;
        }
        ensureTouchMode(false);
        return false;
    }

    void setLocalDragState(Object obj) {
        this.mLocalDragState = obj;
    }

    private void handleDragEvent(DragEvent dragEvent) {
        if (this.mView != null && this.mAdded) {
            int i = dragEvent.mAction;
            if (i == 1) {
                this.mCurrentDragView = null;
                this.mDragDescription = dragEvent.mClipDescription;
            } else {
                if (i == 4) {
                    this.mDragDescription = null;
                }
                dragEvent.mClipDescription = this.mDragDescription;
            }
            if (i == 6) {
                if (View.sCascadedDragDrop) {
                    this.mView.dispatchDragEnterExitInPreN(dragEvent);
                }
                setDragFocus(null, dragEvent);
            } else {
                if (i == 2 || i == 3) {
                    this.mDragPoint.set(dragEvent.mX, dragEvent.mY);
                    if (this.mTranslator != null) {
                        this.mTranslator.translatePointInScreenToAppWindow(this.mDragPoint);
                    }
                    if (this.mCurScrollY != 0) {
                        this.mDragPoint.offset(0.0f, this.mCurScrollY);
                    }
                    dragEvent.mX = this.mDragPoint.x;
                    dragEvent.mY = this.mDragPoint.y;
                }
                View view = this.mCurrentDragView;
                if (i == 3 && dragEvent.mClipData != null) {
                    dragEvent.mClipData.prepareToEnterProcess();
                }
                boolean zDispatchDragEvent = this.mView.dispatchDragEvent(dragEvent);
                if (i == 2 && !dragEvent.mEventHandlerWasCalled) {
                    setDragFocus(null, dragEvent);
                }
                if (view != this.mCurrentDragView) {
                    if (view != null) {
                        try {
                            this.mWindowSession.dragRecipientExited(this.mWindow);
                        } catch (RemoteException e) {
                            Slog.e(this.mTag, "Unable to note drag target change");
                        }
                    }
                    if (this.mCurrentDragView != null) {
                        this.mWindowSession.dragRecipientEntered(this.mWindow);
                    }
                }
                if (i == 3) {
                    try {
                        Log.i(this.mTag, "Reporting drop result: " + zDispatchDragEvent);
                        this.mWindowSession.reportDropResult(this.mWindow, zDispatchDragEvent);
                    } catch (RemoteException e2) {
                        Log.e(this.mTag, "Unable to report drop result");
                    }
                }
                if (i == 4) {
                    this.mCurrentDragView = null;
                    setLocalDragState(null);
                    this.mAttachInfo.mDragToken = null;
                    if (this.mAttachInfo.mDragSurface != null) {
                        this.mAttachInfo.mDragSurface.release();
                        this.mAttachInfo.mDragSurface = null;
                    }
                }
            }
        }
        dragEvent.recycle();
    }

    public void handleDispatchSystemUiVisibilityChanged(SystemUiVisibilityInfo systemUiVisibilityInfo) {
        if (this.mSeq != systemUiVisibilityInfo.seq) {
            this.mSeq = systemUiVisibilityInfo.seq;
            this.mAttachInfo.mForceReportNewAttributes = true;
            scheduleTraversals();
        }
        if (this.mView == null) {
            return;
        }
        if (systemUiVisibilityInfo.localChanges != 0) {
            this.mView.updateLocalSystemUiVisibility(systemUiVisibilityInfo.localValue, systemUiVisibilityInfo.localChanges);
        }
        int i = systemUiVisibilityInfo.globalVisibility & 7;
        if (i != this.mAttachInfo.mGlobalSystemUiVisibility) {
            this.mAttachInfo.mGlobalSystemUiVisibility = i;
            this.mView.dispatchSystemUiVisibilityChanged(i);
        }
    }

    public void onWindowTitleChanged() {
        this.mAttachInfo.mForceReportNewAttributes = true;
    }

    public void handleDispatchWindowShown() {
        this.mAttachInfo.mTreeObserver.dispatchOnWindowShown();
    }

    public void handleRequestKeyboardShortcuts(IResultReceiver iResultReceiver, int i) {
        Bundle bundle = new Bundle();
        ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
        if (this.mView != null) {
            this.mView.requestKeyboardShortcuts(arrayList, i);
        }
        bundle.putParcelableArrayList(WindowManager.PARCEL_KEY_SHORTCUTS_ARRAY, arrayList);
        try {
            iResultReceiver.send(0, bundle);
        } catch (RemoteException e) {
        }
    }

    public void getLastTouchPoint(Point point) {
        point.x = (int) this.mLastTouchPoint.x;
        point.y = (int) this.mLastTouchPoint.y;
    }

    public int getLastTouchSource() {
        return this.mLastTouchSource;
    }

    public void setDragFocus(View view, DragEvent dragEvent) {
        if (this.mCurrentDragView != view && !View.sCascadedDragDrop) {
            float f = dragEvent.mX;
            float f2 = dragEvent.mY;
            int i = dragEvent.mAction;
            ClipData clipData = dragEvent.mClipData;
            dragEvent.mX = 0.0f;
            dragEvent.mY = 0.0f;
            dragEvent.mClipData = null;
            if (this.mCurrentDragView != null) {
                dragEvent.mAction = 6;
                this.mCurrentDragView.callDragEventHandler(dragEvent);
            }
            if (view != null) {
                dragEvent.mAction = 5;
                view.callDragEventHandler(dragEvent);
            }
            dragEvent.mAction = i;
            dragEvent.mX = f;
            dragEvent.mY = f2;
            dragEvent.mClipData = clipData;
        }
        this.mCurrentDragView = view;
    }

    private AudioManager getAudioManager() {
        if (this.mView == null) {
            throw new IllegalStateException("getAudioManager called when there is no mView");
        }
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mView.getContext().getSystemService("audio");
        }
        return this.mAudioManager;
    }

    private AutofillManager getAutofillManager() {
        if (this.mView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) this.mView;
            if (viewGroup.getChildCount() > 0) {
                return (AutofillManager) viewGroup.getChildAt(0).getContext().getSystemService(AutofillManager.class);
            }
            return null;
        }
        return null;
    }

    private boolean isAutofillUiShowing() {
        AutofillManager autofillManager = getAutofillManager();
        if (autofillManager == null) {
            return false;
        }
        return autofillManager.isAutofillUiShowing();
    }

    public AccessibilityInteractionController getAccessibilityInteractionController() {
        if (this.mView == null) {
            throw new IllegalStateException("getAccessibilityInteractionController called when there is no mView");
        }
        if (this.mAccessibilityInteractionController == null) {
            this.mAccessibilityInteractionController = new AccessibilityInteractionController(this);
        }
        return this.mAccessibilityInteractionController;
    }

    private int relayoutWindow(WindowManager.LayoutParams layoutParams, int i, boolean z) throws RemoteException {
        boolean z2;
        int i2;
        ?? r8;
        boolean z3;
        float f = this.mAttachInfo.mApplicationScale;
        if (layoutParams == null || this.mTranslator == null) {
            z2 = false;
        } else {
            layoutParams.backup();
            this.mTranslator.translateWindowLayout(layoutParams);
            z2 = true;
        }
        if (layoutParams != null) {
            if (DBG) {
                Log.d(this.mTag, "WindowLayout in layoutWindow:" + layoutParams);
            }
            if (DEBUG_LAYOUT) {
                String str = this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append(">>>>>> CALLING relayoutW+ ");
                sb.append(this.mWindow);
                sb.append(", params = ");
                sb.append(layoutParams);
                sb.append(",viewVisibility = ");
                i2 = i;
                sb.append(i2);
                sb.append(", insetsPending = ");
                boolean z4 = z;
                sb.append(z4);
                sb.append(", appScale = ");
                sb.append(f);
                sb.append(", mWinFrame = ");
                sb.append(this.mWinFrame);
                sb.append(", mSeq = ");
                sb.append(this.mSeq);
                sb.append(", mPendingOverscanInsets = ");
                sb.append(this.mPendingOverscanInsets);
                sb.append(", mPendingContentInsets = ");
                sb.append(this.mPendingContentInsets);
                sb.append(", mPendingVisibleInsets = ");
                sb.append(this.mPendingVisibleInsets);
                sb.append(", mPendingStableInsets = ");
                sb.append(this.mPendingStableInsets);
                sb.append(", mPendingOutsets = ");
                sb.append(this.mPendingOutsets);
                sb.append(", mPendingMergedConfiguration = ");
                sb.append(this.mPendingMergedConfiguration);
                sb.append(", mSurface = ");
                sb.append(this.mSurface);
                sb.append(",valid = ");
                sb.append(this.mSurface.isValid());
                sb.append(", mOrigWindowType = ");
                sb.append(this.mOrigWindowType);
                sb.append(",this = ");
                sb.append(this);
                Log.d(str, sb.toString());
                z3 = z4;
            } else {
                i2 = i;
                z3 = z;
            }
            r8 = z3;
            if (this.mOrigWindowType != layoutParams.type) {
                r8 = z3;
                if (this.mTargetSdkVersion < 14) {
                    Slog.w(this.mTag, "Window type can not be changed after the window is added; ignoring change of " + this.mView);
                    layoutParams.type = this.mOrigWindowType;
                    r8 = z3;
                }
            }
        } else {
            i2 = i;
            r8 = z;
        }
        int iRelayout = this.mWindowSession.relayout(this.mWindow, this.mSeq, layoutParams, (int) ((this.mView.getMeasuredWidth() * f) + 0.5f), (int) ((this.mView.getMeasuredHeight() * f) + 0.5f), i2, r8, this.mSurface.isValid() ? this.mSurface.getNextFrameNumber() : -1L, this.mWinFrame, this.mPendingOverscanInsets, this.mPendingContentInsets, this.mPendingVisibleInsets, this.mPendingStableInsets, this.mPendingOutsets, this.mPendingBackDropFrame, this.mPendingDisplayCutout, this.mPendingMergedConfiguration, this.mSurface);
        this.mPendingAlwaysConsumeNavBar = (iRelayout & 64) != 0;
        if (DEBUG_LAYOUT) {
            Log.d(this.mTag, "<<<<<< BACK FROM relayoutW- : res = " + iRelayout + ", mWinFrame = " + this.mWinFrame + ", mPendingOverscanInsets = " + this.mPendingOverscanInsets + ", mPendingContentInsets = " + this.mPendingContentInsets + ", mPendingVisibleInsets = " + this.mPendingVisibleInsets + ", mPendingStableInsets = " + this.mPendingStableInsets + ", mPendingOutsets = " + this.mPendingOutsets + ", mPendingMergedConfiguration = " + this.mPendingMergedConfiguration + ", mSurface = " + this.mSurface + ",valid = " + this.mSurface.isValid() + ",params = " + layoutParams + ", this = " + this);
        }
        if (z2) {
            layoutParams.restore();
        }
        if (this.mTranslator != null) {
            this.mTranslator.translateRectInScreenToAppWinFrame(this.mWinFrame);
            this.mTranslator.translateRectInScreenToAppWindow(this.mPendingOverscanInsets);
            this.mTranslator.translateRectInScreenToAppWindow(this.mPendingContentInsets);
            this.mTranslator.translateRectInScreenToAppWindow(this.mPendingVisibleInsets);
            this.mTranslator.translateRectInScreenToAppWindow(this.mPendingStableInsets);
        }
        return iRelayout;
    }

    @Override
    public void playSoundEffect(int i) {
        checkThread();
        try {
            AudioManager audioManager = getAudioManager();
            switch (i) {
                case 0:
                    audioManager.playSoundEffect(0);
                    return;
                case 1:
                    audioManager.playSoundEffect(3);
                    return;
                case 2:
                    audioManager.playSoundEffect(1);
                    return;
                case 3:
                    audioManager.playSoundEffect(4);
                    return;
                case 4:
                    audioManager.playSoundEffect(2);
                    return;
                default:
                    throw new IllegalArgumentException("unknown effect id " + i + " not defined in " + SoundEffectConstants.class.getCanonicalName());
            }
        } catch (IllegalStateException e) {
            Log.e(this.mTag, "FATAL EXCEPTION when attempting to play sound effect: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public boolean performHapticFeedback(int i, boolean z) {
        try {
            return this.mWindowSession.performHapticFeedback(this.mWindow, i, z);
        } catch (RemoteException e) {
            Log.e(this.mTag, "performHapticFeedback RemoteException happens in " + this, e);
            return false;
        }
    }

    @Override
    public View focusSearch(View view, int i) {
        checkThread();
        if (!(this.mView instanceof ViewGroup)) {
            return null;
        }
        return FocusFinder.getInstance().findNextFocus((ViewGroup) this.mView, view, i);
    }

    @Override
    public View keyboardNavigationClusterSearch(View view, int i) {
        checkThread();
        return FocusFinder.getInstance().findNextKeyboardNavigationCluster(this.mView, view, i);
    }

    public void debug() {
        this.mView.debug();
    }

    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.println("ViewRoot:");
        printWriter.print(str2);
        printWriter.print("mAdded=");
        printWriter.print(this.mAdded);
        printWriter.print(" mRemoved=");
        printWriter.println(this.mRemoved);
        printWriter.print(str2);
        printWriter.print("mConsumeBatchedInputScheduled=");
        printWriter.println(this.mConsumeBatchedInputScheduled);
        printWriter.print(str2);
        printWriter.print("mConsumeBatchedInputImmediatelyScheduled=");
        printWriter.println(this.mConsumeBatchedInputImmediatelyScheduled);
        printWriter.print(str2);
        printWriter.print("mPendingInputEventCount=");
        printWriter.println(this.mPendingInputEventCount);
        printWriter.print(str2);
        printWriter.print("mProcessInputEventsScheduled=");
        printWriter.println(this.mProcessInputEventsScheduled);
        printWriter.print(str2);
        printWriter.print("mTraversalScheduled=");
        printWriter.print(this.mTraversalScheduled);
        printWriter.print(str2);
        printWriter.print("mIsAmbientMode=");
        printWriter.print(this.mIsAmbientMode);
        if (this.mTraversalScheduled) {
            printWriter.print(" (barrier=");
            printWriter.print(this.mTraversalBarrier);
            printWriter.println(")");
        } else {
            printWriter.println();
        }
        this.mFirstInputStage.dump(str2, printWriter);
        this.mChoreographer.dump(str, printWriter);
        printWriter.print(str);
        printWriter.println("View Hierarchy:");
        dumpViewHierarchy(str2, printWriter, this.mView);
    }

    private void dumpViewHierarchy(String str, PrintWriter printWriter, View view) {
        ViewGroup viewGroup;
        int childCount;
        printWriter.print(str);
        if (view == null) {
            printWriter.println("null");
            return;
        }
        printWriter.println(view.toString());
        if (!(view instanceof ViewGroup) || (childCount = (viewGroup = (ViewGroup) view).getChildCount()) <= 0) {
            return;
        }
        String str2 = str + "  ";
        for (int i = 0; i < childCount; i++) {
            dumpViewHierarchy(str2, printWriter, viewGroup.getChildAt(i));
        }
    }

    public void dumpGfxInfo(int[] iArr) {
        iArr[1] = 0;
        iArr[0] = 0;
        if (this.mView != null) {
            getGfxInfo(this.mView, iArr);
        }
    }

    private static void getGfxInfo(View view, int[] iArr) {
        RenderNode renderNode = view.mRenderNode;
        iArr[0] = iArr[0] + 1;
        if (renderNode != null) {
            iArr[1] = iArr[1] + renderNode.getDebugSize();
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                getGfxInfo(viewGroup.getChildAt(i), iArr);
            }
        }
    }

    boolean die(boolean z) {
        if (ViewDebugManager.DEBUG_LIFECYCLE) {
            Log.v(this.mTag, "die: immediate = " + z + ", mIsInTraversal = " + this.mIsInTraversal + ",mIsDrawing = " + this.mIsDrawing + ",this = " + this, new Throwable());
        }
        if (z && !this.mIsInTraversal) {
            doDie();
            return false;
        }
        if (!this.mIsDrawing) {
            destroyHardwareRenderer();
        } else {
            Log.e(this.mTag, "Attempting to destroy the window while drawing!\n  window=" + this + ", title=" + ((Object) this.mWindowAttributes.getTitle()));
        }
        this.mHandler.sendEmptyMessage(3);
        return true;
    }

    void doDie() {
        checkThread();
        if (LOCAL_LOGV) {
            Log.v(this.mTag, "DIE in " + this + " of " + this.mSurface);
        }
        if (ViewDebugManager.DEBUG_LIFECYCLE) {
            Log.v(this.mTag, "DIE in " + this + " of " + this.mSurface + ", mAdded = " + this.mAdded + ", mFirst = " + this.mFirst);
        }
        synchronized (this) {
            if (this.mRemoved) {
                return;
            }
            boolean z = true;
            this.mRemoved = true;
            if (this.mAdded) {
                dispatchDetachedFromWindow();
            }
            if (this.mAdded && !this.mFirst) {
                destroyHardwareRenderer();
                if (this.mView != null) {
                    int visibility = this.mView.getVisibility();
                    if (this.mViewVisibility == visibility) {
                        z = false;
                    }
                    if (this.mWindowAttributesChanged || z) {
                        try {
                            if ((relayoutWindow(this.mWindowAttributes, visibility, false) & 2) != 0) {
                                this.mWindowSession.finishDrawing(this.mWindow);
                            }
                        } catch (RemoteException e) {
                            Log.e(this.mTag, "RemoteException when finish draw window " + this.mWindow + " in " + this, e);
                        }
                    }
                    this.mSurface.release();
                }
            }
            this.mAdded = false;
            WindowManagerGlobal.getInstance().doRemoveView(this);
        }
    }

    public void requestUpdateConfiguration(Configuration configuration) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(18, configuration));
    }

    public void loadSystemProperties() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ViewRootImpl.this.mProfileRendering = SystemProperties.getBoolean(ViewRootImpl.PROPERTY_PROFILE_RENDERING, false);
                ViewRootImpl.this.profileRendering(ViewRootImpl.this.mAttachInfo.mHasWindowFocus);
                if (ViewRootImpl.this.mAttachInfo.mThreadedRenderer != null && ViewRootImpl.this.mAttachInfo.mThreadedRenderer.loadSystemProperties()) {
                    ViewRootImpl.this.invalidate();
                }
                boolean z = SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false);
                if (z != ViewRootImpl.this.mAttachInfo.mDebugLayout) {
                    ViewRootImpl.this.mAttachInfo.mDebugLayout = z;
                    if (!ViewRootImpl.this.mHandler.hasMessages(22)) {
                        ViewRootImpl.this.mHandler.sendEmptyMessageDelayed(22, 200L);
                    }
                }
            }
        });
    }

    private void destroyHardwareRenderer() {
        ThreadedRenderer threadedRenderer = this.mAttachInfo.mThreadedRenderer;
        if (threadedRenderer != null) {
            if (this.mView != null) {
                threadedRenderer.destroyHardwareResources(this.mView);
            }
            threadedRenderer.destroy();
            threadedRenderer.setRequested(false);
            this.mAttachInfo.mThreadedRenderer = null;
            this.mAttachInfo.mHardwareAccelerated = false;
        }
    }

    private void dispatchResized(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, boolean z, MergedConfiguration mergedConfiguration, Rect rect7, boolean z2, boolean z3, int i, DisplayCutout.ParcelableWrapper parcelableWrapper) {
        Rect rect8 = rect;
        if (DEBUG_LAYOUT) {
            Log.v(this.mTag, "Resizing " + this + ": frame=" + rect8.toShortString() + " contentInsets=" + rect3.toShortString() + " visibleInsets=" + rect4.toShortString() + " reportDraw=" + z + " backDropFrame=" + rect7);
        }
        if (this.mDragResizing && this.mUseMTRenderer) {
            boolean zEquals = rect8.equals(rect7);
            synchronized (this.mWindowCallbacks) {
                for (int size = this.mWindowCallbacks.size() - 1; size >= 0; size--) {
                    this.mWindowCallbacks.get(size).onWindowSizeIsChanging(rect7, zEquals, rect4, rect5);
                }
            }
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(z ? 5 : 4);
        if (this.mTranslator != null) {
            this.mTranslator.translateRectInScreenToAppWindow(rect8);
            this.mTranslator.translateRectInScreenToAppWindow(rect2);
            this.mTranslator.translateRectInScreenToAppWindow(rect3);
            this.mTranslator.translateRectInScreenToAppWindow(rect4);
        }
        SomeArgs someArgsObtain = SomeArgs.obtain();
        boolean z4 = Binder.getCallingPid() == Process.myPid();
        if (z4) {
            rect8 = new Rect(rect8);
        }
        someArgsObtain.arg1 = rect8;
        someArgsObtain.arg2 = z4 ? new Rect(rect3) : rect3;
        someArgsObtain.arg3 = z4 ? new Rect(rect4) : rect4;
        someArgsObtain.arg4 = (!z4 || mergedConfiguration == null) ? mergedConfiguration : new MergedConfiguration(mergedConfiguration);
        someArgsObtain.arg5 = z4 ? new Rect(rect2) : rect2;
        someArgsObtain.arg6 = z4 ? new Rect(rect5) : rect5;
        someArgsObtain.arg7 = z4 ? new Rect(rect6) : rect6;
        someArgsObtain.arg8 = z4 ? new Rect(rect7) : rect7;
        someArgsObtain.arg9 = parcelableWrapper.get();
        someArgsObtain.argi1 = z2 ? 1 : 0;
        someArgsObtain.argi2 = z3 ? 1 : 0;
        someArgsObtain.argi3 = i;
        messageObtainMessage.obj = someArgsObtain;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void dispatchMoved(int i, int i2) {
        if (DEBUG_LAYOUT) {
            Log.v(this.mTag, "Window moved " + this + ": newX=" + i + " newY=" + i2);
        }
        if (this.mTranslator != null) {
            PointF pointF = new PointF(i, i2);
            this.mTranslator.translatePointInScreenToAppWindow(pointF);
            i = (int) (((double) pointF.x) + 0.5d);
            i2 = (int) (((double) pointF.y) + 0.5d);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(23, i, i2));
    }

    private static final class QueuedInputEvent {
        public static final int FLAG_DEFERRED = 2;
        public static final int FLAG_DELIVER_POST_IME = 1;
        public static final int FLAG_FINISHED = 4;
        public static final int FLAG_FINISHED_HANDLED = 8;
        public static final int FLAG_RESYNTHESIZED = 16;
        public static final int FLAG_UNHANDLED = 32;
        public InputEvent mEvent;
        public int mFlags;
        public QueuedInputEvent mNext;
        public InputEventReceiver mReceiver;

        private QueuedInputEvent() {
        }

        public boolean shouldSkipIme() {
            if ((this.mFlags & 1) != 0) {
                return true;
            }
            return (this.mEvent instanceof MotionEvent) && (this.mEvent.isFromSource(2) || this.mEvent.isFromSource(4194304));
        }

        public boolean shouldSendToSynthesizer() {
            if ((this.mFlags & 32) != 0) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("QueuedInputEvent{flags=");
            if (!flagToString("UNHANDLED", 32, flagToString("RESYNTHESIZED", 16, flagToString("FINISHED_HANDLED", 8, flagToString("FINISHED", 4, flagToString("DEFERRED", 2, flagToString("DELIVER_POST_IME", 1, false, sb), sb), sb), sb), sb), sb)) {
                sb.append(WifiEnterpriseConfig.ENGINE_DISABLE);
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append(", hasNextQueuedEvent=");
            sb2.append(this.mEvent != null ? "true" : "false");
            sb.append(sb2.toString());
            StringBuilder sb3 = new StringBuilder();
            sb3.append(", hasInputEventReceiver=");
            sb3.append(this.mReceiver != null ? "true" : "false");
            sb.append(sb3.toString());
            sb.append(", mEvent=" + this.mEvent + "}");
            return sb.toString();
        }

        private boolean flagToString(String str, int i, boolean z, StringBuilder sb) {
            if ((i & this.mFlags) != 0) {
                if (z) {
                    sb.append("|");
                }
                sb.append(str);
                return true;
            }
            return z;
        }
    }

    private QueuedInputEvent obtainQueuedInputEvent(InputEvent inputEvent, InputEventReceiver inputEventReceiver, int i) {
        QueuedInputEvent queuedInputEvent = this.mQueuedInputEventPool;
        if (queuedInputEvent != null) {
            this.mQueuedInputEventPoolSize--;
            this.mQueuedInputEventPool = queuedInputEvent.mNext;
            queuedInputEvent.mNext = null;
        } else {
            queuedInputEvent = new QueuedInputEvent();
        }
        queuedInputEvent.mEvent = inputEvent;
        queuedInputEvent.mReceiver = inputEventReceiver;
        queuedInputEvent.mFlags = i;
        return queuedInputEvent;
    }

    private void recycleQueuedInputEvent(QueuedInputEvent queuedInputEvent) {
        queuedInputEvent.mEvent = null;
        queuedInputEvent.mReceiver = null;
        if (this.mQueuedInputEventPoolSize < 10) {
            this.mQueuedInputEventPoolSize++;
            queuedInputEvent.mNext = this.mQueuedInputEventPool;
            this.mQueuedInputEventPool = queuedInputEvent;
        }
    }

    void enqueueInputEvent(InputEvent inputEvent) {
        enqueueInputEvent(inputEvent, null, 0, false);
    }

    void enqueueInputEvent(InputEvent inputEvent, InputEventReceiver inputEventReceiver, int i, boolean z) {
        adjustInputEventForCompatibility(inputEvent);
        QueuedInputEvent queuedInputEventObtainQueuedInputEvent = obtainQueuedInputEvent(inputEvent, inputEventReceiver, i);
        QueuedInputEvent queuedInputEvent = this.mPendingInputEventTail;
        if (queuedInputEvent == null) {
            this.mPendingInputEventHead = queuedInputEventObtainQueuedInputEvent;
            this.mPendingInputEventTail = queuedInputEventObtainQueuedInputEvent;
        } else {
            queuedInputEvent.mNext = queuedInputEventObtainQueuedInputEvent;
            this.mPendingInputEventTail = queuedInputEventObtainQueuedInputEvent;
        }
        this.mPendingInputEventCount++;
        Trace.traceCounter(4L, this.mPendingInputEventQueueLengthCounterName, this.mPendingInputEventCount);
        if (ViewDebugManager.DEBUG_MET_TRACE && (inputEvent instanceof MotionEvent)) {
            MotionEvent motionEvent = (MotionEvent) inputEvent;
            Trace.traceBegin(4L, "MET_enqueueInputEvent_name: " + ((Object) this.mWindowAttributes.getTitle()));
            Trace.traceEnd(4L);
            Trace.traceBegin(4L, "MET_enqueueInputEvent: " + motionEvent.getEventTime() + "," + motionEvent.getAction() + "," + motionEvent.getX(0) + "," + motionEvent.getY(0));
            Trace.traceEnd(4L);
        }
        if (ViewDebugManager.DEBUG_INPUT || ViewDebugManager.DEBUG_KEY || ViewDebugManager.DEBUG_MOTION || ViewDebugManager.DEBUG_ENG) {
            Log.v(this.mTag, "enqueueInputEvent: event = " + inputEvent + ",processImmediately = " + z + ",mProcessInputEventsScheduled = " + this.mProcessInputEventsScheduled + ", this = " + this);
        }
        if (z) {
            doProcessInputEvents();
        } else {
            scheduleProcessInputEvents();
        }
    }

    private void scheduleProcessInputEvents() {
        if (!this.mProcessInputEventsScheduled) {
            this.mProcessInputEventsScheduled = true;
            Message messageObtainMessage = this.mHandler.obtainMessage(19);
            messageObtainMessage.setAsynchronous(true);
            this.mHandler.sendMessage(messageObtainMessage);
        }
    }

    void doProcessInputEvents() {
        long historicalEventTimeNano;
        while (this.mPendingInputEventHead != null) {
            QueuedInputEvent queuedInputEvent = this.mPendingInputEventHead;
            this.mPendingInputEventHead = queuedInputEvent.mNext;
            if (this.mPendingInputEventHead == null) {
                this.mPendingInputEventTail = null;
            }
            queuedInputEvent.mNext = null;
            this.mPendingInputEventCount--;
            Trace.traceCounter(4L, this.mPendingInputEventQueueLengthCounterName, this.mPendingInputEventCount);
            long eventTimeNano = queuedInputEvent.mEvent.getEventTimeNano();
            if (queuedInputEvent.mEvent instanceof MotionEvent) {
                MotionEvent motionEvent = (MotionEvent) queuedInputEvent.mEvent;
                if (motionEvent.getHistorySize() > 0) {
                    historicalEventTimeNano = motionEvent.getHistoricalEventTimeNano(0);
                } else {
                    historicalEventTimeNano = eventTimeNano;
                }
            }
            this.mChoreographer.mFrameInfo.updateInputEventTime(eventTimeNano, historicalEventTimeNano);
            if (ViewDebugManager.DEBUG_INPUT || ViewDebugManager.DEBUG_KEY || ViewDebugManager.DEBUG_MOTION) {
                Log.v(this.mTag, "doProcessInputEvents: mCurrentInputEvent = " + queuedInputEvent + ", this = " + this);
            }
            deliverInputEvent(queuedInputEvent);
        }
        if (this.mProcessInputEventsScheduled) {
            this.mProcessInputEventsScheduled = false;
            this.mHandler.removeMessages(19);
        }
    }

    private void deliverInputEvent(QueuedInputEvent queuedInputEvent) {
        InputStage inputStage;
        Trace.asyncTraceBegin(8L, "deliverInputEvent", queuedInputEvent.mEvent.getSequenceNumber());
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onInputEvent(queuedInputEvent.mEvent, 0);
        }
        if (queuedInputEvent.shouldSendToSynthesizer()) {
            inputStage = this.mSyntheticInputStage;
        } else {
            inputStage = queuedInputEvent.shouldSkipIme() ? this.mFirstPostImeInputStage : this.mFirstInputStage;
        }
        if (queuedInputEvent.mEvent instanceof KeyEvent) {
            this.mUnhandledKeyManager.preDispatch((KeyEvent) queuedInputEvent.mEvent);
        }
        if (inputStage != null) {
            handleWindowFocusChanged();
            inputStage.deliver(queuedInputEvent);
        } else {
            finishInputEvent(queuedInputEvent);
        }
    }

    private void finishInputEvent(QueuedInputEvent queuedInputEvent) {
        Trace.asyncTraceEnd(8L, "deliverInputEvent", queuedInputEvent.mEvent.getSequenceNumber());
        boolean z = (queuedInputEvent.mFlags & 8) != 0;
        ViewDebugManager.getInstance().debugInputEventFinished(this.mTag, z, queuedInputEvent.mEvent, this);
        if (queuedInputEvent.mReceiver != null) {
            queuedInputEvent.mReceiver.finishInputEvent(queuedInputEvent.mEvent, z);
        } else {
            queuedInputEvent.mEvent.recycleIfNeededAfterDispatch();
        }
        recycleQueuedInputEvent(queuedInputEvent);
    }

    private void adjustInputEventForCompatibility(InputEvent inputEvent) {
        if (this.mTargetSdkVersion < 23 && (inputEvent instanceof MotionEvent)) {
            MotionEvent motionEvent = (MotionEvent) inputEvent;
            int buttonState = motionEvent.getButtonState();
            int i = (buttonState & 96) >> 4;
            if (i != 0) {
                motionEvent.setButtonState(buttonState | i);
            }
        }
    }

    static boolean isTerminalInputEvent(InputEvent inputEvent) {
        if (inputEvent instanceof KeyEvent) {
            return ((KeyEvent) inputEvent).getAction() == 1;
        }
        int action = ((MotionEvent) inputEvent).getAction();
        return action == 1 || action == 3 || action == 10;
    }

    void scheduleConsumeBatchedInput() {
        if (!this.mConsumeBatchedInputScheduled) {
            this.mConsumeBatchedInputScheduled = true;
            this.mChoreographer.postCallback(0, this.mConsumedBatchedInputRunnable, null);
        }
    }

    void unscheduleConsumeBatchedInput() {
        if (this.mConsumeBatchedInputScheduled) {
            this.mConsumeBatchedInputScheduled = false;
            this.mChoreographer.removeCallbacks(0, this.mConsumedBatchedInputRunnable, null);
        }
    }

    void scheduleConsumeBatchedInputImmediately() {
        if (!this.mConsumeBatchedInputImmediatelyScheduled) {
            unscheduleConsumeBatchedInput();
            this.mConsumeBatchedInputImmediatelyScheduled = true;
            this.mHandler.post(this.mConsumeBatchedInputImmediatelyRunnable);
        }
    }

    void doConsumeBatchedInput(long j) {
        if (this.mConsumeBatchedInputScheduled) {
            this.mConsumeBatchedInputScheduled = false;
            if (this.mInputEventReceiver != null) {
                if (ViewDebugManager.DEBUG_MET_TRACE) {
                    Trace.traceBegin(4L, "MET_consumeBatchedInput_name: " + ((Object) this.mWindowAttributes.getTitle()));
                    Trace.traceEnd(4L);
                }
                if (this.mInputEventReceiver.consumeBatchedInputEvents(j) && j != -1) {
                    scheduleConsumeBatchedInput();
                }
            }
            doProcessInputEvents();
        }
    }

    final class TraversalRunnable implements Runnable {
        TraversalRunnable() {
        }

        @Override
        public void run() {
            ViewRootImpl.this.doTraversal();
        }
    }

    final class WindowInputEventReceiver extends InputEventReceiver {
        public WindowInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        @Override
        public void onInputEvent(InputEvent inputEvent, int i) {
            ViewDebugManager.getInstance().debugInputEventStart(inputEvent);
            ViewRootImpl.this.enqueueInputEvent(inputEvent, this, 0, true);
        }

        @Override
        public void onBatchedInputEventPending() {
            if (ViewRootImpl.this.mUnbufferedInputDispatch) {
                super.onBatchedInputEventPending();
            } else {
                ViewRootImpl.this.scheduleConsumeBatchedInput();
            }
        }

        @Override
        public void dispose() {
            ViewRootImpl.this.unscheduleConsumeBatchedInput();
            super.dispose();
        }
    }

    final class ConsumeBatchedInputRunnable implements Runnable {
        ConsumeBatchedInputRunnable() {
        }

        @Override
        public void run() {
            ViewRootImpl.this.doConsumeBatchedInput(ViewRootImpl.this.mChoreographer.getFrameTimeNanos());
        }
    }

    final class ConsumeBatchedInputImmediatelyRunnable implements Runnable {
        ConsumeBatchedInputImmediatelyRunnable() {
        }

        @Override
        public void run() {
            ViewRootImpl.this.doConsumeBatchedInput(-1L);
        }
    }

    final class InvalidateOnAnimationRunnable implements Runnable {
        private boolean mPosted;
        private View.AttachInfo.InvalidateInfo[] mTempViewRects;
        private View[] mTempViews;
        private final ArrayList<View> mViews = new ArrayList<>();
        private final ArrayList<View.AttachInfo.InvalidateInfo> mViewRects = new ArrayList<>();

        InvalidateOnAnimationRunnable() {
        }

        public void addView(View view) {
            synchronized (this) {
                this.mViews.add(view);
                postIfNeededLocked();
            }
        }

        public void addViewRect(View.AttachInfo.InvalidateInfo invalidateInfo) {
            synchronized (this) {
                this.mViewRects.add(invalidateInfo);
                postIfNeededLocked();
            }
        }

        public void removeView(View view) {
            synchronized (this) {
                this.mViews.remove(view);
                int size = this.mViewRects.size();
                while (true) {
                    int i = size - 1;
                    if (size <= 0) {
                        break;
                    }
                    View.AttachInfo.InvalidateInfo invalidateInfo = this.mViewRects.get(i);
                    if (invalidateInfo.target == view) {
                        this.mViewRects.remove(i);
                        invalidateInfo.recycle();
                    }
                    size = i;
                }
                if (this.mPosted && this.mViews.isEmpty() && this.mViewRects.isEmpty()) {
                    ViewRootImpl.this.mChoreographer.removeCallbacks(1, this, null);
                    this.mPosted = false;
                }
            }
        }

        @Override
        public void run() {
            int i;
            int size;
            int size2;
            synchronized (this) {
                this.mPosted = false;
                size = this.mViews.size();
                if (size != 0) {
                    this.mTempViews = (View[]) this.mViews.toArray(this.mTempViews != null ? this.mTempViews : new View[size]);
                    this.mViews.clear();
                }
                size2 = this.mViewRects.size();
                if (size2 != 0) {
                    this.mTempViewRects = (View.AttachInfo.InvalidateInfo[]) this.mViewRects.toArray(this.mTempViewRects != null ? this.mTempViewRects : new View.AttachInfo.InvalidateInfo[size2]);
                    this.mViewRects.clear();
                }
            }
            for (int i2 = 0; i2 < size; i2++) {
                this.mTempViews[i2].invalidate();
                this.mTempViews[i2] = null;
            }
            for (i = 0; i < size2; i++) {
                View.AttachInfo.InvalidateInfo invalidateInfo = this.mTempViewRects[i];
                invalidateInfo.target.invalidate(invalidateInfo.left, invalidateInfo.top, invalidateInfo.right, invalidateInfo.bottom);
                invalidateInfo.recycle();
            }
        }

        private void postIfNeededLocked() {
            if (!this.mPosted) {
                ViewRootImpl.this.mChoreographer.postCallback(1, this, null);
                this.mPosted = true;
            }
        }
    }

    public void dispatchInvalidateDelayed(View view, long j) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, view), j);
    }

    public void dispatchInvalidateRectDelayed(View.AttachInfo.InvalidateInfo invalidateInfo, long j) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2, invalidateInfo), j);
    }

    public void dispatchInvalidateOnAnimation(View view) {
        this.mInvalidateOnAnimationRunnable.addView(view);
    }

    public void dispatchInvalidateRectOnAnimation(View.AttachInfo.InvalidateInfo invalidateInfo) {
        this.mInvalidateOnAnimationRunnable.addViewRect(invalidateInfo);
    }

    public void cancelInvalidate(View view) {
        this.mHandler.removeMessages(1, view);
        this.mHandler.removeMessages(2, view);
        this.mInvalidateOnAnimationRunnable.removeView(view);
    }

    public void dispatchInputEvent(InputEvent inputEvent) {
        dispatchInputEvent(inputEvent, null);
    }

    public void dispatchInputEvent(InputEvent inputEvent, InputEventReceiver inputEventReceiver) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = inputEvent;
        someArgsObtain.arg2 = inputEventReceiver;
        Message messageObtainMessage = this.mHandler.obtainMessage(7, someArgsObtain);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void synthesizeInputEvent(InputEvent inputEvent) {
        Message messageObtainMessage = this.mHandler.obtainMessage(24, inputEvent);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void dispatchKeyFromIme(KeyEvent keyEvent) {
        Message messageObtainMessage = this.mHandler.obtainMessage(11, keyEvent);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void dispatchKeyFromAutofill(KeyEvent keyEvent) {
        Message messageObtainMessage = this.mHandler.obtainMessage(12, keyEvent);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void dispatchUnhandledInputEvent(InputEvent inputEvent) {
        if (inputEvent instanceof MotionEvent) {
            inputEvent = MotionEvent.obtain((MotionEvent) inputEvent);
        }
        synthesizeInputEvent(inputEvent);
    }

    public void dispatchAppVisibility(boolean z) {
        Message messageObtainMessage = this.mHandler.obtainMessage(8);
        messageObtainMessage.arg1 = z ? 1 : 0;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void dispatchGetNewSurface() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9));
    }

    public void windowFocusChanged(boolean z, boolean z2) {
        synchronized (this) {
            this.mWindowFocusChanged = true;
            this.mUpcomingWindowFocus = z;
            this.mUpcomingInTouchMode = z2;
        }
        Message messageObtain = Message.obtain();
        messageObtain.what = 6;
        this.mHandler.sendMessage(messageObtain);
    }

    public void dispatchWindowShown() {
        this.mHandler.sendEmptyMessage(25);
    }

    public void dispatchCloseSystemDialogs(String str) {
        Message messageObtain = Message.obtain();
        messageObtain.what = 14;
        messageObtain.obj = str;
        this.mHandler.sendMessage(messageObtain);
    }

    public void dispatchDragEvent(DragEvent dragEvent) {
        int i;
        if (dragEvent.getAction() == 2) {
            i = 16;
            this.mHandler.removeMessages(16);
        } else {
            i = 15;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(i, dragEvent));
    }

    public void updatePointerIcon(float f, float f2) {
        this.mHandler.removeMessages(27);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(27, MotionEvent.obtain(0L, SystemClock.uptimeMillis(), 7, f, f2, 0)));
    }

    public void dispatchSystemUiVisibilityChanged(int i, int i2, int i3, int i4) {
        SystemUiVisibilityInfo systemUiVisibilityInfo = new SystemUiVisibilityInfo();
        systemUiVisibilityInfo.seq = i;
        systemUiVisibilityInfo.globalVisibility = i2;
        systemUiVisibilityInfo.localValue = i3;
        systemUiVisibilityInfo.localChanges = i4;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(17, systemUiVisibilityInfo));
    }

    public void dispatchCheckFocus() {
        if (!this.mHandler.hasMessages(13)) {
            this.mHandler.sendEmptyMessage(13);
        }
    }

    public void dispatchRequestKeyboardShortcuts(IResultReceiver iResultReceiver, int i) {
        this.mHandler.obtainMessage(26, i, 0, iResultReceiver).sendToTarget();
    }

    public void dispatchPointerCaptureChanged(boolean z) {
        this.mHandler.removeMessages(28);
        Message messageObtainMessage = this.mHandler.obtainMessage(28);
        messageObtainMessage.arg1 = z ? 1 : 0;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void postSendWindowContentChangedCallback(View view, int i) {
        if (this.mSendWindowContentChangedAccessibilityEvent == null) {
            this.mSendWindowContentChangedAccessibilityEvent = new SendWindowContentChangedAccessibilityEvent();
        }
        this.mSendWindowContentChangedAccessibilityEvent.runOrPost(view, i);
    }

    private void removeSendWindowContentChangedCallback() {
        if (this.mSendWindowContentChangedAccessibilityEvent != null) {
            this.mHandler.removeCallbacks(this.mSendWindowContentChangedAccessibilityEvent);
        }
    }

    @Override
    public boolean showContextMenuForChild(View view) {
        return false;
    }

    @Override
    public boolean showContextMenuForChild(View view, float f, float f2) {
        return false;
    }

    @Override
    public ActionMode startActionModeForChild(View view, ActionMode.Callback callback) {
        return null;
    }

    @Override
    public ActionMode startActionModeForChild(View view, ActionMode.Callback callback, int i) {
        return null;
    }

    @Override
    public void createContextMenu(ContextMenu contextMenu) {
    }

    @Override
    public void childDrawableStateChanged(View view) {
    }

    @Override
    public boolean requestSendAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeProvider accessibilityNodeProvider;
        if (this.mView == null || this.mStopped || this.mPausedForTransition) {
            return false;
        }
        if (accessibilityEvent.getEventType() != 2048 && this.mSendWindowContentChangedAccessibilityEvent != null && this.mSendWindowContentChangedAccessibilityEvent.mSource != null) {
            this.mSendWindowContentChangedAccessibilityEvent.removeCallbacksAndRun();
        }
        int eventType = accessibilityEvent.getEventType();
        if (eventType == 2048) {
            handleWindowContentChangedEvent(accessibilityEvent);
        } else if (eventType == 32768) {
            long sourceNodeId = accessibilityEvent.getSourceNodeId();
            View viewFindViewByAccessibilityId = this.mView.findViewByAccessibilityId(AccessibilityNodeInfo.getAccessibilityViewId(sourceNodeId));
            if (viewFindViewByAccessibilityId != null && (accessibilityNodeProvider = viewFindViewByAccessibilityId.getAccessibilityNodeProvider()) != null) {
                setAccessibilityFocus(viewFindViewByAccessibilityId, accessibilityNodeProvider.createAccessibilityNodeInfo(AccessibilityNodeInfo.getVirtualDescendantId(sourceNodeId)));
            }
        } else if (eventType == 65536) {
            View viewFindViewByAccessibilityId2 = this.mView.findViewByAccessibilityId(AccessibilityNodeInfo.getAccessibilityViewId(accessibilityEvent.getSourceNodeId()));
            if (viewFindViewByAccessibilityId2 != null && viewFindViewByAccessibilityId2.getAccessibilityNodeProvider() != null) {
                setAccessibilityFocus(null, null);
            }
        }
        this.mAccessibilityManager.sendAccessibilityEvent(accessibilityEvent);
        return true;
    }

    private void handleWindowContentChangedEvent(AccessibilityEvent accessibilityEvent) {
        View view = this.mAccessibilityFocusedHost;
        if (view == null || this.mAccessibilityFocusedVirtualView == null) {
            return;
        }
        AccessibilityNodeProvider accessibilityNodeProvider = view.getAccessibilityNodeProvider();
        if (accessibilityNodeProvider == null) {
            this.mAccessibilityFocusedHost = null;
            this.mAccessibilityFocusedVirtualView = null;
            view.clearAccessibilityFocusNoCallbacks(0);
            return;
        }
        int contentChangeTypes = accessibilityEvent.getContentChangeTypes();
        if ((contentChangeTypes & 1) == 0 && contentChangeTypes != 0) {
            return;
        }
        int accessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityEvent.getSourceNodeId());
        View view2 = this.mAccessibilityFocusedHost;
        boolean z = false;
        while (view2 != null && !z) {
            if (accessibilityViewId == view2.getAccessibilityViewId()) {
                z = true;
            } else {
                Object parent = view2.getParent();
                if (parent instanceof View) {
                    view2 = (View) parent;
                } else {
                    view2 = null;
                }
            }
        }
        if (!z) {
            return;
        }
        int virtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(this.mAccessibilityFocusedVirtualView.getSourceNodeId());
        Rect rect = this.mTempRect;
        this.mAccessibilityFocusedVirtualView.getBoundsInScreen(rect);
        this.mAccessibilityFocusedVirtualView = accessibilityNodeProvider.createAccessibilityNodeInfo(virtualDescendantId);
        if (this.mAccessibilityFocusedVirtualView == null) {
            this.mAccessibilityFocusedHost = null;
            view.clearAccessibilityFocusNoCallbacks(0);
            accessibilityNodeProvider.performAction(virtualDescendantId, AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS.getId(), null);
            invalidateRectOnScreen(rect);
            return;
        }
        Rect boundsInScreen = this.mAccessibilityFocusedVirtualView.getBoundsInScreen();
        if (!rect.equals(boundsInScreen)) {
            rect.union(boundsInScreen);
            invalidateRectOnScreen(rect);
        }
    }

    @Override
    public void notifySubtreeAccessibilityStateChanged(View view, View view2, int i) {
        postSendWindowContentChangedCallback((View) Preconditions.checkNotNull(view2), i);
    }

    @Override
    public boolean canResolveLayoutDirection() {
        return true;
    }

    @Override
    public boolean isLayoutDirectionResolved() {
        return true;
    }

    @Override
    public int getLayoutDirection() {
        return 0;
    }

    @Override
    public boolean canResolveTextDirection() {
        return true;
    }

    @Override
    public boolean isTextDirectionResolved() {
        return true;
    }

    @Override
    public int getTextDirection() {
        return 1;
    }

    @Override
    public boolean canResolveTextAlignment() {
        return true;
    }

    @Override
    public boolean isTextAlignmentResolved() {
        return true;
    }

    @Override
    public int getTextAlignment() {
        return 1;
    }

    private View getCommonPredecessor(View view, View view2) {
        if (this.mTempHashSet == null) {
            this.mTempHashSet = new HashSet<>();
        }
        HashSet<View> hashSet = this.mTempHashSet;
        hashSet.clear();
        while (view != null) {
            hashSet.add(view);
            Object obj = view.mParent;
            if (obj instanceof View) {
                view = (View) obj;
            } else {
                view = null;
            }
        }
        while (view2 != null) {
            if (hashSet.contains(view2)) {
                hashSet.clear();
                return view2;
            }
            Object obj2 = view2.mParent;
            if (obj2 instanceof View) {
                view2 = (View) obj2;
            } else {
                view2 = null;
            }
        }
        hashSet.clear();
        return null;
    }

    void checkThread() {
        if (this.mThread != Thread.currentThread()) {
            throw new CalledFromWrongThreadException("Only the original thread that created a view hierarchy can touch its views.");
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean z) {
    }

    @Override
    public boolean requestChildRectangleOnScreen(View view, Rect rect, boolean z) {
        if (rect == null) {
            return scrollToRectOrFocus(null, z);
        }
        rect.offset(view.getLeft() - view.getScrollX(), view.getTop() - view.getScrollY());
        boolean zScrollToRectOrFocus = scrollToRectOrFocus(rect, z);
        this.mTempRect.set(rect);
        this.mTempRect.offset(0, -this.mCurScrollY);
        this.mTempRect.offset(this.mAttachInfo.mWindowLeft, this.mAttachInfo.mWindowTop);
        try {
            this.mWindowSession.onRectangleOnScreenRequested(this.mWindow, this.mTempRect);
        } catch (RemoteException e) {
        }
        return zScrollToRectOrFocus;
    }

    @Override
    public void childHasTransientStateChanged(View view, boolean z) {
    }

    @Override
    public boolean onStartNestedScroll(View view, View view2, int i) {
        return false;
    }

    @Override
    public void onStopNestedScroll(View view) {
    }

    @Override
    public void onNestedScrollAccepted(View view, View view2, int i) {
    }

    @Override
    public void onNestedScroll(View view, int i, int i2, int i3, int i4) {
    }

    @Override
    public void onNestedPreScroll(View view, int i, int i2, int[] iArr) {
    }

    @Override
    public boolean onNestedFling(View view, float f, float f2, boolean z) {
        return false;
    }

    @Override
    public boolean onNestedPreFling(View view, float f, float f2) {
        return false;
    }

    @Override
    public boolean onNestedPrePerformAccessibilityAction(View view, int i, Bundle bundle) {
        return false;
    }

    private void reportNextDraw() {
        if (!this.mReportNextDraw) {
            drawPending();
        }
        this.mReportNextDraw = true;
    }

    public void setReportNextDraw() {
        reportNextDraw();
        invalidate();
    }

    void changeCanvasOpacity(boolean z) {
        Log.d(this.mTag, "changeCanvasOpacity: opaque=" + z);
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.setOpaque(z);
        }
    }

    public boolean dispatchUnhandledKeyEvent(KeyEvent keyEvent) {
        return this.mUnhandledKeyManager.dispatch(this.mView, keyEvent);
    }

    class TakenSurfaceHolder extends BaseSurfaceHolder {
        TakenSurfaceHolder() {
        }

        @Override
        public boolean onAllowLockCanvas() {
            return ViewRootImpl.this.mDrawingAllowed;
        }

        @Override
        public void onRelayoutContainer() {
        }

        @Override
        public void setFormat(int i) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceFormat(i);
        }

        @Override
        public void setType(int i) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceType(i);
        }

        @Override
        public void onUpdateSurface() {
            throw new IllegalStateException("Shouldn't be here");
        }

        @Override
        public boolean isCreating() {
            return ViewRootImpl.this.mIsCreating;
        }

        @Override
        public void setFixedSize(int i, int i2) {
            throw new UnsupportedOperationException("Currently only support sizing from layout");
        }

        @Override
        public void setKeepScreenOn(boolean z) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceKeepScreenOn(z);
        }
    }

    static class W extends IWindow.Stub {
        private final WeakReference<ViewRootImpl> mViewAncestor;
        private final IWindowSession mWindowSession;

        W(ViewRootImpl viewRootImpl) {
            this.mViewAncestor = new WeakReference<>(viewRootImpl);
            this.mWindowSession = viewRootImpl.mWindowSession;
        }

        @Override
        public void resized(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, boolean z, MergedConfiguration mergedConfiguration, Rect rect7, boolean z2, boolean z3, int i, DisplayCutout.ParcelableWrapper parcelableWrapper) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (viewRootImpl != null) {
                viewRootImpl.dispatchResized(rect, rect2, rect3, rect4, rect5, rect6, z, mergedConfiguration, rect7, z2, z3, i, parcelableWrapper);
            }
        }

        @Override
        public void moved(int i, int i2) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (viewRootImpl != null) {
                viewRootImpl.dispatchMoved(i, i2);
            }
        }

        @Override
        public void dispatchAppVisibility(boolean z) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (ViewDebugManager.DEBUG_LIFECYCLE) {
                Log.v(ViewRootImpl.TAG, "dispatchAppVisibility: visible = " + z + ", viewAncestor = " + viewRootImpl);
            }
            if (viewRootImpl != null) {
                viewRootImpl.dispatchAppVisibility(z);
            }
        }

        @Override
        public void dispatchGetNewSurface() {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (viewRootImpl != null) {
                viewRootImpl.dispatchGetNewSurface();
            }
        }

        @Override
        public void windowFocusChanged(boolean z, boolean z2) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (ViewRootImpl.DEBUG_IMF) {
                Log.v(ViewRootImpl.TAG, "W windowFocusChanged: hasFocus = " + z + ", inTouchMode = " + z2 + ", viewAncestor = " + viewRootImpl + ", this = " + this);
            }
            if (viewRootImpl != null) {
                viewRootImpl.windowFocusChanged(z, z2);
            }
        }

        private static int checkCallingPermission(String str) {
            try {
                return ActivityManager.getService().checkPermission(str, Binder.getCallingPid(), Binder.getCallingUid());
            } catch (RemoteException e) {
                return -1;
            }
        }

        @Override
        public void executeCommand(String str, String str2, ParcelFileDescriptor parcelFileDescriptor) throws Throwable {
            View view;
            ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream;
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (viewRootImpl == null || (view = viewRootImpl.mView) == null) {
                return;
            }
            if (checkCallingPermission(Manifest.permission.DUMP) != 0) {
                throw new SecurityException("Insufficient permissions to invoke executeCommand() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            }
            ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream2 = null;
            try {
                try {
                    try {
                        autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(parcelFileDescriptor);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e2) {
                    e = e2;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                ViewDebug.dispatchCommand(view, str, str2, autoCloseOutputStream);
                autoCloseOutputStream.close();
            } catch (IOException e3) {
                e = e3;
                autoCloseOutputStream2 = autoCloseOutputStream;
                e.printStackTrace();
                if (autoCloseOutputStream2 != null) {
                    autoCloseOutputStream2.close();
                }
            } catch (Throwable th2) {
                th = th2;
                autoCloseOutputStream2 = autoCloseOutputStream;
                if (autoCloseOutputStream2 != null) {
                    try {
                        autoCloseOutputStream2.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    }
                }
                throw th;
            }
        }

        @Override
        public void closeSystemDialogs(String str) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (ViewRootImpl.LOCAL_LOGV) {
                Log.v(ViewRootImpl.TAG, "Close system dialogs in " + viewRootImpl + " for " + str);
            }
            if (viewRootImpl != null) {
                viewRootImpl.dispatchCloseSystemDialogs(str);
            }
        }

        @Override
        public void dispatchWallpaperOffsets(float f, float f2, float f3, float f4, boolean z) {
            if (z) {
                try {
                    this.mWindowSession.wallpaperOffsetsComplete(asBinder());
                } catch (RemoteException e) {
                    Log.e(ViewRootImpl.TAG, "RemoteException happens when dispatchWallpaperOffsets.", e);
                }
            }
        }

        @Override
        public void dispatchWallpaperCommand(String str, int i, int i2, int i3, Bundle bundle, boolean z) {
            if (z) {
                try {
                    this.mWindowSession.wallpaperCommandComplete(asBinder(), null);
                } catch (RemoteException e) {
                    Log.e(ViewRootImpl.TAG, "RemoteException happens when dispatchWallpaperCommand.", e);
                }
            }
        }

        @Override
        public void dispatchDragEvent(DragEvent dragEvent) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (ViewRootImpl.LOCAL_LOGV || ViewDebugManager.DEBUG_INPUT) {
                Log.v(ViewRootImpl.TAG, "Dispatch drag event " + dragEvent + " in " + viewRootImpl);
            }
            if (viewRootImpl != null) {
                viewRootImpl.dispatchDragEvent(dragEvent);
            }
        }

        @Override
        public void updatePointerIcon(float f, float f2) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (viewRootImpl != null) {
                viewRootImpl.updatePointerIcon(f, f2);
            }
        }

        @Override
        public void dispatchSystemUiVisibilityChanged(int i, int i2, int i3, int i4) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (ViewRootImpl.LOCAL_LOGV) {
                Log.v(ViewRootImpl.TAG, "dispatchSystemUiVisibilityChanged: seq = " + i + ", globalVisibility = " + i2 + ", localValue = " + i3 + ", localChanges = " + i4 + ", viewAncestor" + viewRootImpl);
            }
            if (viewRootImpl != null) {
                viewRootImpl.dispatchSystemUiVisibilityChanged(i, i2, i3, i4);
            }
        }

        @Override
        public void dispatchWindowShown() {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (ViewRootImpl.DEBUG_DRAW) {
                Log.v(ViewRootImpl.TAG, "doneAnimating: viewAncestor" + viewRootImpl);
            }
            if (viewRootImpl != null) {
                viewRootImpl.dispatchWindowShown();
            }
        }

        @Override
        public void requestAppKeyboardShortcuts(IResultReceiver iResultReceiver, int i) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (viewRootImpl != null) {
                viewRootImpl.dispatchRequestKeyboardShortcuts(iResultReceiver, i);
            }
        }

        @Override
        public void dispatchPointerCaptureChanged(boolean z) {
            ViewRootImpl viewRootImpl = this.mViewAncestor.get();
            if (viewRootImpl != null) {
                viewRootImpl.dispatchPointerCaptureChanged(z);
            }
        }
    }

    public static final class CalledFromWrongThreadException extends AndroidRuntimeException {
        public CalledFromWrongThreadException(String str) {
            super(str);
        }
    }

    static HandlerActionQueue getRunQueue() {
        HandlerActionQueue handlerActionQueue = sRunQueues.get();
        if (handlerActionQueue != null) {
            return handlerActionQueue;
        }
        HandlerActionQueue handlerActionQueue2 = new HandlerActionQueue();
        sRunQueues.set(handlerActionQueue2);
        return handlerActionQueue2;
    }

    private void startDragResizing(Rect rect, boolean z, Rect rect2, Rect rect3, int i) {
        if (!this.mDragResizing) {
            this.mDragResizing = true;
            if (this.mUseMTRenderer) {
                for (int size = this.mWindowCallbacks.size() - 1; size >= 0; size--) {
                    this.mWindowCallbacks.get(size).onWindowDragResizeStart(rect, z, rect2, rect3, i);
                }
            }
            this.mFullRedrawNeeded = true;
        }
    }

    private void endDragResizing() {
        if (this.mDragResizing) {
            this.mDragResizing = false;
            if (this.mUseMTRenderer) {
                for (int size = this.mWindowCallbacks.size() - 1; size >= 0; size--) {
                    this.mWindowCallbacks.get(size).onWindowDragResizeEnd();
                }
            }
            this.mFullRedrawNeeded = true;
        }
    }

    private boolean updateContentDrawBounds() {
        boolean zOnContentDrawn;
        if (this.mUseMTRenderer) {
            zOnContentDrawn = false;
            for (int size = this.mWindowCallbacks.size() - 1; size >= 0; size--) {
                zOnContentDrawn |= this.mWindowCallbacks.get(size).onContentDrawn(this.mWindowAttributes.surfaceInsets.left, this.mWindowAttributes.surfaceInsets.top, this.mWidth, this.mHeight);
            }
        } else {
            zOnContentDrawn = false;
        }
        return zOnContentDrawn | (this.mDragResizing && this.mReportNextDraw);
    }

    private void requestDrawWindow() {
        if (!this.mUseMTRenderer) {
            return;
        }
        this.mWindowDrawCountDown = new CountDownLatch(this.mWindowCallbacks.size());
        for (int size = this.mWindowCallbacks.size() - 1; size >= 0; size--) {
            this.mWindowCallbacks.get(size).onRequestDraw(this.mReportNextDraw);
        }
    }

    public void reportActivityRelaunched() {
        this.mActivityRelaunched = true;
    }

    final class AccessibilityInteractionConnectionManager implements AccessibilityManager.AccessibilityStateChangeListener {
        AccessibilityInteractionConnectionManager() {
        }

        @Override
        public void onAccessibilityStateChanged(boolean z) {
            if (z) {
                ensureConnection();
                if (ViewRootImpl.this.mAttachInfo.mHasWindowFocus && ViewRootImpl.this.mView != null) {
                    ViewRootImpl.this.mView.sendAccessibilityEvent(32);
                    View viewFindFocus = ViewRootImpl.this.mView.findFocus();
                    if (viewFindFocus != null && viewFindFocus != ViewRootImpl.this.mView) {
                        viewFindFocus.sendAccessibilityEvent(8);
                        return;
                    }
                    return;
                }
                return;
            }
            ensureNoConnection();
            ViewRootImpl.this.mHandler.obtainMessage(21).sendToTarget();
        }

        public void ensureConnection() {
            if (!(ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId != -1)) {
                ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId = ViewRootImpl.this.mAccessibilityManager.addAccessibilityInteractionConnection(ViewRootImpl.this.mWindow, ViewRootImpl.this.mContext.getPackageName(), new AccessibilityInteractionConnection(ViewRootImpl.this));
            }
        }

        public void ensureNoConnection() {
            if (ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId != -1) {
                ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId = -1;
                ViewRootImpl.this.mAccessibilityManager.removeAccessibilityInteractionConnection(ViewRootImpl.this.mWindow);
            }
        }
    }

    final class HighContrastTextManager implements AccessibilityManager.HighTextContrastChangeListener {
        HighContrastTextManager() {
            ThreadedRenderer.setHighContrastText(ViewRootImpl.this.mAccessibilityManager.isHighTextContrastEnabled());
        }

        @Override
        public void onHighTextContrastStateChanged(boolean z) {
            ThreadedRenderer.setHighContrastText(z);
            ViewRootImpl.this.destroyHardwareResources();
            ViewRootImpl.this.invalidate();
        }
    }

    static final class AccessibilityInteractionConnection extends IAccessibilityInteractionConnection.Stub {
        private final WeakReference<ViewRootImpl> mViewRootImpl;

        AccessibilityInteractionConnection(ViewRootImpl viewRootImpl) {
            this.mViewRootImpl = new WeakReference<>(viewRootImpl);
        }

        @Override
        public void findAccessibilityNodeInfoByAccessibilityId(long j, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec, Bundle bundle) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfoByAccessibilityIdClientThread(j, region, i, iAccessibilityInteractionConnectionCallback, i2, i3, j2, magnificationSpec, bundle);
            } else {
                try {
                    iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfosResult(null, i);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void performAccessibilityAction(long j, int i, Bundle bundle, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().performAccessibilityActionClientThread(j, i, bundle, i2, iAccessibilityInteractionConnectionCallback, i3, i4, j2);
            } else {
                try {
                    iAccessibilityInteractionConnectionCallback.setPerformAccessibilityActionResult(false, i2);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void findAccessibilityNodeInfosByViewId(long j, String str, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfosByViewIdClientThread(j, str, region, i, iAccessibilityInteractionConnectionCallback, i2, i3, j2, magnificationSpec);
            } else {
                try {
                    iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfoResult(null, i);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void findAccessibilityNodeInfosByText(long j, String str, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfosByTextClientThread(j, str, region, i, iAccessibilityInteractionConnectionCallback, i2, i3, j2, magnificationSpec);
            } else {
                try {
                    iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfosResult(null, i);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void findFocus(long j, int i, Region region, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2, MagnificationSpec magnificationSpec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findFocusClientThread(j, i, region, i2, iAccessibilityInteractionConnectionCallback, i3, i4, j2, magnificationSpec);
            } else {
                try {
                    iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfoResult(null, i2);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void focusSearch(long j, int i, Region region, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2, MagnificationSpec magnificationSpec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().focusSearchClientThread(j, i, region, i2, iAccessibilityInteractionConnectionCallback, i3, i4, j2, magnificationSpec);
            } else {
                try {
                    iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfoResult(null, i2);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private class SendWindowContentChangedAccessibilityEvent implements Runnable {
        private int mChangeTypes;
        public long mLastEventTimeMillis;
        public StackTraceElement[] mOrigin;
        public View mSource;

        private SendWindowContentChangedAccessibilityEvent() {
            this.mChangeTypes = 0;
        }

        @Override
        public void run() {
            View view = this.mSource;
            this.mSource = null;
            if (view == null) {
                Log.e(ViewRootImpl.TAG, "Accessibility content change has no source");
                return;
            }
            if (AccessibilityManager.getInstance(ViewRootImpl.this.mContext).isEnabled()) {
                this.mLastEventTimeMillis = SystemClock.uptimeMillis();
                AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain();
                accessibilityEventObtain.setEventType(2048);
                accessibilityEventObtain.setContentChangeTypes(this.mChangeTypes);
                view.sendAccessibilityEventUnchecked(accessibilityEventObtain);
            } else {
                this.mLastEventTimeMillis = 0L;
            }
            view.resetSubtreeAccessibilityStateChanged();
            this.mChangeTypes = 0;
        }

        public void runOrPost(View view, int i) {
            if (ViewRootImpl.this.mHandler.getLooper() != Looper.myLooper()) {
                Log.e(ViewRootImpl.TAG, "Accessibility content change on non-UI thread. Future Android versions will throw an exception.", new CalledFromWrongThreadException("Only the original thread that created a view hierarchy can touch its views."));
                ViewRootImpl.this.mHandler.removeCallbacks(this);
                if (this.mSource != null) {
                    run();
                }
            }
            if (this.mSource != null) {
                View commonPredecessor = ViewRootImpl.this.getCommonPredecessor(this.mSource, view);
                if (commonPredecessor != null) {
                    commonPredecessor = commonPredecessor.getSelfOrParentImportantForA11y();
                }
                if (commonPredecessor != null) {
                    view = commonPredecessor;
                }
                this.mSource = view;
                this.mChangeTypes |= i;
                return;
            }
            this.mSource = view;
            this.mChangeTypes = i;
            long jUptimeMillis = SystemClock.uptimeMillis() - this.mLastEventTimeMillis;
            long sendRecurringAccessibilityEventsInterval = ViewConfiguration.getSendRecurringAccessibilityEventsInterval();
            if (jUptimeMillis >= sendRecurringAccessibilityEventsInterval) {
                removeCallbacksAndRun();
            } else {
                ViewRootImpl.this.mHandler.postDelayed(this, sendRecurringAccessibilityEventsInterval - jUptimeMillis);
            }
        }

        public void removeCallbacksAndRun() {
            ViewRootImpl.this.mHandler.removeCallbacks(this);
            run();
        }
    }

    private static class UnhandledKeyManager {
        private final SparseArray<WeakReference<View>> mCapturedKeys;
        private WeakReference<View> mCurrentReceiver;
        private boolean mDispatched;

        private UnhandledKeyManager() {
            this.mDispatched = true;
            this.mCapturedKeys = new SparseArray<>();
            this.mCurrentReceiver = null;
        }

        boolean dispatch(View view, KeyEvent keyEvent) {
            if (this.mDispatched) {
                return false;
            }
            try {
                Trace.traceBegin(8L, "UnhandledKeyEvent dispatch");
                this.mDispatched = true;
                View viewDispatchUnhandledKeyEvent = view.dispatchUnhandledKeyEvent(keyEvent);
                if (keyEvent.getAction() == 0) {
                    int keyCode = keyEvent.getKeyCode();
                    if (viewDispatchUnhandledKeyEvent != null && !KeyEvent.isModifierKey(keyCode)) {
                        this.mCapturedKeys.put(keyCode, new WeakReference<>(viewDispatchUnhandledKeyEvent));
                    }
                }
                return viewDispatchUnhandledKeyEvent != null;
            } finally {
                Trace.traceEnd(8L);
            }
        }

        void preDispatch(KeyEvent keyEvent) {
            int iIndexOfKey;
            this.mCurrentReceiver = null;
            if (keyEvent.getAction() == 1 && (iIndexOfKey = this.mCapturedKeys.indexOfKey(keyEvent.getKeyCode())) >= 0) {
                this.mCurrentReceiver = this.mCapturedKeys.valueAt(iIndexOfKey);
                this.mCapturedKeys.removeAt(iIndexOfKey);
            }
        }

        boolean preViewDispatch(KeyEvent keyEvent) {
            this.mDispatched = false;
            if (this.mCurrentReceiver == null) {
                this.mCurrentReceiver = this.mCapturedKeys.get(keyEvent.getKeyCode());
            }
            if (this.mCurrentReceiver == null) {
                return false;
            }
            View view = this.mCurrentReceiver.get();
            if (keyEvent.getAction() == 1) {
                this.mCurrentReceiver = null;
            }
            if (view != null && view.isAttachedToWindow()) {
                view.onUnhandledKeyEvent(keyEvent);
            }
            return true;
        }
    }
}
