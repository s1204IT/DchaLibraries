package mf.org.apache.xerces.impl.dv.xs;

import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.util.Base64;
import mf.org.apache.xerces.impl.dv.util.ByteListImpl;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class Base64BinaryDV extends TypeValidator {
    @Override
    public short getAllowedFacets() {
        return (short) 2079;
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        byte[] decoded = Base64.decode(content);
        if (decoded == null) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, SchemaSymbols.ATTVAL_BASE64BINARY});
        }
        return new XBase64(decoded);
    }

    @Override
    public int getDataLength(Object value) {
        return ((XBase64) value).getLength();
    }

    private static final class XBase64 extends ByteListImpl {
        public XBase64(byte[] data) {
            super(data);
        }

        @Override
        public synchronized String toString() {
            if (this.canonical == null) {
                this.canonical = Base64.encode(this.data);
            }
            return this.canonical;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof XBase64)) {
                return false;
            }
            byte[] odata = ((XBase64) obj).data;
            int len = this.data.length;
            if (len != odata.length) {
                return false;
            }
            for (int i = 0; i < len; i++) {
                if (this.data[i] != odata[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < this.data.length; i++) {
                hash = (hash * 37) + (this.data[i] & 255);
            }
            return hash;
        }
    }
}
