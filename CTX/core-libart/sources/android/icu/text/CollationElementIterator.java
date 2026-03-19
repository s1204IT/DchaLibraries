package android.icu.text;

import android.icu.impl.CharacterIteratorWrapper;
import android.icu.impl.coll.Collation;
import android.icu.impl.coll.CollationData;
import android.icu.impl.coll.CollationIterator;
import android.icu.impl.coll.CollationSettings;
import android.icu.impl.coll.ContractionsAndExpansions;
import android.icu.impl.coll.FCDIterCollationIterator;
import android.icu.impl.coll.FCDUTF16CollationIterator;
import android.icu.impl.coll.IterCollationIterator;
import android.icu.impl.coll.UTF16CollationIterator;
import android.icu.impl.coll.UVector32;
import java.text.CharacterIterator;
import java.util.HashMap;
import java.util.Map;

public final class CollationElementIterator {
    static final boolean $assertionsDisabled = false;
    public static final int IGNORABLE = 0;
    public static final int NULLORDER = -1;
    private byte dir_;
    private CollationIterator iter_;
    private UVector32 offsets_;
    private int otherHalf_;
    private RuleBasedCollator rbc_;
    private String string_;

    public static final int primaryOrder(int i) {
        return (i >>> 16) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
    }

    public static final int secondaryOrder(int i) {
        return (i >>> 8) & 255;
    }

    public static final int tertiaryOrder(int i) {
        return i & 255;
    }

    private static final int getFirstHalf(long j, int i) {
        return (((int) j) & (-65536)) | ((i >> 16) & 65280) | ((i >> 8) & 255);
    }

    private static final int getSecondHalf(long j, int i) {
        return (((int) j) << 16) | ((i >> 8) & 65280) | (i & 63);
    }

    private static final boolean ceNeedsTwoParts(long j) {
        return (j & 281470698455103L) != 0;
    }

    private CollationElementIterator(RuleBasedCollator ruleBasedCollator) {
        this.iter_ = null;
        this.rbc_ = ruleBasedCollator;
        this.otherHalf_ = 0;
        this.dir_ = (byte) 0;
        this.offsets_ = null;
    }

    CollationElementIterator(String str, RuleBasedCollator ruleBasedCollator) {
        this(ruleBasedCollator);
        setText(str);
    }

    CollationElementIterator(CharacterIterator characterIterator, RuleBasedCollator ruleBasedCollator) {
        this(ruleBasedCollator);
        setText(characterIterator);
    }

    CollationElementIterator(UCharacterIterator uCharacterIterator, RuleBasedCollator ruleBasedCollator) {
        this(ruleBasedCollator);
        setText(uCharacterIterator);
    }

    public int getOffset() {
        if (this.dir_ < 0 && this.offsets_ != null && !this.offsets_.isEmpty()) {
            int cEsLength = this.iter_.getCEsLength();
            if (this.otherHalf_ != 0) {
                cEsLength++;
            }
            return this.offsets_.elementAti(cEsLength);
        }
        return this.iter_.getOffset();
    }

    public int next() {
        if (this.dir_ > 1) {
            if (this.otherHalf_ != 0) {
                int i = this.otherHalf_;
                this.otherHalf_ = 0;
                return i;
            }
        } else if (this.dir_ == 1 || this.dir_ == 0) {
            this.dir_ = (byte) 2;
        } else {
            throw new IllegalStateException("Illegal change of direction");
        }
        this.iter_.clearCEsIfNoneRemaining();
        long jNextCE = this.iter_.nextCE();
        if (jNextCE == Collation.NO_CE) {
            return -1;
        }
        long j = jNextCE >>> 32;
        int i2 = (int) jNextCE;
        int firstHalf = getFirstHalf(j, i2);
        int secondHalf = getSecondHalf(j, i2);
        if (secondHalf != 0) {
            this.otherHalf_ = secondHalf | 192;
        }
        return firstHalf;
    }

    public int previous() {
        if (this.dir_ < 0) {
            if (this.otherHalf_ != 0) {
                int i = this.otherHalf_;
                this.otherHalf_ = 0;
                return i;
            }
        } else if (this.dir_ == 0) {
            this.iter_.resetToOffset(this.string_.length());
            this.dir_ = (byte) -1;
        } else if (this.dir_ == 1) {
            this.dir_ = (byte) -1;
        } else {
            throw new IllegalStateException("Illegal change of direction");
        }
        if (this.offsets_ == null) {
            this.offsets_ = new UVector32();
        }
        int offset = this.iter_.getCEsLength() == 0 ? this.iter_.getOffset() : 0;
        long jPreviousCE = this.iter_.previousCE(this.offsets_);
        if (jPreviousCE == Collation.NO_CE) {
            return -1;
        }
        long j = jPreviousCE >>> 32;
        int i2 = (int) jPreviousCE;
        int firstHalf = getFirstHalf(j, i2);
        int secondHalf = getSecondHalf(j, i2);
        if (secondHalf != 0) {
            if (this.offsets_.isEmpty()) {
                this.offsets_.addElement(this.iter_.getOffset());
                this.offsets_.addElement(offset);
            }
            this.otherHalf_ = firstHalf;
            return secondHalf | 192;
        }
        return firstHalf;
    }

    public void reset() {
        this.iter_.resetToOffset(0);
        this.otherHalf_ = 0;
        this.dir_ = (byte) 0;
    }

    public void setOffset(int i) {
        int offset;
        if (i > 0 && i < this.string_.length()) {
            int i2 = i;
            do {
                char cCharAt = this.string_.charAt(i2);
                if (!this.rbc_.isUnsafe(cCharAt) || (Character.isHighSurrogate(cCharAt) && !this.rbc_.isUnsafe(this.string_.codePointAt(i2)))) {
                    break;
                } else {
                    i2--;
                }
            } while (i2 > 0);
            if (i2 < i) {
                do {
                    this.iter_.resetToOffset(i2);
                    do {
                        this.iter_.nextCE();
                        offset = this.iter_.getOffset();
                    } while (offset == i2);
                    if (offset <= i) {
                        i2 = offset;
                    }
                } while (offset < i);
                i = i2;
            }
        }
        this.iter_.resetToOffset(i);
        this.otherHalf_ = 0;
        this.dir_ = (byte) 1;
    }

    public void setText(String str) {
        CollationIterator fCDUTF16CollationIterator;
        this.string_ = str;
        boolean zIsNumeric = ((CollationSettings) this.rbc_.settings.readOnly()).isNumeric();
        if (((CollationSettings) this.rbc_.settings.readOnly()).dontCheckFCD()) {
            fCDUTF16CollationIterator = new UTF16CollationIterator(this.rbc_.data, zIsNumeric, this.string_, 0);
        } else {
            fCDUTF16CollationIterator = new FCDUTF16CollationIterator(this.rbc_.data, zIsNumeric, this.string_, 0);
        }
        this.iter_ = fCDUTF16CollationIterator;
        this.otherHalf_ = 0;
        this.dir_ = (byte) 0;
    }

    public void setText(UCharacterIterator uCharacterIterator) {
        CollationIterator fCDIterCollationIterator;
        this.string_ = uCharacterIterator.getText();
        try {
            UCharacterIterator uCharacterIterator2 = (UCharacterIterator) uCharacterIterator.clone();
            uCharacterIterator2.setToStart();
            boolean zIsNumeric = ((CollationSettings) this.rbc_.settings.readOnly()).isNumeric();
            if (((CollationSettings) this.rbc_.settings.readOnly()).dontCheckFCD()) {
                fCDIterCollationIterator = new IterCollationIterator(this.rbc_.data, zIsNumeric, uCharacterIterator2);
            } else {
                fCDIterCollationIterator = new FCDIterCollationIterator(this.rbc_.data, zIsNumeric, uCharacterIterator2, 0);
            }
            this.iter_ = fCDIterCollationIterator;
            this.otherHalf_ = 0;
            this.dir_ = (byte) 0;
        } catch (CloneNotSupportedException e) {
            setText(uCharacterIterator.getText());
        }
    }

    public void setText(CharacterIterator characterIterator) {
        CollationIterator fCDIterCollationIterator;
        CharacterIteratorWrapper characterIteratorWrapper = new CharacterIteratorWrapper(characterIterator);
        characterIteratorWrapper.setToStart();
        this.string_ = characterIteratorWrapper.getText();
        boolean zIsNumeric = ((CollationSettings) this.rbc_.settings.readOnly()).isNumeric();
        if (((CollationSettings) this.rbc_.settings.readOnly()).dontCheckFCD()) {
            fCDIterCollationIterator = new IterCollationIterator(this.rbc_.data, zIsNumeric, characterIteratorWrapper);
        } else {
            fCDIterCollationIterator = new FCDIterCollationIterator(this.rbc_.data, zIsNumeric, characterIteratorWrapper, 0);
        }
        this.iter_ = fCDIterCollationIterator;
        this.otherHalf_ = 0;
        this.dir_ = (byte) 0;
    }

    private static final class MaxExpSink implements ContractionsAndExpansions.CESink {
        static final boolean $assertionsDisabled = false;
        private Map<Integer, Integer> maxExpansions;

        MaxExpSink(Map<Integer, Integer> map) {
            this.maxExpansions = map;
        }

        @Override
        public void handleCE(long j) {
        }

        @Override
        public void handleExpansion(long[] jArr, int i, int i2) {
            int firstHalf;
            if (i2 <= 1) {
                return;
            }
            int i3 = 0;
            for (int i4 = 0; i4 < i2; i4++) {
                i3 += CollationElementIterator.ceNeedsTwoParts(jArr[i + i4]) ? 2 : 1;
            }
            long j = jArr[(i + i2) - 1];
            long j2 = j >>> 32;
            int i5 = (int) j;
            int secondHalf = CollationElementIterator.getSecondHalf(j2, i5);
            if (secondHalf == 0) {
                firstHalf = CollationElementIterator.getFirstHalf(j2, i5);
            } else {
                firstHalf = secondHalf | 192;
            }
            Integer num = this.maxExpansions.get(Integer.valueOf(firstHalf));
            if (num == null || i3 > num.intValue()) {
                this.maxExpansions.put(Integer.valueOf(firstHalf), Integer.valueOf(i3));
            }
        }
    }

    static final Map<Integer, Integer> computeMaxExpansions(CollationData collationData) {
        HashMap map = new HashMap();
        new ContractionsAndExpansions(null, null, new MaxExpSink(map), true).forData(collationData);
        return map;
    }

    public int getMaxExpansion(int i) {
        return getMaxExpansion(this.rbc_.tailoring.maxExpansions, i);
    }

    static int getMaxExpansion(Map<Integer, Integer> map, int i) {
        Integer num;
        if (i == 0) {
            return 1;
        }
        if (map != null && (num = map.get(Integer.valueOf(i))) != null) {
            return num.intValue();
        }
        if ((i & 192) != 192) {
            return 1;
        }
        return 2;
    }

    private byte normalizeDir() {
        if (this.dir_ == 1) {
            return (byte) 0;
        }
        return this.dir_;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CollationElementIterator)) {
            return false;
        }
        CollationElementIterator collationElementIterator = (CollationElementIterator) obj;
        return this.rbc_.equals(collationElementIterator.rbc_) && this.otherHalf_ == collationElementIterator.otherHalf_ && normalizeDir() == collationElementIterator.normalizeDir() && this.string_.equals(collationElementIterator.string_) && this.iter_.equals(collationElementIterator.iter_);
    }

    @Deprecated
    public int hashCode() {
        return 42;
    }

    @Deprecated
    public RuleBasedCollator getRuleBasedCollator() {
        return this.rbc_;
    }
}
