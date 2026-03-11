package com.android.settings.fuelgauge.anomaly;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Objects;

public class Anomaly implements Parcelable {
    public static final int[] ANOMALY_TYPE_LIST = {0, 1, 2};
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Anomaly createFromParcel(Parcel parcel) {
            return new Anomaly(parcel);
        }

        @Override
        public Anomaly[] newArray(int i) {
            return new Anomaly[i];
        }
    };
    public final boolean backgroundRestrictionEnabled;
    public final long bluetoothScanningTimeMs;
    public final CharSequence displayName;
    public final String packageName;
    public final int targetSdkVersion;
    public final int type;
    public final int uid;
    public final long wakelockTimeMs;
    public final int wakeupAlarmCount;

    private Anomaly(Builder builder) {
        this.type = builder.mType;
        this.uid = builder.mUid;
        this.displayName = builder.mDisplayName;
        this.packageName = builder.mPackageName;
        this.wakelockTimeMs = builder.mWakeLockTimeMs;
        this.targetSdkVersion = builder.mTargetSdkVersion;
        this.backgroundRestrictionEnabled = builder.mBgRestrictionEnabled;
        this.bluetoothScanningTimeMs = builder.mBluetoothScanningTimeMs;
        this.wakeupAlarmCount = builder.mWakeupAlarmCount;
    }

    private Anomaly(Parcel parcel) {
        this.type = parcel.readInt();
        this.uid = parcel.readInt();
        this.displayName = parcel.readCharSequence();
        this.packageName = parcel.readString();
        this.wakelockTimeMs = parcel.readLong();
        this.targetSdkVersion = parcel.readInt();
        this.backgroundRestrictionEnabled = parcel.readBoolean();
        this.wakeupAlarmCount = parcel.readInt();
        this.bluetoothScanningTimeMs = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.type);
        parcel.writeInt(this.uid);
        parcel.writeCharSequence(this.displayName);
        parcel.writeString(this.packageName);
        parcel.writeLong(this.wakelockTimeMs);
        parcel.writeInt(this.targetSdkVersion);
        parcel.writeBoolean(this.backgroundRestrictionEnabled);
        parcel.writeInt(this.wakeupAlarmCount);
        parcel.writeLong(this.bluetoothScanningTimeMs);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Anomaly)) {
            return false;
        }
        Anomaly anomaly = (Anomaly) obj;
        return this.type == anomaly.type && this.uid == anomaly.uid && this.wakelockTimeMs == anomaly.wakelockTimeMs && TextUtils.equals(this.displayName, anomaly.displayName) && TextUtils.equals(this.packageName, anomaly.packageName) && this.targetSdkVersion == anomaly.targetSdkVersion && this.backgroundRestrictionEnabled == anomaly.backgroundRestrictionEnabled && this.wakeupAlarmCount == anomaly.wakeupAlarmCount && this.bluetoothScanningTimeMs == anomaly.bluetoothScanningTimeMs;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.type), Integer.valueOf(this.uid), this.displayName, this.packageName, Long.valueOf(this.wakelockTimeMs), Integer.valueOf(this.targetSdkVersion), Boolean.valueOf(this.backgroundRestrictionEnabled), Integer.valueOf(this.wakeupAlarmCount), Long.valueOf(this.bluetoothScanningTimeMs));
    }

    public String toString() {
        return "type=" + toAnomalyTypeText(this.type) + " uid=" + this.uid + " package=" + this.packageName + " displayName=" + ((Object) this.displayName) + " wakelockTimeMs=" + this.wakelockTimeMs + " wakeupAlarmCount=" + this.wakeupAlarmCount + " bluetoothTimeMs=" + this.bluetoothScanningTimeMs;
    }

    private String toAnomalyTypeText(int i) {
        switch (i) {
            case 0:
                return "wakelock";
            case 1:
                return "wakeupAlarm";
            case 2:
                return "unoptimizedBluetoothScan";
            default:
                return "";
        }
    }

    public static final class Builder {
        private boolean mBgRestrictionEnabled;
        private long mBluetoothScanningTimeMs;
        private CharSequence mDisplayName;
        private String mPackageName;
        private int mTargetSdkVersion;
        private int mType;
        private int mUid;
        private long mWakeLockTimeMs;
        private int mWakeupAlarmCount;

        public Builder setType(int i) {
            this.mType = i;
            return this;
        }

        public Builder setUid(int i) {
            this.mUid = i;
            return this;
        }

        public Builder setDisplayName(CharSequence charSequence) {
            this.mDisplayName = charSequence;
            return this;
        }

        public Builder setPackageName(String str) {
            this.mPackageName = str;
            return this;
        }

        public Builder setWakeLockTimeMs(long j) {
            this.mWakeLockTimeMs = j;
            return this;
        }

        public Builder setTargetSdkVersion(int i) {
            this.mTargetSdkVersion = i;
            return this;
        }

        public Builder setBackgroundRestrictionEnabled(boolean z) {
            this.mBgRestrictionEnabled = z;
            return this;
        }

        public Builder setWakeupAlarmCount(int i) {
            this.mWakeupAlarmCount = i;
            return this;
        }

        public Builder setBluetoothScanningTimeMs(long j) {
            this.mBluetoothScanningTimeMs = j;
            return this;
        }

        public Anomaly build() {
            return new Anomaly(this);
        }
    }
}
