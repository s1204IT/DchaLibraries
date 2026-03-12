package android.net.netlink;

import java.nio.ByteBuffer;

public class NetlinkErrorMessage extends NetlinkMessage {
    private StructNlMsgErr mNlMsgErr;

    public static NetlinkErrorMessage parse(StructNlMsgHdr structNlMsgHdr, ByteBuffer byteBuffer) {
        NetlinkErrorMessage netlinkErrorMessage = new NetlinkErrorMessage(structNlMsgHdr);
        netlinkErrorMessage.mNlMsgErr = StructNlMsgErr.parse(byteBuffer);
        if (netlinkErrorMessage.mNlMsgErr == null) {
            return null;
        }
        return netlinkErrorMessage;
    }

    NetlinkErrorMessage(StructNlMsgHdr structNlMsgHdr) {
        super(structNlMsgHdr);
        this.mNlMsgErr = null;
    }

    public StructNlMsgErr getNlMsgError() {
        return this.mNlMsgErr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NetlinkErrorMessage{ nlmsghdr{");
        sb.append(this.mHeader == null ? "" : this.mHeader.toString());
        sb.append("}, nlmsgerr{");
        sb.append(this.mNlMsgErr == null ? "" : this.mNlMsgErr.toString());
        sb.append("} }");
        return sb.toString();
    }
}
