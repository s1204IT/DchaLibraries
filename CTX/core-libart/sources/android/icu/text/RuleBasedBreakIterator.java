package android.icu.text;

import android.icu.impl.CharacterIteration;
import android.icu.impl.ICUBinary;
import android.icu.impl.ICUDebug;
import android.icu.impl.Trie2;
import android.icu.impl.number.Padder;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.text.DictionaryBreakEngine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RuleBasedBreakIterator extends BreakIterator {
    static final boolean $assertionsDisabled = false;
    private static final String RBBI_DEBUG_ARG = "rbbi";
    private static final int RBBI_END = 2;
    private static final int RBBI_RUN = 1;
    private static final int RBBI_START = 0;
    private static final int START_STATE = 1;
    private static final int STOP_STATE = 0;
    private static final boolean TRACE;
    static final String fDebugEnv;
    private static final List<LanguageBreakEngine> gAllBreakEngines;
    private static final UnhandledBreakEngine gUnhandledBreakEngine;
    private static final int kMaxLookaheads = 8;
    private BreakCache fBreakCache;
    private List<LanguageBreakEngine> fBreakEngines;
    private int fBreakType;
    private DictionaryCache fDictionaryCache;
    private int fDictionaryCharCount;
    private boolean fDone;
    private LookAheadResults fLookAheadMatches;
    private int fPosition;
    RBBIDataWrapper fRData;
    private int fRuleStatusIndex;
    private CharacterIterator fText;

    static {
        TRACE = ICUDebug.enabled(RBBI_DEBUG_ARG) && ICUDebug.value(RBBI_DEBUG_ARG).indexOf("trace") >= 0;
        gUnhandledBreakEngine = new UnhandledBreakEngine();
        gAllBreakEngines = new ArrayList();
        gAllBreakEngines.add(gUnhandledBreakEngine);
        fDebugEnv = ICUDebug.enabled(RBBI_DEBUG_ARG) ? ICUDebug.value(RBBI_DEBUG_ARG) : null;
    }

    private RuleBasedBreakIterator() {
        this.fText = new java.text.StringCharacterIterator("");
        this.fBreakCache = new BreakCache();
        this.fDictionaryCache = new DictionaryCache();
        this.fBreakType = 1;
        this.fLookAheadMatches = new LookAheadResults();
        this.fDictionaryCharCount = 0;
        synchronized (gAllBreakEngines) {
            this.fBreakEngines = new ArrayList(gAllBreakEngines);
        }
    }

    public static RuleBasedBreakIterator getInstanceFromCompiledRules(InputStream inputStream) throws IOException {
        RuleBasedBreakIterator ruleBasedBreakIterator = new RuleBasedBreakIterator();
        ruleBasedBreakIterator.fRData = RBBIDataWrapper.get(ICUBinary.getByteBufferFromInputStreamAndCloseStream(inputStream));
        return ruleBasedBreakIterator;
    }

    @Deprecated
    public static RuleBasedBreakIterator getInstanceFromCompiledRules(ByteBuffer byteBuffer) throws IOException {
        RuleBasedBreakIterator ruleBasedBreakIterator = new RuleBasedBreakIterator();
        ruleBasedBreakIterator.fRData = RBBIDataWrapper.get(byteBuffer);
        return ruleBasedBreakIterator;
    }

    public RuleBasedBreakIterator(String str) {
        this();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            compileRules(str, byteArrayOutputStream);
            this.fRData = RBBIDataWrapper.get(ByteBuffer.wrap(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException("RuleBasedBreakIterator rule compilation internal error: " + e.getMessage());
        }
    }

    @Override
    public Object clone() {
        RuleBasedBreakIterator ruleBasedBreakIterator = (RuleBasedBreakIterator) super.clone();
        if (this.fText != null) {
            ruleBasedBreakIterator.fText = (CharacterIterator) this.fText.clone();
        }
        synchronized (gAllBreakEngines) {
            ruleBasedBreakIterator.fBreakEngines = new ArrayList(gAllBreakEngines);
        }
        ruleBasedBreakIterator.fLookAheadMatches = new LookAheadResults();
        Objects.requireNonNull(ruleBasedBreakIterator);
        ruleBasedBreakIterator.fBreakCache = ruleBasedBreakIterator.new BreakCache(this.fBreakCache);
        Objects.requireNonNull(ruleBasedBreakIterator);
        ruleBasedBreakIterator.fDictionaryCache = ruleBasedBreakIterator.new DictionaryCache(this.fDictionaryCache);
        return ruleBasedBreakIterator;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        try {
            RuleBasedBreakIterator ruleBasedBreakIterator = (RuleBasedBreakIterator) obj;
            if (this.fRData != ruleBasedBreakIterator.fRData && (this.fRData == null || ruleBasedBreakIterator.fRData == null)) {
                return false;
            }
            if (this.fRData != null && ruleBasedBreakIterator.fRData != null && !this.fRData.fRuleSource.equals(ruleBasedBreakIterator.fRData.fRuleSource)) {
                return false;
            }
            if (this.fText == null && ruleBasedBreakIterator.fText == null) {
                return true;
            }
            if (this.fText != null && ruleBasedBreakIterator.fText != null && this.fText.equals(ruleBasedBreakIterator.fText)) {
                if (this.fPosition != ruleBasedBreakIterator.fPosition) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        if (this.fRData == null) {
            return "";
        }
        return this.fRData.fRuleSource;
    }

    public int hashCode() {
        return this.fRData.fRuleSource.hashCode();
    }

    @Deprecated
    public void dump(PrintStream printStream) {
        if (printStream == null) {
            printStream = System.out;
        }
        this.fRData.dump(printStream);
    }

    public static void compileRules(String str, OutputStream outputStream) throws IOException {
        RBBIRuleBuilder.compileRules(str, outputStream);
    }

    @Override
    public int first() {
        if (this.fText == null) {
            return -1;
        }
        this.fText.first();
        int index = this.fText.getIndex();
        if (!this.fBreakCache.seek(index)) {
            this.fBreakCache.populateNear(index);
        }
        this.fBreakCache.current();
        return this.fPosition;
    }

    @Override
    public int last() {
        if (this.fText == null) {
            return -1;
        }
        int endIndex = this.fText.getEndIndex();
        isBoundary(endIndex);
        if (this.fPosition != endIndex) {
        }
        return endIndex;
    }

    @Override
    public int next(int i) {
        int iPrevious = 0;
        if (i > 0) {
            while (i > 0 && iPrevious != -1) {
                iPrevious = next();
                i--;
            }
            return iPrevious;
        }
        if (i < 0) {
            while (i < 0 && iPrevious != -1) {
                iPrevious = previous();
                i++;
            }
            return iPrevious;
        }
        return current();
    }

    @Override
    public int next() {
        this.fBreakCache.next();
        if (this.fDone) {
            return -1;
        }
        return this.fPosition;
    }

    @Override
    public int previous() {
        this.fBreakCache.previous();
        if (this.fDone) {
            return -1;
        }
        return this.fPosition;
    }

    @Override
    public int following(int i) {
        if (i < this.fText.getBeginIndex()) {
            return first();
        }
        this.fBreakCache.following(CISetIndex32(this.fText, i));
        if (this.fDone) {
            return -1;
        }
        return this.fPosition;
    }

    @Override
    public int preceding(int i) {
        if (this.fText == null || i > this.fText.getEndIndex()) {
            return last();
        }
        if (i < this.fText.getBeginIndex()) {
            return first();
        }
        this.fBreakCache.preceding(i);
        if (this.fDone) {
            return -1;
        }
        return this.fPosition;
    }

    protected static final void checkOffset(int i, CharacterIterator characterIterator) {
        if (i < characterIterator.getBeginIndex() || i > characterIterator.getEndIndex()) {
            throw new IllegalArgumentException("offset out of bounds");
        }
    }

    @Override
    public boolean isBoundary(int i) {
        checkOffset(i, this.fText);
        int iCISetIndex32 = CISetIndex32(this.fText, i);
        boolean z = false;
        if ((this.fBreakCache.seek(iCISetIndex32) || this.fBreakCache.populateNear(iCISetIndex32)) && this.fBreakCache.current() == i) {
            z = true;
        }
        if (!z) {
            next();
        }
        return z;
    }

    @Override
    public int current() {
        if (this.fText != null) {
            return this.fPosition;
        }
        return -1;
    }

    @Override
    public int getRuleStatus() {
        return this.fRData.fStatusTable[this.fRuleStatusIndex + this.fRData.fStatusTable[this.fRuleStatusIndex]];
    }

    @Override
    public int getRuleStatusVec(int[] iArr) {
        int i = this.fRData.fStatusTable[this.fRuleStatusIndex];
        if (iArr != null) {
            int iMin = Math.min(i, iArr.length);
            for (int i2 = 0; i2 < iMin; i2++) {
                iArr[i2] = this.fRData.fStatusTable[this.fRuleStatusIndex + i2 + 1];
            }
        }
        return i;
    }

    @Override
    public CharacterIterator getText() {
        return this.fText;
    }

    @Override
    public void setText(CharacterIterator characterIterator) {
        if (characterIterator != null) {
            this.fBreakCache.reset(characterIterator.getBeginIndex(), 0);
        } else {
            this.fBreakCache.reset();
        }
        this.fDictionaryCache.reset();
        this.fText = characterIterator;
        first();
    }

    void setBreakType(int i) {
        this.fBreakType = i;
    }

    int getBreakType() {
        return this.fBreakType;
    }

    private LanguageBreakEngine getLanguageBreakEngine(int i) {
        LanguageBreakEngine cjkBreakEngine;
        for (LanguageBreakEngine languageBreakEngine : this.fBreakEngines) {
            if (languageBreakEngine.handles(i, this.fBreakType)) {
                return languageBreakEngine;
            }
        }
        synchronized (gAllBreakEngines) {
            for (LanguageBreakEngine languageBreakEngine2 : gAllBreakEngines) {
                if (languageBreakEngine2.handles(i, this.fBreakType)) {
                    this.fBreakEngines.add(languageBreakEngine2);
                    return languageBreakEngine2;
                }
            }
            int intPropertyValue = UCharacter.getIntPropertyValue(i, UProperty.SCRIPT);
            if (intPropertyValue == 22 || intPropertyValue == 20) {
                intPropertyValue = 17;
            }
            try {
                switch (intPropertyValue) {
                    case 17:
                        if (getBreakType() == 1) {
                            cjkBreakEngine = new CjkBreakEngine(false);
                        } else {
                            gUnhandledBreakEngine.handleChar(i, getBreakType());
                            cjkBreakEngine = gUnhandledBreakEngine;
                        }
                        break;
                    case 18:
                        if (getBreakType() == 1) {
                            cjkBreakEngine = new CjkBreakEngine(true);
                        } else {
                            gUnhandledBreakEngine.handleChar(i, getBreakType());
                            cjkBreakEngine = gUnhandledBreakEngine;
                        }
                        break;
                    case 23:
                        cjkBreakEngine = new KhmerBreakEngine();
                        break;
                    case 24:
                        cjkBreakEngine = new LaoBreakEngine();
                        break;
                    case 28:
                        cjkBreakEngine = new BurmeseBreakEngine();
                        break;
                    case 38:
                        cjkBreakEngine = new ThaiBreakEngine();
                        break;
                    default:
                        gUnhandledBreakEngine.handleChar(i, getBreakType());
                        cjkBreakEngine = gUnhandledBreakEngine;
                        break;
                }
            } catch (IOException e) {
                cjkBreakEngine = null;
            }
            if (cjkBreakEngine != null && cjkBreakEngine != gUnhandledBreakEngine) {
                gAllBreakEngines.add(cjkBreakEngine);
                this.fBreakEngines.add(cjkBreakEngine);
            }
            return cjkBreakEngine;
        }
    }

    private static class LookAheadResults {
        static final boolean $assertionsDisabled = false;
        int fUsedSlotLimit = 0;
        int[] fPositions = new int[8];
        int[] fKeys = new int[8];

        LookAheadResults() {
        }

        int getPosition(int i) {
            for (int i2 = 0; i2 < this.fUsedSlotLimit; i2++) {
                if (this.fKeys[i2] == i) {
                    return this.fPositions[i2];
                }
            }
            return -1;
        }

        void setPosition(int i, int i2) {
            int i3 = 0;
            while (i3 < this.fUsedSlotLimit) {
                if (this.fKeys[i3] != i) {
                    i3++;
                } else {
                    this.fPositions[i3] = i2;
                    return;
                }
            }
            if (i3 >= 8) {
                i3 = 7;
            }
            this.fKeys[i3] = i;
            this.fPositions[i3] = i2;
            this.fUsedSlotLimit = i3 + 1;
        }

        void reset() {
            this.fUsedSlotLimit = 0;
        }
    }

    private int handleNext() {
        short s;
        short s2;
        short s3;
        int position;
        if (TRACE) {
            System.out.println("Handle Next   pos      char  state category");
        }
        this.fRuleStatusIndex = 0;
        this.fDictionaryCharCount = 0;
        CharacterIterator characterIterator = this.fText;
        Trie2 trie2 = this.fRData.fTrie;
        short[] sArr = this.fRData.fFTable;
        int i = this.fPosition;
        characterIterator.setIndex(i);
        int iCurrent = characterIterator.current();
        short s4 = 1;
        if (iCurrent >= 55296 && (iCurrent = CharacterIteration.nextTrail32(characterIterator, iCurrent)) == Integer.MAX_VALUE) {
            this.fDone = true;
            return -1;
        }
        int rowIndex = this.fRData.getRowIndex(1);
        short s5 = 2;
        int i2 = 10;
        int i3 = 5;
        if ((this.fRData.getStateTableFlags(sArr) & 2) != 0) {
            if (TRACE) {
                System.out.print("            " + RBBIDataWrapper.intToString(characterIterator.getIndex(), 5));
                System.out.print(RBBIDataWrapper.intToHexString(iCurrent, 10));
                System.out.println(RBBIDataWrapper.intToString(1, 7) + RBBIDataWrapper.intToString(2, 6));
            }
            s2 = 2;
            s = 0;
        } else {
            s = 1;
            s2 = 3;
        }
        this.fLookAheadMatches.reset();
        short s6 = s;
        int i4 = rowIndex;
        int index = i;
        int next = iCurrent;
        short s7 = 1;
        while (s7 != 0) {
            if (next == Integer.MAX_VALUE) {
                if (s6 == s5) {
                    break;
                }
                s2 = s4;
                s6 = s5;
            } else if (s6 == s4) {
                short s8 = (short) trie2.get(next);
                if ((s8 & 16384) != 0) {
                    this.fDictionaryCharCount += s4;
                    s3 = (short) (s8 & (-16385));
                } else {
                    s3 = s8;
                }
                if (TRACE) {
                    System.out.print("            " + RBBIDataWrapper.intToString(characterIterator.getIndex(), i3));
                    System.out.print(RBBIDataWrapper.intToHexString(next, i2));
                    System.out.println(RBBIDataWrapper.intToString(s7, 7) + RBBIDataWrapper.intToString(s3, 6));
                }
                next = characterIterator.next();
                if (next >= 55296) {
                    next = CharacterIteration.nextTrail32(characterIterator, next);
                }
                s2 = s3;
            } else {
                s6 = 1;
            }
            short s9 = sArr[i4 + 4 + s2];
            int rowIndex2 = this.fRData.getRowIndex(s9);
            int i5 = rowIndex2 + 0;
            if (sArr[i5] == -1) {
                int index2 = characterIterator.getIndex();
                if (next >= 65536 && next <= 1114111) {
                    index2--;
                }
                this.fRuleStatusIndex = sArr[rowIndex2 + 2];
                index = index2;
            }
            short s10 = sArr[i5];
            if (s10 > 0 && (position = this.fLookAheadMatches.getPosition(s10)) >= 0) {
                this.fRuleStatusIndex = sArr[rowIndex2 + 2];
                this.fPosition = position;
                return position;
            }
            short s11 = sArr[rowIndex2 + 1];
            if (s11 != 0) {
                int index3 = characterIterator.getIndex();
                if (next >= 65536 && next <= 1114111) {
                    index3--;
                }
                this.fLookAheadMatches.setPosition(s11, index3);
            }
            s7 = s9;
            i4 = rowIndex2;
            s5 = 2;
            i2 = 10;
            i3 = 5;
            s4 = 1;
        }
        if (index == i) {
            if (TRACE) {
                System.out.println("Iterator did not move. Advancing by 1.");
            }
            characterIterator.setIndex(i);
            CharacterIteration.next32(characterIterator);
            index = characterIterator.getIndex();
            this.fRuleStatusIndex = 0;
        }
        this.fPosition = index;
        if (TRACE) {
            System.out.println("result = " + index);
        }
        return index;
    }

    private int handlePrevious(int i) {
        int i2;
        short s;
        short s2;
        int position;
        char c = 0;
        if (this.fText == null) {
            return 0;
        }
        this.fLookAheadMatches.reset();
        short[] sArr = this.fRData.fSRTable;
        CISetIndex32(this.fText, i);
        if (i == this.fText.getBeginIndex()) {
            return -1;
        }
        int iPrevious32 = CharacterIteration.previous32(this.fText);
        int rowIndex = this.fRData.getRowIndex(1);
        int i3 = 3;
        if ((this.fRData.getStateTableFlags(sArr) & 2) != 0) {
            i3 = 2;
        } else {
            c = 1;
        }
        if (TRACE) {
            System.out.println("Handle Prev   pos   char  state category ");
        }
        int index = i;
        int rowIndex2 = rowIndex;
        short s3 = 1;
        while (true) {
            if (iPrevious32 == Integer.MAX_VALUE) {
                if (c == 2) {
                    break;
                }
                i3 = 1;
                c = 2;
                if (c == 1) {
                }
                if (TRACE) {
                }
                s3 = sArr[rowIndex2 + 4 + i3];
                rowIndex2 = this.fRData.getRowIndex(s3);
                i2 = rowIndex2 + 0;
                if (sArr[i2] == -1) {
                }
                s = sArr[i2];
                if (s > 0) {
                    s2 = sArr[rowIndex2 + 1];
                    if (s2 != 0) {
                    }
                    if (s3 != 0) {
                    }
                }
            } else {
                if (c == 1) {
                    i3 = ((short) this.fRData.fTrie.get(iPrevious32)) & (-16385);
                }
                if (TRACE) {
                    System.out.print("             " + this.fText.getIndex() + "   ");
                    if (32 <= iPrevious32 && iPrevious32 < 127) {
                        System.out.print("  " + iPrevious32 + "  ");
                    } else {
                        System.out.print(Padder.FALLBACK_PADDING_STRING + Integer.toHexString(iPrevious32) + Padder.FALLBACK_PADDING_STRING);
                    }
                    System.out.println(Padder.FALLBACK_PADDING_STRING + ((int) s3) + "  " + i3 + Padder.FALLBACK_PADDING_STRING);
                }
                s3 = sArr[rowIndex2 + 4 + i3];
                rowIndex2 = this.fRData.getRowIndex(s3);
                i2 = rowIndex2 + 0;
                if (sArr[i2] == -1) {
                    index = this.fText.getIndex();
                }
                s = sArr[i2];
                if (s > 0 || (position = this.fLookAheadMatches.getPosition(s)) < 0) {
                    s2 = sArr[rowIndex2 + 1];
                    if (s2 != 0) {
                        this.fLookAheadMatches.setPosition(s2, this.fText.getIndex());
                    }
                    if (s3 != 0) {
                        break;
                    }
                    if (c == 1) {
                        iPrevious32 = CharacterIteration.previous32(this.fText);
                    } else if (c == 0) {
                        c = 1;
                    }
                } else {
                    index = position;
                    break;
                }
            }
        }
        if (index == i) {
            CISetIndex32(this.fText, i);
            CharacterIteration.previous32(this.fText);
            index = this.fText.getIndex();
        }
        if (TRACE) {
            System.out.println("Result = " + index);
        }
        return index;
    }

    private static int CISetIndex32(CharacterIterator characterIterator, int i) {
        if (i <= characterIterator.getBeginIndex()) {
            characterIterator.first();
        } else if (i >= characterIterator.getEndIndex()) {
            characterIterator.setIndex(characterIterator.getEndIndex());
        } else if (Character.isLowSurrogate(characterIterator.setIndex(i)) && !Character.isHighSurrogate(characterIterator.previous())) {
            characterIterator.next();
        }
        return characterIterator.getIndex();
    }

    class DictionaryCache {
        static final boolean $assertionsDisabled = false;
        int fBoundary;
        DictionaryBreakEngine.DequeI fBreaks;
        int fFirstRuleStatusIndex;
        int fLimit;
        int fOtherRuleStatusIndex;
        int fPositionInCache;
        int fStart;
        int fStatusIndex;

        void reset() {
            this.fPositionInCache = -1;
            this.fStart = 0;
            this.fLimit = 0;
            this.fFirstRuleStatusIndex = 0;
            this.fOtherRuleStatusIndex = 0;
            this.fBreaks.removeAllElements();
        }

        boolean following(int i) {
            if (i >= this.fLimit || i < this.fStart) {
                this.fPositionInCache = -1;
                return false;
            }
            if (this.fPositionInCache >= 0 && this.fPositionInCache < this.fBreaks.size() && this.fBreaks.elementAt(this.fPositionInCache) == i) {
                this.fPositionInCache++;
                if (this.fPositionInCache >= this.fBreaks.size()) {
                    this.fPositionInCache = -1;
                    return false;
                }
                this.fBoundary = this.fBreaks.elementAt(this.fPositionInCache);
                this.fStatusIndex = this.fOtherRuleStatusIndex;
                return true;
            }
            this.fPositionInCache = 0;
            while (this.fPositionInCache < this.fBreaks.size()) {
                int iElementAt = this.fBreaks.elementAt(this.fPositionInCache);
                if (iElementAt <= i) {
                    this.fPositionInCache++;
                } else {
                    this.fBoundary = iElementAt;
                    this.fStatusIndex = this.fOtherRuleStatusIndex;
                    return true;
                }
            }
            this.fPositionInCache = -1;
            return false;
        }

        boolean preceding(int i) {
            if (i <= this.fStart || i > this.fLimit) {
                this.fPositionInCache = -1;
                return false;
            }
            if (i == this.fLimit) {
                this.fPositionInCache = this.fBreaks.size() - 1;
                if (this.fPositionInCache >= 0) {
                }
            }
            if (this.fPositionInCache > 0 && this.fPositionInCache < this.fBreaks.size() && this.fBreaks.elementAt(this.fPositionInCache) == i) {
                this.fPositionInCache--;
                int iElementAt = this.fBreaks.elementAt(this.fPositionInCache);
                this.fBoundary = iElementAt;
                this.fStatusIndex = iElementAt == this.fStart ? this.fFirstRuleStatusIndex : this.fOtherRuleStatusIndex;
                return true;
            }
            if (this.fPositionInCache == 0) {
                this.fPositionInCache = -1;
                return false;
            }
            int size = this.fBreaks.size();
            while (true) {
                this.fPositionInCache = size - 1;
                if (this.fPositionInCache >= 0) {
                    int iElementAt2 = this.fBreaks.elementAt(this.fPositionInCache);
                    if (iElementAt2 >= i) {
                        size = this.fPositionInCache;
                    } else {
                        this.fBoundary = iElementAt2;
                        this.fStatusIndex = iElementAt2 == this.fStart ? this.fFirstRuleStatusIndex : this.fOtherRuleStatusIndex;
                        return true;
                    }
                } else {
                    this.fPositionInCache = -1;
                    return false;
                }
            }
        }

        void populateDictionary(int i, int i2, int i3, int i4) {
            if (i2 - i <= 1) {
                return;
            }
            reset();
            this.fFirstRuleStatusIndex = i3;
            this.fOtherRuleStatusIndex = i4;
            RuleBasedBreakIterator.this.fText.setIndex(i);
            int iCurrent32 = CharacterIteration.current32(RuleBasedBreakIterator.this.fText);
            short s = (short) RuleBasedBreakIterator.this.fRData.fTrie.get(iCurrent32);
            int iFindBreaks = 0;
            while (true) {
                int index = RuleBasedBreakIterator.this.fText.getIndex();
                if (index < i2 && (s & 16384) == 0) {
                    iCurrent32 = CharacterIteration.next32(RuleBasedBreakIterator.this.fText);
                    s = (short) RuleBasedBreakIterator.this.fRData.fTrie.get(iCurrent32);
                } else {
                    if (index >= i2) {
                        break;
                    }
                    LanguageBreakEngine languageBreakEngine = RuleBasedBreakIterator.this.getLanguageBreakEngine(iCurrent32);
                    if (languageBreakEngine != null) {
                        iFindBreaks += languageBreakEngine.findBreaks(RuleBasedBreakIterator.this.fText, i, i2, RuleBasedBreakIterator.this.fBreakType, this.fBreaks);
                    }
                    iCurrent32 = CharacterIteration.current32(RuleBasedBreakIterator.this.fText);
                    s = (short) RuleBasedBreakIterator.this.fRData.fTrie.get(iCurrent32);
                }
            }
            if (iFindBreaks > 0) {
                if (i < this.fBreaks.elementAt(0)) {
                    this.fBreaks.offer(i);
                }
                if (i2 > this.fBreaks.peek()) {
                    this.fBreaks.push(i2);
                }
                this.fPositionInCache = 0;
                this.fStart = this.fBreaks.elementAt(0);
                this.fLimit = this.fBreaks.peek();
            }
        }

        DictionaryCache() {
            this.fPositionInCache = -1;
            this.fBreaks = new DictionaryBreakEngine.DequeI();
        }

        DictionaryCache(DictionaryCache dictionaryCache) {
            try {
                this.fBreaks = (DictionaryBreakEngine.DequeI) dictionaryCache.fBreaks.clone();
                this.fPositionInCache = dictionaryCache.fPositionInCache;
                this.fStart = dictionaryCache.fStart;
                this.fLimit = dictionaryCache.fLimit;
                this.fFirstRuleStatusIndex = dictionaryCache.fFirstRuleStatusIndex;
                this.fOtherRuleStatusIndex = dictionaryCache.fOtherRuleStatusIndex;
                this.fBoundary = dictionaryCache.fBoundary;
                this.fStatusIndex = dictionaryCache.fStatusIndex;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class BreakCache {
        static final boolean $assertionsDisabled = false;
        static final int CACHE_SIZE = 128;
        static final boolean RetainCachePosition = false;
        static final boolean UpdateCachePosition = true;
        int[] fBoundaries;
        int fBufIdx;
        int fEndBufIdx;
        DictionaryBreakEngine.DequeI fSideBuffer;
        int fStartBufIdx;
        short[] fStatuses;
        int fTextIdx;

        BreakCache() {
            this.fBoundaries = new int[128];
            this.fStatuses = new short[128];
            this.fSideBuffer = new DictionaryBreakEngine.DequeI();
            reset();
        }

        void reset(int i, int i2) {
            this.fStartBufIdx = 0;
            this.fEndBufIdx = 0;
            this.fTextIdx = i;
            this.fBufIdx = 0;
            this.fBoundaries[0] = i;
            this.fStatuses[0] = (short) i2;
        }

        void reset() {
            reset(0, 0);
        }

        void next() {
            if (this.fBufIdx == this.fEndBufIdx) {
                RuleBasedBreakIterator.this.fDone = !populateFollowing();
                RuleBasedBreakIterator.this.fPosition = this.fTextIdx;
                RuleBasedBreakIterator.this.fRuleStatusIndex = this.fStatuses[this.fBufIdx];
                return;
            }
            this.fBufIdx = modChunkSize(this.fBufIdx + 1);
            this.fTextIdx = RuleBasedBreakIterator.this.fPosition = this.fBoundaries[this.fBufIdx];
            RuleBasedBreakIterator.this.fRuleStatusIndex = this.fStatuses[this.fBufIdx];
        }

        void previous() {
            int i = this.fBufIdx;
            if (this.fBufIdx == this.fStartBufIdx) {
                populatePreceding();
            } else {
                this.fBufIdx = modChunkSize(this.fBufIdx - 1);
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
            }
            RuleBasedBreakIterator.this.fDone = this.fBufIdx == i;
            RuleBasedBreakIterator.this.fPosition = this.fTextIdx;
            RuleBasedBreakIterator.this.fRuleStatusIndex = this.fStatuses[this.fBufIdx];
        }

        void following(int i) {
            if (i == this.fTextIdx || seek(i) || populateNear(i)) {
                RuleBasedBreakIterator.this.fDone = false;
                next();
            }
        }

        void preceding(int i) {
            if (i == this.fTextIdx || seek(i) || populateNear(i)) {
                if (i == this.fTextIdx) {
                    previous();
                } else {
                    current();
                }
            }
        }

        int current() {
            RuleBasedBreakIterator.this.fPosition = this.fTextIdx;
            RuleBasedBreakIterator.this.fRuleStatusIndex = this.fStatuses[this.fBufIdx];
            RuleBasedBreakIterator.this.fDone = false;
            return this.fTextIdx;
        }

        boolean populateNear(int i) {
            int i2;
            if (i < this.fBoundaries[this.fStartBufIdx] - 15 || i > this.fBoundaries[this.fEndBufIdx] + 15) {
                int beginIndex = RuleBasedBreakIterator.this.fText.getBeginIndex();
                if (i > beginIndex + 20) {
                    RuleBasedBreakIterator.this.fPosition = RuleBasedBreakIterator.this.handlePrevious(i);
                    beginIndex = RuleBasedBreakIterator.this.handleNext();
                    i2 = RuleBasedBreakIterator.this.fRuleStatusIndex;
                } else {
                    i2 = 0;
                }
                reset(beginIndex, i2);
            }
            if (this.fBoundaries[this.fEndBufIdx] < i) {
                while (this.fBoundaries[this.fEndBufIdx] < i) {
                    if (!populateFollowing()) {
                        return false;
                    }
                }
                this.fBufIdx = this.fEndBufIdx;
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
                while (this.fTextIdx > i) {
                    previous();
                }
                return true;
            }
            if (this.fBoundaries[this.fStartBufIdx] <= i) {
                return true;
            }
            while (this.fBoundaries[this.fStartBufIdx] > i) {
                populatePreceding();
            }
            this.fBufIdx = this.fStartBufIdx;
            this.fTextIdx = this.fBoundaries[this.fBufIdx];
            while (this.fTextIdx < i) {
                next();
            }
            if (this.fTextIdx > i) {
                previous();
            }
            return true;
        }

        boolean populateFollowing() {
            int iHandleNext;
            int i = this.fBoundaries[this.fEndBufIdx];
            short s = this.fStatuses[this.fEndBufIdx];
            if (RuleBasedBreakIterator.this.fDictionaryCache.following(i)) {
                addFollowing(RuleBasedBreakIterator.this.fDictionaryCache.fBoundary, RuleBasedBreakIterator.this.fDictionaryCache.fStatusIndex, true);
                return true;
            }
            RuleBasedBreakIterator.this.fPosition = i;
            int iHandleNext2 = RuleBasedBreakIterator.this.handleNext();
            if (iHandleNext2 == -1) {
                return false;
            }
            int i2 = RuleBasedBreakIterator.this.fRuleStatusIndex;
            if (RuleBasedBreakIterator.this.fDictionaryCharCount > 0) {
                RuleBasedBreakIterator.this.fDictionaryCache.populateDictionary(i, iHandleNext2, s, i2);
                if (RuleBasedBreakIterator.this.fDictionaryCache.following(i)) {
                    addFollowing(RuleBasedBreakIterator.this.fDictionaryCache.fBoundary, RuleBasedBreakIterator.this.fDictionaryCache.fStatusIndex, true);
                    return true;
                }
            }
            addFollowing(iHandleNext2, i2, true);
            for (int i3 = 0; i3 < 6 && (iHandleNext = RuleBasedBreakIterator.this.handleNext()) != -1 && RuleBasedBreakIterator.this.fDictionaryCharCount <= 0; i3++) {
                addFollowing(iHandleNext, RuleBasedBreakIterator.this.fRuleStatusIndex, false);
            }
            return true;
        }

        boolean populatePreceding() {
            int iHandleNext;
            int i;
            boolean z;
            int beginIndex = RuleBasedBreakIterator.this.fText.getBeginIndex();
            int i2 = this.fBoundaries[this.fStartBufIdx];
            if (i2 != beginIndex) {
                boolean z2 = true;
                if (RuleBasedBreakIterator.this.fDictionaryCache.preceding(i2)) {
                    addPreceding(RuleBasedBreakIterator.this.fDictionaryCache.fBoundary, RuleBasedBreakIterator.this.fDictionaryCache.fStatusIndex, true);
                    return true;
                }
                int iHandlePrevious = i2;
                do {
                    int i3 = iHandlePrevious - 30;
                    if (i3 > beginIndex) {
                        iHandlePrevious = RuleBasedBreakIterator.this.handlePrevious(i3);
                    } else {
                        iHandlePrevious = beginIndex;
                    }
                    if (iHandlePrevious != -1 && iHandlePrevious != beginIndex) {
                        RuleBasedBreakIterator.this.fPosition = iHandlePrevious;
                        iHandleNext = RuleBasedBreakIterator.this.handleNext();
                        i = RuleBasedBreakIterator.this.fRuleStatusIndex;
                    } else {
                        iHandleNext = beginIndex;
                        i = 0;
                    }
                } while (iHandleNext >= i2);
                this.fSideBuffer.removeAllElements();
                this.fSideBuffer.push(iHandleNext);
                this.fSideBuffer.push(i);
                while (true) {
                    int i4 = RuleBasedBreakIterator.this.fPosition = iHandleNext;
                    int iHandleNext2 = RuleBasedBreakIterator.this.handleNext();
                    int i5 = RuleBasedBreakIterator.this.fRuleStatusIndex;
                    if (iHandleNext2 == -1) {
                        break;
                    }
                    if (RuleBasedBreakIterator.this.fDictionaryCharCount != 0) {
                        RuleBasedBreakIterator.this.fDictionaryCache.populateDictionary(i4, iHandleNext2, i, i5);
                        i = i5;
                        z = false;
                        while (true) {
                            if (!RuleBasedBreakIterator.this.fDictionaryCache.following(i4)) {
                                break;
                            }
                            iHandleNext2 = RuleBasedBreakIterator.this.fDictionaryCache.fBoundary;
                            i = RuleBasedBreakIterator.this.fDictionaryCache.fStatusIndex;
                            if (iHandleNext2 < i2) {
                                this.fSideBuffer.push(iHandleNext2);
                                this.fSideBuffer.push(i);
                                i4 = iHandleNext2;
                                z = true;
                            } else {
                                z = true;
                                break;
                            }
                        }
                    } else {
                        i = i5;
                        z = false;
                    }
                    if (!z && iHandleNext2 < i2) {
                        this.fSideBuffer.push(iHandleNext2);
                        this.fSideBuffer.push(i);
                    }
                    if (iHandleNext2 >= i2) {
                        break;
                    }
                    iHandleNext = iHandleNext2;
                }
                if (!this.fSideBuffer.isEmpty()) {
                    addPreceding(this.fSideBuffer.pop(), this.fSideBuffer.pop(), true);
                } else {
                    z2 = false;
                }
                while (!this.fSideBuffer.isEmpty()) {
                    if (!addPreceding(this.fSideBuffer.pop(), this.fSideBuffer.pop(), false)) {
                        break;
                    }
                }
                return z2;
            }
            return false;
        }

        void addFollowing(int i, int i2, boolean z) {
            int iModChunkSize = modChunkSize(this.fEndBufIdx + 1);
            if (iModChunkSize == this.fStartBufIdx) {
                this.fStartBufIdx = modChunkSize(this.fStartBufIdx + 6);
            }
            this.fBoundaries[iModChunkSize] = i;
            this.fStatuses[iModChunkSize] = (short) i2;
            this.fEndBufIdx = iModChunkSize;
            if (z) {
                this.fBufIdx = iModChunkSize;
                this.fTextIdx = i;
            }
        }

        boolean addPreceding(int i, int i2, boolean z) {
            int iModChunkSize = modChunkSize(this.fStartBufIdx - 1);
            if (iModChunkSize == this.fEndBufIdx) {
                if (this.fBufIdx == this.fEndBufIdx && !z) {
                    return false;
                }
                this.fEndBufIdx = modChunkSize(this.fEndBufIdx - 1);
            }
            this.fBoundaries[iModChunkSize] = i;
            this.fStatuses[iModChunkSize] = (short) i2;
            this.fStartBufIdx = iModChunkSize;
            if (z) {
                this.fBufIdx = iModChunkSize;
                this.fTextIdx = i;
            }
            return true;
        }

        boolean seek(int i) {
            if (i < this.fBoundaries[this.fStartBufIdx] || i > this.fBoundaries[this.fEndBufIdx]) {
                return false;
            }
            if (i == this.fBoundaries[this.fStartBufIdx]) {
                this.fBufIdx = this.fStartBufIdx;
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
                return true;
            }
            if (i == this.fBoundaries[this.fEndBufIdx]) {
                this.fBufIdx = this.fEndBufIdx;
                this.fTextIdx = this.fBoundaries[this.fBufIdx];
                return true;
            }
            int iModChunkSize = this.fStartBufIdx;
            int i2 = this.fEndBufIdx;
            while (iModChunkSize != i2) {
                int iModChunkSize2 = modChunkSize(((iModChunkSize + i2) + (iModChunkSize > i2 ? 128 : 0)) / 2);
                if (this.fBoundaries[iModChunkSize2] <= i) {
                    iModChunkSize = modChunkSize(iModChunkSize2 + 1);
                } else {
                    i2 = iModChunkSize2;
                }
            }
            this.fBufIdx = modChunkSize(i2 - 1);
            this.fTextIdx = this.fBoundaries[this.fBufIdx];
            return true;
        }

        BreakCache(BreakCache breakCache) {
            this.fBoundaries = new int[128];
            this.fStatuses = new short[128];
            this.fSideBuffer = new DictionaryBreakEngine.DequeI();
            this.fStartBufIdx = breakCache.fStartBufIdx;
            this.fEndBufIdx = breakCache.fEndBufIdx;
            this.fTextIdx = breakCache.fTextIdx;
            this.fBufIdx = breakCache.fBufIdx;
            this.fBoundaries = (int[]) breakCache.fBoundaries.clone();
            this.fStatuses = (short[]) breakCache.fStatuses.clone();
            this.fSideBuffer = new DictionaryBreakEngine.DequeI();
        }

        void dumpCache() {
            System.out.printf("fTextIdx:%d   fBufIdx:%d%n", Integer.valueOf(this.fTextIdx), Integer.valueOf(this.fBufIdx));
            int iModChunkSize = this.fStartBufIdx;
            while (true) {
                System.out.printf("%d  %d%n", Integer.valueOf(iModChunkSize), Integer.valueOf(this.fBoundaries[iModChunkSize]));
                if (iModChunkSize != this.fEndBufIdx) {
                    iModChunkSize = modChunkSize(iModChunkSize + 1);
                } else {
                    return;
                }
            }
        }

        private final int modChunkSize(int i) {
            return i & 127;
        }
    }
}
