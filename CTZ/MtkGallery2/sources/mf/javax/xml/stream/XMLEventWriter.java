package mf.javax.xml.stream;

import mf.javax.xml.stream.events.XMLEvent;

public interface XMLEventWriter {
    void add(XMLEvent xMLEvent) throws XMLStreamException;

    void flush() throws XMLStreamException;
}
