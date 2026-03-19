package android.telephony.ims;

import android.annotation.SystemApi;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.Log;
import android.telephony.Rlog;

@SystemApi
public final class ImsExternalCallState implements Parcelable {
    public static final int CALL_STATE_CONFIRMED = 1;
    public static final int CALL_STATE_TERMINATED = 2;
    public static final Parcelable.Creator<ImsExternalCallState> CREATOR = new Parcelable.Creator<ImsExternalCallState>() {
        @Override
        public ImsExternalCallState createFromParcel(Parcel parcel) {
            return new ImsExternalCallState(parcel);
        }

        @Override
        public ImsExternalCallState[] newArray(int i) {
            return new ImsExternalCallState[i];
        }
    };
    private static final String TAG = "ImsExternalCallState";
    private Uri mAddress;
    private int mCallId;
    private int mCallState;
    private int mCallType;
    private boolean mIsHeld;
    private boolean mIsPullable;

    public ImsExternalCallState() {
    }

    public ImsExternalCallState(int i, Uri uri, boolean z, int i2, int i3, boolean z2) {
        this.mCallId = i;
        this.mAddress = uri;
        this.mIsPullable = z;
        this.mCallState = i2;
        this.mCallType = i3;
        this.mIsHeld = z2;
        Rlog.d(TAG, "ImsExternalCallState = " + this);
    }

    public ImsExternalCallState(Parcel parcel) {
        this.mCallId = parcel.readInt();
        this.mAddress = (Uri) parcel.readParcelable(ImsExternalCallState.class.getClassLoader());
        this.mIsPullable = parcel.readInt() != 0;
        this.mCallState = parcel.readInt();
        this.mCallType = parcel.readInt();
        this.mIsHeld = parcel.readInt() != 0;
        Rlog.d(TAG, "ImsExternalCallState const = " + this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mCallId);
        parcel.writeParcelable(this.mAddress, 0);
        parcel.writeInt(this.mIsPullable ? 1 : 0);
        parcel.writeInt(this.mCallState);
        parcel.writeInt(this.mCallType);
        parcel.writeInt(this.mIsHeld ? 1 : 0);
        Rlog.d(TAG, "ImsExternalCallState writeToParcel = " + parcel.toString());
    }

    public int getCallId() {
        return this.mCallId;
    }

    public Uri getAddress() {
        return this.mAddress;
    }

    public boolean isCallPullable() {
        return this.mIsPullable;
    }

    public int getCallState() {
        return this.mCallState;
    }

    public int getCallType() {
        return this.mCallType;
    }

    public boolean isCallHeld() {
        return this.mIsHeld;
    }

    public String toString() {
        return "ImsExternalCallState { mCallId = " + this.mCallId + ", mAddress = " + Log.pii(this.mAddress) + ", mIsPullable = " + this.mIsPullable + ", mCallState = " + this.mCallState + ", mCallType = " + this.mCallType + ", mIsHeld = " + this.mIsHeld + "}";
    }
}
