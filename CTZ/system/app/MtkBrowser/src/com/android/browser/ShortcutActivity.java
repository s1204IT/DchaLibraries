package com.android.browser;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

/* loaded from: classes.dex */
public class ShortcutActivity extends Activity implements View.OnClickListener, BrowserBookmarksPage2 {
    private BrowserBookmarksPage mBookmarks;

    @Override // android.app.Activity
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

    @Override // com.android.browser.BrowserBookmarksPage2
    public boolean onBookmarkSelected(Cursor cursor, boolean z) {
        if (z) {
            return false;
        }
        setResult(-1, BrowserBookmarksPage.createShortcutIntent(this, cursor));
        finish();
        return true;
    }

    @Override // com.android.browser.BrowserBookmarksPage2
    public boolean onOpenInNewWindow(String... strArr) {
        return false;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view.getId() == R.id.cancel) {
            finish();
        }
    }
}
