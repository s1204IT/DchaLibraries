package com.android.quicksearchbox;

import android.content.ComponentName;
import android.database.Cursor;
import com.android.quicksearchbox.google.GoogleSource;
import java.util.Collection;

/* loaded from: classes.dex */
public class CursorBackedSourceResult extends CursorBackedSuggestionCursor implements SourceResult {
    private final GoogleSource mSource;

    public CursorBackedSourceResult(GoogleSource googleSource, String str) {
        this(googleSource, str, null);
    }

    public CursorBackedSourceResult(GoogleSource googleSource, String str, Cursor cursor) {
        super(str, cursor);
        this.mSource = googleSource;
    }

    /* JADX DEBUG: Method merged with bridge method: getSuggestionSource()Lcom/android/quicksearchbox/Source; */
    @Override // com.android.quicksearchbox.CursorBackedSuggestionCursor, com.android.quicksearchbox.Suggestion
    public GoogleSource getSuggestionSource() {
        return this.mSource;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public ComponentName getSuggestionIntentComponent() {
        return this.mSource.getIntentComponent();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isSuggestionShortcut() {
        return false;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isHistorySuggestion() {
        return false;
    }

    @Override // com.android.quicksearchbox.CursorBackedSuggestionCursor
    public String toString() {
        return this.mSource + "[" + getUserQuery() + "]";
    }

    @Override // com.android.quicksearchbox.Suggestion
    public SuggestionExtras getExtras() {
        if (this.mCursor == null) {
            return null;
        }
        return CursorBackedSuggestionExtras.createExtrasIfNecessary(this.mCursor, getPosition());
    }

    @Override // com.android.quicksearchbox.SuggestionCursor
    public Collection<String> getExtraColumns() {
        if (this.mCursor == null) {
            return null;
        }
        return CursorBackedSuggestionExtras.getExtraColumns(this.mCursor);
    }
}
