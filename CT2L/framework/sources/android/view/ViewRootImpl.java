package android.view;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.ActivityManagerNative;
import android.content.ClipDescription;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.TtmlUtils;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.AndroidRuntimeException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Choreographer;
import android.view.HardwareRenderer;
import android.view.IWindow;
import android.view.InputDevice;
import android.view.InputQueue;
import android.view.KeyCharacterMap;
import android.view.Surface;
import android.view.SurfaceHolder;
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
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;
import com.android.internal.R;
import com.android.internal.os.SomeArgs;
import com.android.internal.policy.PolicyManager;
import com.android.internal.view.BaseSurfaceHolder;
import com.android.internal.view.RootViewSurfaceTaker;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;

public final class ViewRootImpl implements ViewParent, View.AttachInfo.Callbacks, HardwareRenderer.HardwareDrawCallbacks {
    private static final boolean DBG = false;
    private static final boolean DEBUG_CONFIGURATION = false;
    private static final boolean DEBUG_DIALOG = false;
    private static final boolean DEBUG_DRAW = false;
    private static final boolean DEBUG_FPS = false;
    private static final boolean DEBUG_IMF = false;
    private static final boolean DEBUG_INPUT_RESIZE = false;
    private static final boolean DEBUG_INPUT_STAGES = false;
    private static final boolean DEBUG_LAYOUT = false;
    private static final boolean DEBUG_ORIENTATION = false;
    private static final boolean DEBUG_TRACKBALL = false;
    private static final boolean LOCAL_LOGV = false;
    private static final int MAX_QUEUED_INPUT_EVENT_POOL_SIZE = 10;
    static final int MAX_TRACKBALL_DELAY = 250;
    private static final int MSG_CHECK_FOCUS = 13;
    private static final int MSG_CLEAR_ACCESSIBILITY_FOCUS_HOST = 21;
    private static final int MSG_CLOSE_SYSTEM_DIALOGS = 14;
    private static final int MSG_DIE = 3;
    private static final int MSG_DISPATCH_APP_VISIBILITY = 8;
    private static final int MSG_DISPATCH_DONE_ANIMATING = 22;
    private static final int MSG_DISPATCH_DRAG_EVENT = 15;
    private static final int MSG_DISPATCH_DRAG_LOCATION_EVENT = 16;
    private static final int MSG_DISPATCH_GET_NEW_SURFACE = 9;
    private static final int MSG_DISPATCH_INPUT_EVENT = 7;
    private static final int MSG_DISPATCH_KEY_FROM_IME = 11;
    private static final int MSG_DISPATCH_SYSTEM_UI_VISIBILITY = 17;
    private static final int MSG_DISPATCH_WINDOW_SHOWN = 26;
    private static final int MSG_FINISH_INPUT_CONNECTION = 12;
    private static final int MSG_INVALIDATE = 1;
    private static final int MSG_INVALIDATE_RECT = 2;
    private static final int MSG_INVALIDATE_WORLD = 23;
    private static final int MSG_PROCESS_INPUT_EVENTS = 19;
    private static final int MSG_RESIZED = 4;
    private static final int MSG_RESIZED_REPORT = 5;
    private static final int MSG_SYNTHESIZE_INPUT_EVENT = 25;
    private static final int MSG_UPDATE_CONFIGURATION = 18;
    private static final int MSG_WINDOW_FOCUS_CHANGED = 6;
    private static final int MSG_WINDOW_MOVED = 24;
    public static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    private static final String PROPERTY_MEDIA_DISABLED = "config.disable_media";
    private static final String PROPERTY_PROFILE_RENDERING = "viewroot.profile_rendering";
    private static final String TAG = "ViewRootImpl";
    View mAccessibilityFocusedHost;
    AccessibilityNodeInfo mAccessibilityFocusedVirtualView;
    AccessibilityInteractionConnectionManager mAccessibilityInteractionConnectionManager;
    AccessibilityInteractionController mAccessibilityInteractionController;
    final AccessibilityManager mAccessibilityManager;
    boolean mAdded;
    boolean mAddedTouchMode;
    boolean mApplyInsetsRequested;
    final View.AttachInfo mAttachInfo;
    AudioManager mAudioManager;
    final String mBasePackageName;
    boolean mBlockResizeBuffer;
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
    final Display mDisplay;
    final DisplayAdjustments mDisplayAdjustments;
    private final DisplayManager.DisplayListener mDisplayListener;
    final DisplayManager mDisplayManager;
    ClipDescription mDragDescription;
    boolean mDrawDuringWindowsAnimating;
    boolean mDrawingAllowed;
    FallbackEventHandler mFallbackEventHandler;
    boolean mFirst;
    InputStage mFirstInputStage;
    InputStage mFirstPostImeInputStage;
    private int mFpsNumFrames;
    boolean mFullRedrawNeeded;
    final ViewRootHandler mHandler;
    int mHardwareXOffset;
    int mHardwareYOffset;
    boolean mHasHadWindowFocus;
    int mHeight;
    HighContrastTextManager mHighContrastTextManager;
    InputChannel mInputChannel;
    protected final InputEventConsistencyVerifier mInputEventConsistencyVerifier;
    WindowInputEventReceiver mInputEventReceiver;
    InputQueue mInputQueue;
    InputQueue.Callback mInputQueueCallback;
    final InvalidateOnAnimationRunnable mInvalidateOnAnimationRunnable;
    boolean mIsAnimating;
    private boolean mIsCircularEmulator;
    boolean mIsCreating;
    boolean mIsDrawing;
    private boolean mIsEmulator;
    boolean mIsInTraversal;
    boolean mLastOverscanRequested;
    WeakReference<View> mLastScrolledFocus;
    int mLastSystemUiVisibility;
    boolean mLastWasImTarget;
    boolean mLayoutRequested;
    volatile Object mLocalDragState;
    final WindowLeaked mLocation;
    private boolean mMediaDisabled;
    boolean mNewSurfaceNeeded;
    private final int mNoncompatDensity;
    int mPendingInputEventCount;
    QueuedInputEvent mPendingInputEventHead;
    QueuedInputEvent mPendingInputEventTail;
    private ArrayList<LayoutTransition> mPendingTransitions;
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
    int mResizeAlpha;
    HardwareLayer mResizeBuffer;
    int mResizeBufferDuration;
    long mResizeBufferStartTime;
    final Paint mResizePaint;
    boolean mScrollMayChange;
    int mScrollY;
    Scroller mScroller;
    SendWindowContentChangedAccessibilityEvent mSendWindowContentChangedAccessibilityEvent;
    int mSeq;
    int mSoftInputMode;
    BaseSurfaceHolder mSurfaceHolder;
    SurfaceHolder.Callback2 mSurfaceHolderCallback;
    InputStage mSyntheticInputStage;
    final int mTargetSdkVersion;
    HashSet<View> mTempHashSet;
    final Rect mTempRect;
    final Thread mThread;
    CompatibilityInfo.Translator mTranslator;
    final Region mTransparentRegion;
    int mTraversalBarrier;
    final TraversalRunnable mTraversalRunnable;
    boolean mTraversalScheduled;
    boolean mUnbufferedInputDispatch;
    View mView;
    final ViewConfiguration mViewConfiguration;
    private int mViewLayoutDirectionInitial;
    int mViewVisibility;
    final Rect mVisRect;
    int mWidth;
    boolean mWillDrawSoon;
    final Rect mWinFrame;
    final W mWindow;
    private final boolean mWindowIsRound;
    final IWindowSession mWindowSession;
    boolean mWindowsAnimating;
    static final ThreadLocal<RunQueue> sRunQueues = new ThreadLocal<>();
    static final ArrayList<Runnable> sFirstDrawHandlers = new ArrayList<>();
    static boolean sFirstDrawComplete = false;
    static final ArrayList<ComponentCallbacks> sConfigCallbacks = new ArrayList<>();
    static final Interpolator mResizeInterpolator = new AccelerateDecelerateInterpolator();
    final int[] mTmpLocation = new int[2];
    final TypedValue mTmpValue = new TypedValue();
    final WindowManager.LayoutParams mWindowAttributes = new WindowManager.LayoutParams();
    boolean mAppVisible = true;
    int mOrigWindowType = -1;
    boolean mStopped = false;
    boolean mLastInCompatMode = false;
    String mPendingInputEventQueueLengthCounterName = "pq";
    boolean mWindowAttributesChanged = false;
    int mWindowAttributesChangesFlag = 0;
    final Surface mSurface = new Surface();
    final Rect mPendingOverscanInsets = new Rect();
    final Rect mPendingVisibleInsets = new Rect();
    final Rect mPendingStableInsets = new Rect();
    final Rect mPendingContentInsets = new Rect();
    final ViewTreeObserver.InternalInsetsInfo mLastGivenInsets = new ViewTreeObserver.InternalInsetsInfo();
    final Rect mDispatchContentInsets = new Rect();
    final Rect mDispatchStableInsets = new Rect();
    final Configuration mLastConfiguration = new Configuration();
    final Configuration mPendingConfiguration = new Configuration();
    final PointF mDragPoint = new PointF();
    final PointF mLastTouchPoint = new PointF();
    private long mFpsStartTime = -1;
    private long mFpsPrevTime = -1;
    private boolean mInLayout = false;
    ArrayList<View> mLayoutRequesters = new ArrayList<>();
    boolean mHandlingLayoutInLayoutRequest = false;

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
        this.mProfile = false;
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayChanged(int displayId) {
                int oldDisplayState;
                int newDisplayState;
                if (ViewRootImpl.this.mView != null && ViewRootImpl.this.mDisplay.getDisplayId() == displayId && (oldDisplayState = ViewRootImpl.this.mAttachInfo.mDisplayState) != (newDisplayState = ViewRootImpl.this.mDisplay.getState())) {
                    ViewRootImpl.this.mAttachInfo.mDisplayState = newDisplayState;
                    if (oldDisplayState != 0) {
                        int oldScreenState = toViewScreenState(oldDisplayState);
                        int newScreenState = toViewScreenState(newDisplayState);
                        if (oldScreenState != newScreenState) {
                            ViewRootImpl.this.mView.dispatchScreenStateChanged(newScreenState);
                        }
                        if (oldDisplayState == 1) {
                            ViewRootImpl.this.mFullRedrawNeeded = true;
                            ViewRootImpl.this.scheduleTraversals();
                        }
                    }
                }
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayAdded(int displayId) {
            }

            private int toViewScreenState(int displayState) {
                return displayState == 1 ? 0 : 1;
            }
        };
        this.mResizePaint = new Paint();
        this.mHandler = new ViewRootHandler();
        this.mTraversalRunnable = new TraversalRunnable();
        this.mConsumedBatchedInputRunnable = new ConsumeBatchedInputRunnable();
        this.mConsumeBatchedInputImmediatelyRunnable = new ConsumeBatchedInputImmediatelyRunnable();
        this.mInvalidateOnAnimationRunnable = new InvalidateOnAnimationRunnable();
        this.mContext = context;
        this.mWindowSession = WindowManagerGlobal.getWindowSession();
        this.mDisplay = display;
        this.mBasePackageName = context.getBasePackageName();
        this.mDisplayAdjustments = display.getDisplayAdjustments();
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
        this.mAttachInfo = new View.AttachInfo(this.mWindowSession, this.mWindow, display, this, this.mHandler, this);
        this.mAccessibilityManager = AccessibilityManager.getInstance(context);
        this.mAccessibilityInteractionConnectionManager = new AccessibilityInteractionConnectionManager();
        this.mAccessibilityManager.addAccessibilityStateChangeListener(this.mAccessibilityInteractionConnectionManager);
        this.mHighContrastTextManager = new HighContrastTextManager();
        this.mAccessibilityManager.addHighTextContrastStateChangeListener(this.mHighContrastTextManager);
        this.mViewConfiguration = ViewConfiguration.get(context);
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
        this.mNoncompatDensity = context.getResources().getDisplayMetrics().noncompatDensityDpi;
        this.mFallbackEventHandler = PolicyManager.makeNewFallbackEventHandler(context);
        this.mChoreographer = Choreographer.getInstance();
        this.mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        loadSystemProperties();
        this.mWindowIsRound = context.getResources().getBoolean(R.bool.config_windowIsRound);
    }

    public static void addFirstDrawHandler(Runnable callback) {
        synchronized (sFirstDrawHandlers) {
            if (!sFirstDrawComplete) {
                sFirstDrawHandlers.add(callback);
            }
        }
    }

    public static void addConfigCallback(ComponentCallbacks callback) {
        synchronized (sConfigCallbacks) {
            sConfigCallbacks.add(callback);
        }
    }

    public void profile() {
        this.mProfile = true;
    }

    static boolean isInTouchMode() {
        IWindowSession windowSession = WindowManagerGlobal.peekWindowSession();
        if (windowSession != null) {
            try {
                return windowSession.getInTouchMode();
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
        synchronized (this) {
            if (this.mView == null) {
                this.mView = view;
                this.mAttachInfo.mDisplayState = this.mDisplay.getState();
                this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
                this.mViewLayoutDirectionInitial = this.mView.getRawLayoutDirection();
                this.mFallbackEventHandler.setView(view);
                this.mWindowAttributes.copyFrom(attrs);
                if (this.mWindowAttributes.packageName == null) {
                    this.mWindowAttributes.packageName = this.mBasePackageName;
                }
                WindowManager.LayoutParams attrs2 = this.mWindowAttributes;
                this.mClientWindowLayoutFlags = attrs2.flags;
                setAccessibilityFocus(null, null);
                if (view instanceof RootViewSurfaceTaker) {
                    this.mSurfaceHolderCallback = ((RootViewSurfaceTaker) view).willYouTakeTheSurface();
                    if (this.mSurfaceHolderCallback != null) {
                        this.mSurfaceHolder = new TakenSurfaceHolder();
                        this.mSurfaceHolder.setFormat(0);
                    }
                }
                int surfaceInset = (int) Math.ceil(view.getZ() * 2.0f);
                attrs2.surfaceInsets.set(surfaceInset, surfaceInset, surfaceInset, surfaceInset);
                CompatibilityInfo compatibilityInfo = this.mDisplayAdjustments.getCompatibilityInfo();
                this.mTranslator = compatibilityInfo.getTranslator();
                this.mDisplayAdjustments.setActivityToken(attrs2.token);
                if (this.mSurfaceHolder == null) {
                    enableHardwareAcceleration(attrs2);
                }
                boolean restore = false;
                if (this.mTranslator != null) {
                    this.mSurface.setCompatibilityTranslator(this.mTranslator);
                    restore = true;
                    attrs2.backup();
                    this.mTranslator.translateWindowLayout(attrs2);
                }
                if (!compatibilityInfo.supportsScreen()) {
                    attrs2.privateFlags |= 128;
                    this.mLastInCompatMode = true;
                }
                this.mSoftInputMode = attrs2.softInputMode;
                this.mWindowAttributesChanged = true;
                this.mWindowAttributesChangesFlag = -1;
                this.mAttachInfo.mRootView = view;
                this.mAttachInfo.mScalingRequired = this.mTranslator != null;
                this.mAttachInfo.mApplicationScale = this.mTranslator == null ? 1.0f : this.mTranslator.applicationScale;
                if (panelParentView != null) {
                    this.mAttachInfo.mPanelParentWindowToken = panelParentView.getApplicationWindowToken();
                }
                this.mAdded = true;
                requestLayout();
                if ((this.mWindowAttributes.inputFeatures & 2) == 0) {
                    this.mInputChannel = new InputChannel();
                }
                try {
                    try {
                        this.mOrigWindowType = this.mWindowAttributes.type;
                        this.mAttachInfo.mRecomputeGlobalAttributes = true;
                        collectViewAttributes();
                        int res = this.mWindowSession.addToDisplay(this.mWindow, this.mSeq, this.mWindowAttributes, getHostVisibility(), this.mDisplay.getDisplayId(), this.mAttachInfo.mContentInsets, this.mAttachInfo.mStableInsets, this.mInputChannel);
                        if (this.mTranslator != null) {
                            this.mTranslator.translateRectInScreenToAppWindow(this.mAttachInfo.mContentInsets);
                        }
                        this.mPendingOverscanInsets.set(0, 0, 0, 0);
                        this.mPendingContentInsets.set(this.mAttachInfo.mContentInsets);
                        this.mPendingStableInsets.set(this.mAttachInfo.mStableInsets);
                        this.mPendingVisibleInsets.set(0, 0, 0, 0);
                        if (res < 0) {
                            this.mAttachInfo.mRootView = null;
                            this.mAdded = false;
                            this.mFallbackEventHandler.setView(null);
                            unscheduleTraversals();
                            setAccessibilityFocus(null, null);
                            switch (res) {
                                case -10:
                                    throw new WindowManager.InvalidDisplayException("Unable to add window " + this.mWindow + " -- the specified window type is not valid");
                                case -9:
                                    throw new WindowManager.InvalidDisplayException("Unable to add window " + this.mWindow + " -- the specified display can not be found");
                                case -8:
                                    throw new WindowManager.BadTokenException("Unable to add window " + this.mWindow + " -- permission denied for this window type");
                                case -7:
                                    throw new WindowManager.BadTokenException("Unable to add window " + this.mWindow + " -- another window of this type already exists");
                                case -6:
                                    return;
                                case -5:
                                    throw new WindowManager.BadTokenException("Unable to add window -- window " + this.mWindow + " has already been added");
                                case -4:
                                    throw new WindowManager.BadTokenException("Unable to add window -- app for token " + attrs2.token + " is exiting");
                                case -3:
                                    throw new WindowManager.BadTokenException("Unable to add window -- token " + attrs2.token + " is not for an application");
                                case -2:
                                case -1:
                                    throw new WindowManager.BadTokenException("Unable to add window -- token " + attrs2.token + " is not valid; is your activity running?");
                                default:
                                    throw new RuntimeException("Unable to add window -- unknown error code " + res);
                            }
                        }
                        if (view instanceof RootViewSurfaceTaker) {
                            this.mInputQueueCallback = ((RootViewSurfaceTaker) view).willYouTakeTheInputQueue();
                        }
                        if (this.mInputChannel != null) {
                            if (this.mInputQueueCallback != null) {
                                this.mInputQueue = new InputQueue();
                                this.mInputQueueCallback.onInputQueueCreated(this.mInputQueue);
                            }
                            this.mInputEventReceiver = new WindowInputEventReceiver(this.mInputChannel, Looper.myLooper());
                        }
                        view.assignParent(this);
                        this.mAddedTouchMode = (res & 1) != 0;
                        this.mAppVisible = (res & 2) != 0;
                        if (this.mAccessibilityManager.isEnabled()) {
                            this.mAccessibilityInteractionConnectionManager.ensureConnection();
                        }
                        if (view.getImportantForAccessibility() == 0) {
                            view.setImportantForAccessibility(1);
                        }
                        CharSequence counterSuffix = attrs2.getTitle();
                        this.mSyntheticInputStage = new SyntheticInputStage();
                        InputStage viewPostImeStage = new ViewPostImeInputStage(this.mSyntheticInputStage);
                        InputStage nativePostImeStage = new NativePostImeInputStage(viewPostImeStage, "aq:native-post-ime:" + ((Object) counterSuffix));
                        InputStage earlyPostImeStage = new EarlyPostImeInputStage(nativePostImeStage);
                        InputStage imeStage = new ImeInputStage(earlyPostImeStage, "aq:ime:" + ((Object) counterSuffix));
                        InputStage viewPreImeStage = new ViewPreImeInputStage(imeStage);
                        InputStage nativePreImeStage = new NativePreImeInputStage(viewPreImeStage, "aq:native-pre-ime:" + ((Object) counterSuffix));
                        this.mFirstInputStage = nativePreImeStage;
                        this.mFirstPostImeInputStage = earlyPostImeStage;
                        this.mPendingInputEventQueueLengthCounterName = "aq:pending:" + ((Object) counterSuffix);
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
                    if (restore) {
                        attrs2.restore();
                    }
                }
            }
        }
    }

    private boolean isInLocalFocusMode() {
        return (this.mWindowAttributes.flags & 268435456) != 0;
    }

    void destroyHardwareResources() {
        if (this.mAttachInfo.mHardwareRenderer != null) {
            this.mAttachInfo.mHardwareRenderer.destroyHardwareResources(this.mView);
            this.mAttachInfo.mHardwareRenderer.destroy();
        }
    }

    public void detachFunctor(long functor) {
        this.mBlockResizeBuffer = true;
        if (this.mAttachInfo.mHardwareRenderer != null) {
            this.mAttachInfo.mHardwareRenderer.stopDrawing();
        }
    }

    public void invokeFunctor(long functor, boolean waitForCompletion) {
        ThreadedRenderer.invokeFunctor(functor, waitForCompletion);
    }

    public void registerAnimatingRenderNode(RenderNode animator) {
        if (this.mAttachInfo.mHardwareRenderer != null) {
            this.mAttachInfo.mHardwareRenderer.registerAnimatingRenderNode(animator);
            return;
        }
        if (this.mAttachInfo.mPendingAnimatingRenderNodes == null) {
            this.mAttachInfo.mPendingAnimatingRenderNodes = new ArrayList();
        }
        this.mAttachInfo.mPendingAnimatingRenderNodes.add(animator);
    }

    private void enableHardwareAcceleration(WindowManager.LayoutParams attrs) {
        this.mAttachInfo.mHardwareAccelerated = false;
        this.mAttachInfo.mHardwareAccelerationRequested = false;
        if (this.mTranslator == null) {
            boolean hardwareAccelerated = (attrs.flags & 16777216) != 0;
            if (hardwareAccelerated && HardwareRenderer.isAvailable()) {
                boolean fakeHwAccelerated = (attrs.privateFlags & 1) != 0;
                boolean forceHwAccelerated = (attrs.privateFlags & 2) != 0;
                if (fakeHwAccelerated) {
                    this.mAttachInfo.mHardwareAccelerationRequested = true;
                    return;
                }
                if (!HardwareRenderer.sRendererDisabled || (HardwareRenderer.sSystemRendererDisabled && forceHwAccelerated)) {
                    if (this.mAttachInfo.mHardwareRenderer != null) {
                        this.mAttachInfo.mHardwareRenderer.destroy();
                    }
                    Rect insets = attrs.surfaceInsets;
                    boolean hasSurfaceInsets = (insets.left == 0 && insets.right == 0 && insets.top == 0 && insets.bottom == 0) ? false : true;
                    boolean translucent = attrs.format != -1 || hasSurfaceInsets;
                    this.mAttachInfo.mHardwareRenderer = HardwareRenderer.create(this.mContext, translucent);
                    if (this.mAttachInfo.mHardwareRenderer != null) {
                        this.mAttachInfo.mHardwareRenderer.setName(attrs.getTitle().toString());
                        View.AttachInfo attachInfo = this.mAttachInfo;
                        this.mAttachInfo.mHardwareAccelerationRequested = true;
                        attachInfo.mHardwareAccelerated = true;
                    }
                }
            }
        }
    }

    public View getView() {
        return this.mView;
    }

    final WindowLeaked getLocation() {
        return this.mLocation;
    }

    void setLayoutParams(WindowManager.LayoutParams attrs, boolean newView) {
        synchronized (this) {
            int oldInsetLeft = this.mWindowAttributes.surfaceInsets.left;
            int oldInsetTop = this.mWindowAttributes.surfaceInsets.top;
            int oldInsetRight = this.mWindowAttributes.surfaceInsets.right;
            int oldInsetBottom = this.mWindowAttributes.surfaceInsets.bottom;
            int oldSoftInputMode = this.mWindowAttributes.softInputMode;
            this.mClientWindowLayoutFlags = attrs.flags;
            int compatibleWindowFlag = this.mWindowAttributes.privateFlags & 128;
            attrs.systemUiVisibility = this.mWindowAttributes.systemUiVisibility;
            attrs.subtreeSystemUiVisibility = this.mWindowAttributes.subtreeSystemUiVisibility;
            this.mWindowAttributesChangesFlag = this.mWindowAttributes.copyFrom(attrs);
            if ((this.mWindowAttributesChangesFlag & 524288) != 0) {
                this.mAttachInfo.mRecomputeGlobalAttributes = true;
            }
            if (this.mWindowAttributes.packageName == null) {
                this.mWindowAttributes.packageName = this.mBasePackageName;
            }
            this.mWindowAttributes.privateFlags |= compatibleWindowFlag;
            this.mWindowAttributes.surfaceInsets.set(oldInsetLeft, oldInsetTop, oldInsetRight, oldInsetBottom);
            applyKeepScreenOnFlag(this.mWindowAttributes);
            if (newView) {
                this.mSoftInputMode = attrs.softInputMode;
                requestLayout();
            }
            if ((attrs.softInputMode & 240) == 0) {
                this.mWindowAttributes.softInputMode = (this.mWindowAttributes.softInputMode & (-241)) | (oldSoftInputMode & 240);
            }
            this.mWindowAttributesChanged = true;
            scheduleTraversals();
        }
    }

    void handleAppVisibility(boolean visible) {
        if (this.mAppVisible != visible) {
            this.mAppVisible = visible;
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

    @Override
    public void requestFitSystemWindows() {
        checkThread();
        this.mApplyInsetsRequested = true;
        scheduleTraversals();
    }

    @Override
    public void requestLayout() {
        if (!this.mHandlingLayoutInLayoutRequest) {
            checkThread();
            this.mLayoutRequested = true;
            scheduleTraversals();
        }
    }

    @Override
    public boolean isLayoutRequested() {
        return this.mLayoutRequested;
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
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                invalidateWorld(parent.getChildAt(i));
            }
        }
    }

    @Override
    public void invalidateChild(View child, Rect dirty) {
        invalidateChildInParent(null, dirty);
    }

    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        checkThread();
        if (dirty == null) {
            invalidate();
        } else if (!dirty.isEmpty() || this.mIsAnimating) {
            if (this.mCurScrollY != 0 || this.mTranslator != null) {
                this.mTempRect.set(dirty);
                dirty = this.mTempRect;
                if (this.mCurScrollY != 0) {
                    dirty.offset(0, -this.mCurScrollY);
                }
                if (this.mTranslator != null) {
                    this.mTranslator.translateRectInAppWindowToScreen(dirty);
                }
                if (this.mAttachInfo.mScalingRequired) {
                    dirty.inset(-1, -1);
                }
            }
            Rect localDirty = this.mDirty;
            if (!localDirty.isEmpty() && !localDirty.contains(dirty)) {
                this.mAttachInfo.mSetIgnoreDirtyState = true;
                this.mAttachInfo.mIgnoreDirtyState = true;
            }
            localDirty.union(dirty.left, dirty.top, dirty.right, dirty.bottom);
            float appScale = this.mAttachInfo.mApplicationScale;
            boolean intersected = localDirty.intersect(0, 0, (int) ((this.mWidth * appScale) + 0.5f), (int) ((this.mHeight * appScale) + 0.5f));
            if (!intersected) {
                localDirty.setEmpty();
            }
            if (!this.mWillDrawSoon && (intersected || this.mIsAnimating)) {
                scheduleTraversals();
            }
        }
        return null;
    }

    void setStopped(boolean stopped) {
        if (this.mStopped != stopped) {
            this.mStopped = stopped;
            if (!stopped) {
                scheduleTraversals();
            }
        }
    }

    @Override
    public ViewParent getParent() {
        return null;
    }

    @Override
    public boolean getChildVisibleRect(View child, Rect r, Point offset) {
        if (child != this.mView) {
            throw new RuntimeException("child is not mine, honest!");
        }
        return r.intersect(0, 0, this.mWidth, this.mHeight);
    }

    @Override
    public void bringChildToFront(View child) {
    }

    int getHostVisibility() {
        if (this.mAppVisible) {
            return this.mView.getVisibility();
        }
        return 8;
    }

    void disposeResizeBuffer() {
        if (this.mResizeBuffer != null) {
            this.mResizeBuffer.destroy();
            this.mResizeBuffer = null;
        }
    }

    public void requestTransitionStart(LayoutTransition transition) {
        if (this.mPendingTransitions == null || !this.mPendingTransitions.contains(transition)) {
            if (this.mPendingTransitions == null) {
                this.mPendingTransitions = new ArrayList<>();
            }
            this.mPendingTransitions.add(transition);
        }
    }

    void notifyRendererOfFramePending() {
        if (this.mAttachInfo.mHardwareRenderer != null) {
            this.mAttachInfo.mHardwareRenderer.notifyFramePending();
        }
    }

    void scheduleTraversals() {
        if (!this.mTraversalScheduled) {
            this.mTraversalScheduled = true;
            this.mTraversalBarrier = this.mHandler.getLooper().postSyncBarrier();
            this.mChoreographer.postCallback(2, this.mTraversalRunnable, null);
            if (!this.mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            notifyRendererOfFramePending();
        }
    }

    void unscheduleTraversals() {
        if (this.mTraversalScheduled) {
            this.mTraversalScheduled = false;
            this.mHandler.getLooper().removeSyncBarrier(this.mTraversalBarrier);
            this.mChoreographer.removeCallbacks(2, this.mTraversalRunnable, null);
        }
    }

    void doTraversal() {
        if (this.mTraversalScheduled) {
            this.mTraversalScheduled = false;
            this.mHandler.getLooper().removeSyncBarrier(this.mTraversalBarrier);
            if (this.mProfile) {
                Debug.startMethodTracing("ViewAncestor");
            }
            Trace.traceBegin(8L, "performTraversals");
            try {
                performTraversals();
                Trace.traceEnd(8L);
                if (this.mProfile) {
                    Debug.stopMethodTracing();
                    this.mProfile = false;
                }
            } catch (Throwable th) {
                Trace.traceEnd(8L);
                throw th;
            }
        }
    }

    private void applyKeepScreenOnFlag(WindowManager.LayoutParams params) {
        if (this.mAttachInfo.mKeepScreenOn) {
            params.flags |= 128;
        } else {
            params.flags = (params.flags & (-129)) | (this.mClientWindowLayoutFlags & 128);
        }
    }

    private boolean collectViewAttributes() {
        if (!this.mAttachInfo.mRecomputeGlobalAttributes) {
            return false;
        }
        this.mAttachInfo.mRecomputeGlobalAttributes = false;
        boolean oldScreenOn = this.mAttachInfo.mKeepScreenOn;
        this.mAttachInfo.mKeepScreenOn = false;
        this.mAttachInfo.mSystemUiVisibility = 0;
        this.mAttachInfo.mHasSystemUiListeners = false;
        this.mView.dispatchCollectViewAttributes(this.mAttachInfo, 0);
        this.mAttachInfo.mSystemUiVisibility &= this.mAttachInfo.mDisabledSystemUiVisibility ^ (-1);
        WindowManager.LayoutParams params = this.mWindowAttributes;
        this.mAttachInfo.mSystemUiVisibility |= getImpliedSystemUiVisibility(params);
        if (this.mAttachInfo.mKeepScreenOn == oldScreenOn && this.mAttachInfo.mSystemUiVisibility == params.subtreeSystemUiVisibility && this.mAttachInfo.mHasSystemUiListeners == params.hasSystemUiListeners) {
            return false;
        }
        applyKeepScreenOnFlag(params);
        params.subtreeSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
        params.hasSystemUiListeners = this.mAttachInfo.mHasSystemUiListeners;
        this.mView.dispatchWindowSystemUiVisiblityChanged(this.mAttachInfo.mSystemUiVisibility);
        return true;
    }

    private int getImpliedSystemUiVisibility(WindowManager.LayoutParams params) {
        int vis = 0;
        if ((params.flags & 67108864) != 0) {
            vis = 0 | 1280;
        }
        if ((params.flags & 134217728) != 0) {
            return vis | 768;
        }
        return vis;
    }

    private boolean measureHierarchy(View host, WindowManager.LayoutParams lp, Resources res, int desiredWindowWidth, int desiredWindowHeight) {
        boolean goodMeasure = false;
        if (lp.width == -2) {
            DisplayMetrics packageMetrics = res.getDisplayMetrics();
            res.getValue(R.dimen.config_prefDialogWidth, this.mTmpValue, true);
            int baseSize = 0;
            if (this.mTmpValue.type == 5) {
                baseSize = (int) this.mTmpValue.getDimension(packageMetrics);
            }
            if (baseSize != 0 && desiredWindowWidth > baseSize) {
                int childWidthMeasureSpec = getRootMeasureSpec(baseSize, lp.width);
                int childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight, lp.height);
                performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                if ((host.getMeasuredWidthAndState() & 16777216) == 0) {
                    goodMeasure = true;
                } else {
                    int childWidthMeasureSpec2 = getRootMeasureSpec((baseSize + desiredWindowWidth) / 2, lp.width);
                    performMeasure(childWidthMeasureSpec2, childHeightMeasureSpec);
                    if ((host.getMeasuredWidthAndState() & 16777216) == 0) {
                        goodMeasure = true;
                    }
                }
            }
        }
        if (goodMeasure) {
            return false;
        }
        int childWidthMeasureSpec3 = getRootMeasureSpec(desiredWindowWidth, lp.width);
        performMeasure(childWidthMeasureSpec3, getRootMeasureSpec(desiredWindowHeight, lp.height));
        if (this.mWidth == host.getMeasuredWidth() && this.mHeight == host.getMeasuredHeight()) {
            return false;
        }
        return true;
    }

    void transformMatrixToGlobal(Matrix m) {
        m.preTranslate(this.mAttachInfo.mWindowLeft, this.mAttachInfo.mWindowTop);
    }

    void transformMatrixToLocal(Matrix m) {
        m.postTranslate(-this.mAttachInfo.mWindowLeft, -this.mAttachInfo.mWindowTop);
    }

    void dispatchApplyInsets(View host) {
        this.mDispatchContentInsets.set(this.mAttachInfo.mContentInsets);
        this.mDispatchStableInsets.set(this.mAttachInfo.mStableInsets);
        boolean isRound = (this.mIsEmulator && this.mIsCircularEmulator) || this.mWindowIsRound;
        host.dispatchApplyWindowInsets(new WindowInsets(this.mDispatchContentInsets, null, this.mDispatchStableInsets, isRound));
    }

    private void performTraversals() {
        int desiredWindowWidth;
        int desiredWindowHeight;
        Rect contentInsets;
        Rect visibleInsets;
        Region touchableRegion;
        View host = this.mView;
        if (host != null && this.mAdded) {
            this.mIsInTraversal = true;
            this.mWillDrawSoon = true;
            boolean windowSizeMayChange = false;
            boolean newSurface = false;
            boolean surfaceChanged = false;
            WindowManager.LayoutParams lp = this.mWindowAttributes;
            int viewVisibility = getHostVisibility();
            boolean viewVisibilityChanged = this.mViewVisibility != viewVisibility || this.mNewSurfaceNeeded;
            WindowManager.LayoutParams params = null;
            if (this.mWindowAttributesChanged) {
                this.mWindowAttributesChanged = false;
                surfaceChanged = true;
                params = lp;
            }
            CompatibilityInfo compatibilityInfo = this.mDisplayAdjustments.getCompatibilityInfo();
            if (compatibilityInfo.supportsScreen() == this.mLastInCompatMode) {
                params = lp;
                this.mFullRedrawNeeded = true;
                this.mLayoutRequested = true;
                if (this.mLastInCompatMode) {
                    params.privateFlags &= -129;
                    this.mLastInCompatMode = false;
                } else {
                    params.privateFlags |= 128;
                    this.mLastInCompatMode = true;
                }
            }
            this.mWindowAttributesChangesFlag = 0;
            Rect frame = this.mWinFrame;
            if (this.mFirst) {
                this.mFullRedrawNeeded = true;
                this.mLayoutRequested = true;
                if (lp.type == 2014 || lp.type == 2011) {
                    Point size = new Point();
                    this.mDisplay.getRealSize(size);
                    desiredWindowWidth = size.x;
                    desiredWindowHeight = size.y;
                } else {
                    DisplayMetrics packageMetrics = this.mView.getContext().getResources().getDisplayMetrics();
                    desiredWindowWidth = packageMetrics.widthPixels;
                    desiredWindowHeight = packageMetrics.heightPixels;
                }
                this.mAttachInfo.mUse32BitDrawingCache = true;
                this.mAttachInfo.mHasWindowFocus = false;
                this.mAttachInfo.mWindowVisibility = viewVisibility;
                this.mAttachInfo.mRecomputeGlobalAttributes = false;
                viewVisibilityChanged = false;
                this.mLastConfiguration.setTo(host.getResources().getConfiguration());
                this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                if (this.mViewLayoutDirectionInitial == 2) {
                    host.setLayoutDirection(this.mLastConfiguration.getLayoutDirection());
                }
                host.dispatchAttachedToWindow(this.mAttachInfo, 0);
                this.mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
                dispatchApplyInsets(host);
            } else {
                desiredWindowWidth = frame.width();
                desiredWindowHeight = frame.height();
                if (desiredWindowWidth != this.mWidth || desiredWindowHeight != this.mHeight) {
                    this.mFullRedrawNeeded = true;
                    this.mLayoutRequested = true;
                    windowSizeMayChange = true;
                }
            }
            if (viewVisibilityChanged) {
                this.mAttachInfo.mWindowVisibility = viewVisibility;
                host.dispatchWindowVisibilityChanged(viewVisibility);
                if (viewVisibility != 0 || this.mNewSurfaceNeeded) {
                    destroyHardwareResources();
                }
                if (viewVisibility == 8) {
                    this.mHasHadWindowFocus = false;
                }
            }
            getRunQueue().executeActions(this.mAttachInfo.mHandler);
            boolean insetsChanged = false;
            boolean layoutRequested = this.mLayoutRequested && !this.mStopped;
            if (layoutRequested) {
                Resources res = this.mView.getContext().getResources();
                if (this.mFirst) {
                    this.mAttachInfo.mInTouchMode = !this.mAddedTouchMode;
                    ensureTouchModeLocally(this.mAddedTouchMode);
                } else {
                    if (!this.mPendingOverscanInsets.equals(this.mAttachInfo.mOverscanInsets)) {
                        insetsChanged = true;
                    }
                    if (!this.mPendingContentInsets.equals(this.mAttachInfo.mContentInsets)) {
                        insetsChanged = true;
                    }
                    if (!this.mPendingStableInsets.equals(this.mAttachInfo.mStableInsets)) {
                        insetsChanged = true;
                    }
                    if (!this.mPendingVisibleInsets.equals(this.mAttachInfo.mVisibleInsets)) {
                        this.mAttachInfo.mVisibleInsets.set(this.mPendingVisibleInsets);
                    }
                    if (lp.width == -2 || lp.height == -2) {
                        windowSizeMayChange = true;
                        if (lp.type == 2014 || lp.type == 2011) {
                            Point size2 = new Point();
                            this.mDisplay.getRealSize(size2);
                            desiredWindowWidth = size2.x;
                            desiredWindowHeight = size2.y;
                        } else {
                            DisplayMetrics packageMetrics2 = res.getDisplayMetrics();
                            desiredWindowWidth = packageMetrics2.widthPixels;
                            desiredWindowHeight = packageMetrics2.heightPixels;
                        }
                    }
                }
                windowSizeMayChange |= measureHierarchy(host, lp, res, desiredWindowWidth, desiredWindowHeight);
            }
            if (collectViewAttributes()) {
                params = lp;
            }
            if (this.mAttachInfo.mForceReportNewAttributes) {
                this.mAttachInfo.mForceReportNewAttributes = false;
                params = lp;
            }
            if (this.mFirst || this.mAttachInfo.mViewVisibilityChanged) {
                this.mAttachInfo.mViewVisibilityChanged = false;
                int resizeMode = this.mSoftInputMode & 240;
                if (resizeMode == 0) {
                    int N = this.mAttachInfo.mScrollContainers.size();
                    for (int i = 0; i < N; i++) {
                        if (this.mAttachInfo.mScrollContainers.get(i).isShown()) {
                            resizeMode = 16;
                        }
                    }
                    if (resizeMode == 0) {
                        resizeMode = 32;
                    }
                    if ((lp.softInputMode & 240) != resizeMode) {
                        lp.softInputMode = (lp.softInputMode & (-241)) | resizeMode;
                        params = lp;
                    }
                }
            }
            if (params != null) {
                if ((host.mPrivateFlags & 512) != 0 && !PixelFormat.formatHasAlpha(params.format)) {
                    params.format = -3;
                }
                this.mAttachInfo.mOverscanRequested = (params.flags & 33554432) != 0;
            }
            if (this.mApplyInsetsRequested) {
                this.mApplyInsetsRequested = false;
                this.mLastOverscanRequested = this.mAttachInfo.mOverscanRequested;
                dispatchApplyInsets(host);
                if (this.mLayoutRequested) {
                    windowSizeMayChange |= measureHierarchy(host, lp, this.mView.getContext().getResources(), desiredWindowWidth, desiredWindowHeight);
                }
            }
            if (layoutRequested) {
                this.mLayoutRequested = false;
            }
            boolean windowShouldResize = layoutRequested && windowSizeMayChange && !(this.mWidth == host.getMeasuredWidth() && this.mHeight == host.getMeasuredHeight() && ((lp.width != -2 || frame.width() >= desiredWindowWidth || frame.width() == this.mWidth) && (lp.height != -2 || frame.height() >= desiredWindowHeight || frame.height() == this.mHeight)));
            boolean computesInternalInsets = this.mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners() || this.mAttachInfo.mHasNonEmptyGivenInternalInsets;
            boolean insetsPending = false;
            int relayoutResult = 0;
            if (this.mFirst || windowShouldResize || insetsChanged || viewVisibilityChanged || params != null) {
                if (viewVisibility == 0) {
                    insetsPending = computesInternalInsets && (this.mFirst || viewVisibilityChanged);
                }
                if (this.mSurfaceHolder != null) {
                    this.mSurfaceHolder.mSurfaceLock.lock();
                    this.mDrawingAllowed = true;
                }
                boolean hwInitialized = false;
                boolean contentInsetsChanged = false;
                boolean hadSurface = this.mSurface.isValid();
                try {
                    if (this.mAttachInfo.mHardwareRenderer != null && this.mAttachInfo.mHardwareRenderer.pauseSurface(this.mSurface)) {
                        this.mDirty.set(0, 0, this.mWidth, this.mHeight);
                    }
                    int surfaceGenerationId = this.mSurface.getGenerationId();
                    relayoutResult = relayoutWindow(params, viewVisibility, insetsPending);
                    if (!this.mDrawDuringWindowsAnimating && (relayoutResult & 8) != 0) {
                        this.mWindowsAnimating = true;
                    }
                    if (this.mPendingConfiguration.seq != 0) {
                        updateConfiguration(this.mPendingConfiguration, !this.mFirst);
                        this.mPendingConfiguration.seq = 0;
                    }
                    boolean overscanInsetsChanged = !this.mPendingOverscanInsets.equals(this.mAttachInfo.mOverscanInsets);
                    contentInsetsChanged = !this.mPendingContentInsets.equals(this.mAttachInfo.mContentInsets);
                    boolean visibleInsetsChanged = !this.mPendingVisibleInsets.equals(this.mAttachInfo.mVisibleInsets);
                    boolean stableInsetsChanged = !this.mPendingStableInsets.equals(this.mAttachInfo.mStableInsets);
                    if (contentInsetsChanged) {
                        if (this.mWidth > 0 && this.mHeight > 0 && lp != null && ((lp.systemUiVisibility | lp.subtreeSystemUiVisibility) & 1536) == 0 && this.mSurface != null && this.mSurface.isValid() && !this.mAttachInfo.mTurnOffWindowResizeAnim && this.mAttachInfo.mHardwareRenderer != null && this.mAttachInfo.mHardwareRenderer.isEnabled() && lp != null && !PixelFormat.formatHasAlpha(lp.format) && !this.mBlockResizeBuffer) {
                            disposeResizeBuffer();
                        }
                        this.mAttachInfo.mContentInsets.set(this.mPendingContentInsets);
                    }
                    if (overscanInsetsChanged) {
                        this.mAttachInfo.mOverscanInsets.set(this.mPendingOverscanInsets);
                        contentInsetsChanged = true;
                    }
                    if (stableInsetsChanged) {
                        this.mAttachInfo.mStableInsets.set(this.mPendingStableInsets);
                        contentInsetsChanged = true;
                    }
                    if (contentInsetsChanged || this.mLastSystemUiVisibility != this.mAttachInfo.mSystemUiVisibility || this.mApplyInsetsRequested || this.mLastOverscanRequested != this.mAttachInfo.mOverscanRequested) {
                        this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                        this.mLastOverscanRequested = this.mAttachInfo.mOverscanRequested;
                        this.mApplyInsetsRequested = false;
                        dispatchApplyInsets(host);
                    }
                    if (visibleInsetsChanged) {
                        this.mAttachInfo.mVisibleInsets.set(this.mPendingVisibleInsets);
                    }
                    if (!hadSurface) {
                        if (this.mSurface.isValid()) {
                            newSurface = true;
                            this.mFullRedrawNeeded = true;
                            this.mPreviousTransparentRegion.setEmpty();
                            if (this.mAttachInfo.mHardwareRenderer != null) {
                                try {
                                    hwInitialized = this.mAttachInfo.mHardwareRenderer.initialize(this.mSurface);
                                } catch (Surface.OutOfResourcesException e) {
                                    handleOutOfResourcesException(e);
                                    return;
                                }
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
                        disposeResizeBuffer();
                        if (this.mAttachInfo.mHardwareRenderer != null && this.mAttachInfo.mHardwareRenderer.isEnabled()) {
                            this.mAttachInfo.mHardwareRenderer.destroyHardwareResources(this.mView);
                            this.mAttachInfo.mHardwareRenderer.destroy();
                        }
                    } else if (surfaceGenerationId != this.mSurface.getGenerationId() && this.mSurfaceHolder == null && this.mAttachInfo.mHardwareRenderer != null) {
                        this.mFullRedrawNeeded = true;
                        try {
                            this.mAttachInfo.mHardwareRenderer.updateSurface(this.mSurface);
                        } catch (Surface.OutOfResourcesException e2) {
                            handleOutOfResourcesException(e2);
                            return;
                        }
                    }
                } catch (RemoteException e3) {
                }
                this.mAttachInfo.mWindowLeft = frame.left;
                this.mAttachInfo.mWindowTop = frame.top;
                if (this.mWidth != frame.width() || this.mHeight != frame.height()) {
                    this.mWidth = frame.width();
                    this.mHeight = frame.height();
                }
                if (this.mSurfaceHolder != null) {
                    if (this.mSurface.isValid()) {
                        this.mSurfaceHolder.mSurface = this.mSurface;
                    }
                    this.mSurfaceHolder.setSurfaceFrameSize(this.mWidth, this.mHeight);
                    this.mSurfaceHolder.mSurfaceLock.unlock();
                    if (this.mSurface.isValid()) {
                        if (!hadSurface) {
                            this.mSurfaceHolder.ungetCallbacks();
                            this.mIsCreating = true;
                            this.mSurfaceHolderCallback.surfaceCreated(this.mSurfaceHolder);
                            SurfaceHolder.Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (SurfaceHolder.Callback c : callbacks) {
                                    c.surfaceCreated(this.mSurfaceHolder);
                                }
                            }
                            surfaceChanged = true;
                        }
                        if (surfaceChanged) {
                            this.mSurfaceHolderCallback.surfaceChanged(this.mSurfaceHolder, lp.format, this.mWidth, this.mHeight);
                            SurfaceHolder.Callback[] callbacks2 = this.mSurfaceHolder.getCallbacks();
                            if (callbacks2 != null) {
                                for (SurfaceHolder.Callback c2 : callbacks2) {
                                    c2.surfaceChanged(this.mSurfaceHolder, lp.format, this.mWidth, this.mHeight);
                                }
                            }
                        }
                        this.mIsCreating = false;
                    } else if (hadSurface) {
                        this.mSurfaceHolder.ungetCallbacks();
                        SurfaceHolder.Callback[] callbacks3 = this.mSurfaceHolder.getCallbacks();
                        this.mSurfaceHolderCallback.surfaceDestroyed(this.mSurfaceHolder);
                        if (callbacks3 != null) {
                            for (SurfaceHolder.Callback c3 : callbacks3) {
                                c3.surfaceDestroyed(this.mSurfaceHolder);
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
                if (this.mAttachInfo.mHardwareRenderer != null && this.mAttachInfo.mHardwareRenderer.isEnabled() && (hwInitialized || this.mWidth != this.mAttachInfo.mHardwareRenderer.getWidth() || this.mHeight != this.mAttachInfo.mHardwareRenderer.getHeight())) {
                    this.mAttachInfo.mHardwareRenderer.setup(this.mWidth, this.mHeight, this.mWindowAttributes.surfaceInsets);
                    if (!hwInitialized) {
                        this.mAttachInfo.mHardwareRenderer.invalidate(this.mSurface);
                        this.mFullRedrawNeeded = true;
                    }
                }
                if (!this.mStopped) {
                    boolean focusChangedDueToTouchMode = ensureTouchModeLocally((relayoutResult & 1) != 0);
                    if (focusChangedDueToTouchMode || this.mWidth != host.getMeasuredWidth() || this.mHeight != host.getMeasuredHeight() || contentInsetsChanged) {
                        int childWidthMeasureSpec = getRootMeasureSpec(this.mWidth, lp.width);
                        int childHeightMeasureSpec = getRootMeasureSpec(this.mHeight, lp.height);
                        performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                        int width = host.getMeasuredWidth();
                        int height = host.getMeasuredHeight();
                        boolean measureAgain = false;
                        if (lp.horizontalWeight > 0.0f) {
                            childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width + ((int) ((this.mWidth - width) * lp.horizontalWeight)), 1073741824);
                            measureAgain = true;
                        }
                        if (lp.verticalWeight > 0.0f) {
                            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height + ((int) ((this.mHeight - height) * lp.verticalWeight)), 1073741824);
                            measureAgain = true;
                        }
                        if (measureAgain) {
                            performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                        }
                        layoutRequested = true;
                    }
                }
            } else {
                boolean windowMoved = (this.mAttachInfo.mWindowLeft == frame.left && this.mAttachInfo.mWindowTop == frame.top) ? false : true;
                if (windowMoved) {
                    if (this.mTranslator != null) {
                        this.mTranslator.translateRectInScreenToAppWinFrame(frame);
                    }
                    this.mAttachInfo.mWindowLeft = frame.left;
                    this.mAttachInfo.mWindowTop = frame.top;
                }
            }
            boolean didLayout = layoutRequested && !this.mStopped;
            boolean triggerGlobalLayoutListener = didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
            if (didLayout) {
                performLayout(lp, desiredWindowWidth, desiredWindowHeight);
                if ((host.mPrivateFlags & 512) != 0) {
                    host.getLocationInWindow(this.mTmpLocation);
                    this.mTransparentRegion.set(this.mTmpLocation[0], this.mTmpLocation[1], (this.mTmpLocation[0] + host.mRight) - host.mLeft, (this.mTmpLocation[1] + host.mBottom) - host.mTop);
                    host.gatherTransparentRegion(this.mTransparentRegion);
                    if (this.mTranslator != null) {
                        this.mTranslator.translateRegionInWindowToScreen(this.mTransparentRegion);
                    }
                    if (!this.mTransparentRegion.equals(this.mPreviousTransparentRegion)) {
                        this.mPreviousTransparentRegion.set(this.mTransparentRegion);
                        this.mFullRedrawNeeded = true;
                        try {
                            this.mWindowSession.setTransparentRegion(this.mWindow, this.mTransparentRegion);
                        } catch (RemoteException e4) {
                        }
                    }
                }
            }
            if (triggerGlobalLayoutListener) {
                this.mAttachInfo.mRecomputeGlobalAttributes = false;
                this.mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
            }
            if (computesInternalInsets) {
                ViewTreeObserver.InternalInsetsInfo insets = this.mAttachInfo.mGivenInternalInsets;
                insets.reset();
                this.mAttachInfo.mTreeObserver.dispatchOnComputeInternalInsets(insets);
                this.mAttachInfo.mHasNonEmptyGivenInternalInsets = !insets.isEmpty();
                if (insetsPending || !this.mLastGivenInsets.equals(insets)) {
                    this.mLastGivenInsets.set(insets);
                    if (this.mTranslator != null) {
                        contentInsets = this.mTranslator.getTranslatedContentInsets(insets.contentInsets);
                        visibleInsets = this.mTranslator.getTranslatedVisibleInsets(insets.visibleInsets);
                        touchableRegion = this.mTranslator.getTranslatedTouchableArea(insets.touchableRegion);
                    } else {
                        contentInsets = insets.contentInsets;
                        visibleInsets = insets.visibleInsets;
                        touchableRegion = insets.touchableRegion;
                    }
                    try {
                        this.mWindowSession.setInsets(this.mWindow, insets.mTouchableInsets, contentInsets, visibleInsets, touchableRegion);
                    } catch (RemoteException e5) {
                    }
                }
            }
            boolean skipDraw = false;
            if (this.mFirst) {
                if (this.mView != null && !this.mView.hasFocus()) {
                    this.mView.requestFocus(2);
                }
                if ((relayoutResult & 8) != 0) {
                    this.mWindowsAnimating = true;
                }
            } else if (this.mWindowsAnimating) {
                skipDraw = true;
            }
            this.mFirst = false;
            this.mWillDrawSoon = false;
            this.mNewSurfaceNeeded = false;
            this.mViewVisibility = viewVisibility;
            if (this.mAttachInfo.mHasWindowFocus && !isInLocalFocusMode()) {
                boolean imTarget = WindowManager.LayoutParams.mayUseInputMethod(this.mWindowAttributes.flags);
                if (imTarget != this.mLastWasImTarget) {
                    this.mLastWasImTarget = imTarget;
                    InputMethodManager imm = InputMethodManager.peekInstance();
                    if (imm != null && imTarget) {
                        imm.startGettingWindowFocus(this.mView);
                        imm.onWindowFocus(this.mView, this.mView.findFocus(), this.mWindowAttributes.softInputMode, !this.mHasHadWindowFocus, this.mWindowAttributes.flags);
                    }
                }
            }
            if ((relayoutResult & 2) != 0) {
                this.mReportNextDraw = true;
            }
            boolean cancelDraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw() || viewVisibility != 0;
            if (!cancelDraw && !newSurface) {
                if (!skipDraw || this.mReportNextDraw) {
                    if (this.mPendingTransitions != null && this.mPendingTransitions.size() > 0) {
                        for (int i2 = 0; i2 < this.mPendingTransitions.size(); i2++) {
                            this.mPendingTransitions.get(i2).startChangingAnimations();
                        }
                        this.mPendingTransitions.clear();
                    }
                    performDraw();
                }
            } else if (viewVisibility == 0) {
                scheduleTraversals();
            } else if (this.mPendingTransitions != null && this.mPendingTransitions.size() > 0) {
                for (int i3 = 0; i3 < this.mPendingTransitions.size(); i3++) {
                    this.mPendingTransitions.get(i3).endChangingAnimations();
                }
                this.mPendingTransitions.clear();
            }
            this.mIsInTraversal = false;
        }
    }

    private void handleOutOfResourcesException(Surface.OutOfResourcesException e) {
        Log.e(TAG, "OutOfResourcesException initializing HW surface", e);
        try {
            if (!this.mWindowSession.outOfMemory(this.mWindow) && Process.myUid() != 1000) {
                Slog.w(TAG, "No processes killed for memory; killing self");
                Process.killProcess(Process.myPid());
            }
        } catch (RemoteException e2) {
        }
        this.mLayoutRequested = true;
    }

    private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
        Trace.traceBegin(8L, "measure");
        try {
            this.mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
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

    private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth, int desiredWindowHeight) {
        ArrayList<View> validLayoutRequesters;
        this.mLayoutRequested = false;
        this.mScrollMayChange = true;
        this.mInLayout = true;
        View host = this.mView;
        Trace.traceBegin(8L, TtmlUtils.TAG_LAYOUT);
        try {
            host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
            this.mInLayout = false;
            int numViewsRequestingLayout = this.mLayoutRequesters.size();
            if (numViewsRequestingLayout > 0 && (validLayoutRequesters = getValidLayoutRequesters(this.mLayoutRequesters, false)) != null) {
                this.mHandlingLayoutInLayoutRequest = true;
                int numValidRequests = validLayoutRequesters.size();
                for (int i = 0; i < numValidRequests; i++) {
                    View view = validLayoutRequesters.get(i);
                    Log.w("View", "requestLayout() improperly called by " + view + " during layout: running second layout pass");
                    view.requestLayout();
                }
                measureHierarchy(host, lp, this.mView.getContext().getResources(), desiredWindowWidth, desiredWindowHeight);
                this.mInLayout = true;
                host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
                this.mHandlingLayoutInLayoutRequest = false;
                final ArrayList<View> validLayoutRequesters2 = getValidLayoutRequesters(this.mLayoutRequesters, true);
                if (validLayoutRequesters2 != null) {
                    getRunQueue().post(new Runnable() {
                        @Override
                        public void run() {
                            int numValidRequests2 = validLayoutRequesters2.size();
                            for (int i2 = 0; i2 < numValidRequests2; i2++) {
                                View view2 = (View) validLayoutRequesters2.get(i2);
                                Log.w("View", "requestLayout() improperly called by " + view2 + " during second layout pass: posting in next frame");
                                view2.requestLayout();
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

    private ArrayList<View> getValidLayoutRequesters(ArrayList<View> layoutRequesters, boolean secondLayoutRequests) {
        int numViewsRequestingLayout = layoutRequesters.size();
        ArrayList<View> validLayoutRequesters = null;
        for (int i = 0; i < numViewsRequestingLayout; i++) {
            View view = layoutRequesters.get(i);
            if (view != null && view.mAttachInfo != null && view.mParent != null && (secondLayoutRequests || (view.mPrivateFlags & 4096) == 4096)) {
                boolean gone = false;
                View parent = view;
                while (true) {
                    if (parent == null) {
                        break;
                    }
                    if ((parent.mViewFlags & 12) == 8) {
                        gone = true;
                        break;
                    }
                    if (parent.mParent instanceof View) {
                        parent = (View) parent.mParent;
                    } else {
                        parent = null;
                    }
                }
                if (!gone) {
                    if (validLayoutRequesters == null) {
                        validLayoutRequesters = new ArrayList<>();
                    }
                    validLayoutRequesters.add(view);
                }
            }
        }
        if (!secondLayoutRequests) {
            for (int i2 = 0; i2 < numViewsRequestingLayout; i2++) {
                View view2 = layoutRequesters.get(i2);
                while (view2 != null && (view2.mPrivateFlags & 4096) != 0) {
                    view2.mPrivateFlags &= -4097;
                    if (view2.mParent instanceof View) {
                        view2 = (View) view2.mParent;
                    } else {
                        view2 = null;
                    }
                }
            }
        }
        layoutRequesters.clear();
        return validLayoutRequesters;
    }

    @Override
    public void requestTransparentRegion(View child) {
        checkThread();
        if (this.mView == child) {
            this.mView.mPrivateFlags |= 512;
            this.mWindowAttributesChanged = true;
            this.mWindowAttributesChangesFlag = 0;
            requestLayout();
        }
    }

    private static int getRootMeasureSpec(int windowSize, int rootDimension) {
        switch (rootDimension) {
            case -2:
                int measureSpec = View.MeasureSpec.makeMeasureSpec(windowSize, Integer.MIN_VALUE);
                return measureSpec;
            case -1:
                int measureSpec2 = View.MeasureSpec.makeMeasureSpec(windowSize, 1073741824);
                return measureSpec2;
            default:
                int measureSpec3 = View.MeasureSpec.makeMeasureSpec(rootDimension, 1073741824);
                return measureSpec3;
        }
    }

    @Override
    public void onHardwarePreDraw(HardwareCanvas canvas) {
        canvas.translate(-this.mHardwareXOffset, -this.mHardwareYOffset);
    }

    @Override
    public void onHardwarePostDraw(HardwareCanvas canvas) {
        if (this.mResizeBuffer != null) {
            this.mResizePaint.setAlpha(this.mResizeAlpha);
            canvas.drawHardwareLayer(this.mResizeBuffer, this.mHardwareXOffset, this.mHardwareYOffset, this.mResizePaint);
        }
        drawAccessibilityFocusedDrawableIfNeeded(canvas);
    }

    void outputDisplayList(View view) {
        RenderNode renderNode = view.getDisplayList();
        if (renderNode != null) {
            renderNode.output();
        }
    }

    private void profileRendering(boolean enabled) {
        if (this.mProfileRendering) {
            this.mRenderProfilingEnabled = enabled;
            if (this.mRenderProfiler != null) {
                this.mChoreographer.removeFrameCallback(this.mRenderProfiler);
            }
            if (this.mRenderProfilingEnabled) {
                if (this.mRenderProfiler == null) {
                    this.mRenderProfiler = new Choreographer.FrameCallback() {
                        @Override
                        public void doFrame(long frameTimeNanos) {
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
        long nowTime = System.currentTimeMillis();
        if (this.mFpsStartTime < 0) {
            this.mFpsPrevTime = nowTime;
            this.mFpsStartTime = nowTime;
            this.mFpsNumFrames = 0;
            return;
        }
        this.mFpsNumFrames++;
        String thisHash = Integer.toHexString(System.identityHashCode(this));
        long frameTime = nowTime - this.mFpsPrevTime;
        long totalTime = nowTime - this.mFpsStartTime;
        Log.v(TAG, "0x" + thisHash + "\tFrame time:\t" + frameTime);
        this.mFpsPrevTime = nowTime;
        if (totalTime > 1000) {
            float fps = (this.mFpsNumFrames * 1000.0f) / totalTime;
            Log.v(TAG, "0x" + thisHash + "\tFPS:\t" + fps);
            this.mFpsStartTime = nowTime;
            this.mFpsNumFrames = 0;
        }
    }

    private void performDraw() {
        if (this.mAttachInfo.mDisplayState != 1 || this.mReportNextDraw) {
            boolean fullRedrawNeeded = this.mFullRedrawNeeded;
            this.mFullRedrawNeeded = false;
            this.mIsDrawing = true;
            Trace.traceBegin(8L, "draw");
            try {
                draw(fullRedrawNeeded);
                this.mIsDrawing = false;
                Trace.traceEnd(8L);
                if (this.mAttachInfo.mPendingAnimatingRenderNodes != null) {
                    int count = this.mAttachInfo.mPendingAnimatingRenderNodes.size();
                    for (int i = 0; i < count; i++) {
                        this.mAttachInfo.mPendingAnimatingRenderNodes.get(i).endAllAnimators();
                    }
                    this.mAttachInfo.mPendingAnimatingRenderNodes.clear();
                }
                if (this.mReportNextDraw) {
                    this.mReportNextDraw = false;
                    if (this.mAttachInfo.mHardwareRenderer != null) {
                        this.mAttachInfo.mHardwareRenderer.fence();
                    }
                    if (this.mSurfaceHolder != null && this.mSurface.isValid()) {
                        this.mSurfaceHolderCallback.surfaceRedrawNeeded(this.mSurfaceHolder);
                        SurfaceHolder.Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
                        if (callbacks != null) {
                            for (SurfaceHolder.Callback c : callbacks) {
                                if (c instanceof SurfaceHolder.Callback2) {
                                    ((SurfaceHolder.Callback2) c).surfaceRedrawNeeded(this.mSurfaceHolder);
                                }
                            }
                        }
                    }
                    try {
                        this.mWindowSession.finishDrawing(this.mWindow);
                    } catch (RemoteException e) {
                    }
                }
            } catch (Throwable th) {
                this.mIsDrawing = false;
                Trace.traceEnd(8L);
                throw th;
            }
        }
    }

    private void draw(boolean fullRedrawNeeded) {
        int curScrollY;
        Surface surface = this.mSurface;
        if (surface.isValid()) {
            if (!sFirstDrawComplete) {
                synchronized (sFirstDrawHandlers) {
                    sFirstDrawComplete = true;
                    int count = sFirstDrawHandlers.size();
                    for (int i = 0; i < count; i++) {
                        this.mHandler.post(sFirstDrawHandlers.get(i));
                    }
                }
            }
            scrollToRectOrFocus(null, false);
            if (this.mAttachInfo.mViewScrollChanged) {
                this.mAttachInfo.mViewScrollChanged = false;
                this.mAttachInfo.mTreeObserver.dispatchOnScrollChanged();
            }
            boolean animating = this.mScroller != null && this.mScroller.computeScrollOffset();
            if (animating) {
                curScrollY = this.mScroller.getCurrY();
            } else {
                curScrollY = this.mScrollY;
            }
            if (this.mCurScrollY != curScrollY) {
                this.mCurScrollY = curScrollY;
                fullRedrawNeeded = true;
                if (this.mView instanceof RootViewSurfaceTaker) {
                    ((RootViewSurfaceTaker) this.mView).onRootViewScrollYChanged(this.mCurScrollY);
                }
            }
            float appScale = this.mAttachInfo.mApplicationScale;
            boolean scalingRequired = this.mAttachInfo.mScalingRequired;
            int resizeAlpha = 0;
            if (this.mResizeBuffer != null) {
                long deltaTime = SystemClock.uptimeMillis() - this.mResizeBufferStartTime;
                if (deltaTime < this.mResizeBufferDuration) {
                    float amt = deltaTime / this.mResizeBufferDuration;
                    animating = true;
                    resizeAlpha = 255 - ((int) (255.0f * mResizeInterpolator.getInterpolation(amt)));
                } else {
                    disposeResizeBuffer();
                }
            }
            Rect dirty = this.mDirty;
            if (this.mSurfaceHolder != null) {
                dirty.setEmpty();
                if (animating) {
                    if (this.mScroller != null) {
                        this.mScroller.abortAnimation();
                    }
                    disposeResizeBuffer();
                    return;
                }
                return;
            }
            if (fullRedrawNeeded) {
                this.mAttachInfo.mIgnoreDirtyState = true;
                dirty.set(0, 0, (int) ((this.mWidth * appScale) + 0.5f), (int) ((this.mHeight * appScale) + 0.5f));
            }
            this.mAttachInfo.mTreeObserver.dispatchOnDraw();
            int xOffset = 0;
            int yOffset = curScrollY;
            WindowManager.LayoutParams params = this.mWindowAttributes;
            Rect surfaceInsets = params != null ? params.surfaceInsets : null;
            if (surfaceInsets != null) {
                xOffset = 0 - surfaceInsets.left;
                yOffset -= surfaceInsets.top;
                dirty.offset(surfaceInsets.left, surfaceInsets.right);
            }
            boolean accessibilityFocusDirty = false;
            Drawable drawable = this.mAttachInfo.mAccessibilityFocusDrawable;
            if (drawable != null) {
                Rect bounds = this.mAttachInfo.mTmpInvalRect;
                boolean hasFocus = getAccessibilityFocusedRect(bounds);
                if (!hasFocus) {
                    bounds.setEmpty();
                }
                if (!bounds.equals(drawable.getBounds())) {
                    accessibilityFocusDirty = true;
                }
            }
            if (!dirty.isEmpty() || this.mIsAnimating || accessibilityFocusDirty) {
                if (this.mAttachInfo.mHardwareRenderer != null && this.mAttachInfo.mHardwareRenderer.isEnabled()) {
                    boolean invalidateRoot = accessibilityFocusDirty;
                    this.mIsAnimating = false;
                    if (this.mHardwareYOffset != yOffset || this.mHardwareXOffset != xOffset) {
                        this.mHardwareYOffset = yOffset;
                        this.mHardwareXOffset = xOffset;
                        invalidateRoot = true;
                    }
                    this.mResizeAlpha = resizeAlpha;
                    if (invalidateRoot) {
                        this.mAttachInfo.mHardwareRenderer.invalidateRoot();
                    }
                    dirty.setEmpty();
                    this.mBlockResizeBuffer = false;
                    this.mAttachInfo.mHardwareRenderer.draw(this.mView, this.mAttachInfo, this);
                } else {
                    if (this.mAttachInfo.mHardwareRenderer != null && !this.mAttachInfo.mHardwareRenderer.isEnabled() && this.mAttachInfo.mHardwareRenderer.isRequested()) {
                        try {
                            this.mAttachInfo.mHardwareRenderer.initializeIfNeeded(this.mWidth, this.mHeight, this.mSurface, surfaceInsets);
                            this.mFullRedrawNeeded = true;
                            scheduleTraversals();
                            return;
                        } catch (Surface.OutOfResourcesException e) {
                            handleOutOfResourcesException(e);
                            return;
                        }
                    }
                    if (!drawSoftware(surface, this.mAttachInfo, xOffset, yOffset, scalingRequired, dirty)) {
                        return;
                    }
                }
            }
            if (animating) {
                this.mFullRedrawNeeded = true;
                scheduleTraversals();
            }
        }
    }

    private boolean drawSoftware(Surface surface, View.AttachInfo attachInfo, int xoff, int yoff, boolean scalingRequired, Rect dirty) {
        try {
            int left = dirty.left;
            int top = dirty.top;
            int right = dirty.right;
            int bottom = dirty.bottom;
            Canvas canvas = this.mSurface.lockCanvas(dirty);
            if (left != dirty.left || top != dirty.top || right != dirty.right || bottom != dirty.bottom) {
                attachInfo.mIgnoreDirtyState = true;
            }
            canvas.setDensity(this.mDensity);
            try {
                if (!canvas.isOpaque() || yoff != 0 || xoff != 0) {
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                }
                dirty.setEmpty();
                this.mIsAnimating = false;
                attachInfo.mDrawingTime = SystemClock.uptimeMillis();
                this.mView.mPrivateFlags |= 32;
                try {
                    canvas.translate(-xoff, -yoff);
                    if (this.mTranslator != null) {
                        this.mTranslator.translateCanvas(canvas);
                    }
                    canvas.setScreenDensity(scalingRequired ? this.mNoncompatDensity : 0);
                    attachInfo.mSetIgnoreDirtyState = false;
                    this.mView.draw(canvas);
                    drawAccessibilityFocusedDrawableIfNeeded(canvas);
                    try {
                        surface.unlockCanvasAndPost(canvas);
                        return true;
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Could not unlock surface", e);
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
                    surface.unlockCanvasAndPost(canvas);
                    throw th;
                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "Could not unlock surface", e2);
                    this.mLayoutRequested = true;
                    return false;
                }
            }
        } catch (Surface.OutOfResourcesException e3) {
            handleOutOfResourcesException(e3);
            return false;
        } catch (IllegalArgumentException e4) {
            Log.e(TAG, "Could not lock surface", e4);
            this.mLayoutRequested = true;
            return false;
        }
    }

    private void drawAccessibilityFocusedDrawableIfNeeded(Canvas canvas) {
        Rect bounds = this.mAttachInfo.mTmpInvalRect;
        if (getAccessibilityFocusedRect(bounds)) {
            Drawable drawable = getAccessibilityFocusedDrawable();
            if (drawable != null) {
                drawable.setBounds(bounds);
                drawable.draw(canvas);
                return;
            }
            return;
        }
        if (this.mAttachInfo.mAccessibilityFocusDrawable != null) {
            this.mAttachInfo.mAccessibilityFocusDrawable.setBounds(0, 0, 0, 0);
        }
    }

    private boolean getAccessibilityFocusedRect(Rect bounds) {
        View host;
        AccessibilityManager manager = AccessibilityManager.getInstance(this.mView.mContext);
        if (!manager.isEnabled() || !manager.isTouchExplorationEnabled() || (host = this.mAccessibilityFocusedHost) == null || host.mAttachInfo == null) {
            return false;
        }
        AccessibilityNodeProvider provider = host.getAccessibilityNodeProvider();
        if (provider == null) {
            host.getBoundsOnScreen(bounds, true);
        } else {
            if (this.mAccessibilityFocusedVirtualView == null) {
                return false;
            }
            this.mAccessibilityFocusedVirtualView.getBoundsInScreen(bounds);
        }
        View.AttachInfo attachInfo = this.mAttachInfo;
        bounds.offset(-attachInfo.mWindowLeft, -attachInfo.mWindowTop);
        bounds.intersect(0, 0, attachInfo.mViewRootImpl.mWidth, attachInfo.mViewRootImpl.mHeight);
        return bounds.isEmpty() ? false : true;
    }

    private Drawable getAccessibilityFocusedDrawable() {
        if (this.mAttachInfo.mAccessibilityFocusDrawable == null) {
            TypedValue value = new TypedValue();
            boolean resolved = this.mView.mContext.getTheme().resolveAttribute(R.attr.accessibilityFocusedDrawable, value, true);
            if (resolved) {
                this.mAttachInfo.mAccessibilityFocusDrawable = this.mView.mContext.getDrawable(value.resourceId);
            }
        }
        return this.mAttachInfo.mAccessibilityFocusDrawable;
    }

    public void setDrawDuringWindowsAnimating(boolean value) {
        this.mDrawDuringWindowsAnimating = value;
        if (value) {
            handleDispatchDoneAnimating();
        }
    }

    boolean scrollToRectOrFocus(Rect rectangle, boolean immediate) {
        Rect ci = this.mAttachInfo.mContentInsets;
        Rect vi = this.mAttachInfo.mVisibleInsets;
        int scrollY = 0;
        boolean handled = false;
        if (vi.left > ci.left || vi.top > ci.top || vi.right > ci.right || vi.bottom > ci.bottom) {
            scrollY = this.mScrollY;
            View focus = this.mView.findFocus();
            if (focus == null) {
                return false;
            }
            View lastScrolledFocus = this.mLastScrolledFocus != null ? this.mLastScrolledFocus.get() : null;
            if (focus != lastScrolledFocus) {
                rectangle = null;
            }
            if (focus != lastScrolledFocus || this.mScrollMayChange || rectangle != null) {
                this.mLastScrolledFocus = new WeakReference<>(focus);
                this.mScrollMayChange = false;
                if (focus.getGlobalVisibleRect(this.mVisRect, null)) {
                    if (rectangle == null) {
                        focus.getFocusedRect(this.mTempRect);
                        if (this.mView instanceof ViewGroup) {
                            ((ViewGroup) this.mView).offsetDescendantRectToMyCoords(focus, this.mTempRect);
                        }
                    } else {
                        this.mTempRect.set(rectangle);
                    }
                    if (this.mTempRect.intersect(this.mVisRect)) {
                        if (this.mTempRect.height() <= (this.mView.getHeight() - vi.top) - vi.bottom) {
                            if (this.mTempRect.top - scrollY < vi.top) {
                                scrollY -= vi.top - (this.mTempRect.top - scrollY);
                            } else if (this.mTempRect.bottom - scrollY > this.mView.getHeight() - vi.bottom) {
                                scrollY += (this.mTempRect.bottom - scrollY) - (this.mView.getHeight() - vi.bottom);
                            }
                        }
                        handled = true;
                    }
                }
            }
        }
        if (scrollY != this.mScrollY) {
            if (!immediate && this.mResizeBuffer == null) {
                if (this.mScroller == null) {
                    this.mScroller = new Scroller(this.mView.getContext());
                }
                this.mScroller.startScroll(0, this.mScrollY, 0, scrollY - this.mScrollY);
            } else if (this.mScroller != null) {
                this.mScroller.abortAnimation();
            }
            this.mScrollY = scrollY;
        }
        return handled;
    }

    public View getAccessibilityFocusedHost() {
        return this.mAccessibilityFocusedHost;
    }

    public AccessibilityNodeInfo getAccessibilityFocusedVirtualView() {
        return this.mAccessibilityFocusedVirtualView;
    }

    void setAccessibilityFocus(View view, AccessibilityNodeInfo node) {
        if (this.mAccessibilityFocusedVirtualView != null) {
            AccessibilityNodeInfo focusNode = this.mAccessibilityFocusedVirtualView;
            View focusHost = this.mAccessibilityFocusedHost;
            this.mAccessibilityFocusedHost = null;
            this.mAccessibilityFocusedVirtualView = null;
            focusHost.clearAccessibilityFocusNoCallbacks();
            AccessibilityNodeProvider provider = focusHost.getAccessibilityNodeProvider();
            if (provider != null) {
                focusNode.getBoundsInParent(this.mTempRect);
                focusHost.invalidate(this.mTempRect);
                int virtualNodeId = AccessibilityNodeInfo.getVirtualDescendantId(focusNode.getSourceNodeId());
                provider.performAction(virtualNodeId, 128, null);
            }
            focusNode.recycle();
        }
        if (this.mAccessibilityFocusedHost != null) {
            this.mAccessibilityFocusedHost.clearAccessibilityFocusNoCallbacks();
        }
        this.mAccessibilityFocusedHost = view;
        this.mAccessibilityFocusedVirtualView = node;
        if (this.mAttachInfo.mHardwareRenderer != null) {
            this.mAttachInfo.mHardwareRenderer.invalidateRoot();
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        checkThread();
        scheduleTraversals();
    }

    @Override
    public void clearChildFocus(View child) {
        checkThread();
        scheduleTraversals();
    }

    @Override
    public ViewParent getParentForAccessibility() {
        return null;
    }

    @Override
    public void focusableViewAvailable(View v) {
        checkThread();
        if (this.mView != null) {
            if (!this.mView.hasFocus()) {
                v.requestFocus();
                return;
            }
            View focused = this.mView.findFocus();
            if (focused instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) focused;
                if (group.getDescendantFocusability() == 262144 && isViewDescendantOf(v, focused)) {
                    v.requestFocus();
                }
            }
        }
    }

    @Override
    public void recomputeViewAttributes(View child) {
        checkThread();
        if (this.mView == child) {
            this.mAttachInfo.mRecomputeGlobalAttributes = true;
            if (!this.mWillDrawSoon) {
                scheduleTraversals();
            }
        }
    }

    void dispatchDetachedFromWindow() {
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
        }
        if (this.mInputChannel != null) {
            this.mInputChannel.dispose();
            this.mInputChannel = null;
        }
        this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
        unscheduleTraversals();
    }

    void updateConfiguration(Configuration config, boolean force) {
        CompatibilityInfo ci = this.mDisplayAdjustments.getCompatibilityInfo();
        if (!ci.equals(CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO)) {
            Configuration config2 = new Configuration(config);
            ci.applyToConfiguration(this.mNoncompatDensity, config2);
            config = config2;
        }
        synchronized (sConfigCallbacks) {
            for (int i = sConfigCallbacks.size() - 1; i >= 0; i--) {
                sConfigCallbacks.get(i).onConfigurationChanged(config);
            }
        }
        if (this.mView != null) {
            Configuration config3 = this.mView.getResources().getConfiguration();
            if (force || this.mLastConfiguration.diff(config3) != 0) {
                int lastLayoutDirection = this.mLastConfiguration.getLayoutDirection();
                int currentLayoutDirection = config3.getLayoutDirection();
                this.mLastConfiguration.setTo(config3);
                if (lastLayoutDirection != currentLayoutDirection && this.mViewLayoutDirectionInitial == 2) {
                    this.mView.setLayoutDirection(currentLayoutDirection);
                }
                this.mView.dispatchConfigurationChanged(config3);
            }
        }
    }

    public static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }
        Object parent2 = child.getParent();
        return (parent2 instanceof ViewGroup) && isViewDescendantOf((View) parent2, parent);
    }

    private static void forceLayout(View view) {
        view.forceLayout();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                forceLayout(group.getChildAt(i));
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
                case 23:
                default:
                    return super.getMessageName(message);
                case 11:
                    return "MSG_DISPATCH_KEY_FROM_IME";
                case 12:
                    return "MSG_FINISH_INPUT_CONNECTION";
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
                case 22:
                    return "MSG_DISPATCH_DONE_ANIMATING";
                case 24:
                    return "MSG_WINDOW_MOVED";
                case 25:
                    return "MSG_SYNTHESIZE_INPUT_EVENT";
                case 26:
                    return "MSG_DISPATCH_WINDOW_SHOWN";
            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ((View) msg.obj).invalidate();
                    return;
                case 2:
                    View.AttachInfo.InvalidateInfo info = (View.AttachInfo.InvalidateInfo) msg.obj;
                    info.target.invalidate(info.left, info.top, info.right, info.bottom);
                    info.recycle();
                    return;
                case 3:
                    ViewRootImpl.this.doDie();
                    return;
                case 4:
                    SomeArgs args = (SomeArgs) msg.obj;
                    if (ViewRootImpl.this.mWinFrame.equals(args.arg1) && ViewRootImpl.this.mPendingOverscanInsets.equals(args.arg5) && ViewRootImpl.this.mPendingContentInsets.equals(args.arg2) && ViewRootImpl.this.mPendingStableInsets.equals(args.arg6) && ViewRootImpl.this.mPendingVisibleInsets.equals(args.arg3) && args.arg4 == null) {
                        return;
                    }
                    break;
                case 5:
                    break;
                case 6:
                    if (ViewRootImpl.this.mAdded) {
                        boolean hasWindowFocus = msg.arg1 != 0;
                        ViewRootImpl.this.mAttachInfo.mHasWindowFocus = hasWindowFocus;
                        ViewRootImpl.this.profileRendering(hasWindowFocus);
                        if (hasWindowFocus) {
                            boolean inTouchMode = msg.arg2 != 0;
                            ViewRootImpl.this.ensureTouchModeLocally(inTouchMode);
                            if (ViewRootImpl.this.mAttachInfo.mHardwareRenderer != null && ViewRootImpl.this.mSurface.isValid()) {
                                ViewRootImpl.this.mFullRedrawNeeded = true;
                                try {
                                    WindowManager.LayoutParams lp = ViewRootImpl.this.mWindowAttributes;
                                    Rect surfaceInsets = lp != null ? lp.surfaceInsets : null;
                                    ViewRootImpl.this.mAttachInfo.mHardwareRenderer.initializeIfNeeded(ViewRootImpl.this.mWidth, ViewRootImpl.this.mHeight, ViewRootImpl.this.mSurface, surfaceInsets);
                                } catch (Surface.OutOfResourcesException e) {
                                    Log.e(ViewRootImpl.TAG, "OutOfResourcesException locking surface", e);
                                    try {
                                        if (!ViewRootImpl.this.mWindowSession.outOfMemory(ViewRootImpl.this.mWindow)) {
                                            Slog.w(ViewRootImpl.TAG, "No processes killed for memory; killing self");
                                            Process.killProcess(Process.myPid());
                                        }
                                        break;
                                    } catch (RemoteException e2) {
                                    }
                                    sendMessageDelayed(obtainMessage(msg.what, msg.arg1, msg.arg2), 500L);
                                    return;
                                }
                            }
                            break;
                        }
                        ViewRootImpl.this.mLastWasImTarget = WindowManager.LayoutParams.mayUseInputMethod(ViewRootImpl.this.mWindowAttributes.flags);
                        InputMethodManager imm = InputMethodManager.peekInstance();
                        if (ViewRootImpl.this.mView != null) {
                            if (hasWindowFocus && imm != null && ViewRootImpl.this.mLastWasImTarget && !ViewRootImpl.this.isInLocalFocusMode()) {
                                imm.startGettingWindowFocus(ViewRootImpl.this.mView);
                            }
                            ViewRootImpl.this.mAttachInfo.mKeyDispatchState.reset();
                            ViewRootImpl.this.mView.dispatchWindowFocusChanged(hasWindowFocus);
                            ViewRootImpl.this.mAttachInfo.mTreeObserver.dispatchOnWindowFocusChange(hasWindowFocus);
                        }
                        if (hasWindowFocus) {
                            if (imm != null && ViewRootImpl.this.mLastWasImTarget && !ViewRootImpl.this.isInLocalFocusMode()) {
                                imm.onWindowFocus(ViewRootImpl.this.mView, ViewRootImpl.this.mView.findFocus(), ViewRootImpl.this.mWindowAttributes.softInputMode, !ViewRootImpl.this.mHasHadWindowFocus, ViewRootImpl.this.mWindowAttributes.flags);
                            }
                            ViewRootImpl.this.mWindowAttributes.softInputMode &= -257;
                            ((WindowManager.LayoutParams) ViewRootImpl.this.mView.getLayoutParams()).softInputMode &= -257;
                            ViewRootImpl.this.mHasHadWindowFocus = true;
                        }
                        if (ViewRootImpl.this.mView != null && ViewRootImpl.this.mAccessibilityManager.isEnabled() && hasWindowFocus) {
                            ViewRootImpl.this.mView.sendAccessibilityEvent(32);
                            return;
                        }
                        return;
                    }
                    return;
                case 7:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    InputEvent event = (InputEvent) args2.arg1;
                    InputEventReceiver receiver = (InputEventReceiver) args2.arg2;
                    ViewRootImpl.this.enqueueInputEvent(event, receiver, 0, true);
                    args2.recycle();
                    return;
                case 8:
                    ViewRootImpl.this.handleAppVisibility(msg.arg1 != 0);
                    return;
                case 9:
                    ViewRootImpl.this.handleGetNewSurface();
                    return;
                case 10:
                case 20:
                default:
                    return;
                case 11:
                    KeyEvent event2 = (KeyEvent) msg.obj;
                    if ((event2.getFlags() & 8) != 0) {
                        event2 = KeyEvent.changeFlags(event2, event2.getFlags() & (-9));
                    }
                    ViewRootImpl.this.enqueueInputEvent(event2, null, 1, true);
                    return;
                case 12:
                    InputMethodManager imm2 = InputMethodManager.peekInstance();
                    if (imm2 != null) {
                        imm2.reportFinishInputConnection((InputConnection) msg.obj);
                        return;
                    }
                    return;
                case 13:
                    InputMethodManager imm3 = InputMethodManager.peekInstance();
                    if (imm3 != null) {
                        imm3.checkFocus();
                        return;
                    }
                    return;
                case 14:
                    if (ViewRootImpl.this.mView != null) {
                        ViewRootImpl.this.mView.onCloseSystemDialogs((String) msg.obj);
                        return;
                    }
                    return;
                case 15:
                case 16:
                    DragEvent event3 = (DragEvent) msg.obj;
                    event3.mLocalState = ViewRootImpl.this.mLocalDragState;
                    ViewRootImpl.this.handleDragEvent(event3);
                    return;
                case 17:
                    ViewRootImpl.this.handleDispatchSystemUiVisibilityChanged((SystemUiVisibilityInfo) msg.obj);
                    return;
                case 18:
                    Configuration config = (Configuration) msg.obj;
                    if (config.isOtherSeqNewer(ViewRootImpl.this.mLastConfiguration)) {
                        config = ViewRootImpl.this.mLastConfiguration;
                    }
                    ViewRootImpl.this.updateConfiguration(config, false);
                    return;
                case 19:
                    ViewRootImpl.this.mProcessInputEventsScheduled = false;
                    ViewRootImpl.this.doProcessInputEvents();
                    return;
                case 21:
                    ViewRootImpl.this.setAccessibilityFocus(null, null);
                    return;
                case 22:
                    ViewRootImpl.this.handleDispatchDoneAnimating();
                    return;
                case 23:
                    if (ViewRootImpl.this.mView != null) {
                        ViewRootImpl.this.invalidateWorld(ViewRootImpl.this.mView);
                        return;
                    }
                    return;
                case 24:
                    if (ViewRootImpl.this.mAdded) {
                        int w = ViewRootImpl.this.mWinFrame.width();
                        int h = ViewRootImpl.this.mWinFrame.height();
                        int l = msg.arg1;
                        int t = msg.arg2;
                        ViewRootImpl.this.mWinFrame.left = l;
                        ViewRootImpl.this.mWinFrame.right = l + w;
                        ViewRootImpl.this.mWinFrame.top = t;
                        ViewRootImpl.this.mWinFrame.bottom = t + h;
                        if (ViewRootImpl.this.mView != null) {
                            ViewRootImpl.forceLayout(ViewRootImpl.this.mView);
                        }
                        ViewRootImpl.this.requestLayout();
                        return;
                    }
                    return;
                case 25:
                    ViewRootImpl.this.enqueueInputEvent((InputEvent) msg.obj, null, 32, true);
                    return;
                case 26:
                    ViewRootImpl.this.handleDispatchWindowShown();
                    return;
            }
            if (ViewRootImpl.this.mAdded) {
                SomeArgs args3 = (SomeArgs) msg.obj;
                Configuration config2 = (Configuration) args3.arg4;
                if (config2 != null) {
                    ViewRootImpl.this.updateConfiguration(config2, false);
                }
                ViewRootImpl.this.mWinFrame.set((Rect) args3.arg1);
                ViewRootImpl.this.mPendingOverscanInsets.set((Rect) args3.arg5);
                ViewRootImpl.this.mPendingContentInsets.set((Rect) args3.arg2);
                ViewRootImpl.this.mPendingStableInsets.set((Rect) args3.arg6);
                ViewRootImpl.this.mPendingVisibleInsets.set((Rect) args3.arg3);
                args3.recycle();
                if (msg.what == 5) {
                    ViewRootImpl.this.mReportNextDraw = true;
                }
                if (ViewRootImpl.this.mView != null) {
                    ViewRootImpl.forceLayout(ViewRootImpl.this.mView);
                }
                ViewRootImpl.this.requestLayout();
            }
        }
    }

    boolean ensureTouchMode(boolean inTouchMode) {
        if (this.mAttachInfo.mInTouchMode == inTouchMode) {
            return false;
        }
        try {
            if (!isInLocalFocusMode()) {
                this.mWindowSession.setInTouchMode(inTouchMode);
            }
            return ensureTouchModeLocally(inTouchMode);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean ensureTouchModeLocally(boolean inTouchMode) {
        if (this.mAttachInfo.mInTouchMode == inTouchMode) {
            return false;
        }
        this.mAttachInfo.mInTouchMode = inTouchMode;
        this.mAttachInfo.mTreeObserver.dispatchOnTouchModeChanged(inTouchMode);
        return inTouchMode ? enterTouchMode() : leaveTouchMode();
    }

    private boolean enterTouchMode() {
        View focused;
        if (this.mView == null || !this.mView.hasFocus() || (focused = this.mView.findFocus()) == null || focused.isFocusableInTouchMode()) {
            return false;
        }
        ViewGroup ancestorToTakeFocus = findAncestorToTakeFocusInTouchMode(focused);
        if (ancestorToTakeFocus != null) {
            return ancestorToTakeFocus.requestFocus();
        }
        focused.clearFocusInternal(null, true, false);
        return true;
    }

    private static ViewGroup findAncestorToTakeFocusInTouchMode(View focused) {
        ViewParent parent = focused.getParent();
        while (parent instanceof ViewGroup) {
            ViewGroup vgParent = (ViewGroup) parent;
            if (vgParent.getDescendantFocusability() != 262144 || !vgParent.isFocusableInTouchMode()) {
                if (vgParent.isRootNamespace()) {
                    return null;
                }
                parent = vgParent.getParent();
            } else {
                return vgParent;
            }
        }
        return null;
    }

    private boolean leaveTouchMode() {
        if (this.mView == null) {
            return false;
        }
        if (this.mView.hasFocus()) {
            View focusedView = this.mView.findFocus();
            if (!(focusedView instanceof ViewGroup) || ((ViewGroup) focusedView).getDescendantFocusability() != 262144) {
                return false;
            }
        }
        View focused = focusSearch(null, 130);
        if (focused != null) {
            return focused.requestFocus(130);
        }
        return false;
    }

    abstract class InputStage {
        protected static final int FINISH_HANDLED = 1;
        protected static final int FINISH_NOT_HANDLED = 2;
        protected static final int FORWARD = 0;
        private final InputStage mNext;

        public InputStage(InputStage next) {
            this.mNext = next;
        }

        public final void deliver(QueuedInputEvent q) {
            if ((q.mFlags & 4) != 0) {
                forward(q);
            } else if (shouldDropInputEvent(q)) {
                finish(q, false);
            } else {
                apply(q, onProcess(q));
            }
        }

        protected void finish(QueuedInputEvent q, boolean handled) {
            q.mFlags |= 4;
            if (handled) {
                q.mFlags |= 8;
            }
            forward(q);
        }

        protected void forward(QueuedInputEvent q) {
            onDeliverToNext(q);
        }

        protected void apply(QueuedInputEvent q, int result) {
            if (result == 0) {
                forward(q);
            } else if (result == 1) {
                finish(q, true);
            } else {
                if (result == 2) {
                    finish(q, false);
                    return;
                }
                throw new IllegalArgumentException("Invalid result: " + result);
            }
        }

        protected int onProcess(QueuedInputEvent q) {
            return 0;
        }

        protected void onDeliverToNext(QueuedInputEvent q) {
            if (this.mNext == null) {
                ViewRootImpl.this.finishInputEvent(q);
            } else {
                this.mNext.deliver(q);
            }
        }

        protected boolean shouldDropInputEvent(QueuedInputEvent q) {
            if (ViewRootImpl.this.mView == null || !ViewRootImpl.this.mAdded) {
                Slog.w(ViewRootImpl.TAG, "Dropping event due to root view being removed: " + q.mEvent);
                return true;
            }
            if ((ViewRootImpl.this.mAttachInfo.mHasWindowFocus && !ViewRootImpl.this.mStopped) || q.mEvent.isFromSource(2)) {
                return false;
            }
            if (ViewRootImpl.isTerminalInputEvent(q.mEvent)) {
                q.mEvent.cancel();
                Slog.w(ViewRootImpl.TAG, "Cancelling event due to no window focus: " + q.mEvent);
                return false;
            }
            Slog.w(ViewRootImpl.TAG, "Dropping event due to no window focus: " + q.mEvent);
            return true;
        }

        void dump(String prefix, PrintWriter writer) {
            if (this.mNext != null) {
                this.mNext.dump(prefix, writer);
            }
        }
    }

    abstract class AsyncInputStage extends InputStage {
        protected static final int DEFER = 3;
        private QueuedInputEvent mQueueHead;
        private int mQueueLength;
        private QueuedInputEvent mQueueTail;
        private final String mTraceCounter;

        public AsyncInputStage(InputStage next, String traceCounter) {
            super(next);
            this.mTraceCounter = traceCounter;
        }

        protected void defer(QueuedInputEvent q) {
            q.mFlags |= 2;
            enqueue(q);
        }

        @Override
        protected void forward(QueuedInputEvent q) {
            q.mFlags &= -3;
            QueuedInputEvent curr = this.mQueueHead;
            if (curr == null) {
                super.forward(q);
                return;
            }
            int deviceId = q.mEvent.getDeviceId();
            QueuedInputEvent prev = null;
            boolean blocked = false;
            while (curr != null && curr != q) {
                if (!blocked && deviceId == curr.mEvent.getDeviceId()) {
                    blocked = true;
                }
                prev = curr;
                curr = curr.mNext;
            }
            if (blocked) {
                if (curr == null) {
                    enqueue(q);
                    return;
                }
                return;
            }
            if (curr != null) {
                curr = curr.mNext;
                dequeue(q, prev);
            }
            super.forward(q);
            while (curr != null) {
                if (deviceId == curr.mEvent.getDeviceId()) {
                    if ((curr.mFlags & 2) == 0) {
                        QueuedInputEvent next = curr.mNext;
                        dequeue(curr, prev);
                        super.forward(curr);
                        curr = next;
                    } else {
                        return;
                    }
                } else {
                    prev = curr;
                    curr = curr.mNext;
                }
            }
        }

        @Override
        protected void apply(QueuedInputEvent q, int result) {
            if (result == 3) {
                defer(q);
            } else {
                super.apply(q, result);
            }
        }

        private void enqueue(QueuedInputEvent q) {
            if (this.mQueueTail == null) {
                this.mQueueHead = q;
                this.mQueueTail = q;
            } else {
                this.mQueueTail.mNext = q;
                this.mQueueTail = q;
            }
            this.mQueueLength++;
            Trace.traceCounter(4L, this.mTraceCounter, this.mQueueLength);
        }

        private void dequeue(QueuedInputEvent q, QueuedInputEvent prev) {
            if (prev == null) {
                this.mQueueHead = q.mNext;
            } else {
                prev.mNext = q.mNext;
            }
            if (this.mQueueTail == q) {
                this.mQueueTail = prev;
            }
            q.mNext = null;
            this.mQueueLength--;
            Trace.traceCounter(4L, this.mTraceCounter, this.mQueueLength);
        }

        @Override
        void dump(String prefix, PrintWriter writer) {
            writer.print(prefix);
            writer.print(getClass().getName());
            writer.print(": mQueueLength=");
            writer.println(this.mQueueLength);
            super.dump(prefix, writer);
        }
    }

    final class NativePreImeInputStage extends AsyncInputStage implements InputQueue.FinishedInputEventCallback {
        public NativePreImeInputStage(InputStage next, String traceCounter) {
            super(next, traceCounter);
        }

        @Override
        protected int onProcess(QueuedInputEvent q) {
            if (ViewRootImpl.this.mInputQueue == null || !(q.mEvent instanceof KeyEvent)) {
                return 0;
            }
            ViewRootImpl.this.mInputQueue.sendInputEvent(q.mEvent, q, true, this);
            return 3;
        }

        @Override
        public void onFinishedInputEvent(Object token, boolean handled) {
            QueuedInputEvent q = (QueuedInputEvent) token;
            if (handled) {
                finish(q, true);
            } else {
                forward(q);
            }
        }
    }

    final class ViewPreImeInputStage extends InputStage {
        public ViewPreImeInputStage(InputStage next) {
            super(next);
        }

        @Override
        protected int onProcess(QueuedInputEvent q) {
            if (q.mEvent instanceof KeyEvent) {
                return processKeyEvent(q);
            }
            return 0;
        }

        private int processKeyEvent(QueuedInputEvent q) {
            KeyEvent event = (KeyEvent) q.mEvent;
            return ViewRootImpl.this.mView.dispatchKeyEventPreIme(event) ? 1 : 0;
        }
    }

    final class ImeInputStage extends AsyncInputStage implements InputMethodManager.FinishedInputEventCallback {
        public ImeInputStage(InputStage next, String traceCounter) {
            super(next, traceCounter);
        }

        @Override
        protected int onProcess(QueuedInputEvent q) {
            InputMethodManager imm;
            if (!ViewRootImpl.this.mLastWasImTarget || ViewRootImpl.this.isInLocalFocusMode() || (imm = InputMethodManager.peekInstance()) == null) {
                return 0;
            }
            InputEvent event = q.mEvent;
            int result = imm.dispatchInputEvent(event, q, this, ViewRootImpl.this.mHandler);
            if (result == 1) {
                return 1;
            }
            return result == 0 ? 0 : 3;
        }

        @Override
        public void onFinishedInputEvent(Object token, boolean handled) {
            QueuedInputEvent q = (QueuedInputEvent) token;
            if (handled) {
                finish(q, true);
            } else {
                forward(q);
            }
        }
    }

    final class EarlyPostImeInputStage extends InputStage {
        public EarlyPostImeInputStage(InputStage next) {
            super(next);
        }

        @Override
        protected int onProcess(QueuedInputEvent q) {
            if (q.mEvent instanceof KeyEvent) {
                return processKeyEvent(q);
            }
            int source = q.mEvent.getSource();
            if ((source & 2) != 0) {
                return processPointerEvent(q);
            }
            return 0;
        }

        private int processKeyEvent(QueuedInputEvent q) {
            KeyEvent event = (KeyEvent) q.mEvent;
            if (ViewRootImpl.this.checkForLeavingTouchModeAndConsume(event)) {
                return 1;
            }
            ViewRootImpl.this.mFallbackEventHandler.preDispatchKeyEvent(event);
            return 0;
        }

        private int processPointerEvent(QueuedInputEvent q) {
            MotionEvent event = (MotionEvent) q.mEvent;
            if (ViewRootImpl.this.mTranslator != null) {
                ViewRootImpl.this.mTranslator.translateEventInScreenToAppWindow(event);
            }
            int action = event.getAction();
            if (action == 0 || action == 8) {
                ViewRootImpl.this.ensureTouchMode(true);
            }
            if (ViewRootImpl.this.mCurScrollY != 0) {
                event.offsetLocation(0.0f, ViewRootImpl.this.mCurScrollY);
            }
            if (event.isTouchEvent()) {
                ViewRootImpl.this.mLastTouchPoint.x = event.getRawX();
                ViewRootImpl.this.mLastTouchPoint.y = event.getRawY();
                return 0;
            }
            return 0;
        }
    }

    final class NativePostImeInputStage extends AsyncInputStage implements InputQueue.FinishedInputEventCallback {
        public NativePostImeInputStage(InputStage next, String traceCounter) {
            super(next, traceCounter);
        }

        @Override
        protected int onProcess(QueuedInputEvent q) {
            if (ViewRootImpl.this.mInputQueue == null) {
                return 0;
            }
            ViewRootImpl.this.mInputQueue.sendInputEvent(q.mEvent, q, false, this);
            return 3;
        }

        @Override
        public void onFinishedInputEvent(Object token, boolean handled) {
            QueuedInputEvent q = (QueuedInputEvent) token;
            if (handled) {
                finish(q, true);
            } else {
                forward(q);
            }
        }
    }

    final class ViewPostImeInputStage extends InputStage {
        public ViewPostImeInputStage(InputStage next) {
            super(next);
        }

        @Override
        protected int onProcess(QueuedInputEvent q) {
            if (q.mEvent instanceof KeyEvent) {
                return processKeyEvent(q);
            }
            ViewRootImpl.this.handleDispatchDoneAnimating();
            int source = q.mEvent.getSource();
            if ((source & 2) != 0) {
                return processPointerEvent(q);
            }
            if ((source & 4) != 0) {
                return processTrackballEvent(q);
            }
            return processGenericMotionEvent(q);
        }

        @Override
        protected void onDeliverToNext(QueuedInputEvent q) {
            if (ViewRootImpl.this.mUnbufferedInputDispatch && (q.mEvent instanceof MotionEvent) && ((MotionEvent) q.mEvent).isTouchEvent() && ViewRootImpl.isTerminalInputEvent(q.mEvent)) {
                ViewRootImpl.this.mUnbufferedInputDispatch = false;
                ViewRootImpl.this.scheduleConsumeBatchedInput();
            }
            super.onDeliverToNext(q);
        }

        private int processKeyEvent(QueuedInputEvent q) {
            KeyEvent event = (KeyEvent) q.mEvent;
            if (event.getAction() != 1) {
                ViewRootImpl.this.handleDispatchDoneAnimating();
            }
            if (ViewRootImpl.this.mView.dispatchKeyEvent(event)) {
                return 1;
            }
            if (shouldDropInputEvent(q)) {
                return 2;
            }
            if (event.getAction() == 0 && event.isCtrlPressed() && event.getRepeatCount() == 0 && !KeyEvent.isModifierKey(event.getKeyCode())) {
                if (ViewRootImpl.this.mView.dispatchKeyShortcutEvent(event)) {
                    return 1;
                }
                if (shouldDropInputEvent(q)) {
                    return 2;
                }
            }
            if (ViewRootImpl.this.mFallbackEventHandler.dispatchKeyEvent(event)) {
                return 1;
            }
            if (shouldDropInputEvent(q)) {
                return 2;
            }
            if (event.getAction() == 0) {
                int direction = 0;
                switch (event.getKeyCode()) {
                    case 19:
                        if (event.hasNoModifiers()) {
                            direction = 33;
                        }
                        break;
                    case 20:
                        if (event.hasNoModifiers()) {
                            direction = 130;
                        }
                        break;
                    case 21:
                        if (event.hasNoModifiers()) {
                            direction = 17;
                        }
                        break;
                    case 22:
                        if (event.hasNoModifiers()) {
                            direction = 66;
                        }
                        break;
                    case 61:
                        if (event.hasNoModifiers()) {
                            direction = 2;
                        } else if (event.hasModifiers(1)) {
                            direction = 1;
                        }
                        break;
                }
                if (direction != 0) {
                    View focused = ViewRootImpl.this.mView.findFocus();
                    if (focused != null) {
                        View v = focused.focusSearch(direction);
                        if (v != null && v != focused) {
                            focused.getFocusedRect(ViewRootImpl.this.mTempRect);
                            if (ViewRootImpl.this.mView instanceof ViewGroup) {
                                ((ViewGroup) ViewRootImpl.this.mView).offsetDescendantRectToMyCoords(focused, ViewRootImpl.this.mTempRect);
                                ((ViewGroup) ViewRootImpl.this.mView).offsetRectIntoDescendantCoords(v, ViewRootImpl.this.mTempRect);
                            }
                            if (v.requestFocus(direction, ViewRootImpl.this.mTempRect)) {
                                ViewRootImpl.this.playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
                                return 1;
                            }
                        }
                        if (ViewRootImpl.this.mView.dispatchUnhandledMove(focused, direction)) {
                            return 1;
                        }
                    } else {
                        View v2 = ViewRootImpl.this.focusSearch(null, direction);
                        if (v2 != null && v2.requestFocus(direction)) {
                            return 1;
                        }
                    }
                }
            }
            return 0;
        }

        private int processPointerEvent(QueuedInputEvent q) {
            MotionEvent event = (MotionEvent) q.mEvent;
            ViewRootImpl.this.mAttachInfo.mUnbufferedDispatchRequested = false;
            boolean handled = ViewRootImpl.this.mView.dispatchPointerEvent(event);
            if (ViewRootImpl.this.mAttachInfo.mUnbufferedDispatchRequested && !ViewRootImpl.this.mUnbufferedInputDispatch) {
                ViewRootImpl.this.mUnbufferedInputDispatch = true;
                if (ViewRootImpl.this.mConsumeBatchedInputScheduled) {
                    ViewRootImpl.this.scheduleConsumeBatchedInputImmediately();
                }
            }
            return handled ? 1 : 0;
        }

        private int processTrackballEvent(QueuedInputEvent q) {
            MotionEvent event = (MotionEvent) q.mEvent;
            return ViewRootImpl.this.mView.dispatchTrackballEvent(event) ? 1 : 0;
        }

        private int processGenericMotionEvent(QueuedInputEvent q) {
            MotionEvent event = (MotionEvent) q.mEvent;
            return ViewRootImpl.this.mView.dispatchGenericMotionEvent(event) ? 1 : 0;
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
        protected int onProcess(QueuedInputEvent q) {
            q.mFlags |= 16;
            if (q.mEvent instanceof MotionEvent) {
                MotionEvent event = (MotionEvent) q.mEvent;
                int source = event.getSource();
                if ((source & 4) != 0) {
                    this.mTrackball.process(event);
                    return 1;
                }
                if ((source & 16) != 0) {
                    this.mJoystick.process(event);
                    return 1;
                }
                if ((source & 2097152) == 2097152) {
                    this.mTouchNavigation.process(event);
                    return 1;
                }
            } else if ((q.mFlags & 32) != 0) {
                this.mKeyboard.process((KeyEvent) q.mEvent);
                return 1;
            }
            return 0;
        }

        @Override
        protected void onDeliverToNext(QueuedInputEvent q) {
            if ((q.mFlags & 16) == 0 && (q.mEvent instanceof MotionEvent)) {
                MotionEvent event = (MotionEvent) q.mEvent;
                int source = event.getSource();
                if ((source & 4) != 0) {
                    this.mTrackball.cancel(event);
                } else if ((source & 16) == 0) {
                    if ((source & 2097152) == 2097152) {
                        this.mTouchNavigation.cancel(event);
                    }
                } else {
                    this.mJoystick.cancel(event);
                }
            }
            super.onDeliverToNext(q);
        }
    }

    final class SyntheticTrackballHandler {
        private long mLastTime;
        private final TrackballAxis mX = new TrackballAxis();
        private final TrackballAxis mY = new TrackballAxis();

        SyntheticTrackballHandler() {
        }

        public void process(MotionEvent event) {
            long curTime = SystemClock.uptimeMillis();
            if (this.mLastTime + 250 < curTime) {
                this.mX.reset(0);
                this.mY.reset(0);
                this.mLastTime = curTime;
            }
            int action = event.getAction();
            int metaState = event.getMetaState();
            switch (action) {
                case 0:
                    this.mX.reset(2);
                    this.mY.reset(2);
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime, curTime, 0, 23, 0, metaState, -1, 0, 1024, 257));
                    break;
                case 1:
                    this.mX.reset(2);
                    this.mY.reset(2);
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime, curTime, 1, 23, 0, metaState, -1, 0, 1024, 257));
                    break;
            }
            float xOff = this.mX.collect(event.getX(), event.getEventTime(), "X");
            float yOff = this.mY.collect(event.getY(), event.getEventTime(), "Y");
            int keycode = 0;
            int movement = 0;
            float accel = 1.0f;
            if (xOff > yOff) {
                movement = this.mX.generate();
                if (movement != 0) {
                    keycode = movement > 0 ? 22 : 21;
                    accel = this.mX.acceleration;
                    this.mY.reset(2);
                }
            } else if (yOff > 0.0f && (movement = this.mY.generate()) != 0) {
                keycode = movement > 0 ? 20 : 19;
                accel = this.mY.acceleration;
                this.mX.reset(2);
            }
            if (keycode != 0) {
                if (movement < 0) {
                    movement = -movement;
                }
                int accelMovement = (int) (movement * accel);
                if (accelMovement > movement) {
                    movement--;
                    int repeatCount = accelMovement - movement;
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime, curTime, 2, keycode, repeatCount, metaState, -1, 0, 1024, 257));
                }
                while (movement > 0) {
                    movement--;
                    curTime = SystemClock.uptimeMillis();
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime, curTime, 0, keycode, 0, metaState, -1, 0, 1024, 257));
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime, curTime, 1, keycode, 0, metaState, -1, 0, 1024, 257));
                }
                this.mLastTime = curTime;
            }
        }

        public void cancel(MotionEvent event) {
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

        void reset(int _step) {
            this.position = 0.0f;
            this.acceleration = 1.0f;
            this.lastMoveTime = 0L;
            this.step = _step;
            this.dir = 0;
        }

        float collect(float off, long time, String axis) {
            long normTime;
            if (off > 0.0f) {
                normTime = (long) (150.0f * off);
                if (this.dir < 0) {
                    this.position = 0.0f;
                    this.step = 0;
                    this.acceleration = 1.0f;
                    this.lastMoveTime = 0L;
                }
                this.dir = 1;
            } else if (off < 0.0f) {
                normTime = (long) ((-off) * 150.0f);
                if (this.dir > 0) {
                    this.position = 0.0f;
                    this.step = 0;
                    this.acceleration = 1.0f;
                    this.lastMoveTime = 0L;
                }
                this.dir = -1;
            } else {
                normTime = 0;
            }
            if (normTime > 0) {
                long delta = time - this.lastMoveTime;
                this.lastMoveTime = time;
                float acc = this.acceleration;
                if (delta < normTime) {
                    float scale = (normTime - delta) * ACCEL_MOVE_SCALING_FACTOR;
                    if (scale > 1.0f) {
                        acc *= scale;
                    }
                    if (acc >= MAX_ACCELERATION) {
                        acc = MAX_ACCELERATION;
                    }
                    this.acceleration = acc;
                } else {
                    float scale2 = (delta - normTime) * ACCEL_MOVE_SCALING_FACTOR;
                    if (scale2 > 1.0f) {
                        acc /= scale2;
                    }
                    if (acc <= 1.0f) {
                        acc = 1.0f;
                    }
                    this.acceleration = acc;
                }
            }
            this.position += off;
            return Math.abs(this.position);
        }

        int generate() {
            int movement = 0;
            this.nonAccelMovement = 0;
            while (true) {
                int dir = this.position >= 0.0f ? 1 : -1;
                switch (this.step) {
                    case 0:
                        if (Math.abs(this.position) >= FIRST_MOVEMENT_THRESHOLD) {
                            movement += dir;
                            this.nonAccelMovement += dir;
                            this.step = 1;
                        }
                        break;
                    case 1:
                        if (Math.abs(this.position) >= SECOND_CUMULATIVE_MOVEMENT_THRESHOLD) {
                            movement += dir;
                            this.nonAccelMovement += dir;
                            this.position -= dir * SECOND_CUMULATIVE_MOVEMENT_THRESHOLD;
                            this.step = 2;
                        }
                        break;
                    default:
                        if (Math.abs(this.position) >= 1.0f) {
                            movement += dir;
                            this.position -= dir * 1.0f;
                            float acc = this.acceleration * 1.1f;
                            if (acc >= MAX_ACCELERATION) {
                                acc = this.acceleration;
                            }
                            this.acceleration = acc;
                        }
                        break;
                }
            }
            return movement;
        }
    }

    final class SyntheticJoystickHandler extends Handler {
        private static final int MSG_ENQUEUE_X_AXIS_KEY_REPEAT = 1;
        private static final int MSG_ENQUEUE_Y_AXIS_KEY_REPEAT = 2;
        private static final String TAG = "SyntheticJoystickHandler";
        private int mLastXDirection;
        private int mLastXKeyCode;
        private int mLastYDirection;
        private int mLastYKeyCode;

        public SyntheticJoystickHandler() {
            super(true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                case 2:
                    KeyEvent oldEvent = (KeyEvent) msg.obj;
                    KeyEvent e = KeyEvent.changeTimeRepeat(oldEvent, SystemClock.uptimeMillis(), oldEvent.getRepeatCount() + 1);
                    if (ViewRootImpl.this.mAttachInfo.mHasWindowFocus) {
                        ViewRootImpl.this.enqueueInputEvent(e);
                        Message m = obtainMessage(msg.what, e);
                        m.setAsynchronous(true);
                        sendMessageDelayed(m, ViewConfiguration.getKeyRepeatDelay());
                    }
                    break;
            }
        }

        public void process(MotionEvent event) {
            switch (event.getActionMasked()) {
                case 2:
                    update(event, true);
                    break;
                case 3:
                    cancel(event);
                    break;
                default:
                    Log.w(TAG, "Unexpected action: " + event.getActionMasked());
                    break;
            }
        }

        private void cancel(MotionEvent event) {
            removeMessages(1);
            removeMessages(2);
            update(event, false);
        }

        private void update(MotionEvent event, boolean synthesizeNewKeys) {
            long time = event.getEventTime();
            int metaState = event.getMetaState();
            int deviceId = event.getDeviceId();
            int source = event.getSource();
            int xDirection = joystickAxisValueToDirection(event.getAxisValue(15));
            if (xDirection == 0) {
                xDirection = joystickAxisValueToDirection(event.getX());
            }
            int yDirection = joystickAxisValueToDirection(event.getAxisValue(16));
            if (yDirection == 0) {
                yDirection = joystickAxisValueToDirection(event.getY());
            }
            if (xDirection != this.mLastXDirection) {
                if (this.mLastXKeyCode != 0) {
                    removeMessages(1);
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(time, time, 1, this.mLastXKeyCode, 0, metaState, deviceId, 0, 1024, source));
                    this.mLastXKeyCode = 0;
                }
                this.mLastXDirection = xDirection;
                if (xDirection != 0 && synthesizeNewKeys) {
                    this.mLastXKeyCode = xDirection > 0 ? 22 : 21;
                    KeyEvent e = new KeyEvent(time, time, 0, this.mLastXKeyCode, 0, metaState, deviceId, 0, 1024, source);
                    ViewRootImpl.this.enqueueInputEvent(e);
                    Message m = obtainMessage(1, e);
                    m.setAsynchronous(true);
                    sendMessageDelayed(m, ViewConfiguration.getKeyRepeatTimeout());
                }
            }
            if (yDirection != this.mLastYDirection) {
                if (this.mLastYKeyCode != 0) {
                    removeMessages(2);
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(time, time, 1, this.mLastYKeyCode, 0, metaState, deviceId, 0, 1024, source));
                    this.mLastYKeyCode = 0;
                }
                this.mLastYDirection = yDirection;
                if (yDirection != 0 && synthesizeNewKeys) {
                    this.mLastYKeyCode = yDirection > 0 ? 20 : 19;
                    KeyEvent e2 = new KeyEvent(time, time, 0, this.mLastYKeyCode, 0, metaState, deviceId, 0, 1024, source);
                    ViewRootImpl.this.enqueueInputEvent(e2);
                    Message m2 = obtainMessage(2, e2);
                    m2.setAsynchronous(true);
                    sendMessageDelayed(m2, ViewConfiguration.getKeyRepeatTimeout());
                }
            }
        }

        private int joystickAxisValueToDirection(float value) {
            if (value >= 0.5f) {
                return 1;
            }
            if (value <= -0.5f) {
                return -1;
            }
            return 0;
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

        static float access$1332(SyntheticTouchNavigationHandler x0, float x1) {
            float f = x0.mFlingVelocity * x1;
            x0.mFlingVelocity = f;
            return f;
        }

        public SyntheticTouchNavigationHandler() {
            super(true);
            this.mCurrentDeviceId = -1;
            this.mActivePointerId = -1;
            this.mPendingKeyCode = 0;
            this.mFlingRunnable = new Runnable() {
                @Override
                public void run() {
                    long time = SystemClock.uptimeMillis();
                    SyntheticTouchNavigationHandler.this.sendKeyDownOrRepeat(time, SyntheticTouchNavigationHandler.this.mPendingKeyCode, SyntheticTouchNavigationHandler.this.mPendingKeyMetaState);
                    SyntheticTouchNavigationHandler.access$1332(SyntheticTouchNavigationHandler.this, SyntheticTouchNavigationHandler.FLING_TICK_DECAY);
                    if (!SyntheticTouchNavigationHandler.this.postFling(time)) {
                        SyntheticTouchNavigationHandler.this.mFlinging = false;
                        SyntheticTouchNavigationHandler.this.finishKeys(time);
                    }
                }
            };
        }

        public void process(MotionEvent event) {
            long time = event.getEventTime();
            int deviceId = event.getDeviceId();
            int source = event.getSource();
            if (this.mCurrentDeviceId != deviceId || this.mCurrentSource != source) {
                finishKeys(time);
                finishTracking(time);
                this.mCurrentDeviceId = deviceId;
                this.mCurrentSource = source;
                this.mCurrentDeviceSupported = false;
                InputDevice device = event.getDevice();
                if (device != null) {
                    InputDevice.MotionRange xRange = device.getMotionRange(0);
                    InputDevice.MotionRange yRange = device.getMotionRange(1);
                    if (xRange != null && yRange != null) {
                        this.mCurrentDeviceSupported = true;
                        float xRes = xRange.getResolution();
                        if (xRes <= 0.0f) {
                            xRes = xRange.getRange() / 48.0f;
                        }
                        float yRes = yRange.getResolution();
                        if (yRes <= 0.0f) {
                            yRes = yRange.getRange() / 48.0f;
                        }
                        float nominalRes = (xRes + yRes) * 0.5f;
                        this.mConfigTickDistance = 12.0f * nominalRes;
                        this.mConfigMinFlingVelocity = MIN_FLING_VELOCITY_TICKS_PER_SECOND * this.mConfigTickDistance;
                        this.mConfigMaxFlingVelocity = MAX_FLING_VELOCITY_TICKS_PER_SECOND * this.mConfigTickDistance;
                    }
                }
            }
            if (this.mCurrentDeviceSupported) {
                int action = event.getActionMasked();
                switch (action) {
                    case 0:
                        boolean caughtFling = this.mFlinging;
                        finishKeys(time);
                        finishTracking(time);
                        this.mActivePointerId = event.getPointerId(0);
                        this.mVelocityTracker = VelocityTracker.obtain();
                        this.mVelocityTracker.addMovement(event);
                        this.mStartX = event.getX();
                        this.mStartY = event.getY();
                        this.mLastX = this.mStartX;
                        this.mLastY = this.mStartY;
                        this.mAccumulatedX = 0.0f;
                        this.mAccumulatedY = 0.0f;
                        this.mConsumedMovement = caughtFling;
                        break;
                    case 1:
                    case 2:
                        if (this.mActivePointerId >= 0) {
                            int index = event.findPointerIndex(this.mActivePointerId);
                            if (index < 0) {
                                finishKeys(time);
                                finishTracking(time);
                            } else {
                                this.mVelocityTracker.addMovement(event);
                                float x = event.getX(index);
                                float y = event.getY(index);
                                this.mAccumulatedX += x - this.mLastX;
                                this.mAccumulatedY += y - this.mLastY;
                                this.mLastX = x;
                                this.mLastY = y;
                                int metaState = event.getMetaState();
                                consumeAccumulatedMovement(time, metaState);
                                if (action == 1) {
                                    if (this.mConsumedMovement && this.mPendingKeyCode != 0) {
                                        this.mVelocityTracker.computeCurrentVelocity(1000, this.mConfigMaxFlingVelocity);
                                        float vx = this.mVelocityTracker.getXVelocity(this.mActivePointerId);
                                        float vy = this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                                        if (!startFling(time, vx, vy)) {
                                            finishKeys(time);
                                        }
                                    }
                                    finishTracking(time);
                                }
                            }
                        }
                        break;
                    case 3:
                        finishKeys(time);
                        finishTracking(time);
                        break;
                }
            }
        }

        public void cancel(MotionEvent event) {
            if (this.mCurrentDeviceId == event.getDeviceId() && this.mCurrentSource == event.getSource()) {
                long time = event.getEventTime();
                finishKeys(time);
                finishTracking(time);
            }
        }

        private void finishKeys(long time) {
            cancelFling();
            sendKeyUp(time);
        }

        private void finishTracking(long time) {
            if (this.mActivePointerId >= 0) {
                this.mActivePointerId = -1;
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
            }
        }

        private void consumeAccumulatedMovement(long time, int metaState) {
            float absX = Math.abs(this.mAccumulatedX);
            float absY = Math.abs(this.mAccumulatedY);
            if (absX >= absY) {
                if (absX >= this.mConfigTickDistance) {
                    this.mAccumulatedX = consumeAccumulatedMovement(time, metaState, this.mAccumulatedX, 21, 22);
                    this.mAccumulatedY = 0.0f;
                    this.mConsumedMovement = true;
                    return;
                }
                return;
            }
            if (absY >= this.mConfigTickDistance) {
                this.mAccumulatedY = consumeAccumulatedMovement(time, metaState, this.mAccumulatedY, 19, 20);
                this.mAccumulatedX = 0.0f;
                this.mConsumedMovement = true;
            }
        }

        private float consumeAccumulatedMovement(long time, int metaState, float accumulator, int negativeKeyCode, int positiveKeyCode) {
            while (accumulator <= (-this.mConfigTickDistance)) {
                sendKeyDownOrRepeat(time, negativeKeyCode, metaState);
                accumulator += this.mConfigTickDistance;
            }
            while (accumulator >= this.mConfigTickDistance) {
                sendKeyDownOrRepeat(time, positiveKeyCode, metaState);
                accumulator -= this.mConfigTickDistance;
            }
            return accumulator;
        }

        private void sendKeyDownOrRepeat(long time, int keyCode, int metaState) {
            if (this.mPendingKeyCode != keyCode) {
                sendKeyUp(time);
                this.mPendingKeyDownTime = time;
                this.mPendingKeyCode = keyCode;
                this.mPendingKeyRepeatCount = 0;
            } else {
                this.mPendingKeyRepeatCount++;
            }
            this.mPendingKeyMetaState = metaState;
            ViewRootImpl.this.enqueueInputEvent(new KeyEvent(this.mPendingKeyDownTime, time, 0, this.mPendingKeyCode, this.mPendingKeyRepeatCount, this.mPendingKeyMetaState, this.mCurrentDeviceId, 1024, this.mCurrentSource));
        }

        private void sendKeyUp(long time) {
            if (this.mPendingKeyCode != 0) {
                ViewRootImpl.this.enqueueInputEvent(new KeyEvent(this.mPendingKeyDownTime, time, 1, this.mPendingKeyCode, 0, this.mPendingKeyMetaState, this.mCurrentDeviceId, 0, 1024, this.mCurrentSource));
                this.mPendingKeyCode = 0;
            }
        }

        private boolean startFling(long time, float vx, float vy) {
            switch (this.mPendingKeyCode) {
                case 19:
                    if ((-vy) < this.mConfigMinFlingVelocity || Math.abs(vx) >= this.mConfigMinFlingVelocity) {
                        return false;
                    }
                    this.mFlingVelocity = -vy;
                    break;
                case 20:
                    if (vy < this.mConfigMinFlingVelocity || Math.abs(vx) >= this.mConfigMinFlingVelocity) {
                        return false;
                    }
                    this.mFlingVelocity = vy;
                    break;
                case 21:
                    if ((-vx) < this.mConfigMinFlingVelocity || Math.abs(vy) >= this.mConfigMinFlingVelocity) {
                        return false;
                    }
                    this.mFlingVelocity = -vx;
                    break;
                case 22:
                    if (vx < this.mConfigMinFlingVelocity || Math.abs(vy) >= this.mConfigMinFlingVelocity) {
                        return false;
                    }
                    this.mFlingVelocity = vx;
                    break;
            }
            this.mFlinging = postFling(time);
            return this.mFlinging;
        }

        private boolean postFling(long time) {
            if (this.mFlingVelocity < this.mConfigMinFlingVelocity) {
                return false;
            }
            long delay = (long) ((this.mConfigTickDistance / this.mFlingVelocity) * 1000.0f);
            postAtTime(this.mFlingRunnable, time + delay);
            return true;
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

        public void process(KeyEvent event) {
            if ((event.getFlags() & 1024) == 0) {
                KeyCharacterMap kcm = event.getKeyCharacterMap();
                int keyCode = event.getKeyCode();
                int metaState = event.getMetaState();
                KeyCharacterMap.FallbackAction fallbackAction = kcm.getFallbackAction(keyCode, metaState);
                if (fallbackAction != null) {
                    int flags = event.getFlags() | 1024;
                    KeyEvent fallbackEvent = KeyEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), fallbackAction.keyCode, event.getRepeatCount(), fallbackAction.metaState, event.getDeviceId(), event.getScanCode(), flags, event.getSource(), null);
                    fallbackAction.recycle();
                    ViewRootImpl.this.enqueueInputEvent(fallbackEvent);
                }
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

    private boolean checkForLeavingTouchModeAndConsume(KeyEvent event) {
        if (!this.mAttachInfo.mInTouchMode) {
            return false;
        }
        int action = event.getAction();
        if ((action != 0 && action != 2) || (event.getFlags() & 4) != 0) {
            return false;
        }
        if (isNavigationKey(event)) {
            return ensureTouchMode(false);
        }
        if (!isTypingKey(event)) {
            return false;
        }
        ensureTouchMode(false);
        return false;
    }

    void setLocalDragState(Object obj) {
        this.mLocalDragState = obj;
    }

    private void handleDragEvent(DragEvent event) {
        if (this.mView != null && this.mAdded) {
            int what = event.mAction;
            if (what == 6) {
                this.mView.dispatchDragEvent(event);
            } else {
                if (what == 1) {
                    this.mCurrentDragView = null;
                    this.mDragDescription = event.mClipDescription;
                } else {
                    event.mClipDescription = this.mDragDescription;
                }
                if (what == 2 || what == 3) {
                    this.mDragPoint.set(event.mX, event.mY);
                    if (this.mTranslator != null) {
                        this.mTranslator.translatePointInScreenToAppWindow(this.mDragPoint);
                    }
                    if (this.mCurScrollY != 0) {
                        this.mDragPoint.offset(0.0f, this.mCurScrollY);
                    }
                    event.mX = this.mDragPoint.x;
                    event.mY = this.mDragPoint.y;
                }
                View prevDragView = this.mCurrentDragView;
                boolean result = this.mView.dispatchDragEvent(event);
                if (prevDragView != this.mCurrentDragView) {
                    if (prevDragView != null) {
                        try {
                            this.mWindowSession.dragRecipientExited(this.mWindow);
                        } catch (RemoteException e) {
                            Slog.e(TAG, "Unable to note drag target change");
                        }
                    }
                    if (this.mCurrentDragView != null) {
                        this.mWindowSession.dragRecipientEntered(this.mWindow);
                    }
                }
                if (what == 3) {
                    this.mDragDescription = null;
                    try {
                        Log.i(TAG, "Reporting drop result: " + result);
                        this.mWindowSession.reportDropResult(this.mWindow, result);
                    } catch (RemoteException e2) {
                        Log.e(TAG, "Unable to report drop result");
                    }
                }
                if (what == 4) {
                    setLocalDragState(null);
                }
            }
        }
        event.recycle();
    }

    public void handleDispatchSystemUiVisibilityChanged(SystemUiVisibilityInfo args) {
        if (this.mSeq != args.seq) {
            this.mSeq = args.seq;
            this.mAttachInfo.mForceReportNewAttributes = true;
            scheduleTraversals();
        }
        if (this.mView != null) {
            if (args.localChanges != 0) {
                this.mView.updateLocalSystemUiVisibility(args.localValue, args.localChanges);
            }
            int visibility = args.globalVisibility & 7;
            if (visibility != this.mAttachInfo.mGlobalSystemUiVisibility) {
                this.mAttachInfo.mGlobalSystemUiVisibility = visibility;
                this.mView.dispatchSystemUiVisibilityChanged(visibility);
            }
        }
    }

    public void handleDispatchDoneAnimating() {
        if (this.mWindowsAnimating) {
            this.mWindowsAnimating = false;
            if (!this.mDirty.isEmpty() || this.mIsAnimating || this.mFullRedrawNeeded) {
                scheduleTraversals();
            }
        }
    }

    public void handleDispatchWindowShown() {
        this.mAttachInfo.mTreeObserver.dispatchOnWindowShown();
    }

    public void getLastTouchPoint(Point outLocation) {
        outLocation.x = (int) this.mLastTouchPoint.x;
        outLocation.y = (int) this.mLastTouchPoint.y;
    }

    public void setDragFocus(View newDragTarget) {
        if (this.mCurrentDragView != newDragTarget) {
            this.mCurrentDragView = newDragTarget;
        }
    }

    private AudioManager getAudioManager() {
        if (this.mView == null) {
            throw new IllegalStateException("getAudioManager called when there is no mView");
        }
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mView.getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        return this.mAudioManager;
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

    private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility, boolean insetsPending) throws RemoteException {
        float appScale = this.mAttachInfo.mApplicationScale;
        boolean restore = false;
        if (params != null && this.mTranslator != null) {
            restore = true;
            params.backup();
            this.mTranslator.translateWindowLayout(params);
        }
        if (params != null) {
        }
        this.mPendingConfiguration.seq = 0;
        if (params != null && this.mOrigWindowType != params.type && this.mTargetSdkVersion < 14) {
            Slog.w(TAG, "Window type can not be changed after the window is added; ignoring change of " + this.mView);
            params.type = this.mOrigWindowType;
        }
        int relayoutResult = this.mWindowSession.relayout(this.mWindow, this.mSeq, params, (int) ((this.mView.getMeasuredWidth() * appScale) + 0.5f), (int) ((this.mView.getMeasuredHeight() * appScale) + 0.5f), viewVisibility, insetsPending ? 1 : 0, this.mWinFrame, this.mPendingOverscanInsets, this.mPendingContentInsets, this.mPendingVisibleInsets, this.mPendingStableInsets, this.mPendingConfiguration, this.mSurface);
        if (restore) {
            params.restore();
        }
        if (this.mTranslator != null) {
            this.mTranslator.translateRectInScreenToAppWinFrame(this.mWinFrame);
            this.mTranslator.translateRectInScreenToAppWindow(this.mPendingOverscanInsets);
            this.mTranslator.translateRectInScreenToAppWindow(this.mPendingContentInsets);
            this.mTranslator.translateRectInScreenToAppWindow(this.mPendingVisibleInsets);
            this.mTranslator.translateRectInScreenToAppWindow(this.mPendingStableInsets);
        }
        return relayoutResult;
    }

    @Override
    public void playSoundEffect(int effectId) {
        checkThread();
        if (!this.mMediaDisabled) {
            try {
                AudioManager audioManager = getAudioManager();
                switch (effectId) {
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
                        throw new IllegalArgumentException("unknown effect id " + effectId + " not defined in " + SoundEffectConstants.class.getCanonicalName());
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "FATAL EXCEPTION when attempting to play sound effect: " + e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean performHapticFeedback(int effectId, boolean always) {
        try {
            return this.mWindowSession.performHapticFeedback(this.mWindow, effectId, always);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public View focusSearch(View focused, int direction) {
        checkThread();
        if (this.mView instanceof ViewGroup) {
            return FocusFinder.getInstance().findNextFocus((ViewGroup) this.mView, focused, direction);
        }
        return null;
    }

    public void debug() {
        this.mView.debug();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.println("ViewRoot:");
        writer.print(innerPrefix);
        writer.print("mAdded=");
        writer.print(this.mAdded);
        writer.print(" mRemoved=");
        writer.println(this.mRemoved);
        writer.print(innerPrefix);
        writer.print("mConsumeBatchedInputScheduled=");
        writer.println(this.mConsumeBatchedInputScheduled);
        writer.print(innerPrefix);
        writer.print("mConsumeBatchedInputImmediatelyScheduled=");
        writer.println(this.mConsumeBatchedInputImmediatelyScheduled);
        writer.print(innerPrefix);
        writer.print("mPendingInputEventCount=");
        writer.println(this.mPendingInputEventCount);
        writer.print(innerPrefix);
        writer.print("mProcessInputEventsScheduled=");
        writer.println(this.mProcessInputEventsScheduled);
        writer.print(innerPrefix);
        writer.print("mTraversalScheduled=");
        writer.print(this.mTraversalScheduled);
        if (this.mTraversalScheduled) {
            writer.print(" (barrier=");
            writer.print(this.mTraversalBarrier);
            writer.println(")");
        } else {
            writer.println();
        }
        this.mFirstInputStage.dump(innerPrefix, writer);
        this.mChoreographer.dump(prefix, writer);
        writer.print(prefix);
        writer.println("View Hierarchy:");
        dumpViewHierarchy(innerPrefix, writer, this.mView);
    }

    private void dumpViewHierarchy(String prefix, PrintWriter writer, View view) {
        ViewGroup grp;
        int N;
        writer.print(prefix);
        if (view == null) {
            writer.println("null");
            return;
        }
        writer.println(view.toString());
        if ((view instanceof ViewGroup) && (N = (grp = (ViewGroup) view).getChildCount()) > 0) {
            String prefix2 = prefix + "  ";
            for (int i = 0; i < N; i++) {
                dumpViewHierarchy(prefix2, writer, grp.getChildAt(i));
            }
        }
    }

    public void dumpGfxInfo(int[] info) {
        info[1] = 0;
        info[0] = 0;
        if (this.mView != null) {
            getGfxInfo(this.mView, info);
        }
    }

    private static void getGfxInfo(View view, int[] info) {
        RenderNode renderNode = view.mRenderNode;
        info[0] = info[0] + 1;
        if (renderNode != null) {
            info[1] = info[1] + renderNode.getDebugSize();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                getGfxInfo(group.getChildAt(i), info);
            }
        }
    }

    boolean die(boolean immediate) {
        if (immediate && !this.mIsInTraversal) {
            doDie();
            return false;
        }
        if (!this.mIsDrawing) {
            destroyHardwareRenderer();
        } else {
            Log.e(TAG, "Attempting to destroy the window while drawing!\n  window=" + this + ", title=" + ((Object) this.mWindowAttributes.getTitle()));
        }
        this.mHandler.sendEmptyMessage(3);
        return true;
    }

    void doDie() {
        checkThread();
        synchronized (this) {
            if (!this.mRemoved) {
                this.mRemoved = true;
                if (this.mAdded) {
                    dispatchDetachedFromWindow();
                }
                if (this.mAdded && !this.mFirst) {
                    destroyHardwareRenderer();
                    if (this.mView != null) {
                        int viewVisibility = this.mView.getVisibility();
                        boolean viewVisibilityChanged = this.mViewVisibility != viewVisibility;
                        if (this.mWindowAttributesChanged || viewVisibilityChanged) {
                            try {
                                if ((relayoutWindow(this.mWindowAttributes, viewVisibility, false) & 2) != 0) {
                                    this.mWindowSession.finishDrawing(this.mWindow);
                                }
                            } catch (RemoteException e) {
                            }
                        }
                        this.mSurface.release();
                    }
                }
                this.mAdded = false;
                WindowManagerGlobal.getInstance().doRemoveView(this);
            }
        }
    }

    public void requestUpdateConfiguration(Configuration config) {
        Message msg = this.mHandler.obtainMessage(18, config);
        this.mHandler.sendMessage(msg);
    }

    public void loadSystemProperties() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ViewRootImpl.this.mProfileRendering = SystemProperties.getBoolean(ViewRootImpl.PROPERTY_PROFILE_RENDERING, false);
                ViewRootImpl.this.profileRendering(ViewRootImpl.this.mAttachInfo.mHasWindowFocus);
                ViewRootImpl.this.mMediaDisabled = SystemProperties.getBoolean(ViewRootImpl.PROPERTY_MEDIA_DISABLED, false);
                if (ViewRootImpl.this.mAttachInfo.mHardwareRenderer != null && ViewRootImpl.this.mAttachInfo.mHardwareRenderer.loadSystemProperties()) {
                    ViewRootImpl.this.invalidate();
                }
                boolean layout = SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false);
                if (layout != ViewRootImpl.this.mAttachInfo.mDebugLayout) {
                    ViewRootImpl.this.mAttachInfo.mDebugLayout = layout;
                    if (!ViewRootImpl.this.mHandler.hasMessages(23)) {
                        ViewRootImpl.this.mHandler.sendEmptyMessageDelayed(23, 200L);
                    }
                }
                ViewRootImpl.this.mIsEmulator = Build.HARDWARE.contains("goldfish");
                ViewRootImpl.this.mIsCircularEmulator = SystemProperties.getBoolean(ViewRootImpl.PROPERTY_EMULATOR_CIRCULAR, false);
            }
        });
    }

    private void destroyHardwareRenderer() {
        HardwareRenderer hardwareRenderer = this.mAttachInfo.mHardwareRenderer;
        if (hardwareRenderer != null) {
            if (this.mView != null) {
                hardwareRenderer.destroyHardwareResources(this.mView);
            }
            hardwareRenderer.destroy();
            hardwareRenderer.setRequested(false);
            this.mAttachInfo.mHardwareRenderer = null;
            this.mAttachInfo.mHardwareAccelerated = false;
        }
    }

    public void dispatchFinishInputConnection(InputConnection connection) {
        Message msg = this.mHandler.obtainMessage(12, connection);
        this.mHandler.sendMessage(msg);
    }

    public void dispatchResized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, boolean reportDraw, Configuration newConfig) {
        Message msg = this.mHandler.obtainMessage(reportDraw ? 5 : 4);
        if (this.mTranslator != null) {
            this.mTranslator.translateRectInScreenToAppWindow(frame);
            this.mTranslator.translateRectInScreenToAppWindow(overscanInsets);
            this.mTranslator.translateRectInScreenToAppWindow(contentInsets);
            this.mTranslator.translateRectInScreenToAppWindow(visibleInsets);
        }
        SomeArgs args = SomeArgs.obtain();
        boolean sameProcessCall = Binder.getCallingPid() == Process.myPid();
        if (sameProcessCall) {
            frame = new Rect(frame);
        }
        args.arg1 = frame;
        if (sameProcessCall) {
            contentInsets = new Rect(contentInsets);
        }
        args.arg2 = contentInsets;
        if (sameProcessCall) {
            visibleInsets = new Rect(visibleInsets);
        }
        args.arg3 = visibleInsets;
        if (sameProcessCall && newConfig != null) {
            newConfig = new Configuration(newConfig);
        }
        args.arg4 = newConfig;
        if (sameProcessCall) {
            overscanInsets = new Rect(overscanInsets);
        }
        args.arg5 = overscanInsets;
        if (sameProcessCall) {
            stableInsets = new Rect(stableInsets);
        }
        args.arg6 = stableInsets;
        msg.obj = args;
        this.mHandler.sendMessage(msg);
    }

    public void dispatchMoved(int newX, int newY) {
        if (this.mTranslator != null) {
            PointF point = new PointF(newX, newY);
            this.mTranslator.translatePointInScreenToAppWindow(point);
            newX = (int) (((double) point.x) + 0.5d);
            newY = (int) (((double) point.y) + 0.5d);
        }
        Message msg = this.mHandler.obtainMessage(24, newX, newY);
        this.mHandler.sendMessage(msg);
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
            return (this.mEvent instanceof MotionEvent) && this.mEvent.isFromSource(2);
        }

        public boolean shouldSendToSynthesizer() {
            return (this.mFlags & 32) != 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("QueuedInputEvent{flags=");
            boolean hasPrevious = flagToString("DELIVER_POST_IME", 1, false, sb);
            if (!flagToString("UNHANDLED", 32, flagToString("RESYNTHESIZED", 16, flagToString("FINISHED_HANDLED", 8, flagToString("FINISHED", 4, flagToString("DEFERRED", 2, hasPrevious, sb), sb), sb), sb), sb)) {
                sb.append(WifiEnterpriseConfig.ENGINE_DISABLE);
            }
            sb.append(", hasNextQueuedEvent=" + (this.mEvent != null ? "true" : "false"));
            sb.append(", hasInputEventReceiver=" + (this.mReceiver != null ? "true" : "false"));
            sb.append(", mEvent=" + this.mEvent + "}");
            return sb.toString();
        }

        private boolean flagToString(String name, int flag, boolean hasPrevious, StringBuilder sb) {
            if ((this.mFlags & flag) != 0) {
                if (hasPrevious) {
                    sb.append("|");
                }
                sb.append(name);
                return true;
            }
            return hasPrevious;
        }
    }

    private QueuedInputEvent obtainQueuedInputEvent(InputEvent event, InputEventReceiver receiver, int flags) {
        QueuedInputEvent q = this.mQueuedInputEventPool;
        if (q != null) {
            this.mQueuedInputEventPoolSize--;
            this.mQueuedInputEventPool = q.mNext;
            q.mNext = null;
        } else {
            q = new QueuedInputEvent();
        }
        q.mEvent = event;
        q.mReceiver = receiver;
        q.mFlags = flags;
        return q;
    }

    private void recycleQueuedInputEvent(QueuedInputEvent q) {
        q.mEvent = null;
        q.mReceiver = null;
        if (this.mQueuedInputEventPoolSize < 10) {
            this.mQueuedInputEventPoolSize++;
            q.mNext = this.mQueuedInputEventPool;
            this.mQueuedInputEventPool = q;
        }
    }

    void enqueueInputEvent(InputEvent event) {
        enqueueInputEvent(event, null, 0, false);
    }

    void enqueueInputEvent(InputEvent event, InputEventReceiver receiver, int flags, boolean processImmediately) {
        QueuedInputEvent q = obtainQueuedInputEvent(event, receiver, flags);
        QueuedInputEvent last = this.mPendingInputEventTail;
        if (last == null) {
            this.mPendingInputEventHead = q;
            this.mPendingInputEventTail = q;
        } else {
            last.mNext = q;
            this.mPendingInputEventTail = q;
        }
        this.mPendingInputEventCount++;
        Trace.traceCounter(4L, this.mPendingInputEventQueueLengthCounterName, this.mPendingInputEventCount);
        if (processImmediately) {
            doProcessInputEvents();
        } else {
            scheduleProcessInputEvents();
        }
    }

    private void scheduleProcessInputEvents() {
        if (!this.mProcessInputEventsScheduled) {
            this.mProcessInputEventsScheduled = true;
            Message msg = this.mHandler.obtainMessage(19);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    void doProcessInputEvents() {
        while (this.mPendingInputEventHead != null) {
            QueuedInputEvent q = this.mPendingInputEventHead;
            this.mPendingInputEventHead = q.mNext;
            if (this.mPendingInputEventHead == null) {
                this.mPendingInputEventTail = null;
            }
            q.mNext = null;
            this.mPendingInputEventCount--;
            Trace.traceCounter(4L, this.mPendingInputEventQueueLengthCounterName, this.mPendingInputEventCount);
            deliverInputEvent(q);
        }
        if (this.mProcessInputEventsScheduled) {
            this.mProcessInputEventsScheduled = false;
            this.mHandler.removeMessages(19);
        }
    }

    private void deliverInputEvent(QueuedInputEvent q) {
        InputStage stage;
        Trace.asyncTraceBegin(8L, "deliverInputEvent", q.mEvent.getSequenceNumber());
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onInputEvent(q.mEvent, 0);
        }
        if (q.shouldSendToSynthesizer()) {
            stage = this.mSyntheticInputStage;
        } else {
            stage = q.shouldSkipIme() ? this.mFirstPostImeInputStage : this.mFirstInputStage;
        }
        if (stage != null) {
            stage.deliver(q);
        } else {
            finishInputEvent(q);
        }
    }

    private void finishInputEvent(QueuedInputEvent q) {
        Trace.asyncTraceEnd(8L, "deliverInputEvent", q.mEvent.getSequenceNumber());
        if (q.mReceiver != null) {
            boolean handled = (q.mFlags & 8) != 0;
            q.mReceiver.finishInputEvent(q.mEvent, handled);
        } else {
            q.mEvent.recycleIfNeededAfterDispatch();
        }
        recycleQueuedInputEvent(q);
    }

    static boolean isTerminalInputEvent(InputEvent event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            return keyEvent.getAction() == 1;
        }
        MotionEvent motionEvent = (MotionEvent) event;
        int action = motionEvent.getAction();
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

    void doConsumeBatchedInput(long frameTimeNanos) {
        if (this.mConsumeBatchedInputScheduled) {
            this.mConsumeBatchedInputScheduled = false;
            if (this.mInputEventReceiver != null && this.mInputEventReceiver.consumeBatchedInputEvents(frameTimeNanos) && frameTimeNanos != -1) {
                scheduleConsumeBatchedInput();
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
        public void onInputEvent(InputEvent event) {
            ViewRootImpl.this.enqueueInputEvent(event, this, 0, true);
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

        public void addViewRect(View.AttachInfo.InvalidateInfo info) {
            synchronized (this) {
                this.mViewRects.add(info);
                postIfNeededLocked();
            }
        }

        public void removeView(View view) {
            synchronized (this) {
                this.mViews.remove(view);
                int i = this.mViewRects.size();
                int i2 = i;
                while (true) {
                    int i3 = i2 - 1;
                    if (i2 <= 0) {
                        break;
                    }
                    View.AttachInfo.InvalidateInfo info = this.mViewRects.get(i3);
                    if (info.target == view) {
                        this.mViewRects.remove(i3);
                        info.recycle();
                    }
                    i2 = i3;
                }
                if (this.mPosted && this.mViews.isEmpty() && this.mViewRects.isEmpty()) {
                    ViewRootImpl.this.mChoreographer.removeCallbacks(1, this, null);
                    this.mPosted = false;
                }
            }
        }

        @Override
        public void run() {
            int viewCount;
            int viewRectCount;
            synchronized (this) {
                this.mPosted = false;
                viewCount = this.mViews.size();
                if (viewCount != 0) {
                    this.mTempViews = (View[]) this.mViews.toArray(this.mTempViews != null ? this.mTempViews : new View[viewCount]);
                    this.mViews.clear();
                }
                viewRectCount = this.mViewRects.size();
                if (viewRectCount != 0) {
                    this.mTempViewRects = (View.AttachInfo.InvalidateInfo[]) this.mViewRects.toArray(this.mTempViewRects != null ? this.mTempViewRects : new View.AttachInfo.InvalidateInfo[viewRectCount]);
                    this.mViewRects.clear();
                }
            }
            for (int i = 0; i < viewCount; i++) {
                this.mTempViews[i].invalidate();
                this.mTempViews[i] = null;
            }
            for (int i2 = 0; i2 < viewRectCount; i2++) {
                View.AttachInfo.InvalidateInfo info = this.mTempViewRects[i2];
                info.target.invalidate(info.left, info.top, info.right, info.bottom);
                info.recycle();
            }
        }

        private void postIfNeededLocked() {
            if (!this.mPosted) {
                ViewRootImpl.this.mChoreographer.postCallback(1, this, null);
                this.mPosted = true;
            }
        }
    }

    public void dispatchInvalidateDelayed(View view, long delayMilliseconds) {
        Message msg = this.mHandler.obtainMessage(1, view);
        this.mHandler.sendMessageDelayed(msg, delayMilliseconds);
    }

    public void dispatchInvalidateRectDelayed(View.AttachInfo.InvalidateInfo info, long delayMilliseconds) {
        Message msg = this.mHandler.obtainMessage(2, info);
        this.mHandler.sendMessageDelayed(msg, delayMilliseconds);
    }

    public void dispatchInvalidateOnAnimation(View view) {
        this.mInvalidateOnAnimationRunnable.addView(view);
    }

    public void dispatchInvalidateRectOnAnimation(View.AttachInfo.InvalidateInfo info) {
        this.mInvalidateOnAnimationRunnable.addViewRect(info);
    }

    public void cancelInvalidate(View view) {
        this.mHandler.removeMessages(1, view);
        this.mHandler.removeMessages(2, view);
        this.mInvalidateOnAnimationRunnable.removeView(view);
    }

    public void dispatchInputEvent(InputEvent event) {
        dispatchInputEvent(event, null);
    }

    public void dispatchInputEvent(InputEvent event, InputEventReceiver receiver) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = event;
        args.arg2 = receiver;
        Message msg = this.mHandler.obtainMessage(7, args);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void synthesizeInputEvent(InputEvent event) {
        Message msg = this.mHandler.obtainMessage(25, event);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void dispatchKeyFromIme(KeyEvent event) {
        Message msg = this.mHandler.obtainMessage(11, event);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void dispatchUnhandledInputEvent(InputEvent event) {
        if (event instanceof MotionEvent) {
            event = MotionEvent.obtain((MotionEvent) event);
        }
        synthesizeInputEvent(event);
    }

    public void dispatchAppVisibility(boolean visible) {
        Message msg = this.mHandler.obtainMessage(8);
        msg.arg1 = visible ? 1 : 0;
        this.mHandler.sendMessage(msg);
    }

    public void dispatchGetNewSurface() {
        Message msg = this.mHandler.obtainMessage(9);
        this.mHandler.sendMessage(msg);
    }

    public void windowFocusChanged(boolean hasFocus, boolean inTouchMode) {
        Message msg = Message.obtain();
        msg.what = 6;
        msg.arg1 = hasFocus ? 1 : 0;
        msg.arg2 = inTouchMode ? 1 : 0;
        this.mHandler.sendMessage(msg);
    }

    public void dispatchWindowShown() {
        this.mHandler.sendEmptyMessage(26);
    }

    public void dispatchCloseSystemDialogs(String reason) {
        Message msg = Message.obtain();
        msg.what = 14;
        msg.obj = reason;
        this.mHandler.sendMessage(msg);
    }

    public void dispatchDragEvent(DragEvent event) {
        int what;
        if (event.getAction() == 2) {
            what = 16;
            this.mHandler.removeMessages(16);
        } else {
            what = 15;
        }
        Message msg = this.mHandler.obtainMessage(what, event);
        this.mHandler.sendMessage(msg);
    }

    public void dispatchSystemUiVisibilityChanged(int seq, int globalVisibility, int localValue, int localChanges) {
        SystemUiVisibilityInfo args = new SystemUiVisibilityInfo();
        args.seq = seq;
        args.globalVisibility = globalVisibility;
        args.localValue = localValue;
        args.localChanges = localChanges;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(17, args));
    }

    public void dispatchDoneAnimating() {
        this.mHandler.sendEmptyMessage(22);
    }

    public void dispatchCheckFocus() {
        if (!this.mHandler.hasMessages(13)) {
            this.mHandler.sendEmptyMessage(13);
        }
    }

    private void postSendWindowContentChangedCallback(View source, int changeType) {
        if (this.mSendWindowContentChangedAccessibilityEvent == null) {
            this.mSendWindowContentChangedAccessibilityEvent = new SendWindowContentChangedAccessibilityEvent();
        }
        this.mSendWindowContentChangedAccessibilityEvent.runOrPost(source, changeType);
    }

    private void removeSendWindowContentChangedCallback() {
        if (this.mSendWindowContentChangedAccessibilityEvent != null) {
            this.mHandler.removeCallbacks(this.mSendWindowContentChangedAccessibilityEvent);
        }
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        return false;
    }

    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
        return null;
    }

    @Override
    public void createContextMenu(ContextMenu menu) {
    }

    @Override
    public void childDrawableStateChanged(View child) {
    }

    @Override
    public boolean requestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        AccessibilityNodeProvider provider;
        AccessibilityNodeProvider provider2;
        AccessibilityNodeInfo node;
        if (this.mView == null) {
            return false;
        }
        int eventType = event.getEventType();
        switch (eventType) {
            case 2048:
                if (this.mAccessibilityFocusedHost != null && this.mAccessibilityFocusedVirtualView != null) {
                    long eventSourceId = event.getSourceNodeId();
                    int hostViewId = AccessibilityNodeInfo.getAccessibilityViewId(eventSourceId);
                    if (hostViewId == this.mAccessibilityFocusedHost.getAccessibilityViewId()) {
                        int changes = event.getContentChangeTypes();
                        if (((changes & 1) != 0 || changes == 0) && (provider = this.mAccessibilityFocusedHost.getAccessibilityNodeProvider()) != null) {
                            int virtualChildId = AccessibilityNodeInfo.getVirtualDescendantId(this.mAccessibilityFocusedVirtualView.getSourceNodeId());
                            if (virtualChildId == Integer.MAX_VALUE) {
                                this.mAccessibilityFocusedVirtualView = provider.createAccessibilityNodeInfo(-1);
                            } else {
                                this.mAccessibilityFocusedVirtualView = provider.createAccessibilityNodeInfo(virtualChildId);
                            }
                        }
                    }
                }
                break;
            case 32768:
                long sourceNodeId = event.getSourceNodeId();
                int accessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(sourceNodeId);
                View source = this.mView.findViewByAccessibilityId(accessibilityViewId);
                if (source != null && (provider2 = source.getAccessibilityNodeProvider()) != null) {
                    int virtualNodeId = AccessibilityNodeInfo.getVirtualDescendantId(sourceNodeId);
                    if (virtualNodeId == Integer.MAX_VALUE) {
                        node = provider2.createAccessibilityNodeInfo(-1);
                    } else {
                        node = provider2.createAccessibilityNodeInfo(virtualNodeId);
                    }
                    setAccessibilityFocus(source, node);
                }
                break;
            case 65536:
                int accessibilityViewId2 = AccessibilityNodeInfo.getAccessibilityViewId(event.getSourceNodeId());
                View source2 = this.mView.findViewByAccessibilityId(accessibilityViewId2);
                if (source2 != null && source2.getAccessibilityNodeProvider() != null) {
                    setAccessibilityFocus(null, null);
                }
                break;
        }
        this.mAccessibilityManager.sendAccessibilityEvent(event);
        return true;
    }

    @Override
    public void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType) {
        postSendWindowContentChangedCallback(source, changeType);
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

    private View getCommonPredecessor(View first, View second) {
        if (this.mTempHashSet == null) {
            this.mTempHashSet = new HashSet<>();
        }
        HashSet<View> seen = this.mTempHashSet;
        seen.clear();
        View firstCurrent = first;
        while (firstCurrent != null) {
            seen.add(firstCurrent);
            Object obj = firstCurrent.mParent;
            if (obj instanceof View) {
                firstCurrent = (View) obj;
            } else {
                firstCurrent = null;
            }
        }
        View secondCurrent = second;
        while (secondCurrent != null) {
            if (seen.contains(secondCurrent)) {
                seen.clear();
                return secondCurrent;
            }
            Object obj2 = secondCurrent.mParent;
            if (obj2 instanceof View) {
                secondCurrent = (View) obj2;
            } else {
                secondCurrent = null;
            }
        }
        seen.clear();
        return null;
    }

    void checkThread() {
        if (this.mThread != Thread.currentThread()) {
            throw new CalledFromWrongThreadException("Only the original thread that created a view hierarchy can touch its views.");
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        boolean scrolled = scrollToRectOrFocus(rectangle, immediate);
        if (rectangle != null) {
            this.mTempRect.set(rectangle);
            this.mTempRect.offset(0, -this.mCurScrollY);
            this.mTempRect.offset(this.mAttachInfo.mWindowLeft, this.mAttachInfo.mWindowTop);
            try {
                this.mWindowSession.onRectangleOnScreenRequested(this.mWindow, this.mTempRect);
            } catch (RemoteException e) {
            }
        }
        return scrolled;
    }

    @Override
    public void childHasTransientStateChanged(View child, boolean hasTransientState) {
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return false;
    }

    @Override
    public void onStopNestedScroll(View target) {
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onNestedPrePerformAccessibilityAction(View target, int action, Bundle args) {
        return false;
    }

    void changeCanvasOpacity(boolean opaque) {
        Log.d(TAG, "changeCanvasOpacity: opaque=" + opaque);
        if (this.mAttachInfo.mHardwareRenderer != null) {
            this.mAttachInfo.mHardwareRenderer.setOpaque(opaque);
        }
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
        public void setFormat(int format) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceFormat(format);
        }

        @Override
        public void setType(int type) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceType(type);
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
        public void setFixedSize(int width, int height) {
            throw new UnsupportedOperationException("Currently only support sizing from layout");
        }

        @Override
        public void setKeepScreenOn(boolean screenOn) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceKeepScreenOn(screenOn);
        }
    }

    static class W extends IWindow.Stub {
        private final WeakReference<ViewRootImpl> mViewAncestor;
        private final IWindowSession mWindowSession;

        W(ViewRootImpl viewAncestor) {
            this.mViewAncestor = new WeakReference<>(viewAncestor);
            this.mWindowSession = viewAncestor.mWindowSession;
        }

        @Override
        public void resized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, boolean reportDraw, Configuration newConfig) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchResized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, reportDraw, newConfig);
            }
        }

        @Override
        public void moved(int newX, int newY) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchMoved(newX, newY);
            }
        }

        @Override
        public void dispatchAppVisibility(boolean visible) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchAppVisibility(visible);
            }
        }

        @Override
        public void dispatchGetNewSurface() {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchGetNewSurface();
            }
        }

        @Override
        public void windowFocusChanged(boolean hasFocus, boolean inTouchMode) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.windowFocusChanged(hasFocus, inTouchMode);
            }
        }

        private static int checkCallingPermission(String permission) {
            try {
                return ActivityManagerNative.getDefault().checkPermission(permission, Binder.getCallingPid(), Binder.getCallingUid());
            } catch (RemoteException e) {
                return -1;
            }
        }

        @Override
        public void executeCommand(String command, String parameters, ParcelFileDescriptor out) throws Throwable {
            View view;
            OutputStream clientStream;
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor == null || (view = viewAncestor.mView) == null) {
                return;
            }
            if (checkCallingPermission(Manifest.permission.DUMP) != 0) {
                throw new SecurityException("Insufficient permissions to invoke executeCommand() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            }
            OutputStream clientStream2 = null;
            try {
                try {
                    clientStream = new ParcelFileDescriptor.AutoCloseOutputStream(out);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e) {
                e = e;
            }
            try {
                ViewDebug.dispatchCommand(view, command, parameters, clientStream);
                if (clientStream != null) {
                    try {
                        clientStream.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            } catch (IOException e3) {
                e = e3;
                clientStream2 = clientStream;
                e.printStackTrace();
                if (clientStream2 != null) {
                    try {
                        clientStream2.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                clientStream2 = clientStream;
                if (clientStream2 != null) {
                    try {
                        clientStream2.close();
                    } catch (IOException e5) {
                        e5.printStackTrace();
                    }
                }
                throw th;
            }
        }

        @Override
        public void closeSystemDialogs(String reason) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchCloseSystemDialogs(reason);
            }
        }

        @Override
        public void dispatchWallpaperOffsets(float x, float y, float xStep, float yStep, boolean sync) {
            if (sync) {
                try {
                    this.mWindowSession.wallpaperOffsetsComplete(asBinder());
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras, boolean sync) {
            if (sync) {
                try {
                    this.mWindowSession.wallpaperCommandComplete(asBinder(), null);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void dispatchDragEvent(DragEvent event) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchDragEvent(event);
            }
        }

        @Override
        public void dispatchSystemUiVisibilityChanged(int seq, int globalVisibility, int localValue, int localChanges) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchSystemUiVisibilityChanged(seq, globalVisibility, localValue, localChanges);
            }
        }

        @Override
        public void doneAnimating() {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchDoneAnimating();
            }
        }

        @Override
        public void dispatchWindowShown() {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchWindowShown();
            }
        }
    }

    public static final class CalledFromWrongThreadException extends AndroidRuntimeException {
        public CalledFromWrongThreadException(String msg) {
            super(msg);
        }
    }

    static RunQueue getRunQueue() {
        RunQueue rq = sRunQueues.get();
        if (rq == null) {
            RunQueue rq2 = new RunQueue();
            sRunQueues.set(rq2);
            return rq2;
        }
        return rq;
    }

    static final class RunQueue {
        private final ArrayList<HandlerAction> mActions = new ArrayList<>();

        RunQueue() {
        }

        void post(Runnable action) {
            postDelayed(action, 0L);
        }

        void postDelayed(Runnable action, long delayMillis) {
            HandlerAction handlerAction = new HandlerAction();
            handlerAction.action = action;
            handlerAction.delay = delayMillis;
            synchronized (this.mActions) {
                this.mActions.add(handlerAction);
            }
        }

        void removeCallbacks(Runnable action) {
            HandlerAction handlerAction = new HandlerAction();
            handlerAction.action = action;
            synchronized (this.mActions) {
                ArrayList<HandlerAction> actions = this.mActions;
                while (actions.remove(handlerAction)) {
                }
            }
        }

        void executeActions(Handler handler) {
            synchronized (this.mActions) {
                ArrayList<HandlerAction> actions = this.mActions;
                int count = actions.size();
                for (int i = 0; i < count; i++) {
                    HandlerAction handlerAction = actions.get(i);
                    handler.postDelayed(handlerAction.action, handlerAction.delay);
                }
                actions.clear();
            }
        }

        private static class HandlerAction {
            Runnable action;
            long delay;

            private HandlerAction() {
            }

            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                HandlerAction that = (HandlerAction) o;
                if (this.action != null) {
                    if (this.action.equals(that.action)) {
                        return true;
                    }
                } else if (that.action == null) {
                    return true;
                }
                return false;
            }

            public int hashCode() {
                int result = this.action != null ? this.action.hashCode() : 0;
                return (result * 31) + ((int) (this.delay ^ (this.delay >>> 32)));
            }
        }
    }

    final class AccessibilityInteractionConnectionManager implements AccessibilityManager.AccessibilityStateChangeListener {
        AccessibilityInteractionConnectionManager() {
        }

        @Override
        public void onAccessibilityStateChanged(boolean enabled) {
            if (enabled) {
                ensureConnection();
                if (ViewRootImpl.this.mAttachInfo.mHasWindowFocus) {
                    ViewRootImpl.this.mView.sendAccessibilityEvent(32);
                    View focusedView = ViewRootImpl.this.mView.findFocus();
                    if (focusedView != null && focusedView != ViewRootImpl.this.mView) {
                        focusedView.sendAccessibilityEvent(8);
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
            boolean registered = ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId != Integer.MAX_VALUE;
            if (!registered) {
                ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId = ViewRootImpl.this.mAccessibilityManager.addAccessibilityInteractionConnection(ViewRootImpl.this.mWindow, new AccessibilityInteractionConnection(ViewRootImpl.this));
            }
        }

        public void ensureNoConnection() {
            boolean registered = ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId != Integer.MAX_VALUE;
            if (registered) {
                ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId = Integer.MAX_VALUE;
                ViewRootImpl.this.mAccessibilityManager.removeAccessibilityInteractionConnection(ViewRootImpl.this.mWindow);
            }
        }
    }

    final class HighContrastTextManager implements AccessibilityManager.HighTextContrastChangeListener {
        HighContrastTextManager() {
            ViewRootImpl.this.mAttachInfo.mHighContrastText = ViewRootImpl.this.mAccessibilityManager.isHighTextContrastEnabled();
        }

        @Override
        public void onHighTextContrastStateChanged(boolean enabled) {
            ViewRootImpl.this.mAttachInfo.mHighContrastText = enabled;
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
        public void findAccessibilityNodeInfoByAccessibilityId(long accessibilityNodeId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfoByAccessibilityIdClientThread(accessibilityNodeId, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec);
            } else {
                try {
                    callback.setFindAccessibilityNodeInfosResult(null, interactionId);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void performAccessibilityAction(long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().performAccessibilityActionClientThread(accessibilityNodeId, action, arguments, interactionId, callback, flags, interrogatingPid, interrogatingTid);
            } else {
                try {
                    callback.setPerformAccessibilityActionResult(false, interactionId);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void findAccessibilityNodeInfosByViewId(long accessibilityNodeId, String viewId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfosByViewIdClientThread(accessibilityNodeId, viewId, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec);
            } else {
                try {
                    callback.setFindAccessibilityNodeInfoResult(null, interactionId);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void findAccessibilityNodeInfosByText(long accessibilityNodeId, String text, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfosByTextClientThread(accessibilityNodeId, text, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec);
            } else {
                try {
                    callback.setFindAccessibilityNodeInfosResult(null, interactionId);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void findFocus(long accessibilityNodeId, int focusType, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findFocusClientThread(accessibilityNodeId, focusType, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec);
            } else {
                try {
                    callback.setFindAccessibilityNodeInfoResult(null, interactionId);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void focusSearch(long accessibilityNodeId, int direction, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().focusSearchClientThread(accessibilityNodeId, direction, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec);
            } else {
                try {
                    callback.setFindAccessibilityNodeInfoResult(null, interactionId);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private class SendWindowContentChangedAccessibilityEvent implements Runnable {
        private int mChangeTypes;
        public long mLastEventTimeMillis;
        public View mSource;

        private SendWindowContentChangedAccessibilityEvent() {
            this.mChangeTypes = 0;
        }

        @Override
        public void run() {
            if (AccessibilityManager.getInstance(ViewRootImpl.this.mContext).isEnabled()) {
                this.mLastEventTimeMillis = SystemClock.uptimeMillis();
                AccessibilityEvent event = AccessibilityEvent.obtain();
                event.setEventType(2048);
                event.setContentChangeTypes(this.mChangeTypes);
                this.mSource.sendAccessibilityEventUnchecked(event);
            } else {
                this.mLastEventTimeMillis = 0L;
            }
            this.mSource.resetSubtreeAccessibilityStateChanged();
            this.mSource = null;
            this.mChangeTypes = 0;
        }

        public void runOrPost(View source, int changeType) {
            if (this.mSource != null) {
                View predecessor = ViewRootImpl.this.getCommonPredecessor(this.mSource, source);
                if (predecessor == null) {
                    predecessor = source;
                }
                this.mSource = predecessor;
                this.mChangeTypes |= changeType;
                return;
            }
            this.mSource = source;
            this.mChangeTypes = changeType;
            long timeSinceLastMillis = SystemClock.uptimeMillis() - this.mLastEventTimeMillis;
            long minEventIntevalMillis = ViewConfiguration.getSendRecurringAccessibilityEventsInterval();
            if (timeSinceLastMillis >= minEventIntevalMillis) {
                this.mSource.removeCallbacks(this);
                run();
            } else {
                this.mSource.postDelayed(this, minEventIntevalMillis - timeSinceLastMillis);
            }
        }
    }
}
