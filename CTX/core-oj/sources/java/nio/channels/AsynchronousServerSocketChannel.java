package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.Future;

public abstract class AsynchronousServerSocketChannel implements AsynchronousChannel, NetworkChannel {
    private final AsynchronousChannelProvider provider;

    public abstract Future<AsynchronousSocketChannel> accept();

    public abstract <A> void accept(A a, CompletionHandler<AsynchronousSocketChannel, ? super A> completionHandler);

    public abstract AsynchronousServerSocketChannel bind(SocketAddress socketAddress, int i) throws IOException;

    @Override
    public abstract SocketAddress getLocalAddress() throws IOException;

    @Override
    public abstract <T> AsynchronousServerSocketChannel setOption(SocketOption<T> socketOption, T t) throws IOException;

    @Override
    public NetworkChannel setOption(SocketOption socketOption, Object obj) throws IOException {
        return setOption((SocketOption<Object>) socketOption, obj);
    }

    protected AsynchronousServerSocketChannel(AsynchronousChannelProvider asynchronousChannelProvider) {
        this.provider = asynchronousChannelProvider;
    }

    public final AsynchronousChannelProvider provider() {
        return this.provider;
    }

    public static AsynchronousServerSocketChannel open(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException {
        return (asynchronousChannelGroup == null ? AsynchronousChannelProvider.provider() : asynchronousChannelGroup.provider()).openAsynchronousServerSocketChannel(asynchronousChannelGroup);
    }

    public static AsynchronousServerSocketChannel open() throws IOException {
        return open(null);
    }

    @Override
    public final AsynchronousServerSocketChannel bind(SocketAddress socketAddress) throws IOException {
        return bind(socketAddress, 0);
    }
}
