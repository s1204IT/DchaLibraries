package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Collections;
import java.util.List;

public final class VisualVoicemailSmsFilterSettings implements Parcelable {
    public static final String DEFAULT_CLIENT_PREFIX = "//VVM";
    public static final int DEFAULT_DESTINATION_PORT = -1;
    public static final int DESTINATION_PORT_ANY = -1;
    public static final int DESTINATION_PORT_DATA_SMS = -2;
    public final String clientPrefix;
    public final int destinationPort;
    public final List<String> originatingNumbers;
    public final String packageName;
    public static final List<String> DEFAULT_ORIGINATING_NUMBERS = Collections.emptyList();
    public static final Parcelable.Creator<VisualVoicemailSmsFilterSettings> CREATOR = new Parcelable.Creator<VisualVoicemailSmsFilterSettings>() {
        @Override
        public VisualVoicemailSmsFilterSettings createFromParcel(Parcel parcel) {
            Builder builder = new Builder();
            builder.setClientPrefix(parcel.readString());
            builder.setOriginatingNumbers(parcel.createStringArrayList());
            builder.setDestinationPort(parcel.readInt());
            builder.setPackageName(parcel.readString());
            return builder.build();
        }

        @Override
        public VisualVoicemailSmsFilterSettings[] newArray(int i) {
            return new VisualVoicemailSmsFilterSettings[i];
        }
    };

    public static class Builder {
        private String mPackageName;
        private String mClientPrefix = VisualVoicemailSmsFilterSettings.DEFAULT_CLIENT_PREFIX;
        private List<String> mOriginatingNumbers = VisualVoicemailSmsFilterSettings.DEFAULT_ORIGINATING_NUMBERS;
        private int mDestinationPort = -1;

        public VisualVoicemailSmsFilterSettings build() {
            return new VisualVoicemailSmsFilterSettings(this);
        }

        public Builder setClientPrefix(String str) {
            if (str == null) {
                throw new IllegalArgumentException("Client prefix cannot be null");
            }
            this.mClientPrefix = str;
            return this;
        }

        public Builder setOriginatingNumbers(List<String> list) {
            if (list == null) {
                throw new IllegalArgumentException("Originating numbers cannot be null");
            }
            this.mOriginatingNumbers = list;
            return this;
        }

        public Builder setDestinationPort(int i) {
            this.mDestinationPort = i;
            return this;
        }

        public Builder setPackageName(String str) {
            this.mPackageName = str;
            return this;
        }
    }

    private VisualVoicemailSmsFilterSettings(Builder builder) {
        this.clientPrefix = builder.mClientPrefix;
        this.originatingNumbers = builder.mOriginatingNumbers;
        this.destinationPort = builder.mDestinationPort;
        this.packageName = builder.mPackageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.clientPrefix);
        parcel.writeStringList(this.originatingNumbers);
        parcel.writeInt(this.destinationPort);
        parcel.writeString(this.packageName);
    }

    public String toString() {
        return "[VisualVoicemailSmsFilterSettings clientPrefix=" + this.clientPrefix + ", originatingNumbers=" + this.originatingNumbers + ", destinationPort=" + this.destinationPort + "]";
    }
}
