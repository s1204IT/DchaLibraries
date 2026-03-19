package mf.org.apache.xml.serialize;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import org.xml.sax.ContentHandler;

public interface Serializer {
    ContentHandler asContentHandler() throws IOException;

    void setOutputByteStream(OutputStream outputStream);

    void setOutputCharStream(Writer writer);
}
