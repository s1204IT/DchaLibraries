package android.net;

import android.bluetooth.BluetoothHidDevice;
import android.net.util.IpUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.system.OsConstants;
import android.util.Log;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class KeepalivePacketData implements Parcelable {
    public static final Parcelable.Creator<KeepalivePacketData> CREATOR = new Parcelable.Creator<KeepalivePacketData>() {
        @Override
        public KeepalivePacketData createFromParcel(Parcel parcel) {
            return new KeepalivePacketData(parcel);
        }

        @Override
        public KeepalivePacketData[] newArray(int i) {
            return new KeepalivePacketData[i];
        }
    };
    private static final int IPV4_HEADER_LENGTH = 20;
    private static final String TAG = "KeepalivePacketData";
    private static final int UDP_HEADER_LENGTH = 8;
    public final InetAddress dstAddress;
    public final int dstPort;
    private final byte[] mPacket;
    public final InetAddress srcAddress;
    public final int srcPort;

    protected KeepalivePacketData(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2, byte[] bArr) throws InvalidPacketException {
        this.srcAddress = inetAddress;
        this.dstAddress = inetAddress2;
        this.srcPort = i;
        this.dstPort = i2;
        this.mPacket = bArr;
        if (inetAddress == null || inetAddress2 == null || !inetAddress.getClass().getName().equals(inetAddress2.getClass().getName())) {
            Log.e(TAG, "Invalid or mismatched InetAddresses in KeepalivePacketData");
            throw new InvalidPacketException(-21);
        }
        if (!IpUtils.isValidUdpOrTcpPort(i) || !IpUtils.isValidUdpOrTcpPort(i2)) {
            Log.e(TAG, "Invalid ports in KeepalivePacketData");
            throw new InvalidPacketException(-22);
        }
    }

    public static class InvalidPacketException extends Exception {
        public final int error;

        public InvalidPacketException(int i) {
            this.error = i;
        }
    }

    public byte[] getPacket() {
        return (byte[]) this.mPacket.clone();
    }

    public static KeepalivePacketData nattKeepalivePacket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws InvalidPacketException {
        if (!(inetAddress instanceof Inet4Address) || !(inetAddress2 instanceof Inet4Address)) {
            throw new InvalidPacketException(-21);
        }
        if (i2 != 4500) {
            throw new InvalidPacketException(-22);
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(29);
        byteBufferAllocate.order(ByteOrder.BIG_ENDIAN);
        byteBufferAllocate.putShort((short) 17664);
        byteBufferAllocate.putShort((short) 29);
        byteBufferAllocate.putInt(0);
        byteBufferAllocate.put(BluetoothHidDevice.SUBCLASS1_KEYBOARD);
        byteBufferAllocate.put((byte) OsConstants.IPPROTO_UDP);
        int iPosition = byteBufferAllocate.position();
        byteBufferAllocate.putShort((short) 0);
        byteBufferAllocate.put(inetAddress.getAddress());
        byteBufferAllocate.put(inetAddress2.getAddress());
        byteBufferAllocate.putShort((short) i);
        byteBufferAllocate.putShort((short) i2);
        byteBufferAllocate.putShort((short) 9);
        int iPosition2 = byteBufferAllocate.position();
        byteBufferAllocate.putShort((short) 0);
        byteBufferAllocate.put((byte) -1);
        byteBufferAllocate.putShort(iPosition, IpUtils.ipChecksum(byteBufferAllocate, 0));
        byteBufferAllocate.putShort(iPosition2, IpUtils.udpChecksum(byteBufferAllocate, 0, 20));
        return new KeepalivePacketData(inetAddress, i, inetAddress2, i2, byteBufferAllocate.array());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.srcAddress.getHostAddress());
        parcel.writeString(this.dstAddress.getHostAddress());
        parcel.writeInt(this.srcPort);
        parcel.writeInt(this.dstPort);
        parcel.writeByteArray(this.mPacket);
    }

    private KeepalivePacketData(Parcel parcel) {
        this.srcAddress = NetworkUtils.numericToInetAddress(parcel.readString());
        this.dstAddress = NetworkUtils.numericToInetAddress(parcel.readString());
        this.srcPort = parcel.readInt();
        this.dstPort = parcel.readInt();
        this.mPacket = parcel.createByteArray();
    }
}
