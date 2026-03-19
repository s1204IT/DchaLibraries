package android.icu.impl;

import android.icu.text.UTF16;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class Trie2 implements Iterable<Range> {
    static final int UNEWTRIE2_INDEX_1_LENGTH = 544;
    static final int UNEWTRIE2_INDEX_GAP_LENGTH = 576;
    static final int UNEWTRIE2_INDEX_GAP_OFFSET = 2080;
    static final int UNEWTRIE2_MAX_DATA_LENGTH = 1115264;
    static final int UNEWTRIE2_MAX_INDEX_2_LENGTH = 35488;
    static final int UTRIE2_BAD_UTF8_DATA_OFFSET = 128;
    static final int UTRIE2_CP_PER_INDEX_1_ENTRY = 2048;
    static final int UTRIE2_DATA_BLOCK_LENGTH = 32;
    static final int UTRIE2_DATA_GRANULARITY = 4;
    static final int UTRIE2_DATA_MASK = 31;
    static final int UTRIE2_DATA_START_OFFSET = 192;
    static final int UTRIE2_INDEX_1_OFFSET = 2112;
    static final int UTRIE2_INDEX_2_BLOCK_LENGTH = 64;
    static final int UTRIE2_INDEX_2_BMP_LENGTH = 2080;
    static final int UTRIE2_INDEX_2_MASK = 63;
    static final int UTRIE2_INDEX_2_OFFSET = 0;
    static final int UTRIE2_INDEX_SHIFT = 2;
    static final int UTRIE2_LSCP_INDEX_2_LENGTH = 32;
    static final int UTRIE2_LSCP_INDEX_2_OFFSET = 2048;
    static final int UTRIE2_MAX_INDEX_1_LENGTH = 512;
    static final int UTRIE2_OMITTED_BMP_INDEX_1_LENGTH = 32;
    static final int UTRIE2_OPTIONS_VALUE_BITS_MASK = 15;
    static final int UTRIE2_SHIFT_1 = 11;
    static final int UTRIE2_SHIFT_1_2 = 6;
    static final int UTRIE2_SHIFT_2 = 5;
    static final int UTRIE2_UTF8_2B_INDEX_2_LENGTH = 32;
    static final int UTRIE2_UTF8_2B_INDEX_2_OFFSET = 2080;
    private static ValueMapper defaultValueMapper = new ValueMapper() {
        @Override
        public int map(int i) {
            return i;
        }
    };
    int data16;
    int[] data32;
    int dataLength;
    int dataNullOffset;
    int errorValue;
    int fHash;
    UTrie2Header header;
    int highStart;
    int highValueIndex;
    char[] index;
    int index2NullOffset;
    int indexLength;
    int initialValue;

    public static class CharSequenceValues {
        public int codePoint;
        public int index;
        public int value;
    }

    public interface ValueMapper {
        int map(int i);
    }

    enum ValueWidth {
        BITS_16,
        BITS_32
    }

    public abstract int get(int i);

    public abstract int getFromU16SingleLead(char c);

    public static Trie2 createFromSerialized(ByteBuffer byteBuffer) throws IOException {
        ValueWidth valueWidth;
        Trie2 trie2_32;
        ByteOrder byteOrderOrder = byteBuffer.order();
        try {
            UTrie2Header uTrie2Header = new UTrie2Header();
            uTrie2Header.signature = byteBuffer.getInt();
            int i = uTrie2Header.signature;
            if (i == 845771348) {
                byteBuffer.order(byteOrderOrder == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                uTrie2Header.signature = 1416784178;
            } else if (i != 1416784178) {
                throw new IllegalArgumentException("Buffer does not contain a serialized UTrie2");
            }
            uTrie2Header.options = byteBuffer.getChar();
            uTrie2Header.indexLength = byteBuffer.getChar();
            uTrie2Header.shiftedDataLength = byteBuffer.getChar();
            uTrie2Header.index2NullOffset = byteBuffer.getChar();
            uTrie2Header.dataNullOffset = byteBuffer.getChar();
            uTrie2Header.shiftedHighStart = byteBuffer.getChar();
            if ((uTrie2Header.options & 15) > 1) {
                throw new IllegalArgumentException("UTrie2 serialized format error.");
            }
            if ((uTrie2Header.options & 15) == 0) {
                valueWidth = ValueWidth.BITS_16;
                trie2_32 = new Trie2_16();
            } else {
                valueWidth = ValueWidth.BITS_32;
                trie2_32 = new Trie2_32();
            }
            trie2_32.header = uTrie2Header;
            trie2_32.indexLength = uTrie2Header.indexLength;
            trie2_32.dataLength = uTrie2Header.shiftedDataLength << 2;
            trie2_32.index2NullOffset = uTrie2Header.index2NullOffset;
            trie2_32.dataNullOffset = uTrie2Header.dataNullOffset;
            trie2_32.highStart = uTrie2Header.shiftedHighStart << 11;
            trie2_32.highValueIndex = trie2_32.dataLength - 4;
            if (valueWidth == ValueWidth.BITS_16) {
                trie2_32.highValueIndex += trie2_32.indexLength;
            }
            int i2 = trie2_32.indexLength;
            if (valueWidth == ValueWidth.BITS_16) {
                i2 += trie2_32.dataLength;
            }
            trie2_32.index = ICUBinary.getChars(byteBuffer, i2, 0);
            if (valueWidth == ValueWidth.BITS_16) {
                trie2_32.data16 = trie2_32.indexLength;
            } else {
                trie2_32.data32 = ICUBinary.getInts(byteBuffer, trie2_32.dataLength, 0);
            }
            switch (valueWidth) {
                case BITS_16:
                    trie2_32.data32 = null;
                    trie2_32.initialValue = trie2_32.index[trie2_32.dataNullOffset];
                    trie2_32.errorValue = trie2_32.index[trie2_32.data16 + 128];
                    break;
                case BITS_32:
                    trie2_32.data16 = 0;
                    trie2_32.initialValue = trie2_32.data32[trie2_32.dataNullOffset];
                    trie2_32.errorValue = trie2_32.data32[128];
                    break;
                default:
                    throw new IllegalArgumentException("UTrie2 serialized format error.");
            }
            return trie2_32;
        } finally {
            byteBuffer.order(byteOrderOrder);
        }
    }

    public static int getVersion(InputStream inputStream, boolean z) throws IOException {
        if (!inputStream.markSupported()) {
            throw new IllegalArgumentException("Input stream must support mark().");
        }
        inputStream.mark(4);
        byte[] bArr = new byte[4];
        int i = inputStream.read(bArr);
        inputStream.reset();
        if (i != bArr.length) {
            return 0;
        }
        if (bArr[0] == 84 && bArr[1] == 114 && bArr[2] == 105 && bArr[3] == 101) {
            return 1;
        }
        if (bArr[0] == 84 && bArr[1] == 114 && bArr[2] == 105 && bArr[3] == 50) {
            return 2;
        }
        if (z) {
            if (bArr[0] == 101 && bArr[1] == 105 && bArr[2] == 114 && bArr[3] == 84) {
                return 1;
            }
            if (bArr[0] == 50 && bArr[1] == 105 && bArr[2] == 114 && bArr[3] == 84) {
                return 2;
            }
        }
        return 0;
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof Trie2)) {
            return false;
        }
        Trie2 trie2 = (Trie2) obj;
        Iterator<Range> it = trie2.iterator();
        for (Range range : this) {
            if (!it.hasNext() || !range.equals(it.next())) {
                return false;
            }
        }
        return !it.hasNext() && this.errorValue == trie2.errorValue && this.initialValue == trie2.initialValue;
    }

    public int hashCode() {
        if (this.fHash == 0) {
            int iInitHash = initHash();
            Iterator<Range> it = iterator();
            while (it.hasNext()) {
                iInitHash = hashInt(iInitHash, it.next().hashCode());
            }
            if (iInitHash == 0) {
                iInitHash = 1;
            }
            this.fHash = iInitHash;
        }
        return this.fHash;
    }

    public static class Range {
        public int endCodePoint;
        public boolean leadSurrogate;
        public int startCodePoint;
        public int value;

        public boolean equals(Object obj) {
            if (obj == null || !obj.getClass().equals(getClass())) {
                return false;
            }
            Range range = (Range) obj;
            return this.startCodePoint == range.startCodePoint && this.endCodePoint == range.endCodePoint && this.value == range.value && this.leadSurrogate == range.leadSurrogate;
        }

        public int hashCode() {
            return Trie2.hashByte(Trie2.hashInt(Trie2.hashUChar32(Trie2.hashUChar32(Trie2.initHash(), this.startCodePoint), this.endCodePoint), this.value), this.leadSurrogate ? 1 : 0);
        }
    }

    @Override
    public Iterator<Range> iterator() {
        return iterator(defaultValueMapper);
    }

    public Iterator<Range> iterator(ValueMapper valueMapper) {
        return new Trie2Iterator(valueMapper);
    }

    public Iterator<Range> iteratorForLeadSurrogate(char c, ValueMapper valueMapper) {
        return new Trie2Iterator(c, valueMapper);
    }

    public Iterator<Range> iteratorForLeadSurrogate(char c) {
        return new Trie2Iterator(c, defaultValueMapper);
    }

    protected int serializeHeader(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(this.header.signature);
        dataOutputStream.writeShort(this.header.options);
        dataOutputStream.writeShort(this.header.indexLength);
        dataOutputStream.writeShort(this.header.shiftedDataLength);
        dataOutputStream.writeShort(this.header.index2NullOffset);
        dataOutputStream.writeShort(this.header.dataNullOffset);
        dataOutputStream.writeShort(this.header.shiftedHighStart);
        for (int i = 0; i < this.header.indexLength; i++) {
            dataOutputStream.writeChar(this.index[i]);
        }
        return 16 + this.header.indexLength;
    }

    public CharSequenceIterator charSequenceIterator(CharSequence charSequence, int i) {
        return new CharSequenceIterator(charSequence, i);
    }

    public class CharSequenceIterator implements Iterator<CharSequenceValues> {
        private CharSequenceValues fResults = new CharSequenceValues();
        private int index;
        private CharSequence text;
        private int textLength;

        CharSequenceIterator(CharSequence charSequence, int i) {
            this.text = charSequence;
            this.textLength = this.text.length();
            set(i);
        }

        public void set(int i) {
            if (i < 0 || i > this.textLength) {
                throw new IndexOutOfBoundsException();
            }
            this.index = i;
        }

        @Override
        public final boolean hasNext() {
            return this.index < this.textLength;
        }

        public final boolean hasPrevious() {
            return this.index > 0;
        }

        @Override
        public CharSequenceValues next() {
            int iCodePointAt = Character.codePointAt(this.text, this.index);
            int i = Trie2.this.get(iCodePointAt);
            this.fResults.index = this.index;
            this.fResults.codePoint = iCodePointAt;
            this.fResults.value = i;
            this.index++;
            if (iCodePointAt >= 65536) {
                this.index++;
            }
            return this.fResults;
        }

        public CharSequenceValues previous() {
            int iCodePointBefore = Character.codePointBefore(this.text, this.index);
            int i = Trie2.this.get(iCodePointBefore);
            this.index--;
            if (iCodePointBefore >= 65536) {
                this.index--;
            }
            this.fResults.index = this.index;
            this.fResults.codePoint = iCodePointBefore;
            this.fResults.value = i;
            return this.fResults;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Trie2.CharSequenceIterator does not support remove().");
        }
    }

    static class UTrie2Header {
        int dataNullOffset;
        int index2NullOffset;
        int indexLength;
        int options;
        int shiftedDataLength;
        int shiftedHighStart;
        int signature;

        UTrie2Header() {
        }
    }

    class Trie2Iterator implements Iterator<Range> {
        private boolean doLeadSurrogates;
        private boolean doingCodePoints;
        private int limitCP;
        private ValueMapper mapper;
        private int nextStart;
        private Range returnValue;

        Trie2Iterator(ValueMapper valueMapper) {
            this.returnValue = new Range();
            this.doingCodePoints = true;
            this.doLeadSurrogates = true;
            this.mapper = valueMapper;
            this.nextStart = 0;
            this.limitCP = 1114112;
            this.doLeadSurrogates = true;
        }

        Trie2Iterator(char c, ValueMapper valueMapper) {
            this.returnValue = new Range();
            this.doingCodePoints = true;
            this.doLeadSurrogates = true;
            if (c < 55296 || c > 56319) {
                throw new IllegalArgumentException("Bad lead surrogate value.");
            }
            this.mapper = valueMapper;
            this.nextStart = (c - 55232) << 10;
            this.limitCP = this.nextStart + 1024;
            this.doLeadSurrogates = false;
        }

        @Override
        public Range next() {
            int map;
            int iRangeEndLS;
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (this.nextStart >= this.limitCP) {
                this.doingCodePoints = false;
                this.nextStart = 55296;
            }
            if (this.doingCodePoints) {
                int i = Trie2.this.get(this.nextStart);
                map = this.mapper.map(i);
                iRangeEndLS = Trie2.this.rangeEnd(this.nextStart, this.limitCP, i);
                while (iRangeEndLS < this.limitCP - 1) {
                    int i2 = iRangeEndLS + 1;
                    int i3 = Trie2.this.get(i2);
                    if (this.mapper.map(i3) != map) {
                        break;
                    }
                    iRangeEndLS = Trie2.this.rangeEnd(i2, this.limitCP, i3);
                }
            } else {
                map = this.mapper.map(Trie2.this.getFromU16SingleLead((char) this.nextStart));
                iRangeEndLS = rangeEndLS((char) this.nextStart);
                while (iRangeEndLS < 56319) {
                    char c = (char) (iRangeEndLS + 1);
                    if (this.mapper.map(Trie2.this.getFromU16SingleLead(c)) != map) {
                        break;
                    }
                    iRangeEndLS = rangeEndLS(c);
                }
            }
            this.returnValue.startCodePoint = this.nextStart;
            this.returnValue.endCodePoint = iRangeEndLS;
            this.returnValue.value = map;
            this.returnValue.leadSurrogate = !this.doingCodePoints;
            this.nextStart = iRangeEndLS + 1;
            return this.returnValue;
        }

        @Override
        public boolean hasNext() {
            return (this.doingCodePoints && (this.doLeadSurrogates || this.nextStart < this.limitCP)) || this.nextStart < 56320;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private int rangeEndLS(char c) {
            if (c >= 56319) {
                return UTF16.LEAD_SURROGATE_MAX_VALUE;
            }
            int fromU16SingleLead = Trie2.this.getFromU16SingleLead(c);
            do {
                c++;
                if (c > 56319) {
                    break;
                }
            } while (Trie2.this.getFromU16SingleLead((char) c) == fromU16SingleLead);
            return c - 1;
        }
    }

    int rangeEnd(int i, int i2, int i3) {
        int iMin = Math.min(this.highStart, i2);
        do {
            i++;
            if (i >= iMin) {
                break;
            }
        } while (get(i) == i3);
        if (i >= this.highStart) {
            i = i2;
        }
        return i - 1;
    }

    private static int initHash() {
        return -2128831035;
    }

    private static int hashByte(int i, int i2) {
        return (i * 16777619) ^ i2;
    }

    private static int hashUChar32(int i, int i2) {
        return hashByte(hashByte(hashByte(i, i2 & 255), (i2 >> 8) & 255), i2 >> 16);
    }

    private static int hashInt(int i, int i2) {
        return hashByte(hashByte(hashByte(hashByte(i, i2 & 255), (i2 >> 8) & 255), (i2 >> 16) & 255), (i2 >> 24) & 255);
    }
}
