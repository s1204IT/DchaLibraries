package com.googlecode.mp4parser.boxes.mp4.samplegrouping;

import com.coremedia.iso.Hex;
import java.nio.ByteBuffer;

public class UnknownEntry extends GroupEntry {
    private ByteBuffer content;

    @Override
    public void parse(ByteBuffer byteBuffer) {
        this.content = (ByteBuffer) byteBuffer.duplicate().rewind();
    }

    @Override
    public ByteBuffer get() {
        return this.content.duplicate();
    }

    public String toString() {
        ByteBuffer byteBufferDuplicate = this.content.duplicate();
        byteBufferDuplicate.rewind();
        byte[] bArr = new byte[byteBufferDuplicate.limit()];
        byteBufferDuplicate.get(bArr);
        return "UnknownEntry{content=" + Hex.encodeHex(bArr) + '}';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UnknownEntry unknownEntry = (UnknownEntry) obj;
        if (this.content == null ? unknownEntry.content == null : this.content.equals(unknownEntry.content)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        if (this.content != null) {
            return this.content.hashCode();
        }
        return 0;
    }
}
