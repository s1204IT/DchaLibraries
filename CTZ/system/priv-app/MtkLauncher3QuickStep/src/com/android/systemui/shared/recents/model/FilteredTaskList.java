package com.android.systemui.shared.recents.model;

import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
class FilteredTaskList {
    private TaskFilter mFilter;
    private final ArrayList<Task> mTasks = new ArrayList<>();
    private final ArrayList<Task> mFilteredTasks = new ArrayList<>();
    private final ArrayMap<Task.TaskKey, Integer> mFilteredTaskIndices = new ArrayMap<>();

    FilteredTaskList() {
    }

    boolean setFilter(TaskFilter filter) {
        ArrayList<Task> prevFilteredTasks = new ArrayList<>(this.mFilteredTasks);
        this.mFilter = filter;
        updateFilteredTasks();
        return !prevFilteredTasks.equals(this.mFilteredTasks);
    }

    void add(Task t) {
        this.mTasks.add(t);
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
        if (t != null && this.mFilteredTaskIndices.containsKey(t.key)) {
            return this.mFilteredTaskIndices.get(t.key).intValue();
        }
        return -1;
    }

    int size() {
        return this.mFilteredTasks.size();
    }

    boolean contains(Task t) {
        return this.mFilteredTaskIndices.containsKey(t.key);
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
        this.mFilteredTaskIndices.clear();
        for (int i = 0; i < taskCount; i++) {
            Task t = this.mFilteredTasks.get(i);
            this.mFilteredTaskIndices.put(t.key, Integer.valueOf(i));
        }
    }

    ArrayList<Task> getTasks() {
        return this.mFilteredTasks;
    }
}
