package com.android.providers.contacts;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.android.providers.contacts.VoicemailContentProvider;
import java.io.FileNotFoundException;

public interface VoicemailTable {

    public interface Delegate {
        int delete(VoicemailContentProvider.UriData uriData, String str, String[] strArr);

        String getType(VoicemailContentProvider.UriData uriData);

        Uri insert(VoicemailContentProvider.UriData uriData, ContentValues contentValues);

        ParcelFileDescriptor openFile(VoicemailContentProvider.UriData uriData, String str) throws FileNotFoundException;

        Cursor query(VoicemailContentProvider.UriData uriData, String[] strArr, String str, String[] strArr2, String str2);

        int update(VoicemailContentProvider.UriData uriData, ContentValues contentValues, String str, String[] strArr);
    }

    public interface DelegateHelper {
        void checkAndAddSourcePackageIntoValues(VoicemailContentProvider.UriData uriData, ContentValues contentValues);

        ParcelFileDescriptor openDataFile(VoicemailContentProvider.UriData uriData, String str) throws FileNotFoundException;
    }
}
