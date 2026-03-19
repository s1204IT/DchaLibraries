package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiPortInfo;
import android.hardware.hdmi.HdmiRecordSources;
import android.hardware.hdmi.HdmiTimerRecordSources;
import android.hardware.hdmi.IHdmiControlCallback;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.DeviceDiscoveryAction;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecLocalDevice;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.pm.PackageManagerService;
import com.android.server.usb.UsbAudioDevice;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

final class HdmiCecLocalDeviceTv extends HdmiCecLocalDevice {
    private static final String TAG = "HdmiCecLocalDeviceTv";

    @HdmiAnnotations.ServiceThreadOnly
    private boolean mArcEstablished;
    private final SparseBooleanArray mArcFeatureEnabled;
    private boolean mAutoDeviceOff;
    private boolean mAutoWakeup;
    private final ArraySet<Integer> mCecSwitches;
    private final DelayedMessageBuffer mDelayedMessageBuffer;
    private final SparseArray<HdmiDeviceInfo> mDeviceInfos;
    private List<Integer> mLocalDeviceAddresses;

    @GuardedBy("mLock")
    private int mPrevPortId;

    @GuardedBy("mLock")
    private List<HdmiDeviceInfo> mSafeAllDeviceInfos;

    @GuardedBy("mLock")
    private List<HdmiDeviceInfo> mSafeExternalInputs;
    private SelectRequestBuffer mSelectRequestBuffer;
    private boolean mSkipRoutingControl;
    private final HdmiCecStandbyModeHandler mStandbyHandler;

    @GuardedBy("mLock")
    private boolean mSystemAudioActivated;

    @GuardedBy("mLock")
    private boolean mSystemAudioMute;

    @GuardedBy("mLock")
    private int mSystemAudioVolume;
    private final TvInputManager.TvInputCallback mTvInputCallback;
    private final HashMap<String, Integer> mTvInputs;

    @HdmiAnnotations.ServiceThreadOnly
    private void addTvInput(String inputId, int deviceId) {
        assertRunOnServiceThread();
        this.mTvInputs.put(inputId, Integer.valueOf(deviceId));
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void removeTvInput(String inputId) {
        assertRunOnServiceThread();
        this.mTvInputs.remove(inputId);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean isInputReady(int deviceId) {
        assertRunOnServiceThread();
        return this.mTvInputs.containsValue(Integer.valueOf(deviceId));
    }

    HdmiCecLocalDeviceTv(HdmiControlService service) {
        super(service, 0);
        this.mArcEstablished = false;
        this.mArcFeatureEnabled = new SparseBooleanArray();
        this.mSystemAudioActivated = false;
        this.mSystemAudioVolume = -1;
        this.mSystemAudioMute = false;
        this.mSafeAllDeviceInfos = Collections.emptyList();
        this.mSafeExternalInputs = Collections.emptyList();
        this.mDeviceInfos = new SparseArray<>();
        this.mCecSwitches = new ArraySet<>();
        this.mDelayedMessageBuffer = new DelayedMessageBuffer(this);
        this.mTvInputCallback = new TvInputManager.TvInputCallback() {
            @Override
            public void onInputAdded(String inputId) {
                HdmiDeviceInfo info;
                TvInputInfo tvInfo = HdmiCecLocalDeviceTv.this.mService.getTvInputManager().getTvInputInfo(inputId);
                if (tvInfo == null || (info = tvInfo.getHdmiDeviceInfo()) == null) {
                    return;
                }
                HdmiCecLocalDeviceTv.this.addTvInput(inputId, info.getId());
                if (!info.isCecDevice()) {
                    return;
                }
                HdmiCecLocalDeviceTv.this.processDelayedActiveSource(info.getLogicalAddress());
            }

            @Override
            public void onInputRemoved(String inputId) {
                HdmiCecLocalDeviceTv.this.removeTvInput(inputId);
            }
        };
        this.mTvInputs = new HashMap<>();
        this.mPrevPortId = -1;
        this.mAutoDeviceOff = this.mService.readBooleanSetting("hdmi_control_auto_device_off_enabled", true);
        this.mAutoWakeup = this.mService.readBooleanSetting("hdmi_control_auto_wakeup_enabled", true);
        this.mStandbyHandler = new HdmiCecStandbyModeHandler(service, this);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void onAddressAllocated(int logicalAddress, int reason) {
        boolean z = false;
        assertRunOnServiceThread();
        List<HdmiPortInfo> ports = this.mService.getPortInfo();
        for (HdmiPortInfo port : ports) {
            this.mArcFeatureEnabled.put(port.getId(), port.isArcSupported());
        }
        this.mService.registerTvInputCallback(this.mTvInputCallback);
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(this.mAddress, this.mService.getPhysicalAddress(), this.mDeviceType));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildDeviceVendorIdCommand(this.mAddress, this.mService.getVendorId()));
        this.mCecSwitches.add(Integer.valueOf(this.mService.getPhysicalAddress()));
        this.mTvInputs.clear();
        this.mSkipRoutingControl = reason == 3;
        if (reason != 0 && reason != 1) {
            z = true;
        }
        launchRoutingControl(z);
        this.mLocalDeviceAddresses = initLocalDeviceAddresses();
        resetSelectRequestBuffer();
        launchDeviceDiscovery();
    }

    @HdmiAnnotations.ServiceThreadOnly
    private List<Integer> initLocalDeviceAddresses() {
        assertRunOnServiceThread();
        List<Integer> addresses = new ArrayList<>();
        for (HdmiCecLocalDevice device : this.mService.getAllLocalDevices()) {
            addresses.add(Integer.valueOf(device.getDeviceInfo().getLogicalAddress()));
        }
        return Collections.unmodifiableList(addresses);
    }

    @HdmiAnnotations.ServiceThreadOnly
    public void setSelectRequestBuffer(SelectRequestBuffer requestBuffer) {
        assertRunOnServiceThread();
        this.mSelectRequestBuffer = requestBuffer;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void resetSelectRequestBuffer() {
        assertRunOnServiceThread();
        setSelectRequestBuffer(SelectRequestBuffer.EMPTY_BUFFER);
    }

    @Override
    protected int getPreferredAddress() {
        return 0;
    }

    @Override
    protected void setPreferredAddress(int addr) {
        Slog.w(TAG, "Preferred addres will not be stored for TV");
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    boolean dispatchMessage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mService.isPowerStandby() && this.mStandbyHandler.handleCommand(message)) {
            return true;
        }
        return super.onMessage(message);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void deviceSelect(int id, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        HdmiDeviceInfo targetDevice = this.mDeviceInfos.get(id);
        if (targetDevice == null) {
            invokeCallback(callback, 3);
            return;
        }
        int targetAddress = targetDevice.getLogicalAddress();
        HdmiCecLocalDevice.ActiveSource active = getActiveSource();
        if (targetDevice.getDevicePowerStatus() == 0 && active.isValid() && targetAddress == active.logicalAddress) {
            invokeCallback(callback, 0);
            return;
        }
        if (targetAddress == 0) {
            handleSelectInternalSource();
            setActiveSource(targetAddress, this.mService.getPhysicalAddress());
            setActivePath(this.mService.getPhysicalAddress());
            invokeCallback(callback, 0);
            return;
        }
        if (!this.mService.isControlEnabled()) {
            setActiveSource(targetDevice);
            invokeCallback(callback, 6);
        } else {
            removeAction(DeviceSelectAction.class);
            addAndStartAction(new DeviceSelectAction(this, targetDevice, callback));
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleSelectInternalSource() {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled() || this.mActiveSource.logicalAddress == this.mAddress) {
            return;
        }
        updateActiveSource(this.mAddress, this.mService.getPhysicalAddress());
        if (this.mSkipRoutingControl) {
            this.mSkipRoutingControl = false;
        } else {
            HdmiCecMessage activeSource = HdmiCecMessageBuilder.buildActiveSource(this.mAddress, this.mService.getPhysicalAddress());
            this.mService.sendCecCommand(activeSource);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void updateActiveSource(int logicalAddress, int physicalAddress) {
        assertRunOnServiceThread();
        updateActiveSource(HdmiCecLocalDevice.ActiveSource.of(logicalAddress, physicalAddress));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void updateActiveSource(HdmiCecLocalDevice.ActiveSource newActive) {
        assertRunOnServiceThread();
        if (this.mActiveSource.equals(newActive)) {
            return;
        }
        setActiveSource(newActive);
        int logicalAddress = newActive.logicalAddress;
        if (getCecDeviceInfo(logicalAddress) == null || logicalAddress == this.mAddress || this.mService.pathToPortId(newActive.physicalAddress) != getActivePortId()) {
            return;
        }
        setPrevPortId(getActivePortId());
    }

    int getPortId(int physicalAddress) {
        return this.mService.pathToPortId(physicalAddress);
    }

    int getPrevPortId() {
        int i;
        synchronized (this.mLock) {
            i = this.mPrevPortId;
        }
        return i;
    }

    void setPrevPortId(int portId) {
        synchronized (this.mLock) {
            this.mPrevPortId = portId;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void updateActiveInput(int path, boolean notifyInputChange) {
        assertRunOnServiceThread();
        setActivePath(path);
        if (!notifyInputChange) {
            return;
        }
        HdmiCecLocalDevice.ActiveSource activeSource = getActiveSource();
        HdmiDeviceInfo info = getCecDeviceInfo(activeSource.logicalAddress);
        if (info == null && (info = this.mService.getDeviceInfoByPort(getActivePortId())) == null) {
            info = new HdmiDeviceInfo(path, getActivePortId());
        }
        this.mService.invokeInputChangeListener(info);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void doManualPortSwitching(int portId, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (!this.mService.isValidPortId(portId)) {
            invokeCallback(callback, 6);
            return;
        }
        if (portId == getActivePortId()) {
            invokeCallback(callback, 0);
            return;
        }
        this.mActiveSource.invalidate();
        if (!this.mService.isControlEnabled()) {
            setActivePortId(portId);
            invokeCallback(callback, 6);
            return;
        }
        int oldPath = getActivePortId() != -1 ? this.mService.portIdToPath(getActivePortId()) : getDeviceInfo().getPhysicalAddress();
        setActivePath(oldPath);
        if (this.mSkipRoutingControl) {
            this.mSkipRoutingControl = false;
        } else {
            int newPath = this.mService.portIdToPath(portId);
            startRoutingControl(oldPath, newPath, true, callback);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startRoutingControl(int oldPath, int newPath, boolean queryDevicePowerStatus, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (oldPath == newPath) {
            return;
        }
        HdmiCecMessage routingChange = HdmiCecMessageBuilder.buildRoutingChange(this.mAddress, oldPath, newPath);
        this.mService.sendCecCommand(routingChange);
        removeAction(RoutingControlAction.class);
        addAndStartAction(new RoutingControlAction(this, newPath, queryDevicePowerStatus, callback));
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getPowerStatus() {
        assertRunOnServiceThread();
        return this.mService.getPowerStatus();
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void sendKeyEvent(int keyCode, boolean isPressed) {
        assertRunOnServiceThread();
        if (!HdmiCecKeycode.isSupportedKeycode(keyCode)) {
            Slog.w(TAG, "Unsupported key: " + keyCode);
            return;
        }
        List<SendKeyAction> action = getActions(SendKeyAction.class);
        int logicalAddress = findKeyReceiverAddress();
        if (logicalAddress == this.mAddress) {
            Slog.w(TAG, "Discard key event to itself :" + keyCode + " pressed:" + isPressed);
            return;
        }
        if (!action.isEmpty()) {
            action.get(0).processKeyEvent(keyCode, isPressed);
        } else if (isPressed && logicalAddress != -1) {
            addAndStartAction(new SendKeyAction(this, logicalAddress, keyCode));
        } else {
            Slog.w(TAG, "Discard key event: " + keyCode + " pressed:" + isPressed);
        }
    }

    private int findKeyReceiverAddress() {
        if (getActiveSource().isValid()) {
            return getActiveSource().logicalAddress;
        }
        HdmiDeviceInfo info = getDeviceInfoByPath(getActivePath());
        if (info != null) {
            return info.getLogicalAddress();
        }
        return -1;
    }

    private static void invokeCallback(IHdmiControlCallback callback, int result) {
        if (callback == null) {
            return;
        }
        try {
            callback.onComplete(result);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int logicalAddress = message.getSource();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        HdmiDeviceInfo info = getCecDeviceInfo(logicalAddress);
        if (info == null) {
            if (!handleNewDeviceAtTheTailOfActivePath(physicalAddress)) {
                HdmiLogger.debug("Device info %X not found; buffering the command", Integer.valueOf(logicalAddress));
                this.mDelayedMessageBuffer.add(message);
            }
        } else if (isInputReady(info.getId()) || info.getDeviceType() == 5) {
            updateDevicePowerStatus(logicalAddress, 0);
            HdmiCecLocalDevice.ActiveSource activeSource = HdmiCecLocalDevice.ActiveSource.of(logicalAddress, physicalAddress);
            ActiveSourceHandler.create(this, null).process(activeSource, info.getDeviceType());
        } else {
            HdmiLogger.debug("Input not ready for device: %X; buffering the command", Integer.valueOf(info.getId()));
            this.mDelayedMessageBuffer.add(message);
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleInactiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (getActiveSource().logicalAddress != message.getSource() || isProhibitMode()) {
            return true;
        }
        int portId = getPrevPortId();
        if (portId != -1) {
            HdmiDeviceInfo inactiveSource = getCecDeviceInfo(message.getSource());
            if (inactiveSource == null || this.mService.pathToPortId(inactiveSource.getPhysicalAddress()) == portId) {
                return true;
            }
            doManualPortSwitching(portId, null);
            setPrevPortId(-1);
        } else {
            this.mActiveSource.invalidate();
            setActivePath(65535);
            this.mService.invokeInputChangeListener(HdmiDeviceInfo.INACTIVE_DEVICE);
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRequestActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mAddress == getActiveSource().logicalAddress) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(this.mAddress, getActivePath()));
            return true;
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGetMenuLanguage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!broadcastMenuLanguage(this.mService.getLanguage())) {
            Slog.w(TAG, "Failed to respond to <Get Menu Language>: " + message.toString());
            return true;
        }
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean broadcastMenuLanguage(String language) {
        assertRunOnServiceThread();
        HdmiCecMessage command = HdmiCecMessageBuilder.buildSetMenuLanguageCommand(this.mAddress, language);
        if (command != null) {
            this.mService.sendCecCommand(command);
            return true;
        }
        return false;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleReportPhysicalAddress(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int path = HdmiUtils.twoBytesToInt(message.getParams());
        int address = message.getSource();
        int type = message.getParams()[2];
        if (updateCecSwitchInfo(address, type, path)) {
            return true;
        }
        if (hasAction(DeviceDiscoveryAction.class)) {
            Slog.i(TAG, "Ignored while Device Discovery Action is in progress: " + message);
            return true;
        }
        if (!isInDeviceList(address, path)) {
            handleNewDeviceAtTheTailOfActivePath(path);
        }
        HdmiDeviceInfo deviceInfo = new HdmiDeviceInfo(address, path, getPortId(path), type, UsbAudioDevice.kAudioDeviceClassMask, HdmiUtils.getDefaultDeviceName(address));
        addCecDevice(deviceInfo);
        startNewDeviceAction(HdmiCecLocalDevice.ActiveSource.of(address, path), type);
        return true;
    }

    @Override
    protected boolean handleReportPowerStatus(HdmiCecMessage command) {
        int newStatus = command.getParams()[0] & 255;
        updateDevicePowerStatus(command.getSource(), newStatus);
        return true;
    }

    @Override
    protected boolean handleTimerStatus(HdmiCecMessage message) {
        return true;
    }

    @Override
    protected boolean handleRecordStatus(HdmiCecMessage message) {
        return true;
    }

    boolean updateCecSwitchInfo(int address, int type, int path) {
        if (address == 15 && type == 6) {
            this.mCecSwitches.add(Integer.valueOf(path));
            updateSafeDeviceInfoList();
            return true;
        }
        if (type == 5) {
            this.mCecSwitches.add(Integer.valueOf(path));
            return false;
        }
        return false;
    }

    void startNewDeviceAction(HdmiCecLocalDevice.ActiveSource activeSource, int deviceType) {
        for (NewDeviceAction action : getActions(NewDeviceAction.class)) {
            if (action.isActionOf(activeSource)) {
                return;
            }
        }
        addAndStartAction(new NewDeviceAction(this, activeSource.logicalAddress, activeSource.physicalAddress, deviceType));
    }

    private boolean handleNewDeviceAtTheTailOfActivePath(int path) {
        if (!isTailOfActivePath(path, getActivePath())) {
            return false;
        }
        int newPath = this.mService.portIdToPath(getActivePortId());
        setActivePath(newPath);
        startRoutingControl(getActivePath(), newPath, false, null);
        return true;
    }

    static boolean isTailOfActivePath(int path, int activePath) {
        if (activePath == 0) {
            return false;
        }
        for (int i = 12; i >= 0; i -= 4) {
            int curActivePath = (activePath >> i) & 15;
            if (curActivePath == 0) {
                return true;
            }
            int curPath = (path >> i) & 15;
            if (curPath != curActivePath) {
                return false;
            }
        }
        return false;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRoutingChange(HdmiCecMessage message) {
        assertRunOnServiceThread();
        byte[] params = message.getParams();
        int currentPath = HdmiUtils.twoBytesToInt(params);
        if (HdmiUtils.isAffectingActiveRoutingPath(getActivePath(), currentPath)) {
            this.mActiveSource.invalidate();
            removeAction(RoutingControlAction.class);
            int newPath = HdmiUtils.twoBytesToInt(params, 2);
            addAndStartAction(new RoutingControlAction(this, newPath, true, null));
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleReportAudioStatus(HdmiCecMessage message) {
        assertRunOnServiceThread();
        byte[] params = message.getParams();
        int mute = params[0] & 128;
        int volume = params[0] & 127;
        setAudioStatus(mute == 128, volume);
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleTextViewOn(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mService.isPowerStandbyOrTransient() && this.mAutoWakeup) {
            this.mService.wakeUp();
            return true;
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleImageViewOn(HdmiCecMessage message) {
        assertRunOnServiceThread();
        return handleTextViewOn(message);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSetOsdName(HdmiCecMessage message) {
        int source = message.getSource();
        HdmiDeviceInfo deviceInfo = getCecDeviceInfo(source);
        if (deviceInfo == null) {
            Slog.e(TAG, "No source device info for <Set Osd Name>." + message);
            return true;
        }
        try {
            String osdName = new String(message.getParams(), "US-ASCII");
            if (deviceInfo.getDisplayName().equals(osdName)) {
                Slog.i(TAG, "Ignore incoming <Set Osd Name> having same osd name:" + message);
                return true;
            }
            addCecDevice(new HdmiDeviceInfo(deviceInfo.getLogicalAddress(), deviceInfo.getPhysicalAddress(), deviceInfo.getPortId(), deviceInfo.getDeviceType(), deviceInfo.getVendorId(), osdName));
            return true;
        } catch (UnsupportedEncodingException e) {
            Slog.e(TAG, "Invalid <Set Osd Name> request:" + message, e);
            return true;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void launchDeviceDiscovery() {
        assertRunOnServiceThread();
        clearDeviceInfoList();
        DeviceDiscoveryAction action = new DeviceDiscoveryAction(this, new DeviceDiscoveryAction.DeviceDiscoveryCallback() {
            @Override
            public void onDeviceDiscoveryDone(List<HdmiDeviceInfo> deviceInfos) {
                for (HdmiDeviceInfo info : deviceInfos) {
                    HdmiCecLocalDeviceTv.this.addCecDevice(info);
                }
                for (HdmiCecLocalDevice device : HdmiCecLocalDeviceTv.this.mService.getAllLocalDevices()) {
                    HdmiCecLocalDeviceTv.this.addCecDevice(device.getDeviceInfo());
                }
                HdmiCecLocalDeviceTv.this.mSelectRequestBuffer.process();
                HdmiCecLocalDeviceTv.this.resetSelectRequestBuffer();
                HdmiCecLocalDeviceTv.this.addAndStartAction(new HotplugDetectionAction(HdmiCecLocalDeviceTv.this));
                HdmiCecLocalDeviceTv.this.addAndStartAction(new PowerStatusMonitorAction(HdmiCecLocalDeviceTv.this));
                HdmiDeviceInfo avr = HdmiCecLocalDeviceTv.this.getAvrDeviceInfo();
                if (avr != null) {
                    HdmiCecLocalDeviceTv.this.onNewAvrAdded(avr);
                } else {
                    HdmiCecLocalDeviceTv.this.setSystemAudioMode(false, true);
                }
            }
        });
        addAndStartAction(action);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void onNewAvrAdded(HdmiDeviceInfo avr) {
        assertRunOnServiceThread();
        addAndStartAction(new SystemAudioAutoInitiationAction(this, avr.getLogicalAddress()));
        if (!isArcFeatureEnabled(avr.getPortId()) || hasAction(SetArcTransmissionStateAction.class)) {
            return;
        }
        startArcAction(true);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void clearDeviceInfoList() {
        assertRunOnServiceThread();
        for (HdmiDeviceInfo info : this.mSafeExternalInputs) {
            invokeDeviceEventListener(info, 2);
        }
        this.mDeviceInfos.clear();
        updateSafeDeviceInfoList();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeSystemAudioMode(boolean enabled, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled() || hasAction(DeviceDiscoveryAction.class)) {
            setSystemAudioMode(false, true);
            invokeCallback(callback, 6);
            return;
        }
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr == null) {
            setSystemAudioMode(false, true);
            invokeCallback(callback, 3);
        } else {
            addAndStartAction(new SystemAudioActionFromTv(this, avr.getLogicalAddress(), enabled, callback));
        }
    }

    void setSystemAudioMode(boolean on, boolean updateSetting) {
        HdmiLogger.debug("System Audio Mode change[old:%b new:%b]", Boolean.valueOf(this.mSystemAudioActivated), Boolean.valueOf(on));
        if (updateSetting) {
            this.mService.writeBooleanSetting("hdmi_system_audio_enabled", on);
        }
        updateAudioManagerForSystemAudio(on);
        synchronized (this.mLock) {
            if (this.mSystemAudioActivated != on) {
                this.mSystemAudioActivated = on;
                this.mService.announceSystemAudioModeChange(on);
            }
        }
    }

    private void updateAudioManagerForSystemAudio(boolean on) {
        int device = this.mService.getAudioManager().setHdmiSystemAudioSupported(on);
        HdmiLogger.debug("[A]UpdateSystemAudio mode[on=%b] output=[%X]", Boolean.valueOf(on), Integer.valueOf(device));
    }

    boolean isSystemAudioActivated() {
        boolean z;
        if (!hasSystemAudioDevice()) {
            return false;
        }
        synchronized (this.mLock) {
            z = this.mSystemAudioActivated;
        }
        return z;
    }

    boolean getSystemAudioModeSetting() {
        return this.mService.readBooleanSetting("hdmi_system_audio_enabled", false);
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean setArcStatus(boolean enabled) {
        assertRunOnServiceThread();
        HdmiLogger.debug("Set Arc Status[old:%b new:%b]", Boolean.valueOf(this.mArcEstablished), Boolean.valueOf(enabled));
        boolean oldStatus = this.mArcEstablished;
        setAudioReturnChannel(enabled);
        notifyArcStatusToAudioService(enabled);
        this.mArcEstablished = enabled;
        return oldStatus;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setAudioReturnChannel(boolean enabled) {
        assertRunOnServiceThread();
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr == null) {
            return;
        }
        this.mService.setAudioReturnChannel(avr.getPortId(), enabled);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void updateArcFeatureStatus(int portId, boolean isConnected) {
        assertRunOnServiceThread();
        HdmiPortInfo portInfo = this.mService.getPortInfo(portId);
        if (!portInfo.isArcSupported()) {
            return;
        }
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr == null) {
            if (isConnected) {
                this.mArcFeatureEnabled.put(portId, isConnected);
            }
        } else {
            if (avr.getPortId() != portId) {
                return;
            }
            changeArcFeatureEnabled(portId, isConnected);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isConnected(int portId) {
        assertRunOnServiceThread();
        return this.mService.isConnected(portId);
    }

    private void notifyArcStatusToAudioService(boolean enabled) {
        this.mService.getAudioManager().setWiredDeviceConnectionState(PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED, enabled ? 1 : 0, "", "");
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isArcEstablished() {
        assertRunOnServiceThread();
        if (this.mArcEstablished) {
            for (int i = 0; i < this.mArcFeatureEnabled.size(); i++) {
                if (this.mArcFeatureEnabled.valueAt(i)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeArcFeatureEnabled(int portId, boolean enabled) {
        assertRunOnServiceThread();
        if (this.mArcFeatureEnabled.get(portId) == enabled) {
            return;
        }
        this.mArcFeatureEnabled.put(portId, enabled);
        if (enabled) {
            if (this.mArcEstablished) {
                return;
            }
            startArcAction(true);
        } else {
            if (!this.mArcEstablished) {
                return;
            }
            startArcAction(false);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isArcFeatureEnabled(int portId) {
        assertRunOnServiceThread();
        return this.mArcFeatureEnabled.get(portId);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startArcAction(boolean enabled) {
        assertRunOnServiceThread();
        HdmiDeviceInfo info = getAvrDeviceInfo();
        if (info == null) {
            Slog.w(TAG, "Failed to start arc action; No AVR device.");
            return;
        }
        if (!canStartArcUpdateAction(info.getLogicalAddress(), enabled)) {
            Slog.w(TAG, "Failed to start arc action; ARC configuration check failed.");
            if (enabled && !isConnectedToArcPort(info.getPhysicalAddress())) {
                displayOsd(1);
                return;
            }
            return;
        }
        if (enabled) {
            removeAction(RequestArcTerminationAction.class);
            if (hasAction(RequestArcInitiationAction.class)) {
                return;
            }
            addAndStartAction(new RequestArcInitiationAction(this, info.getLogicalAddress()));
            return;
        }
        removeAction(RequestArcInitiationAction.class);
        if (hasAction(RequestArcTerminationAction.class)) {
            return;
        }
        addAndStartAction(new RequestArcTerminationAction(this, info.getLogicalAddress()));
    }

    private boolean isDirectConnectAddress(int physicalAddress) {
        return (61440 & physicalAddress) == physicalAddress;
    }

    void setAudioStatus(boolean mute, int volume) {
        synchronized (this.mLock) {
            this.mSystemAudioMute = mute;
            this.mSystemAudioVolume = volume;
            int maxVolume = this.mService.getAudioManager().getStreamMaxVolume(3);
            this.mService.setAudioStatus(mute, VolumeControlAction.scaleToCustomVolume(volume, maxVolume));
            if (mute) {
                volume = 101;
            }
            displayOsd(2, volume);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeVolume(int curVolume, int delta, int maxVolume) {
        assertRunOnServiceThread();
        if (delta == 0 || !isSystemAudioActivated()) {
            return;
        }
        int targetVolume = curVolume + delta;
        int cecVolume = VolumeControlAction.scaleToCecVolume(targetVolume, maxVolume);
        synchronized (this.mLock) {
            if (cecVolume == this.mSystemAudioVolume) {
                this.mService.setAudioStatus(false, VolumeControlAction.scaleToCustomVolume(this.mSystemAudioVolume, maxVolume));
                return;
            }
            List<VolumeControlAction> actions = getActions(VolumeControlAction.class);
            if (actions.isEmpty()) {
                addAndStartAction(new VolumeControlAction(this, getAvrDeviceInfo().getLogicalAddress(), delta > 0));
            } else {
                actions.get(0).handleVolumeChange(delta > 0);
            }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeMute(boolean mute) {
        assertRunOnServiceThread();
        HdmiLogger.debug("[A]:Change mute:%b", Boolean.valueOf(mute));
        synchronized (this.mLock) {
            if (this.mSystemAudioMute == mute) {
                HdmiLogger.debug("No need to change mute.", new Object[0]);
            } else if (!isSystemAudioActivated()) {
                HdmiLogger.debug("[A]:System audio is not activated.", new Object[0]);
            } else {
                removeAction(VolumeControlAction.class);
                sendUserControlPressedAndReleased(getAvrDeviceInfo().getLogicalAddress(), HdmiCecKeycode.getMuteKey(mute));
            }
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleInitiateArc(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!canStartArcUpdateAction(message.getSource(), true)) {
            if (getAvrDeviceInfo() == null) {
                this.mDelayedMessageBuffer.add(message);
                return true;
            }
            this.mService.maySendFeatureAbortCommand(message, 4);
            if (!isConnectedToArcPort(message.getSource())) {
                displayOsd(1);
            }
            return true;
        }
        removeAction(RequestArcInitiationAction.class);
        SetArcTransmissionStateAction action = new SetArcTransmissionStateAction(this, message.getSource(), true);
        addAndStartAction(action);
        return true;
    }

    private boolean canStartArcUpdateAction(int avrAddress, boolean shouldCheckArcFeatureEnabled) {
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr != null && avrAddress == avr.getLogicalAddress() && isConnectedToArcPort(avr.getPhysicalAddress()) && isDirectConnectAddress(avr.getPhysicalAddress())) {
            if (shouldCheckArcFeatureEnabled) {
                return isArcFeatureEnabled(avr.getPortId());
            }
            return true;
        }
        return false;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleTerminateArc(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mService.isPowerStandbyOrTransient()) {
            setArcStatus(false);
            return true;
        }
        removeAction(RequestArcTerminationAction.class);
        SetArcTransmissionStateAction action = new SetArcTransmissionStateAction(this, message.getSource(), false);
        addAndStartAction(action);
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSetSystemAudioMode(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!isMessageForSystemAudio(message)) {
            if (getAvrDeviceInfo() == null) {
                this.mDelayedMessageBuffer.add(message);
            } else {
                HdmiLogger.warning("Invalid <Set System Audio Mode> message:" + message, new Object[0]);
                this.mService.maySendFeatureAbortCommand(message, 4);
            }
            return true;
        }
        removeAction(SystemAudioAutoInitiationAction.class);
        SystemAudioActionFromAvr action = new SystemAudioActionFromAvr(this, message.getSource(), HdmiUtils.parseCommandParamSystemAudioStatus(message), null);
        addAndStartAction(action);
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSystemAudioModeStatus(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!isMessageForSystemAudio(message)) {
            HdmiLogger.warning("Invalid <System Audio Mode Status> message:" + message, new Object[0]);
            return true;
        }
        setSystemAudioMode(HdmiUtils.parseCommandParamSystemAudioStatus(message), true);
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRecordTvScreen(HdmiCecMessage message) {
        List<OneTouchRecordAction> actions = getActions(OneTouchRecordAction.class);
        if (!actions.isEmpty()) {
            OneTouchRecordAction action = actions.get(0);
            if (action.getRecorderAddress() != message.getSource()) {
                announceOneTouchRecordResult(message.getSource(), 48);
            }
            return super.handleRecordTvScreen(message);
        }
        int recorderAddress = message.getSource();
        byte[] recordSource = this.mService.invokeRecordRequestListener(recorderAddress);
        int reason = startOneTouchRecord(recorderAddress, recordSource);
        if (reason != -1) {
            this.mService.maySendFeatureAbortCommand(message, reason);
            return true;
        }
        return true;
    }

    @Override
    protected boolean handleTimerClearedStatus(HdmiCecMessage message) {
        byte[] params = message.getParams();
        int timerClearedStatusData = params[0] & 255;
        announceTimerRecordingResult(message.getSource(), timerClearedStatusData);
        return true;
    }

    void announceOneTouchRecordResult(int recorderAddress, int result) {
        this.mService.invokeOneTouchRecordResult(recorderAddress, result);
    }

    void announceTimerRecordingResult(int recorderAddress, int result) {
        this.mService.invokeTimerRecordingResult(recorderAddress, result);
    }

    void announceClearTimerRecordingResult(int recorderAddress, int result) {
        this.mService.invokeClearTimerRecordingResult(recorderAddress, result);
    }

    private boolean isMessageForSystemAudio(HdmiCecMessage message) {
        if (this.mService.isControlEnabled() && message.getSource() == 5) {
            return (message.getDestination() == 0 || message.getDestination() == 15) && getAvrDeviceInfo() != null;
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private HdmiDeviceInfo addDeviceInfo(HdmiDeviceInfo deviceInfo) {
        assertRunOnServiceThread();
        HdmiDeviceInfo oldDeviceInfo = getCecDeviceInfo(deviceInfo.getLogicalAddress());
        if (oldDeviceInfo != null) {
            removeDeviceInfo(deviceInfo.getId());
        }
        this.mDeviceInfos.append(deviceInfo.getId(), deviceInfo);
        updateSafeDeviceInfoList();
        return oldDeviceInfo;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private HdmiDeviceInfo removeDeviceInfo(int id) {
        assertRunOnServiceThread();
        HdmiDeviceInfo deviceInfo = this.mDeviceInfos.get(id);
        if (deviceInfo != null) {
            this.mDeviceInfos.remove(id);
        }
        updateSafeDeviceInfoList();
        return deviceInfo;
    }

    @HdmiAnnotations.ServiceThreadOnly
    List<HdmiDeviceInfo> getDeviceInfoList(boolean includeLocalDevice) {
        assertRunOnServiceThread();
        if (includeLocalDevice) {
            return HdmiUtils.sparseArrayToList(this.mDeviceInfos);
        }
        ArrayList<HdmiDeviceInfo> infoList = new ArrayList<>();
        for (int i = 0; i < this.mDeviceInfos.size(); i++) {
            HdmiDeviceInfo info = this.mDeviceInfos.valueAt(i);
            if (!isLocalDeviceAddress(info.getLogicalAddress())) {
                infoList.add(info);
            }
        }
        return infoList;
    }

    List<HdmiDeviceInfo> getSafeExternalInputsLocked() {
        return this.mSafeExternalInputs;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void updateSafeDeviceInfoList() {
        assertRunOnServiceThread();
        List<HdmiDeviceInfo> copiedDevices = HdmiUtils.sparseArrayToList(this.mDeviceInfos);
        List<HdmiDeviceInfo> externalInputs = getInputDevices();
        synchronized (this.mLock) {
            this.mSafeAllDeviceInfos = copiedDevices;
            this.mSafeExternalInputs = externalInputs;
        }
    }

    private List<HdmiDeviceInfo> getInputDevices() {
        ArrayList<HdmiDeviceInfo> infoList = new ArrayList<>();
        for (int i = 0; i < this.mDeviceInfos.size(); i++) {
            HdmiDeviceInfo info = this.mDeviceInfos.valueAt(i);
            if (!isLocalDeviceAddress(info.getLogicalAddress()) && info.isSourceType() && !hideDevicesBehindLegacySwitch(info)) {
                infoList.add(info);
            }
        }
        return infoList;
    }

    private boolean hideDevicesBehindLegacySwitch(HdmiDeviceInfo info) {
        return !isConnectedToCecSwitch(info.getPhysicalAddress(), this.mCecSwitches);
    }

    private static boolean isConnectedToCecSwitch(int path, Collection<Integer> switches) {
        Iterator switchPath$iterator = switches.iterator();
        while (switchPath$iterator.hasNext()) {
            int switchPath = ((Integer) switchPath$iterator.next()).intValue();
            if (isParentPath(switchPath, path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isParentPath(int parentPath, int childPath) {
        for (int i = 0; i <= 12; i += 4) {
            int nibble = (childPath >> i) & 15;
            if (nibble != 0) {
                int parentNibble = (parentPath >> i) & 15;
                return parentNibble == 0 && (childPath >> (i + 4)) == (parentPath >> (i + 4));
            }
        }
        return false;
    }

    private void invokeDeviceEventListener(HdmiDeviceInfo info, int status) {
        if (hideDevicesBehindLegacySwitch(info)) {
            return;
        }
        this.mService.invokeDeviceEventListeners(info, status);
    }

    private boolean isLocalDeviceAddress(int address) {
        return this.mLocalDeviceAddresses.contains(Integer.valueOf(address));
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiDeviceInfo getAvrDeviceInfo() {
        assertRunOnServiceThread();
        return getCecDeviceInfo(5);
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiDeviceInfo getCecDeviceInfo(int logicalAddress) {
        assertRunOnServiceThread();
        return this.mDeviceInfos.get(HdmiDeviceInfo.idForCecDevice(logicalAddress));
    }

    boolean hasSystemAudioDevice() {
        return getSafeAvrDeviceInfo() != null;
    }

    HdmiDeviceInfo getSafeAvrDeviceInfo() {
        return getSafeCecDeviceInfo(5);
    }

    HdmiDeviceInfo getSafeCecDeviceInfo(int logicalAddress) {
        synchronized (this.mLock) {
            for (HdmiDeviceInfo info : this.mSafeAllDeviceInfos) {
                if (info.isCecDevice() && info.getLogicalAddress() == logicalAddress) {
                    return info;
                }
            }
            return null;
        }
    }

    List<HdmiDeviceInfo> getSafeCecDevicesLocked() {
        ArrayList<HdmiDeviceInfo> infoList = new ArrayList<>();
        for (HdmiDeviceInfo info : this.mSafeAllDeviceInfos) {
            if (!isLocalDeviceAddress(info.getLogicalAddress())) {
                infoList.add(info);
            }
        }
        return infoList;
    }

    @HdmiAnnotations.ServiceThreadOnly
    final void addCecDevice(HdmiDeviceInfo info) {
        assertRunOnServiceThread();
        HdmiDeviceInfo old = addDeviceInfo(info);
        if (info.getLogicalAddress() == this.mAddress) {
            return;
        }
        if (old == null) {
            invokeDeviceEventListener(info, 1);
        } else {
            if (old.equals(info)) {
                return;
            }
            invokeDeviceEventListener(old, 2);
            invokeDeviceEventListener(info, 1);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    final void removeCecDevice(int address) {
        assertRunOnServiceThread();
        HdmiDeviceInfo info = removeDeviceInfo(HdmiDeviceInfo.idForCecDevice(address));
        this.mCecMessageCache.flushMessagesFrom(address);
        invokeDeviceEventListener(info, 2);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleRemoveActiveRoutingPath(int path) {
        assertRunOnServiceThread();
        if (!isTailOfActivePath(path, getActivePath())) {
            return;
        }
        int newPath = this.mService.portIdToPath(getActivePortId());
        startRoutingControl(getActivePath(), newPath, true, null);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void launchRoutingControl(boolean routingForBootup) {
        assertRunOnServiceThread();
        if (getActivePortId() != -1) {
            if (routingForBootup || isProhibitMode()) {
                return;
            }
            int newPath = this.mService.portIdToPath(getActivePortId());
            setActivePath(newPath);
            startRoutingControl(getActivePath(), newPath, routingForBootup, null);
            return;
        }
        int activePath = this.mService.getPhysicalAddress();
        setActivePath(activePath);
        if (routingForBootup || this.mDelayedMessageBuffer.isBuffered(130)) {
            return;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(this.mAddress, activePath));
    }

    @HdmiAnnotations.ServiceThreadOnly
    final HdmiDeviceInfo getDeviceInfoByPath(int path) {
        assertRunOnServiceThread();
        for (HdmiDeviceInfo info : getDeviceInfoList(false)) {
            if (info.getPhysicalAddress() == path) {
                return info;
            }
        }
        return null;
    }

    HdmiDeviceInfo getSafeDeviceInfoByPath(int path) {
        synchronized (this.mLock) {
            for (HdmiDeviceInfo info : this.mSafeAllDeviceInfos) {
                if (info.getPhysicalAddress() == path) {
                    return info;
                }
            }
            return null;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isInDeviceList(int logicalAddress, int physicalAddress) {
        assertRunOnServiceThread();
        HdmiDeviceInfo device = getCecDeviceInfo(logicalAddress);
        return device != null && device.getPhysicalAddress() == physicalAddress;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    void onHotplug(int portId, boolean connected) {
        assertRunOnServiceThread();
        if (!connected) {
            removeCecSwitches(portId);
        }
        List<HotplugDetectionAction> hotplugActions = getActions(HotplugDetectionAction.class);
        if (!hotplugActions.isEmpty()) {
            hotplugActions.get(0).pollAllDevicesNow();
        }
        updateArcFeatureStatus(portId, connected);
    }

    private void removeCecSwitches(int portId) {
        Iterator<Integer> it = this.mCecSwitches.iterator();
        while (!it.hasNext()) {
            int path = it.next().intValue();
            if (pathToPortId(path) == portId) {
                it.remove();
            }
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    void setAutoDeviceOff(boolean enabled) {
        assertRunOnServiceThread();
        this.mAutoDeviceOff = enabled;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setAutoWakeup(boolean enabled) {
        assertRunOnServiceThread();
        this.mAutoWakeup = enabled;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean getAutoWakeup() {
        assertRunOnServiceThread();
        return this.mAutoWakeup;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void disableDevice(boolean initiatedByCec, HdmiCecLocalDevice.PendingActionClearedCallback callback) {
        assertRunOnServiceThread();
        this.mService.unregisterTvInputCallback(this.mTvInputCallback);
        removeAction(DeviceDiscoveryAction.class);
        removeAction(HotplugDetectionAction.class);
        removeAction(PowerStatusMonitorAction.class);
        removeAction(OneTouchRecordAction.class);
        removeAction(TimerRecordingAction.class);
        disableSystemAudioIfExist();
        disableArcIfExist();
        super.disableDevice(initiatedByCec, callback);
        clearDeviceInfoList();
        getActiveSource().invalidate();
        setActivePath(65535);
        checkIfPendingActionsCleared();
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void disableSystemAudioIfExist() {
        assertRunOnServiceThread();
        if (getAvrDeviceInfo() == null) {
            return;
        }
        removeAction(SystemAudioActionFromAvr.class);
        removeAction(SystemAudioActionFromTv.class);
        removeAction(SystemAudioAutoInitiationAction.class);
        removeAction(SystemAudioStatusAction.class);
        removeAction(VolumeControlAction.class);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void disableArcIfExist() {
        assertRunOnServiceThread();
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr == null) {
            return;
        }
        removeAction(RequestArcInitiationAction.class);
        if (hasAction(RequestArcTerminationAction.class) || !isArcEstablished()) {
            return;
        }
        addAndStartAction(new RequestArcTerminationAction(this, avr.getLogicalAddress()));
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void onStandby(boolean initiatedByCec, int standbyAction) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled() || initiatedByCec || !this.mAutoDeviceOff) {
            return;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(this.mAddress, 15));
    }

    boolean isProhibitMode() {
        return this.mService.isProhibitMode();
    }

    boolean isPowerStandbyOrTransient() {
        return this.mService.isPowerStandbyOrTransient();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void displayOsd(int messageId) {
        assertRunOnServiceThread();
        this.mService.displayOsd(messageId);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void displayOsd(int messageId, int extra) {
        assertRunOnServiceThread();
        this.mService.displayOsd(messageId, extra);
    }

    @HdmiAnnotations.ServiceThreadOnly
    int startOneTouchRecord(int recorderAddress, byte[] recordSource) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceOneTouchRecordResult(recorderAddress, 51);
            return 1;
        }
        if (!checkRecorder(recorderAddress)) {
            Slog.w(TAG, "Invalid recorder address:" + recorderAddress);
            announceOneTouchRecordResult(recorderAddress, 49);
            return 1;
        }
        if (!checkRecordSource(recordSource)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(recordSource));
            announceOneTouchRecordResult(recorderAddress, 50);
            return 2;
        }
        addAndStartAction(new OneTouchRecordAction(this, recorderAddress, recordSource));
        Slog.i(TAG, "Start new [One Touch Record]-Target:" + recorderAddress + ", recordSource:" + Arrays.toString(recordSource));
        return -1;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void stopOneTouchRecord(int recorderAddress) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not stop one touch record. CEC control is disabled.");
            announceOneTouchRecordResult(recorderAddress, 51);
        } else if (!checkRecorder(recorderAddress)) {
            Slog.w(TAG, "Invalid recorder address:" + recorderAddress);
            announceOneTouchRecordResult(recorderAddress, 49);
        } else {
            removeAction(OneTouchRecordAction.class);
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildRecordOff(this.mAddress, recorderAddress));
            Slog.i(TAG, "Stop [One Touch Record]-Target:" + recorderAddress);
        }
    }

    private boolean checkRecorder(int recorderAddress) {
        HdmiDeviceInfo device = getCecDeviceInfo(recorderAddress);
        return device != null && HdmiUtils.getTypeFromAddress(recorderAddress) == 1;
    }

    private boolean checkRecordSource(byte[] recordSource) {
        if (recordSource != null) {
            return HdmiRecordSources.checkRecordSource(recordSource);
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startTimerRecording(int recorderAddress, int sourceType, byte[] recordSource) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceTimerRecordingResult(recorderAddress, 3);
        } else if (!checkRecorder(recorderAddress)) {
            Slog.w(TAG, "Invalid recorder address:" + recorderAddress);
            announceTimerRecordingResult(recorderAddress, 1);
        } else if (!checkTimerRecordingSource(sourceType, recordSource)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(recordSource));
            announceTimerRecordingResult(recorderAddress, 2);
        } else {
            addAndStartAction(new TimerRecordingAction(this, recorderAddress, sourceType, recordSource));
            Slog.i(TAG, "Start [Timer Recording]-Target:" + recorderAddress + ", SourceType:" + sourceType + ", RecordSource:" + Arrays.toString(recordSource));
        }
    }

    private boolean checkTimerRecordingSource(int sourceType, byte[] recordSource) {
        if (recordSource != null) {
            return HdmiTimerRecordSources.checkTimerRecordSource(sourceType, recordSource);
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void clearTimerRecording(int recorderAddress, int sourceType, byte[] recordSource) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceClearTimerRecordingResult(recorderAddress, 162);
        } else if (!checkRecorder(recorderAddress)) {
            Slog.w(TAG, "Invalid recorder address:" + recorderAddress);
            announceClearTimerRecordingResult(recorderAddress, 160);
        } else if (!checkTimerRecordingSource(sourceType, recordSource)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(recordSource));
            announceClearTimerRecordingResult(recorderAddress, 161);
        } else {
            sendClearTimerMessage(recorderAddress, sourceType, recordSource);
        }
    }

    private void sendClearTimerMessage(final int recorderAddress, int sourceType, byte[] recordSource) {
        HdmiCecMessage message;
        switch (sourceType) {
            case 1:
                message = HdmiCecMessageBuilder.buildClearDigitalTimer(this.mAddress, recorderAddress, recordSource);
                break;
            case 2:
                message = HdmiCecMessageBuilder.buildClearAnalogueTimer(this.mAddress, recorderAddress, recordSource);
                break;
            case 3:
                message = HdmiCecMessageBuilder.buildClearExternalTimer(this.mAddress, recorderAddress, recordSource);
                break;
            default:
                Slog.w(TAG, "Invalid source type:" + recorderAddress);
                announceClearTimerRecordingResult(recorderAddress, 161);
                return;
        }
        this.mService.sendCecCommand(message, new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int error) {
                if (error == 0) {
                    return;
                }
                HdmiCecLocalDeviceTv.this.announceClearTimerRecordingResult(recorderAddress, 161);
            }
        });
    }

    void updateDevicePowerStatus(int logicalAddress, int newPowerStatus) {
        HdmiDeviceInfo info = getCecDeviceInfo(logicalAddress);
        if (info == null) {
            Slog.w(TAG, "Can not update power status of non-existing device:" + logicalAddress);
        } else {
            if (info.getDevicePowerStatus() == newPowerStatus) {
                return;
            }
            HdmiDeviceInfo newInfo = HdmiUtils.cloneHdmiDeviceInfo(info, newPowerStatus);
            addDeviceInfo(newInfo);
            invokeDeviceEventListener(newInfo, 3);
        }
    }

    @Override
    protected boolean handleMenuStatus(HdmiCecMessage message) {
        return true;
    }

    @Override
    protected void sendStandby(int deviceId) {
        HdmiDeviceInfo targetDevice = this.mDeviceInfos.get(deviceId);
        if (targetDevice == null) {
            return;
        }
        int targetAddress = targetDevice.getLogicalAddress();
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(this.mAddress, targetAddress));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void processAllDelayedMessages() {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processAllMessages();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void processDelayedMessages(int address) {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processMessagesForDevice(address);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void processDelayedActiveSource(int address) {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processActiveSource(address);
    }

    @Override
    protected void dump(IndentingPrintWriter pw) {
        super.dump(pw);
        pw.println("mArcEstablished: " + this.mArcEstablished);
        pw.println("mArcFeatureEnabled: " + this.mArcFeatureEnabled);
        pw.println("mSystemAudioActivated: " + this.mSystemAudioActivated);
        pw.println("mSystemAudioMute: " + this.mSystemAudioMute);
        pw.println("mAutoDeviceOff: " + this.mAutoDeviceOff);
        pw.println("mAutoWakeup: " + this.mAutoWakeup);
        pw.println("mSkipRoutingControl: " + this.mSkipRoutingControl);
        pw.println("mPrevPortId: " + this.mPrevPortId);
        pw.println("CEC devices:");
        pw.increaseIndent();
        for (HdmiDeviceInfo info : this.mSafeAllDeviceInfos) {
            pw.println(info);
        }
        pw.decreaseIndent();
    }
}
