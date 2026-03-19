package com.android.providers.contacts.util;

import android.content.UriMatcher;
import android.net.Uri;
import com.android.providers.contacts.util.UriType;

public class TypedUriMatcherImpl<T extends UriType> {
    private final String mAuthority;
    private final T mNoMatchUriType;
    private final UriMatcher mUriMatcher = new UriMatcher(-1);
    private final T[] mValues;

    public TypedUriMatcherImpl(String str, T[] tArr) {
        this.mAuthority = str;
        this.mValues = tArr;
        T t = null;
        for (T t2 : tArr) {
            String strPath = t2.path();
            if (strPath == null) {
                t = t2;
            } else {
                addUriType(strPath, t2);
            }
        }
        this.mNoMatchUriType = t;
    }

    private void addUriType(String str, T t) {
        this.mUriMatcher.addURI(this.mAuthority, str, t.ordinal());
    }

    public T match(Uri uri) {
        int iMatch = this.mUriMatcher.match(uri);
        if (iMatch == -1) {
            return this.mNoMatchUriType;
        }
        return this.mValues[iMatch];
    }
}
