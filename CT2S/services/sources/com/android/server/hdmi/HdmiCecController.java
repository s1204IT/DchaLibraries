package com.android.server.hdmi;

import android.hardware.hdmi.HdmiPortInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Predicate;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiControlService;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import libcore.util.EmptyArray;

final class HdmiCecController {
    private static final byte[] EMPTY_BODY = EmptyArray.BYTE;
    private static final int NUM_LOGICAL_ADDRESS = 16;
    private static final String TAG = "HdmiCecController";
    private Handler mControlHandler;
    private Handler mIoHandler;
    private volatile long mNativePtr;
    private final HdmiControlService mService;
    private final Predicate<Integer> mRemoteDeviceAddressPredicate = new Predicate<Integer>() {
        public boolean apply(Integer address) {
            return !HdmiCecController.this.isAllocatedLocalDeviceAddress(address.intValue());
        }
    };
    private final Predicate<Integer> mSystemAudioAddressPredicate = new Predicate<Integer>() {
        public boolean apply(Integer address) {
            return HdmiUtils.getTypeFromAddress(address.intValue()) == 5;
        }
    };
    private final SparseArray<HdmiCecLocalDevice> mLocalDevices = new SparseArray<>();

    interface AllocateAddressCallback {
        void onAllocated(int i, int i2);
    }

    private static native int nativeAddLogicalAddress(long j, int i);

    private static native void nativeClearLogicalAddress(long j);

    private static native int nativeGetPhysicalAddress(long j);

    private static native HdmiPortInfo[] nativeGetPortInfos(long j);

    private static native int nativeGetVendorId(long j);

    private static native int nativeGetVersion(long j);

    private static native long nativeInit(HdmiCecController hdmiCecController, MessageQueue messageQueue);

    private static native boolean nativeIsConnected(long j, int i);

    private static native int nativeSendCecCommand(long j, int i, int i2, byte[] bArr);

    private static native void nativeSetAudioReturnChannel(long j, int i, boolean z);

    private static native void nativeSetOption(long j, int i, int i2);

    private HdmiCecController(HdmiControlService service) {
        this.mService = service;
    }

    static HdmiCecController create(HdmiControlService service) {
        HdmiCecController controller = new HdmiCecController(service);
        long nativePtr = nativeInit(controller, service.getServiceLooper().getQueue());
        if (nativePtr == 0) {
            return null;
        }
        controller.init(nativePtr);
        return controller;
    }

    private void init(long nativePtr) {
        this.mIoHandler = new Handler(this.mService.getIoLooper());
        this.mControlHandler = new Handler(this.mService.getServiceLooper());
        this.mNativePtr = nativePtr;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void addLocalDevice(int deviceType, HdmiCecLocalDevice device) {
        assertRunOnServiceThread();
        this.mLocalDevices.put(deviceType, device);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void allocateLogicalAddress(final int deviceType, final int preferredAddress, final AllocateAddressCallback callback) {
        assertRunOnServiceThread();
        runOnIoThread(new Runnable() {
            @Override
            public void run() {
                HdmiCecController.this.handleAllocateLogicalAddress(deviceType, preferredAddress, callback);
            }
        });
    }

    @HdmiAnnotations.IoThreadOnly
    private void handleAllocateLogicalAddress(final int deviceType, int preferredAddress, final AllocateAddressCallback callback) {
        assertRunOnIoThread();
        int startAddress = preferredAddress;
        if (preferredAddress == 15) {
            int i = 0;
            while (true) {
                if (i >= 16) {
                    break;
                }
                if (deviceType != HdmiUtils.getTypeFromAddress(i)) {
                    i++;
                } else {
                    startAddress = i;
                    break;
                }
            }
        }
        int logicalAddress = 15;
        int i2 = 0;
        while (true) {
            if (i2 >= 16) {
                break;
            }
            int curAddress = (startAddress + i2) % 16;
            if (curAddress != 15 && deviceType == HdmiUtils.getTypeFromAddress(curAddress)) {
                int failedPollingCount = 0;
                for (int j = 0; j < 3; j++) {
                    if (!sendPollMessage(curAddress, curAddress, 1)) {
                        failedPollingCount++;
                    }
                }
                if (failedPollingCount * 2 > 3) {
                    logicalAddress = curAddress;
                    break;
                }
            }
            i2++;
        }
        final int assignedAddress = logicalAddress;
        HdmiLogger.debug("New logical address for device [%d]: [preferred:%d, assigned:%d]", Integer.valueOf(deviceType), Integer.valueOf(preferredAddress), Integer.valueOf(assignedAddress));
        if (callback != null) {
            runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    callback.onAllocated(deviceType, assignedAddress);
                }
            });
        }
    }

    private static byte[] buildBody(int opcode, byte[] params) {
        byte[] body = new byte[params.length + 1];
        body[0] = (byte) opcode;
        System.arraycopy(params, 0, body, 1, params.length);
        return body;
    }

    HdmiPortInfo[] getPortInfos() {
        return nativeGetPortInfos(this.mNativePtr);
    }

    HdmiCecLocalDevice getLocalDevice(int deviceType) {
        return this.mLocalDevices.get(deviceType);
    }

    @HdmiAnnotations.ServiceThreadOnly
    int addLogicalAddress(int newLogicalAddress) {
        assertRunOnServiceThread();
        if (HdmiUtils.isValidAddress(newLogicalAddress)) {
            return nativeAddLogicalAddress(this.mNativePtr, newLogicalAddress);
        }
        return -1;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void clearLogicalAddress() {
        assertRunOnServiceThread();
        for (int i = 0; i < this.mLocalDevices.size(); i++) {
            this.mLocalDevices.valueAt(i).clearAddress();
        }
        nativeClearLogicalAddress(this.mNativePtr);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void clearLocalDevices() {
        assertRunOnServiceThread();
        this.mLocalDevices.clear();
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getPhysicalAddress() {
        assertRunOnServiceThread();
        return nativeGetPhysicalAddress(this.mNativePtr);
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getVersion() {
        assertRunOnServiceThread();
        return nativeGetVersion(this.mNativePtr);
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getVendorId() {
        assertRunOnServiceThread();
        return nativeGetVendorId(this.mNativePtr);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setOption(int flag, int value) {
        assertRunOnServiceThread();
        HdmiLogger.debug("setOption: [flag:%d, value:%d]", Integer.valueOf(flag), Integer.valueOf(value));
        nativeSetOption(this.mNativePtr, flag, value);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setAudioReturnChannel(int port, boolean enabled) {
        assertRunOnServiceThread();
        nativeSetAudioReturnChannel(this.mNativePtr, port, enabled);
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isConnected(int port) {
        assertRunOnServiceThread();
        return nativeIsConnected(this.mNativePtr, port);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void pollDevices(HdmiControlService.DevicePollingCallback callback, int sourceAddress, int pickStrategy, int retryCount) {
        assertRunOnServiceThread();
        List<Integer> pollingCandidates = pickPollCandidates(pickStrategy);
        ArrayList<Integer> allocated = new ArrayList<>();
        runDevicePolling(sourceAddress, pollingCandidates, retryCount, callback, allocated);
    }

    @HdmiAnnotations.ServiceThreadOnly
    List<HdmiCecLocalDevice> getLocalDeviceList() {
        assertRunOnServiceThread();
        return HdmiUtils.sparseArrayToList(this.mLocalDevices);
    }

    private List<Integer> pickPollCandidates(int pickStrategy) {
        Predicate<Integer> pickPredicate;
        int strategy = pickStrategy & 3;
        switch (strategy) {
            case 2:
                pickPredicate = this.mSystemAudioAddressPredicate;
                break;
            default:
                pickPredicate = this.mRemoteDeviceAddressPredicate;
                break;
        }
        int iterationStrategy = pickStrategy & 196608;
        LinkedList<Integer> pollingCandidates = new LinkedList<>();
        switch (iterationStrategy) {
            case 65536:
                for (int i = 0; i <= 14; i++) {
                    if (pickPredicate.apply(Integer.valueOf(i))) {
                        pollingCandidates.add(Integer.valueOf(i));
                    }
                }
                return pollingCandidates;
            default:
                for (int i2 = 14; i2 >= 0; i2--) {
                    if (pickPredicate.apply(Integer.valueOf(i2))) {
                        pollingCandidates.add(Integer.valueOf(i2));
                    }
                }
                return pollingCandidates;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private boolean isAllocatedLocalDeviceAddress(int address) {
        assertRunOnServiceThread();
        for (int i = 0; i < this.mLocalDevices.size(); i++) {
            if (this.mLocalDevices.valueAt(i).isAddressOf(address)) {
                return true;
            }
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void runDevicePolling(final int sourceAddress, final List<Integer> candidates, final int retryCount, final HdmiControlService.DevicePollingCallback callback, final List<Integer> allocated) {
        assertRunOnServiceThread();
        if (candidates.isEmpty()) {
            if (callback != null) {
                HdmiLogger.debug("[P]:AllocatedAddress=%s", allocated.toString());
                callback.onPollingFinished(allocated);
                return;
            }
            return;
        }
        final Integer candidate = candidates.remove(0);
        runOnIoThread(new Runnable() {
            @Override
            public void run() {
                if (HdmiCecController.this.sendPollMessage(sourceAddress, candidate.intValue(), retryCount)) {
                    allocated.add(candidate);
                }
                HdmiCecController.this.runOnServiceThread(new Runnable() {
                    @Override
                    public void run() {
                        HdmiCecController.this.runDevicePolling(sourceAddress, candidates, retryCount, callback, allocated);
                    }
                });
            }
        });
    }

    @HdmiAnnotations.IoThreadOnly
    private boolean sendPollMessage(int sourceAddress, int destinationAddress, int retryCount) {
        assertRunOnIoThread();
        for (int i = 0; i < retryCount; i++) {
            if (nativeSendCecCommand(this.mNativePtr, sourceAddress, destinationAddress, EMPTY_BODY) == 0) {
                return true;
            }
        }
        return false;
    }

    private void assertRunOnIoThread() {
        if (Looper.myLooper() != this.mIoHandler.getLooper()) {
            throw new IllegalStateException("Should run on io thread.");
        }
    }

    private void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mControlHandler.getLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    private void runOnIoThread(Runnable runnable) {
        this.mIoHandler.post(runnable);
    }

    private void runOnServiceThread(Runnable runnable) {
        this.mControlHandler.post(runnable);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void flush(final Runnable runnable) {
        assertRunOnServiceThread();
        runOnIoThread(new Runnable() {
            @Override
            public void run() {
                HdmiCecController.this.runOnServiceThread(runnable);
            }
        });
    }

    private boolean isAcceptableAddress(int address) {
        if (address == 15) {
            return true;
        }
        return isAllocatedLocalDeviceAddress(address);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void onReceiveCommand(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!isAcceptableAddress(message.getDestination()) || !this.mService.handleCecCommand(message)) {
            maySendFeatureAbortCommand(message, 0);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void maySendFeatureAbortCommand(HdmiCecMessage message, int reason) {
        int originalOpcode;
        assertRunOnServiceThread();
        int src = message.getDestination();
        int dest = message.getSource();
        if (src != 15 && dest != 15 && (originalOpcode = message.getOpcode()) != 0) {
            sendCommand(HdmiCecMessageBuilder.buildFeatureAbortCommand(src, dest, originalOpcode, reason));
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void sendCommand(HdmiCecMessage cecMessage) {
        assertRunOnServiceThread();
        sendCommand(cecMessage, null);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void sendCommand(final HdmiCecMessage cecMessage, final HdmiControlService.SendMessageCallback callback) {
        assertRunOnServiceThread();
        runOnIoThread(new Runnable() {
            @Override
            public void run() {
                final int errorCode;
                HdmiLogger.debug("[S]:" + cecMessage, new Object[0]);
                byte[] body = HdmiCecController.buildBody(cecMessage.getOpcode(), cecMessage.getParams());
                int i = 0;
                while (true) {
                    errorCode = HdmiCecController.nativeSendCecCommand(HdmiCecController.this.mNativePtr, cecMessage.getSource(), cecMessage.getDestination(), body);
                    if (errorCode == 0) {
                        break;
                    }
                    int i2 = i + 1;
                    if (i >= 1) {
                        break;
                    } else {
                        i = i2;
                    }
                }
                if (errorCode != 0) {
                    Slog.w(HdmiCecController.TAG, "Failed to send " + cecMessage);
                }
                if (callback != null) {
                    HdmiCecController.this.runOnServiceThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSendCompleted(errorCode);
                        }
                    });
                }
            }
        });
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleIncomingCecCommand(int srcAddress, int dstAddress, byte[] body) {
        assertRunOnServiceThread();
        HdmiCecMessage command = HdmiCecMessageBuilder.of(srcAddress, dstAddress, body);
        HdmiLogger.debug("[R]:" + command, new Object[0]);
        onReceiveCommand(command);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleHotplug(int port, boolean connected) {
        assertRunOnServiceThread();
        HdmiLogger.debug("Hotplug event:[port:%d, connected:%b]", Integer.valueOf(port), Boolean.valueOf(connected));
        this.mService.onHotplug(port, connected);
    }

    void dump(IndentingPrintWriter pw) {
        for (int i = 0; i < this.mLocalDevices.size(); i++) {
            pw.println("HdmiCecLocalDevice #" + i + ":");
            pw.increaseIndent();
            this.mLocalDevices.valueAt(i).dump(pw);
            pw.decreaseIndent();
        }
    }
}
