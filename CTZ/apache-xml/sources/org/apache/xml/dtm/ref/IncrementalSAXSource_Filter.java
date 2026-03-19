package org.apache.xml.dtm.ref;

import java.io.IOException;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.ThreadControllerWrapper;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

public class IncrementalSAXSource_Filter implements IncrementalSAXSource, ContentHandler, DTDHandler, LexicalHandler, ErrorHandler, Runnable {
    private int eventcounter;
    boolean DEBUG = false;
    private CoroutineManager fCoroutineManager = null;
    private int fControllerCoroutineID = -1;
    private int fSourceCoroutineID = -1;
    private ContentHandler clientContentHandler = null;
    private LexicalHandler clientLexicalHandler = null;
    private DTDHandler clientDTDHandler = null;
    private ErrorHandler clientErrorHandler = null;
    private int frequency = 5;
    private boolean fNoMoreEvents = false;
    private XMLReader fXMLReader = null;
    private InputSource fXMLReaderInputSource = null;

    public IncrementalSAXSource_Filter() {
        init(new CoroutineManager(), -1, -1);
    }

    public IncrementalSAXSource_Filter(CoroutineManager coroutineManager, int i) {
        init(coroutineManager, i, -1);
    }

    public static IncrementalSAXSource createIncrementalSAXSource(CoroutineManager coroutineManager, int i) {
        return new IncrementalSAXSource_Filter(coroutineManager, i);
    }

    public void init(CoroutineManager coroutineManager, int i, int i2) {
        if (coroutineManager == null) {
            coroutineManager = new CoroutineManager();
        }
        this.fCoroutineManager = coroutineManager;
        this.fControllerCoroutineID = coroutineManager.co_joinCoroutineSet(i);
        this.fSourceCoroutineID = coroutineManager.co_joinCoroutineSet(i2);
        if (this.fControllerCoroutineID == -1 || this.fSourceCoroutineID == -1) {
            throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COJOINROUTINESET_FAILED, null));
        }
        this.fNoMoreEvents = false;
        this.eventcounter = this.frequency;
    }

    public void setXMLReader(XMLReader xMLReader) {
        this.fXMLReader = xMLReader;
        xMLReader.setContentHandler(this);
        xMLReader.setDTDHandler(this);
        xMLReader.setErrorHandler(this);
        try {
            xMLReader.setProperty("http://xml.org/sax/properties/lexical-handler", this);
        } catch (SAXNotRecognizedException e) {
        } catch (SAXNotSupportedException e2) {
        }
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.clientContentHandler = contentHandler;
    }

    @Override
    public void setDTDHandler(DTDHandler dTDHandler) {
        this.clientDTDHandler = dTDHandler;
    }

    @Override
    public void setLexicalHandler(LexicalHandler lexicalHandler) {
        this.clientLexicalHandler = lexicalHandler;
    }

    public void setErrHandler(ErrorHandler errorHandler) {
        this.clientErrorHandler = errorHandler;
    }

    public void setReturnFrequency(int i) {
        if (i < 1) {
            i = 1;
        }
        this.eventcounter = i;
        this.frequency = i;
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        int i3 = this.eventcounter - 1;
        this.eventcounter = i3;
        if (i3 <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.characters(cArr, i, i2);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.clientContentHandler != null) {
            this.clientContentHandler.endDocument();
        }
        this.eventcounter = 0;
        co_yield(false);
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.endElement(str, str2, str3);
        }
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.endPrefixMapping(str);
        }
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        int i3 = this.eventcounter - 1;
        this.eventcounter = i3;
        if (i3 <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.ignorableWhitespace(cArr, i, i2);
        }
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.processingInstruction(str, str2);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.setDocumentLocator(locator);
        }
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.skippedEntity(str);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        co_entry_pause();
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.startDocument();
        }
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.startElement(str, str2, str3, attributes);
        }
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
        if (this.clientContentHandler != null) {
            this.clientContentHandler.startPrefixMapping(str, str2);
        }
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        if (this.clientLexicalHandler != null) {
            this.clientLexicalHandler.comment(cArr, i, i2);
        }
    }

    @Override
    public void endCDATA() throws SAXException {
        if (this.clientLexicalHandler != null) {
            this.clientLexicalHandler.endCDATA();
        }
    }

    @Override
    public void endDTD() throws SAXException {
        if (this.clientLexicalHandler != null) {
            this.clientLexicalHandler.endDTD();
        }
    }

    @Override
    public void endEntity(String str) throws SAXException {
        if (this.clientLexicalHandler != null) {
            this.clientLexicalHandler.endEntity(str);
        }
    }

    @Override
    public void startCDATA() throws SAXException {
        if (this.clientLexicalHandler != null) {
            this.clientLexicalHandler.startCDATA();
        }
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
        if (this.clientLexicalHandler != null) {
            this.clientLexicalHandler.startDTD(str, str2, str3);
        }
    }

    @Override
    public void startEntity(String str) throws SAXException {
        if (this.clientLexicalHandler != null) {
            this.clientLexicalHandler.startEntity(str);
        }
    }

    @Override
    public void notationDecl(String str, String str2, String str3) throws SAXException {
        if (this.clientDTDHandler != null) {
            this.clientDTDHandler.notationDecl(str, str2, str3);
        }
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
        if (this.clientDTDHandler != null) {
            this.clientDTDHandler.unparsedEntityDecl(str, str2, str3, str4);
        }
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
        if (this.clientErrorHandler != null) {
            this.clientErrorHandler.error(sAXParseException);
        }
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        if (this.clientErrorHandler != null) {
            this.clientErrorHandler.error(sAXParseException);
        }
        this.eventcounter = 0;
        co_yield(false);
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
        if (this.clientErrorHandler != null) {
            this.clientErrorHandler.error(sAXParseException);
        }
    }

    public int getSourceCoroutineID() {
        return this.fSourceCoroutineID;
    }

    public int getControllerCoroutineID() {
        return this.fControllerCoroutineID;
    }

    public CoroutineManager getCoroutineManager() {
        return this.fCoroutineManager;
    }

    protected void count_and_yield(boolean z) throws SAXException {
        if (!z) {
            this.eventcounter = 0;
        }
        int i = this.eventcounter - 1;
        this.eventcounter = i;
        if (i <= 0) {
            co_yield(true);
            this.eventcounter = this.frequency;
        }
    }

    private void co_entry_pause() throws SAXException {
        if (this.fCoroutineManager == null) {
            init(null, -1, -1);
        }
        try {
            if (this.fCoroutineManager.co_entry_pause(this.fSourceCoroutineID) == Boolean.FALSE) {
                co_yield(false);
            }
        } catch (NoSuchMethodException e) {
            if (this.DEBUG) {
                e.printStackTrace();
            }
            throw new SAXException(e);
        }
    }

    private void co_yield(boolean z) throws SAXException {
        if (this.fNoMoreEvents) {
            return;
        }
        try {
            Object objCo_resume = Boolean.FALSE;
            if (z) {
                objCo_resume = this.fCoroutineManager.co_resume(Boolean.TRUE, this.fSourceCoroutineID, this.fControllerCoroutineID);
            }
            if (objCo_resume == Boolean.FALSE) {
                this.fNoMoreEvents = true;
                if (this.fXMLReader != null) {
                    throw new StopException();
                }
                this.fCoroutineManager.co_exit_to(Boolean.FALSE, this.fSourceCoroutineID, this.fControllerCoroutineID);
            }
        } catch (NoSuchMethodException e) {
            this.fNoMoreEvents = true;
            this.fCoroutineManager.co_exit(this.fSourceCoroutineID);
            throw new SAXException(e);
        }
    }

    @Override
    public void startParse(InputSource inputSource) throws SAXException {
        if (this.fNoMoreEvents) {
            throw new SAXException(XMLMessages.createXMLMessage(XMLErrorResources.ER_INCRSAXSRCFILTER_NOT_RESTARTABLE, null));
        }
        if (this.fXMLReader == null) {
            throw new SAXException(XMLMessages.createXMLMessage(XMLErrorResources.ER_XMLRDR_NOT_BEFORE_STARTPARSE, null));
        }
        this.fXMLReaderInputSource = inputSource;
        ThreadControllerWrapper.runThread(this, -1);
    }

    @Override
    public void run() {
        if (this.fXMLReader == null) {
            return;
        }
        if (this.DEBUG) {
            System.out.println("IncrementalSAXSource_Filter parse thread launched");
        }
        Object e = Boolean.FALSE;
        try {
            this.fXMLReader.parse(this.fXMLReaderInputSource);
        } catch (IOException e2) {
            e = e2;
        } catch (StopException e3) {
            if (this.DEBUG) {
                System.out.println("Active IncrementalSAXSource_Filter normal stop exception");
            }
        } catch (SAXException e4) {
            Exception exception = e4.getException();
            if (exception instanceof StopException) {
                if (this.DEBUG) {
                    System.out.println("Active IncrementalSAXSource_Filter normal stop exception");
                }
            } else {
                if (this.DEBUG) {
                    System.out.println("Active IncrementalSAXSource_Filter UNEXPECTED SAX exception: " + exception);
                    exception.printStackTrace();
                }
                e = e4;
            }
        }
        this.fXMLReader = null;
        try {
            this.fNoMoreEvents = true;
            this.fCoroutineManager.co_exit_to(e, this.fSourceCoroutineID, this.fControllerCoroutineID);
        } catch (NoSuchMethodException e5) {
            e5.printStackTrace(System.err);
            this.fCoroutineManager.co_exit(this.fSourceCoroutineID);
        }
    }

    class StopException extends RuntimeException {
        static final long serialVersionUID = -1129245796185754956L;

        StopException() {
        }
    }

    @Override
    public Object deliverMoreNodes(boolean z) {
        if (this.fNoMoreEvents) {
            return Boolean.FALSE;
        }
        try {
            Object objCo_resume = this.fCoroutineManager.co_resume(z ? Boolean.TRUE : Boolean.FALSE, this.fControllerCoroutineID, this.fSourceCoroutineID);
            if (objCo_resume == Boolean.FALSE) {
                this.fCoroutineManager.co_exit(this.fControllerCoroutineID);
            }
            return objCo_resume;
        } catch (NoSuchMethodException e) {
            return e;
        }
    }
}
