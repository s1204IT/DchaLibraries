package java.net;

import java.io.IOException;

class UnknownContentHandler extends ContentHandler {
    static final ContentHandler INSTANCE = new UnknownContentHandler();

    UnknownContentHandler() {
    }

    @Override
    public Object getContent(URLConnection uRLConnection) throws IOException {
        return uRLConnection.getInputStream();
    }
}
