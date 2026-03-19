package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncSubstring extends Function3Args {
    static final long serialVersionUID = -5996676095024715502L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        double dRound;
        int i;
        XMLString xMLStringSubstring;
        XMLString xMLStringXstr = this.m_arg0.execute(xPathContext).xstr();
        double dNum = this.m_arg1.execute(xPathContext).num();
        int length = xMLStringXstr.length();
        if (length <= 0) {
            return XString.EMPTYSTRING;
        }
        if (Double.isNaN(dNum)) {
            dRound = -1000000.0d;
        } else {
            dRound = Math.round(dNum);
            if (dRound > XPath.MATCH_SCORE_QNAME) {
                i = ((int) dRound) - 1;
            }
            if (this.m_arg2 == null) {
                int iRound = ((int) (Math.round(this.m_arg2.num(xPathContext)) + dRound)) - 1;
                if (iRound >= 0) {
                    if (iRound > length) {
                        iRound = length;
                    }
                } else {
                    iRound = 0;
                }
                if (i <= length) {
                    length = i;
                }
                xMLStringSubstring = xMLStringXstr.substring(length, iRound);
            } else {
                if (i <= length) {
                    length = i;
                }
                xMLStringSubstring = xMLStringXstr.substring(length);
            }
            return (XString) xMLStringSubstring;
        }
        i = 0;
        if (this.m_arg2 == null) {
        }
        return (XString) xMLStringSubstring;
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
        if (i < 2) {
            reportWrongNumberArgs();
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createXPATHMessage("ER_TWO_OR_THREE", null));
    }
}
