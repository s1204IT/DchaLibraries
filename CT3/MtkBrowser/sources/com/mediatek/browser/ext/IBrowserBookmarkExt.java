package com.mediatek.browser.ext;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public interface IBrowserBookmarkExt {
    int addDefaultBookmarksForCustomer(SQLiteDatabase sQLiteDatabase);

    boolean bookmarksPageOptionsMenuItemSelected(MenuItem menuItem, Activity activity, long j);

    void createBookmarksPageOptionsMenu(Menu menu, MenuInflater menuInflater);

    boolean customizeEditExistingFolderState(Bundle bundle, boolean z);

    String getCustomizedEditFolderFakeTitleString(Bundle bundle, String str);

    Boolean saveCustomizedEditFolder(Context context, String str, long j, Bundle bundle, String str2);

    boolean shouldSetCustomizedEditFolderSelection(Bundle bundle, boolean z);

    void showCustomizedEditFolderNewFolderView(View view, View view2, Bundle bundle);
}
