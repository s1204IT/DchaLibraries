package com.android.quicksearchbox.google;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.android.quicksearchbox.AbstractInternalSource;
import com.android.quicksearchbox.CursorBackedSourceResult;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.util.NamedTaskExecutor;

public abstract class AbstractGoogleSource extends AbstractInternalSource implements GoogleSource {
    public abstract SourceResult queryInternal(String str);

    public AbstractGoogleSource(Context context, Handler handler, NamedTaskExecutor namedTaskExecutor) {
        super(context, handler, namedTaskExecutor);
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
        return R.mipmap.google_icon;
    }

    @Override
    public SourceResult getSuggestions(String str, int i) {
        return emptyIfNull(queryInternal(str), str);
    }

    private SourceResult emptyIfNull(SourceResult sourceResult, String str) {
        return sourceResult == null ? new CursorBackedSourceResult(this, str) : sourceResult;
    }
}
