package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.GenericURI;
import java.text.ParseException;
import javax.sip.address.URI;
import javax.sip.header.AlertInfoHeader;

public final class AlertInfo extends ParametersHeader implements AlertInfoHeader {
    private static final long serialVersionUID = 4159657362051508719L;
    protected String string;
    protected GenericURI uri;

    public AlertInfo() {
        super("Alert-Info");
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.uri != null) {
            stringBuffer.append(Separators.LESS_THAN);
            stringBuffer.append(this.uri.encode());
            stringBuffer.append(Separators.GREATER_THAN);
        } else if (this.string != null) {
            stringBuffer.append(this.string);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(this.parameters.encode());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setAlertInfo(URI uri) {
        this.uri = (GenericURI) uri;
    }

    @Override
    public void setAlertInfo(String str) {
        this.string = str;
    }

    @Override
    public URI getAlertInfo() {
        if (this.uri != null) {
            return this.uri;
        }
        try {
            return new GenericURI(this.string);
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public Object clone() {
        AlertInfo alertInfo = (AlertInfo) super.clone();
        if (this.uri != null) {
            alertInfo.uri = (GenericURI) this.uri.clone();
        } else if (this.string != null) {
            alertInfo.string = this.string;
        }
        return alertInfo;
    }
}
