package com.android.systemui.recents.misc;

import android.content.Context;
import com.android.systemui.shared.system.TaskStackChangeListener;

public abstract class SysUiTaskStackChangeListener extends TaskStackChangeListener {
    protected final boolean checkCurrentUserId(Context context, boolean z) {
        return checkCurrentUserId(SystemServicesProxy.getInstance(context).getCurrentUser(), z);
    }
}
