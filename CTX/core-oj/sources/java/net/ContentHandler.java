package java.net;

import java.io.IOException;

public abstract class ContentHandler {
    public abstract Object getContent(URLConnection uRLConnection) throws IOException;

    public Object getContent(URLConnection uRLConnection, Class[] clsArr) throws IOException {
        Object content = getContent(uRLConnection);
        for (Class cls : clsArr) {
            if (cls.isInstance(content)) {
                return content;
            }
        }
        return null;
    }
}
