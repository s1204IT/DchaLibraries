package com.android.contacts.common.list;

import android.content.Intent;
import android.net.Uri;

public interface OnPhoneNumberPickerActionListener {
    void onCallNumberDirectly(String str);

    void onHomeInActionBarSelected();

    void onPickPhoneNumberAction(Uri uri);

    void onShortcutIntentCreated(Intent intent);
}
