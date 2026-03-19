package com.android.okhttp.okio;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

final class SegmentedByteString extends ByteString {
    final transient int[] directory;
    final transient byte[][] segments;

    SegmentedByteString(Buffer buffer, int byteCount) {
        super(null);
        Util.checkOffsetAndCount(buffer.size, 0L, byteCount);
        int offset = 0;
        int segmentCount = 0;
        Segment s = buffer.head;
        while (offset < byteCount) {
            if (s.limit == s.pos) {
                throw new AssertionError("s.limit == s.pos");
            }
            offset += s.limit - s.pos;
            segmentCount++;
            s = s.next;
        }
        this.segments = new byte[segmentCount][];
        this.directory = new int[segmentCount * 2];
        int offset2 = 0;
        int segmentCount2 = 0;
        Segment s2 = buffer.head;
        while (offset2 < byteCount) {
            this.segments[segmentCount2] = s2.data;
            offset2 += s2.limit - s2.pos;
            this.directory[segmentCount2] = offset2;
            this.directory[this.segments.length + segmentCount2] = s2.pos;
            s2.shared = true;
            segmentCount2++;
            s2 = s2.next;
        }
    }

    @Override
    public String utf8() {
        return toByteString().utf8();
    }

    @Override
    public String base64() {
        return toByteString().base64();
    }

    @Override
    public String hex() {
        return toByteString().hex();
    }

    @Override
    public ByteString toAsciiLowercase() {
        return toByteString().toAsciiLowercase();
    }

    @Override
    public ByteString toAsciiUppercase() {
        return toByteString().toAsciiUppercase();
    }

    @Override
    public ByteString md5() {
        return toByteString().md5();
    }

    @Override
    public ByteString sha256() {
        return toByteString().sha256();
    }

    @Override
    public String base64Url() {
        return toByteString().base64Url();
    }

    @Override
    public ByteString substring(int beginIndex) {
        return toByteString().substring(beginIndex);
    }

    @Override
    public ByteString substring(int beginIndex, int endIndex) {
        return toByteString().substring(beginIndex, endIndex);
    }

    @Override
    public byte getByte(int pos) {
        Util.checkOffsetAndCount(this.directory[this.segments.length - 1], pos, 1L);
        int segment = segment(pos);
        int segmentOffset = segment == 0 ? 0 : this.directory[segment - 1];
        int segmentPos = this.directory[this.segments.length + segment];
        return this.segments[segment][(pos - segmentOffset) + segmentPos];
    }

    private int segment(int pos) {
        int i = Arrays.binarySearch(this.directory, 0, this.segments.length, pos + 1);
        return i >= 0 ? i : ~i;
    }

    @Override
    public int size() {
        return this.directory[this.segments.length - 1];
    }

    @Override
    public byte[] toByteArray() {
        byte[] result = new byte[this.directory[this.segments.length - 1]];
        int segmentOffset = 0;
        int segmentCount = this.segments.length;
        for (int s = 0; s < segmentCount; s++) {
            int segmentPos = this.directory[segmentCount + s];
            int nextSegmentOffset = this.directory[s];
            System.arraycopy(this.segments[s], segmentPos, result, segmentOffset, nextSegmentOffset - segmentOffset);
            segmentOffset = nextSegmentOffset;
        }
        return result;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("out == null");
        }
        int segmentOffset = 0;
        int segmentCount = this.segments.length;
        for (int s = 0; s < segmentCount; s++) {
            int segmentPos = this.directory[segmentCount + s];
            int nextSegmentOffset = this.directory[s];
            out.write(this.segments[s], segmentPos, nextSegmentOffset - segmentOffset);
            segmentOffset = nextSegmentOffset;
        }
    }

    @Override
    void write(Buffer buffer) {
        int segmentOffset = 0;
        int segmentCount = this.segments.length;
        for (int s = 0; s < segmentCount; s++) {
            int segmentPos = this.directory[segmentCount + s];
            int nextSegmentOffset = this.directory[s];
            Segment segment = new Segment(this.segments[s], segmentPos, (segmentPos + nextSegmentOffset) - segmentOffset);
            if (buffer.head == null) {
                segment.prev = segment;
                segment.next = segment;
                buffer.head = segment;
            } else {
                buffer.head.prev.push(segment);
            }
            segmentOffset = nextSegmentOffset;
        }
        buffer.size += (long) segmentOffset;
    }

    @Override
    public boolean rangeEquals(int offset, ByteString other, int otherOffset, int byteCount) {
        if (offset > size() - byteCount) {
            return false;
        }
        int s = segment(offset);
        while (byteCount > 0) {
            int segmentOffset = s == 0 ? 0 : this.directory[s - 1];
            int segmentSize = this.directory[s] - segmentOffset;
            int stepSize = Math.min(byteCount, (segmentOffset + segmentSize) - offset);
            int segmentPos = this.directory[this.segments.length + s];
            int arrayOffset = (offset - segmentOffset) + segmentPos;
            if (!other.rangeEquals(otherOffset, this.segments[s], arrayOffset, stepSize)) {
                return false;
            }
            offset += stepSize;
            otherOffset += stepSize;
            byteCount -= stepSize;
            s++;
        }
        return true;
    }

    @Override
    public boolean rangeEquals(int offset, byte[] other, int otherOffset, int byteCount) {
        if (offset > size() - byteCount || otherOffset > other.length - byteCount) {
            return false;
        }
        int s = segment(offset);
        while (byteCount > 0) {
            int segmentOffset = s == 0 ? 0 : this.directory[s - 1];
            int segmentSize = this.directory[s] - segmentOffset;
            int stepSize = Math.min(byteCount, (segmentOffset + segmentSize) - offset);
            int segmentPos = this.directory[this.segments.length + s];
            int arrayOffset = (offset - segmentOffset) + segmentPos;
            if (!Util.arrayRangeEquals(this.segments[s], arrayOffset, other, otherOffset, stepSize)) {
                return false;
            }
            offset += stepSize;
            otherOffset += stepSize;
            byteCount -= stepSize;
            s++;
        }
        return true;
    }

    private ByteString toByteString() {
        return new ByteString(toByteArray());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj instanceof ByteString) && obj.size() == size()) {
            return rangeEquals(0, (ByteString) obj, 0, size());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = this.hashCode;
        if (result != 0) {
            return result;
        }
        int result2 = 1;
        int segmentOffset = 0;
        int segmentCount = this.segments.length;
        for (int s = 0; s < segmentCount; s++) {
            byte[] segment = this.segments[s];
            int segmentPos = this.directory[segmentCount + s];
            int nextSegmentOffset = this.directory[s];
            int segmentSize = nextSegmentOffset - segmentOffset;
            int limit = segmentPos + segmentSize;
            for (int i = segmentPos; i < limit; i++) {
                result2 = (result2 * 31) + segment[i];
            }
            segmentOffset = nextSegmentOffset;
        }
        this.hashCode = result2;
        return result2;
    }

    @Override
    public String toString() {
        return toByteString().toString();
    }

    private Object writeReplace() {
        return toByteString();
    }
}
