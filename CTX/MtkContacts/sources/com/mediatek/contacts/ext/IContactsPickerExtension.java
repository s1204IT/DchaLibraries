package com.mediatek.contacts.ext;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.Menu;

public interface IContactsPickerExtension {
    void addSearchMenu(Activity activity, Menu menu);

    boolean enableDisableSearchMenu(boolean z, Menu menu);

    boolean openAddProfileScreen(Uri uri, Context context);
}
