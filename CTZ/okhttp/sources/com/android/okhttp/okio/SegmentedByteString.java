package com.android.okhttp.okio;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

final class SegmentedByteString extends ByteString {
    final transient int[] directory;
    final transient byte[][] segments;

    SegmentedByteString(Buffer buffer, int i) {
        super(null);
        Util.checkOffsetAndCount(buffer.size, 0L, i);
        int i2 = 0;
        Segment segment = buffer.head;
        int i3 = 0;
        int i4 = 0;
        while (i3 < i) {
            if (segment.limit == segment.pos) {
                throw new AssertionError("s.limit == s.pos");
            }
            i3 += segment.limit - segment.pos;
            i4++;
            segment = segment.next;
        }
        this.segments = new byte[i4][];
        this.directory = new int[i4 * 2];
        Segment segment2 = buffer.head;
        int i5 = 0;
        while (i2 < i) {
            this.segments[i5] = segment2.data;
            i2 += segment2.limit - segment2.pos;
            this.directory[i5] = i2;
            this.directory[this.segments.length + i5] = segment2.pos;
            segment2.shared = true;
            i5++;
            segment2 = segment2.next;
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
    public ByteString substring(int i) {
        return toByteString().substring(i);
    }

    @Override
    public ByteString substring(int i, int i2) {
        return toByteString().substring(i, i2);
    }

    @Override
    public byte getByte(int i) {
        Util.checkOffsetAndCount(this.directory[this.segments.length - 1], i, 1L);
        int iSegment = segment(i);
        return this.segments[iSegment][(i - (iSegment == 0 ? 0 : this.directory[iSegment - 1])) + this.directory[this.segments.length + iSegment]];
    }

    private int segment(int i) {
        int iBinarySearch = Arrays.binarySearch(this.directory, 0, this.segments.length, i + 1);
        return iBinarySearch >= 0 ? iBinarySearch : ~iBinarySearch;
    }

    @Override
    public int size() {
        return this.directory[this.segments.length - 1];
    }

    @Override
    public byte[] toByteArray() {
        byte[] bArr = new byte[this.directory[this.segments.length - 1]];
        int length = this.segments.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3 = this.directory[length + i];
            int i4 = this.directory[i];
            System.arraycopy(this.segments[i], i3, bArr, i2, i4 - i2);
            i++;
            i2 = i4;
        }
        return bArr;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("out == null");
        }
        int length = this.segments.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3 = this.directory[length + i];
            int i4 = this.directory[i];
            outputStream.write(this.segments[i], i3, i4 - i2);
            i++;
            i2 = i4;
        }
    }

    @Override
    void write(Buffer buffer) {
        int length = this.segments.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3 = this.directory[length + i];
            int i4 = this.directory[i];
            Segment segment = new Segment(this.segments[i], i3, (i3 + i4) - i2);
            if (buffer.head == null) {
                segment.prev = segment;
                segment.next = segment;
                buffer.head = segment;
            } else {
                buffer.head.prev.push(segment);
            }
            i++;
            i2 = i4;
        }
        buffer.size += (long) i2;
    }

    @Override
    public boolean rangeEquals(int i, ByteString byteString, int i2, int i3) {
        int i4;
        if (i > size() - i3) {
            return false;
        }
        int iSegment = segment(i);
        while (i3 > 0) {
            if (iSegment != 0) {
                i4 = this.directory[iSegment - 1];
            } else {
                i4 = 0;
            }
            int iMin = Math.min(i3, ((this.directory[iSegment] - i4) + i4) - i);
            if (!byteString.rangeEquals(i2, this.segments[iSegment], (i - i4) + this.directory[this.segments.length + iSegment], iMin)) {
                return false;
            }
            i += iMin;
            i2 += iMin;
            i3 -= iMin;
            iSegment++;
        }
        return true;
    }

    @Override
    public boolean rangeEquals(int i, byte[] bArr, int i2, int i3) {
        int i4;
        if (i > size() - i3 || i2 > bArr.length - i3) {
            return false;
        }
        int iSegment = segment(i);
        while (i3 > 0) {
            if (iSegment != 0) {
                i4 = this.directory[iSegment - 1];
            } else {
                i4 = 0;
            }
            int iMin = Math.min(i3, ((this.directory[iSegment] - i4) + i4) - i);
            if (!Util.arrayRangeEquals(this.segments[iSegment], (i - i4) + this.directory[this.segments.length + iSegment], bArr, i2, iMin)) {
                return false;
            }
            i += iMin;
            i2 += iMin;
            i3 -= iMin;
            iSegment++;
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
        if (obj instanceof ByteString) {
            ByteString byteString = (ByteString) obj;
            if (byteString.size() == size() && rangeEquals(0, byteString, 0, size())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int i = this.hashCode;
        if (i != 0) {
            return i;
        }
        int length = this.segments.length;
        int i2 = 0;
        int i3 = 1;
        int i4 = 0;
        while (i2 < length) {
            byte[] bArr = this.segments[i2];
            int i5 = this.directory[length + i2];
            int i6 = this.directory[i2];
            int i7 = (i6 - i4) + i5;
            while (i5 < i7) {
                i3 = bArr[i5] + (31 * i3);
                i5++;
            }
            i2++;
            i4 = i6;
        }
        this.hashCode = i3;
        return i3;
    }

    @Override
    public String toString() {
        return toByteString().toString();
    }

    private Object writeReplace() {
        return toByteString();
    }
}
