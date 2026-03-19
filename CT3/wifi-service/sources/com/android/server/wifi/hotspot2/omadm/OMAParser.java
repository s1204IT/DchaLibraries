package com.android.server.wifi.hotspot2.omadm;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OMAParser extends DefaultHandler {
    private XMLNode mRoot = null;
    private XMLNode mCurrent = null;

    public MOTree parse(String text, String urn) throws SAXException, IOException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(new InputSource(new StringReader(text)), this);
            return new MOTree(this.mRoot, urn);
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        }
    }

    public XMLNode getRoot() {
        return this.mRoot;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        XMLNode parent = this.mCurrent;
        this.mCurrent = new XMLNode(this.mCurrent, qName, attributes);
        if (this.mRoot == null) {
            this.mRoot = this.mCurrent;
        } else {
            parent.addChild(this.mCurrent);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!qName.equals(this.mCurrent.getTag())) {
            throw new SAXException("End tag '" + qName + "' doesn't match current node: " + this.mCurrent);
        }
        try {
            this.mCurrent.close();
            this.mCurrent = this.mCurrent.getParent();
        } catch (IOException ioe) {
            throw new SAXException("Failed to close element", ioe);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.mCurrent.addText(ch, start, length);
    }
}
