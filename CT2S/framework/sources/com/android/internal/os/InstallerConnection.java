package com.android.internal.os;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.BatteryStats;
import android.util.Slog;
import com.android.internal.telephony.PhoneConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class InstallerConnection {
    private static final boolean LOCAL_DEBUG = false;
    private static final String TAG = "InstallerConnection";
    private final byte[] buf = new byte[1024];
    private InputStream mIn;
    private OutputStream mOut;
    private LocalSocket mSocket;

    public synchronized String transact(String cmd) {
        String s;
        if (!connect()) {
            Slog.e(TAG, "connection failed");
            s = "-1";
        } else if (!writeCommand(cmd)) {
            Slog.e(TAG, "write command failed? reconnect!");
            if (!connect() || !writeCommand(cmd)) {
                s = "-1";
            } else {
                int replyLength = readReply();
                if (replyLength > 0) {
                    s = new String(this.buf, 0, replyLength);
                } else {
                    s = "-1";
                }
            }
        }
        return s;
    }

    public int execute(String cmd) {
        String res = transact(cmd);
        try {
            return Integer.parseInt(res);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int dexopt(String apkPath, int uid, boolean isPublic, String instructionSet) {
        return dexopt(apkPath, uid, isPublic, PhoneConstants.APN_TYPE_ALL, instructionSet, false);
    }

    public int dexopt(String apkPath, int uid, boolean isPublic, String pkgName, String instructionSet, boolean vmSafeMode) {
        StringBuilder builder = new StringBuilder("dexopt");
        builder.append(' ');
        builder.append(apkPath);
        builder.append(' ');
        builder.append(uid);
        builder.append(isPublic ? " 1" : " 0");
        builder.append(' ');
        builder.append(pkgName);
        builder.append(' ');
        builder.append(instructionSet);
        builder.append(' ');
        builder.append(vmSafeMode ? " 1" : " 0");
        return execute(builder.toString());
    }

    public int patchoat(String apkPath, int uid, boolean isPublic, String instructionSet) {
        return patchoat(apkPath, uid, isPublic, PhoneConstants.APN_TYPE_ALL, instructionSet);
    }

    public int patchoat(String apkPath, int uid, boolean isPublic, String pkgName, String instructionSet) {
        StringBuilder builder = new StringBuilder("patchoat");
        builder.append(' ');
        builder.append(apkPath);
        builder.append(' ');
        builder.append(uid);
        builder.append(isPublic ? " 1" : " 0");
        builder.append(' ');
        builder.append(pkgName);
        builder.append(' ');
        builder.append(instructionSet);
        return execute(builder.toString());
    }

    private boolean connect() {
        if (this.mSocket != null) {
            return true;
        }
        Slog.i(TAG, "connecting...");
        try {
            this.mSocket = new LocalSocket();
            LocalSocketAddress address = new LocalSocketAddress("installd", LocalSocketAddress.Namespace.RESERVED);
            this.mSocket.connect(address);
            this.mIn = this.mSocket.getInputStream();
            this.mOut = this.mSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            disconnect();
            return false;
        }
    }

    public void disconnect() {
        Slog.i(TAG, "disconnecting...");
        IoUtils.closeQuietly(this.mSocket);
        IoUtils.closeQuietly(this.mIn);
        IoUtils.closeQuietly(this.mOut);
        this.mSocket = null;
        this.mIn = null;
        this.mOut = null;
    }

    private boolean readFully(byte[] buffer, int len) {
        try {
            Streams.readFully(this.mIn, buffer, 0, len);
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "read exception");
            disconnect();
            return false;
        }
    }

    private int readReply() {
        if (!readFully(this.buf, 2)) {
            return -1;
        }
        int len = (this.buf[0] & BatteryStats.HistoryItem.CMD_NULL) | ((this.buf[1] & BatteryStats.HistoryItem.CMD_NULL) << 8);
        if (len < 1 || len > this.buf.length) {
            Slog.e(TAG, "invalid reply length (" + len + ")");
            disconnect();
            return -1;
        }
        if (readFully(this.buf, len)) {
            return len;
        }
        return -1;
    }

    private boolean writeCommand(String cmdString) {
        byte[] cmd = cmdString.getBytes();
        int len = cmd.length;
        if (len < 1 || len > this.buf.length) {
            return false;
        }
        this.buf[0] = (byte) (len & 255);
        this.buf[1] = (byte) ((len >> 8) & 255);
        try {
            this.mOut.write(this.buf, 0, 2);
            this.mOut.write(cmd, 0, len);
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "write error");
            disconnect();
            return false;
        }
    }
}
