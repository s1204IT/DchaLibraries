package com.android.quicksearchbox;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import com.android.quicksearchbox.util.NamedTaskExecutor;

/* loaded from: classes.dex */
public abstract class AbstractInternalSource extends AbstractSource {
    protected abstract int getSourceIconResource();

    public AbstractInternalSource(Context context, Handler handler, NamedTaskExecutor namedTaskExecutor) {
        super(context, handler, namedTaskExecutor);
    }

    @Override // com.android.quicksearchbox.Source
    public String getDefaultIntentData() {
        return null;
    }

    @Override // com.android.quicksearchbox.AbstractSource
    protected String getIconPackage() {
        return getContext().getPackageName();
    }

    @Override // com.android.quicksearchbox.Source
    public Drawable getSourceIcon() {
        return getContext().getResources().getDrawable(getSourceIconResource());
    }

    @Override // com.android.quicksearchbox.Source
    public Uri getSourceIconUri() {
        return Uri.parse("android.resource://" + getContext().getPackageName() + "/" + getSourceIconResource());
    }
}
