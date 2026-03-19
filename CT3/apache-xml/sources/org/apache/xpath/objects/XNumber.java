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

    public XNumber(Number num) {
        this.m_val = num.doubleValue();
        setObject(num);
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
    public double num(XPathContext xctxt) throws TransformerException {
        return this.m_val;
    }

    @Override
    public boolean bool() {
        return (Double.isNaN(this.m_val) || this.m_val == XPath.MATCH_SCORE_QNAME) ? false : true;
    }

    @Override
    public String str() {
        String sign;
        if (Double.isNaN(this.m_val)) {
            return "NaN";
        }
        if (Double.isInfinite(this.m_val)) {
            if (this.m_val > XPath.MATCH_SCORE_QNAME) {
                return Constants.ATTRVAL_INFINITY;
            }
            return "-Infinity";
        }
        double num = this.m_val;
        String s = Double.toString(num);
        int len = s.length();
        if (s.charAt(len - 2) == '.' && s.charAt(len - 1) == '0') {
            String s2 = s.substring(0, len - 2);
            if (s2.equals("-0")) {
                return "0";
            }
            return s2;
        }
        int e = s.indexOf(69);
        if (e < 0) {
            if (s.charAt(len - 1) == '0') {
                return s.substring(0, len - 1);
            }
            return s;
        }
        int exp = Integer.parseInt(s.substring(e + 1));
        if (s.charAt(0) == '-') {
            sign = "-";
            s = s.substring(1);
            e--;
        } else {
            sign = "";
        }
        int nDigits = e - 2;
        if (exp >= nDigits) {
            return sign + s.substring(0, 1) + s.substring(2, e) + zeros(exp - nDigits);
        }
        while (s.charAt(e - 1) == '0') {
            e--;
        }
        if (exp > 0) {
            return sign + s.substring(0, 1) + s.substring(2, exp + 2) + Constants.ATTRVAL_THIS + s.substring(exp + 2, e);
        }
        return sign + "0." + zeros((-1) - exp) + s.substring(0, 1) + s.substring(2, e);
    }

    private static String zeros(int n) {
        if (n < 1) {
            return "";
        }
        char[] buf = new char[n];
        for (int i = 0; i < n; i++) {
            buf[i] = '0';
        }
        return new String(buf);
    }

    @Override
    public Object object() {
        if (this.m_obj == null) {
            setObject(new Double(this.m_val));
        }
        return this.m_obj;
    }

    @Override
    public boolean equals(XObject obj2) {
        int t = obj2.getType();
        try {
            if (t == 4) {
                return obj2.equals((XObject) this);
            }
            return t == 1 ? obj2.bool() == bool() : this.m_val == obj2.num();
        } catch (TransformerException te) {
            throw new WrappedRuntimeException(te);
        }
    }

    @Override
    public boolean isStableNumber() {
        return true;
    }

    @Override
    public void callVisitors(ExpressionOwner owner, XPathVisitor visitor) {
        visitor.visitNumberLiteral(owner, this);
    }
}
