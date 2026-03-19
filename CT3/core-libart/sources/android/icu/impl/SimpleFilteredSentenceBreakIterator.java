package android.icu.impl;

import android.icu.text.BreakIterator;
import android.icu.text.FilteredBreakIteratorBuilder;
import android.icu.text.UCharacterIterator;
import android.icu.util.BytesTrie;
import android.icu.util.CharsTrie;
import android.icu.util.CharsTrieBuilder;
import android.icu.util.StringTrieBuilder;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.text.CharacterIterator;
import java.util.HashSet;

public class SimpleFilteredSentenceBreakIterator extends BreakIterator {
    private CharsTrie backwardsTrie;
    private BreakIterator delegate;
    private CharsTrie forwardsPartialTrie;
    private UCharacterIterator text;

    public SimpleFilteredSentenceBreakIterator(BreakIterator adoptBreakIterator, CharsTrie forwardsPartialTrie, CharsTrie backwardsTrie) {
        this.delegate = adoptBreakIterator;
        this.forwardsPartialTrie = forwardsPartialTrie;
        this.backwardsTrie = backwardsTrie;
    }

    @Override
    public int next() {
        int n = this.delegate.next();
        if (n == -1 || this.backwardsTrie == null) {
            return n;
        }
        this.text = UCharacterIterator.getInstance((CharacterIterator) this.delegate.getText().clone());
        do {
            this.text.setIndex(n);
            this.backwardsTrie.reset();
            if (this.text.previousCodePoint() != 32) {
                this.text.nextCodePoint();
            }
            BytesTrie.Result r = BytesTrie.Result.INTERMEDIATE_VALUE;
            int bestPosn = -1;
            int bestValue = -1;
            while (true) {
                int uch = this.text.previousCodePoint();
                if (uch == -1) {
                    break;
                }
                r = this.backwardsTrie.nextForCodePoint(uch);
                if (!r.hasNext()) {
                    break;
                }
                if (r.hasValue()) {
                    bestPosn = this.text.getIndex();
                    bestValue = this.backwardsTrie.getValue();
                }
            }
            if (r.matches()) {
                bestValue = this.backwardsTrie.getValue();
                bestPosn = this.text.getIndex();
            }
            if (bestPosn >= 0) {
                if (bestValue == 2) {
                    n = this.delegate.next();
                    if (n == -1) {
                        return n;
                    }
                } else if (bestValue == 1 && this.forwardsPartialTrie != null) {
                    this.forwardsPartialTrie.reset();
                    BytesTrie.Result rfwd = BytesTrie.Result.INTERMEDIATE_VALUE;
                    this.text.setIndex(bestPosn);
                    do {
                        int uch2 = this.text.nextCodePoint();
                        if (uch2 == -1) {
                            break;
                        }
                        rfwd = this.forwardsPartialTrie.nextForCodePoint(uch2);
                    } while (rfwd.hasNext());
                    if (rfwd.matches()) {
                        n = this.delegate.next();
                        if (n == -1) {
                            return n;
                        }
                    } else {
                        return n;
                    }
                } else {
                    return n;
                }
            } else {
                return n;
            }
        } while (n != -1);
        return n;
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
        SimpleFilteredSentenceBreakIterator other = (SimpleFilteredSentenceBreakIterator) obj;
        if (this.delegate.equals(other.delegate) && this.text.equals(other.text) && this.backwardsTrie.equals(other.backwardsTrie)) {
            return this.forwardsPartialTrie.equals(other.forwardsPartialTrie);
        }
        return false;
    }

    public int hashCode() {
        return (this.forwardsPartialTrie.hashCode() * 39) + (this.backwardsTrie.hashCode() * 11) + this.delegate.hashCode();
    }

    @Override
    public Object clone() {
        SimpleFilteredSentenceBreakIterator other = (SimpleFilteredSentenceBreakIterator) super.clone();
        return other;
    }

    @Override
    public int first() {
        return this.delegate.first();
    }

    @Override
    public int last() {
        return this.delegate.last();
    }

    @Override
    public int next(int n) {
        throw new UnsupportedOperationException("next(int) is not yet implemented");
    }

    @Override
    public int previous() {
        throw new UnsupportedOperationException("previous() is not yet implemented");
    }

    @Override
    public int following(int offset) {
        throw new UnsupportedOperationException("following(int) is not yet implemented");
    }

    @Override
    public int current() {
        return this.delegate.current();
    }

    @Override
    public int preceding(int offset) {
        throw new UnsupportedOperationException("preceding(int) is not yet implemented");
    }

    @Override
    public CharacterIterator getText() {
        return this.delegate.getText();
    }

    @Override
    public void setText(CharacterIterator newText) {
        this.delegate.setText(newText);
    }

    public static class Builder extends FilteredBreakIteratorBuilder {
        static final int AddToForward = 2;
        static final int MATCH = 2;
        static final int PARTIAL = 1;
        static final int SuppressInReverse = 1;
        private HashSet<String> filterSet;

        public Builder(ULocale loc) {
            ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/brkitr", loc);
            ICUResourceBundle exceptions = rb.findWithFallback("exceptions");
            ICUResourceBundle breaks = exceptions.findWithFallback("SentenceBreak");
            this.filterSet = new HashSet<>();
            if (breaks == null) {
                return;
            }
            int size = breaks.getSize();
            for (int index = 0; index < size; index++) {
                ICUResourceBundle b = (ICUResourceBundle) breaks.get(index);
                String br = b.getString();
                this.filterSet.add(br);
            }
        }

        public Builder() {
            this.filterSet = new HashSet<>();
        }

        @Override
        public boolean suppressBreakAfter(String str) {
            if (this.filterSet == null) {
                this.filterSet = new HashSet<>();
            }
            return this.filterSet.add(str);
        }

        @Override
        public boolean unsuppressBreakAfter(String str) {
            if (this.filterSet == null) {
                return false;
            }
            return this.filterSet.remove(str);
        }

        @Override
        public BreakIterator build(BreakIterator adoptBreakIterator) {
            CharsTrieBuilder builder = new CharsTrieBuilder();
            CharsTrieBuilder builder2 = new CharsTrieBuilder();
            int revCount = 0;
            int fwdCount = 0;
            int subCount = this.filterSet.size();
            String[] ustrs = new String[subCount];
            int[] partials = new int[subCount];
            CharsTrie backwardsTrie = null;
            CharsTrie forwardsPartialTrie = null;
            int i = 0;
            for (String s : this.filterSet) {
                ustrs[i] = s;
                partials[i] = 0;
                i++;
            }
            for (int i2 = 0; i2 < subCount; i2++) {
                int nn = ustrs[i2].indexOf(46);
                if (nn > -1 && nn + 1 != ustrs[i2].length()) {
                    int sameAs = -1;
                    for (int j = 0; j < subCount; j++) {
                        if (j != i2 && ustrs[i2].regionMatches(0, ustrs[j], 0, nn + 1)) {
                            if (partials[j] == 0) {
                                partials[j] = 3;
                            } else if ((partials[j] & 1) != 0) {
                                sameAs = j;
                            }
                        }
                    }
                    if (sameAs == -1 && partials[i2] == 0) {
                        StringBuilder prefix = new StringBuilder(ustrs[i2].substring(0, nn + 1));
                        prefix.reverse();
                        builder.add(prefix, 1);
                        revCount++;
                        partials[i2] = 3;
                    }
                }
            }
            for (int i3 = 0; i3 < subCount; i3++) {
                if (partials[i3] == 0) {
                    StringBuilder reversed = new StringBuilder(ustrs[i3]).reverse();
                    builder.add(reversed, 2);
                    revCount++;
                } else {
                    builder2.add(ustrs[i3], 2);
                    fwdCount++;
                }
            }
            if (revCount > 0) {
                backwardsTrie = builder.build(StringTrieBuilder.Option.FAST);
            }
            if (fwdCount > 0) {
                forwardsPartialTrie = builder2.build(StringTrieBuilder.Option.FAST);
            }
            return new SimpleFilteredSentenceBreakIterator(adoptBreakIterator, forwardsPartialTrie, backwardsTrie);
        }
    }
}
