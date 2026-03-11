package com.android.settings.connecteddevice;

import android.content.Context;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.nfc.NfcPreferenceController;

public class AdvancedConnectedDeviceController extends BasePreferenceController {
    private static final String DRIVING_MODE_SETTINGS_ENABLED = "gearhead:driving_mode_settings_enabled";

    public AdvancedConnectedDeviceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    public CharSequence getSummary() {
        return this.mContext.getText(getConnectedDevicesSummaryResourceId(this.mContext));
    }

    public static int getConnectedDevicesSummaryResourceId(Context context) {
        return getConnectedDevicesSummaryResourceId(new NfcPreferenceController(context, NfcPreferenceController.KEY_TOGGLE_NFC), isDrivingModeAvailable(context));
    }

    static boolean isDrivingModeAvailable(Context context) {
        return Settings.System.getInt(context.getContentResolver(), DRIVING_MODE_SETTINGS_ENABLED, 0) == 1;
    }

    static int getConnectedDevicesSummaryResourceId(NfcPreferenceController nfcPreferenceController, boolean z) {
        if (nfcPreferenceController.isAvailable()) {
            if (z) {
                return R.string.connected_devices_dashboard_summary;
            }
            return R.string.connected_devices_dashboard_no_driving_mode_summary;
        }
        if (z) {
            return R.string.connected_devices_dashboard_no_nfc_summary;
        }
        return R.string.connected_devices_dashboard_no_driving_mode_no_nfc_summary;
    }
}
