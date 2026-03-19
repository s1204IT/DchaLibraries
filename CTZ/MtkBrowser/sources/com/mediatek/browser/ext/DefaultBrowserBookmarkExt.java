package com.mediatek.browser.ext;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class DefaultBrowserBookmarkExt implements IBrowserBookmarkExt {
    @Override
    public int addDefaultBookmarksForCustomer(SQLiteDatabase sQLiteDatabase) {
        Log.i("@M_DefaultBrowserBookmarkExt", "Enter: addDefaultBookmarksForCustomer --default implement");
        return 0;
    }

    @Override
    public void createBookmarksPageOptionsMenu(Menu menu, MenuInflater menuInflater) {
        Log.i("@M_DefaultBrowserBookmarkExt", "Enter: createBookmarksPageOptionsMenu --default implement");
    }

    @Override
    public boolean bookmarksPageOptionsMenuItemSelected(MenuItem menuItem, Activity activity, long j) {
        Log.i("@M_DefaultBrowserBookmarkExt", "Enter: bookmarksPageOptionsMenuItemSelected --default implement");
        return false;
    }

    @Override
    public boolean customizeEditExistingFolderState(Bundle bundle, boolean z) {
        Log.i("DefaultBrowserBookmarkExt", "Enter: customizeEditExistingFolderState --default implement");
        return z;
    }

    @Override
    public String getCustomizedEditFolderFakeTitleString(Bundle bundle, String str) {
        Log.i("DefaultBrowserBookmarkExt", "Enter: getCustomizedEditFolderFakeTitleString --default implement");
        return str;
    }

    @Override
    public void showCustomizedEditFolderNewFolderView(View view, View view2, Bundle bundle) {
        Log.i("DefaultBrowserBookmarkExt", "Enter: showCustomizedEditFolderNewFolderView --default implement");
        if (view != null) {
            view.setVisibility(0);
        }
        if (view2 != null) {
            view2.setVisibility(0);
        }
    }

    @Override
    public boolean shouldSetCustomizedEditFolderSelection(Bundle bundle, boolean z) {
        Log.i("DefaultBrowserBookmarkExt", "Enter: shouldSetCustomizedEditFolderSelection --default implement");
        return z;
    }

    @Override
    public Boolean saveCustomizedEditFolder(Context context, String str, long j, Bundle bundle, String str2) {
        Log.i("DefaultBrowserBookmarkExt", "Enter: saveCustomizedEditFolder --default implement");
        return null;
    }
}
