package mf.org.apache.xerces.stax.events;

import java.io.IOException;
import java.io.Writer;
import mf.javax.xml.stream.Location;
import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.events.Characters;
import mf.org.apache.xerces.util.XMLChar;

public final class CharactersImpl extends XMLEventImpl implements Characters {
    private final String fData;

    public CharactersImpl(String data, int eventType, Location location) {
        super(eventType, location);
        this.fData = data != null ? data : "";
    }

    @Override
    public String getData() {
        return this.fData;
    }

    public boolean isWhiteSpace() {
        int length = this.fData != null ? this.fData.length() : 0;
        if (length == 0) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!XMLChar.isSpace(this.fData.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean isCData() {
        return 12 == getEventType();
    }

    public boolean isIgnorableWhiteSpace() {
        return 6 == getEventType();
    }

    @Override
    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        try {
            writer.write(this.fData);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }
}
