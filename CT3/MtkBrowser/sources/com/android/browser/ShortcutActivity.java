package com.android.browser;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

public class ShortcutActivity extends Activity implements BookmarksPageCallbacks, View.OnClickListener {
    private BrowserBookmarksPage mBookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.shortcut_bookmark_title);
        setContentView(R.layout.pick_bookmark);
        this.mBookmarks = (BrowserBookmarksPage) getFragmentManager().findFragmentById(R.id.bookmarks);
        this.mBookmarks.setEnableContextMenu(false);
        this.mBookmarks.setCallbackListener(this);
        View cancel = findViewById(R.id.cancel);
        if (cancel == null) {
            return;
        }
        cancel.setOnClickListener(this);
    }

    @Override
    public boolean onBookmarkSelected(Cursor c, boolean isFolder) {
        if (isFolder) {
            return false;
        }
        Intent intent = BrowserBookmarksPage.createShortcutIntent(this, c);
        setResult(-1, intent);
        finish();
        return true;
    }

    @Override
    public boolean onOpenInNewWindow(String... urls) {
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                finish();
                break;
        }
    }
}
