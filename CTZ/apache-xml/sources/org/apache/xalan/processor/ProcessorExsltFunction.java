package org.apache.xalan.processor;

import org.apache.xalan.templates.ElemApplyImport;
import org.apache.xalan.templates.ElemApplyTemplates;
import org.apache.xalan.templates.ElemAttribute;
import org.apache.xalan.templates.ElemCallTemplate;
import org.apache.xalan.templates.ElemComment;
import org.apache.xalan.templates.ElemCopy;
import org.apache.xalan.templates.ElemCopyOf;
import org.apache.xalan.templates.ElemElement;
import org.apache.xalan.templates.ElemExsltFuncResult;
import org.apache.xalan.templates.ElemExsltFunction;
import org.apache.xalan.templates.ElemFallback;
import org.apache.xalan.templates.ElemLiteralResult;
import org.apache.xalan.templates.ElemMessage;
import org.apache.xalan.templates.ElemNumber;
import org.apache.xalan.templates.ElemPI;
import org.apache.xalan.templates.ElemParam;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemText;
import org.apache.xalan.templates.ElemTextLiteral;
import org.apache.xalan.templates.ElemValueOf;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.Stylesheet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProcessorExsltFunction extends ProcessorTemplateElem {
    static final long serialVersionUID = 2411427965578315332L;

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (!(stylesheetHandler.getElemTemplateElement() instanceof Stylesheet)) {
            stylesheetHandler.error("func:function element must be top level.", new SAXException("func:function element must be top level."));
        }
        super.startElement(stylesheetHandler, str, str2, str3, attributes);
        if (attributes.getValue("name").indexOf(":") <= 0) {
            stylesheetHandler.error("func:function name must have namespace", new SAXException("func:function name must have namespace"));
        }
    }

    @Override
    protected void appendAndPush(StylesheetHandler stylesheetHandler, ElemTemplateElement elemTemplateElement) throws SAXException {
        super.appendAndPush(stylesheetHandler, elemTemplateElement);
        elemTemplateElement.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
        stylesheetHandler.getStylesheet().setTemplate((ElemTemplate) elemTemplateElement);
    }

    @Override
    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        validate(stylesheetHandler.getElemTemplateElement(), stylesheetHandler);
        super.endElement(stylesheetHandler, str, str2, str3);
    }

    public void validate(ElemTemplateElement elemTemplateElement, StylesheetHandler stylesheetHandler) throws SAXException {
        while (elemTemplateElement != null) {
            if ((elemTemplateElement instanceof ElemExsltFuncResult) && elemTemplateElement.getNextSiblingElem() != null && !(elemTemplateElement.getNextSiblingElem() instanceof ElemFallback)) {
                stylesheetHandler.error("func:result has an illegal following sibling (only xsl:fallback allowed)", new SAXException("func:result has an illegal following sibling (only xsl:fallback allowed)"));
            }
            if (((elemTemplateElement instanceof ElemApplyImport) || (elemTemplateElement instanceof ElemApplyTemplates) || (elemTemplateElement instanceof ElemAttribute) || (elemTemplateElement instanceof ElemCallTemplate) || (elemTemplateElement instanceof ElemComment) || (elemTemplateElement instanceof ElemCopy) || (elemTemplateElement instanceof ElemCopyOf) || (elemTemplateElement instanceof ElemElement) || (elemTemplateElement instanceof ElemLiteralResult) || (elemTemplateElement instanceof ElemNumber) || (elemTemplateElement instanceof ElemPI) || (elemTemplateElement instanceof ElemText) || (elemTemplateElement instanceof ElemTextLiteral) || (elemTemplateElement instanceof ElemValueOf)) && !ancestorIsOk(elemTemplateElement)) {
                stylesheetHandler.error("misplaced literal result in a func:function container.", new SAXException("misplaced literal result in a func:function container."));
            }
            ElemTemplateElement parentElem = elemTemplateElement;
            elemTemplateElement = elemTemplateElement.getFirstChildElem();
            while (elemTemplateElement == null) {
                elemTemplateElement = parentElem.getNextSiblingElem();
                if (elemTemplateElement == null) {
                    parentElem = parentElem.getParentElem();
                }
                if (parentElem == null || (parentElem instanceof ElemExsltFunction)) {
                    return;
                }
            }
        }
    }

    boolean ancestorIsOk(ElemTemplateElement elemTemplateElement) {
        while (elemTemplateElement.getParentElem() != null && !(elemTemplateElement.getParentElem() instanceof ElemExsltFunction)) {
            elemTemplateElement = elemTemplateElement.getParentElem();
            if ((elemTemplateElement instanceof ElemExsltFuncResult) || (elemTemplateElement instanceof ElemVariable) || (elemTemplateElement instanceof ElemParam) || (elemTemplateElement instanceof ElemMessage)) {
                return true;
            }
        }
        return false;
    }
}
