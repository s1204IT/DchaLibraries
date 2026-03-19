package android.net.lowpan;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class LowpanProvision implements Parcelable {
    public static final Parcelable.Creator<LowpanProvision> CREATOR = new Parcelable.Creator<LowpanProvision>() {
        @Override
        public LowpanProvision createFromParcel(Parcel parcel) {
            Builder builder = new Builder();
            builder.setLowpanIdentity(LowpanIdentity.CREATOR.createFromParcel(parcel));
            if (parcel.readBoolean()) {
                builder.setLowpanCredential(LowpanCredential.CREATOR.createFromParcel(parcel));
            }
            return builder.build();
        }

        @Override
        public LowpanProvision[] newArray(int i) {
            return new LowpanProvision[i];
        }
    };
    private LowpanCredential mCredential;
    private LowpanIdentity mIdentity;

    public static class Builder {
        private final LowpanProvision provision = new LowpanProvision();

        public Builder setLowpanIdentity(LowpanIdentity lowpanIdentity) {
            this.provision.mIdentity = lowpanIdentity;
            return this;
        }

        public Builder setLowpanCredential(LowpanCredential lowpanCredential) {
            this.provision.mCredential = lowpanCredential;
            return this;
        }

        public LowpanProvision build() {
            return this.provision;
        }
    }

    private LowpanProvision() {
        this.mIdentity = new LowpanIdentity();
        this.mCredential = null;
    }

    public LowpanIdentity getLowpanIdentity() {
        return this.mIdentity;
    }

    public LowpanCredential getLowpanCredential() {
        return this.mCredential;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("LowpanProvision { identity => ");
        stringBuffer.append(this.mIdentity.toString());
        if (this.mCredential != null) {
            stringBuffer.append(", credential => ");
            stringBuffer.append(this.mCredential.toString());
        }
        stringBuffer.append("}");
        return stringBuffer.toString();
    }

    public int hashCode() {
        return Objects.hash(this.mIdentity, this.mCredential);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LowpanProvision)) {
            return false;
        }
        LowpanProvision lowpanProvision = (LowpanProvision) obj;
        return this.mIdentity.equals(lowpanProvision.mIdentity) && Objects.equals(this.mCredential, lowpanProvision.mCredential);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mIdentity.writeToParcel(parcel, i);
        if (this.mCredential == null) {
            parcel.writeBoolean(false);
        } else {
            parcel.writeBoolean(true);
            this.mCredential.writeToParcel(parcel, i);
        }
    }
}
