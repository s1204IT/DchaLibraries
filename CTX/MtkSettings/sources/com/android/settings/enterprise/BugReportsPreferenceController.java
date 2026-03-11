package com.android.settings.enterprise;

import android.content.Context;
import java.util.Date;

public class BugReportsPreferenceController extends AdminActionPreferenceControllerBase {
    public BugReportsPreferenceController(Context context) {
        super(context);
    }

    @Override
    protected Date getAdminActionTimestamp() {
        return this.mFeatureProvider.getLastBugReportRequestTime();
    }

    @Override
    public String getPreferenceKey() {
        return "bug_reports";
    }
}
