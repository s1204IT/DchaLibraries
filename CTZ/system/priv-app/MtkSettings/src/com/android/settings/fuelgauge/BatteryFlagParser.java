package com.android.settings.fuelgauge;

import android.os.BatteryStats;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.settings.fuelgauge.BatteryActiveView;
import com.android.settings.fuelgauge.BatteryInfo;

/* loaded from: classes.dex */
public class BatteryFlagParser implements BatteryActiveView.BatteryActiveProvider, BatteryInfo.BatteryDataParser {
    private final int mAccentColor;
    private final SparseBooleanArray mData = new SparseBooleanArray();
    private final int mFlag;
    private boolean mLastSet;
    private long mLastTime;
    private long mLength;
    private final boolean mState2;

    public BatteryFlagParser(int i, boolean z, int i2) {
        this.mAccentColor = i;
        this.mFlag = i2;
        this.mState2 = z;
    }

    protected boolean isSet(BatteryStats.HistoryItem historyItem) {
        return ((this.mState2 ? historyItem.states2 : historyItem.states) & this.mFlag) != 0;
    }

    @Override // com.android.settings.fuelgauge.BatteryInfo.BatteryDataParser
    public void onParsingStarted(long j, long j2) {
        this.mLength = j2 - j;
    }

    @Override // com.android.settings.fuelgauge.BatteryInfo.BatteryDataParser
    public void onDataPoint(long j, BatteryStats.HistoryItem historyItem) {
        boolean zIsSet = isSet(historyItem);
        if (zIsSet != this.mLastSet) {
            this.mData.put((int) j, zIsSet);
            this.mLastSet = zIsSet;
        }
        this.mLastTime = j;
    }

    @Override // com.android.settings.fuelgauge.BatteryInfo.BatteryDataParser
    public void onDataGap() {
        if (this.mLastSet) {
            this.mData.put((int) this.mLastTime, false);
            this.mLastSet = false;
        }
    }

    @Override // com.android.settings.fuelgauge.BatteryInfo.BatteryDataParser
    public void onParsingDone() {
        if (this.mLastSet) {
            this.mData.put((int) this.mLastTime, false);
            this.mLastSet = false;
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

    private int getColor(boolean z) {
        if (z) {
            return this.mAccentColor;
        }
        return 0;
    }
}
