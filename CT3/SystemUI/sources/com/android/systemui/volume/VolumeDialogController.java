package com.android.systemui.volume;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioSystem;
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
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.Condition;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.R;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.volume.MediaSessions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VolumeDialogController {
    private final AudioManager mAudio;
    private final ComponentName mComponent;
    private final Context mContext;
    private boolean mDestroyed;
    private boolean mEnabled;
    private final boolean mHasVibrator;
    private final MediaSessions mMediaSessions;
    private final NotificationManager mNoMan;
    private final SettingObserver mObserver;
    private final String[] mStreamTitles;
    private final Vibrator mVibrator;
    private VolumePolicy mVolumePolicy;
    private final W mWorker;
    private final HandlerThread mWorkerThread;
    private static final String TAG = Util.logTag(VolumeDialogController.class);
    private static final int[] STREAMS = {4, 6, 8, 3, 5, 2, 1, 7, 9, 0};
    private final Receiver mReceiver = new Receiver(this, null);
    private final VC mVolumeController = new VC(this, 0 == true ? 1 : 0);
    private final C mCallbacks = new C(this, 0 == true ? 1 : 0);
    private final State mState = new State();
    private final MediaSessionsCallbacks mMediaSessionsCallbacksW = new MediaSessionsCallbacks(this, 0 == true ? 1 : 0);
    private boolean mShowDndTile = true;

    public interface Callbacks {
        void onConfigurationChanged();

        void onDismissRequested(int i);

        void onLayoutDirectionChanged(int i);

        void onScreenOff();

        void onShowRequested(int i);

        void onShowSafetyWarning(int i);

        void onShowSilentHint();

        void onShowVibrateHint();

        void onStateChanged(State state);
    }

    public VolumeDialogController(Context context, ComponentName componentName) {
        this.mContext = context.getApplicationContext();
        Events.writeEvent(this.mContext, 5, new Object[0]);
        this.mComponent = componentName;
        this.mWorkerThread = new HandlerThread(VolumeDialogController.class.getSimpleName());
        this.mWorkerThread.start();
        this.mWorker = new W(this.mWorkerThread.getLooper());
        this.mMediaSessions = createMediaSessions(this.mContext, this.mWorkerThread.getLooper(), this.mMediaSessionsCallbacksW);
        this.mAudio = (AudioManager) this.mContext.getSystemService("audio");
        this.mNoMan = (NotificationManager) this.mContext.getSystemService("notification");
        this.mObserver = new SettingObserver(this.mWorker);
        this.mObserver.init();
        this.mReceiver.init();
        this.mStreamTitles = this.mContext.getResources().getStringArray(R.array.volume_stream_titles);
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mHasVibrator = this.mVibrator != null ? this.mVibrator.hasVibrator() : false;
    }

    public AudioManager getAudioManager() {
        return this.mAudio;
    }

    public void dismiss() {
        this.mCallbacks.onDismissRequested(2);
    }

    public void register() {
        try {
            this.mAudio.setVolumeController(this.mVolumeController);
            setVolumePolicy(this.mVolumePolicy);
            showDndTile(this.mShowDndTile);
            try {
                this.mMediaSessions.init();
            } catch (SecurityException e) {
                Log.w(TAG, "No access to media sessions", e);
            }
        } catch (SecurityException e2) {
            Log.w(TAG, "Unable to set the volume controller", e2);
        }
    }

    public void setVolumePolicy(VolumePolicy policy) {
        this.mVolumePolicy = policy;
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

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println(VolumeDialogController.class.getSimpleName() + " state:");
        pw.print("  mEnabled: ");
        pw.println(this.mEnabled);
        pw.print("  mDestroyed: ");
        pw.println(this.mDestroyed);
        pw.print("  mVolumePolicy: ");
        pw.println(this.mVolumePolicy);
        pw.print("  mState: ");
        pw.println(this.mState.toString(4));
        pw.print("  mShowDndTile: ");
        pw.println(this.mShowDndTile);
        pw.print("  mHasVibrator: ");
        pw.println(this.mHasVibrator);
        pw.print("  mRemoteStreams: ");
        pw.println(this.mMediaSessionsCallbacksW.mRemoteStreams.values());
        pw.println();
        this.mMediaSessions.dump(pw);
    }

    public void addCallback(Callbacks callback, Handler handler) {
        this.mCallbacks.add(callback, handler);
    }

    public void getState() {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.sendEmptyMessage(3);
    }

    public void notifyVisible(boolean visible) {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.obtainMessage(12, visible ? 1 : 0, 0).sendToTarget();
    }

    public void userActivity() {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.removeMessages(13);
        this.mWorker.sendEmptyMessage(13);
    }

    public void setRingerMode(int value, boolean external) {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.obtainMessage(4, value, external ? 1 : 0).sendToTarget();
    }

    public void setStreamVolume(int stream, int level) {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.obtainMessage(10, stream, level).sendToTarget();
    }

    public void setActiveStream(int stream) {
        if (this.mDestroyed) {
            return;
        }
        this.mWorker.obtainMessage(11, stream, 0).sendToTarget();
    }

    public void vibrate() {
        if (!this.mHasVibrator) {
            return;
        }
        this.mVibrator.vibrate(50L);
    }

    public boolean hasVibrator() {
        return this.mHasVibrator;
    }

    public void onNotifyVisibleW(boolean visible) {
        if (this.mDestroyed) {
            return;
        }
        this.mAudio.notifyVolumeControllerVisible(this.mVolumeController, visible);
        if (visible || !updateActiveStreamW(-1)) {
            return;
        }
        this.mCallbacks.onStateChanged(this.mState);
    }

    protected void onUserActivityW() {
    }

    public void onShowSafetyWarningW(int flags) {
        this.mCallbacks.onShowSafetyWarning(flags);
    }

    public boolean checkRoutedToBluetoothW(int stream) {
        if (stream != 3) {
            return false;
        }
        boolean routedToBluetooth = (this.mAudio.getDevicesForStream(3) & 896) != 0;
        boolean changed = updateStreamRoutedToBluetoothW(stream, routedToBluetooth);
        return changed;
    }

    public boolean onVolumeChangedW(int stream, int flags) {
        boolean showUI = (flags & 1) != 0;
        boolean fromKey = (flags & 4096) != 0;
        boolean showVibrateHint = (flags & 2048) != 0;
        boolean showSilentHint = (flags & 128) != 0;
        boolean changed = false;
        if (showUI) {
            changed = updateActiveStreamW(stream);
        }
        int lastAudibleStreamVolume = this.mAudio.getLastAudibleStreamVolume(stream);
        boolean changed2 = changed | updateStreamLevelW(stream, lastAudibleStreamVolume) | checkRoutedToBluetoothW(showUI ? 3 : stream);
        if (changed2) {
            this.mCallbacks.onStateChanged(this.mState);
        }
        if (showUI) {
            this.mCallbacks.onShowRequested(1);
        }
        if (showVibrateHint) {
            this.mCallbacks.onShowVibrateHint();
        }
        if (showSilentHint) {
            this.mCallbacks.onShowSilentHint();
        }
        if (changed2 && fromKey) {
            Events.writeEvent(this.mContext, 4, Integer.valueOf(stream), Integer.valueOf(lastAudibleStreamVolume));
        }
        return changed2;
    }

    public boolean updateActiveStreamW(int activeStream) {
        if (activeStream == this.mState.activeStream) {
            return false;
        }
        this.mState.activeStream = activeStream;
        Events.writeEvent(this.mContext, 2, Integer.valueOf(activeStream));
        if (D.BUG) {
            Log.d(TAG, "updateActiveStreamW " + activeStream);
        }
        int s = activeStream < 100 ? activeStream : -1;
        if (D.BUG) {
            Log.d(TAG, "forceVolumeControlStream " + s);
        }
        this.mAudio.forceVolumeControlStream(s);
        return true;
    }

    public StreamState streamStateW(int stream) {
        StreamState ss = this.mState.states.get(stream);
        if (ss == null) {
            StreamState ss2 = new StreamState();
            this.mState.states.put(stream, ss2);
            return ss2;
        }
        return ss;
    }

    public void onGetStateW() {
        for (int stream : STREAMS) {
            updateStreamLevelW(stream, this.mAudio.getLastAudibleStreamVolume(stream));
            streamStateW(stream).levelMin = this.mAudio.getStreamMinVolume(stream);
            streamStateW(stream).levelMax = this.mAudio.getStreamMaxVolume(stream);
            updateStreamMuteW(stream, this.mAudio.isStreamMute(stream));
            StreamState ss = streamStateW(stream);
            ss.muteSupported = this.mAudio.isStreamAffectedByMute(stream);
            ss.name = this.mStreamTitles[stream];
            checkRoutedToBluetoothW(stream);
        }
        updateRingerModeExternalW(this.mAudio.getRingerMode());
        updateZenModeW();
        updateEffectsSuppressorW(this.mNoMan.getEffectsSuppressor());
        this.mCallbacks.onStateChanged(this.mState);
    }

    private boolean updateStreamRoutedToBluetoothW(int stream, boolean routedToBluetooth) {
        StreamState ss = streamStateW(stream);
        if (ss.routedToBluetooth == routedToBluetooth) {
            return false;
        }
        ss.routedToBluetooth = routedToBluetooth;
        if (D.BUG) {
            Log.d(TAG, "updateStreamRoutedToBluetoothW stream=" + stream + " routedToBluetooth=" + routedToBluetooth);
            return true;
        }
        return true;
    }

    public boolean updateStreamLevelW(int stream, int level) {
        StreamState ss = streamStateW(stream);
        if (ss.level == level) {
            return false;
        }
        ss.level = level;
        if (isLogWorthy(stream)) {
            Events.writeEvent(this.mContext, 10, Integer.valueOf(stream), Integer.valueOf(level));
        }
        return true;
    }

    private static boolean isLogWorthy(int stream) {
        switch (stream) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 6:
                return true;
            case 5:
            default:
                return false;
        }
    }

    public boolean updateStreamMuteW(int stream, boolean muted) {
        StreamState ss = streamStateW(stream);
        if (ss.muted == muted) {
            return false;
        }
        ss.muted = muted;
        if (isLogWorthy(stream)) {
            Events.writeEvent(this.mContext, 15, Integer.valueOf(stream), Boolean.valueOf(muted));
        }
        if (muted && isRinger(stream)) {
            updateRingerModeInternalW(this.mAudio.getRingerModeInternal());
        }
        return true;
    }

    private static boolean isRinger(int stream) {
        return stream == 2 || stream == 5;
    }

    public boolean updateEffectsSuppressorW(ComponentName effectsSuppressor) {
        if (Objects.equals(this.mState.effectsSuppressor, effectsSuppressor)) {
            return false;
        }
        this.mState.effectsSuppressor = effectsSuppressor;
        this.mState.effectsSuppressorName = getApplicationName(this.mContext, this.mState.effectsSuppressor);
        Events.writeEvent(this.mContext, 14, this.mState.effectsSuppressor, this.mState.effectsSuppressorName);
        return true;
    }

    private static String getApplicationName(Context context, ComponentName component) {
        String rt;
        if (component == null) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        String pkg = component.getPackageName();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            rt = Objects.toString(ai.loadLabel(pm), "").trim();
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (rt.length() > 0) {
            return rt;
        }
        return pkg;
    }

    public boolean updateZenModeW() {
        int zen = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
        if (this.mState.zenMode == zen) {
            return false;
        }
        this.mState.zenMode = zen;
        Events.writeEvent(this.mContext, 13, Integer.valueOf(zen));
        return true;
    }

    public boolean updateRingerModeExternalW(int rm) {
        if (rm == this.mState.ringerModeExternal) {
            return false;
        }
        this.mState.ringerModeExternal = rm;
        Events.writeEvent(this.mContext, 12, Integer.valueOf(rm));
        return true;
    }

    public boolean updateRingerModeInternalW(int rm) {
        if (rm == this.mState.ringerModeInternal) {
            return false;
        }
        this.mState.ringerModeInternal = rm;
        Events.writeEvent(this.mContext, 11, Integer.valueOf(rm));
        return true;
    }

    public void onSetRingerModeW(int mode, boolean external) {
        if (external) {
            this.mAudio.setRingerMode(mode);
        } else {
            this.mAudio.setRingerModeInternal(mode);
        }
    }

    public void onSetStreamMuteW(int stream, boolean mute) {
        this.mAudio.adjustStreamVolume(stream, mute ? -100 : 100, 0);
    }

    public void onSetStreamVolumeW(int stream, int level) {
        if (D.BUG) {
            Log.d(TAG, "onSetStreamVolume " + stream + " level=" + level);
        }
        if (stream >= 100) {
            this.mMediaSessionsCallbacksW.setStreamVolume(stream, level);
        } else {
            this.mAudio.setStreamVolume(stream, level, 0);
        }
    }

    public void onSetActiveStreamW(int stream) {
        boolean changed = updateActiveStreamW(stream);
        if (!changed) {
            return;
        }
        this.mCallbacks.onStateChanged(this.mState);
    }

    public void onSetExitConditionW(Condition condition) {
        this.mNoMan.setZenMode(this.mState.zenMode, condition != null ? condition.id : null, TAG);
    }

    public void onSetZenModeW(int mode) {
        if (D.BUG) {
            Log.d(TAG, "onSetZenModeW " + mode);
        }
        this.mNoMan.setZenMode(mode, null, TAG);
    }

    public void onDismissRequestedW(int reason) {
        this.mCallbacks.onDismissRequested(reason);
    }

    public void showDndTile(boolean visible) {
        if (D.BUG) {
            Log.d(TAG, "showDndTile");
        }
        DndTile.setVisible(this.mContext, visible);
    }

    private final class VC extends IVolumeController.Stub {
        private final String TAG;

        VC(VolumeDialogController this$0, VC vc) {
            this();
        }

        private VC() {
            this.TAG = VolumeDialogController.TAG + ".VC";
        }

        public void displaySafeVolumeWarning(int flags) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "displaySafeVolumeWarning " + Util.audioManagerFlagsToString(flags));
            }
            if (VolumeDialogController.this.mDestroyed) {
                return;
            }
            VolumeDialogController.this.mWorker.obtainMessage(14, flags, 0).sendToTarget();
        }

        public void volumeChanged(int streamType, int flags) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "volumeChanged " + AudioSystem.streamToString(streamType) + " " + Util.audioManagerFlagsToString(flags));
            }
            if (VolumeDialogController.this.mDestroyed) {
                return;
            }
            VolumeDialogController.this.mWorker.obtainMessage(1, streamType, flags).sendToTarget();
        }

        public void masterMuteChanged(int flags) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "masterMuteChanged");
            }
        }

        public void setLayoutDirection(int layoutDirection) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "setLayoutDirection");
            }
            if (VolumeDialogController.this.mDestroyed) {
                return;
            }
            VolumeDialogController.this.mWorker.obtainMessage(8, layoutDirection, 0).sendToTarget();
        }

        public void dismiss() throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "dismiss requested");
            }
            if (VolumeDialogController.this.mDestroyed) {
                return;
            }
            VolumeDialogController.this.mWorker.obtainMessage(2, 2, 0).sendToTarget();
            VolumeDialogController.this.mWorker.sendEmptyMessage(2);
        }
    }

    private final class W extends Handler {
        W(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    VolumeDialogController.this.onVolumeChangedW(msg.arg1, msg.arg2);
                    break;
                case 2:
                    VolumeDialogController.this.onDismissRequestedW(msg.arg1);
                    break;
                case 3:
                    VolumeDialogController.this.onGetStateW();
                    break;
                case 4:
                    VolumeDialogController.this.onSetRingerModeW(msg.arg1, msg.arg2 != 0);
                    break;
                case 5:
                    VolumeDialogController.this.onSetZenModeW(msg.arg1);
                    break;
                case 6:
                    VolumeDialogController.this.onSetExitConditionW((Condition) msg.obj);
                    break;
                case 7:
                    VolumeDialogController.this.onSetStreamMuteW(msg.arg1, msg.arg2 != 0);
                    break;
                case 8:
                    VolumeDialogController.this.mCallbacks.onLayoutDirectionChanged(msg.arg1);
                    break;
                case 9:
                    VolumeDialogController.this.mCallbacks.onConfigurationChanged();
                    break;
                case 10:
                    VolumeDialogController.this.onSetStreamVolumeW(msg.arg1, msg.arg2);
                    break;
                case 11:
                    VolumeDialogController.this.onSetActiveStreamW(msg.arg1);
                    break;
                case 12:
                    VolumeDialogController.this.onNotifyVisibleW(msg.arg1 != 0);
                    break;
                case 13:
                    VolumeDialogController.this.onUserActivityW();
                    break;
                case 14:
                    VolumeDialogController.this.onShowSafetyWarningW(msg.arg1);
                    break;
            }
        }
    }

    private final class C implements Callbacks {
        private final HashMap<Callbacks, Handler> mCallbackMap;

        C(VolumeDialogController this$0, C c) {
            this();
        }

        private C() {
            this.mCallbackMap = new HashMap<>();
        }

        public void add(Callbacks callback, Handler handler) {
            if (callback == null || handler == null) {
                throw new IllegalArgumentException();
            }
            this.mCallbackMap.put(callback, handler);
        }

        @Override
        public void onShowRequested(final int reason) {
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onShowRequested(reason);
                    }
                });
            }
        }

        @Override
        public void onDismissRequested(final int reason) {
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onDismissRequested(reason);
                    }
                });
            }
        }

        @Override
        public void onStateChanged(State state) {
            long time = System.currentTimeMillis();
            final State copy = state.copy();
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onStateChanged(copy);
                    }
                });
            }
            Events.writeState(time, copy);
        }

        @Override
        public void onLayoutDirectionChanged(final int layoutDirection) {
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onLayoutDirectionChanged(layoutDirection);
                    }
                });
            }
        }

        @Override
        public void onConfigurationChanged() {
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onConfigurationChanged();
                    }
                });
            }
        }

        @Override
        public void onShowVibrateHint() {
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onShowVibrateHint();
                    }
                });
            }
        }

        @Override
        public void onShowSilentHint() {
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onShowSilentHint();
                    }
                });
            }
        }

        @Override
        public void onScreenOff() {
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onScreenOff();
                    }
                });
            }
        }

        @Override
        public void onShowSafetyWarning(final int flags) {
            for (final Map.Entry<Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    @Override
                    public void run() {
                        ((Callbacks) entry.getKey()).onShowSafetyWarning(flags);
                    }
                });
            }
        }
    }

    private final class SettingObserver extends ContentObserver {
        private final Uri SERVICE_URI;
        private final Uri ZEN_MODE_CONFIG_URI;
        private final Uri ZEN_MODE_URI;

        public SettingObserver(Handler handler) {
            super(handler);
            this.SERVICE_URI = Settings.Secure.getUriFor("volume_controller_service_component");
            this.ZEN_MODE_URI = Settings.Global.getUriFor("zen_mode");
            this.ZEN_MODE_CONFIG_URI = Settings.Global.getUriFor("zen_mode_config_etag");
        }

        public void init() {
            VolumeDialogController.this.mContext.getContentResolver().registerContentObserver(this.SERVICE_URI, false, this);
            VolumeDialogController.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE_URI, false, this);
            VolumeDialogController.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE_CONFIG_URI, false, this);
            onChange(true, this.SERVICE_URI);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            boolean enabled;
            boolean changed = false;
            if (this.SERVICE_URI.equals(uri)) {
                String setting = Settings.Secure.getString(VolumeDialogController.this.mContext.getContentResolver(), "volume_controller_service_component");
                if (setting == null || VolumeDialogController.this.mComponent == null) {
                    enabled = false;
                } else {
                    enabled = VolumeDialogController.this.mComponent.equals(ComponentName.unflattenFromString(setting));
                }
                if (enabled == VolumeDialogController.this.mEnabled) {
                    return;
                }
                if (enabled) {
                    VolumeDialogController.this.register();
                }
                VolumeDialogController.this.mEnabled = enabled;
            }
            if (this.ZEN_MODE_URI.equals(uri)) {
                changed = VolumeDialogController.this.updateZenModeW();
            }
            if (!changed) {
                return;
            }
            VolumeDialogController.this.mCallbacks.onStateChanged(VolumeDialogController.this.mState);
        }
    }

    private final class Receiver extends BroadcastReceiver {
        Receiver(VolumeDialogController this$0, Receiver receiver) {
            this();
        }

        private Receiver() {
        }

        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.media.VOLUME_CHANGED_ACTION");
            filter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
            filter.addAction("android.media.RINGER_MODE_CHANGED");
            filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
            filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
            filter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            VolumeDialogController.this.mContext.registerReceiver(this, filter, null, VolumeDialogController.this.mWorker);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean changed = false;
            if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                int stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                int level = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                int oldLevel = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1);
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive VOLUME_CHANGED_ACTION stream=" + stream + " level=" + level + " oldLevel=" + oldLevel);
                }
                changed = VolumeDialogController.this.updateStreamLevelW(stream, level);
            } else if (action.equals("android.media.STREAM_DEVICES_CHANGED_ACTION")) {
                int stream2 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                int devices = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_DEVICES", -1);
                int oldDevices = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES", -1);
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive STREAM_DEVICES_CHANGED_ACTION stream=" + stream2 + " devices=" + devices + " oldDevices=" + oldDevices);
                }
                changed = VolumeDialogController.this.checkRoutedToBluetoothW(stream2) | VolumeDialogController.this.onVolumeChangedW(stream2, 0);
            } else if (action.equals("android.media.RINGER_MODE_CHANGED")) {
                int rm = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive RINGER_MODE_CHANGED_ACTION rm=" + Util.ringerModeToString(rm));
                }
                changed = VolumeDialogController.this.updateRingerModeExternalW(rm);
            } else if (action.equals("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")) {
                int rm2 = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive INTERNAL_RINGER_MODE_CHANGED_ACTION rm=" + Util.ringerModeToString(rm2));
                }
                changed = VolumeDialogController.this.updateRingerModeInternalW(rm2);
            } else if (action.equals("android.media.STREAM_MUTE_CHANGED_ACTION")) {
                int stream3 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                boolean muted = intent.getBooleanExtra("android.media.EXTRA_STREAM_VOLUME_MUTED", false);
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive STREAM_MUTE_CHANGED_ACTION stream=" + stream3 + " muted=" + muted);
                }
                changed = VolumeDialogController.this.updateStreamMuteW(stream3, muted);
            } else if (action.equals("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED")) {
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive ACTION_EFFECTS_SUPPRESSOR_CHANGED");
                }
                changed = VolumeDialogController.this.updateEffectsSuppressorW(VolumeDialogController.this.mNoMan.getEffectsSuppressor());
            } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive ACTION_CONFIGURATION_CHANGED");
                }
                VolumeDialogController.this.mCallbacks.onConfigurationChanged();
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive ACTION_SCREEN_OFF");
                }
                VolumeDialogController.this.mCallbacks.onScreenOff();
            } else if (action.equals("android.intent.action.CLOSE_SYSTEM_DIALOGS")) {
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onReceive ACTION_CLOSE_SYSTEM_DIALOGS");
                }
                VolumeDialogController.this.dismiss();
            }
            if (!changed) {
                return;
            }
            VolumeDialogController.this.mCallbacks.onStateChanged(VolumeDialogController.this.mState);
        }
    }

    private final class MediaSessionsCallbacks implements MediaSessions.Callbacks {
        private int mNextStream;
        private final HashMap<MediaSession.Token, Integer> mRemoteStreams;

        MediaSessionsCallbacks(VolumeDialogController this$0, MediaSessionsCallbacks mediaSessionsCallbacks) {
            this();
        }

        private MediaSessionsCallbacks() {
            this.mRemoteStreams = new HashMap<>();
            this.mNextStream = 100;
        }

        @Override
        public void onRemoteUpdate(MediaSession.Token token, String name, MediaController.PlaybackInfo pi) {
            if (!this.mRemoteStreams.containsKey(token)) {
                this.mRemoteStreams.put(token, Integer.valueOf(this.mNextStream));
                if (D.BUG) {
                    Log.d(VolumeDialogController.TAG, "onRemoteUpdate: " + name + " is stream " + this.mNextStream);
                }
                this.mNextStream++;
            }
            int stream = this.mRemoteStreams.get(token).intValue();
            boolean changed = VolumeDialogController.this.mState.states.indexOfKey(stream) < 0;
            StreamState ss = VolumeDialogController.this.streamStateW(stream);
            ss.dynamic = true;
            ss.levelMin = 0;
            ss.levelMax = pi.getMaxVolume();
            if (ss.level != pi.getCurrentVolume()) {
                ss.level = pi.getCurrentVolume();
                changed = true;
            }
            if (!Objects.equals(ss.name, name)) {
                ss.name = name;
                changed = true;
            }
            if (!changed) {
                return;
            }
            if (D.BUG) {
                Log.d(VolumeDialogController.TAG, "onRemoteUpdate: " + name + ": " + ss.level + " of " + ss.levelMax);
            }
            VolumeDialogController.this.mCallbacks.onStateChanged(VolumeDialogController.this.mState);
        }

        @Override
        public void onRemoteVolumeChanged(MediaSession.Token token, int flags) {
            if (this.mRemoteStreams.get(token) == null) {
                return;
            }
            int stream = this.mRemoteStreams.get(token).intValue();
            boolean showUI = (flags & 1) != 0;
            boolean changed = VolumeDialogController.this.updateActiveStreamW(stream);
            if (showUI) {
                changed |= VolumeDialogController.this.checkRoutedToBluetoothW(3);
            }
            if (changed) {
                VolumeDialogController.this.mCallbacks.onStateChanged(VolumeDialogController.this.mState);
            }
            if (!showUI) {
                return;
            }
            VolumeDialogController.this.mCallbacks.onShowRequested(2);
        }

        @Override
        public void onRemoteRemoved(MediaSession.Token token) {
            int stream = this.mRemoteStreams.get(token).intValue();
            VolumeDialogController.this.mState.states.remove(stream);
            if (VolumeDialogController.this.mState.activeStream == stream) {
                VolumeDialogController.this.updateActiveStreamW(-1);
            }
            VolumeDialogController.this.mCallbacks.onStateChanged(VolumeDialogController.this.mState);
        }

        public void setStreamVolume(int stream, int level) {
            MediaSession.Token t = findToken(stream);
            if (t == null) {
                Log.w(VolumeDialogController.TAG, "setStreamVolume: No token found for stream: " + stream);
            } else {
                VolumeDialogController.this.mMediaSessions.setVolume(t, level);
            }
        }

        private MediaSession.Token findToken(int stream) {
            for (Map.Entry<MediaSession.Token, Integer> entry : this.mRemoteStreams.entrySet()) {
                if (entry.getValue().equals(Integer.valueOf(stream))) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }

    public static final class StreamState {
        public boolean dynamic;
        public int level;
        public int levelMax;
        public int levelMin;
        public boolean muteSupported;
        public boolean muted;
        public String name;
        public boolean routedToBluetooth;

        public StreamState copy() {
            StreamState rt = new StreamState();
            rt.dynamic = this.dynamic;
            rt.level = this.level;
            rt.levelMin = this.levelMin;
            rt.levelMax = this.levelMax;
            rt.muted = this.muted;
            rt.muteSupported = this.muteSupported;
            rt.name = this.name;
            rt.routedToBluetooth = this.routedToBluetooth;
            return rt;
        }
    }

    public static final class State {
        public static int NO_ACTIVE_STREAM = -1;
        public ComponentName effectsSuppressor;
        public String effectsSuppressorName;
        public int ringerModeExternal;
        public int ringerModeInternal;
        public int zenMode;
        public final SparseArray<StreamState> states = new SparseArray<>();
        public int activeStream = NO_ACTIVE_STREAM;

        public State copy() {
            State rt = new State();
            for (int i = 0; i < this.states.size(); i++) {
                rt.states.put(this.states.keyAt(i), this.states.valueAt(i).copy());
            }
            rt.ringerModeExternal = this.ringerModeExternal;
            rt.ringerModeInternal = this.ringerModeInternal;
            rt.zenMode = this.zenMode;
            if (this.effectsSuppressor != null) {
                rt.effectsSuppressor = this.effectsSuppressor.clone();
            }
            rt.effectsSuppressorName = this.effectsSuppressorName;
            rt.activeStream = this.activeStream;
            return rt;
        }

        public String toString() {
            return toString(0);
        }

        public String toString(int indent) {
            StringBuilder sb = new StringBuilder("{");
            if (indent > 0) {
                sep(sb, indent);
            }
            for (int i = 0; i < this.states.size(); i++) {
                if (i > 0) {
                    sep(sb, indent);
                }
                int stream = this.states.keyAt(i);
                StreamState ss = this.states.valueAt(i);
                sb.append(AudioSystem.streamToString(stream)).append(":").append(ss.level).append('[').append(ss.levelMin).append("..").append(ss.levelMax).append(']');
                if (ss.muted) {
                    sb.append(" [MUTED]");
                }
            }
            sep(sb, indent);
            sb.append("ringerModeExternal:").append(this.ringerModeExternal);
            sep(sb, indent);
            sb.append("ringerModeInternal:").append(this.ringerModeInternal);
            sep(sb, indent);
            sb.append("zenMode:").append(this.zenMode);
            sep(sb, indent);
            sb.append("effectsSuppressor:").append(this.effectsSuppressor);
            sep(sb, indent);
            sb.append("effectsSuppressorName:").append(this.effectsSuppressorName);
            sep(sb, indent);
            sb.append("activeStream:").append(this.activeStream);
            if (indent > 0) {
                sep(sb, indent);
            }
            return sb.append('}').toString();
        }

        private static void sep(StringBuilder sb, int indent) {
            if (indent > 0) {
                sb.append('\n');
                for (int i = 0; i < indent; i++) {
                    sb.append(' ');
                }
                return;
            }
            sb.append(',');
        }
    }
}
