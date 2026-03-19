package mf.javax.xml.transform.sax;

import mf.javax.xml.transform.Source;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class SAXSource implements Source {
    private InputSource inputSource;
    private XMLReader reader;

    public XMLReader getXMLReader() {
        return this.reader;
    }

    public InputSource getInputSource() {
        return this.inputSource;
    }
}
