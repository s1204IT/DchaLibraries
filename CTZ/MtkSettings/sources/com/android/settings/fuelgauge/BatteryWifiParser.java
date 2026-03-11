package com.android.settings.fuelgauge;

import android.os.BatteryStats;

public class BatteryWifiParser extends BatteryFlagParser {
    public BatteryWifiParser(int i) {
        super(i, false, 0);
    }

    @Override
    protected boolean isSet(BatteryStats.HistoryItem historyItem) {
        int i = (historyItem.states2 & 15) >> 0;
        switch (i) {
            default:
                switch (i) {
                    case 11:
                    case 12:
                        break;
                    default:
                        return true;
                }
            case 0:
            case 1:
            case 2:
            case 3:
                return false;
        }
    }
}
