package gov.nist.javax.sip.header;

import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.address.Address;
import javax.sip.header.ContactHeader;

public final class Contact extends AddressParametersHeader implements ContactHeader {
    public static final String ACTION = "action";
    public static final String EXPIRES = "expires";
    public static final String PROXY = "proxy";
    public static final String Q = "q";
    public static final String REDIRECT = "redirect";
    private static final long serialVersionUID = 1677294871695706288L;
    private ContactList contactList;
    protected boolean wildCardFlag;

    public Contact() {
        super("Contact");
    }

    @Override
    public void setParameter(String str, String str2) throws ParseException {
        NameValue nameValue = this.parameters.getNameValue(str);
        if (nameValue != null) {
            nameValue.setValueAsObject(str2);
            return;
        }
        NameValue nameValue2 = new NameValue(str, str2);
        if (str.equalsIgnoreCase("methods")) {
            nameValue2.setQuotedValue();
        }
        this.parameters.set(nameValue2);
    }

    @Override
    protected String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (this.wildCardFlag) {
            stringBuffer.append('*');
        } else {
            if (this.address.getAddressType() == 1) {
                this.address.encode(stringBuffer);
            } else {
                stringBuffer.append('<');
                this.address.encode(stringBuffer);
                stringBuffer.append('>');
            }
            if (!this.parameters.isEmpty()) {
                stringBuffer.append(Separators.SEMICOLON);
                this.parameters.encode(stringBuffer);
            }
        }
        return stringBuffer;
    }

    public ContactList getContactList() {
        return this.contactList;
    }

    public boolean getWildCardFlag() {
        return this.wildCardFlag;
    }

    @Override
    public Address getAddress() {
        return this.address;
    }

    public NameValueList getContactParms() {
        return this.parameters;
    }

    @Override
    public int getExpires() {
        return getParameterAsInt("expires");
    }

    @Override
    public void setExpires(int i) {
        this.parameters.set("expires", Integer.valueOf(i));
    }

    @Override
    public float getQValue() {
        return getParameterAsFloat("q");
    }

    public void setContactList(ContactList contactList) {
        this.contactList = contactList;
    }

    @Override
    public void setWildCardFlag(boolean z) {
        this.wildCardFlag = true;
        this.address = new AddressImpl();
        this.address.setWildCardFlag();
    }

    @Override
    public void setAddress(Address address) {
        if (address == null) {
            throw new NullPointerException("null address");
        }
        this.address = (AddressImpl) address;
        this.wildCardFlag = false;
    }

    @Override
    public void setQValue(float f) throws InvalidArgumentException {
        if (f != -1.0f && (f < 0.0f || f > 1.0f)) {
            throw new InvalidArgumentException("JAIN-SIP Exception, Contact, setQValue(), the qValue is not between 0 and 1");
        }
        this.parameters.set("q", Float.valueOf(f));
    }

    @Override
    public Object clone() {
        Contact contact = (Contact) super.clone();
        if (this.contactList != null) {
            contact.contactList = (ContactList) this.contactList.clone();
        }
        return contact;
    }

    @Override
    public void setWildCard() {
        setWildCardFlag(true);
    }

    @Override
    public boolean isWildCard() {
        return this.address.isWildcard();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ContactHeader) && super.equals(obj);
    }

    public void removeSipInstanceParam() {
        if (this.parameters != null) {
            this.parameters.delete(ParameterNames.SIP_INSTANCE);
        }
    }

    public String getSipInstanceParam() {
        return (String) this.parameters.getValue(ParameterNames.SIP_INSTANCE);
    }

    public void setSipInstanceParam(String str) {
        this.parameters.set(ParameterNames.SIP_INSTANCE, str);
    }

    public void removePubGruuParam() {
        if (this.parameters != null) {
            this.parameters.delete(ParameterNames.PUB_GRUU);
        }
    }

    public String getPubGruuParam() {
        return (String) this.parameters.getValue(ParameterNames.PUB_GRUU);
    }

    public void setPubGruuParam(String str) {
        this.parameters.set(ParameterNames.PUB_GRUU, str);
    }

    public void removeTempGruuParam() {
        if (this.parameters != null) {
            this.parameters.delete(ParameterNames.TEMP_GRUU);
        }
    }

    public String getTempGruuParam() {
        return (String) this.parameters.getValue(ParameterNames.TEMP_GRUU);
    }

    public void setTempGruuParam(String str) {
        this.parameters.set(ParameterNames.TEMP_GRUU, str);
    }
}
