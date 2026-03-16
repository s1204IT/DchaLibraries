package java.text;

import libcore.icu.NativeBreakIterator;

class RuleBasedBreakIterator extends BreakIterator {
    RuleBasedBreakIterator(NativeBreakIterator iterator) {
        super(iterator);
    }

    @Override
    public int current() {
        return this.wrapped.current();
    }

    @Override
    public int first() {
        return this.wrapped.first();
    }

    @Override
    public int following(int offset) {
        checkOffset(offset);
        return this.wrapped.following(offset);
    }

    private void checkOffset(int offset) {
        if (!this.wrapped.hasText()) {
            throw new IllegalArgumentException("BreakIterator has no text");
        }
        CharacterIterator it = this.wrapped.getText();
        if (offset < it.getBeginIndex() || offset > it.getEndIndex()) {
            String message = "Valid range is [" + it.getBeginIndex() + " " + it.getEndIndex() + "]";
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public CharacterIterator getText() {
        return this.wrapped.getText();
    }

    @Override
    public int last() {
        return this.wrapped.last();
    }

    @Override
    public int next() {
        return this.wrapped.next();
    }

    @Override
    public int next(int n) {
        return this.wrapped.next(n);
    }

    @Override
    public int previous() {
        return this.wrapped.previous();
    }

    @Override
    public void setText(CharacterIterator newText) {
        if (newText == null) {
            throw new NullPointerException("newText == null");
        }
        newText.current();
        this.wrapped.setText(newText);
    }

    @Override
    public boolean isBoundary(int offset) {
        checkOffset(offset);
        return this.wrapped.isBoundary(offset);
    }

    @Override
    public int preceding(int offset) {
        checkOffset(offset);
        return this.wrapped.preceding(offset);
    }

    public boolean equals(Object o) {
        if (o instanceof RuleBasedBreakIterator) {
            return this.wrapped.equals(((RuleBasedBreakIterator) o).wrapped);
        }
        return false;
    }

    public String toString() {
        return this.wrapped.toString();
    }

    public int hashCode() {
        return this.wrapped.hashCode();
    }

    @Override
    public Object clone() {
        RuleBasedBreakIterator cloned = (RuleBasedBreakIterator) super.clone();
        cloned.wrapped = (NativeBreakIterator) this.wrapped.clone();
        return cloned;
    }
}
