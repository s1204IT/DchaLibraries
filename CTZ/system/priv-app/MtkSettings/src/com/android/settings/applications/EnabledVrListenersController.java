package com.android.settings.applications;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/* loaded from: classes.dex */
public class EnabledVrListenersController extends BasePreferenceController {
    static final String KEY_ENABLED_VR_LISTENERS = "enabled_vr_listeners";

    public EnabledVrListenersController(Context context) {
        super(context, KEY_ENABLED_VR_LISTENERS);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_show_enabled_vr_listeners)) {
            return 0;
        }
        return 2;
    }
}
