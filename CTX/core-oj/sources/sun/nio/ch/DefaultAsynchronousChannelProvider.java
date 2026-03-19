package sun.nio.ch;

import java.nio.channels.spi.AsynchronousChannelProvider;

public class DefaultAsynchronousChannelProvider {
    private DefaultAsynchronousChannelProvider() {
    }

    private static AsynchronousChannelProvider createProvider(String str) {
        try {
            try {
                return (AsynchronousChannelProvider) Class.forName(str).newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new AssertionError(e);
            }
        } catch (ClassNotFoundException e2) {
            throw new AssertionError(e2);
        }
    }

    public static AsynchronousChannelProvider create() {
        return createProvider("sun.nio.ch.LinuxAsynchronousChannelProvider");
    }
}
