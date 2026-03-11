package com.android.quicksearchbox.google;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.android.quicksearchbox.AbstractInternalSource;
import com.android.quicksearchbox.CursorBackedSourceResult;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.util.NamedTaskExecutor;

public abstract class AbstractGoogleSource extends AbstractInternalSource implements GoogleSource {
    public AbstractGoogleSource(Context context, Handler handler, NamedTaskExecutor namedTaskExecutor) {
        super(context, handler, namedTaskExecutor);
    }

    private SourceResult emptyIfNull(SourceResult sourceResult, String str) {
        return sourceResult == null ? new CursorBackedSourceResult(this, str) : sourceResult;
    }

    @Override
    public Intent createVoiceSearchIntent(Bundle bundle) {
        return createVoiceWebSearchIntent(bundle);
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
        return 2130903040;
    }

    @Override
    public SourceResult getSuggestions(String str, int i) {
        return emptyIfNull(queryInternal(str), str);
    }

    public abstract SourceResult queryInternal(String str);
}
