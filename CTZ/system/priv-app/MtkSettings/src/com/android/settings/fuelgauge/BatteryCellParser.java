package com.android.settings.fuelgauge;

import android.os.BatteryStats;
import android.util.SparseIntArray;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryActiveView;
import com.android.settings.fuelgauge.BatteryInfo;

/* loaded from: classes.dex */
public class BatteryCellParser implements BatteryActiveView.BatteryActiveProvider, BatteryInfo.BatteryDataParser {
    private final SparseIntArray mData = new SparseIntArray();
    private long mLastTime;
    private int mLastValue;
    private long mLength;

    protected int getValue(BatteryStats.HistoryItem historyItem) {
        if (((historyItem.states & 448) >> 6) == 3) {
            return 0;
        }
        if ((historyItem.states & 2097152) != 0) {
            return 1;
        }
        return ((historyItem.states & 56) >> 3) + 2;
    }

    @Override // com.android.settings.fuelgauge.BatteryInfo.BatteryDataParser
    public void onParsingStarted(long j, long j2) {
        this.mLength = j2 - j;
    }

    @Override // com.android.settings.fuelgauge.BatteryInfo.BatteryDataParser
    public void onDataPoint(long j, BatteryStats.HistoryItem historyItem) {
        int value = getValue(historyItem);
        if (value != this.mLastValue) {
            this.mData.put((int) j, value);
            this.mLastValue = value;
        }
        this.mLastTime = j;
    }

    @Override // com.android.settings.fuelgauge.BatteryInfo.BatteryDataParser
    public void onDataGap() {
        if (this.mLastValue != 0) {
            this.mData.put((int) this.mLastTime, 0);
            this.mLastValue = 0;
        }
    }

    @Override // com.android.settings.fuelgauge.BatteryInfo.BatteryDataParser
    public void onParsingDone() {
        if (this.mLastValue != 0) {
            this.mData.put((int) this.mLastTime, 0);
            this.mLastValue = 0;
        }
    }

    @Override // com.android.settings.fuelgauge.BatteryActiveView.BatteryActiveProvider
    public long getPeriod() {
        return this.mLength;
    }

    @Override // com.android.settings.fuelgauge.BatteryActiveView.BatteryActiveProvider
    public boolean hasData() {
        return this.mData.size() > 1;
    }

    @Override // com.android.settings.fuelgauge.BatteryActiveView.BatteryActiveProvider
    public SparseIntArray getColorArray() {
        SparseIntArray sparseIntArray = new SparseIntArray();
        for (int i = 0; i < this.mData.size(); i++) {
            sparseIntArray.put(this.mData.keyAt(i), getColor(this.mData.valueAt(i)));
        }
        return sparseIntArray;
    }

    private int getColor(int i) {
        return Utils.BADNESS_COLORS[i];
    }
}
