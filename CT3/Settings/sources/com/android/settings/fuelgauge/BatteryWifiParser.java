package com.android.settings.fuelgauge;

import android.os.BatteryStats;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class BatteryWifiParser extends BatteryFlagParser {
    public BatteryWifiParser(int accentColor) {
        super(accentColor, false, 0);
    }

    @Override
    protected boolean isSet(BatteryStats.HistoryItem record) {
        switch ((record.states2 & 15) >> 0) {
            case DefaultWfcSettingsExt.RESUME:
            case DefaultWfcSettingsExt.PAUSE:
            case DefaultWfcSettingsExt.CREATE:
            case DefaultWfcSettingsExt.DESTROY:
            case 11:
            case 12:
                return false;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            default:
                return true;
        }
    }
}
