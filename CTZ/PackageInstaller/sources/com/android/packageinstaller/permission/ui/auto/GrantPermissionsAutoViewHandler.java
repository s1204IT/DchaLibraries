package com.android.packageinstaller.permission.ui.auto;

import android.app.Activity;
import android.view.WindowManager;
import com.android.packageinstaller.permission.ui.handheld.GrantPermissionsViewHandlerImpl;

public class GrantPermissionsAutoViewHandler extends GrantPermissionsViewHandlerImpl {
    public GrantPermissionsAutoViewHandler(Activity activity, String str) {
        super(activity, str);
    }

    @Override
    public void updateWindowAttributes(WindowManager.LayoutParams layoutParams) {
        layoutParams.width = -1;
        layoutParams.height = -2;
    }
}
