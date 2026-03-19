package android.net.util;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.FileDescriptor;
import libcore.io.IoUtils;

public abstract class PacketReader {
    public static final int DEFAULT_RECV_BUF_SIZE = 2048;
    private static final int FD_EVENTS = 5;
    private static final int UNREGISTER_THIS_FD = 0;
    private FileDescriptor mFd;
    private final Handler mHandler;
    private final byte[] mPacket;
    private long mPacketsReceived;
    private final MessageQueue mQueue;

    protected abstract FileDescriptor createFd();

    protected static void closeFd(FileDescriptor fileDescriptor) {
        IoUtils.closeQuietly(fileDescriptor);
    }

    protected PacketReader(Handler handler) {
        this(handler, 2048);
    }

    protected PacketReader(Handler handler, int i) {
        this.mHandler = handler;
        this.mQueue = this.mHandler.getLooper().getQueue();
        this.mPacket = new byte[Math.max(i, 2048)];
    }

    public final void start() {
        if (onCorrectThread()) {
            createAndRegisterFd();
        } else {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PacketReader.lambda$start$0(this.f$0);
                }
            });
        }
    }

    public static void lambda$start$0(PacketReader packetReader) {
        packetReader.logError("start() called from off-thread", null);
        packetReader.createAndRegisterFd();
    }

    public final void stop() {
        if (onCorrectThread()) {
            unregisterAndDestroyFd();
        } else {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PacketReader.lambda$stop$1(this.f$0);
                }
            });
        }
    }

    public static void lambda$stop$1(PacketReader packetReader) {
        packetReader.logError("stop() called from off-thread", null);
        packetReader.unregisterAndDestroyFd();
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public final int recvBufSize() {
        return this.mPacket.length;
    }

    public final long numPacketsReceived() {
        return this.mPacketsReceived;
    }

    protected int readPacket(FileDescriptor fileDescriptor, byte[] bArr) throws Exception {
        return Os.read(fileDescriptor, bArr, 0, bArr.length);
    }

    protected void handlePacket(byte[] bArr, int i) {
    }

    protected void logError(String str, Exception exc) {
    }

    protected void onStart() {
    }

    protected void onStop() {
    }

    private void createAndRegisterFd() {
        if (this.mFd != null) {
            return;
        }
        try {
            this.mFd = createFd();
            if (this.mFd != null) {
                IoUtils.setBlocking(this.mFd, false);
            }
            if (this.mFd == null) {
                return;
            }
            this.mQueue.addOnFileDescriptorEventListener(this.mFd, 5, new MessageQueue.OnFileDescriptorEventListener() {
                @Override
                public int onFileDescriptorEvents(FileDescriptor fileDescriptor, int i) {
                    if (!PacketReader.this.isRunning() || !PacketReader.this.handleInput()) {
                        PacketReader.this.unregisterAndDestroyFd();
                        return 0;
                    }
                    return 5;
                }
            });
            onStart();
        } catch (Exception e) {
            logError("Failed to create socket: ", e);
            closeFd(this.mFd);
            this.mFd = null;
        }
    }

    private boolean isRunning() {
        return this.mFd != null && this.mFd.valid();
    }

    private boolean handleInput() {
        while (isRunning()) {
            try {
                int packet = readPacket(this.mFd, this.mPacket);
                if (packet < 1) {
                    if (isRunning()) {
                        logError("Socket closed, exiting", null);
                    }
                    return false;
                }
                this.mPacketsReceived++;
                try {
                    handlePacket(this.mPacket, packet);
                } catch (Exception e) {
                    logError("handlePacket error: ", e);
                    return false;
                }
            } catch (ErrnoException e2) {
                if (e2.errno == OsConstants.EAGAIN) {
                    return true;
                }
                if (e2.errno != OsConstants.EINTR) {
                    if (isRunning()) {
                        logError("readPacket error: ", e2);
                        return false;
                    }
                    return false;
                }
            } catch (Exception e3) {
                if (isRunning()) {
                    logError("readPacket error: ", e3);
                    return false;
                }
                return false;
            }
        }
        return false;
    }

    private void unregisterAndDestroyFd() {
        if (this.mFd == null) {
            return;
        }
        this.mQueue.removeOnFileDescriptorEventListener(this.mFd);
        closeFd(this.mFd);
        this.mFd = null;
        onStop();
    }

    private boolean onCorrectThread() {
        return this.mHandler.getLooper() == Looper.myLooper();
    }
}
