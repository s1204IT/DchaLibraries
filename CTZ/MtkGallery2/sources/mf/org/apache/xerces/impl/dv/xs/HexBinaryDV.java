package mf.org.apache.xerces.impl.dv.xs;

import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.util.ByteListImpl;
import mf.org.apache.xerces.impl.dv.util.HexBin;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class HexBinaryDV extends TypeValidator {
    @Override
    public short getAllowedFacets() {
        return (short) 2079;
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        byte[] decoded = HexBin.decode(content);
        if (decoded == null) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, SchemaSymbols.ATTVAL_HEXBINARY});
        }
        return new XHex(decoded);
    }

    @Override
    public int getDataLength(Object value) {
        return ((XHex) value).getLength();
    }

    private static final class XHex extends ByteListImpl {
        public XHex(byte[] data) {
            super(data);
        }

        @Override
        public synchronized String toString() {
            if (this.canonical == null) {
                this.canonical = HexBin.encode(this.data);
            }
            return this.canonical;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof XHex)) {
                return false;
            }
            byte[] odata = ((XHex) obj).data;
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
