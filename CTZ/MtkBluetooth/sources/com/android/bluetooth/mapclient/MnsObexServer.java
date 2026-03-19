package com.android.bluetooth.mapclient;

import android.util.Log;
import com.android.bluetooth.ObexServerSockets;
import com.android.bluetooth.opp.BluetoothShare;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ServerRequestHandler;

class MnsObexServer extends ServerRequestHandler {
    private static final byte[] MNS_TARGET = {-69, 88, 43, 65, 66, 12, 17, -37, -80, -34, 8, 0, 32, 12, -102, 102};
    private static final String TAG = "MnsObexServer";
    private static final String TYPE = "x-bt/MAP-event-report";
    private static final boolean VDBG = false;
    private final ObexServerSockets mObexServerSockets;
    private final WeakReference<MceStateMachine> mStateMachineReference;

    MnsObexServer(MceStateMachine mceStateMachine, ObexServerSockets obexServerSockets) {
        this.mStateMachineReference = new WeakReference<>(mceStateMachine);
        this.mObexServerSockets = obexServerSockets;
    }

    public int onConnect(HeaderSet headerSet, HeaderSet headerSet2) {
        try {
            if (!Arrays.equals((byte[]) headerSet.getHeader(70), MNS_TARGET)) {
                return 198;
            }
            headerSet2.setHeader(74, MNS_TARGET);
            return 160;
        } catch (IOException e) {
            return 208;
        }
    }

    public void onDisconnect(HeaderSet headerSet, HeaderSet headerSet2) {
    }

    public int onGet(Operation operation) {
        return BluetoothShare.STATUS_RUNNING;
    }

    public int onPut(Operation operation) {
        try {
            HeaderSet receivedHeader = operation.getReceivedHeader();
            String str = (String) receivedHeader.getHeader(66);
            ObexAppParameters obexAppParametersFromHeaderSet = ObexAppParameters.fromHeaderSet(receivedHeader);
            if (TYPE.equals(str) && obexAppParametersFromHeaderSet.exists((byte) 15)) {
                Byte.valueOf(obexAppParametersFromHeaderSet.getByte((byte) 15));
                EventReport eventReportFromStream = EventReport.fromStream(operation.openDataInputStream());
                operation.close();
                MceStateMachine mceStateMachine = this.mStateMachineReference.get();
                if (mceStateMachine != null) {
                    mceStateMachine.receiveEvent(eventReportFromStream);
                    return 160;
                }
                return 160;
            }
            return BluetoothShare.STATUS_RUNNING;
        } catch (IOException e) {
            Log.e(TAG, "I/O exception when handling PUT request", e);
            return 208;
        }
    }

    public int onAbort(HeaderSet headerSet, HeaderSet headerSet2) {
        return 209;
    }

    public int onSetPath(HeaderSet headerSet, HeaderSet headerSet2, boolean z, boolean z2) {
        return BluetoothShare.STATUS_RUNNING;
    }

    public void onClose() {
    }
}
