package com.android.providers.contacts.enterprise;

import android.content.ContentUris;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.providers.contacts.ContactsProvider2;

public class EnterpriseContactsCursorWrapper extends CursorWrapper {
    private static final boolean VERBOSE_LOGGING = Log.isLoggable("EnterpriseCursorWrapper", 2);
    private static final UriMatcher sUriMatcher = ContactsProvider2.sUriMatcher;
    private final int[] contactIdIndices;
    private final Long mDirectoryId;
    private final boolean mIsDirectoryRemote;
    private final String[] originalColumnNames;

    public EnterpriseContactsCursorWrapper(Cursor cursor, String[] strArr, int[] iArr, Long l) {
        super(cursor);
        this.contactIdIndices = iArr;
        this.originalColumnNames = strArr;
        this.mDirectoryId = l;
        this.mIsDirectoryRemote = l != null && ContactsContract.Directory.isRemoteDirectoryId(l.longValue());
    }

    @Override
    public int getColumnCount() {
        return this.originalColumnNames.length;
    }

    @Override
    public String[] getColumnNames() {
        return this.originalColumnNames;
    }

    @Override
    public String getString(int i) {
        String string;
        long j;
        string = super.getString(i);
        String columnName = super.getColumnName(i);
        j = super.getLong(this.contactIdIndices[0]);
        switch (columnName) {
            case "photo_thumb_uri":
                if (this.mIsDirectoryRemote) {
                    return getRemoteDirectoryFileUri(string);
                }
                return getCorpThumbnailUri(j, getWrappedCursor());
            case "photo_uri":
                if (this.mIsDirectoryRemote) {
                    return getRemoteDirectoryFileUri(string);
                }
                return getCorpDisplayPhotoUri(j, getWrappedCursor());
            case "photo_file_id":
            case "photo_id":
                return null;
            case "custom_ringtone":
                String string2 = super.getString(i);
                if (string2 == null || Uri.parse(string2).isPathPrefixMatch(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)) {
                    return string2;
                }
                return null;
            case "lookup":
                String string3 = super.getString(i);
                if (TextUtils.isEmpty(string3)) {
                    return null;
                }
                return ContactsContract.Contacts.ENTERPRISE_CONTACT_LOOKUP_PREFIX + string3;
            default:
                return string;
        }
    }

    @Override
    public int getInt(int i) {
        return (int) getLong(i);
    }

    @Override
    public long getLong(int i) {
        long j = super.getLong(i);
        if (ArrayUtils.contains(this.contactIdIndices, i)) {
            return j + ContactsContract.Contacts.ENTERPRISE_CONTACT_ID_BASE;
        }
        String columnName = getColumnName(i);
        byte b = -1;
        int iHashCode = columnName.hashCode();
        if (iHashCode != -1274270136) {
            if (iHashCode == 1563708849 && columnName.equals("photo_file_id")) {
                b = 0;
            }
        } else if (columnName.equals("photo_id")) {
            b = 1;
        }
        switch (b) {
            case 0:
            case 1:
                return 0L;
            default:
                return j;
        }
    }

    private String getRemoteDirectoryFileUri(String str) {
        if (str == null) {
            return null;
        }
        Uri.Builder builderBuildUpon = ContactsContract.Directory.ENTERPRISE_FILE_URI.buildUpon();
        builderBuildUpon.appendPath(str);
        builderBuildUpon.appendQueryParameter("directory", Long.toString(this.mDirectoryId.longValue()));
        String string = builderBuildUpon.build().toString();
        if (VERBOSE_LOGGING) {
            Log.v("EnterpriseCursorWrapper", "getCorpDirectoryFileUri: output URI=" + string);
        }
        return string;
    }

    private static String getCorpThumbnailUri(long j, Cursor cursor) {
        String string = cursor.getString(cursor.getColumnIndex("photo_thumb_uri"));
        if (string == null) {
            return null;
        }
        if (sUriMatcher.match(Uri.parse(string)) == 1009) {
            return ContentUris.appendId(ContactsContract.Contacts.CORP_CONTENT_URI.buildUpon(), j).appendPath("photo").build().toString();
        }
        Log.e("EnterpriseCursorWrapper", "EnterpriseContactsCursorWrapper contains invalid PHOTO_THUMBNAIL_URI");
        return null;
    }

    private static String getCorpDisplayPhotoUri(long j, Cursor cursor) {
        String string = cursor.getString(cursor.getColumnIndex("photo_uri"));
        if (string == null) {
            return null;
        }
        int iMatch = sUriMatcher.match(Uri.parse(string));
        if (iMatch == 1009) {
            return ContentUris.appendId(ContactsContract.Contacts.CORP_CONTENT_URI.buildUpon(), j).appendPath("photo").build().toString();
        }
        if (iMatch == 1012 || iMatch == 22000) {
            return ContentUris.appendId(ContactsContract.Contacts.CORP_CONTENT_URI.buildUpon(), j).appendPath("display_photo").build().toString();
        }
        Log.e("EnterpriseCursorWrapper", "EnterpriseContactsCursorWrapper contains invalid PHOTO_URI");
        return null;
    }
}
