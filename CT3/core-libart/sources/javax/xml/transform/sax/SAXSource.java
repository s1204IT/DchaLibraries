package javax.xml.transform.sax;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class SAXSource implements Source {
    public static final String FEATURE = "http://javax.xml.transform.sax.SAXSource/feature";
    private InputSource inputSource;
    private XMLReader reader;

    public SAXSource() {
    }

    public SAXSource(XMLReader reader, InputSource inputSource) {
        this.reader = reader;
        this.inputSource = inputSource;
    }

    public SAXSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    public void setXMLReader(XMLReader reader) {
        this.reader = reader;
    }

    public XMLReader getXMLReader() {
        return this.reader;
    }

    public void setInputSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    public InputSource getInputSource() {
        return this.inputSource;
    }

    @Override
    public void setSystemId(String systemId) {
        if (this.inputSource == null) {
            this.inputSource = new InputSource(systemId);
        } else {
            this.inputSource.setSystemId(systemId);
        }
    }

    @Override
    public String getSystemId() {
        if (this.inputSource == null) {
            return null;
        }
        return this.inputSource.getSystemId();
    }

    public static InputSource sourceToInputSource(Source source) {
        if (source instanceof SAXSource) {
            return source.getInputSource();
        }
        if (source instanceof StreamSource) {
            InputSource isource = new InputSource(source.getSystemId());
            isource.setByteStream(source.getInputStream());
            isource.setCharacterStream(source.getReader());
            isource.setPublicId(source.getPublicId());
            return isource;
        }
        return null;
    }
}
