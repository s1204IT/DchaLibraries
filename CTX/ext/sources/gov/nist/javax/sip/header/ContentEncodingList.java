package gov.nist.javax.sip.header;

public final class ContentEncodingList extends SIPHeaderList<ContentEncoding> {
    private static final long serialVersionUID = 7365216146576273970L;

    @Override
    public Object clone() {
        ContentEncodingList contentEncodingList = new ContentEncodingList();
        contentEncodingList.clonehlist(this.hlist);
        return contentEncodingList;
    }

    public ContentEncodingList() {
        super(ContentEncoding.class, "Content-Encoding");
    }
}
