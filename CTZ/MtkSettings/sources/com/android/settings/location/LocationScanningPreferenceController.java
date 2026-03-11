package com.android.settings.location;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class LocationScanningPreferenceController extends BasePreferenceController {
    static final String KEY_LOCATION_SCANNING = "location_scanning";

    public LocationScanningPreferenceController(Context context) {
        super(context, KEY_LOCATION_SCANNING);
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_show_location_scanning)) {
            return 0;
        }
        return 2;
    }
}
