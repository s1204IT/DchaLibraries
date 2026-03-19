package gov.nist.javax.sip.header;

public class AcceptLanguageList extends SIPHeaderList<AcceptLanguage> {
    private static final long serialVersionUID = -3289606805203488840L;

    @Override
    public Object clone() {
        AcceptLanguageList acceptLanguageList = new AcceptLanguageList();
        acceptLanguageList.clonehlist(this.hlist);
        return acceptLanguageList;
    }

    public AcceptLanguageList() {
        super(AcceptLanguage.class, "Accept-Language");
    }

    @Override
    public AcceptLanguage getFirst() {
        AcceptLanguage acceptLanguage = (AcceptLanguage) super.getFirst();
        if (acceptLanguage != null) {
            return acceptLanguage;
        }
        return new AcceptLanguage();
    }

    @Override
    public AcceptLanguage getLast() {
        AcceptLanguage acceptLanguage = (AcceptLanguage) super.getLast();
        if (acceptLanguage != null) {
            return acceptLanguage;
        }
        return new AcceptLanguage();
    }
}
