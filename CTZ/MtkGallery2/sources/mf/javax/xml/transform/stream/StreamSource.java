package mf.javax.xml.transform.stream;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import mf.javax.xml.transform.Source;

public class StreamSource implements Source {
    private InputStream inputStream;
    private String publicId;
    private Reader reader;
    private String systemId;

    public StreamSource() {
    }

    public StreamSource(InputStream inputStream) {
        setInputStream(inputStream);
    }

    public StreamSource(String systemId) {
        this.systemId = systemId;
    }

    public StreamSource(File f) {
        setSystemId(f.toURI().toASCIIString());
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    public Reader getReader() {
        return this.reader;
    }

    public String getPublicId() {
        return this.publicId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getSystemId() {
        return this.systemId;
    }
}
