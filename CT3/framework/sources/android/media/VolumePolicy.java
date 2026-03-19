package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class VolumePolicy implements Parcelable {
    public final boolean doNotDisturbWhenSilent;
    public final int vibrateToSilentDebounce;
    public final boolean volumeDownToEnterSilent;
    public final boolean volumeUpToExitSilent;
    public static final VolumePolicy DEFAULT = new VolumePolicy(false, false, true, 400);
    public static final Parcelable.Creator<VolumePolicy> CREATOR = new Parcelable.Creator<VolumePolicy>() {
        @Override
        public VolumePolicy createFromParcel(Parcel p) {
            return new VolumePolicy(p.readInt() != 0, p.readInt() != 0, p.readInt() != 0, p.readInt());
        }

        @Override
        public VolumePolicy[] newArray(int size) {
            return new VolumePolicy[size];
        }
    };

    public VolumePolicy(boolean volumeDownToEnterSilent, boolean volumeUpToExitSilent, boolean doNotDisturbWhenSilent, int vibrateToSilentDebounce) {
        this.volumeDownToEnterSilent = volumeDownToEnterSilent;
        this.volumeUpToExitSilent = volumeUpToExitSilent;
        this.doNotDisturbWhenSilent = doNotDisturbWhenSilent;
        this.vibrateToSilentDebounce = vibrateToSilentDebounce;
    }

    public String toString() {
        return "VolumePolicy[volumeDownToEnterSilent=" + this.volumeDownToEnterSilent + ",volumeUpToExitSilent=" + this.volumeUpToExitSilent + ",doNotDisturbWhenSilent=" + this.doNotDisturbWhenSilent + ",vibrateToSilentDebounce=" + this.vibrateToSilentDebounce + "]";
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.volumeDownToEnterSilent), Boolean.valueOf(this.volumeUpToExitSilent), Boolean.valueOf(this.doNotDisturbWhenSilent), Integer.valueOf(this.vibrateToSilentDebounce));
    }

    public boolean equals(Object o) {
        if (!(o instanceof VolumePolicy)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        VolumePolicy other = (VolumePolicy) o;
        if (other.volumeDownToEnterSilent == this.volumeDownToEnterSilent && other.volumeUpToExitSilent == this.volumeUpToExitSilent && other.doNotDisturbWhenSilent == this.doNotDisturbWhenSilent) {
            return other.vibrateToSilentDebounce == this.vibrateToSilentDebounce;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.volumeDownToEnterSilent ? 1 : 0);
        dest.writeInt(this.volumeUpToExitSilent ? 1 : 0);
        dest.writeInt(this.doNotDisturbWhenSilent ? 1 : 0);
        dest.writeInt(this.vibrateToSilentDebounce);
    }
}
