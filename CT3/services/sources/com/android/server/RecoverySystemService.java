package com.android.server;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.IRecoverySystem;
import android.os.IRecoverySystemProgressListener;
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
    private static final int SOCKET_CONNECTION_MAX_RETRY = 30;
    private static final String TAG = "RecoverySystemService";
    private static final String UNCRYPT_SOCKET = "uncrypt";
    private Context mContext;

    public RecoverySystemService(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public void onStart() {
        publishBinderService("recovery", new BinderService(this, null));
    }

    private final class BinderService extends IRecoverySystem.Stub {
        BinderService(RecoverySystemService this$0, BinderService binderService) {
            this();
        }

        private BinderService() {
        }

        public boolean uncrypt(String filename, IRecoverySystemProgressListener listener) throws Throwable {
            FileWriter uncryptFile;
            RecoverySystemService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            RecoverySystem.UNCRYPT_PACKAGE_FILE.delete();
            Throwable th = null;
            FileWriter uncryptFile2 = null;
            try {
                uncryptFile = new FileWriter(RecoverySystem.UNCRYPT_PACKAGE_FILE);
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                uncryptFile.write(filename + "\n");
                if (uncryptFile != null) {
                    try {
                        try {
                            uncryptFile.close();
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } catch (IOException e) {
                        e = e;
                        Slog.e(RecoverySystemService.TAG, "IOException when writing \"" + RecoverySystem.UNCRYPT_PACKAGE_FILE + "\": ", e);
                        return false;
                    }
                }
                if (th != null) {
                    throw th;
                }
                SystemProperties.set("ctl.start", RecoverySystemService.UNCRYPT_SOCKET);
                LocalSocket socket = connectService();
                if (socket == null) {
                    Slog.e(RecoverySystemService.TAG, "Failed to connect to uncrypt socket");
                    return false;
                }
                DataInputStream dataInputStream = null;
                DataOutputStream dos = null;
                try {
                    try {
                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        try {
                            DataOutputStream dos2 = new DataOutputStream(socket.getOutputStream());
                            int lastStatus = Integer.MIN_VALUE;
                            while (true) {
                                try {
                                    int status = dis.readInt();
                                    if (status != lastStatus || lastStatus == Integer.MIN_VALUE) {
                                        lastStatus = status;
                                        if (status < 0 || status > 100) {
                                            break;
                                        }
                                        Slog.i(RecoverySystemService.TAG, "uncrypt read status: " + status);
                                        if (listener != null) {
                                            try {
                                                listener.onProgress(status);
                                            } catch (RemoteException e2) {
                                                Slog.w(RecoverySystemService.TAG, "RemoteException when posting progress");
                                            }
                                        }
                                        if (status == 100) {
                                            Slog.i(RecoverySystemService.TAG, "uncrypt successfully finished.");
                                            dos2.writeInt(0);
                                            IoUtils.closeQuietly(dis);
                                            IoUtils.closeQuietly(dos2);
                                            IoUtils.closeQuietly(socket);
                                            return true;
                                        }
                                    }
                                } catch (IOException e3) {
                                    e = e3;
                                    dos = dos2;
                                    dataInputStream = dis;
                                    Slog.e(RecoverySystemService.TAG, "IOException when reading status: ", e);
                                    IoUtils.closeQuietly(dataInputStream);
                                    IoUtils.closeQuietly(dos);
                                    IoUtils.closeQuietly(socket);
                                    return false;
                                } catch (Throwable th4) {
                                    th = th4;
                                    dos = dos2;
                                    dataInputStream = dis;
                                    IoUtils.closeQuietly(dataInputStream);
                                    IoUtils.closeQuietly(dos);
                                    IoUtils.closeQuietly(socket);
                                    throw th;
                                }
                            }
                        } catch (IOException e4) {
                            e = e4;
                            dataInputStream = dis;
                        } catch (Throwable th5) {
                            th = th5;
                            dataInputStream = dis;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                } catch (IOException e5) {
                    e = e5;
                }
            } catch (Throwable th7) {
                th = th7;
                uncryptFile2 = uncryptFile;
                try {
                    throw th;
                } catch (Throwable th8) {
                    th = th;
                    th = th8;
                    if (uncryptFile2 != null) {
                        try {
                            uncryptFile2.close();
                        } catch (Throwable th9) {
                            if (th == null) {
                                th = th9;
                            } else if (th != th9) {
                                th.addSuppressed(th9);
                            }
                        }
                    }
                    if (th == null) {
                        throw th;
                    }
                    throw th;
                }
            }
        }

        public boolean clearBcb() {
            return setupOrClearBcb(false, null);
        }

        public boolean setupBcb(String command) {
            return setupOrClearBcb(true, command);
        }

        private LocalSocket connectService() {
            LocalSocket socket = new LocalSocket();
            boolean done = false;
            int retry = 0;
            while (true) {
                if (retry >= 30) {
                    break;
                }
                try {
                    socket.connect(new LocalSocketAddress(RecoverySystemService.UNCRYPT_SOCKET, LocalSocketAddress.Namespace.RESERVED));
                    done = true;
                    break;
                } catch (IOException e) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e2) {
                        Slog.w(RecoverySystemService.TAG, "Interrupted: ", e2);
                    }
                    retry++;
                }
            }
            if (!done) {
                Slog.e(RecoverySystemService.TAG, "Timed out connecting to uncrypt socket");
                return null;
            }
            return socket;
        }

        private boolean setupOrClearBcb(boolean isSetup, String command) throws Throwable {
            RecoverySystemService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            if (isSetup) {
                SystemProperties.set("ctl.start", "setup-bcb");
            } else {
                SystemProperties.set("ctl.start", "clear-bcb");
            }
            LocalSocket socket = connectService();
            if (socket == null) {
                Slog.e(RecoverySystemService.TAG, "Failed to connect to uncrypt socket");
                return false;
            }
            DataInputStream dis = null;
            DataOutputStream dos = null;
            try {
                try {
                    DataInputStream dis2 = new DataInputStream(socket.getInputStream());
                    try {
                        DataOutputStream dos2 = new DataOutputStream(socket.getOutputStream());
                        if (isSetup) {
                            try {
                                dos2.writeInt(command.length());
                                dos2.writeBytes(command);
                                dos2.flush();
                            } catch (IOException e) {
                                e = e;
                                dos = dos2;
                                dis = dis2;
                                Slog.e(RecoverySystemService.TAG, "IOException when communicating with uncrypt: ", e);
                                IoUtils.closeQuietly(dis);
                                IoUtils.closeQuietly(dos);
                                IoUtils.closeQuietly(socket);
                                return false;
                            } catch (Throwable th) {
                                th = th;
                                dos = dos2;
                                dis = dis2;
                                IoUtils.closeQuietly(dis);
                                IoUtils.closeQuietly(dos);
                                IoUtils.closeQuietly(socket);
                                throw th;
                            }
                        }
                        int status = dis2.readInt();
                        dos2.writeInt(0);
                        if (status == 100) {
                            Slog.i(RecoverySystemService.TAG, "uncrypt " + (isSetup ? "setup" : "clear") + " bcb successfully finished.");
                            IoUtils.closeQuietly(dis2);
                            IoUtils.closeQuietly(dos2);
                            IoUtils.closeQuietly(socket);
                            return true;
                        }
                        Slog.e(RecoverySystemService.TAG, "uncrypt failed with status: " + status);
                        IoUtils.closeQuietly(dis2);
                        IoUtils.closeQuietly(dos2);
                        IoUtils.closeQuietly(socket);
                        return false;
                    } catch (IOException e2) {
                        e = e2;
                        dis = dis2;
                    } catch (Throwable th2) {
                        th = th2;
                        dis = dis2;
                    }
                } catch (IOException e3) {
                    e = e3;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }
}
