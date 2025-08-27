package com.android.quickstep.fallback;

import com.android.launcher3.uioverrides.TaskViewTouchController;
import com.android.quickstep.RecentsActivity;

/* loaded from: classes.dex */
public class RecentsTaskController extends TaskViewTouchController<RecentsActivity> {
    public RecentsTaskController(RecentsActivity recentsActivity) {
        super(recentsActivity);
    }

    @Override // com.android.launcher3.uioverrides.TaskViewTouchController
    protected boolean isRecentsInteractive() {
        return ((RecentsActivity) this.mActivity).hasWindowFocus();
    }
}
