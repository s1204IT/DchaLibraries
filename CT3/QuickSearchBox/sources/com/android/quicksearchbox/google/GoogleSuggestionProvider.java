package com.android.quicksearchbox.google;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import com.android.quicksearchbox.CursorBackedSourceResult;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursorBackedCursor;

public class GoogleSuggestionProvider extends ContentProvider {
    private GoogleSource mSource;
    private UriMatcher mUriMatcher;

    @Override
    public boolean onCreate() {
        this.mSource = QsbApplication.get(getContext()).getGoogleSource();
        this.mUriMatcher = buildUriMatcher(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.android.search.suggest";
    }

    private SourceResult emptyIfNull(SourceResult result, GoogleSource source, String query) {
        return result == null ? new CursorBackedSourceResult(source, query) : result;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int match = this.mUriMatcher.match(uri);
        if (match == 0) {
            String query = getQuery(uri);
            return new SuggestionCursorBackedCursor(emptyIfNull(this.mSource.queryExternal(query), this.mSource, query));
        }
        if (match == 1) {
            String shortcutId = getQuery(uri);
            String extraData = uri.getQueryParameter("suggest_intent_extra_data");
            return new SuggestionCursorBackedCursor(this.mSource.refreshShortcut(shortcutId, extraData));
        }
        throw new IllegalArgumentException("Unknown URI " + uri);
    }

    private String getQuery(Uri uri) {
        if (uri.getPathSegments().size() > 1) {
            return uri.getLastPathSegment();
        }
        return "";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private UriMatcher buildUriMatcher(Context context) {
        String authority = getAuthority(context);
        UriMatcher matcher = new UriMatcher(-1);
        matcher.addURI(authority, "search_suggest_query", 0);
        matcher.addURI(authority, "search_suggest_query/*", 0);
        matcher.addURI(authority, "search_suggest_shortcut", 1);
        matcher.addURI(authority, "search_suggest_shortcut/*", 1);
        return matcher;
    }

    protected String getAuthority(Context context) {
        return context.getPackageName() + ".google";
    }
}
