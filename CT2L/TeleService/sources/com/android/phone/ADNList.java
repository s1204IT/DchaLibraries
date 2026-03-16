package com.android.phone;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ADNList extends ListActivity {
    private static final String[] COLUMN_NAMES = {"name", "number", "emails"};
    private static final int[] VIEW_NAMES = {android.R.id.text1, android.R.id.text2};
    protected CursorAdapter mCursorAdapter;
    private TextView mEmptyText;
    protected QueryHandler mQueryHandler;
    protected Cursor mCursor = null;
    protected int mInitialSelection = -1;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().requestFeature(5);
        setContentView(R.layout.adn_list);
        this.mEmptyText = (TextView) findViewById(android.R.id.empty);
        this.mQueryHandler = new QueryHandler(getContentResolver());
    }

    @Override
    protected void onResume() {
        super.onResume();
        query();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mCursor != null) {
            this.mCursor.deactivate();
        }
    }

    protected Uri resolveIntent() {
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Uri.parse("content://icc/adn"));
        }
        return intent.getData();
    }

    private void query() {
        Uri uri = resolveIntent();
        this.mQueryHandler.startQuery(0, null, uri, COLUMN_NAMES, null, null, null);
        displayProgress(true);
    }

    private void reQuery() {
        query();
    }

    private void setAdapter() {
        if (this.mCursorAdapter == null) {
            this.mCursorAdapter = newAdapter();
            setListAdapter(this.mCursorAdapter);
        } else {
            this.mCursorAdapter.changeCursor(this.mCursor);
        }
        if (this.mInitialSelection >= 0 && this.mInitialSelection < this.mCursorAdapter.getCount()) {
            setSelection(this.mInitialSelection);
            getListView().setFocusableInTouchMode(true);
            getListView().requestFocus();
        }
    }

    protected CursorAdapter newAdapter() {
        return new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, this.mCursor, COLUMN_NAMES, VIEW_NAMES);
    }

    private void displayProgress(boolean loading) {
        int i;
        TextView textView = this.mEmptyText;
        if (loading) {
            i = R.string.simContacts_emptyLoading;
        } else {
            i = isAirplaneModeOn(this) ? R.string.simContacts_airplaneMode : R.string.simContacts_empty;
        }
        textView.setText(i);
        getWindow().setFeatureInt(5, loading ? -1 : -2);
    }

    private static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            ADNList.this.mCursor = c;
            ADNList.this.setAdapter();
            ADNList.this.displayProgress(false);
            ADNList.this.invalidateOptionsMenu();
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            ADNList.this.reQuery();
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            ADNList.this.reQuery();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            ADNList.this.reQuery();
        }
    }
}
