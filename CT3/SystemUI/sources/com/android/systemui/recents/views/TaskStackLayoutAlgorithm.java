package com.android.systemui.recents.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.ViewDebug;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.RecentsDebugFlags;
import com.android.systemui.recents.misc.FreePathInterpolator;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TaskStackLayoutAlgorithm {

    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseBottomMargin;
    private int mBaseInitialBottomOffset;
    private int mBaseInitialTopOffset;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseSideMargin;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseTopMargin;
    private TaskStackLayoutAlgorithmCallbacks mCb;
    Context mContext;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusState;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusedBottomPeekHeight;
    private Path mFocusedCurve;
    private FreePathInterpolator mFocusedCurveInterpolator;
    private Path mFocusedDimCurve;
    private FreePathInterpolator mFocusedDimCurveInterpolator;
    private Range mFocusedRange;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusedTopPeekHeight;
    FreeformWorkspaceLayoutAlgorithm mFreeformLayoutAlgorithm;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mFreeformStackGap;

    @ViewDebug.ExportedProperty(category = "recents")
    float mFrontMostTaskP;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialBottomOffset;

    @ViewDebug.ExportedProperty(category = "recents")
    float mInitialScrollP;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialTopOffset;

    @ViewDebug.ExportedProperty(category = "recents")
    float mMaxScrollP;

    @ViewDebug.ExportedProperty(category = "recents")
    int mMaxTranslationZ;
    private int mMinMargin;

    @ViewDebug.ExportedProperty(category = "recents")
    float mMinScrollP;

    @ViewDebug.ExportedProperty(category = "recents")
    int mMinTranslationZ;

    @ViewDebug.ExportedProperty(category = "recents")
    int mNumFreeformTasks;

    @ViewDebug.ExportedProperty(category = "recents")
    int mNumStackTasks;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mStackBottomOffset;
    private Path mUnfocusedCurve;
    private FreePathInterpolator mUnfocusedCurveInterpolator;
    private Path mUnfocusedDimCurve;
    private FreePathInterpolator mUnfocusedDimCurveInterpolator;
    private Range mUnfocusedRange;
    private StackState mState = StackState.SPLIT;

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mTaskRect = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mFreeformRect = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mStackRect = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mSystemInsets = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mStackActionButtonRect = new Rect();
    private SparseIntArray mTaskIndexMap = new SparseIntArray();
    private SparseArray<Float> mTaskIndexOverrideMap = new SparseArray<>();
    TaskViewTransform mBackOfStackTransform = new TaskViewTransform();
    TaskViewTransform mFrontOfStackTransform = new TaskViewTransform();

    public interface TaskStackLayoutAlgorithmCallbacks {
        void onFocusStateChanged(int i, int i2);
    }

    public static class StackState {
        public final int freeformBackgroundAlpha;
        public final float freeformHeightPct;
        public static final StackState FREEFORM_ONLY = new StackState(1.0f, 255);
        public static final StackState STACK_ONLY = new StackState(0.0f, 0);
        public static final StackState SPLIT = new StackState(0.5f, 255);

        private StackState(float freeformHeightPct, int freeformBackgroundAlpha) {
            this.freeformHeightPct = freeformHeightPct;
            this.freeformBackgroundAlpha = freeformBackgroundAlpha;
        }

        public static StackState getStackStateForStack(TaskStack stack) {
            SystemServicesProxy ssp = Recents.getSystemServices();
            boolean hasFreeformWorkspaces = ssp.hasFreeformWorkspaceSupport();
            int freeformCount = stack.getFreeformTaskCount();
            int stackCount = stack.getStackTaskCount();
            if (hasFreeformWorkspaces && stackCount > 0 && freeformCount > 0) {
                return SPLIT;
            }
            if (hasFreeformWorkspaces && freeformCount > 0) {
                return FREEFORM_ONLY;
            }
            return STACK_ONLY;
        }

        public void computeRects(Rect freeformRectOut, Rect stackRectOut, Rect taskStackBounds, int topMargin, int freeformGap, int stackBottomOffset) {
            int availableHeight = (taskStackBounds.height() - topMargin) - stackBottomOffset;
            int ffPaddedHeight = (int) (availableHeight * this.freeformHeightPct);
            int ffHeight = Math.max(0, ffPaddedHeight - freeformGap);
            freeformRectOut.set(taskStackBounds.left, taskStackBounds.top + topMargin, taskStackBounds.right, taskStackBounds.top + topMargin + ffHeight);
            stackRectOut.set(taskStackBounds.left, taskStackBounds.top, taskStackBounds.right, taskStackBounds.bottom);
            if (ffPaddedHeight > 0) {
                stackRectOut.top += ffPaddedHeight;
            } else {
                stackRectOut.top += topMargin;
            }
        }
    }

    public class VisibilityReport {
        public int numVisibleTasks;
        public int numVisibleThumbnails;

        VisibilityReport(int tasks, int thumbnails) {
            this.numVisibleTasks = tasks;
            this.numVisibleThumbnails = thumbnails;
        }
    }

    public TaskStackLayoutAlgorithm(Context context, TaskStackLayoutAlgorithmCallbacks cb) {
        Resources res = context.getResources();
        this.mContext = context;
        this.mCb = cb;
        this.mFreeformLayoutAlgorithm = new FreeformWorkspaceLayoutAlgorithm(context);
        this.mMinMargin = res.getDimensionPixelSize(R.dimen.recents_layout_min_margin);
        this.mBaseTopMargin = getDimensionForDevice(context, R.dimen.recents_layout_top_margin_phone, R.dimen.recents_layout_top_margin_tablet, R.dimen.recents_layout_top_margin_tablet_xlarge);
        this.mBaseSideMargin = getDimensionForDevice(context, R.dimen.recents_layout_side_margin_phone, R.dimen.recents_layout_side_margin_tablet, R.dimen.recents_layout_side_margin_tablet_xlarge);
        this.mBaseBottomMargin = res.getDimensionPixelSize(R.dimen.recents_layout_bottom_margin);
        this.mFreeformStackGap = res.getDimensionPixelSize(R.dimen.recents_freeform_layout_bottom_margin);
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        Resources res = context.getResources();
        this.mFocusedRange = new Range(res.getFloat(R.integer.recents_layout_focused_range_min), res.getFloat(R.integer.recents_layout_focused_range_max));
        this.mUnfocusedRange = new Range(res.getFloat(R.integer.recents_layout_unfocused_range_min), res.getFloat(R.integer.recents_layout_unfocused_range_max));
        this.mFocusState = getInitialFocusState();
        this.mFocusedTopPeekHeight = res.getDimensionPixelSize(R.dimen.recents_layout_top_peek_size);
        this.mFocusedBottomPeekHeight = res.getDimensionPixelSize(R.dimen.recents_layout_bottom_peek_size);
        this.mMinTranslationZ = res.getDimensionPixelSize(R.dimen.recents_layout_z_min);
        this.mMaxTranslationZ = res.getDimensionPixelSize(R.dimen.recents_layout_z_max);
        this.mBaseInitialTopOffset = getDimensionForDevice(context, R.dimen.recents_layout_initial_top_offset_phone_port, R.dimen.recents_layout_initial_top_offset_phone_land, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet);
        this.mBaseInitialBottomOffset = getDimensionForDevice(context, R.dimen.recents_layout_initial_bottom_offset_phone_port, R.dimen.recents_layout_initial_bottom_offset_phone_land, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet);
        this.mFreeformLayoutAlgorithm.reloadOnConfigurationChange(context);
    }

    public void reset() {
        this.mTaskIndexOverrideMap.clear();
        setFocusState(getInitialFocusState());
    }

    public boolean setSystemInsets(Rect systemInsets) {
        boolean changed = !this.mSystemInsets.equals(systemInsets);
        this.mSystemInsets.set(systemInsets);
        return changed;
    }

    public void setFocusState(int focusState) {
        int prevFocusState = this.mFocusState;
        this.mFocusState = focusState;
        updateFrontBackTransforms();
        if (this.mCb == null) {
            return;
        }
        this.mCb.onFocusStateChanged(prevFocusState, focusState);
    }

    public int getFocusState() {
        return this.mFocusState;
    }

    public void initialize(Rect displayRect, Rect windowRect, Rect taskStackBounds, StackState state) {
        Rect lastStackRect = new Rect(this.mStackRect);
        int topMargin = getScaleForExtent(windowRect, displayRect, this.mBaseTopMargin, this.mMinMargin, 1);
        int bottomMargin = getScaleForExtent(windowRect, displayRect, this.mBaseBottomMargin, this.mMinMargin, 1);
        this.mInitialTopOffset = getScaleForExtent(windowRect, displayRect, this.mBaseInitialTopOffset, this.mMinMargin, 1);
        this.mInitialBottomOffset = this.mBaseInitialBottomOffset;
        this.mState = state;
        this.mStackBottomOffset = this.mSystemInsets.bottom + bottomMargin;
        state.computeRects(this.mFreeformRect, this.mStackRect, taskStackBounds, topMargin, this.mFreeformStackGap, this.mStackBottomOffset);
        this.mStackActionButtonRect.set(this.mStackRect.left, this.mStackRect.top - topMargin, this.mStackRect.right, this.mStackRect.top + this.mFocusedTopPeekHeight);
        int height = (this.mStackRect.height() - this.mInitialTopOffset) - this.mStackBottomOffset;
        this.mTaskRect.set(this.mStackRect.left, this.mStackRect.top, this.mStackRect.right, this.mStackRect.top + height);
        if (lastStackRect.equals(this.mStackRect)) {
            return;
        }
        this.mUnfocusedCurve = constructUnfocusedCurve();
        this.mUnfocusedCurveInterpolator = new FreePathInterpolator(this.mUnfocusedCurve);
        this.mFocusedCurve = constructFocusedCurve();
        this.mFocusedCurveInterpolator = new FreePathInterpolator(this.mFocusedCurve);
        this.mUnfocusedDimCurve = constructUnfocusedDimCurve();
        this.mUnfocusedDimCurveInterpolator = new FreePathInterpolator(this.mUnfocusedDimCurve);
        this.mFocusedDimCurve = constructFocusedDimCurve();
        this.mFocusedDimCurveInterpolator = new FreePathInterpolator(this.mFocusedDimCurve);
        updateFrontBackTransforms();
    }

    void update(TaskStack stack, ArraySet<Task.TaskKey> ignoreTasksSet) {
        boolean scrollToFront;
        SystemServicesProxy ssp = Recents.getSystemServices();
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        this.mTaskIndexMap.clear();
        ArrayList<Task> tasks = stack.getStackTasks();
        if (tasks.isEmpty()) {
            this.mFrontMostTaskP = 0.0f;
            this.mInitialScrollP = 0.0f;
            this.mMaxScrollP = 0.0f;
            this.mMinScrollP = 0.0f;
            this.mNumFreeformTasks = 0;
            this.mNumStackTasks = 0;
            return;
        }
        ArrayList<Task> freeformTasks = new ArrayList<>();
        ArrayList<Task> stackTasks = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (!ignoreTasksSet.contains(task.key)) {
                if (task.isFreeformTask()) {
                    freeformTasks.add(task);
                } else {
                    stackTasks.add(task);
                }
            }
        }
        this.mNumStackTasks = stackTasks.size();
        this.mNumFreeformTasks = freeformTasks.size();
        int taskCount = stackTasks.size();
        for (int i2 = 0; i2 < taskCount; i2++) {
            this.mTaskIndexMap.put(stackTasks.get(i2).key.id, i2);
        }
        if (!freeformTasks.isEmpty()) {
            this.mFreeformLayoutAlgorithm.update(freeformTasks, this);
        }
        Task launchTask = stack.getLaunchTarget();
        int launchTaskIndex = launchTask != null ? stack.indexOfStackTask(launchTask) : this.mNumStackTasks - 1;
        if (getInitialFocusState() == 1) {
            int maxBottomOffset = this.mStackBottomOffset + this.mTaskRect.height();
            float maxBottomNormX = getNormalizedXFromFocusedY(maxBottomOffset, 1);
            this.mFocusedRange.offset(0.0f);
            this.mMinScrollP = 0.0f;
            this.mMaxScrollP = Math.max(this.mMinScrollP, (this.mNumStackTasks - 1) - Math.max(0.0f, this.mFocusedRange.getAbsoluteX(maxBottomNormX)));
            if (launchState.launchedFromHome) {
                this.mInitialScrollP = Utilities.clamp(launchTaskIndex, this.mMinScrollP, this.mMaxScrollP);
                return;
            } else {
                this.mInitialScrollP = Utilities.clamp(launchTaskIndex - 1, this.mMinScrollP, this.mMaxScrollP);
                return;
            }
        }
        if (!ssp.hasFreeformWorkspaceSupport() && this.mNumStackTasks == 1) {
            this.mMinScrollP = 0.0f;
            this.mMaxScrollP = 0.0f;
            this.mInitialScrollP = 0.0f;
            return;
        }
        int maxBottomOffset2 = this.mStackBottomOffset + this.mTaskRect.height();
        float maxBottomNormX2 = getNormalizedXFromUnfocusedY(maxBottomOffset2, 1);
        this.mUnfocusedRange.offset(0.0f);
        this.mMinScrollP = 0.0f;
        this.mMaxScrollP = Math.max(this.mMinScrollP, (this.mNumStackTasks - 1) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(maxBottomNormX2)));
        if (launchState.launchedFromHome) {
            scrollToFront = true;
        } else {
            scrollToFront = launchState.launchedViaDockGesture;
        }
        if (launchState.launchedWithAltTab) {
            this.mInitialScrollP = Utilities.clamp(launchTaskIndex, this.mMinScrollP, this.mMaxScrollP);
        } else if (scrollToFront) {
            this.mInitialScrollP = Utilities.clamp(launchTaskIndex, this.mMinScrollP, this.mMaxScrollP);
        } else {
            float initialTopNormX = getNormalizedXFromUnfocusedY(this.mInitialTopOffset, 0);
            this.mInitialScrollP = Math.max(this.mMinScrollP, Math.min(this.mMaxScrollP, this.mNumStackTasks - 2) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(initialTopNormX)));
        }
    }

    public void setTaskOverridesForInitialState(TaskStack stack, boolean ignoreScrollToFront) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        this.mTaskIndexOverrideMap.clear();
        boolean z = !launchState.launchedFromHome ? launchState.launchedViaDockGesture : true;
        if (getInitialFocusState() != 0 || this.mNumStackTasks <= 1) {
            return;
        }
        if (ignoreScrollToFront || !(launchState.launchedWithAltTab || z)) {
            float minBottomTaskNormX = getNormalizedXFromUnfocusedY(this.mSystemInsets.bottom + this.mInitialBottomOffset, 1);
            float maxBottomTaskNormX = getNormalizedXFromUnfocusedY((this.mFocusedTopPeekHeight + this.mTaskRect.height()) - this.mMinMargin, 0);
            float[] initialNormX = this.mNumStackTasks <= 2 ? new float[]{Math.min(maxBottomTaskNormX, minBottomTaskNormX), getNormalizedXFromUnfocusedY(this.mFocusedTopPeekHeight, 0)} : new float[]{minBottomTaskNormX, getNormalizedXFromUnfocusedY(this.mInitialTopOffset, 0)};
            this.mUnfocusedRange.offset(0.0f);
            List<Task> tasks = stack.getStackTasks();
            int taskCount = tasks.size();
            for (int i = taskCount - 1; i >= 0; i--) {
                int indexFromFront = (taskCount - i) - 1;
                if (indexFromFront >= initialNormX.length) {
                    return;
                }
                float newTaskProgress = this.mInitialScrollP + this.mUnfocusedRange.getAbsoluteX(initialNormX[indexFromFront]);
                this.mTaskIndexOverrideMap.put(tasks.get(i).key.id, Float.valueOf(newTaskProgress));
            }
        }
    }

    public void addUnfocusedTaskOverride(Task task, float stackScroll) {
        if (this.mFocusState == 0) {
            return;
        }
        this.mFocusedRange.offset(stackScroll);
        this.mUnfocusedRange.offset(stackScroll);
        float focusedRangeX = this.mFocusedRange.getNormalizedX(this.mTaskIndexMap.get(task.key.id));
        float focusedY = this.mFocusedCurveInterpolator.getInterpolation(focusedRangeX);
        float unfocusedRangeX = this.mUnfocusedCurveInterpolator.getX(focusedY);
        float unfocusedTaskProgress = stackScroll + this.mUnfocusedRange.getAbsoluteX(unfocusedRangeX);
        if (Float.compare(focusedRangeX, unfocusedRangeX) == 0) {
            return;
        }
        this.mTaskIndexOverrideMap.put(task.key.id, Float.valueOf(unfocusedTaskProgress));
    }

    public void addUnfocusedTaskOverride(TaskView taskView, float stackScroll) {
        this.mFocusedRange.offset(stackScroll);
        this.mUnfocusedRange.offset(stackScroll);
        Task task = taskView.getTask();
        int top = taskView.getTop() - this.mTaskRect.top;
        float focusedRangeX = getNormalizedXFromFocusedY(top, 0);
        float unfocusedRangeX = getNormalizedXFromUnfocusedY(top, 0);
        float unfocusedTaskProgress = stackScroll + this.mUnfocusedRange.getAbsoluteX(unfocusedRangeX);
        if (Float.compare(focusedRangeX, unfocusedRangeX) == 0) {
            return;
        }
        this.mTaskIndexOverrideMap.put(task.key.id, Float.valueOf(unfocusedTaskProgress));
    }

    public void clearUnfocusedTaskOverrides() {
        this.mTaskIndexOverrideMap.clear();
    }

    public float updateFocusStateOnScroll(float lastTargetStackScroll, float targetStackScroll, float lastStackScroll) {
        if (targetStackScroll == lastStackScroll) {
            return targetStackScroll;
        }
        float deltaScroll = targetStackScroll - lastStackScroll;
        float deltaTargetScroll = targetStackScroll - lastTargetStackScroll;
        float newScroll = targetStackScroll;
        this.mUnfocusedRange.offset(targetStackScroll);
        for (int i = this.mTaskIndexOverrideMap.size() - 1; i >= 0; i--) {
            int taskId = this.mTaskIndexOverrideMap.keyAt(i);
            float x = this.mTaskIndexMap.get(taskId);
            float overrideX = this.mTaskIndexOverrideMap.get(taskId, Float.valueOf(0.0f)).floatValue();
            float newOverrideX = overrideX + deltaScroll;
            if (isInvalidOverrideX(x, overrideX, newOverrideX)) {
                this.mTaskIndexOverrideMap.removeAt(i);
            } else if ((overrideX >= x && deltaScroll <= 0.0f) || (overrideX <= x && deltaScroll >= 0.0f)) {
                this.mTaskIndexOverrideMap.put(taskId, Float.valueOf(newOverrideX));
            } else {
                newScroll = lastStackScroll;
                float newOverrideX2 = overrideX - deltaTargetScroll;
                if (isInvalidOverrideX(x, overrideX, newOverrideX2)) {
                    this.mTaskIndexOverrideMap.removeAt(i);
                } else {
                    this.mTaskIndexOverrideMap.put(taskId, Float.valueOf(newOverrideX2));
                }
            }
        }
        return newScroll;
    }

    private boolean isInvalidOverrideX(float x, float overrideX, float newOverrideX) {
        boolean outOfBounds = this.mUnfocusedRange.getNormalizedX(newOverrideX) < 0.0f || this.mUnfocusedRange.getNormalizedX(newOverrideX) > 1.0f;
        if (outOfBounds) {
            return true;
        }
        if (overrideX < x || x < newOverrideX) {
            return overrideX <= x && x <= newOverrideX;
        }
        return true;
    }

    public int getInitialFocusState() {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        RecentsDebugFlags debugFlags = Recents.getDebugFlags();
        if (debugFlags.isPagingEnabled() || launchState.launchedWithAltTab) {
            return 1;
        }
        return 0;
    }

    public TaskViewTransform getBackOfStackTransform() {
        return this.mBackOfStackTransform;
    }

    public TaskViewTransform getFrontOfStackTransform() {
        return this.mFrontOfStackTransform;
    }

    public StackState getStackState() {
        return this.mState;
    }

    public boolean isInitialized() {
        return !this.mStackRect.isEmpty();
    }

    public VisibilityReport computeStackVisibilityReport(ArrayList<Task> tasks) {
        if (tasks.size() <= 1) {
            return new VisibilityReport(1, 1);
        }
        if (this.mNumStackTasks == 0) {
            return new VisibilityReport(Math.max(this.mNumFreeformTasks, 1), Math.max(this.mNumFreeformTasks, 1));
        }
        TaskViewTransform tmpTransform = new TaskViewTransform();
        Range currentRange = ((float) getInitialFocusState()) > 0.0f ? this.mFocusedRange : this.mUnfocusedRange;
        currentRange.offset(this.mInitialScrollP);
        int taskBarHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.recents_task_view_header_height);
        int numVisibleTasks = Math.max(this.mNumFreeformTasks, 1);
        int numVisibleThumbnails = Math.max(this.mNumFreeformTasks, 1);
        float prevScreenY = 2.1474836E9f;
        int i = tasks.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            Task task = tasks.get(i);
            if (!task.isFreeformTask()) {
                float taskProgress = getStackScrollForTask(task);
                if (currentRange.isInRange(taskProgress)) {
                    boolean isFrontMostTaskInGroup = task.group != null ? task.group.isFrontMostTask(task) : true;
                    if (isFrontMostTaskInGroup) {
                        getStackTransform(taskProgress, taskProgress, this.mInitialScrollP, this.mFocusState, tmpTransform, null, false, false);
                        float screenY = tmpTransform.rect.top;
                        boolean hasVisibleThumbnail = prevScreenY - screenY > ((float) taskBarHeight);
                        if (hasVisibleThumbnail) {
                            numVisibleThumbnails++;
                            numVisibleTasks++;
                            prevScreenY = screenY;
                        } else {
                            for (int j = i; j >= 0; j--) {
                                numVisibleTasks++;
                                if (!currentRange.isInRange(getStackScrollForTask(tasks.get(j)))) {
                                }
                            }
                        }
                    } else if (!isFrontMostTaskInGroup) {
                        numVisibleTasks++;
                    }
                } else {
                    continue;
                }
            }
            i--;
        }
        return new VisibilityReport(numVisibleTasks, numVisibleThumbnails);
    }

    public TaskViewTransform getStackTransform(Task task, float stackScroll, TaskViewTransform transformOut, TaskViewTransform frontTransform) {
        return getStackTransform(task, stackScroll, this.mFocusState, transformOut, frontTransform, false, false);
    }

    public TaskViewTransform getStackTransform(Task task, float stackScroll, TaskViewTransform transformOut, TaskViewTransform frontTransform, boolean ignoreTaskOverrides) {
        return getStackTransform(task, stackScroll, this.mFocusState, transformOut, frontTransform, false, ignoreTaskOverrides);
    }

    public TaskViewTransform getStackTransform(Task task, float stackScroll, int focusState, TaskViewTransform transformOut, TaskViewTransform frontTransform, boolean forceUpdate, boolean ignoreTaskOverrides) {
        float taskProgress;
        if (this.mFreeformLayoutAlgorithm.isTransformAvailable(task, this)) {
            this.mFreeformLayoutAlgorithm.getTransform(task, transformOut, this);
            return transformOut;
        }
        int nonOverrideTaskProgress = this.mTaskIndexMap.get(task.key.id, -1);
        if (task == null || nonOverrideTaskProgress == -1) {
            transformOut.reset();
            return transformOut;
        }
        if (ignoreTaskOverrides) {
            taskProgress = nonOverrideTaskProgress;
        } else {
            taskProgress = getStackScrollForTask(task);
        }
        getStackTransform(taskProgress, nonOverrideTaskProgress, stackScroll, focusState, transformOut, frontTransform, false, forceUpdate);
        return transformOut;
    }

    public TaskViewTransform getStackTransformScreenCoordinates(Task task, float stackScroll, TaskViewTransform transformOut, TaskViewTransform frontTransform, Rect windowOverrideRect) {
        TaskViewTransform transform = getStackTransform(task, stackScroll, this.mFocusState, transformOut, frontTransform, true, false);
        return transformToScreenCoordinates(transform, windowOverrideRect);
    }

    public TaskViewTransform transformToScreenCoordinates(TaskViewTransform transformOut, Rect windowOverrideRect) {
        Rect windowRect;
        if (windowOverrideRect != null) {
            windowRect = windowOverrideRect;
        } else {
            windowRect = Recents.getSystemServices().getWindowRect();
        }
        transformOut.rect.offset(windowRect.left, windowRect.top);
        return transformOut;
    }

    public void getStackTransform(float taskProgress, float nonOverrideTaskProgress, float stackScroll, int focusState, TaskViewTransform transformOut, TaskViewTransform frontTransform, boolean ignoreSingleTaskCase, boolean forceUpdate) {
        int y;
        float z;
        float dimAlpha;
        float viewOutlineAlpha;
        boolean z2;
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mUnfocusedRange.offset(stackScroll);
        this.mFocusedRange.offset(stackScroll);
        boolean unfocusedVisible = this.mUnfocusedRange.isInRange(taskProgress);
        boolean focusedVisible = this.mFocusedRange.isInRange(taskProgress);
        if (!forceUpdate && !unfocusedVisible && !focusedVisible) {
            transformOut.reset();
            return;
        }
        this.mUnfocusedRange.offset(stackScroll);
        this.mFocusedRange.offset(stackScroll);
        float unfocusedRangeX = this.mUnfocusedRange.getNormalizedX(taskProgress);
        float focusedRangeX = this.mFocusedRange.getNormalizedX(taskProgress);
        float boundedStackScroll = Utilities.clamp(stackScroll, this.mMinScrollP, this.mMaxScrollP);
        this.mUnfocusedRange.offset(boundedStackScroll);
        this.mFocusedRange.offset(boundedStackScroll);
        float boundedScrollUnfocusedRangeX = this.mUnfocusedRange.getNormalizedX(taskProgress);
        float boundedScrollUnfocusedNonOverrideRangeX = this.mUnfocusedRange.getNormalizedX(nonOverrideTaskProgress);
        float lowerBoundedStackScroll = Utilities.clamp(stackScroll, -3.4028235E38f, this.mMaxScrollP);
        this.mUnfocusedRange.offset(lowerBoundedStackScroll);
        this.mFocusedRange.offset(lowerBoundedStackScroll);
        float lowerBoundedUnfocusedRangeX = this.mUnfocusedRange.getNormalizedX(taskProgress);
        float lowerBoundedFocusedRangeX = this.mFocusedRange.getNormalizedX(taskProgress);
        int x = (this.mStackRect.width() - this.mTaskRect.width()) / 2;
        if (!ssp.hasFreeformWorkspaceSupport() && this.mNumStackTasks == 1 && !ignoreSingleTaskCase) {
            float tmpP = (this.mMinScrollP - stackScroll) / this.mNumStackTasks;
            int centerYOffset = (this.mStackRect.top - this.mTaskRect.top) + (((this.mStackRect.height() - this.mSystemInsets.bottom) - this.mTaskRect.height()) / 2);
            y = centerYOffset + getYForDeltaP(tmpP, 0.0f);
            z = this.mMaxTranslationZ;
            dimAlpha = 0.0f;
            viewOutlineAlpha = 1.0f;
        } else {
            int unfocusedY = (int) ((1.0f - this.mUnfocusedCurveInterpolator.getInterpolation(unfocusedRangeX)) * this.mStackRect.height());
            int focusedY = (int) ((1.0f - this.mFocusedCurveInterpolator.getInterpolation(focusedRangeX)) * this.mStackRect.height());
            float unfocusedDim = this.mUnfocusedDimCurveInterpolator.getInterpolation(lowerBoundedUnfocusedRangeX);
            float focusedDim = this.mFocusedDimCurveInterpolator.getInterpolation(lowerBoundedFocusedRangeX);
            if (this.mNumStackTasks <= 2 && nonOverrideTaskProgress == 0.0f) {
                if (boundedScrollUnfocusedRangeX >= 0.5f) {
                    unfocusedDim = 0.0f;
                } else {
                    float offset = this.mUnfocusedDimCurveInterpolator.getInterpolation(0.5f);
                    unfocusedDim = (unfocusedDim - offset) * (0.25f / (0.25f - offset));
                }
            }
            y = (this.mStackRect.top - this.mTaskRect.top) + ((int) Utilities.mapRange(focusState, unfocusedY, focusedY));
            z = Utilities.mapRange(Utilities.clamp01(boundedScrollUnfocusedNonOverrideRangeX), this.mMinTranslationZ, this.mMaxTranslationZ);
            dimAlpha = Utilities.mapRange(focusState, unfocusedDim, focusedDim);
            viewOutlineAlpha = Utilities.mapRange(Utilities.clamp01(boundedScrollUnfocusedRangeX), 0.0f, 2.0f);
        }
        transformOut.scale = 1.0f;
        transformOut.alpha = 1.0f;
        transformOut.translationZ = z;
        transformOut.dimAlpha = dimAlpha;
        transformOut.viewOutlineAlpha = viewOutlineAlpha;
        transformOut.rect.set(this.mTaskRect);
        transformOut.rect.offset(x, y);
        Utilities.scaleRectAboutCenter(transformOut.rect, transformOut.scale);
        if (transformOut.rect.top >= this.mStackRect.bottom) {
            z2 = false;
        } else {
            z2 = frontTransform == null || transformOut.rect.top != frontTransform.rect.top;
        }
        transformOut.visible = z2;
    }

    public Rect getUntransformedTaskViewBounds() {
        return new Rect(this.mTaskRect);
    }

    float getStackScrollForTask(Task t) {
        Float overrideP = this.mTaskIndexOverrideMap.get(t.key.id, null);
        if (overrideP == null) {
            return this.mTaskIndexMap.get(t.key.id, 0);
        }
        return overrideP.floatValue();
    }

    float getStackScrollForTaskIgnoreOverrides(Task t) {
        return this.mTaskIndexMap.get(t.key.id, 0);
    }

    float getStackScrollForTaskAtInitialOffset(Task t) {
        float normX = getNormalizedXFromUnfocusedY(this.mInitialTopOffset, 0);
        this.mUnfocusedRange.offset(0.0f);
        return Utilities.clamp(this.mTaskIndexMap.get(t.key.id, 0) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(normX)), this.mMinScrollP, this.mMaxScrollP);
    }

    public float getDeltaPForY(int downY, int y) {
        float deltaP = ((y - downY) / this.mStackRect.height()) * this.mUnfocusedCurveInterpolator.getArcLength();
        return -deltaP;
    }

    public int getYForDeltaP(float downScrollP, float p) {
        int y = (int) ((p - downScrollP) * this.mStackRect.height() * (1.0f / this.mUnfocusedCurveInterpolator.getArcLength()));
        return -y;
    }

    public void getTaskStackBounds(Rect displayRect, Rect windowRect, int topInset, int rightInset, Rect taskStackBounds) {
        taskStackBounds.set(windowRect.left, windowRect.top + topInset, windowRect.right - rightInset, windowRect.bottom);
        int sideMargin = getScaleForExtent(windowRect, displayRect, this.mBaseSideMargin, this.mMinMargin, 0);
        int targetStackWidth = taskStackBounds.width() - (sideMargin * 2);
        if (Utilities.getAppConfiguration(this.mContext).orientation == 2) {
            Rect portraitDisplayRect = new Rect(0, 0, Math.min(displayRect.width(), displayRect.height()), Math.max(displayRect.width(), displayRect.height()));
            int portraitSideMargin = getScaleForExtent(portraitDisplayRect, portraitDisplayRect, this.mBaseSideMargin, this.mMinMargin, 0);
            targetStackWidth = Math.min(targetStackWidth, portraitDisplayRect.width() - (portraitSideMargin * 2));
        }
        taskStackBounds.inset((taskStackBounds.width() - targetStackWidth) / 2, 0);
    }

    public static int getDimensionForDevice(Context ctx, int phoneResId, int tabletResId, int xlargeTabletResId) {
        return getDimensionForDevice(ctx, phoneResId, phoneResId, tabletResId, tabletResId, xlargeTabletResId, xlargeTabletResId);
    }

    public static int getDimensionForDevice(Context ctx, int phonePortResId, int phoneLandResId, int tabletPortResId, int tabletLandResId, int xlargeTabletPortResId, int xlargeTabletLandResId) {
        RecentsConfiguration config = Recents.getConfiguration();
        Resources res = ctx.getResources();
        boolean isLandscape = Utilities.getAppConfiguration(ctx).orientation == 2;
        if (config.isXLargeScreen) {
            if (!isLandscape) {
                xlargeTabletLandResId = xlargeTabletPortResId;
            }
            return res.getDimensionPixelSize(xlargeTabletLandResId);
        }
        if (config.isLargeScreen) {
            if (!isLandscape) {
                tabletLandResId = tabletPortResId;
            }
            return res.getDimensionPixelSize(tabletLandResId);
        }
        if (!isLandscape) {
            phoneLandResId = phonePortResId;
        }
        return res.getDimensionPixelSize(phoneLandResId);
    }

    private float getNormalizedXFromUnfocusedY(float y, int fromSide) {
        float offset;
        if (fromSide == 0) {
            offset = this.mStackRect.height() - y;
        } else {
            offset = y;
        }
        float offsetPct = offset / this.mStackRect.height();
        return this.mUnfocusedCurveInterpolator.getX(offsetPct);
    }

    private float getNormalizedXFromFocusedY(float y, int fromSide) {
        float offset;
        if (fromSide == 0) {
            offset = this.mStackRect.height() - y;
        } else {
            offset = y;
        }
        float offsetPct = offset / this.mStackRect.height();
        return this.mFocusedCurveInterpolator.getX(offsetPct);
    }

    private Path constructFocusedCurve() {
        float topPeekHeightPct = this.mFocusedTopPeekHeight / this.mStackRect.height();
        float bottomPeekHeightPct = (this.mStackBottomOffset + this.mFocusedBottomPeekHeight) / this.mStackRect.height();
        float minBottomPeekHeightPct = ((this.mFocusedTopPeekHeight + this.mTaskRect.height()) - this.mMinMargin) / this.mStackRect.height();
        Path p = new Path();
        p.moveTo(0.0f, 1.0f);
        p.lineTo(0.5f, 1.0f - topPeekHeightPct);
        p.lineTo(1.0f - (0.5f / this.mFocusedRange.relativeMax), Math.max(1.0f - minBottomPeekHeightPct, bottomPeekHeightPct));
        p.lineTo(1.0f, 0.0f);
        return p;
    }

    private Path constructUnfocusedCurve() {
        float topPeekHeightPct = this.mFocusedTopPeekHeight / this.mStackRect.height();
        float slope = ((1.0f - topPeekHeightPct) - 0.975f) / 0.099999994f;
        float b = 1.0f - (0.4f * slope);
        float cpoint2Y = (0.65f * slope) + b;
        Path p = new Path();
        p.moveTo(0.0f, 1.0f);
        p.cubicTo(0.0f, 1.0f, 0.4f, 0.975f, 0.5f, 1.0f - topPeekHeightPct);
        p.cubicTo(0.5f, 1.0f - topPeekHeightPct, 0.65f, cpoint2Y, 1.0f, 0.0f);
        return p;
    }

    private Path constructFocusedDimCurve() {
        Path p = new Path();
        p.moveTo(0.0f, 0.25f);
        p.lineTo(0.5f, 0.0f);
        p.lineTo((0.5f / this.mFocusedRange.relativeMax) + 0.5f, 0.25f);
        p.lineTo(1.0f, 0.25f);
        return p;
    }

    private Path constructUnfocusedDimCurve() {
        float focusX = getNormalizedXFromUnfocusedY(this.mInitialTopOffset, 0);
        float cpoint2X = focusX + ((1.0f - focusX) / 2.0f);
        Path p = new Path();
        p.moveTo(0.0f, 0.25f);
        p.cubicTo(0.5f * focusX, 0.25f, 0.75f * focusX, 0.1875f, focusX, 0.0f);
        p.cubicTo(cpoint2X, 0.0f, cpoint2X, 0.15f, 1.0f, 0.15f);
        return p;
    }

    private int getScaleForExtent(Rect instance, Rect other, int value, int minValue, int extent) {
        if (extent == 0) {
            float scale = Utilities.clamp01(instance.width() / other.width());
            return Math.max(minValue, (int) (value * scale));
        }
        if (extent == 1) {
            float scale2 = Utilities.clamp01(instance.height() / other.height());
            return Math.max(minValue, (int) (value * scale2));
        }
        return value;
    }

    private void updateFrontBackTransforms() {
        if (this.mStackRect.isEmpty()) {
            return;
        }
        float min = Utilities.mapRange(this.mFocusState, this.mUnfocusedRange.relativeMin, this.mFocusedRange.relativeMin);
        float max = Utilities.mapRange(this.mFocusState, this.mUnfocusedRange.relativeMax, this.mFocusedRange.relativeMax);
        getStackTransform(min, min, 0.0f, this.mFocusState, this.mBackOfStackTransform, null, true, true);
        getStackTransform(max, max, 0.0f, this.mFocusState, this.mFrontOfStackTransform, null, true, true);
        this.mBackOfStackTransform.visible = true;
        this.mFrontOfStackTransform.visible = true;
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.print("TaskStackLayoutAlgorithm");
        writer.write(" numStackTasks=");
        writer.write(this.mNumStackTasks);
        writer.println();
        writer.print(innerPrefix);
        writer.print("insets=");
        writer.print(Utilities.dumpRect(this.mSystemInsets));
        writer.print(" stack=");
        writer.print(Utilities.dumpRect(this.mStackRect));
        writer.print(" task=");
        writer.print(Utilities.dumpRect(this.mTaskRect));
        writer.print(" freeform=");
        writer.print(Utilities.dumpRect(this.mFreeformRect));
        writer.print(" actionButton=");
        writer.print(Utilities.dumpRect(this.mStackActionButtonRect));
        writer.println();
        writer.print(innerPrefix);
        writer.print("minScroll=");
        writer.print(this.mMinScrollP);
        writer.print(" maxScroll=");
        writer.print(this.mMaxScrollP);
        writer.print(" initialScroll=");
        writer.print(this.mInitialScrollP);
        writer.println();
        writer.print(innerPrefix);
        writer.print("focusState=");
        writer.print(this.mFocusState);
        writer.println();
        if (this.mTaskIndexOverrideMap.size() <= 0) {
            return;
        }
        for (int i = this.mTaskIndexOverrideMap.size() - 1; i >= 0; i--) {
            int taskId = this.mTaskIndexOverrideMap.keyAt(i);
            float x = this.mTaskIndexMap.get(taskId);
            float overrideX = this.mTaskIndexOverrideMap.get(taskId, Float.valueOf(0.0f)).floatValue();
            writer.print(innerPrefix);
            writer.print("taskId= ");
            writer.print(taskId);
            writer.print(" x= ");
            writer.print(x);
            writer.print(" overrideX= ");
            writer.print(overrideX);
            writer.println();
        }
    }
}
