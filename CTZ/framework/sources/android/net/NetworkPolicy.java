package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.BackupUtils;
import android.util.Range;
import android.util.RecurrenceRule;
import com.android.internal.util.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Objects;

public class NetworkPolicy implements Parcelable, Comparable<NetworkPolicy> {
    public static final Parcelable.Creator<NetworkPolicy> CREATOR = new Parcelable.Creator<NetworkPolicy>() {
        @Override
        public NetworkPolicy createFromParcel(Parcel parcel) {
            return new NetworkPolicy(parcel);
        }

        @Override
        public NetworkPolicy[] newArray(int i) {
            return new NetworkPolicy[i];
        }
    };
    public static final int CYCLE_NONE = -1;
    private static final long DEFAULT_MTU = 1500;
    public static final long LIMIT_DISABLED = -1;
    public static final long SNOOZE_NEVER = -1;
    private static final int VERSION_INIT = 1;
    private static final int VERSION_RAPID = 3;
    private static final int VERSION_RULE = 2;
    public static final long WARNING_DISABLED = -1;
    public RecurrenceRule cycleRule;
    public boolean inferred;
    public long lastLimitSnooze;
    public long lastRapidSnooze;
    public long lastWarningSnooze;
    public long limitBytes;

    @Deprecated
    public boolean metered;
    public NetworkTemplate template;
    public long warningBytes;

    public static RecurrenceRule buildRule(int i, ZoneId zoneId) {
        if (i != -1) {
            return RecurrenceRule.buildRecurringMonthly(i, zoneId);
        }
        return RecurrenceRule.buildNever();
    }

    @Deprecated
    public NetworkPolicy(NetworkTemplate networkTemplate, int i, String str, long j, long j2, boolean z) {
        this(networkTemplate, i, str, j, j2, -1L, -1L, z, false);
    }

    @Deprecated
    public NetworkPolicy(NetworkTemplate networkTemplate, int i, String str, long j, long j2, long j3, long j4, boolean z, boolean z2) {
        this(networkTemplate, buildRule(i, ZoneId.of(str)), j, j2, j3, j4, z, z2);
    }

    @Deprecated
    public NetworkPolicy(NetworkTemplate networkTemplate, RecurrenceRule recurrenceRule, long j, long j2, long j3, long j4, boolean z, boolean z2) {
        this(networkTemplate, recurrenceRule, j, j2, j3, j4, -1L, z, z2);
    }

    public NetworkPolicy(NetworkTemplate networkTemplate, RecurrenceRule recurrenceRule, long j, long j2, long j3, long j4, long j5, boolean z, boolean z2) {
        this.warningBytes = -1L;
        this.limitBytes = -1L;
        this.lastWarningSnooze = -1L;
        this.lastLimitSnooze = -1L;
        this.lastRapidSnooze = -1L;
        this.metered = true;
        this.inferred = false;
        this.template = (NetworkTemplate) Preconditions.checkNotNull(networkTemplate, "missing NetworkTemplate");
        this.cycleRule = (RecurrenceRule) Preconditions.checkNotNull(recurrenceRule, "missing RecurrenceRule");
        this.warningBytes = j;
        this.limitBytes = j2;
        this.lastWarningSnooze = j3;
        this.lastLimitSnooze = j4;
        this.lastRapidSnooze = j5;
        this.metered = z;
        this.inferred = z2;
    }

    private NetworkPolicy(Parcel parcel) {
        boolean z;
        this.warningBytes = -1L;
        this.limitBytes = -1L;
        this.lastWarningSnooze = -1L;
        this.lastLimitSnooze = -1L;
        this.lastRapidSnooze = -1L;
        this.metered = true;
        this.inferred = false;
        this.template = (NetworkTemplate) parcel.readParcelable(null);
        this.cycleRule = (RecurrenceRule) parcel.readParcelable(null);
        this.warningBytes = parcel.readLong();
        this.limitBytes = parcel.readLong();
        this.lastWarningSnooze = parcel.readLong();
        this.lastLimitSnooze = parcel.readLong();
        this.lastRapidSnooze = parcel.readLong();
        if (parcel.readInt() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.metered = z;
        this.inferred = parcel.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.template, i);
        parcel.writeParcelable(this.cycleRule, i);
        parcel.writeLong(this.warningBytes);
        parcel.writeLong(this.limitBytes);
        parcel.writeLong(this.lastWarningSnooze);
        parcel.writeLong(this.lastLimitSnooze);
        parcel.writeLong(this.lastRapidSnooze);
        parcel.writeInt(this.metered ? 1 : 0);
        parcel.writeInt(this.inferred ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Iterator<Range<ZonedDateTime>> cycleIterator() {
        return this.cycleRule.cycleIterator();
    }

    public boolean isOverWarning(long j) {
        return this.warningBytes != -1 && j >= this.warningBytes;
    }

    public boolean isOverLimit(long j) {
        return this.limitBytes != -1 && j + 3000 >= this.limitBytes;
    }

    public void clearSnooze() {
        this.lastWarningSnooze = -1L;
        this.lastLimitSnooze = -1L;
        this.lastRapidSnooze = -1L;
    }

    public boolean hasCycle() {
        return this.cycleRule.cycleIterator().hasNext();
    }

    @Override
    public int compareTo(NetworkPolicy networkPolicy) {
        if (networkPolicy == null || networkPolicy.limitBytes == -1) {
            return -1;
        }
        if (this.limitBytes == -1 || networkPolicy.limitBytes < this.limitBytes) {
            return 1;
        }
        return 0;
    }

    public int hashCode() {
        return Objects.hash(this.template, this.cycleRule, Long.valueOf(this.warningBytes), Long.valueOf(this.limitBytes), Long.valueOf(this.lastWarningSnooze), Long.valueOf(this.lastLimitSnooze), Long.valueOf(this.lastRapidSnooze), Boolean.valueOf(this.metered), Boolean.valueOf(this.inferred));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkPolicy)) {
            return false;
        }
        NetworkPolicy networkPolicy = (NetworkPolicy) obj;
        return this.warningBytes == networkPolicy.warningBytes && this.limitBytes == networkPolicy.limitBytes && this.lastWarningSnooze == networkPolicy.lastWarningSnooze && this.lastLimitSnooze == networkPolicy.lastLimitSnooze && this.lastRapidSnooze == networkPolicy.lastRapidSnooze && this.metered == networkPolicy.metered && this.inferred == networkPolicy.inferred && Objects.equals(this.template, networkPolicy.template) && Objects.equals(this.cycleRule, networkPolicy.cycleRule);
    }

    public String toString() {
        return "NetworkPolicy{template=" + this.template + " cycleRule=" + this.cycleRule + " warningBytes=" + this.warningBytes + " limitBytes=" + this.limitBytes + " lastWarningSnooze=" + this.lastWarningSnooze + " lastLimitSnooze=" + this.lastLimitSnooze + " lastRapidSnooze=" + this.lastRapidSnooze + " metered=" + this.metered + " inferred=" + this.inferred + "}";
    }

    public byte[] getBytesForBackup() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeInt(3);
        dataOutputStream.write(this.template.getBytesForBackup());
        this.cycleRule.writeToStream(dataOutputStream);
        dataOutputStream.writeLong(this.warningBytes);
        dataOutputStream.writeLong(this.limitBytes);
        dataOutputStream.writeLong(this.lastWarningSnooze);
        dataOutputStream.writeLong(this.lastLimitSnooze);
        dataOutputStream.writeLong(this.lastRapidSnooze);
        dataOutputStream.writeInt(this.metered ? 1 : 0);
        dataOutputStream.writeInt(this.inferred ? 1 : 0);
        return byteArrayOutputStream.toByteArray();
    }

    public static NetworkPolicy getNetworkPolicyFromBackup(DataInputStream dataInputStream) throws BackupUtils.BadVersionException, IOException {
        RecurrenceRule recurrenceRuleBuildRule;
        long j;
        int i = dataInputStream.readInt();
        if (i < 1 || i > 3) {
            throw new BackupUtils.BadVersionException("Unknown backup version: " + i);
        }
        NetworkTemplate networkTemplateFromBackup = NetworkTemplate.getNetworkTemplateFromBackup(dataInputStream);
        if (i >= 2) {
            recurrenceRuleBuildRule = new RecurrenceRule(dataInputStream);
        } else {
            recurrenceRuleBuildRule = buildRule(dataInputStream.readInt(), ZoneId.of(BackupUtils.readString(dataInputStream)));
        }
        RecurrenceRule recurrenceRule = recurrenceRuleBuildRule;
        long j2 = dataInputStream.readLong();
        long j3 = dataInputStream.readLong();
        long j4 = dataInputStream.readLong();
        long j5 = dataInputStream.readLong();
        if (i >= 3) {
            j = dataInputStream.readLong();
        } else {
            j = -1;
        }
        return new NetworkPolicy(networkTemplateFromBackup, recurrenceRule, j2, j3, j4, j5, j, dataInputStream.readInt() == 1, dataInputStream.readInt() == 1);
    }
}
