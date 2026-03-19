package org.xml.sax.ext;

import libcore.util.EmptyArray;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public class Attributes2Impl extends AttributesImpl implements Attributes2 {
    private boolean[] declared;
    private boolean[] specified;

    public Attributes2Impl() {
        this.declared = EmptyArray.BOOLEAN;
        this.specified = EmptyArray.BOOLEAN;
    }

    public Attributes2Impl(Attributes attributes) {
        super(attributes);
    }

    @Override
    public boolean isDeclared(int i) {
        if (i < 0 || i >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + i);
        }
        return this.declared[i];
    }

    @Override
    public boolean isDeclared(String str, String str2) {
        int index = getIndex(str, str2);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: local=" + str2 + ", namespace=" + str);
        }
        return this.declared[index];
    }

    @Override
    public boolean isDeclared(String str) {
        int index = getIndex(str);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: " + str);
        }
        return this.declared[index];
    }

    @Override
    public boolean isSpecified(int i) {
        if (i < 0 || i >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + i);
        }
        return this.specified[i];
    }

    @Override
    public boolean isSpecified(String str, String str2) {
        int index = getIndex(str, str2);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: local=" + str2 + ", namespace=" + str);
        }
        return this.specified[index];
    }

    @Override
    public boolean isSpecified(String str) {
        int index = getIndex(str);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: " + str);
        }
        return this.specified[index];
    }

    @Override
    public void setAttributes(Attributes attributes) {
        int length = attributes.getLength();
        super.setAttributes(attributes);
        this.declared = new boolean[length];
        this.specified = new boolean[length];
        int i = 0;
        if (attributes instanceof Attributes2) {
            Attributes2 attributes2 = (Attributes2) attributes;
            while (i < length) {
                this.declared[i] = attributes2.isDeclared(i);
                this.specified[i] = attributes2.isSpecified(i);
                i++;
            }
            return;
        }
        while (i < length) {
            this.declared[i] = !"CDATA".equals(attributes.getType(i));
            this.specified[i] = true;
            i++;
        }
    }

    @Override
    public void addAttribute(String str, String str2, String str3, String str4, String str5) {
        super.addAttribute(str, str2, str3, str4, str5);
        int length = getLength();
        if (length > this.specified.length) {
            boolean[] zArr = new boolean[length];
            System.arraycopy(this.declared, 0, zArr, 0, this.declared.length);
            this.declared = zArr;
            boolean[] zArr2 = new boolean[length];
            System.arraycopy(this.specified, 0, zArr2, 0, this.specified.length);
            this.specified = zArr2;
        }
        int i = length - 1;
        this.specified[i] = true;
        this.declared[i] = true ^ "CDATA".equals(str4);
    }

    @Override
    public void removeAttribute(int i) {
        int length = getLength() - 1;
        super.removeAttribute(i);
        if (i != length) {
            int i2 = i + 1;
            int i3 = length - i;
            System.arraycopy(this.declared, i2, this.declared, i, i3);
            System.arraycopy(this.specified, i2, this.specified, i, i3);
        }
    }

    public void setDeclared(int i, boolean z) {
        if (i < 0 || i >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + i);
        }
        this.declared[i] = z;
    }

    public void setSpecified(int i, boolean z) {
        if (i < 0 || i >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + i);
        }
        this.specified[i] = z;
    }
}
