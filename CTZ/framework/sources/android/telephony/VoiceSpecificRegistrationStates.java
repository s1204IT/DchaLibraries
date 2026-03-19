package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class VoiceSpecificRegistrationStates implements Parcelable {
    public static final Parcelable.Creator<VoiceSpecificRegistrationStates> CREATOR = new Parcelable.Creator<VoiceSpecificRegistrationStates>() {
        @Override
        public VoiceSpecificRegistrationStates createFromParcel(Parcel parcel) {
            return new VoiceSpecificRegistrationStates(parcel);
        }

        @Override
        public VoiceSpecificRegistrationStates[] newArray(int i) {
            return new VoiceSpecificRegistrationStates[i];
        }
    };
    public final boolean cssSupported;
    public final int defaultRoamingIndicator;
    public final int roamingIndicator;
    public final int systemIsInPrl;

    VoiceSpecificRegistrationStates(boolean z, int i, int i2, int i3) {
        this.cssSupported = z;
        this.roamingIndicator = i;
        this.systemIsInPrl = i2;
        this.defaultRoamingIndicator = i3;
    }

    private VoiceSpecificRegistrationStates(Parcel parcel) {
        this.cssSupported = parcel.readBoolean();
        this.roamingIndicator = parcel.readInt();
        this.systemIsInPrl = parcel.readInt();
        this.defaultRoamingIndicator = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeBoolean(this.cssSupported);
        parcel.writeInt(this.roamingIndicator);
        parcel.writeInt(this.systemIsInPrl);
        parcel.writeInt(this.defaultRoamingIndicator);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "VoiceSpecificRegistrationStates { mCssSupported=" + this.cssSupported + " mRoamingIndicator=" + this.roamingIndicator + " mSystemIsInPrl=" + this.systemIsInPrl + " mDefaultRoamingIndicator=" + this.defaultRoamingIndicator + "}";
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.cssSupported), Integer.valueOf(this.roamingIndicator), Integer.valueOf(this.systemIsInPrl), Integer.valueOf(this.defaultRoamingIndicator));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof VoiceSpecificRegistrationStates)) {
            return false;
        }
        VoiceSpecificRegistrationStates voiceSpecificRegistrationStates = (VoiceSpecificRegistrationStates) obj;
        if (this.cssSupported == voiceSpecificRegistrationStates.cssSupported && this.roamingIndicator == voiceSpecificRegistrationStates.roamingIndicator && this.systemIsInPrl == voiceSpecificRegistrationStates.systemIsInPrl && this.defaultRoamingIndicator == voiceSpecificRegistrationStates.defaultRoamingIndicator) {
            return true;
        }
        return false;
    }
}
