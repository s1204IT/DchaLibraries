package gov.nist.javax.sip.address;

import gov.nist.core.NameValueList;
import gov.nist.core.Separators;
import java.text.ParseException;
import java.util.Iterator;
import javax.sip.address.TelURL;

public class TelURLImpl extends GenericURI implements TelURL {
    private static final long serialVersionUID = 5873527320305915954L;
    protected TelephoneNumber telephoneNumber;

    public TelURLImpl() {
        this.scheme = "tel";
    }

    public void setTelephoneNumber(TelephoneNumber telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    @Override
    public String getIsdnSubAddress() {
        return this.telephoneNumber.getIsdnSubaddress();
    }

    @Override
    public String getPostDial() {
        return this.telephoneNumber.getPostDial();
    }

    @Override
    public String getScheme() {
        return this.scheme;
    }

    @Override
    public boolean isGlobal() {
        return this.telephoneNumber.isGlobal();
    }

    @Override
    public boolean isSipURI() {
        return false;
    }

    @Override
    public void setGlobal(boolean global) {
        this.telephoneNumber.setGlobal(global);
    }

    @Override
    public void setIsdnSubAddress(String isdnSubAddress) {
        this.telephoneNumber.setIsdnSubaddress(isdnSubAddress);
    }

    @Override
    public void setPostDial(String postDial) {
        this.telephoneNumber.setPostDial(postDial);
    }

    @Override
    public void setPhoneNumber(String telephoneNumber) {
        this.telephoneNumber.setPhoneNumber(telephoneNumber);
    }

    @Override
    public String getPhoneNumber() {
        return this.telephoneNumber.getPhoneNumber();
    }

    @Override
    public String toString() {
        return this.scheme + Separators.COLON + this.telephoneNumber.encode();
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer buffer) {
        buffer.append(this.scheme).append(':');
        this.telephoneNumber.encode(buffer);
        return buffer;
    }

    @Override
    public Object clone() {
        TelURLImpl retval = (TelURLImpl) super.clone();
        if (this.telephoneNumber != null) {
            retval.telephoneNumber = (TelephoneNumber) this.telephoneNumber.clone();
        }
        return retval;
    }

    @Override
    public String getParameter(String parameterName) {
        return this.telephoneNumber.getParameter(parameterName);
    }

    @Override
    public void setParameter(String name, String value) {
        this.telephoneNumber.setParameter(name, value);
    }

    @Override
    public Iterator<String> getParameterNames() {
        return this.telephoneNumber.getParameterNames();
    }

    public NameValueList getParameters() {
        return this.telephoneNumber.getParameters();
    }

    @Override
    public void removeParameter(String name) {
        this.telephoneNumber.removeParameter(name);
    }

    @Override
    public void setPhoneContext(String phoneContext) throws ParseException {
        if (phoneContext == null) {
            removeParameter("phone-context");
        } else {
            setParameter("phone-context", phoneContext);
        }
    }

    @Override
    public String getPhoneContext() {
        return getParameter("phone-context");
    }
}
