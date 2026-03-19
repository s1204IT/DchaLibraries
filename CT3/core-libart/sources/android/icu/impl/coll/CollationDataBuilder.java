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
import com.android.dex.DexFormat;
import dalvik.bytecode.Opcodes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

final class CollationDataBuilder {

    static final boolean f37assertionsDisabled;
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

    static {
        f37assertionsDisabled = !CollationDataBuilder.class.desiredAssertionStatus();
    }

    CollationDataBuilder() {
        this.ce32s.addElement(0);
    }

    void initForTailoring(CollationData b) {
        if (this.trie != null) {
            throw new IllegalStateException("attempt to reuse a CollationDataBuilder");
        }
        if (b == null) {
            throw new IllegalArgumentException("null CollationData");
        }
        this.base = b;
        this.trie = new Trie2Writable(192, -195323);
        for (int c = 192; c <= 255; c++) {
            this.trie.set(c, 192);
        }
        int hangulCE32 = Collation.makeCE32FromTagAndIndex(12, 0);
        this.trie.setRange(Normalizer2Impl.Hangul.HANGUL_BASE, Normalizer2Impl.Hangul.HANGUL_END, hangulCE32, true);
        this.unsafeBackwardSet.addAll(b.unsafeBackwardSet);
    }

    boolean isCompressibleLeadByte(int b) {
        return this.base.isCompressibleLeadByte(b);
    }

    boolean isCompressiblePrimary(long p) {
        return isCompressibleLeadByte(((int) p) >>> 24);
    }

    boolean hasMappings() {
        return this.modified;
    }

    boolean isAssigned(int c) {
        return Collation.isAssignedCE32(this.trie.get(c));
    }

    void add(CharSequence prefix, CharSequence s, long[] ces, int cesLength) {
        int ce32 = encodeCEs(ces, cesLength);
        addCE32(prefix, s, ce32);
    }

    int encodeCEs(long[] ces, int cesLength) {
        if (cesLength < 0 || cesLength > 31) {
            throw new IllegalArgumentException("mapping to too many CEs");
        }
        if (!isMutable()) {
            throw new IllegalStateException("attempt to add mappings after build()");
        }
        if (cesLength == 0) {
            return encodeOneCEAsCE32(0L);
        }
        if (cesLength == 1) {
            return encodeOneCE(ces[0]);
        }
        if (cesLength == 2) {
            long ce0 = ces[0];
            long ce1 = ces[1];
            long p0 = ce0 >>> 32;
            if ((72057594037862655L & ce0) == 83886080 && ((-4278190081L) & ce1) == 1280 && p0 != 0) {
                return ((int) p0) | ((((int) ce0) & Normalizer2Impl.JAMO_VT) << 8) | ((((int) ce1) >> 16) & Normalizer2Impl.JAMO_VT) | 192 | 4;
            }
        }
        int[] newCE32s = new int[31];
        for (int i = 0; i != cesLength; i++) {
            int ce32 = encodeOneCEAsCE32(ces[i]);
            if (ce32 != 1) {
                newCE32s[i] = ce32;
            } else {
                return encodeExpansion(ces, 0, cesLength);
            }
        }
        return encodeExpansion32(newCE32s, 0, cesLength);
    }

    void addCE32(CharSequence prefix, CharSequence s, int ce32) {
        ConditionalCE32 cond;
        if (s.length() == 0) {
            throw new IllegalArgumentException("mapping from empty string");
        }
        if (!isMutable()) {
            throw new IllegalStateException("attempt to add mappings after build()");
        }
        int c = Character.codePointAt(s, 0);
        int cLength = Character.charCount(c);
        int oldCE32 = this.trie.get(c);
        boolean hasContext = prefix.length() != 0 || s.length() > cLength;
        if (oldCE32 == 192) {
            int baseCE32 = this.base.getFinalCE32(this.base.getCE32(c));
            if (hasContext || Collation.ce32HasContext(baseCE32)) {
                oldCE32 = copyFromBaseCE32(c, baseCE32, true);
                this.trie.set(c, oldCE32);
            }
        }
        if (!hasContext) {
            if (!isBuilderContextCE32(oldCE32)) {
                this.trie.set(c, ce32);
            } else {
                ConditionalCE32 cond2 = getConditionalCE32ForCE32(oldCE32);
                cond2.builtCE32 = 1;
                cond2.ce32 = ce32;
            }
        } else {
            if (!isBuilderContextCE32(oldCE32)) {
                int index = addConditionalCE32(DexFormat.MAGIC_SUFFIX, oldCE32);
                int contextCE32 = makeBuilderContextCE32(index);
                this.trie.set(c, contextCE32);
                this.contextChars.add(c);
                cond = getConditionalCE32(index);
            } else {
                cond = getConditionalCE32ForCE32(oldCE32);
                cond.builtCE32 = 1;
            }
            CharSequence suffix = s.subSequence(cLength, s.length());
            String context = new StringBuilder().append((char) prefix.length()).append(prefix).append(suffix).toString();
            this.unsafeBackwardSet.addAll(suffix);
            while (true) {
                int next = cond.next;
                if (next < 0) {
                    cond.next = addConditionalCE32(context, ce32);
                    break;
                }
                ConditionalCE32 nextCond = getConditionalCE32(next);
                int cmp = context.compareTo(nextCond.context);
                if (cmp < 0) {
                    int index2 = addConditionalCE32(context, ce32);
                    cond.next = index2;
                    getConditionalCE32(index2).next = next;
                    break;
                } else {
                    if (cmp == 0) {
                        nextCond.ce32 = ce32;
                        break;
                    }
                    cond = nextCond;
                }
            }
        }
        this.modified = true;
    }

    void copyFrom(CollationDataBuilder src, CEModifier modifier) {
        if (!isMutable()) {
            throw new IllegalStateException("attempt to copyFrom() after build()");
        }
        CopyHelper helper = new CopyHelper(src, this, modifier);
        for (Trie2.Range range : src.trie) {
            if (range.leadSurrogate) {
                break;
            } else {
                enumRangeForCopy(range.startCodePoint, range.endCodePoint, range.value, helper);
            }
        }
        this.modified |= src.modified;
    }

    void optimize(UnicodeSet set) {
        if (set.isEmpty()) {
            return;
        }
        UnicodeSetIterator iter = new UnicodeSetIterator(set);
        while (iter.next() && iter.codepoint != UnicodeSetIterator.IS_STRING) {
            int c = iter.codepoint;
            int ce32 = this.trie.get(c);
            if (ce32 == 192) {
                int ce322 = this.base.getFinalCE32(this.base.getCE32(c));
                this.trie.set(c, copyFromBaseCE32(c, ce322, true));
            }
        }
        this.modified = true;
    }

    void suppressContractions(UnicodeSet set) {
        if (set.isEmpty()) {
            return;
        }
        UnicodeSetIterator iter = new UnicodeSetIterator(set);
        while (iter.next() && iter.codepoint != UnicodeSetIterator.IS_STRING) {
            int c = iter.codepoint;
            int ce32 = this.trie.get(c);
            if (ce32 == 192) {
                int ce322 = this.base.getFinalCE32(this.base.getCE32(c));
                if (Collation.ce32HasContext(ce322)) {
                    this.trie.set(c, copyFromBaseCE32(c, ce322, false));
                }
            } else if (isBuilderContextCE32(ce32)) {
                this.trie.set(c, getConditionalCE32ForCE32(ce32).ce32);
                this.contextChars.remove(c);
            }
        }
        this.modified = true;
    }

    void enableFastLatin() {
        this.fastLatinEnabled = true;
    }

    void build(CollationData data) {
        buildMappings(data);
        if (this.base != null) {
            data.numericPrimary = this.base.numericPrimary;
            data.compressibleBytes = this.base.compressibleBytes;
            data.numScripts = this.base.numScripts;
            data.scriptsIndex = this.base.scriptsIndex;
            data.scriptStarts = this.base.scriptStarts;
        }
        buildFastLatinTable(data);
    }

    int getCEs(CharSequence s, long[] ces, int cesLength) {
        return getCEs(s, 0, ces, cesLength);
    }

    int getCEs(CharSequence prefix, CharSequence s, long[] ces, int cesLength) {
        int prefixLength = prefix.length();
        if (prefixLength == 0) {
            return getCEs(s, 0, ces, cesLength);
        }
        return getCEs(new StringBuilder(prefix).append(s), prefixLength, ces, cesLength);
    }

    private static final class ConditionalCE32 {
        int ce32;
        String context;
        int defaultCE32 = 1;
        int builtCE32 = 1;
        int next = -1;

        ConditionalCE32(String ct, int ce) {
            this.context = ct;
            this.ce32 = ce;
        }

        boolean hasContext() {
            return this.context.length() > 1;
        }

        int prefixLength() {
            return this.context.charAt(0);
        }
    }

    protected int getCE32FromOffsetCE32(boolean fromBase, int c, int ce32) {
        int i = Collation.indexFromCE32(ce32);
        long dataCE = fromBase ? this.base.ces[i] : this.ce64s.elementAti(i);
        long p = Collation.getThreeBytePrimaryForOffsetData(c, dataCE);
        return Collation.makeLongPrimaryCE32(p);
    }

    protected int addCE(long ce) {
        int length = this.ce64s.size();
        for (int i = 0; i < length; i++) {
            if (ce == this.ce64s.elementAti(i)) {
                return i;
            }
        }
        this.ce64s.addElement(ce);
        return length;
    }

    protected int addCE32(int ce32) {
        int length = this.ce32s.size();
        for (int i = 0; i < length; i++) {
            if (ce32 == this.ce32s.elementAti(i)) {
                return i;
            }
        }
        this.ce32s.addElement(ce32);
        return length;
    }

    protected int addConditionalCE32(String context, int ce32) {
        if (!f37assertionsDisabled) {
            if (!(context.length() != 0)) {
                throw new AssertionError();
            }
        }
        int index = this.conditionalCE32s.size();
        if (index > 524287) {
            throw new IndexOutOfBoundsException("too many context-sensitive mappings");
        }
        ConditionalCE32 cond = new ConditionalCE32(context, ce32);
        this.conditionalCE32s.add(cond);
        return index;
    }

    protected ConditionalCE32 getConditionalCE32(int index) {
        return this.conditionalCE32s.get(index);
    }

    protected ConditionalCE32 getConditionalCE32ForCE32(int ce32) {
        return getConditionalCE32(Collation.indexFromCE32(ce32));
    }

    protected static int makeBuilderContextCE32(int index) {
        return Collation.makeCE32FromTagAndIndex(7, index);
    }

    protected static boolean isBuilderContextCE32(int ce32) {
        return Collation.hasCE32Tag(ce32, 7);
    }

    protected static int encodeOneCEAsCE32(long ce) {
        long p = ce >>> 32;
        int lower32 = (int) ce;
        int t = lower32 & 65535;
        if (!f37assertionsDisabled) {
            if (!((t & Collation.CASE_MASK) != 49152)) {
                throw new AssertionError();
            }
        }
        if ((281470698455295L & ce) == 0) {
            return ((int) p) | (lower32 >>> 16) | (t >> 8);
        }
        if ((1099511627775L & ce) == 83887360) {
            return Collation.makeLongPrimaryCE32(p);
        }
        if (p == 0 && (t & 255) == 0) {
            return Collation.makeLongSecondaryCE32(lower32);
        }
        return 1;
    }

    protected int encodeOneCE(long ce) {
        int ce32 = encodeOneCEAsCE32(ce);
        if (ce32 != 1) {
            return ce32;
        }
        int index = addCE(ce);
        if (index > 524287) {
            throw new IndexOutOfBoundsException("too many mappings");
        }
        return Collation.makeCE32FromTagIndexAndLength(6, index, 1);
    }

    protected int encodeExpansion(long[] ces, int start, int length) {
        long first = ces[start];
        int ce64sMax = this.ce64s.size() - length;
        for (int i = 0; i <= ce64sMax; i++) {
            if (first == this.ce64s.elementAti(i)) {
                if (i > 524287) {
                    throw new IndexOutOfBoundsException("too many mappings");
                }
                for (int j = 1; j != length; j++) {
                    if (this.ce64s.elementAti(i + j) != ces[start + j]) {
                        break;
                    }
                }
                return Collation.makeCE32FromTagIndexAndLength(6, i, length);
            }
        }
        int i2 = this.ce64s.size();
        if (i2 > 524287) {
            throw new IndexOutOfBoundsException("too many mappings");
        }
        for (int j2 = 0; j2 < length; j2++) {
            this.ce64s.addElement(ces[start + j2]);
        }
        return Collation.makeCE32FromTagIndexAndLength(6, i2, length);
    }

    protected int encodeExpansion32(int[] newCE32s, int start, int length) {
        int first = newCE32s[start];
        int ce32sMax = this.ce32s.size() - length;
        for (int i = 0; i <= ce32sMax; i++) {
            if (first == this.ce32s.elementAti(i)) {
                if (i > 524287) {
                    throw new IndexOutOfBoundsException("too many mappings");
                }
                for (int j = 1; j != length; j++) {
                    if (this.ce32s.elementAti(i + j) != newCE32s[start + j]) {
                        break;
                    }
                }
                return Collation.makeCE32FromTagIndexAndLength(5, i, length);
            }
        }
        int i2 = this.ce32s.size();
        if (i2 > 524287) {
            throw new IndexOutOfBoundsException("too many mappings");
        }
        for (int j2 = 0; j2 < length; j2++) {
            this.ce32s.addElement(newCE32s[start + j2]);
        }
        return Collation.makeCE32FromTagIndexAndLength(5, i2, length);
    }

    protected int copyFromBaseCE32(int c, int ce32, boolean withContext) {
        int index;
        int index2;
        if (!Collation.isSpecialCE32(ce32)) {
            return ce32;
        }
        switch (Collation.tagFromCE32(ce32)) {
            case 1:
            case 2:
            case 4:
                return ce32;
            case 3:
            case 7:
            case 10:
            case 11:
            case 13:
            default:
                throw new AssertionError("copyFromBaseCE32(c, ce32, withContext) requires ce32 == base.getFinalCE32(ce32)");
            case 5:
                int index3 = Collation.indexFromCE32(ce32);
                int length = Collation.lengthFromCE32(ce32);
                return encodeExpansion32(this.base.ce32s, index3, length);
            case 6:
                int index4 = Collation.indexFromCE32(ce32);
                int length2 = Collation.lengthFromCE32(ce32);
                return encodeExpansion(this.base.ces, index4, length2);
            case 8:
                int trieIndex = Collation.indexFromCE32(ce32);
                int ce322 = this.base.getCE32FromContexts(trieIndex);
                if (!withContext) {
                    return copyFromBaseCE32(c, ce322, false);
                }
                ConditionalCE32 head = new ConditionalCE32("", 0);
                StringBuilder context = new StringBuilder(DexFormat.MAGIC_SUFFIX);
                if (Collation.isContractionCE32(ce322)) {
                    index = copyContractionsFromBaseCE32(context, c, ce322, head);
                } else {
                    index = addConditionalCE32(context.toString(), copyFromBaseCE32(c, ce322, true));
                    head.next = index;
                }
                ConditionalCE32 cond = getConditionalCE32(index);
                CharsTrie.Iterator prefixes = CharsTrie.iterator(this.base.contexts, trieIndex + 2, 0);
                while (prefixes.hasNext()) {
                    CharsTrie.Entry entry = prefixes.next();
                    context.setLength(0);
                    context.append(entry.chars).reverse().insert(0, (char) entry.chars.length());
                    int ce323 = entry.value;
                    if (Collation.isContractionCE32(ce323)) {
                        index2 = copyContractionsFromBaseCE32(context, c, ce323, cond);
                    } else {
                        index2 = addConditionalCE32(context.toString(), copyFromBaseCE32(c, ce323, true));
                        cond.next = index2;
                    }
                    cond = getConditionalCE32(index2);
                }
                int ce324 = makeBuilderContextCE32(head.next);
                this.contextChars.add(c);
                return ce324;
            case 9:
                if (!withContext) {
                    return copyFromBaseCE32(c, this.base.getCE32FromContexts(Collation.indexFromCE32(ce32)), false);
                }
                ConditionalCE32 head2 = new ConditionalCE32("", 0);
                copyContractionsFromBaseCE32(new StringBuilder(DexFormat.MAGIC_SUFFIX), c, ce32, head2);
                int ce325 = makeBuilderContextCE32(head2.next);
                this.contextChars.add(c);
                return ce325;
            case 12:
                throw new UnsupportedOperationException("We forbid tailoring of Hangul syllables.");
            case 14:
                return getCE32FromOffsetCE32(true, c, ce32);
            case 15:
                return encodeOneCE(Collation.unassignedCEFromCodePoint(c));
        }
    }

    protected int copyContractionsFromBaseCE32(StringBuilder context, int c, int ce32, ConditionalCE32 cond) {
        int index;
        int trieIndex = Collation.indexFromCE32(ce32);
        if ((ce32 & 256) != 0) {
            if (!f37assertionsDisabled) {
                if (!(context.length() > 1)) {
                    throw new AssertionError();
                }
            }
            index = -1;
        } else {
            int ce322 = this.base.getCE32FromContexts(trieIndex);
            if (!f37assertionsDisabled) {
                if (!(!Collation.isContractionCE32(ce322))) {
                    throw new AssertionError();
                }
            }
            index = addConditionalCE32(context.toString(), copyFromBaseCE32(c, ce322, true));
            cond.next = index;
            cond = getConditionalCE32(index);
        }
        int suffixStart = context.length();
        CharsTrie.Iterator suffixes = CharsTrie.iterator(this.base.contexts, trieIndex + 2, 0);
        while (suffixes.hasNext()) {
            CharsTrie.Entry entry = suffixes.next();
            context.append(entry.chars);
            index = addConditionalCE32(context.toString(), copyFromBaseCE32(c, entry.value, true));
            cond.next = index;
            cond = getConditionalCE32(index);
            context.setLength(suffixStart);
        }
        if (!f37assertionsDisabled) {
            if (!(index >= 0)) {
                throw new AssertionError();
            }
        }
        return index;
    }

    private static final class CopyHelper {

        static final boolean f38assertionsDisabled;
        CollationDataBuilder dest;
        long[] modifiedCEs = new long[31];
        CEModifier modifier;
        CollationDataBuilder src;

        static {
            f38assertionsDisabled = !CopyHelper.class.desiredAssertionStatus();
        }

        CopyHelper(CollationDataBuilder s, CollationDataBuilder d, CEModifier m) {
            this.src = s;
            this.dest = d;
            this.modifier = m;
        }

        void copyRangeCE32(int start, int end, int ce32) {
            int ce322 = copyCE32(ce32);
            this.dest.trie.setRange(start, end, ce322, true);
            if (!CollationDataBuilder.isBuilderContextCE32(ce322)) {
                return;
            }
            this.dest.contextChars.add(start, end);
        }

        int copyCE32(int ce32) {
            if (!Collation.isSpecialCE32(ce32)) {
                long ce = this.modifier.modifyCE32(ce32);
                if (ce != Collation.NO_CE) {
                    return this.dest.encodeOneCE(ce);
                }
                return ce32;
            }
            int tag = Collation.tagFromCE32(ce32);
            if (tag == 5) {
                int[] srcCE32s = this.src.ce32s.getBuffer();
                int srcIndex = Collation.indexFromCE32(ce32);
                int length = Collation.lengthFromCE32(ce32);
                boolean isModified = false;
                for (int i = 0; i < length; i++) {
                    int ce322 = srcCE32s[srcIndex + i];
                    if (!Collation.isSpecialCE32(ce322)) {
                        long ce2 = this.modifier.modifyCE32(ce322);
                        if (ce2 == Collation.NO_CE) {
                            if (isModified) {
                                this.modifiedCEs[i] = Collation.ceFromCE32(ce322);
                            }
                        } else {
                            if (!isModified) {
                                for (int j = 0; j < i; j++) {
                                    this.modifiedCEs[j] = Collation.ceFromCE32(srcCE32s[srcIndex + j]);
                                }
                                isModified = true;
                            }
                            this.modifiedCEs[i] = ce2;
                        }
                    }
                }
                if (isModified) {
                    return this.dest.encodeCEs(this.modifiedCEs, length);
                }
                return this.dest.encodeExpansion32(srcCE32s, srcIndex, length);
            }
            if (tag == 6) {
                long[] srcCEs = this.src.ce64s.getBuffer();
                int srcIndex2 = Collation.indexFromCE32(ce32);
                int length2 = Collation.lengthFromCE32(ce32);
                boolean isModified2 = false;
                for (int i2 = 0; i2 < length2; i2++) {
                    long srcCE = srcCEs[srcIndex2 + i2];
                    long ce3 = this.modifier.modifyCE(srcCE);
                    if (ce3 == Collation.NO_CE) {
                        if (isModified2) {
                            this.modifiedCEs[i2] = srcCE;
                        }
                    } else {
                        if (!isModified2) {
                            for (int j2 = 0; j2 < i2; j2++) {
                                this.modifiedCEs[j2] = srcCEs[srcIndex2 + j2];
                            }
                            isModified2 = true;
                        }
                        this.modifiedCEs[i2] = ce3;
                    }
                }
                if (isModified2) {
                    return this.dest.encodeCEs(this.modifiedCEs, length2);
                }
                return this.dest.encodeExpansion(srcCEs, srcIndex2, length2);
            }
            if (tag == 7) {
                ConditionalCE32 cond = this.src.getConditionalCE32ForCE32(ce32);
                if (!f38assertionsDisabled) {
                    if (!(!cond.hasContext())) {
                        throw new AssertionError();
                    }
                }
                int destIndex = this.dest.addConditionalCE32(cond.context, copyCE32(cond.ce32));
                int ce323 = CollationDataBuilder.makeBuilderContextCE32(destIndex);
                while (cond.next >= 0) {
                    cond = this.src.getConditionalCE32(cond.next);
                    ConditionalCE32 prevDestCond = this.dest.getConditionalCE32(destIndex);
                    destIndex = this.dest.addConditionalCE32(cond.context, copyCE32(cond.ce32));
                    int suffixStart = cond.prefixLength() + 1;
                    this.dest.unsafeBackwardSet.addAll(cond.context.substring(suffixStart));
                    prevDestCond.next = destIndex;
                }
                return ce323;
            }
            if (!f38assertionsDisabled) {
                boolean z = tag == 1 || tag == 2 || tag == 4 || tag == 12;
                if (z) {
                    return ce32;
                }
                throw new AssertionError();
            }
            return ce32;
        }
    }

    private static void enumRangeForCopy(int start, int end, int value, CopyHelper helper) {
        if (value == -1 || value == 192) {
            return;
        }
        helper.copyRangeCE32(start, end, value);
    }

    protected boolean getJamoCE32s(int[] jamoCE32s) {
        boolean anyJamoAssigned = this.base == null;
        boolean needToCopyFromBase = false;
        for (int j = 0; j < 67; j++) {
            int jamo = jamoCpFromIndex(j);
            boolean fromBase = false;
            int ce32 = this.trie.get(jamo);
            anyJamoAssigned |= Collation.isAssignedCE32(ce32);
            if (ce32 == 192) {
                fromBase = true;
                ce32 = this.base.getCE32(jamo);
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
                    case 5:
                    case 6:
                    case 8:
                    case 9:
                        if (fromBase) {
                            ce32 = 192;
                            needToCopyFromBase = true;
                        }
                        break;
                    case 14:
                        ce32 = getCE32FromOffsetCE32(fromBase, jamo, ce32);
                        break;
                    case 15:
                        if (!f37assertionsDisabled && !fromBase) {
                            throw new AssertionError();
                        }
                        ce32 = 192;
                        needToCopyFromBase = true;
                        break;
                        break;
                }
            }
            jamoCE32s[j] = ce32;
        }
        if (anyJamoAssigned && needToCopyFromBase) {
            for (int j2 = 0; j2 < 67; j2++) {
                if (jamoCE32s[j2] == 192) {
                    int jamo2 = jamoCpFromIndex(j2);
                    jamoCE32s[j2] = copyFromBaseCE32(jamo2, this.base.getCE32(jamo2), true);
                }
            }
        }
        return anyJamoAssigned;
    }

    protected void setDigitTags() {
        UnicodeSet digits = new UnicodeSet("[:Nd:]");
        UnicodeSetIterator iter = new UnicodeSetIterator(digits);
        while (iter.next()) {
            if (!f37assertionsDisabled) {
                if (!(iter.codepoint != UnicodeSetIterator.IS_STRING)) {
                    throw new AssertionError();
                }
            }
            int c = iter.codepoint;
            int ce32 = this.trie.get(c);
            if (ce32 != 192 && ce32 != -1) {
                int index = addCE32(ce32);
                if (index > 524287) {
                    throw new IndexOutOfBoundsException("too many mappings");
                }
                this.trie.set(c, Collation.makeCE32FromTagIndexAndLength(10, index, UCharacter.digit(c)));
            }
        }
    }

    protected void setLeadSurrogates() {
        int value;
        for (char c = 55296; c < 56320; c = (char) (c + 1)) {
            int leadValue = -1;
            Iterator<Trie2.Range> trieIterator = this.trie.iteratorForLeadSurrogate(c);
            while (true) {
                if (trieIterator.hasNext()) {
                    Trie2.Range range = trieIterator.next();
                    int value2 = range.value;
                    if (value2 == -1) {
                        value = 0;
                    } else if (value2 == 192) {
                        value = 256;
                    } else {
                        leadValue = 512;
                        break;
                    }
                    if (leadValue < 0) {
                        leadValue = value;
                    } else if (leadValue != value) {
                        leadValue = 512;
                        break;
                    }
                }
            }
            this.trie.setForLeadSurrogateCodeUnit(c, Collation.makeCE32FromTagAndIndex(13, 0) | leadValue);
        }
    }

    protected void buildMappings(CollationData data) {
        if (!isMutable()) {
            throw new IllegalStateException("attempt to build() after build()");
        }
        buildContexts();
        int[] jamoCE32s = new int[67];
        int jamoIndex = -1;
        if (getJamoCE32s(jamoCE32s)) {
            jamoIndex = this.ce32s.size();
            for (int i = 0; i < 67; i++) {
                this.ce32s.addElement(jamoCE32s[i]);
            }
            boolean isAnyJamoVTSpecial = false;
            int i2 = 19;
            while (true) {
                if (i2 >= 67) {
                    break;
                }
                if (!Collation.isSpecialCE32(jamoCE32s[i2])) {
                    i2++;
                } else {
                    isAnyJamoVTSpecial = true;
                    break;
                }
            }
            int hangulCE32 = Collation.makeCE32FromTagAndIndex(12, 0);
            int c = Normalizer2Impl.Hangul.HANGUL_BASE;
            for (int i3 = 0; i3 < 19; i3++) {
                int ce32 = hangulCE32;
                if (!isAnyJamoVTSpecial && !Collation.isSpecialCE32(jamoCE32s[i3])) {
                    ce32 = hangulCE32 | 256;
                }
                int limit = c + Normalizer2Impl.Hangul.JAMO_VT_COUNT;
                this.trie.setRange(c, limit - 1, ce32, true);
                c = limit;
            }
        } else {
            int c2 = Normalizer2Impl.Hangul.HANGUL_BASE;
            while (c2 < 55204) {
                int ce322 = this.base.getCE32(c2);
                if (!f37assertionsDisabled && !Collation.hasCE32Tag(ce322, 12)) {
                    throw new AssertionError();
                }
                int limit2 = c2 + Normalizer2Impl.Hangul.JAMO_VT_COUNT;
                this.trie.setRange(c2, limit2 - 1, ce322, true);
                c2 = limit2;
            }
        }
        setDigitTags();
        setLeadSurrogates();
        this.ce32s.setElementAt(this.trie.get(0), 0);
        this.trie.set(0, Collation.makeCE32FromTagAndIndex(11, 0));
        data.trie = this.trie.toTrie2_32();
        int c3 = 65536;
        char c4 = 55296;
        while (c4 < 56320) {
            if (this.unsafeBackwardSet.containsSome(c3, c3 + Opcodes.OP_NEW_INSTANCE_JUMBO)) {
                this.unsafeBackwardSet.add(c4);
            }
            c4 = (char) (c4 + 1);
            c3 += 1024;
        }
        this.unsafeBackwardSet.freeze();
        data.ce32s = this.ce32s.getBuffer();
        data.ces = this.ce64s.getBuffer();
        data.contexts = this.contexts.toString();
        data.base = this.base;
        if (jamoIndex >= 0) {
            data.jamoCE32s = jamoCE32s;
        } else {
            data.jamoCE32s = this.base.jamoCE32s;
        }
        data.unsafeBackwardSet = this.unsafeBackwardSet;
    }

    protected void clearContexts() {
        this.contexts.setLength(0);
        UnicodeSetIterator iter = new UnicodeSetIterator(this.contextChars);
        while (iter.next()) {
            if (!f37assertionsDisabled) {
                if (!(iter.codepoint != UnicodeSetIterator.IS_STRING)) {
                    throw new AssertionError();
                }
            }
            int ce32 = this.trie.get(iter.codepoint);
            if (!f37assertionsDisabled && !isBuilderContextCE32(ce32)) {
                throw new AssertionError();
            }
            getConditionalCE32ForCE32(ce32).builtCE32 = 1;
        }
    }

    protected void buildContexts() {
        this.contexts.setLength(0);
        UnicodeSetIterator iter = new UnicodeSetIterator(this.contextChars);
        while (iter.next()) {
            if (!f37assertionsDisabled) {
                if (!(iter.codepoint != UnicodeSetIterator.IS_STRING)) {
                    throw new AssertionError();
                }
            }
            int c = iter.codepoint;
            int ce32 = this.trie.get(c);
            if (!isBuilderContextCE32(ce32)) {
                throw new AssertionError("Impossible: No context data for c in contextChars.");
            }
            ConditionalCE32 cond = getConditionalCE32ForCE32(ce32);
            this.trie.set(c, buildContext(cond));
        }
    }

    protected int buildContext(ConditionalCE32 head) {
        ConditionalCE32 lastCond;
        ConditionalCE32 cond;
        int ce32;
        if (!f37assertionsDisabled) {
            if (!(!head.hasContext())) {
                throw new AssertionError();
            }
        }
        if (!f37assertionsDisabled) {
            if (!(head.next >= 0)) {
                throw new AssertionError();
            }
        }
        CharsTrieBuilder prefixBuilder = new CharsTrieBuilder();
        CharsTrieBuilder contractionBuilder = new CharsTrieBuilder();
        ConditionalCE32 cond2 = head;
        while (true) {
            if (!f37assertionsDisabled) {
                if (!(cond2 != head ? cond2.hasContext() : true)) {
                    throw new AssertionError();
                }
            }
            int prefixLength = cond2.prefixLength();
            StringBuilder prefix = new StringBuilder().append((CharSequence) cond2.context, 0, prefixLength + 1);
            String prefixString = prefix.toString();
            ConditionalCE32 firstCond = cond2;
            do {
                lastCond = cond2;
                if (cond2.next < 0) {
                    break;
                }
                cond2 = getConditionalCE32(cond2.next);
            } while (cond2.context.startsWith(prefixString));
            int suffixStart = prefixLength + 1;
            if (lastCond.context.length() == suffixStart) {
                if (!f37assertionsDisabled) {
                    if (!(firstCond == lastCond)) {
                        throw new AssertionError();
                    }
                }
                ce32 = lastCond.ce32;
                cond = lastCond;
            } else {
                contractionBuilder.clear();
                int emptySuffixCE32 = 1;
                int flags = 0;
                if (firstCond.context.length() == suffixStart) {
                    emptySuffixCE32 = firstCond.ce32;
                    cond = getConditionalCE32(firstCond.next);
                } else {
                    flags = 256;
                    ConditionalCE32 cond3 = head;
                    while (true) {
                        int length = cond3.prefixLength();
                        if (length == prefixLength) {
                            break;
                        }
                        if (cond3.defaultCE32 != 1 && (length == 0 || prefixString.regionMatches(prefix.length() - length, cond3.context, 1, length))) {
                            emptySuffixCE32 = cond3.defaultCE32;
                        }
                        cond3 = getConditionalCE32(cond3.next);
                    }
                    cond = firstCond;
                }
                int flags2 = flags | 512;
                while (true) {
                    String suffix = cond.context.substring(suffixStart);
                    int fcd16 = this.nfcImpl.getFCD16(suffix.codePointAt(0));
                    if (fcd16 <= 255) {
                        flags2 &= -513;
                    }
                    int fcd162 = this.nfcImpl.getFCD16(suffix.codePointBefore(suffix.length()));
                    if (fcd162 > 255) {
                        flags2 |= 1024;
                    }
                    contractionBuilder.add(suffix, cond.ce32);
                    if (cond == lastCond) {
                        break;
                    }
                    cond = getConditionalCE32(cond.next);
                }
                int index = addContextTrie(emptySuffixCE32, contractionBuilder);
                if (index > 524287) {
                    throw new IndexOutOfBoundsException("too many context-sensitive mappings");
                }
                ce32 = Collation.makeCE32FromTagAndIndex(9, index) | flags2;
            }
            if (!f37assertionsDisabled) {
                if (!(cond == lastCond)) {
                    throw new AssertionError();
                }
            }
            firstCond.defaultCE32 = ce32;
            if (prefixLength == 0) {
                if (cond.next < 0) {
                    return ce32;
                }
            } else {
                prefix.delete(0, 1);
                prefix.reverse();
                prefixBuilder.add(prefix, ce32);
                if (cond.next < 0) {
                    if (!f37assertionsDisabled) {
                        if (!(head.defaultCE32 != 1)) {
                            throw new AssertionError();
                        }
                    }
                    int index2 = addContextTrie(head.defaultCE32, prefixBuilder);
                    if (index2 > 524287) {
                        throw new IndexOutOfBoundsException("too many context-sensitive mappings");
                    }
                    return Collation.makeCE32FromTagAndIndex(8, index2);
                }
            }
            cond2 = getConditionalCE32(cond.next);
        }
    }

    protected int addContextTrie(int defaultCE32, CharsTrieBuilder trieBuilder) {
        StringBuilder context = new StringBuilder();
        context.append((char) (defaultCE32 >> 16)).append((char) defaultCE32);
        context.append(trieBuilder.buildCharSequence(StringTrieBuilder.Option.SMALL));
        int index = this.contexts.indexOf(context.toString());
        if (index < 0) {
            int index2 = this.contexts.length();
            this.contexts.append((CharSequence) context);
            return index2;
        }
        return index;
    }

    protected void buildFastLatinTable(CollationData data) {
        if (this.fastLatinEnabled) {
            this.fastLatinBuilder = new CollationFastLatinBuilder();
            if (this.fastLatinBuilder.forData(data)) {
                char[] header = this.fastLatinBuilder.getHeader();
                char[] table = this.fastLatinBuilder.getTable();
                if (this.base != null && Arrays.equals(header, this.base.fastLatinTableHeader) && Arrays.equals(table, this.base.fastLatinTable)) {
                    this.fastLatinBuilder = null;
                    header = this.base.fastLatinTableHeader;
                    table = this.base.fastLatinTable;
                }
                data.fastLatinTableHeader = header;
                data.fastLatinTable = table;
                return;
            }
            this.fastLatinBuilder = null;
        }
    }

    protected int getCEs(CharSequence s, int start, long[] ces, int cesLength) {
        if (this.collIter == null) {
            this.collIter = new DataBuilderCollationIterator(this, new CollationData(this.nfcImpl));
            if (this.collIter == null) {
                return 0;
            }
        }
        return this.collIter.fetchCEs(s, start, ces, cesLength);
    }

    protected static int jamoCpFromIndex(int i) {
        if (i < 19) {
            return i + Normalizer2Impl.Hangul.JAMO_L_BASE;
        }
        int i2 = i - 19;
        return i2 < 21 ? i2 + Normalizer2Impl.Hangul.JAMO_V_BASE : (i2 - 21) + 4520;
    }

    private static final class DataBuilderCollationIterator extends CollationIterator {

        static final boolean f39assertionsDisabled;
        protected final CollationDataBuilder builder;
        protected final CollationData builderData;
        protected final int[] jamoCE32s;
        protected int pos;
        protected CharSequence s;

        static {
            f39assertionsDisabled = !DataBuilderCollationIterator.class.desiredAssertionStatus();
        }

        DataBuilderCollationIterator(CollationDataBuilder b, CollationData newData) {
            super(newData, false);
            this.jamoCE32s = new int[67];
            this.builder = b;
            this.builderData = newData;
            this.builderData.base = this.builder.base;
            for (int j = 0; j < 67; j++) {
                int jamo = CollationDataBuilder.jamoCpFromIndex(j);
                this.jamoCE32s[j] = Collation.makeCE32FromTagAndIndex(7, jamo) | 256;
            }
            this.builderData.jamoCE32s = this.jamoCE32s;
        }

        int fetchCEs(CharSequence str, int start, long[] ces, int cesLength) {
            CollationData d;
            this.builderData.ce32s = this.builder.ce32s.getBuffer();
            this.builderData.ces = this.builder.ce64s.getBuffer();
            this.builderData.contexts = this.builder.contexts.toString();
            reset();
            this.s = str;
            this.pos = start;
            while (this.pos < this.s.length()) {
                clearCEs();
                int c = Character.codePointAt(this.s, this.pos);
                this.pos += Character.charCount(c);
                int ce32 = this.builder.trie.get(c);
                if (ce32 == 192) {
                    d = this.builder.base;
                    ce32 = this.builder.base.getCE32(c);
                } else {
                    d = this.builderData;
                }
                appendCEsFromCE32(d, c, ce32, true);
                for (int i = 0; i < getCEsLength(); i++) {
                    long ce = getCE(i);
                    if (ce != 0) {
                        if (cesLength < 31) {
                            ces[cesLength] = ce;
                        }
                        cesLength++;
                    }
                }
            }
            return cesLength;
        }

        @Override
        public void resetToOffset(int newOffset) {
            reset();
            this.pos = newOffset;
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
            int c = Character.codePointAt(this.s, this.pos);
            this.pos += Character.charCount(c);
            return c;
        }

        @Override
        public int previousCodePoint() {
            if (this.pos == 0) {
                return -1;
            }
            int c = Character.codePointBefore(this.s, this.pos);
            this.pos -= Character.charCount(c);
            return c;
        }

        @Override
        protected void forwardNumCodePoints(int num) {
            this.pos = Character.offsetByCodePoints(this.s, this.pos, num);
        }

        @Override
        protected void backwardNumCodePoints(int num) {
            this.pos = Character.offsetByCodePoints(this.s, this.pos, -num);
        }

        @Override
        protected int getDataCE32(int c) {
            return this.builder.trie.get(c);
        }

        @Override
        protected int getCE32FromBuilderData(int ce32) {
            if (!f39assertionsDisabled && !Collation.hasCE32Tag(ce32, 7)) {
                throw new AssertionError();
            }
            if ((ce32 & 256) != 0) {
                int jamo = Collation.indexFromCE32(ce32);
                return this.builder.trie.get(jamo);
            }
            ConditionalCE32 cond = this.builder.getConditionalCE32ForCE32(ce32);
            if (cond.builtCE32 == 1) {
                try {
                    cond.builtCE32 = this.builder.buildContext(cond);
                } catch (IndexOutOfBoundsException e) {
                    this.builder.clearContexts();
                    cond.builtCE32 = this.builder.buildContext(cond);
                }
                this.builderData.contexts = this.builder.contexts.toString();
            }
            return cond.builtCE32;
        }
    }

    protected final boolean isMutable() {
        return (this.trie == null || this.unsafeBackwardSet == null || this.unsafeBackwardSet.isFrozen()) ? false : true;
    }
}
