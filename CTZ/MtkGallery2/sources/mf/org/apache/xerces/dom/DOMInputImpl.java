package mf.org.apache.xerces.dom;

import java.io.InputStream;
import java.io.Reader;
import mf.org.w3c.dom.ls.LSInput;

public class DOMInputImpl implements LSInput {
    protected String fBaseSystemId;
    protected InputStream fByteStream;
    protected boolean fCertifiedText;
    protected Reader fCharStream;
    protected String fData;
    protected String fEncoding;
    protected String fPublicId;
    protected String fSystemId;

    public DOMInputImpl() {
        this.fPublicId = null;
        this.fSystemId = null;
        this.fBaseSystemId = null;
        this.fByteStream = null;
        this.fCharStream = null;
        this.fData = null;
        this.fEncoding = null;
        this.fCertifiedText = false;
    }

    public DOMInputImpl(String publicId, String systemId, String baseSystemId) {
        this.fPublicId = null;
        this.fSystemId = null;
        this.fBaseSystemId = null;
        this.fByteStream = null;
        this.fCharStream = null;
        this.fData = null;
        this.fEncoding = null;
        this.fCertifiedText = false;
        this.fPublicId = publicId;
        this.fSystemId = systemId;
        this.fBaseSystemId = baseSystemId;
    }

    public DOMInputImpl(String publicId, String systemId, String baseSystemId, InputStream byteStream, String encoding) {
        this.fPublicId = null;
        this.fSystemId = null;
        this.fBaseSystemId = null;
        this.fByteStream = null;
        this.fCharStream = null;
        this.fData = null;
        this.fEncoding = null;
        this.fCertifiedText = false;
        this.fPublicId = publicId;
        this.fSystemId = systemId;
        this.fBaseSystemId = baseSystemId;
        this.fByteStream = byteStream;
        this.fEncoding = encoding;
    }

    public DOMInputImpl(String publicId, String systemId, String baseSystemId, Reader charStream, String encoding) {
        this.fPublicId = null;
        this.fSystemId = null;
        this.fBaseSystemId = null;
        this.fByteStream = null;
        this.fCharStream = null;
        this.fData = null;
        this.fEncoding = null;
        this.fCertifiedText = false;
        this.fPublicId = publicId;
        this.fSystemId = systemId;
        this.fBaseSystemId = baseSystemId;
        this.fCharStream = charStream;
        this.fEncoding = encoding;
    }

    public DOMInputImpl(String publicId, String systemId, String baseSystemId, String data, String encoding) {
        this.fPublicId = null;
        this.fSystemId = null;
        this.fBaseSystemId = null;
        this.fByteStream = null;
        this.fCharStream = null;
        this.fData = null;
        this.fEncoding = null;
        this.fCertifiedText = false;
        this.fPublicId = publicId;
        this.fSystemId = systemId;
        this.fBaseSystemId = baseSystemId;
        this.fData = data;
        this.fEncoding = encoding;
    }

    @Override
    public InputStream getByteStream() {
        return this.fByteStream;
    }

    @Override
    public void setByteStream(InputStream byteStream) {
        this.fByteStream = byteStream;
    }

    @Override
    public Reader getCharacterStream() {
        return this.fCharStream;
    }

    @Override
    public void setCharacterStream(Reader characterStream) {
        this.fCharStream = characterStream;
    }

    @Override
    public String getStringData() {
        return this.fData;
    }

    public void setStringData(String stringData) {
        this.fData = stringData;
    }

    @Override
    public String getEncoding() {
        return this.fEncoding;
    }

    @Override
    public void setEncoding(String encoding) {
        this.fEncoding = encoding;
    }

    @Override
    public String getPublicId() {
        return this.fPublicId;
    }

    @Override
    public void setPublicId(String publicId) {
        this.fPublicId = publicId;
    }

    @Override
    public String getSystemId() {
        return this.fSystemId;
    }

    @Override
    public void setSystemId(String systemId) {
        this.fSystemId = systemId;
    }

    @Override
    public String getBaseURI() {
        return this.fBaseSystemId;
    }

    @Override
    public void setBaseURI(String baseURI) {
        this.fBaseSystemId = baseURI;
    }

    public boolean getCertifiedText() {
        return this.fCertifiedText;
    }

    public void setCertifiedText(boolean certifiedText) {
        this.fCertifiedText = certifiedText;
    }
}
