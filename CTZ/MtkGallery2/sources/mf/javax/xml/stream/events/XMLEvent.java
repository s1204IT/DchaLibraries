package mf.javax.xml.stream.events;

import java.io.Writer;
import mf.javax.xml.stream.Location;
import mf.javax.xml.stream.XMLStreamException;

public interface XMLEvent {
    Characters asCharacters();

    EndElement asEndElement();

    StartElement asStartElement();

    int getEventType();

    Location getLocation();

    boolean isStartDocument();

    void writeAsEncodedUnicode(Writer writer) throws XMLStreamException;
}
