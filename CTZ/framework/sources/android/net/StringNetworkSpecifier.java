package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import java.util.Objects;

public final class StringNetworkSpecifier extends NetworkSpecifier implements Parcelable {
    public static final Parcelable.Creator<StringNetworkSpecifier> CREATOR = new Parcelable.Creator<StringNetworkSpecifier>() {
        @Override
        public StringNetworkSpecifier createFromParcel(Parcel parcel) {
            return new StringNetworkSpecifier(parcel.readString());
        }

        @Override
        public StringNetworkSpecifier[] newArray(int i) {
            return new StringNetworkSpecifier[i];
        }
    };
    public final String specifier;

    public StringNetworkSpecifier(String str) {
        Preconditions.checkStringNotEmpty(str);
        this.specifier = str;
    }

    @Override
    public boolean satisfiedBy(NetworkSpecifier networkSpecifier) {
        return equals(networkSpecifier);
    }

    public boolean equals(Object obj) {
        if (obj instanceof StringNetworkSpecifier) {
            return TextUtils.equals(this.specifier, ((StringNetworkSpecifier) obj).specifier);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hashCode(this.specifier);
    }

    public String toString() {
        return this.specifier;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.specifier);
    }
}
