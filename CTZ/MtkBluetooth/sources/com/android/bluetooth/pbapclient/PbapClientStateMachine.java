package com.android.bluetooth.pbapclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.UserManager;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.pbapclient.PbapClientConnectionHandler;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.List;

final class PbapClientStateMachine extends StateMachine {
    static final int CONNECT_TIMEOUT = 10000;
    private static final boolean DBG = true;
    static final int DISCONNECT_TIMEOUT = 3000;
    static final int MSG_CONNECTION_CLOSED = 7;
    static final int MSG_CONNECTION_COMPLETE = 5;
    static final int MSG_CONNECTION_FAILED = 6;
    private static final int MSG_CONNECT_TIMEOUT = 3;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_DISCONNECT_TIMEOUT = 4;
    static final int MSG_RESUME_DOWNLOAD = 8;
    private static final int MSG_SDP_COMPLETE = 9;
    private static final String TAG = "PbapClientStateMachine";
    private State mConnected;
    private State mConnecting;
    private PbapClientConnectionHandler mConnectionHandler;
    private final BluetoothDevice mCurrentDevice;
    private State mDisconnected;
    private State mDisconnecting;
    private HandlerThread mHandlerThread;
    private final Object mLock;
    private int mMostRecentState;
    private PbapClientService mService;
    private UserManager mUserManager;

    PbapClientStateMachine(PbapClientService pbapClientService, BluetoothDevice bluetoothDevice) {
        super(TAG);
        this.mHandlerThread = null;
        this.mUserManager = null;
        this.mMostRecentState = 0;
        this.mService = pbapClientService;
        this.mCurrentDevice = bluetoothDevice;
        this.mLock = new Object();
        this.mUserManager = UserManager.get(this.mService);
        this.mDisconnected = new Disconnected();
        this.mConnecting = new Connecting();
        this.mDisconnecting = new Disconnecting();
        this.mConnected = new Connected();
        addState(this.mDisconnected);
        addState(this.mConnecting);
        addState(this.mDisconnecting);
        addState(this.mConnected);
        setInitialState(this.mConnecting);
    }

    class Disconnected extends State {
        Disconnected() {
        }

        public void enter() {
            Log.d(PbapClientStateMachine.TAG, "Enter Disconnected: " + PbapClientStateMachine.this.getCurrentMessage().what);
            PbapClientStateMachine.this.onConnectionStateChanged(PbapClientStateMachine.this.mCurrentDevice, PbapClientStateMachine.this.mMostRecentState, 0);
            PbapClientStateMachine.this.mMostRecentState = 0;
            PbapClientStateMachine.this.quit();
        }
    }

    class Connecting extends State {
        private SDPBroadcastReceiver mSdpReceiver;

        Connecting() {
        }

        public void enter() {
            Log.d(PbapClientStateMachine.TAG, "Enter Connecting: " + PbapClientStateMachine.this.getCurrentMessage().what);
            PbapClientStateMachine.this.onConnectionStateChanged(PbapClientStateMachine.this.mCurrentDevice, PbapClientStateMachine.this.mMostRecentState, 1);
            this.mSdpReceiver = new SDPBroadcastReceiver();
            this.mSdpReceiver.register();
            PbapClientStateMachine.this.mCurrentDevice.sdpSearch(BluetoothUuid.PBAP_PSE);
            PbapClientStateMachine.this.mMostRecentState = 1;
            PbapClientStateMachine.this.mHandlerThread = new HandlerThread("PBAP PCE handler", 10);
            PbapClientStateMachine.this.mHandlerThread.start();
            PbapClientStateMachine.this.mConnectionHandler = new PbapClientConnectionHandler.Builder().setLooper(PbapClientStateMachine.this.mHandlerThread.getLooper()).setContext(PbapClientStateMachine.this.mService).setClientSM(PbapClientStateMachine.this).setRemoteDevice(PbapClientStateMachine.this.mCurrentDevice).build();
            PbapClientStateMachine.this.sendMessageDelayed(3, 10000L);
        }

        public boolean processMessage(Message message) {
            Log.d(PbapClientStateMachine.TAG, "Processing MSG " + message.what + " from " + getName());
            switch (message.what) {
                case 2:
                    if ((message.obj instanceof BluetoothDevice) && message.obj.equals(PbapClientStateMachine.this.mCurrentDevice)) {
                        PbapClientStateMachine.this.removeMessages(3);
                        PbapClientStateMachine.this.transitionTo(PbapClientStateMachine.this.mDisconnecting);
                    }
                    return true;
                case 3:
                case 6:
                    PbapClientStateMachine.this.removeMessages(3);
                    PbapClientStateMachine.this.transitionTo(PbapClientStateMachine.this.mDisconnecting);
                    return true;
                case 4:
                case 7:
                case 8:
                default:
                    Log.w(PbapClientStateMachine.TAG, "Received unexpected message while Connecting");
                    return false;
                case 5:
                    PbapClientStateMachine.this.removeMessages(3);
                    PbapClientStateMachine.this.transitionTo(PbapClientStateMachine.this.mConnected);
                    return true;
                case 9:
                    PbapClientStateMachine.this.mConnectionHandler.obtainMessage(1, message.obj).sendToTarget();
                    return true;
            }
        }

        public void exit() {
            this.mSdpReceiver.unregister();
            this.mSdpReceiver = null;
        }

        private class SDPBroadcastReceiver extends BroadcastReceiver {
            private SDPBroadcastReceiver() {
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.v(PbapClientStateMachine.TAG, "onReceive" + action);
                if (action.equals("android.bluetooth.device.action.SDP_RECORD")) {
                    if (!((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE")).equals(PbapClientStateMachine.this.getDevice())) {
                        Log.w(PbapClientStateMachine.TAG, "SDP Record fetched for different device - Ignore");
                        return;
                    }
                    ParcelUuid parcelUuid = (ParcelUuid) intent.getParcelableExtra("android.bluetooth.device.extra.UUID");
                    Log.v(PbapClientStateMachine.TAG, "Received UUID: " + parcelUuid.toString());
                    Log.v(PbapClientStateMachine.TAG, "expected UUID: " + BluetoothUuid.PBAP_PSE.toString());
                    if (parcelUuid.equals(BluetoothUuid.PBAP_PSE)) {
                        PbapClientStateMachine.this.sendMessage(9, intent.getParcelableExtra("android.bluetooth.device.extra.SDP_RECORD"));
                    }
                }
            }

            public void register() {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.bluetooth.device.action.SDP_RECORD");
                PbapClientStateMachine.this.mService.registerReceiver(this, intentFilter);
            }

            public void unregister() {
                PbapClientStateMachine.this.mService.unregisterReceiver(this);
            }
        }
    }

    class Disconnecting extends State {
        Disconnecting() {
        }

        public void enter() {
            Log.d(PbapClientStateMachine.TAG, "Enter Disconnecting: " + PbapClientStateMachine.this.getCurrentMessage().what);
            PbapClientStateMachine.this.onConnectionStateChanged(PbapClientStateMachine.this.mCurrentDevice, PbapClientStateMachine.this.mMostRecentState, 3);
            PbapClientStateMachine.this.mMostRecentState = 3;
            PbapClientStateMachine.this.mConnectionHandler.obtainMessage(2).sendToTarget();
            PbapClientStateMachine.this.sendMessageDelayed(4, 3000L);
        }

        public boolean processMessage(Message message) {
            Log.d(PbapClientStateMachine.TAG, "Processing MSG " + message.what + " from " + getName());
            int i = message.what;
            if (i == 2) {
                PbapClientStateMachine.this.deferMessage(message);
                return true;
            }
            if (i != 4) {
                switch (i) {
                    case 7:
                        PbapClientStateMachine.this.removeMessages(4);
                        PbapClientStateMachine.this.mHandlerThread.quitSafely();
                        PbapClientStateMachine.this.transitionTo(PbapClientStateMachine.this.mDisconnected);
                        return true;
                    case 8:
                        return true;
                    default:
                        Log.w(PbapClientStateMachine.TAG, "Received unexpected message while Disconnecting");
                        return false;
                }
            }
            Log.w(PbapClientStateMachine.TAG, "Disconnect Timeout, Forcing");
            PbapClientStateMachine.this.mConnectionHandler.abort();
            return true;
        }
    }

    class Connected extends State {
        Connected() {
        }

        public void enter() {
            Log.d(PbapClientStateMachine.TAG, "Enter Connected: " + PbapClientStateMachine.this.getCurrentMessage().what);
            PbapClientStateMachine.this.onConnectionStateChanged(PbapClientStateMachine.this.mCurrentDevice, PbapClientStateMachine.this.mMostRecentState, 2);
            PbapClientStateMachine.this.mMostRecentState = 2;
            if (PbapClientStateMachine.this.mUserManager.isUserUnlocked()) {
                PbapClientStateMachine.this.mConnectionHandler.obtainMessage(3).sendToTarget();
            }
        }

        public boolean processMessage(Message message) {
            Log.d(PbapClientStateMachine.TAG, "Processing MSG " + message.what + " from " + getName());
            int i = message.what;
            if (i != 2) {
                if (i == 8) {
                    PbapClientStateMachine.this.mConnectionHandler.obtainMessage(3).sendToTarget();
                    return true;
                }
                Log.w(PbapClientStateMachine.TAG, "Received unexpected message while Connected");
                return false;
            }
            if ((message.obj instanceof BluetoothDevice) && ((BluetoothDevice) message.obj).equals(PbapClientStateMachine.this.mCurrentDevice)) {
                PbapClientStateMachine.this.transitionTo(PbapClientStateMachine.this.mDisconnecting);
                return true;
            }
            return true;
        }
    }

    private void onConnectionStateChanged(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (bluetoothDevice == null) {
            Log.w(TAG, "onConnectionStateChanged with invalid device");
            return;
        }
        if (i != i2 && i2 == 2) {
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.PBAP_CLIENT);
        }
        Log.d(TAG, "Connection state " + bluetoothDevice + ": " + i + "->" + i2);
        Intent intent = new Intent("android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED");
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i2);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        this.mService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    public void disconnect(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "Disconnect Request " + bluetoothDevice);
        sendMessage(2, bluetoothDevice);
    }

    public void resumeDownload() {
        sendMessage(8);
    }

    void doQuit() {
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
        }
        quitNow();
    }

    protected void onQuitting() {
        this.mService.cleanupDevice(this.mCurrentDevice);
    }

    public int getConnectionState() {
        IState currentState = getCurrentState();
        if (currentState instanceof Disconnected) {
            return 0;
        }
        if (currentState instanceof Connecting) {
            return 1;
        }
        if (currentState instanceof Connected) {
            return 2;
        }
        if (currentState instanceof Disconnecting) {
            return 3;
        }
        Log.w(TAG, "Unknown State");
        return 0;
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        int connectionState;
        BluetoothDevice device;
        synchronized (this.mLock) {
            connectionState = getConnectionState();
            device = getDevice();
        }
        ArrayList arrayList = new ArrayList();
        for (int i : iArr) {
            if (connectionState == i && device != null) {
                arrayList.add(device);
            }
        }
        return arrayList;
    }

    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return 0;
        }
        synchronized (this.mLock) {
            if (!bluetoothDevice.equals(this.mCurrentDevice)) {
                return 0;
            }
            return getConnectionState();
        }
    }

    public BluetoothDevice getDevice() {
        if (getCurrentState() instanceof Disconnected) {
            return null;
        }
        return this.mCurrentDevice;
    }

    Context getContext() {
        return this.mService;
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mCurrentDevice: " + this.mCurrentDevice);
        ProfileService.println(sb, "StateMachine: " + toString());
    }
}
