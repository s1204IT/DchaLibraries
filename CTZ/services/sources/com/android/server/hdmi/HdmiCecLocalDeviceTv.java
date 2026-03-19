package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiPortInfo;
import android.hardware.hdmi.HdmiRecordSources;
import android.hardware.hdmi.HdmiTimerRecordSources;
import android.hardware.hdmi.IHdmiControlCallback;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.util.NetworkConstants;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.hdmi.DeviceDiscoveryAction;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecLocalDevice;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.pm.DumpState;
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
    private boolean mSystemAudioControlFeatureEnabled;

    @GuardedBy("mLock")
    private boolean mSystemAudioMute;

    @GuardedBy("mLock")
    private int mSystemAudioVolume;
    private final TvInputManager.TvInputCallback mTvInputCallback;
    private final HashMap<String, Integer> mTvInputs;

    @HdmiAnnotations.ServiceThreadOnly
    private void addTvInput(String str, int i) {
        assertRunOnServiceThread();
        this.mTvInputs.put(str, Integer.valueOf(i));
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void removeTvInput(String str) {
        assertRunOnServiceThread();
        this.mTvInputs.remove(str);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean isInputReady(int i) {
        assertRunOnServiceThread();
        return this.mTvInputs.containsValue(Integer.valueOf(i));
    }

    HdmiCecLocalDeviceTv(HdmiControlService hdmiControlService) {
        super(hdmiControlService, 0);
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
            public void onInputAdded(String str) {
                HdmiDeviceInfo hdmiDeviceInfo;
                TvInputInfo tvInputInfo = HdmiCecLocalDeviceTv.this.mService.getTvInputManager().getTvInputInfo(str);
                if (tvInputInfo == null || (hdmiDeviceInfo = tvInputInfo.getHdmiDeviceInfo()) == null) {
                    return;
                }
                HdmiCecLocalDeviceTv.this.addTvInput(str, hdmiDeviceInfo.getId());
                if (hdmiDeviceInfo.isCecDevice()) {
                    HdmiCecLocalDeviceTv.this.processDelayedActiveSource(hdmiDeviceInfo.getLogicalAddress());
                }
            }

            @Override
            public void onInputRemoved(String str) {
                HdmiCecLocalDeviceTv.this.removeTvInput(str);
            }
        };
        this.mTvInputs = new HashMap<>();
        this.mPrevPortId = -1;
        this.mAutoDeviceOff = this.mService.readBooleanSetting("hdmi_control_auto_device_off_enabled", true);
        this.mAutoWakeup = this.mService.readBooleanSetting("hdmi_control_auto_wakeup_enabled", true);
        this.mSystemAudioControlFeatureEnabled = this.mService.readBooleanSetting("hdmi_system_audio_control_enabled", true);
        this.mStandbyHandler = new HdmiCecStandbyModeHandler(hdmiControlService, this);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void onAddressAllocated(int i, int i2) {
        assertRunOnServiceThread();
        for (HdmiPortInfo hdmiPortInfo : this.mService.getPortInfo()) {
            this.mArcFeatureEnabled.put(hdmiPortInfo.getId(), hdmiPortInfo.isArcSupported());
        }
        this.mService.registerTvInputCallback(this.mTvInputCallback);
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(this.mAddress, this.mService.getPhysicalAddress(), this.mDeviceType));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildDeviceVendorIdCommand(this.mAddress, this.mService.getVendorId()));
        this.mCecSwitches.add(Integer.valueOf(this.mService.getPhysicalAddress()));
        this.mTvInputs.clear();
        boolean z = false;
        this.mSkipRoutingControl = i2 == 3;
        if (i2 != 0 && i2 != 1) {
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
        ArrayList arrayList = new ArrayList();
        Iterator<HdmiCecLocalDevice> it = this.mService.getAllLocalDevices().iterator();
        while (it.hasNext()) {
            arrayList.add(Integer.valueOf(it.next().getDeviceInfo().getLogicalAddress()));
        }
        return Collections.unmodifiableList(arrayList);
    }

    @HdmiAnnotations.ServiceThreadOnly
    public void setSelectRequestBuffer(SelectRequestBuffer selectRequestBuffer) {
        assertRunOnServiceThread();
        this.mSelectRequestBuffer = selectRequestBuffer;
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
    protected void setPreferredAddress(int i) {
        Slog.w(TAG, "Preferred addres will not be stored for TV");
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    boolean dispatchMessage(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (this.mService.isPowerStandby() && !this.mService.isWakeUpMessageReceived() && this.mStandbyHandler.handleCommand(hdmiCecMessage)) {
            return true;
        }
        return super.onMessage(hdmiCecMessage);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void deviceSelect(int i, IHdmiControlCallback iHdmiControlCallback) {
        assertRunOnServiceThread();
        HdmiDeviceInfo hdmiDeviceInfo = this.mDeviceInfos.get(i);
        if (hdmiDeviceInfo == null) {
            invokeCallback(iHdmiControlCallback, 3);
            return;
        }
        int logicalAddress = hdmiDeviceInfo.getLogicalAddress();
        HdmiCecLocalDevice.ActiveSource activeSource = getActiveSource();
        if (hdmiDeviceInfo.getDevicePowerStatus() == 0 && activeSource.isValid() && logicalAddress == activeSource.logicalAddress) {
            invokeCallback(iHdmiControlCallback, 0);
            return;
        }
        if (logicalAddress == 0) {
            handleSelectInternalSource();
            setActiveSource(logicalAddress, this.mService.getPhysicalAddress());
            setActivePath(this.mService.getPhysicalAddress());
            invokeCallback(iHdmiControlCallback, 0);
            return;
        }
        if (!this.mService.isControlEnabled()) {
            setActiveSource(hdmiDeviceInfo);
            invokeCallback(iHdmiControlCallback, 6);
        } else {
            removeAction(DeviceSelectAction.class);
            addAndStartAction(new DeviceSelectAction(this, hdmiDeviceInfo, iHdmiControlCallback));
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleSelectInternalSource() {
        assertRunOnServiceThread();
        if (this.mService.isControlEnabled() && this.mActiveSource.logicalAddress != this.mAddress) {
            updateActiveSource(this.mAddress, this.mService.getPhysicalAddress());
            if (this.mSkipRoutingControl) {
                this.mSkipRoutingControl = false;
            } else {
                this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(this.mAddress, this.mService.getPhysicalAddress()));
            }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void updateActiveSource(int i, int i2) {
        assertRunOnServiceThread();
        updateActiveSource(HdmiCecLocalDevice.ActiveSource.of(i, i2));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void updateActiveSource(HdmiCecLocalDevice.ActiveSource activeSource) {
        assertRunOnServiceThread();
        if (this.mActiveSource.equals(activeSource)) {
            return;
        }
        setActiveSource(activeSource);
        int i = activeSource.logicalAddress;
        if (getCecDeviceInfo(i) != null && i != this.mAddress && this.mService.pathToPortId(activeSource.physicalAddress) == getActivePortId()) {
            setPrevPortId(getActivePortId());
        }
    }

    int getPortId(int i) {
        return this.mService.pathToPortId(i);
    }

    int getPrevPortId() {
        int i;
        synchronized (this.mLock) {
            i = this.mPrevPortId;
        }
        return i;
    }

    void setPrevPortId(int i) {
        synchronized (this.mLock) {
            this.mPrevPortId = i;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void updateActiveInput(int i, boolean z) {
        assertRunOnServiceThread();
        setActivePath(i);
        if (z) {
            HdmiDeviceInfo cecDeviceInfo = getCecDeviceInfo(getActiveSource().logicalAddress);
            if (cecDeviceInfo == null && (cecDeviceInfo = this.mService.getDeviceInfoByPort(getActivePortId())) == null) {
                cecDeviceInfo = new HdmiDeviceInfo(i, getActivePortId());
            }
            this.mService.invokeInputChangeListener(cecDeviceInfo);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void doManualPortSwitching(int i, IHdmiControlCallback iHdmiControlCallback) {
        assertRunOnServiceThread();
        if (!this.mService.isValidPortId(i)) {
            invokeCallback(iHdmiControlCallback, 6);
            return;
        }
        if (i == getActivePortId()) {
            invokeCallback(iHdmiControlCallback, 0);
            return;
        }
        this.mActiveSource.invalidate();
        if (!this.mService.isControlEnabled()) {
            setActivePortId(i);
            invokeCallback(iHdmiControlCallback, 6);
            return;
        }
        int iPortIdToPath = getActivePortId() != -1 ? this.mService.portIdToPath(getActivePortId()) : getDeviceInfo().getPhysicalAddress();
        setActivePath(iPortIdToPath);
        if (this.mSkipRoutingControl) {
            this.mSkipRoutingControl = false;
        } else {
            startRoutingControl(iPortIdToPath, this.mService.portIdToPath(i), true, iHdmiControlCallback);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startRoutingControl(int i, int i2, boolean z, IHdmiControlCallback iHdmiControlCallback) {
        assertRunOnServiceThread();
        if (i == i2) {
            return;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildRoutingChange(this.mAddress, i, i2));
        removeAction(RoutingControlAction.class);
        addAndStartAction(new RoutingControlAction(this, i2, z, iHdmiControlCallback));
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getPowerStatus() {
        assertRunOnServiceThread();
        return this.mService.getPowerStatus();
    }

    @Override
    protected int findKeyReceiverAddress() {
        if (getActiveSource().isValid()) {
            return getActiveSource().logicalAddress;
        }
        HdmiDeviceInfo deviceInfoByPath = getDeviceInfoByPath(getActivePath());
        if (deviceInfoByPath != null) {
            return deviceInfoByPath.getLogicalAddress();
        }
        return -1;
    }

    private static void invokeCallback(IHdmiControlCallback iHdmiControlCallback, int i) {
        if (iHdmiControlCallback == null) {
            return;
        }
        try {
            iHdmiControlCallback.onComplete(i);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleActiveSource(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        int source = hdmiCecMessage.getSource();
        int iTwoBytesToInt = HdmiUtils.twoBytesToInt(hdmiCecMessage.getParams());
        HdmiDeviceInfo cecDeviceInfo = getCecDeviceInfo(source);
        if (cecDeviceInfo == null) {
            if (!handleNewDeviceAtTheTailOfActivePath(iTwoBytesToInt)) {
                HdmiLogger.debug("Device info %X not found; buffering the command", Integer.valueOf(source));
                this.mDelayedMessageBuffer.add(hdmiCecMessage);
            }
        } else if (isInputReady(cecDeviceInfo.getId()) || cecDeviceInfo.getDeviceType() == 5) {
            updateDevicePowerStatus(source, 0);
            ActiveSourceHandler.create(this, null).process(HdmiCecLocalDevice.ActiveSource.of(source, iTwoBytesToInt), cecDeviceInfo.getDeviceType());
        } else {
            HdmiLogger.debug("Input not ready for device: %X; buffering the command", Integer.valueOf(cecDeviceInfo.getId()));
            this.mDelayedMessageBuffer.add(hdmiCecMessage);
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleInactiveSource(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (getActiveSource().logicalAddress != hdmiCecMessage.getSource() || isProhibitMode()) {
            return true;
        }
        int prevPortId = getPrevPortId();
        if (prevPortId != -1) {
            HdmiDeviceInfo cecDeviceInfo = getCecDeviceInfo(hdmiCecMessage.getSource());
            if (cecDeviceInfo == null || this.mService.pathToPortId(cecDeviceInfo.getPhysicalAddress()) == prevPortId) {
                return true;
            }
            doManualPortSwitching(prevPortId, null);
            setPrevPortId(-1);
        } else {
            this.mActiveSource.invalidate();
            setActivePath(NetworkConstants.ARP_HWTYPE_RESERVED_HI);
            this.mService.invokeInputChangeListener(HdmiDeviceInfo.INACTIVE_DEVICE);
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRequestActiveSource(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (this.mAddress == getActiveSource().logicalAddress) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(this.mAddress, getActivePath()));
            return true;
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGetMenuLanguage(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (!broadcastMenuLanguage(this.mService.getLanguage())) {
            Slog.w(TAG, "Failed to respond to <Get Menu Language>: " + hdmiCecMessage.toString());
            return true;
        }
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean broadcastMenuLanguage(String str) {
        assertRunOnServiceThread();
        HdmiCecMessage hdmiCecMessageBuildSetMenuLanguageCommand = HdmiCecMessageBuilder.buildSetMenuLanguageCommand(this.mAddress, str);
        if (hdmiCecMessageBuildSetMenuLanguageCommand != null) {
            this.mService.sendCecCommand(hdmiCecMessageBuildSetMenuLanguageCommand);
            return true;
        }
        return false;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleReportPhysicalAddress(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        int iTwoBytesToInt = HdmiUtils.twoBytesToInt(hdmiCecMessage.getParams());
        int source = hdmiCecMessage.getSource();
        byte b = hdmiCecMessage.getParams()[2];
        if (updateCecSwitchInfo(source, b, iTwoBytesToInt)) {
            return true;
        }
        if (hasAction(DeviceDiscoveryAction.class)) {
            Slog.i(TAG, "Ignored while Device Discovery Action is in progress: " + hdmiCecMessage);
            return true;
        }
        if (!isInDeviceList(source, iTwoBytesToInt)) {
            handleNewDeviceAtTheTailOfActivePath(iTwoBytesToInt);
        }
        addCecDevice(new HdmiDeviceInfo(source, iTwoBytesToInt, getPortId(iTwoBytesToInt), b, 16777215, HdmiUtils.getDefaultDeviceName(source)));
        startNewDeviceAction(HdmiCecLocalDevice.ActiveSource.of(source, iTwoBytesToInt), b);
        return true;
    }

    @Override
    protected boolean handleReportPowerStatus(HdmiCecMessage hdmiCecMessage) {
        updateDevicePowerStatus(hdmiCecMessage.getSource(), hdmiCecMessage.getParams()[0] & 255);
        return true;
    }

    @Override
    protected boolean handleTimerStatus(HdmiCecMessage hdmiCecMessage) {
        return true;
    }

    @Override
    protected boolean handleRecordStatus(HdmiCecMessage hdmiCecMessage) {
        return true;
    }

    boolean updateCecSwitchInfo(int i, int i2, int i3) {
        if (i == 15 && i2 == 6) {
            this.mCecSwitches.add(Integer.valueOf(i3));
            updateSafeDeviceInfoList();
            return true;
        }
        if (i2 == 5) {
            this.mCecSwitches.add(Integer.valueOf(i3));
            return false;
        }
        return false;
    }

    void startNewDeviceAction(HdmiCecLocalDevice.ActiveSource activeSource, int i) {
        Iterator it = getActions(NewDeviceAction.class).iterator();
        while (it.hasNext()) {
            if (((NewDeviceAction) it.next()).isActionOf(activeSource)) {
                return;
            }
        }
        addAndStartAction(new NewDeviceAction(this, activeSource.logicalAddress, activeSource.physicalAddress, i));
    }

    private boolean handleNewDeviceAtTheTailOfActivePath(int i) {
        if (!isTailOfActivePath(i, getActivePath())) {
            return false;
        }
        int iPortIdToPath = this.mService.portIdToPath(getActivePortId());
        setActivePath(iPortIdToPath);
        startRoutingControl(getActivePath(), iPortIdToPath, false, null);
        return true;
    }

    static boolean isTailOfActivePath(int i, int i2) {
        if (i2 == 0) {
            return false;
        }
        for (int i3 = 12; i3 >= 0; i3 -= 4) {
            int i4 = (i2 >> i3) & 15;
            if (i4 == 0) {
                return true;
            }
            if (((i >> i3) & 15) != i4) {
                return false;
            }
        }
        return false;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRoutingChange(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        byte[] params = hdmiCecMessage.getParams();
        if (HdmiUtils.isAffectingActiveRoutingPath(getActivePath(), HdmiUtils.twoBytesToInt(params))) {
            this.mActiveSource.invalidate();
            removeAction(RoutingControlAction.class);
            addAndStartAction(new RoutingControlAction(this, HdmiUtils.twoBytesToInt(params, 2), true, null));
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleReportAudioStatus(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        setAudioStatus(HdmiUtils.isAudioStatusMute(hdmiCecMessage), HdmiUtils.getAudioStatusVolume(hdmiCecMessage));
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleTextViewOn(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (this.mService.isPowerStandbyOrTransient() && this.mAutoWakeup) {
            this.mService.wakeUp();
            return true;
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleImageViewOn(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        return handleTextViewOn(hdmiCecMessage);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSetOsdName(HdmiCecMessage hdmiCecMessage) {
        HdmiDeviceInfo cecDeviceInfo = getCecDeviceInfo(hdmiCecMessage.getSource());
        if (cecDeviceInfo == null) {
            Slog.e(TAG, "No source device info for <Set Osd Name>." + hdmiCecMessage);
            return true;
        }
        try {
            String str = new String(hdmiCecMessage.getParams(), "US-ASCII");
            if (cecDeviceInfo.getDisplayName().equals(str)) {
                Slog.i(TAG, "Ignore incoming <Set Osd Name> having same osd name:" + hdmiCecMessage);
                return true;
            }
            addCecDevice(new HdmiDeviceInfo(cecDeviceInfo.getLogicalAddress(), cecDeviceInfo.getPhysicalAddress(), cecDeviceInfo.getPortId(), cecDeviceInfo.getDeviceType(), cecDeviceInfo.getVendorId(), str));
            return true;
        } catch (UnsupportedEncodingException e) {
            Slog.e(TAG, "Invalid <Set Osd Name> request:" + hdmiCecMessage, e);
            return true;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void launchDeviceDiscovery() {
        assertRunOnServiceThread();
        clearDeviceInfoList();
        addAndStartAction(new DeviceDiscoveryAction(this, new DeviceDiscoveryAction.DeviceDiscoveryCallback() {
            @Override
            public void onDeviceDiscoveryDone(List<HdmiDeviceInfo> list) {
                Iterator<HdmiDeviceInfo> it = list.iterator();
                while (it.hasNext()) {
                    HdmiCecLocalDeviceTv.this.addCecDevice(it.next());
                }
                Iterator<HdmiCecLocalDevice> it2 = HdmiCecLocalDeviceTv.this.mService.getAllLocalDevices().iterator();
                while (it2.hasNext()) {
                    HdmiCecLocalDeviceTv.this.addCecDevice(it2.next().getDeviceInfo());
                }
                HdmiCecLocalDeviceTv.this.mSelectRequestBuffer.process();
                HdmiCecLocalDeviceTv.this.resetSelectRequestBuffer();
                HdmiCecLocalDeviceTv.this.addAndStartAction(new HotplugDetectionAction(HdmiCecLocalDeviceTv.this));
                HdmiCecLocalDeviceTv.this.addAndStartAction(new PowerStatusMonitorAction(HdmiCecLocalDeviceTv.this));
                HdmiDeviceInfo avrDeviceInfo = HdmiCecLocalDeviceTv.this.getAvrDeviceInfo();
                if (avrDeviceInfo != null) {
                    HdmiCecLocalDeviceTv.this.onNewAvrAdded(avrDeviceInfo);
                } else {
                    HdmiCecLocalDeviceTv.this.setSystemAudioMode(false);
                }
            }
        }));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void onNewAvrAdded(HdmiDeviceInfo hdmiDeviceInfo) {
        assertRunOnServiceThread();
        addAndStartAction(new SystemAudioAutoInitiationAction(this, hdmiDeviceInfo.getLogicalAddress()));
        if (isConnected(hdmiDeviceInfo.getPortId()) && isArcFeatureEnabled(hdmiDeviceInfo.getPortId()) && !hasAction(SetArcTransmissionStateAction.class)) {
            startArcAction(true);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void clearDeviceInfoList() {
        assertRunOnServiceThread();
        Iterator<HdmiDeviceInfo> it = this.mSafeExternalInputs.iterator();
        while (it.hasNext()) {
            invokeDeviceEventListener(it.next(), 2);
        }
        this.mDeviceInfos.clear();
        updateSafeDeviceInfoList();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeSystemAudioMode(boolean z, IHdmiControlCallback iHdmiControlCallback) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled() || hasAction(DeviceDiscoveryAction.class)) {
            setSystemAudioMode(false);
            invokeCallback(iHdmiControlCallback, 6);
            return;
        }
        HdmiDeviceInfo avrDeviceInfo = getAvrDeviceInfo();
        if (avrDeviceInfo == null) {
            setSystemAudioMode(false);
            invokeCallback(iHdmiControlCallback, 3);
        } else {
            addAndStartAction(new SystemAudioActionFromTv(this, avrDeviceInfo.getLogicalAddress(), z, iHdmiControlCallback));
        }
    }

    void setSystemAudioMode(boolean z) {
        if (!isSystemAudioControlFeatureEnabled() && z) {
            HdmiLogger.debug("Cannot turn on system audio mode because the System Audio Control feature is disabled.", new Object[0]);
            return;
        }
        HdmiLogger.debug("System Audio Mode change[old:%b new:%b]", Boolean.valueOf(this.mSystemAudioActivated), Boolean.valueOf(z));
        updateAudioManagerForSystemAudio(z);
        synchronized (this.mLock) {
            if (this.mSystemAudioActivated != z) {
                this.mSystemAudioActivated = z;
                this.mService.announceSystemAudioModeChange(z);
            }
            startArcAction(z);
        }
    }

    private void updateAudioManagerForSystemAudio(boolean z) {
        HdmiLogger.debug("[A]UpdateSystemAudio mode[on=%b] output=[%X]", Boolean.valueOf(z), Integer.valueOf(this.mService.getAudioManager().setHdmiSystemAudioSupported(z)));
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

    @HdmiAnnotations.ServiceThreadOnly
    void setSystemAudioControlFeatureEnabled(boolean z) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            this.mSystemAudioControlFeatureEnabled = z;
        }
        if (hasSystemAudioDevice()) {
            changeSystemAudioMode(z, null);
        }
    }

    boolean isSystemAudioControlFeatureEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSystemAudioControlFeatureEnabled;
        }
        return z;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean setArcStatus(boolean z) {
        assertRunOnServiceThread();
        HdmiLogger.debug("Set Arc Status[old:%b new:%b]", Boolean.valueOf(this.mArcEstablished), Boolean.valueOf(z));
        boolean z2 = this.mArcEstablished;
        enableAudioReturnChannel(z);
        notifyArcStatusToAudioService(z);
        this.mArcEstablished = z;
        return z2;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void enableAudioReturnChannel(boolean z) {
        assertRunOnServiceThread();
        HdmiDeviceInfo avrDeviceInfo = getAvrDeviceInfo();
        if (avrDeviceInfo != null) {
            this.mService.enableAudioReturnChannel(avrDeviceInfo.getPortId(), z);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isConnected(int i) {
        assertRunOnServiceThread();
        return this.mService.isConnected(i);
    }

    private void notifyArcStatusToAudioService(boolean z) {
        this.mService.getAudioManager().setWiredDeviceConnectionState(DumpState.DUMP_DOMAIN_PREFERRED, z ? 1 : 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
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
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeArcFeatureEnabled(int i, boolean z) {
        assertRunOnServiceThread();
        if (this.mArcFeatureEnabled.get(i) == z) {
            return;
        }
        this.mArcFeatureEnabled.put(i, z);
        HdmiDeviceInfo avrDeviceInfo = getAvrDeviceInfo();
        if (avrDeviceInfo == null || avrDeviceInfo.getPortId() != i) {
            return;
        }
        if (z && !this.mArcEstablished) {
            startArcAction(true);
        } else if (!z && this.mArcEstablished) {
            startArcAction(false);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isArcFeatureEnabled(int i) {
        assertRunOnServiceThread();
        return this.mArcFeatureEnabled.get(i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startArcAction(boolean z) {
        assertRunOnServiceThread();
        HdmiDeviceInfo avrDeviceInfo = getAvrDeviceInfo();
        if (avrDeviceInfo == null) {
            Slog.w(TAG, "Failed to start arc action; No AVR device.");
            return;
        }
        if (!canStartArcUpdateAction(avrDeviceInfo.getLogicalAddress(), z)) {
            Slog.w(TAG, "Failed to start arc action; ARC configuration check failed.");
            if (z && !isConnectedToArcPort(avrDeviceInfo.getPhysicalAddress())) {
                displayOsd(1);
                return;
            }
            return;
        }
        if (z) {
            removeAction(RequestArcTerminationAction.class);
            if (!hasAction(RequestArcInitiationAction.class)) {
                addAndStartAction(new RequestArcInitiationAction(this, avrDeviceInfo.getLogicalAddress()));
                return;
            }
            return;
        }
        removeAction(RequestArcInitiationAction.class);
        if (!hasAction(RequestArcTerminationAction.class)) {
            addAndStartAction(new RequestArcTerminationAction(this, avrDeviceInfo.getLogicalAddress()));
        }
    }

    private boolean isDirectConnectAddress(int i) {
        return (61440 & i) == i;
    }

    void setAudioStatus(boolean z, int i) {
        if (!isSystemAudioActivated()) {
            return;
        }
        synchronized (this.mLock) {
            this.mSystemAudioMute = z;
            this.mSystemAudioVolume = i;
            this.mService.setAudioStatus(z, VolumeControlAction.scaleToCustomVolume(i, this.mService.getAudioManager().getStreamMaxVolume(3)));
            if (z) {
                i = 101;
            }
            displayOsd(2, i);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeVolume(int i, int i2, int i3) {
        assertRunOnServiceThread();
        if (getAvrDeviceInfo() == null || i2 == 0 || !isSystemAudioActivated()) {
            return;
        }
        int iScaleToCecVolume = VolumeControlAction.scaleToCecVolume(i + i2, i3);
        synchronized (this.mLock) {
            if (iScaleToCecVolume == this.mSystemAudioVolume) {
                this.mService.setAudioStatus(false, VolumeControlAction.scaleToCustomVolume(this.mSystemAudioVolume, i3));
                return;
            }
            List actions = getActions(VolumeControlAction.class);
            if (actions.isEmpty()) {
                addAndStartAction(new VolumeControlAction(this, getAvrDeviceInfo().getLogicalAddress(), i2 > 0));
            } else {
                ((VolumeControlAction) actions.get(0)).handleVolumeChange(i2 > 0);
            }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeMute(boolean z) {
        assertRunOnServiceThread();
        if (getAvrDeviceInfo() == null) {
            return;
        }
        HdmiLogger.debug("[A]:Change mute:%b", Boolean.valueOf(z));
        synchronized (this.mLock) {
            if (this.mSystemAudioMute == z) {
                HdmiLogger.debug("No need to change mute.", new Object[0]);
            } else if (!isSystemAudioActivated()) {
                HdmiLogger.debug("[A]:System audio is not activated.", new Object[0]);
            } else {
                removeAction(VolumeControlAction.class);
                sendUserControlPressedAndReleased(getAvrDeviceInfo().getLogicalAddress(), HdmiCecKeycode.getMuteKey(z));
            }
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleInitiateArc(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (!canStartArcUpdateAction(hdmiCecMessage.getSource(), true)) {
            if (getAvrDeviceInfo() == null) {
                this.mDelayedMessageBuffer.add(hdmiCecMessage);
                return true;
            }
            this.mService.maySendFeatureAbortCommand(hdmiCecMessage, 4);
            if (!isConnectedToArcPort(hdmiCecMessage.getSource())) {
                displayOsd(1);
            }
            return true;
        }
        removeAction(RequestArcInitiationAction.class);
        addAndStartAction(new SetArcTransmissionStateAction(this, hdmiCecMessage.getSource(), true));
        return true;
    }

    private boolean canStartArcUpdateAction(int i, boolean z) {
        HdmiDeviceInfo avrDeviceInfo = getAvrDeviceInfo();
        if (avrDeviceInfo == null || i != avrDeviceInfo.getLogicalAddress() || !isConnectedToArcPort(avrDeviceInfo.getPhysicalAddress()) || !isDirectConnectAddress(avrDeviceInfo.getPhysicalAddress())) {
            return false;
        }
        if (!z) {
            return true;
        }
        if (!isConnected(avrDeviceInfo.getPortId()) || !isArcFeatureEnabled(avrDeviceInfo.getPortId())) {
            return false;
        }
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleTerminateArc(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (this.mService.isPowerStandbyOrTransient()) {
            setArcStatus(false);
            return true;
        }
        removeAction(RequestArcTerminationAction.class);
        addAndStartAction(new SetArcTransmissionStateAction(this, hdmiCecMessage.getSource(), false));
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSetSystemAudioMode(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        boolean commandParamSystemAudioStatus = HdmiUtils.parseCommandParamSystemAudioStatus(hdmiCecMessage);
        if (!isMessageForSystemAudio(hdmiCecMessage)) {
            if (getAvrDeviceInfo() == null) {
                this.mDelayedMessageBuffer.add(hdmiCecMessage);
            } else {
                HdmiLogger.warning("Invalid <Set System Audio Mode> message:" + hdmiCecMessage, new Object[0]);
                this.mService.maySendFeatureAbortCommand(hdmiCecMessage, 4);
            }
            return true;
        }
        if (commandParamSystemAudioStatus && !isSystemAudioControlFeatureEnabled()) {
            HdmiLogger.debug("Ignoring <Set System Audio Mode> message because the System Audio Control feature is disabled: %s", hdmiCecMessage);
            this.mService.maySendFeatureAbortCommand(hdmiCecMessage, 4);
            return true;
        }
        removeAction(SystemAudioAutoInitiationAction.class);
        addAndStartAction(new SystemAudioActionFromAvr(this, hdmiCecMessage.getSource(), commandParamSystemAudioStatus, null));
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSystemAudioModeStatus(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (!isMessageForSystemAudio(hdmiCecMessage)) {
            HdmiLogger.warning("Invalid <System Audio Mode Status> message:" + hdmiCecMessage, new Object[0]);
            return true;
        }
        setSystemAudioMode(HdmiUtils.parseCommandParamSystemAudioStatus(hdmiCecMessage));
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRecordTvScreen(HdmiCecMessage hdmiCecMessage) {
        List actions = getActions(OneTouchRecordAction.class);
        if (!actions.isEmpty()) {
            if (((OneTouchRecordAction) actions.get(0)).getRecorderAddress() != hdmiCecMessage.getSource()) {
                announceOneTouchRecordResult(hdmiCecMessage.getSource(), 48);
            }
            return super.handleRecordTvScreen(hdmiCecMessage);
        }
        int source = hdmiCecMessage.getSource();
        int iStartOneTouchRecord = startOneTouchRecord(source, this.mService.invokeRecordRequestListener(source));
        if (iStartOneTouchRecord != -1) {
            this.mService.maySendFeatureAbortCommand(hdmiCecMessage, iStartOneTouchRecord);
            return true;
        }
        return true;
    }

    @Override
    protected boolean handleTimerClearedStatus(HdmiCecMessage hdmiCecMessage) {
        announceTimerRecordingResult(hdmiCecMessage.getSource(), hdmiCecMessage.getParams()[0] & 255);
        return true;
    }

    void announceOneTouchRecordResult(int i, int i2) {
        this.mService.invokeOneTouchRecordResult(i, i2);
    }

    void announceTimerRecordingResult(int i, int i2) {
        this.mService.invokeTimerRecordingResult(i, i2);
    }

    void announceClearTimerRecordingResult(int i, int i2) {
        this.mService.invokeClearTimerRecordingResult(i, i2);
    }

    private boolean isMessageForSystemAudio(HdmiCecMessage hdmiCecMessage) {
        return this.mService.isControlEnabled() && hdmiCecMessage.getSource() == 5 && (hdmiCecMessage.getDestination() == 0 || hdmiCecMessage.getDestination() == 15) && getAvrDeviceInfo() != null;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private HdmiDeviceInfo addDeviceInfo(HdmiDeviceInfo hdmiDeviceInfo) {
        assertRunOnServiceThread();
        HdmiDeviceInfo cecDeviceInfo = getCecDeviceInfo(hdmiDeviceInfo.getLogicalAddress());
        if (cecDeviceInfo != null) {
            removeDeviceInfo(hdmiDeviceInfo.getId());
        }
        this.mDeviceInfos.append(hdmiDeviceInfo.getId(), hdmiDeviceInfo);
        updateSafeDeviceInfoList();
        return cecDeviceInfo;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private HdmiDeviceInfo removeDeviceInfo(int i) {
        assertRunOnServiceThread();
        HdmiDeviceInfo hdmiDeviceInfo = this.mDeviceInfos.get(i);
        if (hdmiDeviceInfo != null) {
            this.mDeviceInfos.remove(i);
        }
        updateSafeDeviceInfoList();
        return hdmiDeviceInfo;
    }

    @HdmiAnnotations.ServiceThreadOnly
    List<HdmiDeviceInfo> getDeviceInfoList(boolean z) {
        assertRunOnServiceThread();
        if (z) {
            return HdmiUtils.sparseArrayToList(this.mDeviceInfos);
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mDeviceInfos.size(); i++) {
            HdmiDeviceInfo hdmiDeviceInfoValueAt = this.mDeviceInfos.valueAt(i);
            if (!isLocalDeviceAddress(hdmiDeviceInfoValueAt.getLogicalAddress())) {
                arrayList.add(hdmiDeviceInfoValueAt);
            }
        }
        return arrayList;
    }

    @GuardedBy("mLock")
    List<HdmiDeviceInfo> getSafeExternalInputsLocked() {
        return this.mSafeExternalInputs;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void updateSafeDeviceInfoList() {
        assertRunOnServiceThread();
        List<HdmiDeviceInfo> listSparseArrayToList = HdmiUtils.sparseArrayToList(this.mDeviceInfos);
        List<HdmiDeviceInfo> inputDevices = getInputDevices();
        synchronized (this.mLock) {
            this.mSafeAllDeviceInfos = listSparseArrayToList;
            this.mSafeExternalInputs = inputDevices;
        }
    }

    private List<HdmiDeviceInfo> getInputDevices() {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mDeviceInfos.size(); i++) {
            HdmiDeviceInfo hdmiDeviceInfoValueAt = this.mDeviceInfos.valueAt(i);
            if (!isLocalDeviceAddress(hdmiDeviceInfoValueAt.getLogicalAddress()) && hdmiDeviceInfoValueAt.isSourceType() && !hideDevicesBehindLegacySwitch(hdmiDeviceInfoValueAt)) {
                arrayList.add(hdmiDeviceInfoValueAt);
            }
        }
        return arrayList;
    }

    private boolean hideDevicesBehindLegacySwitch(HdmiDeviceInfo hdmiDeviceInfo) {
        return !isConnectedToCecSwitch(hdmiDeviceInfo.getPhysicalAddress(), this.mCecSwitches);
    }

    private static boolean isConnectedToCecSwitch(int i, Collection<Integer> collection) {
        Iterator<Integer> it = collection.iterator();
        while (it.hasNext()) {
            if (isParentPath(it.next().intValue(), i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isParentPath(int i, int i2) {
        for (int i3 = 0; i3 <= 12; i3 += 4) {
            if (((i2 >> i3) & 15) != 0) {
                if (((i >> i3) & 15) != 0) {
                    return false;
                }
                int i4 = i3 + 4;
                return (i2 >> i4) == (i >> i4);
            }
        }
        return false;
    }

    private void invokeDeviceEventListener(HdmiDeviceInfo hdmiDeviceInfo, int i) {
        if (!hideDevicesBehindLegacySwitch(hdmiDeviceInfo)) {
            this.mService.invokeDeviceEventListeners(hdmiDeviceInfo, i);
        }
    }

    private boolean isLocalDeviceAddress(int i) {
        return this.mLocalDeviceAddresses.contains(Integer.valueOf(i));
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiDeviceInfo getAvrDeviceInfo() {
        assertRunOnServiceThread();
        return getCecDeviceInfo(5);
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiDeviceInfo getCecDeviceInfo(int i) {
        assertRunOnServiceThread();
        return this.mDeviceInfos.get(HdmiDeviceInfo.idForCecDevice(i));
    }

    boolean hasSystemAudioDevice() {
        return getSafeAvrDeviceInfo() != null;
    }

    HdmiDeviceInfo getSafeAvrDeviceInfo() {
        return getSafeCecDeviceInfo(5);
    }

    HdmiDeviceInfo getSafeCecDeviceInfo(int i) {
        synchronized (this.mLock) {
            for (HdmiDeviceInfo hdmiDeviceInfo : this.mSafeAllDeviceInfos) {
                if (hdmiDeviceInfo.isCecDevice() && hdmiDeviceInfo.getLogicalAddress() == i) {
                    return hdmiDeviceInfo;
                }
            }
            return null;
        }
    }

    @GuardedBy("mLock")
    List<HdmiDeviceInfo> getSafeCecDevicesLocked() {
        ArrayList arrayList = new ArrayList();
        for (HdmiDeviceInfo hdmiDeviceInfo : this.mSafeAllDeviceInfos) {
            if (!isLocalDeviceAddress(hdmiDeviceInfo.getLogicalAddress())) {
                arrayList.add(hdmiDeviceInfo);
            }
        }
        return arrayList;
    }

    @HdmiAnnotations.ServiceThreadOnly
    final void addCecDevice(HdmiDeviceInfo hdmiDeviceInfo) {
        assertRunOnServiceThread();
        HdmiDeviceInfo hdmiDeviceInfoAddDeviceInfo = addDeviceInfo(hdmiDeviceInfo);
        if (hdmiDeviceInfo.getLogicalAddress() == this.mAddress) {
            return;
        }
        if (hdmiDeviceInfoAddDeviceInfo == null) {
            invokeDeviceEventListener(hdmiDeviceInfo, 1);
        } else if (!hdmiDeviceInfoAddDeviceInfo.equals(hdmiDeviceInfo)) {
            invokeDeviceEventListener(hdmiDeviceInfoAddDeviceInfo, 2);
            invokeDeviceEventListener(hdmiDeviceInfo, 1);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    final void removeCecDevice(int i) {
        assertRunOnServiceThread();
        HdmiDeviceInfo hdmiDeviceInfoRemoveDeviceInfo = removeDeviceInfo(HdmiDeviceInfo.idForCecDevice(i));
        this.mCecMessageCache.flushMessagesFrom(i);
        invokeDeviceEventListener(hdmiDeviceInfoRemoveDeviceInfo, 2);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void handleRemoveActiveRoutingPath(int i) {
        assertRunOnServiceThread();
        if (isTailOfActivePath(i, getActivePath())) {
            startRoutingControl(getActivePath(), this.mService.portIdToPath(getActivePortId()), true, null);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void launchRoutingControl(boolean z) {
        assertRunOnServiceThread();
        if (getActivePortId() != -1) {
            if (!z && !isProhibitMode()) {
                int iPortIdToPath = this.mService.portIdToPath(getActivePortId());
                setActivePath(iPortIdToPath);
                startRoutingControl(getActivePath(), iPortIdToPath, z, null);
                return;
            }
            return;
        }
        int physicalAddress = this.mService.getPhysicalAddress();
        setActivePath(physicalAddress);
        if (!z && !this.mDelayedMessageBuffer.isBuffered(130)) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(this.mAddress, physicalAddress));
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    final HdmiDeviceInfo getDeviceInfoByPath(int i) {
        assertRunOnServiceThread();
        for (HdmiDeviceInfo hdmiDeviceInfo : getDeviceInfoList(false)) {
            if (hdmiDeviceInfo.getPhysicalAddress() == i) {
                return hdmiDeviceInfo;
            }
        }
        return null;
    }

    HdmiDeviceInfo getSafeDeviceInfoByPath(int i) {
        synchronized (this.mLock) {
            for (HdmiDeviceInfo hdmiDeviceInfo : this.mSafeAllDeviceInfos) {
                if (hdmiDeviceInfo.getPhysicalAddress() == i) {
                    return hdmiDeviceInfo;
                }
            }
            return null;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isInDeviceList(int i, int i2) {
        assertRunOnServiceThread();
        HdmiDeviceInfo cecDeviceInfo = getCecDeviceInfo(i);
        return cecDeviceInfo != null && cecDeviceInfo.getPhysicalAddress() == i2;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    void onHotplug(int i, boolean z) {
        assertRunOnServiceThread();
        if (!z) {
            removeCecSwitches(i);
        }
        List actions = getActions(HotplugDetectionAction.class);
        if (!actions.isEmpty()) {
            ((HotplugDetectionAction) actions.get(0)).pollAllDevicesNow();
        }
    }

    private void removeCecSwitches(int i) {
        Iterator<Integer> it = this.mCecSwitches.iterator();
        while (!it.hasNext()) {
            if (pathToPortId(it.next().intValue()) == i) {
                it.remove();
            }
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    void setAutoDeviceOff(boolean z) {
        assertRunOnServiceThread();
        this.mAutoDeviceOff = z;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setAutoWakeup(boolean z) {
        assertRunOnServiceThread();
        this.mAutoWakeup = z;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean getAutoWakeup() {
        assertRunOnServiceThread();
        return this.mAutoWakeup;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void disableDevice(boolean z, HdmiCecLocalDevice.PendingActionClearedCallback pendingActionClearedCallback) {
        assertRunOnServiceThread();
        this.mService.unregisterTvInputCallback(this.mTvInputCallback);
        removeAction(DeviceDiscoveryAction.class);
        removeAction(HotplugDetectionAction.class);
        removeAction(PowerStatusMonitorAction.class);
        removeAction(OneTouchRecordAction.class);
        removeAction(TimerRecordingAction.class);
        disableSystemAudioIfExist();
        disableArcIfExist();
        super.disableDevice(z, pendingActionClearedCallback);
        clearDeviceInfoList();
        getActiveSource().invalidate();
        setActivePath(NetworkConstants.ARP_HWTYPE_RESERVED_HI);
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
        HdmiDeviceInfo avrDeviceInfo = getAvrDeviceInfo();
        if (avrDeviceInfo == null) {
            return;
        }
        removeAction(RequestArcInitiationAction.class);
        if (!hasAction(RequestArcTerminationAction.class) && isArcEstablished()) {
            addAndStartAction(new RequestArcTerminationAction(this, avrDeviceInfo.getLogicalAddress()));
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void onStandby(boolean z, int i) {
        assertRunOnServiceThread();
        if (this.mService.isControlEnabled() && !z && this.mAutoDeviceOff) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(this.mAddress, 15));
        }
    }

    boolean isProhibitMode() {
        return this.mService.isProhibitMode();
    }

    boolean isPowerStandbyOrTransient() {
        return this.mService.isPowerStandbyOrTransient();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void displayOsd(int i) {
        assertRunOnServiceThread();
        this.mService.displayOsd(i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void displayOsd(int i, int i2) {
        assertRunOnServiceThread();
        this.mService.displayOsd(i, i2);
    }

    @HdmiAnnotations.ServiceThreadOnly
    int startOneTouchRecord(int i, byte[] bArr) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceOneTouchRecordResult(i, 51);
            return 1;
        }
        if (!checkRecorder(i)) {
            Slog.w(TAG, "Invalid recorder address:" + i);
            announceOneTouchRecordResult(i, 49);
            return 1;
        }
        if (!checkRecordSource(bArr)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(bArr));
            announceOneTouchRecordResult(i, 50);
            return 2;
        }
        addAndStartAction(new OneTouchRecordAction(this, i, bArr));
        Slog.i(TAG, "Start new [One Touch Record]-Target:" + i + ", recordSource:" + Arrays.toString(bArr));
        return -1;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void stopOneTouchRecord(int i) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not stop one touch record. CEC control is disabled.");
            announceOneTouchRecordResult(i, 51);
            return;
        }
        if (!checkRecorder(i)) {
            Slog.w(TAG, "Invalid recorder address:" + i);
            announceOneTouchRecordResult(i, 49);
            return;
        }
        removeAction(OneTouchRecordAction.class);
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildRecordOff(this.mAddress, i));
        Slog.i(TAG, "Stop [One Touch Record]-Target:" + i);
    }

    private boolean checkRecorder(int i) {
        return getCecDeviceInfo(i) != null && HdmiUtils.getTypeFromAddress(i) == 1;
    }

    private boolean checkRecordSource(byte[] bArr) {
        return bArr != null && HdmiRecordSources.checkRecordSource(bArr);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startTimerRecording(int i, int i2, byte[] bArr) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceTimerRecordingResult(i, 3);
            return;
        }
        if (!checkRecorder(i)) {
            Slog.w(TAG, "Invalid recorder address:" + i);
            announceTimerRecordingResult(i, 1);
            return;
        }
        if (!checkTimerRecordingSource(i2, bArr)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(bArr));
            announceTimerRecordingResult(i, 2);
            return;
        }
        addAndStartAction(new TimerRecordingAction(this, i, i2, bArr));
        Slog.i(TAG, "Start [Timer Recording]-Target:" + i + ", SourceType:" + i2 + ", RecordSource:" + Arrays.toString(bArr));
    }

    private boolean checkTimerRecordingSource(int i, byte[] bArr) {
        return bArr != null && HdmiTimerRecordSources.checkTimerRecordSource(i, bArr);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void clearTimerRecording(int i, int i2, byte[] bArr) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceClearTimerRecordingResult(i, 162);
            return;
        }
        if (!checkRecorder(i)) {
            Slog.w(TAG, "Invalid recorder address:" + i);
            announceClearTimerRecordingResult(i, 160);
            return;
        }
        if (!checkTimerRecordingSource(i2, bArr)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(bArr));
            announceClearTimerRecordingResult(i, 161);
            return;
        }
        sendClearTimerMessage(i, i2, bArr);
    }

    private void sendClearTimerMessage(final int i, int i2, byte[] bArr) {
        HdmiCecMessage hdmiCecMessageBuildClearDigitalTimer;
        switch (i2) {
            case 1:
                hdmiCecMessageBuildClearDigitalTimer = HdmiCecMessageBuilder.buildClearDigitalTimer(this.mAddress, i, bArr);
                break;
            case 2:
                hdmiCecMessageBuildClearDigitalTimer = HdmiCecMessageBuilder.buildClearAnalogueTimer(this.mAddress, i, bArr);
                break;
            case 3:
                hdmiCecMessageBuildClearDigitalTimer = HdmiCecMessageBuilder.buildClearExternalTimer(this.mAddress, i, bArr);
                break;
            default:
                Slog.w(TAG, "Invalid source type:" + i);
                announceClearTimerRecordingResult(i, 161);
                return;
        }
        this.mService.sendCecCommand(hdmiCecMessageBuildClearDigitalTimer, new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int i3) {
                if (i3 != 0) {
                    HdmiCecLocalDeviceTv.this.announceClearTimerRecordingResult(i, 161);
                }
            }
        });
    }

    void updateDevicePowerStatus(int i, int i2) {
        HdmiDeviceInfo cecDeviceInfo = getCecDeviceInfo(i);
        if (cecDeviceInfo == null) {
            Slog.w(TAG, "Can not update power status of non-existing device:" + i);
            return;
        }
        if (cecDeviceInfo.getDevicePowerStatus() == i2) {
            return;
        }
        HdmiDeviceInfo hdmiDeviceInfoCloneHdmiDeviceInfo = HdmiUtils.cloneHdmiDeviceInfo(cecDeviceInfo, i2);
        addDeviceInfo(hdmiDeviceInfoCloneHdmiDeviceInfo);
        invokeDeviceEventListener(hdmiDeviceInfoCloneHdmiDeviceInfo, 3);
    }

    @Override
    protected boolean handleMenuStatus(HdmiCecMessage hdmiCecMessage) {
        return true;
    }

    @Override
    protected void sendStandby(int i) {
        HdmiDeviceInfo hdmiDeviceInfo = this.mDeviceInfos.get(i);
        if (hdmiDeviceInfo == null) {
            return;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(this.mAddress, hdmiDeviceInfo.getLogicalAddress()));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void processAllDelayedMessages() {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processAllMessages();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void processDelayedMessages(int i) {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processMessagesForDevice(i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void processDelayedActiveSource(int i) {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processActiveSource(i);
    }

    @Override
    protected void dump(IndentingPrintWriter indentingPrintWriter) {
        super.dump(indentingPrintWriter);
        indentingPrintWriter.println("mArcEstablished: " + this.mArcEstablished);
        indentingPrintWriter.println("mArcFeatureEnabled: " + this.mArcFeatureEnabled);
        indentingPrintWriter.println("mSystemAudioActivated: " + this.mSystemAudioActivated);
        indentingPrintWriter.println("mSystemAudioMute: " + this.mSystemAudioMute);
        indentingPrintWriter.println("mSystemAudioControlFeatureEnabled: " + this.mSystemAudioControlFeatureEnabled);
        indentingPrintWriter.println("mAutoDeviceOff: " + this.mAutoDeviceOff);
        indentingPrintWriter.println("mAutoWakeup: " + this.mAutoWakeup);
        indentingPrintWriter.println("mSkipRoutingControl: " + this.mSkipRoutingControl);
        indentingPrintWriter.println("mPrevPortId: " + this.mPrevPortId);
        indentingPrintWriter.println("CEC devices:");
        indentingPrintWriter.increaseIndent();
        Iterator<HdmiDeviceInfo> it = this.mSafeAllDeviceInfos.iterator();
        while (it.hasNext()) {
            indentingPrintWriter.println(it.next());
        }
        indentingPrintWriter.decreaseIndent();
    }
}
