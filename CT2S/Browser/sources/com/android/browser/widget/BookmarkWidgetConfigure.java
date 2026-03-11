package com.android.browser.widget;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BrowserContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.android.browser.AddBookmarkPage;
import com.android.browser.R;

public class BookmarkWidgetConfigure extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    private ArrayAdapter<AddBookmarkPage.BookmarkAccount> mAccountAdapter;
    private int mAppWidgetId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(0);
        setVisible(false);
        setContentView(R.layout.widget_account_selection);
        findViewById(R.id.cancel).setOnClickListener(this);
        this.mAccountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        setListAdapter(this.mAccountAdapter);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            this.mAppWidgetId = extras.getInt("appWidgetId", 0);
        }
        if (this.mAppWidgetId == 0) {
            finish();
        } else {
            getLoaderManager().initLoader(1, null, this);
        }
    }

    @Override
    public void onClick(View v) {
        finish();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        AddBookmarkPage.BookmarkAccount account = this.mAccountAdapter.getItem(position);
        pickAccount(account.rootFolderId);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AccountsLoader(this);
    }

    void pickAccount(long rootId) {
        BookmarkThumbnailWidgetService.setupWidgetState(this, this.mAppWidgetId, rootId);
        Intent result = new Intent();
        result.putExtra("appWidgetId", this.mAppWidgetId);
        setResult(-1, result);
        finish();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            pickAccount(1L);
        } else if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            pickAccount(cursor.getLong(2));
        } else {
            this.mAccountAdapter.clear();
            while (cursor.moveToNext()) {
                this.mAccountAdapter.add(new AddBookmarkPage.BookmarkAccount(this, cursor));
            }
            setVisible(true);
        }
        getLoaderManager().destroyLoader(1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    static class AccountsLoader extends CursorLoader {
        static final String[] PROJECTION = {"account_name", "account_type", "root_id"};

        public AccountsLoader(Context context) {
            super(context, BrowserContract.Accounts.CONTENT_URI.buildUpon().appendQueryParameter("allowEmptyAccounts", "false").build(), PROJECTION, null, null, null);
        }
    }
}
