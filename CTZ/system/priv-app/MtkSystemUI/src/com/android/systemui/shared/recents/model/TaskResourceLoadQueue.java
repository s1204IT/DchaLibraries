package com.android.systemui.shared.recents.model;

import java.util.concurrent.ConcurrentLinkedQueue;

/* loaded from: classes.dex */
class TaskResourceLoadQueue {
    private final ConcurrentLinkedQueue<Task> mQueue = new ConcurrentLinkedQueue<>();

    TaskResourceLoadQueue() {
    }

    void addTask(Task task) {
        if (!this.mQueue.contains(task)) {
            this.mQueue.add(task);
        }
        synchronized (this) {
            notifyAll();
        }
    }

    Task nextTask() {
        return this.mQueue.poll();
    }

    void removeTask(Task task) {
        this.mQueue.remove(task);
    }

    void clearTasks() {
        this.mQueue.clear();
    }

    boolean isEmpty() {
        return this.mQueue.isEmpty();
    }
}
