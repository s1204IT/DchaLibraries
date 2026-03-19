package org.apache.xpath.objects;

import javax.xml.transform.TransformerException;
import org.apache.xml.utils.WrappedRuntimeException;

public class XBooleanStatic extends XBoolean {
    static final long serialVersionUID = -8064147275772687409L;
    private final boolean m_val;

    public XBooleanStatic(boolean z) {
        super(z);
        this.m_val = z;
    }

    @Override
    public boolean equals(XObject xObject) {
        try {
            return this.m_val == xObject.bool();
        } catch (TransformerException e) {
            throw new WrappedRuntimeException(e);
        }
    }
}
