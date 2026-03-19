package android.icu.impl.coll;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2;
import android.icu.impl.Trie2Writable;
import android.icu.lang.UCharacter;
import android.icu.text.UnicodeSet;
import android.icu.text.UnicodeSetIterator;
import android.icu.util.CharsTrie;
import android.icu.util.CharsTrieBuilder;
import android.icu.util.StringTrieBuilder;
import dalvik.bytecode.Opcodes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

final class CollationDataBuilder {
    static final boolean $assertionsDisabled = false;
    private static final int IS_BUILDER_JAMO_CE32 = 256;
    protected UnicodeSet contextChars = new UnicodeSet();
    protected StringBuilder contexts = new StringBuilder();
    protected UnicodeSet unsafeBackwardSet = new UnicodeSet();
    protected Normalizer2Impl nfcImpl = Norm2AllModes.getNFCInstance().impl;
    protected CollationData base = null;
    protected CollationSettings baseSettings = null;
    protected Trie2Writable trie = null;
    protected UVector32 ce32s = new UVector32();
    protected UVector64 ce64s = new UVector64();
    protected ArrayList<ConditionalCE32> conditionalCE32s = new ArrayList<>();
    protected boolean modified = false;
    protected boolean fastLatinEnabled = false;
    protected CollationFastLatinBuilder fastLatinBuilder = null;
    protected DataBuilderCollationIterator collIter = null;

    interface CEModifier {
        long modifyCE(long j);

        long modifyCE32(int i);
    }

    CollationDataBuilder() {
        this.ce32s.addElement(0);
    }

    void initForTailoring(CollationData collationData) {
        if (this.trie != null) {
            throw new IllegalStateException("attempt to reuse a CollationDataBuilder");
        }
        if (collationData == null) {
            throw new IllegalArgumentException("null CollationData");
        }
        this.base = collationData;
        this.trie = new Trie2Writable(192, -195323);
        for (int i = 192; i <= 255; i++) {
            this.trie.set(i, 192);
        }
        this.trie.setRange(Normalizer2Impl.Hangul.HANGUL_BASE, Normalizer2Impl.Hangul.HANGUL_END, Collation.makeCE32FromTagAndIndex(12, 0), true);
        this.unsafeBackwardSet.addAll(collationData.unsafeBackwardSet);
    }

    boolean isCompressibleLeadByte(int i) {
        return this.base.isCompressibleLeadByte(i);
    }

    boolean isCompressiblePrimary(long j) {
        return isCompressibleLeadByte(((int) j) >>> 24);
    }

    boolean hasMappings() {
        return this.modified;
    }

    boolean isAssigned(int i) {
        return Collation.isAssignedCE32(this.trie.get(i));
    }

    void add(CharSequence charSequence, CharSequence charSequence2, long[] jArr, int i) {
        addCE32(charSequence, charSequence2, encodeCEs(jArr, i));
    }

    int encodeCEs(long[] jArr, int i) {
        if (i < 0 || i > 31) {
            throw new IllegalArgumentException("mapping to too many CEs");
        }
        if (isMutable()) {
            if (i != 0) {
                if (i != 1) {
                    if (i == 2) {
                        long j = jArr[0];
                        long j2 = jArr[1];
                        long j3 = j >>> 32;
                        if ((72057594037862655L & j) == 83886080 && ((-4278190081L) & j2) == 1280 && j3 != 0) {
                            return ((int) j3) | ((((int) j) & 65280) << 8) | ((((int) j2) >> 16) & 65280) | 192 | 4;
                        }
                    }
                    int[] iArr = new int[31];
                    for (int i2 = 0; i2 != i; i2++) {
                        int iEncodeOneCEAsCE32 = encodeOneCEAsCE32(jArr[i2]);
                        if (iEncodeOneCEAsCE32 != 1) {
                            iArr[i2] = iEncodeOneCEAsCE32;
                        } else {
                            return encodeExpansion(jArr, 0, i);
                        }
                    }
                    return encodeExpansion32(iArr, 0, i);
                }
                return encodeOneCE(jArr[0]);
            }
            return encodeOneCEAsCE32(0L);
        }
        throw new IllegalStateException("attempt to add mappings after build()");
    }

    void addCE32(CharSequence charSequence, CharSequence charSequence2, int i) {
        ConditionalCE32 conditionalCE32ForCE32;
        if (charSequence2.length() == 0) {
            throw new IllegalArgumentException("mapping from empty string");
        }
        if (!isMutable()) {
            throw new IllegalStateException("attempt to add mappings after build()");
        }
        boolean z = false;
        int iCodePointAt = Character.codePointAt(charSequence2, 0);
        int iCharCount = Character.charCount(iCodePointAt);
        int iCopyFromBaseCE32 = this.trie.get(iCodePointAt);
        if (charSequence.length() != 0 || charSequence2.length() > iCharCount) {
            z = true;
        }
        if (iCopyFromBaseCE32 == 192) {
            int finalCE32 = this.base.getFinalCE32(this.base.getCE32(iCodePointAt));
            if (z || Collation.ce32HasContext(finalCE32)) {
                iCopyFromBaseCE32 = copyFromBaseCE32(iCodePointAt, finalCE32, true);
                this.trie.set(iCodePointAt, iCopyFromBaseCE32);
            }
        }
        if (!z) {
            if (!isBuilderContextCE32(iCopyFromBaseCE32)) {
                this.trie.set(iCodePointAt, i);
            } else {
                ConditionalCE32 conditionalCE32ForCE322 = getConditionalCE32ForCE32(iCopyFromBaseCE32);
                conditionalCE32ForCE322.builtCE32 = 1;
                conditionalCE32ForCE322.ce32 = i;
            }
        } else {
            if (!isBuilderContextCE32(iCopyFromBaseCE32)) {
                int iAddConditionalCE32 = addConditionalCE32("\u0000", iCopyFromBaseCE32);
                this.trie.set(iCodePointAt, makeBuilderContextCE32(iAddConditionalCE32));
                this.contextChars.add(iCodePointAt);
                conditionalCE32ForCE32 = getConditionalCE32(iAddConditionalCE32);
            } else {
                conditionalCE32ForCE32 = getConditionalCE32ForCE32(iCopyFromBaseCE32);
                conditionalCE32ForCE32.builtCE32 = 1;
            }
            CharSequence charSequenceSubSequence = charSequence2.subSequence(iCharCount, charSequence2.length());
            StringBuilder sb = new StringBuilder();
            sb.append((char) charSequence.length());
            sb.append(charSequence);
            sb.append(charSequenceSubSequence);
            String string = sb.toString();
            this.unsafeBackwardSet.addAll(charSequenceSubSequence);
            while (true) {
                int i2 = conditionalCE32ForCE32.next;
                if (i2 < 0) {
                    conditionalCE32ForCE32.next = addConditionalCE32(string, i);
                    break;
                }
                ConditionalCE32 conditionalCE32 = getConditionalCE32(i2);
                int iCompareTo = string.compareTo(conditionalCE32.context);
                if (iCompareTo < 0) {
                    int iAddConditionalCE322 = addConditionalCE32(string, i);
                    conditionalCE32ForCE32.next = iAddConditionalCE322;
                    getConditionalCE32(iAddConditionalCE322).next = i2;
                    break;
                } else if (iCompareTo != 0) {
                    conditionalCE32ForCE32 = conditionalCE32;
                } else {
                    conditionalCE32.ce32 = i;
                    break;
                }
            }
        }
        this.modified = true;
    }

    void copyFrom(CollationDataBuilder collationDataBuilder, CEModifier cEModifier) {
        if (!isMutable()) {
            throw new IllegalStateException("attempt to copyFrom() after build()");
        }
        CopyHelper copyHelper = new CopyHelper(collationDataBuilder, this, cEModifier);
        for (Trie2.Range range : collationDataBuilder.trie) {
            if (range.leadSurrogate) {
                break;
            } else {
                enumRangeForCopy(range.startCodePoint, range.endCodePoint, range.value, copyHelper);
            }
        }
        this.modified = collationDataBuilder.modified | this.modified;
    }

    void optimize(UnicodeSet unicodeSet) {
        if (unicodeSet.isEmpty()) {
            return;
        }
        UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(unicodeSet);
        while (unicodeSetIterator.next() && unicodeSetIterator.codepoint != UnicodeSetIterator.IS_STRING) {
            int i = unicodeSetIterator.codepoint;
            if (this.trie.get(i) == 192) {
                this.trie.set(i, copyFromBaseCE32(i, this.base.getFinalCE32(this.base.getCE32(i)), true));
            }
        }
        this.modified = true;
    }

    void suppressContractions(UnicodeSet unicodeSet) {
        if (unicodeSet.isEmpty()) {
            return;
        }
        UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(unicodeSet);
        while (unicodeSetIterator.next() && unicodeSetIterator.codepoint != UnicodeSetIterator.IS_STRING) {
            int i = unicodeSetIterator.codepoint;
            int i2 = this.trie.get(i);
            if (i2 == 192) {
                int finalCE32 = this.base.getFinalCE32(this.base.getCE32(i));
                if (Collation.ce32HasContext(finalCE32)) {
                    this.trie.set(i, copyFromBaseCE32(i, finalCE32, false));
                }
            } else if (isBuilderContextCE32(i2)) {
                this.trie.set(i, getConditionalCE32ForCE32(i2).ce32);
                this.contextChars.remove(i);
            }
        }
        this.modified = true;
    }

    void enableFastLatin() {
        this.fastLatinEnabled = true;
    }

    void build(CollationData collationData) {
        buildMappings(collationData);
        if (this.base != null) {
            collationData.numericPrimary = this.base.numericPrimary;
            collationData.compressibleBytes = this.base.compressibleBytes;
            collationData.numScripts = this.base.numScripts;
            collationData.scriptsIndex = this.base.scriptsIndex;
            collationData.scriptStarts = this.base.scriptStarts;
        }
        buildFastLatinTable(collationData);
    }

    int getCEs(CharSequence charSequence, long[] jArr, int i) {
        return getCEs(charSequence, 0, jArr, i);
    }

    int getCEs(CharSequence charSequence, CharSequence charSequence2, long[] jArr, int i) {
        int length = charSequence.length();
        if (length == 0) {
            return getCEs(charSequence2, 0, jArr, i);
        }
        StringBuilder sb = new StringBuilder(charSequence);
        sb.append(charSequence2);
        return getCEs(sb, length, jArr, i);
    }

    private static final class ConditionalCE32 {
        int ce32;
        String context;
        int defaultCE32 = 1;
        int builtCE32 = 1;
        int next = -1;

        ConditionalCE32(String str, int i) {
            this.context = str;
            this.ce32 = i;
        }

        boolean hasContext() {
            return this.context.length() > 1;
        }

        int prefixLength() {
            return this.context.charAt(0);
        }
    }

    protected int getCE32FromOffsetCE32(boolean z, int i, int i2) {
        int iIndexFromCE32 = Collation.indexFromCE32(i2);
        return Collation.makeLongPrimaryCE32(Collation.getThreeBytePrimaryForOffsetData(i, z ? this.base.ces[iIndexFromCE32] : this.ce64s.elementAti(iIndexFromCE32)));
    }

    protected int addCE(long j) {
        int size = this.ce64s.size();
        for (int i = 0; i < size; i++) {
            if (j == this.ce64s.elementAti(i)) {
                return i;
            }
        }
        this.ce64s.addElement(j);
        return size;
    }

    protected int addCE32(int i) {
        int size = this.ce32s.size();
        for (int i2 = 0; i2 < size; i2++) {
            if (i == this.ce32s.elementAti(i2)) {
                return i2;
            }
        }
        this.ce32s.addElement(i);
        return size;
    }

    protected int addConditionalCE32(String str, int i) {
        int size = this.conditionalCE32s.size();
        if (size > 524287) {
            throw new IndexOutOfBoundsException("too many context-sensitive mappings");
        }
        this.conditionalCE32s.add(new ConditionalCE32(str, i));
        return size;
    }

    protected ConditionalCE32 getConditionalCE32(int i) {
        return this.conditionalCE32s.get(i);
    }

    protected ConditionalCE32 getConditionalCE32ForCE32(int i) {
        return getConditionalCE32(Collation.indexFromCE32(i));
    }

    protected static int makeBuilderContextCE32(int i) {
        return Collation.makeCE32FromTagAndIndex(7, i);
    }

    protected static boolean isBuilderContextCE32(int i) {
        return Collation.hasCE32Tag(i, 7);
    }

    protected static int encodeOneCEAsCE32(long j) {
        long j2 = j >>> 32;
        int i = (int) j;
        int i2 = 65535 & i;
        if ((281470698455295L & j) == 0) {
            return ((int) j2) | (i >>> 16) | (i2 >> 8);
        }
        if ((j & 1099511627775L) == 83887360) {
            return Collation.makeLongPrimaryCE32(j2);
        }
        if (j2 == 0 && (i2 & 255) == 0) {
            return Collation.makeLongSecondaryCE32(i);
        }
        return 1;
    }

    protected int encodeOneCE(long j) {
        int iEncodeOneCEAsCE32 = encodeOneCEAsCE32(j);
        if (iEncodeOneCEAsCE32 != 1) {
            return iEncodeOneCEAsCE32;
        }
        int iAddCE = addCE(j);
        if (iAddCE <= 524287) {
            return Collation.makeCE32FromTagIndexAndLength(6, iAddCE, 1);
        }
        throw new IndexOutOfBoundsException("too many mappings");
    }

    protected int encodeExpansion(long[] jArr, int i, int i2) {
        long j = jArr[i];
        int size = this.ce64s.size() - i2;
        for (int i3 = 0; i3 <= size; i3++) {
            if (j == this.ce64s.elementAti(i3)) {
                if (i3 > 524287) {
                    throw new IndexOutOfBoundsException("too many mappings");
                }
                for (int i4 = 1; i4 != i2; i4++) {
                    if (this.ce64s.elementAti(i3 + i4) != jArr[i + i4]) {
                        break;
                    }
                }
                return Collation.makeCE32FromTagIndexAndLength(6, i3, i2);
            }
        }
        int size2 = this.ce64s.size();
        if (size2 > 524287) {
            throw new IndexOutOfBoundsException("too many mappings");
        }
        for (int i5 = 0; i5 < i2; i5++) {
            this.ce64s.addElement(jArr[i + i5]);
        }
        return Collation.makeCE32FromTagIndexAndLength(6, size2, i2);
    }

    protected int encodeExpansion32(int[] iArr, int i, int i2) {
        int i3 = iArr[i];
        int size = this.ce32s.size() - i2;
        for (int i4 = 0; i4 <= size; i4++) {
            if (i3 == this.ce32s.elementAti(i4)) {
                if (i4 > 524287) {
                    throw new IndexOutOfBoundsException("too many mappings");
                }
                for (int i5 = 1; i5 != i2; i5++) {
                    if (this.ce32s.elementAti(i4 + i5) != iArr[i + i5]) {
                        break;
                    }
                }
                return Collation.makeCE32FromTagIndexAndLength(5, i4, i2);
            }
        }
        int size2 = this.ce32s.size();
        if (size2 > 524287) {
            throw new IndexOutOfBoundsException("too many mappings");
        }
        for (int i6 = 0; i6 < i2; i6++) {
            this.ce32s.addElement(iArr[i + i6]);
        }
        return Collation.makeCE32FromTagIndexAndLength(5, size2, i2);
    }

    protected int copyFromBaseCE32(int i, int i2, boolean z) {
        int iAddConditionalCE32;
        int iCopyContractionsFromBaseCE32;
        if (!Collation.isSpecialCE32(i2)) {
            return i2;
        }
        switch (Collation.tagFromCE32(i2)) {
            case 1:
            case 2:
            case 4:
                return i2;
            case 3:
            case 7:
            case 10:
            case 11:
            case 13:
            default:
                throw new AssertionError("copyFromBaseCE32(c, ce32, withContext) requires ce32 == base.getFinalCE32(ce32)");
            case 5:
                return encodeExpansion32(this.base.ce32s, Collation.indexFromCE32(i2), Collation.lengthFromCE32(i2));
            case 6:
                return encodeExpansion(this.base.ces, Collation.indexFromCE32(i2), Collation.lengthFromCE32(i2));
            case 8:
                int iIndexFromCE32 = Collation.indexFromCE32(i2);
                int cE32FromContexts = this.base.getCE32FromContexts(iIndexFromCE32);
                if (!z) {
                    return copyFromBaseCE32(i, cE32FromContexts, false);
                }
                ConditionalCE32 conditionalCE32 = new ConditionalCE32("", 0);
                StringBuilder sb = new StringBuilder("\u0000");
                if (Collation.isContractionCE32(cE32FromContexts)) {
                    iAddConditionalCE32 = copyContractionsFromBaseCE32(sb, i, cE32FromContexts, conditionalCE32);
                } else {
                    iAddConditionalCE32 = addConditionalCE32(sb.toString(), copyFromBaseCE32(i, cE32FromContexts, true));
                    conditionalCE32.next = iAddConditionalCE32;
                }
                ConditionalCE32 conditionalCE322 = getConditionalCE32(iAddConditionalCE32);
                CharsTrie.Iterator it = CharsTrie.iterator(this.base.contexts, iIndexFromCE32 + 2, 0);
                while (it.hasNext()) {
                    CharsTrie.Entry next = it.next();
                    sb.setLength(0);
                    sb.append(next.chars);
                    sb.reverse().insert(0, (char) next.chars.length());
                    int i3 = next.value;
                    if (Collation.isContractionCE32(i3)) {
                        iCopyContractionsFromBaseCE32 = copyContractionsFromBaseCE32(sb, i, i3, conditionalCE322);
                    } else {
                        int iAddConditionalCE322 = addConditionalCE32(sb.toString(), copyFromBaseCE32(i, i3, true));
                        conditionalCE322.next = iAddConditionalCE322;
                        iCopyContractionsFromBaseCE32 = iAddConditionalCE322;
                    }
                    conditionalCE322 = getConditionalCE32(iCopyContractionsFromBaseCE32);
                }
                int iMakeBuilderContextCE32 = makeBuilderContextCE32(conditionalCE32.next);
                this.contextChars.add(i);
                return iMakeBuilderContextCE32;
            case 9:
                if (!z) {
                    return copyFromBaseCE32(i, this.base.getCE32FromContexts(Collation.indexFromCE32(i2)), false);
                }
                ConditionalCE32 conditionalCE323 = new ConditionalCE32("", 0);
                copyContractionsFromBaseCE32(new StringBuilder("\u0000"), i, i2, conditionalCE323);
                int iMakeBuilderContextCE322 = makeBuilderContextCE32(conditionalCE323.next);
                this.contextChars.add(i);
                return iMakeBuilderContextCE322;
            case 12:
                throw new UnsupportedOperationException("We forbid tailoring of Hangul syllables.");
            case 14:
                return getCE32FromOffsetCE32(true, i, i2);
            case 15:
                return encodeOneCE(Collation.unassignedCEFromCodePoint(i));
        }
    }

    protected int copyContractionsFromBaseCE32(StringBuilder sb, int i, int i2, ConditionalCE32 conditionalCE32) {
        int iAddConditionalCE32;
        int iIndexFromCE32 = Collation.indexFromCE32(i2);
        if ((i2 & 256) != 0) {
            iAddConditionalCE32 = -1;
        } else {
            iAddConditionalCE32 = addConditionalCE32(sb.toString(), copyFromBaseCE32(i, this.base.getCE32FromContexts(iIndexFromCE32), true));
            conditionalCE32.next = iAddConditionalCE32;
            conditionalCE32 = getConditionalCE32(iAddConditionalCE32);
        }
        int length = sb.length();
        CharsTrie.Iterator it = CharsTrie.iterator(this.base.contexts, iIndexFromCE32 + 2, 0);
        while (it.hasNext()) {
            CharsTrie.Entry next = it.next();
            sb.append(next.chars);
            iAddConditionalCE32 = addConditionalCE32(sb.toString(), copyFromBaseCE32(i, next.value, true));
            conditionalCE32.next = iAddConditionalCE32;
            conditionalCE32 = getConditionalCE32(iAddConditionalCE32);
            sb.setLength(length);
        }
        return iAddConditionalCE32;
    }

    private static final class CopyHelper {
        static final boolean $assertionsDisabled = false;
        CollationDataBuilder dest;
        long[] modifiedCEs = new long[31];
        CEModifier modifier;
        CollationDataBuilder src;

        CopyHelper(CollationDataBuilder collationDataBuilder, CollationDataBuilder collationDataBuilder2, CEModifier cEModifier) {
            this.src = collationDataBuilder;
            this.dest = collationDataBuilder2;
            this.modifier = cEModifier;
        }

        void copyRangeCE32(int i, int i2, int i3) {
            int iCopyCE32 = copyCE32(i3);
            this.dest.trie.setRange(i, i2, iCopyCE32, true);
            if (CollationDataBuilder.isBuilderContextCE32(iCopyCE32)) {
                this.dest.contextChars.add(i, i2);
            }
        }

        int copyCE32(int i) {
            if (!Collation.isSpecialCE32(i)) {
                long jModifyCE32 = this.modifier.modifyCE32(i);
                if (jModifyCE32 != Collation.NO_CE) {
                    return this.dest.encodeOneCE(jModifyCE32);
                }
                return i;
            }
            int iTagFromCE32 = Collation.tagFromCE32(i);
            if (iTagFromCE32 == 5) {
                int[] buffer = this.src.ce32s.getBuffer();
                int iIndexFromCE32 = Collation.indexFromCE32(i);
                int iLengthFromCE32 = Collation.lengthFromCE32(i);
                boolean z = false;
                for (int i2 = 0; i2 < iLengthFromCE32; i2++) {
                    int i3 = buffer[iIndexFromCE32 + i2];
                    if (!Collation.isSpecialCE32(i3)) {
                        long jModifyCE322 = this.modifier.modifyCE32(i3);
                        if (jModifyCE322 == Collation.NO_CE) {
                            if (z) {
                                this.modifiedCEs[i2] = Collation.ceFromCE32(i3);
                            }
                        } else {
                            if (!z) {
                                for (int i4 = 0; i4 < i2; i4++) {
                                    this.modifiedCEs[i4] = Collation.ceFromCE32(buffer[iIndexFromCE32 + i4]);
                                }
                                z = true;
                            }
                            this.modifiedCEs[i2] = jModifyCE322;
                        }
                    }
                }
                if (z) {
                    return this.dest.encodeCEs(this.modifiedCEs, iLengthFromCE32);
                }
                return this.dest.encodeExpansion32(buffer, iIndexFromCE32, iLengthFromCE32);
            }
            if (iTagFromCE32 != 6) {
                if (iTagFromCE32 == 7) {
                    ConditionalCE32 conditionalCE32ForCE32 = this.src.getConditionalCE32ForCE32(i);
                    int iAddConditionalCE32 = this.dest.addConditionalCE32(conditionalCE32ForCE32.context, copyCE32(conditionalCE32ForCE32.ce32));
                    int iMakeBuilderContextCE32 = CollationDataBuilder.makeBuilderContextCE32(iAddConditionalCE32);
                    while (conditionalCE32ForCE32.next >= 0) {
                        conditionalCE32ForCE32 = this.src.getConditionalCE32(conditionalCE32ForCE32.next);
                        ConditionalCE32 conditionalCE32 = this.dest.getConditionalCE32(iAddConditionalCE32);
                        int iAddConditionalCE322 = this.dest.addConditionalCE32(conditionalCE32ForCE32.context, copyCE32(conditionalCE32ForCE32.ce32));
                        this.dest.unsafeBackwardSet.addAll(conditionalCE32ForCE32.context.substring(conditionalCE32ForCE32.prefixLength() + 1));
                        conditionalCE32.next = iAddConditionalCE322;
                        iAddConditionalCE32 = iAddConditionalCE322;
                    }
                    return iMakeBuilderContextCE32;
                }
                return i;
            }
            long[] buffer2 = this.src.ce64s.getBuffer();
            int iIndexFromCE322 = Collation.indexFromCE32(i);
            int iLengthFromCE322 = Collation.lengthFromCE32(i);
            boolean z2 = false;
            for (int i5 = 0; i5 < iLengthFromCE322; i5++) {
                long j = buffer2[iIndexFromCE322 + i5];
                long jModifyCE = this.modifier.modifyCE(j);
                if (jModifyCE == Collation.NO_CE) {
                    if (z2) {
                        this.modifiedCEs[i5] = j;
                    }
                } else {
                    if (!z2) {
                        for (int i6 = 0; i6 < i5; i6++) {
                            this.modifiedCEs[i6] = buffer2[iIndexFromCE322 + i6];
                        }
                        z2 = true;
                    }
                    this.modifiedCEs[i5] = jModifyCE;
                }
            }
            if (z2) {
                return this.dest.encodeCEs(this.modifiedCEs, iLengthFromCE322);
            }
            return this.dest.encodeExpansion(buffer2, iIndexFromCE322, iLengthFromCE322);
        }
    }

    private static void enumRangeForCopy(int i, int i2, int i3, CopyHelper copyHelper) {
        if (i3 != -1 && i3 != 192) {
            copyHelper.copyRangeCE32(i, i2, i3);
        }
    }

    protected boolean getJamoCE32s(int[] iArr) {
        boolean z;
        boolean zIsAssignedCE32 = this.base == null;
        int i = 0;
        boolean z2 = false;
        while (true) {
            int cE32FromOffsetCE32 = 192;
            if (i < 67) {
                int iJamoCpFromIndex = jamoCpFromIndex(i);
                int ce32 = this.trie.get(iJamoCpFromIndex);
                zIsAssignedCE32 |= Collation.isAssignedCE32(ce32);
                if (ce32 == 192) {
                    ce32 = this.base.getCE32(iJamoCpFromIndex);
                    z = true;
                } else {
                    z = false;
                }
                if (Collation.isSpecialCE32(ce32)) {
                    switch (Collation.tagFromCE32(ce32)) {
                        case 0:
                        case 3:
                        case 7:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                            throw new AssertionError(String.format("unexpected special tag in ce32=0x%08x", Integer.valueOf(ce32)));
                        case 1:
                        case 2:
                        case 4:
                        default:
                            cE32FromOffsetCE32 = ce32;
                            break;
                        case 5:
                        case 6:
                        case 8:
                        case 9:
                            if (!z) {
                                cE32FromOffsetCE32 = ce32;
                            }
                            z2 = true;
                            iArr[i] = cE32FromOffsetCE32;
                            i++;
                            break;
                        case 14:
                            cE32FromOffsetCE32 = getCE32FromOffsetCE32(z, iJamoCpFromIndex, ce32);
                            iArr[i] = cE32FromOffsetCE32;
                            i++;
                            break;
                        case 15:
                            z2 = true;
                            iArr[i] = cE32FromOffsetCE32;
                            i++;
                            break;
                    }
                } else {
                    cE32FromOffsetCE32 = ce32;
                }
                iArr[i] = cE32FromOffsetCE32;
                i++;
            } else {
                if (zIsAssignedCE32 && z2) {
                    for (int i2 = 0; i2 < 67; i2++) {
                        if (iArr[i2] == 192) {
                            int iJamoCpFromIndex2 = jamoCpFromIndex(i2);
                            iArr[i2] = copyFromBaseCE32(iJamoCpFromIndex2, this.base.getCE32(iJamoCpFromIndex2), true);
                        }
                    }
                }
                return zIsAssignedCE32;
            }
        }
    }

    protected void setDigitTags() {
        UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(new UnicodeSet("[:Nd:]"));
        while (unicodeSetIterator.next()) {
            int i = unicodeSetIterator.codepoint;
            int i2 = this.trie.get(i);
            if (i2 != 192 && i2 != -1) {
                int iAddCE32 = addCE32(i2);
                if (iAddCE32 > 524287) {
                    throw new IndexOutOfBoundsException("too many mappings");
                }
                this.trie.set(i, Collation.makeCE32FromTagIndexAndLength(10, iAddCE32, UCharacter.digit(i)));
            }
        }
    }

    protected void setLeadSurrogates() {
        int i;
        int i2;
        for (char c = 55296; c < 56320; c = (char) (c + 1)) {
            Iterator<Trie2.Range> itIteratorForLeadSurrogate = this.trie.iteratorForLeadSurrogate(c);
            int i3 = -1;
            while (true) {
                i = 512;
                if (itIteratorForLeadSurrogate.hasNext()) {
                    int i4 = itIteratorForLeadSurrogate.next().value;
                    if (i4 != -1) {
                        if (i4 == 192) {
                            i2 = 256;
                        }
                    } else {
                        i2 = 0;
                    }
                    if (i3 >= 0) {
                        if (i3 != i2) {
                            break;
                        }
                    } else {
                        i3 = i2;
                    }
                } else {
                    i = i3;
                    break;
                }
            }
            this.trie.setForLeadSurrogateCodeUnit(c, Collation.makeCE32FromTagAndIndex(13, 0) | i);
        }
    }

    protected void buildMappings(CollationData collationData) {
        boolean z;
        int i;
        if (!isMutable()) {
            throw new IllegalStateException("attempt to build() after build()");
        }
        buildContexts();
        int[] iArr = new int[67];
        int size = -1;
        boolean jamoCE32s = getJamoCE32s(iArr);
        int i2 = Normalizer2Impl.Hangul.HANGUL_BASE;
        if (jamoCE32s) {
            size = this.ce32s.size();
            for (int i3 = 0; i3 < 67; i3++) {
                this.ce32s.addElement(iArr[i3]);
            }
            int i4 = 19;
            while (true) {
                if (i4 < 67) {
                    if (!Collation.isSpecialCE32(iArr[i4])) {
                        i4++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            int iMakeCE32FromTagAndIndex = Collation.makeCE32FromTagAndIndex(12, 0);
            int i5 = 44032;
            int i6 = 0;
            while (i6 < 19) {
                if (!z && !Collation.isSpecialCE32(iArr[i6])) {
                    i = iMakeCE32FromTagAndIndex | 256;
                } else {
                    i = iMakeCE32FromTagAndIndex;
                }
                int i7 = i5 + Normalizer2Impl.Hangul.JAMO_VT_COUNT;
                this.trie.setRange(i5, i7 - 1, i, true);
                i6++;
                i5 = i7;
            }
        } else {
            while (i2 < 55204) {
                int ce32 = this.base.getCE32(i2);
                int i8 = i2 + Normalizer2Impl.Hangul.JAMO_VT_COUNT;
                this.trie.setRange(i2, i8 - 1, ce32, true);
                i2 = i8;
            }
        }
        setDigitTags();
        setLeadSurrogates();
        this.ce32s.setElementAt(this.trie.get(0), 0);
        this.trie.set(0, Collation.makeCE32FromTagAndIndex(11, 0));
        collationData.trie = this.trie.toTrie2_32();
        int i9 = 65536;
        char c = 55296;
        while (c < 56320) {
            if (this.unsafeBackwardSet.containsSome(i9, i9 + Opcodes.OP_NEW_INSTANCE_JUMBO)) {
                this.unsafeBackwardSet.add(c);
            }
            c = (char) (c + 1);
            i9 += 1024;
        }
        this.unsafeBackwardSet.freeze();
        collationData.ce32s = this.ce32s.getBuffer();
        collationData.ces = this.ce64s.getBuffer();
        collationData.contexts = this.contexts.toString();
        collationData.base = this.base;
        if (size >= 0) {
            collationData.jamoCE32s = iArr;
        } else {
            collationData.jamoCE32s = this.base.jamoCE32s;
        }
        collationData.unsafeBackwardSet = this.unsafeBackwardSet;
    }

    protected void clearContexts() {
        this.contexts.setLength(0);
        UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(this.contextChars);
        while (unicodeSetIterator.next()) {
            getConditionalCE32ForCE32(this.trie.get(unicodeSetIterator.codepoint)).builtCE32 = 1;
        }
    }

    protected void buildContexts() {
        this.contexts.setLength(0);
        UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(this.contextChars);
        while (unicodeSetIterator.next()) {
            int i = unicodeSetIterator.codepoint;
            int i2 = this.trie.get(i);
            if (!isBuilderContextCE32(i2)) {
                throw new AssertionError("Impossible: No context data for c in contextChars.");
            }
            this.trie.set(i, buildContext(getConditionalCE32ForCE32(i2)));
        }
    }

    protected int buildContext(ConditionalCE32 conditionalCE32) {
        ConditionalCE32 conditionalCE322;
        int i;
        int i2;
        int iMakeCE32FromTagAndIndex;
        CharsTrieBuilder charsTrieBuilder = new CharsTrieBuilder();
        CharsTrieBuilder charsTrieBuilder2 = new CharsTrieBuilder();
        ConditionalCE32 conditionalCE323 = conditionalCE32;
        while (true) {
            int iPrefixLength = conditionalCE323.prefixLength();
            StringBuilder sb = new StringBuilder();
            int i3 = iPrefixLength + 1;
            int i4 = 0;
            sb.append((CharSequence) conditionalCE323.context, 0, i3);
            String string = sb.toString();
            ConditionalCE32 conditionalCE324 = conditionalCE323;
            while (conditionalCE324.next >= 0) {
                ConditionalCE32 conditionalCE325 = getConditionalCE32(conditionalCE324.next);
                if (!conditionalCE325.context.startsWith(string)) {
                    break;
                }
                conditionalCE324 = conditionalCE325;
            }
            int i5 = 524287;
            if (conditionalCE324.context.length() == i3) {
                iMakeCE32FromTagAndIndex = conditionalCE324.ce32;
                conditionalCE322 = conditionalCE324;
            } else {
                charsTrieBuilder2.clear();
                if (conditionalCE323.context.length() == i3) {
                    i = conditionalCE323.ce32;
                    conditionalCE322 = getConditionalCE32(conditionalCE323.next);
                    i2 = 0;
                } else {
                    ConditionalCE32 conditionalCE326 = conditionalCE32;
                    int i6 = 1;
                    while (true) {
                        int iPrefixLength2 = conditionalCE326.prefixLength();
                        if (iPrefixLength2 == iPrefixLength) {
                            break;
                        }
                        if (conditionalCE326.defaultCE32 != 1 && (iPrefixLength2 == 0 || string.regionMatches(sb.length() - iPrefixLength2, conditionalCE326.context, 1, iPrefixLength2))) {
                            i6 = conditionalCE326.defaultCE32;
                        }
                        conditionalCE326 = getConditionalCE32(conditionalCE326.next);
                        i4 = 0;
                        i5 = 524287;
                    }
                    conditionalCE322 = conditionalCE323;
                    i = i6;
                    i2 = 256;
                }
                int i7 = i2 | 512;
                while (true) {
                    String strSubstring = conditionalCE322.context.substring(i3);
                    if (this.nfcImpl.getFCD16(strSubstring.codePointAt(i4)) <= 255) {
                        i7 &= -513;
                    }
                    if (this.nfcImpl.getFCD16(strSubstring.codePointBefore(strSubstring.length())) > 255) {
                        i7 |= 1024;
                    }
                    charsTrieBuilder2.add(strSubstring, conditionalCE322.ce32);
                    if (conditionalCE322 == conditionalCE324) {
                        break;
                    }
                    i4 = 0;
                    conditionalCE322 = getConditionalCE32(conditionalCE322.next);
                }
                int iAddContextTrie = addContextTrie(i, charsTrieBuilder2);
                if (iAddContextTrie > i5) {
                    throw new IndexOutOfBoundsException("too many context-sensitive mappings");
                }
                iMakeCE32FromTagAndIndex = Collation.makeCE32FromTagAndIndex(9, iAddContextTrie) | i7;
            }
            conditionalCE323.defaultCE32 = iMakeCE32FromTagAndIndex;
            if (iPrefixLength == 0) {
                if (conditionalCE322.next < 0) {
                    return iMakeCE32FromTagAndIndex;
                }
            } else {
                sb.delete(0, 1);
                sb.reverse();
                charsTrieBuilder.add(sb, iMakeCE32FromTagAndIndex);
                if (conditionalCE322.next < 0) {
                    int iAddContextTrie2 = addContextTrie(conditionalCE32.defaultCE32, charsTrieBuilder);
                    if (iAddContextTrie2 > i5) {
                        throw new IndexOutOfBoundsException("too many context-sensitive mappings");
                    }
                    return Collation.makeCE32FromTagAndIndex(8, iAddContextTrie2);
                }
            }
            conditionalCE323 = getConditionalCE32(conditionalCE322.next);
        }
    }

    protected int addContextTrie(int i, CharsTrieBuilder charsTrieBuilder) {
        StringBuilder sb = new StringBuilder();
        sb.append((char) (i >> 16));
        sb.append((char) i);
        sb.append(charsTrieBuilder.buildCharSequence(StringTrieBuilder.Option.SMALL));
        int iIndexOf = this.contexts.indexOf(sb.toString());
        if (iIndexOf < 0) {
            int length = this.contexts.length();
            this.contexts.append((CharSequence) sb);
            return length;
        }
        return iIndexOf;
    }

    protected void buildFastLatinTable(CollationData collationData) {
        if (this.fastLatinEnabled) {
            this.fastLatinBuilder = new CollationFastLatinBuilder();
            if (this.fastLatinBuilder.forData(collationData)) {
                char[] header = this.fastLatinBuilder.getHeader();
                char[] table = this.fastLatinBuilder.getTable();
                if (this.base != null && Arrays.equals(header, this.base.fastLatinTableHeader) && Arrays.equals(table, this.base.fastLatinTable)) {
                    this.fastLatinBuilder = null;
                    header = this.base.fastLatinTableHeader;
                    table = this.base.fastLatinTable;
                }
                collationData.fastLatinTableHeader = header;
                collationData.fastLatinTable = table;
                return;
            }
            this.fastLatinBuilder = null;
        }
    }

    protected int getCEs(CharSequence charSequence, int i, long[] jArr, int i2) {
        if (this.collIter == null) {
            this.collIter = new DataBuilderCollationIterator(this, new CollationData(this.nfcImpl));
            if (this.collIter == null) {
                return 0;
            }
        }
        return this.collIter.fetchCEs(charSequence, i, jArr, i2);
    }

    protected static int jamoCpFromIndex(int i) {
        if (i < 19) {
            return Normalizer2Impl.Hangul.JAMO_L_BASE + i;
        }
        int i2 = i - 19;
        return i2 < 21 ? Normalizer2Impl.Hangul.JAMO_V_BASE + i2 : 4520 + (i2 - 21);
    }

    private static final class DataBuilderCollationIterator extends CollationIterator {
        static final boolean $assertionsDisabled = false;
        protected final CollationDataBuilder builder;
        protected final CollationData builderData;
        protected final int[] jamoCE32s;
        protected int pos;
        protected CharSequence s;

        DataBuilderCollationIterator(CollationDataBuilder collationDataBuilder, CollationData collationData) {
            super(collationData, false);
            this.jamoCE32s = new int[67];
            this.builder = collationDataBuilder;
            this.builderData = collationData;
            this.builderData.base = this.builder.base;
            for (int i = 0; i < 67; i++) {
                this.jamoCE32s[i] = Collation.makeCE32FromTagAndIndex(7, CollationDataBuilder.jamoCpFromIndex(i)) | 256;
            }
            this.builderData.jamoCE32s = this.jamoCE32s;
        }

        int fetchCEs(CharSequence charSequence, int i, long[] jArr, int i2) {
            int ce32;
            CollationData collationData;
            this.builderData.ce32s = this.builder.ce32s.getBuffer();
            this.builderData.ces = this.builder.ce64s.getBuffer();
            this.builderData.contexts = this.builder.contexts.toString();
            reset();
            this.s = charSequence;
            this.pos = i;
            while (this.pos < this.s.length()) {
                clearCEs();
                int iCodePointAt = Character.codePointAt(this.s, this.pos);
                this.pos += Character.charCount(iCodePointAt);
                int i3 = this.builder.trie.get(iCodePointAt);
                if (i3 == 192) {
                    collationData = this.builder.base;
                    ce32 = this.builder.base.getCE32(iCodePointAt);
                } else {
                    ce32 = i3;
                    collationData = this.builderData;
                }
                appendCEsFromCE32(collationData, iCodePointAt, ce32, true);
                for (int i4 = 0; i4 < getCEsLength(); i4++) {
                    long ce = getCE(i4);
                    if (ce != 0) {
                        if (i2 < 31) {
                            jArr[i2] = ce;
                        }
                        i2++;
                    }
                }
            }
            return i2;
        }

        @Override
        public void resetToOffset(int i) {
            reset();
            this.pos = i;
        }

        @Override
        public int getOffset() {
            return this.pos;
        }

        @Override
        public int nextCodePoint() {
            if (this.pos == this.s.length()) {
                return -1;
            }
            int iCodePointAt = Character.codePointAt(this.s, this.pos);
            this.pos += Character.charCount(iCodePointAt);
            return iCodePointAt;
        }

        @Override
        public int previousCodePoint() {
            if (this.pos == 0) {
                return -1;
            }
            int iCodePointBefore = Character.codePointBefore(this.s, this.pos);
            this.pos -= Character.charCount(iCodePointBefore);
            return iCodePointBefore;
        }

        @Override
        protected void forwardNumCodePoints(int i) {
            this.pos = Character.offsetByCodePoints(this.s, this.pos, i);
        }

        @Override
        protected void backwardNumCodePoints(int i) {
            this.pos = Character.offsetByCodePoints(this.s, this.pos, -i);
        }

        @Override
        protected int getDataCE32(int i) {
            return this.builder.trie.get(i);
        }

        @Override
        protected int getCE32FromBuilderData(int i) {
            if ((i & 256) != 0) {
                return this.builder.trie.get(Collation.indexFromCE32(i));
            }
            ConditionalCE32 conditionalCE32ForCE32 = this.builder.getConditionalCE32ForCE32(i);
            if (conditionalCE32ForCE32.builtCE32 == 1) {
                try {
                    conditionalCE32ForCE32.builtCE32 = this.builder.buildContext(conditionalCE32ForCE32);
                } catch (IndexOutOfBoundsException e) {
                    this.builder.clearContexts();
                    conditionalCE32ForCE32.builtCE32 = this.builder.buildContext(conditionalCE32ForCE32);
                }
                this.builderData.contexts = this.builder.contexts.toString();
            }
            return conditionalCE32ForCE32.builtCE32;
        }
    }

    protected final boolean isMutable() {
        return (this.trie == null || this.unsafeBackwardSet == null || this.unsafeBackwardSet.isFrozen()) ? false : true;
    }
}
