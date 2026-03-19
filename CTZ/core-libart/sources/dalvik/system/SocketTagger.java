package dalvik.system;

import java.io.FileDescriptor;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

public abstract class SocketTagger {
    private static SocketTagger tagger = new SocketTagger() {
        @Override
        public void tag(FileDescriptor fileDescriptor) throws SocketException {
        }

        @Override
        public void untag(FileDescriptor fileDescriptor) throws SocketException {
        }
    };

    public abstract void tag(FileDescriptor fileDescriptor) throws SocketException;

    public abstract void untag(FileDescriptor fileDescriptor) throws SocketException;

    public final void tag(Socket socket) throws SocketException {
        if (!socket.isClosed()) {
            tag(socket.getFileDescriptor$());
        }
    }

    public final void untag(Socket socket) throws SocketException {
        if (!socket.isClosed()) {
            untag(socket.getFileDescriptor$());
        }
    }

    public final void tag(DatagramSocket datagramSocket) throws SocketException {
        if (!datagramSocket.isClosed()) {
            tag(datagramSocket.getFileDescriptor$());
        }
    }

    public final void untag(DatagramSocket datagramSocket) throws SocketException {
        if (!datagramSocket.isClosed()) {
            untag(datagramSocket.getFileDescriptor$());
        }
    }

    public static synchronized void set(SocketTagger socketTagger) {
        if (socketTagger == null) {
            throw new NullPointerException("tagger == null");
        }
        tagger = socketTagger;
    }

    public static synchronized SocketTagger get() {
        return tagger;
    }
}
