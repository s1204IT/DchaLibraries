package gov.nist.javax.sip.stack;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;

public class HandshakeCompletedListenerImpl implements HandshakeCompletedListener {
    private HandshakeCompletedEvent handshakeCompletedEvent;
    private TLSMessageChannel tlsMessageChannel;

    public HandshakeCompletedListenerImpl(TLSMessageChannel tLSMessageChannel) {
        this.tlsMessageChannel = tLSMessageChannel;
        tLSMessageChannel.setHandshakeCompletedListener(this);
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        this.handshakeCompletedEvent = handshakeCompletedEvent;
    }

    public HandshakeCompletedEvent getHandshakeCompletedEvent() {
        return this.handshakeCompletedEvent;
    }
}
