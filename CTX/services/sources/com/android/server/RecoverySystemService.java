package com.android.server;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.IRecoverySystem;
import android.os.IRecoverySystemProgressListener;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import libcore.io.IoUtils;

public final class RecoverySystemService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String INIT_SERVICE_CLEAR_BCB = "init.svc.clear-bcb";
    private static final String INIT_SERVICE_SETUP_BCB = "init.svc.setup-bcb";
    private static final String INIT_SERVICE_UNCRYPT = "init.svc.uncrypt";
    private static final int SOCKET_CONNECTION_MAX_RETRY = 30;
    private static final String TAG = "RecoverySystemService";
    private static final String UNCRYPT_SOCKET = "uncrypt";
    private static final Object sRequestLock = new Object();
    private Context mContext;

    public RecoverySystemService(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public void onStart() {
        publishBinderService("recovery", new BinderService());
    }

    private final class BinderService extends IRecoverySystem.Stub {
        private BinderService() {
        }

        public boolean uncrypt(String str, IRecoverySystemProgressListener iRecoverySystemProgressListener) {
            DataInputStream dataInputStream;
            DataOutputStream dataOutputStream;
            int i;
            synchronized (RecoverySystemService.sRequestLock) {
                Throwable th = null;
                DataInputStream dataInputStream2 = null;
                th = null;
                RecoverySystemService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
                if (!checkAndWaitForUncryptService()) {
                    Slog.e(RecoverySystemService.TAG, "uncrypt service is unavailable.");
                    return false;
                }
                RecoverySystem.UNCRYPT_PACKAGE_FILE.delete();
                try {
                    FileWriter fileWriter = new FileWriter(RecoverySystem.UNCRYPT_PACKAGE_FILE);
                    try {
                        try {
                            fileWriter.write(str + "\n");
                            fileWriter.close();
                            SystemProperties.set("ctl.start", RecoverySystemService.UNCRYPT_SOCKET);
                            LocalSocket localSocketConnectService = connectService();
                            if (localSocketConnectService == null) {
                                Slog.e(RecoverySystemService.TAG, "Failed to connect to uncrypt socket");
                                return false;
                            }
                            try {
                                dataInputStream = new DataInputStream(localSocketConnectService.getInputStream());
                                try {
                                    dataOutputStream = new DataOutputStream(localSocketConnectService.getOutputStream());
                                    int i2 = Integer.MIN_VALUE;
                                    while (true) {
                                        try {
                                            i = dataInputStream.readInt();
                                            if (i != i2 || i2 == Integer.MIN_VALUE) {
                                                if (i < 0 || i > 100) {
                                                    break;
                                                }
                                                Slog.i(RecoverySystemService.TAG, "uncrypt read status: " + i);
                                                if (iRecoverySystemProgressListener != null) {
                                                    try {
                                                        iRecoverySystemProgressListener.onProgress(i);
                                                    } catch (RemoteException e) {
                                                        Slog.w(RecoverySystemService.TAG, "RemoteException when posting progress");
                                                    }
                                                }
                                                if (i == 100) {
                                                    Slog.i(RecoverySystemService.TAG, "uncrypt successfully finished.");
                                                    dataOutputStream.writeInt(0);
                                                    IoUtils.closeQuietly(dataInputStream);
                                                    IoUtils.closeQuietly(dataOutputStream);
                                                    IoUtils.closeQuietly(localSocketConnectService);
                                                    return true;
                                                }
                                                i2 = i;
                                            }
                                        } catch (IOException e2) {
                                            e = e2;
                                            dataInputStream2 = dataInputStream;
                                            try {
                                                Slog.e(RecoverySystemService.TAG, "IOException when reading status: ", e);
                                                IoUtils.closeQuietly(dataInputStream2);
                                                IoUtils.closeQuietly(dataOutputStream);
                                                IoUtils.closeQuietly(localSocketConnectService);
                                                return false;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                dataInputStream = dataInputStream2;
                                                IoUtils.closeQuietly(dataInputStream);
                                                IoUtils.closeQuietly(dataOutputStream);
                                                IoUtils.closeQuietly(localSocketConnectService);
                                                throw th;
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                            IoUtils.closeQuietly(dataInputStream);
                                            IoUtils.closeQuietly(dataOutputStream);
                                            IoUtils.closeQuietly(localSocketConnectService);
                                            throw th;
                                        }
                                    }
                                    Slog.e(RecoverySystemService.TAG, "uncrypt failed with status: " + i);
                                    dataOutputStream.writeInt(0);
                                    IoUtils.closeQuietly(dataInputStream);
                                    IoUtils.closeQuietly(dataOutputStream);
                                    IoUtils.closeQuietly(localSocketConnectService);
                                    return false;
                                } catch (IOException e3) {
                                    e = e3;
                                    dataOutputStream = null;
                                } catch (Throwable th4) {
                                    th = th4;
                                    dataOutputStream = null;
                                }
                            } catch (IOException e4) {
                                e = e4;
                                dataOutputStream = null;
                            } catch (Throwable th5) {
                                th = th5;
                                dataInputStream = null;
                                dataOutputStream = null;
                            }
                        } finally {
                        }
                    } catch (Throwable th6) {
                        if (th != null) {
                            try {
                                fileWriter.close();
                            } catch (Throwable th7) {
                                th.addSuppressed(th7);
                            }
                        } else {
                            fileWriter.close();
                        }
                        throw th6;
                    }
                } catch (IOException e5) {
                    Slog.e(RecoverySystemService.TAG, "IOException when writing \"" + RecoverySystem.UNCRYPT_PACKAGE_FILE + "\":", e5);
                    return false;
                }
            }
        }

        public boolean clearBcb() {
            boolean z;
            synchronized (RecoverySystemService.sRequestLock) {
                z = setupOrClearBcb(false, null);
            }
            return z;
        }

        public boolean setupBcb(String str) {
            boolean z;
            synchronized (RecoverySystemService.sRequestLock) {
                z = setupOrClearBcb(true, str);
            }
            return z;
        }

        public void rebootRecoveryWithCommand(String str) {
            synchronized (RecoverySystemService.sRequestLock) {
                if (setupOrClearBcb(true, str)) {
                    ((PowerManager) RecoverySystemService.this.mContext.getSystemService("power")).reboot("recovery");
                }
            }
        }

        private boolean checkAndWaitForUncryptService() {
            for (int i = 0; i < 30; i++) {
                if (!("running".equals(SystemProperties.get(RecoverySystemService.INIT_SERVICE_UNCRYPT)) || "running".equals(SystemProperties.get(RecoverySystemService.INIT_SERVICE_SETUP_BCB)) || "running".equals(SystemProperties.get(RecoverySystemService.INIT_SERVICE_CLEAR_BCB)))) {
                    return true;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Slog.w(RecoverySystemService.TAG, "Interrupted:", e);
                }
            }
            return false;
        }

        private LocalSocket connectService() {
            LocalSocket localSocket = new LocalSocket();
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= 30) {
                    break;
                }
                try {
                    localSocket.connect(new LocalSocketAddress(RecoverySystemService.UNCRYPT_SOCKET, LocalSocketAddress.Namespace.RESERVED));
                    z = true;
                    break;
                } catch (IOException e) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e2) {
                        Slog.w(RecoverySystemService.TAG, "Interrupted:", e2);
                    }
                    i++;
                }
            }
            if (!z) {
                Slog.e(RecoverySystemService.TAG, "Timed out connecting to uncrypt socket");
                return null;
            }
            return localSocket;
        }

        private boolean setupOrClearBcb(boolean z, String str) throws Throwable {
            DataInputStream dataInputStream;
            DataOutputStream dataOutputStream;
            DataInputStream dataInputStream2 = null;
            RecoverySystemService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            if (!checkAndWaitForUncryptService()) {
                Slog.e(RecoverySystemService.TAG, "uncrypt service is unavailable.");
                return false;
            }
            if (z) {
                SystemProperties.set("ctl.start", "setup-bcb");
            } else {
                SystemProperties.set("ctl.start", "clear-bcb");
            }
            LocalSocket localSocketConnectService = connectService();
            if (localSocketConnectService == null) {
                Slog.e(RecoverySystemService.TAG, "Failed to connect to uncrypt socket");
                return false;
            }
            try {
                dataInputStream = new DataInputStream(localSocketConnectService.getInputStream());
                try {
                    dataOutputStream = new DataOutputStream(localSocketConnectService.getOutputStream());
                    if (z) {
                        try {
                            byte[] bytes = str.getBytes("UTF-8");
                            dataOutputStream.writeInt(bytes.length);
                            dataOutputStream.write(bytes, 0, bytes.length);
                            dataOutputStream.flush();
                        } catch (IOException e) {
                            e = e;
                            dataInputStream2 = dataInputStream;
                            try {
                                Slog.e(RecoverySystemService.TAG, "IOException when communicating with uncrypt:", e);
                                IoUtils.closeQuietly(dataInputStream2);
                                IoUtils.closeQuietly(dataOutputStream);
                                IoUtils.closeQuietly(localSocketConnectService);
                                return false;
                            } catch (Throwable th) {
                                th = th;
                                dataInputStream = dataInputStream2;
                                IoUtils.closeQuietly(dataInputStream);
                                IoUtils.closeQuietly(dataOutputStream);
                                IoUtils.closeQuietly(localSocketConnectService);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            IoUtils.closeQuietly(dataInputStream);
                            IoUtils.closeQuietly(dataOutputStream);
                            IoUtils.closeQuietly(localSocketConnectService);
                            throw th;
                        }
                    }
                    int i = dataInputStream.readInt();
                    dataOutputStream.writeInt(0);
                    if (i != 100) {
                        Slog.e(RecoverySystemService.TAG, "uncrypt failed with status: " + i);
                        IoUtils.closeQuietly(dataInputStream);
                        IoUtils.closeQuietly(dataOutputStream);
                        IoUtils.closeQuietly(localSocketConnectService);
                        return false;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("uncrypt ");
                    sb.append(z ? "setup" : "clear");
                    sb.append(" bcb successfully finished.");
                    Slog.i(RecoverySystemService.TAG, sb.toString());
                    IoUtils.closeQuietly(dataInputStream);
                    IoUtils.closeQuietly(dataOutputStream);
                    IoUtils.closeQuietly(localSocketConnectService);
                    return true;
                } catch (IOException e2) {
                    e = e2;
                    dataOutputStream = null;
                } catch (Throwable th3) {
                    th = th3;
                    dataOutputStream = null;
                }
            } catch (IOException e3) {
                e = e3;
                dataOutputStream = null;
            } catch (Throwable th4) {
                th = th4;
                dataInputStream = null;
                dataOutputStream = null;
            }
        }
    }
}
