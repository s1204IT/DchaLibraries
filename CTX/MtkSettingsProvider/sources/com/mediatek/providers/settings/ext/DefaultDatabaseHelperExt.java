package com.mediatek.providers.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;

public class DefaultDatabaseHelperExt extends ContextWrapper implements IDatabaseHelperExt {
    public DefaultDatabaseHelperExt(Context context) {
        super(context);
    }

    @Override
    public String getResStr(Context context, String str, String str2) {
        return str2;
    }

    @Override
    public String getResBoolean(Context context, String str, String str2) {
        return str2;
    }

    @Override
    public String getResInteger(Context context, String str, String str2) {
        return str2;
    }
}
