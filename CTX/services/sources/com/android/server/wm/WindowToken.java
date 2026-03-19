package com.android.server.wm;

import android.content.res.Configuration;
import android.os.Debug;
import android.os.IBinder;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.server.pm.DumpState;
import java.io.PrintWriter;
import java.util.Comparator;

public class WindowToken extends WindowContainer<WindowState> {
    private static final String TAG = "WindowManager";
    boolean hasVisible;
    protected DisplayContent mDisplayContent;
    private boolean mHidden;
    final boolean mOwnerCanManageAppTokens;
    boolean mPersistOnEmpty;
    final boolean mRoundedCornerOverlay;
    private final Comparator<WindowState> mWindowComparator;
    boolean paused;
    boolean sendingToBottom;
    String stringName;
    final IBinder token;
    boolean waitingToShow;
    final int windowType;

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
    public void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        super.onAnimationLeashCreated(transaction, surfaceControl);
    }

    @Override
    public void onAnimationLeashDestroyed(SurfaceControl.Transaction transaction) {
        super.onAnimationLeashDestroyed(transaction);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        super.onOverrideConfigurationChanged(configuration);
    }

    public static int lambda$new$0(WindowToken windowToken, WindowState windowState, WindowState windowState2) {
        if (windowState.mToken != windowToken) {
            throw new IllegalArgumentException("newWindow=" + windowState + " is not a child of token=" + windowToken);
        }
        if (windowState2.mToken == windowToken) {
            return windowToken.isFirstChildWindowGreaterThanSecond(windowState, windowState2) ? 1 : -1;
        }
        throw new IllegalArgumentException("existingWindow=" + windowState2 + " is not a child of token=" + windowToken);
    }

    WindowToken(WindowManagerService windowManagerService, IBinder iBinder, int i, boolean z, DisplayContent displayContent, boolean z2) {
        this(windowManagerService, iBinder, i, z, displayContent, z2, false);
    }

    WindowToken(WindowManagerService windowManagerService, IBinder iBinder, int i, boolean z, DisplayContent displayContent, boolean z2, boolean z3) {
        super(windowManagerService);
        this.paused = false;
        this.mWindowComparator = new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return WindowToken.lambda$new$0(this.f$0, (WindowState) obj, (WindowState) obj2);
            }
        };
        this.token = iBinder;
        this.windowType = i;
        this.mPersistOnEmpty = z;
        this.mOwnerCanManageAppTokens = z2;
        this.mRoundedCornerOverlay = z3;
        onDisplayChanged(displayContent);
    }

    void setHidden(boolean z) {
        if (z != this.mHidden) {
            this.mHidden = z;
        }
    }

    boolean isHidden() {
        return this.mHidden;
    }

    void removeAllWindowsIfPossible() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState windowState = (WindowState) this.mChildren.get(size);
            if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                Slog.w("WindowManager", "removeAllWindowsIfPossible: removing win=" + windowState);
            }
            windowState.removeIfPossible();
        }
    }

    void setExiting() {
        if (this.mChildren.size() == 0) {
            super.removeImmediately();
            return;
        }
        this.mPersistOnEmpty = false;
        if (this.mHidden) {
            return;
        }
        int size = this.mChildren.size();
        boolean zOnSetAppExiting = false;
        boolean z = false;
        for (int i = 0; i < size; i++) {
            WindowState windowState = (WindowState) this.mChildren.get(i);
            if (windowState.mWinAnimator.isAnimationSet()) {
                z = true;
            }
            zOnSetAppExiting |= windowState.onSetAppExiting();
        }
        setHidden(true);
        if (zOnSetAppExiting) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
            this.mService.updateFocusedWindowLocked(0, false);
        }
        if (z) {
            this.mDisplayContent.mExitingTokens.add(this);
        }
    }

    protected boolean isFirstChildWindowGreaterThanSecond(WindowState windowState, WindowState windowState2) {
        return windowState.mBaseLayer >= windowState2.mBaseLayer;
    }

    void addWindow(WindowState windowState) {
        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
            Slog.d("WindowManager", "addWindow: win=" + windowState + " Callers=" + Debug.getCallers(5));
        }
        if (!windowState.isChildWindow() && !this.mChildren.contains(windowState)) {
            if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v("WindowManager", "Adding " + windowState + " to " + this);
            }
            addChild(windowState, this.mWindowComparator);
            this.mService.mWindowsChanged = true;
        }
    }

    boolean isEmpty() {
        return this.mChildren.isEmpty();
    }

    WindowState getReplacingWindow() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState replacingWindow = ((WindowState) this.mChildren.get(size)).getReplacingWindow();
            if (replacingWindow != null) {
                return replacingWindow;
            }
        }
        return null;
    }

    boolean windowsCanBeWallpaperTarget() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if ((((WindowState) this.mChildren.get(size)).mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
                return true;
            }
        }
        return false;
    }

    int getHighestAnimLayer() {
        int i = -1;
        for (int i2 = 0; i2 < this.mChildren.size(); i2++) {
            int highestAnimLayer = ((WindowState) this.mChildren.get(i2)).getHighestAnimLayer();
            if (highestAnimLayer > i) {
                i = highestAnimLayer;
            }
        }
        return i;
    }

    AppWindowToken asAppWindowToken() {
        return null;
    }

    DisplayContent getDisplayContent() {
        return this.mDisplayContent;
    }

    @Override
    void removeImmediately() {
        if (this.mDisplayContent != null) {
            this.mDisplayContent.removeWindowToken(this.token);
        }
        super.removeImmediately();
    }

    @Override
    void onDisplayChanged(DisplayContent displayContent) {
        displayContent.reParentWindowToken(this);
        this.mDisplayContent = displayContent;
        if (this.mRoundedCornerOverlay) {
            this.mDisplayContent.reparentToOverlay(this.mPendingTransaction, this.mSurfaceControl);
        }
        super.onDisplayChanged(displayContent);
    }

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, z);
        protoOutputStream.write(1120986464258L, System.identityHashCode(this));
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((WindowState) this.mChildren.get(i)).writeToProto(protoOutputStream, 2246267895811L, z);
        }
        protoOutputStream.write(1133871366148L, this.mHidden);
        protoOutputStream.write(1133871366149L, this.waitingToShow);
        protoOutputStream.write(1133871366150L, this.paused);
        protoOutputStream.end(jStart);
    }

    @Override
    void dump(PrintWriter printWriter, String str, boolean z) {
        super.dump(printWriter, str, z);
        printWriter.print(str);
        printWriter.print("windows=");
        printWriter.println(this.mChildren);
        printWriter.print(str);
        printWriter.print("windowType=");
        printWriter.print(this.windowType);
        printWriter.print(" hidden=");
        printWriter.print(this.mHidden);
        printWriter.print(" hasVisible=");
        printWriter.println(this.hasVisible);
        if (this.waitingToShow || this.sendingToBottom) {
            printWriter.print(str);
            printWriter.print("waitingToShow=");
            printWriter.print(this.waitingToShow);
            printWriter.print(" sendingToBottom=");
            printWriter.print(this.sendingToBottom);
        }
    }

    public String toString() {
        if (this.stringName == null) {
            this.stringName = "WindowToken{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.token + '}';
        }
        return this.stringName;
    }

    @Override
    String getName() {
        return toString();
    }

    boolean okToDisplay() {
        return this.mDisplayContent != null && this.mDisplayContent.okToDisplay();
    }

    boolean okToAnimate() {
        return this.mDisplayContent != null && this.mDisplayContent.okToAnimate();
    }

    boolean canLayerAboveSystemBars() {
        return this.mOwnerCanManageAppTokens && this.mService.mPolicy.getWindowLayerFromTypeLw(this.windowType, this.mOwnerCanManageAppTokens) > this.mService.mPolicy.getWindowLayerFromTypeLw(2019, this.mOwnerCanManageAppTokens);
    }
}
