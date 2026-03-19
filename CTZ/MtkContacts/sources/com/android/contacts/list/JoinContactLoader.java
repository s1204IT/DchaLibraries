package com.android.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;

public class JoinContactLoader extends CursorLoader {
    private String[] mProjection;
    private Uri mSuggestionUri;

    public static class JoinContactLoaderResult extends CursorWrapper {
        public final Cursor suggestionCursor;

        public JoinContactLoaderResult(Cursor cursor, Cursor cursor2) {
            super(cursor);
            this.suggestionCursor = cursor2;
        }

        @Override
        public void close() {
            try {
                if (this.suggestionCursor != null) {
                    this.suggestionCursor.close();
                }
            } finally {
                if (super.getWrappedCursor() != null) {
                    super.close();
                }
            }
        }
    }

    public JoinContactLoader(Context context) {
        super(context, null, null, null, null, null);
    }

    public void setSuggestionUri(Uri uri) {
        this.mSuggestionUri = uri;
    }

    @Override
    public void setProjection(String[] strArr) {
        super.setProjection(strArr);
        this.mProjection = strArr;
    }

    @Override
    public Cursor loadInBackground() {
        Cursor cursorQuery = getContext().getContentResolver().query(this.mSuggestionUri, this.mProjection, null, null, null);
        if (cursorQuery == null) {
            return null;
        }
        try {
            Cursor cursorLoadInBackground = super.loadInBackground();
            if (cursorLoadInBackground == null) {
                return null;
            }
            return new JoinContactLoaderResult(cursorLoadInBackground, cursorQuery);
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }
}
