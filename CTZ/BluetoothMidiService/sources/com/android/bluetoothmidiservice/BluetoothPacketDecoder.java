package com.android.bluetoothmidiservice;

import android.media.midi.MidiReceiver;
import android.util.Log;
import java.io.IOException;

public class BluetoothPacketDecoder extends PacketDecoder {
    private final byte[] mBuffer;
    private MidiBtleTimeTracker mTimeTracker;
    private final int TIMESTAMP_MASK_HIGH = 8064;
    private final int TIMESTAMP_MASK_LOW = 127;
    private final int HEADER_TIMESTAMP_MASK = 63;

    public BluetoothPacketDecoder(int i) {
        this.mBuffer = new byte[i];
    }

    public void decodePacket(byte[] bArr, MidiReceiver midiReceiver) {
        if (this.mTimeTracker == null) {
            this.mTimeTracker = new MidiBtleTimeTracker(System.nanoTime());
        }
        if (bArr.length < 1) {
            Log.e("BluetoothPacketDecoder", "empty packet");
            return;
        }
        byte b = bArr[0];
        if ((b & 192) != 128) {
            Log.e("BluetoothPacketDecoder", "packet does not start with header");
            return;
        }
        int i = 0;
        int i2 = 0;
        int i3 = (b & 63) << 7;
        long jConvertTimestampToNanotime = 0;
        boolean z = false;
        int i4 = 0;
        for (int i5 = 1; i5 < bArr.length; i5++) {
            byte b2 = bArr[i5];
            if ((b2 & 128) != 0 && !z) {
                int i6 = b2 & 127;
                if (i6 < i4) {
                    i3 = (i3 + 128) & 8064;
                }
                int i7 = i3;
                int i8 = i7 | i6;
                if (i8 != i2) {
                    if (i > 0) {
                        try {
                            midiReceiver.send(this.mBuffer, 0, i, jConvertTimestampToNanotime);
                        } catch (IOException e) {
                        }
                        i = 0;
                    }
                } else {
                    i8 = i2;
                }
                jConvertTimestampToNanotime = this.mTimeTracker.convertTimestampToNanotime(i8, System.nanoTime());
                i4 = i6;
                i3 = i7;
                i2 = i8;
                z = true;
            } else {
                this.mBuffer[i] = b2;
                z = false;
                i++;
            }
        }
        if (i > 0) {
            try {
                midiReceiver.send(this.mBuffer, 0, i, jConvertTimestampToNanotime);
            } catch (IOException e2) {
            }
        }
    }
}
