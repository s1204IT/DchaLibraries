package android.icu.text;

import android.icu.impl.number.Padder;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ULocale;
import java.text.CharacterIterator;

final class BreakTransliterator extends Transliterator {
    static final int LETTER_OR_MARK_MASK = 510;
    private BreakIterator bi;
    private int[] boundaries;
    private int boundaryCount;
    private String insertion;

    public BreakTransliterator(String str, UnicodeFilter unicodeFilter, BreakIterator breakIterator, String str2) {
        super(str, unicodeFilter);
        this.boundaries = new int[50];
        this.boundaryCount = 0;
        this.bi = breakIterator;
        this.insertion = str2;
    }

    public BreakTransliterator(String str, UnicodeFilter unicodeFilter) {
        this(str, unicodeFilter, null, Padder.FALLBACK_PADDING_STRING);
    }

    public String getInsertion() {
        return this.insertion;
    }

    public void setInsertion(String str) {
        this.insertion = str;
    }

    public BreakIterator getBreakIterator() {
        if (this.bi == null) {
            this.bi = BreakIterator.getWordInstance(new ULocale("th_TH"));
        }
        return this.bi;
    }

    public void setBreakIterator(BreakIterator breakIterator) {
        this.bi = breakIterator;
    }

    @Override
    protected synchronized void handleTransliterate(Replaceable replaceable, Transliterator.Position position, boolean z) {
        int i;
        int length = 0;
        this.boundaryCount = 0;
        getBreakIterator();
        this.bi.setText(new ReplaceableCharacterIterator(replaceable, position.start, position.limit, position.start));
        int iFirst = this.bi.first();
        while (iFirst != -1 && iFirst < position.limit) {
            if (iFirst != 0 && ((1 << UCharacter.getType(UTF16.charAt(replaceable, iFirst - 1))) & LETTER_OR_MARK_MASK) != 0 && ((1 << UCharacter.getType(UTF16.charAt(replaceable, iFirst))) & LETTER_OR_MARK_MASK) != 0) {
                if (this.boundaryCount >= this.boundaries.length) {
                    int[] iArr = new int[this.boundaries.length * 2];
                    System.arraycopy(this.boundaries, 0, iArr, 0, this.boundaries.length);
                    this.boundaries = iArr;
                }
                int[] iArr2 = this.boundaries;
                int i2 = this.boundaryCount;
                this.boundaryCount = i2 + 1;
                iArr2[i2] = iFirst;
            }
            iFirst = this.bi.next();
        }
        if (this.boundaryCount != 0) {
            length = this.boundaryCount * this.insertion.length();
            i = this.boundaries[this.boundaryCount - 1];
            while (this.boundaryCount > 0) {
                int[] iArr3 = this.boundaries;
                int i3 = this.boundaryCount - 1;
                this.boundaryCount = i3;
                int i4 = iArr3[i3];
                replaceable.replace(i4, i4, this.insertion);
            }
        } else {
            i = 0;
        }
        position.contextLimit += length;
        position.limit += length;
        position.start = z ? i + length : position.limit;
    }

    static void register() {
        Transliterator.registerInstance(new BreakTransliterator("Any-BreakInternal", null), false);
    }

    static final class ReplaceableCharacterIterator implements CharacterIterator {
        private int begin;
        private int end;
        private int pos;
        private Replaceable text;

        public ReplaceableCharacterIterator(Replaceable replaceable, int i, int i2, int i3) {
            if (replaceable == null) {
                throw new NullPointerException();
            }
            this.text = replaceable;
            if (i < 0 || i > i2 || i2 > replaceable.length()) {
                throw new IllegalArgumentException("Invalid substring range");
            }
            if (i3 < i || i3 > i2) {
                throw new IllegalArgumentException("Invalid position");
            }
            this.begin = i;
            this.end = i2;
            this.pos = i3;
        }

        public void setText(Replaceable replaceable) {
            if (replaceable == null) {
                throw new NullPointerException();
            }
            this.text = replaceable;
            this.begin = 0;
            this.end = replaceable.length();
            this.pos = 0;
        }

        @Override
        public char first() {
            this.pos = this.begin;
            return current();
        }

        @Override
        public char last() {
            if (this.end != this.begin) {
                this.pos = this.end - 1;
            } else {
                this.pos = this.end;
            }
            return current();
        }

        @Override
        public char setIndex(int i) {
            if (i < this.begin || i > this.end) {
                throw new IllegalArgumentException("Invalid index");
            }
            this.pos = i;
            return current();
        }

        @Override
        public char current() {
            if (this.pos >= this.begin && this.pos < this.end) {
                return this.text.charAt(this.pos);
            }
            return (char) 65535;
        }

        @Override
        public char next() {
            if (this.pos < this.end - 1) {
                this.pos++;
                return this.text.charAt(this.pos);
            }
            this.pos = this.end;
            return (char) 65535;
        }

        @Override
        public char previous() {
            if (this.pos > this.begin) {
                this.pos--;
                return this.text.charAt(this.pos);
            }
            return (char) 65535;
        }

        @Override
        public int getBeginIndex() {
            return this.begin;
        }

        @Override
        public int getEndIndex() {
            return this.end;
        }

        @Override
        public int getIndex() {
            return this.pos;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReplaceableCharacterIterator)) {
                return false;
            }
            ReplaceableCharacterIterator replaceableCharacterIterator = (ReplaceableCharacterIterator) obj;
            return hashCode() == replaceableCharacterIterator.hashCode() && this.text.equals(replaceableCharacterIterator.text) && this.pos == replaceableCharacterIterator.pos && this.begin == replaceableCharacterIterator.begin && this.end == replaceableCharacterIterator.end;
        }

        public int hashCode() {
            return ((this.text.hashCode() ^ this.pos) ^ this.begin) ^ this.end;
        }

        @Override
        public Object clone() {
            try {
                return (ReplaceableCharacterIterator) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new ICUCloneNotSupportedException();
            }
        }
    }

    @Override
    public void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        if (getFilterAsUnicodeSet(unicodeSet).size() != 0) {
            unicodeSet3.addAll(this.insertion);
        }
    }
}
