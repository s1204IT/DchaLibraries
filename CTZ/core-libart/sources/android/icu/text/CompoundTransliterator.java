package android.icu.text;

import android.icu.text.Transliterator;
import java.util.List;

class CompoundTransliterator extends Transliterator {
    private int numAnonymousRBTs;
    private Transliterator[] trans;

    CompoundTransliterator(List<Transliterator> list) {
        this(list, 0);
    }

    CompoundTransliterator(List<Transliterator> list, int i) {
        super("", null);
        this.numAnonymousRBTs = 0;
        this.trans = null;
        init(list, 0, false);
        this.numAnonymousRBTs = i;
    }

    CompoundTransliterator(String str, UnicodeFilter unicodeFilter, Transliterator[] transliteratorArr, int i) {
        super(str, unicodeFilter);
        this.numAnonymousRBTs = 0;
        this.trans = transliteratorArr;
        this.numAnonymousRBTs = i;
    }

    private void init(List<Transliterator> list, int i, boolean z) {
        int i2;
        int size = list.size();
        this.trans = new Transliterator[size];
        for (int i3 = 0; i3 < size; i3++) {
            if (i != 0) {
                i2 = (size - 1) - i3;
            } else {
                i2 = i3;
            }
            this.trans[i3] = list.get(i2);
        }
        if (i == 1 && z) {
            StringBuilder sb = new StringBuilder();
            for (int i4 = 0; i4 < size; i4++) {
                if (i4 > 0) {
                    sb.append(';');
                }
                sb.append(this.trans[i4].getID());
            }
            setID(sb.toString());
        }
        computeMaximumContextLength();
    }

    public int getCount() {
        return this.trans.length;
    }

    public Transliterator getTransliterator(int i) {
        return this.trans[i];
    }

    private static void _smartAppend(StringBuilder sb, char c) {
        if (sb.length() != 0 && sb.charAt(sb.length() - 1) != c) {
            sb.append(c);
        }
    }

    @Override
    public String toRules(boolean z) {
        String rules;
        StringBuilder sb = new StringBuilder();
        if (this.numAnonymousRBTs >= 1 && getFilter() != null) {
            sb.append("::");
            sb.append(getFilter().toPattern(z));
            sb.append(';');
        }
        for (int i = 0; i < this.trans.length; i++) {
            if (this.trans[i].getID().startsWith("%Pass")) {
                rules = this.trans[i].toRules(z);
                if (this.numAnonymousRBTs > 1 && i > 0 && this.trans[i - 1].getID().startsWith("%Pass")) {
                    rules = "::Null;" + rules;
                }
            } else {
                rules = this.trans[i].getID().indexOf(59) >= 0 ? this.trans[i].toRules(z) : this.trans[i].baseToRules(z);
            }
            _smartAppend(sb, '\n');
            sb.append(rules);
            _smartAppend(sb, ';');
        }
        return sb.toString();
    }

    @Override
    public void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        UnicodeSet unicodeSet4 = new UnicodeSet(getFilterAsUnicodeSet(unicodeSet));
        UnicodeSet unicodeSet5 = new UnicodeSet();
        for (int i = 0; i < this.trans.length; i++) {
            unicodeSet5.clear();
            this.trans[i].addSourceTargetSet(unicodeSet4, unicodeSet2, unicodeSet5);
            unicodeSet3.addAll(unicodeSet5);
            unicodeSet4.addAll(unicodeSet5);
        }
    }

    @Override
    protected void handleTransliterate(Replaceable replaceable, Transliterator.Position position, boolean z) {
        if (this.trans.length < 1) {
            position.start = position.limit;
            return;
        }
        int i = position.limit;
        int i2 = position.start;
        int i3 = 0;
        for (int i4 = 0; i4 < this.trans.length; i4++) {
            position.start = i2;
            int i5 = position.limit;
            if (position.start == position.limit) {
                break;
            }
            this.trans[i4].filteredTransliterate(replaceable, position, z);
            if (!z && position.start != position.limit) {
                throw new RuntimeException("ERROR: Incomplete non-incremental transliteration by " + this.trans[i4].getID());
            }
            i3 += position.limit - i5;
            if (z) {
                position.limit = position.start;
            }
        }
        position.limit = i + i3;
    }

    private void computeMaximumContextLength() {
        int i = 0;
        for (int i2 = 0; i2 < this.trans.length; i2++) {
            int maximumContextLength = this.trans[i2].getMaximumContextLength();
            if (maximumContextLength > i) {
                i = maximumContextLength;
            }
        }
        setMaximumContextLength(i);
    }

    public Transliterator safeClone() {
        UnicodeFilter filter = getFilter();
        if (filter != null && (filter instanceof UnicodeSet)) {
            filter = new UnicodeSet((UnicodeSet) filter);
        }
        return new CompoundTransliterator(getID(), filter, this.trans, this.numAnonymousRBTs);
    }
}
