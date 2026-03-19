package com.android.contacts.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;
import com.mediatek.contacts.util.Log;

public final class ContactLoaderUtils {
    public static Uri ensureIsContactUri(ContentResolver contentResolver, Uri uri) throws IllegalArgumentException {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        String authority = uri.getAuthority();
        if ("com.android.contacts".equals(authority)) {
            String type = contentResolver.getType(uri);
            Log.d("ContactLoaderUtils", "[ensureIsContactUri] type=" + type);
            if ("vnd.android.cursor.item/contact".equals(type)) {
                return uri;
            }
            if ("vnd.android.cursor.item/raw_contact".equals(type)) {
                long id = ContentUris.parseId(uri);
                Log.d("ContactLoaderUtils", "[ensureIsContactUri] rawContactId 1 =" + id);
                return ContactsContract.RawContacts.getContactLookupUri(contentResolver, ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id));
            }
            throw new IllegalArgumentException("uri format is unknown");
        }
        if ("contacts".equals(authority)) {
            long id2 = ContentUris.parseId(uri);
            Log.d("ContactLoaderUtils", "[ensureIsContactUri] rawContactId 2 =" + id2);
            return ContactsContract.RawContacts.getContactLookupUri(contentResolver, ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id2));
        }
        throw new IllegalArgumentException("uri authority is unknown");
    }
}
