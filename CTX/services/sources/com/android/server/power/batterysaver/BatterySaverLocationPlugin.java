package com.android.server.power.batterysaver;

import android.content.Context;
import android.provider.Settings;
import com.android.server.power.batterysaver.BatterySaverController;

public class BatterySaverLocationPlugin implements BatterySaverController.Plugin {
    private static final boolean DEBUG = false;
    private static final String TAG = "BatterySaverLocationPlugin";
    private final Context mContext;

    public BatterySaverLocationPlugin(Context context) {
        this.mContext = context;
    }

    @Override
    public void onBatterySaverChanged(BatterySaverController batterySaverController) {
        updateLocationState(batterySaverController);
    }

    @Override
    public void onSystemReady(BatterySaverController batterySaverController) {
        updateLocationState(batterySaverController);
    }

    private void updateLocationState(BatterySaverController batterySaverController) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "location_global_kill_switch", (batterySaverController.getBatterySaverPolicy().getGpsMode() == 2 && batterySaverController.isEnabled() && !batterySaverController.isInteractive()) ? 1 : 0);
    }
}
