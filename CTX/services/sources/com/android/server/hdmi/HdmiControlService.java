package com.android.server.hdmi;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiHotplugEvent;
import android.hardware.hdmi.HdmiPortInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.hardware.hdmi.IHdmiControlService;
import android.hardware.hdmi.IHdmiDeviceEventListener;
import android.hardware.hdmi.IHdmiHotplugEventListener;
import android.hardware.hdmi.IHdmiInputChangeListener;
import android.hardware.hdmi.IHdmiMhlVendorCommandListener;
import android.hardware.hdmi.IHdmiRecordListener;
import android.hardware.hdmi.IHdmiSystemAudioModeChangeListener;
import android.hardware.hdmi.IHdmiVendorCommandListener;
import android.media.AudioManager;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecController;
import com.android.server.hdmi.HdmiCecLocalDevice;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import libcore.util.EmptyArray;

public final class HdmiControlService extends SystemService {
    static final int INITIATED_BY_BOOT_UP = 1;
    static final int INITIATED_BY_ENABLE_CEC = 0;
    static final int INITIATED_BY_HOTPLUG = 4;
    static final int INITIATED_BY_SCREEN_ON = 2;
    static final int INITIATED_BY_WAKE_UP_MESSAGE = 3;
    static final String PERMISSION = "android.permission.HDMI_CEC";
    static final int STANDBY_SCREEN_OFF = 0;
    static final int STANDBY_SHUTDOWN = 1;
    private static final String TAG = "HdmiControlService";
    private final Locale HONG_KONG;
    private final Locale MACAU;

    @HdmiAnnotations.ServiceThreadOnly
    private int mActivePortId;
    private boolean mAddressAllocated;
    private HdmiCecController mCecController;
    private final CecMessageBuffer mCecMessageBuffer;

    @GuardedBy("mLock")
    private final ArrayList<DeviceEventListenerRecord> mDeviceEventListenerRecords;
    private final Handler mHandler;
    private final HdmiControlBroadcastReceiver mHdmiControlBroadcastReceiver;

    @GuardedBy("mLock")
    private boolean mHdmiControlEnabled;

    @GuardedBy("mLock")
    private final ArrayList<HotplugEventListenerRecord> mHotplugEventListenerRecords;

    @GuardedBy("mLock")
    private InputChangeListenerRecord mInputChangeListenerRecord;
    private final HandlerThread mIoThread;

    @HdmiAnnotations.ServiceThreadOnly
    private String mLanguage;

    @HdmiAnnotations.ServiceThreadOnly
    private int mLastInputMhl;
    private final List<Integer> mLocalDevices;
    private final Object mLock;
    private HdmiCecMessageValidator mMessageValidator;
    private HdmiMhlControllerStub mMhlController;

    @GuardedBy("mLock")
    private List<HdmiDeviceInfo> mMhlDevices;

    @GuardedBy("mLock")
    private boolean mMhlInputChangeEnabled;

    @GuardedBy("mLock")
    private final ArrayList<HdmiMhlVendorCommandListenerRecord> mMhlVendorCommandListenerRecords;
    private UnmodifiableSparseArray<HdmiDeviceInfo> mPortDeviceMap;
    private UnmodifiableSparseIntArray mPortIdMap;
    private List<HdmiPortInfo> mPortInfo;
    private UnmodifiableSparseArray<HdmiPortInfo> mPortInfoMap;
    private PowerManager mPowerManager;

    @HdmiAnnotations.ServiceThreadOnly
    private int mPowerStatus;

    @GuardedBy("mLock")
    private boolean mProhibitMode;

    @GuardedBy("mLock")
    private HdmiRecordListenerRecord mRecordListenerRecord;
    private final SelectRequestBuffer mSelectRequestBuffer;
    private final SettingsObserver mSettingsObserver;

    @HdmiAnnotations.ServiceThreadOnly
    private boolean mStandbyMessageReceived;
    private final ArrayList<SystemAudioModeChangeListenerRecord> mSystemAudioModeChangeListenerRecords;
    private TvInputManager mTvInputManager;

    @GuardedBy("mLock")
    private final ArrayList<VendorCommandListenerRecord> mVendorCommandListenerRecords;

    @HdmiAnnotations.ServiceThreadOnly
    private boolean mWakeUpMessageReceived;

    interface DevicePollingCallback {
        void onPollingFinished(List<Integer> list);
    }

    interface SendMessageCallback {
        void onSendCompleted(int i);
    }

    private class HdmiControlBroadcastReceiver extends BroadcastReceiver {
        private HdmiControlBroadcastReceiver() {
        }

        @Override
        @HdmiAnnotations.ServiceThreadOnly
        public void onReceive(Context context, Intent intent) {
            byte b;
            HdmiControlService.this.assertRunOnServiceThread();
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -2128145023) {
                if (iHashCode != -1454123155) {
                    if (iHashCode != 158859398) {
                        b = (iHashCode == 1947666138 && action.equals("android.intent.action.ACTION_SHUTDOWN")) ? (byte) 3 : (byte) -1;
                    } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                        b = 2;
                    }
                } else if (action.equals("android.intent.action.SCREEN_ON")) {
                    b = 1;
                }
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    if (HdmiControlService.this.isPowerOnOrTransient()) {
                        HdmiControlService.this.onStandby(0);
                    }
                    break;
                case 1:
                    if (HdmiControlService.this.isPowerStandbyOrTransient()) {
                        HdmiControlService.this.onWakeUp();
                    }
                    break;
                case 2:
                    String menuLanguage = getMenuLanguage();
                    if (!HdmiControlService.this.mLanguage.equals(menuLanguage)) {
                        HdmiControlService.this.onLanguageChanged(menuLanguage);
                    }
                    break;
                case 3:
                    if (HdmiControlService.this.isPowerOnOrTransient()) {
                        HdmiControlService.this.onStandby(1);
                    }
                    break;
            }
        }

        private String getMenuLanguage() {
            Locale locale = Locale.getDefault();
            if (locale.equals(Locale.TAIWAN) || locale.equals(HdmiControlService.this.HONG_KONG) || locale.equals(HdmiControlService.this.MACAU)) {
                return "chi";
            }
            return locale.getISO3Language();
        }
    }

    private final class CecMessageBuffer {
        private List<HdmiCecMessage> mBuffer;

        private CecMessageBuffer() {
            this.mBuffer = new ArrayList();
        }

        public void bufferMessage(HdmiCecMessage hdmiCecMessage) {
            int opcode = hdmiCecMessage.getOpcode();
            if (opcode == 4 || opcode == 13) {
                bufferImageOrTextViewOn(hdmiCecMessage);
            } else if (opcode == 130) {
                bufferActiveSource(hdmiCecMessage);
            }
        }

        public void processMessages() {
            for (final HdmiCecMessage hdmiCecMessage : this.mBuffer) {
                HdmiControlService.this.runOnServiceThread(new Runnable() {
                    @Override
                    public void run() {
                        HdmiControlService.this.handleCecCommand(hdmiCecMessage);
                    }
                });
            }
            this.mBuffer.clear();
        }

        private void bufferActiveSource(HdmiCecMessage hdmiCecMessage) {
            if (!replaceMessageIfBuffered(hdmiCecMessage, 130)) {
                this.mBuffer.add(hdmiCecMessage);
            }
        }

        private void bufferImageOrTextViewOn(HdmiCecMessage hdmiCecMessage) {
            if (!replaceMessageIfBuffered(hdmiCecMessage, 4) && !replaceMessageIfBuffered(hdmiCecMessage, 13)) {
                this.mBuffer.add(hdmiCecMessage);
            }
        }

        private boolean replaceMessageIfBuffered(HdmiCecMessage hdmiCecMessage, int i) {
            for (int i2 = 0; i2 < this.mBuffer.size(); i2++) {
                if (this.mBuffer.get(i2).getOpcode() == i) {
                    this.mBuffer.set(i2, hdmiCecMessage);
                    return true;
                }
            }
            return false;
        }
    }

    public HdmiControlService(Context context) {
        super(context);
        this.HONG_KONG = new Locale("zh", "HK");
        this.MACAU = new Locale("zh", "MO");
        this.mIoThread = new HandlerThread("Hdmi Control Io Thread");
        this.mLock = new Object();
        this.mHotplugEventListenerRecords = new ArrayList<>();
        this.mDeviceEventListenerRecords = new ArrayList<>();
        this.mVendorCommandListenerRecords = new ArrayList<>();
        this.mSystemAudioModeChangeListenerRecords = new ArrayList<>();
        this.mHandler = new Handler();
        this.mHdmiControlBroadcastReceiver = new HdmiControlBroadcastReceiver();
        this.mPowerStatus = 1;
        this.mLanguage = Locale.getDefault().getISO3Language();
        this.mStandbyMessageReceived = false;
        this.mWakeUpMessageReceived = false;
        this.mActivePortId = -1;
        this.mMhlVendorCommandListenerRecords = new ArrayList<>();
        this.mLastInputMhl = -1;
        this.mAddressAllocated = false;
        this.mCecMessageBuffer = new CecMessageBuffer();
        this.mSelectRequestBuffer = new SelectRequestBuffer();
        this.mLocalDevices = getIntList(SystemProperties.get("ro.hdmi.device_type"));
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
    }

    private static List<Integer> getIntList(String str) {
        ArrayList arrayList = new ArrayList();
        TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(',');
        simpleStringSplitter.setString(str);
        for (String str2 : simpleStringSplitter) {
            try {
                arrayList.add(Integer.valueOf(Integer.parseInt(str2)));
            } catch (NumberFormatException e) {
                Slog.w(TAG, "Can't parseInt: " + str2);
            }
        }
        return Collections.unmodifiableList(arrayList);
    }

    @Override
    public void onStart() {
        this.mIoThread.start();
        this.mPowerStatus = 2;
        this.mProhibitMode = false;
        this.mHdmiControlEnabled = readBooleanSetting("hdmi_control_enabled", true);
        this.mMhlInputChangeEnabled = readBooleanSetting("mhl_input_switching_enabled", true);
        this.mCecController = HdmiCecController.create(this);
        if (this.mCecController != null) {
            if (this.mHdmiControlEnabled) {
                initializeCec(1);
            }
            this.mMhlController = HdmiMhlControllerStub.create(this);
            if (!this.mMhlController.isReady()) {
                Slog.i(TAG, "Device does not support MHL-control.");
            }
            this.mMhlDevices = Collections.emptyList();
            initPortInfo();
            this.mMessageValidator = new HdmiCecMessageValidator(this);
            publishBinderService("hdmi_control", new BinderService());
            if (this.mCecController != null) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.SCREEN_OFF");
                intentFilter.addAction("android.intent.action.SCREEN_ON");
                intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
                intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
                getContext().registerReceiver(this.mHdmiControlBroadcastReceiver, intentFilter);
                registerContentObserver();
            }
            this.mMhlController.setOption(HdmiCecKeycode.CEC_KEYCODE_SELECT_MEDIA_FUNCTION, 1);
            return;
        }
        Slog.i(TAG, "Device does not support HDMI-CEC.");
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            this.mTvInputManager = (TvInputManager) getContext().getSystemService("tv_input");
            this.mPowerManager = (PowerManager) getContext().getSystemService("power");
        }
    }

    TvInputManager getTvInputManager() {
        return this.mTvInputManager;
    }

    void registerTvInputCallback(TvInputManager.TvInputCallback tvInputCallback) {
        if (this.mTvInputManager == null) {
            return;
        }
        this.mTvInputManager.registerCallback(tvInputCallback, this.mHandler);
    }

    void unregisterTvInputCallback(TvInputManager.TvInputCallback tvInputCallback) {
        if (this.mTvInputManager == null) {
            return;
        }
        this.mTvInputManager.unregisterCallback(tvInputCallback);
    }

    PowerManager getPowerManager() {
        return this.mPowerManager;
    }

    private void onInitializeCecComplete(int i) {
        int i2 = 2;
        if (this.mPowerStatus == 2) {
            this.mPowerStatus = 0;
        }
        this.mWakeUpMessageReceived = false;
        if (isTvDeviceEnabled()) {
            this.mCecController.setOption(1, tv().getAutoWakeup());
        }
        switch (i) {
            case 0:
                i2 = 1;
                break;
            case 1:
                i2 = 0;
                break;
            case 2:
            case 3:
                break;
            default:
                i2 = -1;
                break;
        }
        if (i2 != -1) {
            invokeVendorCommandListenersOnControlStateChanged(true, i2);
        }
    }

    private void registerContentObserver() {
        ContentResolver contentResolver = getContext().getContentResolver();
        for (String str : new String[]{"hdmi_control_enabled", "hdmi_control_auto_wakeup_enabled", "hdmi_control_auto_device_off_enabled", "hdmi_system_audio_control_enabled", "mhl_input_switching_enabled", "mhl_power_charge_enabled"}) {
            contentResolver.registerContentObserver(Settings.Global.getUriFor(str), false, this.mSettingsObserver, -1);
        }
    }

    private class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            boolean booleanSetting;
            String lastPathSegment = uri.getLastPathSegment();
            booleanSetting = HdmiControlService.this.readBooleanSetting(lastPathSegment, true);
            switch (lastPathSegment) {
                case "hdmi_control_enabled":
                    HdmiControlService.this.setControlEnabled(booleanSetting);
                    break;
                case "hdmi_control_auto_wakeup_enabled":
                    if (HdmiControlService.this.isTvDeviceEnabled()) {
                        HdmiControlService.this.tv().setAutoWakeup(booleanSetting);
                    }
                    HdmiControlService.this.setCecOption(1, booleanSetting);
                    break;
                case "hdmi_control_auto_device_off_enabled":
                    Iterator it = HdmiControlService.this.mLocalDevices.iterator();
                    while (it.hasNext()) {
                        HdmiCecLocalDevice localDevice = HdmiControlService.this.mCecController.getLocalDevice(((Integer) it.next()).intValue());
                        if (localDevice != null) {
                            localDevice.setAutoDeviceOff(booleanSetting);
                        }
                    }
                    break;
                case "hdmi_system_audio_control_enabled":
                    if (HdmiControlService.this.isTvDeviceEnabled()) {
                        HdmiControlService.this.tv().setSystemAudioControlFeatureEnabled(booleanSetting);
                        break;
                    }
                    break;
                case "mhl_input_switching_enabled":
                    HdmiControlService.this.setMhlInputChangeEnabled(booleanSetting);
                    break;
                case "mhl_power_charge_enabled":
                    HdmiControlService.this.mMhlController.setOption(HdmiCecKeycode.CEC_KEYCODE_RESTORE_VOLUME_FUNCTION, HdmiControlService.toInt(booleanSetting));
                    break;
            }
        }
    }

    private static int toInt(boolean z) {
        return z ? 1 : 0;
    }

    boolean readBooleanSetting(String str, boolean z) {
        return Settings.Global.getInt(getContext().getContentResolver(), str, toInt(z)) == 1;
    }

    void writeBooleanSetting(String str, boolean z) {
        Settings.Global.putInt(getContext().getContentResolver(), str, toInt(z));
    }

    private void initializeCec(int i) {
        this.mAddressAllocated = false;
        this.mCecController.setOption(3, true);
        this.mCecController.setLanguage(this.mLanguage);
        initializeLocalDevices(i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void initializeLocalDevices(int i) {
        assertRunOnServiceThread();
        ArrayList<HdmiCecLocalDevice> arrayList = new ArrayList<>();
        Iterator<Integer> it = this.mLocalDevices.iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            HdmiCecLocalDevice localDevice = this.mCecController.getLocalDevice(iIntValue);
            if (localDevice == null) {
                localDevice = HdmiCecLocalDevice.create(this, iIntValue);
            }
            localDevice.init();
            arrayList.add(localDevice);
        }
        clearLocalDevices();
        allocateLogicalAddress(arrayList, i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void allocateLogicalAddress(final ArrayList<HdmiCecLocalDevice> arrayList, final int i) {
        assertRunOnServiceThread();
        this.mCecController.clearLogicalAddress();
        final ArrayList arrayList2 = new ArrayList();
        final int[] iArr = new int[1];
        this.mAddressAllocated = arrayList.isEmpty();
        this.mSelectRequestBuffer.clear();
        for (final HdmiCecLocalDevice hdmiCecLocalDevice : arrayList) {
            this.mCecController.allocateLogicalAddress(hdmiCecLocalDevice.getType(), hdmiCecLocalDevice.getPreferredAddress(), new HdmiCecController.AllocateAddressCallback() {
                @Override
                public void onAllocated(int i2, int i3) {
                    if (i3 != 15) {
                        hdmiCecLocalDevice.setDeviceInfo(HdmiControlService.this.createDeviceInfo(i3, i2, 0));
                        HdmiControlService.this.mCecController.addLocalDevice(i2, hdmiCecLocalDevice);
                        HdmiControlService.this.mCecController.addLogicalAddress(i3);
                        arrayList2.add(hdmiCecLocalDevice);
                    } else {
                        Slog.e(HdmiControlService.TAG, "Failed to allocate address:[device_type:" + i2 + "]");
                    }
                    int size = arrayList.size();
                    int[] iArr2 = iArr;
                    int i4 = iArr2[0] + 1;
                    iArr2[0] = i4;
                    if (size == i4) {
                        HdmiControlService.this.mAddressAllocated = true;
                        if (i != 4) {
                            HdmiControlService.this.onInitializeCecComplete(i);
                        }
                        HdmiControlService.this.notifyAddressAllocated(arrayList2, i);
                        HdmiControlService.this.mCecMessageBuffer.processMessages();
                    }
                }
            });
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void notifyAddressAllocated(ArrayList<HdmiCecLocalDevice> arrayList, int i) {
        assertRunOnServiceThread();
        for (HdmiCecLocalDevice hdmiCecLocalDevice : arrayList) {
            hdmiCecLocalDevice.handleAddressAllocated(hdmiCecLocalDevice.getDeviceInfo().getLogicalAddress(), i);
        }
        if (isTvDeviceEnabled()) {
            tv().setSelectRequestBuffer(this.mSelectRequestBuffer);
        }
    }

    boolean isAddressAllocated() {
        return this.mAddressAllocated;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void initPortInfo() {
        HdmiPortInfo[] portInfos;
        assertRunOnServiceThread();
        if (this.mCecController != null) {
            portInfos = this.mCecController.getPortInfos();
        } else {
            portInfos = null;
        }
        if (portInfos == null) {
            return;
        }
        SparseArray sparseArray = new SparseArray();
        SparseIntArray sparseIntArray = new SparseIntArray();
        SparseArray sparseArray2 = new SparseArray();
        for (HdmiPortInfo hdmiPortInfo : portInfos) {
            sparseIntArray.put(hdmiPortInfo.getAddress(), hdmiPortInfo.getId());
            sparseArray.put(hdmiPortInfo.getId(), hdmiPortInfo);
            sparseArray2.put(hdmiPortInfo.getId(), new HdmiDeviceInfo(hdmiPortInfo.getAddress(), hdmiPortInfo.getId()));
        }
        this.mPortIdMap = new UnmodifiableSparseIntArray(sparseIntArray);
        this.mPortInfoMap = new UnmodifiableSparseArray<>(sparseArray);
        this.mPortDeviceMap = new UnmodifiableSparseArray<>(sparseArray2);
        HdmiPortInfo[] portInfos2 = this.mMhlController.getPortInfos();
        ArraySet arraySet = new ArraySet(portInfos2.length);
        for (HdmiPortInfo hdmiPortInfo2 : portInfos2) {
            if (hdmiPortInfo2.isMhlSupported()) {
                arraySet.add(Integer.valueOf(hdmiPortInfo2.getId()));
            }
        }
        if (arraySet.isEmpty()) {
            this.mPortInfo = Collections.unmodifiableList(Arrays.asList(portInfos));
            return;
        }
        ArrayList arrayList = new ArrayList(portInfos.length);
        for (HdmiPortInfo hdmiPortInfo3 : portInfos) {
            if (arraySet.contains(Integer.valueOf(hdmiPortInfo3.getId()))) {
                arrayList.add(new HdmiPortInfo(hdmiPortInfo3.getId(), hdmiPortInfo3.getType(), hdmiPortInfo3.getAddress(), hdmiPortInfo3.isCecSupported(), true, hdmiPortInfo3.isArcSupported()));
            } else {
                arrayList.add(hdmiPortInfo3);
            }
        }
        this.mPortInfo = Collections.unmodifiableList(arrayList);
    }

    List<HdmiPortInfo> getPortInfo() {
        return this.mPortInfo;
    }

    HdmiPortInfo getPortInfo(int i) {
        return this.mPortInfoMap.get(i, null);
    }

    int portIdToPath(int i) {
        HdmiPortInfo portInfo = getPortInfo(i);
        if (portInfo == null) {
            Slog.e(TAG, "Cannot find the port info: " + i);
            return NetworkConstants.ARP_HWTYPE_RESERVED_HI;
        }
        return portInfo.getAddress();
    }

    int pathToPortId(int i) {
        return this.mPortIdMap.get(i & 61440, -1);
    }

    boolean isValidPortId(int i) {
        return getPortInfo(i) != null;
    }

    Looper getIoLooper() {
        return this.mIoThread.getLooper();
    }

    Looper getServiceLooper() {
        return this.mHandler.getLooper();
    }

    int getPhysicalAddress() {
        return this.mCecController.getPhysicalAddress();
    }

    int getVendorId() {
        return this.mCecController.getVendorId();
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiDeviceInfo getDeviceInfo(int i) {
        assertRunOnServiceThread();
        if (tv() == null) {
            return null;
        }
        return tv().getCecDeviceInfo(i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiDeviceInfo getDeviceInfoByPort(int i) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub localDevice = this.mMhlController.getLocalDevice(i);
        if (localDevice != null) {
            return localDevice.getInfo();
        }
        return null;
    }

    int getCecVersion() {
        return this.mCecController.getVersion();
    }

    boolean isConnectedToArcPort(int i) {
        int iPathToPortId = pathToPortId(i);
        if (iPathToPortId != -1) {
            return this.mPortInfoMap.get(iPathToPortId).isArcSupported();
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isConnected(int i) {
        assertRunOnServiceThread();
        return this.mCecController.isConnected(i);
    }

    void runOnServiceThread(Runnable runnable) {
        this.mHandler.post(runnable);
    }

    void runOnServiceThreadAtFrontOfQueue(Runnable runnable) {
        this.mHandler.postAtFrontOfQueue(runnable);
    }

    private void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mHandler.getLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void sendCecCommand(HdmiCecMessage hdmiCecMessage, SendMessageCallback sendMessageCallback) {
        assertRunOnServiceThread();
        if (this.mMessageValidator.isValid(hdmiCecMessage) == 0) {
            this.mCecController.sendCommand(hdmiCecMessage, sendMessageCallback);
            return;
        }
        HdmiLogger.error("Invalid message type:" + hdmiCecMessage, new Object[0]);
        if (sendMessageCallback != null) {
            sendMessageCallback.onSendCompleted(3);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void sendCecCommand(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        sendCecCommand(hdmiCecMessage, null);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void maySendFeatureAbortCommand(HdmiCecMessage hdmiCecMessage, int i) {
        assertRunOnServiceThread();
        this.mCecController.maySendFeatureAbortCommand(hdmiCecMessage, i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean handleCecCommand(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (!this.mAddressAllocated) {
            this.mCecMessageBuffer.bufferMessage(hdmiCecMessage);
            return true;
        }
        int iIsValid = this.mMessageValidator.isValid(hdmiCecMessage);
        if (iIsValid != 0) {
            if (iIsValid == 3) {
                maySendFeatureAbortCommand(hdmiCecMessage, 3);
            }
            return true;
        }
        return dispatchMessageToLocalDevice(hdmiCecMessage);
    }

    void enableAudioReturnChannel(int i, boolean z) {
        this.mCecController.enableAudioReturnChannel(i, z);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private boolean dispatchMessageToLocalDevice(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        Iterator<HdmiCecLocalDevice> it = this.mCecController.getLocalDeviceList().iterator();
        while (it.hasNext()) {
            if (it.next().dispatchMessage(hdmiCecMessage) && hdmiCecMessage.getDestination() != 15) {
                return true;
            }
        }
        if (hdmiCecMessage.getDestination() != 15) {
            HdmiLogger.warning("Unhandled cec command:" + hdmiCecMessage, new Object[0]);
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void onHotplug(int i, boolean z) {
        assertRunOnServiceThread();
        if (z && !isTvDevice()) {
            ArrayList<HdmiCecLocalDevice> arrayList = new ArrayList<>();
            Iterator<Integer> it = this.mLocalDevices.iterator();
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                HdmiCecLocalDevice localDevice = this.mCecController.getLocalDevice(iIntValue);
                if (localDevice == null) {
                    localDevice = HdmiCecLocalDevice.create(this, iIntValue);
                    localDevice.init();
                }
                arrayList.add(localDevice);
            }
            allocateLogicalAddress(arrayList, 4);
        }
        Iterator<HdmiCecLocalDevice> it2 = this.mCecController.getLocalDeviceList().iterator();
        while (it2.hasNext()) {
            it2.next().onHotplug(i, z);
        }
        announceHotplugEvent(i, z);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void pollDevices(DevicePollingCallback devicePollingCallback, int i, int i2, int i3) {
        assertRunOnServiceThread();
        this.mCecController.pollDevices(devicePollingCallback, i, checkPollStrategy(i2), i3);
    }

    private int checkPollStrategy(int i) {
        int i2 = i & 3;
        if (i2 == 0) {
            throw new IllegalArgumentException("Invalid poll strategy:" + i);
        }
        int i3 = 196608 & i;
        if (i3 == 0) {
            throw new IllegalArgumentException("Invalid iteration strategy:" + i);
        }
        return i2 | i3;
    }

    List<HdmiCecLocalDevice> getAllLocalDevices() {
        assertRunOnServiceThread();
        return this.mCecController.getLocalDeviceList();
    }

    Object getServiceLock() {
        return this.mLock;
    }

    void setAudioStatus(boolean z, int i) {
        if (!isTvDeviceEnabled() || !tv().isSystemAudioActivated()) {
            return;
        }
        AudioManager audioManager = getAudioManager();
        boolean zIsStreamMute = audioManager.isStreamMute(3);
        if (z) {
            if (!zIsStreamMute) {
                audioManager.setStreamMute(3, true);
                return;
            }
            return;
        }
        if (zIsStreamMute) {
            audioManager.setStreamMute(3, false);
        }
        if (i >= 0 && i <= 100) {
            Slog.i(TAG, "volume: " + i);
            audioManager.setStreamVolume(3, i, UsbTerminalTypes.TERMINAL_USB_STREAMING);
        }
    }

    void announceSystemAudioModeChange(boolean z) {
        synchronized (this.mLock) {
            Iterator<SystemAudioModeChangeListenerRecord> it = this.mSystemAudioModeChangeListenerRecords.iterator();
            while (it.hasNext()) {
                invokeSystemAudioModeChangeLocked(it.next().mListener, z);
            }
        }
    }

    private HdmiDeviceInfo createDeviceInfo(int i, int i2, int i3) {
        return new HdmiDeviceInfo(i, getPhysicalAddress(), pathToPortId(getPhysicalAddress()), i2, getVendorId(), Build.MODEL);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleMhlHotplugEvent(int i, boolean z) {
        assertRunOnServiceThread();
        if (z) {
            HdmiMhlLocalDeviceStub hdmiMhlLocalDeviceStub = new HdmiMhlLocalDeviceStub(this, i);
            HdmiMhlLocalDeviceStub hdmiMhlLocalDeviceStubAddLocalDevice = this.mMhlController.addLocalDevice(hdmiMhlLocalDeviceStub);
            if (hdmiMhlLocalDeviceStubAddLocalDevice != null) {
                hdmiMhlLocalDeviceStubAddLocalDevice.onDeviceRemoved();
                Slog.i(TAG, "Old device of port " + i + " is removed");
            }
            invokeDeviceEventListeners(hdmiMhlLocalDeviceStub.getInfo(), 1);
            updateSafeMhlInput();
        } else {
            HdmiMhlLocalDeviceStub hdmiMhlLocalDeviceStubRemoveLocalDevice = this.mMhlController.removeLocalDevice(i);
            if (hdmiMhlLocalDeviceStubRemoveLocalDevice != null) {
                hdmiMhlLocalDeviceStubRemoveLocalDevice.onDeviceRemoved();
                invokeDeviceEventListeners(hdmiMhlLocalDeviceStubRemoveLocalDevice.getInfo(), 2);
                updateSafeMhlInput();
            } else {
                Slog.w(TAG, "No device to remove:[portId=" + i);
            }
        }
        announceHotplugEvent(i, z);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleMhlBusModeChanged(int i, int i2) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub localDevice = this.mMhlController.getLocalDevice(i);
        if (localDevice != null) {
            localDevice.setBusMode(i2);
            return;
        }
        Slog.w(TAG, "No mhl device exists for bus mode change[portId:" + i + ", busmode:" + i2 + "]");
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleMhlBusOvercurrent(int i, boolean z) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub localDevice = this.mMhlController.getLocalDevice(i);
        if (localDevice != null) {
            localDevice.onBusOvercurrentDetected(z);
            return;
        }
        Slog.w(TAG, "No mhl device exists for bus overcurrent event[portId:" + i + "]");
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleMhlDeviceStatusChanged(int i, int i2, int i3) {
        assertRunOnServiceThread();
        HdmiMhlLocalDeviceStub localDevice = this.mMhlController.getLocalDevice(i);
        if (localDevice != null) {
            localDevice.setDeviceStatusChange(i2, i3);
            return;
        }
        Slog.w(TAG, "No mhl device exists for device status event[portId:" + i + ", adopterId:" + i2 + ", deviceId:" + i3 + "]");
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void updateSafeMhlInput() {
        assertRunOnServiceThread();
        List<HdmiDeviceInfo> listEmptyList = Collections.emptyList();
        SparseArray<HdmiMhlLocalDeviceStub> allLocalDevices = this.mMhlController.getAllLocalDevices();
        for (int i = 0; i < allLocalDevices.size(); i++) {
            HdmiMhlLocalDeviceStub hdmiMhlLocalDeviceStubValueAt = allLocalDevices.valueAt(i);
            if (hdmiMhlLocalDeviceStubValueAt.getInfo() != null) {
                if (listEmptyList.isEmpty()) {
                    listEmptyList = new ArrayList<>();
                }
                listEmptyList.add(hdmiMhlLocalDeviceStubValueAt.getInfo());
            }
        }
        synchronized (this.mLock) {
            this.mMhlDevices = listEmptyList;
        }
    }

    @GuardedBy("mLock")
    private List<HdmiDeviceInfo> getMhlDevicesLocked() {
        return this.mMhlDevices;
    }

    private class HdmiMhlVendorCommandListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiMhlVendorCommandListener mListener;

        public HdmiMhlVendorCommandListenerRecord(IHdmiMhlVendorCommandListener iHdmiMhlVendorCommandListener) {
            this.mListener = iHdmiMhlVendorCommandListener;
        }

        @Override
        public void binderDied() {
            HdmiControlService.this.mMhlVendorCommandListenerRecords.remove(this);
        }
    }

    private final class HotplugEventListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiHotplugEventListener mListener;

        public HotplugEventListenerRecord(IHdmiHotplugEventListener iHdmiHotplugEventListener) {
            this.mListener = iHdmiHotplugEventListener;
        }

        @Override
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mHotplugEventListenerRecords.remove(this);
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof HotplugEventListenerRecord) {
                return obj == this || ((HotplugEventListenerRecord) obj).mListener == this.mListener;
            }
            return false;
        }

        public int hashCode() {
            return this.mListener.hashCode();
        }
    }

    private final class DeviceEventListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiDeviceEventListener mListener;

        public DeviceEventListenerRecord(IHdmiDeviceEventListener iHdmiDeviceEventListener) {
            this.mListener = iHdmiDeviceEventListener;
        }

        @Override
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mDeviceEventListenerRecords.remove(this);
            }
        }
    }

    private final class SystemAudioModeChangeListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiSystemAudioModeChangeListener mListener;

        public SystemAudioModeChangeListenerRecord(IHdmiSystemAudioModeChangeListener iHdmiSystemAudioModeChangeListener) {
            this.mListener = iHdmiSystemAudioModeChangeListener;
        }

        @Override
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mSystemAudioModeChangeListenerRecords.remove(this);
            }
        }
    }

    class VendorCommandListenerRecord implements IBinder.DeathRecipient {
        private final int mDeviceType;
        private final IHdmiVendorCommandListener mListener;

        public VendorCommandListenerRecord(IHdmiVendorCommandListener iHdmiVendorCommandListener, int i) {
            this.mListener = iHdmiVendorCommandListener;
            this.mDeviceType = i;
        }

        @Override
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                HdmiControlService.this.mVendorCommandListenerRecords.remove(this);
            }
        }
    }

    private class HdmiRecordListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiRecordListener mListener;

        public HdmiRecordListenerRecord(IHdmiRecordListener iHdmiRecordListener) {
            this.mListener = iHdmiRecordListener;
        }

        @Override
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                if (HdmiControlService.this.mRecordListenerRecord == this) {
                    HdmiControlService.this.mRecordListenerRecord = null;
                }
            }
        }
    }

    private void enforceAccessPermission() {
        getContext().enforceCallingOrSelfPermission(PERMISSION, TAG);
    }

    private final class BinderService extends IHdmiControlService.Stub {
        private BinderService() {
        }

        public int[] getSupportedTypes() {
            HdmiControlService.this.enforceAccessPermission();
            int[] iArr = new int[HdmiControlService.this.mLocalDevices.size()];
            for (int i = 0; i < iArr.length; i++) {
                iArr[i] = ((Integer) HdmiControlService.this.mLocalDevices.get(i)).intValue();
            }
            return iArr;
        }

        public HdmiDeviceInfo getActiveSource() {
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
            if (hdmiCecLocalDeviceTvTv == null) {
                Slog.w(HdmiControlService.TAG, "Local tv device not available");
                return null;
            }
            HdmiCecLocalDevice.ActiveSource activeSource = hdmiCecLocalDeviceTvTv.getActiveSource();
            if (activeSource.isValid()) {
                return new HdmiDeviceInfo(activeSource.logicalAddress, activeSource.physicalAddress, -1, -1, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            int activePath = hdmiCecLocalDeviceTvTv.getActivePath();
            if (activePath == 65535) {
                return null;
            }
            HdmiDeviceInfo safeDeviceInfoByPath = hdmiCecLocalDeviceTvTv.getSafeDeviceInfoByPath(activePath);
            return safeDeviceInfoByPath != null ? safeDeviceInfoByPath : new HdmiDeviceInfo(activePath, hdmiCecLocalDeviceTvTv.getActivePortId());
        }

        public void deviceSelect(final int i, final IHdmiControlCallback iHdmiControlCallback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    if (iHdmiControlCallback == null) {
                        Slog.e(HdmiControlService.TAG, "Callback cannot be null");
                        return;
                    }
                    HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
                    if (hdmiCecLocalDeviceTvTv == null) {
                        if (!HdmiControlService.this.mAddressAllocated) {
                            HdmiControlService.this.mSelectRequestBuffer.set(SelectRequestBuffer.newDeviceSelect(HdmiControlService.this, i, iHdmiControlCallback));
                            return;
                        } else {
                            Slog.w(HdmiControlService.TAG, "Local tv device not available");
                            HdmiControlService.this.invokeCallback(iHdmiControlCallback, 2);
                            return;
                        }
                    }
                    HdmiMhlLocalDeviceStub localDeviceById = HdmiControlService.this.mMhlController.getLocalDeviceById(i);
                    if (localDeviceById != null) {
                        if (localDeviceById.getPortId() == hdmiCecLocalDeviceTvTv.getActivePortId()) {
                            HdmiControlService.this.invokeCallback(iHdmiControlCallback, 0);
                            return;
                        } else {
                            localDeviceById.turnOn(iHdmiControlCallback);
                            hdmiCecLocalDeviceTvTv.doManualPortSwitching(localDeviceById.getPortId(), null);
                            return;
                        }
                    }
                    hdmiCecLocalDeviceTvTv.deviceSelect(i, iHdmiControlCallback);
                }
            });
        }

        public void portSelect(final int i, final IHdmiControlCallback iHdmiControlCallback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    if (iHdmiControlCallback == null) {
                        Slog.e(HdmiControlService.TAG, "Callback cannot be null");
                        return;
                    }
                    HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
                    if (hdmiCecLocalDeviceTvTv == null) {
                        if (!HdmiControlService.this.mAddressAllocated) {
                            HdmiControlService.this.mSelectRequestBuffer.set(SelectRequestBuffer.newPortSelect(HdmiControlService.this, i, iHdmiControlCallback));
                            return;
                        } else {
                            Slog.w(HdmiControlService.TAG, "Local tv device not available");
                            HdmiControlService.this.invokeCallback(iHdmiControlCallback, 2);
                            return;
                        }
                    }
                    hdmiCecLocalDeviceTvTv.doManualPortSwitching(i, iHdmiControlCallback);
                }
            });
        }

        public void sendKeyEvent(final int i, final int i2, final boolean z) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiMhlLocalDeviceStub localDevice = HdmiControlService.this.mMhlController.getLocalDevice(HdmiControlService.this.mActivePortId);
                    if (localDevice == null) {
                        if (HdmiControlService.this.mCecController != null) {
                            HdmiCecLocalDevice localDevice2 = HdmiControlService.this.mCecController.getLocalDevice(i);
                            if (localDevice2 == null) {
                                Slog.w(HdmiControlService.TAG, "Local device not available");
                                return;
                            } else {
                                localDevice2.sendKeyEvent(i2, z);
                                return;
                            }
                        }
                        return;
                    }
                    localDevice.sendKeyEvent(i2, z);
                }
            });
        }

        public void oneTouchPlay(final IHdmiControlCallback iHdmiControlCallback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiControlService.this.oneTouchPlay(iHdmiControlCallback);
                }
            });
        }

        public void queryDisplayStatus(final IHdmiControlCallback iHdmiControlCallback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiControlService.this.queryDisplayStatus(iHdmiControlCallback);
                }
            });
        }

        public void addHotplugEventListener(IHdmiHotplugEventListener iHdmiHotplugEventListener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addHotplugEventListener(iHdmiHotplugEventListener);
        }

        public void removeHotplugEventListener(IHdmiHotplugEventListener iHdmiHotplugEventListener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.removeHotplugEventListener(iHdmiHotplugEventListener);
        }

        public void addDeviceEventListener(IHdmiDeviceEventListener iHdmiDeviceEventListener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addDeviceEventListener(iHdmiDeviceEventListener);
        }

        public List<HdmiPortInfo> getPortInfo() {
            HdmiControlService.this.enforceAccessPermission();
            return HdmiControlService.this.getPortInfo();
        }

        public boolean canChangeSystemAudioMode() {
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
            if (hdmiCecLocalDeviceTvTv == null) {
                return false;
            }
            return hdmiCecLocalDeviceTvTv.hasSystemAudioDevice();
        }

        public boolean getSystemAudioMode() {
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
            if (hdmiCecLocalDeviceTvTv == null) {
                return false;
            }
            return hdmiCecLocalDeviceTvTv.isSystemAudioActivated();
        }

        public void setSystemAudioMode(final boolean z, final IHdmiControlCallback iHdmiControlCallback) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
                    if (hdmiCecLocalDeviceTvTv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                        HdmiControlService.this.invokeCallback(iHdmiControlCallback, 2);
                    } else {
                        hdmiCecLocalDeviceTvTv.changeSystemAudioMode(z, iHdmiControlCallback);
                    }
                }
            });
        }

        public void addSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener iHdmiSystemAudioModeChangeListener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addSystemAudioModeChangeListner(iHdmiSystemAudioModeChangeListener);
        }

        public void removeSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener iHdmiSystemAudioModeChangeListener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.removeSystemAudioModeChangeListener(iHdmiSystemAudioModeChangeListener);
        }

        public void setInputChangeListener(IHdmiInputChangeListener iHdmiInputChangeListener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.setInputChangeListener(iHdmiInputChangeListener);
        }

        public List<HdmiDeviceInfo> getInputDevices() {
            List<HdmiDeviceInfo> safeExternalInputsLocked;
            List<HdmiDeviceInfo> listMergeToUnmodifiableList;
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
            synchronized (HdmiControlService.this.mLock) {
                try {
                    if (hdmiCecLocalDeviceTvTv == null) {
                        safeExternalInputsLocked = Collections.emptyList();
                    } else {
                        safeExternalInputsLocked = hdmiCecLocalDeviceTvTv.getSafeExternalInputsLocked();
                    }
                    listMergeToUnmodifiableList = HdmiUtils.mergeToUnmodifiableList(safeExternalInputsLocked, HdmiControlService.this.getMhlDevicesLocked());
                } catch (Throwable th) {
                    throw th;
                }
            }
            return listMergeToUnmodifiableList;
        }

        public List<HdmiDeviceInfo> getDeviceList() {
            List<HdmiDeviceInfo> safeCecDevicesLocked;
            HdmiControlService.this.enforceAccessPermission();
            HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
            synchronized (HdmiControlService.this.mLock) {
                try {
                    if (hdmiCecLocalDeviceTvTv == null) {
                        safeCecDevicesLocked = Collections.emptyList();
                    } else {
                        safeCecDevicesLocked = hdmiCecLocalDeviceTvTv.getSafeCecDevicesLocked();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return safeCecDevicesLocked;
        }

        public void setSystemAudioVolume(final int i, final int i2, final int i3) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
                    if (hdmiCecLocalDeviceTvTv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                    } else {
                        hdmiCecLocalDeviceTvTv.changeVolume(i, i2 - i, i3);
                    }
                }
            });
        }

        public void setSystemAudioMute(final boolean z) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = HdmiControlService.this.tv();
                    if (hdmiCecLocalDeviceTvTv == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available");
                    } else {
                        hdmiCecLocalDeviceTvTv.changeMute(z);
                    }
                }
            });
        }

        public void setArcMode(boolean z) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    if (HdmiControlService.this.tv() == null) {
                        Slog.w(HdmiControlService.TAG, "Local tv device not available to change arc mode.");
                    }
                }
            });
        }

        public void setProhibitMode(boolean z) {
            HdmiControlService.this.enforceAccessPermission();
            if (!HdmiControlService.this.isTvDevice()) {
                return;
            }
            HdmiControlService.this.setProhibitMode(z);
        }

        public void addVendorCommandListener(IHdmiVendorCommandListener iHdmiVendorCommandListener, int i) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addVendorCommandListener(iHdmiVendorCommandListener, i);
        }

        public void sendVendorCommand(final int i, final int i2, final byte[] bArr, final boolean z) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiCecLocalDevice localDevice = HdmiControlService.this.mCecController.getLocalDevice(i);
                    if (localDevice == null) {
                        Slog.w(HdmiControlService.TAG, "Local device not available");
                    } else if (z) {
                        HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildVendorCommandWithId(localDevice.getDeviceInfo().getLogicalAddress(), i2, HdmiControlService.this.getVendorId(), bArr));
                    } else {
                        HdmiControlService.this.sendCecCommand(HdmiCecMessageBuilder.buildVendorCommand(localDevice.getDeviceInfo().getLogicalAddress(), i2, bArr));
                    }
                }
            });
        }

        public void sendStandby(final int i, final int i2) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiMhlLocalDeviceStub localDeviceById = HdmiControlService.this.mMhlController.getLocalDeviceById(i2);
                    if (localDeviceById == null) {
                        HdmiCecLocalDevice localDevice = HdmiControlService.this.mCecController.getLocalDevice(i);
                        if (localDevice == null) {
                            Slog.w(HdmiControlService.TAG, "Local device not available");
                            return;
                        } else {
                            localDevice.sendStandby(i2);
                            return;
                        }
                    }
                    localDeviceById.sendStandby();
                }
            });
        }

        public void setHdmiRecordListener(IHdmiRecordListener iHdmiRecordListener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.setHdmiRecordListener(iHdmiRecordListener);
        }

        public void startOneTouchRecord(final int i, final byte[] bArr) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    if (!HdmiControlService.this.isTvDeviceEnabled()) {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    } else {
                        HdmiControlService.this.tv().startOneTouchRecord(i, bArr);
                    }
                }
            });
        }

        public void stopOneTouchRecord(final int i) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    if (!HdmiControlService.this.isTvDeviceEnabled()) {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    } else {
                        HdmiControlService.this.tv().stopOneTouchRecord(i);
                    }
                }
            });
        }

        public void startTimerRecording(final int i, final int i2, final byte[] bArr) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    if (!HdmiControlService.this.isTvDeviceEnabled()) {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    } else {
                        HdmiControlService.this.tv().startTimerRecording(i, i2, bArr);
                    }
                }
            });
        }

        public void clearTimerRecording(final int i, final int i2, final byte[] bArr) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    if (!HdmiControlService.this.isTvDeviceEnabled()) {
                        Slog.w(HdmiControlService.TAG, "TV device is not enabled.");
                    } else {
                        HdmiControlService.this.tv().clearTimerRecording(i, i2, bArr);
                    }
                }
            });
        }

        public void sendMhlVendorCommand(final int i, final int i2, final int i3, final byte[] bArr) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    if (!HdmiControlService.this.isControlEnabled()) {
                        Slog.w(HdmiControlService.TAG, "Hdmi control is disabled.");
                        return;
                    }
                    if (HdmiControlService.this.mMhlController.getLocalDevice(i) != null) {
                        HdmiControlService.this.mMhlController.sendVendorCommand(i, i2, i3, bArr);
                        return;
                    }
                    Slog.w(HdmiControlService.TAG, "Invalid port id:" + i);
                }
            });
        }

        public void addHdmiMhlVendorCommandListener(IHdmiMhlVendorCommandListener iHdmiMhlVendorCommandListener) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.addHdmiMhlVendorCommandListener(iHdmiMhlVendorCommandListener);
        }

        public void setStandbyMode(final boolean z) {
            HdmiControlService.this.enforceAccessPermission();
            HdmiControlService.this.runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiControlService.this.setStandbyMode(z);
                }
            });
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(HdmiControlService.this.getContext(), HdmiControlService.TAG, printWriter)) {
                IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
                indentingPrintWriter.println("mHdmiControlEnabled: " + HdmiControlService.this.mHdmiControlEnabled);
                indentingPrintWriter.println("mProhibitMode: " + HdmiControlService.this.mProhibitMode);
                if (HdmiControlService.this.mCecController != null) {
                    indentingPrintWriter.println("mCecController: ");
                    indentingPrintWriter.increaseIndent();
                    HdmiControlService.this.mCecController.dump(indentingPrintWriter);
                    indentingPrintWriter.decreaseIndent();
                }
                indentingPrintWriter.println("mMhlController: ");
                indentingPrintWriter.increaseIndent();
                HdmiControlService.this.mMhlController.dump(indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("mPortInfo: ");
                indentingPrintWriter.increaseIndent();
                Iterator it = HdmiControlService.this.mPortInfo.iterator();
                while (it.hasNext()) {
                    indentingPrintWriter.println("- " + ((HdmiPortInfo) it.next()));
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("mPowerStatus: " + HdmiControlService.this.mPowerStatus);
            }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void oneTouchPlay(IHdmiControlCallback iHdmiControlCallback) {
        assertRunOnServiceThread();
        HdmiCecLocalDevicePlayback hdmiCecLocalDevicePlaybackPlayback = playback();
        if (hdmiCecLocalDevicePlaybackPlayback == null) {
            Slog.w(TAG, "Local playback device not available");
            invokeCallback(iHdmiControlCallback, 2);
        } else {
            hdmiCecLocalDevicePlaybackPlayback.oneTouchPlay(iHdmiControlCallback);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void queryDisplayStatus(IHdmiControlCallback iHdmiControlCallback) {
        assertRunOnServiceThread();
        HdmiCecLocalDevicePlayback hdmiCecLocalDevicePlaybackPlayback = playback();
        if (hdmiCecLocalDevicePlaybackPlayback == null) {
            Slog.w(TAG, "Local playback device not available");
            invokeCallback(iHdmiControlCallback, 2);
        } else {
            hdmiCecLocalDevicePlaybackPlayback.queryDisplayStatus(iHdmiControlCallback);
        }
    }

    private void addHotplugEventListener(final IHdmiHotplugEventListener iHdmiHotplugEventListener) {
        final HotplugEventListenerRecord hotplugEventListenerRecord = new HotplugEventListenerRecord(iHdmiHotplugEventListener);
        try {
            iHdmiHotplugEventListener.asBinder().linkToDeath(hotplugEventListenerRecord, 0);
            synchronized (this.mLock) {
                this.mHotplugEventListenerRecords.add(hotplugEventListenerRecord);
            }
            runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (HdmiControlService.this.mLock) {
                        if (HdmiControlService.this.mHotplugEventListenerRecords.contains(hotplugEventListenerRecord)) {
                            for (HdmiPortInfo hdmiPortInfo : HdmiControlService.this.mPortInfo) {
                                HdmiHotplugEvent hdmiHotplugEvent = new HdmiHotplugEvent(hdmiPortInfo.getId(), HdmiControlService.this.mCecController.isConnected(hdmiPortInfo.getId()));
                                synchronized (HdmiControlService.this.mLock) {
                                    HdmiControlService.this.invokeHotplugEventListenerLocked(iHdmiHotplugEventListener, hdmiHotplugEvent);
                                }
                            }
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    private void removeHotplugEventListener(IHdmiHotplugEventListener iHdmiHotplugEventListener) {
        synchronized (this.mLock) {
            Iterator<HotplugEventListenerRecord> it = this.mHotplugEventListenerRecords.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                HotplugEventListenerRecord next = it.next();
                if (next.mListener.asBinder() == iHdmiHotplugEventListener.asBinder()) {
                    iHdmiHotplugEventListener.asBinder().unlinkToDeath(next, 0);
                    this.mHotplugEventListenerRecords.remove(next);
                    break;
                }
            }
        }
    }

    private void addDeviceEventListener(IHdmiDeviceEventListener iHdmiDeviceEventListener) {
        DeviceEventListenerRecord deviceEventListenerRecord = new DeviceEventListenerRecord(iHdmiDeviceEventListener);
        try {
            iHdmiDeviceEventListener.asBinder().linkToDeath(deviceEventListenerRecord, 0);
            synchronized (this.mLock) {
                this.mDeviceEventListenerRecords.add(deviceEventListenerRecord);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    void invokeDeviceEventListeners(HdmiDeviceInfo hdmiDeviceInfo, int i) {
        synchronized (this.mLock) {
            Iterator<DeviceEventListenerRecord> it = this.mDeviceEventListenerRecords.iterator();
            while (it.hasNext()) {
                try {
                    it.next().mListener.onStatusChanged(hdmiDeviceInfo, i);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to report device event:" + e);
                }
            }
        }
    }

    private void addSystemAudioModeChangeListner(IHdmiSystemAudioModeChangeListener iHdmiSystemAudioModeChangeListener) {
        SystemAudioModeChangeListenerRecord systemAudioModeChangeListenerRecord = new SystemAudioModeChangeListenerRecord(iHdmiSystemAudioModeChangeListener);
        try {
            iHdmiSystemAudioModeChangeListener.asBinder().linkToDeath(systemAudioModeChangeListenerRecord, 0);
            synchronized (this.mLock) {
                this.mSystemAudioModeChangeListenerRecords.add(systemAudioModeChangeListenerRecord);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    private void removeSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener iHdmiSystemAudioModeChangeListener) {
        synchronized (this.mLock) {
            Iterator<SystemAudioModeChangeListenerRecord> it = this.mSystemAudioModeChangeListenerRecords.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                SystemAudioModeChangeListenerRecord next = it.next();
                if (next.mListener.asBinder() == iHdmiSystemAudioModeChangeListener) {
                    iHdmiSystemAudioModeChangeListener.asBinder().unlinkToDeath(next, 0);
                    this.mSystemAudioModeChangeListenerRecords.remove(next);
                    break;
                }
            }
        }
    }

    private final class InputChangeListenerRecord implements IBinder.DeathRecipient {
        private final IHdmiInputChangeListener mListener;

        public InputChangeListenerRecord(IHdmiInputChangeListener iHdmiInputChangeListener) {
            this.mListener = iHdmiInputChangeListener;
        }

        @Override
        public void binderDied() {
            synchronized (HdmiControlService.this.mLock) {
                if (HdmiControlService.this.mInputChangeListenerRecord == this) {
                    HdmiControlService.this.mInputChangeListenerRecord = null;
                }
            }
        }
    }

    private void setInputChangeListener(IHdmiInputChangeListener iHdmiInputChangeListener) {
        synchronized (this.mLock) {
            this.mInputChangeListenerRecord = new InputChangeListenerRecord(iHdmiInputChangeListener);
            try {
                iHdmiInputChangeListener.asBinder().linkToDeath(this.mInputChangeListenerRecord, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "Listener already died");
            }
        }
    }

    void invokeInputChangeListener(HdmiDeviceInfo hdmiDeviceInfo) {
        synchronized (this.mLock) {
            if (this.mInputChangeListenerRecord != null) {
                try {
                    this.mInputChangeListenerRecord.mListener.onChanged(hdmiDeviceInfo);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Exception thrown by IHdmiInputChangeListener: " + e);
                }
            }
        }
    }

    private void setHdmiRecordListener(IHdmiRecordListener iHdmiRecordListener) {
        synchronized (this.mLock) {
            this.mRecordListenerRecord = new HdmiRecordListenerRecord(iHdmiRecordListener);
            try {
                iHdmiRecordListener.asBinder().linkToDeath(this.mRecordListenerRecord, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "Listener already died.", e);
            }
        }
    }

    byte[] invokeRecordRequestListener(int i) {
        synchronized (this.mLock) {
            if (this.mRecordListenerRecord != null) {
                try {
                    return this.mRecordListenerRecord.mListener.getOneTouchRecordSource(i);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to start record.", e);
                }
            }
            return EmptyArray.BYTE;
        }
    }

    void invokeOneTouchRecordResult(int i, int i2) {
        synchronized (this.mLock) {
            if (this.mRecordListenerRecord != null) {
                try {
                    this.mRecordListenerRecord.mListener.onOneTouchRecordResult(i, i2);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onOneTouchRecordResult.", e);
                }
            }
        }
    }

    void invokeTimerRecordingResult(int i, int i2) {
        synchronized (this.mLock) {
            if (this.mRecordListenerRecord != null) {
                try {
                    this.mRecordListenerRecord.mListener.onTimerRecordingResult(i, i2);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onTimerRecordingResult.", e);
                }
            }
        }
    }

    void invokeClearTimerRecordingResult(int i, int i2) {
        synchronized (this.mLock) {
            if (this.mRecordListenerRecord != null) {
                try {
                    this.mRecordListenerRecord.mListener.onClearTimerRecordingResult(i, i2);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to call onClearTimerRecordingResult.", e);
                }
            }
        }
    }

    private void invokeCallback(IHdmiControlCallback iHdmiControlCallback, int i) {
        try {
            iHdmiControlCallback.onComplete(i);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    private void invokeSystemAudioModeChangeLocked(IHdmiSystemAudioModeChangeListener iHdmiSystemAudioModeChangeListener, boolean z) {
        try {
            iHdmiSystemAudioModeChangeListener.onStatusChanged(z);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    private void announceHotplugEvent(int i, boolean z) {
        HdmiHotplugEvent hdmiHotplugEvent = new HdmiHotplugEvent(i, z);
        synchronized (this.mLock) {
            Iterator<HotplugEventListenerRecord> it = this.mHotplugEventListenerRecords.iterator();
            while (it.hasNext()) {
                invokeHotplugEventListenerLocked(it.next().mListener, hdmiHotplugEvent);
            }
        }
    }

    private void invokeHotplugEventListenerLocked(IHdmiHotplugEventListener iHdmiHotplugEventListener, HdmiHotplugEvent hdmiHotplugEvent) {
        try {
            iHdmiHotplugEventListener.onReceived(hdmiHotplugEvent);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to report hotplug event:" + hdmiHotplugEvent.toString(), e);
        }
    }

    public HdmiCecLocalDeviceTv tv() {
        return (HdmiCecLocalDeviceTv) this.mCecController.getLocalDevice(0);
    }

    boolean isTvDevice() {
        return this.mLocalDevices.contains(0);
    }

    boolean isTvDeviceEnabled() {
        return isTvDevice() && tv() != null;
    }

    private HdmiCecLocalDevicePlayback playback() {
        return (HdmiCecLocalDevicePlayback) this.mCecController.getLocalDevice(4);
    }

    AudioManager getAudioManager() {
        return (AudioManager) getContext().getSystemService("audio");
    }

    boolean isControlEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mHdmiControlEnabled;
        }
        return z;
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getPowerStatus() {
        assertRunOnServiceThread();
        return this.mPowerStatus;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isPowerOnOrTransient() {
        assertRunOnServiceThread();
        return this.mPowerStatus == 0 || this.mPowerStatus == 2;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isPowerStandbyOrTransient() {
        assertRunOnServiceThread();
        return this.mPowerStatus == 1 || this.mPowerStatus == 3;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isPowerStandby() {
        assertRunOnServiceThread();
        return this.mPowerStatus == 1;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void wakeUp() {
        assertRunOnServiceThread();
        this.mWakeUpMessageReceived = true;
        this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.hdmi:WAKE");
    }

    @HdmiAnnotations.ServiceThreadOnly
    void standby() {
        assertRunOnServiceThread();
        if (!canGoToStandby()) {
            return;
        }
        this.mStandbyMessageReceived = true;
        this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 5, 0);
    }

    boolean isWakeUpMessageReceived() {
        return this.mWakeUpMessageReceived;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void onWakeUp() {
        assertRunOnServiceThread();
        int i = 2;
        this.mPowerStatus = 2;
        if (this.mCecController != null) {
            if (this.mHdmiControlEnabled) {
                if (this.mWakeUpMessageReceived) {
                    i = 3;
                }
                initializeCec(i);
                return;
            }
            return;
        }
        Slog.i(TAG, "Device does not support HDMI-CEC.");
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void onStandby(final int i) {
        assertRunOnServiceThread();
        this.mPowerStatus = 3;
        invokeVendorCommandListenersOnControlStateChanged(false, 3);
        if (!canGoToStandby()) {
            this.mPowerStatus = 1;
        } else {
            final List<HdmiCecLocalDevice> allLocalDevices = getAllLocalDevices();
            disableDevices(new HdmiCecLocalDevice.PendingActionClearedCallback() {
                @Override
                public void onCleared(HdmiCecLocalDevice hdmiCecLocalDevice) {
                    Slog.v(HdmiControlService.TAG, "On standby-action cleared:" + hdmiCecLocalDevice.mDeviceType);
                    allLocalDevices.remove(hdmiCecLocalDevice);
                    if (allLocalDevices.isEmpty()) {
                        HdmiControlService.this.onStandbyCompleted(i);
                    }
                }
            });
        }
    }

    private boolean canGoToStandby() {
        Iterator<HdmiCecLocalDevice> it = this.mCecController.getLocalDeviceList().iterator();
        while (it.hasNext()) {
            if (!it.next().canGoToStandby()) {
                return false;
            }
        }
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void onLanguageChanged(String str) {
        assertRunOnServiceThread();
        this.mLanguage = str;
        if (isTvDeviceEnabled()) {
            tv().broadcastMenuLanguage(str);
            this.mCecController.setLanguage(str);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    String getLanguage() {
        assertRunOnServiceThread();
        return this.mLanguage;
    }

    private void disableDevices(HdmiCecLocalDevice.PendingActionClearedCallback pendingActionClearedCallback) {
        if (this.mCecController != null) {
            Iterator<HdmiCecLocalDevice> it = this.mCecController.getLocalDeviceList().iterator();
            while (it.hasNext()) {
                it.next().disableDevice(this.mStandbyMessageReceived, pendingActionClearedCallback);
            }
        }
        this.mMhlController.clearAllLocalDevices();
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void clearLocalDevices() {
        assertRunOnServiceThread();
        if (this.mCecController == null) {
            return;
        }
        this.mCecController.clearLogicalAddress();
        this.mCecController.clearLocalDevices();
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void onStandbyCompleted(int i) {
        assertRunOnServiceThread();
        Slog.v(TAG, "onStandbyCompleted");
        if (this.mPowerStatus != 3) {
            return;
        }
        this.mPowerStatus = 1;
        Iterator<HdmiCecLocalDevice> it = this.mCecController.getLocalDeviceList().iterator();
        while (it.hasNext()) {
            it.next().onStandby(this.mStandbyMessageReceived, i);
        }
        this.mStandbyMessageReceived = false;
        this.mCecController.setOption(3, false);
        this.mMhlController.setOption(HdmiCecKeycode.CEC_KEYCODE_SELECT_MEDIA_FUNCTION, 0);
    }

    private void addVendorCommandListener(IHdmiVendorCommandListener iHdmiVendorCommandListener, int i) {
        VendorCommandListenerRecord vendorCommandListenerRecord = new VendorCommandListenerRecord(iHdmiVendorCommandListener, i);
        try {
            iHdmiVendorCommandListener.asBinder().linkToDeath(vendorCommandListenerRecord, 0);
            synchronized (this.mLock) {
                this.mVendorCommandListenerRecords.add(vendorCommandListenerRecord);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died");
        }
    }

    boolean invokeVendorCommandListenersOnReceived(int i, int i2, int i3, byte[] bArr, boolean z) {
        synchronized (this.mLock) {
            if (this.mVendorCommandListenerRecords.isEmpty()) {
                return false;
            }
            for (VendorCommandListenerRecord vendorCommandListenerRecord : this.mVendorCommandListenerRecords) {
                if (vendorCommandListenerRecord.mDeviceType == i) {
                    try {
                        vendorCommandListenerRecord.mListener.onReceived(i2, i3, bArr, z);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to notify vendor command reception", e);
                    }
                }
            }
            return true;
        }
    }

    boolean invokeVendorCommandListenersOnControlStateChanged(boolean z, int i) {
        synchronized (this.mLock) {
            if (this.mVendorCommandListenerRecords.isEmpty()) {
                return false;
            }
            Iterator<VendorCommandListenerRecord> it = this.mVendorCommandListenerRecords.iterator();
            while (it.hasNext()) {
                try {
                    it.next().mListener.onControlStateChanged(z, i);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to notify control-state-changed to vendor handler", e);
                }
            }
            return true;
        }
    }

    private void addHdmiMhlVendorCommandListener(IHdmiMhlVendorCommandListener iHdmiMhlVendorCommandListener) {
        HdmiMhlVendorCommandListenerRecord hdmiMhlVendorCommandListenerRecord = new HdmiMhlVendorCommandListenerRecord(iHdmiMhlVendorCommandListener);
        try {
            iHdmiMhlVendorCommandListener.asBinder().linkToDeath(hdmiMhlVendorCommandListenerRecord, 0);
            synchronized (this.mLock) {
                this.mMhlVendorCommandListenerRecords.add(hdmiMhlVendorCommandListenerRecord);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Listener already died.");
        }
    }

    void invokeMhlVendorCommandListeners(int i, int i2, int i3, byte[] bArr) {
        synchronized (this.mLock) {
            Iterator<HdmiMhlVendorCommandListenerRecord> it = this.mMhlVendorCommandListenerRecords.iterator();
            while (it.hasNext()) {
                try {
                    it.next().mListener.onReceived(i, i2, i3, bArr);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to notify MHL vendor command", e);
                }
            }
        }
    }

    void setStandbyMode(boolean z) {
        assertRunOnServiceThread();
        if (isPowerOnOrTransient() && z) {
            this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 5, 0);
            if (playback() != null) {
                playback().sendStandby(0);
                return;
            }
            return;
        }
        if (isPowerStandbyOrTransient() && !z) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.hdmi:WAKE");
            if (playback() != null) {
                oneTouchPlay(new IHdmiControlCallback.Stub() {
                    public void onComplete(int i) {
                        if (i != 0) {
                            Slog.w(HdmiControlService.TAG, "Failed to complete 'one touch play'. result=" + i);
                        }
                    }
                });
            }
        }
    }

    boolean isProhibitMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mProhibitMode;
        }
        return z;
    }

    void setProhibitMode(boolean z) {
        synchronized (this.mLock) {
            this.mProhibitMode = z;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setCecOption(int i, boolean z) {
        assertRunOnServiceThread();
        this.mCecController.setOption(i, z);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setControlEnabled(boolean z) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            this.mHdmiControlEnabled = z;
        }
        if (z) {
            enableHdmiControlService();
        } else {
            invokeVendorCommandListenersOnControlStateChanged(false, 1);
            runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    HdmiControlService.this.disableHdmiControlService();
                }
            });
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void enableHdmiControlService() {
        this.mCecController.setOption(3, true);
        this.mMhlController.setOption(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION, 1);
        initializeCec(0);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void disableHdmiControlService() {
        disableDevices(new HdmiCecLocalDevice.PendingActionClearedCallback() {
            @Override
            public void onCleared(HdmiCecLocalDevice hdmiCecLocalDevice) {
                HdmiControlService.this.assertRunOnServiceThread();
                HdmiControlService.this.mCecController.flush(new Runnable() {
                    @Override
                    public void run() {
                        HdmiControlService.this.mCecController.setOption(2, false);
                        HdmiControlService.this.mMhlController.setOption(HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION, 0);
                        HdmiControlService.this.clearLocalDevices();
                    }
                });
            }
        });
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setActivePortId(int i) {
        assertRunOnServiceThread();
        this.mActivePortId = i;
        setLastInputForMhl(-1);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setLastInputForMhl(int i) {
        assertRunOnServiceThread();
        this.mLastInputMhl = i;
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getLastInputForMhl() {
        assertRunOnServiceThread();
        return this.mLastInputMhl;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeInputForMhl(int i, boolean z) {
        assertRunOnServiceThread();
        if (tv() == null) {
            return;
        }
        final int activePortId = z ? tv().getActivePortId() : -1;
        if (i != -1) {
            tv().doManualPortSwitching(i, new IHdmiControlCallback.Stub() {
                public void onComplete(int i2) throws RemoteException {
                    HdmiControlService.this.setLastInputForMhl(activePortId);
                }
            });
        }
        tv().setActivePortId(i);
        HdmiMhlLocalDeviceStub localDevice = this.mMhlController.getLocalDevice(i);
        invokeInputChangeListener(localDevice != null ? localDevice.getInfo() : this.mPortDeviceMap.get(i, HdmiDeviceInfo.INACTIVE_DEVICE));
    }

    void setMhlInputChangeEnabled(boolean z) {
        this.mMhlController.setOption(101, toInt(z));
        synchronized (this.mLock) {
            this.mMhlInputChangeEnabled = z;
        }
    }

    boolean isMhlInputChangeEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mMhlInputChangeEnabled;
        }
        return z;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void displayOsd(int i) {
        assertRunOnServiceThread();
        Intent intent = new Intent("android.hardware.hdmi.action.OSD_MESSAGE");
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_ID", i);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void displayOsd(int i, int i2) {
        assertRunOnServiceThread();
        Intent intent = new Intent("android.hardware.hdmi.action.OSD_MESSAGE");
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_ID", i);
        intent.putExtra("android.hardware.hdmi.extra.MESSAGE_EXTRA_PARAM1", i2);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, PERMISSION);
    }
}
