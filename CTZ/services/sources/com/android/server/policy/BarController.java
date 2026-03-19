package com.android.server.policy;

import android.app.StatusBarManager;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import com.android.server.LocalServices;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.PrintWriter;

public class BarController {
    private static final boolean DEBUG = false;
    private static final int MSG_NAV_BAR_VISIBILITY_CHANGED = 1;
    private static final int TRANSIENT_BAR_HIDING = 3;
    private static final int TRANSIENT_BAR_NONE = 0;
    private static final int TRANSIENT_BAR_SHOWING = 2;
    private static final int TRANSIENT_BAR_SHOW_REQUESTED = 1;
    private static final int TRANSLUCENT_ANIMATION_DELAY_MS = 1000;
    private long mLastTranslucent;
    private boolean mNoAnimationOnNextShow;
    private boolean mPendingShow;
    private boolean mSetUnHideFlagWhenNextTransparent;
    private boolean mShowTransparent;
    protected StatusBarManagerInternal mStatusBarInternal;
    private final int mStatusBarManagerId;
    protected final String mTag;
    private int mTransientBarState;
    private final int mTransientFlag;
    private final int mTranslucentFlag;
    private final int mTranslucentWmFlag;
    private final int mTransparentFlag;
    private final int mUnhideFlag;
    private OnBarVisibilityChangedListener mVisibilityChangeListener;
    protected WindowManagerPolicy.WindowState mWin;
    private final Object mServiceAquireLock = new Object();
    private int mState = 0;
    private final Rect mContentFrame = new Rect();
    protected final Handler mHandler = new BarHandler();

    interface OnBarVisibilityChangedListener {
        void onBarVisibilityChanged(boolean z);
    }

    public BarController(String str, int i, int i2, int i3, int i4, int i5, int i6) {
        this.mTag = "BarController." + str;
        this.mTransientFlag = i;
        this.mUnhideFlag = i2;
        this.mTranslucentFlag = i3;
        this.mStatusBarManagerId = i4;
        this.mTranslucentWmFlag = i5;
        this.mTransparentFlag = i6;
    }

    public void setWindow(WindowManagerPolicy.WindowState windowState) {
        this.mWin = windowState;
    }

    public void setContentFrame(Rect rect) {
        this.mContentFrame.set(rect);
    }

    public void setShowTransparent(boolean z) {
        if (z != this.mShowTransparent) {
            this.mShowTransparent = z;
            this.mSetUnHideFlagWhenNextTransparent = z;
            this.mNoAnimationOnNextShow = true;
        }
    }

    public void showTransient() {
        if (this.mWin != null) {
            setTransientBarState(1);
        }
    }

    public boolean isTransientShowing() {
        return this.mTransientBarState == 2;
    }

    public boolean isTransientShowRequested() {
        return this.mTransientBarState == 1;
    }

    public boolean wasRecentlyTranslucent() {
        return SystemClock.uptimeMillis() - this.mLastTranslucent < 1000;
    }

    public void adjustSystemUiVisibilityLw(int i, int i2) {
        if (this.mWin != null && this.mTransientBarState == 2 && (this.mTransientFlag & i2) == 0) {
            setTransientBarState(3);
            setBarShowingLw(false);
        } else if (this.mWin != null && (i & this.mUnhideFlag) != 0 && (this.mUnhideFlag & i2) == 0) {
            setBarShowingLw(true);
        }
    }

    public int applyTranslucentFlagLw(WindowManagerPolicy.WindowState windowState, int i, int i2) {
        int i3;
        int i4;
        if (this.mWin != null) {
            if (windowState != null && (windowState.getAttrs().privateFlags & 512) == 0) {
                int windowFlags = PolicyControl.getWindowFlags(windowState, null);
                if ((this.mTranslucentWmFlag & windowFlags) != 0) {
                    i3 = i | this.mTranslucentFlag;
                } else {
                    i3 = i & (~this.mTranslucentFlag);
                }
                if ((windowFlags & Integer.MIN_VALUE) != 0 && isTransparentAllowed(windowState)) {
                    i4 = this.mTransparentFlag | i3;
                } else {
                    i4 = (~this.mTransparentFlag) & i3;
                }
                return i4;
            }
            return (this.mTransparentFlag & i2) | ((((~this.mTranslucentFlag) & i) | (this.mTranslucentFlag & i2)) & (~this.mTransparentFlag));
        }
        return i;
    }

    boolean isTransparentAllowed(WindowManagerPolicy.WindowState windowState) {
        return windowState == null || !windowState.isLetterboxedOverlappingWith(this.mContentFrame);
    }

    public boolean setBarShowingLw(boolean z) {
        boolean zHideLw;
        if (this.mWin == null) {
            return false;
        }
        if (z && this.mTransientBarState == 3) {
            this.mPendingShow = true;
            return false;
        }
        boolean zIsVisibleLw = this.mWin.isVisibleLw();
        boolean zIsAnimatingLw = this.mWin.isAnimatingLw();
        if (z) {
            zHideLw = this.mWin.showLw((this.mNoAnimationOnNextShow || skipAnimation()) ? false : true);
        } else {
            zHideLw = this.mWin.hideLw((this.mNoAnimationOnNextShow || skipAnimation()) ? false : true);
        }
        this.mNoAnimationOnNextShow = false;
        boolean zUpdateStateLw = updateStateLw(computeStateLw(zIsVisibleLw, zIsAnimatingLw, this.mWin, zHideLw));
        if (zHideLw && this.mVisibilityChangeListener != null) {
            this.mHandler.obtainMessage(1, z ? 1 : 0, 0).sendToTarget();
        }
        return zHideLw || zUpdateStateLw;
    }

    void setOnBarVisibilityChangedListener(OnBarVisibilityChangedListener onBarVisibilityChangedListener, boolean z) {
        this.mVisibilityChangeListener = onBarVisibilityChangedListener;
        if (z) {
            this.mHandler.obtainMessage(1, this.mState == 0 ? 1 : 0, 0).sendToTarget();
        }
    }

    protected boolean skipAnimation() {
        return false;
    }

    private int computeStateLw(boolean z, boolean z2, WindowManagerPolicy.WindowState windowState, boolean z3) {
        if (windowState.isDrawnLw()) {
            boolean zIsVisibleLw = windowState.isVisibleLw();
            boolean zIsAnimatingLw = windowState.isAnimatingLw();
            if (this.mState == 1 && !z3 && !zIsVisibleLw) {
                return 2;
            }
            if (this.mState == 2 && zIsVisibleLw) {
                return 0;
            }
            if (z3) {
                return (z && zIsVisibleLw && !z2 && zIsAnimatingLw) ? 1 : 0;
            }
        }
        return this.mState;
    }

    private boolean updateStateLw(final int i) {
        if (i != this.mState) {
            this.mState = i;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    StatusBarManagerInternal statusBarInternal = BarController.this.getStatusBarInternal();
                    if (statusBarInternal != null) {
                        statusBarInternal.setWindowState(BarController.this.mStatusBarManagerId, i);
                    }
                }
            });
            return true;
        }
        return false;
    }

    public boolean checkHiddenLw() {
        if (this.mWin != null && this.mWin.isDrawnLw()) {
            if (!this.mWin.isVisibleLw() && !this.mWin.isAnimatingLw()) {
                updateStateLw(2);
            }
            if (this.mTransientBarState == 3 && !this.mWin.isVisibleLw()) {
                setTransientBarState(0);
                if (this.mPendingShow) {
                    setBarShowingLw(true);
                    this.mPendingShow = false;
                }
                return true;
            }
        }
        return false;
    }

    public boolean checkShowTransientBarLw() {
        return (this.mTransientBarState == 2 || this.mTransientBarState == 1 || this.mWin == null || this.mWin.isDisplayedLw()) ? false : true;
    }

    public int updateVisibilityLw(boolean z, int i, int i2) {
        if (this.mWin == null) {
            return i2;
        }
        if (isTransientShowing() || isTransientShowRequested()) {
            if (z) {
                int i3 = this.mTransientFlag | i2;
                if ((this.mTransientFlag & i) == 0) {
                    i3 |= this.mUnhideFlag;
                }
                i2 = i3;
                setTransientBarState(2);
            } else {
                setTransientBarState(0);
            }
        }
        if (this.mShowTransparent) {
            i2 |= this.mTransparentFlag;
            if (this.mSetUnHideFlagWhenNextTransparent) {
                i2 |= this.mUnhideFlag;
                this.mSetUnHideFlagWhenNextTransparent = false;
            }
        }
        if (this.mTransientBarState != 0) {
            i2 = (this.mTransientFlag | i2) & (-2);
        }
        if ((this.mTranslucentFlag & i2) != 0 || (this.mTranslucentFlag & i) != 0 || ((i2 | i) & this.mTransparentFlag) != 0) {
            this.mLastTranslucent = SystemClock.uptimeMillis();
        }
        return i2;
    }

    private void setTransientBarState(int i) {
        if (this.mWin != null && i != this.mTransientBarState) {
            if (this.mTransientBarState == 2 || i == 2) {
                this.mLastTranslucent = SystemClock.uptimeMillis();
            }
            this.mTransientBarState = i;
        }
    }

    protected StatusBarManagerInternal getStatusBarInternal() {
        StatusBarManagerInternal statusBarManagerInternal;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarInternal == null) {
                this.mStatusBarInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            }
            statusBarManagerInternal = this.mStatusBarInternal;
        }
        return statusBarManagerInternal;
    }

    private static String transientBarStateToString(int i) {
        if (i == 3) {
            return "TRANSIENT_BAR_HIDING";
        }
        if (i == 2) {
            return "TRANSIENT_BAR_SHOWING";
        }
        if (i == 1) {
            return "TRANSIENT_BAR_SHOW_REQUESTED";
        }
        if (i == 0) {
            return "TRANSIENT_BAR_NONE";
        }
        throw new IllegalArgumentException("Unknown state " + i);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1159641169921L, this.mState);
        protoOutputStream.write(1159641169922L, this.mTransientBarState);
        protoOutputStream.end(jStart);
    }

    public void dump(PrintWriter printWriter, String str) {
        if (this.mWin != null) {
            printWriter.print(str);
            printWriter.println(this.mTag);
            printWriter.print(str);
            printWriter.print("  ");
            printWriter.print("mState");
            printWriter.print('=');
            printWriter.println(StatusBarManager.windowStateToString(this.mState));
            printWriter.print(str);
            printWriter.print("  ");
            printWriter.print("mTransientBar");
            printWriter.print('=');
            printWriter.println(transientBarStateToString(this.mTransientBarState));
            printWriter.print(str);
            printWriter.print("  mContentFrame=");
            printWriter.println(this.mContentFrame);
        }
    }

    private class BarHandler extends Handler {
        private BarHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                boolean z = message.arg1 != 0;
                if (BarController.this.mVisibilityChangeListener != null) {
                    BarController.this.mVisibilityChangeListener.onBarVisibilityChanged(z);
                }
            }
        }
    }
}
