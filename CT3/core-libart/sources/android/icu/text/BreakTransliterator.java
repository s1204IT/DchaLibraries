package android.icu.text;

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

    public BreakTransliterator(String ID, UnicodeFilter filter, BreakIterator bi, String insertion) {
        super(ID, filter);
        this.boundaries = new int[50];
        this.boundaryCount = 0;
        this.bi = bi;
        this.insertion = insertion;
    }

    public BreakTransliterator(String ID, UnicodeFilter filter) {
        this(ID, filter, null, " ");
    }

    public String getInsertion() {
        return this.insertion;
    }

    public void setInsertion(String insertion) {
        this.insertion = insertion;
    }

    public BreakIterator getBreakIterator() {
        if (this.bi == null) {
            this.bi = BreakIterator.getWordInstance(new ULocale("th_TH"));
        }
        return this.bi;
    }

    public void setBreakIterator(BreakIterator bi) {
        this.bi = bi;
    }

    @Override
    protected synchronized void handleTransliterate(Replaceable text, Transliterator.Position pos, boolean incremental) {
        this.boundaryCount = 0;
        getBreakIterator();
        this.bi.setText(new ReplaceableCharacterIterator(text, pos.start, pos.limit, pos.start));
        int boundary = this.bi.first();
        while (boundary != -1 && boundary < pos.limit) {
            if (boundary != 0) {
                int cp = UTF16.charAt(text, boundary - 1);
                int type = UCharacter.getType(cp);
                if (((1 << type) & LETTER_OR_MARK_MASK) != 0) {
                    int cp2 = UTF16.charAt(text, boundary);
                    int type2 = UCharacter.getType(cp2);
                    if (((1 << type2) & LETTER_OR_MARK_MASK) != 0) {
                        if (this.boundaryCount >= this.boundaries.length) {
                            int[] temp = new int[this.boundaries.length * 2];
                            System.arraycopy(this.boundaries, 0, temp, 0, this.boundaries.length);
                            this.boundaries = temp;
                        }
                        int[] iArr = this.boundaries;
                        int i = this.boundaryCount;
                        this.boundaryCount = i + 1;
                        iArr[i] = boundary;
                    }
                }
            }
            boundary = this.bi.next();
        }
        int delta = 0;
        int lastBoundary = 0;
        if (this.boundaryCount != 0) {
            delta = this.boundaryCount * this.insertion.length();
            lastBoundary = this.boundaries[this.boundaryCount - 1];
            while (this.boundaryCount > 0) {
                int[] iArr2 = this.boundaries;
                int i2 = this.boundaryCount - 1;
                this.boundaryCount = i2;
                int boundary2 = iArr2[i2];
                text.replace(boundary2, boundary2, this.insertion);
            }
        }
        pos.contextLimit += delta;
        pos.limit += delta;
        pos.start = incremental ? lastBoundary + delta : pos.limit;
    }

    static void register() {
        Transliterator trans = new BreakTransliterator("Any-BreakInternal", null);
        Transliterator.registerInstance(trans, false);
    }

    static final class ReplaceableCharacterIterator implements CharacterIterator {
        private int begin;
        private int end;
        private int pos;
        private Replaceable text;

        public ReplaceableCharacterIterator(Replaceable text, int begin, int end, int pos) {
            if (text == null) {
                throw new NullPointerException();
            }
            this.text = text;
            if (begin < 0 || begin > end || end > text.length()) {
                throw new IllegalArgumentException("Invalid substring range");
            }
            if (pos < begin || pos > end) {
                throw new IllegalArgumentException("Invalid position");
            }
            this.begin = begin;
            this.end = end;
            this.pos = pos;
        }

        public void setText(Replaceable text) {
            if (text == null) {
                throw new NullPointerException();
            }
            this.text = text;
            this.begin = 0;
            this.end = text.length();
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
        public char setIndex(int p) {
            if (p < this.begin || p > this.end) {
                throw new IllegalArgumentException("Invalid index");
            }
            this.pos = p;
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
            return (obj instanceof ReplaceableCharacterIterator) && hashCode() == obj.hashCode() && this.text.equals(obj.text) && this.pos == obj.pos && this.begin == obj.begin && this.end == obj.end;
        }

        public int hashCode() {
            return ((this.text.hashCode() ^ this.pos) ^ this.begin) ^ this.end;
        }

        @Override
        public Object clone() {
            try {
                ReplaceableCharacterIterator other = (ReplaceableCharacterIterator) super.clone();
                return other;
            } catch (CloneNotSupportedException e) {
                throw new ICUCloneNotSupportedException();
            }
        }
    }

    @Override
    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        UnicodeSet myFilter = getFilterAsUnicodeSet(inputFilter);
        if (myFilter.size() == 0) {
            return;
        }
        targetSet.addAll(this.insertion);
    }
}
