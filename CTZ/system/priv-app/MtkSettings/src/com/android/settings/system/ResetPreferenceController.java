package com.android.settings.system;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/* loaded from: classes.dex */
public class ResetPreferenceController extends BasePreferenceController {
    public ResetPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_show_reset_dashboard)) {
            return 0;
        }
        return 2;
    }
}
