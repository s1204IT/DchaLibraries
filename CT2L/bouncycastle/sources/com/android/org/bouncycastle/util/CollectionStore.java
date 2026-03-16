package com.android.org.bouncycastle.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionStore implements Store {
    private Collection _local;

    public CollectionStore(Collection collection) {
        this._local = new ArrayList(collection);
    }

    @Override
    public Collection getMatches(Selector selector) {
        if (selector == null) {
            return new ArrayList(this._local);
        }
        List col = new ArrayList();
        for (Object obj : this._local) {
            if (selector.match(obj)) {
                col.add(obj);
            }
        }
        return col;
    }
}
