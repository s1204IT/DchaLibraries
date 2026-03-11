package com.android.quicksearchbox;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import com.android.quicksearchbox.util.NamedTaskExecutor;

public abstract class AbstractInternalSource extends AbstractSource {
    public AbstractInternalSource(Context context, Handler handler, NamedTaskExecutor namedTaskExecutor) {
        super(context, handler, namedTaskExecutor);
    }

    @Override
    public String getDefaultIntentData() {
        return null;
    }

    @Override
    protected String getIconPackage() {
        return getContext().getPackageName();
    }

    @Override
    public Drawable getSourceIcon() {
        return getContext().getResources().getDrawable(getSourceIconResource());
    }

    protected abstract int getSourceIconResource();

    @Override
    public Uri getSourceIconUri() {
        return Uri.parse("android.resource://" + getContext().getPackageName() + "/" + getSourceIconResource());
    }
}
