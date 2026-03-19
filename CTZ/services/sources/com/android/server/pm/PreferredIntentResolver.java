package com.android.server.pm;

import com.android.server.IntentResolver;
import java.io.PrintWriter;

public class PreferredIntentResolver extends IntentResolver<PreferredActivity, PreferredActivity> {
    @Override
    protected PreferredActivity[] newArray(int i) {
        return new PreferredActivity[i];
    }

    @Override
    protected boolean isPackageForFilter(String str, PreferredActivity preferredActivity) {
        return str.equals(preferredActivity.mPref.mComponent.getPackageName());
    }

    @Override
    protected void dumpFilter(PrintWriter printWriter, String str, PreferredActivity preferredActivity) {
        preferredActivity.mPref.dump(printWriter, str, preferredActivity);
    }
}
