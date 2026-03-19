package android.telecom;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telecom.IVideoProvider;
import java.util.ArrayList;
import java.util.List;

public final class ParcelableConference implements Parcelable {
    public static final Parcelable.Creator<ParcelableConference> CREATOR = new Parcelable.Creator<ParcelableConference>() {
        @Override
        public ParcelableConference createFromParcel(Parcel parcel) {
            ClassLoader classLoader = ParcelableConference.class.getClassLoader();
            PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) parcel.readParcelable(classLoader);
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            ArrayList arrayList = new ArrayList(2);
            parcel.readList(arrayList, classLoader);
            long j = parcel.readLong();
            return new ParcelableConference(phoneAccountHandle, i, i2, parcel.readInt(), arrayList, IVideoProvider.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), j, parcel.readLong(), (StatusHints) parcel.readParcelable(classLoader), parcel.readBundle(classLoader));
        }

        @Override
        public ParcelableConference[] newArray(int i) {
            return new ParcelableConference[i];
        }
    };
    private long mConnectElapsedTimeMillis;
    private long mConnectTimeMillis;
    private int mConnectionCapabilities;
    private List<String> mConnectionIds;
    private int mConnectionProperties;
    private Bundle mExtras;
    private PhoneAccountHandle mPhoneAccount;
    private int mState;
    private StatusHints mStatusHints;
    private final IVideoProvider mVideoProvider;
    private final int mVideoState;

    public ParcelableConference(PhoneAccountHandle phoneAccountHandle, int i, int i2, int i3, List<String> list, IVideoProvider iVideoProvider, int i4, long j, long j2, StatusHints statusHints, Bundle bundle) {
        this.mConnectTimeMillis = 0L;
        this.mConnectElapsedTimeMillis = 0L;
        this.mPhoneAccount = phoneAccountHandle;
        this.mState = i;
        this.mConnectionCapabilities = i2;
        this.mConnectionProperties = i3;
        this.mConnectionIds = list;
        this.mVideoProvider = iVideoProvider;
        this.mVideoState = i4;
        this.mConnectTimeMillis = j;
        this.mStatusHints = statusHints;
        this.mExtras = bundle;
        this.mConnectElapsedTimeMillis = j2;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("account: ");
        stringBuffer.append(this.mPhoneAccount);
        stringBuffer.append(", state: ");
        stringBuffer.append(Connection.stateToString(this.mState));
        stringBuffer.append(", capabilities: ");
        stringBuffer.append(Connection.capabilitiesToString(this.mConnectionCapabilities));
        stringBuffer.append(", properties: ");
        stringBuffer.append(Connection.propertiesToString(this.mConnectionProperties));
        stringBuffer.append(", connectTime: ");
        stringBuffer.append(this.mConnectTimeMillis);
        stringBuffer.append(", children: ");
        stringBuffer.append(this.mConnectionIds);
        stringBuffer.append(", VideoState: ");
        stringBuffer.append(this.mVideoState);
        stringBuffer.append(", VideoProvider: ");
        stringBuffer.append(this.mVideoProvider);
        return stringBuffer.toString();
    }

    public PhoneAccountHandle getPhoneAccount() {
        return this.mPhoneAccount;
    }

    public int getState() {
        return this.mState;
    }

    public int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    public int getConnectionProperties() {
        return this.mConnectionProperties;
    }

    public List<String> getConnectionIds() {
        return this.mConnectionIds;
    }

    public long getConnectTimeMillis() {
        return this.mConnectTimeMillis;
    }

    public long getConnectElapsedTimeMillis() {
        return this.mConnectElapsedTimeMillis;
    }

    public IVideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mPhoneAccount, 0);
        parcel.writeInt(this.mState);
        parcel.writeInt(this.mConnectionCapabilities);
        parcel.writeList(this.mConnectionIds);
        parcel.writeLong(this.mConnectTimeMillis);
        parcel.writeStrongBinder(this.mVideoProvider != null ? this.mVideoProvider.asBinder() : null);
        parcel.writeInt(this.mVideoState);
        parcel.writeParcelable(this.mStatusHints, 0);
        parcel.writeBundle(this.mExtras);
        parcel.writeInt(this.mConnectionProperties);
        parcel.writeLong(this.mConnectElapsedTimeMillis);
    }
}
