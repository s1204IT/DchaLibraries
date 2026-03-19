package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class UssdResponse implements Parcelable {
    public static final Parcelable.Creator<UssdResponse> CREATOR = new Parcelable.Creator<UssdResponse>() {
        @Override
        public UssdResponse createFromParcel(Parcel parcel) {
            return new UssdResponse(parcel.readString(), TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel));
        }

        @Override
        public UssdResponse[] newArray(int i) {
            return new UssdResponse[i];
        }
    };
    private CharSequence mReturnMessage;
    private String mUssdRequest;

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mUssdRequest);
        TextUtils.writeToParcel(this.mReturnMessage, parcel, 0);
    }

    public String getUssdRequest() {
        return this.mUssdRequest;
    }

    public CharSequence getReturnMessage() {
        return this.mReturnMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public UssdResponse(String str, CharSequence charSequence) {
        this.mUssdRequest = str;
        this.mReturnMessage = charSequence;
    }
}
