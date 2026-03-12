package com.android.systemui.recents.model;

import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TaskStack {
    TaskStackCallbacks mCb;
    FilteredTaskList mTaskList = new FilteredTaskList();
    ArrayList<TaskGrouping> mGroups = new ArrayList<>();
    HashMap<Integer, TaskGrouping> mAffinitiesGroups = new HashMap<>();

    public interface TaskStackCallbacks {
        void onStackTaskAdded(TaskStack taskStack, Task task);

        void onStackTaskRemoved(TaskStack taskStack, Task task, Task task2);

        void onStackUnfiltered(TaskStack taskStack, ArrayList<Task> arrayList);
    }

    public void setCallbacks(TaskStackCallbacks cb) {
        this.mCb = cb;
    }

    public void reset() {
        this.mCb = null;
        this.mTaskList.reset();
        this.mGroups.clear();
        this.mAffinitiesGroups.clear();
    }

    public void removeTask(Task t) {
        if (this.mTaskList.contains(t)) {
            this.mTaskList.remove(t);
            TaskGrouping group = t.group;
            group.removeTask(t);
            if (group.getTaskCount() == 0) {
                removeGroup(group);
            }
            t.lockToThisTask = false;
            Task newFrontMostTask = getFrontMostTask();
            if (newFrontMostTask != null && newFrontMostTask.lockToTaskEnabled) {
                newFrontMostTask.lockToThisTask = true;
            }
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, t, newFrontMostTask);
            }
        }
    }

    public void setTasks(List<Task> tasks) {
        ArrayList<Task> taskList = this.mTaskList.getTasks();
        int taskCount = taskList.size();
        for (int i = 0; i < taskCount; i++) {
            Task t = taskList.get(i);
            this.mTaskList.remove(t);
            TaskGrouping group = t.group;
            group.removeTask(t);
            if (group.getTaskCount() == 0) {
                removeGroup(group);
            }
            t.lockToThisTask = false;
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, t, null);
            }
        }
        this.mTaskList.set(tasks);
        for (Task t2 : tasks) {
            if (this.mCb != null) {
                this.mCb.onStackTaskAdded(this, t2);
            }
        }
    }

    public Task getFrontMostTask() {
        if (this.mTaskList.size() == 0) {
            return null;
        }
        return this.mTaskList.getTasks().get(this.mTaskList.size() - 1);
    }

    public ArrayList<Task.TaskKey> getTaskKeys() {
        ArrayList<Task.TaskKey> taskKeys = new ArrayList<>();
        ArrayList<Task> tasks = this.mTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            taskKeys.add(tasks.get(i).key);
        }
        return taskKeys;
    }

    public ArrayList<Task> getTasks() {
        return this.mTaskList.getTasks();
    }

    public int getTaskCount() {
        return this.mTaskList.size();
    }

    public int indexOfTask(Task t) {
        return this.mTaskList.indexOf(t);
    }

    public Task findTaskWithId(int taskId) {
        ArrayList<Task> tasks = this.mTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            if (task.key.id == taskId) {
                return task;
            }
        }
        return null;
    }

    public void unfilterTasks() {
        ArrayList<Task> oldStack = new ArrayList<>(this.mTaskList.getTasks());
        this.mTaskList.removeFilter();
        if (this.mCb != null) {
            this.mCb.onStackUnfiltered(this, oldStack);
        }
    }

    public boolean hasFilteredTasks() {
        return this.mTaskList.hasFilter();
    }

    public void addGroup(TaskGrouping group) {
        this.mGroups.add(group);
        this.mAffinitiesGroups.put(Integer.valueOf(group.affiliation), group);
    }

    public void removeGroup(TaskGrouping group) {
        this.mGroups.remove(group);
        this.mAffinitiesGroups.remove(Integer.valueOf(group.affiliation));
    }

    public TaskGrouping getGroupWithAffiliation(int affiliation) {
        return this.mAffinitiesGroups.get(Integer.valueOf(affiliation));
    }

    public void createAffiliatedGroupings(RecentsConfiguration config) {
        TaskGrouping group;
        HashMap<Task.TaskKey, Task> tasksMap = new HashMap<>();
        ArrayList<Task> tasks = this.mTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task t = tasks.get(i);
            int affiliation = t.taskAffiliation > 0 ? t.taskAffiliation : 65536 + t.key.id;
            if (this.mAffinitiesGroups.containsKey(Integer.valueOf(affiliation))) {
                group = getGroupWithAffiliation(affiliation);
            } else {
                group = new TaskGrouping(affiliation);
                addGroup(group);
            }
            group.addTask(t);
            tasksMap.put(t.key, t);
        }
        float minAlpha = config.taskBarViewAffiliationColorMinAlpha;
        int taskGroupCount = this.mGroups.size();
        for (int i2 = 0; i2 < taskGroupCount; i2++) {
            TaskGrouping group2 = this.mGroups.get(i2);
            int taskCount2 = group2.getTaskCount();
            if (taskCount2 > 1) {
                int affiliationColor = tasksMap.get(group2.mTaskKeys.get(0)).taskAffiliationColor;
                float alphaStep = (1.0f - minAlpha) / taskCount2;
                float alpha = 1.0f;
                for (int j = 0; j < taskCount2; j++) {
                    tasksMap.get(group2.mTaskKeys.get(j)).colorPrimary = Utilities.getColorWithOverlay(affiliationColor, -1, alpha);
                    alpha -= alphaStep;
                }
            }
        }
    }

    public String toString() {
        String str = "Tasks:\n";
        for (Task t : this.mTaskList.getTasks()) {
            str = str + "  " + t.toString() + "\n";
        }
        return str;
    }
}
