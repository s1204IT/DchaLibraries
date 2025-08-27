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

/* loaded from: classes.dex */
public abstract class AbstractGoogleSource extends AbstractInternalSource implements GoogleSource {
    public abstract SourceResult queryInternal(String str);

    public AbstractGoogleSource(Context context, Handler handler, NamedTaskExecutor namedTaskExecutor) {
        super(context, handler, namedTaskExecutor);
    }

    @Override // com.android.quicksearchbox.Source
    public Intent createVoiceSearchIntent(Bundle bundle) {
        return createVoiceWebSearchIntent(bundle);
    }

    @Override // com.android.quicksearchbox.Source
    public String getDefaultIntentAction() {
        return "android.intent.action.WEB_SEARCH";
    }

    @Override // com.android.quicksearchbox.SuggestionCursorProvider
    public String getName() {
        return "com.android.quicksearchbox/.google.GoogleSearch";
    }

    @Override // com.android.quicksearchbox.AbstractInternalSource
    protected int getSourceIconResource() {
        return R.mipmap.google_icon;
    }

    /* JADX DEBUG: Method merged with bridge method: getSuggestions(Ljava/lang/String;I)Lcom/android/quicksearchbox/SuggestionCursor; */
    @Override // com.android.quicksearchbox.SuggestionCursorProvider
    public SourceResult getSuggestions(String str, int i) {
        return emptyIfNull(queryInternal(str), str);
    }

    private SourceResult emptyIfNull(SourceResult sourceResult, String str) {
        return sourceResult == null ? new CursorBackedSourceResult(this, str) : sourceResult;
    }
}
