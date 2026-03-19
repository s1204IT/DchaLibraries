package android.icu.impl;

import android.icu.impl.ICUResourceBundle;
import android.icu.text.BreakIterator;
import android.icu.text.FilteredBreakIteratorBuilder;
import android.icu.text.UCharacterIterator;
import android.icu.util.BytesTrie;
import android.icu.util.CharsTrie;
import android.icu.util.CharsTrieBuilder;
import android.icu.util.StringTrieBuilder;
import android.icu.util.ULocale;
import java.text.CharacterIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class SimpleFilteredSentenceBreakIterator extends BreakIterator {
    private CharsTrie backwardsTrie;
    private BreakIterator delegate;
    private CharsTrie forwardsPartialTrie;
    private UCharacterIterator text;

    public SimpleFilteredSentenceBreakIterator(BreakIterator breakIterator, CharsTrie charsTrie, CharsTrie charsTrie2) {
        this.delegate = breakIterator;
        this.forwardsPartialTrie = charsTrie;
        this.backwardsTrie = charsTrie2;
    }

    private final void resetState() {
        this.text = UCharacterIterator.getInstance((CharacterIterator) this.delegate.getText().clone());
    }

    private final boolean breakExceptionAt(int i) {
        this.text.setIndex(i);
        this.backwardsTrie.reset();
        if (this.text.previousCodePoint() != 32) {
            this.text.nextCodePoint();
        }
        BytesTrie.Result resultNextForCodePoint = BytesTrie.Result.INTERMEDIATE_VALUE;
        int index = -1;
        int value = -1;
        while (true) {
            int iPreviousCodePoint = this.text.previousCodePoint();
            if (iPreviousCodePoint == -1) {
                break;
            }
            resultNextForCodePoint = this.backwardsTrie.nextForCodePoint(iPreviousCodePoint);
            if (!resultNextForCodePoint.hasNext()) {
                break;
            }
            if (resultNextForCodePoint.hasValue()) {
                index = this.text.getIndex();
                value = this.backwardsTrie.getValue();
            }
        }
        if (resultNextForCodePoint.matches()) {
            value = this.backwardsTrie.getValue();
            index = this.text.getIndex();
        }
        if (index >= 0) {
            if (value == 2) {
                return true;
            }
            if (value == 1 && this.forwardsPartialTrie != null) {
                this.forwardsPartialTrie.reset();
                BytesTrie.Result resultNextForCodePoint2 = BytesTrie.Result.INTERMEDIATE_VALUE;
                this.text.setIndex(index);
                do {
                    int iNextCodePoint = this.text.nextCodePoint();
                    if (iNextCodePoint == -1) {
                        break;
                    }
                    resultNextForCodePoint2 = this.forwardsPartialTrie.nextForCodePoint(iNextCodePoint);
                } while (resultNextForCodePoint2.hasNext());
                return resultNextForCodePoint2.matches();
            }
            return false;
        }
        return false;
    }

    private final int internalNext(int i) {
        if (i == -1 || this.backwardsTrie == null) {
            return i;
        }
        resetState();
        int length = this.text.getLength();
        while (i != -1 && i != length) {
            if (breakExceptionAt(i)) {
                i = this.delegate.next();
            } else {
                return i;
            }
        }
        return i;
    }

    private final int internalPrev(int i) {
        if (i == 0 || i == -1 || this.backwardsTrie == null) {
            return i;
        }
        resetState();
        while (i != -1 && i != 0) {
            if (breakExceptionAt(i)) {
                i = this.delegate.previous();
            } else {
                return i;
            }
        }
        return i;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SimpleFilteredSentenceBreakIterator simpleFilteredSentenceBreakIterator = (SimpleFilteredSentenceBreakIterator) obj;
        if (!this.delegate.equals(simpleFilteredSentenceBreakIterator.delegate) || !this.text.equals(simpleFilteredSentenceBreakIterator.text) || !this.backwardsTrie.equals(simpleFilteredSentenceBreakIterator.backwardsTrie) || !this.forwardsPartialTrie.equals(simpleFilteredSentenceBreakIterator.forwardsPartialTrie)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (this.forwardsPartialTrie.hashCode() * 39) + (this.backwardsTrie.hashCode() * 11) + this.delegate.hashCode();
    }

    @Override
    public Object clone() {
        return (SimpleFilteredSentenceBreakIterator) super.clone();
    }

    @Override
    public int first() {
        return this.delegate.first();
    }

    @Override
    public int preceding(int i) {
        return internalPrev(this.delegate.preceding(i));
    }

    @Override
    public int previous() {
        return internalPrev(this.delegate.previous());
    }

    @Override
    public int current() {
        return this.delegate.current();
    }

    @Override
    public boolean isBoundary(int i) {
        if (!this.delegate.isBoundary(i)) {
            return false;
        }
        if (this.backwardsTrie == null) {
            return true;
        }
        resetState();
        return !breakExceptionAt(i);
    }

    @Override
    public int next() {
        return internalNext(this.delegate.next());
    }

    @Override
    public int next(int i) {
        return internalNext(this.delegate.next(i));
    }

    @Override
    public int following(int i) {
        return internalNext(this.delegate.following(i));
    }

    @Override
    public int last() {
        return this.delegate.last();
    }

    @Override
    public CharacterIterator getText() {
        return this.delegate.getText();
    }

    @Override
    public void setText(CharacterIterator characterIterator) {
        this.delegate.setText(characterIterator);
    }

    public static class Builder extends FilteredBreakIteratorBuilder {
        static final int AddToForward = 2;
        static final int MATCH = 2;
        static final int PARTIAL = 1;
        static final int SuppressInReverse = 1;
        private HashSet<CharSequence> filterSet;

        public Builder(Locale locale) {
            this(ULocale.forLocale(locale));
        }

        public Builder(ULocale uLocale) {
            this.filterSet = new HashSet<>();
            ICUResourceBundle iCUResourceBundleFindWithFallback = ICUResourceBundle.getBundleInstance(ICUData.ICU_BRKITR_BASE_NAME, uLocale, ICUResourceBundle.OpenType.LOCALE_ROOT).findWithFallback("exceptions/SentenceBreak");
            if (iCUResourceBundleFindWithFallback != null) {
                int size = iCUResourceBundleFindWithFallback.getSize();
                for (int i = 0; i < size; i++) {
                    this.filterSet.add(((ICUResourceBundle) iCUResourceBundleFindWithFallback.get(i)).getString());
                }
            }
        }

        public Builder() {
            this.filterSet = new HashSet<>();
        }

        @Override
        public boolean suppressBreakAfter(CharSequence charSequence) {
            return this.filterSet.add(charSequence);
        }

        @Override
        public boolean unsuppressBreakAfter(CharSequence charSequence) {
            return this.filterSet.remove(charSequence);
        }

        @Override
        public BreakIterator wrapIteratorWithFilter(BreakIterator breakIterator) {
            CharsTrie charsTrieBuild;
            int i;
            if (this.filterSet.isEmpty()) {
                return breakIterator;
            }
            CharsTrieBuilder charsTrieBuilder = new CharsTrieBuilder();
            CharsTrieBuilder charsTrieBuilder2 = new CharsTrieBuilder();
            int size = this.filterSet.size();
            CharSequence[] charSequenceArr = new CharSequence[size];
            int[] iArr = new int[size];
            Iterator<CharSequence> it = this.filterSet.iterator();
            int i2 = 0;
            while (it.hasNext()) {
                charSequenceArr[i2] = it.next();
                iArr[i2] = 0;
                i2++;
            }
            int i3 = 0;
            for (int i4 = 0; i4 < size; i4++) {
                String string = charSequenceArr[i4].toString();
                int iIndexOf = string.indexOf(46);
                if (iIndexOf > -1 && (i = iIndexOf + 1) != string.length()) {
                    int i5 = -1;
                    for (int i6 = 0; i6 < size; i6++) {
                        if (i6 != i4 && string.regionMatches(0, charSequenceArr[i6].toString(), 0, i)) {
                            if (iArr[i6] == 0) {
                                iArr[i6] = 3;
                            } else if ((iArr[i6] & 1) != 0) {
                                i5 = i6;
                            }
                        }
                    }
                    if (i5 == -1 && iArr[i4] == 0) {
                        StringBuilder sb = new StringBuilder(string.substring(0, i));
                        sb.reverse();
                        charsTrieBuilder.add(sb, 1);
                        i3++;
                        iArr[i4] = 3;
                    }
                }
            }
            int i7 = 0;
            for (int i8 = 0; i8 < size; i8++) {
                String string2 = charSequenceArr[i8].toString();
                if (iArr[i8] == 0) {
                    charsTrieBuilder.add(new StringBuilder(string2).reverse(), 2);
                    i3++;
                } else {
                    charsTrieBuilder2.add(string2, 2);
                    i7++;
                }
            }
            CharsTrie charsTrieBuild2 = null;
            if (i3 > 0) {
                charsTrieBuild = charsTrieBuilder.build(StringTrieBuilder.Option.FAST);
            } else {
                charsTrieBuild = null;
            }
            if (i7 > 0) {
                charsTrieBuild2 = charsTrieBuilder2.build(StringTrieBuilder.Option.FAST);
            }
            return new SimpleFilteredSentenceBreakIterator(breakIterator, charsTrieBuild2, charsTrieBuild);
        }
    }
}
