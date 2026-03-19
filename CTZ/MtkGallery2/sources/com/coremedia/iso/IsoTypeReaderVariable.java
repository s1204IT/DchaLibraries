package com.coremedia.iso;

import java.nio.ByteBuffer;

public final class IsoTypeReaderVariable {
    public static long read(ByteBuffer byteBuffer, int i) {
        if (i != 8) {
            switch (i) {
                case 1:
                    return IsoTypeReader.readUInt8(byteBuffer);
                case 2:
                    return IsoTypeReader.readUInt16(byteBuffer);
                case 3:
                    return IsoTypeReader.readUInt24(byteBuffer);
                case 4:
                    return IsoTypeReader.readUInt32(byteBuffer);
                default:
                    throw new RuntimeException("I don't know how to read " + i + " bytes");
            }
        }
        return IsoTypeReader.readUInt64(byteBuffer);
    }
}
