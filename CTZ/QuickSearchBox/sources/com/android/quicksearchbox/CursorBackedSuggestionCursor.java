package com.android.quicksearchbox;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public abstract class CursorBackedSuggestionCursor implements SuggestionCursor {
    protected final Cursor mCursor;
    private final String mUserQuery;
    private boolean mClosed = false;
    private final int mFormatCol = getColumnIndex("suggest_format");
    private final int mText1Col = getColumnIndex("suggest_text_1");
    private final int mText2Col = getColumnIndex("suggest_text_2");
    private final int mText2UrlCol = getColumnIndex("suggest_text_2_url");
    private final int mIcon1Col = getColumnIndex("suggest_icon_1");
    private final int mIcon2Col = getColumnIndex("suggest_icon_2");
    private final int mRefreshSpinnerCol = getColumnIndex("suggest_spinner_while_refreshing");

    public CursorBackedSuggestionCursor(String str, Cursor cursor) {
        this.mUserQuery = str;
        this.mCursor = cursor;
    }

    @Override
    public void close() {
        if (this.mClosed) {
            throw new IllegalStateException("Double close()");
        }
        this.mClosed = true;
        if (this.mCursor != null) {
            try {
                this.mCursor.close();
            } catch (RuntimeException e) {
                Log.e("QSB.CursorBackedSuggestionCursor", "close() failed, ", e);
            }
        }
    }

    protected void finalize() {
        if (this.mClosed) {
            return;
        }
        Log.e("QSB.CursorBackedSuggestionCursor", "LEAK! Finalized without being closed: " + toString());
    }

    protected int getColumnIndex(String str) {
        if (this.mCursor == null) {
            return -1;
        }
        try {
            return this.mCursor.getColumnIndex(str);
        } catch (RuntimeException e) {
            Log.e("QSB.CursorBackedSuggestionCursor", "getColumnIndex() failed, ", e);
            return -1;
        }
    }

    @Override
    public int getCount() {
        if (this.mClosed) {
            throw new IllegalStateException("getCount() after close()");
        }
        if (this.mCursor == null) {
            return 0;
        }
        try {
            return this.mCursor.getCount();
        } catch (RuntimeException e) {
            Log.e("QSB.CursorBackedSuggestionCursor", "getCount() failed, ", e);
            return 0;
        }
    }

    public int getPosition() {
        if (this.mClosed) {
            throw new IllegalStateException("getPosition after close()");
        }
        try {
            return this.mCursor.getPosition();
        } catch (RuntimeException e) {
            Log.e("QSB.CursorBackedSuggestionCursor", "getPosition() failed, ", e);
            return -1;
        }
    }

    @Override
    public String getShortcutId() {
        return getStringOrNull("suggest_shortcut_id");
    }

    protected String getStringOrNull(int i) {
        if (this.mCursor == null || i == -1) {
            return null;
        }
        try {
            return this.mCursor.getString(i);
        } catch (RuntimeException e) {
            Log.e("QSB.CursorBackedSuggestionCursor", "getString() failed, ", e);
            return null;
        }
    }

    protected String getStringOrNull(String str) {
        return getStringOrNull(getColumnIndex(str));
    }

    @Override
    public String getSuggestionFormat() {
        return getStringOrNull(this.mFormatCol);
    }

    @Override
    public String getSuggestionIcon1() {
        return getStringOrNull(this.mIcon1Col);
    }

    @Override
    public String getSuggestionIcon2() {
        return getStringOrNull(this.mIcon2Col);
    }

    @Override
    public String getSuggestionIntentAction() {
        String stringOrNull = getStringOrNull("suggest_intent_action");
        return stringOrNull != null ? stringOrNull : getSuggestionSource().getDefaultIntentAction();
    }

    @Override
    public String getSuggestionIntentDataString() {
        String stringOrNull;
        String stringOrNull2 = getStringOrNull("suggest_intent_data");
        if (stringOrNull2 == null) {
            stringOrNull2 = getSuggestionSource().getDefaultIntentData();
        }
        if (stringOrNull2 == null || (stringOrNull = getStringOrNull("suggest_intent_data_id")) == null) {
            return stringOrNull2;
        }
        return stringOrNull2 + "/" + Uri.encode(stringOrNull);
    }

    @Override
    public String getSuggestionIntentExtraData() {
        return getStringOrNull("suggest_intent_extra_data");
    }

    @Override
    public String getSuggestionLogType() {
        return getStringOrNull("suggest_log_type");
    }

    @Override
    public String getSuggestionQuery() {
        return getStringOrNull("suggest_intent_query");
    }

    @Override
    public abstract Source getSuggestionSource();

    @Override
    public String getSuggestionText1() {
        return getStringOrNull(this.mText1Col);
    }

    @Override
    public String getSuggestionText2() {
        return getStringOrNull(this.mText2Col);
    }

    @Override
    public String getSuggestionText2Url() {
        return getStringOrNull(this.mText2UrlCol);
    }

    @Override
    public String getUserQuery() {
        return this.mUserQuery;
    }

    @Override
    public boolean isSpinnerWhileRefreshing() {
        return "true".equals(getStringOrNull(this.mRefreshSpinnerCol));
    }

    @Override
    public boolean isWebSearchSuggestion() {
        return "android.intent.action.WEB_SEARCH".equals(getSuggestionIntentAction());
    }

    @Override
    public void moveTo(int i) {
        if (this.mClosed) {
            throw new IllegalStateException("moveTo(" + i + ") after close()");
        }
        try {
            if (this.mCursor.moveToPosition(i)) {
                return;
            }
            Log.e("QSB.CursorBackedSuggestionCursor", "moveToPosition(" + i + ") failed, count=" + getCount());
        } catch (RuntimeException e) {
            Log.e("QSB.CursorBackedSuggestionCursor", "moveToPosition() failed, ", e);
        }
    }

    public String toString() {
        return getClass().getSimpleName() + "[" + this.mUserQuery + "]";
    }
}
