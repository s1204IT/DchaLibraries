package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/* loaded from: classes.dex */
public class DefaultAppListExt extends ContextWrapper implements IAppListExt {
    private static final String TAG = "DefaultAppListExt";

    public DefaultAppListExt(Context context) {
        super(context);
        Log.i(TAG, "constructor\n");
    }

    @Override // com.mediatek.settings.ext.IAppListExt
    public View addLayoutAppView(View view, TextView textView, TextView textView2, int i, Drawable drawable, ViewGroup viewGroup) {
        return view;
    }

    @Override // com.mediatek.settings.ext.IAppListExt
    public void setAppListItem(String str, int i) {
    }
}
