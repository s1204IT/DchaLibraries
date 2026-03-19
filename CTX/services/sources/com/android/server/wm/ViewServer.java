package com.android.server.wm;

import android.util.Slog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.wm.WindowManagerService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ViewServer implements Runnable {
    private static final String COMMAND_PROTOCOL_VERSION = "PROTOCOL";
    private static final String COMMAND_SERVER_VERSION = "SERVER";
    private static final String COMMAND_WINDOW_MANAGER_AUTOLIST = "AUTOLIST";
    private static final String COMMAND_WINDOW_MANAGER_GET_FOCUS = "GET_FOCUS";
    private static final String COMMAND_WINDOW_MANAGER_LIST = "LIST";
    private static final String LOG_TAG = "WindowManager";
    private static final String VALUE_PROTOCOL_VERSION = "4";
    private static final String VALUE_SERVER_VERSION = "4";
    public static final int VIEW_SERVER_DEFAULT_PORT = 4939;
    private static final int VIEW_SERVER_MAX_CONNECTIONS = 10;
    private final int mPort;
    private ServerSocket mServer;
    private Thread mThread;
    private ExecutorService mThreadPool;
    private final WindowManagerService mWindowManager;

    ViewServer(WindowManagerService windowManagerService, int i) {
        this.mWindowManager = windowManagerService;
        this.mPort = i;
    }

    boolean start() throws IOException {
        if (this.mThread != null) {
            return false;
        }
        this.mServer = new ServerSocket(this.mPort, 10, InetAddress.getLocalHost());
        this.mThread = new Thread(this, "Remote View Server [port=" + this.mPort + "]");
        this.mThreadPool = Executors.newFixedThreadPool(10);
        this.mThread.start();
        return true;
    }

    boolean stop() {
        if (this.mThread != null) {
            this.mThread.interrupt();
            if (this.mThreadPool != null) {
                try {
                    this.mThreadPool.shutdownNow();
                } catch (SecurityException e) {
                    Slog.w("WindowManager", "Could not stop all view server threads");
                }
            }
            this.mThreadPool = null;
            this.mThread = null;
            try {
                this.mServer.close();
                this.mServer = null;
                return true;
            } catch (IOException e2) {
                Slog.w("WindowManager", "Could not close the view server");
                return false;
            }
        }
        return false;
    }

    boolean isRunning() {
        return this.mThread != null && this.mThread.isAlive();
    }

    @Override
    public void run() {
        while (Thread.currentThread() == this.mThread) {
            try {
                Socket socketAccept = this.mServer.accept();
                if (this.mThreadPool != null) {
                    this.mThreadPool.submit(new ViewServerWorker(socketAccept));
                } else {
                    try {
                        socketAccept.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e2) {
                Slog.w("WindowManager", "Connection error: ", e2);
            }
        }
    }

    private static boolean writeValue(Socket socket, String str) throws Throwable {
        BufferedWriter bufferedWriter;
        boolean z = false;
        BufferedWriter bufferedWriter2 = null;
        try {
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()), 8192);
            } catch (IOException e) {
            }
            try {
                bufferedWriter.write(str);
                bufferedWriter.write("\n");
                bufferedWriter.flush();
                bufferedWriter.close();
                z = true;
            } catch (Exception e2) {
                bufferedWriter2 = bufferedWriter;
                if (bufferedWriter2 != null) {
                    bufferedWriter2.close();
                }
                return z;
            } catch (Throwable th) {
                th = th;
                bufferedWriter2 = bufferedWriter;
                if (bufferedWriter2 != null) {
                    try {
                        bufferedWriter2.close();
                    } catch (IOException e3) {
                    }
                }
                throw th;
            }
        } catch (Exception e4) {
        } catch (Throwable th2) {
            th = th2;
        }
        return z;
    }

    class ViewServerWorker implements Runnable, WindowManagerService.WindowChangeListener {
        private Socket mClient;
        private boolean mNeedWindowListUpdate = false;
        private boolean mNeedFocusedWindowUpdate = false;

        public ViewServerWorker(Socket socket) {
            this.mClient = socket;
        }

        @Override
        public void run() throws Throwable {
            BufferedReader bufferedReader;
            Throwable th;
            IOException e;
            String strSubstring;
            try {
                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(this.mClient.getInputStream()), 1024);
                    try {
                        try {
                            String line = bufferedReader.readLine();
                            int iIndexOf = line.indexOf(32);
                            if (iIndexOf == -1) {
                                strSubstring = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                            } else {
                                String strSubstring2 = line.substring(0, iIndexOf);
                                strSubstring = line.substring(iIndexOf + 1);
                                line = strSubstring2;
                            }
                            boolean zWriteValue = (ViewServer.COMMAND_PROTOCOL_VERSION.equalsIgnoreCase(line) || ViewServer.COMMAND_SERVER_VERSION.equalsIgnoreCase(line)) ? ViewServer.writeValue(this.mClient, "4") : ViewServer.COMMAND_WINDOW_MANAGER_LIST.equalsIgnoreCase(line) ? ViewServer.this.mWindowManager.viewServerListWindows(this.mClient) : ViewServer.COMMAND_WINDOW_MANAGER_GET_FOCUS.equalsIgnoreCase(line) ? ViewServer.this.mWindowManager.viewServerGetFocusedWindow(this.mClient) : ViewServer.COMMAND_WINDOW_MANAGER_AUTOLIST.equalsIgnoreCase(line) ? windowManagerAutolistLoop() : ViewServer.this.mWindowManager.viewServerWindowCommand(this.mClient, line, strSubstring);
                            if (!zWriteValue) {
                                Slog.w("WindowManager", "An error occurred with the command: " + line);
                            }
                            try {
                                bufferedReader.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                        } catch (IOException e3) {
                            e = e3;
                            Slog.w("WindowManager", "Connection error: ", e);
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                } catch (IOException e4) {
                                    e4.printStackTrace();
                                }
                            }
                            if (this.mClient == null) {
                                return;
                            } else {
                                this.mClient.close();
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e5) {
                                e5.printStackTrace();
                            }
                        }
                        if (this.mClient != null) {
                            throw th;
                        }
                        try {
                            this.mClient.close();
                            throw th;
                        } catch (IOException e6) {
                            e6.printStackTrace();
                            throw th;
                        }
                    }
                } catch (IOException e7) {
                    e7.printStackTrace();
                    return;
                }
            } catch (IOException e8) {
                bufferedReader = null;
                e = e8;
            } catch (Throwable th3) {
                bufferedReader = null;
                th = th3;
                if (bufferedReader != null) {
                }
                if (this.mClient != null) {
                }
            }
            if (this.mClient != null) {
                this.mClient.close();
            }
        }

        @Override
        public void windowsChanged() {
            synchronized (this) {
                this.mNeedWindowListUpdate = true;
                notifyAll();
            }
        }

        @Override
        public void focusChanged() {
            synchronized (this) {
                this.mNeedFocusedWindowUpdate = true;
                notifyAll();
            }
        }

        private boolean windowManagerAutolistLoop() throws Throwable {
            BufferedWriter bufferedWriter;
            boolean z;
            boolean z2;
            ViewServer.this.mWindowManager.addWindowChangeListener(this);
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(this.mClient.getOutputStream()));
                while (!Thread.interrupted()) {
                    try {
                        synchronized (this) {
                            while (!this.mNeedWindowListUpdate && !this.mNeedFocusedWindowUpdate) {
                                wait();
                            }
                            z = false;
                            if (this.mNeedWindowListUpdate) {
                                this.mNeedWindowListUpdate = false;
                                z2 = true;
                            } else {
                                z2 = false;
                            }
                            if (this.mNeedFocusedWindowUpdate) {
                                this.mNeedFocusedWindowUpdate = false;
                                z = true;
                            }
                        }
                        if (z2) {
                            bufferedWriter.write("LIST UPDATE\n");
                            bufferedWriter.flush();
                        }
                        if (z) {
                            bufferedWriter.write("ACTION_FOCUS UPDATE\n");
                            bufferedWriter.flush();
                        }
                    } catch (Exception e) {
                        if (bufferedWriter != null) {
                            try {
                                bufferedWriter.close();
                            } catch (IOException e2) {
                            }
                        }
                        ViewServer.this.mWindowManager.removeWindowChangeListener(this);
                        return true;
                    } catch (Throwable th) {
                        th = th;
                        if (bufferedWriter != null) {
                            try {
                                bufferedWriter.close();
                            } catch (IOException e3) {
                            }
                        }
                        ViewServer.this.mWindowManager.removeWindowChangeListener(this);
                        throw th;
                    }
                }
                try {
                    bufferedWriter.close();
                } catch (IOException e4) {
                }
            } catch (Exception e5) {
                bufferedWriter = null;
            } catch (Throwable th2) {
                th = th2;
                bufferedWriter = null;
            }
            ViewServer.this.mWindowManager.removeWindowChangeListener(this);
            return true;
        }
    }
}
