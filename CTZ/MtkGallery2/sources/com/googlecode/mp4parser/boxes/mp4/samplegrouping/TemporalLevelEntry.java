package com.googlecode.mp4parser.boxes.mp4.samplegrouping;

import java.nio.ByteBuffer;

public class TemporalLevelEntry extends GroupEntry {
    private boolean levelIndependentlyDecodable;
    private short reserved;

    @Override
    public void parse(ByteBuffer byteBuffer) {
        this.levelIndependentlyDecodable = (byteBuffer.get() & 128) == 128;
    }

    @Override
    public ByteBuffer get() {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1);
        byteBufferAllocate.put((byte) (this.levelIndependentlyDecodable ? 128 : 0));
        byteBufferAllocate.rewind();
        return byteBufferAllocate;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TemporalLevelEntry temporalLevelEntry = (TemporalLevelEntry) obj;
        if (this.levelIndependentlyDecodable == temporalLevelEntry.levelIndependentlyDecodable && this.reserved == temporalLevelEntry.reserved) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (this.levelIndependentlyDecodable ? 1 : 0)) + this.reserved;
    }

    public String toString() {
        return "TemporalLevelEntry{levelIndependentlyDecodable=" + this.levelIndependentlyDecodable + '}';
    }
}
