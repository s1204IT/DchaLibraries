package mf.javax.xml.transform.sax;

import mf.javax.xml.transform.Result;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

public class SAXResult implements Result {
    private ContentHandler handler;
    private LexicalHandler lexhandler;

    public ContentHandler getHandler() {
        return this.handler;
    }

    public LexicalHandler getLexicalHandler() {
        return this.lexhandler;
    }
}
