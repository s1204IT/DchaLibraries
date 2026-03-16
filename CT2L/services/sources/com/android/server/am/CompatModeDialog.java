package com.android.server.am;

import android.R;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

public final class CompatModeDialog extends Dialog {
    final CheckBox mAlwaysShow;
    final ApplicationInfo mAppInfo;
    final Switch mCompatEnabled;
    final View mHint;
    final ActivityManagerService mService;

    public CompatModeDialog(ActivityManagerService service, Context context, ApplicationInfo appInfo) {
        super(context, R.style.Theme.Holo.Dialog.MinWidth);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        getWindow().requestFeature(1);
        getWindow().setType(2002);
        getWindow().setGravity(81);
        this.mService = service;
        this.mAppInfo = appInfo;
        setContentView(R.layout.alert_dialog_holo);
        this.mCompatEnabled = (Switch) findViewById(R.id.deleteButton);
        this.mCompatEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                synchronized (CompatModeDialog.this.mService) {
                    CompatModeDialog.this.mService.mCompatModePackages.setPackageScreenCompatModeLocked(CompatModeDialog.this.mAppInfo.packageName, CompatModeDialog.this.mCompatEnabled.isChecked() ? 1 : 0);
                    CompatModeDialog.this.updateControls();
                }
            }
        });
        this.mAlwaysShow = (CheckBox) findViewById(R.id.density);
        this.mAlwaysShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                synchronized (CompatModeDialog.this.mService) {
                    CompatModeDialog.this.mService.mCompatModePackages.setPackageAskCompatModeLocked(CompatModeDialog.this.mAppInfo.packageName, CompatModeDialog.this.mAlwaysShow.isChecked());
                    CompatModeDialog.this.updateControls();
                }
            }
        });
        this.mHint = findViewById(R.id.deny_button);
        updateControls();
    }

    void updateControls() {
        synchronized (this.mService) {
            int mode = this.mService.mCompatModePackages.computeCompatModeLocked(this.mAppInfo);
            this.mCompatEnabled.setChecked(mode == 1);
            boolean ask = this.mService.mCompatModePackages.getPackageAskCompatModeLocked(this.mAppInfo.packageName);
            this.mAlwaysShow.setChecked(ask);
            this.mHint.setVisibility(ask ? 4 : 0);
        }
    }
}
