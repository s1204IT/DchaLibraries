package com.android.systemui.volume;

import android.content.res.Configuration;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.IRemoteVolumeController;
import android.media.IVolumeController;
import android.media.session.ISessionController;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl;
import com.android.systemui.volume.VolumePanel;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class VolumeUI extends SystemUI {
    private static final Uri SETTING_URI = Settings.Global.getUriFor("systemui_volume_controller");
    private AudioManager mAudioManager;
    private int mDismissDelay;
    private boolean mEnabled;
    private MediaSessionManager mMediaSessionManager;
    private VolumePanel mPanel;
    private RemoteVolumeController mRemoteVolumeController;
    private VolumeController mVolumeController;
    private final Handler mHandler = new Handler();
    private final ContentObserver mObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (VolumeUI.SETTING_URI.equals(uri)) {
                VolumeUI.this.updateController();
            }
        }
    };
    private final Runnable mStartZenSettings = new Runnable() {
        @Override
        public void run() {
            if (BenesseExtension.getDchaState() == 0) {
                ((PhoneStatusBar) VolumeUI.this.getComponent(PhoneStatusBar.class)).startActivityDismissingKeyguard(ZenModePanel.ZEN_SETTINGS, true, true);
                VolumeUI.this.mPanel.postDismiss(VolumeUI.this.mDismissDelay);
            }
        }
    };

    @Override
    public void start() {
        this.mEnabled = this.mContext.getResources().getBoolean(R.bool.enable_volume_ui);
        if (this.mEnabled) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
            this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
            initPanel();
            this.mVolumeController = new VolumeController();
            this.mRemoteVolumeController = new RemoteVolumeController();
            putComponent(VolumeComponent.class, this.mVolumeController);
            updateController();
            this.mContext.getContentResolver().registerContentObserver(SETTING_URI, false, this.mObserver);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mPanel != null) {
            this.mPanel.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("mEnabled=");
        pw.println(this.mEnabled);
        if (this.mPanel != null) {
            this.mPanel.dump(fd, pw, args);
        }
    }

    public void updateController() {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "systemui_volume_controller", 1) != 0) {
            Log.d("VolumeUI", "Registering volume controller");
            this.mAudioManager.setVolumeController(this.mVolumeController);
            this.mMediaSessionManager.setRemoteVolumeController(this.mRemoteVolumeController);
        } else {
            Log.d("VolumeUI", "Unregistering volume controller");
            this.mAudioManager.setVolumeController(null);
            this.mMediaSessionManager.setRemoteVolumeController(null);
        }
    }

    private void initPanel() {
        this.mDismissDelay = this.mContext.getResources().getInteger(R.integer.volume_panel_dismiss_delay);
        this.mPanel = new VolumePanel(this.mContext, new ZenModeControllerImpl(this.mContext, this.mHandler));
        this.mPanel.setCallback(new VolumePanel.Callback() {
            @Override
            public void onZenSettings() {
                VolumeUI.this.mHandler.removeCallbacks(VolumeUI.this.mStartZenSettings);
                VolumeUI.this.mHandler.post(VolumeUI.this.mStartZenSettings);
            }

            @Override
            public void onInteraction() {
                KeyguardViewMediator kvm = (KeyguardViewMediator) VolumeUI.this.getComponent(KeyguardViewMediator.class);
                if (kvm != null) {
                    kvm.userActivity();
                }
            }

            @Override
            public void onVisible(boolean visible) {
                if (VolumeUI.this.mAudioManager != null && VolumeUI.this.mVolumeController != null) {
                    VolumeUI.this.mAudioManager.notifyVolumeControllerVisible(VolumeUI.this.mVolumeController, visible);
                }
            }
        });
    }

    private final class VolumeController extends IVolumeController.Stub implements VolumeComponent {
        private VolumeController() {
        }

        public void displaySafeVolumeWarning(int flags) throws RemoteException {
            VolumeUI.this.mPanel.postDisplaySafeVolumeWarning(flags);
        }

        public void volumeChanged(int streamType, int flags) throws RemoteException {
            VolumeUI.this.mPanel.postVolumeChanged(streamType, flags);
        }

        public void masterVolumeChanged(int flags) throws RemoteException {
            VolumeUI.this.mPanel.postMasterVolumeChanged(flags);
        }

        public void masterMuteChanged(int flags) throws RemoteException {
            VolumeUI.this.mPanel.postMasterMuteChanged(flags);
        }

        public void setLayoutDirection(int layoutDirection) throws RemoteException {
            VolumeUI.this.mPanel.postLayoutDirection(layoutDirection);
        }

        public void dismiss() throws RemoteException {
            dismissNow();
        }

        @Override
        public ZenModeController getZenController() {
            return VolumeUI.this.mPanel.getZenController();
        }

        @Override
        public void dispatchDemoCommand(String command, Bundle args) {
            VolumeUI.this.mPanel.dispatchDemoCommand(command, args);
        }

        @Override
        public void dismissNow() {
            VolumeUI.this.mPanel.postDismiss(0L);
        }
    }

    private final class RemoteVolumeController extends IRemoteVolumeController.Stub {
        private RemoteVolumeController() {
        }

        public void remoteVolumeChanged(ISessionController binder, int flags) throws RemoteException {
            MediaController controller = new MediaController(VolumeUI.this.mContext, binder);
            VolumeUI.this.mPanel.postRemoteVolumeChanged(controller, flags);
        }

        public void updateRemoteController(ISessionController session) throws RemoteException {
            VolumeUI.this.mPanel.postRemoteSliderVisibility(session != null);
        }
    }
}
