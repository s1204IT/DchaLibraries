package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.BitSet;

public final class ApfProgramEvent implements Parcelable {
    public static final Parcelable.Creator<ApfProgramEvent> CREATOR = new Parcelable.Creator<ApfProgramEvent>() {
        @Override
        public ApfProgramEvent createFromParcel(Parcel parcel) {
            return new ApfProgramEvent(parcel);
        }

        @Override
        public ApfProgramEvent[] newArray(int i) {
            return new ApfProgramEvent[i];
        }
    };
    public static final int FLAG_HAS_IPV4_ADDRESS = 1;
    public static final int FLAG_MULTICAST_FILTER_ON = 0;
    public long actualLifetime;
    public int currentRas;
    public int filteredRas;
    public int flags;
    public long lifetime;
    public int programLength;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    public ApfProgramEvent() {
    }

    private ApfProgramEvent(Parcel parcel) {
        this.lifetime = parcel.readLong();
        this.actualLifetime = parcel.readLong();
        this.filteredRas = parcel.readInt();
        this.currentRas = parcel.readInt();
        this.programLength = parcel.readInt();
        this.flags = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.lifetime);
        parcel.writeLong(this.actualLifetime);
        parcel.writeInt(this.filteredRas);
        parcel.writeInt(this.currentRas);
        parcel.writeInt(this.programLength);
        parcel.writeInt(i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        String str;
        if (this.lifetime < Long.MAX_VALUE) {
            str = this.lifetime + "s";
        } else {
            str = "forever";
        }
        return String.format("ApfProgramEvent(%d/%d RAs %dB %ds/%s %s)", Integer.valueOf(this.filteredRas), Integer.valueOf(this.currentRas), Integer.valueOf(this.programLength), Long.valueOf(this.actualLifetime), str, namesOf(this.flags));
    }

    public static int flagsFor(boolean z, boolean z2) {
        int i;
        if (z) {
            i = 2;
        } else {
            i = 0;
        }
        if (z2) {
            return i | 1;
        }
        return i;
    }

    private static String namesOf(int i) {
        ArrayList arrayList = new ArrayList(Integer.bitCount(i));
        long j = i & Integer.MAX_VALUE;
        int i2 = 0;
        BitSet bitSetValueOf = BitSet.valueOf(new long[]{j});
        while (true) {
            int iNextSetBit = bitSetValueOf.nextSetBit(i2);
            if (iNextSetBit >= 0) {
                arrayList.add(Decoder.constants.get(iNextSetBit));
                i2 = iNextSetBit + 1;
            } else {
                return TextUtils.join("|", arrayList);
            }
        }
    }

    static final class Decoder {
        static final SparseArray<String> constants = MessageUtils.findMessageNames(new Class[]{ApfProgramEvent.class}, new String[]{"FLAG_"});

        Decoder() {
        }
    }
}
