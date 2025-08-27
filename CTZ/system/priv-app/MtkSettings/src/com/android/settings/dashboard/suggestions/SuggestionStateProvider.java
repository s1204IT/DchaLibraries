package com.android.settings.dashboard.suggestions;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public class SuggestionStateProvider extends ContentProvider {
    static final String EXTRA_CANDIDATE_ID = "candidate_id";
    static final String METHOD_GET_SUGGESTION_STATE = "getSuggestionState";

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        return true;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        throw new UnsupportedOperationException("query operation not supported currently.");
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("getType operation not supported currently.");
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("insert operation not supported currently.");
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException("delete operation not supported currently.");
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException("update operation not supported currently.");
    }

    @Override // android.content.ContentProvider
    public Bundle call(String str, String str2, Bundle bundle) {
        boolean zIsSuggestionComplete;
        Bundle bundle2 = new Bundle();
        if (METHOD_GET_SUGGESTION_STATE.equals(str)) {
            String string = bundle.getString(EXTRA_CANDIDATE_ID);
            ComponentName componentName = (ComponentName) bundle.getParcelable("android.intent.extra.COMPONENT_NAME");
            if (componentName == null) {
                zIsSuggestionComplete = true;
            } else {
                Context context = getContext();
                zIsSuggestionComplete = FeatureFactory.getFactory(context).getSuggestionFeatureProvider(context).isSuggestionComplete(context, componentName);
            }
            Log.d("SugstStatusProvider", "Suggestion " + string + " complete: " + zIsSuggestionComplete);
            bundle2.putBoolean("candidate_is_complete", zIsSuggestionComplete);
        }
        return bundle2;
    }
}
