package mf.org.apache.xerces.impl.dv;

import mf.org.apache.xerces.impl.xs.util.ShortListImpl;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.xs.ShortList;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.apache.xerces.xs.XSValue;

public class ValidatedInfo implements XSValue {
    public XSSimpleType actualType;
    public Object actualValue;
    public short actualValueType;
    public ShortList itemValueTypes;
    public XSSimpleType memberType;
    public XSSimpleType[] memberTypes;
    public String normalizedValue;

    public void reset() {
        this.normalizedValue = null;
        this.actualValue = null;
        this.actualValueType = (short) 45;
        this.actualType = null;
        this.memberType = null;
        this.memberTypes = null;
        this.itemValueTypes = null;
    }

    public String stringValue() {
        if (this.actualValue == null) {
            return this.normalizedValue;
        }
        return this.actualValue.toString();
    }

    public static boolean isComparable(ValidatedInfo info1, ValidatedInfo info2) {
        short primitiveType1 = convertToPrimitiveKind(info1.actualValueType);
        short primitiveType2 = convertToPrimitiveKind(info2.actualValueType);
        if (primitiveType1 != primitiveType2) {
            return (primitiveType1 == 1 && primitiveType2 == 2) || (primitiveType1 == 2 && primitiveType2 == 1);
        }
        if (primitiveType1 == 44 || primitiveType1 == 43) {
            ShortList typeList1 = info1.itemValueTypes;
            ShortList typeList2 = info2.itemValueTypes;
            int typeList1Length = typeList1 != null ? typeList1.getLength() : 0;
            int typeList2Length = typeList2 != null ? typeList2.getLength() : 0;
            if (typeList1Length != typeList2Length) {
                return false;
            }
            for (int i = 0; i < typeList1Length; i++) {
                short primitiveItem1 = convertToPrimitiveKind(typeList1.item(i));
                short primitiveItem2 = convertToPrimitiveKind(typeList2.item(i));
                if (primitiveItem1 != primitiveItem2 && ((primitiveItem1 != 1 || primitiveItem2 != 2) && (primitiveItem1 != 2 || primitiveItem2 != 1))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static short convertToPrimitiveKind(short valueType) {
        if (valueType <= 20) {
            return valueType;
        }
        if (valueType <= 29) {
            return (short) 2;
        }
        if (valueType <= 42) {
            return (short) 4;
        }
        return valueType;
    }

    @Override
    public Object getActualValue() {
        return this.actualValue;
    }

    @Override
    public short getActualValueType() {
        return this.actualValueType;
    }

    @Override
    public ShortList getListValueTypes() {
        return this.itemValueTypes == null ? ShortListImpl.EMPTY_LIST : this.itemValueTypes;
    }

    @Override
    public XSObjectList getMemberTypeDefinitions() {
        if (this.memberTypes == null) {
            return XSObjectListImpl.EMPTY_LIST;
        }
        return new XSObjectListImpl(this.memberTypes, this.memberTypes.length);
    }

    @Override
    public String getNormalizedValue() {
        return this.normalizedValue;
    }

    @Override
    public XSSimpleTypeDefinition getTypeDefinition() {
        return this.actualType;
    }

    @Override
    public XSSimpleTypeDefinition getMemberTypeDefinition() {
        return this.memberType;
    }

    public void copyFrom(XSValue xSValue) {
        if (xSValue == 0) {
            reset();
            return;
        }
        if (xSValue instanceof ValidatedInfo) {
            this.normalizedValue = xSValue.normalizedValue;
            this.actualValue = xSValue.actualValue;
            this.actualValueType = xSValue.actualValueType;
            this.actualType = xSValue.actualType;
            this.memberType = xSValue.memberType;
            this.memberTypes = xSValue.memberTypes;
            this.itemValueTypes = xSValue.itemValueTypes;
            return;
        }
        this.normalizedValue = xSValue.getNormalizedValue();
        this.actualValue = xSValue.getActualValue();
        this.actualValueType = xSValue.getActualValueType();
        this.actualType = (XSSimpleType) xSValue.getTypeDefinition();
        this.memberType = (XSSimpleType) xSValue.getMemberTypeDefinition();
        XSSimpleType realType = this.memberType == null ? this.actualType : this.memberType;
        if (realType != null && realType.getBuiltInKind() == 43) {
            XSObjectList members = xSValue.getMemberTypeDefinitions();
            this.memberTypes = new XSSimpleType[members.getLength()];
            for (int i = 0; i < members.getLength(); i++) {
                this.memberTypes[i] = (XSSimpleType) members.get(i);
            }
        } else {
            this.memberTypes = null;
        }
        this.itemValueTypes = xSValue.getListValueTypes();
    }
}
