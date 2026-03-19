package javax.xml.transform.sax;

import javax.xml.transform.Result;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

public class SAXResult implements Result {
    public static final String FEATURE = "http://javax.xml.transform.sax.SAXResult/feature";
    private ContentHandler handler;
    private LexicalHandler lexhandler;
    private String systemId;

    public SAXResult() {
    }

    public SAXResult(ContentHandler contentHandler) {
        setHandler(contentHandler);
    }

    public void setHandler(ContentHandler contentHandler) {
        this.handler = contentHandler;
    }

    public ContentHandler getHandler() {
        return this.handler;
    }

    public void setLexicalHandler(LexicalHandler lexicalHandler) {
        this.lexhandler = lexicalHandler;
    }

    public LexicalHandler getLexicalHandler() {
        return this.lexhandler;
    }

    @Override
    public void setSystemId(String str) {
        this.systemId = str;
    }

    @Override
    public String getSystemId() {
        return this.systemId;
    }
}
