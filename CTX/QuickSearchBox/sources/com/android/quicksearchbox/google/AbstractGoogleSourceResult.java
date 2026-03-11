package com.android.quicksearchbox.google;

import android.content.ComponentName;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionExtras;
import java.util.Collection;

public abstract class AbstractGoogleSourceResult implements SourceResult {
    private int mPos = 0;
    private final Source mSource;
    private final String mUserQuery;

    public AbstractGoogleSourceResult(Source source, String str) {
        this.mSource = source;
        this.mUserQuery = str;
    }

    @Override
    public void close() {
    }

    @Override
    public Collection<String> getExtraColumns() {
        return null;
    }

    @Override
    public SuggestionExtras getExtras() {
        return null;
    }

    public int getPosition() {
        return this.mPos;
    }

    @Override
    public String getShortcutId() {
        return null;
    }

    @Override
    public String getSuggestionFormat() {
        return null;
    }

    @Override
    public String getSuggestionIcon1() {
        return String.valueOf(2130837566);
    }

    @Override
    public String getSuggestionIcon2() {
        return null;
    }

    @Override
    public String getSuggestionIntentAction() {
        return this.mSource.getDefaultIntentAction();
    }

    @Override
    public ComponentName getSuggestionIntentComponent() {
        return this.mSource.getIntentComponent();
    }

    @Override
    public String getSuggestionIntentDataString() {
        return null;
    }

    @Override
    public String getSuggestionIntentExtraData() {
        return null;
    }

    @Override
    public String getSuggestionLogType() {
        return null;
    }

    @Override
    public abstract String getSuggestionQuery();

    @Override
    public Source getSuggestionSource() {
        return this.mSource;
    }

    @Override
    public String getSuggestionText1() {
        return getSuggestionQuery();
    }

    @Override
    public String getSuggestionText2() {
        return null;
    }

    @Override
    public String getSuggestionText2Url() {
        return null;
    }

    @Override
    public String getUserQuery() {
        return this.mUserQuery;
    }

    @Override
    public boolean isHistorySuggestion() {
        return false;
    }

    @Override
    public boolean isSpinnerWhileRefreshing() {
        return false;
    }

    @Override
    public boolean isSuggestionShortcut() {
        return false;
    }

    @Override
    public boolean isWebSearchSuggestion() {
        return true;
    }

    @Override
    public void moveTo(int i) {
        this.mPos = i;
    }
}
