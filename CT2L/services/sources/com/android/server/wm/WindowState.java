package com.android.server.wm;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.IApplicationToken;
import android.view.IWindow;
import android.view.IWindowFocusObserver;
import android.view.IWindowId;
import android.view.InputChannel;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import com.android.server.am.ProcessList;
import com.android.server.input.InputWindowHandle;
import com.android.server.voiceinteraction.SoundTriggerHelper;
import java.io.PrintWriter;

final class WindowState implements WindowManagerPolicy.WindowState {
    static final String TAG = "WindowState";
    boolean mAppFreezing;
    final int mAppOp;
    AppWindowToken mAppToken;
    boolean mAttachedHidden;
    final WindowState mAttachedWindow;
    final int mBaseLayer;
    final IWindow mClient;
    private boolean mConfigHasChanged;
    boolean mContentChanged;
    boolean mContentInsetsChanged;
    final Context mContext;
    final DeathRecipient mDeathRecipient;
    boolean mDestroying;
    DisplayContent mDisplayContent;
    boolean mEnforceSizeCompat;
    boolean mExiting;
    RemoteCallbackList<IWindowFocusObserver> mFocusCallbacks;
    private boolean mForceHideNonSystemOverlayWindow;
    boolean mGivenInsetsPending;
    boolean mHaveFrame;
    InputChannel mInputChannel;
    final InputWindowHandle mInputWindowHandle;
    final boolean mIsFloatingLayer;
    final boolean mIsImWindow;
    final boolean mIsWallpaper;
    int mLastFreezeDuration;
    int mLastRequestedHeight;
    int mLastRequestedWidth;
    CharSequence mLastTitle;
    int mLayer;
    final boolean mLayoutAttached;
    boolean mLayoutNeeded;
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
    final WindowStateAnimator mWinAnimator;
    int mXOffset;
    int mYOffset;
    final WindowManager.LayoutParams mAttrs = new WindowManager.LayoutParams();
    final WindowList mChildWindows = new WindowList();
    boolean mPolicyVisibility = true;
    boolean mPolicyVisibilityAfterAnim = true;
    boolean mAppOpVisibility = true;
    int mLayoutSeq = -1;
    Configuration mConfiguration = null;
    final RectF mShownFrame = new RectF();
    final Rect mVisibleInsets = new Rect();
    final Rect mLastVisibleInsets = new Rect();
    final Rect mContentInsets = new Rect();
    final Rect mLastContentInsets = new Rect();
    final Rect mOverscanInsets = new Rect();
    final Rect mLastOverscanInsets = new Rect();
    final Rect mStableInsets = new Rect();
    final Rect mLastStableInsets = new Rect();
    final Rect mGivenContentInsets = new Rect();
    final Rect mGivenVisibleInsets = new Rect();
    final Region mGivenTouchableRegion = new Region();
    int mTouchableInsets = 0;
    final Rect mSystemDecorRect = new Rect();
    final Rect mLastSystemDecorRect = new Rect();
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
    float mWallpaperX = -1.0f;
    float mWallpaperY = -1.0f;
    float mWallpaperXStep = -1.0f;
    float mWallpaperYStep = -1.0f;
    int mWallpaperDisplayOffsetX = SoundTriggerHelper.STATUS_ERROR;
    int mWallpaperDisplayOffsetY = SoundTriggerHelper.STATUS_ERROR;
    boolean mHasSurface = false;
    boolean mUnderStatusBar = true;
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
        DeathRecipient deathRecipient = new DeathRecipient();
        this.mSeq = seq;
        this.mEnforceSizeCompat = (this.mAttrs.privateFlags & 128) != 0;
        try {
            c.asBinder().linkToDeath(deathRecipient, 0);
            this.mDeathRecipient = deathRecipient;
            if (this.mAttrs.type >= 1000 && this.mAttrs.type <= 1999) {
                this.mBaseLayer = (this.mPolicy.windowTypeToLayerLw(attachedWindow.mAttrs.type) * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 1000;
                this.mSubLayer = this.mPolicy.subWindowTypeToLayerLw(a.type);
                this.mAttachedWindow = attachedWindow;
                int children_size = this.mAttachedWindow.mChildWindows.size();
                if (children_size == 0) {
                    this.mAttachedWindow.mChildWindows.add(this);
                } else {
                    int i = 0;
                    while (true) {
                        if (i >= children_size) {
                            break;
                        }
                        WindowState child = this.mAttachedWindow.mChildWindows.get(i);
                        if (this.mSubLayer < child.mSubLayer) {
                            this.mAttachedWindow.mChildWindows.add(i, this);
                            break;
                        } else if (this.mSubLayer > child.mSubLayer || this.mBaseLayer > child.mBaseLayer) {
                            i++;
                        } else {
                            this.mAttachedWindow.mChildWindows.add(i, this);
                            break;
                        }
                    }
                    if (children_size == this.mAttachedWindow.mChildWindows.size()) {
                        this.mAttachedWindow.mChildWindows.add(this);
                    }
                }
                this.mLayoutAttached = this.mAttrs.type != 1003;
                this.mIsImWindow = attachedWindow.mAttrs.type == 2011 || attachedWindow.mAttrs.type == 2012;
                this.mIsWallpaper = attachedWindow.mAttrs.type == 2013;
                this.mIsFloatingLayer = this.mIsImWindow || this.mIsWallpaper;
            } else {
                this.mBaseLayer = (this.mPolicy.windowTypeToLayerLw(a.type) * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 1000;
                this.mSubLayer = 0;
                this.mAttachedWindow = null;
                this.mLayoutAttached = false;
                this.mIsImWindow = this.mAttrs.type == 2011 || this.mAttrs.type == 2012;
                this.mIsWallpaper = this.mAttrs.type == 2013;
                this.mIsFloatingLayer = this.mIsImWindow || this.mIsWallpaper;
            }
            WindowState appWin = this;
            while (appWin.mAttachedWindow != null) {
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
        this.mSession.windowAddedLocked();
    }

    public int getOwningUid() {
        return this.mOwnerUid;
    }

    public String getOwningPackage() {
        return this.mAttrs.packageName;
    }

    public void computeFrameLw(Rect pf, Rect df, Rect of, Rect cf, Rect vf, Rect dcf, Rect sf) {
        int w;
        int h;
        float x;
        float y;
        DisplayContent displayContent;
        this.mHaveFrame = true;
        TaskStack stack = this.mAppToken != null ? getStack() : null;
        if (stack != null && !stack.isFullscreen()) {
            getStackBounds(stack, this.mContainingFrame);
            if (this.mUnderStatusBar) {
                this.mContainingFrame.top = pf.top;
            }
        } else {
            this.mContainingFrame.set(pf);
        }
        this.mDisplayFrame.set(df);
        int pw = this.mContainingFrame.width();
        int ph = this.mContainingFrame.height();
        if ((this.mAttrs.flags & 16384) != 0) {
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
        int fw = this.mFrame.width();
        int fh = this.mFrame.height();
        if (this.mEnforceSizeCompat) {
            x = this.mAttrs.x * this.mGlobalScale;
            y = this.mAttrs.y * this.mGlobalScale;
        } else {
            x = this.mAttrs.x;
            y = this.mAttrs.y;
        }
        Gravity.apply(this.mAttrs.gravity, w, h, this.mContainingFrame, (int) ((this.mAttrs.horizontalMargin * pw) + x), (int) ((this.mAttrs.verticalMargin * ph) + y), this.mFrame);
        Gravity.applyDisplay(this.mAttrs.gravity, df, this.mFrame);
        this.mContentFrame.set(Math.max(this.mContentFrame.left, this.mFrame.left), Math.max(this.mContentFrame.top, this.mFrame.top), Math.min(this.mContentFrame.right, this.mFrame.right), Math.min(this.mContentFrame.bottom, this.mFrame.bottom));
        this.mVisibleFrame.set(Math.max(this.mVisibleFrame.left, this.mFrame.left), Math.max(this.mVisibleFrame.top, this.mFrame.top), Math.min(this.mVisibleFrame.right, this.mFrame.right), Math.min(this.mVisibleFrame.bottom, this.mFrame.bottom));
        this.mStableFrame.set(Math.max(this.mStableFrame.left, this.mFrame.left), Math.max(this.mStableFrame.top, this.mFrame.top), Math.min(this.mStableFrame.right, this.mFrame.right), Math.min(this.mStableFrame.bottom, this.mFrame.bottom));
        this.mOverscanInsets.set(Math.max(this.mOverscanFrame.left - this.mFrame.left, 0), Math.max(this.mOverscanFrame.top - this.mFrame.top, 0), Math.max(this.mFrame.right - this.mOverscanFrame.right, 0), Math.max(this.mFrame.bottom - this.mOverscanFrame.bottom, 0));
        this.mContentInsets.set(this.mContentFrame.left - this.mFrame.left, this.mContentFrame.top - this.mFrame.top, this.mFrame.right - this.mContentFrame.right, this.mFrame.bottom - this.mContentFrame.bottom);
        this.mVisibleInsets.set(this.mVisibleFrame.left - this.mFrame.left, this.mVisibleFrame.top - this.mFrame.top, this.mFrame.right - this.mVisibleFrame.right, this.mFrame.bottom - this.mVisibleFrame.bottom);
        this.mStableInsets.set(Math.max(this.mStableFrame.left - this.mFrame.left, 0), Math.max(this.mStableFrame.top - this.mFrame.top, 0), Math.max(this.mFrame.right - this.mStableFrame.right, 0), Math.max(this.mFrame.bottom - this.mStableFrame.bottom, 0));
        this.mCompatFrame.set(this.mFrame);
        if (this.mEnforceSizeCompat) {
            this.mOverscanInsets.scale(this.mInvGlobalScale);
            this.mContentInsets.scale(this.mInvGlobalScale);
            this.mVisibleInsets.scale(this.mInvGlobalScale);
            this.mStableInsets.scale(this.mInvGlobalScale);
            this.mCompatFrame.scale(this.mInvGlobalScale);
        }
        if (this.mIsWallpaper) {
            if ((fw != this.mFrame.width() || fh != this.mFrame.height()) && (displayContent = getDisplayContent()) != null) {
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                this.mService.updateWallpaperOffsetLocked(this, displayInfo.logicalWidth, displayInfo.logicalHeight, false);
            }
        }
    }

    public Rect getFrameLw() {
        return this.mFrame;
    }

    public RectF getShownFrameLw() {
        return this.mShownFrame;
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
        return this.mOverscanInsetsChanged || this.mContentInsetsChanged || this.mVisibleInsetsChanged;
    }

    public DisplayContent getDisplayContent() {
        if (this.mAppToken == null || this.mNotOnAppsDisplay) {
            return this.mDisplayContent;
        }
        TaskStack stack = getStack();
        return stack == null ? this.mDisplayContent : stack.getDisplayContent();
    }

    public int getDisplayId() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return -1;
        }
        return displayContent.getDisplayId();
    }

    TaskStack getStack() {
        AppWindowToken wtoken = this.mAppToken == null ? this.mService.mFocusedApp : this.mAppToken;
        if (wtoken != null) {
            Task task = this.mService.mTaskIdToTask.get(wtoken.groupId);
            if (task != null) {
                if (task.mStack != null) {
                    return task.mStack;
                }
                Slog.e(TAG, "getStack: mStack null for task=" + task);
            } else {
                Slog.e(TAG, "getStack: " + this + " couldn't find taskId=" + wtoken.groupId + " Callers=" + Debug.getCallers(4));
            }
        }
        return this.mDisplayContent.getHomeStack();
    }

    void getStackBounds(Rect bounds) {
        getStackBounds(getStack(), bounds);
    }

    private void getStackBounds(TaskStack stack, Rect bounds) {
        if (stack != null) {
            stack.getBounds(bounds);
        } else {
            bounds.set(this.mFrame);
        }
    }

    public long getInputDispatchingTimeoutNanos() {
        if (this.mAppToken != null) {
            return this.mAppToken.inputDispatchingTimeoutNanos;
        }
        return 5000000000L;
    }

    public boolean hasAppShownWindows() {
        return this.mAppToken != null && (this.mAppToken.firstWindowDrawn || this.mAppToken.startingDisplayed);
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

    public boolean isVisibleLw() {
        AppWindowToken atoken = this.mAppToken;
        return this.mHasSurface && this.mPolicyVisibility && !this.mAttachedHidden && !((atoken != null && atoken.hiddenRequested) || this.mExiting || this.mDestroying);
    }

    public boolean isVisibleOrBehindKeyguardLw() {
        boolean z = true;
        if (this.mRootToken.waitingToShow && this.mService.mAppTransition.isTransitionSet()) {
            return false;
        }
        AppWindowToken atoken = this.mAppToken;
        boolean animating = (atoken == null || atoken.mAppAnimator.animation == null) ? false : true;
        if (!this.mHasSurface || this.mDestroying || this.mExiting || (atoken != null ? atoken.hiddenRequested : !this.mPolicyVisibility) || ((this.mAttachedHidden || this.mViewVisibility != 0 || this.mRootToken.hidden) && this.mWinAnimator.mAnimation == null && !animating)) {
            z = false;
        }
        return z;
    }

    public boolean isWinVisibleLw() {
        AppWindowToken atoken = this.mAppToken;
        return this.mHasSurface && this.mPolicyVisibility && !this.mAttachedHidden && !((atoken != null && atoken.hiddenRequested && !atoken.mAppAnimator.animating) || this.mExiting || this.mDestroying);
    }

    boolean isVisibleNow() {
        return this.mHasSurface && this.mPolicyVisibility && !this.mAttachedHidden && !((this.mRootToken.hidden && this.mAttrs.type != 3) || this.mExiting || this.mDestroying);
    }

    boolean isPotentialDragTarget() {
        return (!isVisibleNow() || this.mRemoved || this.mInputChannel == null || this.mInputWindowHandle == null) ? false : true;
    }

    boolean isVisibleOrAdding() {
        AppWindowToken atoken = this.mAppToken;
        return (this.mHasSurface || (!this.mRelayoutCalled && this.mViewVisibility == 0)) && this.mPolicyVisibility && !this.mAttachedHidden && !((atoken != null && atoken.hiddenRequested) || this.mExiting || this.mDestroying);
    }

    boolean isOnScreen() {
        return this.mPolicyVisibility && isOnScreenIgnoringKeyguard();
    }

    boolean isOnScreenIgnoringKeyguard() {
        if (!this.mHasSurface || this.mDestroying) {
            return false;
        }
        AppWindowToken atoken = this.mAppToken;
        return atoken != null ? ((this.mAttachedHidden || atoken.hiddenRequested) && this.mWinAnimator.mAnimation == null && atoken.mAppAnimator.animation == null) ? false : true : (this.mAttachedHidden && this.mWinAnimator.mAnimation == null) ? false : true;
    }

    boolean isReadyForDisplay() {
        if (!(this.mRootToken.waitingToShow && this.mService.mAppTransition.isTransitionSet()) && this.mHasSurface && this.mPolicyVisibility && !this.mDestroying) {
            return ((this.mAttachedHidden || this.mViewVisibility != 0 || this.mRootToken.hidden) && this.mWinAnimator.mAnimation == null && (this.mAppToken == null || this.mAppToken.mAppAnimator.animation == null)) ? false : true;
        }
        return false;
    }

    boolean isReadyForDisplayIgnoringKeyguard() {
        if (this.mRootToken.waitingToShow && this.mService.mAppTransition.isTransitionSet()) {
            return false;
        }
        AppWindowToken atoken = this.mAppToken;
        if ((atoken != null || this.mPolicyVisibility) && this.mHasSurface && !this.mDestroying) {
            return ((this.mAttachedHidden || this.mViewVisibility != 0 || this.mRootToken.hidden) && this.mWinAnimator.mAnimation == null && (atoken == null || atoken.mAppAnimator.animation == null || this.mWinAnimator.isDummyAnimation())) ? false : true;
        }
        return false;
    }

    public boolean isDisplayedLw() {
        AppWindowToken atoken = this.mAppToken;
        return isDrawnLw() && this.mPolicyVisibility && ((!this.mAttachedHidden && (atoken == null || !atoken.hiddenRequested)) || this.mWinAnimator.mAnimating || !(atoken == null || atoken.mAppAnimator.animation == null));
    }

    public boolean isAnimatingLw() {
        return (this.mWinAnimator.mAnimation == null && (this.mAppToken == null || this.mAppToken.mAppAnimator.animation == null)) ? false : true;
    }

    public boolean isGoneForLayoutLw() {
        AppWindowToken atoken = this.mAppToken;
        return this.mViewVisibility == 8 || !this.mRelayoutCalled || (atoken == null && this.mRootToken.hidden) || ((atoken != null && (atoken.hiddenRequested || atoken.hidden)) || this.mAttachedHidden || ((this.mExiting && !isAnimatingLw()) || this.mDestroying));
    }

    public boolean isDrawFinishedLw() {
        return this.mHasSurface && !this.mDestroying && (this.mWinAnimator.mDrawState == 2 || this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4);
    }

    public boolean isDrawnLw() {
        return this.mHasSurface && !this.mDestroying && (this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4);
    }

    boolean isOpaqueDrawn() {
        return (this.mAttrs.format == -1 || this.mAttrs.type == 2013) && isDrawnLw() && this.mWinAnimator.mAnimation == null && (this.mAppToken == null || this.mAppToken.mAppAnimator.animation == null);
    }

    boolean shouldAnimateMove() {
        return this.mContentChanged && !this.mExiting && !this.mWinAnimator.mLastHidden && this.mService.okToDisplay() && !(this.mFrame.top == this.mLastFrame.top && this.mFrame.left == this.mLastFrame.left) && (this.mAttrs.privateFlags & 64) == 0 && (this.mAttachedWindow == null || !this.mAttachedWindow.shouldAnimateMove());
    }

    boolean isFullscreen(int screenWidth, int screenHeight) {
        return this.mFrame.left <= 0 && this.mFrame.top <= 0 && this.mFrame.right >= screenWidth && this.mFrame.bottom >= screenHeight;
    }

    boolean isConfigChanged() {
        boolean configChanged = this.mConfiguration != this.mService.mCurConfiguration && (this.mConfiguration == null || this.mConfiguration.diff(this.mService.mCurConfiguration) != 0);
        if ((this.mAttrs.privateFlags & 1024) != 0) {
            this.mConfigHasChanged |= configChanged;
            return this.mConfigHasChanged;
        }
        return configChanged;
    }

    void removeLocked() {
        disposeInputChannel();
        if (this.mAttachedWindow != null) {
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

    void setConfiguration(Configuration newConfig) {
        this.mConfiguration = newConfig;
        this.mConfigHasChanged = false;
    }

    void setInputChannel(InputChannel inputChannel) {
        if (this.mInputChannel != null) {
            throw new IllegalStateException("Window already has an input channel.");
        }
        this.mInputChannel = inputChannel;
        this.mInputWindowHandle.inputChannel = inputChannel;
    }

    void disposeInputChannel() {
        if (this.mInputChannel != null) {
            this.mService.mInputManager.unregisterInputChannel(this.mInputChannel);
            this.mInputChannel.dispose();
            this.mInputChannel = null;
        }
        this.mInputWindowHandle.inputChannel = null;
    }

    private class DeathRecipient implements IBinder.DeathRecipient {
        private DeathRecipient() {
        }

        @Override
        public void binderDied() {
            try {
                synchronized (WindowState.this.mService.mWindowMap) {
                    WindowState win = WindowState.this.mService.windowForClientLocked(WindowState.this.mSession, WindowState.this.mClient, false);
                    Slog.i(WindowState.TAG, "WIN DEATH: " + win);
                    if (win != null) {
                        WindowState.this.mService.removeWindowLocked(WindowState.this.mSession, win);
                    } else if (WindowState.this.mHasSurface) {
                        Slog.e(WindowState.TAG, "!!! LEAK !!! Window removed but surface still valid.");
                        WindowState.this.mService.removeWindowLocked(WindowState.this.mSession, WindowState.this);
                    }
                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public final boolean canReceiveKeys() {
        return isVisibleOrAdding() && this.mViewVisibility == 0 && (this.mAttrs.flags & 8) == 0;
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
        if (doAnimation) {
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
            this.mPolicyVisibilityAfterAnim = false;
            this.mPolicyVisibility = false;
            this.mService.enableScreenIfNeededLocked();
            if (this.mService.mCurrentFocus == this) {
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
            if ((WindowManager.LayoutParams.isSystemAlertWindowType(this.mAttrs.type) || this.mAttrs.type == 2005) && this.mForceHideNonSystemOverlayWindow != forceHide) {
                this.mForceHideNonSystemOverlayWindow = forceHide;
                if (forceHide) {
                    hideLw(true, true);
                } else {
                    showLw(true, true);
                }
            }
        }
    }

    public void setAppOpVisibilityLw(boolean state) {
        if (this.mAppOpVisibility != state) {
            this.mAppOpVisibility = state;
            if (state) {
                showLw(true, true);
            } else {
                hideLw(true, true);
            }
        }
    }

    public boolean isAlive() {
        return this.mClient.asBinder().isBinderAlive();
    }

    boolean isClosing() {
        return this.mExiting || this.mService.mClosingApps.contains(this.mAppToken);
    }

    public boolean isDefaultDisplay() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return false;
        }
        return displayContent.isDefaultDisplay;
    }

    public void setShowToOwnerOnlyLocked(boolean showToOwnerOnly) {
        this.mShowToOwnerOnly = showToOwnerOnly;
    }

    boolean isHiddenFromUserLocked() {
        WindowState win = this;
        while (win.mAttachedWindow != null) {
            win = win.mAttachedWindow;
        }
        if (win.mAttrs.type < 2000 && win.mAppToken != null && win.mAppToken.showWhenLocked) {
            DisplayContent displayContent = win.getDisplayContent();
            if (displayContent == null) {
                return true;
            }
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            if (win.mFrame.left <= 0 && win.mFrame.top <= 0 && win.mFrame.right >= displayInfo.appWidth && win.mFrame.bottom >= displayInfo.appHeight) {
                return false;
            }
        }
        return win.mShowToOwnerOnly && !this.mService.isCurrentProfileLocked(UserHandle.getUserId(win.mOwnerUid));
    }

    private static void applyInsets(Region outRegion, Rect frame, Rect inset) {
        outRegion.set(frame.left + inset.left, frame.top + inset.top, frame.right - inset.right, frame.bottom - inset.bottom);
    }

    public void getTouchableRegion(Region outRegion) {
        Rect frame = this.mFrame;
        switch (this.mTouchableInsets) {
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
            default:
                outRegion.set(frame);
                break;
        }
    }

    WindowList getWindowList() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return null;
        }
        return displayContent.getWindowList();
    }

    public void reportFocusChangedSerialized(boolean r5, boolean r6) {
        r4.mClient.windowFocusChanged(r5, r6);
        if (r4.mFocusCallbacks != null) {
            r0 = r4.mFocusCallbacks.beginBroadcast();
            r1 = 0;
            while (r1 < r0) {
                r2 = r4.mFocusCallbacks.getBroadcastItem(r1);
                if (r5) {
                    r2.focusGained(r4.mWindowId.asBinder());
                } else {
                    r2.focusLost(r4.mWindowId.asBinder());
                }
                while (true) {
                    r1 = r1 + 1;
                }
            }
            r4.mFocusCallbacks.finishBroadcast();
            return;
        } else {
            return;
        }
    }

    void reportResized() {
        try {
            boolean configChanged = isConfigChanged();
            setConfiguration(this.mService.mCurConfiguration);
            final Rect frame = this.mFrame;
            final Rect overscanInsets = this.mLastOverscanInsets;
            final Rect contentInsets = this.mLastContentInsets;
            final Rect visibleInsets = this.mLastVisibleInsets;
            final Rect stableInsets = this.mLastStableInsets;
            final boolean reportDraw = this.mWinAnimator.mDrawState == 1;
            final Configuration newConfig = configChanged ? this.mConfiguration : null;
            if (this.mAttrs.type != 3 && (this.mClient instanceof IWindow.Stub)) {
                this.mService.mH.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            WindowState.this.mClient.resized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, reportDraw, newConfig);
                        } catch (RemoteException e) {
                        }
                    }
                });
            } else {
                this.mClient.resized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, reportDraw, newConfig);
            }
            if (this.mService.mAccessibilityController != null && getDisplayId() == 0) {
                this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
            }
            this.mOverscanInsetsChanged = false;
            this.mContentInsetsChanged = false;
            this.mVisibleInsetsChanged = false;
            this.mStableInsetsChanged = false;
            this.mWinAnimator.mSurfaceResized = false;
        } catch (RemoteException e) {
            this.mOrientationChanging = false;
            this.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mService.mDisplayFreezeTime);
        }
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

    void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        int i;
        pw.print(prefix);
        pw.print("mDisplayId=");
        pw.print(getDisplayId());
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
        if (this.mAttachedWindow != null || this.mLayoutAttached) {
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
            } else {
                i = this.mAppToken != null ? this.mAppToken.mAppAnimator.animLayerAdjustment : 0;
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
            pw.print("mConfiguration=");
            pw.println(this.mConfiguration);
        }
        pw.print(prefix);
        pw.print("mHasSurface=");
        pw.print(this.mHasSurface);
        pw.print(" mShownFrame=");
        this.mShownFrame.printShortString(pw);
        pw.print(" isReadyForDisplay()=");
        pw.println(isReadyForDisplay());
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mFrame=");
            this.mFrame.printShortString(pw);
            pw.print(" last=");
            this.mLastFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("mSystemDecorRect=");
            this.mSystemDecorRect.printShortString(pw);
            pw.print(" last=");
            this.mLastSystemDecorRect.printShortString(pw);
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
            pw.print("Cur insets: overscan=");
            this.mOverscanInsets.printShortString(pw);
            pw.print(" content=");
            this.mContentInsets.printShortString(pw);
            pw.print(" visible=");
            this.mVisibleInsets.printShortString(pw);
            pw.print(" stable=");
            this.mStableInsets.printShortString(pw);
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
            pw.println();
        }
        pw.print(prefix);
        pw.print(this.mWinAnimator);
        pw.println(":");
        this.mWinAnimator.dump(pw, prefix + "  ", dumpAll);
        if (this.mExiting || this.mRemoveOnExit || this.mDestroying || this.mRemoved) {
            pw.print(prefix);
            pw.print("mExiting=");
            pw.print(this.mExiting);
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
    }

    boolean hideNonSystemOverlayWindowsWhenVisible() {
        return (this.mAttrs.privateFlags & 524288) != 0 && this.mSession.mCanHideNonSystemOverlayWindows;
    }

    String makeInputChannelName() {
        return Integer.toHexString(System.identityHashCode(this)) + " " + ((Object) this.mAttrs.getTitle());
    }

    public String toString() {
        CharSequence title = this.mAttrs.getTitle();
        if (title == null || title.length() <= 0) {
            title = this.mAttrs.packageName;
        }
        if (this.mStringNameCache == null || this.mLastTitle != title || this.mWasExiting != this.mExiting) {
            this.mLastTitle = title;
            this.mWasExiting = this.mExiting;
            this.mStringNameCache = "Window{" + Integer.toHexString(System.identityHashCode(this)) + " u" + UserHandle.getUserId(this.mSession.mUid) + " " + ((Object) this.mLastTitle) + (this.mExiting ? " EXITING}" : "}");
        }
        return this.mStringNameCache;
    }
}
