package com.android.systemui.volume;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.media.VolumePolicy;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.volume.VolumeDialog;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class VolumeDialogComponent implements VolumeComponent, TunerService.Tunable {
    private final Context mContext;
    private final VolumeDialogController mController;
    private final VolumeDialog mDialog;
    private final SystemUI mSysui;
    private final ZenModeController mZenModeController;
    private VolumePolicy mVolumePolicy = new VolumePolicy(true, true, true, 0);
    private final VolumeDialog.Callback mVolumeDialogCallback = new VolumeDialog.Callback() {
        @Override
        public void onZenPrioritySettingsClicked() {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            VolumeDialogComponent.this.startSettings(ZenModePanel.ZEN_PRIORITY_SETTINGS);
        }
    };
    private Handler mHandler = new Handler(true);

    public VolumeDialogComponent(SystemUI sysui, Context context, Handler handler, ZenModeController zen) {
        this.mSysui = sysui;
        this.mContext = context;
        this.mController = new VolumeDialogController(context, null) {
            @Override
            protected void onUserActivityW() {
                VolumeDialogComponent.this.sendUserActivity();
            }
        };
        ContentObserver obs = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                if (BenesseExtension.getDchaState() != 0) {
                    VolumeDialogComponent.this.mVolumePolicy = new VolumePolicy(false, false, false, 0);
                } else {
                    VolumeDialogComponent.this.mVolumePolicy = new VolumePolicy(true, true, true, 0);
                }
                VolumeDialogComponent.this.mController.setVolumePolicy(VolumeDialogComponent.this.mVolumePolicy);
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, obs, -1);
        this.mZenModeController = zen;
        this.mDialog = new VolumeDialog(context, 2020, this.mController, zen, this.mVolumeDialogCallback);
        applyConfiguration();
        TunerService.get(this.mContext).addTunable(this, "sysui_volume_down_silent", "sysui_volume_up_silent", "sysui_do_not_disturb");
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if ("sysui_volume_down_silent".equals(key)) {
            boolean volumeDownToEnterSilent = newValue == null || Integer.parseInt(newValue) != 0;
            setVolumePolicy(volumeDownToEnterSilent, this.mVolumePolicy.volumeUpToExitSilent, this.mVolumePolicy.doNotDisturbWhenSilent, this.mVolumePolicy.vibrateToSilentDebounce);
        } else if ("sysui_volume_up_silent".equals(key)) {
            boolean volumeUpToExitSilent = newValue == null || Integer.parseInt(newValue) != 0;
            setVolumePolicy(this.mVolumePolicy.volumeDownToEnterSilent, volumeUpToExitSilent, this.mVolumePolicy.doNotDisturbWhenSilent, this.mVolumePolicy.vibrateToSilentDebounce);
        } else {
            if (!"sysui_do_not_disturb".equals(key)) {
                return;
            }
            boolean doNotDisturbWhenSilent = newValue == null || Integer.parseInt(newValue) != 0;
            setVolumePolicy(this.mVolumePolicy.volumeDownToEnterSilent, this.mVolumePolicy.volumeUpToExitSilent, doNotDisturbWhenSilent, this.mVolumePolicy.vibrateToSilentDebounce);
        }
    }

    private void setVolumePolicy(boolean volumeDownToEnterSilent, boolean volumeUpToExitSilent, boolean doNotDisturbWhenSilent, int vibrateToSilentDebounce) {
        this.mVolumePolicy = new VolumePolicy(volumeDownToEnterSilent, volumeUpToExitSilent, doNotDisturbWhenSilent, vibrateToSilentDebounce);
        this.mController.setVolumePolicy(this.mVolumePolicy);
    }

    public void sendUserActivity() {
        KeyguardViewMediator kvm = (KeyguardViewMediator) this.mSysui.getComponent(KeyguardViewMediator.class);
        if (kvm == null) {
            return;
        }
        kvm.userActivity();
    }

    private void applyConfiguration() {
        this.mDialog.setStreamImportant(4, true);
        this.mDialog.setStreamImportant(1, false);
        this.mDialog.setShowHeaders(false);
        this.mDialog.setAutomute(true);
        this.mDialog.setSilentMode(false);
        this.mController.setVolumePolicy(this.mVolumePolicy);
        this.mController.showDndTile(true);
    }

    @Override
    public ZenModeController getZenController() {
        return this.mZenModeController;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void dismissNow() {
        this.mController.dismiss();
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
    }

    @Override
    public void register() {
        this.mController.register();
        DndTile.setCombinedIcon(this.mContext, true);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mController.dump(fd, pw, args);
        this.mDialog.dump(pw);
    }

    public void startSettings(Intent intent) {
        ((PhoneStatusBar) this.mSysui.getComponent(PhoneStatusBar.class)).startActivityDismissingKeyguard(intent, true, true);
    }
}
