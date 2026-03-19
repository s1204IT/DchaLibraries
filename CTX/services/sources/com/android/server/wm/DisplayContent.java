package com.android.server.wm;

import android.R;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.hardware.display.DisplayManagerInternal;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.InputDevice;
import android.view.MagnificationSpec;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ToBooleanFunction;
import com.android.internal.view.IInputMethodClient;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.wm.DisplayContent;
import com.android.server.wm.WindowContainer;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.utils.CoordinateTransforms;
import com.android.server.wm.utils.RotationCache;
import com.android.server.wm.utils.WmDisplayCutout;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

class DisplayContent extends WindowContainer<DisplayChildWindowContainer> {
    private static final String TAG = "WindowManager";
    boolean isDefaultDisplay;
    private final AboveAppWindowContainers mAboveAppWindowsContainers;
    private boolean mAltOrientation;
    private final Consumer<WindowState> mApplyPostLayoutPolicy;
    private final Consumer<WindowState> mApplySurfaceChangesTransaction;
    int mBaseDisplayDensity;
    int mBaseDisplayHeight;
    private Rect mBaseDisplayRect;
    int mBaseDisplayWidth;
    private final NonAppWindowContainers mBelowAppWindowsContainers;
    private final DisplayMetrics mCompatDisplayMetrics;
    float mCompatibleScreenScale;
    private final Predicate<WindowState> mComputeImeTargetPredicate;
    private int mDeferUpdateImeTargetCount;
    private boolean mDeferredRemoval;
    private final Display mDisplay;
    private final RotationCache<DisplayCutout, WmDisplayCutout> mDisplayCutoutCache;
    DisplayFrames mDisplayFrames;
    private final int mDisplayId;
    private final DisplayInfo mDisplayInfo;
    private final DisplayMetrics mDisplayMetrics;
    private boolean mDisplayReady;
    boolean mDisplayScalingDisabled;
    final DockedStackDividerController mDividerControllerLocked;
    final ArrayList<WindowToken> mExitingTokens;
    private final ToBooleanFunction<WindowState> mFindFocusedWindow;
    private boolean mHaveApp;
    private boolean mHaveBootMsg;
    private boolean mHaveKeyguard;
    private boolean mHaveWallpaper;
    private final NonMagnifiableWindowContainers mImeWindowsContainers;
    DisplayCutout mInitialDisplayCutout;
    int mInitialDisplayDensity;
    int mInitialDisplayHeight;
    int mInitialDisplayWidth;
    private int mLastKeyguardForcedOrientation;
    private int mLastOrientation;
    private boolean mLastWallpaperVisible;
    private int mLastWindowForcedOrientation;
    private boolean mLayoutNeeded;
    int mLayoutSeq;
    private MagnificationSpec mMagnificationSpec;
    private int mMaxUiWidth;
    private SurfaceControl mOverlayLayer;
    private final Consumer<WindowState> mPerformLayout;
    private final Consumer<WindowState> mPerformLayoutAttached;
    final PinnedStackController mPinnedStackControllerLocked;
    final DisplayMetrics mRealDisplayMetrics;
    private boolean mRemovingDisplay;
    private int mRotation;
    private final Consumer<WindowState> mScheduleToastTimeout;
    private final SurfaceSession mSession;
    boolean mShouldOverrideDisplayConfiguration;
    private int mSurfaceSize;
    TaskTapPointerEventListener mTapDetector;
    final ArraySet<WindowState> mTapExcludeProvidingWindows;
    final ArrayList<WindowState> mTapExcludedWindows;
    private final TaskStackContainers mTaskStackContainers;
    private final ApplySurfaceChangesTransactionState mTmpApplySurfaceChangesTransactionState;
    private final Rect mTmpBounds;
    private final DisplayMetrics mTmpDisplayMetrics;
    private final float[] mTmpFloats;
    private boolean mTmpInitial;
    private final Matrix mTmpMatrix;
    private boolean mTmpRecoveringMemory;
    private final Rect mTmpRect;
    private final Rect mTmpRect2;
    private final RectF mTmpRectF;
    private final Region mTmpRegion;
    private final TaskForResizePointSearchResult mTmpTaskForResizePointSearchResult;
    private final LinkedList<AppWindowToken> mTmpUpdateAllDrawn;
    private WindowState mTmpWindow;
    private WindowState mTmpWindow2;
    private WindowAnimator mTmpWindowAnimator;
    private final HashMap<IBinder, WindowToken> mTokenMap;
    private Region mTouchExcludeRegion;
    private boolean mUpdateImeTarget;
    private final Consumer<WindowState> mUpdateWallpaperForAnimator;
    private final Consumer<WindowState> mUpdateWindowsForAnimator;
    WallpaperController mWallpaperController;
    private SurfaceControl mWindowingLayer;
    private WmsExt mWmsExt;
    int pendingLayoutChanges;

    @Override
    protected void addChild(WindowContainer windowContainer, Comparator comparator) {
        addChild((DisplayChildWindowContainer) windowContainer, (Comparator<DisplayChildWindowContainer>) comparator);
    }

    public static void lambda$new$0(DisplayContent displayContent, WindowState windowState) {
        WindowStateAnimator windowStateAnimator = windowState.mWinAnimator;
        AppWindowToken appWindowToken = windowState.mAppToken;
        if (windowStateAnimator.mDrawState == 3) {
            if ((appWindowToken == null || appWindowToken.allDrawn) && windowState.performShowLocked()) {
                displayContent.pendingLayoutChanges |= 8;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    displayContent.mService.mWindowPlacerLocked.debugLayoutRepeats("updateWindowsAndWallpaperLocked 5", displayContent.pendingLayoutChanges);
                }
            }
        }
    }

    public static void lambda$new$1(DisplayContent displayContent, WindowState windowState) {
        TaskStack stack;
        AnimationAdapter animation;
        TaskStack stack2;
        WindowStateAnimator windowStateAnimator = windowState.mWinAnimator;
        if (windowStateAnimator.mSurfaceController == null || !windowStateAnimator.hasSurface()) {
            return;
        }
        int i = windowState.mAttrs.flags;
        if (windowStateAnimator.isAnimationSet() && (animation = windowState.getAnimation()) != null) {
            if ((i & DumpState.DUMP_DEXOPT) != 0 && animation.getDetachWallpaper()) {
                displayContent.mTmpWindow = windowState;
            }
            int backgroundColor = animation.getBackgroundColor();
            if (backgroundColor != 0 && (stack2 = windowState.getStack()) != null) {
                stack2.setAnimationBackground(windowStateAnimator, backgroundColor);
            }
        }
        AppWindowToken appWindowToken = windowStateAnimator.mWin.mAppToken;
        AnimationAdapter animation2 = appWindowToken != null ? appWindowToken.getAnimation() : null;
        if (animation2 != null) {
            if ((i & DumpState.DUMP_DEXOPT) != 0 && animation2.getDetachWallpaper()) {
                displayContent.mTmpWindow = windowState;
            }
            int backgroundColor2 = animation2.getBackgroundColor();
            if (backgroundColor2 != 0 && (stack = windowState.getStack()) != null) {
                stack.setAnimationBackground(windowStateAnimator, backgroundColor2);
            }
        }
    }

    public static void lambda$new$2(DisplayContent displayContent, WindowState windowState) {
        int i = displayContent.mTmpWindow.mOwnerUid;
        WindowManagerService.H h = displayContent.mService.mH;
        if (windowState.mAttrs.type == 2005 && windowState.mOwnerUid == i && !h.hasMessages(52, windowState)) {
            h.sendMessageDelayed(h.obtainMessage(52, windowState), windowState.mAttrs.hideTimeoutMilliseconds);
        }
    }

    public static boolean lambda$new$3(DisplayContent displayContent, WindowState windowState) {
        AppWindowToken appWindowToken = displayContent.mService.mFocusedApp;
        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
            Slog.v("WindowManager", "Looking for focus: " + windowState + ", flags=" + windowState.mAttrs.flags + ", canReceive=" + windowState.canReceiveKeys());
        }
        if (!windowState.canReceiveKeys()) {
            return false;
        }
        AppWindowToken appWindowToken2 = windowState.mAppToken;
        if (appWindowToken2 != null && (appWindowToken2.removed || appWindowToken2.sendingToBottom)) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("Skipping ");
                sb.append(appWindowToken2);
                sb.append(" because ");
                sb.append(appWindowToken2.removed ? "removed" : "sendingToBottom");
                Slog.v("WindowManager", sb.toString());
            }
            return false;
        }
        if (appWindowToken == null) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                Slog.v("WindowManager", "findFocusedWindow: focusedApp=null using new focus @ " + windowState);
            }
            displayContent.mTmpWindow = windowState;
            return true;
        }
        if (!appWindowToken.windowsAreFocusable()) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                Slog.v("WindowManager", "findFocusedWindow: focusedApp windows not focusable using new focus @ " + windowState);
            }
            displayContent.mTmpWindow = windowState;
            return true;
        }
        if (appWindowToken2 != null && windowState.mAttrs.type != 3 && appWindowToken.compareTo((WindowContainer) appWindowToken2) > 0) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                Slog.v("WindowManager", "findFocusedWindow: Reached focused app=" + appWindowToken);
            }
            displayContent.mTmpWindow = null;
            return true;
        }
        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
            Slog.v("WindowManager", "findFocusedWindow: Found new focus @ " + windowState);
        }
        displayContent.mTmpWindow = windowState;
        return true;
    }

    public static void lambda$new$4(DisplayContent displayContent, WindowState windowState) {
        boolean z = (displayContent.mTmpWindow != null && displayContent.mService.mPolicy.canBeHiddenByKeyguardLw(windowState)) || windowState.isGoneForLayoutLw();
        if (WindowManagerDebugConfig.DEBUG_LAYOUT && !windowState.mLayoutAttached) {
            Slog.v("WindowManager", "1ST PASS " + windowState + ": gone=" + z + " mHaveFrame=" + windowState.mHaveFrame + " mLayoutAttached=" + windowState.mLayoutAttached + " screen changed=" + windowState.isConfigChanged());
            AppWindowToken appWindowToken = windowState.mAppToken;
            if (z) {
                StringBuilder sb = new StringBuilder();
                sb.append("  GONE: mViewVisibility=");
                sb.append(windowState.mViewVisibility);
                sb.append(" mRelayoutCalled=");
                sb.append(windowState.mRelayoutCalled);
                sb.append(" hidden=");
                sb.append(windowState.mToken.isHidden());
                sb.append(" hiddenRequested=");
                sb.append(appWindowToken != null && appWindowToken.hiddenRequested);
                sb.append(" parentHidden=");
                sb.append(windowState.isParentWindowHidden());
                Slog.v("WindowManager", sb.toString());
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("  VIS: mViewVisibility=");
                sb2.append(windowState.mViewVisibility);
                sb2.append(" mRelayoutCalled=");
                sb2.append(windowState.mRelayoutCalled);
                sb2.append(" hidden=");
                sb2.append(windowState.mToken.isHidden());
                sb2.append(" hiddenRequested=");
                sb2.append(appWindowToken != null && appWindowToken.hiddenRequested);
                sb2.append(" parentHidden=");
                sb2.append(windowState.isParentWindowHidden());
                Slog.v("WindowManager", sb2.toString());
            }
        }
        if (z && windowState.mHaveFrame && !windowState.mLayoutNeeded) {
            if ((!windowState.isConfigChanged() && !windowState.setReportResizeHints()) || windowState.isGoneForLayoutLw()) {
                return;
            }
            if ((windowState.mAttrs.privateFlags & 1024) == 0 && (!windowState.mHasSurface || windowState.mAppToken == null || !windowState.mAppToken.layoutConfigChanges)) {
                return;
            }
        }
        if (!windowState.mLayoutAttached) {
            if (displayContent.mTmpInitial) {
                windowState.mContentChanged = false;
            }
            if (windowState.mAttrs.type == 2023) {
                displayContent.mTmpWindow = windowState;
            }
            windowState.mLayoutNeeded = false;
            windowState.prelayout();
            boolean z2 = !windowState.isLaidOut();
            displayContent.mService.mPolicy.layoutWindowLw(windowState, null, displayContent.mDisplayFrames);
            windowState.mLayoutSeq = displayContent.mLayoutSeq;
            if (z2) {
                windowState.updateLastInsetValues();
            }
            if (windowState.mAppToken != null) {
                windowState.mAppToken.layoutLetterbox(windowState);
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v("WindowManager", "  LAYOUT: mFrame=" + windowState.mFrame + " mContainingFrame=" + windowState.mContainingFrame + " mDisplayFrame=" + windowState.mDisplayFrame);
            }
        }
    }

    public static void lambda$new$5(DisplayContent displayContent, WindowState windowState) {
        if (windowState.mLayoutAttached) {
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v("WindowManager", "2ND PASS " + windowState + " mHaveFrame=" + windowState.mHaveFrame + " mViewVisibility=" + windowState.mViewVisibility + " mRelayoutCalled=" + windowState.mRelayoutCalled);
            }
            if (displayContent.mTmpWindow != null && displayContent.mService.mPolicy.canBeHiddenByKeyguardLw(windowState)) {
                return;
            }
            if ((windowState.mViewVisibility != 8 && windowState.mRelayoutCalled) || !windowState.mHaveFrame || windowState.mLayoutNeeded) {
                if (displayContent.mTmpInitial) {
                    windowState.mContentChanged = false;
                }
                windowState.mLayoutNeeded = false;
                windowState.prelayout();
                displayContent.mService.mPolicy.layoutWindowLw(windowState, windowState.getParentWindow(), displayContent.mDisplayFrames);
                windowState.mLayoutSeq = displayContent.mLayoutSeq;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    Slog.v("WindowManager", " LAYOUT: mFrame=" + windowState.mFrame + " mContainingFrame=" + windowState.mContainingFrame + " mDisplayFrame=" + windowState.mDisplayFrame);
                    return;
                }
                return;
            }
            return;
        }
        if (windowState.mAttrs.type == 2023) {
            displayContent.mTmpWindow = displayContent.mTmpWindow2;
        }
    }

    public static boolean lambda$new$6(DisplayContent displayContent, WindowState windowState) {
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD && displayContent.mUpdateImeTarget) {
            Slog.i("WindowManager", "Checking window @" + windowState + " fl=0x" + Integer.toHexString(windowState.mAttrs.flags));
        }
        return windowState.canBeImeTarget();
    }

    public static void lambda$new$8(DisplayContent displayContent, WindowState windowState) {
        WindowSurfacePlacer windowSurfacePlacer = displayContent.mService.mWindowPlacerLocked;
        boolean z = windowState.mObscured != displayContent.mTmpApplySurfaceChangesTransactionState.obscured;
        RootWindowContainer rootWindowContainer = displayContent.mService.mRoot;
        boolean z2 = !displayContent.mService.mLosingFocus.isEmpty();
        windowState.mObscured = displayContent.mTmpApplySurfaceChangesTransactionState.obscured;
        if (!displayContent.mTmpApplySurfaceChangesTransactionState.obscured) {
            boolean zIsDisplayedLw = windowState.isDisplayedLw();
            if (zIsDisplayedLw && windowState.isObscuringDisplay()) {
                rootWindowContainer.mObscuringWindow = windowState;
                displayContent.mTmpApplySurfaceChangesTransactionState.obscured = true;
            }
            displayContent.mTmpApplySurfaceChangesTransactionState.displayHasContent |= rootWindowContainer.handleNotObscuredLocked(windowState, displayContent.mTmpApplySurfaceChangesTransactionState.obscured, displayContent.mTmpApplySurfaceChangesTransactionState.syswin);
            if (windowState.mHasSurface && zIsDisplayedLw) {
                int i = windowState.mAttrs.type;
                if (i == 2008 || i == 2010 || (windowState.mAttrs.privateFlags & 1024) != 0) {
                    displayContent.mTmpApplySurfaceChangesTransactionState.syswin = true;
                }
                if (displayContent.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate == 0.0f && windowState.mAttrs.preferredRefreshRate != 0.0f) {
                    displayContent.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate = windowState.mAttrs.preferredRefreshRate;
                }
                if (displayContent.mTmpApplySurfaceChangesTransactionState.preferredModeId == 0 && windowState.mAttrs.preferredDisplayModeId != 0) {
                    displayContent.mTmpApplySurfaceChangesTransactionState.preferredModeId = windowState.mAttrs.preferredDisplayModeId;
                }
            }
        }
        if (displayContent.isDefaultDisplay && z && windowState.isVisibleLw() && displayContent.mWallpaperController.isWallpaperTarget(windowState)) {
            displayContent.mWallpaperController.updateWallpaperVisibility();
        }
        windowState.handleWindowMovedIfNeeded();
        WindowStateAnimator windowStateAnimator = windowState.mWinAnimator;
        windowState.mContentChanged = false;
        if (windowState.mHasSurface) {
            boolean zCommitFinishDrawingLocked = windowStateAnimator.commitFinishDrawingLocked();
            if (displayContent.isDefaultDisplay && zCommitFinishDrawingLocked) {
                if (windowState.mAttrs.type == 2023) {
                    displayContent.pendingLayoutChanges |= 1;
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                        windowSurfacePlacer.debugLayoutRepeats("dream and commitFinishDrawingLocked true", displayContent.pendingLayoutChanges);
                    }
                }
                if ((windowState.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
                    if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                        Slog.v("WindowManager", "First draw done in potential wallpaper target " + windowState);
                    }
                    rootWindowContainer.mWallpaperMayChange = true;
                    displayContent.pendingLayoutChanges |= 4;
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                        windowSurfacePlacer.debugLayoutRepeats("wallpaper and commitFinishDrawingLocked true", displayContent.pendingLayoutChanges);
                    }
                }
            }
        }
        AppWindowToken appWindowToken = windowState.mAppToken;
        if (appWindowToken != null) {
            appWindowToken.updateLetterboxSurface(windowState);
            if (appWindowToken.updateDrawnWindowStates(windowState) && !displayContent.mTmpUpdateAllDrawn.contains(appWindowToken)) {
                displayContent.mTmpUpdateAllDrawn.add(appWindowToken);
            }
        }
        if (displayContent.isDefaultDisplay && z2 && windowState == displayContent.mService.mCurrentFocus && windowState.isDisplayedLw()) {
            displayContent.mTmpApplySurfaceChangesTransactionState.focusDisplayed = true;
        }
        windowState.updateResizingWindowIfNeeded();
    }

    DisplayContent(Display display, WindowManagerService windowManagerService, WallpaperController wallpaperController, DisplayWindowController displayWindowController) {
        boolean z;
        super(windowManagerService);
        this.mTaskStackContainers = new TaskStackContainers(this.mService);
        this.mAboveAppWindowsContainers = new AboveAppWindowContainers("mAboveAppWindowsContainers", this.mService);
        this.mBelowAppWindowsContainers = new NonAppWindowContainers("mBelowAppWindowsContainers", this.mService);
        this.mImeWindowsContainers = new NonMagnifiableWindowContainers("mImeWindowsContainers", this.mService);
        this.mTokenMap = new HashMap<>();
        this.mInitialDisplayWidth = 0;
        this.mInitialDisplayHeight = 0;
        this.mInitialDisplayDensity = 0;
        this.mDisplayCutoutCache = new RotationCache<>(new RotationCache.RotationDependentComputation() {
            @Override
            public final Object compute(Object obj, int i) {
                return this.f$0.calculateDisplayCutoutForRotationUncached((DisplayCutout) obj, i);
            }
        });
        this.mBaseDisplayWidth = 0;
        this.mBaseDisplayHeight = 0;
        this.mBaseDisplayDensity = 0;
        this.mDisplayInfo = new DisplayInfo();
        this.mDisplayMetrics = new DisplayMetrics();
        this.mRealDisplayMetrics = new DisplayMetrics();
        this.mTmpDisplayMetrics = new DisplayMetrics();
        this.mCompatDisplayMetrics = new DisplayMetrics();
        this.mRotation = 0;
        this.mLastOrientation = -1;
        this.mAltOrientation = false;
        this.mLastWindowForcedOrientation = -1;
        this.mLastKeyguardForcedOrientation = -1;
        this.mLastWallpaperVisible = false;
        this.mBaseDisplayRect = new Rect();
        this.mShouldOverrideDisplayConfiguration = true;
        this.mExitingTokens = new ArrayList<>();
        this.mTouchExcludeRegion = new Region();
        this.mTmpRect = new Rect();
        this.mTmpRect2 = new Rect();
        this.mTmpRectF = new RectF();
        this.mTmpMatrix = new Matrix();
        this.mTmpRegion = new Region();
        this.mTmpBounds = new Rect();
        this.mTapExcludedWindows = new ArrayList<>();
        this.mTapExcludeProvidingWindows = new ArraySet<>();
        this.mHaveBootMsg = false;
        this.mHaveApp = false;
        this.mHaveWallpaper = false;
        this.mHaveKeyguard = true;
        this.mTmpUpdateAllDrawn = new LinkedList<>();
        this.mTmpTaskForResizePointSearchResult = new TaskForResizePointSearchResult();
        this.mTmpApplySurfaceChangesTransactionState = new ApplySurfaceChangesTransactionState();
        this.mRemovingDisplay = false;
        this.mDisplayReady = false;
        this.mSession = new SurfaceSession();
        this.mLayoutSeq = 0;
        this.mTmpFloats = new float[9];
        this.mUpdateWindowsForAnimator = new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$new$0(this.f$0, (WindowState) obj);
            }
        };
        this.mUpdateWallpaperForAnimator = new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$new$1(this.f$0, (WindowState) obj);
            }
        };
        this.mScheduleToastTimeout = new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$new$2(this.f$0, (WindowState) obj);
            }
        };
        this.mFindFocusedWindow = new ToBooleanFunction() {
            public final boolean apply(Object obj) {
                return DisplayContent.lambda$new$3(this.f$0, (WindowState) obj);
            }
        };
        this.mPerformLayout = new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$new$4(this.f$0, (WindowState) obj);
            }
        };
        this.mPerformLayoutAttached = new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$new$5(this.f$0, (WindowState) obj);
            }
        };
        this.mComputeImeTargetPredicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DisplayContent.lambda$new$6(this.f$0, (WindowState) obj);
            }
        };
        this.mApplyPostLayoutPolicy = new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent displayContent = this.f$0;
                WindowState windowState = (WindowState) obj;
                displayContent.mService.mPolicy.applyPostLayoutPolicyLw(windowState, windowState.mAttrs, windowState.getParentWindow(), displayContent.mService.mInputMethodTarget);
            }
        };
        this.mApplySurfaceChangesTransaction = new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$new$8(this.f$0, (WindowState) obj);
            }
        };
        this.mWmsExt = MtkSystemServiceFactory.getInstance().makeWmsExt();
        setController(displayWindowController);
        if (windowManagerService.mRoot.getDisplayContent(display.getDisplayId()) != null) {
            throw new IllegalArgumentException("Display with ID=" + display.getDisplayId() + " already exists=" + windowManagerService.mRoot.getDisplayContent(display.getDisplayId()) + " new=" + display);
        }
        this.mDisplay = display;
        this.mDisplayId = display.getDisplayId();
        this.mWallpaperController = wallpaperController;
        display.getDisplayInfo(this.mDisplayInfo);
        display.getMetrics(this.mDisplayMetrics);
        if (this.mDisplayId != 0) {
            z = false;
        } else {
            z = true;
        }
        this.isDefaultDisplay = z;
        this.mDisplayFrames = new DisplayFrames(this.mDisplayId, this.mDisplayInfo, calculateDisplayCutoutForRotation(this.mDisplayInfo.rotation));
        initializeDisplayBaseInfo();
        this.mDividerControllerLocked = new DockedStackDividerController(windowManagerService, this);
        this.mPinnedStackControllerLocked = new PinnedStackController(windowManagerService, this);
        this.mSurfaceSize = Math.max(this.mBaseDisplayHeight, this.mBaseDisplayWidth) * 2;
        SurfaceControl.Builder containerLayer = this.mService.makeSurfaceBuilder(this.mSession).setSize(this.mSurfaceSize, this.mSurfaceSize).setOpaque(true).setContainerLayer(true);
        this.mWindowingLayer = containerLayer.setName("Display Root").build();
        this.mOverlayLayer = containerLayer.setName("Display Overlays").build();
        getPendingTransaction().setLayer(this.mWindowingLayer, 0).setLayerStack(this.mWindowingLayer, this.mDisplayId).show(this.mWindowingLayer).setLayer(this.mOverlayLayer, 1).setLayerStack(this.mOverlayLayer, this.mDisplayId).show(this.mOverlayLayer);
        getPendingTransaction().apply();
        super.addChild(this.mBelowAppWindowsContainers, (Comparator<NonAppWindowContainers>) null);
        super.addChild(this.mTaskStackContainers, (Comparator<TaskStackContainers>) null);
        super.addChild(this.mAboveAppWindowsContainers, (Comparator<AboveAppWindowContainers>) null);
        super.addChild(this.mImeWindowsContainers, (Comparator<NonMagnifiableWindowContainers>) null);
        this.mService.mRoot.addChild(this, (Comparator<DisplayContent>) null);
        this.mDisplayReady = true;
    }

    boolean isReady() {
        return this.mService.mDisplayReady && this.mDisplayReady;
    }

    int getDisplayId() {
        return this.mDisplayId;
    }

    WindowToken getWindowToken(IBinder iBinder) {
        return this.mTokenMap.get(iBinder);
    }

    AppWindowToken getAppWindowToken(IBinder iBinder) {
        WindowToken windowToken = getWindowToken(iBinder);
        if (windowToken == null) {
            return null;
        }
        return windowToken.asAppWindowToken();
    }

    private void addWindowToken(IBinder iBinder, WindowToken windowToken) {
        DisplayContent windowTokenDisplay = this.mService.mRoot.getWindowTokenDisplay(windowToken);
        if (windowTokenDisplay != null) {
            throw new IllegalArgumentException("Can't map token=" + windowToken + " to display=" + getName() + " already mapped to display=" + windowTokenDisplay + " tokens=" + windowTokenDisplay.mTokenMap);
        }
        if (iBinder == null) {
            throw new IllegalArgumentException("Can't map token=" + windowToken + " to display=" + getName() + " binder is null");
        }
        if (windowToken == null) {
            throw new IllegalArgumentException("Can't map null token to display=" + getName() + " binder=" + iBinder);
        }
        this.mTokenMap.put(iBinder, windowToken);
        if (windowToken.asAppWindowToken() == null) {
            switch (windowToken.windowType) {
                case 2011:
                case 2012:
                    this.mImeWindowsContainers.addChild(windowToken);
                    return;
                case 2013:
                    this.mBelowAppWindowsContainers.addChild(windowToken);
                    return;
                default:
                    this.mAboveAppWindowsContainers.addChild(windowToken);
                    return;
            }
        }
    }

    WindowToken removeWindowToken(IBinder iBinder) {
        WindowToken windowTokenRemove = this.mTokenMap.remove(iBinder);
        if (windowTokenRemove != null && windowTokenRemove.asAppWindowToken() == null) {
            windowTokenRemove.setExiting();
        }
        return windowTokenRemove;
    }

    void reParentWindowToken(WindowToken windowToken) {
        DisplayContent displayContent = windowToken.getDisplayContent();
        if (displayContent == this) {
            return;
        }
        if (displayContent != null && displayContent.mTokenMap.remove(windowToken.token) != null && windowToken.asAppWindowToken() == null) {
            windowToken.getParent().removeChild(windowToken);
        }
        addWindowToken(windowToken.token, windowToken);
    }

    void removeAppToken(IBinder iBinder) {
        WindowToken windowTokenRemoveWindowToken = removeWindowToken(iBinder);
        if (windowTokenRemoveWindowToken == null) {
            Slog.w("WindowManager", "removeAppToken: Attempted to remove non-existing token: " + iBinder);
            return;
        }
        AppWindowToken appWindowTokenAsAppWindowToken = windowTokenRemoveWindowToken.asAppWindowToken();
        if (appWindowTokenAsAppWindowToken == null) {
            Slog.w("WindowManager", "Attempted to remove non-App token: " + iBinder + " token=" + windowTokenRemoveWindowToken);
            return;
        }
        appWindowTokenAsAppWindowToken.onRemovedFromDisplay();
    }

    Display getDisplay() {
        return this.mDisplay;
    }

    DisplayInfo getDisplayInfo() {
        return this.mDisplayInfo;
    }

    DisplayMetrics getDisplayMetrics() {
        return this.mDisplayMetrics;
    }

    int getRotation() {
        return this.mRotation;
    }

    @VisibleForTesting
    void setRotation(int i) {
        this.mRotation = i;
    }

    int getLastOrientation() {
        return this.mLastOrientation;
    }

    void setLastOrientation(int i) {
        this.mLastOrientation = i;
    }

    boolean getAltOrientation() {
        return this.mAltOrientation;
    }

    void setAltOrientation(boolean z) {
        this.mAltOrientation = z;
    }

    int getLastWindowForcedOrientation() {
        return this.mLastWindowForcedOrientation;
    }

    boolean updateRotationUnchecked() {
        return updateRotationUnchecked(false);
    }

    boolean updateRotationUnchecked(boolean z) {
        ScreenRotationAnimation screenRotationAnimationLocked;
        ?? r4;
        if (!z) {
            if (this.mService.mDeferredRotationPauseCount > 0) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Deferring rotation, rotation is paused.");
                }
                return false;
            }
            ScreenRotationAnimation screenRotationAnimationLocked2 = this.mService.mAnimator.getScreenRotationAnimationLocked(this.mDisplayId);
            if (screenRotationAnimationLocked2 != null && screenRotationAnimationLocked2.isAnimating()) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Deferring rotation, animation in progress.");
                }
                return false;
            }
            if (this.mService.mDisplayFrozen) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Deferring rotation, still finishing previous rotation");
                }
                return false;
            }
        }
        if (!this.mService.mDisplayEnabled) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Deferring rotation, display is not enabled.");
            }
            return false;
        }
        final int i = this.mRotation;
        int i2 = this.mLastOrientation;
        boolean z2 = this.mAltOrientation;
        final int iRotationForOrientationLw = this.mService.mPolicy.rotationForOrientationLw(i2, i, this.isDefaultDisplay);
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "Computed rotation=" + iRotationForOrientationLw + " for display id=" + this.mDisplayId + " based on lastOrientation=" + i2 + " and oldRotation=" + i);
        }
        final boolean zShouldRotateSeamlessly = this.mService.mPolicy.shouldRotateSeamlessly(i, iRotationForOrientationLw);
        if (zShouldRotateSeamlessly) {
            if (getWindow(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((WindowState) obj).mSeamlesslyRotated;
                }
            }) != null && !z) {
                return false;
            }
            if (hasPinnedStack()) {
                zShouldRotateSeamlessly = false;
            }
            int i3 = 0;
            while (true) {
                if (i3 >= this.mService.mSessions.size()) {
                    break;
                }
                if (!this.mService.mSessions.valueAt(i3).hasAlertWindowSurfaces()) {
                    i3++;
                } else {
                    zShouldRotateSeamlessly = false;
                    break;
                }
            }
        }
        boolean z3 = !this.mService.mPolicy.rotationHasCompatibleMetricsLw(i2, iRotationForOrientationLw);
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            StringBuilder sb = new StringBuilder();
            sb.append("Display id=");
            sb.append(this.mDisplayId);
            sb.append(" selected orientation ");
            sb.append(i2);
            sb.append(", got rotation ");
            sb.append(iRotationForOrientationLw);
            sb.append(" which has ");
            sb.append(z3 ? "incompatible" : "compatible");
            sb.append(" metrics");
            Slog.v("WindowManager", sb.toString());
        }
        if (i == iRotationForOrientationLw && z2 == z3) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Display id=");
            sb2.append(this.mDisplayId);
            sb2.append(" rotation changed to ");
            sb2.append(iRotationForOrientationLw);
            sb2.append(z3 ? " (alt)" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb2.append(" from ");
            sb2.append(i);
            sb2.append(z2 ? " (alt)" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb2.append(", lastOrientation=");
            sb2.append(i2);
            Slog.v("WindowManager", sb2.toString());
        }
        if (deltaRotation(iRotationForOrientationLw, i) != 2) {
            this.mService.mWaitingForConfig = true;
        }
        this.mRotation = iRotationForOrientationLw;
        this.mAltOrientation = z3;
        if (this.isDefaultDisplay) {
            this.mService.mPolicy.setRotationLw(iRotationForOrientationLw);
        }
        this.mService.mWindowsFreezingScreen = 1;
        this.mService.mH.removeMessages(11);
        this.mService.mH.sendEmptyMessageDelayed(11, 2000L);
        setLayoutNeeded();
        int[] iArr = new int[2];
        this.mService.mPolicy.selectRotationAnimationLw(iArr);
        if (!zShouldRotateSeamlessly) {
            this.mService.startFreezingDisplayLocked(iArr[0], iArr[1], this);
            screenRotationAnimationLocked = this.mService.mAnimator.getScreenRotationAnimationLocked(this.mDisplayId);
        } else {
            screenRotationAnimationLocked = null;
            this.mService.startSeamlessRotation();
        }
        ScreenRotationAnimation screenRotationAnimation = screenRotationAnimationLocked;
        updateDisplayAndOrientation(getConfiguration().uiMode);
        if (screenRotationAnimation != null && screenRotationAnimation.hasScreenshot()) {
            boolean z4 = true;
            r4 = z4;
            if (screenRotationAnimation.setRotation(getPendingTransaction(), iRotationForOrientationLw, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, this.mService.getTransitionAnimationScaleLocked(), this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight)) {
                this.mService.scheduleAnimationLocked();
                r4 = z4;
            }
        } else {
            r4 = 1;
        }
        if (zShouldRotateSeamlessly) {
            forAllWindows((Consumer<WindowState>) new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((WindowState) obj).mWinAnimator.seamlesslyRotateWindow(this.f$0.getPendingTransaction(), i, iRotationForOrientationLw);
                }
            }, (boolean) r4);
        }
        this.mService.mDisplayManagerInternal.performTraversal(getPendingTransaction());
        scheduleAnimation();
        forAllWindows((Consumer<WindowState>) new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$updateRotationUnchecked$11(this.f$0, zShouldRotateSeamlessly, (WindowState) obj);
            }
        }, (boolean) r4);
        if (zShouldRotateSeamlessly) {
            this.mService.mH.removeMessages(54);
            this.mService.mH.sendEmptyMessageDelayed(54, 2000L);
        }
        for (int size = this.mService.mRotationWatchers.size() - r4; size >= 0; size--) {
            WindowManagerService.RotationWatcher rotationWatcher = this.mService.mRotationWatchers.get(size);
            if (rotationWatcher.mDisplayId == this.mDisplayId) {
                try {
                    rotationWatcher.mWatcher.onRotationChanged(iRotationForOrientationLw);
                } catch (RemoteException e) {
                }
            }
        }
        if (screenRotationAnimation == null && this.mService.mAccessibilityController != null && this.isDefaultDisplay) {
            this.mService.mAccessibilityController.onRotationChangedLocked(this);
        }
        return r4;
    }

    public static void lambda$updateRotationUnchecked$11(DisplayContent displayContent, boolean z, WindowState windowState) {
        if (windowState.mHasSurface && !z) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Set mOrientationChanging of " + windowState);
            }
            windowState.setOrientationChanging(true);
            displayContent.mService.mRoot.mOrientationChangeComplete = false;
            windowState.mLastFreezeDuration = 0;
        }
        windowState.mReportOrientationChanged = true;
    }

    void configureDisplayPolicy() {
        this.mService.mPolicy.setInitialDisplaySize(getDisplay(), this.mBaseDisplayWidth, this.mBaseDisplayHeight, this.mBaseDisplayDensity);
        this.mDisplayFrames.onDisplayInfoUpdated(this.mDisplayInfo, calculateDisplayCutoutForRotation(this.mDisplayInfo.rotation));
    }

    public DisplayInfo updateDisplayAndOrientation(int i) {
        int i2;
        int i3;
        int i4;
        int i5;
        boolean z = true;
        if (this.mRotation != 1 && this.mRotation != 3) {
            z = false;
        }
        int i6 = z ? this.mBaseDisplayHeight : this.mBaseDisplayWidth;
        int i7 = z ? this.mBaseDisplayWidth : this.mBaseDisplayHeight;
        if (!this.mAltOrientation) {
            i2 = i6;
            i3 = i7;
        } else if (i6 > i7) {
            i2 = (int) (i7 / 1.3f);
            if (i2 >= i6) {
                i2 = i6;
            }
            i3 = i7;
        } else {
            int i8 = (int) (i6 / 1.3f);
            if (i8 < i7) {
                i3 = i8;
                i2 = i6;
            }
            i2 = i6;
            i3 = i7;
        }
        DisplayCutout displayCutout = calculateDisplayCutoutForRotation(this.mRotation).getDisplayCutout();
        int i9 = i2;
        int i10 = i3;
        int nonDecorDisplayWidth = this.mService.mPolicy.getNonDecorDisplayWidth(i9, i10, this.mRotation, i, this.mDisplayId, displayCutout);
        int nonDecorDisplayHeight = this.mService.mPolicy.getNonDecorDisplayHeight(i9, i10, this.mRotation, i, this.mDisplayId, displayCutout);
        this.mDisplayInfo.rotation = this.mRotation;
        this.mDisplayInfo.logicalWidth = i2;
        this.mDisplayInfo.logicalHeight = i3;
        if (this.mWmsExt.isFullScreenCropState(this.mService.mFocusedApp)) {
            Rect switchFrame = this.mWmsExt.getSwitchFrame(i6, i7);
            i4 = i2 - (switchFrame.left + switchFrame.right);
            i5 = i3 - (switchFrame.top + switchFrame.bottom);
            nonDecorDisplayWidth -= switchFrame.left + switchFrame.right;
            nonDecorDisplayHeight -= switchFrame.top + switchFrame.bottom;
        } else {
            i4 = 0;
            i5 = 0;
        }
        this.mDisplayInfo.fullscreenCropInfo.width = i4;
        this.mDisplayInfo.fullscreenCropInfo.height = i5;
        this.mDisplayInfo.logicalDensityDpi = this.mBaseDisplayDensity;
        this.mDisplayInfo.appWidth = nonDecorDisplayWidth;
        this.mDisplayInfo.appHeight = nonDecorDisplayHeight;
        if (this.isDefaultDisplay) {
            this.mDisplayInfo.getLogicalMetrics(this.mRealDisplayMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, (Configuration) null);
        }
        DisplayInfo displayInfo = this.mDisplayInfo;
        if (displayCutout.isEmpty()) {
            displayCutout = null;
        }
        displayInfo.displayCutout = displayCutout;
        this.mDisplayInfo.getAppMetrics(this.mDisplayMetrics);
        if (this.mDisplayScalingDisabled) {
            this.mDisplayInfo.flags |= 1073741824;
        } else {
            this.mDisplayInfo.flags &= -1073741825;
        }
        this.mService.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(this.mDisplayId, this.mShouldOverrideDisplayConfiguration ? this.mDisplayInfo : null);
        this.mBaseDisplayRect.set(0, 0, i2, i3);
        if (this.isDefaultDisplay) {
            this.mCompatibleScreenScale = CompatibilityInfo.computeCompatibleScaling(this.mDisplayMetrics, this.mCompatDisplayMetrics);
        }
        updateBounds();
        return this.mDisplayInfo;
    }

    WmDisplayCutout calculateDisplayCutoutForRotation(int i) {
        return this.mDisplayCutoutCache.getOrCompute(this.mInitialDisplayCutout, i);
    }

    private WmDisplayCutout calculateDisplayCutoutForRotationUncached(DisplayCutout displayCutout, int i) {
        if (displayCutout == null || displayCutout == DisplayCutout.NO_CUTOUT) {
            return WmDisplayCutout.NO_CUTOUT;
        }
        if (i == 0) {
            return WmDisplayCutout.computeSafeInsets(displayCutout, this.mInitialDisplayWidth, this.mInitialDisplayHeight);
        }
        boolean z = true;
        if (i != 1 && i != 3) {
            z = false;
        }
        Path boundaryPath = displayCutout.getBounds().getBoundaryPath();
        CoordinateTransforms.transformPhysicalToLogicalCoordinates(i, this.mInitialDisplayWidth, this.mInitialDisplayHeight, this.mTmpMatrix);
        boundaryPath.transform(this.mTmpMatrix);
        return WmDisplayCutout.computeSafeInsets(DisplayCutout.fromBounds(boundaryPath), z ? this.mInitialDisplayHeight : this.mInitialDisplayWidth, z ? this.mInitialDisplayWidth : this.mInitialDisplayHeight);
    }

    void computeScreenConfiguration(Configuration configuration) {
        int i;
        int i2;
        int i3;
        int i4;
        DisplayInfo displayInfoUpdateDisplayAndOrientation = updateDisplayAndOrientation(configuration.uiMode);
        int i5 = displayInfoUpdateDisplayAndOrientation.logicalWidth;
        int i6 = displayInfoUpdateDisplayAndOrientation.logicalHeight;
        configuration.orientation = i5 <= i6 ? 1 : 2;
        configuration.windowConfiguration.setWindowingMode(1);
        float f = this.mDisplayMetrics.density;
        configuration.screenWidthDp = (int) (this.mService.mPolicy.getConfigDisplayWidth(i5, i6, displayInfoUpdateDisplayAndOrientation.rotation, configuration.uiMode, this.mDisplayId, displayInfoUpdateDisplayAndOrientation.displayCutout) / f);
        configuration.screenHeightDp = (int) (this.mService.mPolicy.getConfigDisplayHeight(i5, i6, displayInfoUpdateDisplayAndOrientation.rotation, configuration.uiMode, this.mDisplayId, displayInfoUpdateDisplayAndOrientation.displayCutout) / f);
        this.mService.mPolicy.getNonDecorInsetsLw(displayInfoUpdateDisplayAndOrientation.rotation, i5, i6, displayInfoUpdateDisplayAndOrientation.displayCutout, this.mTmpRect);
        int i7 = this.mTmpRect.left;
        int i8 = this.mTmpRect.top;
        configuration.windowConfiguration.setAppBounds(i7, i8, displayInfoUpdateDisplayAndOrientation.appWidth + i7, displayInfoUpdateDisplayAndOrientation.appHeight + i8);
        boolean z = displayInfoUpdateDisplayAndOrientation.rotation == 1 || displayInfoUpdateDisplayAndOrientation.rotation == 3;
        computeSizeRangesAndScreenLayout(displayInfoUpdateDisplayAndOrientation, this.mDisplayId, z, configuration.uiMode, i5, i6, f, configuration);
        int i9 = configuration.screenLayout & (-769);
        if ((displayInfoUpdateDisplayAndOrientation.flags & 16) != 0) {
            i = 512;
        } else {
            i = 256;
        }
        configuration.screenLayout = i9 | i;
        configuration.compatScreenWidthDp = (int) (configuration.screenWidthDp / this.mCompatibleScreenScale);
        configuration.compatScreenHeightDp = (int) (configuration.screenHeightDp / this.mCompatibleScreenScale);
        configuration.compatSmallestScreenWidthDp = computeCompatSmallestWidth(z, configuration.uiMode, i5, i6, this.mDisplayId);
        configuration.densityDpi = displayInfoUpdateDisplayAndOrientation.logicalDensityDpi;
        if (displayInfoUpdateDisplayAndOrientation.isHdr()) {
            i2 = 8;
        } else {
            i2 = 4;
        }
        if (!displayInfoUpdateDisplayAndOrientation.isWideColorGamut() || !this.mService.hasWideColorGamutSupport()) {
            i3 = 1;
        } else {
            i3 = 2;
        }
        configuration.colorMode = i2 | i3;
        configuration.touchscreen = 1;
        configuration.keyboard = 1;
        configuration.navigation = 1;
        InputDevice[] inputDevices = this.mService.mInputManager.getInputDevices();
        int length = inputDevices != null ? inputDevices.length : 0;
        int i10 = 0;
        int i11 = 0;
        for (int i12 = 0; i12 < length; i12++) {
            InputDevice inputDevice = inputDevices[i12];
            if (!inputDevice.isVirtual()) {
                int sources = inputDevice.getSources();
                int i13 = inputDevice.isExternal() ? 2 : 1;
                if (this.mService.mIsTouchDevice) {
                    if ((sources & UsbACInterface.FORMAT_II_AC3) == 4098) {
                        configuration.touchscreen = 3;
                    }
                } else {
                    configuration.touchscreen = 1;
                }
                if ((sources & 65540) == 65540) {
                    configuration.navigation = 3;
                    i10 |= i13;
                } else {
                    if ((sources & UsbTerminalTypes.TERMINAL_IN_MIC) == 513 && configuration.navigation == 1) {
                        i4 = 2;
                        configuration.navigation = 2;
                        i10 |= i13;
                    }
                    if (inputDevice.getKeyboardType() != i4) {
                        configuration.keyboard = i4;
                        i11 |= i13;
                    }
                }
                i4 = 2;
                if (inputDevice.getKeyboardType() != i4) {
                }
            }
        }
        if (configuration.navigation == 1 && this.mService.mHasPermanentDpad) {
            configuration.navigation = 2;
            i10 |= 1;
        }
        boolean z2 = configuration.keyboard != 1;
        if (z2 != this.mService.mHardKeyboardAvailable) {
            this.mService.mHardKeyboardAvailable = z2;
            this.mService.mH.removeMessages(22);
            this.mService.mH.sendEmptyMessage(22);
        }
        configuration.keyboardHidden = 1;
        configuration.hardKeyboardHidden = 1;
        configuration.navigationHidden = 1;
        this.mService.mPolicy.adjustConfigurationLw(configuration, i11, i10);
    }

    private int computeCompatSmallestWidth(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        this.mTmpDisplayMetrics.setTo(this.mDisplayMetrics);
        DisplayMetrics displayMetrics = this.mTmpDisplayMetrics;
        if (z) {
            i6 = i2;
            i5 = i3;
        } else {
            i5 = i2;
            i6 = i3;
        }
        return reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(0, 0, i, displayMetrics, i5, i6, i4), 1, i, displayMetrics, i6, i5, i4), 2, i, displayMetrics, i5, i6, i4), 3, i, displayMetrics, i6, i5, i4);
    }

    private int reduceCompatConfigWidthSize(int i, int i2, int i3, DisplayMetrics displayMetrics, int i4, int i5, int i6) {
        displayMetrics.noncompatWidthPixels = this.mService.mPolicy.getNonDecorDisplayWidth(i4, i5, i2, i3, i6, this.mDisplayInfo.displayCutout);
        displayMetrics.noncompatHeightPixels = this.mService.mPolicy.getNonDecorDisplayHeight(i4, i5, i2, i3, i6, this.mDisplayInfo.displayCutout);
        int iComputeCompatibleScaling = (int) (((displayMetrics.noncompatWidthPixels / CompatibilityInfo.computeCompatibleScaling(displayMetrics, (DisplayMetrics) null)) / displayMetrics.density) + 0.5f);
        return (i == 0 || iComputeCompatibleScaling < i) ? iComputeCompatibleScaling : i;
    }

    private void computeSizeRangesAndScreenLayout(DisplayInfo displayInfo, int i, boolean z, int i2, int i3, int i4, float f, Configuration configuration) {
        int i5;
        int i6;
        if (z) {
            i6 = i3;
            i5 = i4;
        } else {
            i5 = i3;
            i6 = i4;
        }
        displayInfo.smallestNominalAppWidth = 1073741824;
        displayInfo.smallestNominalAppHeight = 1073741824;
        displayInfo.largestNominalAppWidth = 0;
        displayInfo.largestNominalAppHeight = 0;
        adjustDisplaySizeRanges(displayInfo, i, 0, i2, i5, i6);
        adjustDisplaySizeRanges(displayInfo, i, 1, i2, i6, i5);
        adjustDisplaySizeRanges(displayInfo, i, 2, i2, i5, i6);
        adjustDisplaySizeRanges(displayInfo, i, 3, i2, i6, i5);
        int iReduceConfigLayout = reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(Configuration.resetScreenLayout(configuration.screenLayout), 0, f, i5, i6, i2, i), 1, f, i6, i5, i2, i), 2, f, i5, i6, i2, i), 3, f, i6, i5, i2, i);
        configuration.smallestScreenWidthDp = (int) (displayInfo.smallestNominalAppWidth / f);
        configuration.screenLayout = iReduceConfigLayout;
    }

    private int reduceConfigLayout(int i, int i2, float f, int i3, int i4, int i5, int i6) {
        int nonDecorDisplayWidth = this.mService.mPolicy.getNonDecorDisplayWidth(i3, i4, i2, i5, i6, this.mDisplayInfo.displayCutout);
        int nonDecorDisplayHeight = this.mService.mPolicy.getNonDecorDisplayHeight(i3, i4, i2, i5, i6, this.mDisplayInfo.displayCutout);
        if (nonDecorDisplayWidth >= nonDecorDisplayHeight) {
            nonDecorDisplayWidth = nonDecorDisplayHeight;
            nonDecorDisplayHeight = nonDecorDisplayWidth;
        }
        return Configuration.reduceScreenLayout(i, (int) (nonDecorDisplayHeight / f), (int) (nonDecorDisplayWidth / f));
    }

    private void adjustDisplaySizeRanges(DisplayInfo displayInfo, int i, int i2, int i3, int i4, int i5) {
        DisplayCutout displayCutout = calculateDisplayCutoutForRotation(i2).getDisplayCutout();
        int configDisplayWidth = this.mService.mPolicy.getConfigDisplayWidth(i4, i5, i2, i3, i, displayCutout);
        if (configDisplayWidth < displayInfo.smallestNominalAppWidth) {
            displayInfo.smallestNominalAppWidth = configDisplayWidth;
        }
        if (configDisplayWidth > displayInfo.largestNominalAppWidth) {
            displayInfo.largestNominalAppWidth = configDisplayWidth;
        }
        int configDisplayHeight = this.mService.mPolicy.getConfigDisplayHeight(i4, i5, i2, i3, i, displayCutout);
        if (configDisplayHeight < displayInfo.smallestNominalAppHeight) {
            displayInfo.smallestNominalAppHeight = configDisplayHeight;
        }
        if (configDisplayHeight > displayInfo.largestNominalAppHeight) {
            displayInfo.largestNominalAppHeight = configDisplayHeight;
        }
    }

    DockedStackDividerController getDockedDividerController() {
        return this.mDividerControllerLocked;
    }

    PinnedStackController getPinnedStackController() {
        return this.mPinnedStackControllerLocked;
    }

    boolean hasAccess(int i) {
        return this.mDisplay.hasAccess(i);
    }

    boolean isPrivate() {
        return (this.mDisplay.getFlags() & 4) != 0;
    }

    TaskStack getHomeStack() {
        return this.mTaskStackContainers.getHomeStack();
    }

    TaskStack getSplitScreenPrimaryStack() {
        TaskStack splitScreenPrimaryStack = this.mTaskStackContainers.getSplitScreenPrimaryStack();
        if (splitScreenPrimaryStack == null || !splitScreenPrimaryStack.isVisible()) {
            return null;
        }
        return splitScreenPrimaryStack;
    }

    boolean hasSplitScreenPrimaryStack() {
        return getSplitScreenPrimaryStack() != null;
    }

    TaskStack getSplitScreenPrimaryStackIgnoringVisibility() {
        return this.mTaskStackContainers.getSplitScreenPrimaryStack();
    }

    TaskStack getPinnedStack() {
        return this.mTaskStackContainers.getPinnedStack();
    }

    private boolean hasPinnedStack() {
        return this.mTaskStackContainers.getPinnedStack() != null;
    }

    TaskStack getTopStackInWindowingMode(int i) {
        return getStack(i, 0);
    }

    TaskStack getStack(int i, int i2) {
        return this.mTaskStackContainers.getStack(i, i2);
    }

    @VisibleForTesting
    TaskStack getTopStack() {
        return this.mTaskStackContainers.getTopStack();
    }

    ArrayList<Task> getVisibleTasks() {
        return this.mTaskStackContainers.getVisibleTasks();
    }

    void onStackWindowingModeChanged(TaskStack taskStack) {
        this.mTaskStackContainers.onStackWindowingModeChanged(taskStack);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mService.reconfigureDisplayLocked(this);
        if (getDockedDividerController() != null) {
            getDockedDividerController().onConfigurationChanged();
        }
        if (getPinnedStackController() != null) {
            getPinnedStackController().onConfigurationChanged();
        }
    }

    void updateStackBoundsAfterConfigChange(List<TaskStack> list) {
        for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
            TaskStack taskStack = (TaskStack) this.mTaskStackContainers.getChildAt(childCount);
            if (taskStack.updateBoundsAfterConfigChange()) {
                list.add(taskStack);
            }
        }
        if (!hasPinnedStack()) {
            this.mPinnedStackControllerLocked.onDisplayInfoChanged();
        }
    }

    @Override
    boolean fillsParent() {
        return true;
    }

    @Override
    boolean isVisible() {
        return true;
    }

    @Override
    void onAppTransitionDone() {
        super.onAppTransitionDone();
        this.mService.mWindowsChanged = true;
    }

    private boolean skipTraverseChild(WindowContainer windowContainer) {
        if (windowContainer == this.mImeWindowsContainers && this.mService.mInputMethodTarget != null && !hasSplitScreenPrimaryStack()) {
            return true;
        }
        return false;
    }

    @Override
    boolean forAllWindows(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
        if (z) {
            for (int size = this.mChildren.size() - 1; size >= 0; size--) {
                DisplayChildWindowContainer displayChildWindowContainer = (DisplayChildWindowContainer) this.mChildren.get(size);
                if (!skipTraverseChild(displayChildWindowContainer) && displayChildWindowContainer.forAllWindows(toBooleanFunction, z)) {
                    return true;
                }
            }
        } else {
            int size2 = this.mChildren.size();
            for (int i = 0; i < size2; i++) {
                DisplayChildWindowContainer displayChildWindowContainer2 = (DisplayChildWindowContainer) this.mChildren.get(i);
                if (!skipTraverseChild(displayChildWindowContainer2) && displayChildWindowContainer2.forAllWindows(toBooleanFunction, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean forAllImeWindows(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
        return this.mImeWindowsContainers.forAllWindows(toBooleanFunction, z);
    }

    @Override
    int getOrientation() {
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            this.mService.mWindowManagerDebugger.debugGetOrientation("WindowManager", this.mService.mDisplayFrozen, this.mLastWindowForcedOrientation, this.mLastKeyguardForcedOrientation);
        }
        WindowManagerPolicy windowManagerPolicy = this.mService.mPolicy;
        if (this.mService.mDisplayFrozen) {
            if (this.mLastWindowForcedOrientation != -1) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Display id=" + this.mDisplayId + " is frozen, return " + this.mLastWindowForcedOrientation);
                }
                return this.mLastWindowForcedOrientation;
            }
            if (windowManagerPolicy.isKeyguardLocked()) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Display id=" + this.mDisplayId + " is frozen while keyguard locked, return " + this.mLastOrientation);
                }
                return this.mLastOrientation;
            }
        } else {
            int orientation = this.mAboveAppWindowsContainers.getOrientation();
            if (orientation != -2) {
                return orientation;
            }
        }
        return this.mTaskStackContainers.getOrientation();
    }

    void updateDisplayInfo() {
        updateBaseDisplayMetricsIfNeeded();
        this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(childCount)).updateDisplayInfo(null);
        }
    }

    void initializeDisplayBaseInfo() {
        DisplayInfo displayInfo;
        DisplayManagerInternal displayManagerInternal = this.mService.mDisplayManagerInternal;
        if (displayManagerInternal != null && (displayInfo = displayManagerInternal.getDisplayInfo(this.mDisplayId)) != null) {
            this.mDisplayInfo.copyFrom(displayInfo);
        }
        updateBaseDisplayMetrics(this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight, this.mDisplayInfo.logicalDensityDpi);
        this.mInitialDisplayWidth = this.mDisplayInfo.logicalWidth;
        this.mInitialDisplayHeight = this.mDisplayInfo.logicalHeight;
        this.mInitialDisplayDensity = this.mDisplayInfo.logicalDensityDpi;
        this.mInitialDisplayCutout = this.mDisplayInfo.displayCutout;
    }

    private void updateBaseDisplayMetricsIfNeeded() {
        this.mService.mDisplayManagerInternal.getNonOverrideDisplayInfo(this.mDisplayId, this.mDisplayInfo);
        int i = this.mDisplayInfo.rotation;
        boolean z = false;
        boolean z2 = i == 1 || i == 3;
        int i2 = z2 ? this.mDisplayInfo.logicalHeight : this.mDisplayInfo.logicalWidth;
        int i3 = z2 ? this.mDisplayInfo.logicalWidth : this.mDisplayInfo.logicalHeight;
        int i4 = this.mDisplayInfo.logicalDensityDpi;
        DisplayCutout displayCutout = this.mDisplayInfo.displayCutout;
        if ((this.mInitialDisplayWidth == i2 && this.mInitialDisplayHeight == i3 && this.mInitialDisplayDensity == this.mDisplayInfo.logicalDensityDpi && Objects.equals(this.mInitialDisplayCutout, displayCutout)) ? false : true) {
            boolean z3 = (this.mBaseDisplayWidth == this.mInitialDisplayWidth && this.mBaseDisplayHeight == this.mInitialDisplayHeight) ? false : true;
            if (this.mBaseDisplayDensity != this.mInitialDisplayDensity) {
                z = true;
            }
            updateBaseDisplayMetrics(z3 ? this.mBaseDisplayWidth : i2, z3 ? this.mBaseDisplayHeight : i3, z ? this.mBaseDisplayDensity : i4);
            this.mInitialDisplayWidth = i2;
            this.mInitialDisplayHeight = i3;
            this.mInitialDisplayDensity = i4;
            this.mInitialDisplayCutout = displayCutout;
            this.mService.reconfigureDisplayLocked(this);
        }
    }

    void setMaxUiWidth(int i) {
        if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
            Slog.v("WindowManager", "Setting max ui width:" + i + " on display:" + getDisplayId());
        }
        this.mMaxUiWidth = i;
        updateBaseDisplayMetrics(this.mBaseDisplayWidth, this.mBaseDisplayHeight, this.mBaseDisplayDensity);
    }

    void updateBaseDisplayMetrics(int i, int i2, int i3) {
        this.mBaseDisplayWidth = i;
        this.mBaseDisplayHeight = i2;
        this.mBaseDisplayDensity = i3;
        if (this.mMaxUiWidth > 0 && this.mBaseDisplayWidth > this.mMaxUiWidth) {
            this.mBaseDisplayHeight = (this.mMaxUiWidth * this.mBaseDisplayHeight) / this.mBaseDisplayWidth;
            this.mBaseDisplayDensity = (this.mMaxUiWidth * this.mBaseDisplayDensity) / this.mBaseDisplayWidth;
            this.mBaseDisplayWidth = this.mMaxUiWidth;
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                Slog.v("WindowManager", "Applying config restraints:" + this.mBaseDisplayWidth + "x" + this.mBaseDisplayHeight + " at density:" + this.mBaseDisplayDensity + " on display:" + getDisplayId());
            }
        }
        this.mBaseDisplayRect.set(0, 0, this.mBaseDisplayWidth, this.mBaseDisplayHeight);
        updateBounds();
    }

    void getStableRect(Rect rect) {
        rect.set(this.mDisplayFrames.mStable);
    }

    TaskStack createStack(int i, boolean z, StackWindowController stackWindowController) {
        if (WindowManagerDebugConfig.DEBUG_STACK) {
            Slog.d("WindowManager", "Create new stackId=" + i + " on displayId=" + this.mDisplayId);
        }
        TaskStack taskStack = new TaskStack(this.mService, i, stackWindowController);
        this.mTaskStackContainers.addStackToDisplay(taskStack, z);
        return taskStack;
    }

    void moveStackToDisplay(TaskStack taskStack, boolean z) {
        DisplayContent displayContent = taskStack.getDisplayContent();
        if (displayContent == null) {
            throw new IllegalStateException("Trying to move stackId=" + taskStack.mStackId + " which is not currently attached to any display");
        }
        if (displayContent.getDisplayId() == this.mDisplayId) {
            throw new IllegalArgumentException("Trying to move stackId=" + taskStack.mStackId + " to its current displayId=" + this.mDisplayId);
        }
        displayContent.mTaskStackContainers.removeChild(taskStack);
        this.mTaskStackContainers.addStackToDisplay(taskStack, z);
    }

    protected void addChild(DisplayChildWindowContainer displayChildWindowContainer, Comparator<DisplayChildWindowContainer> comparator) {
        throw new UnsupportedOperationException("See DisplayChildWindowContainer");
    }

    @Override
    protected void addChild(DisplayChildWindowContainer displayChildWindowContainer, int i) {
        throw new UnsupportedOperationException("See DisplayChildWindowContainer");
    }

    @Override
    protected void removeChild(DisplayChildWindowContainer displayChildWindowContainer) {
        if (this.mRemovingDisplay) {
            super.removeChild(displayChildWindowContainer);
            return;
        }
        throw new UnsupportedOperationException("See DisplayChildWindowContainer");
    }

    @Override
    void positionChildAt(int i, DisplayChildWindowContainer displayChildWindowContainer, boolean z) {
        getParent().positionChildAt(i, this, z);
    }

    void positionStackAt(int i, TaskStack taskStack) {
        this.mTaskStackContainers.positionChildAt(i, taskStack, false);
        layoutAndAssignWindowLayersIfNeeded();
    }

    int taskIdFromPoint(int i, int i2) {
        int iTaskIdFromPoint;
        int childCount = this.mTaskStackContainers.getChildCount();
        do {
            childCount--;
            if (childCount < 0) {
                return -1;
            }
            iTaskIdFromPoint = ((TaskStack) this.mTaskStackContainers.getChildAt(childCount)).taskIdFromPoint(i, i2);
        } while (iTaskIdFromPoint == -1);
        return iTaskIdFromPoint;
    }

    Task findTaskForResizePoint(int i, int i2) {
        int iDipToPixel = WindowManagerService.dipToPixel(30, this.mDisplayMetrics);
        this.mTmpTaskForResizePointSearchResult.reset();
        int childCount = this.mTaskStackContainers.getChildCount();
        do {
            childCount--;
            if (childCount < 0) {
                return null;
            }
            TaskStack taskStack = (TaskStack) this.mTaskStackContainers.getChildAt(childCount);
            if (!taskStack.getWindowConfiguration().canResizeTask()) {
                return null;
            }
            taskStack.findTaskForResizePoint(i, i2, iDipToPixel, this.mTmpTaskForResizePointSearchResult);
        } while (!this.mTmpTaskForResizePointSearchResult.searchDone);
        return this.mTmpTaskForResizePointSearchResult.taskForResize;
    }

    void setTouchExcludeRegion(Task task) {
        if (task == null) {
            this.mTouchExcludeRegion.setEmpty();
        } else {
            this.mTouchExcludeRegion.set(this.mBaseDisplayRect);
            int iDipToPixel = WindowManagerService.dipToPixel(30, this.mDisplayMetrics);
            this.mTmpRect2.setEmpty();
            for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
                ((TaskStack) this.mTaskStackContainers.getChildAt(childCount)).setTouchExcludeRegion(task, iDipToPixel, this.mTouchExcludeRegion, this.mDisplayFrames.mContent, this.mTmpRect2);
            }
            if (!this.mTmpRect2.isEmpty()) {
                this.mTouchExcludeRegion.op(this.mTmpRect2, Region.Op.UNION);
            }
        }
        WindowState windowState = this.mService.mInputMethodWindow;
        if (windowState != null && windowState.isVisibleLw()) {
            windowState.getTouchableRegion(this.mTmpRegion);
            if (windowState.getDisplayId() == this.mDisplayId) {
                this.mTouchExcludeRegion.op(this.mTmpRegion, Region.Op.UNION);
            } else {
                windowState.getDisplayContent().setTouchExcludeRegion(null);
            }
        }
        for (int size = this.mTapExcludedWindows.size() - 1; size >= 0; size--) {
            this.mTapExcludedWindows.get(size).getTouchableRegion(this.mTmpRegion);
            this.mTouchExcludeRegion.op(this.mTmpRegion, Region.Op.UNION);
        }
        for (int size2 = this.mTapExcludeProvidingWindows.size() - 1; size2 >= 0; size2--) {
            this.mTapExcludeProvidingWindows.valueAt(size2).amendTapExcludeRegion(this.mTouchExcludeRegion);
        }
        if (this.mDisplayId == 0 && getSplitScreenPrimaryStack() != null) {
            this.mDividerControllerLocked.getTouchRegion(this.mTmpRect);
            this.mTmpRegion.set(this.mTmpRect);
            this.mTouchExcludeRegion.op(this.mTmpRegion, Region.Op.UNION);
        }
        if (this.mTapDetector != null) {
            this.mTapDetector.setTouchExcludeRegion(this.mTouchExcludeRegion);
        }
    }

    @Override
    void switchUser() {
        super.switchUser();
        this.mService.mWindowsChanged = true;
    }

    private void resetAnimationBackgroundAnimator() {
        for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(childCount)).resetAnimationBackgroundAnimator();
        }
    }

    @Override
    void removeIfPossible() {
        if (isAnimating()) {
            this.mDeferredRemoval = true;
        } else {
            removeImmediately();
        }
    }

    @Override
    void removeImmediately() {
        this.mRemovingDisplay = true;
        try {
            super.removeImmediately();
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                Slog.v("WindowManager", "Removing display=" + this);
            }
            if (this.mService.canDispatchPointerEvents()) {
                if (this.mTapDetector != null) {
                    this.mService.unregisterPointerEventListener(this.mTapDetector);
                }
                if (this.mDisplayId == 0 && this.mService.mMousePositionTracker != null) {
                    this.mService.unregisterPointerEventListener(this.mService.mMousePositionTracker);
                }
            }
            this.mService.mAnimator.removeDisplayLocked(this.mDisplayId);
            this.mRemovingDisplay = false;
            this.mService.onDisplayRemoved(this.mDisplayId);
        } catch (Throwable th) {
            this.mRemovingDisplay = false;
            throw th;
        }
    }

    @Override
    boolean checkCompleteDeferredRemoval() {
        if (!super.checkCompleteDeferredRemoval() && this.mDeferredRemoval) {
            removeImmediately();
            return false;
        }
        return true;
    }

    boolean isRemovalDeferred() {
        return this.mDeferredRemoval;
    }

    boolean animateForIme(float f, float f2, float f3) {
        boolean zUpdateAdjustForIme = false;
        for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
            TaskStack taskStack = (TaskStack) this.mTaskStackContainers.getChildAt(childCount);
            if (taskStack != null && taskStack.isAdjustedForIme()) {
                if (f >= 1.0f && f2 == 0.0f && f3 == 0.0f) {
                    taskStack.resetAdjustedForIme(true);
                    zUpdateAdjustForIme = true;
                } else {
                    this.mDividerControllerLocked.mLastAnimationProgress = this.mDividerControllerLocked.getInterpolatedAnimationValue(f);
                    this.mDividerControllerLocked.mLastDividerProgress = this.mDividerControllerLocked.getInterpolatedDividerValue(f);
                    zUpdateAdjustForIme |= taskStack.updateAdjustForIme(this.mDividerControllerLocked.mLastAnimationProgress, this.mDividerControllerLocked.mLastDividerProgress, false);
                }
                if (f >= 1.0f) {
                    taskStack.endImeAdjustAnimation();
                }
            }
        }
        return zUpdateAdjustForIme;
    }

    boolean clearImeAdjustAnimation() {
        boolean z = false;
        for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
            TaskStack taskStack = (TaskStack) this.mTaskStackContainers.getChildAt(childCount);
            if (taskStack != null && taskStack.isAdjustedForIme()) {
                taskStack.resetAdjustedForIme(true);
                z = true;
            }
        }
        return z;
    }

    void beginImeAdjustAnimation() {
        for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
            TaskStack taskStack = (TaskStack) this.mTaskStackContainers.getChildAt(childCount);
            if (taskStack.isVisible() && taskStack.isAdjustedForIme()) {
                taskStack.beginImeAdjustAnimation();
            }
        }
    }

    void adjustForImeIfNeeded() {
        int dockSide;
        WindowState windowState = this.mService.mInputMethodWindow;
        boolean z = windowState != null && windowState.isVisibleLw() && windowState.isDisplayedLw() && !this.mDividerControllerLocked.isImeHideRequested();
        boolean zIsStackVisible = isStackVisible(3);
        TaskStack imeFocusStackLocked = this.mService.getImeFocusStackLocked();
        if (zIsStackVisible && imeFocusStackLocked != null) {
            dockSide = imeFocusStackLocked.getDockSide();
        } else {
            dockSide = -1;
        }
        boolean z2 = dockSide == 2;
        boolean z3 = dockSide == 4;
        boolean zIsMinimizedDock = this.mDividerControllerLocked.isMinimizedDock();
        int inputMethodWindowVisibleHeight = this.mDisplayFrames.getInputMethodWindowVisibleHeight();
        boolean z4 = z && inputMethodWindowVisibleHeight != this.mDividerControllerLocked.getImeHeightAdjustedFor();
        if (z && zIsStackVisible && ((z2 || z3) && !zIsMinimizedDock)) {
            for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
                TaskStack taskStack = (TaskStack) this.mTaskStackContainers.getChildAt(childCount);
                boolean z5 = taskStack.getDockSide() == 4;
                if (taskStack.isVisible() && ((z3 || z5) && taskStack.inSplitScreenWindowingMode())) {
                    taskStack.setAdjustedForIme(windowState, z3 && z4);
                } else {
                    taskStack.resetAdjustedForIme(false);
                }
            }
            this.mDividerControllerLocked.setAdjustedForIme(z3, true, true, windowState, inputMethodWindowVisibleHeight);
        } else {
            for (int childCount2 = this.mTaskStackContainers.getChildCount() - 1; childCount2 >= 0; childCount2--) {
                ((TaskStack) this.mTaskStackContainers.getChildAt(childCount2)).resetAdjustedForIme(!zIsStackVisible);
            }
            this.mDividerControllerLocked.setAdjustedForIme(false, false, zIsStackVisible, windowState, inputMethodWindowVisibleHeight);
        }
        this.mPinnedStackControllerLocked.setAdjustedForIme(z, inputMethodWindowVisibleHeight);
    }

    int getLayerForAnimationBackground(WindowStateAnimator windowStateAnimator) {
        WindowState window = this.mBelowAppWindowsContainers.getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DisplayContent.lambda$getLayerForAnimationBackground$12((WindowState) obj);
            }
        });
        if (window != null) {
            return window.mWinAnimator.mAnimLayer;
        }
        return windowStateAnimator.mAnimLayer;
    }

    static boolean lambda$getLayerForAnimationBackground$12(WindowState windowState) {
        return windowState.mIsWallpaper && windowState.isVisibleNow();
    }

    void prepareFreezingTaskBounds() {
        for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(childCount)).prepareFreezingTaskBounds();
        }
    }

    void rotateBounds(int i, int i2, Rect rect) {
        getBounds(this.mTmpRect, i2);
        createRotationMatrix(deltaRotation(i2, i), this.mTmpRect.width(), this.mTmpRect.height(), this.mTmpMatrix);
        this.mTmpRectF.set(rect);
        this.mTmpMatrix.mapRect(this.mTmpRectF);
        this.mTmpRectF.round(rect);
    }

    static int deltaRotation(int i, int i2) {
        int i3 = i2 - i;
        return i3 < 0 ? i3 + 4 : i3;
    }

    private static void createRotationMatrix(int i, float f, float f2, Matrix matrix) {
        createRotationMatrix(i, 0.0f, 0.0f, f, f2, matrix);
    }

    static void createRotationMatrix(int i, float f, float f2, float f3, float f4, Matrix matrix) {
        switch (i) {
            case 0:
                matrix.reset();
                break;
            case 1:
                matrix.setRotate(90.0f, 0.0f, 0.0f);
                matrix.postTranslate(f3, 0.0f);
                matrix.postTranslate(-f2, f);
                break;
            case 2:
                matrix.reset();
                break;
            case 3:
                matrix.setRotate(270.0f, 0.0f, 0.0f);
                matrix.postTranslate(0.0f, f4);
                matrix.postTranslate(f2, 0.0f);
                break;
        }
    }

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, z);
        protoOutputStream.write(1120986464258L, this.mDisplayId);
        for (int childCount = this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(childCount)).writeToProto(protoOutputStream, 2246267895811L, z);
        }
        this.mDividerControllerLocked.writeToProto(protoOutputStream, 1146756268036L);
        this.mPinnedStackControllerLocked.writeToProto(protoOutputStream, 1146756268037L);
        for (int childCount2 = this.mAboveAppWindowsContainers.getChildCount() - 1; childCount2 >= 0; childCount2--) {
            ((WindowToken) this.mAboveAppWindowsContainers.getChildAt(childCount2)).writeToProto(protoOutputStream, 2246267895814L, z);
        }
        for (int childCount3 = this.mBelowAppWindowsContainers.getChildCount() - 1; childCount3 >= 0; childCount3--) {
            ((WindowToken) this.mBelowAppWindowsContainers.getChildAt(childCount3)).writeToProto(protoOutputStream, 2246267895815L, z);
        }
        for (int childCount4 = this.mImeWindowsContainers.getChildCount() - 1; childCount4 >= 0; childCount4--) {
            ((WindowToken) this.mImeWindowsContainers.getChildAt(childCount4)).writeToProto(protoOutputStream, 2246267895816L, z);
        }
        protoOutputStream.write(1120986464265L, this.mBaseDisplayDensity);
        this.mDisplayInfo.writeToProto(protoOutputStream, 1146756268042L);
        protoOutputStream.write(1120986464267L, this.mRotation);
        ScreenRotationAnimation screenRotationAnimationLocked = this.mService.mAnimator.getScreenRotationAnimationLocked(this.mDisplayId);
        if (screenRotationAnimationLocked != null) {
            screenRotationAnimationLocked.writeToProto(protoOutputStream, 1146756268044L);
        }
        this.mDisplayFrames.writeToProto(protoOutputStream, 1146756268045L);
        protoOutputStream.end(jStart);
    }

    @Override
    public void dump(PrintWriter printWriter, String str, boolean z) {
        super.dump(printWriter, str, z);
        printWriter.print(str);
        printWriter.print("Display: mDisplayId=");
        printWriter.println(this.mDisplayId);
        String str2 = "  " + str;
        printWriter.print(str2);
        printWriter.print("init=");
        printWriter.print(this.mInitialDisplayWidth);
        printWriter.print("x");
        printWriter.print(this.mInitialDisplayHeight);
        printWriter.print(" ");
        printWriter.print(this.mInitialDisplayDensity);
        printWriter.print("dpi");
        if (this.mInitialDisplayWidth != this.mBaseDisplayWidth || this.mInitialDisplayHeight != this.mBaseDisplayHeight || this.mInitialDisplayDensity != this.mBaseDisplayDensity) {
            printWriter.print(" base=");
            printWriter.print(this.mBaseDisplayWidth);
            printWriter.print("x");
            printWriter.print(this.mBaseDisplayHeight);
            printWriter.print(" ");
            printWriter.print(this.mBaseDisplayDensity);
            printWriter.print("dpi");
        }
        if (this.mDisplayScalingDisabled) {
            printWriter.println(" noscale");
        }
        printWriter.print(" cur=");
        printWriter.print(this.mDisplayInfo.logicalWidth);
        printWriter.print("x");
        printWriter.print(this.mDisplayInfo.logicalHeight);
        printWriter.print(" app=");
        printWriter.print(this.mDisplayInfo.appWidth);
        printWriter.print("x");
        printWriter.print(this.mDisplayInfo.appHeight);
        printWriter.print(" rng=");
        printWriter.print(this.mDisplayInfo.smallestNominalAppWidth);
        printWriter.print("x");
        printWriter.print(this.mDisplayInfo.smallestNominalAppHeight);
        printWriter.print("-");
        printWriter.print(this.mDisplayInfo.largestNominalAppWidth);
        printWriter.print("x");
        printWriter.println(this.mDisplayInfo.largestNominalAppHeight);
        printWriter.print(str2 + "deferred=" + this.mDeferredRemoval + " mLayoutNeeded=" + this.mLayoutNeeded);
        StringBuilder sb = new StringBuilder();
        sb.append(" mTouchExcludeRegion=");
        sb.append(this.mTouchExcludeRegion);
        printWriter.println(sb.toString());
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mLayoutSeq=");
        printWriter.println(this.mLayoutSeq);
        printWriter.println();
        printWriter.println(str + "Application tokens in top down Z order:");
        for (int childCount = this.mTaskStackContainers.getChildCount() + (-1); childCount >= 0; childCount += -1) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(childCount)).dump(printWriter, str + "  ", z);
        }
        printWriter.println();
        if (!this.mExitingTokens.isEmpty()) {
            printWriter.println();
            printWriter.println("  Exiting tokens:");
            for (int size = this.mExitingTokens.size() - 1; size >= 0; size--) {
                WindowToken windowToken = this.mExitingTokens.get(size);
                printWriter.print("  Exiting #");
                printWriter.print(size);
                printWriter.print(' ');
                printWriter.print(windowToken);
                printWriter.println(':');
                windowToken.dump(printWriter, "    ", z);
            }
        }
        printWriter.println();
        TaskStack homeStack = getHomeStack();
        if (homeStack != null) {
            printWriter.println(str + "homeStack=" + homeStack.getName());
        }
        TaskStack pinnedStack = getPinnedStack();
        if (pinnedStack != null) {
            printWriter.println(str + "pinnedStack=" + pinnedStack.getName());
        }
        TaskStack splitScreenPrimaryStack = getSplitScreenPrimaryStack();
        if (splitScreenPrimaryStack != null) {
            printWriter.println(str + "splitScreenPrimaryStack=" + splitScreenPrimaryStack.getName());
        }
        printWriter.println();
        this.mDividerControllerLocked.dump(str, printWriter);
        printWriter.println();
        this.mPinnedStackControllerLocked.dump(str, printWriter);
        printWriter.println();
        this.mDisplayFrames.dump(str, printWriter);
    }

    public String toString() {
        return "Display " + this.mDisplayId + " info=" + this.mDisplayInfo + " stacks=" + this.mChildren;
    }

    @Override
    String getName() {
        return "Display " + this.mDisplayId + " name=\"" + this.mDisplayInfo.name + "\"";
    }

    boolean isStackVisible(int i) {
        TaskStack topStackInWindowingMode = getTopStackInWindowingMode(i);
        return topStackInWindowingMode != null && topStackInWindowingMode.isVisible();
    }

    WindowState getTouchableWinAtPointLocked(float f, float f2) {
        final int i = (int) f;
        final int i2 = (int) f2;
        return getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DisplayContent.lambda$getTouchableWinAtPointLocked$13(this.f$0, i, i2, (WindowState) obj);
            }
        });
    }

    public static boolean lambda$getTouchableWinAtPointLocked$13(DisplayContent displayContent, int i, int i2, WindowState windowState) {
        int i3 = windowState.mAttrs.flags;
        if (!windowState.isVisibleLw() || (i3 & 16) != 0) {
            return false;
        }
        windowState.getVisibleBounds(displayContent.mTmpRect);
        if (!displayContent.mTmpRect.contains(i, i2)) {
            return false;
        }
        windowState.getTouchableRegion(displayContent.mTmpRegion);
        return displayContent.mTmpRegion.contains(i, i2) || (i3 & 40) == 0;
    }

    boolean canAddToastWindowForUid(final int i) {
        return getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DisplayContent.lambda$canAddToastWindowForUid$14(i, (WindowState) obj);
            }
        }) != null || getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DisplayContent.lambda$canAddToastWindowForUid$15(i, (WindowState) obj);
            }
        }) == null;
    }

    static boolean lambda$canAddToastWindowForUid$14(int i, WindowState windowState) {
        return windowState.mOwnerUid == i && windowState.isFocused();
    }

    static boolean lambda$canAddToastWindowForUid$15(int i, WindowState windowState) {
        return windowState.mAttrs.type == 2005 && windowState.mOwnerUid == i && !windowState.mPermanentlyHidden && !windowState.mWindowRemovalAllowed;
    }

    void scheduleToastWindowsTimeoutIfNeededLocked(WindowState windowState, WindowState windowState2) {
        if (windowState != null) {
            if (windowState2 != null && windowState2.mOwnerUid == windowState.mOwnerUid) {
                return;
            }
            this.mTmpWindow = windowState;
            forAllWindows(this.mScheduleToastTimeout, false);
        }
    }

    WindowState findFocusedWindow() {
        this.mTmpWindow = null;
        forAllWindows(this.mFindFocusedWindow, true);
        if (this.mTmpWindow == null) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                Slog.v("WindowManager", "findFocusedWindow: No focusable windows.");
            }
            return null;
        }
        return this.mTmpWindow;
    }

    void assignWindowLayers(boolean z) {
        Trace.traceBegin(32L, "assignWindowLayers");
        assignChildLayers(getPendingTransaction());
        if (z) {
            setLayoutNeeded();
        }
        scheduleAnimation();
        Trace.traceEnd(32L);
    }

    void layoutAndAssignWindowLayersIfNeeded() {
        this.mService.mWindowsChanged = true;
        setLayoutNeeded();
        if (!this.mService.updateFocusedWindowLocked(3, false)) {
            assignWindowLayers(false);
        }
        this.mService.mInputMonitor.setUpdateInputWindowsNeededLw();
        this.mService.mWindowPlacerLocked.performSurfacePlacement();
        this.mService.mInputMonitor.updateInputWindowsLw(false);
    }

    boolean destroyLeakedSurfaces() {
        this.mTmpWindow = null;
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$destroyLeakedSurfaces$16(this.f$0, (WindowState) obj);
            }
        }, false);
        return this.mTmpWindow != null;
    }

    public static void lambda$destroyLeakedSurfaces$16(DisplayContent displayContent, WindowState windowState) {
        WindowStateAnimator windowStateAnimator = windowState.mWinAnimator;
        if (windowStateAnimator.mSurfaceController == null) {
            return;
        }
        if (!displayContent.mService.mSessions.contains(windowStateAnimator.mSession)) {
            Slog.w("WindowManager", "LEAKED SURFACE (session doesn't exist): " + windowState + " surface=" + windowStateAnimator.mSurfaceController + " token=" + windowState.mToken + " pid=" + windowState.mSession.mPid + " uid=" + windowState.mSession.mUid);
            windowStateAnimator.destroySurface();
            displayContent.mService.mForceRemoves.add(windowState);
            displayContent.mTmpWindow = windowState;
            return;
        }
        if (windowState.mAppToken != null && windowState.mAppToken.isClientHidden()) {
            Slog.w("WindowManager", "LEAKED SURFACE (app token hidden): " + windowState + " surface=" + windowStateAnimator.mSurfaceController + " token=" + windowState.mAppToken);
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                WindowManagerService.logSurface(windowState, "LEAK DESTROY", false);
            }
            windowStateAnimator.destroySurface();
            displayContent.mTmpWindow = windowState;
        }
    }

    WindowState computeImeTarget(boolean z) {
        AppWindowToken appWindowToken;
        String str;
        String str2;
        AppWindowToken appWindowToken2;
        WindowState imeTargetBelowWindow;
        if (this.mService.mInputMethodWindow == null) {
            if (z) {
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    Slog.w("WindowManager", "Moving IM target from " + this.mService.mInputMethodTarget + " to null since mInputMethodWindow is null");
                }
                setInputMethodTarget(null, this.mService.mInputMethodTargetWaitingAnim);
            }
            return null;
        }
        WindowState windowState = this.mService.mInputMethodTarget;
        if (!canUpdateImeTarget()) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.w("WindowManager", "Defer updating IME target");
            }
            return windowState;
        }
        this.mUpdateImeTarget = z;
        WindowState window = getWindow(this.mComputeImeTargetPredicate);
        if (window != null && window.mAttrs.type == 3 && (appWindowToken2 = window.mAppToken) != null && (imeTargetBelowWindow = appWindowToken2.getImeTargetBelowWindow(window)) != null) {
            window = imeTargetBelowWindow;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD && z) {
            Slog.v("WindowManager", "Proposed new IME target: " + window);
        }
        if (windowState != null && windowState.isDisplayedLw() && windowState.isClosing() && (window == null || window.isActivityTypeHome())) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.v("WindowManager", "New target is home while current target is closing, not changing");
            }
            return windowState;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.v("WindowManager", "Desired input method target=" + window + " updateImeTarget=" + z);
        }
        if (window == null) {
            if (z) {
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Moving IM target from ");
                    sb.append(windowState);
                    sb.append(" to null.");
                    if (WindowManagerDebugConfig.SHOW_STACK_CRAWLS) {
                        str2 = " Callers=" + Debug.getCallers(4);
                    } else {
                        str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    }
                    sb.append(str2);
                    Slog.w("WindowManager", sb.toString());
                }
                setInputMethodTarget(null, this.mService.mInputMethodTargetWaitingAnim);
            }
            return null;
        }
        if (z) {
            if (windowState != null) {
                appWindowToken = windowState.mAppToken;
            } else {
                appWindowToken = null;
            }
            if (appWindowToken != null) {
                WindowState highestAnimLayerWindow = appWindowToken.isSelfAnimating() ? appWindowToken.getHighestAnimLayerWindow(windowState) : null;
                if (highestAnimLayerWindow != null) {
                    AppTransition appTransition = this.mService.mAppTransition;
                    if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                        Slog.v("WindowManager", appTransition + " " + highestAnimLayerWindow + " animating=" + highestAnimLayerWindow.mWinAnimator.isAnimationSet() + " layer=" + highestAnimLayerWindow.mWinAnimator.mAnimLayer + " new layer=" + window.mWinAnimator.mAnimLayer);
                    }
                    if (appTransition.isTransitionSet()) {
                        setInputMethodTarget(highestAnimLayerWindow, true);
                        return highestAnimLayerWindow;
                    }
                    if (highestAnimLayerWindow.mWinAnimator.isAnimationSet() && highestAnimLayerWindow.mWinAnimator.mAnimLayer > window.mWinAnimator.mAnimLayer) {
                        setInputMethodTarget(highestAnimLayerWindow, true);
                        return highestAnimLayerWindow;
                    }
                }
            }
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Moving IM target from ");
                sb2.append(windowState);
                sb2.append(" to ");
                sb2.append(window);
                if (WindowManagerDebugConfig.SHOW_STACK_CRAWLS) {
                    str = " Callers=" + Debug.getCallers(4);
                } else {
                    str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                }
                sb2.append(str);
                Slog.w("WindowManager", sb2.toString());
            }
            setInputMethodTarget(window, false);
        }
        return window;
    }

    private void setInputMethodTarget(WindowState windowState, boolean z) {
        if (windowState == this.mService.mInputMethodTarget && this.mService.mInputMethodTargetWaitingAnim == z) {
            return;
        }
        this.mService.mInputMethodTarget = windowState;
        this.mService.mInputMethodTargetWaitingAnim = z;
        assignWindowLayers(false);
    }

    boolean getNeedsMenu(final WindowState windowState, final WindowManagerPolicy.WindowState windowState2) {
        if (windowState.mAttrs.needsMenuKey != 0) {
            return windowState.mAttrs.needsMenuKey == 1;
        }
        this.mTmpWindow = null;
        WindowState window = getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DisplayContent.lambda$getNeedsMenu$17(this.f$0, windowState, windowState2, (WindowState) obj);
            }
        });
        return window != null && window.mAttrs.needsMenuKey == 1;
    }

    public static boolean lambda$getNeedsMenu$17(DisplayContent displayContent, WindowState windowState, WindowManagerPolicy.WindowState windowState2, WindowState windowState3) {
        if (windowState3 == windowState) {
            displayContent.mTmpWindow = windowState3;
        }
        if (displayContent.mTmpWindow == null) {
            return false;
        }
        return windowState3.mAttrs.needsMenuKey != 0 || windowState3 == windowState2;
    }

    void setLayoutNeeded() {
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.w("WindowManager", "setLayoutNeeded: callers=" + Debug.getCallers(3));
        }
        this.mLayoutNeeded = true;
    }

    private void clearLayoutNeeded() {
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.w("WindowManager", "clearLayoutNeeded: callers=" + Debug.getCallers(3));
        }
        this.mLayoutNeeded = false;
    }

    boolean isLayoutNeeded() {
        return this.mLayoutNeeded;
    }

    void dumpTokens(PrintWriter printWriter, boolean z) {
        if (this.mTokenMap.isEmpty()) {
            return;
        }
        printWriter.println("  Display #" + this.mDisplayId);
        for (WindowToken windowToken : this.mTokenMap.values()) {
            printWriter.print("  ");
            printWriter.print(windowToken);
            if (z) {
                printWriter.println(':');
                windowToken.dump(printWriter, "    ", z);
            } else {
                printWriter.println();
            }
        }
    }

    void dumpWindowAnimators(final PrintWriter printWriter, final String str) {
        final int[] iArr = new int[1];
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$dumpWindowAnimators$18(printWriter, str, iArr, (WindowState) obj);
            }
        }, false);
    }

    static void lambda$dumpWindowAnimators$18(PrintWriter printWriter, String str, int[] iArr, WindowState windowState) {
        printWriter.println(str + "Window #" + iArr[0] + ": " + windowState.mWinAnimator);
        iArr[0] = iArr[0] + 1;
    }

    void startKeyguardExitOnNonAppWindows(final boolean z, final boolean z2) {
        final WindowManagerPolicy windowManagerPolicy = this.mService.mPolicy;
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$startKeyguardExitOnNonAppWindows$19(windowManagerPolicy, z, z2, (WindowState) obj);
            }
        }, true);
    }

    static void lambda$startKeyguardExitOnNonAppWindows$19(WindowManagerPolicy windowManagerPolicy, boolean z, boolean z2, WindowState windowState) {
        if (windowState.mAppToken == null && windowManagerPolicy.canBeHiddenByKeyguardLw(windowState) && windowState.wouldBeVisibleIfPolicyIgnored() && !windowState.isVisible()) {
            windowState.startAnimation(windowManagerPolicy.createHiddenByKeyguardExit(z, z2));
        }
    }

    boolean checkWaitingForWindows() {
        this.mHaveBootMsg = false;
        this.mHaveApp = false;
        this.mHaveWallpaper = false;
        this.mHaveKeyguard = true;
        if (getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DisplayContent.lambda$checkWaitingForWindows$20(this.f$0, (WindowState) obj);
            }
        }) != null) {
            return true;
        }
        boolean z = this.mService.mContext.getResources().getBoolean(R.^attr-private.horizontalProgressLayout) && this.mService.mContext.getResources().getBoolean(R.^attr-private.cornerRadius) && !this.mService.mOnlyCore;
        if (WindowManagerDebugConfig.DEBUG_SCREEN_ON || WindowManagerDebugConfig.DEBUG_BOOT) {
            Slog.i("WindowManager", "******** booted=" + this.mService.mSystemBooted + " msg=" + this.mService.mShowingBootMessages + " haveBoot=" + this.mHaveBootMsg + " haveApp=" + this.mHaveApp + " haveWall=" + this.mHaveWallpaper + " wallEnabled=" + z + " haveKeyguard=" + this.mHaveKeyguard);
        }
        if (!this.mService.mSystemBooted && !this.mHaveBootMsg) {
            return true;
        }
        if (!this.mService.mSystemBooted || ((this.mHaveApp || this.mHaveKeyguard) && (!z || this.mHaveWallpaper))) {
            return false;
        }
        return true;
    }

    public static boolean lambda$checkWaitingForWindows$20(DisplayContent displayContent, WindowState windowState) {
        if (windowState.isVisibleLw() && !windowState.mObscured && !windowState.isDrawnLw()) {
            return true;
        }
        if (windowState.isDrawnLw()) {
            if (windowState.mAttrs.type == 2021) {
                displayContent.mHaveBootMsg = true;
                return false;
            }
            if (windowState.mAttrs.type == 2 || windowState.mAttrs.type == 4) {
                displayContent.mHaveApp = true;
                return false;
            }
            if (windowState.mAttrs.type == 2013) {
                displayContent.mHaveWallpaper = true;
                return false;
            }
            if (windowState.mAttrs.type == 2000) {
                displayContent.mHaveKeyguard = displayContent.mService.mPolicy.isKeyguardDrawnLw();
                return false;
            }
            return false;
        }
        return false;
    }

    void updateWindowsForAnimator(WindowAnimator windowAnimator) {
        this.mTmpWindowAnimator = windowAnimator;
        forAllWindows(this.mUpdateWindowsForAnimator, true);
    }

    void updateWallpaperForAnimator(WindowAnimator windowAnimator) {
        resetAnimationBackgroundAnimator();
        this.mTmpWindow = null;
        this.mTmpWindowAnimator = windowAnimator;
        forAllWindows(this.mUpdateWallpaperForAnimator, true);
        if (windowAnimator.mWindowDetachedWallpaper != this.mTmpWindow) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "Detached wallpaper changed from " + windowAnimator.mWindowDetachedWallpaper + " to " + this.mTmpWindow);
            }
            windowAnimator.mWindowDetachedWallpaper = this.mTmpWindow;
            windowAnimator.mBulkUpdateParams |= 2;
        }
    }

    boolean inputMethodClientHasFocus(IInputMethodClient iInputMethodClient) {
        WindowState windowStateComputeImeTarget = computeImeTarget(false);
        if (windowStateComputeImeTarget == null) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.i("WindowManager", "Desired input method target: " + windowStateComputeImeTarget);
            Slog.i("WindowManager", "Current focus: " + this.mService.mCurrentFocus);
            Slog.i("WindowManager", "Last focus: " + this.mService.mLastFocus);
        }
        IInputMethodClient iInputMethodClient2 = windowStateComputeImeTarget.mSession.mClient;
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.i("WindowManager", "IM target client: " + iInputMethodClient2);
            if (iInputMethodClient2 != null) {
                Slog.i("WindowManager", "IM target client binder: " + iInputMethodClient2.asBinder());
                Slog.i("WindowManager", "Requesting client binder: " + iInputMethodClient.asBinder());
            }
        }
        return iInputMethodClient2 != null && iInputMethodClient2.asBinder() == iInputMethodClient.asBinder();
    }

    boolean hasSecureWindowOnScreen() {
        return getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DisplayContent.lambda$hasSecureWindowOnScreen$21((WindowState) obj);
            }
        }) != null;
    }

    static boolean lambda$hasSecureWindowOnScreen$21(WindowState windowState) {
        return windowState.isOnScreen() && (windowState.mAttrs.flags & 8192) != 0;
    }

    void updateSystemUiVisibility(final int i, final int i2) {
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$updateSystemUiVisibility$22(i, i2, (WindowState) obj);
            }
        }, true);
    }

    static void lambda$updateSystemUiVisibility$22(int i, int i2, WindowState windowState) {
        try {
            int i3 = windowState.mSystemUiVisibility;
            int i4 = i2 & (i3 ^ i);
            int i5 = ((~i4) & i3) | (i & i4);
            if (i5 != i3) {
                windowState.mSeq++;
                windowState.mSystemUiVisibility = i5;
            }
            if (i5 != i3 || windowState.mAttrs.hasSystemUiListeners) {
                windowState.mClient.dispatchSystemUiVisibilityChanged(windowState.mSeq, i, i5, i4);
            }
        } catch (RemoteException e) {
        }
    }

    void onWindowFreezeTimeout() {
        Slog.w("WindowManager", "Window freeze timeout expired.");
        this.mService.mWindowsFreezingScreen = 2;
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$onWindowFreezeTimeout$23(this.f$0, (WindowState) obj);
            }
        }, true);
        this.mService.mWindowPlacerLocked.performSurfacePlacement();
    }

    public static void lambda$onWindowFreezeTimeout$23(DisplayContent displayContent, WindowState windowState) {
        if (!windowState.getOrientationChanging()) {
            return;
        }
        windowState.orientationChangeTimedOut();
        windowState.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - displayContent.mService.mDisplayFreezeTime);
        Slog.w("WindowManager", "Force clearing orientation change: " + windowState);
    }

    void waitForAllWindowsDrawn() {
        final WindowManagerPolicy windowManagerPolicy = this.mService.mPolicy;
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$waitForAllWindowsDrawn$24(this.f$0, windowManagerPolicy, (WindowState) obj);
            }
        }, true);
    }

    public static void lambda$waitForAllWindowsDrawn$24(DisplayContent displayContent, WindowManagerPolicy windowManagerPolicy, WindowState windowState) {
        boolean zIsKeyguardHostWindow = windowManagerPolicy.isKeyguardHostWindow(windowState.mAttrs);
        if (windowState.isVisibleLw()) {
            if (windowState.mAppToken != null || zIsKeyguardHostWindow) {
                windowState.mWinAnimator.mDrawState = 1;
                windowState.mLastContentInsets.set(-1, -1, -1, -1);
                displayContent.mService.mWaitingForDrawn.add(windowState);
            }
        }
    }

    boolean applySurfaceChangesTransaction(boolean z) {
        int i = this.mDisplayInfo.logicalWidth;
        int i2 = this.mDisplayInfo.logicalHeight;
        WindowSurfacePlacer windowSurfacePlacer = this.mService.mWindowPlacerLocked;
        this.mTmpUpdateAllDrawn.clear();
        int i3 = 0;
        while (true) {
            i3++;
            if (i3 > 6) {
                Slog.w("WindowManager", "Animation repeat aborted after too many iterations");
                clearLayoutNeeded();
                break;
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                windowSurfacePlacer.debugLayoutRepeats("On entry to LockedInner", this.pendingLayoutChanges);
            }
            if (this.isDefaultDisplay && (this.pendingLayoutChanges & 4) != 0) {
                this.mWallpaperController.adjustWallpaperWindows(this);
            }
            if (this.isDefaultDisplay && (this.pendingLayoutChanges & 2) != 0) {
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    Slog.v("WindowManager", "Computing new config from layout");
                }
                if (this.mService.updateOrientationFromAppTokensLocked(this.mDisplayId)) {
                    setLayoutNeeded();
                    this.mService.mH.obtainMessage(18, Integer.valueOf(this.mDisplayId)).sendToTarget();
                }
            }
            if ((this.pendingLayoutChanges & 1) != 0) {
                setLayoutNeeded();
            }
            if (i3 < 4) {
                performLayout(i3 == 1, false);
            } else {
                Slog.w("WindowManager", "Layout repeat skipped after too many iterations");
            }
            this.pendingLayoutChanges = 0;
            if (this.isDefaultDisplay) {
                this.mService.mPolicy.beginPostLayoutPolicyLw(i, i2);
                forAllWindows(this.mApplyPostLayoutPolicy, true);
                this.pendingLayoutChanges |= this.mService.mPolicy.finishPostLayoutPolicyLw();
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    windowSurfacePlacer.debugLayoutRepeats("after finishPostLayoutPolicyLw", this.pendingLayoutChanges);
                }
            }
            if (this.pendingLayoutChanges == 0) {
                break;
            }
        }
        this.mTmpApplySurfaceChangesTransactionState.reset();
        this.mTmpRecoveringMemory = z;
        forAllWindows(this.mApplySurfaceChangesTransaction, true);
        prepareSurfaces();
        this.mService.mDisplayManagerInternal.setDisplayProperties(this.mDisplayId, this.mTmpApplySurfaceChangesTransactionState.displayHasContent, this.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate, this.mTmpApplySurfaceChangesTransactionState.preferredModeId, true);
        boolean zIsWallpaperVisible = this.mWallpaperController.isWallpaperVisible();
        if (zIsWallpaperVisible != this.mLastWallpaperVisible) {
            this.mLastWallpaperVisible = zIsWallpaperVisible;
            this.mService.mWallpaperVisibilityListeners.notifyWallpaperVisibilityChanged(this);
        }
        while (!this.mTmpUpdateAllDrawn.isEmpty()) {
            this.mTmpUpdateAllDrawn.removeLast().updateAllDrawn();
        }
        return this.mTmpApplySurfaceChangesTransactionState.focusDisplayed;
    }

    private void updateBounds() {
        calculateBounds(this.mTmpBounds);
        setBounds(this.mTmpBounds);
    }

    private void calculateBounds(Rect rect) {
        int i = this.mDisplayInfo.rotation;
        boolean z = true;
        if (i != 1 && i != 3) {
            z = false;
        }
        int i2 = z ? this.mBaseDisplayHeight : this.mBaseDisplayWidth;
        int i3 = z ? this.mBaseDisplayWidth : this.mBaseDisplayHeight;
        int i4 = this.mDisplayInfo.logicalWidth;
        int i5 = (i2 - i4) / 2;
        int i6 = this.mDisplayInfo.logicalHeight;
        int i7 = (i3 - i6) / 2;
        rect.set(i5, i7, i4 + i5, i6 + i7);
    }

    @Override
    public void getBounds(Rect rect) {
        calculateBounds(rect);
    }

    private void getBounds(Rect rect, int i) {
        getBounds(rect);
        int iDeltaRotation = deltaRotation(this.mDisplayInfo.rotation, i);
        if (iDeltaRotation == 1 || iDeltaRotation == 3) {
            createRotationMatrix(iDeltaRotation, this.mBaseDisplayWidth, this.mBaseDisplayHeight, this.mTmpMatrix);
            this.mTmpRectF.set(rect);
            this.mTmpMatrix.mapRect(this.mTmpRectF);
            this.mTmpRectF.round(rect);
        }
    }

    void performLayout(boolean z, boolean z2) {
        if (!isLayoutNeeded()) {
            return;
        }
        clearLayoutNeeded();
        int i = this.mDisplayInfo.logicalWidth;
        int i2 = this.mDisplayInfo.logicalHeight;
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.v("WindowManager", "-------------------------------------");
            Slog.v("WindowManager", "performLayout: needed=" + isLayoutNeeded() + " dw=" + i + " dh=" + i2);
        }
        this.mDisplayFrames.onDisplayInfoUpdated(this.mDisplayInfo, calculateDisplayCutoutForRotation(this.mDisplayInfo.rotation));
        this.mDisplayFrames.mRotation = this.mRotation;
        this.mService.mPolicy.beginLayoutLw(this.mDisplayFrames, getConfiguration().uiMode);
        if (this.isDefaultDisplay) {
            this.mService.mSystemDecorLayer = this.mService.mPolicy.getSystemDecorLayerLw();
            this.mService.mScreenRect.set(0, 0, i, i2);
        }
        int i3 = this.mLayoutSeq + 1;
        if (i3 < 0) {
            i3 = 0;
        }
        this.mLayoutSeq = i3;
        this.mTmpWindow = null;
        this.mTmpInitial = z;
        forAllWindows(this.mPerformLayout, true);
        this.mTmpWindow2 = this.mTmpWindow;
        this.mTmpWindow = null;
        forAllWindows(this.mPerformLayoutAttached, true);
        this.mService.mInputMonitor.layoutInputConsumers(i, i2);
        this.mService.mInputMonitor.setUpdateInputWindowsNeededLw();
        if (z2) {
            this.mService.mInputMonitor.updateInputWindowsLw(false);
        }
        this.mService.mPolicy.finishLayoutLw();
        this.mService.mH.sendEmptyMessage(41);
    }

    Bitmap screenshotDisplayLocked(Bitmap.Config config) {
        int i;
        if (!this.mService.mPolicy.isScreenOn()) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.i("WindowManager", "Attempted to take screenshot while display was off.");
            }
            return null;
        }
        int i2 = this.mDisplayInfo.logicalWidth;
        int i3 = this.mDisplayInfo.logicalHeight;
        if (i2 <= 0 || i3 <= 0) {
            return null;
        }
        Rect rect = new Rect(0, 0, i2, i3);
        int rotation = this.mDisplay.getRotation();
        int i4 = 3;
        if (rotation == 1 || rotation == 3) {
            if (rotation != 1) {
                i4 = 1;
            }
            i = i4;
        } else {
            i = rotation;
        }
        convertCropForSurfaceFlinger(rect, i, i2, i3);
        ScreenRotationAnimation screenRotationAnimationLocked = this.mService.mAnimator.getScreenRotationAnimationLocked(0);
        boolean z = screenRotationAnimationLocked != null && screenRotationAnimationLocked.isAnimating();
        if (WindowManagerDebugConfig.DEBUG_SCREENSHOT && z) {
            Slog.v("WindowManager", "Taking screenshot while rotating");
        }
        Bitmap bitmapScreenshot = SurfaceControl.screenshot(rect, i2, i3, 0, 1, z, i);
        if (bitmapScreenshot == null) {
            Slog.w("WindowManager", "Failed to take screenshot");
            return null;
        }
        Bitmap bitmapCreateAshmemBitmap = bitmapScreenshot.createAshmemBitmap(config);
        bitmapScreenshot.recycle();
        return bitmapCreateAshmemBitmap;
    }

    private static void convertCropForSurfaceFlinger(Rect rect, int i, int i2, int i3) {
        if (i == 1) {
            int i4 = rect.top;
            rect.top = i2 - rect.right;
            rect.right = rect.bottom;
            rect.bottom = i2 - rect.left;
            rect.left = i4;
            return;
        }
        if (i == 2) {
            int i5 = rect.top;
            rect.top = i3 - rect.bottom;
            rect.bottom = i3 - i5;
            int i6 = rect.right;
            rect.right = i2 - rect.left;
            rect.left = i2 - i6;
            return;
        }
        if (i == 3) {
            int i7 = rect.top;
            rect.top = rect.left;
            rect.left = i3 - rect.bottom;
            rect.bottom = rect.right;
            rect.right = i3 - i7;
        }
    }

    void onSeamlessRotationTimeout() {
        this.mTmpWindow = null;
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                DisplayContent.lambda$onSeamlessRotationTimeout$25(this.f$0, (WindowState) obj);
            }
        }, true);
        if (this.mTmpWindow != null) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    public static void lambda$onSeamlessRotationTimeout$25(DisplayContent displayContent, WindowState windowState) {
        if (!windowState.mSeamlesslyRotated) {
            return;
        }
        displayContent.mTmpWindow = windowState;
        windowState.setDisplayLayoutNeeded();
        displayContent.mService.markForSeamlessRotation(windowState, false);
    }

    void setExitingTokensHasVisible(boolean z) {
        for (int size = this.mExitingTokens.size() - 1; size >= 0; size--) {
            this.mExitingTokens.get(size).hasVisible = z;
        }
        this.mTaskStackContainers.setExitingTokensHasVisible(z);
    }

    void removeExistingTokensIfPossible() {
        for (int size = this.mExitingTokens.size() - 1; size >= 0; size--) {
            if (!this.mExitingTokens.get(size).hasVisible) {
                this.mExitingTokens.remove(size);
            }
        }
        this.mTaskStackContainers.removeExistingAppTokensIfPossible();
    }

    @Override
    void onDescendantOverrideConfigurationChanged() {
        setLayoutNeeded();
        this.mService.requestTraversal();
    }

    boolean okToDisplay() {
        return this.mDisplayId == 0 ? !this.mService.mDisplayFrozen && this.mService.mDisplayEnabled && this.mService.mPolicy.isScreenOn() : this.mDisplayInfo.state == 2;
    }

    boolean okToAnimate() {
        return okToDisplay() && (this.mDisplayId != 0 || this.mService.mPolicy.okToAnimate());
    }

    static final class TaskForResizePointSearchResult {
        boolean searchDone;
        Task taskForResize;

        TaskForResizePointSearchResult() {
        }

        void reset() {
            this.searchDone = false;
            this.taskForResize = null;
        }
    }

    private static final class ApplySurfaceChangesTransactionState {
        boolean displayHasContent;
        boolean focusDisplayed;
        boolean obscured;
        int preferredModeId;
        float preferredRefreshRate;
        boolean syswin;

        private ApplySurfaceChangesTransactionState() {
        }

        void reset() {
            this.displayHasContent = false;
            this.obscured = false;
            this.syswin = false;
            this.focusDisplayed = false;
            this.preferredRefreshRate = 0.0f;
            this.preferredModeId = 0;
        }
    }

    private static final class ScreenshotApplicationState {
        WindowState appWin;
        int maxLayer;
        int minLayer;
        boolean screenshotReady;

        private ScreenshotApplicationState() {
        }

        void reset(boolean z) {
            this.appWin = null;
            this.maxLayer = 0;
            this.minLayer = 0;
            this.screenshotReady = z;
            this.minLayer = z ? 0 : Integer.MAX_VALUE;
        }
    }

    static class DisplayChildWindowContainer<E extends WindowContainer> extends WindowContainer<E> {
        DisplayChildWindowContainer(WindowManagerService windowManagerService) {
            super(windowManagerService);
        }

        @Override
        boolean fillsParent() {
            return true;
        }

        @Override
        boolean isVisible() {
            return true;
        }
    }

    private final class TaskStackContainers extends DisplayChildWindowContainer<TaskStack> {
        SurfaceControl mAppAnimationLayer;
        SurfaceControl mBoostedAppAnimationLayer;
        SurfaceControl mHomeAppAnimationLayer;
        private TaskStack mHomeStack;
        private TaskStack mPinnedStack;
        SurfaceControl mSplitScreenDividerAnchor;
        private TaskStack mSplitScreenPrimaryStack;

        TaskStackContainers(WindowManagerService windowManagerService) {
            super(windowManagerService);
            this.mAppAnimationLayer = null;
            this.mBoostedAppAnimationLayer = null;
            this.mHomeAppAnimationLayer = null;
            this.mSplitScreenDividerAnchor = null;
            this.mHomeStack = null;
            this.mPinnedStack = null;
            this.mSplitScreenPrimaryStack = null;
        }

        TaskStack getStack(int i, int i2) {
            if (i2 == 2) {
                return this.mHomeStack;
            }
            if (i == 2) {
                return this.mPinnedStack;
            }
            if (i != 3) {
                for (int childCount = DisplayContent.this.mTaskStackContainers.getChildCount() - 1; childCount >= 0; childCount--) {
                    TaskStack taskStack = (TaskStack) DisplayContent.this.mTaskStackContainers.getChildAt(childCount);
                    if (i2 == 0 && i == taskStack.getWindowingMode()) {
                        return taskStack;
                    }
                    if (taskStack.isCompatible(i, i2)) {
                        return taskStack;
                    }
                }
                return null;
            }
            return this.mSplitScreenPrimaryStack;
        }

        @VisibleForTesting
        TaskStack getTopStack() {
            if (DisplayContent.this.mTaskStackContainers.getChildCount() > 0) {
                return (TaskStack) DisplayContent.this.mTaskStackContainers.getChildAt(DisplayContent.this.mTaskStackContainers.getChildCount() - 1);
            }
            return null;
        }

        TaskStack getHomeStack() {
            if (this.mHomeStack == null && DisplayContent.this.mDisplayId == 0) {
                Slog.e("WindowManager", "getHomeStack: Returning null from this=" + this);
            }
            return this.mHomeStack;
        }

        TaskStack getPinnedStack() {
            return this.mPinnedStack;
        }

        TaskStack getSplitScreenPrimaryStack() {
            return this.mSplitScreenPrimaryStack;
        }

        ArrayList<Task> getVisibleTasks() {
            final ArrayList<Task> arrayList = new ArrayList<>();
            forAllTasks(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    DisplayContent.TaskStackContainers.lambda$getVisibleTasks$0(arrayList, (Task) obj);
                }
            });
            return arrayList;
        }

        static void lambda$getVisibleTasks$0(ArrayList arrayList, Task task) {
            if (task.isVisible()) {
                arrayList.add(task);
            }
        }

        void addStackToDisplay(TaskStack taskStack, boolean z) {
            addStackReferenceIfNeeded(taskStack);
            addChild(taskStack, z);
            taskStack.onDisplayChanged(DisplayContent.this);
        }

        void onStackWindowingModeChanged(TaskStack taskStack) {
            removeStackReferenceIfNeeded(taskStack);
            addStackReferenceIfNeeded(taskStack);
            if (taskStack == this.mPinnedStack && getTopStack() != taskStack) {
                positionChildAt(Integer.MAX_VALUE, taskStack, false);
            }
        }

        private void addStackReferenceIfNeeded(TaskStack taskStack) {
            if (taskStack.isActivityTypeHome()) {
                if (this.mHomeStack != null) {
                    throw new IllegalArgumentException("addStackReferenceIfNeeded: home stack=" + this.mHomeStack + " already exist on display=" + this + " stack=" + taskStack);
                }
                this.mHomeStack = taskStack;
            }
            int windowingMode = taskStack.getWindowingMode();
            if (windowingMode == 2) {
                if (this.mPinnedStack != null) {
                    throw new IllegalArgumentException("addStackReferenceIfNeeded: pinned stack=" + this.mPinnedStack + " already exist on display=" + this + " stack=" + taskStack);
                }
                this.mPinnedStack = taskStack;
                return;
            }
            if (windowingMode == 3) {
                if (this.mSplitScreenPrimaryStack != null) {
                    throw new IllegalArgumentException("addStackReferenceIfNeeded: split-screen-primary stack=" + this.mSplitScreenPrimaryStack + " already exist on display=" + this + " stack=" + taskStack);
                }
                this.mSplitScreenPrimaryStack = taskStack;
                DisplayContent.this.mDividerControllerLocked.notifyDockedStackExistsChanged(true);
            }
        }

        private void removeStackReferenceIfNeeded(TaskStack taskStack) {
            if (taskStack == this.mHomeStack) {
                this.mHomeStack = null;
                return;
            }
            if (taskStack == this.mPinnedStack) {
                this.mPinnedStack = null;
            } else if (taskStack == this.mSplitScreenPrimaryStack) {
                this.mSplitScreenPrimaryStack = null;
                this.mService.setDockedStackCreateStateLocked(0, null);
                DisplayContent.this.mDividerControllerLocked.notifyDockedStackExistsChanged(false);
            }
        }

        private void addChild(TaskStack taskStack, boolean z) {
            addChild(taskStack, findPositionForStack(z ? this.mChildren.size() : 0, taskStack, true));
            DisplayContent.this.setLayoutNeeded();
        }

        @Override
        protected void removeChild(TaskStack taskStack) {
            super.removeChild(taskStack);
            removeStackReferenceIfNeeded(taskStack);
        }

        @Override
        boolean isOnTop() {
            return true;
        }

        @Override
        void positionChildAt(int i, TaskStack taskStack, boolean z) {
            if (taskStack.getWindowConfiguration().isAlwaysOnTop() && i != Integer.MAX_VALUE) {
                Slog.w("WindowManager", "Ignoring move of always-on-top stack=" + this + " to bottom");
                super.positionChildAt(this.mChildren.indexOf(taskStack), taskStack, false);
                return;
            }
            super.positionChildAt(findPositionForStack(i, taskStack, false), taskStack, z);
            DisplayContent.this.setLayoutNeeded();
        }

        private int findPositionForStack(int i, TaskStack taskStack, boolean z) {
            boolean z2 = true;
            int size = this.mChildren.size() - 1;
            boolean z3 = i == Integer.MAX_VALUE;
            if (!z ? i < size : i < size + 1) {
                z2 = false;
            }
            if ((z2 | z3) && taskStack.getWindowingMode() != 2 && DisplayContent.this.hasPinnedStack()) {
                if (((TaskStack) this.mChildren.get(size)).getWindowingMode() != 2) {
                    throw new IllegalStateException("Pinned stack isn't top stack??? " + this.mChildren);
                }
                if (!z) {
                    size--;
                }
                return size;
            }
            return i;
        }

        @Override
        boolean forAllWindows(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
            if (z) {
                if (super.forAllWindows(toBooleanFunction, z) || forAllExitingAppTokenWindows(toBooleanFunction, z)) {
                    return true;
                }
                return false;
            }
            if (forAllExitingAppTokenWindows(toBooleanFunction, z) || super.forAllWindows(toBooleanFunction, z)) {
                return true;
            }
            return false;
        }

        private boolean forAllExitingAppTokenWindows(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
            if (z) {
                for (int size = this.mChildren.size() - 1; size >= 0; size--) {
                    AppTokenList appTokenList = ((TaskStack) this.mChildren.get(size)).mExitingAppTokens;
                    for (int size2 = appTokenList.size() - 1; size2 >= 0; size2--) {
                        if (appTokenList.get(size2).forAllWindowsUnchecked(toBooleanFunction, z)) {
                            return true;
                        }
                    }
                }
            } else {
                int size3 = this.mChildren.size();
                for (int i = 0; i < size3; i++) {
                    AppTokenList appTokenList2 = ((TaskStack) this.mChildren.get(i)).mExitingAppTokens;
                    int size4 = appTokenList2.size();
                    for (int i2 = 0; i2 < size4; i2++) {
                        if (appTokenList2.get(i2).forAllWindowsUnchecked(toBooleanFunction, z)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        void setExitingTokensHasVisible(boolean z) {
            for (int size = this.mChildren.size() - 1; size >= 0; size--) {
                AppTokenList appTokenList = ((TaskStack) this.mChildren.get(size)).mExitingAppTokens;
                for (int size2 = appTokenList.size() - 1; size2 >= 0; size2--) {
                    appTokenList.get(size2).hasVisible = z;
                }
            }
        }

        void removeExistingAppTokensIfPossible() {
            for (int size = this.mChildren.size() - 1; size >= 0; size--) {
                AppTokenList appTokenList = ((TaskStack) this.mChildren.get(size)).mExitingAppTokens;
                for (int size2 = appTokenList.size() - 1; size2 >= 0; size2--) {
                    AppWindowToken appWindowToken = appTokenList.get(size2);
                    if (!appWindowToken.hasVisible && !this.mService.mClosingApps.contains(appWindowToken) && (!appWindowToken.mIsExiting || appWindowToken.isEmpty())) {
                        cancelAnimation();
                        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE || WindowManagerDebugConfig.DEBUG_TOKEN_MOVEMENT) {
                            Slog.v("WindowManager", "performLayout: App token exiting now removed" + appWindowToken);
                        }
                        appWindowToken.removeIfPossible();
                    }
                }
            }
        }

        @Override
        int getOrientation() {
            int orientation;
            if (DisplayContent.this.isStackVisible(3) || DisplayContent.this.isStackVisible(5)) {
                if (this.mHomeStack == null || !this.mHomeStack.isVisible() || !DisplayContent.this.mDividerControllerLocked.isMinimizedDock() || ((DisplayContent.this.mDividerControllerLocked.isHomeStackResizable() && this.mHomeStack.matchParentBounds()) || (orientation = this.mHomeStack.getOrientation()) == -2)) {
                    return -1;
                }
                return orientation;
            }
            int orientation2 = super.getOrientation();
            if (this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Forcing UNSPECIFIED orientation in car for display id=" + DisplayContent.this.mDisplayId + ". Ignoring " + orientation2);
                }
                return -1;
            }
            if (orientation2 != -2 && orientation2 != 3) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "App is requesting an orientation, return " + orientation2 + " for display id=" + DisplayContent.this.mDisplayId);
                }
                return orientation2;
            }
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "No app is requesting an orientation, return " + DisplayContent.this.mLastOrientation + " for display id=" + DisplayContent.this.mDisplayId);
            }
            return DisplayContent.this.mLastOrientation;
        }

        @Override
        void assignChildLayers(SurfaceControl.Transaction transaction) {
            assignStackOrdering(transaction);
            for (int i = 0; i < this.mChildren.size(); i++) {
                ((TaskStack) this.mChildren.get(i)).assignChildLayers(transaction);
            }
        }

        void assignStackOrdering(SurfaceControl.Transaction transaction) {
            int i;
            int i2 = 0;
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            int i6 = 0;
            while (i2 <= 2) {
                int i7 = i4;
                int i8 = i3;
                for (int i9 = 0; i9 < this.mChildren.size(); i9++) {
                    TaskStack taskStack = (TaskStack) this.mChildren.get(i9);
                    if ((i2 != 0 || taskStack.isActivityTypeHome()) && ((i2 != 1 || (!taskStack.isActivityTypeHome() && !taskStack.isAlwaysOnTop())) && (i2 != 2 || taskStack.isAlwaysOnTop()))) {
                        int i10 = i6 + 1;
                        taskStack.assignLayer(transaction, i6);
                        if (taskStack.inSplitScreenWindowingMode() && this.mSplitScreenDividerAnchor != null) {
                            transaction.setLayer(this.mSplitScreenDividerAnchor, i10);
                            i10++;
                        }
                        if ((taskStack.isTaskAnimating() || taskStack.isAppAnimating()) && i2 != 2) {
                            i = i10 + 1;
                        } else {
                            int i11 = i10;
                            i10 = i8;
                            i = i11;
                        }
                        if (i2 != 2) {
                            i6 = i + 1;
                            i7 = i;
                        } else {
                            i6 = i;
                        }
                        i8 = i10;
                    }
                }
                if (i2 == 0) {
                    i5 = i6;
                    i6++;
                }
                i2++;
                i3 = i8;
                i4 = i7;
            }
            if (this.mAppAnimationLayer != null) {
                transaction.setLayer(this.mAppAnimationLayer, i3);
            }
            if (this.mBoostedAppAnimationLayer != null) {
                transaction.setLayer(this.mBoostedAppAnimationLayer, i4);
            }
            if (this.mHomeAppAnimationLayer != null) {
                transaction.setLayer(this.mHomeAppAnimationLayer, i5);
            }
        }

        @Override
        SurfaceControl getAppAnimationLayer(@WindowContainer.AnimationLayer int i) {
            switch (i) {
                case 1:
                    return this.mBoostedAppAnimationLayer;
                case 2:
                    return this.mHomeAppAnimationLayer;
                default:
                    return this.mAppAnimationLayer;
            }
        }

        SurfaceControl getSplitScreenDividerAnchor() {
            return this.mSplitScreenDividerAnchor;
        }

        @Override
        void onParentSet() {
            super.onParentSet();
            if (getParent() != null) {
                this.mAppAnimationLayer = makeChildSurface(null).setName("animationLayer").build();
                this.mBoostedAppAnimationLayer = makeChildSurface(null).setName("boostedAnimationLayer").build();
                this.mHomeAppAnimationLayer = makeChildSurface(null).setName("homeAnimationLayer").build();
                this.mSplitScreenDividerAnchor = makeChildSurface(null).setName("splitScreenDividerAnchor").build();
                getPendingTransaction().show(this.mAppAnimationLayer).show(this.mBoostedAppAnimationLayer).show(this.mHomeAppAnimationLayer).show(this.mSplitScreenDividerAnchor);
                scheduleAnimation();
                return;
            }
            this.mAppAnimationLayer.destroy();
            this.mAppAnimationLayer = null;
            this.mBoostedAppAnimationLayer.destroy();
            this.mBoostedAppAnimationLayer = null;
            this.mHomeAppAnimationLayer.destroy();
            this.mHomeAppAnimationLayer = null;
            this.mSplitScreenDividerAnchor.destroy();
            this.mSplitScreenDividerAnchor = null;
        }
    }

    private final class AboveAppWindowContainers extends NonAppWindowContainers {
        AboveAppWindowContainers(String str, WindowManagerService windowManagerService) {
            super(str, windowManagerService);
        }

        @Override
        void assignChildLayers(SurfaceControl.Transaction transaction) {
            assignChildLayers(transaction, null);
        }

        void assignChildLayers(SurfaceControl.Transaction transaction, WindowContainer windowContainer) {
            boolean z = (windowContainer == null || windowContainer.getSurfaceControl() == null) ? false : true;
            for (int i = 0; i < this.mChildren.size(); i++) {
                WindowToken windowToken = (WindowToken) this.mChildren.get(i);
                if (windowToken.windowType == 2034) {
                    windowToken.assignRelativeLayer(transaction, DisplayContent.this.mTaskStackContainers.getSplitScreenDividerAnchor(), 1);
                } else {
                    windowToken.assignLayer(transaction, i);
                    windowToken.assignChildLayers(transaction);
                    int windowLayerFromTypeLw = this.mService.mPolicy.getWindowLayerFromTypeLw(windowToken.windowType, windowToken.mOwnerCanManageAppTokens);
                    if (z && windowLayerFromTypeLw >= this.mService.mPolicy.getWindowLayerFromTypeLw(2012, true)) {
                        windowContainer.assignRelativeLayer(transaction, windowToken.getSurfaceControl(), -1);
                        z = false;
                    }
                }
            }
            if (z) {
                windowContainer.assignRelativeLayer(transaction, getSurfaceControl(), Integer.MAX_VALUE);
            }
        }
    }

    private class NonAppWindowContainers extends DisplayChildWindowContainer<WindowToken> {
        private final Dimmer mDimmer;
        private final Predicate<WindowState> mGetOrientingWindow;
        private final String mName;
        private final Rect mTmpDimBoundsRect;
        private final Comparator<WindowToken> mWindowComparator;

        public static int lambda$new$0(NonAppWindowContainers nonAppWindowContainers, WindowToken windowToken, WindowToken windowToken2) {
            return nonAppWindowContainers.mService.mPolicy.getWindowLayerFromTypeLw(windowToken.windowType, windowToken.mOwnerCanManageAppTokens) < nonAppWindowContainers.mService.mPolicy.getWindowLayerFromTypeLw(windowToken2.windowType, windowToken2.mOwnerCanManageAppTokens) ? -1 : 1;
        }

        public static boolean lambda$new$1(NonAppWindowContainers nonAppWindowContainers, WindowState windowState) {
            int i;
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                nonAppWindowContainers.mService.mWindowManagerDebugger.debugGetOrientingWindow("WindowManager", windowState, windowState.mAttrs, windowState.isVisibleLw(), windowState.mPolicyVisibilityAfterAnim, windowState.mPolicyVisibility, windowState.mDestroying);
            }
            return (!windowState.isVisibleLw() || !windowState.mPolicyVisibilityAfterAnim || (i = windowState.mAttrs.screenOrientation) == -1 || i == 3 || i == -2) ? false : true;
        }

        NonAppWindowContainers(String str, WindowManagerService windowManagerService) {
            super(windowManagerService);
            this.mWindowComparator = new Comparator() {
                @Override
                public final int compare(Object obj, Object obj2) {
                    return DisplayContent.NonAppWindowContainers.lambda$new$0(this.f$0, (WindowToken) obj, (WindowToken) obj2);
                }
            };
            this.mGetOrientingWindow = new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return DisplayContent.NonAppWindowContainers.lambda$new$1(this.f$0, (WindowState) obj);
                }
            };
            this.mDimmer = new Dimmer(this);
            this.mTmpDimBoundsRect = new Rect();
            this.mName = str;
        }

        void addChild(WindowToken windowToken) {
            addChild(windowToken, this.mWindowComparator);
        }

        @Override
        int getOrientation() {
            WindowManagerPolicy windowManagerPolicy = this.mService.mPolicy;
            WindowState window = getWindow(this.mGetOrientingWindow);
            if (window == null) {
                DisplayContent.this.mLastWindowForcedOrientation = -1;
                boolean z = this.mService.mAppTransition.getAppTransition() == 23 && this.mService.mUnknownAppVisibilityController.allResolved();
                if (windowManagerPolicy.isKeyguardShowingAndNotOccluded() || z) {
                    return DisplayContent.this.mLastKeyguardForcedOrientation;
                }
                return -2;
            }
            int i = window.mAttrs.screenOrientation;
            if (windowManagerPolicy.isKeyguardHostWindow(window.mAttrs)) {
                DisplayContent.this.mLastKeyguardForcedOrientation = i;
                if (this.mService.mKeyguardGoingAway) {
                    DisplayContent.this.mLastWindowForcedOrientation = -1;
                    return -2;
                }
            }
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", window + " forcing orientation to " + i + " for display id=" + DisplayContent.this.mDisplayId);
            }
            return DisplayContent.this.mLastWindowForcedOrientation = i;
        }

        @Override
        String getName() {
            return this.mName;
        }

        @Override
        Dimmer getDimmer() {
            return this.mDimmer;
        }

        @Override
        void prepareSurfaces() {
            this.mDimmer.resetDimStates();
            super.prepareSurfaces();
            getBounds(this.mTmpDimBoundsRect);
            if (this.mDimmer.updateDims(getPendingTransaction(), this.mTmpDimBoundsRect)) {
                scheduleAnimation();
            }
        }
    }

    private class NonMagnifiableWindowContainers extends NonAppWindowContainers {
        NonMagnifiableWindowContainers(String str, WindowManagerService windowManagerService) {
            super(str, windowManagerService);
        }

        @Override
        void applyMagnificationSpec(SurfaceControl.Transaction transaction, MagnificationSpec magnificationSpec) {
        }
    }

    SurfaceControl.Builder makeSurface(SurfaceSession surfaceSession) {
        return this.mService.makeSurfaceBuilder(surfaceSession).setParent(this.mWindowingLayer);
    }

    @Override
    SurfaceSession getSession() {
        return this.mSession;
    }

    @Override
    SurfaceControl.Builder makeChildSurface(WindowContainer windowContainer) {
        SurfaceControl.Builder builderMakeSurfaceBuilder = this.mService.makeSurfaceBuilder(windowContainer != null ? windowContainer.getSession() : getSession());
        builderMakeSurfaceBuilder.setSize(this.mSurfaceSize, this.mSurfaceSize);
        builderMakeSurfaceBuilder.setContainerLayer(true);
        if (windowContainer == null) {
            return builderMakeSurfaceBuilder;
        }
        return builderMakeSurfaceBuilder.setName(windowContainer.getName()).setParent(this.mWindowingLayer);
    }

    SurfaceControl.Builder makeOverlay() {
        return this.mService.makeSurfaceBuilder(this.mSession).setParent(this.mOverlayLayer);
    }

    void reparentToOverlay(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        transaction.reparent(surfaceControl, this.mOverlayLayer.getHandle());
    }

    void applyMagnificationSpec(MagnificationSpec magnificationSpec) {
        if (magnificationSpec.scale != 1.0d) {
            this.mMagnificationSpec = magnificationSpec;
        } else {
            this.mMagnificationSpec = null;
        }
        applyMagnificationSpec(getPendingTransaction(), magnificationSpec);
        getPendingTransaction().apply();
    }

    void reapplyMagnificationSpec() {
        if (this.mMagnificationSpec != null) {
            applyMagnificationSpec(getPendingTransaction(), this.mMagnificationSpec);
        }
    }

    @Override
    void onParentSet() {
    }

    @Override
    void assignChildLayers(SurfaceControl.Transaction transaction) {
        boolean z = false;
        this.mBelowAppWindowsContainers.assignLayer(transaction, 0);
        this.mTaskStackContainers.assignLayer(transaction, 1);
        this.mAboveAppWindowsContainers.assignLayer(transaction, 2);
        WindowState windowState = this.mService.mInputMethodTarget;
        if (windowState != null && !windowState.inSplitScreenWindowingMode() && !windowState.mToken.isAppAnimating() && windowState.getSurfaceControl() != null) {
            this.mImeWindowsContainers.assignRelativeLayer(transaction, windowState.getSurfaceControl(), 1);
        } else {
            z = true;
        }
        this.mBelowAppWindowsContainers.assignChildLayers(transaction);
        this.mTaskStackContainers.assignChildLayers(transaction);
        this.mAboveAppWindowsContainers.assignChildLayers(transaction, z ? this.mImeWindowsContainers : null);
        this.mImeWindowsContainers.assignChildLayers(transaction);
    }

    void assignRelativeLayerForImeTargetChild(SurfaceControl.Transaction transaction, WindowContainer windowContainer) {
        windowContainer.assignRelativeLayer(transaction, this.mImeWindowsContainers.getSurfaceControl(), 1);
    }

    @Override
    void prepareSurfaces() {
        ScreenRotationAnimation screenRotationAnimationLocked = this.mService.mAnimator.getScreenRotationAnimationLocked(this.mDisplayId);
        if (screenRotationAnimationLocked != null && screenRotationAnimationLocked.isAnimating()) {
            screenRotationAnimationLocked.getEnterTransformation().getMatrix().getValues(this.mTmpFloats);
            this.mPendingTransaction.setMatrix(this.mWindowingLayer, this.mTmpFloats[0], this.mTmpFloats[3], this.mTmpFloats[1], this.mTmpFloats[4]);
            this.mPendingTransaction.setPosition(this.mWindowingLayer, this.mTmpFloats[2], this.mTmpFloats[5]);
            this.mPendingTransaction.setAlpha(this.mWindowingLayer, screenRotationAnimationLocked.getEnterTransformation().getAlpha());
        }
        super.prepareSurfaces();
    }

    void assignStackOrdering() {
        this.mTaskStackContainers.assignStackOrdering(getPendingTransaction());
    }

    void deferUpdateImeTarget() {
        this.mDeferUpdateImeTargetCount++;
    }

    void continueUpdateImeTarget() {
        if (this.mDeferUpdateImeTargetCount == 0) {
            return;
        }
        this.mDeferUpdateImeTargetCount--;
        if (this.mDeferUpdateImeTargetCount == 0) {
            computeImeTarget(true);
        }
    }

    private boolean canUpdateImeTarget() {
        return this.mDeferUpdateImeTargetCount == 0;
    }
}
