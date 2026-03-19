package com.android.internal.content;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.logging.nano.MetricsProto;
import java.util.Objects;

public class ReferrerIntent extends Intent {
    public static final Parcelable.Creator<ReferrerIntent> CREATOR = new Parcelable.Creator<ReferrerIntent>() {
        @Override
        public ReferrerIntent createFromParcel(Parcel parcel) {
            return new ReferrerIntent(parcel);
        }

        @Override
        public ReferrerIntent[] newArray(int i) {
            return new ReferrerIntent[i];
        }
    };
    public final String mReferrer;

    public ReferrerIntent(Intent intent, String str) {
        super(intent);
        this.mReferrer = str;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(this.mReferrer);
    }

    ReferrerIntent(Parcel parcel) {
        readFromParcel(parcel);
        this.mReferrer = parcel.readString();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ReferrerIntent)) {
            return false;
        }
        ReferrerIntent referrerIntent = (ReferrerIntent) obj;
        return filterEquals(referrerIntent) && Objects.equals(this.mReferrer, referrerIntent.mReferrer);
    }

    public int hashCode() {
        return (31 * (MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + filterHashCode())) + Objects.hashCode(this.mReferrer);
    }
}
