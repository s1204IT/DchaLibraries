package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2;
import android.icu.impl.Utility;
import android.icu.text.UnicodeSet;
import android.icu.util.CharsTrie;
import java.util.Iterator;

public final class TailoredSet {
    static final boolean $assertionsDisabled = false;
    private CollationData baseData;
    private CollationData data;
    private String suffix;
    private UnicodeSet tailored;
    private StringBuilder unreversedPrefix = new StringBuilder();

    public TailoredSet(UnicodeSet unicodeSet) {
        this.tailored = unicodeSet;
    }

    public void forData(CollationData collationData) {
        this.data = collationData;
        this.baseData = collationData.base;
        for (Trie2.Range range : this.data.trie) {
            if (!range.leadSurrogate) {
                enumTailoredRange(range.startCodePoint, range.endCodePoint, range.value, this);
            } else {
                return;
            }
        }
    }

    private void enumTailoredRange(int i, int i2, int i3, TailoredSet tailoredSet) {
        if (i3 == 192) {
            return;
        }
        tailoredSet.handleCE32(i, i2, i3);
    }

    private void handleCE32(int i, int i2, int i3) {
        if (Collation.isSpecialCE32(i3) && (i3 = this.data.getIndirectCE32(i3)) == 192) {
            return;
        }
        do {
            int finalCE32 = this.baseData.getFinalCE32(this.baseData.getCE32(i));
            if (Collation.isSelfContainedCE32(i3) && Collation.isSelfContainedCE32(finalCE32)) {
                if (i3 != finalCE32) {
                    this.tailored.add(i);
                }
            } else {
                compare(i, i3, finalCE32);
            }
            i++;
        } while (i <= i2);
    }

    private void compare(int i, int i2, int i3) {
        int iTagFromCE32;
        if (Collation.isPrefixCE32(i2)) {
            int iIndexFromCE32 = Collation.indexFromCE32(i2);
            int finalCE32 = this.data.getFinalCE32(this.data.getCE32FromContexts(iIndexFromCE32));
            if (Collation.isPrefixCE32(i3)) {
                int iIndexFromCE322 = Collation.indexFromCE32(i3);
                int finalCE322 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(iIndexFromCE322));
                comparePrefixes(i, this.data.contexts, iIndexFromCE32 + 2, this.baseData.contexts, iIndexFromCE322 + 2);
                i3 = finalCE322;
            } else {
                addPrefixes(this.data, i, this.data.contexts, iIndexFromCE32 + 2);
            }
            i2 = finalCE32;
        } else if (Collation.isPrefixCE32(i3)) {
            int iIndexFromCE323 = Collation.indexFromCE32(i3);
            int finalCE323 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(iIndexFromCE323));
            addPrefixes(this.baseData, i, this.baseData.contexts, iIndexFromCE323 + 2);
            i3 = finalCE323;
        }
        if (Collation.isContractionCE32(i2)) {
            int iIndexFromCE324 = Collation.indexFromCE32(i2);
            if ((i2 & 256) == 0) {
                i2 = this.data.getFinalCE32(this.data.getCE32FromContexts(iIndexFromCE324));
            } else {
                i2 = 1;
            }
            if (Collation.isContractionCE32(i3)) {
                int iIndexFromCE325 = Collation.indexFromCE32(i3);
                if ((i3 & 256) == 0) {
                    i3 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(iIndexFromCE325));
                } else {
                    i3 = 1;
                }
                compareContractions(i, this.data.contexts, iIndexFromCE324 + 2, this.baseData.contexts, iIndexFromCE325 + 2);
            } else {
                addContractions(i, this.data.contexts, iIndexFromCE324 + 2);
            }
        } else if (Collation.isContractionCE32(i3)) {
            int iIndexFromCE326 = Collation.indexFromCE32(i3);
            int finalCE324 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(iIndexFromCE326));
            addContractions(i, this.baseData.contexts, iIndexFromCE326 + 2);
            i3 = finalCE324;
        }
        if (Collation.isSpecialCE32(i2)) {
            iTagFromCE32 = Collation.tagFromCE32(i2);
        } else {
            iTagFromCE32 = -1;
        }
        int iTagFromCE322 = Collation.isSpecialCE32(i3) ? Collation.tagFromCE32(i3) : -1;
        if (iTagFromCE322 == 14) {
            if (!Collation.isLongPrimaryCE32(i2)) {
                add(i);
                return;
            } else {
                if (Collation.primaryFromLongPrimaryCE32(i2) != Collation.getThreeBytePrimaryForOffsetData(i, this.baseData.ces[Collation.indexFromCE32(i3)])) {
                    add(i);
                    return;
                }
            }
        }
        if (iTagFromCE32 != iTagFromCE322) {
            add(i);
            return;
        }
        int i4 = 0;
        if (iTagFromCE32 == 5) {
            int iLengthFromCE32 = Collation.lengthFromCE32(i2);
            if (iLengthFromCE32 != Collation.lengthFromCE32(i3)) {
                add(i);
                return;
            }
            int iIndexFromCE327 = Collation.indexFromCE32(i2);
            int iIndexFromCE328 = Collation.indexFromCE32(i3);
            while (i4 < iLengthFromCE32) {
                if (this.data.ce32s[iIndexFromCE327 + i4] == this.baseData.ce32s[iIndexFromCE328 + i4]) {
                    i4++;
                } else {
                    add(i);
                    return;
                }
            }
            return;
        }
        if (iTagFromCE32 == 6) {
            int iLengthFromCE322 = Collation.lengthFromCE32(i2);
            if (iLengthFromCE322 != Collation.lengthFromCE32(i3)) {
                add(i);
                return;
            }
            int iIndexFromCE329 = Collation.indexFromCE32(i2);
            int iIndexFromCE3210 = Collation.indexFromCE32(i3);
            while (i4 < iLengthFromCE322) {
                if (this.data.ces[iIndexFromCE329 + i4] == this.baseData.ces[iIndexFromCE3210 + i4]) {
                    i4++;
                } else {
                    add(i);
                    return;
                }
            }
            return;
        }
        if (iTagFromCE32 != 12) {
            if (i2 != i3) {
                add(i);
                return;
            }
            return;
        }
        StringBuilder sb = new StringBuilder();
        int iDecompose = Normalizer2Impl.Hangul.decompose(i, sb);
        if (this.tailored.contains(sb.charAt(0)) || this.tailored.contains(sb.charAt(1)) || (iDecompose == 3 && this.tailored.contains(sb.charAt(2)))) {
            add(i);
        }
    }

    private void comparePrefixes(int i, CharSequence charSequence, int i2, CharSequence charSequence2, int i3) {
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(charSequence, i2).iterator2();
        Iterator<CharsTrie.Entry> itIterator22 = new CharsTrie(charSequence2, i3).iterator2();
        String string = null;
        String string2 = null;
        CharsTrie.Entry entry = null;
        CharsTrie.Entry entry2 = null;
        while (true) {
            if (string == null) {
                if (!itIterator2.hasNext()) {
                    string = "\uffff";
                    entry = null;
                } else {
                    CharsTrie.Entry next = itIterator2.next();
                    entry = next;
                    string = next.chars.toString();
                }
            }
            if (string2 == null) {
                if (!itIterator22.hasNext()) {
                    string2 = "\uffff";
                    entry2 = null;
                } else {
                    CharsTrie.Entry next2 = itIterator22.next();
                    entry2 = next2;
                    string2 = next2.chars.toString();
                }
            }
            if (!Utility.sameObjects(string, "\uffff") || !Utility.sameObjects(string2, "\uffff")) {
                int iCompareTo = string.compareTo(string2);
                if (iCompareTo < 0) {
                    addPrefix(this.data, string, i, entry.value);
                    string = null;
                    entry = null;
                } else if (iCompareTo > 0) {
                    addPrefix(this.baseData, string2, i, entry2.value);
                    string2 = null;
                    entry2 = null;
                } else {
                    setPrefix(string);
                    compare(i, entry.value, entry2.value);
                    resetPrefix();
                    string = null;
                    string2 = null;
                    entry = null;
                    entry2 = null;
                }
            } else {
                return;
            }
        }
    }

    private void compareContractions(int i, CharSequence charSequence, int i2, CharSequence charSequence2, int i3) {
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(charSequence, i2).iterator2();
        Iterator<CharsTrie.Entry> itIterator22 = new CharsTrie(charSequence2, i3).iterator2();
        String string = null;
        String string2 = null;
        CharsTrie.Entry entry = null;
        CharsTrie.Entry entry2 = null;
        while (true) {
            if (string == null) {
                if (!itIterator2.hasNext()) {
                    string = "\uffff\uffff";
                    entry = null;
                } else {
                    CharsTrie.Entry next = itIterator2.next();
                    entry = next;
                    string = next.chars.toString();
                }
            }
            if (string2 == null) {
                if (!itIterator22.hasNext()) {
                    string2 = "\uffff\uffff";
                    entry2 = null;
                } else {
                    CharsTrie.Entry next2 = itIterator22.next();
                    entry2 = next2;
                    string2 = next2.chars.toString();
                }
            }
            if (!Utility.sameObjects(string, "\uffff\uffff") || !Utility.sameObjects(string2, "\uffff\uffff")) {
                int iCompareTo = string.compareTo(string2);
                if (iCompareTo < 0) {
                    addSuffix(i, string);
                    string = null;
                    entry = null;
                } else if (iCompareTo > 0) {
                    addSuffix(i, string2);
                    string2 = null;
                    entry2 = null;
                } else {
                    this.suffix = string;
                    compare(i, entry.value, entry2.value);
                    this.suffix = null;
                    string = null;
                    string2 = null;
                    entry = null;
                    entry2 = null;
                }
            } else {
                return;
            }
        }
    }

    private void addPrefixes(CollationData collationData, int i, CharSequence charSequence, int i2) {
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(charSequence, i2).iterator2();
        while (itIterator2.hasNext()) {
            CharsTrie.Entry next = itIterator2.next();
            addPrefix(collationData, next.chars, i, next.value);
        }
    }

    private void addPrefix(CollationData collationData, CharSequence charSequence, int i, int i2) {
        setPrefix(charSequence);
        int finalCE32 = collationData.getFinalCE32(i2);
        if (Collation.isContractionCE32(finalCE32)) {
            addContractions(i, collationData.contexts, Collation.indexFromCE32(finalCE32) + 2);
        }
        this.tailored.add(new StringBuilder(this.unreversedPrefix.appendCodePoint(i)));
        resetPrefix();
    }

    private void addContractions(int i, CharSequence charSequence, int i2) {
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(charSequence, i2).iterator2();
        while (itIterator2.hasNext()) {
            addSuffix(i, itIterator2.next().chars);
        }
    }

    private void addSuffix(int i, CharSequence charSequence) {
        UnicodeSet unicodeSet = this.tailored;
        StringBuilder sbAppendCodePoint = new StringBuilder(this.unreversedPrefix).appendCodePoint(i);
        sbAppendCodePoint.append(charSequence);
        unicodeSet.add(sbAppendCodePoint);
    }

    private void add(int i) {
        if (this.unreversedPrefix.length() == 0 && this.suffix == null) {
            this.tailored.add(i);
            return;
        }
        StringBuilder sb = new StringBuilder(this.unreversedPrefix);
        sb.appendCodePoint(i);
        if (this.suffix != null) {
            sb.append(this.suffix);
        }
        this.tailored.add(sb);
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
