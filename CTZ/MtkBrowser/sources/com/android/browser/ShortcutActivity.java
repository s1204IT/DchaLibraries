package com.android.browser;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

public class ShortcutActivity extends Activity implements View.OnClickListener, BookmarksPageCallbacks {
    private BrowserBookmarksPage mBookmarks;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTitle(R.string.shortcut_bookmark_title);
        setContentView(R.layout.pick_bookmark);
        this.mBookmarks = (BrowserBookmarksPage) getFragmentManager().findFragmentById(R.id.bookmarks);
        this.mBookmarks.setEnableContextMenu(false);
        this.mBookmarks.setCallbackListener(this);
        View viewFindViewById = findViewById(R.id.cancel);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(this);
        }
    }

    @Override
    public boolean onBookmarkSelected(Cursor cursor, boolean z) {
        if (z) {
            return false;
        }
        setResult(-1, BrowserBookmarksPage.createShortcutIntent(this, cursor));
        finish();
        return true;
    }

    @Override
    public boolean onOpenInNewWindow(String... strArr) {
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.cancel) {
            finish();
        }
    }
}
