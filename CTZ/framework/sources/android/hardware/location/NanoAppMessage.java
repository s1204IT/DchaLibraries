package android.hardware.location;

import android.annotation.SystemApi;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.Logging.Session;

@SystemApi
public final class NanoAppMessage implements Parcelable {
    public static final Parcelable.Creator<NanoAppMessage> CREATOR = new Parcelable.Creator<NanoAppMessage>() {
        @Override
        public NanoAppMessage createFromParcel(Parcel parcel) {
            return new NanoAppMessage(parcel);
        }

        @Override
        public NanoAppMessage[] newArray(int i) {
            return new NanoAppMessage[i];
        }
    };
    private static final int DEBUG_LOG_NUM_BYTES = 16;
    private boolean mIsBroadcasted;
    private byte[] mMessageBody;
    private int mMessageType;
    private long mNanoAppId;

    private NanoAppMessage(long j, int i, byte[] bArr, boolean z) {
        this.mNanoAppId = j;
        this.mMessageType = i;
        this.mMessageBody = bArr;
        this.mIsBroadcasted = z;
    }

    public static NanoAppMessage createMessageToNanoApp(long j, int i, byte[] bArr) {
        return new NanoAppMessage(j, i, bArr, false);
    }

    public static NanoAppMessage createMessageFromNanoApp(long j, int i, byte[] bArr, boolean z) {
        return new NanoAppMessage(j, i, bArr, z);
    }

    public long getNanoAppId() {
        return this.mNanoAppId;
    }

    public int getMessageType() {
        return this.mMessageType;
    }

    public byte[] getMessageBody() {
        return this.mMessageBody;
    }

    public boolean isBroadcastMessage() {
        return this.mIsBroadcasted;
    }

    private NanoAppMessage(Parcel parcel) {
        this.mNanoAppId = parcel.readLong();
        this.mIsBroadcasted = parcel.readInt() == 1;
        this.mMessageType = parcel.readInt();
        this.mMessageBody = new byte[parcel.readInt()];
        parcel.readByteArray(this.mMessageBody);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mNanoAppId);
        parcel.writeInt(this.mIsBroadcasted ? 1 : 0);
        parcel.writeInt(this.mMessageType);
        parcel.writeInt(this.mMessageBody.length);
        parcel.writeByteArray(this.mMessageBody);
    }

    public String toString() {
        int length = this.mMessageBody.length;
        StringBuilder sb = new StringBuilder();
        sb.append("NanoAppMessage[type = ");
        sb.append(this.mMessageType);
        sb.append(", length = ");
        sb.append(this.mMessageBody.length);
        sb.append(" bytes, ");
        sb.append(this.mIsBroadcasted ? "broadcast" : "unicast");
        sb.append(", nanoapp = 0x");
        sb.append(Long.toHexString(this.mNanoAppId));
        sb.append("](");
        String string = sb.toString();
        if (length > 0) {
            string = string + "data = 0x";
        }
        int i = 0;
        while (i < Math.min(length, 16)) {
            string = string + Byte.toHexString(this.mMessageBody[i], true);
            i++;
            if (i % 4 == 0) {
                string = string + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
        }
        if (length > 16) {
            string = string + Session.TRUNCATE_STRING;
        }
        return string + ")";
    }
}
