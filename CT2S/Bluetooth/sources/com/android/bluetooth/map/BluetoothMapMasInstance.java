package com.android.bluetooth.map;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;
import javax.obex.Authenticator;
import javax.obex.ServerSession;

public class BluetoothMapMasInstance {
    private static final boolean D = true;
    private static final int SDP_MAP_MSG_TYPE_EMAIL = 1;
    private static final int SDP_MAP_MSG_TYPE_MMS = 8;
    private static final int SDP_MAP_MSG_TYPE_SMS_CDMA = 4;
    private static final int SDP_MAP_MSG_TYPE_SMS_GSM = 2;
    private static final String TAG = "BluetoothMapMasInstance";
    private static final boolean V = false;
    private BluetoothMapEmailSettingsItem mAccount;
    private BluetoothAdapter mAdapter;
    private String mBaseEmailUri;
    private Context mContext;
    private boolean mEnableSmsMms;
    private volatile boolean mInterrupted;
    private BluetoothMapService mMapService;
    private int mMasInstanceId;
    BluetoothMapContentObserver mObserver;
    private Handler mServiceHandler;
    private SocketAcceptThread mAcceptThread = null;
    private ServerSession mServerSession = null;
    private BluetoothServerSocket mServerSocket = null;
    private BluetoothSocket mConnSocket = null;
    private BluetoothDevice mRemoteDevice = null;
    private BluetoothMnsObexClient mMnsClient = null;

    public BluetoothMapMasInstance(BluetoothMapService mapService, Context context, BluetoothMapEmailSettingsItem account, int masId, boolean enableSmsMms) {
        this.mServiceHandler = null;
        this.mMapService = null;
        this.mContext = null;
        this.mAccount = null;
        this.mBaseEmailUri = null;
        this.mMasInstanceId = -1;
        this.mEnableSmsMms = false;
        this.mMapService = mapService;
        this.mServiceHandler = mapService.getHandler();
        this.mContext = context;
        this.mAccount = account;
        if (account != null) {
            this.mBaseEmailUri = account.mBase_uri;
        }
        this.mMasInstanceId = masId;
        this.mEnableSmsMms = enableSmsMms;
        init();
    }

    public String toString() {
        return "MasId: " + this.mMasInstanceId + " Uri:" + this.mBaseEmailUri + " SMS/MMS:" + this.mEnableSmsMms;
    }

    private void init() {
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getMasId() {
        return this.mMasInstanceId;
    }

    private class SocketAcceptThread extends Thread {
        private boolean stopped;

        private SocketAcceptThread() {
            this.stopped = false;
        }

        @Override
        public void run() {
            if (BluetoothMapMasInstance.this.mServerSocket != null || BluetoothMapMasInstance.this.initSocket()) {
                while (!this.stopped) {
                    try {
                        Log.d(BluetoothMapMasInstance.TAG, "Accepting socket connection...");
                        BluetoothServerSocket serverSocket = BluetoothMapMasInstance.this.mServerSocket;
                        if (serverSocket == null) {
                            Log.w(BluetoothMapMasInstance.TAG, "mServerSocket is null");
                            return;
                        }
                        BluetoothMapMasInstance.this.mConnSocket = serverSocket.accept();
                        Log.d(BluetoothMapMasInstance.TAG, "Accepted socket connection...");
                        synchronized (BluetoothMapMasInstance.this) {
                            if (BluetoothMapMasInstance.this.mConnSocket == null) {
                                Log.w(BluetoothMapMasInstance.TAG, "mConnSocket is null");
                                return;
                            }
                            BluetoothMapMasInstance.this.mRemoteDevice = BluetoothMapMasInstance.this.mConnSocket.getRemoteDevice();
                            if (BluetoothMapMasInstance.this.mRemoteDevice != null) {
                                boolean isValid = BluetoothMapMasInstance.this.mMapService.onConnect(BluetoothMapMasInstance.this.mRemoteDevice, BluetoothMapMasInstance.this);
                                if (!isValid) {
                                    Log.i(BluetoothMapMasInstance.TAG, "RemoteDevice is invalid - closing.");
                                    BluetoothMapMasInstance.this.mConnSocket.close();
                                    BluetoothMapMasInstance.this.mConnSocket = null;
                                } else {
                                    this.stopped = true;
                                }
                            } else {
                                Log.i(BluetoothMapMasInstance.TAG, "getRemoteDevice() = null");
                                return;
                            }
                        }
                    } catch (IOException ex) {
                        this.stopped = true;
                        Log.v(BluetoothMapMasInstance.TAG, "Accept exception: (expected at shutdown)", ex);
                    }
                }
            }
        }

        void shutdown() {
            this.stopped = true;
            try {
            } catch (IOException e) {
                Log.d(BluetoothMapMasInstance.TAG, "Exception while thread shurdown:", e);
            } finally {
                BluetoothMapMasInstance.this.mServerSocket = null;
            }
            if (BluetoothMapMasInstance.this.mServerSocket != null) {
                BluetoothMapMasInstance.this.mServerSocket.close();
            }
            interrupt();
        }
    }

    public void startRfcommSocketListener() {
        Log.d(TAG, "Map Service startRfcommSocketListener");
        this.mInterrupted = false;
        if (this.mAcceptThread == null) {
            this.mAcceptThread = new SocketAcceptThread();
            this.mAcceptThread.setName("BluetoothMapAcceptThread masId=" + this.mMasInstanceId);
            this.mAcceptThread.start();
        }
    }

    private final boolean initSocket() {
        Log.d(TAG, "MAS initSocket()");
        boolean initSocketOK = false;
        int i = 0;
        while (true) {
            if (i < 10 && !this.mInterrupted) {
                initSocketOK = true;
                try {
                    String masId = String.format("%02x", Integer.valueOf(this.mMasInstanceId & 255));
                    String masName = "";
                    int messageTypeFlags = 0;
                    if (this.mEnableSmsMms) {
                        masName = "SMS/MMS";
                        messageTypeFlags = 0 | 14;
                    }
                    if (this.mBaseEmailUri != null) {
                        if (this.mEnableSmsMms) {
                            masName = masName + "/EMAIL";
                        } else {
                            masName = this.mAccount.getName();
                        }
                        messageTypeFlags |= 1;
                    }
                    String msgTypes = String.format("%02x", Integer.valueOf(messageTypeFlags & 255));
                    String sdpString = masId + msgTypes + masName;
                    this.mServerSocket = this.mAdapter.listenUsingRfcommWithServiceRecord(sdpString, BluetoothUuid.MAS.getUuid());
                } catch (IOException e) {
                    Log.e(TAG, "Error create RfcommServerSocket " + e.toString());
                    initSocketOK = false;
                }
                if (!initSocketOK && this.mAdapter != null) {
                    int state = this.mAdapter.getState();
                    if (state != 11 && state != 12) {
                        Log.w(TAG, "initServerSocket failed as BT is (being) turned off");
                        break;
                    }
                    try {
                        Thread.sleep(300L);
                    } catch (InterruptedException e2) {
                        Log.e(TAG, "socketAcceptThread thread was interrupted (3)");
                    }
                    i++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (this.mInterrupted) {
            initSocketOK = false;
            closeServerSocket();
        }
        if (!initSocketOK) {
            Log.e(TAG, "Error to create listening socket after 10 try");
        }
        return initSocketOK;
    }

    public boolean startObexServerSession(BluetoothMnsObexClient mnsClient) throws IOException, RemoteException {
        Log.d(TAG, "Map Service startObexServerSession masid = " + this.mMasInstanceId);
        if (this.mConnSocket != null) {
            if (this.mServerSession != null) {
                return true;
            }
            this.mMnsClient = mnsClient;
            this.mObserver = new BluetoothMapContentObserver(this.mContext, this.mMnsClient, this, this.mAccount, this.mEnableSmsMms);
            this.mObserver.init();
            BluetoothMapObexServer mapServer = new BluetoothMapObexServer(this.mServiceHandler, this.mContext, this.mObserver, this.mMasInstanceId, this.mAccount, this.mEnableSmsMms);
            BluetoothMapRfcommTransport transport = new BluetoothMapRfcommTransport(this.mConnSocket);
            this.mServerSession = new ServerSession(transport, mapServer, (Authenticator) null);
            Log.d(TAG, "    ServerSession started.");
            return true;
        }
        Log.d(TAG, "    No connection for this instance");
        return false;
    }

    public boolean handleSmsSendIntent(Context context, Intent intent) {
        if (this.mObserver != null) {
            return this.mObserver.handleSmsSendIntent(context, intent);
        }
        return false;
    }

    public boolean isStarted() {
        return this.mConnSocket != null;
    }

    public void shutdown() {
        Log.d(TAG, "MAP Service shutdown");
        if (this.mServerSession != null) {
            this.mServerSession.close();
            this.mServerSession = null;
        }
        if (this.mObserver != null) {
            this.mObserver.deinit();
            this.mObserver = null;
        }
        this.mInterrupted = true;
        if (this.mAcceptThread != null) {
            this.mAcceptThread.shutdown();
            try {
                this.mAcceptThread.join();
            } catch (InterruptedException e) {
            }
            this.mAcceptThread = null;
        }
        closeConnectionSocket();
    }

    public void restartObexServerSession() {
        Log.d(TAG, "MAP Service stopObexServerSession");
        shutdown();
        startRfcommSocketListener();
    }

    private final synchronized void closeServerSocket() {
        if (this.mServerSocket != null) {
            try {
                try {
                    this.mServerSocket.close();
                } catch (IOException ex) {
                    Log.e(TAG, "Close Server Socket error: " + ex);
                    this.mServerSocket = null;
                }
            } finally {
                this.mServerSocket = null;
            }
        }
    }

    private final synchronized void closeConnectionSocket() {
        try {
            if (this.mConnSocket != null) {
                try {
                    this.mConnSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Close Connection Socket error: " + e.toString());
                    this.mConnSocket = null;
                }
            }
        } finally {
            this.mConnSocket = null;
        }
    }
}
