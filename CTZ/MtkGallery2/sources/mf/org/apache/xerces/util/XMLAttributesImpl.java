package mf.org.apache.xerces.util;

import mf.org.apache.xerces.dom3.as.ASContentModel;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;

public class XMLAttributesImpl implements XMLAttributes {
    protected static final int SIZE_LIMIT = 20;
    protected static final int TABLE_SIZE = 101;
    protected Attribute[] fAttributeTableView;
    protected int[] fAttributeTableViewChainState;
    protected Attribute[] fAttributes;
    protected boolean fIsTableViewConsistent;
    protected int fLargeCount;
    protected int fLength;
    protected boolean fNamespaces;
    protected int fTableViewBuckets;

    public XMLAttributesImpl() {
        this(TABLE_SIZE);
    }

    public XMLAttributesImpl(int tableSize) {
        this.fNamespaces = true;
        this.fLargeCount = 1;
        this.fAttributes = new Attribute[4];
        this.fTableViewBuckets = tableSize;
        for (int i = 0; i < this.fAttributes.length; i++) {
            this.fAttributes[i] = new Attribute();
        }
    }

    public void setNamespaces(boolean namespaces) {
        this.fNamespaces = namespaces;
    }

    @Override
    public int addAttribute(QName name, String type, String value) {
        int index;
        int index2;
        if (this.fLength < 20) {
            if (name.uri != null && name.uri.length() != 0) {
                index = getIndexFast(name.uri, name.localpart);
            } else {
                index = getIndexFast(name.rawname);
            }
            if (index == -1) {
                index = this.fLength;
                int i = this.fLength;
                this.fLength = i + 1;
                if (i == this.fAttributes.length) {
                    Attribute[] attributes = new Attribute[this.fAttributes.length + 4];
                    System.arraycopy(this.fAttributes, 0, attributes, 0, this.fAttributes.length);
                    for (int i2 = this.fAttributes.length; i2 < attributes.length; i2++) {
                        attributes[i2] = new Attribute();
                    }
                    this.fAttributes = attributes;
                }
            }
        } else if (name.uri != null && name.uri.length() != 0) {
            int indexFast = getIndexFast(name.uri, name.localpart);
            index2 = indexFast;
            if (indexFast != -1) {
                index = index2;
            }
        } else {
            if (!this.fIsTableViewConsistent || this.fLength == 20) {
                prepareAndPopulateTableView();
                this.fIsTableViewConsistent = true;
            }
            int bucket = getTableViewBucket(name.rawname);
            if (this.fAttributeTableViewChainState[bucket] != this.fLargeCount) {
                int index3 = this.fLength;
                int i3 = this.fLength;
                this.fLength = i3 + 1;
                if (i3 == this.fAttributes.length) {
                    Attribute[] attributes2 = new Attribute[this.fAttributes.length << 1];
                    System.arraycopy(this.fAttributes, 0, attributes2, 0, this.fAttributes.length);
                    for (int i4 = this.fAttributes.length; i4 < attributes2.length; i4++) {
                        attributes2[i4] = new Attribute();
                    }
                    this.fAttributes = attributes2;
                }
                this.fAttributeTableViewChainState[bucket] = this.fLargeCount;
                this.fAttributes[index3].next = null;
                this.fAttributeTableView[bucket] = this.fAttributes[index3];
                index = index3;
            } else {
                Attribute found = this.fAttributeTableView[bucket];
                while (found != null && found.name.rawname != name.rawname) {
                    found = found.next;
                }
                if (found == null) {
                    index2 = this.fLength;
                    int i5 = this.fLength;
                    this.fLength = i5 + 1;
                    if (i5 == this.fAttributes.length) {
                        Attribute[] attributes3 = new Attribute[this.fAttributes.length << 1];
                        System.arraycopy(this.fAttributes, 0, attributes3, 0, this.fAttributes.length);
                        for (int i6 = this.fAttributes.length; i6 < attributes3.length; i6++) {
                            attributes3[i6] = new Attribute();
                        }
                        this.fAttributes = attributes3;
                    }
                    this.fAttributes[index2].next = this.fAttributeTableView[bucket];
                    this.fAttributeTableView[bucket] = this.fAttributes[index2];
                    index = index2;
                } else {
                    index = getIndexFast(name.rawname);
                }
            }
        }
        Attribute attribute = this.fAttributes[index];
        attribute.name.setValues(name);
        attribute.type = type;
        attribute.value = value;
        attribute.nonNormalizedValue = value;
        attribute.specified = false;
        attribute.augs.removeAllItems();
        return index;
    }

    @Override
    public void removeAllAttributes() {
        this.fLength = 0;
    }

    @Override
    public void removeAttributeAt(int attrIndex) {
        this.fIsTableViewConsistent = false;
        if (attrIndex < this.fLength - 1) {
            Attribute removedAttr = this.fAttributes[attrIndex];
            System.arraycopy(this.fAttributes, attrIndex + 1, this.fAttributes, attrIndex, (this.fLength - attrIndex) - 1);
            this.fAttributes[this.fLength - 1] = removedAttr;
        }
        this.fLength--;
    }

    @Override
    public void setName(int attrIndex, QName attrName) {
        this.fAttributes[attrIndex].name.setValues(attrName);
    }

    @Override
    public void getName(int attrIndex, QName attrName) {
        attrName.setValues(this.fAttributes[attrIndex].name);
    }

    @Override
    public void setType(int attrIndex, String attrType) {
        this.fAttributes[attrIndex].type = attrType;
    }

    @Override
    public void setValue(int attrIndex, String attrValue) {
        Attribute attribute = this.fAttributes[attrIndex];
        attribute.value = attrValue;
        attribute.nonNormalizedValue = attrValue;
    }

    @Override
    public void setNonNormalizedValue(int attrIndex, String attrValue) {
        if (attrValue == null) {
            attrValue = this.fAttributes[attrIndex].value;
        }
        this.fAttributes[attrIndex].nonNormalizedValue = attrValue;
    }

    @Override
    public String getNonNormalizedValue(int attrIndex) {
        String value = this.fAttributes[attrIndex].nonNormalizedValue;
        return value;
    }

    @Override
    public void setSpecified(int attrIndex, boolean specified) {
        this.fAttributes[attrIndex].specified = specified;
    }

    @Override
    public boolean isSpecified(int attrIndex) {
        return this.fAttributes[attrIndex].specified;
    }

    @Override
    public int getLength() {
        return this.fLength;
    }

    @Override
    public String getType(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        return getReportableType(this.fAttributes[index].type);
    }

    @Override
    public String getType(String qname) {
        int index = getIndex(qname);
        if (index != -1) {
            return getReportableType(this.fAttributes[index].type);
        }
        return null;
    }

    @Override
    public String getValue(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        return this.fAttributes[index].value;
    }

    @Override
    public String getValue(String qname) {
        int index = getIndex(qname);
        if (index != -1) {
            return this.fAttributes[index].value;
        }
        return null;
    }

    public String getName(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        return this.fAttributes[index].name.rawname;
    }

    @Override
    public int getIndex(String qName) {
        for (int i = 0; i < this.fLength; i++) {
            Attribute attribute = this.fAttributes[i];
            if (attribute.name.rawname != null && attribute.name.rawname.equals(qName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getIndex(String uri, String localPart) {
        for (int i = 0; i < this.fLength; i++) {
            Attribute attribute = this.fAttributes[i];
            if (attribute.name.localpart != null && attribute.name.localpart.equals(localPart) && (uri == attribute.name.uri || (uri != null && attribute.name.uri != null && attribute.name.uri.equals(uri)))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getLocalName(int index) {
        if (!this.fNamespaces) {
            return "";
        }
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        return this.fAttributes[index].name.localpart;
    }

    @Override
    public String getQName(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        String rawname = this.fAttributes[index].name.rawname;
        return rawname != null ? rawname : "";
    }

    @Override
    public String getType(String uri, String localName) {
        int index;
        if (this.fNamespaces && (index = getIndex(uri, localName)) != -1) {
            return getReportableType(this.fAttributes[index].type);
        }
        return null;
    }

    @Override
    public String getPrefix(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        String prefix = this.fAttributes[index].name.prefix;
        return prefix != null ? prefix : "";
    }

    @Override
    public String getURI(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        String uri = this.fAttributes[index].name.uri;
        return uri;
    }

    @Override
    public String getValue(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index != -1) {
            return getValue(index);
        }
        return null;
    }

    @Override
    public Augmentations getAugmentations(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index != -1) {
            return this.fAttributes[index].augs;
        }
        return null;
    }

    @Override
    public Augmentations getAugmentations(String qName) {
        int index = getIndex(qName);
        if (index != -1) {
            return this.fAttributes[index].augs;
        }
        return null;
    }

    @Override
    public Augmentations getAugmentations(int attributeIndex) {
        if (attributeIndex < 0 || attributeIndex >= this.fLength) {
            return null;
        }
        return this.fAttributes[attributeIndex].augs;
    }

    @Override
    public void setAugmentations(int attrIndex, Augmentations augs) {
        this.fAttributes[attrIndex].augs = augs;
    }

    public void setURI(int attrIndex, String uri) {
        this.fAttributes[attrIndex].name.uri = uri;
    }

    public int getIndexFast(String qName) {
        for (int i = 0; i < this.fLength; i++) {
            Attribute attribute = this.fAttributes[i];
            if (attribute.name.rawname == qName) {
                return i;
            }
        }
        return -1;
    }

    public void addAttributeNS(QName name, String type, String value) {
        Attribute[] attributes;
        int index = this.fLength;
        int i = this.fLength;
        this.fLength = i + 1;
        if (i == this.fAttributes.length) {
            if (this.fLength < 20) {
                attributes = new Attribute[this.fAttributes.length + 4];
            } else {
                Attribute[] attributes2 = this.fAttributes;
                attributes = new Attribute[attributes2.length << 1];
            }
            System.arraycopy(this.fAttributes, 0, attributes, 0, this.fAttributes.length);
            for (int i2 = this.fAttributes.length; i2 < attributes.length; i2++) {
                attributes[i2] = new Attribute();
            }
            this.fAttributes = attributes;
        }
        Attribute[] attributes3 = this.fAttributes;
        Attribute attribute = attributes3[index];
        attribute.name.setValues(name);
        attribute.type = type;
        attribute.value = value;
        attribute.nonNormalizedValue = value;
        attribute.specified = false;
        attribute.augs.removeAllItems();
    }

    public QName checkDuplicatesNS() {
        if (this.fLength <= 20) {
            for (int i = 0; i < this.fLength - 1; i++) {
                Attribute att1 = this.fAttributes[i];
                for (int j = i + 1; j < this.fLength; j++) {
                    Attribute att2 = this.fAttributes[j];
                    if (att1.name.localpart == att2.name.localpart && att1.name.uri == att2.name.uri) {
                        return att2.name;
                    }
                }
            }
        } else {
            this.fIsTableViewConsistent = false;
            prepareTableView();
            for (int i2 = this.fLength - 1; i2 >= 0; i2--) {
                Attribute attr = this.fAttributes[i2];
                int bucket = getTableViewBucket(attr.name.localpart, attr.name.uri);
                if (this.fAttributeTableViewChainState[bucket] != this.fLargeCount) {
                    this.fAttributeTableViewChainState[bucket] = this.fLargeCount;
                    attr.next = null;
                    this.fAttributeTableView[bucket] = attr;
                } else {
                    for (Attribute found = this.fAttributeTableView[bucket]; found != null; found = found.next) {
                        if (found.name.localpart == attr.name.localpart && found.name.uri == attr.name.uri) {
                            return attr.name;
                        }
                    }
                    attr.next = this.fAttributeTableView[bucket];
                    this.fAttributeTableView[bucket] = attr;
                }
            }
        }
        return null;
    }

    public int getIndexFast(String uri, String localPart) {
        for (int i = 0; i < this.fLength; i++) {
            Attribute attribute = this.fAttributes[i];
            if (attribute.name.localpart == localPart && attribute.name.uri == uri) {
                return i;
            }
        }
        return -1;
    }

    private String getReportableType(String type) {
        if (type.charAt(0) == '(') {
            return SchemaSymbols.ATTVAL_NMTOKEN;
        }
        return type;
    }

    protected int getTableViewBucket(String qname) {
        return (qname.hashCode() & ASContentModel.AS_UNBOUNDED) % this.fTableViewBuckets;
    }

    protected int getTableViewBucket(String localpart, String uri) {
        if (uri == null) {
            return (Integer.MAX_VALUE & localpart.hashCode()) % this.fTableViewBuckets;
        }
        return (Integer.MAX_VALUE & (localpart.hashCode() + uri.hashCode())) % this.fTableViewBuckets;
    }

    protected void cleanTableView() {
        int i = this.fLargeCount + 1;
        this.fLargeCount = i;
        if (i < 0) {
            if (this.fAttributeTableViewChainState != null) {
                for (int i2 = this.fTableViewBuckets - 1; i2 >= 0; i2--) {
                    this.fAttributeTableViewChainState[i2] = 0;
                }
            }
            this.fLargeCount = 1;
        }
    }

    protected void prepareTableView() {
        if (this.fAttributeTableView == null) {
            this.fAttributeTableView = new Attribute[this.fTableViewBuckets];
            this.fAttributeTableViewChainState = new int[this.fTableViewBuckets];
        } else {
            cleanTableView();
        }
    }

    protected void prepareAndPopulateTableView() {
        prepareTableView();
        for (int i = 0; i < this.fLength; i++) {
            Attribute attr = this.fAttributes[i];
            int bucket = getTableViewBucket(attr.name.rawname);
            if (this.fAttributeTableViewChainState[bucket] != this.fLargeCount) {
                this.fAttributeTableViewChainState[bucket] = this.fLargeCount;
                attr.next = null;
                this.fAttributeTableView[bucket] = attr;
            } else {
                attr.next = this.fAttributeTableView[bucket];
                this.fAttributeTableView[bucket] = attr;
            }
        }
    }

    static class Attribute {
        public Attribute next;
        public String nonNormalizedValue;
        public boolean specified;
        public String type;
        public String value;
        public final QName name = new QName();
        public Augmentations augs = new AugmentationsImpl();

        Attribute() {
        }
    }
}
