package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.AddressParametersHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.ExtensionHeader;

public class PServedUser extends AddressParametersHeader implements PServedUserHeader, SIPHeaderNamesIms, ExtensionHeader {
    public PServedUser(AddressImpl addressImpl) {
        super("P-Served-User");
        this.address = addressImpl;
    }

    public PServedUser() {
        super("P-Served-User");
    }

    @Override
    public String getRegistrationState() {
        return getParameter(ParameterNamesIms.REGISTRATION_STATE);
    }

    @Override
    public String getSessionCase() {
        return getParameter(ParameterNamesIms.SESSION_CASE);
    }

    @Override
    public void setRegistrationState(String str) {
        if (str != null) {
            if (str.equals("reg") || str.equals("unreg")) {
                try {
                    setParameter(ParameterNamesIms.REGISTRATION_STATE, str);
                    return;
                } catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }
            }
            try {
                throw new InvalidArgumentException("Value can be either reg or unreg");
            } catch (InvalidArgumentException e2) {
                e2.printStackTrace();
                return;
            }
        }
        throw new NullPointerException("regstate Parameter value is null");
    }

    @Override
    public void setSessionCase(String str) {
        if (str != null) {
            if (str.equals("orig") || str.equals("term")) {
                try {
                    setParameter(ParameterNamesIms.SESSION_CASE, str);
                    return;
                } catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }
            }
            try {
                throw new InvalidArgumentException("Value can be either orig or term");
            } catch (InvalidArgumentException e2) {
                e2.printStackTrace();
                return;
            }
        }
        throw new NullPointerException("sess-case Parameter value is null");
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.address.encode());
        if (this.parameters.containsKey(ParameterNamesIms.REGISTRATION_STATE)) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(ParameterNamesIms.REGISTRATION_STATE);
            stringBuffer.append(Separators.EQUALS);
            stringBuffer.append(getRegistrationState());
        }
        if (this.parameters.containsKey(ParameterNamesIms.SESSION_CASE)) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(ParameterNamesIms.SESSION_CASE);
            stringBuffer.append(Separators.EQUALS);
            stringBuffer.append(getSessionCase());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PServedUser) {
            return getAddress().equals(((PServedUser) obj).getAddress());
        }
        return false;
    }

    @Override
    public Object clone() {
        return (PServedUser) super.clone();
    }
}
