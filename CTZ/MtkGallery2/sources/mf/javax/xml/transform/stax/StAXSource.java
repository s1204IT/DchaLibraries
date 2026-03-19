package mf.javax.xml.transform.stax;

import mf.javax.xml.stream.XMLEventReader;
import mf.javax.xml.stream.XMLStreamReader;
import mf.javax.xml.transform.Source;

public class StAXSource implements Source {
    private XMLEventReader xmlEventReader;
    private XMLStreamReader xmlStreamReader;

    public XMLEventReader getXMLEventReader() {
        return this.xmlEventReader;
    }

    public XMLStreamReader getXMLStreamReader() {
        return this.xmlStreamReader;
    }
}
