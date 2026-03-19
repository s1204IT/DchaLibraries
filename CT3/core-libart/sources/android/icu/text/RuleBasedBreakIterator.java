package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.CharTrie;
import android.icu.impl.CharacterIteration;
import android.icu.impl.ICUBinary;
import android.icu.impl.ICUDebug;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.text.DictionaryBreakEngine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.CharacterIterator;
import java.util.concurrent.ConcurrentHashMap;

public class RuleBasedBreakIterator extends BreakIterator {

    static final boolean f87assertionsDisabled;
    private static final String RBBI_DEBUG_ARG = "rbbi";
    private static final int RBBI_END = 2;
    private static final int RBBI_RUN = 1;
    private static final int RBBI_START = 0;
    private static final int START_STATE = 1;
    private static final int STOP_STATE = 0;
    private static final boolean TRACE;
    static final String fDebugEnv;
    private final ConcurrentHashMap<Integer, LanguageBreakEngine> fBreakEngines;
    private int fBreakType;
    private int[] fCachedBreakPositions;
    private int fDictionaryCharCount;
    private int fLastRuleStatusIndex;
    private boolean fLastStatusIndexValid;
    private int fPositionInCache;
    RBBIDataWrapper fRData;
    private CharacterIterator fText;
    private final UnhandledBreakEngine fUnhandledBreakEngine;

    private RuleBasedBreakIterator() {
        this.fText = new java.text.StringCharacterIterator("");
        this.fBreakType = 2;
        this.fUnhandledBreakEngine = new UnhandledBreakEngine();
        this.fBreakEngines = new ConcurrentHashMap<>();
        this.fLastStatusIndexValid = true;
        this.fDictionaryCharCount = 0;
        this.fBreakEngines.put(-1, this.fUnhandledBreakEngine);
    }

    public static RuleBasedBreakIterator getInstanceFromCompiledRules(InputStream is) throws IOException {
        RuleBasedBreakIterator This = new RuleBasedBreakIterator();
        This.fRData = RBBIDataWrapper.get(ICUBinary.getByteBufferFromInputStreamAndCloseStream(is));
        return This;
    }

    @Deprecated
    public static RuleBasedBreakIterator getInstanceFromCompiledRules(ByteBuffer bytes) throws IOException {
        RuleBasedBreakIterator This = new RuleBasedBreakIterator();
        This.fRData = RBBIDataWrapper.get(bytes);
        return This;
    }

    public RuleBasedBreakIterator(String rules) {
        this();
        try {
            ByteArrayOutputStream ruleOS = new ByteArrayOutputStream();
            compileRules(rules, ruleOS);
            this.fRData = RBBIDataWrapper.get(ByteBuffer.wrap(ruleOS.toByteArray()));
        } catch (IOException e) {
            RuntimeException rte = new RuntimeException("RuleBasedBreakIterator rule compilation internal error: " + e.getMessage());
            throw rte;
        }
    }

    @Override
    public Object clone() {
        RuleBasedBreakIterator result = (RuleBasedBreakIterator) super.clone();
        if (this.fText != null) {
            result.fText = (CharacterIterator) this.fText.clone();
        }
        return result;
    }

    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        try {
            RuleBasedBreakIterator other = (RuleBasedBreakIterator) that;
            if (this.fRData != other.fRData && (this.fRData == null || other.fRData == null)) {
                return false;
            }
            if (this.fRData != null && other.fRData != null && !this.fRData.fRuleSource.equals(other.fRData.fRuleSource)) {
                return false;
            }
            if (this.fText == null && other.fText == null) {
                return true;
            }
            if (this.fText == null || other.fText == null) {
                return false;
            }
            return this.fText.equals(other.fText);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        if (this.fRData == null) {
            return "";
        }
        String retStr = this.fRData.fRuleSource;
        return retStr;
    }

    public int hashCode() {
        return this.fRData.fRuleSource.hashCode();
    }

    static {
        boolean z = false;
        f87assertionsDisabled = !RuleBasedBreakIterator.class.desiredAssertionStatus();
        if (ICUDebug.enabled(RBBI_DEBUG_ARG) && ICUDebug.value(RBBI_DEBUG_ARG).indexOf("trace") >= 0) {
            z = true;
        }
        TRACE = z;
        fDebugEnv = ICUDebug.enabled(RBBI_DEBUG_ARG) ? ICUDebug.value(RBBI_DEBUG_ARG) : null;
    }

    private void reset() {
        this.fCachedBreakPositions = null;
        this.fDictionaryCharCount = 0;
        this.fPositionInCache = 0;
    }

    @Deprecated
    public void dump() {
        this.fRData.dump();
    }

    public static void compileRules(String rules, OutputStream ruleBinary) throws IOException {
        RBBIRuleBuilder.compileRules(rules, ruleBinary);
    }

    @Override
    public int first() {
        this.fCachedBreakPositions = null;
        this.fDictionaryCharCount = 0;
        this.fPositionInCache = 0;
        this.fLastRuleStatusIndex = 0;
        this.fLastStatusIndexValid = true;
        if (this.fText == null) {
            return -1;
        }
        this.fText.first();
        return this.fText.getIndex();
    }

    @Override
    public int last() {
        this.fCachedBreakPositions = null;
        this.fDictionaryCharCount = 0;
        this.fPositionInCache = 0;
        if (this.fText == null) {
            this.fLastRuleStatusIndex = 0;
            this.fLastStatusIndexValid = true;
            return -1;
        }
        this.fLastStatusIndexValid = false;
        int pos = this.fText.getEndIndex();
        this.fText.setIndex(pos);
        return pos;
    }

    @Override
    public int next(int n) {
        int result = current();
        while (n > 0) {
            result = next();
            n--;
        }
        while (n < 0) {
            result = previous();
            n++;
        }
        return result;
    }

    @Override
    public int next() {
        if (this.fCachedBreakPositions != null) {
            if (this.fPositionInCache < this.fCachedBreakPositions.length - 1) {
                this.fPositionInCache++;
                int pos = this.fCachedBreakPositions[this.fPositionInCache];
                this.fText.setIndex(pos);
                return pos;
            }
            reset();
        }
        int startPos = current();
        this.fDictionaryCharCount = 0;
        int result = handleNext(this.fRData.fFTable);
        if (this.fDictionaryCharCount > 0) {
            return checkDictionary(startPos, result, false);
        }
        return result;
    }

    private int checkDictionary(int startPos, int endPos, boolean reverse) {
        int c;
        int category;
        int c2;
        int category2;
        reset();
        if (endPos - startPos <= 1) {
            return reverse ? startPos : endPos;
        }
        this.fText.setIndex(reverse ? endPos : startPos);
        if (reverse) {
            CharacterIteration.previous32(this.fText);
        }
        int rangeStart = startPos;
        int rangeEnd = endPos;
        DictionaryBreakEngine.DequeI breaks = new DictionaryBreakEngine.DequeI();
        int foundBreakCount = 0;
        int c3 = CharacterIteration.current32(this.fText);
        int category3 = (short) this.fRData.fTrie.getCodePointValue(c3);
        if ((category3 & 16384) != 0) {
            if (reverse) {
                do {
                    CharacterIteration.next32(this.fText);
                    c2 = CharacterIteration.current32(this.fText);
                    category2 = (short) this.fRData.fTrie.getCodePointValue(c2);
                    if (c2 == Integer.MAX_VALUE) {
                        break;
                    }
                } while ((category2 & 16384) != 0);
                rangeEnd = this.fText.getIndex();
                if (c2 == Integer.MAX_VALUE) {
                    c3 = CharacterIteration.previous32(this.fText);
                } else {
                    c3 = CharacterIteration.previous32(this.fText);
                }
            } else {
                do {
                    c = CharacterIteration.previous32(this.fText);
                    category = (short) this.fRData.fTrie.getCodePointValue(c);
                    if (c == Integer.MAX_VALUE) {
                        break;
                    }
                } while ((category & 16384) != 0);
                if (c == Integer.MAX_VALUE) {
                    c3 = CharacterIteration.current32(this.fText);
                } else {
                    CharacterIteration.next32(this.fText);
                    c3 = CharacterIteration.current32(this.fText);
                }
                rangeStart = this.fText.getIndex();
            }
            category3 = (short) this.fRData.fTrie.getCodePointValue(c3);
        }
        if (reverse) {
            this.fText.setIndex(rangeStart);
            c3 = CharacterIteration.current32(this.fText);
            category3 = (short) this.fRData.fTrie.getCodePointValue(c3);
        }
        LanguageBreakEngine lbe = null;
        while (true) {
            int current = this.fText.getIndex();
            if (current < rangeEnd && (category3 & 16384) == 0) {
                CharacterIteration.next32(this.fText);
                c3 = CharacterIteration.current32(this.fText);
                category3 = (short) this.fRData.fTrie.getCodePointValue(c3);
            } else if (current < rangeEnd) {
                lbe = getLanguageBreakEngine(c3);
                if (lbe != null) {
                    int startingIdx = this.fText.getIndex();
                    foundBreakCount += lbe.findBreaks(this.fText, rangeStart, rangeEnd, false, this.fBreakType, breaks);
                    if (!f87assertionsDisabled) {
                        if (!(this.fText.getIndex() > startingIdx)) {
                            throw new AssertionError();
                        }
                    }
                }
                c3 = CharacterIteration.current32(this.fText);
                category3 = (short) this.fRData.fTrie.getCodePointValue(c3);
            } else {
                if (foundBreakCount > 0) {
                    if (foundBreakCount != breaks.size()) {
                        System.out.println("oops, foundBreakCount != breaks.size().  LBE = " + lbe.getClass());
                    }
                    if (!f87assertionsDisabled) {
                        if (!(foundBreakCount == breaks.size())) {
                            throw new AssertionError();
                        }
                    }
                    if (startPos < breaks.peekLast()) {
                        breaks.offer(startPos);
                    }
                    if (endPos > breaks.peek()) {
                        breaks.push(endPos);
                    }
                    this.fCachedBreakPositions = new int[breaks.size()];
                    int i = 0;
                    while (breaks.size() > 0) {
                        this.fCachedBreakPositions[i] = breaks.pollLast();
                        i++;
                    }
                    if (reverse) {
                        return preceding(endPos);
                    }
                    return following(startPos);
                }
                this.fText.setIndex(reverse ? startPos : endPos);
                return reverse ? startPos : endPos;
            }
        }
    }

    @Override
    public int previous() {
        CharacterIterator text = getText();
        this.fLastStatusIndexValid = false;
        if (this.fCachedBreakPositions != null) {
            if (this.fPositionInCache > 0) {
                this.fPositionInCache--;
                if (this.fPositionInCache <= 0) {
                    this.fLastStatusIndexValid = false;
                }
                int pos = this.fCachedBreakPositions[this.fPositionInCache];
                text.setIndex(pos);
                return pos;
            }
            reset();
        }
        int startPos = current();
        if (this.fText == null || startPos == this.fText.getBeginIndex()) {
            this.fLastRuleStatusIndex = 0;
            this.fLastStatusIndexValid = true;
            return -1;
        }
        if (this.fRData.fSRTable != null || this.fRData.fSFTable != null) {
            int result = handlePrevious(this.fRData.fRTable);
            if (this.fDictionaryCharCount > 0) {
                return checkDictionary(result, startPos, true);
            }
            return result;
        }
        int start = current();
        CharacterIteration.previous32(this.fText);
        int lastResult = handlePrevious(this.fRData.fRTable);
        if (lastResult == -1) {
            lastResult = this.fText.getBeginIndex();
            this.fText.setIndex(lastResult);
        }
        int lastTag = 0;
        boolean breakTagValid = false;
        while (true) {
            int result2 = next();
            if (result2 == -1 || result2 >= start) {
                break;
            }
            lastResult = result2;
            lastTag = this.fLastRuleStatusIndex;
            breakTagValid = true;
        }
        this.fText.setIndex(lastResult);
        this.fLastRuleStatusIndex = lastTag;
        this.fLastStatusIndexValid = breakTagValid;
        return lastResult;
    }

    @Override
    public int following(int offset) {
        CharacterIterator text = getText();
        if (this.fCachedBreakPositions == null || offset < this.fCachedBreakPositions[0] || offset >= this.fCachedBreakPositions[this.fCachedBreakPositions.length - 1]) {
            this.fCachedBreakPositions = null;
            return rulesFollowing(offset);
        }
        this.fPositionInCache = 0;
        while (this.fPositionInCache < this.fCachedBreakPositions.length && offset >= this.fCachedBreakPositions[this.fPositionInCache]) {
            this.fPositionInCache++;
        }
        text.setIndex(this.fCachedBreakPositions[this.fPositionInCache]);
        return text.getIndex();
    }

    private int rulesFollowing(int offset) {
        this.fLastRuleStatusIndex = 0;
        this.fLastStatusIndexValid = true;
        if (this.fText == null || offset >= this.fText.getEndIndex()) {
            last();
            return next();
        }
        if (offset < this.fText.getBeginIndex()) {
            return first();
        }
        if (this.fRData.fSRTable != null) {
            this.fText.setIndex(offset);
            CharacterIteration.next32(this.fText);
            handlePrevious(this.fRData.fSRTable);
            int result = next();
            while (result <= offset) {
                result = next();
            }
            return result;
        }
        if (this.fRData.fSFTable != null) {
            this.fText.setIndex(offset);
            CharacterIteration.previous32(this.fText);
            handleNext(this.fRData.fSFTable);
            int oldresult = previous();
            while (oldresult > offset) {
                int result2 = previous();
                if (result2 <= offset) {
                    return oldresult;
                }
                oldresult = result2;
            }
            int result3 = next();
            if (result3 <= offset) {
                return next();
            }
            return result3;
        }
        this.fText.setIndex(offset);
        if (offset == this.fText.getBeginIndex()) {
            return next();
        }
        int result4 = previous();
        while (result4 != -1 && result4 <= offset) {
            result4 = next();
        }
        return result4;
    }

    @Override
    public int preceding(int offset) {
        CharacterIterator text = getText();
        if (this.fCachedBreakPositions == null || offset <= this.fCachedBreakPositions[0] || offset > this.fCachedBreakPositions[this.fCachedBreakPositions.length - 1]) {
            this.fCachedBreakPositions = null;
            return rulesPreceding(offset);
        }
        this.fPositionInCache = 0;
        while (this.fPositionInCache < this.fCachedBreakPositions.length && offset > this.fCachedBreakPositions[this.fPositionInCache]) {
            this.fPositionInCache++;
        }
        this.fPositionInCache--;
        text.setIndex(this.fCachedBreakPositions[this.fPositionInCache]);
        return text.getIndex();
    }

    private int rulesPreceding(int offset) {
        if (this.fText == null || offset > this.fText.getEndIndex()) {
            return last();
        }
        if (offset < this.fText.getBeginIndex()) {
            return first();
        }
        if (this.fRData.fSFTable != null) {
            this.fText.setIndex(offset);
            CharacterIteration.previous32(this.fText);
            handleNext(this.fRData.fSFTable);
            int result = previous();
            while (result >= offset) {
                result = previous();
            }
            return result;
        }
        if (this.fRData.fSRTable != null) {
            this.fText.setIndex(offset);
            CharacterIteration.next32(this.fText);
            handlePrevious(this.fRData.fSRTable);
            int oldresult = next();
            while (oldresult < offset) {
                int result2 = next();
                if (result2 >= offset) {
                    return oldresult;
                }
                oldresult = result2;
            }
            int result3 = previous();
            if (result3 >= offset) {
                return previous();
            }
            return result3;
        }
        this.fText.setIndex(offset);
        return previous();
    }

    protected static final void checkOffset(int offset, CharacterIterator text) {
        if (offset >= text.getBeginIndex() && offset <= text.getEndIndex()) {
        } else {
            throw new IllegalArgumentException("offset out of bounds");
        }
    }

    @Override
    public boolean isBoundary(int offset) {
        checkOffset(offset, this.fText);
        if (offset == this.fText.getBeginIndex()) {
            first();
            return true;
        }
        if (offset == this.fText.getEndIndex()) {
            last();
            return true;
        }
        this.fText.setIndex(offset);
        CharacterIteration.previous32(this.fText);
        int pos = this.fText.getIndex();
        return following(pos) == offset;
    }

    @Override
    public int current() {
        if (this.fText != null) {
            return this.fText.getIndex();
        }
        return -1;
    }

    private void makeRuleStatusValid() {
        boolean z = false;
        if (this.fLastStatusIndexValid) {
            return;
        }
        int curr = current();
        if (curr == -1 || curr == this.fText.getBeginIndex()) {
            this.fLastRuleStatusIndex = 0;
            this.fLastStatusIndexValid = true;
        } else {
            int pa = this.fText.getIndex();
            first();
            int pb = current();
            while (this.fText.getIndex() < pa) {
                pb = next();
            }
            Assert.assrt(pa == pb);
        }
        Assert.assrt(this.fLastStatusIndexValid);
        if (this.fLastRuleStatusIndex >= 0 && this.fLastRuleStatusIndex < this.fRData.fStatusTable.length) {
            z = true;
        }
        Assert.assrt(z);
    }

    @Override
    public int getRuleStatus() {
        makeRuleStatusValid();
        int idx = this.fLastRuleStatusIndex + this.fRData.fStatusTable[this.fLastRuleStatusIndex];
        int tagVal = this.fRData.fStatusTable[idx];
        return tagVal;
    }

    @Override
    public int getRuleStatusVec(int[] fillInArray) {
        makeRuleStatusValid();
        int numStatusVals = this.fRData.fStatusTable[this.fLastRuleStatusIndex];
        if (fillInArray != null) {
            int numToCopy = Math.min(numStatusVals, fillInArray.length);
            for (int i = 0; i < numToCopy; i++) {
                fillInArray[i] = this.fRData.fStatusTable[this.fLastRuleStatusIndex + i + 1];
            }
        }
        return numStatusVals;
    }

    @Override
    public CharacterIterator getText() {
        return this.fText;
    }

    @Override
    public void setText(CharacterIterator newText) {
        this.fText = newText;
        first();
    }

    void setBreakType(int type) {
        this.fBreakType = type;
    }

    int getBreakType() {
        return this.fBreakType;
    }

    private LanguageBreakEngine getLanguageBreakEngine(int c) {
        LanguageBreakEngine eng;
        LanguageBreakEngine existingEngine;
        for (LanguageBreakEngine candidate : this.fBreakEngines.values()) {
            if (candidate.handles(c, this.fBreakType)) {
                return candidate;
            }
        }
        int script = UCharacter.getIntPropertyValue(c, UProperty.SCRIPT);
        if (script == 22 || script == 20) {
            script = 17;
        }
        this.fBreakEngines.get(Integer.valueOf(script));
        try {
            switch (script) {
                case 17:
                    if (getBreakType() == 1) {
                        eng = new CjkBreakEngine(false);
                    } else {
                        this.fUnhandledBreakEngine.handleChar(c, getBreakType());
                        eng = this.fUnhandledBreakEngine;
                    }
                    break;
                case 18:
                    if (getBreakType() == 1) {
                        eng = new CjkBreakEngine(true);
                    } else {
                        this.fUnhandledBreakEngine.handleChar(c, getBreakType());
                        eng = this.fUnhandledBreakEngine;
                    }
                    break;
                case 23:
                    eng = new KhmerBreakEngine();
                    break;
                case 24:
                    eng = new LaoBreakEngine();
                    break;
                case 28:
                    eng = new BurmeseBreakEngine();
                    break;
                case 38:
                    eng = new ThaiBreakEngine();
                    break;
                default:
                    this.fUnhandledBreakEngine.handleChar(c, getBreakType());
                    eng = this.fUnhandledBreakEngine;
                    break;
            }
        } catch (IOException e) {
            eng = null;
        }
        if (eng != null && eng != this.fUnhandledBreakEngine && (existingEngine = this.fBreakEngines.putIfAbsent(Integer.valueOf(script), eng)) != null) {
            return existingEngine;
        }
        return eng;
    }

    private int handleNext(short[] stateTable) {
        if (TRACE) {
            System.out.println("Handle Next   pos      char  state category");
        }
        this.fLastStatusIndexValid = true;
        this.fLastRuleStatusIndex = 0;
        CharacterIterator text = this.fText;
        CharTrie trie = this.fRData.fTrie;
        int c = text.current();
        if (c >= 55296 && (c = CharacterIteration.nextTrail32(text, c)) == Integer.MAX_VALUE) {
            return -1;
        }
        int initialPosition = text.getIndex();
        int result = initialPosition;
        short s = 1;
        int row = this.fRData.getRowIndex(1);
        short category = 3;
        int flagsState = this.fRData.getStateTableFlags(stateTable);
        int mode = 1;
        if ((flagsState & 2) != 0) {
            category = 2;
            mode = 0;
            if (TRACE) {
                System.out.print("            " + RBBIDataWrapper.intToString(text.getIndex(), 5));
                System.out.print(RBBIDataWrapper.intToHexString(c, 10));
                System.out.println(RBBIDataWrapper.intToString(1, 7) + RBBIDataWrapper.intToString(2, 6));
            }
        }
        short s2 = 0;
        short s3 = 0;
        int lookaheadResult = 0;
        while (true) {
            if (s == 0) {
                break;
            }
            if (c == Integer.MAX_VALUE) {
                if (mode == 2) {
                    if (lookaheadResult > result) {
                        result = lookaheadResult;
                        this.fLastRuleStatusIndex = s3;
                    }
                } else {
                    mode = 2;
                    category = 1;
                }
            } else if (mode == 1) {
                category = (short) trie.getCodePointValue(c);
                if ((category & 16384) != 0) {
                    this.fDictionaryCharCount++;
                    category = (short) (category & (-16385));
                }
                if (TRACE) {
                    System.out.print("            " + RBBIDataWrapper.intToString(text.getIndex(), 5));
                    System.out.print(RBBIDataWrapper.intToHexString(c, 10));
                    System.out.println(RBBIDataWrapper.intToString(s, 7) + RBBIDataWrapper.intToString(category, 6));
                }
                c = text.next();
                if (c >= 55296) {
                    c = CharacterIteration.nextTrail32(text, c);
                }
            } else {
                mode = 1;
            }
            s = stateTable[row + 4 + category];
            row = this.fRData.getRowIndex(s);
            if (stateTable[row + 0] == -1) {
                result = text.getIndex();
                if (c >= 65536 && c <= 1114111) {
                    result--;
                }
                this.fLastRuleStatusIndex = stateTable[row + 2];
            }
            if (stateTable[row + 1] != 0) {
                if (s2 != 0 && stateTable[row + 0] == s2) {
                    result = lookaheadResult;
                    this.fLastRuleStatusIndex = s3;
                    s2 = 0;
                    if ((flagsState & 1) != 0) {
                        text.setIndex(result);
                        return result;
                    }
                } else {
                    lookaheadResult = text.getIndex();
                    if (c >= 65536 && c <= 1114111) {
                        lookaheadResult--;
                    }
                    s2 = stateTable[row + 1];
                    s3 = stateTable[row + 2];
                }
            } else if (stateTable[row + 0] != 0) {
                s2 = 0;
            }
        }
    }

    private int handlePrevious(short[] stateTable) {
        if (this.fText == null || stateTable == null) {
            return 0;
        }
        short s = 0;
        int lookaheadResult = 0;
        boolean lookAheadHardBreak = (this.fRData.getStateTableFlags(stateTable) & 1) != 0;
        this.fLastStatusIndexValid = false;
        this.fLastRuleStatusIndex = 0;
        int initialPosition = this.fText.getIndex();
        int result = initialPosition;
        int c = CharacterIteration.previous32(this.fText);
        short s2 = 1;
        int row = this.fRData.getRowIndex(1);
        int category = 3;
        int mode = 1;
        if ((this.fRData.getStateTableFlags(stateTable) & 2) != 0) {
            category = 2;
            mode = 0;
        }
        if (TRACE) {
            System.out.println("Handle Prev   pos   char  state category ");
        }
        while (true) {
            if (c == Integer.MAX_VALUE) {
                if (mode == 2 || this.fRData.fHeader.fVersion == 1) {
                    break;
                }
                mode = 2;
                category = 1;
                if (mode == 1) {
                }
                if (TRACE) {
                }
                s2 = stateTable[row + 4 + category];
                row = this.fRData.getRowIndex(s2);
                if (stateTable[row + 0] == -1) {
                }
                if (stateTable[row + 1] == 0) {
                }
                if (s2 != 0) {
                }
            } else {
                if (mode == 1) {
                    category = (short) this.fRData.fTrie.getCodePointValue(c);
                    if ((category & 16384) != 0) {
                        this.fDictionaryCharCount++;
                        category &= -16385;
                    }
                }
                if (TRACE) {
                    System.out.print("             " + this.fText.getIndex() + "   ");
                    if (32 <= c && c < 127) {
                        System.out.print("  " + c + "  ");
                    } else {
                        System.out.print(" " + Integer.toHexString(c) + " ");
                    }
                    System.out.println(" " + ((int) s2) + "  " + category + " ");
                }
                s2 = stateTable[row + 4 + category];
                row = this.fRData.getRowIndex(s2);
                if (stateTable[row + 0] == -1) {
                    result = this.fText.getIndex();
                }
                if (stateTable[row + 1] == 0) {
                    if (s != 0 && stateTable[row + 0] == s) {
                        result = lookaheadResult;
                        s = 0;
                        if (lookAheadHardBreak) {
                            break;
                        }
                    } else {
                        lookaheadResult = this.fText.getIndex();
                        s = stateTable[row + 1];
                    }
                } else if (stateTable[row + 0] != 0 && !lookAheadHardBreak) {
                    s = 0;
                }
                if (s2 != 0) {
                    break;
                }
                if (mode == 1) {
                    c = CharacterIteration.previous32(this.fText);
                } else if (mode == 0) {
                    mode = 1;
                }
            }
        }
        if (result == initialPosition) {
            this.fText.setIndex(initialPosition);
            CharacterIteration.previous32(this.fText);
            result = this.fText.getIndex();
        }
        this.fText.setIndex(result);
        if (TRACE) {
            System.out.println("Result = " + result);
        }
        return result;
    }
}
