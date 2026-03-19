package com.android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.SystemProperties;
import android.util.Log;
import java.io.IOException;
import javax.obex.Authenticator;
import javax.obex.ServerSession;

public class ObexServerSockets {
    private static final int CREATE_RETRY_TIME = 10;
    private static final boolean D = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final int NUMBER_OF_SOCKET_TYPES = 2;
    private static final String STAG = "ObexServerSockets";
    private static volatile int sInstanceCounter;
    private final IObexConnectionHandler mConHandler;
    private final BluetoothServerSocket mL2capSocket;
    private SocketAcceptThread mL2capThread;
    private final BluetoothServerSocket mRfcommSocket;
    private SocketAcceptThread mRfcommThread;
    private final String mTag;

    private ObexServerSockets(IObexConnectionHandler iObexConnectionHandler, BluetoothServerSocket bluetoothServerSocket, BluetoothServerSocket bluetoothServerSocket2) {
        this.mConHandler = iObexConnectionHandler;
        this.mRfcommSocket = bluetoothServerSocket;
        this.mL2capSocket = bluetoothServerSocket2;
        StringBuilder sb = new StringBuilder();
        sb.append(STAG);
        int i = sInstanceCounter;
        sInstanceCounter = i + 1;
        sb.append(i);
        this.mTag = sb.toString();
    }

    public static ObexServerSockets create(IObexConnectionHandler iObexConnectionHandler) {
        return create(iObexConnectionHandler, -2, -2, true);
    }

    public static ObexServerSockets createInsecure(IObexConnectionHandler iObexConnectionHandler) {
        return create(iObexConnectionHandler, -2, -2, false);
    }

    public static ObexServerSockets createInsecureWithFixedChannels(IObexConnectionHandler iObexConnectionHandler, int i, int i2) {
        if (D) {
            Log.d(STAG, "createInsecureWithFixedChannels(rfcomm = " + i + ", l2capPsm = " + i2 + ")");
        }
        return create(iObexConnectionHandler, i, i2, false);
    }

    public static ObexServerSockets createWithFixedChannels(IObexConnectionHandler iObexConnectionHandler, int i, int i2) {
        if (D) {
            Log.d(STAG, "createWithFixedChannels(rfcomm = " + i + ", l2capPsm = " + i2 + ")");
        }
        return create(iObexConnectionHandler, i, i2, true);
    }

    private static ObexServerSockets create(IObexConnectionHandler iObexConnectionHandler, int i, int i2, boolean z) {
        BluetoothServerSocket bluetoothServerSocketListenUsingInsecureL2capOn;
        BluetoothServerSocket bluetoothServerSocketListenUsingRfcommOn;
        if (D) {
            Log.d(STAG, "create(rfcomm = " + i + ", l2capPsm = " + i2 + ")");
        }
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null) {
            throw new RuntimeException("No bluetooth adapter...");
        }
        int i3 = 0;
        boolean z2 = false;
        BluetoothServerSocket bluetoothServerSocket = null;
        BluetoothServerSocket bluetoothServerSocket2 = null;
        while (true) {
            if (i3 >= 10) {
                break;
            }
            if (bluetoothServerSocket == null) {
                if (z) {
                    try {
                        bluetoothServerSocketListenUsingRfcommOn = defaultAdapter.listenUsingRfcommOn(i);
                    } catch (IOException e) {
                        Log.e(STAG, "Error create ServerSockets ", e);
                        z2 = false;
                    }
                } else {
                    bluetoothServerSocketListenUsingRfcommOn = defaultAdapter.listenUsingInsecureRfcommOn(i);
                }
                bluetoothServerSocket = bluetoothServerSocketListenUsingRfcommOn;
                if (bluetoothServerSocket2 != null) {
                }
                BluetoothServerSocket bluetoothServerSocket3 = bluetoothServerSocketListenUsingInsecureL2capOn;
                z2 = true;
                bluetoothServerSocket2 = bluetoothServerSocket3;
                if (!z2) {
                }
            } else {
                if (bluetoothServerSocket2 != null) {
                    if (z) {
                        bluetoothServerSocketListenUsingInsecureL2capOn = defaultAdapter.listenUsingL2capOn(i2);
                    } else {
                        bluetoothServerSocketListenUsingInsecureL2capOn = defaultAdapter.listenUsingInsecureL2capOn(i2);
                    }
                } else {
                    bluetoothServerSocketListenUsingInsecureL2capOn = bluetoothServerSocket2;
                }
                BluetoothServerSocket bluetoothServerSocket32 = bluetoothServerSocketListenUsingInsecureL2capOn;
                z2 = true;
                bluetoothServerSocket2 = bluetoothServerSocket32;
                if (!z2) {
                    int state = defaultAdapter.getState();
                    if (state != 11 && state != 12) {
                        Log.w(STAG, "initServerSockets failed as BT is (being) turned off");
                        break;
                    }
                    try {
                        if (D) {
                            Log.v(STAG, "waiting 300 ms...");
                        }
                        Thread.sleep(300L);
                    } catch (InterruptedException e2) {
                        Log.e(STAG, "create() was interrupted");
                    }
                    i3++;
                } else {
                    break;
                }
            }
        }
        if (z2) {
            if (D) {
                Log.d(STAG, "Succeed to create listening sockets ");
            }
            ObexServerSockets obexServerSockets = new ObexServerSockets(iObexConnectionHandler, bluetoothServerSocket, bluetoothServerSocket2);
            obexServerSockets.startAccept();
            return obexServerSockets;
        }
        Log.e(STAG, "Error to create listening socket after 10 try");
        return null;
    }

    public int getRfcommChannel() {
        return this.mRfcommSocket.getChannel();
    }

    public int getL2capPsm() {
        return this.mL2capSocket.getChannel();
    }

    private void startAccept() {
        if (D) {
            Log.d(this.mTag, "startAccept()");
        }
        this.mRfcommThread = new SocketAcceptThread(this.mRfcommSocket);
        this.mRfcommThread.start();
        this.mL2capThread = new SocketAcceptThread(this.mL2capSocket);
        this.mL2capThread.start();
    }

    private synchronized boolean onConnect(BluetoothDevice bluetoothDevice, BluetoothSocket bluetoothSocket) {
        if (D) {
            Log.d(this.mTag, "onConnect() socket: " + bluetoothSocket);
        }
        return this.mConHandler.onConnect(bluetoothDevice, bluetoothSocket);
    }

    private synchronized void onAcceptFailed() {
        shutdown(false);
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null && defaultAdapter.getState() == 12) {
            Log.d(this.mTag, "onAcceptFailed() calling shutdown...");
            this.mConHandler.onAcceptFailed();
        }
    }

    public synchronized void shutdown(boolean z) {
        if (D) {
            Log.d(this.mTag, "shutdown(block = " + z + ")");
        }
        if (this.mRfcommThread != null) {
            this.mRfcommThread.shutdown();
        }
        if (this.mL2capThread != null) {
            this.mL2capThread.shutdown();
        }
        if (z) {
            while (true) {
                if (this.mRfcommThread == null && this.mL2capThread == null) {
                    break;
                }
                try {
                    if (this.mRfcommThread != null) {
                        this.mRfcommThread.join();
                        this.mRfcommThread = null;
                    }
                    if (this.mL2capThread != null) {
                        this.mL2capThread.join();
                        this.mL2capThread = null;
                    }
                } catch (InterruptedException e) {
                    Log.i(this.mTag, "shutdown() interrupted, continue waiting...", e);
                }
            }
        } else {
            this.mRfcommThread = null;
            this.mL2capThread = null;
        }
    }

    private class SocketAcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;
        private boolean mStopped = false;

        SocketAcceptThread(BluetoothServerSocket bluetoothServerSocket) {
            if (bluetoothServerSocket == null) {
                throw new IllegalArgumentException("serverSocket cannot be null");
            }
            this.mServerSocket = bluetoothServerSocket;
        }

        @Override
        public void run() {
            while (!this.mStopped) {
                try {
                    try {
                        if (ObexServerSockets.D) {
                            Log.d(ObexServerSockets.this.mTag, "Accepting socket connection...");
                        }
                        BluetoothSocket bluetoothSocketAccept = this.mServerSocket.accept();
                        if (ObexServerSockets.D) {
                            Log.d(ObexServerSockets.this.mTag, "Accepted socket connection from: " + this.mServerSocket);
                        }
                        if (bluetoothSocketAccept == null) {
                            Log.w(ObexServerSockets.this.mTag, "connSocket is null - reattempt accept");
                        } else {
                            BluetoothDevice remoteDevice = bluetoothSocketAccept.getRemoteDevice();
                            if (remoteDevice == null) {
                                Log.i(ObexServerSockets.this.mTag, "getRemoteDevice() = null - reattempt accept");
                                try {
                                    bluetoothSocketAccept.close();
                                } catch (IOException e) {
                                    Log.w(ObexServerSockets.this.mTag, "Error closing the socket. ignoring...", e);
                                }
                            } else if (!ObexServerSockets.this.onConnect(remoteDevice, bluetoothSocketAccept)) {
                                Log.i(ObexServerSockets.this.mTag, "RemoteDevice is invalid - creating ObexRejectServer.");
                                new ServerSession(new BluetoothObexTransport(bluetoothSocketAccept), new ObexRejectServer(211, bluetoothSocketAccept), (Authenticator) null);
                            }
                        }
                    } catch (IOException e2) {
                        if (!this.mStopped) {
                            Log.w(ObexServerSockets.this.mTag, "Accept exception for " + this.mServerSocket, e2);
                            ObexServerSockets.this.onAcceptFailed();
                        }
                        this.mStopped = true;
                    }
                } finally {
                    if (ObexServerSockets.D) {
                        Log.d(ObexServerSockets.this.mTag, "AcceptThread ended for: " + this.mServerSocket);
                    }
                }
            }
        }

        public void shutdown() {
            if (!this.mStopped) {
                this.mStopped = true;
                try {
                    this.mServerSocket.close();
                } catch (IOException e) {
                    if (ObexServerSockets.D) {
                        Log.d(ObexServerSockets.this.mTag, "Exception while thread shutdown:", e);
                    }
                }
            }
            if (!Thread.currentThread().equals(this)) {
                if (ObexServerSockets.D) {
                    Log.d(ObexServerSockets.this.mTag, "shutdown called from another thread - interrupt().");
                }
                interrupt();
            }
        }
    }
}
