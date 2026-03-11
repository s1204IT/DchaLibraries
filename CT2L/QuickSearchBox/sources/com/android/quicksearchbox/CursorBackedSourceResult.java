package com.android.quicksearchbox;

import android.content.ComponentName;
import android.database.Cursor;
import com.android.quicksearchbox.google.GoogleSource;
import java.util.Collection;

public class CursorBackedSourceResult extends CursorBackedSuggestionCursor implements SourceResult {
    private final GoogleSource mSource;

    public CursorBackedSourceResult(GoogleSource source, String userQuery) {
        this(source, userQuery, null);
    }

    public CursorBackedSourceResult(GoogleSource source, String userQuery, Cursor cursor) {
        super(userQuery, cursor);
        this.mSource = source;
    }

    @Override
    public GoogleSource getSuggestionSource() {
        return this.mSource;
    }

    @Override
    public ComponentName getSuggestionIntentComponent() {
        return this.mSource.getIntentComponent();
    }

    @Override
    public boolean isSuggestionShortcut() {
        return false;
    }

    @Override
    public boolean isHistorySuggestion() {
        return false;
    }

    @Override
    public String toString() {
        return this.mSource + "[" + getUserQuery() + "]";
    }

    @Override
    public SuggestionExtras getExtras() {
        if (this.mCursor == null) {
            return null;
        }
        return CursorBackedSuggestionExtras.createExtrasIfNecessary(this.mCursor, getPosition());
    }

    @Override
    public Collection<String> getExtraColumns() {
        if (this.mCursor == null) {
            return null;
        }
        return CursorBackedSuggestionExtras.getExtraColumns(this.mCursor);
    }
}
