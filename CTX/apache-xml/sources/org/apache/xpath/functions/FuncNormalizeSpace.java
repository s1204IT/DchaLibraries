package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class FuncNormalizeSpace extends FunctionDef1Arg {
    static final long serialVersionUID = -3377956872032190880L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return (XString) getArg0AsString(xPathContext).fixWhiteSpace(true, true, false);
    }

    @Override
    public void executeCharsToContentHandler(XPathContext xPathContext, ContentHandler contentHandler) throws TransformerException, SAXException {
        if (Arg0IsNodesetExpr()) {
            int arg0AsNode = getArg0AsNode(xPathContext);
            if (-1 != arg0AsNode) {
                xPathContext.getDTM(arg0AsNode).dispatchCharactersEvents(arg0AsNode, contentHandler, true);
                return;
            }
            return;
        }
        execute(xPathContext).dispatchCharactersEvents(contentHandler);
    }
}
