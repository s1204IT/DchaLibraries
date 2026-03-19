package com.android.documentsui.services;

import android.net.Uri;

public class ResourceException extends Exception {
    public ResourceException(String str, Uri uri, Exception exc) {
        super(String.format(str, uri.toString()), exc);
    }

    public ResourceException(String str, Uri uri, Uri uri2, Exception exc) {
        super(String.format(str, uri.toString(), uri2.toString()), exc);
    }

    public ResourceException(String str) {
        super(str);
    }

    public ResourceException(String str, Uri uri) {
        super(String.format(str, uri.toString()));
    }
}
