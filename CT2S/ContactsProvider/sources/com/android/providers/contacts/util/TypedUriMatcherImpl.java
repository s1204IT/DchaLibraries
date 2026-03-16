package com.android.providers.contacts.util;

import android.content.UriMatcher;
import android.net.Uri;

public class TypedUriMatcherImpl {
    private final String mAuthority;

    private final UriType mNoMatchUriType;
    private final UriMatcher mUriMatcher = new UriMatcher(-1);

    private final UriType[] mValues;

    public TypedUriMatcherImpl(String authority, UriType[] uriTypeArr) {
        this.mAuthority = authority;
        this.mValues = uriTypeArr;
        UriType uriType = null;
        for (UriType uriType2 : uriTypeArr) {
            String path = uriType2.path();
            if (path != null) {
                addUriType(path, uriType2);
            } else {
                uriType = uriType2;
            }
        }
        this.mNoMatchUriType = uriType;
    }

    private void addUriType(String path, UriType uriType) {
        this.mUriMatcher.addURI(this.mAuthority, path, uriType.ordinal());
    }

    public UriType match(Uri uri) {
        int match = this.mUriMatcher.match(uri);
        return match == -1 ? this.mNoMatchUriType : this.mValues[match];
    }
}
