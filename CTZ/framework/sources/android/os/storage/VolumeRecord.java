package android.os.storage;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.DebugUtils;
import android.util.TimeUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import java.util.Objects;

public class VolumeRecord implements Parcelable {
    public static final Parcelable.Creator<VolumeRecord> CREATOR = new Parcelable.Creator<VolumeRecord>() {
        @Override
        public VolumeRecord createFromParcel(Parcel parcel) {
            return new VolumeRecord(parcel);
        }

        @Override
        public VolumeRecord[] newArray(int i) {
            return new VolumeRecord[i];
        }
    };
    public static final String EXTRA_FS_UUID = "android.os.storage.extra.FS_UUID";
    public static final int USER_FLAG_INITED = 1;
    public static final int USER_FLAG_SNOOZED = 2;
    public long createdMillis;
    public final String fsUuid;
    public long lastBenchMillis;
    public long lastTrimMillis;
    public String nickname;
    public String partGuid;
    public final int type;
    public int userFlags;

    public VolumeRecord(int i, String str) {
        this.type = i;
        this.fsUuid = (String) Preconditions.checkNotNull(str);
    }

    public VolumeRecord(Parcel parcel) {
        this.type = parcel.readInt();
        this.fsUuid = parcel.readString();
        this.partGuid = parcel.readString();
        this.nickname = parcel.readString();
        this.userFlags = parcel.readInt();
        this.createdMillis = parcel.readLong();
        this.lastTrimMillis = parcel.readLong();
        this.lastBenchMillis = parcel.readLong();
    }

    public int getType() {
        return this.type;
    }

    public String getFsUuid() {
        return this.fsUuid;
    }

    public String getNickname() {
        return this.nickname;
    }

    public boolean isInited() {
        return (this.userFlags & 1) != 0;
    }

    public boolean isSnoozed() {
        return (this.userFlags & 2) != 0;
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("VolumeRecord:");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.printPair("type", DebugUtils.valueToString(VolumeInfo.class, "TYPE_", this.type));
        indentingPrintWriter.printPair("fsUuid", this.fsUuid);
        indentingPrintWriter.printPair("partGuid", this.partGuid);
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("nickname", this.nickname);
        indentingPrintWriter.printPair("userFlags", DebugUtils.flagsToString(VolumeRecord.class, "USER_FLAG_", this.userFlags));
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("createdMillis", TimeUtils.formatForLogging(this.createdMillis));
        indentingPrintWriter.printPair("lastTrimMillis", TimeUtils.formatForLogging(this.lastTrimMillis));
        indentingPrintWriter.printPair("lastBenchMillis", TimeUtils.formatForLogging(this.lastBenchMillis));
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
    }

    public VolumeRecord m28clone() {
        Parcel parcelObtain = Parcel.obtain();
        try {
            writeToParcel(parcelObtain, 0);
            parcelObtain.setDataPosition(0);
            return CREATOR.createFromParcel(parcelObtain);
        } finally {
            parcelObtain.recycle();
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof VolumeRecord) {
            return Objects.equals(this.fsUuid, ((VolumeRecord) obj).fsUuid);
        }
        return false;
    }

    public int hashCode() {
        return this.fsUuid.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.type);
        parcel.writeString(this.fsUuid);
        parcel.writeString(this.partGuid);
        parcel.writeString(this.nickname);
        parcel.writeInt(this.userFlags);
        parcel.writeLong(this.createdMillis);
        parcel.writeLong(this.lastTrimMillis);
        parcel.writeLong(this.lastBenchMillis);
    }
}
