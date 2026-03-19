package android.icu.impl.coll;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.coll.CollationDataBuilder;
import android.icu.impl.coll.CollationRuleParser;
import android.icu.text.CanonicalIterator;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.Normalizer2;
import android.icu.text.UnicodeSet;
import android.icu.text.UnicodeSetIterator;
import android.icu.util.ULocale;
import java.text.ParseException;

public final class CollationBuilder extends CollationRuleParser.Sink {
    static final boolean $assertionsDisabled = false;
    private static final UnicodeSet COMPOSITES = new UnicodeSet("[:NFD_QC=N:]");
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

    static {
        COMPOSITES.remove(Normalizer2Impl.Hangul.HANGUL_BASE, Normalizer2Impl.Hangul.HANGUL_END);
    }

    private static final class BundleImporter implements CollationRuleParser.Importer {
        BundleImporter() {
        }

        @Override
        public String getRules(String str, String str2) {
            return CollationLoader.loadRules(new ULocale(str), str2);
        }
    }

    public CollationBuilder(CollationTailoring collationTailoring) {
        this.base = collationTailoring;
        this.baseData = collationTailoring.data;
        this.rootElements = new CollationRootElements(collationTailoring.data.rootElements);
        this.nfcImpl.ensureCanonIterData();
        this.dataBuilder.initForTailoring(this.baseData);
    }

    public CollationTailoring parseAndBuild(String str) throws ParseException {
        if (this.baseData.rootElements == null) {
            throw new UnsupportedOperationException("missing root elements data, tailoring not supported");
        }
        CollationTailoring collationTailoring = new CollationTailoring(this.base.settings);
        CollationRuleParser collationRuleParser = new CollationRuleParser(this.baseData);
        this.variableTop = ((CollationSettings) this.base.settings.readOnly()).variableTop;
        collationRuleParser.setSink(this);
        collationRuleParser.setImporter(new BundleImporter());
        CollationSettings collationSettings = (CollationSettings) collationTailoring.settings.copyOnWrite();
        collationRuleParser.parse(str, collationSettings);
        if (this.dataBuilder.hasMappings()) {
            makeTailoredCEs();
            closeOverComposites();
            finalizeCEs();
            this.optimizeSet.add(0, 127);
            this.optimizeSet.add(192, 255);
            this.optimizeSet.remove(Normalizer2Impl.Hangul.HANGUL_BASE, Normalizer2Impl.Hangul.HANGUL_END);
            this.dataBuilder.optimize(this.optimizeSet);
            collationTailoring.ensureOwnedData();
            if (this.fastLatinEnabled) {
                this.dataBuilder.enableFastLatin();
            }
            this.dataBuilder.build(collationTailoring.ownedData);
            this.dataBuilder = null;
        } else {
            collationTailoring.data = this.baseData;
        }
        collationSettings.fastLatinOptions = CollationFastLatin.getOptions(collationTailoring.data, collationSettings, collationSettings.fastLatinPrimaries);
        collationTailoring.setRules(str);
        collationTailoring.setVersion(this.base.version, 0);
        return collationTailoring;
    }

    @Override
    void addReset(int i, CharSequence charSequence) {
        int iFindOrInsertWeakNode;
        int iWeight16FromNode;
        if (charSequence.charAt(0) == 65534) {
            this.ces[0] = getSpecialResetPosition(charSequence);
            this.cesLength = 1;
        } else {
            this.cesLength = this.dataBuilder.getCEs(this.nfd.normalize(charSequence), this.ces, 0);
            if (this.cesLength > 31) {
                throw new IllegalArgumentException("reset position maps to too many collation elements (more than 31)");
            }
        }
        if (i == 15) {
            return;
        }
        int iFindOrInsertNodeForCEs = findOrInsertNodeForCEs(i);
        long jElementAti = this.nodes.elementAti(iFindOrInsertNodeForCEs);
        while (strengthFromNode(jElementAti) > i) {
            iFindOrInsertNodeForCEs = previousIndexFromNode(jElementAti);
            jElementAti = this.nodes.elementAti(iFindOrInsertNodeForCEs);
        }
        if (strengthFromNode(jElementAti) == i && isTailoredNode(jElementAti)) {
            iFindOrInsertWeakNode = previousIndexFromNode(jElementAti);
        } else if (i == 0) {
            long jWeight32FromNode = weight32FromNode(jElementAti);
            if (jWeight32FromNode == 0) {
                throw new UnsupportedOperationException("reset primary-before ignorable not possible");
            }
            if (jWeight32FromNode <= this.rootElements.getFirstPrimary()) {
                throw new UnsupportedOperationException("reset primary-before first non-ignorable not supported");
            }
            if (jWeight32FromNode == 4278321664L) {
                throw new UnsupportedOperationException("reset primary-before [first trailing] not supported");
            }
            iFindOrInsertWeakNode = findOrInsertNodeForPrimary(this.rootElements.getPrimaryBefore(jWeight32FromNode, this.baseData.isCompressiblePrimary(jWeight32FromNode)));
            while (true) {
                int iNextIndexFromNode = nextIndexFromNode(this.nodes.elementAti(iFindOrInsertWeakNode));
                if (iNextIndexFromNode == 0) {
                    break;
                } else {
                    iFindOrInsertWeakNode = iNextIndexFromNode;
                }
            }
        } else {
            int iFindCommonNode = findCommonNode(iFindOrInsertNodeForCEs, 1);
            if (i >= 2) {
                iFindCommonNode = findCommonNode(iFindCommonNode, 2);
            }
            long jElementAti2 = this.nodes.elementAti(iFindCommonNode);
            if (strengthFromNode(jElementAti2) == i) {
                if (weight16FromNode(jElementAti2) == 0) {
                    throw new UnsupportedOperationException(i == 1 ? "reset secondary-before secondary ignorable not possible" : "reset tertiary-before completely ignorable not possible");
                }
                int weight16Before = getWeight16Before(iFindCommonNode, jElementAti2, i);
                int iPreviousIndexFromNode = previousIndexFromNode(jElementAti2);
                int iPreviousIndexFromNode2 = iPreviousIndexFromNode;
                while (true) {
                    long jElementAti3 = this.nodes.elementAti(iPreviousIndexFromNode2);
                    int iStrengthFromNode = strengthFromNode(jElementAti3);
                    if (iStrengthFromNode < i) {
                        iWeight16FromNode = Collation.COMMON_WEIGHT16;
                        break;
                    } else if (iStrengthFromNode != i || isTailoredNode(jElementAti3)) {
                        iPreviousIndexFromNode2 = previousIndexFromNode(jElementAti3);
                    } else {
                        iWeight16FromNode = weight16FromNode(jElementAti3);
                        break;
                    }
                }
                if (iWeight16FromNode != weight16Before) {
                    iPreviousIndexFromNode = insertNodeBetween(iPreviousIndexFromNode, iFindCommonNode, nodeFromWeight16(weight16Before) | nodeFromStrength(i));
                }
                iFindOrInsertWeakNode = iPreviousIndexFromNode;
            } else {
                iFindOrInsertWeakNode = findOrInsertWeakNode(iFindCommonNode, getWeight16Before(iFindCommonNode, jElementAti2, i), i);
            }
            i = ceStrength(this.ces[this.cesLength - 1]);
        }
        this.ces[this.cesLength - 1] = tempCEFromIndexAndStrength(iFindOrInsertWeakNode, i);
    }

    private int getWeight16Before(int i, long j, int i2) {
        int iWeight16FromNode;
        int iStrengthFromNode = strengthFromNode(j);
        int iWeight16FromNode2 = Collation.COMMON_WEIGHT16;
        if (iStrengthFromNode == 2) {
            iWeight16FromNode = weight16FromNode(j);
        } else {
            iWeight16FromNode = 1280;
        }
        while (strengthFromNode(j) > 1) {
            j = this.nodes.elementAti(previousIndexFromNode(j));
        }
        if (isTailoredNode(j)) {
            return 256;
        }
        if (strengthFromNode(j) == 1) {
            iWeight16FromNode2 = weight16FromNode(j);
        }
        while (strengthFromNode(j) > 0) {
            j = this.nodes.elementAti(previousIndexFromNode(j));
        }
        if (isTailoredNode(j)) {
            return 256;
        }
        long jWeight32FromNode = weight32FromNode(j);
        if (i2 == 1) {
            return this.rootElements.getSecondaryBefore(jWeight32FromNode, iWeight16FromNode2);
        }
        return this.rootElements.getTertiaryBefore(jWeight32FromNode, iWeight16FromNode2, iWeight16FromNode);
    }

    private long getSpecialResetPosition(CharSequence charSequence) {
        long lastTertiaryCE;
        int i;
        int iStrengthFromNode;
        boolean z;
        CollationRuleParser.Position position = CollationRuleParser.POSITION_VALUES[charSequence.charAt(1) - 10240];
        switch (position) {
            case FIRST_TERTIARY_IGNORABLE:
                return 0L;
            case LAST_TERTIARY_IGNORABLE:
                return 0L;
            case FIRST_SECONDARY_IGNORABLE:
                int iNextIndexFromNode = nextIndexFromNode(this.nodes.elementAti(findOrInsertNodeForRootCE(0L, 2)));
                if (iNextIndexFromNode != 0) {
                    long jElementAti = this.nodes.elementAti(iNextIndexFromNode);
                    if (isTailoredNode(jElementAti) && strengthFromNode(jElementAti) == 2) {
                        return tempCEFromIndexAndStrength(iNextIndexFromNode, 2);
                    }
                }
                return this.rootElements.getFirstTertiaryCE();
            case LAST_SECONDARY_IGNORABLE:
                lastTertiaryCE = this.rootElements.getLastTertiaryCE();
                i = 2;
                z = false;
                int iFindOrInsertNodeForRootCE = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti2 = this.nodes.elementAti(iFindOrInsertNodeForRootCE);
                if ((position.ordinal() & 1) != 0) {
                    while (true) {
                        int iNextIndexFromNode2 = nextIndexFromNode(jElementAti2);
                        if (iNextIndexFromNode2 != 0) {
                            long jElementAti3 = this.nodes.elementAti(iNextIndexFromNode2);
                            if (strengthFromNode(jElementAti3) >= i) {
                                iFindOrInsertNodeForRootCE = iNextIndexFromNode2;
                                jElementAti2 = jElementAti3;
                            }
                        }
                    }
                    return isTailoredNode(jElementAti2) ? tempCEFromIndexAndStrength(iFindOrInsertNodeForRootCE, i) : lastTertiaryCE;
                }
                if (!nodeHasAnyBefore(jElementAti2) && z) {
                    iFindOrInsertNodeForRootCE = nextIndexFromNode(jElementAti2);
                    if (iFindOrInsertNodeForRootCE != 0) {
                        jElementAti2 = this.nodes.elementAti(iFindOrInsertNodeForRootCE);
                        lastTertiaryCE = tempCEFromIndexAndStrength(iFindOrInsertNodeForRootCE, i);
                    } else {
                        long j = lastTertiaryCE >>> 32;
                        lastTertiaryCE = Collation.makeCE(this.rootElements.getPrimaryAfter(j, this.rootElements.findPrimary(j), this.baseData.isCompressiblePrimary(j)));
                        iFindOrInsertNodeForRootCE = findOrInsertNodeForRootCE(lastTertiaryCE, 0);
                        jElementAti2 = this.nodes.elementAti(iFindOrInsertNodeForRootCE);
                    }
                }
                if (!nodeHasAnyBefore(jElementAti2)) {
                    return lastTertiaryCE;
                }
                if (nodeHasBefore2(jElementAti2)) {
                    iFindOrInsertNodeForRootCE = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(jElementAti2)));
                    jElementAti2 = this.nodes.elementAti(iFindOrInsertNodeForRootCE);
                }
                if (nodeHasBefore3(jElementAti2)) {
                    iFindOrInsertNodeForRootCE = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(jElementAti2)));
                }
                return tempCEFromIndexAndStrength(iFindOrInsertNodeForRootCE, i);
            case FIRST_PRIMARY_IGNORABLE:
                long jElementAti4 = this.nodes.elementAti(findOrInsertNodeForRootCE(0L, 1));
                while (true) {
                    int iNextIndexFromNode3 = nextIndexFromNode(jElementAti4);
                    if (iNextIndexFromNode3 != 0 && (iStrengthFromNode = strengthFromNode((jElementAti4 = this.nodes.elementAti(iNextIndexFromNode3)))) >= 1) {
                        if (iStrengthFromNode == 1) {
                            if (isTailoredNode(jElementAti4)) {
                                if (nodeHasBefore3(jElementAti4)) {
                                    iNextIndexFromNode3 = nextIndexFromNode(this.nodes.elementAti(nextIndexFromNode(jElementAti4)));
                                }
                                return tempCEFromIndexAndStrength(iNextIndexFromNode3, 1);
                            }
                        }
                    }
                }
                lastTertiaryCE = this.rootElements.getFirstSecondaryCE();
                i = 1;
                z = false;
                int iFindOrInsertNodeForRootCE2 = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti22 = this.nodes.elementAti(iFindOrInsertNodeForRootCE2);
                if ((position.ordinal() & 1) != 0) {
                }
                break;
            case LAST_PRIMARY_IGNORABLE:
                lastTertiaryCE = this.rootElements.getLastSecondaryCE();
                i = 1;
                z = false;
                int iFindOrInsertNodeForRootCE22 = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti222 = this.nodes.elementAti(iFindOrInsertNodeForRootCE22);
                if ((position.ordinal() & 1) != 0) {
                }
                break;
            case FIRST_VARIABLE:
                lastTertiaryCE = this.rootElements.getFirstPrimaryCE();
                z = true;
                i = 0;
                int iFindOrInsertNodeForRootCE222 = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti2222 = this.nodes.elementAti(iFindOrInsertNodeForRootCE222);
                if ((position.ordinal() & 1) != 0) {
                }
                break;
            case LAST_VARIABLE:
                lastTertiaryCE = this.rootElements.lastCEWithPrimaryBefore(this.variableTop + 1);
                i = 0;
                z = false;
                int iFindOrInsertNodeForRootCE2222 = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti22222 = this.nodes.elementAti(iFindOrInsertNodeForRootCE2222);
                if ((position.ordinal() & 1) != 0) {
                }
                break;
            case FIRST_REGULAR:
                lastTertiaryCE = this.rootElements.firstCEWithPrimaryAtLeast(this.variableTop + 1);
                z = true;
                i = 0;
                int iFindOrInsertNodeForRootCE22222 = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti222222 = this.nodes.elementAti(iFindOrInsertNodeForRootCE22222);
                if ((position.ordinal() & 1) != 0) {
                }
                break;
            case LAST_REGULAR:
                lastTertiaryCE = this.rootElements.firstCEWithPrimaryAtLeast(this.baseData.getFirstPrimaryForGroup(17));
                i = 0;
                z = false;
                int iFindOrInsertNodeForRootCE222222 = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti2222222 = this.nodes.elementAti(iFindOrInsertNodeForRootCE222222);
                if ((position.ordinal() & 1) != 0) {
                }
                break;
            case FIRST_IMPLICIT:
                lastTertiaryCE = this.baseData.getSingleCE(19968);
                i = 0;
                z = false;
                int iFindOrInsertNodeForRootCE2222222 = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti22222222 = this.nodes.elementAti(iFindOrInsertNodeForRootCE2222222);
                if ((position.ordinal() & 1) != 0) {
                }
                break;
            case LAST_IMPLICIT:
                throw new UnsupportedOperationException("reset to [last implicit] not supported");
            case FIRST_TRAILING:
                lastTertiaryCE = Collation.makeCE(4278321664L);
                z = true;
                i = 0;
                int iFindOrInsertNodeForRootCE22222222 = findOrInsertNodeForRootCE(lastTertiaryCE, i);
                long jElementAti222222222 = this.nodes.elementAti(iFindOrInsertNodeForRootCE22222222);
                if ((position.ordinal() & 1) != 0) {
                }
                break;
            case LAST_TRAILING:
                throw new IllegalArgumentException("LDML forbids tailoring to U+FFFF");
            default:
                return 0L;
        }
    }

    @Override
    void addRelation(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        String strNormalize;
        if (charSequence.length() == 0) {
            strNormalize = "";
        } else {
            strNormalize = this.nfd.normalize(charSequence);
        }
        String str = strNormalize;
        String strNormalize2 = this.nfd.normalize(charSequence2);
        int length = strNormalize2.length();
        if (length >= 2) {
            char cCharAt = strNormalize2.charAt(0);
            if (Normalizer2Impl.Hangul.isJamoL(cCharAt) || Normalizer2Impl.Hangul.isJamoV(cCharAt)) {
                throw new UnsupportedOperationException("contractions starting with conjoining Jamo L or V not supported");
            }
            char cCharAt2 = strNormalize2.charAt(length - 1);
            if (Normalizer2Impl.Hangul.isJamoL(cCharAt2) || (Normalizer2Impl.Hangul.isJamoV(cCharAt2) && Normalizer2Impl.Hangul.isJamoL(strNormalize2.charAt(length - 2)))) {
                throw new UnsupportedOperationException("contractions ending with conjoining Jamo L or L+V not supported");
            }
        }
        if (i != 15) {
            int iFindOrInsertNodeForCEs = findOrInsertNodeForCEs(i);
            long j = this.ces[this.cesLength - 1];
            if (i == 0 && !isTempCE(j) && (j >>> 32) == 0) {
                throw new UnsupportedOperationException("tailoring primary after ignorables not supported");
            }
            if (i == 3 && j == 0) {
                throw new UnsupportedOperationException("tailoring quaternary after tertiary ignorables not supported");
            }
            int iInsertTailoredNodeAfter = insertTailoredNodeAfter(iFindOrInsertNodeForCEs, i);
            int iCeStrength = ceStrength(j);
            if (i >= iCeStrength) {
                i = iCeStrength;
            }
            this.ces[this.cesLength - 1] = tempCEFromIndexAndStrength(iInsertTailoredNodeAfter, i);
        }
        setCaseBits(strNormalize2);
        int i2 = this.cesLength;
        if (charSequence3.length() != 0) {
            this.cesLength = this.dataBuilder.getCEs(this.nfd.normalize(charSequence3), this.ces, this.cesLength);
            if (this.cesLength > 31) {
                throw new IllegalArgumentException("extension string adds too many collation elements (more than 31 total)");
            }
        }
        addWithClosure(str, strNormalize2, this.ces, this.cesLength, ((str.contentEquals(charSequence) && strNormalize2.contentEquals(charSequence2)) || ignorePrefix(charSequence) || ignoreString(charSequence2)) ? -1 : addIfDifferent(charSequence, charSequence2, this.ces, this.cesLength, -1));
        this.cesLength = i2;
    }

    private int findOrInsertNodeForCEs(int i) {
        long j;
        while (true) {
            if (this.cesLength == 0) {
                j = 0;
                this.ces[0] = 0;
                this.cesLength = 1;
                break;
            }
            j = this.ces[this.cesLength - 1];
            if (ceStrength(j) <= i) {
                break;
            }
            this.cesLength--;
        }
        if (isTempCE(j)) {
            return indexFromTempCE(j);
        }
        if (((int) (j >>> 56)) == 254) {
            throw new UnsupportedOperationException("tailoring relative to an unassigned code point not supported");
        }
        return findOrInsertNodeForRootCE(j, i);
    }

    private int findOrInsertNodeForRootCE(long j, int i) {
        int iFindOrInsertNodeForPrimary = findOrInsertNodeForPrimary(j >>> 32);
        if (i >= 1) {
            int i2 = (int) j;
            int iFindOrInsertWeakNode = findOrInsertWeakNode(iFindOrInsertNodeForPrimary, i2 >>> 16, 1);
            if (i >= 2) {
                return findOrInsertWeakNode(iFindOrInsertWeakNode, i2 & Collation.ONLY_TERTIARY_MASK, 2);
            }
            return iFindOrInsertWeakNode;
        }
        return iFindOrInsertNodeForPrimary;
    }

    private static final int binarySearchForRootPrimaryNode(int[] iArr, int i, long[] jArr, long j) {
        if (i == 0) {
            return -1;
        }
        int i2 = 0;
        while (true) {
            int i3 = (int) ((((long) i2) + ((long) i)) / 2);
            long j2 = jArr[iArr[i3]] >>> 32;
            if (j == j2) {
                return i3;
            }
            if (j < j2) {
                if (i3 == i2) {
                    return ~i2;
                }
                i = i3;
            } else {
                if (i3 == i2) {
                    return ~(i2 + 1);
                }
                i2 = i3;
            }
        }
    }

    private int findOrInsertNodeForPrimary(long j) {
        int iBinarySearchForRootPrimaryNode = binarySearchForRootPrimaryNode(this.rootPrimaryIndexes.getBuffer(), this.rootPrimaryIndexes.size(), this.nodes.getBuffer(), j);
        if (iBinarySearchForRootPrimaryNode >= 0) {
            return this.rootPrimaryIndexes.elementAti(iBinarySearchForRootPrimaryNode);
        }
        int size = this.nodes.size();
        this.nodes.addElement(nodeFromWeight32(j));
        this.rootPrimaryIndexes.insertElementAt(size, ~iBinarySearchForRootPrimaryNode);
        return size;
    }

    private int findOrInsertWeakNode(int i, int i2, int i3) {
        int iNextIndexFromNode;
        if (i2 == 1280) {
            return findCommonNode(i, i3);
        }
        long jElementAti = this.nodes.elementAti(i);
        if (i2 != 0 && i2 < 1280) {
            long j = i3 == 1 ? 64 : 32;
            if ((jElementAti & j) == 0) {
                long jNodeFromWeight16 = nodeFromWeight16(Collation.COMMON_WEIGHT16) | nodeFromStrength(i3);
                if (i3 == 1) {
                    jNodeFromWeight16 |= 32 & jElementAti;
                    jElementAti &= -33;
                }
                this.nodes.setElementAt(jElementAti | j, i);
                int iNextIndexFromNode2 = nextIndexFromNode(jElementAti);
                int iInsertNodeBetween = insertNodeBetween(i, iNextIndexFromNode2, nodeFromStrength(i3) | nodeFromWeight16(i2));
                insertNodeBetween(iInsertNodeBetween, iNextIndexFromNode2, jNodeFromWeight16);
                return iInsertNodeBetween;
            }
        }
        while (true) {
            iNextIndexFromNode = nextIndexFromNode(jElementAti);
            if (iNextIndexFromNode == 0) {
                break;
            }
            jElementAti = this.nodes.elementAti(iNextIndexFromNode);
            int iStrengthFromNode = strengthFromNode(jElementAti);
            if (iStrengthFromNode <= i3) {
                if (iStrengthFromNode < i3) {
                    break;
                }
                if (isTailoredNode(jElementAti)) {
                    continue;
                } else {
                    int iWeight16FromNode = weight16FromNode(jElementAti);
                    if (iWeight16FromNode == i2) {
                        return iNextIndexFromNode;
                    }
                    if (iWeight16FromNode > i2) {
                        break;
                    }
                }
            }
            i = iNextIndexFromNode;
        }
        return insertNodeBetween(i, iNextIndexFromNode, nodeFromStrength(i3) | nodeFromWeight16(i2));
    }

    private int insertTailoredNodeAfter(int i, int i2) {
        int iNextIndexFromNode;
        if (i2 >= 1) {
            i = findCommonNode(i, 1);
            if (i2 >= 2) {
                i = findCommonNode(i, 2);
            }
        }
        long jElementAti = this.nodes.elementAti(i);
        while (true) {
            iNextIndexFromNode = nextIndexFromNode(jElementAti);
            if (iNextIndexFromNode == 0) {
                break;
            }
            long jElementAti2 = this.nodes.elementAti(iNextIndexFromNode);
            if (strengthFromNode(jElementAti2) <= i2) {
                break;
            }
            i = iNextIndexFromNode;
            jElementAti = jElementAti2;
        }
        return insertNodeBetween(i, iNextIndexFromNode, 8 | nodeFromStrength(i2));
    }

    private int insertNodeBetween(int i, int i2, long j) {
        int size = this.nodes.size();
        this.nodes.addElement(j | nodeFromPreviousIndex(i) | nodeFromNextIndex(i2));
        this.nodes.setElementAt(changeNodeNextIndex(this.nodes.elementAti(i), size), i);
        if (i2 != 0) {
            this.nodes.setElementAt(changeNodePreviousIndex(this.nodes.elementAti(i2), size), i2);
        }
        return size;
    }

    private int findCommonNode(int i, int i2) {
        long jElementAti = this.nodes.elementAti(i);
        if (strengthFromNode(jElementAti) >= i2) {
            return i;
        }
        if (i2 != 1 ? !nodeHasBefore3(jElementAti) : !nodeHasBefore2(jElementAti)) {
            return i;
        }
        long jElementAti2 = this.nodes.elementAti(nextIndexFromNode(jElementAti));
        while (true) {
            int iNextIndexFromNode = nextIndexFromNode(jElementAti2);
            jElementAti2 = this.nodes.elementAti(iNextIndexFromNode);
            if (!isTailoredNode(jElementAti2) && strengthFromNode(jElementAti2) <= i2 && weight16FromNode(jElementAti2) >= 1280) {
                return iNextIndexFromNode;
            }
        }
    }

    private void setCaseBits(CharSequence charSequence) {
        int i = 0;
        for (int i2 = 0; i2 < this.cesLength; i2++) {
            if (ceStrength(this.ces[i2]) == 0) {
                i++;
            }
        }
        long j = 0;
        if (i > 0) {
            UTF16CollationIterator uTF16CollationIterator = new UTF16CollationIterator(this.baseData, false, charSequence, 0);
            int iFetchCEs = uTF16CollationIterator.fetchCEs() - 1;
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            long j2 = 0;
            while (true) {
                if (i3 >= iFetchCEs) {
                    break;
                }
                long ce = uTF16CollationIterator.getCE(i3);
                if ((ce >>> 32) != 0) {
                    i4++;
                    int i6 = (((int) ce) >> 14) & 3;
                    if (i4 < i) {
                        j2 |= ((long) i6) << ((i4 - 1) * 2);
                    } else if (i4 == i) {
                        i5 = i6;
                    } else if (i6 != i5) {
                        i5 = 1;
                        break;
                    }
                }
                i3++;
            }
            if (i4 >= i) {
                j = j2 | (((long) i5) << ((i - 1) * 2));
            } else {
                j = j2;
            }
        }
        for (int i7 = 0; i7 < this.cesLength; i7++) {
            long j3 = this.ces[i7] & (-49153);
            int iCeStrength = ceStrength(j3);
            if (iCeStrength == 0) {
                j3 |= (3 & j) << 14;
                j >>>= 2;
            } else if (iCeStrength == 2) {
                j3 |= 32768;
            }
            this.ces[i7] = j3;
        }
    }

    @Override
    void suppressContractions(UnicodeSet unicodeSet) {
        this.dataBuilder.suppressContractions(unicodeSet);
    }

    @Override
    void optimize(UnicodeSet unicodeSet) {
        this.optimizeSet.addAll(unicodeSet);
    }

    private int addWithClosure(CharSequence charSequence, CharSequence charSequence2, long[] jArr, int i, int i2) {
        int iAddOnlyClosure = addOnlyClosure(charSequence, charSequence2, jArr, i, addIfDifferent(charSequence, charSequence2, jArr, i, i2));
        addTailComposites(charSequence, charSequence2);
        return iAddOnlyClosure;
    }

    private int addOnlyClosure(CharSequence charSequence, CharSequence charSequence2, long[] jArr, int i, int i2) {
        if (charSequence.length() == 0) {
            CanonicalIterator canonicalIterator = new CanonicalIterator(charSequence2.toString());
            int iAddIfDifferent = i2;
            while (true) {
                String next = canonicalIterator.next();
                if (next != null) {
                    if (!ignoreString(next) && !next.contentEquals(charSequence2)) {
                        iAddIfDifferent = addIfDifferent("", next, jArr, i, iAddIfDifferent);
                    }
                } else {
                    return iAddIfDifferent;
                }
            }
        } else {
            CanonicalIterator canonicalIterator2 = new CanonicalIterator(charSequence.toString());
            CanonicalIterator canonicalIterator3 = new CanonicalIterator(charSequence2.toString());
            while (true) {
                String next2 = canonicalIterator2.next();
                if (next2 != null) {
                    if (!ignorePrefix(next2)) {
                        boolean zContentEquals = next2.contentEquals(charSequence);
                        int iAddIfDifferent2 = i2;
                        while (true) {
                            String next3 = canonicalIterator3.next();
                            if (next3 == null) {
                                break;
                            }
                            if (!ignoreString(next3) && (!zContentEquals || !next3.contentEquals(charSequence2))) {
                                iAddIfDifferent2 = addIfDifferent(next2, next3, jArr, i, iAddIfDifferent2);
                            }
                        }
                        canonicalIterator3.reset();
                        i2 = iAddIfDifferent2;
                    }
                } else {
                    return i2;
                }
            }
        }
    }

    private void addTailComposites(CharSequence charSequence, CharSequence charSequence2) {
        int cEs;
        int iAddIfDifferent;
        int length = charSequence2.length();
        while (length != 0) {
            int iCodePointBefore = Character.codePointBefore(charSequence2, length);
            if (this.nfd.getCombiningClass(iCodePointBefore) == 0) {
                if (Normalizer2Impl.Hangul.isJamoL(iCodePointBefore)) {
                    return;
                }
                UnicodeSet unicodeSet = new UnicodeSet();
                if (this.nfcImpl.getCanonStartSet(iCodePointBefore, unicodeSet)) {
                    StringBuilder sb = new StringBuilder();
                    StringBuilder sb2 = new StringBuilder();
                    long[] jArr = new long[31];
                    UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(unicodeSet);
                    while (unicodeSetIterator.next()) {
                        int i = unicodeSetIterator.codepoint;
                        if (mergeCompositeIntoString(charSequence2, length, i, this.nfd.getDecomposition(i), sb, sb2) && (cEs = this.dataBuilder.getCEs(charSequence, sb, jArr, 0)) <= 31 && (iAddIfDifferent = addIfDifferent(charSequence, sb2, jArr, cEs, -1)) != -1) {
                            addOnlyClosure(charSequence, sb, jArr, cEs, iAddIfDifferent);
                        }
                    }
                    return;
                }
                return;
            }
            length -= Character.charCount(iCodePointBefore);
        }
    }

    private boolean mergeCompositeIntoString(CharSequence charSequence, int i, int i2, CharSequence charSequence2, StringBuilder sb, StringBuilder sb2) {
        int iOffsetByCodePoints = Character.offsetByCodePoints(charSequence2, 0, 1);
        if (iOffsetByCodePoints == charSequence2.length() || equalSubSequences(charSequence, i, charSequence2, iOffsetByCodePoints)) {
            return false;
        }
        sb.setLength(0);
        sb.append(charSequence, 0, i);
        sb2.setLength(0);
        sb2.append(charSequence, 0, i - iOffsetByCodePoints);
        sb2.appendCodePoint(i2);
        int combiningClass = 0;
        int i3 = 0;
        int iCharCount = iOffsetByCodePoints;
        int iCharCount2 = i;
        int iCodePointAt = -1;
        while (true) {
            if (iCodePointAt < 0) {
                if (iCharCount2 >= charSequence.length()) {
                    break;
                }
                iCodePointAt = Character.codePointAt(charSequence, iCharCount2);
                combiningClass = this.nfd.getCombiningClass(iCodePointAt);
                if (iCharCount < charSequence2.length()) {
                }
            } else {
                if (iCharCount < charSequence2.length()) {
                    break;
                }
                int iCodePointAt2 = Character.codePointAt(charSequence2, iCharCount);
                int combiningClass2 = this.nfd.getCombiningClass(iCodePointAt2);
                if (combiningClass2 == 0 || combiningClass < combiningClass2) {
                    return false;
                }
                if (combiningClass2 < combiningClass) {
                    sb.appendCodePoint(iCodePointAt2);
                    iCharCount += Character.charCount(iCodePointAt2);
                } else {
                    if (iCodePointAt2 != iCodePointAt) {
                        return false;
                    }
                    sb.appendCodePoint(iCodePointAt2);
                    iCharCount += Character.charCount(iCodePointAt2);
                    iCharCount2 += Character.charCount(iCodePointAt2);
                    iCodePointAt = -1;
                }
                i3 = combiningClass2;
            }
        }
    }

    private boolean equalSubSequences(CharSequence charSequence, int i, CharSequence charSequence2, int i2) {
        int length = charSequence.length();
        if (length - i != charSequence2.length() - i2) {
            return false;
        }
        while (i < length) {
            int i3 = i + 1;
            int i4 = i2 + 1;
            if (charSequence.charAt(i) != charSequence2.charAt(i2)) {
                return false;
            }
            i = i3;
            i2 = i4;
        }
        return true;
    }

    private boolean ignorePrefix(CharSequence charSequence) {
        return !isFCD(charSequence);
    }

    private boolean ignoreString(CharSequence charSequence) {
        return !isFCD(charSequence) || Normalizer2Impl.Hangul.isHangul(charSequence.charAt(0));
    }

    private boolean isFCD(CharSequence charSequence) {
        return this.fcd.isNormalized(charSequence);
    }

    private void closeOverComposites() {
        UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(COMPOSITES);
        while (unicodeSetIterator.next()) {
            this.cesLength = this.dataBuilder.getCEs(this.nfd.getDecomposition(unicodeSetIterator.codepoint), this.ces, 0);
            if (this.cesLength <= 31) {
                addIfDifferent("", unicodeSetIterator.getString(), this.ces, this.cesLength, -1);
            }
        }
    }

    private int addIfDifferent(CharSequence charSequence, CharSequence charSequence2, long[] jArr, int i, int i2) {
        long[] jArr2 = new long[31];
        if (!sameCEs(jArr, i, jArr2, this.dataBuilder.getCEs(charSequence, charSequence2, jArr2, 0))) {
            if (i2 == -1) {
                i2 = this.dataBuilder.encodeCEs(jArr, i);
            }
            this.dataBuilder.addCE32(charSequence, charSequence2, i2);
        }
        return i2;
    }

    private static boolean sameCEs(long[] jArr, int i, long[] jArr2, int i2) {
        if (i != i2) {
            return false;
        }
        for (int i3 = 0; i3 < i; i3++) {
            if (jArr[i3] != jArr2[i3]) {
                return false;
            }
        }
        return true;
    }

    private static final int alignWeightRight(int i) {
        if (i != 0) {
            while ((i & 255) == 0) {
                i >>>= 8;
            }
        }
        return i;
    }

    private void makeTailoredCEs() {
        int i;
        int i2;
        int i3;
        int iWeight16FromNode;
        int iWeight16FromNode2;
        int i4;
        int i5;
        int secondaryAfter;
        long j;
        int tertiaryAfter;
        CollationWeights collationWeights = new CollationWeights();
        CollationWeights collationWeights2 = new CollationWeights();
        CollationWeights collationWeights3 = new CollationWeights();
        long[] buffer = this.nodes.getBuffer();
        int i6 = 0;
        while (i6 < this.rootPrimaryIndexes.size()) {
            long j2 = buffer[this.rootPrimaryIndexes.elementAti(i6)];
            long jWeight32FromNode = weight32FromNode(j2);
            int tertiaryBoundary = jWeight32FromNode == 0 ? 0 : Collation.COMMON_WEIGHT16;
            int iFindPrimary = jWeight32FromNode == 0 ? 0 : this.rootElements.findPrimary(jWeight32FromNode);
            int iNextIndexFromNode = nextIndexFromNode(j2);
            long j3 = jWeight32FromNode;
            int i7 = tertiaryBoundary;
            int i8 = 0;
            int i9 = 0;
            int i10 = 0;
            int i11 = 0;
            while (iNextIndexFromNode != 0) {
                int i12 = i6;
                long j4 = buffer[iNextIndexFromNode];
                int iNextIndexFromNode2 = nextIndexFromNode(j4);
                long j5 = j3;
                int iStrengthFromNode = strengthFromNode(j4);
                if (iStrengthFromNode != 3) {
                    if (iStrengthFromNode == 2) {
                        if (isTailoredNode(j4)) {
                            if (i9 == 0) {
                                int iCountTailoredNodes = countTailoredNodes(buffer, iNextIndexFromNode2, 2) + 1;
                                if (tertiaryBoundary == 0) {
                                    tertiaryBoundary = this.rootElements.getTertiaryBoundary() - 256;
                                    tertiaryAfter = ((int) this.rootElements.getFirstTertiaryCE()) & Collation.ONLY_TERTIARY_MASK;
                                } else {
                                    tertiaryAfter = (i11 == 0 && i10 == 0) ? this.rootElements.getTertiaryAfter(iFindPrimary, i7, tertiaryBoundary) : tertiaryBoundary == 256 ? Collation.COMMON_WEIGHT16 : this.rootElements.getTertiaryBoundary();
                                }
                                collationWeights3.initForTertiary();
                                i = iNextIndexFromNode;
                                j = j5;
                                i5 = 1;
                                i4 = i7;
                                if (!collationWeights3.allocWeights(tertiaryBoundary, tertiaryAfter, iCountTailoredNodes)) {
                                    throw new UnsupportedOperationException("tertiary tailoring gap too small");
                                }
                            } else {
                                i4 = i7;
                                i = iNextIndexFromNode;
                                j = j5;
                                i5 = i9;
                            }
                            iWeight16FromNode2 = (int) collationWeights3.nextWeight();
                            j3 = j;
                        } else {
                            i4 = i7;
                            i = iNextIndexFromNode;
                            iWeight16FromNode2 = weight16FromNode(j4);
                            j3 = j5;
                            i5 = 0;
                        }
                        i3 = 0;
                    } else {
                        int i13 = i7;
                        i = iNextIndexFromNode;
                        long jNextWeight = j5;
                        int i14 = 1;
                        if (iStrengthFromNode == 1) {
                            if (isTailoredNode(j4)) {
                                if (i10 == 0) {
                                    int iCountTailoredNodes2 = countTailoredNodes(buffer, iNextIndexFromNode2, 1) + 1;
                                    int lastCommonSecondary = i13;
                                    if (lastCommonSecondary == 0) {
                                        int secondaryBoundary = this.rootElements.getSecondaryBoundary() - 256;
                                        secondaryAfter = (int) (this.rootElements.getFirstSecondaryCE() >> 16);
                                        lastCommonSecondary = secondaryBoundary;
                                    } else {
                                        secondaryAfter = i11 == 0 ? this.rootElements.getSecondaryAfter(iFindPrimary, lastCommonSecondary) : lastCommonSecondary == 256 ? Collation.COMMON_WEIGHT16 : this.rootElements.getSecondaryBoundary();
                                    }
                                    if (lastCommonSecondary == 1280) {
                                        lastCommonSecondary = this.rootElements.getLastCommonSecondary();
                                    }
                                    collationWeights2.initForSecondary();
                                    i2 = Collation.COMMON_WEIGHT16;
                                    if (!collationWeights2.allocWeights(lastCommonSecondary, secondaryAfter, iCountTailoredNodes2)) {
                                        throw new UnsupportedOperationException("secondary tailoring gap too small");
                                    }
                                } else {
                                    i2 = Collation.COMMON_WEIGHT16;
                                    i14 = i10;
                                }
                                iWeight16FromNode = (int) collationWeights2.nextWeight();
                                i10 = i14;
                            } else {
                                i2 = Collation.COMMON_WEIGHT16;
                                iWeight16FromNode = weight16FromNode(j4);
                                i10 = 0;
                            }
                            i3 = 0;
                        } else {
                            i2 = Collation.COMMON_WEIGHT16;
                            if (i11 == 0) {
                                int iCountTailoredNodes3 = countTailoredNodes(buffer, iNextIndexFromNode2, 0) + 1;
                                boolean zIsCompressiblePrimary = this.baseData.isCompressiblePrimary(jNextWeight);
                                long primaryAfter = this.rootElements.getPrimaryAfter(jNextWeight, iFindPrimary, zIsCompressiblePrimary);
                                collationWeights.initForPrimary(zIsCompressiblePrimary);
                                i3 = 0;
                                if (!collationWeights.allocWeights(jNextWeight, primaryAfter, iCountTailoredNodes3)) {
                                    throw new UnsupportedOperationException("primary tailoring gap too small");
                                }
                            } else {
                                i3 = 0;
                                i14 = i11;
                            }
                            jNextWeight = collationWeights.nextWeight();
                            i11 = i14;
                            i10 = i3;
                            iWeight16FromNode = 1280;
                        }
                        iWeight16FromNode2 = iWeight16FromNode == 0 ? i3 : i2;
                        i4 = iWeight16FromNode;
                        i5 = i3;
                        j3 = jNextWeight;
                    }
                    tertiaryBoundary = iWeight16FromNode2;
                    i9 = i5;
                    i8 = i3;
                    i7 = i4;
                } else {
                    if (i8 == 3) {
                        throw new UnsupportedOperationException("quaternary tailoring gap too small");
                    }
                    i8++;
                    i = iNextIndexFromNode;
                    j3 = j5;
                }
                if (isTailoredNode(j4)) {
                    buffer[i] = Collation.makeCE(j3, i7, tertiaryBoundary, i8);
                }
                iNextIndexFromNode = iNextIndexFromNode2;
                i6 = i12;
            }
            i6++;
        }
    }

    private static int countTailoredNodes(long[] jArr, int i, int i2) {
        int i3 = 0;
        while (i != 0) {
            long j = jArr[i];
            if (strengthFromNode(j) < i2) {
                break;
            }
            if (strengthFromNode(j) == i2) {
                if (!isTailoredNode(j)) {
                    break;
                }
                i3++;
            }
            i = nextIndexFromNode(j);
        }
        return i3;
    }

    private static final class CEFinalizer implements CollationDataBuilder.CEModifier {
        static final boolean $assertionsDisabled = false;
        private long[] finalCEs;

        CEFinalizer(long[] jArr) {
            this.finalCEs = jArr;
        }

        @Override
        public long modifyCE32(int i) {
            if (CollationBuilder.isTempCE32(i)) {
                return this.finalCEs[CollationBuilder.indexFromTempCE32(i)] | ((long) ((i & 192) << 8));
            }
            return Collation.NO_CE;
        }

        @Override
        public long modifyCE(long j) {
            if (CollationBuilder.isTempCE(j)) {
                return (j & 49152) | this.finalCEs[CollationBuilder.indexFromTempCE(j)];
            }
            return Collation.NO_CE;
        }
    }

    private void finalizeCEs() {
        CollationDataBuilder collationDataBuilder = new CollationDataBuilder();
        collationDataBuilder.initForTailoring(this.baseData);
        collationDataBuilder.copyFrom(this.dataBuilder, new CEFinalizer(this.nodes.getBuffer()));
        this.dataBuilder = collationDataBuilder;
    }

    private static long tempCEFromIndexAndStrength(int i, int i2) {
        return 4629700417037541376L + (((long) (1040384 & i)) << 43) + (((long) (i & 8128)) << 42) + ((long) ((i & 63) << 24)) + ((long) (i2 << 8));
    }

    private static int indexFromTempCE(long j) {
        long j2 = j - 4629700417037541376L;
        return (((int) (j2 >> 24)) & 63) | (((int) (j2 >> 43)) & 1040384) | (((int) (j2 >> 42)) & 8128);
    }

    private static int strengthFromTempCE(long j) {
        return (((int) j) >> 8) & 3;
    }

    private static boolean isTempCE(long j) {
        int i = ((int) j) >>> 24;
        return 6 <= i && i <= 69;
    }

    private static int indexFromTempCE32(int i) {
        int i2 = i - 1077937696;
        return ((i2 >> 8) & 63) | ((i2 >> 11) & 1040384) | ((i2 >> 10) & 8128);
    }

    private static boolean isTempCE32(int i) {
        int i2;
        return (i & 255) >= 2 && 6 <= (i2 = (i >> 8) & 255) && i2 <= 69;
    }

    private static int ceStrength(long j) {
        if (isTempCE(j)) {
            return strengthFromTempCE(j);
        }
        if (((-72057594037927936L) & j) != 0) {
            return 0;
        }
        if ((((int) j) & (-16777216)) != 0) {
            return 1;
        }
        return j != 0 ? 2 : 15;
    }

    private static long nodeFromWeight32(long j) {
        return j << 32;
    }

    private static long nodeFromWeight16(int i) {
        return ((long) i) << 48;
    }

    private static long nodeFromPreviousIndex(int i) {
        return ((long) i) << 28;
    }

    private static long nodeFromNextIndex(int i) {
        return i << 8;
    }

    private static long nodeFromStrength(int i) {
        return i;
    }

    private static long weight32FromNode(long j) {
        return j >>> 32;
    }

    private static int weight16FromNode(long j) {
        return ((int) (j >> 48)) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
    }

    private static int previousIndexFromNode(long j) {
        return ((int) (j >> 28)) & MAX_INDEX;
    }

    private static int nextIndexFromNode(long j) {
        return (((int) j) >> 8) & MAX_INDEX;
    }

    private static int strengthFromNode(long j) {
        return ((int) j) & 3;
    }

    private static boolean nodeHasBefore2(long j) {
        return (j & 64) != 0;
    }

    private static boolean nodeHasBefore3(long j) {
        return (j & 32) != 0;
    }

    private static boolean nodeHasAnyBefore(long j) {
        return (j & 96) != 0;
    }

    private static boolean isTailoredNode(long j) {
        return (j & 8) != 0;
    }

    private static long changeNodePreviousIndex(long j, int i) {
        return (j & (-281474708275201L)) | nodeFromPreviousIndex(i);
    }

    private static long changeNodeNextIndex(long j, int i) {
        return (j & (-268435201)) | nodeFromNextIndex(i);
    }
}
