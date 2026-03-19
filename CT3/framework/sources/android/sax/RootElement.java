package android.sax;

import android.net.ProxyInfo;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RootElement extends Element {
    final Handler handler;

    public RootElement(String uri, String localName) {
        super(null, uri, localName, 0);
        this.handler = new Handler();
    }

    public RootElement(String localName) {
        this(ProxyInfo.LOCAL_EXCL_LIST, localName);
    }

    public ContentHandler getContentHandler() {
        return this.handler;
    }

    class Handler extends DefaultHandler {
        Locator locator;
        int depth = -1;
        Element current = null;
        StringBuilder bodyBuilder = null;

        Handler() {
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            Children children;
            Element child;
            int depth = this.depth + 1;
            this.depth = depth;
            if (depth == 0) {
                startRoot(uri, localName, attributes);
                return;
            }
            if (this.bodyBuilder != null) {
                throw new BadXmlException("Encountered mixed content within text element named " + this.current + ".", this.locator);
            }
            if (depth != this.current.depth + 1 || (children = this.current.children) == null || (child = children.get(uri, localName)) == null) {
                return;
            }
            start(child, attributes);
        }

        void startRoot(String uri, String localName, Attributes attributes) throws SAXException {
            Element root = RootElement.this;
            if (root.uri.compareTo(uri) != 0 || root.localName.compareTo(localName) != 0) {
                throw new BadXmlException("Root element name does not match. Expected: " + root + ", Got: " + Element.toString(uri, localName), this.locator);
            }
            start(root, attributes);
        }

        void start(Element e, Attributes attributes) {
            this.current = e;
            if (e.startElementListener != null) {
                e.startElementListener.start(attributes);
            }
            if (e.endTextElementListener != null) {
                this.bodyBuilder = new StringBuilder();
            }
            e.resetRequiredChildren();
            e.visited = true;
        }

        @Override
        public void characters(char[] buffer, int start, int length) throws SAXException {
            if (this.bodyBuilder == null) {
                return;
            }
            this.bodyBuilder.append(buffer, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            Element current = this.current;
            if (this.depth == current.depth) {
                current.checkRequiredChildren(this.locator);
                if (current.endElementListener != null) {
                    current.endElementListener.end();
                }
                if (this.bodyBuilder != null) {
                    String body = this.bodyBuilder.toString();
                    this.bodyBuilder = null;
                    current.endTextElementListener.end(body);
                }
                this.current = current.parent;
            }
            this.depth--;
        }
    }
}
