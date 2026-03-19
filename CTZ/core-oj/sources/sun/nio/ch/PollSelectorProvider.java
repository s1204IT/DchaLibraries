package sun.nio.ch;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelector;

public class PollSelectorProvider extends SelectorProviderImpl {
    @Override
    public AbstractSelector openSelector() throws IOException {
        return new PollSelectorImpl(this);
    }
}
