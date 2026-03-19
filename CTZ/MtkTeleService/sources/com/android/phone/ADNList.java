package com.android.phone;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.util.Log;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.mediatek.settings.TelephonyUtils;

public class ADNList extends ListActivity {
    protected static final boolean DBG = true;
    protected static final int DELETE_TOKEN = 3;
    protected static final int EMAILS_COLUMN = 3;
    public static final String ICC_ADN_SUBID_URI = "content://icc/adn/subId/";
    public static final String ICC_ADN_URI = "content://icc/adn";
    protected static final int INSERT_TOKEN = 1;
    protected static final int NAME_COLUMN = 1;
    protected static final int NUMBER_COLUMN = 2;
    protected static final int QUERY_TOKEN = 0;
    protected static final String TAG = "ADNList";
    protected static final int UPDATE_TOKEN = 2;
    protected CursorAdapter mCursorAdapter;
    private TextView mEmptyText;
    protected QueryHandler mQueryHandler;
    private static final String[] COLUMN_NAMES = {"name", "number", "emails"};
    private static final int[] VIEW_NAMES = {android.R.id.text1, android.R.id.text2};
    protected Cursor mCursor = null;
    protected int mInitialSelection = -1;
    protected int mSubId = -1;

    @Override
    protected void onCreate(Bundle bundle) {
        log("onCreate");
        super.onCreate(bundle);
        getWindow().requestFeature(5);
        setContentView(R.layout.adn_list);
        this.mEmptyText = (TextView) findViewById(android.R.id.empty);
        this.mQueryHandler = new QueryHandler(getContentResolver());
        this.mSubId = getIntent().getIntExtra("subscription", -1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        query();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mCursor != null && !isInMultiWindowMode()) {
            log("onStop deactivate cursor");
            this.mCursor.deactivate();
        }
    }

    protected Uri resolveIntent() {
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Uri.parse(ICC_ADN_SUBID_URI + this.mSubId));
        }
        return intent.getData();
    }

    private void query() {
        Uri uriResolveIntent = resolveIntent();
        log("query: starting an async query");
        this.mQueryHandler.startQuery(0, null, uriResolveIntent, COLUMN_NAMES, null, null, null);
        displayProgress(DBG);
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
            getListView().setFocusableInTouchMode(DBG);
            getListView().requestFocus();
        }
    }

    protected CursorAdapter newAdapter() {
        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, this.mCursor, COLUMN_NAMES, VIEW_NAMES);
        simpleCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int i) {
                view.setTextAlignment(5);
                if (i == 2) {
                    String string = cursor.getString(2);
                    if (string != null) {
                        string = BidiFormatter.getInstance().unicodeWrap(string, TextDirectionHeuristics.LTR, ADNList.DBG);
                    }
                    if (view instanceof TextView) {
                        ((TextView) view).setText(string);
                    }
                    return ADNList.DBG;
                }
                return false;
            }
        });
        return simpleCursorAdapter;
    }

    private void displayProgress(boolean z) {
        int i;
        log("displayProgress: " + z);
        TextView textView = this.mEmptyText;
        if (z) {
            i = R.string.simContacts_emptyLoading;
        } else {
            i = TelephonyUtils.isAirplaneModeOn(this) ? R.string.simContacts_airplaneMode : R.string.simContacts_empty;
        }
        textView.setText(i);
        getWindow().setFeatureInt(5, z ? -1 : -2);
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            ADNList.this.log("onQueryComplete: cursor.count=" + cursor.getCount());
            ADNList.this.mCursor = cursor;
            ADNList.this.setAdapter();
            ADNList.this.displayProgress(false);
            ADNList.this.invalidateOptionsMenu();
        }

        @Override
        protected void onInsertComplete(int i, Object obj, Uri uri) {
            ADNList.this.log("onInsertComplete: requery");
            ADNList.this.reQuery();
        }

        @Override
        protected void onUpdateComplete(int i, Object obj, int i2) {
            ADNList.this.log("onUpdateComplete: requery");
            ADNList.this.reQuery();
        }

        @Override
        protected void onDeleteComplete(int i, Object obj, int i2) {
            ADNList.this.log("onDeleteComplete: requery");
            ADNList.this.reQuery();
        }
    }

    protected void log(String str) {
        Log.d(TAG, "[ADNList] " + str);
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        if (this.mCursor != null && !this.mCursor.isClosed()) {
            this.mCursor.close();
        }
        super.onDestroy();
    }
}
