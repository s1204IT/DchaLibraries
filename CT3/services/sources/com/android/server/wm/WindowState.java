package com.android.server.wm;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.IApplicationToken;
import android.view.IWindow;
import android.view.IWindowFocusObserver;
import android.view.IWindowId;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import com.android.server.input.InputWindowHandle;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.DimLayer;
import com.mediatek.multiwindow.MultiWindowManager;
import java.io.PrintWriter;

final class WindowState implements WindowManagerPolicy.WindowState {
    static final boolean DEBUG_DISABLE_SAVING_SURFACES = false;
    static final int MINIMUM_VISIBLE_HEIGHT_IN_DP = 32;
    static final int MINIMUM_VISIBLE_WIDTH_IN_DP = 48;
    static final int RESIZE_HANDLE_WIDTH_IN_DP = 30;
    boolean mAnimatingExit;
    private boolean mAnimatingWithSavedSurface;
    boolean mAppDied;
    boolean mAppFreezing;
    final int mAppOp;
    AppWindowToken mAppToken;
    boolean mAttachedHidden;
    final WindowState mAttachedWindow;
    final int mBaseLayer;
    final IWindow mClient;
    InputChannel mClientChannel;
    private boolean mConfigHasChanged;
    boolean mContentChanged;
    boolean mContentInsetsChanged;
    final Context mContext;
    private DeadWindowEventReceiver mDeadWindowEventReceiver;
    final DeathRecipient mDeathRecipient;
    boolean mDestroying;
    DisplayContent mDisplayContent;
    boolean mDragResizing;
    boolean mDragResizingChangeReported;
    PowerManager.WakeLock mDrawLock;
    boolean mEnforceSizeCompat;
    RemoteCallbackList<IWindowFocusObserver> mFocusCallbacks;
    private boolean mForceHideNonSystemOverlayWindow;
    boolean mGivenInsetsPending;
    boolean mHaveFrame;
    boolean mInRelayout;
    InputChannel mInputChannel;
    final InputWindowHandle mInputWindowHandle;
    final boolean mIsFloatingLayer;
    final boolean mIsImWindow;
    final boolean mIsWallpaper;
    private boolean mJustMovedInStack;
    int mLastFreezeDuration;
    int mLastRequestedHeight;
    int mLastRequestedWidth;
    CharSequence mLastTitle;
    int mLayer;
    final boolean mLayoutAttached;
    boolean mLayoutNeeded;
    boolean mMovedByResize;
    boolean mNotOnAppsDisplay;
    boolean mObscured;
    boolean mOrientationChanging;
    boolean mOverscanInsetsChanged;
    final boolean mOwnerCanAddInternalSystemWindow;
    final int mOwnerUid;
    final WindowManagerPolicy mPolicy;
    boolean mRebuilding;
    boolean mRelayoutCalled;
    boolean mRemoveOnExit;
    boolean mRemoved;
    int mRequestedHeight;
    int mRequestedWidth;
    int mResizeMode;
    private boolean mResizedWhileNotDragResizing;
    private boolean mResizedWhileNotDragResizingReported;
    WindowToken mRootToken;
    int mSeq;
    final WindowManagerService mService;
    final Session mSession;
    private boolean mShowToOwnerOnly;
    boolean mStableInsetsChanged;
    String mStringNameCache;
    final int mSubLayer;
    int mSystemUiVisibility;
    AppWindowToken mTargetAppToken;
    WindowToken mToken;
    boolean mTurnOnScreen;
    int mViewVisibility;
    boolean mVisibleInsetsChanged;
    boolean mWallpaperVisible;
    boolean mWasExiting;
    boolean mWasVisibleBeforeClientHidden;
    final WindowStateAnimator mWinAnimator;
    boolean mWindowRemovalAllowed;
    int mXOffset;
    int mYOffset;
    static final String TAG = "WindowManager";
    private static final Rect sTmpRect = new Rect();
    static final Region sEmptyRegion = new Region();
    final WindowManager.LayoutParams mAttrs = new WindowManager.LayoutParams();
    final WindowList mChildWindows = new WindowList();
    boolean mPolicyVisibility = true;
    boolean mPolicyVisibilityAfterAnim = true;
    boolean mAppOpVisibility = true;
    int mLayoutSeq = -1;
    private final Configuration mTmpConfig = new Configuration();
    private Configuration mMergedConfiguration = new Configuration();
    final Point mShownPosition = new Point();
    final Rect mVisibleInsets = new Rect();
    final Rect mLastVisibleInsets = new Rect();
    final Rect mContentInsets = new Rect();
    final Rect mLastContentInsets = new Rect();
    final Rect mOverscanInsets = new Rect();
    final Rect mLastOverscanInsets = new Rect();
    final Rect mStableInsets = new Rect();
    final Rect mLastStableInsets = new Rect();
    final Rect mOutsets = new Rect();
    final Rect mLastOutsets = new Rect();
    boolean mOutsetsChanged = false;
    final Rect mGivenContentInsets = new Rect();
    final Rect mGivenVisibleInsets = new Rect();
    final Region mGivenTouchableRegion = new Region();
    int mTouchableInsets = 0;
    float mGlobalScale = 1.0f;
    float mInvGlobalScale = 1.0f;
    float mHScale = 1.0f;
    float mVScale = 1.0f;
    float mLastHScale = 1.0f;
    float mLastVScale = 1.0f;
    final Matrix mTmpMatrix = new Matrix();
    final Rect mFrame = new Rect();
    final Rect mLastFrame = new Rect();
    final Rect mCompatFrame = new Rect();
    final Rect mContainingFrame = new Rect();
    final Rect mParentFrame = new Rect();
    final Rect mDisplayFrame = new Rect();
    final Rect mOverscanFrame = new Rect();
    final Rect mStableFrame = new Rect();
    final Rect mDecorFrame = new Rect();
    final Rect mContentFrame = new Rect();
    final Rect mVisibleFrame = new Rect();
    final Rect mOutsetFrame = new Rect();
    final Rect mInsetFrame = new Rect();
    float mWallpaperX = -1.0f;
    float mWallpaperY = -1.0f;
    float mWallpaperXStep = -1.0f;
    float mWallpaperYStep = -1.0f;
    int mWallpaperDisplayOffsetX = Integer.MIN_VALUE;
    int mWallpaperDisplayOffsetY = Integer.MIN_VALUE;
    boolean mHasSurface = false;
    private boolean mSurfaceSaved = false;
    boolean mWillReplaceWindow = false;
    boolean mReplacingRemoveRequested = false;
    boolean mAnimateReplacingWindow = false;
    WindowState mReplacingWindow = null;
    boolean mSkipEnterAnimationForSeamlessReplacement = false;
    private final Rect mTmpRect = new Rect();
    boolean mResizedWhileGone = false;
    final IWindowId mWindowId = new IWindowId.Stub() {
        public void registerFocusObserver(IWindowFocusObserver observer) {
            WindowState.this.registerFocusObserver(observer);
        }

        public void unregisterFocusObserver(IWindowFocusObserver observer) {
            WindowState.this.unregisterFocusObserver(observer);
        }

        public boolean isFocused() {
            return WindowState.this.isFocused();
        }
    };

    WindowState(WindowManagerService service, Session s, IWindow c, WindowToken token, WindowState attachedWindow, int appOp, int seq, WindowManager.LayoutParams a, int viewVisibility, DisplayContent displayContent) {
        WindowToken parent;
        this.mNotOnAppsDisplay = false;
        this.mService = service;
        this.mSession = s;
        this.mClient = c;
        this.mAppOp = appOp;
        this.mToken = token;
        this.mOwnerUid = s.mUid;
        this.mOwnerCanAddInternalSystemWindow = s.mCanAddInternalSystemWindow;
        this.mAttrs.copyFrom(a);
        this.mViewVisibility = viewVisibility;
        this.mDisplayContent = displayContent;
        this.mPolicy = this.mService.mPolicy;
        this.mContext = this.mService.mContext;
        DeathRecipient deathRecipient = new DeathRecipient(this, null);
        this.mSeq = seq;
        this.mEnforceSizeCompat = (this.mAttrs.privateFlags & 128) != 0;
        if (WindowManagerService.localLOGV) {
            Slog.v(TAG, "Window " + this + " client=" + c.asBinder() + " token=" + token + " (" + this.mAttrs.token + ") params=" + a);
        }
        try {
            c.asBinder().linkToDeath(deathRecipient, 0);
            this.mDeathRecipient = deathRecipient;
            if (this.mAttrs.type < 1000 || this.mAttrs.type > 1999) {
                this.mBaseLayer = (this.mPolicy.windowTypeToLayerLw(a.type) * 10000) + 1000;
                this.mSubLayer = 0;
                this.mAttachedWindow = null;
                this.mLayoutAttached = false;
                boolean z = this.mAttrs.type == 2011 || this.mAttrs.type == 2012;
                this.mIsImWindow = z;
                this.mIsWallpaper = this.mAttrs.type == 2013;
                this.mIsFloatingLayer = !this.mIsImWindow ? this.mIsWallpaper : true;
            } else {
                this.mBaseLayer = (this.mPolicy.windowTypeToLayerLw(attachedWindow.mAttrs.type) * 10000) + 1000;
                this.mSubLayer = this.mPolicy.subWindowTypeToLayerLw(a.type);
                this.mAttachedWindow = attachedWindow;
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.v(TAG, "Adding " + this + " to " + this.mAttachedWindow);
                }
                WindowList childWindows = this.mAttachedWindow.mChildWindows;
                int numChildWindows = childWindows.size();
                if (numChildWindows == 0) {
                    childWindows.add(this);
                } else {
                    boolean added = false;
                    for (int i = 0; i < numChildWindows; i++) {
                        int childSubLayer = childWindows.get(i).mSubLayer;
                        if (this.mSubLayer < childSubLayer || (this.mSubLayer == childSubLayer && childSubLayer < 0)) {
                            childWindows.add(i, this);
                            added = true;
                            break;
                        }
                    }
                    if (!added) {
                        childWindows.add(this);
                    }
                }
                this.mLayoutAttached = this.mAttrs.type != 1003;
                boolean z2 = attachedWindow.mAttrs.type == 2011 || attachedWindow.mAttrs.type == 2012;
                this.mIsImWindow = z2;
                this.mIsWallpaper = attachedWindow.mAttrs.type == 2013;
                this.mIsFloatingLayer = !this.mIsImWindow ? this.mIsWallpaper : true;
            }
            WindowState appWin = this;
            while (appWin.isChildWindow()) {
                appWin = appWin.mAttachedWindow;
            }
            WindowToken appToken = appWin.mToken;
            while (appToken.appWindowToken == null && (parent = this.mService.mTokenMap.get(appToken.token)) != null && appToken != parent) {
                appToken = parent;
            }
            this.mRootToken = appToken;
            this.mAppToken = appToken.appWindowToken;
            if (this.mAppToken != null) {
                DisplayContent appDisplay = getDisplayContent();
                this.mNotOnAppsDisplay = displayContent != appDisplay;
                if (this.mAppToken.showForAllUsers) {
                    this.mAttrs.flags |= PackageManagerService.DumpState.DUMP_FROZEN;
                }
            }
            this.mWinAnimator = new WindowStateAnimator(this);
            this.mWinAnimator.mAlpha = a.alpha;
            this.mRequestedWidth = 0;
            this.mRequestedHeight = 0;
            this.mLastRequestedWidth = 0;
            this.mLastRequestedHeight = 0;
            this.mXOffset = 0;
            this.mYOffset = 0;
            this.mLayer = 0;
            this.mInputWindowHandle = new InputWindowHandle(this.mAppToken != null ? this.mAppToken.mInputApplicationHandle : null, this, displayContent.getDisplayId());
        } catch (RemoteException e) {
            this.mDeathRecipient = null;
            this.mAttachedWindow = null;
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
            Slog.v(TAG, "Attaching " + this + " token=" + this.mToken + ", list=" + this.mToken.windows);
        }
        this.mSession.windowAddedLocked();
    }

    public int getOwningUid() {
        return this.mOwnerUid;
    }

    public String getOwningPackage() {
        return this.mAttrs.packageName;
    }

    private void subtractInsets(Rect frame, Rect layoutFrame, Rect insetFrame, Rect displayFrame) {
        int left = Math.max(0, insetFrame.left - Math.max(layoutFrame.left, displayFrame.left));
        int top = Math.max(0, insetFrame.top - Math.max(layoutFrame.top, displayFrame.top));
        int right = Math.max(0, Math.min(layoutFrame.right, displayFrame.right) - insetFrame.right);
        int bottom = Math.max(0, Math.min(layoutFrame.bottom, displayFrame.bottom) - insetFrame.bottom);
        frame.inset(left, top, right, bottom);
    }

    public void computeFrameLw(Rect pf, Rect df, Rect of, Rect cf, Rect vf, Rect dcf, Rect sf, Rect osf) {
        Rect layoutDisplayFrame;
        Rect layoutContainingFrame;
        int layoutXDiff;
        int layoutYDiff;
        DisplayContent displayContent;
        if (!this.mWillReplaceWindow || (!this.mAnimatingExit && this.mReplacingRemoveRequested)) {
            this.mHaveFrame = true;
            Task task = getTask();
            boolean fullscreenTask = !isInMultiWindowMode();
            boolean zIsFloating = task != null ? task.isFloating() : false;
            if (fullscreenTask) {
                this.mInsetFrame.setEmpty();
            } else {
                task.getTempInsetBounds(this.mInsetFrame);
            }
            if (fullscreenTask || layoutInParentFrame() || !isDefaultDisplay()) {
                this.mContainingFrame.set(pf);
                this.mDisplayFrame.set(df);
                layoutDisplayFrame = df;
                layoutContainingFrame = pf;
                layoutXDiff = 0;
                layoutYDiff = 0;
            } else {
                task.getBounds(this.mContainingFrame);
                if (this.mAppToken != null && !this.mAppToken.mFrozenBounds.isEmpty()) {
                    Rect frozen = this.mAppToken.mFrozenBounds.peek();
                    this.mContainingFrame.right = this.mContainingFrame.left + frozen.width();
                    this.mContainingFrame.bottom = this.mContainingFrame.top + frozen.height();
                }
                WindowState imeWin = this.mService.mInputMethodWindow;
                if (imeWin != null && imeWin.isVisibleNow() && this.mService.mInputMethodTarget == this) {
                    if (zIsFloating && this.mContainingFrame.bottom > cf.bottom) {
                        this.mContainingFrame.top -= this.mContainingFrame.bottom - cf.bottom;
                    } else if (this.mContainingFrame.bottom > pf.bottom) {
                        this.mContainingFrame.bottom = pf.bottom;
                    }
                }
                if (zIsFloating && this.mContainingFrame.isEmpty()) {
                    this.mContainingFrame.set(cf);
                }
                this.mDisplayFrame.set(this.mContainingFrame);
                layoutXDiff = !this.mInsetFrame.isEmpty() ? this.mInsetFrame.left - this.mContainingFrame.left : 0;
                layoutYDiff = !this.mInsetFrame.isEmpty() ? this.mInsetFrame.top - this.mContainingFrame.top : 0;
                layoutContainingFrame = !this.mInsetFrame.isEmpty() ? this.mInsetFrame : this.mContainingFrame;
                this.mTmpRect.set(0, 0, this.mDisplayContent.getDisplayInfo().logicalWidth, this.mDisplayContent.getDisplayInfo().logicalHeight);
                subtractInsets(this.mDisplayFrame, layoutContainingFrame, df, this.mTmpRect);
                if (!layoutInParentFrame()) {
                    subtractInsets(this.mContainingFrame, layoutContainingFrame, pf, this.mTmpRect);
                    subtractInsets(this.mInsetFrame, layoutContainingFrame, pf, this.mTmpRect);
                }
                layoutDisplayFrame = df;
                df.intersect(layoutContainingFrame);
            }
            int pw = this.mContainingFrame.width();
            int ph = this.mContainingFrame.height();
            if (!this.mParentFrame.equals(pf)) {
                this.mParentFrame.set(pf);
                this.mContentChanged = true;
            }
            if (this.mRequestedWidth != this.mLastRequestedWidth || this.mRequestedHeight != this.mLastRequestedHeight) {
                this.mLastRequestedWidth = this.mRequestedWidth;
                this.mLastRequestedHeight = this.mRequestedHeight;
                this.mContentChanged = true;
            }
            this.mOverscanFrame.set(of);
            this.mContentFrame.set(cf);
            this.mVisibleFrame.set(vf);
            this.mDecorFrame.set(dcf);
            this.mStableFrame.set(sf);
            boolean hasOutsets = osf != null;
            if (hasOutsets) {
                this.mOutsetFrame.set(osf);
            }
            int fw = this.mFrame.width();
            int fh = this.mFrame.height();
            applyGravityAndUpdateFrame(layoutContainingFrame, layoutDisplayFrame);
            if (hasOutsets) {
                this.mOutsets.set(Math.max(this.mContentFrame.left - this.mOutsetFrame.left, 0), Math.max(this.mContentFrame.top - this.mOutsetFrame.top, 0), Math.max(this.mOutsetFrame.right - this.mContentFrame.right, 0), Math.max(this.mOutsetFrame.bottom - this.mContentFrame.bottom, 0));
            } else {
                this.mOutsets.set(0, 0, 0, 0);
            }
            if (zIsFloating && !this.mFrame.isEmpty()) {
                int height = Math.min(this.mFrame.height(), this.mContentFrame.height());
                int width = Math.min(this.mContentFrame.width(), this.mFrame.width());
                DisplayMetrics displayMetrics = getDisplayContent().getDisplayMetrics();
                int minVisibleHeight = WindowManagerService.dipToPixel(32, displayMetrics);
                int minVisibleWidth = WindowManagerService.dipToPixel(48, displayMetrics);
                int top = Math.max(this.mContentFrame.top, Math.min(this.mFrame.top, this.mContentFrame.bottom - minVisibleHeight));
                int left = Math.max((this.mContentFrame.left + minVisibleWidth) - width, Math.min(this.mFrame.left, this.mContentFrame.right - minVisibleWidth));
                this.mFrame.set(left, top, left + width, top + height);
                this.mContentFrame.set(this.mFrame);
                this.mVisibleFrame.set(this.mContentFrame);
                this.mStableFrame.set(this.mContentFrame);
            } else if (this.mAttrs.type == 2034) {
                this.mDisplayContent.getDockedDividerController().positionDockedStackedDivider(this.mFrame);
                this.mContentFrame.set(this.mFrame);
                if (!this.mFrame.equals(this.mLastFrame)) {
                    this.mMovedByResize = true;
                }
            } else {
                this.mContentFrame.set(Math.max(this.mContentFrame.left, this.mFrame.left), Math.max(this.mContentFrame.top, this.mFrame.top), Math.min(this.mContentFrame.right, this.mFrame.right), Math.min(this.mContentFrame.bottom, this.mFrame.bottom));
                this.mVisibleFrame.set(Math.max(this.mVisibleFrame.left, this.mFrame.left), Math.max(this.mVisibleFrame.top, this.mFrame.top), Math.min(this.mVisibleFrame.right, this.mFrame.right), Math.min(this.mVisibleFrame.bottom, this.mFrame.bottom));
                this.mStableFrame.set(Math.max(this.mStableFrame.left, this.mFrame.left), Math.max(this.mStableFrame.top, this.mFrame.top), Math.min(this.mStableFrame.right, this.mFrame.right), Math.min(this.mStableFrame.bottom, this.mFrame.bottom));
            }
            if (fullscreenTask && !zIsFloating) {
                this.mOverscanInsets.set(Math.max(this.mOverscanFrame.left - layoutContainingFrame.left, 0), Math.max(this.mOverscanFrame.top - layoutContainingFrame.top, 0), Math.max(layoutContainingFrame.right - this.mOverscanFrame.right, 0), Math.max(layoutContainingFrame.bottom - this.mOverscanFrame.bottom, 0));
            }
            if (this.mAttrs.type == 2034) {
                this.mStableInsets.set(Math.max(this.mStableFrame.left - this.mDisplayFrame.left, 0), Math.max(this.mStableFrame.top - this.mDisplayFrame.top, 0), Math.max(this.mDisplayFrame.right - this.mStableFrame.right, 0), Math.max(this.mDisplayFrame.bottom - this.mStableFrame.bottom, 0));
                this.mContentInsets.setEmpty();
                this.mVisibleInsets.setEmpty();
            } else {
                getDisplayContent().getLogicalDisplayRect(this.mTmpRect);
                boolean overrideRightInset = !fullscreenTask && this.mFrame.right > this.mTmpRect.right;
                boolean overrideBottomInset = !fullscreenTask && this.mFrame.bottom > this.mTmpRect.bottom;
                this.mContentInsets.set(this.mContentFrame.left - this.mFrame.left, this.mContentFrame.top - this.mFrame.top, overrideRightInset ? this.mTmpRect.right - this.mContentFrame.right : this.mFrame.right - this.mContentFrame.right, overrideBottomInset ? this.mTmpRect.bottom - this.mContentFrame.bottom : this.mFrame.bottom - this.mContentFrame.bottom);
                this.mVisibleInsets.set(this.mVisibleFrame.left - this.mFrame.left, this.mVisibleFrame.top - this.mFrame.top, overrideRightInset ? this.mTmpRect.right - this.mVisibleFrame.right : this.mFrame.right - this.mVisibleFrame.right, overrideBottomInset ? this.mTmpRect.bottom - this.mVisibleFrame.bottom : this.mFrame.bottom - this.mVisibleFrame.bottom);
                this.mStableInsets.set(Math.max(this.mStableFrame.left - this.mFrame.left, 0), Math.max(this.mStableFrame.top - this.mFrame.top, 0), overrideRightInset ? Math.max(this.mTmpRect.right - this.mStableFrame.right, 0) : Math.max(this.mFrame.right - this.mStableFrame.right, 0), overrideBottomInset ? Math.max(this.mTmpRect.bottom - this.mStableFrame.bottom, 0) : Math.max(this.mFrame.bottom - this.mStableFrame.bottom, 0));
            }
            this.mFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mCompatFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mContentFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mVisibleFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mStableFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mCompatFrame.set(this.mFrame);
            if (this.mEnforceSizeCompat) {
                this.mOverscanInsets.scale(this.mInvGlobalScale);
                this.mContentInsets.scale(this.mInvGlobalScale);
                this.mVisibleInsets.scale(this.mInvGlobalScale);
                this.mStableInsets.scale(this.mInvGlobalScale);
                this.mOutsets.scale(this.mInvGlobalScale);
                this.mCompatFrame.scale(this.mInvGlobalScale);
            }
            if (this.mIsWallpaper && ((fw != this.mFrame.width() || fh != this.mFrame.height()) && (displayContent = getDisplayContent()) != null)) {
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                this.mService.mWallpaperControllerLocked.updateWallpaperOffset(this, displayInfo.logicalWidth, displayInfo.logicalHeight, false);
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT || WindowManagerService.localLOGV) {
                Slog.v(TAG, "Resolving (mRequestedWidth=" + this.mRequestedWidth + ", mRequestedheight=" + this.mRequestedHeight + ") to (pw=" + pw + ", ph=" + ph + "): frame=" + this.mFrame.toShortString() + " ci=" + this.mContentInsets.toShortString() + " vi=" + this.mVisibleInsets.toShortString() + " si=" + this.mStableInsets.toShortString() + " of=" + this.mOutsets.toShortString());
            }
        }
    }

    public Rect getFrameLw() {
        return this.mFrame;
    }

    public Point getShownPositionLw() {
        return this.mShownPosition;
    }

    public Rect getDisplayFrameLw() {
        return this.mDisplayFrame;
    }

    public Rect getOverscanFrameLw() {
        return this.mOverscanFrame;
    }

    public Rect getContentFrameLw() {
        return this.mContentFrame;
    }

    public Rect getVisibleFrameLw() {
        return this.mVisibleFrame;
    }

    public boolean getGivenInsetsPendingLw() {
        return this.mGivenInsetsPending;
    }

    public Rect getGivenContentInsetsLw() {
        return this.mGivenContentInsets;
    }

    public Rect getGivenVisibleInsetsLw() {
        return this.mGivenVisibleInsets;
    }

    public WindowManager.LayoutParams getAttrs() {
        return this.mAttrs;
    }

    public boolean getNeedsMenuLw(WindowManagerPolicy.WindowState bottom) {
        int index = -1;
        WindowState ws = this;
        WindowList windows = getWindowList();
        while (ws.mAttrs.needsMenuKey == 0) {
            if (ws == bottom) {
                return false;
            }
            if (index < 0) {
                index = windows.indexOf(ws);
            }
            index--;
            if (index < 0) {
                return false;
            }
            WindowState ws2 = windows.get(index);
            ws = ws2;
        }
        return ws.mAttrs.needsMenuKey == 1;
    }

    public int getSystemUiVisibility() {
        return this.mSystemUiVisibility;
    }

    public int getSurfaceLayer() {
        return this.mLayer;
    }

    public int getBaseType() {
        WindowState win = this;
        while (win.isChildWindow()) {
            win = win.mAttachedWindow;
        }
        return win.mAttrs.type;
    }

    public IApplicationToken getAppToken() {
        if (this.mAppToken != null) {
            return this.mAppToken.appToken;
        }
        return null;
    }

    public boolean isVoiceInteraction() {
        if (this.mAppToken != null) {
            return this.mAppToken.voiceInteraction;
        }
        return false;
    }

    boolean setInsetsChanged() {
        this.mOverscanInsetsChanged = (!this.mLastOverscanInsets.equals(this.mOverscanInsets)) | this.mOverscanInsetsChanged;
        this.mContentInsetsChanged = (!this.mLastContentInsets.equals(this.mContentInsets)) | this.mContentInsetsChanged;
        this.mVisibleInsetsChanged = (!this.mLastVisibleInsets.equals(this.mVisibleInsets)) | this.mVisibleInsetsChanged;
        this.mStableInsetsChanged = (!this.mLastStableInsets.equals(this.mStableInsets)) | this.mStableInsetsChanged;
        this.mOutsetsChanged |= this.mLastOutsets.equals(this.mOutsets) ? false : true;
        if (this.mOverscanInsetsChanged || this.mContentInsetsChanged || this.mVisibleInsetsChanged) {
            return true;
        }
        return this.mOutsetsChanged;
    }

    public DisplayContent getDisplayContent() {
        if (this.mAppToken == null || this.mNotOnAppsDisplay) {
            return this.mDisplayContent;
        }
        TaskStack stack = getStack();
        return stack == null ? this.mDisplayContent : stack.getDisplayContent();
    }

    public DisplayInfo getDisplayInfo() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent != null) {
            return displayContent.getDisplayInfo();
        }
        return null;
    }

    public int getDisplayId() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return -1;
        }
        return displayContent.getDisplayId();
    }

    Task getTask() {
        if (this.mAppToken != null) {
            return this.mAppToken.mTask;
        }
        return null;
    }

    TaskStack getStack() {
        Task task = getTask();
        if (task != null && task.mStack != null) {
            return task.mStack;
        }
        if (this.mAttrs.type < 2000 || this.mDisplayContent == null) {
            return null;
        }
        return this.mDisplayContent.getHomeStack();
    }

    void getVisibleBounds(Rect bounds) {
        Task task = getTask();
        boolean intersectWithStackBounds = task != null ? task.cropWindowsToStackBounds() : false;
        bounds.setEmpty();
        this.mTmpRect.setEmpty();
        if (intersectWithStackBounds) {
            TaskStack stack = task.mStack;
            if (stack != null) {
                stack.getDimBounds(this.mTmpRect);
            } else {
                intersectWithStackBounds = false;
            }
        }
        bounds.set(this.mVisibleFrame);
        if (intersectWithStackBounds) {
            bounds.intersect(this.mTmpRect);
        }
        if (!bounds.isEmpty()) {
            return;
        }
        bounds.set(this.mFrame);
        if (intersectWithStackBounds) {
            bounds.intersect(this.mTmpRect);
        }
    }

    public long getInputDispatchingTimeoutNanos() {
        if (this.mAppToken != null) {
            return this.mAppToken.inputDispatchingTimeoutNanos;
        }
        return 30000000000L;
    }

    public boolean hasAppShownWindows() {
        if (this.mAppToken == null) {
            return false;
        }
        if (this.mAppToken.firstWindowDrawn) {
            return true;
        }
        return this.mAppToken.startingDisplayed;
    }

    boolean isIdentityMatrix(float dsdx, float dtdx, float dsdy, float dtdy) {
        return dsdx >= 0.99999f && dsdx <= 1.00001f && dtdy >= 0.99999f && dtdy <= 1.00001f && dtdx >= -1.0E-6f && dtdx <= 1.0E-6f && dsdy >= -1.0E-6f && dsdy <= 1.0E-6f;
    }

    void prelayout() {
        if (this.mEnforceSizeCompat) {
            this.mGlobalScale = this.mService.mCompatibleScreenScale;
            this.mInvGlobalScale = 1.0f / this.mGlobalScale;
        } else {
            this.mInvGlobalScale = 1.0f;
            this.mGlobalScale = 1.0f;
        }
    }

    private boolean isVisibleUnchecked() {
        if (!this.mHasSurface || !this.mPolicyVisibility || this.mAttachedHidden || this.mAnimatingExit || this.mDestroying) {
            return false;
        }
        if (this.mIsWallpaper) {
            return this.mWallpaperVisible;
        }
        return true;
    }

    public boolean isVisibleLw() {
        if (this.mAppToken == null || !this.mAppToken.hiddenRequested) {
            return isVisibleUnchecked();
        }
        return false;
    }

    public boolean isVisibleOrBehindKeyguardLw() {
        if (this.mRootToken.waitingToShow && this.mService.mAppTransition.isTransitionSet()) {
            return false;
        }
        AppWindowToken atoken = this.mAppToken;
        boolean animating = (atoken == null || atoken.mAppAnimator.animation == null) ? false : true;
        if (!this.mHasSurface || this.mDestroying || this.mAnimatingExit || (atoken != null ? atoken.hiddenRequested : !this.mPolicyVisibility)) {
            return false;
        }
        if ((this.mAttachedHidden || this.mViewVisibility != 0 || this.mRootToken.hidden) && this.mWinAnimator.mAnimation == null) {
            return animating;
        }
        return true;
    }

    public boolean isWinVisibleLw() {
        if (this.mAppToken == null || !this.mAppToken.hiddenRequested || this.mAppToken.mAppAnimator.animating) {
            return isVisibleUnchecked();
        }
        return false;
    }

    boolean isVisibleNow() {
        if (!this.mRootToken.hidden || this.mAttrs.type == 3) {
            return isVisibleUnchecked();
        }
        return false;
    }

    boolean isPotentialDragTarget() {
        return (!isVisibleNow() || this.mRemoved || this.mInputChannel == null || this.mInputWindowHandle == null) ? false : true;
    }

    boolean isVisibleOrAdding() {
        AppWindowToken atoken = this.mAppToken;
        if ((this.mHasSurface || (!this.mRelayoutCalled && this.mViewVisibility == 0)) && this.mPolicyVisibility && !this.mAttachedHidden) {
            return ((atoken != null && atoken.hiddenRequested) || this.mAnimatingExit || this.mDestroying) ? false : true;
        }
        return false;
    }

    boolean isOnScreen() {
        if (this.mPolicyVisibility) {
            return isOnScreenIgnoringKeyguard();
        }
        return false;
    }

    boolean isOnScreenIgnoringKeyguard() {
        if (!this.mHasSurface || this.mDestroying) {
            return false;
        }
        AppWindowToken atoken = this.mAppToken;
        return atoken != null ? ((this.mAttachedHidden || atoken.hiddenRequested) && this.mWinAnimator.mAnimation == null && atoken.mAppAnimator.animation == null) ? false : true : (this.mAttachedHidden && this.mWinAnimator.mAnimation == null) ? false : true;
    }

    boolean mightAffectAllDrawn(boolean visibleOnly) {
        boolean isViewVisible = (this.mAppToken == null || !this.mAppToken.clientHidden) && this.mViewVisibility == 0 && !this.mWindowRemovalAllowed;
        return (((!isOnScreenIgnoringKeyguard() || (visibleOnly && !isViewVisible)) && this.mWinAnimator.mAttrType != 1) || this.mAnimatingExit || this.mDestroying) ? false : true;
    }

    boolean isInteresting() {
        if (this.mAppToken == null || this.mAppDied) {
            return false;
        }
        return (this.mAppToken.mAppAnimator.freezingScreen && this.mAppFreezing) ? false : true;
    }

    boolean isReadyForDisplay() {
        if (this.mRootToken.waitingToShow && this.mService.mAppTransition.isTransitionSet()) {
            return false;
        }
        if (!this.mHasSurface || !this.mPolicyVisibility || this.mDestroying) {
            return false;
        }
        if ((this.mAttachedHidden || this.mViewVisibility != 0 || this.mRootToken.hidden) && this.mWinAnimator.mAnimation == null) {
            return (this.mAppToken == null || this.mAppToken.mAppAnimator.animation == null) ? false : true;
        }
        return true;
    }

    boolean isReadyForDisplayIgnoringKeyguard() {
        if (this.mRootToken.waitingToShow && this.mService.mAppTransition.isTransitionSet()) {
            return false;
        }
        AppWindowToken atoken = this.mAppToken;
        if ((atoken == null && !this.mPolicyVisibility) || !this.mHasSurface || this.mDestroying) {
            return false;
        }
        if ((this.mAttachedHidden || this.mViewVisibility != 0 || this.mRootToken.hidden) && this.mWinAnimator.mAnimation == null) {
            return (atoken == null || atoken.mAppAnimator.animation == null || this.mWinAnimator.isDummyAnimation()) ? false : true;
        }
        return true;
    }

    public boolean isDisplayedLw() {
        AppWindowToken atoken = this.mAppToken;
        if (!isDrawnLw() || !this.mPolicyVisibility) {
            return false;
        }
        if ((this.mAttachedHidden || (atoken != null && atoken.hiddenRequested)) && !this.mWinAnimator.mAnimating) {
            return (atoken == null || atoken.mAppAnimator.animation == null) ? false : true;
        }
        return true;
    }

    public boolean isAnimatingLw() {
        if (this.mWinAnimator.mAnimation == null) {
            return (this.mAppToken == null || this.mAppToken.mAppAnimator.animation == null) ? false : true;
        }
        return true;
    }

    public boolean isGoneForLayoutLw() {
        AppWindowToken atoken = this.mAppToken;
        if (this.mViewVisibility == 8 || !this.mRelayoutCalled || ((atoken == null && this.mRootToken.hidden) || ((atoken != null && atoken.hiddenRequested) || this.mAttachedHidden || (this.mAnimatingExit && !isAnimatingLw())))) {
            return true;
        }
        return this.mDestroying;
    }

    public boolean isDrawFinishedLw() {
        if (!this.mHasSurface || this.mDestroying) {
            return false;
        }
        return this.mWinAnimator.mDrawState == 2 || this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4;
    }

    public boolean isDrawnLw() {
        if (!this.mHasSurface || this.mDestroying) {
            return false;
        }
        return this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4;
    }

    boolean isOpaqueDrawn() {
        if (((!this.mIsWallpaper && this.mAttrs.format == -1) || (this.mIsWallpaper && this.mWallpaperVisible)) && isDrawnLw() && this.mWinAnimator.mAnimation == null) {
            return this.mAppToken == null || this.mAppToken.mAppAnimator.animation == null;
        }
        return false;
    }

    boolean hasMoved() {
        if (!this.mHasSurface || (!(this.mContentChanged || this.mMovedByResize) || this.mAnimatingExit || !this.mService.okToDisplay() || (this.mFrame.top == this.mLastFrame.top && this.mFrame.left == this.mLastFrame.left))) {
            return false;
        }
        return this.mAttachedWindow == null || !this.mAttachedWindow.hasMoved();
    }

    boolean isObscuringFullscreen(DisplayInfo displayInfo) {
        Task task = getTask();
        return (task == null || task.mStack == null || task.mStack.isFullscreen()) && isOpaqueDrawn() && isFrameFullscreen(displayInfo);
    }

    boolean isFrameFullscreen(DisplayInfo displayInfo) {
        return this.mFrame.left <= 0 && this.mFrame.top <= 0 && this.mFrame.right >= displayInfo.appWidth && this.mFrame.bottom >= displayInfo.appHeight;
    }

    boolean isConfigChanged() {
        getMergedConfig(this.mTmpConfig);
        boolean configChanged = this.mMergedConfiguration.equals(Configuration.EMPTY) || this.mTmpConfig.diff(this.mMergedConfiguration) != 0;
        if ((this.mAttrs.privateFlags & 1024) != 0) {
            this.mConfigHasChanged |= configChanged;
            boolean configChanged2 = this.mConfigHasChanged;
            return configChanged2;
        }
        return configChanged;
    }

    boolean isAdjustedForMinimizedDock() {
        if (this.mAppToken == null || this.mAppToken.mTask == null) {
            return false;
        }
        return this.mAppToken.mTask.mStack.isAdjustedForMinimizedDock();
    }

    void removeLocked() {
        disposeInputChannel();
        if (isChildWindow()) {
            if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v(TAG, "Removing " + this + " from " + this.mAttachedWindow);
            }
            this.mAttachedWindow.mChildWindows.remove(this);
        }
        this.mWinAnimator.destroyDeferredSurfaceLocked();
        this.mWinAnimator.destroySurfaceLocked();
        this.mSession.windowRemovedLocked();
        try {
            this.mClient.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
        } catch (RuntimeException e) {
        }
    }

    void setHasSurface(boolean hasSurface) {
        this.mHasSurface = hasSurface;
    }

    int getAnimLayerAdjustment() {
        if (this.mTargetAppToken != null) {
            return this.mTargetAppToken.mAppAnimator.animLayerAdjustment;
        }
        if (this.mAppToken != null) {
            return this.mAppToken.mAppAnimator.animLayerAdjustment;
        }
        return 0;
    }

    void scheduleAnimationIfDimming() {
        DimLayer.DimLayerUser dimLayerUser;
        if (this.mDisplayContent == null || (dimLayerUser = getDimLayerUser()) == null || !this.mDisplayContent.mDimLayerController.isDimming(dimLayerUser, this.mWinAnimator)) {
            return;
        }
        this.mService.scheduleAnimationLocked();
    }

    void notifyMovedInStack() {
        this.mJustMovedInStack = true;
    }

    boolean hasJustMovedInStack() {
        return this.mJustMovedInStack;
    }

    void resetJustMovedInStack() {
        this.mJustMovedInStack = false;
    }

    private final class DeadWindowEventReceiver extends InputEventReceiver {
        DeadWindowEventReceiver(InputChannel inputChannel) {
            super(inputChannel, WindowState.this.mService.mH.getLooper());
        }

        public void onInputEvent(InputEvent event) {
            finishInputEvent(event, true);
        }
    }

    void openInputChannel(InputChannel outInputChannel) {
        if (this.mInputChannel != null) {
            throw new IllegalStateException("Window already has an input channel.");
        }
        String name = makeInputChannelName();
        InputChannel[] inputChannels = InputChannel.openInputChannelPair(name);
        this.mInputChannel = inputChannels[0];
        this.mClientChannel = inputChannels[1];
        this.mInputWindowHandle.inputChannel = inputChannels[0];
        if (outInputChannel != null) {
            this.mClientChannel.transferTo(outInputChannel);
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

    void applyDimLayerIfNeeded() {
        AppWindowToken token = this.mAppToken;
        if (token != null && token.removed) {
            return;
        }
        if (!this.mAnimatingExit && this.mAppDied) {
            this.mDisplayContent.mDimLayerController.applyDimAbove(getDimLayerUser(), this.mWinAnimator);
        } else {
            if ((this.mAttrs.flags & 2) == 0 || this.mDisplayContent == null || this.mAnimatingExit || !isVisibleUnchecked()) {
                return;
            }
            this.mDisplayContent.mDimLayerController.applyDimBehind(getDimLayerUser(), this.mWinAnimator);
        }
    }

    DimLayer.DimLayerUser getDimLayerUser() {
        Task task = getTask();
        if (task != null) {
            return task;
        }
        return getStack();
    }

    void maybeRemoveReplacedWindow() {
        if (this.mAppToken == null) {
            return;
        }
        for (int i = this.mAppToken.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState win = this.mAppToken.allAppWindows.get(i);
            if (win.mWillReplaceWindow && win.mReplacingWindow == this && hasDrawnLw()) {
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.d(TAG, "Removing replaced window: " + win);
                }
                if (win.isDimming()) {
                    win.transferDimToReplacement();
                }
                win.mWillReplaceWindow = false;
                boolean animateReplacingWindow = win.mAnimateReplacingWindow;
                win.mAnimateReplacingWindow = false;
                win.mReplacingRemoveRequested = false;
                win.mReplacingWindow = null;
                this.mSkipEnterAnimationForSeamlessReplacement = false;
                if (win.mAnimatingExit || !animateReplacingWindow) {
                    this.mService.removeWindowInnerLocked(win);
                }
            }
        }
    }

    void setDisplayLayoutNeeded() {
        if (this.mDisplayContent == null) {
            return;
        }
        this.mDisplayContent.layoutNeeded = true;
    }

    boolean inDockedWorkspace() {
        Task task = getTask();
        if (task != null) {
            return task.inDockedWorkspace();
        }
        return false;
    }

    boolean inPinnedWorkspace() {
        Task task = getTask();
        if (task != null) {
            return task.inPinnedWorkspace();
        }
        return false;
    }

    boolean isDockedInEffect() {
        Task task = getTask();
        if (task != null) {
            return task.isDockedInEffect();
        }
        return false;
    }

    void applyScrollIfNeeded() {
        Task task = getTask();
        if (task == null) {
            return;
        }
        task.applyScrollToWindowIfNeeded(this);
    }

    void applyAdjustForImeIfNeeded() {
        Task task = getTask();
        if (task == null || task.mStack == null || !task.mStack.isAdjustedForIme()) {
            return;
        }
        task.mStack.applyAdjustForImeIfNeeded(task);
    }

    int getTouchableRegion(Region region, int flags) {
        boolean modal = (flags & 40) == 0;
        if (modal && this.mAppToken != null) {
            flags |= 32;
            DimLayer.DimLayerUser dimLayerUser = getDimLayerUser();
            if (dimLayerUser != null) {
                dimLayerUser.getDimBounds(this.mTmpRect);
            } else {
                getVisibleBounds(this.mTmpRect);
            }
            if (inFreeformWorkspace()) {
                DisplayMetrics displayMetrics = getDisplayContent().getDisplayMetrics();
                int delta = WindowManagerService.dipToPixel(30, displayMetrics);
                this.mTmpRect.inset(-delta, -delta);
            }
            region.set(this.mTmpRect);
            cropRegionToStackBoundsIfNeeded(region);
        } else {
            getTouchableRegion(region);
        }
        return flags;
    }

    void checkPolicyVisibilityChange() {
        if (this.mPolicyVisibility != this.mPolicyVisibilityAfterAnim) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG, "Policy visibility changing after anim in " + this.mWinAnimator + ": " + this.mPolicyVisibilityAfterAnim);
            }
            this.mPolicyVisibility = this.mPolicyVisibilityAfterAnim;
            setDisplayLayoutNeeded();
            if (this.mPolicyVisibility) {
                return;
            }
            if (this.mService.mCurrentFocus == this) {
                if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                    Slog.i(TAG, "setAnimationLocked: setting mFocusMayChange true");
                }
                this.mService.mFocusMayChange = true;
            }
            this.mService.enableScreenIfNeededLocked();
        }
    }

    void setRequestedSize(int requestedWidth, int requestedHeight) {
        if (this.mRequestedWidth == requestedWidth && this.mRequestedHeight == requestedHeight) {
            return;
        }
        this.mLayoutNeeded = true;
        this.mRequestedWidth = requestedWidth;
        this.mRequestedHeight = requestedHeight;
    }

    void prepareWindowToDisplayDuringRelayout(Configuration outConfig) {
        if ((this.mAttrs.softInputMode & 240) == 16) {
            this.mLayoutNeeded = true;
        }
        if (isDrawnLw() && this.mService.okToDisplay()) {
            this.mWinAnimator.applyEnterAnimationLocked();
        }
        if ((this.mAttrs.flags & 2097152) != 0) {
            Slog.v(TAG, "Relayout window turning screen on: " + this);
            this.mTurnOnScreen = true;
        }
        if (!isConfigChanged()) {
            return;
        }
        Configuration newConfig = updateConfiguration();
        if (WindowManagerDebugConfig.DEBUG_CONFIGURATION) {
            Slog.i(TAG, "Window " + this + " visible with new config: " + newConfig);
        }
        outConfig.setTo(newConfig);
    }

    void adjustStartingWindowFlags() {
        if (this.mAttrs.type != 1 || this.mAppToken == null || this.mAppToken.startingWindow == null) {
            return;
        }
        WindowManager.LayoutParams sa = this.mAppToken.startingWindow.mAttrs;
        sa.flags = (sa.flags & (-4718594)) | (this.mAttrs.flags & 4718593);
    }

    void setWindowScale(int requestedWidth, int requestedHeight) {
        boolean scaledWindow = (this.mAttrs.flags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0;
        if (scaledWindow) {
            this.mHScale = this.mAttrs.width != requestedWidth ? this.mAttrs.width / requestedWidth : 1.0f;
            this.mVScale = this.mAttrs.height != requestedHeight ? this.mAttrs.height / requestedHeight : 1.0f;
        } else {
            this.mVScale = 1.0f;
            this.mHScale = 1.0f;
        }
    }

    private class DeathRecipient implements IBinder.DeathRecipient {
        DeathRecipient(WindowState this$0, DeathRecipient deathRecipient) {
            this();
        }

        private DeathRecipient() {
        }

        @Override
        public void binderDied() {
            try {
                synchronized (WindowState.this.mService.mWindowMap) {
                    WindowState win = WindowState.this.mService.windowForClientLocked(WindowState.this.mSession, WindowState.this.mClient, false);
                    Slog.i(WindowState.TAG, "WIN DEATH: " + win);
                    if (win != null) {
                        WindowState.this.mService.removeWindowLocked(win, WindowState.this.shouldKeepVisibleDeadAppWindow());
                        if (win.mAttrs.type == 2034) {
                            TaskStack stack = WindowState.this.mService.mStackIdToStack.get(3);
                            if (stack != null) {
                                stack.resetDockedStackToMiddle();
                            }
                            WindowState.this.mService.setDockedStackResizing(false);
                        }
                    } else if (WindowState.this.mHasSurface) {
                        Slog.e(WindowState.TAG, "!!! LEAK !!! Window removed but surface still valid.");
                        WindowState.this.mService.removeWindowLocked(WindowState.this);
                    }
                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    boolean shouldKeepVisibleDeadAppWindow() {
        TaskStack stack;
        if (!isWinVisibleLw() || this.mAppToken == null || this.mAppToken.clientHidden || this.mAttrs.token != this.mClient.asBinder() || this.mAttrs.type == 3 || (stack = getStack()) == null) {
            return false;
        }
        return ActivityManager.StackId.keepVisibleDeadAppWindowOnScreen(stack.mStackId);
    }

    boolean canReceiveKeys() {
        if (isVisibleOrAdding() && this.mViewVisibility == 0 && !this.mRemoveOnExit && (this.mAttrs.flags & 8) == 0) {
            return (this.mAppToken == null || this.mAppToken.windowsAreFocusable()) && !isAdjustedForMinimizedDock();
        }
        return false;
    }

    public boolean hasDrawnLw() {
        return this.mWinAnimator.mDrawState == 4;
    }

    public boolean showLw(boolean doAnimation) {
        return showLw(doAnimation, true);
    }

    boolean showLw(boolean doAnimation, boolean requestAnim) {
        if (isHiddenFromUserLocked() || !this.mAppOpVisibility || this.mForceHideNonSystemOverlayWindow) {
            return false;
        }
        if (this.mPolicyVisibility && this.mPolicyVisibilityAfterAnim) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "Policy visibility true: " + this);
        }
        if (doAnimation) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG, "doAnimation: mPolicyVisibility=" + this.mPolicyVisibility + " mAnimation=" + this.mWinAnimator.mAnimation);
            }
            if (!this.mService.okToDisplay()) {
                doAnimation = false;
            } else if (this.mPolicyVisibility && this.mWinAnimator.mAnimation == null) {
                doAnimation = false;
            }
        }
        this.mPolicyVisibility = true;
        this.mPolicyVisibilityAfterAnim = true;
        if (doAnimation) {
            this.mWinAnimator.applyAnimationLocked(1, true);
        }
        if (requestAnim) {
            this.mService.scheduleAnimationLocked();
        }
        return true;
    }

    public boolean hideLw(boolean doAnimation) {
        return hideLw(doAnimation, true);
    }

    boolean hideLw(boolean doAnimation, boolean requestAnim) {
        if (doAnimation && !this.mService.okToDisplay()) {
            doAnimation = false;
        }
        boolean current = doAnimation ? this.mPolicyVisibilityAfterAnim : this.mPolicyVisibility;
        if (!current) {
            return false;
        }
        if (doAnimation) {
            this.mWinAnimator.applyAnimationLocked(2, false);
            if (this.mWinAnimator.mAnimation == null) {
                doAnimation = false;
            }
        }
        if (doAnimation) {
            this.mPolicyVisibilityAfterAnim = false;
        } else {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG, "Policy visibility false: " + this);
            }
            this.mPolicyVisibilityAfterAnim = false;
            this.mPolicyVisibility = false;
            this.mService.enableScreenIfNeededLocked();
            if (this.mService.mCurrentFocus == this) {
                if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                    Slog.i(TAG, "WindowState.hideLw: setting mFocusMayChange true");
                }
                this.mService.mFocusMayChange = true;
            }
        }
        if (requestAnim) {
            this.mService.scheduleAnimationLocked();
        }
        return true;
    }

    void setForceHideNonSystemOverlayWindowIfNeeded(boolean forceHide) {
        if (!this.mOwnerCanAddInternalSystemWindow) {
            if ((!WindowManager.LayoutParams.isSystemAlertWindowType(this.mAttrs.type) && this.mAttrs.type != 2005) || this.mForceHideNonSystemOverlayWindow == forceHide) {
                return;
            }
            this.mForceHideNonSystemOverlayWindow = forceHide;
            if (forceHide) {
                hideLw(true, true);
            } else {
                showLw(true, true);
            }
        }
    }

    public void setAppOpVisibilityLw(boolean state) {
        if (this.mAppOpVisibility == state) {
            return;
        }
        this.mAppOpVisibility = state;
        if (state) {
            showLw(true, true);
        } else {
            hideLw(true, true);
        }
    }

    public void pokeDrawLockLw(long timeout) {
        if (isVisibleOrAdding()) {
            if (this.mDrawLock == null) {
                CharSequence tag = getWindowTag();
                this.mDrawLock = this.mService.mPowerManager.newWakeLock(128, "Window:" + tag);
                this.mDrawLock.setReferenceCounted(false);
                this.mDrawLock.setWorkSource(new WorkSource(this.mOwnerUid, this.mAttrs.packageName));
            }
            if (WindowManagerDebugConfig.DEBUG_POWER) {
                Slog.d(TAG, "pokeDrawLock: poking draw lock on behalf of visible window owned by " + this.mAttrs.packageName);
            }
            this.mDrawLock.acquire(timeout);
            return;
        }
        if (!WindowManagerDebugConfig.DEBUG_POWER) {
            return;
        }
        Slog.d(TAG, "pokeDrawLock: suppressed draw lock request for invisible window owned by " + this.mAttrs.packageName);
    }

    public boolean isAlive() {
        return this.mClient.asBinder().isBinderAlive();
    }

    boolean isClosing() {
        if (this.mAnimatingExit) {
            return true;
        }
        return this.mService.mClosingApps.contains(this.mAppToken);
    }

    boolean isAnimatingWithSavedSurface() {
        return this.mAnimatingWithSavedSurface;
    }

    boolean isAnimatingInvisibleWithSavedSurface() {
        if (!this.mAnimatingWithSavedSurface) {
            return false;
        }
        if (this.mViewVisibility == 0) {
            return this.mWindowRemovalAllowed;
        }
        return true;
    }

    public void setVisibleBeforeClientHidden() {
        this.mWasVisibleBeforeClientHidden = (this.mViewVisibility != 0 ? this.mAnimatingWithSavedSurface : true) | this.mWasVisibleBeforeClientHidden;
    }

    public void clearVisibleBeforeClientHidden() {
        this.mWasVisibleBeforeClientHidden = false;
    }

    public boolean wasVisibleBeforeClientHidden() {
        return this.mWasVisibleBeforeClientHidden;
    }

    private boolean shouldSaveSurface() {
        Task task;
        if (this.mWinAnimator.mSurfaceController == null || !this.mWasVisibleBeforeClientHidden || (this.mAttrs.flags & PackageManagerService.DumpState.DUMP_PREFERRED_XML) != 0 || ActivityManager.isLowRamDeviceStatic() || (task = getTask()) == null || task.inHomeStack()) {
            return false;
        }
        AppWindowToken taskTop = task.getTopVisibleAppToken();
        if ((taskTop == null || taskTop == this.mAppToken) && !this.mResizedWhileGone) {
            return this.mAppToken.shouldSaveSurface();
        }
        return false;
    }

    void destroyOrSaveSurface() {
        this.mSurfaceSaved = shouldSaveSurface();
        if (this.mSurfaceSaved) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "Saving surface: " + this);
            }
            this.mSession.setTransparentRegion(this.mClient, sEmptyRegion);
            this.mWinAnimator.hide("saved surface");
            this.mWinAnimator.mDrawState = 0;
            setHasSurface(false);
            if (this.mWinAnimator.mSurfaceController != null) {
                this.mWinAnimator.mSurfaceController.disconnectInTransaction();
            }
            this.mAnimatingWithSavedSurface = false;
        } else {
            this.mWinAnimator.destroySurfaceLocked();
        }
        this.mAnimatingExit = false;
    }

    void destroySavedSurface() {
        if (this.mSurfaceSaved) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "Destroying saved surface: " + this);
            }
            this.mWinAnimator.destroySurfaceLocked();
        }
        this.mWasVisibleBeforeClientHidden = false;
    }

    void restoreSavedSurface() {
        if (!this.mSurfaceSaved) {
            return;
        }
        this.mSurfaceSaved = false;
        if (this.mWinAnimator.mSurfaceController != null) {
            setHasSurface(true);
            this.mWinAnimator.mDrawState = 3;
            this.mAnimatingWithSavedSurface = true;
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return;
            }
            Slog.v(TAG, "Restoring saved surface: " + this);
            return;
        }
        Slog.wtf(TAG, "Failed to restore saved surface: surface gone! " + this);
    }

    boolean canRestoreSurface() {
        if (this.mWasVisibleBeforeClientHidden) {
            return this.mSurfaceSaved;
        }
        return false;
    }

    boolean hasSavedSurface() {
        return this.mSurfaceSaved;
    }

    void clearHasSavedSurface() {
        this.mSurfaceSaved = false;
        this.mAnimatingWithSavedSurface = false;
        if (!this.mWasVisibleBeforeClientHidden) {
            return;
        }
        this.mAppToken.destroySavedSurfaces();
    }

    boolean clearAnimatingWithSavedSurface() {
        if (!this.mAnimatingWithSavedSurface) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.d(TAG, "clearAnimatingWithSavedSurface(): win=" + this);
        }
        this.mAnimatingWithSavedSurface = false;
        return true;
    }

    public boolean isDefaultDisplay() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return false;
        }
        return displayContent.isDefaultDisplay;
    }

    public boolean isDimming() {
        DimLayer.DimLayerUser dimLayerUser = getDimLayerUser();
        if (dimLayerUser == null || this.mDisplayContent == null) {
            return false;
        }
        return this.mDisplayContent.mDimLayerController.isDimming(dimLayerUser, this.mWinAnimator);
    }

    public void setShowToOwnerOnlyLocked(boolean showToOwnerOnly) {
        this.mShowToOwnerOnly = showToOwnerOnly;
    }

    boolean isHiddenFromUserLocked() {
        WindowState win = this;
        while (win.isChildWindow()) {
            win = win.mAttachedWindow;
        }
        return (win.mAttrs.type >= 2000 || win.mAppToken == null || !win.mAppToken.showForAllUsers || win.mFrame.left > win.mDisplayFrame.left || win.mFrame.top > win.mDisplayFrame.top || win.mFrame.right < win.mStableFrame.right || win.mFrame.bottom < win.mStableFrame.bottom) && win.mShowToOwnerOnly && !this.mService.isCurrentProfileLocked(UserHandle.getUserId(win.mOwnerUid));
    }

    private static void applyInsets(Region outRegion, Rect frame, Rect inset) {
        outRegion.set(frame.left + inset.left, frame.top + inset.top, frame.right - inset.right, frame.bottom - inset.bottom);
    }

    void getTouchableRegion(Region outRegion) {
        Rect frame = this.mFrame;
        switch (this.mTouchableInsets) {
            case 0:
            default:
                outRegion.set(frame);
                break;
            case 1:
                applyInsets(outRegion, frame, this.mGivenContentInsets);
                break;
            case 2:
                applyInsets(outRegion, frame, this.mGivenVisibleInsets);
                break;
            case 3:
                Region givenTouchableRegion = this.mGivenTouchableRegion;
                outRegion.set(givenTouchableRegion);
                outRegion.translate(frame.left, frame.top);
                break;
        }
        cropRegionToStackBoundsIfNeeded(outRegion);
    }

    void cropRegionToStackBoundsIfNeeded(Region region) {
        TaskStack stack;
        Task task = getTask();
        if (task == null || !task.cropWindowsToStackBounds() || (stack = task.mStack) == null) {
            return;
        }
        stack.getDimBounds(this.mTmpRect);
        region.op(this.mTmpRect, Region.Op.INTERSECT);
    }

    WindowList getWindowList() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return null;
        }
        return displayContent.getWindowList();
    }

    public void reportFocusChangedSerialized(boolean r6, boolean r7) {
        r5.mClient.windowFocusChanged(r6, r7);
        if (r5.mFocusCallbacks != null) {
            r0 = r5.mFocusCallbacks.beginBroadcast();
            r2 = 0;
            while (r2 < r0) {
                r3 = r5.mFocusCallbacks.getBroadcastItem(r2);
                if (r6) {
                    r3.focusGained(r5.mWindowId.asBinder());
                } else {
                    r3.focusLost(r5.mWindowId.asBinder());
                }
                while (true) {
                    r2 = r2 + 1;
                }
            }
            r5.mFocusCallbacks.finishBroadcast();
            return;
        } else {
            return;
        }
    }

    private Configuration updateConfiguration() {
        boolean configChanged = isConfigChanged();
        getMergedConfig(this.mMergedConfiguration);
        this.mConfigHasChanged = false;
        if ((WindowManagerDebugConfig.DEBUG_RESIZE || WindowManagerDebugConfig.DEBUG_ORIENTATION || WindowManagerDebugConfig.DEBUG_CONFIGURATION) && configChanged) {
            Slog.i(TAG, "Sending new config to window " + this + ":  / mergedConfig=" + this.mMergedConfiguration);
        }
        return this.mMergedConfiguration;
    }

    private void getMergedConfig(Configuration outConfig) {
        Configuration overrideConfig;
        if (this.mAppToken != null && this.mAppToken.mFrozenMergedConfig.size() > 0) {
            outConfig.setTo(this.mAppToken.mFrozenMergedConfig.peek());
            return;
        }
        Task task = getTask();
        if (task != null) {
            overrideConfig = task.mOverrideConfig;
        } else {
            overrideConfig = Configuration.EMPTY;
        }
        Configuration serviceConfig = this.mService.mCurConfiguration;
        outConfig.setTo(serviceConfig);
        if (overrideConfig == Configuration.EMPTY) {
            return;
        }
        outConfig.updateFrom(overrideConfig);
    }

    void reportResized() {
        Trace.traceBegin(32L, "wm.reportResized_" + getWindowTag());
        try {
            if (WindowManagerDebugConfig.DEBUG_RESIZE || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v(TAG, "Reporting new frame to " + this + ": " + this.mCompatFrame);
            }
            final Configuration configurationUpdateConfiguration = isConfigChanged() ? updateConfiguration() : null;
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION && this.mWinAnimator.mDrawState == 1) {
                Slog.i(TAG, "Resizing " + this + " WITH DRAW PENDING");
            }
            final Rect frame = this.mFrame;
            final Rect overscanInsets = this.mLastOverscanInsets;
            final Rect contentInsets = this.mLastContentInsets;
            final Rect visibleInsets = this.mLastVisibleInsets;
            final Rect stableInsets = this.mLastStableInsets;
            final Rect outsets = this.mLastOutsets;
            final boolean reportDraw = this.mWinAnimator.mDrawState == 1;
            if (this.mAttrs.type != 3 && (this.mClient instanceof IWindow.Stub)) {
                this.mService.mH.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            WindowState.this.dispatchResized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, outsets, reportDraw, configurationUpdateConfiguration);
                        } catch (RemoteException e) {
                        }
                    }
                });
            } else {
                dispatchResized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, outsets, reportDraw, configurationUpdateConfiguration);
            }
            if (this.mService.mAccessibilityController != null && getDisplayId() == 0) {
                this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
            }
            this.mOverscanInsetsChanged = false;
            this.mContentInsetsChanged = false;
            this.mVisibleInsetsChanged = false;
            this.mStableInsetsChanged = false;
            this.mOutsetsChanged = false;
            this.mResizedWhileNotDragResizingReported = true;
            this.mWinAnimator.mSurfaceResized = false;
        } catch (RemoteException e) {
            this.mOrientationChanging = false;
            this.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mService.mDisplayFreezeTime);
            Slog.w(TAG, "Failed to report 'resized' to the client of " + this + ", removing this window.");
            this.mService.mPendingRemove.add(this);
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
        Trace.traceEnd(32L);
    }

    Rect getBackdropFrame(Rect frame) {
        boolean zIsDragResizeChanged = !isDragResizing() ? isDragResizeChanged() : true;
        if (ActivityManager.StackId.useWindowFrameForBackdrop(getStackId()) || !zIsDragResizeChanged) {
            return frame;
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

    private void dispatchResized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, Rect outsets, boolean reportDraw, Configuration newConfig) throws RemoteException {
        this.mClient.resized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, outsets, reportDraw, newConfig, getBackdropFrame(frame), !isDragResizeChanged() ? this.mResizedWhileNotDragResizing : true, this.mPolicy.isNavBarForcedShownLw(this));
        this.mDragResizingChangeReported = true;
    }

    public void registerFocusObserver(IWindowFocusObserver observer) {
        synchronized (this.mService.mWindowMap) {
            if (this.mFocusCallbacks == null) {
                this.mFocusCallbacks = new RemoteCallbackList<>();
            }
            this.mFocusCallbacks.register(observer);
        }
    }

    public void unregisterFocusObserver(IWindowFocusObserver observer) {
        synchronized (this.mService.mWindowMap) {
            if (this.mFocusCallbacks != null) {
                this.mFocusCallbacks.unregister(observer);
            }
        }
    }

    public boolean isFocused() {
        boolean z;
        synchronized (this.mService.mWindowMap) {
            z = this.mService.mCurrentFocus == this;
        }
        return z;
    }

    boolean inFreeformWorkspace() {
        Task task = getTask();
        if (task != null) {
            return task.inFreeformWorkspace();
        }
        return false;
    }

    public boolean isInMultiWindowMode() {
        Task task = getTask();
        return (task == null || task.isFullscreen()) ? false : true;
    }

    boolean isDragResizeChanged() {
        return this.mDragResizing != computeDragResizing();
    }

    boolean isDragResizingChangeReported() {
        return this.mDragResizingChangeReported;
    }

    void resetDragResizingChangeReported() {
        this.mDragResizingChangeReported = false;
    }

    void setResizedWhileNotDragResizing(boolean resizedWhileNotDragResizing) {
        this.mResizedWhileNotDragResizing = resizedWhileNotDragResizing;
        this.mResizedWhileNotDragResizingReported = !resizedWhileNotDragResizing;
    }

    boolean isResizedWhileNotDragResizing() {
        return this.mResizedWhileNotDragResizing;
    }

    boolean isResizedWhileNotDragResizingReported() {
        return this.mResizedWhileNotDragResizingReported;
    }

    int getResizeMode() {
        return this.mResizeMode;
    }

    boolean computeDragResizing() {
        Task task = getTask();
        if (task == null || this.mAttrs.width != -1 || this.mAttrs.height != -1) {
            return false;
        }
        if (task.isDragResizing()) {
            return true;
        }
        return ((!this.mDisplayContent.mDividerControllerLocked.isResizing() && (this.mAppToken == null || this.mAppToken.mFrozenBounds.isEmpty())) || task.inFreeformWorkspace() || isGoneForLayoutLw()) ? false : true;
    }

    void setDragResizing() {
        int i;
        boolean resizing = computeDragResizing();
        if (resizing == this.mDragResizing) {
            return;
        }
        this.mDragResizing = resizing;
        Task task = getTask();
        if (task != null && task.isDragResizing()) {
            this.mResizeMode = task.getDragResizeMode();
            return;
        }
        if (this.mDragResizing && this.mDisplayContent.mDividerControllerLocked.isResizing()) {
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
        return this.mDragResizing && getResizeMode() == 1;
    }

    void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        int i = 0;
        TaskStack stack = getStack();
        pw.print(prefix);
        pw.print("mDisplayId=");
        pw.print(getDisplayId());
        if (stack != null) {
            pw.print(" stackId=");
            pw.print(stack.mStackId);
        }
        if (this.mNotOnAppsDisplay) {
            pw.print(" mNotOnAppsDisplay=");
            pw.print(this.mNotOnAppsDisplay);
        }
        pw.print(" mSession=");
        pw.print(this.mSession);
        pw.print(" mClient=");
        pw.println(this.mClient.asBinder());
        pw.print(prefix);
        pw.print("mOwnerUid=");
        pw.print(this.mOwnerUid);
        pw.print(" mShowToOwnerOnly=");
        pw.print(this.mShowToOwnerOnly);
        pw.print(" package=");
        pw.print(this.mAttrs.packageName);
        pw.print(" appop=");
        pw.println(AppOpsManager.opToName(this.mAppOp));
        pw.print(prefix);
        pw.print("mAttrs=");
        pw.println(this.mAttrs);
        pw.print(prefix);
        pw.print("Requested w=");
        pw.print(this.mRequestedWidth);
        pw.print(" h=");
        pw.print(this.mRequestedHeight);
        pw.print(" mLayoutSeq=");
        pw.println(this.mLayoutSeq);
        if (this.mRequestedWidth != this.mLastRequestedWidth || this.mRequestedHeight != this.mLastRequestedHeight) {
            pw.print(prefix);
            pw.print("LastRequested w=");
            pw.print(this.mLastRequestedWidth);
            pw.print(" h=");
            pw.println(this.mLastRequestedHeight);
        }
        if (isChildWindow() || this.mLayoutAttached) {
            pw.print(prefix);
            pw.print("mAttachedWindow=");
            pw.print(this.mAttachedWindow);
            pw.print(" mLayoutAttached=");
            pw.println(this.mLayoutAttached);
        }
        if (this.mIsImWindow || this.mIsWallpaper || this.mIsFloatingLayer) {
            pw.print(prefix);
            pw.print("mIsImWindow=");
            pw.print(this.mIsImWindow);
            pw.print(" mIsWallpaper=");
            pw.print(this.mIsWallpaper);
            pw.print(" mIsFloatingLayer=");
            pw.print(this.mIsFloatingLayer);
            pw.print(" mWallpaperVisible=");
            pw.println(this.mWallpaperVisible);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mBaseLayer=");
            pw.print(this.mBaseLayer);
            pw.print(" mSubLayer=");
            pw.print(this.mSubLayer);
            pw.print(" mAnimLayer=");
            pw.print(this.mLayer);
            pw.print("+");
            if (this.mTargetAppToken != null) {
                i = this.mTargetAppToken.mAppAnimator.animLayerAdjustment;
            } else if (this.mAppToken != null) {
                i = this.mAppToken.mAppAnimator.animLayerAdjustment;
            }
            pw.print(i);
            pw.print("=");
            pw.print(this.mWinAnimator.mAnimLayer);
            pw.print(" mLastLayer=");
            pw.println(this.mWinAnimator.mLastLayer);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mToken=");
            pw.println(this.mToken);
            pw.print(prefix);
            pw.print("mRootToken=");
            pw.println(this.mRootToken);
            if (this.mAppToken != null) {
                pw.print(prefix);
                pw.print("mAppToken=");
                pw.println(this.mAppToken);
                pw.print(prefix);
                pw.print(" isAnimatingWithSavedSurface()=");
                pw.print(isAnimatingWithSavedSurface());
                pw.print(" mAppDied=");
                pw.println(this.mAppDied);
            }
            if (this.mTargetAppToken != null) {
                pw.print(prefix);
                pw.print("mTargetAppToken=");
                pw.println(this.mTargetAppToken);
            }
            pw.print(prefix);
            pw.print("mViewVisibility=0x");
            pw.print(Integer.toHexString(this.mViewVisibility));
            pw.print(" mHaveFrame=");
            pw.print(this.mHaveFrame);
            pw.print(" mObscured=");
            pw.println(this.mObscured);
            pw.print(prefix);
            pw.print("mSeq=");
            pw.print(this.mSeq);
            pw.print(" mSystemUiVisibility=0x");
            pw.println(Integer.toHexString(this.mSystemUiVisibility));
        }
        if (!this.mPolicyVisibility || !this.mPolicyVisibilityAfterAnim || !this.mAppOpVisibility || this.mAttachedHidden) {
            pw.print(prefix);
            pw.print("mPolicyVisibility=");
            pw.print(this.mPolicyVisibility);
            pw.print(" mPolicyVisibilityAfterAnim=");
            pw.print(this.mPolicyVisibilityAfterAnim);
            pw.print(" mAppOpVisibility=");
            pw.print(this.mAppOpVisibility);
            pw.print(" mAttachedHidden=");
            pw.println(this.mAttachedHidden);
        }
        if (!this.mRelayoutCalled || this.mLayoutNeeded) {
            pw.print(prefix);
            pw.print("mRelayoutCalled=");
            pw.print(this.mRelayoutCalled);
            pw.print(" mLayoutNeeded=");
            pw.println(this.mLayoutNeeded);
        }
        if (this.mXOffset != 0 || this.mYOffset != 0) {
            pw.print(prefix);
            pw.print("Offsets x=");
            pw.print(this.mXOffset);
            pw.print(" y=");
            pw.println(this.mYOffset);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mGivenContentInsets=");
            this.mGivenContentInsets.printShortString(pw);
            pw.print(" mGivenVisibleInsets=");
            this.mGivenVisibleInsets.printShortString(pw);
            pw.println();
            if (this.mTouchableInsets != 0 || this.mGivenInsetsPending) {
                pw.print(prefix);
                pw.print("mTouchableInsets=");
                pw.print(this.mTouchableInsets);
                pw.print(" mGivenInsetsPending=");
                pw.println(this.mGivenInsetsPending);
                Region region = new Region();
                getTouchableRegion(region);
                pw.print(prefix);
                pw.print("touchable region=");
                pw.println(region);
            }
            pw.print(prefix);
            pw.print("mMergedConfiguration=");
            pw.println(this.mMergedConfiguration);
        }
        pw.print(prefix);
        pw.print("mHasSurface=");
        pw.print(this.mHasSurface);
        pw.print(" mShownPosition=");
        this.mShownPosition.printShortString(pw);
        pw.print(" isReadyForDisplay()=");
        pw.print(isReadyForDisplay());
        pw.print(" hasSavedSurface()=");
        pw.print(hasSavedSurface());
        pw.print(" canReceiveKeys()=");
        pw.print(canReceiveKeys());
        pw.print(" mWindowRemovalAllowed=");
        pw.println(this.mWindowRemovalAllowed);
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mFrame=");
            this.mFrame.printShortString(pw);
            pw.print(" last=");
            this.mLastFrame.printShortString(pw);
            pw.println();
        }
        if (this.mEnforceSizeCompat) {
            pw.print(prefix);
            pw.print("mCompatFrame=");
            this.mCompatFrame.printShortString(pw);
            pw.println();
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("Frames: containing=");
            this.mContainingFrame.printShortString(pw);
            pw.print(" parent=");
            this.mParentFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("    display=");
            this.mDisplayFrame.printShortString(pw);
            pw.print(" overscan=");
            this.mOverscanFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("    content=");
            this.mContentFrame.printShortString(pw);
            pw.print(" visible=");
            this.mVisibleFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("    decor=");
            this.mDecorFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("    outset=");
            this.mOutsetFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("Cur insets: overscan=");
            this.mOverscanInsets.printShortString(pw);
            pw.print(" content=");
            this.mContentInsets.printShortString(pw);
            pw.print(" visible=");
            this.mVisibleInsets.printShortString(pw);
            pw.print(" stable=");
            this.mStableInsets.printShortString(pw);
            pw.print(" surface=");
            this.mAttrs.surfaceInsets.printShortString(pw);
            pw.print(" outsets=");
            this.mOutsets.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("Lst insets: overscan=");
            this.mLastOverscanInsets.printShortString(pw);
            pw.print(" content=");
            this.mLastContentInsets.printShortString(pw);
            pw.print(" visible=");
            this.mLastVisibleInsets.printShortString(pw);
            pw.print(" stable=");
            this.mLastStableInsets.printShortString(pw);
            pw.print(" physical=");
            this.mLastOutsets.printShortString(pw);
            pw.print(" outset=");
            this.mLastOutsets.printShortString(pw);
            pw.println();
        }
        pw.print(prefix);
        pw.print(this.mWinAnimator);
        pw.println(":");
        this.mWinAnimator.dump(pw, prefix + "  ", dumpAll);
        if (this.mAnimatingExit || this.mRemoveOnExit || this.mDestroying || this.mRemoved) {
            pw.print(prefix);
            pw.print("mAnimatingExit=");
            pw.print(this.mAnimatingExit);
            pw.print(" mRemoveOnExit=");
            pw.print(this.mRemoveOnExit);
            pw.print(" mDestroying=");
            pw.print(this.mDestroying);
            pw.print(" mRemoved=");
            pw.println(this.mRemoved);
        }
        if (this.mOrientationChanging || this.mAppFreezing || this.mTurnOnScreen) {
            pw.print(prefix);
            pw.print("mOrientationChanging=");
            pw.print(this.mOrientationChanging);
            pw.print(" mAppFreezing=");
            pw.print(this.mAppFreezing);
            pw.print(" mTurnOnScreen=");
            pw.println(this.mTurnOnScreen);
        }
        if (this.mLastFreezeDuration != 0) {
            pw.print(prefix);
            pw.print("mLastFreezeDuration=");
            TimeUtils.formatDuration(this.mLastFreezeDuration, pw);
            pw.println();
        }
        if (this.mHScale != 1.0f || this.mVScale != 1.0f) {
            pw.print(prefix);
            pw.print("mHScale=");
            pw.print(this.mHScale);
            pw.print(" mVScale=");
            pw.println(this.mVScale);
        }
        if (this.mWallpaperX != -1.0f || this.mWallpaperY != -1.0f) {
            pw.print(prefix);
            pw.print("mWallpaperX=");
            pw.print(this.mWallpaperX);
            pw.print(" mWallpaperY=");
            pw.println(this.mWallpaperY);
        }
        if (this.mWallpaperXStep != -1.0f || this.mWallpaperYStep != -1.0f) {
            pw.print(prefix);
            pw.print("mWallpaperXStep=");
            pw.print(this.mWallpaperXStep);
            pw.print(" mWallpaperYStep=");
            pw.println(this.mWallpaperYStep);
        }
        if (this.mWallpaperDisplayOffsetX != Integer.MIN_VALUE || this.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            pw.print(prefix);
            pw.print("mWallpaperDisplayOffsetX=");
            pw.print(this.mWallpaperDisplayOffsetX);
            pw.print(" mWallpaperDisplayOffsetY=");
            pw.println(this.mWallpaperDisplayOffsetY);
        }
        if (this.mDrawLock != null) {
            pw.print(prefix);
            pw.println("mDrawLock=" + this.mDrawLock);
        }
        if (isDragResizing()) {
            pw.print(prefix);
            pw.println("isDragResizing=" + isDragResizing());
        }
        if (!computeDragResizing()) {
            return;
        }
        pw.print(prefix);
        pw.println("computeDragResizing=" + computeDragResizing());
    }

    boolean hideNonSystemOverlayWindowsWhenVisible() {
        if ((this.mAttrs.privateFlags & PackageManagerService.DumpState.DUMP_FROZEN) != 0) {
            return this.mSession.mCanHideNonSystemOverlayWindows;
        }
        return false;
    }

    String makeInputChannelName() {
        return Integer.toHexString(System.identityHashCode(this)) + " " + getWindowTag();
    }

    CharSequence getWindowTag() {
        CharSequence tag = this.mAttrs.getTitle();
        if (tag == null || tag.length() <= 0) {
            return this.mAttrs.packageName;
        }
        return tag;
    }

    public String toString() {
        CharSequence title = getWindowTag();
        if (this.mStringNameCache == null || this.mLastTitle != title || this.mWasExiting != this.mAnimatingExit) {
            this.mLastTitle = title;
            this.mWasExiting = this.mAnimatingExit;
            this.mStringNameCache = "Window{" + Integer.toHexString(System.identityHashCode(this)) + " u" + UserHandle.getUserId(this.mSession.mUid) + " " + this.mLastTitle + (this.mAnimatingExit ? " EXITING}" : "}");
        }
        return this.mStringNameCache;
    }

    void transformClipRectFromScreenToSurfaceSpace(Rect clipRect) {
        if (this.mHScale >= 0.0f) {
            clipRect.left = (int) (clipRect.left / this.mHScale);
            clipRect.right = (int) Math.ceil(clipRect.right / this.mHScale);
        }
        if (this.mVScale < 0.0f) {
            return;
        }
        clipRect.top = (int) (clipRect.top / this.mVScale);
        clipRect.bottom = (int) Math.ceil(clipRect.bottom / this.mVScale);
    }

    void applyGravityAndUpdateFrame(Rect containingFrame, Rect displayFrame) {
        boolean fitToDisplay;
        int w;
        int h;
        float x;
        float y;
        int pw = containingFrame.width();
        int ph = containingFrame.height();
        Task task = getTask();
        boolean nonFullscreenTask = isInMultiWindowMode();
        boolean noLimits = (this.mAttrs.flags & 512) != 0;
        if (task == null || !nonFullscreenTask) {
            fitToDisplay = true;
        } else {
            fitToDisplay = isChildWindow() && !noLimits;
        }
        if ((this.mAttrs.flags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0) {
            if (this.mAttrs.width < 0) {
                w = pw;
            } else if (this.mEnforceSizeCompat) {
                w = (int) ((this.mAttrs.width * this.mGlobalScale) + 0.5f);
            } else {
                w = this.mAttrs.width;
            }
            if (this.mAttrs.height < 0) {
                h = ph;
            } else if (this.mEnforceSizeCompat) {
                h = (int) ((this.mAttrs.height * this.mGlobalScale) + 0.5f);
            } else {
                h = this.mAttrs.height;
            }
        } else {
            if (this.mAttrs.width == -1) {
                w = pw;
            } else if (this.mEnforceSizeCompat) {
                w = (int) ((this.mRequestedWidth * this.mGlobalScale) + 0.5f);
            } else {
                w = this.mRequestedWidth;
            }
            if (this.mAttrs.height == -1) {
                h = ph;
            } else if (this.mEnforceSizeCompat) {
                h = (int) ((this.mRequestedHeight * this.mGlobalScale) + 0.5f);
            } else {
                h = this.mRequestedHeight;
            }
        }
        if (this.mEnforceSizeCompat) {
            x = this.mAttrs.x * this.mGlobalScale;
            y = this.mAttrs.y * this.mGlobalScale;
        } else {
            x = this.mAttrs.x;
            y = this.mAttrs.y;
        }
        if (nonFullscreenTask && !layoutInParentFrame()) {
            w = Math.min(w, pw);
            h = Math.min(h, ph);
        }
        Gravity.apply(this.mAttrs.gravity, w, h, containingFrame, (int) ((this.mAttrs.horizontalMargin * pw) + x), (int) ((this.mAttrs.verticalMargin * ph) + y), this.mFrame);
        if (MultiWindowManager.isSupported() && containingFrame != null && !containingFrame.contains(this.mFrame) && inFreeformWorkspace() && isChildWindow()) {
            if (WindowManagerService.localLOGV) {
                Slog.d(TAG, "[BMW]mFrame [" + this.mFrame + "] is outside, apply containingFrame[" + containingFrame + "]");
            }
            Gravity.applyDisplay(this.mAttrs.gravity, containingFrame, this.mFrame);
        }
        if (fitToDisplay) {
            Gravity.applyDisplay(this.mAttrs.gravity, displayFrame, this.mFrame);
        }
        this.mCompatFrame.set(this.mFrame);
        if (!this.mEnforceSizeCompat) {
            return;
        }
        this.mCompatFrame.scale(this.mInvGlobalScale);
    }

    boolean isChildWindow() {
        return this.mAttachedWindow != null;
    }

    boolean layoutInParentFrame() {
        return isChildWindow() && (this.mAttrs.privateFlags & PackageManagerService.DumpState.DUMP_INSTALLS) != 0;
    }

    void setReplacing(boolean animate) {
        if ((this.mAttrs.privateFlags & PackageManagerService.DumpState.DUMP_VERSION) != 0 || this.mAttrs.type == 3) {
            return;
        }
        this.mWillReplaceWindow = true;
        this.mReplacingWindow = null;
        this.mAnimateReplacingWindow = animate;
    }

    void resetReplacing() {
        this.mWillReplaceWindow = false;
        this.mReplacingWindow = null;
        this.mAnimateReplacingWindow = false;
    }

    void requestUpdateWallpaperIfNeeded() {
        if (this.mDisplayContent == null || (this.mAttrs.flags & PackageManagerService.DumpState.DUMP_DEXOPT) == 0) {
            return;
        }
        this.mDisplayContent.pendingLayoutChanges |= 4;
        this.mDisplayContent.layoutNeeded = true;
        this.mService.mWindowPlacerLocked.requestTraversal();
    }

    float translateToWindowX(float x) {
        float winX = x - this.mFrame.left;
        if (this.mEnforceSizeCompat) {
            return winX * this.mGlobalScale;
        }
        return winX;
    }

    float translateToWindowY(float y) {
        float winY = y - this.mFrame.top;
        if (this.mEnforceSizeCompat) {
            return winY * this.mGlobalScale;
        }
        return winY;
    }

    void transferDimToReplacement() {
        DimLayer.DimLayerUser dimLayerUser = getDimLayerUser();
        if (dimLayerUser == null || this.mDisplayContent == null) {
            return;
        }
        this.mDisplayContent.mDimLayerController.applyDim(dimLayerUser, this.mReplacingWindow.mWinAnimator, (this.mAttrs.flags & 2) != 0);
    }

    boolean shouldBeReplacedWithChildren() {
        return isChildWindow() || this.mAttrs.type == 2;
    }

    public boolean isFastStartingWindow() {
        if (this.mAttrs.type == 3) {
            return this.mAttrs.getTitle().equals("FastStarting");
        }
        return false;
    }
}
