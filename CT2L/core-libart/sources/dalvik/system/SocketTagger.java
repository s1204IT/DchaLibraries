package dalvik.system;

import java.io.FileDescriptor;
import java.net.Socket;
import java.net.SocketException;

public abstract class SocketTagger {
    private static SocketTagger tagger = new SocketTagger() {
        @Override
        public void tag(FileDescriptor socketDescriptor) throws SocketException {
        }

        @Override
        public void untag(FileDescriptor socketDescriptor) throws SocketException {
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

    public static synchronized void set(SocketTagger tagger2) {
        if (tagger2 == null) {
            throw new NullPointerException("tagger == null");
        }
        tagger = tagger2;
    }

    public static synchronized SocketTagger get() {
        return tagger;
    }
}
