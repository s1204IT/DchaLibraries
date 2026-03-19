package org.apache.xml.serializer;

import java.util.Hashtable;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public final class AttributesImplSerializer extends AttributesImpl {
    private static final int MAX = 12;
    private static final int MAXMinus1 = 11;
    private final Hashtable m_indexFromQName = new Hashtable();
    private final StringBuffer m_buff = new StringBuffer();

    @Override
    public final int getIndex(String str) {
        if (super.getLength() < 12) {
            return super.getIndex(str);
        }
        Integer num = (Integer) this.m_indexFromQName.get(str);
        if (num == null) {
            return -1;
        }
        return num.intValue();
    }

    @Override
    public final void addAttribute(String str, String str2, String str3, String str4, String str5) {
        int length = super.getLength();
        super.addAttribute(str, str2, str3, str4, str5);
        if (length < 11) {
            return;
        }
        if (length == 11) {
            switchOverToHash(12);
            return;
        }
        Integer num = new Integer(length);
        this.m_indexFromQName.put(str3, num);
        this.m_buff.setLength(0);
        StringBuffer stringBuffer = this.m_buff;
        stringBuffer.append('{');
        stringBuffer.append(str);
        stringBuffer.append('}');
        stringBuffer.append(str2);
        this.m_indexFromQName.put(this.m_buff.toString(), num);
    }

    private void switchOverToHash(int i) {
        for (int i2 = 0; i2 < i; i2++) {
            String qName = super.getQName(i2);
            Integer num = new Integer(i2);
            this.m_indexFromQName.put(qName, num);
            String uri = super.getURI(i2);
            String localName = super.getLocalName(i2);
            this.m_buff.setLength(0);
            StringBuffer stringBuffer = this.m_buff;
            stringBuffer.append('{');
            stringBuffer.append(uri);
            stringBuffer.append('}');
            stringBuffer.append(localName);
            this.m_indexFromQName.put(this.m_buff.toString(), num);
        }
    }

    @Override
    public final void clear() {
        int length = super.getLength();
        super.clear();
        if (12 <= length) {
            this.m_indexFromQName.clear();
        }
    }

    @Override
    public final void setAttributes(Attributes attributes) {
        super.setAttributes(attributes);
        int length = attributes.getLength();
        if (12 <= length) {
            switchOverToHash(length);
        }
    }

    @Override
    public final int getIndex(String str, String str2) {
        if (super.getLength() < 12) {
            return super.getIndex(str, str2);
        }
        this.m_buff.setLength(0);
        StringBuffer stringBuffer = this.m_buff;
        stringBuffer.append('{');
        stringBuffer.append(str);
        stringBuffer.append('}');
        stringBuffer.append(str2);
        Integer num = (Integer) this.m_indexFromQName.get(this.m_buff.toString());
        if (num == null) {
            return -1;
        }
        return num.intValue();
    }
}
