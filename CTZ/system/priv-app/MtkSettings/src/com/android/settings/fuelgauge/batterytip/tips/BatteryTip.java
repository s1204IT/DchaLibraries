package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.util.SparseIntArray;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/* loaded from: classes.dex */
public abstract class BatteryTip implements Parcelable, Comparable<BatteryTip> {
    static final SparseIntArray TIP_ORDER = new SparseIntArray();
    protected boolean mNeedUpdate;
    protected boolean mShowDialog;
    protected int mState;
    protected int mType;

    public abstract int getIconId();

    public abstract CharSequence getSummary(Context context);

    public abstract CharSequence getTitle(Context context);

    public abstract void log(Context context, MetricsFeatureProvider metricsFeatureProvider);

    public abstract void updateState(BatteryTip batteryTip);

    static {
        TIP_ORDER.append(1, 0);
        TIP_ORDER.append(3, 1);
        TIP_ORDER.append(2, 2);
        TIP_ORDER.append(5, 3);
        TIP_ORDER.append(6, 4);
        TIP_ORDER.append(0, 5);
        TIP_ORDER.append(4, 6);
        TIP_ORDER.append(7, 7);
    }

    BatteryTip(Parcel parcel) {
        this.mType = parcel.readInt();
        this.mState = parcel.readInt();
        this.mShowDialog = parcel.readBoolean();
        this.mNeedUpdate = parcel.readBoolean();
    }

    BatteryTip(int i, int i2, boolean z) {
        this.mType = i;
        this.mState = i2;
        this.mShowDialog = z;
        this.mNeedUpdate = true;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        parcel.writeInt(this.mState);
        parcel.writeBoolean(this.mShowDialog);
        parcel.writeBoolean(this.mNeedUpdate);
    }

    public Preference buildPreference(Context context) {
        Preference preference = new Preference(context);
        preference.setKey(getKey());
        preference.setTitle(getTitle(context));
        preference.setSummary(getSummary(context));
        preference.setIcon(getIconId());
        return preference;
    }

    public boolean shouldShowDialog() {
        return this.mShowDialog;
    }

    public boolean needUpdate() {
        return this.mNeedUpdate;
    }

    public String getKey() {
        return "key_battery_tip" + this.mType;
    }

    public int getType() {
        return this.mType;
    }

    public int getState() {
        return this.mState;
    }

    /* JADX DEBUG: Method merged with bridge method: compareTo(Ljava/lang/Object;)I */
    @Override // java.lang.Comparable
    public int compareTo(BatteryTip batteryTip) {
        return TIP_ORDER.get(this.mType) - TIP_ORDER.get(batteryTip.mType);
    }

    public String toString() {
        return "type=" + this.mType + " state=" + this.mState;
    }
}
