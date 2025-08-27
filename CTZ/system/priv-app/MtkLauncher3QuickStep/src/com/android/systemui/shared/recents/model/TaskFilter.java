package com.android.systemui.shared.recents.model;

import android.util.SparseArray;

/* loaded from: classes.dex */
public interface TaskFilter {
    boolean acceptTask(SparseArray<Task> sparseArray, Task task, int i);
}
