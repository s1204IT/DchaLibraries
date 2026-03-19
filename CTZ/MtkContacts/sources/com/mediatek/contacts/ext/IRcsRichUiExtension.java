package com.mediatek.contacts.ext;

import android.content.Context;
import android.net.Uri;
import com.android.vcard.VCardEntry;

public interface IRcsRichUiExtension {
    void loadRichScrnByContactUri(Uri uri, Context context);

    void loadRichScrnByVcardEntry(boolean z, VCardEntry vCardEntry, Context context);
}
