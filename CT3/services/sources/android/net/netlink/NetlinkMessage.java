package android.net.netlink;

import com.android.server.wm.WindowManagerService;
import java.nio.ByteBuffer;

public class NetlinkMessage {
    private static final String TAG = "NetlinkMessage";
    protected StructNlMsgHdr mHeader;

    public static NetlinkMessage parse(ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            byteBuffer.position();
        }
        StructNlMsgHdr nlmsghdr = StructNlMsgHdr.parse(byteBuffer);
        if (nlmsghdr == null) {
            return null;
        }
        int payloadLength = NetlinkConstants.alignedLengthOf(nlmsghdr.nlmsg_len) - 16;
        if (payloadLength < 0 || payloadLength > byteBuffer.remaining()) {
            byteBuffer.position(byteBuffer.limit());
            return null;
        }
        switch (nlmsghdr.nlmsg_type) {
            case 2:
                break;
            case 3:
                byteBuffer.position(byteBuffer.position() + payloadLength);
                break;
            case WindowManagerService.H.DO_DISPLAY_REMOVED:
            case 29:
            case 30:
                break;
            default:
                if (nlmsghdr.nlmsg_type <= 15) {
                    byteBuffer.position(byteBuffer.position() + payloadLength);
                }
                break;
        }
        return null;
    }

    public NetlinkMessage(StructNlMsgHdr nlmsghdr) {
        this.mHeader = nlmsghdr;
    }

    public StructNlMsgHdr getHeader() {
        return this.mHeader;
    }

    public String toString() {
        return "NetlinkMessage{" + (this.mHeader == null ? "" : this.mHeader.toString()) + "}";
    }
}
