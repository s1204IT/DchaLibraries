package com.android.systemui.volume;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.VolumePolicy;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import com.android.settingslib.applications.InterestingConfigChanges;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.VolumeDialog;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.volume.VolumeDialogControllerImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VolumeDialogComponent implements TunerService.Tunable, VolumeComponent, VolumeDialogControllerImpl.UserActivityListener {
    private final Context mContext;
    private VolumeDialog mDialog;
    private final SystemUI mSysui;
    private final InterestingConfigChanges mConfigChanges = new InterestingConfigChanges(-1073741820);
    private VolumePolicy mVolumePolicy = new VolumePolicy(false, false, false, 400);
    private final VolumeDialog.Callback mVolumeDialogCallback = new VolumeDialog.Callback() {
        @Override
        public void onZenSettingsClicked() {
            if (BenesseExtension.getDchaState() == 0) {
                VolumeDialogComponent.this.startSettings(ZenModePanel.ZEN_SETTINGS);
            }
        }

        @Override
        public void onZenPrioritySettingsClicked() {
            if (BenesseExtension.getDchaState() == 0) {
                VolumeDialogComponent.this.startSettings(ZenModePanel.ZEN_PRIORITY_SETTINGS);
            }
        }
    };
    private final VolumeDialogControllerImpl mController = (VolumeDialogControllerImpl) Dependency.get(VolumeDialogController.class);

    public VolumeDialogComponent(SystemUI systemUI, Context context, Handler handler) {
        this.mSysui = systemUI;
        this.mContext = context;
        this.mController.setUserActivityListener(this);
        ((PluginDependencyProvider) Dependency.get(PluginDependencyProvider.class)).allowPluginDependency(VolumeDialogController.class);
        ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(VolumeDialog.class).withPlugin(VolumeDialog.class).withDefault(new Supplier() {
            @Override
            public final Object get() {
                return this.f$0.createDefault();
            }
        }).withFeature("android.hardware.type.automotive", new Supplier() {
            @Override
            public final Object get() {
                return this.f$0.createCarDefault();
            }
        }).withCallback(new Consumer() {
            @Override
            public final void accept(Object obj) {
                VolumeDialogComponent.lambda$new$0(this.f$0, (VolumeDialog) obj);
            }
        }).build();
        applyConfiguration();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_volume_down_silent", "sysui_volume_up_silent", "sysui_do_not_disturb");
    }

    public static void lambda$new$0(VolumeDialogComponent volumeDialogComponent, VolumeDialog volumeDialog) {
        if (volumeDialogComponent.mDialog != null) {
            volumeDialogComponent.mDialog.destroy();
        }
        volumeDialogComponent.mDialog = volumeDialog;
        volumeDialogComponent.mDialog.init(2020, volumeDialogComponent.mVolumeDialogCallback);
    }

    private VolumeDialog createDefault() {
        VolumeDialogImpl volumeDialogImpl = new VolumeDialogImpl(this.mContext);
        volumeDialogImpl.setStreamImportant(1, false);
        volumeDialogImpl.setAutomute(true);
        volumeDialogImpl.setSilentMode(false);
        return volumeDialogImpl;
    }

    private VolumeDialog createCarDefault() {
        return new CarVolumeDialogImpl(this.mContext);
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        boolean z = false;
        if ("sysui_volume_down_silent".equals(str)) {
            if (str2 != null && Integer.parseInt(str2) != 0) {
                z = true;
            }
            setVolumePolicy(z, this.mVolumePolicy.volumeUpToExitSilent, this.mVolumePolicy.doNotDisturbWhenSilent, this.mVolumePolicy.vibrateToSilentDebounce);
            return;
        }
        if ("sysui_volume_up_silent".equals(str)) {
            if (str2 != null && Integer.parseInt(str2) != 0) {
                z = true;
            }
            setVolumePolicy(this.mVolumePolicy.volumeDownToEnterSilent, z, this.mVolumePolicy.doNotDisturbWhenSilent, this.mVolumePolicy.vibrateToSilentDebounce);
            return;
        }
        if ("sysui_do_not_disturb".equals(str)) {
            if (str2 != null && Integer.parseInt(str2) != 0) {
                z = true;
            }
            setVolumePolicy(this.mVolumePolicy.volumeDownToEnterSilent, this.mVolumePolicy.volumeUpToExitSilent, z, this.mVolumePolicy.vibrateToSilentDebounce);
        }
    }

    private void setVolumePolicy(boolean z, boolean z2, boolean z3, int i) {
        this.mVolumePolicy = new VolumePolicy(z, z2, z3, i);
        this.mController.setVolumePolicy(this.mVolumePolicy);
    }

    void setEnableDialogs(boolean z, boolean z2) {
        this.mController.setEnableDialogs(z, z2);
    }

    @Override
    public void onUserActivity() {
        KeyguardViewMediator keyguardViewMediator = (KeyguardViewMediator) this.mSysui.getComponent(KeyguardViewMediator.class);
        if (keyguardViewMediator != null) {
            keyguardViewMediator.userActivity();
        }
    }

    private void applyConfiguration() {
        this.mController.setVolumePolicy(this.mVolumePolicy);
        this.mController.showDndTile(true);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        if (this.mConfigChanges.applyNewConfig(this.mContext.getResources())) {
            this.mController.mCallbacks.onConfigurationChanged();
        }
    }

    @Override
    public void dismissNow() {
        this.mController.dismiss();
    }

    @Override
    public void dispatchDemoCommand(String str, Bundle bundle) {
    }

    @Override
    public void register() {
        this.mController.register();
        DndTile.setCombinedIcon(this.mContext, true);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    private void startSettings(Intent intent) {
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).startActivity(intent, true, true);
    }
}
