package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PChargingVector extends ParametersHeader implements PChargingVectorHeader, SIPHeaderNamesIms, ExtensionHeader {
    public PChargingVector() {
        super("P-Charging-Vector");
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        getNameValue(ParameterNamesIms.ICID_VALUE).encode(stringBuffer);
        if (this.parameters.containsKey(ParameterNamesIms.ICID_GENERATED_AT)) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(ParameterNamesIms.ICID_GENERATED_AT);
            stringBuffer.append(Separators.EQUALS);
            stringBuffer.append(getICIDGeneratedAt());
        }
        if (this.parameters.containsKey(ParameterNamesIms.TERM_IOI)) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(ParameterNamesIms.TERM_IOI);
            stringBuffer.append(Separators.EQUALS);
            stringBuffer.append(getTerminatingIOI());
        }
        if (this.parameters.containsKey(ParameterNamesIms.ORIG_IOI)) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(ParameterNamesIms.ORIG_IOI);
            stringBuffer.append(Separators.EQUALS);
            stringBuffer.append(getOriginatingIOI());
        }
        return stringBuffer.toString();
    }

    @Override
    public String getICID() {
        return getParameter(ParameterNamesIms.ICID_VALUE);
    }

    @Override
    public void setICID(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Vector, setICID(), the icid parameter is null.");
        }
        setParameter(ParameterNamesIms.ICID_VALUE, str);
    }

    @Override
    public String getICIDGeneratedAt() {
        return getParameter(ParameterNamesIms.ICID_GENERATED_AT);
    }

    @Override
    public void setICIDGeneratedAt(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Vector, setICIDGeneratedAt(), the host parameter is null.");
        }
        setParameter(ParameterNamesIms.ICID_GENERATED_AT, str);
    }

    @Override
    public String getOriginatingIOI() {
        return getParameter(ParameterNamesIms.ORIG_IOI);
    }

    @Override
    public void setOriginatingIOI(String str) throws ParseException {
        if (str == null || str.length() == 0) {
            removeParameter(ParameterNamesIms.ORIG_IOI);
        } else {
            setParameter(ParameterNamesIms.ORIG_IOI, str);
        }
    }

    @Override
    public String getTerminatingIOI() {
        return getParameter(ParameterNamesIms.TERM_IOI);
    }

    @Override
    public void setTerminatingIOI(String str) throws ParseException {
        if (str == null || str.length() == 0) {
            removeParameter(ParameterNamesIms.TERM_IOI);
        } else {
            setParameter(ParameterNamesIms.TERM_IOI, str);
        }
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }
}
