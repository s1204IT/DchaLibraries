package org.apache.xalan.transformer;

import java.io.IOException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.TransformerHandler;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.ref.IncrementalSAXSource_Filter;
import org.apache.xml.dtm.ref.sax2dtm.SAX2DTM;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

public class TransformerHandlerImpl implements EntityResolver, DTDHandler, ContentHandler, ErrorHandler, LexicalHandler, TransformerHandler, DeclHandler {
    private static boolean DEBUG = false;
    private String m_baseSystemID;
    private ContentHandler m_contentHandler;
    private DTDHandler m_dtdHandler;
    DTM m_dtm;
    private EntityResolver m_entityResolver;
    private ErrorHandler m_errorHandler;
    private final boolean m_incremental;
    private LexicalHandler m_lexicalHandler;
    private final boolean m_optimizer;
    private final boolean m_source_location;
    private TransformerImpl m_transformer;
    private boolean m_insideParse = false;
    private Result m_result = null;
    private Locator m_locator = null;
    private DeclHandler m_declHandler = null;

    public TransformerHandlerImpl(TransformerImpl transformerImpl, boolean z, String str) {
        this.m_entityResolver = null;
        this.m_dtdHandler = null;
        this.m_contentHandler = null;
        this.m_errorHandler = null;
        this.m_lexicalHandler = null;
        this.m_transformer = transformerImpl;
        this.m_baseSystemID = str;
        DTM dtm = transformerImpl.getXPathContext().getDTM(null, true, transformerImpl, true, true);
        this.m_dtm = dtm;
        dtm.setDocumentBaseURI(str);
        this.m_contentHandler = dtm.getContentHandler();
        this.m_dtdHandler = dtm.getDTDHandler();
        this.m_entityResolver = dtm.getEntityResolver();
        this.m_errorHandler = dtm.getErrorHandler();
        this.m_lexicalHandler = dtm.getLexicalHandler();
        this.m_incremental = transformerImpl.getIncremental();
        this.m_optimizer = transformerImpl.getOptimize();
        this.m_source_location = transformerImpl.getSource_location();
    }

    protected void clearCoRoutine() {
        clearCoRoutine(null);
    }

    protected void clearCoRoutine(SAXException sAXException) {
        if (sAXException != null) {
            this.m_transformer.setExceptionThrown(sAXException);
        }
        if (this.m_dtm instanceof SAX2DTM) {
            if (DEBUG) {
                System.err.println("In clearCoRoutine...");
            }
            try {
                SAX2DTM sax2dtm = (SAX2DTM) this.m_dtm;
                if (this.m_contentHandler != null && (this.m_contentHandler instanceof IncrementalSAXSource_Filter)) {
                    ((IncrementalSAXSource_Filter) this.m_contentHandler).deliverMoreNodes(false);
                }
                sax2dtm.clearCoRoutine(true);
                this.m_contentHandler = null;
                this.m_dtdHandler = null;
                this.m_entityResolver = null;
                this.m_errorHandler = null;
                this.m_lexicalHandler = null;
            } catch (Throwable th) {
                th.printStackTrace();
            }
            if (DEBUG) {
                System.err.println("...exiting clearCoRoutine");
            }
        }
    }

    @Override
    public void setResult(Result result) throws IllegalArgumentException {
        if (result == null) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_RESULT_NULL, null));
        }
        try {
            this.m_transformer.setSerializationHandler(this.m_transformer.createSerializationHandler(result));
            this.m_result = result;
        } catch (TransformerException e) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_RESULT_COULD_NOT_BE_SET, null));
        }
    }

    @Override
    public void setSystemId(String str) {
        this.m_baseSystemID = str;
        this.m_dtm.setDocumentBaseURI(str);
    }

    @Override
    public String getSystemId() {
        return this.m_baseSystemID;
    }

    @Override
    public Transformer getTransformer() {
        return this.m_transformer;
    }

    @Override
    public InputSource resolveEntity(String str, String str2) throws SAXException, IOException {
        if (this.m_entityResolver != null) {
            return this.m_entityResolver.resolveEntity(str, str2);
        }
        return null;
    }

    @Override
    public void notationDecl(String str, String str2, String str3) throws SAXException {
        if (this.m_dtdHandler != null) {
            this.m_dtdHandler.notationDecl(str, str2, str3);
        }
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
        if (this.m_dtdHandler != null) {
            this.m_dtdHandler.unparsedEntityDecl(str, str2, str3, str4);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#setDocumentLocator: " + locator.getSystemId());
        }
        this.m_locator = locator;
        if (this.m_baseSystemID == null) {
            setSystemId(locator.getSystemId());
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.setDocumentLocator(locator);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#startDocument");
        }
        this.m_insideParse = true;
        if (this.m_contentHandler != null) {
            if (this.m_incremental) {
                this.m_transformer.setSourceTreeDocForThread(this.m_dtm.getDocument());
                this.m_transformer.runTransformThread(Thread.currentThread().getPriority());
            }
            this.m_contentHandler.startDocument();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#endDocument");
        }
        this.m_insideParse = false;
        if (this.m_contentHandler != null) {
            this.m_contentHandler.endDocument();
        }
        if (this.m_incremental) {
            this.m_transformer.waitTransformThread();
        } else {
            this.m_transformer.setSourceTreeDocForThread(this.m_dtm.getDocument());
            this.m_transformer.run();
        }
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#startPrefixMapping: " + str + ", " + str2);
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.startPrefixMapping(str, str2);
        }
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#endPrefixMapping: " + str);
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.endPrefixMapping(str);
        }
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#startElement: " + str3);
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.startElement(str, str2, str3, attributes);
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#endElement: " + str3);
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.endElement(str, str2, str3);
        }
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#characters: " + i + ", " + i2);
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.characters(cArr, i, i2);
        }
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#ignorableWhitespace: " + i + ", " + i2);
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.ignorableWhitespace(cArr, i, i2);
        }
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#processingInstruction: " + str + ", " + str2);
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.processingInstruction(str, str2);
        }
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#skippedEntity: " + str);
        }
        if (this.m_contentHandler != null) {
            this.m_contentHandler.skippedEntity(str);
        }
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
        ErrorListener errorListener = this.m_transformer.getErrorListener();
        if (errorListener instanceof ErrorHandler) {
            ((ErrorHandler) errorListener).warning(sAXParseException);
        } else {
            try {
                errorListener.warning(new TransformerException(sAXParseException));
            } catch (TransformerException e) {
                throw sAXParseException;
            }
        }
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
        ErrorListener errorListener = this.m_transformer.getErrorListener();
        if (errorListener instanceof ErrorHandler) {
            ((ErrorHandler) errorListener).error(sAXParseException);
            if (this.m_errorHandler != null) {
                this.m_errorHandler.error(sAXParseException);
                return;
            }
            return;
        }
        try {
            errorListener.error(new TransformerException(sAXParseException));
            if (this.m_errorHandler != null) {
                this.m_errorHandler.error(sAXParseException);
            }
        } catch (TransformerException e) {
            throw sAXParseException;
        }
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        if (this.m_errorHandler != null) {
            try {
                this.m_errorHandler.fatalError(sAXParseException);
            } catch (SAXParseException e) {
            }
        }
        ErrorListener errorListener = this.m_transformer.getErrorListener();
        if (errorListener instanceof ErrorHandler) {
            ((ErrorHandler) errorListener).fatalError(sAXParseException);
            if (this.m_errorHandler != null) {
                this.m_errorHandler.fatalError(sAXParseException);
                return;
            }
            return;
        }
        try {
            errorListener.fatalError(new TransformerException(sAXParseException));
            if (this.m_errorHandler != null) {
                this.m_errorHandler.fatalError(sAXParseException);
            }
        } catch (TransformerException e2) {
            throw sAXParseException;
        }
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#startDTD: " + str + ", " + str2 + ", " + str3);
        }
        if (this.m_lexicalHandler != null) {
            this.m_lexicalHandler.startDTD(str, str2, str3);
        }
    }

    @Override
    public void endDTD() throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#endDTD");
        }
        if (this.m_lexicalHandler != null) {
            this.m_lexicalHandler.endDTD();
        }
    }

    @Override
    public void startEntity(String str) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#startEntity: " + str);
        }
        if (this.m_lexicalHandler != null) {
            this.m_lexicalHandler.startEntity(str);
        }
    }

    @Override
    public void endEntity(String str) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#endEntity: " + str);
        }
        if (this.m_lexicalHandler != null) {
            this.m_lexicalHandler.endEntity(str);
        }
    }

    @Override
    public void startCDATA() throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#startCDATA");
        }
        if (this.m_lexicalHandler != null) {
            this.m_lexicalHandler.startCDATA();
        }
    }

    @Override
    public void endCDATA() throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#endCDATA");
        }
        if (this.m_lexicalHandler != null) {
            this.m_lexicalHandler.endCDATA();
        }
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#comment: " + i + ", " + i2);
        }
        if (this.m_lexicalHandler != null) {
            this.m_lexicalHandler.comment(cArr, i, i2);
        }
    }

    @Override
    public void elementDecl(String str, String str2) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#elementDecl: " + str + ", " + str2);
        }
        if (this.m_declHandler != null) {
            this.m_declHandler.elementDecl(str, str2);
        }
    }

    @Override
    public void attributeDecl(String str, String str2, String str3, String str4, String str5) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#attributeDecl: " + str + ", " + str2 + ", etc...");
        }
        if (this.m_declHandler != null) {
            this.m_declHandler.attributeDecl(str, str2, str3, str4, str5);
        }
    }

    @Override
    public void internalEntityDecl(String str, String str2) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#internalEntityDecl: " + str + ", " + str2);
        }
        if (this.m_declHandler != null) {
            this.m_declHandler.internalEntityDecl(str, str2);
        }
    }

    @Override
    public void externalEntityDecl(String str, String str2, String str3) throws SAXException {
        if (DEBUG) {
            System.out.println("TransformerHandlerImpl#externalEntityDecl: " + str + ", " + str2 + ", " + str3);
        }
        if (this.m_declHandler != null) {
            this.m_declHandler.externalEntityDecl(str, str2, str3);
        }
    }
}
