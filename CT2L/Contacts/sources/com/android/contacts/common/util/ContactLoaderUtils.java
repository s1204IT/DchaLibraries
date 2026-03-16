package com.android.contacts.common.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;

public final class ContactLoaderUtils {
    public static Uri ensureIsContactUri(ContentResolver resolver, Uri uri) throws IllegalArgumentException {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        String authority = uri.getAuthority();
        if ("com.android.contacts".equals(authority)) {
            String type = resolver.getType(uri);
            if (!"vnd.android.cursor.item/contact".equals(type)) {
                if ("vnd.android.cursor.item/raw_contact".equals(type)) {
                    long rawContactId = ContentUris.parseId(uri);
                    return ContactsContract.RawContacts.getContactLookupUri(resolver, ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId));
                }
                throw new IllegalArgumentException("uri format is unknown");
            }
            return uri;
        }
        if ("contacts".equals(authority)) {
            long rawContactId2 = ContentUris.parseId(uri);
            return ContactsContract.RawContacts.getContactLookupUri(resolver, ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId2));
        }
        throw new IllegalArgumentException("uri authority is unknown");
    }
}
