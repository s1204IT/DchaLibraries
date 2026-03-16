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

        public JoinContactLoaderResult(Cursor baseCursor, Cursor suggestionCursor) {
            super(baseCursor);
            this.suggestionCursor = suggestionCursor;
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
    public void setProjection(String[] projection) {
        super.setProjection(projection);
        this.mProjection = projection;
    }

    @Override
    public Cursor loadInBackground() {
        Cursor suggestionsCursor = getContext().getContentResolver().query(this.mSuggestionUri, this.mProjection, null, null, null);
        if (suggestionsCursor == null) {
            return null;
        }
        try {
            Cursor baseCursor = super.loadInBackground();
            if (baseCursor == null) {
            }
            JoinContactLoaderResult result = new JoinContactLoaderResult(baseCursor, suggestionsCursor);
            Cursor cursorToClose = null;
            if (0 != 0) {
                cursorToClose.close();
            }
            return result;
        } finally {
            if (suggestionsCursor != null) {
                suggestionsCursor.close();
            }
        }
    }
}
