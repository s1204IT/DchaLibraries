package android.net.netlink;

import android.system.ErrnoException;
import android.system.NetlinkSocketAddress;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import libcore.io.IoUtils;

public class NetlinkSocket {
    public static void sendOneShotKernelMessage(int i, byte[] bArr) throws ErrnoException {
        String string;
        try {
            FileDescriptor fileDescriptorForProto = forProto(i);
            connectToKernel(fileDescriptorForProto);
            sendMessage(fileDescriptorForProto, bArr, 0, bArr.length, 300L);
            ByteBuffer byteBufferRecvMessage = recvMessage(fileDescriptorForProto, 8192, 300L);
            ?? r0 = NetlinkMessage.parse(byteBufferRecvMessage);
            if (r0 != 0 && (r0 instanceof NetlinkErrorMessage) && r0.getNlMsgError() != null) {
                int i2 = r0.getNlMsgError().error;
                if (i2 != 0) {
                    Log.e("NetlinkSocket", "Error in NetlinkSocket.sendOneShotKernelMessage, errmsg=" + r0.toString());
                    throw new ErrnoException(r0.toString(), Math.abs(i2));
                }
                IoUtils.closeQuietly(fileDescriptorForProto);
                return;
            }
            if (r0 == 0) {
                byteBufferRecvMessage.position(0);
                string = "raw bytes: " + NetlinkConstants.hexify(byteBufferRecvMessage);
            } else {
                string = r0.toString();
            }
            Log.e("NetlinkSocket", "Error in NetlinkSocket.sendOneShotKernelMessage, errmsg=" + string);
            throw new ErrnoException(string, OsConstants.EPROTO);
        } catch (InterruptedIOException e) {
            Log.e("NetlinkSocket", "Error in NetlinkSocket.sendOneShotKernelMessage", e);
            throw new ErrnoException("Error in NetlinkSocket.sendOneShotKernelMessage", OsConstants.ETIMEDOUT, e);
        } catch (SocketException e2) {
            Log.e("NetlinkSocket", "Error in NetlinkSocket.sendOneShotKernelMessage", e2);
            throw new ErrnoException("Error in NetlinkSocket.sendOneShotKernelMessage", OsConstants.EIO, e2);
        }
    }

    public static FileDescriptor forProto(int i) throws ErrnoException {
        FileDescriptor fileDescriptorSocket = Os.socket(OsConstants.AF_NETLINK, OsConstants.SOCK_DGRAM, i);
        Os.setsockoptInt(fileDescriptorSocket, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, 65536);
        return fileDescriptorSocket;
    }

    public static void connectToKernel(FileDescriptor fileDescriptor) throws SocketException, ErrnoException {
        Os.connect(fileDescriptor, new NetlinkSocketAddress(0, 0));
    }

    private static void checkTimeout(long j) {
        if (j < 0) {
            throw new IllegalArgumentException("Negative timeouts not permitted");
        }
    }

    public static ByteBuffer recvMessage(FileDescriptor fileDescriptor, int i, long j) throws ErrnoException, IllegalArgumentException, InterruptedIOException {
        checkTimeout(j);
        Os.setsockoptTimeval(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(j));
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(i);
        int i2 = Os.read(fileDescriptor, byteBufferAllocate);
        if (i2 == i) {
            Log.w("NetlinkSocket", "maximum read");
        }
        byteBufferAllocate.position(0);
        byteBufferAllocate.limit(i2);
        byteBufferAllocate.order(ByteOrder.nativeOrder());
        return byteBufferAllocate;
    }

    public static int sendMessage(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, long j) throws ErrnoException, IllegalArgumentException, InterruptedIOException {
        checkTimeout(j);
        Os.setsockoptTimeval(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(j));
        return Os.write(fileDescriptor, bArr, i, i2);
    }
}
