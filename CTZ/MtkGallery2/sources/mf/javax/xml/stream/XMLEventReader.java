package mf.javax.xml.stream;

import java.util.Iterator;
import mf.javax.xml.stream.events.XMLEvent;

public interface XMLEventReader extends Iterator {
    @Override
    boolean hasNext();

    XMLEvent nextEvent() throws XMLStreamException;

    XMLEvent peek() throws XMLStreamException;
}
