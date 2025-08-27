package com.android.settings.enterprise;

import android.content.Context;
import java.util.Date;

/* loaded from: classes.dex */
public class BugReportsPreferenceController extends AdminActionPreferenceControllerBase {
    public BugReportsPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settings.enterprise.AdminActionPreferenceControllerBase
    protected Date getAdminActionTimestamp() {
        return this.mFeatureProvider.getLastBugReportRequestTime();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bug_reports";
    }
}
