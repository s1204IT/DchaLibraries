package com.android.documentsui.archives;

import android.net.Uri;

public class ArchiveId {
    static final boolean $assertionsDisabled = false;
    public final int mAccessMode;
    public final Uri mArchiveUri;
    public final String mPath;

    public ArchiveId(Uri uri, int i, String str) {
        this.mArchiveUri = uri;
        this.mAccessMode = i;
        this.mPath = str;
    }

    public static ArchiveId fromDocumentId(String str) {
        int iIndexOf = str.indexOf(35);
        int i = iIndexOf + 1;
        int iIndexOf2 = str.indexOf(35, i);
        String strSubstring = str.substring(0, iIndexOf);
        String strSubstring2 = str.substring(i, iIndexOf2);
        return new ArchiveId(Uri.parse(strSubstring), Integer.parseInt(strSubstring2), str.substring(iIndexOf2 + 1));
    }

    public String toDocumentId() {
        return this.mArchiveUri.toString() + '#' + this.mAccessMode + '#' + this.mPath;
    }
}
