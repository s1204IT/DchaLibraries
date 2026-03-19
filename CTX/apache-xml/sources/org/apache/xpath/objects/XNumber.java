package org.apache.xpath.objects;

import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.Constants;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;

public class XNumber extends XObject {
    static final long serialVersionUID = -2720400709619020193L;
    double m_val;

    public XNumber(double d) {
        this.m_val = d;
    }

    public XNumber(Number number) {
        this.m_val = number.doubleValue();
        setObject(number);
    }

    @Override
    public int getType() {
        return 2;
    }

    @Override
    public String getTypeString() {
        return "#NUMBER";
    }

    @Override
    public double num() {
        return this.m_val;
    }

    @Override
    public double num(XPathContext xPathContext) throws TransformerException {
        return this.m_val;
    }

    @Override
    public boolean bool() {
        return (Double.isNaN(this.m_val) || this.m_val == XPath.MATCH_SCORE_QNAME) ? false : true;
    }

    @Override
    public String str() {
        String str;
        if (Double.isNaN(this.m_val)) {
            return "NaN";
        }
        if (Double.isInfinite(this.m_val)) {
            if (this.m_val > XPath.MATCH_SCORE_QNAME) {
                return Constants.ATTRVAL_INFINITY;
            }
            return "-Infinity";
        }
        String string = Double.toString(this.m_val);
        int length = string.length();
        int i = length - 2;
        if (string.charAt(i) == '.' && string.charAt(length - 1) == '0') {
            String strSubstring = string.substring(0, i);
            if (strSubstring.equals("-0")) {
                return "0";
            }
            return strSubstring;
        }
        int iIndexOf = string.indexOf(69);
        if (iIndexOf < 0) {
            int i2 = length - 1;
            if (string.charAt(i2) == '0') {
                return string.substring(0, i2);
            }
            return string;
        }
        int i3 = Integer.parseInt(string.substring(iIndexOf + 1));
        if (string.charAt(0) == '-') {
            str = "-";
            string = string.substring(1);
            iIndexOf--;
        } else {
            str = "";
        }
        int i4 = iIndexOf - 2;
        if (i3 >= i4) {
            return str + string.substring(0, 1) + string.substring(2, iIndexOf) + zeros(i3 - i4);
        }
        while (string.charAt(iIndexOf - 1) == '0') {
            iIndexOf--;
        }
        if (i3 > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(string.substring(0, 1));
            int i5 = i3 + 2;
            sb.append(string.substring(2, i5));
            sb.append(Constants.ATTRVAL_THIS);
            sb.append(string.substring(i5, iIndexOf));
            return sb.toString();
        }
        return str + "0." + zeros((-1) - i3) + string.substring(0, 1) + string.substring(2, iIndexOf);
    }

    private static String zeros(int i) {
        if (i < 1) {
            return "";
        }
        char[] cArr = new char[i];
        for (int i2 = 0; i2 < i; i2++) {
            cArr[i2] = '0';
        }
        return new String(cArr);
    }

    @Override
    public Object object() {
        if (this.m_obj == null) {
            setObject(new Double(this.m_val));
        }
        return this.m_obj;
    }

    @Override
    public boolean equals(XObject xObject) {
        int type = xObject.getType();
        try {
            if (type == 4) {
                return xObject.equals((XObject) this);
            }
            return type == 1 ? xObject.bool() == bool() : this.m_val == xObject.num();
        } catch (TransformerException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public boolean isStableNumber() {
        return true;
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        xPathVisitor.visitNumberLiteral(expressionOwner, this);
    }
}
