package sun.nio.ch;

import java.io.FileDescriptor;
import java.nio.channels.Pipe;
import java.nio.channels.spi.SelectorProvider;

class PipeImpl extends Pipe {
    private final Pipe.SinkChannel sink;
    private final Pipe.SourceChannel source;

    PipeImpl(SelectorProvider selectorProvider) {
        long jMakePipe = IOUtil.makePipe(true);
        FileDescriptor fileDescriptor = new FileDescriptor();
        IOUtil.setfdVal(fileDescriptor, (int) (jMakePipe >>> 32));
        this.source = new SourceChannelImpl(selectorProvider, fileDescriptor);
        FileDescriptor fileDescriptor2 = new FileDescriptor();
        IOUtil.setfdVal(fileDescriptor2, (int) jMakePipe);
        this.sink = new SinkChannelImpl(selectorProvider, fileDescriptor2);
    }

    @Override
    public Pipe.SourceChannel source() {
        return this.source;
    }

    @Override
    public Pipe.SinkChannel sink() {
        return this.sink;
    }
}
