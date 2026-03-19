package com.googlecode.mp4parser.boxes.piff;

import com.coremedia.iso.Hex;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionSpecificHeader {
    protected static Map<UUID, Class<? extends ProtectionSpecificHeader>> uuidRegistry = new HashMap();
    ByteBuffer data;

    static {
        uuidRegistry.put(UUID.fromString("9A04F079-9840-4286-AB92-E65BE0885F95"), PlayReadyHeader.class);
    }

    public boolean equals(Object obj) {
        if ((obj instanceof ProtectionSpecificHeader) && getClass().equals(obj.getClass())) {
            return this.data.equals(obj.data);
        }
        return false;
    }

    public static ProtectionSpecificHeader createFor(UUID uuid, ByteBuffer byteBuffer) {
        Class<? extends ProtectionSpecificHeader> cls = uuidRegistry.get(uuid);
        ProtectionSpecificHeader protectionSpecificHeader = new ProtectionSpecificHeader();
        if (cls != null) {
            try {
                protectionSpecificHeader = cls.newInstance();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e2) {
                throw new RuntimeException(e2);
            }
        }
        protectionSpecificHeader.parse(byteBuffer);
        return protectionSpecificHeader;
    }

    public void parse(ByteBuffer byteBuffer) {
        this.data = byteBuffer;
    }

    public ByteBuffer getData() {
        return this.data;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProtectionSpecificHeader");
        sb.append("{data=");
        ByteBuffer byteBufferDuplicate = getData().duplicate();
        byteBufferDuplicate.rewind();
        byte[] bArr = new byte[byteBufferDuplicate.limit()];
        byteBufferDuplicate.get(bArr);
        sb.append(Hex.encodeHex(bArr));
        sb.append('}');
        return sb.toString();
    }
}
