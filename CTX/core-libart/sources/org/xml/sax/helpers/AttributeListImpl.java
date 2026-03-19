package org.xml.sax.helpers;

import java.util.ArrayList;
import org.xml.sax.AttributeList;

@Deprecated
public class AttributeListImpl implements AttributeList {
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<String> types = new ArrayList<>();
    private ArrayList<String> values = new ArrayList<>();

    public AttributeListImpl() {
    }

    public AttributeListImpl(AttributeList attributeList) {
        setAttributeList(attributeList);
    }

    public void setAttributeList(AttributeList attributeList) {
        int length = attributeList.getLength();
        clear();
        for (int i = 0; i < length; i++) {
            addAttribute(attributeList.getName(i), attributeList.getType(i), attributeList.getValue(i));
        }
    }

    public void addAttribute(String str, String str2, String str3) {
        this.names.add(str);
        this.types.add(str2);
        this.values.add(str3);
    }

    public void removeAttribute(String str) {
        int iIndexOf = this.names.indexOf(str);
        if (iIndexOf != -1) {
            this.names.remove(iIndexOf);
            this.types.remove(iIndexOf);
            this.values.remove(iIndexOf);
        }
    }

    public void clear() {
        this.names.clear();
        this.types.clear();
        this.values.clear();
    }

    @Override
    public int getLength() {
        return this.names.size();
    }

    @Override
    public String getName(int i) {
        if (i < 0 || i >= this.names.size()) {
            return null;
        }
        return this.names.get(i);
    }

    @Override
    public String getType(int i) {
        if (i < 0 || i >= this.types.size()) {
            return null;
        }
        return this.types.get(i);
    }

    @Override
    public String getValue(int i) {
        if (i < 0 || i >= this.values.size()) {
            return null;
        }
        return this.values.get(i);
    }

    @Override
    public String getType(String str) {
        return getType(this.names.indexOf(str));
    }

    @Override
    public String getValue(String str) {
        return getValue(this.names.indexOf(str));
    }
}
