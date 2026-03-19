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

    private static final int[] f22androidicuimplTrie2$ValueWidthSwitchesValues = null;
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
        public int map(int in) {
            return in;
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

    private static int[] m54getandroidicuimplTrie2$ValueWidthSwitchesValues() {
        if (f22androidicuimplTrie2$ValueWidthSwitchesValues != null) {
            return f22androidicuimplTrie2$ValueWidthSwitchesValues;
        }
        int[] iArr = new int[ValueWidth.valuesCustom().length];
        try {
            iArr[ValueWidth.BITS_16.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ValueWidth.BITS_32.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f22androidicuimplTrie2$ValueWidthSwitchesValues = iArr;
        return iArr;
    }

    public abstract int get(int i);

    public abstract int getFromU16SingleLead(char c);

    public static Trie2 createFromSerialized(ByteBuffer bytes) throws IOException {
        ValueWidth width;
        Trie2 This;
        ByteOrder outerByteOrder = bytes.order();
        try {
            UTrie2Header header = new UTrie2Header();
            header.signature = bytes.getInt();
            switch (header.signature) {
                case 845771348:
                    boolean isBigEndian = outerByteOrder == ByteOrder.BIG_ENDIAN;
                    bytes.order(isBigEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                    header.signature = 1416784178;
                    break;
                case 1416784178:
                    break;
                default:
                    throw new IllegalArgumentException("Buffer does not contain a serialized UTrie2");
            }
            header.options = bytes.getChar();
            header.indexLength = bytes.getChar();
            header.shiftedDataLength = bytes.getChar();
            header.index2NullOffset = bytes.getChar();
            header.dataNullOffset = bytes.getChar();
            header.shiftedHighStart = bytes.getChar();
            if ((header.options & 15) > 1) {
                throw new IllegalArgumentException("UTrie2 serialized format error.");
            }
            if ((header.options & 15) == 0) {
                width = ValueWidth.BITS_16;
                This = new Trie2_16();
            } else {
                width = ValueWidth.BITS_32;
                This = new Trie2_32();
            }
            This.header = header;
            This.indexLength = header.indexLength;
            This.dataLength = header.shiftedDataLength << 2;
            This.index2NullOffset = header.index2NullOffset;
            This.dataNullOffset = header.dataNullOffset;
            This.highStart = header.shiftedHighStart << 11;
            This.highValueIndex = This.dataLength - 4;
            if (width == ValueWidth.BITS_16) {
                This.highValueIndex += This.indexLength;
            }
            int indexArraySize = This.indexLength;
            if (width == ValueWidth.BITS_16) {
                indexArraySize += This.dataLength;
            }
            This.index = ICUBinary.getChars(bytes, indexArraySize, 0);
            if (width == ValueWidth.BITS_16) {
                This.data16 = This.indexLength;
            } else {
                This.data32 = ICUBinary.getInts(bytes, This.dataLength, 0);
            }
            switch (m54getandroidicuimplTrie2$ValueWidthSwitchesValues()[width.ordinal()]) {
                case 1:
                    This.data32 = null;
                    This.initialValue = This.index[This.dataNullOffset];
                    This.errorValue = This.index[This.data16 + 128];
                    break;
                case 2:
                    This.data16 = 0;
                    This.initialValue = This.data32[This.dataNullOffset];
                    This.errorValue = This.data32[128];
                    break;
                default:
                    throw new IllegalArgumentException("UTrie2 serialized format error.");
            }
            return This;
        } finally {
            bytes.order(outerByteOrder);
        }
    }

    public static int getVersion(InputStream is, boolean littleEndianOk) throws IOException {
        if (!is.markSupported()) {
            throw new IllegalArgumentException("Input stream must support mark().");
        }
        is.mark(4);
        byte[] sig = new byte[4];
        int read = is.read(sig);
        is.reset();
        if (read != sig.length) {
            return 0;
        }
        if (sig[0] == 84 && sig[1] == 114 && sig[2] == 105 && sig[3] == 101) {
            return 1;
        }
        if (sig[0] == 84 && sig[1] == 114 && sig[2] == 105 && sig[3] == 50) {
            return 2;
        }
        if (littleEndianOk) {
            if (sig[0] == 101 && sig[1] == 105 && sig[2] == 114 && sig[3] == 84) {
                return 1;
            }
            if (sig[0] == 50 && sig[1] == 105 && sig[2] == 114 && sig[3] == 84) {
                return 2;
            }
        }
        return 0;
    }

    public final boolean equals(Object other) {
        if (!(other instanceof Trie2)) {
            return false;
        }
        Trie2 OtherTrie = (Trie2) other;
        Iterator<Range> otherIter = OtherTrie.iterator();
        for (Range rangeFromThis : this) {
            if (!otherIter.hasNext()) {
                return false;
            }
            Range rangeFromOther = otherIter.next();
            if (!rangeFromThis.equals(rangeFromOther)) {
                return false;
            }
        }
        return !otherIter.hasNext() && this.errorValue == OtherTrie.errorValue && this.initialValue == OtherTrie.initialValue;
    }

    public int hashCode() {
        if (this.fHash == 0) {
            int hash = initHash();
            for (Range r : this) {
                hash = hashInt(hash, r.hashCode());
            }
            if (hash == 0) {
                hash = 1;
            }
            this.fHash = hash;
        }
        return this.fHash;
    }

    public static class Range {
        public int endCodePoint;
        public boolean leadSurrogate;
        public int startCodePoint;
        public int value;

        public boolean equals(Object other) {
            if (other == null || !other.getClass().equals(getClass())) {
                return false;
            }
            Range tother = (Range) other;
            return this.startCodePoint == tother.startCodePoint && this.endCodePoint == tother.endCodePoint && this.value == tother.value && this.leadSurrogate == tother.leadSurrogate;
        }

        public int hashCode() {
            int h = Trie2.initHash();
            return Trie2.hashByte(Trie2.hashInt(Trie2.hashUChar32(Trie2.hashUChar32(h, this.startCodePoint), this.endCodePoint), this.value), this.leadSurrogate ? 1 : 0);
        }
    }

    @Override
    public Iterator<Range> iterator() {
        return iterator(defaultValueMapper);
    }

    public Iterator<Range> iterator(ValueMapper mapper) {
        return new Trie2Iterator(mapper);
    }

    public Iterator<Range> iteratorForLeadSurrogate(char lead, ValueMapper mapper) {
        return new Trie2Iterator(lead, mapper);
    }

    public Iterator<Range> iteratorForLeadSurrogate(char lead) {
        return new Trie2Iterator(lead, defaultValueMapper);
    }

    protected int serializeHeader(DataOutputStream dos) throws IOException {
        dos.writeInt(this.header.signature);
        dos.writeShort(this.header.options);
        dos.writeShort(this.header.indexLength);
        dos.writeShort(this.header.shiftedDataLength);
        dos.writeShort(this.header.index2NullOffset);
        dos.writeShort(this.header.dataNullOffset);
        dos.writeShort(this.header.shiftedHighStart);
        for (int i = 0; i < this.header.indexLength; i++) {
            dos.writeChar(this.index[i]);
        }
        int bytesWritten = this.header.indexLength + 16;
        return bytesWritten;
    }

    public CharSequenceIterator charSequenceIterator(CharSequence text, int index) {
        return new CharSequenceIterator(text, index);
    }

    public class CharSequenceIterator implements Iterator<CharSequenceValues> {
        private CharSequenceValues fResults = new CharSequenceValues();
        private int index;
        private CharSequence text;
        private int textLength;

        CharSequenceIterator(CharSequence t, int index) {
            this.text = t;
            this.textLength = this.text.length();
            set(index);
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
            int c = Character.codePointAt(this.text, this.index);
            int val = Trie2.this.get(c);
            this.fResults.index = this.index;
            this.fResults.codePoint = c;
            this.fResults.value = val;
            this.index++;
            if (c >= 65536) {
                this.index++;
            }
            return this.fResults;
        }

        public CharSequenceValues previous() {
            int c = Character.codePointBefore(this.text, this.index);
            int val = Trie2.this.get(c);
            this.index--;
            if (c >= 65536) {
                this.index--;
            }
            this.fResults.index = this.index;
            this.fResults.codePoint = c;
            this.fResults.value = val;
            return this.fResults;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Trie2.CharSequenceIterator does not support remove().");
        }
    }

    enum ValueWidth {
        BITS_16,
        BITS_32;

        public static ValueWidth[] valuesCustom() {
            return values();
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

        Trie2Iterator(ValueMapper vm) {
            this.returnValue = new Range();
            this.doingCodePoints = true;
            this.doLeadSurrogates = true;
            this.mapper = vm;
            this.nextStart = 0;
            this.limitCP = 1114112;
            this.doLeadSurrogates = true;
        }

        Trie2Iterator(char leadSurrogate, ValueMapper vm) {
            this.returnValue = new Range();
            this.doingCodePoints = true;
            this.doLeadSurrogates = true;
            if (leadSurrogate < 55296 || leadSurrogate > 56319) {
                throw new IllegalArgumentException("Bad lead surrogate value.");
            }
            this.mapper = vm;
            this.nextStart = (leadSurrogate - 55232) << 10;
            this.limitCP = this.nextStart + 1024;
            this.doLeadSurrogates = false;
        }

        @Override
        public Range next() {
            int mappedVal;
            int endOfRange;
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (this.nextStart >= this.limitCP) {
                this.doingCodePoints = false;
                this.nextStart = 55296;
            }
            if (this.doingCodePoints) {
                int val = Trie2.this.get(this.nextStart);
                mappedVal = this.mapper.map(val);
                endOfRange = Trie2.this.rangeEnd(this.nextStart, this.limitCP, val);
                while (endOfRange < this.limitCP - 1) {
                    int val2 = Trie2.this.get(endOfRange + 1);
                    if (this.mapper.map(val2) != mappedVal) {
                        break;
                    }
                    endOfRange = Trie2.this.rangeEnd(endOfRange + 1, this.limitCP, val2);
                }
            } else {
                int val3 = Trie2.this.getFromU16SingleLead((char) this.nextStart);
                mappedVal = this.mapper.map(val3);
                endOfRange = rangeEndLS((char) this.nextStart);
                while (endOfRange < 56319) {
                    int val4 = Trie2.this.getFromU16SingleLead((char) (endOfRange + 1));
                    if (this.mapper.map(val4) != mappedVal) {
                        break;
                    }
                    endOfRange = rangeEndLS((char) (endOfRange + 1));
                }
            }
            this.returnValue.startCodePoint = this.nextStart;
            this.returnValue.endCodePoint = endOfRange;
            this.returnValue.value = mappedVal;
            this.returnValue.leadSurrogate = this.doingCodePoints ? false : true;
            this.nextStart = endOfRange + 1;
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

        private int rangeEndLS(char startingLS) {
            if (startingLS >= 56319) {
                return UTF16.LEAD_SURROGATE_MAX_VALUE;
            }
            int val = Trie2.this.getFromU16SingleLead(startingLS);
            int c = startingLS + 1;
            while (c <= 56319 && Trie2.this.getFromU16SingleLead((char) c) == val) {
                c++;
            }
            return c - 1;
        }
    }

    int rangeEnd(int start, int limitp, int val) {
        int limit = Math.min(this.highStart, limitp);
        int c = start + 1;
        while (c < limit && get(c) == val) {
            c++;
        }
        if (c >= this.highStart) {
            c = limitp;
        }
        return c - 1;
    }

    private static int initHash() {
        return -2128831035;
    }

    private static int hashByte(int h, int b) {
        return (h * 16777619) ^ b;
    }

    private static int hashUChar32(int h, int c) {
        return hashByte(hashByte(hashByte(h, c & 255), (c >> 8) & 255), c >> 16);
    }

    private static int hashInt(int h, int i) {
        return hashByte(hashByte(hashByte(hashByte(h, i & 255), (i >> 8) & 255), (i >> 16) & 255), (i >> 24) & 255);
    }
}
