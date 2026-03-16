package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.input.InputManager;
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
        public void handleMessage(Message msg) {
            switch (msg.what) {
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

        public ActiveSource(int logical, int physical) {
            this.logicalAddress = logical;
            this.physicalAddress = physical;
        }

        public static ActiveSource of(int logical, int physical) {
            return new ActiveSource(logical, physical);
        }

        public boolean isValid() {
            return HdmiUtils.isValidAddress(this.logicalAddress);
        }

        public void invalidate() {
            this.logicalAddress = -1;
            this.physicalAddress = 65535;
        }

        public boolean equals(int logical, int physical) {
            return this.logicalAddress == logical && this.physicalAddress == physical;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ActiveSource)) {
                return false;
            }
            ActiveSource that = (ActiveSource) obj;
            return that.logicalAddress == this.logicalAddress && that.physicalAddress == this.physicalAddress;
        }

        public int hashCode() {
            return (this.logicalAddress * 29) + this.physicalAddress;
        }

        public String toString() {
            StringBuffer s = new StringBuffer();
            String logicalAddressString = this.logicalAddress == -1 ? "invalid" : String.format("0x%02x", Integer.valueOf(this.logicalAddress));
            s.append("logical_address: ").append(logicalAddressString);
            String physicalAddressString = this.physicalAddress == 65535 ? "invalid" : String.format("0x%04x", Integer.valueOf(this.physicalAddress));
            s.append(", physical_address: ").append(physicalAddressString);
            return s.toString();
        }
    }

    protected HdmiCecLocalDevice(HdmiControlService service, int deviceType) {
        this.mService = service;
        this.mDeviceType = deviceType;
        this.mLock = service.getServiceLock();
    }

    static HdmiCecLocalDevice create(HdmiControlService service, int deviceType) {
        switch (deviceType) {
            case 0:
                return new HdmiCecLocalDeviceTv(service);
            case 4:
                return new HdmiCecLocalDevicePlayback(service);
            default:
                return null;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void init() {
        assertRunOnServiceThread();
        this.mPreferredAddress = getPreferredAddress();
    }

    protected boolean isInputReady(int deviceId) {
        return true;
    }

    protected boolean canGoToStandby() {
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean dispatchMessage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int dest = message.getDestination();
        if (dest != this.mAddress && dest != 15) {
            return false;
        }
        this.mCecMessageCache.cacheMessage(message);
        return onMessage(message);
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected final boolean onMessage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (dispatchMessageToAction(message)) {
            return true;
        }
        switch (message.getOpcode()) {
            case 4:
                return handleImageViewOn(message);
            case 10:
                return handleRecordStatus(message);
            case 13:
                return handleTextViewOn(message);
            case 15:
                return handleRecordTvScreen(message);
            case HdmiCecKeycode.CEC_KEYCODE_DISPLAY_INFORMATION:
                return handleTimerStatus(message);
            case HdmiCecKeycode.CEC_KEYCODE_HELP:
                return handleStandby(message);
            case HdmiCecKeycode.CEC_KEYCODE_MUTE:
                return handleTimerClearedStatus(message);
            case HdmiCecKeycode.CEC_KEYCODE_PLAY:
                return handleUserControlPressed(message);
            case HdmiCecKeycode.CEC_KEYCODE_STOP:
                return handleUserControlReleased();
            case HdmiCecKeycode.CEC_KEYCODE_PAUSE:
                return handleGiveOsdName(message);
            case HdmiCecKeycode.CEC_KEYCODE_RECORD:
                return handleSetOsdName(message);
            case 114:
                return handleSetSystemAudioMode(message);
            case 122:
                return handleReportAudioStatus(message);
            case 126:
                return handleSystemAudioModeStatus(message);
            case 128:
                return handleRoutingChange(message);
            case 129:
                return handleRoutingInformation(message);
            case 130:
                return handleActiveSource(message);
            case 131:
                return handleGivePhysicalAddress();
            case 132:
                return handleReportPhysicalAddress(message);
            case 133:
                return handleRequestActiveSource(message);
            case 134:
                return handleSetStreamPath(message);
            case 137:
                return handleVendorCommand(message);
            case 140:
                return handleGiveDeviceVendorId();
            case 141:
                return handleMenuRequest(message);
            case 142:
                return handleMenuStatus(message);
            case 143:
                return handleGiveDevicePowerStatus(message);
            case 144:
                return handleReportPowerStatus(message);
            case HdmiCecKeycode.UI_BROADCAST_DIGITAL_COMMNICATIONS_SATELLITE_2:
                return handleGetMenuLanguage(message);
            case 157:
                return handleInactiveSource(message);
            case 159:
                return handleGetCecVersion(message);
            case 160:
                return handleVendorCommandWithId(message);
            case 192:
                return handleInitiateArc(message);
            case 197:
                return handleTerminateArc(message);
            default:
                return false;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private boolean dispatchMessageToAction(HdmiCecMessage message) {
        assertRunOnServiceThread();
        boolean processed = false;
        for (HdmiCecFeatureAction action : new ArrayList(this.mActions)) {
            boolean result = action.processCommand(message);
            processed = processed || result;
        }
        return processed;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGivePhysicalAddress() {
        assertRunOnServiceThread();
        int physicalAddress = this.mService.getPhysicalAddress();
        HdmiCecMessage cecMessage = HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(this.mAddress, physicalAddress, this.mDeviceType);
        this.mService.sendCecCommand(cecMessage);
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGiveDeviceVendorId() {
        assertRunOnServiceThread();
        int vendorId = this.mService.getVendorId();
        HdmiCecMessage cecMessage = HdmiCecMessageBuilder.buildDeviceVendorIdCommand(this.mAddress, vendorId);
        this.mService.sendCecCommand(cecMessage);
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGetCecVersion(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int version = this.mService.getCecVersion();
        HdmiCecMessage cecMessage = HdmiCecMessageBuilder.buildCecVersion(message.getDestination(), message.getSource(), version);
        this.mService.sendCecCommand(cecMessage);
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleActiveSource(HdmiCecMessage message) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleInactiveSource(HdmiCecMessage message) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRequestActiveSource(HdmiCecMessage message) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGetMenuLanguage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        Slog.w(TAG, "Only TV can handle <Get Menu Language>:" + message.toString());
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleGiveOsdName(HdmiCecMessage message) {
        assertRunOnServiceThread();
        HdmiCecMessage cecMessage = HdmiCecMessageBuilder.buildSetOsdNameCommand(this.mAddress, message.getSource(), this.mDeviceInfo.getDisplayName());
        if (cecMessage != null) {
            this.mService.sendCecCommand(cecMessage);
            return true;
        }
        Slog.w(TAG, "Failed to build <Get Osd Name>:" + this.mDeviceInfo.getDisplayName());
        return true;
    }

    protected boolean handleRoutingChange(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleRoutingInformation(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleReportPhysicalAddress(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleSystemAudioModeStatus(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleSetSystemAudioMode(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleTerminateArc(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleInitiateArc(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleReportAudioStatus(HdmiCecMessage message) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleStandby(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled() || this.mService.isProhibitMode() || !this.mService.isPowerOnOrTransient()) {
            return false;
        }
        this.mService.standby();
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleUserControlPressed(HdmiCecMessage message) {
        assertRunOnServiceThread();
        this.mHandler.removeMessages(2);
        if (this.mService.isPowerOnOrTransient() && isPowerOffOrToggleCommand(message)) {
            this.mService.standby();
            return true;
        }
        if (this.mService.isPowerStandbyOrTransient() && isPowerOnOrToggleCommand(message)) {
            this.mService.wakeUp();
            return true;
        }
        long downTime = SystemClock.uptimeMillis();
        byte[] params = message.getParams();
        int keycode = HdmiCecKeycode.cecKeycodeAndParamsToAndroidKey(params);
        int keyRepeatCount = 0;
        if (this.mLastKeycode != -1) {
            if (keycode == this.mLastKeycode) {
                keyRepeatCount = this.mLastKeyRepeatCount + 1;
            } else {
                injectKeyEvent(downTime, 1, this.mLastKeycode, 0);
            }
        }
        this.mLastKeycode = keycode;
        this.mLastKeyRepeatCount = keyRepeatCount;
        if (keycode == -1) {
            return false;
        }
        injectKeyEvent(downTime, 0, keycode, keyRepeatCount);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 2), 550L);
        return true;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleUserControlReleased() {
        assertRunOnServiceThread();
        this.mHandler.removeMessages(2);
        this.mLastKeyRepeatCount = 0;
        if (this.mLastKeycode == -1) {
            return false;
        }
        long upTime = SystemClock.uptimeMillis();
        injectKeyEvent(upTime, 1, this.mLastKeycode, 0);
        this.mLastKeycode = -1;
        return true;
    }

    static void injectKeyEvent(long time, int action, int keycode, int repeat) {
        KeyEvent keyEvent = KeyEvent.obtain(time, time, action, keycode, repeat, 0, -1, 0, 8, 33554433, null);
        InputManager.getInstance().injectInputEvent(keyEvent, 0);
        keyEvent.recycle();
    }

    static boolean isPowerOnOrToggleCommand(HdmiCecMessage message) {
        byte[] params = message.getParams();
        if (message.getOpcode() == 68) {
            return params[0] == 64 || params[0] == 109 || params[0] == 107;
        }
        return false;
    }

    static boolean isPowerOffOrToggleCommand(HdmiCecMessage message) {
        byte[] params = message.getParams();
        if (message.getOpcode() == 68) {
            return params[0] == 64 || params[0] == 108 || params[0] == 107;
        }
        return false;
    }

    protected boolean handleTextViewOn(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleImageViewOn(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleSetStreamPath(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleGiveDevicePowerStatus(HdmiCecMessage message) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPowerStatus(this.mAddress, message.getSource(), this.mService.getPowerStatus()));
        return true;
    }

    protected boolean handleMenuRequest(HdmiCecMessage message) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportMenuStatus(this.mAddress, message.getSource(), 0));
        return true;
    }

    protected boolean handleMenuStatus(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleVendorCommand(HdmiCecMessage message) {
        if (!this.mService.invokeVendorCommandListenersOnReceived(this.mDeviceType, message.getSource(), message.getDestination(), message.getParams(), false)) {
            this.mService.maySendFeatureAbortCommand(message, 1);
        }
        return true;
    }

    protected boolean handleVendorCommandWithId(HdmiCecMessage message) {
        byte[] params = message.getParams();
        int vendorId = HdmiUtils.threeBytesToInt(params);
        if (vendorId == this.mService.getVendorId()) {
            if (!this.mService.invokeVendorCommandListenersOnReceived(this.mDeviceType, message.getSource(), message.getDestination(), params, true)) {
                this.mService.maySendFeatureAbortCommand(message, 1);
            }
        } else if (message.getDestination() != 15 && message.getSource() != 15) {
            Slog.v(TAG, "Wrong direct vendor command. Replying with <Feature Abort>");
            this.mService.maySendFeatureAbortCommand(message, 0);
        } else {
            Slog.v(TAG, "Wrong broadcast vendor command. Ignoring");
        }
        return true;
    }

    protected void sendStandby(int deviceId) {
    }

    protected boolean handleSetOsdName(HdmiCecMessage message) {
        return true;
    }

    protected boolean handleRecordTvScreen(HdmiCecMessage message) {
        this.mService.maySendFeatureAbortCommand(message, 2);
        return true;
    }

    protected boolean handleTimerClearedStatus(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleReportPowerStatus(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleTimerStatus(HdmiCecMessage message) {
        return false;
    }

    protected boolean handleRecordStatus(HdmiCecMessage message) {
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    final void handleAddressAllocated(int logicalAddress, int reason) {
        assertRunOnServiceThread();
        this.mPreferredAddress = logicalAddress;
        this.mAddress = logicalAddress;
        onAddressAllocated(logicalAddress, reason);
        setPreferredAddress(logicalAddress);
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
    void setDeviceInfo(HdmiDeviceInfo info) {
        assertRunOnServiceThread();
        this.mDeviceInfo = info;
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isAddressOf(int addr) {
        assertRunOnServiceThread();
        return addr == this.mAddress;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void clearAddress() {
        assertRunOnServiceThread();
        this.mAddress = 15;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void addAndStartAction(HdmiCecFeatureAction action) {
        assertRunOnServiceThread();
        this.mActions.add(action);
        if (this.mService.isPowerStandbyOrTransient()) {
            Slog.i(TAG, "Not ready to start action. Queued for deferred start:" + action);
        } else {
            action.start();
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startQueuedActions() {
        assertRunOnServiceThread();
        for (HdmiCecFeatureAction action : this.mActions) {
            if (!action.started()) {
                Slog.i(TAG, "Starting queued action:" + action);
                action.start();
            }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    <T extends HdmiCecFeatureAction> boolean hasAction(Class<T> clazz) {
        assertRunOnServiceThread();
        for (HdmiCecFeatureAction action : this.mActions) {
            if (action.getClass().equals(clazz)) {
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
    void removeAction(HdmiCecFeatureAction action) {
        assertRunOnServiceThread();
        action.finish(false);
        this.mActions.remove(action);
        checkIfPendingActionsCleared();
    }

    @HdmiAnnotations.ServiceThreadOnly
    <T extends HdmiCecFeatureAction> void removeAction(Class<T> clazz) {
        assertRunOnServiceThread();
        removeActionExcept(clazz, null);
    }

    @HdmiAnnotations.ServiceThreadOnly
    <T extends HdmiCecFeatureAction> void removeActionExcept(Class<T> clazz, HdmiCecFeatureAction exception) {
        assertRunOnServiceThread();
        Iterator<HdmiCecFeatureAction> iter = this.mActions.iterator();
        while (iter.hasNext()) {
            HdmiCecFeatureAction action = iter.next();
            if (action != exception && action.getClass().equals(clazz)) {
                action.finish(false);
                iter.remove();
            }
        }
        checkIfPendingActionsCleared();
    }

    protected void checkIfPendingActionsCleared() {
        if (this.mActions.isEmpty() && this.mPendingActionClearedCallback != null) {
            PendingActionClearedCallback callback = this.mPendingActionClearedCallback;
            this.mPendingActionClearedCallback = null;
            callback.onCleared(this);
        }
    }

    protected void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mService.getServiceLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    void onHotplug(int portId, boolean connected) {
    }

    final HdmiControlService getService() {
        return this.mService;
    }

    @HdmiAnnotations.ServiceThreadOnly
    final boolean isConnectedToArcPort(int path) {
        assertRunOnServiceThread();
        return this.mService.isConnectedToArcPort(path);
    }

    ActiveSource getActiveSource() {
        ActiveSource activeSource;
        synchronized (this.mLock) {
            activeSource = this.mActiveSource;
        }
        return activeSource;
    }

    void setActiveSource(ActiveSource newActive) {
        setActiveSource(newActive.logicalAddress, newActive.physicalAddress);
    }

    void setActiveSource(HdmiDeviceInfo info) {
        setActiveSource(info.getLogicalAddress(), info.getPhysicalAddress());
    }

    void setActiveSource(int logicalAddress, int physicalAddress) {
        synchronized (this.mLock) {
            this.mActiveSource.logicalAddress = logicalAddress;
            this.mActiveSource.physicalAddress = physicalAddress;
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

    void setActivePath(int path) {
        synchronized (this.mLock) {
            this.mActiveRoutingPath = path;
        }
        this.mService.setActivePortId(pathToPortId(path));
    }

    int getActivePortId() {
        int iPathToPortId;
        synchronized (this.mLock) {
            iPathToPortId = this.mService.pathToPortId(this.mActiveRoutingPath);
        }
        return iPathToPortId;
    }

    void setActivePortId(int portId) {
        setActivePath(this.mService.portIdToPath(portId));
    }

    @HdmiAnnotations.ServiceThreadOnly
    HdmiCecMessageCache getCecMessageCache() {
        assertRunOnServiceThread();
        return this.mCecMessageCache;
    }

    @HdmiAnnotations.ServiceThreadOnly
    int pathToPortId(int newPath) {
        assertRunOnServiceThread();
        return this.mService.pathToPortId(newPath);
    }

    protected void onStandby(boolean initiatedByCec) {
    }

    protected void disableDevice(boolean initiatedByCec, final PendingActionClearedCallback origialCallback) {
        this.mPendingActionClearedCallback = new PendingActionClearedCallback() {
            @Override
            public void onCleared(HdmiCecLocalDevice device) {
                HdmiCecLocalDevice.this.mHandler.removeMessages(1);
                origialCallback.onCleared(device);
            }
        };
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 1), 5000L);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleDisableDeviceTimeout() {
        assertRunOnServiceThread();
        Iterator<HdmiCecFeatureAction> iter = this.mActions.iterator();
        while (iter.hasNext()) {
            HdmiCecFeatureAction action = iter.next();
            action.finish(false);
            iter.remove();
        }
    }

    protected void sendKeyEvent(int keyCode, boolean isPressed) {
        Slog.w(TAG, "sendKeyEvent not implemented");
    }

    void sendUserControlPressedAndReleased(int targetAddress, int cecKeycode) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildUserControlPressed(this.mAddress, targetAddress, cecKeycode));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildUserControlReleased(this.mAddress, targetAddress));
    }

    protected void dump(IndentingPrintWriter pw) {
        pw.println("mDeviceType: " + this.mDeviceType);
        pw.println("mAddress: " + this.mAddress);
        pw.println("mPreferredAddress: " + this.mPreferredAddress);
        pw.println("mDeviceInfo: " + this.mDeviceInfo);
        pw.println("mActiveSource: " + this.mActiveSource);
        pw.println(String.format("mActiveRoutingPath: 0x%04x", Integer.valueOf(this.mActiveRoutingPath)));
    }
}
