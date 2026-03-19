package com.android.server.wm;

import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.WindowManagerService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
    private ArrayList<Socket> mSockets = new ArrayList<>();
    private Thread mThread;
    private ExecutorService mThreadPool;
    private final WindowManagerService mWindowManager;

    ViewServer(WindowManagerService windowManager, int port) {
        this.mWindowManager = windowManager;
        this.mPort = port;
    }

    boolean start() throws IOException {
        synchronized (this) {
            if (this.mThread != null) {
                return false;
            }
            this.mServer = new ServerSocket(this.mPort, 10, InetAddress.getLocalHost());
            this.mThread = new Thread(this, "Remote View Server [port=" + this.mPort + "]");
            this.mThreadPool = Executors.newFixedThreadPool(10);
            this.mThread.start();
            return true;
        }
    }

    boolean stop() {
        synchronized (this) {
            if (this.mThread != null) {
                this.mThread.interrupt();
                if (this.mThreadPool != null) {
                    try {
                        try {
                            this.mThreadPool.shutdownNow();
                            synchronized (this.mSockets) {
                                for (int i = 0; i < this.mSockets.size(); i++) {
                                    try {
                                        this.mSockets.get(i).close();
                                    } catch (IOException e) {
                                        Slog.w(LOG_TAG, "Could not close mSockets");
                                    }
                                }
                                this.mSockets.clear();
                            }
                        } catch (SecurityException e2) {
                            Slog.w(LOG_TAG, "Could not stop all view server threads");
                            synchronized (this.mSockets) {
                                for (int i2 = 0; i2 < this.mSockets.size(); i2++) {
                                    try {
                                        this.mSockets.get(i2).close();
                                    } catch (IOException e3) {
                                        Slog.w(LOG_TAG, "Could not close mSockets");
                                    }
                                }
                                this.mSockets.clear();
                            }
                        }
                    } catch (Throwable th) {
                        synchronized (this.mSockets) {
                            for (int i3 = 0; i3 < this.mSockets.size(); i3++) {
                                try {
                                    this.mSockets.get(i3).close();
                                } catch (IOException e4) {
                                    Slog.w(LOG_TAG, "Could not close mSockets");
                                }
                            }
                            this.mSockets.clear();
                            throw th;
                        }
                    }
                }
                this.mThreadPool = null;
                this.mThread = null;
                try {
                    this.mServer.close();
                    this.mServer = null;
                    return true;
                } catch (IOException e5) {
                    Slog.w(LOG_TAG, "Could not close the view server");
                }
            }
            return false;
        }
    }

    boolean isRunning() {
        if (this.mThread != null) {
            return this.mThread.isAlive();
        }
        return false;
    }

    @Override
    public void run() {
        while (Thread.currentThread() == this.mThread && this.mServer != null) {
            try {
                Socket client = this.mServer.accept();
                if (this.mThreadPool != null) {
                    synchronized (this.mSockets) {
                        this.mSockets.add(client);
                    }
                    this.mThreadPool.submit(new ViewServerWorker(client));
                } else {
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e2) {
                Slog.w(LOG_TAG, "Connection error: ", e2);
            }
        }
    }

    private static boolean writeValue(Socket client, String value) throws Throwable {
        BufferedWriter out = null;
        try {
            OutputStream clientStream = client.getOutputStream();
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(clientStream), PackageManagerService.DumpState.DUMP_PREFERRED_XML);
            try {
                out2.write(value);
                out2.write("\n");
                out2.flush();
                boolean result = true;
                if (out2 != null) {
                    try {
                        out2.close();
                    } catch (IOException e) {
                        result = false;
                    }
                }
                return result;
            } catch (Exception e2) {
                out = out2;
                if (out == null) {
                    return false;
                }
                try {
                    out.close();
                    return false;
                } catch (IOException e3) {
                    return false;
                }
            } catch (Throwable th) {
                th = th;
                out = out2;
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    class ViewServerWorker implements Runnable, WindowManagerService.WindowChangeListener {
        private Socket mClient;
        private boolean mNeedWindowListUpdate = false;
        private boolean mNeedFocusedWindowUpdate = false;

        public ViewServerWorker(Socket client) {
            this.mClient = client;
        }

        @Override
        public void run() throws Throwable {
            BufferedReader in;
            String command;
            String parameters;
            BufferedReader in2 = null;
            try {
                try {
                    in = new BufferedReader(new InputStreamReader(this.mClient.getInputStream()), 1024);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e) {
                e = e;
            }
            try {
                String request = in.readLine();
                int index = request.indexOf(32);
                if (index == -1) {
                    command = request;
                    parameters = "";
                } else {
                    command = request.substring(0, index);
                    parameters = request.substring(index + 1);
                }
                boolean result = (ViewServer.COMMAND_PROTOCOL_VERSION.equalsIgnoreCase(command) || ViewServer.COMMAND_SERVER_VERSION.equalsIgnoreCase(command)) ? ViewServer.writeValue(this.mClient, "4") : ViewServer.COMMAND_WINDOW_MANAGER_LIST.equalsIgnoreCase(command) ? ViewServer.this.mWindowManager.viewServerListWindows(this.mClient) : ViewServer.COMMAND_WINDOW_MANAGER_GET_FOCUS.equalsIgnoreCase(command) ? ViewServer.this.mWindowManager.viewServerGetFocusedWindow(this.mClient) : ViewServer.COMMAND_WINDOW_MANAGER_AUTOLIST.equalsIgnoreCase(command) ? windowManagerAutolistLoop() : ViewServer.this.mWindowManager.viewServerWindowCommand(this.mClient, command, parameters);
                if (!result) {
                    Slog.w(ViewServer.LOG_TAG, "An error occurred with the command: " + command);
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                if (this.mClient != null) {
                    try {
                        this.mClient.close();
                        synchronized (ViewServer.this.mSockets) {
                            ViewServer.this.mSockets.remove(this.mClient);
                        }
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                in2 = in;
            } catch (IOException e4) {
                e = e4;
                in2 = in;
                Slog.w(ViewServer.LOG_TAG, "Connection error: ", e);
                if (in2 != null) {
                    try {
                        in2.close();
                    } catch (IOException e5) {
                        e5.printStackTrace();
                    }
                }
                if (this.mClient != null) {
                    try {
                        this.mClient.close();
                        synchronized (ViewServer.this.mSockets) {
                            ViewServer.this.mSockets.remove(this.mClient);
                        }
                    } catch (IOException e6) {
                        e6.printStackTrace();
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                in2 = in;
                if (in2 != null) {
                    try {
                        in2.close();
                    } catch (IOException e7) {
                        e7.printStackTrace();
                    }
                }
                if (this.mClient != null) {
                    try {
                        this.mClient.close();
                        synchronized (ViewServer.this.mSockets) {
                            ViewServer.this.mSockets.remove(this.mClient);
                        }
                    } catch (IOException e8) {
                        e8.printStackTrace();
                    }
                }
                throw th;
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
            ViewServer.this.mWindowManager.addWindowChangeListener(this);
            BufferedWriter bufferedWriter = null;
            try {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(this.mClient.getOutputStream()));
                while (!Thread.interrupted()) {
                    try {
                        boolean needWindowListUpdate = false;
                        boolean needFocusedWindowUpdate = false;
                        synchronized (this) {
                            while (!this.mNeedWindowListUpdate && !this.mNeedFocusedWindowUpdate) {
                                wait();
                            }
                            if (this.mNeedWindowListUpdate) {
                                this.mNeedWindowListUpdate = false;
                                needWindowListUpdate = true;
                            }
                            if (this.mNeedFocusedWindowUpdate) {
                                this.mNeedFocusedWindowUpdate = false;
                                needFocusedWindowUpdate = true;
                            }
                        }
                        if (needWindowListUpdate) {
                            out.write("LIST UPDATE\n");
                            out.flush();
                        }
                        if (needFocusedWindowUpdate) {
                            out.write("ACTION_FOCUS UPDATE\n");
                            out.flush();
                        }
                    } catch (Exception e) {
                        bufferedWriter = out;
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
                        bufferedWriter = out;
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
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e4) {
                    }
                }
                ViewServer.this.mWindowManager.removeWindowChangeListener(this);
                return true;
            } catch (Exception e5) {
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }
}
