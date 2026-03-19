package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.input.InputManager;
import android.net.util.NetworkConstants;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.HdmiAnnotations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

abstract class HdmiCecLocalDevice {
    private static final int DEVICE_CLEANUP_TIMEOUT = 5000;
    private static final int FOLLOWER_SAFETY_TIMEOUT = 550;
    private static final int MSG_DISABLE_DEVICE_TIMEOUT = 1;
    private static final int MSG_USER_CONTROL_RELEASE_TIMEOUT = 2;
    private static final String TAG = "HdmiCecLocalDevice";

    @GuardedBy("mLock")
    private int mActiveRoutingPath;
    protected HdmiDeviceInfo mDeviceInfo;
    protected final int mDeviceType;
    protected final Object mLock;
    protected PendingActionClearedCallback mPendingActionClearedCallback;
    protected int mPreferredAddress;
    protected final HdmiControlService mService;
    protected int mLastKeycode = -1;
    protected int mLastKeyRepeatCount = 0;

    @GuardedBy("mLock")
    protected final ActiveSource mActiveSource = new ActiveSource();
    protected final HdmiCecMessageCache mCecMessageCache = new HdmiCecMessageCache();
    private final ArrayList<HdmiCecFeatureAction> mActions = new ArrayList<>();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    HdmiCecLocalDevice.this.handleDisableDeviceTimeout();
                    break;
                case 2:
                    HdmiCecLocalDevice.this.handleUserControlReleased();
                    break;
            }
        }
    };
    protected int mAddress = 15;

    interface PendingActionClearedCallback {
        void onCleared(HdmiCecLocalDevice hdmiCecLocalDevice);
    }

    protected abstract int getPreferredAddress();

    protected abstract void onAddressAllocated(int i, int i2);

    protected abstract void setPreferredAddress(int i);

    static class ActiveSource {
        int logicalAddress;
        int physicalAddress;

        public ActiveSource() {
            invalidate();
        }

        public ActiveSource(int i, int i2) {
            this.logicalAddress = i;
            this.physicalAddress = i2;
        }

        public static ActiveSource of(ActiveSource activeSource) {
            return new ActiveSource(activeSource.logicalAddress, activeSource.physicalAddress);
        }

        public static ActiveSource of(int i, int i2) {
            return new ActiveSource(i, i2);
        }

        public boolean isValid() {
            return HdmiUtils.isValidAddress(this.logicalAddress);
        }

        public void invalidate() {
            this.logicalAddress = -1;
            this.physicalAddress = NetworkConstants.ARP_HWTYPE_RESERVED_HI;
        }

        public boolean equals(int i, int i2) {
            return this.logicalAddress == i && this.physicalAddress == i2;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ActiveSource)) {
                return false;
            }
            ActiveSource activeSource = (ActiveSource) obj;
            return activeSource.logicalAddress == this.logicalAddress && activeSource.physicalAddress == this.physicalAddress;
        }

        public int hashCode() {
            return (this.logicalAddress * 29) + this.physicalAddress;
        }

        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            String str = this.logicalAddress == -1 ? "invalid" : String.format("0x%02x", Integer.valueOf(this.logicalAddress));
            stringBuffer.append("(");
            stringBuffer.append(str);
            String str2 = this.physicalAddress == 65535 ? "invalid" : String.format("0x%04x", Integer.valueOf(this.physicalAddress));
            stringBuffer.append(", ");
            stringBuffer.append(str2);
            stringBuffer.append(")");
            return stringBuffer.toString();
        }
    }

    protected HdmiCecLocalDevice(HdmiControlService hdmiControlService, int i) {
        this.mService = hdmiControlService;
        this.mDeviceType = i;
        this.mLock = hdmiControlService.getServiceLock();
    }

    static HdmiCecLocalDevice create(HdmiControlService hdmiControlService, int i) {
        if (i == 0) {
            return new HdmiCecLocalDeviceTv(hdmiControlService);
        }
        if (i == 4) {
            return new HdmiCecLocalDevicePlayback(hdmiControlService);
        }
        return null;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void init() {
        assertRunOnServiceThread();
        this.mPreferredAddress = getPreferredAddress();
        this.mPendingActionClearedCallback = null;
    }

    protected boolean isInputReady(int i) {
        return true;
    }

    protected boolean canGoToStandby() {
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean dispatchMessage(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        int destination = hdmiCecMessage.getDestination();
        if (destination != this.mAddress && destination != 15) {
            return false;
        }
        this.mCecMessageCache.cacheMessage(hdmiCecMessage);
        return onMessage(hdmiCecMessage);
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected final boolean onMessage(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (dispatchMessageToAction(hdmiCecMessage)) {
            return true;
        }
        int opcode = hdmiCecMessage.getOpcode();
        switch (opcode) {
            case 53:
                return handleTimerStatus(hdmiCecMessage);
            case 54:
                return handleStandby(hdmiCecMessage);
            default:
                switch (opcode) {
                    case 67:
                        return handleTimerClearedStatus(hdmiCecMessage);
                    case 68:
                        return handleUserControlPressed(hdmiCecMessage);
                    case HdmiCecKeycode.CEC_KEYCODE_STOP:
                        return handleUserControlReleased();
                    case HdmiCecKeycode.CEC_KEYCODE_PAUSE:
                        return handleGiveOsdName(hdmiCecMessage);
                    case HdmiCecKeycode.CEC_KEYCODE_RECORD:
                        return handleSetOsdName(hdmiCecMessage);
                    default:
                        switch (opcode) {
                            case 128:
                                return handleRoutingChange(hdmiCecMessage);
                            case NetworkConstants.ICMPV6_ECHO_REPLY_TYPE:
                                return handleRoutingInformation(hdmiCecMessage);
                            case 130:
                                return handleActiveSource(hdmiCecMessage);
                            case 131:
                                return handleGivePhysicalAddress();
                            case 132:
                                return handleReportPhysicalAddress(hdmiCecMessage);
                            case NetworkConstants.ICMPV6_ROUTER_SOLICITATION:
                                return handleRequestActiveSource(hdmiCecMessage);
                            case NetworkConstants.ICMPV6_ROUTER_ADVERTISEMENT:
                                return handleSetStreamPath(hdmiCecMessage);
                            default:
                                switch (opcode) {
                                    case 140:
                                        return handleGiveDeviceVendorId();
                                    case 141:
                                        return handleMenuRequest(hdmiCecMessage);
                                    case 142:
                                        return handleMenuStatus(hdmiCecMessage);
                                    case 143:
                                        return handleGiveDevicePowerStatus(hdmiCecMessage);
                                    case 144:
                                        return handleReportPowerStatus(hdmiCecMessage);
                                    case HdmiCecKeycode.UI_BROADCAST_DIGITAL_COMMNICATIONS_SATELLITE_2:
                                        return handleGetMenuLanguage(hdmiCecMessage);
                                    default:
                                        switch (opcode) {
                                            case 159:
                                                return handleGetCecVersion(hdmiCecMessage);
                                            case 160:
                                                return handleVendorCommandWithId(hdmiCecMessage);
                                            default:
                                                switch (opcode) {
                                                    case 4:
                                                        return handleImageViewOn(hdmiCecMessage);
                                                    case 10:
                                                        return handleRecordStatus(hdmiCecMessage);
                                                    case 13:
                                                        return handleTextViewOn(hdmiCecMessage);
                                                    case 15:
                                                        return handleRecordTvScreen(hdmiCecMessage);
                                                    case HdmiCecKeycode.CEC_KEYCODE_PREVIOUS_CHANNEL:
                                                        return handleSetMenuLanguage(hdmiCecMessage);
                                                    case 114:
                                                        return handleSetSystemAudioMode(hdmiCecMessage);
                                                    case 122:
                                                        return handleReportAudioStatus(hdmiCecMessage);
                                                    case 126:
                                                        return handleSystemAudioModeStatus(hdmiCecMessage);
                                                    case 137:
                                                        return handleVendorCommand(hdmiCecMessage);
                                                    case 157:
                                                        return handleInactiveSource(hdmiCecMessage);
                                                    case 192:
                                                        return handleInitiateArc(hdmiCecMessage);
                                                    case 197:
                                                        return handleTerminateArc(hdmiCecMessage);
                                                    default:
                                                        return false;
                                                }
                                        }
                                }
                        }
                }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private boolean dispatchMessageToAction(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        Iterator it = new ArrayList(this.mActions).iterator();
        boolean z = false;
        while (it.hasNext()) {
            boolean zProcessCommand = ((HdmiCecFeatureAction) it.next()).processCommand(hdmiCecMessage);
            if (z || zProcessCommand) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGivePhysicalAddress() {
        assertRunOnServiceThread();
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(this.mAddress, this.mService.getPhysicalAddress(), this.mDeviceType));
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGiveDeviceVendorId() {
        assertRunOnServiceThread();
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildDeviceVendorIdCommand(this.mAddress, this.mService.getVendorId()));
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGetCecVersion(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildCecVersion(hdmiCecMessage.getDestination(), hdmiCecMessage.getSource(), this.mService.getCecVersion()));
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleActiveSource(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleInactiveSource(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRequestActiveSource(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGetMenuLanguage(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        Slog.w(TAG, "Only TV can handle <Get Menu Language>:" + hdmiCecMessage.toString());
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSetMenuLanguage(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        Slog.w(TAG, "Only Playback device can handle <Set Menu Language>:" + hdmiCecMessage.toString());
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGiveOsdName(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        HdmiCecMessage hdmiCecMessageBuildSetOsdNameCommand = HdmiCecMessageBuilder.buildSetOsdNameCommand(this.mAddress, hdmiCecMessage.getSource(), this.mDeviceInfo.getDisplayName());
        if (hdmiCecMessageBuildSetOsdNameCommand != null) {
            this.mService.sendCecCommand(hdmiCecMessageBuildSetOsdNameCommand);
            return true;
        }
        Slog.w(TAG, "Failed to build <Get Osd Name>:" + this.mDeviceInfo.getDisplayName());
        return true;
    }

    protected boolean handleRoutingChange(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleRoutingInformation(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleReportPhysicalAddress(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleSystemAudioModeStatus(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleSetSystemAudioMode(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleTerminateArc(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleInitiateArc(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleReportAudioStatus(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleStandby(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (this.mService.isControlEnabled() && !this.mService.isProhibitMode() && this.mService.isPowerOnOrTransient()) {
            this.mService.standby();
            return true;
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleUserControlPressed(HdmiCecMessage hdmiCecMessage) {
        int i;
        assertRunOnServiceThread();
        this.mHandler.removeMessages(2);
        if (this.mService.isPowerOnOrTransient() && isPowerOffOrToggleCommand(hdmiCecMessage)) {
            this.mService.standby();
            return true;
        }
        if (this.mService.isPowerStandbyOrTransient() && isPowerOnOrToggleCommand(hdmiCecMessage)) {
            this.mService.wakeUp();
            return true;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        int iCecKeycodeAndParamsToAndroidKey = HdmiCecKeycode.cecKeycodeAndParamsToAndroidKey(hdmiCecMessage.getParams());
        if (this.mLastKeycode != -1) {
            if (iCecKeycodeAndParamsToAndroidKey == this.mLastKeycode) {
                i = this.mLastKeyRepeatCount + 1;
            } else {
                injectKeyEvent(jUptimeMillis, 1, this.mLastKeycode, 0);
                i = 0;
            }
        } else {
            i = 0;
        }
        this.mLastKeycode = iCecKeycodeAndParamsToAndroidKey;
        this.mLastKeyRepeatCount = i;
        if (iCecKeycodeAndParamsToAndroidKey == -1) {
            return false;
        }
        injectKeyEvent(jUptimeMillis, 0, iCecKeycodeAndParamsToAndroidKey, i);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 2), 550L);
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleUserControlReleased() {
        assertRunOnServiceThread();
        this.mHandler.removeMessages(2);
        this.mLastKeyRepeatCount = 0;
        if (this.mLastKeycode != -1) {
            injectKeyEvent(SystemClock.uptimeMillis(), 1, this.mLastKeycode, 0);
            this.mLastKeycode = -1;
            return true;
        }
        return false;
    }

    static void injectKeyEvent(long j, int i, int i2, int i3) {
        KeyEvent keyEventObtain = KeyEvent.obtain(j, j, i, i2, i3, 0, -1, 0, 8, 33554433, null);
        InputManager.getInstance().injectInputEvent(keyEventObtain, 0);
        keyEventObtain.recycle();
    }

    static boolean isPowerOnOrToggleCommand(HdmiCecMessage hdmiCecMessage) {
        byte[] params = hdmiCecMessage.getParams();
        if (hdmiCecMessage.getOpcode() == 68) {
            return params[0] == 64 || params[0] == 109 || params[0] == 107;
        }
        return false;
    }

    static boolean isPowerOffOrToggleCommand(HdmiCecMessage hdmiCecMessage) {
        byte[] params = hdmiCecMessage.getParams();
        if (hdmiCecMessage.getOpcode() == 68) {
            return params[0] == 64 || params[0] == 108 || params[0] == 107;
        }
        return false;
    }

    protected boolean handleTextViewOn(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleImageViewOn(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleSetStreamPath(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleGiveDevicePowerStatus(HdmiCecMessage hdmiCecMessage) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPowerStatus(this.mAddress, hdmiCecMessage.getSource(), this.mService.getPowerStatus()));
        return true;
    }

    protected boolean handleMenuRequest(HdmiCecMessage hdmiCecMessage) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportMenuStatus(this.mAddress, hdmiCecMessage.getSource(), 0));
        return true;
    }

    protected boolean handleMenuStatus(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleVendorCommand(HdmiCecMessage hdmiCecMessage) {
        if (!this.mService.invokeVendorCommandListenersOnReceived(this.mDeviceType, hdmiCecMessage.getSource(), hdmiCecMessage.getDestination(), hdmiCecMessage.getParams(), false)) {
            this.mService.maySendFeatureAbortCommand(hdmiCecMessage, 1);
        }
        return true;
    }

    protected boolean handleVendorCommandWithId(HdmiCecMessage hdmiCecMessage) {
        byte[] params = hdmiCecMessage.getParams();
        if (HdmiUtils.threeBytesToInt(params) == this.mService.getVendorId()) {
            if (!this.mService.invokeVendorCommandListenersOnReceived(this.mDeviceType, hdmiCecMessage.getSource(), hdmiCecMessage.getDestination(), params, true)) {
                this.mService.maySendFeatureAbortCommand(hdmiCecMessage, 1);
            }
        } else if (hdmiCecMessage.getDestination() != 15 && hdmiCecMessage.getSource() != 15) {
            Slog.v(TAG, "Wrong direct vendor command. Replying with <Feature Abort>");
            this.mService.maySendFeatureAbortCommand(hdmiCecMessage, 0);
        } else {
            Slog.v(TAG, "Wrong broadcast vendor command. Ignoring");
        }
        return true;
    }

    protected void sendStandby(int i) {
    }

    protected boolean handleSetOsdName(HdmiCecMessage hdmiCecMessage) {
        return true;
    }

    protected boolean handleRecordTvScreen(HdmiCecMessage hdmiCecMessage) {
        this.mService.maySendFeatureAbortCommand(hdmiCecMessage, 2);
        return true;
    }

    protected boolean handleTimerClearedStatus(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleReportPowerStatus(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleTimerStatus(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    protected boolean handleRecordStatus(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    final void handleAddressAllocated(int i, int i2) {
        assertRunOnServiceThread();
        this.mPreferredAddress = i;
        this.mAddress = i;
        onAddressAllocated(i, i2);
        setPreferredAddress(i);
    }

    int getType() {
        return this.mDeviceType;
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiDeviceInfo getDeviceInfo() {
        assertRunOnServiceThread();
        return this.mDeviceInfo;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setDeviceInfo(HdmiDeviceInfo hdmiDeviceInfo) {
        assertRunOnServiceThread();
        this.mDeviceInfo = hdmiDeviceInfo;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isAddressOf(int i) {
        assertRunOnServiceThread();
        return i == this.mAddress;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void clearAddress() {
        assertRunOnServiceThread();
        this.mAddress = 15;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void addAndStartAction(HdmiCecFeatureAction hdmiCecFeatureAction) {
        assertRunOnServiceThread();
        this.mActions.add(hdmiCecFeatureAction);
        if (this.mService.isPowerStandby() || !this.mService.isAddressAllocated()) {
            Slog.i(TAG, "Not ready to start action. Queued for deferred start:" + hdmiCecFeatureAction);
            return;
        }
        hdmiCecFeatureAction.start();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startQueuedActions() {
        assertRunOnServiceThread();
        for (HdmiCecFeatureAction hdmiCecFeatureAction : new ArrayList(this.mActions)) {
            if (!hdmiCecFeatureAction.started()) {
                Slog.i(TAG, "Starting queued action:" + hdmiCecFeatureAction);
                hdmiCecFeatureAction.start();
            }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    <T extends HdmiCecFeatureAction> boolean hasAction(Class<T> cls) {
        assertRunOnServiceThread();
        Iterator<HdmiCecFeatureAction> it = this.mActions.iterator();
        while (it.hasNext()) {
            if (it.next().getClass().equals(cls)) {
                return true;
            }
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    <T extends HdmiCecFeatureAction> List<T> getActions(Class<T> cls) {
        assertRunOnServiceThread();
        ArrayList arrayList = (List<T>) Collections.emptyList();
        for (HdmiCecFeatureAction hdmiCecFeatureAction : this.mActions) {
            if (hdmiCecFeatureAction.getClass().equals(cls)) {
                if (arrayList.isEmpty()) {
                    arrayList = new ArrayList();
                }
                arrayList.add(hdmiCecFeatureAction);
            }
        }
        return (List<T>) arrayList;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void removeAction(HdmiCecFeatureAction hdmiCecFeatureAction) {
        assertRunOnServiceThread();
        hdmiCecFeatureAction.finish(false);
        this.mActions.remove(hdmiCecFeatureAction);
        checkIfPendingActionsCleared();
    }

    @HdmiAnnotations.ServiceThreadOnly
    <T extends HdmiCecFeatureAction> void removeAction(Class<T> cls) {
        assertRunOnServiceThread();
        removeActionExcept(cls, null);
    }

    @HdmiAnnotations.ServiceThreadOnly
    <T extends HdmiCecFeatureAction> void removeActionExcept(Class<T> cls, HdmiCecFeatureAction hdmiCecFeatureAction) {
        assertRunOnServiceThread();
        Iterator<HdmiCecFeatureAction> it = this.mActions.iterator();
        while (it.hasNext()) {
            HdmiCecFeatureAction next = it.next();
            if (next != hdmiCecFeatureAction && next.getClass().equals(cls)) {
                next.finish(false);
                it.remove();
            }
        }
        checkIfPendingActionsCleared();
    }

    protected void checkIfPendingActionsCleared() {
        if (this.mActions.isEmpty() && this.mPendingActionClearedCallback != null) {
            PendingActionClearedCallback pendingActionClearedCallback = this.mPendingActionClearedCallback;
            this.mPendingActionClearedCallback = null;
            pendingActionClearedCallback.onCleared(this);
        }
    }

    protected void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mService.getServiceLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    void setAutoDeviceOff(boolean z) {
    }

    void onHotplug(int i, boolean z) {
    }

    final HdmiControlService getService() {
        return this.mService;
    }

    @HdmiAnnotations.ServiceThreadOnly
    final boolean isConnectedToArcPort(int i) {
        assertRunOnServiceThread();
        return this.mService.isConnectedToArcPort(i);
    }

    ActiveSource getActiveSource() {
        ActiveSource activeSource;
        synchronized (this.mLock) {
            activeSource = this.mActiveSource;
        }
        return activeSource;
    }

    void setActiveSource(ActiveSource activeSource) {
        setActiveSource(activeSource.logicalAddress, activeSource.physicalAddress);
    }

    void setActiveSource(HdmiDeviceInfo hdmiDeviceInfo) {
        setActiveSource(hdmiDeviceInfo.getLogicalAddress(), hdmiDeviceInfo.getPhysicalAddress());
    }

    void setActiveSource(int i, int i2) {
        synchronized (this.mLock) {
            this.mActiveSource.logicalAddress = i;
            this.mActiveSource.physicalAddress = i2;
        }
        this.mService.setLastInputForMhl(-1);
    }

    int getActivePath() {
        int i;
        synchronized (this.mLock) {
            i = this.mActiveRoutingPath;
        }
        return i;
    }

    void setActivePath(int i) {
        synchronized (this.mLock) {
            this.mActiveRoutingPath = i;
        }
        this.mService.setActivePortId(pathToPortId(i));
    }

    int getActivePortId() {
        int iPathToPortId;
        synchronized (this.mLock) {
            iPathToPortId = this.mService.pathToPortId(this.mActiveRoutingPath);
        }
        return iPathToPortId;
    }

    void setActivePortId(int i) {
        setActivePath(this.mService.portIdToPath(i));
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiCecMessageCache getCecMessageCache() {
        assertRunOnServiceThread();
        return this.mCecMessageCache;
    }

    @HdmiAnnotations.ServiceThreadOnly
    int pathToPortId(int i) {
        assertRunOnServiceThread();
        return this.mService.pathToPortId(i);
    }

    protected void onStandby(boolean z, int i) {
    }

    protected void disableDevice(boolean z, final PendingActionClearedCallback pendingActionClearedCallback) {
        this.mPendingActionClearedCallback = new PendingActionClearedCallback() {
            @Override
            public void onCleared(HdmiCecLocalDevice hdmiCecLocalDevice) {
                HdmiCecLocalDevice.this.mHandler.removeMessages(1);
                pendingActionClearedCallback.onCleared(hdmiCecLocalDevice);
            }
        };
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 1), 5000L);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleDisableDeviceTimeout() {
        assertRunOnServiceThread();
        Iterator<HdmiCecFeatureAction> it = this.mActions.iterator();
        while (it.hasNext()) {
            it.next().finish(false);
            it.remove();
        }
        if (this.mPendingActionClearedCallback != null) {
            this.mPendingActionClearedCallback.onCleared(this);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected void sendKeyEvent(int i, boolean z) {
        assertRunOnServiceThread();
        if (!HdmiCecKeycode.isSupportedKeycode(i)) {
            Slog.w(TAG, "Unsupported key: " + i);
            return;
        }
        List actions = getActions(SendKeyAction.class);
        int iFindKeyReceiverAddress = findKeyReceiverAddress();
        if (iFindKeyReceiverAddress == -1 || iFindKeyReceiverAddress == this.mAddress) {
            Slog.w(TAG, "Discard key event: " + i + ", pressed:" + z + ", receiverAddr=" + iFindKeyReceiverAddress);
            return;
        }
        if (!actions.isEmpty()) {
            ((SendKeyAction) actions.get(0)).processKeyEvent(i, z);
        } else if (z) {
            addAndStartAction(new SendKeyAction(this, iFindKeyReceiverAddress, i));
        }
    }

    protected int findKeyReceiverAddress() {
        Slog.w(TAG, "findKeyReceiverAddress is not implemented");
        return -1;
    }

    void sendUserControlPressedAndReleased(int i, int i2) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildUserControlPressed(this.mAddress, i, i2));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildUserControlReleased(this.mAddress, i));
    }

    protected void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("mDeviceType: " + this.mDeviceType);
        indentingPrintWriter.println("mAddress: " + this.mAddress);
        indentingPrintWriter.println("mPreferredAddress: " + this.mPreferredAddress);
        indentingPrintWriter.println("mDeviceInfo: " + this.mDeviceInfo);
        indentingPrintWriter.println("mActiveSource: " + this.mActiveSource);
        indentingPrintWriter.println(String.format("mActiveRoutingPath: 0x%04x", Integer.valueOf(this.mActiveRoutingPath)));
    }
}
