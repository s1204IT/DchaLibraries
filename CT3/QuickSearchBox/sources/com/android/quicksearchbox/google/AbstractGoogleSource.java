package com.android.quicksearchbox.google;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.android.quicksearchbox.AbstractInternalSource;
import com.android.quicksearchbox.CursorBackedSourceResult;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.util.NamedTaskExecutor;

public abstract class AbstractGoogleSource extends AbstractInternalSource implements GoogleSource {
    @Override
    public abstract ComponentName getIntentComponent();

    @Override
    public abstract SourceResult queryExternal(String str);

    public abstract SourceResult queryInternal(String str);

    @Override
    public abstract SuggestionCursor refreshShortcut(String str, String str2);

    public AbstractGoogleSource(Context context, Handler uiThread, NamedTaskExecutor iconLoader) {
        super(context, uiThread, iconLoader);
    }

    @Override
    public Intent createVoiceSearchIntent(Bundle appData) {
        return createVoiceWebSearchIntent(appData);
    }

    @Override
    public String getDefaultIntentAction() {
        return "android.intent.action.WEB_SEARCH";
    }

    @Override
    public String getName() {
        return "com.android.quicksearchbox/.google.GoogleSearch";
    }

    @Override
    protected int getSourceIconResource() {
        return R.mipmap.google_icon;
    }

    @Override
    public SourceResult getSuggestions(String query, int queryLimit) {
        return emptyIfNull(queryInternal(query), query);
    }

    private SourceResult emptyIfNull(SourceResult result, String query) {
        return result == null ? new CursorBackedSourceResult(this, query) : result;
    }
}
