package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2;
import android.icu.text.UnicodeSet;
import android.icu.util.CharsTrie;
import java.util.Iterator;

public final class TailoredSet {

    static final boolean f56assertionsDisabled;
    private CollationData baseData;
    private CollationData data;
    private String suffix;
    private UnicodeSet tailored;
    private StringBuilder unreversedPrefix = new StringBuilder();

    static {
        f56assertionsDisabled = !TailoredSet.class.desiredAssertionStatus();
    }

    public TailoredSet(UnicodeSet t) {
        this.tailored = t;
    }

    public void forData(CollationData d) {
        this.data = d;
        this.baseData = d.base;
        if (!f56assertionsDisabled) {
            if (!(this.baseData != null)) {
                throw new AssertionError();
            }
        }
        for (Trie2.Range range : this.data.trie) {
            if (range.leadSurrogate) {
                return;
            } else {
                enumTailoredRange(range.startCodePoint, range.endCodePoint, range.value, this);
            }
        }
    }

    private void enumTailoredRange(int start, int end, int ce32, TailoredSet ts) {
        if (ce32 == 192) {
            return;
        }
        ts.handleCE32(start, end, ce32);
    }

    private void handleCE32(int start, int end, int ce32) {
        if (!f56assertionsDisabled) {
            if (!(ce32 != 192)) {
                throw new AssertionError();
            }
        }
        if (!Collation.isSpecialCE32(ce32) || (ce32 = this.data.getIndirectCE32(ce32)) != 192) {
            do {
                int baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32(start));
                if (Collation.isSelfContainedCE32(ce32) && Collation.isSelfContainedCE32(baseCE32)) {
                    if (ce32 != baseCE32) {
                        this.tailored.add(start);
                    }
                } else {
                    compare(start, ce32, baseCE32);
                }
                start++;
            } while (start <= end);
        }
    }

    private void compare(int c, int ce32, int baseCE32) {
        int tag;
        int baseTag;
        if (Collation.isPrefixCE32(ce32)) {
            int dataIndex = Collation.indexFromCE32(ce32);
            ce32 = this.data.getFinalCE32(this.data.getCE32FromContexts(dataIndex));
            if (Collation.isPrefixCE32(baseCE32)) {
                int baseIndex = Collation.indexFromCE32(baseCE32);
                baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(baseIndex));
                comparePrefixes(c, this.data.contexts, dataIndex + 2, this.baseData.contexts, baseIndex + 2);
            } else {
                addPrefixes(this.data, c, this.data.contexts, dataIndex + 2);
            }
        } else if (Collation.isPrefixCE32(baseCE32)) {
            int baseIndex2 = Collation.indexFromCE32(baseCE32);
            baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(baseIndex2));
            addPrefixes(this.baseData, c, this.baseData.contexts, baseIndex2 + 2);
        }
        if (Collation.isContractionCE32(ce32)) {
            int dataIndex2 = Collation.indexFromCE32(ce32);
            if ((ce32 & 256) != 0) {
                ce32 = 1;
            } else {
                ce32 = this.data.getFinalCE32(this.data.getCE32FromContexts(dataIndex2));
            }
            if (Collation.isContractionCE32(baseCE32)) {
                int baseIndex3 = Collation.indexFromCE32(baseCE32);
                if ((baseCE32 & 256) != 0) {
                    baseCE32 = 1;
                } else {
                    baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(baseIndex3));
                }
                compareContractions(c, this.data.contexts, dataIndex2 + 2, this.baseData.contexts, baseIndex3 + 2);
            } else {
                addContractions(c, this.data.contexts, dataIndex2 + 2);
            }
        } else if (Collation.isContractionCE32(baseCE32)) {
            int baseIndex4 = Collation.indexFromCE32(baseCE32);
            baseCE32 = this.baseData.getFinalCE32(this.baseData.getCE32FromContexts(baseIndex4));
            addContractions(c, this.baseData.contexts, baseIndex4 + 2);
        }
        if (Collation.isSpecialCE32(ce32)) {
            tag = Collation.tagFromCE32(ce32);
            if (!f56assertionsDisabled) {
                if (!(tag != 8)) {
                    throw new AssertionError();
                }
            }
            if (!f56assertionsDisabled) {
                if (!(tag != 9)) {
                    throw new AssertionError();
                }
            }
            if (!f56assertionsDisabled) {
                if (!(tag != 14)) {
                    throw new AssertionError();
                }
            }
        } else {
            tag = -1;
        }
        if (Collation.isSpecialCE32(baseCE32)) {
            baseTag = Collation.tagFromCE32(baseCE32);
            if (!f56assertionsDisabled) {
                if (!(baseTag != 8)) {
                    throw new AssertionError();
                }
            }
            if (!f56assertionsDisabled) {
                if (!(baseTag != 9)) {
                    throw new AssertionError();
                }
            }
        } else {
            baseTag = -1;
        }
        if (baseTag == 14) {
            if (!Collation.isLongPrimaryCE32(ce32)) {
                add(c);
                return;
            }
            long dataCE = this.baseData.ces[Collation.indexFromCE32(baseCE32)];
            long p = Collation.getThreeBytePrimaryForOffsetData(c, dataCE);
            if (Collation.primaryFromLongPrimaryCE32(ce32) != p) {
                add(c);
                return;
            }
        }
        if (tag != baseTag) {
            add(c);
            return;
        }
        if (tag == 5) {
            int length = Collation.lengthFromCE32(ce32);
            int baseLength = Collation.lengthFromCE32(baseCE32);
            if (length != baseLength) {
                add(c);
                return;
            }
            int idx0 = Collation.indexFromCE32(ce32);
            int idx1 = Collation.indexFromCE32(baseCE32);
            for (int i = 0; i < length; i++) {
                if (this.data.ce32s[idx0 + i] != this.baseData.ce32s[idx1 + i]) {
                    add(c);
                    return;
                }
            }
            return;
        }
        if (tag == 6) {
            int length2 = Collation.lengthFromCE32(ce32);
            int baseLength2 = Collation.lengthFromCE32(baseCE32);
            if (length2 != baseLength2) {
                add(c);
                return;
            }
            int idx02 = Collation.indexFromCE32(ce32);
            int idx12 = Collation.indexFromCE32(baseCE32);
            for (int i2 = 0; i2 < length2; i2++) {
                if (this.data.ces[idx02 + i2] != this.baseData.ces[idx12 + i2]) {
                    add(c);
                    return;
                }
            }
            return;
        }
        if (tag == 12) {
            StringBuilder jamos = new StringBuilder();
            int length3 = Normalizer2Impl.Hangul.decompose(c, jamos);
            if (!this.tailored.contains(jamos.charAt(0)) && !this.tailored.contains(jamos.charAt(1)) && (length3 != 3 || !this.tailored.contains(jamos.charAt(2)))) {
                return;
            }
            add(c);
            return;
        }
        if (ce32 == baseCE32) {
            return;
        }
        add(c);
    }

    private void comparePrefixes(int c, CharSequence p, int pidx, CharSequence q, int qidx) {
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(p, pidx).iterator2();
        Iterator<CharsTrie.Entry> itIterator22 = new CharsTrie(q, qidx).iterator2();
        String tp = null;
        String bp = null;
        CharsTrie.Entry te = null;
        CharsTrie.Entry be = null;
        while (true) {
            if (tp == null) {
                if (itIterator2.hasNext()) {
                    te = itIterator2.next();
                    tp = te.chars.toString();
                } else {
                    te = null;
                    tp = "\uffff";
                }
            }
            if (bp == null) {
                if (itIterator22.hasNext()) {
                    be = itIterator22.next();
                    bp = be.chars.toString();
                } else {
                    be = null;
                    bp = "\uffff";
                }
            }
            if (tp == "\uffff" && bp == "\uffff") {
                return;
            }
            int cmp = tp.compareTo(bp);
            if (cmp < 0) {
                if (!f56assertionsDisabled) {
                    if (!(te != null)) {
                        throw new AssertionError();
                    }
                }
                addPrefix(this.data, tp, c, te.value);
                te = null;
                tp = null;
            } else if (cmp > 0) {
                if (!f56assertionsDisabled) {
                    if (!(be != null)) {
                        throw new AssertionError();
                    }
                }
                addPrefix(this.baseData, bp, c, be.value);
                be = null;
                bp = null;
            } else {
                setPrefix(tp);
                if (!f56assertionsDisabled) {
                    if (!((te == null || be == null) ? false : true)) {
                        throw new AssertionError();
                    }
                }
                compare(c, te.value, be.value);
                resetPrefix();
                be = null;
                te = null;
                bp = null;
                tp = null;
            }
        }
    }

    private void compareContractions(int c, CharSequence p, int pidx, CharSequence q, int qidx) {
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(p, pidx).iterator2();
        Iterator<CharsTrie.Entry> itIterator22 = new CharsTrie(q, qidx).iterator2();
        String ts = null;
        String bs = null;
        CharsTrie.Entry te = null;
        CharsTrie.Entry be = null;
        while (true) {
            if (ts == null) {
                if (itIterator2.hasNext()) {
                    te = itIterator2.next();
                    ts = te.chars.toString();
                } else {
                    te = null;
                    ts = "\uffff\uffff";
                }
            }
            if (bs == null) {
                if (itIterator22.hasNext()) {
                    be = itIterator22.next();
                    bs = be.chars.toString();
                } else {
                    be = null;
                    bs = "\uffff\uffff";
                }
            }
            if (ts == "\uffff\uffff" && bs == "\uffff\uffff") {
                return;
            }
            int cmp = ts.compareTo(bs);
            if (cmp < 0) {
                addSuffix(c, ts);
                te = null;
                ts = null;
            } else if (cmp > 0) {
                addSuffix(c, bs);
                be = null;
                bs = null;
            } else {
                this.suffix = ts;
                compare(c, te.value, be.value);
                this.suffix = null;
                be = null;
                te = null;
                bs = null;
                ts = null;
            }
        }
    }

    private void addPrefixes(CollationData d, int c, CharSequence p, int pidx) {
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(p, pidx).iterator2();
        while (itIterator2.hasNext()) {
            CharsTrie.Entry e = itIterator2.next();
            addPrefix(d, e.chars, c, e.value);
        }
    }

    private void addPrefix(CollationData d, CharSequence pfx, int c, int ce32) {
        setPrefix(pfx);
        int ce322 = d.getFinalCE32(ce32);
        if (Collation.isContractionCE32(ce322)) {
            int idx = Collation.indexFromCE32(ce322);
            addContractions(c, d.contexts, idx + 2);
        }
        this.tailored.add(new StringBuilder(this.unreversedPrefix.appendCodePoint(c)));
        resetPrefix();
    }

    private void addContractions(int c, CharSequence p, int pidx) {
        Iterator<CharsTrie.Entry> itIterator2 = new CharsTrie(p, pidx).iterator2();
        while (itIterator2.hasNext()) {
            CharsTrie.Entry e = itIterator2.next();
            addSuffix(c, e.chars);
        }
    }

    private void addSuffix(int c, CharSequence sfx) {
        this.tailored.add(new StringBuilder(this.unreversedPrefix).appendCodePoint(c).append(sfx));
    }

    private void add(int c) {
        if (this.unreversedPrefix.length() == 0 && this.suffix == null) {
            this.tailored.add(c);
            return;
        }
        StringBuilder s = new StringBuilder(this.unreversedPrefix);
        s.appendCodePoint(c);
        if (this.suffix != null) {
            s.append(this.suffix);
        }
        this.tailored.add(s);
    }

    private void setPrefix(CharSequence pfx) {
        this.unreversedPrefix.setLength(0);
        this.unreversedPrefix.append(pfx).reverse();
    }

    private void resetPrefix() {
        this.unreversedPrefix.setLength(0);
    }
}
