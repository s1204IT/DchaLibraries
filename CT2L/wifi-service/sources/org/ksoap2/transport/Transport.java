package org.ksoap2.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.util.List;
import org.ksoap2.SoapEnvelope;
import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParserException;

public abstract class Transport {
    protected static final String CONTENT_TYPE_SOAP_XML_CHARSET_UTF_8 = "application/soap+xml;charset=utf-8";
    protected static final String CONTENT_TYPE_XML_CHARSET_UTF_8 = "text/xml;charset=utf-8";
    protected static final String USER_AGENT = "ksoap2-android/2.6.0+";
    private int bufferLength;
    public boolean debug;
    protected Proxy proxy;
    public String requestDump;
    public String responseDump;
    protected int timeout;
    protected String url;
    private String xmlVersionTag;

    public abstract List call(String str, SoapEnvelope soapEnvelope, List list) throws XmlPullParserException, IOException;

    public abstract String getHost();

    public abstract String getPath();

    public abstract int getPort();

    public abstract ServiceConnection getServiceConnection() throws IOException;

    public Transport() {
        this.timeout = ServiceConnection.DEFAULT_TIMEOUT;
        this.debug = true;
        this.xmlVersionTag = "";
        this.bufferLength = ServiceConnection.DEFAULT_BUFFER_SIZE;
    }

    public Transport(String url) {
        this((Proxy) null, url);
    }

    public Transport(String url, int timeout) {
        this.timeout = ServiceConnection.DEFAULT_TIMEOUT;
        this.debug = true;
        this.xmlVersionTag = "";
        this.bufferLength = ServiceConnection.DEFAULT_BUFFER_SIZE;
        this.url = url;
        this.timeout = timeout;
    }

    public Transport(String url, int timeout, int bufferLength) {
        this.timeout = ServiceConnection.DEFAULT_TIMEOUT;
        this.debug = true;
        this.xmlVersionTag = "";
        this.bufferLength = ServiceConnection.DEFAULT_BUFFER_SIZE;
        this.url = url;
        this.timeout = timeout;
        this.bufferLength = bufferLength;
    }

    public Transport(Proxy proxy, String url) {
        this.timeout = ServiceConnection.DEFAULT_TIMEOUT;
        this.debug = true;
        this.xmlVersionTag = "";
        this.bufferLength = ServiceConnection.DEFAULT_BUFFER_SIZE;
        this.proxy = proxy;
        this.url = url;
    }

    public Transport(Proxy proxy, String url, int timeout) {
        this.timeout = ServiceConnection.DEFAULT_TIMEOUT;
        this.debug = true;
        this.xmlVersionTag = "";
        this.bufferLength = ServiceConnection.DEFAULT_BUFFER_SIZE;
        this.proxy = proxy;
        this.url = url;
        this.timeout = timeout;
    }

    public Transport(Proxy proxy, String url, int timeout, int bufferLength) {
        this.timeout = ServiceConnection.DEFAULT_TIMEOUT;
        this.debug = true;
        this.xmlVersionTag = "";
        this.bufferLength = ServiceConnection.DEFAULT_BUFFER_SIZE;
        this.proxy = proxy;
        this.url = url;
        this.timeout = timeout;
        this.bufferLength = bufferLength;
    }

    protected void parseResponse(SoapEnvelope envelope, InputStream is) throws XmlPullParserException, IOException {
        KXmlParser kXmlParser = new KXmlParser();
        kXmlParser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", true);
        kXmlParser.setInput(is, null);
        envelope.parse(kXmlParser);
    }

    protected byte[] createRequestData(SoapEnvelope envelope, String encoding) throws IOException {
        System.out.println("createRequestData");
        ByteArrayOutputStream bos = new ByteArrayOutputStream(this.bufferLength);
        bos.write(this.xmlVersionTag.getBytes());
        System.out.println("bos.write");
        KXmlSerializer kXmlSerializer = new KXmlSerializer();
        System.out.println("new KXmlSerializer");
        kXmlSerializer.setOutput(bos, encoding);
        System.out.println("xw.setOutput");
        envelope.write(kXmlSerializer);
        System.out.println("envelope.write");
        kXmlSerializer.flush();
        bos.write(13);
        bos.write(10);
        bos.flush();
        byte[] result = bos.toByteArray();
        System.out.println("createRequestData end");
        return result;
    }

    protected byte[] createRequestData(SoapEnvelope envelope) throws IOException {
        return createRequestData(envelope, null);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setXmlVersionTag(String tag) {
        this.xmlVersionTag = tag;
    }

    public void reset() {
    }

    public void call(String targetNamespace, SoapEnvelope envelope) throws XmlPullParserException, IOException {
        call(targetNamespace, envelope, null);
    }
}
