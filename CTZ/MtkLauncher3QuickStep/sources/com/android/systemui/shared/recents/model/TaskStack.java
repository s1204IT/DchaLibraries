package com.android.systemui.shared.recents.model;

import android.content.ComponentName;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.utilities.AnimationProps;
import com.android.systemui.shared.system.PackageManagerWrapper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TaskStack {
    private static final String TAG = "TaskStack";
    private TaskStackCallbacks mCb;
    private final ArrayList<Task> mRawTaskList = new ArrayList<>();
    private final FilteredTaskList mStackTaskList = new FilteredTaskList();

    public interface TaskStackCallbacks {
        void onStackTaskAdded(TaskStack taskStack, Task task);

        void onStackTaskRemoved(TaskStack taskStack, Task task, Task task2, AnimationProps animationProps, boolean z, boolean z2);

        void onStackTasksRemoved(TaskStack taskStack);

        void onStackTasksUpdated(TaskStack taskStack);
    }

    public TaskStack() {
        this.mStackTaskList.setFilter(new TaskFilter() {
            @Override
            public final boolean acceptTask(SparseArray sparseArray, Task task, int i) {
                return task.isStackTask;
            }
        });
    }

    public void setCallbacks(TaskStackCallbacks cb) {
        this.mCb = cb;
    }

    public void removeTask(Task t, AnimationProps animation, boolean fromDockGesture) {
        removeTask(t, animation, fromDockGesture, true);
    }

    public void removeTask(Task t, AnimationProps animation, boolean fromDockGesture, boolean dismissRecentsIfAllRemoved) {
        if (this.mStackTaskList.contains(t)) {
            this.mStackTaskList.remove(t);
            Task newFrontMostTask = getFrontMostTask();
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, t, newFrontMostTask, animation, fromDockGesture, dismissRecentsIfAllRemoved);
            }
        }
        this.mRawTaskList.remove(t);
    }

    public void removeAllTasks(boolean notifyStackChanges) {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task t = tasks.get(i);
            this.mStackTaskList.remove(t);
            this.mRawTaskList.remove(t);
        }
        if (this.mCb != null && notifyStackChanges) {
            this.mCb.onStackTasksRemoved(this);
        }
    }

    public void setTasks(TaskStack stack, boolean notifyStackChanges) {
        setTasks(stack.mRawTaskList, notifyStackChanges);
    }

    public void setTasks(List<Task> tasks, boolean notifyStackChanges) {
        ArrayMap<Task.TaskKey, Task> currentTasksMap = createTaskKeyMapFromList(this.mRawTaskList);
        ArrayMap<Task.TaskKey, Task> newTasksMap = createTaskKeyMapFromList(tasks);
        ArrayList<Task> addedTasks = new ArrayList<>();
        ArrayList<Task> removedTasks = new ArrayList<>();
        ArrayList<Task> allTasks = new ArrayList<>();
        boolean notifyStackChanges2 = this.mCb == null ? false : notifyStackChanges;
        int taskCount = this.mRawTaskList.size();
        for (int i = taskCount - 1; i >= 0; i--) {
            Task task = this.mRawTaskList.get(i);
            if (!newTasksMap.containsKey(task.key) && notifyStackChanges2) {
                removedTasks.add(task);
            }
        }
        int taskCount2 = tasks.size();
        int i2 = 0;
        for (int i3 = 0; i3 < taskCount2; i3++) {
            Task newTask = tasks.get(i3);
            Task currentTask = currentTasksMap.get(newTask.key);
            if (currentTask == null && notifyStackChanges2) {
                addedTasks.add(newTask);
            } else if (currentTask != null) {
                currentTask.copyFrom(newTask);
                newTask = currentTask;
            }
            allTasks.add(newTask);
        }
        for (int i4 = allTasks.size() - 1; i4 >= 0; i4--) {
            allTasks.get(i4).temporarySortIndexInStack = i4;
        }
        this.mStackTaskList.set(allTasks);
        this.mRawTaskList.clear();
        this.mRawTaskList.addAll(allTasks);
        int removedTaskCount = removedTasks.size();
        Task newFrontMostTask = getFrontMostTask();
        int i5 = 0;
        while (true) {
            int i6 = i5;
            if (i6 >= removedTaskCount) {
                break;
            }
            this.mCb.onStackTaskRemoved(this, removedTasks.get(i6), newFrontMostTask, AnimationProps.IMMEDIATE, false, true);
            i5 = i6 + 1;
            removedTaskCount = removedTaskCount;
        }
        int addedTaskCount = addedTasks.size();
        while (true) {
            int i7 = i2;
            if (i7 >= addedTaskCount) {
                break;
            }
            this.mCb.onStackTaskAdded(this, addedTasks.get(i7));
            i2 = i7 + 1;
        }
        if (notifyStackChanges2) {
            this.mCb.onStackTasksUpdated(this);
        }
    }

    public Task getFrontMostTask() {
        ArrayList<Task> stackTasks = this.mStackTaskList.getTasks();
        if (stackTasks.isEmpty()) {
            return null;
        }
        return stackTasks.get(stackTasks.size() - 1);
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

    public ArrayList<Task> getTasks() {
        return this.mStackTaskList.getTasks();
    }

    public ArrayList<Task> computeAllTasksList() {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.addAll(this.mStackTaskList.getTasks());
        return tasks;
    }

    public int getTaskCount() {
        return this.mStackTaskList.size();
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

    public boolean isNextLaunchTargetPip(long lastPipTime) {
        Task launchTarget = getLaunchTarget();
        Task nextLaunchTarget = getNextLaunchTargetRaw();
        return (nextLaunchTarget == null || lastPipTime <= 0) ? launchTarget != null && lastPipTime > 0 && getTaskCount() == 1 : lastPipTime > nextLaunchTarget.key.lastActiveTime;
    }

    public Task getNextLaunchTarget() {
        Task nextLaunchTarget = getNextLaunchTargetRaw();
        if (nextLaunchTarget != null) {
            return nextLaunchTarget;
        }
        return getTasks().get(getTaskCount() - 1);
    }

    private Task getNextLaunchTargetRaw() {
        int launchTaskIndex;
        int taskCount = getTaskCount();
        if (taskCount == 0 || (launchTaskIndex = indexOfTask(getLaunchTarget())) == -1 || launchTaskIndex <= 0) {
            return null;
        }
        return getTasks().get(launchTaskIndex - 1);
    }

    public int indexOfTask(Task t) {
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

    public ArraySet<ComponentName> computeComponentsRemoved(String packageName, int userId) {
        ArraySet<ComponentName> existingComponents = new ArraySet<>();
        ArraySet<ComponentName> removedComponents = new ArraySet<>();
        ArrayList<Task.TaskKey> taskKeys = getTaskKeys();
        int taskKeyCount = taskKeys.size();
        for (int i = 0; i < taskKeyCount; i++) {
            Task.TaskKey t = taskKeys.get(i);
            if (t.userId == userId) {
                ComponentName cn = t.getComponent();
                if (cn.getPackageName().equals(packageName) && !existingComponents.contains(cn)) {
                    if (PackageManagerWrapper.getInstance().getActivityInfo(cn, userId) != null) {
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
        writer.print(TAG);
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
