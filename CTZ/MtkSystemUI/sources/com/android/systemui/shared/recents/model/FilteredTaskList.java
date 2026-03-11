package com.android.systemui.shared.recents.model;

import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task;
import java.util.ArrayList;
import java.util.List;

class FilteredTaskList {
    private TaskFilter mFilter;
    private final ArrayList<Task> mTasks = new ArrayList<>();
    private final ArrayList<Task> mFilteredTasks = new ArrayList<>();
    private final ArrayMap<Task.TaskKey, Integer> mFilteredTaskIndices = new ArrayMap<>();

    FilteredTaskList() {
    }

    boolean setFilter(TaskFilter taskFilter) {
        ArrayList arrayList = new ArrayList(this.mFilteredTasks);
        this.mFilter = taskFilter;
        updateFilteredTasks();
        return !arrayList.equals(this.mFilteredTasks);
    }

    void set(List<Task> list) {
        this.mTasks.clear();
        this.mTasks.addAll(list);
        updateFilteredTasks();
    }

    boolean remove(Task task) {
        if (this.mFilteredTasks.contains(task)) {
            boolean zRemove = this.mTasks.remove(task);
            updateFilteredTasks();
            return zRemove;
        }
        return false;
    }

    int indexOf(Task task) {
        if (task != null && this.mFilteredTaskIndices.containsKey(task.key)) {
            return this.mFilteredTaskIndices.get(task.key).intValue();
        }
        return -1;
    }

    int size() {
        return this.mFilteredTasks.size();
    }

    boolean contains(Task task) {
        return this.mFilteredTaskIndices.containsKey(task.key);
    }

    private void updateFilteredTasks() {
        this.mFilteredTasks.clear();
        if (this.mFilter != null) {
            SparseArray<Task> sparseArray = new SparseArray<>();
            int size = this.mTasks.size();
            for (int i = 0; i < size; i++) {
                Task task = this.mTasks.get(i);
                sparseArray.put(task.key.id, task);
            }
            for (int i2 = 0; i2 < size; i2++) {
                Task task2 = this.mTasks.get(i2);
                if (this.mFilter.acceptTask(sparseArray, task2, i2)) {
                    this.mFilteredTasks.add(task2);
                }
            }
        } else {
            this.mFilteredTasks.addAll(this.mTasks);
        }
        updateFilteredTaskIndices();
    }

    private void updateFilteredTaskIndices() {
        int size = this.mFilteredTasks.size();
        this.mFilteredTaskIndices.clear();
        for (int i = 0; i < size; i++) {
            this.mFilteredTaskIndices.put(this.mFilteredTasks.get(i).key, Integer.valueOf(i));
        }
    }

    ArrayList<Task> getTasks() {
        return this.mFilteredTasks;
    }
}
