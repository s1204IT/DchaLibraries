package gov.nist.javax.sip.header;

import java.util.Locale;
import javax.sip.header.ContentLanguageHeader;

public class ContentLanguage extends SIPHeader implements ContentLanguageHeader {
    private static final long serialVersionUID = -5195728427134181070L;
    protected Locale locale;

    public ContentLanguage() {
        super("Content-Language");
    }

    public ContentLanguage(String str) {
        super("Content-Language");
        setLanguageTag(str);
    }

    @Override
    public String encodeBody() {
        return getLanguageTag();
    }

    @Override
    public String getLanguageTag() {
        if ("".equals(this.locale.getCountry())) {
            return this.locale.getLanguage();
        }
        return this.locale.getLanguage() + '-' + this.locale.getCountry();
    }

    @Override
    public void setLanguageTag(String str) {
        int iIndexOf = str.indexOf(45);
        if (iIndexOf >= 0) {
            this.locale = new Locale(str.substring(0, iIndexOf), str.substring(iIndexOf + 1));
        } else {
            this.locale = new Locale(str);
        }
    }

    @Override
    public Locale getContentLanguage() {
        return this.locale;
    }

    @Override
    public void setContentLanguage(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Object clone() {
        ContentLanguage contentLanguage = (ContentLanguage) super.clone();
        if (this.locale != null) {
            contentLanguage.locale = (Locale) this.locale.clone();
        }
        return contentLanguage;
    }
}
