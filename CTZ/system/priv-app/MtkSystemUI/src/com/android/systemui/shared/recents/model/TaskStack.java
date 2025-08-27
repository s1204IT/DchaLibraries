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

/* loaded from: classes.dex */
public class TaskStack {
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
        this.mStackTaskList.setFilter(new TaskFilter() { // from class: com.android.systemui.shared.recents.model.-$$Lambda$TaskStack$gkuBLLtJ6FV7PDAxT-_KECDzTOI
            @Override // com.android.systemui.shared.recents.model.TaskFilter
            public final boolean acceptTask(SparseArray sparseArray, Task task, int i) {
                return task.isStackTask;
            }
        });
    }

    public void setCallbacks(TaskStackCallbacks taskStackCallbacks) {
        this.mCb = taskStackCallbacks;
    }

    public void removeTask(Task task, AnimationProps animationProps, boolean z) {
        removeTask(task, animationProps, z, true);
    }

    public void removeTask(Task task, AnimationProps animationProps, boolean z, boolean z2) {
        if (this.mStackTaskList.contains(task)) {
            this.mStackTaskList.remove(task);
            Task frontMostTask = getFrontMostTask();
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, task, frontMostTask, animationProps, z, z2);
            }
        }
        this.mRawTaskList.remove(task);
    }

    public void removeAllTasks(boolean z) {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        for (int size = tasks.size() - 1; size >= 0; size--) {
            Task task = tasks.get(size);
            this.mStackTaskList.remove(task);
            this.mRawTaskList.remove(task);
        }
        if (this.mCb != null && z) {
            this.mCb.onStackTasksRemoved(this);
        }
    }

    public void setTasks(TaskStack taskStack, boolean z) {
        setTasks(taskStack.mRawTaskList, z);
    }

    public void setTasks(List<Task> list, boolean z) {
        ArrayMap<Task.TaskKey, Task> arrayMapCreateTaskKeyMapFromList = createTaskKeyMapFromList(this.mRawTaskList);
        ArrayMap<Task.TaskKey, Task> arrayMapCreateTaskKeyMapFromList2 = createTaskKeyMapFromList(list);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        boolean z2 = this.mCb == null ? false : z;
        for (int size = this.mRawTaskList.size() - 1; size >= 0; size--) {
            Task task = this.mRawTaskList.get(size);
            if (!arrayMapCreateTaskKeyMapFromList2.containsKey(task.key) && z2) {
                arrayList2.add(task);
            }
        }
        int size2 = list.size();
        for (int i = 0; i < size2; i++) {
            Task task2 = list.get(i);
            Task task3 = arrayMapCreateTaskKeyMapFromList.get(task2.key);
            if (task3 == null && z2) {
                arrayList.add(task2);
            } else if (task3 != null) {
                task3.copyFrom(task2);
                task2 = task3;
            }
            arrayList3.add(task2);
        }
        for (int size3 = arrayList3.size() - 1; size3 >= 0; size3--) {
            ((Task) arrayList3.get(size3)).temporarySortIndexInStack = size3;
        }
        this.mStackTaskList.set(arrayList3);
        this.mRawTaskList.clear();
        this.mRawTaskList.addAll(arrayList3);
        int size4 = arrayList2.size();
        Task frontMostTask = getFrontMostTask();
        for (int i2 = 0; i2 < size4; i2++) {
            this.mCb.onStackTaskRemoved(this, (Task) arrayList2.get(i2), frontMostTask, AnimationProps.IMMEDIATE, false, true);
        }
        int size5 = arrayList.size();
        for (int i3 = 0; i3 < size5; i3++) {
            this.mCb.onStackTaskAdded(this, (Task) arrayList.get(i3));
        }
        if (z2) {
            this.mCb.onStackTasksUpdated(this);
        }
    }

    public Task getFrontMostTask() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        if (tasks.isEmpty()) {
            return null;
        }
        return tasks.get(tasks.size() - 1);
    }

    public ArrayList<Task.TaskKey> getTaskKeys() {
        ArrayList<Task.TaskKey> arrayList = new ArrayList<>();
        ArrayList<Task> arrayListComputeAllTasksList = computeAllTasksList();
        int size = arrayListComputeAllTasksList.size();
        for (int i = 0; i < size; i++) {
            arrayList.add(arrayListComputeAllTasksList.get(i).key);
        }
        return arrayList;
    }

    public ArrayList<Task> getTasks() {
        return this.mStackTaskList.getTasks();
    }

    public ArrayList<Task> computeAllTasksList() {
        ArrayList<Task> arrayList = new ArrayList<>();
        arrayList.addAll(this.mStackTaskList.getTasks());
        return arrayList;
    }

    public int getTaskCount() {
        return this.mStackTaskList.size();
    }

    public Task getLaunchTarget() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int size = tasks.size();
        for (int i = 0; i < size; i++) {
            Task task = tasks.get(i);
            if (task.isLaunchTarget) {
                return task;
            }
        }
        return null;
    }

    public boolean isNextLaunchTargetPip(long j) {
        Task launchTarget = getLaunchTarget();
        Task nextLaunchTargetRaw = getNextLaunchTargetRaw();
        return (nextLaunchTargetRaw == null || j <= 0) ? launchTarget != null && j > 0 && getTaskCount() == 1 : j > nextLaunchTargetRaw.key.lastActiveTime;
    }

    public Task getNextLaunchTarget() {
        Task nextLaunchTargetRaw = getNextLaunchTargetRaw();
        if (nextLaunchTargetRaw != null) {
            return nextLaunchTargetRaw;
        }
        return getTasks().get(getTaskCount() - 1);
    }

    private Task getNextLaunchTargetRaw() {
        int iIndexOfTask;
        if (getTaskCount() == 0 || (iIndexOfTask = indexOfTask(getLaunchTarget())) == -1 || iIndexOfTask <= 0) {
            return null;
        }
        return getTasks().get(iIndexOfTask - 1);
    }

    public int indexOfTask(Task task) {
        return this.mStackTaskList.indexOf(task);
    }

    public Task findTaskWithId(int i) {
        ArrayList<Task> arrayListComputeAllTasksList = computeAllTasksList();
        int size = arrayListComputeAllTasksList.size();
        for (int i2 = 0; i2 < size; i2++) {
            Task task = arrayListComputeAllTasksList.get(i2);
            if (task.key.id == i) {
                return task;
            }
        }
        return null;
    }

    public ArraySet<ComponentName> computeComponentsRemoved(String str, int i) {
        ArraySet arraySet = new ArraySet();
        ArraySet<ComponentName> arraySet2 = new ArraySet<>();
        ArrayList<Task.TaskKey> taskKeys = getTaskKeys();
        int size = taskKeys.size();
        for (int i2 = 0; i2 < size; i2++) {
            Task.TaskKey taskKey = taskKeys.get(i2);
            if (taskKey.userId == i) {
                ComponentName component = taskKey.getComponent();
                if (component.getPackageName().equals(str) && !arraySet.contains(component)) {
                    if (PackageManagerWrapper.getInstance().getActivityInfo(component, i) != null) {
                        arraySet.add(component);
                    } else {
                        arraySet2.add(component);
                    }
                }
            }
        }
        return arraySet2;
    }

    public String toString() {
        String str = "Stack Tasks (" + this.mStackTaskList.size() + "):\n";
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int size = tasks.size();
        for (int i = 0; i < size; i++) {
            str = str + "    " + tasks.get(i).toString() + "\n";
        }
        return str;
    }

    private ArrayMap<Task.TaskKey, Task> createTaskKeyMapFromList(List<Task> list) {
        ArrayMap<Task.TaskKey, Task> arrayMap = new ArrayMap<>(list.size());
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Task task = list.get(i);
            arrayMap.put(task.key, task);
        }
        return arrayMap;
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.print("TaskStack");
        printWriter.print(" numStackTasks=");
        printWriter.print(this.mStackTaskList.size());
        printWriter.println();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int size = tasks.size();
        for (int i = 0; i < size; i++) {
            tasks.get(i).dump(str2, printWriter);
        }
    }
}
