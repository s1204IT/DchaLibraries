package android.net.wifi.hotspot2.omadm;

import android.text.TextUtils;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLParser extends DefaultHandler {
    private XMLNode mRoot = null;
    private XMLNode mCurrent = null;

    public XMLNode parse(String str) throws SAXException, IOException {
        if (TextUtils.isEmpty(str)) {
            throw new IOException("XML string not provided");
        }
        this.mRoot = null;
        this.mCurrent = null;
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(new StringReader(str)), this);
            return this.mRoot;
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        XMLNode xMLNode = this.mCurrent;
        this.mCurrent = new XMLNode(xMLNode, str3);
        if (this.mRoot == null) {
            this.mRoot = this.mCurrent;
        } else {
            if (xMLNode == null) {
                throw new SAXException("More than one root nodes");
            }
            xMLNode.addChild(this.mCurrent);
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        if (!str3.equals(this.mCurrent.getTag())) {
            throw new SAXException("End tag '" + str3 + "' doesn't match current node: " + this.mCurrent);
        }
        this.mCurrent.close();
        this.mCurrent = this.mCurrent.getParent();
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        this.mCurrent.addText(new String(cArr, i, i2));
    }
}
