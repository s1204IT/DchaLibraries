package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.app.LocalePicker;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecLocalDevice;
import java.io.UnsupportedEncodingException;
import java.util.List;

final class HdmiCecLocalDevicePlayback extends HdmiCecLocalDevice {
    private static final String TAG = "HdmiCecLocalDevicePlayback";
    private boolean mAutoTvOff;
    private boolean mIsActiveSource;
    private ActiveWakeLock mWakeLock;
    private static final boolean WAKE_ON_HOTPLUG = SystemProperties.getBoolean("ro.hdmi.wake_on_hotplug", true);
    private static final boolean SET_MENU_LANGUAGE = SystemProperties.getBoolean("ro.hdmi.set_menu_language", false);

    private interface ActiveWakeLock {
        void acquire();

        boolean isHeld();

        void release();
    }

    HdmiCecLocalDevicePlayback(HdmiControlService hdmiControlService) {
        super(hdmiControlService, 4);
        this.mIsActiveSource = false;
        this.mAutoTvOff = this.mService.readBooleanSetting("hdmi_control_auto_device_off_enabled", false);
        this.mService.writeBooleanSetting("hdmi_control_auto_device_off_enabled", this.mAutoTvOff);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void onAddressAllocated(int i, int i2) {
        assertRunOnServiceThread();
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(this.mAddress, this.mService.getPhysicalAddress(), this.mDeviceType));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildDeviceVendorIdCommand(this.mAddress, this.mService.getVendorId()));
        startQueuedActions();
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected int getPreferredAddress() {
        assertRunOnServiceThread();
        return SystemProperties.getInt("persist.sys.hdmi.addr.playback", 15);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void setPreferredAddress(int i) {
        assertRunOnServiceThread();
        SystemProperties.set("persist.sys.hdmi.addr.playback", String.valueOf(i));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void oneTouchPlay(IHdmiControlCallback iHdmiControlCallback) {
        assertRunOnServiceThread();
        List actions = getActions(OneTouchPlayAction.class);
        if (!actions.isEmpty()) {
            Slog.i(TAG, "oneTouchPlay already in progress");
            ((OneTouchPlayAction) actions.get(0)).addCallback(iHdmiControlCallback);
            return;
        }
        OneTouchPlayAction oneTouchPlayActionCreate = OneTouchPlayAction.create(this, 0, iHdmiControlCallback);
        if (oneTouchPlayActionCreate == null) {
            Slog.w(TAG, "Cannot initiate oneTouchPlay");
            invokeCallback(iHdmiControlCallback, 5);
        } else {
            addAndStartAction(oneTouchPlayActionCreate);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void queryDisplayStatus(IHdmiControlCallback iHdmiControlCallback) {
        assertRunOnServiceThread();
        List actions = getActions(DevicePowerStatusAction.class);
        if (!actions.isEmpty()) {
            Slog.i(TAG, "queryDisplayStatus already in progress");
            ((DevicePowerStatusAction) actions.get(0)).addCallback(iHdmiControlCallback);
            return;
        }
        DevicePowerStatusAction devicePowerStatusActionCreate = DevicePowerStatusAction.create(this, 0, iHdmiControlCallback);
        if (devicePowerStatusActionCreate == null) {
            Slog.w(TAG, "Cannot initiate queryDisplayStatus");
            invokeCallback(iHdmiControlCallback, 5);
        } else {
            addAndStartAction(devicePowerStatusActionCreate);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void invokeCallback(IHdmiControlCallback iHdmiControlCallback, int i) {
        assertRunOnServiceThread();
        try {
            iHdmiControlCallback.onComplete(i);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    void onHotplug(int i, boolean z) {
        assertRunOnServiceThread();
        this.mCecMessageCache.flushAll();
        if (WAKE_ON_HOTPLUG && z && this.mService.isPowerStandbyOrTransient()) {
            this.mService.wakeUp();
        }
        if (!z) {
            getWakeLock().release();
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void onStandby(boolean z, int i) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled() || z || !this.mAutoTvOff) {
            return;
        }
        switch (i) {
            case 0:
                this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(this.mAddress, 0));
                break;
            case 1:
                this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(this.mAddress, 15));
                break;
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    void setAutoDeviceOff(boolean z) {
        assertRunOnServiceThread();
        this.mAutoTvOff = z;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setActiveSource(boolean z) {
        assertRunOnServiceThread();
        this.mIsActiveSource = z;
        if (z) {
            getWakeLock().acquire();
        } else {
            getWakeLock().release();
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private ActiveWakeLock getWakeLock() {
        assertRunOnServiceThread();
        if (this.mWakeLock == null) {
            if (SystemProperties.getBoolean("persist.sys.hdmi.keep_awake", true)) {
                this.mWakeLock = new SystemWakeLock();
            } else {
                this.mWakeLock = new ActiveWakeLock() {
                    @Override
                    public void acquire() {
                    }

                    @Override
                    public void release() {
                    }

                    @Override
                    public boolean isHeld() {
                        return false;
                    }
                };
                HdmiLogger.debug("No wakelock is used to keep the display on.", new Object[0]);
            }
        }
        return this.mWakeLock;
    }

    @Override
    protected boolean canGoToStandby() {
        return !getWakeLock().isHeld();
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleActiveSource(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        mayResetActiveSource(HdmiUtils.twoBytesToInt(hdmiCecMessage.getParams()));
        return true;
    }

    private void mayResetActiveSource(int i) {
        if (i != this.mService.getPhysicalAddress()) {
            setActiveSource(false);
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleUserControlPressed(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        wakeUpIfActiveSource();
        return super.handleUserControlPressed(hdmiCecMessage);
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSetStreamPath(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        maySetActiveSource(HdmiUtils.twoBytesToInt(hdmiCecMessage.getParams()));
        maySendActiveSource(hdmiCecMessage.getSource());
        wakeUpIfActiveSource();
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRoutingChange(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        maySetActiveSource(HdmiUtils.twoBytesToInt(hdmiCecMessage.getParams(), 2));
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRoutingInformation(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        maySetActiveSource(HdmiUtils.twoBytesToInt(hdmiCecMessage.getParams()));
        return true;
    }

    private void maySetActiveSource(int i) {
        setActiveSource(i == this.mService.getPhysicalAddress());
    }

    private void wakeUpIfActiveSource() {
        if (!this.mIsActiveSource) {
            return;
        }
        if (this.mService.isPowerStandbyOrTransient() || !this.mService.getPowerManager().isScreenOn()) {
            this.mService.wakeUp();
        }
    }

    private void maySendActiveSource(int i) {
        if (this.mIsActiveSource) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(this.mAddress, this.mService.getPhysicalAddress()));
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportMenuStatus(this.mAddress, i, 0));
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRequestActiveSource(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        maySendActiveSource(hdmiCecMessage.getSource());
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSetMenuLanguage(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (!SET_MENU_LANGUAGE) {
            return false;
        }
        try {
            String str = new String(hdmiCecMessage.getParams(), 0, 3, "US-ASCII");
            if (this.mService.getContext().getResources().getConfiguration().locale.getISO3Language().equals(str)) {
                return true;
            }
            for (LocalePicker.LocaleInfo localeInfo : LocalePicker.getAllAssetLocales(this.mService.getContext(), false)) {
                if (localeInfo.getLocale().getISO3Language().equals(str)) {
                    LocalePicker.updateLocale(localeInfo.getLocale());
                    return true;
                }
            }
            Slog.w(TAG, "Can't handle <Set Menu Language> of " + str);
            return false;
        } catch (UnsupportedEncodingException e) {
            Slog.w(TAG, "Can't handle <Set Menu Language>", e);
            return false;
        }
    }

    @Override
    protected int findKeyReceiverAddress() {
        return 0;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void sendStandby(int i) {
        assertRunOnServiceThread();
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(this.mAddress, 0));
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void disableDevice(boolean z, HdmiCecLocalDevice.PendingActionClearedCallback pendingActionClearedCallback) {
        super.disableDevice(z, pendingActionClearedCallback);
        assertRunOnServiceThread();
        if (!z && this.mIsActiveSource) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildInactiveSource(this.mAddress, this.mService.getPhysicalAddress()));
        }
        setActiveSource(false);
        checkIfPendingActionsCleared();
    }

    @Override
    protected void dump(IndentingPrintWriter indentingPrintWriter) {
        super.dump(indentingPrintWriter);
        indentingPrintWriter.println("mIsActiveSource: " + this.mIsActiveSource);
        indentingPrintWriter.println("mAutoTvOff:" + this.mAutoTvOff);
    }

    private class SystemWakeLock implements ActiveWakeLock {
        private final PowerManager.WakeLock mWakeLock;

        public SystemWakeLock() {
            this.mWakeLock = HdmiCecLocalDevicePlayback.this.mService.getPowerManager().newWakeLock(1, HdmiCecLocalDevicePlayback.TAG);
            this.mWakeLock.setReferenceCounted(false);
        }

        @Override
        public void acquire() {
            this.mWakeLock.acquire();
            HdmiLogger.debug("active source: %b. Wake lock acquired", Boolean.valueOf(HdmiCecLocalDevicePlayback.this.mIsActiveSource));
        }

        @Override
        public void release() {
            this.mWakeLock.release();
            HdmiLogger.debug("Wake lock released", new Object[0]);
        }

        @Override
        public boolean isHeld() {
            return this.mWakeLock.isHeld();
        }
    }
}
