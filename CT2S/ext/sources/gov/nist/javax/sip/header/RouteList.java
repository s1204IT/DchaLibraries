package gov.nist.javax.sip.header;

import java.util.ListIterator;

public class RouteList extends SIPHeaderList<Route> {
    private static final long serialVersionUID = 3407603519354809748L;

    public RouteList() {
        super(Route.class, "Route");
    }

    @Override
    public Object clone() {
        RouteList retval = new RouteList();
        retval.clonehlist(this.hlist);
        return retval;
    }

    @Override
    public String encode() {
        return this.hlist.isEmpty() ? "" : super.encode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RouteList)) {
            return false;
        }
        RouteList that = (RouteList) other;
        if (size() != that.size()) {
            return false;
        }
        ListIterator<Route> it = listIterator();
        ListIterator<Route> it1 = that.listIterator();
        while (it.hasNext()) {
            Route route = it.next();
            Route route1 = it1.next();
            if (!route.equals(route1)) {
                return false;
            }
        }
        return true;
    }
}
