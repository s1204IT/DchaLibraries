package android.media;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.IAudioFocusDispatcher;
import android.media.IAudioServerStateDispatcher;
import android.media.IAudioService;
import android.media.IPlaybackConfigDispatcher;
import android.media.IRecordingConfigDispatcher;
import android.media.audiopolicy.AudioPolicy;
import android.media.session.MediaSessionLegacyHelper;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class AudioManager {
    public static final String ACTION_AUDIO_BECOMING_NOISY = "android.media.AUDIO_BECOMING_NOISY";
    public static final String ACTION_HDMI_AUDIO_PLUG = "android.media.action.HDMI_AUDIO_PLUG";
    public static final String ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    public static final String ACTION_MICROPHONE_MUTE_CHANGED = "android.media.action.MICROPHONE_MUTE_CHANGED";

    @Deprecated
    public static final String ACTION_SCO_AUDIO_STATE_CHANGED = "android.media.SCO_AUDIO_STATE_CHANGED";
    public static final String ACTION_SCO_AUDIO_STATE_UPDATED = "android.media.ACTION_SCO_AUDIO_STATE_UPDATED";
    public static final int ADJUST_LOWER = -1;
    public static final int ADJUST_MUTE = -100;
    public static final int ADJUST_RAISE = 1;
    public static final int ADJUST_SAME = 0;
    public static final int ADJUST_TOGGLE_MUTE = 101;
    public static final int ADJUST_UNMUTE = 100;
    public static final int AUDIOFOCUS_FLAGS_APPS = 3;
    public static final int AUDIOFOCUS_FLAGS_SYSTEM = 7;

    @SystemApi
    public static final int AUDIOFOCUS_FLAG_DELAY_OK = 1;

    @SystemApi
    public static final int AUDIOFOCUS_FLAG_LOCK = 4;

    @SystemApi
    public static final int AUDIOFOCUS_FLAG_PAUSES_ON_DUCKABLE_LOSS = 2;
    public static final int AUDIOFOCUS_GAIN = 1;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
    public static final int AUDIOFOCUS_LOSS = -1;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT = -2;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = -3;
    public static final int AUDIOFOCUS_NONE = 0;
    public static final int AUDIOFOCUS_REQUEST_DELAYED = 2;
    public static final int AUDIOFOCUS_REQUEST_FAILED = 0;
    public static final int AUDIOFOCUS_REQUEST_GRANTED = 1;
    public static final int AUDIOFOCUS_REQUEST_WAITING_FOR_EXT_POLICY = 100;
    static final int AUDIOPORT_GENERATION_INIT = 0;
    public static final int AUDIO_SESSION_ID_GENERATE = 0;
    public static final int DEVICE_IN_ANLG_DOCK_HEADSET = -2147483136;
    public static final int DEVICE_IN_BACK_MIC = -2147483520;
    public static final int DEVICE_IN_BLUETOOTH_SCO_HEADSET = -2147483640;
    public static final int DEVICE_IN_BUILTIN_MIC = -2147483644;
    public static final int DEVICE_IN_DGTL_DOCK_HEADSET = -2147482624;
    public static final int DEVICE_IN_FM_TUNER = -2147475456;
    public static final int DEVICE_IN_HDMI = -2147483616;
    public static final int DEVICE_IN_LINE = -2147450880;
    public static final int DEVICE_IN_LOOPBACK = -2147221504;
    public static final int DEVICE_IN_SPDIF = -2147418112;
    public static final int DEVICE_IN_TELEPHONY_RX = -2147483584;
    public static final int DEVICE_IN_TV_TUNER = -2147467264;
    public static final int DEVICE_IN_USB_ACCESSORY = -2147481600;
    public static final int DEVICE_IN_USB_DEVICE = -2147479552;
    public static final int DEVICE_IN_WIRED_HEADSET = -2147483632;
    public static final int DEVICE_NONE = 0;
    public static final int DEVICE_OUT_ANLG_DOCK_HEADSET = 2048;
    public static final int DEVICE_OUT_AUX_DIGITAL = 1024;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP = 128;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES = 256;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER = 512;
    public static final int DEVICE_OUT_BLUETOOTH_SCO = 16;
    public static final int DEVICE_OUT_BLUETOOTH_SCO_CARKIT = 64;
    public static final int DEVICE_OUT_BLUETOOTH_SCO_HEADSET = 32;
    public static final int DEVICE_OUT_DEFAULT = 1073741824;
    public static final int DEVICE_OUT_DGTL_DOCK_HEADSET = 4096;
    public static final int DEVICE_OUT_EARPIECE = 1;
    public static final int DEVICE_OUT_FM = 1048576;
    public static final int DEVICE_OUT_HDMI = 1024;
    public static final int DEVICE_OUT_HDMI_ARC = 262144;
    public static final int DEVICE_OUT_LINE = 131072;
    public static final int DEVICE_OUT_REMOTE_SUBMIX = 32768;
    public static final int DEVICE_OUT_SPDIF = 524288;
    public static final int DEVICE_OUT_SPEAKER = 2;
    public static final int DEVICE_OUT_TELEPHONY_TX = 65536;
    public static final int DEVICE_OUT_USB_ACCESSORY = 8192;
    public static final int DEVICE_OUT_USB_DEVICE = 16384;
    public static final int DEVICE_OUT_USB_HEADSET = 67108864;
    public static final int DEVICE_OUT_WIRED_HEADPHONE = 8;
    public static final int DEVICE_OUT_WIRED_HEADSET = 4;
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_DEAD_OBJECT = -6;
    public static final int ERROR_INVALID_OPERATION = -3;
    public static final int ERROR_NO_INIT = -5;
    public static final int ERROR_PERMISSION_DENIED = -4;
    public static final String EXTRA_AUDIO_PLUG_STATE = "android.media.extra.AUDIO_PLUG_STATE";
    public static final String EXTRA_ENCODINGS = "android.media.extra.ENCODINGS";
    public static final String EXTRA_MASTER_VOLUME_MUTED = "android.media.EXTRA_MASTER_VOLUME_MUTED";
    public static final String EXTRA_MAX_CHANNEL_COUNT = "android.media.extra.MAX_CHANNEL_COUNT";
    public static final String EXTRA_PREV_VOLUME_STREAM_DEVICES = "android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES";
    public static final String EXTRA_PREV_VOLUME_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";
    public static final String EXTRA_RINGER_MODE = "android.media.EXTRA_RINGER_MODE";
    public static final String EXTRA_SCO_AUDIO_PREVIOUS_STATE = "android.media.extra.SCO_AUDIO_PREVIOUS_STATE";
    public static final String EXTRA_SCO_AUDIO_STATE = "android.media.extra.SCO_AUDIO_STATE";
    public static final String EXTRA_STREAM_VOLUME_MUTED = "android.media.EXTRA_STREAM_VOLUME_MUTED";
    public static final String EXTRA_VIBRATE_SETTING = "android.media.EXTRA_VIBRATE_SETTING";
    public static final String EXTRA_VIBRATE_TYPE = "android.media.EXTRA_VIBRATE_TYPE";
    public static final String EXTRA_VOLUME_STREAM_DEVICES = "android.media.EXTRA_VOLUME_STREAM_DEVICES";
    public static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    public static final String EXTRA_VOLUME_STREAM_TYPE_ALIAS = "android.media.EXTRA_VOLUME_STREAM_TYPE_ALIAS";
    public static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
    private static final int EXT_FOCUS_POLICY_TIMEOUT_MS = 200;
    public static final int FLAG_ACTIVE_MEDIA_ONLY = 512;
    public static final int FLAG_ALLOW_RINGER_MODES = 2;
    public static final int FLAG_BLUETOOTH_ABS_VOLUME = 64;
    public static final int FLAG_FIXED_VOLUME = 32;
    public static final int FLAG_FROM_KEY = 4096;
    public static final int FLAG_HDMI_SYSTEM_AUDIO_VOLUME = 256;
    public static final int FLAG_PLAY_SOUND = 4;
    public static final int FLAG_REMOVE_SOUND_AND_VIBRATE = 8;
    public static final int FLAG_SHOW_SILENT_HINT = 128;
    public static final int FLAG_SHOW_UI = 1;
    public static final int FLAG_SHOW_UI_WARNINGS = 1024;
    public static final int FLAG_SHOW_VIBRATE_HINT = 2048;
    public static final int FLAG_VIBRATE = 16;
    private static final String FOCUS_CLIENT_ID_STRING = "android_audio_focus_client_id";
    public static final int FX_FOCUS_NAVIGATION_DOWN = 2;
    public static final int FX_FOCUS_NAVIGATION_LEFT = 3;
    public static final int FX_FOCUS_NAVIGATION_RIGHT = 4;
    public static final int FX_FOCUS_NAVIGATION_UP = 1;
    public static final int FX_KEYPRESS_DELETE = 7;
    public static final int FX_KEYPRESS_INVALID = 9;
    public static final int FX_KEYPRESS_RETURN = 8;
    public static final int FX_KEYPRESS_SPACEBAR = 6;
    public static final int FX_KEYPRESS_STANDARD = 5;
    public static final int FX_KEY_CLICK = 0;
    public static final int GET_DEVICES_ALL = 3;
    public static final int GET_DEVICES_INPUTS = 1;
    public static final int GET_DEVICES_OUTPUTS = 2;
    public static final String INTERNAL_RINGER_MODE_CHANGED_ACTION = "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION";
    public static final String MASTER_MUTE_CHANGED_ACTION = "android.media.MASTER_MUTE_CHANGED_ACTION";
    public static final int MODE_CURRENT = -1;
    public static final int MODE_INVALID = -2;
    public static final int MODE_IN_CALL = 2;
    public static final int MODE_IN_COMMUNICATION = 3;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_RINGTONE = 1;
    private static final int MSG_DEVICES_CALLBACK_REGISTERED = 0;
    private static final int MSG_DEVICES_DEVICES_ADDED = 1;
    private static final int MSG_DEVICES_DEVICES_REMOVED = 2;
    private static final int MSSG_FOCUS_CHANGE = 0;
    private static final int MSSG_PLAYBACK_CONFIG_CHANGE = 2;
    private static final int MSSG_RECORDING_CONFIG_CHANGE = 1;
    public static final int NUM_SOUND_EFFECTS = 10;

    @Deprecated
    public static final int NUM_STREAMS = 5;
    public static final String PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER";
    public static final String PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE";
    public static final String PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED = "android.media.property.SUPPORT_AUDIO_SOURCE_UNPROCESSED";
    public static final String PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND = "android.media.property.SUPPORT_MIC_NEAR_ULTRASOUND";
    public static final String PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND = "android.media.property.SUPPORT_SPEAKER_NEAR_ULTRASOUND";
    public static final int RECORD_CONFIG_EVENT_START = 1;
    public static final int RECORD_CONFIG_EVENT_STOP = 0;
    public static final String RINGER_MODE_CHANGED_ACTION = "android.media.RINGER_MODE_CHANGED";
    public static final int RINGER_MODE_MAX = 2;
    public static final int RINGER_MODE_NORMAL = 2;
    public static final int RINGER_MODE_SILENT = 0;
    public static final int RINGER_MODE_VIBRATE = 1;

    @Deprecated
    public static final int ROUTE_ALL = -1;

    @Deprecated
    public static final int ROUTE_BLUETOOTH = 4;

    @Deprecated
    public static final int ROUTE_BLUETOOTH_A2DP = 16;

    @Deprecated
    public static final int ROUTE_BLUETOOTH_SCO = 4;

    @Deprecated
    public static final int ROUTE_EARPIECE = 1;

    @Deprecated
    public static final int ROUTE_HEADSET = 8;

    @Deprecated
    public static final int ROUTE_SPEAKER = 2;
    public static final int SCO_AUDIO_STATE_CONNECTED = 1;
    public static final int SCO_AUDIO_STATE_CONNECTING = 2;
    public static final int SCO_AUDIO_STATE_DISCONNECTED = 0;
    public static final int SCO_AUDIO_STATE_ERROR = -1;
    public static final int STREAM_ACCESSIBILITY = 10;
    public static final int STREAM_ALARM = 4;
    public static final int STREAM_BLUETOOTH_SCO = 6;
    public static final String STREAM_DEVICES_CHANGED_ACTION = "android.media.STREAM_DEVICES_CHANGED_ACTION";
    public static final int STREAM_DTMF = 8;
    public static final int STREAM_MUSIC = 3;
    public static final String STREAM_MUTE_CHANGED_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION";
    public static final int STREAM_NOTIFICATION = 5;
    public static final int STREAM_RING = 2;
    public static final int STREAM_SYSTEM = 1;
    public static final int STREAM_SYSTEM_ENFORCED = 7;
    public static final int STREAM_TTS = 9;
    public static final int STREAM_VOICE_CALL = 0;
    public static final int SUCCESS = 0;
    private static final String TAG = "AudioManager";
    public static final int USE_DEFAULT_STREAM_TYPE = Integer.MIN_VALUE;
    public static final String VIBRATE_SETTING_CHANGED_ACTION = "android.media.VIBRATE_SETTING_CHANGED";
    public static final int VIBRATE_SETTING_OFF = 0;
    public static final int VIBRATE_SETTING_ON = 1;
    public static final int VIBRATE_SETTING_ONLY_SILENT = 2;
    public static final int VIBRATE_TYPE_NOTIFICATION = 1;
    public static final int VIBRATE_TYPE_RINGER = 0;
    public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final float VOLUME_MIN_DB = -758.0f;
    private static IAudioService sService;
    private Context mApplicationContext;
    private final IAudioFocusDispatcher mAudioFocusDispatcher;
    private final ConcurrentHashMap<String, FocusRequestInfo> mAudioFocusIdListenerMap;
    private AudioServerStateCallback mAudioServerStateCb;
    private final Object mAudioServerStateCbLock;
    private final IAudioServerStateDispatcher mAudioServerStateDispatcher;
    private Executor mAudioServerStateExec;
    private final ArrayMap<AudioDeviceCallback, NativeEventHandlerDelegate> mDeviceCallbacks;

    @GuardedBy("mFocusRequestsLock")
    private HashMap<String, BlockingFocusResultReceiver> mFocusRequestsAwaitingResult;
    private final Object mFocusRequestsLock;
    private final IBinder mICallBack;
    private Context mOriginalContext;
    private final IPlaybackConfigDispatcher mPlayCb;
    private List<AudioPlaybackCallbackInfo> mPlaybackCallbackList;
    private final Object mPlaybackCallbackLock;
    private OnAmPortUpdateListener mPortListener;
    private ArrayList<AudioDevicePort> mPreviousPorts;
    private final IRecordingConfigDispatcher mRecCb;
    private List<AudioRecordingCallbackInfo> mRecordCallbackList;
    private final Object mRecordCallbackLock;
    private final ServiceEventHandlerDelegate mServiceEventHandlerDelegate;
    private final boolean mUseFixedVolume;
    private final boolean mUseVolumeKeySounds;
    private long mVolumeKeyUpTime;
    private static final boolean DEBUG = !"user".equals(Build.TYPE);
    private static final AudioPortEventHandler sAudioPortEventHandler = new AudioPortEventHandler();
    private static final String[] FLAG_NAMES = {"FLAG_SHOW_UI", "FLAG_ALLOW_RINGER_MODES", "FLAG_PLAY_SOUND", "FLAG_REMOVE_SOUND_AND_VIBRATE", "FLAG_VIBRATE", "FLAG_FIXED_VOLUME", "FLAG_BLUETOOTH_ABS_VOLUME", "FLAG_SHOW_SILENT_HINT", "FLAG_HDMI_SYSTEM_AUDIO_VOLUME", "FLAG_ACTIVE_MEDIA_ONLY", "FLAG_SHOW_UI_WARNINGS", "FLAG_SHOW_VIBRATE_HINT", "FLAG_FROM_KEY"};
    static Integer sAudioPortGeneration = new Integer(0);
    static ArrayList<AudioPort> sAudioPortsCached = new ArrayList<>();
    static ArrayList<AudioPort> sPreviousAudioPortsCached = new ArrayList<>();
    static ArrayList<AudioPatch> sAudioPatchesCached = new ArrayList<>();

    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusRequestResult {
    }

    public interface OnAudioFocusChangeListener {
        void onAudioFocusChange(int i);
    }

    public interface OnAudioPortUpdateListener {
        void onAudioPatchListUpdate(AudioPatch[] audioPatchArr);

        void onAudioPortListUpdate(AudioPort[] audioPortArr);

        void onServiceDied();
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PublicStreamTypes {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface VolumeAdjustment {
    }

    public static final String adjustToString(int i) {
        if (i != -100) {
            switch (i) {
                case -1:
                    return "ADJUST_LOWER";
                case 0:
                    return "ADJUST_SAME";
                case 1:
                    return "ADJUST_RAISE";
                default:
                    switch (i) {
                        case 100:
                            return "ADJUST_UNMUTE";
                        case 101:
                            return "ADJUST_TOGGLE_MUTE";
                        default:
                            return "unknown adjust mode " + i;
                    }
            }
        }
        return "ADJUST_MUTE";
    }

    public static String flagsToString(int i) {
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < FLAG_NAMES.length; i2++) {
            int i3 = 1 << i2;
            if ((i & i3) != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(FLAG_NAMES[i2]);
                i &= ~i3;
            }
        }
        if (i != 0) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        return sb.toString();
    }

    public AudioManager() {
        this.mAudioFocusIdListenerMap = new ConcurrentHashMap<>();
        this.mServiceEventHandlerDelegate = new ServiceEventHandlerDelegate(null);
        this.mAudioFocusDispatcher = new IAudioFocusDispatcher.Stub() {
            @Override
            public void dispatchAudioFocusChange(int i, String str) {
                FocusRequestInfo focusRequestInfoFindFocusRequestInfo = AudioManager.this.findFocusRequestInfo(str);
                if (focusRequestInfoFindFocusRequestInfo != null && focusRequestInfoFindFocusRequestInfo.mRequest.getOnAudioFocusChangeListener() != null) {
                    Handler handler = focusRequestInfoFindFocusRequestInfo.mHandler == null ? AudioManager.this.mServiceEventHandlerDelegate.getHandler() : focusRequestInfoFindFocusRequestInfo.mHandler;
                    handler.sendMessage(handler.obtainMessage(0, i, 0, str));
                }
            }

            @Override
            public void dispatchFocusResultFromExtPolicy(int i, String str) {
                synchronized (AudioManager.this.mFocusRequestsLock) {
                    BlockingFocusResultReceiver blockingFocusResultReceiver = (BlockingFocusResultReceiver) AudioManager.this.mFocusRequestsAwaitingResult.remove(str);
                    if (blockingFocusResultReceiver != null) {
                        blockingFocusResultReceiver.notifyResult(i);
                    } else {
                        Log.e(AudioManager.TAG, "dispatchFocusResultFromExtPolicy found no result receiver");
                    }
                }
            }
        };
        this.mFocusRequestsLock = new Object();
        this.mPlaybackCallbackLock = new Object();
        this.mPlayCb = new IPlaybackConfigDispatcher.Stub() {
            @Override
            public void dispatchPlaybackConfigChange(List<AudioPlaybackConfiguration> list, boolean z) {
                if (z) {
                    Binder.flushPendingCommands();
                }
                synchronized (AudioManager.this.mPlaybackCallbackLock) {
                    if (AudioManager.this.mPlaybackCallbackList != null) {
                        for (int i = 0; i < AudioManager.this.mPlaybackCallbackList.size(); i++) {
                            AudioPlaybackCallbackInfo audioPlaybackCallbackInfo = (AudioPlaybackCallbackInfo) AudioManager.this.mPlaybackCallbackList.get(i);
                            if (audioPlaybackCallbackInfo.mHandler != null) {
                                audioPlaybackCallbackInfo.mHandler.sendMessage(audioPlaybackCallbackInfo.mHandler.obtainMessage(2, new PlaybackConfigChangeCallbackData(audioPlaybackCallbackInfo.mCb, list)));
                            }
                        }
                    }
                }
            }
        };
        this.mRecordCallbackLock = new Object();
        this.mRecCb = new IRecordingConfigDispatcher.Stub() {
            @Override
            public void dispatchRecordingConfigChange(List<AudioRecordingConfiguration> list) {
                synchronized (AudioManager.this.mRecordCallbackLock) {
                    if (AudioManager.this.mRecordCallbackList != null) {
                        for (int i = 0; i < AudioManager.this.mRecordCallbackList.size(); i++) {
                            AudioRecordingCallbackInfo audioRecordingCallbackInfo = (AudioRecordingCallbackInfo) AudioManager.this.mRecordCallbackList.get(i);
                            if (audioRecordingCallbackInfo.mHandler != null) {
                                audioRecordingCallbackInfo.mHandler.sendMessage(audioRecordingCallbackInfo.mHandler.obtainMessage(1, new RecordConfigChangeCallbackData(audioRecordingCallbackInfo.mCb, list)));
                            }
                        }
                    }
                }
            }
        };
        this.mICallBack = new Binder();
        this.mPortListener = null;
        this.mDeviceCallbacks = new ArrayMap<>();
        this.mPreviousPorts = new ArrayList<>();
        this.mAudioServerStateCbLock = new Object();
        this.mAudioServerStateDispatcher = new AnonymousClass4();
        this.mUseVolumeKeySounds = true;
        this.mUseFixedVolume = false;
    }

    public AudioManager(Context context) {
        this.mAudioFocusIdListenerMap = new ConcurrentHashMap<>();
        this.mServiceEventHandlerDelegate = new ServiceEventHandlerDelegate(null);
        this.mAudioFocusDispatcher = new IAudioFocusDispatcher.Stub() {
            @Override
            public void dispatchAudioFocusChange(int i, String str) {
                FocusRequestInfo focusRequestInfoFindFocusRequestInfo = AudioManager.this.findFocusRequestInfo(str);
                if (focusRequestInfoFindFocusRequestInfo != null && focusRequestInfoFindFocusRequestInfo.mRequest.getOnAudioFocusChangeListener() != null) {
                    Handler handler = focusRequestInfoFindFocusRequestInfo.mHandler == null ? AudioManager.this.mServiceEventHandlerDelegate.getHandler() : focusRequestInfoFindFocusRequestInfo.mHandler;
                    handler.sendMessage(handler.obtainMessage(0, i, 0, str));
                }
            }

            @Override
            public void dispatchFocusResultFromExtPolicy(int i, String str) {
                synchronized (AudioManager.this.mFocusRequestsLock) {
                    BlockingFocusResultReceiver blockingFocusResultReceiver = (BlockingFocusResultReceiver) AudioManager.this.mFocusRequestsAwaitingResult.remove(str);
                    if (blockingFocusResultReceiver != null) {
                        blockingFocusResultReceiver.notifyResult(i);
                    } else {
                        Log.e(AudioManager.TAG, "dispatchFocusResultFromExtPolicy found no result receiver");
                    }
                }
            }
        };
        this.mFocusRequestsLock = new Object();
        this.mPlaybackCallbackLock = new Object();
        this.mPlayCb = new IPlaybackConfigDispatcher.Stub() {
            @Override
            public void dispatchPlaybackConfigChange(List<AudioPlaybackConfiguration> list, boolean z) {
                if (z) {
                    Binder.flushPendingCommands();
                }
                synchronized (AudioManager.this.mPlaybackCallbackLock) {
                    if (AudioManager.this.mPlaybackCallbackList != null) {
                        for (int i = 0; i < AudioManager.this.mPlaybackCallbackList.size(); i++) {
                            AudioPlaybackCallbackInfo audioPlaybackCallbackInfo = (AudioPlaybackCallbackInfo) AudioManager.this.mPlaybackCallbackList.get(i);
                            if (audioPlaybackCallbackInfo.mHandler != null) {
                                audioPlaybackCallbackInfo.mHandler.sendMessage(audioPlaybackCallbackInfo.mHandler.obtainMessage(2, new PlaybackConfigChangeCallbackData(audioPlaybackCallbackInfo.mCb, list)));
                            }
                        }
                    }
                }
            }
        };
        this.mRecordCallbackLock = new Object();
        this.mRecCb = new IRecordingConfigDispatcher.Stub() {
            @Override
            public void dispatchRecordingConfigChange(List<AudioRecordingConfiguration> list) {
                synchronized (AudioManager.this.mRecordCallbackLock) {
                    if (AudioManager.this.mRecordCallbackList != null) {
                        for (int i = 0; i < AudioManager.this.mRecordCallbackList.size(); i++) {
                            AudioRecordingCallbackInfo audioRecordingCallbackInfo = (AudioRecordingCallbackInfo) AudioManager.this.mRecordCallbackList.get(i);
                            if (audioRecordingCallbackInfo.mHandler != null) {
                                audioRecordingCallbackInfo.mHandler.sendMessage(audioRecordingCallbackInfo.mHandler.obtainMessage(1, new RecordConfigChangeCallbackData(audioRecordingCallbackInfo.mCb, list)));
                            }
                        }
                    }
                }
            }
        };
        this.mICallBack = new Binder();
        this.mPortListener = null;
        this.mDeviceCallbacks = new ArrayMap<>();
        this.mPreviousPorts = new ArrayList<>();
        this.mAudioServerStateCbLock = new Object();
        this.mAudioServerStateDispatcher = new AnonymousClass4();
        setContext(context);
        this.mUseVolumeKeySounds = getContext().getResources().getBoolean(R.bool.config_useVolumeKeySounds);
        this.mUseFixedVolume = getContext().getResources().getBoolean(R.bool.config_useFixedVolume);
    }

    private Context getContext() {
        if (this.mApplicationContext == null) {
            setContext(this.mOriginalContext);
        }
        if (this.mApplicationContext != null) {
            return this.mApplicationContext;
        }
        return this.mOriginalContext;
    }

    private void setContext(Context context) {
        this.mApplicationContext = context.getApplicationContext();
        if (this.mApplicationContext != null) {
            this.mOriginalContext = null;
        } else {
            this.mOriginalContext = context;
        }
    }

    private static IAudioService getService() {
        if (sService != null) {
            return sService;
        }
        sService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        return sService;
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent) {
        MediaSessionLegacyHelper.getHelper(getContext()).sendMediaButtonEvent(keyEvent, false);
    }

    public void preDispatchKeyEvent(KeyEvent keyEvent, int i) {
        int keyCode = keyEvent.getKeyCode();
        if (keyCode != 25 && keyCode != 24 && keyCode != 164 && this.mVolumeKeyUpTime + 300 > SystemClock.uptimeMillis()) {
            adjustSuggestedStreamVolume(0, i, 8);
        }
    }

    public boolean isVolumeFixed() {
        return this.mUseFixedVolume;
    }

    public void adjustStreamVolume(int i, int i2, int i3) {
        try {
            getService().adjustStreamVolume(i, i2, i3, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void adjustVolume(int i, int i2) {
        MediaSessionLegacyHelper.getHelper(getContext()).sendAdjustVolumeBy(Integer.MIN_VALUE, i, i2);
    }

    public void adjustSuggestedStreamVolume(int i, int i2, int i3) {
        MediaSessionLegacyHelper.getHelper(getContext()).sendAdjustVolumeBy(i2, i, i3);
    }

    public void setMasterMute(boolean z, int i) {
        try {
            getService().setMasterMute(z, i, getContext().getOpPackageName(), UserHandle.getCallingUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getRingerMode() {
        try {
            return getService().getRingerModeExternal();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static boolean isValidRingerMode(int i) {
        if (i < 0 || i > 2) {
            return false;
        }
        try {
            return getService().isValidRingerMode(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getStreamMaxVolume(int i) {
        try {
            return getService().getStreamMaxVolume(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getStreamMinVolume(int i) {
        if (!isPublicStreamType(i)) {
            throw new IllegalArgumentException("Invalid stream type " + i);
        }
        return getStreamMinVolumeInt(i);
    }

    public int getStreamMinVolumeInt(int i) {
        try {
            return getService().getStreamMinVolume(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getStreamVolume(int i) {
        try {
            return getService().getStreamVolume(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public float getStreamVolumeDb(int i, int i2, int i3) {
        if (!isPublicStreamType(i)) {
            throw new IllegalArgumentException("Invalid stream type " + i);
        }
        if (i2 > getStreamMaxVolume(i) || i2 < getStreamMinVolume(i)) {
            throw new IllegalArgumentException("Invalid stream volume index " + i2);
        }
        if (!AudioDeviceInfo.isValidAudioDeviceTypeOut(i3)) {
            throw new IllegalArgumentException("Invalid audio output device type " + i3);
        }
        float streamVolumeDB = AudioSystem.getStreamVolumeDB(i, i2, AudioDeviceInfo.convertDeviceTypeToInternalDevice(i3));
        if (streamVolumeDB <= VOLUME_MIN_DB) {
            return Float.NEGATIVE_INFINITY;
        }
        return streamVolumeDB;
    }

    private static boolean isPublicStreamType(int i) {
        if (i == 8 || i == 10) {
            return true;
        }
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    public int getLastAudibleStreamVolume(int i) {
        try {
            return getService().getLastAudibleStreamVolume(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getUiSoundsStreamType() {
        try {
            return getService().getUiSoundsStreamType();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRingerMode(int i) {
        if (!isValidRingerMode(i)) {
            return;
        }
        try {
            getService().setRingerModeExternal(i, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setStreamVolume(int i, int i2, int i3) {
        try {
            getService().setStreamVolume(i, i2, i3, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setStreamSolo(int i, boolean z) {
        Log.w(TAG, "setStreamSolo has been deprecated. Do not use.");
    }

    @Deprecated
    public void setStreamMute(int i, boolean z) {
        Log.w(TAG, "setStreamMute is deprecated. adjustStreamVolume should be used instead.");
        int i2 = z ? -100 : 100;
        if (i == Integer.MIN_VALUE) {
            adjustSuggestedStreamVolume(i2, i, 0);
        } else {
            adjustStreamVolume(i, i2, 0);
        }
    }

    public boolean isStreamMute(int i) {
        try {
            return getService().isStreamMute(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isMasterMute() {
        try {
            return getService().isMasterMute();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forceVolumeControlStream(int i) {
        try {
            getService().forceVolumeControlStream(i, this.mICallBack);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean shouldVibrate(int i) {
        try {
            return getService().shouldVibrate(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getVibrateSetting(int i) {
        try {
            return getService().getVibrateSetting(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setVibrateSetting(int i, int i2) {
        try {
            getService().setVibrateSetting(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setSpeakerphoneOn(boolean z) {
        try {
            getService().setSpeakerphoneOn(z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSpeakerphoneOn() {
        try {
            return getService().isSpeakerphoneOn();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isOffloadedPlaybackSupported(AudioFormat audioFormat) {
        return AudioSystem.isOffloadSupported(audioFormat);
    }

    public boolean isBluetoothScoAvailableOffCall() {
        return getContext().getResources().getBoolean(R.bool.config_bluetooth_sco_off_call);
    }

    public void startBluetoothSco() {
        if (DEBUG) {
            Log.d(TAG, "startBluetoothSco()");
        }
        try {
            getService().startBluetoothSco(this.mICallBack, getContext().getApplicationInfo().targetSdkVersion);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startBluetoothScoVirtualCall() {
        try {
            getService().startBluetoothScoVirtualCall(this.mICallBack);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void stopBluetoothSco() {
        if (DEBUG) {
            Log.d(TAG, "stopBluetoothSco()");
        }
        try {
            getService().stopBluetoothSco(this.mICallBack);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setBluetoothScoOn(boolean z) {
        if (DEBUG) {
            Log.d(TAG, "setBluetoothScoOn(" + z + ")");
        }
        try {
            getService().setBluetoothScoOn(z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isBluetoothScoOn() {
        if (DEBUG) {
            Log.d(TAG, "isBluetoothScoOn()");
        }
        try {
            return getService().isBluetoothScoOn();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setBluetoothA2dpOn(boolean z) {
    }

    public boolean isBluetoothA2dpOn() {
        return AudioSystem.getDeviceConnectionState(128, "") == 1 || AudioSystem.getDeviceConnectionState(256, "") == 1 || AudioSystem.getDeviceConnectionState(512, "") == 1;
    }

    @Deprecated
    public void setWiredHeadsetOn(boolean z) {
    }

    public boolean isWiredHeadsetOn() {
        if (AudioSystem.getDeviceConnectionState(4, "") == 0 && AudioSystem.getDeviceConnectionState(8, "") == 0 && AudioSystem.getDeviceConnectionState(67108864, "") == 0) {
            return false;
        }
        return true;
    }

    public void setMicrophoneMute(boolean z) {
        try {
            getService().setMicrophoneMute(z, getContext().getOpPackageName(), UserHandle.getCallingUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isMicrophoneMute() {
        return AudioSystem.isMicrophoneMuted();
    }

    public void setMode(int i) {
        try {
            getService().setMode(i, this.mICallBack, this.mApplicationContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getMode() {
        try {
            return getService().getMode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setRouting(int i, int i2, int i3) {
    }

    @Deprecated
    public int getRouting(int i) {
        return -1;
    }

    public boolean isMusicActive() {
        return AudioSystem.isStreamActive(3, 0);
    }

    public boolean isMusicActiveRemotely() {
        return AudioSystem.isStreamActiveRemotely(3, 0);
    }

    public boolean isAudioFocusExclusive() {
        try {
            return getService().getCurrentAudioFocus() == 4;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int generateAudioSessionId() {
        int iNewAudioSessionId = AudioSystem.newAudioSessionId();
        if (iNewAudioSessionId > 0) {
            return iNewAudioSessionId;
        }
        Log.e(TAG, "Failure to generate a new audio session ID");
        return -1;
    }

    @Deprecated
    public void setParameter(String str, String str2) {
        setParameters(str + "=" + str2);
    }

    public void setParameters(String str) {
        AudioSystem.setParameters(str);
    }

    public String getParameters(String str) {
        return AudioSystem.getParameters(str);
    }

    public void playSoundEffect(int i) {
        if (i < 0 || i >= 10 || !querySoundEffectsEnabled(Process.myUserHandle().getIdentifier())) {
            return;
        }
        try {
            getService().playSoundEffect(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void playSoundEffect(int i, int i2) {
        if (i < 0 || i >= 10 || !querySoundEffectsEnabled(i2)) {
            return;
        }
        try {
            getService().playSoundEffect(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void playSoundEffect(int i, float f) {
        if (i < 0 || i >= 10) {
            return;
        }
        try {
            getService().playSoundEffectVolume(i, f);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean querySoundEffectsEnabled(int i) {
        return Settings.System.getIntForUser(getContext().getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0, i) != 0;
    }

    public void loadSoundEffects() {
        try {
            getService().loadSoundEffects();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unloadSoundEffects() {
        try {
            getService().unloadSoundEffects();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class FocusRequestInfo {
        final Handler mHandler;
        final AudioFocusRequest mRequest;

        FocusRequestInfo(AudioFocusRequest audioFocusRequest, Handler handler) {
            this.mRequest = audioFocusRequest;
            this.mHandler = handler;
        }
    }

    private FocusRequestInfo findFocusRequestInfo(String str) {
        return this.mAudioFocusIdListenerMap.get(str);
    }

    private class ServiceEventHandlerDelegate {
        private final Handler mHandler;

        ServiceEventHandlerDelegate(Handler handler) {
            Looper looper;
            if (handler == null) {
                looper = Looper.myLooper();
                if (looper == null) {
                    looper = Looper.getMainLooper();
                }
            } else {
                looper = handler.getLooper();
            }
            if (looper != null) {
                this.mHandler = new Handler(looper) {
                    @Override
                    public void handleMessage(Message message) {
                        OnAudioFocusChangeListener onAudioFocusChangeListener;
                        switch (message.what) {
                            case 0:
                                FocusRequestInfo focusRequestInfoFindFocusRequestInfo = AudioManager.this.findFocusRequestInfo((String) message.obj);
                                if (focusRequestInfoFindFocusRequestInfo != null && (onAudioFocusChangeListener = focusRequestInfoFindFocusRequestInfo.mRequest.getOnAudioFocusChangeListener()) != null) {
                                    Log.d(AudioManager.TAG, "dispatching onAudioFocusChange(" + message.arg1 + ") to " + message.obj);
                                    onAudioFocusChangeListener.onAudioFocusChange(message.arg1);
                                    break;
                                }
                                break;
                            case 1:
                                RecordConfigChangeCallbackData recordConfigChangeCallbackData = (RecordConfigChangeCallbackData) message.obj;
                                if (recordConfigChangeCallbackData.mCb != null) {
                                    recordConfigChangeCallbackData.mCb.onRecordingConfigChanged(recordConfigChangeCallbackData.mConfigs);
                                }
                                break;
                            case 2:
                                PlaybackConfigChangeCallbackData playbackConfigChangeCallbackData = (PlaybackConfigChangeCallbackData) message.obj;
                                if (playbackConfigChangeCallbackData.mCb != null) {
                                    if (AudioManager.DEBUG) {
                                        Log.d(AudioManager.TAG, "dispatching onPlaybackConfigChanged()");
                                    }
                                    playbackConfigChangeCallbackData.mCb.onPlaybackConfigChanged(playbackConfigChangeCallbackData.mConfigs);
                                }
                                break;
                            default:
                                Log.e(AudioManager.TAG, "Unknown event " + message.what);
                                break;
                        }
                    }
                };
            } else {
                this.mHandler = null;
            }
        }

        Handler getHandler() {
            return this.mHandler;
        }
    }

    private String getIdForAudioFocusListener(OnAudioFocusChangeListener onAudioFocusChangeListener) {
        if (onAudioFocusChangeListener == null) {
            return new String(toString());
        }
        return new String(toString() + onAudioFocusChangeListener.toString());
    }

    public void registerAudioFocusRequest(AudioFocusRequest audioFocusRequest) {
        Handler onAudioFocusChangeListenerHandler = audioFocusRequest.getOnAudioFocusChangeListenerHandler();
        this.mAudioFocusIdListenerMap.put(getIdForAudioFocusListener(audioFocusRequest.getOnAudioFocusChangeListener()), new FocusRequestInfo(audioFocusRequest, onAudioFocusChangeListenerHandler == null ? null : new ServiceEventHandlerDelegate(onAudioFocusChangeListenerHandler).getHandler()));
    }

    public void unregisterAudioFocusRequest(OnAudioFocusChangeListener onAudioFocusChangeListener) {
        this.mAudioFocusIdListenerMap.remove(getIdForAudioFocusListener(onAudioFocusChangeListener));
    }

    public int requestAudioFocus(OnAudioFocusChangeListener onAudioFocusChangeListener, int i, int i2) {
        PlayerBase.deprecateStreamTypeForPlayback(i, TAG, "requestAudioFocus()");
        try {
            return requestAudioFocus(onAudioFocusChangeListener, new AudioAttributes.Builder().setInternalLegacyStreamType(i).build(), i2, 0);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Audio focus request denied due to ", e);
            return 0;
        }
    }

    public int requestAudioFocus(AudioFocusRequest audioFocusRequest) {
        return requestAudioFocus(audioFocusRequest, null);
    }

    public int abandonAudioFocusRequest(AudioFocusRequest audioFocusRequest) {
        if (audioFocusRequest == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusRequest");
        }
        return abandonAudioFocus(audioFocusRequest.getOnAudioFocusChangeListener(), audioFocusRequest.getAudioAttributes());
    }

    @SystemApi
    public int requestAudioFocus(OnAudioFocusChangeListener onAudioFocusChangeListener, AudioAttributes audioAttributes, int i, int i2) throws IllegalArgumentException {
        int i3 = i2 & 3;
        if (i2 != i3) {
            throw new IllegalArgumentException("Invalid flags 0x" + Integer.toHexString(i2).toUpperCase());
        }
        return requestAudioFocus(onAudioFocusChangeListener, audioAttributes, i, i3, null);
    }

    @SystemApi
    public int requestAudioFocus(OnAudioFocusChangeListener onAudioFocusChangeListener, AudioAttributes audioAttributes, int i, int i2, AudioPolicy audioPolicy) throws IllegalArgumentException {
        if (audioAttributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes argument");
        }
        if (!AudioFocusRequest.isValidFocusGain(i)) {
            throw new IllegalArgumentException("Invalid duration hint");
        }
        if (i2 != (i2 & 7)) {
            throw new IllegalArgumentException("Illegal flags 0x" + Integer.toHexString(i2).toUpperCase());
        }
        int i3 = i2 & 1;
        if (i3 == 1 && onAudioFocusChangeListener == null) {
            throw new IllegalArgumentException("Illegal null focus listener when flagged as accepting delayed focus grant");
        }
        int i4 = i2 & 2;
        if (i4 == 2 && onAudioFocusChangeListener == null) {
            throw new IllegalArgumentException("Illegal null focus listener when flagged as pausing instead of ducking");
        }
        int i5 = i2 & 4;
        if (i5 == 4 && audioPolicy == null) {
            throw new IllegalArgumentException("Illegal null audio policy when locking audio focus");
        }
        return requestAudioFocus(new AudioFocusRequest.Builder(i).setOnAudioFocusChangeListenerInt(onAudioFocusChangeListener, null).setAudioAttributes(audioAttributes).setAcceptsDelayedFocusGain(i3 == 1).setWillPauseWhenDucked(i4 == 2).setLocksFocus(i5 == 4).build(), audioPolicy);
    }

    @SystemApi
    public int requestAudioFocus(AudioFocusRequest audioFocusRequest, AudioPolicy audioPolicy) {
        int i;
        if (audioFocusRequest == null) {
            throw new NullPointerException("Illegal null AudioFocusRequest");
        }
        if (audioFocusRequest.locksFocus() && audioPolicy == null) {
            throw new IllegalArgumentException("Illegal null audio policy when locking audio focus");
        }
        registerAudioFocusRequest(audioFocusRequest);
        IAudioService service = getService();
        try {
            i = getContext().getApplicationInfo().targetSdkVersion;
        } catch (NullPointerException e) {
            i = Build.VERSION.SDK_INT;
        }
        int i2 = i;
        String idForAudioFocusListener = getIdForAudioFocusListener(audioFocusRequest.getOnAudioFocusChangeListener());
        synchronized (this.mFocusRequestsLock) {
            try {
                int iRequestAudioFocus = service.requestAudioFocus(audioFocusRequest.getAudioAttributes(), audioFocusRequest.getFocusGain(), this.mICallBack, this.mAudioFocusDispatcher, idForAudioFocusListener, getContext().getOpPackageName(), audioFocusRequest.getFlags(), audioPolicy != null ? audioPolicy.cb() : null, i2);
                if (iRequestAudioFocus != 100) {
                    return iRequestAudioFocus;
                }
                if (this.mFocusRequestsAwaitingResult == null) {
                    this.mFocusRequestsAwaitingResult = new HashMap<>(1);
                }
                BlockingFocusResultReceiver blockingFocusResultReceiver = new BlockingFocusResultReceiver(idForAudioFocusListener);
                this.mFocusRequestsAwaitingResult.put(idForAudioFocusListener, blockingFocusResultReceiver);
                blockingFocusResultReceiver.waitForResult(200L);
                if (DEBUG && !blockingFocusResultReceiver.receivedResult()) {
                    Log.e(TAG, "requestAudio response from ext policy timed out, denying request");
                }
                synchronized (this.mFocusRequestsLock) {
                    this.mFocusRequestsAwaitingResult.remove(idForAudioFocusListener);
                }
                return blockingFocusResultReceiver.requestResult();
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }
    }

    private static final class SafeWaitObject {
        private boolean mQuit;

        private SafeWaitObject() {
            this.mQuit = false;
        }

        public void safeNotify() {
            synchronized (this) {
                this.mQuit = true;
                notify();
            }
        }

        public void safeWait(long j) throws InterruptedException {
            long jCurrentTimeMillis = System.currentTimeMillis() + j;
            synchronized (this) {
                while (!this.mQuit) {
                    long jCurrentTimeMillis2 = jCurrentTimeMillis - System.currentTimeMillis();
                    if (jCurrentTimeMillis2 < 0) {
                        break;
                    } else {
                        wait(jCurrentTimeMillis2);
                    }
                }
            }
        }
    }

    private static final class BlockingFocusResultReceiver {
        private final String mFocusClientId;
        private final SafeWaitObject mLock = new SafeWaitObject();

        @GuardedBy("mLock")
        private boolean mResultReceived = false;
        private int mFocusRequestResult = 0;

        BlockingFocusResultReceiver(String str) {
            this.mFocusClientId = str;
        }

        boolean receivedResult() {
            return this.mResultReceived;
        }

        int requestResult() {
            return this.mFocusRequestResult;
        }

        void notifyResult(int i) {
            synchronized (this.mLock) {
                this.mResultReceived = true;
                this.mFocusRequestResult = i;
                this.mLock.safeNotify();
            }
        }

        public void waitForResult(long j) {
            synchronized (this.mLock) {
                if (this.mResultReceived) {
                    return;
                }
                try {
                    this.mLock.safeWait(j);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void requestAudioFocusForCall(int i, int i2) {
        try {
            getService().requestAudioFocus(new AudioAttributes.Builder().setInternalLegacyStreamType(i).build(), i2, this.mICallBack, null, AudioSystem.IN_VOICE_COMM_FOCUS_ID, getContext().getOpPackageName(), 4, null, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getFocusRampTimeMs(int i, AudioAttributes audioAttributes) {
        try {
            return getService().getFocusRampTimeMs(i, audioAttributes);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setFocusRequestResult(AudioFocusInfo audioFocusInfo, int i, AudioPolicy audioPolicy) {
        if (audioFocusInfo == null) {
            throw new IllegalArgumentException("Illegal null AudioFocusInfo");
        }
        if (audioPolicy == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy");
        }
        try {
            getService().setFocusRequestResultFromExtPolicy(audioFocusInfo, i, audioPolicy.cb());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int dispatchAudioFocusChange(AudioFocusInfo audioFocusInfo, int i, AudioPolicy audioPolicy) {
        if (audioFocusInfo == null) {
            throw new NullPointerException("Illegal null AudioFocusInfo");
        }
        if (audioPolicy == null) {
            throw new NullPointerException("Illegal null AudioPolicy");
        }
        try {
            return getService().dispatchFocusChange(audioFocusInfo, i, audioPolicy.cb());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void abandonAudioFocusForCall() {
        try {
            getService().abandonAudioFocus(null, AudioSystem.IN_VOICE_COMM_FOCUS_ID, null, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int abandonAudioFocus(OnAudioFocusChangeListener onAudioFocusChangeListener) {
        return abandonAudioFocus(onAudioFocusChangeListener, null);
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public int abandonAudioFocus(OnAudioFocusChangeListener onAudioFocusChangeListener, AudioAttributes audioAttributes) {
        unregisterAudioFocusRequest(onAudioFocusChangeListener);
        try {
            return getService().abandonAudioFocus(this.mAudioFocusDispatcher, getIdForAudioFocusListener(onAudioFocusChangeListener), audioAttributes, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void registerMediaButtonEventReceiver(ComponentName componentName) {
        if (componentName == null) {
            return;
        }
        if (!componentName.getPackageName().equals(getContext().getPackageName())) {
            Log.e(TAG, "registerMediaButtonEventReceiver() error: receiver and context package names don't match");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(componentName);
        registerMediaButtonIntent(PendingIntent.getBroadcast(getContext(), 0, intent, 0), componentName);
    }

    @Deprecated
    public void registerMediaButtonEventReceiver(PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            return;
        }
        registerMediaButtonIntent(pendingIntent, null);
    }

    public void registerMediaButtonIntent(PendingIntent pendingIntent, ComponentName componentName) {
        if (pendingIntent == null) {
            Log.e(TAG, "Cannot call registerMediaButtonIntent() with a null parameter");
        } else {
            MediaSessionLegacyHelper.getHelper(getContext()).addMediaButtonListener(pendingIntent, componentName, getContext());
        }
    }

    @Deprecated
    public void unregisterMediaButtonEventReceiver(ComponentName componentName) {
        if (componentName == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(componentName);
        unregisterMediaButtonIntent(PendingIntent.getBroadcast(getContext(), 0, intent, 0));
    }

    @Deprecated
    public void unregisterMediaButtonEventReceiver(PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            return;
        }
        unregisterMediaButtonIntent(pendingIntent);
    }

    public void unregisterMediaButtonIntent(PendingIntent pendingIntent) {
        MediaSessionLegacyHelper.getHelper(getContext()).removeMediaButtonListener(pendingIntent);
    }

    @Deprecated
    public void registerRemoteControlClient(RemoteControlClient remoteControlClient) {
        if (remoteControlClient == null || remoteControlClient.getRcMediaIntent() == null) {
            return;
        }
        remoteControlClient.registerWithSession(MediaSessionLegacyHelper.getHelper(getContext()));
    }

    @Deprecated
    public void unregisterRemoteControlClient(RemoteControlClient remoteControlClient) {
        if (remoteControlClient == null || remoteControlClient.getRcMediaIntent() == null) {
            return;
        }
        remoteControlClient.unregisterWithSession(MediaSessionLegacyHelper.getHelper(getContext()));
    }

    @Deprecated
    public boolean registerRemoteController(RemoteController remoteController) {
        if (remoteController == null) {
            return false;
        }
        remoteController.startListeningToSessions();
        return true;
    }

    @Deprecated
    public void unregisterRemoteController(RemoteController remoteController) {
        if (remoteController == null) {
            return;
        }
        remoteController.stopListeningToSessions();
    }

    @SystemApi
    public int registerAudioPolicy(AudioPolicy audioPolicy) {
        if (audioPolicy == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy argument");
        }
        try {
            String strRegisterAudioPolicy = getService().registerAudioPolicy(audioPolicy.getConfig(), audioPolicy.cb(), audioPolicy.hasFocusListener(), audioPolicy.isFocusPolicy(), audioPolicy.isVolumeController());
            if (strRegisterAudioPolicy == null) {
                return -1;
            }
            audioPolicy.setRegistration(strRegisterAudioPolicy);
            return 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void unregisterAudioPolicyAsync(AudioPolicy audioPolicy) {
        if (audioPolicy == null) {
            throw new IllegalArgumentException("Illegal null AudioPolicy argument");
        }
        try {
            getService().unregisterAudioPolicyAsync(audioPolicy.cb());
            audioPolicy.setRegistration(null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static abstract class AudioPlaybackCallback {
        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> list) {
        }
    }

    private static class AudioPlaybackCallbackInfo {
        final AudioPlaybackCallback mCb;
        final Handler mHandler;

        AudioPlaybackCallbackInfo(AudioPlaybackCallback audioPlaybackCallback, Handler handler) {
            this.mCb = audioPlaybackCallback;
            this.mHandler = handler;
        }
    }

    private static final class PlaybackConfigChangeCallbackData {
        final AudioPlaybackCallback mCb;
        final List<AudioPlaybackConfiguration> mConfigs;

        PlaybackConfigChangeCallbackData(AudioPlaybackCallback audioPlaybackCallback, List<AudioPlaybackConfiguration> list) {
            this.mCb = audioPlaybackCallback;
            this.mConfigs = list;
        }
    }

    public void registerAudioPlaybackCallback(AudioPlaybackCallback audioPlaybackCallback, Handler handler) {
        if (audioPlaybackCallback == null) {
            throw new IllegalArgumentException("Illegal null AudioPlaybackCallback argument");
        }
        synchronized (this.mPlaybackCallbackLock) {
            if (this.mPlaybackCallbackList == null) {
                this.mPlaybackCallbackList = new ArrayList();
            }
            int size = this.mPlaybackCallbackList.size();
            if (!hasPlaybackCallback_sync(audioPlaybackCallback)) {
                this.mPlaybackCallbackList.add(new AudioPlaybackCallbackInfo(audioPlaybackCallback, new ServiceEventHandlerDelegate(handler).getHandler()));
                int size2 = this.mPlaybackCallbackList.size();
                if (size == 0 && size2 > 0) {
                    try {
                        getService().registerPlaybackCallback(this.mPlayCb);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } else {
                Log.w(TAG, "attempt to call registerAudioPlaybackCallback() on a previouslyregistered callback");
            }
        }
    }

    public void unregisterAudioPlaybackCallback(AudioPlaybackCallback audioPlaybackCallback) {
        if (audioPlaybackCallback == null) {
            throw new IllegalArgumentException("Illegal null AudioPlaybackCallback argument");
        }
        synchronized (this.mPlaybackCallbackLock) {
            if (this.mPlaybackCallbackList == null) {
                Log.w(TAG, "attempt to call unregisterAudioPlaybackCallback() on a callback that was never registered");
                return;
            }
            int size = this.mPlaybackCallbackList.size();
            if (removePlaybackCallback_sync(audioPlaybackCallback)) {
                int size2 = this.mPlaybackCallbackList.size();
                if (size > 0 && size2 == 0) {
                    try {
                        getService().unregisterPlaybackCallback(this.mPlayCb);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } else {
                Log.w(TAG, "attempt to call unregisterAudioPlaybackCallback() on a callback already unregistered or never registered");
            }
        }
    }

    public List<AudioPlaybackConfiguration> getActivePlaybackConfigurations() {
        try {
            return getService().getActivePlaybackConfigurations();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean hasPlaybackCallback_sync(AudioPlaybackCallback audioPlaybackCallback) {
        if (this.mPlaybackCallbackList != null) {
            for (int i = 0; i < this.mPlaybackCallbackList.size(); i++) {
                if (audioPlaybackCallback.equals(this.mPlaybackCallbackList.get(i).mCb)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean removePlaybackCallback_sync(AudioPlaybackCallback audioPlaybackCallback) {
        if (this.mPlaybackCallbackList != null) {
            for (int i = 0; i < this.mPlaybackCallbackList.size(); i++) {
                if (audioPlaybackCallback.equals(this.mPlaybackCallbackList.get(i).mCb)) {
                    this.mPlaybackCallbackList.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public static abstract class AudioRecordingCallback {
        public void onRecordingConfigChanged(List<AudioRecordingConfiguration> list) {
        }
    }

    private static class AudioRecordingCallbackInfo {
        final AudioRecordingCallback mCb;
        final Handler mHandler;

        AudioRecordingCallbackInfo(AudioRecordingCallback audioRecordingCallback, Handler handler) {
            this.mCb = audioRecordingCallback;
            this.mHandler = handler;
        }
    }

    private static final class RecordConfigChangeCallbackData {
        final AudioRecordingCallback mCb;
        final List<AudioRecordingConfiguration> mConfigs;

        RecordConfigChangeCallbackData(AudioRecordingCallback audioRecordingCallback, List<AudioRecordingConfiguration> list) {
            this.mCb = audioRecordingCallback;
            this.mConfigs = list;
        }
    }

    public void registerAudioRecordingCallback(AudioRecordingCallback audioRecordingCallback, Handler handler) {
        if (audioRecordingCallback == null) {
            throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");
        }
        synchronized (this.mRecordCallbackLock) {
            if (this.mRecordCallbackList == null) {
                this.mRecordCallbackList = new ArrayList();
            }
            int size = this.mRecordCallbackList.size();
            if (!hasRecordCallback_sync(audioRecordingCallback)) {
                this.mRecordCallbackList.add(new AudioRecordingCallbackInfo(audioRecordingCallback, new ServiceEventHandlerDelegate(handler).getHandler()));
                int size2 = this.mRecordCallbackList.size();
                if (size == 0 && size2 > 0) {
                    try {
                        getService().registerRecordingCallback(this.mRecCb);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } else {
                Log.w(TAG, "attempt to call registerAudioRecordingCallback() on a previouslyregistered callback");
            }
        }
    }

    public void unregisterAudioRecordingCallback(AudioRecordingCallback audioRecordingCallback) {
        if (audioRecordingCallback == null) {
            throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");
        }
        synchronized (this.mRecordCallbackLock) {
            if (this.mRecordCallbackList == null) {
                return;
            }
            int size = this.mRecordCallbackList.size();
            if (removeRecordCallback_sync(audioRecordingCallback)) {
                int size2 = this.mRecordCallbackList.size();
                if (size > 0 && size2 == 0) {
                    try {
                        getService().unregisterRecordingCallback(this.mRecCb);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            } else {
                Log.w(TAG, "attempt to call unregisterAudioRecordingCallback() on a callback already unregistered or never registered");
            }
        }
    }

    public List<AudioRecordingConfiguration> getActiveRecordingConfigurations() {
        try {
            return getService().getActiveRecordingConfigurations();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean hasRecordCallback_sync(AudioRecordingCallback audioRecordingCallback) {
        if (this.mRecordCallbackList != null) {
            for (int i = 0; i < this.mRecordCallbackList.size(); i++) {
                if (audioRecordingCallback.equals(this.mRecordCallbackList.get(i).mCb)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean removeRecordCallback_sync(AudioRecordingCallback audioRecordingCallback) {
        if (this.mRecordCallbackList != null) {
            for (int i = 0; i < this.mRecordCallbackList.size(); i++) {
                if (audioRecordingCallback.equals(this.mRecordCallbackList.get(i).mCb)) {
                    this.mRecordCallbackList.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public void reloadAudioSettings() {
        try {
            getService().reloadAudioSettings();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void avrcpSupportsAbsoluteVolume(String str, boolean z) {
        try {
            getService().avrcpSupportsAbsoluteVolume(str, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSilentMode() {
        int ringerMode = getRingerMode();
        return ringerMode == 0 || ringerMode == 1;
    }

    public static boolean isOutputDevice(int i) {
        return (i & Integer.MIN_VALUE) == 0;
    }

    public static boolean isInputDevice(int i) {
        return (i & Integer.MIN_VALUE) == Integer.MIN_VALUE;
    }

    public int getDevicesForStream(int i) {
        if (i != 8 && i != 10) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    break;
                default:
                    return 0;
            }
        }
        return AudioSystem.getDevicesForStream(i);
    }

    public void setWiredDeviceConnectionState(int i, int i2, String str, String str2) {
        try {
            getService().setWiredDeviceConnectionState(i, i2, str, str2, this.mApplicationContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setHearingAidDeviceConnectionState(BluetoothDevice bluetoothDevice, int i) {
        try {
            getService().setHearingAidDeviceConnectionState(bluetoothDevice, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int setBluetoothA2dpDeviceConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) {
        try {
            return getService().setBluetoothA2dpDeviceConnectionState(bluetoothDevice, i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(BluetoothDevice bluetoothDevice, int i, int i2, boolean z, int i3) {
        try {
            return getService().setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(bluetoothDevice, i, i2, z, i3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void handleBluetoothA2dpDeviceConfigChange(BluetoothDevice bluetoothDevice) {
        try {
            getService().handleBluetoothA2dpDeviceConfigChange(bluetoothDevice);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public IRingtonePlayer getRingtonePlayer() {
        try {
            return getService().getRingtonePlayer();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getProperty(String str) {
        if (PROPERTY_OUTPUT_SAMPLE_RATE.equals(str)) {
            int primaryOutputSamplingRate = AudioSystem.getPrimaryOutputSamplingRate();
            if (primaryOutputSamplingRate > 0) {
                return Integer.toString(primaryOutputSamplingRate);
            }
            return null;
        }
        if (PROPERTY_OUTPUT_FRAMES_PER_BUFFER.equals(str)) {
            int primaryOutputFrameCount = AudioSystem.getPrimaryOutputFrameCount();
            if (primaryOutputFrameCount > 0) {
                return Integer.toString(primaryOutputFrameCount);
            }
            return null;
        }
        if (PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND.equals(str)) {
            return String.valueOf(getContext().getResources().getBoolean(R.bool.config_supportMicNearUltrasound));
        }
        if (PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND.equals(str)) {
            return String.valueOf(getContext().getResources().getBoolean(R.bool.config_supportSpeakerNearUltrasound));
        }
        if (PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED.equals(str)) {
            return String.valueOf(getContext().getResources().getBoolean(R.bool.config_supportAudioSourceUnprocessed));
        }
        return null;
    }

    public int getOutputLatency(int i) {
        return AudioSystem.getOutputLatency(i);
    }

    public void setVolumeController(IVolumeController iVolumeController) {
        try {
            getService().setVolumeController(iVolumeController);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void notifyVolumeControllerVisible(IVolumeController iVolumeController, boolean z) {
        try {
            getService().notifyVolumeControllerVisible(iVolumeController, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isStreamAffectedByRingerMode(int i) {
        try {
            return getService().isStreamAffectedByRingerMode(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isStreamAffectedByMute(int i) {
        try {
            return getService().isStreamAffectedByMute(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disableSafeMediaVolume() {
        try {
            getService().disableSafeMediaVolume(this.mApplicationContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRingerModeInternal(int i) {
        try {
            getService().setRingerModeInternal(i, getContext().getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getRingerModeInternal() {
        try {
            return getService().getRingerModeInternal();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setVolumePolicy(VolumePolicy volumePolicy) {
        try {
            getService().setVolumePolicy(volumePolicy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int setHdmiSystemAudioSupported(boolean z) {
        try {
            return getService().setHdmiSystemAudioSupported(z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public boolean isHdmiSystemAudioSupported() {
        try {
            return getService().isHdmiSystemAudioSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int listAudioPorts(ArrayList<AudioPort> arrayList) {
        return updateAudioPortCache(arrayList, null, null);
    }

    public static int listPreviousAudioPorts(ArrayList<AudioPort> arrayList) {
        return updateAudioPortCache(null, null, arrayList);
    }

    public static int listAudioDevicePorts(ArrayList<AudioDevicePort> arrayList) {
        if (arrayList == null) {
            return -2;
        }
        ArrayList arrayList2 = new ArrayList();
        int iUpdateAudioPortCache = updateAudioPortCache(arrayList2, null, null);
        if (iUpdateAudioPortCache == 0) {
            filterDevicePorts(arrayList2, arrayList);
        }
        return iUpdateAudioPortCache;
    }

    public static int listPreviousAudioDevicePorts(ArrayList<AudioDevicePort> arrayList) {
        if (arrayList == null) {
            return -2;
        }
        ArrayList arrayList2 = new ArrayList();
        int iUpdateAudioPortCache = updateAudioPortCache(null, null, arrayList2);
        if (iUpdateAudioPortCache == 0) {
            filterDevicePorts(arrayList2, arrayList);
        }
        return iUpdateAudioPortCache;
    }

    private static void filterDevicePorts(ArrayList<AudioPort> arrayList, ArrayList<AudioDevicePort> arrayList2) {
        arrayList2.clear();
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i) instanceof AudioDevicePort) {
                arrayList2.add((AudioDevicePort) arrayList.get(i));
            }
        }
    }

    public static int createAudioPatch(AudioPatch[] audioPatchArr, AudioPortConfig[] audioPortConfigArr, AudioPortConfig[] audioPortConfigArr2) {
        return AudioSystem.createAudioPatch(audioPatchArr, audioPortConfigArr, audioPortConfigArr2);
    }

    public static int releaseAudioPatch(AudioPatch audioPatch) {
        return AudioSystem.releaseAudioPatch(audioPatch);
    }

    public static int listAudioPatches(ArrayList<AudioPatch> arrayList) {
        return updateAudioPortCache(null, arrayList, null);
    }

    public static int setAudioPortGain(AudioPort audioPort, AudioGainConfig audioGainConfig) {
        if (audioPort == null || audioGainConfig == null) {
            return -2;
        }
        AudioPortConfig audioPortConfigActiveConfig = audioPort.activeConfig();
        AudioPortConfig audioPortConfig = new AudioPortConfig(audioPort, audioPortConfigActiveConfig.samplingRate(), audioPortConfigActiveConfig.channelMask(), audioPortConfigActiveConfig.format(), audioGainConfig);
        audioPortConfig.mConfigMask = 8;
        return AudioSystem.setAudioPortConfig(audioPortConfig);
    }

    public void registerAudioPortUpdateListener(OnAudioPortUpdateListener onAudioPortUpdateListener) {
        sAudioPortEventHandler.init();
        sAudioPortEventHandler.registerListener(onAudioPortUpdateListener);
    }

    public void unregisterAudioPortUpdateListener(OnAudioPortUpdateListener onAudioPortUpdateListener) {
        sAudioPortEventHandler.unregisterListener(onAudioPortUpdateListener);
    }

    static int resetAudioPortGeneration() {
        int iIntValue;
        synchronized (sAudioPortGeneration) {
            iIntValue = sAudioPortGeneration.intValue();
            sAudioPortGeneration = 0;
        }
        return iIntValue;
    }

    static int updateAudioPortCache(ArrayList<AudioPort> arrayList, ArrayList<AudioPatch> arrayList2, ArrayList<AudioPort> arrayList3) {
        sAudioPortEventHandler.init();
        synchronized (sAudioPortGeneration) {
            if (sAudioPortGeneration.intValue() == 0) {
                int[] iArr = new int[1];
                int[] iArr2 = new int[1];
                ArrayList<AudioPort> arrayList4 = new ArrayList<>();
                ArrayList<AudioPatch> arrayList5 = new ArrayList<>();
                while (true) {
                    arrayList4.clear();
                    int iListAudioPorts = AudioSystem.listAudioPorts(arrayList4, iArr2);
                    if (iListAudioPorts != 0) {
                        Log.w(TAG, "updateAudioPortCache: listAudioPorts failed");
                        return iListAudioPorts;
                    }
                    arrayList5.clear();
                    int iListAudioPatches = AudioSystem.listAudioPatches(arrayList5, iArr);
                    if (iListAudioPatches != 0) {
                        Log.w(TAG, "updateAudioPortCache: listAudioPatches failed");
                        return iListAudioPatches;
                    }
                    if (iArr[0] == iArr2[0] || (arrayList != null && arrayList2 != null)) {
                        break;
                    }
                }
            }
            if (arrayList != null) {
                arrayList.clear();
                arrayList.addAll(sAudioPortsCached);
            }
            if (arrayList2 != null) {
                arrayList2.clear();
                arrayList2.addAll(sAudioPatchesCached);
            }
            if (arrayList3 != null) {
                arrayList3.clear();
                arrayList3.addAll(sPreviousAudioPortsCached);
            }
            return 0;
        }
    }

    static AudioPortConfig updatePortConfig(AudioPortConfig audioPortConfig, ArrayList<AudioPort> arrayList) {
        AudioPort audioPortPort = audioPortConfig.port();
        int i = 0;
        while (true) {
            if (i >= arrayList.size()) {
                break;
            }
            if (!arrayList.get(i).handle().equals(audioPortPort.handle())) {
                i++;
            } else {
                audioPortPort = arrayList.get(i);
                break;
            }
        }
        if (i == arrayList.size()) {
            Log.e(TAG, "updatePortConfig port not found for handle: " + audioPortPort.handle().id());
            return null;
        }
        AudioGainConfig audioGainConfigGain = audioPortConfig.gain();
        if (audioGainConfigGain != null) {
            audioGainConfigGain = audioPortPort.gain(audioGainConfigGain.index()).buildConfig(audioGainConfigGain.mode(), audioGainConfigGain.channelMask(), audioGainConfigGain.values(), audioGainConfigGain.rampDurationMs());
        }
        return audioPortPort.buildConfig(audioPortConfig.samplingRate(), audioPortConfig.channelMask(), audioPortConfig.format(), audioGainConfigGain);
    }

    private static boolean checkFlags(AudioDevicePort audioDevicePort, int i) {
        if (audioDevicePort.role() != 2 || (i & 2) == 0) {
            return audioDevicePort.role() == 1 && (i & 1) != 0;
        }
        return true;
    }

    private static boolean checkTypes(AudioDevicePort audioDevicePort) {
        return AudioDeviceInfo.convertInternalDeviceToDeviceType(audioDevicePort.type()) != 0;
    }

    public AudioDeviceInfo[] getDevices(int i) {
        return getDevicesStatic(i);
    }

    private static AudioDeviceInfo[] infoListFromPortList(ArrayList<AudioDevicePort> arrayList, int i) {
        int i2 = 0;
        int i3 = 0;
        for (AudioDevicePort audioDevicePort : arrayList) {
            if (checkTypes(audioDevicePort) && checkFlags(audioDevicePort, i)) {
                i3++;
            }
        }
        AudioDeviceInfo[] audioDeviceInfoArr = new AudioDeviceInfo[i3];
        for (AudioDevicePort audioDevicePort2 : arrayList) {
            if (checkTypes(audioDevicePort2) && checkFlags(audioDevicePort2, i)) {
                audioDeviceInfoArr[i2] = new AudioDeviceInfo(audioDevicePort2);
                i2++;
            }
        }
        return audioDeviceInfoArr;
    }

    private static AudioDeviceInfo[] calcListDeltas(ArrayList<AudioDevicePort> arrayList, ArrayList<AudioDevicePort> arrayList2, int i) {
        ArrayList arrayList3 = new ArrayList();
        for (int i2 = 0; i2 < arrayList2.size(); i2++) {
            AudioDevicePort audioDevicePort = arrayList2.get(i2);
            boolean z = false;
            for (int i3 = 0; i3 < arrayList.size() && !z; i3++) {
                z = audioDevicePort.id() == arrayList.get(i3).id();
            }
            if (!z) {
                arrayList3.add(audioDevicePort);
            }
        }
        return infoListFromPortList(arrayList3, i);
    }

    public static AudioDeviceInfo[] getDevicesStatic(int i) {
        ArrayList arrayList = new ArrayList();
        if (listAudioDevicePorts(arrayList) != 0) {
            return new AudioDeviceInfo[0];
        }
        return infoListFromPortList(arrayList, i);
    }

    public void registerAudioDeviceCallback(AudioDeviceCallback audioDeviceCallback, Handler handler) {
        synchronized (this.mDeviceCallbacks) {
            if (audioDeviceCallback != null) {
                try {
                    if (!this.mDeviceCallbacks.containsKey(audioDeviceCallback)) {
                        if (this.mDeviceCallbacks.size() == 0) {
                            if (this.mPortListener == null) {
                                this.mPortListener = new OnAmPortUpdateListener();
                            }
                            registerAudioPortUpdateListener(this.mPortListener);
                        }
                        NativeEventHandlerDelegate nativeEventHandlerDelegate = new NativeEventHandlerDelegate(audioDeviceCallback, handler);
                        this.mDeviceCallbacks.put(audioDeviceCallback, nativeEventHandlerDelegate);
                        broadcastDeviceListChange_sync(nativeEventHandlerDelegate.getHandler());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    public void unregisterAudioDeviceCallback(AudioDeviceCallback audioDeviceCallback) {
        synchronized (this.mDeviceCallbacks) {
            if (this.mDeviceCallbacks.containsKey(audioDeviceCallback)) {
                this.mDeviceCallbacks.remove(audioDeviceCallback);
                if (this.mDeviceCallbacks.size() == 0) {
                    unregisterAudioPortUpdateListener(this.mPortListener);
                }
            }
        }
    }

    public static void setPortIdForMicrophones(ArrayList<MicrophoneInfo> arrayList) {
        AudioDeviceInfo[] devicesStatic = getDevicesStatic(1);
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            int length = devicesStatic.length;
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                AudioDeviceInfo audioDeviceInfo = devicesStatic[i];
                if (audioDeviceInfo.getPort().type() != arrayList.get(size).getInternalDeviceType() || !TextUtils.equals(audioDeviceInfo.getAddress(), arrayList.get(size).getAddress())) {
                    i++;
                } else {
                    arrayList.get(size).setId(audioDeviceInfo.getId());
                    z = true;
                    break;
                }
            }
            if (!z) {
                Log.i(TAG, "Failed to find port id for device with type:" + arrayList.get(size).getType() + " address:" + arrayList.get(size).getAddress());
                arrayList.remove(size);
            }
        }
    }

    public static MicrophoneInfo microphoneInfoFromAudioDeviceInfo(AudioDeviceInfo audioDeviceInfo) {
        int i;
        int type = audioDeviceInfo.getType();
        if (type == 15 || type == 18) {
            i = 1;
        } else {
            i = type == 0 ? 0 : 3;
        }
        MicrophoneInfo microphoneInfo = new MicrophoneInfo(audioDeviceInfo.getPort().name() + audioDeviceInfo.getId(), audioDeviceInfo.getPort().type(), audioDeviceInfo.getAddress(), i, -1, -1, MicrophoneInfo.POSITION_UNKNOWN, MicrophoneInfo.ORIENTATION_UNKNOWN, new ArrayList(), new ArrayList(), -3.4028235E38f, -3.4028235E38f, -3.4028235E38f, 0);
        microphoneInfo.setId(audioDeviceInfo.getId());
        return microphoneInfo;
    }

    private void addMicrophonesFromAudioDeviceInfo(ArrayList<MicrophoneInfo> arrayList, HashSet<Integer> hashSet) {
        for (AudioDeviceInfo audioDeviceInfo : getDevicesStatic(1)) {
            if (!hashSet.contains(Integer.valueOf(audioDeviceInfo.getType()))) {
                arrayList.add(microphoneInfoFromAudioDeviceInfo(audioDeviceInfo));
            }
        }
    }

    public List<MicrophoneInfo> getMicrophones() throws IOException {
        ArrayList<MicrophoneInfo> arrayList = new ArrayList<>();
        int microphones = AudioSystem.getMicrophones(arrayList);
        HashSet<Integer> hashSet = new HashSet<>();
        hashSet.add(18);
        if (microphones != 0) {
            if (microphones != -3) {
                Log.e(TAG, "getMicrophones failed:" + microphones);
            }
            Log.i(TAG, "fallback on device info");
            addMicrophonesFromAudioDeviceInfo(arrayList, hashSet);
            return arrayList;
        }
        setPortIdForMicrophones(arrayList);
        hashSet.add(15);
        addMicrophonesFromAudioDeviceInfo(arrayList, hashSet);
        return arrayList;
    }

    private void broadcastDeviceListChange_sync(Handler handler) {
        ArrayList<AudioDevicePort> arrayList = new ArrayList<>();
        if (listAudioDevicePorts(arrayList) != 0) {
            return;
        }
        if (handler != null) {
            handler.sendMessage(Message.obtain(handler, 0, infoListFromPortList(arrayList, 3)));
        } else {
            AudioDeviceInfo[] audioDeviceInfoArrCalcListDeltas = calcListDeltas(this.mPreviousPorts, arrayList, 3);
            AudioDeviceInfo[] audioDeviceInfoArrCalcListDeltas2 = calcListDeltas(arrayList, this.mPreviousPorts, 3);
            if (audioDeviceInfoArrCalcListDeltas.length != 0 || audioDeviceInfoArrCalcListDeltas2.length != 0) {
                for (int i = 0; i < this.mDeviceCallbacks.size(); i++) {
                    Handler handler2 = this.mDeviceCallbacks.valueAt(i).getHandler();
                    if (handler2 != null) {
                        if (audioDeviceInfoArrCalcListDeltas2.length != 0) {
                            handler2.sendMessage(Message.obtain(handler2, 2, audioDeviceInfoArrCalcListDeltas2));
                        }
                        if (audioDeviceInfoArrCalcListDeltas.length != 0) {
                            handler2.sendMessage(Message.obtain(handler2, 1, audioDeviceInfoArrCalcListDeltas));
                        }
                    }
                }
            }
        }
        this.mPreviousPorts = arrayList;
    }

    private class OnAmPortUpdateListener implements OnAudioPortUpdateListener {
        static final String TAG = "OnAmPortUpdateListener";

        private OnAmPortUpdateListener() {
        }

        @Override
        public void onAudioPortListUpdate(AudioPort[] audioPortArr) {
            synchronized (AudioManager.this.mDeviceCallbacks) {
                AudioManager.this.broadcastDeviceListChange_sync(null);
            }
        }

        @Override
        public void onAudioPatchListUpdate(AudioPatch[] audioPatchArr) {
        }

        @Override
        public void onServiceDied() {
            synchronized (AudioManager.this.mDeviceCallbacks) {
                AudioManager.this.broadcastDeviceListChange_sync(null);
            }
        }
    }

    @SystemApi
    public static abstract class AudioServerStateCallback {
        public void onAudioServerDown() {
        }

        public void onAudioServerUp() {
        }
    }

    class AnonymousClass4 extends IAudioServerStateDispatcher.Stub {
        AnonymousClass4() {
        }

        @Override
        public void dispatchAudioServerStateChange(boolean z) {
            Executor executor;
            final AudioServerStateCallback audioServerStateCallback;
            synchronized (AudioManager.this.mAudioServerStateCbLock) {
                executor = AudioManager.this.mAudioServerStateExec;
                audioServerStateCallback = AudioManager.this.mAudioServerStateCb;
            }
            if (executor == null || audioServerStateCallback == null) {
                return;
            }
            if (z) {
                executor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        audioServerStateCallback.onAudioServerUp();
                    }
                });
            } else {
                executor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        audioServerStateCallback.onAudioServerDown();
                    }
                });
            }
        }
    }

    @SystemApi
    public void setAudioServerStateCallback(Executor executor, AudioServerStateCallback audioServerStateCallback) {
        if (audioServerStateCallback == null) {
            throw new IllegalArgumentException("Illegal null AudioServerStateCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Illegal null Executor for the AudioServerStateCallback");
        }
        synchronized (this.mAudioServerStateCbLock) {
            if (this.mAudioServerStateCb != null) {
                throw new IllegalStateException("setAudioServerStateCallback called with already registered callabck");
            }
            try {
                getService().registerAudioServerStateDispatcher(this.mAudioServerStateDispatcher);
                this.mAudioServerStateExec = executor;
                this.mAudioServerStateCb = audioServerStateCallback;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public void clearAudioServerStateCallback() {
        synchronized (this.mAudioServerStateCbLock) {
            if (this.mAudioServerStateCb != null) {
                try {
                    getService().unregisterAudioServerStateDispatcher(this.mAudioServerStateDispatcher);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            this.mAudioServerStateExec = null;
            this.mAudioServerStateCb = null;
        }
    }

    @SystemApi
    public boolean isAudioServerRunning() {
        try {
            return getService().isAudioServerRunning();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Map<Integer, Boolean> getSurroundFormats() {
        HashMap map = new HashMap();
        int surroundFormats = AudioSystem.getSurroundFormats(map, false);
        if (surroundFormats != 0) {
            Log.e(TAG, "getSurroundFormats failed:" + surroundFormats);
            return new HashMap();
        }
        return map;
    }

    public boolean setSurroundFormatEnabled(int i, boolean z) {
        return AudioSystem.setSurroundFormatEnabled(i, z) == 0;
    }

    public Map<Integer, Boolean> getReportedSurroundFormats() {
        HashMap map = new HashMap();
        int surroundFormats = AudioSystem.getSurroundFormats(map, true);
        if (surroundFormats != 0) {
            Log.e(TAG, "getReportedSurroundFormats failed:" + surroundFormats);
            return new HashMap();
        }
        return map;
    }

    private class NativeEventHandlerDelegate {
        private final Handler mHandler;

        NativeEventHandlerDelegate(final AudioDeviceCallback audioDeviceCallback, Handler handler) {
            Looper mainLooper;
            if (handler != null) {
                mainLooper = handler.getLooper();
            } else {
                mainLooper = Looper.getMainLooper();
            }
            if (mainLooper != null) {
                this.mHandler = new Handler(mainLooper) {
                    @Override
                    public void handleMessage(Message message) {
                        switch (message.what) {
                            case 0:
                            case 1:
                                if (audioDeviceCallback != null) {
                                    audioDeviceCallback.onAudioDevicesAdded((AudioDeviceInfo[]) message.obj);
                                }
                                break;
                            case 2:
                                if (audioDeviceCallback != null) {
                                    audioDeviceCallback.onAudioDevicesRemoved((AudioDeviceInfo[]) message.obj);
                                }
                                break;
                            default:
                                Log.e(AudioManager.TAG, "Unknown native event type: " + message.what);
                                break;
                        }
                    }
                };
            } else {
                this.mHandler = null;
            }
        }

        Handler getHandler() {
            return this.mHandler;
        }
    }
}
