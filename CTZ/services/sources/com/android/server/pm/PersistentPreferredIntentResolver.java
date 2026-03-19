package com.android.server.pm;

import com.android.server.IntentResolver;

public class PersistentPreferredIntentResolver extends IntentResolver<PersistentPreferredActivity, PersistentPreferredActivity> {
    @Override
    protected PersistentPreferredActivity[] newArray(int i) {
        return new PersistentPreferredActivity[i];
    }

    @Override
    protected boolean isPackageForFilter(String str, PersistentPreferredActivity persistentPreferredActivity) {
        return str.equals(persistentPreferredActivity.mComponent.getPackageName());
    }
}
