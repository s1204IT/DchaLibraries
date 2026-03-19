package com.android.bluetoothmidiservice;

import android.media.midi.MidiReceiver;

public abstract class PacketEncoder extends MidiReceiver {

    public interface PacketReceiver {
        void writePacket(byte[] bArr, int i);
    }
}
