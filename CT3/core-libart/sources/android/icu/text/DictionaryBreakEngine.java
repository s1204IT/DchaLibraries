package android.icu.text;

import android.icu.impl.CharacterIteration;
import java.text.CharacterIterator;
import java.util.BitSet;

abstract class DictionaryBreakEngine implements LanguageBreakEngine {
    UnicodeSet fSet = new UnicodeSet();
    private BitSet fTypes = new BitSet(32);

    abstract int divideUpDictionaryRange(CharacterIterator characterIterator, int i, int i2, DequeI dequeI);

    static class PossibleWord {
        private static final int POSSIBLE_WORD_LIST_MAX = 20;
        private int current;
        private int mark;
        private int prefix;
        private int[] lengths = new int[20];
        private int[] count = new int[1];
        private int offset = -1;

        public int candidates(CharacterIterator fIter, DictionaryMatcher dict, int rangeEnd) {
            int start = fIter.getIndex();
            if (start != this.offset) {
                this.offset = start;
                this.prefix = dict.matches(fIter, rangeEnd - start, this.lengths, this.count, this.lengths.length);
                if (this.count[0] <= 0) {
                    fIter.setIndex(start);
                }
            }
            if (this.count[0] > 0) {
                fIter.setIndex(this.lengths[this.count[0] - 1] + start);
            }
            this.current = this.count[0] - 1;
            this.mark = this.current;
            return this.count[0];
        }

        public int acceptMarked(CharacterIterator fIter) {
            fIter.setIndex(this.offset + this.lengths[this.mark]);
            return this.lengths[this.mark];
        }

        public boolean backUp(CharacterIterator fIter) {
            if (this.current <= 0) {
                return false;
            }
            int i = this.offset;
            int[] iArr = this.lengths;
            int i2 = this.current - 1;
            this.current = i2;
            fIter.setIndex(i + iArr[i2]);
            return true;
        }

        public int longestPrefix() {
            return this.prefix;
        }

        public void markCurrent() {
            this.mark = this.current;
        }
    }

    static class DequeI {

        static final boolean f70assertionsDisabled;
        private int[] data = new int[50];
        private int lastIdx = 4;
        private int firstIdx = 4;

        static {
            f70assertionsDisabled = !DequeI.class.desiredAssertionStatus();
        }

        DequeI() {
        }

        int size() {
            return this.firstIdx - this.lastIdx;
        }

        boolean isEmpty() {
            return size() == 0;
        }

        private void grow() {
            int[] newData = new int[this.data.length * 2];
            System.arraycopy(this.data, 0, newData, 0, this.data.length);
            this.data = newData;
        }

        void offer(int v) {
            if (!f70assertionsDisabled) {
                if (!(this.lastIdx > 0)) {
                    throw new AssertionError();
                }
            }
            int[] iArr = this.data;
            int i = this.lastIdx - 1;
            this.lastIdx = i;
            iArr[i] = v;
        }

        void push(int v) {
            if (this.firstIdx >= this.data.length) {
                grow();
            }
            int[] iArr = this.data;
            int i = this.firstIdx;
            this.firstIdx = i + 1;
            iArr[i] = v;
        }

        int pop() {
            if (!f70assertionsDisabled) {
                if (!(size() > 0)) {
                    throw new AssertionError();
                }
            }
            int[] iArr = this.data;
            int i = this.firstIdx - 1;
            this.firstIdx = i;
            return iArr[i];
        }

        int peek() {
            if (!f70assertionsDisabled) {
                if (!(size() > 0)) {
                    throw new AssertionError();
                }
            }
            return this.data[this.firstIdx - 1];
        }

        int peekLast() {
            if (!f70assertionsDisabled) {
                if (!(size() > 0)) {
                    throw new AssertionError();
                }
            }
            return this.data[this.lastIdx];
        }

        int pollLast() {
            if (!f70assertionsDisabled) {
                if (!(size() > 0)) {
                    throw new AssertionError();
                }
            }
            int[] iArr = this.data;
            int i = this.lastIdx;
            this.lastIdx = i + 1;
            return iArr[i];
        }

        boolean contains(int v) {
            for (int i = this.lastIdx; i < this.firstIdx; i++) {
                if (this.data[i] == v) {
                    return true;
                }
            }
            return false;
        }
    }

    public DictionaryBreakEngine(Integer... breakTypes) {
        for (Integer type : breakTypes) {
            this.fTypes.set(type.intValue());
        }
    }

    @Override
    public boolean handles(int c, int breakType) {
        if (this.fTypes.get(breakType)) {
            return this.fSet.contains(c);
        }
        return false;
    }

    @Override
    public int findBreaks(CharacterIterator text, int startPos, int endPos, boolean reverse, int breakType, DequeI foundBreaks) {
        int current;
        int rangeStart;
        int rangeEnd;
        int start = text.getIndex();
        int c = CharacterIteration.current32(text);
        if (reverse) {
            boolean isDict = this.fSet.contains(c);
            while (true) {
                current = text.getIndex();
                if (current <= startPos || !isDict) {
                    break;
                }
                isDict = this.fSet.contains(CharacterIteration.previous32(text));
            }
            if (current < startPos) {
                rangeStart = startPos;
            } else {
                rangeStart = current + (isDict ? 0 : 1);
            }
            rangeEnd = start + 1;
        } else {
            while (true) {
                current = text.getIndex();
                if (current >= endPos || !this.fSet.contains(c)) {
                    break;
                }
                CharacterIteration.next32(text);
                c = CharacterIteration.current32(text);
            }
            rangeStart = start;
            rangeEnd = current;
        }
        int result = divideUpDictionaryRange(text, rangeStart, rangeEnd, foundBreaks);
        text.setIndex(current);
        return result;
    }

    void setCharacters(UnicodeSet set) {
        this.fSet = new UnicodeSet(set);
        this.fSet.compact();
    }
}
