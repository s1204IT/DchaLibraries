package mf.org.apache.xerces.impl.dv.xs;

import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xs.datatypes.XSQName;

public class QNameDV extends TypeValidator {
    private static final String EMPTY_STRING = "".intern();

    @Override
    public short getAllowedFacets() {
        return (short) 2079;
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        String prefix;
        String localpart;
        int colonptr = content.indexOf(":");
        if (colonptr > 0) {
            prefix = context.getSymbol(content.substring(0, colonptr));
            localpart = content.substring(colonptr + 1);
        } else {
            prefix = EMPTY_STRING;
            localpart = content;
        }
        if (prefix.length() > 0 && !XMLChar.isValidNCName(prefix)) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, SchemaSymbols.ATTVAL_QNAME});
        }
        if (!XMLChar.isValidNCName(localpart)) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, SchemaSymbols.ATTVAL_QNAME});
        }
        String uri = context.getURI(prefix);
        if (prefix.length() > 0 && uri == null) {
            throw new InvalidDatatypeValueException("UndeclaredPrefix", new Object[]{content, prefix});
        }
        return new XQName(prefix, context.getSymbol(localpart), context.getSymbol(content), uri);
    }

    @Override
    public int getDataLength(Object value) {
        return ((XQName) value).rawname.length();
    }

    private static final class XQName extends QName implements XSQName {
        public XQName(String prefix, String localpart, String rawname, String uri) {
            setValues(prefix, localpart, rawname, uri);
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof QName)) {
                return false;
            }
            QName qname = (QName) object;
            return this.uri == qname.uri && this.localpart == qname.localpart;
        }

        @Override
        public String toString() {
            return this.rawname;
        }

        @Override
        public javax.xml.namespace.QName getJAXPQName() {
            return new javax.xml.namespace.QName(this.uri, this.localpart, this.prefix);
        }

        @Override
        public QName getXNIQName() {
            return this;
        }
    }
}
