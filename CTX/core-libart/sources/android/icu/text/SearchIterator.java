package android.icu.text;

import java.text.CharacterIterator;

public abstract class SearchIterator {
    public static final int DONE = -1;
    protected BreakIterator breakIterator;
    protected int matchLength;
    Search search_ = new Search();
    protected CharacterIterator targetText;

    public enum ElementComparisonType {
        STANDARD_ELEMENT_COMPARISON,
        PATTERN_BASE_WEIGHT_IS_WILDCARD,
        ANY_BASE_WEIGHT_IS_WILDCARD
    }

    public abstract int getIndex();

    protected abstract int handleNext(int i);

    protected abstract int handlePrevious(int i);

    final class Search {
        ElementComparisonType elementComparisonType_;
        BreakIterator internalBreakIter_;
        boolean isCanonicalMatch_;
        boolean isForwardSearching_;
        boolean isOverlap_;
        int matchedIndex_;
        boolean reset_;

        Search() {
        }

        CharacterIterator text() {
            return SearchIterator.this.targetText;
        }

        void setTarget(CharacterIterator characterIterator) {
            SearchIterator.this.targetText = characterIterator;
        }

        BreakIterator breakIter() {
            return SearchIterator.this.breakIterator;
        }

        void setBreakIter(BreakIterator breakIterator) {
            SearchIterator.this.breakIterator = breakIterator;
        }

        int matchedLength() {
            return SearchIterator.this.matchLength;
        }

        void setMatchedLength(int i) {
            SearchIterator.this.matchLength = i;
        }

        int beginIndex() {
            if (SearchIterator.this.targetText == null) {
                return 0;
            }
            return SearchIterator.this.targetText.getBeginIndex();
        }

        int endIndex() {
            if (SearchIterator.this.targetText == null) {
                return 0;
            }
            return SearchIterator.this.targetText.getEndIndex();
        }
    }

    public void setIndex(int i) {
        if (i < this.search_.beginIndex() || i > this.search_.endIndex()) {
            throw new IndexOutOfBoundsException("setIndex(int) expected position to be between " + this.search_.beginIndex() + " and " + this.search_.endIndex());
        }
        this.search_.reset_ = false;
        this.search_.setMatchedLength(0);
        this.search_.matchedIndex_ = -1;
    }

    public void setOverlapping(boolean z) {
        this.search_.isOverlap_ = z;
    }

    public void setBreakIterator(BreakIterator breakIterator) {
        this.search_.setBreakIter(breakIterator);
        if (this.search_.breakIter() != null && this.search_.text() != null) {
            this.search_.breakIter().setText((CharacterIterator) this.search_.text().clone());
        }
    }

    public void setTarget(CharacterIterator characterIterator) {
        if (characterIterator == null || characterIterator.getEndIndex() == characterIterator.getIndex()) {
            throw new IllegalArgumentException("Illegal null or empty text");
        }
        characterIterator.setIndex(characterIterator.getBeginIndex());
        this.search_.setTarget(characterIterator);
        this.search_.matchedIndex_ = -1;
        this.search_.setMatchedLength(0);
        this.search_.reset_ = true;
        this.search_.isForwardSearching_ = true;
        if (this.search_.breakIter() != null) {
            this.search_.breakIter().setText((CharacterIterator) characterIterator.clone());
        }
        if (this.search_.internalBreakIter_ != null) {
            this.search_.internalBreakIter_.setText((CharacterIterator) characterIterator.clone());
        }
    }

    public int getMatchStart() {
        return this.search_.matchedIndex_;
    }

    public int getMatchLength() {
        return this.search_.matchedLength();
    }

    public BreakIterator getBreakIterator() {
        return this.search_.breakIter();
    }

    public CharacterIterator getTarget() {
        return this.search_.text();
    }

    public String getMatchedText() {
        if (this.search_.matchedLength() > 0) {
            int iMatchedLength = this.search_.matchedIndex_ + this.search_.matchedLength();
            StringBuilder sb = new StringBuilder(this.search_.matchedLength());
            CharacterIterator characterIteratorText = this.search_.text();
            characterIteratorText.setIndex(this.search_.matchedIndex_);
            while (characterIteratorText.getIndex() < iMatchedLength) {
                sb.append(characterIteratorText.current());
                characterIteratorText.next();
            }
            characterIteratorText.setIndex(this.search_.matchedIndex_);
            return sb.toString();
        }
        return null;
    }

    public int next() {
        int index = getIndex();
        int i = this.search_.matchedIndex_;
        int iMatchedLength = this.search_.matchedLength();
        this.search_.reset_ = false;
        if (this.search_.isForwardSearching_) {
            int iEndIndex = this.search_.endIndex();
            if (index == iEndIndex || i == iEndIndex || (i != -1 && i + iMatchedLength >= iEndIndex)) {
                setMatchNotFound();
                return -1;
            }
        } else {
            this.search_.isForwardSearching_ = true;
            if (this.search_.matchedIndex_ != -1) {
                return i;
            }
        }
        if (iMatchedLength > 0) {
            if (this.search_.isOverlap_) {
                index++;
            } else {
                index += iMatchedLength;
            }
        }
        return handleNext(index);
    }

    public int previous() {
        int index;
        if (this.search_.reset_) {
            index = this.search_.endIndex();
            this.search_.isForwardSearching_ = false;
            this.search_.reset_ = false;
            setIndex(index);
        } else {
            index = getIndex();
        }
        int iMatchedLength = this.search_.matchedIndex_;
        if (this.search_.isForwardSearching_) {
            this.search_.isForwardSearching_ = false;
            if (iMatchedLength != -1) {
                return iMatchedLength;
            }
        } else {
            int iBeginIndex = this.search_.beginIndex();
            if (index == iBeginIndex || iMatchedLength == iBeginIndex) {
                setMatchNotFound();
                return -1;
            }
        }
        if (iMatchedLength != -1) {
            if (this.search_.isOverlap_) {
                iMatchedLength += this.search_.matchedLength() - 2;
            }
            return handlePrevious(iMatchedLength);
        }
        return handlePrevious(index);
    }

    public boolean isOverlapping() {
        return this.search_.isOverlap_;
    }

    public void reset() {
        setMatchNotFound();
        setIndex(this.search_.beginIndex());
        this.search_.isOverlap_ = false;
        this.search_.isCanonicalMatch_ = false;
        this.search_.elementComparisonType_ = ElementComparisonType.STANDARD_ELEMENT_COMPARISON;
        this.search_.isForwardSearching_ = true;
        this.search_.reset_ = true;
    }

    public final int first() {
        int iBeginIndex = this.search_.beginIndex();
        setIndex(iBeginIndex);
        return handleNext(iBeginIndex);
    }

    public final int following(int i) {
        setIndex(i);
        return handleNext(i);
    }

    public final int last() {
        int iEndIndex = this.search_.endIndex();
        setIndex(iEndIndex);
        return handlePrevious(iEndIndex);
    }

    public final int preceding(int i) {
        setIndex(i);
        return handlePrevious(i);
    }

    protected SearchIterator(CharacterIterator characterIterator, BreakIterator breakIterator) {
        if (characterIterator == null || characterIterator.getEndIndex() - characterIterator.getBeginIndex() == 0) {
            throw new IllegalArgumentException("Illegal argument target.  Argument can not be null or of length 0");
        }
        this.search_.setTarget(characterIterator);
        this.search_.setBreakIter(breakIterator);
        if (this.search_.breakIter() != null) {
            this.search_.breakIter().setText((CharacterIterator) characterIterator.clone());
        }
        this.search_.isOverlap_ = false;
        this.search_.isCanonicalMatch_ = false;
        this.search_.elementComparisonType_ = ElementComparisonType.STANDARD_ELEMENT_COMPARISON;
        this.search_.isForwardSearching_ = true;
        this.search_.reset_ = true;
        this.search_.matchedIndex_ = -1;
        this.search_.setMatchedLength(0);
    }

    protected void setMatchLength(int i) {
        this.search_.setMatchedLength(i);
    }

    @Deprecated
    protected void setMatchNotFound() {
        this.search_.matchedIndex_ = -1;
        this.search_.setMatchedLength(0);
    }

    public void setElementComparisonType(ElementComparisonType elementComparisonType) {
        this.search_.elementComparisonType_ = elementComparisonType;
    }

    public ElementComparisonType getElementComparisonType() {
        return this.search_.elementComparisonType_;
    }
}
