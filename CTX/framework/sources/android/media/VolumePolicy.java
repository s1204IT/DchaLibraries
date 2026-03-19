package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class VolumePolicy implements Parcelable {
    public static final int A11Y_MODE_INDEPENDENT_A11Y_VOLUME = 1;
    public static final int A11Y_MODE_MEDIA_A11Y_VOLUME = 0;
    public final boolean doNotDisturbWhenSilent;
    public final int vibrateToSilentDebounce;
    public final boolean volumeDownToEnterSilent;
    public final boolean volumeUpToExitSilent;
    public static final VolumePolicy DEFAULT = new VolumePolicy(false, false, false, 400);
    public static final Parcelable.Creator<VolumePolicy> CREATOR = new Parcelable.Creator<VolumePolicy>() {
        @Override
        public VolumePolicy createFromParcel(Parcel parcel) {
            return new VolumePolicy(parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt());
        }

        @Override
        public VolumePolicy[] newArray(int i) {
            return new VolumePolicy[i];
        }
    };

    public VolumePolicy(boolean z, boolean z2, boolean z3, int i) {
        this.volumeDownToEnterSilent = z;
        this.volumeUpToExitSilent = z2;
        this.doNotDisturbWhenSilent = z3;
        this.vibrateToSilentDebounce = i;
    }

    public String toString() {
        return "VolumePolicy[volumeDownToEnterSilent=" + this.volumeDownToEnterSilent + ",volumeUpToExitSilent=" + this.volumeUpToExitSilent + ",doNotDisturbWhenSilent=" + this.doNotDisturbWhenSilent + ",vibrateToSilentDebounce=" + this.vibrateToSilentDebounce + "]";
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.volumeDownToEnterSilent), Boolean.valueOf(this.volumeUpToExitSilent), Boolean.valueOf(this.doNotDisturbWhenSilent), Integer.valueOf(this.vibrateToSilentDebounce));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof VolumePolicy)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        VolumePolicy volumePolicy = (VolumePolicy) obj;
        return volumePolicy.volumeDownToEnterSilent == this.volumeDownToEnterSilent && volumePolicy.volumeUpToExitSilent == this.volumeUpToExitSilent && volumePolicy.doNotDisturbWhenSilent == this.doNotDisturbWhenSilent && volumePolicy.vibrateToSilentDebounce == this.vibrateToSilentDebounce;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.volumeDownToEnterSilent ? 1 : 0);
        parcel.writeInt(this.volumeUpToExitSilent ? 1 : 0);
        parcel.writeInt(this.doNotDisturbWhenSilent ? 1 : 0);
        parcel.writeInt(this.vibrateToSilentDebounce);
    }
}
