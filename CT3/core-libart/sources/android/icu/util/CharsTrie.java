package android.icu.util;

import android.icu.text.UTF16;
import android.icu.util.BytesTrie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public final class CharsTrie implements Cloneable, Iterable<Entry> {

    static final boolean f106assertionsDisabled;
    static final int kMaxBranchLinearSubNodeLength = 5;
    static final int kMaxLinearMatchLength = 16;
    static final int kMaxOneUnitDelta = 64511;
    static final int kMaxOneUnitNodeValue = 255;
    static final int kMaxOneUnitValue = 16383;
    static final int kMaxTwoUnitDelta = 67043327;
    static final int kMaxTwoUnitNodeValue = 16646143;
    static final int kMaxTwoUnitValue = 1073676287;
    static final int kMinLinearMatch = 48;
    static final int kMinTwoUnitDeltaLead = 64512;
    static final int kMinTwoUnitNodeValueLead = 16448;
    static final int kMinTwoUnitValueLead = 16384;
    static final int kMinValueLead = 64;
    static final int kNodeTypeMask = 63;
    static final int kThreeUnitDeltaLead = 65535;
    static final int kThreeUnitNodeValueLead = 32704;
    static final int kThreeUnitValueLead = 32767;
    static final int kValueIsFinal = 32768;
    private static BytesTrie.Result[] valueResults_;
    private CharSequence chars_;
    private int pos_;
    private int remainingMatchLength_ = -1;
    private int root_;

    public static final class State {
        private CharSequence chars;
        private int pos;
        private int remainingMatchLength;
        private int root;
    }

    public CharsTrie(CharSequence trieChars, int offset) {
        this.chars_ = trieChars;
        this.root_ = offset;
        this.pos_ = offset;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public CharsTrie reset() {
        this.pos_ = this.root_;
        this.remainingMatchLength_ = -1;
        return this;
    }

    public CharsTrie saveState(State state) {
        state.chars = this.chars_;
        state.root = this.root_;
        state.pos = this.pos_;
        state.remainingMatchLength = this.remainingMatchLength_;
        return this;
    }

    public CharsTrie resetToState(State state) {
        if (this.chars_ == state.chars && this.chars_ != null && this.root_ == state.root) {
            this.pos_ = state.pos;
            this.remainingMatchLength_ = state.remainingMatchLength;
            return this;
        }
        throw new IllegalArgumentException("incompatible trie state");
    }

    public BytesTrie.Result current() {
        int node;
        int pos = this.pos_;
        if (pos < 0) {
            return BytesTrie.Result.NO_MATCH;
        }
        return (this.remainingMatchLength_ >= 0 || (node = this.chars_.charAt(pos)) < 64) ? BytesTrie.Result.NO_VALUE : valueResults_[node >> 15];
    }

    public BytesTrie.Result first(int inUnit) {
        this.remainingMatchLength_ = -1;
        return nextImpl(this.root_, inUnit);
    }

    public BytesTrie.Result firstForCodePoint(int cp) {
        if (cp <= 65535) {
            return first(cp);
        }
        if (first(UTF16.getLeadSurrogate(cp)).hasNext()) {
            return next(UTF16.getTrailSurrogate(cp));
        }
        return BytesTrie.Result.NO_MATCH;
    }

    public BytesTrie.Result next(int inUnit) {
        int node;
        int pos = this.pos_;
        if (pos < 0) {
            return BytesTrie.Result.NO_MATCH;
        }
        int length = this.remainingMatchLength_;
        if (length >= 0) {
            int pos2 = pos + 1;
            if (inUnit == this.chars_.charAt(pos)) {
                int length2 = length - 1;
                this.remainingMatchLength_ = length2;
                this.pos_ = pos2;
                return (length2 >= 0 || (node = this.chars_.charAt(pos2)) < 64) ? BytesTrie.Result.NO_VALUE : valueResults_[node >> 15];
            }
            stop();
            return BytesTrie.Result.NO_MATCH;
        }
        return nextImpl(pos, inUnit);
    }

    public BytesTrie.Result nextForCodePoint(int cp) {
        if (cp <= 65535) {
            return next(cp);
        }
        if (next(UTF16.getLeadSurrogate(cp)).hasNext()) {
            return next(UTF16.getTrailSurrogate(cp));
        }
        return BytesTrie.Result.NO_MATCH;
    }

    public BytesTrie.Result next(CharSequence s, int sIndex, int sLimit) {
        int node;
        if (sIndex >= sLimit) {
            return current();
        }
        int pos = this.pos_;
        if (pos < 0) {
            return BytesTrie.Result.NO_MATCH;
        }
        int length = this.remainingMatchLength_;
        int pos2 = pos;
        int sIndex2 = sIndex;
        while (sIndex2 != sLimit) {
            int sIndex3 = sIndex2 + 1;
            char inUnit = s.charAt(sIndex2);
            if (length < 0) {
                this.remainingMatchLength_ = length;
                int pos3 = pos2 + 1;
                int node2 = this.chars_.charAt(pos2);
                while (true) {
                    sIndex2 = sIndex3;
                    if (node2 < 48) {
                        BytesTrie.Result result = branchNext(pos3, node2, inUnit);
                        if (result == BytesTrie.Result.NO_MATCH) {
                            return BytesTrie.Result.NO_MATCH;
                        }
                        if (sIndex2 == sLimit) {
                            return result;
                        }
                        if (result == BytesTrie.Result.FINAL_VALUE) {
                            stop();
                            return BytesTrie.Result.NO_MATCH;
                        }
                        sIndex3 = sIndex2 + 1;
                        inUnit = s.charAt(sIndex2);
                        int pos4 = this.pos_;
                        node2 = this.chars_.charAt(pos4);
                        pos3 = pos4 + 1;
                    } else if (node2 < 64) {
                        int length2 = node2 - 48;
                        if (inUnit != this.chars_.charAt(pos3)) {
                            stop();
                            return BytesTrie.Result.NO_MATCH;
                        }
                        length = length2 - 1;
                        pos2 = pos3 + 1;
                    } else {
                        if ((32768 & node2) != 0) {
                            stop();
                            return BytesTrie.Result.NO_MATCH;
                        }
                        pos3 = skipNodeValue(pos3, node2);
                        node2 &= 63;
                        sIndex3 = sIndex2;
                    }
                }
            } else {
                if (inUnit != this.chars_.charAt(pos2)) {
                    stop();
                    return BytesTrie.Result.NO_MATCH;
                }
                length--;
                pos2++;
                sIndex2 = sIndex3;
            }
        }
        this.remainingMatchLength_ = length;
        this.pos_ = pos2;
        return (length >= 0 || (node = this.chars_.charAt(pos2)) < 64) ? BytesTrie.Result.NO_VALUE : valueResults_[node >> 15];
    }

    public int getValue() {
        int pos = this.pos_;
        int pos2 = pos + 1;
        int leadUnit = this.chars_.charAt(pos);
        if (!f106assertionsDisabled) {
            if (!(leadUnit >= 64)) {
                throw new AssertionError();
            }
        }
        return (32768 & leadUnit) != 0 ? readValue(this.chars_, pos2, leadUnit & kThreeUnitValueLead) : readNodeValue(this.chars_, pos2, leadUnit);
    }

    public long getUniqueValue() {
        int pos = this.pos_;
        if (pos < 0) {
            return 0L;
        }
        long uniqueValue = findUniqueValue(this.chars_, this.remainingMatchLength_ + pos + 1, 0L);
        return (uniqueValue << 31) >> 31;
    }

    public int getNextChars(Appendable out) {
        int pos;
        int pos2 = this.pos_;
        if (pos2 < 0) {
            return 0;
        }
        if (this.remainingMatchLength_ >= 0) {
            append(out, this.chars_.charAt(pos2));
            return 1;
        }
        int pos3 = pos2 + 1;
        int node = this.chars_.charAt(pos2);
        if (node >= 64) {
            if ((32768 & node) != 0) {
                return 0;
            }
            int pos4 = skipNodeValue(pos3, node);
            node &= 63;
            pos3 = pos4;
        }
        if (node < 48) {
            if (node == 0) {
                pos = pos3 + 1;
                node = this.chars_.charAt(pos3);
            } else {
                pos = pos3;
            }
            int node2 = node + 1;
            getNextBranchChars(this.chars_, pos, node2, out);
            return node2;
        }
        append(out, this.chars_.charAt(pos3));
        return 1;
    }

    @Override
    public java.util.Iterator<Entry> iterator2() {
        return new Iterator(this.chars_, this.pos_, this.remainingMatchLength_, 0, null);
    }

    public Iterator iterator(int maxStringLength) {
        return new Iterator(this.chars_, this.pos_, this.remainingMatchLength_, maxStringLength, null);
    }

    public static Iterator iterator(CharSequence trieChars, int offset, int maxStringLength) {
        return new Iterator(trieChars, offset, -1, maxStringLength, null);
    }

    public static final class Entry {
        public CharSequence chars;
        public int value;

        Entry(Entry entry) {
            this();
        }

        private Entry() {
        }
    }

    public static final class Iterator implements java.util.Iterator<Entry> {
        private CharSequence chars_;
        private Entry entry_;
        private int initialPos_;
        private int initialRemainingMatchLength_;
        private int maxLength_;
        private int pos_;
        private int remainingMatchLength_;
        private boolean skipValue_;
        private ArrayList<Long> stack_;
        private StringBuilder str_;

        Iterator(CharSequence trieChars, int offset, int remainingMatchLength, int maxStringLength, Iterator iterator) {
            this(trieChars, offset, remainingMatchLength, maxStringLength);
        }

        private Iterator(CharSequence trieChars, int offset, int remainingMatchLength, int maxStringLength) {
            this.str_ = new StringBuilder();
            this.entry_ = new Entry(null);
            this.stack_ = new ArrayList<>();
            this.chars_ = trieChars;
            this.initialPos_ = offset;
            this.pos_ = offset;
            this.initialRemainingMatchLength_ = remainingMatchLength;
            this.remainingMatchLength_ = remainingMatchLength;
            this.maxLength_ = maxStringLength;
            int length = this.remainingMatchLength_;
            if (length < 0) {
                return;
            }
            int length2 = length + 1;
            if (this.maxLength_ > 0 && length2 > this.maxLength_) {
                length2 = this.maxLength_;
            }
            this.str_.append(this.chars_, this.pos_, this.pos_ + length2);
            this.pos_ += length2;
            this.remainingMatchLength_ -= length2;
        }

        public Iterator reset() {
            this.pos_ = this.initialPos_;
            this.remainingMatchLength_ = this.initialRemainingMatchLength_;
            this.skipValue_ = false;
            int length = this.remainingMatchLength_ + 1;
            if (this.maxLength_ > 0 && length > this.maxLength_) {
                length = this.maxLength_;
            }
            this.str_.setLength(length);
            this.pos_ += length;
            this.remainingMatchLength_ -= length;
            this.stack_.clear();
            return this;
        }

        @Override
        public boolean hasNext() {
            return this.pos_ >= 0 || !this.stack_.isEmpty();
        }

        @Override
        public Entry next() {
            int pos;
            int pos2 = this.pos_;
            if (pos2 < 0) {
                if (this.stack_.isEmpty()) {
                    throw new NoSuchElementException();
                }
                long top = this.stack_.remove(this.stack_.size() - 1).longValue();
                int length = (int) top;
                int pos3 = (int) (top >> 32);
                this.str_.setLength(65535 & length);
                int length2 = length >>> 16;
                if (length2 > 1) {
                    pos2 = branchNext(pos3, length2);
                    if (pos2 < 0) {
                        return this.entry_;
                    }
                } else {
                    this.str_.append(this.chars_.charAt(pos3));
                    pos2 = pos3 + 1;
                }
            }
            if (this.remainingMatchLength_ >= 0) {
                return truncateAndStop();
            }
            while (true) {
                int pos4 = pos2 + 1;
                int node = this.chars_.charAt(pos2);
                if (node < 64) {
                    pos = pos4;
                } else if (this.skipValue_) {
                    pos = CharsTrie.skipNodeValue(pos4, node);
                    node &= 63;
                    this.skipValue_ = false;
                } else {
                    boolean isFinal = (32768 & node) != 0;
                    if (isFinal) {
                        this.entry_.value = CharsTrie.readValue(this.chars_, pos4, node & CharsTrie.kThreeUnitValueLead);
                    } else {
                        this.entry_.value = CharsTrie.readNodeValue(this.chars_, pos4, node);
                    }
                    if (isFinal || (this.maxLength_ > 0 && this.str_.length() == this.maxLength_)) {
                        this.pos_ = -1;
                    } else {
                        this.pos_ = pos4 - 1;
                        this.skipValue_ = true;
                    }
                    this.entry_.chars = this.str_;
                    return this.entry_;
                }
                if (this.maxLength_ > 0 && this.str_.length() == this.maxLength_) {
                    return truncateAndStop();
                }
                if (node < 48) {
                    if (node == 0) {
                        node = this.chars_.charAt(pos);
                        pos++;
                    }
                    pos2 = branchNext(pos, node + 1);
                    if (pos2 < 0) {
                        return this.entry_;
                    }
                } else {
                    int length3 = (node - 48) + 1;
                    if (this.maxLength_ > 0 && this.str_.length() + length3 > this.maxLength_) {
                        this.str_.append(this.chars_, pos, (this.maxLength_ + pos) - this.str_.length());
                        return truncateAndStop();
                    }
                    this.str_.append(this.chars_, pos, pos + length3);
                    pos2 = pos + length3;
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Entry truncateAndStop() {
            this.pos_ = -1;
            this.entry_.chars = this.str_;
            this.entry_.value = -1;
            return this.entry_;
        }

        private int branchNext(int pos, int length) {
            int pos2;
            while (true) {
                pos2 = pos;
                if (length <= 5) {
                    break;
                }
                int pos3 = pos2 + 1;
                this.stack_.add(Long.valueOf((((long) CharsTrie.skipDelta(this.chars_, pos3)) << 32) | ((long) ((length - (length >> 1)) << 16)) | ((long) this.str_.length())));
                length >>= 1;
                pos = CharsTrie.jumpByDelta(this.chars_, pos3);
            }
            int pos4 = pos2 + 1;
            char trieUnit = this.chars_.charAt(pos2);
            int pos5 = pos4 + 1;
            int node = this.chars_.charAt(pos4);
            boolean isFinal = (32768 & node) != 0;
            CharSequence charSequence = this.chars_;
            int node2 = node & CharsTrie.kThreeUnitValueLead;
            int value = CharsTrie.readValue(charSequence, pos5, node2);
            int pos6 = CharsTrie.skipValue(pos5, node2);
            this.stack_.add(Long.valueOf((((long) pos6) << 32) | ((long) ((length - 1) << 16)) | ((long) this.str_.length())));
            this.str_.append(trieUnit);
            if (isFinal) {
                this.pos_ = -1;
                this.entry_.chars = this.str_;
                this.entry_.value = value;
                return -1;
            }
            return pos6 + value;
        }
    }

    private void stop() {
        this.pos_ = -1;
    }

    private static int readValue(CharSequence chars, int pos, int leadUnit) {
        if (leadUnit < 16384) {
            return leadUnit;
        }
        if (leadUnit < kThreeUnitValueLead) {
            int value = ((leadUnit - 16384) << 16) | chars.charAt(pos);
            return value;
        }
        int value2 = (chars.charAt(pos) << 16) | chars.charAt(pos + 1);
        return value2;
    }

    private static int skipValue(int pos, int leadUnit) {
        if (leadUnit >= 16384) {
            if (leadUnit < kThreeUnitValueLead) {
                return pos + 1;
            }
            return pos + 2;
        }
        return pos;
    }

    private static int skipValue(CharSequence chars, int pos) {
        int leadUnit = chars.charAt(pos);
        return skipValue(pos + 1, leadUnit & kThreeUnitValueLead);
    }

    private static int readNodeValue(CharSequence chars, int pos, int leadUnit) {
        boolean z = false;
        if (!f106assertionsDisabled) {
            if (64 <= leadUnit && leadUnit < 32768) {
                z = true;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        if (leadUnit < kMinTwoUnitNodeValueLead) {
            int value = (leadUnit >> 6) - 1;
            return value;
        }
        if (leadUnit < kThreeUnitNodeValueLead) {
            int value2 = (((leadUnit & kThreeUnitNodeValueLead) - 16448) << 10) | chars.charAt(pos);
            return value2;
        }
        int value3 = (chars.charAt(pos) << 16) | chars.charAt(pos + 1);
        return value3;
    }

    private static int skipNodeValue(int pos, int leadUnit) {
        boolean z = false;
        if (!f106assertionsDisabled) {
            if (64 <= leadUnit && leadUnit < 32768) {
                z = true;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        if (leadUnit >= kMinTwoUnitNodeValueLead) {
            if (leadUnit < kThreeUnitNodeValueLead) {
                return pos + 1;
            }
            return pos + 2;
        }
        return pos;
    }

    private static int jumpByDelta(CharSequence chars, int pos) {
        int pos2;
        int pos3 = pos + 1;
        int delta = chars.charAt(pos);
        if (delta < kMinTwoUnitDeltaLead) {
            pos2 = pos3;
        } else if (delta == 65535) {
            delta = (chars.charAt(pos3) << 16) | chars.charAt(pos3 + 1);
            pos2 = pos3 + 2;
        } else {
            pos2 = pos3 + 1;
            delta = ((delta - kMinTwoUnitDeltaLead) << 16) | chars.charAt(pos3);
        }
        return pos2 + delta;
    }

    private static int skipDelta(CharSequence chars, int pos) {
        int pos2 = pos + 1;
        int delta = chars.charAt(pos);
        if (delta < kMinTwoUnitDeltaLead) {
            return pos2;
        }
        if (delta == 65535) {
            return pos2 + 2;
        }
        return pos2 + 1;
    }

    static {
        f106assertionsDisabled = !CharsTrie.class.desiredAssertionStatus();
        valueResults_ = new BytesTrie.Result[]{BytesTrie.Result.INTERMEDIATE_VALUE, BytesTrie.Result.FINAL_VALUE};
    }

    private BytesTrie.Result branchNext(int pos, int length, int inUnit) {
        int pos2;
        int delta;
        int pos3;
        BytesTrie.Result result;
        if (length == 0) {
            length = this.chars_.charAt(pos);
            pos++;
        }
        int length2 = length + 1;
        while (true) {
            pos2 = pos;
            if (length2 <= 5) {
                break;
            }
            int pos4 = pos2 + 1;
            if (inUnit < this.chars_.charAt(pos2)) {
                length2 >>= 1;
                pos = jumpByDelta(this.chars_, pos4);
            } else {
                length2 -= length2 >> 1;
                pos = skipDelta(this.chars_, pos4);
            }
        }
        int pos5 = pos2;
        do {
            int pos6 = pos5 + 1;
            if (inUnit == this.chars_.charAt(pos5)) {
                int node = this.chars_.charAt(pos6);
                if ((32768 & node) != 0) {
                    result = BytesTrie.Result.FINAL_VALUE;
                    pos3 = pos6;
                } else {
                    int pos7 = pos6 + 1;
                    if (node < 16384) {
                        delta = node;
                    } else if (node < kThreeUnitValueLead) {
                        delta = ((node - 16384) << 16) | this.chars_.charAt(pos7);
                        pos7++;
                    } else {
                        delta = (this.chars_.charAt(pos7) << 16) | this.chars_.charAt(pos7 + 1);
                        pos7 += 2;
                    }
                    pos3 = pos7 + delta;
                    int node2 = this.chars_.charAt(pos3);
                    result = node2 >= 64 ? valueResults_[node2 >> 15] : BytesTrie.Result.NO_VALUE;
                }
                this.pos_ = pos3;
                return result;
            }
            length2--;
            pos5 = skipValue(this.chars_, pos6);
        } while (length2 > 1);
        int pos8 = pos5 + 1;
        if (inUnit == this.chars_.charAt(pos5)) {
            this.pos_ = pos8;
            int node3 = this.chars_.charAt(pos8);
            return node3 >= 64 ? valueResults_[node3 >> 15] : BytesTrie.Result.NO_VALUE;
        }
        stop();
        return BytesTrie.Result.NO_MATCH;
    }

    private BytesTrie.Result nextImpl(int pos, int inUnit) {
        int node;
        int pos2 = pos + 1;
        int node2 = this.chars_.charAt(pos);
        while (node2 >= 48) {
            if (node2 < 64) {
                int length = node2 - 48;
                int pos3 = pos2 + 1;
                if (inUnit == this.chars_.charAt(pos2)) {
                    int length2 = length - 1;
                    this.remainingMatchLength_ = length2;
                    this.pos_ = pos3;
                    return (length2 >= 0 || (node = this.chars_.charAt(pos3)) < 64) ? BytesTrie.Result.NO_VALUE : valueResults_[node >> 15];
                }
            } else if ((32768 & node2) == 0) {
                int pos4 = skipNodeValue(pos2, node2);
                node2 &= 63;
                pos2 = pos4;
            }
            stop();
            return BytesTrie.Result.NO_MATCH;
        }
        return branchNext(pos2, node2, inUnit);
    }

    private static long findUniqueValueFromBranch(CharSequence chars, int pos, int length, long uniqueValue) {
        while (length > 5) {
            int pos2 = pos + 1;
            uniqueValue = findUniqueValueFromBranch(chars, jumpByDelta(chars, pos2), length >> 1, uniqueValue);
            if (uniqueValue == 0) {
                return 0L;
            }
            length -= length >> 1;
            pos = skipDelta(chars, pos2);
        }
        do {
            int pos3 = pos + 1;
            int pos4 = pos3 + 1;
            int node = chars.charAt(pos3);
            boolean isFinal = (32768 & node) != 0;
            int node2 = node & kThreeUnitValueLead;
            int value = readValue(chars, pos4, node2);
            pos = skipValue(pos4, node2);
            if (!isFinal) {
                uniqueValue = findUniqueValue(chars, pos + value, uniqueValue);
                if (uniqueValue == 0) {
                    return 0L;
                }
            } else if (uniqueValue != 0) {
                if (value != ((int) (uniqueValue >> 1))) {
                    return 0L;
                }
            } else {
                uniqueValue = (((long) value) << 1) | 1;
            }
            length--;
        } while (length > 1);
        return (((long) (pos + 1)) << 33) | (8589934591L & uniqueValue);
    }

    private static long findUniqueValue(CharSequence chars, int pos, long uniqueValue) {
        int value;
        int pos2;
        int pos3;
        int pos4 = pos + 1;
        int node = chars.charAt(pos);
        while (true) {
            if (node < 48) {
                if (node == 0) {
                    pos3 = pos4 + 1;
                    node = chars.charAt(pos4);
                } else {
                    pos3 = pos4;
                }
                uniqueValue = findUniqueValueFromBranch(chars, pos3, node + 1, uniqueValue);
                if (uniqueValue == 0) {
                    return 0L;
                }
                int pos5 = (int) (uniqueValue >>> 33);
                node = chars.charAt(pos5);
                pos2 = pos5 + 1;
            } else if (node < 64) {
                int pos6 = pos4 + (node - 48) + 1;
                node = chars.charAt(pos6);
                pos2 = pos6 + 1;
            } else {
                boolean isFinal = (32768 & node) != 0;
                if (isFinal) {
                    value = readValue(chars, pos4, node & kThreeUnitValueLead);
                } else {
                    value = readNodeValue(chars, pos4, node);
                }
                if (uniqueValue != 0) {
                    if (value != ((int) (uniqueValue >> 1))) {
                        return 0L;
                    }
                } else {
                    uniqueValue = (((long) value) << 1) | 1;
                }
                if (isFinal) {
                    return uniqueValue;
                }
                pos2 = skipNodeValue(pos4, node);
                node &= 63;
            }
            pos4 = pos2;
        }
    }

    private static void getNextBranchChars(CharSequence chars, int pos, int length, Appendable out) {
        while (length > 5) {
            int pos2 = pos + 1;
            getNextBranchChars(chars, jumpByDelta(chars, pos2), length >> 1, out);
            length -= length >> 1;
            pos = skipDelta(chars, pos2);
        }
        do {
            append(out, chars.charAt(pos));
            pos = skipValue(chars, pos + 1);
            length--;
        } while (length > 1);
        append(out, chars.charAt(pos));
    }

    private static void append(Appendable out, int c) {
        try {
            out.append((char) c);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
