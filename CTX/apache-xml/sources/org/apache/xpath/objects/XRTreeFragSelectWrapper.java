package org.apache.xpath.objects;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.Expression;
import org.apache.xpath.XPathContext;
import org.apache.xpath.res.XPATHErrorResources;

public class XRTreeFragSelectWrapper extends XRTreeFrag implements Cloneable {
    static final long serialVersionUID = -6526177905590461251L;

    public XRTreeFragSelectWrapper(Expression expression) {
        super(expression);
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        ((Expression) this.m_obj).fixupVariables(vector, i);
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        XObject xObjectExecute = ((Expression) this.m_obj).execute(xPathContext);
        xObjectExecute.allowDetachToRelease(this.m_allowRelease);
        if (xObjectExecute.getType() == 3) {
            return xObjectExecute;
        }
        return new XString(xObjectExecute.str());
    }

    @Override
    public void detach() {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER, null));
    }

    @Override
    public double num() throws TransformerException {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER, null));
    }

    @Override
    public XMLString xstr() {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER, null));
    }

    @Override
    public String str() {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER, null));
    }

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public int rtf() {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER, null));
    }

    @Override
    public DTMIterator asNodeIterator() {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER, null));
    }
}
