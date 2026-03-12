package com.android.systemui.recents.model;

interface TaskFilter {
    boolean acceptTask(Task task, int i);
}
