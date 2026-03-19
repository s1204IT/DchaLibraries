package com.android.bluetooth.pbap;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.bluetooth.IObexConnectionHandler;
import com.android.bluetooth.ObexRejectServer;
import com.android.bluetooth.R;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.bluetooth.sap.SapService;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.vcard.VCardConfig;
import java.io.IOException;
import javax.obex.Authenticator;
import javax.obex.ServerSession;

class PbapStateMachine extends StateMachine {
    static final int AUTHORIZED = 1;
    static final int AUTH_CANCELLED = 8;
    static final int AUTH_KEY_INPUT = 7;
    static final int CREATE_NOTIFICATION = 5;
    private static final boolean DEBUG = true;
    static final int DISCONNECT = 3;
    private static final String PBAP_OBEX_NOTIFICATION_CHANNEL = "pbap_obex_notification_channel";
    static final int REJECTED = 2;
    static final int REMOVE_NOTIFICATION = 6;
    static final int REQUEST_PERMISSION = 4;
    private static final String TAG = "PbapStateMachine";
    private static final boolean VERBOSE = true;
    private BluetoothSocket mConnSocket;
    private final Connected mConnected;
    private final Finished mFinished;
    private IObexConnectionHandler mIObexConnectionHandler;
    private int mNotificationId;
    private BluetoothPbapAuthenticator mObexAuth;
    private BluetoothPbapObexServer mPbapServer;
    private PbapStateBase mPrevState;
    private BluetoothDevice mRemoteDevice;
    private ServerSession mServerSession;
    private BluetoothPbapService mService;
    private Handler mServiceHandler;
    private final WaitingForAuth mWaitingForAuth;

    private PbapStateMachine(BluetoothPbapService bluetoothPbapService, Looper looper, BluetoothDevice bluetoothDevice, BluetoothSocket bluetoothSocket, IObexConnectionHandler iObexConnectionHandler, Handler handler, int i) {
        super(TAG, looper);
        this.mWaitingForAuth = new WaitingForAuth();
        this.mFinished = new Finished();
        this.mConnected = new Connected();
        this.mService = bluetoothPbapService;
        this.mIObexConnectionHandler = iObexConnectionHandler;
        this.mRemoteDevice = bluetoothDevice;
        this.mServiceHandler = handler;
        this.mConnSocket = bluetoothSocket;
        this.mNotificationId = i;
        addState(this.mFinished);
        addState(this.mWaitingForAuth);
        addState(this.mConnected);
        setInitialState(this.mWaitingForAuth);
    }

    static PbapStateMachine make(BluetoothPbapService bluetoothPbapService, Looper looper, BluetoothDevice bluetoothDevice, BluetoothSocket bluetoothSocket, IObexConnectionHandler iObexConnectionHandler, Handler handler, int i) {
        PbapStateMachine pbapStateMachine = new PbapStateMachine(bluetoothPbapService, looper, bluetoothDevice, bluetoothSocket, iObexConnectionHandler, handler, i);
        pbapStateMachine.start();
        return pbapStateMachine;
    }

    BluetoothDevice getRemoteDevice() {
        return this.mRemoteDevice;
    }

    private abstract class PbapStateBase extends State {
        abstract int getConnectionStateInt();

        private PbapStateBase() {
        }

        public void enter() {
            if (!(this instanceof WaitingForAuth) && PbapStateMachine.this.mPrevState == null) {
                throw new IllegalStateException("mPrevState is null on entering initial state");
            }
            enforceValidConnectionStateTransition();
        }

        public void exit() {
            PbapStateMachine.this.mPrevState = this;
        }

        private void broadcastConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) {
            stateLogD("broadcastConnectionState " + bluetoothDevice + ": " + i + "->" + i2);
            Intent intent = new Intent("android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED");
            intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i);
            intent.putExtra("android.bluetooth.profile.extra.STATE", i2);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
            intent.addFlags(16777216);
            PbapStateMachine.this.mService.sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
        }

        void broadcastStateTransitions() {
            int connectionStateInt;
            if (PbapStateMachine.this.mPrevState != null) {
                connectionStateInt = PbapStateMachine.this.mPrevState.getConnectionStateInt();
            } else {
                connectionStateInt = 0;
            }
            if (getConnectionStateInt() != connectionStateInt) {
                stateLogD("connection state changed: " + PbapStateMachine.this.mRemoteDevice + ": " + PbapStateMachine.this.mPrevState + " -> " + this);
                broadcastConnectionState(PbapStateMachine.this.mRemoteDevice, connectionStateInt, getConnectionStateInt());
            }
        }

        private void enforceValidConnectionStateTransition() {
            boolean z = false;
            if (this != PbapStateMachine.this.mWaitingForAuth ? !(this != PbapStateMachine.this.mFinished ? this != PbapStateMachine.this.mConnected || (PbapStateMachine.this.mPrevState != PbapStateMachine.this.mFinished && PbapStateMachine.this.mPrevState != PbapStateMachine.this.mWaitingForAuth) : PbapStateMachine.this.mPrevState != PbapStateMachine.this.mConnected && PbapStateMachine.this.mPrevState != PbapStateMachine.this.mWaitingForAuth) : PbapStateMachine.this.mPrevState == null) {
                z = true;
            }
            if (!z) {
                throw new IllegalStateException("Invalid state transition from " + PbapStateMachine.this.mPrevState + " to " + this + " for device " + PbapStateMachine.this.mRemoteDevice);
            }
        }

        void stateLogD(String str) {
            PbapStateMachine.this.log(getName() + ": currentDevice=" + PbapStateMachine.this.mRemoteDevice + ", msg=" + str);
        }
    }

    class WaitingForAuth extends PbapStateBase {
        WaitingForAuth() {
            super();
        }

        @Override
        int getConnectionStateInt() {
            return 1;
        }

        @Override
        public void enter() {
            super.enter();
            broadcastStateTransitions();
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case 1:
                    PbapStateMachine.this.transitionTo(PbapStateMachine.this.mConnected);
                    break;
                case 2:
                    rejectConnection();
                    PbapStateMachine.this.transitionTo(PbapStateMachine.this.mFinished);
                    break;
                case 3:
                    PbapStateMachine.this.mServiceHandler.removeMessages(2, PbapStateMachine.this);
                    PbapStateMachine.this.mServiceHandler.obtainMessage(2, PbapStateMachine.this).sendToTarget();
                    PbapStateMachine.this.transitionTo(PbapStateMachine.this.mFinished);
                    break;
                case 4:
                    PbapStateMachine.this.mService.checkOrGetPhonebookPermission(PbapStateMachine.this);
                    break;
            }
            return true;
        }

        private void rejectConnection() {
            PbapStateMachine.this.mPbapServer = new BluetoothPbapObexServer(PbapStateMachine.this.mServiceHandler, PbapStateMachine.this.mService, PbapStateMachine.this);
            try {
                PbapStateMachine.this.mServerSession = new ServerSession(new BluetoothObexTransport(PbapStateMachine.this.mConnSocket), new ObexRejectServer(211, PbapStateMachine.this.mConnSocket), (Authenticator) null);
            } catch (IOException e) {
                Log.e(PbapStateMachine.TAG, "Caught exception starting OBEX reject server session" + e.toString());
            }
        }
    }

    class Finished extends PbapStateBase {
        Finished() {
            super();
        }

        @Override
        int getConnectionStateInt() {
            return 0;
        }

        @Override
        public void enter() {
            super.enter();
            if (PbapStateMachine.this.mServerSession != null) {
                PbapStateMachine.this.mServerSession.close();
                PbapStateMachine.this.mServerSession = null;
            }
            try {
                PbapStateMachine.this.mConnSocket.close();
                PbapStateMachine.this.mConnSocket = null;
            } catch (IOException e) {
                Log.e(PbapStateMachine.TAG, "Close Connection Socket error: " + e.toString());
            }
            PbapStateMachine.this.mServiceHandler.obtainMessage(SapService.MSG_RELEASE_WAKE_LOCK, PbapStateMachine.this).sendToTarget();
            broadcastStateTransitions();
        }
    }

    class Connected extends PbapStateBase {
        Connected() {
            super();
        }

        @Override
        int getConnectionStateInt() {
            return 2;
        }

        @Override
        public void enter() {
            try {
                startObexServerSession();
            } catch (IOException e) {
                Log.e(PbapStateMachine.TAG, "Caught exception starting OBEX server session" + e.toString());
            }
            broadcastStateTransitions();
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.PBAP);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 3) {
                stopObexServerSession();
                return true;
            }
            switch (i) {
                case 5:
                    createPbapNotification();
                    break;
                case 6:
                    PbapStateMachine.this.mService.sendBroadcast(new Intent("com.android.bluetooth.pbap.userconfirmtimeout"));
                    notifyAuthCancelled();
                    removePbapNotification(PbapStateMachine.this.mNotificationId);
                    break;
                case 7:
                    notifyAuthKeyInput((String) message.obj);
                    break;
                case 8:
                    notifyAuthCancelled();
                    break;
            }
            return true;
        }

        private void startObexServerSession() throws IOException {
            Log.v(PbapStateMachine.TAG, "Pbap Service startObexServerSession");
            PbapStateMachine.this.mServiceHandler.sendMessage(PbapStateMachine.this.mServiceHandler.obtainMessage(5004));
            PbapStateMachine.this.mPbapServer = new BluetoothPbapObexServer(PbapStateMachine.this.mServiceHandler, PbapStateMachine.this.mService, PbapStateMachine.this);
            synchronized (this) {
                PbapStateMachine.this.mObexAuth = new BluetoothPbapAuthenticator(PbapStateMachine.this);
                PbapStateMachine.this.mObexAuth.setChallenged(false);
                PbapStateMachine.this.mObexAuth.setCancelled(false);
            }
            try {
                BluetoothObexTransport bluetoothObexTransport = new BluetoothObexTransport(PbapStateMachine.this.mConnSocket);
                PbapStateMachine.this.mServerSession = new ServerSession(bluetoothObexTransport, PbapStateMachine.this.mPbapServer, PbapStateMachine.this.mObexAuth);
            } catch (NullPointerException e) {
                Log.e(PbapStateMachine.TAG, "mConnSocket is null, stopObexServerSession", e);
                stopObexServerSession();
            }
        }

        private void stopObexServerSession() {
            Log.v(PbapStateMachine.TAG, "Pbap Service stopObexServerSession");
            PbapStateMachine.this.transitionTo(PbapStateMachine.this.mFinished);
        }

        private void createPbapNotification() {
            NotificationManager notificationManager = (NotificationManager) PbapStateMachine.this.mService.getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION);
            notificationManager.createNotificationChannel(new NotificationChannel(PbapStateMachine.PBAP_OBEX_NOTIFICATION_CHANNEL, PbapStateMachine.this.mService.getString(R.string.pbap_notification_group), 4));
            Intent intent = new Intent();
            intent.setClass(PbapStateMachine.this.mService, BluetoothPbapActivity.class);
            intent.putExtra("com.android.bluetooth.pbap.device", PbapStateMachine.this.mRemoteDevice);
            intent.addFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            intent.setAction("com.android.bluetooth.pbap.authchall");
            Intent intent2 = new Intent();
            intent2.setClass(PbapStateMachine.this.mService, BluetoothPbapService.class);
            intent2.setAction("com.android.bluetooth.pbap.authcancelled");
            notificationManager.notify(PbapStateMachine.this.mNotificationId, new Notification.Builder(PbapStateMachine.this.mService, PbapStateMachine.PBAP_OBEX_NOTIFICATION_CHANNEL).setWhen(System.currentTimeMillis()).setContentTitle(PbapStateMachine.this.mService.getString(R.string.auth_notif_title)).setContentText(PbapStateMachine.this.mService.getString(R.string.auth_notif_message, new Object[]{PbapStateMachine.this.mRemoteDevice.getName()})).setSmallIcon(17301632).setTicker(PbapStateMachine.this.mService.getString(R.string.auth_notif_ticker)).setColor(PbapStateMachine.this.mService.getResources().getColor(android.R.color.car_colorPrimary, PbapStateMachine.this.mService.getTheme())).setFlag(16, true).setFlag(8, true).setContentIntent(PendingIntent.getActivity(PbapStateMachine.this.mService, 0, intent, 0)).setDeleteIntent(PendingIntent.getBroadcast(PbapStateMachine.this.mService, 0, intent2, 0)).setLocalOnly(true).build());
        }

        private void removePbapNotification(int i) {
            ((NotificationManager) PbapStateMachine.this.mService.getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION)).cancel(i);
        }

        private synchronized void notifyAuthCancelled() {
            PbapStateMachine.this.mObexAuth.setCancelled(true);
        }

        private synchronized void notifyAuthKeyInput(String str) {
            if (str != null) {
                try {
                    PbapStateMachine.this.mObexAuth.setSessionKey(str);
                } catch (Throwable th) {
                    throw th;
                }
            }
            PbapStateMachine.this.mObexAuth.setChallenged(true);
        }
    }

    synchronized int getConnectionState() {
        PbapStateBase currentState = getCurrentState();
        if (currentState == null) {
            return 0;
        }
        return currentState.getConnectionStateInt();
    }

    protected void log(String str) {
        super.log(str);
    }
}
