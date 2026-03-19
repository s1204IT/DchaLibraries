package com.android.server.hdmi;

import android.hardware.hdmi.HdmiPortInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiControlService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Predicate;
import libcore.util.EmptyArray;
import sun.util.locale.LanguageTag;

final class HdmiCecController {
    private static final byte[] EMPTY_BODY = EmptyArray.BYTE;
    private static final int MAX_CEC_MESSAGE_HISTORY = 20;
    private static final int NUM_LOGICAL_ADDRESS = 16;
    private static final String TAG = "HdmiCecController";
    private Handler mControlHandler;
    private Handler mIoHandler;
    private volatile long mNativePtr;
    private final HdmiControlService mService;
    private final Predicate<Integer> mRemoteDeviceAddressPredicate = new Predicate<Integer>() {
        @Override
        public boolean test(Integer num) {
            return !HdmiCecController.this.isAllocatedLocalDeviceAddress(num.intValue());
        }
    };
    private final Predicate<Integer> mSystemAudioAddressPredicate = new Predicate<Integer>() {
        @Override
        public boolean test(Integer num) {
            return HdmiUtils.getTypeFromAddress(num.intValue()) == 5;
        }
    };
    private final SparseArray<HdmiCecLocalDevice> mLocalDevices = new SparseArray<>();
    private final ArrayBlockingQueue<MessageHistoryRecord> mMessageHistory = new ArrayBlockingQueue<>(20);

    interface AllocateAddressCallback {
        void onAllocated(int i, int i2);
    }

    private static native int nativeAddLogicalAddress(long j, int i);

    private static native void nativeClearLogicalAddress(long j);

    private static native void nativeEnableAudioReturnChannel(long j, int i, boolean z);

    private static native int nativeGetPhysicalAddress(long j);

    private static native HdmiPortInfo[] nativeGetPortInfos(long j);

    private static native int nativeGetVendorId(long j);

    private static native int nativeGetVersion(long j);

    private static native long nativeInit(HdmiCecController hdmiCecController, MessageQueue messageQueue);

    private static native boolean nativeIsConnected(long j, int i);

    private static native int nativeSendCecCommand(long j, int i, int i2, byte[] bArr);

    private static native void nativeSetLanguage(long j, String str);

    private static native void nativeSetOption(long j, int i, boolean z);

    private HdmiCecController(HdmiControlService hdmiControlService) {
        this.mService = hdmiControlService;
    }

    static HdmiCecController create(HdmiControlService hdmiControlService) {
        HdmiCecController hdmiCecController = new HdmiCecController(hdmiControlService);
        long jNativeInit = nativeInit(hdmiCecController, hdmiControlService.getServiceLooper().getQueue());
        if (jNativeInit == 0) {
            return null;
        }
        hdmiCecController.init(jNativeInit);
        return hdmiCecController;
    }

    private void init(long j) {
        this.mIoHandler = new Handler(this.mService.getIoLooper());
        this.mControlHandler = new Handler(this.mService.getServiceLooper());
        this.mNativePtr = j;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void addLocalDevice(int i, HdmiCecLocalDevice hdmiCecLocalDevice) {
        assertRunOnServiceThread();
        this.mLocalDevices.put(i, hdmiCecLocalDevice);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void allocateLogicalAddress(final int i, final int i2, final AllocateAddressCallback allocateAddressCallback) {
        assertRunOnServiceThread();
        runOnIoThread(new Runnable() {
            @Override
            public void run() {
                HdmiCecController.this.handleAllocateLogicalAddress(i, i2, allocateAddressCallback);
            }
        });
    }

    @HdmiAnnotations.IoThreadOnly
    private void handleAllocateLogicalAddress(final int i, int i2, final AllocateAddressCallback allocateAddressCallback) {
        int i3;
        boolean z;
        assertRunOnIoThread();
        final int i4 = 15;
        if (i2 == 15) {
            i3 = 0;
            while (i3 < 16) {
                if (i == HdmiUtils.getTypeFromAddress(i3)) {
                    break;
                } else {
                    i3++;
                }
            }
            i3 = i2;
        } else {
            i3 = i2;
        }
        int i5 = 0;
        while (true) {
            if (i5 >= 16) {
                break;
            }
            int i6 = (i3 + i5) % 16;
            if (i6 != 15 && i == HdmiUtils.getTypeFromAddress(i6)) {
                int i7 = 0;
                while (true) {
                    if (i7 < 3) {
                        if (!sendPollMessage(i6, i6, 1)) {
                            i7++;
                        } else {
                            z = true;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    i4 = i6;
                    break;
                }
            }
            i5++;
        }
        HdmiLogger.debug("New logical address for device [%d]: [preferred:%d, assigned:%d]", Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i4));
        if (allocateAddressCallback != null) {
            runOnServiceThread(new Runnable() {
                @Override
                public void run() {
                    allocateAddressCallback.onAllocated(i, i4);
                }
            });
        }
    }

    private static byte[] buildBody(int i, byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length + 1];
        bArr2[0] = (byte) i;
        System.arraycopy(bArr, 0, bArr2, 1, bArr.length);
        return bArr2;
    }

    HdmiPortInfo[] getPortInfos() {
        return nativeGetPortInfos(this.mNativePtr);
    }

    HdmiCecLocalDevice getLocalDevice(int i) {
        return this.mLocalDevices.get(i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    int addLogicalAddress(int i) {
        assertRunOnServiceThread();
        if (HdmiUtils.isValidAddress(i)) {
            return nativeAddLogicalAddress(this.mNativePtr, i);
        }
        return 2;
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
    void setOption(int i, boolean z) {
        assertRunOnServiceThread();
        HdmiLogger.debug("setOption: [flag:%d, enabled:%b]", Integer.valueOf(i), Boolean.valueOf(z));
        nativeSetOption(this.mNativePtr, i, z);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setLanguage(String str) {
        assertRunOnServiceThread();
        if (!LanguageTag.isLanguage(str)) {
            return;
        }
        nativeSetLanguage(this.mNativePtr, str);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void enableAudioReturnChannel(int i, boolean z) {
        assertRunOnServiceThread();
        nativeEnableAudioReturnChannel(this.mNativePtr, i, z);
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isConnected(int i) {
        assertRunOnServiceThread();
        return nativeIsConnected(this.mNativePtr, i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void pollDevices(HdmiControlService.DevicePollingCallback devicePollingCallback, int i, int i2, int i3) {
        assertRunOnServiceThread();
        runDevicePolling(i, pickPollCandidates(i2), i3, devicePollingCallback, new ArrayList());
    }

    @HdmiAnnotations.ServiceThreadOnly
    List<HdmiCecLocalDevice> getLocalDeviceList() {
        assertRunOnServiceThread();
        return HdmiUtils.sparseArrayToList(this.mLocalDevices);
    }

    private List<Integer> pickPollCandidates(int i) {
        Predicate<Integer> predicate;
        if ((i & 3) == 2) {
            predicate = this.mSystemAudioAddressPredicate;
        } else {
            predicate = this.mRemoteDeviceAddressPredicate;
        }
        int i2 = i & 196608;
        LinkedList linkedList = new LinkedList();
        if (i2 == 65536) {
            for (int i3 = 0; i3 <= 14; i3++) {
                if (predicate.test(Integer.valueOf(i3))) {
                    linkedList.add(Integer.valueOf(i3));
                }
            }
        } else {
            for (int i4 = 14; i4 >= 0; i4--) {
                if (predicate.test(Integer.valueOf(i4))) {
                    linkedList.add(Integer.valueOf(i4));
                }
            }
        }
        return linkedList;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private boolean isAllocatedLocalDeviceAddress(int i) {
        assertRunOnServiceThread();
        for (int i2 = 0; i2 < this.mLocalDevices.size(); i2++) {
            if (this.mLocalDevices.valueAt(i2).isAddressOf(i)) {
                return true;
            }
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void runDevicePolling(final int i, final List<Integer> list, final int i2, final HdmiControlService.DevicePollingCallback devicePollingCallback, final List<Integer> list2) {
        assertRunOnServiceThread();
        if (list.isEmpty()) {
            if (devicePollingCallback != null) {
                HdmiLogger.debug("[P]:AllocatedAddress=%s", list2.toString());
                devicePollingCallback.onPollingFinished(list2);
                return;
            }
            return;
        }
        final Integer numRemove = list.remove(0);
        runOnIoThread(new Runnable() {
            @Override
            public void run() {
                if (HdmiCecController.this.sendPollMessage(i, numRemove.intValue(), i2)) {
                    list2.add(numRemove);
                }
                HdmiCecController.this.runOnServiceThread(new Runnable() {
                    @Override
                    public void run() {
                        HdmiCecController.this.runDevicePolling(i, list, i2, devicePollingCallback, list2);
                    }
                });
            }
        });
    }

    @HdmiAnnotations.IoThreadOnly
    private boolean sendPollMessage(int i, int i2, int i3) {
        assertRunOnIoThread();
        for (int i4 = 0; i4 < i3; i4++) {
            int iNativeSendCecCommand = nativeSendCecCommand(this.mNativePtr, i, i2, EMPTY_BODY);
            if (iNativeSendCecCommand == 0) {
                return true;
            }
            if (iNativeSendCecCommand != 1) {
                HdmiLogger.warning("Failed to send a polling message(%d->%d) with return code %d", Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(iNativeSendCecCommand));
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

    private boolean isAcceptableAddress(int i) {
        if (i == 15) {
            return true;
        }
        return isAllocatedLocalDeviceAddress(i);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void onReceiveCommand(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        if (isAcceptableAddress(hdmiCecMessage.getDestination()) && this.mService.handleCecCommand(hdmiCecMessage)) {
            return;
        }
        maySendFeatureAbortCommand(hdmiCecMessage, 0);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void maySendFeatureAbortCommand(HdmiCecMessage hdmiCecMessage, int i) {
        int opcode;
        assertRunOnServiceThread();
        int destination = hdmiCecMessage.getDestination();
        int source = hdmiCecMessage.getSource();
        if (destination == 15 || source == 15 || (opcode = hdmiCecMessage.getOpcode()) == 0) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildFeatureAbortCommand(destination, source, opcode, i));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void sendCommand(HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        sendCommand(hdmiCecMessage, null);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void sendCommand(final HdmiCecMessage hdmiCecMessage, final HdmiControlService.SendMessageCallback sendMessageCallback) {
        assertRunOnServiceThread();
        addMessageToHistory(false, hdmiCecMessage);
        runOnIoThread(new Runnable() {
            @Override
            public void run() {
                final int iNativeSendCecCommand;
                int i = 0;
                HdmiLogger.debug("[S]:" + hdmiCecMessage, new Object[0]);
                byte[] bArrBuildBody = HdmiCecController.buildBody(hdmiCecMessage.getOpcode(), hdmiCecMessage.getParams());
                while (true) {
                    iNativeSendCecCommand = HdmiCecController.nativeSendCecCommand(HdmiCecController.this.mNativePtr, hdmiCecMessage.getSource(), hdmiCecMessage.getDestination(), bArrBuildBody);
                    if (iNativeSendCecCommand == 0) {
                        break;
                    }
                    int i2 = i + 1;
                    if (i >= 1) {
                        break;
                    } else {
                        i = i2;
                    }
                }
                if (iNativeSendCecCommand != 0) {
                    Slog.w(HdmiCecController.TAG, "Failed to send " + hdmiCecMessage + " with errorCode=" + iNativeSendCecCommand);
                }
                if (sendMessageCallback != null) {
                    HdmiCecController.this.runOnServiceThread(new Runnable() {
                        @Override
                        public void run() {
                            sendMessageCallback.onSendCompleted(iNativeSendCecCommand);
                        }
                    });
                }
            }
        });
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleIncomingCecCommand(int i, int i2, byte[] bArr) {
        assertRunOnServiceThread();
        HdmiCecMessage hdmiCecMessageOf = HdmiCecMessageBuilder.of(i, i2, bArr);
        HdmiLogger.debug("[R]:" + hdmiCecMessageOf, new Object[0]);
        addMessageToHistory(true, hdmiCecMessageOf);
        onReceiveCommand(hdmiCecMessageOf);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleHotplug(int i, boolean z) {
        assertRunOnServiceThread();
        HdmiLogger.debug("Hotplug event:[port:%d, connected:%b]", Integer.valueOf(i), Boolean.valueOf(z));
        this.mService.onHotplug(i, z);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void addMessageToHistory(boolean z, HdmiCecMessage hdmiCecMessage) {
        assertRunOnServiceThread();
        MessageHistoryRecord messageHistoryRecord = new MessageHistoryRecord(z, hdmiCecMessage);
        if (!this.mMessageHistory.offer(messageHistoryRecord)) {
            this.mMessageHistory.poll();
            this.mMessageHistory.offer(messageHistoryRecord);
        }
    }

    void dump(IndentingPrintWriter indentingPrintWriter) {
        for (int i = 0; i < this.mLocalDevices.size(); i++) {
            indentingPrintWriter.println("HdmiCecLocalDevice #" + i + ":");
            indentingPrintWriter.increaseIndent();
            this.mLocalDevices.valueAt(i).dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.println("CEC message history:");
        indentingPrintWriter.increaseIndent();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Iterator<MessageHistoryRecord> it = this.mMessageHistory.iterator();
        while (it.hasNext()) {
            it.next().dump(indentingPrintWriter, simpleDateFormat);
        }
        indentingPrintWriter.decreaseIndent();
    }

    private final class MessageHistoryRecord {
        private final boolean mIsReceived;
        private final HdmiCecMessage mMessage;
        private final long mTime = System.currentTimeMillis();

        public MessageHistoryRecord(boolean z, HdmiCecMessage hdmiCecMessage) {
            this.mIsReceived = z;
            this.mMessage = hdmiCecMessage;
        }

        void dump(IndentingPrintWriter indentingPrintWriter, SimpleDateFormat simpleDateFormat) {
            indentingPrintWriter.print(this.mIsReceived ? "[R]" : "[S]");
            indentingPrintWriter.print(" time=");
            indentingPrintWriter.print(simpleDateFormat.format(new Date(this.mTime)));
            indentingPrintWriter.print(" message=");
            indentingPrintWriter.println(this.mMessage);
        }
    }
}
