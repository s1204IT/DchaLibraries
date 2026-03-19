package com.android.server.policy;

import android.app.StatusBarManager;
import android.os.Handler;
import android.os.SystemClock;
import android.view.WindowManagerPolicy;
import com.android.server.LocalServices;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.PrintWriter;

public class BarController {
    private static final boolean DEBUG = false;
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
    protected WindowManagerPolicy.WindowState mWin;
    private final Object mServiceAquireLock = new Object();
    private int mState = 0;
    protected final Handler mHandler = new Handler();

    public BarController(String tag, int transientFlag, int unhideFlag, int translucentFlag, int statusBarManagerId, int translucentWmFlag, int transparentFlag) {
        this.mTag = "BarController." + tag;
        this.mTransientFlag = transientFlag;
        this.mUnhideFlag = unhideFlag;
        this.mTranslucentFlag = translucentFlag;
        this.mStatusBarManagerId = statusBarManagerId;
        this.mTranslucentWmFlag = translucentWmFlag;
        this.mTransparentFlag = transparentFlag;
    }

    public void setWindow(WindowManagerPolicy.WindowState win) {
        this.mWin = win;
    }

    public void setShowTransparent(boolean transparent) {
        if (transparent == this.mShowTransparent) {
            return;
        }
        this.mShowTransparent = transparent;
        this.mSetUnHideFlagWhenNextTransparent = transparent;
        this.mNoAnimationOnNextShow = true;
    }

    public void showTransient() {
        if (this.mWin == null) {
            return;
        }
        setTransientBarState(1);
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

    public void adjustSystemUiVisibilityLw(int oldVis, int vis) {
        if (this.mWin != null && this.mTransientBarState == 2 && (this.mTransientFlag & vis) == 0) {
            setTransientBarState(3);
            setBarShowingLw(false);
        } else {
            if (this.mWin == null || (this.mUnhideFlag & oldVis) == 0 || (this.mUnhideFlag & vis) != 0) {
                return;
            }
            setBarShowingLw(true);
        }
    }

    public int applyTranslucentFlagLw(WindowManagerPolicy.WindowState win, int vis, int oldVis) {
        int vis2;
        if (this.mWin != null) {
            if (win != null && (win.getAttrs().privateFlags & 512) == 0) {
                int fl = PolicyControl.getWindowFlags(win, null);
                if ((this.mTranslucentWmFlag & fl) != 0) {
                    vis2 = vis | this.mTranslucentFlag;
                } else {
                    vis2 = vis & (~this.mTranslucentFlag);
                }
                if ((Integer.MIN_VALUE & fl) != 0) {
                    return vis2 | this.mTransparentFlag;
                }
                return vis2 & (~this.mTransparentFlag);
            }
            return ((~this.mTransparentFlag) & (((~this.mTranslucentFlag) & vis) | (this.mTranslucentFlag & oldVis))) | (this.mTransparentFlag & oldVis);
        }
        return vis;
    }

    public boolean setBarShowingLw(boolean show) {
        boolean change;
        if (this.mWin == null) {
            return false;
        }
        if (show && this.mTransientBarState == 3) {
            this.mPendingShow = true;
            return false;
        }
        boolean wasVis = this.mWin.isVisibleLw();
        boolean wasAnim = this.mWin.isAnimatingLw();
        if (show) {
            change = this.mWin.showLw((this.mNoAnimationOnNextShow || skipAnimation()) ? false : true);
        } else {
            change = this.mWin.hideLw((this.mNoAnimationOnNextShow || skipAnimation()) ? false : true);
        }
        this.mNoAnimationOnNextShow = false;
        int state = computeStateLw(wasVis, wasAnim, this.mWin, change);
        boolean stateChanged = updateStateLw(state);
        if (change) {
            return true;
        }
        return stateChanged;
    }

    protected boolean skipAnimation() {
        return false;
    }

    private int computeStateLw(boolean wasVis, boolean wasAnim, WindowManagerPolicy.WindowState win, boolean change) {
        if (win.isDrawnLw()) {
            boolean vis = win.isVisibleLw();
            boolean anim = win.isAnimatingLw();
            if (this.mState == 1 && !change && !vis) {
                return 2;
            }
            if (this.mState == 2 && vis) {
                return 0;
            }
            if (change) {
                return (wasVis && vis && !wasAnim && anim) ? 1 : 0;
            }
        }
        return this.mState;
    }

    private boolean updateStateLw(final int state) {
        if (state != this.mState) {
            this.mState = state;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    StatusBarManagerInternal statusbar = BarController.this.getStatusBarInternal();
                    if (statusbar == null) {
                        return;
                    }
                    statusbar.setWindowState(BarController.this.mStatusBarManagerId, state);
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

    public int updateVisibilityLw(boolean transientAllowed, int oldVis, int vis) {
        if (this.mWin == null) {
            return vis;
        }
        if (isTransientShowing() || isTransientShowRequested()) {
            if (transientAllowed) {
                vis |= this.mTransientFlag;
                if ((this.mTransientFlag & oldVis) == 0) {
                    vis |= this.mUnhideFlag;
                }
                setTransientBarState(2);
            } else {
                setTransientBarState(0);
            }
        }
        if (this.mShowTransparent) {
            vis |= this.mTransparentFlag;
            if (this.mSetUnHideFlagWhenNextTransparent) {
                vis |= this.mUnhideFlag;
                this.mSetUnHideFlagWhenNextTransparent = false;
            }
        }
        if (this.mTransientBarState != 0) {
            vis = (vis | this.mTransientFlag) & (-2);
        }
        if ((this.mTranslucentFlag & vis) != 0 || (this.mTranslucentFlag & oldVis) != 0 || ((vis | oldVis) & this.mTransparentFlag) != 0) {
            this.mLastTranslucent = SystemClock.uptimeMillis();
        }
        return vis;
    }

    private void setTransientBarState(int state) {
        if (this.mWin == null || state == this.mTransientBarState) {
            return;
        }
        if (this.mTransientBarState == 2 || state == 2) {
            this.mLastTranslucent = SystemClock.uptimeMillis();
        }
        this.mTransientBarState = state;
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

    private static String transientBarStateToString(int state) {
        if (state == 3) {
            return "TRANSIENT_BAR_HIDING";
        }
        if (state == 2) {
            return "TRANSIENT_BAR_SHOWING";
        }
        if (state == 1) {
            return "TRANSIENT_BAR_SHOW_REQUESTED";
        }
        if (state == 0) {
            return "TRANSIENT_BAR_NONE";
        }
        throw new IllegalArgumentException("Unknown state " + state);
    }

    public void dump(PrintWriter pw, String prefix) {
        if (this.mWin == null) {
            return;
        }
        pw.print(prefix);
        pw.println(this.mTag);
        pw.print(prefix);
        pw.print("  ");
        pw.print("mState");
        pw.print('=');
        pw.println(StatusBarManager.windowStateToString(this.mState));
        pw.print(prefix);
        pw.print("  ");
        pw.print("mTransientBar");
        pw.print('=');
        pw.println(transientBarStateToString(this.mTransientBarState));
    }
}
