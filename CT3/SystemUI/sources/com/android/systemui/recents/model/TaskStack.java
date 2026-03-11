package com.android.systemui.recents.model;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntProperty;
import android.util.Property;
import android.util.SparseArray;
import android.view.animation.Interpolator;
import com.android.internal.policy.DockedDividerUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.AnimationProps;
import com.android.systemui.recents.views.DropTarget;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TaskStack {
    TaskStackCallbacks mCb;
    private Comparator<Task> FREEFORM_COMPARATOR = new Comparator<Task>() {
        @Override
        public int compare(Task o1, Task o2) {
            if (o1.isFreeformTask() && !o2.isFreeformTask()) {
                return 1;
            }
            if (o2.isFreeformTask() && !o1.isFreeformTask()) {
                return -1;
            }
            return Long.compare(o1.temporarySortIndexInStack, o2.temporarySortIndexInStack);
        }
    };
    ArrayList<Task> mRawTaskList = new ArrayList<>();
    FilteredTaskList mStackTaskList = new FilteredTaskList();
    ArrayList<TaskGrouping> mGroups = new ArrayList<>();
    ArrayMap<Integer, TaskGrouping> mAffinitiesGroups = new ArrayMap<>();

    public interface TaskStackCallbacks {
        void onStackTaskAdded(TaskStack taskStack, Task task);

        void onStackTaskRemoved(TaskStack taskStack, Task task, Task task2, AnimationProps animationProps, boolean z);

        void onStackTasksRemoved(TaskStack taskStack);

        void onStackTasksUpdated(TaskStack taskStack);
    }

    public static class DockState implements DropTarget {
        public final int createMode;
        private final RectF dockArea;
        public final int dockSide;
        private final RectF expandedTouchDockArea;
        private final RectF touchArea;
        public final ViewState viewState;
        public static final DockState NONE = new DockState(-1, -1, 80, 255, 0, null, null, null);
        public static final DockState LEFT = new DockState(1, 0, 80, 0, 1, new RectF(0.0f, 0.0f, 0.125f, 1.0f), new RectF(0.0f, 0.0f, 0.125f, 1.0f), new RectF(0.0f, 0.0f, 0.5f, 1.0f));
        public static final DockState TOP = new DockState(2, 0, 80, 0, 0, new RectF(0.0f, 0.0f, 1.0f, 0.125f), new RectF(0.0f, 0.0f, 1.0f, 0.125f), new RectF(0.0f, 0.0f, 1.0f, 0.5f));
        public static final DockState RIGHT = new DockState(3, 1, 80, 0, 1, new RectF(0.875f, 0.0f, 1.0f, 1.0f), new RectF(0.875f, 0.0f, 1.0f, 1.0f), new RectF(0.5f, 0.0f, 1.0f, 1.0f));
        public static final DockState BOTTOM = new DockState(4, 1, 80, 0, 0, new RectF(0.0f, 0.875f, 1.0f, 1.0f), new RectF(0.0f, 0.875f, 1.0f, 1.0f), new RectF(0.0f, 0.5f, 1.0f, 1.0f));

        @Override
        public boolean acceptsDrop(int x, int y, int width, int height, boolean isCurrentTarget) {
            if (isCurrentTarget) {
                return areaContainsPoint(this.expandedTouchDockArea, width, height, x, y);
            }
            return areaContainsPoint(this.touchArea, width, height, x, y);
        }

        public static class ViewState {
            private static final IntProperty<ViewState> HINT_ALPHA = new IntProperty<ViewState>("drawableAlpha") {
                @Override
                public void setValue(ViewState object, int alpha) {
                    object.mHintTextAlpha = alpha;
                    object.dockAreaOverlay.invalidateSelf();
                }

                @Override
                public Integer get(ViewState object) {
                    return Integer.valueOf(object.mHintTextAlpha);
                }
            };
            public final int dockAreaAlpha;
            public final ColorDrawable dockAreaOverlay;
            public final int hintTextAlpha;
            public final int hintTextOrientation;
            private AnimatorSet mDockAreaOverlayAnimator;
            private String mHintText;
            private int mHintTextAlpha;
            private Point mHintTextBounds;
            private Paint mHintTextPaint;
            private final int mHintTextResId;
            private Rect mTmpRect;

            ViewState(int areaAlpha, int hintAlpha, int hintOrientation, int hintTextResId, ViewState viewState) {
                this(areaAlpha, hintAlpha, hintOrientation, hintTextResId);
            }

            private ViewState(int areaAlpha, int hintAlpha, int hintOrientation, int hintTextResId) {
                this.mHintTextBounds = new Point();
                this.mHintTextAlpha = 255;
                this.mTmpRect = new Rect();
                this.dockAreaAlpha = areaAlpha;
                this.dockAreaOverlay = new ColorDrawable(-1);
                this.dockAreaOverlay.setAlpha(0);
                this.hintTextAlpha = hintAlpha;
                this.hintTextOrientation = hintOrientation;
                this.mHintTextResId = hintTextResId;
                this.mHintTextPaint = new Paint(1);
                this.mHintTextPaint.setColor(-1);
            }

            public void update(Context context) {
                Resources res = context.getResources();
                this.mHintText = context.getString(this.mHintTextResId);
                this.mHintTextPaint.setTextSize(res.getDimensionPixelSize(R.dimen.recents_drag_hint_text_size));
                this.mHintTextPaint.getTextBounds(this.mHintText, 0, this.mHintText.length(), this.mTmpRect);
                this.mHintTextBounds.set((int) this.mHintTextPaint.measureText(this.mHintText), this.mTmpRect.height());
            }

            public void draw(Canvas canvas) {
                if (this.dockAreaOverlay.getAlpha() > 0) {
                    this.dockAreaOverlay.draw(canvas);
                }
                if (this.mHintTextAlpha <= 0) {
                    return;
                }
                Rect bounds = this.dockAreaOverlay.getBounds();
                int x = bounds.left + ((bounds.width() - this.mHintTextBounds.x) / 2);
                int y = bounds.top + ((bounds.height() + this.mHintTextBounds.y) / 2);
                this.mHintTextPaint.setAlpha(this.mHintTextAlpha);
                if (this.hintTextOrientation == 1) {
                    canvas.save();
                    canvas.rotate(-90.0f, bounds.centerX(), bounds.centerY());
                }
                canvas.drawText(this.mHintText, x, y, this.mHintTextPaint);
                if (this.hintTextOrientation != 1) {
                    return;
                }
                canvas.restore();
            }

            public void startAnimation(Rect bounds, int areaAlpha, int hintAlpha, int duration, Interpolator interpolator, boolean animateAlpha, boolean animateBounds) {
                Interpolator interpolator2;
                if (this.mDockAreaOverlayAnimator != null) {
                    this.mDockAreaOverlayAnimator.cancel();
                }
                ArrayList<Animator> animators = new ArrayList<>();
                if (this.dockAreaOverlay.getAlpha() != areaAlpha) {
                    if (animateAlpha) {
                        ObjectAnimator anim = ObjectAnimator.ofInt(this.dockAreaOverlay, (Property<ColorDrawable, Integer>) Utilities.DRAWABLE_ALPHA, this.dockAreaOverlay.getAlpha(), areaAlpha);
                        anim.setDuration(duration);
                        anim.setInterpolator(interpolator);
                        animators.add(anim);
                    } else {
                        this.dockAreaOverlay.setAlpha(areaAlpha);
                    }
                }
                if (this.mHintTextAlpha != hintAlpha) {
                    if (animateAlpha) {
                        ObjectAnimator anim2 = ObjectAnimator.ofInt(this, HINT_ALPHA, this.mHintTextAlpha, hintAlpha);
                        anim2.setDuration(150L);
                        if (hintAlpha > this.mHintTextAlpha) {
                            interpolator2 = Interpolators.ALPHA_IN;
                        } else {
                            interpolator2 = Interpolators.ALPHA_OUT;
                        }
                        anim2.setInterpolator(interpolator2);
                        animators.add(anim2);
                    } else {
                        this.mHintTextAlpha = hintAlpha;
                        this.dockAreaOverlay.invalidateSelf();
                    }
                }
                if (bounds != null && !this.dockAreaOverlay.getBounds().equals(bounds)) {
                    if (animateBounds) {
                        PropertyValuesHolder prop = PropertyValuesHolder.ofObject(Utilities.DRAWABLE_RECT, Utilities.RECT_EVALUATOR, new Rect(this.dockAreaOverlay.getBounds()), bounds);
                        ObjectAnimator anim3 = ObjectAnimator.ofPropertyValuesHolder(this.dockAreaOverlay, prop);
                        anim3.setDuration(duration);
                        anim3.setInterpolator(interpolator);
                        animators.add(anim3);
                    } else {
                        this.dockAreaOverlay.setBounds(bounds);
                    }
                }
                if (animators.isEmpty()) {
                    return;
                }
                this.mDockAreaOverlayAnimator = new AnimatorSet();
                this.mDockAreaOverlayAnimator.playTogether(animators);
                this.mDockAreaOverlayAnimator.start();
            }
        }

        DockState(int dockSide, int createMode, int dockAreaAlpha, int hintTextAlpha, int hintTextOrientation, RectF touchArea, RectF dockArea, RectF expandedTouchDockArea) {
            this.dockSide = dockSide;
            this.createMode = createMode;
            this.viewState = new ViewState(dockAreaAlpha, hintTextAlpha, hintTextOrientation, R.string.recents_drag_hint_message, null);
            this.dockArea = dockArea;
            this.touchArea = touchArea;
            this.expandedTouchDockArea = expandedTouchDockArea;
        }

        public void update(Context context) {
            this.viewState.update(context);
        }

        public boolean areaContainsPoint(RectF area, int width, int height, float x, float y) {
            int left = (int) (area.left * width);
            int top = (int) (area.top * height);
            int right = (int) (area.right * width);
            int bottom = (int) (area.bottom * height);
            return x >= ((float) left) && y >= ((float) top) && x <= ((float) right) && y <= ((float) bottom);
        }

        public Rect getPreDockedBounds(int width, int height) {
            return new Rect((int) (this.dockArea.left * width), (int) (this.dockArea.top * height), (int) (this.dockArea.right * width), (int) (this.dockArea.bottom * height));
        }

        public Rect getDockedBounds(int width, int height, int dividerSize, Rect insets, Resources res) {
            boolean isHorizontalDivision = res.getConfiguration().orientation == 1;
            int position = DockedDividerUtils.calculateMiddlePosition(isHorizontalDivision, insets, width, height, dividerSize);
            Rect newWindowBounds = new Rect();
            DockedDividerUtils.calculateBoundsForPosition(position, this.dockSide, newWindowBounds, width, height, dividerSize);
            return newWindowBounds;
        }

        public Rect getDockedTaskStackBounds(Rect displayRect, int width, int height, int dividerSize, Rect insets, TaskStackLayoutAlgorithm layoutAlgorithm, Resources res, Rect windowRectOut) {
            int top;
            boolean isHorizontalDivision = res.getConfiguration().orientation == 1;
            int position = DockedDividerUtils.calculateMiddlePosition(isHorizontalDivision, insets, width, height, dividerSize);
            DockedDividerUtils.calculateBoundsForPosition(position, DockedDividerUtils.invertDockSide(this.dockSide), windowRectOut, width, height, dividerSize);
            Rect taskStackBounds = new Rect();
            if (this.dockArea.bottom < 1.0f) {
                top = 0;
            } else {
                top = insets.top;
            }
            layoutAlgorithm.getTaskStackBounds(displayRect, windowRectOut, top, insets.right, taskStackBounds);
            return taskStackBounds;
        }
    }

    public TaskStack() {
        this.mStackTaskList.setFilter(new TaskFilter() {
            @Override
            public boolean acceptTask(SparseArray<Task> taskIdMap, Task t, int index) {
                return t.isStackTask;
            }
        });
    }

    public void setCallbacks(TaskStackCallbacks cb) {
        this.mCb = cb;
    }

    public void moveTaskToStack(Task task, int newStackId) {
        ArrayList<Task> taskList = this.mStackTaskList.getTasks();
        int taskCount = taskList.size();
        if (!task.isFreeformTask() && newStackId == 2) {
            this.mStackTaskList.moveTaskToStack(task, taskCount, newStackId);
            return;
        }
        if (!task.isFreeformTask() || newStackId != 1) {
            return;
        }
        int insertIndex = 0;
        int i = taskCount - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            if (taskList.get(i).isFreeformTask()) {
                i--;
            } else {
                insertIndex = i + 1;
                break;
            }
        }
        this.mStackTaskList.moveTaskToStack(task, insertIndex, newStackId);
    }

    void removeTaskImpl(FilteredTaskList taskList, Task t) {
        taskList.remove(t);
        TaskGrouping group = t.group;
        if (group == null) {
            return;
        }
        group.removeTask(t);
        if (group.getTaskCount() != 0) {
            return;
        }
        removeGroup(group);
    }

    public void removeTask(Task t, AnimationProps animation, boolean fromDockGesture) {
        if (this.mStackTaskList.contains(t)) {
            removeTaskImpl(this.mStackTaskList, t);
            Task newFrontMostTask = getStackFrontMostTask(false);
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, t, newFrontMostTask, animation, fromDockGesture);
            }
        }
        this.mRawTaskList.remove(t);
    }

    public void removeAllTasks() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task t = tasks.get(i);
            removeTaskImpl(this.mStackTaskList, t);
            this.mRawTaskList.remove(t);
        }
        if (this.mCb == null) {
            return;
        }
        this.mCb.onStackTasksRemoved(this);
    }

    public void setTasks(Context context, List<Task> tasks, boolean notifyStackChanges) {
        ArrayMap<Task.TaskKey, Task> currentTasksMap = createTaskKeyMapFromList(this.mRawTaskList);
        ArrayMap<Task.TaskKey, Task> newTasksMap = createTaskKeyMapFromList(tasks);
        ArrayList<Task> addedTasks = new ArrayList<>();
        ArrayList<Task> removedTasks = new ArrayList<>();
        ArrayList<Task> allTasks = new ArrayList<>();
        if (this.mCb == null) {
            notifyStackChanges = false;
        }
        int taskCount = this.mRawTaskList.size();
        for (int i = taskCount - 1; i >= 0; i--) {
            Task task = this.mRawTaskList.get(i);
            if (!newTasksMap.containsKey(task.key) && notifyStackChanges) {
                removedTasks.add(task);
            }
            task.setGroup(null);
        }
        int taskCount2 = tasks.size();
        for (int i2 = 0; i2 < taskCount2; i2++) {
            Task newTask = tasks.get(i2);
            Task currentTask = currentTasksMap.get(newTask.key);
            if (currentTask == null && notifyStackChanges) {
                addedTasks.add(newTask);
            } else if (currentTask != null) {
                currentTask.copyFrom(newTask);
                newTask = currentTask;
            }
            allTasks.add(newTask);
        }
        for (int i3 = allTasks.size() - 1; i3 >= 0; i3--) {
            allTasks.get(i3).temporarySortIndexInStack = i3;
        }
        Collections.sort(allTasks, this.FREEFORM_COMPARATOR);
        this.mStackTaskList.set(allTasks);
        this.mRawTaskList = allTasks;
        createAffiliatedGroupings(context);
        int removedTaskCount = removedTasks.size();
        Task newFrontMostTask = getStackFrontMostTask(false);
        for (int i4 = 0; i4 < removedTaskCount; i4++) {
            this.mCb.onStackTaskRemoved(this, removedTasks.get(i4), newFrontMostTask, AnimationProps.IMMEDIATE, false);
        }
        int addedTaskCount = addedTasks.size();
        for (int i5 = 0; i5 < addedTaskCount; i5++) {
            this.mCb.onStackTaskAdded(this, addedTasks.get(i5));
        }
        if (!notifyStackChanges) {
            return;
        }
        this.mCb.onStackTasksUpdated(this);
    }

    public Task getStackFrontMostTask(boolean includeFreeformTasks) {
        ArrayList<Task> stackTasks = this.mStackTaskList.getTasks();
        if (stackTasks.isEmpty()) {
            return null;
        }
        for (int i = stackTasks.size() - 1; i >= 0; i--) {
            Task task = stackTasks.get(i);
            if (!task.isFreeformTask() || includeFreeformTasks) {
                return task;
            }
        }
        return null;
    }

    public ArrayList<Task.TaskKey> getTaskKeys() {
        ArrayList<Task.TaskKey> taskKeys = new ArrayList<>();
        ArrayList<Task> tasks = computeAllTasksList();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            taskKeys.add(task.key);
        }
        return taskKeys;
    }

    public ArrayList<Task> getStackTasks() {
        return this.mStackTaskList.getTasks();
    }

    public ArrayList<Task> getFreeformTasks() {
        ArrayList<Task> freeformTasks = new ArrayList<>();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            if (task.isFreeformTask()) {
                freeformTasks.add(task);
            }
        }
        return freeformTasks;
    }

    public ArrayList<Task> computeAllTasksList() {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.addAll(this.mStackTaskList.getTasks());
        return tasks;
    }

    public int getTaskCount() {
        return this.mStackTaskList.size();
    }

    public int getStackTaskCount() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int stackCount = 0;
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            if (!task.isFreeformTask()) {
                stackCount++;
            }
        }
        return stackCount;
    }

    public int getFreeformTaskCount() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int freeformCount = 0;
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            if (task.isFreeformTask()) {
                freeformCount++;
            }
        }
        return freeformCount;
    }

    public Task getLaunchTarget() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            if (task.isLaunchTarget) {
                return task;
            }
        }
        return null;
    }

    public int indexOfStackTask(Task t) {
        return this.mStackTaskList.indexOf(t);
    }

    public Task findTaskWithId(int taskId) {
        ArrayList<Task> tasks = computeAllTasksList();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            if (task.key.id == taskId) {
                return task;
            }
        }
        return null;
    }

    public void addGroup(TaskGrouping group) {
        this.mGroups.add(group);
        this.mAffinitiesGroups.put(Integer.valueOf(group.affiliation), group);
    }

    public void removeGroup(TaskGrouping group) {
        this.mGroups.remove(group);
        this.mAffinitiesGroups.remove(Integer.valueOf(group.affiliation));
    }

    void createAffiliatedGroupings(Context context) {
        this.mGroups.clear();
        this.mAffinitiesGroups.clear();
        ArrayMap<Task.TaskKey, Task> tasksMap = new ArrayMap<>();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task t = tasks.get(i);
            TaskGrouping group = new TaskGrouping(t.key.id);
            addGroup(group);
            group.addTask(t);
            tasksMap.put(t.key, t);
        }
        float minAlpha = context.getResources().getFloat(R.dimen.recents_task_affiliation_color_min_alpha_percentage);
        int taskGroupCount = this.mGroups.size();
        for (int i2 = 0; i2 < taskGroupCount; i2++) {
            TaskGrouping group2 = this.mGroups.get(i2);
            int taskCount2 = group2.getTaskCount();
            if (taskCount2 > 1) {
                int affiliationColor = tasksMap.get(group2.mTaskKeys.get(0)).affiliationColor;
                float alphaStep = (1.0f - minAlpha) / taskCount2;
                float alpha = 1.0f;
                for (int j = 0; j < taskCount2; j++) {
                    tasksMap.get(group2.mTaskKeys.get(j)).colorPrimary = Utilities.getColorWithOverlay(affiliationColor, -1, alpha);
                    alpha -= alphaStep;
                }
            }
        }
    }

    public ArraySet<ComponentName> computeComponentsRemoved(String packageName, int userId) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        ArraySet<ComponentName> existingComponents = new ArraySet<>();
        ArraySet<ComponentName> removedComponents = new ArraySet<>();
        ArrayList<Task.TaskKey> taskKeys = getTaskKeys();
        int taskKeyCount = taskKeys.size();
        for (int i = 0; i < taskKeyCount; i++) {
            Task.TaskKey t = taskKeys.get(i);
            if (t.userId == userId) {
                ComponentName cn = t.getComponent();
                if (cn.getPackageName().equals(packageName) && !existingComponents.contains(cn)) {
                    if (ssp.getActivityInfo(cn, userId) != null) {
                        existingComponents.add(cn);
                    } else {
                        removedComponents.add(cn);
                    }
                }
            }
        }
        return removedComponents;
    }

    public String toString() {
        String str = "Stack Tasks (" + this.mStackTaskList.size() + "):\n";
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            str = str + "    " + tasks.get(i).toString() + "\n";
        }
        return str;
    }

    private ArrayMap<Task.TaskKey, Task> createTaskKeyMapFromList(List<Task> tasks) {
        ArrayMap<Task.TaskKey, Task> map = new ArrayMap<>(tasks.size());
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            map.put(task.key, task);
        }
        return map;
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.print("TaskStack");
        writer.print(" numStackTasks=");
        writer.print(this.mStackTaskList.size());
        writer.println();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            tasks.get(i).dump(innerPrefix, writer);
        }
    }
}
