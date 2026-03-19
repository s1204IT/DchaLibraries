package mf.javax.xml.transform.stax;

import mf.javax.xml.stream.XMLEventWriter;
import mf.javax.xml.stream.XMLStreamWriter;
import mf.javax.xml.transform.Result;

public class StAXResult implements Result {
    private XMLEventWriter xmlEventWriter;
    private XMLStreamWriter xmlStreamWriter;

    public XMLEventWriter getXMLEventWriter() {
        return this.xmlEventWriter;
    }

    public XMLStreamWriter getXMLStreamWriter() {
        return this.xmlStreamWriter;
    }
}
