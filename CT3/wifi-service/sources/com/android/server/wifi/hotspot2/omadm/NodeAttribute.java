package com.android.server.wifi.hotspot2.omadm;

public class NodeAttribute {
    private final String mName;
    private final String mType;
    private final String mValue;

    public NodeAttribute(String name, String type, String value) {
        this.mName = name;
        this.mType = type;
        this.mValue = value;
    }

    public String getName() {
        return this.mName;
    }

    public String getValue() {
        return this.mValue;
    }

    public String getType() {
        return this.mType;
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        NodeAttribute that = (NodeAttribute) thatObject;
        if (this.mName.equals(that.mName) && this.mType.equals(that.mType)) {
            return this.mValue.equals(that.mValue);
        }
        return false;
    }

    public String toString() {
        return String.format("%s (%s) = '%s'", this.mName, this.mType, this.mValue);
    }
}
