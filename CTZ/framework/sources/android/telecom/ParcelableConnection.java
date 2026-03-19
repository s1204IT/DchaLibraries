package android.telecom;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telecom.IVideoProvider;
import java.util.ArrayList;
import java.util.List;

public final class ParcelableConnection implements Parcelable {
    public static final Parcelable.Creator<ParcelableConnection> CREATOR = new Parcelable.Creator<ParcelableConnection>() {
        @Override
        public ParcelableConnection createFromParcel(Parcel parcel) {
            ClassLoader classLoader = ParcelableConnection.class.getClassLoader();
            PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) parcel.readParcelable(classLoader);
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            Uri uri = (Uri) parcel.readParcelable(classLoader);
            int i3 = parcel.readInt();
            String string = parcel.readString();
            int i4 = parcel.readInt();
            IVideoProvider iVideoProviderAsInterface = IVideoProvider.Stub.asInterface(parcel.readStrongBinder());
            int i5 = parcel.readInt();
            boolean z = parcel.readByte() == 1;
            boolean z2 = parcel.readByte() == 1;
            long j = parcel.readLong();
            StatusHints statusHints = (StatusHints) parcel.readParcelable(classLoader);
            DisconnectCause disconnectCause = (DisconnectCause) parcel.readParcelable(classLoader);
            ArrayList arrayList = new ArrayList();
            parcel.readStringList(arrayList);
            return new ParcelableConnection(phoneAccountHandle, i, i2, parcel.readInt(), parcel.readInt(), uri, i3, string, i4, iVideoProviderAsInterface, i5, z, z2, j, parcel.readLong(), statusHints, disconnectCause, arrayList, Bundle.setDefusable(parcel.readBundle(classLoader), true), parcel.readString());
        }

        @Override
        public ParcelableConnection[] newArray(int i) {
            return new ParcelableConnection[i];
        }
    };
    private final Uri mAddress;
    private final int mAddressPresentation;
    private final String mCallerDisplayName;
    private final int mCallerDisplayNamePresentation;
    private final List<String> mConferenceableConnectionIds;
    private final long mConnectElapsedTimeMillis;
    private final long mConnectTimeMillis;
    private final int mConnectionCapabilities;
    private final int mConnectionProperties;
    private final DisconnectCause mDisconnectCause;
    private final Bundle mExtras;
    private final boolean mIsVoipAudioMode;
    private String mParentCallId;
    private final PhoneAccountHandle mPhoneAccount;
    private final boolean mRingbackRequested;
    private final int mState;
    private final StatusHints mStatusHints;
    private final int mSupportedAudioRoutes;
    private final IVideoProvider mVideoProvider;
    private final int mVideoState;

    public ParcelableConnection(PhoneAccountHandle phoneAccountHandle, int i, int i2, int i3, int i4, Uri uri, int i5, String str, int i6, IVideoProvider iVideoProvider, int i7, boolean z, boolean z2, long j, long j2, StatusHints statusHints, DisconnectCause disconnectCause, List<String> list, Bundle bundle, String str2) {
        this(phoneAccountHandle, i, i2, i3, i4, uri, i5, str, i6, iVideoProvider, i7, z, z2, j, j2, statusHints, disconnectCause, list, bundle);
        this.mParentCallId = str2;
    }

    public ParcelableConnection(PhoneAccountHandle phoneAccountHandle, int i, int i2, int i3, int i4, Uri uri, int i5, String str, int i6, IVideoProvider iVideoProvider, int i7, boolean z, boolean z2, long j, long j2, StatusHints statusHints, DisconnectCause disconnectCause, List<String> list, Bundle bundle) {
        this.mPhoneAccount = phoneAccountHandle;
        this.mState = i;
        this.mConnectionCapabilities = i2;
        this.mConnectionProperties = i3;
        this.mSupportedAudioRoutes = i4;
        this.mAddress = uri;
        this.mAddressPresentation = i5;
        this.mCallerDisplayName = str;
        this.mCallerDisplayNamePresentation = i6;
        this.mVideoProvider = iVideoProvider;
        this.mVideoState = i7;
        this.mRingbackRequested = z;
        this.mIsVoipAudioMode = z2;
        this.mConnectTimeMillis = j;
        this.mConnectElapsedTimeMillis = j2;
        this.mStatusHints = statusHints;
        this.mDisconnectCause = disconnectCause;
        this.mConferenceableConnectionIds = list;
        this.mExtras = bundle;
        this.mParentCallId = null;
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

    public int getSupportedAudioRoutes() {
        return this.mSupportedAudioRoutes;
    }

    public Uri getHandle() {
        return this.mAddress;
    }

    public int getHandlePresentation() {
        return this.mAddressPresentation;
    }

    public String getCallerDisplayName() {
        return this.mCallerDisplayName;
    }

    public int getCallerDisplayNamePresentation() {
        return this.mCallerDisplayNamePresentation;
    }

    public IVideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public boolean isRingbackRequested() {
        return this.mRingbackRequested;
    }

    public boolean getIsVoipAudioMode() {
        return this.mIsVoipAudioMode;
    }

    public long getConnectTimeMillis() {
        return this.mConnectTimeMillis;
    }

    public long getConnectElapsedTimeMillis() {
        return this.mConnectElapsedTimeMillis;
    }

    public final StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public final DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    public final List<String> getConferenceableConnectionIds() {
        return this.mConferenceableConnectionIds;
    }

    public final Bundle getExtras() {
        return this.mExtras;
    }

    public final String getParentCallId() {
        return this.mParentCallId;
    }

    public String toString() {
        return "ParcelableConnection [act:" + this.mPhoneAccount + "], state:" + this.mState + ", capabilities:" + Connection.capabilitiesToString(this.mConnectionCapabilities) + ", properties:" + Connection.propertiesToString(this.mConnectionProperties) + ", extras:" + this.mExtras + ", parent:" + this.mParentCallId;
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
        parcel.writeParcelable(this.mAddress, 0);
        parcel.writeInt(this.mAddressPresentation);
        parcel.writeString(this.mCallerDisplayName);
        parcel.writeInt(this.mCallerDisplayNamePresentation);
        parcel.writeStrongBinder(this.mVideoProvider != null ? this.mVideoProvider.asBinder() : null);
        parcel.writeInt(this.mVideoState);
        parcel.writeByte(this.mRingbackRequested ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.mIsVoipAudioMode ? (byte) 1 : (byte) 0);
        parcel.writeLong(this.mConnectTimeMillis);
        parcel.writeParcelable(this.mStatusHints, 0);
        parcel.writeParcelable(this.mDisconnectCause, 0);
        parcel.writeStringList(this.mConferenceableConnectionIds);
        parcel.writeBundle(this.mExtras);
        parcel.writeInt(this.mConnectionProperties);
        parcel.writeInt(this.mSupportedAudioRoutes);
        parcel.writeString(this.mParentCallId);
        parcel.writeLong(this.mConnectElapsedTimeMillis);
    }
}
