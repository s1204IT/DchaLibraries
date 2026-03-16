package org.ccil.cowan.tagsoup;

import java.io.PrintWriter;
import java.io.Writer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class PYXWriter implements ScanHandler, ContentHandler, LexicalHandler {
    private static char[] dummy = new char[1];
    private String attrName;
    private PrintWriter theWriter;

    @Override
    public void adup(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.println(this.attrName);
        this.attrName = null;
    }

    @Override
    public void aname(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.print('A');
        this.theWriter.write(buff, offset, length);
        this.theWriter.print(' ');
        this.attrName = new String(buff, offset, length);
    }

    @Override
    public void aval(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.write(buff, offset, length);
        this.theWriter.println();
        this.attrName = null;
    }

    @Override
    public void cmnt(char[] buff, int offset, int length) throws SAXException {
    }

    @Override
    public void entity(char[] buff, int offset, int length) throws SAXException {
    }

    @Override
    public int getEntity() {
        return 0;
    }

    @Override
    public void eof(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.close();
    }

    @Override
    public void etag(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.print(')');
        this.theWriter.write(buff, offset, length);
        this.theWriter.println();
    }

    @Override
    public void decl(char[] buff, int offset, int length) throws SAXException {
    }

    @Override
    public void gi(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.print('(');
        this.theWriter.write(buff, offset, length);
        this.theWriter.println();
    }

    @Override
    public void cdsect(char[] buff, int offset, int length) throws SAXException {
        pcdata(buff, offset, length);
    }

    @Override
    public void pcdata(char[] buff, int offset, int length) throws SAXException {
        if (length != 0) {
            boolean inProgress = false;
            int length2 = length + offset;
            for (int i = offset; i < length2; i++) {
                if (buff[i] == '\n') {
                    if (inProgress) {
                        this.theWriter.println();
                    }
                    this.theWriter.println("-\\n");
                    inProgress = false;
                } else {
                    if (!inProgress) {
                        this.theWriter.print('-');
                    }
                    switch (buff[i]) {
                        case '\t':
                            this.theWriter.print("\\t");
                            break;
                        case '\\':
                            this.theWriter.print("\\\\");
                            break;
                        default:
                            this.theWriter.print(buff[i]);
                            break;
                    }
                    inProgress = true;
                }
            }
            if (inProgress) {
                this.theWriter.println();
            }
        }
    }

    @Override
    public void pitarget(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.print('?');
        this.theWriter.write(buff, offset, length);
        this.theWriter.write(32);
    }

    @Override
    public void pi(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.write(buff, offset, length);
        this.theWriter.println();
    }

    @Override
    public void stagc(char[] buff, int offset, int length) throws SAXException {
    }

    @Override
    public void stage(char[] buff, int offset, int length) throws SAXException {
        this.theWriter.println("!");
    }

    @Override
    public void characters(char[] buff, int offset, int length) throws SAXException {
        pcdata(buff, offset, length);
    }

    @Override
    public void endDocument() throws SAXException {
        this.theWriter.close();
    }

    @Override
    public void endElement(String uri, String localname, String qname) throws SAXException {
        if (qname.length() == 0) {
            qname = localname;
        }
        this.theWriter.print(')');
        this.theWriter.println(qname);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] buff, int offset, int length) throws SAXException {
        characters(buff, offset, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        this.theWriter.print('?');
        this.theWriter.print(target);
        this.theWriter.print(' ');
        this.theWriter.println(data);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void startElement(String uri, String localname, String qname, Attributes atts) throws SAXException {
        if (qname.length() == 0) {
            qname = localname;
        }
        this.theWriter.print('(');
        this.theWriter.println(qname);
        int length = atts.getLength();
        for (int i = 0; i < length; i++) {
            String qname2 = atts.getQName(i);
            if (qname2.length() == 0) {
                qname2 = atts.getLocalName(i);
            }
            this.theWriter.print('A');
            this.theWriter.print(qname2);
            this.theWriter.print(' ');
            this.theWriter.println(atts.getValue(i));
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        cmnt(ch, start, length);
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void endEntity(String name) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
    }

    @Override
    public void startEntity(String name) throws SAXException {
    }

    public PYXWriter(Writer w) {
        if (w instanceof PrintWriter) {
            this.theWriter = (PrintWriter) w;
        } else {
            this.theWriter = new PrintWriter(w);
        }
    }
}
