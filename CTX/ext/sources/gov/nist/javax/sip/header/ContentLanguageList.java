package gov.nist.javax.sip.header;

public final class ContentLanguageList extends SIPHeaderList<ContentLanguage> {
    private static final long serialVersionUID = -5302265987802886465L;

    @Override
    public Object clone() {
        ContentLanguageList contentLanguageList = new ContentLanguageList();
        contentLanguageList.clonehlist(this.hlist);
        return contentLanguageList;
    }

    public ContentLanguageList() {
        super(ContentLanguage.class, "Content-Language");
    }
}
