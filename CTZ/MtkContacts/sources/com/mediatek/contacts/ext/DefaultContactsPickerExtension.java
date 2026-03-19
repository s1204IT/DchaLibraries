package com.mediatek.contacts.ext;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.Menu;

public class DefaultContactsPickerExtension implements IContactsPickerExtension {
    @Override
    public void addSearchMenu(Activity activity, Menu menu) {
    }

    @Override
    public boolean enableDisableSearchMenu(boolean z, Menu menu) {
        return true;
    }

    @Override
    public boolean openAddProfileScreen(Uri uri, Context context) {
        return false;
    }
}
