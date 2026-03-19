package gov.nist.javax.sip.header;

import java.util.ListIterator;

public class RouteList extends SIPHeaderList<Route> {
    private static final long serialVersionUID = 3407603519354809748L;

    public RouteList() {
        super(Route.class, "Route");
    }

    @Override
    public Object clone() {
        RouteList routeList = new RouteList();
        routeList.clonehlist(this.hlist);
        return routeList;
    }

    @Override
    public String encode() {
        return this.hlist.isEmpty() ? "" : super.encode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RouteList)) {
            return false;
        }
        RouteList routeList = (RouteList) obj;
        if (size() != routeList.size()) {
            return false;
        }
        ListIterator<Route> listIterator = listIterator();
        ListIterator<Route> listIterator2 = routeList.listIterator();
        while (listIterator.hasNext()) {
            if (!listIterator.next().equals(listIterator2.next())) {
                return false;
            }
        }
        return true;
    }
}
