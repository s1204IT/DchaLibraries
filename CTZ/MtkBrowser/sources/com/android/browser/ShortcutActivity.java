package com.android.browser;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

public class ShortcutActivity extends Activity implements View.OnClickListener, BookmarksPageCallbacks {
    private BrowserBookmarksPage mBookmarks;

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
    public void onClick(View view) {
        if (view.getId() != 2131558462) {
            return;
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTitle(2131493028);
        setContentView(2130968614);
        this.mBookmarks = (BrowserBookmarksPage) getFragmentManager().findFragmentById(2131558445);
        this.mBookmarks.setEnableContextMenu(false);
        this.mBookmarks.setCallbackListener(this);
        View viewFindViewById = findViewById(2131558462);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(this);
        }
    }

    @Override
    public boolean onOpenInNewWindow(String... strArr) {
        return false;
    }
}
