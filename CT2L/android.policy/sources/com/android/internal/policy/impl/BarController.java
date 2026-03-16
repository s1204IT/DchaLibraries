package com.android.internal.policy.impl;

import android.app.StatusBarManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.view.WindowManagerPolicy;
import com.android.internal.statusbar.IStatusBarService;
import java.io.PrintWriter;

public class BarController {
    private static final boolean DEBUG = false;
    private static final int TRANSIENT_BAR_HIDING = 3;
    private static final int TRANSIENT_BAR_NONE = 0;
    private static final int TRANSIENT_BAR_SHOWING = 2;
    private static final int TRANSIENT_BAR_SHOW_REQUESTED = 1;
    private static final int TRANSLUCENT_ANIMATION_DELAY_MS = 1000;
    private long mLastTranslucent;
    private boolean mPendingShow;
    private final int mStatusBarManagerId;
    private IStatusBarService mStatusBarService;
    private final String mTag;
    private int mTransientBarState;
    private final int mTransientFlag;
    private final int mTranslucentFlag;
    private final int mTranslucentWmFlag;
    private final int mUnhideFlag;
    private WindowManagerPolicy.WindowState mWin;
    private final Object mServiceAquireLock = new Object();
    private int mState = TRANSIENT_BAR_NONE;
    private final Handler mHandler = new Handler();

    public BarController(String tag, int transientFlag, int unhideFlag, int translucentFlag, int statusBarManagerId, int translucentWmFlag) {
        this.mTag = "BarController." + tag;
        this.mTransientFlag = transientFlag;
        this.mUnhideFlag = unhideFlag;
        this.mTranslucentFlag = translucentFlag;
        this.mStatusBarManagerId = statusBarManagerId;
        this.mTranslucentWmFlag = translucentWmFlag;
    }

    public void setWindow(WindowManagerPolicy.WindowState win) {
        this.mWin = win;
    }

    public void showTransient() {
        if (this.mWin != null) {
            setTransientBarState(1);
        }
    }

    public boolean isTransientShowing() {
        if (this.mTransientBarState == 2) {
            return true;
        }
        return DEBUG;
    }

    public boolean isTransientShowRequested() {
        if (this.mTransientBarState == 1) {
            return true;
        }
        return DEBUG;
    }

    public boolean wasRecentlyTranslucent() {
        if (SystemClock.uptimeMillis() - this.mLastTranslucent < 1000) {
            return true;
        }
        return DEBUG;
    }

    public void adjustSystemUiVisibilityLw(int oldVis, int vis) {
        if (this.mWin != null && this.mTransientBarState == 2 && (this.mTransientFlag & vis) == 0) {
            setTransientBarState(3);
            setBarShowingLw(DEBUG);
        } else if (this.mWin != null && (this.mUnhideFlag & oldVis) != 0 && (this.mUnhideFlag & vis) == 0) {
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
                    vis2 = vis & (this.mTranslucentFlag ^ (-1));
                }
                if ((Integer.MIN_VALUE & fl) != 0) {
                    return vis2 | 32768;
                }
                return vis2 & (-32769);
            }
            return ((((this.mTranslucentFlag ^ (-1)) & vis) | (this.mTranslucentFlag & oldVis)) & (-32769)) | (oldVis & 32768);
        }
        return vis;
    }

    public boolean setBarShowingLw(boolean show) {
        if (this.mWin == null) {
            return DEBUG;
        }
        if (show && this.mTransientBarState == 3) {
            this.mPendingShow = true;
            return DEBUG;
        }
        boolean wasVis = this.mWin.isVisibleLw();
        boolean wasAnim = this.mWin.isAnimatingLw();
        boolean change = show ? this.mWin.showLw(true) : this.mWin.hideLw(true);
        int state = computeStateLw(wasVis, wasAnim, this.mWin, change);
        boolean stateChanged = updateStateLw(state);
        if (change || stateChanged) {
            return true;
        }
        return DEBUG;
    }

    private int computeStateLw(boolean wasVis, boolean wasAnim, WindowManagerPolicy.WindowState win, boolean change) {
        if (win.hasDrawnLw()) {
            boolean vis = win.isVisibleLw();
            boolean anim = win.isAnimatingLw();
            if (this.mState == 1 && !change && !vis) {
                return 2;
            }
            if (this.mState == 2 && vis) {
                return TRANSIENT_BAR_NONE;
            }
            if (change) {
                if (wasVis && vis && !wasAnim && anim) {
                    return 1;
                }
                return TRANSIENT_BAR_NONE;
            }
        }
        return this.mState;
    }

    private boolean updateStateLw(final int state) {
        if (state == this.mState) {
            return DEBUG;
        }
        this.mState = state;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    IStatusBarService statusbar = BarController.this.getStatusBarService();
                    if (statusbar != null) {
                        statusbar.setWindowState(BarController.this.mStatusBarManagerId, state);
                    }
                } catch (RemoteException e) {
                    BarController.this.mStatusBarService = null;
                }
            }
        });
        return true;
    }

    public boolean checkHiddenLw() {
        if (this.mWin != null && this.mWin.hasDrawnLw()) {
            if (!this.mWin.isVisibleLw() && !this.mWin.isAnimatingLw()) {
                updateStateLw(2);
            }
            if (this.mTransientBarState == 3 && !this.mWin.isVisibleLw()) {
                setTransientBarState(TRANSIENT_BAR_NONE);
                if (!this.mPendingShow) {
                    return true;
                }
                setBarShowingLw(true);
                this.mPendingShow = DEBUG;
                return true;
            }
        }
        return DEBUG;
    }

    public boolean checkShowTransientBarLw() {
        if (this.mTransientBarState == 2 || this.mTransientBarState == 1 || this.mWin == null || this.mWin.isDisplayedLw()) {
            return DEBUG;
        }
        return true;
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
                setTransientBarState(TRANSIENT_BAR_NONE);
            }
        }
        if (this.mTransientBarState != 0) {
            vis = (vis | this.mTransientFlag) & (-2);
        }
        if ((this.mTranslucentFlag & vis) != 0 || (this.mTranslucentFlag & oldVis) != 0 || ((vis | oldVis) & 4) != 0) {
            this.mLastTranslucent = SystemClock.uptimeMillis();
        }
        return vis;
    }

    private void setTransientBarState(int state) {
        if (this.mWin != null && state != this.mTransientBarState) {
            if (this.mTransientBarState == 2 || state == 2) {
                this.mLastTranslucent = SystemClock.uptimeMillis();
            }
            this.mTransientBarState = state;
        }
    }

    private IStatusBarService getStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
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
        if (this.mWin != null) {
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
}
