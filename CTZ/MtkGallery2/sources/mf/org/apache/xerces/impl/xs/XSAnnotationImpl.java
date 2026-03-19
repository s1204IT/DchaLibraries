package mf.org.apache.xerces.impl.xs;

import java.io.IOException;
import java.io.StringReader;
import mf.org.apache.xerces.dom.CoreDocumentImpl;
import mf.org.apache.xerces.parsers.DOMParser;
import mf.org.apache.xerces.parsers.SAXParser;
import mf.org.apache.xerces.xs.XSAnnotation;
import mf.org.apache.xerces.xs.XSNamespaceItem;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XSAnnotationImpl implements XSAnnotation {
    private String fData;
    private SchemaGrammar fGrammar;

    public XSAnnotationImpl(String contents, SchemaGrammar grammar) {
        this.fData = null;
        this.fGrammar = null;
        this.fData = contents;
        this.fGrammar = grammar;
    }

    @Override
    public boolean writeAnnotation(Object target, short targetType) {
        if (targetType == 1 || targetType == 3) {
            writeToDOM((Node) target, targetType);
            return true;
        }
        if (targetType == 2) {
            writeToSAX((ContentHandler) target);
            return true;
        }
        return false;
    }

    @Override
    public String getAnnotationString() {
        return this.fData;
    }

    @Override
    public short getType() {
        return (short) 12;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public XSNamespaceItem getNamespaceItem() {
        return null;
    }

    private synchronized void writeToSAX(ContentHandler handler) {
        SAXParser parser = this.fGrammar.getSAXParser();
        StringReader aReader = new StringReader(this.fData);
        InputSource aSource = new InputSource(aReader);
        parser.setContentHandler(handler);
        try {
            parser.parse(aSource);
        } catch (IOException e) {
        } catch (SAXException e2) {
        }
        parser.setContentHandler(null);
    }

    private synchronized void writeToDOM(Node target, short type) {
        Node newElem;
        try {
            Document futureOwner = type == 1 ? target.getOwnerDocument() : (Document) target;
            DOMParser parser = this.fGrammar.getDOMParser();
            StringReader aReader = new StringReader(this.fData);
            InputSource aSource = new InputSource(aReader);
            try {
                parser.parse(aSource);
            } catch (IOException e) {
            } catch (SAXException e2) {
            }
            Document aDocument = parser.getDocument();
            parser.dropDocumentReferences();
            Element annotation = aDocument.getDocumentElement();
            if (!(futureOwner instanceof CoreDocumentImpl) || (newElem = futureOwner.adoptNode(annotation)) == null) {
                newElem = futureOwner.importNode(annotation, true);
            }
            target.insertBefore(newElem, target.getFirstChild());
        } catch (Throwable th) {
            throw th;
        }
    }
}
