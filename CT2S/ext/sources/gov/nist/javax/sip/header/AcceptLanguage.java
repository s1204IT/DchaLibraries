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
        StringBuffer encoding = new StringBuffer();
        if (this.languageRange != null) {
            encoding.append(this.languageRange);
        }
        if (!this.parameters.isEmpty()) {
            encoding.append(Separators.SEMICOLON).append(this.parameters.encode());
        }
        return encoding.toString();
    }

    public String getLanguageRange() {
        return this.languageRange;
    }

    @Override
    public float getQValue() {
        if (hasParameter("q")) {
            return ((Float) this.parameters.getValue("q")).floatValue();
        }
        return -1.0f;
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
    public void setLanguageRange(String languageRange) {
        this.languageRange = languageRange.trim();
    }

    @Override
    public void setQValue(float q) throws InvalidArgumentException {
        if (q < 0.0d || q > 1.0d) {
            throw new InvalidArgumentException("qvalue out of range!");
        }
        if (q == -1.0f) {
            removeParameter("q");
        } else {
            setParameter(new NameValue("q", Float.valueOf(q)));
        }
    }

    @Override
    public Locale getAcceptLanguage() {
        if (this.languageRange == null) {
            return null;
        }
        int dash = this.languageRange.indexOf(45);
        if (dash >= 0) {
            return new Locale(this.languageRange.substring(0, dash), this.languageRange.substring(dash + 1));
        }
        return new Locale(this.languageRange);
    }

    @Override
    public void setAcceptLanguage(Locale language) {
        if ("".equals(language.getCountry())) {
            this.languageRange = language.getLanguage();
        } else {
            this.languageRange = language.getLanguage() + '-' + language.getCountry();
        }
    }
}
