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

    public CompatModeDialog(ActivityManagerService activityManagerService, Context context, ApplicationInfo applicationInfo) {
        super(context, R.style.Theme.Holo.Dialog.MinWidth);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        getWindow().requestFeature(1);
        getWindow().setType(2002);
        getWindow().setGravity(81);
        this.mService = activityManagerService;
        this.mAppInfo = applicationInfo;
        setContentView(R.layout.alert_dialog_leanback_button_panel_side);
        this.mCompatEnabled = (Switch) findViewById(R.id.alternate_expand_target);
        this.mCompatEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                synchronized (CompatModeDialog.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        CompatModeDialog.this.mService.mCompatModePackages.setPackageScreenCompatModeLocked(CompatModeDialog.this.mAppInfo.packageName, CompatModeDialog.this.mCompatEnabled.isChecked() ? 1 : 0);
                        CompatModeDialog.this.updateControls();
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        });
        this.mAlwaysShow = (CheckBox) findViewById(R.id.accessibility_autoclick_position_button);
        this.mAlwaysShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                synchronized (CompatModeDialog.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        CompatModeDialog.this.mService.mCompatModePackages.setPackageAskCompatModeLocked(CompatModeDialog.this.mAppInfo.packageName, CompatModeDialog.this.mAlwaysShow.isChecked());
                        CompatModeDialog.this.updateControls();
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        });
        this.mHint = findViewById(R.id.issued_on_header);
        updateControls();
    }

    void updateControls() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                int iComputeCompatModeLocked = this.mService.mCompatModePackages.computeCompatModeLocked(this.mAppInfo);
                Switch r2 = this.mCompatEnabled;
                boolean z = true;
                if (iComputeCompatModeLocked != 1) {
                    z = false;
                }
                r2.setChecked(z);
                boolean packageAskCompatModeLocked = this.mService.mCompatModePackages.getPackageAskCompatModeLocked(this.mAppInfo.packageName);
                this.mAlwaysShow.setChecked(packageAskCompatModeLocked);
                this.mHint.setVisibility(packageAskCompatModeLocked ? 4 : 0);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }
}
