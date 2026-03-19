package com.android.server.audio;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IUidObserver;
import android.app.NotificationManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.hdmi.HdmiTvClient;
import android.media.AudioAttributes;
import android.media.AudioDevicePort;
import android.media.AudioFocusInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.IAudioFocusDispatcher;
import android.media.IAudioRoutesObserver;
import android.media.IAudioServerStateDispatcher;
import android.media.IAudioService;
import android.media.IPlaybackConfigDispatcher;
import android.media.IRecordingConfigDispatcher;
import android.media.IRingtonePlayer;
import android.media.IVolumeController;
import android.media.MediaPlayer;
import android.media.PlayerBase;
import android.media.SoundPool;
import android.media.VolumePolicy;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioPolicyConfig;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.BatteryService;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.audio.AudioEventLogger;
import com.android.server.audio.AudioServiceEvents;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;

public class AudioService extends IAudioService.Stub implements AccessibilityManager.TouchExplorationStateChangeListener, AccessibilityManager.AccessibilityServicesStateChangeListener {
    private static final String ASSET_FILE_VERSION = "1.0";
    private static final String ATTR_ASSET_FILE = "file";
    private static final String ATTR_ASSET_ID = "id";
    private static final String ATTR_GROUP_NAME = "name";
    private static final String ATTR_VERSION = "version";
    private static final int BTA2DP_DOCK_TIMEOUT_MILLIS = 8000;
    private static final int BT_HEADSET_CNCT_TIMEOUT_MS = 3000;
    private static final int BT_HEARING_AID_GAIN_MIN = -128;
    public static final String CONNECT_INTENT_KEY_ADDRESS = "address";
    public static final String CONNECT_INTENT_KEY_DEVICE_CLASS = "class";
    public static final String CONNECT_INTENT_KEY_HAS_CAPTURE = "hasCapture";
    public static final String CONNECT_INTENT_KEY_HAS_MIDI = "hasMIDI";
    public static final String CONNECT_INTENT_KEY_HAS_PLAYBACK = "hasPlayback";
    public static final String CONNECT_INTENT_KEY_PORT_NAME = "portName";
    public static final String CONNECT_INTENT_KEY_STATE = "state";
    protected static final boolean DEBUG_AP;
    protected static final boolean DEBUG_DEVICES;
    protected static final boolean DEBUG_MODE;
    protected static final boolean DEBUG_VOL;
    private static final int DEFAULT_STREAM_TYPE_OVERRIDE_DELAY_MS = 0;
    protected static final int DEFAULT_VOL_STREAM_NO_PLAYBACK = 3;
    private static final int DEVICE_MEDIA_UNMUTED_ON_PLUG = 67266444;
    private static final int DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG = 67264524;
    private static final int FLAG_ADJUST_VOLUME = 1;
    private static final String GROUP_TOUCH_SOUNDS = "touch_sounds";
    private static final int INDICATE_SYSTEM_READY_RETRY_DELAY_MS = 1000;
    protected static final boolean LOGD = !"user".equals(Build.TYPE);
    protected static int[] MAX_STREAM_VOLUME = null;
    protected static int[] MIN_STREAM_VOLUME = null;
    private static final int MSG_A2DP_DEVICE_CONFIG_CHANGE = 103;
    private static final int MSG_ACCESSORY_PLUG_MEDIA_UNMUTE = 27;
    private static final int MSG_AUDIO_SERVER_DIED = 4;
    private static final int MSG_BROADCAST_AUDIO_BECOMING_NOISY = 15;
    private static final int MSG_BROADCAST_BT_CONNECTION_STATE = 19;
    private static final int MSG_BTA2DP_DOCK_TIMEOUT = 106;
    private static final int MSG_BT_HEADSET_CNCT_FAILED = 9;
    private static final int MSG_CHECK_MUSIC_ACTIVE = 14;
    private static final int MSG_CONFIGURE_SAFE_MEDIA_VOLUME = 16;
    private static final int MSG_CONFIGURE_SAFE_MEDIA_VOLUME_FORCED = 17;
    private static final int MSG_DISABLE_AUDIO_FOR_UID = 104;
    private static final int MSG_DISPATCH_AUDIO_SERVER_STATE = 29;
    private static final int MSG_DYN_POLICY_MIX_STATE_UPDATE = 25;
    private static final int MSG_ENABLE_SURROUND_FORMATS = 30;
    private static final int MSG_INDICATE_SYSTEM_READY = 26;
    private static final int MSG_LOAD_SOUND_EFFECTS = 7;
    private static final int MSG_NOTIFY_VOL_EVENT = 28;
    private static final int MSG_PERSIST_MUSIC_ACTIVE_MS = 22;
    private static final int MSG_PERSIST_RINGER_MODE = 3;
    private static final int MSG_PERSIST_SAFE_VOLUME_STATE = 18;
    private static final int MSG_PERSIST_VOLUME = 1;
    private static final int MSG_PLAY_SOUND_EFFECT = 5;
    private static final int MSG_REPORT_NEW_ROUTES = 12;
    private static final int MSG_SET_A2DP_SINK_CONNECTION_STATE = 102;
    private static final int MSG_SET_A2DP_SRC_CONNECTION_STATE = 101;
    private static final int MSG_SET_ALL_VOLUMES = 10;
    private static final int MSG_SET_DEVICE_VOLUME = 0;
    private static final int MSG_SET_FORCE_BT_A2DP_USE = 13;
    private static final int MSG_SET_FORCE_USE = 8;
    private static final int MSG_SET_HEARING_AID_CONNECTION_STATE = 105;
    private static final int MSG_SET_WIRED_DEVICE_CONNECTION_STATE = 100;
    private static final int MSG_SYSTEM_READY = 21;
    private static final int MSG_UNLOAD_SOUND_EFFECTS = 20;
    private static final int MSG_UNMUTE_STREAM = 24;
    private static final int MUSIC_ACTIVE_POLL_PERIOD_MS = 60000;
    private static final int NUM_SOUNDPOOL_CHANNELS = 4;
    private static final int PERSIST_DELAY = 500;
    private static final String[] RINGER_MODE_NAMES;
    private static final int SAFE_MEDIA_VOLUME_ACTIVE = 3;
    private static final int SAFE_MEDIA_VOLUME_DISABLED = 1;
    private static final int SAFE_MEDIA_VOLUME_INACTIVE = 2;
    private static final int SAFE_MEDIA_VOLUME_NOT_CONFIGURED = 0;
    private static final int SAFE_VOLUME_CONFIGURE_TIMEOUT_MS = 30000;
    private static final int SCO_MODE_MAX = 2;
    private static final int SCO_MODE_RAW = 1;
    private static final int SCO_MODE_UNDEFINED = -1;
    private static final int SCO_MODE_VIRTUAL_CALL = 0;
    private static final int SCO_MODE_VR = 2;
    private static final int SCO_STATE_ACTIVATE_REQ = 1;
    private static final int SCO_STATE_ACTIVE_EXTERNAL = 2;
    private static final int SCO_STATE_ACTIVE_INTERNAL = 3;
    private static final int SCO_STATE_DEACTIVATE_REQ = 4;
    private static final int SCO_STATE_DEACTIVATING = 5;
    private static final int SCO_STATE_INACTIVE = 0;
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final int SOUND_EFFECTS_LOAD_TIMEOUT_MS = 5000;
    private static final String SOUND_EFFECTS_PATH = "/media/audio/ui/";
    private static final List<String> SOUND_EFFECT_FILES;
    private static final int[] STREAM_VOLUME_OPS;
    private static final String TAG = "AudioService";
    private static final String TAG_ASSET = "asset";
    private static final String TAG_AUDIO_ASSETS = "audio_assets";
    private static final String TAG_GROUP = "group";
    private static final int TOUCH_EXPLORE_STREAM_TYPE_OVERRIDE_DELAY_MS = 1000;
    private static final int UNMUTE_STREAM_DELAY = 350;
    private static final int UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX = 72000000;
    private static final AudioAttributes VIBRATION_ATTRIBUTES;
    private static Long mLastDeviceConnectMsgTime;
    protected static int[] mStreamVolumeAlias;
    private static boolean sIndependentA11yVolume;
    private static int sSoundEffectVolumeDb;
    private static int sStreamOverrideDelayMs;
    private BluetoothA2dp mA2dp;
    private int[] mAccessibilityServiceUids;
    private final AppOpsManager mAppOps;
    private PowerManager.WakeLock mAudioEventWakeLock;
    private AudioHandler mAudioHandler;
    private AudioSystemThread mAudioSystemThread;
    private boolean mBluetoothA2dpEnabled;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothDevice mBluetoothHeadsetDevice;

    @GuardedBy("mSettingsLock")
    private boolean mCameraSoundForced;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private String mDockAddress;
    private String mEnabledSurroundFormats;
    private int mEncodedSurroundMode;
    private IAudioPolicyCallback mExtVolumeController;
    private int mForcedUseForComm;
    private int mForcedUseForCommExt;
    private final boolean mHasVibrator;
    private boolean mHdmiCecSink;
    private MyDisplayStatusCallback mHdmiDisplayStatusCallback;
    private HdmiControlManager mHdmiManager;
    private HdmiPlaybackClient mHdmiPlaybackClient;
    private HdmiTvClient mHdmiTvClient;
    private BluetoothHearingAid mHearingAid;
    private final boolean mIsSingleVolume;
    private long mLoweredFromNormalToVibrateTime;
    private final MediaFocusControl mMediaFocusControl;
    private final boolean mMonitorRotation;
    private int mMusicActiveMs;
    private int mMuteAffectedStreams;
    private NotificationManager mNm;
    private StreamVolumeCommand mPendingVolumeCommand;
    private final int mPlatformType;
    private final PlaybackActivityMonitor mPlaybackMonitor;
    private final BroadcastReceiver mReceiver;
    private final RecordingActivityMonitor mRecordMonitor;
    private int mRingerAndZenModeMutedStreams;

    @GuardedBy("mSettingsLock")
    private int mRingerMode;
    private AudioManagerInternal.RingerModeDelegate mRingerModeDelegate;
    private volatile IRingtonePlayer mRingtonePlayer;
    private int mSafeMediaVolumeIndex;
    private Integer mSafeMediaVolumeState;
    private float mSafeUsbMediaVolumeDbfs;
    private int mSafeUsbMediaVolumeIndex;
    private int mScoAudioMode;
    private int mScoAudioState;
    private int mScoConnectionState;
    private SettingsObserver mSettingsObserver;
    private SoundPool mSoundPool;
    private SoundPoolCallback mSoundPoolCallBack;
    private SoundPoolListenerThread mSoundPoolListenerThread;
    private VolumeStreamState[] mStreamStates;
    private boolean mSurroundModeChanged;
    private boolean mSystemReady;
    private final boolean mUseFixedVolume;
    private final UserManagerInternal.UserRestrictionsListener mUserRestrictionsListener;
    private boolean mUserSwitchedReceived;
    private int mVibrateSetting;
    private Vibrator mVibrator;
    private final VolumeController mVolumeController = new VolumeController();
    private int mMode = 0;
    private final Object mSettingsLock = new Object();
    private final Object mSoundEffectsLock = new Object();
    private final int[][] SOUND_EFFECT_FILES_MAP = (int[][]) Array.newInstance((Class<?>) int.class, 10, 2);
    private final int[] STREAM_VOLUME_ALIAS_VOICE = {0, 2, 2, 3, 4, 2, 6, 2, 2, 3, 3};
    private final int[] STREAM_VOLUME_ALIAS_TELEVISION = {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3};
    private final int[] STREAM_VOLUME_ALIAS_DEFAULT = {0, 2, 2, 3, 4, 2, 6, 2, 2, 3, 3};
    private final AudioSystem.ErrorCallback mAudioSystemCallback = new AudioSystem.ErrorCallback() {
        public void onError(int i) {
            if (i == 100) {
                AudioService.sendMsg(AudioService.this.mAudioHandler, 4, 1, 0, 0, null, 0);
                AudioService.sendMsg(AudioService.this.mAudioHandler, 29, 2, 0, 0, null, 0);
            }
        }
    };

    @GuardedBy("mSettingsLock")
    private int mRingerModeExternal = -1;
    private int mRingerModeAffectedStreams = 0;
    private int mZenModeAffectedStreams = 0;
    private final ArrayMap<String, DeviceListSpec> mConnectedDevices = new ArrayMap<>();
    private final ArrayList<SetModeDeathHandler> mSetModeDeathHandlers = new ArrayList<>();
    private final ArrayList<ScoClient> mScoClients = new ArrayList<>();
    private Looper mSoundPoolLooper = null;
    private int mPrevVolDirection = 0;
    private int mVolumeControlStream = -1;
    private boolean mUserSelectedVolumeControlStream = false;
    private final Object mForceControlStreamLock = new Object();
    private ForceControlStreamClient mForceControlStreamClient = null;
    private final Object mBluetoothA2dpEnabledLock = new Object();
    final AudioRoutesInfo mCurAudioRoutes = new AudioRoutesInfo();
    final RemoteCallbackList<IAudioRoutesObserver> mRoutesObservers = new RemoteCallbackList<>();
    int mFixedVolumeDevices = 2890752;
    int mFullVolumeDevices = 0;
    private boolean mDockAudioMediaEnabled = true;
    private int mDockState = 0;
    private final Object mHearingAidLock = new Object();
    private final Object mA2dpAvrcpLock = new Object();
    private boolean mAvrcpAbsVolSupported = false;
    private VolumePolicy mVolumePolicy = VolumePolicy.DEFAULT;
    private final Object mAccessibilityServiceUidsLock = new Object();
    private final IUidObserver mUidObserver = new IUidObserver.Stub() {
        public void onUidStateChanged(int i, int i2, long j) {
        }

        public void onUidGone(int i, boolean z) {
            disableAudioForUid(false, i);
        }

        public void onUidActive(int i) throws RemoteException {
        }

        public void onUidIdle(int i, boolean z) {
        }

        public void onUidCachedChanged(int i, boolean z) {
            disableAudioForUid(z, i);
        }

        private void disableAudioForUid(boolean z, int i) {
            AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 104, z ? 1 : 0, i, null, 0);
        }
    };
    private int mRmtSbmxFullVolRefCount = 0;
    private ArrayList<RmtSbmxFullVolDeathHandler> mRmtSbmxFullVolDeathHandlers = new ArrayList<>();
    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            boolean zConnectBluetoothScoAudioHelper;
            if (i == 11) {
                List<BluetoothDevice> connectedDevices = bluetoothProfile.getConnectedDevices();
                if (connectedDevices.size() > 0) {
                    BluetoothDevice bluetoothDevice = connectedDevices.get(0);
                    synchronized (AudioService.this.mConnectedDevices) {
                        AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 101, bluetoothProfile.getConnectionState(bluetoothDevice), 0, bluetoothDevice, 0);
                    }
                    return;
                }
                return;
            }
            int i2 = 1;
            if (i != 21) {
                switch (i) {
                    case 1:
                        synchronized (AudioService.this.mScoClients) {
                            AudioService.this.mAudioHandler.removeMessages(9);
                            AudioService.this.mBluetoothHeadset = (BluetoothHeadset) bluetoothProfile;
                            AudioService.this.setBtScoActiveDevice(AudioService.this.mBluetoothHeadset.getActiveDevice());
                            AudioService.this.checkScoAudioState();
                            if (AudioService.this.mScoAudioState == 1 || AudioService.this.mScoAudioState == 4) {
                                if (AudioService.this.mBluetoothHeadsetDevice != null) {
                                    int i3 = AudioService.this.mScoAudioState;
                                    if (i3 == 1) {
                                        zConnectBluetoothScoAudioHelper = AudioService.connectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode);
                                        if (zConnectBluetoothScoAudioHelper) {
                                            AudioService.this.mScoAudioState = 3;
                                        }
                                    } else if (i3 == 4) {
                                        zConnectBluetoothScoAudioHelper = AudioService.disconnectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode);
                                        if (zConnectBluetoothScoAudioHelper) {
                                            AudioService.this.mScoAudioState = 5;
                                        }
                                    } else {
                                        zConnectBluetoothScoAudioHelper = false;
                                    }
                                    if (!zConnectBluetoothScoAudioHelper) {
                                    }
                                } else {
                                    zConnectBluetoothScoAudioHelper = false;
                                    if (!zConnectBluetoothScoAudioHelper) {
                                        AudioService.this.mScoAudioState = 0;
                                        AudioService.this.broadcastScoConnectionState(0);
                                    }
                                }
                            }
                            break;
                        }
                        return;
                    case 2:
                        synchronized (AudioService.this.mConnectedDevices) {
                            synchronized (AudioService.this.mA2dpAvrcpLock) {
                                AudioService.this.mA2dp = (BluetoothA2dp) bluetoothProfile;
                                List<BluetoothDevice> connectedDevices2 = AudioService.this.mA2dp.getConnectedDevices();
                                if (connectedDevices2.size() > 0) {
                                    BluetoothDevice bluetoothDevice2 = connectedDevices2.get(0);
                                    int connectionState = AudioService.this.mA2dp.getConnectionState(bluetoothDevice2);
                                    if (connectionState != 2) {
                                        i2 = 0;
                                    }
                                    AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 102, connectionState, -1, bluetoothDevice2, AudioService.this.checkSendBecomingNoisyIntent(128, i2, 0));
                                }
                                break;
                            }
                        }
                        return;
                    default:
                        return;
                }
            }
            synchronized (AudioService.this.mConnectedDevices) {
                synchronized (AudioService.this.mHearingAidLock) {
                    AudioService.this.mHearingAid = (BluetoothHearingAid) bluetoothProfile;
                    List<BluetoothDevice> connectedDevices3 = AudioService.this.mHearingAid.getConnectedDevices();
                    if (connectedDevices3.size() > 0) {
                        BluetoothDevice bluetoothDevice3 = connectedDevices3.get(0);
                        int connectionState2 = AudioService.this.mHearingAid.getConnectionState(bluetoothDevice3);
                        if (connectionState2 != 2) {
                            i2 = 0;
                        }
                        AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 105, connectionState2, 0, bluetoothDevice3, AudioService.this.checkSendBecomingNoisyIntent(134217728, i2, 0));
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(int i) {
            if (i == 11) {
                AudioService.this.disconnectA2dpSink();
                return;
            }
            if (i != 21) {
                switch (i) {
                    case 1:
                        AudioService.this.disconnectHeadset();
                        break;
                    case 2:
                        AudioService.this.disconnectA2dp();
                        break;
                }
                return;
            }
            AudioService.this.disconnectHearingAid();
        }
    };
    int mBecomingNoisyIntentDevices = 201490316;
    private int mMcc = 0;
    private final int mSafeMediaVolumeDevices = 67108876;
    private boolean mHdmiSystemAudioSupported = false;
    final int LOG_NB_EVENTS_PHONE_STATE = 20;
    final int LOG_NB_EVENTS_WIRED_DEV_CONNECTION = 30;
    final int LOG_NB_EVENTS_FORCE_USE = 20;
    final int LOG_NB_EVENTS_VOLUME = 40;
    final int LOG_NB_EVENTS_DYN_POLICY = 10;
    private final AudioEventLogger mModeLogger = new AudioEventLogger(20, "phone state (logged after successfull call to AudioSystem.setPhoneState(int))");
    private final AudioEventLogger mWiredDevLogger = new AudioEventLogger(30, "wired device connection (logged before onSetWiredDeviceConnectionState() is executed)");
    private final AudioEventLogger mForceUseLogger = new AudioEventLogger(20, "force use (logged before setForceUse() is executed)");
    private final AudioEventLogger mVolumeLogger = new AudioEventLogger(40, "volume changes (logged when command received by AudioService)");
    private final AudioEventLogger mDynPolicyLogger = new AudioEventLogger(10, "dynamic policy events (logged when command received by AudioService)");
    private final Object mExtVolumeControllerLock = new Object();
    private final AudioSystem.DynamicPolicyCallback mDynPolicyCallback = new AudioSystem.DynamicPolicyCallback() {
        public void onDynamicPolicyMixStateUpdate(String str, int i) {
            if (!TextUtils.isEmpty(str)) {
                AudioService.sendMsg(AudioService.this.mAudioHandler, 25, 2, i, 0, str, 0);
            }
        }
    };
    private HashMap<IBinder, AsdProxy> mAudioServerStateListeners = new HashMap<>();
    private final HashMap<IBinder, AudioPolicyProxy> mAudioPolicies = new HashMap<>();

    @GuardedBy("mAudioPolicies")
    private int mAudioPolicyCounter = 0;
    private final UserManagerInternal mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
    private final ActivityManagerInternal mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);

    static int access$11808(AudioService audioService) {
        int i = audioService.mAudioPolicyCounter;
        audioService.mAudioPolicyCounter = i + 1;
        return i;
    }

    static {
        boolean z = true;
        DEBUG_MODE = Log.isLoggable("AudioService.MOD", 3) || LOGD;
        DEBUG_AP = Log.isLoggable("AudioService.AP", 3) || LOGD;
        DEBUG_VOL = Log.isLoggable("AudioService.VOL", 3) || LOGD;
        if (!Log.isLoggable("AudioService.DEVICES", 3) && !LOGD) {
            z = false;
        }
        DEBUG_DEVICES = z;
        SOUND_EFFECT_FILES = new ArrayList();
        MAX_STREAM_VOLUME = new int[]{7, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15};
        MIN_STREAM_VOLUME = new int[]{1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
        STREAM_VOLUME_OPS = new int[]{34, 36, 35, 36, 37, 38, 39, 36, 36, 36, 64};
        VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
        mLastDeviceConnectMsgTime = new Long(0L);
        sIndependentA11yVolume = false;
        RINGER_MODE_NAMES = new String[]{"SILENT", "VIBRATE", PriorityDump.PRIORITY_ARG_NORMAL};
    }

    private boolean isPlatformVoice() {
        return this.mPlatformType == 1;
    }

    private boolean isPlatformTelevision() {
        return this.mPlatformType == 2;
    }

    private boolean isPlatformAutomotive() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
    }

    private class DeviceListSpec {
        String mDeviceAddress;
        String mDeviceName;
        int mDeviceType;

        public DeviceListSpec(int i, String str, String str2) {
            this.mDeviceType = i;
            this.mDeviceName = str;
            this.mDeviceAddress = str2;
        }

        public String toString() {
            return "[type:0x" + Integer.toHexString(this.mDeviceType) + " name:" + this.mDeviceName + " address:" + this.mDeviceAddress + "]";
        }
    }

    private String makeDeviceListKey(int i, String str) {
        return "0x" + Integer.toHexString(i) + ":" + str;
    }

    public static String makeAlsaAddressString(int i, int i2) {
        return "card=" + i + ";device=" + i2 + ";";
    }

    public static final class Lifecycle extends SystemService {
        private AudioService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new AudioService(context);
        }

        @Override
        public void onStart() {
            publishBinderService("audio", this.mService);
        }

        @Override
        public void onBootPhase(int i) {
            if (i == 550) {
                this.mService.systemReady();
            }
        }
    }

    public AudioService(Context context) {
        int i;
        this.mReceiver = new AudioServiceBroadcastReceiver();
        this.mUserRestrictionsListener = new AudioServiceUserRestrictionsListener();
        this.mHdmiDisplayStatusCallback = new MyDisplayStatusCallback();
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mPlatformType = AudioSystem.getPlatformType(context);
        this.mIsSingleVolume = AudioSystem.isSingleVolume(context);
        this.mAudioEventWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "handleAudioEvent");
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mHasVibrator = this.mVibrator == null ? false : this.mVibrator.hasVibrator();
        int i2 = SystemProperties.getInt("ro.config.vc_call_vol_steps", -1);
        if (i2 != -1) {
            MAX_STREAM_VOLUME[0] = i2;
            AudioSystem.DEFAULT_STREAM_VOLUME[0] = (i2 * 3) / 4;
        }
        int i3 = SystemProperties.getInt("ro.config.media_vol_steps", -1);
        if (i3 != -1) {
            MAX_STREAM_VOLUME[3] = i3;
        }
        int i4 = SystemProperties.getInt("ro.config.media_vol_default", -1);
        if (i4 != -1 && i4 <= MAX_STREAM_VOLUME[3]) {
            AudioSystem.DEFAULT_STREAM_VOLUME[3] = i4;
        } else if (isPlatformTelevision()) {
            AudioSystem.DEFAULT_STREAM_VOLUME[3] = MAX_STREAM_VOLUME[3] / 4;
        } else {
            AudioSystem.DEFAULT_STREAM_VOLUME[3] = MAX_STREAM_VOLUME[3] / 3;
        }
        int i5 = SystemProperties.getInt("ro.config.alarm_vol_steps", -1);
        if (i5 != -1) {
            MAX_STREAM_VOLUME[4] = i5;
        }
        int i6 = SystemProperties.getInt("ro.config.alarm_vol_default", -1);
        if (i6 == -1 || i6 > MAX_STREAM_VOLUME[4]) {
            AudioSystem.DEFAULT_STREAM_VOLUME[4] = (6 * MAX_STREAM_VOLUME[4]) / 7;
        } else {
            AudioSystem.DEFAULT_STREAM_VOLUME[4] = i6;
        }
        int i7 = SystemProperties.getInt("ro.config.system_vol_steps", -1);
        if (i7 != -1) {
            MAX_STREAM_VOLUME[1] = i7;
        }
        int i8 = SystemProperties.getInt("ro.config.system_vol_default", -1);
        if (i8 == -1 || i8 > MAX_STREAM_VOLUME[1]) {
            AudioSystem.DEFAULT_STREAM_VOLUME[1] = MAX_STREAM_VOLUME[1];
        } else {
            AudioSystem.DEFAULT_STREAM_VOLUME[1] = i8;
        }
        sSoundEffectVolumeDb = context.getResources().getInteger(R.integer.config_dreamsBatteryLevelMinimumWhenNotPowered);
        this.mForcedUseForComm = 0;
        createAudioSystemThread();
        AudioSystem.setErrorCallback(this.mAudioSystemCallback);
        boolean cameraSoundForced = readCameraSoundForced();
        this.mCameraSoundForced = new Boolean(cameraSoundForced).booleanValue();
        AudioHandler audioHandler = this.mAudioHandler;
        if (!cameraSoundForced) {
            i = 0;
        } else {
            i = 11;
        }
        sendMsg(audioHandler, 8, 2, 4, i, new String("AudioService ctor"), 0);
        this.mSafeMediaVolumeState = new Integer(Settings.Global.getInt(this.mContentResolver, "audio_safe_volume_state", 0));
        this.mSafeMediaVolumeIndex = this.mContext.getResources().getInteger(R.integer.config_doublePressOnPowerBehavior) * 10;
        this.mUseFixedVolume = this.mContext.getResources().getBoolean(R.^attr-private.pointerIconVectorFillInverse);
        updateStreamVolumeAlias(false, TAG);
        readPersistedSettings();
        readUserRestrictions();
        this.mSettingsObserver = new SettingsObserver();
        createStreamStates();
        this.mSafeUsbMediaVolumeIndex = getSafeUsbMediaVolumeIndex();
        this.mPlaybackMonitor = new PlaybackActivityMonitor(context, MAX_STREAM_VOLUME[4]);
        this.mMediaFocusControl = new MediaFocusControl(this.mContext, this.mPlaybackMonitor);
        this.mRecordMonitor = new RecordingActivityMonitor(this.mContext);
        readAndSetLowRamDevice();
        this.mRingerAndZenModeMutedStreams = 0;
        setRingerModeInt(getRingerModeInternal(), false);
        IntentFilter intentFilter = new IntentFilter("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED");
        intentFilter.addAction("android.intent.action.DOCK_EVENT");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_BACKGROUND");
        intentFilter.addAction("android.intent.action.USER_FOREGROUND");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mMonitorRotation = SystemProperties.getBoolean("ro.audio.monitorRotation", false);
        if (this.mMonitorRotation) {
            RotationHelper.init(this.mContext, this.mAudioHandler);
        }
        intentFilter.addAction("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
        intentFilter.addAction("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
        context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
        LocalServices.addService(AudioManagerInternal.class, new AudioServiceInternal());
        this.mUserManagerInternal.addUserRestrictionsListener(this.mUserRestrictionsListener);
        this.mRecordMonitor.initMonitor();
    }

    public void systemReady() {
        sendMsg(this.mAudioHandler, 21, 2, 0, 0, null, 0);
    }

    public void onSystemReady() {
        this.mSystemReady = true;
        sendMsg(this.mAudioHandler, 7, 2, 0, 0, null, 0);
        this.mScoConnectionState = -1;
        resetBluetoothSco();
        getBluetoothHeadset();
        Intent intent = new Intent("android.media.SCO_AUDIO_STATE_CHANGED");
        intent.putExtra("android.media.extra.SCO_AUDIO_STATE", 0);
        sendStickyBroadcastToAll(intent);
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null) {
            defaultAdapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 2);
            defaultAdapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 21);
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.hdmi.cec")) {
            this.mHdmiManager = (HdmiControlManager) this.mContext.getSystemService(HdmiControlManager.class);
            synchronized (this.mHdmiManager) {
                this.mHdmiTvClient = this.mHdmiManager.getTvClient();
                if (this.mHdmiTvClient != null) {
                    this.mFixedVolumeDevices &= -2883587;
                }
                this.mHdmiPlaybackClient = this.mHdmiManager.getPlaybackClient();
                this.mHdmiCecSink = false;
            }
        }
        this.mNm = (NotificationManager) this.mContext.getSystemService("notification");
        sendMsg(this.mAudioHandler, 17, 0, 0, 0, TAG, SystemProperties.getBoolean("audio.safemedia.bypass", false) ? 0 : SAFE_VOLUME_CONFIGURE_TIMEOUT_MS);
        initA11yMonitoring();
        onIndicateSystemReady();
    }

    void onIndicateSystemReady() {
        if (AudioSystem.systemReady() == 0) {
            return;
        }
        sendMsg(this.mAudioHandler, 26, 0, 0, 0, null, 1000);
    }

    public void onAudioServerDied() {
        int i;
        int i2;
        if (!this.mSystemReady || AudioSystem.checkAudioFlinger() != 0) {
            Log.e(TAG, "Audioserver died.");
            sendMsg(this.mAudioHandler, 4, 1, 0, 0, null, 500);
            return;
        }
        Log.e(TAG, "Audioserver started.");
        AudioSystem.setParameters("restarting=true");
        readAndSetLowRamDevice();
        synchronized (this.mConnectedDevices) {
            i = 0;
            for (int i3 = 0; i3 < this.mConnectedDevices.size(); i3++) {
                DeviceListSpec deviceListSpecValueAt = this.mConnectedDevices.valueAt(i3);
                AudioSystem.setDeviceConnectionState(deviceListSpecValueAt.mDeviceType, 1, deviceListSpecValueAt.mDeviceAddress, deviceListSpecValueAt.mDeviceName);
            }
        }
        if (AudioSystem.setPhoneState(this.mMode) == 0) {
            this.mModeLogger.log(new AudioEventLogger.StringEvent("onAudioServerDied causes setPhoneState(" + AudioSystem.modeToString(this.mMode) + ")"));
        }
        this.mForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(0, this.mForcedUseForComm, "onAudioServerDied"));
        AudioSystem.setForceUse(0, this.mForcedUseForComm);
        this.mForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(2, this.mForcedUseForComm, "onAudioServerDied"));
        AudioSystem.setForceUse(2, this.mForcedUseForComm);
        synchronized (this.mSettingsLock) {
            if (this.mCameraSoundForced) {
                i2 = 11;
            } else {
                i2 = 0;
            }
        }
        this.mForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(4, i2, "onAudioServerDied"));
        if (LOGD) {
            Log.i(TAG, "setForceUse(" + AudioSystem.forceUseUsageToString(4) + ", " + AudioSystem.forceUseUsageToString(0) + ", " + AudioSystem.forceUseConfigToString(this.mForcedUseForComm) + ", " + AudioSystem.forceUseUsageToString(2) + ", " + AudioSystem.forceUseConfigToString(this.mForcedUseForComm) + ", " + AudioSystem.forceUseConfigToString(i2) + ") due to onAudioServerDied");
        }
        AudioSystem.setForceUse(4, i2);
        int numStreamTypes = AudioSystem.getNumStreamTypes() - 1;
        while (true) {
            if (numStreamTypes < 0) {
                break;
            }
            VolumeStreamState volumeStreamState = this.mStreamStates[numStreamTypes];
            AudioSystem.initStreamVolume(numStreamTypes, volumeStreamState.mIndexMin / 10, volumeStreamState.mIndexMax / 10);
            volumeStreamState.applyAllVolumes();
            numStreamTypes--;
        }
        updateMasterMono(this.mContentResolver);
        setRingerModeInt(getRingerModeInternal(), false);
        if (this.mMonitorRotation) {
            RotationHelper.updateOrientation();
        }
        synchronized (this.mBluetoothA2dpEnabledLock) {
            int i4 = this.mBluetoothA2dpEnabled ? 0 : 10;
            this.mForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(1, i4, "onAudioServerDied"));
            if (LOGD) {
                Log.i(TAG, "setForceUse(" + AudioSystem.forceUseUsageToString(1) + ", " + AudioSystem.forceUseConfigToString(i4) + ") due to onAudioServerDied");
            }
            AudioSystem.setForceUse(1, i4);
        }
        synchronized (this.mSettingsLock) {
            if (this.mDockAudioMediaEnabled) {
                i = 8;
            }
            this.mForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(3, i, "onAudioServerDied"));
            if (LOGD) {
                Log.i(TAG, "setForceUse(" + AudioSystem.forceUseUsageToString(3) + ", " + AudioSystem.forceUseConfigToString(i) + ") due to onAudioServerDied");
            }
            AudioSystem.setForceUse(3, i);
            sendEncodedSurroundMode(this.mContentResolver, "onAudioServerDied");
            sendEnabledSurroundFormats(this.mContentResolver, true);
        }
        if (this.mHdmiManager != null) {
            synchronized (this.mHdmiManager) {
                if (this.mHdmiTvClient != null) {
                    setHdmiSystemAudioSupported(this.mHdmiSystemAudioSupported);
                }
            }
        }
        synchronized (this.mAudioPolicies) {
            Iterator<AudioPolicyProxy> it = this.mAudioPolicies.values().iterator();
            while (it.hasNext()) {
                it.next().connectMixes();
            }
        }
        onIndicateSystemReady();
        AudioSystem.setParameters("restarting=false");
        sendMsg(this.mAudioHandler, 29, 2, 1, 0, null, 0);
    }

    private void onDispatchAudioServerStateChange(boolean z) {
        synchronized (this.mAudioServerStateListeners) {
            Iterator<AsdProxy> it = this.mAudioServerStateListeners.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().callback().dispatchAudioServerStateChange(z);
                } catch (RemoteException e) {
                    Log.w(TAG, "Could not call dispatchAudioServerStateChange()", e);
                }
            }
        }
    }

    private void createAudioSystemThread() {
        this.mAudioSystemThread = new AudioSystemThread();
        this.mAudioSystemThread.start();
        waitForAudioHandlerCreation();
    }

    private void waitForAudioHandlerCreation() {
        synchronized (this) {
            while (this.mAudioHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting on volume handler.");
                }
            }
        }
    }

    private void checkAllAliasStreamVolumes() {
        synchronized (this.mSettingsLock) {
            synchronized (VolumeStreamState.class) {
                int numStreamTypes = AudioSystem.getNumStreamTypes();
                for (int i = 0; i < numStreamTypes; i++) {
                    this.mStreamStates[i].setAllIndexes(this.mStreamStates[mStreamVolumeAlias[i]], TAG);
                    if (!this.mStreamStates[i].mIsMuted) {
                        this.mStreamStates[i].applyAllVolumes();
                    }
                }
            }
        }
    }

    private void checkAllFixedVolumeDevices() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < numStreamTypes; i++) {
            this.mStreamStates[i].checkFixedVolumeDevices();
        }
    }

    private void checkAllFixedVolumeDevices(int i) {
        this.mStreamStates[i].checkFixedVolumeDevices();
    }

    private void checkMuteAffectedStreams() {
        for (int i = 0; i < this.mStreamStates.length; i++) {
            VolumeStreamState volumeStreamState = this.mStreamStates[i];
            if (volumeStreamState.mIndexMin > 0 && volumeStreamState.mStreamType != 0) {
                this.mMuteAffectedStreams = (~(1 << volumeStreamState.mStreamType)) & this.mMuteAffectedStreams;
            }
        }
    }

    private void createStreamStates() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        VolumeStreamState[] volumeStreamStateArr = new VolumeStreamState[numStreamTypes];
        this.mStreamStates = volumeStreamStateArr;
        for (int i = 0; i < numStreamTypes; i++) {
            volumeStreamStateArr[i] = new VolumeStreamState(Settings.System.VOLUME_SETTINGS_INT[mStreamVolumeAlias[i]], i);
        }
        checkAllFixedVolumeDevices();
        checkAllAliasStreamVolumes();
        checkMuteAffectedStreams();
        updateDefaultVolumes();
    }

    private void updateDefaultVolumes() {
        for (int i = 0; i < this.mStreamStates.length; i++) {
            if (i != mStreamVolumeAlias[i]) {
                AudioSystem.DEFAULT_STREAM_VOLUME[i] = rescaleIndex(AudioSystem.DEFAULT_STREAM_VOLUME[mStreamVolumeAlias[i]], mStreamVolumeAlias[i], i);
            }
        }
    }

    private void dumpStreamStates(PrintWriter printWriter) {
        printWriter.println("\nStream volumes (device: index)");
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < numStreamTypes; i++) {
            printWriter.println("- " + AudioSystem.STREAM_NAMES[i] + ":");
            this.mStreamStates[i].dump(printWriter);
            printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        printWriter.print("\n- mute affected streams = 0x");
        printWriter.println(Integer.toHexString(this.mMuteAffectedStreams));
    }

    private void updateStreamVolumeAlias(boolean z, String str) {
        int i = 3;
        int i2 = sIndependentA11yVolume ? 10 : 3;
        if (this.mIsSingleVolume) {
            mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_TELEVISION;
        } else if (this.mPlatformType == 1) {
            mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_VOICE;
            i = 2;
        } else {
            mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_DEFAULT;
        }
        if (this.mIsSingleVolume) {
            this.mRingerModeAffectedStreams = 0;
        } else if (isInCommunication()) {
            this.mRingerModeAffectedStreams &= -257;
            i = 0;
        } else {
            this.mRingerModeAffectedStreams |= 256;
        }
        mStreamVolumeAlias[8] = i;
        mStreamVolumeAlias[10] = i2;
        if (z && this.mStreamStates != null) {
            updateDefaultVolumes();
            synchronized (this.mSettingsLock) {
                synchronized (VolumeStreamState.class) {
                    this.mStreamStates[8].setAllIndexes(this.mStreamStates[i], str);
                    this.mStreamStates[10].mVolumeIndexSettingName = Settings.System.VOLUME_SETTINGS_INT[i2];
                    this.mStreamStates[10].setAllIndexes(this.mStreamStates[i2], str);
                    this.mStreamStates[10].refreshRange(mStreamVolumeAlias[10]);
                }
            }
            if (sIndependentA11yVolume) {
                this.mStreamStates[10].readSettings();
            }
            setRingerModeInt(getRingerModeInternal(), false);
            sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[8], 0);
            sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[10], 0);
        }
    }

    private void readDockAudioSettings(ContentResolver contentResolver) {
        this.mDockAudioMediaEnabled = Settings.Global.getInt(contentResolver, "dock_audio_media_enabled", 0) == 1;
        sendMsg(this.mAudioHandler, 8, 2, 3, this.mDockAudioMediaEnabled ? 8 : 0, new String("readDockAudioSettings"), 0);
    }

    private void updateMasterMono(ContentResolver contentResolver) {
        boolean z = Settings.System.getIntForUser(contentResolver, "master_mono", 0, -2) == 1;
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Master mono %b", Boolean.valueOf(z)));
        }
        AudioSystem.setMasterMono(z);
    }

    private void sendEncodedSurroundMode(ContentResolver contentResolver, String str) {
        sendEncodedSurroundMode(Settings.Global.getInt(contentResolver, "encoded_surround_output", 0), str);
    }

    private void sendEncodedSurroundMode(int i, String str) {
        int i2;
        int i3;
        switch (i) {
            case 0:
                i2 = 0;
                i3 = i2;
                break;
            case 1:
                i2 = 13;
                i3 = i2;
                break;
            case 2:
                i2 = 14;
                i3 = i2;
                break;
            case 3:
                i2 = 15;
                i3 = i2;
                break;
            default:
                Log.e(TAG, "updateSurroundSoundSettings: illegal value " + i);
                i3 = 16;
                break;
        }
        if (i3 != 16) {
            sendMsg(this.mAudioHandler, 8, 2, 6, i3, str, 0);
        }
    }

    private void sendEnabledSurroundFormats(ContentResolver contentResolver, boolean z) {
        boolean z2;
        if (this.mEncodedSurroundMode != 3) {
            return;
        }
        String string = Settings.Global.getString(contentResolver, "encoded_surround_output_enabled_formats");
        if (string == null) {
            string = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        if (!z && TextUtils.equals(string, this.mEnabledSurroundFormats)) {
            return;
        }
        this.mEnabledSurroundFormats = string;
        String[] strArrSplit = TextUtils.split(string, ",");
        ArrayList arrayList = new ArrayList();
        for (String str : strArrSplit) {
            try {
                int iIntValue = Integer.valueOf(str).intValue();
                int[] iArr = AudioFormat.SURROUND_SOUND_ENCODING;
                int length = iArr.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        if (iArr[i] != iIntValue) {
                            i++;
                        } else {
                            z2 = true;
                            break;
                        }
                    } else {
                        z2 = false;
                        break;
                    }
                }
                if (z2 && !arrayList.contains(Integer.valueOf(iIntValue))) {
                    arrayList.add(Integer.valueOf(iIntValue));
                }
            } catch (Exception e) {
                Log.e(TAG, "Invalid enabled surround format:" + str);
            }
        }
        Settings.Global.putString(this.mContext.getContentResolver(), "encoded_surround_output_enabled_formats", TextUtils.join(",", arrayList));
        sendMsg(this.mAudioHandler, 30, 2, 0, 0, arrayList, 0);
    }

    private void onEnableSurroundFormats(ArrayList<Integer> arrayList) {
        for (int i : AudioFormat.SURROUND_SOUND_ENCODING) {
            boolean zContains = arrayList.contains(Integer.valueOf(i));
            Log.i(TAG, "enable surround format:" + i + " " + zContains + " " + AudioSystem.setSurroundFormatEnabled(i, zContains));
        }
    }

    private void readPersistedSettings() {
        ContentResolver contentResolver = this.mContentResolver;
        int i = 2;
        int i2 = Settings.Global.getInt(contentResolver, "mode_ringer", 2);
        int i3 = !isValidRingerMode(i2) ? 2 : i2;
        if (i3 == 1 && !this.mHasVibrator) {
            i3 = 0;
        }
        if (i3 != i2) {
            Settings.Global.putInt(contentResolver, "mode_ringer", i3);
        }
        if (this.mUseFixedVolume || this.mIsSingleVolume) {
            i3 = 2;
        }
        synchronized (this.mSettingsLock) {
            this.mRingerMode = i3;
            if (this.mRingerModeExternal == -1) {
                this.mRingerModeExternal = this.mRingerMode;
            }
            this.mVibrateSetting = AudioSystem.getValueForVibrateSetting(0, 1, this.mHasVibrator ? 2 : 0);
            int i4 = this.mVibrateSetting;
            if (!this.mHasVibrator) {
                i = 0;
            }
            this.mVibrateSetting = AudioSystem.getValueForVibrateSetting(i4, 0, i);
            updateRingerAndZenModeAffectedStreams();
            readDockAudioSettings(contentResolver);
            sendEncodedSurroundMode(contentResolver, "readPersistedSettings");
            sendEnabledSurroundFormats(contentResolver, true);
        }
        this.mMuteAffectedStreams = Settings.System.getIntForUser(contentResolver, "mute_streams_affected", 47, -2);
        updateMasterMono(contentResolver);
        broadcastRingerMode("android.media.RINGER_MODE_CHANGED", this.mRingerModeExternal);
        broadcastRingerMode("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION", this.mRingerMode);
        broadcastVibrateSetting(0);
        broadcastVibrateSetting(1);
        this.mVolumeController.loadSettings(contentResolver);
    }

    private void readUserRestrictions() {
        int currentUserId = getCurrentUserId();
        boolean z = this.mUserManagerInternal.getUserRestriction(currentUserId, "disallow_unmute_device") || this.mUserManagerInternal.getUserRestriction(currentUserId, "no_adjust_volume");
        if (this.mUseFixedVolume) {
            AudioSystem.setMasterVolume(1.0f);
            z = false;
        }
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Master mute %s, user=%d", Boolean.valueOf(z), Integer.valueOf(currentUserId)));
        }
        setSystemAudioMute(z);
        AudioSystem.setMasterMute(z);
        broadcastMasterMuteStatus(z);
        boolean userRestriction = this.mUserManagerInternal.getUserRestriction(currentUserId, "no_unmute_microphone");
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Mic mute %s, user=%d", Boolean.valueOf(userRestriction), Integer.valueOf(currentUserId)));
        }
        AudioSystem.muteMicrophone(userRestriction);
    }

    private int rescaleIndex(int i, int i2, int i3) {
        int maxIndex = ((i * this.mStreamStates[i3].getMaxIndex()) + (this.mStreamStates[i2].getMaxIndex() / 2)) / this.mStreamStates[i2].getMaxIndex();
        if (maxIndex < this.mStreamStates[i3].getMinIndex()) {
            return this.mStreamStates[i3].getMinIndex();
        }
        return maxIndex;
    }

    public void adjustSuggestedStreamVolume(int i, int i2, int i3, String str, String str2) {
        IAudioPolicyCallback iAudioPolicyCallback;
        synchronized (this.mExtVolumeControllerLock) {
            iAudioPolicyCallback = this.mExtVolumeController;
        }
        if (iAudioPolicyCallback != null) {
            sendMsg(this.mAudioHandler, 28, 2, i, 0, iAudioPolicyCallback, 0);
        } else {
            adjustSuggestedStreamVolume(i, i2, i3, str, str2, Binder.getCallingUid());
        }
    }

    private void adjustSuggestedStreamVolume(int i, int i2, int i3, String str, String str2, int i4) {
        int i5;
        int activeStreamType;
        boolean zWasStreamActiveRecently;
        int i6;
        if (DEBUG_VOL) {
            Log.d(TAG, "adjustSuggestedStreamVolume() stream=" + i2 + ", flags=" + i3 + ", caller=" + str2 + ", volControlStream=" + this.mVolumeControlStream + ", userSelect=" + this.mUserSelectedVolumeControlStream);
        }
        this.mVolumeLogger.log(new AudioServiceEvents.VolumeEvent(0, i2, i, i3, str + SliceClientPermissions.SliceAuthority.DELIMITER + str2 + " uid:" + i4));
        synchronized (this.mForceControlStreamLock) {
            i5 = 0;
            if (this.mUserSelectedVolumeControlStream) {
                activeStreamType = this.mVolumeControlStream;
            } else {
                activeStreamType = getActiveStreamType(i2);
                if (activeStreamType == 2 || activeStreamType == 5) {
                    zWasStreamActiveRecently = wasStreamActiveRecently(activeStreamType, 0);
                } else {
                    zWasStreamActiveRecently = AudioSystem.isStreamActive(activeStreamType, 0);
                }
                if (!zWasStreamActiveRecently && this.mVolumeControlStream != -1) {
                    activeStreamType = this.mVolumeControlStream;
                }
            }
        }
        boolean zIsMuteAdjust = isMuteAdjust(i);
        ensureValidStreamType(activeStreamType);
        int i7 = mStreamVolumeAlias[activeStreamType];
        if ((i3 & 4) != 0 && i7 != 2) {
            i3 &= -5;
        }
        if (this.mVolumeController.suppressAdjustment(i7, i3, zIsMuteAdjust)) {
            int i8 = i3 & (-5) & (-17);
            if (DEBUG_VOL) {
                Log.d(TAG, "Volume controller suppressed adjustment");
            }
            i6 = i8;
        } else {
            i5 = i;
            i6 = i3;
        }
        adjustStreamVolume(activeStreamType, i5, i6, str, str2, i4);
    }

    public void adjustStreamVolume(int i, int i2, int i3, String str) {
        if (i == 10 && !canChangeAccessibilityVolume()) {
            Log.w(TAG, "Trying to call adjustStreamVolume() for a11y withoutCHANGE_ACCESSIBILITY_VOLUME / callingPackage=" + str);
            return;
        }
        this.mVolumeLogger.log(new AudioServiceEvents.VolumeEvent(1, i, i2, i3, str));
        adjustStreamVolume(i, i2, i3, str, str, Binder.getCallingUid());
    }

    protected void adjustStreamVolume(int i, int i2, int i3, String str, String str2, int i4) {
        int uid;
        int iRescaleIndex;
        int i5;
        int i6;
        int i7;
        int i8;
        int iCheckForRingerModeChange;
        int i9;
        boolean z;
        int i10;
        int i11;
        int i12;
        VolumeStreamState volumeStreamState;
        int i13;
        int i14;
        boolean z2;
        boolean z3;
        if (this.mUseFixedVolume) {
            return;
        }
        if (DEBUG_VOL) {
            Log.d(TAG, "adjustStreamVolume() stream=" + i + ", dir=" + i2 + ", flags=" + i3 + ", caller=" + str2);
        }
        ensureValidDirection(i2);
        ensureValidStreamType(i);
        boolean zIsMuteAdjust = isMuteAdjust(i2);
        if (zIsMuteAdjust && !isStreamAffectedByMute(i)) {
            return;
        }
        if (zIsMuteAdjust && i == 0 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Log.w(TAG, "MODIFY_PHONE_STATE Permission Denial: adjustStreamVolume from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        int i15 = mStreamVolumeAlias[i];
        VolumeStreamState volumeStreamState2 = this.mStreamStates[i15];
        int deviceForStream = getDeviceForStream(i15);
        int index = volumeStreamState2.getIndex(deviceForStream);
        int i16 = deviceForStream & 896;
        if (i16 == 0 && (i3 & 64) != 0) {
            return;
        }
        if (i4 == 1000) {
            uid = UserHandle.getUid(getCurrentUserId(), UserHandle.getAppId(i4));
        } else {
            uid = i4;
        }
        if (this.mAppOps.noteOp(STREAM_VOLUME_OPS[i15], uid, str) != 0) {
            return;
        }
        synchronized (this.mSafeMediaVolumeState) {
            this.mPendingVolumeCommand = null;
        }
        int i17 = i3 & (-33);
        if (i15 == 3 && (this.mFixedVolumeDevices & deviceForStream) != 0) {
            i17 |= 32;
            if (this.mSafeMediaVolumeState.intValue() == 3 && (67108876 & deviceForStream) != 0) {
                iRescaleIndex = safeMediaVolumeIndex(deviceForStream);
            } else {
                iRescaleIndex = volumeStreamState2.getMaxIndex();
            }
            if (index != 0) {
                i5 = iRescaleIndex;
                i6 = i5;
            }
            if ((i17 & 2) == 0 || i15 == getUiSoundsStreamType()) {
                if (getRingerModeInternal() == 1) {
                    i17 &= -17;
                }
                int i18 = i17;
                i7 = i16;
                i8 = 1;
                iCheckForRingerModeChange = checkForRingerModeChange(i5, i2, i6, volumeStreamState2.mIsMuted, str, i18);
                boolean z4 = (iCheckForRingerModeChange & 1) == 0;
                if ((iCheckForRingerModeChange & 128) == 0) {
                    i9 = i18 | 128;
                } else {
                    i9 = i18;
                }
                if ((iCheckForRingerModeChange & 2048) == 0) {
                    i17 = i9 | 2048;
                    z = z4;
                } else {
                    z = z4;
                    i17 = i9;
                }
            } else {
                z = true;
                i7 = i16;
                i8 = 1;
            }
            if (!volumeAdjustmentAllowedByDnd(i15, i17)) {
                z = false;
            }
            int index2 = this.mStreamStates[i].getIndex(deviceForStream);
            if (!z && i2 != 0) {
                this.mAudioHandler.removeMessages(24);
                if (zIsMuteAdjust) {
                    boolean z5 = i2 == 101 ? volumeStreamState2.mIsMuted ^ i8 : i2 == -100 ? i8 : false;
                    i11 = 3;
                    if (i15 == 3) {
                        setSystemAudioMute(z5);
                    }
                    for (int i19 = 0; i19 < this.mStreamStates.length; i19++) {
                        if (i15 == mStreamVolumeAlias[i19] && (!readCameraSoundForced() || this.mStreamStates[i19].getStreamType() != 7)) {
                            this.mStreamStates[i19].mute(z5);
                        }
                    }
                } else {
                    i11 = 3;
                    if (i2 == i8 && !checkSafeMediaVolume(i15, i5 + i6, deviceForStream)) {
                        Log.e(TAG, "adjustStreamVolume() safe volume index = " + index2);
                        this.mVolumeController.postDisplaySafeVolumeWarning(i17);
                    } else {
                        if (volumeStreamState2.adjustIndex(i2 * i6, deviceForStream, str2) || volumeStreamState2.mIsMuted) {
                            if (volumeStreamState2.mIsMuted) {
                                if (i2 == i8) {
                                    z2 = false;
                                    volumeStreamState2.mute(false);
                                } else {
                                    z2 = false;
                                    if (i2 == -1 && this.mIsSingleVolume) {
                                        i12 = i8;
                                        i14 = i7;
                                        i10 = deviceForStream;
                                        volumeStreamState = volumeStreamState2;
                                        i13 = i15;
                                        sendMsg(this.mAudioHandler, 24, 2, i15, i17, null, UNMUTE_STREAM_DELAY);
                                    }
                                }
                                i12 = i8;
                                i10 = deviceForStream;
                                volumeStreamState = volumeStreamState2;
                                i13 = i15;
                                i14 = i7;
                            } else {
                                i12 = i8;
                                i10 = deviceForStream;
                                volumeStreamState = volumeStreamState2;
                                i13 = i15;
                                i14 = i7;
                                z2 = false;
                            }
                            sendMsg(this.mAudioHandler, 0, 2, i10, 0, volumeStreamState, 0);
                            z3 = i12;
                        }
                        int index3 = this.mStreamStates[i].getIndex(i10);
                        if (i13 == i11 && i14 != 0 && (i17 & 64) == 0) {
                            synchronized (this.mA2dpAvrcpLock) {
                                if (this.mA2dp != null && this.mAvrcpAbsVolSupported) {
                                    this.mA2dp.setAvrcpAbsoluteVolume(index3 / 10);
                                }
                            }
                        }
                        if ((134217728 & i10) != 0) {
                            setHearingAidVolume(index3, i);
                        }
                        if (i13 == i11) {
                            setSystemAudioVolume(index2, index3, getStreamMaxVolume(i), i17);
                        }
                        if (this.mHdmiManager != null) {
                            synchronized (this.mHdmiManager) {
                                if (this.mHdmiCecSink && i13 == i11 && index2 != index3) {
                                    synchronized (this.mHdmiPlaybackClient) {
                                        int i20 = i2 == -1 ? 25 : 24;
                                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                                        try {
                                            this.mHdmiPlaybackClient.sendKeyEvent(i20, z3);
                                            this.mHdmiPlaybackClient.sendKeyEvent(i20, z2);
                                        } finally {
                                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                z3 = i8;
                i10 = deviceForStream;
                i13 = i15;
                i14 = i7;
                z2 = false;
                int index32 = this.mStreamStates[i].getIndex(i10);
                if (i13 == i11) {
                    synchronized (this.mA2dpAvrcpLock) {
                    }
                }
                if ((134217728 & i10) != 0) {
                }
                if (i13 == i11) {
                }
                if (this.mHdmiManager != null) {
                }
            } else {
                i10 = deviceForStream;
            }
            sendVolumeUpdate(i, index2, this.mStreamStates[i].getIndex(i10), i17);
        }
        iRescaleIndex = rescaleIndex(10, i, i15);
        i5 = index;
        i6 = iRescaleIndex;
        if ((i17 & 2) == 0) {
            if (getRingerModeInternal() == 1) {
            }
            int i182 = i17;
            i7 = i16;
            i8 = 1;
            iCheckForRingerModeChange = checkForRingerModeChange(i5, i2, i6, volumeStreamState2.mIsMuted, str, i182);
            if ((iCheckForRingerModeChange & 1) == 0) {
            }
            if ((iCheckForRingerModeChange & 128) == 0) {
            }
            if ((iCheckForRingerModeChange & 2048) == 0) {
            }
        }
        if (!volumeAdjustmentAllowedByDnd(i15, i17)) {
        }
        int index22 = this.mStreamStates[i].getIndex(deviceForStream);
        if (!z) {
            i10 = deviceForStream;
        }
        sendVolumeUpdate(i, index22, this.mStreamStates[i].getIndex(i10), i17);
    }

    private void onUnmuteStream(int i, int i2) {
        this.mStreamStates[i].mute(false);
        int index = this.mStreamStates[i].getIndex(getDeviceForStream(i));
        sendVolumeUpdate(i, index, index, i2);
    }

    private void setSystemAudioVolume(int i, int i2, int i3, int i4) {
        if (this.mHdmiManager == null || this.mHdmiTvClient == null || i == i2 || (i4 & 256) != 0) {
            return;
        }
        synchronized (this.mHdmiManager) {
            if (this.mHdmiSystemAudioSupported) {
                synchronized (this.mHdmiTvClient) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        this.mHdmiTvClient.setSystemAudioVolume(i, i2, i3);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            }
        }
    }

    class StreamVolumeCommand {
        public final int mDevice;
        public final int mFlags;
        public final int mIndex;
        public final int mStreamType;

        StreamVolumeCommand(int i, int i2, int i3, int i4) {
            this.mStreamType = i;
            this.mIndex = i2;
            this.mFlags = i3;
            this.mDevice = i4;
        }

        public String toString() {
            return "{streamType=" + this.mStreamType + ",index=" + this.mIndex + ",flags=" + this.mFlags + ",device=" + this.mDevice + '}';
        }
    }

    private int getNewRingerMode(int i, int i2, int i3) {
        if (this.mIsSingleVolume) {
            return getRingerModeExternal();
        }
        if ((i3 & 2) != 0 || i == getUiSoundsStreamType()) {
            if (i2 != 0) {
                return 2;
            }
            if (this.mHasVibrator) {
                return 1;
            }
            return this.mVolumePolicy.volumeDownToEnterSilent ? 0 : 2;
        }
        return getRingerModeExternal();
    }

    private boolean isAndroidNPlus(String str) {
        try {
            if (this.mContext.getPackageManager().getApplicationInfoAsUser(str, 0, UserHandle.getUserId(Binder.getCallingUid())).targetSdkVersion >= 24) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    private boolean wouldToggleZenMode(int i) {
        if (getRingerModeExternal() != 0 || i == 0) {
            return getRingerModeExternal() != 0 && i == 0;
        }
        return true;
    }

    private void onSetStreamVolume(int i, int i2, int i3, int i4, String str) {
        int i5 = mStreamVolumeAlias[i];
        setStreamVolumeInt(i5, i2, i4, false, str);
        if ((i3 & 2) != 0 || i5 == getUiSoundsStreamType()) {
            setRingerMode(getNewRingerMode(i5, i2, i3), "AudioService.onSetStreamVolume", false);
        }
        this.mStreamStates[i5].mute(i2 == 0);
    }

    public void setStreamVolume(int i, int i2, int i3, String str) {
        if (i == 10 && !canChangeAccessibilityVolume()) {
            Log.w(TAG, "Trying to call setStreamVolume() for a11y without CHANGE_ACCESSIBILITY_VOLUME  callingPackage=" + str);
            return;
        }
        if (i != 0 || i2 != 0 || this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") == 0) {
            this.mVolumeLogger.log(new AudioServiceEvents.VolumeEvent(2, i, i2, i3, str));
            setStreamVolume(i, i2, i3, str, str, Binder.getCallingUid());
        } else {
            Log.w(TAG, "Trying to call setStreamVolume() for STREAM_VOICE_CALL and index 0 without MODIFY_PHONE_STATE  callingPackage=" + str);
        }
    }

    private boolean canChangeAccessibilityVolume() {
        synchronized (this.mAccessibilityServiceUidsLock) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_ACCESSIBILITY_VOLUME") == 0) {
                return true;
            }
            if (this.mAccessibilityServiceUids != null) {
                int callingUid = Binder.getCallingUid();
                for (int i = 0; i < this.mAccessibilityServiceUids.length; i++) {
                    if (this.mAccessibilityServiceUids[i] == callingUid) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void setStreamVolume(int i, int i2, int i3, String str, String str2, int i4) {
        int index;
        int iRescaleIndex;
        int i5;
        if (DEBUG_VOL) {
            Log.d(TAG, "setStreamVolume(stream=" + i + ", index=" + i2 + ", calling=" + str + ")");
        }
        if (this.mUseFixedVolume) {
            return;
        }
        ensureValidStreamType(i);
        int i6 = mStreamVolumeAlias[i];
        VolumeStreamState volumeStreamState = this.mStreamStates[i6];
        int deviceForStream = getDeviceForStream(i);
        int i7 = deviceForStream & 896;
        if (i7 == 0 && (i3 & 64) != 0) {
            return;
        }
        if (i4 == 1000) {
            i4 = UserHandle.getUid(getCurrentUserId(), UserHandle.getAppId(i4));
        }
        if (this.mAppOps.noteOp(STREAM_VOLUME_OPS[i6], i4, str) != 0) {
            return;
        }
        if (isAndroidNPlus(str) && wouldToggleZenMode(getNewRingerMode(i6, i2, i3)) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(str)) {
            throw new SecurityException("Not allowed to change Do Not Disturb state");
        }
        if (!volumeAdjustmentAllowedByDnd(i6, i3)) {
            return;
        }
        synchronized (this.mSafeMediaVolumeState) {
            this.mPendingVolumeCommand = null;
            index = volumeStreamState.getIndex(deviceForStream);
            iRescaleIndex = rescaleIndex(i2 * 10, i, i6);
            if (i6 == 3 && i7 != 0 && (i3 & 64) == 0) {
                synchronized (this.mA2dpAvrcpLock) {
                    if (this.mA2dp != null && this.mAvrcpAbsVolSupported) {
                        this.mA2dp.setAvrcpAbsoluteVolume(iRescaleIndex / 10);
                    }
                }
            }
            if ((134217728 & deviceForStream) != 0) {
                setHearingAidVolume(iRescaleIndex, i);
            }
            if (i6 == 3) {
                setSystemAudioVolume(index, iRescaleIndex, getStreamMaxVolume(i), i3);
            }
            i5 = i3 & (-33);
            if (i6 == 3 && (this.mFixedVolumeDevices & deviceForStream) != 0) {
                i5 |= 32;
                if (iRescaleIndex != 0) {
                    iRescaleIndex = (this.mSafeMediaVolumeState.intValue() != 3 || (67108876 & deviceForStream) == 0) ? volumeStreamState.getMaxIndex() : safeMediaVolumeIndex(deviceForStream);
                }
            }
            if (!checkSafeMediaVolume(i6, iRescaleIndex, deviceForStream)) {
                this.mVolumeController.postDisplaySafeVolumeWarning(i5);
                this.mPendingVolumeCommand = new StreamVolumeCommand(i, iRescaleIndex, i5, deviceForStream);
            } else {
                onSetStreamVolume(i, iRescaleIndex, i5, deviceForStream, str2);
                iRescaleIndex = this.mStreamStates[i].getIndex(deviceForStream);
            }
        }
        sendVolumeUpdate(i, index, iRescaleIndex, i5);
    }

    private boolean volumeAdjustmentAllowedByDnd(int i, int i2) {
        switch (this.mNm.getZenMode()) {
            case 1:
            case 2:
            case 3:
                if (!isStreamMutedByRingerOrZenMode(i) || i == getUiSoundsStreamType() || (i2 & 2) != 0) {
                }
                break;
        }
        return true;
    }

    public void forceVolumeControlStream(int i, IBinder iBinder) {
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("forceVolumeControlStream(%d)", Integer.valueOf(i)));
        }
        synchronized (this.mForceControlStreamLock) {
            if (this.mVolumeControlStream != -1 && i != -1) {
                this.mUserSelectedVolumeControlStream = true;
            }
            this.mVolumeControlStream = i;
            if (this.mVolumeControlStream == -1) {
                if (this.mForceControlStreamClient != null) {
                    this.mForceControlStreamClient.release();
                    this.mForceControlStreamClient = null;
                }
                this.mUserSelectedVolumeControlStream = false;
            } else if (this.mForceControlStreamClient == null) {
                this.mForceControlStreamClient = new ForceControlStreamClient(iBinder);
            } else if (this.mForceControlStreamClient.getBinder() == iBinder) {
                Log.d(TAG, "forceVolumeControlStream cb:" + iBinder + " is already linked.");
            } else {
                this.mForceControlStreamClient.release();
                this.mForceControlStreamClient = new ForceControlStreamClient(iBinder);
            }
        }
    }

    private class ForceControlStreamClient implements IBinder.DeathRecipient {
        private IBinder mCb;

        ForceControlStreamClient(IBinder iBinder) {
            if (iBinder != null) {
                try {
                    iBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Log.w(AudioService.TAG, "ForceControlStreamClient() could not link to " + iBinder + " binder death");
                    iBinder = null;
                }
            }
            this.mCb = iBinder;
        }

        @Override
        public void binderDied() {
            synchronized (AudioService.this.mForceControlStreamLock) {
                Log.w(AudioService.TAG, "SCO client died");
                if (AudioService.this.mForceControlStreamClient == this) {
                    AudioService.this.mForceControlStreamClient = null;
                    AudioService.this.mVolumeControlStream = -1;
                    AudioService.this.mUserSelectedVolumeControlStream = false;
                } else {
                    Log.w(AudioService.TAG, "unregistered control stream client died");
                }
            }
        }

        public void release() {
            if (this.mCb != null) {
                this.mCb.unlinkToDeath(this, 0);
                this.mCb = null;
            }
        }

        public IBinder getBinder() {
            return this.mCb;
        }
    }

    private void sendBroadcastToAll(Intent intent) {
        intent.addFlags(67108864);
        intent.addFlags(268435456);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void sendStickyBroadcastToAll(Intent intent) {
        intent.addFlags(268435456);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private int getCurrentUserId() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int i = ActivityManager.getService().getCurrentUser().id;
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return i;
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    protected void sendVolumeUpdate(int i, int i2, int i3, int i4) {
        int i5 = mStreamVolumeAlias[i];
        if (i5 == 3) {
            i4 = updateFlagsForSystemAudio(i4);
        }
        this.mVolumeController.postVolumeChanged(i5, i4);
    }

    private int updateFlagsForSystemAudio(int i) {
        if (this.mHdmiTvClient != null) {
            synchronized (this.mHdmiTvClient) {
                if (this.mHdmiSystemAudioSupported && (i & 256) == 0) {
                    i &= -2;
                }
            }
        }
        return i;
    }

    private void sendMasterMuteUpdate(boolean z, int i) {
        this.mVolumeController.postMasterMuteChanged(updateFlagsForSystemAudio(i));
        broadcastMasterMuteStatus(z);
    }

    private void broadcastMasterMuteStatus(boolean z) {
        Intent intent = new Intent("android.media.MASTER_MUTE_CHANGED_ACTION");
        intent.putExtra("android.media.EXTRA_MASTER_VOLUME_MUTED", z);
        intent.addFlags(603979776);
        sendStickyBroadcastToAll(intent);
    }

    private void setStreamVolumeInt(int i, int i2, int i3, boolean z, String str) {
        VolumeStreamState volumeStreamState = this.mStreamStates[i];
        if (volumeStreamState.setIndex(i2, i3, str) || z) {
            sendMsg(this.mAudioHandler, 0, 2, i3, 0, volumeStreamState, 0);
        }
    }

    private void setSystemAudioMute(boolean z) {
        if (this.mHdmiManager == null || this.mHdmiTvClient == null) {
            return;
        }
        synchronized (this.mHdmiManager) {
            if (this.mHdmiSystemAudioSupported) {
                synchronized (this.mHdmiTvClient) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        this.mHdmiTvClient.setSystemAudioMute(z);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            }
        }
    }

    public boolean isStreamMute(int i) {
        boolean z;
        if (i == Integer.MIN_VALUE) {
            i = getActiveStreamType(i);
        }
        synchronized (VolumeStreamState.class) {
            ensureValidStreamType(i);
            z = this.mStreamStates[i].mIsMuted;
        }
        return z;
    }

    private class RmtSbmxFullVolDeathHandler implements IBinder.DeathRecipient {
        private IBinder mICallback;

        RmtSbmxFullVolDeathHandler(IBinder iBinder) {
            this.mICallback = iBinder;
            try {
                iBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Log.e(AudioService.TAG, "can't link to death", e);
            }
        }

        boolean isHandlerFor(IBinder iBinder) {
            return this.mICallback.equals(iBinder);
        }

        void forget() {
            try {
                this.mICallback.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Log.e(AudioService.TAG, "error unlinking to death", e);
            }
        }

        @Override
        public void binderDied() {
            Log.w(AudioService.TAG, "Recorder with remote submix at full volume died " + this.mICallback);
            AudioService.this.forceRemoteSubmixFullVolume(false, this.mICallback);
        }
    }

    private boolean discardRmtSbmxFullVolDeathHandlerFor(IBinder iBinder) {
        for (RmtSbmxFullVolDeathHandler rmtSbmxFullVolDeathHandler : this.mRmtSbmxFullVolDeathHandlers) {
            if (rmtSbmxFullVolDeathHandler.isHandlerFor(iBinder)) {
                rmtSbmxFullVolDeathHandler.forget();
                this.mRmtSbmxFullVolDeathHandlers.remove(rmtSbmxFullVolDeathHandler);
                return true;
            }
        }
        return false;
    }

    private boolean hasRmtSbmxFullVolDeathHandlerFor(IBinder iBinder) {
        Iterator<RmtSbmxFullVolDeathHandler> it = this.mRmtSbmxFullVolDeathHandlers.iterator();
        while (it.hasNext()) {
            if (it.next().isHandlerFor(iBinder)) {
                return true;
            }
        }
        return false;
    }

    public void forceRemoteSubmixFullVolume(boolean z, IBinder iBinder) {
        if (iBinder == null) {
            return;
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.CAPTURE_AUDIO_OUTPUT") != 0) {
            Log.w(TAG, "Trying to call forceRemoteSubmixFullVolume() without CAPTURE_AUDIO_OUTPUT");
            return;
        }
        synchronized (this.mRmtSbmxFullVolDeathHandlers) {
            boolean z2 = false;
            try {
                if (z) {
                    if (!hasRmtSbmxFullVolDeathHandlerFor(iBinder)) {
                        this.mRmtSbmxFullVolDeathHandlers.add(new RmtSbmxFullVolDeathHandler(iBinder));
                        if (this.mRmtSbmxFullVolRefCount == 0) {
                            this.mFullVolumeDevices |= 32768;
                            this.mFixedVolumeDevices |= 32768;
                            z2 = true;
                        }
                        this.mRmtSbmxFullVolRefCount++;
                    }
                } else if (discardRmtSbmxFullVolDeathHandlerFor(iBinder) && this.mRmtSbmxFullVolRefCount > 0) {
                    this.mRmtSbmxFullVolRefCount--;
                    if (this.mRmtSbmxFullVolRefCount == 0) {
                        this.mFullVolumeDevices &= -32769;
                        this.mFixedVolumeDevices &= -32769;
                        z2 = true;
                    }
                }
                if (z2) {
                    checkAllFixedVolumeDevices(3);
                    this.mStreamStates[3].applyAllVolumes();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void setMasterMuteInternal(boolean z, int i, String str, int i2, int i3) {
        if (i2 == 1000) {
            i2 = UserHandle.getUid(i3, UserHandle.getAppId(i2));
        }
        if (!z && this.mAppOps.noteOp(33, i2, str) != 0) {
            return;
        }
        if (i3 != UserHandle.getCallingUserId() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            return;
        }
        setMasterMuteInternalNoCallerCheck(z, i, i3);
    }

    private void setMasterMuteInternalNoCallerCheck(boolean z, int i, int i2) {
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Master mute %s, %d, user=%d", Boolean.valueOf(z), Integer.valueOf(i), Integer.valueOf(i2)));
        }
        if ((isPlatformAutomotive() || !this.mUseFixedVolume) && getCurrentUserId() == i2 && z != AudioSystem.getMasterMute()) {
            setSystemAudioMute(z);
            AudioSystem.setMasterMute(z);
            sendMasterMuteUpdate(z, i);
            Intent intent = new Intent("android.media.MASTER_MUTE_CHANGED_ACTION");
            intent.putExtra("android.media.EXTRA_MASTER_VOLUME_MUTED", z);
            sendBroadcastToAll(intent);
        }
    }

    public boolean isMasterMute() {
        return AudioSystem.getMasterMute();
    }

    public void setMasterMute(boolean z, int i, String str, int i2) {
        setMasterMuteInternal(z, i, str, Binder.getCallingUid(), i2);
    }

    public int getStreamVolume(int i) {
        int i2;
        ensureValidStreamType(i);
        int deviceForStream = getDeviceForStream(i);
        synchronized (VolumeStreamState.class) {
            int index = this.mStreamStates[i].getIndex(deviceForStream);
            if (this.mStreamStates[i].mIsMuted) {
                index = 0;
            }
            if (index != 0 && mStreamVolumeAlias[i] == 3 && (deviceForStream & this.mFixedVolumeDevices) != 0) {
                index = this.mStreamStates[i].getMaxIndex();
            }
            i2 = (index + 5) / 10;
        }
        return i2;
    }

    public int getStreamMaxVolume(int i) {
        ensureValidStreamType(i);
        return (this.mStreamStates[i].getMaxIndex() + 5) / 10;
    }

    public int getStreamMinVolume(int i) {
        ensureValidStreamType(i);
        return (this.mStreamStates[i].getMinIndex() + 5) / 10;
    }

    public int getLastAudibleStreamVolume(int i) {
        ensureValidStreamType(i);
        return (this.mStreamStates[i].getIndex(getDeviceForStream(i)) + 5) / 10;
    }

    public int getUiSoundsStreamType() {
        return mStreamVolumeAlias[1];
    }

    public void setMicrophoneMute(boolean z, String str, int i) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000) {
            callingUid = UserHandle.getUid(i, UserHandle.getAppId(callingUid));
        }
        if ((!z && this.mAppOps.noteOp(44, callingUid, str) != 0) || !checkAudioSettingsPermission("setMicrophoneMute()")) {
            return;
        }
        if (i != UserHandle.getCallingUserId() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            return;
        }
        setMicrophoneMuteNoCallerCheck(z, i);
    }

    private void setMicrophoneMuteNoCallerCheck(boolean z, int i) {
        if (DEBUG_VOL) {
            Log.d(TAG, String.format("Mic mute %s, user=%d", Boolean.valueOf(z), Integer.valueOf(i)));
        }
        if (getCurrentUserId() == i) {
            boolean zIsMicrophoneMuted = AudioSystem.isMicrophoneMuted();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            AudioSystem.muteMicrophone(z);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (z != zIsMicrophoneMuted) {
                this.mContext.sendBroadcast(new Intent("android.media.action.MICROPHONE_MUTE_CHANGED").setFlags(1073741824));
            }
        }
    }

    public int getRingerModeExternal() {
        int i;
        synchronized (this.mSettingsLock) {
            i = this.mRingerModeExternal;
        }
        return i;
    }

    public int getRingerModeInternal() {
        int i;
        synchronized (this.mSettingsLock) {
            i = this.mRingerMode;
        }
        return i;
    }

    private void ensureValidRingerMode(int i) {
        if (!isValidRingerMode(i)) {
            throw new IllegalArgumentException("Bad ringer mode " + i);
        }
    }

    public boolean isValidRingerMode(int i) {
        return i >= 0 && i <= 2;
    }

    public void setRingerModeExternal(int i, String str) {
        if (isAndroidNPlus(str) && wouldToggleZenMode(i) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(str)) {
            throw new SecurityException("Not allowed to change Do Not Disturb state");
        }
        setRingerMode(i, str, true);
    }

    public void setRingerModeInternal(int i, String str) {
        enforceVolumeController("setRingerModeInternal");
        setRingerMode(i, str, false);
    }

    public void silenceRingerModeInternal(String str) {
        int intForUser;
        VibrationEffect vibrationEffect;
        int i;
        if (this.mContext.getResources().getBoolean(R.^attr-private.position)) {
            intForUser = Settings.Secure.getIntForUser(this.mContentResolver, "volume_hush_gesture", 0, -2);
        } else {
            intForUser = 0;
        }
        int i2 = 1;
        switch (intForUser) {
            case 1:
                vibrationEffect = VibrationEffect.get(5);
                i = R.string.mobile_provisioning_apn;
                break;
            case 2:
                vibrationEffect = VibrationEffect.get(1);
                i = 17041032;
                i2 = 0;
                break;
            default:
                vibrationEffect = null;
                i2 = 0;
                i = 0;
                break;
        }
        maybeVibrate(vibrationEffect);
        setRingerModeInternal(i2, str);
        Toast.makeText(this.mContext, i, 0).show();
    }

    private boolean maybeVibrate(VibrationEffect vibrationEffect) {
        if (!this.mHasVibrator) {
            return false;
        }
        if ((Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) == 0) || vibrationEffect == null) {
            return false;
        }
        this.mVibrator.vibrate(Binder.getCallingUid(), this.mContext.getOpPackageName(), vibrationEffect, VIBRATION_ATTRIBUTES);
        return true;
    }

    private void setRingerMode(int i, String str, boolean z) {
        if (this.mUseFixedVolume || this.mIsSingleVolume) {
            return;
        }
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("Bad caller: " + str);
        }
        ensureValidRingerMode(i);
        if (i == 1 && !this.mHasVibrator) {
            i = 0;
        }
        int iOnSetRingerModeInternal = i;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mSettingsLock) {
                int ringerModeInternal = getRingerModeInternal();
                int ringerModeExternal = getRingerModeExternal();
                if (z) {
                    setRingerModeExt(iOnSetRingerModeInternal);
                    if (this.mRingerModeDelegate != null) {
                        iOnSetRingerModeInternal = this.mRingerModeDelegate.onSetRingerModeExternal(ringerModeExternal, iOnSetRingerModeInternal, str, ringerModeInternal, this.mVolumePolicy);
                    }
                    if (iOnSetRingerModeInternal != ringerModeInternal) {
                        setRingerModeInt(iOnSetRingerModeInternal, true);
                    }
                } else {
                    if (iOnSetRingerModeInternal != ringerModeInternal) {
                        setRingerModeInt(iOnSetRingerModeInternal, true);
                    }
                    if (this.mRingerModeDelegate != null) {
                        iOnSetRingerModeInternal = this.mRingerModeDelegate.onSetRingerModeInternal(ringerModeInternal, iOnSetRingerModeInternal, str, ringerModeExternal, this.mVolumePolicy);
                    }
                    setRingerModeExt(iOnSetRingerModeInternal);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void setRingerModeExt(int i) {
        synchronized (this.mSettingsLock) {
            if (i == this.mRingerModeExternal) {
                return;
            }
            this.mRingerModeExternal = i;
            broadcastRingerMode("android.media.RINGER_MODE_CHANGED", i);
        }
    }

    @GuardedBy("mSettingsLock")
    private void muteRingerModeStreams() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        if (this.mNm == null) {
            this.mNm = (NotificationManager) this.mContext.getSystemService("notification");
        }
        int i = this.mRingerMode;
        boolean z = i == 1 || i == 0;
        boolean z2 = i == 1 && isBluetoothScoOn();
        sendMsg(this.mAudioHandler, 8, 2, 7, z2 ? 3 : 0, "muteRingerModeStreams() from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid(), 0);
        int i2 = numStreamTypes - 1;
        while (i2 >= 0) {
            boolean zIsStreamMutedByRingerOrZenMode = isStreamMutedByRingerOrZenMode(i2);
            boolean z3 = shouldZenMuteStream(i2) || (z && isStreamAffectedByRingerMode(i2) && (!z2 || i2 != 2));
            if (zIsStreamMutedByRingerOrZenMode != z3) {
                if (!z3) {
                    if (mStreamVolumeAlias[i2] == 2) {
                        synchronized (VolumeStreamState.class) {
                            VolumeStreamState volumeStreamState = this.mStreamStates[i2];
                            for (int i3 = 0; i3 < volumeStreamState.mIndexMap.size(); i3++) {
                                int iKeyAt = volumeStreamState.mIndexMap.keyAt(i3);
                                if (volumeStreamState.mIndexMap.valueAt(i3) == 0) {
                                    volumeStreamState.setIndex(10, iKeyAt, TAG);
                                }
                            }
                            sendMsg(this.mAudioHandler, 1, 2, getDeviceForStream(i2), 0, this.mStreamStates[i2], 500);
                        }
                    }
                    this.mStreamStates[i2].mute(false);
                    this.mRingerAndZenModeMutedStreams &= ~(1 << i2);
                } else {
                    this.mStreamStates[i2].mute(true);
                    this.mRingerAndZenModeMutedStreams |= 1 << i2;
                }
            }
            i2--;
        }
    }

    private boolean isAlarm(int i) {
        return i == 4;
    }

    private boolean isNotificationOrRinger(int i) {
        return i == 5 || i == 2;
    }

    private boolean isMedia(int i) {
        return i == 3;
    }

    private boolean isSystem(int i) {
        return i == 1;
    }

    private void setRingerModeInt(int i, boolean z) {
        boolean z2;
        synchronized (this.mSettingsLock) {
            z2 = this.mRingerMode != i;
            this.mRingerMode = i;
            muteRingerModeStreams();
        }
        if (z) {
            sendMsg(this.mAudioHandler, 3, 0, 0, 0, null, 500);
        }
        if (z2) {
            broadcastRingerMode("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION", i);
        }
    }

    public boolean shouldVibrate(int i) {
        if (!this.mHasVibrator) {
            return false;
        }
        switch (getVibrateSetting(i)) {
            case 1:
                if (getRingerModeExternal() == 0) {
                    break;
                }
                break;
            case 2:
                if (getRingerModeExternal() != 1) {
                    break;
                }
                break;
        }
        return false;
    }

    public int getVibrateSetting(int i) {
        if (this.mHasVibrator) {
            return (this.mVibrateSetting >> (i * 2)) & 3;
        }
        return 0;
    }

    public void setVibrateSetting(int i, int i2) {
        if (this.mHasVibrator) {
            this.mVibrateSetting = AudioSystem.getValueForVibrateSetting(this.mVibrateSetting, i, i2);
            broadcastVibrateSetting(i);
        }
    }

    private class SetModeDeathHandler implements IBinder.DeathRecipient {
        private IBinder mCb;
        private int mMode = 0;
        private int mPid;

        SetModeDeathHandler(IBinder iBinder, int i) {
            this.mCb = iBinder;
            this.mPid = i;
        }

        @Override
        public void binderDied() {
            int modeInt;
            int pid;
            synchronized (AudioService.this.mSetModeDeathHandlers) {
                Log.w(AudioService.TAG, "setMode() client died");
                modeInt = 0;
                if (!AudioService.this.mSetModeDeathHandlers.isEmpty()) {
                    pid = ((SetModeDeathHandler) AudioService.this.mSetModeDeathHandlers.get(0)).getPid();
                } else {
                    pid = 0;
                }
                if (AudioService.this.mSetModeDeathHandlers.indexOf(this) >= 0) {
                    modeInt = AudioService.this.setModeInt(0, this.mCb, this.mPid, AudioService.TAG);
                } else {
                    Log.w(AudioService.TAG, "unregistered setMode() client died");
                }
            }
            if (modeInt != pid && modeInt != 0) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                AudioService.this.disconnectBluetoothSco(modeInt);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int getPid() {
            return this.mPid;
        }

        public void setMode(int i) {
            this.mMode = i;
        }

        public int getMode() {
            return this.mMode;
        }

        public IBinder getBinder() {
            return this.mCb;
        }
    }

    public void setMode(int i, IBinder iBinder, String str) {
        int pid;
        int modeInt;
        if (DEBUG_MODE) {
            Log.v(TAG, "setMode(mode=" + i + ", callingPackage=" + str + ")");
        }
        if (!checkAudioSettingsPermission("setMode()")) {
            return;
        }
        if (i == 2 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Log.w(TAG, "MODIFY_PHONE_STATE Permission Denial: setMode(MODE_IN_CALL) from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        if (i < -1 || i >= 4) {
            return;
        }
        synchronized (this.mSetModeDeathHandlers) {
            pid = this.mSetModeDeathHandlers.isEmpty() ? 0 : this.mSetModeDeathHandlers.get(0).getPid();
            if (i == -1) {
                i = this.mMode;
            }
            modeInt = setModeInt(i, iBinder, Binder.getCallingPid(), str);
        }
        if (modeInt != pid && modeInt != 0) {
            disconnectBluetoothSco(modeInt);
        }
    }

    private int setModeInt(int i, IBinder iBinder, int i2, String str) {
        int i3;
        int i4;
        int phoneState;
        int pid;
        if (DEBUG_MODE) {
            Log.v(TAG, "setModeInt(mode=" + i + ", pid=" + i2 + ", caller=" + str + ")");
        }
        if (iBinder == null) {
            Log.e(TAG, "setModeInt() called with null binder");
            return 0;
        }
        SetModeDeathHandler setModeDeathHandler = null;
        Iterator<SetModeDeathHandler> it = this.mSetModeDeathHandlers.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            SetModeDeathHandler next = it.next();
            if (next.getPid() == i2) {
                it.remove();
                next.getBinder().unlinkToDeath(next, 0);
                setModeDeathHandler = next;
                break;
            }
        }
        while (true) {
            if (i == 0) {
                if (!this.mSetModeDeathHandlers.isEmpty()) {
                    SetModeDeathHandler setModeDeathHandler2 = this.mSetModeDeathHandlers.get(0);
                    IBinder binder = setModeDeathHandler2.getBinder();
                    int mode = setModeDeathHandler2.getMode();
                    if (DEBUG_MODE) {
                        Log.w(TAG, " using mode=" + mode + " instead due to death hdlr at pid=" + setModeDeathHandler2.mPid);
                    }
                    i3 = mode;
                    setModeDeathHandler = setModeDeathHandler2;
                    iBinder = binder;
                }
                if (i3 == this.mMode) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    phoneState = AudioSystem.setPhoneState(i3);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    if (phoneState == 0) {
                        if (DEBUG_MODE) {
                            Log.v(TAG, " mode successfully set to " + i3);
                        }
                        this.mMode = i3;
                    } else {
                        if (setModeDeathHandler != null) {
                            this.mSetModeDeathHandlers.remove(setModeDeathHandler);
                            iBinder.unlinkToDeath(setModeDeathHandler, 0);
                        }
                        if (DEBUG_MODE) {
                            Log.w(TAG, " mode set to MODE_NORMAL after phoneState pb");
                        }
                        i = 0;
                    }
                    i4 = i;
                } else {
                    i4 = i;
                    phoneState = 0;
                }
                if (phoneState == 0 || this.mSetModeDeathHandlers.isEmpty()) {
                    break;
                }
                i = i4;
            } else {
                if (setModeDeathHandler == null) {
                    setModeDeathHandler = new SetModeDeathHandler(iBinder, i2);
                }
                try {
                    iBinder.linkToDeath(setModeDeathHandler, 0);
                } catch (RemoteException e) {
                    Log.w(TAG, "setMode() could not link to " + iBinder + " binder death");
                }
                this.mSetModeDeathHandlers.add(0, setModeDeathHandler);
                setModeDeathHandler.setMode(i);
            }
            i3 = i;
            if (i3 == this.mMode) {
            }
            if (phoneState == 0) {
                break;
            }
            break;
            break;
        }
        if (phoneState != 0) {
            return 0;
        }
        if (i3 != 0) {
            if (!this.mSetModeDeathHandlers.isEmpty()) {
                pid = this.mSetModeDeathHandlers.get(0).getPid();
            } else {
                Log.e(TAG, "setMode() different from MODE_NORMAL with empty mode client stack");
                pid = 0;
            }
        } else {
            pid = 0;
        }
        this.mModeLogger.log(new AudioServiceEvents.PhoneStateEvent(str, i2, i4, pid, i3));
        int activeStreamType = getActiveStreamType(Integer.MIN_VALUE);
        int deviceForStream = getDeviceForStream(activeStreamType);
        setStreamVolumeInt(mStreamVolumeAlias[activeStreamType], this.mStreamStates[mStreamVolumeAlias[activeStreamType]].getIndex(deviceForStream), deviceForStream, true, str);
        updateStreamVolumeAlias(true, str);
        return pid;
    }

    public int getMode() {
        return this.mMode;
    }

    class LoadSoundEffectReply {
        public int mStatus = 1;

        LoadSoundEffectReply() {
        }
    }

    private void loadTouchSoundAssetDefaults() {
        SOUND_EFFECT_FILES.add("Effect_Tick.ogg");
        for (int i = 0; i < 10; i++) {
            this.SOUND_EFFECT_FILES_MAP[i][0] = 0;
            this.SOUND_EFFECT_FILES_MAP[i][1] = -1;
        }
    }

    private void loadTouchSoundAssets() throws Throwable {
        Throwable th;
        XmlResourceParser xml;
        XmlPullParserException e;
        IOException e2;
        Resources.NotFoundException e3;
        boolean z;
        if (SOUND_EFFECT_FILES.isEmpty()) {
            loadTouchSoundAssetDefaults();
            XmlResourceParser xmlResourceParser = null;
            try {
                try {
                    xml = this.mContext.getResources().getXml(R.xml.audio_assets);
                    try {
                        XmlUtils.beginDocument(xml, TAG_AUDIO_ASSETS);
                        if (ASSET_FILE_VERSION.equals(xml.getAttributeValue(null, ATTR_VERSION))) {
                            while (true) {
                                XmlUtils.nextElement(xml);
                                String name = xml.getName();
                                if (name != null) {
                                    if (name.equals(TAG_GROUP) && GROUP_TOUCH_SOUNDS.equals(xml.getAttributeValue(null, "name"))) {
                                        z = true;
                                        break;
                                    }
                                } else {
                                    z = false;
                                    break;
                                }
                            }
                            while (z) {
                                XmlUtils.nextElement(xml);
                                String name2 = xml.getName();
                                if (name2 == null || !name2.equals(TAG_ASSET)) {
                                    break;
                                }
                                String attributeValue = xml.getAttributeValue(null, ATTR_ASSET_ID);
                                String attributeValue2 = xml.getAttributeValue(null, ATTR_ASSET_FILE);
                                try {
                                    int i = AudioManager.class.getField(attributeValue).getInt(null);
                                    int iIndexOf = SOUND_EFFECT_FILES.indexOf(attributeValue2);
                                    if (iIndexOf == -1) {
                                        iIndexOf = SOUND_EFFECT_FILES.size();
                                        SOUND_EFFECT_FILES.add(attributeValue2);
                                    }
                                    this.SOUND_EFFECT_FILES_MAP[i][0] = iIndexOf;
                                } catch (Exception e4) {
                                    Log.w(TAG, "Invalid touch sound ID: " + attributeValue);
                                }
                            }
                        }
                        if (xml == null) {
                            return;
                        }
                    } catch (Resources.NotFoundException e5) {
                        e3 = e5;
                        Log.w(TAG, "audio assets file not found", e3);
                        if (xml == null) {
                            return;
                        }
                    } catch (IOException e6) {
                        e2 = e6;
                        Log.w(TAG, "I/O exception reading touch sound assets", e2);
                        if (xml == null) {
                            return;
                        }
                    } catch (XmlPullParserException e7) {
                        e = e7;
                        Log.w(TAG, "XML parser exception reading touch sound assets", e);
                        if (xml == null) {
                            return;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (0 != 0) {
                        xmlResourceParser.close();
                    }
                    throw th;
                }
            } catch (Resources.NotFoundException e8) {
                xml = null;
                e3 = e8;
            } catch (IOException e9) {
                xml = null;
                e2 = e9;
            } catch (XmlPullParserException e10) {
                xml = null;
                e = e10;
            } catch (Throwable th3) {
                th = th3;
                if (0 != 0) {
                }
                throw th;
            }
            xml.close();
        }
    }

    public void playSoundEffect(int i) {
        playSoundEffectVolume(i, -1.0f);
    }

    public void playSoundEffectVolume(int i, float f) {
        if (isStreamMutedByRingerOrZenMode(1)) {
            return;
        }
        if (i >= 10 || i < 0) {
            Log.w(TAG, "AudioService effectType value " + i + " out of range");
            return;
        }
        sendMsg(this.mAudioHandler, 5, 2, i, (int) (f * 1000.0f), null, 0);
    }

    public boolean loadSoundEffects() {
        LoadSoundEffectReply loadSoundEffectReply = new LoadSoundEffectReply();
        synchronized (loadSoundEffectReply) {
            sendMsg(this.mAudioHandler, 7, 2, 0, 0, loadSoundEffectReply, 0);
            int i = 3;
            while (loadSoundEffectReply.mStatus == 1) {
                int i2 = i - 1;
                if (i <= 0) {
                    break;
                }
                try {
                    loadSoundEffectReply.wait(5000L);
                } catch (InterruptedException e) {
                    Log.w(TAG, "loadSoundEffects Interrupted while waiting sound pool loaded.");
                }
                i = i2;
            }
        }
        return loadSoundEffectReply.mStatus == 0;
    }

    public void unloadSoundEffects() {
        sendMsg(this.mAudioHandler, 20, 2, 0, 0, null, 0);
    }

    class SoundPoolListenerThread extends Thread {
        public SoundPoolListenerThread() {
            super("SoundPoolListenerThread");
        }

        @Override
        public void run() {
            Looper.prepare();
            AudioService.this.mSoundPoolLooper = Looper.myLooper();
            synchronized (AudioService.this.mSoundEffectsLock) {
                if (AudioService.this.mSoundPool != null) {
                    AudioService.this.mSoundPoolCallBack = new SoundPoolCallback();
                    AudioService.this.mSoundPool.setOnLoadCompleteListener(AudioService.this.mSoundPoolCallBack);
                }
                AudioService.this.mSoundEffectsLock.notify();
            }
            Looper.loop();
        }
    }

    private final class SoundPoolCallback implements SoundPool.OnLoadCompleteListener {
        List<Integer> mSamples;
        int mStatus;

        private SoundPoolCallback() {
            this.mStatus = 1;
            this.mSamples = new ArrayList();
        }

        public int status() {
            return this.mStatus;
        }

        public void setSamples(int[] iArr) {
            for (int i = 0; i < iArr.length; i++) {
                if (iArr[i] > 0) {
                    this.mSamples.add(Integer.valueOf(iArr[i]));
                }
            }
        }

        @Override
        public void onLoadComplete(SoundPool soundPool, int i, int i2) {
            synchronized (AudioService.this.mSoundEffectsLock) {
                int iIndexOf = this.mSamples.indexOf(Integer.valueOf(i));
                if (iIndexOf >= 0) {
                    this.mSamples.remove(iIndexOf);
                }
                if (i2 != 0 || this.mSamples.isEmpty()) {
                    this.mStatus = i2;
                    AudioService.this.mSoundEffectsLock.notify();
                }
            }
        }
    }

    public void reloadAudioSettings() {
        readAudioSettings(false);
    }

    private void readAudioSettings(boolean z) {
        readPersistedSettings();
        readUserRestrictions();
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < numStreamTypes; i++) {
            VolumeStreamState volumeStreamState = this.mStreamStates[i];
            if (!z || mStreamVolumeAlias[i] != 3) {
                volumeStreamState.readSettings();
                synchronized (VolumeStreamState.class) {
                    if (volumeStreamState.mIsMuted && ((!isStreamAffectedByMute(i) && !isStreamMutedByRingerOrZenMode(i)) || this.mUseFixedVolume)) {
                        volumeStreamState.mIsMuted = false;
                    }
                }
            }
        }
        setRingerModeInt(getRingerModeInternal(), false);
        checkAllFixedVolumeDevices();
        checkAllAliasStreamVolumes();
        checkMuteAffectedStreams();
        synchronized (this.mSafeMediaVolumeState) {
            this.mMusicActiveMs = MathUtils.constrain(Settings.Secure.getIntForUser(this.mContentResolver, "unsafe_volume_music_active_ms", 0, -2), 0, UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX);
            if (this.mSafeMediaVolumeState.intValue() == 3) {
                enforceSafeMediaVolume(TAG);
            }
        }
    }

    public void setSpeakerphoneOn(boolean z) {
        if (!checkAudioSettingsPermission("setSpeakerphoneOn()")) {
            return;
        }
        String str = "setSpeakerphoneOn(" + z + ") from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid();
        if (!z) {
            if (this.mForcedUseForComm == 1) {
                this.mForcedUseForComm = 0;
            }
        } else {
            if (this.mForcedUseForComm == 3) {
                sendMsg(this.mAudioHandler, 8, 2, 2, 0, str, 0);
            }
            this.mForcedUseForComm = 1;
        }
        this.mForcedUseForCommExt = this.mForcedUseForComm;
        sendMsg(this.mAudioHandler, 8, 2, 0, this.mForcedUseForComm, str, 0);
    }

    public boolean isSpeakerphoneOn() {
        return this.mForcedUseForCommExt == 1;
    }

    public void setBluetoothScoOn(boolean z) {
        if (!checkAudioSettingsPermission("setBluetoothScoOn()")) {
            Log.d(TAG, "checkAudioSettingsPermission declined");
            return;
        }
        if (Binder.getCallingUid() >= 10000) {
            this.mForcedUseForCommExt = z ? 3 : 0;
            Log.d(TAG, "Binder.getCallingUid() = " + Binder.getCallingUid() + "mForcedUseForCommExt = " + this.mForcedUseForCommExt);
            return;
        }
        setBluetoothScoOnInt(z, "setBluetoothScoOn(" + z + ") from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid());
    }

    public void setBluetoothScoOnInt(boolean z, String str) {
        Log.i(TAG, "setBluetoothScoOnInt: " + z + " " + str);
        if (z) {
            synchronized (this.mScoClients) {
                if (this.mBluetoothHeadset != null && this.mBluetoothHeadset.getAudioState(this.mBluetoothHeadsetDevice) != 12) {
                    this.mForcedUseForCommExt = 3;
                    Log.w(TAG, "setBluetoothScoOnInt(true) failed because " + this.mBluetoothHeadsetDevice + " is not in audio connected mode");
                    return;
                }
                this.mForcedUseForComm = 3;
            }
        } else if (this.mForcedUseForComm == 3) {
            this.mForcedUseForComm = 0;
        }
        this.mForcedUseForCommExt = this.mForcedUseForComm;
        StringBuilder sb = new StringBuilder();
        sb.append("BT_SCO=");
        sb.append(z ? "on" : "off");
        AudioSystem.setParameters(sb.toString());
        sendMsg(this.mAudioHandler, 8, 2, 0, this.mForcedUseForComm, str, 0);
        sendMsg(this.mAudioHandler, 8, 2, 2, this.mForcedUseForComm, str, 0);
        setRingerModeInt(getRingerModeInternal(), false);
    }

    public boolean isBluetoothScoOn() {
        return this.mForcedUseForCommExt == 3;
    }

    public void setBluetoothA2dpOn(boolean z) {
        String str = "setBluetoothA2dpOn(" + z + ") from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid();
        synchronized (this.mBluetoothA2dpEnabledLock) {
            if (this.mBluetoothA2dpEnabled == z) {
                return;
            }
            this.mBluetoothA2dpEnabled = z;
            sendMsg(this.mAudioHandler, 13, 2, 1, this.mBluetoothA2dpEnabled ? 0 : 10, str, 0);
        }
    }

    public boolean isBluetoothA2dpOn() {
        boolean z;
        synchronized (this.mBluetoothA2dpEnabledLock) {
            z = this.mBluetoothA2dpEnabled;
        }
        return z;
    }

    public void startBluetoothSco(IBinder iBinder, int i) {
        startBluetoothScoInt(iBinder, i < 18 ? 0 : -1);
    }

    public void startBluetoothScoVirtualCall(IBinder iBinder) {
        startBluetoothScoInt(iBinder, 0);
    }

    void startBluetoothScoInt(IBinder iBinder, int i) {
        if (!checkAudioSettingsPermission("startBluetoothSco()") || !this.mSystemReady) {
            return;
        }
        ScoClient scoClient = getScoClient(iBinder, true);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        scoClient.incCount(i);
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    public void stopBluetoothSco(IBinder iBinder) {
        if (!checkAudioSettingsPermission("stopBluetoothSco()") || !this.mSystemReady) {
            return;
        }
        ScoClient scoClient = getScoClient(iBinder, false);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        if (scoClient != null) {
            scoClient.decCount();
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private class ScoClient implements IBinder.DeathRecipient {
        private IBinder mCb;
        private int mCreatorPid = Binder.getCallingPid();
        private int mStartcount = 0;

        ScoClient(IBinder iBinder) {
            this.mCb = iBinder;
        }

        @Override
        public void binderDied() {
            synchronized (AudioService.this.mScoClients) {
                Log.w(AudioService.TAG, "SCO client died");
                if (AudioService.this.mScoClients.indexOf(this) < 0) {
                    Log.w(AudioService.TAG, "unregistered SCO client died");
                } else {
                    clearCount(true);
                    AudioService.this.mScoClients.remove(this);
                }
            }
        }

        public void incCount(int i) {
            synchronized (AudioService.this.mScoClients) {
                requestScoState(12, i);
                if (this.mStartcount == 0) {
                    try {
                        this.mCb.linkToDeath(this, 0);
                    } catch (RemoteException e) {
                        Log.w(AudioService.TAG, "ScoClient  incCount() could not link to " + this.mCb + " binder death");
                    }
                    if (this.mStartcount != 1) {
                        Log.i(AudioService.TAG, "mStartcount is 1, calling setBluetoothScoOn(true)in system context");
                        AudioService.this.setBluetoothScoOn(true);
                    } else if (this.mStartcount == 0) {
                        this.mStartcount++;
                        Log.i(AudioService.TAG, "mStartcount is 0, incrementing by 1");
                    }
                } else if (this.mStartcount != 1) {
                }
            }
        }

        public void decCount() {
            synchronized (AudioService.this.mScoClients) {
                if (this.mStartcount == 0) {
                    Log.w(AudioService.TAG, "ScoClient.decCount() already 0");
                } else {
                    this.mStartcount--;
                    if (this.mStartcount == 0) {
                        try {
                            this.mCb.unlinkToDeath(this, 0);
                        } catch (NoSuchElementException e) {
                            Log.w(AudioService.TAG, "decCount() going to 0 but not registered to binder");
                        }
                    }
                    requestScoState(10, 0);
                }
            }
        }

        public void clearCount(boolean z) {
            synchronized (AudioService.this.mScoClients) {
                if (this.mStartcount != 0) {
                    try {
                        this.mCb.unlinkToDeath(this, 0);
                    } catch (NoSuchElementException e) {
                        Log.w(AudioService.TAG, "clearCount() mStartcount: " + this.mStartcount + " != 0 but not registered to binder");
                    }
                    this.mStartcount = 0;
                    if (z) {
                        requestScoState(10, 0);
                    }
                } else {
                    this.mStartcount = 0;
                    if (z) {
                    }
                }
            }
        }

        public int getCount() {
            return this.mStartcount;
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public int getPid() {
            return this.mCreatorPid;
        }

        public int totalCount() {
            int count;
            synchronized (AudioService.this.mScoClients) {
                count = 0;
                Iterator it = AudioService.this.mScoClients.iterator();
                while (it.hasNext()) {
                    count += ((ScoClient) it.next()).getCount();
                }
            }
            return count;
        }

        private void requestScoState(int i, int i2) {
            int pid;
            AudioService.this.checkScoAudioState();
            int i3 = totalCount();
            if (i3 != 0) {
                Log.i(AudioService.TAG, "requestScoState: state=" + i + ", scoAudioMode=" + i2 + ", clientCount=" + i3);
                return;
            }
            if (i == 12) {
                AudioService.this.broadcastScoConnectionState(2);
                synchronized (AudioService.this.mSetModeDeathHandlers) {
                    if (!AudioService.this.mSetModeDeathHandlers.isEmpty()) {
                        pid = ((SetModeDeathHandler) AudioService.this.mSetModeDeathHandlers.get(0)).getPid();
                    } else {
                        pid = 0;
                    }
                    if (pid == 0 || pid == this.mCreatorPid) {
                        int i4 = AudioService.this.mScoAudioState;
                        if (i4 != 0) {
                            switch (i4) {
                                case 4:
                                    AudioService.this.mScoAudioState = 3;
                                    AudioService.this.broadcastScoConnectionState(1);
                                    break;
                                case 5:
                                    AudioService.this.mScoAudioState = 1;
                                    break;
                                default:
                                    Log.w(AudioService.TAG, "requestScoState: failed to connect in state " + AudioService.this.mScoAudioState + ", scoAudioMode=" + i2);
                                    AudioService.this.broadcastScoConnectionState(0);
                                    break;
                            }
                        } else {
                            AudioService.this.mScoAudioMode = i2;
                            if (i2 == -1) {
                                AudioService.this.mScoAudioMode = 0;
                                if (AudioService.this.mBluetoothHeadsetDevice != null) {
                                    AudioService.this.mScoAudioMode = Settings.Global.getInt(AudioService.this.mContentResolver, "bluetooth_sco_channel_" + AudioService.this.mBluetoothHeadsetDevice.getAddress(), 0);
                                    if (AudioService.this.mScoAudioMode > 2 || AudioService.this.mScoAudioMode < 0) {
                                        AudioService.this.mScoAudioMode = 0;
                                    }
                                }
                            }
                            if (AudioService.this.mBluetoothHeadset == null) {
                                if (AudioService.this.getBluetoothHeadset()) {
                                    AudioService.this.mScoAudioState = 1;
                                } else {
                                    Log.w(AudioService.TAG, "requestScoState: getBluetoothHeadset failed during connection, mScoAudioMode=" + AudioService.this.mScoAudioMode);
                                    AudioService.this.broadcastScoConnectionState(0);
                                }
                            } else if (AudioService.this.mBluetoothHeadsetDevice == null) {
                                Log.w(AudioService.TAG, "requestScoState: no active device while connecting, mScoAudioMode=" + AudioService.this.mScoAudioMode);
                                AudioService.this.broadcastScoConnectionState(0);
                            } else if (AudioService.connectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode)) {
                                AudioService.this.mScoAudioState = 3;
                            } else {
                                Log.w(AudioService.TAG, "requestScoState: connect to " + AudioService.this.mBluetoothHeadsetDevice + " failed, mScoAudioMode=" + AudioService.this.mScoAudioMode);
                                AudioService.this.broadcastScoConnectionState(0);
                            }
                        }
                        return;
                    }
                    Log.w(AudioService.TAG, "requestScoState: audio mode is not NORMAL and modeOwnerPid " + pid + " != creatorPid " + this.mCreatorPid);
                    AudioService.this.broadcastScoConnectionState(0);
                    return;
                }
            }
            if (i == 10) {
                int i5 = AudioService.this.mScoAudioState;
                if (i5 == 1) {
                    AudioService.this.mScoAudioState = 0;
                    AudioService.this.broadcastScoConnectionState(0);
                    return;
                }
                if (i5 != 3) {
                    Log.w(AudioService.TAG, "requestScoState: failed to disconnect in state " + AudioService.this.mScoAudioState + ", scoAudioMode=" + i2);
                    AudioService.this.broadcastScoConnectionState(0);
                    return;
                }
                if (AudioService.this.mBluetoothHeadset == null) {
                    if (AudioService.this.getBluetoothHeadset()) {
                        AudioService.this.mScoAudioState = 4;
                        return;
                    }
                    Log.w(AudioService.TAG, "requestScoState: getBluetoothHeadset failed during disconnection, mScoAudioMode=" + AudioService.this.mScoAudioMode);
                    AudioService.this.mScoAudioState = 0;
                    AudioService.this.broadcastScoConnectionState(0);
                    return;
                }
                if (AudioService.this.mBluetoothHeadsetDevice == null) {
                    AudioService.this.mScoAudioState = 0;
                    AudioService.this.broadcastScoConnectionState(0);
                } else if (AudioService.disconnectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode)) {
                    AudioService.this.mScoAudioState = 5;
                } else {
                    AudioService.this.mScoAudioState = 0;
                    AudioService.this.broadcastScoConnectionState(0);
                }
            }
        }
    }

    private void checkScoAudioState() {
        synchronized (this.mScoClients) {
            if (this.mBluetoothHeadset != null && this.mBluetoothHeadsetDevice != null && this.mScoAudioState == 0 && this.mBluetoothHeadset.getAudioState(this.mBluetoothHeadsetDevice) != 10) {
                this.mScoAudioState = 2;
            }
        }
    }

    private ScoClient getScoClient(IBinder iBinder, boolean z) {
        synchronized (this.mScoClients) {
            for (ScoClient scoClient : this.mScoClients) {
                if (scoClient.getBinder() == iBinder) {
                    return scoClient;
                }
            }
            if (z) {
                ScoClient scoClient2 = new ScoClient(iBinder);
                this.mScoClients.add(scoClient2);
                return scoClient2;
            }
            return null;
        }
    }

    public void clearAllScoClients(int i, boolean z) {
        synchronized (this.mScoClients) {
            ScoClient scoClient = null;
            for (ScoClient scoClient2 : this.mScoClients) {
                if (scoClient2.getPid() != i) {
                    scoClient2.clearCount(z);
                } else {
                    scoClient = scoClient2;
                }
            }
            this.mScoClients.clear();
            if (scoClient != null) {
                this.mScoClients.add(scoClient);
            }
        }
    }

    private boolean getBluetoothHeadset() {
        boolean profileProxy;
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null) {
            profileProxy = defaultAdapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 1);
        } else {
            profileProxy = false;
        }
        sendMsg(this.mAudioHandler, 9, 0, 0, 0, null, profileProxy ? BT_HEADSET_CNCT_TIMEOUT_MS : 0);
        return profileProxy;
    }

    private void disconnectBluetoothSco(int i) {
        synchronized (this.mScoClients) {
            checkScoAudioState();
            if (this.mScoAudioState == 2) {
                return;
            }
            clearAllScoClients(i, true);
        }
    }

    private static boolean disconnectBluetoothScoAudioHelper(BluetoothHeadset bluetoothHeadset, BluetoothDevice bluetoothDevice, int i) {
        switch (i) {
            case 0:
                return bluetoothHeadset.stopScoUsingVirtualVoiceCall();
            case 1:
                return bluetoothHeadset.disconnectAudio();
            case 2:
                return bluetoothHeadset.stopVoiceRecognition(bluetoothDevice);
            default:
                return false;
        }
    }

    private static boolean connectBluetoothScoAudioHelper(BluetoothHeadset bluetoothHeadset, BluetoothDevice bluetoothDevice, int i) {
        switch (i) {
            case 0:
                return bluetoothHeadset.startScoUsingVirtualVoiceCall();
            case 1:
                return bluetoothHeadset.connectAudio();
            case 2:
                return bluetoothHeadset.startVoiceRecognition(bluetoothDevice);
            default:
                return false;
        }
    }

    private void resetBluetoothSco() {
        synchronized (this.mScoClients) {
            clearAllScoClients(0, false);
            this.mScoAudioState = 0;
            broadcastScoConnectionState(0);
        }
        AudioSystem.setParameters("A2dpSuspended=false");
        setBluetoothScoOnInt(false, "resetBluetoothSco");
    }

    private void broadcastScoConnectionState(int i) {
        sendMsg(this.mAudioHandler, 19, 2, i, 0, null, 0);
    }

    private void onBroadcastScoConnectionState(int i) {
        if (i != this.mScoConnectionState) {
            Intent intent = new Intent("android.media.ACTION_SCO_AUDIO_STATE_UPDATED");
            intent.putExtra("android.media.extra.SCO_AUDIO_STATE", i);
            intent.putExtra("android.media.extra.SCO_AUDIO_PREVIOUS_STATE", this.mScoConnectionState);
            sendStickyBroadcastToAll(intent);
            this.mScoConnectionState = i;
        }
    }

    private boolean handleBtScoActiveDeviceChange(BluetoothDevice bluetoothDevice, boolean z) {
        boolean zHandleDeviceConnection;
        if (bluetoothDevice == null) {
            return true;
        }
        String address = bluetoothDevice.getAddress();
        BluetoothClass bluetoothClass = bluetoothDevice.getBluetoothClass();
        int[] iArr = {16, 32, 64};
        if (bluetoothClass != null) {
            int deviceClass = bluetoothClass.getDeviceClass();
            if (deviceClass == 1028 || deviceClass == 1032) {
                iArr = new int[]{32};
            } else if (deviceClass == 1056) {
                iArr = new int[]{64};
            }
        }
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            address = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String name = bluetoothDevice.getName();
        if (z) {
            zHandleDeviceConnection = handleDeviceConnection(z, iArr[0], address, name) | false;
        } else {
            boolean zHandleDeviceConnection2 = false;
            for (int i : iArr) {
                zHandleDeviceConnection2 |= handleDeviceConnection(z, i, address, name);
            }
            zHandleDeviceConnection = zHandleDeviceConnection2;
        }
        if (handleDeviceConnection(z, -2147483640, address, name) && zHandleDeviceConnection) {
            return true;
        }
        return false;
    }

    private void setBtScoActiveDevice(BluetoothDevice bluetoothDevice) {
        synchronized (this.mScoClients) {
            Log.i(TAG, "setBtScoActiveDevice: " + this.mBluetoothHeadsetDevice + " -> " + bluetoothDevice);
            BluetoothDevice bluetoothDevice2 = this.mBluetoothHeadsetDevice;
            if (!Objects.equals(bluetoothDevice, bluetoothDevice2)) {
                if (!handleBtScoActiveDeviceChange(bluetoothDevice2, false)) {
                    Log.w(TAG, "setBtScoActiveDevice() failed to remove previous device " + bluetoothDevice2);
                }
                if (!handleBtScoActiveDeviceChange(bluetoothDevice, true)) {
                    Log.e(TAG, "setBtScoActiveDevice() failed to add new device " + bluetoothDevice);
                    bluetoothDevice = null;
                }
                this.mBluetoothHeadsetDevice = bluetoothDevice;
                if (this.mBluetoothHeadsetDevice == null) {
                    resetBluetoothSco();
                }
            }
        }
    }

    void disconnectAllBluetoothProfiles() {
        disconnectA2dp();
        disconnectA2dpSink();
        disconnectHeadset();
        disconnectHearingAid();
    }

    void disconnectA2dp() {
        synchronized (this.mConnectedDevices) {
            synchronized (this.mA2dpAvrcpLock) {
                ArraySet arraySet = null;
                for (int i = 0; i < this.mConnectedDevices.size(); i++) {
                    DeviceListSpec deviceListSpecValueAt = this.mConnectedDevices.valueAt(i);
                    if (deviceListSpecValueAt.mDeviceType == 128) {
                        if (arraySet == null) {
                            arraySet = new ArraySet();
                        }
                        arraySet.add(deviceListSpecValueAt.mDeviceAddress);
                    }
                }
                if (arraySet != null) {
                    int iCheckSendBecomingNoisyIntent = checkSendBecomingNoisyIntent(128, 0, 0);
                    for (int i2 = 0; i2 < arraySet.size(); i2++) {
                        makeA2dpDeviceUnavailableLater((String) arraySet.valueAt(i2), iCheckSendBecomingNoisyIntent);
                    }
                }
            }
        }
    }

    void disconnectA2dpSink() {
        synchronized (this.mConnectedDevices) {
            ArraySet arraySet = null;
            for (int i = 0; i < this.mConnectedDevices.size(); i++) {
                DeviceListSpec deviceListSpecValueAt = this.mConnectedDevices.valueAt(i);
                if (deviceListSpecValueAt.mDeviceType == -2147352576) {
                    if (arraySet == null) {
                        arraySet = new ArraySet();
                    }
                    arraySet.add(deviceListSpecValueAt.mDeviceAddress);
                }
            }
            if (arraySet != null) {
                for (int i2 = 0; i2 < arraySet.size(); i2++) {
                    makeA2dpSrcUnavailable((String) arraySet.valueAt(i2));
                }
            }
        }
    }

    void disconnectHeadset() {
        synchronized (this.mScoClients) {
            setBtScoActiveDevice(null);
            this.mBluetoothHeadset = null;
        }
    }

    void disconnectHearingAid() {
        synchronized (this.mConnectedDevices) {
            synchronized (this.mHearingAidLock) {
                ArraySet arraySet = null;
                for (int i = 0; i < this.mConnectedDevices.size(); i++) {
                    DeviceListSpec deviceListSpecValueAt = this.mConnectedDevices.valueAt(i);
                    if (deviceListSpecValueAt.mDeviceType == 134217728) {
                        if (arraySet == null) {
                            arraySet = new ArraySet();
                        }
                        arraySet.add(deviceListSpecValueAt.mDeviceAddress);
                    }
                }
                if (arraySet != null) {
                    checkSendBecomingNoisyIntent(134217728, 0, 0);
                    for (int i2 = 0; i2 < arraySet.size(); i2++) {
                        makeHearingAidDeviceUnavailable((String) arraySet.valueAt(i2));
                    }
                }
            }
        }
    }

    private void onCheckMusicActive(String str) {
        synchronized (this.mSafeMediaVolumeState) {
            if (this.mSafeMediaVolumeState.intValue() == 2) {
                int deviceForStream = getDeviceForStream(3);
                if ((67108876 & deviceForStream) != 0) {
                    sendMsg(this.mAudioHandler, 14, 0, 0, 0, str, MUSIC_ACTIVE_POLL_PERIOD_MS);
                    int index = this.mStreamStates[3].getIndex(deviceForStream);
                    if (AudioSystem.isStreamActive(3, 0) && index > safeMediaVolumeIndex(deviceForStream)) {
                        this.mMusicActiveMs += MUSIC_ACTIVE_POLL_PERIOD_MS;
                        if (this.mMusicActiveMs > UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX) {
                            setSafeMediaVolumeEnabled(true, str);
                            this.mMusicActiveMs = 0;
                        }
                        saveMusicActiveMs();
                    }
                }
            }
        }
    }

    private void saveMusicActiveMs() {
        this.mAudioHandler.obtainMessage(22, this.mMusicActiveMs, 0).sendToTarget();
    }

    private int getSafeUsbMediaVolumeIndex() {
        int i = MIN_STREAM_VOLUME[3];
        int i2 = MAX_STREAM_VOLUME[3];
        this.mSafeUsbMediaVolumeDbfs = this.mContext.getResources().getInteger(R.integer.config_doublePressOnStemPrimaryBehavior) / 100.0f;
        while (true) {
            if (Math.abs(i2 - i) <= 1) {
                break;
            }
            int i3 = (i2 + i) / 2;
            float streamVolumeDB = AudioSystem.getStreamVolumeDB(3, i3, 67108864);
            if (Float.isNaN(streamVolumeDB)) {
                break;
            }
            if (streamVolumeDB == this.mSafeUsbMediaVolumeDbfs) {
                i = i3;
                break;
            }
            if (streamVolumeDB < this.mSafeUsbMediaVolumeDbfs) {
                i = i3;
            } else {
                i2 = i3;
            }
        }
        return i * 10;
    }

    private void onConfigureSafeVolume(boolean z, String str) {
        int i;
        synchronized (this.mSafeMediaVolumeState) {
            int i2 = this.mContext.getResources().getConfiguration().mcc;
            if (this.mMcc != i2 || (this.mMcc == 0 && z)) {
                this.mSafeMediaVolumeIndex = this.mContext.getResources().getInteger(R.integer.config_doublePressOnPowerBehavior) * 10;
                this.mSafeUsbMediaVolumeIndex = getSafeUsbMediaVolumeIndex();
                boolean z2 = SystemProperties.getBoolean("audio.safemedia.force", false) || this.mContext.getResources().getBoolean(R.^attr-private.maxCollapsedHeightSmall);
                boolean z3 = SystemProperties.getBoolean("audio.safemedia.bypass", false);
                if (z2 && !z3) {
                    if (this.mSafeMediaVolumeState.intValue() != 2) {
                        if (this.mMusicActiveMs == 0) {
                            this.mSafeMediaVolumeState = 3;
                            enforceSafeMediaVolume(str);
                        } else {
                            this.mSafeMediaVolumeState = 2;
                        }
                    }
                    i = 3;
                } else {
                    this.mSafeMediaVolumeState = 1;
                    i = 1;
                }
                this.mMcc = i2;
                sendMsg(this.mAudioHandler, 18, 2, i, 0, null, 0);
            }
        }
    }

    private int checkForRingerModeChange(int i, int i2, int i3, boolean z, String str, int i4) {
        if (isPlatformTelevision() || this.mIsSingleVolume) {
            return 1;
        }
        int ringerModeInternal = getRingerModeInternal();
        switch (ringerModeInternal) {
            case 0:
                if (!this.mIsSingleVolume || i2 != -1 || i < i3 * 2 || !z) {
                    if (i2 == 1 || i2 == 101 || i2 == 100) {
                        if (this.mVolumePolicy.volumeUpToExitSilent) {
                            ringerModeInternal = (this.mHasVibrator && i2 == 1) ? 1 : 2;
                        } else {
                            i = NetworkConstants.ICMPV6_ECHO_REPLY_TYPE;
                        }
                    }
                    i &= -2;
                    break;
                }
                break;
            case 1:
                if (!this.mHasVibrator) {
                    Log.e(TAG, "checkForRingerModeChange() current ringer mode is vibratebut no vibrator is present");
                } else if (i2 != -1) {
                    if (i2 == 1 || i2 == 101 || i2 == 100) {
                    }
                    i &= -2;
                } else {
                    if (this.mIsSingleVolume && i >= i3 * 2 && z) {
                        ringerModeInternal = 2;
                    } else if (this.mPrevVolDirection != -1) {
                        if (!this.mVolumePolicy.volumeDownToEnterSilent) {
                            i = 2049;
                        } else if (SystemClock.uptimeMillis() - this.mLoweredFromNormalToVibrateTime > this.mVolumePolicy.vibrateToSilentDebounce && this.mRingerModeDelegate.canVolumeDownEnterSilent()) {
                            ringerModeInternal = 0;
                        }
                    }
                    i &= -2;
                }
                break;
            case 2:
                if (i2 != -1) {
                    if (this.mIsSingleVolume && (i2 == 101 || i2 == -100)) {
                        ringerModeInternal = this.mHasVibrator ? 1 : 0;
                        i = 0;
                    }
                } else if (!this.mHasVibrator) {
                    if (i == i3 && this.mVolumePolicy.volumeDownToEnterSilent) {
                        ringerModeInternal = 0;
                    }
                } else if (i3 <= i && i < 2 * i3) {
                    this.mLoweredFromNormalToVibrateTime = SystemClock.uptimeMillis();
                    ringerModeInternal = 1;
                }
                break;
            default:
                Log.e(TAG, "checkForRingerModeChange() wrong ringer mode: " + ringerModeInternal);
                break;
        }
        if (isAndroidNPlus(str) && wouldToggleZenMode(ringerModeInternal) && !this.mNm.isNotificationPolicyAccessGrantedForPackage(str) && (i4 & 4096) == 0) {
            throw new SecurityException("Not allowed to change Do Not Disturb state");
        }
        setRingerMode(ringerModeInternal, "AudioService.checkForRingerModeChange", false);
        this.mPrevVolDirection = i2;
        return i;
    }

    public boolean isStreamAffectedByRingerMode(int i) {
        return ((1 << i) & this.mRingerModeAffectedStreams) != 0;
    }

    private boolean shouldZenMuteStream(int i) {
        if (this.mNm.getZenMode() != 1) {
            return false;
        }
        NotificationManager.Policy notificationPolicy = this.mNm.getNotificationPolicy();
        return (((notificationPolicy.priorityCategories & 32) == 0) && isAlarm(i)) || (((notificationPolicy.priorityCategories & 64) == 0) && isMedia(i)) || ((((notificationPolicy.priorityCategories & 128) == 0) && isSystem(i)) || (ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(this.mNm.getNotificationPolicy()) && isNotificationOrRinger(i)));
    }

    private boolean isStreamMutedByRingerOrZenMode(int i) {
        return ((1 << i) & this.mRingerAndZenModeMutedStreams) != 0;
    }

    private boolean updateZenModeAffectedStreams() {
        int i;
        int i2;
        if (this.mSystemReady && this.mNm.getZenMode() == 1) {
            NotificationManager.Policy notificationPolicy = this.mNm.getNotificationPolicy();
            if ((notificationPolicy.priorityCategories & 32) == 0) {
                i2 = 16;
            } else {
                i2 = 0;
            }
            if ((notificationPolicy.priorityCategories & 64) == 0) {
                i2 |= 8;
            }
            if ((notificationPolicy.priorityCategories & 128) == 0) {
                i = i2 | 2;
            } else {
                i = i2;
            }
        } else {
            i = 0;
        }
        if (this.mZenModeAffectedStreams == i) {
            return false;
        }
        this.mZenModeAffectedStreams = i;
        return true;
    }

    @GuardedBy("mSettingsLock")
    private boolean updateRingerAndZenModeAffectedStreams() {
        int i;
        int i2;
        boolean zUpdateZenModeAffectedStreams = updateZenModeAffectedStreams();
        int intForUser = Settings.System.getIntForUser(this.mContentResolver, "mode_ringer_streams_affected", 166, -2);
        if (this.mIsSingleVolume) {
            intForUser = 0;
        } else if (this.mRingerModeDelegate != null) {
            intForUser = this.mRingerModeDelegate.getRingerModeAffectedStreams(intForUser);
        }
        if (this.mCameraSoundForced) {
            i = intForUser & (-129);
        } else {
            i = intForUser | 128;
        }
        if (mStreamVolumeAlias[8] == 2) {
            i2 = i | 256;
        } else {
            i2 = i & (-257);
        }
        if (i2 != this.mRingerModeAffectedStreams) {
            Settings.System.putIntForUser(this.mContentResolver, "mode_ringer_streams_affected", i2, -2);
            this.mRingerModeAffectedStreams = i2;
            return true;
        }
        return zUpdateZenModeAffectedStreams;
    }

    public boolean isStreamAffectedByMute(int i) {
        return ((1 << i) & this.mMuteAffectedStreams) != 0;
    }

    private void ensureValidDirection(int i) {
        if (i != -100) {
            switch (i) {
                case -1:
                case 0:
                case 1:
                    return;
                default:
                    switch (i) {
                        case 100:
                        case 101:
                            return;
                        default:
                            throw new IllegalArgumentException("Bad direction " + i);
                    }
            }
        }
    }

    private void ensureValidStreamType(int i) {
        if (i < 0 || i >= this.mStreamStates.length) {
            throw new IllegalArgumentException("Bad stream type " + i);
        }
    }

    private boolean isMuteAdjust(int i) {
        return i == -100 || i == 100 || i == 101;
    }

    private boolean isInCommunication() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        boolean zIsInCall = telecomManager.isInCall();
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        return zIsInCall || getMode() == 3 || getMode() == 2;
    }

    private boolean wasStreamActiveRecently(int i, int i2) {
        return AudioSystem.isStreamActive(i, i2) || AudioSystem.isStreamActiveRemotely(i, i2);
    }

    private int getActiveStreamType(int i) {
        if (this.mIsSingleVolume && i == Integer.MIN_VALUE) {
            return 3;
        }
        if (this.mPlatformType == 1) {
            if (isInCommunication()) {
                return AudioSystem.getForceUse(0) == 3 ? 6 : 0;
            }
            if (i == Integer.MIN_VALUE) {
                if (wasStreamActiveRecently(2, sStreamOverrideDelayMs)) {
                    if (DEBUG_VOL) {
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING stream active");
                    }
                    return 2;
                }
                if (wasStreamActiveRecently(5, sStreamOverrideDelayMs)) {
                    if (DEBUG_VOL) {
                        Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION stream active");
                    }
                    return 5;
                }
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing DEFAULT_VOL_STREAM_NO_PLAYBACK(3) b/c default");
                }
                return 3;
            }
            if (wasStreamActiveRecently(5, sStreamOverrideDelayMs)) {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION stream active");
                }
                return 5;
            }
            if (wasStreamActiveRecently(2, sStreamOverrideDelayMs)) {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING stream active");
                }
                return 2;
            }
        }
        if (isInCommunication()) {
            if (AudioSystem.getForceUse(0) == 3) {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_BLUETOOTH_SCO");
                }
                return 6;
            }
            if (DEBUG_VOL) {
                Log.v(TAG, "getActiveStreamType: Forcing STREAM_VOICE_CALL");
            }
            return 0;
        }
        if (AudioSystem.isStreamActive(5, sStreamOverrideDelayMs)) {
            if (DEBUG_VOL) {
                Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION");
            }
            return 5;
        }
        if (AudioSystem.isStreamActive(2, sStreamOverrideDelayMs)) {
            if (DEBUG_VOL) {
                Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING");
            }
            return 2;
        }
        if (i == Integer.MIN_VALUE) {
            if (AudioSystem.isStreamActive(5, sStreamOverrideDelayMs)) {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION");
                }
                return 5;
            }
            if (AudioSystem.isStreamActive(2, sStreamOverrideDelayMs)) {
                if (DEBUG_VOL) {
                    Log.v(TAG, "getActiveStreamType: Forcing STREAM_RING");
                }
                return 2;
            }
            if (DEBUG_VOL) {
                Log.v(TAG, "getActiveStreamType: Forcing DEFAULT_VOL_STREAM_NO_PLAYBACK(3) b/c default");
            }
            return 3;
        }
        if (DEBUG_VOL) {
            Log.v(TAG, "getActiveStreamType: Returning suggested type " + i);
        }
        return i;
    }

    private void broadcastRingerMode(String str, int i) {
        Intent intent = new Intent(str);
        intent.putExtra("android.media.EXTRA_RINGER_MODE", i);
        intent.addFlags(603979776);
        sendStickyBroadcastToAll(intent);
    }

    private void broadcastVibrateSetting(int i) {
        if (this.mActivityManagerInternal.isSystemReady()) {
            Intent intent = new Intent("android.media.VIBRATE_SETTING_CHANGED");
            intent.putExtra("android.media.EXTRA_VIBRATE_TYPE", i);
            intent.putExtra("android.media.EXTRA_VIBRATE_SETTING", getVibrateSetting(i));
            sendBroadcastToAll(intent);
        }
    }

    private void queueMsgUnderWakeLock(Handler handler, int i, int i2, int i3, Object obj, int i4) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        this.mAudioEventWakeLock.acquire();
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        sendMsg(handler, i, 2, i2, i3, obj, i4);
    }

    private static void sendMsg(Handler handler, int i, int i2, int i3, int i4, Object obj, int i5) {
        if (i2 == 0) {
            handler.removeMessages(i);
        } else if (i2 == 1 && handler.hasMessages(i)) {
            return;
        }
        synchronized (mLastDeviceConnectMsgTime) {
            long jUptimeMillis = SystemClock.uptimeMillis() + ((long) i5);
            if (i == 101 || i == 102 || i == 105 || i == 100 || i == 103 || i == 106) {
                if (mLastDeviceConnectMsgTime.longValue() >= jUptimeMillis) {
                    jUptimeMillis = mLastDeviceConnectMsgTime.longValue() + 30;
                }
                mLastDeviceConnectMsgTime = Long.valueOf(jUptimeMillis);
            }
            handler.sendMessageAtTime(handler.obtainMessage(i, i3, i4, obj), jUptimeMillis);
        }
    }

    boolean checkAudioSettingsPermission(String str) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_SETTINGS") == 0) {
            return true;
        }
        Log.w(TAG, "Audio Settings Permission Denial: " + str + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        return false;
    }

    private int getDeviceForStream(int i) {
        int devicesForStream = getDevicesForStream(i);
        if (((devicesForStream - 1) & devicesForStream) == 0) {
            return devicesForStream;
        }
        if ((devicesForStream & 2) != 0) {
            return 2;
        }
        return (devicesForStream & DumpState.DUMP_DOMAIN_PREFERRED) != 0 ? DumpState.DUMP_DOMAIN_PREFERRED : (devicesForStream & DumpState.DUMP_FROZEN) != 0 ? DumpState.DUMP_FROZEN : (devicesForStream & DumpState.DUMP_COMPILER_STATS) != 0 ? DumpState.DUMP_COMPILER_STATS : devicesForStream & 896;
    }

    private int getDevicesForStream(int i) {
        return getDevicesForStream(i, true);
    }

    private int getDevicesForStream(int i, boolean z) {
        int iObserveDevicesForStream_syncVSS;
        ensureValidStreamType(i);
        synchronized (VolumeStreamState.class) {
            iObserveDevicesForStream_syncVSS = this.mStreamStates[i].observeDevicesForStream_syncVSS(z);
        }
        return iObserveDevicesForStream_syncVSS;
    }

    private void observeDevicesForStreams(int i) {
        synchronized (VolumeStreamState.class) {
            for (int i2 = 0; i2 < this.mStreamStates.length; i2++) {
                if (i2 != i) {
                    this.mStreamStates[i2].observeDevicesForStream_syncVSS(false);
                }
            }
        }
    }

    class WiredDeviceConnectionState {
        public final String mAddress;
        public final String mCaller;
        public final String mName;
        public final int mState;
        public final int mType;

        public WiredDeviceConnectionState(int i, int i2, String str, String str2, String str3) {
            this.mType = i;
            this.mState = i2;
            this.mAddress = str;
            this.mName = str2;
            this.mCaller = str3;
        }
    }

    public void setWiredDeviceConnectionState(int i, int i2, String str, String str2, String str3) {
        String str4;
        String str5;
        synchronized (this.mConnectedDevices) {
            if (DEBUG_DEVICES) {
                StringBuilder sb = new StringBuilder();
                sb.append("setWiredDeviceConnectionState(");
                sb.append(i2);
                sb.append(" nm: ");
                str5 = str2;
                sb.append(str5);
                sb.append(" addr:");
                str4 = str;
                sb.append(str4);
                sb.append(")");
                Slog.i(TAG, sb.toString());
            } else {
                str4 = str;
                str5 = str2;
            }
            queueMsgUnderWakeLock(this.mAudioHandler, 100, 0, 0, new WiredDeviceConnectionState(i, i2, str4, str5, str3), checkSendBecomingNoisyIntent(i, i2, 0));
        }
    }

    public void setHearingAidDeviceConnectionState(BluetoothDevice bluetoothDevice, int i) {
        Log.i(TAG, "setBluetoothHearingAidDeviceConnectionState");
        setBluetoothHearingAidDeviceConnectionState(bluetoothDevice, i, false, 0);
    }

    public int setBluetoothHearingAidDeviceConnectionState(BluetoothDevice bluetoothDevice, int i, boolean z, int i2) {
        int i3;
        synchronized (this.mConnectedDevices) {
            if (!z) {
                iCheckSendBecomingNoisyIntent = checkSendBecomingNoisyIntent(134217728, i == 2 ? 1 : 0, i2);
                i3 = iCheckSendBecomingNoisyIntent;
                queueMsgUnderWakeLock(this.mAudioHandler, 105, i, 0, bluetoothDevice, i3);
            } else {
                i3 = iCheckSendBecomingNoisyIntent;
                queueMsgUnderWakeLock(this.mAudioHandler, 105, i, 0, bluetoothDevice, i3);
            }
        }
        return i3;
    }

    public int setBluetoothA2dpDeviceConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) {
        return setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(bluetoothDevice, i, i2, false, -1);
    }

    public int setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(BluetoothDevice bluetoothDevice, int i, int i2, boolean z, int i3) {
        if (this.mAudioHandler.hasMessages(102, bluetoothDevice)) {
            return 0;
        }
        return setBluetoothA2dpDeviceConnectionStateInt(bluetoothDevice, i, i2, z, 0, i3);
    }

    public int setBluetoothA2dpDeviceConnectionStateInt(BluetoothDevice bluetoothDevice, int i, int i2, boolean z, int i3, int i4) {
        int i5;
        if (i2 != 2 && i2 != 11) {
            throw new IllegalArgumentException("invalid profile " + i2);
        }
        synchronized (this.mConnectedDevices) {
            if (i2 == 2 && !z) {
                iCheckSendBecomingNoisyIntent = checkSendBecomingNoisyIntent(128, i == 2 ? 1 : 0, i3);
                i5 = iCheckSendBecomingNoisyIntent;
                if (DEBUG_DEVICES) {
                }
                queueMsgUnderWakeLock(this.mAudioHandler, i2 != 2 ? 102 : 101, i, i4, bluetoothDevice, i5);
            } else {
                i5 = iCheckSendBecomingNoisyIntent;
                if (DEBUG_DEVICES) {
                    Log.d(TAG, "setBluetoothA2dpDeviceConnectionStateInt device: " + bluetoothDevice + " state: " + i + " delay(ms): " + i5 + " suppressNoisyIntent: " + z);
                }
                queueMsgUnderWakeLock(this.mAudioHandler, i2 != 2 ? 102 : 101, i, i4, bluetoothDevice, i5);
            }
        }
        return i5;
    }

    public void handleBluetoothA2dpDeviceConfigChange(BluetoothDevice bluetoothDevice) {
        synchronized (this.mConnectedDevices) {
            queueMsgUnderWakeLock(this.mAudioHandler, 103, 0, 0, bluetoothDevice, 0);
        }
    }

    private void onAccessoryPlugMediaUnmute(int i) {
        if (DEBUG_VOL) {
            Log.i(TAG, String.format("onAccessoryPlugMediaUnmute newDevice=%d [%s]", Integer.valueOf(i), AudioSystem.getOutputDeviceName(i)));
        }
        synchronized (this.mConnectedDevices) {
            if (this.mNm.getZenMode() != 2 && (DEVICE_MEDIA_UNMUTED_ON_PLUG & i) != 0 && this.mStreamStates[3].mIsMuted && this.mStreamStates[3].getIndex(i) != 0 && (AudioSystem.getDevicesForStream(3) & i) != 0) {
                if (DEBUG_VOL) {
                    Log.i(TAG, String.format(" onAccessoryPlugMediaUnmute unmuting device=%d [%s]", Integer.valueOf(i), AudioSystem.getOutputDeviceName(i)));
                }
                this.mStreamStates[3].mute(false);
            }
        }
    }

    public class VolumeStreamState {
        private final SparseIntArray mIndexMap;
        private int mIndexMax;
        private int mIndexMin;
        private boolean mIsMuted;
        private int mObservedDevices;
        private final Intent mStreamDevicesChanged;
        private final int mStreamType;
        private final Intent mVolumeChanged;
        private String mVolumeIndexSettingName;

        private VolumeStreamState(String str, int i) {
            this.mIndexMap = new SparseIntArray(8);
            this.mVolumeIndexSettingName = str;
            this.mStreamType = i;
            this.mIndexMin = AudioService.MIN_STREAM_VOLUME[i] * 10;
            this.mIndexMax = AudioService.MAX_STREAM_VOLUME[i] * 10;
            AudioSystem.initStreamVolume(i, this.mIndexMin / 10, this.mIndexMax / 10);
            readSettings();
            this.mVolumeChanged = new Intent("android.media.VOLUME_CHANGED_ACTION");
            this.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", this.mStreamType);
            this.mStreamDevicesChanged = new Intent("android.media.STREAM_DEVICES_CHANGED_ACTION");
            this.mStreamDevicesChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", this.mStreamType);
        }

        public int observeDevicesForStream_syncVSS(boolean z) {
            int devicesForStream = AudioSystem.getDevicesForStream(this.mStreamType);
            if (devicesForStream == this.mObservedDevices) {
                return devicesForStream;
            }
            int i = this.mObservedDevices;
            this.mObservedDevices = devicesForStream;
            if (z) {
                AudioService.this.observeDevicesForStreams(this.mStreamType);
            }
            if (AudioService.mStreamVolumeAlias[this.mStreamType] == this.mStreamType) {
                EventLogTags.writeStreamDevicesChanged(this.mStreamType, i, devicesForStream);
            }
            AudioService.this.sendBroadcastToAll(this.mStreamDevicesChanged.putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES", i).putExtra("android.media.EXTRA_VOLUME_STREAM_DEVICES", devicesForStream));
            return devicesForStream;
        }

        public String getSettingNameForDevice(int i) {
            if (!hasValidSettingsName()) {
                return null;
            }
            String outputDeviceName = AudioSystem.getOutputDeviceName(i);
            if (outputDeviceName.isEmpty()) {
                return this.mVolumeIndexSettingName;
            }
            return this.mVolumeIndexSettingName + "_" + outputDeviceName;
        }

        private boolean hasValidSettingsName() {
            return (this.mVolumeIndexSettingName == null || this.mVolumeIndexSettingName.isEmpty()) ? false : true;
        }

        public void readSettings() {
            int intForUser;
            synchronized (AudioService.this.mSettingsLock) {
                synchronized (VolumeStreamState.class) {
                    if (AudioService.this.mUseFixedVolume) {
                        this.mIndexMap.put(1073741824, this.mIndexMax);
                        return;
                    }
                    if (this.mStreamType != 1 && this.mStreamType != 7) {
                        synchronized (VolumeStreamState.class) {
                            int i = 1342177279;
                            int i2 = 0;
                            while (i != 0) {
                                int i3 = 1 << i2;
                                if ((i3 & i) != 0) {
                                    i &= ~i3;
                                    if (i3 == 1073741824) {
                                        intForUser = AudioSystem.DEFAULT_STREAM_VOLUME[this.mStreamType];
                                    } else {
                                        intForUser = -1;
                                    }
                                    if (hasValidSettingsName()) {
                                        intForUser = Settings.System.getIntForUser(AudioService.this.mContentResolver, getSettingNameForDevice(i3), intForUser, -2);
                                    }
                                    if (intForUser != -1) {
                                        this.mIndexMap.put(i3, getValidIndex(intForUser * 10));
                                    }
                                }
                                i2++;
                            }
                        }
                        return;
                    }
                    int i4 = 10 * AudioSystem.DEFAULT_STREAM_VOLUME[this.mStreamType];
                    if (AudioService.this.mCameraSoundForced) {
                        i4 = this.mIndexMax;
                    }
                    this.mIndexMap.put(1073741824, i4);
                }
            }
        }

        private int getAbsoluteVolumeIndex(int i) {
            if (i == 0) {
                return 0;
            }
            if (i == 1) {
                return ((int) (((double) this.mIndexMax) * 0.5d)) / 10;
            }
            if (i == 2) {
                return ((int) (((double) this.mIndexMax) * 0.7d)) / 10;
            }
            if (i == 3) {
                return ((int) (((double) this.mIndexMax) * 0.85d)) / 10;
            }
            return (this.mIndexMax + 5) / 10;
        }

        public void applyDeviceVolume_syncVSS(int i) {
            int index;
            if (this.mIsMuted) {
                index = 0;
            } else if ((i & 896) != 0 && AudioService.this.mAvrcpAbsVolSupported) {
                index = getAbsoluteVolumeIndex((getIndex(i) + 5) / 10);
            } else if ((AudioService.this.mFullVolumeDevices & i) != 0 || (134217728 & i) != 0) {
                index = (this.mIndexMax + 5) / 10;
            } else {
                index = (getIndex(i) + 5) / 10;
            }
            AudioSystem.setStreamVolumeIndex(this.mStreamType, index, i);
        }

        public void applyAllVolumes() {
            int iValueAt;
            synchronized (VolumeStreamState.class) {
                int index = 0;
                for (int i = 0; i < this.mIndexMap.size(); i++) {
                    int iKeyAt = this.mIndexMap.keyAt(i);
                    if (iKeyAt != 1073741824) {
                        if (!this.mIsMuted) {
                            if ((iKeyAt & 896) != 0 && AudioService.this.mAvrcpAbsVolSupported) {
                                iValueAt = getAbsoluteVolumeIndex((getIndex(iKeyAt) + 5) / 10);
                            } else if ((AudioService.this.mFullVolumeDevices & iKeyAt) != 0 || (134217728 & iKeyAt) != 0) {
                                iValueAt = (this.mIndexMax + 5) / 10;
                            } else {
                                iValueAt = (this.mIndexMap.valueAt(i) + 5) / 10;
                            }
                        } else {
                            iValueAt = 0;
                        }
                        AudioSystem.setStreamVolumeIndex(this.mStreamType, iValueAt, iKeyAt);
                    }
                }
                if (!this.mIsMuted) {
                    index = (getIndex(1073741824) + 5) / 10;
                }
                AudioSystem.setStreamVolumeIndex(this.mStreamType, index, 1073741824);
            }
        }

        public boolean adjustIndex(int i, int i2, String str) {
            return setIndex(getIndex(i2) + i, i2, str);
        }

        public boolean setIndex(int i, int i2, String str) {
            int index;
            int validIndex;
            boolean z;
            synchronized (AudioService.this.mSettingsLock) {
                synchronized (VolumeStreamState.class) {
                    index = getIndex(i2);
                    validIndex = getValidIndex(i);
                    if (this.mStreamType == 7 && AudioService.this.mCameraSoundForced) {
                        validIndex = this.mIndexMax;
                    }
                    this.mIndexMap.put(i2, validIndex);
                    z = index != validIndex;
                    boolean z2 = i2 == AudioService.this.getDeviceForStream(this.mStreamType);
                    for (int numStreamTypes = AudioSystem.getNumStreamTypes() - 1; numStreamTypes >= 0; numStreamTypes--) {
                        VolumeStreamState volumeStreamState = AudioService.this.mStreamStates[numStreamTypes];
                        if (numStreamTypes != this.mStreamType && AudioService.mStreamVolumeAlias[numStreamTypes] == this.mStreamType && (z || !volumeStreamState.hasIndexForDevice(i2))) {
                            int iRescaleIndex = AudioService.this.rescaleIndex(validIndex, this.mStreamType, numStreamTypes);
                            volumeStreamState.setIndex(iRescaleIndex, i2, str);
                            if (z2) {
                                volumeStreamState.setIndex(iRescaleIndex, AudioService.this.getDeviceForStream(numStreamTypes), str);
                            }
                        }
                    }
                    if (z && this.mStreamType == 2 && i2 == 2) {
                        for (int i3 = 0; i3 < this.mIndexMap.size(); i3++) {
                            int iKeyAt = this.mIndexMap.keyAt(i3);
                            if ((iKeyAt & 112) != 0) {
                                this.mIndexMap.put(iKeyAt, validIndex);
                            }
                        }
                    }
                }
            }
            if (z) {
                int i4 = (index + 5) / 10;
                int i5 = (validIndex + 5) / 10;
                if (AudioService.mStreamVolumeAlias[this.mStreamType] == this.mStreamType) {
                    if (str == null) {
                        Log.w(AudioService.TAG, "No caller for volume_changed event", new Throwable());
                    }
                    EventLogTags.writeVolumeChanged(this.mStreamType, i4, i5, this.mIndexMax / 10, str);
                }
                this.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", i5);
                this.mVolumeChanged.putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", i4);
                this.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE_ALIAS", AudioService.mStreamVolumeAlias[this.mStreamType]);
                AudioService.this.sendBroadcastToAll(this.mVolumeChanged);
            }
            return z;
        }

        public int getIndex(int i) {
            int i2;
            synchronized (VolumeStreamState.class) {
                i2 = this.mIndexMap.get(i, -1);
                if (i2 == -1) {
                    i2 = this.mIndexMap.get(1073741824);
                }
            }
            return i2;
        }

        public boolean hasIndexForDevice(int i) {
            boolean z;
            synchronized (VolumeStreamState.class) {
                z = this.mIndexMap.get(i, -1) != -1;
            }
            return z;
        }

        public int getMaxIndex() {
            return this.mIndexMax;
        }

        public int getMinIndex() {
            return this.mIndexMin;
        }

        @GuardedBy("VolumeStreamState.class")
        public void refreshRange(int i) {
            this.mIndexMin = AudioService.MIN_STREAM_VOLUME[i] * 10;
            this.mIndexMax = AudioService.MAX_STREAM_VOLUME[i] * 10;
            for (int i2 = 0; i2 < this.mIndexMap.size(); i2++) {
                this.mIndexMap.put(this.mIndexMap.keyAt(i2), getValidIndex(this.mIndexMap.valueAt(i2)));
            }
        }

        @GuardedBy("VolumeStreamState.class")
        public void setAllIndexes(VolumeStreamState volumeStreamState, String str) {
            if (this.mStreamType == volumeStreamState.mStreamType) {
                return;
            }
            int streamType = volumeStreamState.getStreamType();
            int iRescaleIndex = AudioService.this.rescaleIndex(volumeStreamState.getIndex(1073741824), streamType, this.mStreamType);
            for (int i = 0; i < this.mIndexMap.size(); i++) {
                this.mIndexMap.put(this.mIndexMap.keyAt(i), iRescaleIndex);
            }
            SparseIntArray sparseIntArray = volumeStreamState.mIndexMap;
            for (int i2 = 0; i2 < sparseIntArray.size(); i2++) {
                setIndex(AudioService.this.rescaleIndex(sparseIntArray.valueAt(i2), streamType, this.mStreamType), sparseIntArray.keyAt(i2), str);
            }
        }

        @GuardedBy("VolumeStreamState.class")
        public void setAllIndexesToMax() {
            for (int i = 0; i < this.mIndexMap.size(); i++) {
                this.mIndexMap.put(this.mIndexMap.keyAt(i), this.mIndexMax);
            }
        }

        public void mute(boolean z) {
            boolean z2;
            synchronized (VolumeStreamState.class) {
                if (z != this.mIsMuted) {
                    z2 = true;
                    this.mIsMuted = z;
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 10, 2, 0, 0, this, 0);
                } else {
                    z2 = false;
                }
            }
            if (z2) {
                Intent intent = new Intent("android.media.STREAM_MUTE_CHANGED_ACTION");
                intent.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", this.mStreamType);
                intent.putExtra("android.media.EXTRA_STREAM_VOLUME_MUTED", z);
                AudioService.this.sendBroadcastToAll(intent);
            }
        }

        public int getStreamType() {
            return this.mStreamType;
        }

        public void checkFixedVolumeDevices() {
            synchronized (VolumeStreamState.class) {
                if (AudioService.mStreamVolumeAlias[this.mStreamType] == 3) {
                    for (int i = 0; i < this.mIndexMap.size(); i++) {
                        int iKeyAt = this.mIndexMap.keyAt(i);
                        int iValueAt = this.mIndexMap.valueAt(i);
                        if ((AudioService.this.mFullVolumeDevices & iKeyAt) != 0 || ((AudioService.this.mFixedVolumeDevices & iKeyAt) != 0 && iValueAt != 0)) {
                            this.mIndexMap.put(iKeyAt, this.mIndexMax);
                        }
                        applyDeviceVolume_syncVSS(iKeyAt);
                    }
                }
            }
        }

        private int getValidIndex(int i) {
            if (i >= this.mIndexMin) {
                if (AudioService.this.mUseFixedVolume || i > this.mIndexMax) {
                    return this.mIndexMax;
                }
                return i;
            }
            return this.mIndexMin;
        }

        private void dump(PrintWriter printWriter) {
            printWriter.print("   Muted: ");
            printWriter.println(this.mIsMuted);
            printWriter.print("   Min: ");
            printWriter.println((this.mIndexMin + 5) / 10);
            printWriter.print("   Max: ");
            printWriter.println((this.mIndexMax + 5) / 10);
            printWriter.print("   Current: ");
            int i = 0;
            for (int i2 = 0; i2 < this.mIndexMap.size(); i2++) {
                if (i2 > 0) {
                    printWriter.print(", ");
                }
                int iKeyAt = this.mIndexMap.keyAt(i2);
                printWriter.print(Integer.toHexString(iKeyAt));
                String outputDeviceName = iKeyAt == 1073741824 ? BatteryService.HealthServiceWrapper.INSTANCE_VENDOR : AudioSystem.getOutputDeviceName(iKeyAt);
                if (!outputDeviceName.isEmpty()) {
                    printWriter.print(" (");
                    printWriter.print(outputDeviceName);
                    printWriter.print(")");
                }
                printWriter.print(": ");
                printWriter.print((this.mIndexMap.valueAt(i2) + 5) / 10);
            }
            printWriter.println();
            printWriter.print("   Devices: ");
            int devicesForStream = AudioService.this.getDevicesForStream(this.mStreamType);
            int i3 = 0;
            while (true) {
                int i4 = 1 << i;
                if (i4 != 1073741824) {
                    if ((devicesForStream & i4) != 0) {
                        int i5 = i3 + 1;
                        if (i3 > 0) {
                            printWriter.print(", ");
                        }
                        printWriter.print(AudioSystem.getOutputDeviceName(i4));
                        i3 = i5;
                    }
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    private class AudioSystemThread extends Thread {
        AudioSystemThread() {
            super(AudioService.TAG);
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (AudioService.this) {
                AudioService.this.mAudioHandler = new AudioHandler();
                AudioService.this.notify();
            }
            Looper.loop();
        }
    }

    private void setDeviceVolume(VolumeStreamState volumeStreamState, int i) {
        synchronized (VolumeStreamState.class) {
            volumeStreamState.applyDeviceVolume_syncVSS(i);
            for (int numStreamTypes = AudioSystem.getNumStreamTypes() - 1; numStreamTypes >= 0; numStreamTypes--) {
                if (numStreamTypes != volumeStreamState.mStreamType && mStreamVolumeAlias[numStreamTypes] == volumeStreamState.mStreamType) {
                    int deviceForStream = getDeviceForStream(numStreamTypes);
                    if (i != deviceForStream && this.mAvrcpAbsVolSupported && (i & 896) != 0) {
                        this.mStreamStates[numStreamTypes].applyDeviceVolume_syncVSS(i);
                    }
                    this.mStreamStates[numStreamTypes].applyDeviceVolume_syncVSS(deviceForStream);
                }
            }
        }
        sendMsg(this.mAudioHandler, 1, 2, i, 0, volumeStreamState, 500);
    }

    private class AudioHandler extends Handler {
        private AudioHandler() {
        }

        private void setAllVolumes(VolumeStreamState volumeStreamState) {
            volumeStreamState.applyAllVolumes();
            for (int numStreamTypes = AudioSystem.getNumStreamTypes() - 1; numStreamTypes >= 0; numStreamTypes--) {
                if (numStreamTypes != volumeStreamState.mStreamType && AudioService.mStreamVolumeAlias[numStreamTypes] == volumeStreamState.mStreamType) {
                    AudioService.this.mStreamStates[numStreamTypes].applyAllVolumes();
                }
            }
        }

        private void persistVolume(VolumeStreamState volumeStreamState, int i) {
            if (!AudioService.this.mUseFixedVolume) {
                if ((!AudioService.this.mIsSingleVolume || volumeStreamState.mStreamType == 3) && volumeStreamState.hasValidSettingsName()) {
                    Settings.System.putIntForUser(AudioService.this.mContentResolver, volumeStreamState.getSettingNameForDevice(i), (volumeStreamState.getIndex(i) + 5) / 10, -2);
                }
            }
        }

        private void persistRingerMode(int i) {
            if (!AudioService.this.mUseFixedVolume) {
                Settings.Global.putInt(AudioService.this.mContentResolver, "mode_ringer", i);
            }
        }

        private String getSoundEffectFilePath(int i) {
            String str = Environment.getProductDirectory() + AudioService.SOUND_EFFECTS_PATH + ((String) AudioService.SOUND_EFFECT_FILES.get(AudioService.this.SOUND_EFFECT_FILES_MAP[i][0]));
            if (!new File(str).isFile()) {
                return Environment.getRootDirectory() + AudioService.SOUND_EFFECTS_PATH + ((String) AudioService.SOUND_EFFECT_FILES.get(AudioService.this.SOUND_EFFECT_FILES_MAP[i][0]));
            }
            return str;
        }

        private boolean onLoadSoundEffects() {
            int iStatus;
            synchronized (AudioService.this.mSoundEffectsLock) {
                if (AudioService.this.mSystemReady) {
                    if (AudioService.this.mSoundPool != null) {
                        return true;
                    }
                    AudioService.this.loadTouchSoundAssets();
                    AudioService.this.mSoundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setContentType(4).build()).build();
                    AudioService.this.mSoundPoolCallBack = null;
                    AudioService.this.mSoundPoolListenerThread = AudioService.this.new SoundPoolListenerThread();
                    AudioService.this.mSoundPoolListenerThread.start();
                    int i = 3;
                    while (AudioService.this.mSoundPoolCallBack == null) {
                        int i2 = i - 1;
                        if (i <= 0) {
                            break;
                        }
                        try {
                            AudioService.this.mSoundEffectsLock.wait(5000L);
                        } catch (InterruptedException e) {
                            Log.w(AudioService.TAG, "Interrupted while waiting sound pool listener thread.");
                        }
                        i = i2;
                    }
                    if (AudioService.this.mSoundPoolCallBack != null) {
                        int[] iArr = new int[AudioService.SOUND_EFFECT_FILES.size()];
                        for (int i3 = 0; i3 < AudioService.SOUND_EFFECT_FILES.size(); i3++) {
                            iArr[i3] = -1;
                        }
                        int i4 = 0;
                        for (int i5 = 0; i5 < 10; i5++) {
                            if (AudioService.this.SOUND_EFFECT_FILES_MAP[i5][1] != 0) {
                                if (iArr[AudioService.this.SOUND_EFFECT_FILES_MAP[i5][0]] != -1) {
                                    AudioService.this.SOUND_EFFECT_FILES_MAP[i5][1] = iArr[AudioService.this.SOUND_EFFECT_FILES_MAP[i5][0]];
                                } else {
                                    String soundEffectFilePath = getSoundEffectFilePath(i5);
                                    int iLoad = AudioService.this.mSoundPool.load(soundEffectFilePath, 0);
                                    if (iLoad > 0) {
                                        AudioService.this.SOUND_EFFECT_FILES_MAP[i5][1] = iLoad;
                                        iArr[AudioService.this.SOUND_EFFECT_FILES_MAP[i5][0]] = iLoad;
                                        i4++;
                                    } else {
                                        Log.w(AudioService.TAG, "Soundpool could not load file: " + soundEffectFilePath);
                                    }
                                }
                            }
                        }
                        if (i4 > 0) {
                            AudioService.this.mSoundPoolCallBack.setSamples(iArr);
                            int i6 = 3;
                            iStatus = 1;
                            while (iStatus == 1) {
                                int i7 = i6 - 1;
                                if (i6 <= 0) {
                                    break;
                                }
                                try {
                                    AudioService.this.mSoundEffectsLock.wait(5000L);
                                    iStatus = AudioService.this.mSoundPoolCallBack.status();
                                } catch (InterruptedException e2) {
                                    Log.w(AudioService.TAG, "Interrupted while waiting sound pool callback.");
                                }
                                i6 = i7;
                            }
                        } else {
                            iStatus = -1;
                        }
                        if (AudioService.this.mSoundPoolLooper != null) {
                            AudioService.this.mSoundPoolLooper.quit();
                            AudioService.this.mSoundPoolLooper = null;
                        }
                        AudioService.this.mSoundPoolListenerThread = null;
                        if (iStatus != 0) {
                            Log.w(AudioService.TAG, "onLoadSoundEffects(), Error " + iStatus + " while loading samples");
                            for (int i8 = 0; i8 < 10; i8++) {
                                if (AudioService.this.SOUND_EFFECT_FILES_MAP[i8][1] > 0) {
                                    AudioService.this.SOUND_EFFECT_FILES_MAP[i8][1] = -1;
                                }
                            }
                            AudioService.this.mSoundPool.release();
                            AudioService.this.mSoundPool = null;
                        }
                        return iStatus == 0;
                    }
                    Log.w(AudioService.TAG, "onLoadSoundEffects() SoundPool listener or thread creation error");
                    if (AudioService.this.mSoundPoolLooper != null) {
                        AudioService.this.mSoundPoolLooper.quit();
                        AudioService.this.mSoundPoolLooper = null;
                    }
                    AudioService.this.mSoundPoolListenerThread = null;
                    AudioService.this.mSoundPool.release();
                    AudioService.this.mSoundPool = null;
                    return false;
                }
                Log.w(AudioService.TAG, "onLoadSoundEffects() called before boot complete");
                return false;
            }
        }

        private void onUnloadSoundEffects() {
            synchronized (AudioService.this.mSoundEffectsLock) {
                if (AudioService.this.mSoundPool == null) {
                    return;
                }
                int[] iArr = new int[AudioService.SOUND_EFFECT_FILES.size()];
                for (int i = 0; i < AudioService.SOUND_EFFECT_FILES.size(); i++) {
                    iArr[i] = 0;
                }
                for (int i2 = 0; i2 < 10; i2++) {
                    if (AudioService.this.SOUND_EFFECT_FILES_MAP[i2][1] > 0 && iArr[AudioService.this.SOUND_EFFECT_FILES_MAP[i2][0]] == 0) {
                        AudioService.this.mSoundPool.unload(AudioService.this.SOUND_EFFECT_FILES_MAP[i2][1]);
                        AudioService.this.SOUND_EFFECT_FILES_MAP[i2][1] = -1;
                        iArr[AudioService.this.SOUND_EFFECT_FILES_MAP[i2][0]] = -1;
                    }
                }
                AudioService.this.mSoundPool.release();
                AudioService.this.mSoundPool = null;
            }
        }

        private void onPlaySoundEffect(int i, int i2) {
            float fPow;
            synchronized (AudioService.this.mSoundEffectsLock) {
                onLoadSoundEffects();
                if (AudioService.this.mSoundPool == null) {
                    return;
                }
                if (i2 < 0) {
                    fPow = (float) Math.pow(10.0d, AudioService.sSoundEffectVolumeDb / 20.0f);
                } else {
                    fPow = i2 / 1000.0f;
                }
                float f = fPow;
                if (AudioService.this.SOUND_EFFECT_FILES_MAP[i][1] > 0) {
                    AudioService.this.mSoundPool.play(AudioService.this.SOUND_EFFECT_FILES_MAP[i][1], f, f, 0, 0, 1.0f);
                } else {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    try {
                        try {
                            mediaPlayer.setDataSource(getSoundEffectFilePath(i));
                            mediaPlayer.setAudioStreamType(1);
                            mediaPlayer.prepare();
                            mediaPlayer.setVolume(f);
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer2) {
                                    AudioHandler.this.cleanupPlayer(mediaPlayer2);
                                }
                            });
                            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                @Override
                                public boolean onError(MediaPlayer mediaPlayer2, int i3, int i4) {
                                    AudioHandler.this.cleanupPlayer(mediaPlayer2);
                                    return true;
                                }
                            });
                            mediaPlayer.start();
                        } catch (IOException e) {
                            Log.w(AudioService.TAG, "MediaPlayer IOException: " + e);
                        }
                    } catch (IllegalArgumentException e2) {
                        Log.w(AudioService.TAG, "MediaPlayer IllegalArgumentException: " + e2);
                    } catch (IllegalStateException e3) {
                        Log.w(AudioService.TAG, "MediaPlayer IllegalStateException: " + e3);
                    }
                }
            }
        }

        private void cleanupPlayer(MediaPlayer mediaPlayer) {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                } catch (IllegalStateException e) {
                    Log.w(AudioService.TAG, "MediaPlayer IllegalStateException: " + e);
                }
            }
        }

        private void setForceUse(int i, int i2, String str) {
            synchronized (AudioService.this.mConnectedDevices) {
                AudioService.this.setForceUseInt_SyncDevices(i, i2, str);
            }
        }

        private void onPersistSafeVolumeState(int i) {
            Settings.Global.putInt(AudioService.this.mContentResolver, "audio_safe_volume_state", i);
        }

        private void onNotifyVolumeEvent(IAudioPolicyCallback iAudioPolicyCallback, int i) {
            try {
                iAudioPolicyCallback.notifyVolumeAdjust(i);
            } catch (Exception e) {
            }
        }

        @Override
        public void handleMessage(Message message) {
            AudioRoutesInfo audioRoutesInfo;
            int i = message.what;
            switch (i) {
                case 0:
                    AudioService.this.setDeviceVolume((VolumeStreamState) message.obj, message.arg1);
                    return;
                case 1:
                    persistVolume((VolumeStreamState) message.obj, message.arg1);
                    return;
                default:
                    switch (i) {
                        case 3:
                            persistRingerMode(AudioService.this.getRingerModeInternal());
                            return;
                        case 4:
                            AudioService.this.onAudioServerDied();
                            return;
                        case 5:
                            onPlaySoundEffect(message.arg1, message.arg2);
                            return;
                        default:
                            switch (i) {
                                case 7:
                                    boolean zOnLoadSoundEffects = onLoadSoundEffects();
                                    if (message.obj != null) {
                                        LoadSoundEffectReply loadSoundEffectReply = (LoadSoundEffectReply) message.obj;
                                        synchronized (loadSoundEffectReply) {
                                            loadSoundEffectReply.mStatus = zOnLoadSoundEffects ? 0 : -1;
                                            loadSoundEffectReply.notify();
                                            break;
                                        }
                                        return;
                                    }
                                    return;
                                case 8:
                                    break;
                                case 9:
                                    AudioService.this.resetBluetoothSco();
                                    return;
                                case 10:
                                    setAllVolumes((VolumeStreamState) message.obj);
                                    return;
                                default:
                                    switch (i) {
                                        case 12:
                                            int iBeginBroadcast = AudioService.this.mRoutesObservers.beginBroadcast();
                                            if (iBeginBroadcast > 0) {
                                                synchronized (AudioService.this.mCurAudioRoutes) {
                                                    audioRoutesInfo = new AudioRoutesInfo(AudioService.this.mCurAudioRoutes);
                                                    break;
                                                }
                                                while (iBeginBroadcast > 0) {
                                                    iBeginBroadcast--;
                                                    try {
                                                        AudioService.this.mRoutesObservers.getBroadcastItem(iBeginBroadcast).dispatchAudioRoutesChanged(audioRoutesInfo);
                                                    } catch (RemoteException e) {
                                                    }
                                                }
                                            }
                                            AudioService.this.mRoutesObservers.finishBroadcast();
                                            AudioService.this.observeDevicesForStreams(-1);
                                            return;
                                        case 13:
                                            break;
                                        case 14:
                                            AudioService.this.onCheckMusicActive((String) message.obj);
                                            return;
                                        case 15:
                                            AudioService.this.onSendBecomingNoisyIntent();
                                            return;
                                        case 16:
                                        case 17:
                                            AudioService.this.onConfigureSafeVolume(message.what == 17, (String) message.obj);
                                            return;
                                        case 18:
                                            onPersistSafeVolumeState(message.arg1);
                                            return;
                                        case 19:
                                            AudioService.this.onBroadcastScoConnectionState(message.arg1);
                                            return;
                                        case 20:
                                            onUnloadSoundEffects();
                                            return;
                                        case 21:
                                            AudioService.this.onSystemReady();
                                            return;
                                        case 22:
                                            Settings.Secure.putIntForUser(AudioService.this.mContentResolver, "unsafe_volume_music_active_ms", message.arg1, -2);
                                            return;
                                        default:
                                            switch (i) {
                                                case 24:
                                                    AudioService.this.onUnmuteStream(message.arg1, message.arg2);
                                                    return;
                                                case 25:
                                                    AudioService.this.onDynPolicyMixStateUpdate((String) message.obj, message.arg1);
                                                    return;
                                                case 26:
                                                    AudioService.this.onIndicateSystemReady();
                                                    return;
                                                case AudioService.MSG_ACCESSORY_PLUG_MEDIA_UNMUTE:
                                                    AudioService.this.onAccessoryPlugMediaUnmute(message.arg1);
                                                    return;
                                                case 28:
                                                    onNotifyVolumeEvent((IAudioPolicyCallback) message.obj, message.arg1);
                                                    return;
                                                case 29:
                                                    AudioService.this.onDispatchAudioServerStateChange(message.arg1 == 1);
                                                    return;
                                                case 30:
                                                    AudioService.this.onEnableSurroundFormats((ArrayList) message.obj);
                                                    return;
                                                default:
                                                    switch (i) {
                                                        case 100:
                                                            WiredDeviceConnectionState wiredDeviceConnectionState = (WiredDeviceConnectionState) message.obj;
                                                            AudioService.this.mWiredDevLogger.log(new AudioServiceEvents.WiredDevConnectEvent(wiredDeviceConnectionState));
                                                            AudioService.this.onSetWiredDeviceConnectionState(wiredDeviceConnectionState.mType, wiredDeviceConnectionState.mState, wiredDeviceConnectionState.mAddress, wiredDeviceConnectionState.mName, wiredDeviceConnectionState.mCaller);
                                                            AudioService.this.mAudioEventWakeLock.release();
                                                            return;
                                                        case 101:
                                                            AudioService.this.onSetA2dpSourceConnectionState((BluetoothDevice) message.obj, message.arg1);
                                                            AudioService.this.mAudioEventWakeLock.release();
                                                            return;
                                                        case 102:
                                                            AudioService.this.onSetA2dpSinkConnectionState((BluetoothDevice) message.obj, message.arg1, message.arg2);
                                                            AudioService.this.mAudioEventWakeLock.release();
                                                            return;
                                                        case 103:
                                                            AudioService.this.onBluetoothA2dpDeviceConfigChange((BluetoothDevice) message.obj);
                                                            AudioService.this.mAudioEventWakeLock.release();
                                                            return;
                                                        case 104:
                                                            AudioService.this.mPlaybackMonitor.disableAudioForUid(message.arg1 == 1, message.arg2);
                                                            AudioService.this.mAudioEventWakeLock.release();
                                                            return;
                                                        case 105:
                                                            AudioService.this.onSetHearingAidConnectionState((BluetoothDevice) message.obj, message.arg1);
                                                            AudioService.this.mAudioEventWakeLock.release();
                                                            return;
                                                        case 106:
                                                            synchronized (AudioService.this.mConnectedDevices) {
                                                                AudioService.this.makeA2dpDeviceUnavailableNow((String) message.obj);
                                                                break;
                                                            }
                                                            AudioService.this.mAudioEventWakeLock.release();
                                                            return;
                                                        default:
                                                            return;
                                                    }
                                            }
                                    }
                                    break;
                            }
                            setForceUse(message.arg1, message.arg2, (String) message.obj);
                            return;
                    }
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(new Handler());
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("zen_mode"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("zen_mode_config_etag"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("mode_ringer_streams_affected"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("dock_audio_media_enabled"), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("master_mono"), false, this);
            AudioService.this.mEncodedSurroundMode = Settings.Global.getInt(AudioService.this.mContentResolver, "encoded_surround_output", 0);
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("encoded_surround_output"), false, this);
            AudioService.this.mEnabledSurroundFormats = Settings.Global.getString(AudioService.this.mContentResolver, "encoded_surround_output_enabled_formats");
            AudioService.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("encoded_surround_output_enabled_formats"), false, this);
        }

        @Override
        public void onChange(boolean z) {
            super.onChange(z);
            synchronized (AudioService.this.mSettingsLock) {
                if (AudioService.this.updateRingerAndZenModeAffectedStreams()) {
                    AudioService.this.setRingerModeInt(AudioService.this.getRingerModeInternal(), false);
                }
                AudioService.this.readDockAudioSettings(AudioService.this.mContentResolver);
                AudioService.this.updateMasterMono(AudioService.this.mContentResolver);
                updateEncodedSurroundOutput();
                AudioService.this.sendEnabledSurroundFormats(AudioService.this.mContentResolver, AudioService.this.mSurroundModeChanged);
            }
        }

        private void updateEncodedSurroundOutput() {
            int i = Settings.Global.getInt(AudioService.this.mContentResolver, "encoded_surround_output", 0);
            if (AudioService.this.mEncodedSurroundMode != i) {
                AudioService.this.sendEncodedSurroundMode(i, "SettingsObserver");
                synchronized (AudioService.this.mConnectedDevices) {
                    if (((DeviceListSpec) AudioService.this.mConnectedDevices.get(AudioService.this.makeDeviceListKey(1024, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS))) != null) {
                        AudioService.this.setWiredDeviceConnectionState(1024, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, PackageManagerService.PLATFORM_PACKAGE_NAME);
                        AudioService.this.setWiredDeviceConnectionState(1024, 1, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, PackageManagerService.PLATFORM_PACKAGE_NAME);
                    }
                }
                AudioService.this.mEncodedSurroundMode = i;
                AudioService.this.mSurroundModeChanged = true;
                return;
            }
            AudioService.this.mSurroundModeChanged = false;
        }
    }

    private void makeA2dpDeviceAvailable(String str, String str2, String str3) {
        VolumeStreamState volumeStreamState = this.mStreamStates[3];
        setBluetoothA2dpOnInt(true, str3);
        AudioSystem.setDeviceConnectionState(128, 1, str, str2);
        AudioSystem.setParameters("A2dpSuspended=false");
        this.mConnectedDevices.put(makeDeviceListKey(128, str), new DeviceListSpec(128, str2, str));
        sendMsg(this.mAudioHandler, MSG_ACCESSORY_PLUG_MEDIA_UNMUTE, 2, 128, 0, null, 0);
    }

    private void onSendBecomingNoisyIntent() {
        sendBroadcastToAll(new Intent("android.media.AUDIO_BECOMING_NOISY"));
    }

    private void makeA2dpDeviceUnavailableNow(String str) {
        if (str == null) {
            return;
        }
        synchronized (this.mA2dpAvrcpLock) {
            this.mAvrcpAbsVolSupported = false;
        }
        AudioSystem.setDeviceConnectionState(128, 0, str, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mConnectedDevices.remove(makeDeviceListKey(128, str));
        setCurrentAudioRouteName(null);
        if (this.mDockAddress == str) {
            this.mDockAddress = null;
        }
    }

    private void makeA2dpDeviceUnavailableLater(String str, int i) {
        AudioSystem.setParameters("A2dpSuspended=true");
        this.mConnectedDevices.remove(makeDeviceListKey(128, str));
        queueMsgUnderWakeLock(this.mAudioHandler, 106, 0, 0, str, i);
    }

    private void makeA2dpSrcAvailable(String str) {
        AudioSystem.setDeviceConnectionState(-2147352576, 1, str, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mConnectedDevices.put(makeDeviceListKey(-2147352576, str), new DeviceListSpec(-2147352576, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str));
    }

    private void makeA2dpSrcUnavailable(String str) {
        AudioSystem.setDeviceConnectionState(-2147352576, 0, str, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mConnectedDevices.remove(makeDeviceListKey(-2147352576, str));
    }

    private void setHearingAidVolume(int i, int i2) {
        synchronized (this.mHearingAidLock) {
            if (this.mHearingAid != null) {
                int streamVolumeDB = (int) AudioSystem.getStreamVolumeDB(i2, i / 10, 134217728);
                if (streamVolumeDB < -128) {
                    streamVolumeDB = -128;
                }
                this.mHearingAid.setVolume(streamVolumeDB);
            }
        }
    }

    private void makeHearingAidDeviceAvailable(String str, String str2, String str3) {
        setHearingAidVolume(this.mStreamStates[3].getIndex(134217728), 3);
        AudioSystem.setDeviceConnectionState(134217728, 1, str, str2);
        this.mConnectedDevices.put(makeDeviceListKey(134217728, str), new DeviceListSpec(134217728, str2, str));
        sendMsg(this.mAudioHandler, MSG_ACCESSORY_PLUG_MEDIA_UNMUTE, 2, 134217728, 0, null, 0);
    }

    private void makeHearingAidDeviceUnavailable(String str) {
        AudioSystem.setDeviceConnectionState(134217728, 0, str, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mConnectedDevices.remove(makeDeviceListKey(134217728, str));
        setCurrentAudioRouteName(null);
    }

    private void cancelA2dpDeviceTimeout() {
        this.mAudioHandler.removeMessages(106);
    }

    private boolean hasScheduledA2dpDockTimeout() {
        return this.mAudioHandler.hasMessages(106);
    }

    private void onSetA2dpSinkConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (DEBUG_DEVICES) {
            Log.d(TAG, "onSetA2dpSinkConnectionState btDevice= " + bluetoothDevice + " state= " + i + " is dock: " + bluetoothDevice.isBluetoothDock());
        }
        if (bluetoothDevice == null) {
            return;
        }
        String address = bluetoothDevice.getAddress();
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            address = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        synchronized (this.mConnectedDevices) {
            boolean z = this.mConnectedDevices.get(makeDeviceListKey(128, bluetoothDevice.getAddress())) != null;
            if (z && i != 2) {
                if (bluetoothDevice.isBluetoothDock()) {
                    if (i == 0) {
                        makeA2dpDeviceUnavailableLater(address, 8000);
                    }
                } else {
                    makeA2dpDeviceUnavailableNow(address);
                }
                setCurrentAudioRouteName(null);
            } else if (!z && i == 2) {
                if (bluetoothDevice.isBluetoothDock()) {
                    cancelA2dpDeviceTimeout();
                    this.mDockAddress = address;
                } else if (hasScheduledA2dpDockTimeout() && this.mDockAddress != null) {
                    cancelA2dpDeviceTimeout();
                    makeA2dpDeviceUnavailableNow(this.mDockAddress);
                }
                if (i2 != -1) {
                    VolumeStreamState volumeStreamState = this.mStreamStates[3];
                    volumeStreamState.setIndex(i2 * 10, 128, "onSetA2dpSinkConnectionState");
                    setDeviceVolume(volumeStreamState, 128);
                }
                makeA2dpDeviceAvailable(address, bluetoothDevice.getName(), "onSetA2dpSinkConnectionState");
                setCurrentAudioRouteName(bluetoothDevice.getAliasName());
            }
        }
    }

    private void onSetA2dpSourceConnectionState(BluetoothDevice bluetoothDevice, int i) {
        if (DEBUG_VOL) {
            Log.d(TAG, "onSetA2dpSourceConnectionState btDevice=" + bluetoothDevice + " state=" + i);
        }
        if (bluetoothDevice == null) {
            return;
        }
        String address = bluetoothDevice.getAddress();
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            address = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        synchronized (this.mConnectedDevices) {
            boolean z = this.mConnectedDevices.get(makeDeviceListKey(-2147352576, address)) != null;
            if (z && i != 2) {
                makeA2dpSrcUnavailable(address);
            } else if (!z && i == 2) {
                makeA2dpSrcAvailable(address);
            }
        }
    }

    private void onSetHearingAidConnectionState(BluetoothDevice bluetoothDevice, int i) {
        if (DEBUG_DEVICES) {
            Log.d(TAG, "onSetHearingAidConnectionState btDevice=" + bluetoothDevice + ", state=" + i);
        }
        if (bluetoothDevice == null) {
            return;
        }
        String address = bluetoothDevice.getAddress();
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            address = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        synchronized (this.mConnectedDevices) {
            boolean z = this.mConnectedDevices.get(makeDeviceListKey(134217728, bluetoothDevice.getAddress())) != null;
            if (z && i != 2) {
                makeHearingAidDeviceUnavailable(address);
                setCurrentAudioRouteName(null);
            } else if (!z && i == 2) {
                makeHearingAidDeviceAvailable(address, bluetoothDevice.getName(), "onSetHearingAidConnectionState");
                setCurrentAudioRouteName(bluetoothDevice.getAliasName());
            }
        }
    }

    private void setCurrentAudioRouteName(String str) {
        synchronized (this.mCurAudioRoutes) {
            if (!TextUtils.equals(this.mCurAudioRoutes.bluetoothName, str)) {
                this.mCurAudioRoutes.bluetoothName = str;
                sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
            }
        }
    }

    private void onBluetoothA2dpDeviceConfigChange(BluetoothDevice bluetoothDevice) {
        if (DEBUG_DEVICES) {
            Log.d(TAG, "onBluetoothA2dpDeviceConfigChange btDevice=" + bluetoothDevice);
        }
        if (bluetoothDevice == null) {
            return;
        }
        String address = bluetoothDevice.getAddress();
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            address = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        synchronized (this.mConnectedDevices) {
            if (this.mAudioHandler.hasMessages(102, bluetoothDevice)) {
                return;
            }
            if (this.mConnectedDevices.get(makeDeviceListKey(128, address)) != null) {
                int deviceForStream = getDeviceForStream(3);
                if (AudioSystem.handleDeviceConfigChange(128, address, bluetoothDevice.getName()) != 0) {
                    setBluetoothA2dpDeviceConnectionStateInt(bluetoothDevice, 0, 2, false, deviceForStream, -1);
                }
            }
        }
    }

    public void avrcpSupportsAbsoluteVolume(String str, boolean z) {
        synchronized (this.mA2dpAvrcpLock) {
            this.mAvrcpAbsVolSupported = z;
            sendMsg(this.mAudioHandler, 0, 2, 128, 0, this.mStreamStates[3], 0);
        }
    }

    private boolean handleDeviceConnection(boolean z, int i, String str, String str2) {
        if (DEBUG_DEVICES) {
            Slog.i(TAG, "handleDeviceConnection(" + z + " dev:" + Integer.toHexString(i) + " address:" + str + " name:" + str2 + ")");
        }
        synchronized (this.mConnectedDevices) {
            String strMakeDeviceListKey = makeDeviceListKey(i, str);
            if (DEBUG_DEVICES) {
                Slog.i(TAG, "deviceKey:" + strMakeDeviceListKey);
            }
            DeviceListSpec deviceListSpec = this.mConnectedDevices.get(strMakeDeviceListKey);
            boolean z2 = deviceListSpec != null;
            if (DEBUG_DEVICES) {
                Slog.i(TAG, "deviceSpec:" + deviceListSpec + " is(already)Connected:" + z2);
            }
            if (z && !z2) {
                int deviceConnectionState = AudioSystem.setDeviceConnectionState(i, 1, str, str2);
                if (deviceConnectionState == 0) {
                    this.mConnectedDevices.put(strMakeDeviceListKey, new DeviceListSpec(i, str2, str));
                    sendMsg(this.mAudioHandler, MSG_ACCESSORY_PLUG_MEDIA_UNMUTE, 2, i, 0, null, 0);
                    return true;
                }
                Slog.e(TAG, "not connecting device 0x" + Integer.toHexString(i) + " due to command error " + deviceConnectionState);
                return false;
            }
            if (!z && z2) {
                AudioSystem.setDeviceConnectionState(i, 0, str, str2);
                this.mConnectedDevices.remove(strMakeDeviceListKey);
                return true;
            }
            Log.w(TAG, "handleDeviceConnection() failed, deviceKey=" + strMakeDeviceListKey + ", deviceSpec=" + deviceListSpec + ", connect=" + z);
            return false;
        }
    }

    private int checkSendBecomingNoisyIntent(int i, int i2, int i3) {
        if (i2 != 0 || (this.mBecomingNoisyIntentDevices & i) == 0) {
            return 0;
        }
        int i4 = 0;
        for (int i5 = 0; i5 < this.mConnectedDevices.size(); i5++) {
            int i6 = this.mConnectedDevices.valueAt(i5).mDeviceType;
            if ((Integer.MIN_VALUE & i6) == 0 && (this.mBecomingNoisyIntentDevices & i6) != 0) {
                i4 |= i6;
            }
        }
        if (i3 == 0) {
            i3 = getDeviceForStream(3);
        }
        if ((i != i3 && !isInCommunication()) || i != i4 || hasMediaDynamicPolicy()) {
            return 0;
        }
        this.mAudioHandler.removeMessages(15);
        sendMsg(this.mAudioHandler, 15, 0, 0, 0, null, 0);
        return 1000;
    }

    private boolean hasMediaDynamicPolicy() {
        synchronized (this.mAudioPolicies) {
            if (this.mAudioPolicies.isEmpty()) {
                return false;
            }
            Iterator<AudioPolicyProxy> it = this.mAudioPolicies.values().iterator();
            while (it.hasNext()) {
                if (it.next().hasMixAffectingUsage(1)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void updateAudioRoutes(int i, int i2) {
        int i3;
        int i4 = 8;
        if (i == 4) {
            i4 = 1;
        } else if (i == 8 || i == 131072) {
            i4 = 2;
        } else if (i != 1024 && i != 262144) {
            i4 = (i == 16384 || i == 67108864) ? 16 : 0;
        }
        synchronized (this.mCurAudioRoutes) {
            if (i4 != 0) {
                try {
                    int i5 = this.mCurAudioRoutes.mainType;
                    if (i2 != 0) {
                        i3 = i5 | i4;
                    } else {
                        i3 = (~i4) & i5;
                    }
                    if (i3 != this.mCurAudioRoutes.mainType) {
                        this.mCurAudioRoutes.mainType = i3;
                        sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    private void sendDeviceConnectionIntent(int i, int i2, String str, String str2) {
        if (DEBUG_DEVICES) {
            Slog.i(TAG, "sendDeviceConnectionIntent(dev:0x" + Integer.toHexString(i) + " state:0x" + Integer.toHexString(i2) + " address:" + str + " name:" + str2 + ");");
        }
        Intent intent = new Intent();
        if (i == 4) {
            intent.setAction("android.intent.action.HEADSET_PLUG");
            intent.putExtra("microphone", 1);
        } else {
            int i3 = 0;
            if (i == 8 || i == 131072) {
                intent.setAction("android.intent.action.HEADSET_PLUG");
                intent.putExtra("microphone", 0);
            } else if (i == 67108864) {
                intent.setAction("android.intent.action.HEADSET_PLUG");
                if (AudioSystem.getDeviceConnectionState(-2113929216, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) == 1) {
                    i3 = 1;
                }
                intent.putExtra("microphone", i3);
            } else if (i == -2113929216) {
                if (AudioSystem.getDeviceConnectionState(67108864, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) == 1) {
                    intent.setAction("android.intent.action.HEADSET_PLUG");
                    intent.putExtra("microphone", 1);
                } else {
                    return;
                }
            } else if (i == 1024 || i == 262144) {
                configureHdmiPlugIntent(intent, i2);
            }
        }
        if (intent.getAction() == null) {
            return;
        }
        if (i2 == 0 && intent.getAction() == "android.intent.action.HEADSET_PLUG" && (AudioSystem.getDeviceConnectionState(67108864, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) == 1 || AudioSystem.getDeviceConnectionState(8, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) == 1 || AudioSystem.getDeviceConnectionState(4, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) == 1)) {
            return;
        }
        intent.putExtra(CONNECT_INTENT_KEY_STATE, i2);
        intent.putExtra(CONNECT_INTENT_KEY_ADDRESS, str);
        intent.putExtra(CONNECT_INTENT_KEY_PORT_NAME, str2);
        intent.addFlags(1073741824);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ActivityManager.broadcastStickyIntent(intent, -1);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void onSetWiredDeviceConnectionState(int i, int i2, String str, String str2, String str3) {
        String str4;
        boolean z;
        if (DEBUG_DEVICES) {
            StringBuilder sb = new StringBuilder();
            sb.append("onSetWiredDeviceConnectionState(dev:");
            sb.append(Integer.toHexString(i));
            sb.append(" state:");
            sb.append(Integer.toHexString(i2));
            sb.append(" address:");
            sb.append(str);
            sb.append(" deviceName:");
            sb.append(str2);
            sb.append(" caller: ");
            str4 = str3;
            sb.append(str4);
            sb.append(");");
            Slog.i(TAG, sb.toString());
        } else {
            str4 = str3;
        }
        synchronized (this.mConnectedDevices) {
            if (i2 == 0 && (i & DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG) != 0) {
                try {
                    setBluetoothA2dpOnInt(true, "onSetWiredDeviceConnectionState state 0");
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (handleDeviceConnection(i2 == 1, i, str, str2)) {
                if (i2 != 0) {
                    if ((DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG & i) != 0) {
                        setBluetoothA2dpOnInt(false, "onSetWiredDeviceConnectionState state not 0");
                    }
                    if ((67108876 & i) != 0) {
                        String str5 = str4;
                        z = false;
                        sendMsg(this.mAudioHandler, 14, 0, 0, 0, str5, MUSIC_ACTIVE_POLL_PERIOD_MS);
                    } else {
                        z = false;
                    }
                    if (isPlatformTelevision() && (i & 1024) != 0) {
                        this.mFixedVolumeDevices |= 1024;
                        checkAllFixedVolumeDevices();
                        if (this.mHdmiManager != null) {
                            synchronized (this.mHdmiManager) {
                                if (this.mHdmiPlaybackClient != null) {
                                    this.mHdmiCecSink = z;
                                    this.mHdmiPlaybackClient.queryDisplayStatus(this.mHdmiDisplayStatusCallback);
                                }
                            }
                        }
                    }
                    if ((i & 1024) != 0) {
                        sendEnabledSurroundFormats(this.mContentResolver, true);
                    }
                } else if (isPlatformTelevision() && (i & 1024) != 0 && this.mHdmiManager != null) {
                    synchronized (this.mHdmiManager) {
                        this.mHdmiCecSink = false;
                    }
                }
                sendDeviceConnectionIntent(i, i2, str, str2);
                updateAudioRoutes(i, i2);
            }
        }
    }

    private void configureHdmiPlugIntent(Intent intent, int i) {
        intent.setAction("android.media.action.HDMI_AUDIO_PLUG");
        intent.putExtra("android.media.extra.AUDIO_PLUG_STATE", i);
        if (i == 1) {
            ArrayList<AudioDevicePort> arrayList = new ArrayList();
            if (AudioSystem.listAudioPorts(arrayList, new int[1]) == 0) {
                for (AudioDevicePort audioDevicePort : arrayList) {
                    if (audioDevicePort instanceof AudioDevicePort) {
                        AudioDevicePort audioDevicePort2 = audioDevicePort;
                        if (audioDevicePort2.type() == 1024 || audioDevicePort2.type() == 262144) {
                            int[] iArrFilterPublicFormats = AudioFormat.filterPublicFormats(audioDevicePort2.formats());
                            if (iArrFilterPublicFormats.length > 0) {
                                ArrayList arrayList2 = new ArrayList(1);
                                for (int i2 : iArrFilterPublicFormats) {
                                    if (i2 != 0) {
                                        arrayList2.add(Integer.valueOf(i2));
                                    }
                                }
                                int[] iArr = new int[arrayList2.size()];
                                for (int i3 = 0; i3 < iArr.length; i3++) {
                                    iArr[i3] = ((Integer) arrayList2.get(i3)).intValue();
                                }
                                intent.putExtra("android.media.extra.ENCODINGS", iArr);
                            }
                            int i4 = 0;
                            for (int i5 : audioDevicePort2.channelMasks()) {
                                int iChannelCountFromOutChannelMask = AudioFormat.channelCountFromOutChannelMask(i5);
                                if (iChannelCountFromOutChannelMask > i4) {
                                    i4 = iChannelCountFromOutChannelMask;
                                }
                            }
                            intent.putExtra("android.media.extra.MAX_CHANNEL_COUNT", i4);
                        }
                    }
                }
            }
        }
    }

    private class AudioServiceBroadcastReceiver extends BroadcastReceiver {
        private AudioServiceBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean z;
            String action = intent.getAction();
            int i = 0;
            if (action.equals("android.intent.action.DOCK_EVENT")) {
                int intExtra = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                switch (intExtra) {
                    case 1:
                        i = 7;
                        break;
                    case 2:
                        i = 6;
                        break;
                    case 3:
                        i = 8;
                        break;
                    case 4:
                        i = 9;
                        break;
                }
                if (intExtra != 3 && (intExtra != 0 || AudioService.this.mDockState != 3)) {
                    AudioService.this.mForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(3, i, "ACTION_DOCK_EVENT intent"));
                    AudioSystem.setForceUse(3, i);
                }
                AudioService.this.mDockState = intExtra;
                return;
            }
            if (action.equals("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED")) {
                AudioService.this.setBtScoActiveDevice((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE"));
                return;
            }
            boolean z2 = true;
            if (action.equals("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED")) {
                synchronized (AudioService.this.mScoClients) {
                    int intExtra2 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                    z = !AudioService.this.mScoClients.isEmpty() && (AudioService.this.mScoAudioState == 3 || AudioService.this.mScoAudioState == 1 || AudioService.this.mScoAudioState == 4 || AudioService.this.mScoAudioState == 5);
                    switch (intExtra2) {
                        case 10:
                            AudioService.this.setBluetoothScoOn(false);
                            if (AudioService.this.mScoAudioState == 1 && AudioService.this.mBluetoothHeadset != null && AudioService.this.mBluetoothHeadsetDevice != null && AudioService.connectBluetoothScoAudioHelper(AudioService.this.mBluetoothHeadset, AudioService.this.mBluetoothHeadsetDevice, AudioService.this.mScoAudioMode)) {
                                AudioService.this.mScoAudioState = 3;
                                z = false;
                            } else {
                                AudioService audioService = AudioService.this;
                                if (AudioService.this.mScoAudioState != 3) {
                                    z2 = false;
                                }
                                audioService.clearAllScoClients(0, z2);
                                AudioService.this.mScoAudioState = 0;
                            }
                            break;
                        case 11:
                            if (AudioService.this.mScoAudioState != 3 && AudioService.this.mScoAudioState != 4) {
                                AudioService.this.mScoAudioState = 2;
                            }
                            z = false;
                            i = -1;
                            break;
                        case 12:
                            if (AudioService.this.mScoAudioState != 3 && AudioService.this.mScoAudioState != 4) {
                                AudioService.this.mScoAudioState = 2;
                            }
                            AudioService.this.setBluetoothScoOn(true);
                            i = 1;
                            break;
                        default:
                            z = false;
                            i = -1;
                            break;
                    }
                }
                if (z) {
                    AudioService.this.broadcastScoConnectionState(i);
                    Intent intent2 = new Intent("android.media.SCO_AUDIO_STATE_CHANGED");
                    intent2.putExtra("android.media.extra.SCO_AUDIO_STATE", i);
                    AudioService.this.sendStickyBroadcastToAll(intent2);
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.SCREEN_ON")) {
                if (AudioService.this.mMonitorRotation) {
                    RotationHelper.enable();
                }
                AudioSystem.setParameters("screen_state=on");
                return;
            }
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                if (AudioService.this.mMonitorRotation) {
                    RotationHelper.disable();
                }
                AudioSystem.setParameters("screen_state=off");
                return;
            }
            if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                AudioService.this.handleConfigurationChanged(context);
                return;
            }
            if (action.equals("android.intent.action.USER_SWITCHED")) {
                if (AudioService.this.mUserSwitchedReceived) {
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 15, 0, 0, 0, null, 0);
                }
                AudioService.this.mUserSwitchedReceived = true;
                AudioService.this.mMediaFocusControl.discardAudioFocusOwner();
                AudioService.this.readAudioSettings(true);
                AudioService.sendMsg(AudioService.this.mAudioHandler, 10, 2, 0, 0, AudioService.this.mStreamStates[3], 0);
                return;
            }
            if (action.equals("android.intent.action.USER_BACKGROUND")) {
                int intExtra3 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (intExtra3 >= 0) {
                    AudioService.this.killBackgroundUserProcessesWithRecordAudioPermission(UserManagerService.getInstance().getUserInfo(intExtra3));
                }
                UserManagerService.getInstance().setUserRestriction("no_record_audio", true, intExtra3);
                return;
            }
            if (action.equals("android.intent.action.USER_FOREGROUND")) {
                UserManagerService.getInstance().setUserRestriction("no_record_audio", false, intent.getIntExtra("android.intent.extra.user_handle", -1));
                return;
            }
            if (!action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                if (action.equals("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION") || action.equals("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION")) {
                    AudioService.this.handleAudioEffectBroadcast(context, intent);
                    return;
                }
                return;
            }
            int intExtra4 = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
            if (intExtra4 == 10 || intExtra4 == 13) {
                AudioService.this.disconnectAllBluetoothProfiles();
            }
        }
    }

    private class AudioServiceUserRestrictionsListener implements UserManagerInternal.UserRestrictionsListener {
        private AudioServiceUserRestrictionsListener() {
        }

        public void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2) {
            boolean z = bundle2.getBoolean("no_unmute_microphone");
            boolean z2 = bundle.getBoolean("no_unmute_microphone");
            if (z != z2) {
                AudioService.this.setMicrophoneMuteNoCallerCheck(z2, i);
            }
            boolean z3 = true;
            boolean z4 = bundle2.getBoolean("no_adjust_volume") || bundle2.getBoolean("disallow_unmute_device");
            if (!bundle.getBoolean("no_adjust_volume") && !bundle.getBoolean("disallow_unmute_device")) {
                z3 = false;
            }
            if (z4 != z3) {
                AudioService.this.setMasterMuteInternalNoCallerCheck(z3, 0, i);
            }
        }
    }

    private void handleAudioEffectBroadcast(Context context, Intent intent) {
        ResolveInfo resolveInfo;
        String str = intent.getPackage();
        if (str != null) {
            Log.w(TAG, "effect broadcast already targeted to " + str);
            return;
        }
        intent.addFlags(32);
        List<ResolveInfo> listQueryBroadcastReceivers = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        if (listQueryBroadcastReceivers != null && listQueryBroadcastReceivers.size() != 0 && (resolveInfo = listQueryBroadcastReceivers.get(0)) != null && resolveInfo.activityInfo != null && resolveInfo.activityInfo.packageName != null) {
            intent.setPackage(resolveInfo.activityInfo.packageName);
            context.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            Log.w(TAG, "couldn't find receiver package for effect intent");
        }
    }

    private void killBackgroundUserProcessesWithRecordAudioPermission(UserInfo userInfo) {
        ComponentName homeActivityForUser;
        PackageManager packageManager = this.mContext.getPackageManager();
        if (!userInfo.isManagedProfile()) {
            homeActivityForUser = this.mActivityManagerInternal.getHomeActivityForUser(userInfo.id);
        } else {
            homeActivityForUser = null;
        }
        try {
            List list = AppGlobals.getPackageManager().getPackagesHoldingPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 0, userInfo.id).getList();
            for (int size = list.size() - 1; size >= 0; size--) {
                PackageInfo packageInfo = (PackageInfo) list.get(size);
                if (UserHandle.getAppId(packageInfo.applicationInfo.uid) >= 10000 && packageManager.checkPermission("android.permission.INTERACT_ACROSS_USERS", packageInfo.packageName) != 0 && (homeActivityForUser == null || !packageInfo.packageName.equals(homeActivityForUser.getPackageName()) || !packageInfo.applicationInfo.isSystemApp())) {
                    try {
                        int i = packageInfo.applicationInfo.uid;
                        ActivityManager.getService().killUid(UserHandle.getAppId(i), UserHandle.getUserId(i), "killBackgroundUserProcessesWithAudioRecordPermission");
                    } catch (RemoteException e) {
                        Log.w(TAG, "Error calling killUid", e);
                    }
                }
            }
        } catch (RemoteException e2) {
            throw new AndroidRuntimeException(e2);
        }
    }

    private boolean forceFocusDuckingForAccessibility(AudioAttributes audioAttributes, int i, int i2) {
        Bundle bundle;
        if (audioAttributes == null || audioAttributes.getUsage() != 11 || i != 3 || (bundle = audioAttributes.getBundle()) == null || !bundle.getBoolean("a11y_force_ducking")) {
            return false;
        }
        if (i2 == 0) {
            return true;
        }
        synchronized (this.mAccessibilityServiceUidsLock) {
            if (this.mAccessibilityServiceUids != null) {
                int callingUid = Binder.getCallingUid();
                for (int i3 = 0; i3 < this.mAccessibilityServiceUids.length; i3++) {
                    if (this.mAccessibilityServiceUids[i3] == callingUid) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public int requestAudioFocus(AudioAttributes audioAttributes, int i, IBinder iBinder, IAudioFocusDispatcher iAudioFocusDispatcher, String str, String str2, int i2, IAudioPolicyCallback iAudioPolicyCallback, int i3) {
        String str3;
        if ((i2 & 4) == 4) {
            str3 = str;
            if ("AudioFocus_For_Phone_Ring_And_Calls".equals(str3)) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
                    Log.e(TAG, "Invalid permission to (un)lock audio focus", new Exception());
                    return 0;
                }
            } else {
                synchronized (this.mAudioPolicies) {
                    if (!this.mAudioPolicies.containsKey(iAudioPolicyCallback.asBinder())) {
                        Log.e(TAG, "Invalid unregistered AudioPolicy to (un)lock audio focus");
                        return 0;
                    }
                }
            }
        } else {
            str3 = str;
        }
        return this.mMediaFocusControl.requestAudioFocus(audioAttributes, i, iBinder, iAudioFocusDispatcher, str3, str2, i2, i3, forceFocusDuckingForAccessibility(audioAttributes, i, Binder.getCallingUid()));
    }

    public int abandonAudioFocus(IAudioFocusDispatcher iAudioFocusDispatcher, String str, AudioAttributes audioAttributes, String str2) {
        return this.mMediaFocusControl.abandonAudioFocus(iAudioFocusDispatcher, str, audioAttributes, str2);
    }

    public void unregisterAudioFocusClient(String str) {
        this.mMediaFocusControl.unregisterAudioFocusClient(str);
    }

    public int getCurrentAudioFocus() {
        return this.mMediaFocusControl.getCurrentAudioFocus();
    }

    public int getFocusRampTimeMs(int i, AudioAttributes audioAttributes) {
        MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
        return MediaFocusControl.getFocusRampTimeMs(i, audioAttributes);
    }

    private boolean readCameraSoundForced() {
        return SystemProperties.getBoolean("audio.camerasound.force", false) || this.mContext.getResources().getBoolean(R.^attr-private.colorProgressBackgroundNormal);
    }

    private void handleConfigurationChanged(Context context) {
        try {
            Configuration configuration = context.getResources().getConfiguration();
            sendMsg(this.mAudioHandler, 16, 0, 0, 0, TAG, 0);
            boolean cameraSoundForced = readCameraSoundForced();
            synchronized (this.mSettingsLock) {
                int i = 0;
                boolean z = cameraSoundForced != this.mCameraSoundForced;
                this.mCameraSoundForced = cameraSoundForced;
                if (z) {
                    if (!this.mIsSingleVolume) {
                        synchronized (VolumeStreamState.class) {
                            VolumeStreamState volumeStreamState = this.mStreamStates[7];
                            if (cameraSoundForced) {
                                volumeStreamState.setAllIndexesToMax();
                                this.mRingerModeAffectedStreams &= -129;
                            } else {
                                volumeStreamState.setAllIndexes(this.mStreamStates[1], TAG);
                                this.mRingerModeAffectedStreams |= 128;
                            }
                        }
                        setRingerModeInt(getRingerModeInternal(), false);
                    }
                    AudioHandler audioHandler = this.mAudioHandler;
                    if (cameraSoundForced) {
                        i = 11;
                    }
                    sendMsg(audioHandler, 8, 2, 4, i, new String("handleConfigurationChanged"), 0);
                    sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[7], 0);
                }
            }
            this.mVolumeController.setLayoutDirection(configuration.getLayoutDirection());
        } catch (Exception e) {
            Log.e(TAG, "Error handling configuration change: ", e);
        }
    }

    public void setBluetoothA2dpOnInt(boolean z, String str) {
        synchronized (this.mBluetoothA2dpEnabledLock) {
            this.mBluetoothA2dpEnabled = z;
            this.mAudioHandler.removeMessages(13);
            setForceUseInt_SyncDevices(1, this.mBluetoothA2dpEnabled ? 0 : 10, str);
        }
    }

    private void setForceUseInt_SyncDevices(int i, int i2, String str) {
        if (i == 1) {
            sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
        }
        this.mForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(i, i2, str));
        if (LOGD) {
            Log.i(TAG, "setForceUse(" + AudioSystem.forceUseUsageToString(i) + ", " + AudioSystem.forceUseConfigToString(i2) + ") due to " + str);
        }
        AudioSystem.setForceUse(i, i2);
    }

    public void setRingtonePlayer(IRingtonePlayer iRingtonePlayer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.REMOTE_AUDIO_PLAYBACK", null);
        this.mRingtonePlayer = iRingtonePlayer;
    }

    public IRingtonePlayer getRingtonePlayer() {
        return this.mRingtonePlayer;
    }

    public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver iAudioRoutesObserver) {
        AudioRoutesInfo audioRoutesInfo;
        synchronized (this.mCurAudioRoutes) {
            audioRoutesInfo = new AudioRoutesInfo(this.mCurAudioRoutes);
            this.mRoutesObservers.register(iAudioRoutesObserver);
        }
        return audioRoutesInfo;
    }

    private int safeMediaVolumeIndex(int i) {
        if ((67108876 & i) == 0) {
            return MAX_STREAM_VOLUME[3];
        }
        if (i == 67108864) {
            return this.mSafeUsbMediaVolumeIndex;
        }
        return this.mSafeMediaVolumeIndex;
    }

    private void setSafeMediaVolumeEnabled(boolean z, String str) {
        synchronized (this.mSafeMediaVolumeState) {
            if (this.mSafeMediaVolumeState.intValue() != 0 && this.mSafeMediaVolumeState.intValue() != 1) {
                if (z && this.mSafeMediaVolumeState.intValue() == 2) {
                    this.mSafeMediaVolumeState = 3;
                    enforceSafeMediaVolume(str);
                } else if (!z && this.mSafeMediaVolumeState.intValue() == 3) {
                    this.mSafeMediaVolumeState = 2;
                    this.mMusicActiveMs = 1;
                    saveMusicActiveMs();
                    sendMsg(this.mAudioHandler, 14, 0, 0, 0, str, MUSIC_ACTIVE_POLL_PERIOD_MS);
                }
            }
        }
    }

    private void enforceSafeMediaVolume(String str) {
        VolumeStreamState volumeStreamState = this.mStreamStates[3];
        int i = 67108876;
        int i2 = 0;
        while (i != 0) {
            int i3 = i2 + 1;
            int i4 = 1 << i2;
            if ((i4 & i) != 0) {
                if (volumeStreamState.getIndex(i4) > safeMediaVolumeIndex(i4)) {
                    volumeStreamState.setIndex(safeMediaVolumeIndex(i4), i4, str);
                    sendMsg(this.mAudioHandler, 0, 2, i4, 0, volumeStreamState, 0);
                }
                i &= ~i4;
            }
            i2 = i3;
        }
    }

    private boolean checkSafeMediaVolume(int i, int i2, int i3) {
        synchronized (this.mSafeMediaVolumeState) {
            if (this.mSafeMediaVolumeState.intValue() == 3 && mStreamVolumeAlias[i] == 3 && (67108876 & i3) != 0 && i2 > safeMediaVolumeIndex(i3)) {
                return false;
            }
            return true;
        }
    }

    public void disableSafeMediaVolume(String str) {
        enforceVolumeController("disable the safe media volume");
        synchronized (this.mSafeMediaVolumeState) {
            setSafeMediaVolumeEnabled(false, str);
            if (this.mPendingVolumeCommand != null) {
                onSetStreamVolume(this.mPendingVolumeCommand.mStreamType, this.mPendingVolumeCommand.mIndex, this.mPendingVolumeCommand.mFlags, this.mPendingVolumeCommand.mDevice, str);
                this.mPendingVolumeCommand = null;
            }
        }
    }

    private class MyDisplayStatusCallback implements HdmiPlaybackClient.DisplayStatusCallback {
        private MyDisplayStatusCallback() {
        }

        public void onComplete(int i) {
            if (AudioService.this.mHdmiManager != null) {
                synchronized (AudioService.this.mHdmiManager) {
                    AudioService.this.mHdmiCecSink = i != -1;
                    if (AudioService.this.isPlatformTelevision() && !AudioService.this.mHdmiCecSink) {
                        AudioService.this.mFixedVolumeDevices &= -1025;
                    }
                    AudioService.this.checkAllFixedVolumeDevices();
                }
            }
        }
    }

    public int setHdmiSystemAudioSupported(boolean z) {
        if (this.mHdmiManager != null) {
            synchronized (this.mHdmiManager) {
                if (this.mHdmiTvClient == null) {
                    Log.w(TAG, "Only Hdmi-Cec enabled TV device supports system audio mode.");
                    return 0;
                }
                synchronized (this.mHdmiTvClient) {
                    if (this.mHdmiSystemAudioSupported != z) {
                        this.mHdmiSystemAudioSupported = z;
                        devicesForStream = z ? 12 : 0;
                        this.mForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(5, devicesForStream, "setHdmiSystemAudioSupported"));
                        AudioSystem.setForceUse(5, devicesForStream);
                    }
                    devicesForStream = getDevicesForStream(3);
                }
            }
        }
        return devicesForStream;
    }

    public boolean isHdmiSystemAudioSupported() {
        return this.mHdmiSystemAudioSupported;
    }

    private void initA11yMonitoring() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        updateDefaultStreamOverrideDelay(accessibilityManager.isTouchExplorationEnabled());
        updateA11yVolumeAlias(accessibilityManager.isAccessibilityVolumeStreamActive());
        accessibilityManager.addTouchExplorationStateChangeListener(this, null);
        accessibilityManager.addAccessibilityServicesStateChangeListener(this, (Handler) null);
    }

    @Override
    public void onTouchExplorationStateChanged(boolean z) {
        updateDefaultStreamOverrideDelay(z);
    }

    private void updateDefaultStreamOverrideDelay(boolean z) {
        if (z) {
            sStreamOverrideDelayMs = 1000;
        } else {
            sStreamOverrideDelayMs = 0;
        }
        if (DEBUG_VOL) {
            Log.d(TAG, "Touch exploration enabled=" + z + " stream override delay is now " + sStreamOverrideDelayMs + " ms");
        }
    }

    @Override
    public void onAccessibilityServicesStateChanged(AccessibilityManager accessibilityManager) {
        updateA11yVolumeAlias(accessibilityManager.isAccessibilityVolumeStreamActive());
    }

    private void updateA11yVolumeAlias(boolean z) {
        if (DEBUG_VOL) {
            Log.d(TAG, "Accessibility volume enabled = " + z);
        }
        if (sIndependentA11yVolume != z) {
            sIndependentA11yVolume = z;
            updateStreamVolumeAlias(true, TAG);
            this.mVolumeController.setA11yMode(sIndependentA11yVolume ? 1 : 0);
            this.mVolumeController.postVolumeChanged(10, 0);
        }
    }

    public boolean isCameraSoundForced() {
        boolean z;
        synchronized (this.mSettingsLock) {
            z = this.mCameraSoundForced;
        }
        return z;
    }

    private void dumpRingerMode(PrintWriter printWriter) {
        printWriter.println("\nRinger mode: ");
        printWriter.println("- mode (internal) = " + RINGER_MODE_NAMES[this.mRingerMode]);
        printWriter.println("- mode (external) = " + RINGER_MODE_NAMES[this.mRingerModeExternal]);
        dumpRingerModeStreams(printWriter, "affected", this.mRingerModeAffectedStreams);
        dumpRingerModeStreams(printWriter, "muted", this.mRingerAndZenModeMutedStreams);
        printWriter.print("- delegate = ");
        printWriter.println(this.mRingerModeDelegate);
    }

    private void dumpRingerModeStreams(PrintWriter printWriter, String str, int i) {
        printWriter.print("- ringer mode ");
        printWriter.print(str);
        printWriter.print(" streams = 0x");
        printWriter.print(Integer.toHexString(i));
        if (i != 0) {
            printWriter.print(" (");
            int i2 = i;
            boolean z = true;
            for (int i3 = 0; i3 < AudioSystem.STREAM_NAMES.length; i3++) {
                int i4 = 1 << i3;
                if ((i2 & i4) != 0) {
                    if (!z) {
                        printWriter.print(',');
                    }
                    printWriter.print(AudioSystem.STREAM_NAMES[i3]);
                    i2 &= ~i4;
                    z = false;
                }
            }
            if (i2 != 0) {
                if (!z) {
                    printWriter.print(',');
                }
                printWriter.print(i2);
            }
            printWriter.print(')');
        }
        printWriter.println();
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            this.mMediaFocusControl.dump(printWriter);
            dumpStreamStates(printWriter);
            dumpRingerMode(printWriter);
            printWriter.println("\nAudio routes:");
            printWriter.print("  mMainType=0x");
            printWriter.println(Integer.toHexString(this.mCurAudioRoutes.mainType));
            printWriter.print("  mBluetoothName=");
            printWriter.println(this.mCurAudioRoutes.bluetoothName);
            printWriter.println("\nOther state:");
            printWriter.print("  mVolumeController=");
            printWriter.println(this.mVolumeController);
            printWriter.print("  mSafeMediaVolumeState=");
            printWriter.println(safeMediaVolumeStateToString(this.mSafeMediaVolumeState));
            printWriter.print("  mSafeMediaVolumeIndex=");
            printWriter.println(this.mSafeMediaVolumeIndex);
            printWriter.print("  mSafeUsbMediaVolumeIndex=");
            printWriter.println(this.mSafeUsbMediaVolumeIndex);
            printWriter.print("  mSafeUsbMediaVolumeDbfs=");
            printWriter.println(this.mSafeUsbMediaVolumeDbfs);
            printWriter.print("  sIndependentA11yVolume=");
            printWriter.println(sIndependentA11yVolume);
            printWriter.print("  mPendingVolumeCommand=");
            printWriter.println(this.mPendingVolumeCommand);
            printWriter.print("  mMusicActiveMs=");
            printWriter.println(this.mMusicActiveMs);
            printWriter.print("  mMcc=");
            printWriter.println(this.mMcc);
            printWriter.print("  mCameraSoundForced=");
            printWriter.println(this.mCameraSoundForced);
            printWriter.print("  mHasVibrator=");
            printWriter.println(this.mHasVibrator);
            printWriter.print("  mVolumePolicy=");
            printWriter.println(this.mVolumePolicy);
            printWriter.print("  mAvrcpAbsVolSupported=");
            printWriter.println(this.mAvrcpAbsVolSupported);
            dumpAudioPolicies(printWriter);
            this.mDynPolicyLogger.dump(printWriter);
            this.mPlaybackMonitor.dump(printWriter);
            this.mRecordMonitor.dump(printWriter);
            printWriter.println("\n");
            printWriter.println("\nEvent logs:");
            this.mModeLogger.dump(printWriter);
            printWriter.println("\n");
            this.mWiredDevLogger.dump(printWriter);
            printWriter.println("\n");
            this.mForceUseLogger.dump(printWriter);
            printWriter.println("\n");
            this.mVolumeLogger.dump(printWriter);
        }
    }

    private static String safeMediaVolumeStateToString(Integer num) {
        switch (num.intValue()) {
            case 0:
                return "SAFE_MEDIA_VOLUME_NOT_CONFIGURED";
            case 1:
                return "SAFE_MEDIA_VOLUME_DISABLED";
            case 2:
                return "SAFE_MEDIA_VOLUME_INACTIVE";
            case 3:
                return "SAFE_MEDIA_VOLUME_ACTIVE";
            default:
                return null;
        }
    }

    private static void readAndSetLowRamDevice() {
        long j;
        boolean zIsLowRamDeviceStatic = ActivityManager.isLowRamDeviceStatic();
        try {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            ActivityManager.getService().getMemoryInfo(memoryInfo);
            j = memoryInfo.totalMem;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot obtain MemoryInfo from ActivityManager, assume low memory device");
            zIsLowRamDeviceStatic = true;
            j = 1073741824;
        }
        int lowRamDevice = AudioSystem.setLowRamDevice(zIsLowRamDeviceStatic, j);
        if (lowRamDevice != 0) {
            Log.w(TAG, "AudioFlinger informed of device's low RAM attribute; status " + lowRamDevice);
        }
    }

    private void enforceVolumeController(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "Only SystemUI can " + str);
    }

    public void setVolumeController(final IVolumeController iVolumeController) {
        enforceVolumeController("set the volume controller");
        if (this.mVolumeController.isSameBinder(iVolumeController)) {
            return;
        }
        this.mVolumeController.postDismiss();
        if (iVolumeController != null) {
            try {
                iVolumeController.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        if (AudioService.this.mVolumeController.isSameBinder(iVolumeController)) {
                            Log.w(AudioService.TAG, "Current remote volume controller died, unregistering");
                            AudioService.this.setVolumeController(null);
                        }
                    }
                }, 0);
            } catch (RemoteException e) {
            }
        }
        this.mVolumeController.setController(iVolumeController);
        if (DEBUG_VOL) {
            Log.d(TAG, "Volume controller: " + this.mVolumeController);
        }
    }

    public void notifyVolumeControllerVisible(IVolumeController iVolumeController, boolean z) {
        enforceVolumeController("notify about volume controller visibility");
        if (!this.mVolumeController.isSameBinder(iVolumeController)) {
            return;
        }
        this.mVolumeController.setVisible(z);
        if (DEBUG_VOL) {
            Log.d(TAG, "Volume controller visible: " + z);
        }
    }

    public void setVolumePolicy(VolumePolicy volumePolicy) {
        enforceVolumeController("set volume policy");
        if (volumePolicy != null && !volumePolicy.equals(this.mVolumePolicy)) {
            this.mVolumePolicy = volumePolicy;
            if (DEBUG_VOL) {
                Log.d(TAG, "Volume policy changed: " + this.mVolumePolicy);
            }
        }
    }

    public static class VolumeController {
        private static final String TAG = "VolumeController";
        private IVolumeController mController;
        private int mLongPressTimeout;
        private long mNextLongPress;
        private boolean mVisible;

        public void setController(IVolumeController iVolumeController) {
            this.mController = iVolumeController;
            this.mVisible = false;
        }

        public void loadSettings(ContentResolver contentResolver) {
            this.mLongPressTimeout = Settings.Secure.getIntForUser(contentResolver, "long_press_timeout", 500, -2);
        }

        public boolean suppressAdjustment(int i, int i2, boolean z) {
            if (z) {
                return false;
            }
            if (i == 3 && this.mController != null) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                if ((i2 & 1) != 0 && !this.mVisible) {
                    if (this.mNextLongPress >= jUptimeMillis) {
                        return true;
                    }
                    this.mNextLongPress = jUptimeMillis + ((long) this.mLongPressTimeout);
                    return true;
                }
                if (this.mNextLongPress > 0) {
                    if (jUptimeMillis <= this.mNextLongPress) {
                        return true;
                    }
                    this.mNextLongPress = 0L;
                }
            }
            return false;
        }

        public void setVisible(boolean z) {
            this.mVisible = z;
        }

        public boolean isSameBinder(IVolumeController iVolumeController) {
            return Objects.equals(asBinder(), binder(iVolumeController));
        }

        public IBinder asBinder() {
            return binder(this.mController);
        }

        private static IBinder binder(IVolumeController iVolumeController) {
            if (iVolumeController == null) {
                return null;
            }
            return iVolumeController.asBinder();
        }

        public String toString() {
            return "VolumeController(" + asBinder() + ",mVisible=" + this.mVisible + ")";
        }

        public void postDisplaySafeVolumeWarning(int i) {
            if (this.mController == null) {
                return;
            }
            try {
                this.mController.displaySafeVolumeWarning(i);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling displaySafeVolumeWarning", e);
            }
        }

        public void postVolumeChanged(int i, int i2) {
            if (this.mController == null) {
                return;
            }
            try {
                this.mController.volumeChanged(i, i2);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling volumeChanged", e);
            }
        }

        public void postMasterMuteChanged(int i) {
            if (this.mController == null) {
                return;
            }
            try {
                this.mController.masterMuteChanged(i);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling masterMuteChanged", e);
            }
        }

        public void setLayoutDirection(int i) {
            if (this.mController == null) {
                return;
            }
            try {
                this.mController.setLayoutDirection(i);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling setLayoutDirection", e);
            }
        }

        public void postDismiss() {
            if (this.mController == null) {
                return;
            }
            try {
                this.mController.dismiss();
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling dismiss", e);
            }
        }

        public void setA11yMode(int i) {
            if (this.mController == null) {
                return;
            }
            try {
                this.mController.setA11yMode(i);
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling setA11Mode", e);
            }
        }
    }

    final class AudioServiceInternal extends AudioManagerInternal {
        AudioServiceInternal() {
        }

        public void setRingerModeDelegate(AudioManagerInternal.RingerModeDelegate ringerModeDelegate) {
            AudioService.this.mRingerModeDelegate = ringerModeDelegate;
            if (AudioService.this.mRingerModeDelegate != null) {
                synchronized (AudioService.this.mSettingsLock) {
                    AudioService.this.updateRingerAndZenModeAffectedStreams();
                }
                setRingerModeInternal(getRingerModeInternal(), "AudioService.setRingerModeDelegate");
            }
        }

        public void adjustSuggestedStreamVolumeForUid(int i, int i2, int i3, String str, int i4) {
            AudioService.this.adjustSuggestedStreamVolume(i2, i, i3, str, str, i4);
        }

        public void adjustStreamVolumeForUid(int i, int i2, int i3, String str, int i4) {
            AudioService.this.adjustStreamVolume(i, i2, i3, str, str, i4);
        }

        public void setStreamVolumeForUid(int i, int i2, int i3, String str, int i4) {
            AudioService.this.setStreamVolume(i, i2, i3, str, str, i4);
        }

        public int getRingerModeInternal() {
            return AudioService.this.getRingerModeInternal();
        }

        public void setRingerModeInternal(int i, String str) {
            AudioService.this.setRingerModeInternal(i, str);
        }

        public void silenceRingerModeInternal(String str) {
            AudioService.this.silenceRingerModeInternal(str);
        }

        public void updateRingerModeAffectedStreamsInternal() {
            synchronized (AudioService.this.mSettingsLock) {
                if (AudioService.this.updateRingerAndZenModeAffectedStreams()) {
                    AudioService.this.setRingerModeInt(getRingerModeInternal(), false);
                }
            }
        }

        public void setAccessibilityServiceUids(IntArray intArray) {
            synchronized (AudioService.this.mAccessibilityServiceUidsLock) {
                if (intArray.size() == 0) {
                    AudioService.this.mAccessibilityServiceUids = null;
                } else {
                    int i = 0;
                    boolean z = AudioService.this.mAccessibilityServiceUids == null || AudioService.this.mAccessibilityServiceUids.length != intArray.size();
                    if (!z) {
                        while (true) {
                            if (i >= AudioService.this.mAccessibilityServiceUids.length) {
                                break;
                            }
                            if (intArray.get(i) == AudioService.this.mAccessibilityServiceUids[i]) {
                                i++;
                            } else {
                                z = true;
                                break;
                            }
                        }
                    }
                    if (z) {
                        AudioService.this.mAccessibilityServiceUids = intArray.toArray();
                    }
                }
            }
        }
    }

    public String registerAudioPolicy(AudioPolicyConfig audioPolicyConfig, IAudioPolicyCallback iAudioPolicyCallback, boolean z, boolean z2, boolean z3) {
        AudioSystem.setDynamicPolicyCallback(this.mDynPolicyCallback);
        if (!(this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0)) {
            Slog.w(TAG, "Can't register audio policy for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", need MODIFY_AUDIO_ROUTING");
            return null;
        }
        this.mDynPolicyLogger.log(new AudioEventLogger.StringEvent("registerAudioPolicy for " + iAudioPolicyCallback.asBinder() + " with config:" + audioPolicyConfig).printLog(TAG));
        synchronized (this.mAudioPolicies) {
            try {
                try {
                    if (this.mAudioPolicies.containsKey(iAudioPolicyCallback.asBinder())) {
                        Slog.e(TAG, "Cannot re-register policy");
                        return null;
                    }
                    AudioPolicyProxy audioPolicyProxy = new AudioPolicyProxy(audioPolicyConfig, iAudioPolicyCallback, z, z2, z3);
                    iAudioPolicyCallback.asBinder().linkToDeath(audioPolicyProxy, 0);
                    String registrationId = audioPolicyProxy.getRegistrationId();
                    this.mAudioPolicies.put(iAudioPolicyCallback.asBinder(), audioPolicyProxy);
                    return registrationId;
                } catch (RemoteException e) {
                    Slog.w(TAG, "Audio policy registration failed, could not link to " + iAudioPolicyCallback + " binder death", e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void unregisterAudioPolicyAsync(IAudioPolicyCallback iAudioPolicyCallback) {
        this.mDynPolicyLogger.log(new AudioEventLogger.StringEvent("unregisterAudioPolicyAsync for " + iAudioPolicyCallback.asBinder()).printLog(TAG));
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy audioPolicyProxyRemove = this.mAudioPolicies.remove(iAudioPolicyCallback.asBinder());
            if (audioPolicyProxyRemove == null) {
                Slog.w(TAG, "Trying to unregister unknown audio policy for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid());
                return;
            }
            iAudioPolicyCallback.asBinder().unlinkToDeath(audioPolicyProxyRemove, 0);
            audioPolicyProxyRemove.release();
        }
    }

    @GuardedBy("mAudioPolicies")
    private AudioPolicyProxy checkUpdateForPolicy(IAudioPolicyCallback iAudioPolicyCallback, String str) {
        if (!(this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0)) {
            Slog.w(TAG, str + " for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", need MODIFY_AUDIO_ROUTING");
            return null;
        }
        AudioPolicyProxy audioPolicyProxy = this.mAudioPolicies.get(iAudioPolicyCallback.asBinder());
        if (audioPolicyProxy == null) {
            Slog.w(TAG, str + " for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", unregistered policy");
            return null;
        }
        return audioPolicyProxy;
    }

    public int addMixForPolicy(AudioPolicyConfig audioPolicyConfig, IAudioPolicyCallback iAudioPolicyCallback) {
        if (DEBUG_AP) {
            Log.d(TAG, "addMixForPolicy for " + iAudioPolicyCallback.asBinder() + " with config:" + audioPolicyConfig);
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy audioPolicyProxyCheckUpdateForPolicy = checkUpdateForPolicy(iAudioPolicyCallback, "Cannot add AudioMix in audio policy");
            if (audioPolicyProxyCheckUpdateForPolicy == null) {
                return -1;
            }
            audioPolicyProxyCheckUpdateForPolicy.addMixes(audioPolicyConfig.getMixes());
            return 0;
        }
    }

    public int removeMixForPolicy(AudioPolicyConfig audioPolicyConfig, IAudioPolicyCallback iAudioPolicyCallback) {
        if (DEBUG_AP) {
            Log.d(TAG, "removeMixForPolicy for " + iAudioPolicyCallback.asBinder() + " with config:" + audioPolicyConfig);
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy audioPolicyProxyCheckUpdateForPolicy = checkUpdateForPolicy(iAudioPolicyCallback, "Cannot add AudioMix in audio policy");
            if (audioPolicyProxyCheckUpdateForPolicy == null) {
                return -1;
            }
            audioPolicyProxyCheckUpdateForPolicy.removeMixes(audioPolicyConfig.getMixes());
            return 0;
        }
    }

    public int setFocusPropertiesForPolicy(int i, IAudioPolicyCallback iAudioPolicyCallback) {
        if (DEBUG_AP) {
            Log.d(TAG, "setFocusPropertiesForPolicy() duck behavior=" + i + " policy " + iAudioPolicyCallback.asBinder());
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy audioPolicyProxyCheckUpdateForPolicy = checkUpdateForPolicy(iAudioPolicyCallback, "Cannot change audio policy focus properties");
            if (audioPolicyProxyCheckUpdateForPolicy == null) {
                return -1;
            }
            if (!this.mAudioPolicies.containsKey(iAudioPolicyCallback.asBinder())) {
                Slog.e(TAG, "Cannot change audio policy focus properties, unregistered policy");
                return -1;
            }
            boolean z = true;
            if (i == 1) {
                Iterator<AudioPolicyProxy> it = this.mAudioPolicies.values().iterator();
                while (it.hasNext()) {
                    if (it.next().mFocusDuckBehavior == 1) {
                        Slog.e(TAG, "Cannot change audio policy ducking behavior, already handled");
                        return -1;
                    }
                }
            }
            audioPolicyProxyCheckUpdateForPolicy.mFocusDuckBehavior = i;
            MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
            if (i != 1) {
                z = false;
            }
            mediaFocusControl.setDuckingInExtPolicyAvailable(z);
            return 0;
        }
    }

    private void setExtVolumeController(IAudioPolicyCallback iAudioPolicyCallback) {
        if (!this.mContext.getResources().getBoolean(R.^attr-private.interpolatorZ)) {
            Log.e(TAG, "Cannot set external volume controller: device not set for volume keys handled in PhoneWindowManager");
            return;
        }
        synchronized (this.mExtVolumeControllerLock) {
            if (this.mExtVolumeController != null && !this.mExtVolumeController.asBinder().pingBinder()) {
                Log.e(TAG, "Cannot set external volume controller: existing controller");
            }
            this.mExtVolumeController = iAudioPolicyCallback;
        }
    }

    private void dumpAudioPolicies(PrintWriter printWriter) {
        printWriter.println("\nAudio policies:");
        synchronized (this.mAudioPolicies) {
            Iterator<AudioPolicyProxy> it = this.mAudioPolicies.values().iterator();
            while (it.hasNext()) {
                printWriter.println(it.next().toLogFriendlyString());
            }
        }
    }

    private void onDynPolicyMixStateUpdate(String str, int i) {
        if (DEBUG_AP) {
            Log.d(TAG, "onDynamicPolicyMixStateUpdate(" + str + ", " + i + ")");
        }
        synchronized (this.mAudioPolicies) {
            for (AudioPolicyProxy audioPolicyProxy : this.mAudioPolicies.values()) {
                Iterator it = audioPolicyProxy.getMixes().iterator();
                while (it.hasNext()) {
                    if (((AudioMix) it.next()).getRegistration().equals(str)) {
                        try {
                            audioPolicyProxy.mPolicyCallback.notifyMixStateUpdate(str, i);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Can't call notifyMixStateUpdate() on IAudioPolicyCallback " + audioPolicyProxy.mPolicyCallback.asBinder(), e);
                        }
                        return;
                    }
                }
            }
        }
    }

    public void registerRecordingCallback(IRecordingConfigDispatcher iRecordingConfigDispatcher) {
        this.mRecordMonitor.registerRecordingCallback(iRecordingConfigDispatcher, this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0);
    }

    public void unregisterRecordingCallback(IRecordingConfigDispatcher iRecordingConfigDispatcher) {
        this.mRecordMonitor.unregisterRecordingCallback(iRecordingConfigDispatcher);
    }

    public List<AudioRecordingConfiguration> getActiveRecordingConfigurations() {
        return this.mRecordMonitor.getActiveRecordingConfigurations(this.mContext.checkCallingPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0);
    }

    public void disableRingtoneSync(int i) {
        if (UserHandle.getCallingUserId() != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "disable sound settings syncing for another profile");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContentResolver, "sync_parent_sounds", 0, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void registerPlaybackCallback(IPlaybackConfigDispatcher iPlaybackConfigDispatcher) {
        this.mPlaybackMonitor.registerPlaybackCallback(iPlaybackConfigDispatcher, this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0);
    }

    public void unregisterPlaybackCallback(IPlaybackConfigDispatcher iPlaybackConfigDispatcher) {
        this.mPlaybackMonitor.unregisterPlaybackCallback(iPlaybackConfigDispatcher);
    }

    public List<AudioPlaybackConfiguration> getActivePlaybackConfigurations() {
        return this.mPlaybackMonitor.getActivePlaybackConfigurations(this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") == 0);
    }

    public int trackPlayer(PlayerBase.PlayerIdCard playerIdCard) {
        return this.mPlaybackMonitor.trackPlayer(playerIdCard);
    }

    public void playerAttributes(int i, AudioAttributes audioAttributes) {
        this.mPlaybackMonitor.playerAttributes(i, audioAttributes, Binder.getCallingUid());
    }

    public void playerEvent(int i, int i2) {
        this.mPlaybackMonitor.playerEvent(i, i2, Binder.getCallingUid());
    }

    public void playerHasOpPlayAudio(int i, boolean z) {
        this.mPlaybackMonitor.playerHasOpPlayAudio(i, z, Binder.getCallingUid());
    }

    public void releasePlayer(int i) {
        this.mPlaybackMonitor.releasePlayer(i, Binder.getCallingUid());
    }

    public class AudioPolicyProxy extends AudioPolicyConfig implements IBinder.DeathRecipient {
        private static final String TAG = "AudioPolicyProxy";
        int mFocusDuckBehavior;
        final boolean mHasFocusListener;
        boolean mIsFocusPolicy;
        final boolean mIsVolumeController;
        final IAudioPolicyCallback mPolicyCallback;

        AudioPolicyProxy(AudioPolicyConfig audioPolicyConfig, IAudioPolicyCallback iAudioPolicyCallback, boolean z, boolean z2, boolean z3) {
            super(audioPolicyConfig);
            this.mFocusDuckBehavior = 0;
            this.mIsFocusPolicy = false;
            setRegistration(new String(audioPolicyConfig.hashCode() + ":ap:" + AudioService.access$11808(AudioService.this)));
            this.mPolicyCallback = iAudioPolicyCallback;
            this.mHasFocusListener = z;
            this.mIsVolumeController = z3;
            if (this.mHasFocusListener) {
                AudioService.this.mMediaFocusControl.addFocusFollower(this.mPolicyCallback);
                if (z2) {
                    this.mIsFocusPolicy = true;
                    AudioService.this.mMediaFocusControl.setFocusPolicy(this.mPolicyCallback);
                }
            }
            if (this.mIsVolumeController) {
                AudioService.this.setExtVolumeController(this.mPolicyCallback);
            }
            connectMixes();
        }

        @Override
        public void binderDied() {
            synchronized (AudioService.this.mAudioPolicies) {
                Log.i(TAG, "audio policy " + this.mPolicyCallback + " died");
                release();
                AudioService.this.mAudioPolicies.remove(this.mPolicyCallback.asBinder());
            }
            if (this.mIsVolumeController) {
                synchronized (AudioService.this.mExtVolumeControllerLock) {
                    AudioService.this.mExtVolumeController = null;
                }
            }
        }

        String getRegistrationId() {
            return getRegistration();
        }

        void release() {
            if (this.mIsFocusPolicy) {
                AudioService.this.mMediaFocusControl.unsetFocusPolicy(this.mPolicyCallback);
            }
            if (this.mFocusDuckBehavior == 1) {
                AudioService.this.mMediaFocusControl.setDuckingInExtPolicyAvailable(false);
            }
            if (this.mHasFocusListener) {
                AudioService.this.mMediaFocusControl.removeFocusFollower(this.mPolicyCallback);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            AudioSystem.registerPolicyMixes(this.mMixes, false);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }

        boolean hasMixAffectingUsage(int i) {
            Iterator it = this.mMixes.iterator();
            while (it.hasNext()) {
                if (((AudioMix) it.next()).isAffectingUsage(i)) {
                    return true;
                }
            }
            return false;
        }

        void addMixes(ArrayList<AudioMix> arrayList) {
            synchronized (this.mMixes) {
                AudioSystem.registerPolicyMixes(this.mMixes, false);
                add(arrayList);
                AudioSystem.registerPolicyMixes(this.mMixes, true);
            }
        }

        void removeMixes(ArrayList<AudioMix> arrayList) {
            synchronized (this.mMixes) {
                AudioSystem.registerPolicyMixes(this.mMixes, false);
                remove(arrayList);
                AudioSystem.registerPolicyMixes(this.mMixes, true);
            }
        }

        void connectMixes() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            AudioSystem.registerPolicyMixes(this.mMixes, true);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int dispatchFocusChange(AudioFocusInfo audioFocusInfo, int i, IAudioPolicyCallback iAudioPolicyCallback) {
        int iDispatchFocusChange;
        if (audioFocusInfo == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusInfo");
        }
        if (iAudioPolicyCallback == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy callback");
        }
        synchronized (this.mAudioPolicies) {
            if (!this.mAudioPolicies.containsKey(iAudioPolicyCallback.asBinder())) {
                throw new IllegalStateException("Unregistered AudioPolicy for focus dispatch");
            }
            iDispatchFocusChange = this.mMediaFocusControl.dispatchFocusChange(audioFocusInfo, i);
        }
        return iDispatchFocusChange;
    }

    public void setFocusRequestResultFromExtPolicy(AudioFocusInfo audioFocusInfo, int i, IAudioPolicyCallback iAudioPolicyCallback) {
        if (audioFocusInfo == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusInfo");
        }
        if (iAudioPolicyCallback == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy callback");
        }
        synchronized (this.mAudioPolicies) {
            if (!this.mAudioPolicies.containsKey(iAudioPolicyCallback.asBinder())) {
                throw new IllegalStateException("Unregistered AudioPolicy for external focus");
            }
            this.mMediaFocusControl.setFocusRequestResultFromExtPolicy(audioFocusInfo, i);
        }
    }

    private class AsdProxy implements IBinder.DeathRecipient {
        private final IAudioServerStateDispatcher mAsd;

        AsdProxy(IAudioServerStateDispatcher iAudioServerStateDispatcher) {
            this.mAsd = iAudioServerStateDispatcher;
        }

        @Override
        public void binderDied() {
            synchronized (AudioService.this.mAudioServerStateListeners) {
                AudioService.this.mAudioServerStateListeners.remove(this.mAsd.asBinder());
            }
        }

        IAudioServerStateDispatcher callback() {
            return this.mAsd;
        }
    }

    private void checkMonitorAudioServerStatePermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") != 0) {
            throw new SecurityException("Not allowed to monitor audioserver state");
        }
    }

    public void registerAudioServerStateDispatcher(IAudioServerStateDispatcher iAudioServerStateDispatcher) {
        checkMonitorAudioServerStatePermission();
        synchronized (this.mAudioServerStateListeners) {
            if (this.mAudioServerStateListeners.containsKey(iAudioServerStateDispatcher.asBinder())) {
                Slog.w(TAG, "Cannot re-register audio server state dispatcher");
                return;
            }
            AsdProxy asdProxy = new AsdProxy(iAudioServerStateDispatcher);
            try {
                iAudioServerStateDispatcher.asBinder().linkToDeath(asdProxy, 0);
            } catch (RemoteException e) {
            }
            this.mAudioServerStateListeners.put(iAudioServerStateDispatcher.asBinder(), asdProxy);
        }
    }

    public void unregisterAudioServerStateDispatcher(IAudioServerStateDispatcher iAudioServerStateDispatcher) {
        checkMonitorAudioServerStatePermission();
        synchronized (this.mAudioServerStateListeners) {
            AsdProxy asdProxyRemove = this.mAudioServerStateListeners.remove(iAudioServerStateDispatcher.asBinder());
            if (asdProxyRemove == null) {
                Slog.w(TAG, "Trying to unregister unknown audioserver state dispatcher for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid());
                return;
            }
            iAudioServerStateDispatcher.asBinder().unlinkToDeath(asdProxyRemove, 0);
        }
    }

    public boolean isAudioServerRunning() {
        checkMonitorAudioServerStatePermission();
        return AudioSystem.checkAudioFlinger() == 0;
    }
}
