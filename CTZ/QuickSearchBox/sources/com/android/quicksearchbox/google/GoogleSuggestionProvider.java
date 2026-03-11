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

    private UriMatcher buildUriMatcher(Context context) {
        String authority = getAuthority(context);
        UriMatcher uriMatcher = new UriMatcher(-1);
        uriMatcher.addURI(authority, "search_suggest_query", 0);
        uriMatcher.addURI(authority, "search_suggest_query/*", 0);
        uriMatcher.addURI(authority, "search_suggest_shortcut", 1);
        uriMatcher.addURI(authority, "search_suggest_shortcut/*", 1);
        return uriMatcher;
    }

    private SourceResult emptyIfNull(SourceResult sourceResult, GoogleSource googleSource, String str) {
        return sourceResult == null ? new CursorBackedSourceResult(googleSource, str) : sourceResult;
    }

    private String getQuery(Uri uri) {
        return uri.getPathSegments().size() > 1 ? uri.getLastPathSegment() : "";
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException();
    }

    protected String getAuthority(Context context) {
        return context.getPackageName() + ".google";
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.android.search.suggest";
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onCreate() {
        this.mSource = QsbApplication.get(getContext()).getGoogleSource();
        this.mUriMatcher = buildUriMatcher(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        int iMatch = this.mUriMatcher.match(uri);
        if (iMatch == 0) {
            String query = getQuery(uri);
            return new SuggestionCursorBackedCursor(emptyIfNull(this.mSource.queryExternal(query), this.mSource, query));
        }
        if (iMatch == 1) {
            return new SuggestionCursorBackedCursor(this.mSource.refreshShortcut(getQuery(uri), uri.getQueryParameter("suggest_intent_extra_data")));
        }
        throw new IllegalArgumentException("Unknown URI " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException();
    }
}
