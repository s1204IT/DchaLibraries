package com.android.server.tv;

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
import android.view.Surface;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.tv.TvInputHal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final IHdmiDeviceEventListener mHdmiDeviceEventListener;
    private final IHdmiHotplugEventListener mHdmiHotplugEventListener;
    private final IHdmiSystemAudioModeChangeListener mHdmiSystemAudioModeChangeListener;
    private final Listener mListener;
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
        this.mHal.init();
    }

    public void onBootPhase(int i) {
        if (i == 500) {
            IHdmiControlService iHdmiControlServiceAsInterface = IHdmiControlService.Stub.asInterface(ServiceManager.getService("hdmi_control"));
            if (iHdmiControlServiceAsInterface != null) {
                try {
                    iHdmiControlServiceAsInterface.addHotplugEventListener(this.mHdmiHotplugEventListener);
                    iHdmiControlServiceAsInterface.addDeviceEventListener(this.mHdmiDeviceEventListener);
                    iHdmiControlServiceAsInterface.addSystemAudioModeChangeListener(this.mHdmiSystemAudioModeChangeListener);
                    this.mHdmiDeviceList.addAll(iHdmiControlServiceAsInterface.getInputDevices());
                } catch (RemoteException e) {
                    Slog.w(TAG, "Error registering listeners to HdmiControlService:", e);
                }
            } else {
                Slog.w(TAG, "HdmiControlService is not available");
            }
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
            intentFilter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
            this.mContext.registerReceiver(this.mVolumeReceiver, intentFilter);
            updateVolume();
        }
    }

    @Override
    public void onDeviceAvailable(TvInputHardwareInfo tvInputHardwareInfo, TvStreamConfig[] tvStreamConfigArr) {
        synchronized (this.mLock) {
            Connection connection = new Connection(tvInputHardwareInfo);
            connection.updateConfigsLocked(tvStreamConfigArr);
            this.mConnections.put(tvInputHardwareInfo.getDeviceId(), connection);
            buildHardwareListLocked();
            this.mHandler.obtainMessage(2, 0, 0, tvInputHardwareInfo).sendToTarget();
            if (tvInputHardwareInfo.getType() == 9) {
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
    public void onDeviceUnavailable(int i) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(i);
            if (connection == null) {
                Slog.e(TAG, "onDeviceUnavailable: Cannot find a connection with " + i);
                return;
            }
            connection.resetLocked(null, null, null, null, null);
            this.mConnections.remove(i);
            buildHardwareListLocked();
            TvInputHardwareInfo hardwareInfoLocked = connection.getHardwareInfoLocked();
            if (hardwareInfoLocked.getType() == 9) {
                Iterator<HdmiDeviceInfo> it = this.mHdmiDeviceList.iterator();
                while (it.hasNext()) {
                    HdmiDeviceInfo next = it.next();
                    if (next.getPortId() == hardwareInfoLocked.getHdmiPortId()) {
                        this.mHandler.obtainMessage(5, 0, 0, next).sendToTarget();
                        it.remove();
                    }
                }
            }
            this.mHandler.obtainMessage(3, 0, 0, hardwareInfoLocked).sendToTarget();
        }
    }

    @Override
    public void onStreamConfigurationChanged(int i, TvStreamConfig[] tvStreamConfigArr) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(i);
            if (connection == null) {
                Slog.e(TAG, "StreamConfigurationChanged: Cannot find a connection with " + i);
                return;
            }
            int configsLengthLocked = connection.getConfigsLengthLocked();
            connection.updateConfigsLocked(tvStreamConfigArr);
            String str = this.mHardwareInputIdMap.get(i);
            if (str != null) {
                if ((configsLengthLocked == 0) != (connection.getConfigsLengthLocked() == 0)) {
                    this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, str).sendToTarget();
                }
            }
            ITvInputHardwareCallback callbackLocked = connection.getCallbackLocked();
            if (callbackLocked != null) {
                try {
                    callbackLocked.onStreamConfigChanged(tvStreamConfigArr);
                } catch (RemoteException e) {
                    Slog.e(TAG, "error in onStreamConfigurationChanged", e);
                }
            }
        }
    }

    @Override
    public void onFirstFrameCaptured(int i, int i2) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(i);
            if (connection == null) {
                Slog.e(TAG, "FirstFrameCaptured: Cannot find a connection with " + i);
                return;
            }
            Runnable onFirstFrameCapturedLocked = connection.getOnFirstFrameCapturedLocked();
            if (onFirstFrameCapturedLocked != null) {
                onFirstFrameCapturedLocked.run();
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

    private boolean checkUidChangedLocked(Connection connection, int i, int i2) {
        Integer callingUidLocked = connection.getCallingUidLocked();
        Integer resolvedUserIdLocked = connection.getResolvedUserIdLocked();
        return callingUidLocked == null || resolvedUserIdLocked == null || callingUidLocked.intValue() != i || resolvedUserIdLocked.intValue() != i2;
    }

    public void addHardwareInput(int i, TvInputInfo tvInputInfo) {
        String str;
        synchronized (this.mLock) {
            String str2 = this.mHardwareInputIdMap.get(i);
            if (str2 != null) {
                Slog.w(TAG, "Trying to override previous registration: old = " + this.mInputMap.get(str2) + ":" + i + ", new = " + tvInputInfo + ":" + i);
            }
            this.mHardwareInputIdMap.put(i, tvInputInfo.getId());
            this.mInputMap.put(tvInputInfo.getId(), tvInputInfo);
            for (int i2 = 0; i2 < this.mHdmiStateMap.size(); i2++) {
                TvInputHardwareInfo tvInputHardwareInfoFindHardwareInfoForHdmiPortLocked = findHardwareInfoForHdmiPortLocked(this.mHdmiStateMap.keyAt(i2));
                if (tvInputHardwareInfoFindHardwareInfoForHdmiPortLocked != null && (str = this.mHardwareInputIdMap.get(tvInputHardwareInfoFindHardwareInfoForHdmiPortLocked.getDeviceId())) != null && str.equals(tvInputInfo.getId())) {
                    this.mHandler.obtainMessage(1, this.mHdmiStateMap.valueAt(i2) ? 0 : 1, 0, str).sendToTarget();
                    return;
                }
            }
            Connection connection = this.mConnections.get(i);
            if (connection != null) {
                this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, tvInputInfo.getId()).sendToTarget();
            }
        }
    }

    private static <T> int indexOfEqualValue(SparseArray<T> sparseArray, T t) {
        for (int i = 0; i < sparseArray.size(); i++) {
            if (sparseArray.valueAt(i).equals(t)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean intArrayContains(int[] iArr, int i) {
        for (int i2 : iArr) {
            if (i2 == i) {
                return true;
            }
        }
        return false;
    }

    public void addHdmiInput(int i, TvInputInfo tvInputInfo) {
        if (tvInputInfo.getType() != 1007) {
            throw new IllegalArgumentException("info (" + tvInputInfo + ") has non-HDMI type.");
        }
        synchronized (this.mLock) {
            if (indexOfEqualValue(this.mHardwareInputIdMap, tvInputInfo.getParentId()) < 0) {
                throw new IllegalArgumentException("info (" + tvInputInfo + ") has invalid parentId.");
            }
            String str = this.mHdmiInputIdMap.get(i);
            if (str != null) {
                Slog.w(TAG, "Trying to override previous registration: old = " + this.mInputMap.get(str) + ":" + i + ", new = " + tvInputInfo + ":" + i);
            }
            this.mHdmiInputIdMap.put(i, tvInputInfo.getId());
            this.mInputMap.put(tvInputInfo.getId(), tvInputInfo);
        }
    }

    public void removeHardwareInput(String str) {
        synchronized (this.mLock) {
            this.mInputMap.remove(str);
            int iIndexOfEqualValue = indexOfEqualValue(this.mHardwareInputIdMap, str);
            if (iIndexOfEqualValue >= 0) {
                this.mHardwareInputIdMap.removeAt(iIndexOfEqualValue);
            }
            int iIndexOfEqualValue2 = indexOfEqualValue(this.mHdmiInputIdMap, str);
            if (iIndexOfEqualValue2 >= 0) {
                this.mHdmiInputIdMap.removeAt(iIndexOfEqualValue2);
            }
        }
    }

    public ITvInputHardware acquireHardware(int i, ITvInputHardwareCallback iTvInputHardwareCallback, TvInputInfo tvInputInfo, int i2, int i3) {
        if (iTvInputHardwareCallback == null) {
            throw new NullPointerException();
        }
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(i);
            if (connection == null) {
                Slog.e(TAG, "Invalid deviceId : " + i);
                return null;
            }
            if (checkUidChangedLocked(connection, i2, i3)) {
                TvInputHardwareImpl tvInputHardwareImpl = new TvInputHardwareImpl(connection.getHardwareInfoLocked());
                try {
                    iTvInputHardwareCallback.asBinder().linkToDeath(connection, 0);
                    connection.resetLocked(tvInputHardwareImpl, iTvInputHardwareCallback, tvInputInfo, Integer.valueOf(i2), Integer.valueOf(i3));
                } catch (RemoteException e) {
                    tvInputHardwareImpl.release();
                    return null;
                }
            }
            return connection.getHardwareLocked();
        }
    }

    public void releaseHardware(int i, ITvInputHardware iTvInputHardware, int i2, int i3) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(i);
            if (connection == null) {
                Slog.e(TAG, "Invalid deviceId : " + i);
                return;
            }
            if (connection.getHardwareLocked() == iTvInputHardware && !checkUidChangedLocked(connection, i2, i3)) {
                connection.resetLocked(null, null, null, null, null);
            }
        }
    }

    private TvInputHardwareInfo findHardwareInfoForHdmiPortLocked(int i) {
        for (TvInputHardwareInfo tvInputHardwareInfo : this.mHardwareList) {
            if (tvInputHardwareInfo.getType() == 9 && tvInputHardwareInfo.getHdmiPortId() == i) {
                return tvInputHardwareInfo;
            }
        }
        return null;
    }

    private int findDeviceIdForInputIdLocked(String str) {
        for (int i = 0; i < this.mConnections.size(); i++) {
            if (this.mConnections.get(i).getInfoLocked().getId().equals(str)) {
                return i;
            }
        }
        return -1;
    }

    public List<TvStreamConfig> getAvailableTvStreamConfigList(String str, int i, int i2) {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mLock) {
            int iFindDeviceIdForInputIdLocked = findDeviceIdForInputIdLocked(str);
            if (iFindDeviceIdForInputIdLocked < 0) {
                Slog.e(TAG, "Invalid inputId : " + str);
                return arrayList;
            }
            for (TvStreamConfig tvStreamConfig : this.mConnections.get(iFindDeviceIdForInputIdLocked).getConfigsLocked()) {
                if (tvStreamConfig.getType() == 2) {
                    arrayList.add(tvStreamConfig);
                }
            }
            return arrayList;
        }
    }

    public boolean captureFrame(String str, Surface surface, final TvStreamConfig tvStreamConfig, int i, int i2) {
        synchronized (this.mLock) {
            int iFindDeviceIdForInputIdLocked = findDeviceIdForInputIdLocked(str);
            if (iFindDeviceIdForInputIdLocked < 0) {
                Slog.e(TAG, "Invalid inputId : " + str);
                return false;
            }
            Connection connection = this.mConnections.get(iFindDeviceIdForInputIdLocked);
            final TvInputHardwareImpl hardwareImplLocked = connection.getHardwareImplLocked();
            if (hardwareImplLocked == null) {
                return false;
            }
            Runnable onFirstFrameCapturedLocked = connection.getOnFirstFrameCapturedLocked();
            if (onFirstFrameCapturedLocked != null) {
                onFirstFrameCapturedLocked.run();
                connection.setOnFirstFrameCapturedLocked(null);
            }
            boolean zStartCapture = hardwareImplLocked.startCapture(surface, tvStreamConfig);
            if (zStartCapture) {
                connection.setOnFirstFrameCapturedLocked(new Runnable() {
                    @Override
                    public void run() {
                        hardwareImplLocked.stopCapture(tvStreamConfig);
                    }
                });
            }
            return zStartCapture;
        }
    }

    private void processPendingHdmiDeviceEventsLocked() {
        Iterator<Message> it = this.mPendingHdmiDeviceEvents.iterator();
        while (it.hasNext()) {
            Message next = it.next();
            if (findHardwareInfoForHdmiPortLocked(((HdmiDeviceInfo) next.obj).getPortId()) != null) {
                next.sendToTarget();
                it.remove();
            }
        }
    }

    private void updateVolume() {
        this.mCurrentMaxIndex = this.mAudioManager.getStreamMaxVolume(3);
        this.mCurrentIndex = this.mAudioManager.getStreamVolume(3);
    }

    private void handleVolumeChange(Context context, Intent intent) {
        byte b;
        int intExtra;
        String action = intent.getAction();
        int iHashCode = action.hashCode();
        if (iHashCode != -1940635523) {
            b = (iHashCode == 1920758225 && action.equals("android.media.STREAM_MUTE_CHANGED_ACTION")) ? (byte) 1 : (byte) -1;
        } else if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
            b = 0;
        }
        switch (b) {
            case 0:
                if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) != 3 || (intExtra = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)) == this.mCurrentIndex) {
                    return;
                }
                this.mCurrentIndex = intExtra;
                break;
                break;
            case 1:
                if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) != 3) {
                    return;
                }
                break;
            default:
                Slog.w(TAG, "Unrecognized intent: " + intent);
                return;
        }
        synchronized (this.mLock) {
            for (int i = 0; i < this.mConnections.size(); i++) {
                TvInputHardwareImpl hardwareImplLocked = this.mConnections.valueAt(i).getHardwareImplLocked();
                if (hardwareImplLocked != null) {
                    hardwareImplLocked.onMediaStreamVolumeChanged();
                }
            }
        }
    }

    private float getMediaStreamVolume() {
        return this.mCurrentIndex / this.mCurrentMaxIndex;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, indentingPrintWriter)) {
            synchronized (this.mLock) {
                indentingPrintWriter.println("TvInputHardwareManager Info:");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("mConnections: deviceId -> Connection");
                indentingPrintWriter.increaseIndent();
                for (int i = 0; i < this.mConnections.size(); i++) {
                    indentingPrintWriter.println(this.mConnections.keyAt(i) + ": " + this.mConnections.valueAt(i));
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("mHardwareList:");
                indentingPrintWriter.increaseIndent();
                Iterator<TvInputHardwareInfo> it = this.mHardwareList.iterator();
                while (it.hasNext()) {
                    indentingPrintWriter.println(it.next());
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("mHdmiDeviceList:");
                indentingPrintWriter.increaseIndent();
                Iterator<HdmiDeviceInfo> it2 = this.mHdmiDeviceList.iterator();
                while (it2.hasNext()) {
                    indentingPrintWriter.println(it2.next());
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("mHardwareInputIdMap: deviceId -> inputId");
                indentingPrintWriter.increaseIndent();
                for (int i2 = 0; i2 < this.mHardwareInputIdMap.size(); i2++) {
                    indentingPrintWriter.println(this.mHardwareInputIdMap.keyAt(i2) + ": " + this.mHardwareInputIdMap.valueAt(i2));
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("mHdmiInputIdMap: id -> inputId");
                indentingPrintWriter.increaseIndent();
                for (int i3 = 0; i3 < this.mHdmiInputIdMap.size(); i3++) {
                    indentingPrintWriter.println(this.mHdmiInputIdMap.keyAt(i3) + ": " + this.mHdmiInputIdMap.valueAt(i3));
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("mInputMap: inputId -> inputInfo");
                indentingPrintWriter.increaseIndent();
                for (Map.Entry<String, TvInputInfo> entry : this.mInputMap.entrySet()) {
                    indentingPrintWriter.println(entry.getKey() + ": " + entry.getValue());
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.decreaseIndent();
            }
        }
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

        public Connection(TvInputHardwareInfo tvInputHardwareInfo) {
            this.mHardwareInfo = tvInputHardwareInfo;
        }

        public void resetLocked(TvInputHardwareImpl tvInputHardwareImpl, ITvInputHardwareCallback iTvInputHardwareCallback, TvInputInfo tvInputInfo, Integer num, Integer num2) {
            if (this.mHardware != null) {
                try {
                    this.mCallback.onReleased();
                } catch (RemoteException e) {
                    Slog.e(TvInputHardwareManager.TAG, "error in Connection::resetLocked", e);
                }
                this.mHardware.release();
            }
            this.mHardware = tvInputHardwareImpl;
            this.mCallback = iTvInputHardwareCallback;
            this.mInfo = tvInputInfo;
            this.mCallingUid = num;
            this.mResolvedUserId = num2;
            this.mOnFirstFrameCaptured = null;
            if (this.mHardware != null && this.mCallback != null) {
                try {
                    this.mCallback.onStreamConfigChanged(getConfigsLocked());
                } catch (RemoteException e2) {
                    Slog.e(TvInputHardwareManager.TAG, "error in Connection::resetLocked", e2);
                }
            }
        }

        public void updateConfigsLocked(TvStreamConfig[] tvStreamConfigArr) {
            this.mConfigs = tvStreamConfigArr;
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

        public String toString() {
            return "Connection{ mHardwareInfo: " + this.mHardwareInfo + ", mInfo: " + this.mInfo + ", mCallback: " + this.mCallback + ", mConfigs: " + Arrays.toString(this.mConfigs) + ", mCallingUid: " + this.mCallingUid + ", mResolvedUserId: " + this.mResolvedUserId + " }";
        }

        private int getConfigsLengthLocked() {
            if (this.mConfigs == null) {
                return 0;
            }
            return this.mConfigs.length;
        }

        private int getInputStateLocked() {
            if (getConfigsLengthLocked() > 0) {
                return 0;
            }
            switch (this.mHardwareInfo.getCableConnectionStatus()) {
            }
            return 0;
        }
    }

    private class TvInputHardwareImpl extends ITvInputHardware.Stub {
        private AudioDevicePort mAudioSource;
        private final TvInputHardwareInfo mInfo;
        private boolean mReleased = false;
        private final Object mImplLock = new Object();
        private final AudioManager.OnAudioPortUpdateListener mAudioListener = new AudioManager.OnAudioPortUpdateListener() {
            public void onAudioPortListUpdate(AudioPort[] audioPortArr) {
                synchronized (TvInputHardwareImpl.this.mImplLock) {
                    TvInputHardwareImpl.this.updateAudioConfigLocked();
                }
            }

            public void onAudioPatchListUpdate(AudioPatch[] audioPatchArr) {
            }

            public void onServiceDied() {
                synchronized (TvInputHardwareImpl.this.mImplLock) {
                    TvInputHardwareImpl.this.mAudioSource = null;
                    TvInputHardwareImpl.this.mAudioSink.clear();
                    if (TvInputHardwareImpl.this.mAudioPatch != null) {
                        AudioManager unused = TvInputHardwareManager.this.mAudioManager;
                        AudioManager.releaseAudioPatch(TvInputHardwareImpl.this.mAudioPatch);
                        TvInputHardwareImpl.this.mAudioPatch = null;
                    }
                }
            }
        };
        private int mOverrideAudioType = 0;
        private String mOverrideAudioAddress = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        private List<AudioDevicePort> mAudioSink = new ArrayList();
        private AudioPatch mAudioPatch = null;
        private float mCommittedVolume = -1.0f;
        private float mSourceVolume = 0.0f;
        private TvStreamConfig mActiveConfig = null;
        private int mDesiredSamplingRate = 0;
        private int mDesiredChannelMask = 1;
        private int mDesiredFormat = 1;

        public TvInputHardwareImpl(TvInputHardwareInfo tvInputHardwareInfo) {
            this.mInfo = tvInputHardwareInfo;
            TvInputHardwareManager.this.mAudioManager.registerAudioPortUpdateListener(this.mAudioListener);
            if (this.mInfo.getAudioType() != 0) {
                this.mAudioSource = findAudioDevicePort(this.mInfo.getAudioType(), this.mInfo.getAudioAddress());
                findAudioSinkFromAudioPolicy(this.mAudioSink);
            }
        }

        private void findAudioSinkFromAudioPolicy(List<AudioDevicePort> list) {
            list.clear();
            ArrayList<AudioDevicePort> arrayList = new ArrayList();
            AudioManager unused = TvInputHardwareManager.this.mAudioManager;
            if (AudioManager.listAudioDevicePorts(arrayList) == 0) {
                int devicesForStream = TvInputHardwareManager.this.mAudioManager.getDevicesForStream(3);
                for (AudioDevicePort audioDevicePort : arrayList) {
                    if ((audioDevicePort.type() & devicesForStream) != 0 && (audioDevicePort.type() & Integer.MIN_VALUE) == 0) {
                        list.add(audioDevicePort);
                    }
                }
            }
        }

        private AudioDevicePort findAudioDevicePort(int i, String str) {
            if (i == 0) {
                return null;
            }
            ArrayList<AudioDevicePort> arrayList = new ArrayList();
            AudioManager unused = TvInputHardwareManager.this.mAudioManager;
            if (AudioManager.listAudioDevicePorts(arrayList) != 0) {
                return null;
            }
            for (AudioDevicePort audioDevicePort : arrayList) {
                if (audioDevicePort.type() == i && audioDevicePort.address().equals(str)) {
                    return audioDevicePort;
                }
            }
            return null;
        }

        public void release() {
            synchronized (this.mImplLock) {
                TvInputHardwareManager.this.mAudioManager.unregisterAudioPortUpdateListener(this.mAudioListener);
                if (this.mAudioPatch != null) {
                    AudioManager unused = TvInputHardwareManager.this.mAudioManager;
                    AudioManager.releaseAudioPatch(this.mAudioPatch);
                    this.mAudioPatch = null;
                }
                this.mReleased = true;
            }
        }

        public boolean setSurface(Surface surface, TvStreamConfig tvStreamConfig) throws RemoteException {
            int iRemoveStream;
            int iAddOrUpdateStream;
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
                boolean z = true;
                if (surface == null) {
                    if (this.mActiveConfig != null) {
                        iAddOrUpdateStream = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), this.mActiveConfig);
                        this.mActiveConfig = null;
                    } else {
                        return true;
                    }
                } else {
                    if (tvStreamConfig == null) {
                        return false;
                    }
                    if (this.mActiveConfig != null && !tvStreamConfig.equals(this.mActiveConfig)) {
                        iRemoveStream = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), this.mActiveConfig);
                        if (iRemoveStream != 0) {
                            this.mActiveConfig = null;
                        }
                    } else {
                        iRemoveStream = 0;
                    }
                    if (iRemoveStream == 0) {
                        iAddOrUpdateStream = TvInputHardwareManager.this.mHal.addOrUpdateStream(this.mInfo.getDeviceId(), surface, tvStreamConfig);
                        if (iAddOrUpdateStream == 0) {
                            this.mActiveConfig = tvStreamConfig;
                        }
                    } else {
                        iAddOrUpdateStream = iRemoveStream;
                    }
                }
                updateAudioConfigLocked();
                if (iAddOrUpdateStream != 0) {
                    z = false;
                }
                return z;
            }
        }

        private void updateAudioConfigLocked() {
            AudioGainConfig audioGainConfigBuildConfig;
            int iSamplingRate;
            int i;
            int i2;
            AudioGain audioGain;
            int iMaxValue;
            boolean zUpdateAudioSinkLocked = updateAudioSinkLocked();
            boolean zUpdateAudioSourceLocked = updateAudioSourceLocked();
            if (this.mAudioSource != null && !this.mAudioSink.isEmpty() && this.mActiveConfig != null) {
                TvInputHardwareManager.this.updateVolume();
                float mediaStreamVolume = this.mSourceVolume * TvInputHardwareManager.this.getMediaStreamVolume();
                int i3 = 1;
                if (this.mAudioSource.gains().length > 0 && mediaStreamVolume != this.mCommittedVolume) {
                    AudioGain[] audioGainArrGains = this.mAudioSource.gains();
                    int length = audioGainArrGains.length;
                    int i4 = 0;
                    while (true) {
                        if (i4 < length) {
                            audioGain = audioGainArrGains[i4];
                            if ((audioGain.mode() & 1) != 0) {
                                break;
                            } else {
                                i4++;
                            }
                        } else {
                            audioGain = null;
                            break;
                        }
                    }
                    if (audioGain == null) {
                        Slog.w(TvInputHardwareManager.TAG, "No audio source gain with MODE_JOINT support exists.");
                        audioGainConfigBuildConfig = null;
                    } else {
                        int iMaxValue2 = (audioGain.maxValue() - audioGain.minValue()) / audioGain.stepValue();
                        int iMinValue = audioGain.minValue();
                        if (mediaStreamVolume < 1.0f) {
                            iMaxValue = iMinValue + (audioGain.stepValue() * ((int) (((double) (iMaxValue2 * mediaStreamVolume)) + 0.5d)));
                        } else {
                            iMaxValue = audioGain.maxValue();
                        }
                        audioGainConfigBuildConfig = audioGain.buildConfig(1, audioGain.channelMask(), new int[]{iMaxValue}, 0);
                    }
                } else {
                    audioGainConfigBuildConfig = null;
                }
                AudioPortConfig audioPortConfigActiveConfig = this.mAudioSource.activeConfig();
                ArrayList arrayList = new ArrayList();
                AudioPatch[] audioPatchArr = {this.mAudioPatch};
                boolean z = zUpdateAudioSourceLocked || zUpdateAudioSinkLocked;
                for (AudioDevicePort audioDevicePort : this.mAudioSink) {
                    AudioDevicePortConfig audioDevicePortConfigActiveConfig = audioDevicePort.activeConfig();
                    int iSamplingRate2 = this.mDesiredSamplingRate;
                    int iChannelMask = this.mDesiredChannelMask;
                    int i5 = this.mDesiredFormat;
                    if (audioDevicePortConfigActiveConfig != null) {
                        if (iSamplingRate2 == 0) {
                            iSamplingRate2 = audioDevicePortConfigActiveConfig.samplingRate();
                        }
                        if (iChannelMask == i3) {
                            iChannelMask = audioDevicePortConfigActiveConfig.channelMask();
                        }
                        if (i5 == i3) {
                            iChannelMask = audioDevicePortConfigActiveConfig.format();
                        }
                    }
                    if (audioDevicePortConfigActiveConfig == null || audioDevicePortConfigActiveConfig.samplingRate() != iSamplingRate2 || audioDevicePortConfigActiveConfig.channelMask() != iChannelMask || audioDevicePortConfigActiveConfig.format() != i5) {
                        if (!TvInputHardwareManager.intArrayContains(audioDevicePort.samplingRates(), iSamplingRate2) && audioDevicePort.samplingRates().length > 0) {
                            iSamplingRate2 = audioDevicePort.samplingRates()[0];
                        }
                        if (!TvInputHardwareManager.intArrayContains(audioDevicePort.channelMasks(), iChannelMask)) {
                            iChannelMask = 1;
                        }
                        if (!TvInputHardwareManager.intArrayContains(audioDevicePort.formats(), i5)) {
                            i5 = 1;
                        }
                        audioDevicePortConfigActiveConfig = audioDevicePort.buildConfig(iSamplingRate2, iChannelMask, i5, (AudioGainConfig) null);
                        z = true;
                    }
                    arrayList.add(audioDevicePortConfigActiveConfig);
                    i3 = 1;
                }
                AudioPortConfig audioPortConfig = (AudioPortConfig) arrayList.get(0);
                if (audioPortConfigActiveConfig == null || audioGainConfigBuildConfig != null) {
                    if (TvInputHardwareManager.intArrayContains(this.mAudioSource.samplingRates(), audioPortConfig.samplingRate())) {
                        iSamplingRate = audioPortConfig.samplingRate();
                    } else if (this.mAudioSource.samplingRates().length > 0) {
                        iSamplingRate = this.mAudioSource.samplingRates()[0];
                    } else {
                        iSamplingRate = 0;
                    }
                    int[] iArrChannelMasks = this.mAudioSource.channelMasks();
                    int length2 = iArrChannelMasks.length;
                    int i6 = 0;
                    while (true) {
                        if (i6 < length2) {
                            i = iArrChannelMasks[i6];
                            if (AudioFormat.channelCountFromOutChannelMask(audioPortConfig.channelMask()) == AudioFormat.channelCountFromInChannelMask(i)) {
                                break;
                            } else {
                                i6++;
                            }
                        } else {
                            i = 1;
                            break;
                        }
                    }
                    if (TvInputHardwareManager.intArrayContains(this.mAudioSource.formats(), audioPortConfig.format())) {
                        i2 = audioPortConfig.format();
                    } else {
                        i2 = 1;
                    }
                    audioPortConfigActiveConfig = this.mAudioSource.buildConfig(iSamplingRate, i, i2, audioGainConfigBuildConfig);
                    z = true;
                }
                if (z) {
                    this.mCommittedVolume = mediaStreamVolume;
                    if (this.mAudioPatch != null) {
                        AudioManager unused = TvInputHardwareManager.this.mAudioManager;
                        AudioManager.releaseAudioPatch(this.mAudioPatch);
                    }
                    AudioManager unused2 = TvInputHardwareManager.this.mAudioManager;
                    AudioManager.createAudioPatch(audioPatchArr, new AudioPortConfig[]{audioPortConfigActiveConfig}, (AudioPortConfig[]) arrayList.toArray(new AudioPortConfig[arrayList.size()]));
                    this.mAudioPatch = audioPatchArr[0];
                    if (audioGainConfigBuildConfig != null) {
                        AudioManager unused3 = TvInputHardwareManager.this.mAudioManager;
                        AudioManager.setAudioPortGain(this.mAudioSource, audioGainConfigBuildConfig);
                        return;
                    }
                    return;
                }
                return;
            }
            if (this.mAudioPatch != null) {
                AudioManager unused4 = TvInputHardwareManager.this.mAudioManager;
                AudioManager.releaseAudioPatch(this.mAudioPatch);
                this.mAudioPatch = null;
            }
        }

        public void setStreamVolume(float f) throws RemoteException {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
                this.mSourceVolume = f;
                updateAudioConfigLocked();
            }
        }

        private boolean startCapture(Surface surface, TvStreamConfig tvStreamConfig) {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    return false;
                }
                if (surface != null && tvStreamConfig != null) {
                    if (tvStreamConfig.getType() != 2) {
                        return false;
                    }
                    return TvInputHardwareManager.this.mHal.addOrUpdateStream(this.mInfo.getDeviceId(), surface, tvStreamConfig) == 0;
                }
                return false;
            }
        }

        private boolean stopCapture(TvStreamConfig tvStreamConfig) {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    return false;
                }
                if (tvStreamConfig == null) {
                    return false;
                }
                return TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), tvStreamConfig) == 0;
            }
        }

        private boolean updateAudioSourceLocked() {
            if (this.mInfo.getAudioType() == 0) {
                return false;
            }
            AudioDevicePort audioDevicePort = this.mAudioSource;
            this.mAudioSource = findAudioDevicePort(this.mInfo.getAudioType(), this.mInfo.getAudioAddress());
            if (this.mAudioSource == null) {
                if (audioDevicePort == null) {
                    return false;
                }
            } else if (this.mAudioSource.equals(audioDevicePort)) {
                return false;
            }
            return true;
        }

        private boolean updateAudioSinkLocked() {
            if (this.mInfo.getAudioType() == 0) {
                return false;
            }
            List<AudioDevicePort> list = this.mAudioSink;
            this.mAudioSink = new ArrayList();
            if (this.mOverrideAudioType == 0) {
                findAudioSinkFromAudioPolicy(this.mAudioSink);
            } else {
                AudioDevicePort audioDevicePortFindAudioDevicePort = findAudioDevicePort(this.mOverrideAudioType, this.mOverrideAudioAddress);
                if (audioDevicePortFindAudioDevicePort != null) {
                    this.mAudioSink.add(audioDevicePortFindAudioDevicePort);
                }
            }
            if (this.mAudioSink.size() != list.size()) {
                return true;
            }
            list.removeAll(this.mAudioSink);
            return !list.isEmpty();
        }

        private void handleAudioSinkUpdated() {
            synchronized (this.mImplLock) {
                updateAudioConfigLocked();
            }
        }

        public void overrideAudioSink(int i, String str, int i2, int i3, int i4) {
            synchronized (this.mImplLock) {
                this.mOverrideAudioType = i;
                this.mOverrideAudioAddress = str;
                this.mDesiredSamplingRate = i2;
                this.mDesiredChannelMask = i3;
                this.mDesiredFormat = i4;
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
        public final void handleMessage(Message message) {
            String str;
            switch (message.what) {
                case 1:
                    TvInputHardwareManager.this.mListener.onStateChanged((String) message.obj, message.arg1);
                    return;
                case 2:
                    TvInputHardwareManager.this.mListener.onHardwareDeviceAdded((TvInputHardwareInfo) message.obj);
                    return;
                case 3:
                    TvInputHardwareManager.this.mListener.onHardwareDeviceRemoved((TvInputHardwareInfo) message.obj);
                    return;
                case 4:
                    TvInputHardwareManager.this.mListener.onHdmiDeviceAdded((HdmiDeviceInfo) message.obj);
                    return;
                case 5:
                    TvInputHardwareManager.this.mListener.onHdmiDeviceRemoved((HdmiDeviceInfo) message.obj);
                    return;
                case 6:
                    HdmiDeviceInfo hdmiDeviceInfo = (HdmiDeviceInfo) message.obj;
                    synchronized (TvInputHardwareManager.this.mLock) {
                        str = (String) TvInputHardwareManager.this.mHdmiInputIdMap.get(hdmiDeviceInfo.getId());
                        break;
                    }
                    if (str != null) {
                        TvInputHardwareManager.this.mListener.onHdmiDeviceUpdated(str, hdmiDeviceInfo);
                        return;
                    } else {
                        Slog.w(TvInputHardwareManager.TAG, "Could not resolve input ID matching the device info; ignoring.");
                        return;
                    }
                default:
                    Slog.w(TvInputHardwareManager.TAG, "Unhandled message: " + message);
                    return;
            }
        }
    }

    private final class HdmiHotplugEventListener extends IHdmiHotplugEventListener.Stub {
        private HdmiHotplugEventListener() {
        }

        public void onReceived(HdmiHotplugEvent hdmiHotplugEvent) {
            synchronized (TvInputHardwareManager.this.mLock) {
                TvInputHardwareManager.this.mHdmiStateMap.put(hdmiHotplugEvent.getPort(), hdmiHotplugEvent.isConnected());
                TvInputHardwareInfo tvInputHardwareInfoFindHardwareInfoForHdmiPortLocked = TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(hdmiHotplugEvent.getPort());
                if (tvInputHardwareInfoFindHardwareInfoForHdmiPortLocked == null) {
                    return;
                }
                String str = (String) TvInputHardwareManager.this.mHardwareInputIdMap.get(tvInputHardwareInfoFindHardwareInfoForHdmiPortLocked.getDeviceId());
                if (str == null) {
                    return;
                }
                TvInputHardwareManager.this.mHandler.obtainMessage(1, hdmiHotplugEvent.isConnected() ? 0 : 1, 0, str).sendToTarget();
            }
        }
    }

    private final class HdmiDeviceEventListener extends IHdmiDeviceEventListener.Stub {
        private HdmiDeviceEventListener() {
        }

        public void onStatusChanged(HdmiDeviceInfo hdmiDeviceInfo, int i) {
            int i2;
            if (hdmiDeviceInfo.isSourceType()) {
                synchronized (TvInputHardwareManager.this.mLock) {
                    HdmiDeviceInfo hdmiDeviceInfo2 = null;
                    switch (i) {
                        case 1:
                            if (findHdmiDeviceInfo(hdmiDeviceInfo.getId()) == null) {
                                TvInputHardwareManager.this.mHdmiDeviceList.add(hdmiDeviceInfo);
                                i2 = 4;
                                hdmiDeviceInfo2 = hdmiDeviceInfo;
                                Message messageObtainMessage = TvInputHardwareManager.this.mHandler.obtainMessage(i2, 0, 0, hdmiDeviceInfo2);
                                if (TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(hdmiDeviceInfo.getPortId()) != null) {
                                    TvInputHardwareManager.this.mPendingHdmiDeviceEvents.add(messageObtainMessage);
                                } else {
                                    messageObtainMessage.sendToTarget();
                                }
                                return;
                            }
                            Slog.w(TvInputHardwareManager.TAG, "The list already contains " + hdmiDeviceInfo + "; ignoring.");
                            return;
                        case 2:
                            if (!TvInputHardwareManager.this.mHdmiDeviceList.remove(findHdmiDeviceInfo(hdmiDeviceInfo.getId()))) {
                                Slog.w(TvInputHardwareManager.TAG, "The list doesn't contain " + hdmiDeviceInfo + "; ignoring.");
                                return;
                            }
                            i2 = 5;
                            hdmiDeviceInfo2 = hdmiDeviceInfo;
                            Message messageObtainMessage2 = TvInputHardwareManager.this.mHandler.obtainMessage(i2, 0, 0, hdmiDeviceInfo2);
                            if (TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(hdmiDeviceInfo.getPortId()) != null) {
                            }
                            return;
                        case 3:
                            if (!TvInputHardwareManager.this.mHdmiDeviceList.remove(findHdmiDeviceInfo(hdmiDeviceInfo.getId()))) {
                                Slog.w(TvInputHardwareManager.TAG, "The list doesn't contain " + hdmiDeviceInfo + "; ignoring.");
                                return;
                            }
                            TvInputHardwareManager.this.mHdmiDeviceList.add(hdmiDeviceInfo);
                            i2 = 6;
                            hdmiDeviceInfo2 = hdmiDeviceInfo;
                            Message messageObtainMessage22 = TvInputHardwareManager.this.mHandler.obtainMessage(i2, 0, 0, hdmiDeviceInfo2);
                            if (TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(hdmiDeviceInfo.getPortId()) != null) {
                            }
                            return;
                        default:
                            i2 = 0;
                            Message messageObtainMessage222 = TvInputHardwareManager.this.mHandler.obtainMessage(i2, 0, 0, hdmiDeviceInfo2);
                            if (TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(hdmiDeviceInfo.getPortId()) != null) {
                            }
                            return;
                    }
                }
            }
        }

        private HdmiDeviceInfo findHdmiDeviceInfo(int i) {
            for (HdmiDeviceInfo hdmiDeviceInfo : TvInputHardwareManager.this.mHdmiDeviceList) {
                if (hdmiDeviceInfo.getId() == i) {
                    return hdmiDeviceInfo;
                }
            }
            return null;
        }
    }

    private final class HdmiSystemAudioModeChangeListener extends IHdmiSystemAudioModeChangeListener.Stub {
        private HdmiSystemAudioModeChangeListener() {
        }

        public void onStatusChanged(boolean z) throws RemoteException {
            synchronized (TvInputHardwareManager.this.mLock) {
                for (int i = 0; i < TvInputHardwareManager.this.mConnections.size(); i++) {
                    TvInputHardwareImpl hardwareImplLocked = ((Connection) TvInputHardwareManager.this.mConnections.valueAt(i)).getHardwareImplLocked();
                    if (hardwareImplLocked != null) {
                        hardwareImplLocked.handleAudioSinkUpdated();
                    }
                }
            }
        }
    }
}
