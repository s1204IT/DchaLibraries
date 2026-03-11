package com.android.systemui.recents.model;

import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.systemui.recents.model.Task;
import java.util.ArrayList;
import java.util.List;

class FilteredTaskList {
    TaskFilter mFilter;
    ArrayList<Task> mTasks = new ArrayList<>();
    ArrayList<Task> mFilteredTasks = new ArrayList<>();
    ArrayMap<Task.TaskKey, Integer> mTaskIndices = new ArrayMap<>();

    FilteredTaskList() {
    }

    boolean setFilter(TaskFilter filter) {
        ArrayList<Task> prevFilteredTasks = new ArrayList<>(this.mFilteredTasks);
        this.mFilter = filter;
        updateFilteredTasks();
        if (!prevFilteredTasks.equals(this.mFilteredTasks)) {
            return true;
        }
        return false;
    }

    public void moveTaskToStack(Task task, int insertIndex, int newStackId) {
        int taskIndex = indexOf(task);
        if (taskIndex != insertIndex) {
            this.mTasks.remove(taskIndex);
            if (taskIndex < insertIndex) {
                insertIndex--;
            }
            this.mTasks.add(insertIndex, task);
        }
        task.setStackId(newStackId);
        updateFilteredTasks();
    }

    void set(List<Task> tasks) {
        this.mTasks.clear();
        this.mTasks.addAll(tasks);
        updateFilteredTasks();
    }

    boolean remove(Task t) {
        if (this.mFilteredTasks.contains(t)) {
            boolean removed = this.mTasks.remove(t);
            updateFilteredTasks();
            return removed;
        }
        return false;
    }

    int indexOf(Task t) {
        if (t != null && this.mTaskIndices.containsKey(t.key)) {
            return this.mTaskIndices.get(t.key).intValue();
        }
        return -1;
    }

    int size() {
        return this.mFilteredTasks.size();
    }

    boolean contains(Task t) {
        return this.mTaskIndices.containsKey(t.key);
    }

    private void updateFilteredTasks() {
        this.mFilteredTasks.clear();
        if (this.mFilter != null) {
            SparseArray<Task> taskIdMap = new SparseArray<>();
            int taskCount = this.mTasks.size();
            for (int i = 0; i < taskCount; i++) {
                Task t = this.mTasks.get(i);
                taskIdMap.put(t.key.id, t);
            }
            for (int i2 = 0; i2 < taskCount; i2++) {
                Task t2 = this.mTasks.get(i2);
                if (this.mFilter.acceptTask(taskIdMap, t2, i2)) {
                    this.mFilteredTasks.add(t2);
                }
            }
        } else {
            this.mFilteredTasks.addAll(this.mTasks);
        }
        updateFilteredTaskIndices();
    }

    private void updateFilteredTaskIndices() {
        int taskCount = this.mFilteredTasks.size();
        this.mTaskIndices.clear();
        for (int i = 0; i < taskCount; i++) {
            Task t = this.mFilteredTasks.get(i);
            this.mTaskIndices.put(t.key, Integer.valueOf(i));
        }
    }

    ArrayList<Task> getTasks() {
        return this.mFilteredTasks;
    }
}
