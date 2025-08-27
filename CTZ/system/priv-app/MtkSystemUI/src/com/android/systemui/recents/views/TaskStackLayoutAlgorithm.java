package com.android.systemui.recents.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.ViewDebug;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.FreePathInterpolator;
import com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm;
import com.android.systemui.recents.views.lowram.TaskStackLowRamLayoutAlgorithm;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.TaskStack;
import com.android.systemui.shared.recents.utilities.Utilities;
import java.io.PrintWriter;
import java.util.ArrayList;

/* loaded from: classes.dex */
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
    private TaskStackLayoutAlgorithm2 mFocusedRange;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusedTopPeekHeight;

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
    public int mMaxTranslationZ;
    private int mMinMargin;

    @ViewDebug.ExportedProperty(category = "recents")
    float mMinScrollP;

    @ViewDebug.ExportedProperty(category = "recents")
    int mMinTranslationZ;

    @ViewDebug.ExportedProperty(category = "recents")
    int mNumStackTasks;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mStackBottomOffset;
    TaskGridLayoutAlgorithm mTaskGridLayoutAlgorithm;
    TaskStackLowRamLayoutAlgorithm mTaskStackLowRamLayoutAlgorithm;
    private int mTitleBarHeight;
    private Path mUnfocusedCurve;
    private FreePathInterpolator mUnfocusedCurveInterpolator;
    private Path mUnfocusedDimCurve;
    private FreePathInterpolator mUnfocusedDimCurveInterpolator;
    private TaskStackLayoutAlgorithm2 mUnfocusedRange;

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mTaskRect = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mStackRect = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mSystemInsets = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStackActionButtonRect = new Rect();
    private SparseIntArray mTaskIndexMap = new SparseIntArray();
    private SparseArray<Float> mTaskIndexOverrideMap = new SparseArray<>();
    TaskViewTransform mBackOfStackTransform = new TaskViewTransform();
    TaskViewTransform mFrontOfStackTransform = new TaskViewTransform();

    public interface TaskStackLayoutAlgorithmCallbacks {
        void onFocusStateChanged(int i, int i2);
    }

    boolean useGridLayout() {
        return Recents.getConfiguration().isGridEnabled;
    }

    public static class VisibilityReport {
        public int numVisibleTasks;
        public int numVisibleThumbnails;

        public VisibilityReport(int i, int i2) {
            this.numVisibleTasks = i;
            this.numVisibleThumbnails = i2;
        }
    }

    public TaskStackLayoutAlgorithm(Context context, TaskStackLayoutAlgorithmCallbacks taskStackLayoutAlgorithmCallbacks) {
        this.mContext = context;
        this.mCb = taskStackLayoutAlgorithmCallbacks;
        this.mTaskGridLayoutAlgorithm = new TaskGridLayoutAlgorithm(context);
        this.mTaskStackLowRamLayoutAlgorithm = new TaskStackLowRamLayoutAlgorithm(context);
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        Resources resources = context.getResources();
        this.mFocusedRange = new TaskStackLayoutAlgorithm2(resources.getFloat(R.integer.recents_layout_focused_range_min), resources.getFloat(R.integer.recents_layout_focused_range_max));
        this.mUnfocusedRange = new TaskStackLayoutAlgorithm2(resources.getFloat(R.integer.recents_layout_unfocused_range_min), resources.getFloat(R.integer.recents_layout_unfocused_range_max));
        this.mFocusState = getInitialFocusState();
        this.mFocusedTopPeekHeight = resources.getDimensionPixelSize(R.dimen.recents_layout_top_peek_size);
        this.mFocusedBottomPeekHeight = resources.getDimensionPixelSize(R.dimen.recents_layout_bottom_peek_size);
        this.mMinTranslationZ = resources.getDimensionPixelSize(R.dimen.recents_layout_z_min);
        this.mMaxTranslationZ = resources.getDimensionPixelSize(R.dimen.recents_layout_z_max);
        this.mBaseInitialTopOffset = getDimensionForDevice(context, R.dimen.recents_layout_initial_top_offset_phone_port, R.dimen.recents_layout_initial_top_offset_phone_land, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet);
        this.mBaseInitialBottomOffset = getDimensionForDevice(context, R.dimen.recents_layout_initial_bottom_offset_phone_port, R.dimen.recents_layout_initial_bottom_offset_phone_land, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet);
        this.mTaskGridLayoutAlgorithm.reloadOnConfigurationChange(context);
        this.mTaskStackLowRamLayoutAlgorithm.reloadOnConfigurationChange(context);
        this.mMinMargin = resources.getDimensionPixelSize(R.dimen.recents_layout_min_margin);
        this.mBaseTopMargin = getDimensionForDevice(context, R.dimen.recents_layout_top_margin_phone, R.dimen.recents_layout_top_margin_tablet, R.dimen.recents_layout_top_margin_tablet_xlarge, R.dimen.recents_layout_top_margin_tablet);
        this.mBaseSideMargin = getDimensionForDevice(context, R.dimen.recents_layout_side_margin_phone, R.dimen.recents_layout_side_margin_tablet, R.dimen.recents_layout_side_margin_tablet_xlarge, R.dimen.recents_layout_side_margin_tablet);
        this.mBaseBottomMargin = resources.getDimensionPixelSize(R.dimen.recents_layout_bottom_margin);
        this.mTitleBarHeight = getDimensionForDevice(this.mContext, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_grid_task_view_header_height);
    }

    public void reset() {
        this.mTaskIndexOverrideMap.clear();
        setFocusState(getInitialFocusState());
    }

    public boolean setSystemInsets(Rect rect) {
        boolean z = !this.mSystemInsets.equals(rect);
        this.mSystemInsets.set(rect);
        this.mTaskGridLayoutAlgorithm.setSystemInsets(rect);
        this.mTaskStackLowRamLayoutAlgorithm.setSystemInsets(rect);
        return z;
    }

    public void setFocusState(int i) {
        int i2 = this.mFocusState;
        this.mFocusState = i;
        updateFrontBackTransforms();
        if (this.mCb != null) {
            this.mCb.onFocusStateChanged(i2, i);
        }
    }

    public int getFocusState() {
        return this.mFocusState;
    }

    public void initialize(Rect rect, Rect rect2, Rect rect3) {
        Rect rect4 = new Rect(this.mStackRect);
        int scaleForExtent = getScaleForExtent(rect2, rect, this.mBaseTopMargin, this.mMinMargin, 1);
        int scaleForExtent2 = getScaleForExtent(rect2, rect, this.mBaseBottomMargin, this.mMinMargin, 1);
        this.mInitialTopOffset = getScaleForExtent(rect2, rect, this.mBaseInitialTopOffset, this.mMinMargin, 1);
        this.mInitialBottomOffset = this.mBaseInitialBottomOffset;
        this.mStackBottomOffset = this.mSystemInsets.bottom + scaleForExtent2;
        this.mStackRect.set(rect3);
        this.mStackRect.top += scaleForExtent;
        this.mStackActionButtonRect.set(this.mStackRect.left, this.mStackRect.top - scaleForExtent, this.mStackRect.right, this.mStackRect.top + this.mFocusedTopPeekHeight);
        this.mTaskRect.set(this.mStackRect.left, this.mStackRect.top, this.mStackRect.right, this.mStackRect.top + ((this.mStackRect.height() - this.mInitialTopOffset) - this.mStackBottomOffset));
        if (this.mTaskRect.width() <= 0 || this.mTaskRect.height() <= 0) {
            Log.e("TaskStackLayoutAlgorithm", "Invalid task rect: taskRect=" + this.mTaskRect + " stackRect=" + this.mStackRect + " displayRect=" + rect + " windowRect=" + rect2 + " taskStackBounds=" + rect3);
        }
        if (!rect4.equals(this.mStackRect)) {
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
        this.mTaskGridLayoutAlgorithm.initialize(rect2);
        this.mTaskStackLowRamLayoutAlgorithm.initialize(rect2);
    }

    public void update(TaskStack taskStack, ArraySet<Task.TaskKey> arraySet, RecentsActivityLaunchState recentsActivityLaunchState, float f) {
        int iIndexOfTask;
        float minScrollP;
        float fMax;
        Recents.getSystemServices();
        this.mTaskIndexMap.clear();
        ArrayList<Task> tasks = taskStack.getTasks();
        if (tasks.isEmpty()) {
            this.mFrontMostTaskP = 0.0f;
            this.mInitialScrollP = 0.0f;
            this.mMaxScrollP = 0.0f;
            this.mMinScrollP = 0.0f;
            this.mNumStackTasks = 0;
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (!arraySet.contains(task.key)) {
                arrayList.add(task);
            }
        }
        this.mNumStackTasks = arrayList.size();
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            this.mTaskIndexMap.put(((Task) arrayList.get(i2)).key.id, i2);
        }
        Task launchTarget = taskStack.getLaunchTarget();
        boolean z = true;
        if (launchTarget == null) {
            iIndexOfTask = this.mNumStackTasks - 1;
        } else {
            iIndexOfTask = taskStack.indexOfTask(launchTarget);
        }
        if (getInitialFocusState() == 1) {
            float normalizedXFromFocusedY = getNormalizedXFromFocusedY(this.mStackBottomOffset + this.mTaskRect.height(), 1);
            this.mFocusedRange.offset(0.0f);
            this.mMinScrollP = 0.0f;
            this.mMaxScrollP = Math.max(this.mMinScrollP, (this.mNumStackTasks - 1) - Math.max(0.0f, this.mFocusedRange.getAbsoluteX(normalizedXFromFocusedY)));
            if (recentsActivityLaunchState.launchedFromHome || recentsActivityLaunchState.launchedFromPipApp || recentsActivityLaunchState.launchedWithNextPipApp) {
                this.mInitialScrollP = Utilities.clamp(iIndexOfTask, this.mMinScrollP, this.mMaxScrollP);
                return;
            } else {
                this.mInitialScrollP = Utilities.clamp(iIndexOfTask - 1, this.mMinScrollP, this.mMaxScrollP);
                return;
            }
        }
        if (this.mNumStackTasks != 1) {
            float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY(this.mStackBottomOffset + this.mTaskRect.height(), 1);
            this.mUnfocusedRange.offset(0.0f);
            if (Recents.getConfiguration().isLowRamDevice) {
                minScrollP = this.mTaskStackLowRamLayoutAlgorithm.getMinScrollP();
            } else {
                minScrollP = 0.0f;
            }
            this.mMinScrollP = minScrollP;
            if (Recents.getConfiguration().isLowRamDevice) {
                fMax = this.mTaskStackLowRamLayoutAlgorithm.getMaxScrollP(size);
            } else {
                fMax = Math.max(this.mMinScrollP, (this.mNumStackTasks - 1) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(normalizedXFromUnfocusedY)));
            }
            this.mMaxScrollP = fMax;
            if (!recentsActivityLaunchState.launchedFromHome && !recentsActivityLaunchState.launchedFromPipApp && !recentsActivityLaunchState.launchedWithNextPipApp && !recentsActivityLaunchState.launchedViaDockGesture) {
                z = false;
            }
            if (recentsActivityLaunchState.launchedWithAltTab) {
                this.mInitialScrollP = Utilities.clamp(iIndexOfTask, this.mMinScrollP, this.mMaxScrollP);
                return;
            }
            if (0.0f <= f && f <= 1.0f) {
                this.mInitialScrollP = Utilities.mapRange(f, this.mMinScrollP, this.mMaxScrollP);
                return;
            }
            if (Recents.getConfiguration().isLowRamDevice) {
                this.mInitialScrollP = this.mTaskStackLowRamLayoutAlgorithm.getInitialScrollP(this.mNumStackTasks, z);
                return;
            } else if (z) {
                this.mInitialScrollP = Utilities.clamp(iIndexOfTask, this.mMinScrollP, this.mMaxScrollP);
                return;
            } else {
                this.mInitialScrollP = Math.max(this.mMinScrollP, Math.min(this.mMaxScrollP, this.mNumStackTasks - 2) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(getNormalizedXFromUnfocusedY(this.mInitialTopOffset, 0))));
                return;
            }
        }
        this.mMinScrollP = 0.0f;
        this.mMaxScrollP = 0.0f;
        this.mInitialScrollP = 0.0f;
    }

    public void setTaskOverridesForInitialState(TaskStack taskStack, boolean z) {
        float[] fArr;
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        this.mTaskIndexOverrideMap.clear();
        boolean z2 = launchState.launchedFromHome || launchState.launchedFromPipApp || launchState.launchedWithNextPipApp || launchState.launchedViaDockGesture;
        if (getInitialFocusState() == 0 && this.mNumStackTasks > 1) {
            if (z || (!launchState.launchedWithAltTab && !z2)) {
                float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY(this.mSystemInsets.bottom + this.mInitialBottomOffset, 1);
                float normalizedXFromUnfocusedY2 = getNormalizedXFromUnfocusedY((this.mFocusedTopPeekHeight + this.mTaskRect.height()) - this.mMinMargin, 0);
                if (this.mNumStackTasks <= 2) {
                    fArr = new float[]{Math.min(normalizedXFromUnfocusedY2, normalizedXFromUnfocusedY), getNormalizedXFromUnfocusedY(this.mFocusedTopPeekHeight, 0)};
                } else {
                    fArr = new float[]{normalizedXFromUnfocusedY, getNormalizedXFromUnfocusedY(this.mInitialTopOffset, 0)};
                }
                this.mUnfocusedRange.offset(0.0f);
                ArrayList<Task> tasks = taskStack.getTasks();
                int size = tasks.size();
                for (int i = size - 1; i >= 0; i--) {
                    int i2 = (size - i) - 1;
                    if (i2 < fArr.length) {
                        this.mTaskIndexOverrideMap.put(tasks.get(i).key.id, Float.valueOf(this.mInitialScrollP + this.mUnfocusedRange.getAbsoluteX(fArr[i2])));
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public void addUnfocusedTaskOverride(Task task, float f) {
        if (this.mFocusState != 0) {
            this.mFocusedRange.offset(f);
            this.mUnfocusedRange.offset(f);
            float normalizedX = this.mFocusedRange.getNormalizedX(this.mTaskIndexMap.get(task.key.id));
            float x = this.mUnfocusedCurveInterpolator.getX(this.mFocusedCurveInterpolator.getInterpolation(normalizedX));
            float absoluteX = f + this.mUnfocusedRange.getAbsoluteX(x);
            if (Float.compare(normalizedX, x) != 0) {
                this.mTaskIndexOverrideMap.put(task.key.id, Float.valueOf(absoluteX));
            }
        }
    }

    public void addUnfocusedTaskOverride(TaskView taskView, float f) {
        this.mFocusedRange.offset(f);
        this.mUnfocusedRange.offset(f);
        Task task = taskView.getTask();
        float top = taskView.getTop() - this.mTaskRect.top;
        float normalizedXFromFocusedY = getNormalizedXFromFocusedY(top, 0);
        float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY(top, 0);
        float absoluteX = f + this.mUnfocusedRange.getAbsoluteX(normalizedXFromUnfocusedY);
        if (Float.compare(normalizedXFromFocusedY, normalizedXFromUnfocusedY) != 0) {
            this.mTaskIndexOverrideMap.put(task.key.id, Float.valueOf(absoluteX));
        }
    }

    public void clearUnfocusedTaskOverrides() {
        this.mTaskIndexOverrideMap.clear();
    }

    public float updateFocusStateOnScroll(float f, float f2, float f3) {
        if (f2 == f3 || Recents.getConfiguration().isLowRamDevice) {
            return f2;
        }
        float f4 = f2 - f3;
        float f5 = f2 - f;
        this.mUnfocusedRange.offset(f2);
        for (int size = this.mTaskIndexOverrideMap.size() - 1; size >= 0; size--) {
            int iKeyAt = this.mTaskIndexOverrideMap.keyAt(size);
            float f6 = this.mTaskIndexMap.get(iKeyAt);
            float fFloatValue = this.mTaskIndexOverrideMap.get(iKeyAt, Float.valueOf(0.0f)).floatValue();
            float f7 = fFloatValue + f4;
            if (isInvalidOverrideX(f6, fFloatValue, f7)) {
                this.mTaskIndexOverrideMap.removeAt(size);
            } else if ((fFloatValue >= f6 && f4 <= 0.0f) || (fFloatValue <= f6 && f4 >= 0.0f)) {
                this.mTaskIndexOverrideMap.put(iKeyAt, Float.valueOf(f7));
            } else {
                float f8 = fFloatValue - f5;
                if (isInvalidOverrideX(f6, fFloatValue, f8)) {
                    this.mTaskIndexOverrideMap.removeAt(size);
                } else {
                    this.mTaskIndexOverrideMap.put(iKeyAt, Float.valueOf(f8));
                }
                f2 = f3;
            }
        }
        return f2;
    }

    private boolean isInvalidOverrideX(float f, float f2, float f3) {
        if (this.mUnfocusedRange.getNormalizedX(f3) < 0.0f || this.mUnfocusedRange.getNormalizedX(f3) > 1.0f) {
            return true;
        }
        if (f2 < f || f < f3) {
            return f2 <= f && f <= f3;
        }
        return true;
    }

    public int getInitialFocusState() {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        Recents.getDebugFlags();
        if (launchState.launchedWithAltTab) {
            return 1;
        }
        return 0;
    }

    public Rect getStackActionButtonRect() {
        return useGridLayout() ? this.mTaskGridLayoutAlgorithm.getStackActionButtonRect() : this.mStackActionButtonRect;
    }

    public TaskViewTransform getBackOfStackTransform() {
        return this.mBackOfStackTransform;
    }

    public TaskViewTransform getFrontOfStackTransform() {
        return this.mFrontOfStackTransform;
    }

    public boolean isInitialized() {
        return !this.mStackRect.isEmpty();
    }

    public VisibilityReport computeStackVisibilityReport(ArrayList<Task> arrayList) throws Resources.NotFoundException {
        int i;
        int i2;
        if (useGridLayout()) {
            return this.mTaskGridLayoutAlgorithm.computeStackVisibilityReport(arrayList);
        }
        if (Recents.getConfiguration().isLowRamDevice) {
            return this.mTaskStackLowRamLayoutAlgorithm.computeStackVisibilityReport(arrayList);
        }
        if (arrayList.size() <= 1) {
            return new VisibilityReport(1, 1);
        }
        TaskViewTransform taskViewTransform = new TaskViewTransform();
        TaskStackLayoutAlgorithm2 taskStackLayoutAlgorithm2 = ((float) getInitialFocusState()) > 0.0f ? this.mFocusedRange : this.mUnfocusedRange;
        taskStackLayoutAlgorithm2.offset(this.mInitialScrollP);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.recents_task_view_header_height);
        float f = 2.1474836E9f;
        int size = arrayList.size() - 1;
        int i3 = 0;
        int i4 = 0;
        while (true) {
            if (size >= 0) {
                float stackScrollForTask = getStackScrollForTask(arrayList.get(size));
                if (!taskStackLayoutAlgorithm2.isInRange(stackScrollForTask)) {
                    i2 = size;
                } else {
                    i = i3;
                    i2 = size;
                    getStackTransform(stackScrollForTask, stackScrollForTask, this.mInitialScrollP, this.mFocusState, taskViewTransform, null, false, false);
                    float f2 = taskViewTransform.rect.top;
                    if (f - f2 > ((float) dimensionPixelSize)) {
                        i3 = i + 1;
                        i4++;
                        f = f2;
                    } else {
                        for (int i5 = i2; i5 >= 0 && taskStackLayoutAlgorithm2.isInRange(getStackScrollForTask(arrayList.get(i5))); i5--) {
                            i4++;
                        }
                    }
                }
                size = i2 - 1;
            } else {
                i = i3;
                break;
            }
        }
        return new VisibilityReport(i4, i);
    }

    public TaskViewTransform getStackTransform(Task task, float f, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2) {
        return getStackTransform(task, f, this.mFocusState, taskViewTransform, taskViewTransform2, false, false);
    }

    public TaskViewTransform getStackTransform(Task task, float f, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2, boolean z) {
        return getStackTransform(task, f, this.mFocusState, taskViewTransform, taskViewTransform2, false, z);
    }

    public TaskViewTransform getStackTransform(Task task, float f, int i, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2, boolean z, boolean z2) {
        float stackScrollForTask;
        if (useGridLayout()) {
            this.mTaskGridLayoutAlgorithm.getTransform(this.mTaskIndexMap.get(task.key.id), this.mTaskIndexMap.size(), taskViewTransform, this);
            return taskViewTransform;
        }
        if (Recents.getConfiguration().isLowRamDevice) {
            if (task == null) {
                taskViewTransform.reset();
                return taskViewTransform;
            }
            this.mTaskStackLowRamLayoutAlgorithm.getTransform(this.mTaskIndexMap.get(task.key.id), f, taskViewTransform, this.mNumStackTasks, this);
            return taskViewTransform;
        }
        int i2 = this.mTaskIndexMap.get(task.key.id, -1);
        if (task == null || i2 == -1) {
            taskViewTransform.reset();
            return taskViewTransform;
        }
        if (z2) {
            stackScrollForTask = i2;
        } else {
            stackScrollForTask = getStackScrollForTask(task);
        }
        getStackTransform(stackScrollForTask, i2, f, i, taskViewTransform, taskViewTransform2, false, z);
        return taskViewTransform;
    }

    public TaskViewTransform getStackTransformScreenCoordinates(Task task, float f, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2, Rect rect) {
        return transformToScreenCoordinates(getStackTransform(task, f, this.mFocusState, taskViewTransform, taskViewTransform2, true, false), rect);
    }

    TaskViewTransform transformToScreenCoordinates(TaskViewTransform taskViewTransform, Rect rect) {
        if (rect == null) {
            rect = Recents.getSystemServices().getWindowRect();
        }
        taskViewTransform.rect.offset(rect.left, rect.top);
        if (useGridLayout()) {
            taskViewTransform.rect.offset(0.0f, this.mTitleBarHeight);
        }
        return taskViewTransform;
    }

    public void getStackTransform(float f, float f2, float f3, int i, TaskViewTransform taskViewTransform, TaskViewTransform taskViewTransform2, boolean z, boolean z2) {
        float f4;
        int i2;
        float fMapRange;
        Recents.getSystemServices();
        this.mUnfocusedRange.offset(f3);
        this.mFocusedRange.offset(f3);
        boolean zIsInRange = this.mUnfocusedRange.isInRange(f);
        boolean zIsInRange2 = this.mFocusedRange.isInRange(f);
        if (!z2 && !zIsInRange && !zIsInRange2) {
            taskViewTransform.reset();
            return;
        }
        this.mUnfocusedRange.offset(f3);
        this.mFocusedRange.offset(f3);
        float normalizedX = this.mUnfocusedRange.getNormalizedX(f);
        float normalizedX2 = this.mFocusedRange.getNormalizedX(f);
        float fClamp = Utilities.clamp(f3, this.mMinScrollP, this.mMaxScrollP);
        this.mUnfocusedRange.offset(fClamp);
        this.mFocusedRange.offset(fClamp);
        float normalizedX3 = this.mUnfocusedRange.getNormalizedX(f);
        float normalizedX4 = this.mUnfocusedRange.getNormalizedX(f2);
        float fClamp2 = Utilities.clamp(f3, -3.4028235E38f, this.mMaxScrollP);
        this.mUnfocusedRange.offset(fClamp2);
        this.mFocusedRange.offset(fClamp2);
        float normalizedX5 = this.mUnfocusedRange.getNormalizedX(f);
        float normalizedX6 = this.mFocusedRange.getNormalizedX(f);
        int iWidth = (this.mStackRect.width() - this.mTaskRect.width()) / 2;
        boolean z3 = true;
        float fMapRange2 = 0.0f;
        if (this.mNumStackTasks == 1 && !z) {
            int iHeight = (this.mStackRect.top - this.mTaskRect.top) + (((this.mStackRect.height() - this.mSystemInsets.bottom) - this.mTaskRect.height()) / 2) + getYForDeltaP((this.mMinScrollP - f3) / this.mNumStackTasks, 0.0f);
            f4 = this.mMaxTranslationZ;
            i2 = iHeight;
            fMapRange = 1.0f;
        } else {
            int interpolation = (int) ((1.0f - this.mUnfocusedCurveInterpolator.getInterpolation(normalizedX)) * this.mStackRect.height());
            int interpolation2 = (int) ((1.0f - this.mFocusedCurveInterpolator.getInterpolation(normalizedX2)) * this.mStackRect.height());
            float interpolation3 = this.mUnfocusedDimCurveInterpolator.getInterpolation(normalizedX5);
            float interpolation4 = this.mFocusedDimCurveInterpolator.getInterpolation(normalizedX6);
            if (this.mNumStackTasks <= 2 && f2 == 0.0f) {
                if (normalizedX3 < 0.5f) {
                    float interpolation5 = this.mUnfocusedDimCurveInterpolator.getInterpolation(0.5f);
                    interpolation3 = (interpolation3 - interpolation5) * (0.25f / (0.25f - interpolation5));
                } else {
                    interpolation3 = 0.0f;
                }
            }
            float f5 = i;
            int iMapRange = (this.mStackRect.top - this.mTaskRect.top) + ((int) Utilities.mapRange(f5, interpolation, interpolation2));
            float fMapRange3 = Utilities.mapRange(Utilities.clamp01(normalizedX4), this.mMinTranslationZ, this.mMaxTranslationZ);
            fMapRange2 = Utilities.mapRange(f5, interpolation3, interpolation4);
            f4 = fMapRange3;
            i2 = iMapRange;
            fMapRange = Utilities.mapRange(Utilities.clamp01(normalizedX3), 0.0f, 2.0f);
        }
        taskViewTransform.scale = 1.0f;
        taskViewTransform.alpha = 1.0f;
        taskViewTransform.translationZ = f4;
        taskViewTransform.dimAlpha = fMapRange2;
        taskViewTransform.viewOutlineAlpha = fMapRange;
        taskViewTransform.rect.set(this.mTaskRect);
        taskViewTransform.rect.offset(iWidth, i2);
        Utilities.scaleRectAboutCenter(taskViewTransform.rect, taskViewTransform.scale);
        if (taskViewTransform.rect.top >= this.mStackRect.bottom || (taskViewTransform2 != null && taskViewTransform.rect.top == taskViewTransform2.rect.top)) {
            z3 = false;
        }
        taskViewTransform.visible = z3;
    }

    public Rect getUntransformedTaskViewBounds() {
        return new Rect(this.mTaskRect);
    }

    float getStackScrollForTask(Task task) {
        Float f = this.mTaskIndexOverrideMap.get(task.key.id, null);
        if (Recents.getConfiguration().isLowRamDevice || f == null) {
            return this.mTaskIndexMap.get(task.key.id, 0);
        }
        return f.floatValue();
    }

    float getStackScrollForTaskIgnoreOverrides(Task task) {
        return this.mTaskIndexMap.get(task.key.id, 0);
    }

    float getStackScrollForTaskAtInitialOffset(Task task) {
        if (Recents.getConfiguration().isLowRamDevice) {
            RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
            return this.mTaskStackLowRamLayoutAlgorithm.getInitialScrollP(this.mNumStackTasks, launchState.launchedFromHome || launchState.launchedFromPipApp || launchState.launchedWithNextPipApp);
        }
        float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY(this.mInitialTopOffset, 0);
        this.mUnfocusedRange.offset(0.0f);
        return Utilities.clamp(this.mTaskIndexMap.get(task.key.id, 0) - Math.max(0.0f, this.mUnfocusedRange.getAbsoluteX(normalizedXFromUnfocusedY)), this.mMinScrollP, this.mMaxScrollP);
    }

    public float getDeltaPForY(int i, int i2) {
        if (Recents.getConfiguration().isLowRamDevice) {
            return this.mTaskStackLowRamLayoutAlgorithm.scrollToPercentage(i - i2);
        }
        return -(((i2 - i) / this.mStackRect.height()) * this.mUnfocusedCurveInterpolator.getArcLength());
    }

    public int getYForDeltaP(float f, float f2) {
        if (Recents.getConfiguration().isLowRamDevice) {
            return this.mTaskStackLowRamLayoutAlgorithm.percentageToScroll(f - f2);
        }
        return -((int) ((f2 - f) * this.mStackRect.height() * (1.0f / this.mUnfocusedCurveInterpolator.getArcLength())));
    }

    public void getTaskStackBounds(Rect rect, Rect rect2, int i, int i2, int i3, Rect rect3) {
        rect3.set(rect2.left + i2, rect2.top + i, rect2.right - i3, rect2.bottom);
        int iWidth = rect3.width() - (getScaleForExtent(rect2, rect, this.mBaseSideMargin, this.mMinMargin, 0) * 2);
        if (Utilities.getAppConfiguration(this.mContext).orientation == 2) {
            Rect rect4 = new Rect(0, 0, Math.min(rect.width(), rect.height()), Math.max(rect.width(), rect.height()));
            iWidth = Math.min(iWidth, rect4.width() - (getScaleForExtent(rect4, rect4, this.mBaseSideMargin, this.mMinMargin, 0) * 2));
        }
        rect3.inset((rect3.width() - iWidth) / 2, 0);
    }

    public static int getDimensionForDevice(Context context, int i, int i2, int i3, int i4) {
        return getDimensionForDevice(context, i, i, i2, i2, i3, i3, i4);
    }

    public static int getDimensionForDevice(Context context, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        RecentsConfiguration configuration = Recents.getConfiguration();
        Resources resources = context.getResources();
        boolean z = Utilities.getAppConfiguration(context).orientation == 2;
        if (configuration.isGridEnabled) {
            return resources.getDimensionPixelSize(i7);
        }
        if (configuration.isXLargeScreen) {
            if (z) {
                i5 = i6;
            }
            return resources.getDimensionPixelSize(i5);
        }
        if (configuration.isLargeScreen) {
            if (z) {
                i3 = i4;
            }
            return resources.getDimensionPixelSize(i3);
        }
        if (z) {
            i = i2;
        }
        return resources.getDimensionPixelSize(i);
    }

    private float getNormalizedXFromUnfocusedY(float f, int i) {
        if (i == 0) {
            f = this.mStackRect.height() - f;
        }
        return this.mUnfocusedCurveInterpolator.getX(f / this.mStackRect.height());
    }

    private float getNormalizedXFromFocusedY(float f, int i) {
        if (i == 0) {
            f = this.mStackRect.height() - f;
        }
        return this.mFocusedCurveInterpolator.getX(f / this.mStackRect.height());
    }

    private Path constructFocusedCurve() {
        Path path = new Path();
        path.moveTo(0.0f, 1.0f);
        path.lineTo(0.5f, 1.0f - (this.mFocusedTopPeekHeight / this.mStackRect.height()));
        path.lineTo(1.0f - (0.5f / this.mFocusedRange.relativeMax), Math.max(1.0f - (((this.mFocusedTopPeekHeight + this.mTaskRect.height()) - this.mMinMargin) / this.mStackRect.height()), (this.mStackBottomOffset + this.mFocusedBottomPeekHeight) / this.mStackRect.height()));
        path.lineTo(1.0f, 0.0f);
        return path;
    }

    private Path constructUnfocusedCurve() {
        float fHeight = 1.0f - (this.mFocusedTopPeekHeight / this.mStackRect.height());
        float f = (fHeight - 0.975f) / 0.099999994f;
        Path path = new Path();
        path.moveTo(0.0f, 1.0f);
        path.cubicTo(0.0f, 1.0f, 0.4f, 0.975f, 0.5f, fHeight);
        path.cubicTo(0.5f, fHeight, 0.65f, (f * 0.65f) + (1.0f - (0.4f * f)), 1.0f, 0.0f);
        return path;
    }

    private Path constructFocusedDimCurve() {
        Path path = new Path();
        path.moveTo(0.0f, 0.25f);
        path.lineTo(0.5f, 0.0f);
        path.lineTo(0.5f + (0.5f / this.mFocusedRange.relativeMax), 0.25f);
        path.lineTo(1.0f, 0.25f);
        return path;
    }

    private Path constructUnfocusedDimCurve() {
        float normalizedXFromUnfocusedY = getNormalizedXFromUnfocusedY(this.mInitialTopOffset, 0);
        float f = normalizedXFromUnfocusedY + ((1.0f - normalizedXFromUnfocusedY) / 2.0f);
        Path path = new Path();
        path.moveTo(0.0f, 0.25f);
        path.cubicTo(normalizedXFromUnfocusedY * 0.5f, 0.25f, normalizedXFromUnfocusedY * 0.75f, 0.1875f, normalizedXFromUnfocusedY, 0.0f);
        path.cubicTo(f, 0.0f, f, 0.15f, 1.0f, 0.15f);
        return path;
    }

    private int getScaleForExtent(Rect rect, Rect rect2, int i, int i2, int i3) {
        if (i3 == 0) {
            return Math.max(i2, (int) (Utilities.clamp01(rect.width() / rect2.width()) * i));
        }
        if (i3 == 1) {
            return Math.max(i2, (int) (Utilities.clamp01(rect.height() / rect2.height()) * i));
        }
        return i;
    }

    private void updateFrontBackTransforms() {
        if (this.mStackRect.isEmpty()) {
            return;
        }
        if (Recents.getConfiguration().isLowRamDevice) {
            this.mTaskStackLowRamLayoutAlgorithm.getBackOfStackTransform(this.mBackOfStackTransform, this);
            this.mTaskStackLowRamLayoutAlgorithm.getFrontOfStackTransform(this.mFrontOfStackTransform, this);
            return;
        }
        float fMapRange = Utilities.mapRange(this.mFocusState, this.mUnfocusedRange.relativeMin, this.mFocusedRange.relativeMin);
        float fMapRange2 = Utilities.mapRange(this.mFocusState, this.mUnfocusedRange.relativeMax, this.mFocusedRange.relativeMax);
        getStackTransform(fMapRange, fMapRange, 0.0f, this.mFocusState, this.mBackOfStackTransform, null, true, true);
        getStackTransform(fMapRange2, fMapRange2, 0.0f, this.mFocusState, this.mFrontOfStackTransform, null, true, true);
        this.mBackOfStackTransform.visible = true;
        this.mFrontOfStackTransform.visible = true;
    }

    public Rect getTaskRect() {
        if (Recents.getConfiguration().isLowRamDevice) {
            return this.mTaskStackLowRamLayoutAlgorithm.getTaskRect();
        }
        return useGridLayout() ? this.mTaskGridLayoutAlgorithm.getTaskGridRect() : this.mTaskRect;
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.print("TaskStackLayoutAlgorithm");
        printWriter.write(" numStackTasks=");
        printWriter.print(this.mNumStackTasks);
        printWriter.println();
        printWriter.print(str2);
        printWriter.print("insets=");
        printWriter.print(Utilities.dumpRect(this.mSystemInsets));
        printWriter.print(" stack=");
        printWriter.print(Utilities.dumpRect(this.mStackRect));
        printWriter.print(" task=");
        printWriter.print(Utilities.dumpRect(this.mTaskRect));
        printWriter.print(" actionButton=");
        printWriter.print(Utilities.dumpRect(this.mStackActionButtonRect));
        printWriter.println();
        printWriter.print(str2);
        printWriter.print("minScroll=");
        printWriter.print(this.mMinScrollP);
        printWriter.print(" maxScroll=");
        printWriter.print(this.mMaxScrollP);
        printWriter.print(" initialScroll=");
        printWriter.print(this.mInitialScrollP);
        printWriter.println();
        printWriter.print(str2);
        printWriter.print("focusState=");
        printWriter.print(this.mFocusState);
        printWriter.println();
        if (this.mTaskIndexOverrideMap.size() > 0) {
            for (int size = this.mTaskIndexOverrideMap.size() - 1; size >= 0; size--) {
                int iKeyAt = this.mTaskIndexOverrideMap.keyAt(size);
                float f = this.mTaskIndexMap.get(iKeyAt);
                float fFloatValue = this.mTaskIndexOverrideMap.get(iKeyAt, Float.valueOf(0.0f)).floatValue();
                printWriter.print(str2);
                printWriter.print("taskId= ");
                printWriter.print(iKeyAt);
                printWriter.print(" x= ");
                printWriter.print(f);
                printWriter.print(" overrideX= ");
                printWriter.print(fFloatValue);
                printWriter.println();
            }
        }
    }
}
