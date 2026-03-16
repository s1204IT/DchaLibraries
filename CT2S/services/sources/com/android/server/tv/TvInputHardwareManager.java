package com.android.server.tv;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiHotplugEvent;
import android.hardware.hdmi.IHdmiControlService;
import android.hardware.hdmi.IHdmiDeviceEventListener;
import android.hardware.hdmi.IHdmiHotplugEventListener;
import android.hardware.hdmi.IHdmiSystemAudioModeChangeListener;
import android.media.AudioDevicePort;
import android.media.AudioDevicePortConfig;
import android.media.AudioFormat;
import android.media.AudioGain;
import android.media.AudioGainConfig;
import android.media.AudioManager;
import android.media.AudioPatch;
import android.media.AudioPort;
import android.media.AudioPortConfig;
import android.media.tv.ITvInputHardware;
import android.media.tv.ITvInputHardwareCallback;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Surface;
import com.android.server.tv.TvInputHal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class TvInputHardwareManager implements TvInputHal.Callback {
    private static final String TAG = TvInputHardwareManager.class.getSimpleName();
    private final AudioManager mAudioManager;
    private final Context mContext;
    private final Handler mHandler;
    private IHdmiControlService mHdmiControlService;
    private final IHdmiDeviceEventListener mHdmiDeviceEventListener;
    private final IHdmiHotplugEventListener mHdmiHotplugEventListener;
    private final IHdmiSystemAudioModeChangeListener mHdmiSystemAudioModeChangeListener;
    private final Listener mListener;
    private final boolean mUseMasterVolume;
    private final TvInputHal mHal = new TvInputHal(this);
    private final SparseArray<Connection> mConnections = new SparseArray<>();
    private final List<TvInputHardwareInfo> mHardwareList = new ArrayList();
    private final List<HdmiDeviceInfo> mHdmiDeviceList = new LinkedList();
    private final SparseArray<String> mHardwareInputIdMap = new SparseArray<>();
    private final SparseArray<String> mHdmiInputIdMap = new SparseArray<>();
    private final Map<String, TvInputInfo> mInputMap = new ArrayMap();
    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TvInputHardwareManager.this.handleVolumeChange(context, intent);
        }
    };
    private int mCurrentIndex = 0;
    private int mCurrentMaxIndex = 0;
    private final SparseBooleanArray mHdmiStateMap = new SparseBooleanArray();
    private final List<Message> mPendingHdmiDeviceEvents = new LinkedList();
    private final Object mLock = new Object();

    interface Listener {
        void onHardwareDeviceAdded(TvInputHardwareInfo tvInputHardwareInfo);

        void onHardwareDeviceRemoved(TvInputHardwareInfo tvInputHardwareInfo);

        void onHdmiDeviceAdded(HdmiDeviceInfo hdmiDeviceInfo);

        void onHdmiDeviceRemoved(HdmiDeviceInfo hdmiDeviceInfo);

        void onHdmiDeviceUpdated(String str, HdmiDeviceInfo hdmiDeviceInfo);

        void onStateChanged(String str, int i);
    }

    public TvInputHardwareManager(Context context, Listener listener) {
        this.mHdmiHotplugEventListener = new HdmiHotplugEventListener();
        this.mHdmiDeviceEventListener = new HdmiDeviceEventListener();
        this.mHdmiSystemAudioModeChangeListener = new HdmiSystemAudioModeChangeListener();
        this.mHandler = new ListenerHandler();
        this.mContext = context;
        this.mListener = listener;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mUseMasterVolume = this.mContext.getResources().getBoolean(R.^attr-private.alertDialogCenterButtons);
        this.mHal.init();
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mHdmiControlService = IHdmiControlService.Stub.asInterface(ServiceManager.getService("hdmi_control"));
            if (this.mHdmiControlService != null) {
                try {
                    this.mHdmiControlService.addHotplugEventListener(this.mHdmiHotplugEventListener);
                    this.mHdmiControlService.addDeviceEventListener(this.mHdmiDeviceEventListener);
                    this.mHdmiControlService.addSystemAudioModeChangeListener(this.mHdmiSystemAudioModeChangeListener);
                    this.mHdmiDeviceList.addAll(this.mHdmiControlService.getInputDevices());
                } catch (RemoteException e) {
                    Slog.w(TAG, "Error registering listeners to HdmiControlService:", e);
                }
            } else {
                Slog.w(TAG, "HdmiControlService is not available");
            }
            if (!this.mUseMasterVolume) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.media.VOLUME_CHANGED_ACTION");
                filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
                this.mContext.registerReceiver(this.mVolumeReceiver, filter);
            }
            updateVolume();
        }
    }

    @Override
    public void onDeviceAvailable(TvInputHardwareInfo info, TvStreamConfig[] configs) {
        synchronized (this.mLock) {
            Connection connection = new Connection(info);
            connection.updateConfigsLocked(configs);
            this.mConnections.put(info.getDeviceId(), connection);
            buildHardwareListLocked();
            this.mHandler.obtainMessage(2, 0, 0, info).sendToTarget();
            if (info.getType() == 9) {
                processPendingHdmiDeviceEventsLocked();
            }
        }
    }

    private void buildHardwareListLocked() {
        this.mHardwareList.clear();
        for (int i = 0; i < this.mConnections.size(); i++) {
            this.mHardwareList.add(this.mConnections.valueAt(i).getHardwareInfoLocked());
        }
    }

    @Override
    public void onDeviceUnavailable(int deviceId) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "onDeviceUnavailable: Cannot find a connection with " + deviceId);
                return;
            }
            connection.resetLocked(null, null, null, null, null);
            this.mConnections.remove(deviceId);
            buildHardwareListLocked();
            TvInputHardwareInfo info = connection.getHardwareInfoLocked();
            if (info.getType() == 9) {
                Iterator<HdmiDeviceInfo> it = this.mHdmiDeviceList.iterator();
                while (it.hasNext()) {
                    HdmiDeviceInfo deviceInfo = it.next();
                    if (deviceInfo.getPortId() == info.getHdmiPortId()) {
                        this.mHandler.obtainMessage(5, 0, 0, deviceInfo).sendToTarget();
                        it.remove();
                    }
                }
            }
            this.mHandler.obtainMessage(3, 0, 0, info).sendToTarget();
        }
    }

    @Override
    public void onStreamConfigurationChanged(int deviceId, TvStreamConfig[] configs) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "StreamConfigurationChanged: Cannot find a connection with " + deviceId);
                return;
            }
            connection.updateConfigsLocked(configs);
            String inputId = this.mHardwareInputIdMap.get(deviceId);
            if (inputId != null) {
                this.mHandler.obtainMessage(1, convertConnectedToState(configs.length > 0), 0, inputId).sendToTarget();
            }
            ITvInputHardwareCallback callback = connection.getCallbackLocked();
            if (callback != null) {
                try {
                    callback.onStreamConfigChanged(configs);
                } catch (RemoteException e) {
                    Slog.e(TAG, "error in onStreamConfigurationChanged", e);
                }
            }
        }
    }

    @Override
    public void onFirstFrameCaptured(int deviceId, int streamId) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "FirstFrameCaptured: Cannot find a connection with " + deviceId);
                return;
            }
            Runnable runnable = connection.getOnFirstFrameCapturedLocked();
            if (runnable != null) {
                runnable.run();
                connection.setOnFirstFrameCapturedLocked(null);
            }
        }
    }

    public List<TvInputHardwareInfo> getHardwareList() {
        List<TvInputHardwareInfo> listUnmodifiableList;
        synchronized (this.mLock) {
            listUnmodifiableList = Collections.unmodifiableList(this.mHardwareList);
        }
        return listUnmodifiableList;
    }

    public List<HdmiDeviceInfo> getHdmiDeviceList() {
        List<HdmiDeviceInfo> listUnmodifiableList;
        synchronized (this.mLock) {
            listUnmodifiableList = Collections.unmodifiableList(this.mHdmiDeviceList);
        }
        return listUnmodifiableList;
    }

    private boolean checkUidChangedLocked(Connection connection, int callingUid, int resolvedUserId) {
        Integer connectionCallingUid = connection.getCallingUidLocked();
        Integer connectionResolvedUserId = connection.getResolvedUserIdLocked();
        return connectionCallingUid == null || connectionResolvedUserId == null || connectionCallingUid.intValue() != callingUid || connectionResolvedUserId.intValue() != resolvedUserId;
    }

    private int convertConnectedToState(boolean connected) {
        return connected ? 0 : 2;
    }

    public void addHardwareTvInput(int deviceId, TvInputInfo info) {
        String inputId;
        synchronized (this.mLock) {
            String oldInputId = this.mHardwareInputIdMap.get(deviceId);
            if (oldInputId != null) {
                Slog.w(TAG, "Trying to override previous registration: old = " + this.mInputMap.get(oldInputId) + ":" + deviceId + ", new = " + info + ":" + deviceId);
            }
            this.mHardwareInputIdMap.put(deviceId, info.getId());
            this.mInputMap.put(info.getId(), info);
            for (int i = 0; i < this.mHdmiStateMap.size(); i++) {
                TvInputHardwareInfo hardwareInfo = findHardwareInfoForHdmiPortLocked(this.mHdmiStateMap.keyAt(i));
                if (hardwareInfo != null && (inputId = this.mHardwareInputIdMap.get(hardwareInfo.getDeviceId())) != null && inputId.equals(info.getId())) {
                    this.mHandler.obtainMessage(1, convertConnectedToState(this.mHdmiStateMap.valueAt(i)), 0, inputId).sendToTarget();
                    return;
                }
            }
            Connection connection = this.mConnections.get(deviceId);
            if (connection != null) {
                this.mHandler.obtainMessage(1, convertConnectedToState(connection.getConfigsLocked().length > 0), 0, info.getId()).sendToTarget();
            }
        }
    }

    private static <T> int indexOfEqualValue(SparseArray<T> map, T value) {
        for (int i = 0; i < map.size(); i++) {
            if (map.valueAt(i).equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean intArrayContains(int[] array, int value) {
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    public void addHdmiTvInput(int id, TvInputInfo info) {
        if (info.getType() != 1007) {
            throw new IllegalArgumentException("info (" + info + ") has non-HDMI type.");
        }
        synchronized (this.mLock) {
            String parentId = info.getParentId();
            int parentIndex = indexOfEqualValue(this.mHardwareInputIdMap, parentId);
            if (parentIndex < 0) {
                throw new IllegalArgumentException("info (" + info + ") has invalid parentId.");
            }
            String oldInputId = this.mHdmiInputIdMap.get(id);
            if (oldInputId != null) {
                Slog.w(TAG, "Trying to override previous registration: old = " + this.mInputMap.get(oldInputId) + ":" + id + ", new = " + info + ":" + id);
            }
            this.mHdmiInputIdMap.put(id, info.getId());
            this.mInputMap.put(info.getId(), info);
        }
    }

    public void removeTvInput(String inputId) {
        synchronized (this.mLock) {
            this.mInputMap.remove(inputId);
            int hardwareIndex = indexOfEqualValue(this.mHardwareInputIdMap, inputId);
            if (hardwareIndex >= 0) {
                this.mHardwareInputIdMap.removeAt(hardwareIndex);
            }
            int deviceIndex = indexOfEqualValue(this.mHdmiInputIdMap, inputId);
            if (deviceIndex >= 0) {
                this.mHdmiInputIdMap.removeAt(deviceIndex);
            }
        }
    }

    public ITvInputHardware acquireHardware(int deviceId, ITvInputHardwareCallback callback, TvInputInfo info, int callingUid, int resolvedUserId) {
        ITvInputHardware hardwareLocked = null;
        if (callback == null) {
            throw new NullPointerException();
        }
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "Invalid deviceId : " + deviceId);
            } else if (checkUidChangedLocked(connection, callingUid, resolvedUserId)) {
                TvInputHardwareImpl hardware = new TvInputHardwareImpl(connection.getHardwareInfoLocked());
                try {
                    callback.asBinder().linkToDeath(connection, 0);
                    connection.resetLocked(hardware, callback, info, Integer.valueOf(callingUid), Integer.valueOf(resolvedUserId));
                    hardwareLocked = connection.getHardwareLocked();
                } catch (RemoteException e) {
                    hardware.release();
                }
            } else {
                hardwareLocked = connection.getHardwareLocked();
            }
        }
        return hardwareLocked;
    }

    public void releaseHardware(int deviceId, ITvInputHardware hardware, int callingUid, int resolvedUserId) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "Invalid deviceId : " + deviceId);
            } else if (connection.getHardwareLocked() == hardware && !checkUidChangedLocked(connection, callingUid, resolvedUserId)) {
                connection.resetLocked(null, null, null, null, null);
            }
        }
    }

    private TvInputHardwareInfo findHardwareInfoForHdmiPortLocked(int port) {
        for (TvInputHardwareInfo hardwareInfo : this.mHardwareList) {
            if (hardwareInfo.getType() == 9 && hardwareInfo.getHdmiPortId() == port) {
                return hardwareInfo;
            }
        }
        return null;
    }

    private int findDeviceIdForInputIdLocked(String inputId) {
        for (int i = 0; i < this.mConnections.size(); i++) {
            Connection connection = this.mConnections.get(i);
            if (connection.getInfoLocked().getId().equals(inputId)) {
                return i;
            }
        }
        return -1;
    }

    public List<TvStreamConfig> getAvailableTvStreamConfigList(String inputId, int callingUid, int resolvedUserId) {
        List<TvStreamConfig> configsList = new ArrayList<>();
        synchronized (this.mLock) {
            int deviceId = findDeviceIdForInputIdLocked(inputId);
            if (deviceId < 0) {
                Slog.e(TAG, "Invalid inputId : " + inputId);
            } else {
                Connection connection = this.mConnections.get(deviceId);
                TvStreamConfig[] arr$ = connection.getConfigsLocked();
                for (TvStreamConfig config : arr$) {
                    if (config.getType() == 2) {
                        configsList.add(config);
                    }
                }
            }
        }
        return configsList;
    }

    public boolean captureFrame(String inputId, Surface surface, final TvStreamConfig config, int callingUid, int resolvedUserId) {
        boolean result = false;
        synchronized (this.mLock) {
            int deviceId = findDeviceIdForInputIdLocked(inputId);
            if (deviceId < 0) {
                Slog.e(TAG, "Invalid inputId : " + inputId);
            } else {
                Connection connection = this.mConnections.get(deviceId);
                final TvInputHardwareImpl hardwareImpl = connection.getHardwareImplLocked();
                if (hardwareImpl != null) {
                    Runnable runnable = connection.getOnFirstFrameCapturedLocked();
                    if (runnable != null) {
                        runnable.run();
                        connection.setOnFirstFrameCapturedLocked(null);
                    }
                    result = hardwareImpl.startCapture(surface, config);
                    if (result) {
                        connection.setOnFirstFrameCapturedLocked(new Runnable() {
                            @Override
                            public void run() {
                                hardwareImpl.stopCapture(config);
                            }
                        });
                    }
                }
            }
        }
        return result;
    }

    private void processPendingHdmiDeviceEventsLocked() {
        Iterator<Message> it = this.mPendingHdmiDeviceEvents.iterator();
        while (it.hasNext()) {
            Message msg = it.next();
            HdmiDeviceInfo deviceInfo = (HdmiDeviceInfo) msg.obj;
            TvInputHardwareInfo hardwareInfo = findHardwareInfoForHdmiPortLocked(deviceInfo.getPortId());
            if (hardwareInfo != null) {
                msg.sendToTarget();
                it.remove();
            }
        }
    }

    private void updateVolume() {
        this.mCurrentMaxIndex = this.mAudioManager.getStreamMaxVolume(3);
        this.mCurrentIndex = this.mAudioManager.getStreamVolume(3);
    }

    private void handleVolumeChange(Context context, Intent intent) {
        int index;
        String action = intent.getAction();
        if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
            int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
            if (streamType == 3 && (index = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)) != this.mCurrentIndex) {
                this.mCurrentIndex = index;
            } else {
                return;
            }
        } else if (action.equals("android.media.STREAM_MUTE_CHANGED_ACTION")) {
            int streamType2 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
            if (streamType2 != 3) {
                return;
            }
        } else {
            Slog.w(TAG, "Unrecognized intent: " + intent);
            return;
        }
        synchronized (this.mLock) {
            for (int i = 0; i < this.mConnections.size(); i++) {
                TvInputHardwareImpl hardwareImpl = this.mConnections.valueAt(i).getHardwareImplLocked();
                if (hardwareImpl != null) {
                    hardwareImpl.onMediaStreamVolumeChanged();
                }
            }
        }
    }

    private float getMediaStreamVolume() {
        if (this.mUseMasterVolume) {
            return 1.0f;
        }
        return this.mCurrentIndex / this.mCurrentMaxIndex;
    }

    private class Connection implements IBinder.DeathRecipient {
        private ITvInputHardwareCallback mCallback;
        private final TvInputHardwareInfo mHardwareInfo;
        private TvInputInfo mInfo;
        private Runnable mOnFirstFrameCaptured;
        private TvInputHardwareImpl mHardware = null;
        private TvStreamConfig[] mConfigs = null;
        private Integer mCallingUid = null;
        private Integer mResolvedUserId = null;

        public Connection(TvInputHardwareInfo hardwareInfo) {
            this.mHardwareInfo = hardwareInfo;
        }

        public void resetLocked(TvInputHardwareImpl hardware, ITvInputHardwareCallback callback, TvInputInfo info, Integer callingUid, Integer resolvedUserId) {
            if (this.mHardware != null) {
                try {
                    this.mCallback.onReleased();
                } catch (RemoteException e) {
                    Slog.e(TvInputHardwareManager.TAG, "error in Connection::resetLocked", e);
                }
                this.mHardware.release();
            }
            this.mHardware = hardware;
            this.mCallback = callback;
            this.mInfo = info;
            this.mCallingUid = callingUid;
            this.mResolvedUserId = resolvedUserId;
            this.mOnFirstFrameCaptured = null;
            if (this.mHardware != null && this.mCallback != null) {
                try {
                    this.mCallback.onStreamConfigChanged(getConfigsLocked());
                } catch (RemoteException e2) {
                    Slog.e(TvInputHardwareManager.TAG, "error in Connection::resetLocked", e2);
                }
            }
        }

        public void updateConfigsLocked(TvStreamConfig[] configs) {
            this.mConfigs = configs;
        }

        public TvInputHardwareInfo getHardwareInfoLocked() {
            return this.mHardwareInfo;
        }

        public TvInputInfo getInfoLocked() {
            return this.mInfo;
        }

        public ITvInputHardware getHardwareLocked() {
            return this.mHardware;
        }

        public TvInputHardwareImpl getHardwareImplLocked() {
            return this.mHardware;
        }

        public ITvInputHardwareCallback getCallbackLocked() {
            return this.mCallback;
        }

        public TvStreamConfig[] getConfigsLocked() {
            return this.mConfigs;
        }

        public Integer getCallingUidLocked() {
            return this.mCallingUid;
        }

        public Integer getResolvedUserIdLocked() {
            return this.mResolvedUserId;
        }

        public void setOnFirstFrameCapturedLocked(Runnable runnable) {
            this.mOnFirstFrameCaptured = runnable;
        }

        public Runnable getOnFirstFrameCapturedLocked() {
            return this.mOnFirstFrameCaptured;
        }

        @Override
        public void binderDied() {
            synchronized (TvInputHardwareManager.this.mLock) {
                resetLocked(null, null, null, null, null);
            }
        }
    }

    private class TvInputHardwareImpl extends ITvInputHardware.Stub {
        private AudioDevicePort mAudioSource;
        private final TvInputHardwareInfo mInfo;
        private boolean mReleased = false;
        private final Object mImplLock = new Object();
        private final AudioManager.OnAudioPortUpdateListener mAudioListener = new AudioManager.OnAudioPortUpdateListener() {
            public void onAudioPortListUpdate(AudioPort[] portList) {
                synchronized (TvInputHardwareImpl.this.mImplLock) {
                    TvInputHardwareImpl.this.updateAudioConfigLocked();
                }
            }

            public void onAudioPatchListUpdate(AudioPatch[] patchList) {
            }

            public void onServiceDied() {
                synchronized (TvInputHardwareImpl.this.mImplLock) {
                    TvInputHardwareImpl.this.mAudioSource = null;
                    TvInputHardwareImpl.this.mAudioSink.clear();
                    TvInputHardwareImpl.this.mAudioPatch = null;
                }
            }
        };
        private int mOverrideAudioType = 0;
        private String mOverrideAudioAddress = "";
        private List<AudioDevicePort> mAudioSink = new ArrayList();
        private AudioPatch mAudioPatch = null;
        private float mCommittedVolume = -1.0f;
        private float mSourceVolume = 0.0f;
        private TvStreamConfig mActiveConfig = null;
        private int mDesiredSamplingRate = 0;
        private int mDesiredChannelMask = 1;
        private int mDesiredFormat = 1;

        public TvInputHardwareImpl(TvInputHardwareInfo info) {
            this.mInfo = info;
            TvInputHardwareManager.this.mAudioManager.registerAudioPortUpdateListener(this.mAudioListener);
            if (this.mInfo.getAudioType() != 0) {
                this.mAudioSource = findAudioDevicePort(this.mInfo.getAudioType(), this.mInfo.getAudioAddress());
                findAudioSinkFromAudioPolicy(this.mAudioSink);
            }
        }

        private void findAudioSinkFromAudioPolicy(List<AudioDevicePort> sinks) {
            sinks.clear();
            ArrayList<AudioPort> devicePorts = new ArrayList<>();
            if (TvInputHardwareManager.this.mAudioManager.listAudioDevicePorts(devicePorts) == 0) {
                int sinkDevice = TvInputHardwareManager.this.mAudioManager.getDevicesForStream(3);
                for (AudioPort port : devicePorts) {
                    AudioDevicePort devicePort = (AudioDevicePort) port;
                    if ((devicePort.type() & sinkDevice) != 0) {
                        sinks.add(devicePort);
                    }
                }
            }
        }

        private AudioDevicePort findAudioDevicePort(int type, String address) {
            if (type == 0) {
                return null;
            }
            ArrayList<AudioPort> devicePorts = new ArrayList<>();
            if (TvInputHardwareManager.this.mAudioManager.listAudioDevicePorts(devicePorts) != 0) {
                return null;
            }
            Iterator<AudioPort> it = devicePorts.iterator();
            while (it.hasNext()) {
                AudioDevicePort devicePort = (AudioDevicePort) it.next();
                if (devicePort.type() == type && devicePort.address().equals(address)) {
                    return devicePort;
                }
            }
            return null;
        }

        public void release() {
            synchronized (this.mImplLock) {
                TvInputHardwareManager.this.mAudioManager.unregisterAudioPortUpdateListener(this.mAudioListener);
                if (this.mAudioPatch != null) {
                    TvInputHardwareManager.this.mAudioManager.releaseAudioPatch(this.mAudioPatch);
                    this.mAudioPatch = null;
                }
                this.mReleased = true;
            }
        }

        public boolean setSurface(Surface surface, TvStreamConfig config) throws RemoteException {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
                int result = 0;
                if (surface == null) {
                    if (this.mActiveConfig == null) {
                        return true;
                    }
                    result = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), this.mActiveConfig);
                    this.mActiveConfig = null;
                } else {
                    if (config == null) {
                        return false;
                    }
                    if (this.mActiveConfig != null && !config.equals(this.mActiveConfig) && (result = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), this.mActiveConfig)) != 0) {
                        this.mActiveConfig = null;
                    }
                    if (result == 0 && (result = TvInputHardwareManager.this.mHal.addOrUpdateStream(this.mInfo.getDeviceId(), surface, config)) == 0) {
                        this.mActiveConfig = config;
                    }
                }
                updateAudioConfigLocked();
                return result == 0;
            }
        }

        private void updateAudioConfigLocked() {
            int gainValue;
            boolean sinkUpdated = updateAudioSinkLocked();
            boolean sourceUpdated = updateAudioSourceLocked();
            if (this.mAudioSource != null && !this.mAudioSink.isEmpty() && this.mActiveConfig != null) {
                TvInputHardwareManager.this.updateVolume();
                float volume = this.mSourceVolume * TvInputHardwareManager.this.getMediaStreamVolume();
                AudioGainConfig sourceGainConfig = null;
                if (this.mAudioSource.gains().length > 0 && volume != this.mCommittedVolume) {
                    AudioGain sourceGain = null;
                    AudioGain[] arr$ = this.mAudioSource.gains();
                    int len$ = arr$.length;
                    int i$ = 0;
                    while (true) {
                        if (i$ >= len$) {
                            break;
                        }
                        AudioGain gain = arr$[i$];
                        if ((gain.mode() & 1) == 0) {
                            i$++;
                        } else {
                            sourceGain = gain;
                            break;
                        }
                    }
                    if (sourceGain == null) {
                        Slog.w(TvInputHardwareManager.TAG, "No audio source gain with MODE_JOINT support exists.");
                    } else {
                        int steps = (sourceGain.maxValue() - sourceGain.minValue()) / sourceGain.stepValue();
                        int gainValue2 = sourceGain.minValue();
                        if (volume < 1.0f) {
                            gainValue = gainValue2 + (sourceGain.stepValue() * ((int) (((double) (steps * volume)) + 0.5d)));
                        } else {
                            gainValue = sourceGain.maxValue();
                        }
                        int[] gainValues = {gainValue};
                        sourceGainConfig = sourceGain.buildConfig(1, sourceGain.channelMask(), gainValues, 0);
                    }
                }
                AudioPortConfig sourceConfig = this.mAudioSource.activeConfig();
                List<AudioPortConfig> sinkConfigs = new ArrayList<>();
                AudioPatch[] audioPatchArray = {this.mAudioPatch};
                boolean shouldRecreateAudioPatch = sourceUpdated || sinkUpdated;
                for (AudioDevicePort audioSink : this.mAudioSink) {
                    AudioDevicePortConfig audioDevicePortConfigActiveConfig = audioSink.activeConfig();
                    int sinkSamplingRate = this.mDesiredSamplingRate;
                    int sinkChannelMask = this.mDesiredChannelMask;
                    int sinkFormat = this.mDesiredFormat;
                    if (audioDevicePortConfigActiveConfig != null) {
                        if (sinkSamplingRate == 0) {
                            sinkSamplingRate = audioDevicePortConfigActiveConfig.samplingRate();
                        }
                        if (sinkChannelMask == 1) {
                            sinkChannelMask = audioDevicePortConfigActiveConfig.channelMask();
                        }
                        if (sinkFormat == 1) {
                            sinkChannelMask = audioDevicePortConfigActiveConfig.format();
                        }
                    }
                    if (audioDevicePortConfigActiveConfig == null || audioDevicePortConfigActiveConfig.samplingRate() != sinkSamplingRate || audioDevicePortConfigActiveConfig.channelMask() != sinkChannelMask || audioDevicePortConfigActiveConfig.format() != sinkFormat) {
                        if (!TvInputHardwareManager.intArrayContains(audioSink.samplingRates(), sinkSamplingRate) && audioSink.samplingRates().length > 0) {
                            sinkSamplingRate = audioSink.samplingRates()[0];
                        }
                        if (!TvInputHardwareManager.intArrayContains(audioSink.channelMasks(), sinkChannelMask)) {
                            sinkChannelMask = 1;
                        }
                        if (!TvInputHardwareManager.intArrayContains(audioSink.formats(), sinkFormat)) {
                            sinkFormat = 1;
                        }
                        audioDevicePortConfigActiveConfig = audioSink.buildConfig(sinkSamplingRate, sinkChannelMask, sinkFormat, (AudioGainConfig) null);
                        shouldRecreateAudioPatch = true;
                    }
                    sinkConfigs.add(audioDevicePortConfigActiveConfig);
                }
                AudioPortConfig sinkConfig = sinkConfigs.get(0);
                if (sourceConfig == null || sourceGainConfig != null) {
                    int sourceSamplingRate = 0;
                    if (TvInputHardwareManager.intArrayContains(this.mAudioSource.samplingRates(), sinkConfig.samplingRate())) {
                        sourceSamplingRate = sinkConfig.samplingRate();
                    } else if (this.mAudioSource.samplingRates().length > 0) {
                        sourceSamplingRate = this.mAudioSource.samplingRates()[0];
                    }
                    int sourceChannelMask = 1;
                    int[] arr$2 = this.mAudioSource.channelMasks();
                    int len$2 = arr$2.length;
                    int i$2 = 0;
                    while (true) {
                        if (i$2 >= len$2) {
                            break;
                        }
                        int inChannelMask = arr$2[i$2];
                        if (AudioFormat.channelCountFromOutChannelMask(sinkConfig.channelMask()) != AudioFormat.channelCountFromInChannelMask(inChannelMask)) {
                            i$2++;
                        } else {
                            sourceChannelMask = inChannelMask;
                            break;
                        }
                    }
                    int sourceFormat = 1;
                    if (TvInputHardwareManager.intArrayContains(this.mAudioSource.formats(), sinkConfig.format())) {
                        sourceFormat = sinkConfig.format();
                    }
                    sourceConfig = this.mAudioSource.buildConfig(sourceSamplingRate, sourceChannelMask, sourceFormat, sourceGainConfig);
                    shouldRecreateAudioPatch = true;
                }
                if (shouldRecreateAudioPatch) {
                    this.mCommittedVolume = volume;
                    TvInputHardwareManager.this.mAudioManager.createAudioPatch(audioPatchArray, new AudioPortConfig[]{sourceConfig}, (AudioPortConfig[]) sinkConfigs.toArray(new AudioPortConfig[0]));
                    this.mAudioPatch = audioPatchArray[0];
                    if (sourceGainConfig != null) {
                        TvInputHardwareManager.this.mAudioManager.setAudioPortGain(this.mAudioSource, sourceGainConfig);
                        return;
                    }
                    return;
                }
                return;
            }
            if (this.mAudioPatch != null) {
                TvInputHardwareManager.this.mAudioManager.releaseAudioPatch(this.mAudioPatch);
                this.mAudioPatch = null;
            }
        }

        public void setStreamVolume(float volume) throws RemoteException {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
                this.mSourceVolume = volume;
                updateAudioConfigLocked();
            }
        }

        public boolean dispatchKeyEventToHdmi(KeyEvent event) throws RemoteException {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
            }
            if (this.mInfo.getType() != 9) {
            }
            return false;
        }

        private boolean startCapture(Surface surface, TvStreamConfig config) {
            synchronized (this.mImplLock) {
                if (!this.mReleased) {
                    if (surface != null && config != null) {
                        if (config.getType() == 2) {
                            int result = TvInputHardwareManager.this.mHal.addOrUpdateStream(this.mInfo.getDeviceId(), surface, config);
                            z = result == 0;
                        }
                    }
                }
            }
            return z;
        }

        private boolean stopCapture(TvStreamConfig config) {
            synchronized (this.mImplLock) {
                if (!this.mReleased) {
                    if (config != null) {
                        int result = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), config);
                        z = result == 0;
                    }
                }
            }
            return z;
        }

        private boolean updateAudioSourceLocked() {
            boolean z = true;
            if (this.mInfo.getAudioType() == 0) {
                return false;
            }
            AudioDevicePort previousSource = this.mAudioSource;
            this.mAudioSource = findAudioDevicePort(this.mInfo.getAudioType(), this.mInfo.getAudioAddress());
            if (this.mAudioSource == null) {
                if (previousSource == null) {
                    z = false;
                }
            } else if (this.mAudioSource.equals(previousSource)) {
                z = false;
            }
            return z;
        }

        private boolean updateAudioSinkLocked() {
            if (this.mInfo.getAudioType() == 0) {
                return false;
            }
            List<AudioDevicePort> previousSink = this.mAudioSink;
            this.mAudioSink = new ArrayList();
            if (this.mOverrideAudioType == 0) {
                findAudioSinkFromAudioPolicy(this.mAudioSink);
            } else {
                AudioDevicePort audioSink = findAudioDevicePort(this.mOverrideAudioType, this.mOverrideAudioAddress);
                if (audioSink != null) {
                    this.mAudioSink.add(audioSink);
                }
            }
            if (this.mAudioSink.size() != previousSink.size()) {
                return true;
            }
            previousSink.removeAll(this.mAudioSink);
            return !previousSink.isEmpty();
        }

        private void handleAudioSinkUpdated() {
            synchronized (this.mImplLock) {
                updateAudioConfigLocked();
            }
        }

        public void overrideAudioSink(int audioType, String audioAddress, int samplingRate, int channelMask, int format) {
            synchronized (this.mImplLock) {
                this.mOverrideAudioType = audioType;
                this.mOverrideAudioAddress = audioAddress;
                this.mDesiredSamplingRate = samplingRate;
                this.mDesiredChannelMask = channelMask;
                this.mDesiredFormat = format;
                updateAudioConfigLocked();
            }
        }

        public void onMediaStreamVolumeChanged() {
            synchronized (this.mImplLock) {
                updateAudioConfigLocked();
            }
        }
    }

    private class ListenerHandler extends Handler {
        private static final int HARDWARE_DEVICE_ADDED = 2;
        private static final int HARDWARE_DEVICE_REMOVED = 3;
        private static final int HDMI_DEVICE_ADDED = 4;
        private static final int HDMI_DEVICE_REMOVED = 5;
        private static final int HDMI_DEVICE_UPDATED = 6;
        private static final int STATE_CHANGED = 1;

        private ListenerHandler() {
        }

        @Override
        public final void handleMessage(Message msg) {
            String inputId;
            switch (msg.what) {
                case 1:
                    String inputId2 = (String) msg.obj;
                    int state = msg.arg1;
                    TvInputHardwareManager.this.mListener.onStateChanged(inputId2, state);
                    return;
                case 2:
                    TvInputHardwareManager.this.mListener.onHardwareDeviceAdded((TvInputHardwareInfo) msg.obj);
                    return;
                case 3:
                    TvInputHardwareManager.this.mListener.onHardwareDeviceRemoved((TvInputHardwareInfo) msg.obj);
                    return;
                case 4:
                    TvInputHardwareManager.this.mListener.onHdmiDeviceAdded((HdmiDeviceInfo) msg.obj);
                    return;
                case 5:
                    TvInputHardwareManager.this.mListener.onHdmiDeviceRemoved((HdmiDeviceInfo) msg.obj);
                    return;
                case 6:
                    HdmiDeviceInfo info = (HdmiDeviceInfo) msg.obj;
                    synchronized (TvInputHardwareManager.this.mLock) {
                        inputId = (String) TvInputHardwareManager.this.mHdmiInputIdMap.get(info.getId());
                        break;
                    }
                    if (inputId != null) {
                        TvInputHardwareManager.this.mListener.onHdmiDeviceUpdated(inputId, info);
                        return;
                    } else {
                        Slog.w(TvInputHardwareManager.TAG, "Could not resolve input ID matching the device info; ignoring.");
                        return;
                    }
                default:
                    Slog.w(TvInputHardwareManager.TAG, "Unhandled message: " + msg);
                    return;
            }
        }
    }

    private final class HdmiHotplugEventListener extends IHdmiHotplugEventListener.Stub {
        private HdmiHotplugEventListener() {
        }

        public void onReceived(HdmiHotplugEvent event) {
            synchronized (TvInputHardwareManager.this.mLock) {
                TvInputHardwareManager.this.mHdmiStateMap.put(event.getPort(), event.isConnected());
                TvInputHardwareInfo hardwareInfo = TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(event.getPort());
                if (hardwareInfo != null) {
                    String inputId = (String) TvInputHardwareManager.this.mHardwareInputIdMap.get(hardwareInfo.getDeviceId());
                    if (inputId != null) {
                        TvInputHardwareManager.this.mHandler.obtainMessage(1, TvInputHardwareManager.this.convertConnectedToState(event.isConnected()), 0, inputId).sendToTarget();
                    }
                }
            }
        }
    }

    private final class HdmiDeviceEventListener extends IHdmiDeviceEventListener.Stub {
        private HdmiDeviceEventListener() {
        }

        public void onStatusChanged(HdmiDeviceInfo deviceInfo, int status) {
            if (deviceInfo.isSourceType()) {
                synchronized (TvInputHardwareManager.this.mLock) {
                    int messageType = 0;
                    Object obj = null;
                    switch (status) {
                        case 1:
                            if (findHdmiDeviceInfo(deviceInfo.getId()) == null) {
                                TvInputHardwareManager.this.mHdmiDeviceList.add(deviceInfo);
                                messageType = 4;
                                obj = deviceInfo;
                            } else {
                                Slog.w(TvInputHardwareManager.TAG, "The list already contains " + deviceInfo + "; ignoring.");
                                return;
                            }
                            break;
                        case 2:
                            HdmiDeviceInfo originalDeviceInfo = findHdmiDeviceInfo(deviceInfo.getId());
                            if (!TvInputHardwareManager.this.mHdmiDeviceList.remove(originalDeviceInfo)) {
                                Slog.w(TvInputHardwareManager.TAG, "The list doesn't contain " + deviceInfo + "; ignoring.");
                                return;
                            } else {
                                messageType = 5;
                                obj = deviceInfo;
                            }
                            break;
                        case 3:
                            HdmiDeviceInfo originalDeviceInfo2 = findHdmiDeviceInfo(deviceInfo.getId());
                            if (!TvInputHardwareManager.this.mHdmiDeviceList.remove(originalDeviceInfo2)) {
                                Slog.w(TvInputHardwareManager.TAG, "The list doesn't contain " + deviceInfo + "; ignoring.");
                                return;
                            }
                            TvInputHardwareManager.this.mHdmiDeviceList.add(deviceInfo);
                            messageType = 6;
                            obj = deviceInfo;
                            break;
                            break;
                    }
                    Message msg = TvInputHardwareManager.this.mHandler.obtainMessage(messageType, 0, 0, obj);
                    if (TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(deviceInfo.getPortId()) == null) {
                        TvInputHardwareManager.this.mPendingHdmiDeviceEvents.add(msg);
                    } else {
                        msg.sendToTarget();
                    }
                }
            }
        }

        private HdmiDeviceInfo findHdmiDeviceInfo(int id) {
            for (HdmiDeviceInfo info : TvInputHardwareManager.this.mHdmiDeviceList) {
                if (info.getId() == id) {
                    return info;
                }
            }
            return null;
        }
    }

    private final class HdmiSystemAudioModeChangeListener extends IHdmiSystemAudioModeChangeListener.Stub {
        private HdmiSystemAudioModeChangeListener() {
        }

        public void onStatusChanged(boolean enabled) throws RemoteException {
            synchronized (TvInputHardwareManager.this.mLock) {
                for (int i = 0; i < TvInputHardwareManager.this.mConnections.size(); i++) {
                    TvInputHardwareImpl impl = ((Connection) TvInputHardwareManager.this.mConnections.valueAt(i)).getHardwareImplLocked();
                    if (impl != null) {
                        impl.handleAudioSinkUpdated();
                    }
                }
            }
        }
    }
}
