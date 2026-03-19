package org.apache.xalan.templates;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
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
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        DecimalFormat decimalFormat;
        DecimalFormat decimalFormat2;
        ElemTemplateElement elemTemplateElement = (ElemTemplateElement) xPathContext.getNamespaceContext();
        StylesheetRoot stylesheetRoot = elemTemplateElement.getStylesheetRoot();
        double dNum = getArg0().execute(xPathContext).num();
        String str = getArg1().execute(xPathContext).str();
        if (str.indexOf(164) > 0) {
            stylesheetRoot.error(XSLTErrorResources.ER_CURRENCY_SIGN_ILLEGAL);
        }
        try {
            Expression arg2 = getArg2();
            if (arg2 != null) {
                String str2 = arg2.execute(xPathContext).str();
                DecimalFormatSymbols decimalFormatComposed = stylesheetRoot.getDecimalFormatComposed(new QName(str2, xPathContext.getNamespaceContext()));
                if (decimalFormatComposed == null) {
                    warn(xPathContext, XSLTErrorResources.WG_NO_DECIMALFORMAT_DECLARATION, new Object[]{str2});
                    decimalFormat = null;
                } else {
                    decimalFormat = new DecimalFormat();
                    decimalFormat.setDecimalFormatSymbols(decimalFormatComposed);
                    decimalFormat.applyLocalizedPattern(str);
                }
            } else {
                decimalFormat = null;
            }
            if (decimalFormat == null) {
                DecimalFormatSymbols decimalFormatComposed2 = stylesheetRoot.getDecimalFormatComposed(new QName(""));
                if (decimalFormatComposed2 != null) {
                    decimalFormat2 = new DecimalFormat();
                    decimalFormat2.setDecimalFormatSymbols(decimalFormatComposed2);
                    decimalFormat2.applyLocalizedPattern(str);
                } else {
                    DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.US);
                    decimalFormatSymbols.setInfinity(Constants.ATTRVAL_INFINITY);
                    decimalFormatSymbols.setNaN("NaN");
                    decimalFormat2 = new DecimalFormat();
                    decimalFormat2.setDecimalFormatSymbols(decimalFormatSymbols);
                    if (str != null) {
                        decimalFormat2.applyLocalizedPattern(str);
                    }
                }
            } else {
                decimalFormat2 = decimalFormat;
            }
            return new XString(decimalFormat2.format(dNum));
        } catch (Exception e) {
            elemTemplateElement.error(XSLTErrorResources.ER_MALFORMED_FORMAT_STRING, new Object[]{str});
            return XString.EMPTYSTRING;
        }
    }

    @Override
    public void warn(XPathContext xPathContext, String str, Object[] objArr) throws TransformerException {
        xPathContext.getErrorListener().warning(new TransformerException(XSLMessages.createWarning(str, objArr), (SAXSourceLocator) xPathContext.getSAXLocator()));
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
        if (i > 3 || i < 2) {
            reportWrongNumberArgs();
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createMessage("ER_TWO_OR_THREE", null));
    }
}
