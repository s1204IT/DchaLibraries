package gov.nist.javax.sip.header;

public class RecordRouteList extends SIPHeaderList<RecordRoute> {
    private static final long serialVersionUID = 1724940469426766691L;

    @Override
    public Object clone() {
        RecordRouteList recordRouteList = new RecordRouteList();
        recordRouteList.clonehlist(this.hlist);
        return recordRouteList;
    }

    public RecordRouteList() {
        super(RecordRoute.class, "Record-Route");
    }
}
