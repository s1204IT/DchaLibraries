package com.android.packageinstaller.permission.ui;

import android.R;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import com.android.packageinstaller.DeviceUtils;
import com.android.packageinstaller.permission.ui.ConfirmActionDialogFragment;
import com.android.packageinstaller.permission.ui.handheld.ReviewPermissionsFragment;
import com.android.packageinstaller.permission.ui.wear.ReviewPermissionsWearFragment;

public final class ReviewPermissionsActivity extends Activity implements ConfirmActionDialogFragment.OnActionConfirmedListener {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PackageInfo targetPackageInfo = getTargetPackageInfo();
        if (targetPackageInfo == null) {
            finish();
            return;
        }
        if (DeviceUtils.isWear(this)) {
            getFragmentManager().beginTransaction().replace(R.id.content, ReviewPermissionsWearFragment.newInstance(targetPackageInfo)).commit();
        } else {
            setContentView(com.android.packageinstaller.R.layout.review_permissions);
            if (getFragmentManager().findFragmentById(com.android.packageinstaller.R.id.preferences_frame) == null) {
                getFragmentManager().beginTransaction().add(com.android.packageinstaller.R.id.preferences_frame, ReviewPermissionsFragment.newInstance(targetPackageInfo)).commit();
            }
        }
    }

    @Override
    public void onActionConfirmed(String str) {
        ComponentCallbacks2 componentCallbacks2FindFragmentById = getFragmentManager().findFragmentById(com.android.packageinstaller.R.id.preferences_frame);
        if (componentCallbacks2FindFragmentById instanceof ConfirmActionDialogFragment.OnActionConfirmedListener) {
            ((ConfirmActionDialogFragment.OnActionConfirmedListener) componentCallbacks2FindFragmentById).onActionConfirmed(str);
        }
    }

    private PackageInfo getTargetPackageInfo() {
        String stringExtra = getIntent().getStringExtra("android.intent.extra.PACKAGE_NAME");
        if (TextUtils.isEmpty(stringExtra)) {
            return null;
        }
        try {
            return getPackageManager().getPackageInfo(stringExtra, 4096);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
