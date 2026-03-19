package android.icu.impl.coll;

import android.icu.impl.Trie2;
import android.icu.text.UnicodeSet;
import android.icu.util.CharsTrie;
import java.util.Iterator;

public final class ContractionsAndExpansions {
    static final boolean $assertionsDisabled = false;
    private boolean addPrefixes;
    private UnicodeSet contractions;
    private CollationData data;
    private UnicodeSet expansions;
    private UnicodeSet ranges;
    private CESink sink;
    private String suffix;
    private int checkTailored = 0;
    private UnicodeSet tailored = new UnicodeSet();
    private StringBuilder unreversedPrefix = new StringBuilder();
    private long[] ces = new long[31];

    public interface CESink {
        void handleCE(long j);

        void handleExpansion(long[] jArr, int i, int i2);
    }

    public ContractionsAndExpansions(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, CESink cESink, boolean z) {
        this.contractions = unicodeSet;
        this.expansions = unicodeSet2;
        this.sink = cESink;
        this.addPrefixes = z;
    }

    public void forData(CollationData collationData) {
        if (collationData.base != null) {
            this.checkTailored = -1;
        }
        this.data = collationData;
        for (Trie2.Range range : this.data.trie) {
            if (range.leadSurrogate) {
                break;
            } else {
                enumCnERange(range.startCodePoint, range.endCodePoint, range.value, this);
            }
        }
        if (collationData.base == null) {
            return;
        }
        this.tailored.freeze();
        this.checkTailored = 1;
        this.data = collationData.base;
        for (Trie2.Range range2 : this.data.trie) {
            if (!range2.leadSurrogate) {
                enumCnERange(range2.startCodePoint, range2.endCodePoint, range2.value, this);
            } else {
                return;
            }
        }
    }

    private void enumCnERange(int i, int i2, int i3, ContractionsAndExpansions contractionsAndExpansions) {
        if (contractionsAndExpansions.checkTailored != 0) {
            if (contractionsAndExpansions.checkTailored < 0) {
                if (i3 == 192) {
                    return;
                } else {
                    contractionsAndExpansions.tailored.add(i, i2);
                }
            } else if (i == i2) {
                if (contractionsAndExpansions.tailored.contains(i)) {
                    return;
                }
            } else if (contractionsAndExpansions.tailored.containsSome(i, i2)) {
                if (contractionsAndExpansions.ranges == null) {
                    contractionsAndExpansions.ranges = new UnicodeSet();
                }
                contractionsAndExpansions.ranges.set(i, i2).removeAll(contractionsAndExpansions.tailored);
                int rangeCount = contractionsAndExpansions.ranges.getRangeCount();
                for (int i4 = 0; i4 < rangeCount; i4++) {
                    contractionsAndExpansions.handleCE32(contractionsAndExpansions.ranges.getRangeStart(i4), contractionsAndExpansions.ranges.getRangeEnd(i4), i3);
                }
            }
        }
        contractionsAndExpansions.handleCE32(i, i2, i3);
    }

    public void forCodePoint(CollationData collationData, int i) {
        int ce32 = collationData.getCE32(i);
        if (ce32 == 192) {
            collationData = collationData.base;
            ce32 = collationData.getCE32(i);
        }
        this.data = collationData;
        handleCE32(i, i, ce32);
    }

    private void handleCE32(int i, int i2, int i3) {
        while ((i3 & 255) >= 192) {
            switch (Collation.tagFromCE32(i3)) {
                case 0:
                    return;
                case 1:
                    if (this.sink != null) {
                        this.sink.handleCE(Collation.ceFromLongPrimaryCE32(i3));
                        return;
                    }
                    return;
                case 2:
                    if (this.sink != null) {
                        this.sink.handleCE(Collation.ceFromLongSecondaryCE32(i3));
                        return;
                    }
                    return;
                case 3:
                case 7:
                case 13:
                    throw new AssertionError(String.format("Unexpected CE32 tag type %d for ce32=0x%08x", Integer.valueOf(Collation.tagFromCE32(i3)), Integer.valueOf(i3)));
                case 4:
                    if (this.sink != null) {
                        this.ces[0] = Collation.latinCE0FromCE32(i3);
                        this.ces[1] = Collation.latinCE1FromCE32(i3);
                        this.sink.handleExpansion(this.ces, 0, 2);
                    }
                    if (this.unreversedPrefix.length() == 0) {
                        addExpansions(i, i2);
                        return;
                    }
                    return;
                case 5:
                    if (this.sink != null) {
                        int iIndexFromCE32 = Collation.indexFromCE32(i3);
                        int iLengthFromCE32 = Collation.lengthFromCE32(i3);
                        for (int i4 = 0; i4 < iLengthFromCE32; i4++) {
                            this.ces[i4] = Collation.ceFromCE32(this.data.ce32s[iIndexFromCE32 + i4]);
                        }
                        this.sink.handleExpansion(this.ces, 0, iLengthFromCE32);
                    }
                    if (this.unreversedPrefix.length() == 0) {
                        addExpansions(i, i2);
                        return;
                    }
                    return;
                case 6:
                    if (this.sink != null) {
                        this.sink.handleExpansion(this.data.ces, Collation.indexFromCE32(i3), Collation.lengthFromCE32(i3));
                    }
                    if (this.unreversedPrefix.length() == 0) {
                        addExpansions(i, i2);
                        return;
                    }
                    return;
                case 8:
                    handlePrefixes(i, i2, i3);
                    return;
                case 9:
                    handleContractions(i, i2, i3);
                    return;
                case 10:
                    i3 = this.data.ce32s[Collation.indexFromCE32(i3)];
                    break;
                case 11:
                    i3 = this.data.ce32s[0];
                    break;
                case 12:
                    if (this.sink != null) {
                        UTF16CollationIterator uTF16CollationIterator = new UTF16CollationIterator(this.data);
                        StringBuilder sb = new StringBuilder(1);
                        for (int i5 = i; i5 <= i2; i5++) {
                            sb.setLength(0);
                            sb.appendCodePoint(i5);
                            uTF16CollationIterator.setText(false, sb, 0);
                            this.sink.handleExpansion(uTF16CollationIterator.getCEs(), 0, uTF16CollationIterator.fetchCEs() - 1);
                        }
                    }
                    if (this.unreversedPrefix.length() == 0) {
                        addExpansions(i, i2);
                        return;
                    }
                    return;
                case 14:
                    return;
                case 15:
                    return;
            }
        }
        if (this.sink != null) {
            this.sink.handleCE(Collation.ceFromSimpleCE32(i3));
        }
    }

    private void handlePrefixes(int i, int i2, int i3) {
        int iIndexFromCE32 = Collation.indexFromCE32(i3);
        handleCE32(i, i2, this.data.getCE32FromContexts(iIndexFromCE32));
        if (!this.addPrefixes) {
            return;
        }
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(this.data.contexts, iIndexFromCE32 + 2).iterator2();
        while (itIterator2.hasNext()) {
            CharsTrie.Entry next = itIterator2.next();
            setPrefix(next.chars);
            addStrings(i, i2, this.contractions);
            addStrings(i, i2, this.expansions);
            handleCE32(i, i2, next.value);
        }
        resetPrefix();
    }

    void handleContractions(int i, int i2, int i3) {
        int iIndexFromCE32 = Collation.indexFromCE32(i3);
        if ((i3 & 256) == 0) {
            handleCE32(i, i2, this.data.getCE32FromContexts(iIndexFromCE32));
        }
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(this.data.contexts, iIndexFromCE32 + 2).iterator2();
        while (itIterator2.hasNext()) {
            CharsTrie.Entry next = itIterator2.next();
            this.suffix = next.chars.toString();
            addStrings(i, i2, this.contractions);
            if (this.unreversedPrefix.length() != 0) {
                addStrings(i, i2, this.expansions);
            }
            handleCE32(i, i2, next.value);
        }
        this.suffix = null;
    }

    void addExpansions(int i, int i2) {
        if (this.unreversedPrefix.length() == 0 && this.suffix == null) {
            if (this.expansions != null) {
                this.expansions.add(i, i2);
                return;
            }
            return;
        }
        addStrings(i, i2, this.expansions);
    }

    void addStrings(int i, int i2, UnicodeSet unicodeSet) {
        if (unicodeSet == null) {
            return;
        }
        StringBuilder sb = new StringBuilder(this.unreversedPrefix);
        do {
            sb.appendCodePoint(i);
            if (this.suffix != null) {
                sb.append(this.suffix);
            }
            unicodeSet.add(sb);
            sb.setLength(this.unreversedPrefix.length());
            i++;
        } while (i <= i2);
    }

    private void setPrefix(CharSequence charSequence) {
        this.unreversedPrefix.setLength(0);
        StringBuilder sb = this.unreversedPrefix;
        sb.append(charSequence);
        sb.reverse();
    }

    private void resetPrefix() {
        this.unreversedPrefix.setLength(0);
    }
}
