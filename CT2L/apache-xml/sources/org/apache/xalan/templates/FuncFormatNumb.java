package org.apache.xalan.templates;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xpath.Expression;
import org.apache.xpath.XPathContext;
import org.apache.xpath.functions.Function3Args;
import org.apache.xpath.functions.WrongNumberArgsException;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncFormatNumb extends Function3Args {
    static final long serialVersionUID = -8869935264870858636L;

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        DecimalFormat formatter;
        DecimalFormat formatter2;
        ElemTemplateElement templElem = (ElemTemplateElement) xctxt.getNamespaceContext();
        StylesheetRoot ss = templElem.getStylesheetRoot();
        double num = getArg0().execute(xctxt).num();
        String patternStr = getArg1().execute(xctxt).str();
        if (patternStr.indexOf(164) > 0) {
            ss.error(XSLTErrorResources.ER_CURRENCY_SIGN_ILLEGAL);
        }
        try {
            Expression arg2Expr = getArg2();
            if (arg2Expr == null) {
                formatter = null;
            } else {
                String dfName = arg2Expr.execute(xctxt).str();
                QName qname = new QName(dfName, xctxt.getNamespaceContext());
                DecimalFormatSymbols dfs = ss.getDecimalFormatComposed(qname);
                if (dfs == null) {
                    warn(xctxt, XSLTErrorResources.WG_NO_DECIMALFORMAT_DECLARATION, new Object[]{dfName});
                    formatter = null;
                } else {
                    formatter = new DecimalFormat();
                    try {
                        formatter.setDecimalFormatSymbols(dfs);
                        formatter.applyLocalizedPattern(patternStr);
                    } catch (Exception e) {
                        templElem.error(XSLTErrorResources.ER_MALFORMED_FORMAT_STRING, new Object[]{patternStr});
                        return XString.EMPTYSTRING;
                    }
                }
            }
            if (formatter == null) {
                DecimalFormatSymbols dfs2 = ss.getDecimalFormatComposed(new QName(""));
                if (dfs2 != null) {
                    formatter2 = new DecimalFormat();
                    formatter2.setDecimalFormatSymbols(dfs2);
                    formatter2.applyLocalizedPattern(patternStr);
                } else {
                    DecimalFormatSymbols dfs3 = new DecimalFormatSymbols(Locale.US);
                    try {
                        dfs3.setInfinity(Constants.ATTRVAL_INFINITY);
                        dfs3.setNaN("NaN");
                        formatter2 = new DecimalFormat();
                        try {
                            formatter2.setDecimalFormatSymbols(dfs3);
                            if (patternStr != null) {
                                formatter2.applyLocalizedPattern(patternStr);
                            }
                        } catch (Exception e2) {
                            templElem.error(XSLTErrorResources.ER_MALFORMED_FORMAT_STRING, new Object[]{patternStr});
                            return XString.EMPTYSTRING;
                        }
                    } catch (Exception e3) {
                    }
                }
            } else {
                formatter2 = formatter;
            }
            return new XString(formatter2.format(num));
        } catch (Exception e4) {
        }
    }

    @Override
    public void warn(XPathContext xctxt, String msg, Object[] args) throws TransformerException {
        String formattedMsg = XSLMessages.createWarning(msg, args);
        ErrorListener errHandler = xctxt.getErrorListener();
        errHandler.warning(new TransformerException(formattedMsg, (SAXSourceLocator) xctxt.getSAXLocator()));
    }

    @Override
    public void checkNumberArgs(int argNum) throws WrongNumberArgsException {
        if (argNum > 3 || argNum < 2) {
            reportWrongNumberArgs();
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createMessage("ER_TWO_OR_THREE", null));
    }
}
