package com.android.quicksearchbox.google;

import android.content.ComponentName;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionExtras;
import java.util.Collection;

/* loaded from: classes.dex */
public abstract class AbstractGoogleSourceResult implements SourceResult {
    private int mPos = 0;
    private final Source mSource;
    private final String mUserQuery;

    @Override // com.android.quicksearchbox.Suggestion
    public abstract String getSuggestionQuery();

    public AbstractGoogleSourceResult(Source source, String str) {
        this.mSource = source;
        this.mUserQuery = str;
    }

    @Override // com.android.quicksearchbox.SuggestionCursor, com.android.quicksearchbox.util.QuietlyCloseable, java.io.Closeable, java.lang.AutoCloseable
    public void close() {
    }

    public int getPosition() {
        return this.mPos;
    }

    @Override // com.android.quicksearchbox.SuggestionCursor
    public String getUserQuery() {
        return this.mUserQuery;
    }

    @Override // com.android.quicksearchbox.SuggestionCursor
    public void moveTo(int i) {
        this.mPos = i;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionText1() {
        return getSuggestionQuery();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public Source getSuggestionSource() {
        return this.mSource;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isSuggestionShortcut() {
        return false;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getShortcutId() {
        return null;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionFormat() {
        return null;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIcon1() {
        return String.valueOf(R.drawable.magnifying_glass);
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIcon2() {
        return null;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIntentAction() {
        return this.mSource.getDefaultIntentAction();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public ComponentName getSuggestionIntentComponent() {
        return this.mSource.getIntentComponent();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIntentDataString() {
        return null;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIntentExtraData() {
        return null;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionLogType() {
        return null;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionText2() {
        return null;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionText2Url() {
        return null;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isSpinnerWhileRefreshing() {
        return false;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isWebSearchSuggestion() {
        return true;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isHistorySuggestion() {
        return false;
    }

    @Override // com.android.quicksearchbox.Suggestion
    public SuggestionExtras getExtras() {
        return null;
    }

    @Override // com.android.quicksearchbox.SuggestionCursor
    public Collection<String> getExtraColumns() {
        return null;
    }
}
