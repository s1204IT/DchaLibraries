package com.android.quickstep;

import android.content.Context;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Preconditions;

public class OverviewCallbacks {
    private static OverviewCallbacks sInstance;

    public static OverviewCallbacks get(Context context) {
        Preconditions.assertUIThread();
        if (sInstance == null) {
            sInstance = (OverviewCallbacks) Utilities.getOverrideObject(OverviewCallbacks.class, context.getApplicationContext(), R.string.overview_callbacks_class);
        }
        return sInstance;
    }

    public void onInitOverviewTransition() {
    }

    public void onResetOverview() {
    }

    public void closeAllWindows() {
    }
}
