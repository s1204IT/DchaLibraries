package com.android.settings.connecteddevice;

import android.content.Context;
import android.provider.Settings;
import android.util.FeatureFlagUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;

/* loaded from: classes.dex */
public class BluetoothOnWhileDrivingPreferenceController extends TogglePreferenceController implements PreferenceControllerMixin {
    static final String KEY_BLUETOOTH_ON_DRIVING = "bluetooth_on_while_driving";

    public BluetoothOnWhileDrivingPreferenceController(Context context) {
        super(context, KEY_BLUETOOTH_ON_DRIVING);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (FeatureFlagUtils.isEnabled(this.mContext, "settings_bluetooth_while_driving")) {
            return 0;
        }
        return 1;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), KEY_BLUETOOTH_ON_DRIVING, 0) != 0;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        return Settings.Secure.putInt(this.mContext.getContentResolver(), KEY_BLUETOOTH_ON_DRIVING, z ? 1 : 0);
    }
}
