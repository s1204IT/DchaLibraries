package com.android.quicksearchbox.google;

import android.content.ComponentName;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionExtras;
import java.util.Collection;

public abstract class AbstractGoogleSourceResult implements SourceResult {
    private int mPos = 0;
    private final Source mSource;
    private final String mUserQuery;

    @Override
    public abstract String getSuggestionQuery();

    public AbstractGoogleSourceResult(Source source, String userQuery) {
        this.mSource = source;
        this.mUserQuery = userQuery;
    }

    @Override
    public void close() {
    }

    public int getPosition() {
        return this.mPos;
    }

    @Override
    public String getUserQuery() {
        return this.mUserQuery;
    }

    @Override
    public void moveTo(int pos) {
        this.mPos = pos;
    }

    @Override
    public String getSuggestionText1() {
        return getSuggestionQuery();
    }

    @Override
    public Source getSuggestionSource() {
        return this.mSource;
    }

    @Override
    public boolean isSuggestionShortcut() {
        return false;
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
        return String.valueOf(R.drawable.magnifying_glass);
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
    public String getSuggestionText2() {
        return null;
    }

    @Override
    public String getSuggestionText2Url() {
        return null;
    }

    @Override
    public boolean isSpinnerWhileRefreshing() {
        return false;
    }

    @Override
    public boolean isWebSearchSuggestion() {
        return true;
    }

    @Override
    public boolean isHistorySuggestion() {
        return false;
    }

    @Override
    public SuggestionExtras getExtras() {
        return null;
    }

    @Override
    public Collection<String> getExtraColumns() {
        return null;
    }
}
