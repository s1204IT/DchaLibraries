package android.media;

import android.annotation.SystemApi;
import android.media.AudioAttributes;
import android.media.IPlayer;
import android.media.PlayerBase;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public final class AudioPlaybackConfiguration implements Parcelable {
    private static final boolean DEBUG = false;
    public static final int PLAYER_PIID_INVALID = -1;
    public static final int PLAYER_PIID_UNASSIGNED = 0;

    @SystemApi
    public static final int PLAYER_STATE_IDLE = 1;

    @SystemApi
    public static final int PLAYER_STATE_PAUSED = 3;

    @SystemApi
    public static final int PLAYER_STATE_RELEASED = 0;

    @SystemApi
    public static final int PLAYER_STATE_STARTED = 2;

    @SystemApi
    public static final int PLAYER_STATE_STOPPED = 4;

    @SystemApi
    public static final int PLAYER_STATE_UNKNOWN = -1;
    public static final int PLAYER_TYPE_AAUDIO = 13;
    public static final int PLAYER_TYPE_EXTERNAL_PROXY = 15;
    public static final int PLAYER_TYPE_HW_SOURCE = 14;

    @SystemApi
    public static final int PLAYER_TYPE_JAM_AUDIOTRACK = 1;

    @SystemApi
    public static final int PLAYER_TYPE_JAM_MEDIAPLAYER = 2;

    @SystemApi
    public static final int PLAYER_TYPE_JAM_SOUNDPOOL = 3;

    @SystemApi
    public static final int PLAYER_TYPE_SLES_AUDIOPLAYER_BUFFERQUEUE = 11;

    @SystemApi
    public static final int PLAYER_TYPE_SLES_AUDIOPLAYER_URI_FD = 12;

    @SystemApi
    public static final int PLAYER_TYPE_UNKNOWN = -1;
    public static final int PLAYER_UPID_INVALID = -1;
    public static PlayerDeathMonitor sPlayerDeathMonitor;
    private int mClientPid;
    private int mClientUid;
    private IPlayerShell mIPlayerShell;
    private AudioAttributes mPlayerAttr;
    private final int mPlayerIId;
    private int mPlayerState;
    private int mPlayerType;
    private static final String TAG = new String("AudioPlaybackConfiguration");
    public static final Parcelable.Creator<AudioPlaybackConfiguration> CREATOR = new Parcelable.Creator<AudioPlaybackConfiguration>() {
        @Override
        public AudioPlaybackConfiguration createFromParcel(Parcel parcel) {
            return new AudioPlaybackConfiguration(parcel);
        }

        @Override
        public AudioPlaybackConfiguration[] newArray(int i) {
            return new AudioPlaybackConfiguration[i];
        }
    };

    public interface PlayerDeathMonitor {
        void playerDeath(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerState {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerType {
    }

    private AudioPlaybackConfiguration(int i) {
        this.mPlayerIId = i;
        this.mIPlayerShell = null;
    }

    public AudioPlaybackConfiguration(PlayerBase.PlayerIdCard playerIdCard, int i, int i2, int i3) {
        this.mPlayerIId = i;
        this.mPlayerType = playerIdCard.mPlayerType;
        this.mClientUid = i2;
        this.mClientPid = i3;
        this.mPlayerState = 1;
        this.mPlayerAttr = playerIdCard.mAttributes;
        if (sPlayerDeathMonitor != null && playerIdCard.mIPlayer != null) {
            this.mIPlayerShell = new IPlayerShell(this, playerIdCard.mIPlayer);
        } else {
            this.mIPlayerShell = null;
        }
    }

    public void init() {
        synchronized (this) {
            if (this.mIPlayerShell != null) {
                this.mIPlayerShell.monitorDeath();
            }
        }
    }

    public static AudioPlaybackConfiguration anonymizedCopy(AudioPlaybackConfiguration audioPlaybackConfiguration) {
        AudioPlaybackConfiguration audioPlaybackConfiguration2 = new AudioPlaybackConfiguration(audioPlaybackConfiguration.mPlayerIId);
        audioPlaybackConfiguration2.mPlayerState = audioPlaybackConfiguration.mPlayerState;
        audioPlaybackConfiguration2.mPlayerAttr = new AudioAttributes.Builder().setUsage(audioPlaybackConfiguration.mPlayerAttr.getUsage()).setContentType(audioPlaybackConfiguration.mPlayerAttr.getContentType()).setFlags(audioPlaybackConfiguration.mPlayerAttr.getFlags()).build();
        audioPlaybackConfiguration2.mPlayerType = -1;
        audioPlaybackConfiguration2.mClientUid = -1;
        audioPlaybackConfiguration2.mClientPid = -1;
        audioPlaybackConfiguration2.mIPlayerShell = null;
        return audioPlaybackConfiguration2;
    }

    public AudioAttributes getAudioAttributes() {
        return this.mPlayerAttr;
    }

    @SystemApi
    public int getClientUid() {
        return this.mClientUid;
    }

    @SystemApi
    public int getClientPid() {
        return this.mClientPid;
    }

    @SystemApi
    public int getPlayerType() {
        switch (this.mPlayerType) {
            case 13:
            case 14:
            case 15:
                return -1;
            default:
                return this.mPlayerType;
        }
    }

    @SystemApi
    public int getPlayerState() {
        return this.mPlayerState;
    }

    @SystemApi
    public int getPlayerInterfaceId() {
        return this.mPlayerIId;
    }

    @SystemApi
    public PlayerProxy getPlayerProxy() {
        IPlayerShell iPlayerShell;
        synchronized (this) {
            iPlayerShell = this.mIPlayerShell;
        }
        if (iPlayerShell == null) {
            return null;
        }
        return new PlayerProxy(this);
    }

    IPlayer getIPlayer() {
        IPlayerShell iPlayerShell;
        synchronized (this) {
            iPlayerShell = this.mIPlayerShell;
        }
        if (iPlayerShell == null) {
            return null;
        }
        return iPlayerShell.getIPlayer();
    }

    public boolean handleAudioAttributesEvent(AudioAttributes audioAttributes) {
        boolean z = !audioAttributes.equals(this.mPlayerAttr);
        this.mPlayerAttr = audioAttributes;
        return z;
    }

    public boolean handleStateEvent(int i) {
        boolean z;
        synchronized (this) {
            z = this.mPlayerState != i;
            this.mPlayerState = i;
            if (z && i == 0 && this.mIPlayerShell != null) {
                this.mIPlayerShell.release();
                this.mIPlayerShell = null;
            }
        }
        return z;
    }

    private void playerDied() {
        if (sPlayerDeathMonitor != null) {
            sPlayerDeathMonitor.playerDeath(this.mPlayerIId);
        }
    }

    public boolean isActive() {
        if (this.mPlayerState == 2) {
            return true;
        }
        return false;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("  " + toLogFriendlyString(this));
    }

    public static String toLogFriendlyString(AudioPlaybackConfiguration audioPlaybackConfiguration) {
        return new String("ID:" + audioPlaybackConfiguration.mPlayerIId + " -- type:" + toLogFriendlyPlayerType(audioPlaybackConfiguration.mPlayerType) + " -- u/pid:" + audioPlaybackConfiguration.mClientUid + "/" + audioPlaybackConfiguration.mClientPid + " -- state:" + toLogFriendlyPlayerState(audioPlaybackConfiguration.mPlayerState) + " -- attr:" + audioPlaybackConfiguration.mPlayerAttr);
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mPlayerIId), Integer.valueOf(this.mPlayerType), Integer.valueOf(this.mClientUid), Integer.valueOf(this.mClientPid));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        IPlayerShell iPlayerShell;
        parcel.writeInt(this.mPlayerIId);
        parcel.writeInt(this.mPlayerType);
        parcel.writeInt(this.mClientUid);
        parcel.writeInt(this.mClientPid);
        parcel.writeInt(this.mPlayerState);
        this.mPlayerAttr.writeToParcel(parcel, 0);
        synchronized (this) {
            iPlayerShell = this.mIPlayerShell;
        }
        parcel.writeStrongInterface(iPlayerShell == null ? null : iPlayerShell.getIPlayer());
    }

    private AudioPlaybackConfiguration(Parcel parcel) {
        this.mPlayerIId = parcel.readInt();
        this.mPlayerType = parcel.readInt();
        this.mClientUid = parcel.readInt();
        this.mClientPid = parcel.readInt();
        this.mPlayerState = parcel.readInt();
        this.mPlayerAttr = AudioAttributes.CREATOR.createFromParcel(parcel);
        IPlayer iPlayerAsInterface = IPlayer.Stub.asInterface(parcel.readStrongBinder());
        this.mIPlayerShell = iPlayerAsInterface != null ? new IPlayerShell(null, iPlayerAsInterface) : null;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AudioPlaybackConfiguration)) {
            return false;
        }
        AudioPlaybackConfiguration audioPlaybackConfiguration = (AudioPlaybackConfiguration) obj;
        if (this.mPlayerIId == audioPlaybackConfiguration.mPlayerIId && this.mPlayerType == audioPlaybackConfiguration.mPlayerType && this.mClientUid == audioPlaybackConfiguration.mClientUid && this.mClientPid == audioPlaybackConfiguration.mClientPid) {
            return true;
        }
        return false;
    }

    static final class IPlayerShell implements IBinder.DeathRecipient {
        private volatile IPlayer mIPlayer;
        final AudioPlaybackConfiguration mMonitor;

        IPlayerShell(AudioPlaybackConfiguration audioPlaybackConfiguration, IPlayer iPlayer) {
            this.mMonitor = audioPlaybackConfiguration;
            this.mIPlayer = iPlayer;
        }

        synchronized void monitorDeath() {
            if (this.mIPlayer == null) {
                return;
            }
            try {
                this.mIPlayer.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                if (this.mMonitor != null) {
                    Log.w(AudioPlaybackConfiguration.TAG, "Could not link to client death for piid=" + this.mMonitor.mPlayerIId, e);
                } else {
                    Log.w(AudioPlaybackConfiguration.TAG, "Could not link to client death", e);
                }
            }
        }

        IPlayer getIPlayer() {
            return this.mIPlayer;
        }

        @Override
        public void binderDied() {
            if (this.mMonitor != null) {
                this.mMonitor.playerDied();
            }
        }

        synchronized void release() {
            if (this.mIPlayer == null) {
                return;
            }
            this.mIPlayer.asBinder().unlinkToDeath(this, 0);
            this.mIPlayer = null;
            Binder.flushPendingCommands();
        }
    }

    public static String toLogFriendlyPlayerType(int i) {
        if (i == -1) {
            return "unknown";
        }
        switch (i) {
            case 1:
                return "android.media.AudioTrack";
            case 2:
                return "android.media.MediaPlayer";
            case 3:
                return "android.media.SoundPool";
            default:
                switch (i) {
                    case 11:
                        return "OpenSL ES AudioPlayer (Buffer Queue)";
                    case 12:
                        return "OpenSL ES AudioPlayer (URI/FD)";
                    case 13:
                        return "AAudio";
                    case 14:
                        return "hardware source";
                    case 15:
                        return "external proxy";
                    default:
                        return "unknown player type " + i + " - FIXME";
                }
        }
    }

    public static String toLogFriendlyPlayerState(int i) {
        switch (i) {
            case -1:
                return "unknown";
            case 0:
                return "released";
            case 1:
                return "idle";
            case 2:
                return "started";
            case 3:
                return "paused";
            case 4:
                return "stopped";
            default:
                return "unknown player state - FIXME";
        }
    }
}
