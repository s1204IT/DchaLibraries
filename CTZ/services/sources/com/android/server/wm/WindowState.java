package com.android.server.wm;

import android.R;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.IApplicationToken;
import android.view.IWindow;
import android.view.IWindowFocusObserver;
import android.view.IWindowId;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowInfo;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.input.InputWindowHandle;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.LocalAnimationAdapter;
import com.android.server.wm.utils.WmDisplayCutout;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.wm.WindowManagerDebugger;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

public class WindowState extends WindowContainer<WindowState> implements WindowManagerPolicy.WindowState {
    private static final float DEFAULT_DIM_AMOUNT_DEAD_WINDOW = 0.5f;
    static final int MINIMUM_VISIBLE_HEIGHT_IN_DP = 32;
    static final int MINIMUM_VISIBLE_WIDTH_IN_DP = 48;
    static final int RESIZE_HANDLE_WIDTH_IN_DP = 30;
    static final String TAG = "WindowManager";
    private static final Comparator<WindowState> sWindowSubLayerComparator = new Comparator<WindowState>() {
        @Override
        public int compare(WindowState windowState, WindowState windowState2) {
            int i = windowState.mSubLayer;
            int i2 = windowState2.mSubLayer;
            if (i < i2) {
                return -1;
            }
            if (i == i2 && i2 < 0) {
                return -1;
            }
            return 1;
        }
    };
    private boolean mAnimateReplacingWindow;
    boolean mAnimatingExit;
    boolean mAppDied;
    boolean mAppFreezing;
    final int mAppOp;
    private boolean mAppOpVisibility;
    AppWindowToken mAppToken;
    final WindowManager.LayoutParams mAttrs;
    final int mBaseLayer;
    final IWindow mClient;
    private InputChannel mClientChannel;
    final Rect mCompatFrame;
    final Rect mContainingFrame;
    boolean mContentChanged;
    private final Rect mContentFrame;
    final Rect mContentInsets;
    private boolean mContentInsetsChanged;
    final Context mContext;
    private DeadWindowEventReceiver mDeadWindowEventReceiver;
    final DeathRecipient mDeathRecipient;
    final Rect mDecorFrame;
    boolean mDestroying;
    WmDisplayCutout mDisplayCutout;
    private boolean mDisplayCutoutChanged;
    final Rect mDisplayFrame;
    private boolean mDragResizing;
    private boolean mDragResizingChangeReported;
    private PowerManager.WakeLock mDrawLock;
    private boolean mDrawnStateEvaluated;
    public boolean mEnforceSizeCompat;
    private RemoteCallbackList<IWindowFocusObserver> mFocusCallbacks;
    private boolean mForceHideNonSystemOverlayWindow;
    final Rect mFrame;
    private long mFrameNumber;
    private boolean mFrameSizeChanged;
    final Rect mGivenContentInsets;
    boolean mGivenInsetsPending;
    final Region mGivenTouchableRegion;
    final Rect mGivenVisibleInsets;
    float mGlobalScale;
    float mHScale;
    public float mHWScale;
    boolean mHasSurface;
    boolean mHaveFrame;
    boolean mHidden;
    private boolean mHiddenWhileSuspended;
    boolean mInRelayout;
    InputChannel mInputChannel;
    final InputWindowHandle mInputWindowHandle;
    private final Rect mInsetFrame;
    float mInvGlobalScale;
    private boolean mIsChildWindow;
    private boolean mIsDimming;
    private final boolean mIsFloatingLayer;
    private boolean mIsFullscreenOn;
    final boolean mIsImWindow;
    final boolean mIsWallpaper;
    final Rect mLastContentInsets;
    private WmDisplayCutout mLastDisplayCutout;
    final Rect mLastFrame;
    int mLastFreezeDuration;
    float mLastHScale;
    private final Rect mLastOutsets;
    private final Rect mLastOverscanInsets;
    final Rect mLastRelayoutContentInsets;
    private final MergedConfiguration mLastReportedConfiguration;
    private int mLastRequestedHeight;
    private int mLastRequestedWidth;
    private final Rect mLastStableInsets;
    final Rect mLastSurfaceInsets;
    private CharSequence mLastTitle;
    float mLastVScale;
    private final Rect mLastVisibleInsets;
    int mLastVisibleLayoutRotation;
    int mLayer;
    final boolean mLayoutAttached;
    boolean mLayoutNeeded;
    int mLayoutSeq;
    private boolean mMovedByResize;
    public boolean mNeedHWResizer;
    boolean mObscured;
    private boolean mOrientationChangeTimedOut;
    private boolean mOrientationChanging;
    private final Rect mOutsetFrame;
    final Rect mOutsets;
    private boolean mOutsetsChanged;
    private final Rect mOverscanFrame;
    final Rect mOverscanInsets;
    private boolean mOverscanInsetsChanged;
    final boolean mOwnerCanAddInternalSystemWindow;
    final int mOwnerUid;
    final Rect mParentFrame;
    private boolean mParentFrameWasClippedByDisplayCutout;
    boolean mPermanentlyHidden;
    final WindowManagerPolicy mPolicy;
    boolean mPolicyVisibility;
    boolean mPolicyVisibilityAfterAnim;
    private PowerManagerWrapper mPowerManagerWrapper;
    boolean mRelayoutCalled;
    boolean mRemoveOnExit;
    boolean mRemoved;
    private WindowState mReplacementWindow;
    private boolean mReplacingRemoveRequested;
    boolean mReportOrientationChanged;
    int mRequestedHeight;
    int mRequestedWidth;
    private int mResizeMode;
    boolean mResizedWhileGone;
    boolean mSeamlesslyRotated;
    int mSeq;
    final Session mSession;
    private boolean mShowToOwnerOnly;
    boolean mSkipEnterAnimationForSeamlessReplacement;
    private final Rect mStableFrame;
    final Rect mStableInsets;
    private boolean mStableInsetsChanged;
    private String mStringNameCache;
    final int mSubLayer;
    private final Point mSurfacePosition;
    int mSystemUiVisibility;
    private TapExcludeRegionHolder mTapExcludeRegionHolder;
    final Matrix mTmpMatrix;
    private final Rect mTmpRect;
    WindowToken mToken;
    int mTouchableInsets;
    float mVScale;
    int mViewVisibility;
    final Rect mVisibleFrame;
    final Rect mVisibleInsets;
    private boolean mVisibleInsetsChanged;
    int mWallpaperDisplayOffsetX;
    int mWallpaperDisplayOffsetY;
    boolean mWallpaperVisible;
    float mWallpaperX;
    float mWallpaperXStep;
    float mWallpaperY;
    float mWallpaperYStep;
    private boolean mWasExiting;
    private boolean mWasVisibleBeforeClientHidden;
    boolean mWillReplaceWindow;
    final WindowStateAnimator mWinAnimator;
    final WindowId mWindowId;
    boolean mWindowRemovalAllowed;
    private WmsExt mWmsExt;

    interface PowerManagerWrapper {
        boolean isInteractive();

        void wakeUp(long j, String str);
    }

    @Override
    public void commitPendingTransaction() {
        super.commitPendingTransaction();
    }

    @Override
    public int compareTo(WindowContainer windowContainer) {
        return super.compareTo(windowContainer);
    }

    @Override
    public SurfaceControl getAnimationLeashParent() {
        return super.getAnimationLeashParent();
    }

    @Override
    public SurfaceControl getParentSurfaceControl() {
        return super.getParentSurfaceControl();
    }

    @Override
    public SurfaceControl.Transaction getPendingTransaction() {
        return super.getPendingTransaction();
    }

    @Override
    public SurfaceControl getSurfaceControl() {
        return super.getSurfaceControl();
    }

    @Override
    public int getSurfaceHeight() {
        return super.getSurfaceHeight();
    }

    @Override
    public int getSurfaceWidth() {
        return super.getSurfaceWidth();
    }

    @Override
    public SurfaceControl.Builder makeAnimationLeash() {
        return super.makeAnimationLeash();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        super.onOverrideConfigurationChanged(configuration);
    }

    WindowState(final WindowManagerService windowManagerService, Session session, IWindow iWindow, WindowToken windowToken, WindowState windowState, int i, int i2, WindowManager.LayoutParams layoutParams, int i3, int i4, boolean z) {
        this(windowManagerService, session, iWindow, windowToken, windowState, i, i2, layoutParams, i3, i4, z, new PowerManagerWrapper() {
            @Override
            public void wakeUp(long j, String str) {
                windowManagerService.mPowerManager.wakeUp(j, str);
            }

            @Override
            public boolean isInteractive() {
                return windowManagerService.mPowerManager.isInteractive();
            }
        });
    }

    WindowState(WindowManagerService windowManagerService, Session session, IWindow iWindow, WindowToken windowToken, WindowState windowState, int i, int i2, WindowManager.LayoutParams layoutParams, int i3, int i4, boolean z, PowerManagerWrapper powerManagerWrapper) {
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        super(windowManagerService);
        this.mAttrs = new WindowManager.LayoutParams();
        this.mPolicyVisibility = true;
        this.mPolicyVisibilityAfterAnim = true;
        this.mAppOpVisibility = true;
        this.mHidden = true;
        this.mDragResizingChangeReported = true;
        this.mLayoutSeq = -1;
        this.mLastReportedConfiguration = new MergedConfiguration();
        this.mVisibleInsets = new Rect();
        this.mLastVisibleInsets = new Rect();
        this.mContentInsets = new Rect();
        this.mLastContentInsets = new Rect();
        this.mLastRelayoutContentInsets = new Rect();
        this.mOverscanInsets = new Rect();
        this.mLastOverscanInsets = new Rect();
        this.mStableInsets = new Rect();
        this.mLastStableInsets = new Rect();
        this.mOutsets = new Rect();
        this.mLastOutsets = new Rect();
        this.mOutsetsChanged = false;
        this.mDisplayCutout = WmDisplayCutout.NO_CUTOUT;
        this.mLastDisplayCutout = WmDisplayCutout.NO_CUTOUT;
        this.mGivenContentInsets = new Rect();
        this.mGivenVisibleInsets = new Rect();
        this.mGivenTouchableRegion = new Region();
        this.mTouchableInsets = 0;
        this.mGlobalScale = 1.0f;
        this.mInvGlobalScale = 1.0f;
        this.mHScale = 1.0f;
        this.mVScale = 1.0f;
        this.mLastHScale = 1.0f;
        this.mLastVScale = 1.0f;
        this.mTmpMatrix = new Matrix();
        this.mFrame = new Rect();
        this.mLastFrame = new Rect();
        this.mFrameSizeChanged = false;
        this.mCompatFrame = new Rect();
        this.mContainingFrame = new Rect();
        this.mParentFrame = new Rect();
        this.mDisplayFrame = new Rect();
        this.mOverscanFrame = new Rect();
        this.mStableFrame = new Rect();
        this.mDecorFrame = new Rect();
        this.mContentFrame = new Rect();
        this.mVisibleFrame = new Rect();
        this.mOutsetFrame = new Rect();
        this.mInsetFrame = new Rect();
        this.mWallpaperX = -1.0f;
        this.mWallpaperY = -1.0f;
        this.mWallpaperXStep = -1.0f;
        this.mWallpaperYStep = -1.0f;
        this.mWallpaperDisplayOffsetX = Integer.MIN_VALUE;
        this.mWallpaperDisplayOffsetY = Integer.MIN_VALUE;
        this.mLastVisibleLayoutRotation = -1;
        this.mHasSurface = false;
        this.mWillReplaceWindow = false;
        this.mReplacingRemoveRequested = false;
        this.mAnimateReplacingWindow = false;
        this.mReplacementWindow = null;
        this.mSkipEnterAnimationForSeamlessReplacement = false;
        this.mTmpRect = new Rect();
        this.mResizedWhileGone = false;
        this.mSeamlesslyRotated = false;
        this.mLastSurfaceInsets = new Rect();
        this.mSurfacePosition = new Point();
        this.mNeedHWResizer = false;
        this.mHWScale = this.mGlobalScale;
        this.mFrameNumber = -1L;
        this.mIsDimming = false;
        this.mWmsExt = MtkSystemServiceFactory.getInstance().makeWmsExt();
        this.mIsFullscreenOn = true;
        this.mSession = session;
        this.mClient = iWindow;
        this.mAppOp = i;
        this.mToken = windowToken;
        this.mAppToken = this.mToken.asAppWindowToken();
        this.mOwnerUid = i4;
        this.mOwnerCanAddInternalSystemWindow = z;
        this.mWindowId = new WindowId();
        this.mAttrs.copyFrom(layoutParams);
        this.mLastSurfaceInsets.set(this.mAttrs.surfaceInsets);
        this.mViewVisibility = i3;
        this.mPolicy = this.mService.mPolicy;
        this.mContext = this.mService.mContext;
        DeathRecipient deathRecipient = new DeathRecipient();
        this.mSeq = i2;
        if ((this.mAttrs.privateFlags & 128) != 0) {
            z2 = true;
        } else {
            z2 = false;
        }
        this.mEnforceSizeCompat = z2;
        this.mPowerManagerWrapper = powerManagerWrapper;
        if (WindowManagerService.localLOGV) {
            Slog.v("WindowManager", "Window " + this + " client=" + iWindow.asBinder() + " token=" + windowToken + " (" + this.mAttrs.token + ") params=" + layoutParams);
        }
        try {
            iWindow.asBinder().linkToDeath(deathRecipient, 0);
            this.mDeathRecipient = deathRecipient;
            if (this.mAttrs.type >= 1000 && this.mAttrs.type <= 1999) {
                this.mBaseLayer = (this.mPolicy.getWindowLayerLw(windowState) * 10000) + 1000;
                this.mSubLayer = this.mPolicy.getSubWindowLayerFromTypeLw(layoutParams.type);
                this.mIsChildWindow = true;
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.v("WindowManager", "Adding " + this + " to " + windowState);
                }
                windowState.addChild(this, sWindowSubLayerComparator);
                if (this.mAttrs.type != 1003) {
                    z6 = true;
                } else {
                    z6 = false;
                }
                this.mLayoutAttached = z6;
                if (windowState.mAttrs.type == 2011 || windowState.mAttrs.type == 2012) {
                    z7 = true;
                } else {
                    z7 = false;
                }
                this.mIsImWindow = z7;
                if (windowState.mAttrs.type == 2013) {
                    z8 = true;
                } else {
                    z8 = false;
                }
                this.mIsWallpaper = z8;
            } else {
                this.mBaseLayer = (this.mPolicy.getWindowLayerLw(this) * 10000) + 1000;
                this.mSubLayer = 0;
                this.mIsChildWindow = false;
                this.mLayoutAttached = false;
                if (this.mAttrs.type == 2011 || this.mAttrs.type == 2012) {
                    z3 = true;
                } else {
                    z3 = false;
                }
                this.mIsImWindow = z3;
                if (this.mAttrs.type == 2013) {
                    z4 = true;
                } else {
                    z4 = false;
                }
                this.mIsWallpaper = z4;
            }
            if (this.mIsImWindow || this.mIsWallpaper) {
                z5 = true;
            } else {
                z5 = false;
            }
            this.mIsFloatingLayer = z5;
            if (this.mAppToken != null && this.mAppToken.mShowForAllUsers) {
                this.mAttrs.flags |= DumpState.DUMP_FROZEN;
            }
            this.mWinAnimator = new WindowStateAnimator(this);
            this.mWinAnimator.mAlpha = layoutParams.alpha;
            this.mRequestedWidth = 0;
            this.mRequestedHeight = 0;
            this.mLastRequestedWidth = 0;
            this.mLastRequestedHeight = 0;
            this.mLayer = 0;
            this.mInputWindowHandle = new InputWindowHandle(this.mAppToken != null ? this.mAppToken.mInputApplicationHandle : null, this, iWindow, getDisplayId());
            if (this.mWmsExt.isFullScreenCropState(this.mService.mFocusedApp)) {
                this.mIsFullscreenOn = this.mAppToken != null ? this.mAppToken.isFullscreenOn : true;
            }
        } catch (RemoteException e) {
            this.mDeathRecipient = null;
            this.mIsChildWindow = false;
            this.mLayoutAttached = false;
            this.mIsImWindow = false;
            this.mIsWallpaper = false;
            this.mIsFloatingLayer = false;
            this.mBaseLayer = 0;
            this.mSubLayer = 0;
            this.mInputWindowHandle = null;
            this.mWinAnimator = null;
        }
    }

    void attach() {
        if (WindowManagerService.localLOGV) {
            Slog.v("WindowManager", "Attaching " + this + " token=" + this.mToken);
        }
        this.mSession.windowAddedLocked(this.mAttrs.packageName);
    }

    boolean getDrawnStateEvaluated() {
        return this.mDrawnStateEvaluated;
    }

    void setDrawnStateEvaluated(boolean z) {
        this.mDrawnStateEvaluated = z;
    }

    @Override
    void onParentSet() {
        super.onParentSet();
        setDrawnStateEvaluated(false);
        getDisplayContent().reapplyMagnificationSpec();
    }

    @Override
    public int getOwningUid() {
        return this.mOwnerUid;
    }

    @Override
    public String getOwningPackage() {
        return this.mAttrs.packageName;
    }

    @Override
    public boolean canAddInternalSystemWindow() {
        return this.mOwnerCanAddInternalSystemWindow;
    }

    @Override
    public boolean canAcquireSleepToken() {
        return this.mSession.mCanAcquireSleepToken;
    }

    private void subtractInsets(Rect rect, Rect rect2, Rect rect3, Rect rect4) {
        rect.inset(Math.max(0, rect3.left - Math.max(rect2.left, rect4.left)), Math.max(0, rect3.top - Math.max(rect2.top, rect4.top)), Math.max(0, Math.min(rect2.right, rect4.right) - rect3.right), Math.max(0, Math.min(rect2.bottom, rect4.bottom) - rect3.bottom));
    }

    @Override
    public void computeFrameLw(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, Rect rect7, Rect rect8, WmDisplayCutout wmDisplayCutout, boolean z) {
        Rect rect9;
        int i;
        int i2;
        int i3;
        int i4;
        boolean z2;
        WmDisplayCutout wmDisplayCutout2;
        DisplayContent displayContent;
        if (this.mWillReplaceWindow && (this.mAnimatingExit || !this.mReplacingRemoveRequested)) {
            return;
        }
        this.mHaveFrame = true;
        this.mParentFrameWasClippedByDisplayCutout = z;
        Task task = getTask();
        boolean zInFullscreenContainer = inFullscreenContainer();
        boolean z3 = task != null && task.isFloating();
        DisplayContent displayContent2 = getDisplayContent();
        if (task != null && isInMultiWindowMode()) {
            task.getTempInsetBounds(this.mInsetFrame);
        } else {
            this.mInsetFrame.setEmpty();
        }
        if (zInFullscreenContainer || layoutInParentFrame()) {
            this.mContainingFrame.set(rect);
            this.mDisplayFrame.set(rect2);
            rect9 = rect;
            i = 0;
            i2 = 0;
        } else {
            getBounds(this.mContainingFrame);
            if (this.mAppToken != null && !this.mAppToken.mFrozenBounds.isEmpty()) {
                Rect rectPeek = this.mAppToken.mFrozenBounds.peek();
                this.mContainingFrame.right = this.mContainingFrame.left + rectPeek.width();
                this.mContainingFrame.bottom = this.mContainingFrame.top + rectPeek.height();
            }
            WindowState windowState = this.mService.mInputMethodWindow;
            if (windowState != null && windowState.isVisibleNow() && isInputMethodTarget()) {
                if (inFreeformWindowingMode() && this.mContainingFrame.bottom > rect4.bottom) {
                    this.mContainingFrame.top -= this.mContainingFrame.bottom - rect4.bottom;
                } else if (!inPinnedWindowingMode() && this.mContainingFrame.bottom > rect.bottom) {
                    this.mContainingFrame.bottom = rect.bottom;
                }
            }
            if (z3 && this.mContainingFrame.isEmpty()) {
                this.mContainingFrame.set(rect4);
            }
            TaskStack stack = getStack();
            if (inPinnedWindowingMode() && stack != null && stack.lastAnimatingBoundsWasToFullscreen()) {
                this.mInsetFrame.intersectUnchecked(rect);
                this.mContainingFrame.intersectUnchecked(rect);
            }
            this.mDisplayFrame.set(this.mContainingFrame);
            i = !this.mInsetFrame.isEmpty() ? this.mInsetFrame.left - this.mContainingFrame.left : 0;
            i2 = !this.mInsetFrame.isEmpty() ? this.mInsetFrame.top - this.mContainingFrame.top : 0;
            rect9 = !this.mInsetFrame.isEmpty() ? this.mInsetFrame : this.mContainingFrame;
            this.mTmpRect.set(0, 0, displayContent2.getDisplayInfo().logicalWidth, displayContent2.getDisplayInfo().logicalHeight);
            subtractInsets(this.mDisplayFrame, rect9, rect2, this.mTmpRect);
            if (!layoutInParentFrame()) {
                subtractInsets(this.mContainingFrame, rect9, rect, this.mTmpRect);
                subtractInsets(this.mInsetFrame, rect9, rect, this.mTmpRect);
            }
            rect2.intersect(rect9);
        }
        int iWidth = this.mContainingFrame.width();
        int iHeight = this.mContainingFrame.height();
        if (!this.mParentFrame.equals(rect)) {
            this.mParentFrame.set(rect);
            this.mContentChanged = true;
        }
        if (this.mRequestedWidth != this.mLastRequestedWidth || this.mRequestedHeight != this.mLastRequestedHeight) {
            this.mLastRequestedWidth = this.mRequestedWidth;
            this.mLastRequestedHeight = this.mRequestedHeight;
            this.mContentChanged = true;
        }
        this.mOverscanFrame.set(rect3);
        this.mContentFrame.set(rect4);
        this.mVisibleFrame.set(rect5);
        this.mDecorFrame.set(rect6);
        this.mStableFrame.set(rect7);
        boolean z4 = rect8 != null;
        if (z4) {
            this.mOutsetFrame.set(rect8);
        }
        int iWidth2 = this.mFrame.width();
        int iHeight2 = this.mFrame.height();
        applyGravityAndUpdateFrame(rect9, rect2);
        if (z4) {
            i4 = iHeight;
            i3 = iWidth;
            this.mOutsets.set(Math.max(this.mContentFrame.left - this.mOutsetFrame.left, 0), Math.max(this.mContentFrame.top - this.mOutsetFrame.top, 0), Math.max(this.mOutsetFrame.right - this.mContentFrame.right, 0), Math.max(this.mOutsetFrame.bottom - this.mContentFrame.bottom, 0));
        } else {
            i3 = iWidth;
            i4 = iHeight;
            this.mOutsets.set(0, 0, 0, 0);
        }
        if (z3 && !this.mFrame.isEmpty()) {
            Rect rect10 = task.inPinnedWindowingMode() ? this.mFrame : this.mContentFrame;
            int iMin = Math.min(this.mFrame.height(), rect10.height());
            int iMin2 = Math.min(rect10.width(), this.mFrame.width());
            DisplayMetrics displayMetrics = getDisplayContent().getDisplayMetrics();
            int iMin3 = Math.min(iMin, WindowManagerService.dipToPixel(32, displayMetrics));
            int iMin4 = Math.min(iMin2, WindowManagerService.dipToPixel(48, displayMetrics));
            int iMax = Math.max(rect10.top, Math.min(this.mFrame.top, rect10.bottom - iMin3));
            int iMax2 = Math.max((rect10.left + iMin4) - iMin2, Math.min(this.mFrame.left, rect10.right - iMin4));
            this.mFrame.set(iMax2, iMax, iMin2 + iMax2, iMin + iMax);
            this.mContentFrame.set(this.mFrame);
            this.mVisibleFrame.set(this.mContentFrame);
            this.mStableFrame.set(this.mContentFrame);
        } else {
            if (this.mAttrs.type == 2034) {
                displayContent2.getDockedDividerController().positionDockedStackedDivider(this.mFrame);
                this.mContentFrame.set(this.mFrame);
                if (!this.mFrame.equals(this.mLastFrame)) {
                    z2 = true;
                    this.mMovedByResize = true;
                }
            } else {
                z2 = true;
                this.mContentFrame.set(Math.max(this.mContentFrame.left, this.mFrame.left), Math.max(this.mContentFrame.top, this.mFrame.top), Math.min(this.mContentFrame.right, this.mFrame.right), Math.min(this.mContentFrame.bottom, this.mFrame.bottom));
                this.mVisibleFrame.set(Math.max(this.mVisibleFrame.left, this.mFrame.left), Math.max(this.mVisibleFrame.top, this.mFrame.top), Math.min(this.mVisibleFrame.right, this.mFrame.right), Math.min(this.mVisibleFrame.bottom, this.mFrame.bottom));
                this.mStableFrame.set(Math.max(this.mStableFrame.left, this.mFrame.left), Math.max(this.mStableFrame.top, this.mFrame.top), Math.min(this.mStableFrame.right, this.mFrame.right), Math.min(this.mStableFrame.bottom, this.mFrame.bottom));
            }
            if (zInFullscreenContainer && !z3) {
                this.mOverscanInsets.set(Math.max(this.mOverscanFrame.left - rect9.left, 0), Math.max(this.mOverscanFrame.top - rect9.top, 0), Math.max(rect9.right - this.mOverscanFrame.right, 0), Math.max(rect9.bottom - this.mOverscanFrame.bottom, 0));
            }
            if (this.mAttrs.type != 2034) {
                WmDisplayCutout wmDisplayCutoutCalculateRelativeTo = wmDisplayCutout.calculateRelativeTo(this.mDisplayFrame);
                this.mTmpRect.set(this.mDisplayFrame);
                this.mTmpRect.inset(wmDisplayCutoutCalculateRelativeTo.getDisplayCutout().getSafeInsets());
                this.mTmpRect.intersectUnchecked(this.mStableFrame);
                this.mStableInsets.set(Math.max(this.mTmpRect.left - this.mDisplayFrame.left, 0), Math.max(this.mTmpRect.top - this.mDisplayFrame.top, 0), Math.max(this.mDisplayFrame.right - this.mTmpRect.right, 0), Math.max(this.mDisplayFrame.bottom - this.mTmpRect.bottom, 0));
                this.mContentInsets.setEmpty();
                this.mVisibleInsets.setEmpty();
                wmDisplayCutout2 = WmDisplayCutout.NO_CUTOUT;
            } else {
                getDisplayContent().getBounds(this.mTmpRect);
                boolean z5 = (z3 || zInFullscreenContainer || this.mFrame.right <= this.mTmpRect.right) ? false : z2;
                if (z3 || zInFullscreenContainer || this.mFrame.bottom <= this.mTmpRect.bottom) {
                    z2 = false;
                }
                this.mContentInsets.set(this.mContentFrame.left - this.mFrame.left, this.mContentFrame.top - this.mFrame.top, z5 ? this.mTmpRect.right - this.mContentFrame.right : this.mFrame.right - this.mContentFrame.right, z2 ? this.mTmpRect.bottom - this.mContentFrame.bottom : this.mFrame.bottom - this.mContentFrame.bottom);
                this.mVisibleInsets.set(this.mVisibleFrame.left - this.mFrame.left, this.mVisibleFrame.top - this.mFrame.top, z5 ? this.mTmpRect.right - this.mVisibleFrame.right : this.mFrame.right - this.mVisibleFrame.right, z2 ? this.mTmpRect.bottom - this.mVisibleFrame.bottom : this.mFrame.bottom - this.mVisibleFrame.bottom);
                this.mStableInsets.set(Math.max(this.mStableFrame.left - this.mFrame.left, 0), Math.max(this.mStableFrame.top - this.mFrame.top, 0), z5 ? Math.max(this.mTmpRect.right - this.mStableFrame.right, 0) : Math.max(this.mFrame.right - this.mStableFrame.right, 0), z2 ? Math.max(this.mTmpRect.bottom - this.mStableFrame.bottom, 0) : Math.max(this.mFrame.bottom - this.mStableFrame.bottom, 0));
                wmDisplayCutout2 = wmDisplayCutout;
            }
            this.mDisplayCutout = wmDisplayCutout2.calculateRelativeTo(this.mFrame);
            int i5 = -i;
            int i6 = -i2;
            this.mFrame.offset(i5, i6);
            this.mCompatFrame.offset(i5, i6);
            this.mContentFrame.offset(i5, i6);
            this.mVisibleFrame.offset(i5, i6);
            this.mStableFrame.offset(i5, i6);
            this.mCompatFrame.set(this.mFrame);
            if (this.mEnforceSizeCompat) {
                this.mOverscanInsets.scale(this.mInvGlobalScale);
                this.mContentInsets.scale(this.mInvGlobalScale);
                this.mVisibleInsets.scale(this.mInvGlobalScale);
                this.mStableInsets.scale(this.mInvGlobalScale);
                this.mOutsets.scale(this.mInvGlobalScale);
                this.mCompatFrame.scale(this.mInvGlobalScale);
            }
            if (this.mIsWallpaper && ((iWidth2 != this.mFrame.width() || iHeight2 != this.mFrame.height()) && (displayContent = getDisplayContent()) != null)) {
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                getDisplayContent().mWallpaperController.updateWallpaperOffset(this, displayInfo.logicalWidth, displayInfo.logicalHeight, false);
            }
            if (!WindowManagerDebugConfig.DEBUG_LAYOUT || WindowManagerService.localLOGV) {
                Slog.v("WindowManager", "Resolving (mRequestedWidth=" + this.mRequestedWidth + ", mRequestedheight=" + this.mRequestedHeight + ") to (pw=" + i3 + ", ph=" + i4 + "): frame=" + this.mFrame.toShortString() + " ci=" + this.mContentInsets.toShortString() + " vi=" + this.mVisibleInsets.toShortString() + " si=" + this.mStableInsets.toShortString() + " of=" + this.mOutsets.toShortString());
            }
            return;
        }
        z2 = true;
        if (zInFullscreenContainer) {
            this.mOverscanInsets.set(Math.max(this.mOverscanFrame.left - rect9.left, 0), Math.max(this.mOverscanFrame.top - rect9.top, 0), Math.max(rect9.right - this.mOverscanFrame.right, 0), Math.max(rect9.bottom - this.mOverscanFrame.bottom, 0));
        }
        if (this.mAttrs.type != 2034) {
        }
        this.mDisplayCutout = wmDisplayCutout2.calculateRelativeTo(this.mFrame);
        int i52 = -i;
        int i62 = -i2;
        this.mFrame.offset(i52, i62);
        this.mCompatFrame.offset(i52, i62);
        this.mContentFrame.offset(i52, i62);
        this.mVisibleFrame.offset(i52, i62);
        this.mStableFrame.offset(i52, i62);
        this.mCompatFrame.set(this.mFrame);
        if (this.mEnforceSizeCompat) {
        }
        if (this.mIsWallpaper) {
            DisplayInfo displayInfo2 = displayContent.getDisplayInfo();
            getDisplayContent().mWallpaperController.updateWallpaperOffset(this, displayInfo2.logicalWidth, displayInfo2.logicalHeight, false);
        }
        if (!WindowManagerDebugConfig.DEBUG_LAYOUT) {
        }
        Slog.v("WindowManager", "Resolving (mRequestedWidth=" + this.mRequestedWidth + ", mRequestedheight=" + this.mRequestedHeight + ") to (pw=" + i3 + ", ph=" + i4 + "): frame=" + this.mFrame.toShortString() + " ci=" + this.mContentInsets.toShortString() + " vi=" + this.mVisibleInsets.toShortString() + " si=" + this.mStableInsets.toShortString() + " of=" + this.mOutsets.toShortString());
    }

    @Override
    public Rect getBounds() {
        if (isInMultiWindowMode()) {
            return getTask().getBounds();
        }
        if (this.mAppToken != null) {
            return this.mAppToken.getBounds();
        }
        return super.getBounds();
    }

    @Override
    public Rect getFrameLw() {
        return this.mFrame;
    }

    @Override
    public Rect getDisplayFrameLw() {
        return this.mDisplayFrame;
    }

    @Override
    public Rect getOverscanFrameLw() {
        return this.mOverscanFrame;
    }

    @Override
    public Rect getContentFrameLw() {
        return this.mContentFrame;
    }

    @Override
    public Rect getVisibleFrameLw() {
        return this.mVisibleFrame;
    }

    Rect getStableFrameLw() {
        return this.mStableFrame;
    }

    @Override
    public boolean getGivenInsetsPendingLw() {
        return this.mGivenInsetsPending;
    }

    @Override
    public Rect getGivenContentInsetsLw() {
        return this.mGivenContentInsets;
    }

    @Override
    public Rect getGivenVisibleInsetsLw() {
        return this.mGivenVisibleInsets;
    }

    @Override
    public WindowManager.LayoutParams getAttrs() {
        return this.mAttrs;
    }

    @Override
    public boolean getNeedsMenuLw(WindowManagerPolicy.WindowState windowState) {
        return getDisplayContent().getNeedsMenu(this, windowState);
    }

    @Override
    public int getSystemUiVisibility() {
        return this.mSystemUiVisibility;
    }

    @Override
    public int getSurfaceLayer() {
        return this.mLayer;
    }

    @Override
    public int getBaseType() {
        return getTopParentWindow().mAttrs.type;
    }

    @Override
    public IApplicationToken getAppToken() {
        if (this.mAppToken != null) {
            return this.mAppToken.appToken;
        }
        return null;
    }

    @Override
    public boolean isVoiceInteraction() {
        return this.mAppToken != null && this.mAppToken.mVoiceInteraction;
    }

    boolean setReportResizeHints() {
        this.mOverscanInsetsChanged |= !this.mLastOverscanInsets.equals(this.mOverscanInsets);
        this.mContentInsetsChanged |= !this.mLastContentInsets.equals(this.mContentInsets);
        this.mVisibleInsetsChanged |= !this.mLastVisibleInsets.equals(this.mVisibleInsets);
        this.mStableInsetsChanged |= !this.mLastStableInsets.equals(this.mStableInsets);
        this.mOutsetsChanged |= !this.mLastOutsets.equals(this.mOutsets);
        this.mFrameSizeChanged |= (this.mLastFrame.width() == this.mFrame.width() && this.mLastFrame.height() == this.mFrame.height()) ? false : true;
        this.mDisplayCutoutChanged |= !this.mLastDisplayCutout.equals(this.mDisplayCutout);
        return this.mOverscanInsetsChanged || this.mContentInsetsChanged || this.mVisibleInsetsChanged || this.mOutsetsChanged || this.mFrameSizeChanged || this.mDisplayCutoutChanged;
    }

    void updateResizingWindowIfNeeded() {
        WindowStateAnimator windowStateAnimator = this.mWinAnimator;
        if (!this.mHasSurface || getDisplayContent().mLayoutSeq != this.mLayoutSeq || isGoneForLayoutLw()) {
            return;
        }
        Task task = getTask();
        if (task != null && task.mStack.isAnimatingBounds()) {
            return;
        }
        setReportResizeHints();
        boolean zIsConfigChanged = isConfigChanged();
        if (WindowManagerDebugConfig.DEBUG_CONFIGURATION && zIsConfigChanged) {
            Slog.v("WindowManager", "Win " + this + " config changed: " + getConfiguration());
        }
        boolean z = isDragResizeChanged() && !isDragResizingChangeReported();
        if (WindowManagerService.localLOGV) {
            Slog.v("WindowManager", "Resizing " + this + ": configChanged=" + zIsConfigChanged + " dragResizingChanged=" + z + " last=" + this.mLastFrame + " frame=" + this.mFrame);
        }
        this.mLastFrame.set(this.mFrame);
        if (this.mContentInsetsChanged || this.mVisibleInsetsChanged || this.mStableInsetsChanged || windowStateAnimator.mSurfaceResized || this.mOutsetsChanged || this.mFrameSizeChanged || this.mDisplayCutoutChanged || zIsConfigChanged || z || this.mReportOrientationChanged) {
            if (WindowManagerDebugConfig.DEBUG_RESIZE || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Resize reasons for w=" + this + ":  contentInsetsChanged=" + this.mContentInsetsChanged + " " + this.mContentInsets.toShortString() + " visibleInsetsChanged=" + this.mVisibleInsetsChanged + " " + this.mVisibleInsets.toShortString() + " stableInsetsChanged=" + this.mStableInsetsChanged + " " + this.mStableInsets.toShortString() + " outsetsChanged=" + this.mOutsetsChanged + " " + this.mOutsets.toShortString() + " surfaceResized=" + windowStateAnimator.mSurfaceResized + " configChanged=" + zIsConfigChanged + " dragResizingChanged=" + z + " reportOrientationChanged=" + this.mReportOrientationChanged + " displayCutoutChanged=" + this.mDisplayCutoutChanged + " contentInsets=" + this.mContentInsets + " visibleInsets=" + this.mVisibleInsets);
            }
            if (this.mAppToken != null && this.mAppDied) {
                this.mAppToken.removeDeadWindows();
                return;
            }
            updateLastInsetValues();
            this.mService.makeWindowFreezingScreenIfNeededLocked(this);
            if (getOrientationChanging() || z) {
                if (WindowManagerDebugConfig.DEBUG_ANIM || WindowManagerDebugConfig.DEBUG_ORIENTATION || WindowManagerDebugConfig.DEBUG_RESIZE) {
                    Slog.v("WindowManager", "Orientation or resize start waiting for draw, mDrawState=DRAW_PENDING in " + this + ", surfaceController " + windowStateAnimator.mSurfaceController);
                }
                windowStateAnimator.mDrawState = 1;
                if (this.mAppToken != null) {
                    this.mAppToken.clearAllDrawn();
                }
            }
            if (!this.mService.mResizingWindows.contains(this)) {
                if (WindowManagerDebugConfig.DEBUG_RESIZE || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Resizing window " + this);
                }
                this.mService.mResizingWindows.add(this);
                return;
            }
            return;
        }
        if (getOrientationChanging() && isDrawnLw()) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Orientation not waiting for draw in " + this + ", surfaceController " + windowStateAnimator.mSurfaceController);
            }
            setOrientationChanging(false);
            this.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mService.mDisplayFreezeTime);
        }
    }

    boolean getOrientationChanging() {
        return ((!this.mOrientationChanging && (!isVisible() || getConfiguration().orientation == getLastReportedConfiguration().orientation)) || this.mSeamlesslyRotated || this.mOrientationChangeTimedOut) ? false : true;
    }

    void setOrientationChanging(boolean z) {
        this.mOrientationChanging = z;
        this.mOrientationChangeTimedOut = false;
    }

    void orientationChangeTimedOut() {
        this.mOrientationChangeTimedOut = true;
    }

    DisplayContent getDisplayContent() {
        return this.mToken.getDisplayContent();
    }

    @Override
    void onDisplayChanged(DisplayContent displayContent) {
        super.onDisplayChanged(displayContent);
        if (displayContent != null) {
            this.mLayoutSeq = displayContent.mLayoutSeq - 1;
            this.mInputWindowHandle.displayId = displayContent.getDisplayId();
        }
    }

    DisplayInfo getDisplayInfo() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent != null) {
            return displayContent.getDisplayInfo();
        }
        return null;
    }

    @Override
    public int getDisplayId() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return -1;
        }
        return displayContent.getDisplayId();
    }

    Task getTask() {
        if (this.mAppToken != null) {
            return this.mAppToken.getTask();
        }
        return null;
    }

    TaskStack getStack() {
        Task task = getTask();
        if (task != null && task.mStack != null) {
            return task.mStack;
        }
        DisplayContent displayContent = getDisplayContent();
        if (this.mAttrs.type < 2000 || displayContent == null) {
            return null;
        }
        return displayContent.getHomeStack();
    }

    void getVisibleBounds(Rect rect) {
        Task task = getTask();
        boolean z = false;
        boolean z2 = task != null && task.cropWindowsToStackBounds();
        rect.setEmpty();
        this.mTmpRect.setEmpty();
        if (z2) {
            TaskStack taskStack = task.mStack;
            if (taskStack != null) {
                taskStack.getDimBounds(this.mTmpRect);
                z = z2;
            }
        } else {
            z = z2;
        }
        rect.set(this.mVisibleFrame);
        if (z) {
            rect.intersect(this.mTmpRect);
        }
        if (rect.isEmpty()) {
            rect.set(this.mFrame);
            if (z) {
                rect.intersect(this.mTmpRect);
            }
        }
    }

    public long getInputDispatchingTimeoutNanos() {
        if (this.mAppToken != null) {
            return this.mAppToken.mInputDispatchingTimeoutNanos;
        }
        return 30000000000L;
    }

    @Override
    public boolean hasAppShownWindows() {
        return this.mAppToken != null && (this.mAppToken.firstWindowDrawn || this.mAppToken.startingDisplayed);
    }

    boolean isIdentityMatrix(float f, float f2, float f3, float f4) {
        return f >= 0.99999f && f <= 1.00001f && f4 >= 0.99999f && f4 <= 1.00001f && f2 >= -1.0E-6f && f2 <= 1.0E-6f && f3 >= -1.0E-6f && f3 <= 1.0E-6f;
    }

    void prelayout() {
        if (this.mEnforceSizeCompat) {
            this.mGlobalScale = getDisplayContent().mCompatibleScreenScale;
            if (this.mNeedHWResizer) {
                this.mGlobalScale = this.mHWScale;
                Slog.v("Scale_Test", "windowstate prelayout() Need HWResizer, mGlobalScale = " + this.mGlobalScale + " , this = " + this);
            }
            this.mInvGlobalScale = 1.0f / this.mGlobalScale;
            return;
        }
        this.mInvGlobalScale = 1.0f;
        this.mGlobalScale = 1.0f;
    }

    @Override
    boolean hasContentToDisplay() {
        if (!this.mAppFreezing && isDrawnLw()) {
            if (this.mViewVisibility != 0) {
                if (this.mWinAnimator.isAnimationSet() && !this.mService.mAppTransition.isTransitionSet()) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return super.hasContentToDisplay();
    }

    @Override
    boolean isVisible() {
        return wouldBeVisibleIfPolicyIgnored() && this.mPolicyVisibility;
    }

    boolean wouldBeVisibleIfPolicyIgnored() {
        return (!this.mHasSurface || isParentWindowHidden() || this.mAnimatingExit || this.mDestroying || (this.mIsWallpaper && !this.mWallpaperVisible)) ? false : true;
    }

    @Override
    public boolean isVisibleLw() {
        return isVisible();
    }

    boolean isWinVisibleLw() {
        return (this.mAppToken == null || !this.mAppToken.hiddenRequested || this.mAppToken.isSelfAnimating()) && isVisible();
    }

    boolean isVisibleNow() {
        return (!this.mToken.isHidden() || this.mAttrs.type == 3) && isVisible();
    }

    boolean isPotentialDragTarget() {
        return (!isVisibleNow() || this.mRemoved || this.mInputChannel == null || this.mInputWindowHandle == null) ? false : true;
    }

    boolean isVisibleOrAdding() {
        AppWindowToken appWindowToken = this.mAppToken;
        return (this.mHasSurface || (!this.mRelayoutCalled && this.mViewVisibility == 0)) && this.mPolicyVisibility && !isParentWindowHidden() && !((appWindowToken != null && appWindowToken.hiddenRequested) || this.mAnimatingExit || this.mDestroying);
    }

    boolean isOnScreen() {
        if (!this.mHasSurface || this.mDestroying || !this.mPolicyVisibility) {
            return false;
        }
        AppWindowToken appWindowToken = this.mAppToken;
        return appWindowToken != null ? !(isParentWindowHidden() || appWindowToken.hiddenRequested) || this.mWinAnimator.isAnimationSet() : !isParentWindowHidden() || this.mWinAnimator.isAnimationSet();
    }

    boolean mightAffectAllDrawn() {
        return ((!isOnScreen() && !(this.mWinAnimator.mAttrType == 1 || this.mWinAnimator.mAttrType == 4)) || this.mAnimatingExit || this.mDestroying) ? false : true;
    }

    boolean isInteresting() {
        return (this.mAppToken == null || this.mAppDied || (this.mAppToken.isFreezingScreen() && this.mAppFreezing)) ? false : true;
    }

    boolean isReadyForDisplay() {
        if (!(this.mToken.waitingToShow && this.mService.mAppTransition.isTransitionSet()) && this.mHasSurface && this.mPolicyVisibility && !this.mDestroying) {
            return !(isParentWindowHidden() || this.mViewVisibility != 0 || this.mToken.isHidden()) || this.mWinAnimator.isAnimationSet();
        }
        return false;
    }

    @Override
    public boolean canAffectSystemUiFlags() {
        if (this.mAttrs.alpha == 0.0f) {
            return false;
        }
        if (this.mAppToken == null) {
            return this.mWinAnimator.getShown() && !(this.mAnimatingExit || this.mDestroying);
        }
        Task task = getTask();
        return (task != null && task.canAffectSystemUiFlags()) && !this.mAppToken.isHidden();
    }

    @Override
    public boolean isDisplayedLw() {
        AppWindowToken appWindowToken = this.mAppToken;
        return isDrawnLw() && this.mPolicyVisibility && ((!isParentWindowHidden() && (appWindowToken == null || !appWindowToken.hiddenRequested)) || this.mWinAnimator.isAnimationSet());
    }

    @Override
    public boolean isAnimatingLw() {
        return isAnimating();
    }

    @Override
    public boolean isGoneForLayoutLw() {
        AppWindowToken appWindowToken = this.mAppToken;
        return this.mViewVisibility == 8 || !this.mRelayoutCalled || (appWindowToken == null && this.mToken.isHidden()) || ((appWindowToken != null && appWindowToken.hiddenRequested) || isParentWindowHidden() || ((this.mAnimatingExit && !isAnimatingLw()) || this.mDestroying));
    }

    public boolean isDrawFinishedLw() {
        return this.mHasSurface && !this.mDestroying && (this.mWinAnimator.mDrawState == 2 || this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4);
    }

    @Override
    public boolean isDrawnLw() {
        return this.mHasSurface && !this.mDestroying && (this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4);
    }

    private boolean isOpaqueDrawn() {
        return ((!this.mIsWallpaper && this.mAttrs.format == -1) || (this.mIsWallpaper && this.mWallpaperVisible)) && isDrawnLw() && !this.mWinAnimator.isAnimationSet();
    }

    @Override
    void onMovedByResize() {
        if (WindowManagerDebugConfig.DEBUG_RESIZE) {
            Slog.d("WindowManager", "onMovedByResize: Moving " + this);
        }
        this.mMovedByResize = true;
        super.onMovedByResize();
    }

    boolean onAppVisibilityChanged(boolean z, boolean z2) {
        boolean zOnAppVisibilityChanged = false;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            zOnAppVisibilityChanged |= ((WindowState) this.mChildren.get(size)).onAppVisibilityChanged(z, z2);
        }
        if (this.mAttrs.type == 3) {
            if (!z && isVisibleNow() && this.mAppToken.isSelfAnimating()) {
                this.mAnimatingExit = true;
                this.mRemoveOnExit = true;
                this.mWindowRemovalAllowed = true;
            }
            return zOnAppVisibilityChanged;
        }
        boolean zIsVisibleNow = isVisibleNow();
        if (z != zIsVisibleNow) {
            if (!z2 && zIsVisibleNow) {
                AccessibilityController accessibilityController = this.mService.mAccessibilityController;
                this.mWinAnimator.applyAnimationLocked(2, false);
                if (accessibilityController != null && getDisplayId() == 0) {
                    accessibilityController.onWindowTransitionLocked(this, 2);
                }
            }
            setDisplayLayoutNeeded();
            return true;
        }
        return zOnAppVisibilityChanged;
    }

    boolean onSetAppExiting() {
        DisplayContent displayContent = getDisplayContent();
        boolean zOnSetAppExiting = false;
        if (isVisibleNow()) {
            this.mWinAnimator.applyAnimationLocked(2, false);
            if (this.mService.mAccessibilityController != null && isDefaultDisplay()) {
                this.mService.mAccessibilityController.onWindowTransitionLocked(this, 2);
            }
            if (displayContent != null) {
                displayContent.setLayoutNeeded();
            }
            zOnSetAppExiting = true;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            zOnSetAppExiting |= ((WindowState) this.mChildren.get(size)).onSetAppExiting();
        }
        return zOnSetAppExiting;
    }

    @Override
    void onResize() {
        ArrayList<WindowState> arrayList = this.mService.mResizingWindows;
        if (this.mHasSurface && !isGoneForLayoutLw() && !arrayList.contains(this)) {
            if (WindowManagerDebugConfig.DEBUG_RESIZE) {
                Slog.d("WindowManager", "onResize: Resizing " + this);
            }
            arrayList.add(this);
        }
        if (isGoneForLayoutLw()) {
            this.mResizedWhileGone = true;
        }
        super.onResize();
    }

    void onUnfreezeBounds() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).onUnfreezeBounds();
        }
        if (!this.mHasSurface) {
            return;
        }
        this.mLayoutNeeded = true;
        setDisplayLayoutNeeded();
        if (!this.mService.mResizingWindows.contains(this)) {
            this.mService.mResizingWindows.add(this);
        }
    }

    void handleWindowMovedIfNeeded() {
        if (!hasMoved()) {
            return;
        }
        int i = this.mFrame.left;
        int i2 = this.mFrame.top;
        Task task = getTask();
        boolean z = task != null && (task.mStack.isAdjustedForMinimizedDockedStack() || task.mStack.isAdjustedForIme());
        if (this.mToken.okToAnimate() && (this.mAttrs.privateFlags & 64) == 0 && !isDragResizing() && !z && getWindowConfiguration().hasMovementAnimations() && !this.mWinAnimator.mLastHidden) {
            startMoveAnimation(i, i2);
        }
        if (this.mService.mAccessibilityController != null && getDisplayContent().getDisplayId() == 0) {
            this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
        }
        try {
            this.mClient.moved(i, i2);
        } catch (RemoteException e) {
        }
        this.mMovedByResize = false;
    }

    private boolean hasMoved() {
        return this.mHasSurface && !((!this.mContentChanged && !this.mMovedByResize) || this.mAnimatingExit || ((this.mFrame.top == this.mLastFrame.top && this.mFrame.left == this.mLastFrame.left) || (this.mIsChildWindow && getParentWindow().hasMoved())));
    }

    boolean isObscuringDisplay() {
        Task task = getTask();
        return (task == null || task.mStack == null || task.mStack.fillsParent()) && isOpaqueDrawn() && fillsDisplay();
    }

    boolean fillsDisplay() {
        DisplayInfo displayInfo = getDisplayInfo();
        return this.mFrame.left <= 0 && this.mFrame.top <= 0 && this.mFrame.right >= displayInfo.appWidth && this.mFrame.bottom >= displayInfo.appHeight;
    }

    boolean isConfigChanged() {
        return !getLastReportedConfiguration().equals(getConfiguration());
    }

    void onWindowReplacementTimeout() {
        if (this.mWillReplaceWindow) {
            removeImmediately();
            return;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).onWindowReplacementTimeout();
        }
    }

    @Override
    void forceWindowsScaleableInTransaction(boolean z) {
        if (this.mWinAnimator != null && this.mWinAnimator.hasSurface()) {
            this.mWinAnimator.mSurfaceController.forceScaleableInTransaction(z);
        }
        super.forceWindowsScaleableInTransaction(z);
    }

    @Override
    void removeImmediately() {
        super.removeImmediately();
        if (this.mRemoved) {
            if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v("WindowManager", "WS.removeImmediately: " + this + " Already removed...");
                return;
            }
            return;
        }
        this.mRemoved = true;
        this.mWillReplaceWindow = false;
        if (this.mReplacementWindow != null) {
            this.mReplacementWindow.mSkipEnterAnimationForSeamlessReplacement = false;
        }
        DisplayContent displayContent = getDisplayContent();
        if (isInputMethodTarget()) {
            displayContent.computeImeTarget(true);
        }
        if (WindowManagerService.excludeWindowTypeFromTapOutTask(this.mAttrs.type)) {
            displayContent.mTapExcludedWindows.remove(this);
        }
        if (this.mTapExcludeRegionHolder != null) {
            displayContent.mTapExcludeProvidingWindows.remove(this);
        }
        this.mPolicy.removeWindowLw(this);
        disposeInputChannel();
        this.mWinAnimator.destroyDeferredSurfaceLocked();
        this.mWinAnimator.destroySurfaceLocked();
        this.mSession.windowRemovedLocked();
        try {
            this.mClient.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
        } catch (RuntimeException e) {
        }
        this.mService.postWindowRemoveCleanupLocked(this);
    }

    @Override
    void removeIfPossible() {
        super.removeIfPossible();
        removeIfPossible(false);
    }

    private void removeIfPossible(boolean z) {
        boolean zIsWinVisibleLw;
        this.mWindowRemovalAllowed = true;
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "removeIfPossible: " + this + " callers=" + Debug.getCallers(5));
        }
        boolean z2 = this.mAttrs.type == 3;
        if (z2 && WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            Slog.d("WindowManager", "Starting window removed " + this);
        }
        if (WindowManagerService.localLOGV || WindowManagerDebugConfig.DEBUG_FOCUS || (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT && this == this.mService.mCurrentFocus)) {
            Slog.v("WindowManager", "Remove " + this + " client=" + Integer.toHexString(System.identityHashCode(this.mClient.asBinder())) + ", surfaceController=" + this.mWinAnimator.mSurfaceController + " Callers=" + Debug.getCallers(5));
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            disposeInputChannel();
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                StringBuilder sb = new StringBuilder();
                sb.append("Remove ");
                sb.append(this);
                sb.append(": mSurfaceController=");
                sb.append(this.mWinAnimator.mSurfaceController);
                sb.append(" mAnimatingExit=");
                sb.append(this.mAnimatingExit);
                sb.append(" mRemoveOnExit=");
                sb.append(this.mRemoveOnExit);
                sb.append(" mHasSurface=");
                sb.append(this.mHasSurface);
                sb.append(" surfaceShowing=");
                sb.append(this.mWinAnimator.getShown());
                sb.append(" isAnimationSet=");
                sb.append(this.mWinAnimator.isAnimationSet());
                sb.append(" app-animation=");
                sb.append(this.mAppToken != null ? Boolean.valueOf(this.mAppToken.isSelfAnimating()) : "false");
                sb.append(" mWillReplaceWindow=");
                sb.append(this.mWillReplaceWindow);
                sb.append(" inPendingTransaction=");
                sb.append(this.mAppToken != null ? this.mAppToken.inPendingTransaction : false);
                sb.append(" mDisplayFrozen=");
                sb.append(this.mService.mDisplayFrozen);
                sb.append(" callers=");
                sb.append(Debug.getCallers(6));
                Slog.v("WindowManager", sb.toString());
            }
            int displayId = getDisplayId();
            if (!this.mHasSurface || !this.mToken.okToAnimate()) {
                zIsWinVisibleLw = false;
            } else {
                if (this.mWillReplaceWindow) {
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        Slog.v("WindowManager", "Preserving " + this + " until the new one is added");
                    }
                    this.mAnimatingExit = true;
                    this.mReplacingRemoveRequested = true;
                    return;
                }
                zIsWinVisibleLw = isWinVisibleLw();
                if (z) {
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        Slog.v("WindowManager", "Not removing " + this + " because app died while it's visible");
                    }
                    this.mAppDied = true;
                    setDisplayLayoutNeeded();
                    this.mService.mWindowPlacerLocked.performSurfacePlacement();
                    openInputChannel(null);
                    this.mService.mInputMonitor.updateInputWindowsLw(true);
                    return;
                }
                if (zIsWinVisibleLw) {
                    int i = z2 ? 5 : 2;
                    if (this.mWinAnimator.applyAnimationLocked(i, false)) {
                        this.mAnimatingExit = true;
                        setDisplayLayoutNeeded();
                        this.mService.requestTraversal();
                    }
                    if (this.mService.mAccessibilityController != null && displayId == 0) {
                        this.mService.mAccessibilityController.onWindowTransitionLocked(this, i);
                    }
                }
                boolean z3 = this.mWinAnimator.isAnimationSet() && (this.mAppToken == null || !this.mAppToken.isWaitingForTransitionStart());
                boolean z4 = z2 && this.mAppToken != null && this.mAppToken.isLastWindow(this);
                if (this.mWinAnimator.getShown() && this.mAnimatingExit && (!z4 || z3)) {
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        Slog.v("WindowManager", "Not removing " + this + " due to exit animation ");
                    }
                    setupWindowForRemoveOnExit();
                    if (this.mAppToken != null) {
                        this.mAppToken.updateReportedVisibilityLocked();
                    }
                    return;
                }
                this.mAnimatingExit = false;
            }
            removeImmediately();
            if (zIsWinVisibleLw && this.mService.updateOrientationFromAppTokensLocked(displayId)) {
                this.mService.mH.obtainMessage(18, Integer.valueOf(displayId)).sendToTarget();
            }
            this.mService.updateFocusedWindowLocked(0, true);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void setupWindowForRemoveOnExit() {
        this.mRemoveOnExit = true;
        setDisplayLayoutNeeded();
        boolean zUpdateFocusedWindowLocked = this.mService.updateFocusedWindowLocked(3, false);
        this.mService.mWindowPlacerLocked.performSurfacePlacement();
        if (zUpdateFocusedWindowLocked) {
            this.mService.mInputMonitor.updateInputWindowsLw(false);
        }
    }

    void setHasSurface(boolean z) {
        this.mHasSurface = z;
    }

    boolean canBeImeTarget() {
        boolean z;
        if (this.mIsImWindow) {
            return false;
        }
        if (this.mAppToken == null || this.mAppToken.windowsAreFocusable()) {
            z = true;
        } else {
            z = false;
        }
        if (!z) {
            return false;
        }
        int i = this.mAttrs.flags & 131080;
        int i2 = this.mAttrs.type;
        if (i != 0 && i != 131080 && i2 != 3) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.i("WindowManager", "isVisibleOrAdding " + this + ": " + isVisibleOrAdding());
            if (!isVisibleOrAdding()) {
                Slog.i("WindowManager", "  mSurfaceController=" + this.mWinAnimator.mSurfaceController + " relayoutCalled=" + this.mRelayoutCalled + " viewVis=" + this.mViewVisibility + " policyVis=" + this.mPolicyVisibility + " policyVisAfterAnim=" + this.mPolicyVisibilityAfterAnim + " parentHidden=" + isParentWindowHidden() + " exiting=" + this.mAnimatingExit + " destroying=" + this.mDestroying);
                if (this.mAppToken != null) {
                    Slog.i("WindowManager", "  mAppToken.hiddenRequested=" + this.mAppToken.hiddenRequested);
                }
            }
        }
        return isVisibleOrAdding();
    }

    private final class DeadWindowEventReceiver extends InputEventReceiver {
        DeadWindowEventReceiver(InputChannel inputChannel) {
            super(inputChannel, WindowState.this.mService.mH.getLooper());
        }

        public void onInputEvent(InputEvent inputEvent, int i) {
            finishInputEvent(inputEvent, true);
        }
    }

    void openInputChannel(InputChannel inputChannel) {
        if (this.mInputChannel != null) {
            throw new IllegalStateException("Window already has an input channel.");
        }
        InputChannel[] inputChannelArrOpenInputChannelPair = InputChannel.openInputChannelPair(getName());
        this.mInputChannel = inputChannelArrOpenInputChannelPair[0];
        this.mClientChannel = inputChannelArrOpenInputChannelPair[1];
        this.mInputWindowHandle.inputChannel = inputChannelArrOpenInputChannelPair[0];
        if (inputChannel != null) {
            this.mClientChannel.transferTo(inputChannel);
            this.mClientChannel.dispose();
            this.mClientChannel = null;
        } else {
            this.mDeadWindowEventReceiver = new DeadWindowEventReceiver(this.mClientChannel);
        }
        this.mService.mInputManager.registerInputChannel(this.mInputChannel, this.mInputWindowHandle);
    }

    void disposeInputChannel() {
        if (this.mDeadWindowEventReceiver != null) {
            this.mDeadWindowEventReceiver.dispose();
            this.mDeadWindowEventReceiver = null;
        }
        if (this.mInputChannel != null) {
            this.mService.mInputManager.unregisterInputChannel(this.mInputChannel);
            this.mInputChannel.dispose();
            this.mInputChannel = null;
        }
        if (this.mClientChannel != null) {
            this.mClientChannel.dispose();
            this.mClientChannel = null;
        }
        this.mInputWindowHandle.inputChannel = null;
    }

    boolean removeReplacedWindowIfNeeded(WindowState windowState) {
        if (this.mWillReplaceWindow && this.mReplacementWindow == windowState && windowState.hasDrawnLw()) {
            windowState.mSkipEnterAnimationForSeamlessReplacement = false;
            removeReplacedWindow();
            return true;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (((WindowState) this.mChildren.get(size)).removeReplacedWindowIfNeeded(windowState)) {
                return true;
            }
        }
        return false;
    }

    private void removeReplacedWindow() {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.d("WindowManager", "Removing replaced window: " + this);
        }
        this.mWillReplaceWindow = false;
        this.mAnimateReplacingWindow = false;
        this.mReplacingRemoveRequested = false;
        this.mReplacementWindow = null;
        if (this.mAnimatingExit || !this.mAnimateReplacingWindow) {
            removeImmediately();
        }
    }

    boolean setReplacementWindowIfNeeded(WindowState windowState) {
        boolean replacementWindowIfNeeded;
        if (this.mWillReplaceWindow && this.mReplacementWindow == null && getWindowTag().toString().equals(windowState.getWindowTag().toString())) {
            this.mReplacementWindow = windowState;
            windowState.mSkipEnterAnimationForSeamlessReplacement = !this.mAnimateReplacingWindow;
            replacementWindowIfNeeded = true;
        } else {
            replacementWindowIfNeeded = false;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            replacementWindowIfNeeded |= ((WindowState) this.mChildren.get(size)).setReplacementWindowIfNeeded(windowState);
        }
        return replacementWindowIfNeeded;
    }

    void setDisplayLayoutNeeded() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent != null) {
            displayContent.setLayoutNeeded();
        }
    }

    void applyAdjustForImeIfNeeded() {
        Task task = getTask();
        if (task != null && task.mStack != null && task.mStack.isAdjustedForIme()) {
            task.mStack.applyAdjustForImeIfNeeded(task);
        }
    }

    @Override
    void switchUser() {
        super.switchUser();
        if (isHiddenFromUserLocked()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.w("WindowManager", "user changing, hiding " + this + ", attrs=" + this.mAttrs.type + ", belonging to " + this.mOwnerUid);
            }
            hideLw(false);
        }
    }

    int getTouchableRegion(Region region, int i) {
        if (((i & 40) == 0) && this.mAppToken != null) {
            i |= 32;
            Task task = getTask();
            if (task != null) {
                task.getDimBounds(this.mTmpRect);
            } else {
                getStack().getDimBounds(this.mTmpRect);
            }
            if (inFreeformWindowingMode()) {
                int i2 = -WindowManagerService.dipToPixel(30, getDisplayContent().getDisplayMetrics());
                this.mTmpRect.inset(i2, i2);
            }
            region.set(this.mTmpRect);
            cropRegionToStackBoundsIfNeeded(region);
        } else {
            getTouchableRegion(region);
        }
        return i;
    }

    void checkPolicyVisibilityChange() {
        if (this.mPolicyVisibility != this.mPolicyVisibilityAfterAnim) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "Policy visibility changing after anim in " + this.mWinAnimator + ": " + this.mPolicyVisibilityAfterAnim);
            }
            this.mPolicyVisibility = this.mPolicyVisibilityAfterAnim;
            if (!this.mPolicyVisibility) {
                this.mWinAnimator.hide("checkPolicyVisibilityChange");
                if (this.mService.mCurrentFocus == this) {
                    if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                        Slog.i("WindowManager", "setAnimationLocked: setting mFocusMayChange true");
                    }
                    this.mService.mFocusMayChange = true;
                }
                setDisplayLayoutNeeded();
                this.mService.enableScreenIfNeededLocked();
            }
        }
    }

    void setRequestedSize(int i, int i2) {
        if (this.mRequestedWidth != i || this.mRequestedHeight != i2) {
            this.mLayoutNeeded = true;
            this.mRequestedWidth = i;
            this.mRequestedHeight = i2;
        }
    }

    void prepareWindowToDisplayDuringRelayout(boolean z) {
        boolean z2 = (this.mAttrs.flags & DumpState.DUMP_COMPILER_STATS) != 0;
        boolean z3 = this.mService.mAllowTheaterModeWakeFromLayout || Settings.Global.getInt(this.mService.mContext.getContentResolver(), "theater_mode_on", 0) == 0;
        boolean z4 = this.mAppToken == null || this.mAppToken.canTurnScreenOn();
        if (z2) {
            if (z3 && z4 && !this.mPowerManagerWrapper.isInteractive()) {
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_POWER) {
                    Slog.v("WindowManager", "Relayout window turning screen on: " + this);
                    this.mPowerManagerWrapper.wakeUp(SystemClock.uptimeMillis(), "android.server.wm:TURN_ON");
                } else {
                    WindowManagerDebugger windowManagerDebugger = this.mService.mWindowManagerDebugger;
                    if (WindowManagerDebugger.WMS_DEBUG_USER) {
                    }
                    this.mPowerManagerWrapper.wakeUp(SystemClock.uptimeMillis(), "android.server.wm:TURN_ON");
                }
            }
            if (this.mAppToken != null) {
                this.mAppToken.setCanTurnScreenOn(false);
            }
        }
        if (z) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "Already visible and does not turn on screen, skip preparing: " + this);
                return;
            }
            return;
        }
        if ((this.mAttrs.softInputMode & 240) == 16) {
            this.mLayoutNeeded = true;
        }
        if (isDrawnLw() && this.mToken.okToAnimate()) {
            this.mWinAnimator.applyEnterAnimationLocked();
        }
    }

    void getMergedConfiguration(MergedConfiguration mergedConfiguration) {
        mergedConfiguration.setConfiguration(this.mService.mRoot.getConfiguration(), getMergedOverrideConfiguration());
    }

    void setLastReportedMergedConfiguration(MergedConfiguration mergedConfiguration) {
        this.mLastReportedConfiguration.setTo(mergedConfiguration);
    }

    void getLastReportedMergedConfiguration(MergedConfiguration mergedConfiguration) {
        mergedConfiguration.setTo(this.mLastReportedConfiguration);
    }

    private Configuration getLastReportedConfiguration() {
        return this.mLastReportedConfiguration.getMergedConfiguration();
    }

    void adjustStartingWindowFlags() {
        if (this.mAttrs.type == 1 && this.mAppToken != null && this.mAppToken.startingWindow != null) {
            WindowManager.LayoutParams layoutParams = this.mAppToken.startingWindow.mAttrs;
            layoutParams.flags = (layoutParams.flags & (-4718594)) | (this.mAttrs.flags & 4718593);
        }
    }

    void setWindowScale(int i, int i2) {
        float f;
        float f2 = 1.0f;
        if ((this.mAttrs.flags & 16384) != 0) {
            if (this.mAttrs.width == i) {
                f = 1.0f;
            } else {
                f = this.mAttrs.width / i;
            }
            this.mHScale = f;
            if (this.mAttrs.height != i2) {
                f2 = this.mAttrs.height / i2;
            }
            this.mVScale = f2;
            return;
        }
        this.mVScale = 1.0f;
        this.mHScale = 1.0f;
    }

    private class DeathRecipient implements IBinder.DeathRecipient {
        private DeathRecipient() {
        }

        @Override
        public void binderDied() {
            boolean z;
            try {
                synchronized (WindowState.this.mService.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        WindowState windowStateWindowForClientLocked = WindowState.this.mService.windowForClientLocked(WindowState.this.mSession, WindowState.this.mClient, false);
                        Slog.i("WindowManager", "WIN DEATH: " + windowStateWindowForClientLocked);
                        if (windowStateWindowForClientLocked != null) {
                            DisplayContent displayContent = WindowState.this.getDisplayContent();
                            if (windowStateWindowForClientLocked.mAppToken != null && windowStateWindowForClientLocked.mAppToken.findMainWindow() == windowStateWindowForClientLocked) {
                                WindowState.this.mService.mTaskSnapshotController.onAppDied(windowStateWindowForClientLocked.mAppToken);
                            }
                            windowStateWindowForClientLocked.removeIfPossible(WindowState.this.shouldKeepVisibleDeadAppWindow());
                            if (windowStateWindowForClientLocked.mAttrs.type == 2034) {
                                TaskStack splitScreenPrimaryStackIgnoringVisibility = displayContent.getSplitScreenPrimaryStackIgnoringVisibility();
                                if (splitScreenPrimaryStackIgnoringVisibility != null) {
                                    splitScreenPrimaryStackIgnoringVisibility.resetDockedStackToMiddle();
                                }
                                z = true;
                            } else {
                                z = false;
                            }
                        } else {
                            if (WindowState.this.mHasSurface) {
                                Slog.e("WindowManager", "!!! LEAK !!! Window removed but surface still valid.");
                                WindowState.this.removeIfPossible();
                            }
                            z = false;
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                if (z) {
                    try {
                        WindowState.this.mService.mActivityManager.setSplitScreenResizing(false);
                    } catch (RemoteException e) {
                        throw e.rethrowAsRuntimeException();
                    }
                }
            } catch (IllegalArgumentException e2) {
            }
        }
    }

    private boolean shouldKeepVisibleDeadAppWindow() {
        if (!isWinVisibleLw() || this.mAppToken == null || this.mAppToken.isClientHidden() || this.mAttrs.token != this.mClient.asBinder() || this.mAttrs.type == 3) {
            return false;
        }
        return getWindowConfiguration().keepVisibleDeadAppWindowOnScreen();
    }

    boolean canReceiveKeys() {
        return isVisibleOrAdding() && this.mViewVisibility == 0 && !this.mRemoveOnExit && (this.mAttrs.flags & 8) == 0 && (this.mAppToken == null || this.mAppToken.windowsAreFocusable()) && !canReceiveTouchInput();
    }

    boolean canReceiveTouchInput() {
        return (this.mAppToken == null || this.mAppToken.getTask() == null || !this.mAppToken.getTask().mStack.shouldIgnoreInput()) ? false : true;
    }

    @Override
    public boolean hasDrawnLw() {
        return this.mWinAnimator.mDrawState == 4;
    }

    @Override
    public boolean showLw(boolean z) {
        return showLw(z, true);
    }

    boolean showLw(boolean z, boolean z2) {
        if (isHiddenFromUserLocked() || !this.mAppOpVisibility || this.mPermanentlyHidden || this.mHiddenWhileSuspended || this.mForceHideNonSystemOverlayWindow) {
            return false;
        }
        if (this.mPolicyVisibility && this.mPolicyVisibilityAfterAnim) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Policy visibility true: " + this);
        }
        if (z) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "doAnimation: mPolicyVisibility=" + this.mPolicyVisibility + " isAnimationSet=" + this.mWinAnimator.isAnimationSet());
            }
            if (!this.mToken.okToAnimate() || (this.mPolicyVisibility && !this.mWinAnimator.isAnimationSet())) {
                z = false;
            }
        }
        this.mPolicyVisibility = true;
        this.mPolicyVisibilityAfterAnim = true;
        if (z) {
            this.mWinAnimator.applyAnimationLocked(1, true);
        }
        if (z2) {
            this.mService.scheduleAnimationLocked();
        }
        if ((this.mAttrs.flags & 8) == 0) {
            this.mService.updateFocusedWindowLocked(0, false);
        }
        return true;
    }

    @Override
    public boolean hideLw(boolean z) {
        return hideLw(z, true);
    }

    boolean hideLw(boolean z, boolean z2) {
        if (z && !this.mToken.okToAnimate()) {
            z = false;
        }
        if (!(z ? this.mPolicyVisibilityAfterAnim : this.mPolicyVisibility)) {
            return false;
        }
        if (z) {
            this.mWinAnimator.applyAnimationLocked(2, false);
            if (!this.mWinAnimator.isAnimationSet()) {
                z = false;
            }
        }
        this.mPolicyVisibilityAfterAnim = false;
        if (!z) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "Policy visibility false: " + this);
            }
            this.mPolicyVisibility = false;
            this.mService.enableScreenIfNeededLocked();
            if (this.mService.mCurrentFocus == this) {
                if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                    Slog.i("WindowManager", "WindowState.hideLw: setting mFocusMayChange true");
                }
                this.mService.mFocusMayChange = true;
            }
        }
        if (z2) {
            this.mService.scheduleAnimationLocked();
        }
        if (this.mService.mCurrentFocus == this) {
            this.mService.updateFocusedWindowLocked(0, false);
        }
        return true;
    }

    void setForceHideNonSystemOverlayWindowIfNeeded(boolean z) {
        if (!this.mOwnerCanAddInternalSystemWindow) {
            if ((!WindowManager.LayoutParams.isSystemAlertWindowType(this.mAttrs.type) && this.mAttrs.type != 2005) || this.mForceHideNonSystemOverlayWindow == z) {
                return;
            }
            this.mForceHideNonSystemOverlayWindow = z;
            if (z) {
                hideLw(true, true);
            } else {
                showLw(true, true);
            }
        }
    }

    void setHiddenWhileSuspended(boolean z) {
        if (!this.mOwnerCanAddInternalSystemWindow) {
            if ((!WindowManager.LayoutParams.isSystemAlertWindowType(this.mAttrs.type) && this.mAttrs.type != 2005) || this.mHiddenWhileSuspended == z) {
                return;
            }
            this.mHiddenWhileSuspended = z;
            if (z) {
                hideLw(true, true);
            } else {
                showLw(true, true);
            }
        }
    }

    private void setAppOpVisibilityLw(boolean z) {
        if (this.mAppOpVisibility != z) {
            this.mAppOpVisibility = z;
            if (z) {
                showLw(true, true);
            } else {
                hideLw(true, true);
            }
        }
    }

    void initAppOpsState() {
        int iStartOpNoThrow;
        if (this.mAppOp != -1 && this.mAppOpVisibility && (iStartOpNoThrow = this.mService.mAppOps.startOpNoThrow(this.mAppOp, getOwningUid(), getOwningPackage(), true)) != 0 && iStartOpNoThrow != 3) {
            setAppOpVisibilityLw(false);
        }
    }

    void resetAppOpsState() {
        if (this.mAppOp != -1 && this.mAppOpVisibility) {
            this.mService.mAppOps.finishOp(this.mAppOp, getOwningUid(), getOwningPackage());
        }
    }

    void updateAppOpsState() {
        if (this.mAppOp == -1) {
            return;
        }
        int owningUid = getOwningUid();
        String owningPackage = getOwningPackage();
        if (this.mAppOpVisibility) {
            int iCheckOpNoThrow = this.mService.mAppOps.checkOpNoThrow(this.mAppOp, owningUid, owningPackage);
            if (iCheckOpNoThrow != 0 && iCheckOpNoThrow != 3) {
                this.mService.mAppOps.finishOp(this.mAppOp, owningUid, owningPackage);
                setAppOpVisibilityLw(false);
                return;
            }
            return;
        }
        int iStartOpNoThrow = this.mService.mAppOps.startOpNoThrow(this.mAppOp, owningUid, owningPackage, true);
        if (iStartOpNoThrow == 0 || iStartOpNoThrow == 3) {
            setAppOpVisibilityLw(true);
        }
    }

    public void hidePermanentlyLw() {
        if (!this.mPermanentlyHidden) {
            this.mPermanentlyHidden = true;
            hideLw(true, true);
        }
    }

    public void pokeDrawLockLw(long j) {
        if (isVisibleOrAdding()) {
            if (this.mDrawLock == null) {
                CharSequence windowTag = getWindowTag();
                this.mDrawLock = this.mService.mPowerManager.newWakeLock(128, "Window:" + ((Object) windowTag));
                this.mDrawLock.setReferenceCounted(false);
                this.mDrawLock.setWorkSource(new WorkSource(this.mOwnerUid, this.mAttrs.packageName));
            }
            if (WindowManagerDebugConfig.DEBUG_POWER) {
                Slog.d("WindowManager", "pokeDrawLock: poking draw lock on behalf of visible window owned by " + this.mAttrs.packageName);
            }
            this.mDrawLock.acquire(j);
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_POWER) {
            Slog.d("WindowManager", "pokeDrawLock: suppressed draw lock request for invisible window owned by " + this.mAttrs.packageName);
        }
    }

    @Override
    public boolean isAlive() {
        return this.mClient.asBinder().isBinderAlive();
    }

    boolean isClosing() {
        return this.mAnimatingExit || (this.mAppToken != null && this.mAppToken.isClosingOrEnteringPip());
    }

    void addWinAnimatorToList(ArrayList<WindowStateAnimator> arrayList) {
        arrayList.add(this.mWinAnimator);
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).addWinAnimatorToList(arrayList);
        }
    }

    @Override
    void sendAppVisibilityToClients() {
        super.sendAppVisibilityToClients();
        boolean zIsClientHidden = this.mAppToken.isClientHidden();
        if (this.mAttrs.type == 3 && zIsClientHidden) {
            return;
        }
        if (zIsClientHidden) {
            for (int size = this.mChildren.size() - 1; size >= 0; size--) {
                ((WindowState) this.mChildren.get(size)).mWinAnimator.detachChildren();
            }
            this.mWinAnimator.detachChildren();
        }
        try {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                StringBuilder sb = new StringBuilder();
                sb.append("Setting visibility of ");
                sb.append(this);
                sb.append(": ");
                sb.append(!zIsClientHidden);
                Slog.v("WindowManager", sb.toString());
            }
            this.mClient.dispatchAppVisibility(!zIsClientHidden);
        } catch (RemoteException e) {
        }
    }

    void onStartFreezingScreen() {
        this.mAppFreezing = true;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).onStartFreezingScreen();
        }
    }

    boolean onStopFreezingScreen() {
        boolean zOnStopFreezingScreen = false;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            zOnStopFreezingScreen |= ((WindowState) this.mChildren.get(size)).onStopFreezingScreen();
        }
        if (!this.mAppFreezing) {
            return zOnStopFreezingScreen;
        }
        this.mAppFreezing = false;
        if (this.mHasSurface && !getOrientationChanging() && this.mService.mWindowsFreezingScreen != 2) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "set mOrientationChanging of " + this);
            }
            setOrientationChanging(true);
            this.mService.mRoot.mOrientationChangeComplete = false;
        }
        this.mLastFreezeDuration = 0;
        setDisplayLayoutNeeded();
        return true;
    }

    boolean destroySurface(boolean z, boolean z2) {
        ArrayList arrayList = new ArrayList(this.mChildren);
        boolean zDestroySurface = false;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            zDestroySurface |= ((WindowState) arrayList.get(size)).destroySurface(z, z2);
        }
        if (!z2 && !this.mWindowRemovalAllowed && !z) {
            return zDestroySurface;
        }
        if (z2 || this.mWindowRemovalAllowed) {
            this.mWinAnimator.destroyPreservedSurfaceLocked();
        }
        if (this.mDestroying) {
            if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.e("WindowManager", "win=" + this + " destroySurfaces: appStopped=" + z2 + " win.mWindowRemovalAllowed=" + this.mWindowRemovalAllowed + " win.mRemoveOnExit=" + this.mRemoveOnExit);
            }
            if (!z || this.mRemoveOnExit) {
                destroySurfaceUnchecked();
            }
            if (this.mRemoveOnExit) {
                removeImmediately();
            }
            if (z) {
                requestUpdateWallpaperIfNeeded();
            }
            this.mDestroying = false;
            return true;
        }
        return zDestroySurface;
    }

    void destroySurfaceUnchecked() {
        this.mWinAnimator.destroySurfaceLocked();
        this.mAnimatingExit = false;
    }

    @Override
    public boolean isDefaultDisplay() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return false;
        }
        return displayContent.isDefaultDisplay;
    }

    void setShowToOwnerOnlyLocked(boolean z) {
        this.mShowToOwnerOnly = z;
    }

    private boolean isHiddenFromUserLocked() {
        WindowState topParentWindow = getTopParentWindow();
        return (topParentWindow.mAttrs.type >= 2000 || topParentWindow.mAppToken == null || !topParentWindow.mAppToken.mShowForAllUsers || topParentWindow.mFrame.left > topParentWindow.mDisplayFrame.left || topParentWindow.mFrame.top > topParentWindow.mDisplayFrame.top || topParentWindow.mFrame.right < topParentWindow.mStableFrame.right || topParentWindow.mFrame.bottom < topParentWindow.mStableFrame.bottom) && topParentWindow.mShowToOwnerOnly && !this.mService.isCurrentProfileLocked(UserHandle.getUserId(topParentWindow.mOwnerUid));
    }

    private static void applyInsets(Region region, Rect rect, Rect rect2) {
        region.set(rect.left + rect2.left, rect.top + rect2.top, rect.right - rect2.right, rect.bottom - rect2.bottom);
    }

    void getTouchableRegion(Region region) {
        Rect rect = this.mFrame;
        switch (this.mTouchableInsets) {
            case 1:
                applyInsets(region, rect, this.mGivenContentInsets);
                break;
            case 2:
                applyInsets(region, rect, this.mGivenVisibleInsets);
                break;
            case 3:
                region.set(this.mGivenTouchableRegion);
                region.translate(rect.left, rect.top);
                break;
            default:
                region.set(rect);
                break;
        }
        cropRegionToStackBoundsIfNeeded(region);
    }

    private void cropRegionToStackBoundsIfNeeded(Region region) {
        TaskStack taskStack;
        Task task = getTask();
        if (task == null || !task.cropWindowsToStackBounds() || (taskStack = task.mStack) == null) {
            return;
        }
        taskStack.getDimBounds(this.mTmpRect);
        region.op(this.mTmpRect, Region.Op.INTERSECT);
    }

    void reportFocusChangedSerialized(boolean z, boolean z2) {
        try {
            this.mClient.windowFocusChanged(z, z2);
        } catch (RemoteException e) {
        }
        if (this.mFocusCallbacks != null) {
            int iBeginBroadcast = this.mFocusCallbacks.beginBroadcast();
            for (int i = 0; i < iBeginBroadcast; i++) {
                IWindowFocusObserver broadcastItem = this.mFocusCallbacks.getBroadcastItem(i);
                if (z) {
                    try {
                        broadcastItem.focusGained(this.mWindowId.asBinder());
                    } catch (RemoteException e2) {
                    }
                } else {
                    broadcastItem.focusLost(this.mWindowId.asBinder());
                }
            }
            this.mFocusCallbacks.finishBroadcast();
        }
    }

    @Override
    public Configuration getConfiguration() {
        if (this.mAppToken != null && this.mAppToken.mFrozenMergedConfig.size() > 0) {
            return this.mAppToken.mFrozenMergedConfig.peek();
        }
        return super.getConfiguration();
    }

    void reportResized() {
        WindowState windowState;
        Trace.traceBegin(32L, "wm.reportResized_" + ((Object) getWindowTag()));
        try {
            if (WindowManagerDebugConfig.DEBUG_RESIZE || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Reporting new frame to " + this + ": " + this.mCompatFrame);
            }
            final MergedConfiguration mergedConfiguration = new MergedConfiguration(this.mService.mRoot.getConfiguration(), getMergedOverrideConfiguration());
            setLastReportedMergedConfiguration(mergedConfiguration);
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION && this.mWinAnimator.mDrawState == 1) {
                Slog.i("WindowManager", "Resizing " + this + " WITH DRAW PENDING");
            }
            final Rect rect = this.mFrame;
            final Rect rect2 = this.mLastOverscanInsets;
            final Rect rect3 = this.mLastContentInsets;
            final Rect rect4 = this.mLastVisibleInsets;
            final Rect rect5 = this.mLastStableInsets;
            final Rect rect6 = this.mLastOutsets;
            final boolean z = this.mWinAnimator.mDrawState == 1;
            final boolean z2 = this.mReportOrientationChanged;
            final int displayId = getDisplayId();
            final DisplayCutout displayCutout = this.mDisplayCutout.getDisplayCutout();
            if (this.mAttrs.type != 3 && (this.mClient instanceof IWindow.Stub)) {
                try {
                    this.mService.mH.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                WindowState.this.dispatchResized(rect, rect2, rect3, rect4, rect5, rect6, z, mergedConfiguration, z2, displayId, displayCutout);
                            } catch (RemoteException e) {
                            }
                        }
                    });
                    windowState = this;
                } catch (RemoteException e) {
                    windowState = this;
                    windowState.setOrientationChanging(false);
                    windowState.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - windowState.mService.mDisplayFreezeTime);
                    Slog.w("WindowManager", "Failed to report 'resized' to the client of " + windowState + ", removing this window.");
                    windowState.mService.mPendingRemove.add(windowState);
                    windowState.mService.mWindowPlacerLocked.requestTraversal();
                }
            } else {
                windowState = this;
                try {
                    windowState.dispatchResized(rect, rect2, rect3, rect4, rect5, rect6, z, mergedConfiguration, z2, displayId, displayCutout);
                } catch (RemoteException e2) {
                    windowState.setOrientationChanging(false);
                    windowState.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - windowState.mService.mDisplayFreezeTime);
                    Slog.w("WindowManager", "Failed to report 'resized' to the client of " + windowState + ", removing this window.");
                    windowState.mService.mPendingRemove.add(windowState);
                    windowState.mService.mWindowPlacerLocked.requestTraversal();
                }
            }
            if (windowState.mService.mAccessibilityController != null && getDisplayId() == 0) {
                windowState.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
            }
            windowState.mOverscanInsetsChanged = false;
            windowState.mContentInsetsChanged = false;
            windowState.mVisibleInsetsChanged = false;
            windowState.mStableInsetsChanged = false;
            windowState.mOutsetsChanged = false;
            windowState.mFrameSizeChanged = false;
            windowState.mDisplayCutoutChanged = false;
            windowState.mWinAnimator.mSurfaceResized = false;
            windowState.mReportOrientationChanged = false;
        } catch (RemoteException e3) {
            windowState = this;
        }
        Trace.traceEnd(32L);
    }

    Rect getBackdropFrame(Rect rect) {
        boolean z;
        if (isDragResizing() || isDragResizeChanged()) {
            z = true;
        } else {
            z = false;
        }
        if (getWindowConfiguration().useWindowFrameForBackdrop() || !z) {
            return rect;
        }
        DisplayInfo displayInfo = getDisplayInfo();
        this.mTmpRect.set(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
        return this.mTmpRect;
    }

    public int getStackId() {
        TaskStack stack = getStack();
        if (stack == null) {
            return -1;
        }
        return stack.mStackId;
    }

    private void dispatchResized(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, boolean z, MergedConfiguration mergedConfiguration, boolean z2, int i, DisplayCutout displayCutout) throws RemoteException {
        this.mClient.resized(rect, rect2, rect3, rect4, rect5, rect6, z, mergedConfiguration, getBackdropFrame(rect), isDragResizeChanged() || z2, this.mPolicy.isNavBarForcedShownLw(this), i, new DisplayCutout.ParcelableWrapper(displayCutout));
        this.mDragResizingChangeReported = true;
    }

    public void registerFocusObserver(IWindowFocusObserver iWindowFocusObserver) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mFocusCallbacks == null) {
                    this.mFocusCallbacks = new RemoteCallbackList<>();
                }
                this.mFocusCallbacks.register(iWindowFocusObserver);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void unregisterFocusObserver(IWindowFocusObserver iWindowFocusObserver) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mFocusCallbacks != null) {
                    this.mFocusCallbacks.unregister(iWindowFocusObserver);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean isFocused() {
        boolean z;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = this.mService.mCurrentFocus == this;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    @Override
    public boolean isInMultiWindowMode() {
        Task task = getTask();
        return (task == null || task.isFullscreen()) ? false : true;
    }

    private boolean inFullscreenContainer() {
        return this.mAppToken == null || (this.mAppToken.matchParentBounds() && !isInMultiWindowMode());
    }

    boolean isLetterboxedAppWindow() {
        return !(isInMultiWindowMode() || this.mAppToken == null || this.mAppToken.matchParentBounds()) || isLetterboxedForDisplayCutoutLw();
    }

    @Override
    public boolean isLetterboxedForDisplayCutoutLw() {
        if (this.mAppToken != null && this.mParentFrameWasClippedByDisplayCutout && this.mAttrs.layoutInDisplayCutoutMode != 1 && this.mAttrs.isFullscreen()) {
            return !frameCoversEntireAppTokenBounds();
        }
        return false;
    }

    private boolean frameCoversEntireAppTokenBounds() {
        this.mTmpRect.set(this.mAppToken.getBounds());
        this.mTmpRect.intersectUnchecked(this.mFrame);
        return this.mAppToken.getBounds().equals(this.mTmpRect);
    }

    @Override
    public boolean isLetterboxedOverlappingWith(Rect rect) {
        return this.mAppToken != null && this.mAppToken.isLetterboxOverlappingWith(rect);
    }

    boolean isDragResizeChanged() {
        return this.mDragResizing != computeDragResizing();
    }

    @Override
    void setWaitingForDrawnIfResizingChanged() {
        if (isDragResizeChanged()) {
            this.mService.mWaitingForDrawn.add(this);
        }
        super.setWaitingForDrawnIfResizingChanged();
    }

    private boolean isDragResizingChangeReported() {
        return this.mDragResizingChangeReported;
    }

    @Override
    void resetDragResizingChangeReported() {
        this.mDragResizingChangeReported = false;
        super.resetDragResizingChangeReported();
    }

    int getResizeMode() {
        return this.mResizeMode;
    }

    private boolean computeDragResizing() {
        Task task = getTask();
        if (task == null) {
            return false;
        }
        if ((!inSplitScreenWindowingMode() && !inFreeformWindowingMode()) || this.mAttrs.width != -1 || this.mAttrs.height != -1) {
            return false;
        }
        if (task.isDragResizing()) {
            return true;
        }
        return ((!getDisplayContent().mDividerControllerLocked.isResizing() && (this.mAppToken == null || this.mAppToken.mFrozenBounds.isEmpty())) || task.inFreeformWindowingMode() || isGoneForLayoutLw()) ? false : true;
    }

    void setDragResizing() {
        int i;
        boolean zComputeDragResizing = computeDragResizing();
        if (zComputeDragResizing == this.mDragResizing) {
            return;
        }
        this.mDragResizing = zComputeDragResizing;
        Task task = getTask();
        if (task != null && task.isDragResizing()) {
            this.mResizeMode = task.getDragResizeMode();
            return;
        }
        if (this.mDragResizing && getDisplayContent().mDividerControllerLocked.isResizing()) {
            i = 1;
        } else {
            i = 0;
        }
        this.mResizeMode = i;
    }

    boolean isDragResizing() {
        return this.mDragResizing;
    }

    boolean isDockedResizing() {
        if (this.mDragResizing && getResizeMode() == 1) {
            return true;
        }
        return isChildWindow() && getParentWindow().isDockedResizing();
    }

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, z);
        writeIdentifierToProto(protoOutputStream, 1146756268034L);
        protoOutputStream.write(1120986464259L, getDisplayId());
        protoOutputStream.write(1120986464260L, getStackId());
        this.mAttrs.writeToProto(protoOutputStream, 1146756268037L);
        this.mGivenContentInsets.writeToProto(protoOutputStream, 1146756268038L);
        this.mFrame.writeToProto(protoOutputStream, 1146756268039L);
        this.mContainingFrame.writeToProto(protoOutputStream, 1146756268040L);
        this.mParentFrame.writeToProto(protoOutputStream, 1146756268041L);
        this.mContentFrame.writeToProto(protoOutputStream, 1146756268042L);
        this.mContentInsets.writeToProto(protoOutputStream, 1146756268043L);
        this.mAttrs.surfaceInsets.writeToProto(protoOutputStream, 1146756268044L);
        this.mSurfacePosition.writeToProto(protoOutputStream, 1146756268048L);
        this.mWinAnimator.writeToProto(protoOutputStream, 1146756268045L);
        protoOutputStream.write(1133871366158L, this.mAnimatingExit);
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((WindowState) this.mChildren.get(i)).writeToProto(protoOutputStream, 2246267895823L, z);
        }
        protoOutputStream.write(1120986464274L, this.mRequestedWidth);
        protoOutputStream.write(1120986464275L, this.mRequestedHeight);
        protoOutputStream.write(1120986464276L, this.mViewVisibility);
        protoOutputStream.write(1120986464277L, this.mSystemUiVisibility);
        protoOutputStream.write(1133871366166L, this.mHasSurface);
        protoOutputStream.write(1133871366167L, isReadyForDisplay());
        this.mDisplayFrame.writeToProto(protoOutputStream, 1146756268056L);
        this.mOverscanFrame.writeToProto(protoOutputStream, 1146756268057L);
        this.mVisibleFrame.writeToProto(protoOutputStream, 1146756268058L);
        this.mDecorFrame.writeToProto(protoOutputStream, 1146756268059L);
        this.mOutsetFrame.writeToProto(protoOutputStream, 1146756268060L);
        this.mOverscanInsets.writeToProto(protoOutputStream, 1146756268061L);
        this.mVisibleInsets.writeToProto(protoOutputStream, 1146756268062L);
        this.mStableInsets.writeToProto(protoOutputStream, 1146756268063L);
        this.mOutsets.writeToProto(protoOutputStream, 1146756268064L);
        this.mDisplayCutout.getDisplayCutout().writeToProto(protoOutputStream, 1146756268065L);
        protoOutputStream.write(1133871366178L, this.mRemoveOnExit);
        protoOutputStream.write(1133871366179L, this.mDestroying);
        protoOutputStream.write(1133871366180L, this.mRemoved);
        protoOutputStream.write(1133871366181L, isOnScreen());
        protoOutputStream.write(1133871366182L, isVisible());
        protoOutputStream.end(jStart);
    }

    @Override
    public void writeIdentifierToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, System.identityHashCode(this));
        protoOutputStream.write(1120986464258L, UserHandle.getUserId(this.mOwnerUid));
        CharSequence windowTag = getWindowTag();
        if (windowTag != null) {
            protoOutputStream.write(1138166333443L, windowTag.toString());
        }
        protoOutputStream.end(jStart);
    }

    @Override
    void dump(PrintWriter printWriter, String str, boolean z) {
        TaskStack stack = getStack();
        printWriter.print(str);
        printWriter.print("mDisplayId=");
        printWriter.print(getDisplayId());
        if (stack != null) {
            printWriter.print(" stackId=");
            printWriter.print(stack.mStackId);
        }
        printWriter.print(" mSession=");
        printWriter.print(this.mSession);
        printWriter.print(" mClient=");
        printWriter.println(this.mClient.asBinder());
        printWriter.print(str);
        printWriter.print("mOwnerUid=");
        printWriter.print(this.mOwnerUid);
        printWriter.print(" mShowToOwnerOnly=");
        printWriter.print(this.mShowToOwnerOnly);
        printWriter.print(" package=");
        printWriter.print(this.mAttrs.packageName);
        printWriter.print(" appop=");
        printWriter.println(AppOpsManager.opToName(this.mAppOp));
        printWriter.print(str);
        printWriter.print("mAttrs=");
        printWriter.println(this.mAttrs.toString(str));
        printWriter.print(str);
        printWriter.print("Requested w=");
        printWriter.print(this.mRequestedWidth);
        printWriter.print(" h=");
        printWriter.print(this.mRequestedHeight);
        printWriter.print(" mLayoutSeq=");
        printWriter.println(this.mLayoutSeq);
        if (this.mRequestedWidth != this.mLastRequestedWidth || this.mRequestedHeight != this.mLastRequestedHeight) {
            printWriter.print(str);
            printWriter.print("LastRequested w=");
            printWriter.print(this.mLastRequestedWidth);
            printWriter.print(" h=");
            printWriter.println(this.mLastRequestedHeight);
        }
        if (this.mIsChildWindow || this.mLayoutAttached) {
            printWriter.print(str);
            printWriter.print("mParentWindow=");
            printWriter.print(getParentWindow());
            printWriter.print(" mLayoutAttached=");
            printWriter.println(this.mLayoutAttached);
        }
        if (this.mIsImWindow || this.mIsWallpaper || this.mIsFloatingLayer) {
            printWriter.print(str);
            printWriter.print("mIsImWindow=");
            printWriter.print(this.mIsImWindow);
            printWriter.print(" mIsWallpaper=");
            printWriter.print(this.mIsWallpaper);
            printWriter.print(" mIsFloatingLayer=");
            printWriter.print(this.mIsFloatingLayer);
            printWriter.print(" mWallpaperVisible=");
            printWriter.println(this.mWallpaperVisible);
        }
        if (z) {
            printWriter.print(str);
            printWriter.print("mBaseLayer=");
            printWriter.print(this.mBaseLayer);
            printWriter.print(" mSubLayer=");
            printWriter.print(this.mSubLayer);
            printWriter.print(" mAnimLayer=");
            printWriter.print(this.mLayer);
            printWriter.print("+");
            printWriter.print("=");
            printWriter.print(this.mWinAnimator.mAnimLayer);
            printWriter.print(" mLastLayer=");
            printWriter.println(this.mWinAnimator.mLastLayer);
        }
        if (z) {
            printWriter.print(str);
            printWriter.print("mToken=");
            printWriter.println(this.mToken);
            if (this.mAppToken != null) {
                printWriter.print(str);
                printWriter.print("mAppToken=");
                printWriter.println(this.mAppToken);
                printWriter.print(str);
                printWriter.print(" isAnimatingWithSavedSurface()=");
                printWriter.print(" mAppDied=");
                printWriter.print(this.mAppDied);
                printWriter.print(str);
                printWriter.print("drawnStateEvaluated=");
                printWriter.print(getDrawnStateEvaluated());
                printWriter.print(str);
                printWriter.print("mightAffectAllDrawn=");
                printWriter.println(mightAffectAllDrawn());
            }
            printWriter.print(str);
            printWriter.print("mViewVisibility=0x");
            printWriter.print(Integer.toHexString(this.mViewVisibility));
            printWriter.print(" mHaveFrame=");
            printWriter.print(this.mHaveFrame);
            printWriter.print(" mObscured=");
            printWriter.println(this.mObscured);
            printWriter.print(str);
            printWriter.print("mSeq=");
            printWriter.print(this.mSeq);
            printWriter.print(" mSystemUiVisibility=0x");
            printWriter.println(Integer.toHexString(this.mSystemUiVisibility));
        }
        if (!this.mPolicyVisibility || !this.mPolicyVisibilityAfterAnim || !this.mAppOpVisibility || isParentWindowHidden() || this.mPermanentlyHidden || this.mForceHideNonSystemOverlayWindow || this.mHiddenWhileSuspended) {
            printWriter.print(str);
            printWriter.print("mPolicyVisibility=");
            printWriter.print(this.mPolicyVisibility);
            printWriter.print(" mPolicyVisibilityAfterAnim=");
            printWriter.print(this.mPolicyVisibilityAfterAnim);
            printWriter.print(" mAppOpVisibility=");
            printWriter.print(this.mAppOpVisibility);
            printWriter.print(" parentHidden=");
            printWriter.print(isParentWindowHidden());
            printWriter.print(" mPermanentlyHidden=");
            printWriter.print(this.mPermanentlyHidden);
            printWriter.print(" mHiddenWhileSuspended=");
            printWriter.print(this.mHiddenWhileSuspended);
            printWriter.print(" mForceHideNonSystemOverlayWindow=");
            printWriter.println(this.mForceHideNonSystemOverlayWindow);
        }
        if (!this.mRelayoutCalled || this.mLayoutNeeded) {
            printWriter.print(str);
            printWriter.print("mRelayoutCalled=");
            printWriter.print(this.mRelayoutCalled);
            printWriter.print(" mLayoutNeeded=");
            printWriter.println(this.mLayoutNeeded);
        }
        if (z) {
            printWriter.print(str);
            printWriter.print("mGivenContentInsets=");
            this.mGivenContentInsets.printShortString(printWriter);
            printWriter.print(" mGivenVisibleInsets=");
            this.mGivenVisibleInsets.printShortString(printWriter);
            printWriter.println();
            if (this.mTouchableInsets != 0 || this.mGivenInsetsPending) {
                printWriter.print(str);
                printWriter.print("mTouchableInsets=");
                printWriter.print(this.mTouchableInsets);
                printWriter.print(" mGivenInsetsPending=");
                printWriter.println(this.mGivenInsetsPending);
                Region region = new Region();
                getTouchableRegion(region);
                printWriter.print(str);
                printWriter.print("touchable region=");
                printWriter.println(region);
            }
            printWriter.print(str);
            printWriter.print("mFullConfiguration=");
            printWriter.println(getConfiguration());
            printWriter.print(str);
            printWriter.print("mLastReportedConfiguration=");
            printWriter.println(getLastReportedConfiguration());
        }
        printWriter.print(str);
        printWriter.print("mHasSurface=");
        printWriter.print(this.mHasSurface);
        printWriter.print(" isReadyForDisplay()=");
        printWriter.print(isReadyForDisplay());
        printWriter.print(" canReceiveKeys()=");
        printWriter.print(canReceiveKeys());
        printWriter.print(" mWindowRemovalAllowed=");
        printWriter.println(this.mWindowRemovalAllowed);
        if (z) {
            printWriter.print(str);
            printWriter.print("mFrame=");
            this.mFrame.printShortString(printWriter);
            printWriter.print(" last=");
            this.mLastFrame.printShortString(printWriter);
            printWriter.println();
        }
        if (this.mEnforceSizeCompat) {
            printWriter.print(str);
            printWriter.print("mCompatFrame=");
            this.mCompatFrame.printShortString(printWriter);
            printWriter.println();
        }
        if (z) {
            printWriter.print(str);
            printWriter.print("Frames: containing=");
            this.mContainingFrame.printShortString(printWriter);
            printWriter.print(" parent=");
            this.mParentFrame.printShortString(printWriter);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("    display=");
            this.mDisplayFrame.printShortString(printWriter);
            printWriter.print(" overscan=");
            this.mOverscanFrame.printShortString(printWriter);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("    content=");
            this.mContentFrame.printShortString(printWriter);
            printWriter.print(" visible=");
            this.mVisibleFrame.printShortString(printWriter);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("    decor=");
            this.mDecorFrame.printShortString(printWriter);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("    outset=");
            this.mOutsetFrame.printShortString(printWriter);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("Cur insets: overscan=");
            this.mOverscanInsets.printShortString(printWriter);
            printWriter.print(" content=");
            this.mContentInsets.printShortString(printWriter);
            printWriter.print(" visible=");
            this.mVisibleInsets.printShortString(printWriter);
            printWriter.print(" stable=");
            this.mStableInsets.printShortString(printWriter);
            printWriter.print(" surface=");
            this.mAttrs.surfaceInsets.printShortString(printWriter);
            printWriter.print(" outsets=");
            this.mOutsets.printShortString(printWriter);
            printWriter.print(" cutout=" + this.mDisplayCutout.getDisplayCutout());
            printWriter.println();
            printWriter.print(str);
            printWriter.print("Lst insets: overscan=");
            this.mLastOverscanInsets.printShortString(printWriter);
            printWriter.print(" content=");
            this.mLastContentInsets.printShortString(printWriter);
            printWriter.print(" visible=");
            this.mLastVisibleInsets.printShortString(printWriter);
            printWriter.print(" stable=");
            this.mLastStableInsets.printShortString(printWriter);
            printWriter.print(" physical=");
            this.mLastOutsets.printShortString(printWriter);
            printWriter.print(" outset=");
            this.mLastOutsets.printShortString(printWriter);
            printWriter.print(" cutout=" + this.mLastDisplayCutout);
            printWriter.println();
        }
        super.dump(printWriter, str, z);
        printWriter.print(str);
        printWriter.print(this.mWinAnimator);
        printWriter.println(":");
        this.mWinAnimator.dump(printWriter, str + "  ", z);
        if (this.mAnimatingExit || this.mRemoveOnExit || this.mDestroying || this.mRemoved) {
            printWriter.print(str);
            printWriter.print("mAnimatingExit=");
            printWriter.print(this.mAnimatingExit);
            printWriter.print(" mRemoveOnExit=");
            printWriter.print(this.mRemoveOnExit);
            printWriter.print(" mDestroying=");
            printWriter.print(this.mDestroying);
            printWriter.print(" mRemoved=");
            printWriter.println(this.mRemoved);
        }
        if (getOrientationChanging() || this.mAppFreezing || this.mReportOrientationChanged) {
            printWriter.print(str);
            printWriter.print("mOrientationChanging=");
            printWriter.print(this.mOrientationChanging);
            printWriter.print(" configOrientationChanging=");
            printWriter.print(getLastReportedConfiguration().orientation != getConfiguration().orientation);
            printWriter.print(" mAppFreezing=");
            printWriter.print(this.mAppFreezing);
            printWriter.print(" mReportOrientationChanged=");
            printWriter.println(this.mReportOrientationChanged);
        }
        if (this.mLastFreezeDuration != 0) {
            printWriter.print(str);
            printWriter.print("mLastFreezeDuration=");
            TimeUtils.formatDuration(this.mLastFreezeDuration, printWriter);
            printWriter.println();
        }
        if (this.mHScale != 1.0f || this.mVScale != 1.0f) {
            printWriter.print(str);
            printWriter.print("mHScale=");
            printWriter.print(this.mHScale);
            printWriter.print(" mVScale=");
            printWriter.println(this.mVScale);
        }
        if (this.mWallpaperX != -1.0f || this.mWallpaperY != -1.0f) {
            printWriter.print(str);
            printWriter.print("mWallpaperX=");
            printWriter.print(this.mWallpaperX);
            printWriter.print(" mWallpaperY=");
            printWriter.println(this.mWallpaperY);
        }
        if (this.mWallpaperXStep != -1.0f || this.mWallpaperYStep != -1.0f) {
            printWriter.print(str);
            printWriter.print("mWallpaperXStep=");
            printWriter.print(this.mWallpaperXStep);
            printWriter.print(" mWallpaperYStep=");
            printWriter.println(this.mWallpaperYStep);
        }
        if (this.mWallpaperDisplayOffsetX != Integer.MIN_VALUE || this.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            printWriter.print(str);
            printWriter.print("mWallpaperDisplayOffsetX=");
            printWriter.print(this.mWallpaperDisplayOffsetX);
            printWriter.print(" mWallpaperDisplayOffsetY=");
            printWriter.println(this.mWallpaperDisplayOffsetY);
        }
        if (this.mDrawLock != null) {
            printWriter.print(str);
            printWriter.println("mDrawLock=" + this.mDrawLock);
        }
        if (isDragResizing()) {
            printWriter.print(str);
            printWriter.println("isDragResizing=" + isDragResizing());
        }
        if (computeDragResizing()) {
            printWriter.print(str);
            printWriter.println("computeDragResizing=" + computeDragResizing());
        }
        printWriter.print(str);
        printWriter.println("isOnScreen=" + isOnScreen());
        printWriter.print(str);
        printWriter.println("isVisible=" + isVisible());
    }

    @Override
    String getName() {
        return Integer.toHexString(System.identityHashCode(this)) + " " + ((Object) getWindowTag());
    }

    CharSequence getWindowTag() {
        CharSequence title = this.mAttrs.getTitle();
        if (title == null || title.length() <= 0) {
            return this.mAttrs.packageName;
        }
        return title;
    }

    public String toString() {
        CharSequence windowTag = getWindowTag();
        if (this.mStringNameCache == null || this.mLastTitle != windowTag || this.mWasExiting != this.mAnimatingExit) {
            this.mLastTitle = windowTag;
            this.mWasExiting = this.mAnimatingExit;
            StringBuilder sb = new StringBuilder();
            sb.append("Window{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" u");
            sb.append(UserHandle.getUserId(this.mOwnerUid));
            sb.append(" ");
            sb.append((Object) this.mLastTitle);
            sb.append(this.mAnimatingExit ? " EXITING}" : "}");
            this.mStringNameCache = sb.toString();
        }
        return this.mStringNameCache;
    }

    void transformClipRectFromScreenToSurfaceSpace(Rect rect) {
        if (this.mHScale >= 0.0f) {
            rect.left = (int) (rect.left / this.mHScale);
            rect.right = (int) Math.ceil(rect.right / this.mHScale);
        }
        if (this.mVScale >= 0.0f) {
            rect.top = (int) (rect.top / this.mVScale);
            rect.bottom = (int) Math.ceil(rect.bottom / this.mVScale);
        }
    }

    void applyGravityAndUpdateFrame(Rect rect, Rect rect2) {
        int iMin;
        int iMin2;
        float f;
        float f2;
        int iWidth = rect.width();
        int iHeight = rect.height();
        Task task = getTask();
        boolean z = true;
        boolean z2 = !inFullscreenContainer();
        boolean z3 = (this.mAttrs.flags & 512) != 0;
        if (task != null && z2 && (this.mAttrs.type == 1 || z3)) {
            z = false;
        }
        if ((this.mAttrs.flags & 16384) != 0) {
            if (this.mAttrs.width >= 0) {
                if (this.mEnforceSizeCompat) {
                    iMin = (int) ((this.mAttrs.width * this.mGlobalScale) + 0.5f);
                } else {
                    iMin = this.mAttrs.width;
                }
            } else {
                iMin = iWidth;
            }
            if (this.mAttrs.height >= 0) {
                if (this.mEnforceSizeCompat) {
                    iMin2 = (int) ((this.mAttrs.height * this.mGlobalScale) + 0.5f);
                } else {
                    iMin2 = this.mAttrs.height;
                }
            } else {
                iMin2 = iHeight;
            }
        } else {
            if (this.mAttrs.width != -1) {
                if (this.mEnforceSizeCompat) {
                    iMin = (int) ((this.mRequestedWidth * this.mGlobalScale) + 0.5f);
                } else {
                    iMin = this.mRequestedWidth;
                }
            } else {
                iMin = iWidth;
            }
            if (this.mAttrs.height != -1) {
                if (this.mEnforceSizeCompat) {
                    iMin2 = (int) ((this.mRequestedHeight * this.mGlobalScale) + 0.5f);
                } else {
                    iMin2 = this.mRequestedHeight;
                }
            }
        }
        if (this.mEnforceSizeCompat) {
            f = this.mAttrs.x * this.mGlobalScale;
            f2 = this.mAttrs.y * this.mGlobalScale;
        } else {
            f = this.mAttrs.x;
            f2 = this.mAttrs.y;
        }
        if (z2 && !layoutInParentFrame()) {
            iMin = Math.min(iMin, iWidth);
            iMin2 = Math.min(iMin2, iHeight);
        }
        Gravity.apply(this.mAttrs.gravity, iMin, iMin2, rect, (int) (f + (this.mAttrs.horizontalMargin * iWidth)), (int) (f2 + (this.mAttrs.verticalMargin * iHeight)), this.mFrame);
        if (z) {
            Gravity.applyDisplay(this.mAttrs.gravity, rect2, this.mFrame);
        }
        this.mCompatFrame.set(this.mFrame);
        if (this.mEnforceSizeCompat) {
            this.mCompatFrame.scale(this.mInvGlobalScale);
        }
    }

    boolean isChildWindow() {
        return this.mIsChildWindow;
    }

    boolean layoutInParentFrame() {
        return this.mIsChildWindow && (this.mAttrs.privateFlags & 65536) != 0;
    }

    boolean hideNonSystemOverlayWindowsWhenVisible() {
        return (this.mAttrs.privateFlags & DumpState.DUMP_FROZEN) != 0 && this.mSession.mCanHideNonSystemOverlayWindows;
    }

    WindowState getParentWindow() {
        if (this.mIsChildWindow) {
            return (WindowState) super.getParent();
        }
        return null;
    }

    WindowState getTopParentWindow() {
        WindowState windowState;
        WindowState parentWindow = this;
        loop0: while (true) {
            windowState = parentWindow;
            while (parentWindow != null && parentWindow.mIsChildWindow) {
                parentWindow = parentWindow.getParentWindow();
                if (parentWindow != null) {
                    break;
                }
            }
        }
        return windowState;
    }

    boolean isParentWindowHidden() {
        WindowState parentWindow = getParentWindow();
        return parentWindow != null && parentWindow.mHidden;
    }

    void setWillReplaceWindow(boolean z) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).setWillReplaceWindow(z);
        }
        if ((this.mAttrs.privateFlags & 32768) != 0 || this.mAttrs.type == 3) {
            return;
        }
        this.mWillReplaceWindow = true;
        this.mReplacementWindow = null;
        this.mAnimateReplacingWindow = z;
    }

    void clearWillReplaceWindow() {
        this.mWillReplaceWindow = false;
        this.mReplacementWindow = null;
        this.mAnimateReplacingWindow = false;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).clearWillReplaceWindow();
        }
    }

    boolean waitingForReplacement() {
        if (this.mWillReplaceWindow) {
            return true;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (((WindowState) this.mChildren.get(size)).waitingForReplacement()) {
                return true;
            }
        }
        return false;
    }

    void requestUpdateWallpaperIfNeeded() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent != null && (this.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
            displayContent.pendingLayoutChanges |= 4;
            displayContent.setLayoutNeeded();
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).requestUpdateWallpaperIfNeeded();
        }
    }

    float translateToWindowX(float f) {
        float f2 = f - this.mFrame.left;
        if (this.mEnforceSizeCompat) {
            return f2 * this.mGlobalScale;
        }
        return f2;
    }

    float translateToWindowY(float f) {
        float f2 = f - this.mFrame.top;
        if (this.mEnforceSizeCompat) {
            return f2 * this.mGlobalScale;
        }
        return f2;
    }

    boolean shouldBeReplacedWithChildren() {
        return this.mIsChildWindow || this.mAttrs.type == 2 || this.mAttrs.type == 4;
    }

    void setWillReplaceChildWindows() {
        if (shouldBeReplacedWithChildren()) {
            setWillReplaceWindow(false);
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).setWillReplaceChildWindows();
        }
    }

    WindowState getReplacingWindow() {
        if (this.mAnimatingExit && this.mWillReplaceWindow && this.mAnimateReplacingWindow) {
            return this;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState replacingWindow = ((WindowState) this.mChildren.get(size)).getReplacingWindow();
            if (replacingWindow != null) {
                return replacingWindow;
            }
        }
        return null;
    }

    @Override
    public int getRotationAnimationHint() {
        if (this.mAppToken != null) {
            return this.mAppToken.mRotationAnimationHint;
        }
        return -1;
    }

    @Override
    public boolean isInputMethodWindow() {
        return this.mIsImWindow;
    }

    boolean performShowLocked() {
        if (isHiddenFromUserLocked()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.w("WindowManager", "hiding " + this + ", belonging to " + this.mOwnerUid);
            }
            hideLw(false);
            return false;
        }
        logPerformShow("performShow on ");
        int i = this.mWinAnimator.mDrawState;
        if ((i == 4 || i == 3) && this.mAttrs.type != 3 && this.mAppToken != null) {
            this.mAppToken.onFirstWindowDrawn(this, this.mWinAnimator);
        }
        if (this.mWinAnimator.mDrawState != 3 || !isReadyForDisplay()) {
            return false;
        }
        logPerformShow("Showing ");
        this.mService.enableScreenIfNeededLocked();
        this.mWinAnimator.applyEnterAnimationLocked();
        this.mWinAnimator.mLastAlpha = -1.0f;
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v("WindowManager", "performShowLocked: mDrawState=HAS_DRAWN in " + this);
        }
        this.mWinAnimator.mDrawState = 4;
        this.mService.scheduleAnimationLocked();
        if (this.mHidden) {
            this.mHidden = false;
            DisplayContent displayContent = getDisplayContent();
            for (int size = this.mChildren.size() - 1; size >= 0; size--) {
                WindowState windowState = (WindowState) this.mChildren.get(size);
                if (windowState.mWinAnimator.mSurfaceController != null) {
                    windowState.performShowLocked();
                    if (displayContent != null) {
                        displayContent.setLayoutNeeded();
                    }
                }
            }
        }
        if (this.mAttrs.type == 2011) {
            getDisplayContent().mDividerControllerLocked.resetImeHideRequested();
        }
        return true;
    }

    private void logPerformShow(String str) {
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY || (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && this.mAttrs.type == 3)) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(this);
            sb.append(": mDrawState=");
            sb.append(this.mWinAnimator.drawStateToString());
            sb.append(" readyForDisplay=");
            sb.append(isReadyForDisplay());
            sb.append(" starting=");
            boolean z = false;
            sb.append(this.mAttrs.type == 3);
            sb.append(" during animation: policyVis=");
            sb.append(this.mPolicyVisibility);
            sb.append(" parentHidden=");
            sb.append(isParentWindowHidden());
            sb.append(" tok.hiddenRequested=");
            sb.append(this.mAppToken != null && this.mAppToken.hiddenRequested);
            sb.append(" tok.hidden=");
            sb.append(this.mAppToken != null && this.mAppToken.isHidden());
            sb.append(" animationSet=");
            sb.append(this.mWinAnimator.isAnimationSet());
            sb.append(" tok animating=");
            if (this.mAppToken != null && this.mAppToken.isSelfAnimating()) {
                z = true;
            }
            sb.append(z);
            sb.append(" Callers=");
            sb.append(Debug.getCallers(4));
            Slog.v("WindowManager", sb.toString());
        }
    }

    WindowInfo getWindowInfo() {
        WindowInfo windowInfoObtain = WindowInfo.obtain();
        windowInfoObtain.type = this.mAttrs.type;
        windowInfoObtain.layer = this.mLayer;
        windowInfoObtain.token = this.mClient.asBinder();
        if (this.mAppToken != null) {
            windowInfoObtain.activityToken = this.mAppToken.appToken.asBinder();
        }
        windowInfoObtain.title = this.mAttrs.accessibilityTitle;
        boolean z = this.mAttrs.type >= 1000 && this.mAttrs.type <= 1999;
        boolean z2 = windowInfoObtain.type == 2032;
        if (TextUtils.isEmpty(windowInfoObtain.title) && (z || z2)) {
            windowInfoObtain.title = this.mAttrs.getTitle();
        }
        windowInfoObtain.accessibilityIdOfAnchor = this.mAttrs.accessibilityIdOfAnchor;
        windowInfoObtain.focused = isFocused();
        Task task = getTask();
        windowInfoObtain.inPictureInPicture = task != null && task.inPinnedWindowingMode();
        if (this.mIsChildWindow) {
            windowInfoObtain.parentToken = getParentWindow().mClient.asBinder();
        }
        int size = this.mChildren.size();
        if (size > 0) {
            if (windowInfoObtain.childTokens == null) {
                windowInfoObtain.childTokens = new ArrayList(size);
            }
            for (int i = 0; i < size; i++) {
                windowInfoObtain.childTokens.add(((WindowState) this.mChildren.get(i)).mClient.asBinder());
            }
        }
        return windowInfoObtain;
    }

    int getHighestAnimLayer() {
        int i = this.mWinAnimator.mAnimLayer;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            int highestAnimLayer = ((WindowState) this.mChildren.get(size)).getHighestAnimLayer();
            if (highestAnimLayer > i) {
                i = highestAnimLayer;
            }
        }
        return i;
    }

    @Override
    boolean forAllWindows(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
        if (this.mChildren.isEmpty()) {
            return applyInOrderWithImeWindows(toBooleanFunction, z);
        }
        if (z) {
            return forAllWindowTopToBottom(toBooleanFunction);
        }
        return forAllWindowBottomToTop(toBooleanFunction);
    }

    private boolean forAllWindowBottomToTop(ToBooleanFunction<WindowState> toBooleanFunction) {
        int size = this.mChildren.size();
        WindowState windowState = (WindowState) this.mChildren.get(0);
        int i = 0;
        while (i < size && windowState.mSubLayer < 0) {
            if (windowState.applyInOrderWithImeWindows(toBooleanFunction, false)) {
                return true;
            }
            i++;
            if (i >= size) {
                break;
            }
            windowState = (WindowState) this.mChildren.get(i);
        }
        if (applyInOrderWithImeWindows(toBooleanFunction, false)) {
            return true;
        }
        while (i < size) {
            if (windowState.applyInOrderWithImeWindows(toBooleanFunction, false)) {
                return true;
            }
            i++;
            if (i >= size) {
                break;
            }
            windowState = (WindowState) this.mChildren.get(i);
        }
        return false;
    }

    private boolean forAllWindowTopToBottom(ToBooleanFunction<WindowState> toBooleanFunction) {
        int size = this.mChildren.size() - 1;
        WindowState windowState = (WindowState) this.mChildren.get(size);
        while (size >= 0 && windowState.mSubLayer >= 0) {
            if (windowState.applyInOrderWithImeWindows(toBooleanFunction, true)) {
                return true;
            }
            size--;
            if (size < 0) {
                break;
            }
            windowState = (WindowState) this.mChildren.get(size);
        }
        if (applyInOrderWithImeWindows(toBooleanFunction, true)) {
            return true;
        }
        while (size >= 0) {
            if (windowState.applyInOrderWithImeWindows(toBooleanFunction, true)) {
                return true;
            }
            size--;
            if (size >= 0) {
                windowState = (WindowState) this.mChildren.get(size);
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean applyImeWindowsIfNeeded(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
        if (isInputMethodTarget() && !inSplitScreenWindowingMode() && getDisplayContent().forAllImeWindows(toBooleanFunction, z)) {
            return true;
        }
        return false;
    }

    private boolean applyInOrderWithImeWindows(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
        if (z) {
            if (applyImeWindowsIfNeeded(toBooleanFunction, z) || toBooleanFunction.apply(this)) {
                return true;
            }
            return false;
        }
        if (toBooleanFunction.apply(this) || applyImeWindowsIfNeeded(toBooleanFunction, z)) {
            return true;
        }
        return false;
    }

    @Override
    WindowState getWindow(Predicate<WindowState> predicate) {
        if (this.mChildren.isEmpty()) {
            if (predicate.test(this)) {
                return this;
            }
            return null;
        }
        int size = this.mChildren.size() - 1;
        WindowState windowState = (WindowState) this.mChildren.get(size);
        while (size >= 0 && windowState.mSubLayer >= 0) {
            if (predicate.test(windowState)) {
                return windowState;
            }
            size--;
            if (size < 0) {
                break;
            }
            windowState = (WindowState) this.mChildren.get(size);
        }
        if (predicate.test(this)) {
            return this;
        }
        while (size >= 0) {
            if (predicate.test(windowState)) {
                return windowState;
            }
            size--;
            if (size < 0) {
                break;
            }
            windowState = (WindowState) this.mChildren.get(size);
        }
        return null;
    }

    @VisibleForTesting
    boolean isSelfOrAncestorWindowAnimatingExit() {
        WindowState parentWindow = this;
        while (!parentWindow.mAnimatingExit) {
            parentWindow = parentWindow.getParentWindow();
            if (parentWindow == null) {
                return false;
            }
        }
        return true;
    }

    void onExitAnimationDone() {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v("WindowManager", "onExitAnimationDone in " + this + ": exiting=" + this.mAnimatingExit + " remove=" + this.mRemoveOnExit + " selfAnimating=" + isSelfAnimating());
        }
        if (!this.mChildren.isEmpty()) {
            ArrayList arrayList = new ArrayList(this.mChildren);
            for (int size = arrayList.size() - 1; size >= 0; size--) {
                ((WindowState) arrayList.get(size)).onExitAnimationDone();
            }
        }
        if (this.mWinAnimator.mEnteringAnimation) {
            this.mWinAnimator.mEnteringAnimation = false;
            this.mService.requestTraversal();
            if (this.mAppToken == null) {
                try {
                    this.mClient.dispatchWindowShown();
                } catch (RemoteException e) {
                }
            }
        }
        if (isSelfAnimating()) {
            return;
        }
        if (this.mService.mAccessibilityController != null && getDisplayId() == 0) {
            this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
        }
        if (!isSelfOrAncestorWindowAnimatingExit()) {
            return;
        }
        if (WindowManagerService.localLOGV || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "Exit animation finished in " + this + ": remove=" + this.mRemoveOnExit);
        }
        this.mDestroying = true;
        boolean zHasSurface = this.mWinAnimator.hasSurface();
        this.mWinAnimator.hide(getPendingTransaction(), "onExitAnimationDone");
        if (this.mAppToken != null) {
            this.mAppToken.destroySurfaces();
        } else {
            if (zHasSurface) {
                this.mService.mDestroySurface.add(this);
            }
            if (this.mRemoveOnExit) {
                this.mService.mPendingRemove.add(this);
                this.mRemoveOnExit = false;
            }
        }
        this.mAnimatingExit = false;
        getDisplayContent().mWallpaperController.hideWallpapers(this);
    }

    boolean clearAnimatingFlags() {
        boolean zClearAnimatingFlags;
        if (this.mWillReplaceWindow || this.mRemoveOnExit) {
            zClearAnimatingFlags = false;
        } else {
            if (this.mAnimatingExit) {
                this.mAnimatingExit = false;
                zClearAnimatingFlags = true;
            } else {
                zClearAnimatingFlags = false;
            }
            if (this.mDestroying) {
                this.mDestroying = false;
                this.mService.mDestroySurface.remove(this);
                zClearAnimatingFlags = true;
            }
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            zClearAnimatingFlags |= ((WindowState) this.mChildren.get(size)).clearAnimatingFlags();
        }
        return zClearAnimatingFlags;
    }

    public boolean isRtl() {
        return getConfiguration().getLayoutDirection() == 1;
    }

    void hideWallpaperWindow(boolean z, String str) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).hideWallpaperWindow(z, str);
        }
        if (!this.mWinAnimator.mLastHidden || z) {
            this.mWinAnimator.hide(str);
            dispatchWallpaperVisibility(false);
            DisplayContent displayContent = getDisplayContent();
            if (displayContent != null) {
                displayContent.pendingLayoutChanges |= 4;
            }
        }
    }

    void dispatchWallpaperVisibility(boolean z) {
        boolean z2 = getDisplayContent().mWallpaperController.mDeferredHideWallpaper == null;
        if (this.mWallpaperVisible != z) {
            if (z2 || z) {
                this.mWallpaperVisible = z;
                try {
                    if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                        Slog.v("WindowManager", "Updating vis of wallpaper " + this + ": " + z + " from:\n" + Debug.getCallers(4, "  "));
                    }
                    this.mClient.dispatchAppVisibility(z);
                } catch (RemoteException e) {
                }
            }
        }
    }

    boolean hasVisibleNotDrawnWallpaper() {
        if (this.mWallpaperVisible && !isDrawnLw()) {
            return true;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (((WindowState) this.mChildren.get(size)).hasVisibleNotDrawnWallpaper()) {
                return true;
            }
        }
        return false;
    }

    void updateReportedVisibility(UpdateReportedVisibilityResults updateReportedVisibilityResults) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).updateReportedVisibility(updateReportedVisibilityResults);
        }
        if (this.mAppFreezing || this.mViewVisibility != 0 || this.mAttrs.type == 3 || this.mDestroying) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Win " + this + ": isDrawn=" + isDrawnLw() + ", isAnimationSet=" + this.mWinAnimator.isAnimationSet());
            if (!isDrawnLw()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Not displayed: s=");
                sb.append(this.mWinAnimator.mSurfaceController);
                sb.append(" pv=");
                sb.append(this.mPolicyVisibility);
                sb.append(" mDrawState=");
                sb.append(this.mWinAnimator.mDrawState);
                sb.append(" ph=");
                sb.append(isParentWindowHidden());
                sb.append(" th=");
                sb.append(this.mAppToken != null ? this.mAppToken.hiddenRequested : false);
                sb.append(" a=");
                sb.append(this.mWinAnimator.isAnimationSet());
                Slog.v("WindowManager", sb.toString());
            }
        }
        updateReportedVisibilityResults.numInteresting++;
        if (isDrawnLw()) {
            updateReportedVisibilityResults.numDrawn++;
            if (!this.mWinAnimator.isAnimationSet()) {
                updateReportedVisibilityResults.numVisible++;
            }
            updateReportedVisibilityResults.nowGone = false;
            return;
        }
        if (this.mWinAnimator.isAnimationSet()) {
            updateReportedVisibilityResults.nowGone = false;
        }
    }

    private boolean skipDecorCrop() {
        if (this.mDecorFrame.isEmpty()) {
            return true;
        }
        if (this.mAppToken != null) {
            return false;
        }
        return this.mToken.canLayerAboveSystemBars();
    }

    void calculatePolicyCrop(Rect rect) {
        DisplayInfo displayInfo = getDisplayContent().getDisplayInfo();
        if (!isDefaultDisplay()) {
            rect.set(0, 0, this.mCompatFrame.width(), this.mCompatFrame.height());
            rect.intersect(-this.mCompatFrame.left, -this.mCompatFrame.top, displayInfo.logicalWidth - this.mCompatFrame.left, displayInfo.logicalHeight - this.mCompatFrame.top);
        } else if (skipDecorCrop()) {
            rect.set(0, 0, this.mCompatFrame.width(), this.mCompatFrame.height());
        } else {
            calculateSystemDecorRect(rect);
        }
    }

    private void calculateSystemDecorRect(Rect rect) {
        Rect rect2 = this.mDecorFrame;
        int iWidth = this.mFrame.width();
        int iHeight = this.mFrame.height();
        int i = this.mFrame.left;
        int i2 = this.mFrame.top;
        boolean z = false;
        if (isDockedResizing()) {
            DisplayInfo displayInfo = getDisplayContent().getDisplayInfo();
            rect.set(0, 0, Math.max(iWidth, displayInfo.logicalWidth), Math.max(iHeight, displayInfo.logicalHeight));
        } else {
            rect.set(0, 0, iWidth, iHeight);
        }
        if ((!inFreeformWindowingMode() || !isAnimatingLw()) && !isDockedResizing()) {
            z = true;
        }
        if (z) {
            rect.intersect(rect2.left - i, rect2.top - i2, rect2.right - i, rect2.bottom - i2);
        }
        if (this.mEnforceSizeCompat && this.mInvGlobalScale != 1.0f) {
            float f = this.mInvGlobalScale;
            rect.left = (int) ((rect.left * f) - 0.5f);
            rect.top = (int) ((rect.top * f) - 0.5f);
            rect.right = (int) (((rect.right + 1) * f) - 0.5f);
            rect.bottom = (int) (((rect.bottom + 1) * f) - 0.5f);
        }
    }

    void expandForSurfaceInsets(Rect rect) {
        rect.inset(-this.mAttrs.surfaceInsets.left, -this.mAttrs.surfaceInsets.top, -this.mAttrs.surfaceInsets.right, -this.mAttrs.surfaceInsets.bottom);
    }

    boolean surfaceInsetsChanging() {
        return !this.mLastSurfaceInsets.equals(this.mAttrs.surfaceInsets);
    }

    int relayoutVisibleWindow(int i, int i2, int i3) {
        boolean zIsVisibleLw = isVisibleLw();
        int i4 = 0;
        int i5 = i | ((zIsVisibleLw && isDrawnLw()) ? 0 : 2);
        if (this.mAnimatingExit) {
            Slog.d("WindowManager", "relayoutVisibleWindow: " + this + " mAnimatingExit=true, mRemoveOnExit=" + this.mRemoveOnExit + ", mDestroying=" + this.mDestroying);
            this.mWinAnimator.cancelExitAnimationForNextAnimationLocked();
            this.mAnimatingExit = false;
        }
        if (this.mDestroying) {
            this.mDestroying = false;
            this.mService.mDestroySurface.remove(this);
        }
        if (i3 == 8) {
            this.mWinAnimator.mEnterAnimationPending = true;
        }
        this.mLastVisibleLayoutRotation = getDisplayContent().getRotation();
        this.mWinAnimator.mEnteringAnimation = true;
        prepareWindowToDisplayDuringRelayout(zIsVisibleLw);
        if ((i2 & 8) != 0 && !this.mWinAnimator.tryChangeFormatInPlaceLocked()) {
            this.mWinAnimator.preserveSurfaceLocked();
            i5 |= 6;
        }
        if (isDragResizeChanged()) {
            setDragResizing();
            if (this.mHasSurface && !isChildWindow()) {
                this.mWinAnimator.preserveSurfaceLocked();
                i5 |= 6;
            }
        }
        boolean z = isDragResizing() && getResizeMode() == 0;
        boolean z2 = isDragResizing() && getResizeMode() == 1;
        int i6 = i5 | (z ? 16 : 0);
        if (z2) {
            i4 = 8;
        }
        return i6 | i4;
    }

    boolean isLaidOut() {
        return this.mLayoutSeq != -1;
    }

    void updateLastInsetValues() {
        this.mLastOverscanInsets.set(this.mOverscanInsets);
        this.mLastContentInsets.set(this.mContentInsets);
        this.mLastVisibleInsets.set(this.mVisibleInsets);
        this.mLastStableInsets.set(this.mStableInsets);
        this.mLastOutsets.set(this.mOutsets);
        this.mLastDisplayCutout = this.mDisplayCutout;
    }

    void startAnimation(Animation animation) {
        DisplayInfo displayInfo = getDisplayContent().getDisplayInfo();
        animation.initialize(this.mFrame.width(), this.mFrame.height(), displayInfo.appWidth, displayInfo.appHeight);
        animation.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        animation.scaleCurrentDuration(this.mService.getWindowAnimationScaleLocked());
        startAnimation(this.mPendingTransaction, new LocalAnimationAdapter(new WindowAnimationSpec(animation, this.mSurfacePosition, false), this.mService.mSurfaceAnimationRunner));
        commitPendingTransaction();
    }

    private void startMoveAnimation(int i, int i2) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v("WindowManager", "Setting move animation on " + this);
        }
        Point point = new Point();
        Point point2 = new Point();
        transformFrameToSurfacePosition(this.mLastFrame.left, this.mLastFrame.top, point);
        transformFrameToSurfacePosition(i, i2, point2);
        startAnimation(getPendingTransaction(), new LocalAnimationAdapter(new MoveAnimationSpec(point.x, point.y, point2.x, point2.y), this.mService.mSurfaceAnimationRunner));
    }

    private void startAnimation(SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter) {
        startAnimation(transaction, animationAdapter, this.mWinAnimator.mLastHidden);
    }

    @Override
    protected void onAnimationFinished() {
        this.mWinAnimator.onAnimationFinished();
    }

    void getTransformationMatrix(float[] fArr, Matrix matrix) {
        fArr[0] = this.mWinAnimator.mDsDx;
        fArr[3] = this.mWinAnimator.mDtDx;
        fArr[1] = this.mWinAnimator.mDtDy;
        fArr[4] = this.mWinAnimator.mDsDy;
        int i = this.mSurfacePosition.x;
        int i2 = this.mSurfacePosition.y;
        WindowContainer parent = getParent();
        if (isChildWindow()) {
            WindowState parentWindow = getParentWindow();
            i += parentWindow.mFrame.left - parentWindow.mAttrs.surfaceInsets.left;
            i2 += parentWindow.mFrame.top - parentWindow.mAttrs.surfaceInsets.top;
        } else if (parent != null) {
            Rect bounds = parent.getBounds();
            i += bounds.left;
            i2 += bounds.top;
        }
        fArr[2] = i;
        fArr[5] = i2;
        fArr[6] = 0.0f;
        fArr[7] = 0.0f;
        fArr[8] = 1.0f;
        matrix.setValues(fArr);
    }

    static final class UpdateReportedVisibilityResults {
        boolean nowGone = true;
        int numDrawn;
        int numInteresting;
        int numVisible;

        UpdateReportedVisibilityResults() {
        }

        void reset() {
            this.numInteresting = 0;
            this.numVisible = 0;
            this.numDrawn = 0;
            this.nowGone = true;
        }
    }

    private static final class WindowId extends IWindowId.Stub {
        private final WeakReference<WindowState> mOuter;

        private WindowId(WindowState windowState) {
            this.mOuter = new WeakReference<>(windowState);
        }

        public void registerFocusObserver(IWindowFocusObserver iWindowFocusObserver) {
            WindowState windowState = this.mOuter.get();
            if (windowState != null) {
                windowState.registerFocusObserver(iWindowFocusObserver);
            }
        }

        public void unregisterFocusObserver(IWindowFocusObserver iWindowFocusObserver) {
            WindowState windowState = this.mOuter.get();
            if (windowState != null) {
                windowState.unregisterFocusObserver(iWindowFocusObserver);
            }
        }

        public boolean isFocused() {
            WindowState windowState = this.mOuter.get();
            return windowState != null && windowState.isFocused();
        }
    }

    @Override
    boolean shouldMagnify() {
        if (this.mAttrs.type == 2011 || this.mAttrs.type == 2012 || this.mAttrs.type == 2027 || this.mAttrs.type == 2019 || this.mAttrs.type == 2024) {
            return false;
        }
        return true;
    }

    @Override
    SurfaceSession getSession() {
        if (this.mSession.mSurfaceSession != null) {
            return this.mSession.mSurfaceSession;
        }
        return getParent().getSession();
    }

    @Override
    boolean needsZBoost() {
        AppWindowToken appWindowToken;
        if (this.mIsImWindow && this.mService.mInputMethodTarget != null && (appWindowToken = this.mService.mInputMethodTarget.mAppToken) != null) {
            return appWindowToken.needsZBoost();
        }
        return this.mWillReplaceWindow;
    }

    private void applyDims(Dimmer dimmer) {
        if (!this.mAnimatingExit && this.mAppDied) {
            this.mIsDimming = true;
            dimmer.dimAbove(getPendingTransaction(), this, 0.5f);
        } else if ((this.mAttrs.flags & 2) != 0 && isVisibleNow() && !this.mHidden) {
            this.mIsDimming = true;
            dimmer.dimBelow(getPendingTransaction(), this, this.mAttrs.dimAmount);
        }
    }

    @Override
    void prepareSurfaces() {
        Dimmer dimmer = getDimmer();
        this.mIsDimming = false;
        if (dimmer != null) {
            applyDims(dimmer);
        }
        updateSurfacePosition();
        this.mWinAnimator.prepareSurfaceLocked(true);
        super.prepareSurfaces();
    }

    @Override
    public void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        super.onAnimationLeashCreated(transaction, surfaceControl);
        transaction.setPosition(this.mSurfaceControl, 0.0f, 0.0f);
        this.mLastSurfacePosition.set(0, 0);
    }

    @Override
    public void onAnimationLeashDestroyed(SurfaceControl.Transaction transaction) {
        super.onAnimationLeashDestroyed(transaction);
        updateSurfacePosition(transaction);
    }

    @Override
    void updateSurfacePosition() {
        updateSurfacePosition(getPendingTransaction());
    }

    private void updateSurfacePosition(SurfaceControl.Transaction transaction) {
        if (this.mSurfaceControl == null) {
            return;
        }
        transformFrameToSurfacePosition(this.mFrame.left, this.mFrame.top, this.mSurfacePosition);
        if (!this.mSurfaceAnimator.hasLeash() && !this.mLastSurfacePosition.equals(this.mSurfacePosition)) {
            transaction.setPosition(this.mSurfaceControl, this.mSurfacePosition.x, this.mSurfacePosition.y);
            this.mLastSurfacePosition.set(this.mSurfacePosition.x, this.mSurfacePosition.y);
            if (surfaceInsetsChanging() && this.mWinAnimator.hasSurface()) {
                this.mLastSurfaceInsets.set(this.mAttrs.surfaceInsets);
                transaction.deferTransactionUntil(this.mSurfaceControl, this.mWinAnimator.mSurfaceController.mSurfaceControl.getHandle(), getFrameNumber());
            }
        }
    }

    private void transformFrameToSurfacePosition(int i, int i2, Point point) {
        point.set(i, i2);
        WindowContainer parent = getParent();
        if (isChildWindow()) {
            WindowState parentWindow = getParentWindow();
            point.offset((-parentWindow.mFrame.left) + parentWindow.mAttrs.surfaceInsets.left, (-parentWindow.mFrame.top) + parentWindow.mAttrs.surfaceInsets.top);
        } else if (parent != null) {
            Rect bounds = parent.getBounds();
            point.offset(-bounds.left, -bounds.top);
        }
        TaskStack stack = getStack();
        if (stack != null) {
            int stackOutset = stack.getStackOutset();
            point.offset(stackOutset, stackOutset);
        }
        point.offset(-this.mAttrs.surfaceInsets.left, -this.mAttrs.surfaceInsets.top);
    }

    boolean needsRelativeLayeringToIme() {
        WindowState windowState;
        if (!inSplitScreenWindowingMode()) {
            return false;
        }
        if (!isChildWindow()) {
            return (this.mAppToken == null || (windowState = this.mService.mInputMethodTarget) == null || windowState == this || windowState.mToken != this.mToken || windowState.compareTo((WindowContainer) this) > 0) ? false : true;
        }
        if (getParentWindow().isInputMethodTarget()) {
            return true;
        }
        return false;
    }

    @Override
    void assignLayer(SurfaceControl.Transaction transaction, int i) {
        if (needsRelativeLayeringToIme()) {
            getDisplayContent().assignRelativeLayerForImeTargetChild(transaction, this);
        } else {
            super.assignLayer(transaction, i);
        }
    }

    @Override
    public boolean isDimming() {
        return this.mIsDimming;
    }

    @Override
    public void assignChildLayers(SurfaceControl.Transaction transaction) {
        int i = 1;
        for (int i2 = 0; i2 < this.mChildren.size(); i2++) {
            WindowState windowState = (WindowState) this.mChildren.get(i2);
            if (windowState.mAttrs.type == 1001) {
                windowState.assignLayer(transaction, -2);
            } else if (windowState.mAttrs.type == 1004) {
                windowState.assignLayer(transaction, -1);
            } else {
                windowState.assignLayer(transaction, i);
            }
            windowState.assignChildLayers(transaction);
            i++;
        }
    }

    void updateTapExcludeRegion(int i, int i2, int i3, int i4, int i5) {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            throw new IllegalStateException("Trying to update window not attached to any display.");
        }
        if (this.mTapExcludeRegionHolder == null) {
            this.mTapExcludeRegionHolder = new TapExcludeRegionHolder();
            displayContent.mTapExcludeProvidingWindows.add(this);
        }
        this.mTapExcludeRegionHolder.updateRegion(i, i2, i3, i4, i5);
        displayContent.setTouchExcludeRegion(this.mService.mFocusedApp != null && this.mService.mFocusedApp.getDisplayContent() == displayContent ? this.mService.mFocusedApp.getTask() : null);
    }

    void amendTapExcludeRegion(Region region) {
        this.mTapExcludeRegionHolder.amendRegion(region, getBounds());
    }

    @Override
    public boolean isInputMethodTarget() {
        return this.mService.mInputMethodTarget == this;
    }

    long getFrameNumber() {
        return this.mFrameNumber;
    }

    void setFrameNumber(long j) {
        this.mFrameNumber = j;
    }

    private final class MoveAnimationSpec implements LocalAnimationAdapter.AnimationSpec {
        private final long mDuration;
        private Point mFrom;
        private Interpolator mInterpolator;
        private Point mTo;

        private MoveAnimationSpec(int i, int i2, int i3, int i4) {
            this.mFrom = new Point();
            this.mTo = new Point();
            Animation animationLoadAnimation = AnimationUtils.loadAnimation(WindowState.this.mContext, R.anim.task_fragment_clear_top_open_exit);
            this.mDuration = (long) (animationLoadAnimation.computeDurationHint() * WindowState.this.mService.getWindowAnimationScaleLocked());
            this.mInterpolator = animationLoadAnimation.getInterpolator();
            this.mFrom.set(i, i2);
            this.mTo.set(i3, i4);
        }

        @Override
        public long getDuration() {
            return this.mDuration;
        }

        @Override
        public void apply(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, long j) {
            float interpolation = this.mInterpolator.getInterpolation(j / getDuration());
            transaction.setPosition(surfaceControl, this.mFrom.x + ((this.mTo.x - this.mFrom.x) * interpolation), this.mFrom.y + ((this.mTo.y - this.mFrom.y) * interpolation));
        }

        @Override
        public void dump(PrintWriter printWriter, String str) {
            printWriter.print(str);
            printWriter.print("from=");
            printWriter.print(this.mFrom);
            printWriter.print(" to=");
            printWriter.print(this.mTo);
            printWriter.print(" duration=");
            printWriter.println(this.mDuration);
        }

        @Override
        public void writeToProtoInner(ProtoOutputStream protoOutputStream) {
            long jStart = protoOutputStream.start(1146756268034L);
            this.mFrom.writeToProto(protoOutputStream, 1146756268033L);
            this.mTo.writeToProto(protoOutputStream, 1146756268034L);
            protoOutputStream.write(1112396529667L, this.mDuration);
            protoOutputStream.end(jStart);
        }
    }

    @Override
    public boolean isFullscreenOn() {
        return this.mIsFullscreenOn;
    }
}
