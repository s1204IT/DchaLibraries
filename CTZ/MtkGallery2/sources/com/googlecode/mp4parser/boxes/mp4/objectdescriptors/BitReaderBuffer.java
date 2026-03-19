package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;

import java.nio.ByteBuffer;

public class BitReaderBuffer {
    private ByteBuffer buffer;
    int initialPos;
    int position;

    public BitReaderBuffer(ByteBuffer byteBuffer) {
        this.buffer = byteBuffer;
        this.initialPos = byteBuffer.position();
    }

    public int readBits(int i) {
        int bits;
        int i2 = this.buffer.get(this.initialPos + (this.position / 8));
        if (i2 < 0) {
            i2 += 256;
        }
        int i3 = 8 - (this.position % 8);
        if (i <= i3) {
            bits = ((i2 << (this.position % 8)) & 255) >> ((this.position % 8) + (i3 - i));
            this.position += i;
        } else {
            int i4 = i - i3;
            bits = (readBits(i3) << i4) + readBits(i4);
        }
        this.buffer.position(this.initialPos + ((int) Math.ceil(((double) this.position) / 8.0d)));
        return bits;
    }

    public int remainingBits() {
        return (this.buffer.limit() * 8) - this.position;
    }
}
