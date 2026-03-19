package android.app.timezone;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class RulesState implements Parcelable {
    private static final byte BYTE_FALSE = 0;
    private static final byte BYTE_TRUE = 1;
    public static final Parcelable.Creator<RulesState> CREATOR = new Parcelable.Creator<RulesState>() {
        @Override
        public RulesState createFromParcel(Parcel parcel) {
            return RulesState.createFromParcel(parcel);
        }

        @Override
        public RulesState[] newArray(int i) {
            return new RulesState[i];
        }
    };
    public static final int DISTRO_STATUS_INSTALLED = 2;
    public static final int DISTRO_STATUS_NONE = 1;
    public static final int DISTRO_STATUS_UNKNOWN = 0;
    public static final int STAGED_OPERATION_INSTALL = 3;
    public static final int STAGED_OPERATION_NONE = 1;
    public static final int STAGED_OPERATION_UNINSTALL = 2;
    public static final int STAGED_OPERATION_UNKNOWN = 0;
    private final DistroFormatVersion mDistroFormatVersionSupported;
    private final int mDistroStatus;
    private final DistroRulesVersion mInstalledDistroRulesVersion;
    private final boolean mOperationInProgress;
    private final DistroRulesVersion mStagedDistroRulesVersion;
    private final int mStagedOperationType;
    private final String mSystemRulesVersion;

    @Retention(RetentionPolicy.SOURCE)
    private @interface DistroStatus {
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface StagedOperationType {
    }

    public RulesState(String str, DistroFormatVersion distroFormatVersion, boolean z, int i, DistroRulesVersion distroRulesVersion, int i2, DistroRulesVersion distroRulesVersion2) {
        this.mSystemRulesVersion = Utils.validateRulesVersion("systemRulesVersion", str);
        this.mDistroFormatVersionSupported = (DistroFormatVersion) Utils.validateNotNull("distroFormatVersionSupported", distroFormatVersion);
        this.mOperationInProgress = z;
        if (z && i != 0) {
            throw new IllegalArgumentException("stagedOperationType != STAGED_OPERATION_UNKNOWN");
        }
        this.mStagedOperationType = validateStagedOperation(i);
        this.mStagedDistroRulesVersion = (DistroRulesVersion) Utils.validateConditionalNull(this.mStagedOperationType == 3, "stagedDistroRulesVersion", distroRulesVersion);
        this.mDistroStatus = validateDistroStatus(i2);
        this.mInstalledDistroRulesVersion = (DistroRulesVersion) Utils.validateConditionalNull(this.mDistroStatus == 2, "installedDistroRulesVersion", distroRulesVersion2);
    }

    public String getSystemRulesVersion() {
        return this.mSystemRulesVersion;
    }

    public boolean isOperationInProgress() {
        return this.mOperationInProgress;
    }

    public int getStagedOperationType() {
        return this.mStagedOperationType;
    }

    public DistroRulesVersion getStagedDistroRulesVersion() {
        return this.mStagedDistroRulesVersion;
    }

    public int getDistroStatus() {
        return this.mDistroStatus;
    }

    public DistroRulesVersion getInstalledDistroRulesVersion() {
        return this.mInstalledDistroRulesVersion;
    }

    public boolean isDistroFormatVersionSupported(DistroFormatVersion distroFormatVersion) {
        return this.mDistroFormatVersionSupported.supports(distroFormatVersion);
    }

    public boolean isSystemVersionNewerThan(DistroRulesVersion distroRulesVersion) {
        return this.mSystemRulesVersion.compareTo(distroRulesVersion.getRulesVersion()) > 0;
    }

    private static RulesState createFromParcel(Parcel parcel) {
        boolean z;
        String string = parcel.readString();
        DistroFormatVersion distroFormatVersion = (DistroFormatVersion) parcel.readParcelable(null);
        if (parcel.readByte() != 1) {
            z = false;
        } else {
            z = true;
        }
        return new RulesState(string, distroFormatVersion, z, parcel.readByte(), (DistroRulesVersion) parcel.readParcelable(null), parcel.readByte(), (DistroRulesVersion) parcel.readParcelable(null));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mSystemRulesVersion);
        parcel.writeParcelable(this.mDistroFormatVersionSupported, 0);
        parcel.writeByte(this.mOperationInProgress ? (byte) 1 : (byte) 0);
        parcel.writeByte((byte) this.mStagedOperationType);
        parcel.writeParcelable(this.mStagedDistroRulesVersion, 0);
        parcel.writeByte((byte) this.mDistroStatus);
        parcel.writeParcelable(this.mInstalledDistroRulesVersion, 0);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RulesState rulesState = (RulesState) obj;
        if (this.mOperationInProgress != rulesState.mOperationInProgress || this.mStagedOperationType != rulesState.mStagedOperationType || this.mDistroStatus != rulesState.mDistroStatus || !this.mSystemRulesVersion.equals(rulesState.mSystemRulesVersion) || !this.mDistroFormatVersionSupported.equals(rulesState.mDistroFormatVersionSupported)) {
            return false;
        }
        if (this.mStagedDistroRulesVersion == null ? rulesState.mStagedDistroRulesVersion != null : !this.mStagedDistroRulesVersion.equals(rulesState.mStagedDistroRulesVersion)) {
            return false;
        }
        if (this.mInstalledDistroRulesVersion != null) {
            return this.mInstalledDistroRulesVersion.equals(rulesState.mInstalledDistroRulesVersion);
        }
        if (rulesState.mInstalledDistroRulesVersion == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int iHashCode;
        int iHashCode2 = ((((((this.mSystemRulesVersion.hashCode() * 31) + this.mDistroFormatVersionSupported.hashCode()) * 31) + (this.mOperationInProgress ? 1 : 0)) * 31) + this.mStagedOperationType) * 31;
        if (this.mStagedDistroRulesVersion != null) {
            iHashCode = this.mStagedDistroRulesVersion.hashCode();
        } else {
            iHashCode = 0;
        }
        return (31 * (((iHashCode2 + iHashCode) * 31) + this.mDistroStatus)) + (this.mInstalledDistroRulesVersion != null ? this.mInstalledDistroRulesVersion.hashCode() : 0);
    }

    public String toString() {
        return "RulesState{mSystemRulesVersion='" + this.mSystemRulesVersion + DateFormat.QUOTE + ", mDistroFormatVersionSupported=" + this.mDistroFormatVersionSupported + ", mOperationInProgress=" + this.mOperationInProgress + ", mStagedOperationType=" + this.mStagedOperationType + ", mStagedDistroRulesVersion=" + this.mStagedDistroRulesVersion + ", mDistroStatus=" + this.mDistroStatus + ", mInstalledDistroRulesVersion=" + this.mInstalledDistroRulesVersion + '}';
    }

    private static int validateStagedOperation(int i) {
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("Unknown operation type=" + i);
        }
        return i;
    }

    private static int validateDistroStatus(int i) {
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException("Unknown distro status=" + i);
        }
        return i;
    }
}
