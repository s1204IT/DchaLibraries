package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecLocalDevice;

final class HdmiCecLocalDevicePlayback extends HdmiCecLocalDevice {
    private static final String TAG = "HdmiCecLocalDevicePlayback";
    private boolean mIsActiveSource;
    private PowerManager.WakeLock mWakeLock;

    HdmiCecLocalDevicePlayback(HdmiControlService service) {
        super(service, 4);
        this.mIsActiveSource = false;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void onAddressAllocated(int logicalAddress, int reason) {
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
    protected void setPreferredAddress(int addr) {
        assertRunOnServiceThread();
        SystemProperties.set("persist.sys.hdmi.addr.playback", String.valueOf(addr));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void oneTouchPlay(IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (hasAction(OneTouchPlayAction.class)) {
            Slog.w(TAG, "oneTouchPlay already in progress");
            invokeCallback(callback, 4);
            return;
        }
        OneTouchPlayAction action = OneTouchPlayAction.create(this, 0, callback);
        if (action == null) {
            Slog.w(TAG, "Cannot initiate oneTouchPlay");
            invokeCallback(callback, 5);
        } else {
            addAndStartAction(action);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void queryDisplayStatus(IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (hasAction(DevicePowerStatusAction.class)) {
            Slog.w(TAG, "queryDisplayStatus already in progress");
            invokeCallback(callback, 4);
            return;
        }
        DevicePowerStatusAction action = DevicePowerStatusAction.create(this, 0, callback);
        if (action == null) {
            Slog.w(TAG, "Cannot initiate queryDisplayStatus");
            invokeCallback(callback, 5);
        } else {
            addAndStartAction(action);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void invokeCallback(IHdmiControlCallback callback, int result) {
        assertRunOnServiceThread();
        try {
            callback.onComplete(result);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    void onHotplug(int portId, boolean connected) {
        assertRunOnServiceThread();
        this.mCecMessageCache.flushAll();
        if (connected && this.mService.isPowerStandbyOrTransient()) {
            this.mService.wakeUp();
        }
        if (!connected) {
            getWakeLock().release();
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setActiveSource(boolean on) {
        assertRunOnServiceThread();
        this.mIsActiveSource = on;
        if (on) {
            getWakeLock().acquire();
            HdmiLogger.debug("active source: %b. Wake lock acquired", Boolean.valueOf(this.mIsActiveSource));
        } else {
            getWakeLock().release();
            HdmiLogger.debug("Wake lock released", new Object[0]);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private PowerManager.WakeLock getWakeLock() {
        assertRunOnServiceThread();
        if (this.mWakeLock == null) {
            this.mWakeLock = this.mService.getPowerManager().newWakeLock(1, TAG);
            this.mWakeLock.setReferenceCounted(false);
        }
        return this.mWakeLock;
    }

    @Override
    protected boolean canGoToStandby() {
        return !getWakeLock().isHeld();
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        mayResetActiveSource(physicalAddress);
        return true;
    }

    private void mayResetActiveSource(int physicalAddress) {
        if (physicalAddress != this.mService.getPhysicalAddress()) {
            setActiveSource(false);
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleSetStreamPath(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        maySetActiveSource(physicalAddress);
        maySendActiveSource(message.getSource());
        wakeUpIfActiveSource();
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRoutingChange(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int newPath = HdmiUtils.twoBytesToInt(message.getParams(), 2);
        maySetActiveSource(newPath);
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRoutingInformation(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        maySetActiveSource(physicalAddress);
        return true;
    }

    private void maySetActiveSource(int physicalAddress) {
        setActiveSource(physicalAddress == this.mService.getPhysicalAddress());
    }

    private void wakeUpIfActiveSource() {
        if (this.mIsActiveSource && this.mService.isPowerStandbyOrTransient()) {
            this.mService.wakeUp();
        }
    }

    private void maySendActiveSource(int dest) {
        if (this.mIsActiveSource) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(this.mAddress, this.mService.getPhysicalAddress()));
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportMenuStatus(this.mAddress, dest, 0));
        }
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean handleRequestActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        maySendActiveSource(message.getSource());
        return true;
    }

    @Override
    @HdmiAnnotations.ServiceThreadOnly
    protected void disableDevice(boolean initiatedByCec, HdmiCecLocalDevice.PendingActionClearedCallback callback) {
        super.disableDevice(initiatedByCec, callback);
        assertRunOnServiceThread();
        if (!initiatedByCec && this.mIsActiveSource) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildInactiveSource(this.mAddress, this.mService.getPhysicalAddress()));
        }
        setActiveSource(false);
        checkIfPendingActionsCleared();
    }

    @Override
    protected void dump(IndentingPrintWriter pw) {
        super.dump(pw);
        pw.println("mIsActiveSource: " + this.mIsActiveSource);
    }
}
