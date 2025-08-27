package com.android.systemui.volume;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.IVolumeController;
import android.media.VolumePolicy;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.util.ArrayMap;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.annotations.GuardedBy;
import com.android.systemui.Dumpable;
import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.volume.MediaSessions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/* loaded from: classes.dex */
public class VolumeDialogControllerImpl implements Dumpable, VolumeDialogController {
    private AudioManager mAudio;
    private IAudioService mAudioService;
    private final Context mContext;
    private boolean mDestroyed;
    private final boolean mHasVibrator;
    private long mLastToggledRingerOn;
    private final MediaSessions mMediaSessions;
    private final NotificationManager mNoMan;
    private final NotificationManager mNotificationManager;
    private final SettingObserver mObserver;
    private boolean mShowA11yStream;
    private boolean mShowSafetyWarning;
    private boolean mShowVolumeDialog;
    protected StatusBar mStatusBar;

    @GuardedBy("this")
    private UserActivityListener mUserActivityListener;
    private final Vibrator mVibrator;
    private VolumePolicy mVolumePolicy;
    private final W mWorker;
    private final HandlerThread mWorkerThread;
    private static final String TAG = Util.logTag(VolumeDialogControllerImpl.class);
    private static final AudioAttributes SONIFICIATION_VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    static final ArrayMap<Integer, Integer> STREAMS = new ArrayMap<>();
    private final Receiver mReceiver = new Receiver();
    protected C mCallbacks = new C();
    private final VolumeDialogController.State mState = new VolumeDialogController.State();
    protected final MediaSessionsCallbacks mMediaSessionsCallbacksW = new MediaSessionsCallbacks();
    private boolean mShowDndTile = true;
    protected final VC mVolumeController = new VC();

    public interface UserActivityListener {
        void onUserActivity();
    }

    static {
        STREAMS.put(4, Integer.valueOf(R.string.stream_alarm));
        STREAMS.put(6, Integer.valueOf(R.string.stream_bluetooth_sco));
        STREAMS.put(8, Integer.valueOf(R.string.stream_dtmf));
        STREAMS.put(3, Integer.valueOf(R.string.stream_music));
        STREAMS.put(10, Integer.valueOf(R.string.stream_accessibility));
        STREAMS.put(5, Integer.valueOf(R.string.stream_notification));
        STREAMS.put(2, Integer.valueOf(R.string.stream_ring));
        STREAMS.put(1, Integer.valueOf(R.string.stream_system));
        STREAMS.put(7, Integer.valueOf(R.string.stream_system_enforced));
        STREAMS.put(9, Integer.valueOf(R.string.stream_tts));
        STREAMS.put(0, Integer.valueOf(R.string.stream_voice_call));
    }

    public VolumeDialogControllerImpl(Context context) {
        this.mContext = context.getApplicationContext();
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        Events.writeEvent(this.mContext, 5, new Object[0]);
        this.mWorkerThread = new HandlerThread(VolumeDialogControllerImpl.class.getSimpleName());
        this.mWorkerThread.start();
        this.mWorker = new W(this.mWorkerThread.getLooper());
        this.mMediaSessions = createMediaSessions(this.mContext, this.mWorkerThread.getLooper(), this.mMediaSessionsCallbacksW);
        this.mAudio = (AudioManager) this.mContext.getSystemService("audio");
        this.mNoMan = (NotificationManager) this.mContext.getSystemService("notification");
        this.mObserver = new SettingObserver(this.mWorker);
        this.mObserver.init();
        this.mReceiver.init();
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mHasVibrator = this.mVibrator != null && this.mVibrator.hasVibrator();
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        updateStatusBar();
        this.mVolumeController.setA11yMode(((AccessibilityManager) context.getSystemService(AccessibilityManager.class)).isAccessibilityVolumeStreamActive() ? 1 : 0);
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public AudioManager getAudioManager() {
        return this.mAudio;
    }

    public void dismiss() {
        this.mCallbacks.onDismissRequested(2);
    }

    protected void setVolumeController() {
        try {
            this.mAudio.setVolumeController(this.mVolumeController);
        } catch (SecurityException e) {
            Log.w(TAG, "Unable to set the volume controller", e);
        }
    }

    protected void setAudioManagerStreamVolume(int i, int i2, int i3) {
        this.mAudio.setStreamVolume(i, i2, i3);
    }

    protected int getAudioManagerStreamVolume(int i) {
        return this.mAudio.getLastAudibleStreamVolume(i);
    }

    protected int getAudioManagerStreamMaxVolume(int i) {
        return this.mAudio.getStreamMaxVolume(i);
    }

    protected int getAudioManagerStreamMinVolume(int i) {
        return this.mAudio.getStreamMinVolumeInt(i);
    }

    public void register() {
        setVolumeController();
        setVolumePolicy(this.mVolumePolicy);
        showDndTile(this.mShowDndTile);
        try {
            this.mMediaSessions.init();
        } catch (SecurityException e) {
            Log.w(TAG, "No access to media sessions", e);
        }
    }

    public void setVolumePolicy(VolumePolicy volumePolicy) {
        this.mVolumePolicy = volumePolicy;
        if (this.mVolumePolicy == null) {
            return;
        }
        try {
            this.mAudio.setVolumePolicy(this.mVolumePolicy);
        } catch (NoSuchMethodError e) {
            Log.w(TAG, "No volume policy api");
        }
    }

    protected MediaSessions createMediaSessions(Context context, Looper looper, MediaSessions.Callbacks callbacks) {
        return new MediaSessions(context, looper, callbacks);
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(VolumeDialogControllerImpl.class.getSimpleName() + " state:");
        printWriter.print("  mDestroyed: ");
        printWriter.println(this.mDestroyed);
        printWriter.print("  mVolumePolicy: ");
        printWriter.println(this.mVolumePolicy);
        printWriter.print("  mState: ");
        printWriter.println(this.mState.toString(4));
        printWriter.print("  mShowDndTile: ");
        printWriter.println(this.mShowDndTile);
        printWriter.print("  mHasVibrator: ");
        printWriter.println(this.mHasVibrator);
        printWriter.print("  mRemoteStreams: ");
        printWriter.println(this.mMediaSessionsCallbacksW.mRemoteStreams.values());
        printWriter.print("  mShowA11yStream: ");
        printWriter.println(this.mShowA11yStream);
        printWriter.println();
        this.mMediaSessions.dump(printWriter);
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void addCallback(VolumeDialogController.Callbacks callbacks, Handler handler) {
        this.mCallbacks.add(callbacks, handler);
        callbacks.onAccessibilityModeChanged(Boolean.valueOf(this.mShowA11yStream));
    }

    public void setUserActivityListener(UserActivityListener userActivityListener) {
        if (this.mDestroyed) {
            return;
        }
        synchronized (this) {
            this.mUserActivityListener = userActivityListener;
        }
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void removeCallback(VolumeDialogController.Callbacks callbacks) {
        this.mCallbacks.remove(callbacks);
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void getState() {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.sendEmptyMessage(3);
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void notifyVisible(boolean z) {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.obtainMessage(12, z ? 1 : 0, 0).sendToTarget();
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void userActivity() {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.removeMessages(13);
        this.mWorker.sendEmptyMessage(13);
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void setRingerMode(int i, boolean z) {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.obtainMessage(4, i, z ? 1 : 0).sendToTarget();
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void setStreamVolume(int i, int i2) {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.obtainMessage(10, i, i2).sendToTarget();
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void setActiveStream(int i) {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.obtainMessage(11, i, 0).sendToTarget();
    }

    public void setEnableDialogs(boolean z, boolean z2) {
        this.mShowVolumeDialog = z;
        this.mShowSafetyWarning = z2;
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void scheduleTouchFeedback() {
        this.mLastToggledRingerOn = System.currentTimeMillis();
    }

    private void playTouchFeedback() {
        if (System.currentTimeMillis() - this.mLastToggledRingerOn < 1000) {
            try {
                this.mAudioService.playSoundEffect(5);
            } catch (RemoteException e) {
            }
        }
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public void vibrate(VibrationEffect vibrationEffect) {
        if (this.mHasVibrator) {
            this.mVibrator.vibrate(vibrationEffect, SONIFICIATION_VIBRATION_ATTRIBUTES);
        }
    }

    @Override // com.android.systemui.plugins.VolumeDialogController
    public boolean hasVibrator() {
        return this.mHasVibrator;
    }

    private void onNotifyVisibleW(boolean z) {
        if (this.mDestroyed) {
            return;
        }
        this.mAudio.notifyVolumeControllerVisible(this.mVolumeController, z);
        if (!z && updateActiveStreamW(-1)) {
            this.mCallbacks.onStateChanged(this.mState);
        }
    }

    private void onUserActivityW() {
        synchronized (this) {
            if (this.mUserActivityListener != null) {
                this.mUserActivityListener.onUserActivity();
            }
        }
    }

    private void onShowSafetyWarningW(int i) {
        if (this.mShowSafetyWarning) {
            this.mCallbacks.onShowSafetyWarning(i);
        }
    }

    private void onAccessibilityModeChanged(Boolean bool) {
        this.mCallbacks.onAccessibilityModeChanged(bool);
    }

    private boolean checkRoutedToBluetoothW(int i) {
        if (i == 3) {
            return false | updateStreamRoutedToBluetoothW(i, (this.mAudio.getDevicesForStream(3) & 896) != 0);
        }
        return false;
    }

    private void updateStatusBar() {
        if (this.mStatusBar == null) {
            this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        }
    }

    private boolean shouldShowUI(int i) {
        updateStatusBar();
        if (this.mStatusBar != null) {
            if (this.mStatusBar.getWakefulnessState() == 0 || this.mStatusBar.getWakefulnessState() == 3 || !this.mStatusBar.isDeviceInteractive() || (i & 1) == 0 || !this.mShowVolumeDialog) {
                return false;
            }
        } else if (!this.mShowVolumeDialog || (i & 1) == 0) {
            return false;
        }
        return true;
    }

    boolean onVolumeChangedW(int i, int i2) {
        boolean zUpdateActiveStreamW;
        boolean zShouldShowUI = shouldShowUI(i2);
        boolean z = (i2 & 4096) != 0;
        boolean z2 = (i2 & 2048) != 0;
        boolean z3 = (i2 & 128) != 0;
        if (zShouldShowUI) {
            zUpdateActiveStreamW = updateActiveStreamW(i) | false;
        } else {
            zUpdateActiveStreamW = false;
        }
        int audioManagerStreamVolume = getAudioManagerStreamVolume(i);
        boolean zUpdateStreamLevelW = zUpdateActiveStreamW | updateStreamLevelW(i, audioManagerStreamVolume) | checkRoutedToBluetoothW(zShouldShowUI ? 3 : i);
        if (zUpdateStreamLevelW) {
            this.mCallbacks.onStateChanged(this.mState);
        }
        if (zShouldShowUI) {
            this.mCallbacks.onShowRequested(1);
        }
        if (z2) {
            this.mCallbacks.onShowVibrateHint();
        }
        if (z3) {
            this.mCallbacks.onShowSilentHint();
        }
        if (zUpdateStreamLevelW && z) {
            Events.writeEvent(this.mContext, 4, Integer.valueOf(i), Integer.valueOf(audioManagerStreamVolume));
        }
        return zUpdateStreamLevelW;
    }

    private boolean updateActiveStreamW(int i) {
        if (i == this.mState.activeStream) {
            return false;
        }
        this.mState.activeStream = i;
        Events.writeEvent(this.mContext, 2, Integer.valueOf(i));
        if (D.BUG) {
            Log.d(TAG, "updateActiveStreamW " + i);
        }
        if (i >= 100) {
            i = -1;
        }
        if (D.BUG) {
            Log.d(TAG, "forceVolumeControlStream " + i);
        }
        this.mAudio.forceVolumeControlStream(i);
        return true;
    }

    private VolumeDialogController.StreamState streamStateW(int i) {
        VolumeDialogController.StreamState streamState = this.mState.states.get(i);
        if (streamState == null) {
            VolumeDialogController.StreamState streamState2 = new VolumeDialogController.StreamState();
            this.mState.states.put(i, streamState2);
            return streamState2;
        }
        return streamState;
    }

    private void onGetStateW() {
        Iterator<Integer> it = STREAMS.keySet().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            updateStreamLevelW(iIntValue, getAudioManagerStreamVolume(iIntValue));
            streamStateW(iIntValue).levelMin = getAudioManagerStreamMinVolume(iIntValue);
            streamStateW(iIntValue).levelMax = Math.max(1, getAudioManagerStreamMaxVolume(iIntValue));
            updateStreamMuteW(iIntValue, this.mAudio.isStreamMute(iIntValue));
            VolumeDialogController.StreamState streamStateStreamStateW = streamStateW(iIntValue);
            streamStateStreamStateW.muteSupported = this.mAudio.isStreamAffectedByMute(iIntValue);
            streamStateStreamStateW.name = STREAMS.get(Integer.valueOf(iIntValue)).intValue();
            checkRoutedToBluetoothW(iIntValue);
        }
        updateRingerModeExternalW(this.mAudio.getRingerMode());
        updateZenModeW();
        updateZenConfig();
        updateEffectsSuppressorW(this.mNoMan.getEffectsSuppressor());
        this.mCallbacks.onStateChanged(this.mState);
    }

    private boolean updateStreamRoutedToBluetoothW(int i, boolean z) {
        VolumeDialogController.StreamState streamStateStreamStateW = streamStateW(i);
        if (streamStateStreamStateW.routedToBluetooth == z) {
            return false;
        }
        streamStateStreamStateW.routedToBluetooth = z;
        if (D.BUG) {
            Log.d(TAG, "updateStreamRoutedToBluetoothW stream=" + i + " routedToBluetooth=" + z);
            return true;
        }
        return true;
    }

    private boolean updateStreamLevelW(int i, int i2) {
        VolumeDialogController.StreamState streamStateStreamStateW = streamStateW(i);
        if (streamStateStreamStateW.level == i2) {
            return false;
        }
        streamStateStreamStateW.level = i2;
        if (isLogWorthy(i)) {
            Events.writeEvent(this.mContext, 10, Integer.valueOf(i), Integer.valueOf(i2));
        }
        return true;
    }

    private static boolean isLogWorthy(int i) {
        if (i != 6) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }

    private boolean updateStreamMuteW(int i, boolean z) {
        VolumeDialogController.StreamState streamStateStreamStateW = streamStateW(i);
        if (streamStateStreamStateW.muted == z) {
            return false;
        }
        streamStateStreamStateW.muted = z;
        if (isLogWorthy(i)) {
            Events.writeEvent(this.mContext, 15, Integer.valueOf(i), Boolean.valueOf(z));
        }
        if (z && isRinger(i)) {
            updateRingerModeInternalW(this.mAudio.getRingerModeInternal());
        }
        return true;
    }

    private static boolean isRinger(int i) {
        return i == 2 || i == 5;
    }

    private boolean updateEffectsSuppressorW(ComponentName componentName) {
        if (Objects.equals(this.mState.effectsSuppressor, componentName)) {
            return false;
        }
        this.mState.effectsSuppressor = componentName;
        this.mState.effectsSuppressorName = getApplicationName(this.mContext, this.mState.effectsSuppressor);
        Events.writeEvent(this.mContext, 14, this.mState.effectsSuppressor, this.mState.effectsSuppressorName);
        return true;
    }

    private static String getApplicationName(Context context, ComponentName componentName) {
        String strTrim;
        if (componentName == null) {
            return null;
        }
        PackageManager packageManager = context.getPackageManager();
        String packageName = componentName.getPackageName();
        try {
            strTrim = Objects.toString(packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager), "").trim();
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (strTrim.length() > 0) {
            return strTrim;
        }
        return packageName;
    }

    private boolean updateZenModeW() {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
        if (this.mState.zenMode == i) {
            return false;
        }
        this.mState.zenMode = i;
        Events.writeEvent(this.mContext, 13, Integer.valueOf(i));
        return true;
    }

    private boolean updateZenConfig() {
        NotificationManager.Policy notificationPolicy = this.mNotificationManager.getNotificationPolicy();
        boolean z = (notificationPolicy.priorityCategories & 32) == 0;
        boolean z2 = (notificationPolicy.priorityCategories & 64) == 0;
        boolean z3 = (notificationPolicy.priorityCategories & 128) == 0;
        boolean zAreAllPriorityOnlyNotificationZenSoundsMuted = ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(notificationPolicy);
        if (this.mState.disallowAlarms == z && this.mState.disallowMedia == z2 && this.mState.disallowRinger == zAreAllPriorityOnlyNotificationZenSoundsMuted && this.mState.disallowSystem == z3) {
            return false;
        }
        this.mState.disallowAlarms = z;
        this.mState.disallowMedia = z2;
        this.mState.disallowSystem = z3;
        this.mState.disallowRinger = zAreAllPriorityOnlyNotificationZenSoundsMuted;
        Events.writeEvent(this.mContext, 17, "disallowAlarms=" + z + " disallowMedia=" + z2 + " disallowSystem=" + z3 + " disallowRinger=" + zAreAllPriorityOnlyNotificationZenSoundsMuted);
        return true;
    }

    private boolean updateRingerModeExternalW(int i) {
        if (i == this.mState.ringerModeExternal) {
            return false;
        }
        this.mState.ringerModeExternal = i;
        Events.writeEvent(this.mContext, 12, Integer.valueOf(i));
        return true;
    }

    private boolean updateRingerModeInternalW(int i) {
        if (i == this.mState.ringerModeInternal) {
            return false;
        }
        this.mState.ringerModeInternal = i;
        Events.writeEvent(this.mContext, 11, Integer.valueOf(i));
        if (this.mState.ringerModeInternal == 2) {
            playTouchFeedback();
        }
        return true;
    }

    private void onSetRingerModeW(int i, boolean z) {
        if (z) {
            this.mAudio.setRingerMode(i);
        } else {
            this.mAudio.setRingerModeInternal(i);
        }
    }

    private void onSetStreamMuteW(int i, boolean z) {
        this.mAudio.adjustStreamVolume(i, z ? -100 : 100, 0);
    }

    private void onSetStreamVolumeW(int i, int i2) {
        if (D.BUG) {
            Log.d(TAG, "onSetStreamVolume " + i + " level=" + i2);
        }
        if (i >= 100) {
            this.mMediaSessionsCallbacksW.setStreamVolume(i, i2);
        } else {
            setAudioManagerStreamVolume(i, i2, 0);
        }
    }

    private void onSetActiveStreamW(int i) {
        if (updateActiveStreamW(i)) {
            this.mCallbacks.onStateChanged(this.mState);
        }
    }

    private void onSetExitConditionW(Condition condition) {
        this.mNoMan.setZenMode(this.mState.zenMode, condition != null ? condition.id : null, TAG);
    }

    private void onSetZenModeW(int i) {
        if (D.BUG) {
            Log.d(TAG, "onSetZenModeW " + i);
        }
        this.mNoMan.setZenMode(i, null, TAG);
    }

    private void onDismissRequestedW(int i) {
        this.mCallbacks.onDismissRequested(i);
    }

    public void showDndTile(boolean z) {
        if (D.BUG) {
            Log.d(TAG, "showDndTile");
        }
        DndTile.setVisible(this.mContext, z);
    }

    private final class VC extends IVolumeController.Stub {
        private final String TAG;

        private VC() {
            this.TAG = VolumeDialogControllerImpl.TAG + ".VC";
        }

        /* synthetic */ VC(VolumeDialogControllerImpl volumeDialogControllerImpl, AnonymousClass1 anonymousClass1) {
            this();
        }

        public void displaySafeVolumeWarning(int i) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "displaySafeVolumeWarning " + Util.audioManagerFlagsToString(i));
            }
            if (VolumeDialogControllerImpl.this.mDestroyed) {
                return;
            }
            VolumeDialogControllerImpl.this.mWorker.obtainMessage(14, i, 0).sendToTarget();
        }

        public void volumeChanged(int i, int i2) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "volumeChanged " + AudioSystem.streamToString(i) + " " + Util.audioManagerFlagsToString(i2));
            }
            if (VolumeDialogControllerImpl.this.mDestroyed) {
                return;
            }
            VolumeDialogControllerImpl.this.mWorker.obtainMessage(1, i, i2).sendToTarget();
        }

        public void masterMuteChanged(int i) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "masterMuteChanged");
            }
        }

        public void setLayoutDirection(int i) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "setLayoutDirection");
            }
            if (VolumeDialogControllerImpl.this.mDestroyed) {
                return;
            }
            VolumeDialogControllerImpl.this.mWorker.obtainMessage(8, i, 0).sendToTarget();
        }

        public void dismiss() throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "dismiss requested");
            }
            if (VolumeDialogControllerImpl.this.mDestroyed) {
                return;
            }
            VolumeDialogControllerImpl.this.mWorker.obtainMessage(2, 2, 0).sendToTarget();
            VolumeDialogControllerImpl.this.mWorker.sendEmptyMessage(2);
        }

        public void setA11yMode(int i) {
            if (D.BUG) {
                Log.d(this.TAG, "setA11yMode to " + i);
            }
            if (VolumeDialogControllerImpl.this.mDestroyed) {
                return;
            }
            switch (i) {
                case 0:
                    VolumeDialogControllerImpl.this.mShowA11yStream = false;
                    break;
                case 1:
                    VolumeDialogControllerImpl.this.mShowA11yStream = true;
                    break;
                default:
                    Log.e(this.TAG, "Invalid accessibility mode " + i);
                    break;
            }
            VolumeDialogControllerImpl.this.mWorker.obtainMessage(15, Boolean.valueOf(VolumeDialogControllerImpl.this.mShowA11yStream)).sendToTarget();
        }
    }

    private final class W extends Handler {
        W(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    VolumeDialogControllerImpl.this.onVolumeChangedW(message.arg1, message.arg2);
                    break;
                case 2:
                    VolumeDialogControllerImpl.this.onDismissRequestedW(message.arg1);
                    break;
                case 3:
                    VolumeDialogControllerImpl.this.onGetStateW();
                    break;
                case 4:
                    VolumeDialogControllerImpl.this.onSetRingerModeW(message.arg1, message.arg2 != 0);
                    break;
                case 5:
                    VolumeDialogControllerImpl.this.onSetZenModeW(message.arg1);
                    break;
                case 6:
                    VolumeDialogControllerImpl.this.onSetExitConditionW((Condition) message.obj);
                    break;
                case 7:
                    VolumeDialogControllerImpl.this.onSetStreamMuteW(message.arg1, message.arg2 != 0);
                    break;
                case 8:
                    VolumeDialogControllerImpl.this.mCallbacks.onLayoutDirectionChanged(message.arg1);
                    break;
                case 9:
                    VolumeDialogControllerImpl.this.mCallbacks.onConfigurationChanged();
                    break;
                case 10:
                    VolumeDialogControllerImpl.this.onSetStreamVolumeW(message.arg1, message.arg2);
                    break;
                case 11:
                    VolumeDialogControllerImpl.this.onSetActiveStreamW(message.arg1);
                    break;
                case 12:
                    VolumeDialogControllerImpl.this.onNotifyVisibleW(message.arg1 != 0);
                    break;
                case 13:
                    VolumeDialogControllerImpl.this.onUserActivityW();
                    break;
                case 14:
                    VolumeDialogControllerImpl.this.onShowSafetyWarningW(message.arg1);
                    break;
                case 15:
                    VolumeDialogControllerImpl.this.onAccessibilityModeChanged((Boolean) message.obj);
                    break;
            }
        }
    }

    class C implements VolumeDialogController.Callbacks {
        private final HashMap<VolumeDialogController.Callbacks, Handler> mCallbackMap = new HashMap<>();

        C() {
        }

        public void add(VolumeDialogController.Callbacks callbacks, Handler handler) {
            if (callbacks == null || handler == null) {
                throw new IllegalArgumentException();
            }
            this.mCallbackMap.put(callbacks, handler);
        }

        public void remove(VolumeDialogController.Callbacks callbacks) {
            this.mCallbackMap.remove(callbacks);
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$1 */
        class AnonymousClass1 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;
            final /* synthetic */ int val$reason;

            AnonymousClass1(Map.Entry entry, int i) {
                entry = entry;
                i = i;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onShowRequested(i);
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onShowRequested(int i) {
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.1
                    final /* synthetic */ Map.Entry val$entry;
                    final /* synthetic */ int val$reason;

                    AnonymousClass1(Map.Entry entry2, int i2) {
                        entry = entry2;
                        i = i2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onShowRequested(i);
                    }
                });
            }
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$2 */
        class AnonymousClass2 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;
            final /* synthetic */ int val$reason;

            AnonymousClass2(Map.Entry entry, int i) {
                entry = entry;
                i = i;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onDismissRequested(i);
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onDismissRequested(int i) {
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.2
                    final /* synthetic */ Map.Entry val$entry;
                    final /* synthetic */ int val$reason;

                    AnonymousClass2(Map.Entry entry2, int i2) {
                        entry = entry2;
                        i = i2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onDismissRequested(i);
                    }
                });
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onStateChanged(VolumeDialogController.State state) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            VolumeDialogController.State stateCopy = state.copy();
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.3
                    final /* synthetic */ VolumeDialogController.State val$copy;
                    final /* synthetic */ Map.Entry val$entry;

                    AnonymousClass3(Map.Entry entry2, VolumeDialogController.State stateCopy2) {
                        entry = entry2;
                        state = stateCopy2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onStateChanged(state);
                    }
                });
            }
            Events.writeState(jCurrentTimeMillis, stateCopy2);
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$3 */
        class AnonymousClass3 implements Runnable {
            final /* synthetic */ VolumeDialogController.State val$copy;
            final /* synthetic */ Map.Entry val$entry;

            AnonymousClass3(Map.Entry entry2, VolumeDialogController.State stateCopy2) {
                entry = entry2;
                state = stateCopy2;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onStateChanged(state);
            }
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$4 */
        class AnonymousClass4 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;
            final /* synthetic */ int val$layoutDirection;

            AnonymousClass4(Map.Entry entry, int i) {
                entry = entry;
                i = i;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onLayoutDirectionChanged(i);
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onLayoutDirectionChanged(int i) {
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.4
                    final /* synthetic */ Map.Entry val$entry;
                    final /* synthetic */ int val$layoutDirection;

                    AnonymousClass4(Map.Entry entry2, int i2) {
                        entry = entry2;
                        i = i2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onLayoutDirectionChanged(i);
                    }
                });
            }
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$5 */
        class AnonymousClass5 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;

            AnonymousClass5(Map.Entry entry) {
                entry = entry;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onConfigurationChanged();
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onConfigurationChanged() {
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.5
                    final /* synthetic */ Map.Entry val$entry;

                    AnonymousClass5(Map.Entry entry2) {
                        entry = entry2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onConfigurationChanged();
                    }
                });
            }
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$6 */
        class AnonymousClass6 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;

            AnonymousClass6(Map.Entry entry) {
                entry = entry;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onShowVibrateHint();
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onShowVibrateHint() {
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.6
                    final /* synthetic */ Map.Entry val$entry;

                    AnonymousClass6(Map.Entry entry2) {
                        entry = entry2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onShowVibrateHint();
                    }
                });
            }
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$7 */
        class AnonymousClass7 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;

            AnonymousClass7(Map.Entry entry) {
                entry = entry;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onShowSilentHint();
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onShowSilentHint() {
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.7
                    final /* synthetic */ Map.Entry val$entry;

                    AnonymousClass7(Map.Entry entry2) {
                        entry = entry2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onShowSilentHint();
                    }
                });
            }
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$8 */
        class AnonymousClass8 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;

            AnonymousClass8(Map.Entry entry) {
                entry = entry;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onScreenOff();
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onScreenOff() {
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.8
                    final /* synthetic */ Map.Entry val$entry;

                    AnonymousClass8(Map.Entry entry2) {
                        entry = entry2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onScreenOff();
                    }
                });
            }
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$9 */
        class AnonymousClass9 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;
            final /* synthetic */ int val$flags;

            AnonymousClass9(Map.Entry entry, int i) {
                entry = entry;
                i = i;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onShowSafetyWarning(i);
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onShowSafetyWarning(int i) {
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.9
                    final /* synthetic */ Map.Entry val$entry;
                    final /* synthetic */ int val$flags;

                    AnonymousClass9(Map.Entry entry2, int i2) {
                        entry = entry2;
                        i = i2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onShowSafetyWarning(i);
                    }
                });
            }
        }

        @Override // com.android.systemui.plugins.VolumeDialogController.Callbacks
        public void onAccessibilityModeChanged(Boolean bool) {
            boolean zBooleanValue = bool == null ? false : bool.booleanValue();
            for (Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() { // from class: com.android.systemui.volume.VolumeDialogControllerImpl.C.10
                    final /* synthetic */ Map.Entry val$entry;
                    final /* synthetic */ boolean val$show;

                    AnonymousClass10(Map.Entry entry2, boolean zBooleanValue2) {
                        entry = entry2;
                        z = zBooleanValue2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onAccessibilityModeChanged(Boolean.valueOf(z));
                    }
                });
            }
        }

        /* renamed from: com.android.systemui.volume.VolumeDialogControllerImpl$C$10 */
        class AnonymousClass10 implements Runnable {
            final /* synthetic */ Map.Entry val$entry;
            final /* synthetic */ boolean val$show;

            AnonymousClass10(Map.Entry entry2, boolean zBooleanValue2) {
                entry = entry2;
                z = zBooleanValue2;
            }

            @Override // java.lang.Runnable
            public void run() {
                ((VolumeDialogController.Callbacks) entry.getKey()).onAccessibilityModeChanged(Boolean.valueOf(z));
            }
        }
    }

    private final class SettingObserver extends ContentObserver {
        private final Uri ZEN_MODE_CONFIG_URI;
        private final Uri ZEN_MODE_URI;

        public SettingObserver(Handler handler) {
            super(handler);
            this.ZEN_MODE_URI = Settings.Global.getUriFor("zen_mode");
            this.ZEN_MODE_CONFIG_URI = Settings.Global.getUriFor("zen_mode_config_etag");
        }

        public void init() {
            VolumeDialogControllerImpl.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE_URI, false, this);
            VolumeDialogControllerImpl.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE_CONFIG_URI, false, this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            boolean zUpdateZenConfig;
            if (this.ZEN_MODE_URI.equals(uri)) {
                zUpdateZenConfig = VolumeDialogControllerImpl.this.updateZenModeW();
            } else {
                zUpdateZenConfig = false;
            }
            if (this.ZEN_MODE_CONFIG_URI.equals(uri)) {
                zUpdateZenConfig |= VolumeDialogControllerImpl.this.updateZenConfig();
            }
            if (zUpdateZenConfig) {
                VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
            }
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        /* synthetic */ Receiver(VolumeDialogControllerImpl volumeDialogControllerImpl, AnonymousClass1 anonymousClass1) {
            this();
        }

        public void init() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
            intentFilter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
            intentFilter.addAction("android.media.RINGER_MODE_CHANGED");
            intentFilter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
            intentFilter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
            intentFilter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
            intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            VolumeDialogControllerImpl.this.mContext.registerReceiver(this, intentFilter, null, VolumeDialogControllerImpl.this.mWorker);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean zUpdateEffectsSuppressorW = false;
            if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                int intExtra = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                int intExtra2 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                int intExtra3 = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1);
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive VOLUME_CHANGED_ACTION stream=" + intExtra + " level=" + intExtra2 + " oldLevel=" + intExtra3);
                }
                zUpdateEffectsSuppressorW = VolumeDialogControllerImpl.this.updateStreamLevelW(intExtra, intExtra2);
            } else if (action.equals("android.media.STREAM_DEVICES_CHANGED_ACTION")) {
                int intExtra4 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                int intExtra5 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_DEVICES", -1);
                int intExtra6 = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES", -1);
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive STREAM_DEVICES_CHANGED_ACTION stream=" + intExtra4 + " devices=" + intExtra5 + " oldDevices=" + intExtra6);
                }
                zUpdateEffectsSuppressorW = VolumeDialogControllerImpl.this.checkRoutedToBluetoothW(intExtra4) | VolumeDialogControllerImpl.this.onVolumeChangedW(intExtra4, 0);
            } else if (action.equals("android.media.RINGER_MODE_CHANGED")) {
                int intExtra7 = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive RINGER_MODE_CHANGED_ACTION rm=" + Util.ringerModeToString(intExtra7));
                }
                zUpdateEffectsSuppressorW = VolumeDialogControllerImpl.this.updateRingerModeExternalW(intExtra7);
            } else if (action.equals("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")) {
                int intExtra8 = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive INTERNAL_RINGER_MODE_CHANGED_ACTION rm=" + Util.ringerModeToString(intExtra8));
                }
                zUpdateEffectsSuppressorW = VolumeDialogControllerImpl.this.updateRingerModeInternalW(intExtra8);
            } else if (action.equals("android.media.STREAM_MUTE_CHANGED_ACTION")) {
                int intExtra9 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                boolean booleanExtra = intent.getBooleanExtra("android.media.EXTRA_STREAM_VOLUME_MUTED", false);
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive STREAM_MUTE_CHANGED_ACTION stream=" + intExtra9 + " muted=" + booleanExtra);
                }
                zUpdateEffectsSuppressorW = VolumeDialogControllerImpl.this.updateStreamMuteW(intExtra9, booleanExtra);
            } else if (action.equals("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED")) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive ACTION_EFFECTS_SUPPRESSOR_CHANGED");
                }
                zUpdateEffectsSuppressorW = VolumeDialogControllerImpl.this.updateEffectsSuppressorW(VolumeDialogControllerImpl.this.mNoMan.getEffectsSuppressor());
            } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive ACTION_CONFIGURATION_CHANGED");
                }
                VolumeDialogControllerImpl.this.mCallbacks.onConfigurationChanged();
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive ACTION_SCREEN_OFF");
                }
                VolumeDialogControllerImpl.this.mCallbacks.onScreenOff();
            } else if (action.equals("android.intent.action.CLOSE_SYSTEM_DIALOGS")) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive ACTION_CLOSE_SYSTEM_DIALOGS");
                }
                VolumeDialogControllerImpl.this.dismiss();
            }
            if (zUpdateEffectsSuppressorW) {
                VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
            }
        }
    }

    protected final class MediaSessionsCallbacks implements MediaSessions.Callbacks {
        private final HashMap<MediaSession.Token, Integer> mRemoteStreams = new HashMap<>();
        private int mNextStream = 100;

        protected MediaSessionsCallbacks() {
        }

        @Override // com.android.systemui.volume.MediaSessions.Callbacks
        public void onRemoteUpdate(MediaSession.Token token, String str, MediaController.PlaybackInfo playbackInfo) {
            addStream(token, "onRemoteUpdate");
            int iIntValue = this.mRemoteStreams.get(token).intValue();
            boolean z = VolumeDialogControllerImpl.this.mState.states.indexOfKey(iIntValue) < 0;
            VolumeDialogController.StreamState streamStateStreamStateW = VolumeDialogControllerImpl.this.streamStateW(iIntValue);
            streamStateStreamStateW.dynamic = true;
            streamStateStreamStateW.levelMin = 0;
            streamStateStreamStateW.levelMax = playbackInfo.getMaxVolume();
            if (streamStateStreamStateW.level != playbackInfo.getCurrentVolume()) {
                streamStateStreamStateW.level = playbackInfo.getCurrentVolume();
                z = true;
            }
            if (!Objects.equals(streamStateStreamStateW.remoteLabel, str)) {
                streamStateStreamStateW.name = -1;
                streamStateStreamStateW.remoteLabel = str;
                z = true;
            }
            if (z) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onRemoteUpdate: " + str + ": " + streamStateStreamStateW.level + " of " + streamStateStreamStateW.levelMax);
                }
                VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
            }
        }

        @Override // com.android.systemui.volume.MediaSessions.Callbacks
        public void onRemoteVolumeChanged(MediaSession.Token token, int i) {
            addStream(token, "onRemoteVolumeChanged");
            int iIntValue = this.mRemoteStreams.get(token).intValue();
            boolean zShouldShowUI = VolumeDialogControllerImpl.this.shouldShowUI(i);
            boolean zUpdateActiveStreamW = VolumeDialogControllerImpl.this.updateActiveStreamW(iIntValue);
            if (zShouldShowUI) {
                zUpdateActiveStreamW |= VolumeDialogControllerImpl.this.checkRoutedToBluetoothW(3);
            }
            if (zUpdateActiveStreamW) {
                VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
            }
            if (zShouldShowUI) {
                VolumeDialogControllerImpl.this.mCallbacks.onShowRequested(2);
            }
        }

        @Override // com.android.systemui.volume.MediaSessions.Callbacks
        public void onRemoteRemoved(MediaSession.Token token) {
            if (!this.mRemoteStreams.containsKey(token)) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onRemoteRemoved: stream doesn't exist, aborting remote removed for token:" + token.toString());
                    return;
                }
                return;
            }
            int iIntValue = this.mRemoteStreams.get(token).intValue();
            VolumeDialogControllerImpl.this.mState.states.remove(iIntValue);
            if (VolumeDialogControllerImpl.this.mState.activeStream == iIntValue) {
                VolumeDialogControllerImpl.this.updateActiveStreamW(-1);
            }
            VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
        }

        public void setStreamVolume(int i, int i2) {
            MediaSession.Token tokenFindToken = findToken(i);
            if (tokenFindToken == null) {
                Log.w(VolumeDialogControllerImpl.TAG, "setStreamVolume: No token found for stream: " + i);
                return;
            }
            VolumeDialogControllerImpl.this.mMediaSessions.setVolume(tokenFindToken, i2);
        }

        private MediaSession.Token findToken(int i) {
            for (Map.Entry<MediaSession.Token, Integer> entry : this.mRemoteStreams.entrySet()) {
                if (entry.getValue().equals(Integer.valueOf(i))) {
                    return entry.getKey();
                }
            }
            return null;
        }

        private void addStream(MediaSession.Token token, String str) {
            if (!this.mRemoteStreams.containsKey(token)) {
                this.mRemoteStreams.put(token, Integer.valueOf(this.mNextStream));
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, str + ": added stream " + this.mNextStream + " from token + " + token.toString());
                }
                this.mNextStream++;
            }
        }
    }
}
