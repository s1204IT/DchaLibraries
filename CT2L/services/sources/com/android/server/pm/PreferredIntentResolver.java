package com.android.server.pm;

import com.android.server.IntentResolver;
import java.io.PrintWriter;

public class PreferredIntentResolver extends IntentResolver<PreferredActivity, PreferredActivity> {
    @Override
    protected PreferredActivity[] newArray(int size) {
        return new PreferredActivity[size];
    }

    @Override
    protected boolean isPackageForFilter(String packageName, PreferredActivity filter) {
        return packageName.equals(filter.mPref.mComponent.getPackageName());
    }

    @Override
    protected void dumpFilter(PrintWriter out, String prefix, PreferredActivity filter) {
        filter.mPref.dump(out, prefix, filter);
    }
}
