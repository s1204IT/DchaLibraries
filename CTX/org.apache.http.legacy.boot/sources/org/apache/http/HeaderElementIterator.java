package org.apache.http;

import java.util.Iterator;

@Deprecated
public interface HeaderElementIterator extends Iterator {
    @Override
    boolean hasNext();

    HeaderElement nextElement();
}
