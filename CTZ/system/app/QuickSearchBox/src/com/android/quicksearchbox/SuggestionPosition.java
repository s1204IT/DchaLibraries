package com.android.quicksearchbox;

/* loaded from: classes.dex */
public class SuggestionPosition extends AbstractSuggestionWrapper {
    private final SuggestionCursor mCursor;
    private final int mPosition;

    public SuggestionPosition(SuggestionCursor suggestionCursor, int i) {
        this.mCursor = suggestionCursor;
        this.mPosition = i;
    }

    public SuggestionCursor getCursor() {
        return this.mCursor;
    }

    @Override // com.android.quicksearchbox.AbstractSuggestionWrapper
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
