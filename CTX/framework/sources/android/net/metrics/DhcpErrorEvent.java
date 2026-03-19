package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;

public final class DhcpErrorEvent implements Parcelable {
    public static final int DHCP_ERROR = 4;
    public static final int L2_ERROR = 1;
    public static final int L3_ERROR = 2;
    public static final int L4_ERROR = 3;
    public static final int MISC_ERROR = 5;
    public final int errorCode;
    public static final int L2_TOO_SHORT = makeErrorCode(1, 1);
    public static final int L2_WRONG_ETH_TYPE = makeErrorCode(1, 2);
    public static final int L3_TOO_SHORT = makeErrorCode(2, 1);
    public static final int L3_NOT_IPV4 = makeErrorCode(2, 2);
    public static final int L3_INVALID_IP = makeErrorCode(2, 3);
    public static final int L4_NOT_UDP = makeErrorCode(3, 1);
    public static final int L4_WRONG_PORT = makeErrorCode(3, 2);
    public static final int BOOTP_TOO_SHORT = makeErrorCode(4, 1);
    public static final int DHCP_BAD_MAGIC_COOKIE = makeErrorCode(4, 2);
    public static final int DHCP_INVALID_OPTION_LENGTH = makeErrorCode(4, 3);
    public static final int DHCP_NO_MSG_TYPE = makeErrorCode(4, 4);
    public static final int DHCP_UNKNOWN_MSG_TYPE = makeErrorCode(4, 5);
    public static final int DHCP_NO_COOKIE = makeErrorCode(4, 6);
    public static final int BUFFER_UNDERFLOW = makeErrorCode(5, 1);
    public static final int RECEIVE_ERROR = makeErrorCode(5, 2);
    public static final int PARSING_ERROR = makeErrorCode(5, 3);
    public static final Parcelable.Creator<DhcpErrorEvent> CREATOR = new Parcelable.Creator<DhcpErrorEvent>() {
        @Override
        public DhcpErrorEvent createFromParcel(Parcel parcel) {
            return new DhcpErrorEvent(parcel);
        }

        @Override
        public DhcpErrorEvent[] newArray(int i) {
            return new DhcpErrorEvent[i];
        }
    };

    public DhcpErrorEvent(int i) {
        this.errorCode = i;
    }

    private DhcpErrorEvent(Parcel parcel) {
        this.errorCode = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.errorCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static int errorCodeWithOption(int i, int i2) {
        return (i & (-65536)) | (i2 & 255);
    }

    private static int makeErrorCode(int i, int i2) {
        return (i << 24) | ((i2 & 255) << 16);
    }

    public String toString() {
        return String.format("DhcpErrorEvent(%s)", Decoder.constants.get(this.errorCode));
    }

    static final class Decoder {
        static final SparseArray<String> constants = MessageUtils.findMessageNames(new Class[]{DhcpErrorEvent.class}, new String[]{"L2_", "L3_", "L4_", "BOOTP_", "DHCP_", "BUFFER_", "RECEIVE_", "PARSING_"});

        Decoder() {
        }
    }
}
