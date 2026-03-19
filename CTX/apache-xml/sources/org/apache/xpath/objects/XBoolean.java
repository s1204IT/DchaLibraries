package org.apache.xpath.objects;

import javax.xml.transform.TransformerException;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.XPath;
import org.apache.xpath.compiler.Keywords;

public class XBoolean extends XObject {
    static final long serialVersionUID = -2964933058866100881L;
    private final boolean m_val;
    public static final XBoolean S_TRUE = new XBooleanStatic(true);
    public static final XBoolean S_FALSE = new XBooleanStatic(false);

    public XBoolean(boolean z) {
        this.m_val = z;
    }

    public XBoolean(Boolean bool) {
        this.m_val = bool.booleanValue();
        setObject(bool);
    }

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public String getTypeString() {
        return "#BOOLEAN";
    }

    @Override
    public double num() {
        if (this.m_val) {
            return 1.0d;
        }
        return XPath.MATCH_SCORE_QNAME;
    }

    @Override
    public boolean bool() {
        return this.m_val;
    }

    @Override
    public String str() {
        return this.m_val ? Keywords.FUNC_TRUE_STRING : Keywords.FUNC_FALSE_STRING;
    }

    @Override
    public Object object() {
        if (this.m_obj == null) {
            setObject(new Boolean(this.m_val));
        }
        return this.m_obj;
    }

    @Override
    public boolean equals(XObject xObject) {
        if (xObject.getType() == 4) {
            return xObject.equals((XObject) this);
        }
        try {
            return this.m_val == xObject.bool();
        } catch (TransformerException e) {
            throw new WrappedRuntimeException(e);
        }
    }
}
