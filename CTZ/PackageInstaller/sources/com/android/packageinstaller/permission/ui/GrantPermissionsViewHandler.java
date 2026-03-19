package com.android.packageinstaller.permission.ui;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public interface GrantPermissionsViewHandler {

    public interface ResultListener {
        void onPermissionGrantResult(String str, boolean z, boolean z2);
    }

    View createView();

    void loadInstanceState(Bundle bundle);

    void saveInstanceState(Bundle bundle);

    void updateUi(String str, int i, int i2, Icon icon, CharSequence charSequence, boolean z);

    void updateWindowAttributes(WindowManager.LayoutParams layoutParams);
}
