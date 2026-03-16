package org.apache.xml.dtm.ref;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DTMNodeListBase implements NodeList {
    @Override
    public Node item(int index) {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }
}
