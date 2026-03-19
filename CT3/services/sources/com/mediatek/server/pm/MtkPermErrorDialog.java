package com.mediatek.server.pm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.os.BenesseExtension;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppErrorDialog;
import com.android.server.am.MtkAppErrorDialog;
import com.mediatek.Manifest;
import com.mediatek.internal.R;

public class MtkPermErrorDialog extends MtkAppErrorDialog implements View.OnClickListener {
    private final Context mContext;
    private String mPermission;
    private String mPkgName;
    private String mProcessName;

    public MtkPermErrorDialog(Context context, ActivityManagerService service, AppErrorDialog.Data data, String permission, String processName, String pkgName) {
        super(context, service, data);
        this.mContext = context;
        this.mPermission = permission;
        this.mProcessName = processName;
        this.mPkgName = pkgName;
        setupUiComponents();
    }

    private void setupUiComponents() {
        CharSequence applicationName;
        String message;
        Resources res = this.mContext.getResources();
        setTitle((CharSequence) null);
        CharSequence permissionName = "";
        try {
            PermissionInfo permissionInfo = this.mContext.getPackageManager().getPermissionInfo(this.mPermission, 0);
            permissionName = permissionInfo.loadLabel(this.mContext.getPackageManager());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        try {
            ApplicationInfo ai = this.mContext.getPackageManager().getApplicationInfo(this.mPkgName, 0);
            applicationName = this.mContext.getPackageManager().getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e2) {
            applicationName = this.mProcessName;
        }
        final boolean isWlanPerm = Manifest.permission.CTA_ENABLE_WIFI.equals(this.mPermission);
        final boolean isBtPerm = Manifest.permission.CTA_ENABLE_BT.equals(this.mPermission);
        if (!TextUtils.isEmpty(permissionName)) {
            if (isWlanPerm || isBtPerm) {
                message = res.getString(R.string.aerr_application_permission_connectivity, applicationName, permissionName);
            } else {
                message = res.getString(R.string.aerr_application_permission, applicationName, permissionName);
            }
        } else {
            message = res.getString(R.string.aerr_application_unknown_permission, applicationName);
        }
        setMessage(message);
        setButton(-1, res.getText(R.string.mtk_perm_err_dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String actionName;
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                if (isWlanPerm) {
                    actionName = "android.settings.WIFI_SETTINGS";
                } else if (isBtPerm) {
                    actionName = "android.settings.BLUETOOTH_SETTINGS";
                } else {
                    actionName = "android.intent.action.MANAGE_APP_DETAILED_PERMISSIONS";
                }
                Intent intent = new Intent(actionName);
                intent.putExtra("android.intent.extra.PACKAGE_NAME", MtkPermErrorDialog.this.mPkgName);
                intent.putExtra("android.intent.extra.PERMISSION_NAME", MtkPermErrorDialog.this.mPermission);
                intent.setFlags(268435456);
                MtkPermErrorDialog.this.mContext.startActivity(intent);
                MtkPermErrorDialog.this.clickButtonForResult(1);
            }
        });
        setButton(-3, res.getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MtkPermErrorDialog.this.clickButtonForResult(1);
            }
        });
    }

    @Override
    public void initLayout(FrameLayout frame) {
    }
}
