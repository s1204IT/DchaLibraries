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

        public int candidates(CharacterIterator characterIterator, DictionaryMatcher dictionaryMatcher, int i) {
            int index = characterIterator.getIndex();
            if (index != this.offset) {
                this.offset = index;
                this.prefix = dictionaryMatcher.matches(characterIterator, i - index, this.lengths, this.count, this.lengths.length);
                if (this.count[0] <= 0) {
                    characterIterator.setIndex(index);
                }
            }
            if (this.count[0] > 0) {
                characterIterator.setIndex(index + this.lengths[this.count[0] - 1]);
            }
            this.current = this.count[0] - 1;
            this.mark = this.current;
            return this.count[0];
        }

        public int acceptMarked(CharacterIterator characterIterator) {
            characterIterator.setIndex(this.offset + this.lengths[this.mark]);
            return this.lengths[this.mark];
        }

        public boolean backUp(CharacterIterator characterIterator) {
            if (this.current > 0) {
                int i = this.offset;
                int[] iArr = this.lengths;
                int i2 = this.current - 1;
                this.current = i2;
                characterIterator.setIndex(i + iArr[i2]);
                return true;
            }
            return false;
        }

        public int longestPrefix() {
            return this.prefix;
        }

        public void markCurrent() {
            this.mark = this.current;
        }
    }

    static class DequeI implements Cloneable {
        static final boolean $assertionsDisabled = false;
        private int[] data = new int[50];
        private int lastIdx = 4;
        private int firstIdx = 4;

        DequeI() {
        }

        public Object clone() throws CloneNotSupportedException {
            DequeI dequeI = (DequeI) super.clone();
            dequeI.data = (int[]) this.data.clone();
            return dequeI;
        }

        int size() {
            return this.firstIdx - this.lastIdx;
        }

        boolean isEmpty() {
            return size() == 0;
        }

        private void grow() {
            int[] iArr = new int[this.data.length * 2];
            System.arraycopy(this.data, 0, iArr, 0, this.data.length);
            this.data = iArr;
        }

        void offer(int i) {
            int[] iArr = this.data;
            int i2 = this.lastIdx - 1;
            this.lastIdx = i2;
            iArr[i2] = i;
        }

        void push(int i) {
            if (this.firstIdx >= this.data.length) {
                grow();
            }
            int[] iArr = this.data;
            int i2 = this.firstIdx;
            this.firstIdx = i2 + 1;
            iArr[i2] = i;
        }

        int pop() {
            int[] iArr = this.data;
            int i = this.firstIdx - 1;
            this.firstIdx = i;
            return iArr[i];
        }

        int peek() {
            return this.data[this.firstIdx - 1];
        }

        int peekLast() {
            return this.data[this.lastIdx];
        }

        int pollLast() {
            int[] iArr = this.data;
            int i = this.lastIdx;
            this.lastIdx = i + 1;
            return iArr[i];
        }

        boolean contains(int i) {
            for (int i2 = this.lastIdx; i2 < this.firstIdx; i2++) {
                if (this.data[i2] == i) {
                    return true;
                }
            }
            return false;
        }

        int elementAt(int i) {
            return this.data[this.lastIdx + i];
        }

        void removeAllElements() {
            this.firstIdx = 4;
            this.lastIdx = 4;
        }
    }

    public DictionaryBreakEngine(Integer... numArr) {
        for (Integer num : numArr) {
            this.fTypes.set(num.intValue());
        }
    }

    @Override
    public boolean handles(int i, int i2) {
        return this.fTypes.get(i2) && this.fSet.contains(i);
    }

    @Override
    public int findBreaks(CharacterIterator characterIterator, int i, int i2, int i3, DequeI dequeI) {
        int index;
        int index2 = characterIterator.getIndex();
        int iCurrent32 = CharacterIteration.current32(characterIterator);
        while (true) {
            index = characterIterator.getIndex();
            if (index >= i2 || !this.fSet.contains(iCurrent32)) {
                break;
            }
            CharacterIteration.next32(characterIterator);
            iCurrent32 = CharacterIteration.current32(characterIterator);
        }
        int iDivideUpDictionaryRange = divideUpDictionaryRange(characterIterator, index2, index, dequeI);
        characterIterator.setIndex(index);
        return iDivideUpDictionaryRange;
    }

    void setCharacters(UnicodeSet unicodeSet) {
        this.fSet = new UnicodeSet(unicodeSet);
        this.fSet.compact();
    }
}
