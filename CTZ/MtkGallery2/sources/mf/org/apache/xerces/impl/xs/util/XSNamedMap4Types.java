package mf.org.apache.xerces.impl.xs.util;

import mf.org.apache.xerces.util.SymbolHash;
import mf.org.apache.xerces.xs.XSObject;
import mf.org.apache.xerces.xs.XSTypeDefinition;

public final class XSNamedMap4Types extends XSNamedMapImpl {
    private final short fType;

    public XSNamedMap4Types(String namespace, SymbolHash map, short type) {
        super(namespace, map);
        this.fType = type;
    }

    public XSNamedMap4Types(String[] namespaces, SymbolHash[] maps, int num, short type) {
        super(namespaces, maps, num);
        this.fType = type;
    }

    @Override
    public synchronized int getLength() {
        int length;
        if (this.fLength == -1) {
            int length2 = 0;
            for (int i = 0; i < this.fNSNum; i++) {
                length2 += this.fMaps[i].getLength();
            }
            int pos = 0;
            XSObject[] array = new XSObject[length2];
            for (int i2 = 0; i2 < this.fNSNum; i2++) {
                pos += this.fMaps[i2].getValues(array, pos);
            }
            this.fLength = 0;
            this.fArray = new XSObject[length2];
            for (int i3 = 0; i3 < length2; i3++) {
                XSTypeDefinition type = (XSTypeDefinition) array[i3];
                if (type.getTypeCategory() == this.fType) {
                    XSObject[] xSObjectArr = this.fArray;
                    int i4 = this.fLength;
                    this.fLength = i4 + 1;
                    xSObjectArr[i4] = type;
                }
            }
        }
        length = this.fLength;
        return length;
    }

    @Override
    public XSObject itemByName(String namespace, String localName) {
        for (int i = 0; i < this.fNSNum; i++) {
            if (isEqual(namespace, this.fNamespaces[i])) {
                XSTypeDefinition type = (XSTypeDefinition) this.fMaps[i].get(localName);
                if (type == null || type.getTypeCategory() != this.fType) {
                    return null;
                }
                return type;
            }
        }
        return null;
    }

    @Override
    public synchronized XSObject item(int index) {
        if (this.fArray == null) {
            getLength();
        }
        if (index >= 0 && index < this.fLength) {
            return this.fArray[index];
        }
        return null;
    }
}
