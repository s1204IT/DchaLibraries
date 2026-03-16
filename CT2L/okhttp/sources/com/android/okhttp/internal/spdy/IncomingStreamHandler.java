package com.android.okhttp.internal.spdy;

import java.io.IOException;

public interface IncomingStreamHandler {
    public static final IncomingStreamHandler REFUSE_INCOMING_STREAMS = new IncomingStreamHandler() {
        @Override
        public void receive(SpdyStream stream) throws IOException {
            stream.close(ErrorCode.REFUSED_STREAM);
        }
    };

    void receive(SpdyStream spdyStream) throws IOException;
}
