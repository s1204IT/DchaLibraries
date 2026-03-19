package mf.org.apache.xml.serialize;

import java.io.IOException;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Node;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TextSerializer extends BaseMarkupSerializer {
    public TextSerializer() {
        super(new OutputFormat("text", null, false));
    }

    @Override
    public void startElement(String namespaceURI, String localName, String rawName, Attributes attrs) throws SAXException {
        startElement(rawName == null ? localName : rawName, null);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String rawName) throws SAXException {
        endElement(rawName == null ? localName : rawName);
    }

    @Override
    public void startElement(String tagName, AttributeList attrs) throws SAXException {
        try {
            ElementState state = getElementState();
            if (isDocumentState() && !this._started) {
                startDocument(tagName);
            }
            boolean preserveSpace = state.preserveSpace;
            enterElementState(null, null, tagName, preserveSpace);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    public void endElement(String tagName) throws SAXException {
        try {
            endElementIO(tagName);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    public void endElementIO(String tagName) throws IOException {
        getElementState();
        ElementState state = leaveElementState();
        state.afterElement = true;
        state.empty = false;
        if (isDocumentState()) {
            this._printer.flush();
        }
    }

    @Override
    public void processingInstructionIO(String target, String code) throws IOException {
    }

    @Override
    public void comment(String text) {
    }

    @Override
    public void comment(char[] chars, int start, int length) {
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        try {
            ElementState state = content();
            state.inCData = false;
            state.doCData = false;
            printText(chars, start, length, true, true);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    protected void characters(String text, boolean unescaped) throws IOException {
        ElementState state = content();
        state.inCData = false;
        state.doCData = false;
        printText(text, true, true);
    }

    protected void startDocument(String rootTagName) throws IOException {
        this._printer.leaveDTD();
        this._started = true;
        serializePreRoot();
    }

    @Override
    protected void serializeElement(Element elem) throws IOException {
        String tagName = elem.getTagName();
        ElementState state = getElementState();
        if (isDocumentState() && !this._started) {
            startDocument(tagName);
        }
        boolean preserveSpace = state.preserveSpace;
        if (elem.hasChildNodes()) {
            enterElementState(null, null, tagName, preserveSpace);
            for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                serializeNode(child);
            }
            endElementIO(tagName);
            return;
        }
        if (!isDocumentState()) {
            state.afterElement = true;
            state.empty = false;
        }
    }

    @Override
    protected void serializeNode(Node node) throws IOException {
        switch (node.getNodeType()) {
            case 1:
                serializeElement((Element) node);
                break;
            case 3:
                String text = node.getNodeValue();
                if (text != null) {
                    characters(node.getNodeValue(), true);
                }
                break;
            case 4:
                String text2 = node.getNodeValue();
                if (text2 != null) {
                    characters(node.getNodeValue(), true);
                }
                break;
            case 9:
            case 11:
                for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    serializeNode(child);
                }
                break;
        }
    }

    @Override
    protected ElementState content() {
        ElementState state = getElementState();
        if (!isDocumentState()) {
            if (state.empty) {
                state.empty = false;
            }
            state.afterElement = false;
        }
        return state;
    }

    @Override
    protected String getEntityRef(int ch) {
        return null;
    }
}
