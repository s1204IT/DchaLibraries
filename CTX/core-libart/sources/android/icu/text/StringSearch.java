package android.icu.text;

import android.icu.impl.coll.Collation;
import android.icu.text.SearchIterator;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import java.text.CharacterIterator;
import java.util.Locale;

public final class StringSearch extends SearchIterator {
    private static int CE_LEVEL2_BASE = 5;
    private static int CE_LEVEL3_BASE = 327680;
    private static final int CE_MATCH = -1;
    private static final int CE_NO_MATCH = 0;
    private static final int CE_SKIP_PATN = 2;
    private static final int CE_SKIP_TARG = 1;
    private static final int INITIAL_ARRAY_SIZE_ = 256;
    private static final int PRIMARYORDERMASK = -65536;
    private static final int SECONDARYORDERMASK = 65280;
    private static final int TERTIARYORDERMASK = 255;
    int ceMask_;
    private RuleBasedCollator collator_;
    private Normalizer2 nfd_;
    private Pattern pattern_;
    private int strength_;
    private CollationElementIterator textIter_;
    private CollationPCE textProcessedIter_;
    private boolean toShift_;
    private CollationElementIterator utilIter_;
    int variableTop_;

    public StringSearch(String str, CharacterIterator characterIterator, RuleBasedCollator ruleBasedCollator, BreakIterator breakIterator) {
        super(characterIterator, breakIterator);
        if (ruleBasedCollator.getNumericCollation()) {
            throw new UnsupportedOperationException("Numeric collation is not supported by StringSearch");
        }
        this.collator_ = ruleBasedCollator;
        this.strength_ = ruleBasedCollator.getStrength();
        this.ceMask_ = getMask(this.strength_);
        this.toShift_ = ruleBasedCollator.isAlternateHandlingShifted();
        this.variableTop_ = ruleBasedCollator.getVariableTop();
        this.nfd_ = Normalizer2.getNFDInstance();
        this.pattern_ = new Pattern(str);
        this.search_.setMatchedLength(0);
        this.search_.matchedIndex_ = -1;
        this.utilIter_ = null;
        this.textIter_ = new CollationElementIterator(characterIterator, ruleBasedCollator);
        this.textProcessedIter_ = null;
        ULocale locale = ruleBasedCollator.getLocale(ULocale.VALID_LOCALE);
        this.search_.internalBreakIter_ = BreakIterator.getCharacterInstance(locale == null ? ULocale.ROOT : locale);
        this.search_.internalBreakIter_.setText((CharacterIterator) characterIterator.clone());
        initialize();
    }

    public StringSearch(String str, CharacterIterator characterIterator, RuleBasedCollator ruleBasedCollator) {
        this(str, characterIterator, ruleBasedCollator, null);
    }

    public StringSearch(String str, CharacterIterator characterIterator, Locale locale) {
        this(str, characterIterator, ULocale.forLocale(locale));
    }

    public StringSearch(String str, CharacterIterator characterIterator, ULocale uLocale) {
        this(str, characterIterator, (RuleBasedCollator) Collator.getInstance(uLocale), null);
    }

    public StringSearch(String str, String str2) {
        this(str, new java.text.StringCharacterIterator(str2), (RuleBasedCollator) Collator.getInstance(), null);
    }

    public RuleBasedCollator getCollator() {
        return this.collator_;
    }

    public void setCollator(RuleBasedCollator ruleBasedCollator) {
        if (ruleBasedCollator == null) {
            throw new IllegalArgumentException("Collator can not be null");
        }
        this.collator_ = ruleBasedCollator;
        this.ceMask_ = getMask(this.collator_.getStrength());
        ULocale locale = ruleBasedCollator.getLocale(ULocale.VALID_LOCALE);
        SearchIterator.Search search = this.search_;
        if (locale == null) {
            locale = ULocale.ROOT;
        }
        search.internalBreakIter_ = BreakIterator.getCharacterInstance(locale);
        this.search_.internalBreakIter_.setText((CharacterIterator) this.search_.text().clone());
        this.toShift_ = ruleBasedCollator.isAlternateHandlingShifted();
        this.variableTop_ = ruleBasedCollator.getVariableTop();
        this.textIter_ = new CollationElementIterator(this.pattern_.text_, ruleBasedCollator);
        this.utilIter_ = new CollationElementIterator(this.pattern_.text_, ruleBasedCollator);
        initialize();
    }

    public String getPattern() {
        return this.pattern_.text_;
    }

    public void setPattern(String str) {
        if (str == null || str.length() <= 0) {
            throw new IllegalArgumentException("Pattern to search for can not be null or of length 0");
        }
        this.pattern_.text_ = str;
        initialize();
    }

    public boolean isCanonical() {
        return this.search_.isCanonicalMatch_;
    }

    public void setCanonical(boolean z) {
        this.search_.isCanonicalMatch_ = z;
    }

    @Override
    public void setTarget(CharacterIterator characterIterator) {
        super.setTarget(characterIterator);
        this.textIter_.setText(characterIterator);
    }

    @Override
    public int getIndex() {
        int offset = this.textIter_.getOffset();
        if (isOutOfBounds(this.search_.beginIndex(), this.search_.endIndex(), offset)) {
            return -1;
        }
        return offset;
    }

    @Override
    public void setIndex(int i) {
        super.setIndex(i);
        this.textIter_.setOffset(i);
    }

    @Override
    public void reset() {
        int strength = this.collator_.getStrength();
        boolean z = (this.strength_ >= 3 || strength < 3) && (this.strength_ < 3 || strength >= 3);
        this.strength_ = this.collator_.getStrength();
        int mask = getMask(this.strength_);
        if (this.ceMask_ != mask) {
            this.ceMask_ = mask;
            z = false;
        }
        boolean zIsAlternateHandlingShifted = this.collator_.isAlternateHandlingShifted();
        if (this.toShift_ != zIsAlternateHandlingShifted) {
            this.toShift_ = zIsAlternateHandlingShifted;
            z = false;
        }
        int variableTop = this.collator_.getVariableTop();
        if (this.variableTop_ != variableTop) {
            this.variableTop_ = variableTop;
            z = false;
        }
        if (!z) {
            initialize();
        }
        this.textIter_.setText(this.search_.text());
        this.search_.setMatchedLength(0);
        this.search_.matchedIndex_ = -1;
        this.search_.isOverlap_ = false;
        this.search_.isCanonicalMatch_ = false;
        this.search_.elementComparisonType_ = SearchIterator.ElementComparisonType.STANDARD_ELEMENT_COMPARISON;
        this.search_.isForwardSearching_ = true;
        this.search_.reset_ = true;
    }

    @Override
    protected int handleNext(int i) {
        if (this.pattern_.CELength_ == 0) {
            this.search_.matchedIndex_ = this.search_.matchedIndex_ == -1 ? getIndex() : this.search_.matchedIndex_ + 1;
            this.search_.setMatchedLength(0);
            this.textIter_.setOffset(this.search_.matchedIndex_);
            if (this.search_.matchedIndex_ == this.search_.endIndex()) {
                this.search_.matchedIndex_ = -1;
            }
            return -1;
        }
        if (this.search_.matchedLength() <= 0) {
            this.search_.matchedIndex_ = i - 1;
        }
        this.textIter_.setOffset(i);
        if (this.search_.isCanonicalMatch_) {
            handleNextCanonical();
        } else {
            handleNextExact();
        }
        if (this.search_.matchedIndex_ == -1) {
            this.textIter_.setOffset(this.search_.endIndex());
        } else {
            this.textIter_.setOffset(this.search_.matchedIndex_);
        }
        return this.search_.matchedIndex_;
    }

    @Override
    protected int handlePrevious(int i) {
        if (this.pattern_.CELength_ == 0) {
            this.search_.matchedIndex_ = this.search_.matchedIndex_ == -1 ? getIndex() : this.search_.matchedIndex_;
            if (this.search_.matchedIndex_ == this.search_.beginIndex()) {
                setMatchNotFound();
            } else {
                SearchIterator.Search search = this.search_;
                search.matchedIndex_--;
                this.textIter_.setOffset(this.search_.matchedIndex_);
                this.search_.setMatchedLength(0);
            }
        } else {
            this.textIter_.setOffset(i);
            if (this.search_.isCanonicalMatch_) {
                handlePreviousCanonical();
            } else {
                handlePreviousExact();
            }
        }
        return this.search_.matchedIndex_;
    }

    private static int getMask(int i) {
        switch (i) {
            case 0:
                return PRIMARYORDERMASK;
            case 1:
                return -256;
            default:
                return -1;
        }
    }

    private int getCE(int i) {
        int i2 = i & this.ceMask_;
        if (this.toShift_) {
            if (this.variableTop_ > i2) {
                if (this.strength_ >= 3) {
                    return i2 & PRIMARYORDERMASK;
                }
                return 0;
            }
            return i2;
        }
        if (this.strength_ >= 3 && i2 == 0) {
            return DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
        }
        return i2;
    }

    private static int[] addToIntArray(int[] iArr, int i, int i2, int i3) {
        int length = iArr.length;
        if (i + 1 == length) {
            int[] iArr2 = new int[length + i3];
            System.arraycopy(iArr, 0, iArr2, 0, i);
            iArr = iArr2;
        }
        iArr[i] = i2;
        return iArr;
    }

    private static long[] addToLongArray(long[] jArr, int i, int i2, long j, int i3) {
        if (i + 1 == i2) {
            long[] jArr2 = new long[i2 + i3];
            System.arraycopy(jArr, 0, jArr2, 0, i);
            jArr = jArr2;
        }
        jArr[i] = j;
        return jArr;
    }

    private int initializePatternCETable() {
        int[] iArrAddToIntArray = new int[256];
        int length = this.pattern_.text_.length();
        CollationElementIterator collationElementIterator = this.utilIter_;
        if (collationElementIterator == null) {
            collationElementIterator = new CollationElementIterator(this.pattern_.text_, this.collator_);
            this.utilIter_ = collationElementIterator;
        } else {
            collationElementIterator.setText(this.pattern_.text_);
        }
        int i = 0;
        int maxExpansion = 0;
        while (true) {
            int next = collationElementIterator.next();
            if (next != -1) {
                int ce = getCE(next);
                if (ce != 0) {
                    iArrAddToIntArray = addToIntArray(iArrAddToIntArray, i, ce, (length - collationElementIterator.getOffset()) + 1);
                    i++;
                }
                maxExpansion += collationElementIterator.getMaxExpansion(next) - 1;
            } else {
                iArrAddToIntArray[i] = 0;
                this.pattern_.CE_ = iArrAddToIntArray;
                this.pattern_.CELength_ = i;
                return maxExpansion;
            }
        }
    }

    private int initializePatternPCETable() {
        long[] jArr = new long[256];
        int length = jArr.length;
        int length2 = this.pattern_.text_.length();
        CollationElementIterator collationElementIterator = this.utilIter_;
        if (collationElementIterator == null) {
            collationElementIterator = new CollationElementIterator(this.pattern_.text_, this.collator_);
            this.utilIter_ = collationElementIterator;
        } else {
            collationElementIterator.setText(this.pattern_.text_);
        }
        CollationElementIterator collationElementIterator2 = collationElementIterator;
        CollationPCE collationPCE = new CollationPCE(collationElementIterator2);
        long[] jArrAddToLongArray = jArr;
        int i = 0;
        while (true) {
            long jNextProcessed = collationPCE.nextProcessed(null);
            if (jNextProcessed != -1) {
                jArrAddToLongArray = addToLongArray(jArrAddToLongArray, i, length, jNextProcessed, (length2 - collationElementIterator2.getOffset()) + 1);
                i++;
            } else {
                jArrAddToLongArray[i] = 0;
                this.pattern_.PCE_ = jArrAddToLongArray;
                this.pattern_.PCELength_ = i;
                return 0;
            }
        }
    }

    private int initializePattern() {
        this.pattern_.PCE_ = null;
        return initializePatternCETable();
    }

    private void initialize() {
        initializePattern();
    }

    @Override
    @Deprecated
    protected void setMatchNotFound() {
        super.setMatchNotFound();
        if (this.search_.isForwardSearching_) {
            this.textIter_.setOffset(this.search_.text().getEndIndex());
        } else {
            this.textIter_.setOffset(0);
        }
    }

    private static final boolean isOutOfBounds(int i, int i2, int i3) {
        return i3 < i || i3 > i2;
    }

    private boolean checkIdentical(int i, int i2) {
        if (this.strength_ != 15) {
            return true;
        }
        String string = getString(this.targetText, i, i2 - i);
        if (Normalizer.quickCheck(string, Normalizer.NFD, 0) == Normalizer.NO) {
            string = Normalizer.decompose(string, false);
        }
        String strDecompose = this.pattern_.text_;
        if (Normalizer.quickCheck(strDecompose, Normalizer.NFD, 0) == Normalizer.NO) {
            strDecompose = Normalizer.decompose(strDecompose, false);
        }
        return string.equals(strDecompose);
    }

    private boolean initTextProcessedIter() {
        if (this.textProcessedIter_ == null) {
            this.textProcessedIter_ = new CollationPCE(this.textIter_);
            return true;
        }
        this.textProcessedIter_.init(this.textIter_);
        return true;
    }

    private int nextBoundaryAfter(int i) {
        BreakIterator breakIteratorBreakIter = this.search_.breakIter();
        if (breakIteratorBreakIter == null) {
            breakIteratorBreakIter = this.search_.internalBreakIter_;
        }
        if (breakIteratorBreakIter != null) {
            return breakIteratorBreakIter.following(i);
        }
        return i;
    }

    private boolean isBreakBoundary(int i) {
        BreakIterator breakIteratorBreakIter = this.search_.breakIter();
        if (breakIteratorBreakIter == null) {
            breakIteratorBreakIter = this.search_.internalBreakIter_;
        }
        return breakIteratorBreakIter != null && breakIteratorBreakIter.isBoundary(i);
    }

    private static int compareCE64s(long j, long j2, SearchIterator.ElementComparisonType elementComparisonType) {
        if (j == j2) {
            return -1;
        }
        if (elementComparisonType == SearchIterator.ElementComparisonType.STANDARD_ELEMENT_COMPARISON) {
            return 0;
        }
        long j3 = j >>> 32;
        long j4 = j2 >>> 32;
        int i = (int) (j3 & Collation.MAX_PRIMARY);
        int i2 = (int) (j4 & Collation.MAX_PRIMARY);
        if (i != i2) {
            if (i == 0) {
                return 1;
            }
            return (i2 == 0 && elementComparisonType == SearchIterator.ElementComparisonType.ANY_BASE_WEIGHT_IS_WILDCARD) ? 2 : 0;
        }
        int i3 = (int) (j3 & 65535);
        int i4 = (int) (j4 & 65535);
        if (i3 != i4) {
            if (i3 == 0) {
                return 1;
            }
            if (i4 == 0 && elementComparisonType == SearchIterator.ElementComparisonType.ANY_BASE_WEIGHT_IS_WILDCARD) {
                return 2;
            }
            if (i4 != CE_LEVEL2_BASE) {
                return (elementComparisonType == SearchIterator.ElementComparisonType.ANY_BASE_WEIGHT_IS_WILDCARD && i3 == CE_LEVEL2_BASE) ? -1 : 0;
            }
            return -1;
        }
        int i5 = (int) (j & Collation.MAX_PRIMARY);
        int i6 = (int) (j2 & Collation.MAX_PRIMARY);
        if (i5 == i6 || i6 == CE_LEVEL3_BASE) {
            return -1;
        }
        return (elementComparisonType == SearchIterator.ElementComparisonType.ANY_BASE_WEIGHT_IS_WILDCARD && i5 == CE_LEVEL3_BASE) ? -1 : 0;
    }

    private static class Match {
        int limit_;
        int start_;

        private Match() {
            this.start_ = -1;
            this.limit_ = -1;
        }
    }

    private boolean search(int i, Match match) {
        boolean z;
        int i2;
        int i3;
        CEBuffer cEBuffer;
        int i4;
        int i5;
        CEI cei;
        int i6;
        int i7;
        boolean z2;
        if (this.pattern_.CELength_ == 0 || i < this.search_.beginIndex() || i > this.search_.endIndex()) {
            throw new IllegalArgumentException("search(" + i + ", m) - expected position to be between " + this.search_.beginIndex() + " and " + this.search_.endIndex());
        }
        if (this.pattern_.PCE_ == null) {
            initializePatternPCETable();
        }
        this.textIter_.setOffset(i);
        CEBuffer cEBuffer2 = new CEBuffer(this);
        CEI cei2 = null;
        int i8 = 0;
        int i9 = -1;
        int i10 = -1;
        while (true) {
            CEI cei3 = cEBuffer2.get(i8);
            if (cei3 == null) {
                throw new ICUException("CEBuffer.get(" + i8 + ") returned null.");
            }
            CEI cei4 = cei2;
            int i11 = 0;
            int i12 = 0;
            long j = 0;
            while (true) {
                if (i11 >= this.pattern_.PCELength_) {
                    z = true;
                    cei2 = cei4;
                    break;
                }
                j = this.pattern_.PCE_[i11];
                cei4 = cEBuffer2.get(i8 + i11 + i12);
                int iCompareCE64s = compareCE64s(cei4.ce_, j, this.search_.elementComparisonType_);
                if (iCompareCE64s == 0) {
                    cei2 = cei4;
                    z = false;
                    break;
                }
                if (iCompareCE64s > 0) {
                    if (iCompareCE64s == 1) {
                        i11--;
                        i12++;
                    } else {
                        i12--;
                    }
                }
                i11++;
            }
            int i13 = i12 + this.pattern_.PCELength_;
            if (z) {
                i2 = i9;
                i3 = i10;
            } else {
                if (cei2 != null) {
                    i2 = i9;
                    i3 = i10;
                    if (cei2.ce_ != -1) {
                    }
                    i8++;
                    cEBuffer2 = cEBuffer;
                } else {
                    i2 = i9;
                    i3 = i10;
                }
                cEBuffer = cEBuffer2;
                i9 = i2;
                i10 = i3;
                i8++;
                cEBuffer2 = cEBuffer;
            }
            if (!z) {
                i4 = i2;
                break;
            }
            int i14 = i8 + i13;
            CEI cei5 = cEBuffer2.get(i14 - 1);
            i4 = cei3.lowIndex_;
            int iNextBoundaryAfter = cei5.lowIndex_;
            if (this.search_.elementComparisonType_ == SearchIterator.ElementComparisonType.STANDARD_ELEMENT_COMPARISON) {
                cei = cEBuffer2.get(i14);
                i7 = cei.lowIndex_;
                if (cei.lowIndex_ != cei.highIndex_ || cei.ce_ == -1) {
                    cEBuffer = cEBuffer2;
                    if (!isBreakBoundary(i4)) {
                        z = false;
                    }
                    if (i4 == cei3.highIndex_) {
                        z = false;
                    }
                    z2 = this.breakIterator != null && ((cei.ce_ >>> 32) & Collation.MAX_PRIMARY) != 0 && i7 >= cei5.highIndex_ && cei.highIndex_ > i7 && (this.nfd_.hasBoundaryBefore(codePointAt(this.targetText, i7)) || this.nfd_.hasBoundaryAfter(codePointBefore(this.targetText, i7)));
                    if (iNextBoundaryAfter < i7 || ((iNextBoundaryAfter != cei5.highIndex_ || !isBreakBoundary(iNextBoundaryAfter)) && ((iNextBoundaryAfter = nextBoundaryAfter(iNextBoundaryAfter)) < cei5.highIndex_ || (z2 && iNextBoundaryAfter >= i7)))) {
                        iNextBoundaryAfter = i7;
                    }
                    if (!z2) {
                        if (iNextBoundaryAfter > i7) {
                            z = false;
                        }
                        if (!isBreakBoundary(iNextBoundaryAfter)) {
                            z = false;
                        }
                    }
                    if (!checkIdentical(i4, iNextBoundaryAfter)) {
                        z = false;
                    }
                    if (z) {
                        i3 = iNextBoundaryAfter;
                        break;
                    }
                    i10 = iNextBoundaryAfter;
                    i9 = i4;
                    i8++;
                    cEBuffer2 = cEBuffer;
                } else {
                    cEBuffer = cEBuffer2;
                    z = false;
                    if (!isBreakBoundary(i4)) {
                    }
                    if (i4 == cei3.highIndex_) {
                    }
                    if (this.breakIterator != null) {
                    }
                    if (iNextBoundaryAfter < i7) {
                    }
                    iNextBoundaryAfter = i7;
                    if (!z2) {
                    }
                    if (!checkIdentical(i4, iNextBoundaryAfter)) {
                    }
                    if (z) {
                    }
                }
            } else {
                while (true) {
                    cei = cEBuffer2.get(i8 + i13);
                    i6 = cei.lowIndex_;
                    if (cei.ce_ == -1) {
                        cEBuffer = cEBuffer2;
                        break;
                    }
                    if (((cei.ce_ >>> 32) & Collation.MAX_PRIMARY) == 0) {
                        cEBuffer = cEBuffer2;
                        int iCompareCE64s2 = compareCE64s(cei.ce_, j, this.search_.elementComparisonType_);
                        if (iCompareCE64s2 == 0 || iCompareCE64s2 == 2) {
                            break;
                        }
                        i13++;
                        cEBuffer2 = cEBuffer;
                    } else {
                        cEBuffer = cEBuffer2;
                    }
                }
                i7 = i6;
                if (!isBreakBoundary(i4)) {
                }
                if (i4 == cei3.highIndex_) {
                }
                if (this.breakIterator != null) {
                }
                if (iNextBoundaryAfter < i7) {
                }
                iNextBoundaryAfter = i7;
                if (!z2) {
                }
                if (!checkIdentical(i4, iNextBoundaryAfter)) {
                }
                if (z) {
                }
            }
        }
        if (z) {
            i5 = i3;
        } else {
            i5 = -1;
            i4 = -1;
        }
        if (match != null) {
            match.start_ = i4;
            match.limit_ = i5;
        }
        return z;
    }

    private static int codePointAt(CharacterIterator characterIterator, int i) {
        int index = characterIterator.getIndex();
        char index2 = characterIterator.setIndex(i);
        boolean zIsHighSurrogate = Character.isHighSurrogate(index2);
        int codePoint = index2;
        if (zIsHighSurrogate) {
            char next = characterIterator.next();
            codePoint = index2;
            if (Character.isLowSurrogate(next)) {
                codePoint = Character.toCodePoint(index2, next);
            }
        }
        characterIterator.setIndex(index);
        return codePoint;
    }

    private static int codePointBefore(CharacterIterator characterIterator, int i) {
        int index = characterIterator.getIndex();
        characterIterator.setIndex(i);
        char cPrevious = characterIterator.previous();
        boolean zIsLowSurrogate = Character.isLowSurrogate(cPrevious);
        int codePoint = cPrevious;
        if (zIsLowSurrogate) {
            char cPrevious2 = characterIterator.previous();
            codePoint = cPrevious;
            if (Character.isHighSurrogate(cPrevious2)) {
                codePoint = Character.toCodePoint(cPrevious2, cPrevious);
            }
        }
        characterIterator.setIndex(index);
        return codePoint;
    }

    private boolean searchBackwards(int i, Match match) {
        int i2;
        int i3;
        boolean z;
        int i4;
        CEI cei;
        boolean z2;
        int i5;
        int iNextBoundaryAfter;
        if (this.pattern_.CELength_ == 0 || i < this.search_.beginIndex() || i > this.search_.endIndex()) {
            throw new IllegalArgumentException("searchBackwards(" + i + ", m) - expected position to be between " + this.search_.beginIndex() + " and " + this.search_.endIndex());
        }
        if (this.pattern_.PCE_ == null) {
            initializePatternPCETable();
        }
        CEBuffer cEBuffer = new CEBuffer(this);
        if (i < this.search_.endIndex()) {
            this.textIter_.setOffset(this.search_.internalBreakIter_.following(i));
            i2 = 0;
            while (cEBuffer.getPrevious(i2).lowIndex_ >= i) {
                i2++;
            }
        } else {
            this.textIter_.setOffset(i);
            i2 = 0;
        }
        CEI cei2 = null;
        int i6 = -1;
        int i7 = -1;
        while (true) {
            CEI previous = cEBuffer.getPrevious(i2);
            if (previous == null) {
                throw new ICUException("CEBuffer.getPrevious(" + i2 + ") returned null.");
            }
            int i8 = this.pattern_.PCELength_ - 1;
            CEI cei3 = cei2;
            int i9 = 0;
            while (true) {
                if (i8 >= 0) {
                    long j = this.pattern_.PCE_[i8];
                    CEI previous2 = cEBuffer.getPrevious((((this.pattern_.PCELength_ + i2) - 1) - i8) + i9);
                    i3 = i6;
                    int iCompareCE64s = compareCE64s(previous2.ce_, j, this.search_.elementComparisonType_);
                    if (iCompareCE64s != 0) {
                        if (iCompareCE64s > 0) {
                            if (iCompareCE64s == 1) {
                                i8++;
                                i9++;
                            } else {
                                i9--;
                            }
                        }
                        i8--;
                        cei3 = previous2;
                        i6 = i3;
                    } else {
                        cei3 = previous2;
                        z = false;
                        break;
                    }
                } else {
                    i3 = i6;
                    z = true;
                    break;
                }
            }
            if (!z) {
                if (cei3 != null) {
                    i4 = i9;
                    if (cei3.ce_ != -1) {
                    }
                    i2++;
                    cei2 = cei;
                }
                cei = cei3;
                i6 = i3;
                i2++;
                cei2 = cei;
            } else {
                i4 = i9;
            }
            if (z) {
                CEI previous3 = cEBuffer.getPrevious(((this.pattern_.PCELength_ + i2) - 1) + i4);
                int i10 = previous3.lowIndex_;
                if (!isBreakBoundary(i10)) {
                    z = false;
                }
                z2 = i10 == previous3.highIndex_ ? false : z;
                int i11 = previous.lowIndex_;
                if (i2 > 0) {
                    CEI previous4 = cEBuffer.getPrevious(i2 - 1);
                    if (previous4.lowIndex_ == previous4.highIndex_) {
                        cei = cei3;
                        if (previous4.ce_ != -1) {
                            z2 = false;
                        }
                    } else {
                        cei = cei3;
                    }
                    int i12 = previous4.lowIndex_;
                    boolean z3 = this.breakIterator == null && ((previous4.ce_ >>> 32) & Collation.MAX_PRIMARY) != 0 && i12 >= previous.highIndex_ && previous4.highIndex_ > i12 && (this.nfd_.hasBoundaryBefore(codePointAt(this.targetText, i12)) || this.nfd_.hasBoundaryAfter(codePointBefore(this.targetText, i12)));
                    if (i11 >= i12 || (iNextBoundaryAfter = nextBoundaryAfter(i11)) < previous.highIndex_ || (z3 && iNextBoundaryAfter >= i12)) {
                        iNextBoundaryAfter = i12;
                    }
                    if (!z3) {
                        if (iNextBoundaryAfter > i12) {
                            z2 = false;
                        }
                        if (!isBreakBoundary(iNextBoundaryAfter)) {
                            z2 = false;
                        }
                    }
                } else {
                    cei = cei3;
                    iNextBoundaryAfter = nextBoundaryAfter(i11);
                    if (iNextBoundaryAfter <= 0 || i <= iNextBoundaryAfter) {
                        iNextBoundaryAfter = i;
                    }
                }
                if (!checkIdentical(i10, iNextBoundaryAfter)) {
                    z2 = false;
                }
                if (!z2) {
                    i6 = i10;
                    i7 = iNextBoundaryAfter;
                    i2++;
                    cei2 = cei;
                } else {
                    i7 = iNextBoundaryAfter;
                    i5 = i10;
                    break;
                }
            } else {
                z2 = z;
                i5 = i3;
                break;
            }
        }
        if (!z2) {
            i5 = -1;
            i7 = -1;
        }
        if (match != null) {
            match.start_ = i5;
            match.limit_ = i7;
        }
        return z2;
    }

    private boolean handleNextExact() {
        return handleNextCommonImpl();
    }

    private boolean handleNextCanonical() {
        return handleNextCommonImpl();
    }

    private boolean handleNextCommonImpl() {
        int offset = this.textIter_.getOffset();
        Match match = new Match();
        if (search(offset, match)) {
            this.search_.matchedIndex_ = match.start_;
            this.search_.setMatchedLength(match.limit_ - match.start_);
            return true;
        }
        setMatchNotFound();
        return false;
    }

    private boolean handlePreviousExact() {
        return handlePreviousCommonImpl();
    }

    private boolean handlePreviousCanonical() {
        return handlePreviousCommonImpl();
    }

    private boolean handlePreviousCommonImpl() {
        int offset;
        if (this.search_.isOverlap_) {
            if (this.search_.matchedIndex_ != -1) {
                offset = (this.search_.matchedIndex_ + this.search_.matchedLength()) - 1;
            } else {
                initializePatternPCETable();
                if (!initTextProcessedIter()) {
                    setMatchNotFound();
                    return false;
                }
                for (int i = 0; i < this.pattern_.PCELength_ - 1 && this.textProcessedIter_.nextProcessed(null) != -1; i++) {
                }
                offset = this.textIter_.getOffset();
            }
        } else {
            offset = this.textIter_.getOffset();
        }
        Match match = new Match();
        if (searchBackwards(offset, match)) {
            this.search_.matchedIndex_ = match.start_;
            this.search_.setMatchedLength(match.limit_ - match.start_);
            return true;
        }
        setMatchNotFound();
        return false;
    }

    private static final String getString(CharacterIterator characterIterator, int i, int i2) {
        StringBuilder sb = new StringBuilder(i2);
        int index = characterIterator.getIndex();
        characterIterator.setIndex(i);
        for (int i3 = 0; i3 < i2; i3++) {
            sb.append(characterIterator.current());
            characterIterator.next();
        }
        characterIterator.setIndex(index);
        return sb.toString();
    }

    private static final class Pattern {
        int[] CE_;
        long[] PCE_;
        String text_;
        int PCELength_ = 0;
        int CELength_ = 0;

        protected Pattern(String str) {
            this.text_ = str;
        }
    }

    private static class CollationPCE {
        private static final int BUFFER_GROW = 8;
        private static final int CONTINUATION_MARKER = 192;
        private static final int DEFAULT_BUFFER_SIZE = 16;
        private static final int PRIMARYORDERMASK = -65536;
        public static final long PROCESSED_NULLORDER = -1;
        private CollationElementIterator cei_;
        private boolean isShifted_;
        private PCEBuffer pceBuffer_ = new PCEBuffer();
        private int strength_;
        private boolean toShift_;
        private int variableTop_;

        public static final class Range {
            int ixHigh_;
            int ixLow_;
        }

        public CollationPCE(CollationElementIterator collationElementIterator) {
            init(collationElementIterator);
        }

        public void init(CollationElementIterator collationElementIterator) {
            this.cei_ = collationElementIterator;
            init(collationElementIterator.getRuleBasedCollator());
        }

        private void init(RuleBasedCollator ruleBasedCollator) {
            this.strength_ = ruleBasedCollator.getStrength();
            this.toShift_ = ruleBasedCollator.isAlternateHandlingShifted();
            this.isShifted_ = false;
            this.variableTop_ = ruleBasedCollator.getVariableTop();
        }

        private long processCE(int i) {
            long jTertiaryOrder;
            long jSecondaryOrder;
            switch (this.strength_) {
                case 0:
                    jTertiaryOrder = 0;
                    jSecondaryOrder = 0;
                    break;
                case 1:
                    jTertiaryOrder = 0;
                    jSecondaryOrder = CollationElementIterator.secondaryOrder(i);
                    break;
                default:
                    jTertiaryOrder = CollationElementIterator.tertiaryOrder(i);
                    jSecondaryOrder = CollationElementIterator.secondaryOrder(i);
                    break;
            }
            long jPrimaryOrder = CollationElementIterator.primaryOrder(i);
            if ((!this.toShift_ || this.variableTop_ <= i || jPrimaryOrder == 0) && !(this.isShifted_ && jPrimaryOrder == 0)) {
                j = this.strength_ >= 3 ? 65535L : 0L;
                this.isShifted_ = false;
                long j = j;
                j = jPrimaryOrder;
                jPrimaryOrder = j;
            } else {
                if (jPrimaryOrder == 0) {
                    return 0L;
                }
                if (this.strength_ < 3) {
                    jPrimaryOrder = 0;
                }
                this.isShifted_ = true;
                jTertiaryOrder = 0;
                jSecondaryOrder = 0;
            }
            return (j << 48) | (jSecondaryOrder << 32) | (jTertiaryOrder << 16) | jPrimaryOrder;
        }

        public long nextProcessed(Range range) {
            int offset;
            int offset2;
            long jProcessCE;
            this.pceBuffer_.reset();
            while (true) {
                offset = this.cei_.getOffset();
                int next = this.cei_.next();
                offset2 = this.cei_.getOffset();
                if (next == -1) {
                    jProcessCE = -1;
                    break;
                }
                jProcessCE = processCE(next);
                if (jProcessCE != 0) {
                    break;
                }
            }
            if (range != null) {
                range.ixLow_ = offset;
                range.ixHigh_ = offset2;
            }
            return jProcessCE;
        }

        public long previousProcessed(Range range) {
            while (this.pceBuffer_.empty()) {
                RCEBuffer rCEBuffer = new RCEBuffer();
                boolean z = false;
                while (true) {
                    int offset = this.cei_.getOffset();
                    int iPrevious = this.cei_.previous();
                    int offset2 = this.cei_.getOffset();
                    if (iPrevious == -1) {
                        if (rCEBuffer.empty()) {
                            z = true;
                        }
                    } else {
                        rCEBuffer.put(iPrevious, offset2, offset);
                        if ((PRIMARYORDERMASK & iPrevious) != 0 && !isContinuation(iPrevious)) {
                            break;
                        }
                    }
                }
                if (z) {
                    break;
                }
                while (!rCEBuffer.empty()) {
                    RCEI rcei = rCEBuffer.get();
                    long jProcessCE = processCE(rcei.ce_);
                    if (jProcessCE != 0) {
                        this.pceBuffer_.put(jProcessCE, rcei.low_, rcei.high_);
                    }
                }
            }
            if (this.pceBuffer_.empty()) {
                if (range != null) {
                    range.ixLow_ = -1;
                    range.ixHigh_ = -1;
                    return -1L;
                }
                return -1L;
            }
            PCEI pcei = this.pceBuffer_.get();
            if (range != null) {
                range.ixLow_ = pcei.low_;
                range.ixHigh_ = pcei.high_;
            }
            return pcei.ce_;
        }

        private static boolean isContinuation(int i) {
            return (i & 192) == 192;
        }

        private static final class PCEI {
            long ce_;
            int high_;
            int low_;

            private PCEI() {
            }
        }

        private static final class PCEBuffer {
            private int bufferIndex_;
            private PCEI[] buffer_;

            private PCEBuffer() {
                this.buffer_ = new PCEI[16];
                this.bufferIndex_ = 0;
            }

            void reset() {
                this.bufferIndex_ = 0;
            }

            boolean empty() {
                return this.bufferIndex_ <= 0;
            }

            void put(long j, int i, int i2) {
                if (this.bufferIndex_ >= this.buffer_.length) {
                    PCEI[] pceiArr = new PCEI[this.buffer_.length + 8];
                    System.arraycopy(this.buffer_, 0, pceiArr, 0, this.buffer_.length);
                    this.buffer_ = pceiArr;
                }
                this.buffer_[this.bufferIndex_] = new PCEI();
                this.buffer_[this.bufferIndex_].ce_ = j;
                this.buffer_[this.bufferIndex_].low_ = i;
                this.buffer_[this.bufferIndex_].high_ = i2;
                this.bufferIndex_++;
            }

            PCEI get() {
                if (this.bufferIndex_ > 0) {
                    PCEI[] pceiArr = this.buffer_;
                    int i = this.bufferIndex_ - 1;
                    this.bufferIndex_ = i;
                    return pceiArr[i];
                }
                return null;
            }
        }

        private static final class RCEI {
            int ce_;
            int high_;
            int low_;

            private RCEI() {
            }
        }

        private static final class RCEBuffer {
            private int bufferIndex_;
            private RCEI[] buffer_;

            private RCEBuffer() {
                this.buffer_ = new RCEI[16];
                this.bufferIndex_ = 0;
            }

            boolean empty() {
                return this.bufferIndex_ <= 0;
            }

            void put(int i, int i2, int i3) {
                if (this.bufferIndex_ >= this.buffer_.length) {
                    RCEI[] rceiArr = new RCEI[this.buffer_.length + 8];
                    System.arraycopy(this.buffer_, 0, rceiArr, 0, this.buffer_.length);
                    this.buffer_ = rceiArr;
                }
                this.buffer_[this.bufferIndex_] = new RCEI();
                this.buffer_[this.bufferIndex_].ce_ = i;
                this.buffer_[this.bufferIndex_].low_ = i2;
                this.buffer_[this.bufferIndex_].high_ = i3;
                this.bufferIndex_++;
            }

            RCEI get() {
                if (this.bufferIndex_ > 0) {
                    RCEI[] rceiArr = this.buffer_;
                    int i = this.bufferIndex_ - 1;
                    this.bufferIndex_ = i;
                    return rceiArr[i];
                }
                return null;
            }
        }
    }

    private static class CEI {
        long ce_;
        int highIndex_;
        int lowIndex_;

        private CEI() {
        }
    }

    private static class CEBuffer {
        static final boolean $assertionsDisabled = false;
        static final int CEBUFFER_EXTRA = 32;
        static final int MAX_TARGET_IGNORABLES_PER_PAT_JAMO_L = 8;
        static final int MAX_TARGET_IGNORABLES_PER_PAT_OTHER = 3;
        int bufSize_;
        CEI[] buf_;
        int firstIx_;
        int limitIx_;
        StringSearch strSearch_;

        CEBuffer(StringSearch stringSearch) {
            String str;
            this.strSearch_ = stringSearch;
            this.bufSize_ = stringSearch.pattern_.PCELength_ + 32;
            if (stringSearch.search_.elementComparisonType_ != SearchIterator.ElementComparisonType.STANDARD_ELEMENT_COMPARISON && (str = stringSearch.pattern_.text_) != null) {
                for (int i = 0; i < str.length(); i++) {
                    if (MIGHT_BE_JAMO_L(str.charAt(i))) {
                        this.bufSize_ += 8;
                    } else {
                        this.bufSize_ += 3;
                    }
                }
            }
            this.firstIx_ = 0;
            this.limitIx_ = 0;
            if (!stringSearch.initTextProcessedIter()) {
                return;
            }
            this.buf_ = new CEI[this.bufSize_];
        }

        CEI get(int i) {
            int i2 = i % this.bufSize_;
            if (i >= this.firstIx_ && i < this.limitIx_) {
                return this.buf_[i2];
            }
            if (i != this.limitIx_) {
                return null;
            }
            this.limitIx_++;
            if (this.limitIx_ - this.firstIx_ >= this.bufSize_) {
                this.firstIx_++;
            }
            CollationPCE.Range range = new CollationPCE.Range();
            if (this.buf_[i2] == null) {
                this.buf_[i2] = new CEI();
            }
            this.buf_[i2].ce_ = this.strSearch_.textProcessedIter_.nextProcessed(range);
            this.buf_[i2].lowIndex_ = range.ixLow_;
            this.buf_[i2].highIndex_ = range.ixHigh_;
            return this.buf_[i2];
        }

        CEI getPrevious(int i) {
            int i2 = i % this.bufSize_;
            if (i >= this.firstIx_ && i < this.limitIx_) {
                return this.buf_[i2];
            }
            if (i != this.limitIx_) {
                return null;
            }
            this.limitIx_++;
            if (this.limitIx_ - this.firstIx_ >= this.bufSize_) {
                this.firstIx_++;
            }
            CollationPCE.Range range = new CollationPCE.Range();
            if (this.buf_[i2] == null) {
                this.buf_[i2] = new CEI();
            }
            this.buf_[i2].ce_ = this.strSearch_.textProcessedIter_.previousProcessed(range);
            this.buf_[i2].lowIndex_ = range.ixLow_;
            this.buf_[i2].highIndex_ = range.ixHigh_;
            return this.buf_[i2];
        }

        static boolean MIGHT_BE_JAMO_L(char c) {
            return (c >= 4352 && c <= 4446) || (c >= 12593 && c <= 12622) || (c >= 12645 && c <= 12678);
        }
    }
}
