package com.android.systemui.recents.model;

import android.util.ArrayMap;
import com.android.systemui.recents.model.Task;
import java.util.ArrayList;

public class TaskGrouping {
    int affiliation;
    long latestActiveTimeInGroup;
    Task.TaskKey mFrontMostTaskKey;
    ArrayList<Task.TaskKey> mTaskKeys = new ArrayList<>();
    ArrayMap<Task.TaskKey, Integer> mTaskKeyIndices = new ArrayMap<>();

    public TaskGrouping(int affiliation) {
        this.affiliation = affiliation;
    }

    void addTask(Task t) {
        this.mTaskKeys.add(t.key);
        if (t.key.lastActiveTime > this.latestActiveTimeInGroup) {
            this.latestActiveTimeInGroup = t.key.lastActiveTime;
        }
        t.setGroup(this);
        updateTaskIndices();
    }

    void removeTask(Task t) {
        this.mTaskKeys.remove(t.key);
        this.latestActiveTimeInGroup = 0L;
        int taskCount = this.mTaskKeys.size();
        for (int i = 0; i < taskCount; i++) {
            long lastActiveTime = this.mTaskKeys.get(i).lastActiveTime;
            if (lastActiveTime > this.latestActiveTimeInGroup) {
                this.latestActiveTimeInGroup = lastActiveTime;
            }
        }
        t.setGroup(null);
        updateTaskIndices();
    }

    public Task.TaskKey getNextTaskInGroup(Task t) {
        int i = indexOf(t);
        if (i + 1 < getTaskCount()) {
            return this.mTaskKeys.get(i + 1);
        }
        return null;
    }

    public Task.TaskKey getPrevTaskInGroup(Task t) {
        int i = indexOf(t);
        if (i - 1 >= 0) {
            return this.mTaskKeys.get(i - 1);
        }
        return null;
    }

    public boolean isFrontMostTask(Task t) {
        return t.key == this.mFrontMostTaskKey;
    }

    public int indexOf(Task t) {
        return this.mTaskKeyIndices.get(t.key).intValue();
    }

    public boolean isTaskAboveTask(Task t, Task below) {
        if (this.mTaskKeyIndices.containsKey(t.key) && this.mTaskKeyIndices.containsKey(below.key)) {
            return this.mTaskKeyIndices.get(t.key).intValue() > this.mTaskKeyIndices.get(below.key).intValue();
        }
        return false;
    }

    public int getTaskCount() {
        return this.mTaskKeys.size();
    }

    private void updateTaskIndices() {
        if (this.mTaskKeys.isEmpty()) {
            this.mFrontMostTaskKey = null;
            this.mTaskKeyIndices.clear();
            return;
        }
        int taskCount = this.mTaskKeys.size();
        this.mFrontMostTaskKey = this.mTaskKeys.get(this.mTaskKeys.size() - 1);
        this.mTaskKeyIndices.clear();
        for (int i = 0; i < taskCount; i++) {
            Task.TaskKey k = this.mTaskKeys.get(i);
            this.mTaskKeyIndices.put(k, Integer.valueOf(i));
        }
    }
}
