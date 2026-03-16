package com.android.quicksearchbox;

public class SuggestionPosition extends AbstractSuggestionWrapper {
    private final SuggestionCursor mCursor;
    private final int mPosition;

    public SuggestionPosition(SuggestionCursor cursor, int suggestionPos) {
        this.mCursor = cursor;
        this.mPosition = suggestionPos;
    }

    public SuggestionCursor getCursor() {
        return this.mCursor;
    }

    @Override
    protected Suggestion current() {
        this.mCursor.moveTo(this.mPosition);
        return this.mCursor;
    }

    public int getPosition() {
        return this.mPosition;
    }

    public String toString() {
        return this.mCursor + ":" + this.mPosition;
    }
}
