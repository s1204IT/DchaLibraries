package com.android.server.am;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class UnsupportedDisplaySizeDialog {
    private final AlertDialog mDialog;
    private final String mPackageName;

    public UnsupportedDisplaySizeDialog(final AppWarnings appWarnings, Context context, ApplicationInfo applicationInfo) {
        this.mPackageName = applicationInfo.packageName;
        this.mDialog = new AlertDialog.Builder(context).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setMessage(context.getString(R.string.mime_type_presentation, applicationInfo.loadSafeLabel(context.getPackageManager()))).setView(R.layout.preference_list_content_single).create();
        this.mDialog.create();
        Window window = this.mDialog.getWindow();
        window.setType(2002);
        window.getAttributes().setTitle("UnsupportedDisplaySizeDialog");
        CheckBox checkBox = (CheckBox) this.mDialog.findViewById(R.id.accessibility_autoclick_position_button);
        checkBox.setChecked(true);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                appWarnings.setPackageFlag(this.f$0.mPackageName, 1, !z);
            }
        });
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
