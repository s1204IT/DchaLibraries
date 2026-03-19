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
    public void setGlobal(boolean z) {
        this.telephoneNumber.setGlobal(z);
    }

    @Override
    public void setIsdnSubAddress(String str) {
        this.telephoneNumber.setIsdnSubaddress(str);
    }

    @Override
    public void setPostDial(String str) {
        this.telephoneNumber.setPostDial(str);
    }

    @Override
    public void setPhoneNumber(String str) {
        this.telephoneNumber.setPhoneNumber(str);
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
    public StringBuffer encode(StringBuffer stringBuffer) {
        stringBuffer.append(this.scheme);
        stringBuffer.append(':');
        this.telephoneNumber.encode(stringBuffer);
        return stringBuffer;
    }

    @Override
    public Object clone() {
        TelURLImpl telURLImpl = (TelURLImpl) super.clone();
        if (this.telephoneNumber != null) {
            telURLImpl.telephoneNumber = (TelephoneNumber) this.telephoneNumber.clone();
        }
        return telURLImpl;
    }

    @Override
    public String getParameter(String str) {
        return this.telephoneNumber.getParameter(str);
    }

    @Override
    public void setParameter(String str, String str2) {
        this.telephoneNumber.setParameter(str, str2);
    }

    @Override
    public Iterator<String> getParameterNames() {
        return this.telephoneNumber.getParameterNames();
    }

    public NameValueList getParameters() {
        return this.telephoneNumber.getParameters();
    }

    @Override
    public void removeParameter(String str) {
        this.telephoneNumber.removeParameter(str);
    }

    @Override
    public void setPhoneContext(String str) throws ParseException {
        if (str == null) {
            removeParameter("phone-context");
        } else {
            setParameter("phone-context", str);
        }
    }

    @Override
    public String getPhoneContext() {
        return getParameter("phone-context");
    }
}
