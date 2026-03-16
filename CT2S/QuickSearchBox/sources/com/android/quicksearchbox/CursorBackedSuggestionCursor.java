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

    @Override
    public abstract Source getSuggestionSource();

    public CursorBackedSuggestionCursor(String userQuery, Cursor cursor) {
        this.mUserQuery = userQuery;
        this.mCursor = cursor;
    }

    @Override
    public String getUserQuery() {
        return this.mUserQuery;
    }

    @Override
    public String getSuggestionLogType() {
        return getStringOrNull("suggest_log_type");
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
            } catch (RuntimeException ex) {
                Log.e("QSB.CursorBackedSuggestionCursor", "close() failed, ", ex);
            }
        }
    }

    protected void finalize() {
        if (!this.mClosed) {
            Log.e("QSB.CursorBackedSuggestionCursor", "LEAK! Finalized without being closed: " + toString());
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
        } catch (RuntimeException ex) {
            Log.e("QSB.CursorBackedSuggestionCursor", "getCount() failed, ", ex);
            return 0;
        }
    }

    @Override
    public void moveTo(int pos) {
        if (this.mClosed) {
            throw new IllegalStateException("moveTo(" + pos + ") after close()");
        }
        try {
            if (!this.mCursor.moveToPosition(pos)) {
                Log.e("QSB.CursorBackedSuggestionCursor", "moveToPosition(" + pos + ") failed, count=" + getCount());
            }
        } catch (RuntimeException ex) {
            Log.e("QSB.CursorBackedSuggestionCursor", "moveToPosition() failed, ", ex);
        }
    }

    public int getPosition() {
        if (this.mClosed) {
            throw new IllegalStateException("getPosition after close()");
        }
        try {
            return this.mCursor.getPosition();
        } catch (RuntimeException ex) {
            Log.e("QSB.CursorBackedSuggestionCursor", "getPosition() failed, ", ex);
            return -1;
        }
    }

    @Override
    public String getShortcutId() {
        return getStringOrNull("suggest_shortcut_id");
    }

    @Override
    public String getSuggestionFormat() {
        return getStringOrNull(this.mFormatCol);
    }

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
    public String getSuggestionIcon1() {
        return getStringOrNull(this.mIcon1Col);
    }

    @Override
    public String getSuggestionIcon2() {
        return getStringOrNull(this.mIcon2Col);
    }

    @Override
    public boolean isSpinnerWhileRefreshing() {
        return "true".equals(getStringOrNull(this.mRefreshSpinnerCol));
    }

    @Override
    public String getSuggestionIntentAction() {
        String action = getStringOrNull("suggest_intent_action");
        return action != null ? action : getSuggestionSource().getDefaultIntentAction();
    }

    @Override
    public String getSuggestionQuery() {
        return getStringOrNull("suggest_intent_query");
    }

    @Override
    public String getSuggestionIntentDataString() {
        String id;
        String data = getStringOrNull("suggest_intent_data");
        if (data == null) {
            data = getSuggestionSource().getDefaultIntentData();
        }
        if (data != null && (id = getStringOrNull("suggest_intent_data_id")) != null) {
            return data + "/" + Uri.encode(id);
        }
        return data;
    }

    @Override
    public String getSuggestionIntentExtraData() {
        return getStringOrNull("suggest_intent_extra_data");
    }

    @Override
    public boolean isWebSearchSuggestion() {
        return "android.intent.action.WEB_SEARCH".equals(getSuggestionIntentAction());
    }

    protected int getColumnIndex(String colName) {
        if (this.mCursor == null) {
            return -1;
        }
        try {
            return this.mCursor.getColumnIndex(colName);
        } catch (RuntimeException ex) {
            Log.e("QSB.CursorBackedSuggestionCursor", "getColumnIndex() failed, ", ex);
            return -1;
        }
    }

    protected String getStringOrNull(int col) {
        if (this.mCursor == null || col == -1) {
            return null;
        }
        try {
            return this.mCursor.getString(col);
        } catch (RuntimeException ex) {
            Log.e("QSB.CursorBackedSuggestionCursor", "getString() failed, ", ex);
            return null;
        }
    }

    protected String getStringOrNull(String colName) {
        int col = getColumnIndex(colName);
        return getStringOrNull(col);
    }

    public String toString() {
        return getClass().getSimpleName() + "[" + this.mUserQuery + "]";
    }
}
