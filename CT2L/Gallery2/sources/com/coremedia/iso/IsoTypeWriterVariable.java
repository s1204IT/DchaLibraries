package com.coremedia.iso;

import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import java.nio.ByteBuffer;

public final class IsoTypeWriterVariable {
    public static void write(long v, ByteBuffer bb, int bytes) {
        switch (bytes) {
            case 1:
                IsoTypeWriter.writeUInt8(bb, (int) (255 & v));
                return;
            case 2:
                IsoTypeWriter.writeUInt16(bb, (int) (65535 & v));
                return;
            case 3:
                IsoTypeWriter.writeUInt24(bb, (int) (16777215 & v));
                return;
            case 4:
                IsoTypeWriter.writeUInt32(bb, v);
                return;
            case 5:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
            case 7:
            default:
                throw new RuntimeException("I don't know how to read " + bytes + " bytes");
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                IsoTypeWriter.writeUInt64(bb, v);
                return;
        }
    }
}
