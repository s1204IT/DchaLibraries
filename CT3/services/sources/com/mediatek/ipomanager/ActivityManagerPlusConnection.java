package com.mediatek.ipomanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Parcelable;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import com.android.internal.app.ShutdownManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class ActivityManagerPlusConnection {
    private static final String TAG = "ActivityManagerPlusConnection";
    private static boolean sBooting = false;
    private static ActivityManagerPlusConnection sInstance;
    private Context mContext;
    private final int BUFFER_SIZE = 4096;
    private boolean mProcessing = false;
    private ServerSocketThread mServerSocketThread = new ServerSocketThread();

    private ActivityManagerPlusConnection(Context context) {
        this.mContext = context;
    }

    public static synchronized ActivityManagerPlusConnection getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ActivityManagerPlusConnection(context);
        }
        return sInstance;
    }

    public static boolean inBooting() {
        return sBooting;
    }

    public void startSocketServer() {
        Slog.i(TAG, "startSocketServer");
        if (this.mServerSocketThread != null) {
            if (!this.mServerSocketThread.isAlive()) {
                Slog.i(TAG, "SocketServer is not running, start it!");
                this.mProcessing = true;
                this.mServerSocketThread.start();
            } else if (!this.mProcessing) {
                this.mProcessing = true;
            }
        }
    }

    public void stopSocketServer() {
        Slog.i(TAG, "stopSocketServer");
        if (this.mServerSocketThread == null) {
            return;
        }
        this.mServerSocketThread.stopRun();
    }

    private class ServerSocketThread extends Thread {
        private LocalServerSocket serverSocket;

        private ServerSocketThread() {
        }

        private void stopRun() {
            ActivityManagerPlusConnection.this.mProcessing = false;
        }

        @Override
        public void run() {
            try {
                ActivityManagerPlusConnection.this.mProcessing = true;
                this.serverSocket = new LocalServerSocket("com.mediatek.ipomanager.ActivityManagerPlusConnection");
                while (ActivityManagerPlusConnection.this.mProcessing) {
                    Slog.i(ActivityManagerPlusConnection.TAG, "wait for new client coming!");
                    try {
                        LocalSocket localSocketAccept = this.serverSocket.accept();
                        if (ActivityManagerPlusConnection.this.mProcessing) {
                            Slog.i(ActivityManagerPlusConnection.TAG, "new client coming!");
                            ActivityManagerPlusConnection.this.new InteractClientSocketThread(localSocketAccept).start();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        ActivityManagerPlusConnection.this.mProcessing = false;
                    }
                }
                if (this.serverSocket != null) {
                    try {
                        this.serverSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                Slog.i(ActivityManagerPlusConnection.TAG, "ServerSocketThread exit");
            } catch (IOException e3) {
                e3.printStackTrace();
                ActivityManagerPlusConnection.this.mProcessing = false;
            }
        }
    }

    private class InteractClientSocketThread extends Thread {
        static final String ACK = "ok";
        private LocalSocket interactClientSocket;
        private final Object mActionDoneSync = new Object();
        private boolean mActionDone = false;

        public InteractClientSocketThread(LocalSocket localSocket) {
            this.interactClientSocket = localSocket;
        }

        void actionDone() {
            synchronized (this.mActionDoneSync) {
                this.mActionDone = true;
                this.mActionDoneSync.notifyAll();
            }
        }

        @Override
        public void run() throws Throwable {
            InputStream inputStream;
            InputStream inputStream2;
            OutputStream outputStream;
            int i;
            OutputStream outputStream2 = null;
            String str = "";
            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    InteractClientSocketThread.this.actionDone();
                }
            };
            try {
                inputStream = this.interactClientSocket.getInputStream();
                try {
                    char[] cArr = new char[4096];
                    int i2 = new InputStreamReader(inputStream).read(cArr);
                    if (i2 != -1) {
                        String str2 = new String(cArr, 0, i2);
                        Slog.i(ActivityManagerPlusConnection.TAG, "Receive String from client: " + str2);
                        if (str2.equals("ACTION_PREBOOT_IPO")) {
                            boolean unused = ActivityManagerPlusConnection.sBooting = true;
                            ShutdownManager.getInstance().preRestoreStates(ActivityManagerPlusConnection.this.mContext);
                            ActivityManagerPlusConnection.this.mContext.sendOrderedBroadcastAsUser(new Intent("android.intent.action.ACTION_PREBOOT_IPO"), UserHandle.ALL, null, broadcastReceiver, null, 0, null, null);
                            str = "android.intent.action.ACTION_PREBOOT_IPO";
                        } else {
                            if (!str2.equals("ACTION_BOOT_IPO")) {
                                Slog.i(ActivityManagerPlusConnection.TAG, "unrecognized intent request: " + str2);
                                if (inputStream != null) {
                                    try {
                                        inputStream.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (0 != 0) {
                                    try {
                                        outputStream2.close();
                                    } catch (IOException e2) {
                                        e2.printStackTrace();
                                    }
                                }
                                try {
                                    this.interactClientSocket.close();
                                    return;
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                    return;
                                }
                            }
                            UserManager userManager = ActivityManagerPlus.getUserManager(ActivityManagerPlusConnection.this.mContext);
                            if (userManager != null) {
                                List users = userManager.getUsers();
                                for (int i3 = 0; i3 < users.size(); i3++) {
                                    Intent intent = new Intent("android.intent.action.ACTION_BOOT_IPO");
                                    intent.putExtra("android.intent.extra.user_handle", (Parcelable) users.get(i3));
                                    intent.addFlags(134217728);
                                    if (i3 != 0) {
                                        ActivityManagerPlusConnection.this.mContext.sendOrderedBroadcastAsUser(intent, new UserHandle(((UserInfo) users.get(i3)).id), null, null, null, 0, null, null);
                                    } else {
                                        ActivityManagerPlusConnection.this.mContext.sendOrderedBroadcastAsUser(intent, new UserHandle(((UserInfo) users.get(i3)).id), null, broadcastReceiver, null, 0, null, null);
                                    }
                                }
                                str = "android.intent.action.ACTION_BOOT_IPO";
                            } else {
                                Slog.e(ActivityManagerPlusConnection.TAG, "ActivityManagerPlus not ready");
                                str = "android.intent.action.ACTION_BOOT_IPO";
                            }
                        }
                    }
                    synchronized (this.mActionDoneSync) {
                        i = 0;
                        while (!this.mActionDone) {
                            try {
                                this.mActionDoneSync.wait(200L);
                                i++;
                            } catch (InterruptedException e4) {
                                Slog.i(ActivityManagerPlusConnection.TAG, "wait " + str + " but interrupted");
                                e4.printStackTrace();
                            }
                        }
                    }
                    Slog.i(ActivityManagerPlusConnection.TAG, str + " completed for " + ((((double) i) * 200.0d) / 1000.0d) + "s");
                    if (str.equals("android.intent.action.ACTION_BOOT_IPO")) {
                        boolean unused2 = ActivityManagerPlusConnection.sBooting = false;
                    }
                    outputStream = this.interactClientSocket.getOutputStream();
                } catch (IOException e5) {
                    e = e5;
                    outputStream = null;
                    inputStream2 = inputStream;
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e6) {
                e = e6;
                inputStream2 = null;
                outputStream = null;
            } catch (Throwable th2) {
                th = th2;
                inputStream = null;
            }
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                outputStreamWriter.write(ACK, 0, ACK.length());
                outputStreamWriter.flush();
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e7) {
                        e7.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e8) {
                        e8.printStackTrace();
                    }
                }
                try {
                    this.interactClientSocket.close();
                } catch (IOException e9) {
                    e9.printStackTrace();
                }
            } catch (IOException e10) {
                e = e10;
                inputStream2 = inputStream;
                try {
                    Slog.i(ActivityManagerPlusConnection.TAG, "transfer data error");
                    e.printStackTrace();
                    if (inputStream2 != null) {
                        try {
                            inputStream2.close();
                        } catch (IOException e11) {
                            e11.printStackTrace();
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e12) {
                            e12.printStackTrace();
                        }
                    }
                    try {
                        this.interactClientSocket.close();
                    } catch (IOException e13) {
                        e13.printStackTrace();
                    }
                } catch (Throwable th3) {
                    th = th3;
                    outputStream2 = outputStream;
                    inputStream = inputStream2;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e14) {
                            e14.printStackTrace();
                        }
                    }
                    if (outputStream2 != null) {
                        try {
                            outputStream2.close();
                        } catch (IOException e15) {
                            e15.printStackTrace();
                        }
                    }
                    try {
                        this.interactClientSocket.close();
                        throw th;
                    } catch (IOException e16) {
                        e16.printStackTrace();
                        throw th;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                outputStream2 = outputStream;
                if (inputStream != null) {
                }
                if (outputStream2 != null) {
                }
                this.interactClientSocket.close();
                throw th;
            }
        }
    }
}
