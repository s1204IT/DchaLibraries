package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.graphics.Point;
import android.util.IntArray;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.DisplayWindowController;
import com.android.server.wm.WindowContainerListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

class ActivityDisplay extends ConfigurationContainer<ActivityStack> implements WindowContainerListener {
    static final int POSITION_BOTTOM = Integer.MIN_VALUE;
    static final int POSITION_TOP = Integer.MAX_VALUE;
    private static final String TAG = "ActivityManager";
    private static final String TAG_STACK = TAG + ActivityManagerDebugConfig.POSTFIX_STACK;
    private static int sNextFreeStackId = 0;
    final ArrayList<ActivityManagerInternal.SleepToken> mAllSleepTokens;
    Display mDisplay;
    private IntArray mDisplayAccessUIDs;
    int mDisplayId;
    private ActivityStack mHomeStack;
    ActivityManagerInternal.SleepToken mOffToken;
    private ActivityStack mPinnedStack;
    private ActivityStack mRecentsStack;
    private boolean mSleeping;
    private ActivityStack mSplitScreenPrimaryStack;
    private ArrayList<OnStackOrderChangedListener> mStackOrderChangedCallbacks;
    private final ArrayList<ActivityStack> mStacks;
    private ActivityStackSupervisor mSupervisor;
    private Point mTmpDisplaySize;
    private DisplayWindowController mWindowContainerController;

    interface OnStackOrderChangedListener {
        void onStackOrderChanged();
    }

    @VisibleForTesting
    ActivityDisplay(ActivityStackSupervisor activityStackSupervisor, int i) {
        this(activityStackSupervisor, activityStackSupervisor.mDisplayManager.getDisplay(i));
    }

    ActivityDisplay(ActivityStackSupervisor activityStackSupervisor, Display display) {
        this.mStacks = new ArrayList<>();
        this.mStackOrderChangedCallbacks = new ArrayList<>();
        this.mDisplayAccessUIDs = new IntArray();
        this.mAllSleepTokens = new ArrayList<>();
        this.mHomeStack = null;
        this.mRecentsStack = null;
        this.mPinnedStack = null;
        this.mSplitScreenPrimaryStack = null;
        this.mTmpDisplaySize = new Point();
        this.mSupervisor = activityStackSupervisor;
        this.mDisplayId = display.getDisplayId();
        this.mDisplay = display;
        this.mWindowContainerController = createWindowContainerController();
        updateBounds();
    }

    protected DisplayWindowController createWindowContainerController() {
        return new DisplayWindowController(this.mDisplay, this);
    }

    void updateBounds() {
        this.mDisplay.getSize(this.mTmpDisplaySize);
        setBounds(0, 0, this.mTmpDisplaySize.x, this.mTmpDisplaySize.y);
    }

    void addChild(ActivityStack activityStack, int i) {
        if (i == Integer.MIN_VALUE) {
            i = 0;
        } else if (i == POSITION_TOP) {
            i = this.mStacks.size();
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG_STACK, "addChild: attaching " + activityStack + " to displayId=" + this.mDisplayId + " position=" + i);
        }
        addStackReferenceIfNeeded(activityStack);
        positionChildAt(activityStack, i);
        this.mSupervisor.mService.updateSleepIfNeededLocked();
    }

    void removeChild(ActivityStack activityStack) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG_STACK, "removeChild: detaching " + activityStack + " from displayId=" + this.mDisplayId);
        }
        this.mStacks.remove(activityStack);
        removeStackReferenceIfNeeded(activityStack);
        this.mSupervisor.mService.updateSleepIfNeededLocked();
        onStackOrderChanged();
    }

    void positionChildAtTop(ActivityStack activityStack) {
        positionChildAt(activityStack, this.mStacks.size());
    }

    void positionChildAtBottom(ActivityStack activityStack) {
        positionChildAt(activityStack, 0);
    }

    private void positionChildAt(ActivityStack activityStack, int i) {
        this.mStacks.remove(activityStack);
        int topInsertPosition = getTopInsertPosition(activityStack, i);
        this.mStacks.add(topInsertPosition, activityStack);
        this.mWindowContainerController.positionChildAt(activityStack.getWindowContainerController(), topInsertPosition);
        onStackOrderChanged();
    }

    private int getTopInsertPosition(ActivityStack activityStack, int i) {
        int size = this.mStacks.size();
        if (size > 0) {
            ActivityStack activityStack2 = this.mStacks.get(size - 1);
            if (activityStack2.getWindowConfiguration().isAlwaysOnTop() && activityStack2 != activityStack) {
                size--;
            }
        }
        return Math.min(size, i);
    }

    <T extends ActivityStack> T getStack(int i) {
        for (int size = this.mStacks.size() - 1; size >= 0; size--) {
            T t = (T) this.mStacks.get(size);
            if (t.mStackId == i) {
                return t;
            }
        }
        return null;
    }

    <T extends ActivityStack> T getStack(int i, int i2) {
        if (i2 == 2) {
            return (T) this.mHomeStack;
        }
        if (i2 == 3) {
            return (T) this.mRecentsStack;
        }
        if (i == 2) {
            return (T) this.mPinnedStack;
        }
        if (i == 3) {
            return (T) this.mSplitScreenPrimaryStack;
        }
        for (int size = this.mStacks.size() - 1; size >= 0; size--) {
            T t = (T) this.mStacks.get(size);
            if (t.isCompatible(i, i2)) {
                return t;
            }
        }
        return null;
    }

    private boolean alwaysCreateStack(int i, int i2) {
        return i2 == 1 && (i == 1 || i == 5 || i == 4);
    }

    <T extends ActivityStack> T getOrCreateStack(int i, int i2, boolean z) {
        T t;
        if (!alwaysCreateStack(i, i2) && (t = (T) getStack(i, i2)) != null) {
            return t;
        }
        return (T) createStack(i, i2, z);
    }

    <T extends ActivityStack> T getOrCreateStack(ActivityRecord activityRecord, ActivityOptions activityOptions, TaskRecord taskRecord, int i, boolean z) {
        return (T) getOrCreateStack(resolveWindowingMode(activityRecord, activityOptions, taskRecord, i), i, z);
    }

    private int getNextStackId() {
        int i = sNextFreeStackId;
        sNextFreeStackId = i + 1;
        return i;
    }

    <T extends ActivityStack> T createStack(int i, int i2, boolean z) {
        ActivityStack stack;
        if (i2 == 0) {
            i2 = 1;
        }
        if (i2 != 1 && (stack = getStack(0, i2)) != null) {
            throw new IllegalArgumentException("Stack=" + stack + " of activityType=" + i2 + " already on display=" + this + ". Can't have multiple.");
        }
        ActivityManagerService activityManagerService = this.mSupervisor.mService;
        if (!isWindowingModeSupported(i, activityManagerService.mSupportsMultiWindow, activityManagerService.mSupportsSplitScreenMultiWindow, activityManagerService.mSupportsFreeformWindowManagement, activityManagerService.mSupportsPictureInPicture, i2)) {
            throw new IllegalArgumentException("Can't create stack for unsupported windowingMode=" + i);
        }
        if (i == 0 && (i = getWindowingMode()) == 0) {
            i = 1;
        }
        return (T) createStackUnchecked(i, i2, getNextStackId(), z);
    }

    @VisibleForTesting
    <T extends ActivityStack> T createStackUnchecked(int i, int i2, int i3, boolean z) {
        if (i == 2) {
            return new PinnedActivityStack(this, i3, this.mSupervisor, z);
        }
        return (T) new ActivityStack(this, i3, this.mSupervisor, i, i2, z);
    }

    void removeStacksInWindowingModes(int... iArr) {
        if (iArr == null || iArr.length == 0) {
            return;
        }
        for (int length = iArr.length - 1; length >= 0; length--) {
            int i = iArr[length];
            for (int size = this.mStacks.size() - 1; size >= 0; size--) {
                ActivityStack activityStack = this.mStacks.get(size);
                if (activityStack.isActivityTypeStandardOrUndefined() && activityStack.getWindowingMode() == i) {
                    this.mSupervisor.removeStack(activityStack);
                }
            }
        }
    }

    void removeStacksWithActivityTypes(int... iArr) {
        if (iArr == null || iArr.length == 0) {
            return;
        }
        for (int length = iArr.length - 1; length >= 0; length--) {
            int i = iArr[length];
            for (int size = this.mStacks.size() - 1; size >= 0; size--) {
                ActivityStack activityStack = this.mStacks.get(size);
                if (activityStack.getActivityType() == i) {
                    this.mSupervisor.removeStack(activityStack);
                }
            }
        }
    }

    void onStackWindowingModeChanged(ActivityStack activityStack) {
        removeStackReferenceIfNeeded(activityStack);
        addStackReferenceIfNeeded(activityStack);
    }

    private void addStackReferenceIfNeeded(ActivityStack activityStack) {
        int activityType = activityStack.getActivityType();
        int windowingMode = activityStack.getWindowingMode();
        if (activityType == 2) {
            if (this.mHomeStack != null && this.mHomeStack != activityStack) {
                throw new IllegalArgumentException("addStackReferenceIfNeeded: home stack=" + this.mHomeStack + " already exist on display=" + this + " stack=" + activityStack);
            }
            this.mHomeStack = activityStack;
        } else if (activityType == 3) {
            if (this.mRecentsStack != null && this.mRecentsStack != activityStack) {
                throw new IllegalArgumentException("addStackReferenceIfNeeded: recents stack=" + this.mRecentsStack + " already exist on display=" + this + " stack=" + activityStack);
            }
            this.mRecentsStack = activityStack;
        }
        if (windowingMode == 2) {
            if (this.mPinnedStack != null && this.mPinnedStack != activityStack) {
                throw new IllegalArgumentException("addStackReferenceIfNeeded: pinned stack=" + this.mPinnedStack + " already exist on display=" + this + " stack=" + activityStack);
            }
            this.mPinnedStack = activityStack;
            return;
        }
        if (windowingMode == 3) {
            if (this.mSplitScreenPrimaryStack != null && this.mSplitScreenPrimaryStack != activityStack) {
                throw new IllegalArgumentException("addStackReferenceIfNeeded: split-screen-primary stack=" + this.mSplitScreenPrimaryStack + " already exist on display=" + this + " stack=" + activityStack);
            }
            this.mSplitScreenPrimaryStack = activityStack;
            onSplitScreenModeActivated();
        }
    }

    private void removeStackReferenceIfNeeded(ActivityStack activityStack) {
        if (activityStack == this.mHomeStack) {
            this.mHomeStack = null;
            return;
        }
        if (activityStack == this.mRecentsStack) {
            this.mRecentsStack = null;
            return;
        }
        if (activityStack == this.mPinnedStack) {
            this.mPinnedStack = null;
        } else if (activityStack == this.mSplitScreenPrimaryStack) {
            this.mSplitScreenPrimaryStack = null;
            onSplitScreenModeDismissed();
        }
    }

    private void onSplitScreenModeDismissed() {
        this.mSupervisor.mWindowManager.deferSurfaceLayout();
        try {
            for (int size = this.mStacks.size() - 1; size >= 0; size--) {
                ActivityStack activityStack = this.mStacks.get(size);
                if (activityStack.inSplitScreenSecondaryWindowingMode()) {
                    activityStack.setWindowingMode(1, false, false, false, true);
                }
            }
        } finally {
            ActivityStack topStackInWindowingMode = getTopStackInWindowingMode(1);
            if (topStackInWindowingMode != null && this.mHomeStack != null && !isTopStack(this.mHomeStack)) {
                this.mHomeStack.moveToFront("onSplitScreenModeDismissed");
                topStackInWindowingMode.moveToFront("onSplitScreenModeDismissed");
            }
            this.mSupervisor.mWindowManager.continueSurfaceLayout();
        }
    }

    private void onSplitScreenModeActivated() {
        this.mSupervisor.mWindowManager.deferSurfaceLayout();
        try {
            for (int size = this.mStacks.size() - 1; size >= 0; size--) {
                ActivityStack activityStack = this.mStacks.get(size);
                if (activityStack != this.mSplitScreenPrimaryStack && activityStack.affectedBySplitScreenResize()) {
                    activityStack.setWindowingMode(4, false, false, true, true);
                }
            }
        } finally {
            this.mSupervisor.mWindowManager.continueSurfaceLayout();
        }
    }

    private boolean isWindowingModeSupported(int i, boolean z, boolean z2, boolean z3, boolean z4, int i2) {
        if (i == 0 || i == 1) {
            return true;
        }
        if (!z) {
            return false;
        }
        if (i == 3 || i == 4) {
            if (z2 && WindowConfiguration.supportSplitScreenWindowingMode(i2)) {
                return true;
            }
            return false;
        }
        if (!z3 && i == 5) {
            return false;
        }
        if (z4 || i != 2) {
            return true;
        }
        return false;
    }

    int resolveWindowingMode(ActivityRecord activityRecord, ActivityOptions activityOptions, TaskRecord taskRecord, int i) {
        int launchWindowingMode = activityOptions != null ? activityOptions.getLaunchWindowingMode() : 0;
        if (launchWindowingMode == 0) {
            if (taskRecord != null) {
                launchWindowingMode = taskRecord.getWindowingMode();
            }
            if (launchWindowingMode == 0 && activityRecord != null) {
                launchWindowingMode = activityRecord.getWindowingMode();
            }
            if (launchWindowingMode == 0) {
                launchWindowingMode = getWindowingMode();
            }
        }
        ActivityManagerService activityManagerService = this.mSupervisor.mService;
        boolean zIsResizeable = activityManagerService.mSupportsMultiWindow;
        boolean zSupportsSplitScreenWindowingMode = activityManagerService.mSupportsSplitScreenMultiWindow;
        boolean zSupportsFreeform = activityManagerService.mSupportsFreeformWindowManagement;
        boolean zSupportsPictureInPicture = activityManagerService.mSupportsPictureInPicture;
        if (zIsResizeable) {
            if (taskRecord != null) {
                zIsResizeable = taskRecord.isResizeable();
                zSupportsSplitScreenWindowingMode = taskRecord.supportsSplitScreenWindowingMode();
            } else if (activityRecord != null) {
                zIsResizeable = activityRecord.isResizeable();
                zSupportsSplitScreenWindowingMode = activityRecord.supportsSplitScreenWindowingMode();
                zSupportsFreeform = activityRecord.supportsFreeform();
                zSupportsPictureInPicture = activityRecord.supportsPictureInPicture();
            }
        }
        boolean z = zSupportsPictureInPicture;
        boolean z2 = zIsResizeable;
        boolean z3 = zSupportsSplitScreenWindowingMode;
        boolean z4 = zSupportsFreeform;
        boolean zHasSplitScreenPrimaryStack = hasSplitScreenPrimaryStack();
        if (zHasSplitScreenPrimaryStack || launchWindowingMode != 4) {
            if (zHasSplitScreenPrimaryStack && launchWindowingMode == 1 && z3) {
                launchWindowingMode = 4;
            }
        } else {
            launchWindowingMode = 1;
        }
        if (launchWindowingMode != 0 && isWindowingModeSupported(launchWindowingMode, z2, z3, z4, z, i)) {
            return launchWindowingMode;
        }
        int windowingMode = getWindowingMode();
        if (windowingMode != 0) {
            return windowingMode;
        }
        return 1;
    }

    ActivityStack getTopStack() {
        if (this.mStacks.isEmpty()) {
            return null;
        }
        return this.mStacks.get(this.mStacks.size() - 1);
    }

    boolean isTopStack(ActivityStack activityStack) {
        return activityStack == getTopStack();
    }

    boolean isTopNotPinnedStack(ActivityStack activityStack) {
        for (int size = this.mStacks.size() - 1; size >= 0; size--) {
            ActivityStack activityStack2 = this.mStacks.get(size);
            if (!activityStack2.inPinnedWindowingMode()) {
                return activityStack2 == activityStack;
            }
        }
        return false;
    }

    ActivityStack getTopStackInWindowingMode(int i) {
        for (int size = this.mStacks.size() - 1; size >= 0; size--) {
            ActivityStack activityStack = this.mStacks.get(size);
            if (i == activityStack.getWindowingMode()) {
                return activityStack;
            }
        }
        return null;
    }

    int getIndexOf(ActivityStack activityStack) {
        return this.mStacks.indexOf(activityStack);
    }

    void onLockTaskPackagesUpdated() {
        for (int size = this.mStacks.size() - 1; size >= 0; size--) {
            this.mStacks.get(size).onLockTaskPackagesUpdated();
        }
    }

    void onExitingSplitScreenMode() {
        this.mSplitScreenPrimaryStack = null;
    }

    ActivityStack getSplitScreenPrimaryStack() {
        return this.mSplitScreenPrimaryStack;
    }

    boolean hasSplitScreenPrimaryStack() {
        return this.mSplitScreenPrimaryStack != null;
    }

    PinnedActivityStack getPinnedStack() {
        return (PinnedActivityStack) this.mPinnedStack;
    }

    boolean hasPinnedStack() {
        return this.mPinnedStack != null;
    }

    public String toString() {
        return "ActivityDisplay={" + this.mDisplayId + " numStacks=" + this.mStacks.size() + "}";
    }

    @Override
    protected int getChildCount() {
        return this.mStacks.size();
    }

    @Override
    protected ActivityStack getChildAt(int i) {
        return this.mStacks.get(i);
    }

    @Override
    protected ConfigurationContainer getParent() {
        return this.mSupervisor;
    }

    boolean isPrivate() {
        return (this.mDisplay.getFlags() & 4) != 0;
    }

    boolean isUidPresent(int i) {
        Iterator<ActivityStack> it = this.mStacks.iterator();
        while (it.hasNext()) {
            if (it.next().isUidPresent(i)) {
                return true;
            }
        }
        return false;
    }

    void remove() {
        boolean zShouldDestroyContentOnRemove = shouldDestroyContentOnRemove();
        while (getChildCount() > 0) {
            ActivityStack childAt = getChildAt(0);
            if (!zShouldDestroyContentOnRemove) {
                this.mSupervisor.moveTasksToFullscreenStackLocked(childAt, true);
            } else {
                childAt.onOverrideConfigurationChanged(childAt.getConfiguration());
                this.mSupervisor.moveStackToDisplayLocked(childAt.mStackId, 0, false);
                childAt.finishAllActivitiesLocked(true);
            }
        }
        this.mWindowContainerController.removeContainer();
        this.mWindowContainerController = null;
    }

    IntArray getPresentUIDs() {
        this.mDisplayAccessUIDs.clear();
        Iterator<ActivityStack> it = this.mStacks.iterator();
        while (it.hasNext()) {
            it.next().getPresentUIDs(this.mDisplayAccessUIDs);
        }
        return this.mDisplayAccessUIDs;
    }

    private boolean shouldDestroyContentOnRemove() {
        return this.mDisplay.getRemoveMode() == 1;
    }

    boolean shouldSleep() {
        return (this.mStacks.isEmpty() || !this.mAllSleepTokens.isEmpty()) && this.mSupervisor.mService.mRunningVoice == null;
    }

    ActivityStack getStackAbove(ActivityStack activityStack) {
        int iIndexOf = this.mStacks.indexOf(activityStack) + 1;
        if (iIndexOf < this.mStacks.size()) {
            return this.mStacks.get(iIndexOf);
        }
        return null;
    }

    void moveStackBehindBottomMostVisibleStack(ActivityStack activityStack) {
        if (activityStack.shouldBeVisible(null)) {
            return;
        }
        positionChildAtBottom(activityStack);
        int size = this.mStacks.size();
        for (int i = 0; i < size; i++) {
            ActivityStack activityStack2 = this.mStacks.get(i);
            if (activityStack2 != activityStack) {
                int windowingMode = activityStack2.getWindowingMode();
                boolean z = windowingMode == 1 || windowingMode == 4;
                if (activityStack2.shouldBeVisible(null) && z) {
                    positionChildAt(activityStack, Math.max(0, i - 1));
                    return;
                }
            }
        }
    }

    void moveStackBehindStack(ActivityStack activityStack, ActivityStack activityStack2) {
        if (activityStack2 == null || activityStack2 == activityStack) {
            return;
        }
        int iIndexOf = this.mStacks.indexOf(activityStack);
        int iIndexOf2 = this.mStacks.indexOf(activityStack2);
        if (iIndexOf <= iIndexOf2) {
            iIndexOf2--;
        }
        positionChildAt(activityStack, Math.max(0, iIndexOf2));
    }

    boolean isSleeping() {
        return this.mSleeping;
    }

    void setIsSleeping(boolean z) {
        this.mSleeping = z;
    }

    void registerStackOrderChangedListener(OnStackOrderChangedListener onStackOrderChangedListener) {
        if (!this.mStackOrderChangedCallbacks.contains(onStackOrderChangedListener)) {
            this.mStackOrderChangedCallbacks.add(onStackOrderChangedListener);
        }
    }

    void unregisterStackOrderChangedListener(OnStackOrderChangedListener onStackOrderChangedListener) {
        this.mStackOrderChangedCallbacks.remove(onStackOrderChangedListener);
    }

    private void onStackOrderChanged() {
        for (int size = this.mStackOrderChangedCallbacks.size() - 1; size >= 0; size--) {
            this.mStackOrderChangedCallbacks.get(size).onStackOrderChanged();
        }
    }

    public void deferUpdateImeTarget() {
        this.mWindowContainerController.deferUpdateImeTarget();
    }

    public void continueUpdateImeTarget() {
        this.mWindowContainerController.continueUpdateImeTarget();
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "displayId=" + this.mDisplayId + " stacks=" + this.mStacks.size());
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(" ");
        String string = sb.toString();
        if (this.mHomeStack != null) {
            printWriter.println(string + "mHomeStack=" + this.mHomeStack);
        }
        if (this.mRecentsStack != null) {
            printWriter.println(string + "mRecentsStack=" + this.mRecentsStack);
        }
        if (this.mPinnedStack != null) {
            printWriter.println(string + "mPinnedStack=" + this.mPinnedStack);
        }
        if (this.mSplitScreenPrimaryStack != null) {
            printWriter.println(string + "mSplitScreenPrimaryStack=" + this.mSplitScreenPrimaryStack);
        }
    }

    public void dumpStacks(PrintWriter printWriter) {
        for (int size = this.mStacks.size() - 1; size >= 0; size--) {
            printWriter.print(this.mStacks.get(size).mStackId);
            if (size > 0) {
                printWriter.print(",");
            }
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, false);
        protoOutputStream.write(1120986464258L, this.mDisplayId);
        for (int size = this.mStacks.size() - 1; size >= 0; size--) {
            this.mStacks.get(size).writeToProto(protoOutputStream, 2246267895811L);
        }
        protoOutputStream.end(jStart);
    }
}
