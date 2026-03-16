package com.googlecode.mp4parser.boxes.mp4.samplegrouping;

import java.nio.ByteBuffer;

public class VisualRandomAccessEntry extends GroupEntry {
    private short numLeadingSamples;
    private boolean numLeadingSamplesKnown;

    @Override
    public void parse(ByteBuffer byteBuffer) {
        byte b = byteBuffer.get();
        this.numLeadingSamplesKnown = (b & 128) == 128;
        this.numLeadingSamples = (short) (b & 127);
    }

    @Override
    public ByteBuffer get() {
        ByteBuffer content = ByteBuffer.allocate(1);
        content.put((byte) ((this.numLeadingSamplesKnown ? 128 : 0) | (this.numLeadingSamples & 127)));
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
        VisualRandomAccessEntry that = (VisualRandomAccessEntry) o;
        return this.numLeadingSamples == that.numLeadingSamples && this.numLeadingSamplesKnown == that.numLeadingSamplesKnown;
    }

    public int hashCode() {
        int result = this.numLeadingSamplesKnown ? 1 : 0;
        return (result * 31) + this.numLeadingSamples;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VisualRandomAccessEntry");
        sb.append("{numLeadingSamplesKnown=").append(this.numLeadingSamplesKnown);
        sb.append(", numLeadingSamples=").append((int) this.numLeadingSamples);
        sb.append('}');
        return sb.toString();
    }
}
