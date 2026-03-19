package com.android.setupwizardlib.util;

import android.content.Context;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;

public class FallbackThemeWrapper extends ContextThemeWrapper {
    public FallbackThemeWrapper(Context context, int i) {
        super(context, i);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int i, boolean z) {
        theme.applyStyle(i, false);
    }
}
