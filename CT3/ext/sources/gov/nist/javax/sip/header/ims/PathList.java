package gov.nist.javax.sip.header.ims;

import gov.nist.javax.sip.header.SIPHeaderList;

public class PathList extends SIPHeaderList<Path> {
    public PathList() {
        super(Path.class, "Path");
    }

    @Override
    public Object clone() {
        PathList retval = new PathList();
        return retval.clonehlist(this.hlist);
    }
}
