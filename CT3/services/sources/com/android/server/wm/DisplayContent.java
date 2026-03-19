package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import com.mediatek.multiwindow.MultiWindowManager;
import java.io.PrintWriter;
import java.util.ArrayList;

class DisplayContent {
    final boolean isDefaultDisplay;
    boolean layoutNeeded;
    boolean mDeferredRemoval;
    final DimLayerController mDimLayerController;
    private final Display mDisplay;
    private final int mDisplayId;
    boolean mDisplayScalingDisabled;
    final DockedStackDividerController mDividerControllerLocked;
    final WindowManagerService mService;
    TaskTapPointerEventListener mTapDetector;
    int pendingLayoutChanges;
    private final WindowList mWindows = new WindowList();
    int mInitialDisplayWidth = 0;
    int mInitialDisplayHeight = 0;
    int mInitialDisplayDensity = 0;
    float mInitialPhysicalXDpi = 0.0f;
    float mInitialPhysicalYDpi = 0.0f;
    int mBaseDisplayWidth = 0;
    int mBaseDisplayHeight = 0;
    int mBaseDisplayDensity = 0;
    private final DisplayInfo mDisplayInfo = new DisplayInfo();
    private final DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    Rect mBaseDisplayRect = new Rect();
    Rect mContentRect = new Rect();
    final ArrayList<WindowToken> mExitingTokens = new ArrayList<>();
    private final ArrayList<TaskStack> mStacks = new ArrayList<>();
    private TaskStack mHomeStack = null;
    Region mTouchExcludeRegion = new Region();
    Region mNonResizeableRegion = new Region();
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpRect2 = new Rect();
    private final Region mTmpRegion = new Region();
    final ArrayList<Task> mTmpTaskHistory = new ArrayList<>();
    final ArrayList<WindowState> mTapExcludedWindows = new ArrayList<>();

    DisplayContent(Display display, WindowManagerService service) {
        this.mDisplay = display;
        this.mDisplayId = display.getDisplayId();
        display.getDisplayInfo(this.mDisplayInfo);
        display.getMetrics(this.mDisplayMetrics);
        this.isDefaultDisplay = this.mDisplayId == 0;
        this.mService = service;
        initializeDisplayBaseInfo();
        this.mDividerControllerLocked = new DockedStackDividerController(service, this);
        this.mDimLayerController = new DimLayerController(this);
    }

    int getDisplayId() {
        return this.mDisplayId;
    }

    WindowList getWindowList() {
        return this.mWindows;
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

    DockedStackDividerController getDockedDividerController() {
        return this.mDividerControllerLocked;
    }

    public boolean hasAccess(int uid) {
        return this.mDisplay.hasAccess(uid);
    }

    public boolean isPrivate() {
        return (this.mDisplay.getFlags() & 4) != 0;
    }

    ArrayList<TaskStack> getStacks() {
        return this.mStacks;
    }

    ArrayList<Task> getTasks() {
        this.mTmpTaskHistory.clear();
        int numStacks = this.mStacks.size();
        for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
            this.mTmpTaskHistory.addAll(this.mStacks.get(stackNdx).getTasks());
        }
        return this.mTmpTaskHistory;
    }

    TaskStack getHomeStack() {
        if (this.mHomeStack == null && this.mDisplayId == 0) {
            Slog.e("WindowManager", "getHomeStack: Returning null from this=" + this);
        }
        return this.mHomeStack;
    }

    void updateDisplayInfo() {
        this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        for (int i = this.mStacks.size() - 1; i >= 0; i--) {
            this.mStacks.get(i).updateDisplayInfo(null);
        }
    }

    void initializeDisplayBaseInfo() {
        DisplayInfo newDisplayInfo = this.mService.mDisplayManagerInternal.getDisplayInfo(this.mDisplayId);
        if (newDisplayInfo != null) {
            this.mDisplayInfo.copyFrom(newDisplayInfo);
        }
        int i = this.mDisplayInfo.logicalWidth;
        this.mInitialDisplayWidth = i;
        this.mBaseDisplayWidth = i;
        int i2 = this.mDisplayInfo.logicalHeight;
        this.mInitialDisplayHeight = i2;
        this.mBaseDisplayHeight = i2;
        int i3 = this.mDisplayInfo.logicalDensityDpi;
        this.mInitialDisplayDensity = i3;
        this.mBaseDisplayDensity = i3;
        this.mBaseDisplayRect.set(0, 0, this.mBaseDisplayWidth, this.mBaseDisplayHeight);
        this.mInitialPhysicalXDpi = this.mDisplayInfo.physicalXDpi;
        this.mInitialPhysicalYDpi = this.mDisplayInfo.physicalYDpi;
    }

    void getLogicalDisplayRect(Rect out) {
        boolean rotated = true;
        int orientation = this.mDisplayInfo.rotation;
        if (orientation != 1 && orientation != 3) {
            rotated = false;
        }
        int physWidth = rotated ? this.mBaseDisplayHeight : this.mBaseDisplayWidth;
        int physHeight = rotated ? this.mBaseDisplayWidth : this.mBaseDisplayHeight;
        int width = this.mDisplayInfo.logicalWidth;
        int left = (physWidth - width) / 2;
        int height = this.mDisplayInfo.logicalHeight;
        int top = (physHeight - height) / 2;
        out.set(left, top, left + width, top + height);
    }

    void getContentRect(Rect out) {
        out.set(this.mContentRect);
    }

    void attachStack(TaskStack stack, boolean onTop) {
        if (stack.mStackId == 0) {
            if (this.mHomeStack != null) {
                throw new IllegalArgumentException("attachStack: HOME_STACK_ID (0) not first.");
            }
            this.mHomeStack = stack;
        }
        if (onTop) {
            this.mStacks.add(stack);
        } else {
            this.mStacks.add(0, stack);
        }
        this.layoutNeeded = true;
    }

    void moveStack(TaskStack stack, boolean toTop) {
        if (ActivityManager.StackId.isAlwaysOnTop(stack.mStackId) && !toTop) {
            Slog.w("WindowManager", "Ignoring move of always-on-top stack=" + stack + " to bottom");
            return;
        }
        if (!this.mStacks.remove(stack)) {
            Slog.wtf("WindowManager", "moving stack that was not added: " + stack, new Throwable());
        }
        int addIndex = toTop ? this.mStacks.size() : 0;
        if (toTop && this.mService.isStackVisibleLocked(4) && stack.mStackId != 4) {
            addIndex--;
            TaskStack topStack = this.mStacks.get(addIndex);
            if (topStack.mStackId != 4) {
                throw new IllegalStateException("Pinned stack isn't top stack??? " + this.mStacks);
            }
        }
        this.mStacks.add(addIndex, stack);
    }

    void detachStack(TaskStack stack) {
        this.mDimLayerController.removeDimLayerUser(stack);
        this.mStacks.remove(stack);
    }

    void resize(Rect contentRect) {
        this.mContentRect.set(contentRect);
    }

    int taskIdFromPoint(int x, int y) {
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = this.mStacks.get(stackNdx);
            stack.getBounds(this.mTmpRect);
            if (this.mTmpRect.contains(x, y) && !stack.isAdjustedForMinimizedDockedStack()) {
                ArrayList<Task> tasks = stack.getTasks();
                for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                    Task task = tasks.get(taskNdx);
                    WindowState win = task.getTopVisibleAppMainWindow();
                    if (win != null) {
                        task.getDimBounds(this.mTmpRect);
                        if (this.mTmpRect.contains(x, y)) {
                            return task.mTaskId;
                        }
                    }
                }
            }
        }
        return -1;
    }

    Task findTaskForControlPoint(int x, int y) {
        WindowManagerService windowManagerService = this.mService;
        int delta = WindowManagerService.dipToPixel(30, this.mDisplayMetrics);
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = this.mStacks.get(stackNdx);
            if (!ActivityManager.StackId.isTaskResizeAllowed(stack.mStackId)) {
                break;
            }
            ArrayList<Task> tasks = stack.getTasks();
            for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                Task task = tasks.get(taskNdx);
                if (task.isFullscreen()) {
                    return null;
                }
                task.getDimBounds(this.mTmpRect);
                this.mTmpRect.inset(-delta, -delta);
                if (this.mTmpRect.contains(x, y)) {
                    this.mTmpRect.inset(delta, delta);
                    if (this.mTmpRect.contains(x, y)) {
                        return null;
                    }
                    return task;
                }
            }
        }
        return null;
    }

    void setTouchExcludeRegion(Task focusedTask) {
        Task stickyTask = null;
        this.mTouchExcludeRegion.set(this.mBaseDisplayRect);
        WindowManagerService windowManagerService = this.mService;
        int delta = WindowManagerService.dipToPixel(30, this.mDisplayMetrics);
        boolean addBackFocusedTask = false;
        this.mNonResizeableRegion.setEmpty();
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = this.mStacks.get(stackNdx);
            ArrayList<Task> tasks = stack.getTasks();
            int taskNdx = tasks.size() - 1;
            while (true) {
                if (taskNdx >= 0) {
                    Task task = tasks.get(taskNdx);
                    if (MultiWindowManager.isSupported() && task.mSticky) {
                        stickyTask = task;
                    }
                    AppWindowToken token = task.getTopVisibleAppToken();
                    if (token != null && token.isVisible()) {
                        task.getDimBounds(this.mTmpRect);
                        if (task == focusedTask) {
                            addBackFocusedTask = true;
                            this.mTmpRect2.set(this.mTmpRect);
                        }
                        boolean isFreeformed = task.inFreeformWorkspace();
                        if (task != focusedTask || isFreeformed) {
                            if (isFreeformed) {
                                this.mTmpRect.inset(-delta, -delta);
                                this.mTmpRect.intersect(this.mContentRect);
                            }
                            this.mTouchExcludeRegion.op(this.mTmpRect, Region.Op.DIFFERENCE);
                        }
                        if (task.isTwoFingerScrollMode()) {
                            stack.getBounds(this.mTmpRect);
                            this.mNonResizeableRegion.op(this.mTmpRect, Region.Op.UNION);
                            break;
                        }
                    }
                    taskNdx--;
                }
            }
        }
        if (addBackFocusedTask) {
            this.mTouchExcludeRegion.op(this.mTmpRect2, Region.Op.UNION);
        }
        WindowState inputMethod = this.mService.mInputMethodWindow;
        if (inputMethod != null && inputMethod.isVisibleLw()) {
            inputMethod.getTouchableRegion(this.mTmpRegion);
            this.mTouchExcludeRegion.op(this.mTmpRegion, Region.Op.UNION);
        }
        for (int i = this.mTapExcludedWindows.size() - 1; i >= 0; i--) {
            WindowState win = this.mTapExcludedWindows.get(i);
            win.getTouchableRegion(this.mTmpRegion);
            this.mTouchExcludeRegion.op(this.mTmpRegion, Region.Op.UNION);
        }
        if (getDockedStackVisibleForUserLocked() != null) {
            this.mDividerControllerLocked.getTouchRegion(this.mTmpRect);
            this.mTmpRegion.set(this.mTmpRect);
            this.mTouchExcludeRegion.op(this.mTmpRegion, Region.Op.UNION);
        }
        if (MultiWindowManager.isSupported() && stickyTask != null && stickyTask != focusedTask) {
            Rect rect = new Rect();
            stickyTask.getBounds(rect);
            Region tmpStickyRegion = new Region(rect);
            this.mTouchExcludeRegion.op(tmpStickyRegion, Region.Op.DIFFERENCE);
        }
        if (this.mTapDetector == null) {
            return;
        }
        this.mTapDetector.setTouchExcludeRegion(this.mTouchExcludeRegion, this.mNonResizeableRegion);
    }

    void switchUserStacks() {
        WindowList windows = getWindowList();
        for (int i = 0; i < windows.size(); i++) {
            WindowState win = windows.get(i);
            if (win.isHiddenFromUserLocked()) {
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.w("WindowManager", "user changing, hiding " + win + ", attrs=" + win.mAttrs.type + ", belonging to " + win.mOwnerUid);
                }
                win.hideLw(false);
            }
        }
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            this.mStacks.get(stackNdx).switchUser();
        }
    }

    void resetAnimationBackgroundAnimator() {
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            this.mStacks.get(stackNdx).resetAnimationBackgroundAnimator();
        }
    }

    boolean animateDimLayers() {
        return this.mDimLayerController.animateDimLayers();
    }

    void resetDimming() {
        this.mDimLayerController.resetDimming();
    }

    boolean isDimming() {
        return this.mDimLayerController.isDimming();
    }

    void stopDimmingIfNeeded() {
        this.mDimLayerController.stopDimmingIfNeeded();
    }

    void close() {
        this.mDimLayerController.close();
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            this.mStacks.get(stackNdx).close();
        }
    }

    boolean isAnimating() {
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = this.mStacks.get(stackNdx);
            if (stack.isAnimating()) {
                return true;
            }
        }
        return false;
    }

    void checkForDeferredActions() {
        boolean animating = false;
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = this.mStacks.get(stackNdx);
            if (stack.isAnimating()) {
                animating = true;
            } else {
                if (stack.mDeferDetach) {
                    this.mService.detachStackLocked(this, stack);
                }
                ArrayList<Task> tasks = stack.getTasks();
                for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                    Task task = tasks.get(taskNdx);
                    AppTokenList tokens = task.mAppTokens;
                    for (int tokenNdx = tokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                        AppWindowToken wtoken = tokens.get(tokenNdx);
                        if (wtoken.mIsExiting) {
                            wtoken.removeAppFromTaskLocked();
                        }
                    }
                }
            }
        }
        if (animating || !this.mDeferredRemoval) {
            return;
        }
        this.mService.onDisplayRemoved(this.mDisplayId);
    }

    void rotateBounds(int oldRotation, int newRotation, Rect bounds) {
        int rotationDelta = deltaRotation(oldRotation, newRotation);
        getLogicalDisplayRect(this.mTmpRect);
        switch (rotationDelta) {
            case 0:
                this.mTmpRect2.set(bounds);
                break;
            case 1:
                this.mTmpRect2.top = this.mTmpRect.bottom - bounds.right;
                this.mTmpRect2.left = bounds.top;
                this.mTmpRect2.right = this.mTmpRect2.left + bounds.height();
                this.mTmpRect2.bottom = this.mTmpRect2.top + bounds.width();
                break;
            case 2:
                this.mTmpRect2.top = this.mTmpRect.bottom - bounds.bottom;
                this.mTmpRect2.left = this.mTmpRect.right - bounds.right;
                this.mTmpRect2.right = this.mTmpRect2.left + bounds.width();
                this.mTmpRect2.bottom = this.mTmpRect2.top + bounds.height();
                break;
            case 3:
                this.mTmpRect2.top = bounds.left;
                this.mTmpRect2.left = this.mTmpRect.right - bounds.bottom;
                this.mTmpRect2.right = this.mTmpRect2.left + bounds.height();
                this.mTmpRect2.bottom = this.mTmpRect2.top + bounds.width();
                break;
        }
        bounds.set(this.mTmpRect2);
    }

    static int deltaRotation(int oldRotation, int newRotation) {
        int delta = newRotation - oldRotation;
        return delta < 0 ? delta + 4 : delta;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("Display: mDisplayId=");
        pw.println(this.mDisplayId);
        String subPrefix = "  " + prefix;
        pw.print(subPrefix);
        pw.print("init=");
        pw.print(this.mInitialDisplayWidth);
        pw.print("x");
        pw.print(this.mInitialDisplayHeight);
        pw.print(" ");
        pw.print(this.mInitialDisplayDensity);
        pw.print("dpi");
        if (this.mInitialDisplayWidth != this.mBaseDisplayWidth || this.mInitialDisplayHeight != this.mBaseDisplayHeight || this.mInitialDisplayDensity != this.mBaseDisplayDensity) {
            pw.print(" base=");
            pw.print(this.mBaseDisplayWidth);
            pw.print("x");
            pw.print(this.mBaseDisplayHeight);
            pw.print(" ");
            pw.print(this.mBaseDisplayDensity);
            pw.print("dpi");
        }
        if (this.mDisplayScalingDisabled) {
            pw.println(" noscale");
        }
        pw.print(" cur=");
        pw.print(this.mDisplayInfo.logicalWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.logicalHeight);
        pw.print(" app=");
        pw.print(this.mDisplayInfo.appWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.appHeight);
        pw.print(" rng=");
        pw.print(this.mDisplayInfo.smallestNominalAppWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.smallestNominalAppHeight);
        pw.print("-");
        pw.print(this.mDisplayInfo.largestNominalAppWidth);
        pw.print("x");
        pw.println(this.mDisplayInfo.largestNominalAppHeight);
        pw.print(subPrefix);
        pw.print("deferred=");
        pw.print(this.mDeferredRemoval);
        pw.print(" layoutNeeded=");
        pw.println(this.layoutNeeded);
        pw.println();
        pw.println("  Application tokens in top down Z order:");
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = this.mStacks.get(stackNdx);
            stack.dump(prefix + "  ", pw);
        }
        pw.println();
        if (!this.mExitingTokens.isEmpty()) {
            pw.println();
            pw.println("  Exiting tokens:");
            for (int i = this.mExitingTokens.size() - 1; i >= 0; i--) {
                WindowToken token = this.mExitingTokens.get(i);
                pw.print("  Exiting #");
                pw.print(i);
                pw.print(' ');
                pw.print(token);
                pw.println(':');
                token.dump(pw, "    ");
            }
        }
        pw.println();
        this.mDimLayerController.dump(prefix + "  ", pw);
        pw.println();
        this.mDividerControllerLocked.dump(prefix + "  ", pw);
    }

    public String toString() {
        return "Display " + this.mDisplayId + " info=" + this.mDisplayInfo + " stacks=" + this.mStacks;
    }

    TaskStack getDockedStackLocked() {
        TaskStack stack = this.mService.mStackIdToStack.get(3);
        if (stack == null || !stack.isVisibleLocked()) {
            return null;
        }
        return stack;
    }

    TaskStack getDockedStackVisibleForUserLocked() {
        TaskStack stack = this.mService.mStackIdToStack.get(3);
        if (stack == null || !stack.isVisibleForUserLocked()) {
            return null;
        }
        return stack;
    }

    WindowState getTouchableWinAtPointLocked(float xf, float yf) {
        int x = (int) xf;
        int y = (int) yf;
        for (int i = this.mWindows.size() - 1; i >= 0; i--) {
            WindowState window = this.mWindows.get(i);
            int flags = window.mAttrs.flags;
            if (window.isVisibleLw() && (flags & 16) == 0) {
                window.getVisibleBounds(this.mTmpRect);
                if (this.mTmpRect.contains(x, y)) {
                    window.getTouchableRegion(this.mTmpRegion);
                    int touchFlags = flags & 40;
                    if (this.mTmpRegion.contains(x, y) || touchFlags == 0) {
                        return window;
                    }
                } else {
                    continue;
                }
            }
        }
        return null;
    }
}
