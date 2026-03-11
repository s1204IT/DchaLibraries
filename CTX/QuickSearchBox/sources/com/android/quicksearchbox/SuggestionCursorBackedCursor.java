package com.android.quicksearchbox;

import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class SuggestionCursorBackedCursor extends AbstractCursor {
    public static final String[] COLUMNS = {"_id", "suggest_text_1", "suggest_text_2", "suggest_text_2_url", "suggest_icon_1", "suggest_icon_2", "suggest_intent_action", "suggest_intent_data", "suggest_intent_extra_data", "suggest_intent_query", "suggest_format", "suggest_shortcut_id", "suggest_spinner_while_refreshing"};
    private final SuggestionCursor mCursor;
    private ArrayList<String> mExtraColumns;

    public SuggestionCursorBackedCursor(SuggestionCursor suggestionCursor) {
        this.mCursor = suggestionCursor;
    }

    private Suggestion get() {
        this.mCursor.moveTo(getPosition());
        return this.mCursor;
    }

    private String getExtra(int i) {
        int length = COLUMNS.length;
        SuggestionExtras extras = get().getExtras();
        if (extras != null) {
            return extras.getExtra(this.mExtraColumns.get(i - length));
        }
        return null;
    }

    @Override
    public void close() {
        super.close();
        this.mCursor.close();
    }

    @Override
    public String[] getColumnNames() {
        Collection<String> extraColumns = this.mCursor.getExtraColumns();
        if (extraColumns == null) {
            return COLUMNS;
        }
        ArrayList arrayList = new ArrayList(COLUMNS.length + extraColumns.size());
        this.mExtraColumns = new ArrayList<>(extraColumns);
        arrayList.addAll(Arrays.asList(COLUMNS));
        arrayList.addAll(this.mExtraColumns);
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    @Override
    public int getCount() {
        return this.mCursor.getCount();
    }

    @Override
    public double getDouble(int i) {
        try {
            return Double.valueOf(getString(i)).doubleValue();
        } catch (NumberFormatException e) {
            return 0.0d;
        }
    }

    @Override
    public float getFloat(int i) {
        try {
            return Float.valueOf(getString(i)).floatValue();
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    @Override
    public int getInt(int i) {
        if (i == 0) {
            return getPosition();
        }
        try {
            return Integer.valueOf(getString(i)).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public long getLong(int i) {
        try {
            return Long.valueOf(getString(i)).longValue();
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public short getShort(int i) {
        try {
            return Short.valueOf(getString(i)).shortValue();
        } catch (NumberFormatException e) {
            return (short) 0;
        }
    }

    @Override
    public String getString(int i) {
        if (i >= COLUMNS.length) {
            return getExtra(i);
        }
        switch (i) {
            case 0:
                return String.valueOf(getPosition());
            case 1:
                return get().getSuggestionText1();
            case 2:
                return get().getSuggestionText2();
            case 3:
                return get().getSuggestionText2Url();
            case 4:
                return get().getSuggestionIcon1();
            case 5:
                return get().getSuggestionIcon2();
            case 6:
                return get().getSuggestionIntentAction();
            case 7:
                return get().getSuggestionIntentDataString();
            case 8:
                return get().getSuggestionIntentExtraData();
            case 9:
                return get().getSuggestionQuery();
            case 10:
                return get().getSuggestionFormat();
            case 11:
                return get().getShortcutId();
            case 12:
                return String.valueOf(get().isSpinnerWhileRefreshing());
            default:
                throw new CursorIndexOutOfBoundsException("Requested column " + i + " of " + COLUMNS.length);
        }
    }

    @Override
    public boolean isNull(int i) {
        return getString(i) == null;
    }
}
