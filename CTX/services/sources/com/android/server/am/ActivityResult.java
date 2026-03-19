package com.android.server.am;

import android.app.ResultInfo;
import android.content.Intent;

final class ActivityResult extends ResultInfo {
    final ActivityRecord mFrom;

    public ActivityResult(ActivityRecord activityRecord, String str, int i, int i2, Intent intent) {
        super(str, i, i2, intent);
        this.mFrom = activityRecord;
    }
}
