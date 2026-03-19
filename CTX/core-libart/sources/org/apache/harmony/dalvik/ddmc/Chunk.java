package org.apache.harmony.dalvik.ddmc;

import java.nio.ByteBuffer;

public class Chunk {
    public byte[] data;
    public int length;
    public int offset;
    public int type;

    public Chunk() {
    }

    public Chunk(int i, byte[] bArr, int i2, int i3) {
        this.type = i;
        this.data = bArr;
        this.offset = i2;
        this.length = i3;
    }

    public Chunk(int i, ByteBuffer byteBuffer) {
        this.type = i;
        this.data = byteBuffer.array();
        this.offset = byteBuffer.arrayOffset();
        this.length = byteBuffer.position();
    }
}
