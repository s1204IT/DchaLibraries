package com.android.bluetooth.opp;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

class TestTcpListener {
    private static final int ACCEPT_WAIT_TIMEOUT = 5000;
    public static final int DEFAULT_OPP_CHANNEL = 12;
    public static final int MSG_INCOMING_BTOPP_CONNECTION = 100;
    private static final String TAG = "BtOppRfcommListener";
    private int mBtOppRfcommChannel;
    private Handler mCallback;
    private volatile boolean mInterrupted;
    private Thread mSocketAcceptThread;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;

    TestTcpListener() {
        this(12);
    }

    TestTcpListener(int i) {
        this.mBtOppRfcommChannel = -1;
        this.mBtOppRfcommChannel = i;
    }

    public synchronized boolean start(Handler handler) {
        if (this.mSocketAcceptThread == null) {
            this.mCallback = handler;
            this.mSocketAcceptThread = new Thread(TAG) {
                ServerSocket mServerSocket;

                @Override
                public void run() {
                    if (TestTcpListener.D) {
                        Log.d(TestTcpListener.TAG, "RfcommSocket listen thread starting");
                    }
                    try {
                        if (TestTcpListener.V) {
                            Log.v(TestTcpListener.TAG, "Create server RfcommSocket on channel" + TestTcpListener.this.mBtOppRfcommChannel);
                        }
                        this.mServerSocket = new ServerSocket(6500, 1);
                    } catch (IOException e) {
                        Log.e(TestTcpListener.TAG, "Error listing on channel" + TestTcpListener.this.mBtOppRfcommChannel);
                        TestTcpListener.this.mInterrupted = true;
                    }
                    while (!TestTcpListener.this.mInterrupted) {
                        try {
                            this.mServerSocket.setSoTimeout(5000);
                            Socket socketAccept = this.mServerSocket.accept();
                            if (socketAccept == null) {
                                if (TestTcpListener.V) {
                                    Log.v(TestTcpListener.TAG, "incomming connection time out");
                                }
                            } else {
                                if (TestTcpListener.D) {
                                    Log.d(TestTcpListener.TAG, "RfcommSocket connected!");
                                }
                                Log.d(TestTcpListener.TAG, "remote addr is " + socketAccept.getRemoteSocketAddress());
                                TestTcpTransport testTcpTransport = new TestTcpTransport(socketAccept);
                                Message messageObtain = Message.obtain();
                                messageObtain.setTarget(TestTcpListener.this.mCallback);
                                messageObtain.what = 100;
                                messageObtain.obj = testTcpTransport;
                                messageObtain.sendToTarget();
                            }
                        } catch (SocketException e2) {
                            Log.e(TestTcpListener.TAG, "Error accept connection " + e2);
                        } catch (IOException e3) {
                            Log.e(TestTcpListener.TAG, "Error accept connection " + e3);
                        }
                        if (TestTcpListener.this.mInterrupted) {
                            Log.e(TestTcpListener.TAG, "socketAcceptThread thread was interrupted (2), exiting");
                        }
                    }
                    if (TestTcpListener.D) {
                        Log.d(TestTcpListener.TAG, "RfcommSocket listen thread finished");
                    }
                }
            };
            this.mInterrupted = false;
            this.mSocketAcceptThread.start();
        }
        return true;
    }

    public synchronized void stop() {
        if (this.mSocketAcceptThread != null) {
            if (D) {
                Log.d(TAG, "stopping Connect Thread");
            }
            this.mInterrupted = true;
            try {
                this.mSocketAcceptThread.interrupt();
                if (V) {
                    Log.v(TAG, "waiting for thread to terminate");
                }
                this.mSocketAcceptThread.join();
                this.mSocketAcceptThread = null;
                this.mCallback = null;
            } catch (InterruptedException e) {
                if (V) {
                    Log.v(TAG, "Interrupted waiting for Accept Thread to join");
                }
            }
        }
    }
}
