package sun.nio.ch;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.IllegalChannelGroupException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class LinuxAsynchronousChannelProvider extends AsynchronousChannelProvider {
    private static volatile EPollPort defaultPort;

    private EPollPort defaultEventPort() throws IOException {
        if (defaultPort == null) {
            synchronized (LinuxAsynchronousChannelProvider.class) {
                if (defaultPort == null) {
                    defaultPort = new EPollPort(this, ThreadPool.getDefault()).start();
                }
            }
        }
        return defaultPort;
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int i, ThreadFactory threadFactory) throws IOException {
        return new EPollPort(this, ThreadPool.create(i, threadFactory)).start();
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executorService, int i) throws IOException {
        return new EPollPort(this, ThreadPool.wrap(executorService, i)).start();
    }

    private Port toPort(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException {
        if (asynchronousChannelGroup == null) {
            return defaultEventPort();
        }
        if (!(asynchronousChannelGroup instanceof EPollPort)) {
            throw new IllegalChannelGroupException();
        }
        return (Port) asynchronousChannelGroup;
    }

    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException {
        return new UnixAsynchronousServerSocketChannelImpl(toPort(asynchronousChannelGroup));
    }

    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException {
        return new UnixAsynchronousSocketChannelImpl(toPort(asynchronousChannelGroup));
    }
}
