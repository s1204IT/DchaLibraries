package android.net.netlink;

import java.nio.ByteBuffer;

public class StructNlMsgErr {
    public static final int STRUCT_SIZE = 20;
    public int error;
    public StructNlMsgHdr msg;

    public static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.remaining() >= 20;
    }

    public static StructNlMsgErr parse(ByteBuffer byteBuffer) {
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        StructNlMsgErr structNlMsgErr = new StructNlMsgErr();
        structNlMsgErr.error = byteBuffer.getInt();
        structNlMsgErr.msg = StructNlMsgHdr.parse(byteBuffer);
        return structNlMsgErr;
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.putInt(this.error);
        if (this.msg != null) {
            this.msg.pack(byteBuffer);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StructNlMsgErr{ error{");
        sb.append(this.error);
        sb.append("}, msg{");
        sb.append(this.msg == null ? "" : this.msg.toString());
        sb.append("} }");
        return sb.toString();
    }
}
