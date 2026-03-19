package com.android.providers.contacts;

import android.content.Context;

public class ShadowCallLogProvider extends CallLogProvider {
    @Override
    protected CallLogDatabaseHelper getDatabaseHelper(Context context) {
        return CallLogDatabaseHelper.getInstanceForShadow(context);
    }

    @Override
    protected boolean isShadow() {
        return true;
    }
}
