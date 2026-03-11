package com.android.systemui.recents.model;

import android.util.SparseArray;

interface TaskFilter {
    boolean acceptTask(SparseArray<Task> sparseArray, Task task, int i);
}
