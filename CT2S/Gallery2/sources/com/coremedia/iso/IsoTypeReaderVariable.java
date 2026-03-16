package com.coremedia.iso;

import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import java.nio.ByteBuffer;

public final class IsoTypeReaderVariable {
    public static long read(ByteBuffer bb, int bytes) {
        switch (bytes) {
            case 1:
                return IsoTypeReader.readUInt8(bb);
            case 2:
                return IsoTypeReader.readUInt16(bb);
            case 3:
                return IsoTypeReader.readUInt24(bb);
            case 4:
                return IsoTypeReader.readUInt32(bb);
            case 5:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
            case 7:
            default:
                throw new RuntimeException("I don't know how to read " + bytes + " bytes");
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                return IsoTypeReader.readUInt64(bb);
        }
    }
}
