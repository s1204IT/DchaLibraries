package gov.nist.javax.sip.header;

import gov.nist.core.NameValue;
import gov.nist.core.Separators;
import java.util.Locale;
import javax.sip.InvalidArgumentException;
import javax.sip.header.AcceptLanguageHeader;

public final class AcceptLanguage extends ParametersHeader implements AcceptLanguageHeader {
    private static final long serialVersionUID = -4473982069737324919L;
    protected String languageRange;

    public AcceptLanguage() {
        super("Accept-Language");
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.languageRange != null) {
            stringBuffer.append(this.languageRange);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(this.parameters.encode());
        }
        return stringBuffer.toString();
    }

    public String getLanguageRange() {
        return this.languageRange;
    }

    @Override
    public float getQValue() {
        if (!hasParameter("q")) {
            return -1.0f;
        }
        return ((Float) this.parameters.getValue("q")).floatValue();
    }

    @Override
    public boolean hasQValue() {
        return hasParameter("q");
    }

    @Override
    public void removeQValue() {
        removeParameter("q");
    }

    @Override
    public void setLanguageRange(String str) {
        this.languageRange = str.trim();
    }

    @Override
    public void setQValue(float f) throws InvalidArgumentException {
        double d = f;
        if (d < 0.0d || d > 1.0d) {
            throw new InvalidArgumentException("qvalue out of range!");
        }
        if (f == -1.0f) {
            removeParameter("q");
        } else {
            setParameter(new NameValue("q", Float.valueOf(f)));
        }
    }

    @Override
    public Locale getAcceptLanguage() {
        if (this.languageRange == null) {
            return null;
        }
        int iIndexOf = this.languageRange.indexOf(45);
        if (iIndexOf >= 0) {
            return new Locale(this.languageRange.substring(0, iIndexOf), this.languageRange.substring(iIndexOf + 1));
        }
        return new Locale(this.languageRange);
    }

    @Override
    public void setAcceptLanguage(Locale locale) {
        if ("".equals(locale.getCountry())) {
            this.languageRange = locale.getLanguage();
            return;
        }
        this.languageRange = locale.getLanguage() + '-' + locale.getCountry();
    }
}
