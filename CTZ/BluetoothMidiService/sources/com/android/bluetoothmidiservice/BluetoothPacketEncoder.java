package com.android.bluetoothmidiservice;

import android.media.midi.MidiReceiver;
import com.android.bluetoothmidiservice.PacketEncoder;
import com.android.internal.midi.MidiConstants;
import com.android.internal.midi.MidiFramer;
import java.io.IOException;

public class BluetoothPacketEncoder extends PacketEncoder {
    private int mAccumulatedBytes;
    private final byte[] mAccumulationBuffer;
    private final PacketEncoder.PacketReceiver mPacketReceiver;
    private int mPacketTimestamp;
    private byte mRunningStatus;
    private boolean mWritePending;
    private final Object mLock = new Object();
    private final MidiReceiver mFramedDataReceiver = new MidiReceiver() {
        @Override
        public void onSend(byte[] bArr, int i, int i2, long j) throws IOException {
            synchronized (BluetoothPacketEncoder.this.mLock) {
                int i3 = ((int) (j / 1000000)) & 8191;
                byte b = bArr[i];
                boolean z = b == -16;
                boolean z2 = (b & 128) == 0;
                int i4 = (z || z2) ? 1 : i2;
                boolean z3 = (b == BluetoothPacketEncoder.this.mRunningStatus && i3 == BluetoothPacketEncoder.this.mPacketTimestamp) ? false : true;
                if (!z) {
                    if (z2) {
                        z3 = false;
                    }
                } else {
                    z3 = true;
                }
                if (z3) {
                    i4++;
                }
                if (b == BluetoothPacketEncoder.this.mRunningStatus) {
                    i4--;
                }
                if (BluetoothPacketEncoder.this.mAccumulatedBytes + i4 > BluetoothPacketEncoder.this.mAccumulationBuffer.length) {
                    BluetoothPacketEncoder.this.flushLocked(true);
                }
                if (BluetoothPacketEncoder.this.appendHeader(i3)) {
                    z3 = !z2;
                }
                if (z3) {
                    BluetoothPacketEncoder.this.mAccumulationBuffer[BluetoothPacketEncoder.access$308(BluetoothPacketEncoder.this)] = (byte) ((i3 & 127) | 128);
                    BluetoothPacketEncoder.this.mPacketTimestamp = i3;
                }
                if (!z && !z2) {
                    if (b != BluetoothPacketEncoder.this.mRunningStatus) {
                        BluetoothPacketEncoder.this.mAccumulationBuffer[BluetoothPacketEncoder.access$308(BluetoothPacketEncoder.this)] = b;
                        if (MidiConstants.allowRunningStatus(b)) {
                            BluetoothPacketEncoder.this.mRunningStatus = b;
                        } else if (MidiConstants.cancelsRunningStatus(b)) {
                            BluetoothPacketEncoder.this.mRunningStatus = (byte) 0;
                        }
                    }
                    int i5 = i2 - 1;
                    System.arraycopy(bArr, i + 1, BluetoothPacketEncoder.this.mAccumulationBuffer, BluetoothPacketEncoder.this.mAccumulatedBytes, i5);
                    BluetoothPacketEncoder.access$312(BluetoothPacketEncoder.this, i5);
                } else {
                    boolean z4 = bArr[(i + i2) - 1] == -9;
                    if (z4) {
                        i2--;
                    }
                    while (i2 > 0) {
                        if (BluetoothPacketEncoder.this.mAccumulatedBytes == BluetoothPacketEncoder.this.mAccumulationBuffer.length) {
                            BluetoothPacketEncoder.this.flushLocked(true);
                            BluetoothPacketEncoder.this.appendHeader(i3);
                        }
                        int length = BluetoothPacketEncoder.this.mAccumulationBuffer.length - BluetoothPacketEncoder.this.mAccumulatedBytes;
                        if (length > i2) {
                            length = i2;
                        }
                        System.arraycopy(bArr, i, BluetoothPacketEncoder.this.mAccumulationBuffer, BluetoothPacketEncoder.this.mAccumulatedBytes, length);
                        BluetoothPacketEncoder.access$312(BluetoothPacketEncoder.this, length);
                        i += length;
                        i2 -= length;
                    }
                    if (z4) {
                        if (BluetoothPacketEncoder.this.mAccumulatedBytes + 2 > BluetoothPacketEncoder.this.mAccumulationBuffer.length) {
                            BluetoothPacketEncoder.this.flushLocked(true);
                            BluetoothPacketEncoder.this.appendHeader(i3);
                        }
                        BluetoothPacketEncoder.this.mAccumulationBuffer[BluetoothPacketEncoder.access$308(BluetoothPacketEncoder.this)] = (byte) ((i3 & 127) | 128);
                        BluetoothPacketEncoder.this.mAccumulationBuffer[BluetoothPacketEncoder.access$308(BluetoothPacketEncoder.this)] = -9;
                    }
                }
                BluetoothPacketEncoder.this.flushLocked(false);
            }
        }
    };
    private final MidiFramer mMidiFramer = new MidiFramer(this.mFramedDataReceiver);

    static int access$308(BluetoothPacketEncoder bluetoothPacketEncoder) {
        int i = bluetoothPacketEncoder.mAccumulatedBytes;
        bluetoothPacketEncoder.mAccumulatedBytes = i + 1;
        return i;
    }

    static int access$312(BluetoothPacketEncoder bluetoothPacketEncoder, int i) {
        int i2 = bluetoothPacketEncoder.mAccumulatedBytes + i;
        bluetoothPacketEncoder.mAccumulatedBytes = i2;
        return i2;
    }

    private boolean appendHeader(int i) {
        if (this.mAccumulatedBytes == 0) {
            byte[] bArr = this.mAccumulationBuffer;
            int i2 = this.mAccumulatedBytes;
            this.mAccumulatedBytes = i2 + 1;
            bArr[i2] = (byte) (128 | ((i >> 7) & 63));
            this.mPacketTimestamp = i;
            return true;
        }
        return false;
    }

    public BluetoothPacketEncoder(PacketEncoder.PacketReceiver packetReceiver, int i) {
        this.mPacketReceiver = packetReceiver;
        this.mAccumulationBuffer = new byte[i];
    }

    @Override
    public void onSend(byte[] bArr, int i, int i2, long j) throws IOException {
        this.mMidiFramer.send(bArr, i, i2, j);
    }

    public void writeComplete() {
        synchronized (this.mLock) {
            this.mWritePending = false;
            flushLocked(false);
            this.mLock.notify();
        }
    }

    private void flushLocked(boolean z) {
        if (this.mWritePending && !z) {
            return;
        }
        while (this.mWritePending && this.mAccumulatedBytes > 0) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
            }
        }
        if (this.mAccumulatedBytes > 0) {
            this.mPacketReceiver.writePacket(this.mAccumulationBuffer, this.mAccumulatedBytes);
            this.mAccumulatedBytes = 0;
            this.mPacketTimestamp = 0;
            this.mRunningStatus = (byte) 0;
            this.mWritePending = true;
        }
    }
}
