package com.android.server.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioSystem;
import android.media.IPlaybackConfigDispatcher;
import android.media.PlayerBase;
import android.media.VolumeShaper;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.server.audio.AudioEventLogger;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class PlaybackActivityMonitor implements AudioPlaybackConfiguration.PlayerDeathMonitor, PlayerFocusEnforcer {
    private static final boolean DEBUG;
    private static final VolumeShaper.Configuration DUCK_ID;
    private static final VolumeShaper.Configuration DUCK_VSHAPE;
    private static final int FLAGS_FOR_SILENCE_OVERRIDE = 192;
    private static final VolumeShaper.Operation PLAY_CREATE_IF_NEEDED;
    private static final VolumeShaper.Operation PLAY_SKIP_RAMP;
    public static final String TAG = "AudioService.PlaybackActivityMonitor";
    private static final int[] UNDUCKABLE_PLAYER_TYPES;
    private static final int VOLUME_SHAPER_SYSTEM_DUCK_ID = 1;
    private static final AudioEventLogger sEventLogger;
    private final Context mContext;
    private final int mMaxAlarmVolume;
    private final ArrayList<PlayMonitorClient> mClients = new ArrayList<>();
    private boolean mHasPublicClients = false;
    private final Object mPlayerLock = new Object();
    private final HashMap<Integer, AudioPlaybackConfiguration> mPlayers = new HashMap<>();
    private int mSavedAlarmVolume = -1;
    private int mPrivilegedAlarmActiveCount = 0;
    private final ArrayList<Integer> mBannedUids = new ArrayList<>();
    private final ArrayList<Integer> mMutedPlayers = new ArrayList<>();
    private final DuckingManager mDuckingManager = new DuckingManager();

    static {
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
        DUCK_VSHAPE = new VolumeShaper.Configuration.Builder().setId(1).setCurve(new float[]{0.0f, 1.0f}, new float[]{1.0f, 0.2f}).setOptionFlags(2).setDuration(MediaFocusControl.getFocusRampTimeMs(3, new AudioAttributes.Builder().setUsage(5).build())).build();
        DUCK_ID = new VolumeShaper.Configuration(1);
        PLAY_CREATE_IF_NEEDED = new VolumeShaper.Operation.Builder(VolumeShaper.Operation.PLAY).createIfNeeded().build();
        UNDUCKABLE_PLAYER_TYPES = new int[]{13, 3};
        PLAY_SKIP_RAMP = new VolumeShaper.Operation.Builder(PLAY_CREATE_IF_NEEDED).setXOffset(1.0f).build();
        sEventLogger = new AudioEventLogger(100, "playback activity as reported through PlayerBase");
    }

    PlaybackActivityMonitor(Context context, int i) {
        this.mContext = context;
        this.mMaxAlarmVolume = i;
        PlayMonitorClient.sListenerDeathMonitor = this;
        AudioPlaybackConfiguration.sPlayerDeathMonitor = this;
    }

    public void disableAudioForUid(boolean z, int i) {
        synchronized (this.mPlayerLock) {
            int iIndexOf = this.mBannedUids.indexOf(new Integer(i));
            if (iIndexOf >= 0) {
                if (!z) {
                    if (DEBUG) {
                        sEventLogger.log(new AudioEventLogger.StringEvent("unbanning uid:" + i));
                    }
                    this.mBannedUids.remove(iIndexOf);
                }
            } else if (z) {
                Iterator<AudioPlaybackConfiguration> it = this.mPlayers.values().iterator();
                while (it.hasNext()) {
                    checkBanPlayer(it.next(), i);
                }
                if (DEBUG) {
                    sEventLogger.log(new AudioEventLogger.StringEvent("banning uid:" + i));
                }
                this.mBannedUids.add(new Integer(i));
            }
        }
    }

    private boolean checkBanPlayer(AudioPlaybackConfiguration audioPlaybackConfiguration, int i) {
        boolean z = audioPlaybackConfiguration.getClientUid() == i;
        if (z) {
            int playerInterfaceId = audioPlaybackConfiguration.getPlayerInterfaceId();
            try {
                Log.v(TAG, "banning player " + playerInterfaceId + " uid:" + i);
                audioPlaybackConfiguration.getPlayerProxy().pause();
            } catch (Exception e) {
                Log.e(TAG, "error banning player " + playerInterfaceId + " uid:" + i, e);
            }
        }
        return z;
    }

    public int trackPlayer(PlayerBase.PlayerIdCard playerIdCard) {
        int iNewAudioPlayerId = AudioSystem.newAudioPlayerId();
        if (DEBUG) {
            Log.v(TAG, "trackPlayer() new piid=" + iNewAudioPlayerId);
        }
        AudioPlaybackConfiguration audioPlaybackConfiguration = new AudioPlaybackConfiguration(playerIdCard, iNewAudioPlayerId, Binder.getCallingUid(), Binder.getCallingPid());
        audioPlaybackConfiguration.init();
        sEventLogger.log(new NewPlayerEvent(audioPlaybackConfiguration));
        synchronized (this.mPlayerLock) {
            this.mPlayers.put(Integer.valueOf(iNewAudioPlayerId), audioPlaybackConfiguration);
        }
        return iNewAudioPlayerId;
    }

    public void playerAttributes(int i, AudioAttributes audioAttributes, int i2) {
        boolean zHandleAudioAttributesEvent;
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration audioPlaybackConfiguration = this.mPlayers.get(new Integer(i));
            if (checkConfigurationCaller(i, audioPlaybackConfiguration, i2)) {
                sEventLogger.log(new AudioAttrEvent(i, audioAttributes));
                zHandleAudioAttributesEvent = audioPlaybackConfiguration.handleAudioAttributesEvent(audioAttributes);
            } else {
                Log.e(TAG, "Error updating audio attributes");
                zHandleAudioAttributesEvent = false;
            }
        }
        if (zHandleAudioAttributesEvent) {
            dispatchPlaybackChange(false);
        }
    }

    private void checkVolumeForPrivilegedAlarm(AudioPlaybackConfiguration audioPlaybackConfiguration, int i) {
        if ((i == 2 || audioPlaybackConfiguration.getPlayerState() == 2) && (audioPlaybackConfiguration.getAudioAttributes().getAllFlags() & FLAGS_FOR_SILENCE_OVERRIDE) == FLAGS_FOR_SILENCE_OVERRIDE && audioPlaybackConfiguration.getAudioAttributes().getUsage() == 4 && this.mContext.checkPermission("android.permission.MODIFY_PHONE_STATE", audioPlaybackConfiguration.getClientPid(), audioPlaybackConfiguration.getClientUid()) == 0) {
            if (i == 2 && audioPlaybackConfiguration.getPlayerState() != 2) {
                int i2 = this.mPrivilegedAlarmActiveCount;
                this.mPrivilegedAlarmActiveCount = i2 + 1;
                if (i2 == 0) {
                    this.mSavedAlarmVolume = AudioSystem.getStreamVolumeIndex(4, 2);
                    AudioSystem.setStreamVolumeIndex(4, this.mMaxAlarmVolume, 2);
                    return;
                }
                return;
            }
            if (i != 2 && audioPlaybackConfiguration.getPlayerState() == 2) {
                int i3 = this.mPrivilegedAlarmActiveCount - 1;
                this.mPrivilegedAlarmActiveCount = i3;
                if (i3 == 0 && AudioSystem.getStreamVolumeIndex(4, 2) == this.mMaxAlarmVolume) {
                    AudioSystem.setStreamVolumeIndex(4, this.mSavedAlarmVolume, 2);
                }
            }
        }
    }

    public void playerEvent(int i, int i2, int i3) {
        boolean zHandleStateEvent;
        if (DEBUG) {
            Log.v(TAG, String.format("playerEvent(piid=%d, event=%d)", Integer.valueOf(i), Integer.valueOf(i2)));
        }
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration audioPlaybackConfiguration = this.mPlayers.get(new Integer(i));
            if (audioPlaybackConfiguration == null) {
                return;
            }
            sEventLogger.log(new PlayerEvent(i, i2));
            if (i2 == 2) {
                Iterator<Integer> it = this.mBannedUids.iterator();
                while (it.hasNext()) {
                    if (checkBanPlayer(audioPlaybackConfiguration, it.next().intValue())) {
                        sEventLogger.log(new AudioEventLogger.StringEvent("not starting piid:" + i + " ,is banned"));
                        return;
                    }
                }
            }
            if (audioPlaybackConfiguration.getPlayerType() == 3) {
                return;
            }
            if (checkConfigurationCaller(i, audioPlaybackConfiguration, i3)) {
                checkVolumeForPrivilegedAlarm(audioPlaybackConfiguration, i2);
                zHandleStateEvent = audioPlaybackConfiguration.handleStateEvent(i2);
            } else {
                Log.e(TAG, "Error handling event " + i2);
                zHandleStateEvent = false;
            }
            if (zHandleStateEvent && i2 == 2) {
                this.mDuckingManager.checkDuck(audioPlaybackConfiguration);
            }
            if (zHandleStateEvent) {
                dispatchPlaybackChange(i2 == 0);
            }
        }
    }

    public void playerHasOpPlayAudio(int i, boolean z, int i2) {
        sEventLogger.log(new PlayerOpPlayAudioEvent(i, z, i2));
    }

    public void releasePlayer(int i, int i2) {
        boolean zHandleStateEvent;
        if (DEBUG) {
            Log.v(TAG, "releasePlayer() for piid=" + i);
        }
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration audioPlaybackConfiguration = this.mPlayers.get(new Integer(i));
            zHandleStateEvent = false;
            if (checkConfigurationCaller(i, audioPlaybackConfiguration, i2)) {
                sEventLogger.log(new AudioEventLogger.StringEvent("releasing player piid:" + i));
                this.mPlayers.remove(new Integer(i));
                this.mDuckingManager.removeReleased(audioPlaybackConfiguration);
                checkVolumeForPrivilegedAlarm(audioPlaybackConfiguration, 0);
                zHandleStateEvent = audioPlaybackConfiguration.handleStateEvent(0);
            }
        }
        if (zHandleStateEvent) {
            dispatchPlaybackChange(true);
        }
    }

    public void playerDeath(int i) {
        releasePlayer(i, 0);
    }

    protected void dump(PrintWriter printWriter) {
        printWriter.println("\nPlaybackActivityMonitor dump time: " + DateFormat.getTimeInstance().format(new Date()));
        synchronized (this.mPlayerLock) {
            printWriter.println("\n  playback listeners:");
            synchronized (this.mClients) {
                for (PlayMonitorClient playMonitorClient : this.mClients) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" ");
                    sb.append(playMonitorClient.mIsPrivileged ? "(S)" : "(P)");
                    sb.append(playMonitorClient.toString());
                    printWriter.print(sb.toString());
                }
            }
            printWriter.println("\n");
            printWriter.println("\n  players:");
            ArrayList arrayList = new ArrayList(this.mPlayers.keySet());
            Collections.sort(arrayList);
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                AudioPlaybackConfiguration audioPlaybackConfiguration = this.mPlayers.get((Integer) it.next());
                if (audioPlaybackConfiguration != null) {
                    audioPlaybackConfiguration.dump(printWriter);
                }
            }
            printWriter.println("\n  ducked players piids:");
            this.mDuckingManager.dump(printWriter);
            printWriter.print("\n  muted player piids:");
            Iterator<Integer> it2 = this.mMutedPlayers.iterator();
            while (it2.hasNext()) {
                printWriter.print(" " + it2.next().intValue());
            }
            printWriter.println();
            printWriter.print("\n  banned uids:");
            Iterator<Integer> it3 = this.mBannedUids.iterator();
            while (it3.hasNext()) {
                printWriter.print(" " + it3.next().intValue());
            }
            printWriter.println("\n");
            sEventLogger.dump(printWriter);
        }
    }

    private static boolean checkConfigurationCaller(int i, AudioPlaybackConfiguration audioPlaybackConfiguration, int i2) {
        if (audioPlaybackConfiguration == null) {
            return false;
        }
        if (i2 != 0 && audioPlaybackConfiguration.getClientUid() != i2) {
            Log.e(TAG, "Forbidden operation from uid " + i2 + " for player " + i);
            return false;
        }
        return true;
    }

    private void dispatchPlaybackChange(boolean z) {
        synchronized (this.mClients) {
            if (this.mClients.isEmpty()) {
                return;
            }
            if (DEBUG) {
                Log.v(TAG, "dispatchPlaybackChange to " + this.mClients.size() + " clients");
            }
            synchronized (this.mPlayerLock) {
                if (this.mPlayers.isEmpty()) {
                    return;
                }
                ArrayList arrayList = new ArrayList(this.mPlayers.values());
                synchronized (this.mClients) {
                    if (this.mClients.isEmpty()) {
                        return;
                    }
                    ArrayList<AudioPlaybackConfiguration> arrayListAnonymizeForPublicConsumption = this.mHasPublicClients ? anonymizeForPublicConsumption(arrayList) : null;
                    for (PlayMonitorClient playMonitorClient : this.mClients) {
                        try {
                            if (playMonitorClient.mErrorCount < 5) {
                                if (playMonitorClient.mIsPrivileged) {
                                    playMonitorClient.mDispatcherCb.dispatchPlaybackConfigChange(arrayList, z);
                                } else {
                                    playMonitorClient.mDispatcherCb.dispatchPlaybackConfigChange(arrayListAnonymizeForPublicConsumption, false);
                                }
                            }
                        } catch (RemoteException e) {
                            playMonitorClient.mErrorCount++;
                            Log.e(TAG, "Error (" + playMonitorClient.mErrorCount + ") trying to dispatch playback config change to " + playMonitorClient, e);
                        }
                    }
                }
            }
        }
    }

    private ArrayList<AudioPlaybackConfiguration> anonymizeForPublicConsumption(List<AudioPlaybackConfiguration> list) {
        ArrayList<AudioPlaybackConfiguration> arrayList = new ArrayList<>();
        for (AudioPlaybackConfiguration audioPlaybackConfiguration : list) {
            if (audioPlaybackConfiguration.isActive()) {
                arrayList.add(AudioPlaybackConfiguration.anonymizedCopy(audioPlaybackConfiguration));
            }
        }
        return arrayList;
    }

    @Override
    public boolean duckPlayers(FocusRequester focusRequester, FocusRequester focusRequester2, boolean z) {
        if (DEBUG) {
            Log.v(TAG, String.format("duckPlayers: uids winner=%d loser=%d", Integer.valueOf(focusRequester.getClientUid()), Integer.valueOf(focusRequester2.getClientUid())));
        }
        synchronized (this.mPlayerLock) {
            if (this.mPlayers.isEmpty()) {
                return true;
            }
            ArrayList<AudioPlaybackConfiguration> arrayList = new ArrayList<>();
            for (AudioPlaybackConfiguration audioPlaybackConfiguration : this.mPlayers.values()) {
                if (!focusRequester.hasSameUid(audioPlaybackConfiguration.getClientUid()) && focusRequester2.hasSameUid(audioPlaybackConfiguration.getClientUid()) && audioPlaybackConfiguration.getPlayerState() == 2) {
                    if (!z && audioPlaybackConfiguration.getAudioAttributes().getContentType() == 1) {
                        Log.v(TAG, "not ducking player " + audioPlaybackConfiguration.getPlayerInterfaceId() + " uid:" + audioPlaybackConfiguration.getClientUid() + " pid:" + audioPlaybackConfiguration.getClientPid() + " - SPEECH");
                        return false;
                    }
                    if (ArrayUtils.contains(UNDUCKABLE_PLAYER_TYPES, audioPlaybackConfiguration.getPlayerType())) {
                        Log.v(TAG, "not ducking player " + audioPlaybackConfiguration.getPlayerInterfaceId() + " uid:" + audioPlaybackConfiguration.getClientUid() + " pid:" + audioPlaybackConfiguration.getClientPid() + " due to type:" + AudioPlaybackConfiguration.toLogFriendlyPlayerType(audioPlaybackConfiguration.getPlayerType()));
                        return false;
                    }
                    arrayList.add(audioPlaybackConfiguration);
                }
            }
            this.mDuckingManager.duckUid(focusRequester2.getClientUid(), arrayList);
            return true;
        }
    }

    @Override
    public void unduckPlayers(FocusRequester focusRequester) {
        if (DEBUG) {
            Log.v(TAG, "unduckPlayers: uids winner=" + focusRequester.getClientUid());
        }
        synchronized (this.mPlayerLock) {
            this.mDuckingManager.unduckUid(focusRequester.getClientUid(), this.mPlayers);
        }
    }

    @Override
    public void mutePlayersForCall(int[] iArr) {
        boolean z;
        if (DEBUG) {
            String str = new String("mutePlayersForCall: usages=");
            for (int i : iArr) {
                str = str + " " + i;
            }
            Log.v(TAG, str);
        }
        synchronized (this.mPlayerLock) {
            for (Integer num : this.mPlayers.keySet()) {
                AudioPlaybackConfiguration audioPlaybackConfiguration = this.mPlayers.get(num);
                if (audioPlaybackConfiguration != null) {
                    int usage = audioPlaybackConfiguration.getAudioAttributes().getUsage();
                    int length = iArr.length;
                    int i2 = 0;
                    while (true) {
                        if (i2 < length) {
                            if (usage != iArr[i2]) {
                                i2++;
                            } else {
                                z = true;
                                break;
                            }
                        } else {
                            z = false;
                            break;
                        }
                    }
                    if (z) {
                        try {
                            sEventLogger.log(new AudioEventLogger.StringEvent("call: muting piid:" + num + " uid:" + audioPlaybackConfiguration.getClientUid()).printLog(TAG));
                            audioPlaybackConfiguration.getPlayerProxy().setVolume(0.0f);
                            this.mMutedPlayers.add(new Integer(num.intValue()));
                        } catch (Exception e) {
                            Log.e(TAG, "call: error muting player " + num, e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void unmutePlayersForCall() {
        if (DEBUG) {
            Log.v(TAG, "unmutePlayersForCall()");
        }
        synchronized (this.mPlayerLock) {
            if (this.mMutedPlayers.isEmpty()) {
                return;
            }
            Iterator<Integer> it = this.mMutedPlayers.iterator();
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                AudioPlaybackConfiguration audioPlaybackConfiguration = this.mPlayers.get(Integer.valueOf(iIntValue));
                if (audioPlaybackConfiguration != null) {
                    try {
                        sEventLogger.log(new AudioEventLogger.StringEvent("call: unmuting piid:" + iIntValue).printLog(TAG));
                        audioPlaybackConfiguration.getPlayerProxy().setVolume(1.0f);
                    } catch (Exception e) {
                        Log.e(TAG, "call: error unmuting player " + iIntValue + " uid:" + audioPlaybackConfiguration.getClientUid(), e);
                    }
                }
            }
            this.mMutedPlayers.clear();
        }
    }

    void registerPlaybackCallback(IPlaybackConfigDispatcher iPlaybackConfigDispatcher, boolean z) {
        if (iPlaybackConfigDispatcher == null) {
            return;
        }
        synchronized (this.mClients) {
            PlayMonitorClient playMonitorClient = new PlayMonitorClient(iPlaybackConfigDispatcher, z);
            if (playMonitorClient.init()) {
                if (!z) {
                    this.mHasPublicClients = true;
                }
                this.mClients.add(playMonitorClient);
            }
        }
    }

    void unregisterPlaybackCallback(IPlaybackConfigDispatcher iPlaybackConfigDispatcher) {
        if (iPlaybackConfigDispatcher == null) {
            return;
        }
        synchronized (this.mClients) {
            Iterator<PlayMonitorClient> it = this.mClients.iterator();
            boolean z = false;
            while (it.hasNext()) {
                PlayMonitorClient next = it.next();
                if (iPlaybackConfigDispatcher.equals(next.mDispatcherCb)) {
                    next.release();
                    it.remove();
                } else if (!next.mIsPrivileged) {
                    z = true;
                }
            }
            this.mHasPublicClients = z;
        }
    }

    List<AudioPlaybackConfiguration> getActivePlaybackConfigurations(boolean z) {
        ArrayList<AudioPlaybackConfiguration> arrayListAnonymizeForPublicConsumption;
        synchronized (this.mPlayers) {
            try {
                if (z) {
                    return new ArrayList(this.mPlayers.values());
                }
                synchronized (this.mPlayerLock) {
                    arrayListAnonymizeForPublicConsumption = anonymizeForPublicConsumption(new ArrayList(this.mPlayers.values()));
                }
                return arrayListAnonymizeForPublicConsumption;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static final class PlayMonitorClient implements IBinder.DeathRecipient {
        static final int MAX_ERRORS = 5;
        static PlaybackActivityMonitor sListenerDeathMonitor;
        final IPlaybackConfigDispatcher mDispatcherCb;
        int mErrorCount = 0;
        final boolean mIsPrivileged;

        PlayMonitorClient(IPlaybackConfigDispatcher iPlaybackConfigDispatcher, boolean z) {
            this.mDispatcherCb = iPlaybackConfigDispatcher;
            this.mIsPrivileged = z;
        }

        @Override
        public void binderDied() {
            Log.w(PlaybackActivityMonitor.TAG, "client died");
            sListenerDeathMonitor.unregisterPlaybackCallback(this.mDispatcherCb);
        }

        boolean init() {
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(PlaybackActivityMonitor.TAG, "Could not link to client death", e);
                return false;
            }
        }

        void release() {
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    private static final class DuckingManager {
        private final HashMap<Integer, DuckedApp> mDuckers;

        private DuckingManager() {
            this.mDuckers = new HashMap<>();
        }

        synchronized void duckUid(int i, ArrayList<AudioPlaybackConfiguration> arrayList) {
            if (PlaybackActivityMonitor.DEBUG) {
                Log.v(PlaybackActivityMonitor.TAG, "DuckingManager: duckUid() uid:" + i);
            }
            if (!this.mDuckers.containsKey(Integer.valueOf(i))) {
                this.mDuckers.put(Integer.valueOf(i), new DuckedApp(i));
            }
            DuckedApp duckedApp = this.mDuckers.get(Integer.valueOf(i));
            Iterator<AudioPlaybackConfiguration> it = arrayList.iterator();
            while (it.hasNext()) {
                duckedApp.addDuck(it.next(), false);
            }
        }

        synchronized void unduckUid(int i, HashMap<Integer, AudioPlaybackConfiguration> map) {
            if (PlaybackActivityMonitor.DEBUG) {
                Log.v(PlaybackActivityMonitor.TAG, "DuckingManager: unduckUid() uid:" + i);
            }
            DuckedApp duckedAppRemove = this.mDuckers.remove(Integer.valueOf(i));
            if (duckedAppRemove == null) {
                return;
            }
            duckedAppRemove.removeUnduckAll(map);
        }

        synchronized void checkDuck(AudioPlaybackConfiguration audioPlaybackConfiguration) {
            if (PlaybackActivityMonitor.DEBUG) {
                Log.v(PlaybackActivityMonitor.TAG, "DuckingManager: checkDuck() player piid:" + audioPlaybackConfiguration.getPlayerInterfaceId() + " uid:" + audioPlaybackConfiguration.getClientUid());
            }
            DuckedApp duckedApp = this.mDuckers.get(Integer.valueOf(audioPlaybackConfiguration.getClientUid()));
            if (duckedApp == null) {
                return;
            }
            duckedApp.addDuck(audioPlaybackConfiguration, true);
        }

        synchronized void dump(PrintWriter printWriter) {
            Iterator<DuckedApp> it = this.mDuckers.values().iterator();
            while (it.hasNext()) {
                it.next().dump(printWriter);
            }
        }

        synchronized void removeReleased(AudioPlaybackConfiguration audioPlaybackConfiguration) {
            int clientUid = audioPlaybackConfiguration.getClientUid();
            if (PlaybackActivityMonitor.DEBUG) {
                Log.v(PlaybackActivityMonitor.TAG, "DuckingManager: removedReleased() player piid: " + audioPlaybackConfiguration.getPlayerInterfaceId() + " uid:" + clientUid);
            }
            DuckedApp duckedApp = this.mDuckers.get(Integer.valueOf(clientUid));
            if (duckedApp == null) {
                return;
            }
            duckedApp.removeReleased(audioPlaybackConfiguration);
        }

        private static final class DuckedApp {
            private final ArrayList<Integer> mDuckedPlayers = new ArrayList<>();
            private final int mUid;

            DuckedApp(int i) {
                this.mUid = i;
            }

            void dump(PrintWriter printWriter) {
                printWriter.print("\t uid:" + this.mUid + " piids:");
                Iterator<Integer> it = this.mDuckedPlayers.iterator();
                while (it.hasNext()) {
                    printWriter.print(" " + it.next().intValue());
                }
                printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }

            void addDuck(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
                int iIntValue = new Integer(audioPlaybackConfiguration.getPlayerInterfaceId()).intValue();
                if (this.mDuckedPlayers.contains(Integer.valueOf(iIntValue))) {
                    if (PlaybackActivityMonitor.DEBUG) {
                        Log.v(PlaybackActivityMonitor.TAG, "player piid:" + iIntValue + " already ducked");
                        return;
                    }
                    return;
                }
                try {
                    PlaybackActivityMonitor.sEventLogger.log(new DuckEvent(audioPlaybackConfiguration, z).printLog(PlaybackActivityMonitor.TAG));
                    audioPlaybackConfiguration.getPlayerProxy().applyVolumeShaper(PlaybackActivityMonitor.DUCK_VSHAPE, z ? PlaybackActivityMonitor.PLAY_SKIP_RAMP : PlaybackActivityMonitor.PLAY_CREATE_IF_NEEDED);
                    this.mDuckedPlayers.add(Integer.valueOf(iIntValue));
                } catch (Exception e) {
                    Log.e(PlaybackActivityMonitor.TAG, "Error ducking player piid:" + iIntValue + " uid:" + this.mUid, e);
                }
            }

            void removeUnduckAll(HashMap<Integer, AudioPlaybackConfiguration> map) {
                Iterator<Integer> it = this.mDuckedPlayers.iterator();
                while (it.hasNext()) {
                    int iIntValue = it.next().intValue();
                    AudioPlaybackConfiguration audioPlaybackConfiguration = map.get(Integer.valueOf(iIntValue));
                    if (audioPlaybackConfiguration != null) {
                        try {
                            PlaybackActivityMonitor.sEventLogger.log(new AudioEventLogger.StringEvent("unducking piid:" + iIntValue).printLog(PlaybackActivityMonitor.TAG));
                            audioPlaybackConfiguration.getPlayerProxy().applyVolumeShaper(PlaybackActivityMonitor.DUCK_ID, VolumeShaper.Operation.REVERSE);
                        } catch (Exception e) {
                            Log.e(PlaybackActivityMonitor.TAG, "Error unducking player piid:" + iIntValue + " uid:" + this.mUid, e);
                        }
                    } else if (PlaybackActivityMonitor.DEBUG) {
                        Log.v(PlaybackActivityMonitor.TAG, "Error unducking player piid:" + iIntValue + ", player not found for uid " + this.mUid);
                    }
                }
                this.mDuckedPlayers.clear();
            }

            void removeReleased(AudioPlaybackConfiguration audioPlaybackConfiguration) {
                this.mDuckedPlayers.remove(new Integer(audioPlaybackConfiguration.getPlayerInterfaceId()));
            }
        }
    }

    private static final class PlayerEvent extends AudioEventLogger.Event {
        final int mPlayerIId;
        final int mState;

        PlayerEvent(int i, int i2) {
            this.mPlayerIId = i;
            this.mState = i2;
        }

        @Override
        public String eventToString() {
            return "player piid:" + this.mPlayerIId + " state:" + AudioPlaybackConfiguration.toLogFriendlyPlayerState(this.mState);
        }
    }

    private static final class PlayerOpPlayAudioEvent extends AudioEventLogger.Event {
        final boolean mHasOp;
        final int mPlayerIId;
        final int mUid;

        PlayerOpPlayAudioEvent(int i, boolean z, int i2) {
            this.mPlayerIId = i;
            this.mHasOp = z;
            this.mUid = i2;
        }

        @Override
        public String eventToString() {
            return "player piid:" + this.mPlayerIId + " has OP_PLAY_AUDIO:" + this.mHasOp + " in uid:" + this.mUid;
        }
    }

    private static final class NewPlayerEvent extends AudioEventLogger.Event {
        private final int mClientPid;
        private final int mClientUid;
        private final AudioAttributes mPlayerAttr;
        private final int mPlayerIId;
        private final int mPlayerType;

        NewPlayerEvent(AudioPlaybackConfiguration audioPlaybackConfiguration) {
            this.mPlayerIId = audioPlaybackConfiguration.getPlayerInterfaceId();
            this.mPlayerType = audioPlaybackConfiguration.getPlayerType();
            this.mClientUid = audioPlaybackConfiguration.getClientUid();
            this.mClientPid = audioPlaybackConfiguration.getClientPid();
            this.mPlayerAttr = audioPlaybackConfiguration.getAudioAttributes();
        }

        @Override
        public String eventToString() {
            return new String("new player piid:" + this.mPlayerIId + " uid/pid:" + this.mClientUid + SliceClientPermissions.SliceAuthority.DELIMITER + this.mClientPid + " type:" + AudioPlaybackConfiguration.toLogFriendlyPlayerType(this.mPlayerType) + " attr:" + this.mPlayerAttr);
        }
    }

    private static final class DuckEvent extends AudioEventLogger.Event {
        private final int mClientPid;
        private final int mClientUid;
        private final int mPlayerIId;
        private final boolean mSkipRamp;

        DuckEvent(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
            this.mPlayerIId = audioPlaybackConfiguration.getPlayerInterfaceId();
            this.mSkipRamp = z;
            this.mClientUid = audioPlaybackConfiguration.getClientUid();
            this.mClientPid = audioPlaybackConfiguration.getClientPid();
        }

        @Override
        public String eventToString() {
            return "ducking player piid:" + this.mPlayerIId + " uid/pid:" + this.mClientUid + SliceClientPermissions.SliceAuthority.DELIMITER + this.mClientPid + " skip ramp:" + this.mSkipRamp;
        }
    }

    private static final class AudioAttrEvent extends AudioEventLogger.Event {
        private final AudioAttributes mPlayerAttr;
        private final int mPlayerIId;

        AudioAttrEvent(int i, AudioAttributes audioAttributes) {
            this.mPlayerIId = i;
            this.mPlayerAttr = audioAttributes;
        }

        @Override
        public String eventToString() {
            return new String("player piid:" + this.mPlayerIId + " new AudioAttributes:" + this.mPlayerAttr);
        }
    }
}
