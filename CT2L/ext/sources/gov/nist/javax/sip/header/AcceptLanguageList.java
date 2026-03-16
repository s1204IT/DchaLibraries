package gov.nist.javax.sip.header;

public class AcceptLanguageList extends SIPHeaderList<AcceptLanguage> {
    private static final long serialVersionUID = -3289606805203488840L;

    @Override
    public Object clone() {
        AcceptLanguageList retval = new AcceptLanguageList();
        retval.clonehlist(this.hlist);
        return retval;
    }

    public AcceptLanguageList() {
        super(AcceptLanguage.class, "Accept-Language");
    }

    @Override
    public AcceptLanguage getFirst() {
        AcceptLanguage retval = (AcceptLanguage) super.getFirst();
        return retval != null ? retval : new AcceptLanguage();
    }

    @Override
    public AcceptLanguage getLast() {
        AcceptLanguage retval = (AcceptLanguage) super.getLast();
        return retval != null ? retval : new AcceptLanguage();
    }
}
