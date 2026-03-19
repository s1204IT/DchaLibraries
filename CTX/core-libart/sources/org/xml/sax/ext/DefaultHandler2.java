package org.xml.sax.ext;

import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DefaultHandler2 extends DefaultHandler implements LexicalHandler, DeclHandler, EntityResolver2 {
    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void startEntity(String str) throws SAXException {
    }

    @Override
    public void endEntity(String str) throws SAXException {
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void attributeDecl(String str, String str2, String str3, String str4, String str5) throws SAXException {
    }

    @Override
    public void elementDecl(String str, String str2) throws SAXException {
    }

    @Override
    public void externalEntityDecl(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void internalEntityDecl(String str, String str2) throws SAXException {
    }

    @Override
    public InputSource getExternalSubset(String str, String str2) throws SAXException, IOException {
        return null;
    }

    @Override
    public InputSource resolveEntity(String str, String str2, String str3, String str4) throws SAXException, IOException {
        return null;
    }

    @Override
    public InputSource resolveEntity(String str, String str2) throws SAXException, IOException {
        return resolveEntity(null, str, null, str2);
    }
}
