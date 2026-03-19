package com.android.internal.midi;

import android.media.midi.MidiReceiver;
import java.io.IOException;

public class MidiFramer extends MidiReceiver {
    public String TAG = "MidiFramer";
    private byte[] mBuffer = new byte[3];
    private int mCount;
    private boolean mInSysEx;
    private int mNeeded;
    private MidiReceiver mReceiver;
    private byte mRunningStatus;

    public MidiFramer(MidiReceiver midiReceiver) {
        this.mReceiver = midiReceiver;
    }

    public static String formatMidiData(byte[] bArr, int i, int i2) {
        String str = "MIDI+" + i + " : ";
        for (int i3 = 0; i3 < i2; i3++) {
            str = str + String.format("0x%02X, ", Byte.valueOf(bArr[i + i3]));
        }
        return str;
    }

    @Override
    public void onSend(byte[] bArr, int i, int i2, long j) throws IOException {
        int i3;
        int i4 = i;
        int i5 = this.mInSysEx ? i : -1;
        for (int i6 = 0; i6 < i2; i6++) {
            byte b = bArr[i4];
            int i7 = b & 255;
            if (i7 >= 128) {
                if (i7 < 240) {
                    this.mRunningStatus = b;
                    this.mCount = 1;
                    this.mNeeded = MidiConstants.getBytesPerMessage(b) - 1;
                } else if (i7 >= 248) {
                    if (this.mInSysEx) {
                        this.mReceiver.send(bArr, i5, i4 - i5, j);
                        i3 = i4 + 1;
                    } else {
                        i3 = i5;
                    }
                    this.mReceiver.send(bArr, i4, 1, j);
                    i5 = i3;
                } else if (i7 == 240) {
                    this.mInSysEx = true;
                    i5 = i4;
                } else if (i7 != 247) {
                    this.mBuffer[0] = b;
                    this.mRunningStatus = (byte) 0;
                    this.mCount = 1;
                    this.mNeeded = MidiConstants.getBytesPerMessage(b) - 1;
                } else if (this.mInSysEx) {
                    this.mReceiver.send(bArr, i5, (i4 - i5) + 1, j);
                    this.mInSysEx = false;
                    i5 = -1;
                }
            } else if (!this.mInSysEx) {
                byte[] bArr2 = this.mBuffer;
                int i8 = this.mCount;
                this.mCount = i8 + 1;
                bArr2[i8] = b;
                int i9 = this.mNeeded - 1;
                this.mNeeded = i9;
                if (i9 == 0) {
                    if (this.mRunningStatus != 0) {
                        this.mBuffer[0] = this.mRunningStatus;
                    }
                    this.mReceiver.send(this.mBuffer, 0, this.mCount, j);
                    this.mNeeded = MidiConstants.getBytesPerMessage(this.mBuffer[0]) - 1;
                    this.mCount = 1;
                }
            }
            i4++;
        }
        if (i5 < 0 || i5 >= i4) {
            return;
        }
        this.mReceiver.send(bArr, i5, i4 - i5, j);
    }
}
