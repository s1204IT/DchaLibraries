package android.net.ip;

import android.net.NetworkUtils;
import android.net.util.ConnectivityPacketSummary;
import android.net.util.InterfaceParams;
import android.net.util.PacketReader;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import libcore.util.HexEncoding;

public class ConnectivityPacketTracker {
    private static final boolean DBG = false;
    private static final String MARK_NAMED_START = "--- START (%s) ---";
    private static final String MARK_NAMED_STOP = "--- STOP (%s) ---";
    private static final String MARK_START = "--- START ---";
    private static final String MARK_STOP = "--- STOP ---";
    private static final String TAG = ConnectivityPacketTracker.class.getSimpleName();
    private String mDisplayName;
    private final LocalLog mLog;
    private final PacketReader mPacketListener;
    private boolean mRunning;
    private final String mTag;

    public ConnectivityPacketTracker(Handler handler, InterfaceParams interfaceParams, LocalLog localLog) {
        if (interfaceParams == null) {
            throw new IllegalArgumentException("null InterfaceParams");
        }
        this.mTag = TAG + "." + interfaceParams.name;
        this.mLog = localLog;
        this.mPacketListener = new PacketListener(handler, interfaceParams);
    }

    public void start(String str) {
        this.mRunning = true;
        this.mDisplayName = str;
        this.mPacketListener.start();
    }

    public void stop() {
        this.mPacketListener.stop();
        this.mRunning = false;
        this.mDisplayName = null;
    }

    private final class PacketListener extends PacketReader {
        private final InterfaceParams mInterface;

        PacketListener(Handler handler, InterfaceParams interfaceParams) {
            super(handler, interfaceParams.defaultMtu);
            this.mInterface = interfaceParams;
        }

        @Override
        protected FileDescriptor createFd() {
            FileDescriptor fileDescriptorSocket;
            try {
                fileDescriptorSocket = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, 0);
            } catch (ErrnoException | IOException e) {
                e = e;
                fileDescriptorSocket = null;
            }
            try {
                NetworkUtils.attachControlPacketFilter(fileDescriptorSocket, OsConstants.ARPHRD_ETHER);
                Os.bind(fileDescriptorSocket, new PacketSocketAddress((short) OsConstants.ETH_P_ALL, this.mInterface.index));
                return fileDescriptorSocket;
            } catch (ErrnoException | IOException e2) {
                e = e2;
                logError("Failed to create packet tracking socket: ", e);
                closeFd(fileDescriptorSocket);
                return null;
            }
        }

        @Override
        protected void handlePacket(byte[] bArr, int i) {
            String strSummarize = ConnectivityPacketSummary.summarize(this.mInterface.macAddr, bArr, i);
            if (strSummarize == null) {
                return;
            }
            addLogEntry(strSummarize + "\n[" + new String(HexEncoding.encode(bArr, 0, i)) + "]");
        }

        @Override
        protected void onStart() {
            String str;
            if (TextUtils.isEmpty(ConnectivityPacketTracker.this.mDisplayName)) {
                str = ConnectivityPacketTracker.MARK_START;
            } else {
                str = String.format(ConnectivityPacketTracker.MARK_NAMED_START, ConnectivityPacketTracker.this.mDisplayName);
            }
            ConnectivityPacketTracker.this.mLog.log(str);
        }

        @Override
        protected void onStop() {
            String str;
            if (TextUtils.isEmpty(ConnectivityPacketTracker.this.mDisplayName)) {
                str = ConnectivityPacketTracker.MARK_STOP;
            } else {
                str = String.format(ConnectivityPacketTracker.MARK_NAMED_STOP, ConnectivityPacketTracker.this.mDisplayName);
            }
            if (!ConnectivityPacketTracker.this.mRunning) {
                str = str + " (packet listener stopped unexpectedly)";
            }
            ConnectivityPacketTracker.this.mLog.log(str);
        }

        @Override
        protected void logError(String str, Exception exc) {
            Log.e(ConnectivityPacketTracker.this.mTag, str, exc);
            addLogEntry(str + exc);
        }

        private void addLogEntry(String str) {
            ConnectivityPacketTracker.this.mLog.log(str);
        }
    }
}
