package java.text;

class IcuIteratorWrapper extends BreakIterator {
    private android.icu.text.BreakIterator wrapped;

    IcuIteratorWrapper(android.icu.text.BreakIterator breakIterator) {
        this.wrapped = breakIterator;
    }

    @Override
    public Object clone() {
        IcuIteratorWrapper icuIteratorWrapper = (IcuIteratorWrapper) super.clone();
        icuIteratorWrapper.wrapped = (android.icu.text.BreakIterator) this.wrapped.clone();
        return icuIteratorWrapper;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IcuIteratorWrapper)) {
            return false;
        }
        return this.wrapped.equals(((IcuIteratorWrapper) obj).wrapped);
    }

    public String toString() {
        return this.wrapped.toString();
    }

    public int hashCode() {
        return this.wrapped.hashCode();
    }

    @Override
    public int first() {
        return this.wrapped.first();
    }

    @Override
    public int last() {
        return this.wrapped.last();
    }

    @Override
    public int next(int i) {
        return this.wrapped.next(i);
    }

    @Override
    public int next() {
        return this.wrapped.next();
    }

    @Override
    public int previous() {
        return this.wrapped.previous();
    }

    protected static final void checkOffset(int i, CharacterIterator characterIterator) {
        if (i < characterIterator.getBeginIndex() || i > characterIterator.getEndIndex()) {
            throw new IllegalArgumentException("offset out of bounds");
        }
    }

    @Override
    public int following(int i) {
        checkOffset(i, getText());
        return this.wrapped.following(i);
    }

    @Override
    public int preceding(int i) {
        checkOffset(i, getText());
        return this.wrapped.preceding(i);
    }

    @Override
    public boolean isBoundary(int i) {
        checkOffset(i, getText());
        return this.wrapped.isBoundary(i);
    }

    @Override
    public int current() {
        return this.wrapped.current();
    }

    @Override
    public CharacterIterator getText() {
        return this.wrapped.getText();
    }

    @Override
    public void setText(String str) {
        this.wrapped.setText(str);
    }

    @Override
    public void setText(CharacterIterator characterIterator) {
        characterIterator.current();
        this.wrapped.setText(characterIterator);
    }
}
