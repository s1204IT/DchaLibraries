package gov.nist.javax.sip.address;

import gov.nist.core.GenericObject;
import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.core.Separators;
import java.util.Iterator;

public class TelephoneNumber extends NetObject {
    public static final String ISUB = "isub";
    public static final String PHONE_CONTEXT_TAG = "context-tag";
    public static final String POSTDIAL = "postdial";
    public static final String PROVIDER_TAG = "provider-tag";
    protected boolean isglobal;
    protected NameValueList parameters = new NameValueList();
    protected String phoneNumber;

    public void deleteParm(String str) {
        this.parameters.delete(str);
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getPostDial() {
        return (String) this.parameters.getValue("postdial");
    }

    public String getIsdnSubaddress() {
        return (String) this.parameters.getValue("isub");
    }

    public boolean hasPostDial() {
        return this.parameters.getValue("postdial") != null;
    }

    public boolean hasParm(String str) {
        return this.parameters.hasNameValue(str);
    }

    public boolean hasIsdnSubaddress() {
        return hasParm("isub");
    }

    public boolean isGlobal() {
        return this.isglobal;
    }

    public void removePostDial() {
        this.parameters.delete("postdial");
    }

    public void removeIsdnSubaddress() {
        deleteParm("isub");
    }

    public void setParameters(NameValueList nameValueList) {
        this.parameters = nameValueList;
    }

    public void setGlobal(boolean z) {
        this.isglobal = z;
    }

    public void setPostDial(String str) {
        this.parameters.set(new NameValue("postdial", str));
    }

    public void setParm(String str, Object obj) {
        this.parameters.set(new NameValue(str, obj));
    }

    public void setIsdnSubaddress(String str) {
        setParm("isub", str);
    }

    public void setPhoneNumber(String str) {
        this.phoneNumber = str;
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        if (this.isglobal) {
            stringBuffer.append('+');
        }
        stringBuffer.append(this.phoneNumber);
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.parameters.encode(stringBuffer);
        }
        return stringBuffer;
    }

    public String getParameter(String str) {
        Object value = this.parameters.getValue(str);
        if (value == null) {
            return null;
        }
        if (value instanceof GenericObject) {
            return ((GenericObject) value).encode();
        }
        return value.toString();
    }

    public Iterator<String> getParameterNames() {
        return this.parameters.getNames();
    }

    public void removeParameter(String str) {
        this.parameters.delete(str);
    }

    public void setParameter(String str, String str2) {
        this.parameters.set(new NameValue(str, str2));
    }

    @Override
    public Object clone() {
        TelephoneNumber telephoneNumber = (TelephoneNumber) super.clone();
        if (this.parameters != null) {
            telephoneNumber.parameters = (NameValueList) this.parameters.clone();
        }
        return telephoneNumber;
    }

    public NameValueList getParameters() {
        return this.parameters;
    }
}
