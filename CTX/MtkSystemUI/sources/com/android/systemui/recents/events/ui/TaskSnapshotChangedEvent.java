package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.shared.recents.model.ThumbnailData;

public class TaskSnapshotChangedEvent extends EventBus.Event {
    public final int taskId;
    public final ThumbnailData thumbnailData;

    public TaskSnapshotChangedEvent(int i, ThumbnailData thumbnailData) {
        this.taskId = i;
        this.thumbnailData = thumbnailData;
    }
}
