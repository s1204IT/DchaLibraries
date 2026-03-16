package com.googlecode.mp4parser.boxes.mp4.samplegrouping;

import java.nio.ByteBuffer;

public class RollRecoveryEntry extends GroupEntry {
    private short rollDistance;

    @Override
    public void parse(ByteBuffer byteBuffer) {
        this.rollDistance = byteBuffer.getShort();
    }

    @Override
    public ByteBuffer get() {
        ByteBuffer content = ByteBuffer.allocate(2);
        content.putShort(this.rollDistance);
        content.rewind();
        return content;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RollRecoveryEntry entry = (RollRecoveryEntry) o;
        return this.rollDistance == entry.rollDistance;
    }

    public int hashCode() {
        return this.rollDistance;
    }
}
