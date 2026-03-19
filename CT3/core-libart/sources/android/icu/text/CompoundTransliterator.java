package android.icu.text;

import android.icu.text.Transliterator;
import java.util.List;

class CompoundTransliterator extends Transliterator {
    private int numAnonymousRBTs;
    private Transliterator[] trans;

    CompoundTransliterator(List<Transliterator> list) {
        this(list, 0);
    }

    CompoundTransliterator(List<Transliterator> list, int numAnonymousRBTs) {
        super("", null);
        this.numAnonymousRBTs = 0;
        this.trans = null;
        init(list, 0, false);
        this.numAnonymousRBTs = numAnonymousRBTs;
    }

    CompoundTransliterator(String id, UnicodeFilter filter2, Transliterator[] trans2, int numAnonymousRBTs2) {
        super(id, filter2);
        this.numAnonymousRBTs = 0;
        this.trans = trans2;
        this.numAnonymousRBTs = numAnonymousRBTs2;
    }

    private void init(List<Transliterator> list, int direction, boolean fixReverseID) {
        int count = list.size();
        this.trans = new Transliterator[count];
        for (int i = 0; i < count; i++) {
            int j = direction == 0 ? i : (count - 1) - i;
            this.trans[i] = list.get(j);
        }
        if (direction == 1 && fixReverseID) {
            StringBuilder newID = new StringBuilder();
            for (int i2 = 0; i2 < count; i2++) {
                if (i2 > 0) {
                    newID.append(';');
                }
                newID.append(this.trans[i2].getID());
            }
            setID(newID.toString());
        }
        computeMaximumContextLength();
    }

    public int getCount() {
        return this.trans.length;
    }

    public Transliterator getTransliterator(int index) {
        return this.trans[index];
    }

    private static void _smartAppend(StringBuilder buf, char c) {
        if (buf.length() == 0 || buf.charAt(buf.length() - 1) == c) {
            return;
        }
        buf.append(c);
    }

    @Override
    public String toRules(boolean escapeUnprintable) {
        String rule;
        StringBuilder rulesSource = new StringBuilder();
        if (this.numAnonymousRBTs >= 1 && getFilter() != null) {
            rulesSource.append("::").append(getFilter().toPattern(escapeUnprintable)).append(';');
        }
        for (int i = 0; i < this.trans.length; i++) {
            if (this.trans[i].getID().startsWith("%Pass")) {
                rule = this.trans[i].toRules(escapeUnprintable);
                if (this.numAnonymousRBTs > 1 && i > 0 && this.trans[i - 1].getID().startsWith("%Pass")) {
                    rule = "::Null;" + rule;
                }
            } else {
                rule = this.trans[i].getID().indexOf(59) >= 0 ? this.trans[i].toRules(escapeUnprintable) : this.trans[i].baseToRules(escapeUnprintable);
            }
            _smartAppend(rulesSource, '\n');
            rulesSource.append(rule);
            _smartAppend(rulesSource, ';');
        }
        return rulesSource.toString();
    }

    @Override
    public void addSourceTargetSet(UnicodeSet filter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        UnicodeSet myFilter = new UnicodeSet(getFilterAsUnicodeSet(filter));
        UnicodeSet tempTargetSet = new UnicodeSet();
        for (int i = 0; i < this.trans.length; i++) {
            tempTargetSet.clear();
            this.trans[i].addSourceTargetSet(myFilter, sourceSet, tempTargetSet);
            targetSet.addAll(tempTargetSet);
            myFilter.addAll(tempTargetSet);
        }
    }

    @Override
    protected void handleTransliterate(Replaceable text, Transliterator.Position index, boolean incremental) {
        if (this.trans.length < 1) {
            index.start = index.limit;
            return;
        }
        int compoundLimit = index.limit;
        int compoundStart = index.start;
        int delta = 0;
        for (int i = 0; i < this.trans.length; i++) {
            index.start = compoundStart;
            int limit = index.limit;
            if (index.start == index.limit) {
                break;
            }
            this.trans[i].filteredTransliterate(text, index, incremental);
            if (!incremental && index.start != index.limit) {
                throw new RuntimeException("ERROR: Incomplete non-incremental transliteration by " + this.trans[i].getID());
            }
            delta += index.limit - limit;
            if (incremental) {
                index.limit = index.start;
            }
        }
        index.limit = compoundLimit + delta;
    }

    private void computeMaximumContextLength() {
        int max = 0;
        for (int i = 0; i < this.trans.length; i++) {
            int len = this.trans[i].getMaximumContextLength();
            if (len > max) {
                max = len;
            }
        }
        setMaximumContextLength(max);
    }

    public Transliterator safeClone() {
        ?? filter = getFilter();
        ?? unicodeSet = filter;
        if (filter != 0) {
            boolean z = filter instanceof UnicodeSet;
            unicodeSet = filter;
            if (z) {
                unicodeSet = new UnicodeSet((UnicodeSet) filter);
            }
        }
        return new CompoundTransliterator(getID(), unicodeSet, this.trans, this.numAnonymousRBTs);
    }
}
