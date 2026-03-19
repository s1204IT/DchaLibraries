package com.android.packageinstaller.permission.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.MenuItem;
import com.android.packageinstaller.DeviceUtils;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.ui.auto.AppPermissionsFragment;
import com.android.packageinstaller.permission.ui.handheld.AllAppPermissionsFragment;
import com.android.packageinstaller.permission.ui.handheld.ManageStandardPermissionsFragment;
import com.android.packageinstaller.permission.ui.television.ManagePermissionsFragment;
import com.android.packageinstaller.permission.ui.television.PermissionAppsFragment;
import com.android.packageinstaller.permission.ui.wear.AppPermissionsFragmentWear;

public final class ManagePermissionsActivity extends OverlayTouchActivity {
    @Override
    public void onCreate(Bundle bundle) {
        Fragment fragmentNewInstance;
        if (DeviceUtils.isAuto(this)) {
            setTheme(R.style.CarSettingTheme);
        }
        super.onCreate(bundle);
        if (bundle != null) {
            return;
        }
        String action = getIntent().getAction();
        byte b = -1;
        int iHashCode = action.hashCode();
        if (iHashCode != -1168603379) {
            if (iHashCode != 1685512017) {
                if (iHashCode == 1861372431 && action.equals("android.intent.action.MANAGE_PERMISSIONS")) {
                    b = 0;
                }
            } else if (action.equals("android.intent.action.MANAGE_APP_PERMISSIONS")) {
                b = 1;
            }
        } else if (action.equals("android.intent.action.MANAGE_PERMISSION_APPS")) {
            b = 2;
        }
        switch (b) {
            case DialogFragment.STYLE_NORMAL:
                if (DeviceUtils.isTelevision(this)) {
                    fragmentNewInstance = ManagePermissionsFragment.newInstance();
                } else {
                    fragmentNewInstance = ManageStandardPermissionsFragment.newInstance();
                }
                break;
            case DialogFragment.STYLE_NO_TITLE:
                String stringExtra = getIntent().getStringExtra("android.intent.extra.PACKAGE_NAME");
                if (stringExtra == null) {
                    Log.i("ManagePermissionsActivity", "Missing mandatory argument EXTRA_PACKAGE_NAME");
                    finish();
                    return;
                }
                if (DeviceUtils.isAuto(this)) {
                    fragmentNewInstance = AppPermissionsFragment.newInstance(stringExtra);
                } else if (DeviceUtils.isWear(this)) {
                    fragmentNewInstance = AppPermissionsFragmentWear.newInstance(stringExtra);
                } else if (DeviceUtils.isTelevision(this)) {
                    fragmentNewInstance = com.android.packageinstaller.permission.ui.television.AppPermissionsFragment.newInstance(stringExtra);
                } else if (getIntent().getBooleanExtra("com.android.packageinstaller.extra.ALL_PERMISSIONS", false)) {
                    fragmentNewInstance = AllAppPermissionsFragment.newInstance(stringExtra);
                } else {
                    fragmentNewInstance = com.android.packageinstaller.permission.ui.handheld.AppPermissionsFragment.newInstance(stringExtra);
                }
                break;
                break;
            case DialogFragment.STYLE_NO_FRAME:
                String stringExtra2 = getIntent().getStringExtra("android.intent.extra.PERMISSION_NAME");
                if (stringExtra2 == null) {
                    Log.i("ManagePermissionsActivity", "Missing mandatory argument EXTRA_PERMISSION_NAME");
                    finish();
                    return;
                } else if (DeviceUtils.isTelevision(this)) {
                    fragmentNewInstance = PermissionAppsFragment.newInstance(stringExtra2);
                } else {
                    fragmentNewInstance = com.android.packageinstaller.permission.ui.handheld.PermissionAppsFragment.newInstance(stringExtra2);
                }
                break;
            default:
                Log.w("ManagePermissionsActivity", "Unrecognized action " + action);
                finish();
                return;
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragmentNewInstance).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (DeviceUtils.isAuto(this)) {
            if (menuItem.getItemId() == 16908332) {
                onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(menuItem);
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
