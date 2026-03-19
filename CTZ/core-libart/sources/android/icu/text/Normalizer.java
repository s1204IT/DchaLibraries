package android.icu.text;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.UCaseProps;
import android.icu.lang.UCharacter;
import android.icu.util.ICUCloneNotSupportedException;
import java.nio.CharBuffer;
import java.text.CharacterIterator;

public final class Normalizer implements Cloneable {
    public static final int COMPARE_CODE_POINT_ORDER = 32768;
    private static final int COMPARE_EQUIV = 524288;
    public static final int COMPARE_IGNORE_CASE = 65536;

    @Deprecated
    public static final int COMPARE_NORM_OPTIONS_SHIFT = 20;

    @Deprecated
    public static final Mode COMPOSE;

    @Deprecated
    public static final Mode COMPOSE_COMPAT;

    @Deprecated
    public static final Mode DECOMP;

    @Deprecated
    public static final Mode DECOMP_COMPAT;

    @Deprecated
    public static final Mode DEFAULT;

    @Deprecated
    public static final int DONE = -1;

    @Deprecated
    public static final Mode FCD;
    public static final int FOLD_CASE_DEFAULT = 0;
    public static final int FOLD_CASE_EXCLUDE_SPECIAL_I = 1;

    @Deprecated
    public static final int IGNORE_HANGUL = 1;
    public static final int INPUT_IS_FCD = 131072;
    public static final QuickCheckResult MAYBE;

    @Deprecated
    public static final Mode NFC;

    @Deprecated
    public static final Mode NFD;

    @Deprecated
    public static final Mode NFKC;

    @Deprecated
    public static final Mode NFKD;
    public static final QuickCheckResult NO;

    @Deprecated
    public static final Mode NONE;

    @Deprecated
    public static final Mode NO_OP;

    @Deprecated
    public static final int UNICODE_3_2 = 32;
    public static final QuickCheckResult YES;
    private StringBuilder buffer;
    private int bufferPos;
    private int currentIndex;
    private Mode mode;
    private int nextIndex;
    private Normalizer2 norm2;
    private int options;
    private UCharacterIterator text;

    private static final class ModeImpl {
        private final Normalizer2 normalizer2;

        private ModeImpl(Normalizer2 normalizer2) {
            this.normalizer2 = normalizer2;
        }
    }

    private static final class NFDModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFDInstance());

        private NFDModeImpl() {
        }
    }

    private static final class NFKDModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFKDInstance());

        private NFKDModeImpl() {
        }
    }

    private static final class NFCModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFCInstance());

        private NFCModeImpl() {
        }
    }

    private static final class NFKCModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFKCInstance());

        private NFKCModeImpl() {
        }
    }

    private static final class FCDModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Norm2AllModes.getFCDNormalizer2());

        private FCDModeImpl() {
        }
    }

    private static final class Unicode32 {
        private static final UnicodeSet INSTANCE = new UnicodeSet("[:age=3.2:]").freeze();

        private Unicode32() {
        }
    }

    private static final class NFD32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFDInstance(), Unicode32.INSTANCE));

        private NFD32ModeImpl() {
        }
    }

    private static final class NFKD32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFKDInstance(), Unicode32.INSTANCE));

        private NFKD32ModeImpl() {
        }
    }

    private static final class NFC32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFCInstance(), Unicode32.INSTANCE));

        private NFC32ModeImpl() {
        }
    }

    private static final class NFKC32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFKCInstance(), Unicode32.INSTANCE));

        private NFKC32ModeImpl() {
        }
    }

    private static final class FCD32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Norm2AllModes.getFCDNormalizer2(), Unicode32.INSTANCE));

        private FCD32ModeImpl() {
        }
    }

    @Deprecated
    public static abstract class Mode {
        @Deprecated
        protected abstract Normalizer2 getNormalizer2(int i);

        @Deprecated
        protected Mode() {
        }
    }

    private static final class NONEMode extends Mode {
        private NONEMode() {
        }

        @Override
        protected Normalizer2 getNormalizer2(int i) {
            return Norm2AllModes.NOOP_NORMALIZER2;
        }
    }

    private static final class NFDMode extends Mode {
        private NFDMode() {
        }

        @Override
        protected Normalizer2 getNormalizer2(int i) {
            ModeImpl modeImpl;
            if ((i & 32) == 0) {
                modeImpl = NFDModeImpl.INSTANCE;
            } else {
                modeImpl = NFD32ModeImpl.INSTANCE;
            }
            return modeImpl.normalizer2;
        }
    }

    private static final class NFKDMode extends Mode {
        private NFKDMode() {
        }

        @Override
        protected Normalizer2 getNormalizer2(int i) {
            ModeImpl modeImpl;
            if ((i & 32) == 0) {
                modeImpl = NFKDModeImpl.INSTANCE;
            } else {
                modeImpl = NFKD32ModeImpl.INSTANCE;
            }
            return modeImpl.normalizer2;
        }
    }

    private static final class NFCMode extends Mode {
        private NFCMode() {
        }

        @Override
        protected Normalizer2 getNormalizer2(int i) {
            ModeImpl modeImpl;
            if ((i & 32) == 0) {
                modeImpl = NFCModeImpl.INSTANCE;
            } else {
                modeImpl = NFC32ModeImpl.INSTANCE;
            }
            return modeImpl.normalizer2;
        }
    }

    private static final class NFKCMode extends Mode {
        private NFKCMode() {
        }

        @Override
        protected Normalizer2 getNormalizer2(int i) {
            ModeImpl modeImpl;
            if ((i & 32) == 0) {
                modeImpl = NFKCModeImpl.INSTANCE;
            } else {
                modeImpl = NFKC32ModeImpl.INSTANCE;
            }
            return modeImpl.normalizer2;
        }
    }

    private static final class FCDMode extends Mode {
        private FCDMode() {
        }

        @Override
        protected Normalizer2 getNormalizer2(int i) {
            ModeImpl modeImpl;
            if ((i & 32) == 0) {
                modeImpl = FCDModeImpl.INSTANCE;
            } else {
                modeImpl = FCD32ModeImpl.INSTANCE;
            }
            return modeImpl.normalizer2;
        }
    }

    static {
        NONE = new NONEMode();
        NFD = new NFDMode();
        NFKD = new NFKDMode();
        NFC = new NFCMode();
        DEFAULT = NFC;
        NFKC = new NFKCMode();
        FCD = new FCDMode();
        NO_OP = NONE;
        COMPOSE = NFC;
        COMPOSE_COMPAT = NFKC;
        DECOMP = NFD;
        DECOMP_COMPAT = NFKD;
        NO = new QuickCheckResult(0);
        YES = new QuickCheckResult(1);
        MAYBE = new QuickCheckResult(2);
    }

    public static final class QuickCheckResult {
        private QuickCheckResult(int i) {
        }
    }

    @Deprecated
    public Normalizer(String str, Mode mode, int i) {
        this.text = UCharacterIterator.getInstance(str);
        this.mode = mode;
        this.options = i;
        this.norm2 = mode.getNormalizer2(i);
        this.buffer = new StringBuilder();
    }

    @Deprecated
    public Normalizer(CharacterIterator characterIterator, Mode mode, int i) {
        this.text = UCharacterIterator.getInstance((CharacterIterator) characterIterator.clone());
        this.mode = mode;
        this.options = i;
        this.norm2 = mode.getNormalizer2(i);
        this.buffer = new StringBuilder();
    }

    @Deprecated
    public Normalizer(UCharacterIterator uCharacterIterator, Mode mode, int i) {
        try {
            this.text = (UCharacterIterator) uCharacterIterator.clone();
            this.mode = mode;
            this.options = i;
            this.norm2 = mode.getNormalizer2(i);
            this.buffer = new StringBuilder();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    @Deprecated
    public Object clone() {
        try {
            Normalizer normalizer = (Normalizer) super.clone();
            normalizer.text = (UCharacterIterator) this.text.clone();
            normalizer.mode = this.mode;
            normalizer.options = this.options;
            normalizer.norm2 = this.norm2;
            normalizer.buffer = new StringBuilder(this.buffer);
            normalizer.bufferPos = this.bufferPos;
            normalizer.currentIndex = this.currentIndex;
            normalizer.nextIndex = this.nextIndex;
            return normalizer;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    private static final Normalizer2 getComposeNormalizer2(boolean z, int i) {
        return (z ? NFKC : NFC).getNormalizer2(i);
    }

    private static final Normalizer2 getDecomposeNormalizer2(boolean z, int i) {
        return (z ? NFKD : NFD).getNormalizer2(i);
    }

    @Deprecated
    public static String compose(String str, boolean z) {
        return compose(str, z, 0);
    }

    @Deprecated
    public static String compose(String str, boolean z, int i) {
        return getComposeNormalizer2(z, i).normalize(str);
    }

    @Deprecated
    public static int compose(char[] cArr, char[] cArr2, boolean z, int i) {
        return compose(cArr, 0, cArr.length, cArr2, 0, cArr2.length, z, i);
    }

    @Deprecated
    public static int compose(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4, boolean z, int i5) {
        CharBuffer charBufferWrap = CharBuffer.wrap(cArr, i, i2 - i);
        CharsAppendable charsAppendable = new CharsAppendable(cArr2, i3, i4);
        getComposeNormalizer2(z, i5).normalize(charBufferWrap, charsAppendable);
        return charsAppendable.length();
    }

    @Deprecated
    public static String decompose(String str, boolean z) {
        return decompose(str, z, 0);
    }

    @Deprecated
    public static String decompose(String str, boolean z, int i) {
        return getDecomposeNormalizer2(z, i).normalize(str);
    }

    @Deprecated
    public static int decompose(char[] cArr, char[] cArr2, boolean z, int i) {
        return decompose(cArr, 0, cArr.length, cArr2, 0, cArr2.length, z, i);
    }

    @Deprecated
    public static int decompose(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4, boolean z, int i5) {
        CharBuffer charBufferWrap = CharBuffer.wrap(cArr, i, i2 - i);
        CharsAppendable charsAppendable = new CharsAppendable(cArr2, i3, i4);
        getDecomposeNormalizer2(z, i5).normalize(charBufferWrap, charsAppendable);
        return charsAppendable.length();
    }

    @Deprecated
    public static String normalize(String str, Mode mode, int i) {
        return mode.getNormalizer2(i).normalize(str);
    }

    @Deprecated
    public static String normalize(String str, Mode mode) {
        return normalize(str, mode, 0);
    }

    @Deprecated
    public static int normalize(char[] cArr, char[] cArr2, Mode mode, int i) {
        return normalize(cArr, 0, cArr.length, cArr2, 0, cArr2.length, mode, i);
    }

    @Deprecated
    public static int normalize(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4, Mode mode, int i5) {
        CharBuffer charBufferWrap = CharBuffer.wrap(cArr, i, i2 - i);
        CharsAppendable charsAppendable = new CharsAppendable(cArr2, i3, i4);
        mode.getNormalizer2(i5).normalize(charBufferWrap, charsAppendable);
        return charsAppendable.length();
    }

    @Deprecated
    public static String normalize(int i, Mode mode, int i2) {
        if (mode == NFD && i2 == 0) {
            String decomposition = Normalizer2.getNFCInstance().getDecomposition(i);
            if (decomposition == null) {
                return UTF16.valueOf(i);
            }
            return decomposition;
        }
        return normalize(UTF16.valueOf(i), mode, i2);
    }

    @Deprecated
    public static String normalize(int i, Mode mode) {
        return normalize(i, mode, 0);
    }

    @Deprecated
    public static QuickCheckResult quickCheck(String str, Mode mode) {
        return quickCheck(str, mode, 0);
    }

    @Deprecated
    public static QuickCheckResult quickCheck(String str, Mode mode, int i) {
        return mode.getNormalizer2(i).quickCheck(str);
    }

    @Deprecated
    public static QuickCheckResult quickCheck(char[] cArr, Mode mode, int i) {
        return quickCheck(cArr, 0, cArr.length, mode, i);
    }

    @Deprecated
    public static QuickCheckResult quickCheck(char[] cArr, int i, int i2, Mode mode, int i3) {
        return mode.getNormalizer2(i3).quickCheck(CharBuffer.wrap(cArr, i, i2 - i));
    }

    @Deprecated
    public static boolean isNormalized(char[] cArr, int i, int i2, Mode mode, int i3) {
        return mode.getNormalizer2(i3).isNormalized(CharBuffer.wrap(cArr, i, i2 - i));
    }

    @Deprecated
    public static boolean isNormalized(String str, Mode mode, int i) {
        return mode.getNormalizer2(i).isNormalized(str);
    }

    @Deprecated
    public static boolean isNormalized(int i, Mode mode, int i2) {
        return isNormalized(UTF16.valueOf(i), mode, i2);
    }

    public static int compare(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4, int i5) {
        if (cArr == null || i < 0 || i2 < 0 || cArr2 == null || i3 < 0 || i4 < 0 || i2 < i || i4 < i3) {
            throw new IllegalArgumentException();
        }
        return internalCompare(CharBuffer.wrap(cArr, i, i2 - i), CharBuffer.wrap(cArr2, i3, i4 - i3), i5);
    }

    public static int compare(String str, String str2, int i) {
        return internalCompare(str, str2, i);
    }

    public static int compare(char[] cArr, char[] cArr2, int i) {
        return internalCompare(CharBuffer.wrap(cArr), CharBuffer.wrap(cArr2), i);
    }

    public static int compare(int i, int i2, int i3) {
        return internalCompare(UTF16.valueOf(i), UTF16.valueOf(i2), i3 | 131072);
    }

    public static int compare(int i, String str, int i2) {
        return internalCompare(UTF16.valueOf(i), str, i2);
    }

    @Deprecated
    public static int concatenate(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4, char[] cArr3, int i5, int i6, Mode mode, int i7) {
        if (cArr3 == null) {
            throw new IllegalArgumentException();
        }
        if (cArr2 == cArr3 && i3 < i6 && i5 < i4) {
            throw new IllegalArgumentException("overlapping right and dst ranges");
        }
        int i8 = i2 - i;
        StringBuilder sb = new StringBuilder(((i8 + i4) - i3) + 16);
        sb.append(cArr, i, i8);
        mode.getNormalizer2(i7).append(sb, CharBuffer.wrap(cArr2, i3, i4 - i3));
        int length = sb.length();
        if (length <= i6 - i5) {
            sb.getChars(0, length, cArr3, i5);
            return length;
        }
        throw new IndexOutOfBoundsException(Integer.toString(length));
    }

    @Deprecated
    public static String concatenate(char[] cArr, char[] cArr2, Mode mode, int i) {
        StringBuilder sb = new StringBuilder(cArr.length + cArr2.length + 16);
        sb.append(cArr);
        return mode.getNormalizer2(i).append(sb, CharBuffer.wrap(cArr2)).toString();
    }

    @Deprecated
    public static String concatenate(String str, String str2, Mode mode, int i) {
        StringBuilder sb = new StringBuilder(str.length() + str2.length() + 16);
        sb.append(str);
        return mode.getNormalizer2(i).append(sb, str2).toString();
    }

    @Deprecated
    public static int getFC_NFKC_Closure(int i, char[] cArr) {
        String fC_NFKC_Closure = getFC_NFKC_Closure(i);
        int length = fC_NFKC_Closure.length();
        if (length != 0 && cArr != null && length <= cArr.length) {
            fC_NFKC_Closure.getChars(0, length, cArr, 0);
        }
        return length;
    }

    @Deprecated
    public static String getFC_NFKC_Closure(int i) {
        Normalizer2 normalizer2 = NFKCModeImpl.INSTANCE.normalizer2;
        UCaseProps uCaseProps = UCaseProps.INSTANCE;
        StringBuilder sb = new StringBuilder();
        int fullFolding = uCaseProps.toFullFolding(i, sb, 0);
        if (fullFolding < 0) {
            Normalizer2Impl normalizer2Impl = ((Norm2AllModes.Normalizer2WithImpl) normalizer2).impl;
            if (normalizer2Impl.getCompQuickCheck(normalizer2Impl.getNorm16(i)) != 0) {
                return "";
            }
            sb.appendCodePoint(i);
        } else if (fullFolding > 31) {
            sb.appendCodePoint(fullFolding);
        }
        String strNormalize = normalizer2.normalize(sb);
        String strNormalize2 = normalizer2.normalize(UCharacter.foldCase(strNormalize, 0));
        if (strNormalize.equals(strNormalize2)) {
            return "";
        }
        return strNormalize2;
    }

    @Deprecated
    public int current() {
        if (this.bufferPos < this.buffer.length() || nextNormalize()) {
            return this.buffer.codePointAt(this.bufferPos);
        }
        return -1;
    }

    @Deprecated
    public int next() {
        if (this.bufferPos < this.buffer.length() || nextNormalize()) {
            int iCodePointAt = this.buffer.codePointAt(this.bufferPos);
            this.bufferPos += Character.charCount(iCodePointAt);
            return iCodePointAt;
        }
        return -1;
    }

    @Deprecated
    public int previous() {
        if (this.bufferPos > 0 || previousNormalize()) {
            int iCodePointBefore = this.buffer.codePointBefore(this.bufferPos);
            this.bufferPos -= Character.charCount(iCodePointBefore);
            return iCodePointBefore;
        }
        return -1;
    }

    @Deprecated
    public void reset() {
        this.text.setToStart();
        this.nextIndex = 0;
        this.currentIndex = 0;
        clearBuffer();
    }

    @Deprecated
    public void setIndexOnly(int i) {
        this.text.setIndex(i);
        this.nextIndex = i;
        this.currentIndex = i;
        clearBuffer();
    }

    @Deprecated
    public int setIndex(int i) {
        setIndexOnly(i);
        return current();
    }

    @Deprecated
    public int getBeginIndex() {
        return 0;
    }

    @Deprecated
    public int getEndIndex() {
        return endIndex();
    }

    @Deprecated
    public int first() {
        reset();
        return next();
    }

    @Deprecated
    public int last() {
        this.text.setToLimit();
        int index = this.text.getIndex();
        this.nextIndex = index;
        this.currentIndex = index;
        clearBuffer();
        return previous();
    }

    @Deprecated
    public int getIndex() {
        if (this.bufferPos < this.buffer.length()) {
            return this.currentIndex;
        }
        return this.nextIndex;
    }

    @Deprecated
    public int startIndex() {
        return 0;
    }

    @Deprecated
    public int endIndex() {
        return this.text.getLength();
    }

    @Deprecated
    public void setMode(Mode mode) {
        this.mode = mode;
        this.norm2 = this.mode.getNormalizer2(this.options);
    }

    @Deprecated
    public Mode getMode() {
        return this.mode;
    }

    @Deprecated
    public void setOption(int i, boolean z) {
        if (z) {
            this.options = i | this.options;
        } else {
            this.options = (~i) & this.options;
        }
        this.norm2 = this.mode.getNormalizer2(this.options);
    }

    @Deprecated
    public int getOption(int i) {
        if ((i & this.options) != 0) {
            return 1;
        }
        return 0;
    }

    @Deprecated
    public int getText(char[] cArr) {
        return this.text.getText(cArr);
    }

    @Deprecated
    public int getLength() {
        return this.text.getLength();
    }

    @Deprecated
    public String getText() {
        return this.text.getText();
    }

    @Deprecated
    public void setText(StringBuffer stringBuffer) {
        UCharacterIterator uCharacterIterator = UCharacterIterator.getInstance(stringBuffer);
        if (uCharacterIterator == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }
        this.text = uCharacterIterator;
        reset();
    }

    @Deprecated
    public void setText(char[] cArr) {
        UCharacterIterator uCharacterIterator = UCharacterIterator.getInstance(cArr);
        if (uCharacterIterator == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }
        this.text = uCharacterIterator;
        reset();
    }

    @Deprecated
    public void setText(String str) {
        UCharacterIterator uCharacterIterator = UCharacterIterator.getInstance(str);
        if (uCharacterIterator == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }
        this.text = uCharacterIterator;
        reset();
    }

    @Deprecated
    public void setText(CharacterIterator characterIterator) {
        UCharacterIterator uCharacterIterator = UCharacterIterator.getInstance(characterIterator);
        if (uCharacterIterator == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }
        this.text = uCharacterIterator;
        reset();
    }

    @Deprecated
    public void setText(UCharacterIterator uCharacterIterator) {
        try {
            UCharacterIterator uCharacterIterator2 = (UCharacterIterator) uCharacterIterator.clone();
            if (uCharacterIterator2 == null) {
                throw new IllegalStateException("Could not create a new UCharacterIterator");
            }
            this.text = uCharacterIterator2;
            reset();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("Could not clone the UCharacterIterator", e);
        }
    }

    private void clearBuffer() {
        this.buffer.setLength(0);
        this.bufferPos = 0;
    }

    private boolean nextNormalize() {
        clearBuffer();
        this.currentIndex = this.nextIndex;
        this.text.setIndex(this.nextIndex);
        int iNextCodePoint = this.text.nextCodePoint();
        if (iNextCodePoint < 0) {
            return false;
        }
        StringBuilder sbAppendCodePoint = new StringBuilder().appendCodePoint(iNextCodePoint);
        while (true) {
            int iNextCodePoint2 = this.text.nextCodePoint();
            if (iNextCodePoint2 < 0) {
                break;
            }
            if (this.norm2.hasBoundaryBefore(iNextCodePoint2)) {
                this.text.moveCodePointIndex(-1);
                break;
            }
            sbAppendCodePoint.appendCodePoint(iNextCodePoint2);
        }
        this.nextIndex = this.text.getIndex();
        this.norm2.normalize((CharSequence) sbAppendCodePoint, this.buffer);
        return this.buffer.length() != 0;
    }

    private boolean previousNormalize() {
        int iPreviousCodePoint;
        clearBuffer();
        this.nextIndex = this.currentIndex;
        this.text.setIndex(this.currentIndex);
        StringBuilder sb = new StringBuilder();
        do {
            iPreviousCodePoint = this.text.previousCodePoint();
            if (iPreviousCodePoint < 0) {
                break;
            }
            if (iPreviousCodePoint <= 65535) {
                sb.insert(0, (char) iPreviousCodePoint);
            } else {
                sb.insert(0, Character.toChars(iPreviousCodePoint));
            }
        } while (!this.norm2.hasBoundaryBefore(iPreviousCodePoint));
        this.currentIndex = this.text.getIndex();
        this.norm2.normalize((CharSequence) sb, this.buffer);
        this.bufferPos = this.buffer.length();
        return this.buffer.length() != 0;
    }

    private static int internalCompare(CharSequence charSequence, CharSequence charSequence2, int i) {
        Normalizer2 normalizer2;
        int i2 = i >>> 20;
        int i3 = i | 524288;
        if ((131072 & i3) == 0 || (i3 & 1) != 0) {
            if ((i3 & 1) != 0) {
                normalizer2 = NFD.getNormalizer2(i2);
            } else {
                normalizer2 = FCD.getNormalizer2(i2);
            }
            int iSpanQuickCheckYes = normalizer2.spanQuickCheckYes(charSequence);
            int iSpanQuickCheckYes2 = normalizer2.spanQuickCheckYes(charSequence2);
            if (iSpanQuickCheckYes < charSequence.length()) {
                StringBuilder sb = new StringBuilder(charSequence.length() + 16);
                sb.append(charSequence, 0, iSpanQuickCheckYes);
                charSequence = normalizer2.normalizeSecondAndAppend(sb, charSequence.subSequence(iSpanQuickCheckYes, charSequence.length()));
            }
            if (iSpanQuickCheckYes2 < charSequence2.length()) {
                StringBuilder sb2 = new StringBuilder(charSequence2.length() + 16);
                sb2.append(charSequence2, 0, iSpanQuickCheckYes2);
                charSequence2 = normalizer2.normalizeSecondAndAppend(sb2, charSequence2.subSequence(iSpanQuickCheckYes2, charSequence2.length()));
            }
        }
        return cmpEquivFold(charSequence, charSequence2, i3);
    }

    private static final class CmpEquivLevel {
        CharSequence cs;
        int s;

        private CmpEquivLevel() {
        }
    }

    private static final CmpEquivLevel[] createCmpEquivLevelStack() {
        return new CmpEquivLevel[]{new CmpEquivLevel(), new CmpEquivLevel()};
    }

    static int cmpEquivFold(CharSequence charSequence, CharSequence charSequence2, int i) {
        UCaseProps uCaseProps;
        StringBuilder sb;
        StringBuilder sb2;
        int i2;
        ?? r13;
        int i3;
        int iCharAt;
        int i4;
        int i5;
        Normalizer2Impl normalizer2Impl;
        CharSequence charSequence3;
        int i6;
        int i7;
        int i8;
        char c;
        int i9;
        int codePoint;
        char c2;
        int i10;
        int codePoint2;
        int iCharAt2;
        int i11;
        StringBuilder sb3;
        int iCharAt3;
        Normalizer2Impl normalizer2Impl2;
        String decomposition;
        int i12;
        int fullFolding;
        CharSequence charSequence4;
        CharSequence charSequence5;
        int i13 = 524288 & i;
        Normalizer2Impl normalizer2Impl3 = i13 != 0 ? Norm2AllModes.getNFCInstance().impl : null;
        int i14 = 65536 & i;
        if (i14 != 0) {
            uCaseProps = UCaseProps.INSTANCE;
            sb = new StringBuilder();
            sb2 = new StringBuilder();
        } else {
            uCaseProps = null;
            sb = null;
            sb2 = null;
        }
        int i15 = -1;
        ?? decomposition2 = charSequence;
        CharSequence charSequence6 = charSequence2;
        int length = charSequence.length();
        int length2 = charSequence2.length();
        int iCharAt4 = -1;
        int iCharAt5 = -1;
        int i16 = 0;
        CmpEquivLevel[] cmpEquivLevelArrCreateCmpEquivLevelStack = null;
        int i17 = 0;
        int i18 = 0;
        CmpEquivLevel[] cmpEquivLevelArrCreateCmpEquivLevelStack2 = null;
        int i19 = 0;
        while (true) {
            if (iCharAt4 < 0) {
                int length3 = length;
                ?? r12 = decomposition2;
                while (true) {
                    if (i17 != length3) {
                        int i20 = i17 + 1;
                        iCharAt = r12.charAt(i17);
                        i2 = i16;
                        r13 = r12;
                        i3 = length3;
                        i4 = i20;
                        break;
                    }
                    if (i16 == 0) {
                        i2 = i16;
                        r13 = r12;
                        i3 = length3;
                        i4 = i17;
                        iCharAt = i15;
                        break;
                    }
                    do {
                        i16 += i15;
                        charSequence5 = cmpEquivLevelArrCreateCmpEquivLevelStack[i16].cs;
                    } while (charSequence5 == null);
                    i17 = cmpEquivLevelArrCreateCmpEquivLevelStack[i16].s;
                    length3 = charSequence5.length();
                    r12 = charSequence5;
                }
            } else {
                i2 = i16;
                r13 = decomposition2;
                i3 = length;
                int i21 = i17;
                iCharAt = iCharAt4;
                i4 = i21;
            }
            if (iCharAt5 < 0) {
                CharSequence charSequence7 = charSequence6;
                int i22 = i19;
                i8 = length2;
                while (true) {
                    if (i22 != i8) {
                        int i23 = i22 + 1;
                        CharSequence charSequence8 = charSequence7;
                        iCharAt5 = charSequence8.charAt(i22);
                        i5 = i13;
                        normalizer2Impl = normalizer2Impl3;
                        charSequence3 = charSequence8;
                        i7 = i23;
                        i6 = i18;
                        break;
                    }
                    if (i18 == 0) {
                        i5 = i13;
                        normalizer2Impl = normalizer2Impl3;
                        i6 = i18;
                        charSequence3 = charSequence7;
                        int i24 = i15;
                        i7 = i22;
                        iCharAt5 = i24;
                        break;
                    }
                    do {
                        i18--;
                        charSequence4 = cmpEquivLevelArrCreateCmpEquivLevelStack2[i18].cs;
                    } while (charSequence4 == null);
                    int i25 = cmpEquivLevelArrCreateCmpEquivLevelStack2[i18].s;
                    int length4 = charSequence4.length();
                    charSequence7 = charSequence4;
                    i22 = i25;
                    i8 = length4;
                }
            } else {
                i5 = i13;
                normalizer2Impl = normalizer2Impl3;
                charSequence3 = charSequence6;
                i6 = i18;
                i7 = i19;
                i8 = length2;
            }
            if (iCharAt == iCharAt5) {
                if (iCharAt < 0) {
                    return 0;
                }
                i18 = i6;
                charSequence6 = charSequence3;
                i17 = i4;
                i19 = i7;
                length2 = i8;
                length = i3;
                decomposition2 = r13;
                normalizer2Impl3 = normalizer2Impl;
                i13 = i5;
                iCharAt4 = -1;
                iCharAt5 = -1;
                i15 = -1;
            } else {
                if (iCharAt < 0) {
                    return -1;
                }
                if (iCharAt5 < 0) {
                    return 1;
                }
                StringBuilder sb4 = sb2;
                c = (char) iCharAt;
                if (!UTF16.isSurrogate(c)) {
                    i9 = i3;
                    int i26 = i6;
                    c2 = (char) iCharAt5;
                    if (!UTF16.isSurrogate(c2)) {
                        i10 = i8;
                        iCharAt2 = iCharAt5;
                        if (i2 != 0 && i14 != 0 && (fullFolding = uCaseProps.toFullFolding(codePoint, sb, i)) >= 0) {
                            if (UTF16.isSurrogate(c)) {
                                if (Normalizer2Impl.UTF16Plus.isSurrogateLead(iCharAt)) {
                                    i4++;
                                } else {
                                    i7--;
                                    iCharAt2 = charSequence3.charAt(i7 - 1);
                                }
                            }
                            i19 = i7;
                            if (cmpEquivLevelArrCreateCmpEquivLevelStack == null) {
                                cmpEquivLevelArrCreateCmpEquivLevelStack = createCmpEquivLevelStack();
                            }
                            cmpEquivLevelArrCreateCmpEquivLevelStack[0].cs = r13;
                            cmpEquivLevelArrCreateCmpEquivLevelStack[0].s = i4;
                            i16 = i2 + 1;
                            if (fullFolding <= 31) {
                                sb.delete(0, sb.length() - fullFolding);
                            } else {
                                sb.setLength(0);
                                sb.appendCodePoint(fullFolding);
                            }
                            charSequence6 = charSequence3;
                            decomposition2 = sb;
                            iCharAt4 = -1;
                            i15 = -1;
                            normalizer2Impl3 = normalizer2Impl;
                            sb2 = sb4;
                            i18 = i26;
                            length2 = i10;
                            iCharAt5 = iCharAt2;
                            i17 = 0;
                            length = sb.length();
                            i13 = i5;
                        } else if (i26 == 0 || i14 == 0) {
                            i11 = i14;
                            sb3 = sb4;
                            iCharAt3 = iCharAt2;
                            UCaseProps uCaseProps2 = uCaseProps;
                            if (i2 < 2 && i5 != 0) {
                                normalizer2Impl2 = normalizer2Impl;
                                decomposition2 = normalizer2Impl2.getDecomposition(codePoint);
                                if (decomposition2 == 0) {
                                    int i27 = i2;
                                    if (i26 >= 2) {
                                        break;
                                    }
                                    break;
                                }
                                if (UTF16.isSurrogate(c)) {
                                    if (Normalizer2Impl.UTF16Plus.isSurrogateLead(iCharAt)) {
                                        i4++;
                                    } else {
                                        i7--;
                                        iCharAt3 = charSequence3.charAt(i7 - 1);
                                    }
                                }
                                i19 = i7;
                                if (cmpEquivLevelArrCreateCmpEquivLevelStack == null) {
                                    cmpEquivLevelArrCreateCmpEquivLevelStack = createCmpEquivLevelStack();
                                }
                                cmpEquivLevelArrCreateCmpEquivLevelStack[i2].cs = r13;
                                cmpEquivLevelArrCreateCmpEquivLevelStack[i2].s = i4;
                                int i28 = i2 + 1;
                                if (i28 < 2) {
                                    cmpEquivLevelArrCreateCmpEquivLevelStack[i28].cs = null;
                                    i16 = i28 + 1;
                                } else {
                                    i16 = i28;
                                }
                                sb2 = sb3;
                                iCharAt4 = -1;
                                i15 = -1;
                                i17 = 0;
                                i18 = i26;
                                length2 = i10;
                                length = decomposition2.length();
                                charSequence6 = charSequence3;
                                iCharAt5 = iCharAt3;
                                normalizer2Impl3 = normalizer2Impl2;
                                i13 = i5;
                                i14 = i11;
                                uCaseProps = uCaseProps2;
                            } else {
                                normalizer2Impl2 = normalizer2Impl;
                                int i272 = i2;
                                if (i26 >= 2 || i5 == 0 || (decomposition = normalizer2Impl2.getDecomposition(codePoint2)) == null) {
                                    break;
                                }
                                if (!UTF16.isSurrogate(c2)) {
                                    int i29 = iCharAt;
                                    i17 = i4;
                                    iCharAt4 = i29;
                                    if (cmpEquivLevelArrCreateCmpEquivLevelStack2 == null) {
                                        cmpEquivLevelArrCreateCmpEquivLevelStack2 = createCmpEquivLevelStack();
                                    }
                                    cmpEquivLevelArrCreateCmpEquivLevelStack2[i26].cs = charSequence3;
                                    cmpEquivLevelArrCreateCmpEquivLevelStack2[i26].s = i7;
                                    i12 = i26 + 1;
                                    if (i12 < 2) {
                                        cmpEquivLevelArrCreateCmpEquivLevelStack2[i12].cs = null;
                                        i18 = i12 + 1;
                                    } else {
                                        i18 = i12;
                                    }
                                    length2 = decomposition.length();
                                    normalizer2Impl3 = normalizer2Impl2;
                                    sb2 = sb3;
                                    decomposition2 = r13;
                                    iCharAt5 = -1;
                                    i15 = -1;
                                    i19 = 0;
                                    i13 = i5;
                                    length = i9;
                                    i14 = i11;
                                    uCaseProps = uCaseProps2;
                                    i16 = i272;
                                    charSequence6 = decomposition;
                                } else if (Normalizer2Impl.UTF16Plus.isSurrogateLead(iCharAt3)) {
                                    i7++;
                                    int i292 = iCharAt;
                                    i17 = i4;
                                    iCharAt4 = i292;
                                    if (cmpEquivLevelArrCreateCmpEquivLevelStack2 == null) {
                                    }
                                    cmpEquivLevelArrCreateCmpEquivLevelStack2[i26].cs = charSequence3;
                                    cmpEquivLevelArrCreateCmpEquivLevelStack2[i26].s = i7;
                                    i12 = i26 + 1;
                                    if (i12 < 2) {
                                    }
                                    length2 = decomposition.length();
                                    normalizer2Impl3 = normalizer2Impl2;
                                    sb2 = sb3;
                                    decomposition2 = r13;
                                    iCharAt5 = -1;
                                    i15 = -1;
                                    i19 = 0;
                                    i13 = i5;
                                    length = i9;
                                    i14 = i11;
                                    uCaseProps = uCaseProps2;
                                    i16 = i272;
                                    charSequence6 = decomposition;
                                } else {
                                    int i30 = i4 - 1;
                                    i17 = i30;
                                    iCharAt4 = r13.charAt(i30 - 1);
                                    if (cmpEquivLevelArrCreateCmpEquivLevelStack2 == null) {
                                    }
                                    cmpEquivLevelArrCreateCmpEquivLevelStack2[i26].cs = charSequence3;
                                    cmpEquivLevelArrCreateCmpEquivLevelStack2[i26].s = i7;
                                    i12 = i26 + 1;
                                    if (i12 < 2) {
                                    }
                                    length2 = decomposition.length();
                                    normalizer2Impl3 = normalizer2Impl2;
                                    sb2 = sb3;
                                    decomposition2 = r13;
                                    iCharAt5 = -1;
                                    i15 = -1;
                                    i19 = 0;
                                    i13 = i5;
                                    length = i9;
                                    i14 = i11;
                                    uCaseProps = uCaseProps2;
                                    i16 = i272;
                                    charSequence6 = decomposition;
                                }
                            }
                        } else {
                            i11 = i14;
                            sb3 = sb4;
                            int fullFolding2 = uCaseProps.toFullFolding(codePoint2, sb3, i);
                            if (fullFolding2 < 0) {
                                iCharAt3 = iCharAt2;
                                UCaseProps uCaseProps22 = uCaseProps;
                                if (i2 < 2) {
                                }
                                normalizer2Impl2 = normalizer2Impl;
                                int i2722 = i2;
                                if (i26 >= 2) {
                                }
                            } else if (!UTF16.isSurrogate(c2)) {
                                int i31 = iCharAt;
                                i17 = i4;
                                iCharAt4 = i31;
                                if (cmpEquivLevelArrCreateCmpEquivLevelStack2 == null) {
                                    cmpEquivLevelArrCreateCmpEquivLevelStack2 = createCmpEquivLevelStack();
                                }
                                cmpEquivLevelArrCreateCmpEquivLevelStack2[0].cs = charSequence3;
                                cmpEquivLevelArrCreateCmpEquivLevelStack2[0].s = i7;
                                i18 = i26 + 1;
                                if (fullFolding2 <= 31) {
                                    sb3.delete(0, sb3.length() - fullFolding2);
                                } else {
                                    sb3.setLength(0);
                                    sb3.appendCodePoint(fullFolding2);
                                }
                                length2 = sb3.length();
                                i19 = 0;
                                sb2 = sb3;
                                charSequence6 = sb2;
                                decomposition2 = r13;
                                iCharAt5 = -1;
                                i15 = -1;
                                normalizer2Impl3 = normalizer2Impl;
                                i13 = i5;
                                length = i9;
                                i14 = i11;
                            } else if (Normalizer2Impl.UTF16Plus.isSurrogateLead(iCharAt2)) {
                                i7++;
                                int i312 = iCharAt;
                                i17 = i4;
                                iCharAt4 = i312;
                                if (cmpEquivLevelArrCreateCmpEquivLevelStack2 == null) {
                                }
                                cmpEquivLevelArrCreateCmpEquivLevelStack2[0].cs = charSequence3;
                                cmpEquivLevelArrCreateCmpEquivLevelStack2[0].s = i7;
                                i18 = i26 + 1;
                                if (fullFolding2 <= 31) {
                                }
                                length2 = sb3.length();
                                i19 = 0;
                                sb2 = sb3;
                                charSequence6 = sb2;
                                decomposition2 = r13;
                                iCharAt5 = -1;
                                i15 = -1;
                                normalizer2Impl3 = normalizer2Impl;
                                i13 = i5;
                                length = i9;
                                i14 = i11;
                            } else {
                                int i32 = i4 - 1;
                                i17 = i32;
                                iCharAt4 = r13.charAt(i32 - 1);
                                if (cmpEquivLevelArrCreateCmpEquivLevelStack2 == null) {
                                }
                                cmpEquivLevelArrCreateCmpEquivLevelStack2[0].cs = charSequence3;
                                cmpEquivLevelArrCreateCmpEquivLevelStack2[0].s = i7;
                                i18 = i26 + 1;
                                if (fullFolding2 <= 31) {
                                }
                                length2 = sb3.length();
                                i19 = 0;
                                sb2 = sb3;
                                charSequence6 = sb2;
                                decomposition2 = r13;
                                iCharAt5 = -1;
                                i15 = -1;
                                normalizer2Impl3 = normalizer2Impl;
                                i13 = i5;
                                length = i9;
                                i14 = i11;
                            }
                        }
                    } else if (Normalizer2Impl.UTF16Plus.isSurrogateLead(iCharAt5)) {
                        if (i7 != i8) {
                            i10 = i8;
                            char cCharAt = charSequence3.charAt(i7);
                            if (Character.isLowSurrogate(cCharAt)) {
                                codePoint2 = Character.toCodePoint(c2, cCharAt);
                            }
                            iCharAt2 = iCharAt5;
                            if (i2 != 0) {
                            }
                            if (i26 == 0) {
                            }
                            i11 = i14;
                            sb3 = sb4;
                            iCharAt3 = iCharAt2;
                            UCaseProps uCaseProps222 = uCaseProps;
                            if (i2 < 2) {
                            }
                            normalizer2Impl2 = normalizer2Impl;
                            int i27222 = i2;
                            if (i26 >= 2) {
                            }
                        }
                        iCharAt2 = iCharAt5;
                        if (i2 != 0) {
                        }
                        if (i26 == 0) {
                        }
                        i11 = i14;
                        sb3 = sb4;
                        iCharAt3 = iCharAt2;
                        UCaseProps uCaseProps2222 = uCaseProps;
                        if (i2 < 2) {
                        }
                        normalizer2Impl2 = normalizer2Impl;
                        int i272222 = i2;
                        if (i26 >= 2) {
                        }
                    } else {
                        i10 = i8;
                        int i33 = i7 - 2;
                        if (i33 >= 0) {
                            char cCharAt2 = charSequence3.charAt(i33);
                            codePoint2 = Character.isHighSurrogate(cCharAt2) ? Character.toCodePoint(cCharAt2, c2) : iCharAt5;
                            iCharAt2 = iCharAt5;
                            if (i2 != 0) {
                            }
                            if (i26 == 0) {
                            }
                            i11 = i14;
                            sb3 = sb4;
                            iCharAt3 = iCharAt2;
                            UCaseProps uCaseProps22222 = uCaseProps;
                            if (i2 < 2) {
                            }
                            normalizer2Impl2 = normalizer2Impl;
                            int i2722222 = i2;
                            if (i26 >= 2) {
                            }
                        }
                    }
                } else if (Normalizer2Impl.UTF16Plus.isSurrogateLead(iCharAt)) {
                    if (i4 != i3) {
                        i9 = i3;
                        char cCharAt3 = r13.charAt(i4);
                        if (Character.isLowSurrogate(cCharAt3)) {
                            codePoint = Character.toCodePoint(c, cCharAt3);
                        }
                        int i262 = i6;
                        c2 = (char) iCharAt5;
                        if (!UTF16.isSurrogate(c2)) {
                        }
                    }
                    int i2622 = i6;
                    c2 = (char) iCharAt5;
                    if (!UTF16.isSurrogate(c2)) {
                    }
                } else {
                    i9 = i3;
                    int i34 = i4 - 2;
                    if (i34 >= 0) {
                        char cCharAt4 = r13.charAt(i34);
                        codePoint = Character.isHighSurrogate(cCharAt4) ? Character.toCodePoint(cCharAt4, c) : iCharAt;
                        int i26222 = i6;
                        c2 = (char) iCharAt5;
                        if (!UTF16.isSurrogate(c2)) {
                        }
                    }
                }
            }
            i16 = i2;
        }
        if (iCharAt >= 55296 && iCharAt3 >= 55296 && (i & 32768) != 0) {
            if ((iCharAt > 56319 || i4 == i9 || !Character.isLowSurrogate(r13.charAt(i4))) && (!Character.isLowSurrogate(c) || i4 - 1 == 0 || !Character.isHighSurrogate(r13.charAt(i4 - 2)))) {
                iCharAt -= 10240;
            }
            if ((iCharAt3 > 56319 || i7 == i10 || !Character.isLowSurrogate(charSequence3.charAt(i7))) && (!Character.isLowSurrogate(c2) || i7 - 1 == 0 || !Character.isHighSurrogate(charSequence3.charAt(i7 - 2)))) {
                iCharAt3 -= 10240;
            }
        }
        return iCharAt - iCharAt3;
    }

    private static final class CharsAppendable implements Appendable {
        private final char[] chars;
        private final int limit;
        private int offset;
        private final int start;

        public CharsAppendable(char[] cArr, int i, int i2) {
            this.chars = cArr;
            this.offset = i;
            this.start = i;
            this.limit = i2;
        }

        public int length() {
            int i = this.offset - this.start;
            if (this.offset <= this.limit) {
                return i;
            }
            throw new IndexOutOfBoundsException(Integer.toString(i));
        }

        @Override
        public Appendable append(char c) {
            if (this.offset < this.limit) {
                this.chars[this.offset] = c;
            }
            this.offset++;
            return this;
        }

        @Override
        public Appendable append(CharSequence charSequence) {
            return append(charSequence, 0, charSequence.length());
        }

        @Override
        public Appendable append(CharSequence charSequence, int i, int i2) {
            int i3 = i2 - i;
            if (i3 <= this.limit - this.offset) {
                while (i < i2) {
                    char[] cArr = this.chars;
                    int i4 = this.offset;
                    this.offset = i4 + 1;
                    cArr[i4] = charSequence.charAt(i);
                    i++;
                }
            } else {
                this.offset += i3;
            }
            return this;
        }
    }
}
