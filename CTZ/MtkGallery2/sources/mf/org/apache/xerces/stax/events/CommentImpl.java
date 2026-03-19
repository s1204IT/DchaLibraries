package mf.org.apache.xerces.stax.events;

import java.io.IOException;
import java.io.Writer;
import mf.javax.xml.stream.Location;
import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.events.Comment;

public final class CommentImpl extends XMLEventImpl implements Comment {
    private final String fText;

    public CommentImpl(String text, Location location) {
        super(5, location);
        this.fText = text != null ? text : "";
    }

    @Override
    public String getText() {
        return this.fText;
    }

    @Override
    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        try {
            writer.write("<!--");
            writer.write(this.fText);
            writer.write("-->");
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }
}
