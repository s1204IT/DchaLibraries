package android.icu.impl.coll;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.coll.CollationDataBuilder;
import android.icu.impl.coll.CollationRuleParser;
import android.icu.text.CanonicalIterator;
import android.icu.text.Normalizer2;
import android.icu.text.UnicodeSet;
import android.icu.text.UnicodeSetIterator;
import android.icu.util.ULocale;
import java.text.ParseException;

public final class CollationBuilder extends CollationRuleParser.Sink {

    private static final int[] f32x5b9f57fe = null;

    static final boolean f33assertionsDisabled;
    private static final UnicodeSet COMPOSITES;
    private static final boolean DEBUG = false;
    private static final int HAS_BEFORE2 = 64;
    private static final int HAS_BEFORE3 = 32;
    private static final int IS_TAILORED = 8;
    private static final int MAX_INDEX = 1048575;
    private CollationTailoring base;
    private CollationData baseData;
    private CollationRootElements rootElements;
    private UnicodeSet optimizeSet = new UnicodeSet();
    private long[] ces = new long[31];
    private Normalizer2 nfd = Normalizer2.getNFDInstance();
    private Normalizer2 fcd = Norm2AllModes.getFCDNormalizer2();
    private Normalizer2Impl nfcImpl = Norm2AllModes.getNFCInstance().impl;
    private long variableTop = 0;
    private CollationDataBuilder dataBuilder = new CollationDataBuilder();
    private boolean fastLatinEnabled = true;
    private int cesLength = 0;
    private UVector32 rootPrimaryIndexes = new UVector32();
    private UVector64 nodes = new UVector64();

    private static int[] m71xbaa87bda() {
        if (f32x5b9f57fe != null) {
            return f32x5b9f57fe;
        }
        int[] iArr = new int[CollationRuleParser.Position.valuesCustom().length];
        try {
            iArr[CollationRuleParser.Position.FIRST_IMPLICIT.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[CollationRuleParser.Position.FIRST_PRIMARY_IGNORABLE.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[CollationRuleParser.Position.FIRST_REGULAR.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[CollationRuleParser.Position.FIRST_SECONDARY_IGNORABLE.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[CollationRuleParser.Position.FIRST_TERTIARY_IGNORABLE.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[CollationRuleParser.Position.FIRST_TRAILING.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[CollationRuleParser.Position.FIRST_VARIABLE.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[CollationRuleParser.Position.LAST_IMPLICIT.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[CollationRuleParser.Position.LAST_PRIMARY_IGNORABLE.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[CollationRuleParser.Position.LAST_REGULAR.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[CollationRuleParser.Position.LAST_SECONDARY_IGNORABLE.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[CollationRuleParser.Position.LAST_TERTIARY_IGNORABLE.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[CollationRuleParser.Position.LAST_TRAILING.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[CollationRuleParser.Position.LAST_VARIABLE.ordinal()] = 14;
        } catch (NoSuchFieldError e14) {
        }
        f32x5b9f57fe = iArr;
        return iArr;
    }

    private static final class BundleImporter implements CollationRuleParser.Importer {
        BundleImporter() {
        }

        @Override
        public String getRules(String localeID, String collationType) {
            return CollationLoader.loadRules(new ULocale(localeID), collationType);
        }
    }

    public CollationBuilder(CollationTailoring b) {
        this.base = b;
        this.baseData = b.data;
        this.rootElements = new CollationRootElements(b.data.rootElements);
        this.nfcImpl.ensureCanonIterData();
        this.dataBuilder.initForTailoring(this.baseData);
    }

    public CollationTailoring parseAndBuild(String ruleString) throws ParseException {
        if (this.baseData.rootElements == null) {
            throw new UnsupportedOperationException("missing root elements data, tailoring not supported");
        }
        CollationTailoring tailoring = new CollationTailoring(this.base.settings);
        CollationRuleParser parser = new CollationRuleParser(this.baseData);
        this.variableTop = ((CollationSettings) this.base.settings.readOnly()).variableTop;
        parser.setSink(this);
        parser.setImporter(new BundleImporter());
        CollationSettings ownedSettings = (CollationSettings) tailoring.settings.copyOnWrite();
        parser.parse(ruleString, ownedSettings);
        if (this.dataBuilder.hasMappings()) {
            makeTailoredCEs();
            closeOverComposites();
            finalizeCEs();
            this.optimizeSet.add(0, 127);
            this.optimizeSet.add(192, 255);
            this.optimizeSet.remove(Normalizer2Impl.Hangul.HANGUL_BASE, Normalizer2Impl.Hangul.HANGUL_END);
            this.dataBuilder.optimize(this.optimizeSet);
            tailoring.ensureOwnedData();
            if (this.fastLatinEnabled) {
                this.dataBuilder.enableFastLatin();
            }
            this.dataBuilder.build(tailoring.ownedData);
            this.dataBuilder = null;
        } else {
            tailoring.data = this.baseData;
        }
        ownedSettings.fastLatinOptions = CollationFastLatin.getOptions(tailoring.data, ownedSettings, ownedSettings.fastLatinPrimaries);
        tailoring.setRules(ruleString);
        tailoring.setVersion(this.base.version, 0);
        return tailoring;
    }

    @Override
    void addReset(int strength, CharSequence str) {
        int index;
        int previousWeight16;
        String str2;
        if (!f33assertionsDisabled) {
            if (!(str.length() != 0)) {
                throw new AssertionError();
            }
        }
        if (str.charAt(0) == 65534) {
            this.ces[0] = getSpecialResetPosition(str);
            this.cesLength = 1;
            if (!f33assertionsDisabled) {
                if (!((this.ces[0] & 49344) == 0)) {
                    throw new AssertionError();
                }
            }
        } else {
            String nfdString = this.nfd.normalize(str);
            this.cesLength = this.dataBuilder.getCEs(nfdString, this.ces, 0);
            if (this.cesLength > 31) {
                throw new IllegalArgumentException("reset position maps to too many collation elements (more than 31)");
            }
        }
        if (strength == 15) {
            return;
        }
        if (!f33assertionsDisabled) {
            if (!(strength >= 0 && strength <= 2)) {
                throw new AssertionError();
            }
        }
        int index2 = findOrInsertNodeForCEs(strength);
        long node = this.nodes.elementAti(index2);
        while (strengthFromNode(node) > strength) {
            index2 = previousIndexFromNode(node);
            node = this.nodes.elementAti(index2);
        }
        if (strengthFromNode(node) == strength && isTailoredNode(node)) {
            index = previousIndexFromNode(node);
        } else if (strength == 0) {
            long p = weight32FromNode(node);
            if (p != 0) {
                if (p > this.rootElements.getFirstPrimary()) {
                    if (p == 4278321664L) {
                        throw new UnsupportedOperationException("reset primary-before [first trailing] not supported");
                    }
                    index = findOrInsertNodeForPrimary(this.rootElements.getPrimaryBefore(p, this.baseData.isCompressiblePrimary(p)));
                    while (true) {
                        long node2 = this.nodes.elementAti(index);
                        int nextIndex = nextIndexFromNode(node2);
                        if (nextIndex == 0) {
                            break;
                        } else {
                            index = nextIndex;
                        }
                    }
                } else {
                    throw new UnsupportedOperationException("reset primary-before first non-ignorable not supported");
                }
            } else {
                throw new UnsupportedOperationException("reset primary-before ignorable not possible");
            }
        } else {
            int index3 = findCommonNode(index2, 1);
            if (strength >= 2) {
                index3 = findCommonNode(index3, 2);
            }
            long node3 = this.nodes.elementAti(index3);
            if (strengthFromNode(node3) == strength) {
                int weight16 = weight16FromNode(node3);
                if (weight16 == 0) {
                    if (strength == 1) {
                        str2 = "reset secondary-before secondary ignorable not possible";
                    } else {
                        str2 = "reset tertiary-before completely ignorable not possible";
                    }
                    throw new UnsupportedOperationException(str2);
                }
                if (!f33assertionsDisabled) {
                    if (!(weight16 > 256)) {
                        throw new AssertionError();
                    }
                }
                int weight162 = getWeight16Before(index3, node3, strength);
                int previousIndex = previousIndexFromNode(node3);
                int i = previousIndex;
                while (true) {
                    long node4 = this.nodes.elementAti(i);
                    int previousStrength = strengthFromNode(node4);
                    if (previousStrength < strength) {
                        if (!f33assertionsDisabled) {
                            if (!(weight162 >= 1280 || i == previousIndex)) {
                                throw new AssertionError();
                            }
                        }
                        previousWeight16 = Collation.COMMON_WEIGHT16;
                    } else if (previousStrength != strength || isTailoredNode(node4)) {
                        i = previousIndexFromNode(node4);
                    } else {
                        previousWeight16 = weight16FromNode(node4);
                        break;
                    }
                }
                if (previousWeight16 == weight162) {
                    index = previousIndex;
                } else {
                    long node5 = nodeFromWeight16(weight162) | nodeFromStrength(strength);
                    index = insertNodeBetween(previousIndex, index3, node5);
                }
            } else {
                index = findOrInsertWeakNode(index3, getWeight16Before(index3, node3, strength), strength);
            }
            strength = ceStrength(this.ces[this.cesLength - 1]);
        }
        this.ces[this.cesLength - 1] = tempCEFromIndexAndStrength(index, strength);
    }

    private int getWeight16Before(int index, long node, int level) {
        int t;
        int s;
        if (!f33assertionsDisabled) {
            if (!(strengthFromNode(node) < level || !isTailoredNode(node))) {
                throw new AssertionError();
            }
        }
        if (strengthFromNode(node) == 2) {
            t = weight16FromNode(node);
        } else {
            t = Collation.COMMON_WEIGHT16;
        }
        while (strengthFromNode(node) > 1) {
            int index2 = previousIndexFromNode(node);
            node = this.nodes.elementAti(index2);
        }
        if (isTailoredNode(node)) {
            return 256;
        }
        if (strengthFromNode(node) == 1) {
            s = weight16FromNode(node);
        } else {
            s = Collation.COMMON_WEIGHT16;
        }
        while (strengthFromNode(node) > 0) {
            int index3 = previousIndexFromNode(node);
            node = this.nodes.elementAti(index3);
        }
        if (isTailoredNode(node)) {
            return 256;
        }
        long p = weight32FromNode(node);
        if (level == 1) {
            return this.rootElements.getSecondaryBefore(p, s);
        }
        int weight16 = this.rootElements.getTertiaryBefore(p, s, t);
        if (f33assertionsDisabled) {
            return weight16;
        }
        if ((weight16 & (-16192)) == 0) {
            return weight16;
        }
        throw new AssertionError();
    }

    private long getSpecialResetPosition(CharSequence str) {
        long ce;
        int strength;
        if (!f33assertionsDisabled) {
            if (!(str.length() == 2)) {
                throw new AssertionError();
            }
        }
        int strength2 = 0;
        boolean isBoundary = false;
        CollationRuleParser.Position pos = CollationRuleParser.POSITION_VALUES[str.charAt(1) - 10240];
        switch (m71xbaa87bda()[pos.ordinal()]) {
            case 1:
                ce = this.baseData.getSingleCE(19968);
                break;
            case 2:
                long node = this.nodes.elementAti(findOrInsertNodeForRootCE(0L, 1));
                while (true) {
                    int index = nextIndexFromNode(node);
                    if (index != 0 && (strength = strengthFromNode((node = this.nodes.elementAti(index)))) >= 1) {
                        if (strength == 1) {
                            if (isTailoredNode(node)) {
                                if (nodeHasBefore3(node)) {
                                    index = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(node)));
                                    if (!f33assertionsDisabled && !isTailoredNode(this.nodes.elementAti(index))) {
                                        throw new AssertionError();
                                    }
                                }
                                return tempCEFromIndexAndStrength(index, 1);
                            }
                        }
                    }
                }
                ce = this.rootElements.getFirstSecondaryCE();
                strength2 = 1;
                break;
            case 3:
                ce = this.rootElements.firstCEWithPrimaryAtLeast(this.variableTop + 1);
                isBoundary = true;
                break;
            case 4:
                long node2 = this.nodes.elementAti(findOrInsertNodeForRootCE(0L, 2));
                int index2 = nextIndexFromNode(node2);
                if (index2 != 0) {
                    long node3 = this.nodes.elementAti(index2);
                    if (!f33assertionsDisabled) {
                        if (!(strengthFromNode(node3) <= 2)) {
                            throw new AssertionError();
                        }
                    }
                    if (isTailoredNode(node3) && strengthFromNode(node3) == 2) {
                        return tempCEFromIndexAndStrength(index2, 2);
                    }
                }
                return this.rootElements.getFirstTertiaryCE();
            case 5:
                return 0L;
            case 6:
                ce = Collation.makeCE(4278321664L);
                isBoundary = true;
                break;
            case 7:
                ce = this.rootElements.getFirstPrimaryCE();
                isBoundary = true;
                break;
            case 8:
                throw new UnsupportedOperationException("reset to [last implicit] not supported");
            case 9:
                ce = this.rootElements.getLastSecondaryCE();
                strength2 = 1;
                break;
            case 10:
                ce = this.rootElements.firstCEWithPrimaryAtLeast(this.baseData.getFirstPrimaryForGroup(17));
                break;
            case 11:
                ce = this.rootElements.getLastTertiaryCE();
                strength2 = 2;
                break;
            case 12:
                return 0L;
            case 13:
                throw new IllegalArgumentException("LDML forbids tailoring to U+FFFF");
            case 14:
                ce = this.rootElements.lastCEWithPrimaryBefore(this.variableTop + 1);
                break;
            default:
                if (f33assertionsDisabled) {
                    return 0L;
                }
                throw new AssertionError();
        }
        int index3 = findOrInsertNodeForRootCE(ce, strength2);
        long node4 = this.nodes.elementAti(index3);
        if ((pos.ordinal() & 1) == 0) {
            if (!nodeHasAnyBefore(node4) && isBoundary) {
                index3 = nextIndexFromNode(node4);
                if (index3 != 0) {
                    node4 = this.nodes.elementAti(index3);
                    if (!f33assertionsDisabled && !isTailoredNode(node4)) {
                        throw new AssertionError();
                    }
                    ce = tempCEFromIndexAndStrength(index3, strength2);
                } else {
                    if (!f33assertionsDisabled) {
                        if (!(strength2 == 0)) {
                            throw new AssertionError();
                        }
                    }
                    long p = ce >>> 32;
                    int pIndex = this.rootElements.findPrimary(p);
                    boolean isCompressible = this.baseData.isCompressiblePrimary(p);
                    ce = Collation.makeCE(this.rootElements.getPrimaryAfter(p, pIndex, isCompressible));
                    index3 = findOrInsertNodeForRootCE(ce, 0);
                    node4 = this.nodes.elementAti(index3);
                }
            }
            if (nodeHasAnyBefore(node4)) {
                if (nodeHasBefore2(node4)) {
                    index3 = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(node4)));
                    node4 = this.nodes.elementAti(index3);
                }
                if (nodeHasBefore3(node4)) {
                    index3 = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(node4)));
                }
                if (!f33assertionsDisabled && !isTailoredNode(this.nodes.elementAti(index3))) {
                    throw new AssertionError();
                }
                long ce2 = tempCEFromIndexAndStrength(index3, strength2);
                return ce2;
            }
            return ce;
        }
        while (true) {
            int nextIndex = nextIndexFromNode(node4);
            if (nextIndex != 0) {
                long nextNode = this.nodes.elementAti(nextIndex);
                if (strengthFromNode(nextNode) >= strength2) {
                    index3 = nextIndex;
                    node4 = nextNode;
                }
            }
        }
        if (isTailoredNode(node4)) {
            long ce3 = tempCEFromIndexAndStrength(index3, strength2);
            return ce3;
        }
        return ce;
    }

    @Override
    void addRelation(int strength, CharSequence prefix, CharSequence str, CharSequence extension) {
        String nfdPrefix;
        if (prefix.length() == 0) {
            nfdPrefix = "";
        } else {
            nfdPrefix = this.nfd.normalize(prefix);
        }
        String nfdString = this.nfd.normalize(str);
        int nfdLength = nfdString.length();
        if (nfdLength >= 2) {
            char c = nfdString.charAt(0);
            if (Normalizer2Impl.Hangul.isJamoL(c) || Normalizer2Impl.Hangul.isJamoV(c)) {
                throw new UnsupportedOperationException("contractions starting with conjoining Jamo L or V not supported");
            }
            char c2 = nfdString.charAt(nfdLength - 1);
            if (Normalizer2Impl.Hangul.isJamoL(c2) || (Normalizer2Impl.Hangul.isJamoV(c2) && Normalizer2Impl.Hangul.isJamoL(nfdString.charAt(nfdLength - 2)))) {
                throw new UnsupportedOperationException("contractions ending with conjoining Jamo L or L+V not supported");
            }
        }
        if (strength != 15) {
            int index = findOrInsertNodeForCEs(strength);
            if (!f33assertionsDisabled) {
                if (!(this.cesLength > 0)) {
                    throw new AssertionError();
                }
            }
            long ce = this.ces[this.cesLength - 1];
            if (strength == 0 && !isTempCE(ce) && (ce >>> 32) == 0) {
                throw new UnsupportedOperationException("tailoring primary after ignorables not supported");
            }
            if (strength == 3 && ce == 0) {
                throw new UnsupportedOperationException("tailoring quaternary after tertiary ignorables not supported");
            }
            int index2 = insertTailoredNodeAfter(index, strength);
            int tempStrength = ceStrength(ce);
            if (strength < tempStrength) {
                tempStrength = strength;
            }
            this.ces[this.cesLength - 1] = tempCEFromIndexAndStrength(index2, tempStrength);
        }
        setCaseBits(nfdString);
        int cesLengthBeforeExtension = this.cesLength;
        if (extension.length() != 0) {
            String nfdExtension = this.nfd.normalize(extension);
            this.cesLength = this.dataBuilder.getCEs(nfdExtension, this.ces, this.cesLength);
            if (this.cesLength > 31) {
                throw new IllegalArgumentException("extension string adds too many collation elements (more than 31 total)");
            }
        }
        int ce32 = -1;
        if ((!nfdPrefix.contentEquals(prefix) || !nfdString.contentEquals(str)) && !ignorePrefix(prefix) && !ignoreString(str)) {
            ce32 = addIfDifferent(prefix, str, this.ces, this.cesLength, -1);
        }
        addWithClosure(nfdPrefix, nfdString, this.ces, this.cesLength, ce32);
        this.cesLength = cesLengthBeforeExtension;
    }

    private int findOrInsertNodeForCEs(int strength) {
        long ce;
        if (!f33assertionsDisabled) {
            if (!(strength >= 0 && strength <= 3)) {
                throw new AssertionError();
            }
        }
        while (true) {
            if (this.cesLength == 0) {
                this.ces[0] = 0;
                ce = 0;
                this.cesLength = 1;
                break;
            }
            ce = this.ces[this.cesLength - 1];
            if (ceStrength(ce) <= strength) {
                break;
            }
            this.cesLength--;
        }
        if (isTempCE(ce)) {
            return indexFromTempCE(ce);
        }
        if (((int) (ce >>> 56)) == 254) {
            throw new UnsupportedOperationException("tailoring relative to an unassigned code point not supported");
        }
        return findOrInsertNodeForRootCE(ce, strength);
    }

    private int findOrInsertNodeForRootCE(long ce, int strength) {
        if (!f33assertionsDisabled) {
            if (!(((int) (ce >>> 56)) != 254)) {
                throw new AssertionError();
            }
        }
        if (!f33assertionsDisabled) {
            if (!((192 & ce) == 0)) {
                throw new AssertionError();
            }
        }
        int index = findOrInsertNodeForPrimary(ce >>> 32);
        if (strength >= 1) {
            int lower32 = (int) ce;
            int index2 = findOrInsertWeakNode(index, lower32 >>> 16, 1);
            if (strength >= 2) {
                return findOrInsertWeakNode(index2, lower32 & Collation.ONLY_TERTIARY_MASK, 2);
            }
            return index2;
        }
        return index;
    }

    private static final int binarySearchForRootPrimaryNode(int[] rootPrimaryIndexes, int length, long[] nodes, long p) {
        if (length == 0) {
            return -1;
        }
        int start = 0;
        int limit = length;
        while (true) {
            int i = (start + limit) / 2;
            long node = nodes[rootPrimaryIndexes[i]];
            long nodePrimary = node >>> 32;
            if (p == nodePrimary) {
                return i;
            }
            if (p < nodePrimary) {
                if (i == start) {
                    return ~start;
                }
                limit = i;
            } else {
                if (i == start) {
                    return ~(start + 1);
                }
                start = i;
            }
        }
    }

    private int findOrInsertNodeForPrimary(long p) {
        int rootIndex = binarySearchForRootPrimaryNode(this.rootPrimaryIndexes.getBuffer(), this.rootPrimaryIndexes.size(), this.nodes.getBuffer(), p);
        if (rootIndex >= 0) {
            return this.rootPrimaryIndexes.elementAti(rootIndex);
        }
        int index = this.nodes.size();
        this.nodes.addElement(nodeFromWeight32(p));
        this.rootPrimaryIndexes.insertElementAt(index, ~rootIndex);
        return index;
    }

    private int findOrInsertWeakNode(int index, int weight16, int level) {
        int nextIndex;
        if (!f33assertionsDisabled) {
            if (!(index >= 0 && index < this.nodes.size())) {
                throw new AssertionError();
            }
        }
        if (!f33assertionsDisabled) {
            if (!(1 <= level && level <= 2)) {
                throw new AssertionError();
            }
        }
        if (weight16 == 1280) {
            return findCommonNode(index, level);
        }
        long node = this.nodes.elementAti(index);
        if (!f33assertionsDisabled) {
            if (!(strengthFromNode(node) < level)) {
                throw new AssertionError();
            }
        }
        if (weight16 != 0 && weight16 < 1280) {
            int hasThisLevelBefore = level == 1 ? 64 : 32;
            if ((((long) hasThisLevelBefore) & node) == 0) {
                long commonNode = nodeFromWeight16(Collation.COMMON_WEIGHT16) | nodeFromStrength(level);
                if (level == 1) {
                    commonNode |= 32 & node;
                    node &= -33;
                }
                this.nodes.setElementAt(((long) hasThisLevelBefore) | node, index);
                int nextIndex2 = nextIndexFromNode(node);
                int index2 = insertNodeBetween(index, nextIndex2, nodeFromWeight16(weight16) | nodeFromStrength(level));
                insertNodeBetween(index2, nextIndex2, commonNode);
                return index2;
            }
        }
        while (true) {
            nextIndex = nextIndexFromNode(node);
            if (nextIndex == 0) {
                break;
            }
            node = this.nodes.elementAti(nextIndex);
            int nextStrength = strengthFromNode(node);
            if (nextStrength <= level) {
                if (nextStrength < level) {
                    break;
                }
                if (isTailoredNode(node)) {
                    continue;
                } else {
                    int nextWeight16 = weight16FromNode(node);
                    if (nextWeight16 == weight16) {
                        return nextIndex;
                    }
                    if (nextWeight16 > weight16) {
                        break;
                    }
                }
            }
            index = nextIndex;
        }
        return insertNodeBetween(index, nextIndex, nodeFromWeight16(weight16) | nodeFromStrength(level));
    }

    private int insertTailoredNodeAfter(int index, int strength) {
        int nextIndex;
        boolean z = false;
        if (!f33assertionsDisabled) {
            if (index >= 0 && index < this.nodes.size()) {
                z = true;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        if (strength >= 1) {
            index = findCommonNode(index, 1);
            if (strength >= 2) {
                index = findCommonNode(index, 2);
            }
        }
        long node = this.nodes.elementAti(index);
        while (true) {
            nextIndex = nextIndexFromNode(node);
            if (nextIndex == 0) {
                break;
            }
            node = this.nodes.elementAti(nextIndex);
            if (strengthFromNode(node) <= strength) {
                break;
            }
            index = nextIndex;
        }
        long node2 = 8 | nodeFromStrength(strength);
        return insertNodeBetween(index, nextIndex, node2);
    }

    private int insertNodeBetween(int index, int nextIndex, long node) {
        if (!f33assertionsDisabled) {
            if (!(previousIndexFromNode(node) == 0)) {
                throw new AssertionError();
            }
        }
        if (!f33assertionsDisabled) {
            if (!(nextIndexFromNode(node) == 0)) {
                throw new AssertionError();
            }
        }
        if (!f33assertionsDisabled) {
            if (!(nextIndexFromNode(this.nodes.elementAti(index)) == nextIndex)) {
                throw new AssertionError();
            }
        }
        int newIndex = this.nodes.size();
        this.nodes.addElement(node | nodeFromPreviousIndex(index) | nodeFromNextIndex(nextIndex));
        this.nodes.setElementAt(changeNodeNextIndex(this.nodes.elementAti(index), newIndex), index);
        if (nextIndex != 0) {
            this.nodes.setElementAt(changeNodePreviousIndex(this.nodes.elementAti(nextIndex), newIndex), nextIndex);
        }
        return newIndex;
    }

    private int findCommonNode(int index, int strength) {
        if (!f33assertionsDisabled) {
            if (!(1 <= strength && strength <= 2)) {
                throw new AssertionError();
            }
        }
        long node = this.nodes.elementAti(index);
        if (strengthFromNode(node) >= strength) {
            return index;
        }
        if (strength != 1 ? !nodeHasBefore3(node) : !nodeHasBefore2(node)) {
            return index;
        }
        long node2 = this.nodes.elementAti(nextIndexFromNode(node));
        if (!f33assertionsDisabled) {
            boolean z = !isTailoredNode(node2) && strengthFromNode(node2) == strength && weight16FromNode(node2) < 1280;
            if (!z) {
                throw new AssertionError();
            }
        }
        while (true) {
            int index2 = nextIndexFromNode(node2);
            node2 = this.nodes.elementAti(index2);
            if (!f33assertionsDisabled) {
                if (!(strengthFromNode(node2) >= strength)) {
                    throw new AssertionError();
                }
            }
            if (!isTailoredNode(node2) && strengthFromNode(node2) <= strength && weight16FromNode(node2) >= 1280) {
                if (!f33assertionsDisabled) {
                    if (!(weight16FromNode(node2) == 1280)) {
                        throw new AssertionError();
                    }
                }
                return index2;
            }
        }
    }

    private void setCaseBits(CharSequence nfdString) {
        int numTailoredPrimaries = 0;
        for (int i = 0; i < this.cesLength; i++) {
            if (ceStrength(this.ces[i]) == 0) {
                numTailoredPrimaries++;
            }
        }
        if (!f33assertionsDisabled) {
            if (!(numTailoredPrimaries <= 31)) {
                throw new AssertionError();
            }
        }
        long cases = 0;
        if (numTailoredPrimaries > 0) {
            UTF16CollationIterator baseCEs = new UTF16CollationIterator(this.baseData, false, nfdString, 0);
            int baseCEsLength = baseCEs.fetchCEs() - 1;
            if (!f33assertionsDisabled) {
                if (!(baseCEsLength >= 0 && baseCEs.getCE(baseCEsLength) == Collation.NO_CE)) {
                    throw new AssertionError();
                }
            }
            int lastCase = 0;
            int numBasePrimaries = 0;
            int i2 = 0;
            while (true) {
                if (i2 >= baseCEsLength) {
                    break;
                }
                long ce = baseCEs.getCE(i2);
                if ((ce >>> 32) != 0) {
                    numBasePrimaries++;
                    int c = (((int) ce) >> 14) & 3;
                    if (!f33assertionsDisabled) {
                        if (!(c == 0 || c == 2)) {
                            throw new AssertionError();
                        }
                    }
                    if (numBasePrimaries < numTailoredPrimaries) {
                        cases |= ((long) c) << ((numBasePrimaries - 1) * 2);
                    } else if (numBasePrimaries == numTailoredPrimaries) {
                        lastCase = c;
                    } else if (c != lastCase) {
                        lastCase = 1;
                        break;
                    }
                }
                i2++;
            }
            if (numBasePrimaries >= numTailoredPrimaries) {
                cases |= ((long) lastCase) << ((numTailoredPrimaries - 1) * 2);
            }
        }
        for (int i3 = 0; i3 < this.cesLength; i3++) {
            long ce2 = this.ces[i3] & (-49153);
            int strength = ceStrength(ce2);
            if (strength == 0) {
                ce2 |= (3 & cases) << 14;
                cases >>>= 2;
            } else if (strength == 2) {
                ce2 |= 32768;
            }
            this.ces[i3] = ce2;
        }
    }

    @Override
    void suppressContractions(UnicodeSet set) {
        this.dataBuilder.suppressContractions(set);
    }

    @Override
    void optimize(UnicodeSet set) {
        this.optimizeSet.addAll(set);
    }

    private int addWithClosure(CharSequence nfdPrefix, CharSequence nfdString, long[] newCEs, int newCEsLength, int ce32) {
        int ce322 = addOnlyClosure(nfdPrefix, nfdString, newCEs, newCEsLength, addIfDifferent(nfdPrefix, nfdString, newCEs, newCEsLength, ce32));
        addTailComposites(nfdPrefix, nfdString);
        return ce322;
    }

    private int addOnlyClosure(CharSequence nfdPrefix, CharSequence nfdString, long[] newCEs, int newCEsLength, int ce32) {
        if (nfdPrefix.length() == 0) {
            CanonicalIterator stringIter = new CanonicalIterator(nfdString.toString());
            while (true) {
                String str = stringIter.next();
                if (str == null) {
                    break;
                }
                if (!ignoreString(str) && !str.contentEquals(nfdString)) {
                    ce32 = addIfDifferent("", str, newCEs, newCEsLength, ce32);
                }
            }
        } else {
            CanonicalIterator prefixIter = new CanonicalIterator(nfdPrefix.toString());
            CanonicalIterator stringIter2 = new CanonicalIterator(nfdString.toString());
            while (true) {
                String prefix = prefixIter.next();
                if (prefix == null) {
                    break;
                }
                if (!ignorePrefix(prefix)) {
                    boolean samePrefix = prefix.contentEquals(nfdPrefix);
                    while (true) {
                        String str2 = stringIter2.next();
                        if (str2 == null) {
                            break;
                        }
                        if (!ignoreString(str2) && (!samePrefix || !str2.contentEquals(nfdString))) {
                            ce32 = addIfDifferent(prefix, str2, newCEs, newCEsLength, ce32);
                        }
                    }
                    stringIter2.reset();
                }
            }
        }
        return ce32;
    }

    private void addTailComposites(CharSequence nfdPrefix, CharSequence nfdString) {
        int newCEsLength;
        int ce32;
        int indexAfterLastStarter = nfdString.length();
        while (indexAfterLastStarter != 0) {
            int lastStarter = Character.codePointBefore(nfdString, indexAfterLastStarter);
            if (this.nfd.getCombiningClass(lastStarter) != 0) {
                indexAfterLastStarter -= Character.charCount(lastStarter);
            } else {
                if (Normalizer2Impl.Hangul.isJamoL(lastStarter)) {
                    return;
                }
                UnicodeSet composites = new UnicodeSet();
                if (this.nfcImpl.getCanonStartSet(lastStarter, composites)) {
                    StringBuilder newNFDString = new StringBuilder();
                    StringBuilder newString = new StringBuilder();
                    long[] newCEs = new long[31];
                    UnicodeSetIterator iter = new UnicodeSetIterator(composites);
                    while (iter.next()) {
                        if (!f33assertionsDisabled) {
                            if (!(iter.codepoint != UnicodeSetIterator.IS_STRING)) {
                                throw new AssertionError();
                            }
                        }
                        int composite = iter.codepoint;
                        String decomp = this.nfd.getDecomposition(composite);
                        if (mergeCompositeIntoString(nfdString, indexAfterLastStarter, composite, decomp, newNFDString, newString) && (newCEsLength = this.dataBuilder.getCEs(nfdPrefix, newNFDString, newCEs, 0)) <= 31 && (ce32 = addIfDifferent(nfdPrefix, newString, newCEs, newCEsLength, -1)) != -1) {
                            addOnlyClosure(nfdPrefix, newNFDString, newCEs, newCEsLength, ce32);
                        }
                    }
                    return;
                }
                return;
            }
        }
    }

    private boolean mergeCompositeIntoString(CharSequence nfdString, int indexAfterLastStarter, int composite, CharSequence decomp, StringBuilder newNFDString, StringBuilder newString) {
        if (!f33assertionsDisabled) {
            if (!(Character.codePointBefore(nfdString, indexAfterLastStarter) == Character.codePointAt(decomp, 0))) {
                throw new AssertionError();
            }
        }
        int lastStarterLength = Character.offsetByCodePoints(decomp, 0, 1);
        if (lastStarterLength == decomp.length() || equalSubSequences(nfdString, indexAfterLastStarter, decomp, lastStarterLength)) {
            return false;
        }
        newNFDString.setLength(0);
        newNFDString.append(nfdString, 0, indexAfterLastStarter);
        newString.setLength(0);
        newString.append(nfdString, 0, indexAfterLastStarter - lastStarterLength).appendCodePoint(composite);
        int sourceIndex = indexAfterLastStarter;
        int decompIndex = lastStarterLength;
        int sourceChar = -1;
        int sourceCC = 0;
        int decompCC = 0;
        while (true) {
            if (sourceChar < 0) {
                if (sourceIndex >= nfdString.length()) {
                    break;
                }
                sourceChar = Character.codePointAt(nfdString, sourceIndex);
                sourceCC = this.nfd.getCombiningClass(sourceChar);
                if (!f33assertionsDisabled) {
                    if (!(sourceCC != 0)) {
                        throw new AssertionError();
                    }
                }
                if (decompIndex < decomp.length()) {
                }
            } else {
                if (decompIndex < decomp.length()) {
                    break;
                }
                int decompChar = Character.codePointAt(decomp, decompIndex);
                decompCC = this.nfd.getCombiningClass(decompChar);
                if (decompCC == 0 || sourceCC < decompCC) {
                    return false;
                }
                if (decompCC < sourceCC) {
                    newNFDString.appendCodePoint(decompChar);
                    decompIndex += Character.charCount(decompChar);
                } else {
                    if (decompChar != sourceChar) {
                        return false;
                    }
                    newNFDString.appendCodePoint(decompChar);
                    decompIndex += Character.charCount(decompChar);
                    sourceIndex += Character.charCount(decompChar);
                    sourceChar = -1;
                }
            }
        }
    }

    private boolean equalSubSequences(CharSequence left, int leftStart, CharSequence right, int rightStart) {
        int rightStart2;
        int leftStart2;
        int leftLength = left.length();
        if (leftLength - leftStart != right.length() - rightStart) {
            return false;
        }
        do {
            rightStart2 = rightStart;
            leftStart2 = leftStart;
            if (leftStart2 < leftLength) {
                leftStart = leftStart2 + 1;
                rightStart = rightStart2 + 1;
            } else {
                return true;
            }
        } while (left.charAt(leftStart2) == right.charAt(rightStart2));
        return false;
    }

    private boolean ignorePrefix(CharSequence s) {
        return !isFCD(s);
    }

    private boolean ignoreString(CharSequence s) {
        if (isFCD(s)) {
            return Normalizer2Impl.Hangul.isHangul(s.charAt(0));
        }
        return true;
    }

    private boolean isFCD(CharSequence s) {
        return this.fcd.isNormalized(s);
    }

    static {
        f33assertionsDisabled = !CollationBuilder.class.desiredAssertionStatus();
        COMPOSITES = new UnicodeSet("[:NFD_QC=N:]");
        COMPOSITES.remove(Normalizer2Impl.Hangul.HANGUL_BASE, Normalizer2Impl.Hangul.HANGUL_END);
    }

    private void closeOverComposites() {
        UnicodeSetIterator iter = new UnicodeSetIterator(COMPOSITES);
        while (iter.next()) {
            if (!f33assertionsDisabled) {
                if (!(iter.codepoint != UnicodeSetIterator.IS_STRING)) {
                    throw new AssertionError();
                }
            }
            String nfdString = this.nfd.getDecomposition(iter.codepoint);
            this.cesLength = this.dataBuilder.getCEs(nfdString, this.ces, 0);
            if (this.cesLength <= 31) {
                String composite = iter.getString();
                addIfDifferent("", composite, this.ces, this.cesLength, -1);
            }
        }
    }

    private int addIfDifferent(CharSequence prefix, CharSequence str, long[] newCEs, int newCEsLength, int ce32) {
        long[] oldCEs = new long[31];
        int oldCEsLength = this.dataBuilder.getCEs(prefix, str, oldCEs, 0);
        if (!sameCEs(newCEs, newCEsLength, oldCEs, oldCEsLength)) {
            if (ce32 == -1) {
                ce32 = this.dataBuilder.encodeCEs(newCEs, newCEsLength);
            }
            this.dataBuilder.addCE32(prefix, str, ce32);
        }
        return ce32;
    }

    private static boolean sameCEs(long[] ces1, int ces1Length, long[] ces2, int ces2Length) {
        if (ces1Length != ces2Length) {
            return false;
        }
        if (!f33assertionsDisabled) {
            if (!(ces1Length <= 31)) {
                throw new AssertionError();
            }
        }
        for (int i = 0; i < ces1Length; i++) {
            if (ces1[i] != ces2[i]) {
                return false;
            }
        }
        return true;
    }

    private static final int alignWeightRight(int w) {
        if (w != 0) {
            while ((w & 255) == 0) {
                w >>>= 8;
            }
        }
        return w;
    }

    private void makeTailoredCEs() {
        int sLimit;
        int tLimit;
        CollationWeights primaries = new CollationWeights();
        CollationWeights secondaries = new CollationWeights();
        CollationWeights tertiaries = new CollationWeights();
        long[] nodesArray = this.nodes.getBuffer();
        for (int rpi = 0; rpi < this.rootPrimaryIndexes.size(); rpi++) {
            int i = this.rootPrimaryIndexes.elementAti(rpi);
            long node = nodesArray[i];
            long p = weight32FromNode(node);
            int s = p == 0 ? 0 : Collation.COMMON_WEIGHT16;
            int t = s;
            int q = 0;
            boolean pIsTailored = false;
            boolean sIsTailored = false;
            boolean tIsTailored = false;
            int pIndex = p == 0 ? 0 : this.rootElements.findPrimary(p);
            int nextIndex = nextIndexFromNode(node);
            while (nextIndex != 0) {
                int i2 = nextIndex;
                long node2 = nodesArray[nextIndex];
                nextIndex = nextIndexFromNode(node2);
                int strength = strengthFromNode(node2);
                if (strength == 3) {
                    if (!f33assertionsDisabled && !isTailoredNode(node2)) {
                        throw new AssertionError();
                    }
                    if (q == 3) {
                        throw new UnsupportedOperationException("quaternary tailoring gap too small");
                    }
                    q++;
                } else {
                    if (strength == 2) {
                        if (isTailoredNode(node2)) {
                            if (!tIsTailored) {
                                int tCount = countTailoredNodes(nodesArray, nextIndex, 2) + 1;
                                if (t == 0) {
                                    t = this.rootElements.getTertiaryBoundary() - 256;
                                    tLimit = ((int) this.rootElements.getFirstTertiaryCE()) & Collation.ONLY_TERTIARY_MASK;
                                } else if (!pIsTailored && !sIsTailored) {
                                    tLimit = this.rootElements.getTertiaryAfter(pIndex, s, t);
                                } else if (t == 256) {
                                    tLimit = Collation.COMMON_WEIGHT16;
                                } else {
                                    if (!f33assertionsDisabled) {
                                        if (!(t == 1280)) {
                                            throw new AssertionError();
                                        }
                                    }
                                    tLimit = this.rootElements.getTertiaryBoundary();
                                }
                                if (!f33assertionsDisabled) {
                                    if (!(tLimit == 16384 || (tLimit & (-16192)) == 0)) {
                                        throw new AssertionError();
                                    }
                                }
                                tertiaries.initForTertiary();
                                if (!tertiaries.allocWeights(t, tLimit, tCount)) {
                                    throw new UnsupportedOperationException("tertiary tailoring gap too small");
                                }
                                tIsTailored = true;
                            }
                            t = (int) tertiaries.nextWeight();
                            if (!f33assertionsDisabled) {
                                if (!(t != -1)) {
                                    throw new AssertionError();
                                }
                            }
                        } else {
                            t = weight16FromNode(node2);
                            tIsTailored = false;
                        }
                    } else {
                        if (strength == 1) {
                            if (isTailoredNode(node2)) {
                                if (!sIsTailored) {
                                    int sCount = countTailoredNodes(nodesArray, nextIndex, 1) + 1;
                                    if (s == 0) {
                                        s = this.rootElements.getSecondaryBoundary() - 256;
                                        sLimit = (int) (this.rootElements.getFirstSecondaryCE() >> 16);
                                    } else if (!pIsTailored) {
                                        sLimit = this.rootElements.getSecondaryAfter(pIndex, s);
                                    } else if (s == 256) {
                                        sLimit = Collation.COMMON_WEIGHT16;
                                    } else {
                                        if (!f33assertionsDisabled) {
                                            if (!(s == 1280)) {
                                                throw new AssertionError();
                                            }
                                        }
                                        sLimit = this.rootElements.getSecondaryBoundary();
                                    }
                                    if (s == 1280) {
                                        s = this.rootElements.getLastCommonSecondary();
                                    }
                                    secondaries.initForSecondary();
                                    if (!secondaries.allocWeights(s, sLimit, sCount)) {
                                        throw new UnsupportedOperationException("secondary tailoring gap too small");
                                    }
                                    sIsTailored = true;
                                }
                                s = (int) secondaries.nextWeight();
                                if (!f33assertionsDisabled) {
                                    if (!(s != -1)) {
                                        throw new AssertionError();
                                    }
                                }
                            } else {
                                s = weight16FromNode(node2);
                                sIsTailored = false;
                            }
                        } else {
                            if (!f33assertionsDisabled && !isTailoredNode(node2)) {
                                throw new AssertionError();
                            }
                            if (!pIsTailored) {
                                int pCount = countTailoredNodes(nodesArray, nextIndex, 0) + 1;
                                boolean isCompressible = this.baseData.isCompressiblePrimary(p);
                                long pLimit = this.rootElements.getPrimaryAfter(p, pIndex, isCompressible);
                                primaries.initForPrimary(isCompressible);
                                if (!primaries.allocWeights(p, pLimit, pCount)) {
                                    throw new UnsupportedOperationException("primary tailoring gap too small");
                                }
                                pIsTailored = true;
                            }
                            p = primaries.nextWeight();
                            if (!f33assertionsDisabled) {
                                if (!(p != 4294967295L)) {
                                    throw new AssertionError();
                                }
                            }
                            s = Collation.COMMON_WEIGHT16;
                            sIsTailored = false;
                        }
                        t = s == 0 ? 0 : Collation.COMMON_WEIGHT16;
                        tIsTailored = false;
                    }
                    q = 0;
                }
                if (isTailoredNode(node2)) {
                    nodesArray[i2] = Collation.makeCE(p, s, t, q);
                }
            }
        }
    }

    private static int countTailoredNodes(long[] nodesArray, int i, int strength) {
        int count = 0;
        while (i != 0) {
            long node = nodesArray[i];
            if (strengthFromNode(node) < strength) {
                break;
            }
            if (strengthFromNode(node) == strength) {
                if (!isTailoredNode(node)) {
                    break;
                }
                count++;
            }
            i = nextIndexFromNode(node);
        }
        return count;
    }

    private static final class CEFinalizer implements CollationDataBuilder.CEModifier {

        static final boolean f34assertionsDisabled;
        private long[] finalCEs;

        static {
            f34assertionsDisabled = !CEFinalizer.class.desiredAssertionStatus();
        }

        CEFinalizer(long[] ces) {
            this.finalCEs = ces;
        }

        @Override
        public long modifyCE32(int ce32) {
            if (!f34assertionsDisabled) {
                if (!(!Collation.isSpecialCE32(ce32))) {
                    throw new AssertionError();
                }
            }
            if (CollationBuilder.isTempCE32(ce32)) {
                return this.finalCEs[CollationBuilder.indexFromTempCE32(ce32)] | ((long) ((ce32 & 192) << 8));
            }
            return Collation.NO_CE;
        }

        @Override
        public long modifyCE(long ce) {
            if (CollationBuilder.isTempCE(ce)) {
                return this.finalCEs[CollationBuilder.indexFromTempCE(ce)] | (49152 & ce);
            }
            return Collation.NO_CE;
        }
    }

    private void finalizeCEs() {
        CollationDataBuilder newBuilder = new CollationDataBuilder();
        newBuilder.initForTailoring(this.baseData);
        CEFinalizer finalizer = new CEFinalizer(this.nodes.getBuffer());
        newBuilder.copyFrom(this.dataBuilder, finalizer);
        this.dataBuilder = newBuilder;
    }

    private static long tempCEFromIndexAndStrength(int index, int strength) {
        return (((long) (1040384 & index)) << 43) + 4629700417037541376L + (((long) (index & 8128)) << 42) + ((long) ((index & 63) << 24)) + ((long) (strength << 8));
    }

    private static int indexFromTempCE(long tempCE) {
        long tempCE2 = tempCE - 4629700417037541376L;
        return (((int) (tempCE2 >> 43)) & 1040384) | (((int) (tempCE2 >> 42)) & 8128) | (((int) (tempCE2 >> 24)) & 63);
    }

    private static int strengthFromTempCE(long tempCE) {
        return (((int) tempCE) >> 8) & 3;
    }

    private static boolean isTempCE(long ce) {
        int sec = ((int) ce) >>> 24;
        return 6 <= sec && sec <= 69;
    }

    private static int indexFromTempCE32(int tempCE32) {
        int tempCE322 = tempCE32 - 1077937696;
        return ((tempCE322 >> 11) & 1040384) | ((tempCE322 >> 10) & 8128) | ((tempCE322 >> 8) & 63);
    }

    private static boolean isTempCE32(int ce32) {
        return (ce32 & 255) >= 2 && 6 <= ((ce32 >> 8) & 255) && ((ce32 >> 8) & 255) <= 69;
    }

    private static int ceStrength(long ce) {
        if (isTempCE(ce)) {
            return strengthFromTempCE(ce);
        }
        if (((-72057594037927936L) & ce) != 0) {
            return 0;
        }
        if ((((int) ce) & (-16777216)) != 0) {
            return 1;
        }
        return ce != 0 ? 2 : 15;
    }

    private static long nodeFromWeight32(long weight32) {
        return weight32 << 32;
    }

    private static long nodeFromWeight16(int weight16) {
        return ((long) weight16) << 48;
    }

    private static long nodeFromPreviousIndex(int previous) {
        return ((long) previous) << 28;
    }

    private static long nodeFromNextIndex(int next) {
        return next << 8;
    }

    private static long nodeFromStrength(int strength) {
        return strength;
    }

    private static long weight32FromNode(long node) {
        return node >>> 32;
    }

    private static int weight16FromNode(long node) {
        return ((int) (node >> 48)) & 65535;
    }

    private static int previousIndexFromNode(long node) {
        return ((int) (node >> 28)) & MAX_INDEX;
    }

    private static int nextIndexFromNode(long node) {
        return (((int) node) >> 8) & MAX_INDEX;
    }

    private static int strengthFromNode(long node) {
        return ((int) node) & 3;
    }

    private static boolean nodeHasBefore2(long node) {
        return (64 & node) != 0;
    }

    private static boolean nodeHasBefore3(long node) {
        return (32 & node) != 0;
    }

    private static boolean nodeHasAnyBefore(long node) {
        return (96 & node) != 0;
    }

    private static boolean isTailoredNode(long node) {
        return (8 & node) != 0;
    }

    private static long changeNodePreviousIndex(long node, int previous) {
        return ((-281474708275201L) & node) | nodeFromPreviousIndex(previous);
    }

    private static long changeNodeNextIndex(long node, int next) {
        return ((-268435201) & node) | nodeFromNextIndex(next);
    }
}
