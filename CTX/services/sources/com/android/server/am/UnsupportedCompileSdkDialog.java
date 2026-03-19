package com.android.server.am;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.android.server.utils.AppInstallerUtil;

public class UnsupportedCompileSdkDialog {
    private final AlertDialog mDialog;
    private final String mPackageName;

    public UnsupportedCompileSdkDialog(final AppWarnings appWarnings, final Context context, ApplicationInfo applicationInfo) {
        this.mPackageName = applicationInfo.packageName;
        AlertDialog.Builder view = new AlertDialog.Builder(context).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setMessage(context.getString(R.string.mime_type_generic_ext, applicationInfo.loadSafeLabel(context.getPackageManager()))).setView(R.layout.preference_list_content_material);
        final Intent intentCreateIntent = AppInstallerUtil.createIntent(context, applicationInfo.packageName);
        if (intentCreateIntent != null) {
            view.setNeutralButton(R.string.mime_type_generic, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    context.startActivity(intentCreateIntent);
                }
            });
        }
        this.mDialog = view.create();
        this.mDialog.create();
        Window window = this.mDialog.getWindow();
        window.setType(2002);
        window.getAttributes().setTitle("UnsupportedCompileSdkDialog");
        CheckBox checkBox = (CheckBox) this.mDialog.findViewById(R.id.accessibility_autoclick_position_button);
        checkBox.setChecked(true);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                appWarnings.setPackageFlag(this.f$0.mPackageName, 2, !z);
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
