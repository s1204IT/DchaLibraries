package org.apache.xalan.processor;

import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemText;
import org.apache.xalan.templates.ElemTextLiteral;
import org.apache.xml.utils.XMLCharacterRecognizer;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ProcessorCharacters extends XSLTElementProcessor {
    static final long serialVersionUID = 8632900007814162650L;
    private ElemText m_xslTextElement;
    protected Node m_firstBackPointer = null;
    private StringBuffer m_accumulator = new StringBuffer();

    @Override
    public void startNonText(StylesheetHandler stylesheetHandler) throws SAXException {
        boolean disableOutputEscaping;
        if (this == stylesheetHandler.getCurrentProcessor()) {
            stylesheetHandler.popProcessor();
        }
        int length = this.m_accumulator.length();
        if ((length > 0 && (this.m_xslTextElement != null || !XMLCharacterRecognizer.isWhiteSpace(this.m_accumulator))) || stylesheetHandler.isSpacePreserve()) {
            ElemTextLiteral elemTextLiteral = new ElemTextLiteral();
            elemTextLiteral.setDOMBackPointer(this.m_firstBackPointer);
            elemTextLiteral.setLocaterInfo(stylesheetHandler.getLocator());
            try {
                elemTextLiteral.setPrefixes(stylesheetHandler.getNamespaceSupport());
                if (this.m_xslTextElement != null) {
                    disableOutputEscaping = this.m_xslTextElement.getDisableOutputEscaping();
                } else {
                    disableOutputEscaping = false;
                }
                elemTextLiteral.setDisableOutputEscaping(disableOutputEscaping);
                elemTextLiteral.setPreserveSpace(true);
                char[] cArr = new char[length];
                this.m_accumulator.getChars(0, length, cArr, 0);
                elemTextLiteral.setChars(cArr);
                stylesheetHandler.getElemTemplateElement().appendChild((ElemTemplateElement) elemTextLiteral);
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        }
        this.m_accumulator.setLength(0);
        this.m_firstBackPointer = null;
    }

    @Override
    public void characters(StylesheetHandler stylesheetHandler, char[] cArr, int i, int i2) throws SAXException {
        this.m_accumulator.append(cArr, i, i2);
        if (this.m_firstBackPointer == null) {
            this.m_firstBackPointer = stylesheetHandler.getOriginatingNode();
        }
        if (this != stylesheetHandler.getCurrentProcessor()) {
            stylesheetHandler.pushProcessor(this);
        }
    }

    @Override
    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        startNonText(stylesheetHandler);
        stylesheetHandler.getCurrentProcessor().endElement(stylesheetHandler, str, str2, str3);
        stylesheetHandler.popProcessor();
    }

    void setXslTextElement(ElemText elemText) {
        this.m_xslTextElement = elemText;
    }
}
