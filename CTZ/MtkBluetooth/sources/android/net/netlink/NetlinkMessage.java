package android.net.netlink;

import java.nio.ByteBuffer;

public class NetlinkMessage {
    private static final String TAG = "NetlinkMessage";
    protected StructNlMsgHdr mHeader;

    public static NetlinkMessage parse(ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            byteBuffer.position();
        }
        StructNlMsgHdr structNlMsgHdr = StructNlMsgHdr.parse(byteBuffer);
        if (structNlMsgHdr == null) {
            return null;
        }
        int iAlignedLengthOf = NetlinkConstants.alignedLengthOf(structNlMsgHdr.nlmsg_len) - 16;
        if (iAlignedLengthOf < 0 || iAlignedLengthOf > byteBuffer.remaining()) {
            byteBuffer.position(byteBuffer.limit());
            return null;
        }
        short s = structNlMsgHdr.nlmsg_type;
        switch (s) {
            case 2:
                return NetlinkErrorMessage.parse(structNlMsgHdr, byteBuffer);
            case 3:
                byteBuffer.position(byteBuffer.position() + iAlignedLengthOf);
                return new NetlinkMessage(structNlMsgHdr);
            default:
                switch (s) {
                    case 28:
                    case 29:
                    case 30:
                        return RtNetlinkNeighborMessage.parse(structNlMsgHdr, byteBuffer);
                    default:
                        if (structNlMsgHdr.nlmsg_type > 15) {
                            return null;
                        }
                        byteBuffer.position(byteBuffer.position() + iAlignedLengthOf);
                        return new NetlinkMessage(structNlMsgHdr);
                }
        }
    }

    public NetlinkMessage(StructNlMsgHdr structNlMsgHdr) {
        this.mHeader = structNlMsgHdr;
    }

    public StructNlMsgHdr getHeader() {
        return this.mHeader;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NetlinkMessage{");
        sb.append(this.mHeader == null ? "" : this.mHeader.toString());
        sb.append("}");
        return sb.toString();
    }
}
