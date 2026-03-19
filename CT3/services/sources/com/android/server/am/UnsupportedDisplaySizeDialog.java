package com.android.server.am;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class UnsupportedDisplaySizeDialog {
    private final AlertDialog mDialog;
    private final String mPackageName;

    public UnsupportedDisplaySizeDialog(final ActivityManagerService service, Context context, ApplicationInfo appInfo) {
        this.mPackageName = appInfo.packageName;
        PackageManager pm = context.getPackageManager();
        CharSequence label = appInfo.loadSafeLabel(pm);
        CharSequence message = context.getString(R.string.emergency_calling_do_not_show_again, label);
        this.mDialog = new AlertDialog.Builder(context).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setMessage(message).setView(R.layout.popup_menu_header_item_layout).create();
        this.mDialog.create();
        Window window = this.mDialog.getWindow();
        window.setType(2002);
        window.getAttributes().setTitle("UnsupportedDisplaySizeDialog");
        CheckBox alwaysShow = (CheckBox) this.mDialog.findViewById(R.id.floating_popup_container);
        alwaysShow.setChecked(true);
        alwaysShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                UnsupportedDisplaySizeDialog.this.m977com_android_server_am_UnsupportedDisplaySizeDialog_lambda$1(service, arg0, arg1);
            }
        });
    }

    void m977com_android_server_am_UnsupportedDisplaySizeDialog_lambda$1(ActivityManagerService service, CompoundButton buttonView, boolean isChecked) {
        synchronized (service) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                service.mCompatModePackages.setPackageNotifyUnsupportedZoomLocked(this.mPackageName, isChecked);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void show() {
        this.mDialog.show();
    }

    public void dismiss() {
        this.mDialog.dismiss();
    }
}
