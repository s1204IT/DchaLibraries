package mf.org.apache.xerces.impl.dv.xs;

import java.util.AbstractList;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.xs.datatypes.ObjectList;

public class ListDV extends TypeValidator {
    @Override
    public short getAllowedFacets() {
        return (short) 2079;
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        return content;
    }

    @Override
    public int getDataLength(Object value) {
        return ((ListData) value).getLength();
    }

    static final class ListData extends AbstractList implements ObjectList {
        private String canonical;
        final Object[] data;

        public ListData(Object[] data) {
            this.data = data;
        }

        @Override
        public synchronized String toString() {
            if (this.canonical == null) {
                int len = this.data.length;
                StringBuffer buf = new StringBuffer();
                if (len > 0) {
                    buf.append(this.data[0].toString());
                }
                for (int i = 1; i < len; i++) {
                    buf.append(' ');
                    buf.append(this.data[i].toString());
                }
                this.canonical = buf.toString();
            }
            return this.canonical;
        }

        @Override
        public int getLength() {
            return this.data.length;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ListData)) {
                return false;
            }
            Object[] odata = ((ListData) obj).data;
            int count = this.data.length;
            if (count != odata.length) {
                return false;
            }
            for (int i = 0; i < count; i++) {
                if (!this.data[i].equals(odata[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < this.data.length; i++) {
                hash ^= this.data[i].hashCode();
            }
            return hash;
        }

        @Override
        public boolean contains(Object item) {
            for (int i = 0; i < this.data.length; i++) {
                if (item == this.data[i]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Object item(int index) {
            if (index < 0 || index >= this.data.length) {
                return null;
            }
            return this.data[index];
        }

        @Override
        public Object get(int index) {
            if (index >= 0 && index < this.data.length) {
                return this.data[index];
            }
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        @Override
        public int size() {
            return getLength();
        }
    }
}
